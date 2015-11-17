import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import scenario.StringActions;
import structures.plant.NondetMooreAutomaton;

public class WaterLevelGenerator {
	private static StringActions toStringActions(String action) {
		return new StringActions(action + (action.equals("aboveh") || action.equals("abovehh") ? ",sensorwet" : ",sensordry"));
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		final List<String> allActions = Arrays.asList("abovehh", "aboveh", "aboveth",
				"abovesp", "belowsp", "belowth", "belowl", "belowll");
		final Random rnd = new Random();
		for (int k = 1; k <= 5; k++) {
			// generate the model
			final List<StringActions> actions = new ArrayList<>();
			final List<Boolean> isStart = new ArrayList<>();
			for (int i = 0; i < allActions.size(); i++) {
				final String action = allActions.get(i);
				for (int j = 0; j < k; j++) {
					actions.add(toStringActions(action));
					isStart.add(action.contains("sp"));
				}
			}
			final NondetMooreAutomaton waterLevel = new NondetMooreAutomaton(8 * k, actions, isStart);
			
			for (int i = 0; i < actions.size(); i++) {
				for (int j = 0; j < actions.size(); j++) {
					final int dist = Math.abs(i - j);
					if (dist > 0 && dist <= k) {
						waterLevel.state(i).addTransition(i < j ? "closed" : "open", waterLevel.state(j));
					}
				}
			}
			
			final String dir = "qbf/plant-synthesis/";
			final String prefix = dir + "water-level-" + k;
			
			try (PrintWriter pw = new PrintWriter(new File(prefix + ".dot"))) {
				pw.println(waterLevel);
			}

			// generate scenarios
			try (PrintWriter traceWriter = new PrintWriter(new File(prefix + ".sc"))) {
				for (int i = 0; i < actions.size(); i++) {
					if (isStart.get(i)) {
						List<String> scEvents;
						List<String> scActions;
						int currentState;
						
						// up & down trace
						scEvents = new ArrayList<>();
						scActions = new ArrayList<>();
						scEvents.add("");
						scActions.add(actions.get(i).toString());
						
						currentState = i;
						while (currentState > 0) {
							int step = rnd.nextInt(k) + 1;
							currentState -= step;
							currentState = Math.max(0, currentState);
							scEvents.add("open");
							scActions.add(actions.get(currentState).toString());
						}
						while (currentState < actions.size() - 1) {
							int step = rnd.nextInt(k) + 1;
							currentState += step;
							currentState = Math.min(actions.size() - 1, currentState);
							scEvents.add("closed");
							scActions.add(actions.get(currentState).toString());
						}
						
						traceWriter.println(String.join("; ", scEvents));
						traceWriter.println(String.join("; ", scActions));
						
						// down & up trace
						scEvents = new ArrayList<>();
						scActions = new ArrayList<>();
						scEvents.add("");
						scActions.add(actions.get(i).toString());
						
						currentState = i;
						while (currentState < actions.size() - 1) {
							int step = rnd.nextInt(k) + 1;
							currentState += step;
							currentState = Math.min(actions.size() - 1, currentState);
							scEvents.add("closed");
							scActions.add(actions.get(currentState).toString());
						}
						while (currentState > 0) {
							int step = rnd.nextInt(k) + 1;
							currentState -= step;
							currentState = Math.max(0, currentState);
							scEvents.add("open");
							scActions.add(actions.get(currentState).toString());
						}
						
						traceWriter.println(String.join("; ", scEvents));
						traceWriter.println(String.join("; ", scActions));
					}
				}
			}
			
			// generate LTL properties
			try (PrintWriter ltlWriter = new PrintWriter(new File(prefix + ".ltl"))) {
				try (Scanner sc = new Scanner(new File(dir + "water-level-basic.ltl"))) {
					while (sc.hasNextLine()) {
						ltlWriter.println(sc.nextLine());
					}
				}
				for (int i = 0; i < allActions.size() - 1; i++) {
					final String firstAction = allActions.get(i);
					final String secondAction = allActions.get(i + 1);
					final List<String> parts = new ArrayList<>();
					for (int j = 0; j < k; j++) {
						parts.add(xWrap(firstAction, j, true));
						parts.add(xWrap("closed", j + 1, false));
					}
					final String formula = "G(!(" + String.join(" && ", parts)
							+ ") || " + xWrap(secondAction, k, true) + ")";
					ltlWriter.println(formula);
				}
				for (int i = allActions.size() - 1; i > 0; i--) {
					final String firstAction = allActions.get(i);
					final String secondAction = allActions.get(i - 1);
					final List<String> parts = new ArrayList<>();
					for (int j = 0; j < k; j++) {
						parts.add(xWrap(firstAction, j, true));
						parts.add(xWrap("open", j + 1, false));
					}
					final String formula = "G(!(" + String.join(" && ", parts)
							+ ") || " + xWrap(secondAction, k, true) + ")";
					ltlWriter.println(formula);
				}
			}
		}
	}
	
	private static String xWrap(String str, int times, boolean wasAction) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < times; i++) {
			sb.append("X(");
		}
		sb.append((wasAction ? "action" : "event") + "(" + str + ")");
		for (int i = 0; i < times; i++) {
			sb.append(")");
		}
		return sb.toString();
	}
}
