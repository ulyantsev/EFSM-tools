package main;

import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Option;

import algorithms.AutomatonGenerator;
import structures.mealy.MealyAutomaton;

public class AutomatonGeneratorMain extends MainBase {
	@Option(name = "--size", aliases = { "-s" },
            usage = "EFSM size", metaVar = "<size>", required = true)
	private int size;

	@Option(name = "--eventsCount", aliases = { "-ec" },
            usage = "number of events", metaVar = "<cnt>", required = true)
	private int eventsCount;

	@Option(name = "--varsCount", aliases = { "-vc" },
            usage = "number of variables", metaVar = "<cnt>", required = true)
	private int varsCount;

	@Option(name = "--actionsCount", aliases = { "-ac" },
            usage = "number of actions", metaVar = "<cnt>", required = true)
	private int actionsCount;

	@Option(name = "--minActions", aliases = { "-mina" },
            usage = "minimum number of actions on transitions", metaVar = "<cnt>")
	private int minActionsCount = 0;

	@Option(name = "--maxActions", aliases = { "-maxa" },
            usage = "maximum number of actions on transitions", metaVar = "<cnt>")
	private int maxActionsCount = 2;

	@Option(name = "--transitionsPersent", aliases = { "-p" },
            usage = "transitions percentage", metaVar = "<percentage>")
	private int transitionsPercentage = 50;

	@Option(name = "--randseed", aliases = { "-rs" },
            usage = "random seed", metaVar = "<seed>")
	private int randseed;

	@Option(name = "--output", aliases = { "-o" },
            usage = "filepath to write EFSM in GV format", metaVar = "<filepath>")
	private String filepath;

    public static void main(String[] args) {
        new AutomatonGeneratorMain().run(args, Author.VU, "Random extended finite state machine (EFSM) generator");
    }

    @Override
	protected void launcher() {
        initializeRandom(randseed);
		final MealyAutomaton automaton = AutomatonGenerator.generate(
                size, eventsCount, actionsCount, minActionsCount,
				maxActionsCount, varsCount, transitionsPercentage / 100., random());
        if (filepath == null) {
            System.out.println(automaton);
        } else {
            saveToFile(automaton, filepath);
        }
	}
}
