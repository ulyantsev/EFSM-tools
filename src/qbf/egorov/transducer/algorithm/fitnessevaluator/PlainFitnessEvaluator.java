package qbf.egorov.transducer.algorithm.fitnessevaluator;

import qbf.egorov.transducer.algorithm.FST;
import qbf.egorov.transducer.algorithm.FitnessCalculator;

public class PlainFitnessEvaluator extends FitnessEvaluator {

	public PlainFitnessEvaluator(FitnessCalculator fitnessCalculator) {
		super(fitnessCalculator);
	}

	@Override
	public double getFitness(FST fst) {
		if (!fst.isFitnessCalculated()) {
			numberOfFitnessEvaluations++;
			fst.setFitness(fitnessCalculator.calcFitness(fst));
			fst.setNeedToComputeFitness(false);
			fst.setFitnessCalculated(true);
		}
		return fst.fitness();	
	}

}
