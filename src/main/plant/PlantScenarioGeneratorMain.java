package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Option;

import scenario.StringActions;
import scenario.StringScenario;
import structures.plant.MooreTransition;
import structures.plant.NondetMooreAutomaton;
import bool.MyBooleanExpression;

public class PlantScenarioGeneratorMain extends MainBase {
	@Option(name = "--automaton", aliases = { "-a" }, usage = "plant model", metaVar = "<fp>", required = true)
	private String automatonFilepath;

	@Option(name = "--number", aliases = { "-cnt" }, usage = "number of scenarios to generate", metaVar = "<num>")
	private int scenarioNumber;

	@Option(name = "--output", aliases = { "-o" }, usage = "output file", metaVar = "<fp>")
	private String scenarioFilepath;

	@Option(name = "--randseed", aliases = { "-rs" }, usage = "random seed", metaVar = "<seed>")
	private int randseed;

	@Option(name = "--minLength", aliases = { "-minl" }, usage = "minimum scenario length", metaVar = "<min>")
	private int minLength;

	@Option(name = "--maxLength", aliases = { "-maxl" }, usage = "maximum scenario length", metaVar = "<max>")
	private int maxLength;

    public static void main(String[] args) {
        new PlantScenarioGeneratorMain().run(args, Author.IB, "Scenario generator for plant models");
    }

    @Override
    protected void launcher() throws FileNotFoundException {
        initializeRandom(randseed);
		final NondetMooreAutomaton automaton = NondetMooreAutomaton.readGV(automatonFilepath);
		final List<StringScenario> scenarios = new ArrayList<>();
		
		for (int i = 0; i < scenarioNumber; i++) {
			final List<Integer> startStates = automaton.initialStates();
			final int startState = startStates.get(random().nextInt(startStates.size()));
			final int length = minLength + random().nextInt(maxLength - minLength + 1);
			final List<String> events = new ArrayList<>();
			final List<MyBooleanExpression> expressions = new ArrayList<>();
			final List<StringActions> actions = new ArrayList<>();
			events.add("");
			actions.add(automaton.state(startState).actions());
			expressions.add(MyBooleanExpression.getTautology());
			int curState = startState;
			for (int j = 0; j < length; j++) {
				final List<MooreTransition> transitions
					= new ArrayList<>(automaton.state(curState).transitions());
				final MooreTransition transition
					= transitions.get(random().nextInt(transitions.size()));
				events.add(transition.event());
				actions.add(transition.dst().actions());
				expressions.add(MyBooleanExpression.getTautology());
				curState = transition.dst().number();
			}
			scenarios.add(new StringScenario(true, events, expressions, actions));
		}

		final String scenarioString = String.join("\n\n",
				scenarios.stream().map(Object::toString).collect(Collectors.toList()));
		
		if (scenarioFilepath != null) {
            saveToFile(scenarioString, scenarioFilepath);
		} else {
			System.out.println(scenarioString);
		}
	}
}
