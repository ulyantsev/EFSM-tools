/*
 * This file is part of Boolean Expression Solver. Boolean Expression Solver is
 * a program to build a truth table for a Boolean expression.
 * 
 * Copyright (C) 2007 Chris Bouchard
 * 
 * Boolean Expression Solver is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 * 
 * Boolean Expression Solver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Boolean Expression Solver; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package bool;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An operator in a boolean expression with precedence and arity. Contains
 * static utility methods to work with operators.
 * 
 * @author Chris Bouchard
 * @version 0.1 Beta
 */
public abstract class BooleanOperator
{
    /**
     * Map of strings representing unary operators to their implementation
     */
    private static final Map<String, BooleanOperator> OPERATORS = new HashMap<String, BooleanOperator>();
    
    static
    {
        // IFF (Biconditional)
        OPERATORS.put("=", new BooleanOperator(2, 5)
        {
            @Override
            public boolean apply(final boolean... operands)
            {
                return (!operands[0] || operands[1]) && (!operands[1] || operands[0]);
            }
        });
        
        // IF (Implication)
        OPERATORS.put(">", new BooleanOperator(2, 4)
        {
            @Override
            public boolean apply(final boolean... operands)
            {
                return !operands[0] || operands[1];
            }
        });
        
        // XOR (Exclusive Disjunction)
        OPERATORS.put("+", new BooleanOperator(2, 3)
        {
            @Override
            public boolean apply(final boolean... operands)
            {
                return (operands[0] || operands[1]) && !(operands[0] && operands[1]);
            }
        });
        
        // OR (Inclusive Disjunction)
        OPERATORS.put("|", new BooleanOperator(2, 2)
        {
            @Override
            public boolean apply(final boolean... operands)
            {
                return operands[0] || operands[1];
            }
        });
        
        // AND (Conjunction)
        OPERATORS.put("&", new BooleanOperator(2, 1)
        {
            @Override
            public boolean apply(final boolean... operands)
            {
                return operands[0] && operands[1];
            }
        });
        
        // NOT (Negation)
        OPERATORS.put("~", new BooleanOperator(1, 0)
        {
            @Override
            public boolean apply(final boolean... operands)
            {
                return !operands[0];
            }
        });
    }
    
    /**
     * The arity of this operator. Arity is the number of operands that an
     * operator takes as arguments
     */
    private final int arity;
    
    /**
     * The precedence of this operator among other operators. Higher precedence
     * operators are evaluated after lower precedence operators.
     */
    private final int precedence;
    
    /**
     * Construct a new BooleanOperator with the given precedence and arity
     * 
     * @param arity
     *            the arity of this operator
     * @param precedence
     *            the precedence of this operator
     */
    private BooleanOperator(final int arity, final int precedence)
    {
        this.arity = arity;
        this.precedence = precedence;
    }
    
    /**
     * Return the BooleanOperator represented by the given string.
     * 
     * @param oper
     *            the string representing the BooleanOperator to return
     * 
     * @return the BooleanOperator represented by the given string
     * 
     * @throws NoSuchElementException
     */
    public static BooleanOperator getOperator(final String oper) throws NoSuchElementException
    {
        if (OPERATORS.containsKey(oper))
        {
            return OPERATORS.get(oper);
        }
        else
        {
            throw new NoSuchElementException("There is no operator " + oper);
        }
    }
    
    /**
     * Return true if oper represents an operator, else false.
     * 
     * @param oper
     *            the string representing an operator
     * 
     * @return true if oper represents an operator, else false
     */
    public static boolean isOperator(final String oper)
    {
        return OPERATORS.containsKey(oper);
    }
    
    /**
     * Evaluate the truth value of this operator for the given operand(s). If
     * the number of operands does not equal the arity of this operator, throw
     * an IllegalArgumentException.
     * 
     * @param operands
     *            the operands for which to evaluate this operator
     * 
     * @return the truth value of this operator for the given operand(s)
     * 
     * @throws IllegalArgumentException
     *             if the number of operands does not equal the arity of this
     *             operator
     * 
     * @see #apply(boolean...)
     * @see #getArity()
     */
    public boolean evaluate(final boolean... operands) throws IllegalArgumentException
    {
        if (operands.length == arity)
        {
            return apply(operands);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }
    
    /**
     * Return the arity of this operator.
     * 
     * @return the arity of this operator
     */
    public int getArity()
    {
        return arity;
    }
    
    /**
     * Return the precedence of this operator.
     * 
     * @return the precedence of this operator
     */
    public int getPrecedence()
    {
        return precedence;
    }
    
    /**
     * Evaluate the truth value of this operator for the given operand(s).
     * 
     * @param operands
     *            the operands for which to evaluate this operator
     * 
     * @return the truth value of this operator for the given operand(s)
     * 
     * @see #evaluate(boolean...)
     */
    protected abstract boolean apply(boolean... operands);
}
