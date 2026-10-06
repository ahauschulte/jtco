package io.github.ahauschulte.jtco;

import io.github.ahauschulte.jtco.primitive.IntTailCall;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterleavingEngineTest {

    @Test
    @DisplayName("Submit generic TailCall tasks and drain via runAll")
    void testBasicSubmitAndRunAll() {
        final InterleavingEngine engine = InterleavingEngine.create();

        final CompletableFuture<String> f1 = engine.submit(() ->
                TailCall.call(() -> TailCall.done("result1")));

        final CompletableFuture<Integer> f2 = engine.submit(() ->
                TailCall.call(() ->
                        TailCall.call(() -> TailCall.done(42))));

        assertThat(engine.hasActiveTasks()).isTrue();
        assertThat(f1.isDone()).isFalse();
        assertThat(f2.isDone()).isFalse();

        engine.runAll();

        assertThat(engine.hasActiveTasks()).isFalse();
        assertThat(f1).isCompletedWithValue("result1");
        assertThat(f2).isCompletedWithValue(42);
    }

    @Test
    @DisplayName("Early completion: short task completes its future before long task finishes")
    void testEarlyCompletion() {
        final InterleavingEngine engine = InterleavingEngine.create();

        final CompletableFuture<String> fastFuture = engine.submit(() ->
                TailCall.call(() -> TailCall.done("fast"))); // 2 steps

        final CompletableFuture<String> slowFuture = engine.submit(() ->
                TailCall.call(() ->
                        TailCall.call(() ->
                                TailCall.call(() ->
                                        TailCall.call(() ->
                                                TailCall.call(() -> TailCall.done("slow"))))))); // 6 steps

        // Round 1
        engine.step();
        assertThat(fastFuture.isDone()).isFalse();
        assertThat(slowFuture.isDone()).isFalse();

        // Round 2 - fast task completes!
        engine.step();
        assertThat(fastFuture).isCompletedWithValue("fast");
        assertThat(slowFuture.isDone()).isFalse(); // slow task still continuing!

        // Complete remaining rounds
        engine.runAll();
        assertThat(slowFuture).isCompletedWithValue("slow");
    }

    @Test
    @DisplayName("Dynamic submission: task dynamically submits a child task during execution")
    void testDynamicSubmission() {
        final InterleavingEngine engine = InterleavingEngine.create();
        final java.util.concurrent.atomic.AtomicReference<CompletableFuture<Integer>> dynamicHolder = new java.util.concurrent.atomic.AtomicReference<>();

        final CompletableFuture<String> parentFuture = engine.submit(() ->
                TailCall.call(() -> {
                    // Dynamically spawn child task
                    dynamicHolder.set(engine.submitInt(() ->
                            IntTailCall.call(() -> IntTailCall.done(999))));
                    return TailCall.done("parent-done");
                }));

        engine.runAll();

        assertThat(parentFuture).isCompletedWithValue("parent-done");
        assertThat(dynamicHolder.get()).isNotNull().isCompletedWithValue(999);
        assertThat(engine.hasActiveTasks()).isFalse();
    }

    @Test
    @DisplayName("Fault isolation: failing task completes exceptionally without crashing healthy tasks")
    void testFaultIsolation() {
        final InterleavingEngine engine = InterleavingEngine.create();

        final CompletableFuture<String> healthyFuture = engine.submit(() ->
                TailCall.call(() ->
                        TailCall.call(() -> TailCall.done("healthy"))));

        final CompletableFuture<String> failingFuture = engine.submit(() ->
                TailCall.call(() -> {
                    throw new IllegalStateException("boom in step");
                }));

        engine.runAll();

        assertThat(healthyFuture).isCompletedWithValue("healthy");
        assertThat(failingFuture).isCompletedExceptionally();
        assertThatThrownBy(failingFuture::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("boom in step");
        assertThat(engine.hasActiveTasks()).isFalse();
    }

    @Test
    @DisplayName("Cooperative cancellation: cancelling a task stops it from progressing")
    void testCooperativeCancellation() {
        final InterleavingEngine engine = InterleavingEngine.create();
        final AtomicInteger steppedCount = new AtomicInteger();

        final CompletableFuture<String> cancelledFuture = engine.submit(() -> {
            steppedCount.incrementAndGet();
            return TailCall.call(() -> {
                steppedCount.incrementAndGet();
                return TailCall.done("done");
            });
        });

        // Run 1 step (initializes task and calls initial supplier)
        engine.step();
        assertThat(steppedCount.get()).isEqualTo(1);

        // Cancel the future before next step
        cancelledFuture.cancel(true);

        // Step again
        engine.runAll();

        // Step should not have advanced again
        assertThat(steppedCount.get()).isEqualTo(1);
        assertThat(cancelledFuture.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Cancel all pending and active tasks")
    void testCancelAll() {
        final InterleavingEngine engine = InterleavingEngine.create();

        final CompletableFuture<String> f1 = engine.submit(() ->
                TailCall.call(() -> TailCall.done("1")));
        final CompletableFuture<String> f2 = engine.submit(() ->
                TailCall.call(() -> TailCall.done("2")));

        engine.cancelAll();

        assertThat(f1.isCancelled()).isTrue();
        assertThat(f2.isCancelled()).isTrue();
        assertThat(engine.hasActiveTasks()).isFalse();
        assertThat(engine.activeTaskCount()).isZero();
    }

    @Test
    @DisplayName("step(maxRounds) limits stepping iterations")
    void testStepMaxRounds() {
        final InterleavingEngine engine = InterleavingEngine.create();

        final CompletableFuture<Integer> future = engine.submitInt(() ->
                IntTailCall.call(() ->
                        IntTailCall.call(() ->
                                IntTailCall.call(() ->
                                        IntTailCall.call(() -> IntTailCall.done(100))))));

        final int rounds = engine.step(2);
        assertThat(rounds).isEqualTo(2);
        assertThat(future.isDone()).isFalse();

        engine.runAll();
        assertThat(future).isCompletedWithValue(100);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> engine.step(0));
    }

    @Test
    @DisplayName("runAsync execution on custom executor")
    void testRunAsync() throws Exception {
        final InterleavingEngine engine = InterleavingEngine.create();
        final ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            final CompletableFuture<String> f1 = engine.submit(() ->
                    TailCall.call(() -> TailCall.done("async-val")));
            final CompletableFuture<Integer> f2 = engine.submitInt(() ->
                    IntTailCall.call(() -> IntTailCall.done(777)));

            final CompletableFuture<Void> all = engine.runAsync(executor);
            all.join();

            assertThat(f1).isCompletedWithValue("async-val");
            assertThat(f2).isCompletedWithValue(777);
            assertThat(engine.hasActiveTasks()).isFalse();
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Submit all 8 primitive types via specialized submit methods")
    void testPrimitiveSubmits() {
        final InterleavingEngine engine = InterleavingEngine.create();

        final CompletableFuture<Integer> fInt = engine.submitInt(() -> IntTailCall.done(1));
        final CompletableFuture<Long> fLong = engine.submitLong(() -> io.github.ahauschulte.jtco.primitive.LongTailCall.done(2L));
        final CompletableFuture<Double> fDouble = engine.submitDouble(() -> io.github.ahauschulte.jtco.primitive.DoubleTailCall.done(3.0));
        final CompletableFuture<Float> fFloat = engine.submitFloat(() -> io.github.ahauschulte.jtco.primitive.FloatTailCall.done(4.0f));
        final CompletableFuture<Boolean> fBool = engine.submitBoolean(() -> io.github.ahauschulte.jtco.primitive.BooleanTailCall.done(true));
        final CompletableFuture<Byte> fByte = engine.submitByte(() -> io.github.ahauschulte.jtco.primitive.ByteTailCall.done((byte) 5));
        final CompletableFuture<Short> fShort = engine.submitShort(() -> io.github.ahauschulte.jtco.primitive.ShortTailCall.done((short) 6));
        final CompletableFuture<Character> fChar = engine.submitChar(() -> io.github.ahauschulte.jtco.primitive.CharTailCall.done('Z'));

        engine.runAll();

        assertThat(fInt).isCompletedWithValue(1);
        assertThat(fLong).isCompletedWithValue(2L);
        assertThat(fDouble).isCompletedWithValue(3.0);
        assertThat(fFloat).isCompletedWithValue(4.0f);
        assertThat(fBool).isCompletedWithValue(true);
        assertThat(fByte).isCompletedWithValue((byte) 5);
        assertThat(fShort).isCompletedWithValue((short) 6);
        assertThat(fChar).isCompletedWithValue('Z');
    }

    @Test
    @DisplayName("Null validation checks")
    void testNullChecks() {
        final InterleavingEngine engine = InterleavingEngine.create();
        assertThatNullPointerException().isThrownBy(() -> engine.submit(null));
        assertThatNullPointerException().isThrownBy(() -> engine.submitStep(null));
        assertThatNullPointerException().isThrownBy(() -> engine.submitInt(null));
        assertThatNullPointerException().isThrownBy(() -> engine.submitLong(null));
        assertThatNullPointerException().isThrownBy(() -> engine.submitDouble(null));
        assertThatNullPointerException().isThrownBy(() -> engine.submitFloat(null));
        assertThatNullPointerException().isThrownBy(() -> engine.submitBoolean(null));
        assertThatNullPointerException().isThrownBy(() -> engine.submitByte(null));
        assertThatNullPointerException().isThrownBy(() -> engine.submitShort(null));
        assertThatNullPointerException().isThrownBy(() -> engine.submitChar(null));
        assertThatNullPointerException().isThrownBy(() -> engine.runAsync(null));
    }
}
