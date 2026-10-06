package io.github.ahauschulte.jtco.primitive;

import io.github.ahauschulte.jtco.ContinuationStep;
import io.github.ahauschulte.jtco.TailCall;
import io.github.ahauschulte.jtco.TailStep;
import io.github.ahauschulte.jtco.TerminalStep;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Represents a tail call returning a {@code double}, facilitating tail call optimization with zero boxing.
 *
 * <p>This interface allows recursive algorithms returning primitive {@code double} values to be executed
 * iteratively without risking a {@link StackOverflowError} and without allocating boxed {@link Double} objects.
 *
 * <p><b>Example</b>
 * {@snippet lang = "java":
 * import io.github.ahauschulte.jtco.primitive.DoubleTailCall;
 *
 * public class DoubleSum {
 *     private static DoubleTailCall sum(final int n, final double acc) {
 *         if (n <= 0) {
 *             return DoubleTailCall.done(acc);
 *         }
 *         return DoubleTailCall.call(() -> sum(n - 1, acc + 0.5));
 *     }
 *
 *     public static double compute(final int n) {
 *         return sum(n, 0.0).eval();
 *     }
 * }
 * }
 */
public sealed interface DoubleTailCall extends TailStep permits DoubleTailCall.Terminal, DoubleTailCall.Continuation {

    /**
     * Evaluates this {@link DoubleTailCall} and all subsequent calls within the call chain with zero boxing.
     *
     * <p>If a {@link RuntimeException} is raised during the evaluation of the tail call chain, this method will
     * propagate that exact {@code RuntimeException}.
     *
     * @return the unboxed {@code double} result of the tail call
     * @throws NullPointerException if a {@code TailCall} in the tail call chain is {@code null}
     */
    double eval();

    /**
     * Asynchronously evaluates this unboxed tail call on the common {@link ForkJoinPool}.
     *
     * @return a {@link CompletableFuture} containing the evaluated {@link Double} result
     */
    default CompletableFuture<Double> evalAsync() {
        return evalAsync(ForkJoinPool.commonPool());
    }

    /**
     * Asynchronously evaluates this unboxed tail call using the specified executor.
     *
     * @param executor the executor to run the evaluation on
     * @return a {@link CompletableFuture} containing the evaluated {@link Double} result
     * @throws NullPointerException if {@code executor} is {@code null}
     */
    default CompletableFuture<Double> evalAsync(final Executor executor) {
        Objects.requireNonNull(executor, "executor must not be null");
        return CompletableFuture.supplyAsync(this::eval, executor);
    }

    /**
     * Adapts this unboxed tail call into a boxed {@link TailCall} of {@link Double}.
     *
     * <p>Evaluation remains unboxed until explicitly evaluated via {@link TailCall#evaluate()}.
     *
     * @return a boxed {@link TailCall} representing this calculation
     */
    default TailCall<Double> boxed() {
        return TailCall.continueWith(() -> TailCall.terminateWith(eval()));
    }

    /**
     * Creates the terminal step in the tail call chain with the given unboxed result.
     *
     * @param result the result of the tail call chain
     * @return the terminal step in the tail call chain
     */
    static DoubleTailCall terminateWith(final double result) {
        return new Terminal(result);
    }

    /**
     * Concise alias for {@link #terminateWith(double)}.
     *
     * @param result the result of the tail call chain
     * @return the terminal step in the tail call chain
     */
    static DoubleTailCall done(final double result) {
        return terminateWith(result);
    }

