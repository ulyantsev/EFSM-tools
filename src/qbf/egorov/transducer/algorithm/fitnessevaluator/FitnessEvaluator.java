package qbf.egorov.transducer.algorithm.fitnessevaluator;

import qbf.egorov.transducer.algorithm.FST;
import qbf.egorov.transducer.algorithm.FitnessCalculator;

public abstract class FitnessEvaluator {
	protected int numberOfFitnessEvaluations = 0;
	protected FitnessCalculator fitnessCalculator;
	protected int cntLazySaved = 0;
	protected int cntBfsSaved = 0;
	
	public FitnessEvaluator(FitnessCalculator fitnessCalculator) {
		this.fitnessCalculator = fitnessCalculator;
	}
	
	public abstract double getFitness(FST fst);
	
	public int getNumberOfFitnessEvaluations() {
		return numberOfFitnessEvaluations;
	}

	public int getCntLazySaved() {
		return cntLazySaved;
	}

	public int getCntBfsSaved() {
		return cntBfsSaved;
	}
}
