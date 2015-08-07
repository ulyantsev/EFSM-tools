import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Random;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import structures.Automaton;
import algorithms.AutomatonGVLoader;
import algorithms.ScenarioGenerator;

public class ScenariosGeneratorMain {

	@Option(name = "--automaton", aliases = { "-a" }, usage = "given EFSM in GV format", metaVar = "<fp>", required = true)
	private String automatonFilepath;

	@Option(name = "--count", aliases = { "-cnt" }, usage = "scenarios count to generate", metaVar = "<cnt>")
	private int scenariosCount;

	@Option(name = "--output", aliases = { "-o" }, usage = "file to write scenarios", metaVar = "<fp>")
	private String scenariosFilepath;

	@Option(name = "--randseed", aliases = { "-rs" }, usage = "random seed", metaVar = "<seed>")
	private int randseed;

	@Option(name = "--minLength", aliases = { "-minl" }, usage = "minimum scenario length", metaVar = "<min>")
	private int minLength;

	@Option(name = "--maxLength", aliases = { "-maxl" }, usage = "maximum scenario length", metaVar = "<max>")
	private int maxLength;

	@Option(name = "--sumLength", aliases = { "-suml" }, usage = "summary scenarios length", metaVar = "<sum>")
	private int sumLength;

	@Option(name = "--cover", aliases = { "-c" }, handler = BooleanOptionHandler.class, usage = "BFS-based generation")
	private boolean cover;

	private void launcher(String[] args) {

		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("Random scenarios generator from given extended finite state machine (EFSM)");
			System.out.println("Author: Vladimir Ulyantsev (ulyantsev@rain.ifmo.ru)\n");
			System.out.print("Usage: ");
			parser.printSingleLineUsage(System.out);
			System.out.println();
			parser.printUsage(System.out);
			return;
		}

		Random random;
		if (randseed == 0) {
			random = new Random();
		} else {
			random = new Random(randseed);
		}

		Automaton automaton;
		try {
			automaton = AutomatonGVLoader.load(automatonFilepath);
		} catch (IOException e) {
			System.err.println("Can't open file " + automatonFilepath);
			e.printStackTrace();
			return;
		} catch (ParseException e) {
			System.err.println("Can't read EFSM from file " + automatonFilepath);
			e.printStackTrace();
			return;
		}
		
		String scenarios = null;
		if (cover) {
			if (scenariosCount != 0 || minLength != 0 || maxLength != 0) {
				System.err.println("With --cover option on --count, --minLength, --maxLength options not available");
				return;
			}
			
			if (sumLength == 0) {
				scenarios = ScenarioGenerator.generateScenariosWithBFS(automaton);
			} else {
				scenarios = ScenarioGenerator.generateScenariosWithBFS(automaton, sumLength, random);
			}			
		} else {
			if (scenariosCount == 0) {
				System.err.println("With --cover option OFF --count option must be defined");
				return;				
			}
			
			if (maxLength == 0) {
				maxLength = automaton.statesCount();
			}

			if (sumLength == 0) {
				sumLength = (maxLength + minLength) * scenariosCount / 2;
			}

			scenarios = ScenarioGenerator.generateScenarios(automaton, scenariosCount, minLength, maxLength,
					sumLength, random);
		}
		
		if (scenariosFilepath != null) {
			try {
				PrintWriter pw = new PrintWriter(new File(scenariosFilepath));
				pw.println(scenarios);
				pw.close();
			} catch (IOException e) {
				System.err.println("ERROR: Problems with writing to file " + scenariosFilepath);
				e.printStackTrace();
			}
		} else {
			System.out.println(scenarios);
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
		new ScenariosGeneratorMain().run(args);

	}
}
