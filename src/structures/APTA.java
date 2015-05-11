package structures;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import actions.StringActions;
import bool.MyBooleanExpression;

public class APTA {
    private final Map<Integer, Map<String, Integer>> nodes = new LinkedHashMap<>();
    private final Map<Integer, NodeType> nodeTypes = new LinkedHashMap<>();
    private final Map<Integer, NodeColor> nodeColors = new LinkedHashMap<>();
    
    private enum NodeType {
    	POSITIVE("+"), NEGATIVE("-"), UNKNOWN("?");

    	private final String symbol;
    	
    	NodeType(String symbol) {
    		this.symbol = symbol;
    	}
    	
    	@Override
    	public String toString() {
    		return symbol;
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
    		return "[color = " + dotColor + "]";
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
    
    private List<Integer> bfs() {
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
    	return new ArrayList<>(visited);
    }
    
    private boolean canMerge(int n1, int n2) {
    	final NodeType l1 = nodeTypes.get(n1);
    	final NodeType l2 = nodeTypes.get(n2);
    	return l1 == NodeType.UNKNOWN || l2 == NodeType.UNKNOWN || l1 == l2;
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
    
    public Optional<APTA> bestMerge() {
    	System.out.println("BEFORE: " + this);
    	
    	int bestScore = Integer.MIN_VALUE;
    	APTA bestMerge = null;
    	
    	// while (there exists a blue node that cannot be merged with any red node)
    	// 	 promote the shallowest such blue node to red
    	final List<Integer> bfsNodes = bfs();
    	while (true) {
    		boolean recoloredBlueRed = false;
	    	l1: for (int b : bfsNodes) {
	    		if (nodeColors.get(b) == NodeColor.BLUE) {
	    			for (int r : nodeColors.keySet()) {
	    	    		if (nodeColors.get(r) == NodeColor.RED) {
	    	    			if (canMerge(b, r)) {
	    	    				// there exists a red node such that the blue node can be merged with it
	    	    				continue l1;
	    	    			}
	    	    		}
	    			}
	    			// there is no such red node, promote b to RED
	    			nodeColors.put(b, NodeColor.RED);
	    			recoloredBlueRed = true;
	    			//System.out.println(b + " -> RED");
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
	    			for (int wOther : isolated) {
	        			if (nodes.get(wOther).values().contains(w)) {
	        				continue l2;
	        			}
	        		}
	    			// w is the root of an isolated tree, promote w to BLUE
	    			nodeColors.put(w, NodeColor.BLUE);
	    			recoloredWriteBlue = true;
	    			//System.out.println(w + " -> BLUE");
    			}
    		}
    		if (recoloredBlueRed || recoloredWriteBlue) {
    			continue;
    		} else {
    			break;
    		}
    	}

    	System.out.println("AFTER: " + this);
    	
    	for (int r : nodeColors.keySet()) {
    		if (nodeColors.get(r) == NodeColor.RED) {
    			for (int b : nodeColors.keySet()) {
	    			if (nodeColors.get(b) == NodeColor.BLUE) {
	    				final APTA copy = copy(r, b);
	    				final int score = copy.mergeBlueFringe(r, b, 0);
	    				
	    				if (score >= bestScore) {
	    					bestScore = score;
	    					bestMerge = copy;
	    					copy.removeNode(b);
	    				}
	    			}
	    		}
    		}
    	}
		return Optional.ofNullable(bestMerge);
	}
    
    private void removeNode(int node) {
    	nodes.remove(node);
    	nodeColors.remove(node);
    	nodeTypes.remove(node);
    }
    
    public int mergeBlueFringe(int r, int b, int score) {
    	final NodeType rLabel = nodeTypes.get(r);
    	final NodeType bLabel = nodeTypes.get(b);
    	
    	if (bLabel != NodeType.UNKNOWN) {
    		if (rLabel != NodeType.UNKNOWN) {
    			if (rLabel == bLabel) {
        			score++;
        		} else {
        			score = Integer.MIN_VALUE;
        		}
        	} else {
        		nodeTypes.put(r, bLabel);
        	}
    	}
    	
    	for (Map.Entry<String, Integer> redEdge : nodes.get(r).entrySet()) {
    		final int redChild = redEdge.getValue();
    		final String event = redEdge.getKey();
    		final Integer blueChild = nodes.get(b).get(event);
    		if (blueChild != null) {
				score = mergeBlueFringe(redChild, blueChild, score);
				redEdge.setValue(blueChild);
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

    public int size() {
        return nodes.size();
    }
    
    public Automaton toAutomaton() {
		final Automaton a = new Automaton(size());
		for (Map.Entry<Integer, Map<String, Integer>> node : nodes.entrySet()) {
			int src = node.getKey();
			NodeType srcType = nodeTypes.get(node.getKey());
			if (srcType == NodeType.NEGATIVE) {
				continue;
			}
			for (Map.Entry<String, Integer> t : node.getValue().entrySet()) {
				int dst = t.getValue();
				NodeType dstType = nodeTypes.get(dst);
				if (dstType == NodeType.NEGATIVE) {
					continue;
				}
				final Node from = a.getState(src);
				final Node to = a.getState(dst);
				a.addTransition(from, new Transition(from, to, t.getKey(),
						MyBooleanExpression.getTautology(), new StringActions("")));
			}
		}
		
		return a;
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
        sb.append("digraph APTA {\n    node [shape = circle];\n");

        for (Map.Entry<Integer, NodeColor> entry : nodeColors.entrySet()) {
        	sb.append("    " + entry.getKey() + " " + entry.getValue() + ";\n");
        }
        for (Map.Entry<Integer, Map<String, Integer>> node : nodes.entrySet()) {
			for (Map.Entry<String, Integer> t : node.getValue().entrySet()) {
				int dst = t.getValue();
				NodeType dstType = nodeTypes.get(dst);
				sb.append("    " + node.getKey() + " -> " + dst +
						" [label = \"" + dstType + t.getKey() + "\"];\n");
			}
        }
        sb.append("}\n");
        return sb.toString();
    }
}
