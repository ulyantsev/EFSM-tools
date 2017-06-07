package continuous_trace_builders;

import continuous_trace_builders.parameters.Parameter;
import continuous_trace_builders.parameters.RealParameter;
import continuous_trace_builders.parameters.SegmentsParameter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Created by Dmitry on 07-Jun-17.
 */
public class FairnessConstraintGenerator {
    private final static int MIN_LEN = 5;
    private final static int MIN_VISIT_COUNT = 5;
    private final static double REL_ERROR = 1. / 20;

    private static void initIntGroups(List<Parameter> inputs, List<List<Parameter>> grouping, List<List<Integer>> groups, List<List<Integer>> paramIndexToGroup) {
        Map<Parameter, Integer> index = new HashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            index.put(inputs.get(i), i);
            paramIndexToGroup.add(null);
        }
        for (List<Parameter> group: grouping) {
            List<Integer> intGroup = new ArrayList<>();
            for (Parameter param : group) {
                Integer ind = index.get(param);
                intGroup.add(ind);
                paramIndexToGroup.set(ind, intGroup);
            }
            groups.add(intGroup);
        }
        for (int i = 0; i < inputs.size(); i++) {
            if (paramIndexToGroup.get(i) == null) {
                List<Integer> intGroup = new ArrayList<>(1);
                intGroup.add(i);
                groups.add(intGroup);
                paramIndexToGroup.set(i, intGroup);
            }
        }
    }

    private static int get(Dataset ds, List<double[]> trace, int traceElement, Parameter par) {
        return par.traceNameIndex(ds.get(trace.get(traceElement), par));
    }

    private static Counter[][] collectCounts(Dataset ds, Parameter po, List<List<Integer>> groups, List<Parameter> inputs) {
        int outValsCount = po.valueCount();
        @SuppressWarnings("unchecked")
        Counter[][] inputsCount = new Counter[outValsCount][outValsCount];
        int groupsSize = groups.size();
        for (int i = 0; i < outValsCount; i++) {
            for (int j = 0; j < outValsCount; j++) {
                inputsCount[i][j] = new Counter(groups, inputs);
            }
        }
        int inputsSize = inputs.size();
        int[] vals = new int[inputsSize];
        for (List<double[]> trace : ds.values) {
            int curOutInterval = get(ds, trace, 0, po);
            int start = 1;
            while (start < trace.size() && get(ds, trace, start, po) == curOutInterval) {
                start++;
            }
            int prevOutInterval = curOutInterval;
            curOutInterval = get(ds, trace, start, po);
            Map<List<Integer>, Integer>[] cnt = new HashMap[groupsSize];
            for (int i = 0; i < cnt.length; i++) {
                cnt[i] = new HashMap<>();
            }
            for (int i = start; i < trace.size(); i++) {
                int iOutInterval = get(ds, trace, i, po);
                if (iOutInterval != curOutInterval) {
                    inputsCount[prevOutInterval][iOutInterval].add(cnt);
                    cnt = new HashMap[groupsSize];
                    for (int k = 0; k < cnt.length; k++) {
                        cnt[k] = new HashMap<>();
                    }
                    prevOutInterval = curOutInterval;
                    curOutInterval = iOutInterval;
                }
                for (int j = 0; j < inputsSize; j++) {
                    vals[j] = get(ds, trace, i, inputs.get(j));
                }
                for (int j = 0; j < groupsSize; j++) {
                    List<Integer> group = groups.get(j);
                    List<Integer> key = new ArrayList<>(group.size());
                    for (int item : group) {
                        key.add(vals[item]);
                    }
                    cnt[j].put(key, cnt[j].getOrDefault(key, 0) + 1);
                }
            }
        }
        return inputsCount;
    }


    static List<String> generateFairnessConstraints(Configuration conf, Dataset ds, List<List<Parameter>> grouping) {
        List<Parameter> inputs = conf.inputParameters;
        int inputsSize = inputs.size();
        List<List<Integer>> groups = new ArrayList<>(inputsSize); // groups.toInt() + single elements
        List<List<Integer>> paramIndexToGroup = new ArrayList<>(inputsSize);
        initIntGroups(inputs, grouping, groups, paramIndexToGroup);
        List<String> constraints = new ArrayList<>();

        for (Parameter po : conf.outputParameters) {
            if (po instanceof RealParameter || po instanceof SegmentsParameter) {
                Counter[][] inputsCount = collectCounts(ds, po, groups, inputs);
                int outValsCount = po.valueCount();
                for (int i = 0; i < outValsCount; i++) {
                    for (int j = 0; j < outValsCount; j++) {
                        if (j != i + 2)
                            continue;
                        int mid = i + 1;
                        for (ControlParameter control : getControlParameters(inputsCount[i][j], inputsCount[j][i])) {
                            constraints.add("FAIRNESS !(" + po.traceName() + " = " + mid + control.keyToString(groups, inputs, true) + ")");
                            constraints.add("FAIRNESS !(" + po.traceName() + " = " + mid + control.keyToString(groups, inputs, false) + ")");
                        }
                    }
                }
            }
        }
        return constraints;
    }

    private static class ControlParameter {
        int group;
        List<Integer> minusKey, plusKey;

        ControlParameter(int group, List<Integer> plusKey, List<Integer> minusKey) {
            this.group = group;
            this.minusKey = minusKey;
            this.plusKey = plusKey;
        }

        String keyToString(List<List<Integer>> groups, List<Parameter> inputs, boolean isPlus) {
            StringBuilder res = new StringBuilder();
            List<Integer> list = isPlus ? plusKey : minusKey;
            for (int i = 0; i < list.size(); i++) {
                res.append(" & " + inputs.get(groups.get(group).get(i)).traceName() + "=" + list.get(i));
            }
            return res.toString();
        }
    }

    static List<ControlParameter> getControlParameters(Counter c1, Counter c2) {
        if (c1.vals.size() < MIN_VISIT_COUNT || c2.vals.size() < MIN_VISIT_COUNT)
            return new ArrayList<>();
        List<ControlParameter> res = new ArrayList<>();
        for (int i = 0; i < c1.groupsCount; i++) {
            List<List<Integer>> gKeys = new ArrayList<>(c1.keys[i]);
            for (int j = 0; j < gKeys.size(); j++) {
                for (int k = 0; k < gKeys.size(); k++) {
                    if (j == k) {
                        continue;
                    }
                    List<Integer> k1 = gKeys.get(j);
                    List<Integer> k2 = gKeys.get(k);

                    List<Integer> dif1 = new ArrayList<>();
                    List<Integer> dif2 = new ArrayList<>();
                    for (Map<List<Integer>, Integer>[] row : c1.vals) {
                        dif1.add(row[i].getOrDefault(k1, 0) - row[i].getOrDefault(k2, 0));
                    }
                    for (Map<List<Integer>, Integer>[] row : c2.vals) {
                        dif2.add(row[i].getOrDefault(k2, 0) - row[i].getOrDefault(k1, 0));
                    }
                    Collections.sort(dif1);
                    Collections.sort(dif2);
                    int mid = dif1.get(dif1.size() / 2);
                    if (mid > MIN_LEN && dif1.get(0) >= mid * (1 - REL_ERROR) && dif1.get(dif1.size()-1) <= mid * (1 + REL_ERROR)
                            && dif2.get(0) >= mid * (1 - REL_ERROR) && dif2.get(dif2.size()-1) <= mid * (1 + REL_ERROR)) {
                        res.add(new ControlParameter(i, k1, k2));
                    }
                }
            }
        }
        return res;
    }


    private static class Counter {
        final List<Map<List<Integer>, Integer>[]> vals = new ArrayList<>();
        final List<List<Integer>>[] keys;
        final int groupsCount;

        private void go(int ind, int[] counts, Integer[] cur, List<List<Integer>> ans) {
            if (ind == counts.length) {
                List<Integer> res = new ArrayList<>(ind);
                for (Integer x : cur)
                    res.add(x);
                ans.add(res);
                return;
            }
            for (int i = 0; i < counts[ind]; i++) {
                cur[ind] = i;
                go(ind + 1, counts, cur, ans);
            }
        }

        private List<List<Integer>> kollectKeys(List<Integer> group, List<Parameter> parameters) {
            List<List<Integer>> ans = new ArrayList<>();
            Integer[] cur = new Integer[group.size()];
            int[] counts= new int[group.size()];
            for (int i = 0; i < counts.length; i++) {
                counts[i] = parameters.get(i).valueCount();
            }
            go(0, counts, cur, ans);
            return ans;
        }

        public Counter(List<List<Integer>> groups, List<Parameter> parameters) {
            groupsCount = groups.size();
            keys = new List[groupsCount];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = kollectKeys(groups.get(i), parameters);
            }
        }

        void add(Map<List<Integer>, Integer>[] inputsCount) {
            vals.add(inputsCount);
        }

    }

}
