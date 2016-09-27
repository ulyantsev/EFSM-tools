package exception;

/**
 * (c) Igor Buzhinsky
 */

import structures.mealy.MealyAutomaton;

public class AutomatonFoundException extends Exception {
    public final MealyAutomaton automaton;

    public AutomatonFoundException(MealyAutomaton automaton) {
        this.automaton = automaton;
    }
    
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}