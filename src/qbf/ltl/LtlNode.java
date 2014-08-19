/**
 * LtlNode.java, 11.03.2008
 */
package qbf.ltl;

import org.apache.commons.lang3.StringUtils;

/**
 * TODO: add comment
 *
 * @author: Kirill Egorov
 */
public abstract class LtlNode {
    private String name;

    public LtlNode(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("LtlNode name can't be null or blank");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
