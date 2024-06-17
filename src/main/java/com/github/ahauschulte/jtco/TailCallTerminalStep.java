package com.github.ahauschulte.jtco;

import com.github.ahauschulte.jtco.TailCall.TailCallStep;

final class TailCallTerminalStep<T> extends TailCallStep<T> {
    private final T result;

    TailCallTerminalStep(final T result) {
        this.result = result;
    }

    @Override
    public T getResult() {
        return result;
    }

    @Override
    TailCallStep<T> proceed() {
        return this;
    }

    @Override
    boolean isTerminalStep() {
        return true;
    }
}
