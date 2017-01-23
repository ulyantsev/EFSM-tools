package apros;

/**
 * (c) Igor Buzhinsky
 */

import main.plant.PlantBuilderMain;
import meta.Author;
import structures.moore.NondetMooreAutomaton;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class TraceEvaluationBuilder {
    final static boolean ALL_EVENT_COMBINATIONS = false;

    public static void run(Configuration conf, String directory, String datasetFilename, boolean satBased,
                           int traceIncludeEach, double traceFraction) throws IOException {
        System.out.println("*** LOADING THE DATASET");
        final Dataset ds = Dataset.load(Utils.combinePaths(directory, datasetFilename));
        System.out.println(conf);
        System.out.println();
        final String namePrefix = "automaton" + 0 + ".";
        final List<String> params = TraceTranslator.generateScenarios(conf, directory, ds, new HashSet<>(),
                Utils.combinePaths(directory, namePrefix + "gv"), Utils.combinePaths(directory, namePrefix + "smv"),
                false, satBased, ALL_EVENT_COMBINATIONS, traceIncludeEach, traceFraction);
        System.out.println();
        final PlantBuilderMain builder = new PlantBuilderMain();
        builder.run(params.toArray(new String[params.size()]), Author.IB, "");
        if (!builder.resultAutomaton().isPresent()) {
            System.err.println("No automaton found.");
            return;
        }
        final NondetMooreAutomaton a = builder.resultAutomaton().get();
        System.out.printf("Number of states: %d\n", a.states().size());
        System.out.printf("Number of supported transitions: %d\n", a.transitionNumber()
                - a.unsupportedTransitions().size());
    }
}
