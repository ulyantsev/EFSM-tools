package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import scenario.StringActions;
import structures.plant.NondetMooreAutomaton;

public class PlantAutomatonGeneratorMain {
	@Option(name = "--size", aliases = { "-s" }, usage = "size", metaVar = "<size>", required = true)
	private int size;

	@Option(name = "--eventNumber", aliases = { "-en" }, usage = "used events number", metaVar = "<num>", required = true)
	private int eventNumber;

	@Option(name = "--actionNumber", aliases = { "-an" }, usage = "used actions number", metaVar = "<num>", required = true)
	private int actionNumber;

	@Option(name = "--initialPercentage", aliases = { "-ip" }, usage = "initial state percentage", metaVar = "<num>")
	private int initialPercentage = 25;

	@Option(name = "--actionPercentage", aliases = { "-ap" }, usage = "action percentage", metaVar = "<num>")
	private int actionPercentage = 25;

	@Option(name = "--transitionsPercentage", aliases = { "-tp" }, usage = "transitions percentage", metaVar = "<num>")
	private int transitionPercentage = 20;

	@Option(name = "--randseed", aliases = { "-rs" }, usage = "random seed", metaVar = "<seed>")
	private int randseed;

	@Option(name = "--output", aliases = { "-o" }, usage = "filepath to write the automaton in the GV format", metaVar = "<filepath>")
	private String filepath;

	private void launcher(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("Plant model generator");
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
		
		final List<Boolean> isStart = new ArrayList<>();
		final List<StringActions> stringActions = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			isStart.add(random.nextDouble() < initialPercentage * 0.01);
			final List<String> thisActions = new ArrayList<>();
			for (int j = 0; j < actionNumber; j++) {
				if (random.nextDouble() < actionPercentage * 0.01) {
					thisActions.add("z" + j);
				}
			}
			stringActions.add(new StringActions(String.join(", ", thisActions)));
		}
		if (!isStart.contains(true)) {
			isStart.set(0, true);
		}
		final NondetMooreAutomaton automaton = new NondetMooreAutomaton(size, stringActions, isStart);
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < eventNumber; j++) {
				final String event = String.valueOf((char)('A' + j));
				boolean atLeastOne = false;
				for (int dst = 0; dst < size; dst++) {
					if (random.nextDouble() < transitionPercentage * 0.01) {
						automaton.state(i).addTransition(event, automaton.state(dst));
						atLeastOne = true;
					}
				}
				if (!atLeastOne) {
					automaton.state(i).addTransition(event, automaton.state(random.nextInt(size)));
				}
			}
		}
		
		if (filepath != null) {
			try (PrintWriter pw = new PrintWriter(new File(filepath))) {
				pw.println(automaton);
			} catch (IOException e) {
				System.err.println("ERROR: Problems with writing to file " + filepath);
				e.printStackTrace();
			}
		} else {
			System.out.println(automaton);
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
		new PlantAutomatonGeneratorMain().run(args);
	}

}
