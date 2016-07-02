package algorithms.formula_builders;

import bnf_formulae.BooleanVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by buzhinsky on 7/3/16.
 */
public abstract class FastFormulaBuilder {
    protected final int colorSize;
    protected final List<String> events;
    protected final Map<String, Integer> eventIndices = new TreeMap<>();
    protected final List<String> actions;
    protected final Map<String, Integer> actionIndices = new TreeMap<>();
    protected final List<BooleanVariable> vars = new ArrayList<>();

    protected final boolean deterministic;
    protected final boolean complete;
    protected final boolean bfsConstraints;

    protected FastFormulaBuilder(int colorSize, List<String> events, List<String> actions,
                                 boolean deterministic, boolean complete, boolean bfsConstraints) {
        this.colorSize = colorSize;
        this.events = events;
        for (int i = 0; i < events.size(); i++) {
            eventIndices.put(events.get(i), i);
        }
        this.actions = actions;
        for (int i = 0; i < actions.size(); i++) {
            actionIndices.put(actions.get(i), i);
        }
        this.deterministic = deterministic;
        this.complete = complete;
        this.bfsConstraints = bfsConstraints;
    }

    public static BooleanVariable xVar(int node, int color) {
        return BooleanVariable.byName("x", node, color).get();
    }

    public static BooleanVariable yVar(int from, int to, int event) {
        return BooleanVariable.byName("y", from, to, event).get();
    }

    public static BooleanVariable xxVar(int node, int color, boolean isGlobal) {
        return BooleanVariable.byName(isGlobal ? "xxg" : "xx", node, color).get();
    }

    protected void eventCompletenessConstraints(List<int[]> constraints) {
        for (int i1 = 0; i1 < colorSize; i1++) {
            if (complete) {
                for (int ei = 0; ei < events.size(); ei++) {
                    final int[] constraint = new int[colorSize];
                    for (int i2 = 0; i2 < colorSize; i2++) {
                        constraint[i2] = yVar(i1, i2, ei).number;
                    }
                    constraints.add(constraint);
                }
            } else {
                final int[] constraint = new int[colorSize * events.size()];
                int pos = 0;
                for (int ei = 0; ei < events.size(); ei++) {
                    for (int i2 = 0; i2 < colorSize; i2++) {
                        constraint[pos++] = yVar(i1, i2, ei).number;
                    }
                }
                constraints.add(constraint);
            }
        }
    }

    protected void notMoreThanOneEdgeConstraints(List<int[]> constraints) {
        for (int i1 = 0; i1 < colorSize; i1++) {
            for (int ei = 0; ei < events.size(); ei++) {
                for (int i2 = 0; i2 < colorSize; i2++) {
                    for (int i3 = 0; i3 < i2; i3++) {
                        constraints.add(new int[] {
                                -yVar(i1, i2, ei).number,
                                -yVar(i1, i3, ei).number
                        });
                    }
                }
            }
        }
    }

    // BFS constraints

    protected BooleanVariable pVar(int j, int i) {
        return BooleanVariable.byName("p", j, i).get();
    }

    protected BooleanVariable tVar(int i, int j) {
        return BooleanVariable.byName("t", i, j).get();
    }

    protected BooleanVariable mVar(int event, int i, int j) {
        return BooleanVariable.byName("m", event, i, j).get();
    }

    protected void addBFSVars() {
        if (!bfsConstraints) {
            return;
        }
        // p_ji, t_ij
        for (int i = 0; i < colorSize; i++) {
            for (int j = i + 1; j < colorSize; j++) {
                vars.add(BooleanVariable.getOrCreate("p", j, i));
                vars.add(BooleanVariable.getOrCreate("t", i, j));
            }
        }
        if (events.size() > 2) {
            // m_eij
            for (int ei = 0; ei < events.size(); ei++) {
                for (int i = 0; i < colorSize; i++) {
                    for (int j = i + 1; j < colorSize; j++) {
                        vars.add(BooleanVariable.getOrCreate("m", ei, i, j));
                    }
                }
            }
        }
    }

