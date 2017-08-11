package continuous_trace_builders.fairness_constraints;

import continuous_trace_builders.Configuration;
import continuous_trace_builders.Dataset;
import continuous_trace_builders.parameters.Parameter;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static continuous_trace_builders.fairness_constraints.Helper.*;

public class MonotonicFairnessConstraintGenerator {
    public static List<String> generateFairnessConstraints(Configuration conf, Dataset ds,
                                                           List<List<Parameter>> grouping) throws IOException {
        final List<Parameter> inputs = conf.inputParameters;
        final List<List<Integer>> groups = new ArrayList<>(inputs.size()); // groups.toInt() + single elements
        final List<List<Integer>> paramIndexToGroup = new ArrayList<>(inputs.size());
        Helper.initIntGroups(inputs, grouping, groups, paramIndexToGroup);
        final List<String> constraints = new ArrayList<>();
        final List<List<List<Integer>>> keys = groups.stream().map(group -> collectKeys(group, inputs))
                .collect(Collectors.toList());

        final List<ControlParameter>[] controlParameters = getControlParameters(ds, conf.outputParameters, groups, keys,
                inputs);
        for (int out = 0; out < conf.outputParameters.size(); out++) {
            final Parameter po = conf.outputParameters.get(out);
            final int outValsCount = po.valueCount();
            for (ControlParameter control : controlParameters[out]) {
                for (int pos = 0; pos < outValsCount - 1; pos++) {
                    constraints.add("FAIRNESS !(output_" + po.traceName() + " = " + pos
                            + control.keyToString(groups, inputs, true) + ")");
                    constraints.add("FAIRNESS !(output_" + po.traceName() + " = " + (pos + 1)
                            + control.keyToString(groups, inputs, false) + ")");
                }
            }
        }
        return constraints;
    }

    private static List<ControlParameter>[] getControlParameters(Dataset ds, List<Parameter> outputParameters,
                                                                 List<List<Integer>> groups, List<List<List<Integer>>> keys,
                                                                 List<Parameter> inputs) throws IOException {
        // Init
        Supplier<boolean[][][]> initBoolArray = () -> {
            final boolean[][][] res = new boolean[outputParameters.size()][groups.size()][];
            for (int output = 0; output < outputParameters.size(); output++) {
                for (int group = 0; group < groups.size(); group++) {
                    final List<List<Integer>> groupKeys = keys.get(group);
                    final boolean[] curRes = new boolean[groupKeys.size()];
                    Arrays.fill(curRes, true);
                    res[output][group] = curRes;
                }
            }
            return res;
        };
        @SuppressWarnings("unchecked")
        final Map<List<Integer>, Integer>[] keyToPos = new Map[groups.size()];
        for (int group = 0; group < groups.size(); group++) {
            final Map<List<Integer>, Integer> map = new HashMap<>();
            final List<List<Integer>> groupKeys = keys.get(group);
            for (int i = 0; i < groupKeys.size(); i++) {
                map.put(groupKeys.get(i), i);
            }
            keyToPos[group] = map;
        }
        final boolean[][][] canPlus = initBoolArray.get(), canStay = initBoolArray.get(), canMinus = initBoolArray.get();

        final double[] minOut = new double[outputParameters.size()];
        final double[] maxOut = new double[outputParameters.size()];

        initMinMax(ds, outputParameters, minOut, maxOut);

        findContradictions(ds, outputParameters, groups, inputs, keyToPos, canPlus, canStay, canMinus, minOut, maxOut);

        return collectConstraints(outputParameters, groups, keys, canPlus, canStay, canMinus, minOut, maxOut);
    }

    private static void initMinMax(Dataset ds, List<Parameter> outputParameters, double[] minOut, double[] maxOut)
            throws IOException {
        Arrays.fill(minOut, Double.POSITIVE_INFINITY);
        Arrays.fill(maxOut, Double.NEGATIVE_INFINITY);
        final Dataset.Reader reader = ds.reader();
        while (reader.hasNext()) {
            for (double[] elem : reader.next()) {
                for (int i = 0; i < outputParameters.size(); i++) {
                    final double val = ds.get(elem, outputParameters.get(i));
                    minOut[i] = Math.min(minOut[i], val);
                    maxOut[i] = Math.max(maxOut[i], val);
                }
            }
        }
    }

    private static List<ControlParameter>[] collectConstraints(
            List<Parameter> outputParameters, List<List<Integer>> groups, List<List<List<Integer>>> keys,
            boolean[][][] canPlus, boolean[][][] canStay, boolean[][][] canMinus,
            double[] minOut, double[] maxOut) {
        @SuppressWarnings("unchecked")
        List<ControlParameter>[] res = new List[outputParameters.size()];
        for (int out = 0; out < outputParameters.size(); out++) {
            res[out] = new ArrayList<>();
            final Parameter po = outputParameters.get(out);
            outer: for (int group = 0; group < groups.size(); group++) {
                final List<List<Integer>> groupKeys = keys.get(group);
                List<Integer> plusKey = null;
                List<Integer> minusKey = null;
                for (int i = 0; i < groupKeys.size(); i++) {
                    final boolean stay = canStay[out][group][i];
                    final boolean plus = canPlus[out][group][i];
                    final boolean minus = canMinus[out][group][i];
                    if (!stay && !plus && !minus) {
                        continue outer;
                    }
                    if (!stay && plus && !minus) {
                        plusKey = groupKeys.get(i);
                    }
                    if (!stay && !plus) {
                        minusKey = groupKeys.get(i);
                    }
                }
                if (plusKey != null && minusKey != null) {
                    if (po.traceNameIndex(minOut[out]) > po.traceNameIndex(maxOut[out])) {
                        final List<Integer> temp = plusKey;
                        plusKey = minusKey;
                        minusKey = temp;
                    }
                    res[out].add(new ControlParameter(group, plusKey, minusKey));
                }
            }
        }
        return res;
    }

    private static void findContradictions(Dataset ds, List<Parameter> outputParameters, List<List<Integer>> groups,
                                           List<Parameter> inputs, Map<List<Integer>, Integer>[] keyToPos,
                                           boolean[][][] canPlus, boolean[][][] canStay, boolean[][][] canMinus,
                                           double[] minOut, double[] maxOut) throws IOException {
        final Dataset.Reader reader = ds.reader();
        while (reader.hasNext()) {
            final List<double[]> trace = reader.next();
            for (int i = 0; i < trace.size() - 1; i++) {
                for (int j = 0; j < groups.size(); j++) {
                    final List<Integer> group = groups.get(j);
                    final List<Integer> key = new ArrayList<>(group.size());
                    for (int item : group) {
                        key.add(get(ds, trace, i, inputs.get(item)));
                    }
                    final int index = keyToPos[j].get(key);
                    for (int out = 0; out < outputParameters.size(); out++) {
                        final Parameter po = outputParameters.get(out);
                        final double outDif = ds.get(trace.get(i+1), po) - ds.get(trace.get(i), po);
                        if (outDif < 0) {
                            canPlus[out][j][index] = false;
                            canStay[out][j][index] = false;
                        } else if (outDif == 0) {
                            if (ds.get(trace.get(i), po) != maxOut[out]) {
                                canPlus[out][j][index] = false;
                            }
                            if (ds.get(trace.get(i), po) != minOut[out]) {
                                canMinus[out][j][index] = false;
                            }
                        } else {
                            canStay[out][j][index] = false;
                            canMinus[out][j][index] = false;
                        }
                    }
                }
            }
        }
    }
}
