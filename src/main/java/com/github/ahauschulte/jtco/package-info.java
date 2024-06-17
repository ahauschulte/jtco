/**
 * Contains types for the optimised execution of tail calls.
 *
 * <p>This package provides a means for implementing tail call optimisation in Java, allowing
 * recursive algorithms to be executed in an iterative manner, thus avoiding deep call stacks and improving the
 * performance and reliability of recursive methods.
 *
 * <p>The main component of this package is {@link com.github.ahauschulte.jtco.TailCall}: An interface representing a
 * tail call that can be evaluated iteratively to avoid deep recursion.
 */
package com.github.ahauschulte.jtco;