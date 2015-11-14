package egorov.ltl;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ognl.Node;
import ognl.Ognl;
import ognl.OgnlException;
import egorov.ltl.grammar.LtlNode;
import egorov.ltl.grammar.PredicateFactory;

public class LtlParser {
	private static String simplify(String formula) {
		return formula.trim()
				.replace("wasEvent", "event")
				.replace("wasAction", "action")
				.replace("wasVariable", "variable")
				.replace("ep.", "").replace("co.", "");
	}
	
    private static String duplicateEvents(String formula, int varNumber) {
		final Pattern p = Pattern.compile("event\\((\\w+)\\)");
		final Matcher m = p.matcher(formula);
		final StringBuilder sb = new StringBuilder();
		int lastPos = 0;
		while (m.find()) {
			final String event = m.group(1);
			sb.append(formula.substring(lastPos, m.start()));
			final List<String> expansion = new ArrayList<>();
			for (int j = 0; j < 1 << varNumber; j++) {
				char[] arr = new char[varNumber];
				for (int pos = 0; pos < varNumber; pos++) {
					arr[pos] = ((j >> pos) & 1) == 1 ? '1' : '0';
				}
				expansion.add("event(" + event + String.valueOf(arr) + ")");
			}
			lastPos = m.end();
			String strToAppend = String.join(" || ", expansion);
			if (expansion.size() > 1) {
				strToAppend = "(" + strToAppend + ")";
			}
			sb.append(strToAppend);
		}
		sb.append(formula.substring(lastPos, formula.length()));
		return sb.toString();
	}
    
    private static String expandWasVariable(String formula, int varNumber, List<String> events) {
    	final Pattern p = Pattern.compile("variable\\((\\w+)\\)");
		final Matcher m = p.matcher(formula);
		final StringBuilder sb = new StringBuilder();
		int lastPos = 0;
		while (m.find()) {
			final String varName = m.group(1);
			final int varIndex = Integer.parseInt(varName.substring(1));
			sb.append(formula.substring(lastPos, m.start()));
			final List<String> expansion = new ArrayList<>();
			for (String event : events) {
				for (int j = 0; j < 1 << varNumber; j++) {
					char[] arr = new char[varNumber];
					for (int pos = 0; pos < varNumber; pos++) {
						arr[pos] = ((j >> pos) & 1) == 1 ? '1' : '0';
					}
					if (arr[varIndex] == '1') {
						expansion.add("event(" + event + String.valueOf(arr) + ")");
					}
				}
			}
			lastPos = m.end();
			String strToAppend = String.join(" || ", expansion);
			if (expansion.size() > 1) {
				strToAppend = "(" + strToAppend + ")";
			}
			sb.append(strToAppend);
		}
		sb.append(formula.substring(lastPos, formula.length()));
		return sb.toString();
	}
    
    public static List<String> load(String filepath, int varNumber, List<String> events)
			throws FileNotFoundException {
    	if (filepath == null) {
    		return Collections.emptyList();
    	}
    	final List<String> ans = new ArrayList<>();

		try (Scanner in = new Scanner(new File(filepath))) {
			while (in.hasNextLine()) {
				ans.add(expandWasVariable(duplicateEvents(simplify(in.nextLine()), varNumber), varNumber, events));
			}
		}
		return ans;
	}
    
    public static List<LtlNode> parse(List<String> ltl) throws LtlParseException {
    	return parse(ltl, new GrammarConverter(new PredicateFactory()));
	}
    
    public static List<LtlNode> parse(List<String> ltl, GrammarConverter converter) throws LtlParseException {
    	final List<LtlNode> ans = new ArrayList<>();
    	for (String prop : ltl) {
    		ans.add(parse(prop, converter));
    	}
		return ans;
	}
    
    private static LtlNode parse(String ltlExpr, GrammarConverter converter) throws LtlParseException {
		try {
			return converter.convert((Node) Ognl.parseExpression(ltlExpr));
		} catch (OgnlException e) {
			throw new LtlParseException(e);
		}
	}
}
