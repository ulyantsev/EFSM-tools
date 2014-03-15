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
import java.text.ParseException;
import java.util.*;

/**
 * A class to evaluate Boolean expressions composed of values, variables, binary
 * and unary operators, and brackets based on dictionaries mapping variables to
 * values.
 * 
 * @author Chris Bouchard
 * @version 0.1 Beta
 */
public class BooleanExpression
{
    /**
     * Enumeration of possible states when parsing a Boolean expression
     */
    protected static enum ParseState
    {
        /**
         * The last character parsed was a binary operator
         */
        BINARY_OPERATOR,

        /**
         * The last character parsed was a close bracket
         */
        CLOSE_BRACKET,

        /**
         * The last character parsed was an open bracket
         */
        OPEN_BRACKET,

        /**
         * This is the first character to be parsed
         */
        START,

        /**
         * The last character parsed was a unary operator
         */
        UNARY_OPERATOR,

        /**
         * The last character parsed was a value or variable
         */
        VALUE
    }
    
    /**
     * Map of default variables to their Boolean value
     */
    private static final Map<String, Boolean> STANDARD_DICT = new HashMap<String, Boolean>();
    
    static
    {
        STANDARD_DICT.put("0", false);
        STANDARD_DICT.put("1", true);
    }
    
    /**
     * The given string to be parsed into this BooleanExpression
     */
    private final String expression;
    
    /**
     * Map of default variables to their Boolean value
     */
    private final Map<String, Boolean> userDict;
    
    /**
     * The set of all variables used in this BooleanExpression
     */
    private final Set<String> variables = new HashSet<String>();
    
    /**
     * Construct a new BooleanExpression from the given string and a default
     * standard dictionary
     * 
     * @param expression
     *            the string to be parsed into a Boolean expression
     */
    public BooleanExpression(final String expression)
    {
        this(expression, new HashMap<String, Boolean>());
    }
    
    /**
     * Construct a new BooleanExpression from the given string
     * 
     * @param expression
     *            the string to be parsed into a Boolean expression
     * 
     * @param userDict
     *            the standard dictionary for this expression
     */
    public BooleanExpression(final String expression, final Map<String, Boolean> userDict)
    {
        this.userDict = userDict;
        this.expression = expression;
        
        final Set<Collection<String>> dictionaries = new HashSet<Collection<String>>();
        dictionaries.add(userDict.keySet());
        dictionaries.add(STANDARD_DICT.keySet());
        
        for (final char ch : expression.toCharArray())
        {
            final String str = Character.toString(ch);
            
            if (Variables.isVariable(str, dictionaries))
            {
                variables.add(str);
            }
        }
    }
    
    /**
     * Reduce the values stack based on the given operator.
     * 
     * @param str
     *            the operator to evaluate using the values stack. Will be
     *            modified by this method.
     * 
     * @param valuesStack
     *            the stack of values from which operands should be popped and
     *            the result pushed
     */
    private static void reduce(final String str, final Stack<Boolean> valuesStack)
    {
        final BooleanOperator oper = BooleanOperator.getOperator(str);
        final int arity = oper.getArity();
        final boolean[] operands = new boolean[arity];
        
        for (int i = oper.getArity() - 1; i >= 0; i--)
        {
            operands[i] = valuesStack.pop();
        }
        
        valuesStack.push(BooleanOperator.getOperator(str).evaluate(operands));
    }
    
    /**
     * Reduce the values stack based on the given operator and return it as a
     * string.
     * 
     * @param str
     *            the operator to evaluate using the values stack. Will be
     *            modified by this method.
     * 
     * @param valuesStack
     *            the stack of values from which operands should be popped and
     *            the result pushed
     * 
     * @return a string representing the reduced expression
     */
    private static String strReduce(final String str, final Stack<String> valuesStack)
    {
        final BooleanOperator oper = BooleanOperator.getOperator(str);
        
        if (oper.getArity() == 2)
        {
            final String operand2 = valuesStack.pop();
            final String operand1 = valuesStack.pop();
            
            valuesStack.push(operand1 + str + operand2);
        }
        else if (oper.getArity() == 1)
        {
            valuesStack.push(str + valuesStack.pop());
        }
        
        return valuesStack.peek();
    }
    
