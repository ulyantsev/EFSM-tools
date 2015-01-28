package algorithms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
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
import structures.ScenariosTree;

public class IterativeAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static Pair<Optional<Automaton>, List<Assignment>> automatonFromFormula(BooleanFormula bf, Logger logger, String solverParams, int timeoutSeconds, ScenariosTree tree, int colorSize) throws IOException {
		deleteTrash();
		try (PrintWriter pw = new PrintWriter("_tmp.pretty")) {
			pw.print(bf.toString());
		}
		String strBf = bf.toLimbooleString();
		Pair<List<Assignment>, Long> solution = BooleanFormula.solveAsSat(strBf, logger, solverParams, timeoutSeconds);
		List<Assignment> list = solution.getLeft();
		long time = solution.getRight();
		
		SolverResult ass = list.isEmpty()
			? new SolverResult(time >= timeoutSeconds * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT, (int) time)
			: new SolverResult(list, (int) time);
		logger.info(ass.toString().split("\n")[0]);

		return Pair.of(ass.type() != SolverResults.SAT ?
				Optional.empty() : constructAutomatonFromAssignment(logger, ass, tree, colorSize), list);
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int colorSize, String solverParams, boolean complete, int timeoutSeconds,
			String resultFilePath, String ltlFilePath, List<LtlNode> formulae, boolean bfsConstraints) throws IOException {
		BooleanFormula initialBf = new SatFormulaBuilder(tree, colorSize, complete, bfsConstraints).getFormula();
		FormulaList additionalConstraints = new FormulaList(BinaryOperations.AND);
		
		Optional<Automaton> fsm = Optional.empty();
		int iterations = 0;
		while (true) {
			iterations++;
			BooleanFormula actualFormula = initialBf.and(additionalConstraints.assemble());
			Pair<Optional<Automaton>, List<Assignment>> p = automatonFromFormula(actualFormula, logger, solverParams, timeoutSeconds, tree, colorSize);
			fsm = p.getLeft();
			if (fsm.isPresent()) {// writing file
				try (PrintWriter resultPrintWriter = new PrintWriter(new File(resultFilePath))) {
					resultPrintWriter.println(fsm);
				} catch (FileNotFoundException e) {
					logger.warning("File " + resultFilePath + " not found: " + e.getMessage());
				}
				if (Verifier.verify(resultFilePath, ltlFilePath, colorSize, formulae, logger)) {
					logger.info("Iterations: " + iterations);
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
				logger.info("Iterations: " + iterations);
				return fsm;
			}
		}
	}
}
