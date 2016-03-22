package main.plant;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;

import main.plant.AprosIOScenarioCreator.Configuration;
import main.plant.AprosIOScenarioCreator.Dataset;
import main.plant.AprosIOScenarioCreator.Parameter;
import scenario.StringActions;
import structures.plant.MooreNode;
import structures.plant.MooreTransition;
import structures.plant.NondetMooreAutomaton;

public class CompositionalBuilder {
	final static Configuration CONF1 = new Configuration(
			1.0, Arrays.asList(
			AprosIOScenarioCreator.pressureInLowerPlenum),
			Arrays.asList(AprosIOScenarioCreator.tripSignal));
	
	final static Configuration CONF2 = new Configuration(
			1.0, Arrays.asList(AprosIOScenarioCreator.pressurizerWaterLevel),
			Arrays.asList(AprosIOScenarioCreator.tripSignal));
	
	//private final static Configuration CONF1 = AprosIOScenarioCreator.CONFIGURATION_PROTECTION1;
	//private final static Configuration CONF2 = AprosIOScenarioCreator.CONFIGURATION_PROTECTION7;
	
	static class StatePair {
		final MooreNode first;
		final MooreNode second;
		
		public StatePair(MooreNode first, MooreNode second) {
			this.first = first;
			this.second = second;
		}
		
		boolean isConsistent(Match match) {
			final String[] actions1 = first.actions().getActions();
			final String[] actions2 = second.actions().getActions();
			for (Pair<Parameter, Parameter> pair : match.outputPairs) {
				final String prefix = pair.getLeft().traceNamePrefix();
				final int i1 = actionIntervalIndex(actions1, prefix);
				final int i2 = actionIntervalIndex(actions2, prefix);
				if (i1 != i2) {
					return false;
				}
			}
			return true;
		}
		
		Set<String> actionSet() {
			final Set<String> actions = new TreeSet<>();
			for (String action : first.actions().getActions()) {
				actions.add(action);
			}
			for (String action : second.actions().getActions()) {
				actions.add(action);
			}
			return actions;
		}
		
		Set<String> actionSet(Match match) {
			final Set<String> actions = actionSet();
			
			// remove internal connections
			final Set<String> removing = new TreeSet<>();
			l: for (String a : actions) {
				for (Pair<Parameter, Integer> p : match.outputInputPairs) {
					if (isProperAction(a, p.getLeft().traceNamePrefix())) {
						removing.add(a);
						continue l;
					}
				}
				for (Pair<Integer, Parameter> p : match.inputOutputPairs) {
					if (isProperAction(a, p.getRight().traceNamePrefix())) {
						removing.add(a);
						continue l;
					}
				}
			}
			
			actions.removeAll(removing);
			return actions;
		}
		
		boolean isPresentInTraces(Set<List<String>> allActionCombinationsSorted) {
			final List<String> actions = new ArrayList<>(actionSet());
			return allActionCombinationsSorted.contains(actions);
		}
		
		MooreNode toMooreNode(int number, Match match) {
			final Set<String> actionSet = actionSet(match);
			return new MooreNode(number, new StringActions(actionSet));
		}
	}
	
	// assuming that we have at most 10 intervals
	static boolean isProperAction(String action, String prefix) {
		return action.startsWith(prefix)
				&& action.length() == prefix.length() + 1
				&& Character.isDigit(action.charAt(action.length() - 1));
	}
	
	static int actionIntervalIndex(String[] actions, String prefix) {
		int i;
		for (i = 0; i < actions.length; i++) {
			if (isProperAction(actions[i], prefix)) {
				break;
			}
		}
		return Integer.parseInt(String.valueOf(actions[i].charAt(actions[i].length() - 1)));
	}
	
	static boolean isConsistentWithInputs(MooreNode node, String outgoingEvent,
			Match match, boolean isOutputInput) {
		if (isOutputInput) {
			for (Pair<Parameter, Integer> pair : match.outputInputPairs) {
				final int firstIndex = actionIntervalIndex(node.actions().getActions(),
						pair.getLeft().traceNamePrefix());
				final int secondIndex = Integer.parseInt(String.valueOf(
						outgoingEvent.charAt(pair.getRight() + 1)));
				if (firstIndex != secondIndex) {
					return false;
				}
			}
		} else {
			for (Pair<Integer, Parameter> pair : match.inputOutputPairs) {
				final int secondIndex = actionIntervalIndex(node.actions().getActions(),
						pair.getRight().traceNamePrefix());
				final int firstIndex = Integer.parseInt(String.valueOf(
						outgoingEvent.charAt(pair.getLeft() + 1)));
				if (firstIndex != secondIndex) {
					return false;
				}
			}
		}
		return true;
	}
	
