package com.github.ahauschulte.jtco;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

class SimpleInterleavingTest {

    @Test
    void testInterleavingForInvalidArguments() {
        assertThatNullPointerException()
                .isThrownBy(() -> TailCall.interleave(null));

        assertThatNullPointerException()
                .isThrownBy(() -> TailCall.interleave(List.of(
                        () -> TailCall.terminateWith(42),
                        null)));
    }

    @Test
    void testInterleavingEmptySupplierList() {
        assertThatNoException()
                .isThrownBy(() -> TailCall.interleave(Collections.emptyList()));
    }

    @Test
    void testInterleavingForSupplierEvaluatingToNull() {
        assertThatNoException()
                .isThrownBy(() -> TailCall.interleave(List.of(() -> null)));
    }

    @Test
    void testInterleavingForTerminalStep() {
        final AtomicBoolean called = new AtomicBoolean();
        TailCall.interleave(List.of(
                () -> TailCall.terminateWith(42),
                () -> TailCall.continueWith(() -> {
                    called.set(true);
                    return TailCall.terminateWith("24");
                })));
        assertThat(called).isFalse();
    }
}
