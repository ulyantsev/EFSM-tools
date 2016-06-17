package main.plant.apros;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

public class TraceTranslator {
    final static Configuration CONF_S1 = Configuration.load("s1.conf");
    final static Configuration CONF_S2 = Configuration.load("s2.conf");
    final static Configuration CONF_S3 = Configuration.load("s3.conf");
    final static Configuration CONF_S4 = Configuration.load("s4.conf");
    final static Configuration CONF_S5 = Configuration.load("s5.conf");
    final static Configuration CONF_S6 = Configuration.load("s6.conf");
    final static Configuration CONF_S7 = Configuration.load("s7.conf");
    final static Configuration CONF_S8 = Configuration.load("s8.conf");

    final static Configuration CONF_PLANT = Configuration.load("plant.conf");

	final static Parameter pressurizerWaterLevel = new RealParameter(
			"YP10B001#PR11_LIQ_LEVEL", "water_level", Pair.of(0.0, 850.0), 230.0, 280.0);
	final static Parameter pressureInLowerPlenum = new RealParameter(
			"YC00J005#TA11_PRESSURE", "pressure_lower_plenum", Pair.of(0.0, 202.0), 8.0, 35.0, 80.0, 100.0);
	final static Parameter prot5valve41open = new BoolParameter(
			"RL41S001_VA1#VO_OPEN", "valve41open");
	final static Parameter prot5valve43open = new BoolParameter(
			"RL43S001_VA1#VO_OPEN", "valve43open");
	final static Parameter reacRelPower_entirePlant = new RealParameter(
			"YC00B001#NR1_POWER", "reac_rel_power", Pair.of(0.0, 13.0), 1.0, 9.0, 10.0, 11.0);
	final static Parameter tripSignal = new BoolParameter(
			"YZ10U404FL01#FF_OUTPUT_VALUE", "trip");

	// to improve precision in the NuSMV model
	final static Map<String, Double> PARAM_SCALES = new TreeMap<>();
	static {
        try (Scanner sc = new Scanner(new File(Settings.CONF_LOCATION + "scaling.txt"))) {
            while (sc.hasNextLine()) {
                final String line = sc.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                final String[] tokens = line.split(" ");
                final String aprosName = tokens[0];
                final double scale = Double.parseDouble(tokens[1]);
                PARAM_SCALES.put(aprosName, scale);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
	}

	private final static Configuration CONFIGURATION = CONF_S1;

	private final static String OUTPUT_TRACE_FILENAME = "evaluation/plant-synthesis/vver.sc";
	private final static String OUTPUT_ACTIONSPEC_FILENAME = "evaluation/plant-synthesis/vver.actionspec";
	private final static String OUTPUT_LTL_FILENAME = "evaluation/plant-synthesis/vver.ltl";
	
	private static void allEventCombinations(char[] arr, int index, Set<String> result, List<Parameter> parameters) {
		if (index == arr.length) {
			result.add(String.valueOf(arr));
		} else {
			final int intervalNum = parameters.get(index - 1).valueCount();
			for (int i = 0; i < intervalNum; i++) {
				arr[index] = Character.forDigit(i, 10);
				allEventCombinations(arr, index + 1, result, parameters);
			}
		}
	}
	
	public static List<String> generateScenarios(Configuration conf, Dataset ds, Set<List<String>> allActionCombinations,
			String gvOutput, String smvOutput, boolean addActionDescriptions, int sizeThreshold,
			boolean allEventCombinations) throws FileNotFoundException {
		// traces
		final Set<String> allEvents = new TreeSet<>();
		if (allEventCombinations) {
			// complete event set
			final char[] arr = new char[conf.inputParameters.size() + 1];
			arr[0] = 'A';
			allEventCombinations(arr, 1, allEvents, conf.inputParameters);
		}
		
		// coverage
		final Set<Pair<String, Integer>> inputCovered = new HashSet<>();
		final Set<Pair<String, Integer>> outputCovered = new HashSet<>();
		int totalInputValues = 0;
		int totalOutputValues = 0;
		for (Parameter p : conf.inputParameters) {
			totalInputValues += p.valueCount();
		}
		for (Parameter p : conf.outputParameters) {
			totalOutputValues += p.valueCount();
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
						inputCovered.add(Pair.of(p.aprosName(), index));
						event.append(index);
					}
					
					for (Parameter p : conf.outputParameters) {
						final double value = ds.get(snapshot, p);
						final int index = p.traceNameIndex(value);
						outputCovered.add(Pair.of(p.aprosName(), index));
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
		final List<String> allActions = conf.actions();
		final List<String> allActionDescriptions = conf.actionDescriptions();

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
		if (recommendedSize > sizeThreshold) {
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
			return p.traceName() + " in " + p.limits() + " - " + p;
		};
		for (Parameter p : conf.outputParameters) {
			System.out.println(" output " + describe.apply(p));
		}
		for (Parameter p : conf.inputParameters) {
			System.out.println(" input " + describe.apply(p));
		}
		
		System.out.println(String.format("Input coverage: %.2f%%",
				100.0 * inputCovered.size() / totalInputValues));
		System.out.println(String.format("Output coverage: %.2f%%",
				100.0 * outputCovered.size() / totalOutputValues));
		
		return builderArgs;
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		final long time = System.currentTimeMillis();
		final Dataset ds = new Dataset(CONFIGURATION.intervalSec,
				Settings.TRACE_LOCATION, "", PARAM_SCALES);
		generateScenarios(CONFIGURATION, ds, new HashSet<>(),
				"automaton.gv", "automaton.smv", true, 10, false);
		System.out.println("Execution time: " + (System.currentTimeMillis() - time) + " ms");
	}
}
