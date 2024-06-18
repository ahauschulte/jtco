package com.github.ahauschulte.jtco;

import com.github.ahauschulte.jtco.TailCall.TailCallStep;

final class TailCallTerminalStep<T> implements TailCallStep<T> {
    private final T result;

    TailCallTerminalStep(final T result) {
        this.result = result;
    }

    @Override
    public T evaluate() {
        return result;
    }
}
