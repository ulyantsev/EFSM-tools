package continuous_trace_builders.fairness_constraints;

import continuous_trace_builders.Dataset;
import continuous_trace_builders.parameters.Parameter;

import java.util.*;

/**
 * Created by Dmitry on 07-Jul-17.
 */
public class Helper {
    static void initIntGroups(List<Parameter> inputs, List<List<Parameter>> grouping, List<List<Integer>> groups,
                              List<List<Integer>> paramIndexToGroup) {
        final Map<Parameter, Integer> index = new HashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            index.put(inputs.get(i), i);
            paramIndexToGroup.add(null);
        }
        for (List<Parameter> group: grouping) {
            final List<Integer> intGroup = new ArrayList<>();
            for (Parameter param : group) {
                Integer ind = index.get(param);
                intGroup.add(ind);
                paramIndexToGroup.set(ind, intGroup);
            }
            groups.add(intGroup);
        }
        for (int i = 0; i < inputs.size(); i++) {
            if (paramIndexToGroup.get(i) == null) {
                final List<Integer> intGroup = new ArrayList<>(1);
                intGroup.add(i);
                groups.add(intGroup);
                paramIndexToGroup.set(i, intGroup);
            }
        }
    }

    private static void goCollectKeys(int ind, int[] counts, Integer[] cur, List<List<Integer>> ans) {
        if (ind == counts.length) {
            final List<Integer> res = new ArrayList<>(ind);
            Collections.addAll(res, cur);
            ans.add(res);
            return;
        }
        for (int i = 0; i < counts[ind]; i++) {
            cur[ind] = i;
            goCollectKeys(ind + 1, counts, cur, ans);
        }
    }

    static List<List<Integer>> collectKeys(List<Integer> group, List<Parameter> parameters) {
        final List<List<Integer>> ans = new ArrayList<>();
        final Integer[] cur = new Integer[group.size()];
        final int[] counts = new int[group.size()];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = parameters.get(i).valueCount();
        }
        goCollectKeys(0, counts, cur, ans);
        return ans;
    }

    static int get(Dataset ds, List<double[]> trace, int traceElement, Parameter par) {
        return par.traceNameIndex(ds.get(trace.get(traceElement), par));
    }

    static class ControlParameter {
        int group;
        List<Integer> minusKey, plusKey;

        ControlParameter(int group, List<Integer> plusKey, List<Integer> minusKey) {
            this.group = group;
            this.minusKey = minusKey;
            this.plusKey = plusKey;
        }

        String keyToString(List<List<Integer>> groups, List<Parameter> inputs, boolean isPlus) {
            final StringBuilder res = new StringBuilder();
            final List<Integer> list = isPlus ? plusKey : minusKey;
            for (int i = 0; i < list.size(); i++) {
                Parameter param = inputs.get(groups.get(group).get(i));
                res.append(" & CONT_INPUT_").append(param.traceName()).append(" in ")
                        .append(param.nusmvInterval(list.get(i)));
            }
            return res.toString();
        }
    }
}
