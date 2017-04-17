package automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import algorithms.AutomatonCompleter.CompletenessType;
import bnf_formulae.BooleanFormula;
import bnf_formulae.BooleanFormula.SolveAsSatResult;
import bnf_formulae.QuantifiedBooleanFormula;
import bnf_formulae.QuantifiedBooleanFormula.FormulaSizeException;
import exception.TimeLimitExceededException;
import formula_builders.QbfFormulaBuilder;
import sat_solving.Assignment;
import sat_solving.QbfSolver;
import sat_solving.SatSolver;
import sat_solving.SolverResult;
import sat_solving.SolverResult.SolverResults;
import structures.mealy.MealyAutomaton;
import structures.mealy.ScenarioTree;
import verification.ltl.grammar.LtlNode;
import verification.verifier.Verifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class QbfAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {   
    public final static String PRECOMPUTED_DIR_NAME = "qbf/bfs-prohibited-ys";
    private static final int MAX_FORMULA_SIZE = 1000 * 1000 * 1000;
    
    public static Set<String> getForbiddenYs(Logger logger, int states, int events) throws FileNotFoundException {
        final Set<String> ys = new TreeSet<>();
        try (Scanner sc = new Scanner(new File(PRECOMPUTED_DIR_NAME + "/" + states + "_" + events))) {
            while (sc.hasNext()) {
                ys.add(sc.next());
            }
        }
        logger.info("FORBIDDEN YS: " + ys);
        return ys;
    }
    
    public static Optional<MealyAutomaton> build(Logger logger, ScenarioTree tree,
                                                 List<LtlNode> formulae, int size, QbfSolver qbfSolver,
                                                 boolean useSat, List<String> events, List<String> actions,
                                                 SatSolver satSolver, Verifier verifier, long finishTime,
                                                 CompletenessType completenessType) throws IOException {
        if (useSat) {
            final Set<String> forbiddenYs = getForbiddenYs(logger, size, events.size());
            for (int k = 0; ; k++) {
                if (System.currentTimeMillis() > finishTime) {
                    logger.info("TIME LIMIT EXCEEDED");
                    return Optional.empty();
                }
                logger.info("TRYING k = " + k);
                deleteTrash();
                QuantifiedBooleanFormula qbf = new QbfFormulaBuilder(logger, tree,
                        formulae, size, k, completenessType, events, actions).getFormula(false);
                final int timeLeft = (int) (finishTime - System.currentTimeMillis()) / 1000 + 1;
                
                String formula;
                try {
                    formula = qbf.flatten(size, k, logger, events, actions,
                            forbiddenYs, finishTime, MAX_FORMULA_SIZE, true);
                } catch (FormulaSizeException | TimeLimitExceededException e) {
                    logger.info("FORMULA FOR k = " + k + " IS TOO LARGE OR REQUIRES TOO MUCH TIME TO CONSTRUCT");
                    logger.info(new SolverResult(SolverResults.UNKNOWN).toString());
                    return Optional.empty();
                }
                SolveAsSatResult solution = BooleanFormula.solveAsSat(formula, logger, timeLeft, satSolver);
                List<Assignment> list = solution.list();
                long time = solution.time;
                if (list.isEmpty()) {
                    logger.info(new SolverResult(time >= timeLeft * 1000
                            ? SolverResults.UNKNOWN : SolverResults.UNSAT).toString());
                    return Optional.empty();
                } else {
                    final MealyAutomaton a = constructAutomatonFromAssignment(logger,
                            list, tree, size, true, completenessType).getLeft();
                    if (verifier.verify(a)) {
                        logger.info(new SolverResult(list).toString().split("\n")[0]);
                        return Optional.of(a);
                    } else {
                        continue;
                    }
                }
            }
        } else {
            for (int k = 0; ; k++) {
                if (System.currentTimeMillis() > finishTime) {
                    logger.info("TIME LIMIT EXCEEDED");
                    return Optional.empty();
                }
                logger.info("TRYING k = " + k);
                deleteTrash();
                QuantifiedBooleanFormula qbf = new QbfFormulaBuilder(logger, tree,
                        formulae, size, k, completenessType, events, actions).getFormula(false);
                final int timeLeft = (int) (finishTime - System.currentTimeMillis()) / 1000 + 1;
                SolverResult ass = qbf.solve(logger, qbfSolver, timeLeft);
                logger.info(ass.toString().split("\n")[0]);

                if (ass.type() == SolverResults.SAT) {
                    final MealyAutomaton a = constructAutomatonFromAssignment(logger, ass.list(),
                            tree, size, true, completenessType).getLeft();
                    if (verifier.verify(a)) {
                        return Optional.of(a);
                    } else {
                        continue;
                    }
                } else {
                    return Optional.empty();
                }
            }
        }
    }
}
