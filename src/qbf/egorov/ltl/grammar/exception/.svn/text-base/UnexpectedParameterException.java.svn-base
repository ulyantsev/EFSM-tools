/**
 * UnexpectedParameterException.java, 14.03.2008
 */
package ru.ifmo.ltl.grammar.exception;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class UnexpectedParameterException extends RuntimeException {
    public UnexpectedParameterException(Class<?> aClass) {
	    super("Unexpected class: " + aClass.getName());
    }

    public UnexpectedParameterException(Class<?> aClass, String parameter) {
	    super("Unknown parameter \"" + parameter + "\" for class " + aClass.getName());
    }
}
