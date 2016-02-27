/**
 * IntersectionAutomata.java, 12.04.2008
 */
package verification.verifier;

import java.util.HashMap;
import java.util.Map;

import verification.ltl.buchi.BuchiAutomaton;
import verification.ltl.buchi.BuchiNode;
import verification.ltl.grammar.PredicateFactory;
import verification.statemachine.SimpleState;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class IntersectionAutomata {
    private final PredicateFactory predicates;
    private final BuchiAutomaton buchiAutomata;
    private final Map<String, IntersectionNode> nodeMap = new HashMap<>();

    public IntersectionAutomata(PredicateFactory predicates, BuchiAutomaton buchi) {
        this.predicates = predicates;
        buchiAutomata = buchi;
    }

    public BuchiAutomaton getBuchiAutomata() {
        return buchiAutomata;
    }

    public IntersectionNode getNode(SimpleState state, BuchiNode node) {
        String key = getUniqueKey(state, node);

        IntersectionNode res = nodeMap.get(key);
        if (res == null) {
            res = new IntersectionNode(this, state, node);
            nodeMap.put(key, res);
        }
        return res;
    }

    public PredicateFactory getPredicates() {
        return predicates;
    }

    protected String getUniqueKey(SimpleState state, BuchiNode node) {
        return state.getUniqueName() + "_" + node.getID();
    }
}
