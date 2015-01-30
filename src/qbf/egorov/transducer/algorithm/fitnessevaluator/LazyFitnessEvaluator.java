package qbf.egorov.transducer.algorithm.fitnessevaluator;

import qbf.egorov.transducer.algorithm.FST;
import qbf.egorov.transducer.algorithm.FitnessCalculator;

public class LazyFitnessEvaluator extends FitnessEvaluator {

	public LazyFitnessEvaluator(FitnessCalculator fitnessCalculator) {
		super(fitnessCalculator);
	}

	@Override
	public double getFitness(FST fst) {
		if (!fst.isFitnessCalculated()) {
			if (fst.needToComputeFitness()) {
				numberOfFitnessEvaluations++;
				fst.setFitness(fitnessCalculator.calcFitness(fst));
				fst.setNeedToComputeFitness(false);
			} else {
				cntLazySaved++;
			}
			fst.setFitnessCalculated(true);
		}
		return fst.fitness();	
	}

}
