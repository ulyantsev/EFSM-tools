import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Logger;

import qbf.ltl.LtlParseException;
import qbf.reduction.Verifier;
import structures.Automaton;
import tools.AutomatonGVLoader;


public class VerifierTester {

	public static void main(String[] args) throws IOException, ParseException, LtlParseException {

		Logger logger = Logger.getLogger("Logger");
		for (int s = 2; s <= 3; s++) {
			for (int e = 2; e <= 3; e++) {
				for (int a = 2; a <= 3; a++) {
					Automaton automaton = AutomatonGVLoader.load(
							"qbf/testing/fsm_" + s + "s" + e + "e" + a + "a.dot"
					);
					Verifier v= new Verifier(s, logger, "qbf/testing/fsm_" + s + "s" + e + "e" + a + "a-true.ltl");
					v.verify(automaton);
				}
			}
		}
		
	}

}
