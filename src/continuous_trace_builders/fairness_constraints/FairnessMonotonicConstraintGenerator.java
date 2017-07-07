package continuous_trace_builders.fairness_constraints;

import continuous_trace_builders.Configuration;
import continuous_trace_builders.Dataset;
import continuous_trace_builders.parameters.Parameter;
import continuous_trace_builders.parameters.RealParameter;
import continuous_trace_builders.parameters.SegmentsParameter;

import java.util.*;
import static continuous_trace_builders.fairness_constraints.Helper.*;

/**
 * Created by Dmitry on 07-Jun-17.
 */
public class FairnessMonotonicConstraintGenerator {
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

        for (Parameter po : conf.outputParameters) {
            int outValsCount = po.valueCount();
            for (ControlParameter control : getControlParameters(ds, po, groups, keys, inputs)) {
                for (int pos = 0; pos < outValsCount - 1; pos++) {
                    constraints.add("FAIRNESS !(output_" + po.traceName() + " = " + pos + control.keyToString(groups, inputs, true) + ")");
                    constraints.add("FAIRNESS !(output_" + po.traceName() + " = " + (pos + 1) + control.keyToString(groups, inputs, false) + ")");
                }
            }
        }
        return constraints;
    }

    static List<ControlParameter> getControlParameters(Dataset ds, Parameter po, List<List<Integer>> groups, List<List<List<Integer>>> keys, List<Parameter> inputs) {
        List<List<Boolean>> canPlus = new ArrayList<>();
        List<List<Boolean>> canMinus = new ArrayList<>();
        List<List<Boolean>> canStay = new ArrayList<>();
        List<Map<List<Integer>, Integer>> keyToPos = new ArrayList<>();
        for (int group = 0; group < groups.size(); group++) {
            List<Boolean> curCanPlus = new ArrayList<>();
            List<Boolean> curCanMinus = new ArrayList<>();
            List<Boolean> curCanStay = new ArrayList<>();
            List<List<Integer>> groupKeys = keys.get(group);
            Map<List<Integer>, Integer> map = new HashMap<>();
            for (int i = 0; i < groupKeys.size(); i++) {
                curCanPlus.add(true);
                curCanMinus.add(true);
                curCanStay.add(true);
                map.put(groupKeys.get(i), i);
            }
            canPlus.add(curCanPlus);
            canMinus.add(curCanMinus);
            canStay.add(curCanStay);
            keyToPos.add(map);
        }
        double minOut = ds.get(ds.values.get(0).get(0), po), maxOut = minOut;
        for (List<double[]> trace : ds.values) {
            for (double[] elem : trace) {
                double val = ds.get(elem, po);
                minOut = Math.min(minOut, val);
                maxOut = Math.max(maxOut, val);
            }
        }
        for (List<double[]> trace : ds.values) {
            for (int i = 0; i < trace.size() - 1; i++) {
                double outDif = ds.get(trace.get(i+1), po) - ds.get(trace.get(i), po);
                for (int j = 0; j < groups.size(); j++) {
                    List<Integer> group = groups.get(j);
                    List<Integer> key = new ArrayList<>(group.size());
                    for (int item : group) {
                        key.add(get(ds, trace, i, inputs.get(item)));
                    }
                    int index = keyToPos.get(j).get(key);
                    if (outDif < 0) {
                        canPlus.get(j).set(index, false);
                        canStay.get(j).set(index, false);
                    } else if (outDif == 0) {
                        if (ds.get(trace.get(i), po) != maxOut) {
                            canPlus.get(j).set(index, false);
                        }
                        if (ds.get(trace.get(i), po) != minOut) {
                            canMinus.get(j).set(index, false);
                        }
                    } else {
                        canStay.get(j).set(index, false);
                        canMinus.get(j).set(index, false);
                    }
                }
            }
        }
        List<ControlParameter> res = new ArrayList<>();
        outer:
        for (int group = 0; group < groups.size(); group++) {
            List<List<Integer>> groupKeys = keys.get(group);
            List<Integer> plusKey = null;
            List<Integer> minusKey = null;
            for (int i = 0; i < groupKeys.size(); i++) {
                boolean stay = canStay.get(group).get(i);
                boolean plus = canPlus.get(group).get(i);
                boolean minus = canMinus.get(group).get(i);
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
                if (po.traceNameIndex(minOut) > po.traceNameIndex(maxOut)) {
                    List<Integer> temp = plusKey;
                    plusKey = minusKey;
                    minusKey = temp;
                }
                res.add(new ControlParameter(group, plusKey, minusKey));
            }
        }
        return res;
    }
}
