package continuous_trace_builders.parameters;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SetParameter extends Parameter {
    private final List<Double> values;
    private final List<Integer> roundedValues;

    public SetParameter(String aprosName, String traceName, Double... values) {
        super(aprosName, traceName);
        this.values = new ArrayList<>(Arrays.asList(values));
        roundedValues = this.values.stream().map(v -> (int) Math.round(v)).collect(Collectors.toList());
    }

    @Override
    public List<String> traceNames() {
        final List<String> res = new ArrayList<>();
        for (int j = 0; j < values.size(); j++) {
            res.add(traceName(j));
        }
        return res;
    }

    @Override
    public List<String> descriptions() {
        final List<String> res = new ArrayList<>();
        for (int j = 0; j < values.size(); j++) {
            res.add("[" + j + "] " + traceName() + " = " + values.get(j));
        }
        return res;
    }

    @Override
    public int traceNameIndex(double value) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == value) {
                return i;
            }
        }
        throw new AssertionError();
    }

    public double value(int index) {
        return values.get(index);
    }

    public int roundedValue(int index) {
        return roundedValues.get(index);
    }

    @Override
    public int valueCount() {
        return values.size();
    }

    @Override
    public String toString() {
        return "param " + aprosName() + " (" + traceName() + "): SET" + values;
    }

    @Override
    public String nusmvType() {
        return roundedValues.toString().replace("[", "{").replace("]", "}");
    }

    @Override
    public String spinType() {
        return "int";
    }

    @Override
    public String nusmvCondition(String name, int index) {
        return name + " = " + roundedValues.get(index);
    }

    @Override
    public String spinCondition(String name, int index) {
        return name + " == " + roundedValues.get(index);
    }

    @Override
    public String nusmvInterval(int index) {
        return String.valueOf(roundedValues.get(index));
    }

    @Override
    public String spinInterval(int index) {
        return roundedValues.get(index) + ".." + roundedValues.get(index);
    }

    @Override
    public String defaultValue() {
        return "0";
    }
}
