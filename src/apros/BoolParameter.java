package apros;

/**
 * (c) Igor Buzhinsky
 */

import java.util.Arrays;
import java.util.List;

public class BoolParameter extends Parameter {
    public BoolParameter(String aprosName, String traceName) {
        super(aprosName, traceName);
    }

    @Override
    public List<String> traceNames() {
        return Arrays.asList(traceName() + "0", traceName() + "1");
    }

    @Override
    public List<String> descriptions() {
        return Arrays.asList(traceName() + " = 0", traceName() + " = 1");
    }

    @Override
    public int traceNameIndex(double value) {
        final double eps = 0.01;
        if (Math.abs(value) < eps) {
            return 0;
        } else if (Math.abs(value - 1) < eps) {
            return 1;
        } else {
            throw new RuntimeException("Invalid value for a Boolean parameter!");
        }
    }

    @Override
    public int valueCount() {
        return 2;
    }

    @Override
    public String toString() {
        return "param " + aprosName() + " (" + traceName() + "): BOOL";
    }

    @Override
    public String nusmvType() {
        return "boolean";
    }

    @Override
    public String nusmvCondition(String name, int index) {
        if (index != 0 && index != 1) {
            throw new AssertionError();
        }
        return (index == 0 ? "!" : "") + name;
    }

    @Override
    public String nusmvInterval(int index) {
        if (index != 0 && index != 1) {
            throw new AssertionError();
        }
        return index == 0 ? "FALSE" : "TRUE";
    }

    @Override
    public String defaultValue() {
        return "FALSE";
    }
}
