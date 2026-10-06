package io.github.ahauschulte.jtco;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Represents a tail call, facilitating tail call optimization by enabling iterative evaluation.
 *
 * <p>This interface allows recursive algorithms to be executed in an iterative manner. By converting recursive calls
 * into a series of iterative steps, it helps to avoid the deep call stacks typically associated with recursion, thus
 * preventing stack overflow errors.
 *
 * <p><b>Example</b>
 * {@snippet lang = "java":
 * import io.github.ahauschulte.jtco.TailCall;
 * import java.math.BigInteger;
 *
 * public class Factorial {
 *     private static TailCall<BigInteger> factorial(final int n, final BigInteger prevAcc) { // @highlight substring="TailCall"
 *         if (n == 0) {
 *             return TailCall.terminateWith(prevAcc); // @highlight substring="TailCall.terminateWith"
 *         } else {
 *             return TailCall.continueWith(() -> factorial(n - 1, BigInteger.valueOf(n).multiply(prevAcc))); // @highlight substring="TailCall.continueWith"
 *         }
 *     }
 *
 *     private static BigInteger factorial(final int n) {
 *         if (n < 0) throw new IllegalArgumentException("n must be >= 0");
 *         return factorial(n, BigInteger.ONE).evaluate(); // @highlight substring=".evaluate()"
 *     }
 *
 *     public static void main(final String[] args) {
 *         final int n = 1_000;
 *         System.out.printf("factorial(%d): %d%n", n, factorial(n));
 *     }
 * }
 *}
 *
 * @param <T> the type of the tail call's result
 */
public sealed interface TailCall<T> extends TailStep permits TailCallContinuationStep, TailCallTerminalStep {

    /**
     * Evaluates this {@link TailCall} and all subsequent calls within the call chain.
     *
     * <p>If a {@link RuntimeException} is raised during the evaluation of the tail call chain, this method will
     * propagate that exact {@code RuntimeException}.
     *
     * @return the result of the tail call
     * @throws NullPointerException if a {@code TailCall} in the tail call chain is {@code null}
     */
    T evaluate();

    /**
     * Asynchronously evaluates this {@link TailCall} and all subsequent calls within the call chain on the
     * common {@link ForkJoinPool}.
     *
     * @return a {@link CompletableFuture} containing the evaluated result
     */
    default CompletableFuture<T> evaluateAsync() {
        return evaluateAsync(ForkJoinPool.commonPool());
    }

    /**
     * Asynchronously evaluates this {@link TailCall} and all subsequent calls within the call chain using the specified executor.
     *
     * @param executor the executor to run the evaluation on
     * @return a {@link CompletableFuture} containing the evaluated result
     * @throws NullPointerException if {@code executor} is {@code null}
     */
    default CompletableFuture<T> evaluateAsync(final Executor executor) {
        Objects.requireNonNull(executor, "executor must not be null");
        return CompletableFuture.supplyAsync(this::evaluate, executor);
    }

    /**
     * Creates the terminal step in the tail call chain with the given result.
     *
     * @param result the result of the tail call chain; may be {@code null}
     * @param <T>    the type of the tail call's result
     * @return the terminal step in the tail call chain
     */
    static <T> TailCall<T> terminateWith(final T result) {
        return new TailCallTerminalStep<>(result);
    }

    /**
     * Concise alias for {@link #terminateWith(Object)}.
     *
     * @param result the result of the tail call chain; may be {@code null}
     * @param <T>    the type of the tail call's result
     * @return the terminal step in the tail call chain
     */
    static <T> TailCall<T> done(final T result) {
        return terminateWith(result);
    }

    /**
     * Creates a continuation step in the tail call chain with the given supplier for the next step within the chain.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @param <T>                  the type of the tail call chain's result
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static <T> TailCall<T> continueWith(final Supplier<TailCall<T>> nextTailCallSupplier) {
        return new TailCallContinuationStep<>(nextTailCallSupplier);
    }

    /**
     * Concise alias for {@link #continueWith(Supplier)}.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @param <T>                  the type of the tail call chain's result
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static <T> TailCall<T> call(final Supplier<TailCall<T>> nextTailCallSupplier) {
        return continueWith(nextTailCallSupplier);
    }

    /**
     * Interleaves multiple tail calls, iteratively advancing active tail calls until <b>all</b> tail calls
     * have reached their terminal step.
     *
     * <p>When a tail call reaches its terminal step earlier than others, it remains in its completed state
     * without further advancement, while the remaining non-terminal tail calls continue advancing until
     * every branch has completed.
     *
     * @param tailCallSuppliers the list of tail call suppliers to advance to completion
     * @return a list containing the final terminal {@link TailCall} instances for each supplier
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    @SuppressWarnings("unchecked")
    static List<TailCall<?>> interleaveAll(final List<? extends Supplier<? extends TailCall<?>>> tailCallSuppliers) {
        return (List<TailCall<?>>) (List<?>) TailStep.interleaveAll(tailCallSuppliers);
    }

    /**
     * Interleaves multiple tail calls, advancing them in unison until <b>at least one</b> reaches its terminal step.
     *
     * @param tailCallSuppliers the list of tail call suppliers to interleave
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    static void interleaveAny(final List<? extends Supplier<? extends TailCall<?>>> tailCallSuppliers) {
        TailStep.interleaveAny(tailCallSuppliers);
    }

    /**
     * Interleaves tail calls, iteratively advancing all supplied tail calls together.
     *
     * <p>This method is an alias for {@link #interleaveAny(List)}. If you wish to advance all tail calls to their
     * individual completion regardless of unequal step counts, use {@link #interleaveAll(List)}.
     *
     * @param tailCallSuppliers a list of {@link Supplier} objects that provide {@link TailCall} instances
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any of its elements is {@code null},
     *                              or if a tail call supplier returns {@code null}.
     */
    static void interleave(final List<? extends Supplier<? extends TailCall<?>>> tailCallSuppliers) {
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
    static CompletableFuture<List<TailCall<?>>> interleaveAsync(
            final List<? extends Supplier<? extends TailCall<?>>> tailCallSuppliers,
            final Executor executor
    ) {
        return (CompletableFuture<List<TailCall<?>>>) (CompletableFuture<?>) TailStep.interleaveAsync(tailCallSuppliers, executor);
    }

    /**
     * Interleaves multiple tail calls asynchronously using {@link ForkJoinPool#commonPool()}.
     *
     * @param tailCallSuppliers the list of tail call suppliers
     * @return a {@link CompletableFuture} that resolves with the list of terminal tail calls once all complete
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}
     */
    static CompletableFuture<List<TailCall<?>>> interleaveAsync(
            final List<? extends Supplier<? extends TailCall<?>>> tailCallSuppliers
    ) {
        return interleaveAsync(tailCallSuppliers, ForkJoinPool.commonPool());
    }
}