    /**
     * Return true if obj is a BooleanExpression of the same expression, else
     * false
     * 
     * @param obj
     *            the object to test for equivalence
     * 
     * @return true if obj is a BooleanExpression of the same expression, else
     *         false
     */
    @Override
    public boolean equals(final Object obj)
    {
        return obj instanceof BooleanExpression && ((BooleanExpression) obj).expression.equals(expression);
    }
    
    /**
     * Evaluate this BooleanExpression for the given dictionary of variables to
     * values and returns the truth value. Throw a ParseException if this
     * BooleanExpression is not properly formatted.
     * 
     * A BooleanExpression is not properly formatted if:
     * <ul>
     * <li>a binary operator is not preceded and followed by a value or a
     * properly formatted subexpression that reduces to a value</li>
     * <li>a unary operator is not followed by a value or a properly formatted
     * subexpression that reduces to a value</li>
     * <li>an open bracket is not matched by close bracket</li>
     * <li>an unknown symbol appears in the expression</li>
     * </ul>
     * 
     * @param dict
     *            dictionary of variables to values
     * 
     * @return the truth value of this BooleanExpression for the given
     *         dictionary
     * 
     * @throws ParseException
     *             if this BooleanExpression is not properly formatted.
     */
    public boolean evaluate(final Map<String, Boolean> dict) throws ParseException
    {
        final Stack<Boolean> valuesStack = new Stack<Boolean>();
        final Stack<String> operatorsStack = new Stack<String>();
        
        ParseState state = ParseState.START;
        
        for (int i = 0; i < expression.length(); i++)
        {
            final String str = Character.toString(expression.charAt(i));
            
            if (BooleanOperator.isOperator(str))
            {
                final BooleanOperator oper = BooleanOperator.getOperator(str);
                final int precedence = oper.getPrecedence();
                
                if (oper.getArity() == 2)
                {
                    if (state != ParseState.VALUE && state != ParseState.CLOSE_BRACKET)
                    {
                        throw new ParseException("Unexpected binary operator: " + str, i);
                    }
                    
                    while (!operatorsStack.isEmpty() && BooleanOperator.isOperator(operatorsStack.peek())
                            && precedence >= BooleanOperator.getOperator(operatorsStack.peek()).getPrecedence())
                    {
                        reduce(operatorsStack.pop(), valuesStack);
                    }
                    
                    operatorsStack.push(str);
                    state = ParseState.BINARY_OPERATOR;
                }
                
                else if (oper.getArity() == 1)
                {
                    if (state != ParseState.START && state != ParseState.BINARY_OPERATOR && state != ParseState.OPEN_BRACKET)
                    {
                        throw new ParseException("Unexpected unary operator: " + str, i);
                    }
                    
                    while (!operatorsStack.isEmpty() && BooleanOperator.isOperator(operatorsStack.peek())
                            && precedence >= BooleanOperator.getOperator(operatorsStack.peek()).getPrecedence())
                    {
                        reduce(operatorsStack.pop(), valuesStack);
                    }
                    
                    operatorsStack.push(str);
                    state = ParseState.UNARY_OPERATOR;
                }
            }
            
            else if (Brackets.isOpenBracket(str))
            {
                if (state != ParseState.START && state != ParseState.OPEN_BRACKET && state != ParseState.BINARY_OPERATOR && state != ParseState.UNARY_OPERATOR)
                {
                    throw new ParseException("Unexpected opening brace: " + str, i);
                }
                
                operatorsStack.push(str);
                state = ParseState.OPEN_BRACKET;
            }
            
            else if (Brackets.isCloseBracket(str))
            {
                String oper;
                
                if (state != ParseState.VALUE && state != ParseState.OPEN_BRACKET && state != ParseState.CLOSE_BRACKET)
                {
                    throw new ParseException("Unexpected closing brace: " + str, i);
                }
                
                try
                {
                    while (!Brackets.isBracketPair((oper = operatorsStack.pop()), str))
                    {
                        reduce(oper, valuesStack);
                    }
                }
                catch (final EmptyStackException e)
                {
                    throw new ParseException("Unexpected closing brace: " + str, i);
                }
                
                state = ParseState.CLOSE_BRACKET;
            }
            
            else if (variables.contains(str) && dict.containsKey(str))
            {
                if (state != ParseState.START && state != ParseState.BINARY_OPERATOR && state != ParseState.OPEN_BRACKET && state != ParseState.UNARY_OPERATOR)
                {
                    throw new ParseException("Unexpected value: " + str, i);
                }
                
                valuesStack.push(dict.get(str));
                state = ParseState.VALUE;
            }
            
            else if (userDict.containsKey(str))
            {
                if (state != ParseState.START && state != ParseState.BINARY_OPERATOR && state != ParseState.OPEN_BRACKET && state != ParseState.UNARY_OPERATOR)
                {
                    throw new ParseException("Unexpected value: " + str, i);
                }
                
                valuesStack.push(userDict.get(str));
                state = ParseState.VALUE;
            }
            
            else if (STANDARD_DICT.containsKey(str))
            {
                if (state != ParseState.START && state != ParseState.BINARY_OPERATOR && state != ParseState.OPEN_BRACKET && state != ParseState.UNARY_OPERATOR)
                {
                    throw new ParseException("Unexpected value: " + str, i);
                }
                
                valuesStack.push(STANDARD_DICT.get(str));
                state = ParseState.VALUE;
            }
            
            else
            {
                throw new ParseException("Unknown symbol: " + str, i);
            }
        }
        
        while (!operatorsStack.isEmpty())
        {
            final String oper = operatorsStack.pop();
            
            if (BooleanOperator.isOperator(oper))
            {
                reduce(oper, valuesStack);
            }
            else if (Brackets.isOpenBracket(oper))
            {
                throw new ParseException("Unclosed open bracket: " + oper, expression.length() - 1);
            }
        }
        
        return valuesStack.pop();
    }
    
