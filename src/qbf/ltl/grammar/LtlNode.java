/**
 * LtlNode.java, 11.03.2008
 */
package qbf.ltl.grammar;

import org.apache.commons.lang3.StringUtils;

/**
 * TODO: add comment
 *
 * @author: Kirill Egorov
 */
public abstract class LtlNode {
    private String name;

    public String strExpr;

    public LtlNode(String name, String strExpr) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("LtlNode name can't be null or blank");
        }
        this.name = name;
        this.strExpr = strExpr;
    }

    public LtlNode(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("LtlNode name can't be null or blank");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
    
    public abstract String toFullString();
}