    protected void addBFSConstraints(List<int[]> constraints) {
        if (bfsConstraints) {
            parentConstraints(constraints);
            pDefinitions(constraints);
            tDefinitions(constraints);
            childrenOrderConstraints(constraints);
        }
    }

    protected void parentConstraints(List<int[]> constraints) {
        for (int j = 1; j < colorSize; j++) {
            final int[] options = new int[j];
            for (int i = 0; i < j; i++) {
                options[i] = pVar(j, i).number;
            }
            constraints.add(options);
        }

        for (int k = 0; k < colorSize; k++) {
            for (int i = k + 1; i < colorSize; i++) {
                for (int j = i + 1; j < colorSize - 1; j++) {
                    constraints.add(new int[] {
                            -pVar(j, i).number,
                            -pVar(j + 1, k).number
                    });
                }
            }
        }
    }

    protected void pDefinitions(List<int[]> constraints) {
        for (int i = 0; i < colorSize; i++) {
            for (int j = i + 1; j < colorSize; j++) {
                constraints.add(new int[] {
                        -pVar(j, i).number,
                        tVar(i, j).number
                });
                final int[] options = new int[i + 2];
                for (int k = i - 1; k >= 0; k--) {
                    constraints.add(new int[] {
                            -pVar(j, i).number,
                            -tVar(k, j).number
                    });
                    options[k] = tVar(k, j).number;
                }
                options[i] = -tVar(i, j).number;
                options[i + 1] = pVar(j, i).number;
                constraints.add(options);
            }
        }
    }

    protected void tDefinitions(List<int[]> constraints) {
        for (int i = 0; i < colorSize; i++) {
            for (int j = i + 1; j < colorSize; j++) {
                final int[] options = new int[events.size() + 1];
                for (int ei = 0; ei < events.size(); ei++) {
                    constraints.add(new int[] {
                            -yVar(i, j, ei).number,
                            tVar(i, j).number
                    });
                    options[ei] = yVar(i, j, ei).number;
                }
                options[events.size()] = -tVar(i, j).number;
                constraints.add(options);
            }
        }
    }

    protected void childrenOrderConstraints(List<int[]> constraints) {
        if (events.size() > 2) {
            // m definitions
            for (int i = 0; i < colorSize; i++) {
                for (int j = i + 1; j < colorSize; j++) {
                    for (int ei1 = 0; ei1 < events.size(); ei1++) {
                        constraints.add(new int[] {
                                -mVar(ei1, i, j).number,
                                yVar(i, j, ei1).number
                        });
                        final int[] options = new int[ei1 + 2];
                        for (int ei2 = ei1 - 1; ei2 >= 0; ei2--) {
                            constraints.add(new int[] {
                                    -mVar(ei1, i, j).number,
                                    -yVar(i, j, ei2).number
                            });
                            options[ei2] = yVar(i, j, ei2).number;
                        }
                        options[ei1] = -yVar(i, j, ei1).number;
                        options[ei1 + 1] = mVar(ei1, i, j).number;
                        constraints.add(options);
                    }
                }
            }
            // children constraints
            for (int i = 0; i < colorSize; i++) {
                for (int j = i + 1; j < colorSize - 1; j++) {
                    for (int k = 0; k < events.size(); k++) {
                        for (int n = k + 1; n < events.size(); n++) {
                            constraints.add(new int[] {
                                    -pVar(j, i).number,
                                    -pVar(j + 1, i).number,
                                    -mVar(n, i, j).number,
                                    -mVar(k, i, j + 1).number
                            });
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < colorSize; i++) {
                for (int j = i + 1; j < colorSize - 1; j++) {
                    constraints.add(new int[] {
                            -pVar(j, i).number,
                            -pVar(j + 1, i).number,
                            yVar(i, j, 0).number
                    });
                }
            }
        }
    }
}
