import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import structures.Node;
import structures.ScenariosTree;
import algorithms.AdjacentCalculator;

public class MaxCliqueFinder {
	public static void main(String[] args) throws IOException {
		final File dir = new File("./qbf/testing/incomplete");
		
		for (String name : Arrays.stream(dir.list())
				.filter(name -> name.endsWith(".sc"))
				.sorted()
				.collect(Collectors.toList())) {
			System.out.println("*** " + name);
				
			final ScenariosTree tree = new ScenariosTree();

			try {
				tree.load(dir.getAbsolutePath() + "/" + name, 0);
			} catch (IOException | ParseException e) {
				e.printStackTrace();
				return;
			}

			final Map<Node, Set<Node>> adjacent = AdjacentCalculator.getAdjacent(tree);
			System.out.println(adjacent.size() + " nodes in the consistency graph");
			System.out.println(findClique(adjacent).size() + " is the max-clique size");
		}
		
		// "./qbf/walkinshaw/editor.sc", "./qbf/walkinshaw/jhotdraw.sc", "./qbf/walkinshaw/cvs.sc"
		// zero clique
	}
	
	private static Set<Node> findClique(Map<Node, Set<Node>> adjacent) {
		int maxDegree = 0;
		Node maxV = null;
		final Set<Node> clique = new LinkedHashSet<>();
		
		for (Map.Entry<Node, Set<Node>> pair : adjacent.entrySet()) {
			Node candidate = pair.getKey();
			int candidateDegree = pair.getValue().size();
			if (candidateDegree > maxDegree) {
				maxDegree = candidateDegree;
				maxV = candidate;
			}
		}
		
		Node last = maxV;
		if (last != null) {
			clique.add(last);
			Node anotherOne = findNeighborWithHighestDegree(clique, last, adjacent);
			while (anotherOne != null) {
				clique.add(anotherOne);
				last = anotherOne;
				anotherOne = findNeighborWithHighestDegree(clique, last, adjacent);
			}
		}
		return clique;
	}

	private static Node findNeighborWithHighestDegree(Set<Node> cur, Node v, Map<Node, Set<Node>> adjacent) {
		int maxDegree = 0;
		Node maxNeighbour = null;
		// uv - edge
		for (Node u : adjacent.get(v)) {
			boolean uInClique = true;
			// check if other vertices in cur connected with u
			for (Node w : cur) {
				if (w != v) {
					if (!adjacent.get(w).contains(u)) {
						uInClique = false;
						break;
					}
				}
			}
			if (uInClique) {
				int uDegree = adjacent.get(u).size();
				if (uDegree > maxDegree) {
					maxDegree = uDegree;
					maxNeighbour = u;
				}
			}
		}
		return maxNeighbour;
	}
}
