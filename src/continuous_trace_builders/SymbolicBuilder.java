package continuous_trace_builders;

/**
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.parameters.IgnoredBoolParameter;
import continuous_trace_builders.parameters.Parameter;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Deprecated
public class SymbolicBuilder {
    private static boolean CURRENT_1D;
    private static boolean CURRENT_2D;
    private static boolean CURRENT_3D;
    private static boolean CURRENT_NEXT_2D;
    private static boolean CURRENT_NEXT_3D;

    private static void current1d(Configuration conf, Collection<String> initConstraints,
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
            initConstraints.add(ConstraintBasedBuilder.interval(indices, p, false));
            transConstraints.add(ConstraintBasedBuilder.interval(indices, p, true));
        }
    }

    private static void current2d(Configuration conf, Collection<String> initConstraints,
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

    private static void current3d(Configuration conf, Collection<String> initConstraints,
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
                for (int k = 0; k < j; k++) {
                    final Parameter pk = conf.outputParameters.get(k);
                    if (pk instanceof IgnoredBoolParameter) {
                        continue;
                    }
                    final int[][] tracesK = paramIndices.get(pk);
                    final Set<Integer>[][] indexTuples = new Set[pi.valueCount()][pj.valueCount()];
                    for (int u = 0; u < tracesI.length; u++) {
                        for (int v = 0; v < tracesI[u].length; v++) {
                            final int index1 = tracesI[u][v];
                            final int index2 = tracesJ[u][v];
                            final int index3 = tracesK[u][v];
                            if (indexTuples[index1][index2] == null) {
                                indexTuples[index1][index2] = new TreeSet<>();
                            }
                            indexTuples[index1][index2].add(index3);
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
                            for (int i2 = 0; i2 < pj.valueCount(); i2++) {
                                if (indexTuples[i1][i2] == null) {
                                    continue;
                                }
                                optionList.add(varName.apply(pi) + " = " + i1 + " & "
                                        + varName.apply(pj) + " = " + i2 + " & "
                                        + ConstraintBasedBuilder.interval(indexTuples[i1][i2], pk, next));
                            }
                        }
                        list.add(String.join(" | ", optionList));
                    }
                }
            }
        }
    }

    private static void currentNext2d(Configuration conf, Collection<String> transConstraints,
                                      Map<Parameter, int[][]> paramIndices, boolean disableCurNextOutputs) {
        final Set<Parameter> inputParameters = new HashSet<>(conf.inputParameters);
        final Set<Parameter> outputParameters = new HashSet<>(conf.outputParameters);
        for (Parameter pCurrent : conf.parameters()) {
            if (pCurrent instanceof IgnoredBoolParameter) {
                continue;
            }
            if (disableCurNextOutputs && outputParameters.contains(pCurrent)) {
                continue;
            }
            final int[][] tracesCurrent = paramIndices.get(pCurrent);
            for (Parameter pNext : conf.outputParameters) {
                if (pNext instanceof IgnoredBoolParameter) {
                    continue;
                }
                final int[][] tracesNext = paramIndices.get(pNext);
                final Set<Integer>[] indexPairs = new Set[pCurrent.valueCount()];
                for (int index1 = 0; index1 < pCurrent.valueCount(); index1++) {
                    indexPairs[index1] = new TreeSet<>();
                }
                for (int u = 0; u < tracesCurrent.length; u++) {
                    for (int v = 0; v < tracesCurrent[u].length; v++) {
                        final int index1 = tracesCurrent[u][v];
                        final int index2 = tracesNext[u][v];
                        indexPairs[index1].add(index2);
                    }
                }
                final List<String> optionList = new ArrayList<>();
                for (int i1 = 0; i1 < pCurrent.valueCount(); i1++) {
                    final String currentCondition = inputParameters.contains(pCurrent)
                            ? ("CONT_INPUT_" + pCurrent.traceName() + " in " + pCurrent.nusmvInterval(i1))
                            : ("output_" + pCurrent.traceName() + " = " + i1);
                    final String nextCondition = indexPairs[i1].isEmpty() ? ""
                            : (" & " + ConstraintBasedBuilder.interval(indexPairs[i1], pNext, true));
                    optionList.add(currentCondition + nextCondition);
                }
                transConstraints.add(String.join(" | ", optionList));
            }
        }
    }

    private static void currentNext3d(Configuration conf, Collection<String> transConstraints,
                                      Map<Parameter, int[][]> paramIndices,
                                      boolean disableCurNextOutputs) {
        final Set<Parameter> inputParameters = new HashSet<>(conf.inputParameters);
        final Set<Parameter> outputParameters = new HashSet<>(conf.outputParameters);
        final List<Parameter> allParameters = new ArrayList<>(conf.parameters());
        for (int pi1 = 0; pi1 < allParameters.size(); pi1++) {
            final Parameter pCurrent1 = allParameters.get(pi1);
            if (pCurrent1 instanceof IgnoredBoolParameter) {
                continue;
            }
            if (disableCurNextOutputs && outputParameters.contains(pCurrent1)) {
                continue;
            }
            final int[][] tracesCurrent1 = paramIndices.get(pCurrent1);
            for (int pi2 = 0; pi2 < pi1; pi2++) {
                final Parameter pCurrent2 = allParameters.get(pi2);
                if (pCurrent2 instanceof IgnoredBoolParameter) {
                    continue;
                }
                if (disableCurNextOutputs && outputParameters.contains(pCurrent2)) {
                    continue;
                }
                final int[][] tracesCurrent2 = paramIndices.get(pCurrent2);
                for (Parameter pNext : conf.outputParameters) {
                    if (pNext instanceof IgnoredBoolParameter) {
                        continue;
                    }
                    final int[][] tracesNext = paramIndices.get(pNext);
                    final Set<Integer>[][] indexTuples = new Set[pCurrent1.valueCount()][pCurrent2.valueCount()];
                    for (int index1 = 0; index1 < pCurrent1.valueCount(); index1++) {
                        for (int index2 = 0; index2 < pCurrent2.valueCount(); index2++) {
                            indexTuples[index1][index2] = new TreeSet<>();
                        }
                    }
                    for (int u = 0; u < tracesCurrent1.length; u++) {
                        for (int v = 0; v < tracesCurrent1[u].length; v++) {
                            final int indexCurrent1 = tracesCurrent1[u][v];
                            final int indexCurrent2 = tracesCurrent2[u][v];
                            final int indexNext = tracesNext[u][v];
                            indexTuples[indexCurrent1][indexCurrent2].add(indexNext);
                        }
                    }
                    final List<String> optionList = new ArrayList<>();
                    for (int i1 = 0; i1 < pCurrent1.valueCount(); i1++) {
                        final String currentCondition1 = inputParameters.contains(pCurrent1)
                                ? ("CONT_INPUT_" + pCurrent1.traceName() + " in " + pCurrent1.nusmvInterval(i1))
                                : ("output_" + pCurrent1.traceName() + " = " + i1);
                        for (int i2 = 0; i2 < pCurrent2.valueCount(); i2++) {
                            final String currentCondition2 = inputParameters.contains(pCurrent2)
                                    ? ("CONT_INPUT_" + pCurrent2.traceName() + " in " + pCurrent2.nusmvInterval(i2))
                                    : ("output_" + pCurrent2.traceName() + " = " + i2);
                            final String nextCondition = indexTuples[i1][i2].isEmpty() ? ""
                                    : (" & " + ConstraintBasedBuilder.interval(indexTuples[i1][i2], pNext, true));
                            optionList.add(currentCondition1 + " & " + currentCondition2 + nextCondition);
                        }
                    }
                    transConstraints.add(String.join(" | ", optionList));
                }
            }
        }
    }

    private static void printRes(Configuration conf, Collection<String> initConstraints,
                                 Collection<String> transConstraints, String outFilename) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append(ConstraintBasedBuilder.plantCaption(conf));
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
        sb.append(ConstraintBasedBuilder.plantConversions(conf));

        Utils.writeToFile(outFilename, sb.toString());

        System.out.println("Done; model has been written to: " + outFilename);
        System.out.println("Constraints generated: " + constraintsCount);
    }

    public static void run(Configuration conf, String directory, String datasetFilename, boolean current1D,
                           boolean current2D, boolean current3D, boolean currentNext2D, boolean currentNext3D,
                           boolean disableCurNextOutputs, double traceFraction)
            throws IOException {
        CURRENT_1D = current1D;
        CURRENT_2D = current2D;
        CURRENT_3D = current3D;
        CURRENT_NEXT_2D = currentNext2D;
        CURRENT_NEXT_3D = currentNext3D;

        final Dataset ds = Dataset.load(Utils.combinePaths(directory, datasetFilename));
        final Set<Parameter> allParameters = new LinkedHashSet<>(conf.parameters());
        final Map<Parameter, int[][]> paramIndices = ds.toParamIndices(allParameters, traceFraction);
        final Set<String> initConstraints = new LinkedHashSet<>();
        final Set<String> transConstraints = new LinkedHashSet<>();

        // 1. overall 1-dimensional constraints
        // "each output may only have values found in the traces"
        if (CURRENT_1D) {
            System.out.print("Constraints CURRENT_1D...");
            current1d(conf, initConstraints, transConstraints, paramIndices);
            System.out.println(" done");
        }
        // 2. overall 2-dimensional constraints
        // "for each pair of outputs, only value pairs found in some trace element are possible"
        if (CURRENT_2D) {
            System.out.print("Constraints CURRENT_2D...");
            current2d(conf, initConstraints, transConstraints, paramIndices);
            System.out.println(" done");
        }
        // 3. overall 3-dimensional constraints
        // "for each triple of outputs, only value triples found in some trace element are possible"
        if (CURRENT_3D) {
            System.out.print("Constraints CURRENT_3D...");
            current3d(conf, initConstraints, transConstraints, paramIndices);
            System.out.println(" done");
        }

        // 4. 2-dimensional constraints "(input or output) -> next output"
        // if the input is unknown, then no constraint
        // input combinations require non-intersecting actions
        if (CURRENT_NEXT_2D) {
            System.out.print("Constraints CURRENT_NEXT_2D...");
            currentNext2d(conf, transConstraints, paramIndices, disableCurNextOutputs);
            System.out.println(" done");
        }

        // 4. 3-dimensional constraints "(input or output) pair -> next output"
        if (CURRENT_NEXT_3D) {
            System.out.print("Constraints CURRENT_NEXT_3D...");
            currentNext3d(conf, transConstraints, paramIndices, disableCurNextOutputs);
            System.out.println(" done");
        }

        printRes(conf, initConstraints, transConstraints, Utils.combinePaths(directory, "plant-symbolic.smv"));
    }
}
