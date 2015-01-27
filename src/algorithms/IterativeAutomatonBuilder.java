package algorithms;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import qbf.reduction.Assignment;
import qbf.reduction.BinaryOperations;
import qbf.reduction.BooleanFormula;
import qbf.reduction.FormulaList;
import qbf.reduction.SolverResult;
import qbf.reduction.SolverResult.SolverResults;
import structures.Automaton;
import structures.Node;
import structures.ScenariosTree;
import structures.Transition;
import actions.StringActions;
import bool.MyBooleanExpression;

public class IterativeAutomatonBuilder {
	private static Pair<Optional<Automaton>, List<Assignment>> automatonFromFormula(BooleanFormula bf, Logger logger, String solverParams, int timeoutSeconds, ScenariosTree tree, int colorSize) throws IOException {
		// delete files from the previous run
		Arrays.stream(new File(".").listFiles())
			.filter(f -> f.getName().contains("_tmp."))
			.forEach(File::delete);
		String strBf = bf.toLimbooleString().replace(" ", "");
		Pair<List<Assignment>, Long> solution = BooleanFormula.solveAsSat(strBf, logger, solverParams, timeoutSeconds);
		List<Assignment> list = solution.getLeft();
		long time = solution.getRight();
		
		SolverResult ass = list.isEmpty()
			? new SolverResult(time >= timeoutSeconds * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT, (int) time)
			: new SolverResult(list, (int) time);
		logger.info(ass.toString().split("\n")[0]);
		
		if (ass.type() != SolverResults.SAT) {
			return Pair.of(Optional.empty(), list);
		}
		
		// A reduced copy of the code from QbfAutomatonBuilder. TODO refactor
		try {
			int[] nodeColors = new int[tree.nodesCount()];

			ass.list().stream()
					.filter(a -> a.value && a.var.name.startsWith("x"))
					.forEach(a -> {
						String[] tokens = a.var.name.split("_");
						assert tokens.length == 3;
						int node = Integer.parseInt(tokens[1]);
						int color = Integer.parseInt(tokens[2]);
						nodeColors[node] = color;
					});

			// add transitions from scenarios
			Automaton ans = new Automaton(colorSize);
			for (int i = 0; i < tree.nodesCount(); i++) {
				int color = nodeColors[i];
				Node state = ans.getState(color);
				for (Transition t : tree.getNodes().get(i).getTransitions()) {
					if (!state.hasTransition(t.getEvent(), t.getExpr())) {
						int childColor = nodeColors[t.getDst().getNumber()];
						state.addTransition(t.getEvent(), t.getExpr(),
							t.getActions(), ans.getState(childColor));
					}
				}
			}

			// add other transitions
			for (Assignment a : ass.list().stream()
					.filter(a -> a.value && a.var.name.startsWith("y"))
					.collect(Collectors.toList())) {
				String[] tokens = a.var.name.split("_");
				assert tokens.length == 5;
				int from = Integer.parseInt(tokens[1]);
				int to = Integer.parseInt(tokens[2]);
				String event = tokens[3];
				MyBooleanExpression expr = MyBooleanExpression.get(tokens[4]);
				Node state = ans.getState(from);
				if (!state.hasTransition(event, expr)) {
					// add
					state.addTransition(event, expr,
						new StringActions(""), ans.getState(to));
					logger.info("ADDING TRANSITION WITH EMPTY ACTIONS NOT FROM SCENARIOS");
				}
			}

			return Pair.of(Optional.of(ans), list);
		} catch (ParseException e) {
			e.printStackTrace();
			return Pair.of(Optional.empty(), list);
		}
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int colorSize, String solverParams, boolean complete, int timeoutSeconds) throws IOException {
		BooleanFormula initialBf = new SatFormulaBuilder(tree, colorSize, complete).getFormula();
		FormulaList additionalConstraints = new FormulaList(BinaryOperations.AND);
		
		Optional<Automaton> fsm = Optional.empty();
		while (true) {
			BooleanFormula actualFormula = initialBf.and(additionalConstraints.assemble());
			Pair<Optional<Automaton>, List<Assignment>> p = automatonFromFormula(actualFormula, logger, solverParams, timeoutSeconds, tree, colorSize);
			fsm = p.getLeft();
			if (fsm.isPresent()) {
				// verify
				// TODO
				List<Assignment> ass = p.getRight();
				// only ys should be added into the additional constraints
				logger.info(ass.toString());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			} else {
				// no solution
				return fsm;
			}
		}
	}
}
