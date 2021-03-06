package continuous_trace_builders;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;

public class DatasetSerializer {
    public static void run(String directory, String traceLocation, String traceFilenamePrefix,
                           String paramScaleFilename, double intervalSec, boolean includeFirstElement)
                                 throws IOException {
        final String outDirname = Utils.combinePaths(directory, "dataset_" + traceFilenamePrefix + ".bin");
        System.out.println(new Dataset(intervalSec, Utils.combinePaths(directory, traceLocation),
                traceFilenamePrefix, TraceTranslator.paramScales(Utils.combinePaths(directory, paramScaleFilename)),
                includeFirstElement, outDirname));
    }
}
