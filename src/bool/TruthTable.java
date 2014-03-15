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
 * A truth table of a collection of Boolean expressions and all variables used
 * in the expression.
 * 
 * @author Chris Bouchard
 * @version 0.1 Beta
 */
public class TruthTable {
	/**
	 * The collection of expressions for which the truth table is to be
	 * generated
	 */
	private final Collection<BooleanExpression> expressionCollection;

	/**
	 * The map of variable dictionaries to the map of a Boolean expression to
	 * it's result when that expression is evaluated for that dictionary.
	 * 
	 * A dictionary is a map of string names of variables to their boolean truth
	 * value
	 */
	private final Map<Map<String, Boolean>, Map<BooleanExpression, Boolean>> results;

	/**
	 * The list of dictionaries in the proper order so that the rows of
	 * variables in the truth table count as binary numbers when displayed
	 */
	private final List<Map<String, Boolean>> resultsKeyList;

	/**
	 * The list of variables in the proper order so that the rows of variables
	 * in the truth table count as binary numbers when displayed
	 */
	private final List<String> variablesList;

	/**
	 * Construct a truth table from the given Boolean expression.
	 * 
	 * @param expression
	 *            the Boolean expression for which to construct this truth table
	 * 
	 * @throws ParseException
	 *             if the Boolean expression is not properly formatted
	 */
	public TruthTable(final BooleanExpression expression) throws ParseException {
		this(Collections.singletonList(expression));
	}

	/**
	 * Construct a truth table from the given collection of Boolean expressions.
	 * 
	 * @param expressionCollection
	 *            the collection of Boolean expressions for which to construct
	 *            this truth table
	 * 
	 * @throws ParseException
	 *             if any of the Boolean expressions is not properly formatted
	 */
	public TruthTable(final Collection<BooleanExpression> expressionCollection)
			throws ParseException {
		this.expressionCollection = expressionCollection;
		results = new HashMap<Map<String, Boolean>, Map<BooleanExpression, Boolean>>();

		variablesList = new ArrayList<String>();

		for (final BooleanExpression expression : expressionCollection) {
			for (final String str : expression.getVariableSet()) {
				if (!variablesList.contains(str)) {
					variablesList.add(str);
				}
			}
		}

		Collections.sort(variablesList);
		resultsKeyList = buildDictionary(variablesList);

		for (final Map<String, Boolean> dict : resultsKeyList) {
			final Map<BooleanExpression, Boolean> map = new HashMap<BooleanExpression, Boolean>();

			for (final BooleanExpression expression : expressionCollection) {
				map.put(expression, expression.evaluate(dict));
			}

			results.put(dict, map);
		}
	}

	/**
	 * Construct a truth table from a BooleanExpression created from the given
	 * string.
	 * 
	 * @param str
	 *            the string representing the Boolean expression for which to
	 *            construct this truth table
	 * 
	 * @throws ParseException
	 *             if the boolean expression is not properly formatted
	 */
	public TruthTable(final String str) throws ParseException {
		this(new BooleanExpression(str));
	}

	/**
	 * Return a string representation of this truth table.
	 * 
	 * @return a string representation of this truth table
	 */
	@Override
	public String toString() {
		String str = "";

		// Build the header by listing all variables...
		for (final String var : variablesList) {
			str += var + " ";
		}

		// and then the expressions themselves
		for (final BooleanExpression expression : expressionCollection) {
			str += " " + expression + " ";
		}

		str += "\n";

		// Build each row by listing the values of the variables and the value
		// of the expression
		for (final Map<String, Boolean> dict : resultsKeyList) {
			for (final String var : variablesList) {
				str += (dict.get(var) ? "1" : "0") + " ";
			}

			for (final BooleanExpression expression : expressionCollection) {
				for (int i = 0; i < (expression.toString().length() - 1) / 2; i++) {
					str += " ";
				}

				str += " " + (results.get(dict).get(expression) ? "1" : "0");

				for (int i = 0; i < expression.toString().length() / 2; i++) {
					str += " ";
				}

				str += " ";
			}

			str += "\n";
		}

		str = str.substring(0, str.length() - 1);

		return str;
	}
	
	/**
	 * Return a list of dictionaries of all possible combinations of the given
	 * list of variables in the proper order so that the rows of variables in
	 * the truth table count as binary numbers when displayed.
	 * 
	 * @param vars
	 *            the list of variables for which to create dictionaries
	 * 
	 * @return the list of dictionaries
	 */
	private List<Map<String, Boolean>> buildDictionary(final List<String> vars) {
		final List<Map<String, Boolean>> dictsList = new ArrayList<Map<String, Boolean>>();
		final String nextKey;

		// If there are no variables, return a list containing a single empty
		// dictionary
		if (vars.size() == 0) {
			dictsList.add(new HashMap<String, Boolean>());
			return dictsList;
		}

		nextKey = vars.get(0);

		// Base Case for recursion: If there is only one variable, return a list
		// of dictionaries of it's true and false values
		if (vars.size() == 1) {
			// Execute for both possible values of b
			for (final boolean b : new boolean[] { false, true }) {
				final Map<String, Boolean> dict = new HashMap<String, Boolean>();

				dict.put(nextKey, b);
				dictsList.add(dict);
			}
		}

		else {
			// Build the sub-list of all variables except the first
			final List<String> varsSubList = vars.subList(1, vars.size());

			// Build the dictionary for the sub-list
			final List<Map<String, Boolean>> dictsSubSet = buildDictionary(varsSubList);

			// Execute for both possible values of b
			for (final boolean b : new boolean[] { false, true }) {
				for (final Map<String, Boolean> subDict : dictsSubSet) {
					// New Map to avoid reference issues
					final Map<String, Boolean> dict = new HashMap<String, Boolean>();

					dict.put(nextKey, b);
					dict.putAll(subDict);

					// Add the new dictionary to the list of dictionaries
					dictsList.add(dict);
				}
			}
		}

		return dictsList;
	}
	
	public Map<Map<String, Boolean>, Map<BooleanExpression, Boolean>> getResults() {
		return results;
	}

}
