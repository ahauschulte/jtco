package com.github.ahauschulte.jtco;

@FunctionalInterface
public interface TailCall<T> {

    T getResult();

    abstract sealed class TailCallStep<T> implements TailCall<T>
            permits TailCallContinuationStep, TailCallTerminalStep {
        abstract TailCallStep<T> proceed();

        abstract boolean isTerminalStep();
    }

    static <T> TailCallStep<T> terminateWith(final T result) {
        return new TailCallTerminalStep<>(result);
    }

    static <T> TailCallStep<T> continueWith(final TailCall<TailCallStep<T>> nextTailCallStep) {
        return new TailCallContinuationStep<>(nextTailCallStep);
    }
}
