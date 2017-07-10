package continuous_trace_builders.fairness_constraints;

import continuous_trace_builders.Configuration;
import continuous_trace_builders.Dataset;
import continuous_trace_builders.parameters.Parameter;
import continuous_trace_builders.parameters.RealParameter;
import continuous_trace_builders.parameters.SegmentsParameter;

import java.util.*;
import java.util.function.Supplier;

import static continuous_trace_builders.fairness_constraints.Helper.*;

/**
 * Created by Dmitry on 07-Jun-17.
 */
public class MonotonicFairnessConstraintGenerator {
    public static List<String> generateFairnessConstraints(Configuration conf, Dataset ds, List<List<Parameter>> grouping) {
        List<Parameter> inputs = conf.inputParameters;
        int inputsSize = inputs.size();
        List<List<Integer>> groups = new ArrayList<>(inputsSize); // groups.toInt() + single elements
        List<List<Integer>> paramIndexToGroup = new ArrayList<>(inputsSize);
        Helper.initIntGroups(inputs, grouping, groups, paramIndexToGroup);
        List<String> constraints = new ArrayList<>();
        List<List<List<Integer>>> keys = new ArrayList<>();
        for (List<Integer> group : groups) {
            keys.add(collectKeys(group, inputs));
        }

        List<ControlParameter>[] controlParameters = getControlParameters(ds, conf.outputParameters, groups, keys, inputs);
        for (int out = 0; out < conf.outputParameters.size(); out++) {
            Parameter po = conf.outputParameters.get(out);
            int outValsCount = po.valueCount();
            for (ControlParameter control : controlParameters[out]) {
                for (int pos = 0; pos < outValsCount - 1; pos++) {
                    constraints.add("FAIRNESS !(output_" + po.traceName() + " = " + pos + control.keyToString(groups, inputs, true) + ")");
                    constraints.add("FAIRNESS !(output_" + po.traceName() + " = " + (pos + 1) + control.keyToString(groups, inputs, false) + ")");
                }
            }
        }
        return constraints;
    }




    private static List<ControlParameter>[] getControlParameters(Dataset ds, List<Parameter> outputParameters,
                                                                 List<List<Integer>> groups, List<List<List<Integer>>> keys,
                                                                 List<Parameter> inputs) {
        // Init
        int outputsCount = outputParameters.size();
        int groupsCount = groups.size();

        Supplier<boolean[][][]> initBoolArray = () -> {
            boolean[][][] res = new boolean[outputsCount][groupsCount][];
            for (int output = 0; output < outputsCount; output++) {
                for (int group = 0; group < groups.size(); group++) {
                    List<List<Integer>> groupKeys = keys.get(group);
                    boolean[] curRes = new boolean[groupKeys.size()];
                    Arrays.fill(curRes, true);
                    res[output][group] = curRes;
                }
            }
            return res;
        };
        @SuppressWarnings("unchecked")
        Map<List<Integer>, Integer>[] keyToPos = new Map[groupsCount];
        for (int group = 0; group < groupsCount; group++) {
            Map<List<Integer>, Integer> map = new HashMap<>();
            List<List<Integer>> groupKeys = keys.get(group);
            for (int i = 0; i < groupKeys.size(); i++) {
                map.put(groupKeys.get(i), i);
            }
            keyToPos[group] = map;
        }
        boolean[][][] canPlus = initBoolArray.get(), canStay = initBoolArray.get(), canMinus = initBoolArray.get();

        double[] minOut = new double[outputsCount];
        double[] maxOut = new double[outputsCount];

        initMinMax(ds, outputParameters, outputsCount, minOut, maxOut);

        findContradictions(ds, outputParameters, groups, inputs, keyToPos, canPlus, canStay, canMinus, minOut, maxOut);

        return collectConstraints(outputParameters, groups, keys, canPlus, canStay, canMinus, minOut, maxOut);
    }

    private static void initMinMax(Dataset ds, List<Parameter> outputParameters, int outputsCount, double[] minOut, double[] maxOut) {
        for (int out = 0; out < outputsCount; out++) {
            minOut[out] = maxOut[out] = ds.get(ds.values.get(0).get(0), outputParameters.get(out));
        }
        for (List<double[]> trace : ds.values) {
            for (double[] elem : trace) {
                for (int out = 0; out < outputsCount; out++) {
                    double val = ds.get(elem, outputParameters.get(out));
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
            Parameter po = outputParameters.get(out);
            outer:
            for (int group = 0; group < groups.size(); group++) {
                List<List<Integer>> groupKeys = keys.get(group);
                List<Integer> plusKey = null;
                List<Integer> minusKey = null;
                for (int i = 0; i < groupKeys.size(); i++) {
                    boolean stay = canStay[out][group][i];
                    boolean plus = canPlus[out][group][i];
                    boolean minus = canMinus[out][group][i];
                    if (!stay && !plus && !minus) {
                        continue outer;
                    }
                    if (!stay && plus && !minus) {
                        plusKey = groupKeys.get(i);
                    }
                    if (!stay && !plus && minus) {
                        minusKey = groupKeys.get(i);
                    }
                }
                if (plusKey != null && minusKey != null) {
                    if (po.traceNameIndex(minOut[out]) > po.traceNameIndex(maxOut[out])) {
                        List<Integer> temp = plusKey;
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
                                           double[] minOut, double[] maxOut) {
        for (List<double[]> trace : ds.values) {
            for (int i = 0; i < trace.size() - 1; i++) {
                for (int j = 0; j < groups.size(); j++) {
                    List<Integer> group = groups.get(j);
                    List<Integer> key = new ArrayList<>(group.size());
                    for (int item : group) {
                        key.add(get(ds, trace, i, inputs.get(item)));
                    }
                    int index = keyToPos[j].get(key);
                    for (int out = 0; out < outputParameters.size(); out++) {
                        Parameter po = outputParameters.get(out);
                        double outDif = ds.get(trace.get(i+1), po) - ds.get(trace.get(i), po);
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
