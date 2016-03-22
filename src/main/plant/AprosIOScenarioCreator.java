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
import java.util.function.Function;
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
	
	final static String INPUT_DIRECTORY = "evaluation/plant-synthesis/vver-traces-entire-plant";
	
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
		
		public Dataset(double intervalSec) throws FileNotFoundException {
			for (String filename : new File(INPUT_DIRECTORY).list()) {
				if (!filename.endsWith(".txt")) {
					continue;
				}
				double timestampToRecord = intervalSec;

				try (Scanner sc = new Scanner(new File(INPUT_DIRECTORY
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

	static class Parameter {
		private final List<Double> cutoffs;
		private String traceName;
		private final String aprosName;
		private double min = Double.POSITIVE_INFINITY;
		private double max = Double.NEGATIVE_INFINITY;

		public String aprosName() {
			return aprosName;
		}
		
		public static boolean unify(Parameter p, Parameter q) {
			if (p.aprosName.equals(q.aprosName)) {
				final Set<Double> allCutoffs = new TreeSet<>(p.cutoffs);
				allCutoffs.addAll(q.cutoffs);
				p.cutoffs.clear();
				p.cutoffs.addAll(allCutoffs);
				q.cutoffs.clear();
				q.cutoffs.addAll(allCutoffs);
				q.traceName = p.traceName;
				return true;
			}
			return false;
		}
		
		public Parameter(String aprosName, String name, Double... cutoffs) {
			this.traceName = name;
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

		public String traceNamePrefix() {
			return traceName;
		}
		
		private String traceName(int index) {
			return traceNamePrefix() + index;
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
				if (cutoffs.size() == 1) {
					res.add("any " + traceName);
				} else if (j == 0) {
					res.add(traceName + " < " + cutoffs.get(j));
				} else if (j == cutoffs.size() - 1) {
					res.add(cutoffs.get(j - 1) + " ≤ " + traceName);
				} else {
					res.add(cutoffs.get(j - 1) + " ≤ " + traceName + " < "
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
		
		@Override
		public String toString() {
			return "param " + aprosName + " (" + traceName + ") "
					+ cutoffs.subList(0, cutoffs.size() - 1);
		}
	}

	static class Configuration {
		final double intervalSec;
		final List<Parameter> outputParameters;
		final List<Parameter> inputParameters;
		final List<String> colorRules = new ArrayList<>();

		public List<Parameter> allParameters() {
			final List<Parameter> params = new ArrayList<>(outputParameters);
			params.addAll(inputParameters);
			return params;
		}
		
		public Configuration(double intervalSec,
				List<Parameter> outputParameters,
				List<Parameter> inputParameters) {
			this.intervalSec = intervalSec;
			this.outputParameters = outputParameters;
			this.inputParameters = inputParameters;
		}

		public void addColorRule(Parameter param, int index, String color) {
			colorRules.add(param.traceName(index) + "->" + color);
		}
		
		@Override
		public String toString() {
			return "out:\n  " +  
					String.join("\n  ", outputParameters.stream()
							.map(p -> p.toString()).collect(Collectors.toList()))
					+ "\nin:\n  " + 
					String.join("\n  ", inputParameters.stream()
						.map(p -> p.toString()).collect(Collectors.toList()));
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

	final static Configuration CONFIGURATION_PROTECTION1 = new Configuration(
			1.0, Arrays.asList(
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

	final static Configuration CONFIGURATION_PROTECTION7 = new Configuration(
			1.0,
			Arrays.asList(steamGeneratorLevel56, steamGeneratorLevel54,
					steamGeneratorLevel52, steamGeneratorLevel15,
					steamGeneratorLevel13, steamGeneratorLevel11, busbarVoltage),
			Arrays.asList(prot7pumpSpeed, prot7ValveOpen, prot7ValveClose,
					prot7toProt5signal64, prot7toProt5signal65));

	final static Parameter steamGeneratorPressure56_prot5 = new Parameter(
			"YB56W001#SG12_PRESSURE_3_4", "pressure56x", 4.0); // random cutoff
	final static Parameter steamGeneratorPressure54_prot5 = new Parameter(
			"YB54W001#SG12_PRESSURE_3_4", "pressure54x", 4.0); // random cutoff
	final static Parameter steamGeneratorPressure52_prot5 = new Parameter(
			"YB52W001#SG12_PRESSURE_3_4", "pressure52x", 4.0); // random cutoff
	final static Parameter steamGeneratorPressure15_prot5 = new Parameter(
			"YB15W001#SG12_PRESSURE_3_4", "pressure15x", 4.0); // random cutoff
	final static Parameter steamGeneratorPressure13_prot5 = new Parameter(
			"YB13W001#SG12_PRESSURE_3_4", "pressure13x", 4.0); // random cutoff
	final static Parameter steamGeneratorPressure11_prot5 = new Parameter(
			"YB11W001#SG12_PRESSURE_3_4", "pressure11x", 4.0); // random cutoff
	
	final static Parameter prot5valve41open = new Parameter(
			"RL41S001_VA1#VO_OPEN", "valve41open", 0.5);
	final static Parameter prot5valve41close = new Parameter(
			"RL41S001_VA1#VO_CLOSE", "valve41close", 0.5);
	final static Parameter prot5valve42open = new Parameter(
			"RL42S001_VA1#VO_OPEN", "valve42open", 0.5);
	final static Parameter prot5valve42close = new Parameter(
			"RL42S001_VA1#VO_CLOSE", "valve42close", 0.5);
	final static Parameter prot5valve43open = new Parameter(
			"RL43S001_VA1#VO_OPEN", "valve43open", 0.5);
	final static Parameter prot5valve43close = new Parameter(
			"RL43S001_VA1#VO_CLOSE", "valve43close", 0.5);
	final static Parameter prot5valve44open = new Parameter(
			"RL44S001_VA1#VO_OPEN", "valve44open", 0.5);
	final static Parameter prot5valve44close = new Parameter(
			"RL44S001_VA1#VO_CLOSE", "valve44close", 0.5);
	final static Parameter prot5valve45open = new Parameter(
			"RL45S001_VA1#VO_OPEN", "valve45open", 0.5);
	final static Parameter prot5valve45close = new Parameter(
			"RL45S001_VA1#VO_CLOSE", "valve45close", 0.5);
	final static Parameter prot5valve46open = new Parameter(
			"RL46S001_VA1#VO_OPEN", "valve46open", 0.5);
	final static Parameter prot5valve46close = new Parameter(
			"RL46S001_VA1#VO_CLOSE", "valve46close", 0.5);
	
	final static Configuration CONFIGURATION_PROTECTION5 = new Configuration(
			1.0, Arrays.asList(
			steamGeneratorPressure56_prot5,
			steamGeneratorPressure54_prot5,
			steamGeneratorPressure52_prot5,
			steamGeneratorPressure15_prot5,
			steamGeneratorPressure13_prot5,
			steamGeneratorPressure11_prot5,
			prot7toProt5signal64,
			prot7toProt5signal65), Arrays.asList(
			prot5valve41open, prot5valve41close,
			prot5valve42open, prot5valve42close,
			prot5valve43open, prot5valve43close,
			prot5valve44open, prot5valve44close,
			prot5valve45open, prot5valve45close,
			prot5valve46open, prot5valve46close));

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
			"YB56W001#SG12_PRESSURE_3_4", "pressure56x");
	final static Parameter steamGeneratorPressure54_entirePlant = new Parameter(
			"YB54W001#SG12_PRESSURE_3_4", "pressure54x");
	final static Parameter steamGeneratorPressure52_entirePlant = new Parameter(
			"YB52W001#SG12_PRESSURE_3_4", "pressure52x");
	final static Parameter steamGeneratorPressure15_entirePlant = new Parameter(
			"YB15W001#SG12_PRESSURE_3_4", "pressure15x");
	final static Parameter steamGeneratorPressure13_entirePlant = new Parameter(
			"YB13W001#SG12_PRESSURE_3_4", "pressure13x");
	final static Parameter steamGeneratorPressure11_entirePlant = new Parameter(
			"YB11W001#SG12_PRESSURE_3_4", "pressure11x");
	final static Parameter reacRelPower_entirePlant = new Parameter(
			"YC00B001#NR1_POWER", "reac_rel_power", 0.1, 0.95, 1.0, 1.1);
	final static Parameter pressureUpperPlenum_entirePlant = new Parameter(
			"YC00J030#TA11_PRESSURE", "pressure_upper_plenum", 10.8, 13.4);
	final static Parameter tempUpperPlenum_entirePlant = new Parameter(
			"YC00J030#TA11_TEMPERATURE", "temp_upper_plenum", 180.0, 317.0);
	final static Parameter tripSignal = new Parameter(
			"YZ10U404FL01#FF_OUTPUT_VALUE", "trip", 0.5);

	final static Parameter prot6valveA11open = new Parameter(
			"RA11S003_VA1#VO_OPEN", "valveA11open", 0.5);
	final static Parameter prot6valveA11close = new Parameter(
			"RA11S003_VA1#VO_CLOSE", "valveA11close", 0.5);
	final static Parameter prot6valveA52open = new Parameter(
			"RA52S003_VA1#VO_OPEN", "valveA52open", 0.5);
	final static Parameter prot6valveA52close = new Parameter(
			"RA52S003_VA1#VO_CLOSE", "valveA52close", 0.5);
	final static Parameter prot6valveA13open = new Parameter(
			"RA13S003_VA1#VO_OPEN", "valveA13open", 0.5);
	final static Parameter prot6valveA13close = new Parameter(
			"RA13S003_VA1#VO_CLOSE", "valveA13close", 0.5);
	final static Parameter prot6valveA54open = new Parameter(
			"RA54S003_VA1#VO_OPEN", "valveA54open", 0.5);
	final static Parameter prot6valveA54close = new Parameter(
			"RA54S003_VA1#VO_CLOSE", "valveA54close", 0.5);
	final static Parameter prot6valveA15open = new Parameter(
			"RA15S003_VA1#VO_OPEN", "valveA15open", 0.5);
	final static Parameter prot6valveA15close = new Parameter(
			"RA15S003_VA1#VO_CLOSE", "valveA15close", 0.5);
	final static Parameter prot6valveA56open = new Parameter(
			"RA56S003_VA1#VO_OPEN", "valveA56open", 0.5);
	final static Parameter prot6valveA56close = new Parameter(
			"RA56S003_VA1#VO_CLOSE", "valveA56close", 0.5);
	final static Parameter prot6valveL31open = new Parameter(
			"RL31S003_VA1#VO_OPEN", "valveL31open", 0.5);
	final static Parameter prot6valveL31close = new Parameter(
			"RL31S003_VA1#VO_CLOSE", "valveL31close", 0.5);
	final static Parameter prot6valveL72open = new Parameter(
			"RL72S003_VA1#VO_OPEN", "valveL72open", 0.5);
	final static Parameter prot6valveL72close = new Parameter(
			"RL72S003_VA1#VO_CLOSE", "valveL72close", 0.5);
	final static Parameter prot6valveL33open = new Parameter(
			"RL33S003_VA1#VO_OPEN", "valveL33open", 0.5);
	final static Parameter prot6valveL33close = new Parameter(
			"RL33S003_VA1#VO_CLOSE", "valveL33close", 0.5);
	final static Parameter prot6valveL74open = new Parameter(
			"RL74S003_VA1#VO_OPEN", "valveL74open", 0.5);
	final static Parameter prot6valveL74close = new Parameter(
			"RL74S003_VA1#VO_CLOSE", "valveL74close", 0.5);
	final static Parameter prot6valveL35open = new Parameter(
			"RL35S003_VA1#VO_OPEN", "valveL35open", 0.5);
	final static Parameter prot6valveL35close = new Parameter(
			"RL35S003_VA1#VO_CLOSE", "valveL35close", 0.5);
	final static Parameter prot6valveL76open = new Parameter(
			"RL76S003_VA1#VO_OPEN", "valveL76open", 0.5);
	final static Parameter prot6valveL76close = new Parameter(
			"RL76S003_VA1#VO_CLOSE", "valveL76close", 0.5);
	
	final static Configuration CONFIGURATION_PROTECTION6 = new Configuration(
				1.0, Arrays.asList(
				liveSteamPressure_entirePlant), Arrays.asList(
				prot6valveA11open, prot6valveA11close,
				prot6valveA52open, prot6valveA52close,
				prot6valveA13open, prot6valveA13close,
				prot6valveA54open, prot6valveA54close,
				prot6valveA15open, prot6valveA15close,
				prot6valveA56open, prot6valveA56close,
				prot6valveL31open, prot6valveL31close,
				prot6valveL72open, prot6valveL72close,
				prot6valveL33open, prot6valveL33close,
				prot6valveL74open, prot6valveL74close,
				prot6valveL35open, prot6valveL35close,
				prot6valveL76open, prot6valveL76close));

	final static Parameter coolantPumpStopped51 = new Parameter(
			"SK00C010XG51#BINARY_VALUE", "coolantPumpStopped51", 0.5);
	final static Parameter coolantPumpStopped52 = new Parameter(
			"SK00C010XG52#BINARY_VALUE", "coolantPumpStopped52", 0.5);
	final static Parameter coolantPumpStopped53 = new Parameter(
			"SK00C010XG53#BINARY_VALUE", "coolantPumpStopped53", 0.5);
	final static Parameter coolantPumpStopped54 = new Parameter(
			"SK00C010XG54#BINARY_VALUE", "coolantPumpStopped54", 0.5);
	final static Parameter coolantPumpStopped55 = new Parameter(
			"SK00C010XG55#BINARY_VALUE", "coolantPumpStopped55", 0.5);
	final static Parameter coolantPumpStopped56 = new Parameter(
			"SK00C010XG56#BINARY_VALUE", "coolantPumpStopped56", 0.5);
	final static Parameter rodPosition = new Parameter(
			"YC00B001_RA1#RA_RE_RODP2", "rodPosition", 1.0, 2.0);

	final static Configuration CONFIGURATION_REA_TUR_TRIP = new Configuration(
			1.0, Arrays.asList(
				liveSteamPressure_entirePlant,
				reacRelPower_entirePlant,
				tempUpperPlenum_entirePlant,
				pressureUpperPlenum_entirePlant,
				pressurizerWaterLevel_entirePlant,
				steamGeneratorLevel56_entirePlant,
				steamGeneratorLevel54_entirePlant,
				steamGeneratorLevel52_entirePlant,
				steamGeneratorLevel15_entirePlant,
				steamGeneratorLevel13_entirePlant,
				steamGeneratorLevel11_entirePlant,
				coolantPumpStopped51,
				coolantPumpStopped52,
				coolantPumpStopped53,
				coolantPumpStopped54,
				coolantPumpStopped55,
				coolantPumpStopped56
			), Arrays.asList(rodPosition));
	// there is also YZU001XL48#BINARY_VALUE from protection6,
	// but it just states that liveSteamPressure < 3


	final static Configuration CONFIGURATION_PLANT = new Configuration(
			1.0,
			Arrays.asList(
					pressurizerWaterLevel_entirePlant,
					pressureInLowerPlenum_entirePlant,
					liveSteamPressure_entirePlant,
					busbarVoltage_entirePlant,
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
					reacRelPower_entirePlant,
					pressureUpperPlenum_entirePlant,
					tempUpperPlenum_entirePlant
				), Arrays.asList(tripSignal));
	
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
	
	final static Parameter binSigFromReaPowLimit_reacco = new Parameter(
			"YK00_ROMXL01#BINARY_VALUE", "bin_sig_rea_pow_limit", 0.5);
	final static Parameter anSigFromReaPowLimit_reacco = new Parameter(
			"YK00_ROMXJ35#ANALOG_VALUE", "an_sig_rea_pow_limit", 1.0, 2.0);
	final static Parameter rodPosition_reacco = new Parameter(
			"YC00B001_RA1#RA_RE_RODP", "rod_position", 1.0, 2.0);
	
	final static Configuration CONFIGURATION_REACTOR_CO = new Configuration(
			1.0,
			Arrays.asList(
					binSigFromReaPowLimit_reacco,
					liveSteamPressure_entirePlant,
					anSigFromReaPowLimit_reacco,
					reacRelPower_entirePlant
			), Arrays.asList(rodPosition_reacco));
	
	final static Parameter YA11T001_preslevco = new Parameter(
			"YA11T001#ME_OUTPUT_VALUE", "YA11T001", 270.0);
	final static Parameter YA11T002_preslevco = new Parameter(
			"YA11T002#ME_OUTPUT_VALUE", "YA11T002", 270.0);
	final static Parameter YA12T001_preslevco = new Parameter(
			"YA12T001#ME_OUTPUT_VALUE", "YA12T001", 270.0);
	final static Parameter YA12T002_preslevco = new Parameter(
			"YA12T002#ME_OUTPUT_VALUE", "YA12T002", 270.0);
	final static Parameter YA13T001_preslevco = new Parameter(
			"YA13T001#ME_OUTPUT_VALUE", "YA13T001", 270.0);
	final static Parameter YA13T002_preslevco = new Parameter(
			"YA13T002#ME_OUTPUT_VALUE", "YA13T002", 270.0);
	final static Parameter YA14T001_preslevco = new Parameter(
			"YA14T001#ME_OUTPUT_VALUE", "YA14T001", 270.0);
	final static Parameter YA14T002_preslevco = new Parameter(
			"YA14T002#ME_OUTPUT_VALUE", "YA14T002", 270.0);
	final static Parameter YA15T001_preslevco = new Parameter(
			"YA15T001#ME_OUTPUT_VALUE", "YA15T001", 270.0);
	final static Parameter YA15T002_preslevco = new Parameter(
			"YA15T002#ME_OUTPUT_VALUE", "YA15T002", 270.0);
	final static Parameter YA16T001_preslevco = new Parameter(
			"YA16T001#ME_OUTPUT_VALUE", "YA16T001", 270.0);
	final static Parameter YA16T002_preslevco = new Parameter(
			"YA16T002#ME_OUTPUT_VALUE", "YA16T002", 270.0);
	
	final static Parameter valveE51_preslevco = new Parameter(
			"TE51S002_VA1#V_POSITION_SET_VALUE", "valveE51", 0.5);
	final static Parameter valveK52_preslevco = new Parameter(
			"TK52S002_VA1#V_POSITION_SET_VALUE", "valveK52", 0.5);
	final static Parameter valveK53_preslevco = new Parameter(
			"TK53S002_VA1#V_POSITION_SET_VALUE", "valveK53", 0.5);
	
	final static Configuration CONFIGURATION_PRES_LEV_CONT = new Configuration(
			1.0,
			Arrays.asList(
				YA11T001_preslevco, YA11T002_preslevco,
				YA12T001_preslevco, YA12T002_preslevco,
				YA13T001_preslevco, YA13T002_preslevco,
				YA14T001_preslevco, YA14T002_preslevco,
				YA15T001_preslevco, YA15T002_preslevco,
				YA16T001_preslevco, YA16T002_preslevco
			), Arrays.asList(valveE51_preslevco,
					valveK52_preslevco, valveK53_preslevco));
	
	final static Parameter pressurizerPressure_prespresco = new Parameter(
			"YP10B001_NO8#NO6_PRESSURE", "pressurizer_pressure", 8e6, 9e6, 10e6, 11e6, 12e6, 13e6);
	final static Parameter power_prespresco = new Parameter(
			"YP10B001_HS1#HS_POWER", "power", 0.5);
	final static Parameter valve1311_prespresco = new Parameter(
			"YP13S011_VA1#V_POSITION_SET_VALUE", "valve1311", 0.5);
	final static Parameter valve1411_prespresco = new Parameter(
			"YP14S011_VA1#V_POSITION_SET_VALUE", "valve1411", 0.5);
	final static Parameter valve1308_prespresco = new Parameter(
			"YP13S008_VA1#V_POSITION_SET_VALUE", "valve1308", 0.5);
	final static Parameter valve1408_prespresco = new Parameter(
			"YP14S008_VA1#V_POSITION_SET_VALUE", "valve1408", 0.5);
	final static Parameter valve1305_prespresco = new Parameter(
			"YP13S005_VA1#V_POSITION_SET_VALUE", "valve1305", 0.5);
	final static Parameter valve1405_prespresco = new Parameter(
			"YP14S005_VA1#V_POSITION_SET_VALUE", "valve1405", 0.5);
	final static Parameter valve1302_prespresco = new Parameter(
			"YP13S002_VA1#V_POSITION_SET_VALUE", "valve1302", 0.5);
	final static Parameter valve1402_prespresco = new Parameter(
			"YP14S002_VA1#V_POSITION_SET_VALUE", "valve1402", 0.5);
	
	final static Configuration CONFIGURATION_PRES_PRES_CONT = new Configuration(
			1.0,
			Arrays.asList(
					pressurizerWaterLevel_entirePlant,
					pressurizerPressure_prespresco
			), Arrays.asList(power_prespresco,
					valve1311_prespresco,
					valve1411_prespresco,
					valve1308_prespresco,
					valve1408_prespresco,
					valve1305_prespresco,
					valve1405_prespresco,
					valve1302_prespresco,
					valve1402_prespresco));

	private final static Configuration CONFIGURATION = CONFIGURATION_PROTECTION1;

	private final static String OUTPUT_TRACE_FILENAME = "evaluation/plant-synthesis/vver.sc";
	private final static String OUTPUT_ACTIONSPEC_FILENAME = "evaluation/plant-synthesis/vver.actionspec";
	private final static String OUTPUT_LTL_FILENAME = "evaluation/plant-synthesis/vver.ltl";
	
	public static List<String> generateScenarios(Configuration conf, Dataset ds, Set<List<String>> allActionCombinations,
			String gvOutput, String smvOutput, String binOutput, boolean addActionDescriptions) throws FileNotFoundException {
		// traces
		final Set<String> allEvents = new TreeSet<>();
		
		// coverage
		final Set<Pair<String, Integer>> inputCovered = new HashSet<>();
		final Set<Pair<String, Integer>> outputCovered = new HashSet<>();
		int totalInputValues = 0;
		int totalOutputValues = 0;
		for (Parameter p : conf.inputParameters) {
			totalInputValues += p.cutoffs.size();
		}
		for (Parameter p : conf.outputParameters) {
			totalOutputValues += p.cutoffs.size();
		}
		
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_TRACE_FILENAME))) {
			for (List<double[]> trace : ds.values) {
				final List<String> events = new ArrayList<>();
				final List<String> actions = new ArrayList<>();

				for (double[] snapshot : trace) {
					final StringBuilder event = new StringBuilder("A");
					final List<String> thisActions = new ArrayList<>();
					
					for (Parameter p : conf.inputParameters) {
						final double value = ds.get(snapshot, p);
						final int index = p.traceNameIndex(value);
						inputCovered.add(Pair.of(p.aprosName, index));
						event.append(index);
					}
					
					for (Parameter p : conf.outputParameters) {
						final double value = ds.get(snapshot, p);
						final int index = p.traceNameIndex(value);
						outputCovered.add(Pair.of(p.aprosName, index));
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
			for (Parameter p : conf.outputParameters) {
				for (String str : p.actionspec()) {
					pw.println(str);
				}
			}
		}

		// temporal properties
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_LTL_FILENAME))) {
			for (Parameter p : conf.outputParameters) {
				for (String str : p.temporalProperties()) {
					pw.println(str);
				}
			}
		}

		// all actions
		final List<String> allActions = new ArrayList<>();
		final List<String> allActionDescriptions = new ArrayList<>();
		for (Parameter p : conf.outputParameters) {
			allActions.addAll(p.traceNames());
			allActionDescriptions.addAll(p.descriptions());
		}

		// execution command
		final int recommendedSize = allActionCombinations.size();
		final String nl = " \\\n";

		System.out.println("Run:");
		
		final List<String> builderArgs = new ArrayList<>();
		builderArgs.add(OUTPUT_TRACE_FILENAME);
		builderArgs.add("--actionNames");
		builderArgs.add(String.join(",", allActions));
		if (addActionDescriptions) {
			builderArgs.add("--actionDescriptions");
			builderArgs.add(String.join(",", allActionDescriptions));
		}
		builderArgs.add("--colorRules");
		builderArgs.add(String.join(",", conf.colorRules));
		builderArgs.add("--actionNumber");
		builderArgs.add(String.valueOf(allActions.size()));
		builderArgs.add("--eventNames");
		builderArgs.add(String.join(",", allEvents));
		builderArgs.add("--eventNumber");
		builderArgs.add(String.valueOf(allEvents.size()));
		if (recommendedSize > 10) {
			builderArgs.add("--fast");
			System.out.println("# LTL disabled: estimated state number is too large");
		} else {
			builderArgs.add("--ltl");
			builderArgs.add(OUTPUT_LTL_FILENAME);
		}
		builderArgs.add("--actionspec");
		builderArgs.add(OUTPUT_ACTIONSPEC_FILENAME);
		builderArgs.add("--size");
		builderArgs.add(String.valueOf(recommendedSize));
		builderArgs.add("--varNumber");
		builderArgs.add("0");
		builderArgs.add("--result");
		builderArgs.add(gvOutput);
		builderArgs.add("--nusmv");
		builderArgs.add(smvOutput);
		builderArgs.add("--serialize");
		builderArgs.add(binOutput);

		System.out.print("java -jar jars/plant-automaton-generator.jar ");
		for (String arg : builderArgs) {
			if (arg.startsWith("--")) {
				System.out.print(nl + " " + arg + " ");
			} else {
				System.out.print("\"" + arg + "\"");
			}
		}
		System.out.println();

		// parameter limits
		System.out.println("Found parameter boundaries:");
		final Function<Parameter, String> describe = p -> {
			return p.traceName + " in " + p.limits() + ", bounds " + p.cutoffs.subList(0, p.cutoffs.size() - 1);
		};
		for (Parameter p : conf.outputParameters) {
			System.out.println(" output " + describe.apply(p));
		}
		for (Parameter p : conf.inputParameters) {
			System.out.println(" input " + describe.apply(p));
		}
		
		System.out.println(String.format("Input coverage: %.2f%%", 100.0 * inputCovered.size() / totalInputValues));
		System.out.println(String.format("Output coverage: %.2f%%", 100.0 * outputCovered.size() / totalOutputValues));
		
		return builderArgs;
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		final long time = System.currentTimeMillis();
		final Dataset ds = new Dataset(CONFIGURATION.intervalSec);
		generateScenarios(CONFIGURATION, ds, new HashSet<>(),
				"automaton.gv", "automaton.smv", "automaton.bin", true);
		System.out.println("Execution time: " + (System.currentTimeMillis() - time) + " ms");
	}
}
