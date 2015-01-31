package algorithms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import qbf.ltl.LtlNode;
import qbf.reduction.Assignment;
import qbf.reduction.BinaryOperation;
import qbf.reduction.BinaryOperations;
import qbf.reduction.BooleanFormula;
import qbf.reduction.FormulaList;
import qbf.reduction.SolverResult;
import qbf.reduction.Verifier;
import qbf.reduction.SolverResult.SolverResults;
import structures.Automaton;
import structures.ScenariosTree;

public class IterativeAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static Pair<Optional<Automaton>, List<Assignment>> automatonFromFormula(BooleanFormula bf, Logger logger, String solverParams,
			int timeoutSeconds, ScenariosTree tree, int colorSize) throws IOException {
		deleteTrash();
		try (PrintWriter pw = new PrintWriter("_tmp.pretty")) {
			pw.print(bf.toString());
		}
		final String strBf = bf.toLimbooleString();
		final Pair<List<Assignment>, Long> solution = BooleanFormula.solveAsSat(strBf, logger, solverParams, timeoutSeconds);
		final List<Assignment> list = solution.getLeft();
		final long time = solution.getRight();
		
		final SolverResult ass = list.isEmpty()
			? new SolverResult(time >= timeoutSeconds * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT, (int) time)
			: new SolverResult(list, (int) time);
		logger.info(ass.toString().split("\n")[0]);

		return Pair.of(ass.type() != SolverResults.SAT
				? Optional.empty()
				: constructAutomatonFromAssignment(logger, ass, tree, colorSize), list
		);
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int colorSize, String solverParams, boolean complete,
			int timeoutSeconds, String resultFilePath, String ltlFilePath, List<LtlNode> formulae, boolean bfsConstraints) throws IOException {
		final BooleanFormula initialBf = new SatFormulaBuilder(tree, colorSize, complete, bfsConstraints).getFormula();
		final FormulaList additionalConstraints = new FormulaList(BinaryOperations.AND);
		
		Optional<Automaton> automaton = Optional.empty();
		final long time = System.currentTimeMillis();
		for (int iterations = 0; (System.currentTimeMillis() - time) < timeoutSeconds * 1000; iterations++) {
			iterations++;
			BooleanFormula actualFormula = initialBf.and(additionalConstraints.assemble());
			final int secondsLeft = timeoutSeconds - (int) (System.currentTimeMillis() - time) / 1000 + 1;
			final Pair<Optional<Automaton>, List<Assignment>> p = automatonFromFormula(actualFormula, logger, solverParams,
					secondsLeft, tree, colorSize);
			automaton = p.getLeft();
			if (automaton.isPresent()) {
				System.out.println(automaton.get());
				if (new Verifier(colorSize, logger, ltlFilePath).verify(automaton.get())) {
					logger.info("ITERATIONS: " + iterations);
					return automaton;
				}
				final List<Assignment> assList = p.getRight().stream()
						.filter(ass -> !ass.var.name.startsWith("x"))
						.collect(Collectors.toList());
				final List<BooleanFormula> constraints = assList.stream()
						.map(ass -> ass.value ? ass.var : ass.var.not())
						.collect(Collectors.toList());
				additionalConstraints.add(BinaryOperation.and(constraints).not());
			} else {
				// no solution
				logger.info("ITERATIONS: " + iterations);
				return automaton;
			}
		}
		logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN.");
		return Optional.empty();
	}
}
