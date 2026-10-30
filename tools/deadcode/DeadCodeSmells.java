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
 * DeadCodeSmells SRC_ROOT CLASSPATH [EXTRA.java] : what DeadCodeCutter leaves because it cannot prove a value unused
 * (after it has run): fields nothing reads, parameters nothing reads (methods that override nothing and are overridden
 * by nothing), locals nothing reads, interfaces without members and anonymous classes without a body. A read is any
 * use of the value; a plain assignment (=) is a write, and so is a compound assignment or ++ used as a statement.
 * Mixins are left out (their fields are their targets'). With --cut-params, one unread parameter per method is
 * removed together with its argument at every call site, when the method overrides nothing, nothing overrides it, no
 * method reference names it and every such argument is free of effects; run it again until it cuts nothing.
 */
public final class DeadCodeSmells {
    static Trees trees; static Elements elements; static Types types; static SourcePositions positions;

    public static void main(String[] args) throws Exception {
        List<File> files;
        try (Stream<Path> s = Files.walk(Paths.get(args[0]))) { files = s.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList()); }
        boolean cutParams = Arrays.asList(args).contains("--cut-params");
        for (int i = 2; i < args.length; i++) if (args[i].endsWith(".java")) files.add(new File(args[i]));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        JavacTask task = (JavacTask) compiler.getTask(null, fm, d -> {}, List.of("-proc:none", "-cp", args[1], "--release", "21", "-nowarn"), null, fm.getJavaFileObjectsFromFiles(files));
        List<CompilationUnitTree> units = new ArrayList<>();
        for (CompilationUnitTree u : task.parse()) units.add(u);
        task.analyze();
        trees = Trees.instance(task); elements = task.getElements(); types = task.getTypes(); positions = trees.getSourcePositions();

        Map<Element, String> declared = new LinkedHashMap<>(); // variable -> where
        Map<Element, Integer> reads = new HashMap<>();
        Set<Element> overriding = new HashSet<>();
        Map<Element, List<TreePath>> calls = new HashMap<>();
        Set<Element> named = new HashSet<>(); // by a method reference
        Set<Element> sourceMethods = new HashSet<>();
        Set<TypeElement> records = new HashSet<>();
        Map<Element, Boolean> onlyNull = new HashMap<>(); // field -> every value it is given is null
        List<String> smells = new ArrayList<>();
        for (CompilationUnitTree unit : units) {
            String file = new File(unit.getSourceFile().toUri()).getPath();
            boolean mixin = file.contains("/mixins/");
            new TreePathScanner<Void, Void>() {
                String where(Tree t) { return file.replaceFirst(".*/biomic/", "") + ":" + unit.getLineMap().getLineNumber(positions.getStartPosition(unit, t)); }
                @Override public Void visitClass(ClassTree t, Void v) {
                    Element e = trees.getElement(getCurrentPath());
                    if (e instanceof TypeElement te && te.getKind() == ElementKind.RECORD) records.add(te);
                    if (e != null && e.getKind() == ElementKind.INTERFACE && t.getMembers().isEmpty() && !mixin)
                        smells.add("EMPTY-INTERFACE " + where(t) + " " + ((TypeElement) e).getQualifiedName());
                    if (t.getSimpleName().length() == 0 && t.getMembers().stream().allMatch(m -> m instanceof MethodTree mt && mt.getName().contentEquals("<init>")))
                        smells.add("EMPTY-ANONYMOUS " + where(t));
                    return super.visitClass(t, v);
                }
                @Override public Void visitMethod(MethodTree t, Void v) {
                    Element m = trees.getElement(getCurrentPath());
                    if (m instanceof ExecutableElement ex) { sourceMethods.add(ex); overrides(ex, overriding); }
                    return super.visitMethod(t, v);
                }
                @Override public Void visitMethodInvocation(MethodInvocationTree t, Void v) {
                    Element m = trees.getElement(getCurrentPath());
                    if (m != null) calls.computeIfAbsent(m, k -> new ArrayList<>()).add(getCurrentPath());
                    return super.visitMethodInvocation(t, v);
                }
                @Override public Void visitNewClass(NewClassTree t, Void v) {
                    Element m = trees.getElement(getCurrentPath());
                    if (m != null) calls.computeIfAbsent(m, k -> new ArrayList<>()).add(getCurrentPath());
                    return super.visitNewClass(t, v);
                }
                @Override public Void visitVariable(VariableTree t, Void v) {
                    Element e = trees.getElement(getCurrentPath());
                    if (e != null && !mixin && t.getModifiers().getAnnotations().isEmpty()
                        && !(getCurrentPath().getParentPath().getLeaf() instanceof LambdaExpressionTree)
                        && (e.getKind() == ElementKind.FIELD || e.getKind() == ElementKind.PARAMETER || e.getKind() == ElementKind.LOCAL_VARIABLE)
                        && !t.getName().contentEquals("serialVersionUID") && !t.getName().toString().startsWith("ignored")
                        && !t.getName().contentEquals("_"))
                        declared.put(e, where(t));
                    if (e != null && e.getKind() == ElementKind.FIELD && !e.asType().getKind().isPrimitive()
                        && e.getEnclosingElement().getKind() != ElementKind.RECORD)
                        onlyNull.merge(e, t.getInitializer() == null || t.getInitializer().getKind() == Tree.Kind.NULL_LITERAL, Boolean::logicalAnd);
                    return super.visitVariable(t, v);
                }
                @Override public Void visitAssignment(AssignmentTree t, Void v) {
                    Element target = trees.getElement(new TreePath(getCurrentPath(), t.getVariable()));
                    if (target != null && target.getKind() == ElementKind.FIELD)
                        onlyNull.merge(target, t.getExpression().getKind() == Tree.Kind.NULL_LITERAL, Boolean::logicalAnd);
                    scanTarget(t.getVariable());
                    return scan(t.getExpression(), v);
                }
                @Override public Void visitCompoundAssignment(CompoundAssignmentTree t, Void v) {
                    if (getCurrentPath().getParentPath().getLeaf() instanceof ExpressionStatementTree) { scanTarget(t.getVariable()); return scan(t.getExpression(), v); }
                    return super.visitCompoundAssignment(t, v);
                }
                @Override public Void visitUnary(UnaryTree t, Void v) {
                    if (t.getKind().name().contains("CREMENT") && getCurrentPath().getParentPath().getLeaf() instanceof ExpressionStatementTree) { scanTarget(t.getExpression()); return null; }
                    return super.visitUnary(t, v);
                }
                /** The target of a write: its qualifier and indices are read, the variable itself is not. */
                void scanTarget(ExpressionTree target) {
                    if (target instanceof MemberSelectTree ms) scan(ms.getExpression(), null);
                    else if (target instanceof ArrayAccessTree aa) { scan(aa.getExpression(), null); scan(aa.getIndex(), null); }
                }
                @Override public Void visitIdentifier(IdentifierTree t, Void v) { read(trees.getElement(getCurrentPath())); return super.visitIdentifier(t, v); }
                @Override public Void visitMemberSelect(MemberSelectTree t, Void v) { read(trees.getElement(getCurrentPath())); return super.visitMemberSelect(t, v); }
                @Override public Void visitMemberReference(MemberReferenceTree t, Void v) {
                    Element m = trees.getElement(getCurrentPath());
                    read(m); named.add(m);
                    return super.visitMemberReference(t, v);
                }
                void read(Element e) {
                    if (e == null) return;
                    reads.merge(e, 1, Integer::sum);
                    // A record's accessor reads its component.
                    if (e instanceof ExecutableElement m && m.getEnclosingElement() instanceof TypeElement te && te.getKind() == ElementKind.RECORD)
                        for (RecordComponentElement c : te.getRecordComponents())
                            if (m.equals(c.getAccessor())) for (Element f : te.getEnclosedElements())
                                if (f.getKind() == ElementKind.FIELD && f.getSimpleName().equals(c.getSimpleName())) reads.merge(f, 1, Integer::sum);
                }
            }.scan(unit, null);
        }
        // A record's accessor that implements a called interface method reads its component.
        for (TypeElement te : records) for (RecordComponentElement c : te.getRecordComponents()) {
            ExecutableElement accessor = c.getAccessor();
            boolean read = false;
            for (Element m : List.copyOf(reads.keySet()))
                if (m instanceof ExecutableElement om && om != accessor && om.getSimpleName().equals(accessor.getSimpleName())
                    && elements.overrides(accessor, om, te)) { read = true; break; }
            if (read) for (Element f : te.getEnclosedElements())
                if (f.getKind() == ElementKind.FIELD && f.getSimpleName().equals(c.getSimpleName())) reads.merge(f, 1, Integer::sum);
        }
        for (var e : onlyNull.entrySet())
            if (e.getValue() && reads.containsKey(e.getKey()) && declared.containsKey(e.getKey()))
                smells.add("ALWAYS-NULL " + declared.get(e.getKey()) + " " + e.getKey().getSimpleName());
        for (var e : declared.entrySet()) {
            Element x = e.getKey();
            if (reads.containsKey(x)) continue;
            if (x.getKind() == ElementKind.PARAMETER) {
                Element m = x.getEnclosingElement();
                if (!(m instanceof ExecutableElement ex) || overriding.contains(ex) || named.contains(ex) || ex.getModifiers().contains(Modifier.ABSTRACT)
                    || ex.getModifiers().contains(Modifier.NATIVE) || ex.getEnclosingElement().getKind() == ElementKind.INTERFACE
                    || ex.getEnclosingElement().getKind() == ElementKind.RECORD || isMain(ex)) continue;
                if (m.getEnclosingElement() instanceof TypeElement te && te.getNestingKind() == NestingKind.ANONYMOUS) continue;
                if (lambdaParam(x)) continue;
            }
            smells.add((x.getKind() == ElementKind.FIELD ? "UNREAD-FIELD " : x.getKind() == ElementKind.PARAMETER ? "UNREAD-PARAM " : "UNREAD-LOCAL ")
                + e.getValue() + " " + (x.getKind() == ElementKind.PARAMETER ? x.getEnclosingElement().getSimpleName() + "(" + x.getSimpleName() + ")" : x.getSimpleName()));
        }
        if (cutParams) {
            Map<CompilationUnitTree, List<long[]>> edits = new LinkedHashMap<>();
            Map<ExecutableElement, TreeSet<Integer>> removable = new LinkedHashMap<>();
            for (Element x : declared.keySet()) {
                if (x.getKind() != ElementKind.PARAMETER || reads.containsKey(x) || !(x.getEnclosingElement() instanceof ExecutableElement m)) continue;
                // Annotated methods are called by what reads the annotation (FML's handlers, mixins) with their full list.
                if (!m.getAnnotationMirrors().isEmpty()) continue;
                if (!sourceMethods.contains(m) || overriding.contains(m) || named.contains(m) || m.getModifiers().contains(Modifier.ABSTRACT)
                    || m.getEnclosingElement().getKind() == ElementKind.RECORD || m.getEnclosingElement().getKind() == ElementKind.INTERFACE || isMain(m)) continue;
                if (m.getEnclosingElement() instanceof TypeElement te && te.getNestingKind() == NestingKind.ANONYMOUS) continue;
                int index = -1;
                for (int i = 0; i < m.getParameters().size(); i++) if (m.getParameters().get(i).getSimpleName().equals(x.getSimpleName())) index = i;
                if (index < 0 || trees.getPath(m) == null) continue;
                if (m.isVarArgs() && index == m.getParameters().size() - 1) continue;
                boolean ok = true;
                for (TreePath site : calls.getOrDefault(m, List.of())) {
                    List<? extends ExpressionTree> argsAt = arguments(site);
                    if (argsAt.size() != m.getParameters().size() || !pure(new TreePath(site, argsAt.get(index)))) { ok = false; break; }
                }
                if (!ok) { System.out.println("KEEP-PARAM (argument with effects) " + declared.get(x) + " " + m.getSimpleName() + "(" + x.getSimpleName() + ")"); continue; }
                removable.computeIfAbsent(m, k -> new TreeSet<>()).add(index);
            }
            // A shorter list must not meet another overload's arity: it could clash or rebind a call.
            removable.entrySet().removeIf(r -> overloadedAt(r.getKey(), r.getKey().getParameters().size() - r.getValue().size()));
            int cut = 0;
            for (var r : removable.entrySet()) {
                TreePath decl = trees.getPath(r.getKey());
                edits.computeIfAbsent(decl.getCompilationUnit(), k -> new ArrayList<>())
                    .addAll(listCuts(decl.getCompilationUnit(), ((MethodTree) decl.getLeaf()).getParameters(), r.getValue()));
                for (TreePath site : calls.getOrDefault(r.getKey(), List.of()))
                    edits.computeIfAbsent(site.getCompilationUnit(), k -> new ArrayList<>()).addAll(listCuts(site.getCompilationUnit(), arguments(site), r.getValue()));
                cut += r.getValue().size();
            }
            for (var e : edits.entrySet()) {
                File f = new File(e.getKey().getSourceFile().toUri());
                StringBuilder src = new StringBuilder(Files.readString(f.toPath()));
                List<long[]> ranges = new ArrayList<>(e.getValue());
                ranges.sort((a, b) -> a[0] != b[0] ? Long.compare(a[0], b[0]) : Long.compare(b[1], a[1]));
                List<long[]> outer = new ArrayList<>();
                for (long[] r : ranges) if (outer.isEmpty() || r[0] >= outer.get(outer.size() - 1)[1]) outer.add(r);
                Collections.reverse(outer);
                for (long[] r : outer) src.delete((int) r[0], (int) r[1]);
                Files.writeString(f.toPath(), src.toString());
            }
            System.err.println("parameters cut: " + cut);
        }
        // Parameters every call passes the same literal: their method only ever runs one way.
        for (Element m : sourceMethods) {
            if (!(m instanceof ExecutableElement ex) || overriding.contains(ex) || named.contains(ex)) continue;
            List<TreePath> sites = calls.getOrDefault(ex, List.of());
            if (sites.isEmpty()) continue;
            for (int i = 0; i < ex.getParameters().size(); i++) {
                String literal = null; boolean same = true;
                for (TreePath site : sites) {
                    List<? extends ExpressionTree> a = arguments(site);
                    if (a.size() != ex.getParameters().size() || !(a.get(i) instanceof LiteralTree lit)) { same = false; break; }
                    String text = String.valueOf(lit.getValue());
                    if (literal == null) literal = text; else if (!literal.equals(text)) { same = false; break; }
                }
                if (same && declared.containsKey(ex.getParameters().get(i)) || same && trees.getPath(ex) != null)
                    if (same) smells.add("CONSTANT-PARAM " + where(trees.getPath(ex)) + " " + ex.getSimpleName() + "(" + ex.getParameters().get(i).getSimpleName() + "=" + literal + ") x" + sites.size());
            }
        }
        smells.sort(Comparator.naturalOrder());
        smells.forEach(System.out::println);
        Map<String, Long> counts = new TreeMap<>();
        for (String l : smells) counts.merge(l.substring(0, l.indexOf(' ')), 1L, Long::sum);
        System.err.println(counts);
    }

    static List<? extends ExpressionTree> arguments(TreePath call) {
        return call.getLeaf() instanceof MethodInvocationTree mi ? mi.getArguments() : ((NewClassTree) call.getLeaf()).getArguments();
    }

    /** The text to delete for the given elements of a comma-separated list, their separating commas included. */
    static List<long[]> listCuts(CompilationUnitTree unit, List<? extends Tree> list, Set<Integer> removed) {
        List<long[]> out = new ArrayList<>();
        int n = list.size();
        if (removed.size() == n) { out.add(new long[] { positions.getStartPosition(unit, list.get(0)), positions.getEndPosition(unit, list.get(n - 1)) }); return out; }
        int lastKept = -1;
        for (int i = 0; i < n; i++) if (!removed.contains(i)) lastKept = i;
        for (int i : removed) {
            if (i < lastKept) out.add(new long[] { positions.getStartPosition(unit, list.get(i)), positions.getStartPosition(unit, list.get(i + 1)) });
            else out.add(new long[] { positions.getEndPosition(unit, list.get(i - 1)), positions.getEndPosition(unit, list.get(i)) });
        }
        return out;
    }

    static final java.util.regex.Pattern PURE_CALLS = java.util.regex.Pattern.compile(
        "java\\.lang\\.Math\\..*|java\\.util\\.Objects\\.requireNonNull|java\\.util\\.(List|Set|Map)\\.of|java\\.util\\.Collections\\.(empty|unmodifiable|singleton).*"
        + "|java\\.lang\\.(String|Integer|Long|Double|Float|Boolean)\\.valueOf|java\\.util\\.Arrays\\.(copyOf|asList)");
    static final java.util.regex.Pattern PURE_TYPES = java.util.regex.Pattern.compile(
        "java\\.util\\.(concurrent\\.)?[A-Za-z]*(Map|List|Set|Deque|Queue)|java\\.util\\.(BitSet|Random)|java\\.lang\\.(Object|ThreadLocal|StringBuilder)"
        + "|java\\.util\\.concurrent\\.atomic\\..*|it\\.unimi\\.dsi\\.fastutil\\..*");

    /** As DeadCodeCutter: no calls but PURE_CALLS, no writes, objects only of source or PURE_TYPES classes. */
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
                    || !(trees.getPath(type) != null || PURE_TYPES.matcher(type.getQualifiedName()).matches())) impure[0] = true;
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

    /** Marks m and every source or library method it overrides. */
    static void overrides(ExecutableElement m, Set<Element> out) {
        if (m.getKind() != ElementKind.METHOD || m.getModifiers().contains(Modifier.STATIC) || m.getModifiers().contains(Modifier.PRIVATE)) return;
        TypeElement owner = (TypeElement) m.getEnclosingElement();
        Deque<TypeMirror> pending = new ArrayDeque<>(types.directSupertypes(owner.asType()));
        Set<Element> seen = new HashSet<>();
        while (!pending.isEmpty()) {
            Element st = types.asElement(pending.poll());
            if (!(st instanceof TypeElement ste) || !seen.add(ste)) continue;
            for (Element other : ste.getEnclosedElements())
                if (other instanceof ExecutableElement om && om.getSimpleName().equals(m.getSimpleName()) && elements.overrides(m, om, owner)) { out.add(m); out.add(om); }
            pending.addAll(types.directSupertypes(ste.asType()));
        }
    }

    /** Another method or constructor of the same name, in the type or its supertypes, taking that many parameters. */
    static boolean overloadedAt(ExecutableElement m, int arity) {
        TypeElement owner = (TypeElement) m.getEnclosingElement();
        for (Element e : elements.getAllMembers(owner))
            if (e != m && e instanceof ExecutableElement other && other.getKind() == m.getKind()
                && other.getSimpleName().equals(m.getSimpleName()) && (other.getParameters().size() == arity || other.isVarArgs())) return true;
        return false;
    }

    static String where(TreePath path) {
        if (path == null) return "?";
        CompilationUnitTree unit = path.getCompilationUnit();
        return new File(unit.getSourceFile().toUri()).getPath().replaceFirst(".*/biomic/", "") + ":"
            + unit.getLineMap().getLineNumber(positions.getStartPosition(unit, path.getLeaf()));
    }

    static boolean isMain(ExecutableElement m) { return m.getSimpleName().contentEquals("main") && m.getModifiers().contains(Modifier.STATIC); }

    static boolean lambdaParam(Element p) { return !(p.getEnclosingElement() instanceof ExecutableElement); }
}
