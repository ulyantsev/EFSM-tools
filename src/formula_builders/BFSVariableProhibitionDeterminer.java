package formula_builders;

/**
 * (c) Igor Buzhinsky
 */

import bnf_formulae.*;
import sat_solving.Assignment;
import sat_solving.SatSolver;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BFSVariableProhibitionDeterminer {
    public static Set<String> getForbiddenYs(int statesNum, int eventNum, Logger logger) throws IOException {
        final int effectiveEventNum = Math.max(2, Math.min(eventNum, statesNum - 2));
        final BFSVariableProhibitionDeterminer d = new BFSVariableProhibitionDeterminer(statesNum, effectiveEventNum);
        return new TreeSet<>(d.check(logger).entrySet().stream().filter(e -> !e.getValue())
                .map(Map.Entry::getKey).collect(Collectors.toList()));
    }
    
    private final List<BooleanVariable> existVars = new ArrayList<>();
    private final FormulaList constraints = new FormulaList(BinaryOperations.AND);
    private final int eventNum;
    private final int colorSize;
    
    private BooleanVariable yVar(int from, int to, int event) {
        return BooleanVariable.byName("y", from, to, event).get();
    }
    
    private BooleanVariable pVar(int j, int i) {
        return BooleanVariable.byName("p", j, i).get();
    }
    
    private BooleanVariable tVar(int i, int j) {
        return BooleanVariable.byName("t", i, j).get();
    }
    
    private BooleanVariable mVar(int event, int i, int j) {
        return BooleanVariable.byName("m", event, i, j).get();
    }
    
    private void addVariables() {
        // y
        for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
            for (int e = 0; e < eventNum; e++) {
                for (int childColor = 0; childColor < colorSize; childColor++) {
                    existVars.add(new BooleanVariable("y", nodeColor, childColor, e));
                }
            }
        }   
        // p_ji, t_ij
        for (int i = 0; i < colorSize; i++) {
            for (int j = i + 1; j < colorSize; j++) {
                existVars.add(new BooleanVariable("p", j, i));
                existVars.add(new BooleanVariable("t", i, j));
            }
        }
        if (eventNum > 2) {
            // m_efij
            for (int e = 0; e < eventNum; e++) {
                for (int i = 0; i < colorSize; i++) {
                    for (int j = i + 1; j < colorSize; j++) {
                        existVars.add(new BooleanVariable("m", e, i, j));
                    }
                }
            }
        }
    }
    
    private BFSVariableProhibitionDeterminer(int colorSize, int eventNum) {
        this.eventNum = eventNum;
        this.colorSize = colorSize;
        addVariables();
        parentConstraints();
        pDefinitions();
        tDefinitions();
        childrenOrderConstraints();
        notMoreThanOneEdgeConstraints();
    }
    
    public Map<String, Boolean> check(Logger logger) throws IOException {
        final Map<String, Boolean> results = new LinkedHashMap<>();
        for (BooleanVariable v : existVars) {
            if (v.name.startsWith("y")) {
                constraints.add(v);
                final List<Assignment> list = BooleanFormula.solveAsSat(constraints.assemble().toLimbooleString(),
                        logger, 100000, SatSolver.CRYPTOMINISAT).list();
                results.put(v.name, !list.isEmpty());
                constraints.removeLast();
            }
        }
        return results;
    }
    
    private void notMoreThanOneEdgeConstraints() {
        for (int e = 0; e < eventNum; e++) {
            for (int parentColor = 0; parentColor < colorSize; parentColor++) {
                for (int color1 = 0; color1 < colorSize; color1++) {
                    BooleanVariable v1 = yVar(parentColor, color1, e);
                    for (int color2 = 0; color2 < color1; color2++) {
                        BooleanVariable v2 = yVar(parentColor, color2, e);
                        constraints.add(v1.not().or(v2.not()));
                    }
                }
            }
        }
    }
    
    private void parentConstraints() {
        for (int j = 1; j < colorSize; j++) {
            final FormulaList options = new FormulaList(BinaryOperations.OR);
            for (int i = 0; i < j; i++) {
                options.add(pVar(j, i));
            }
            constraints.add(options.assemble());
        }
        
        for (int k = 0; k < colorSize; k++) {
            for (int i = k + 1; i < colorSize; i++) {
                for (int j = i + 1; j < colorSize - 1; j++) {
                    constraints.add(pVar(j, i).implies(pVar(j + 1, k).not()));
                }
            }
        }
    }
    
    private void pDefinitions() {
        for (int i = 0; i < colorSize; i++) {
            for (int j = i + 1; j < colorSize; j++) {
                final FormulaList definition = new FormulaList(BinaryOperations.AND);
                definition.add(tVar(i, j));
                for (int k = i - 1; k >=0; k--) {
                    definition.add(tVar(k, j).not());
                }
                constraints.add(pVar(j, i).equivalent(definition.assemble()));
            }
        }
    }
    
    private void tDefinitions() {
        for (int i = 0; i < colorSize; i++) {
            for (int j = i + 1; j < colorSize; j++) {
                final FormulaList definition = new FormulaList(BinaryOperations.OR);
                for (int e = 0; e < eventNum; e++) {
                    definition.add(yVar(i, j, e));
                }
                constraints.add(tVar(i, j).equivalent(definition.assemble()));
            }
        }
    }
    
    private void childrenOrderConstraints() {
        if (eventNum > 2) {
            // m definitions
            for (int i = 0; i < colorSize; i++) {
                for (int j = i + 1; j < colorSize; j++) {
                    for (int e1 = 0; e1 < eventNum; e1++) {
                        final FormulaList definition = new FormulaList(BinaryOperations.AND);
                        definition.add(yVar(i, j, e1));
                        for (int e2 = e1 - 1; e2 >= 0; e2--) {
                            definition.add(yVar(i, j, e2).not());
                        }
                        constraints.add(mVar(e1, i, j).equivalent(definition.assemble()));
                    }
                }
            }
            // children constraints
            for (int i = 0; i < colorSize; i++) {
                for (int j = i + 1; j < colorSize - 1; j++) {
                    for (int k = 0; k < eventNum; k++) {
                        for (int n = k + 1; n < eventNum; n++) {
                            constraints.add(
                                    BinaryOperation.and(
                                            pVar(j, i), pVar(j + 1, i), mVar(n, i, j)
                                    ).implies(mVar(k, i, j + 1).not())
                            );
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < colorSize; i++) {
                for (int j = i + 1; j < colorSize - 1; j++) {
                    constraints.add(
                            pVar(j, i).and(pVar(j + 1, i)).implies(yVar(i, j, 0))
                    );
                }
            }
        }
    }
}
