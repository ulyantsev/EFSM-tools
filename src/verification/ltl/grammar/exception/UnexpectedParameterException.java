/**
 * UnexpectedParameterException.java, 14.03.2008
 */
package verification.ltl.grammar.exception;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class UnexpectedParameterException extends RuntimeException {
    public UnexpectedParameterException(Class<?> aClass) {
	    super("Unexpected class: " + aClass.getName());
    }
}
