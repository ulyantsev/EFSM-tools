package algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import structures.mealy.MealyAutomaton;
import structures.mealy.MealyNode;
import structures.mealy.MealyTransition;

public class AutomatonIsomorphismChecker {
	private static boolean transitionsEquals(MealyTransition first, MealyTransition second) {
		return first.event().equals(second.event()) && first.expr() == second.expr()
				&& first.actions().equals(second.actions());
	}

	public static boolean isIsomorphic(MealyAutomaton first, MealyAutomaton second) {
		final int[] firstToSecond = new int[first.stateCount()];
		Arrays.fill(firstToSecond, -1);
		firstToSecond[first.startState().number()] = second.startState().number();
		
		final List<MealyNode> order = new ArrayList<>();
		order.add(first.startState());
		for (int i = 0; i < first.stateCount(); i++) {
			if (i >= order.size()) {
				break;
			}
			final MealyNode firstNode = order.get(i);
            final int secondNodeNumber = firstToSecond[firstNode.number()];
            final MealyNode secondNode = second.state(secondNodeNumber);
			if (firstNode.transitionCount() != secondNode.transitionCount()) {
				return false;
			}
			
			for (MealyTransition firstTransition : firstNode.transitions()) {
				MealyNode firstDst = firstTransition.dst();
				MealyTransition secondTransition = null;
				for (MealyTransition t : secondNode.transitions()) {
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