	private static void compose(NondetMooreAutomaton a1, NondetMooreAutomaton a2, Match match,
			Set<List<String>> allActionCombinationsSorted) throws FileNotFoundException {
		final List<MooreNode> compositeStates = new ArrayList<>();
		
		final Deque<Pair<StatePair, MooreNode>> q = new ArrayDeque<>();
		final Map<Set<String>, MooreNode> allEnqueudOutputCombinations = new HashMap<>();
		for (int initial1 : a1.initialStates()) {
			for (int initial2 : a2.initialStates()) {
				final MooreNode state1 = a1.state(initial1);
				final MooreNode state2 = a2.state(initial2);
				final StatePair p = new StatePair(state1, state2);
				if (p.isConsistent(match) && p.isPresentInTraces(allActionCombinationsSorted)) {
					final MooreNode node = p.toMooreNode(compositeStates.size(), match);
					allEnqueudOutputCombinations.put(p.actionSet(match), node);
					q.add(Pair.of(p, node));
					compositeStates.add(node);
				}
			}
		}
		final int initialStateNum = q.size();
		
		while (!q.isEmpty()) {
			final Pair<StatePair, MooreNode> retrieved = q.removeLast();
			final StatePair pair = retrieved.getLeft();
			final MooreNode src = retrieved.getRight();
			
			for (MooreTransition t1 : pair.first.transitions()) {
				final String e1 = t1.event();
				
				l: for (MooreTransition t2 : pair.second.transitions()) {
					final String e2 = t2.event();

					// **** The event must be consistent with the NEXT state
					// **** If it is required to be consistent with the FIRST state,
					// **** then there are some semantical problems
					
					// internal connection consistency
					if (!isConsistentWithInputs(t2.dst(), e1, match, false)) {
						continue;
					}
					if (!isConsistentWithInputs(t1.dst(), e2, match, true)) {
						continue;
					}
					
					for (Pair<Integer, Integer> ip : match.inputPairs) {
						if (e1.charAt(ip.getLeft() + 1) != e2.charAt(ip.getRight() + 1)) {
							continue l;
						}
					}
					
					final StatePair p = new StatePair(t1.dst(), t2.dst());
					if (p.isConsistent(match) && p.isPresentInTraces(allActionCombinationsSorted)) {
						final Set<Integer> badSecondIndices = new TreeSet<>();
						final Set<Integer> badFirstIndices = new TreeSet<>();
						
						// duplicate indices
						for (Pair<Integer, Integer> ip : match.inputPairs) {
							badSecondIndices.add(ip.getRight());
						}
						for (Pair<Parameter, Integer> oip : match.outputInputPairs) {
							badSecondIndices.add(oip.getRight());
						}
						for (Pair<Integer, Parameter> iop : match.inputOutputPairs) {
							badFirstIndices.add(iop.getLeft());
						}
							
						final StringBuilder event = new StringBuilder("A");
						for (int i = 1; i < e1.length(); i++) {
							if (!badFirstIndices.contains(i - 1)) {
								event.append(e1.charAt(i));
							}
						}
						for (int i = 1; i < e2.length(); i++) {
							if (!badSecondIndices.contains(i - 1)) {
								event.append(e2.charAt(i));
							}
						}
						
						final Set<String> actionSet = p.actionSet(match);
						MooreNode dst = allEnqueudOutputCombinations.get(actionSet);
						if (dst == null) {
							dst = p.toMooreNode(compositeStates.size(), match);
							q.add(Pair.of(p, dst));
							compositeStates.add(dst);
							allEnqueudOutputCombinations.put(actionSet, dst);
						}
						
						final String e = event.toString();
						if (!src.allDst(e).contains(dst)) {
							src.addTransition(event.toString(), dst);
						}
					}
				}
			}
		}

		/*
		 * queue Q <- all consistent pairs of initial states
		 * while !Q.isEmpty()
		 *   q = (q_1, q_2) <- Q
		 *   foreach pair of consistent outgoing inputs
		 *   	if there is no internal connection conflict
		 *         (current output and outgoing transition input
		 *   	   AND the destination present in the entire trace set
		 *   		 Q <- q
		 * remove internal connections (duplicate inputs and outputs)
		 */
		
		final List<Boolean> isInitial = new ArrayList<>();
		isInitial.addAll(Collections.nCopies(initialStateNum, true));
		isInitial.addAll(Collections.nCopies(compositeStates.size() - initialStateNum, false));
		final NondetMooreAutomaton result = new NondetMooreAutomaton(compositeStates, isInitial);
		System.out.println(result);
		try (PrintWriter pw = new PrintWriter("automaton.gv")) {
			pw.println(result);
		}
	}
	
