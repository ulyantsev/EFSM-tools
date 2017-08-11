package continuous_trace_builders.fairness_constraints;

import continuous_trace_builders.Configuration;
import continuous_trace_builders.Dataset;
import continuous_trace_builders.parameters.Parameter;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static continuous_trace_builders.fairness_constraints.Helper.*;

public class MonotonicFairnessConstraintGenerator {
    public static List<String> generateFairnessConstraints(Configuration conf, Dataset ds,
                                                           List<List<Parameter>> grouping) throws IOException {
        final List<Parameter> inputs = conf.inputParameters;
        final int inputsSize = inputs.size();
        final List<List<Integer>> groups = new ArrayList<>(inputsSize); // groups.toInt() + single elements
        final List<List<Integer>> paramIndexToGroup = new ArrayList<>(inputsSize);
        Helper.initIntGroups(inputs, grouping, groups, paramIndexToGroup);
        final List<String> constraints = new ArrayList<>();
        final List<List<List<Integer>>> keys = new ArrayList<>();
        for (List<Integer> group : groups) {
            keys.add(collectKeys(group, inputs));
        }

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
        final int outputsCount = outputParameters.size();
        final int groupsCount = groups.size();

        Supplier<boolean[][][]> initBoolArray = () -> {
            boolean[][][] res = new boolean[outputsCount][groupsCount][];
            for (int output = 0; output < outputsCount; output++) {
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
        final Map<List<Integer>, Integer>[] keyToPos = new Map[groupsCount];
        for (int group = 0; group < groupsCount; group++) {
            final Map<List<Integer>, Integer> map = new HashMap<>();
            final List<List<Integer>> groupKeys = keys.get(group);
            for (int i = 0; i < groupKeys.size(); i++) {
                map.put(groupKeys.get(i), i);
            }
            keyToPos[group] = map;
        }
        final boolean[][][] canPlus = initBoolArray.get(), canStay = initBoolArray.get(), canMinus = initBoolArray.get();

        final double[] minOut = new double[outputsCount];
        final double[] maxOut = new double[outputsCount];

        initMinMax(ds, outputParameters, outputsCount, minOut, maxOut);

        findContradictions(ds, outputParameters, groups, inputs, keyToPos, canPlus, canStay, canMinus, minOut, maxOut);

        return collectConstraints(outputParameters, groups, keys, canPlus, canStay, canMinus, minOut, maxOut);
    }

    private static void initMinMax(Dataset ds, List<Parameter> outputParameters, int outputsCount, double[] minOut,
                                   double[] maxOut) throws IOException {
        for (int out = 0; out < outputsCount; out++) {
            minOut[out] = maxOut[out] = ds.get(ds.reader().next().get(0), outputParameters.get(out));
        }
        final Dataset.Reader reader = ds.reader();
        while (reader.hasNext()) {
            final List<double[]> trace = reader.next();
            for (double[] elem : trace) {
                for (int out = 0; out < outputsCount; out++) {
                    final double val = ds.get(elem, outputParameters.get(out));
                    minOut[out] = Math.min(minOut[out], val);
                    maxOut[out] = Math.max(maxOut[out], val);
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
                List<List<Integer>> groupKeys = keys.get(group);
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
