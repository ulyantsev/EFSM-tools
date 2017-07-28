package continuous_trace_builders;

/**
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.parameters.Parameter;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TraceModelGenerator {
    public static void run(Configuration conf, String directory, String datasetFilename) throws IOException {
        final Dataset ds = Dataset.load(Utils.combinePaths(directory, datasetFilename));

        final int maxLength = ds.values.stream().mapToInt(List::size).max().getAsInt();
        final int minLength = ds.values.stream().mapToInt(List::size).min().getAsInt();
        if (maxLength != minLength) {
            throw new AssertionError("All traces are currently assumed to have equal lengths.");
        }

        final String outFilename = Utils.combinePaths(directory, "trace-model.smv");
        final String individualTraceDir = Utils.combinePaths(directory, "individual-trace-models");
        new File(individualTraceDir).mkdir();

        writeTraceModel(conf, ds, maxLength, 0, ds.values.size(), outFilename);
        for (int i = 0; i < ds.values.size(); i++) {
            writeTraceModel(conf, ds, maxLength, i, i + 1,
                    individualTraceDir + "/trace-model-" + i + ".smv");
        }

        System.out.println("Done; the model has been written to: " + outFilename);
        System.out.println("Individual trace models have been written to: " + individualTraceDir);
    }

    private static void writeTraceModel(Configuration conf, Dataset ds, int maxLength, int indexFrom, int indexTo,
                                        String filename) throws FileNotFoundException {
        final StringBuilder sb = new StringBuilder();
        sb.append(ConstraintBasedBuilder.plantCaption(conf));
        sb.append("    step: 0..").append(maxLength - 1).append(";\n");
        sb.append("    unsupported: boolean;\n");
        sb.append("FROZENVAR\n    trace: ").append(indexFrom).append("..").append(indexTo - 1).append(";\n");
        sb.append("ASSIGN\n");
        sb.append("    init(step) := 0;\n");
        sb.append("    next(step) := step < ").append(maxLength - 1).append(" ? step + 1 : ").append(maxLength - 1).append(";\n");
        sb.append("    init(unsupported) := FALSE;\n");
        sb.append("    next(unsupported) := step = ").append(maxLength - 1).append(";\n");

        for (Parameter p : conf.outputParameters) {
            sb.append("    output_").append(p.traceName()).append(" := case\n");
            for (int traceIndex = indexFrom; traceIndex < indexTo; traceIndex++) {
                sb.append("        trace = ").append(traceIndex).append(": case\n");
                final List<Set<Integer>> valuesToSteps = new ArrayList<>();
                for (int i = 0; i < p.valueCount(); i++) {
                    valuesToSteps.add(new TreeSet<>());
                }
                for (int step = 0; step < ds.values.get(traceIndex).size(); step++) {
                    final double value = ds.get(ds.values.get(traceIndex).get(step), p);
                    final int res = p.traceNameIndex(value);
                    valuesToSteps.get(res).add(step);
                }

                // more compact representation
                final List<Pair<Integer, Set<Integer>>> pairs = new ArrayList<>();
                for (int i = 0; i < p.valueCount(); i++) {
                    if (!valuesToSteps.get(i).isEmpty()) {
                        pairs.add(Pair.of(i, valuesToSteps.get(i)));
                    }
                }

                pairs.sort((v1, v2) -> Integer.compare(v2.getRight().size(), v1.getRight().size()));
                pairs.add(pairs.remove(0)); // shift

                for (int i = 0; i < pairs.size(); i++) {
                    final String condition = i == pairs.size() - 1 ? "TRUE"
                            : ("step in " + expressWithIntervalsNuSMV(pairs.get(i).getRight()));
                    sb.append("            ").append(condition).append(": ").append(pairs.get(i).getLeft())
                            .append(";\n");

                }
                sb.append("        esac;\n");
            }
            sb.append("    esac;\n");
        }

        sb.append("DEFINE\n");
        sb.append("    loop_executed := unsupported;\n");

        sb.append(ConstraintBasedBuilder.plantConversions(conf));

        Utils.writeToFile(filename, sb.toString());
    }

    private static List<Pair<Integer, Integer>> intervals(Collection<Integer> values) {
        final List<Pair<Integer, Integer>> intervals = new ArrayList<>();
        int min = -1;
        int max = -1;
        for (int value : values) {
            if (min == -1) {
                min = max = value;
            } else if (value == max + 1) {
                max = value;
            } else if (value <= max) {
                throw new AssertionError("Input set must contain increasing values.");
            } else {
                intervals.add(Pair.of(min, max));
                min = max = value;
            }
        }
        intervals.add(Pair.of(min, max));
        return intervals;
    }

    public static String expressWithIntervalsNuSMV(Collection<Integer> values) {
        final List<Pair<Integer, Integer>> intervals = intervals(values);
        final List<String> stringIntervals = new ArrayList<>();
        final Set<Integer> separate = new TreeSet<>();
        for (Pair<Integer, Integer> interval : intervals) {
            if (interval.getLeft() + 1 >= interval.getRight()) {
                separate.add(interval.getLeft());
                separate.add(interval.getRight());
            } else {
                stringIntervals.add(interval.getLeft() + ".." + interval.getRight());
            }
        }
        if (!separate.isEmpty()) {
            stringIntervals.add(separate.toString().replace("[", "{").replace("]", "}"));
        }
        return String.join(" union ", stringIntervals);
    }

    public static String expressWithIntervalsSPIN(Collection<Integer> values, String varName) {
        final List<Pair<Integer, Integer>> intervals = intervals(values);
        final List<String> stringIntervals = new ArrayList<>();
        final Set<Integer> separate = new TreeSet<>();
        for (Pair<Integer, Integer> interval : intervals) {
            if (interval.getLeft() + 1 >= interval.getRight()) {
                separate.add(interval.getLeft());
                separate.add(interval.getRight());
            } else {
                stringIntervals.add(varName + " >= " + interval.getLeft() + " && " + varName + " <= "
                        + interval.getRight());
            }
        }
        if (!separate.isEmpty()) {
            stringIntervals.addAll(separate.stream().map(value -> varName + " == " + value)
                    .collect(Collectors.toList()));
        }
        return String.join(" || ", stringIntervals);
    }
}
