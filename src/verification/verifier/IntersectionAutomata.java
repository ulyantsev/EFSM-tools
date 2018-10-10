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
class IntersectionAutomata {
    private final PredicateFactory predicates;
    private final BuchiAutomaton buchiAutomata;
    private final Map<String, IntersectionNode> nodeMap = new HashMap<>();

    IntersectionAutomata(PredicateFactory predicates, BuchiAutomaton buchi) {
        this.predicates = predicates;
        buchiAutomata = buchi;
    }

    BuchiAutomaton getBuchiAutomata() {
        return buchiAutomata;
    }

    IntersectionNode getNode(SimpleState state, BuchiNode node) {
        String key = getUniqueKey(state, node);

        IntersectionNode res = nodeMap.get(key);
        if (res == null) {
            res = new IntersectionNode(this, state, node);
            nodeMap.put(key, res);
        }
        return res;
    }

    PredicateFactory getPredicates() {
        return predicates;
    }

    private String getUniqueKey(SimpleState state, BuchiNode node) {
        return state.getUniqueName() + "_" + node.getID();
    }
}
