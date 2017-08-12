package continuous_trace_builders;

/**
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.fairness_constraints.ComplexFairnessConstraintGenerator;
import continuous_trace_builders.fairness_constraints.MonotonicFairnessConstraintGenerator;
import continuous_trace_builders.parameters.IgnoredBoolParameter;
import continuous_trace_builders.parameters.Parameter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConstraintBasedBuilder {
    static String plantCaption(Configuration conf) {
        final StringBuilder sb = new StringBuilder();
        final String inputLine = String.join(", ",
                conf.inputParameters.stream().map(p -> "CONT_INPUT_" + p.traceName())
                        .collect(Collectors.toList()));
        sb.append("MODULE PLANT(").append(inputLine).append(")\n");
        sb.append("VAR\n");
        for (Parameter p : conf.outputParameters) {
            sb.append("    output_").append(p.traceName()).append(": 0..").append(p.valueCount() - 1).append(";\n");
        }
        return sb.toString();
    }

    static String plantConversions(Configuration conf) {
        final StringBuilder sb = new StringBuilder();
        sb.append("DEFINE\n");
        // output conversion to continuous values
        for (Parameter p : conf.outputParameters) {
            sb.append("    CONT_").append(p.traceName()).append(" := case\n");
            for (int i = 0; i < p.valueCount(); i++) {
                sb.append("        output_").append(p.traceName()).append(" = ").append(i).append(": ")
                        .append(p.nusmvInterval(i)).append(";\n");
            }
            sb.append("    esac;\n");
        }
        return sb.toString();
    }

    static String interval(Collection<Integer> values, Parameter p, boolean next) {
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

    private static void add1DConstraints(Configuration conf, Collection<String> initConstraints,
                                  Collection<String> transConstraints, Map<Parameter, int[][]> paramIndices) {
        for (Parameter p : conf.outputParameters) {
            if (p instanceof IgnoredBoolParameter) {
                continue;
            }
            final int[][] traces = paramIndices.get(p);
            final Set<Integer> indices = new TreeSet<>();
            for (int[] trace : traces) {
                for (int elem : trace) {
                    indices.add(elem);
                }
            }
            initConstraints.add(interval(indices, p, false));
            transConstraints.add(interval(indices, p, true));
        }
    }

    private static void add2DConstraints(Configuration conf, Collection<String> initConstraints,
                                  Collection<String> transConstraints, Map<Parameter, int[][]> paramIndices) {
        for (int i = 0; i < conf.outputParameters.size(); i++) {
            final Parameter pi = conf.outputParameters.get(i);
            if (pi instanceof IgnoredBoolParameter) {
                continue;
            }
            final int[][] tracesI = paramIndices.get(pi);
            for (int j = 0; j < i; j++) {
                final Parameter pj = conf.outputParameters.get(j);
                if (pj instanceof IgnoredBoolParameter) {
                    continue;
                }
                final int[][] tracesJ = paramIndices.get(pj);
                final Set<Integer>[] indexPairs = new Set[pi.valueCount()];
                for (int u = 0; u < tracesI.length; u++) {
                    for (int v = 0; v < tracesI[u].length; v++) {
                        final int index1 = tracesI[u][v];
                        final int index2 = tracesJ[u][v];
                        if (indexPairs[index1] == null) {
                            indexPairs[index1] = new TreeSet<>();
                        }
                        indexPairs[index1].add(index2);
                    }
                }
                for (Collection<String> list : Arrays.asList(initConstraints, transConstraints)) {
                    final boolean next = list == transConstraints;
                    final Function<Parameter, String> varName = p -> {
                        final String res = "output_" + p.traceName();
                        return next ? ("next(" + res + ")") : res;
                    };

                    final List<String> optionList = new ArrayList<>();
                    for (int i1 = 0; i1 < pi.valueCount(); i1++) {
                        if (indexPairs[i1] == null) {
                            continue;
                        }
                        optionList.add(varName.apply(pi) + " = " + i1 + " & "
                                + ConstraintBasedBuilder.interval(indexPairs[i1], pj, next));
                    }

                    list.add(String.join(" | ", optionList));
                }
            }
        }
    }

    private static void addOIOConstraints(Configuration conf, Collection<String> transConstraints,
                                          List<List<Parameter>> grouping, Map<Parameter, int[][]> paramIndices)
            throws IOException {
        for (Parameter pi : conf.inputParameters) {
            if (pi instanceof IgnoredBoolParameter) {
                continue;
            }
            final int[][] tracesI = paramIndices.get(pi);
            for (Parameter po : conf.outputParameters) {
                if (po instanceof IgnoredBoolParameter) {
                    continue;
                }
                if (grouping.stream().anyMatch(l -> l.contains(pi))) {
                    continue;
                }
                final int[][] tracesO = paramIndices.get(po);

                final Map<Integer, Set<Integer>> indexPairs = new TreeMap<>();

                for (int u = 0; u < tracesI.length; u++) {
                    for (int v = 0; v < tracesI[u].length - 1; v++) {
                        final int indexO1 = tracesO[u][v];
                        final int indexI = tracesI[u][v];
                        final int indexO2 = tracesO[u][v + 1];
                        final int mapIndex = (indexO1 << 16) + indexI;
                        final Set<Integer> set = indexPairs.computeIfAbsent(mapIndex, k -> new TreeSet<>());
                        set.add(indexO2);
                    }
                }
                final List<String> optionList = new ArrayList<>();
                for (Map.Entry<Integer, Set<Integer>> entry : indexPairs.entrySet()) {
                    final int indexI = entry.getKey() & ((1 << 16) - 1);
                    final int indexO1 = entry.getKey() >> 16;
                    final Set<Integer> indices = entry.getValue();
                    final String cond = "CONT_INPUT_" + pi.traceName() + " in " + pi.nusmvInterval(indexI)
                            + " & output_" + po.traceName() + " = " + indexO1;
                    final String next = indices.isEmpty() ? "" : (" & " + interval(indices, po, true));
                    optionList.add(cond + next);
                }

                transConstraints.add(String.join(" | ", optionList));
            }
        }
        for (List<Parameter> inputs : grouping) {
            final int[][][] tracesI = new int[inputs.size()][][];
            for (int i = 0; i < inputs.size(); i++) {
                tracesI[i] = paramIndices.get(inputs.get(i));
            }
            for (Parameter po : conf.outputParameters) {
                if (po instanceof IgnoredBoolParameter) {
                    continue;
                }
                final int[][] tracesO = paramIndices.get(po);
                final Map<List<Integer>, Set<Integer>> indexPairs = new HashMap<>();

                for (int u = 0; u < tracesO.length; u++) {
                    for (int v = 0; v < tracesO[u].length - 1; v++) {
                        final List<Integer> key = new ArrayList<>();
                        for (int i = 0; i < inputs.size(); i++) {
                            final int indexI = tracesI[i][u][v];
                            key.add(indexI);
                        }
                        final int indexO1 = tracesO[u][v];
                        key.add(indexO1);
                        final Set<Integer> set = indexPairs.computeIfAbsent(key, k -> new TreeSet<>());
                        final int indexO2 = tracesO[u][v + 1];
                        set.add(indexO2);
                    }
                }
                final List<String> optionList = new ArrayList<>();
                for (Map.Entry<List<Integer>, Set<Integer>> entry : indexPairs.entrySet()) {
                    final List<Integer> key = new ArrayList<>(entry.getKey());
                    final int indexO1 = key.remove(key.size() - 1);
                    final Set<Integer> indices = entry.getValue();
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < inputs.size(); i++) {
                        final Parameter pi = inputs.get(i);
                        sb.append("CONT_INPUT_").append(pi.traceName()).append(" in ")
                                .append(pi.nusmvInterval(key.get(i))).append(" & ");
                    }
                    sb.append("output_").append(po.traceName()).append(" = ").append(indexO1);
                    final String next = indices.isEmpty() ? "" : (" & " + interval(indices, po, true));
                    optionList.add(sb.toString() + next);
                }

                transConstraints.add(String.join(" | ", optionList));
            }
        }
    }

    private static void addInputStateConstraints(Configuration conf, Collection<String> transConstraints,
                                                 Map<Parameter, int[][]> paramIndices)
            throws IOException {
        for (Parameter pi : conf.inputParameters) {
            if (pi instanceof IgnoredBoolParameter) {
                continue;
            }
            final int[][] tracesI = paramIndices.get(pi);
            for (Parameter po : conf.outputParameters) {
                if (po instanceof IgnoredBoolParameter) {
                    continue;
                }
                final int[][] tracesO = paramIndices.get(po);
                final Map<Integer, Set<Integer>> indexPairs = new TreeMap<>();
                for (int index1 = 0; index1 < pi.valueCount(); index1++) {
                    indexPairs.put(index1, new TreeSet<>());
                }
                for (int u = 0; u < tracesI.length; u++) {
                    for (int v = 0; v < tracesI[u].length - 1; v++) {
                        final int index1 = tracesI[u][v];
                        final int index2 = tracesO[u][v + 1];
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

    private static void addCurrentNextConstraints(Configuration conf, Collection<String> transConstraints,
                                                  Map<Parameter, int[][]> paramIndices)
            throws IOException {
        for (Parameter p : conf.outputParameters) {
            if (p instanceof IgnoredBoolParameter) {
                continue;
            }
            final int[][] traces = paramIndices.get(p);
            final Map<Integer, Set<Integer>> indexPairs = new TreeMap<>();
            for (int[] trace : traces) {
                for (int v = 0; v < trace.length - 1; v++) {
                    final int index1 = trace[v];
                    final int index2 = trace[v + 1];
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

    private static void printRes(Configuration conf, Collection<String> initConstraints,
                                 Collection<String> transConstraints, List<String> fairnessConstraints,
                                 String outFilename) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append(plantCaption(conf));
        sb.append("    loop_executed: boolean;\n");
        sb.append("INIT\n");

        final int constraintsCount = initConstraints.size() + transConstraints.size();
        if (initConstraints.isEmpty()) {
            initConstraints.add("TRUE");
        }
        sb.append("    (").append(String.join(")\n  & (", initConstraints)).append(")\n");
        sb.append("TRANS\n");
        if (transConstraints.isEmpty()) {
            transConstraints.add("TRUE");
        }
        sb.append("    (").append(String.join(")\n  & (", transConstraints)).append(")\n");

        final List<String> outParameters = conf.outputParameters.stream()
            .map(p -> "output_" + p.traceName() + " = next(output_" + p.traceName() + ")")
            .collect(Collectors.toList());

        sb.append("ASSIGN\n");
        sb.append("    init(loop_executed) := FALSE;\n");
        sb.append("    next(loop_executed) := ").append(String.join(" & ", outParameters)).append(";\n");

        sb.append("DEFINE\n");
        sb.append("    unsupported := FALSE;\n");
        sb.append(plantConversions(conf));
        for (String fair : fairnessConstraints) {
            sb.append(fair).append("\n");
        }
        final String transformed = sb.toString()
                .replaceAll("& TRUE", "")
                .replaceAll("in TRUE", "")
                .replaceAll("\\s*&\\s*\\(TRUE\\)\\s*", "\n  ")
                .replaceAll("\\s+\\|", " |")
                .replaceAll("\\s+\\)", ")");
        Utils.writeToFile(outFilename, transformed);

        System.out.println("Done; model has been written to: " + outFilename);
        System.out.println("Constraints generated: " + constraintsCount);
    }

    public static void run(Configuration conf, String directory, String datasetFilename, String groupingFile,
                           boolean disableOVERALL_1D, boolean disableOVERALL_2D, boolean disableOIO_CONSTRAINTS,
                           boolean disableINPUT_STATE, boolean disableCURRENT_NEXT,
                           boolean constraintBasedDisableMONOTONIC_FAIRNESS_CONSTRAINTS,
                           boolean constraintBasedDisableCOMPLEX_FAIRNESS_CONSTRAINTS)
            throws IOException {
        final Dataset ds = Dataset.load(Utils.combinePaths(directory, datasetFilename));
        final Set<String> initConstraints = new LinkedHashSet<>();
        final Set<String> transConstraints = new LinkedHashSet<>();
        final Set<Parameter> allParameters = new LinkedHashSet<>(conf.parameters());
        final Map<Parameter, int[][]> paramIndices = ds.toParamIndices(allParameters);

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
                    final String line = in.readLine();
                    if (line == null || line.trim().equals("")) {
                        break;
                    }
                    grouping.add(Arrays.asList(line.trim().split(" "))
                            .stream()
                            .map(findParameter)
                            .collect(Collectors.toList()));
                }
            }
        }

        // 1. overall 1-dimensional constraints
        // "each output may only have values found in the traces"
        if (!disableOVERALL_1D) {
            add1DConstraints(conf, initConstraints, transConstraints, paramIndices);
        }
        // 2. overall 2-dimensional constraints
        // "for each pair of outputs, only value pairs found in some trace element are possible"
        if (!disableOVERALL_2D) {
            add2DConstraints(conf, initConstraints, transConstraints, paramIndices);
        }
        // 3. Ok = a & Ik = b -> O(k+1)=c
        if (!disableOIO_CONSTRAINTS) {
            addOIOConstraints(conf, transConstraints, grouping, paramIndices);
        }

        // 2. 2-dimensional constraints "input -> possible next state"
        // "for each pair of an input and an output, only output values are possible which occur after
        // the given input value in some pair of contiguous trace elements"
        // if the input is unknown, then no constraint
        // FIXME do something with potential deadlocks, when unknown
        // input combinations require non-intersecting actions
        if (!disableINPUT_STATE) {
            addInputStateConstraints(conf, transConstraints, paramIndices);
        }

        // 2. 2-dimensional constraints "current state -> next state"
        // "for each output, only its value transitions found in some pair of contiguous trace
        // elements are possible"
        if (!disableCURRENT_NEXT) {
            addCurrentNextConstraints(conf, transConstraints, paramIndices);
        }

        List<String> fairnessConstraints = new ArrayList<>();
        if (!constraintBasedDisableMONOTONIC_FAIRNESS_CONSTRAINTS) {
            fairnessConstraints.addAll(MonotonicFairnessConstraintGenerator.generateFairnessConstraints(conf, ds, grouping));
        }
        if (!constraintBasedDisableCOMPLEX_FAIRNESS_CONSTRAINTS) {
            fairnessConstraints.addAll(ComplexFairnessConstraintGenerator.generateFairnessConstraints(conf, grouping, paramIndices));
        }

        printRes(conf, initConstraints, transConstraints, fairnessConstraints, Utils.combinePaths(directory,
                "plant-constraints.smv"));
    }
}