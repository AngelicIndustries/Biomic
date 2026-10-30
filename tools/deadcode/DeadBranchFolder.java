import com.sun.source.tree.*;
import com.sun.source.util.*;

import javax.lang.model.element.*;
import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * DeadBranchFolder SRC_ROOT CLASSPATH [EXTRA.java...] FACT... : folds code that a known constant decides.
 *
 * <p>A FACT is {@code pkg.Class#field=value} (value true, false or null): a field that only ever holds that value; or
 * {@code path/To.java:LINE/param=value}: a parameter of the method declared at that line that every call gives that
 * value (DeadCodeSmells lists them as CONSTANT-PARAM; DeadCodeSmells --cut-params then removes the parameter); or
 * {@code pkg.Class#getter()=value}: a method without arguments that always returns it, called on a plain name.
 * Reads of it become the literal; so do effectively final locals whose initializer folds to a literal. Then
 * {@code !}, {@code &&}, {@code ||}, {@code ==}/{@code !=} against null and {@code ?:} fold where the result is
 * decided without dropping an operand that has effects, and an {@code if} or {@code ?:} whose condition is a literal
 * keeps only the branch that runs. A pass edits each file once, innermost decisions first; run it again until it
 * changes nothing, then remove the facts' fields and what assigned them (the folder lists those sites).</p>
 */
public final class DeadBranchFolder {
    static Trees trees; static SourcePositions positions;
    static final Map<String, String> facts = new HashMap<>(); // "binary.Class#field" -> literal text

    public static void main(String[] args) throws Exception {
        List<File> files;
        try (Stream<Path> s = Files.walk(Paths.get(args[0]))) { files = s.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList()); }
        for (int i = 2; i < args.length; i++) {
            if (args[i].endsWith(".java")) files.add(new File(args[i]));
            else { int eq = args[i].lastIndexOf('='); facts.put(args[i].substring(0, eq), args[i].substring(eq + 1)); }
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        JavacTask task = (JavacTask) compiler.getTask(null, fm, d -> {}, List.of("-proc:none", "-cp", args[1], "--release", "21", "-nowarn"), null, fm.getJavaFileObjectsFromFiles(files));
        List<CompilationUnitTree> units = new ArrayList<>();
        for (CompilationUnitTree u : task.parse()) units.add(u);
        task.analyze();
        trees = Trees.instance(task); positions = trees.getSourcePositions();

        // Constant locals: effectively final, initialized to something that folds to a literal.
        Map<Element, String> constants = new HashMap<>();
        Set<Element> reassigned = new HashSet<>();
        for (CompilationUnitTree unit : units) new TreePathScanner<Void, Void>() {
            @Override public Void visitAssignment(AssignmentTree t, Void v) { mark(t.getVariable()); return super.visitAssignment(t, v); }
            @Override public Void visitCompoundAssignment(CompoundAssignmentTree t, Void v) { mark(t.getVariable()); return super.visitCompoundAssignment(t, v); }
            @Override public Void visitUnary(UnaryTree t, Void v) { if (t.getKind().name().contains("CREMENT")) mark(t.getExpression()); return super.visitUnary(t, v); }
            void mark(ExpressionTree target) { Element e = trees.getElement(new TreePath(getCurrentPath(), target)); if (e != null) reassigned.add(e); }
        }.scan(unit, null);
        Map<CompilationUnitTree, List<Edit>> edits = new LinkedHashMap<>();
        List<String> report = new ArrayList<>();
        for (CompilationUnitTree unit : units) {
            String src = Files.readString(Paths.get(unit.getSourceFile().toUri()));
            List<Edit> out = new ArrayList<>();
            new TreePathScanner<Void, Void>() {
                @Override public Void visitVariable(VariableTree t, Void v) {
                    super.visitVariable(t, v);
                    Element e = trees.getElement(getCurrentPath());
                    if (e != null && e.getKind() == ElementKind.LOCAL_VARIABLE && !reassigned.contains(e) && t.getInitializer() != null) {
                        String value = fold(new TreePath(getCurrentPath(), t.getInitializer()), constants, src);
                        if (isLiteral(value) && pure(new TreePath(getCurrentPath(), t.getInitializer()))) {
                            constants.put(e, value);
                            if (getCurrentPath().getParentPath().getLeaf() instanceof BlockTree) out.add(removeLine(src, start(unit, t), end(unit, t)));
                        }
                    }
                    return null;
                }
            }.scan(unit, null);
            // Decisions: outermost foldable expressions and statements; nested ones wait for the next pass.
            new TreePathScanner<Void, Void>() {
                @Override public Void visitIf(IfTree t, Void v) {
                    String c = fold(new TreePath(getCurrentPath(), t.getCondition()), constants, src);
                    if ("true".equals(c) || "false".equals(c)) {
                        StatementTree keep = "true".equals(c) ? t.getThenStatement() : t.getElseStatement();
                        out.add(replaceStatement(unit, src, getCurrentPath(), keep));
                        return null;
                    }
                    if (c != null && !strip(c).equals(strip(source(unit, src, t.getCondition())))) {
                        out.add(new Edit(start(unit, t.getCondition()), end(unit, t.getCondition()), c));
                        return null;
                    }
                    return super.visitIf(t, v);
                }
                @Override public Void visitConditionalExpression(ConditionalExpressionTree t, Void v) { return expression(t); }
                @Override public Void visitBinary(BinaryTree t, Void v) { return expression(t); }
                @Override public Void visitUnary(UnaryTree t, Void v) { return expression(t); }
                @Override public Void visitIdentifier(IdentifierTree t, Void v) { return expression(t); }
                @Override public Void visitMethodInvocation(MethodInvocationTree t, Void v) {
                    if (t.getArguments().isEmpty()) {
                        String folded = fold(getCurrentPath(), constants, src);
                        if (isLiteral(folded) && !(getCurrentPath().getParentPath().getLeaf() instanceof ExpressionStatementTree)) {
                            out.add(new Edit(start(unit, t), end(unit, t), folded));
                            return null;
                        }
                    }
                    return super.visitMethodInvocation(t, v);
                }
                @Override public Void visitParenthesized(ParenthesizedTree t, Void v) {
                    if (getCurrentPath().getParentPath().getLeaf() instanceof IfTree) return super.visitParenthesized(t, v);
                    return expression(t);
                }
                @Override public Void visitMemberSelect(MemberSelectTree t, Void v) {
                    if (getCurrentPath().getParentPath().getLeaf() instanceof MethodInvocationTree mi && mi.getMethodSelect() == t) return super.visitMemberSelect(t, v);
                    return expression(t);
                }
                Void expression(ExpressionTree t) {
                    TreePath path = getCurrentPath();
                    if (source(unit, src, t) == null) return null;
                    Tree parent = path.getParentPath().getLeaf();
                    // A write's target, a declaration's own name, an import: not a value.
                    if (parent instanceof AssignmentTree a && a.getVariable() == t || parent instanceof ImportTree
                        || parent instanceof CompoundAssignmentTree ca && ca.getVariable() == t) return null;
                    if (parent instanceof UnaryTree u && u.getKind().name().contains("CREMENT")) return null;
                    if (parent instanceof VariableTree vt && constants.containsKey(trees.getElement(path.getParentPath()))) return null;
                    String folded = fold(path, constants, src);
                    String original = source(unit, src, t);
                    if (folded != null && !strip(folded).equals(strip(original))) {
                        out.add(new Edit(start(unit, t), end(unit, t), folded));
                        return null;
                    }
                    return t instanceof IdentifierTree ? null : scanChildren(t);
                }
                Void scanChildren(ExpressionTree t) {
                    if (t instanceof ConditionalExpressionTree c) { scan(c.getCondition(), null); scan(c.getTrueExpression(), null); scan(c.getFalseExpression(), null); }
                    else if (t instanceof BinaryTree b) { scan(b.getLeftOperand(), null); scan(b.getRightOperand(), null); }
                    else if (t instanceof UnaryTree u) scan(u.getExpression(), null);
                    else if (t instanceof MemberSelectTree m) scan(m.getExpression(), null);
                    else if (t instanceof ParenthesizedTree p) scan(p.getExpression(), null);
                    return null;
                }
            }.scan(unit, null);
            if (!out.isEmpty()) edits.put(unit, out);
            // Report what still names a fact field: declarations and writes, for removal by hand.
            new TreePathScanner<Void, Void>() {
                @Override public Void visitVariable(VariableTree t, Void v) {
                    Element e = trees.getElement(getCurrentPath());
                    if (e != null && factOf(e) != null) report.add("DECLARATION " + where(unit, t) + " " + e.getSimpleName());
                    return super.visitVariable(t, v);
                }
                @Override public Void visitAssignment(AssignmentTree t, Void v) {
                    Element e = trees.getElement(new TreePath(getCurrentPath(), t.getVariable()));
                    if (e != null && factOf(e) != null) report.add("WRITE " + where(unit, t) + " " + t);
                    return super.visitAssignment(t, v);
                }
            }.scan(unit, null);
        }
        int count = 0;
        for (var e : edits.entrySet()) {
            Path path = Paths.get(e.getKey().getSourceFile().toUri());
            StringBuilder src = new StringBuilder(Files.readString(path));
            List<Edit> list = new ArrayList<>(e.getValue());
            list.sort((a, b) -> a.start != b.start ? Long.compare(a.start, b.start) : Long.compare(b.end, a.end));
            List<Edit> outer = new ArrayList<>();
            for (Edit x : list) if (outer.isEmpty() || x.start >= outer.get(outer.size() - 1).end) outer.add(x);
            Collections.reverse(outer);
            for (Edit x : outer) { src.replace((int) x.start, (int) x.end, x.text); count++; }
            Files.writeString(path, src.toString());
            System.out.println("FOLDED " + path.getFileName() + " " + outer.size());
        }
        report.forEach(System.out::println);
        System.err.println("folds: " + count);
    }

    record Edit(long start, long end, String text) {}

    static String factOf(Element e) {
        if (e.getKind() == ElementKind.PARAMETER && e.getEnclosingElement() instanceof ExecutableElement m) {
            TreePath method = trees.getPath(m);
            if (method == null) return null;
            CompilationUnitTree unit = method.getCompilationUnit();
            String file = new File(unit.getSourceFile().toUri()).getPath().replaceFirst(".*/biomic/", "");
            return facts.get(file + ":" + unit.getLineMap().getLineNumber(start(unit, method.getLeaf())) + "/" + e.getSimpleName());
        }
        if (!(e.getEnclosingElement() instanceof TypeElement te)) return null;
        return facts.get(te.getQualifiedName() + "#" + e.getSimpleName());
    }

    static boolean isLiteral(String s) { return "true".equals(s) || "false".equals(s) || "null".equals(s); }

    /** The folded source text of an expression, or null when nothing about it folds. */
    static String fold(TreePath path, Map<Element, String> constants, String src) {
        Tree t = path.getLeaf();
        CompilationUnitTree unit = path.getCompilationUnit();
        if (t instanceof ParenthesizedTree p) {
            String inner = fold(new TreePath(path, p.getExpression()), constants, src);
            if (inner == null) return null;
            // Parentheses that only held a decided value or a plain name go with it.
            boolean changed = !strip(inner).equals(strip(source(unit, src, p.getExpression())));
            return changed && inner.matches("-?[\\w.]+") ? inner : "(" + inner + ")";
        }
        if (t instanceof LiteralTree) return source(unit, src, t);
        if (t instanceof IdentifierTree || t instanceof MemberSelectTree) {
            Element e = trees.getElement(path);
            if (e == null) return source(unit, src, t);
            String fact = factOf(e);
            boolean dereferenced = path.getParentPath().getLeaf() instanceof MemberSelectTree ms && ms.getExpression() == t;
            if (fact != null && !(dereferenced && "null".equals(fact))
                && (t instanceof IdentifierTree || ((MemberSelectTree) t).getExpression() instanceof IdentifierTree)) return fact;
            if (constants.containsKey(e)) return constants.get(e);
            return source(unit, src, t);
        }
        if (t instanceof UnaryTree u && u.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
            String inner = fold(new TreePath(path, u.getExpression()), constants, src);
            if ("true".equals(inner)) return "false";
            if ("false".equals(inner)) return "true";
            return inner == null ? null : "!" + inner;
        }
        if (t instanceof BinaryTree b) {
            String l = fold(new TreePath(path, b.getLeftOperand()), constants, src);
            String r = fold(new TreePath(path, b.getRightOperand()), constants, src);
            if (l == null || r == null || source(unit, src, b.getLeftOperand()) == null) return source(unit, src, t);
            boolean lPure = pure(new TreePath(path, b.getLeftOperand()));
            String op = src.substring((int) end(unit, b.getLeftOperand()), (int) start(unit, b.getRightOperand()));
            switch (b.getKind()) {
                case CONDITIONAL_AND -> {
                    if ("false".equals(l)) return "false";
                    if ("true".equals(l)) return r;
                    if ("true".equals(r)) return l;
                    if ("false".equals(r) && lPure) return "false";
                    return l + op + r;
                }
                case CONDITIONAL_OR -> {
                    if ("true".equals(l)) return "true";
                    if ("false".equals(l)) return r;
                    if ("false".equals(r)) return l;
                    if ("true".equals(r) && lPure) return "true";
                    return l + op + r;
                }
                case EQUAL_TO, NOT_EQUAL_TO -> {
                    boolean eq = b.getKind() == Tree.Kind.EQUAL_TO;
                    if ("null".equals(l) && "null".equals(r)) return eq ? "true" : "false";
                    if (isLiteral(l) && isLiteral(r) && !"null".equals(l) && !"null".equals(r)) return String.valueOf(l.equals(r) == eq);
                    return l + op + r;
                }
                default -> { return l + op + r; }
            }
        }
        if (t instanceof MethodInvocationTree mi && mi.getArguments().isEmpty()
            && (mi.getMethodSelect() instanceof IdentifierTree || mi.getMethodSelect() instanceof MemberSelectTree ms
                && (ms.getExpression() instanceof IdentifierTree || ms.getExpression() instanceof MemberSelectTree))) {
            Element m = trees.getElement(path);
            if (m != null && m.getEnclosingElement() instanceof TypeElement te) {
                String fact = facts.get(te.getQualifiedName() + "#" + m.getSimpleName() + "()");
                if (fact != null && !(path.getParentPath().getLeaf() instanceof MemberSelectTree && "null".equals(fact))) return fact;
            }
            return source(unit, src, t);
        }
        if (t instanceof ConditionalExpressionTree c) {
            String cond = fold(new TreePath(path, c.getCondition()), constants, src);
            // ?: binds loosest, so either branch can stand where it stood.
            if ("true".equals(cond)) return fold(new TreePath(path, c.getTrueExpression()), constants, src);
            if ("false".equals(cond)) return fold(new TreePath(path, c.getFalseExpression()), constants, src);
            return source(unit, src, t);
        }
        return source(unit, src, t);
    }

    /** No calls, no writes, no object creation: dropping it changes nothing. */
    static boolean pure(TreePath expression) {
        boolean[] impure = {false};
        new TreePathScanner<Void, Void>() {
            @Override public Void visitMethodInvocation(MethodInvocationTree t, Void v) { impure[0] = true; return null; }
            @Override public Void visitNewClass(NewClassTree t, Void v) { impure[0] = true; return null; }
            @Override public Void visitNewArray(NewArrayTree t, Void v) { impure[0] = true; return null; }
            @Override public Void visitAssignment(AssignmentTree t, Void v) { impure[0] = true; return null; }
            @Override public Void visitCompoundAssignment(CompoundAssignmentTree t, Void v) { impure[0] = true; return null; }
            @Override public Void visitUnary(UnaryTree t, Void v) { if (t.getKind().name().contains("CREMENT")) impure[0] = true; return super.visitUnary(t, v); }
        }.scan(expression, null);
        return !impure[0];
    }

    /** An if statement replaced by the branch that runs (its block's statements inlined), or removed. */
    static Edit replaceStatement(CompilationUnitTree unit, String src, TreePath ifPath, StatementTree keep) {
        Tree ifTree = ifPath.getLeaf();
        long s = start(unit, ifTree), e = end(unit, ifTree);
        boolean inBlock = ifPath.getParentPath().getLeaf() instanceof BlockTree;
        if (keep == null) {
            // "else if (false) ..." with nothing after: the else goes too.
            if (ifPath.getParentPath().getLeaf() instanceof IfTree outer && outer.getElseStatement() == ifTree)
                return new Edit(end(unit, outer.getThenStatement()), e, "");
            if (!inBlock) return new Edit(s, e, ";");
            int ls = lineStart(src, (int) s);
            int le = src.indexOf('\n', (int) e);
            if (src.substring(ls, (int) s).isBlank() && le >= 0 && src.substring((int) e, le).isBlank()) return new Edit(ls, le + 1, "");
            return new Edit(s, e, "");
        }
        if (inBlock && abrupt(keep)) {
            // Whatever follows in the block can no longer run.
            BlockTree parent = (BlockTree) ifPath.getParentPath().getLeaf();
            List<? extends StatementTree> rest = parent.getStatements();
            int at = rest.indexOf(ifTree);
            if (at >= 0 && at < rest.size() - 1) e = end(unit, rest.get(rest.size() - 1));
            String text = keep instanceof BlockTree b && !declaresLocals(b) && !b.getStatements().isEmpty()
                ? src.substring((int) start(unit, b.getStatements().get(0)), (int) end(unit, b.getStatements().get(b.getStatements().size() - 1)))
                : src.substring((int) start(unit, keep), (int) end(unit, keep));
            return new Edit(s, e, text);
        }
        if (keep instanceof BlockTree block && inBlock && !declaresLocals(block)) {
            if (block.getStatements().isEmpty()) return replaceStatement(unit, src, ifPath, null);
            long bs = start(unit, block.getStatements().get(0)), be = end(unit, block.getStatements().get(block.getStatements().size() - 1));
            String body = src.substring((int) bs, (int) be);
            int ifIndent = (int) s - lineStart(src, (int) s), innerIndent = (int) bs - lineStart(src, (int) bs);
            if (src.substring(lineStart(src, (int) bs), (int) bs).isBlank() && innerIndent > ifIndent) {
                String strip = " ".repeat(innerIndent - ifIndent);
                body = body.replace("\n" + strip, "\n");
            }
            return new Edit(s, e, body);
        }
        return new Edit(s, e, src.substring((int) start(unit, keep), (int) end(unit, keep)));
    }

    static boolean abrupt(StatementTree st) {
        if (st instanceof ReturnTree || st instanceof ThrowTree) return true;
        return st instanceof BlockTree b && !b.getStatements().isEmpty() && abrupt(b.getStatements().get(b.getStatements().size() - 1));
    }

    static Edit removeLine(String src, long s, long e) {
        int ls = lineStart(src, (int) s);
        int le = src.indexOf('\n', (int) e);
        if (src.substring(ls, (int) s).isBlank() && le >= 0 && src.substring((int) e, le).isBlank()) return new Edit(ls, le + 1, "");
        return new Edit(s, e, "");
    }

    static boolean declaresLocals(BlockTree block) {
        for (StatementTree st : block.getStatements()) if (st instanceof VariableTree || st instanceof ClassTree) return true;
        return false;
    }

    static int lineStart(String s, int pos) { int i = pos; while (i > 0 && s.charAt(i - 1) != '\n') i--; return i; }
    static long start(CompilationUnitTree u, Tree t) { return positions.getStartPosition(u, t); }
    static long end(CompilationUnitTree u, Tree t) { return positions.getEndPosition(u, t); }
    static String source(CompilationUnitTree u, String src, Tree t) {
        long s = start(u, t), e = end(u, t);
        return s < 0 || e < s ? null : src.substring((int) s, (int) e); // generated trees have no text
    }
    static String strip(String s) { return s == null ? "" : s.replaceAll("\\s+", ""); }
    static String where(CompilationUnitTree u, Tree t) {
        return new File(u.getSourceFile().toUri()).getName() + ":" + u.getLineMap().getLineNumber(start(u, t));
    }
}
