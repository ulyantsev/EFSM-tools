package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
				Arrays.asList(4.3, 4.4, 4.5, 4.7, 4.8, Double.POSITIVE_INFINITY),
				Arrays.asList(9., 10., 11., 12., 13., Double.POSITIVE_INFINITY),
				Arrays.asList(6335., 6700., Double.POSITIVE_INFINITY),
				Arrays.asList(7.75, 8.0, 8.49, Double.POSITIVE_INFINITY)
			);
		
		String[] header = null;
		
		// scenarios
		try (PrintWriter pw = new PrintWriter(new File("qbf/plant-synthesis/vver.sc"))) {
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
				pw.println(String.join("; ", events));
				pw.println(String.join("; ", actions));
			}
		}
		// actionspec
		final List<String> allActions = new ArrayList<>();
		try (PrintWriter pw = new PrintWriter(new File("qbf/plant-synthesis/vver.actionspec"))) {
			for (int i = 0; i < cutoffs.size(); i++) {
				final List<String> actions = new ArrayList<>();
				for (int j = 0; j < cutoffs.get(i).size(); j++) {
					final String action = header[i + 1].replace("_", "") + j;
					actions.add("action(" + action + ")");
					allActions.add(action);
				}
				pw.println(String.join(" || ", actions));
				for (int j = 0; j < actions.size(); j++) {
					for (int k = j + 1; k < actions.size(); k++) {
						pw.println("!" + actions.get(j) + " || !" + actions.get(k));
					}
				}
			}
		}
		// all actions
		System.out.println("All actions: " + allActions.toString()
				.replace(" ", "").replace("[", "").replace("]", ""));
	}
}
