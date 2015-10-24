/**
 * LtlParser.java, 06.04.2008
 */
package egorov.ltl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import egorov.EgorovGrammarConverter;
import egorov.ltl.grammar.BinaryOperator;
import egorov.ltl.grammar.BinaryOperatorType;
import egorov.ltl.grammar.BooleanNode;
import egorov.ltl.grammar.LtlNode;
import egorov.ltl.grammar.PredicateFactory;
import egorov.ltl.grammar.UnaryOperator;
import egorov.ltl.grammar.UnaryOperatorType;
import ognl.Node;
import ognl.Ognl;
import ognl.OgnlException;
import ognl.SimpleNode;

/**
 * The ILtlparser implementation that use Ognl library
 *
 * @author Kirill Egorov
 */
public class LtlParser {
    private EgorovGrammarConverter converter;

    public LtlParser(PredicateFactory predicatesObj) {
        converter = new EgorovGrammarConverter(predicatesObj);
    }
    
    public static String duplicateEvents(String formula, int varNumber) {
		final Pattern p = Pattern.compile("wasEvent\\(ep\\.(\\w+)\\)");
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
				expansion.add("wasEvent(ep." + event + String.valueOf(arr) + ")");
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
    
    /*
     * For simplified usage.
     */
    public static List<LtlNode> loadProperties(String filepath, int varNumber)
			throws FileNotFoundException, LtlParseException {
    	final List<LtlNode> ans = new ArrayList<>();

		try (Scanner in = new Scanner(new File(filepath))) {
			while (in.hasNextLine()) {
				String input = duplicateEvents(in.nextLine().trim(), varNumber);
				if (!input.isEmpty()) {
					ans.add(GrammarConverter.simpleParse(input));
				}
			}
		}
		return ans;
	}
    
    private static class GrammarConverter {
    	static LtlNode simpleParse(String ltlExpr) throws LtlParseException {
    		try {
    			return convert((Node) Ognl.parseExpression(ltlExpr));
    		} catch (OgnlException e) {
    			throw new LtlParseException(e);
    		}
    	}
    	
		static LtlNode convert(Node root) {
			if (root == null) {
				throw new IllegalArgumentException("Node shouldn't be null");
			}
			
			final String className = root.getClass().getSimpleName();

			if (className.equals("ASTMethod")) {
				String name = root.toString().replaceAll("\\(.*$", "");
				// is unary operator?
				for (UnaryOperatorType type : UnaryOperatorType.values()) {
					if (type.toString().equalsIgnoreCase(name)) {
						return createUnaryOperator((SimpleNode) root, type);
					}
				}
				// is binary operator?
				for (BinaryOperatorType type : BinaryOperatorType.values()) {
					if (type.toString().equalsIgnoreCase(name)) {
						return createBinaryOperator((SimpleNode) root, type);
					}
				}
				return new egorov.ltl.grammar.Predicate(name,
						root.jjtGetChild(0).toString().replaceAll(".*\\.", ""));
			} else if (className.equals("ASTAnd")) {
				return createBinaryOperator((SimpleNode) root,
						BinaryOperatorType.AND);
			} else if (className.equals("ASTOr")) {
				return createBinaryOperator((SimpleNode) root,
						BinaryOperatorType.OR);
			} else if (className.equals("ASTNot")) {
				return createUnaryOperator((SimpleNode) root,
						UnaryOperatorType.NEG);
			} else if (className.equals("ASTConst")) {
				String name = root.toString();
				if (name.equals("true") || name.equals("false")) {
					return BooleanNode.getByName(name);
				} else {
					throw new RuntimeException("Unexpected node "
							+ root.getClass());
				}
			}
			throw new RuntimeException("Unexpected node "
					+ root.getClass());
		}

		private static UnaryOperator createUnaryOperator(SimpleNode node,
				UnaryOperatorType type) {
			if (node.jjtGetNumChildren() != 1) {
				throw new RuntimeException(node + " isn't unary operator");
			}
			UnaryOperator op = new UnaryOperator(type);
			op.setOperand(convert(node.jjtGetChild(0)));
			return op;
		}

		private static BinaryOperator createBinaryOperator(SimpleNode node,
				BinaryOperatorType type) {
			if (node.jjtGetNumChildren() < 2) {
				throw new RuntimeException(node + " isn't binary operation");
			}
			return createBinaryOperator(node, 0, type);
		}

		private static BinaryOperator createBinaryOperator(SimpleNode node,
				int i, BinaryOperatorType type) {
			BinaryOperator op = new BinaryOperator(type);
			op.setLeftOperand(convert(node.jjtGetChild(i)));
			LtlNode right = (++i == node.jjtGetNumChildren() - 1) ? convert(node
					.jjtGetChild(i)) : createBinaryOperator(node, i, type);
			op.setRightOperand(right);
			return op;
		}
	}
    
    public LtlNode parse(String ltlExpr) throws LtlParseException {
        try {
            Node root = (Node) Ognl.parseExpression(ltlExpr);
            return converter.convert(root);
        } catch (OgnlException e) {
            throw new LtlParseException(e);
        }
    }
}
