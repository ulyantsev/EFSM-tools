package main.plant.apros;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Dataset {
	private final Map<String, Integer> paramIndices = new HashMap<>();
	final List<List<double[]>> values = new ArrayList<>();
	final Map<String, Double> paramScales;
	
	public double get(double[] values, Parameter p) {
		final Integer index = paramIndices.get(p.aprosName());
		if (index == null) {
			throw new RuntimeException("Missing parameter: " + p.aprosName());
		}
		// possible scaling
		final Double oScale = paramScales.get(p.aprosName());
		final double scale = oScale == null? 1.0 : oScale;
		
		final double result = values[paramIndices.get(p.aprosName())] * scale;
		p.updateLimits(result);
		return result;
	}
	
	public Dataset(double intervalSec, String traceLocation, String traceFilenamePrefix, Map<String, Double> paramScales) throws FileNotFoundException {
		this.paramScales = paramScales;
		for (String filename : new File(traceLocation).list()) {
			if (!filename.endsWith(".txt") || !filename.startsWith(traceFilenamePrefix)) {
				continue;
			}
			double timestampToRecord = intervalSec;

			try (Scanner sc = new Scanner(new File(traceLocation
					+ "/" + filename))) {
				final List<double[]> valueLines = new ArrayList<>();
				values.add(valueLines);

				final int paramNum = Integer.valueOf(sc.nextLine()) - 1;
				sc.nextLine();
				if (paramIndices.isEmpty()) {
					// read param names
					for (int i = 0; i < paramNum; i++) {
						final String[] tokens = sc.nextLine().split(" ");
						final String name = tokens[1] + "#" + tokens[2];
						paramIndices.put(name, paramIndices.size());
					}
				} else {
					// skip param names
					for (int i = 0; i < paramNum; i++) {
						sc.nextLine();
					}
				}

				while (sc.hasNextLine()) {
					final String line = sc.nextLine();
					final String[] tokens = line.split(" +");

					double curTimestamp = Double.parseDouble(tokens[1]);
					if (curTimestamp >= timestampToRecord) {
						timestampToRecord += intervalSec;
					} else {
						continue;
					}

					final double[] valueLine = new double[paramNum];
					for (int i = 0; i < paramNum; i++) {
						valueLine[i] = Double.parseDouble(tokens[i + 2]);
					}
					valueLines.add(valueLine);
				}
			}
		}
	}
}
