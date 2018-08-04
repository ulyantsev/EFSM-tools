package main.plant;

/*
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.*;
import continuous_trace_builders.parameters.Parameter;
import continuous_trace_builders.parameters.RealParameter;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import structures.moore.NondetMooreAutomaton;

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

    public static void main(String[] args) {
        new SmartBuilderMain().run(args, Author.IB,
                "Improvement of ContinuousTraceBuilder which required reduced configurations.");
    }

    @Override
    protected void launcher() throws IOException {
        initializeLogger(logFilePath);
        final Configuration conf = Configuration.load(Utils.combinePaths(directory, confFilename));

        // TODO perform open-loop model checking and exclude unneeded specs
        // generate a dummy plant model
        // compose it
        // verify
        // this will allow avoiding threshold extraction!

        // extracting and adding thresholds from LTL and CTL requirements
        final String specs = new String(Files.readAllBytes(Paths.get(nusmvReqs)), StandardCharsets.UTF_8);
        for (Parameter p : conf.parameters()) {
            if (p instanceof RealParameter) {
                final Set<Integer> newCutoffs = new TreeSet<>();
                final String name = p.traceName();
                final Pattern pattern = Pattern.compile("\\b" + name + " *(<|>|>=|<=|=) *(-?[0-9]+)\\b");
                final Matcher m = pattern.matcher(specs);
                    while (m.find()) {
                    //final String sign = m.group(1);
                    final String strConst = m.group(2);
                    final int intConst = Integer.parseInt(strConst);
                    newCutoffs.add(intConst);
                }
                newCutoffs.forEach(((RealParameter) p)::addCutoff);
                // TODO LATER: interval patterns
                // TODO LATER: intelligent integer boundaries
            }
        }

        System.out.println(conf);

        // TODO start a loop of improvement!
        final double traceFraction = 0.95;
        final int repeats = 2;
        final Dataset ds = Dataset.load(Utils.combinePaths(directory, datasetFilename));
        final double rThreshold = 3;
        while (true) {
            final int referenceNumber = supportedNumber(conf, 1);
            final List<Integer> supportedNumbers = new ArrayList<>();
            for (int i = 0; i < repeats; i++) {
                supportedNumbers.add(supportedNumber(conf, traceFraction));
            }
            final double avgSupportedNumber = supportedNumbers.stream().mapToInt(x -> x).max().orElse(1);
            final double diff = 1 - traceFraction;
            final double r = referenceNumber * diff / (referenceNumber - avgSupportedNumber);
            System.out.println(r);
            if (rThreshold > 3) {
                // TODO update thresholds based on quantiles
            } else {
                break;
            }

            break;
            // TODO evaluate trace sufficiency
            // TODO if sufficient then break else update thresholds based on quantiles
        }

        // TODO repeat all this with symbolic models

        logger().info("Execution time: " + executionTime());
    }

    private int supportedNumber(Configuration conf, double traceFraction) throws IOException {
        return ExplicitStateBuilder.run(conf, directory, datasetFilename, false, 1,
                traceFraction, true, false, false, false, false, false).supportedAndAllTransitionNumbers().getLeft();

    }
}
