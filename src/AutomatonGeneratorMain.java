import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import algorithms.AutomatonGenerator;

import structures.Automaton;

public class AutomatonGeneratorMain {

	@Option(name = "--size", aliases = { "-s" }, usage = "EFSM size", metaVar = "<size>", required = true)
	private int size;

	@Option(name = "--eventsCount", aliases = { "-ec" }, usage = "used events count", metaVar = "<cnt>", required = true)
	private int eventsCount;

	@Option(name = "--varsCount", aliases = { "-vc" }, usage = "used variables count", metaVar = "<cnt>", required = true)
	private int varsCount;

	@Option(name = "--actionsCount", aliases = { "-ac" }, usage = "used actions count", metaVar = "<cnt>", required = true)
	private int actionsCount;

	@Option(name = "--minActions", aliases = { "-mina" }, usage = "minimum transition actions count", metaVar = "<cnt>")
	private int minActionsCount = 0;

	@Option(name = "--maxActions", aliases = { "-maxa" }, usage = "maximum transition actions count", metaVar = "<cnt>")
	private int maxActionsCount = 2;

	@Option(name = "--transitionsPersent", aliases = { "-p" }, usage = "transitions persent", metaVar = "<persent>")
	private int transitionsPersent = 50;

	@Option(name = "--randseed", aliases = { "-rs" }, usage = "random seed", metaVar = "<seed>")
	private int randseed;

	@Option(name = "--output", aliases = { "-o" }, usage = "filepath to write EFSM in GV format", metaVar = "<filepath>")
	private String filepath;

	private void launcher(String[] args) {

		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("Random extended finite state machine (EFSM) generator");
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

		Automaton automaton = AutomatonGenerator.generate(size, eventsCount, actionsCount, minActionsCount,
				maxActionsCount, varsCount, transitionsPersent / 100., random);

		if (filepath != null) {
			try {
				PrintWriter pw = new PrintWriter(new File(filepath));
				pw.println(automaton);
				pw.close();
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
		new AutomatonGeneratorMain().run(args);
	}

}
