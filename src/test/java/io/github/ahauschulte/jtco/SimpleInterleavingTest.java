package io.github.ahauschulte.jtco;

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

    @Test
    void testInterleaveAllWithUnequalSteps() {
        // Task A has 2 steps, Task B has 5 steps, Task C has 1 step
        final Supplier<TailCall<String>> taskA = () ->
                TailCall.call(() -> TailCall.done("resultA"));

        final Supplier<TailCall<Integer>> taskB = () ->
                TailCall.call(() ->
                        TailCall.call(() ->
                                TailCall.call(() ->
                                        TailCall.call(() -> TailCall.done(42)))));

        final Supplier<TailCall<Boolean>> taskC = () -> TailCall.done(true);

        final List<TailCall<?>> results = TailCall.interleaveAll(List.of(taskA, taskB, taskC));

        assertThat(results).hasSize(3);
        assertThat(results.get(0).isTerminal()).isTrue();
        assertThat(results.get(0).evaluate()).isEqualTo("resultA");

        assertThat(results.get(1).isTerminal()).isTrue();
        assertThat(results.get(1).evaluate()).isEqualTo(42);

        assertThat(results.get(2).isTerminal()).isTrue();
        assertThat(results.get(2).evaluate()).isEqualTo(true);
    }

    @Test
    void testInterleaveAllEmptyAndNullChecks() {
        assertThat(TailCall.interleaveAll(List.of())).isEmpty();

        assertThatNullPointerException()
                .isThrownBy(() -> TailCall.interleaveAll(null));

        final List<Supplier<TailCall<?>>> listWithNull = new ArrayList<>();
        listWithNull.add(() -> TailCall.done(1));
        listWithNull.add(null);
        assertThatNullPointerException()
                .isThrownBy(() -> TailCall.interleaveAll(listWithNull));

        assertThatNullPointerException()
                .isThrownBy(() -> TailCall.interleaveAll(List.of(() -> null)));
    }

    @Test
    void testInterleaveAnyStopsOnFirstTerminal() {
        final AtomicBoolean stepAfterTerminalCalled = new AtomicBoolean(false);

        TailCall.interleaveAny(List.of(
                () -> TailCall.done("fast"),
                () -> TailCall.call(() -> {
                    stepAfterTerminalCalled.set(true);
                    return TailCall.done("slow");
                })
        ));

        assertThat(stepAfterTerminalCalled).isFalse();
    }
}
