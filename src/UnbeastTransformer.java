
/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bool.MyBooleanExpression;
import choco.kernel.common.util.tools.ArrayUtils;
import scenario.StringScenario;
import egorov.ltl.LtlParseException;
import egorov.ltl.LtlParser;
import egorov.ltl.grammar.BinaryOperator;
import egorov.ltl.grammar.BooleanNode;
import egorov.ltl.grammar.INodeVisitor;
import egorov.ltl.grammar.LtlNode;
import egorov.ltl.grammar.Predicate;
import egorov.ltl.grammar.UnaryOperator;

public class UnbeastTransformer {
	private static class Visitor implements INodeVisitor<Void, StringBuilder> {
		private Void visitUnary(String name, UnaryOperator op, StringBuilder sb) {
			sb.append("<" + name + ">");
			op.getOperand().accept(this, sb);
			sb.append("</" + name + ">");
			return null;
		}
		
		private Void visitBinary(String name, BinaryOperator op, StringBuilder sb) {
			sb.append("<" + name + ">");
			op.getLeftOperand().accept(this, sb);
			op.getRightOperand().accept(this, sb);
			sb.append("</" + name + ">");
			return null;
		}

		@Override
		public Void visitNeg(UnaryOperator op, StringBuilder d) {
			return visitUnary("Not", op, d);
		}

		@Override
		public Void visitFuture(UnaryOperator op, StringBuilder d) {
			return visitUnary("F", op, d);
		}

		@Override
		public Void visitNext(UnaryOperator op, StringBuilder d) {
			return visitUnary("X", op, d);
		}

		@Override
		public Void visitAnd(BinaryOperator op, StringBuilder d) {
			return visitBinary("And", op, d);
		}

		@Override
		public Void visitOr(BinaryOperator op, StringBuilder d) {
			return visitBinary("Or", op, d);
		}

		@Override
		public Void visitRelease(BinaryOperator op, StringBuilder d) {
			d.append("<Not><U><Not>");
			op.getLeftOperand().accept(this, d);
			d.append("</Not><Not>");
			op.getRightOperand().accept(this, d);
			d.append("</Not></U></Not>");
			return null;
		}

		@Override
		public Void visitUntil(BinaryOperator op, StringBuilder d) {
			return visitBinary("U", op, d);
		}

		@Override
		public Void visitGlobal(UnaryOperator op, StringBuilder d) {
			return visitUnary("G", op, d);
		}

		@Override
		public Void visitBoolean(BooleanNode b, StringBuilder d) {
			d.append(b.getValue() ? "<True></True>" : "<False></False>");
			return null;
		}
		
		@Override
		public Void visitPredicate(Predicate p, StringBuilder d) {
			d.append(tag("Var", p.arg()));
			return null;
		}
	}
	
	private static String tag(String tag, String text) {
		return "<" + tag + ">" + text + "</" + tag + ">";
	}
	
	private static List<String> ltlSpecification(List<LtlNode> nodes) {
		final List<String> ltlStrings = new ArrayList<>();
		for (LtlNode node : nodes) {
			final StringBuilder xmlBuilder = new StringBuilder();
			node.accept(new Visitor(), xmlBuilder);
			ltlStrings.add(xmlBuilder.toString());
		}
		return ltlStrings;
	}
	
	private static String varFormula(String str) {
		boolean negation = str.contains("~");
		if (negation) {
			str = str.replaceAll("~", "");
		}
		String ans = negation ? tag("Not", str) : str;
		ans = tag("Var", ans);
		return ans;
	}
	
	private static String inputFormula(StringScenario sc, int index) {
		final String event = sc.getEvents(index).get(0);
		final MyBooleanExpression expr = sc.getExpr(index);
		String ans = tag("Var", event);
		if (!expr.toString().equals("1")) {
			String exprStr = expr.toString();
			if (exprStr.contains("&")) {
				final String[] tokens = exprStr.split("&");
				if (tokens.length != 2) {
					throw new RuntimeException("Only 2 variables are supported.");
				}
				exprStr = tag("And", varFormula(tokens[0]) + varFormula(tokens[1]));
			} else {
				exprStr = varFormula(exprStr);
			}
			ans = tag("And", ans + exprStr);
		}		
		return ans;
	}
	
	private static String actionFormula(String action, boolean present) {
		String ans = tag("Var", action);
		if (!present) {
			ans = tag("Not", ans);
		}
		return ans;
	}
	
