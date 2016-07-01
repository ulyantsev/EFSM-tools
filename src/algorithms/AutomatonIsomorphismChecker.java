package algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import structures.Automaton;
import structures.Node;
import structures.Transition;

public class AutomatonIsomorphismChecker {
	private static boolean transitionsEquals(Transition first, Transition second) {
		return first.event().equals(second.event()) && first.expr() == second.expr()
				&& first.actions().equals(second.actions());
	}

	public static boolean isIsomorphic(Automaton first, Automaton second) {
		final int[] firstToSecond = new int[first.stateCount()];
		Arrays.fill(firstToSecond, -1);
		firstToSecond[first.startState().number()] = second.startState().number();
		
		final List<Node> order = new ArrayList<>();
		order.add(first.startState());
		for (int i = 0; i < first.stateCount(); i++) {
			if (i >= order.size()) {
				break;
			}
			final Node firstNode = order.get(i);
            final int secondNodeNumber = firstToSecond[firstNode.number()];
            final Node secondNode = second.state(secondNodeNumber);
			if (firstNode.transitionCount() != secondNode.transitionCount()) {
				return false;
			}
			
			for (Transition firstTransition : firstNode.transitions()) {
				Node firstDst = firstTransition.dst();
				Transition secondTransition = null;
				for (Transition t : secondNode.transitions()) {
					if (transitionsEquals(firstTransition, t)) {
						secondTransition = t;
					}
				}
				if (secondTransition == null) {
					return false;
				}
				
				if (firstToSecond[firstDst.number()] > -1) {
					if (firstToSecond[firstDst.number()] != secondTransition.dst().number()) {
						return false;
					}
				} else {
					firstToSecond[firstDst.number()] = secondTransition.dst().number();
				}
				
				if (!order.contains(firstDst)) {
					order.add(firstDst);
				}
			}
		}

		return true;
	}
}
