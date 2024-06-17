package com.github.ahauschulte.jtco;

import com.github.ahauschulte.jtco.TailCall.TailCallStep;

import java.util.Objects;

final class TailCallContinuationStep<T> extends TailCallStep<T> {
    private final TailCall<TailCallStep<T>> nextTailCallStep;

    TailCallContinuationStep(final TailCall<TailCallStep<T>> nextTailCallStep) {
        this.nextTailCallStep = Objects.requireNonNull(nextTailCallStep, "nextTailCallStep must not be null");
    }

    @Override
    public T getResult() {
        TailCallStep<T> tailCallStep = this;
        do {
            tailCallStep = tailCallStep.proceed();
        } while (!tailCallStep.isTerminalStep());
        return tailCallStep.getResult();
    }

    @Override
    TailCallStep<T> proceed() {
        return nextTailCallStep.getResult();
    }

    @Override
    boolean isTerminalStep() {
        return false;
    }
}
