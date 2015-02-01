/**
 * SimpleVerifier.java, 06.04.2008
 */
package qbf.egorov.verifier.impl;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import qbf.egorov.ltl.buchi.IBuchiAutomata;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.statemachine.IState;
import qbf.egorov.verifier.IDfs;
import qbf.egorov.verifier.IDfsListener;
import qbf.egorov.verifier.ISharedData;
import qbf.egorov.verifier.IVerifier;
import qbf.egorov.verifier.automata.IIntersectionTransition;
import qbf.egorov.verifier.automata.IntersectionAutomata;
import qbf.egorov.verifier.automata.IntersectionNode;
import qbf.egorov.verifier.concurrent.SharedData;

/**
 * Simple IVerifier implementation. Use one thread and can't be used cuncurrently.
 *
 * @author Kirill Egorov
 */
public class SimpleVerifier<S extends IState> implements IVerifier<S> {
    private final S initState;
    
    public SimpleVerifier(S initState) {
        if (initState == null) {
            throw new IllegalArgumentException("stateMachine can't be null");
        }
        this.initState = initState;
    }

    @Override
    public List<IIntersectionTransition> verify(IBuchiAutomata buchi, IPredicateFactory<S> predicates,
                                                IDfsListener... listeners) {
        IntersectionAutomata<S> automata = new IntersectionAutomata<S>(predicates, buchi);
        IntersectionNode initial = automata.getNode(initState, buchi.getStartNode(), 0);
        ISharedData sharedData = new SharedData(new HashSet<>());

        IDfs<Deque<IIntersectionTransition>> dfs = new MainDfs(sharedData, -1);
        for (IDfsListener l : listeners) {
            dfs.add(l);
        }

        Deque<IIntersectionTransition> stack = dfs.dfs(initial);

        List<IIntersectionTransition> res = new ArrayList<IIntersectionTransition>(stack.size());

        for (Iterator<IIntersectionTransition> iter = stack.descendingIterator(); iter.hasNext();) {
            res.add(iter.next());
        }
        return res;
    }
}
