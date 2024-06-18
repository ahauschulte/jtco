package com.github.ahauschulte.jtco;

import java.util.function.Supplier;

/**
 * A utility class containing means to express and execute tail calls.
 *
 * <p>A tail call optimisation is applied to tail calls expressed by the means of this class: Instead of executing
 * the tail call recursively it will be executed iteratively. Deeply nested call stacks that are often typical for
 * recursively expressed algorithms can therefore be avoided.
 *
 * <p><b>Example</b>
 * {@snippet lang="java" :
 * import com.github.ahauschulte.jtco.TailCall;
 * import com.github.ahauschulte.jtco.TailCall.TailCallStep;
 *
 * import java.math.BigInteger;
 *
 * public class Main {
 *     private static TailCallStep<BigInteger> factorial(final int n, final BigInteger acc) { // @highlight substring="TailCallStep"
 *         if (n == 0) {
 *             return TailCall.terminateWith(acc); // @highlight substring="TailCall.terminateWith"
 *         } else {
 *             return TailCall.continueWith(() -> // @highlight substring="TailCall.continueWith(() ->"
 *                         factorial(n - 1, BigInteger.valueOf(n).multiply(acc)));
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
 * }
 */
public final class TailCall {

    /**
     * A type representing a single step within the chain of methods calls
     *
     * @param <T> the type of the tail call chain's result
     */
    public sealed interface TailCallStep<T> permits TailCallContinuationStep, TailCallTerminalStep {

        /**
         * Evaluates this tail call step and all subsequent steps in the call chain. If a {@link RuntimeException}
         * is raised during the evaluation of the tail call, this method will throw that exact {@code RuntimeException}.
         *
         * @return the result of the tail call
         */
        T evaluate();
    }

    private TailCall() {
        throw new AssertionError("Instantiation not allowed");
    }

    /**
     * Terminates the tail call chain with the given result
     *
     * @param result the result of the tail call chain
     * @param <T>    the type of the tail call chain's result
     * @return the terminal step in the tail call chain
     */
    public static <T> TailCallStep<T> terminateWith(final T result) {
        return new TailCallTerminalStep<>(result);
    }

    /**
     * Returns the next step in the tail call chain
     *
     * @param nextTailCallStep a {@link Supplier} of the next step in the tail call chain
     * @param <T>              the type of the tail call chain's result
     * @return the next step in the tail call chain
     * @throws NullPointerException if nextTailCallStep is null
     */
    public static <T> TailCallStep<T> continueWith(final Supplier<TailCallStep<T>> nextTailCallStep) {
        return new TailCallContinuationStep<>(nextTailCallStep);
    }
}
