package apros;

/**
 * (c) Igor Buzhinsky
 */

import main.plant.PlantBuilderMain;
import meta.Author;
import org.apache.commons.lang3.tuple.Pair;
import structures.moore.MooreNode;
import structures.moore.MooreTransition;
import structures.moore.NondetMooreAutomaton;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CompositionalBuilder {
    // Block-based composition
    /*final static Configuration CONF_REAC = Configuration.load(Settings.CONF_LOCATION + "reactor.conf");
    final static Configuration CONF_PRESSURIZER = Configuration.load(Settings.CONF_LOCATION + "pressurizer.conf");
    final static Configuration CONF_MISC = Configuration.load(Settings.CONF_LOCATION + "misc.conf");
    final static List<Configuration> CONF_STRUCTURE =
            Arrays.asList(CONF_PRESSURIZER, CONF_REAC, CONF_MISC);
    // Control diagram-based composition
    final static List<Configuration> CONF_NETWORK = Arrays.asList(
            TraceTranslator.CONF_S1,
            TraceTranslator.CONF_S2,
            TraceTranslator.CONF_S4);
    //final static List<Configuration> CONFS = CONF_STRUCTURE;
    final static List<Configuration> CONFS = Arrays.asList(Settings.CONF);*/

    final static boolean PROXIMITY_COMPLETION = true;
    final static boolean ALL_EVENT_COMBINATIONS = false;
    final static boolean ENSURE_COMPLETENESS = true;

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
                                final MooreTransition tCopy = new MooreTransition(state,
                                        tOther.dst(), t.event());
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

    // assuming that we have at most 10 intervals
    static boolean isProperAction(String action, String prefix) {
        return action.startsWith(prefix)
                && action.length() == prefix.length() + 1
                && Character.isDigit(action.charAt(action.length() - 1));
    }

    static int actionIntervalIndex(String[] actions, String prefix) {
        int i;
        for (i = 0; i < actions.length; i++) {
            if (isProperAction(actions[i], prefix)) {
                break;
            }
        }
        return Integer.parseInt(String.valueOf(actions[i].charAt(actions[i].length() - 1)));
    }

    static boolean isConsistentWithInputs(MooreNode node, String outgoingEvent,
            Match match, boolean isOutputInput) {
        if (isOutputInput) {
            for (Pair<Parameter, Integer> pair : match.outputInputPairs) {
                final int firstIndex = actionIntervalIndex(node.actions().getActions(),
                        pair.getLeft().traceName());
                final int secondIndex = Integer.parseInt(String.valueOf(
                        outgoingEvent.charAt(pair.getRight() + 1)));
                if (firstIndex != secondIndex) {
                    return false;
                }
            }
        } else {
            for (Pair<Integer, Parameter> pair : match.inputOutputPairs) {
                final int secondIndex = actionIntervalIndex(node.actions().getActions(),
                        pair.getRight().traceName());
                final int firstIndex = Integer.parseInt(String.valueOf(
                        outgoingEvent.charAt(pair.getLeft() + 1)));
                if (firstIndex != secondIndex) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Configuration composeConfigurations(Configuration c1, Configuration c2, Match match) {
        // outputs
        final Map<String, Parameter> traceNameToParam = new TreeMap<>();
        final Consumer<Parameter> process = p -> {
            traceNameToParam.putIfAbsent(p.traceName(), p);
        };
        c1.outputParameters.forEach(process);
        c2.outputParameters.forEach(process);
        match.badActionPrefixes.forEach(traceNameToParam::remove);
        final List<Parameter> outputs = new ArrayList<>(traceNameToParam.values());
        
        // inputs
        final List<Parameter> inputs = new ArrayList<>();
        for (int i = 0; i < c1.inputParameters.size(); i++) {
            if (!match.badFirstIndices.contains(i)) {
                inputs.add(c1.inputParameters.get(i));
            }
        }
        for (int i = 0; i < c2.inputParameters.size(); i++) {
            if (!match.badSecondIndices.contains(i)) {
                inputs.add(c2.inputParameters.get(i));
            }
        }
        
        return new Configuration(c1.intervalSec, outputs, inputs);
    }

    private static String joinEvents(String e1, String e2, Match match) {
        final StringBuilder event = new StringBuilder("A");
        for (int i = 1; i < e1.length(); i++) {
            if (!match.badFirstIndices.contains(i - 1)) {
                event.append(e1.charAt(i));
            }
        }
        for (int i = 1; i < e2.length(); i++) {
            if (!match.badSecondIndices.contains(i - 1)) {
                event.append(e2.charAt(i));
            }
        }
        return event.toString();
    }

    private static NondetMooreAutomaton compose(NondetMooreAutomaton a1, NondetMooreAutomaton a2,
            Match match, Set<List<String>> allActionCombinationsSorted) throws FileNotFoundException {
        final List<MooreNode> compositeStates = new ArrayList<>();
        
        final Deque<Pair<StatePair, MooreNode>> q = new ArrayDeque<>();
        final Map<Set<String>, MooreNode> allEnqueuedOutputCombinations = new HashMap<>();
        for (int initial1 : a1.initialStates()) {
            for (int initial2 : a2.initialStates()) {
                final MooreNode state1 = a1.state(initial1);
                final MooreNode state2 = a2.state(initial2);
                final StatePair p = new StatePair(state1, state2);
                if (p.isConsistent(match) && p.isPresentInTraces(allActionCombinationsSorted)) {
                    final MooreNode node = p.toMooreNode(compositeStates.size(), match);
                    allEnqueuedOutputCombinations.put(p.actionSet(match), node);
                    q.add(Pair.of(p, node));
                    compositeStates.add(node);
                }
            }
        }
        final int initialStateNum = q.size();
        
        final List<String> allEventsList = new ArrayList<>();
        final StatePair firstPair = q.getFirst().getLeft();
        for (MooreTransition t1 : firstPair.first.transitions()) {
            for (MooreTransition t2 : firstPair.second.transitions()) {
                allEventsList.add(joinEvents(t1.event(), t2.event(), match));
            }
        }
        
        while (!q.isEmpty()) {
            final Pair<StatePair, MooreNode> retrieved = q.removeLast();
            final StatePair pair = retrieved.getLeft();
            final MooreNode src = retrieved.getRight();
            
            final Set<String> allEvents = new TreeSet<>(allEventsList);
            final Map<String, StatePair> potentialTransitions = new TreeMap<>();

            final BiConsumer<StatePair, String> add = (p, event) -> {
                final Set<String> actionSet = p.actionSet(match);
                MooreNode dst = allEnqueuedOutputCombinations.get(actionSet);
                if (dst == null) {
                    dst = p.toMooreNode(compositeStates.size(), match);
                    q.add(Pair.of(p, dst));
                    compositeStates.add(dst);
                    allEnqueuedOutputCombinations.put(actionSet, dst);
                }
                
                if (!src.allDst(event).contains(dst)) {
                    src.addTransition(event.toString(), dst);
                }
            };
            
            for (MooreTransition t1 : pair.first.transitions()) {
                final String e1 = t1.event();
                
                l: for (MooreTransition t2 : pair.second.transitions()) {
                    final String e2 = t2.event();

                    // **** The event must be consistent with the NEXT state
                    // **** If it is required to be consistent with the FIRST state,
                    // **** then there are some semantic problems
                    
                    // internal connection consistency
                    final String event = joinEvents(e1, e2, match);
                    final StatePair p = new StatePair(t1.dst(), t2.dst());
                    
                    if (!isConsistentWithInputs(t2.dst(), e1, match, false)) {
                        continue;
                    }
                    if (!isConsistentWithInputs(t1.dst(), e2, match, true)) {
                        continue;
                    }
                    
                    for (Pair<Integer, Integer> ip : match.inputPairs) {
                        if (e1.charAt(ip.getLeft() + 1) != e2.charAt(ip.getRight() + 1)) {
                            continue l;
                        }
                    }
                    
                    if (!p.isConsistent(match)) {
                        continue;
                    }

                    potentialTransitions.put(event, p);
                    
                    if (p.isPresentInTraces(allActionCombinationsSorted)) {
                        add.accept(p, event);
                        allEvents.remove(event);
                    }
                }
            }
            if (ENSURE_COMPLETENESS) {
                if (!allEvents.isEmpty()) {
                    System.err.println("Missing transitions: " + allEvents.size());
                }
                for (String e : allEvents) {
                    if (potentialTransitions.containsKey(e)) {
                        add.accept(potentialTransitions.get(e), e);
                        System.err.println("Expanding state space beyond traces...");
                    } else {
                        add.accept(pair, e);
                        System.err.println("Adding self-loop...");
                    }
                }
            }
        }

        /*
         * queue Q <- all consistent pairs of initial states
         * while !Q.isEmpty()
         *   q = (q_1, q_2) <- Q
         *   foreach pair of consistent outgoing inputs
         *      if there is no internal connection conflict
         *         (current output and outgoing transition input
         *         AND the destination present in the entire trace set
         *           Q <- q
         * remove internal connections (duplicate inputs and outputs)
         */
        
        final List<Boolean> isInitial = new ArrayList<>();
        isInitial.addAll(Collections.nCopies(initialStateNum, true));
        isInitial.addAll(Collections.nCopies(compositeStates.size() - initialStateNum, false));
        return new NondetMooreAutomaton(compositeStates, isInitial);
    }

    private static Configuration outputConfigurationComposition(Configuration c1, Configuration c2) {
        final List<Parameter> outputParams = new ArrayList<>(c1.outputParameters);
        final Set<String> allParamNames = new HashSet<>();
        for (Parameter p : c1.outputParameters) {
            allParamNames.add(p.aprosName());
        }
        for (Parameter p : c2.outputParameters) {
            if (allParamNames.add(p.aprosName())) {
                outputParams.add(p);
            }
        }
        return new Configuration(c1.intervalSec, outputParams, Collections.emptyList());
    }

    static class Match {
        final List<Pair<Parameter, Parameter>> outputPairs = new ArrayList<>();
        final List<Pair<Integer, Integer>> inputPairs = new ArrayList<>();
        final List<Pair<Integer, Parameter>> inputOutputPairs = new ArrayList<>();
        final List<Pair<Parameter, Integer>> outputInputPairs = new ArrayList<>();
        final Set<Integer> badFirstIndices = new TreeSet<>();
        final Set<Integer> badSecondIndices = new TreeSet<>();
        final Set<String> badActionPrefixes = new TreeSet<>();

        Match(Configuration c1, Configuration c2) {
            for (Parameter p : c1.outputParameters) {
                for (Parameter q : c2.outputParameters) {
                    if (Parameter.unify(p, q)) {
                        outputPairs.add(Pair.of(p, q));
                    }
                }
            }
            for (int i = 0; i < c1.inputParameters.size(); i++) {
                final Parameter p = c1.inputParameters.get(i);
                for (int j = 0; j < c2.inputParameters.size(); j++) {
                    final Parameter q = c2.inputParameters.get(j);
                    if (Parameter.unify(p, q)) {
                        inputPairs.add(Pair.of(i, j));
                    }
                }
            }
            
            for (Parameter p : c1.outputParameters) {
                for (int j = 0; j < c2.inputParameters.size(); j++) {
                    final Parameter q = c2.inputParameters.get(j);
                    if (Parameter.unify(p, q)) {
                        outputInputPairs.add(Pair.of(p, j));
                    }
                }
            }
            
            for (int i = 0; i < c1.inputParameters.size(); i++) {
                final Parameter p = c1.inputParameters.get(i);
                for (Parameter q : c2.outputParameters) {
                    if (Parameter.unify(p, q)) {
                        inputOutputPairs.add(Pair.of(i, q));
                    }
                }
            }
            
            // internal connections
            for (Pair<Integer, Parameter> iop : inputOutputPairs) {
                badFirstIndices.add(iop.getLeft());
            }
            // duplications
            for (Pair<Integer, Integer> ip : inputPairs) {
                badSecondIndices.add(ip.getRight());
            }
            // internal connections
            for (Pair<Parameter, Integer> oip : outputInputPairs) {
                badSecondIndices.add(oip.getRight());
            }
            
            for (Pair<Parameter, Integer> p : outputInputPairs) {
                badActionPrefixes.add(p.getLeft().traceName());
            }
            for (Pair<Integer, Parameter> p : inputOutputPairs) {
                badActionPrefixes.add(p.getRight().traceName());
            }
        }
    }

    public static void run(List<Configuration> confs, String directory, String datasetFilename, boolean satBased,
                           int traceIncludeEach, double traceFraction) throws IOException {
        // 1. Unification of all configuration pairs:
        System.out.println("*** UNIFICATION");
        for (int i = 0; i < confs.size(); i++) {
            for (int j = 0; j < i; j++) {
                new Match(confs.get(i), confs.get(j));
            }
        }
        if (confs.stream().map(s -> s.intervalSec).distinct().count() > 1) {
            System.err.println("Incompatible intervals, stopping.");
            return;
        }
        
        // 2. Load the dataset
        System.out.println("*** LOADING THE DATASET");
        final Dataset ds = Dataset.load(Utils.combinePaths(directory, datasetFilename));
        
        // 3. Build all the basic plants
        final List<NondetMooreAutomaton> automata = new ArrayList<>();
        for (int i = 0; i < confs.size(); i++) {
            System.out.println("*** BUILDING BASIC PLANTS, STAGE " + (i + 1));
            final Configuration conf = confs.get(i);
            System.out.println(conf);
            System.out.println();
            final String namePrefix = "automaton" + i + ".";

            final List<String> params = TraceTranslator.generateScenarios(conf, directory, ds, new HashSet<>(),
                    Utils.combinePaths(directory, namePrefix + "gv"), Utils.combinePaths(directory, namePrefix + "smv"),
                    false, satBased, ALL_EVENT_COMBINATIONS, traceIncludeEach, traceFraction);
            System.out.println();
            final PlantBuilderMain builder = new PlantBuilderMain();
            builder.run(params.toArray(new String[params.size()]), Author.IB, "");
            if (!builder.resultAutomaton().isPresent()) {
                System.err.println("Basic plant model constuction failed; "
                        + "is the number of states sufficient?");
                return;
            }
            final NondetMooreAutomaton a = builder.resultAutomaton().get();
            dumpAutomaton(a, conf, directory, namePrefix, builder.colorRuleMap());
            automata.add(a);
            System.out.println();
        }
        
        // 4. Iteratively compose automata
        Configuration lastConf = confs.get(0);
        NondetMooreAutomaton lastAuto = automata.get(0);
        for (int i = 1; i < confs.size(); i++) {
            System.out.println("*** COMPOSING, STAGE " + i);
            final Configuration conf1 = lastConf;
            final Configuration conf2 = confs.get(i);
            final Match match = new Match(conf1, conf2);
            final String namePrefix = "automaton_comp" + i + ".";
            
            // Obtain the set of all possible composite actions
            final Set<List<String>> allActionCombinations = new HashSet<>();
            TraceTranslator.generateScenarios(outputConfigurationComposition(conf1, conf2),
                    directory, ds, allActionCombinations, "", "", false, satBased,
                    ALL_EVENT_COMBINATIONS, traceIncludeEach, traceFraction);
            final Set<List<String>> allActionCombinationsSorted = new HashSet<>();
            for (List<String> actionCombination : allActionCombinations) {
                final List<String> copy = new ArrayList<>(actionCombination);
                Collections.sort(copy);
                allActionCombinationsSorted.add(copy);
            }

            // Compose
            System.out.println();
            System.out.println("Composing...");
            lastAuto = compose(lastAuto, automata.get(i), match,
                    allActionCombinationsSorted);
            lastConf = composeConfigurations(conf1, conf2, match);
            
            dumpAutomaton(lastAuto, lastConf, directory, namePrefix, Collections.emptyMap());
            System.out.println(lastConf);
            System.out.println();
        }
    }

    // assuming completeness and checking only state 0
    private static List<String> eventsFromAutomaton(NondetMooreAutomaton a) {
        final Set<String> result = new TreeSet<>();
        for (MooreTransition t : a.state(0).transitions()) {
            result.add(t.event());
        }
        return new ArrayList<>(result);
    }

    private static void dumpAutomaton(NondetMooreAutomaton a, Configuration conf,
            String directory, String namePrefix, Map<String, String> colorRules) throws FileNotFoundException {
        NondetMooreAutomaton effectiveA = a;
        if (PROXIMITY_COMPLETION) {
            effectiveA = proximityBasedCompletion(effectiveA, conf);
        }
        /*if (EVOLUTIONARY_NUSMV_OPTIMIZATION) {
            effectiveA = new EvolutionaryNuSMVOptimizer(effectiveA,
                    eventsFromAutomaton(effectiveA), conf).run();
        }*/

        System.out.printf("Fraction of loops in the automaton: %.2f\n",
                effectiveA.loopFraction());
        System.out.printf("Fraction of unsupported transitions in the automaton: %.2f\n",
                effectiveA.unsupportedTransitionFraction());
        System.out.printf("Number of states: %d\n", effectiveA.states().size());
        System.out.printf("Number of supported transitions: %d\n",
                (int) Math.round(effectiveA.transitionNumber() * (1 - effectiveA.unsupportedTransitionFraction())));

        Utils.writeToFile(Utils.combinePaths(directory, namePrefix + "gv"),
                          effectiveA.toString(colorRules, Optional.of(conf)));

        // reduced GV file with transitions merged for different inputs
        Utils.writeToFile(Utils.combinePaths(directory, namePrefix + "reduced." + "gv"),
                          effectiveA.simplify().toString(colorRules, Optional.of(conf)));
        
        Utils.writeToFile(Utils.combinePaths(directory, namePrefix + "smv"),
                          effectiveA.toNuSMVString(eventsFromAutomaton(a),
                              conf.actions(), new ArrayList<>(), Optional.of(conf)));
    }
}
