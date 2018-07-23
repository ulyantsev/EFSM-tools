package continuous_trace_builders;

/**
 * (c) Igor Buzhinsky
 */

import main.plant.PlantBuilderMain;
import meta.Author;
import structures.moore.MooreNode;
import structures.moore.MooreTransition;
import structures.moore.NondetMooreAutomaton;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ExplicitStateBuilder {
    private final static boolean ALL_EVENT_COMBINATIONS = false;

    private static double proximity(String e1, String e2, Configuration conf) {
        double sum = 0;
        for (int i = 0; i < conf.inputParameters.size(); i++) {
            final int v1 = Integer.parseInt(String.valueOf(e1.charAt(i + 1)));
            final int v2 = Integer.parseInt(String.valueOf(e2.charAt(i + 1)));
            final int intDiff = Math.abs(v1 - v2);
            final double scaledDiff = (double) intDiff / (conf.inputParameters.get(i).valueCount() - 1);
            sum += scaledDiff;
        }
        return sum / conf.inputParameters.size();
    }

    private static NondetMooreAutomaton proximityBasedCompletion(NondetMooreAutomaton a, Configuration conf) {
        // NOTE: can increase the total number of transitions!
        final NondetMooreAutomaton res = a.copy();
        int redirected = 0;
        for (MooreNode state : res.states()) {
            final List<MooreTransition> list = new ArrayList<>(state.transitions());
            for (MooreTransition t : list) {
                if (res.unsupportedTransitions().contains(t)) {
                    String closestEvent = null;
                    // use the destination of the closest other supported transition
                    double bestProximity = Double.MAX_VALUE;
                    for (MooreTransition tOther : list) {
                        if (!res.unsupportedTransitions().contains(tOther)) {
                            final double p = proximity(t.event(), tOther.event(), conf);
                            if (p < bestProximity) {
                                bestProximity = p;
                                closestEvent = tOther.event();
                            }
                        }
                    }

                    if (closestEvent != null) {
                        res.removeTransition(state, t);
                        res.unsupportedTransitions().remove(t);
                        for (MooreTransition tOther : list) {
                            if (tOther.event().equals(closestEvent)) {
                                final MooreTransition tCopy = new MooreTransition(state, tOther.dst(), t.event());
                                res.addTransition(state, tCopy);
                                res.unsupportedTransitions().add(tCopy);
                                redirected++;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Transitions redirected based on proximity: " + redirected);
        return res;
    }

    public static void run(Configuration conf, String directory, String datasetFilename, boolean satBased,
                           int traceIncludeEach, double traceFraction, boolean proximityCompletion,
                           boolean outputGv, boolean outputSmv, boolean outputSpin, boolean timedConstraints,
                           boolean makeAllStatesInitial)
            throws IOException {
        final Dataset ds = Dataset.load(Utils.combinePaths(directory, datasetFilename));
        System.out.println(conf);
        System.out.println();
        final List<String> additionalOptions = new ArrayList<>();
        if (timedConstraints) {
            additionalOptions.add("--timedConstraints");
        }
        if (makeAllStatesInitial) {
            additionalOptions.add("--makeAllStatesInitial");
        }
        final List<String> params = TraceTranslator.generateScenarios(conf, directory, ds, new HashSet<>(),
                "", "", false, satBased, ALL_EVENT_COMBINATIONS, traceIncludeEach, traceFraction,
                additionalOptions.toArray(new String[0]));
        System.out.println();
        final PlantBuilderMain builder = new PlantBuilderMain();
        builder.run(params.toArray(new String[0]), Author.IB, "");
        if (!builder.resultAutomaton().isPresent()) {
            throw new RuntimeException("No automaton found!");
        }
        final NondetMooreAutomaton a = builder.resultAutomaton().get();
        dumpAutomaton(a, conf, directory, "plant-explicit.", proximityCompletion, outputGv, outputSmv, outputSpin);
    }

    static void dumpAutomaton(NondetMooreAutomaton a, Configuration conf, String directory, String namePrefix,
                              boolean proximityCompletion, boolean outputGv, boolean outputSmv, boolean outputSpin)
            throws IOException {
        NondetMooreAutomaton effectiveA = a;
        if (proximityCompletion) {
            System.out.println("Proximity-based transition redirection...");
            effectiveA = proximityBasedCompletion(effectiveA, conf);
        }

        dumpProperties(effectiveA);

        if (outputGv) {
            System.out.println("GraphViz output...");
            try (BufferedWriter out = new BufferedWriter(new FileWriter(Utils.combinePaths(directory,
                    namePrefix + "gv")))) {
                effectiveA.toString(conf, new PrintWriter(out));
            }
            // reduced GV file with transitions merged for different inputs
            System.out.println("Reduced GraphViz output...");
            try (BufferedWriter out = new BufferedWriter(new FileWriter(Utils.combinePaths(directory,
                    namePrefix + "reduced." + "gv")))) {
                effectiveA.simplify().toString(conf, new PrintWriter(out));
            }
        }
        if (outputSmv) {
            System.out.println("NuSMV output...");
            try (BufferedWriter out = new BufferedWriter(new FileWriter(Utils.combinePaths(directory,
                    namePrefix + "smv")))) {
                effectiveA.toNuSMVString(eventsFromAutomaton(a), conf.actions(), conf, new PrintWriter(out));
            }
        }
        if (outputSpin) {
            System.out.println("Promela output...");
            try (BufferedWriter out = new BufferedWriter(new FileWriter(Utils.combinePaths(directory,
                    namePrefix + "pml")))) {
                effectiveA.toSPINString(eventsFromAutomaton(a), conf.actions(), conf, new PrintWriter(out));
            }
        }
    }

    private static void dumpProperties(NondetMooreAutomaton a) {
        final int nStates = a.states().size();
        final int nTrans = a.transitionNumber();
        final int nTransUnsup = a.unsupportedTransitions().size();
        final int nTransSup = nTrans - nTransUnsup;
        System.out.println("Number of states: " +  nStates);
        System.out.println("Number of supported transitions: " + nTransSup);
        System.out.println("Fraction of supported transitions: " + (float) ((double) nTransSup / nTrans));
    }

    // assuming completeness and checking only state 0
    private static List<String> eventsFromAutomaton(NondetMooreAutomaton a) {
        return new ArrayList<>(a.state(0).transitions().stream().map(MooreTransition::event)
                .collect(Collectors.toCollection(TreeSet::new)));
    }
}