    /**
     * Creates a continuation step in the tail call chain with the given supplier for the next step within the chain.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static DoubleTailCall continueWith(final Supplier<DoubleTailCall> nextTailCallSupplier) {
        return new Continuation(nextTailCallSupplier);
    }

    /**
     * Concise alias for {@link #continueWith(Supplier)}.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static DoubleTailCall call(final Supplier<DoubleTailCall> nextTailCallSupplier) {
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
     * @return a list containing the final terminal {@link DoubleTailCall} instances for each supplier
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    @SuppressWarnings("unchecked")
    static List<DoubleTailCall> interleaveAll(final List<? extends Supplier<DoubleTailCall>> tailCallSuppliers) {
        return (List<DoubleTailCall>) (List<?>) TailStep.interleaveAll(tailCallSuppliers);
    }

    /**
     * Interleaves multiple tail calls, advancing them in unison until <b>at least one</b> reaches its terminal step.
     *
     * @param tailCallSuppliers the list of tail call suppliers to interleave
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    static void interleaveAny(final List<? extends Supplier<DoubleTailCall>> tailCallSuppliers) {
        TailStep.interleaveAny(tailCallSuppliers);
    }

    /**
     * Interleaves tail calls, iteratively advancing all supplied tail calls together without boxing.
     *
     * <p>This method is an alias for {@link #interleaveAny(List)}. If you wish to advance all tail calls to their
     * individual completion regardless of unequal step counts, use {@link #interleaveAll(List)}.
     *
     * @param tailCallSuppliers a list of {@link Supplier} objects that provide {@link DoubleTailCall} instances
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any of its elements is {@code null},
     *                              or if a tail call supplier returns {@code null}
     */
    static void interleave(final List<? extends Supplier<DoubleTailCall>> tailCallSuppliers) {
        interleaveAny(tailCallSuppliers);
    }

    /**
     * Interleaves multiple tail calls asynchronously on the provided executor.
     *
     * @param tailCallSuppliers the list of tail call suppliers
     * @param executor          the executor to execute the interleaving loop on
     * @return a {@link CompletableFuture} that resolves with the list of terminal tail calls once all complete
     * @throws NullPointerException if {@code tailCallSuppliers} or {@code executor} is {@code null}
     */
    @SuppressWarnings("unchecked")
    static CompletableFuture<List<DoubleTailCall>> interleaveAsync(
            final List<? extends Supplier<DoubleTailCall>> tailCallSuppliers,
            final Executor executor
    ) {
        return (CompletableFuture<List<DoubleTailCall>>) (CompletableFuture<?>) TailStep.interleaveAsync(tailCallSuppliers, executor);
    }

    /**
     * Interleaves multiple tail calls asynchronously using {@link ForkJoinPool#commonPool()}.
     *
     * @param tailCallSuppliers the list of tail call suppliers
     * @return a {@link CompletableFuture} that resolves with the list of terminal tail calls once all complete
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}
     */
    static CompletableFuture<List<DoubleTailCall>> interleaveAsync(
            final List<? extends Supplier<DoubleTailCall>> tailCallSuppliers
    ) {
        return interleaveAsync(tailCallSuppliers, ForkJoinPool.commonPool());
    }

    /**
     * Represents the terminal step containing the unboxed result.
     *
     * @param result the final unboxed {@code double} value
     */
    record Terminal(double result) implements DoubleTailCall, TerminalStep {

        @Override
        public double eval() {
            return result;
        }

        @Override
        public DoubleTailCall proceed() {
            throw new UnsupportedOperationException("Terminal step cannot proceed to another tail call");
        }
    }

    /**
     * Represents a continuation step that advances to the next step via a supplier.
     *
     * @param nextTailCallSupplier the supplier for the next tail call step
     */
    record Continuation(Supplier<DoubleTailCall> nextTailCallSupplier) implements DoubleTailCall, ContinuationStep {

        /**
         * Validates the continuation supplier.
         *
         * @param nextTailCallSupplier the supplier for the next tail call step
         */
        public Continuation {
            Objects.requireNonNull(nextTailCallSupplier, "nextTailCallSupplier must not be null");
        }

        @Override
        public double eval() {
            DoubleTailCall step = this;
            while (step instanceof Continuation continuation) {
                step = Objects.requireNonNull(continuation.proceed(), "No tail call in the chain may be null");
            }
            return ((Terminal) step).result();
        }

        @Override
        public DoubleTailCall proceed() {
            return Objects.requireNonNull(nextTailCallSupplier.get(), "Tail call must not be null");
        }
    }
}
