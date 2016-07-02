package automaton_builders;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import formula_builders.DimacsCnfBuilder;
import structures.mealy.MealyAutomaton;
import structures.mealy.MealyNode;
import structures.mealy.ScenarioTree;
import structures.mealy.MealyTransition;

public class CryptominisatAutomatonBuilder {
	public static MealyAutomaton build(ScenarioTree tree, int k) throws IOException {
		return build(tree, k, null);
	}	
	
	public static MealyAutomaton build(ScenarioTree tree, int k, PrintWriter cnfPrintWriter) throws IOException {
		return build(tree, k, cnfPrintWriter, null);
	}

	public static MealyAutomaton build(ScenarioTree tree, int k, PrintWriter cnfPrintWriter, PrintWriter solverPrintWriter)
			throws IOException {

		String cnf = DimacsCnfBuilder.getCnf(tree, k);
		if (cnfPrintWriter != null) {
			cnfPrintWriter.println(cnf);
			cnfPrintWriter.flush();
		}

		File tmpFile = new File("tmp.cnf");
        try (PrintWriter tmpPW = new PrintWriter(tmpFile)) {
            tmpPW.print(cnf);
        }

		Process p = Runtime.getRuntime().exec("cryptominisat --threads=4 tmp.cnf");
		// Process p = Runtime.getRuntime().exec("cryptominisat tmp.cnf");

		String ansLine = null;
		try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = input.readLine()) != null) {
                if (solverPrintWriter != null) {
                    solverPrintWriter.println(line);
                }
                if (line.charAt(0) == 'v') {
                    ansLine = line;
                }
            }
        }
		tmpFile.delete();

		if (ansLine != null) {
			int[] nodesColors = new int[tree.nodeCount()];
			String[] sp = ansLine.split(" ");
			for (int nodeNum = 0; nodeNum < tree.nodeCount(); nodeNum++) {
				for (int color = 0; color < k; color++) {
					if (sp[1 + nodeNum * k + color].charAt(0) != '-') {
						nodesColors[nodeNum] = color;
					}
				}
			}

			MealyAutomaton ans = new MealyAutomaton(k);
			for (int i = 0; i < tree.nodeCount(); i++) {
				int color = nodesColors[i];
				MealyNode state = ans.state(color);
				for (MealyTransition t : tree.nodes().get(i).transitions()) {
					if (!state.hasTransition(t.event(), t.expr())) {
						int childColor = nodesColors[t.dst().number()];
						state.addTransition(t.event(), t.expr(), t.actions(), ans.state(childColor));
					}
				}
			}
			return ans;
		}

		return null;
	}
}
