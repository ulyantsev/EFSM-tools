package algorithms.automaton_builders;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import algorithms.formula_builders.DimacsCnfBuilder;
import structures.Automaton;
import structures.Node;
import structures.ScenarioTree;
import structures.Transition;

public class CryptominisatAutomatonBuilder {
	public static Automaton build(ScenarioTree tree, int k) throws IOException {
		return build(tree, k, null);
	}	
	
	public static Automaton build(ScenarioTree tree, int k, PrintWriter cnfPrintWriter) throws IOException {
		return build(tree, k, cnfPrintWriter, null);
	}

	public static Automaton build(ScenarioTree tree, int k, PrintWriter cnfPrintWriter, PrintWriter solverPrintWriter)
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

			Automaton ans = new Automaton(k);
			for (int i = 0; i < tree.nodeCount(); i++) {
				int color = nodesColors[i];
				Node state = ans.state(color);
				for (Transition t : tree.nodes().get(i).transitions()) {
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
