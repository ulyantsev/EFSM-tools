package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.reduction.Assignment;
import qbf.reduction.BooleanFormula;
import qbf.reduction.BooleanFormula.DimacsConversionInfo;
import qbf.reduction.BooleanFormula.SolveAsSatResult;
import qbf.reduction.QbfSolver;
import qbf.reduction.QuantifiedBooleanFormula;
import qbf.reduction.QuantifiedBooleanFormula.FormulaSizeException;
import qbf.reduction.SatSolver;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.ScenariosTree;

public class HybridAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static final int MAX_FORMULA_SIZE = 500 * 1000 * 1000;
	
	private static class ExpandableStringFormula {
		public final String initialFormula;
		private final Logger logger;
		private DimacsConversionInfo info;
		private final SatSolver satSolver;
		private final String solverParams;

		public ExpandableStringFormula(String initialFormula, Logger logger, SatSolver satSolver,
				String solverParams) {
			this.initialFormula = initialFormula;
			this.logger = logger;
			this.satSolver = satSolver;
			this.solverParams = solverParams;
		}
		
		/*
		 * Should be called after 'solve' was called at least once.
		 */
		public void includeProhibitionConstraints(List<Assignment> constraints)
				throws IOException {
			assert info != null;
			BooleanFormula.appendProhibitionConstraintsToDimacs(logger,
					Collections.singletonList(constraints), info);
		}
		
		/*
		 * Should be called once.
		 */
		public SolveAsSatResult solve(int timeLeftForSolver) throws IOException {
			if (info == null) {
				final SolveAsSatResult solution = BooleanFormula.solveAsSat(initialFormula,
						logger, solverParams, timeLeftForSolver, satSolver);
				info = solution.info;
				return solution;
			}
			return BooleanFormula.solveDimacs(logger, timeLeftForSolver, satSolver,
					solverParams, info);
		}
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree,
			List<LtlNode> formulae, int size, String ltlFilePath,
			QbfSolver qbfSolver, String solverParams, boolean extractSubterms,
			List<String> events, List<String> actions, SatSolver satSolver,
			Verifier verifier, long finishTime, boolean complete,
			int secToGenerateFormula, int secToSolve) throws IOException {		
		int k = -1;
		boolean maxKFound = false;
		int iteration = 1;
		ExpandableStringFormula formula = null;
		ExpandableStringFormula formulaBackup = null;
		final Set<String> forbiddenYs = QbfAutomatonBuilder.getForbiddenYs(logger, size, events.size());
		deleteTrash();
		SolveAsSatResult solution = null;
		Pair<Automaton, List<Assignment>> autoSolution = null;
		while (System.currentTimeMillis() < finishTime) {
			if (maxKFound) {
				// deal with the previous FSM
				if (complete) {
					logger.info("TRYING TO COMPLETE THE PREVIOUSLY FOUND FSM");
					final Automaton b = constructAutomatonFromAssignment(logger,
							solution.list(), tree, size, false).getLeft();
					try {
						new AutomatonCompleter(verifier, b, events, actions, finishTime).ensureCompleteness();
					} catch (AutomatonFound e) {
						logger.info("SAT");
						return Optional.of(b);
					} catch (TimeLimitExceeded e) {
						logger.info("TIME LIMIT EXCEEDED");
						return Optional.empty();
					}
				}
				formula.includeProhibitionConstraints(autoSolution.getRight().stream()
						.map(v -> v.negate()).collect(Collectors.toList()));
			} else {
				// try next k
				k++;
				final QuantifiedBooleanFormula qbf = new QbfFormulaBuilder(logger, tree,
						formulae, size, k, extractSubterms, complete,
						events, actions).getFormula(true);
				final long time = System.currentTimeMillis();
				try {
					formulaBackup = formula = new ExpandableStringFormula(qbf.flatten(tree, size, k,
							logger, events, actions, forbiddenYs, Math.min(finishTime,
							System.currentTimeMillis() + secToGenerateFormula * 1000), MAX_FORMULA_SIZE),
							logger, satSolver, solverParams);
				} catch (FormulaSizeException | TimeLimitExceeded e) {
					logger.info("FORMULA FOR k = " + k +
							" IS TOO LARGE OR REQUIRES TOO MUCH TIME TO CONSTRUCT, STARTING ITERATIONS");
					logger.info("TRIED CREATING FORMULA FOR " + (System.currentTimeMillis() - time) + "ms");
					k--;
					maxKFound = true;
					continue;
				}
			}
			
			logger.info("TRYING k = " + k + (maxKFound ? (", iteration " + ++iteration) : ""));
			
			final int timeLeftForSolver = !maxKFound && k > 0
				? Math.min(timeLeftForSolver(finishTime), secToSolve)
				: timeLeftForSolver(finishTime);
			solution = formula.solve(timeLeftForSolver);
			final boolean unknown = solution.time >= timeLeftForSolver * 1000;
			
			if (!maxKFound && solution.list().isEmpty() && unknown && k > 0) {
				// too much time
				logger.info("FORMULA FOR k = " + k + " IS TOO HARD FOR THE SOLVER, STARTING ITERATIONS");
				k--;
				maxKFound = true;
				formula = formulaBackup;
				continue;
			} else if (solution.list().isEmpty()) {
				logger.info(unknown ? "UNKNOWN" : "UNSAT");
				return Optional.empty();
			} else {
				autoSolution = constructAutomatonFromAssignment(logger, solution.list(),tree, size, complete);
				if (verifier.verify(autoSolution.getLeft())) {
					logger.info("SAT");
					return Optional.of(autoSolution.getLeft());
				}
			}	
		}
		logger.info("TIME LIMIT EXCEEDED");
		return Optional.empty();
	}
	
	private static int timeLeftForSolver(long finishTime) {
		return (int) (finishTime - System.currentTimeMillis()) / 1000 + 1;
	}
}
