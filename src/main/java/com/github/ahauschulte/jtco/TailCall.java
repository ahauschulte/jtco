package com.github.ahauschulte.jtco;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A representation of a tail call, facilitating tail call optimization by enabling iterative evaluation.
 *
 * <p>This interface defines the structure for tail calls, allowing recursive algorithms to be executed in an
 * iterative manner. This approach helps to avoid the deep call stacks typically associated with recursion, thus
 * preventing stack overflow errors and enhancing performance.
 *
 * <p><b>Example</b>
 * {@snippet lang = "java":
 * import com.github.ahauschulte.jtco.TailCall;
 * import java.math.BigInteger;
 *
 * public class Factorial {
 *     private static TailCall<Integer> factorial(final int n, final Integer prevAcc) { // @highlight substring="TailCall"
 *         if (n == 0) {
 *             return TailCall.terminateWith(acc); // @highlight substring="TailCall.terminateWith"
 *         } else {
 *             return TailCall.continueWith(() -> factorial(n - 1, n * prevAcc)); // @highlight substring="TailCall.continueWith"
 *         }
 *     }
 *
 *     private static BigInteger factorial(final int n) {
 *         return factorial(n, BigInteger.ONE).evaluate();
 *     }
 *
 *     public static void main(final String[] args) {
 *         final int n = 1_000;
 *         System.out.printf("factorial(%d): %d%n", n, factorial(n));
 *     }
 * }
 *}
 *
 * @param <T> the type of the tail call chain's result
 */
public sealed interface TailCall<T> permits TailCallContinuationStep, TailCallTerminalStep {

    /**
     * Evaluates this {@link TailCall} and all subsequent calls within the call chain
     *
     * <p>If a {@link RuntimeException} is raised during the evaluation of the tail call, this method will propagate
     * that exact {@code RuntimeException}.
     *
     * @return the result of the tail call
     * @throws NullPointerException if a {@code TailCall} in the tail call chain is {@code null}
     */
    T evaluate();

    /**
     * Creates a terminal step in the tail call chain with the given result
     *
     * <p>This method is used to signify the end of the tail call chain.
     *
     * @param result the result of the tail call chain
     * @param <T>    the type of the tail call chain's result
     * @return the terminal step in the tail call chain
     */
    static <T> TailCall<T> terminateWith(final T result) {
        return new TailCallTerminalStep<>(result);
    }

    /**
     * Creates a continuation step in the tail call chain with the given supplier for the next tail call
     *
     * <p>This method is used to continue the tail call chain by providing the next step as a supplier.
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
     * Interweaves tail call, iteratively advancing each tail call together.
     *
     * <p>This method processes a list of suppliers that produce tail calls, advancing them in unison. It repeatedly
     * checks if all tail calls in the list can continue and, if so, advances each tail call to its next step. This
     * process ensures that each step of the computation progresses together with the others, rather than one tail
     * call fully completing before the next starts. The iteration stops when at least one tail call cannot proceed
     * further.
     *
     * @param tailCallSuppliers a list of {@link Supplier} objects that provide {@link TailCall} instances.
     * @throws NullPointerException if {@code tailCallSuppliers} is {@code null} or any of its elements
     */
    static void interweave(final List<Supplier<TailCall<?>>> tailCallSuppliers) {
        Objects.requireNonNull(tailCallSuppliers, "tailCallSuppliers must not be null");
        if (tailCallSuppliers.isEmpty()) return;

        final List<TailCall<?>> tailCallList = tailCallSuppliers.stream()
                .map(Supplier::get)
                .collect(Collectors.toCollection(() -> new ArrayList<>(tailCallSuppliers.size())));

        while (tailCallList.stream().allMatch(tailCall -> tailCall instanceof TailCallContinuationStep<?>)) {
            tailCallList.replaceAll(tailCall -> ((TailCallContinuationStep<?>) tailCall).proceed());
        }
    }
}
