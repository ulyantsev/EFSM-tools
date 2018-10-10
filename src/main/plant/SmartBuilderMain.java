package main.plant;

/*
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.*;
import continuous_trace_builders.parameters.Parameter;
import continuous_trace_builders.parameters.RealParameter;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import structures.moore.NondetMooreAutomaton;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SmartBuilderMain extends MainBase {
    @Option(name = "--log", aliases = {"-l"}, usage = "write log to this file", metaVar = "<file>")
    private String logFilePath;

    @Option(name = "--dataset", aliases = {"-ds"}, usage = "filename of the previously serialized dataset",
            metaVar = "<file>")
    private String datasetFilename;

    @Option(name = "--dir", usage = "directory where all work files are stored (config file included)",
            metaVar = "<path>")
    private String directory = "";

    @Option(name = "--partialConfig", aliases = {"-c"}, usage = "partial configuration file name",
            metaVar = "<file>")
    private String confFilename;

    @Option(name = "--nusmvBasicBlocks", usage = "filename of a NuSMV model with basic block models",
            metaVar = "<path>")
    private String nusmvBasicBlocks = "";

    @Option(name = "--nusmvMain", usage = "filename of a NuSMV model with the main module",
            metaVar = "<path>")
    private String nusmvMain = "";

    @Option(name = "--nusmvReqs", usage = "filename with LTL and CTL requirements",
            metaVar = "<path>")
    private String nusmvReqs = "";

    @Option(name = "--randomSeed", usage = "random seed", metaVar = "<seed>")
    private Long randomSeed;

    @Option(name = "--r", usage = "r threshold (for explicit models)", metaVar = "<r>")
    private double rThreshold = 3;

    @Option(name = "--d", usage = "d threshold (for symbolic models)", metaVar = "<d>")
    private double dThreshold = 0.05;

    @Option(name = "--models",
            usage = "list of comma-separated model types (symbolic, explicit), default: both",
            metaVar = "<file>")
    private String models = "symbolic, explicit";

    @Option(name = "--traceFraction", usage = "trace fraction to construct reduced models", metaVar = "<fraction>")
    private double traceFraction = 0.9;

    @Option(name = "--repeats", usage = "number of constructed reduced models", metaVar = "<repeats>")
    private int repeats = 20;

    // from ContinuousTraceBuilder:

    @Option(name = "--grouping",
            usage = "constraint-based: file where parameters grouping is described",
            metaVar = "<file>")
    private String groupingFile;

    @Option(name = "--constraintBasedDisableOVERALL_1D", handler = BooleanOptionHandler.class,
            usage = "constraint-based: disable overall 1D constraints")
    private boolean constraintBasedDisableOVERALL_1D;
    @Option(name = "--constraintBasedDisableOVERALL_2D", handler = BooleanOptionHandler.class,
            usage = "constraint-based: disable overall 2D constraints")
    private boolean constraintBasedDisableOVERALL_2D;
    @Option(name = "--constraintBasedDisableOIO_CONSTRAINTS", handler = BooleanOptionHandler.class,
            usage = "constraint-based: disable output-input-output constraints")
    private boolean constraintBasedDisableOIO_CONSTRAINTS;

    @Option(name = "--constraintBasedDisableINPUT_STATE", handler = BooleanOptionHandler.class,
            usage = "constraint-based: disable input-state constraints")
    private boolean constraintBasedDisableINPUT_STATE;
    @Option(name = "--constraintBasedDisableCURRENT_NEXT", handler = BooleanOptionHandler.class,
            usage = "constraint-based: disable current-next constraints")
    private boolean constraintBasedDisableCURRENT_NEXT;

    @Option(name = "--constraintBasedDisableMONOTONIC_FAIRNESS_CONSTRAINTS", handler = BooleanOptionHandler.class,
            usage = "constraint-based: disable fairness constraints, generated on the base of monotonicity")
    private boolean constraintBasedDisableMONOTONIC_FAIRNESS_CONSTRAINTS;

    @Option(name = "--constraintBasedDisableCOMPLEX_FAIRNESS_CONSTRAINTS", handler = BooleanOptionHandler.class,
            usage = "constraint-based: disable complex fairness constraints, generated on base of constant difference" +
                    " of two values to move from one parameter to another")
    private boolean constraintBasedDisableCOMPLEX_FAIRNESS_CONSTRAINTS;

    public static void main(String[] args) {
        new SmartBuilderMain().run(args, Author.IB,
                "Improvement of ContinuousTraceBuilder which required reduced configurations.");
    }

    private static class ParameterProfile {
        private final Map<RealParameter, Set<Double>> thresholds = new HashMap<>();
        private final Set<RealParameter> exhaustedParameters = new HashSet<>();
        private final Dataset ds;

        private ParameterProfile(Configuration conf, Dataset ds) {
            for (Parameter p : conf.parameters()) {
                if (p instanceof RealParameter) {
                    final RealParameter rp = (RealParameter) p;
                    thresholds.put(rp, new TreeSet<>(Arrays.asList(rp.lowerDoubleBound(),
                            rp.upperDoubleBound())));
                }
            }
            this.ds = ds;
        }

        private void addMandatory(RealParameter p, double value) {
            final NavigableSet<Double> target = (NavigableSet<Double>) thresholds.get(p);
            if (value > target.iterator().next() && value < target.descendingIterator().next()) {
                target.add(value);
            }
        }

        private void updateParameters() {
            for (Map.Entry<RealParameter, Set<Double>> e : thresholds.entrySet()) {
                e.getKey().replaceThresholds(new ArrayList<>(e.getValue()));
            }
        }

        private class Interval implements Comparable<Interval> {
            private final double leftThreshold;
            private final double rightThreshold;
            private final int leftIndex;
            private final int rightIndex;

            public Interval(double leftThreshold, double rightThreshold, int leftIndex, int rightIndex) {
                this.leftThreshold = leftThreshold;
                this.rightThreshold = rightThreshold;
                this.leftIndex = leftIndex;
                this.rightIndex = rightIndex;
            }

            private int value() {
                return rightIndex - leftIndex;
            }

            private int midIndex() {
                return (rightIndex + leftIndex) / 2;
            }

            public boolean between(double value) {
                return value > leftThreshold && value < rightThreshold;
            }

            @Override
            public int compareTo(Interval o) {
                return o.value() - value();
            }

            @Override
            public String toString() {
                return value() + "[" + leftThreshold + " @ " + leftIndex + ", " + rightThreshold + " @ "
                        + rightIndex + "]";
            }
        }

        private static double roundToHalfInteger(double value) {
            return Math.round(value + 0.5) - 0.5;
        }

        private boolean improveThresholds(RealParameter p) throws IOException {
            final List<Double> values = QuantileFinder.sortedValues(ds, p);
            final Set<Double> target = thresholds.get(p);
            final List<Double> currentThresholds = new ArrayList<>(target);
            final int[] indices = new int[currentThresholds.size()];
            for (int i = 1; i < currentThresholds.size() - 1; i++) {
                final double threshold = currentThresholds.get(i);
                indices[i] = Math.abs(Collections.binarySearch(values, threshold));
            }
            indices[indices.length - 1] = values.size();

            final Interval[] intervals = new Interval[indices.length - 1];
            for (int i = 0; i < indices.length - 1; i++) {
                intervals[i] = new Interval(currentThresholds.get(i), currentThresholds.get(i + 1), indices[i],
                        indices[i + 1]);
            }
            Arrays.sort(intervals);
            System.out.println(">>>> " + Arrays.toString(intervals));
            for (Interval i : intervals) {
                if (Math.ceil(i.leftThreshold) >= Math.floor(i.rightThreshold)) {
                    // cannot cut in more intervals
                    continue;
                }

                final int middleIndex = i.midIndex();
                final double middleValue = values.get(middleIndex);
                final double adjustedMiddleValue = roundToHalfInteger(middleValue);

                if (i.between(adjustedMiddleValue)) {
                    // good middle
                    target.add(adjustedMiddleValue);
                    return true;
                } else if (adjustedMiddleValue <= i.leftThreshold) {
                    // step right if possible
                    if (i.between(adjustedMiddleValue + 1)) {
                        target.add(adjustedMiddleValue + 1);
                        return true;
                    }
                } else if (adjustedMiddleValue >= i.rightThreshold) {
                    // step left if possible
                    if (i.between(adjustedMiddleValue - 1)) {
                        target.add(adjustedMiddleValue - 1);
                        return true;
                    }
                }
            }
            return false;
        }

        private void addMinimumAssignedThresholds() throws IOException {
            for (RealParameter p : thresholds.keySet()) {
                final Set<Double> set = thresholds.get(p);
                if (set.size() == 2) {
                    System.out.println(">>>> " + p.traceName() + ": " + thresholds.get(p));
                    improveThresholds(p);
                    System.out.println(">>>> " + p.traceName() + ": " + thresholds.get(p));
                }
            }
            updateParameters();
        }

        private int numberOfIntervals(RealParameter p) {
            return thresholds.get(p).size() - 1;
        }

        private boolean randomComplification(Random rnd) throws IOException {
            class RandomCollection<E> {
                private final NavigableMap<Double, E> map = new TreeMap<>();
                private final Set<E> forbidden = new HashSet<>();
                private double total = 0;
                private int remainingValues = 0;

                public void add(double weight, E item) {
                    total += weight;
                    map.put(total, item);
                    remainingValues++;
                }

                public void remove(E item) {
                    forbidden.add(item);
                    remainingValues--;
                }

                public E next() {
                    if (remainingValues == 0) {
                        return null;
                    }
                    E result;
                    do {
                        result = map.higherEntry(rnd.nextDouble() * total).getValue();
                    } while (forbidden.contains(result));
                    return result;
                }
            }
            while (true) {
                final RandomCollection<RealParameter> rc = new RandomCollection<>();
                for (RealParameter p : thresholds.keySet()) {
                    if (exhaustedParameters.contains(p)) {
                        continue;
                    }
                    final int n = numberOfIntervals(p);
                    if (n < 10) {
                        rc.add(10 - n, p);
                    }
                }
                final RealParameter chosen = rc.next();
                if (chosen == null) {
                    System.out.println(">>>> no more threshold improvements are possible");
                    return false;
                }
                System.out.println(">>>> " + chosen.traceName() + ": " + thresholds.get(chosen));
                final boolean success = improveThresholds(chosen);
                if (success) {
                    System.out.println(">>>> " + chosen.traceName() + ": " + thresholds.get(chosen));
                    break;
                } else {
                    exhaustedParameters.add(chosen);
                    rc.remove(chosen);
                    System.out.println(">>>> " + chosen.traceName() + ": failed to improve thresholds");
                }
            }
            updateParameters();
            return true;
        }
    }

    private static void addSpecThreshold(ParameterProfile profile, RealParameter p, String sign, String strConst) {
        final int intConst = Integer.parseInt(strConst);
        if (sign.equals("=") || sign.equals("!=")) {
            // isolation
            profile.addMandatory(p, intConst - 0.5);
            profile.addMandatory(p, intConst + 0.5);
        } else {
            final double result
                    = sign.equals(">") || sign.equals("<=") ? intConst + 0.5
                    : sign.equals(">=") || sign.equals("<") ? intConst - 0.5
                    : Double.NaN;
            profile.addMandatory(p, result);
        }
    }

    @Override
    protected void launcher() throws IOException {
        initializeLogger(logFilePath);
        final Configuration conf = Configuration.load(Utils.combinePaths(directory, confFilename));
        final Dataset ds = Dataset.load(Utils.combinePaths(directory, datasetFilename));
        final ParameterProfile profile = new ParameterProfile(conf, ds);

        // TODO perform open-loop model checking and exclude unneeded specs
        // generate a dummy plant model
        // compose it
        // verify
        // this will allow avoiding threshold extraction!

        // extracting and adding thresholds from LTL and CTL requirements
        final String specs = new String(Files.readAllBytes(Paths.get(nusmvReqs)), StandardCharsets.UTF_8);
        for (Parameter p : conf.parameters()) {
            if (p instanceof RealParameter) {
                final RealParameter rp = (RealParameter) p;
                final String name = p.traceName();
                {
                    final Pattern pattern = Pattern.compile("\\b" + name + " *(<|>|>=|<=|=) *(-?[0-9]+)\\b");
                    final Matcher m = pattern.matcher(specs);
                    while (m.find()) {
                        addSpecThreshold(profile, rp, m.group(1), m.group(2));
                    }
                }
                {
                    final Pattern pattern = Pattern.compile("\\b" + name + " +in +(-?[0-9]+) *\\.\\. *(-?[0-9]+)\\b");
                    final Matcher m = pattern.matcher(specs);
                    while (m.find()) {
                        addSpecThreshold(profile, rp, ">=", m.group(1));
                        addSpecThreshold(profile, rp, "<=", m.group(2));
                    }
                }
            }
        }

        profile.addMinimumAssignedThresholds();

        // start a loop of improvement
        final Random rnd = randomSeed == null ? new Random() : new Random(randomSeed);
        final Set<String> modelSet = new TreeSet<>(Arrays.asList(models.split(", *")));
        final boolean explicit = modelSet.contains("explicit");
        final boolean symbolic = modelSet.contains("symbolic");
        if (!explicit && !symbolic) {
            // just simulate threshold improvements
            while (true) {
                if (!profile.randomComplification(rnd)) {
                    break;
                }
            }
        } else {
            if (explicit) {
                while (true) {
                    System.out.println(">>>> main construction");
                    final int referenceNumber = supportedNumber(conf, 1);
                    final List<Integer> supportedNumbers = new ArrayList<>();
                    for (int i = 0; i < repeats; i++) {
                        System.out.println(">>>> reduced construction " + (i + 1) + "/" + repeats);
                        supportedNumbers.add(supportedNumber(conf, traceFraction));
                    }
                    final double avgSupportedNumber = supportedNumbers.stream().mapToInt(x -> x).average().orElse(0);
                    final double diff = 1 - traceFraction;
                    final double r = referenceNumber * diff / (referenceNumber - avgSupportedNumber);
                    System.out.println(">>>> r = " + r);
                    if (r <= rThreshold || !profile.randomComplification(rnd)) {
                        ExplicitStateBuilder.dumpAutomaton(effectiveLastAutomaton(), conf, directory, "plant-explicit.",
                                false, true, true, true);
                        // TODO configure outputs (GV, Promela, NuSMV) in command-line parameters
                        break;
                    }
                    lastGoodAutomaton = lastAutomaton;
                }
            }

            if (symbolic) {
                while (true) {
                    System.out.println(">>>> main construction");
                    final List<String> originalModel = callSymbolicBuilder(conf, 1);
                    final List<Double> dValues = new ArrayList<>();
                    for (int i = 0; i < repeats; i++) {
                        System.out.println(">>>> reduced construction " + (i + 1) + "/" + repeats);
                        final List<String> reducedModel = callSymbolicBuilder(conf, traceFraction);
                        dValues.add(compareStringLists(originalModel, reducedModel));
                    }
                    final double d = dValues.stream().mapToDouble(x -> x).average().orElse(0);
                    System.out.println(">>>> d = " + d);
                    if (d > dThreshold || !profile.randomComplification(rnd)) {
                        // TODO dump
                        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path()))) {
                            bw.write(effectiveLastSymbolicModel());
                        }
                        break;
                    }
                    lastGoodSymbolicModel = lastSymbolicModel;
                }
            }
        }

        logger().info("Execution time: " + executionTime());
    }

    private NondetMooreAutomaton lastGoodAutomaton = null;
    private NondetMooreAutomaton lastAutomaton = null;

    private String lastGoodSymbolicModel = null;
    private String lastSymbolicModel = null;

    private NondetMooreAutomaton effectiveLastAutomaton() {
        return lastGoodAutomaton == null ? lastAutomaton : lastGoodAutomaton;
    }

    private String effectiveLastSymbolicModel() {
        return lastGoodSymbolicModel == null ? lastSymbolicModel : lastGoodSymbolicModel;
    }

    private int supportedNumber(Configuration conf, double traceFraction) throws IOException {
        final NondetMooreAutomaton result = ExplicitStateBuilder.run(conf, directory, datasetFilename, false, 1,
                traceFraction, true, false, false, false, false, false);
        if (traceFraction == 1) {
            lastAutomaton = result;
        }
        return result.supportedAndAllTransitionNumbers().getLeft();
    }

    private String path() {
        return Utils.combinePaths(directory, ConstraintBasedBuilder.OUTPUT_FILENAME);
    }

    private List<String> callSymbolicBuilder(Configuration conf, double traceFraction) throws IOException {
        ConstraintBasedBuilder.run(conf, directory, datasetFilename, groupingFile, traceFraction,
                constraintBasedDisableOVERALL_1D, constraintBasedDisableOVERALL_2D,
                constraintBasedDisableOIO_CONSTRAINTS,
                constraintBasedDisableINPUT_STATE, constraintBasedDisableCURRENT_NEXT,
                constraintBasedDisableMONOTONIC_FAIRNESS_CONSTRAINTS,
                constraintBasedDisableCOMPLEX_FAIRNESS_CONSTRAINTS);
        lastSymbolicModel = new String(Files.readAllBytes(Paths.get(path())), StandardCharsets.UTF_8);
        List<String> lines = Files.readAllLines(Paths.get(path()), StandardCharsets.UTF_8);
        lines = lines.stream().filter(l -> l.matches("^  (  \\(|&).*$")).collect(Collectors.toList());
        lines = lines.stream().map(l -> l.replaceAll("(\\S)( \\|)", "$1\n$2").replaceAll("(\\S)( &)", "$1\n$2")).collect(Collectors.toList());
        lines = Arrays.asList(String.join("\n", lines).split("\n"));
        lines = lines.stream().map(l -> l.replaceAll("^ *", "")).collect(Collectors.toList());
        lines = lines.stream().filter(l -> !l.matches("^$")).collect(Collectors.toList());
        return lines;
    }

    private static double compareStringLists(List<String> x, List<String> y) {
        return (double) levensteinDistance(x, y) / x.size();
    }

    private static int levensteinDistance(List<String> x, List<String> y) {
        final int[][] dp = new int[x.size() + 1][y.size() + 1];

        for (int i = 0; i <= x.size(); i++) {
            for (int j = 0; j <= y.size(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + (x.get(i - 1).equals(y.get(j - 1)) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }

        return dp[x.size()][y.size()];
    }
}
