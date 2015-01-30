/**
 * IAutomataReader.java, 01.03.2008
 */
package ru.ifmo.automata.statemachine.io;

import ru.ifmo.automata.statemachine.impl.AutomataFormatException;
import ru.ifmo.automata.statemachine.IStateMachine;
import ru.ifmo.automata.statemachine.IEventProvider;
import ru.ifmo.automata.statemachine.IControlledObject;
import ru.ifmo.automata.statemachine.IState;

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
