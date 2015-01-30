/**
 * IntersectionAutomata.java, 12.04.2008
 */
package ru.ifmo.verifier.automata;

import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.ltl.buchi.IBuchiNode;
import ru.ifmo.ltl.buchi.IBuchiAutomata;
import ru.ifmo.ltl.grammar.predicate.IPredicateFactory;

import java.util.*;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class IntersectionAutomata<S extends IState> implements IIntersectionAutomata<S> {

    private IPredicateFactory<S> predicates;
    private IBuchiAutomata buchiAutomata;

//    private Map<S, Map<IBuchiNode, Map<Integer, IntersectionNode<S>>>> nodeMap
//            = new HashMap<S, Map<IBuchiNode, Map<Integer, IntersectionNode<S>>>>();
    private Map<String, IntersectionNode<S>> nodeMap
            = new HashMap<String, IntersectionNode<S>>();

    public IntersectionAutomata(IPredicateFactory<S> predicates, IBuchiAutomata buchi) {
        if (buchi == null || predicates == null) {
            throw new IllegalArgumentException();
        }
        this.predicates = predicates;
        this.buchiAutomata = buchi;
    }

    public IBuchiAutomata getBuchiAutomata() {
        return buchiAutomata;
    }

    public IntersectionNode<S> getNode(S state, IBuchiNode node, int acceptSet) {
//        Map<IBuchiNode, Map<Integer, IntersectionNode<S>>> buchiMap = nodeMap.get(state);
//        if (buchiMap == null) {
//            buchiMap = new HashMap<IBuchiNode, Map<Integer, IntersectionNode<S>>>();
//            nodeMap.put(state, buchiMap);
//        }
//
//        Map<Integer, IntersectionNode<S>> acceptMap = buchiMap.get(node);
//        if (acceptMap == null) {
//            acceptMap = new HashMap<Integer, IntersectionNode<S>>();
//            buchiMap.put(node, acceptMap);
//        }
//
//        IntersectionNode<S> res = acceptMap.get(acceptSet);
//        if (res == null) {
//            res = new IntersectionNode<S>(this, state, node, acceptSet);
//            acceptMap.put(acceptSet, res);
//            nodes.add(res);
//        }
//        return res;
        String key = getUniqueKey(state, node, acceptSet);

        IntersectionNode<S> res = nodeMap.get(key);
        if (res == null) {
            res = new IntersectionNode<S>(this, state, node, acceptSet);
            nodeMap.put(key, res);
        }
        return res;
    }

    public IPredicateFactory<S> getPredicates() {
        return predicates;
    }

    protected String getUniqueKey(IState state, IBuchiNode node, int acceptSet) {
        return state.getUniqueName() + "_" + node.getID() + "_" + acceptSet;
    }
}
