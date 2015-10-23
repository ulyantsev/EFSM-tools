package algorithms.automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sat_solving.Assignment;
import sat_solving.ExpandableStringFormula;
import sat_solving.SatSolver;
import sat_solving.SolverResult;
import sat_solving.SolverResult.SolverResults;
import scenario.StringActions;
import structures.Automaton;
import structures.plant.NegativePlantScenarioForest;
import structures.plant.NondetMooreAutomaton;
import structures.plant.MooreNode;
import structures.plant.PositivePlantScenarioForest;
import algorithms.formula_builders.PlantFormulaBuilder;
import bnf_formulae.BooleanFormula.SolveAsSatResult;
import egorov.Verifier;
import egorov.ltl.grammar.LtlNode;

public class PlantAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	protected static Optional<Automaton> reportResult(Logger logger, int iterations, Optional<Automaton> a) {
		logger.info("ITERATIONS: " + (iterations + 1));
		return a;
	}
		
	private static NondetMooreAutomaton constructAutomatonFromAssignment(Logger logger, List<Assignment> ass,
			PositivePlantScenarioForest tree, int colorSize) {
		final List<List<String>> actions = new ArrayList<>();
		final List<Boolean> isStart = Arrays.asList(new Boolean[colorSize]);
		for (int i = 0; i < isStart.size(); i++) {
			isStart.set(i, false);
		}
		for (int i = 0; i < isStart.size(); i++) {
			actions.add(new ArrayList<>());
		}
		
		for (Assignment a : ass) {
			if (a.value) {
				final String tokens[] = a.var.name.split("_");
				if (tokens[0].equals("x")) {
					for (MooreNode root : tree.getRoots()) {
						if (tokens[1].equals(root.getNumber() + "")) {
							final int state = Integer.parseInt(tokens[2]);
							isStart.set(state, true);
						}
					}
				} else if (tokens[0].equals("z")) {
					final int state = Integer.parseInt(tokens[1]);
					final String action = tokens[2];
					actions.get(state).add(action);
				}
			}
		}
		
		final NondetMooreAutomaton automaton = new NondetMooreAutomaton(colorSize,
				actions.stream().map(l -> new StringActions(String.join(",", l))).collect(Collectors.toList()),
				isStart);
		
		for (Assignment a : ass) {
			if (a.value) {
				final String tokens[] = a.var.name.split("_");
				if (tokens[0].equals("y")) {
					final int from = Integer.parseInt(tokens[1]);
					final int to = Integer.parseInt(tokens[2]);
					final String event = tokens[3];
					automaton.getState(from).addTransition(event, automaton.getState(to));
				}
			}
		}
		
		return automaton;
	}
	
	public static Optional<NondetMooreAutomaton> build(Logger logger, PositivePlantScenarioForest positiveForest,
			NegativePlantScenarioForest negativeForest, int size, String solverParams,
			String resultFilePath, String ltlFilePath, List<LtlNode> formulae,
			List<String> events, List<String> actions, SatSolver satSolver,
			Verifier verifier, long finishTime) throws IOException {
		deleteTrash();
		
		final PlantFormulaBuilder builder = new PlantFormulaBuilder(size, positiveForest, negativeForest, events, actions);
		final String formula = builder.scenarioConstraints().assemble().simplify().toLimbooleString();
		final ExpandableStringFormula expandableFormula = new ExpandableStringFormula(formula, logger, satSolver, solverParams);
		
		// SAT-solve
		final int secondsLeft = timeLeftForSolver(finishTime);
		final SolveAsSatResult solution = expandableFormula.solve(secondsLeft);
		final List<Assignment> list = solution.list();
		final long time = solution.time;
		
		final SolverResult ass = list.isEmpty()
			? new SolverResult(time >= secondsLeft * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT)
			: new SolverResult(list);
		logger.info(ass.type().toString());

		final Optional<NondetMooreAutomaton> automaton = ass.type() == SolverResults.SAT
			? Optional.of(constructAutomatonFromAssignment(logger, ass.list(), positiveForest, size))
			: Optional.empty();
		
		return automaton;
	}
	
	protected static int timeLeftForSolver(long finishTime) {
		return (int) (finishTime - System.currentTimeMillis()) / 1000 + 1;
	}
}