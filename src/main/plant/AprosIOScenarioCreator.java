package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

public class AprosIOScenarioCreator {
	/*private final static List<Parameter> PARAMETERS_PROTECTION1 = Arrays.asList(
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
			new Parameter(false, "pressure56x"),
			new Parameter(false, "pressure54x"),
			new Parameter(false, "pressure52x"),
			new Parameter(false, "pressure15x"),
			new Parameter(false, "pressure13x"),
			new Parameter(false, "pressure11x"),
			new Parameter(false, "reac_rel_power", 0.1, 0.95, 1.0, 1.1),
			new Parameter(false, "pressure_upper_plenum", 10.8, 13.4),
			new Parameter(false, "temp_upper_plenum", 180.0, 317.0),
			new Parameter(true, "trip", 1.0)
	);*/
	
	static class Dataset {
		private final Map<String, Integer> paramIndices = new HashMap<>();
		private final List<List<double[]>> values = new ArrayList<>();

		public double get(double[] values, Parameter p) {
			final Integer index = paramIndices.get(p.aprosName);
			if (index == null) {
				throw new RuntimeException("Missing parameter: " + p.aprosName);
			}
			final double result = values[paramIndices.get(p.aprosName)];
			p.updateLimits(result);
			return result;
		}
		
		public Dataset(Configuration conf) throws FileNotFoundException {
			for (String filename : new File(conf.inputDirectory).list()) {
				if (!filename.endsWith(".txt")) {
					continue;
				}
				double timestampToRecord = conf.intervalSec;

				try (Scanner sc = new Scanner(new File(conf.inputDirectory
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
							timestampToRecord += conf.intervalSec;
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

	static class Parameter {
		private final List<Double> cutoffs;
		private final String scName;
		private final String aprosName;
		private double min = Double.POSITIVE_INFINITY;
		private double max = Double.NEGATIVE_INFINITY;

		public Parameter(String aprosName, String name, Double... cutoffs) {
			this.scName = name;
			this.aprosName = aprosName;
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
			return scName.replace("_", "") + index;
		}

		// assuming that this is an output parameter
		public List<String> traceNames() {
			final List<String> res = new ArrayList<>();
			for (int j = 0; j < cutoffs.size(); j++) {
				res.add(traceName(j));
			}
			return res;
		}

		// assuming that this is an output parameter
		public List<String> descriptions() {
			final List<String> res = new ArrayList<>();
			for (int j = 0; j < cutoffs.size(); j++) {
				if (cutoffs.size() == 0) {
					res.add("any " + scName);
				} else if (j == 0) {
					res.add(scName + " < " + cutoffs.get(j));
				} else if (j == cutoffs.size() - 1) {
					res.add(cutoffs.get(j - 1) + " ≤ " + scName);
				} else {
					res.add(cutoffs.get(j - 1) + " ≤ " + scName + " < "
							+ cutoffs.get(j));
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

		// assuming that this is an output parameter
		public List<String> actionspec() {
			final List<String> res = new ArrayList<>();
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

		// assuming that this is an input parameter
		// smooth changes
		public List<String> temporalProperties() {
			final List<String> res = new ArrayList<>();
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
				vicinity = vicinity.stream().map(s -> "action(" + s + ")")
						.collect(Collectors.toList());
				res.add("G(!" + vicinity.get(0) + " || X("
						+ String.join(" || ", vicinity) + "))");
			}
			return res;
		}
	}

	static class Configuration {
		final String inputDirectory;
		final double intervalSec;
		final List<Parameter> outputParameters;
		final List<Parameter> inputParameters;
		final List<String> colorRules = new ArrayList<>();

		public Configuration(String inputDirectory, double intervalSec,
				List<Parameter> outputParameters,
				List<Parameter> inputParameters) {
			this.inputDirectory = inputDirectory;
			this.intervalSec = intervalSec;
			this.outputParameters = outputParameters;
			this.inputParameters = inputParameters;
		}

		public void addColorRule(Parameter param, int index, String color) {
			colorRules.add(param.traceName(index) + "->" + color);
		}
	}

	final static Parameter pressurizerWaterLevel = new Parameter(
			"YP10B001#PR11_LIQ_LEVEL", "water_level", 2.3, 2.8);
	final static Parameter pressureInLowerPlenum = new Parameter(
			"YC00J005#TA11_PRESSURE", "pressure_lower_plenum", 3.5, 8.0, 10.0);
	final static Parameter liveSteamPressure = new Parameter(
			"RA00J010#PO11_PRESSURE", "pressure_live_steam", 3.5);
	final static Parameter busbarVoltage = new Parameter(
			"BU_N1#ES_NO_VOLTAGE_REAL", "voltage", 4800.0);
	final static Parameter pumpTQ11SpeedSetopint = new Parameter(
			"TQ11D001_R01#DC2_OUTPUT_VALUE", "tq11_speed_setpoint", 1.0);
	final static Parameter pumpTJ11SpeedSetopint = new Parameter(
			"TJ11D001_R01#DC2_OUTPUT_VALUE", "tj11_speed_setpoint", 1.0);
	final static Parameter pumpTH11SpeedSetpoint = new Parameter(
			"TH11D001_R01#DC2_OUTPUT_VALUE", "th11_speed_setpoint", 1.0);

	private final static Configuration CONFIGURATION_PROTECTION1 = new Configuration(
			"evaluation/plant-synthesis/vver-traces-2", 1.0, Arrays.asList(
					pressurizerWaterLevel, pressureInLowerPlenum,
					liveSteamPressure, busbarVoltage), Arrays.asList(
					pumpTQ11SpeedSetopint, pumpTJ11SpeedSetopint,
					pumpTH11SpeedSetpoint));

	final static Parameter steamGeneratorLevel56 = new Parameter(
			"YB56W001#SG12_LIQ_LEVEL", "level56x", 1.96);
	final static Parameter steamGeneratorLevel54 = new Parameter(
			"YB54W001#SG12_LIQ_LEVEL", "level54x", 1.96);
	final static Parameter steamGeneratorLevel52 = new Parameter(
			"YB52W001#SG12_LIQ_LEVEL", "level52x", 1.96);
	final static Parameter steamGeneratorLevel15 = new Parameter(
			"YB15W001#SG12_LIQ_LEVEL", "level15x", 1.96);
	final static Parameter steamGeneratorLevel13 = new Parameter(
			"YB13W001#SG12_LIQ_LEVEL", "level13x", 1.96);
	final static Parameter steamGeneratorLevel11 = new Parameter(
			"YB11W001#SG12_LIQ_LEVEL", "level11x", 1.96);
	final static Parameter prot7pumpSpeed = new Parameter(
			"RL92D001_PU1#P_SPEED_OLD", "prot7_pump_speed", 1.0);
	final static Parameter prot7ValveOpen = new Parameter(
			"RL92S005_VA1#VO_OPEN", "prot7_valve_open", 0.5);
	final static Parameter prot7ValveClose = new Parameter(
			"RL92S005_VA1#VO_CLOSE", "prot7_valve_close", 0.5);
	final static Parameter prot7toProt5signal64 = new Parameter(
			"YZU001XL64#BINARY_VALUE", "prot7_signal64x", 0.5);
	final static Parameter prot7toProt5signal65 = new Parameter(
			"YZU001XL65#BINARY_VALUE", "prot7_signal65x", 0.5);

	private final static Configuration CONFIGURATION_PROTECTION7 = new Configuration(
			"evaluation/plant-synthesis/vver-traces-protection7",
			1.0,
			Arrays.asList(steamGeneratorLevel56, steamGeneratorLevel54,
					steamGeneratorLevel52, steamGeneratorLevel15,
					steamGeneratorLevel13, steamGeneratorLevel11, busbarVoltage),
			Arrays.asList(prot7pumpSpeed, prot7ValveOpen, prot7ValveClose,
					prot7toProt5signal64, prot7toProt5signal65));

	final static Parameter pressurizerWaterLevel_entirePlant = new Parameter(
			"YP10B001#PR11_LIQ_LEVEL", "pressurizer_water_level", 2.3, 2.8,
			3.705);
	final static Parameter pressureInLowerPlenum_entirePlant = new Parameter(
			"YC00J005#TA11_PRESSURE", "pressure_lower_plenum", 3.5, 8.0, 10.0);
	final static Parameter liveSteamPressure_entirePlant = new Parameter(
			"RA00J010#PO11_PRESSURE", "pressure_live_steam", 3.0, 3.5);
	final static Parameter busbarVoltage_entirePlant = new Parameter(
			"BU_N1#ES_NO_VOLTAGE_REAL", "voltage", 4800.0);
	final static Parameter steamGeneratorLevel56_entirePlant = new Parameter(
			"YB56W001#SG12_LIQ_LEVEL", "level56x", 1.8, 1.96);
	final static Parameter steamGeneratorLevel54_entirePlant = new Parameter(
			"YB54W001#SG12_LIQ_LEVEL", "level54x", 1.8, 1.96);
	final static Parameter steamGeneratorLevel52_entirePlant = new Parameter(
			"YB52W001#SG12_LIQ_LEVEL", "level52x", 1.8, 1.96);
	final static Parameter steamGeneratorLevel15_entirePlant = new Parameter(
			"YB15W001#SG12_LIQ_LEVEL", "level15x", 1.8, 1.96);
	final static Parameter steamGeneratorLevel13_entirePlant = new Parameter(
			"YB13W001#SG12_LIQ_LEVEL", "level13x", 1.8, 1.96);
	final static Parameter steamGeneratorLevel11_entirePlant = new Parameter(
			"YB11W001#SG12_LIQ_LEVEL", "level11x", 1.8, 1.96);

	final static Parameter steamGeneratorPressure56_entirePlant = new Parameter(
			"YB56W001#SG12_PRESSURE_3_4", "level56x");
	final static Parameter steamGeneratorPressure54_entirePlant = new Parameter(
			"YB54W001#SG12_PRESSURE_3_4", "level54x");
	final static Parameter steamGeneratorPressure52_entirePlant = new Parameter(
			"YB52W001#SG12_PRESSURE_3_4", "level52x");
	final static Parameter steamGeneratorPressure15_entirePlant = new Parameter(
			"YB15W001#SG12_PRESSURE_3_4", "level15x");
	final static Parameter steamGeneratorPressure13_entirePlant = new Parameter(
			"YB13W001#SG12_PRESSURE_3_4", "level13x");
	final static Parameter steamGeneratorPressure11_entirePlant = new Parameter(
			"YB11W001#SG12_LIQ_LEVEL", "level11x");
	final static Parameter reacRelPower_entirePlant = new Parameter(
			"YC00B001#NR1_POWER", "reac_rel_power", 0.1, 0.95, 1.0, 1.1);
	final static Parameter pressureUpperPlenum_entirePlant = new Parameter(
			"YC00J030#TA11_PRESSURE", "pressure_upper_plenum", 10.8, 13.4);
	final static Parameter tempUpperPlenum_entirePlant = new Parameter(
			"YC00J030#TA11_TEMPERATURE", "temp_upper_plenum", 180.0, 317.0);
	final static Parameter tripSignal = new Parameter(
			"YZ10U404FL01#FF_OUTPUT_VALUE", "trip", 1.0);

	private final static Configuration CONFIGURATION_PLANT = new Configuration(
			"evaluation/plant-synthesis/vver-traces-plant-2", 1.0,
			Arrays.asList(pressurizerWaterLevel_entirePlant,
					pressureInLowerPlenum_entirePlant,
					liveSteamPressure_entirePlant, busbarVoltage_entirePlant,
					steamGeneratorLevel56_entirePlant,
					steamGeneratorLevel54_entirePlant,
					steamGeneratorLevel52_entirePlant,
					steamGeneratorLevel15_entirePlant,
					steamGeneratorLevel13_entirePlant,
					steamGeneratorLevel11_entirePlant,
					steamGeneratorPressure56_entirePlant,
					steamGeneratorPressure54_entirePlant,
					steamGeneratorPressure52_entirePlant,
					steamGeneratorPressure15_entirePlant,
					steamGeneratorPressure13_entirePlant,
					steamGeneratorPressure11_entirePlant,
					reacRelPower_entirePlant, pressureUpperPlenum_entirePlant,
					tempUpperPlenum_entirePlant), Arrays.asList(tripSignal));
	
	private final static Configuration CONFIGURATION_ENTIRE_PLANT = new Configuration(
			"evaluation/plant-synthesis/vver-traces-entire-plant", 1.0,
			Arrays.asList(pressurizerWaterLevel_entirePlant,
					pressureInLowerPlenum_entirePlant,
					liveSteamPressure_entirePlant, busbarVoltage_entirePlant,
					steamGeneratorLevel56_entirePlant,
					steamGeneratorLevel54_entirePlant,
					steamGeneratorLevel52_entirePlant,
					steamGeneratorLevel15_entirePlant,
					steamGeneratorLevel13_entirePlant,
					steamGeneratorLevel11_entirePlant,
					steamGeneratorPressure56_entirePlant,
					steamGeneratorPressure54_entirePlant,
					steamGeneratorPressure52_entirePlant,
					steamGeneratorPressure15_entirePlant,
					steamGeneratorPressure13_entirePlant,
					steamGeneratorPressure11_entirePlant,
					reacRelPower_entirePlant, pressureUpperPlenum_entirePlant,
					tempUpperPlenum_entirePlant), Arrays.asList(tripSignal));
	
	static {
		// some of the trip conditions
		CONFIGURATION_PLANT.addColorRule(liveSteamPressure_entirePlant, 0, "yellow");
		CONFIGURATION_PLANT.addColorRule(reacRelPower_entirePlant, 4, "yellow");
		CONFIGURATION_PLANT.addColorRule(tempUpperPlenum_entirePlant, 2, "yellow");
		CONFIGURATION_PLANT.addColorRule(pressureUpperPlenum_entirePlant, 2, "yellow");

		// reactor relative power
		CONFIGURATION_PLANT.addColorRule(reacRelPower_entirePlant, 0, "blue");
		CONFIGURATION_PLANT.addColorRule(reacRelPower_entirePlant, 3, "red");
		CONFIGURATION_PLANT.addColorRule(reacRelPower_entirePlant, 4, "red");
	}

	private final static Configuration CONFIGURATION = CONFIGURATION_PLANT;

	private final static String OUTPUT_TRACE_FILENAME = "evaluation/plant-synthesis/vver.sc";
	private final static String OUTPUT_ACTIONSPEC_FILENAME = "evaluation/plant-synthesis/vver.actionspec";
	private final static String OUTPUT_LTL_FILENAME = "evaluation/plant-synthesis/vver.ltl";

	public static void main(String[] args) throws FileNotFoundException {
		final long time = System.currentTimeMillis();
		// traces
		final Set<String> allEvents = new TreeSet<>();
		final Set<List<String>> allActionCombinations = new HashSet<>();
		
		final Dataset ds = new Dataset(CONFIGURATION);
		
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_TRACE_FILENAME))) {
			for (List<double[]> trace : ds.values) {
				final List<String> events = new ArrayList<>();
				final List<String> actions = new ArrayList<>();

				for (double[] snapshot : trace) {
					final StringBuilder event = new StringBuilder("A");
					final List<String> thisActions = new ArrayList<>();
					
					for (Parameter p : CONFIGURATION.inputParameters) {
						double value = ds.get(snapshot, p);
						event.append(p.traceNameIndex(value));
					}
					
					for (Parameter p : CONFIGURATION.outputParameters) {
						double value = ds.get(snapshot, p);
						thisActions.add(p.traceName(value));
					}
					
					events.add(event.toString());
					actions.add(String.join(", ", thisActions));
					allActionCombinations.add(thisActions);
					allEvents.add(event.toString());
				}
				
				events.add(0, "");
				events.remove(events.size() - 1);
				pw.println(String.join("; ", events));
				pw.println(String.join("; ", actions));
			}
		}

		// actionspec
		try (PrintWriter pw = new PrintWriter(new File(
				OUTPUT_ACTIONSPEC_FILENAME))) {
			for (Parameter p : CONFIGURATION.outputParameters) {
				for (String str : p.actionspec()) {
					pw.println(str);
				}
			}
		}

		// temporal properties
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_LTL_FILENAME))) {
			for (Parameter p : CONFIGURATION.outputParameters) {
				for (String str : p.temporalProperties()) {
					pw.println(str);
				}
			}
		}

		// all actions
		final List<String> allActions = new ArrayList<>();
		final List<String> allActionDescriptions = new ArrayList<>();
		for (Parameter p : CONFIGURATION.outputParameters) {
			allActions.addAll(p.traceNames());
			allActionDescriptions.addAll(p.descriptions());
		}

		// execution command
		final int recommendedSize = allActionCombinations.size();
		System.out.println("Run:");
		System.out.println("java -jar jars/plant-automaton-generator.jar "
				+ OUTPUT_TRACE_FILENAME + " --actionNames "
				+ String.join(",", allActions) + " --actionDescriptions "
				+ "\"" + String.join(",", allActionDescriptions) + "\""
				+ " --colorRules " + "\""
				+ String.join(",", CONFIGURATION.colorRules) + "\""
				+ " --actionNumber " + allActions.size() + " --eventNames "
				+ String.join(",", allEvents) + " --eventNumber "
				+ allEvents.size() + " --ltl " + OUTPUT_LTL_FILENAME
				+ " --actionspec " + OUTPUT_ACTIONSPEC_FILENAME + " --size "
				+ recommendedSize
				+ " --varNumber 0 --tree tree.gv --nusmv automaton.smv --fast");

		// parameter limits
		System.out.println("Found parameter boundaries:");
		for (Parameter p : CONFIGURATION.outputParameters) {
			System.out.println("output " + p.scName + " in " + p.limits());
		}
		for (Parameter p : CONFIGURATION.inputParameters) {
			System.out.println("input " + p.scName + " in " + p.limits());
		}
		System.out.println("Execution time: " + (System.currentTimeMillis() - time) + " ms");
	}
}
