/**
 * Function.java, 05.03.2008
 */
package ru.ifmo.automata.statemachine.impl;

import ru.ifmo.automata.statemachine.IFunction;
import org.apache.commons.lang.NotImplementedException;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class Function implements IFunction {
    private String name;
    private String description;
    private Class returnType;

    protected Function(String name, String description, Class returnType) {
        this.name = name;
        this.description = description;
        this.returnType = returnType;
    }

    public Function(String name, Class returnType) {
        this(name, null, returnType);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Object getCurValue() {
        throw new NotImplementedException();
    }

    public Class getReturnType() {
        return returnType;
    }

    public int hashCode() {
        return name.hashCode() ^ returnType.hashCode();
    }
}
