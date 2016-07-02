package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import apros.*;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Option;
import java.io.IOException;
import java.util.*;

public class AprosBuilderMain extends MainBase {
    @Option(name = "--type", aliases = {"-t"},
            usage = "model type: explicit-state, constraint-based, traces, sat-based, prepare-dataset",
            metaVar = "<type>", required = true)
    private String type;

    @Option(name = "--log", aliases = {"-l"}, usage = "write log to this file", metaVar = "<file>")
    private String logFilePath;

    @Option(name = "--config", aliases = {"-c"}, usage = "configuration file name",
            metaVar = "<file>")
    private String confFilename;

    @Option(name = "--traces", aliases = {"-tr"}, usage = "trace file location",
            metaVar = "<directory>")
    private String traceLocation;

    @Option(name = "--tracePrefix", aliases = {"-tp"}, usage = "trace filename prefix",
            metaVar = "<prefix>")
    private String traceFilenamePrefix;

    @Option(name = "--paramScales", aliases = {"-ps"}, usage = "parameter scaling file",
            metaVar = "<file>")
    private String paramScaleFilename;

    @Option(name = "--dataset", aliases = {"-ds"}, usage = "filename of the previously serialized dataset",
            metaVar = "<file>")
    private String datasetFilename;

    @Option(name = "--traceIncludeEach", aliases = {"-ti"}, usage = "use only each k-th trace in the dataset",
            metaVar = "<k>")
    private int traceIncludeEach = 1;

    public static void main(String[] args) {
        new AprosBuilderMain().run(args, Author.IB, "Toolset for NuSMV model generation from Apros traces");
    }

    @Override
    protected void launcher() throws IOException {
        initializeLogger(logFilePath);
        if (Objects.equals(type, "prepare-dataset")) {
            DatasetSerializer.run(traceLocation, traceFilenamePrefix, paramScaleFilename);
        } else {
            final Configuration conf = Configuration.load(confFilename);
            if (Objects.equals(type, "constraint-based")) {
                ConstraintExtractor.run(conf, datasetFilename);
            } else if (Objects.equals(type, "explicit-state")) {
                CompositionalBuilder.run(Arrays.asList(conf), datasetFilename, false, traceIncludeEach);
            } else if (Objects.equals(type, "sat-based")) {
                CompositionalBuilder.run(Arrays.asList(conf), datasetFilename, true, traceIncludeEach);
            } else if (Objects.equals(type, "traces")) {
                TraceModelGenerator.run(conf, datasetFilename);
            } else {
                System.err.println("Invalid request type!");
                return;
            }
        }
        logger().info("Execution time: " + executionTime());
    }
}
