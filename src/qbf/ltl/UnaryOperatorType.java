/**
 * UnaryOperatorType.java, 11.03.2008
 */
package qbf.ltl;

/**
 * TODO: add comment
 *
 * @author: Kirill Egorov
 */
public enum UnaryOperatorType implements IOperatorType {
    NEG("!"), FUTURE("F"), NEXT("X"), GLOBAL("G");

    private String name;

    private UnaryOperatorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
