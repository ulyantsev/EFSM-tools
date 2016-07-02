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
import structures.MealyNode;
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
        final Map<MealyNode, Set<MealyNode>> adjacent = AdjacencyCalculator.getAdjacent(tree);
        final Set<MealyNode> clique = findClique(tree.root(), adjacent);
        checkClique(clique, adjacent);
        System.out.println("MAX-CLIQUE SIZE: " + clique.size());
        System.out.println("NODES: " + clique.stream().map(MealyNode::number).sorted().collect(Collectors.toList()));
	}
	
	private static void checkClique(Set<MealyNode> clique, Map<MealyNode, Set<MealyNode>> adjacent) {
		for (MealyNode u : clique) {
			for (MealyNode v : clique) {
				if (u != v) {
					if (!adjacent.get(u).contains(v)) {
						throw new AssertionError();
					}
				}
			}
		}
	}
	
	private static Set<MealyNode> findClique(MealyNode root, Map<MealyNode, Set<MealyNode>> adjacent) {
		int maxDegree = 0;
		MealyNode maxV = null;
		final Set<MealyNode> clique = new LinkedHashSet<>();
		
		for (Map.Entry<MealyNode, Set<MealyNode>> pair : adjacent.entrySet()) {
			MealyNode candidate = pair.getKey();
			int candidateDegree = pair.getValue().size();
			if (candidateDegree > maxDegree) {
				maxDegree = candidateDegree;
				maxV = candidate;
			}
		}
		
		MealyNode last = maxV;
		if (last != null) {
			clique.add(last);
			MealyNode anotherOne = neighborWithHighestDegree(clique, last, adjacent);
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

	private static MealyNode neighborWithHighestDegree(Set<MealyNode> cur, MealyNode v, Map<MealyNode, Set<MealyNode>> adjacent) {
		int maxDegree = 0;
		MealyNode maxNeighbour = null;
		for (MealyNode u : adjacent.get(v)) {
			boolean uInClique = true;
			for (MealyNode w : cur) {
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
