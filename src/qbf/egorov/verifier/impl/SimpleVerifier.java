/**
 * SimpleVerifier.java, 06.04.2008
 */
package qbf.egorov.verifier.impl;

import qbf.egorov.ltl.LtlParseException;
import qbf.egorov.ltl.buchi.IBuchiAutomata;
import qbf.egorov.ltl.buchi.ITranslator;
import qbf.egorov.ltl.converter.ILtlParser;
import qbf.egorov.ltl.grammar.LtlNode;
import qbf.egorov.ltl.grammar.LtlUtils;
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

import java.util.*;

/**
 * Simple IVerifier implementation. Use one thread and can't be used cuncurrently.
 *
 * @author Kirill Egorov
 */
public class SimpleVerifier<S extends IState> implements IVerifier<S> {
    private S initState;
    private ILtlParser parser;
    private ITranslator translator;

    public SimpleVerifier(S initState) {
        this(initState, null, null);

    }

    public SimpleVerifier(S initState, ILtlParser parser, ITranslator translator) {
        if (initState == null) {
            throw new IllegalArgumentException("stateMachine can't be null");
        }

        this.initState = initState;
        this.parser = parser;
        this.translator = translator;
    }

    public void setParser(ILtlParser parser) {
        this.parser = parser;
    }

    public void setTranslator(ITranslator translator) {
        this.translator = translator;
    }

    public List<IIntersectionTransition> verify(String ltlFormula, IPredicateFactory<S> predicates,
                                                IDfsListener... listeners) throws LtlParseException {
        if (parser == null) {
            throw new UnsupportedOperationException("Can't verify LTL formula without LTL parser."
                    + "Define it first or use List<IStateTransition> verify(IBuchiAutomata buchi) method instead");
        }
        if (translator == null) {
            throw new UnsupportedOperationException("Can't verify LTL formula without LTL translator."
                    + "Define it first or use List<IStateTransition> verify(IBuchiAutomata buchi) method instead");
        }
        LtlNode ltl = parser.parse(ltlFormula);
        ltl = LtlUtils.getInstance().neg(ltl);
        IBuchiAutomata buchi = translator.translate(ltl);

        //TODO: -----------------------
        System.out.println("LTL: " + ltlFormula);
        System.out.println(buchi);
        //-----------------------------
        
        return verify(buchi, predicates, listeners);
    }

    public List<IIntersectionTransition> verify(IBuchiAutomata buchi, IPredicateFactory<S> predicates,
                                                IDfsListener... listeners) {
        IntersectionAutomata<S> automata = new IntersectionAutomata<S>(predicates, buchi);
        IntersectionNode initial = automata.getNode(initState, buchi.getStartNode(), 0);
        ISharedData sharedData = new SharedData(new HashSet<IntersectionNode>(), 0);

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
