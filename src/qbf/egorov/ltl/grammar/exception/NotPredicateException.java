/**
 * NotPredicateException.java, 12.03.2008
 */
package qbf.egorov.ltl.grammar.exception;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class NotPredicateException extends RuntimeException {
    public NotPredicateException() {
    	super();
    }

    public NotPredicateException(String message) {
    	super(message);
    }

    public NotPredicateException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotPredicateException(Throwable cause) {
        super(cause);
    }
}
