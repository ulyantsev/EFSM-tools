/**
 * IAutomataReader.java, 01.03.2008
 */
package qbf.egorov.statemachine.io;

import qbf.egorov.statemachine.IControlledObject;
import qbf.egorov.statemachine.IEventProvider;
import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateMachine;
import qbf.egorov.statemachine.impl.AutomataFormatException;

import java.util.Map;
import java.io.Closeable;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IAutomataReader extends Closeable {

    IStateMachine<? extends IState> readRootStateMachine() throws AutomataFormatException;
    Map<String, IEventProvider> readEventProviders() throws AutomataFormatException;
    Map<String, IControlledObject> readControlledObjects() throws AutomataFormatException;
    Map<String, ? extends IStateMachine<? extends IState>> readStateMachines() throws AutomataFormatException;
}
