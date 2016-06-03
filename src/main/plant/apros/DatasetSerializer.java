package main.plant.apros;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class DatasetSerializer {
	public final static void main(String[] args) throws IOException {
		serialize(Settings.CORRECT_CONTROLLER_TRACE_PREFIX);
		serialize("");
		System.out.println("Done.");
	}
	
	public final static void serialize(String traceFilenamePrefix) throws IOException {
		final Dataset ds = new Dataset(1.0,
				Settings.TRACE_LOCATION, traceFilenamePrefix, TraceTranslator.PARAM_SCALES);
		try (ObjectOutputStream out = new ObjectOutputStream(
				new FileOutputStream("dataset_" + traceFilenamePrefix + ".bin"))) {
			out.writeObject(ds);
		}
	}
}
