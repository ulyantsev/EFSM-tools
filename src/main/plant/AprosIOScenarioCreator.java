package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

public class AprosIOScenarioCreator {
	private final static List<Parameter> PARAMETERS_PROTECTION1 = Arrays.asList(
			new Parameter(false, "water_level", 2.3, 2.8),
			new Parameter(false, "pressure_lower_plenum", 3.5, 8.0, 10.0),
			new Parameter(false, "pressure_live_steam", 3.5),
			new Parameter(false, "voltage", 4800.0),
			new Parameter(true, "tq11_speed_setpoint", 1.0),
			new Parameter(true, "tj11_speed_setpoint", 1.0),
			new Parameter(true, "th11_speed_setpoint", 1.0)
	);
	
	private final static List<Parameter> PARAMETERS_PROTECTION7 = Arrays.asList(
			new Parameter(false, "level56x", 1.96),
			new Parameter(false, "level54x", 1.96),
			new Parameter(false, "level52x", 1.96),
			new Parameter(false, "level15x", 1.96),
			new Parameter(false, "level13x", 1.96),
			new Parameter(false, "level11x", 1.96),
			new Parameter(false, "voltage", 4800.0),
			new Parameter(true, "pump_speed", 1.0),
			new Parameter(true, "valve_open", 0.5),
			new Parameter(true, "valve_close", 0.5),
			new Parameter(true, "signal64x", 0.5),
			new Parameter(true, "signal65x", 0.5)
	);
	
	private final static List<Parameter> PARAMETERS_PLANT = Arrays.asList(
			new Parameter(false, "pressurizer_water_level", 2.3, 2.8, 3.705),
			new Parameter(false, "pressure_lower_plenum", 3.5, 8.0, 10.0),
			new Parameter(false, "pressure_live_steam", 3.0, 3.5),
			new Parameter(false, "voltage", 4800.0),
			new Parameter(false, "level56x", 1.8, 1.96),
			new Parameter(false, "level54x", 1.8, 1.96),
			new Parameter(false, "level52x", 1.8, 1.96),
			new Parameter(false, "level15x", 1.8, 1.96),
			new Parameter(false, "level13x", 1.8, 1.96),
			new Parameter(false, "level11x", 1.8, 1.96),
			new Parameter(false, "pressure56x"/*, 4.6*/), // just random cutoff
			new Parameter(false, "pressure54x"/*, 4.6*/),
			new Parameter(false, "pressure52x"/*, 4.6*/),
			new Parameter(false, "pressure15x"/*, 4.6*/),
			new Parameter(false, "pressure13x"/*, 4.6*/),
			new Parameter(false, "pressure11x"/*, 4.6*/),
			new Parameter(false, "reac_rel_power", 0.1, 0.95, 1.0, 1.1),
			new Parameter(false, "pressure_upper_plenum", 10.8, 13.4),
			new Parameter(false, "temp_upper_plenum", 180.0, 317.0),
			new Parameter(true, "trip", 1.0)
	);
	
	static class Configuration {
		final String inputDirectory;
		final double intervalSec;
		final List<Parameter> parameters;
		final List<String> colorRules = new ArrayList<>();
		
		public Configuration(String inputDirectory, double intervalSec,
				List<Parameter> parameters) {
			this.inputDirectory = inputDirectory;
			this.intervalSec = intervalSec;
			this.parameters = parameters;
		}
		
		public void addColorRule(Parameter param, int index, String color) {
			colorRules.add(param.traceName(index) + "->" + color);
		}
	}
	
	private final static Configuration CONFIGURATION_PROTECTION1 = new Configuration(
			"evaluation/plant-synthesis/vver-traces-2",
			1.0, PARAMETERS_PROTECTION1);
	
	private final static Configuration CONFIGURATION_PROTECTION7 = new Configuration(
			"evaluation/plant-synthesis/vver-traces-protection7",
			1.0, PARAMETERS_PROTECTION7);
	
	private final static Configuration CONFIGURATION_PLANT = new Configuration(
			"evaluation/plant-synthesis/vver-traces-plant-2",
			1.0, PARAMETERS_PLANT);
	static {
		// some of the trip conditions
		CONFIGURATION_PLANT.addColorRule(PARAMETERS_PLANT.get(2), 0, "yellow");
		CONFIGURATION_PLANT.addColorRule(PARAMETERS_PLANT.get(16), 4, "yellow");
		CONFIGURATION_PLANT.addColorRule(PARAMETERS_PLANT.get(18), 2, "yellow");
		CONFIGURATION_PLANT.addColorRule(PARAMETERS_PLANT.get(17), 2, "yellow");

		// reactor relative power
		CONFIGURATION_PLANT.addColorRule(PARAMETERS_PLANT.get(16), 0, "blue");
		CONFIGURATION_PLANT.addColorRule(PARAMETERS_PLANT.get(16), 3, "red");
		CONFIGURATION_PLANT.addColorRule(PARAMETERS_PLANT.get(16), 4, "red");
	}
	
