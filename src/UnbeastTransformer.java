
/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import scenario.StringActions;
import scenario.StringScenario;
import structures.Automaton;
import structures.Node;
import structures.Transition;
import bool.MyBooleanExpression;
import choco.kernel.common.util.tools.ArrayUtils;
import egorov.Verifier;
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
	
	private static List<String> ltlSpecification(List<LtlNode> nodes, boolean incomplete) {
		final List<String> ltlStrings = new ArrayList<>();
		for (LtlNode node : nodes) {
			final StringBuilder xmlBuilder = new StringBuilder();
			node.accept(new Visitor(), xmlBuilder);
			String ltlString = xmlBuilder.toString();
			if (incomplete) {
				ltlString = tag("Or", tag("F", tag("Var", "Invalid")) + ltlString);
			}
			ltlStrings.add(ltlString);
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
	
	private static String outputFormula(StringScenario sc, int index, List<String> actions,
			boolean incomplete) {
		final String[] thisActions = sc.getActions(index).getActions();
		String formula = actionFormula(actions.get(0), ArrayUtils.contains(thisActions, actions.get(0)));
		for (int i = 1; i < actions.size(); i++) {
			formula = tag("And", actionFormula(actions.get(i),
					ArrayUtils.contains(thisActions, actions.get(i))) + formula);
		}
		if (incomplete) {
			formula = tag("And", tag("Not", tag("Var", "Invalid")) + formula);
		}
		
		return formula;
	}
	
	private static List<String> scenarioSpecification(List<StringScenario> scenarios, List<String> actions,
			boolean incomplete) {
		final List<String> scenarioStrings = new ArrayList<>();
		for (StringScenario sc : scenarios) {
			String formula = tag("Or", tag("Not", inputFormula(sc, sc.size() - 1))
					+ outputFormula(sc, sc.size() - 1, actions, incomplete));
			for (int i = sc.size() - 2; i >= 0; i--) {
				formula = tag("Or", tag("Not", inputFormula(sc, i)) + tag("And", 
						outputFormula(sc, i, actions, incomplete) + tag("X", formula)));
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
	
	private static String problemDescription(List<String> events, List<String> variables,
			List<String> actions, List<String> specification, boolean incomplete) {
		final StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n");
		sb.append("<!DOCTYPE SynthesisProblem SYSTEM \"SynSpec.dtd\">\n");
		sb.append("<SynthesisProblem>\n");
		sb.append("<Title>Generated problem</Title>\n");
		sb.append("<Description>Generated description</Description>\n");
		//sb.append("<PathToLTLCompiler>ltl2ba-1.1/ltl2ba -f</PathToLTLCompiler>\n");
		sb.append("<PathToLTLCompiler>./ltl2tgba-wrapper</PathToLTLCompiler>\n");
		sb.append("<GlobalInputs>\n");
		events.forEach(e -> sb.append("  <Bit>" + e + "</Bit>\n"));
		variables.forEach(v -> sb.append("  <Bit>" + v + "</Bit>\n"));
		sb.append("</GlobalInputs>\n");
		sb.append("<GlobalOutputs>\n");
		actions.forEach(a -> sb.append("  <Bit>" + a + "</Bit>\n"));
		if (incomplete) {
			sb.append("  <Bit>Invalid</Bit>\n");
		}
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
	
	private static class Problem {
		final String ltlPath;
		final String scenarioPath;
		final List<String> events;
		final List<String> variables;
		final List<String> actions;
		final boolean incomplete;
		
		public Problem(String ltlPath, String scenarioPath,
				List<String> events, List<String> variables,
				List<String> actions, boolean incomplete) {
			this.ltlPath = ltlPath;
			this.scenarioPath = scenarioPath;
			this.events = events;
			this.variables = variables;
			this.actions = actions;
			this.incomplete = incomplete;
		}
	}
	
	static final Problem pRandom = new Problem("qbf/testing/incomplete/fsm-4-1-true.ltl",
			"qbf/testing/incomplete/fsm-4-1.sc",
			Arrays.asList("A", "B", "C", "D"),
			Arrays.asList(),
			Arrays.asList("z0", "z1", "z2", "z3"),
			true);
	
	static final Problem pElevator = new Problem("qbf/Unbeast-0.6b/my/elevator.ltl",
			"qbf/Unbeast-0.6b/my/elevator.sc",
			Arrays.asList("e11", "e2", "e12", "e3", "e4"),
			Arrays.asList(),
			Arrays.asList("z1", "z2", "z3"),
			true);
	
	static final Problem pElevatorMini = new Problem("qbf/Unbeast-0.6b/my/elevator.ltl",
			"qbf/Unbeast-0.6b/my/elevator-mini.sc",
			Arrays.asList("e11", "e2", "e12", "e3", "e4"),
			Arrays.asList(),
			Arrays.asList("z1", "z2", "z3"),
			true);
	
	static final Problem pSwitch = new Problem("qbf/Unbeast-0.6b/my/switch.ltl",
			"qbf/Unbeast-0.6b/my/switch.sc",
			Arrays.asList("turnon", "turnoff", "touch"),
			Arrays.asList(),
			Arrays.asList("on", "off"),
			false);
	
	public static void main(String[] args) throws IOException, ParseException, LtlParseException {
		final long startTime = System.currentTimeMillis();
		final String outputPath = "generated-problem.xml";
		
		final Problem p = pRandom;
		
		final List<LtlNode> nodes = LtlParser.loadProperties(p.ltlPath, 0);
		final List<StringScenario> scenarios = StringScenario.loadScenarios(p.scenarioPath, -1);
		final Logger logger = Logger.getLogger("Logger");
		final Verifier v = new Verifier(logger, p.ltlPath, p.events, p.actions, p.variables.size());
		final List<String> specification = new ArrayList<>();
		specification.addAll(ltlSpecification(nodes, p.incomplete));
		specification.addAll(scenarioSpecification(scenarios, p.actions, p.incomplete));
		final String problem = problemDescription(p.events, p.variables, p.actions, specification, p.incomplete);
		System.out.println(problem);
		try (PrintWriter pw = new PrintWriter(new File(outputPath))) {
			pw.println(problem);
		}
		
		final long unbeastStartTime = System.currentTimeMillis();
		final Process unbeast = Runtime.getRuntime().exec(
				new String[] { "./unbeast", "../../" + outputPath, "--runSimulator" },
				new String[0], new File("./qbf/Unbeast-0.6b"));
		try (
				final Scanner inputScanner = new Scanner(unbeast.getInputStream());
				final Scanner errorScanner = new Scanner(unbeast.getErrorStream());
				final PrintWriter writer = new PrintWriter(unbeast.getOutputStream(), true);
		) {
			while (true) {
				final String line = inputScanner.nextLine();
				System.out.println(line);
				if (line.equals("Do you want the game position to be printed? (y/n)")) {
					writer.println("y");
					break;
				}
			}
			System.out.println("Unbeast execution time: " + (System.currentTimeMillis() - unbeastStartTime) + " ms");
			while (true) {
				final String line = inputScanner.nextLine();
				//System.out.println(line);
				if (line.startsWith("+-+")) {
					break;
				}
			}
			final Game game = new Game(inputScanner, writer, p.actions, p.events,
					p.incomplete);
			final Automaton a = game.reconstructAutomaton();
			unbeast.destroy();
			
			if (!checkAutomaton(a, p, v, scenarios)) {
				System.err.println("Compliance check failed!");
			} else {
				final Automaton minimizedA = minimizeAutomaton(a, p, v, scenarios);
				System.out.println();
				System.out.println(minimizedA);
				try (PrintWriter pw = new PrintWriter(new File("unbeast-automaton.dot"))) {
					pw.println(minimizedA);
				}
				System.out.println("Total execution time: " + (System.currentTimeMillis() - startTime) + " ms");
			}
		}
	}

	private static Automaton minimizeAutomaton(Automaton a, Problem p, Verifier v, List<StringScenario> scenarios) {
		final Set<Integer> remainingStates = new TreeSet<>();
		for (int i = 0; i < a.statesCount(); i++) {
			remainingStates.add(i);
		}
		Automaton current = a;
		l: while (true) {
			for (int n1 : remainingStates) {
				for (int n2 : remainingStates) {
					if (n1 >= n2) {
						continue;
					}
					final Automaton merged = mergeNodes(n1, n2, current);
					if (!checkAutomaton(merged, p, v, scenarios)) {
						continue;
					}
					current = merged;
					remainingStates.remove(n2);
					System.out.println("Destroyed state, #=" + remainingStates.size());
					continue l;
				}
			}
			break l;
		}
		return current;
	}
	
	/*
	 * Removes transitions to and from node n2.
	 */
	private static Automaton mergeNodes(int n1, int n2, Automaton a) {
		final Automaton result = new Automaton(a.statesCount());
		for (int i = 0; i < a.statesCount(); i++) {
			if (i == n2) {
				continue;
			}
			for (Transition t : a.getState(i).getTransitions()) {
				final Transition newT = new Transition(result.getState(t.getSrc().getNumber()),
						result.getState(t.getDst().getNumber() == n2 ? n1 : t.getDst().getNumber()),
						t.getEvent(), t.getExpr(), t.getActions());
				result.addTransition(result.getState(i), newT);
			}
		}
		return result;
	}
	
	private static boolean checkAutomaton(Automaton a, Problem p, Verifier v, List<StringScenario> scenarios) {
		return v.verify(a) && checkScenarioCompliance(a, p, scenarios);
	}
	
	private static boolean checkScenarioCompliance(Automaton a, Problem p, List<StringScenario> scenarios) {
		// FIXME will not work in the presence of variables
		for (StringScenario sc : scenarios) {
			final List<String> events = new ArrayList<>();
			final List<MyBooleanExpression> expressions = new ArrayList<>();
			final List<StringActions> stringActions = new ArrayList<>();
			for (int i = 0; i < sc.size(); i++) {
				events.add(sc.getEvents(i).get(0));
				expressions.add(MyBooleanExpression.getTautology());
				final String[] initialActions = sc.getActions(i).getActions();
				final List<String> finalActions = new ArrayList<>();
				for (String action : p.actions) {
					if (org.apache.commons.lang3.ArrayUtils.contains(initialActions, action)) {
						finalActions.add(action);
					}
				}
				stringActions.add(new StringActions(String.join(", ", finalActions)));
			}
			final StringScenario scReordered = new StringScenario(true, events, expressions, stringActions);
			if (!a.isCompliantWithScenario(scReordered)) {
				return false;
			}
		}
		return true;
	}
	
	static class Game {
		private final Scanner input;
		private final PrintWriter output;
		private final List<String> actions;
		private final List<String> events;
		private final boolean incomplete;
		
		public Game(Scanner input, PrintWriter output, List<String> actions,
				List<String> events, boolean incomplete) {
			this.input = input;
			this.output = output;
			this.actions = actions;
			this.events = events;
			this.incomplete = incomplete;
		}

		static class GameState {
			final int number;
			final String description;
			final List<String> inputPath;
			final List<String> events = new ArrayList<>();
			final List<GameState> transitions = new ArrayList<>();
			final List<String> actions = new ArrayList<>();
			
			GameState(int actualNumber, String description, List<String> inputPath) {
				this.number = actualNumber;
				this.description = description;
				this.inputPath = inputPath;
			}
		}
		
		private String eventString(int num) {
			final StringBuilder combination = new StringBuilder();
			for (int j = 0; j < num; j++) {
				combination.append("0");
			}
			combination.append("1");
			for (int j = num + 1; j < events.size(); j++) {
				combination.append("0");
			}
			return combination.toString();
		}
		
		private void command(String command) {
			//System.out.println("command : " + command);
			output.println(command);
		}
		
		private String read() {
			final String line = input.nextLine();
			//System.out.println("response: " + line);
			return line;
		}
		
		private void reachState(GameState state) {
			//System.out.println("reaching " + state.description + "...");
			command("r");

			for (String element : state.inputPath) {
				command(element);
				command("c");
				read();
			}
			//System.out.println("reached  " + state.description);
		}
		
		private List<String> step(int num) {
			command(eventString(num));
			final String line1 = read();
			final String[] tokens1 = line1.split("\\|");
			command("c");
			command(eventString(num));
			final String line2 = read();
			final String[] tokens2 = line2.split("\\|");
			return Arrays.asList(
					tokens1[tokens1.length - 3].trim(),
					tokens1[tokens1.length - 2].trim(),
					tokens1[tokens1.length - 1].trim(),
					tokens2[tokens2.length - 3].trim()
			);
		}
		
		private String describeEvents(String eventStr) {
			String varStr;
			if (eventStr.length() > events.size()) {
				varStr = "[" + eventStr.substring(events.size()) + "]";
			} else {
				varStr = "";
			}
			for (int i = 0; i < events.size(); i++) {
				if (eventStr.charAt(i) == '1') {
					return events.get(i) + varStr;
				}
			}
			throw new AssertionError();
		}
		
		private List<String> describeActions(String actionStr) {
			if (incomplete && actionStr.charAt(actions.size()) == '1') {
				return null; // special output
			}
			final List<String> elements = new ArrayList<>();
			for (int i = 0; i < actions.size(); i++) {
				if (actionStr.charAt(i) == '1') {
					elements.add(actions.get(i));
				}
			}
			return elements;
		}
		
		Automaton reconstructAutomaton() throws IOException {
			final List<String> firstStateData = step(0);
			final Map<String, GameState> states = new LinkedHashMap<>();
			final Deque<GameState> unprocessedStates = new LinkedList<>();
			int statesNum = 0;
			final GameState initialState = new GameState(statesNum++,
					firstStateData.get(0), Arrays.asList());
			unprocessedStates.add(initialState);
			states.put(initialState.description, initialState);
			while (!unprocessedStates.isEmpty()) {
				final GameState s = unprocessedStates.removeFirst();
				for (int j = 0; j < events.size(); j++) {
					reachState(s);
					final List<String> reply = step(j);
					//System.out.println("transition: " + reply);
					final String input = reply.get(1);
					final String output = reply.get(2);
					final String newDescription = reply.get(3);
					GameState dest = states.get(newDescription);
					if (dest == null) {
						final List<String> newPath = new ArrayList<>(s.inputPath);
						newPath.add(input);
						dest = new GameState(statesNum++, newDescription, newPath);
						states.put(newDescription, dest);
						unprocessedStates.add(dest);
						System.out.println("New state, current #=" + states.size());
					}
					s.events.add(input);
					s.actions.add(output);
					s.transitions.add(dest);
				}
			}
			
			final Automaton a = new Automaton(states.size());
			
			for (GameState s : states.values()) {
				for (int i = 0; i < s.transitions.size(); i++) {
					final String input = describeEvents(s.events.get(i));
					final List<String> output = describeActions(s.actions.get(i));
					if (output == null) {
						continue;
					}
					final StringActions actions = new StringActions(output
							.toString().replace("[", "").replace("]", ""));
					final Node dst = a.getState(s.transitions.get(i).number);
					a.getState(s.number).addTransition(input, MyBooleanExpression.getTautology(),
							actions, dst);
				}
			}
			return a;
		}
	}
}
