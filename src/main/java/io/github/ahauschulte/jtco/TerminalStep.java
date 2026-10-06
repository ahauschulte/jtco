package io.github.ahauschulte.jtco;

/**
 * Represents a terminal step in a tail call chain that holds the final evaluation result.
 */
public interface TerminalStep extends TailStep {

    @Override
    default boolean isTerminal() {
        return true;
    }

    @Override
    default TailStep proceed() {
        throw new UnsupportedOperationException("Terminal step cannot proceed to another tail call");
    }
}
