package structures.mealy;

/**
 * (c) Igor Buzhinsky
 */

import bool.MyBooleanExpression;
import org.apache.commons.lang3.tuple.Pair;
import scenario.StringActions;
import structures.moore.MooreNode;
import structures.moore.NondetMooreAutomaton;

import java.util.*;

public class APTA {
    private final Map<Integer, Map<String, Integer>> edges = new LinkedHashMap<>();
    private final Map<Integer, Map<String, Integer>> revEdges = new LinkedHashMap<>();
    private final Map<Integer, NodeType> nodeTypes = new LinkedHashMap<>();
    private final Map<Integer, NodeColor> nodeColors = new LinkedHashMap<>();
    private final List<Operation> operations = new ArrayList<>();

    private void revert() {
        for (int i = operations.size() - 1; i >= 0; i--) {
            operations.get(i).revert();
        }
        operations.clear();
    }

    private void apply(List<Operation> op) {
        op.forEach(Operation::apply);
    }

    private interface Operation {
        void apply();
        void revert();
    }

    private class NodeTypesPut implements Operation {
        final int index;
        final NodeType type;
        final NodeType prevType;

        public NodeTypesPut(int index, NodeType type) {
            this.index = index;
            this.type = type;
            prevType = nodeTypes.get(index);
            apply();
        }

        @Override
        public void apply() {
            nodeTypes.put(index, type);
        }

        @Override
        public void revert() {
            if (prevType == null) {
                nodeTypes.remove(index);
            } else {
                nodeTypes.put(index, prevType);
            }
        }

        @Override
        public String toString() {
            return "NodeTypesPut{" + "index=" + index + ", type=" + type + ", prevType=" + prevType + '}';
        }
    }

    private class NodePut implements Operation {
        final int from;
        final String event;
        final int to;
        final boolean present;
        final Integer prevNode;
        final boolean presentRev;
        final Integer prevRevNode;

        public NodePut(int from, String event, int to) {
            this.from = from;
            this.event = event;
            this.to = to;

            present = edges.get(from).containsKey(event);
            prevNode = edges.get(from).get(event);

            presentRev = revEdges.get(to).containsKey(event);
            prevRevNode = revEdges.get(to).get(event);

            apply();
        }

        @Override
        public void apply() {
            edges.get(from).put(event, to);
            revEdges.get(to).put(event, from);
        }

        @Override
        public void revert() {
            if (!present) {
                edges.get(from).remove(event);
            } else {
                edges.get(from).put(event, prevNode);
            }
            if (!presentRev) {
                revEdges.get(to).remove(event);
            } else {
                revEdges.get(to).put(event, prevRevNode);
            }
        }

