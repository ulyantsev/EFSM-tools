package structures.plant;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import scenario.StringActions;
import scenario.StringScenario;

public abstract class PlantScenarioForest {
	protected final Set<MooreNode> roots = new LinkedHashSet<>();
	protected final Set<MooreNode> nodes = new LinkedHashSet<>();

    public Collection<MooreNode> getRoots() {
        return Collections.unmodifiableSet(roots);
    }
    
    public Collection<MooreNode> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    public int rootsCount() {
        return roots.size();
    }
    
    public int nodesCount() {
        return nodes.size();
    }
    
    /*
     * varNumber = -1 for no variable removal
     */
    public void load(String filepath, int varNumber) throws FileNotFoundException, ParseException {
        for (StringScenario scenario : StringScenario.loadScenarios(filepath, varNumber)) {
            addScenario(scenario, 0);
        }
    }
    
    protected abstract void addScenario(StringScenario scenarion, int loopLength);
    protected abstract MooreNode addTransition(MooreNode src, String event, StringActions actions);
    
    protected void checkScenario(StringScenario scenario) {
    	for (int i = 0; i < scenario.size(); i++) {
        	if (scenario.getEvents(i).size() != 1) {
        		throw new RuntimeException("Multi-edges are not supported!");
        	}
    	}
    	
    	if (!scenario.getEvents(0).get(0).isEmpty()) {
    		throw new RuntimeException("The first event must be dummy (i.e. empty string)!");
    	}
    }
}
