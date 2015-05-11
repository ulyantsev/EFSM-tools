package structures;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import actions.StringActions;
import bool.MyBooleanExpression;

public class APTA {
    private final Map<Integer, Set<Pair<String, Integer>>> nodes = new LinkedHashMap<>();
    private final Map<Integer, NodeType> nodeTypes = new LinkedHashMap<>();
    
    private enum NodeType {
    	POSITIVE, NEGATIVE, UNKNOWN
    }

    public APTA() {
    	nodes.put(0, new LinkedHashSet<>());
    	nodeTypes.put(0, NodeType.POSITIVE);
    }
    
    private boolean canMerge(int n1, int n2) {
    	NodeType t1 = nodeTypes.get(n1);
		NodeType t2 = nodeTypes.get(n2);
		return t1 == t2 || t1 == NodeType.UNKNOWN || t2 == NodeType.UNKNOWN;
    }
    
    public List<APTA> possibleMerges() {
    	final List<APTA> result = new ArrayList<>();
    	for (int n1 = 0; n1 < nodes.size(); n1++) {
    		for (int n2 = n1 + 1; n2 < nodes.size(); n2++) {
    			if (canMerge(n1, n2)) {
    				final Optional<APTA> optNewA = merge(n1, n2).get().determine();
    				if (optNewA.isPresent()) {
    					result.add(optNewA.get());
    				}
    			}
    		}
    	}
		return result;
	}
    
    public List<APTA> possibleMerge() {
    	final List<APTA> result = new ArrayList<>();
    	for (int n1 = 0; n1 < nodes.size(); n1++) {
    		for (int n2 = n1 + 1; n2 < nodes.size(); n2++) {
    			if (canMerge(n1, n2)) {
    				final Optional<APTA> optNewA = merge(n1, n2).get().determine();
    				if (optNewA.isPresent()) {
    					result.add(optNewA.get());
    					return result;
    				}
    			}
    		}
    	}
		return result;
	}
	
    public Optional<APTA> determine() {
    	APTA a = this;
    	
    	l: while (true) {
    		for (int i = 0; i < a.nodes.size(); i++) {
    			Map<String, Integer> deterministicTransitions = new LinkedHashMap<>();
    			for (Pair<String, Integer> t : a.nodes.get(i)) {
    				String event = t.getLeft();
    				Integer dst1 = deterministicTransitions.get(event);
    				int dst2 = t.getRight();
    				if (dst1 == null) {
    					deterministicTransitions.put(event, dst2);
    				} else if (dst1 != dst2) {
    					Optional<APTA> merged = a.merge(dst1, dst2);
    					if (merged.isPresent()) {
    						a = merged.get();
    						continue l;
    					} else {
    						return Optional.empty();
    					}
    				}
    			}
    		}
    		break;
    	}
    	
    	return Optional.of(a);
    }
    
	public Optional<APTA> merge(int n1, int n2) {
		if (n1 == n2) {
			throw new AssertionError();
		}
		if (!canMerge(n1, n2)) {
			return Optional.empty();
		}
		if (n1 > n2) {
			int t = n1;
			n1 = n2;
			n2 = t;
		}
		// n1 < n2
		
		APTA a = new APTA();
		
		// remove n2
		int[] nodeMap = new int[size()];
		for (int i = 0; i < n2; i++) {
			nodeMap[i] = i;
		}
		nodeMap[n2] = n1;
		for (int i = n2 + 1; i < size(); i++) {
			nodeMap[i] = i - 1;
		}
		
		NodeType mergedType
				= nodeTypes.get(n1) == NodeType.UNKNOWN ? nodeTypes.get(n2)
				: nodeTypes.get(n1);
				
		for (Map.Entry<Integer, Set<Pair<String, Integer>>> node : nodes.entrySet()) {
			int src = node.getKey();
			int mappedSrc = nodeMap[src];
			a.nodeTypes.put(mappedSrc, nodeTypes.get(src));
			
			Set<Pair<String, Integer>> newSet = a.nodes.get(mappedSrc);
			if (newSet == null) {
				newSet = new LinkedHashSet<>();
				a.nodes.put(mappedSrc, newSet);
			}
			
			for (Pair<String, Integer> t : node.getValue()) {
				int dst = t.getValue();
				int mappedDst = nodeMap[dst];
				newSet.add(Pair.of(t.getKey(), mappedDst));
			}
		}
		a.nodeTypes.put(n1, mergedType);

		return Optional.of(a);
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
    	int dst = -1;
    	
    	for (Pair<String, Integer> t : nodes.get(src)) {
    		if (t.getLeft().equals(event)) {
    			dst = t.getRight();
    		}
    	}
    	
        if (dst == -1) {
        	dst = nodes.size();
        	nodes.put(dst, new LinkedHashSet<>());
        	nodes.get(src).add(Pair.of(event, dst));
        }
        return dst;
    }

    public int size() {
        return nodes.size();
    }
    
    public Automaton toAutomaton() {
		final Automaton a = new Automaton(size());
		for (Map.Entry<Integer, Set<Pair<String, Integer>>> node : nodes.entrySet()) {
			int src = node.getKey();
			NodeType srcType = nodeTypes.get(node.getKey());
			if (srcType == NodeType.NEGATIVE) {
				continue;
			}
			for (Pair<String, Integer> t : node.getValue()) {
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
    
    /*public APTA copy() {
		final APTA a = new APTA();
		for (Map.Entry<Integer, Set<Pair<String, Integer>>> node : nodes.entrySet()) {
			int state = node.getKey();
			a.nodes.put(state, new LinkedHashSet<>());
			for (Pair<String, Integer> t : node.getValue()) {
				a.nodes.get(state).add(t);
			}
		}
		a.negativeNodes.addAll(negativeNodes);
		return a;
    }*/
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph APTA {\n    node [shape = circle];\n");

        for (Map.Entry<Integer, Set<Pair<String, Integer>>> node : nodes.entrySet()) {
			for (Pair<String, Integer> t : node.getValue()) {
				int dst = t.getValue();
				sb.append("    " + node.getKey() + " -> " + dst);
				NodeType dstType = nodeTypes.get(dst);
                String symbol = dstType == NodeType.POSITIVE ? "+"
                		: dstType == NodeType.NEGATIVE ? "-" : "?";
                sb.append(" [label = \"" + symbol + t.getKey() + "\"];\n");
			}
        }
        sb.append("}\n");
        return sb.toString();
    }
}
