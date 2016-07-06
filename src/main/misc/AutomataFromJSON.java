package main.misc;

import jdk.nashorn.internal.ir.LexicalContext;
import jdk.nashorn.internal.ir.PropertyNode;
import jdk.nashorn.internal.ir.visitor.NodeVisitor;
import jdk.nashorn.internal.parser.JSONParser;
import jdk.nashorn.internal.runtime.Source;
import org.apache.commons.lang3.tuple.Pair;
import scenario.StringActions;
import structures.moore.NondetMooreAutomaton;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by buzhinsky on 7/5/16.
 */
public class AutomataFromJSON {
    public static void main(String[] args) throws IOException {
        final String prefix = "evaluation/moore-json/";
        for (String suffix : Arrays.asList("generated", "learned")) {
            final String dir = prefix + suffix + "/";
            for (String filename : new File(dir).list()) {
                if (!filename.endsWith(".json")) {
                    continue;
                }
                System.out.println("processing " + dir + filename);
                final NondetMooreAutomaton automaton = jsonToAutomaton(dir + filename);
                try (PrintWriter out = new PrintWriter(dir + filename.replace(".json", ".dot"))) {
                    out.println(automaton);
                }
            }
        }
    }

    private static NondetMooreAutomaton jsonToAutomaton(String filename) throws IOException {
        final Vis v = new Vis();
        new JSONParser(Source.sourceFor("a", new File(filename)), null).parse().accept(v);

        final List<Boolean> isInitial = new ArrayList<>(Collections.nCopies(v.stateNumber(), false));
        isInitial.set(v.initialState, true);
        final List<StringActions> actions = new ArrayList<>(Collections.nCopies(v.stateNumber(), null));
        for (Map.Entry<Integer, StringActions> entry : v.outputs.entrySet()) {
            actions.set(entry.getKey(), entry.getValue());
        }
        final NondetMooreAutomaton result = new NondetMooreAutomaton(v.stateNumber(), actions, isInitial);
        for (Map.Entry<Pair<Integer, Integer>, Integer> entry : v.transitions.entrySet()) {
            result.state(entry.getKey().getKey()).addTransition("e" + entry.getKey().getValue(),
                    result.state(entry.getValue()));
        }
        return result;
    }

    static class Vis extends NodeVisitor<LexicalContext> {
        Vis() {
            super(new LexicalContext());
        }

        int stateNumber() {
            return stateIndices.size();
        }

        int initialState = -1;
        int source = -1;
        final Map<Integer, StringActions> outputs = new HashMap<>();
        final Map<Pair<Integer, Integer>, Integer> transitions = new HashMap<>();
        final Map<String, Integer> stateIndices = new HashMap<>();

        enum State { DEFAULT, READ_OUTPUTS, READ_TRANSITIONS };
        State readingState = State.DEFAULT;

        int stateIndex(String state) {
            Integer index = stateIndices.get(state);
            if (index == null) {
                index = stateIndices.size();
                stateIndices.put(state, index);
            }
            return index;
        }

        @Override
        public boolean enterPropertyNode(PropertyNode propertyNode) {
            final String key = propertyNode.getKeyName();
            String value;

            switch (key) {
                case "input-alphabet":
                case "output-alphabet":
                case "states":
                    break;
                case "output-function":
                    readingState = State.READ_OUTPUTS;
                    break;
                case "transition-function":
                    readingState = State.READ_TRANSITIONS;
                    break;
                case "initial-state":
                    value = propertyNode.getValue().toString();
                    initialState = stateIndex(value);
                    break;
                default:
                    if (key.startsWith("q_") || key.contains("Q")) {
                        if (readingState == State.READ_OUTPUTS) {
                            value = propertyNode.getValue().toString();
                            outputs.put(stateIndex("\"" + key + "\""),
                                    ScenariosFromJSON.binaryEncodeOutput(number(value, 1, 1)));
                        } else if (readingState == State.READ_TRANSITIONS) {
                            source = stateIndex("\"" + key + "\"");
                        } else {
                            throw new RuntimeException();
                        }
                    } else {
                        value = propertyNode.getValue().toString();
                        transitions.put(Pair.of(source, number(key, 0, 0)), stateIndex(value));
                    }
            }

            return super.enterPropertyNode(propertyNode);
        }
    }

    private static int number(String str, int left, int right) {
        return Integer.parseInt(str.substring(left, str.length() - right));
    }
}
