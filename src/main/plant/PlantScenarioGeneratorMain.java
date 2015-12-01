package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import scenario.StringActions;
import scenario.StringScenario;
import structures.plant.MooreTransition;
import structures.plant.NondetMooreAutomaton;
import bool.MyBooleanExpression;

public class PlantScenarioGeneratorMain {
	@Option(name = "--automaton", aliases = { "-a" }, usage = "plant model", metaVar = "<fp>", required = true)
	private String automatonFilepath;

	@Option(name = "--number", aliases = { "-cnt" }, usage = "number of scenarios to generate", metaVar = "<num>")
	private int scenariosNumber;

	@Option(name = "--output", aliases = { "-o" }, usage = "output file", metaVar = "<fp>")
	private String scenariosFilepath;

	@Option(name = "--randseed", aliases = { "-rs" }, usage = "random seed", metaVar = "<seed>")
	private int randseed;

	@Option(name = "--minLength", aliases = { "-minl" }, usage = "minimum scenario length", metaVar = "<min>")
	private int minLength;

	@Option(name = "--maxLength", aliases = { "-maxl" }, usage = "maximum scenario length", metaVar = "<max>")
	private int maxLength;

	private void launcher(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("Scenario generator for plant models");
			System.out.println("Author: Igor Buzhinsky (igor.buzhinsky@gmail.com)\n");
			System.out.print("Usage: ");
			parser.printSingleLineUsage(System.out);
			System.out.println();
			parser.printUsage(System.out);
			return;
		}

		final Random random;
		if (randseed == 0) {
			random = new Random();
		} else {
			random = new Random(randseed);
		}

		NondetMooreAutomaton automaton;
		try {
			automaton = NondetMooreAutomaton.readGV(automatonFilepath);
		} catch (FileNotFoundException e) {
			System.err.println("Can't open file " + automatonFilepath);
			e.printStackTrace();
			return;
		}
		
		final List<StringScenario> scenarios = new ArrayList<>();
		
		for (int i = 0; i < scenariosNumber; i++) {
			final List<Integer> startStates = automaton.startStates();
			final int startState = startStates.get(random.nextInt(startStates.size()));
			final int length = minLength + random.nextInt(maxLength - minLength + 1);
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
					= transitions.get(random.nextInt(transitions.size()));
				events.add(transition.event());
				actions.add(transition.dst().actions());
				expressions.add(MyBooleanExpression.getTautology());
				curState = transition.dst().number();
			}
			scenarios.add(new StringScenario(true, events, expressions, actions));
		}
		

		final String scenarioString = String.join("\n\n",
				scenarios.stream().map(s -> s.toString()).collect(Collectors.toList()));
		
		if (scenariosFilepath != null) {
			try (PrintWriter pw = new PrintWriter(new File(scenariosFilepath))) {
				pw.println(scenarioString);
			} catch (IOException e) {
				System.err.println("ERROR: Problems with writing to file " + scenariosFilepath);
				e.printStackTrace();
			}
		} else {
			System.out.println(scenarioString);
		}
	}

	public void run(String[] args) {
		try {
			launcher(args);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		new PlantScenarioGeneratorMain().run(args);
	}
}
