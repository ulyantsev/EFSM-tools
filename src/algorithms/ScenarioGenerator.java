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
    static public ArrayList<ArrayList<MealyTransition>> getBFSPaths(MealyAutomaton automaton) {
        ArrayList<ArrayList<MealyTransition>> ans = new ArrayList<>();

        int size = automaton.stateCount();
        @SuppressWarnings("unchecked")
        ArrayList<MealyTransition>[] shortestPaths = new ArrayList[size];

        ArrayList<MealyNode> order = new ArrayList<>();
        order.add(automaton.startState());
        shortestPaths[automaton.startState().number()] = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            MealyNode current = order.get(i);
            ArrayList<MealyTransition> path = shortestPaths[current.number()];

            for (MealyTransition t : current.transitions()) {
                ArrayList<MealyTransition> newPath = new ArrayList<>(path);
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

    static public String pathToScenario(ArrayList<MealyTransition> path) {
        ArrayList<String> events = new ArrayList<>();
        ArrayList<MyBooleanExpression> expressions = new ArrayList<>();
        ArrayList<StringActions> actions = new ArrayList<>();

        for (MealyTransition t : path) {
            events.add(t.event());
            expressions.add(t.expr());
            actions.add(t.actions());
        }
        return new StringScenario(true, events, expressions, actions).toString();
    }
    
    static public String generateScenariosWithBFS(MealyAutomaton automaton) {
        ArrayList<ArrayList<MealyTransition>> paths = getBFSPaths(automaton);
        int lenBFS = 0;
        for (ArrayList<MealyTransition> path : paths) {
            lenBFS += path.size();
        }
 
        return generateScenariosWithBFS(automaton, lenBFS, null);
    }
    
    static public String generateScenariosWithBFS(MealyAutomaton automaton, int sumLength, Random random) {
        ArrayList<ArrayList<MealyTransition>> paths = getBFSPaths(automaton);
        int lenBFS = 0;
        for (ArrayList<MealyTransition> path : paths) {
            lenBFS += path.size();
        }
        if (lenBFS > sumLength) {
            throw new RuntimeException("Impossible to generate scenarios wits summary length [" + sumLength + "] with BFS");
        }

        for (int i = lenBFS; i < sumLength; i++) {
            int randomPathNum = random.nextInt(paths.size());
            ArrayList<MealyTransition> randomPath = paths.get(randomPathNum);

            MealyNode lastNode = randomPath.get(randomPath.size() - 1).dst();
            int randomTransitionNumber = random.nextInt(lastNode.transitionCount());
            MealyTransition randomTransition = lastNode.transitions().toArray(new MealyTransition[0])[randomTransitionNumber];
            randomPath.add(randomTransition);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) {
                sb.append("\n\n");
            }
            sb.append(pathToScenario(paths.get(i)));
        }

        return sb.toString();
    }

    static public String generateScenarios(MealyAutomaton automaton, int scenariosCount, int minLength, int maxLength,
                                           int sumLength, Random random) {
        int[] length = getRandomLength(scenariosCount, minLength, maxLength, sumLength, random);

        List<Collection<MealyTransition>> visitedTransitions = new ArrayList<>();
        for (int i = 0; i < automaton.states().size(); i++) {
            visitedTransitions.add(new ArrayList<>());
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scenariosCount; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(generateScenario(automaton, length[i], visitedTransitions, random));
        }
        return sb.toString();
        
    }

    static private String generateScenario(MealyAutomaton automaton, int length,
                                           List<Collection<MealyTransition>> visitedTransitions, Random random) {
        String events = "", actions = "";

        MealyNode curNode = automaton.startState();
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                events += "; ";
                actions += "; ";
            }

            if (curNode.transitions().isEmpty()) {
                throw new RuntimeException("There is no outcoming transitions from node number " + curNode.number());
            }
                        
            MealyTransition transition = null;
            Collection<MealyTransition> currentVisited = visitedTransitions.get(curNode.number());
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
            
            events += transition.event() + "[" + transition.expr() + "]";
            actions += transition.actions();

            curNode = transition.dst();
        }

        return events + "\n" + actions + "\n";
    }

    static private int[] getRandomLength(int scenariosCount, int minLength, int maxLength, int sumLength, Random random) {
        assert 0 < minLength && minLength <= maxLength;
        assert minLength * scenariosCount <= sumLength;
        assert sumLength <= maxLength * scenariosCount;

        int[] length = new int[scenariosCount];
        for (int i = 0; i < scenariosCount; i++) {
            length[i] = sumLength / scenariosCount;
        }
        for (int i = 0; i < sumLength % scenariosCount; i++) {
            length[i]++;
        }

        for (int i = 0; i < scenariosCount; i++) {
            int s1 = random.nextInt(scenariosCount);
            int s2 = random.nextInt(scenariosCount);

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
