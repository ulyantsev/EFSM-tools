package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import apros.*;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import java.io.IOException;
import java.util.*;

public class AprosBuilderMain extends MainBase {
    @Option(name = "--type", aliases = {"-t"},
            usage = "explicit-state, constraint-based, constraint-based-new, traces, sat-based, prepare-dataset, trace-evaluation, quantiles",
            metaVar = "<type>", required = true)
    private String type;

    @Option(name = "--log", aliases = {"-l"}, usage = "write log to this file", metaVar = "<file>")
    private String logFilePath;

    @Option(name = "--config", aliases = {"-c"}, usage = "configuration file name",
            metaVar = "<file>")
    private String confFilename;

    @Option(name = "--traces", aliases = {"-tr"}, usage = "prepare-dataset: trace file location",
            metaVar = "<directory>")
    private String traceLocation;

    @Option(name = "--tracePrefix", aliases = {"-tp"}, usage = "prepare-dataset: trace filename prefix",
            metaVar = "<prefix>")
    private String traceFilenamePrefix;

    @Option(name = "--paramScales", aliases = {"-ps"}, usage = "prepare-dataset: parameter scaling file",
            metaVar = "<file>")
    private String paramScaleFilename;

    @Option(name = "--dataset", aliases = {"-ds"}, usage = "filename of the previously serialized dataset",
            metaVar = "<file>")
    private String datasetFilename;

    @Option(name = "--traceIncludeEach", aliases = {"-ti"}, usage = "use only each k-th trace in the dataset",
            metaVar = "<k>")
    private int traceIncludeEach = 1;

    @Option(name = "--traceFraction", aliases = {}, usage = "use only a randomly selected fraction of available traces",
            metaVar = "<k>")
    private double traceFraction = 1;

    @Option(name = "--timeInterval", aliases = {},
            usage = "prepare-dataset: minimum time interval between trace elements (default: 1)",
            metaVar = "<double>")
    private double timeInterval = 1.0;

    @Option(name = "--dir", aliases = {}, usage = "directory where all work files are stored (config file included)",
            metaVar = "<path>")
    private String directory = "";

    @Option(name = "--disableCur2D", handler = BooleanOptionHandler.class,
            usage = "constraint-based-new: disable CURRENT_2D constraints")
    private boolean disableCur2D;

    @Option(name = "--disableCur3D", handler = BooleanOptionHandler.class,
            usage = "constraint-based-new: disable CURRENT_3D constraints")
    private boolean disableCur3D;

    @Option(name = "--disableCurNext2D", handler = BooleanOptionHandler.class,
            usage = "constraint-based-new: disable CURRENT_NEXT_2D constraints")
    private boolean disableCurNext2D;

    @Option(name = "--disableCurNext3D", handler = BooleanOptionHandler.class,
            usage = "constraint-based-new: disable CURRENT_NEXT_3D constraints")
    private boolean disableCurNext3D;

    @Option(name = "--disableCurNextOutputs", handler = BooleanOptionHandler.class,
            usage = "constraint-based-new: disable current-next dependencies between outputs")
    private boolean disableCurNextOutputs;

    @Option(name = "--includeFirstElement", handler = BooleanOptionHandler.class,
            usage = "prepare-dataset: do not skip the first element of each trace")
    private boolean includeFirstElement;

    public static void main(String[] args) {
        new AprosBuilderMain().run(args, Author.IB, "Toolset for NuSMV model generation from Apros traces");
    }

    @Override
    protected void launcher() throws IOException {
        initializeLogger(logFilePath);
        if (Objects.equals(type, "prepare-dataset")) {
            DatasetSerializer.run(directory, traceLocation, traceFilenamePrefix, paramScaleFilename, timeInterval,
                    includeFirstElement);
        } else {
            final Configuration conf = Configuration.load(Utils.combinePaths(directory, confFilename));
            if (Objects.equals(type, "constraint-based")) {
                ConstraintExtractor.run(conf, directory, datasetFilename);
            } else if (Objects.equals(type, "constraint-based-new")) {
                SymbolicBuilder.run(conf, directory, datasetFilename, true, !disableCur2D, !disableCur3D,
                        !disableCurNext2D, !disableCurNext3D, disableCurNextOutputs);
            } else if (Objects.equals(type, "explicit-state")) {
                ExplicitStateBuilder.run(conf, directory, datasetFilename, false, traceIncludeEach, traceFraction);
            } else if (Objects.equals(type, "trace-evaluation")) {
                TraceEvaluationBuilder.run(conf, directory, datasetFilename, false, traceIncludeEach, traceFraction);
            } else if (Objects.equals(type, "sat-based")) {
                ExplicitStateBuilder.run(conf, directory, datasetFilename, true, traceIncludeEach, traceFraction);
            } else if (Objects.equals(type, "traces")) {
                TraceModelGenerator.run(conf, directory, datasetFilename);
            } else if (Objects.equals(type, "quantiles")) {
                QuantileFinder.run(conf, directory, datasetFilename);
            } else {
                System.err.println("Invalid request type!");
                return;
            }
        }
        logger().info("Execution time: " + executionTime());
    }
}
