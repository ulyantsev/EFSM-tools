package tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import structures.Automaton;
import structures.Node;
import structures.Transition;

public class AutomatonIsomorphismChecker {
	private static boolean transitionsEquals(Transition first, Transition second) {
		return first.getEvent().equals(second.getEvent()) && first.getExpr() == second.getExpr()
				&& first.getActions().equals(second.getActions());
	}

	public static boolean isIsomorphic(Automaton first, Automaton second) {
		int[] firstToSecond = new int[first.statesCount()];
		Arrays.fill(firstToSecond, -1);
		firstToSecond[first.getStartState().getNumber()] = second.getStartState().getNumber();
		
		List<Node> order = new ArrayList<Node>();
		order.add(first.getStartState());
		for (int i = 0; i < first.statesCount(); i++) {
			if (i >= order.size()) {
				break;
			}
			Node firstNode = order.get(i);
			int secondNodeNumber = firstToSecond[firstNode.getNumber()];
			Node secondNode = second.getState(secondNodeNumber);
			if (firstNode.transitionsCount() != secondNode.transitionsCount()) {
				return false;
			}
			
			for (Transition firstTransition : firstNode.getTransitions()) {
				Node firstDst = firstTransition.getDst();
				Transition secondTransition = null;
				for (Transition t : secondNode.getTransitions()) {
					if (transitionsEquals(firstTransition, t)) {
						secondTransition = t;
					}
				}
				if (secondTransition == null) {
					return false;
				}
				
				if (firstToSecond[firstDst.getNumber()] > -1) {
					if (firstToSecond[firstDst.getNumber()] != secondTransition.getDst().getNumber()) {
						return false;
					}
				} else {
					firstToSecond[firstDst.getNumber()] = secondTransition.getDst().getNumber();
				}
				
				if (!order.contains(firstDst)) {
					order.add(firstDst);
				}
			}
		}

		return true;
	}
}
