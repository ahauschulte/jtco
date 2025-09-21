package io.github.ahauschulte.jtco;

import java.util.Objects;
import java.util.function.Supplier;

final class TailCallContinuationStep<T> implements TailCall<T> {
    private final Supplier<TailCall<T>> nextTailCallSupplier;

    TailCallContinuationStep(final Supplier<TailCall<T>> nextTailCallSupplier) {
        this.nextTailCallSupplier = Objects.requireNonNull(nextTailCallSupplier,
                "nextTailCallSupplier must not be null");
    }

    @Override
    public T evaluate() {
        TailCall<T> tailCallStep = this;
        while (tailCallStep instanceof final TailCallContinuationStep<T> tailCallContinuationStep) {
            tailCallStep = Objects.requireNonNull(tailCallContinuationStep.proceed(),
                    "No tail call in the chain may be null");
        }
        return tailCallStep.evaluate();
    }

    TailCall<T> proceed() {
        return Objects.requireNonNull(nextTailCallSupplier.get(), "Tail call must not be null");
    }
}
