/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.statemachine;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kegorov
 *         Date: Jun 17, 2009
 */
public class ControlledObject {
    private final Map<String, Action> actions = new HashMap<>();

    public ControlledObject(String... actions) {
        for (String a: actions) {
            this.actions.put(a, new Action(a));
        }
    }

    public Action getAction(String actionName) {
        return actions.get(actionName);
    }
}
