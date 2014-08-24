/**
 * BinaryOperatorType.java, 11.03.2008
 */
package qbf.ltl;

/**
 * TODO: add comment
 *
 * @author: Kirill Egorov
 */
public enum BinaryOperatorType {
    AND("&&"), OR("||"), RELEASE("R"), UNTIL("U"), IMPLIES("->");

    private String name;

    private BinaryOperatorType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
