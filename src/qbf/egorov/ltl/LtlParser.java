/**
 * LtlParser.java, 06.04.2008
 */
package qbf.egorov.ltl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import qbf.egorov.ltl.grammar.BinaryOperator;
import qbf.egorov.ltl.grammar.BinaryOperatorType;
import qbf.egorov.ltl.grammar.BooleanNode;
import qbf.egorov.ltl.grammar.LtlNode;
import qbf.egorov.ltl.grammar.UnaryOperator;
import qbf.egorov.ltl.grammar.UnaryOperatorType;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.ognl.EgorovGrammarConverter;
import qbf.egorov.ognl.Node;
import qbf.egorov.ognl.Ognl;
import qbf.egorov.ognl.OgnlException;
import qbf.egorov.ognl.SimpleNode;
import qbf.egorov.statemachine.IAutomataContext;

/**
 * The ILtlparser implementation that use Ognl library
 *
 * @author Kirill Egorov
 */
public class LtlParser {
    private EgorovGrammarConverter converter;

    public LtlParser(IAutomataContext context, IPredicateFactory<?> predicatesObj) {
        converter = new EgorovGrammarConverter(context, predicatesObj);
    }
    
    /*
     * For simplified usage.
     */
    public static List<LtlNode> loadProperties(String filepath)
			throws FileNotFoundException, LtlParseException {
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
				ans.add(GrammarConverter.simpleParse(input));
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
				return new qbf.egorov.ltl.grammar.Predicate(name,
						Collections.singletonList(root.jjtGetChild(0)
								.toString().replaceAll(".*\\.", "")));
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
