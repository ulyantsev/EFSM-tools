package apros;

/**
 * (c) Igor Buzhinsky
 */

import java.util.Arrays;
import java.util.List;

public class IgnoredBoolParameter extends Parameter {
    public IgnoredBoolParameter(String aprosName, String traceName) {
        super(aprosName, traceName);
    }

    @Override
    public List<String> traceNames() {
        return Arrays.asList(traceName() + "0");
    }

    @Override
    public List<String> descriptions() {
        return Arrays.asList(traceName() + " = 0");
    }

    @Override
    public int traceNameIndex(double value) {
        return 0;
    }

    @Override
    public int valueCount() {
        return 1;
    }

    @Override
    public String toString() {
        return "param " + aprosName() + " (" + traceName() + "): IGNORED_BOOL";
    }

    @Override
    public String nusmvType() {
        return "0..0";
    }

    @Override
    public String nusmvCondition(String name, int index) {
        if (index != 0) {
            throw new AssertionError();
        }
        return "TRUE";
    }

    @Override
    public String nusmvInterval(int index) {
        if (index != 0) {
            throw new AssertionError();
        }
        return "{FALSE, TRUE}";
    }

    @Override
    public String defaultValue() {
        return "0";
    }
}
