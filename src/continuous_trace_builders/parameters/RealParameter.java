package continuous_trace_builders.parameters;

/*
 * (c) Igor Buzhinsky
 */

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class RealParameter extends Parameter {
    final List<Double> thresholds;
    private int lowerBound = Integer.MIN_VALUE + 1;
    private int upperBound = Integer.MAX_VALUE;
    private final Pair<Double, Double> doubleBounds;

    public double lowerDoubleBound() {
        return doubleBounds.getLeft();
    }

    public double upperDoubleBound() {
        return doubleBounds.getRight();
    }

    public void replaceThresholds(List<Double> newCutoffs) {
        for (double value : newCutoffs) {
            if (value < doubleBounds.getLeft() || value > doubleBounds.getRight()) {
                throw new RuntimeException();
            }
        }
        final Set<Double> set = new TreeSet<>(newCutoffs);
        set.remove(lowerDoubleBound());
        set.remove(upperDoubleBound());
        thresholds.clear();
        thresholds.addAll(set);
        thresholds.add(Double.POSITIVE_INFINITY);
    }

    public RealParameter(String simulationEnvironmentName, String traceName, Pair<Double, Double> bounds,
                         Double... thresholds) {
        super(simulationEnvironmentName, traceName);
        this.doubleBounds = bounds;
        this.thresholds = new ArrayList<>(Arrays.asList(thresholds));
        this.thresholds.add(Double.POSITIVE_INFINITY);
        if (bounds.getLeft() > lowerBound) {
            lowerBound = (int) Math.round(Math.floor(bounds.getLeft()));
        }
        if (bounds.getRight() < upperBound) {
            upperBound = (int) Math.round(Math.ceil(bounds.getRight()));
        }
    }

    @Override
    public List<String> traceNames() {
        final List<String> res = new ArrayList<>();
        for (int j = 0; j < thresholds.size(); j++) {
            res.add(traceName(j));
        }
        return res;
    }

    @Override
    public List<String> descriptions() {
        final List<String> res = new ArrayList<>();
        for (int j = 0; j < thresholds.size(); j++) {
            final double lower = j == 0 ? doubleBounds.getLeft() : thresholds.get(j - 1);
            final double upper = j == thresholds.size() - 1 ? doubleBounds.getRight() : thresholds.get(j);
            res.add("[" + j + "] " + lower + " ≤ " + traceName() + " < " + upper);
        }
        return res;
    }

    @Override
    public int traceNameIndex(double value) {
        if (value < lowerBound || value > upperBound) {
            throw new RuntimeException("Parameter " + traceName() + ": bounds violated for value " + value);
        }
        for (int i = 0; i < thresholds.size(); i++) {
            if (value < thresholds.get(i)) {
                return i;
            }
        }
        throw new AssertionError();
    }

    @Override
    public int valueCount() {
        return thresholds.size();
    }

    @Override
    public String toString() {
        final ArrayList<Double> thresholds = new ArrayList<>();
        thresholds.add((double) lowerBound);
        thresholds.addAll(this.thresholds.subList(0, this.thresholds.size() - 1));
        thresholds.add((double) upperBound);

        return "param " + simulationEnvironmentName() + " (" + traceName() + "): REAL" + thresholds;
    }

    @Override
    public String nusmvType() {
        return lowerBound + ".." + upperBound;
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
        return interval == 0 ? lowerBound : (int) Math.round(Math.floor(thresholds.get(interval - 1)));
    }
    
    private int intervalMax(int interval) {
        return interval == thresholds.size() - 1 ? upperBound : (int) Math.round(Math.ceil(thresholds.get(interval)));
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
