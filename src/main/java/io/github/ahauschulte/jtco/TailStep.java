package io.github.ahauschulte.jtco;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Common contract representing a single step within a tail call chain.
 *
 * <p>Both generic and primitive tail calls implement this interface, allowing them to be
 * inspected and cooperatively advanced together via {@link #interleave(List)}, {@link #interleaveAll(List)},
 * and {@link #interleaveAny(List)}.
 */
public interface TailStep {

    /**
     * Checks if this step is a terminal step that completes the tail call chain.
     *
     * @return {@code true} if this step is terminal, {@code false} if it can proceed further
     */
    boolean isTerminal();

    /**
     * Advances to the next step in the tail call chain.
     *
     * @return the next {@link TailStep} in the chain
     * @throws UnsupportedOperationException if this step is terminal and cannot proceed
     * @throws NullPointerException          if the supplier produces {@code null}
     */
    TailStep proceed();

    /**
     * Interleaves multiple tail calls, iteratively advancing active tail calls until <b>all</b> tail calls
     * have reached their terminal step.
     *
     * <p>When a tail call reaches its terminal step earlier than others, it remains in its completed state
     * without further advancement, while the remaining non-terminal tail calls continue advancing until
     * every branch has completed.
     *
     * @param suppliers the list of tail call suppliers to advance to completion
     * @return a list containing the final terminal {@link TailStep} instances for each supplier
     * @throws NullPointerException if {@code suppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    static List<TailStep> interleaveAll(final List<? extends Supplier<? extends TailStep>> suppliers) {
        Objects.requireNonNull(suppliers, "suppliers must not be null");
        if (suppliers.isEmpty()) return List.of();

        final int size = suppliers.size();
        final TailStep[] steps = new TailStep[size];
        for (int i = 0; i < size; i++) {
            final Supplier<? extends TailStep> s = Objects.requireNonNull(suppliers.get(i), "Tail call supplier must not be null");
            steps[i] = Objects.requireNonNull(s.get(), "Tail call must not be null");
        }

        boolean hasActive = true;
        while (hasActive) {
            hasActive = false;
            for (int i = 0; i < size; i++) {
                if (!(steps[i] instanceof TerminalStep) && !steps[i].isTerminal()) {
                    steps[i] = Objects.requireNonNull(steps[i].proceed(), "Tail call must not be null");
                    hasActive = true;
                }
            }
        }
        return List.of(steps);
    }

    /**
     * Interleaves multiple tail calls, advancing them in unison until <b>at least one</b> reaches its terminal step.
     *
     * @param suppliers the list of tail call suppliers to interleave
     * @throws NullPointerException if {@code suppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    static void interleaveAny(final List<? extends Supplier<? extends TailStep>> suppliers) {
        Objects.requireNonNull(suppliers, "suppliers must not be null");
        if (suppliers.isEmpty()) return;

        final int size = suppliers.size();
        final TailStep[] steps = new TailStep[size];
        for (int i = 0; i < size; i++) {
            final Supplier<? extends TailStep> s = Objects.requireNonNull(suppliers.get(i), "Tail call supplier must not be null");
            steps[i] = Objects.requireNonNull(s.get(), "Tail call must not be null");
        }

        boolean allContinuing = true;
        while (allContinuing) {
            for (int i = 0; i < size; i++) {
                if (steps[i] instanceof TerminalStep || steps[i].isTerminal()) {
                    allContinuing = false;
                    break;
                }
            }

            if (allContinuing) {
                for (int i = 0; i < size; i++) {
                    steps[i] = Objects.requireNonNull(steps[i].proceed(), "Tail call must not be null");
                }
            }
        }
    }

    /**
     * Interleaves multiple tail calls, advancing them in unison until at least one reaches its terminal step.
     *
     * <p>This method is an alias for {@link #interleaveAny(List)}. If you wish to advance all tail calls to their
     * individual completion regardless of unequal step counts, use {@link #interleaveAll(List)}.
     *
     * @param suppliers the list of tail call suppliers to interleave
     * @throws NullPointerException if {@code suppliers} is {@code null}, any element is {@code null},
     *                              or any supplier produces {@code null}
     */
    static void interleave(final List<? extends Supplier<? extends TailStep>> suppliers) {
        interleaveAny(suppliers);
    }

    /**
     * Interleaves multiple tail calls asynchronously on the provided executor.
     *
     * <p>Submits all suppliers to an {@link InterleavingEngine} and runs them cooperatively, returning
     * a future that resolves with the list of terminal steps in the original order once all tasks complete.
     *
     * @param suppliers the list of tail call suppliers
     * @param executor  the executor to execute the interleaving loop on
     * @return a {@link CompletableFuture} that resolves with the list of terminal steps in the original order
     * @throws NullPointerException if {@code suppliers} or {@code executor} is {@code null}
     */
    static CompletableFuture<List<TailStep>> interleaveAsync(
            final List<? extends Supplier<? extends TailStep>> suppliers,
            final Executor executor
    ) {
        Objects.requireNonNull(suppliers, "suppliers must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        if (suppliers.isEmpty()) return CompletableFuture.completedFuture(List.of());

        final InterleavingEngine engine = InterleavingEngine.create();
        final List<CompletableFuture<TailStep>> futures = new ArrayList<>(suppliers.size());
        for (final Supplier<? extends TailStep> s : suppliers) {
            futures.add(engine.submitStep(s));
        }

        final CompletableFuture<Void> runFuture = engine.runAsync(executor);
        return runFuture.thenApply(v -> {
            final List<TailStep> results = new ArrayList<>(futures.size());
            for (final CompletableFuture<TailStep> f : futures) {
                results.add(f.join());
            }
            return List.copyOf(results);
        });
    }

    /**
     * Interleaves multiple tail calls asynchronously using {@link ForkJoinPool#commonPool()}.
     *
     * @param suppliers the list of tail call suppliers
     * @return a {@link CompletableFuture} that resolves with the list of terminal steps in the original order
     * @throws NullPointerException if {@code suppliers} is {@code null}
     */
    static CompletableFuture<List<TailStep>> interleaveAsync(
            final List<? extends Supplier<? extends TailStep>> suppliers
    ) {
        return interleaveAsync(suppliers, ForkJoinPool.commonPool());
    }
}
