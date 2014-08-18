package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import qbf.ltl.LtlNode;
import qbf.reduction.Assignment;
import qbf.reduction.QuantifiedBooleanFormula;
import qbf.reduction.SolverResult;
import qbf.reduction.SolverResult.SolverResults;
import qbf.reduction.Solvers;
import scenario.StringScenario;
import structures.Automaton;
import structures.Node;
import structures.ScenariosTree;
import structures.Transition;
import actions.StringActions;
import bool.MyBooleanExpression;

public class QbfAutomatonBuilder {
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, List<LtlNode> formulae, int colorSize, int depth, int timeoutSeconds, Solvers solver, boolean extractSubterms, boolean complete, List<String> scenarioPaths) throws IOException {
		// delete files from the previous run
		for (File f : new File(".").listFiles()) {
			if (f.getName().contains("_tmp.")) {
				f.delete();
			}
		}
		
		QbfFormulaBuilder qfb = new QbfFormulaBuilder(logger, tree, formulae, colorSize, depth, extractSubterms, complete);
		QuantifiedBooleanFormula qbf = qfb.getLTLformat(tree);
		SolverResult ass = qbf.solve(logger, solver, timeoutSeconds);
		logger.info(ass.toString().split("\n")[0]);
		
		if (ass.type() != SolverResults.SAT) {
			return Optional.empty();
		}
		
        try {
			int[] nodesColors = new int[tree.nodesCount()];

			ass.list().stream().filter(a -> a.value && a.var.name.startsWith("x")).forEach(a -> {
				String[] tokens = a.var.name.split("_");
				assert tokens.length == 3;
				int node = Integer.parseInt(tokens[1]);
				int color = Integer.parseInt(tokens[2]);
				nodesColors[node] = color;
			});
			
			// add transitions from scenarios
			Automaton ans = new Automaton(colorSize);
			for (int i = 0; i < tree.nodesCount(); i++) {
				int color = nodesColors[i];
				Node state = ans.getState(color);
				for (Transition t : tree.getNodes().get(i).getTransitions()) {
					if (!state.hasTransition(t.getEvent(), t.getExpr())) {
						int childColor = nodesColors[t.getDst().getNumber()];
						state.addTransition(t.getEvent(), t.getExpr(),
							t.getActions(), ans.getState(childColor));
					}
				}
			}
			
			// add other transitions
			for (Assignment a : ass.list().stream().filter(a -> a.value && a.var.name.startsWith("y")).collect(Collectors.toList())) {
				String[] tokens = a.var.name.split("_");
				assert tokens.length == 5;
				int from = Integer.parseInt(tokens[1]);
				int to = Integer.parseInt(tokens[2]);
				String event = tokens[3];
				MyBooleanExpression expr = MyBooleanExpression.get(tokens[4]);
				
				Node state = ans.getState(from);
				
				List<String> properUniqieActions = new ArrayList<>();
				for (Assignment az : ass.list()) {
					if (az.value && az.var.name.startsWith("z_" + from + "_") && az.var.name.endsWith("_" + event + "_" + expr)) {
						properUniqieActions.add(az.var.name.split("_")[2]);
					}
				}
				Collections.sort(properUniqieActions);
				
				if (!state.hasTransition(event, expr)) {
					// add
					state.addTransition(event, expr,
						new StringActions(String.join(",", properUniqieActions)), ans.getState(to));
					logger.info("ADDING TRANSITION NOT FROM SCENARIOS");
				} else {
					// check
					Transition t = state.getTransition(event, expr);
					if (t.getDst() != ans.getState(to)) {
						logger.severe("INVALID TRANSITION DESTINATION");
					}
					List<String> actualActions = new ArrayList<>(new TreeSet<>(Arrays.asList(t.getActions().getActions())));
					if (!actualActions.equals(properUniqieActions)) {
						logger.severe("ACTIONS DO NOT MATCH");
					}
				}
			}
			
			List<StringScenario> scenarios = new ArrayList<>();
        	for (String scenarioPath : scenarioPaths) {
        		scenarios.addAll(StringScenario.loadScenarios(scenarioPath));
        	}

			if (scenarios.stream().allMatch(ans::isCompliesWithScenario)) {
				logger.info("COMPLIES WITH SCENARIOS");
			} else {
				logger.severe("NOT COMPLIES WITH SCENARIOS");
			}

			return Optional.of(ans);
        } catch (FileNotFoundException | ParseException e) {
            e.printStackTrace();
            return Optional.empty();
        }
	}
}
