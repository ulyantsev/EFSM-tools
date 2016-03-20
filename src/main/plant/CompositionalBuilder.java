package main.plant;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import main.plant.AprosIOScenarioCreator.Configuration;
import main.plant.AprosIOScenarioCreator.Dataset;
import main.plant.AprosIOScenarioCreator.Parameter;
import structures.plant.NondetMooreAutomaton;

public class CompositionalBuilder {
	private final static Configuration CONF1 = AprosIOScenarioCreator.CONFIGURATION_PROTECTION1;
	private final static Configuration CONF2 = AprosIOScenarioCreator.CONFIGURATION_PROTECTION7;
	
	private static void compose(NondetMooreAutomaton a1, NondetMooreAutomaton a2) {
		final Deque<?> q = new ArrayDeque<>();
		/*
		 * queue Q <- all consistent pairs of initial states
		 * while !Q.isEmpty()
		 *   q = (q_1, q_2) <- Q
		 *   foreach pair of consistent outgoing inputs
		 *   	if there is no internal connection conflict
		 *         (current output and outgoing transition input
		 *   	   AND the destination present in the entire trace set
		 *   		 Q <- q
		 * remove internal connections (duplicate inputs and outputs)
		 */
	}
	
	public static void main(String[] args) throws FileNotFoundException {		
		// 1. Unify parameters
		for (Parameter p : CONF1.allParameters()) {
			for (Parameter q : CONF2.allParameters()) {
				Parameter.unify(p, q);
			}
		}
		System.out.println(CONF1);
		System.out.println();
		System.out.println(CONF2);
		System.out.println();
		if (CONF1.intervalSec != CONF2.intervalSec) {
			System.err.println("Incompatible intervals, stopping.");
			return;
		}
		
		// 2. Load the dataset
		final Dataset ds = new Dataset(CONF1);
		
		// 3. Run scenario generation & automaton builders
		final List<String> params1 = AprosIOScenarioCreator.generateScenarios(CONF1, ds);
		System.out.println();
		PlantBuilderMain.main(params1.toArray(new String[params1.size()]));
		NondetMooreAutomaton a1 = null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("automaton.bin"))) {
			a1 = (NondetMooreAutomaton) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		System.out.println();
		
		final List<String> params2 = AprosIOScenarioCreator.generateScenarios(CONF2, ds);
		System.out.println();
		PlantBuilderMain.main(params2.toArray(new String[params2.size()]));
		NondetMooreAutomaton a2 = null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("automaton.bin"))) {
			a2 = (NondetMooreAutomaton) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		System.out.println();

		System.out.println(a1);
		System.out.println(a2);
		
		// 4. Compose
		compose(a1, a2);
	}
}
