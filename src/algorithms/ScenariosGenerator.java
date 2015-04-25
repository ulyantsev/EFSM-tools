package algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import scenario.StringScenario;
import structures.Automaton;
import structures.Node;
import structures.Transition;
import actions.StringActions;
import bool.MyBooleanExpression;

public class ScenariosGenerator {
    static public ArrayList<ArrayList<Transition>> getBFSPaths(Automaton automaton) {
    	ArrayList<ArrayList<Transition>> ans = new ArrayList<>();
    	
    	int size = automaton.statesCount();
    	@SuppressWarnings("unchecked")
		ArrayList<Transition>[] shortestPaths = new ArrayList[size];
    	
    	ArrayList<Node> order = new ArrayList<>();
    	order.add(automaton.getStartState());
    	shortestPaths[automaton.getStartState().getNumber()] = new ArrayList<>();
    	
    	for (int i = 0; i < size; i++) {
    		Node current = order.get(i);
    		ArrayList<Transition> path = shortestPaths[current.getNumber()];
    		
    		for (Transition t : current.getTransitions()) {
    			ArrayList<Transition> newPath = new ArrayList<>(path);
    			newPath.add(t);
    			ans.add(newPath);
    			
    			if (shortestPaths[t.getDst().getNumber()] == null) {
    				order.add(t.getDst());
    				shortestPaths[t.getDst().getNumber()] = newPath;
    			}
    		}
    	}
    	
    	return ans;
    }
	
    static public String pathToScenario(ArrayList<Transition> path) {
    	ArrayList<String> events = new ArrayList<String>();
    	ArrayList<MyBooleanExpression> expressions = new ArrayList<>();
    	ArrayList<StringActions> actions = new ArrayList<>();
    	
    	for (Transition t : path) {
    		events.add(t.getEvent());
    		expressions.add(t.getExpr());
    		actions.add(t.getActions());
    	}
    	
    	return new StringScenario(true, events, expressions, actions).toString();
    }
    
    static public String generateScenariosWithBFS(Automaton automaton) {
		ArrayList<ArrayList<Transition>> paths = getBFSPaths(automaton);
		int lenBFS = 0;
		for (ArrayList<Transition> path : paths) {
			lenBFS += path.size();
		}
 
    	return generateScenariosWithBFS(automaton, lenBFS, null);
    }
    
	static public String generateScenariosWithBFS(Automaton automaton, int sumLength, Random random) {
		ArrayList<ArrayList<Transition>> paths = getBFSPaths(automaton);
		int lenBFS = 0;
		for (ArrayList<Transition> path : paths) {
			lenBFS += path.size();
		}
		if (lenBFS > sumLength) {
			throw new RuntimeException("Impossible to generate scenarios wits summary length [" + sumLength + "] with BFS");
		}
		
		for (int i = lenBFS; i < sumLength; i++) {
			int randomPathNum = random.nextInt(paths.size());
			ArrayList<Transition> randomPath = paths.get(randomPathNum);
			
			Node lastNode = randomPath.get(randomPath.size() - 1).getDst();
			int randomTransitionNumber = random.nextInt(lastNode.transitionsCount());
			Transition randomTransition = lastNode.getTransitions().toArray(new Transition[0])[randomTransitionNumber];
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
	
	static public String generateScenarios(Automaton automaton, int scenariosCount, int minLength, int maxLength,
            int sumLength, Random random) {
    	int[] length = getRandomLength(scenariosCount, minLength, maxLength, sumLength, random);

    	List<Collection<Transition>> visitedTransitions = new ArrayList<>(); 
    	for (int i = 0; i < automaton.getStates().size(); i++) {
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

    static private String generateScenario(Automaton automaton, int length, List<Collection<Transition>> visitedTransitions, Random random) {
        String events = "", actions = "";

        Node curNode = automaton.getStartState();
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                events += "; ";
                actions += "; ";
            }

            if (curNode.getTransitions().isEmpty()) {
                throw new RuntimeException("There is no outcoming transitions from node number " + curNode.getNumber());
            }
                        
            Transition transition = null;
            Collection<Transition> currentVisited = visitedTransitions.get(curNode.getNumber());
            if (currentVisited.size() == curNode.getTransitions().size()) {
            	int transitionNum = random.nextInt(curNode.getTransitions().size());
            	transition = curNode.getTransitions().toArray(new Transition[0])[transitionNum];
            } else {
            	for (Transition unvisited : curNode.getTransitions()) {
            		boolean was = false;
            		for (Transition visited : currentVisited) {
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
            
            events += transition.getEvent() + "[" + transition.getExpr() + "]";
            actions += transition.getActions();

            curNode = transition.getDst();
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
