import com.sun.source.tree.*;
import com.sun.source.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * DeadCodeCutter USAGE.txt SRC_ROOT CLASSPATH [--apply] [EXTRA.java] : removes ProGuard-unused members/types from source
 * when no remaining source references them (fixed point over attributed javac trees). Conservative skip rules.
 * Methods that override one another are one family: a family is cut whole when ProGuard reports every member, none
 * overrides a library method and no lambda or method reference implements it. A field that kept code only assigns
 * (plain statements in a block, with values free of effects) goes with those assignments, reported or not. -Dwhy=FILE
 * lists every reported element that survives, with the rule or the kept declaration that holds it.
 */
public final class DeadCodeCutter {
    static Trees trees; static Elements elements; static Types types; static SourcePositions positions;
    static final Map<String, Set<String>> unusedMembers = new HashMap<>(); // binary class -> member keys
    static final Set<String> unusedTypes = new HashSet<>();
    static final Map<Element, TreePath> declarations = new LinkedHashMap<>();
    static final Set<Element> candidates = new LinkedHashSet<>();
    /** Why a reported element survived: the declaration whose kept code referenced it. */
    static final Map<Element, Element> keptBy = new LinkedHashMap<>();
    static Element scanning;
    /** Override families: each source method to the set of source methods it overrides or is overridden by. */
    static final Map<Element, Set<Element>> families = new HashMap<>();
    /** Methods whose family must stay: they override a library method or a lambda implements their interface. */
    static final Set<Element> pinned = new HashSet<>();
    /** Declarations directly inside a declaration (members of a type, members of a method's anonymous classes). */
    static final Map<Element, List<Element>> children = new HashMap<>();
    /** Assignment statements to candidate fields that were skipped as writes, not reads. */
    static final Map<Element, Set<TreePath>> writes = new LinkedHashMap<>();
    /** Calls that have no effect besides their value. */
    static final java.util.regex.Pattern PURE_CALLS = java.util.regex.Pattern.compile(
        "java\\.lang\\.Math\\..*|java\\.util\\.Objects\\.requireNonNull|java\\.util\\.(List|Set|Map)\\.of|java\\.util\\.Collections\\.(empty|unmodifiable|singleton).*"
        + "|java\\.lang\\.(String|Integer|Long|Double|Float|Boolean)\\.valueOf|java\\.util\\.Arrays\\.(copyOf|asList)");
    static final java.util.regex.Pattern PURE_TYPES = java.util.regex.Pattern.compile(
        "java\\.util\\.(concurrent\\.)?[A-Za-z]*(Map|List|Set|Deque|Queue)|java\\.util\\.(BitSet|Random)|java\\.lang\\.(Object|ThreadLocal|StringBuilder)"
        + "|java\\.util\\.concurrent\\.atomic\\..*|it\\.unimi\\.dsi\\.fastutil\\..*");

