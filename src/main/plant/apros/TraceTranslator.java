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
    /*final static Configuration CONF_S1 = Configuration.load(Settings.CONF_LOCATION + "s1.conf");
    final static Configuration CONF_S2 = Configuration.load(Settings.CONF_LOCATION + "s2.conf");
    final static Configuration CONF_S3 = Configuration.load(Settings.CONF_LOCATION + "s3.conf");
    final static Configuration CONF_S4 = Configuration.load(Settings.CONF_LOCATION + "s4.conf");
    final static Configuration CONF_S5 = Configuration.load(Settings.CONF_LOCATION + "s5.conf");
    final static Configuration CONF_S6 = Configuration.load(Settings.CONF_LOCATION + "s6.conf");
    final static Configuration CONF_S7 = Configuration.load(Settings.CONF_LOCATION + "s7.conf");
    final static Configuration CONF_S8 = Configuration.load(Settings.CONF_LOCATION + "s8.conf");

    final static Configuration CONF_PLANT = Configuration.load("plant.conf");*/

	// to improve precision in the NuSMV model
    public static Map<String, Double> paramScales(String filename) {
        final Map<String, Double> paramScales = new TreeMap<>();
        try (Scanner sc = new Scanner(new File(filename))) {
            while (sc.hasNextLine()) {
                final String line = sc.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                final String[] tokens = line.split(" ");
                final String aprosName = tokens[0];
                final double scale = Double.parseDouble(tokens[1]);
                paramScales.put(aprosName, scale);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return paramScales;
    }

	private final static String OUTPUT_TRACE_FILENAME = "apros.sc";
	private final static String OUTPUT_ACTIONSPEC_FILENAME = "apros.actionspec";
	private final static String OUTPUT_LTL_FILENAME = "apros.ltl";
	
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
			String gvOutput, String smvOutput, boolean addActionDescriptions, boolean satBased,
			boolean allEventCombinations, boolean coverageFiltering) throws FileNotFoundException {
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

        int addedTraces = 0;
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_TRACE_FILENAME))) {
			for (List<double[]> trace : ds.values) {
                final int initialCoveredItems = inputCovered.size() + outputCovered.size();

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

                final int finalCoveredItems = inputCovered.size() + outputCovered.size();
                if (!coverageFiltering || finalCoveredItems > initialCoveredItems) {
                    events.add(0, "");
                    events.remove(events.size() - 1);
                    pw.println(String.join("; ", events));
                    pw.println(String.join("; ", actions));
                    addedTraces++;
                }
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
        System.out.println("Traces: " + addedTraces);

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
        if (satBased) {
            builderArgs.add("--ltl");
            builderArgs.add(OUTPUT_LTL_FILENAME);
        } else {
            builderArgs.add("--fast");
            System.out.println("# LTL disabled");
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
}
