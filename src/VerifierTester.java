import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.logging.Logger;

import actions.StringActions;
import bool.MyBooleanExpression;
import qbf.egorov.ltl.LtlParseException;
import qbf.reduction.Verifier;
import structures.Automaton;
import tools.AutomatonGVLoader;

public class VerifierTester {
	private static final Logger logger = Logger.getLogger("Logger");
	
	private static void test1() throws ParseException, FileNotFoundException {
		Automaton automaton = new Automaton(2);
		automaton.getState(0).addTransition("A", MyBooleanExpression.get("1"), new StringActions("z0, z1"), automaton.getState(1));
		automaton.getState(1).addTransition("A", MyBooleanExpression.get("1"), new StringActions("z0, z1"), automaton.getState(0));
		System.out.println(automaton);
		try (PrintWriter resultPrintWriter = new PrintWriter(new File("generated-fsm.gv"))) {
			resultPrintWriter.println(automaton);
		}
		String formula = "X(wasAction(co.z0))";
		System.out.println(formula);
		try (PrintWriter resultPrintWriter = new PrintWriter(new File("formula.ltl"))) {
			resultPrintWriter.println(formula);
		}
		Verifier v = new Verifier(2, logger, "formula.ltl");
		v.verify(automaton);
	}
	
	private static void test2() throws IOException, ParseException {
		for (int s = 2; s <= 10; s++) {
			for (int e = 2; e <= 5; e++) {
				for (int a = 2; a <= 5; a++) {
					Automaton automaton = AutomatonGVLoader.load(
							"qbf/testing/fsm_" + s + "s" + e + "e" + a + "a.dot"
					);
					Verifier v = new Verifier(s, logger, "qbf/testing/fsm_" + s + "s" + e + "e" + a + "a-true.ltl");
					v.verify(automaton);
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException, ParseException, LtlParseException {
		test1();
	}

}
