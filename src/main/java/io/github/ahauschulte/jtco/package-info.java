/**
 * Contains types for the optimised execution of tail calls.
 *
 * <p>This package provides a substitute for native tail-call optimisation in Java, allowing
 * tail-recursive algorithms to be executed iteratively in constant stack space, thereby preventing deep call stacks
 * and stack overflows while improving robustness. Performance characteristics depend on the workload and are typically
 * slower than equivalent handwritten iterative implementations due to additional indirection and allocation overhead.
 *
 * <p>The main component of this package is {@link io.github.ahauschulte.jtco.TailCall}, an interface representing a
 * tail call that can be evaluated iteratively to avoid deep recursion.
 */
package io.github.ahauschulte.jtco;