        @Override
        public String toString() {
            return "NodePut{" + "from=" + from + ", event='" + event + '\'' + ", to=" + to +
                    ", prevNode=" + prevNode + '}';
        }
    }

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
        for (int node : edges.keySet()) {
            nodeColors.put(node, NodeColor.WHITE);
        }
        nodeColors.put(0, NodeColor.RED);
        for (Map.Entry<String, Integer> edges : this.edges.get(0).entrySet()) {
            int dst = edges.getValue();
            nodeColors.put(dst, NodeColor.BLUE);
        }
    }

    public APTA() {
        edges.put(0, new TreeMap<>());
        revEdges.put(0, new TreeMap<>());
        nodeTypes.put(0, NodeType.POSITIVE);
    }
    
    private Set<Integer> bfs() {
        final Deque<Integer> queue = new ArrayDeque<>();
        final Set<Integer> visited = new LinkedHashSet<>();
        queue.add(0);
        while (!queue.isEmpty()) {
            int node = queue.removeFirst();
            visited.add(node);
            edges.get(node).values().stream().filter(child -> !visited.contains(child)).forEach(queue::addLast);
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
            edges.get(node).values().stream().filter(child -> !visited.contains(child)).forEach(queue::addLast);
        }
        return true;
    }

    private int aptaScore(int r, int b) {
        redirect(r, b);
        final int score = merge(r, b, 0);
        revert();
        return score;
    }

    private Pair<List<Operation>, Integer> aptaAndScore(int r, int b) {
        redirect(r, b);
        final int score = merge(r, b, 0);
        final List<Operation> opCopy = operations.isEmpty() ? null : new ArrayList<>(operations);
        revert();
        return Pair.of(opCopy, score);
    }
    
    public void updateColors() {
        final Set<Integer> bfsNodes = bfs();

        while (true) {
            boolean recoloredBlueRed = false;
            l1: for (int b : bfsNodes) {
                if (nodeColors.get(b) == NodeColor.BLUE) {
                    for (int r : nodeColors.keySet()) {
                        if (nodeColors.get(r) == NodeColor.RED) {
                            final int score = aptaScore(r, b);

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
                if (nodeColors.get(w) == NodeColor.BLUE || nodeColors.get(w) == NodeColor.WHITE && isolated(w)) {
                    isolated.add(w);
                }
            }
            
            boolean recoloredWriteBlue = false;
            l2: for (int w : isolated) {
                if (nodeColors.get(w) == NodeColor.WHITE) {
                    for (int parent : revEdges.get(w).values()) {
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
    
    public boolean bestMerge() {
        int bestScore = Integer.MIN_VALUE;
        List<Operation> bestMerge = null;
        
        for (int r : nodeColors.keySet()) {
            if (nodeColors.get(r) == NodeColor.RED) {
                for (int b : nodeColors.keySet()) {
                    if (nodeColors.get(b) == NodeColor.BLUE) {
                        final Pair<List<Operation>, Integer> pair = aptaAndScore(r, b);
                        final int score = pair.getRight();
                        if (score > bestScore) {
                            bestScore = score;
                            bestMerge = pair.getLeft();
                        }
                    }
                }
            }
        }
        if (bestMerge != null) {
            apply(bestMerge);
            removeUnreachableNodes();
            return true;
        }
        return false;
    }
    
    private void removeUnreachableNodes() {
        final Set<Integer> nodesToRemove = new HashSet<>(edges.keySet());
        nodesToRemove.removeAll(bfs());
        nodesToRemove.forEach(this::removeNode);
    }
    
    private void removeNode(int node) {
        edges.remove(node);
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
                operations.add(new NodeTypesPut(r, bLabel));
            }
        }
        
        for (Map.Entry<String, Integer> blueEdge : edges.get(b).entrySet()) {
            final int blueChild = blueEdge.getValue();
            final String event = blueEdge.getKey();
            final Integer redChild = edges.get(r).get(event);
            if (redChild != null) {
                score = merge(redChild, blueChild, score);
                if (score == Integer.MIN_VALUE) {
                    return score;
                }
            } else {
                operations.add(new NodePut(r, event, blueChild));
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
        Integer dst = edges.get(src).get(event);
        if (dst == null) {
            dst = edges.size();
            edges.put(dst, new TreeMap<>());
            revEdges.put(dst, new TreeMap<>());
            new NodePut(src, event, dst);
        }
        return dst;
    }

    private APTA removeNegativeNodes() {
        final APTA a = copy();
        final List<Integer> negative = new ArrayList<>();
        for (Map.Entry<Integer, NodeType> entry : nodeTypes.entrySet()) {
            if (entry.getValue() == NodeType.NEGATIVE) {
                negative.add(entry.getKey());
            }
        }
        for (int node : negative) {
            a.removeNode(node);
            for (Map<String, Integer> transitionMap : a.edges.values()) {
                final List<String> events = new ArrayList<>(transitionMap.keySet());
                events.stream().filter(event -> transitionMap.get(event) == node).forEach(transitionMap::remove);
            }
        }
        a.removeUnreachableNodes();
        return a;
    }
    
    private APTA removeNodeNumGaps() {
        final APTA a = new APTA();
        final int size = edges.keySet().stream().mapToInt(x -> x).max().getAsInt() + 1;
        final int[] mapping = new int[size];
        
        int j = 0;
        for (int node : edges.keySet()) {
            mapping[node] = j;
            a.edges.put(j, new TreeMap<>());
            j++;
        }
        for (Map.Entry<Integer, Map<String, Integer>> node : edges.entrySet()) {
            for (Map.Entry<String, Integer> t : node.getValue().entrySet()) {
                a.edges.get(mapping[node.getKey()]).put(t.getKey(), mapping[t.getValue()]);
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
    
    public MealyAutomaton toAutomaton() {
        final APTA a = removeNegativeNodes().removeNodeNumGaps();
        final MealyAutomaton auto = new MealyAutomaton(a.edges.size());
        for (Map.Entry<Integer, Map<String, Integer>> node : a.edges.entrySet()) {
            final int src = node.getKey();
            for (Map.Entry<String, Integer> t : node.getValue().entrySet()) {
                final int dst = t.getValue();
                final MealyNode from = auto.state(src);
                final MealyNode to = auto.state(dst);
                auto.addTransition(from, new MealyTransition(from, to, t.getKey(),
                        MyBooleanExpression.getTautology(), new StringActions("")));
            }
        }
        
        return auto;
    }

    /*
     * For state merging based nondeterministic automaton synthesis.
     */
    public NondetMooreAutomaton toNondetMooreAutomaton(List<String> actions) {
        final MealyAutomaton mealy = toAutomaton();
        final List<MooreNode> mooreStates = new ArrayList<>();
        final List<Boolean> mooreIsInitial = new ArrayList<>();
        int num = 0;
        final Map<MealyTransition, Integer> transToNum = new HashMap<>();
        // states
        for (MealyNode node : mealy.states()) {
            for (MealyTransition t : node.transitions()) {
                final String label = t.event();
                final String event = label.substring(0, label.length() - actions.size());
                final String action = label.substring(label.length() - actions.size(), label.length());
                final List<String> separatedActions = new ArrayList<>();
                for (int i = 0; i < actions.size(); i++) {
                    if (action.charAt(i) == '1') {
                        separatedActions.add(actions.get(i));
                    }
                }
                final StringActions stringActions = new StringActions(String.join(",", separatedActions));
                final MooreNode mooreNode = new MooreNode(num, stringActions);
                mooreStates.add(mooreNode);
                mooreIsInitial.add(event.isEmpty());
                transToNum.put(t, num);
                num++;
            }
        }
        // transitions
        for (MealyNode node : mealy.states()) {
            for (MealyTransition t1 : node.transitions()) {
                for (MealyTransition t2 : t1.dst().transitions()) {
                    final String label = t2.event();
                    final String event = label.substring(0, label.length() - actions.size());
                    mooreStates.get(transToNum.get(t1)).addTransition(event,
                            mooreStates.get(transToNum.get(t2)));
                }
            }
        }
        return new NondetMooreAutomaton(mooreStates, mooreIsInitial);
    }
    
    // additionally makes parent(b) point to r
    private APTA copy() {
        final APTA a = new APTA();
        for (Map.Entry<Integer, Map<String, Integer>> node : edges.entrySet()) {
            a.edges.put(node.getKey(), new TreeMap<>(node.getValue()));
        }
        for (Map.Entry<Integer, Map<String, Integer>> node : revEdges.entrySet()) {
            a.revEdges.put(node.getKey(), new TreeMap<>(node.getValue()));
        }
        a.nodeTypes.putAll(nodeTypes);
        a.nodeColors.putAll(nodeColors);
        return a;
    }

    // makes parent(b) point to r
    private void redirect(int r, int b) {
        for (Map.Entry<String, Integer> entry : revEdges.get(b).entrySet()) {
            operations.add(new NodePut(entry.getValue(), entry.getKey(), r));
        }
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("digraph APTA { node [shape=circle, width=0.7, fixedsize=true];\n");

        for (int node : edges.keySet()) {
            sb.append(node + "" + nodeColors.get(node) + nodeTypes.get(node) + ";");
        }
        sb.append("\n");
        for (Map.Entry<Integer, Map<String, Integer>> node : edges.entrySet()) {
            for (Map.Entry<String, Integer> t : node.getValue().entrySet()) {
                sb.append(node.getKey() + "->" + t.getValue() +
                        "[label=\"" + t.getKey() + "\"];");
            }
        }
        sb.append(" }\n");
        return sb.toString();
    }
}
