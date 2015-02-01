/**
 * ILtlConverter.java, 06.04.2008
 */
package qbf.egorov.ltl.converter;

import qbf.egorov.ltl.LtlParseException;
import qbf.egorov.ltl.grammar.LtlNode;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface ILtlParser {
    LtlNode parse(String ltlExpr) throws LtlParseException;
}
