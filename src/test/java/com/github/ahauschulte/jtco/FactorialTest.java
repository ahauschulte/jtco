package com.github.ahauschulte.jtco;

import com.github.ahauschulte.jtco.TailCall.TailCallStep;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FactorialTest {

    @Test
    void testFactorial() {
        assertThat(factorial(3)).isEqualTo(6);
    }

    private static TailCallStep<Integer> factorial(final int n, final Integer acc) {
        if (n == 0) {
            return TailCall.terminateWith(acc);
        } else {
            return TailCall.continueWith(() -> factorial(n - 1, n * acc));
        }
    }

    private static int factorial(final int n) {
        return factorial(n, 1).evaluate();
    }

}
