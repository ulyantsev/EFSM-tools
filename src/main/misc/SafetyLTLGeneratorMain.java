package main.misc;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import algorithms.AutomatonGVLoader;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import structures.mealy.MealyAutomaton;
import structures.mealy.MealyNode;
import structures.mealy.MealyTransition;

public class SafetyLTLGeneratorMain extends MainBase {
    @Argument(usage = "path to EFSM in Graphviz format", metaVar = "<efsm.gv>", required = true)
    private String efsm;

	public static void main(String[] args) {
        new SafetyLTLGeneratorMain().run(args, Author.IB, "Generator of safety LTL formulae for a given FSM");
	}

    @Override
    protected void launcher() throws IOException, ParseException {
        final MealyAutomaton automaton = AutomatonGVLoader.load(efsm);
        generateX(automaton);
        generateGFuture(automaton);
        generateGPast(automaton);
    }

	private static void generateX(MealyAutomaton automaton) {
		//final Set<String> allEvents = allEvents(automaton);
		boolean[] possibleStates = new boolean[automaton.stateCount()];
		possibleStates[automaton.startState().number()] = true;
		for (int i = 0; i < automaton.stateCount(); i++) {
			final Set<String> events = new TreeSet<>();
			final boolean[] newStates = new boolean[automaton.stateCount()];
			for (int j = 0; j < possibleStates.length; j++) {
				if (possibleStates[j]) {
					for (MealyTransition t : automaton.state(j).transitions()) {
						events.add(t.event());
						newStates[t.dst().number()] = true;
					}
				}
			}
			//if (!events.equals(allEvents)) {
            final StringBuilder sb = new StringBuilder();
            for (int j = 0; j < i; j++) {
                sb.append("X(");
            }
            sb.append(joinEvents(events));
            for (int j = 0; j < i; j++) {
                sb.append(")");
            }
            System.out.println(sb.toString());
			//}
			possibleStates = newStates;
		}
	}
	
	private static String joinEvents(Collection<String> events) {
		return String.join(" || ", events.stream().map(e -> "wasEvent(ep." + e + ")").collect(Collectors.toList()));
	}
	
	private static Set<String> allEvents(MealyAutomaton automaton) {
		final Set<String> events = new TreeSet<>();
		for (MealyNode node : automaton.states()) {
			for (MealyTransition t : node.transitions()) {
				events.add(t.event());
			}
		}
		return events;
	}
	
	private static void generateGFuture(MealyAutomaton automaton) {
		//final Set<String> allEvents = allEvents(automaton);
		for (String event : allEvents(automaton)) {
			final Set<String> nextEvents = new TreeSet<>();
			for (MealyNode node : automaton.states()) {
				for (MealyTransition t : node.transitions()) {
					if (t.event().equals(event)) {
						for (MealyTransition nextT : t.dst().transitions()) {
							nextEvents.add(nextT.event());
						}
					}
				}
			}
			//if (!nextEvents.equals(allEvents)) {
            System.out.println("G(!(wasEvent(ep." + event + ")) || X(" + joinEvents(nextEvents) + "))");
			//}
		}
	}
	
	private static void generateGPast(MealyAutomaton automaton) {
		//final Set<String> allEvents = allEvents(automaton);
		for (String event : allEvents(automaton)) {
			final Set<String> prevEvents = new TreeSet<>();
			for (MealyNode node : automaton.states()) {
				for (MealyTransition t : node.transitions()) {
					if (t.event().equals(event)) {
						for (MealyNode prevNode : automaton.states()) {
							for (MealyTransition prevT : prevNode.transitions()) {
								if (prevT.dst() == node) {
									prevEvents.add(prevT.event());
								}
							}
						}
					}
				}
			}
			//if (!prevEvents.equals(allEvents)) {
            System.out.println("G(!(X(wasEvent(ep." + event + "))) || " + joinEvents(prevEvents) + ")");
			//}
		}
	}
}
