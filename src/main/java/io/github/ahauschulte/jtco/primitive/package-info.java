/**
 * Contains specialized tail call types for all Java primitive types, providing zero-boxing evaluation.
 *
 * <p>Each primitive type provides a dedicated tail call interface (e.g. {@link io.github.ahauschulte.jtco.primitive.IntTailCall},
 * {@link io.github.ahauschulte.jtco.primitive.LongTailCall}) whose {@code eval()} method executes the tail call
 * chain without any boxing overhead.
 */
package io.github.ahauschulte.jtco.primitive;
