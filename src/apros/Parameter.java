package apros;

/**
 * (c) Igor Buzhinsky
 */

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public abstract class Parameter {
    private String traceName;
    private final String aprosName;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;

    public String aprosName() {
        return aprosName;
    }

    public abstract int valueCount();

    public static boolean unify(Parameter p, Parameter q) {
        if (p.aprosName.equals(q.aprosName)) {
            if (p instanceof RealParameter && q instanceof RealParameter) {
                final RealParameter rp = (RealParameter) p;
                final RealParameter rq = (RealParameter) q;
                final Set<Double> allCutoffs = new TreeSet<>(rp.cutoffs);
                allCutoffs.addAll(rq.cutoffs);
                rp.cutoffs.clear();
                rp.cutoffs.addAll(allCutoffs);
                rq.cutoffs.clear();
                rq.cutoffs.addAll(allCutoffs);
            } else if (p instanceof BoolParameter && q instanceof BoolParameter) {
            } else {
                throw new RuntimeException("Incompatible parameter types.");
            }
            q.traceName = p.traceName;
            return true;
        }
        return false;
    }

    public Parameter(String aprosName, String traceName) {
        this.aprosName = aprosName;
        this.traceName = traceName;
    }

    public void updateLimits(double value) {
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    public Pair<Double, Double> limits() {
        return Pair.of(min, max);
    }

    public String traceName() {
        return traceName;
    }

    protected String traceName(int index) {
        return traceName() + index;
    }

    // assuming that this is an output parameter
    public abstract List<String> traceNames();

    // assuming that this is an output parameter
    public abstract List<String> descriptions();

    public abstract int traceNameIndex(double value);

    public abstract String defaultValue();

    public String traceName(double value) {
        return traceName(traceNameIndex(value));
    }

    public abstract String nusmvType();
    public abstract String nusmvInterval(int index);
    public abstract String nusmvCondition(String name, int index);

    // assuming that this is an output parameter
    public List<String> actionspec() {
        final List<String> res = new ArrayList<>();
        final List<String> actions = new ArrayList<>();
        for (int i = 0; i < valueCount(); i++) {
            actions.add("action(" + traceName(i) + ")");
        }
        res.add(String.join(" || ", actions));
        for (int i = 0; i < actions.size(); i++) {
            for (int j = i + 1; j < actions.size(); j++) {
                res.add("!" + actions.get(i) + " || !" + actions.get(j));
            }
        }
        return res;
    }

    // assuming that this is an input parameter
    // smooth changes
    public List<String> smoothnessTemporalProperties(int smoothnessLevel) {
        final List<String> res = new ArrayList<>();
        if (valueCount() <= 1 + smoothnessLevel) {
            return res;
        }
        for (int i = 0; i < valueCount(); i++) {
            List<String> vicinity = new ArrayList<>();
            vicinity.add(traceName(i));

            for (int j = 1; j <= smoothnessLevel; j++) {
                int smallIndex = i - j;
                if (smallIndex >= 0) {
                    vicinity.add(traceName(smallIndex));
                }
                int largeIndex = i + j;
                if (largeIndex < valueCount()) {
                    vicinity.add(traceName(largeIndex));
                }
            }
            vicinity = vicinity.stream().map(s -> "action(" + s + ")")
                    .collect(Collectors.toList());
            res.add("G(!" + vicinity.get(0) + " || X("
                    + String.join(" || ", vicinity) + "))");
        }
        return res;
    }
}
