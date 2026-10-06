package io.github.ahauschulte.jtco.primitive;

import io.github.ahauschulte.jtco.ContinuationStep;
import io.github.ahauschulte.jtco.TailCall;
import io.github.ahauschulte.jtco.TailStep;
import io.github.ahauschulte.jtco.TerminalStep;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents a tail call returning a {@code short}, facilitating tail call optimization with zero boxing.
 *
 * <p>This interface allows recursive algorithms returning primitive {@code short} values to be executed
 * iteratively without risking a {@link StackOverflowError} and without allocating boxed {@link Short} objects.
 *
 * <p><b>Example</b>
 * {@snippet lang = "java":
 * import io.github.ahauschulte.jtco.primitive.ShortTailCall;
 *
 * public class ShortSum {
 *     private static ShortTailCall sum(final int n, final short acc) {
 *         if (n <= 0) {
 *             return ShortTailCall.done(acc);
 *         }
 *         return ShortTailCall.call(() -> sum(n - 1, (short) (acc + 1)));
 *     }
 *
 *     public static short compute(final int n) {
 *         return sum(n, (short) 0).eval();
 *     }
 * }
 * }
 */
public sealed interface ShortTailCall extends TailStep permits ShortTailCall.Terminal, ShortTailCall.Continuation {

    /**
     * Evaluates this {@link ShortTailCall} and all subsequent calls within the call chain with zero boxing.
     *
     * <p>If a {@link RuntimeException} is raised during the evaluation of the tail call chain, this method will
     * propagate that exact {@code RuntimeException}.
     *
     * @return the unboxed {@code short} result of the tail call
     * @throws NullPointerException if a {@code TailCall} in the tail call chain is {@code null}
     */
    short eval();

    /**
     * Adapts this unboxed tail call into a boxed {@link TailCall} of {@link Short}.
     *
     * <p>Evaluation remains unboxed until explicitly evaluated via {@link TailCall#evaluate()}.
     *
     * @return a boxed {@link TailCall} representing this calculation
     */
    default TailCall<Short> boxed() {
        return TailCall.continueWith(() -> TailCall.terminateWith(eval()));
    }

    /**
     * Creates the terminal step in the tail call chain with the given unboxed result.
     *
     * @param result the result of the tail call chain
     * @return the terminal step in the tail call chain
     */
    static ShortTailCall terminateWith(final short result) {
        return new Terminal(result);
    }

    /**
     * Concise alias for {@link #terminateWith(short)}.
     *
     * @param result the result of the tail call chain
     * @return the terminal step in the tail call chain
     */
    static ShortTailCall done(final short result) {
        return terminateWith(result);
    }

    /**
     * Creates a continuation step in the tail call chain with the given supplier for the next step within the chain.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static ShortTailCall continueWith(final Supplier<ShortTailCall> nextTailCallSupplier) {
        return new Continuation(nextTailCallSupplier);
    }

    /**
     * Concise alias for {@link #continueWith(Supplier)}.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static ShortTailCall call(final Supplier<ShortTailCall> nextTailCallSupplier) {
        return continueWith(nextTailCallSupplier);
    }

    /**
     * Interleaves multiple tail calls, iteratively advancing active tail calls until <b>all</b> tail calls
     * have reached their terminal step without boxing.
     *
     * <p>When a tail call reaches its terminal step earlier than others, it remains in its completed state
     * without further advancement, while the remaining non-terminal tail calls continue advancing until
     * every branch has completed.
     *
     * @param tailCallSuppliers the list of tail call suppliers to advance to completion
     * @return a list containing the final terminal {@link ShortTailCall} instances for each supplier
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    @SuppressWarnings("unchecked")
    static List<ShortTailCall> interleaveAll(final List<? extends Supplier<ShortTailCall>> tailCallSuppliers) {
        return (List<ShortTailCall>) (List<?>) TailStep.interleaveAll(tailCallSuppliers);
    }

    /**
     * Interleaves multiple tail calls, advancing them in unison until <b>at least one</b> reaches its terminal step.
     *
     * @param tailCallSuppliers the list of tail call suppliers to interleave
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    static void interleaveAny(final List<? extends Supplier<ShortTailCall>> tailCallSuppliers) {
        TailStep.interleaveAny(tailCallSuppliers);
    }

    /**
     * Interleaves tail calls, iteratively advancing all supplied tail calls together without boxing.
     *
     * <p>This method is an alias for {@link #interleaveAny(List)}. If you wish to advance all tail calls to their
     * individual completion regardless of unequal step counts, use {@link #interleaveAll(List)}.
     *
     * @param tailCallSuppliers a list of {@link Supplier} objects that provide {@link ShortTailCall} instances
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any of its elements is {@code null},
     *                              or if a tail call supplier returns {@code null}
     */
    static void interleave(final List<? extends Supplier<ShortTailCall>> tailCallSuppliers) {
        interleaveAny(tailCallSuppliers);
    }

    /**
     * Represents the terminal step containing the unboxed result.
     *
     * @param result the final unboxed {@code short} value
     */
    record Terminal(short result) implements ShortTailCall, TerminalStep {

        @Override
        public short eval() {
            return result;
        }

        @Override
        public ShortTailCall proceed() {
            throw new UnsupportedOperationException("Terminal step cannot proceed to another tail call");
        }
    }

    /**
     * Represents a continuation step that advances to the next step via a supplier.
     *
     * @param nextTailCallSupplier the supplier for the next tail call step
     */
    record Continuation(Supplier<ShortTailCall> nextTailCallSupplier) implements ShortTailCall, ContinuationStep {

        /**
         * Validates the continuation supplier.
         *
         * @param nextTailCallSupplier the supplier for the next tail call step
         */
        public Continuation {
            Objects.requireNonNull(nextTailCallSupplier, "nextTailCallSupplier must not be null");
        }

        @Override
        public short eval() {
            ShortTailCall step = this;
            while (step instanceof Continuation continuation) {
                step = Objects.requireNonNull(continuation.proceed(), "No tail call in the chain may be null");
            }
            return ((Terminal) step).result();
        }

        @Override
        public ShortTailCall proceed() {
            return Objects.requireNonNull(nextTailCallSupplier.get(), "Tail call must not be null");
        }
    }
}
