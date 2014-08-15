/**
 * BinaryOperatorType.java, 11.03.2008
 */
package qbf.ltl.grammar;

/**
 * TODO: add comment
 *
 * @author: Kirill Egorov
 */
public enum BinaryOperatorType implements IOperatorType {
    AND("&&"), OR("||"), RELEASE("R"), UNTIL("U"), IMPLIES("->");

    private String name;

    private BinaryOperatorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
