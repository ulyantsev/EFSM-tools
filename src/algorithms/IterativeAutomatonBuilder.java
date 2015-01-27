package algorithms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import qbf.Verifier;
import qbf.ltl.LtlNode;
import qbf.reduction.Assignment;
import qbf.reduction.BinaryOperation;
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

				List<String> properUniqueActions = new ArrayList<>();
				for (Assignment az : ass.list()) {
					if (az.value && az.var.name.startsWith("z_" + from + "_")
							&& az.var.name.endsWith("_" + event + "_" + expr)) {
						properUniqueActions.add(az.var.name.split("_")[2]);
					}
				}
				Collections.sort(properUniqueActions);

				if (!state.hasTransition(event, expr)) {
					// add
					state.addTransition(event, expr,
						new StringActions(String.join(",",
						properUniqueActions)), ans.getState(to));
					logger.info("ADDING TRANSITION NOT FROM SCENARIOS");
				} else {
					// check
					Transition t = state.getTransition(event, expr);
					if (t.getDst() != ans.getState(to)) {
						logger.severe("INVALID TRANSITION DESTINATION");
						return Pair.of(Optional.empty(), list);
					}
					List<String> actualActions = new ArrayList<>(new TreeSet<>(
							Arrays.asList(t.getActions().getActions())));
					if (!actualActions.equals(properUniqueActions)) {
						logger.severe("ACTIONS DO NOT MATCH");
						return Pair.of(Optional.empty(), list);
					}
				}
			}

			return Pair.of(Optional.of(ans), list);
		} catch (ParseException e) {
			e.printStackTrace();
			return Pair.of(Optional.empty(), list);
		}
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int colorSize, String solverParams, boolean complete, int timeoutSeconds,
			String resultFilePath, String ltlFilePath, List<LtlNode> formulae) throws IOException {
		BooleanFormula initialBf = new SatFormulaBuilder(tree, colorSize, complete).getFormula();
		FormulaList additionalConstraints = new FormulaList(BinaryOperations.AND);
		
		Optional<Automaton> fsm = Optional.empty();
		while (true) {
			BooleanFormula actualFormula = initialBf.and(additionalConstraints.assemble());
			Pair<Optional<Automaton>, List<Assignment>> p = automatonFromFormula(actualFormula, logger, solverParams, timeoutSeconds, tree, colorSize);
			fsm = p.getLeft();
			if (fsm.isPresent()) {// writing file
				try (PrintWriter resultPrintWriter = new PrintWriter(new File(resultFilePath))) {
					resultPrintWriter.println(fsm);
				} catch (FileNotFoundException e) {
					logger.warning("File " + resultFilePath + " not found: " + e.getMessage());
				}
				boolean verified = Verifier.verify(resultFilePath, ltlFilePath, colorSize, formulae, logger);
				if (verified) {
					return fsm;
				}
				List<Assignment> assList = p.getRight().stream()
						.filter(ass -> !ass.var.name.startsWith("x"))
						.collect(Collectors.toList());
				System.out.println(assList.stream().filter(x -> x.value).map(x -> x.var)
						.collect(Collectors.toList()));
				List<BooleanFormula> constraints = assList.stream()
						.map(ass -> ass.value ? ass.var : ass.var.not())
						.collect(Collectors.toList());
				additionalConstraints.add(BinaryOperation.and(constraints).not());
			} else {
				// no solution
				return fsm;
			}
		}
	}
}