    /**
     * Return a set of all subexpressions of at least an operator and a value.
     * 
     * @return a set of all subexpressions of at least an operator and a value
     * 
     * @throws ParseException
     *             if this BooleanExpression is not properly formatted.
     */
    public Set<BooleanExpression> getSubExpressions() throws ParseException
    {
        final Stack<String> valuesStack = new Stack<String>();
        final Stack<String> operatorsStack = new Stack<String>();
        final Set<BooleanExpression> exprSet = new HashSet<BooleanExpression>();
        
        ParseState state = ParseState.START;
        
        for (int i = 0; i < expression.length(); i++)
        {
            final String str = Character.toString(expression.charAt(i));
            
            if (BooleanOperator.isOperator(str))
            {
                final BooleanOperator oper = BooleanOperator.getOperator(str);
                final int precedence = oper.getPrecedence();
                
                if (oper.getArity() == 2)
                {
                    if (state != ParseState.VALUE && state != ParseState.CLOSE_BRACKET)
                    {
                        throw new ParseException("Unexpected binary operator: " + str, i);
                    }
                    
                    while (!operatorsStack.isEmpty() && BooleanOperator.isOperator(operatorsStack.peek())
                            && precedence >= BooleanOperator.getOperator(operatorsStack.peek()).getPrecedence())
                    {
                        exprSet.add(new BooleanExpression(strReduce(operatorsStack.pop(), valuesStack), userDict));
                    }
                    
                    operatorsStack.push(str);
                    state = ParseState.BINARY_OPERATOR;
                }
                
                else if (oper.getArity() == 1)
                {
                    if (state != ParseState.START && state != ParseState.BINARY_OPERATOR && state != ParseState.OPEN_BRACKET)
                    {
                        throw new ParseException("Unexpected unary operator: " + str, i);
                    }
                    
                    while (!operatorsStack.isEmpty() && BooleanOperator.isOperator(operatorsStack.peek())
                            && precedence >= BooleanOperator.getOperator(operatorsStack.peek()).getPrecedence())
                    {
                        exprSet.add(new BooleanExpression(strReduce(operatorsStack.pop(), valuesStack), userDict));
                    }
                    
                    operatorsStack.push(str);
                    state = ParseState.UNARY_OPERATOR;
                }
            }
            
            else if (Brackets.isOpenBracket(str))
            {
                if (state != ParseState.START && state != ParseState.OPEN_BRACKET && state != ParseState.BINARY_OPERATOR && state != ParseState.UNARY_OPERATOR)
                {
                    throw new ParseException("Unexpected opening brace: " + str, i);
                }
                
                operatorsStack.push(str);
                state = ParseState.OPEN_BRACKET;
            }
            
            else if (Brackets.isCloseBracket(str))
            {
                String oper;
                
                if (state != ParseState.VALUE && state != ParseState.OPEN_BRACKET && state != ParseState.CLOSE_BRACKET)
                {
                    throw new ParseException("Unexpected closing brace: " + str, i);
                }
                
                try
                {
                    while (!Brackets.isBracketPair((oper = operatorsStack.pop()), str))
                    {
                        exprSet.add(new BooleanExpression(strReduce(oper, valuesStack), userDict));
                    }
                    
                    valuesStack.push(oper + valuesStack.pop() + str);
                }
                catch (final EmptyStackException e)
                {
                    throw new ParseException("Unexpected closing brace: " + str, i);
                }
                
                state = ParseState.CLOSE_BRACKET;
            }
            
            else if (variables.contains(str))
            {
                if (state != ParseState.START && state != ParseState.BINARY_OPERATOR && state != ParseState.OPEN_BRACKET && state != ParseState.UNARY_OPERATOR)
                {
                    throw new ParseException("Unexpected value: " + str, i);
                }
                
                valuesStack.push(str);
                state = ParseState.VALUE;
            }
            
            else if (userDict.containsKey(str))
            {
                if (state != ParseState.START && state != ParseState.BINARY_OPERATOR && state != ParseState.OPEN_BRACKET && state != ParseState.UNARY_OPERATOR)
                {
                    throw new ParseException("Unexpected value: " + str, i);
                }
                
                valuesStack.push(str);
                state = ParseState.VALUE;
            }
            
            else if (STANDARD_DICT.containsKey(str))
            {
                if (state != ParseState.START && state != ParseState.BINARY_OPERATOR && state != ParseState.OPEN_BRACKET && state != ParseState.UNARY_OPERATOR)
                {
                    throw new ParseException("Unexpected value: " + str, i);
                }
                
                valuesStack.push(str);
                state = ParseState.VALUE;
            }
            
            else
            {
                throw new ParseException("Unknown symbol: " + str, i);
            }
        }
        
        while (!operatorsStack.isEmpty())
        {
            final String oper = operatorsStack.pop();
            
            if (BooleanOperator.isOperator(oper))
            {
                exprSet.add(new BooleanExpression(strReduce(oper, valuesStack), userDict));
            }
            else if (Brackets.isOpenBracket(oper))
            {
                throw new ParseException("Unclosed open bracket: " + oper, expression.length() - 1);
            }
        }
        
        if (exprSet.size() == 0)
        {
            exprSet.add(new BooleanExpression(expression, userDict));
        }
        
        return exprSet;
    }
    
    /**
     * Return the set of all variables used in this BooleanExpression.
     * 
     * @return the set of all variables used in this BooleanExpression
     */
    public Set<String> getVariableSet()
    {
        return variables;
    }
    
    /**
     * Return the given string representation of this BooleanExpression.
     * 
     * @return the given string representation of this BooleanExpression
     */
    @Override
    public String toString()
    {
        return expression;
    }
    
    /**
     * Return true if str is a valid variable name, else false.
     * 
     * @param str
     *            the string to evaluate
     * 
     * @return true if str is a valid variable name, else false
     * 
     * private boolean isVariable(final String str) { return
     * !BooleanOperator.isOperator(str) && !Brackets.isOpenBracket(str) &&
     * !Brackets.isCloseBracket(str) && !userDict.containsKey(str) &&
     * !STANDARD_DICT.containsKey(str) && str.length() == 1 &&
     * Character.isLetter(str.charAt(0)); }
     */
}
