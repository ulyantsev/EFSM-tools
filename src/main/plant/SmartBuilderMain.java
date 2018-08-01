package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.*;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SmartBuilderMain extends MainBase {
    @Option(name = "--log", aliases = {"-l"}, usage = "write log to this file", metaVar = "<file>")
    private String logFilePath;

    @Option(name = "--partialConfig", aliases = {"-c"}, usage = "partial configuration file name",
            metaVar = "<file>")
    private String confFilename;

    @Option(name = "--dataset", aliases = {"-ds"}, usage = "filename of the previously serialized dataset",
            metaVar = "<file>")
    private String datasetFilename;

    @Option(name = "--dir", usage = "directory where all work files are stored (config file included)",
            metaVar = "<path>")
    private String directory = "";

    public static void main(String[] args) {
        new SmartBuilderMain().run(args, Author.IB,
                "Improvement of ContinuousTraceBuilder which required reduced configurations.");
    }

    @Override
    protected void launcher() throws IOException {
        initializeLogger(logFilePath);
        // TODO
        logger().info("Execution time: " + executionTime());
    }
}
