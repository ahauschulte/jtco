package io.github.ahauschulte.jtco;

final class TailCallTerminalStep<T> implements TailCall<T> {
    private final T result;

    TailCallTerminalStep(final T result) {
        this.result = result;
    }

    @Override
    public T evaluate() {
        return result;
    }
}
