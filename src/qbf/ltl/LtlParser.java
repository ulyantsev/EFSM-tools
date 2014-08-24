/**
 * LtlParser.java, 06.04.2008
 */
package qbf.ltl;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import ognl.*;

/**
 * The ILtlparser implementation that use Ognl library
 *
 * @author: Kirill Egorov, Igor Buzhinsky
 */
public class LtlParser {
	private static class GrammarConverter {
		public static LtlNode convert(Node root) {
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
				return new qbf.ltl.Predicate(name,
						Collections.singletonList(root.jjtGetChild(0)
								.toString().replaceAll(".*\\.", "")));
			} else if (className.equals("ASTAnd")) {
				return createBinaryOperator((SimpleNode) root,
						BinaryOperatorType.AND);
			} else if (className.equals("ASTOr")) {
				return createBinaryOperator((SimpleNode) root,
						BinaryOperatorType.OR);
			} else if (className.equals("ASTShiftRight")) {
				return createBinaryOperator((SimpleNode) root,
						BinaryOperatorType.IMPLIES);
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

	public static LtlNode parse(String ltlExpr) throws LtlParseException {
		try {
			return GrammarConverter.convert((Node) Ognl
					.parseExpression(ltlExpr));
		} catch (OgnlException e) {
			throw new LtlParseException(e);
		}
	}

	public static List<LtlNode> loadProperties(String filepath)
			throws ParseException, FileNotFoundException, LtlParseException {
		List<LtlNode> ans = new ArrayList<>();

		try (Scanner in = new Scanner(new File(filepath))) {
			while (in.hasNextLine()) {
				String input = in.nextLine().trim();
				if (input.startsWith("#")) {
					// comment
					continue;
				}
				if (input.isEmpty()) {
					continue;
				}
				input = input.replaceAll("->", ">>");
				ans.add(parse(input));
			}
		}
		return ans;
	}
}
