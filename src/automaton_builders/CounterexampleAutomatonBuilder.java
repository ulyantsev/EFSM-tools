package automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import algorithms.AutomatonCompleter;
import algorithms.AutomatonCompleter.CompletenessType;
import bnf_formulae.BinaryOperations;
import bnf_formulae.BooleanFormula;
import bnf_formulae.BooleanFormula.SolveAsSatResult;
import bnf_formulae.FormulaList;
import bool.MyBooleanExpression;
import exception.AutomatonFoundException;
import exception.TimeLimitExceededException;
import formula_builders.CounterexampleFormulaBuilder;
import sat_solving.Assignment;
import sat_solving.ExpandableStringFormula;
import sat_solving.SatSolver;
import sat_solving.SolverResult;
import sat_solving.SolverResult.SolverResults;
import scenario.StringActions;
import scenario.StringScenario;
import structures.mealy.MealyAutomaton;
import structures.mealy.MealyTransition;
import structures.mealy.NegativeScenarioTree;
import structures.mealy.ScenarioTree;
import verification.verifier.Counterexample;
import verification.verifier.Verifier;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

public class CounterexampleAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
    protected static Optional<MealyAutomaton> reportResult(Logger logger, int iterations, Optional<MealyAutomaton> a) {
        logger.info("ITERATIONS: " + (iterations + 1));
        return a;
    }
    
    protected static void addCounterexample(Logger logger, MealyAutomaton a,
            Counterexample counterexample, NegativeScenarioTree negativeTree) {
        int state = a.startState().number();
        final List<MyBooleanExpression> expressions = new ArrayList<>();
        final List<StringActions> actions = new ArrayList<>();
        for (String event : counterexample.events()) {
            final MealyTransition t = a.state(state).transition(event, MyBooleanExpression.getTautology());
            expressions.add(t.expr());
            actions.add(t.actions());
            final int newState = t.dst().number();          
            state = newState;
        }
        logger.info("ADDING COUNTEREXAMPLE: " + counterexample);
        try {
            negativeTree.addScenario(new StringScenario(counterexample.events(), expressions, actions),
                    counterexample.loopLength);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected static void addProhibited(List<Assignment> list,
                                        List<BooleanFormula> prohibited) {
        final FormulaList options = new FormulaList(BinaryOperations.OR);
        for (Assignment ass : list) {
            if (ass.var.name.startsWith("y_") && ass.value || ass.var.name.startsWith("z_")) {
                options.add(ass.value ? ass.var.not() : ass.var);
            }
        }
        prohibited.add(options.assemble());
    }
        
    public static Optional<MealyAutomaton> build(Logger logger, ScenarioTree tree, int size,
                                                 List<String> events, List<String> actions,
                                                 SatSolver satSolver, Verifier verifier, long finishTime,
                                                 CompletenessType completenessType, NegativeScenarioTree negativeTree,
                                                 boolean useCompletionHeuristics) throws IOException {
        deleteTrash();
        
        final List<BooleanFormula> prohibited = new ArrayList<>();
        final CompletenessType effectiveCompletenessType = useCompletionHeuristics
                ? CompletenessType.NO_DEAD_ENDS : completenessType;
        
        ExpandableStringFormula expandableFormula = null;
        final Set<BooleanFormula> previousConstraints = new LinkedHashSet<>();
        final Set<BooleanFormula> negationConstraints = new LinkedHashSet<>();
        
        for (int iteration = 0; System.currentTimeMillis() < finishTime; iteration++) {
            final CounterexampleFormulaBuilder builder = new CounterexampleFormulaBuilder(tree, size, events, actions,
                    effectiveCompletenessType, negativeTree, prohibited);
            final FormulaList negationList = new FormulaList(BinaryOperations.AND);
            if (expandableFormula == null) {
                final BooleanFormula basicFormula = builder.getBasicFormula();
                negationConstraints.addAll(builder.getNegationConstraints());
                negationConstraints.stream().forEach(negationList::add);
                final String formula = basicFormula.and(negationList.assemble())
                        .simplify().toLimbooleString();
                expandableFormula = new ExpandableStringFormula(formula, logger, satSolver);
            } else {
                negationConstraints.addAll(builder.getNegationConstraints());
                final Set<BooleanFormula> diffConstraints = new LinkedHashSet<>(negationConstraints);
                diffConstraints.removeAll(previousConstraints);
                diffConstraints.stream().forEach(negationList::add);
                final String negationFormula = negationList.assemble().simplify().toLimbooleString();
                final List<String> negativeDimacsConstraints = BooleanFormula.extendDimacs(negationFormula,
                        logger, "_tmp.incremental.dimacs", expandableFormula.info());
                expandableFormula.addConstraints(negativeDimacsConstraints);
            }
            previousConstraints.addAll(negationConstraints);

            // SAT-solve
            final int secondsLeft = timeLeftForSolver(finishTime);
            final SolveAsSatResult solution = expandableFormula.solve(secondsLeft);
            final SolverResult ass = solution.toSolverResult(secondsLeft);
            logger.info(ass.type().toString());

            final Optional<MealyAutomaton> automaton = ass.type() == SolverResults.SAT
                ? Optional.of(constructAutomatonFromAssignment
                        (logger, ass.list(), tree, size, true, effectiveCompletenessType).getLeft())
                : Optional.empty();
            
            if (automaton.isPresent()) {
                final List<Counterexample> counterexamples = verifier.verifyWithCounterexamples(automaton.get());
                final boolean verified = counterexamples.stream().allMatch(Counterexample::isEmpty);
                if (verified) {
                    if (completenessType == CompletenessType.NORMAL && useCompletionHeuristics) {
                        logger.info("STARTING HEURISTIC COMPLETION");
                        try {
                            new AutomatonCompleter(verifier, automaton.get(), events, actions,
                                    finishTime, CompletenessType.NORMAL).ensureCompleteness();
                        } catch (AutomatonFoundException e) {
                            return reportResult(logger, iteration, Optional.of(e.automaton));
                        } catch (TimeLimitExceededException e) {
                            logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN");
                            return reportResult(logger, iteration, Optional.empty());
                        }
                        addProhibited(solution.list(), prohibited);
                        logger.info("ADDED PROHIBITED FSM");
                    } else {
                        return reportResult(logger, iteration, automaton);
                    }
                } else {
                    final Set<String> unique = new HashSet<>();
                    for (Counterexample counterexample : counterexamples) {
                        if (!counterexample.isEmpty()) {
                            if (!unique.contains(counterexample.toString())) {
                                unique.add(counterexample.toString());
                                addCounterexample(logger, automaton.get(), counterexample, negativeTree);
                            } else {
                                logger.info("DUPLICATE COUNTEREXAMPLES ON THE SAME ITERATION");
                            }
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