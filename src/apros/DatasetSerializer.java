package apros;

/**
 * (c) Igor Buzhinsky
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class DatasetSerializer {
	public final static void run(String traceLocation, String traceFilenamePrefix,
                                 String paramScaleFilename) throws IOException {
        final Dataset ds = new Dataset(1.0,
                traceLocation, traceFilenamePrefix, TraceTranslator.paramScales(paramScaleFilename));
        final String outFilename = "dataset_" + traceFilenamePrefix + ".bin";
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outFilename))) {
            out.writeObject(ds);
        }
        System.out.println("Done; dataset has been written to: " + outFilename);
	}
}
