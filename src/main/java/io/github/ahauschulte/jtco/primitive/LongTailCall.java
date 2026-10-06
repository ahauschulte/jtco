package io.github.ahauschulte.jtco.primitive;

import io.github.ahauschulte.jtco.ContinuationStep;
import io.github.ahauschulte.jtco.TailCall;
import io.github.ahauschulte.jtco.TailStep;
import io.github.ahauschulte.jtco.TerminalStep;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents a tail call returning a {@code long}, facilitating tail call optimization with zero boxing.
 *
 * <p>This interface allows recursive algorithms returning primitive {@code long} values to be executed
 * iteratively without risking a {@link StackOverflowError} and without allocating boxed {@link Long} objects.
 *
 * <p><b>Example</b>
 * {@snippet lang = "java":
 * import io.github.ahauschulte.jtco.primitive.LongTailCall;
 *
 * public class Fibonacci {
 *     private static LongTailCall fib(final long n, final long a, final long b) {
 *         if (n == 0) {
 *             return LongTailCall.done(a);
 *         }
 *         return LongTailCall.call(() -> fib(n - 1, b, a + b));
 *     }
 *
 *     public static long compute(final long n) {
 *         return fib(n, 0L, 1L).eval();
 *     }
 * }
 * }
 */
public sealed interface LongTailCall extends TailStep permits LongTailCall.Terminal, LongTailCall.Continuation {

    /**
     * Evaluates this {@link LongTailCall} and all subsequent calls within the call chain with zero boxing.
     *
     * <p>If a {@link RuntimeException} is raised during the evaluation of the tail call chain, this method will
     * propagate that exact {@code RuntimeException}.
     *
     * @return the unboxed {@code long} result of the tail call
     * @throws NullPointerException if a {@code TailCall} in the tail call chain is {@code null}
     */
    long eval();

    /**
     * Adapts this unboxed tail call into a boxed {@link TailCall} of {@link Long}.
     *
     * <p>Evaluation remains unboxed until explicitly evaluated via {@link TailCall#evaluate()}.
     *
     * @return a boxed {@link TailCall} representing this calculation
     */
    default TailCall<Long> boxed() {
        return TailCall.continueWith(() -> TailCall.terminateWith(eval()));
    }

    /**
     * Creates the terminal step in the tail call chain with the given unboxed result.
     *
     * @param result the result of the tail call chain
     * @return the terminal step in the tail call chain
     */
    static LongTailCall terminateWith(final long result) {
        return new Terminal(result);
    }

    /**
     * Concise alias for {@link #terminateWith(long)}.
     *
     * @param result the result of the tail call chain
     * @return the terminal step in the tail call chain
     */
    static LongTailCall done(final long result) {
        return terminateWith(result);
    }

    /**
     * Creates a continuation step in the tail call chain with the given supplier for the next step within the chain.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static LongTailCall continueWith(final Supplier<LongTailCall> nextTailCallSupplier) {
        return new Continuation(nextTailCallSupplier);
    }

    /**
     * Concise alias for {@link #continueWith(Supplier)}.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static LongTailCall call(final Supplier<LongTailCall> nextTailCallSupplier) {
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
     * @return a list containing the final terminal {@link LongTailCall} instances for each supplier
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    @SuppressWarnings("unchecked")
    static List<LongTailCall> interleaveAll(final List<? extends Supplier<LongTailCall>> tailCallSuppliers) {
        return (List<LongTailCall>) (List<?>) TailStep.interleaveAll(tailCallSuppliers);
    }

    /**
     * Interleaves multiple tail calls, advancing them in unison until <b>at least one</b> reaches its terminal step.
     *
     * @param tailCallSuppliers the list of tail call suppliers to interleave
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    static void interleaveAny(final List<? extends Supplier<LongTailCall>> tailCallSuppliers) {
        TailStep.interleaveAny(tailCallSuppliers);
    }

    /**
     * Interleaves tail calls, iteratively advancing all supplied tail calls together without boxing.
     *
     * <p>This method is an alias for {@link #interleaveAny(List)}. If you wish to advance all tail calls to their
     * individual completion regardless of unequal step counts, use {@link #interleaveAll(List)}.
     *
     * @param tailCallSuppliers a list of {@link Supplier} objects that provide {@link LongTailCall} instances
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any of its elements is {@code null},
     *                              or if a tail call supplier returns {@code null}
     */
    static void interleave(final List<? extends Supplier<LongTailCall>> tailCallSuppliers) {
        interleaveAny(tailCallSuppliers);
    }

    /**
     * Represents the terminal step containing the unboxed result.
     *
     * @param result the final unboxed {@code long} value
     */
    record Terminal(long result) implements LongTailCall, TerminalStep {

        @Override
        public long eval() {
            return result;
        }

        @Override
        public LongTailCall proceed() {
            throw new UnsupportedOperationException("Terminal step cannot proceed to another tail call");
        }
    }

    /**
     * Represents a continuation step that advances to the next step via a supplier.
     *
     * @param nextTailCallSupplier the supplier for the next tail call step
     */
    record Continuation(Supplier<LongTailCall> nextTailCallSupplier) implements LongTailCall, ContinuationStep {

        /**
         * Validates the continuation supplier.
         *
         * @param nextTailCallSupplier the supplier for the next tail call step
         */
        public Continuation {
            Objects.requireNonNull(nextTailCallSupplier, "nextTailCallSupplier must not be null");
        }

        @Override
        public long eval() {
            LongTailCall step = this;
            while (step instanceof Continuation continuation) {
                step = Objects.requireNonNull(continuation.proceed(), "No tail call in the chain may be null");
            }
            return ((Terminal) step).result();
        }

        @Override
        public LongTailCall proceed() {
            return Objects.requireNonNull(nextTailCallSupplier.get(), "Tail call must not be null");
        }
    }
}
