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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

public class AprosScenarioCreator {
	private final static String INPUT_DIRECTORY = "evaluation/plant-synthesis/vver-traces";
	private final static String OUTPUT_TRACE_FILENAME = "evaluation/plant-synthesis/vver.sc";
	private final static String OUTPUT_ACTIONSPEC_FILENAME = "evaluation/plant-synthesis/vver.actionspec";
	private final static String OUTPUT_LTL_FILENAME = "evaluation/plant-synthesis/vver.ltl";
	
	private final static List<Parameter> PARAMETERS = Arrays.asList(
			new Parameter(false, "pressure_live_stream", 3.5),
			new Parameter(false, "pressure_lower_plenum", 3.5, 8.0, 10.0),
			new Parameter(true, "th11_speed_setpoint", 1.0),
			new Parameter(true, "tj11_speed_setpoint", 1.0),
			new Parameter(true, "tq11_speed_setpoint", 1.0),
			new Parameter(false, "voltage", 4800.0),
			new Parameter(false, "water_level", 2.3, 2.8)
	);
	
	private static class Parameter {
		private final List<Double> cutoffs;
		private final boolean isInput;
		private final String name;
		private double min = Double.POSITIVE_INFINITY;
		private double max = Double.NEGATIVE_INFINITY;
		
		public Parameter(boolean isInput, String name, Double... cutoffs) {
			this.isInput = isInput;
			this.name = name;
			this.cutoffs = new ArrayList<>(Arrays.asList(cutoffs));
			this.cutoffs.add(Double.POSITIVE_INFINITY);
		}
		
		public void updateLimits(double value) {
			min = Math.min(min, value);
			max = Math.max(max, value);
		}
		
		public Pair<Double, Double> limits() {
			return Pair.of(min, max);
		}
		
		private String traceName(int index) {
			return name.replace("_", "") + index;
		}
		
		public List<String> traceNames() {
			final List<String> res = new ArrayList<>();
			if (isInput) {
				return res;
			}
			for (int j = 0; j < cutoffs.size(); j++) {
				res.add(traceName(j));
			}
			return res;
		}
		
		public int traceNameIndex(double value) {
			for (int i = 0; i < cutoffs.size(); i++) {
				if (value < cutoffs.get(i)) {
					return i;
				}
			}
			throw new AssertionError();
		}
		
		public String traceName(double value) {
			return traceName(traceNameIndex(value));
		}
		
		public List<String> actionspec() {
			final List<String> res = new ArrayList<>();
			if (isInput) {
				return res;
			}
			final List<String> actions = new ArrayList<>();
			for (int i = 0; i < cutoffs.size(); i++) {
				actions.add("action(" + traceName(i) + ")");
			}
			res.add(String.join(" || ", actions));
			for (int i = 0; i < actions.size(); i++) {
				for (int j = i + 1; j < actions.size(); j++) {
					res.add("!" + actions.get(i) + " || !" + actions.get(j));
				}
			}
			return res;
		}
		
		// smooth changes
		public List<String> temporalProperties() {
			final List<String> res = new ArrayList<>();
			if (isInput) {
				return res;
			}
			if (cutoffs.size() < 3) {
				return res;
			}
			for (int i = 0; i < cutoffs.size(); i++) {
				List<String> vicinity = new ArrayList<>();
				vicinity.add(traceName(i));
				if (i > 0) {
					vicinity.add(traceName(i - 1));
				}
				if (i < cutoffs.size() - 1) {
					vicinity.add(traceName(i + 1));
				}
				vicinity = vicinity.stream()
						.map(s -> "action(" + s + ")")
						.collect(Collectors.toList());
				res.add("G(!" + vicinity.get(0) + " || X("
						+ String.join(" || ", vicinity) + "))");
			}
			return res;
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		// traces
		final Set<String> allEvents = new TreeSet<>();
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_TRACE_FILENAME))) {
			for (String filename : new File(INPUT_DIRECTORY).list()) {
				if (!filename.endsWith(".txt")) {
					continue;
				}
				final List<String> events = new ArrayList<>();
				final List<String> actions = new ArrayList<>();
				try (Scanner sc = new Scanner(new File(INPUT_DIRECTORY + "/" + filename))) {
					for (int i = 0; i < 2; i++) {
						sc.nextLine();
					}
					while (sc.hasNextLine()) {
						final String line = sc.nextLine().replace(",", ".");
						if (line.replace("\t", "").replace(" ", "").isEmpty()) {
							continue;
						}
						final String[] tokens = line.split("\t");
						final StringBuilder event = new StringBuilder("A");
						final List<String> thisActions = new ArrayList<>();
						for (int i = 0; i < PARAMETERS.size(); i++) {
							final Parameter p = PARAMETERS.get(i);
							final double value = Double.parseDouble(tokens[i + 1]);
							p.updateLimits(value);
							if (p.isInput) {
								event.append(p.traceNameIndex(value));
							} else {
								thisActions.add(p.traceName(value));
							}
						}
						events.add(event.toString());
						actions.add(String.join(", ", thisActions));
						allEvents.add(event.toString());
					}
				}
				events.add(0, "");
				events.remove(events.size() - 1);
				pw.println(String.join("; ", events));
				pw.println(String.join("; ", actions));
			}
		}
		
		// actionspec
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_ACTIONSPEC_FILENAME))) {
			for (Parameter p : PARAMETERS) {
				for (String str : p.actionspec()) {
					pw.println(str);
				}
			}
		}
		
		// temporal properties
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_LTL_FILENAME))) {
			for (Parameter p : PARAMETERS) {
				for (String str : p.temporalProperties()) {
					pw.println(str);
				}
			}
		}
		
		// all actions
		final List<String> allActions = new ArrayList<>();
		for (Parameter p : PARAMETERS) {
			allActions.addAll(p.traceNames());
		}
		
		// execution command
		System.out.println("Run:");
		System.out.println("java -jar jars/plant-automaton-generator.jar "
				+ OUTPUT_TRACE_FILENAME + " --actionNames "
				+ allActions.toString().replace(" ", "").replace("[", "").replace("]", "")
				+ " --actionNumber " + allActions.size()
				+ " --eventNames "
				+ allEvents.toString().replace(" ", "").replace("[", "").replace("]", "")
				+ " --eventNumber " + allEvents.size()
				+ " --ltl " + OUTPUT_LTL_FILENAME
				+ " --actionspec " + OUTPUT_ACTIONSPEC_FILENAME
				+ " --size <SIZE> --varNumber 0 --tree tree.gv");
		
		// paramter limits
		System.out.println("Found parameter boundaries:");
		for (Parameter p : PARAMETERS)  {
			System.out.println(p.name + " in " + p.limits());
		}
	}
}
