package continuous_trace_builders.parameters;

/**
 * (c) Igor Buzhinsky
 */

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SegmentsParameter extends Parameter {
    private final List<Pair<Double, Double>> doubleBounds;
    final double eps = 1e-10;

    public SegmentsParameter(String aprosName, String traceName, List<Pair<Double, Double>> bounds) {
        super(aprosName, traceName);
        this.doubleBounds = bounds;
    }

    @Override
    public List<String> traceNames() {
        final List<String> res = new ArrayList<>();
        for (int j = 0; j < doubleBounds.size(); j++) {
            res.add(traceName(j));
        }
        return res;
    }

    @Override
    public List<String> descriptions() {
        final List<String> res = new ArrayList<>();
        for (int j = 0; j < doubleBounds.size(); j++) {
            Pair<Double, Double> pair = doubleBounds.get(j);
            res.add("[" + j + "] " + pair.getLeft() + " ≤ " + traceName() + " ≤ " + pair.getRight());
        }
        return res;
    }

    @Override
    public int traceNameIndex(double value) {
        for (int i = 0; i < doubleBounds.size(); i++) {
            Pair<Double, Double> pair = doubleBounds.get(i);
            if (pair.getLeft() <= value + eps && value <= pair.getRight() + eps) {
                return i;
            }
        }
        throw new RuntimeException("Parameter " + traceName() + ": bounds violated for value " + value);
    }

    @Override
    public int valueCount() {
        return doubleBounds.size();
    }

    @Override
    public String toString() {
        return "param " + aprosName() + " (" + traceName() + "): REAL" + doubleBounds;
    }

    private String nusmvTypeCached = null;

    @Override
    public String nusmvType() {
        if (nusmvTypeCached != null)
            return nusmvTypeCached;
        int min = intervalMin(0);
        int max = intervalMax(0);
        for (int i = 1; i < doubleBounds.size(); i++) {
            min = Math.min(min, intervalMin(i));
            max = Math.max(max, intervalMax(i));
        }
        return min + ".." + max;
    }

    @Override
    public String spinType() {
        return "int";
    }

    @Override
    public String nusmvCondition(String name, int index) {
        if (index == 0) {
            return name + " <= " + intervalMax(index);
        } else if (index == valueCount() - 1) {
            return name + " >= " + intervalMin(index);
        } else {
            return name + " in " + nusmvInterval(index);
        }
    }

    @Override
    public String spinCondition(String name, int index) {
        if (index == 0) {
            return name + " <= " + intervalMax(index);
        } else if (index == valueCount() - 1) {
            return name + " >= " + intervalMin(index);
        } else {
            return "(" + name + " >= " + intervalMax(index) + " && " + name + " <= " + intervalMax(index) + ")";
        }
    }

    private int intervalMin(int interval) {
        return (int) Math.round(Math.floor(doubleBounds.get(interval).getLeft() + eps));
    }

    private int intervalMax(int interval) {
        return (int) Math.round(Math.ceil(doubleBounds.get(interval).getRight() - eps));
    }

    @Override
    public String nusmvInterval(int index) {
        return intervalMin(index) + ".." + intervalMax(index);
    }

    @Override
    public String spinInterval(int index) {
        return intervalMin(index) + ".." + intervalMax(index);
    }

    @Override
    public String defaultValue() {
        return "0";
    }
}
