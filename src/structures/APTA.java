package structures;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import scenario.StringActions;
import bool.MyBooleanExpression;

public class APTA {
    private final Map<Integer, Map<String, Integer>> nodes = new LinkedHashMap<>();
    private final Map<Integer, NodeType> nodeTypes = new LinkedHashMap<>();
    private final Map<Integer, NodeColor> nodeColors = new LinkedHashMap<>();
    private final Map<Pair<Integer, Integer>, Pair<APTA, Integer>> cachedMerges = new HashMap<>();
    
    private enum NodeType {
    	POSITIVE("solid"), NEGATIVE("dashed"), UNKNOWN("dotted");

    	private final String dotStyle;
    	
    	NodeType(String symbol) {
    		this.dotStyle = symbol;
    	}
    	
    	@Override
    	public String toString() {
    		return "[style=" + dotStyle + "]";
    	}
    }
    
    private enum NodeColor {
    	RED("red"), BLUE("blue"), WHITE("black");
    	
    	private final String dotColor;
    	
    	NodeColor(String dotColor) {
    		this.dotColor = dotColor;
    	}
    	
    	@Override
    	public String toString() {
    		return "[color=" + dotColor + "]";
    	}
    }
    
    public void resetColors() {
    	// the root is RED, its children are BLUE, other nodes are WHITE
    	for (int node : nodes.keySet()) {
    		nodeColors.put(node, NodeColor.WHITE);
    	}
    	nodeColors.put(0, NodeColor.RED);
    	for (Map.Entry<String, Integer> edges : nodes.get(0).entrySet()) {
    		int dst = edges.getValue();
    		nodeColors.put(dst, NodeColor.BLUE);
    	}
    }

    public APTA() {
    	nodes.put(0, new LinkedHashMap<>());
    	nodeTypes.put(0, NodeType.POSITIVE);
    }
    
    private Set<Integer> bfs() {
    	final Deque<Integer> queue = new ArrayDeque<>();
    	final Set<Integer> visited = new LinkedHashSet<>();
    	queue.add(0);
    	while (!queue.isEmpty()) {
    		int node = queue.removeFirst();
    		visited.add(node);
    		for (Integer child : nodes.get(node).values()) {
    			if (!visited.contains(child)) {
    				queue.addLast(child);
    			}
    		}
    	}
    	return visited;
    }
    
    private boolean isolated(int n) {
    	final Deque<Integer> queue = new ArrayDeque<>();
    	final Set<Integer> visited = new LinkedHashSet<>();
    	queue.add(n);
    	while (!queue.isEmpty()) {
    		int node = queue.removeFirst();
    		if (nodeColors.get(node) != NodeColor.WHITE) {
    			return false;
    		}
    		visited.add(node);
    		for (Integer child : nodes.get(node).values()) {
    			if (!visited.contains(child)) {
    				queue.addLast(child);
    			}
    		}
    	}
    	return true;
    }
    
    private Pair<APTA, Integer> aptaAndScore(int r, int b) {
		final Pair<APTA, Integer> cached = cachedMerges.get(Pair.of(r, b));
		if (cached != null) {
			return cached;
		}
		final APTA copy = copy(r, b);
		final int score = copy.merge(r, b, 0);
		cachedMerges.put(Pair.of(r, b), Pair.of(copy, score));
		return Pair.of(copy, score);
    }
    
