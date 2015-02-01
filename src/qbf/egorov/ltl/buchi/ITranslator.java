/**
 * ITranslator.java, 16.03.2008
 */
package qbf.egorov.ltl.buchi;

import qbf.egorov.ltl.grammar.LtlNode;

/**
 * Translate from LTL into buchi automata.
 *
 * @author Kirill Egorov
 */
public interface ITranslator {
    IBuchiAutomata translate(LtlNode root);
}
