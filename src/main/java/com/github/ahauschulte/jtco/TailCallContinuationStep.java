package com.github.ahauschulte.jtco;

import com.github.ahauschulte.jtco.TailCall.TailCallStep;

import java.util.Objects;
import java.util.function.Supplier;

final class TailCallContinuationStep<T> implements TailCallStep<T> {
    private final Supplier<TailCallStep<T>> nextTailCallStep;

    TailCallContinuationStep(final Supplier<TailCallStep<T>> nextTailCallStep) {
        this.nextTailCallStep = Objects.requireNonNull(nextTailCallStep, "nextTailCallStep must not be null");
    }

    @Override
    public T evaluate() {
        TailCallStep<T> tailCallStep = this;
        while (tailCallStep instanceof final TailCallContinuationStep<T> tailCallContinuationStep) {
            tailCallStep = tailCallContinuationStep.proceed();
        }
        return tailCallStep.evaluate();
    }

    private TailCallStep<T> proceed() {
        return nextTailCallStep.get();
    }
}
