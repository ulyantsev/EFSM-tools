/**
 * LtlParser.java, 06.04.2008
 */
package ru.ifmo.ltl.converter;

import ru.ifmo.automata.statemachine.IAutomataContext;
import ru.ifmo.ltl.grammar.LtlNode;
import ru.ifmo.ltl.grammar.predicate.IPredicateFactory;
import ru.ifmo.ltl.LtlParseException;
import ognl.GrammarConverter;
import ognl.Node;
import ognl.Ognl;
import ognl.OgnlException;

/**
 * The ILtlparser implementation that use Ognl library
 *
 * @author Kirill Egorov
 */
public class LtlParser implements ILtlParser {

    private GrammarConverter converter;

    public LtlParser(IAutomataContext context, IPredicateFactory predicatesObj) {
        converter = new GrammarConverter(context, predicatesObj);
    }

    public LtlNode parse(String ltlExpr) throws LtlParseException {
        try {
            Node root = (Node) Ognl.parseExpression(ltlExpr);
            return converter.convert(root);
        } catch (OgnlException e) {
            throw new LtlParseException(e);
        }
    }
}
