package algorithms;

import bool.MyBooleanExpression;
import scenario.StringActions;
import scenario.StringScenario;
import structures.mealy.MealyAutomaton;
import structures.mealy.MealyNode;
import structures.mealy.MealyTransition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class ScenarioGenerator {
    private static List<List<MealyTransition>> getBFSPaths(MealyAutomaton automaton) {
        final List<List<MealyTransition>> ans = new ArrayList<>();

        final int size = automaton.stateCount();
        @SuppressWarnings("unchecked")
        final List<MealyTransition>[] shortestPaths = new ArrayList[size];

        final List<MealyNode> order = new ArrayList<>();
        order.add(automaton.startState());
        shortestPaths[automaton.startState().number()] = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            final MealyNode current = order.get(i);
            final List<MealyTransition> path = shortestPaths[current.number()];

            for (MealyTransition t : current.transitions()) {
                final List<MealyTransition> newPath = new ArrayList<>(path);
                newPath.add(t);
                ans.add(newPath);

                if (shortestPaths[t.dst().number()] == null) {
                    order.add(t.dst());
                    shortestPaths[t.dst().number()] = newPath;
                }
            }
        }

        return ans;
    }

    private static String pathToScenario(List<MealyTransition> path) {
        final ArrayList<String> events = new ArrayList<>();
        final ArrayList<MyBooleanExpression> expressions = new ArrayList<>();
        final ArrayList<StringActions> actions = new ArrayList<>();

        for (MealyTransition t : path) {
            events.add(t.event());
            expressions.add(t.expr());
            actions.add(t.actions());
        }
        return new StringScenario(events, expressions, actions).toString();
    }
    
    public static String generateScenariosWithBFS(MealyAutomaton automaton) {
        final List<List<MealyTransition>> paths = getBFSPaths(automaton);
        int lenBFS = 0;
        for (List<MealyTransition> path : paths) {
            lenBFS += path.size();
        }
 
        return generateScenariosWithBFS(automaton, lenBFS, null);
    }
    
    public static String generateScenariosWithBFS(MealyAutomaton automaton, int sumLength, Random random) {
        final List<List<MealyTransition>> paths = getBFSPaths(automaton);
        int lenBFS = 0;
        for (List<MealyTransition> path : paths) {
            lenBFS += path.size();
        }
        if (lenBFS > sumLength) {
            throw new RuntimeException(
                    "Impossible to generate scenarios wits summary length [" + sumLength + "] with BFS");
        }

        for (int i = lenBFS; i < sumLength; i++) {
            final int randomPathNum = random.nextInt(paths.size());
            final List<MealyTransition> randomPath = paths.get(randomPathNum);

            final MealyNode lastNode = randomPath.get(randomPath.size() - 1).dst();
            final int randomTransitionNumber = random.nextInt(lastNode.transitionCount());
            MealyTransition randomTransition = lastNode.transitions()
                    .toArray(new MealyTransition[0])[randomTransitionNumber];
            randomPath.add(randomTransition);
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) {
                sb.append("\n\n");
            }
            sb.append(pathToScenario(paths.get(i)));
        }

        return sb.toString();
    }

    public static String generateScenarios(MealyAutomaton automaton, int scenariosCount, int minLength, int maxLength,
                                           int sumLength, Random random) {
        final int[] length = getRandomLength(scenariosCount, minLength, maxLength, sumLength, random);

        final List<Collection<MealyTransition>> visitedTransitions = new ArrayList<>();
        for (int i = 0; i < automaton.states().size(); i++) {
            visitedTransitions.add(new ArrayList<>());
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scenariosCount; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(generateScenario(automaton, length[i], visitedTransitions, random));
        }
        return sb.toString();
        
    }

    private static String generateScenario(MealyAutomaton automaton, int length,
                                           List<Collection<MealyTransition>> visitedTransitions, Random random) {
        final StringBuilder events = new StringBuilder();
        final StringBuilder actions = new StringBuilder();

        MealyNode curNode = automaton.startState();
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                events.append("; ");
                actions.append("; ");
            }

            if (curNode.transitions().isEmpty()) {
                throw new RuntimeException("There is no outgoing transitions from node number " + curNode.number());
            }
                        
            MealyTransition transition = null;
            final Collection<MealyTransition> currentVisited = visitedTransitions.get(curNode.number());
            if (currentVisited.size() == curNode.transitions().size()) {
                int transitionNum = random.nextInt(curNode.transitions().size());
                transition = curNode.transitions().toArray(new MealyTransition[0])[transitionNum];
            } else {
                for (MealyTransition unvisited : curNode.transitions()) {
                    boolean was = false;
                    for (MealyTransition visited : currentVisited) {
                        if (unvisited == visited) {
                            was = true;
                        }
                    }
                    if (!was) {
                        transition = unvisited;
                        break;
                    }
                }
                currentVisited.add(transition);
            }
            
            events.append(transition.event() + "[" + transition.expr() + "]");
            actions.append(transition.actions());

            curNode = transition.dst();
        }

        return events + "\n" + actions + "\n";
    }

    private static int[] getRandomLength(int scenariosCount, int minLength, int maxLength, int sumLength,
                                         Random random) {
        if (!(0 < minLength && minLength <= maxLength)) {
            throw new RuntimeException();
        }
        if (!(minLength * scenariosCount <= sumLength)) {
            throw new RuntimeException();
        }
        if (!(sumLength <= maxLength * scenariosCount)) {
            throw new RuntimeException();
        }

        final int[] length = new int[scenariosCount];
        for (int i = 0; i < scenariosCount; i++) {
            length[i] = sumLength / scenariosCount;
        }
        for (int i = 0; i < sumLength % scenariosCount; i++) {
            length[i]++;
        }

        for (int i = 0; i < scenariosCount; i++) {
            final int s1 = random.nextInt(scenariosCount);
            final int s2 = random.nextInt(scenariosCount);

            int maxDiff = Math.min(length[s1] - minLength, maxLength - length[s2]);
            if (maxDiff > 0) {
                int diff = 1 + random.nextInt(maxDiff);
                length[s1] -= diff;
                length[s2] += diff;
            }
        }

        return length;
    }
}
