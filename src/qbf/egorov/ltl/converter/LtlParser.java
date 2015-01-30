/**
 * LtlParser.java, 06.04.2008
 */
package qbf.egorov.ltl.converter;

import qbf.egorov.ltl.LtlParseException;
import qbf.egorov.ltl.grammar.LtlNode;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.ognl.EgorovGrammarConverter;
import qbf.egorov.ognl.Node;
import qbf.egorov.ognl.Ognl;
import qbf.egorov.ognl.OgnlException;
import qbf.egorov.statemachine.IAutomataContext;

/**
 * The ILtlparser implementation that use Ognl library
 *
 * @author Kirill Egorov
 */
public class LtlParser implements ILtlParser {

    private EgorovGrammarConverter converter;

    public LtlParser(IAutomataContext context, IPredicateFactory predicatesObj) {
        converter = new EgorovGrammarConverter(context, predicatesObj);
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
