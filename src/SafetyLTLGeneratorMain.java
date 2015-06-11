import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import structures.Automaton;
import structures.Node;
import structures.Transition;
import tools.AutomatonGVLoader;

public class SafetyLTLGeneratorMain {

	public static void main(String[] args) {
    	if (args.length != 1) {
            System.out.println("Generator of safety LTL formulae for a given FSM");
            System.out.println("Author: Igor Buzhinsky, igor.buzhinsky@gmail.com\n");
            System.out.println("Usage: java -jar safety-ltl-generator.jar <efsm.gv>");
            return;
        }

        Automaton automaton;
        try {
            automaton = AutomatonGVLoader.load(args[0]);
        } catch (IOException e) {
            System.err.println("Can't open file " + args[0]);
            e.printStackTrace();
            return;
        } catch (ParseException e) {
            System.err.println("Can't read EFSM from file " + args[0]);
            e.printStackTrace();
            return;
        }

        generateX(automaton);
        generateGFuture(automaton);
        generateGPast(automaton);
	}

	private static void generateX(Automaton automaton) {
		//final Set<String> allEvents = allEvents(automaton);
		boolean[] possibleStates = new boolean[automaton.statesCount()];
		possibleStates[automaton.getStartState().getNumber()] = true;
		for (int i = 0; i < automaton.statesCount(); i++) {
			final Set<String> events = new TreeSet<>();
			final boolean[] newStates = new boolean[automaton.statesCount()];
			for (int j = 0; j < possibleStates.length; j++) {
				if (possibleStates[j]) {
					for (Transition t : automaton.getState(j).getTransitions()) {
						events.add(t.getEvent());
						newStates[t.getDst().getNumber()] = true;
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
	
	private static Set<String> allEvents(Automaton automaton) {
		final Set<String> events = new TreeSet<>();
		for (Node node : automaton.getStates()) {
			for (Transition t : node.getTransitions()) {
				events.add(t.getEvent());
			}
		}
		return events;
	}
	
	private static void generateGFuture(Automaton automaton) {
		//final Set<String> allEvents = allEvents(automaton);
		for (String event : allEvents(automaton)) {
			final Set<String> nextEvents = new TreeSet<>();
			for (Node node : automaton.getStates()) {
				for (Transition t : node.getTransitions()) {
					if (t.getEvent().equals(event)) {
						for (Transition nextT : t.getDst().getTransitions()) {
							nextEvents.add(nextT.getEvent());
						}
					}
				}
			}
			//if (!nextEvents.equals(allEvents)) {
				System.out.println("G(!(wasEvent(ep." + event + ")) || X(" + joinEvents(nextEvents) + "))");
			//}
		}
	}
	
	private static void generateGPast(Automaton automaton) {
		//final Set<String> allEvents = allEvents(automaton);
		for (String event : allEvents(automaton)) {
			final Set<String> prevEvents = new TreeSet<>();
			for (Node node : automaton.getStates()) {
				for (Transition t : node.getTransitions()) {
					if (t.getEvent().equals(event)) {
						for (Node prevNode : automaton.getStates()) {
							for (Transition prevT : prevNode.getTransitions()) {
								if (prevT.getDst() == node) {
									prevEvents.add(prevT.getEvent());
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
