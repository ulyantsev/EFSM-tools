/**
 * IntersectionAutomata.java, 12.04.2008
 */
package qbf.egorov.verifier;

import qbf.egorov.ltl.buchi.BuchiAutomaton;
import qbf.egorov.ltl.buchi.BuchiNode;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.statemachine.SimpleState;

import java.util.*;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class IntersectionAutomata {
    private final IPredicateFactory predicates;
    private final BuchiAutomaton buchiAutomata;
    private final Map<String, IntersectionNode> nodeMap = new HashMap<>();

    public IntersectionAutomata(IPredicateFactory predicates, BuchiAutomaton buchi) {
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

    public IPredicateFactory getPredicates() {
        return predicates;
    }

    protected String getUniqueKey(SimpleState state, BuchiNode node) {
        return state.getUniqueName() + "_" + node.getID();
    }
}
