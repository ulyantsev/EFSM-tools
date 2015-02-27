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

public class HybridAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static final int MAX_FORMULA_SIZE = 500 * 1000 * 1000;
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree,
			List<LtlNode> formulae, int colorSize, String ltlFilePath,
			QbfSolver qbfSolver, String solverParams, boolean extractSubterms,
			List<String> events, List<String> actions, SatSolver satSolver,
			Verifier verifier, long finishTime, boolean complete,
			int secToGenerateFormula, int secToSolve) throws IOException {		
		int k = -1;
		boolean maxKFound = false;
		int iteration = 1;
		String curFormula = null;
		String formulaBackup = null;
		final FormulaList additionalConstraints = new FormulaList(BinaryOperations.AND);
		final Set<String> forbiddenYs = QbfAutomatonBuilder.getForbiddenYs(colorSize, events.size());
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
						formulae, colorSize, k, extractSubterms, complete,
						events, actions).getFormula(true);
				long time = System.currentTimeMillis();
				try {
					formula = qbf.flatten(tree, colorSize, k, logger, events, actions,
							forbiddenYs, Math.min(finishTime, System.currentTimeMillis() + secToGenerateFormula * 1000),
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
				timeLeftForSolver = Math.min(timeLeftForSolver, secToSolve);
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
			}
			
			if (list.isEmpty()) {
				logger.info(unknown ? "UNKNOWN" : "UNSAT");
				return Optional.empty();
			}
			
			final Pair<Automaton, List<Assignment>> p = constructAutomatonFromAssignment(logger,
					list, tree, colorSize, complete);
			final Automaton a = p.getLeft();
			if (verifier.verify(a)) {
				logger.info("SAT");
				return Optional.of(a);
			}
			
			if (complete) {
				// more search
				final Automaton b = constructAutomatonFromAssignment(logger,
						list, tree, colorSize, false).getLeft();
				try {
					new AutomatonCompleter(verifier, b, events, actions, finishTime).ensureCompleteness();
				} catch (AutomatonFound e) {
					logger.info("SAT");
					logger.info("A MORE THOROUGH SEARCH SUCCEEDED");
					return Optional.of(b);
				} catch (TimeLimitExceeded e) {
					logger.info("TIME LIMIT EXCEEDED");
					return Optional.empty();
				}
			}
			// no complete extensions, continue search

			// add only ys
			additionalConstraints.add(BinaryOperation.and(p.getRight().stream()
					.map(v -> v.value ? v.var : v.var.not())
					.collect(Collectors.toList())
			).not());
		}
	}
}
