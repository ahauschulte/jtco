package io.github.ahauschulte.jtco.primitive;

import io.github.ahauschulte.jtco.ContinuationStep;
import io.github.ahauschulte.jtco.TailCall;
import io.github.ahauschulte.jtco.TailStep;
import io.github.ahauschulte.jtco.TerminalStep;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents a tail call returning a {@code byte}, facilitating tail call optimization with zero boxing.
 *
 * <p>This interface allows recursive algorithms returning primitive {@code byte} values to be executed
 * iteratively without risking a {@link StackOverflowError} and without allocating boxed {@link Byte} objects.
 *
 * <p><b>Example</b>
 * {@snippet lang = "java":
 * import io.github.ahauschulte.jtco.primitive.ByteTailCall;
 *
 * public class ByteSum {
 *     private static ByteTailCall sum(final int n, final byte acc) {
 *         if (n <= 0) {
 *             return ByteTailCall.done(acc);
 *         }
 *         return ByteTailCall.call(() -> sum(n - 1, (byte) (acc + 1)));
 *     }
 *
 *     public static byte compute(final int n) {
 *         return sum(n, (byte) 0).eval();
 *     }
 * }
 * }
 */
public sealed interface ByteTailCall extends TailStep permits ByteTailCall.Terminal, ByteTailCall.Continuation {

    /**
     * Evaluates this {@link ByteTailCall} and all subsequent calls within the call chain with zero boxing.
     *
     * <p>If a {@link RuntimeException} is raised during the evaluation of the tail call chain, this method will
     * propagate that exact {@code RuntimeException}.
     *
     * @return the unboxed {@code byte} result of the tail call
     * @throws NullPointerException if a {@code TailCall} in the tail call chain is {@code null}
     */
    byte eval();

    /**
     * Adapts this unboxed tail call into a boxed {@link TailCall} of {@link Byte}.
     *
     * <p>Evaluation remains unboxed until explicitly evaluated via {@link TailCall#evaluate()}.
     *
     * @return a boxed {@link TailCall} representing this calculation
     */
    default TailCall<Byte> boxed() {
        return TailCall.continueWith(() -> TailCall.terminateWith(eval()));
    }

    /**
     * Creates the terminal step in the tail call chain with the given unboxed result.
     *
     * @param result the result of the tail call chain
     * @return the terminal step in the tail call chain
     */
    static ByteTailCall terminateWith(final byte result) {
        return new Terminal(result);
    }

    /**
     * Concise alias for {@link #terminateWith(byte)}.
     *
     * @param result the result of the tail call chain
     * @return the terminal step in the tail call chain
     */
    static ByteTailCall done(final byte result) {
        return terminateWith(result);
    }

    /**
     * Creates a continuation step in the tail call chain with the given supplier for the next step within the chain.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static ByteTailCall continueWith(final Supplier<ByteTailCall> nextTailCallSupplier) {
        return new Continuation(nextTailCallSupplier);
    }

    /**
     * Concise alias for {@link #continueWith(Supplier)}.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static ByteTailCall call(final Supplier<ByteTailCall> nextTailCallSupplier) {
        return continueWith(nextTailCallSupplier);
    }

    /**
     * Interleaves tail calls, iteratively advancing all supplied tail calls together without boxing.
     *
     * @param tailCallSuppliers a list of {@link Supplier} objects that provide {@link ByteTailCall} instances
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any of its elements is {@code null},
     *                              or if a tail call supplier returns {@code null}
     */
    static void interleave(final List<Supplier<ByteTailCall>> tailCallSuppliers) {
        TailStep.interleave(tailCallSuppliers);
    }

    /**
     * Represents the terminal step containing the unboxed result.
     *
     * @param result the final unboxed {@code byte} value
     */
    record Terminal(byte result) implements ByteTailCall, TerminalStep {

        @Override
        public byte eval() {
            return result;
        }

        @Override
        public ByteTailCall proceed() {
            throw new UnsupportedOperationException("Terminal step cannot proceed to another tail call");
        }
    }

    /**
     * Represents a continuation step that advances to the next step via a supplier.
     *
     * @param nextTailCallSupplier the supplier for the next tail call step
     */
    record Continuation(Supplier<ByteTailCall> nextTailCallSupplier) implements ByteTailCall, ContinuationStep {

        /**
         * Validates the continuation supplier.
         *
         * @param nextTailCallSupplier the supplier for the next tail call step
         */
        public Continuation {
            Objects.requireNonNull(nextTailCallSupplier, "nextTailCallSupplier must not be null");
        }

        @Override
        public byte eval() {
            ByteTailCall step = this;
            while (step instanceof Continuation continuation) {
                step = Objects.requireNonNull(continuation.proceed(), "No tail call in the chain may be null");
            }
            return ((Terminal) step).result();
        }

        @Override
        public ByteTailCall proceed() {
            return Objects.requireNonNull(nextTailCallSupplier.get(), "Tail call must not be null");
        }
    }
}
