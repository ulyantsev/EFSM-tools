import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

import scenario.StringScenario;
import structures.Automaton;
import tools.AutomatonGVLoader;

public class ConsistencyCheckerMain {
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        
        if (args.length != 2) {
            System.out.println("Tool for checking EFSM and scenarios set consistency");
            System.out.println("Author: Vladimir Ulyantsev, ulyantsev@rain.ifmo.ru\n");
            System.out.println("Usage: java -jar checker.jar <efsm.gv> <scenarios>");
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
        
        
        List<StringScenario> scenarios;
        try {
            scenarios = StringScenario.loadScenarios(args[1]);
        } catch (FileNotFoundException e) {
            System.err.println("Can't open file " + args[1]);
            e.printStackTrace();
            return;
        } catch (ParseException e) {
            System.err.println("Can't read scenarios from file " + args[1]);
            e.printStackTrace();
            return;
        }
        
        int faultCount = 0;
        int actionsMistakes = 0;
        int scenariosSumLength = 0;
        for (StringScenario scenario : scenarios) {
            faultCount += automaton.isCompliesWithScenario(scenario) ? 0 : 1;
            actionsMistakes += automaton.calcMissedActions(scenario);
            scenariosSumLength += scenario.size();
        }
        
        System.out.println("Total scenarios count: " + scenarios.size());
        System.out.println("Total scenarios length: " + scenariosSumLength);
        System.out.println("Complies with: " + (scenarios.size() - faultCount));
        System.out.println("Incomplies with: " + faultCount);
        System.out.printf("Complies percent: %.2f\n\n", 100. * (scenarios.size() - faultCount) / scenarios.size());
        System.out.println("Total actions mistakes done: " + actionsMistakes);
        System.out.printf("Actions mistakes percent: %.2f\n", 
                                100. * actionsMistakes / scenariosSumLength);
        
    }
}
