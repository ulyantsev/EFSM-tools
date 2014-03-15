import java.io.IOException;
import java.text.ParseException;

import structures.Automaton;
import tools.AutomatonCompletenessChecker;
import tools.AutomatonGVLoader;

public class CompletenessCheckerMain {

	public static void main(String[] args) {
    	if (args.length != 1) {
            System.out.println("Tool for checking EFSM variables completeness");
            System.out.println("Author: Vladimir Ulyantsev, ulyantsev@rain.ifmo.ru\n");
            System.out.println("Usage: java -jar checker.jar <efsm.gv>");
            return;
        }

        Automaton automaton;
        try {
            automaton = AutomatonGVLoader.load(args[0]);
        } catch (IOException e) {
            System.err.println("Can't open file " + args[0]);
            e.printStackTrace();
            return;
        } catch (ParseException e) {
            System.err.println("Can't read EFSM from file " + args[0]);
            e.printStackTrace();
            return;
        }

        System.out.println(AutomatonCompletenessChecker.checkCompleteness(automaton));       
	}

}
