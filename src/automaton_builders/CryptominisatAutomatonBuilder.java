package automaton_builders;

import formula_builders.DimacsCnfBuilder;
import structures.mealy.MealyAutomaton;
import structures.mealy.MealyNode;
import structures.mealy.MealyTransition;
import structures.mealy.ScenarioTree;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CryptominisatAutomatonBuilder {
	public static MealyAutomaton build(ScenarioTree tree, int k) throws IOException {
		return build(tree, k, null);
	}	
	
	public static MealyAutomaton build(ScenarioTree tree, int k, PrintWriter cnfPrintWriter) throws IOException {
		return build(tree, k, cnfPrintWriter, null);
	}

	public static MealyAutomaton build(ScenarioTree tree, int k, PrintWriter cnfPrintWriter, PrintWriter solverPrintWriter)
			throws IOException {

		final String cnf = DimacsCnfBuilder.getCnf(tree, k);
		if (cnfPrintWriter != null) {
			cnfPrintWriter.println(cnf);
			cnfPrintWriter.flush();
		}

		final File tmpFile = new File("tmp.cnf");
        try (PrintWriter tmpPW = new PrintWriter(tmpFile)) {
            tmpPW.print(cnf);
        }

		final Process p = Runtime.getRuntime().exec("cryptominisat4 --threads=4 tmp.cnf");
		// Process p = Runtime.getRuntime().exec("cryptominisat tmp.cnf");

		final List<String> ansLines = new ArrayList<>();
		try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = input.readLine()) != null) {
                if (solverPrintWriter != null) {
                    solverPrintWriter.println(line);
                }
                if (line.charAt(0) == 'v') {
                    ansLines.add(line);
                }
            }
        }
		tmpFile.delete();

		if (!ansLines.isEmpty()) {
            final int[] nodeColors = new int[tree.nodeCount()];
            ansLines.forEach(l -> Arrays.stream(l.split(" "))
                    .skip(1)
                    .filter(v -> !v.startsWith("-"))
                    .mapToInt(v -> Integer.parseInt(v) - 1)
                    .filter(v -> v != -1 && v < nodeColors.length * k)
                    .forEach(v -> nodeColors[v / k] = v % k)
            );

			final MealyAutomaton ans = new MealyAutomaton(k);
			for (int i = 0; i < tree.nodeCount(); i++) {
                final int color = nodeColors[i];
				final MealyNode state = ans.state(color);
				for (MealyTransition t : tree.nodes().get(i).transitions()) {
					if (!state.hasTransition(t.event(), t.expr())) {
						int childColor = nodeColors[t.dst().number()];
						state.addTransition(t.event(), t.expr(), t.actions(), ans.state(childColor));
					}
				}
			}
			return ans;
		}

		return null;
	}
}
