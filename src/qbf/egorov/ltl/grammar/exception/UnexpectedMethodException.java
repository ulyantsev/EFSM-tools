/**
 * UnexpectedMethodException.java, 12.03.2008
 */
package qbf.egorov.ltl.grammar.exception;

import java.lang.reflect.Method;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class UnexpectedMethodException extends RuntimeException {

    public UnexpectedMethodException(String message) {
	    super(message);
    }
}
