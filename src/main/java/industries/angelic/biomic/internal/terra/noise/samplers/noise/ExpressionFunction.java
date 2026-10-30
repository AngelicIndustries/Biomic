/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.internal.terra.noise.samplers.noise;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import com.dfsek.paralithic.Expression;
import com.dfsek.paralithic.eval.parser.Parser;
import com.dfsek.paralithic.eval.parser.Scope;
import com.dfsek.paralithic.eval.tokenizer.ParseException;
import com.dfsek.paralithic.functions.Function;

import industries.angelic.biomic.internal.terra.noise.paralithic.noise.SeedContext;

/**
 * NoiseSampler implementation using a Paralithic expression.
 */
public class ExpressionFunction extends NoiseFunction {

    private static final boolean PROFILE_EXPRESSIONS = Boolean.getBoolean("biomic.profileExpressions");
    private static final ConcurrentHashMap<String, LongAdder> PROFILE_COUNTS = new ConcurrentHashMap<>();

    private final Expression expression;
    private final String profileKey;
    private final ThreadLocal<EvaluationState> evaluationState = ThreadLocal.withInitial(EvaluationState::new);

    public ExpressionFunction(Map<String, Function> functions, String eq, Map<String, Double> vars)
        throws ParseException {
        Parser p = new Parser();
        Scope scope = new Scope();

        scope.addInvocationVariable("x");
        scope.addInvocationVariable("y");
        scope.addInvocationVariable("z");

        vars.forEach(scope::create);

        functions.forEach(p::registerFunction);

        expression = p.parse(eq, scope);
        profileKey = compact(eq);
        frequency = 1;
    }

    @Override
    public double getNoiseRaw(long seed, double x, double y) {
        recordProfileCall();
        EvaluationState state = evaluationState.get();
        state.context.setSeed(seed);
        state.arguments[0] = x;
        state.arguments[1] = 0.0D;
        state.arguments[2] = y;
        return expression.evaluate(state.context, state.arguments);
    }

    @Override
    public double getNoiseRaw(long seed, double x, double y, double z) {
        recordProfileCall();
        EvaluationState state = evaluationState.get();
        state.context.setSeed(seed);
        state.arguments[0] = x;
        state.arguments[1] = y;
        state.arguments[2] = z;
        return expression.evaluate(state.context, state.arguments);
    }

    public static void resetProfile() {
        if (PROFILE_EXPRESSIONS) PROFILE_COUNTS.clear();
    }

    public static String profileReport(int limit) {
        if (!PROFILE_EXPRESSIONS) return null;
        List<Map.Entry<String, LongAdder>> entries = new ArrayList<>(PROFILE_COUNTS.entrySet());
        entries.sort(
            Comparator.comparingLong(
                (Map.Entry<String, LongAdder> entry) -> entry.getValue()
                    .sum())
                .reversed());
        StringBuilder report = new StringBuilder("Top interpreted Origen expressions:");
        for (int index = 0; index < Math.min(limit, entries.size()); index++) {
            Map.Entry<String, LongAdder> entry = entries.get(index);
            report.append('\n')
                .append(
                    entry.getValue()
                        .sum())
                .append(" x ")
                .append(entry.getKey());
        }
        return report.toString();
    }

    private void recordProfileCall() {
        if (PROFILE_EXPRESSIONS) PROFILE_COUNTS.computeIfAbsent(profileKey, ignored -> new LongAdder())
            .increment();
    }

    private static String compact(String expression) {
        return expression.replace(" ", "")
            .replace("\t", "")
            .replace("\r", "")
            .replace("\n", "");
    }

    private static final class EvaluationState {

        private final SeedContext context = new SeedContext(0L);
        private final double[] arguments = new double[3];
    }

}
