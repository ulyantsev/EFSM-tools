/**
 * SimpleVerifier.java, 06.04.2008
 */
package qbf.egorov.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import qbf.egorov.ltl.buchi.IBuchiAutomata;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.statemachine.IState;
import qbf.egorov.transducer.verifier.TransitionCounter;
import qbf.egorov.verifier.automata.IntersectionAutomata;
import qbf.egorov.verifier.automata.IntersectionNode;
import qbf.egorov.verifier.automata.IntersectionTransition;

/**
 * Simple IVerifier implementation. Use one thread and can't be used cuncurrently.
 *
 * @author Kirill Egorov
 */
public class SimpleVerifier<S extends IState> {
    private final S initState;
    
    public SimpleVerifier(S initState) {
        this.initState = initState;
    }

    public List<IntersectionTransition<?>> verify(IBuchiAutomata buchi, IPredicateFactory<S> predicates,
                                                TransitionCounter counter) {
        IntersectionAutomata<S> automata = new IntersectionAutomata<>(predicates, buchi);
        IntersectionNode<?> initial = automata.getNode(initState, buchi.getStartNode(), 0);
        Deque<IntersectionTransition<?>> stack = new MainDfs().dfs(initial, counter);
        List<IntersectionTransition<?>> res = new ArrayList<>(stack);
        Collections.reverse(res);
        return res;
    }
}