    public void updateColors() {
    	final Set<Integer> bfsNodes = bfs();
    	
    	final Map<Integer, Set<Integer>> backEdges = new LinkedHashMap<>();
    	for (int node : nodes.keySet()) {
    		backEdges.put(node, new HashSet<>());
    	}
    	for (Map.Entry<Integer, Map<String, Integer>> node : nodes.entrySet()) {
			final int src = node.getKey();
			for (Map.Entry<String, Integer> t : node.getValue().entrySet()) {
				final int dst = t.getValue();
				backEdges.get(dst).add(src);
			}
		}
    	
    	while (true) {
    		boolean recoloredBlueRed = false;
	    	l1: for (int b : bfsNodes) {
	    		if (nodeColors.get(b) == NodeColor.BLUE) {
	    			for (int r : nodeColors.keySet()) {
	    	    		if (nodeColors.get(r) == NodeColor.RED) {
	    	    			final Pair<APTA, Integer> pair = aptaAndScore(r, b);
		    				final int score = pair.getRight();
    	    				if (score != Integer.MIN_VALUE) {
	    	    				// there exists a red node such that the blue node
    	    					// can be merged with it
	    	    				continue l1;		
    	    				}
	    	    		}
	    			}
	    			// there is no such red node, promote b to RED
	    			nodeColors.put(b, NodeColor.RED);
	    			recoloredBlueRed = true;
	    		}
	    	}
    	
    		final Set<Integer> isolated = new HashSet<>();
    		for (int w : bfsNodes) {
    			if (nodeColors.get(w) == NodeColor.BLUE
    					|| nodeColors.get(w) == NodeColor.WHITE && isolated(w)) {
    				isolated.add(w);
    			}
    		}
    		
    		boolean recoloredWriteBlue = false;
    		l2: for (int w : isolated) {
    			if (nodeColors.get(w) == NodeColor.WHITE) {
    				for (int parent : backEdges.get(w)) {
    					if (isolated.contains(parent)) {
    						continue l2;
    					}
    				}
    				
	    			// w is the root of an isolated tree, promote w to BLUE
	    			nodeColors.put(w, NodeColor.BLUE);
	    			recoloredWriteBlue = true;
    			}
    		}
    		if (recoloredBlueRed || recoloredWriteBlue) {
    			continue;
    		} else {
    			break;
    		}
    	}
    }
    
    public Optional<APTA> bestMerge() {
    	int bestScore = Integer.MIN_VALUE;
    	APTA bestMerge = null;
    	
    	for (int r : nodeColors.keySet()) {
    		if (nodeColors.get(r) == NodeColor.RED) {
    			for (int b : nodeColors.keySet()) {
	    			if (nodeColors.get(b) == NodeColor.BLUE) {
	    				final Pair<APTA, Integer> pair = aptaAndScore(r, b);
	    				final int score = pair.getRight();
	    				if (score > bestScore) {
		    				final APTA copy = pair.getLeft();
	    					bestScore = score;
	    					bestMerge = copy;
	    				}
	    			}
	    		}
    		}
    	}
    	if (bestMerge != null) {
    		bestMerge.removeUnreachableNodes();
    	}
    	cachedMerges.clear();
		return Optional.ofNullable(bestMerge);
	}
    
    private void removeUnreachableNodes() {
    	final Set<Integer> nodesToRemove = new HashSet<>(nodes.keySet());
		nodesToRemove.removeAll(bfs());
		for (int n : nodesToRemove) {
			removeNode(n);
		}
    }
    
    private void removeNode(int node) {
    	nodes.remove(node);
    	nodeColors.remove(node);
    	nodeTypes.remove(node);
    }
    
    public int merge(int r, int b, int score) {
    	final NodeType rLabel = nodeTypes.get(r);
    	final NodeType bLabel = nodeTypes.get(b);
    	
    	if (bLabel != NodeType.UNKNOWN) {
    		if (rLabel != NodeType.UNKNOWN) {
    			if (rLabel == bLabel) {
        			score++;
        		} else {
        			return Integer.MIN_VALUE;
        		}
        	} else {
        		nodeTypes.put(r, bLabel);
        	}
    	}
    	
    	for (Map.Entry<String, Integer> blueEdge : nodes.get(b).entrySet()) {
    		final int blueChild = blueEdge.getValue();
    		final String event = blueEdge.getKey();
    		final Integer redChild = nodes.get(r).get(event);
    		if (redChild != null) {
    			score = merge(redChild, blueChild, score);
				if (score == Integer.MIN_VALUE) {
					return score;
				}
    		} else {
    			nodes.get(r).put(event, blueChild);
    		}
    	}
    	
    	return score;
    }
	
    public void addScenario(List<String> scenario, boolean positive) {
        int node = 0;
        for (String event : scenario) {
            node = addTransition(node, event);
            if (positive) {
            	nodeTypes.put(node, NodeType.POSITIVE);
            } else if (!nodeTypes.containsKey(node)) {
            	nodeTypes.put(node, NodeType.UNKNOWN);
            }
        }
        if (!positive) {
        	nodeTypes.put(node, NodeType.NEGATIVE);
        }
    }

