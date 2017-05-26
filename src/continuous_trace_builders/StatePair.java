package continuous_trace_builders;

/**
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.CompositionalBuilder.Match;
import continuous_trace_builders.parameters.Parameter;
import org.apache.commons.lang3.tuple.Pair;
import scenario.StringActions;
import structures.moore.MooreNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class StatePair {
    final MooreNode first;
    final MooreNode second;

    public StatePair(MooreNode first, MooreNode second) {
        this.first = first;
        this.second = second;
    }

    boolean isConsistent(Match match) {
        final String[] actions1 = first.actions().getActions();
        final String[] actions2 = second.actions().getActions();
        for (Pair<Parameter, Parameter> pair : match.outputPairs) {
            final String prefix = pair.getLeft().traceName();
            final int i1 = CompositionalBuilder.actionIntervalIndex(actions1, prefix);
            final int i2 = CompositionalBuilder.actionIntervalIndex(actions2, prefix);
            if (i1 != i2) {
                return false;
            }
        }
        return true;
    }

    Set<String> actionSet() {
        final Set<String> actions = new TreeSet<>();
        for (String action : first.actions().getActions()) {
            actions.add(action);
        }
        for (String action : second.actions().getActions()) {
            actions.add(action);
        }
        return actions;
    }

    Set<String> actionSet(Match match) {
        final Set<String> actions = actionSet();

        // remove internal connections
        final Set<String> removing = new TreeSet<>();
        for (String a : actions) {
            for (String prefix : match.badActionPrefixes) {
                if (CompositionalBuilder.isProperAction(a, prefix)) {
                    removing.add(a);
                    break;
                }
            }
        }

        actions.removeAll(removing);
        return actions;
    }

    boolean isPresentInTraces(Set<List<String>> allActionCombinationsSorted) {
        final List<String> actions = new ArrayList<>(actionSet());
        return allActionCombinationsSorted.contains(actions);
    }

    MooreNode toMooreNode(int number, Match match) {
        final Set<String> actionSet = actionSet(match);
        return new MooreNode(number, new StringActions(actionSet));
    }
}
