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

    public static void main(String[] args) {
        new SmartBuilderMain().run(args, Author.IB,
                "Improvement of ContinuousTraceBuilder which required reduced configurations.");
    }

    private static class ParameterProfile {
        private final Map<RealParameter, Set<Double>> mandatoryThresholds = new HashMap<>();
        private final Map<RealParameter, Set<Double>> assignedThresholds = new HashMap<>();
        private final Dataset ds;

        // cached
        private final Map<RealParameter, Map<Integer, List<Double>>> quantiles = new HashMap<>();

        private ParameterProfile(Configuration conf, Dataset ds) {
            for (Parameter p : conf.parameters()) {
                if (p instanceof RealParameter) {
                    final RealParameter rp = (RealParameter) p;
                    mandatoryThresholds.put(rp, new TreeSet<>(Arrays.asList(rp.lowerDoubleBound(),
                            rp.upperDoubleBound())));
                    assignedThresholds.put(rp, new TreeSet<>());
                }
            }
            this.ds = ds;
        }

        private void addMandatory(RealParameter p, double value) {
            mandatoryThresholds.get(p).add(value);
        }

        private void updateParameters() {
            for (RealParameter p : mandatoryThresholds.keySet()) {
                final List<Double> newThresholds = new ArrayList<>();
                newThresholds.addAll(mandatoryThresholds.get(p));
                newThresholds.addAll(assignedThresholds.get(p));
                p.replaceThresholds(newThresholds);
            }
        }

        private List<Double> getQuantiles(RealParameter p, int n) throws IOException {
            Map<Integer, List<Double>> map = quantiles.get(p);
            if (map == null) {
                final Set<Integer> ns = new TreeSet<>();
                for (int i = 2; i <= 10; i++) {
                    ns.add(i);
                }
                map = QuantileFinder.find(ds, p, ns);
                quantiles.put(p, map);
            }
            return map.get(n);
        }

        private void addMinimumAssignedThresholds() throws IOException {
            for (RealParameter p : mandatoryThresholds.keySet()) {
                final Set<Double> set = mandatoryThresholds.get(p);
                if (set.size() == 2) {
                    // add a 2-threshold based on quantiles
                    final double threshold = getQuantiles(p, 2).get(0);
                    assignedThresholds.get(p).add(set.contains(threshold) ? (p.upperDoubleBound()
                            - p.lowerDoubleBound()) / 2 : threshold);
                }
            }
            updateParameters();
        }

        private int numberOfIntervals(RealParameter p) {
            final Set<Double> set = new TreeSet<>(mandatoryThresholds.get(p));
            set.addAll(assignedThresholds.get(p));
            set.remove(p.lowerDoubleBound());
            set.remove(p.upperDoubleBound());
            return set.size() + 1;
        }

        private void randomComplification(Random rnd) throws IOException {
            class RandomCollection<E> {
                private final NavigableMap<Double, E> map = new TreeMap<>();
                private double total = 0;

                public void add(double weight, E result) {
                    if (weight > 0) {
                        total += weight;
                        map.put(total, result);
                    }
                }

                public E next() {
                    double value = rnd.nextDouble() * total;
                    return map.higherEntry(value).getValue();
                }
            }
            while (true) {
                final RandomCollection<RealParameter> rc = new RandomCollection<>();
                for (RealParameter p : assignedThresholds.keySet()) {
                    final int n = numberOfIntervals(p);
                    if (n < 10) {
                        rc.add(10 - n, p);
                    }
                }
                final RealParameter chosen = rc.next();
                final int oldNumberOfIntervals = numberOfIntervals(chosen);
                final int oldNumberOfThresholds = assignedThresholds.get(chosen).size();
                assignedThresholds.get(chosen).clear();
                assignedThresholds.get(chosen).addAll(getQuantiles(chosen, oldNumberOfThresholds + 2));
                final int newNumberOfIntervals = numberOfIntervals(chosen);
                System.out.println(chosen.traceName() + ": " + oldNumberOfIntervals + " -> "
                        + newNumberOfIntervals);
                // FIXME solve the problem when the number of thresholds actually reduces due to value overlapping
                if (newNumberOfIntervals > oldNumberOfIntervals) {
                    break;
                }
            }
            updateParameters();
        }
    }

    private static void addSpecThreshold(ParameterProfile profile, RealParameter p, String sign, String strConst) {
        final int intConst = Integer.parseInt(strConst);
        final double result
                = sign.equals("=") || sign.equals("!=") ? intConst
                : sign.equals(">") || sign.equals("<=") ? intConst + 0.5
                : sign.equals(">=") || sign.equals("<") ? intConst - 0.5
                : Double.NaN;
        profile.addMandatory(p, result);
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
        final int repeats = 2;
        final double rThreshold = 2;
        final Random rnd = randomSeed == null ? new Random() : new Random(randomSeed);
        while (true) {
            final int referenceNumber = supportedNumber(conf, 1);
            final List<Integer> supportedNumbers = new ArrayList<>();
            for (int i = 0; i < repeats; i++) {
                supportedNumbers.add(supportedNumber(conf, traceFraction));
            }
            final double avgSupportedNumber = supportedNumbers.stream().mapToInt(x -> x).max().orElse(1);
            final double diff = 1 - traceFraction;
            final double r = referenceNumber * diff / (referenceNumber - avgSupportedNumber);
            System.out.println("r = " + r);
            if (r > rThreshold) {
                profile.randomComplification(rnd);
            } else {
                break;
            }
        }

        // TODO repeat all this with symbolic models

        logger().info("Execution time: " + executionTime());
    }

    private int supportedNumber(Configuration conf, double traceFraction) throws IOException {
        return ExplicitStateBuilder.run(conf, directory, datasetFilename, false, 1,
                traceFraction, true, false, false, false, false, false).supportedAndAllTransitionNumbers().getLeft();

    }
}
