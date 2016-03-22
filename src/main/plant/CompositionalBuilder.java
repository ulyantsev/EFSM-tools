package main.plant;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
	private final static Configuration CONF1 = AprosIOScenarioCreator.CONFIGURATION_PROTECTION1;
	private final static Configuration CONF2 = AprosIOScenarioCreator.CONFIGURATION_PROTECTION7;
	
	static class StatePair {
		final MooreNode first;
		final MooreNode second;
		
		public StatePair(MooreNode first, MooreNode second) {
			this.first = first;
			this.second = second;
		}
		
		// assuming that we have at most 10 intervals
		private boolean isProperAction(String action, String prefix) {
			return action.startsWith(prefix)
					&& action.length() == prefix.length() + 1
					&& Character.isDigit(action.charAt(action.length() - 1));
		}
		
		private int actionIntervalIndex(String[] actions, String prefix) {
			int i;
			for (i = 0; i < actions.length; i++) {
				if (isProperAction(actions[i], prefix)) {
					break;
				}
			}
			return Integer.parseInt(String.valueOf(actions[i].charAt(actions[i].length() - 1)));
		}
		
		boolean isConsistent(List<Pair<Parameter, Parameter>> matchingOutputPairs) {
			final String[] actions1 = first.actions().getActions();
			final String[] actions2 = second.actions().getActions();
			for (Pair<Parameter, Parameter> pair : matchingOutputPairs) {
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
		
		boolean isPresentInTraces(Set<List<String>> allActionCombinationsSorted) {
			final List<String> actions = new ArrayList<>(actionSet());
			return allActionCombinationsSorted.contains(actions);
		}
		
		MooreNode toMooreNode(int number) {
			return new MooreNode(number, new StringActions(actionSet()));
		}
	}
	
	private static void compose(NondetMooreAutomaton a1, NondetMooreAutomaton a2,
			List<Pair<Parameter, Parameter>> matchingOutputPairs,
			List<Pair<Integer, Integer>> matchingInputPairs,
			Set<List<String>> allActionCombinationsSorted) {
		final List<MooreNode> compositeStates = new ArrayList<>();
		
		final Deque<Pair<StatePair, MooreNode>> q = new ArrayDeque<>();
		final Map<Set<String>, MooreNode> allEnqueudOutputCombinations = new HashMap<>();
		for (int initial1 : a1.initialStates()) {
			for (int initial2 : a2.initialStates()) {
				final MooreNode state1 = a1.state(initial1);
				final MooreNode state2 = a2.state(initial2);
				final StatePair p = new StatePair(state1, state2);
				if (p.isConsistent(matchingOutputPairs) && p.isPresentInTraces(allActionCombinationsSorted)) {
					final MooreNode node = p.toMooreNode(compositeStates.size());
					allEnqueudOutputCombinations.put(p.actionSet(), node);
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
				l: for (MooreTransition t2 : pair.second.transitions()) {
					final String e1 = t1.event();
					final String e2 = t2.event();
					for (Pair<Integer, Integer> ip : matchingInputPairs) {
						if (e1.charAt(ip.getLeft() + 1) != e2.charAt(ip.getRight() + 1)) {
							continue l;
						}
					}
					
					// TODO add check: there is no internal connection conflict
					
					final StatePair p = new StatePair(t1.dst(), t2.dst());
					if (p.isConsistent(matchingOutputPairs) && p.isPresentInTraces(allActionCombinationsSorted)) {
						final StringBuilder event = new StringBuilder(e1);
						final Set<Integer> matchingSecondIndices = new TreeSet<>();
						for (Pair<Integer, Integer> ip : matchingInputPairs) {
							matchingSecondIndices.add(ip.getRight());
						}
						for (int i = 1; i < e2.length(); i++) {
							if (!matchingSecondIndices.contains(i - 1)) {
								event.append(e2.charAt(i));
							}
						}
						
						final Set<String> actionSet = p.actionSet();
						MooreNode dst = allEnqueudOutputCombinations.get(actionSet);
						if (dst == null) {
							dst = p.toMooreNode(compositeStates.size());
							q.add(Pair.of(p, dst));
							compositeStates.add(dst);
							allEnqueudOutputCombinations.put(actionSet, dst);
						}
						
						src.addTransition(event.toString(), dst);
					}
				}
			}
		}
		// TODO remove internal connections (duplicate inputs and outputs)

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
	
	public static void main(String[] args) throws FileNotFoundException {		
		// 1. Unify parameters
		final List<Pair<Parameter, Parameter>> matchingOutputPairs = new ArrayList<>();
		final List<Pair<Integer, Integer>> matchingInputPairs = new ArrayList<>();

		for (Parameter p : CONF1.outputParameters) {
			for (Parameter q : CONF2.outputParameters) {
				if (Parameter.unify(p, q)) {
					matchingOutputPairs.add(Pair.of(p, q));
				}
			}
		}
		for (int i = 0; i < CONF1.inputParameters.size(); i++) {
			final Parameter p = CONF1.inputParameters.get(i);
			for (int j = 0; j < CONF2.inputParameters.size(); j++) {
				final Parameter q = CONF2.inputParameters.get(j);
				if (Parameter.unify(p, q)) {
					matchingInputPairs.add(Pair.of(i, j));
				}
			}
		}
		
		for (Parameter p : CONF1.outputParameters) {
			for (Parameter q : CONF2.inputParameters) {
				Parameter.unify(p, q);
			}
		}
		
		for (Parameter p : CONF1.inputParameters) {
			for (Parameter q : CONF2.outputParameters) {
				Parameter.unify(p, q);
			}
		}
		
		// FIXME add support for matching inputs and outputs
		
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
		final List<String> params1 = AprosIOScenarioCreator.generateScenarios(CONF1, ds);
		System.out.println();
		PlantBuilderMain.main(params1.toArray(new String[params1.size()]));
		NondetMooreAutomaton a1 = null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("automaton.bin"))) {
			a1 = (NondetMooreAutomaton) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		System.out.println();
		System.out.println(a1);
		System.out.println();
		
		final List<String> params2 = AprosIOScenarioCreator.generateScenarios(CONF2, ds);
		System.out.println();
		PlantBuilderMain.main(params2.toArray(new String[params2.size()]));
		NondetMooreAutomaton a2 = null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("automaton.bin"))) {
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
				ds, allActionCombinations);
		final Set<List<String>> allActionCombinationsSorted = new HashSet<>();
		for (List<String> actionCombination : allActionCombinations) {
			final List<String> copy = new ArrayList<>(actionCombination);
			Collections.sort(copy);
			allActionCombinationsSorted.add(copy);
		}
		
		// 5. Compose
		compose(a1, a2, matchingOutputPairs, matchingInputPairs, allActionCombinationsSorted);
	}
}
