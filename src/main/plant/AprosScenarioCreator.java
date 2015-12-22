package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class AprosScenarioCreator {
	public static void main(String[] args) throws FileNotFoundException {
		final List<String> filenames = Arrays.asList(
				"qbf/plant-synthesis/vver-trace1.txt",
				"qbf/plant-synthesis/vver-trace2.txt",
				"qbf/plant-synthesis/vver-trace3.txt");
		final List<List<Double>> cutoffs = Arrays.asList(
				Arrays.asList(4.3, 4.7, Double.POSITIVE_INFINITY),
				Arrays.asList(10., 13., Double.POSITIVE_INFINITY),
				Arrays.asList(Double.POSITIVE_INFINITY),
				Arrays.asList(8.0, Double.POSITIVE_INFINITY)
			);
		
		String[] header = null;
		
		// scenarios
		for (String filename : filenames) {
			final List<String> events = new ArrayList<>();
			final List<String> actions = new ArrayList<>();
			try (Scanner sc = new Scanner(new File(filename))) {
				header = sc.nextLine().split("\t");
				while (sc.hasNextLine()) {
					final String line = sc.nextLine().replace(",", ".");
					final String[] tokens = line.split("\t");
					events.add("A");
					final List<String> thisActions = new ArrayList<>();
					for (int i = 0; i < cutoffs.size(); i++) {
						final double value = Double.parseDouble(tokens[i + 1]);
						for (int j = 0; j < cutoffs.get(i).size(); j++) {
							if (value < cutoffs.get(i).get(j)) {
								thisActions.add(header[i + 1].replace("_", "") + j);
								break;
							}
						}
					}
					actions.add(String.join(", ", thisActions));
				}
			}
			events.set(0, "");
			System.out.println(String.join("; ", events));
			System.out.println(String.join("; ", actions));
		}
		// actionspec
		System.out.println();
		for (int i = 0; i < cutoffs.size(); i++) {
			final List<String> actions = new ArrayList<>();
			for (int j = 0; j < cutoffs.get(i).size(); j++) {
				actions.add("action(" + header[i + 1].replace("_", "") + j + ")");
			}
			System.out.println(String.join(" || ", actions));
			for (int j = 0; j < actions.size(); j++) {
				for (int k = j + 1; k < actions.size(); k++) {
					System.out.println("!" + actions.get(j) + " || !" + actions.get(k));
				}
			}
		}
	}
}
