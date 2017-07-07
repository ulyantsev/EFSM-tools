package continuous_trace_builders;

/**
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.fairness_constraints.FairnessConstraintGenerator;
import continuous_trace_builders.fairness_constraints.FairnessMonotonicConstraintGenerator;
import continuous_trace_builders.parameters.Parameter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConstraintExtractor {
    final static boolean OVERALL_1D = true;
    final static boolean OVERALL_2D = true;
    final static boolean OIO_CONSTRAINTS = true;
    final static boolean FAIRNESS_CONSTRAINS = true;
    final static boolean INPUT_STATE = true;
    final static boolean CURRENT_NEXT = true;

    public static String plantCaption(Configuration conf) {
        final StringBuilder sb = new StringBuilder();
        final String inputLine = String.join(", ",
                conf.inputParameters.stream().map(p -> "CONT_INPUT_" + p.traceName())
                        .collect(Collectors.toList()));
        sb.append("MODULE PLANT(" + inputLine + ")\n");
        sb.append("VAR\n");
        for (Parameter p : conf.outputParameters) {
            sb.append("    output_" + p.traceName() + ": 0.." + (p.valueCount() - 1) + ";\n");
        }
        return sb.toString();
    }

    public static String plantConversions(Configuration conf) {
        final StringBuilder sb = new StringBuilder();
        sb.append("DEFINE\n");
        // output conversion to continuous values
        for (Parameter p : conf.outputParameters) {
            sb.append("    CONT_" + p.traceName() + " := case\n");
            for (int i = 0; i < p.valueCount(); i++) {
                sb.append("        output_" + p.traceName() + " = " + i + ": " + p.nusmvInterval(i) + ";\n");
            }
            sb.append("    esac;\n");
        }
        return sb.toString();
    }

    private static String interval(Collection<Integer> values, Parameter p, boolean next) {
        final String range = TraceModelGenerator.expressWithIntervalsNuSMV(values);
        if (p.valueCount() == 2 && range.equals("{0, 1}")) {
            return "TRUE";
        } else if (!range.contains("{") && !range.contains("union")) {
            final String[] tokens = range.split("\\.\\.");
            final int first = Integer.parseInt(tokens[0]);
            final int second = Integer.parseInt(tokens[1]);
            if (first == 0 && second == p.valueCount() - 1) {
                return "TRUE";
            }
        }
        return (next ? "next(" : "") + "output_" + p.traceName() + (next ? ")" : "") + " in " + range;
    }

    private static void add1DConstraints(Configuration conf, List<String> initConstraints,
                                         List<String> transConstraints, Dataset ds) {
        for (Parameter p : conf.outputParameters) {
            final Set<Integer> indices = new TreeSet<>();
            for (List<double[]> trace : ds.values) {
                for (double[] snapshot : trace) {
                    final int index = p.traceNameIndex(ds.get(snapshot, p));
                    indices.add(index);
                }
            }
            initConstraints.add(interval(indices, p, false));
            transConstraints.add(interval(indices, p, true));
        }
    }

    private static void add2DConstraints(Configuration conf, List<String> initConstraints,
                                         List<String> transConstraints, Dataset ds) {
        for (int i = 0; i < conf.outputParameters.size(); i++) {
            final Parameter pi = conf.outputParameters.get(i);
            for (int j = 0; j < i; j++) {
                final Parameter pj = conf.outputParameters.get(j);
                final Map<Integer, Set<Integer>> indexPairs = new TreeMap<>();
                for (List<double[]> trace : ds.values) {
                    for (double[] snapshot : trace) {
                        final int index1 = pi.traceNameIndex(ds.get(snapshot, pi));
                        final int index2 = pj.traceNameIndex(ds.get(snapshot, pj));
                        Set<Integer> secondIndices = indexPairs.get(index1);
                        if (secondIndices == null) {
                            secondIndices = new TreeSet<>();
                            indexPairs.put(index1, secondIndices);
                        }
                        secondIndices.add(index2);
                    }
                }
                for (List<String> list : Arrays.asList(initConstraints, transConstraints)) {
                    final boolean next = list == transConstraints;
                    final Function<Parameter, String> varName = p -> {
                        final String res = "output_" + p.traceName();
                        return next ? ("next(" + res + ")") : res;
                    };

                    final List<String> optionList = new ArrayList<>();
                    for (Map.Entry<Integer, Set<Integer>> implication : indexPairs.entrySet()) {
                        final int index1 = implication.getKey();
                        optionList.add(varName.apply(pi) + " = " + index1 + " & "
                                + interval(implication.getValue(), pj, next));
                    }

                    list.add(String.join(" | ", optionList));
                }
            }
        }
    }

    private static void addOIOConstraints(Configuration conf, List<String> initConstraints,
                                          List<String> transConstraints, Dataset ds, List<List<Parameter>> grouping) {
        for (Parameter pi : conf.inputParameters) {
            for (Parameter po : conf.outputParameters) {
                if (grouping.stream().anyMatch(l -> l.contains(pi)))
                    continue;
                final Map<Integer, Set<Integer>> indexPairs = new TreeMap<>();
                for (List<double[]> trace : ds.values) {
                    for (int i = 0; i < trace.size() - 1; i++) {
                        final int indexO1 = po.traceNameIndex(ds.get(trace.get(i), po));
                        final int indexI = pi.traceNameIndex(ds.get(trace.get(i), pi));
                        final int indexO2 = po.traceNameIndex(ds.get(trace.get(i + 1), po));
                        int mapIndex = (indexO1 << 16) + indexI;
                        Set<Integer> set = indexPairs.computeIfAbsent(mapIndex, k -> new TreeSet<>());
                        set.add(indexO2);
                    }
                }
                final List<String> optionList = new ArrayList<>();
                for (Map.Entry<Integer, Set<Integer>> entry : indexPairs.entrySet()) {
                    int indexI = entry.getKey() & ((1 << 16) - 1);
                    int indexO1 = entry.getKey() >> 16;
                    Set<Integer> indices = entry.getValue();
                    String cond = "CONT_INPUT_" + pi.traceName() + " in " + pi.nusmvInterval(indexI)
                            + " & output_" + po.traceName() + " = " + indexO1;
                    String next = indices.isEmpty() ? "" : (" & " + interval(indices, po, true));
                    optionList.add(cond + next);
                }

                transConstraints.add(String.join(" | ", optionList));
            }
        }
        for (List<Parameter> inputs : grouping) {
            for (Parameter po : conf.outputParameters) {
                final Map<List<Integer>, Set<Integer>> indexPairs = new HashMap<>();
                for (List<double[]> trace : ds.values) {
                    for (int i = 0; i < trace.size() - 1; i++) {
                        List<Integer> key = new ArrayList<>();
                        for (Parameter pi : inputs) {
                            final int indexI = pi.traceNameIndex(ds.get(trace.get(i), pi));
                            key.add(indexI);
                        }
                        final int indexO1 = po.traceNameIndex(ds.get(trace.get(i), po));
                        key.add(indexO1);
                        Set<Integer> set = indexPairs.computeIfAbsent(key, k -> new TreeSet<>());
                        final int indexO2 = po.traceNameIndex(ds.get(trace.get(i + 1), po));
                        set.add(indexO2);
                    }
                }
                final List<String> optionList = new ArrayList<>();
                for (Map.Entry<List<Integer>, Set<Integer>> entry : indexPairs.entrySet()) {
                    List<Integer> key = new ArrayList<>(entry.getKey());
                    int indexO1 = key.remove(key.size() - 1);
                    Set<Integer> indices = entry.getValue();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < inputs.size(); i++) {
                        Parameter pi = inputs.get(i);
                        sb.append("CONT_INPUT_" + pi.traceName() + " in " + pi.nusmvInterval(key.get(i)) + " & ");
                    }
                    sb.append("output_" + po.traceName() + " = " + indexO1);
                    String next = indices.isEmpty() ? "" : (" & " + interval(indices, po, true));
                    optionList.add(sb.toString() + next);
                }

                transConstraints.add(String.join(" | ", optionList));
            }
        }
    }

    private static void addInputStateConstraints(Configuration conf, List<String> transConstraints, Dataset ds) {
        for (Parameter pi : conf.inputParameters) {
            for (Parameter po : conf.outputParameters) {
                final Map<Integer, Set<Integer>> indexPairs = new TreeMap<>();
                for (int index1 = 0; index1 < pi.valueCount(); index1++) {
                    indexPairs.put(index1, new TreeSet<>());
                }
                for (List<double[]> trace : ds.values) {
                    for (int i = 0; i < trace.size() - 1; i++) {
                        final int index1 = pi.traceNameIndex(ds.get(trace.get(i), pi));
                        final int index2 = po.traceNameIndex(ds.get(trace.get(i + 1), po));
                        indexPairs.get(index1).add(index2);
                    }
                }
                final List<String> optionList = new ArrayList<>();
                for (int index1 = 0; index1 < pi.valueCount(); index1++) {
                    optionList.add("CONT_INPUT_" + pi.traceName()
                            + " in " + pi.nusmvInterval(index1)
                            + (indexPairs.get(index1).isEmpty() ? ""
                            : (" & " + interval(indexPairs.get(index1), po, true))));
                }

                transConstraints.add(String.join(" | ", optionList));
            }
        }
    }

    private static void addCurrentNextConstraints(Configuration conf, List<String> transConstraints, Dataset ds) {
        for (Parameter p : conf.outputParameters) {
            final Map<Integer, Set<Integer>> indexPairs = new TreeMap<>();
            for (List<double[]> trace : ds.values) {
                for (int i = 0; i < trace.size() - 1; i++) {
                    final int index1 = p.traceNameIndex(ds.get(trace.get(i), p));
                    final int index2 = p.traceNameIndex(ds.get(trace.get(i + 1), p));
                    Set<Integer> secondIndices = indexPairs.get(index1);
                    if (secondIndices == null) {
                        secondIndices = new TreeSet<>();
                        indexPairs.put(index1, secondIndices);
                    }
                    secondIndices.add(index2);
                }
            }
            final List<String> optionList = new ArrayList<>();
            for (Map.Entry<Integer, Set<Integer>> implication : indexPairs.entrySet()) {
                final int index1 = implication.getKey();
                optionList.add("output_" + p.traceName()
                        + " = " + index1
                        + (" & " + interval(indexPairs.get(index1), p, true)));
            }
            transConstraints.add(String.join(" | ", optionList));
        }
    }

    private static void printRes(Configuration conf, List<String> initConstraints, List<String> transConstraints,
                                 List<String> fairnessConstraints, String outFilename) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append(plantCaption(conf));
        sb.append("    loop_executed: boolean;\n");
        sb.append("INIT\n");

        final int constraintsCount = initConstraints.size() + transConstraints.size();
        if (initConstraints.isEmpty()) {
            initConstraints.add("TRUE");
        }
        sb.append("    ("
                + String.join(")\n  & (", initConstraints)
                + ")\n");
        sb.append("TRANS\n");
        if (transConstraints.isEmpty()) {
            transConstraints.add("TRUE");
        }
        sb.append("    ("
                + String.join(")\n  & (", transConstraints)
                + ")\n");

        final List<String> outParameters = conf.outputParameters.stream()
            .map(p -> "output_" + p.traceName() + " = next(output_" + p.traceName() + ")")
            .collect(Collectors.toList());

        sb.append("ASSIGN\n");
        sb.append("    init(loop_executed) := FALSE;\n");
        sb.append("    next(loop_executed) := " + String.join(" & ", outParameters) + ";\n");

        sb.append("DEFINE\n");
        sb.append("    unsupported := FALSE;\n");
        sb.append(plantConversions(conf));
        for (String fair: fairnessConstraints) {
            sb.append(fair + "\n");
        }
        String transformed =
                sb.toString()
                        .replaceAll("& TRUE", "")
                        .replaceAll("in TRUE", "")
                        .replaceAll("\\s*&\\s*\\(TRUE\\)\\s*", "\n  ");
        Utils.writeToFile(outFilename, transformed);

        System.out.println("Done; model has been written to: " + outFilename);
        System.out.println("Constraints generated: " + constraintsCount);
    }

    public static void run(Configuration conf, String directory, String datasetFilename, String groupingFile) throws IOException {
        final Dataset ds = Dataset.load(Utils.combinePaths(directory, datasetFilename));
        final List<String> initConstraints = new ArrayList<>();
        final List<String> transConstraints = new ArrayList<>();
        final List<List<Parameter>> grouping = new ArrayList<>();
        if (groupingFile != null) {
            Function<String, Parameter> findParameter = s -> {
                for (Parameter par : conf.inputParameters) {
                    if (par.traceName().equals(s)) {
                        return par;
                    }
                }
                return null;
            };
            try (BufferedReader in = new BufferedReader(new FileReader(Paths.get(directory, groupingFile).toString()))) {
                while (true) {
                    String line = in.readLine();
                    if (line == null || line.trim().equals("")) {
                        break;
                    }
                    grouping.add(Arrays.asList(line.trim().split(" "))
                            .stream()
                            .map(findParameter::apply)
                            .collect(Collectors.toList()));
                }
            }
        }

        // 1. overall 1-dimensional constraints
        // "each output may only have values found in the traces"
        if (OVERALL_1D) {
            add1DConstraints(conf, initConstraints, transConstraints, ds);
        }
        // 2. overall 2-dimensional constraints
        // "for each pair of outputs, only value pairs found in some trace element are possible"
        if (OVERALL_2D) {
            add2DConstraints(conf, initConstraints, transConstraints, ds);
        }
        // 3. Ok = a & Ik = b -> O(k+1)=c
        if (OIO_CONSTRAINTS) {
            addOIOConstraints(conf, initConstraints, transConstraints, ds, grouping);
        }

        // 2. 2-dimensional constraints "input -> possible next state"
        // "for each pair of an input and an output, only output values are possible which occur after
        // the given input value in some pair of contiguous trace elements"
        // if the input is unknown, then no constraint
        // FIXME do something with potential deadlocks, when unknown
        // input combinations require non-intersecting actions
        if (INPUT_STATE) {
            addInputStateConstraints(conf, transConstraints, ds);
        }

        // 2. 2-dimensional constraints "current state -> next state"
        // "for each output, only its value transitions found in some pair of contiguous trace
        // elements are possible"
        if (CURRENT_NEXT) {
            addCurrentNextConstraints(conf, transConstraints, ds);
        }

        List<String> fairnessConstraints = FAIRNESS_CONSTRAINS ? FairnessConstraintGenerator.generateFairnessConstraints(conf, ds, grouping) : new ArrayList<>();
        if (FAIRNESS_CONSTRAINS) {
            fairnessConstraints.addAll(FairnessMonotonicConstraintGenerator.generateFairnessConstraints(conf, ds, grouping));
        }

        printRes(conf, initConstraints, transConstraints, fairnessConstraints, Utils.combinePaths(directory, "plant-constraints.smv"));
    }
}
