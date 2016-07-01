package main;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import structures.Node;
import structures.ScenarioTree;
import algorithms.AdjacencyCalculator;

public class MaxCliqueFinderMain extends MainBase {
    @Argument(usage = "scenario file path", metaVar = "<scenarios>", required = true, index = 0)
    private String sc;

    @Argument(usage = "number of variables", metaVar = "<varNumber (default 0)>", required = false, index = 1)
    private int varNumber = 0;

    public static void main(String[] args) {
        new MaxCliqueFinderMain().run(args, Author.IB, "Greedy max-clique finder for a given scenario file");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        final ScenarioTree tree = new ScenarioTree();
        tree.load(sc, varNumber);
        final Map<Node, Set<Node>> adjacent = AdjacencyCalculator.getAdjacent(tree);
        final Set<Node> clique = findClique(tree.root(), adjacent);
        checkClique(clique, adjacent);
        System.out.println("MAX-CLIQUE SIZE: " + clique.size());
        System.out.println("NODES: " + clique.stream().map(Node::number).sorted().collect(Collectors.toList()));
	}
	
	private static void checkClique(Set<Node> clique, Map<Node, Set<Node>> adjacent) {
		for (Node u : clique) {
			for (Node v : clique) {
				if (u != v) {
					if (!adjacent.get(u).contains(v)) {
						throw new AssertionError();
					}
				}
			}
		}
	}
	
	private static Set<Node> findClique(Node root, Map<Node, Set<Node>> adjacent) {
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
			Node anotherOne = neighborWithHighestDegree(clique, last, adjacent);
			while (anotherOne != null) {
				clique.add(anotherOne);
				last = anotherOne;
				anotherOne = neighborWithHighestDegree(clique, last, adjacent);
			}
		} else {
			clique.add(root);
		}
		
		return clique;
	}

	private static Node neighborWithHighestDegree(Set<Node> cur, Node v, Map<Node, Set<Node>> adjacent) {
		int maxDegree = 0;
		Node maxNeighbour = null;
		for (Node u : adjacent.get(v)) {
			boolean uInClique = true;
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