	private static String outputFormula(StringScenario sc, int index, List<String> actions) {
		final String[] thisActions = sc.getActions(index).getActions();
		String formula = actionFormula(actions.get(0), ArrayUtils.contains(thisActions, actions.get(0)));
		for (int i = 1; i < actions.size(); i++) {
			formula = tag("And", actionFormula(actions.get(i),
					ArrayUtils.contains(thisActions, actions.get(i))) + formula);
		}
		
		return formula;
	}
	
	private static List<String> scenarioSpecification(List<StringScenario> scenarios, List<String> actions) {
		final List<String> scenarioStrings = new ArrayList<>();
		for (StringScenario sc : scenarios) {
			String formula = tag("Or", tag("Not", inputFormula(sc, sc.size() - 1))
					+ outputFormula(sc, sc.size() - 1, actions));
			for (int i = sc.size() - 2; i >= 0; i--) {
				formula = tag("Or", tag("Not", inputFormula(sc, i)) + tag("And", 
						outputFormula(sc, i, actions) + tag("X", formula)));
			}
			scenarioStrings.add(formula);
		}
		return scenarioStrings;
	}
	
	private static List<String> eventAssumptions(List<String> events) {
		final List<String> assumptions = new ArrayList<>();
		String orAssumption = tag("Var", events.get(0));
		for (int i = 1; i < events.size(); i++) {
			orAssumption = tag("Or", tag("Var", events.get(i)) + orAssumption);
		}
		assumptions.add(tag("G", orAssumption));
		for (int i = 0; i < events.size(); i++) {
			for (int j = i + 1; j < events.size(); j++) {
				assumptions.add(tag("G", tag("Not", tag("And", tag("Var", events.get(i))
						+ tag("Var", events.get(j))))));
			}
		}
		return assumptions;
	}
	
	private static String problemDescription(List<String> events, List<String> variables, List<String> actions, List<String> specification) {
		final StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n");
		sb.append("<!DOCTYPE SynthesisProblem SYSTEM \"SynSpec.dtd\">\n");
		sb.append("<SynthesisProblem>\n");
		sb.append("<Title>Generated problem</Title>\n");
		sb.append("<Description>Generated description</Description>\n");
		sb.append("<PathToLTLCompiler>ltl2ba-1.1/ltl2ba -f</PathToLTLCompiler>\n");
		sb.append("<GlobalInputs>\n");
		events.forEach(e -> sb.append("  <Bit>" + e + "</Bit>\n"));
		variables.forEach(v -> sb.append("  <Bit>" + v + "</Bit>\n"));
		sb.append("</GlobalInputs>\n");
		sb.append("<GlobalOutputs>\n");
		actions.forEach(a -> sb.append("  <Bit>" + a + "</Bit>\n"));
		sb.append("</GlobalOutputs>\n");
		sb.append("<Assumptions>\n");
		eventAssumptions(events).forEach(a -> sb.append("  <LTL>" + a + "</LTL>\n"));
		sb.append("</Assumptions>\n");
		sb.append("<Specification>\n");
		specification.forEach(s -> sb.append("  <LTL>" + s + "</LTL>\n"));
		sb.append("</Specification>\n");
		sb.append("</SynthesisProblem>\n");
		return sb.toString();
	}
	
	public static void main(String[] args) throws IOException, ParseException, LtlParseException {
		final String ltlPath = "qbf/testing-daniil/50n/nstates=5/30/formulae";
		final String scenarioPath = "qbf/testing-daniil/50n/nstates=5/30/plain-scenarios";
		final String outputPath = "generated-problem.xml";
		
		final List<String> events = Arrays.asList("A", "B");
		final List<String> variables = Arrays.asList("x0", "x1");
		final List<String> actions = Arrays.asList("z0", "z1");
		
		final List<LtlNode> nodes = LtlParser.loadProperties(ltlPath, 0);
		final List<StringScenario> scenarios = StringScenario.loadScenarios(scenarioPath, -1);
		final List<String> specification = new ArrayList<>();
		specification.addAll(ltlSpecification(nodes));
		specification.addAll(scenarioSpecification(scenarios, actions));
		final String problem = problemDescription(events, variables, actions, specification);
		System.out.println(problem);
		try (PrintWriter pw = new PrintWriter(new File(outputPath))) {
			pw.println(problem);
		}
	}
}
