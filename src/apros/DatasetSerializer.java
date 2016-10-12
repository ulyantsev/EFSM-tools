package apros;

/**
 * (c) Igor Buzhinsky
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class DatasetSerializer {
    public final static void run(String directory, String traceLocation, String traceFilenamePrefix,
                                 String paramScaleFilename, double intervalSec) throws IOException {
        final Dataset ds =
            new Dataset(intervalSec, Utils.combinePaths(directory, traceLocation),
                        traceFilenamePrefix, TraceTranslator.paramScales(Utils.combinePaths(directory, paramScaleFilename)));
        final String outFilename = Utils.combinePaths(directory, "dataset_" + traceFilenamePrefix + ".bin");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outFilename))) {
            out.writeObject(ds);
        }
        System.out.println("Done; dataset has been written to: " + outFilename);
    }
}
