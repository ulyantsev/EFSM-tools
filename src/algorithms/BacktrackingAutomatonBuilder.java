package algorithms;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import qbf.ltl.LtlNode;
import structures.Automaton;
import structures.ScenariosTree;

public class BacktrackingAutomatonBuilder {
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int colorSize, boolean complete,
			int timeoutSeconds, String resultFilePath, String ltlFilePath, List<LtlNode> formulae) throws IOException {
		// TODO
		
		// recursive
		// layer = uncovered layer
		// keep current scenario coloring
		// step:
		//	select transitions from the layer
		//  sort them according to some criterion
		// 	add them in the ways which retain BFS enumeration
		//  probably verify (not f)
		
		return Optional.empty();
	}
}
