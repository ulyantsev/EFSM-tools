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
import java.util.Collection;
import java.util.Set;

/**
 * Utility class to work with variables in a Boolean expression.
 * 
 * @author Chris Bouchard
 * @version 0.1 Beta
 */
public class Variables
{
    /**
     * Return true if str is a valid variable name, else false.
     * 
     * @param str
     *            the string to evaluate
     * 
     * @param dictionaries
     *            the set of dictionaries to check to determine if str is
     *            defined as a constant
     * 
     * @return true if str is a valid variable name, else false
     */
    public static boolean isVariable(final String str, final Set<Collection<String>> dictionaries)
    {
        if (BooleanOperator.isOperator(str) || Brackets.isOpenBracket(str) || Brackets.isCloseBracket(str) || str.length() != 1
                || !Character.isLetter(str.charAt(0)))
        {
            return false;
        }
        
        for (final Collection<String> dict : dictionaries)
        {
            if (dict.contains(str))
            {
                return false;
            }
        }
        
        return true;
    }
}