	private final static Configuration CONFIGURATION = CONFIGURATION_PLANT;
	
	private final static String OUTPUT_TRACE_FILENAME = "evaluation/plant-synthesis/vver.sc";
	private final static String OUTPUT_ACTIONSPEC_FILENAME = "evaluation/plant-synthesis/vver.actionspec";
	private final static String OUTPUT_LTL_FILENAME = "evaluation/plant-synthesis/vver.ltl";

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
		
		public List<String> descriptions() {
			final List<String> res = new ArrayList<>();
			if (isInput) {
				return res;
			}
			for (int j = 0; j < cutoffs.size(); j++) {
				if (cutoffs.size() == 0) {
					res.add("any " + name);
				} else if (j == 0) {
					res.add(name + " < " + cutoffs.get(j));
				} else if (j == cutoffs.size() - 1) {
					res.add(cutoffs.get(j - 1) + " <= " + name);
				} else {
					res.add(cutoffs.get(j - 1) + " <= " + name + " < " + cutoffs.get(j));
				}
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
		final Set<List<String>> allActionCombinations = new HashSet<>();
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_TRACE_FILENAME))) {
			for (String filename : new File(CONFIGURATION.inputDirectory).list()) {
				if (!filename.endsWith(".txt")) {
					continue;
				}
				final List<String> events = new ArrayList<>();
				final List<String> actions = new ArrayList<>();
				
				double timestampToRecord = CONFIGURATION.intervalSec;
				
				try (Scanner sc = new Scanner(new File(CONFIGURATION.inputDirectory + "/" + filename))) {
					for (int i = 0; i < 2 + CONFIGURATION.parameters.size(); i++) {
						sc.nextLine();
					}
					while (sc.hasNextLine()) {
						final String line = sc.nextLine();
						final String[] tokens = line.split(" +");
						
						double curTimestamp = Double.parseDouble(tokens[1]);
						if (curTimestamp >= timestampToRecord) {
							timestampToRecord += CONFIGURATION.intervalSec;
						} else {
							continue;
						}
						
						final StringBuilder event = new StringBuilder("A");
						final List<String> thisActions = new ArrayList<>();
						for (int i = 0; i < CONFIGURATION.parameters.size(); i++) {
							final Parameter p = CONFIGURATION.parameters.get(i);
							final double value = Double.parseDouble(tokens[i + 2]);
							p.updateLimits(value);
							if (p.isInput) {
								event.append(p.traceNameIndex(value));
							} else {
								thisActions.add(p.traceName(value));
							}
						}
						events.add(event.toString());
						actions.add(String.join(", ", thisActions));
						allActionCombinations.add(thisActions);
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
			for (Parameter p : CONFIGURATION.parameters) {
				for (String str : p.actionspec()) {
					pw.println(str);
				}
			}
		}
		
		// temporal properties
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_LTL_FILENAME))) {
			for (Parameter p : CONFIGURATION.parameters) {
				for (String str : p.temporalProperties()) {
					pw.println(str);
				}
			}
		}
		
		// all actions
		final List<String> allActions = new ArrayList<>();
		final List<String> allActionDescriptions = new ArrayList<>();
		for (Parameter p : CONFIGURATION.parameters) {
			allActions.addAll(p.traceNames());
			allActionDescriptions.addAll(p.descriptions());
		}
		
		// execution command
		final int recommendedSize = allActionCombinations.size();
		System.out.println("Run:");
		System.out.println("java -jar jars/plant-automaton-generator.jar "
				+ OUTPUT_TRACE_FILENAME + " --actionNames "
				+ String.join(",", allActions)
				+ " --actionDescriptions "
				+ "\"" + String.join(",", allActionDescriptions) + "\""
				+ " --colorRules "
				+ "\"" + String.join(",", CONFIGURATION.colorRules) + "\""
				+ " --actionNumber " + allActions.size()
				+ " --eventNames "
				+ String.join(",", allEvents)
				+ " --eventNumber " + allEvents.size()
				+ " --ltl " + OUTPUT_LTL_FILENAME
				+ " --actionspec " + OUTPUT_ACTIONSPEC_FILENAME
				+ " --size " + recommendedSize + " --varNumber 0 --tree tree.gv --nusmv automaton.smv --fast");
		
		// parameter limits
		System.out.println("Found parameter boundaries:");
		for (Parameter p : CONFIGURATION.parameters)  {
			System.out.println(p.name + " in " + p.limits());
		}
	}
}
