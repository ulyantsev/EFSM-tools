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

/**
 * A utility class to work with brackets in Boolean expressions.
 * 
 * @author Chris Bouchard
 * @version 0.1 Beta
 */
public class Brackets
{
    /**
     * Map of open brackets to their matching close bracket
     */
    private static final Map<String, String> BRACKET_PAIRS = new HashMap<String, String>();
    
    static
    {
        BRACKET_PAIRS.put("(", ")");
        BRACKET_PAIRS.put("[", "]");
        BRACKET_PAIRS.put("{", "}");
    }
    
    /**
     * Return true if open and close are a matching pair of open and close
     * brackets, respectively, else false.
     * 
     * @param open
     *            the open bracket to evaluate
     * @param close
     *            the close bracket to evaluate
     * 
     * @return true if open and close are a matching pair of brackets, else
     *         false
     */
    public static boolean isBracketPair(final String open, final String close)
    {
        return BRACKET_PAIRS.containsKey(open) && BRACKET_PAIRS.get(open).equals(close);
    }
    
    /**
     * Return true if str is a close bracket, else false
     * 
     * @param str
     *            the string to evaluate
     * 
     * @return true if str is a close bracket, else false
     * 
     * @see #isOpenBracket(String)
     */
    public static boolean isCloseBracket(final String str)
    {
        return BRACKET_PAIRS.containsValue(str);
    }
    
    /**
     * Return true if str is an open bracket, else false.
     * 
     * @param str
     *            the string to evaluate
     * 
     * @return true if str is an open bracket, else false
     * 
     * @see #isCloseBracket(String)
     */
    public static boolean isOpenBracket(final String str)
    {
        return BRACKET_PAIRS.containsKey(str);
    }
}
