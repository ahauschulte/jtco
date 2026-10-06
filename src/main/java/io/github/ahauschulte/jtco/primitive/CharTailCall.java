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
 * Represents a tail call returning a {@code char}, facilitating tail call optimization with zero boxing.
 *
 * <p>This interface allows recursive algorithms returning primitive {@code char} values to be executed
 * iteratively without risking a {@link StackOverflowError} and without allocating boxed {@link Character} objects.
 *
 * <p><b>Example</b>
 * {@snippet lang = "java":
 * import io.github.ahauschulte.jtco.primitive.CharTailCall;
 *
 * public class CaesarShift {
 *     private static CharTailCall shift(final int n, final char c) {
 *         if (n <= 0) {
 *             return CharTailCall.done(c);
 *         }
 *         return CharTailCall.call(() -> shift(n - 1, (char) (c + 1)));
 *     }
 *
 *     public static char compute(final int n, final char start) {
 *         return shift(n, start).eval();
 *     }
 * }
 * }
 */
public sealed interface CharTailCall extends TailStep permits CharTailCall.Terminal, CharTailCall.Continuation {

    /**
     * Evaluates this {@link CharTailCall} and all subsequent calls within the call chain with zero boxing.
     *
     * <p>If a {@link RuntimeException} is raised during the evaluation of the tail call chain, this method will
     * propagate that exact {@code RuntimeException}.
     *
     * @return the unboxed {@code char} result of the tail call
     * @throws NullPointerException if a {@code TailCall} in the tail call chain is {@code null}
     */
    char eval();

    /**
     * Asynchronously evaluates this unboxed tail call on the common {@link ForkJoinPool}.
     *
     * @return a {@link CompletableFuture} containing the evaluated {@link Character} result
     */
    default CompletableFuture<Character> evalAsync() {
        return evalAsync(ForkJoinPool.commonPool());
    }

    /**
     * Asynchronously evaluates this unboxed tail call using the specified executor.
     *
     * @param executor the executor to run the evaluation on
     * @return a {@link CompletableFuture} containing the evaluated {@link Character} result
     * @throws NullPointerException if {@code executor} is {@code null}
     */
    default CompletableFuture<Character> evalAsync(final Executor executor) {
        Objects.requireNonNull(executor, "executor must not be null");
        return CompletableFuture.supplyAsync(this::eval, executor);
    }

    /**
     * Adapts this unboxed tail call into a boxed {@link TailCall} of {@link Character}.
     *
     * <p>Evaluation remains unboxed until explicitly evaluated via {@link TailCall#evaluate()}.
     *
     * @return a boxed {@link TailCall} representing this calculation
     */
    default TailCall<Character> boxed() {
        return TailCall.continueWith(() -> TailCall.terminateWith(eval()));
    }

    /**
     * Creates the terminal step in the tail call chain with the given unboxed result.
     *
     * @param result the result of the tail call chain
     * @return the terminal step in the tail call chain
     */
    static CharTailCall terminateWith(final char result) {
        return new Terminal(result);
    }

    /**
     * Concise alias for {@link #terminateWith(char)}.
     *
     * @param result the result of the tail call chain
     * @return the terminal step in the tail call chain
     */
    static CharTailCall done(final char result) {
        return terminateWith(result);
    }

    /**
     * Creates a continuation step in the tail call chain with the given supplier for the next step within the chain.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static CharTailCall continueWith(final Supplier<CharTailCall> nextTailCallSupplier) {
        return new Continuation(nextTailCallSupplier);
    }

    /**
     * Concise alias for {@link #continueWith(Supplier)}.
     *
     * @param nextTailCallSupplier a {@link Supplier} of the next step in the tail call chain
     * @return the next step in the tail call chain
     * @throws NullPointerException if {@code nextTailCallSupplier} is {@code null}
     */
    static CharTailCall call(final Supplier<CharTailCall> nextTailCallSupplier) {
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
     * @return a list containing the final terminal {@link CharTailCall} instances for each supplier
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    @SuppressWarnings("unchecked")
    static List<CharTailCall> interleaveAll(final List<? extends Supplier<CharTailCall>> tailCallSuppliers) {
        return (List<CharTailCall>) (List<?>) TailStep.interleaveAll(tailCallSuppliers);
    }

    /**
     * Interleaves multiple tail calls, advancing them in unison until <b>at least one</b> reaches its terminal step.
     *
     * @param tailCallSuppliers the list of tail call suppliers to interleave
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    static void interleaveAny(final List<? extends Supplier<CharTailCall>> tailCallSuppliers) {
        TailStep.interleaveAny(tailCallSuppliers);
    }

    /**
     * Interleaves tail calls, iteratively advancing all supplied tail calls together without boxing.
     *
     * <p>This method is an alias for {@link #interleaveAny(List)}. If you wish to advance all tail calls to their
     * individual completion regardless of unequal step counts, use {@link #interleaveAll(List)}.
     *
     * @param tailCallSuppliers a list of {@link Supplier} objects that provide {@link CharTailCall} instances
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}, any of its elements is {@code null},
     *                              or if a tail call supplier returns {@code null}
     */
    static void interleave(final List<? extends Supplier<CharTailCall>> tailCallSuppliers) {
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
    static CompletableFuture<List<CharTailCall>> interleaveAsync(
            final List<? extends Supplier<CharTailCall>> tailCallSuppliers,
            final Executor executor
    ) {
        return (CompletableFuture<List<CharTailCall>>) (CompletableFuture<?>) TailStep.interleaveAsync(tailCallSuppliers, executor);
    }

    /**
     * Interleaves multiple tail calls asynchronously using {@link ForkJoinPool#commonPool()}.
     *
     * @param tailCallSuppliers the list of tail call suppliers
     * @return a {@link CompletableFuture} that resolves with the list of terminal tail calls once all complete
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null}
     */
    static CompletableFuture<List<CharTailCall>> interleaveAsync(
            final List<? extends Supplier<CharTailCall>> tailCallSuppliers
    ) {
        return interleaveAsync(tailCallSuppliers, ForkJoinPool.commonPool());
    }

    /**
     * Represents the terminal step containing the unboxed result.
     *
     * @param result the final unboxed {@code char} value
     */
    record Terminal(char result) implements CharTailCall, TerminalStep {

        @Override
        public char eval() {
            return result;
        }

        @Override
        public CharTailCall proceed() {
            throw new UnsupportedOperationException("Terminal step cannot proceed to another tail call");
        }
    }

    /**
     * Represents a continuation step that advances to the next step via a supplier.
     *
     * @param nextTailCallSupplier the supplier for the next tail call step
     */
    record Continuation(Supplier<CharTailCall> nextTailCallSupplier) implements CharTailCall, ContinuationStep {

        /**
         * Validates the continuation supplier.
         *
         * @param nextTailCallSupplier the supplier for the next tail call step
         */
        public Continuation {
            Objects.requireNonNull(nextTailCallSupplier, "nextTailCallSupplier must not be null");
        }

        @Override
        public char eval() {
            CharTailCall step = this;
            while (step instanceof Continuation continuation) {
                step = Objects.requireNonNull(continuation.proceed(), "No tail call in the chain may be null");
            }
            return ((Terminal) step).result();
        }

        @Override
        public CharTailCall proceed() {
            return Objects.requireNonNull(nextTailCallSupplier.get(), "Tail call must not be null");
        }
    }
}
