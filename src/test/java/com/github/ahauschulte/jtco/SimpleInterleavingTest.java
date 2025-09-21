package com.github.ahauschulte.jtco;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

class SimpleInterleavingTest {

    @Test
    void testInterleavingForInvalidArguments() {
        final List<Supplier<TailCall<?>>> tailCallSuppliersWithNullElement = new ArrayList<>();
        tailCallSuppliersWithNullElement.add(() -> TailCall.terminateWith(42));
        tailCallSuppliersWithNullElement.add(null);

        assertThatNullPointerException()
                .isThrownBy(() -> TailCall.interleave(tailCallSuppliersWithNullElement));

        assertThatNullPointerException()
                .isThrownBy(() -> TailCall.interleave(null));
    }

    @Test
    void testInterleavingEmptySupplierList() {
        assertThatNoException()
                .isThrownBy(() -> TailCall.interleave(Collections.emptyList()));
    }

    @Test
    void testInterleavingForSupplierEvaluatingToNull() {
        assertThatNullPointerException()
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
