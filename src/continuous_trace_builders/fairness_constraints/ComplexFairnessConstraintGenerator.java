package continuous_trace_builders.fairness_constraints;

import continuous_trace_builders.Configuration;
import continuous_trace_builders.Dataset;
import continuous_trace_builders.parameters.Parameter;
import continuous_trace_builders.parameters.RealParameter;
import continuous_trace_builders.parameters.SegmentsParameter;

import java.io.IOException;
import java.util.*;

import static continuous_trace_builders.fairness_constraints.Helper.*;


public class ComplexFairnessConstraintGenerator {
    private final static int MIN_LEN = 5;
    private final static int MIN_VISIT_COUNT = 5;
    private final static double REL_ERROR = 1. / 20;

    @SuppressWarnings("unchecked")
    private static Counter[][] collectCounts(Dataset ds, Parameter po, List<List<Integer>> groups,
                                             List<Parameter> inputs) throws IOException {
        final int outValsCount = po.valueCount();
        final Counter[][] inputsCount = new Counter[outValsCount][outValsCount];
        final int groupsSize = groups.size();
        for (int i = 0; i < outValsCount; i++) {
            for (int j = 0; j < outValsCount; j++) {
                inputsCount[i][j] = new Counter(groups, inputs);
            }
        }
        final Dataset.Reader reader = ds.reader();
        while (reader.hasNext()) {
            final List<double[]> trace = reader.next();
            int curOutInterval = get(ds, trace, 0, po);
            int start = 1;
            while (start < trace.size() && get(ds, trace, start, po) == curOutInterval) {
                start++;
            }
            if (start == trace.size()) {
                continue;
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
                for (int j = 0; j < groupsSize; j++) {
                    List<Integer> group = groups.get(j);
                    List<Integer> key = new ArrayList<>(group.size());
                    for (int item : group) {
                        key.add(get(ds, trace, i, inputs.get(item)));
                    }
                    cnt[j].put(key, cnt[j].getOrDefault(key, 0) + 1);
                }
            }
        }
        return inputsCount;
    }


    public static List<String> generateFairnessConstraints(Configuration conf, Dataset ds,
                                                           List<List<Parameter>> grouping) throws IOException {
        final List<Parameter> inputs = conf.inputParameters;
        final int inputsSize = inputs.size();
        final List<List<Integer>> groups = new ArrayList<>(inputsSize); // groups.toInt() + single elements
        final List<List<Integer>> paramIndexToGroup = new ArrayList<>(inputsSize);
        Helper.initIntGroups(inputs, grouping, groups, paramIndexToGroup);
        final List<String> constraints = new ArrayList<>();

        for (Parameter po : conf.outputParameters) {
            if (po instanceof RealParameter || po instanceof SegmentsParameter) {
                final Counter[][] inputsCount = collectCounts(ds, po, groups, inputs);
                final int outValsCount = po.valueCount();
                for (int i = 0; i < outValsCount; i++) {
                    for (int j = 0; j < outValsCount; j++) {
                        if (j != i + 2) {
                            continue;
                        }
                        final int mid = i + 1;
                        for (ControlParameter control : getControlParameters(inputsCount[i][j], inputsCount[j][i])) {
                            constraints.add("FAIRNESS !(output_" + po.traceName() + " = " + mid
                                    + control.keyToString(groups, inputs, true) + ")");
                            constraints.add("FAIRNESS !(output_" + po.traceName() + " = " + mid
                                    + control.keyToString(groups, inputs, false) + ")");
                        }
                    }
                }
            }
        }
        return constraints;
    }

    private static List<ControlParameter> getControlParameters(Counter c1, Counter c2) {
        if (c1.vals.size() < MIN_VISIT_COUNT || c2.vals.size() < MIN_VISIT_COUNT) {
            return new ArrayList<>();
        }
        final List<ControlParameter> res = new ArrayList<>();
        for (int i = 0; i < c1.groupsCount; i++) {
            final List<List<Integer>> gKeys = new ArrayList<>(c1.keys[i]);
            for (int j = 0; j < gKeys.size(); j++) {
                for (int k = 0; k < gKeys.size(); k++) {
                    if (j == k) {
                        continue;
                    }
                    final List<Integer> k1 = gKeys.get(j);
                    final List<Integer> k2 = gKeys.get(k);

                    final List<Integer> dif1 = new ArrayList<>();
                    final List<Integer> dif2 = new ArrayList<>();
                    for (Map<List<Integer>, Integer>[] row : c1.vals) {
                        dif1.add(row[i].getOrDefault(k1, 0) - row[i].getOrDefault(k2, 0));
                    }
                    for (Map<List<Integer>, Integer>[] row : c2.vals) {
                        dif2.add(row[i].getOrDefault(k2, 0) - row[i].getOrDefault(k1, 0));
                    }
                    Collections.sort(dif1);
                    Collections.sort(dif2);
                    final int mid = dif1.get(dif1.size() / 2);
                    if (mid > MIN_LEN && dif1.get(0) >= mid * (1 - REL_ERROR)
                            && dif1.get(dif1.size()-1) <= mid * (1 + REL_ERROR)
                            && dif2.get(0) >= mid * (1 - REL_ERROR)
                            && dif2.get(dif2.size()-1) <= mid * (1 + REL_ERROR)) {
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

        Counter(List<List<Integer>> groups, List<Parameter> parameters) {
            groupsCount = groups.size();
            //noinspection unchecked
            keys = new List[groupsCount];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = collectKeys(groups.get(i), parameters);
            }
        }

        void add(Map<List<Integer>, Integer>[] inputsCount) {
            vals.add(inputsCount);
        }
    }
}
