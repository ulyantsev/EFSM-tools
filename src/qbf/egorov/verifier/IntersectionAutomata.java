/**
 * IntersectionAutomata.java, 12.04.2008
 */
package qbf.egorov.verifier;

import qbf.egorov.ltl.buchi.BuchiAutomata;
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
    private final BuchiAutomata buchiAutomata;
    private final Map<String, IntersectionNode> nodeMap = new HashMap<>();

    public IntersectionAutomata(IPredicateFactory predicates, BuchiAutomata buchi) {
        this.predicates = predicates;
        buchiAutomata = buchi;
    }

    public BuchiAutomata getBuchiAutomata() {
        return buchiAutomata;
    }

    public IntersectionNode getNode(SimpleState state, BuchiNode node, int acceptSet) {
        String key = getUniqueKey(state, node, acceptSet);

        IntersectionNode res = nodeMap.get(key);
        if (res == null) {
            res = new IntersectionNode(this, state, node, acceptSet);
            nodeMap.put(key, res);
        }
        return res;
    }

    public IPredicateFactory getPredicates() {
        return predicates;
    }

    protected String getUniqueKey(SimpleState state, BuchiNode node, int acceptSet) {
        return state.getUniqueName() + "_" + node.getID() + "_" + acceptSet;
    }
}
