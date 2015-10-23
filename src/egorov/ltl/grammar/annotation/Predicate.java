/**
 * Predicate.java, 12.03.2008
 */
package egorov.ltl.grammar.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Predicate {
}
