package io.github.ahauschulte.jtco;

import io.github.ahauschulte.jtco.primitive.IntTailCall;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class TailCallAsyncTest {

    private TailCall<Integer> countdown(final int n) {
        if (n <= 0) {
            return TailCall.done(0);
        }
        return TailCall.call(() -> countdown(n - 1));
    }

    private IntTailCall intCountdown(final int n) {
        if (n <= 0) {
            return IntTailCall.done(0);
        }
        return IntTailCall.call(() -> intCountdown(n - 1));
    }

    @Test
    @DisplayName("TailCall.evaluateAsync evaluates deep recursion asynchronously")
    void testTailCallEvaluateAsync() {
        final CompletableFuture<Integer> future = countdown(50_000).evaluateAsync();
        assertThat(future.join()).isEqualTo(0);
    }

    @Test
    @DisplayName("TailCall.evaluateAsync with custom executor")
    void testTailCallEvaluateAsyncWithExecutor() {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final CompletableFuture<Integer> future = countdown(10_000).evaluateAsync(executor);
            assertThat(future.join()).isEqualTo(0);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("IntTailCall.evalAsync evaluates primitive tail call asynchronously")
    void testIntTailCallEvalAsync() {
        final CompletableFuture<Integer> future = intCountdown(50_000).evalAsync();
        assertThat(future.join()).isEqualTo(0);
    }

    @Test
    @DisplayName("TailStep.interleaveAsync with mixed tail call types")
    void testTailStepInterleaveAsync() {
        final Supplier<TailCall<String>> genericTask = () ->
                TailCall.call(() -> TailCall.done("finished"));
        final Supplier<IntTailCall> intTask = () ->
                IntTailCall.call(() -> IntTailCall.call(() -> IntTailCall.done(42)));

        final List<Supplier<? extends TailStep>> tasks = List.of(genericTask, intTask);
        final CompletableFuture<List<TailStep>> future = TailStep.interleaveAsync(tasks);

        final List<TailStep> results = future.join();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).isTerminal()).isTrue();
        @SuppressWarnings("unchecked")
        final TailCall<String> tc = (TailCall<String>) results.get(0);
        assertThat(tc.evaluate()).isEqualTo("finished");

        assertThat(results.get(1).isTerminal()).isTrue();
        final IntTailCall itc = (IntTailCall) results.get(1);
        assertThat(itc.eval()).isEqualTo(42);
    }

    @Test
    @DisplayName("TailCall.interleaveAsync resolves typed List of TailCall")
    void testTailCallInterleaveAsync() {
        final Supplier<TailCall<String>> s1 = () -> TailCall.done("first");
        final Supplier<TailCall<Integer>> s2 = () -> TailCall.call(() -> TailCall.done(2));

        final CompletableFuture<List<TailCall<?>>> future = TailCall.interleaveAsync(List.of(s1, s2));
        final List<TailCall<?>> results = future.join();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).evaluate()).isEqualTo("first");
        assertThat(results.get(1).evaluate()).isEqualTo(2);
    }

    @Test
    @DisplayName("IntTailCall.interleaveAsync resolves typed List of IntTailCall")
    void testIntTailCallInterleaveAsync() {
        final Supplier<IntTailCall> s1 = () -> IntTailCall.done(10);
        final Supplier<IntTailCall> s2 = () -> IntTailCall.call(() -> IntTailCall.done(20));

        final CompletableFuture<List<IntTailCall>> future = IntTailCall.interleaveAsync(List.of(s1, s2));
        final List<IntTailCall> results = future.join();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).eval()).isEqualTo(10);
        assertThat(results.get(1).eval()).isEqualTo(20);
    }
}
