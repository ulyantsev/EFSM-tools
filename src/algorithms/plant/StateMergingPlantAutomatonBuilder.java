package algorithms.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import structures.plant.NondetMooreAutomaton;
import structures.plant.PositivePlantScenarioForest;
import algorithms.automaton_builders.ScenarioAndLtlAutomatonBuilder;
import egorov.ltl.grammar.LtlNode;
import egorov.verifier.NondetMooreVerifierPair;

public class StateMergingPlantAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	public static Optional<NondetMooreAutomaton> build(Logger logger, PositivePlantScenarioForest positiveForest,
			String ltlFilePath, List<LtlNode> formulae, List<String> events,
			List<String> actions, NondetMooreVerifierPair verifier, long finishTime) throws IOException {
		
		
		// TODO

		
		return null;
	}
}