    public static void main(String[] args) throws Exception {
        parseUsage(Paths.get(args[0]));
        Path root = Paths.get(args[1]);
        boolean apply = args.length > 3 && args[3].equals("--apply");
        List<File> files;
        try (Stream<Path> s = Files.walk(root)) { files = s.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList()); }
        if (args.length > 4) files.add(new File(args[4]));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        JavacTask task = (JavacTask) compiler.getTask(null, fm, d -> {}, List.of("-proc:none", "-cp", args[2], "--release", "21", "-nowarn"), null, fm.getJavaFileObjectsFromFiles(files));
        List<CompilationUnitTree> units = new ArrayList<>();
        for (CompilationUnitTree u : task.parse()) units.add(u);
        task.analyze();
        trees = Trees.instance(task); elements = task.getElements(); types = task.getTypes(); positions = trees.getSourcePositions();
        Set<TypeElement> functional = new HashSet<>();
        for (CompilationUnitTree unit : units) new TreePathScanner<Void, Void>() {
            @Override public Void visitClass(ClassTree t, Void v) { record(getCurrentPath()); return super.visitClass(t, v); }
            @Override public Void visitMethod(MethodTree t, Void v) { record(getCurrentPath()); return super.visitMethod(t, v); }
            @Override public Void visitVariable(VariableTree t, Void v) {
                if (getCurrentPath().getParentPath().getLeaf() instanceof ClassTree) record(getCurrentPath());
                return super.visitVariable(t, v);
            }
            @Override public Void visitLambdaExpression(LambdaExpressionTree t, Void v) { target(); return super.visitLambdaExpression(t, v); }
            @Override public Void visitMemberReference(MemberReferenceTree t, Void v) { target(); return super.visitMemberReference(t, v); }
            void target() {
                TypeMirror m = trees.getTypeMirror(getCurrentPath());
                if (m != null && types.asElement(m) instanceof TypeElement te) functional.add(te);
            }
        }.scan(unit, null);
        buildFamilies(functional);
        // Candidates from the report, and every field that might only be written.
        for (var e : declarations.entrySet())
            if (isCandidate(e.getKey(), e.getValue()) || writableField(e.getKey(), e.getValue())) candidates.add(e.getKey());
        System.err.println("initial candidates: " + candidates.size());
        // Fixed point: everything not deleted is kept; scan kept code for references to candidates.
        Deque<TreePath> work = new ArrayDeque<>();
        for (var e : declarations.entrySet()) if (!deleted(e.getKey())) work.add(e.getValue());
        for (CompilationUnitTree unit : units) for (Tree t : unit.getTypeDecls()) { /* type decls recorded above */ }
        Set<Element> scanned = new HashSet<>();
        while (true) {
        while (!work.isEmpty()) {
            TreePath path = work.poll();
            Element owner = trees.getElement(path);
            if (owner != null && !scanned.add(owner)) continue;
            scanning = owner;
            for (Element ref : references(path)) keep(ref, work);
        }
        // A kept class never loses all of its explicit constructors (final fields, super calls, instantiation).
        for (Element t : declarations.keySet()) {
            if (!(t instanceof TypeElement te) || deleted(te)) continue;
            List<Element> ctors = te.getEnclosedElements().stream().filter(x -> x.getKind() == ElementKind.CONSTRUCTOR
                && declarations.containsKey(x)).collect(Collectors.toList());
            if (!ctors.isEmpty() && ctors.stream().allMatch(candidates::contains)) for (Element c : ctors) keep(c, work);
        }
        if (work.isEmpty()) break;
        }
        // Report and apply.
        Map<CompilationUnitTree, List<long[]>> cuts = new LinkedHashMap<>();
        Set<CompilationUnitTree> deleteFiles = new LinkedHashSet<>();
        Set<String> deletedTypeNames = new HashSet<>();
        long removedChars = 0; int removed = 0;
        for (Element e : candidates) {
            if (enclosedByCandidate(e)) continue; // removed with its enclosing type
            TreePath p = declarations.get(e);
            CompilationUnitTree unit = p.getCompilationUnit();
            if (e instanceof TypeElement te) deletedTypeNames.add(te.getQualifiedName().toString());
            if (e instanceof TypeElement && p.getParentPath().getLeaf() instanceof CompilationUnitTree) { deleteFiles.add(unit); continue; }
            long[] range = range(unit, p.getLeaf());
            cuts.computeIfAbsent(unit, k -> new ArrayList<>()).add(range);
            removed++;
        }
        for (var e : writes.entrySet()) {
            if (!candidates.contains(e.getKey())) continue;
            for (TreePath statement : e.getValue()) {
                if (deleted(enclosingDeclaration(statement))) continue;
                CompilationUnitTree unit = statement.getCompilationUnit();
                cuts.computeIfAbsent(unit, k -> new ArrayList<>()).add(range(unit, statement.getLeaf()));
                System.out.println("WRITE " + new File(unit.getSourceFile().toUri()).getName() + ":"
                    + unit.getLineMap().getLineNumber(positions.getStartPosition(unit, statement.getLeaf())) + " " + statement.getLeaf());
            }
        }
        for (var e : cuts.entrySet()) {
            if (deleteFiles.contains(e.getKey())) continue;
            removedChars += apply(e.getKey(), e.getValue(), deletedTypeNames, apply);
        }
        for (CompilationUnitTree u : deleteFiles) {
            File f = new File(u.getSourceFile().toUri());
            removedChars += f.length();
            System.out.println("DELETE " + f);
            if (apply) f.delete();
        }
        // Imports of deleted types in surviving files.
        if (apply) for (CompilationUnitTree u : units) {
            if (deleteFiles.contains(u)) continue;
            File f = new File(u.getSourceFile().toUri());
            List<String> lines = Files.readAllLines(f.toPath());
            boolean changed = lines.removeIf(l -> l.startsWith("import ") && deletedTypeNames.contains(l.substring(7).replace(";", "").replace("static ", "").trim()));
            if (changed) Files.write(f.toPath(), lines);
        }
        if (System.getProperty("why") != null) try (PrintWriter why = new PrintWriter(System.getProperty("why"))) {
            for (var e : declarations.entrySet()) {
                Element x = e.getKey();
                if (!isReported(x) || candidates.contains(x) || enclosedByCandidate(x)) continue;
                String rule = notCandidate(x, e.getValue());
                why.println(describe(x) + " :: " + (rule != null ? rule : "kept by " + describe(keptBy.get(x))));
            }
        }
        System.err.printf("members removed: %d, files deleted: %d, chars removed: %d%n", removed, deleteFiles.size(), removedChars);
    }

    static String describe(Element e) {
        if (e == null) return "?";
        Element owner = e.getEnclosingElement();
        String why = isReported(e) ? " (reported, not cut)" : "";
        return (owner instanceof TypeElement te ? te.getQualifiedName() + "#" : "") + e.getSimpleName() + why;
    }

    static boolean isReported(Element e) {
        if (e instanceof TypeElement te) return unusedTypes.contains(elements.getBinaryName(te).toString());
        if (!(e.getEnclosingElement() instanceof TypeElement te)) return false;
        if (isReported(te)) return true; // every member of an unused type
        Set<String> members = unusedMembers.get(elements.getBinaryName(te).toString());
        if (members == null) return false;
        if (e instanceof ExecutableElement m) return members.contains(key(m));
        return members.contains("F " + e.getSimpleName());
    }

    static void record(TreePath path) {
        Element e = trees.getElement(path);
        if (e == null || declarations.putIfAbsent(e, path) != null) return;
        for (Element x = e.getEnclosingElement(); x != null && !(x instanceof PackageElement); x = x.getEnclosingElement())
            if (declarations.containsKey(x)) { children.computeIfAbsent(x, k -> new ArrayList<>()).add(e); break; }
    }

    static boolean deleted(Element e) {
        for (Element x = e; x != null && !(x instanceof PackageElement); x = x.getEnclosingElement()) if (candidates.contains(x)) return true;
        return false;
    }

    static boolean enclosedByCandidate(Element e) {
        for (Element x = e.getEnclosingElement(); x != null && !(x instanceof PackageElement); x = x.getEnclosingElement()) if (candidates.contains(x)) return true;
        return false;
    }

    static void keep(Element ref, Deque<TreePath> work) {
        for (Element x = ref; x != null && !(x instanceof PackageElement); x = x.getEnclosingElement()) {
            if (candidates.remove(x)) {
                keptBy.put(x, scanning);
                TreePath p = declarations.get(x);
                if (p != null) work.add(p);
                work.addAll(writes.getOrDefault(x, Set.of())); // its assignments are statements again
                queueNested(x, work); // they were skipped while it was deleted
            }
            // A kept method keeps its family; members in deleted types come back with their type (queueMembers).
            for (Element y : families.getOrDefault(x, Set.of())) if (candidates.remove(y)) {
                keptBy.put(y, scanning);
                if (!deleted(y)) work.add(declarations.get(y));
            }
        }
    }

    static void buildFamilies(Set<TypeElement> functional) {
        Map<Element, Element> parent = new HashMap<>();
        for (Element e : declarations.keySet()) {
            if (!(e instanceof ExecutableElement m) || m.getKind() != ElementKind.METHOD || m.getModifiers().contains(Modifier.STATIC)) continue;
            TypeElement owner = (TypeElement) m.getEnclosingElement();
            Deque<TypeMirror> pending = new ArrayDeque<>(types.directSupertypes(owner.asType()));
            Set<Element> seen = new HashSet<>();
            while (!pending.isEmpty()) {
                Element st = types.asElement(pending.poll());
                if (!(st instanceof TypeElement ste) || !seen.add(ste)) continue;
                for (Element other : ste.getEnclosedElements())
                    if (other instanceof ExecutableElement om && om.getSimpleName().equals(m.getSimpleName()) && elements.overrides(m, om, owner)) {
                        if (declarations.containsKey(om)) union(parent, m, om); else pinned.add(m);
                    }
                pending.addAll(types.directSupertypes(ste.asType()));
            }
            if (m.getModifiers().contains(Modifier.ABSTRACT) && functional.contains(owner)) pinned.add(m);
            parent.putIfAbsent(m, m);
        }
        Map<Element, Set<Element>> byRoot = new HashMap<>();
        for (Element m : parent.keySet()) byRoot.computeIfAbsent(find(parent, m), k -> new LinkedHashSet<>()).add(m);
        for (Set<Element> family : byRoot.values()) {
            if (family.stream().anyMatch(pinned::contains)) pinned.addAll(family);
            if (family.size() > 1) for (Element m : family) families.put(m, family);
        }
    }

    static Element find(Map<Element, Element> parent, Element e) {
        Element p = parent.getOrDefault(e, e);
        if (p == e) return e;
        Element r = find(parent, p); parent.put(e, r); return r;
    }

    static void union(Map<Element, Element> parent, Element a, Element b) {
        parent.putIfAbsent(a, a); parent.putIfAbsent(b, b);
        Element ra = find(parent, a), rb = find(parent, b);
        if (ra != rb) parent.put(ra, rb);
    }

    static void queueNested(Element owner, Deque<TreePath> work) {
        for (Element m : children.getOrDefault(owner, List.of())) {
            if (candidates.contains(m)) continue;
            work.add(declarations.get(m));
            queueNested(m, work);
        }
    }

    /** Every declared element referenced inside this declaration (members of a type are scanned separately). */
    static Set<Element> references(TreePath path) {
        Set<Element> out = new HashSet<>();
        Tree root = path.getLeaf();
        new TreePathScanner<Void, Void>() {
            @Override public Void visitClass(ClassTree t, Void v) {
                if (t != root) {
                    Element e = trees.getElement(getCurrentPath());
                    if (e != null && e.getEnclosingElement() instanceof TypeElement && declarations.containsKey(e)) return null; // member type: separate
                    // Anonymous and local classes: their header and initializers belong here, their members are roots.
                    add(e);
                    scan(t.getModifiers(), v); scan(t.getTypeParameters(), v); scan(t.getExtendsClause(), v); scan(t.getImplementsClause(), v);
                    for (Tree m : t.getMembers()) if (m instanceof BlockTree) scan(m, v);
                    return null;
                }
                // Root class: header only (modifiers, type params, extends, implements); members are their own roots.
                scan(t.getModifiers(), v); scan(t.getTypeParameters(), v); scan(t.getExtendsClause(), v); scan(t.getImplementsClause(), v);
                for (Tree m : t.getMembers()) if (m instanceof BlockTree) scan(m, v); // initializer blocks
                Element self = trees.getElement(getCurrentPath());
                if (self instanceof TypeElement te) {
                    for (TypeMirror s : types.directSupertypes(te.asType())) add(types.asElement(s));
                    // Enum constants and record components cannot be cut: keep them as references of the type.
                    for (Element m : te.getEnclosedElements()) if (m.getKind() == ElementKind.ENUM_CONSTANT || m.getKind() == ElementKind.RECORD_COMPONENT) add(m);
                }
                return null;
            }
            @Override public Void visitAssignment(AssignmentTree t, Void v) {
                TreePath here = getCurrentPath();
                Element f = trees.getElement(new TreePath(here, t.getVariable()));
                if (f != null && f.getKind() == ElementKind.FIELD && candidates.contains(f) && removableWrite(here)) {
                    writes.computeIfAbsent(f, k -> new LinkedHashSet<>()).add(here.getParentPath());
                    return null;
                }
                return super.visitAssignment(t, v);
            }
            @Override public Void visitIdentifier(IdentifierTree t, Void v) { add(trees.getElement(getCurrentPath())); return super.visitIdentifier(t, v); }
            @Override public Void visitMemberSelect(MemberSelectTree t, Void v) { add(trees.getElement(getCurrentPath())); return super.visitMemberSelect(t, v); }
            @Override public Void visitMemberReference(MemberReferenceTree t, Void v) { add(trees.getElement(getCurrentPath())); return super.visitMemberReference(t, v); }
            @Override public Void visitNewClass(NewClassTree t, Void v) { add(trees.getElement(getCurrentPath())); return super.visitNewClass(t, v); }
            @Override public Void visitMethodInvocation(MethodInvocationTree t, Void v) { add(trees.getElement(getCurrentPath())); return super.visitMethodInvocation(t, v); }
            void add(Element e) { if (e != null) { out.add(e); if (e instanceof ExecutableElement ex && ex.getKind() == ElementKind.CONSTRUCTOR) out.add(ex.getEnclosingElement()); } }
        }.scan(path, null);
        return out;
    }

    static boolean isCandidate(Element e, TreePath path) {
        return isReported(e) && notCandidate(e, path) == null;
    }

    /** Why a reported element is never cut on its own, or null. */
    static String notCandidate(Element e, TreePath path) {
        if (!new File(path.getCompilationUnit().getSourceFile().toUri()).canWrite()) return "protected file";
        if (e instanceof TypeElement te)
            return te.getNestingKind() == NestingKind.ANONYMOUS || te.getNestingKind() == NestingKind.LOCAL ? "local class" : null;
        if (!(e.getEnclosingElement() instanceof TypeElement te)) return "not a member";
        if (te.getKind() == ElementKind.RECORD && !e.getModifiers().contains(Modifier.STATIC)
            && (e.getKind() != ElementKind.METHOD || te.getRecordComponents().stream().anyMatch(c -> c.getAccessor().equals(e))))
            return "record member";
        if (te.getKind() == ElementKind.ANNOTATION_TYPE) return "annotation member";
        if (e.getKind() == ElementKind.ENUM_CONSTANT) return "enum constant";
        if (e instanceof ExecutableElement m) {
            if (m.getKind() == ElementKind.STATIC_INIT || m.getKind() == ElementKind.INSTANCE_INIT) return "initializer";
            if (pinned.contains(m)) return "overrides a library method or implements a lambda's interface";
            if (m.getModifiers().contains(Modifier.ABSTRACT) && te.getAnnotationMirrors().stream()
                .anyMatch(a -> a.getAnnotationType().toString().equals("java.lang.FunctionalInterface")))
                return "a functional interface's method (the interface goes whole or not at all)";
            for (Element y : families.getOrDefault(m, Set.of())) if (!isReported(y)) return "family member used: " + describe(y);
            return null;
        }
        if (e instanceof VariableElement) {
            VariableTree vt = (VariableTree) path.getLeaf();
            if (vt.getInitializer() != null && !pure(new TreePath(path, vt.getInitializer()))) return "initializer with effects";
            // Multi-declarator statements share a start position; leave them alone.
            long start = positions.getStartPosition(path.getCompilationUnit(), vt);
            ClassTree ct = (ClassTree) path.getParentPath().getLeaf();
            for (Tree m : ct.getMembers()) if (m != vt && m instanceof VariableTree && positions.getStartPosition(path.getCompilationUnit(), m) == start) return "multi-declarator";
            return null;
        }
        return "other";
    }

    /** A field kept code may only assign: not a mixin's, not annotated, not a record's or a config's, cuttable. */
    static boolean writableField(Element e, TreePath path) {
        if (e.getKind() != ElementKind.FIELD || !e.getAnnotationMirrors().isEmpty()) return false;
        if (!(e.getEnclosingElement() instanceof TypeElement te) || te.getKind() == ElementKind.RECORD) return false;
        String owner = elements.getBinaryName(te).toString();
        if (owner.contains(".mixins.") || owner.contains(".config.")) return false; // applied by name; serialized
        return notCandidate(e, path) == null;
    }

    /** {@code [this.|name.]field = value;} as a statement of a block, with a value free of effects. */
    static boolean removableWrite(TreePath assignment) {
        AssignmentTree a = (AssignmentTree) assignment.getLeaf();
        TreePath statement = assignment.getParentPath();
        if (!(statement.getLeaf() instanceof ExpressionStatementTree) || !(statement.getParentPath().getLeaf() instanceof BlockTree)) return false;
        ExpressionTree target = a.getVariable();
        if (target instanceof MemberSelectTree ms && !(ms.getExpression() instanceof IdentifierTree)) return false;
        else if (!(target instanceof MemberSelectTree) && !(target instanceof IdentifierTree)) return false;
        return pure(new TreePath(assignment, a.getExpression()));
    }

    static Element enclosingDeclaration(TreePath path) {
        for (TreePath p = path; p != null; p = p.getParentPath()) {
            Tree leaf = p.getLeaf();
            if (leaf instanceof MethodTree || leaf instanceof ClassTree || leaf instanceof VariableTree && p.getParentPath().getLeaf() instanceof ClassTree) {
                Element e = trees.getElement(p);
                if (e != null && declarations.containsKey(e)) return e;
            }
        }
        return null;
    }

    /** No effects besides its value: no calls but PURE_CALLS, no writes, objects only of source or PURE_TYPES classes. */
    static boolean pure(TreePath expression) {
        boolean[] impure = {false};
        new TreePathScanner<Void, Void>() {
            @Override public Void visitMethodInvocation(MethodInvocationTree t, Void v) {
                Element m = trees.getElement(getCurrentPath());
                if (!(m instanceof ExecutableElement && m.getEnclosingElement() instanceof TypeElement owner
                    && PURE_CALLS.matcher(owner.getQualifiedName() + "." + m.getSimpleName()).matches())) impure[0] = true;
                return super.visitMethodInvocation(t, v);
            }
            @Override public Void visitNewClass(NewClassTree t, Void v) {
                Element c = trees.getElement(getCurrentPath());
                if (t.getClassBody() != null || c == null || !(c.getEnclosingElement() instanceof TypeElement type)
                    || !(declarations.containsKey(type) || PURE_TYPES.matcher(type.getQualifiedName()).matches())) impure[0] = true;
                return super.visitNewClass(t, v);
            }
            @Override public Void visitAssignment(AssignmentTree t, Void v) { impure[0] = true; return null; }
            @Override public Void visitCompoundAssignment(CompoundAssignmentTree t, Void v) { impure[0] = true; return null; }
            @Override public Void visitUnary(UnaryTree t, Void v) {
                if (t.getKind().name().contains("CREMENT")) impure[0] = true;
                return super.visitUnary(t, v);
            }
            @Override public Void visitLambdaExpression(LambdaExpressionTree t, Void v) { return null; }
        }.scan(expression, null);
        return !impure[0];
    }

    static String key(ExecutableElement m) {
        String name = m.getKind() == ElementKind.CONSTRUCTOR ? "<init>" : m.getSimpleName().toString();
        String params = m.getParameters().stream().map(p -> erasedName(p.asType())).collect(Collectors.joining(","));
        // An inner class's constructor takes its outer instance first, as ProGuard prints it.
        if (m.getKind() == ElementKind.CONSTRUCTOR && m.getEnclosingElement() instanceof TypeElement te
            && te.getNestingKind() == NestingKind.MEMBER && !te.getModifiers().contains(Modifier.STATIC)
            && te.getKind() == ElementKind.CLASS && te.getEnclosingElement() instanceof TypeElement outer)
            params = elements.getBinaryName(outer) + (params.isEmpty() ? "" : "," + params);
        return "M " + name + "(" + params + ")";
    }

    static String erasedName(TypeMirror t) {
        TypeMirror e = types.erasure(t);
        if (e.getKind() == TypeKind.ARRAY) return erasedName(((ArrayType) e).getComponentType()) + "[]";
        if (e.getKind() == TypeKind.DECLARED) return elements.getBinaryName((TypeElement) ((DeclaredType) e).asElement()).toString();
        return e.toString();
    }

    static void parseUsage(Path usage) throws IOException {
        String current = null;
        for (String line : Files.readAllLines(usage)) {
            if (line.isBlank()) continue;
            if (!line.startsWith(" ")) {
                if (line.endsWith(":")) current = line.substring(0, line.length() - 1);
                else { unusedTypes.add(line.trim()); current = null; }
                continue;
            }
            if (current == null) continue;
            String s = line.trim().replaceFirst("^\\d+:\\d+:", "");
            String simple = current.substring(current.lastIndexOf('.') + 1);
            String keyed;
            int paren = s.indexOf('(');
            if (paren < 0) {
                String[] parts = s.split("\\s+");
                keyed = "F " + parts[parts.length - 1];
            } else {
                String head = s.substring(0, paren).trim();
                String[] parts = head.split("\\s+");
                String name = parts[parts.length - 1];
                if (name.equals(simple)) name = "<init>";
                String params = s.substring(paren + 1, s.lastIndexOf(')')).replace(" ", "");
                keyed = "M " + name + "(" + params + ")";
            }
            unusedMembers.computeIfAbsent(current, k -> new HashSet<>()).add(keyed);
        }
    }

    static long[] range(CompilationUnitTree unit, Tree t) {
        return new long[] { positions.getStartPosition(unit, t), positions.getEndPosition(unit, t) };
    }

    static long apply(CompilationUnitTree unit, List<long[]> ranges, Set<String> deletedTypes, boolean write) throws IOException {
        File f = new File(unit.getSourceFile().toUri());
        String src = Files.readString(f.toPath());
        ranges.sort((a, b) -> a[0] != b[0] ? Long.compare(a[0], b[0]) : Long.compare(b[1], a[1]));
        List<long[]> outer = new ArrayList<>();
        for (long[] r : ranges) if (outer.isEmpty() || r[0] >= outer.get(outer.size() - 1)[1]) outer.add(r);
        ranges = outer;
        ranges.sort((a, b) -> Long.compare(b[0], a[0]));
        StringBuilder out = new StringBuilder(src);
        long removed = 0;
        long lastStart = Long.MAX_VALUE;
        for (long[] r : ranges) {
            int start = (int) r[0], end = (int) r[1];
            if (start < 0 || end <= start || end > lastStart) continue; // nested/overlapping: skip
            // Absorb a directly preceding Javadoc/comment block and the line's leading indentation.
            int s = start;
            int ls = lineStart(out, s);
            if (out.substring(ls, s).isBlank()) {
                s = ls;
                int prev = s;
                while (true) {
                    int pe = prev - 1; if (pe < 0) break;
                    int pls = lineStart(out, pe);
                    String pl = out.substring(pls, pe).trim();
                    if (pl.startsWith("//") || pl.startsWith("*") || pl.startsWith("/**") || pl.startsWith("/*") || pl.endsWith("*/")) { prev = pls; continue; }
                    break;
                }
                s = prev;
            }
            int e = end;
            int le = out.indexOf("\n", e);
            if (le >= 0 && out.substring(e, le).isBlank() && s == lineStart(out, s)) e = le + 1;
            removed += e - s;
            out.delete(s, e);
            lastStart = s;
        }
        if (write) Files.writeString(f.toPath(), out.toString());
        System.out.println("CUT " + f + " -" + removed);
        return removed;
    }

    static int lineStart(CharSequence s, int pos) {
        int i = pos;
        while (i > 0 && s.charAt(i - 1) != '\n') i--;
        return i;
    }
}
