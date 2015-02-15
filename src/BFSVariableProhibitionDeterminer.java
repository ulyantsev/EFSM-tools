import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import qbf.reduction.Assignment;
import qbf.reduction.BinaryOperation;
import qbf.reduction.BinaryOperations;
import qbf.reduction.BooleanFormula;
import qbf.reduction.BooleanVariable;
import qbf.reduction.FormulaList;
import algorithms.QbfAutomatonBuilder;
import algorithms.FormulaBuilder.EventExpressionPair;
import bool.MyBooleanExpression;

public class BFSVariableProhibitionDeterminer {

	public static void main(String[] args) throws ParseException, IOException {
		new File(QbfAutomatonBuilder.PRECOMPUTED_DIR_NAME).mkdir();
		for (int statesNum = 2; statesNum <= 10; statesNum++) {
			for (int eventNum = 2; eventNum <= 5; eventNum++) {
				System.out.println(statesNum + " " + eventNum);
				List<EventExpressionPair> l = new ArrayList<>();
				for (int i = 0; i < eventNum; i++) {
					l.add(new EventExpressionPair((char)('A' + i) + "", MyBooleanExpression.get("1")));
				}
				BFSVariableProhibitionDeterminer d = new BFSVariableProhibitionDeterminer(statesNum, l);
				Logger logger = Logger.getLogger("Logger");
				logger.setUseParentHandlers(false);
				Map<String, Boolean> res = d.check(logger);
				try (PrintWriter pw = new PrintWriter(new File(QbfAutomatonBuilder.PRECOMPUTED_DIR_NAME + "/" + statesNum + "_" + eventNum))) {
					for (Map.Entry<String, Boolean> e : res.entrySet()) {
						if (!e.getValue()) {
							pw.println(e.getKey());
						}
					}
				}
			}
		}
	}
	
	private final List<BooleanVariable> existVars = new ArrayList<>();
	private final FormulaList constraints = new FormulaList(BinaryOperations.AND);
	private final List<EventExpressionPair> efPairs;
	private final int colorSize;
	
	private BooleanVariable yVar(int from, int to, String event, MyBooleanExpression f) {
		return BooleanVariable.byName("y", from, to, event, f).get();
	}
	
	private BooleanVariable pVar(int j, int i) {
		return BooleanVariable.byName("p", j, i).get();
	}
	
	private BooleanVariable tVar(int i, int j) {
		return BooleanVariable.byName("t", i, j).get();
	}
	
	private BooleanVariable mVar(String event, MyBooleanExpression f, int i, int j) {
		return BooleanVariable.byName("m", event, f, i, j).get();
	}
	
	private void addVariables() {
		// y
		for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
			for (EventExpressionPair p : efPairs) {
				for (int childColor = 0; childColor < colorSize; childColor++) {
					existVars.add(new BooleanVariable("y", nodeColor, childColor, p.event, p.expression));
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
		if (efPairs.size() > 2) {
			// m_efij
			for (EventExpressionPair p : efPairs) {
				for (int i = 0; i < colorSize; i++) {
					for (int j = i + 1; j < colorSize; j++) {
						existVars.add(new BooleanVariable("m", p.event, p.expression, i, j));
					}
				}
			}
		}
	}
	
	public BFSVariableProhibitionDeterminer(int colorSize, List<EventExpressionPair> efPairs) {
		this.efPairs = efPairs;
		this.colorSize = colorSize;
		addVariables();
		parentConstraints();
		pDefinitions();
		tDefinitions();
		childrenOrderConstraints();
		notMoreThanOneEdgeConstraints();
	}
	
	public Map<String, Boolean> check(Logger logger) throws IOException {
		Map<String, Boolean> results = new LinkedHashMap<>();
		for (BooleanVariable v : existVars) {
			if (v.name.startsWith("y")) {
				constraints.add(v);
				List<Assignment> list = BooleanFormula.solveAsSat(constraints.assemble().toLimbooleString(), logger, "", 60).getLeft();
				results.put(v.name, !list.isEmpty());
				constraints.removeLast();
			}
		}
		return results;
	}
	
	private void notMoreThanOneEdgeConstraints() {
		for (EventExpressionPair p : efPairs) {
			for (int parentColor = 0; parentColor < colorSize; parentColor++) {
				for (int color1 = 0; color1 < colorSize; color1++) {
					BooleanVariable v1 = yVar(parentColor, color1, p.event, p.expression);
					for (int color2 = 0; color2 < color1; color2++) {
						BooleanVariable v2 = yVar(parentColor, color2, p.event, p.expression);
						constraints.add(v1.not().or(v2.not()));
					}
				}
			}
		}
	}
	
	private void parentConstraints() {
		for (int j = 1; j < colorSize; j++) {
			FormulaList options = new FormulaList(BinaryOperations.OR);
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
				FormulaList definition = new FormulaList(BinaryOperations.AND);
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
				FormulaList definition = new FormulaList(BinaryOperations.OR);
				for (EventExpressionPair p : efPairs) {
					definition.add(yVar(i, j, p.event, p.expression));
				}
				constraints.add(tVar(i, j).equivalent(definition.assemble()));
			}
		}
	}
	
	private void childrenOrderConstraints() {
		if (efPairs.size() > 2) {
			// m definitions
			for (int i = 0; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize; j++) {
					for (int pairIndex1 = 0; pairIndex1 < efPairs.size(); pairIndex1++) {
						EventExpressionPair p1 = efPairs.get(pairIndex1);
						FormulaList definition = new FormulaList(BinaryOperations.AND);
						definition.add(yVar(i, j, p1.event, p1.expression));
						for (int pairIndex2 = pairIndex1 - 1; pairIndex2 >= 0; pairIndex2--) {
							EventExpressionPair p2 = efPairs.get(pairIndex2);
							definition.add(yVar(i, j, p2.event, p2.expression).not());
						}
						constraints.add(mVar(p1.event, p1.expression, i, j).equivalent(definition.assemble()));
					}
				}
			}
			// children constraints
			for (int i = 0; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize - 1; j++) {
					for (int k = 0; k < efPairs.size(); k++) {
						for (int n = k + 1; n < efPairs.size(); n++) {
							constraints.add(
									BinaryOperation.and(
											pVar(j, i), pVar(j + 1, i),
											mVar(efPairs.get(n).event, efPairs.get(n).expression, i, j)
									).implies(
											mVar(efPairs.get(k).event, efPairs.get(k).expression, i, j + 1).not()
									)
							);
						}
					}
				}
			}
		} else {
			for (int i = 0; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize - 1; j++) {
					constraints.add(
							pVar(j, i).and(pVar(j + 1, i))
							.implies(yVar(i, j, efPairs.get(0).event, efPairs.get(0).expression))
					);
				}
			}
		}
	}
}
