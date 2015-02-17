package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.reduction.Assignment;
import qbf.reduction.BinaryOperation;
import qbf.reduction.BinaryOperations;
import qbf.reduction.BooleanFormula;
import qbf.reduction.FormulaList;
import qbf.reduction.QuantifiedBooleanFormula;
import qbf.reduction.QuantifiedBooleanFormula.FormulaSizeException;
import qbf.reduction.QbfSolver;
import qbf.reduction.SatSolver;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.ScenariosTree;
import algorithms.FormulaBuilder.EventExpressionPair;

public class HybridAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static final int MAX_FORMULA_SIZE = 100 * 1000 * 1000;
	private static final int MILLIS_FOR_FORMULA = 5000;
	private static final int SEC_FOR_SOLVER = 5; // >= 2
	
	private static void addProhibitionConstraints(FormulaList constraints, List<Assignment> list) {
		constraints.add(BinaryOperation.and(list.stream()
				.filter(va -> va.var.name.startsWith("y"))
				.map(v -> v.value ? v.var : v.var.not())
				.collect(Collectors.toList())
		).not());
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree,
			List<LtlNode> formulae, int colorSize, String ltlFilePath,
			int timeoutSeconds, QbfSolver qbfSolver, String solverParams, boolean extractSubterms,
			boolean complete, boolean bfsConstraints,
			List<EventExpressionPair> efPairs, List<String> actions, SatSolver satSolver) throws IOException {
		
		final Verifier verifier = new Verifier(colorSize, logger, ltlFilePath,
				EventExpressionPair.getEvents(efPairs), actions);
		final long finishTime = System.currentTimeMillis() + timeoutSeconds * 1000;
		
		int k = -1;
		boolean maxKFound = false;
		int iteration = 1;
		String curFormula = null;
		String formulaBackup = null;
		final FormulaList additionalConstraints = new FormulaList(BinaryOperations.AND);
		final Set<String> forbiddenYs = QbfAutomatonBuilder.getForbiddenYs(colorSize, efPairs.size());
		logger.info("FORBIDDEN YS: " + forbiddenYs);
		while (true) {
			if (System.currentTimeMillis() > finishTime) {
				logger.info("TIME LIMIT EXCEEDED");
				return Optional.empty();
			}
			deleteTrash();
			String formula = null;
			if (maxKFound) {
				assert curFormula != null;
				formula = additionalConstraints.isEmpty()
						? curFormula
						: (curFormula + "&" + additionalConstraints.assemble().toLimbooleString());
			} else {
				// try next k
				k++;
				QuantifiedBooleanFormula qbf = new QbfFormulaBuilder(logger, tree,
						formulae, colorSize, k, extractSubterms, complete, bfsConstraints,
						efPairs, actions).getFormula(true);
				long time = System.currentTimeMillis();
				try {
					formula = qbf.flatten(tree, colorSize, k, logger, efPairs, actions,
							bfsConstraints, forbiddenYs, Math.min(finishTime, System.currentTimeMillis() + MILLIS_FOR_FORMULA),
							MAX_FORMULA_SIZE);
				} catch (FormulaSizeException | TimeLimitExceeded e) {
					logger.info("FORMULA FOR k = " + k + " IS TOO LARGE OR REQUIRES TOO MUCH TIME TO CONSTRUCT, STARTING ITERATIONS");
					logger.info("TRIED CREATING FORMULA FOR " + (System.currentTimeMillis() - time) + "ms");
					k--;
					maxKFound = true;
					continue;
				}
				formulaBackup = curFormula;
				curFormula = formula;
			}
			logger.info("TRYING k = " + k + (maxKFound ? (", iteration " + ++iteration) : ""));
			
			int timeLeftForSolver = (int) (finishTime - System.currentTimeMillis()) / 1000 + 1;
			if (!maxKFound && k > 0) {
				timeLeftForSolver = Math.min(timeLeftForSolver, SEC_FOR_SOLVER);
			}
			final Pair<List<Assignment>, Long> solution = BooleanFormula.solveAsSat(formula,
					logger, solverParams, timeLeftForSolver, satSolver);
			final List<Assignment> list = solution.getLeft();
			final long time = solution.getRight();
			final boolean unknown = time >= timeLeftForSolver * 1000;
			if (!maxKFound && list.isEmpty() && unknown && k > 0) {
				// too much time
				logger.info("FORMULA FOR k = " + k + " IS TOO HARD FOR THE SOLVER, STARTING ITERATIONS");
				k--;
				maxKFound = true;
				curFormula = formulaBackup;
				continue;
			} else if (list.isEmpty()) {
				logger.info(unknown ? "UNKNOWN" : "UNSAT");
				return Optional.empty();
			} else {
				final Automaton a = constructAutomatonFromAssignment(logger,
						list, tree, colorSize, true).getLeft();
				if (verifier.verify(a)) {
					logger.info("SAT");
					return Optional.of(a);
				}
				
				// more search
				final Automaton b = constructAutomatonFromAssignment(logger,
						list, tree, colorSize, false).getLeft();
				try {
					new AutomatonCompleter(verifier, b, efPairs, actions, finishTime).ensureCompleteness();
				} catch (AutomatonFound e) {
					logger.info("SAT");
					logger.info("A MORE THOROUGH SEARCH SUCCEEDED");
					return Optional.of(b);
				} catch (TimeLimitExceeded e) {
					logger.info("TIME LIMIT EXCEEDED");
					return Optional.empty();
				}
				// no complete extensions, continue search

				// add only ys
				addProhibitionConstraints(additionalConstraints, list);
			}
		}
	}
}