	private static Configuration outputConfigurationComposition(Configuration c1, Configuration c2) {
		final List<Parameter> outputParams = new ArrayList<>(c1.outputParameters);
		final Set<String> allParamNames = new HashSet<>();
		for (Parameter p : c1.outputParameters) {
			allParamNames.add(p.aprosName());
		}
		for (Parameter p : c2.outputParameters) {
			if (allParamNames.add(p.aprosName())) {
				outputParams.add(p);
			}
		}
		return new Configuration(c1.intervalSec, outputParams, Collections.emptyList());
	}
	
	static class Match {
		final List<Pair<Parameter, Parameter>> outputPairs = new ArrayList<>();
		final List<Pair<Integer, Integer>> inputPairs = new ArrayList<>();
		final List<Pair<Integer, Parameter>> inputOutputPairs = new ArrayList<>();
		final List<Pair<Parameter, Integer>> outputInputPairs = new ArrayList<>();
	}
	
	public static void main(String[] args) throws FileNotFoundException {		
		// 1. Unify parameters
		final Match match = new Match();

		
		for (Parameter p : CONF1.outputParameters) {
			for (Parameter q : CONF2.outputParameters) {
				if (Parameter.unify(p, q)) {
					match.outputPairs.add(Pair.of(p, q));
				}
			}
		}
		for (int i = 0; i < CONF1.inputParameters.size(); i++) {
			final Parameter p = CONF1.inputParameters.get(i);
			for (int j = 0; j < CONF2.inputParameters.size(); j++) {
				final Parameter q = CONF2.inputParameters.get(j);
				if (Parameter.unify(p, q)) {
					match.inputPairs.add(Pair.of(i, j));
				}
			}
		}
		
		for (Parameter p : CONF1.outputParameters) {
			for (int j = 0; j < CONF2.inputParameters.size(); j++) {
				final Parameter q = CONF2.inputParameters.get(j);
				if (Parameter.unify(p, q)) {
					match.outputInputPairs.add(Pair.of(p, j));
				}
			}
		}
		
		for (int i = 0; i < CONF1.inputParameters.size(); i++) {
			final Parameter p = CONF1.inputParameters.get(i);
			for (Parameter q : CONF2.outputParameters) {
				if (Parameter.unify(p, q)) {
					match.inputOutputPairs.add(Pair.of(i, q));
				}
			}
		}
		
		System.out.println(CONF1);
		System.out.println();
		System.out.println(CONF2);
		System.out.println();
		if (CONF1.intervalSec != CONF2.intervalSec) {
			System.err.println("Incompatible intervals, stopping.");
			return;
		}
		
		// 2. Load the dataset
		final Dataset ds = new Dataset(CONF1.intervalSec);
		
		// 3. Run scenario generation & automaton builders
		final List<String> params1 = AprosIOScenarioCreator.generateScenarios(CONF1,
				ds, new HashSet<>(), "automaton1.gv", "automaton1.smv", "automaton1.bin", false);
		System.out.println();
		PlantBuilderMain.main(params1.toArray(new String[params1.size()]));
		NondetMooreAutomaton a1 = null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("automaton1.bin"))) {
			a1 = (NondetMooreAutomaton) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		System.out.println();
		System.out.println(a1);
		System.out.println();
		
		final List<String> params2 = AprosIOScenarioCreator.generateScenarios(CONF2, ds,
				new HashSet<>(), "automaton2.gv", "automaton2.smv", "automaton2.bin", false);
		System.out.println();
		PlantBuilderMain.main(params2.toArray(new String[params2.size()]));
		NondetMooreAutomaton a2 = null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("automaton2.bin"))) {
			a2 = (NondetMooreAutomaton) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		System.out.println();
		System.out.println(a2);
		System.out.println();

		
		// 4. Obtain the set of all possible composite actions
		final Set<List<String>> allActionCombinations = new HashSet<>();
		AprosIOScenarioCreator.generateScenarios(outputConfigurationComposition(CONF1, CONF2),
				ds, allActionCombinations, "automaton.gv", "automaton.smv", "automaton.bin", false);
		final Set<List<String>> allActionCombinationsSorted = new HashSet<>();
		for (List<String> actionCombination : allActionCombinations) {
			final List<String> copy = new ArrayList<>(actionCombination);
			Collections.sort(copy);
			allActionCombinationsSorted.add(copy);
		}
		
		// 5. Compose
		System.out.println();
		System.out.println("Composing...");
		compose(a1, a2, match, allActionCombinationsSorted);
	}
}
