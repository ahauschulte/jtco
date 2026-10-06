package io.github.ahauschulte.jtco;

import io.github.ahauschulte.jtco.primitive.BooleanTailCall;
import io.github.ahauschulte.jtco.primitive.ByteTailCall;
import io.github.ahauschulte.jtco.primitive.CharTailCall;
import io.github.ahauschulte.jtco.primitive.DoubleTailCall;
import io.github.ahauschulte.jtco.primitive.FloatTailCall;
import io.github.ahauschulte.jtco.primitive.IntTailCall;
import io.github.ahauschulte.jtco.primitive.LongTailCall;
import io.github.ahauschulte.jtco.primitive.ShortTailCall;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A cooperative, thread-safe time-sharing engine for interleaving tail calls.
 *
 * <p>The {@code InterleavingEngine} coordinates the execution of multiple recursive operations in lockstep.
 * Key capabilities include:
 * <ul>
 *     <li><b>Dynamic submission</b>: New tasks can be submitted at any time, even concurrently while the engine is running.</li>
 *     <li><b>Individual {@link CompletableFuture} resolution</b>: Each task's future completes immediately when that task
 *         reaches its terminal step, without waiting for longer tasks to finish.</li>
 *     <li><b>Fault isolation</b>: An exception thrown by one task completes its own future exceptionally without
 *         crashing or terminating concurrent tasks.</li>
 *     <li><b>Cooperative cancellation</b>: Cancelling a task's future stops it from advancing in subsequent steps.</li>
 *     <li><b>Flexible execution</b>: Supports tick-based step execution ({@link #step()}), synchronous draining ({@link #runAll()}),
 *         and asynchronous execution on an {@link Executor} ({@link #runAsync()}).</li>
 * </ul>
 */
public final class InterleavingEngine {

    private final Queue<TaskEntry<?>> pendingQueue = new ConcurrentLinkedQueue<>();
    private final List<TaskEntry<?>> activeTasks = new ArrayList<>();
    private final Object loopLock = new Object();

    private InterleavingEngine() {
    }

    /**
     * Creates a new, empty {@link InterleavingEngine}.
     *
     * @return a new engine instance
     */
    public static InterleavingEngine create() {
        return new InterleavingEngine();
    }

    /**
     * Submits a generic {@link TailCall} supplier for cooperative execution.
     *
     * @param supplier the supplier producing the initial tail call step
     * @param <T>      the tail call result type
     * @return a {@link CompletableFuture} completed with the result when this specific task reaches its terminal step
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public <T> CompletableFuture<T> submit(final Supplier<? extends TailCall<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final CompletableFuture<T> future = new CompletableFuture<>();
        final TaskEntry<T> entry = new TaskEntry<>(
                supplier,
                future,
                step -> {
                    @SuppressWarnings("unchecked")
                    final TailCall<T> tc = (TailCall<T>) step;
                    return tc.evaluate();
                }
        );
        pendingQueue.add(entry);
        return future;
    }

    /**
     * Submits a generic {@link TailStep} supplier for cooperative execution.
     *
     * @param supplier the supplier producing the initial step
     * @return a {@link CompletableFuture} completed with the final terminal step
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public CompletableFuture<TailStep> submitStep(final Supplier<? extends TailStep> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final CompletableFuture<TailStep> future = new CompletableFuture<>();
        final TaskEntry<TailStep> entry = new TaskEntry<>(
                supplier,
                future,
                step -> step
        );
        pendingQueue.add(entry);
        return future;
    }

    /**
     * Submits an {@link IntTailCall} task for cooperative execution.
     *
     * @param supplier the unboxed int tail call supplier
     * @return a {@link CompletableFuture} completed with the unboxed {@code int} (wrapped as {@link Integer})
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public CompletableFuture<Integer> submitInt(final Supplier<IntTailCall> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final CompletableFuture<Integer> future = new CompletableFuture<>();
        pendingQueue.add(new TaskEntry<>(
                supplier,
                future,
                step -> ((IntTailCall) step).eval()
        ));
        return future;
    }

    /**
     * Submits a {@link LongTailCall} task for cooperative execution.
     *
     * @param supplier the unboxed long tail call supplier
     * @return a {@link CompletableFuture} completed with the unboxed {@code long} (wrapped as {@link Long})
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public CompletableFuture<Long> submitLong(final Supplier<LongTailCall> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final CompletableFuture<Long> future = new CompletableFuture<>();
        pendingQueue.add(new TaskEntry<>(
                supplier,
                future,
                step -> ((LongTailCall) step).eval()
        ));
        return future;
    }

    /**
     * Submits a {@link DoubleTailCall} task for cooperative execution.
     *
     * @param supplier the unboxed double tail call supplier
     * @return a {@link CompletableFuture} completed with the unboxed {@code double} (wrapped as {@link Double})
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public CompletableFuture<Double> submitDouble(final Supplier<DoubleTailCall> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final CompletableFuture<Double> future = new CompletableFuture<>();
        pendingQueue.add(new TaskEntry<>(
                supplier,
                future,
                step -> ((DoubleTailCall) step).eval()
        ));
        return future;
    }

    /**
     * Submits a {@link FloatTailCall} task for cooperative execution.
     *
     * @param supplier the unboxed float tail call supplier
     * @return a {@link CompletableFuture} completed with the unboxed {@code float} (wrapped as {@link Float})
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public CompletableFuture<Float> submitFloat(final Supplier<FloatTailCall> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final CompletableFuture<Float> future = new CompletableFuture<>();
        pendingQueue.add(new TaskEntry<>(
                supplier,
                future,
                step -> ((FloatTailCall) step).eval()
        ));
        return future;
    }

    /**
     * Submits a {@link BooleanTailCall} task for cooperative execution.
     *
     * @param supplier the unboxed boolean tail call supplier
     * @return a {@link CompletableFuture} completed with the unboxed {@code boolean} (wrapped as {@link Boolean})
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public CompletableFuture<Boolean> submitBoolean(final Supplier<BooleanTailCall> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingQueue.add(new TaskEntry<>(
                supplier,
                future,
                step -> ((BooleanTailCall) step).eval()
        ));
        return future;
    }

    /**
     * Submits a {@link ByteTailCall} task for cooperative execution.
     *
     * @param supplier the unboxed byte tail call supplier
     * @return a {@link CompletableFuture} completed with the unboxed {@code byte} (wrapped as {@link Byte})
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public CompletableFuture<Byte> submitByte(final Supplier<ByteTailCall> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final CompletableFuture<Byte> future = new CompletableFuture<>();
        pendingQueue.add(new TaskEntry<>(
                supplier,
                future,
                step -> ((ByteTailCall) step).eval()
        ));
        return future;
    }

    /**
     * Submits a {@link ShortTailCall} task for cooperative execution.
     *
     * @param supplier the unboxed short tail call supplier
     * @return a {@link CompletableFuture} completed with the unboxed {@code short} (wrapped as {@link Short})
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public CompletableFuture<Short> submitShort(final Supplier<ShortTailCall> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final CompletableFuture<Short> future = new CompletableFuture<>();
        pendingQueue.add(new TaskEntry<>(
                supplier,
                future,
                step -> ((ShortTailCall) step).eval()
        ));
        return future;
    }

    /**
     * Submits a {@link CharTailCall} task for cooperative execution.
     *
     * @param supplier the unboxed char tail call supplier
     * @return a {@link CompletableFuture} completed with the unboxed {@code char} (wrapped as {@link Character})
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public CompletableFuture<Character> submitChar(final Supplier<CharTailCall> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final CompletableFuture<Character> future = new CompletableFuture<>();
        pendingQueue.add(new TaskEntry<>(
                supplier,
                future,
                step -> ((CharTailCall) step).eval()
        ));
        return future;
    }

    /**
     * Advances all currently active tasks by exactly one round-robin step.
     *
     * <p>Any newly submitted tasks in the pending queue are migrated to the active pool prior to stepping.
     * Tasks that finish (or are cancelled/fail) during this step are completed and removed from the active pool.
     *
     * @return the number of active tasks stepped during this round
     */
    public int step() {
        synchronized (loopLock) {
            drainPendingQueue();
            if (activeTasks.isEmpty()) {
                return 0;
            }

            int steppedCount = 0;
            final int size = activeTasks.size();
            final List<TaskEntry<?>> surviving = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                final TaskEntry<?> task = activeTasks.get(i);
                if (task.future.isCancelled()) {
                    continue;
                }

                try {
                    if (!task.initialized) {
                        task.currentStep = Objects.requireNonNull(task.supplier.get(), "Initial tail call must not be null");
                        task.initialized = true;
                    } else {
                        task.currentStep = Objects.requireNonNull(task.currentStep.proceed(), "Next tail call must not be null");
                    }
                    steppedCount++;

                    if (task.currentStep instanceof TerminalStep || task.currentStep.isTerminal()) {
                        task.completeSuccess();
                    } else {
                        surviving.add(task);
                    }
                } catch (final Throwable t) {
                    task.future.completeExceptionally(t);
                }
            }

            activeTasks.clear();
            activeTasks.addAll(surviving);
            return steppedCount;
        }
    }

    /**
     * Advances active tasks up to {@code maxRounds} times, or until no active or pending tasks remain.
     *
     * @param maxRounds the maximum number of rounds to step
     * @return the total number of rounds stepped
     * @throws IllegalArgumentException if {@code maxRounds < 1}
     */
    public int step(final int maxRounds) {
        if (maxRounds < 1) {
            throw new IllegalArgumentException("maxRounds must be at least 1");
        }
        synchronized (loopLock) {
            int rounds = 0;
            while (rounds < maxRounds && hasActiveTasks()) {
                step();
                rounds++;
            }
            return rounds;
        }
    }

    /**
     * Evaluates all currently pending and active tasks until completion on the current thread.
     */
    public void runAll() {
        synchronized (loopLock) {
            while (hasActiveTasks()) {
                step();
            }
        }
    }

    /**
     * Asynchronously executes all submitted tasks to completion using the provided executor.
     *
     * @param executor the executor to run the interleaving loop on
     * @return a {@link CompletableFuture} that completes when all tasks have finished
     * @throws NullPointerException if {@code executor} is {@code null}
     */
    public CompletableFuture<Void> runAsync(final Executor executor) {
        Objects.requireNonNull(executor, "executor must not be null");
        return CompletableFuture.runAsync(this::runAll, executor);
    }

    /**
     * Asynchronously executes all submitted tasks to completion using {@link ForkJoinPool#commonPool()}.
     *
     * @return a {@link CompletableFuture} that completes when all tasks have finished
     */
    public CompletableFuture<Void> runAsync() {
        return runAsync(ForkJoinPool.commonPool());
    }

    /**
     * Checks if there are any active or pending tasks remaining in the engine.
     *
     * @return {@code true} if there are tasks to process, {@code false} otherwise
     */
    public boolean hasActiveTasks() {
        return !pendingQueue.isEmpty() || !activeTasks.isEmpty();
    }

    /**
     * Returns the total count of active and pending tasks in the engine.
     *
     * @return the total number of unfinished tasks
     */
    public int activeTaskCount() {
        synchronized (loopLock) {
            return pendingQueue.size() + activeTasks.size();
        }
    }

    /**
     * Cancels all remaining pending and active tasks in the engine.
     */
    public void cancelAll() {
        synchronized (loopLock) {
            drainPendingQueue();
            for (final TaskEntry<?> task : activeTasks) {
                task.future.cancel(true);
            }
            activeTasks.clear();
        }
    }

    private void drainPendingQueue() {
        TaskEntry<?> entry;
        while ((entry = pendingQueue.poll()) != null) {
            if (!entry.future.isCancelled()) {
                activeTasks.add(entry);
            }
        }
    }

    private static final class TaskEntry<T> {
        private final Supplier<? extends TailStep> supplier;
        private final CompletableFuture<T> future;
        private final Function<TailStep, T> resultExtractor;
        private TailStep currentStep;
        private boolean initialized;

        private TaskEntry(
                final Supplier<? extends TailStep> supplier,
                final CompletableFuture<T> future,
                final Function<TailStep, T> resultExtractor
        ) {
            this.supplier = supplier;
            this.future = future;
            this.resultExtractor = resultExtractor;
        }

        private void completeSuccess() {
            try {
                final T result = resultExtractor.apply(currentStep);
                future.complete(result);
            } catch (final Throwable t) {
                future.completeExceptionally(t);
            }
        }
    }
}
