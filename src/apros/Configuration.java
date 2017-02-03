package apros;

/**
 * (c) Igor Buzhinsky
 */

import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class Configuration {
    final double intervalSec;
    public final List<Parameter> outputParameters;
    public final List<Parameter> inputParameters;
    final List<String> colorRules = new ArrayList<>();

    public List<Parameter> parameters() {
        final List<Parameter> l = new ArrayList<>(outputParameters);
        l.addAll(inputParameters);
        return l;
    }

    public List<String> actionDescriptions() {
        final List<String> result = new ArrayList<>();
        for (Parameter p : outputParameters) {
            result.addAll(p.descriptions());
        }
        return result;
    }

    public Map<String, String> extendedActionDescriptions() {
        final Map<String, String> descriptions = new LinkedHashMap<>();
        final List<String> actions = actions();
        final List<String> actionDescriptions = actionDescriptions();
        for (int i = 0; i < actions.size(); i++) {
            descriptions.put(actions.get(i), actionDescriptions.get(i));
        }
        return descriptions;
    }

    public List<String> actions() {
        final List<String> result = new ArrayList<>();
        for (Parameter p : outputParameters) {
            result.addAll(p.traceNames());
        }
        return result;
    }

    public List<Pair<String, Parameter>> actionThresholds() {
        final List<Pair<String, Parameter>> actionThresholds = new ArrayList<>();
        for (Parameter p : outputParameters) {
            actionThresholds.add(Pair.of(p.traceName(), p));
        }
        return actionThresholds;
    }

    public List<Pair<String, Parameter>> eventThresholds() {
        final List<Pair<String, Parameter>> eventThresholds = new ArrayList<>();
        for (Parameter p : inputParameters) {
            eventThresholds.add(Pair.of(p.traceName(), p));
        }
        return eventThresholds;
    }

    public Configuration(double intervalSec,
            List<Parameter> outputParameters,
            List<Parameter> inputParameters) {
        this.intervalSec = intervalSec;
        this.outputParameters = outputParameters;
        this.inputParameters = inputParameters;
    }

    public static Configuration load(String filename) {
        final double intervalSec = 1.0;
        final List<Parameter> outputParameters = new ArrayList<>();
        final List<Parameter> inputParameters = new ArrayList<>();
        final List<String[]> colorRules = new ArrayList<>();
        try (Scanner sc = new Scanner(new File(filename))) {
            while (sc.hasNextLine()) {
                final String line = sc.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                final String[] tokens = Utils.splitString(line);
                final String operation = tokens[0];
                if (operation.equals("color_rule")) {
                    colorRules.add(tokens);
                } else if (operation.equals("in") || operation.equals("out")) {
                    final String type = tokens[1];
                    final String aprosName = tokens[2];
                    final String traceName = tokens[3];
                    final Parameter p;
                    if (type.equals("real")) {
                        final double lowerBound = Double.parseDouble(tokens[4]);
                        final Double[] thresholds = new Double[tokens.length - 6];
                        for (int i = 5; i < tokens.length - 1; i++) {
                            thresholds[i - 5] = Double.parseDouble(tokens[i]);
                        }
                        final double upperBound = Double.parseDouble(tokens[tokens.length - 1]);
                        p = new RealParameter(aprosName, traceName, Pair.of(lowerBound, upperBound), thresholds);
                    } else if (type.equals("bool")) {
                        p = new BoolParameter(aprosName, traceName);
                    } else if (type.equals("ignored_bool")) {
                        p = new IgnoredBoolParameter(aprosName, traceName);
                    } else {
                        throw new RuntimeException("Invalid parameter type: " + type);
                    }
                    (operation.equals("out") ? outputParameters : inputParameters).add(p);
                } else {
                    throw new RuntimeException("Invalid operation type: " + operation);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        final Configuration c = new Configuration(intervalSec, outputParameters, inputParameters);
        for (String[] tokens : colorRules) {
            final String traceName = tokens[1];
            final int index = Integer.parseInt(tokens[2]);
            final String color = tokens[3];
            c.addColorRule(traceName, index, color);
        }
        return c;
    }

    public void addColorRule(String traceName, int index, String color) {
        colorRules.add(traceName + index + "->" + color);
    }

    @Override
    public String toString() {
        return "out:\n  " +
                String.join("\n  ", outputParameters.stream()
                        .map(p -> p.toString()).collect(Collectors.toList()))
                + "\nin:\n  " +
                String.join("\n  ", inputParameters.stream()
                    .map(p -> p.toString()).collect(Collectors.toList()));
    }
}