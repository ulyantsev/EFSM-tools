package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import main.plant.apros.*;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AprosBuilderMain {
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

    private void launcher(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        final long startTime = System.currentTimeMillis();

        final CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.out.println("Toolset for NuSMV model generation from Apros traces");
            System.out.println("Author: Igor Buzhinsky (igor.buzhinsky@gmail.com)\n");
            System.out.print("Usage: ");
            parser.printSingleLineUsage(System.out);
            System.out.println();
            parser.printUsage(System.out);
            return;
        }

        final Logger logger = Logger.getLogger("Logger");
        if (logFilePath != null) {
            try {
                final FileHandler fh = new FileHandler(logFilePath, false);
                logger.addHandler(fh);
                final SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);

                logger.setUseParentHandlers(false);
                System.out.println("Log redirected to " + logFilePath);
            } catch (Exception e) {
                System.err.println("Can't work with file " + logFilePath + ": " + e.getMessage());
                return;
            }
        }

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

        final double executionTime = (System.currentTimeMillis() - startTime) / 1000.;
        logger.info("Execution time: " + executionTime);
    }

    public void run(String[] args) {
        try {
            launcher(args);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new AprosBuilderMain().run(args);
    }
}
