/* 
 * Developed by eVelopers Corporation, 2010
 */
package ru.ifmo.ctddev.genetic.transducer.io;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import ru.ifmo.automata.statemachine.IControlledObject;
import ru.ifmo.automata.statemachine.IEventProvider;
import ru.ifmo.ctddev.genetic.transducer.algorithm.FST;
import ru.ifmo.ctddev.genetic.transducer.algorithm.Transition;

import java.io.IOException;
import java.io.Writer;

/**
 * @author kegorov
 *         Date: Mar 24, 2010
 */
public class UnimodModelWriter {

    private static final String START = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!DOCTYPE model PUBLIC \"-//eVelopers Corp.//DTD State machine model V1.0//EN\" \"http://www.evelopers.com/dtd/unimod/statemachine.dtd\">\n" +
            "<model name=\"Model1\">\n" +
            "  <controlledObject class=\"%s\" name=\"o1\"/>\n" +
            "  <eventProvider class=\"%s\" name=\"p1\">\n" +
            "    <association clientRole=\"p1\" targetRef=\"A1\"/>\n" +
            "  </eventProvider>\n" +
            "  <rootStateMachine>\n" +
            "    <stateMachineRef name=\"A1\"/>\n" +
            "  </rootStateMachine>\n" +
            "  <stateMachine name=\"A1\">\n" +
            "    <configStore class=\"com.evelopers.unimod.runtime.config.DistinguishConfigManager\"/>\n" +
            "    <association clientRole=\"A1\" supplierRole=\"o1\" targetRef=\"o1\"/>\n" +
            "    <state name=\"Top\" type=\"NORMAL\">\n";
    private static final String END = "  </stateMachine>\n" +
            "</model>";

    private static final String STATE_TOP_END       = "    </state>\n";
    private static final String INIT_STATE          = "      <state name=\"%s\" type=\"INITIAL\"/>\n";
    private static final String STATE               = "      <state name=\"%s\" type=\"NORMAL\"/>\n";
    private static final String TRANSITION          = "    <transition event=\"%s\" sourceRef=\"%s\" targetRef=\"%s\">\n";
    private static final String TRANSITION_COND     = "    <transition event=\"%s\" guard=\"%s\" sourceRef=\"%s\" targetRef=\"%s\">\n";
    private static final String TRANSITION_END      = "    </transition>\n";
    private static final String ACTION              = "      <outputAction ident=\"o1.%s\"/>\n";

    public static void write(Writer out, FST fst, String controlledObjectClass, String eventProviderClass)
            throws IOException {
        out.write(String.format(START, controlledObjectClass, eventProviderClass));

        for (int i = 0; i < fst.getStates().length; i++) {
            String state = (i != fst.getInitialState()) ? STATE : INIT_STATE;
            out.write(String.format(state, formatStateName(i)));
        }
        out.write(STATE_TOP_END);

        for (int i = 0; i < fst.getStates().length; i++) {
            writeTransitions(out, i, fst.getStates()[i]);
        }

        out.write(END);
    }


    public static void write(Writer out, FST fst, Class<? extends IControlledObject> co,
                      Class<? extends IEventProvider> ep) throws IOException {
        write(out, fst, co.getName(), ep.getName());
    }

    protected static void writeTransitions(Writer out, int i, Transition[] transitions) throws IOException {
        for (Transition t: transitions) {
            String cond = StringUtils.substringBetween(t.getInput(), "[", "]");
            String event = StringUtils.substringBefore(t.getInput(), "[").trim();
            
            if (StringUtils.isBlank(cond)) {
                out.write(String.format(TRANSITION, event,
                        formatStateName(i), formatStateName(t.getNewState())));
            } else {
                cond = StringEscapeUtils.escapeXml(cond);
                out.write(String.format(TRANSITION_COND, event, cond,
                        formatStateName(i), formatStateName(t.getNewState())));
            }
            for (String z: t.getOutput()) {
                out.write(String.format(ACTION, z));
            }
            out.write(String.format(TRANSITION_END));
        }
    }

    protected static String formatStateName(int i) {
        return "s" + i;
    }
}
