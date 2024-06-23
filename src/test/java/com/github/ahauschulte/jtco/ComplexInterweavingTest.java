package com.github.ahauschulte.jtco;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplexInterweavingTest {
    private record CombinedResult(BigInteger factorial, BigInteger fibonacci) {
    }

    private final Deque<BigInteger> individualResultStack = new ArrayDeque<>();
    private final Deque<CombinedResult> combinedResultStack = new ArrayDeque<>();

    private TailCall<BigInteger> factorial(final int n, final int i, final BigInteger prevAcc) {
        individualResultStack.push(prevAcc);
        if (i > n) {
            return TailCall.terminateWith(prevAcc);
        } else {
            final BigInteger nextAcc = BigInteger.valueOf(i).multiply(prevAcc);
            return TailCall.continueWith(() -> factorial(n, i + 1, nextAcc));
        }
    }

    private TailCall<BigInteger> fibonacci(final long n, final BigInteger a, final BigInteger b) {
        individualResultStack.push(a);
        if (n == 0) {
            return TailCall.terminateWith(a);
        } else {
            return TailCall.continueWith(() -> fibonacci(n - 1, b, a.add(b)));
        }
    }

    private TailCall<Void> combineFactorialAndFibonacci() {
        if (individualResultStack.size() < 2) {
            return TailCall.terminateWith(null);
        } else {
            final BigInteger factorial = individualResultStack.pollLast();
            final BigInteger fibonacci = individualResultStack.pollLast();
            combinedResultStack.push(new CombinedResult(factorial, fibonacci));
            return TailCall.continueWith(this::combineFactorialAndFibonacci);
        }
    }

    @Test
    void testInterweavingStartingWithContinuationSteps() {
        TailCall.interweave(List.of(
                () -> factorial(9, 1, BigInteger.ONE),
                () -> fibonacci(15, BigInteger.ZERO, BigInteger.ONE),
                this::combineFactorialAndFibonacci
        ));

        assertThat(combinedResultStack)
                .hasSize(10)
                .containsExactly(
                        new CombinedResult(BigInteger.valueOf(362_880), BigInteger.valueOf(34)),
                        new CombinedResult(BigInteger.valueOf(40_320), BigInteger.valueOf(21)),
                        new CombinedResult(BigInteger.valueOf(5_040), BigInteger.valueOf(13)),
                        new CombinedResult(BigInteger.valueOf(720), BigInteger.valueOf(8)),
                        new CombinedResult(BigInteger.valueOf(120), BigInteger.valueOf(5)),
                        new CombinedResult(BigInteger.valueOf(24), BigInteger.valueOf(3)),
                        new CombinedResult(BigInteger.valueOf(6), BigInteger.valueOf(2)),
                        new CombinedResult(BigInteger.valueOf(2), BigInteger.ONE),
                        new CombinedResult(BigInteger.ONE, BigInteger.ONE),
                        new CombinedResult(BigInteger.ONE, BigInteger.ZERO)
                );
    }

    @Test
    void testInterweavingStartingWithTerminalStep() {
        TailCall.interweave(List.of(
                () -> factorial(0, 1, BigInteger.ONE),
                () -> fibonacci(15, BigInteger.ZERO, BigInteger.ONE),
                this::combineFactorialAndFibonacci
        ));

        assertThat(combinedResultStack)
                .hasSize(1)
                .containsExactly(
                        new CombinedResult(BigInteger.ONE, BigInteger.ZERO)
                );
    }
}
