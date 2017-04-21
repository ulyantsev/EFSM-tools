package main.misc;

/**
 * (c) Igor Buzhinsky
 */

import bool.MyBooleanExpression;
import org.apache.commons.lang3.ArrayUtils;
import scenario.StringActions;
import scenario.StringScenario;
import structures.mealy.MealyAutomaton;
import structures.mealy.MealyNode;
import structures.mealy.MealyTransition;
import verification.ltl.LtlParseException;
import verification.ltl.LtlParser;
import verification.ltl.grammar.*;
import verification.verifier.Verifier;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class UnbeastTransformer {
    public static void main(String[] args) throws IOException, ParseException, LtlParseException {
        randomInstances();
    }
    
    private static void randomInstances() throws IOException, ParseException, LtlParseException {
        final List<String> events = Arrays.asList("A", "B", "C", "D");
        final List<String> actions = Arrays.asList("z0", "z1", "z2", "z3");
        final String prefix = "evaluation/testing/complete/fsm-3-";
        for (int i = 0; i < 50; i++) {
            final Problem p = new Problem(prefix + i + "-true.ltl", prefix + i + ".sc", events, actions);
            runUnbeast(p, "unbeast-automaton-" + i, "unbeast-log-" + i, true);
        }
    }
    
    public static void runUnbeast(Problem p, String automatonPath, String logPath,
            boolean reconstructAutomaton) throws IOException, ParseException, LtlParseException {
        final long startTime = System.currentTimeMillis();
        final String outputPath = "generated-problem.xml";

        final List<String> formulae = LtlParser.load(p.ltlPath, 0, p.events);
        final List<LtlNode> nodes = LtlParser.parse(formulae);
        final List<StringScenario> scenarios = StringScenario.loadScenarios(p.scenarioPath, false);
        final Logger logger = Logger.getLogger("Logger" + System.currentTimeMillis());
        final FileHandler fh = new FileHandler(logPath, false);
        logger.addHandler(fh);
        final SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        
        final Verifier v = new Verifier(logger, formulae, p.events, p.actions);
        final List<String> specification = new ArrayList<>();
        specification.addAll(Generator.ltlSpecification(nodes));
        specification.addAll(Generator.scenarioSpecification(scenarios, p.actions));
        final String problem = Generator.problemDescription(p.events, p.actions, specification);
        logger.info(problem);
        try (PrintWriter pw = new PrintWriter(new File(outputPath))) {
            pw.println(problem);
        }
        
        final long unbeastStartTime = System.currentTimeMillis();
        final Process unbeast = Runtime.getRuntime().exec(
                new String[] { "./unbeast", "../../" + outputPath, "--runSimulator" },
                new String[0], new File("./evaluation/Unbeast-0.6b"));
        try (
                final Scanner inputScanner = new Scanner(unbeast.getInputStream());
                final PrintWriter writer = new PrintWriter(unbeast.getOutputStream(), true);
        ) {
            while (true) {
                final String line = inputScanner.nextLine();
                logger.info(line);
                if (line.equals("Do you want the game position to be printed? (y/n)")) {
                    writer.println("y");
                    break;
                }
            }
            logger.info("Unbeast execution time: " + (System.currentTimeMillis() - unbeastStartTime) + " ms");
            if (!reconstructAutomaton) {
                unbeast.destroy();
                return;
            }
            while (true) {
                final String line = inputScanner.nextLine();
                if (line.startsWith("+-+")) {
                    break;
                }
            }
            final Game game = new Game(inputScanner, writer, p.actions, p.events);
            final MealyAutomaton a = game.reconstructAutomaton(logger);
            unbeast.destroy();
            
            if (!checkAutomaton(a, p, v, scenarios)) {
                logger.severe("Compliance check failed!");
            } else {
                final MealyAutomaton minimizedA = minimizeAutomaton(a, p, v, scenarios, logger);
                logger.info(minimizedA.toString());
                try (PrintWriter pw = new PrintWriter(new File(automatonPath))) {
                    pw.println(minimizedA);
                }
                logger.info("Total execution time: " + (System.currentTimeMillis() - startTime) + " ms");
            }
        }
    }

    private static MealyAutomaton minimizeAutomaton(MealyAutomaton a, Problem p, Verifier v,
                                                    List<StringScenario> scenarios, Logger logger) {
        final Set<Integer> remainingStates = new TreeSet<>();
        for (int i = 0; i < a.stateCount(); i++) {
            remainingStates.add(i);
        }
        MealyAutomaton current = a;
        l: while (true) {
            for (int n1 : remainingStates) {
                for (int n2 : remainingStates) {
                    if (n1 >= n2) {
                        continue;
                    }
                    final MealyAutomaton merged = mergeNodes(n1, n2, current);
                    if (!checkAutomaton(merged, p, v, scenarios)) {
                        continue;
                    }
                    current = merged;
                    remainingStates.remove(n2);
                    logger.info("Destroyed state, #=" + remainingStates.size());
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
    private static MealyAutomaton mergeNodes(int n1, int n2, MealyAutomaton a) {
        final MealyAutomaton result = new MealyAutomaton(a.stateCount());
        for (int i = 0; i < a.stateCount(); i++) {
            if (i == n2) {
                continue;
            }
            for (MealyTransition t : a.state(i).transitions()) {
                final MealyTransition newT = new MealyTransition(result.state(t.src().number()),
                        result.state(t.dst().number() == n2 ? n1 : t.dst().number()),
                        t.event(), t.expr(), t.actions());
                result.addTransition(result.state(i), newT);
            }
        }
        return result;
    }
    
    private static boolean checkAutomaton(MealyAutomaton a, Problem p, Verifier v, List<StringScenario> scenarios) {
        return v.verify(a) && checkScenarioCompliance(a, p, scenarios);
    }
    
    // will not work in the presence of variables
    private static boolean checkScenarioCompliance(MealyAutomaton a, Problem p, List<StringScenario> scenarios) {
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
            final StringScenario scReordered = new StringScenario(events, expressions, stringActions);
            if (!a.compliesWith(scReordered)) {
                return false;
            }
        }
        return true;
    }
    
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
    
    static class Generator {
        static List<String> ltlSpecification(List<LtlNode> nodes) {
            final List<String> ltlStrings = new ArrayList<>();
            for (LtlNode node : nodes) {
                final StringBuilder xmlBuilder = new StringBuilder();
                node.accept(new Visitor(), xmlBuilder);
                ltlStrings.add(xmlBuilder.toString());
            }
            return ltlStrings;
        }
        
        static String varFormula(String str) {
            boolean negation = str.contains("~");
            if (negation) {
                str = str.replaceAll("~", "");
            }
            String ans = negation ? tag("Not", str) : str;
            ans = tag("Var", ans);
            return ans;
        }
        
        static String inputFormula(StringScenario sc, int index) {
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
        
        static String actionFormula(String action, boolean present) {
            final String ans = tag("Var", action);
            return present ? ans : tag("Not", ans);
        }
        
        static String outputFormula(StringScenario sc, int index, List<String> actions) {
            final String[] thisActions = sc.getActions(index).getActions();
            String formula = actionFormula(actions.get(0), ArrayUtils.contains(thisActions, actions.get(0)));
            for (int i = 1; i < actions.size(); i++) {
                formula = tag("And", actionFormula(actions.get(i),
                        ArrayUtils.contains(thisActions, actions.get(i))) + formula);
            }
            
            return formula;
        }
        
        static List<String> scenarioSpecification(List<StringScenario> scenarios, List<String> actions) {
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
        
        static List<String> eventAssumptions(List<String> events) {
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
        
        static String problemDescription(List<String> events, List<String> actions, List<String> specification) {
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
    }
    
    private static class Problem {
        final String ltlPath;
        final String scenarioPath;
        final List<String> events;
        final List<String> actions;

        public Problem(String ltlPath, String scenarioPath, List<String> events, List<String> actions) {
            this.ltlPath = ltlPath;
            this.scenarioPath = scenarioPath;
            this.events = events;
            this.actions = actions;
        }
    }
    
    static class Game {
        private final Scanner input;
        private final PrintWriter output;
        private final List<String> actions;
        private final List<String> events;

        public Game(Scanner input, PrintWriter output, List<String> actions, List<String> events) {
            this.input = input;
            this.output = output;
            this.actions = actions;
            this.events = events;
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
            final List<String> elements = new ArrayList<>();
            for (int i = 0; i < actions.size(); i++) {
                if (actionStr.charAt(i) == '1') {
                    elements.add(actions.get(i));
                }
            }
            return elements;
        }
        
        MealyAutomaton reconstructAutomaton(Logger logger) throws IOException {
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
                        logger.info("New state, current #=" + states.size());
                    }
                    s.events.add(input);
                    s.actions.add(output);
                    s.transitions.add(dest);
                }
            }
            
            final MealyAutomaton a = new MealyAutomaton(states.size());
            
            for (GameState s : states.values()) {
                for (int i = 0; i < s.transitions.size(); i++) {
                    final String input = describeEvents(s.events.get(i));
                    final List<String> output = describeActions(s.actions.get(i));
                    final StringActions actions = new StringActions(output
                            .toString().replace("[", "").replace("]", ""));
                    final MealyNode dst = a.state(s.transitions.get(i).number);
                    a.state(s.number).addTransition(input, MyBooleanExpression.getTautology(), actions, dst);
                }
            }
            return a;
        }
    }
}
