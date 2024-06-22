package com.github.ahauschulte.jtco;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

class SimpleInterweavingTest {

    @Test
    void testInterweavingForInvalidArguments() {
        assertThatNullPointerException()
                .isThrownBy(() -> TailCall.interweave(null));

        assertThatNullPointerException()
                .isThrownBy(() -> TailCall.interweave(List.of(
                        () -> TailCall.terminateWith(42),
                        null)));
    }

    @Test
    void testInterweavingEmptySupplierList() {
        assertThatNoException()
                .isThrownBy(() -> TailCall.interweave(Collections.emptyList()));
    }

    @Test
    void testInterweavingForSupplierEvaluatingToNull() {
        assertThatNoException()
                .isThrownBy(() -> TailCall.interweave(List.of(() -> null)));
    }

    @Test
    void testInterweavingForTerminalStep() {
        final AtomicBoolean called = new AtomicBoolean();
        TailCall.interweave(List.of(
                () -> TailCall.terminateWith(42),
                () -> TailCall.continueWith(() -> {
                    called.set(true);
                    return TailCall.terminateWith("24");
                })));
        assertThat(called).isFalse();
    }
}
