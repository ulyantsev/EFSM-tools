/**
 * ILtl.java, 11.03.2008
 */
package ru.ifmo.ltl;

import ru.ifmo.ltl.grammar.LtlNode;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface ILtlUtils {

    public LtlNode neg(LtlNode root);

    public LtlNode toNnf(LtlNode root);

    /**
     * Fp -> true U p and Gp -> false R p
     * @param root root node
     * @return ltl tree without F and G operators
     */
    public LtlNode normalize(LtlNode root);
}
