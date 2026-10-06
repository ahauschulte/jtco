package io.github.ahauschulte.jtco;

/**
 * Represents a continuation step in a tail call chain that can advance to the next step.
 */
public interface ContinuationStep extends TailStep {

    @Override
    default boolean isTerminal() {
        return false;
    }
}
