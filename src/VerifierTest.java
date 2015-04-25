import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import qbf.egorov.verifier.Counterexample;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.Transition;
import tools.AutomatonGVLoader;
import actions.StringActions;
import bool.MyBooleanExpression;

public class VerifierTest {
	public static void test1() throws FileNotFoundException {
		Logger logger = Logger.getLogger("Logger");
		int varNumber = 0;
		int size = 2;
		List<String> events = Arrays.asList("A", "B", "C");
		List<String> actions = Arrays.asList("z");
		Automaton a = new Automaton(size);
		a.addTransition(a.getState(0), new Transition(a.getState(0), a.getState(1), "A", MyBooleanExpression.getTautology(), new StringActions("z")));
		a.addTransition(a.getState(1), new Transition(a.getState(1), a.getState(0), "B", MyBooleanExpression.getTautology(), new StringActions("z")));
		a.addTransition(a.getState(1), new Transition(a.getState(1), a.getState(1), "C", MyBooleanExpression.getTautology(), new StringActions("")));
		System.out.println(a);
		String filename = "tmp.ltl";

		for (String ltl : Arrays.asList(
				"!wasEvent(ep.A)", "wasEvent(ep.B)", "wasEvent(ep.A)", "!wasEvent(ep.B)",
				"X(wasEvent(ep.B))", "X(wasEvent(ep.C))", "X(wasEvent(ep.B) || wasEvent(ep.C))",
				"true", "false", "G(wasEvent(ep.A))", "G(wasEvent(ep.A) || wasEvent(ep.B))",
				"G(!wasEvent(ep.C) || X(wasEvent(ep.C) || wasEvent(ep.B)))",
				"F(wasEvent(ep.C))", "F(wasEvent(ep.B))", // bad counter-examples!
				"F(G(wasEvent(ep.C)))"
				)) {
			try (PrintWriter pw = new PrintWriter(filename)) {
				pw.println(ltl);
			}
			Verifier v = new Verifier(size, logger, filename, events, actions, varNumber);
			System.out.println(v.verifyWithCounterExamples(a));
			new File(filename).delete();
		}
	}
	
	public static void test2() throws IOException, ParseException {
		Logger logger = Logger.getLogger("Logger");
		Automaton a = AutomatonGVLoader.load("qbf/jhotdraw.gv");
		String filename = "tmp.ltl";
		List<String> formulae = Arrays.asList(
				"G(!(wasEvent(ep.setpos)) || X((wasEvent(ep.edit)) || (wasEvent(ep.setdim))))",
				"!U(!wasEvent(ep.edit), wasEvent(ep.finalise))",
				"!U(!wasEvent(ep.setpos), wasEvent(ep.setdim))",
				"!U(!wasEvent(ep.setpos), wasEvent(ep.edit))",
				"!U(!(wasEvent(ep.figure) || wasEvent(ep.text)), wasEvent(ep.setpos))",
				"G(!wasEvent(ep.finalise) || X(wasEvent(ep.figure) || wasEvent(ep.text)))",
				"!(wasEvent(ep.text) && X(wasEvent(ep.setpos))) || !X(X(wasEvent(ep.setdim)))",
				"G(!(wasEvent(ep.figure) && X(wasEvent(ep.setpos))) || X(X(wasEvent(ep.setdim))))",
				"G(!wasEvent(ep.setdim) || X(!wasEvent(ep.setpos)))",
				"F(wasEvent(ep.setpos))", // false
				"G(!wasEvent(ep.setpos) || X(F(wasEvent(ep.setpos))))", //false
				"F(G(wasEvent(ep.figure)))",  //false
				"F(G(wasEvent(ep.text)))", //false
				"G(wasEvent(ep.figure) || wasEvent(ep.text) || wasEvent(ep.setpos) || wasEvent(ep.setdim))" // false
				);
		for (String ltl : formulae) {
			try (PrintWriter pw = new PrintWriter(filename)) {
				pw.println(ltl);
			}
			Verifier v = new Verifier(a.statesCount(), logger, filename,
					Arrays.asList("figure", "text", "setpos", "edit", "setdim", "finalise"), Arrays.asList(), 0);
			System.out.println(v.verifyWithCounterExamples(a));
			new File(filename).delete();
		}
		
		Verifier v = new Verifier(a.statesCount(), logger, "qbf/walkinshaw/jhotdraw.ltl",
				Arrays.asList("figure", "text", "setpos", "edit", "setdim", "finalise"), Arrays.asList(), 0);
		System.out.println(v.verify(a));
		new File(filename).delete();
	}
	
	/*public static void test3() throws IOException, ParseException {
		Logger logger = Logger.getLogger("Logger");
		Automaton a = AutomatonGVLoader.load("qbf/t.gv");
		Verifier v = new Verifier(a.statesCount(), logger, "qbf/testing-daniil/200n/nstates=5/5/formulae",
				Arrays.asList("A00", "A01", "A10", "A11", "B00", "B01", "B10", "B11"), Arrays.asList("z0", "z1"), 2);
		System.out.println(v.verifyWithCounterExamples(a));
	}*/
	
	public static void randomTestsIgor() throws IOException, ParseException {
		Logger logger = Logger.getLogger("Logger");
		for (int states = 3; states <= 10; states++) {
			for (String completeness : Arrays.asList("incomplete", "complete")) {
				for (int i = 0; i < 50; i++) {
					Automaton a = AutomatonGVLoader.load("qbf/testing/" + completeness + "/fsm-" + states + "-" + i + ".dot");
					System.out.println(a);
					for (boolean verdict : Arrays.asList(true, false)) {
						System.out.println(completeness + " " + states + " " + i + " " + verdict);
						Verifier v = new Verifier(a.statesCount(), logger, "qbf/testing/" + completeness + "/fsm-" + states + "-" + i + "-" + verdict + ".ltl",
							Arrays.asList("A", "B", "C", "D"), Arrays.asList("z0", "z1", "z2", "z3"), 0);
						List<Counterexample> result = v.verifyWithCounterExamples(a);
						boolean boolResult = result.stream().allMatch(Counterexample::isEmpty);
						if (boolResult != verdict) {
							throw new AssertionError("Expected " + verdict + ", got " + boolResult);
						}
						if (!boolResult) {
							System.out.println(result.toString().replaceAll("[ ,]", ""));
						}
					}
				}
			}
		}
	}
	
	public static void randomTestsIgor_() throws IOException, ParseException {
		Logger logger = Logger.getLogger("Logger");
		int states = 3;
		String completeness = "incomplete";
		int i = 12;
		Automaton a = AutomatonGVLoader.load("qbf/testing/" + completeness + "/fsm-" + states + "-" + i + ".dot");
		System.out.println(a);
		boolean verdict = false;
		System.out.println(completeness + " " + states + " " + i + " " + verdict);
		Verifier v = new Verifier(a.statesCount(), logger, "qbf/testing/" + completeness + "/fsm-" + states + "-" + i + "-" + verdict + ".ltl",
			Arrays.asList("A", "B", "C", "D"), Arrays.asList("z0", "z1", "z2", "z3"), 0);
		List<Counterexample> result = v.verifyWithCounterExamples(a);
		System.out.println(result);
		boolean boolResult = result.stream().allMatch(Counterexample::isEmpty);
		if (boolResult != verdict) {
			throw new AssertionError("Expected " + verdict + ", got " + boolResult);
		}
		if (!boolResult) {
			System.out.println(result.toString().replaceAll("[ ,]", ""));
		}
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		//test1();
		//test2();
		randomTestsIgor();
		//randomTestsIgor_();
	}
}
