package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.egorov.verifier.VerifierFactory.Counterexample;
import qbf.reduction.Assignment;
import qbf.reduction.BooleanFormula;
import qbf.reduction.BooleanFormula.SolveAsSatResult;
import qbf.reduction.BooleanVariable;
import qbf.reduction.QbfSolver;
import qbf.reduction.QuantifiedBooleanFormula;
import qbf.reduction.QuantifiedBooleanFormula.FormulaSizeException;
import qbf.reduction.SatSolver;
import qbf.reduction.SolverResult;
import qbf.reduction.SolverResult.SolverResults;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.NegativeScenariosTree;
import structures.ScenariosTree;
import algorithms.AutomatonCompleter.CompletenessType;

public class HybridAutomatonBuilder extends CounterexampleAutomatonBuilder {
	private static final int MAX_FORMULA_SIZE = 500 * 1000 * 1000;
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree,
			List<LtlNode> formulae, int size, String ltlFilePath,
			QbfSolver qbfSolver, String solverParams,
			List<String> events, List<String> actions, SatSolver satSolver,
			Verifier verifier, long finishTime, CompletenessType completenessType,
			int secToGenerateFormula, NegativeScenariosTree negativeTree) throws IOException {		
		boolean maxKFound = false;
		
		deleteTrash();
		final Set<String> forbiddenYs = QbfAutomatonBuilder.getForbiddenYs(logger, size, events.size());
		
		final List<BooleanFormula> prohibited = new ArrayList<>();
		final CompletenessType effectiveCompletenessType = USE_COMPLETENESS_HEURISTICS
				? CompletenessType.NO_DEAD_ENDS : completenessType;
		String forallPart = null;
		
		for (int iteration = 0, k = -1; System.currentTimeMillis() < finishTime; iteration++) {
			if (!maxKFound) {
				k++;
			}
			logger.info("ITERATION " + iteration + ", k = " + k + " AND IS "
					+ (maxKFound ? "FIXED" : "NOT FIXED"));
			BooleanVariable.eraseVariables();
			
			final QuantifiedBooleanFormula qbf = new HybridFormulaBuilder(tree, size, events, actions,
					effectiveCompletenessType, logger, formulae, k, negativeTree, prohibited)
					.getFormula();
			final String existentialPart = qbf.existentialPart().simplify().toLimbooleString();

			if (!maxKFound) {
				final long formulaTime = System.currentTimeMillis();
				try {
					forallPart = qbf.flatten(size, k,
							logger, events, actions, forbiddenYs, Math.min(finishTime,
							System.currentTimeMillis() + secToGenerateFormula * 1000),
							MAX_FORMULA_SIZE - existentialPart.length(), false);
				} catch (FormulaSizeException | TimeLimitExceeded e1) {
					logger.info("FORMULA FOR k = " + k +
							" IS TOO LARGE OR REQUIRES TOO MUCH TIME TO CONSTRUCT, STARTING ITERATIONS");
					logger.info("TRIED CREATING FORMULA FOR " +
							(System.currentTimeMillis() - formulaTime) + "ms");
					k--;
					maxKFound = true;
					continue;
				}
			}
			final String formula = "(" + existentialPart + ")&(" + forallPart + ")";
			
			// SAT-solve
			final int secondsLeft = (int) ((finishTime - System.currentTimeMillis()) / 1000 + 1);
			final SolveAsSatResult solution = BooleanFormula.solveAsSat(formula,
					logger, solverParams, secondsLeft, satSolver);
			final List<Assignment> list = solution.list();
			final long time = solution.time;
			
			final SolverResult ass = list.isEmpty()
				? new SolverResult(time >= secondsLeft * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT)
				: new SolverResult(list);
			logger.info(ass.type().toString());

			final Optional<Automaton> automaton = ass.type() == SolverResults.SAT
				? Optional.of(constructAutomatonFromAssignment
						(logger, ass.list(), tree, size, true, effectiveCompletenessType).getLeft())
				: Optional.empty();
			
			if (automaton.isPresent()) {
				final List<Counterexample> counterexamples = verifier.verifyWithCounterExamples(automaton.get());
				final boolean verified = counterexamples.stream().allMatch(Counterexample::isEmpty);
				if (verified) {
					if (completenessType == CompletenessType.NORMAL && USE_COMPLETENESS_HEURISTICS) {
						logger.info("STARTING HEURISTIC COMPLETION");
						try {
							new AutomatonCompleter(verifier, automaton.get(), events, actions,
									finishTime, CompletenessType.NORMAL).ensureCompleteness();
						} catch (AutomatonFound e) {
							return reportResult(logger, iteration, Optional.of(e.automaton));
						} catch (TimeLimitExceeded e) {
							logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN");
							return reportResult(logger, iteration, Optional.empty());
						}
						addProhibited(logger, list, prohibited);
						logger.info("ADDED PROHIBITED FSM");
					} else {
						return reportResult(logger, iteration, automaton);
					}
				} else {
					for (Counterexample counterexample : counterexamples) {
						if (!counterexample.isEmpty()) {
							addCounterexample(logger, automaton.get(), counterexample, negativeTree);
						} else {
							logger.info("NOT ADDING COUNTEREXAMPLE");
						}
					}
				}
			} else {
				// no solution due to UNSAT or UNKNOWN, stop search
				return reportResult(logger, iteration, Optional.empty());
			}
		}
		logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN");
		return Optional.empty();
	}
}