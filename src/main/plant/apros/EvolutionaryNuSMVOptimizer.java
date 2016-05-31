package main.plant.apros;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import structures.plant.NondetMooreAutomaton;

public class EvolutionaryNuSMVOptimizer {
	private final NondetMooreAutomaton automaton;
	private final int[][] initialSets;

	private int fitnessCalls;
	private int bestFitness;
	private int[] bestSolution;
	
	private final static int MAX_FITNESS_CALLS = 1000000;
	private final static int STAG_CALLS = 20000;

	private final static Random RND = new Random(215);
	
	public EvolutionaryNuSMVOptimizer(NondetMooreAutomaton automaton, List<String> events, List<String> actions) {
		this.automaton = automaton;
		final List<List<Integer>> initialSets = new ArrayList<>();
		automaton.toNuSMVString(events, actions, initialSets);
		final List<List<Integer>> initialSetsFiltered = new ArrayList<>();
		for (List<Integer> s : initialSets) {
			if (s.size() > 2) {
				initialSetsFiltered.add(s);
			}
		}
		this.initialSets = new int[initialSetsFiltered.size()][];
		for (int i = 0; i < this.initialSets.length; i++) {
			final List<Integer> l = initialSetsFiltered.get(i);
			this.initialSets[i] = new int[l.size()];
			for (int j = 0; j < l.size(); j++) {
				this.initialSets[i][j] = l.get(j);
			}
		}
	}
	
	private int fitness(int[] permutation) {
		fitnessCalls++;
		int sum = 0;
		for (int[] s : initialSets) {
			final int[] newSet = new int[s.length];
			for (int i = 0; i < s.length; i++) {
				newSet[i] = permutation[s[i]];
			}
			Arrays.sort(newSet);
			
			int min = -1;
			int max = -1;
			for (int value : newSet) {
				if (min == -1) {
					min = max = value;
				} else if (value == max + 1) {
					max = value;
				} else {
					sum += min == max ? 1 : 2;
					min = max = value;
				}
			}
			sum += min == max ? 1 : 2;
		}
		return sum;
	}
	
	private static void swap(int[] array, int i, int j) {
		final int t = array[i];
		array[i] = array[j];
		array[j] = t;
	}
	
	public NondetMooreAutomaton run() {
		fitnessCalls = 0;
		bestSolution = new int[automaton.stateCount()];
		for (int i = 0; i < bestSolution.length; i++) {
			bestSolution[i] = i; // initial solution
		}
		bestFitness = fitness(bestSolution);
		System.out.println(">>> " + bestFitness);
		
		final long time = System.currentTimeMillis();
		int lastSuccessfulCallsNum = 0;

		while (fitnessCalls < MAX_FITNESS_CALLS) {
			int pos1 = RND.nextInt(bestSolution.length);
			int pos2;
			do {
				pos2 = RND.nextInt(bestSolution.length);
			} while (pos2 == pos1);
			swap(bestSolution, pos1, pos2);
			int candidateFitness = fitness(bestSolution);
			if (candidateFitness <= bestFitness) {
				if (candidateFitness < bestFitness) {
					lastSuccessfulCallsNum = fitnessCalls;
					System.out.println(">>> " + candidateFitness);
				}
				bestFitness = candidateFitness;
			} else {
				swap(bestSolution, pos1, pos2); // revert
			}
			if (fitnessCalls % 1000 == 0) {
				System.out.println(fitnessCalls + " computed");
			}
			if (fitnessCalls - lastSuccessfulCallsNum > STAG_CALLS) {
				System.out.println(">>> Stagnation");
				break;
			}
		}

		System.out.println(">>> Best fitness: " + bestFitness + "\n");
		System.out.println(">>> Method execution time: " + (System.currentTimeMillis() - time) / 1000.0);
		System.out.println(Arrays.toString(bestSolution));
		return automaton.swapStates(bestSolution);
	}
}
