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
import structures.moore.NondetMooreAutomaton;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final static boolean SIMULATE = false;

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
        final double traceFraction = 0.95;
        final int repeats = 20;
        final double rThreshold = 3;
        final Random rnd = randomSeed == null ? new Random() : new Random(randomSeed);
        while (true) {
            if (SIMULATE) {
                if (!profile.randomComplification(rnd)) {
                    break;
                }
            } else {
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

        // TODO repeat all this with symbolic models

        logger().info("Execution time: " + executionTime());
    }

    private NondetMooreAutomaton lastGoodAutomaton = null;
    private NondetMooreAutomaton lastAutomaton = null;

    private NondetMooreAutomaton effectiveLastAutomaton() {
        return lastGoodAutomaton == null ? lastAutomaton : lastGoodAutomaton;
    }

    private int supportedNumber(Configuration conf, double traceFraction) throws IOException {
        final NondetMooreAutomaton result = ExplicitStateBuilder.run(conf, directory, datasetFilename, false, 1,
                traceFraction, true, false, false, false, false, false);
        if (traceFraction == 1) {
            lastAutomaton = result;
        }
        return result.supportedAndAllTransitionNumbers().getLeft();

    }
}