    private int addTransition(int src, String event) {
    	Integer dst = nodes.get(src).get(event);
        if (dst == null) {
        	dst = nodes.size();
        	nodes.put(dst, new LinkedHashMap<>());
        	nodes.get(src).put(event, dst);
        }
        return dst;
    }

    private APTA removeNegativeNodes() {
    	final APTA a = copy(0, 0);
    	final List<Integer> negative = new ArrayList<>();
    	for (Map.Entry<Integer, NodeType> entry : nodeTypes.entrySet()) {
    		if (entry.getValue() == NodeType.NEGATIVE) {
    			negative.add(entry.getKey());
    		}
    	}
    	for (int node : negative) {
    		a.removeNode(node);
    		for (Map<String, Integer> transitionMap : a.nodes.values()) {
    			final List<String> events = new ArrayList<>(transitionMap.keySet());
    			for (String event : events) {
    				if (transitionMap.get(event) == node) {
    					transitionMap.remove(event);
    				}
    			}
    		}
    	}
    	a.removeUnreachableNodes();
    	return a;
    }
    
    private APTA removeNodeNumGaps() {
    	final APTA a = new APTA();
    	final int size = nodes.keySet().stream().mapToInt(x -> x).max().getAsInt() + 1;
    	final int[] mapping = new int[size];
    	
    	int j = 0;
    	for (int node : nodes.keySet()) {
    		mapping[node] = j;
    		a.nodes.put(j, new LinkedHashMap<>());
    		j++;
    	}
    	for (Map.Entry<Integer, Map<String, Integer>> node : nodes.entrySet()) {
			for (Map.Entry<String, Integer> t : node.getValue().entrySet()) {
				a.nodes.get(mapping[node.getKey()]).put(t.getKey(), mapping[t.getValue()]);
			}
		}
    	
    	for (Map.Entry<Integer, NodeType> entry : nodeTypes.entrySet()) {
    		a.nodeTypes.put(mapping[entry.getKey()], entry.getValue());
    	}
    	for (Map.Entry<Integer, NodeColor> entry : nodeColors.entrySet()) {
    		a.nodeColors.put(mapping[entry.getKey()], entry.getValue());
    	}

    	return a;
    }
    
    public Automaton toAutomaton() {
    	final APTA a = removeNegativeNodes().removeNodeNumGaps();
		final Automaton auto = new Automaton(a.nodes.size());
		for (Map.Entry<Integer, Map<String, Integer>> node : a.nodes.entrySet()) {
			final int src = node.getKey();
			for (Map.Entry<String, Integer> t : node.getValue().entrySet()) {
				final int dst = t.getValue();
				final Node from = auto.getState(src);
				final Node to = auto.getState(dst);
				auto.addTransition(from, new Transition(from, to, t.getKey(),
						MyBooleanExpression.getTautology(), new StringActions("")));
			}
		}
		
		return auto;
    }
    
    // additionally makes parent(b) point to r
    private APTA copy(int r, int b) {
		final APTA a = new APTA();
		for (Map.Entry<Integer, Map<String, Integer>> node : nodes.entrySet()) {
			final int state = node.getKey();
			a.nodes.put(state, new LinkedHashMap<>());
			for (Map.Entry<String, Integer> t : node.getValue().entrySet()) {
				int dst = t.getValue();
				a.nodes.get(state).put(t.getKey(), dst == b ? r : dst);
			}
		}
		a.nodeTypes.putAll(nodeTypes);
		a.nodeColors.putAll(nodeColors);
		return a;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph APTA { node [shape=circle, width=0.7, fixedsize=true];\n");

        for (int node : nodes.keySet()) {
        	sb.append(node + "" + nodeColors.get(node) + nodeTypes.get(node) + ";");
        }
        sb.append("\n");
        for (Map.Entry<Integer, Map<String, Integer>> node : nodes.entrySet()) {
			for (Map.Entry<String, Integer> t : node.getValue().entrySet()) {
				sb.append(node.getKey() + "->" + t.getValue() +
						"[label=\"" + t.getKey() + "\"];");
			}
        }
        sb.append(" }\n");
        return sb.toString();
    }
}
