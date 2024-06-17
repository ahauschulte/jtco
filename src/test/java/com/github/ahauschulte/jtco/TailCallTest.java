package com.github.ahauschulte.jtco;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TailCallTest {

    @Test
    void testTailCallTerminationStep() {
        assertThat(TailCall.terminateWith(42).evaluate()).isEqualTo(42);
    }

    @Test
    void testTailCallContinuationWithNullArgument() {
        assertThatNullPointerException()
                .isThrownBy(() -> TailCall.continueWith(null));
    }

    @Test
    void testCallSequence() {
        assertThat(TailCall.continueWith(() ->
                        TailCall.continueWith(() ->
                                TailCall.terminateWith(42)))
                .evaluate())
                .isEqualTo(42);
    }

    @Test
    void testTailCallThrowingContinuation() {
        final TailCall<Integer> recursionChain =
                TailCall.continueWith(() ->
                        TailCall.continueWith(() -> {
                            throw new IllegalStateException();
                        }));
        
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(recursionChain::evaluate);
    }

}
