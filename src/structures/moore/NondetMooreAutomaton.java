package structures.moore;

import continuous_trace_builders.Configuration;
import continuous_trace_builders.TraceModelGenerator;
import continuous_trace_builders.parameters.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import scenario.StringActions;
import scenario.StringScenario;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class NondetMooreAutomaton {
    private final List<Boolean> isInitial = new ArrayList<>();
    private final List<MooreNode> states = new ArrayList<>();
    
    private Set<MooreTransition> unsupportedTransitions = new HashSet<>();

    private List<Integer> loopConstraints = null;

    public void setLoopConstraints(List<Integer> loopConstraints) {
        this.loopConstraints = new ArrayList<>(loopConstraints);
    }

    public Set<MooreTransition> unsupportedTransitions() {
        return unsupportedTransitions;
    }

    private double transitionFraction(Predicate<MooreTransition> p) {
        int matched = 0;
        int all = 0;
        for (MooreNode state : states) {
            for (MooreTransition t : state.transitions()) {
                all++;
                matched += p.test(t) ? 1 : 0;
            }
        }
        return (double) matched / all;
    }

    public double unsupportedTransitionFraction() {
        return transitionFraction(unsupportedTransitions::contains);
    }

    public int transitionNumber() {
        return states.stream().mapToInt(s -> s.transitions().size()).sum();
    }

    public double loopFraction() {
        return transitionFraction(t -> t.dst() == t.src());
    }

    public static NondetMooreAutomaton readGV(String filename) throws FileNotFoundException {
        final Map<String, List<String>> actionRelation = new LinkedHashMap<>();
        final Map<String, List<Pair<Integer, String>>> transitionRelation = new LinkedHashMap<>();
        actionRelation.put("init", new ArrayList<>());
        transitionRelation.put("init", new ArrayList<>());
        final Set<String> events = new LinkedHashSet<>();
        final Set<String> actions = new LinkedHashSet<>();
        final Set<Integer> initial = new LinkedHashSet<>();
        
        try (Scanner sc = new Scanner(new File(filename))) {
            while (sc.hasNextLine()) {
                final String line = sc.nextLine();
                if (!line.contains(";") || line.startsWith("#")) {
                    continue;
                }
                final String tokens[] = line.split(" +");
                if (line.contains("->")) {
                    final String from = tokens[1];
                    // the replacement is needed for initial state declarations as transitions from init_n:
                    final Integer to = Integer.parseInt(tokens[3].replaceAll(";", ""));
                    if (from.equals("init" + to)) {
                        initial.add(to);
                    } else {
                        final String event = tokens[5];
                        transitionRelation.get(from).add(Pair.of(to, event));
                        events.add(event);
                    }
                } else {
                    final String from = tokens[1];
                    if (from.startsWith("init") || from.equals("node")) {
                        continue;
                    }
                    transitionRelation.put(from, new ArrayList<>());
                    final String[] labels = line.split("\"")[1].split("\\\\n");
                    final List<String> theseActions = Arrays.asList(labels).subList(1, labels.length);
                    actionRelation.put(from, theseActions);
                    actions.addAll(theseActions);
                }
            }
        }

        int maxState = 0;
        for (List<Pair<Integer, String>> list : transitionRelation.values()) {
            for (Pair<Integer, String> p : list) {
                maxState = Math.max(maxState, p.getLeft());
            }
        }
        final List<Boolean> initialVector = new ArrayList<>();
        final List<StringActions> actionVector = new ArrayList<>();
        for (int i = 0; i <= maxState; i++) {
            initialVector.add(initial.contains(i));
            actionVector.add(new StringActions(String.join(", ", actionRelation.get(i + ""))));
        }
        
        final NondetMooreAutomaton a = new NondetMooreAutomaton(maxState + 1, actionVector, initialVector);
        for (int i = 0; i <= maxState; i++) {
            for (Pair<Integer, String> p : transitionRelation.get(String.valueOf(i))) {
                a.state(i).addTransition(p.getRight(), a.state(p.getLeft()));
            }
        }
        return a;
    }

    public NondetMooreAutomaton(List<MooreNode> states, List<Boolean> isInitial) {
        this.states.addAll(states);
        this.isInitial.addAll(isInitial);
    }
    
    public NondetMooreAutomaton(int statesCount, List<StringActions> actions, List<Boolean> isInitial) {
        for (int i = 0; i < statesCount; i++) {
            states.add(new MooreNode(i, actions.get(i)));
        }
        this.isInitial.addAll(isInitial);
    }

    public boolean isInitialState(int index) {
        return isInitial.get(index);
    }
    
    public List<Integer> initialStates() {
        final List<Integer> result = new ArrayList<>();
        for (int i = 0; i < states.size(); i++) {
            if (isInitialState(i)) {
                result.add(i);
            }
        }
        return result;
    }

    public MooreNode state(int i) {
        return states.get(i);
    }

    public List<MooreNode> states() {
        return states;
    }

    public int stateCount() {
        return states.size();
    }

    public void addTransition(MooreNode state, MooreTransition transition) {
        state.addTransition(transition);
    }
    
    public void removeTransition(MooreNode state, MooreTransition transition) {
        state.removeTransition(transition);
    }

    public void toString(Configuration conf, PrintWriter pw) {
        pw.append("# generated file; view: dot -Tpng <filename> > filename.png\n"
            + "digraph Automaton {\n");

        final Map<String, String> actionDescriptions = conf != null
                ? conf.extendedActionDescriptions() : new HashMap<>();

        final String initNodes = String.join(", ",
                initialStates().stream().map(s -> "init" + s).collect(Collectors.toList()));
        
        pw.append("    ").append(initNodes).append(" [shape=point, width=0.01, height=0.01, label=\"\", color=white];\n");
        pw.append("    node [shape=box, style=rounded];\n");
        for (int i = 0; i < states.size(); i++) {
            final MooreNode state = states.get(i);

            pw.append("    ").append(String.valueOf(state.number())).append(" [label=\"")
                    .append(state.toString(actionDescriptions)).append("\"").append("]").append(";\n");
            if (isInitial.get(i)) {
                pw.append("    init").append(String.valueOf(state.number())).append(" -> ")
                        .append(String.valueOf(state.number())).append(";\n");
            }
        }
        
        for (MooreNode state : states) {
            for (MooreTransition t : state.transitions()) {
                pw.append("    ").append(String.valueOf(t.src().number())).append(" -> ")
                        .append(String.valueOf(t.dst().number())).append(" [label=\" ")
                        .append(t.event()).append(" \"];\n");
            }
        }

        pw.append("}");
    }

    public String toString(Configuration conf) {
        final StringWriter sw = new StringWriter();
        toString(conf, new PrintWriter(sw));
        return sw.toString();
    }
    
    @Override
    public String toString() {
        return toString(null);
    }

    private static void nusmvEventDescriptions(int[] arr, int index, PrintWriter result,
                                               List<Pair<String, Parameter>> thresholds, List<String> events) {
        if (index == arr.length) {
            final String event = "input_A" + Arrays.toString(arr).replaceAll("[,\\[\\] ]", "");
            if (!events.contains(event)) {
                return;
            }
            final List<String> conditions = new ArrayList<>();
            for (int i = 0; i < arr.length; i++) {
                final String paramName = thresholds.get(i).getLeft();
                final Parameter param = thresholds.get(i).getRight();
                conditions.add(param.nusmvCondition("CONT_INPUT_" + paramName, arr[i]));
            }
            if (conditions.isEmpty()) {
                conditions.add("TRUE");
            }
            result.append("    ").append(event).append(" := ").append(String.join(" & ", conditions)).append(";\n");
        } else {
            final int intervalNum = thresholds.get(index).getRight().valueCount();
            for (int i = 0; i < intervalNum; i++) {
                arr[index] = i;
                nusmvEventDescriptions(arr, index + 1, result, thresholds, events);
            }
        }
    }

    private static void spinEventDescriptions(int[] arr, int index, PrintWriter result,
                                              List<Pair<String, Parameter>> thresholds, List<String> events) {
        if (index == arr.length) {
            final String event = "input_A" + Arrays.toString(arr).replaceAll("[,\\[\\] ]", "");
            if (!events.contains(event)) {
                return;
            }
            final List<String> conditions = new ArrayList<>();
            for (int i = 0; i < arr.length; i++) {
                final String paramName = thresholds.get(i).getLeft();
                final Parameter param = thresholds.get(i).getRight();
                conditions.add(param.spinCondition("PLANT_INPUT_" + paramName, arr[i]));
            }
            if (conditions.isEmpty()) {
                conditions.add("true");
            }
            result.append("        ").append(event).append(" = ").append(String.join(" && ", conditions)).append(";\n");
        } else {
            final int intervalNum = thresholds.get(index).getRight().valueCount();
            for (int i = 0; i < intervalNum; i++) {
                arr[index] = i;
                spinEventDescriptions(arr, index + 1, result, thresholds, events);
            }
        }
    }

    private static void spinEventDeclarations(int[] arr, int index, PrintWriter result,
                                              List<Pair<String, Parameter>> thresholds, List<String> events) {
        if (index == arr.length) {
            final String event = "input_A" + Arrays.toString(arr).replaceAll("[,\\[\\] ]", "");
            if (!events.contains(event)) {
                return;
            }
            result.append("bool ").append(event).append(";\n");
        } else {
            final int intervalNum = thresholds.get(index).getRight().valueCount();
            for (int i = 0; i < intervalNum; i++) {
                arr[index] = i;
                spinEventDeclarations(arr, index + 1, result, thresholds, events);
            }
        }
    }

    private int maxLoopExecutions() {
        return loopConstraints.stream().mapToInt(x -> x).max().getAsInt();
    }

    public void toNuSMVString(List<String> events, List<String> actions, Configuration conf, PrintWriter pw) {
        final List<String> unmodifiedEvents = events;
        events = events.stream().map(s -> "input_" + s).collect(Collectors.toList());
        final List<Pair<String, Parameter>> eventThresholds = conf != null
                ? conf.eventThresholds() : new ArrayList<>();
        final List<Pair<String, Parameter>> actionThresholds = conf != null
                ? conf.actionThresholds() : new ArrayList<>();
        final Map<String, String> actionDescriptions = conf != null
                ? conf.extendedActionDescriptions() : new HashMap<>();
        final String inputLine = String.join(", ", eventThresholds.stream()
                .map(t -> "CONT_INPUT_" + t.getKey()).collect(Collectors.toList()));

        if (false) {
            pw.append("MODULE main\n");
            pw.append("VAR\n");
            pw.append("    plant: PLANT(").append(inputLine).append(");\n");
            for (Pair<String, Parameter> entry : eventThresholds) {
                final String paramName = entry.getLeft();
                final Parameter param = entry.getRight();
                pw.append("    CONT_INPUT_").append(paramName).append(": ").append(param.nusmvType()).append(";\n");
            }
            pw.append("\n");
        }

        pw.append("MODULE PLANT(").append(inputLine).append(")\n");
        pw.append("VAR\n");
        for (Pair<String, Parameter> entry : actionThresholds) {
            final String paramName = entry.getKey();
            final Parameter p = entry.getValue();
            final String name = "CONT_" + paramName;
            pw.append("    ").append(name).append(": ").append(p.nusmvType()).append(";\n");
        }
        pw.append("    unsupported: boolean;\n");
        pw.append("    loop_executed: boolean;\n");
        pw.append("    state: 0..").append(String.valueOf(stateCount() - 1)).append(";\n");
        if (loopConstraints != null) {
            pw.append("    loop_executions: 0..").append(String.valueOf(maxLoopExecutions())).append(";\n");
            pw.append("    loop_executions_violated: boolean;\n");
        }
        pw.append("INIT\n");
        pw.append("    state in ").append(TraceModelGenerator.expressWithIntervalsNuSMV(initialStates())).append("\n");
        pw.append("TRANS\n");
        
        final List<String> stateConstraints = new ArrayList<>();
        for (int i = 0; i < stateCount(); i++) {
            final List<String> options = new ArrayList<>();
            final Map<List<Integer>, Set<String>> map = new LinkedHashMap<>();
            for (String event : events) {
                final List<Integer> destinations = states.get(i).transitions().stream()
                        .filter(t -> ("input_" + t.event()).equals(event))
                        .map(t -> t.dst().number()).collect(Collectors.toList());
                Collections.sort(destinations);
                Set<String> correspondingEvents = map.get(destinations);
                if (correspondingEvents == null) {
                    correspondingEvents = new TreeSet<>();
                    map.put(destinations, correspondingEvents);
                }
                correspondingEvents.add(event);
            }
            final Set<Integer> allSuccStates = states.get(i).transitions().stream()
                    .map(t -> t.dst().number()).collect(Collectors.toCollection(TreeSet::new));
            {
                // if the input is unknown, then the choice for the next state is wide
                final List<Integer> allSuccStatesList = new ArrayList<>(allSuccStates);
                Set<String> correspondingEvents = map.get(allSuccStatesList);
                if (correspondingEvents == null) {
                    correspondingEvents = new TreeSet<>();
                    map.put(allSuccStatesList, correspondingEvents);
                }
                correspondingEvents.add("!known_input");
            }
            for (Map.Entry<List<Integer>, Set<String>> entry : map.entrySet()) {
                final List<Integer> destinations = entry.getKey();
                final Set<String> correspondingEvents = entry.getValue();
                
                options.add("(" + String.join(" | ", correspondingEvents) + ") & next(state) in "
                        + TraceModelGenerator.expressWithIntervalsNuSMV(destinations));
            }

            stateConstraints.add("state = " + i + " -> (\n      " + String.join("\n    | ", options));
        }
        pw.append("    (").append(String.join("\n    )) & (", stateConstraints)).append("))\n");
        
        // marking that there was a transition unsupported by traces
        pw.append("ASSIGN\n");
        pw.append("    init(unsupported) := FALSE;\n");
        pw.append("    next(unsupported) := unsupported | next(current_unsupported);\n");
        pw.append("    init(loop_executed) := FALSE;\n");
        pw.append("    next(loop_executed) := state = next(state);\n");
        if (loopConstraints != null) {
            pw.append("    init(loop_executions) := 0;\n");
            pw.append("    next(loop_executions) := !next(loop_executed) ? 0 : loop_executions >= ")
                    .append(String.valueOf(maxLoopExecutions())).append(" ? ")
                    .append(String.valueOf(maxLoopExecutions())).append(" : (loop_executions + 1);\n");
            pw.append("    init(loop_executions_violated) := FALSE;\n");
            final List<String> options = new ArrayList<>();
            options.add("loop_executions_violated");
            for (int i = 0; i < stateCount(); i++) {
                options.add("next(state) = " + i + " & next(loop_executions) = " + loopConstraints.get(i));
            }
            pw.append("    next(loop_executions_violated) := ").append(String.join("\n        | ", options))
                    .append(";\n");
        }
        pw.append("DEFINE\n");
        pw.append("    known_input := ").append(String.join(" | ", events)).append(";\n");
        final List<String> unsupported = new ArrayList<>();
        unsupported.add("!known_input");
        for (String e : unmodifiedEvents) {
            final Set<Integer> sourceStates = new TreeSet<>();
            for (int i = 0; i < stateCount(); i++) {
                for (MooreTransition t : states.get(i).transitions()) {
                    if (t.event().equals(e) && unsupportedTransitions.contains(t)) {
                        sourceStates.add(i);
                        break;
                    }
                }
            }
            if (!sourceStates.isEmpty()) {
                unsupported.add("input_" + e + " & state in "
                        + TraceModelGenerator.expressWithIntervalsNuSMV(sourceStates));
            }
        }

        pw.append("    current_unsupported := ").append(String.join("\n        | ", unsupported)).append(";\n");
        for (String action : actions) {
            final List<Integer> properStates = new ArrayList<>();
            for (int i = 0; i < stateCount(); i++) {
                if (ArrayUtils.contains(states.get(i).actions().getActions(), action)) {
                    properStates.add(i);
                }
            }
            final String condition = properStates.isEmpty()
                    ? "FALSE"
                    : ("state in " + TraceModelGenerator.expressWithIntervalsNuSMV(properStates));
            final String comment = actionDescriptions.containsKey(action)
                    ? (" -- " + actionDescriptions.get(action)) : "";
            pw.append("    output_").append(action).append(" := ").append(condition).append(";").append(comment)
                    .append("\n");
        }

        // output conversion to continuous values
        pw.append("ASSIGN\n");
        for (Pair<String, Parameter> entry : actionThresholds) {
            final String paramName = entry.getKey();
            final Parameter param = entry.getValue();
            pw.append("    CONT_").append(paramName).append(" := case\n");
            for (int i = 0; i < param.valueCount(); i++) {
                pw.append("        output_").append(paramName).append(String.valueOf(i)).append(": ").append(param.nusmvInterval(i))
                        .append(";\n");
            }
            pw.append("    esac;\n");
        }
        // input conversion to discrete values
        nusmvEventDescriptions(new int[eventThresholds.size()], 0, pw, eventThresholds, events);
    }

    public String toNuSMVString(List<String> events, List<String> actions, Configuration conf) {
        final StringWriter sw = new StringWriter();
        toNuSMVString(events, actions, conf, new PrintWriter(sw));
        return sw.toString();
    }

    private String indent(int spaces, String s) {
        final String indent = StringUtils.repeat(' ', spaces);
        return String.join("\n", Arrays.stream(s.split("\n")).map(x -> indent + x).collect(Collectors.toList()));
    }

    public void toSPINString(List<String> events, List<String> actions, Configuration conf, PrintWriter pw) {
        if (conf == null) {
            throw new AssertionError();
        }
        final List<String> unmodifiedEvents = events;
        events = events.stream().map(s -> "input_" + s).collect(Collectors.toList());
        final List<Pair<String, Parameter>> eventThresholds = conf != null
                ? conf.eventThresholds() : new ArrayList<>();
        final List<Pair<String, Parameter>> actionThresholds = conf != null
                ? conf.actionThresholds() : new ArrayList<>();

        for (Pair<String, Parameter> p : eventThresholds) {
            pw.append(p.getRight().spinType()).append(" PLANT_INPUT_").append(p.getLeft()).append(";\n");
        }
        for (Parameter p : conf.outputParameters) {
            pw.append(p.spinType()).append(" PLANT_OUTPUT_").append(p.traceName()).append(";\n");
            pw.append(p.spinType()).append(" CONT_PLANT_OUTPUT_").append(p.traceName()).append(";\n");
        }

        pw.append("\n");

        pw.append("#define INCLUDE_FAIRNESS\n");
        pw.append("#define INCLUDE_UNSUPPORTED\n");
        pw.append("\n");
        pw.append("#define LTL(x, y) ltl x { X(y) }\n");
        pw.append("\n");
        pw.append("#ifdef INCLUDE_FAIRNESS\n");
        pw.append("#define LTL_FAIRNESS(x, y) ltl x { X((<> [] loop_executed) || (y)) }\n");
        pw.append("#else\n");
        pw.append("#define LTL_FAIRNESS(x, y) ltl x { 0 }\n");
        pw.append("#endif\n");
        pw.append("\n");
        pw.append("#ifdef INCLUDE_UNSUPPORTED\n");
        pw.append("#define LTL_UNSUPPORTED(x, y) ltl x { X((<> current_unsupported) || (y)) }\n");
        pw.append("#else\n");
        pw.append("#define LTL_UNSUPPORTED(x, y) ltl x { 0 }\n");
        pw.append("#endif\n");
        pw.append("\n");
        pw.append("#if defined(INCLUDE_FAIRNESS) && defined(INCLUDE_UNSUPPORTED)\n");
        pw.append("#define LTL_FAIRNESS_UNSUPPORTED(x, y) ltl x { X((<> [] loop_executed) || (<> current_unsupported) || (y)) }\n");
        pw.append("#else\n");
        pw.append("#define LTL_FAIRNESS_UNSUPPORTED(x, y) ltl x { 0 }\n");
        pw.append("#endif\n");
        pw.append("\n");
        pw.append("bool loop_executed;\n");
        pw.append("bool current_unsupported;\n");
        pw.append("\n");
        pw.append("int state = -1;\n");
        spinEventDeclarations(new int[eventThresholds.size()], 0, pw, eventThresholds, events);
        pw.append("bool known_input;\n");
        pw.append("\n");
        pw.append("init { do :: atomic {\n");
        pw.append("\n");
        pw.append("    d_step {\n");
        spinEventDescriptions(new int[eventThresholds.size()], 0, pw, eventThresholds, events);
        pw.append("        known_input = ").append(String.join(" || ", events)).append(";\n");
        pw.append("\n");
        pw.append("        #ifdef INCLUDE_UNSUPPORTED\n");
        final List<String> unsupported = new ArrayList<>();
        unsupported.add("!known_input");
        for (String e : unmodifiedEvents) {
            final Set<Integer> sourceStates = new TreeSet<>();
            for (int i = 0; i < stateCount(); i++) {
                for (MooreTransition t : states.get(i).transitions()) {
                    if (t.event().equals(e) && unsupportedTransitions.contains(t)) {
                        sourceStates.add(i);
                        break;
                    }
                }
            }
            if (!sourceStates.isEmpty()) {
                unsupported.add("input_" + e + " && ("
                        + TraceModelGenerator.expressWithIntervalsSPIN(sourceStates, "state") + ")");
            }
        }
        pw.append("        current_unsupported = ").append(String.join(" ||\n            ", unsupported)).append(";\n");
        pw.append("        #endif\n");
        pw.append("    }\n");
        pw.append("\n");
        pw.append("    #ifdef INCLUDE_FAIRNESS\n");
        pw.append("    int last_state = state;\n");
        pw.append("    #endif\n");
        pw.append("\n");
        pw.append("    if\n");

        // creation of the structure:
        // destination -> source -> inputs which lead to this destination from this source
        final List<Map<Integer, Set<String>>> mapList = new ArrayList<>();
        for (int i = 0; i < stateCount(); i++) {
            final Map<Integer, Set<String>> map = new TreeMap<>();
            for (int j = -1; j < stateCount(); j++) {
                map.put(j, new TreeSet<>());
            }
            mapList.add(map);
        }
        for (int source = 0; source < stateCount(); source++) {
            for (String event : events) {
                final List<Integer> destinations = states.get(source).transitions().stream()
                        .filter(t -> ("input_" + t.event()).equals(event))
                        .map(t -> t.dst().number()).collect(Collectors.toList());
                for (int dest : destinations) {
                    mapList.get(dest).get(source).add(event);
                }
                final Set<Integer> allSuccStates = states.get(source).transitions().stream()
                        .map(t -> t.dst().number()).collect(Collectors.toCollection(TreeSet::new));
                // if the input is unknown, then the choice for the next state is wide
                for (int dest : allSuccStates) {
                    mapList.get(dest).get(source).add("!known_input");
                }
            }
        }
        // transitions to initial states from the pseudo-initial state
        for (int dest : initialStates()) {
            mapList.get(dest).get(-1).add("");
        }

        for (int i = 0; i < stateCount(); i++) {
            final Map<Integer, Set<String>> sources = mapList.get(i);
            final List<String> sourceOptions = new ArrayList<>();
            for (int j = -1; j < stateCount(); j++) {
                final Set<String> inputs = sources.get(j);
                if (inputs.isEmpty()) {
                    continue;
                }
                sourceOptions.add("(state == " + j + (j == -1 ? ""
                        : (" && (" + String.join(" || ", inputs) + ")")) + ")");
            }
            if (sourceOptions.isEmpty()) {
                continue;
            }

            final List<String> properActions = new ArrayList<>();
            for (String action : actions) {
                if (ArrayUtils.contains(states.get(i).actions().getActions(), action)) {
                    properActions.add("PLANT_OUTPUT_" + action.substring(0, action.length() - 1) + " = "
                            + action.charAt(action.length() - 1));
                }
            }

            pw.append("    :: ").append(String.join(" || ", sourceOptions)).append(" -> d_step { state = ")
                    .append(String.valueOf(i)).append("; ").append(String.join("; ", properActions)).append("; }\n");
        }
        pw.append("    fi\n");
        pw.append("\n");

        final StringBuilder dstepSb = new StringBuilder();
        final StringBuilder usualSb = new StringBuilder();

        // output conversion to continuous values
        for (Pair<String, Parameter> entry : actionThresholds) {
            final String paramName = entry.getKey();
            final Parameter param = entry.getValue();
            final StringBuilder effectiveSb = param instanceof IgnoredBoolParameter ? usualSb : dstepSb;
            if (param instanceof BoolParameter) {
                effectiveSb.append("    CONT_PLANT_OUTPUT_").append(paramName).append(" = PLANT_OUTPUT_")
                        .append(paramName).append(";\n\n");
            } else {
                effectiveSb.append("    if\n");
                for (int i = 0; i < param.valueCount(); i++) {
                    final String condition = "    :: PLANT_OUTPUT_" + paramName + " == " + i + " -> ";
                    if (param instanceof IgnoredBoolParameter) {
                        effectiveSb.append("    :: CONT_PLANT_OUTPUT_").append(paramName).append(" = ").append(0)
                                .append(";\n");
                        effectiveSb.append("    :: CONT_PLANT_OUTPUT_").append(paramName).append(" = ").append(1)
                                .append(";\n");
                    } else if (param instanceof SetParameter) {
                        effectiveSb.append(condition).append("CONT_PLANT_OUTPUT_").append(paramName).append(" = ")
                                .append(((SetParameter) param).roundedValue(i)).append(";\n");
                    } else if (param instanceof RealParameter || param instanceof SegmentsParameter) {
                        final String[] tokens = param.spinInterval(i).split("\\.\\.");
                        final int min = Integer.parseInt(tokens[0]);
                        final int max = Integer.parseInt(tokens[1]);
                        final int mid = (max + min) / 2;
                        // by default, only one value is allowed
                        //effectiveSb.append(condition.replace("::", "// ::") + "CONT_PLANT_OUTPUT_" + paramName + " = "
                        //        + min + ";\n");
                        effectiveSb.append(condition).append("CONT_PLANT_OUTPUT_").append(paramName).append(" = ")
                                .append(mid).append(";\n");
                        //effectiveSb.append(condition.replace("::", "// ::") + "CONT_PLANT_OUTPUT_" + paramName + " = "
                        //        + max + ";\n");
                    } else {
                        throw new AssertionError("Unknown parameter type!");
                    }
                }
                effectiveSb.append("    fi\n\n");
            }
        }

        dstepSb.append("    #ifdef INCLUDE_FAIRNESS\n");
        dstepSb.append("    loop_executed = state == last_state;\n");
        dstepSb.append("    #endif\n\n");

        pw.append("    d_step {\n").append(indent(4, dstepSb.toString())).append("\n    }\n\n").append(usualSb)
                .append("} od }\n");
    }
    
    public String toSPINString(List<String> events, List<String> actions, Configuration conf) {
        final StringWriter sw = new StringWriter();
        toSPINString(events, actions, conf, new PrintWriter(sw));
        return sw.toString();
    }

    public boolean compliesWith(List<StringScenario> scenarios, boolean positive, boolean markUnsupportedTransitions) {
        final Set<MooreTransition> supported = new HashSet<>();

        for (StringScenario sc : scenarios) {
            boolean[] curStates = new boolean[states.size()];
            final StringActions firstActions = sc.getActions(0);
            for (int i = 0; i < states.size(); i++) {
                if (isInitialState(i) && states.get(i).actions().setEquals(firstActions)) {
                    curStates[i] = true;
                }
            }
            for (int i = 1; i < sc.size(); i++) {
                final String event = sc.getEvents(i).get(0);
                final StringActions actions = sc.getActions(i);
                final boolean[] newStates = new boolean[states.size()];
                for (int j = 0; j < states.size(); j++) {
                    if (curStates[j]) {
                        for (MooreTransition t : states.get(j).transitions()) {
                            if (t.event().equals(event) && t.dst().actions().setEquals(actions)) {
                                newStates[t.dst().number()] = true;
                                supported.add(t);
                            }
                        }
                    }
                }
                curStates = newStates;
            }
            final boolean passed = ArrayUtils.contains(curStates, true);
            if (passed != positive) {
                return false;
            }
        }

        if (markUnsupportedTransitions) {
            for (MooreNode state : states) {
                unsupportedTransitions.addAll(state.transitions().stream()
                        .filter(t -> !supported.contains(t)).collect(Collectors.toList()));
            }
        }

        return true;
    }

    public boolean isDeterministic() {
        if (initialStates().size() > 1) {
            return false;
        }
        final Set<String> allEvents = new TreeSet<>();
        states.forEach(s -> s.transitions().stream().map(MooreTransition::event).forEach(allEvents::add));
        for (MooreNode s : states) {
            for (String event : allEvents) {
                if (s.allDst(event).size() > 1) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * Requires determinism.
     */
    private double strongCompliance(StringScenario scenario) {
        MooreNode node = states.get(initialStates().get(0));
        for (int pos = 0; pos < scenario.size(); pos++) {
            final StringActions actions = scenario.getActions(pos);
            if (pos == 0) {
                if (!node.actions().setEquals(actions)) {
                    return 0;
                }
            } else {
                final String e = scenario.getEvents(pos).get(0);
                boolean ok = false;
                for (MooreTransition t : node.transitions()) {
                    if (t.event().equals(e) && t.dst().actions().setEquals(actions)) {
                        node = t.dst();
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    return 0;
                }
            }
        }
        return 1;
    }

    /*
     * Requires determinism.
     */
    private double mediumCompliance(StringScenario scenario) {
        MooreNode node = states.get(initialStates().get(0));
        for (int pos = 0; pos < scenario.size(); pos++) {
            final StringActions actions = scenario.getActions(pos);
            if (pos == 0) {
                if (!node.actions().setEquals(actions)) {
                    return 0;
                }
            } else {
                final String e = scenario.getEvents(pos).get(0);
                boolean ok = false;
                for (MooreTransition t : node.transitions()) {
                    if (t.event().equals(e) && t.dst().actions().setEquals(actions)) {
                        node = t.dst();
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    return (double) pos / scenario.size();
                }
            }
        }
        return 1;
    }

    /*
     * Requires determinism.
     */
    private double weakCompliance(StringScenario scenario) {
        MooreNode node = states.get(initialStates().get(0));
        int matched = 0;
        for (int pos = 0; pos < scenario.size(); pos++) {
            final StringActions actions = scenario.getActions(pos);
            if (pos > 0) {
                final String e = scenario.getEvents(pos).get(0);
                for (MooreTransition t : node.transitions()) {
                    if (t.event().equals(e)) {
                        node = t.dst();
                        break;
                    }
                }
            }
            matched += node.actions().setEquals(actions) ? 1 : 0;
        }
        return (double) matched / scenario.size();
    }

    public double strongCompliance(List<StringScenario> scenarios) {
        if (!isDeterministic()) {
            throw new RuntimeException("The automaton must be deterministic.");
        }
        return scenarios.stream().mapToDouble(this::strongCompliance).average().getAsDouble();
    }

    public double mediumCompliance(List<StringScenario> scenarios) {
        if (!isDeterministic()) {
            throw new RuntimeException("The automaton must be deterministic.");
        }
        return scenarios.stream().mapToDouble(this::mediumCompliance).average().getAsDouble();
    }

    public double weakCompliance(List<StringScenario> scenarios) {
        if (!isDeterministic()) {
            throw new RuntimeException("The automaton must be deterministic.");
        }
        return scenarios.stream().mapToDouble(this::weakCompliance).average().getAsDouble();
    }
    
    public NondetMooreAutomaton copy() {
        final List<StringActions> actions = states.stream().map(MooreNode::actions)
                .collect(Collectors.toCollection(ArrayList::new));
        final NondetMooreAutomaton copy = new NondetMooreAutomaton(states.size(), actions,
                new ArrayList<>(this.isInitial));

        for (MooreNode state : states) {
            final MooreNode src = copy.state(state.number());
            for (MooreTransition t : state.transitions()) {
                final MooreTransition tNew
                        = new MooreTransition(src, copy.state(t.dst().number()), t.event());
                src.addTransition(tNew);
                if (unsupportedTransitions.contains(t)) {
                    copy.unsupportedTransitions.add(tNew);
                }
            }
        }

        copy.loopConstraints = loopConstraints;
        return copy;
    }
    
    public NondetMooreAutomaton swapStates(int[] permutation) {
        final NondetMooreAutomaton copy = copy();
        
        for (int i = 0; i < isInitial.size(); i++) {
            copy.isInitial.set(i, isInitial.get(permutation[i]));
        }
        
        copy.states.clear();
        for (int i = 0; i < states.size(); i++) {
            copy.states.add(new MooreNode(i, states.get(permutation[i]).actions()));
        }
    
        for (MooreNode state : states) {
            final MooreNode src = copy.state(permutation[state.number()]);
            for (MooreTransition t : state.transitions()) {
                final MooreNode dst = copy.state(permutation[t.dst().number()]);                
                final MooreTransition tNew = new MooreTransition(src, dst, t.event());
                src.addTransition(tNew);
                if (unsupportedTransitions.contains(t)) {
                    copy.unsupportedTransitions.add(tNew);
                }
            }
        }
        
        return copy;
    }

    public NondetMooreAutomaton simplify() {
        final List<StringActions> actions = states.stream().map(MooreNode::actions)
                .collect(Collectors.toCollection(ArrayList::new));
        final NondetMooreAutomaton copy = new NondetMooreAutomaton(states.size(), actions,
                new ArrayList<>(this.isInitial));

        for (MooreNode state : states) {
            final Set<Integer> destinations = state.transitions().stream()
                    .map(MooreTransition::dst).map(MooreNode::number)
                    .collect(Collectors.toCollection(TreeSet::new));
            final MooreNode copyState = copy.state(state.number());
            for (Integer dst : destinations) {
                copyState.addTransition(" ", copy.state(dst));
            }
        }

        copy.loopConstraints = loopConstraints;
        return copy;
    }

    public Set<Integer> reachableStates() {
        final Set<Integer> visited = new LinkedHashSet<>();
        final Deque<Integer> queue = new ArrayDeque<>(initialStates());
        while (!queue.isEmpty()) {
            final int state = queue.removeFirst();
            if (visited.contains(state)) {
                continue;
            }
            visited.add(state);

            states.get(state).transitions().stream().map(MooreTransition::dst).map(MooreNode::number)
                    .forEach(queue::addLast);
        }
        return visited;
    }
}
