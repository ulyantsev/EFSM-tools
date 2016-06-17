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

import structures.Node;
import structures.ScenarioTree;
import algorithms.AdjacencyCalculator;

public class MaxCliqueFinderMain {
	public static void main(String[] args) throws IOException {
		if (args.length < 1 || args.length > 2) {
            System.out.println("Greedy max-clique finder for a given scenario file");
            System.out.println("Author: Igor Buzhinsky, igor.buzhinsky@gmail.com\n");
            System.out.println("Usage: java -jar max-clique-finder.jar <scenarios.sc> [<varNumber> (default 0)]");
            return;
        }

		final String filename = args[0];
		final int varNumber = args.length == 1 ? 0 : Integer.parseInt(args[1]);
		
		try {
			final ScenarioTree tree = new ScenarioTree();
			tree.load(filename, varNumber);
			final Map<Node, Set<Node>> adjacent = AdjacencyCalculator.getAdjacent(tree);
			final Set<Node> clique = findClique(tree.root(), adjacent);
			checkClique(clique, adjacent);
			System.out.println("MAX-CLIQUE SIZE: " + clique.size());
			System.out.println("NODES: " + clique.stream().map(node -> node.number()).sorted().collect(Collectors.toList()));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
			return;
		}
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
			Node anotherOne = findNeighborWithHighestDegree(clique, last, adjacent);
			while (anotherOne != null) {
				clique.add(anotherOne);
				last = anotherOne;
				anotherOne = findNeighborWithHighestDegree(clique, last, adjacent);
			}
		} else {
			clique.add(root);
		}
		
		return clique;
	}

	private static Node findNeighborWithHighestDegree(Set<Node> cur, Node v, Map<Node, Set<Node>> adjacent) {
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
