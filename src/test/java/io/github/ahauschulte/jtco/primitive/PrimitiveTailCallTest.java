package io.github.ahauschulte.jtco.primitive;

import io.github.ahauschulte.jtco.TailCall;
import io.github.ahauschulte.jtco.TailStep;
import io.github.ahauschulte.jtco.TerminalStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrimitiveTailCallTest {

    private static final int DEEP_RECURSION_DEPTH = 100_000;

    @Nested
    @DisplayName("IntTailCall tests")
    class IntTests {

        private IntTailCall countdown(final int n) {
            if (n <= 0) {
                return IntTailCall.done(0);
            }
            return IntTailCall.call(() -> countdown(n - 1));
        }

        private IntTailCall sum(final int n, final int acc) {
            if (n <= 0) {
                return IntTailCall.terminateWith(acc);
            }
            return IntTailCall.continueWith(() -> sum(n - 1, acc + n));
        }

        @Test
        void testTerminalAndBoxedParity() {
            final IntTailCall step = IntTailCall.terminateWith(42);
            assertThat(step.isTerminal()).isTrue();
            assertThat(step.eval()).isEqualTo(42);
            assertThat(step.boxed().evaluate()).isEqualTo(42);
            assertThatThrownBy(step::proceed).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void testAliasesDoneAndCall() {
            final IntTailCall call = IntTailCall.call(() -> IntTailCall.done(99));
            assertThat(call.eval()).isEqualTo(99);
        }

        @Test
        void testNullChecks() {
            assertThatNullPointerException().isThrownBy(() -> IntTailCall.continueWith(null));
            assertThatNullPointerException().isThrownBy(() -> IntTailCall.call(null));
        }

        @Test
        void testShortChain() {
            final IntTailCall call = IntTailCall.continueWith(() ->
                    IntTailCall.continueWith(() ->
                            IntTailCall.terminateWith(123)));
            assertThat(call.eval()).isEqualTo(123);
        }

        @Test
        void testStackSafetyDeepRecursion() {
            final int result = countdown(DEEP_RECURSION_DEPTH).eval();
            assertThat(result).isEqualTo(0);
        }

        @Test
        void testAccumulatorComputation() {
            final int result = sum(10, 0).eval();
            assertThat(result).isEqualTo(55);
        }

        @Test
        void testBoxedAdapter() {
            final TailCall<Integer> boxed = countdown(5).boxed();
            assertThat(boxed.evaluate()).isEqualTo(0);
        }

        @Test
        void testExceptionPropagation() {
            final IntTailCall failing = IntTailCall.continueWith(() -> {
                throw new IllegalStateException("error in chain");
            });
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(failing::eval)
                    .withMessage("error in chain");
        }

        @Test
        void testInterleave() {
            final AtomicInteger stepsA = new AtomicInteger();
            final AtomicInteger stepsB = new AtomicInteger();

            final List<Supplier<IntTailCall>> list = List.of(
                    () -> {
                        stepsA.incrementAndGet();
                        return countdown(5);
                    },
                    () -> {
                        stepsB.incrementAndGet();
                        return countdown(3);
                    }
            );

            IntTailCall.interleave(list);
            assertThat(stepsA.get()).isEqualTo(1);
            assertThat(stepsB.get()).isEqualTo(1);
        }

        @Test
        void testInterleaveAllWithUnequalSteps() {
            final Supplier<IntTailCall> shortTask = () ->
                    IntTailCall.call(() -> IntTailCall.done(10)); // 2 steps

            final Supplier<IntTailCall> mediumTask = () ->
                    IntTailCall.call(() ->
                            IntTailCall.call(() ->
                                    IntTailCall.call(() ->
                                            IntTailCall.call(() -> IntTailCall.done(25))))); // 5 steps

            final Supplier<IntTailCall> immediateTask = () -> IntTailCall.done(42); // 1 step

            final List<IntTailCall> results = IntTailCall.interleaveAll(List.of(shortTask, mediumTask, immediateTask));

            assertThat(results).hasSize(3);
            assertThat(results.get(0).isTerminal()).isTrue();
            assertThat(results.get(0).eval()).isEqualTo(10);

            assertThat(results.get(1).isTerminal()).isTrue();
            assertThat(results.get(1).eval()).isEqualTo(25);

            assertThat(results.get(2).isTerminal()).isTrue();
            assertThat(results.get(2).eval()).isEqualTo(42);
        }

        @Test
        void testInterleaveAnyStopsOnFirstTerminal() {
            final AtomicInteger secondaryStepped = new AtomicInteger();
            final Supplier<IntTailCall> fastTask = () -> IntTailCall.done(1);
            final Supplier<IntTailCall> slowTask = () -> IntTailCall.call(() -> {
                secondaryStepped.incrementAndGet();
                return IntTailCall.done(2);
            });

            IntTailCall.interleaveAny(List.of(fastTask, slowTask));
            assertThat(secondaryStepped.get()).isZero();
        }
    }

    @Nested
    @DisplayName("LongTailCall tests")
    class LongTests {

        private LongTailCall countdown(final long n) {
            if (n <= 0) {
                return LongTailCall.done(0L);
            }
            return LongTailCall.call(() -> countdown(n - 1));
        }

        @Test
        void testTerminalAndBoxedParity() {
            final LongTailCall step = LongTailCall.terminateWith(123456789L);
            assertThat(step.isTerminal()).isTrue();
            assertThat(step.eval()).isEqualTo(123456789L);
            assertThat(step.boxed().evaluate()).isEqualTo(123456789L);
            assertThatThrownBy(step::proceed).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void testNullChecks() {
            assertThatNullPointerException().isThrownBy(() -> LongTailCall.continueWith(null));
        }

        @Test
        void testStackSafetyDeepRecursion() {
            final long result = countdown(DEEP_RECURSION_DEPTH).eval();
            assertThat(result).isEqualTo(0L);
        }

        @Test
        void testBoxedAdapter() {
            final TailCall<Long> boxed = countdown(10).boxed();
            assertThat(boxed.evaluate()).isEqualTo(0L);
        }

        @Test
        void testInterleave() {
            LongTailCall.interleave(List.of(() -> countdown(2), () -> countdown(4)));
        }
    }

    @Nested
    @DisplayName("DoubleTailCall tests")
    class DoubleTests {

        private DoubleTailCall countdown(final int n, final double acc) {
            if (n <= 0) {
                return DoubleTailCall.done(acc);
            }
            return DoubleTailCall.call(() -> countdown(n - 1, acc + 0.5));
        }

        @Test
        void testTerminalAndBoxedParity() {
            final DoubleTailCall step = DoubleTailCall.terminateWith(3.14159);
            assertThat(step.isTerminal()).isTrue();
            assertThat(step.eval()).isEqualTo(3.14159);
            assertThat(step.boxed().evaluate()).isEqualTo(3.14159);
            assertThatThrownBy(step::proceed).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void testNullChecks() {
            assertThatNullPointerException().isThrownBy(() -> DoubleTailCall.continueWith(null));
        }

        @Test
        void testStackSafetyDeepRecursion() {
            final double result = countdown(DEEP_RECURSION_DEPTH, 0.0).eval();
            assertThat(result).isEqualTo(DEEP_RECURSION_DEPTH * 0.5);
        }

        @Test
        void testBoxedAdapter() {
            final TailCall<Double> boxed = countdown(4, 1.0).boxed();
            assertThat(boxed.evaluate()).isEqualTo(3.0);
        }

        @Test
        void testInterleave() {
            DoubleTailCall.interleave(List.of(() -> countdown(2, 0.0)));
        }
    }

    @Nested
    @DisplayName("FloatTailCall tests")
    class FloatTests {

        private FloatTailCall countdown(final int n, final float acc) {
            if (n <= 0) {
                return FloatTailCall.done(acc);
            }
            return FloatTailCall.call(() -> countdown(n - 1, acc + 1.0f));
        }

        @Test
        void testTerminalAndBoxedParity() {
            final FloatTailCall step = FloatTailCall.terminateWith(2.5f);
            assertThat(step.isTerminal()).isTrue();
            assertThat(step.eval()).isEqualTo(2.5f);
            assertThat(step.boxed().evaluate()).isEqualTo(2.5f);
            assertThatThrownBy(step::proceed).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void testNullChecks() {
            assertThatNullPointerException().isThrownBy(() -> FloatTailCall.continueWith(null));
        }

        @Test
        void testStackSafetyDeepRecursion() {
            final float result = countdown(DEEP_RECURSION_DEPTH, 0.0f).eval();
            assertThat(result).isEqualTo((float) DEEP_RECURSION_DEPTH);
        }

        @Test
        void testBoxedAdapter() {
            final TailCall<Float> boxed = countdown(5, 0.0f).boxed();
            assertThat(boxed.evaluate()).isEqualTo(5.0f);
        }

        @Test
        void testInterleave() {
            FloatTailCall.interleave(List.of(() -> countdown(3, 0.0f)));
        }
    }

    @Nested
    @DisplayName("BooleanTailCall tests")
    class BooleanTests {

        private BooleanTailCall parity(final int n, final boolean isEven) {
            if (n <= 0) {
                return BooleanTailCall.done(isEven);
            }
            return BooleanTailCall.call(() -> parity(n - 1, !isEven));
        }

        @Test
        void testTerminalAndBoxedParity() {
            final BooleanTailCall stepTrue = BooleanTailCall.terminateWith(true);
            final BooleanTailCall stepFalse = BooleanTailCall.terminateWith(false);
            assertThat(stepTrue.eval()).isTrue();
            assertThat(stepTrue.boxed().evaluate()).isTrue();
            assertThat(stepFalse.eval()).isFalse();
            assertThat(stepFalse.boxed().evaluate()).isFalse();
            assertThatThrownBy(stepTrue::proceed).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void testNullChecks() {
            assertThatNullPointerException().isThrownBy(() -> BooleanTailCall.continueWith(null));
        }

        @Test
        void testStackSafetyDeepRecursion() {
            final boolean result = parity(DEEP_RECURSION_DEPTH, true).eval();
            assertThat(result).isTrue(); // Even number of flips
        }

        @Test
        void testBoxedAdapter() {
            final TailCall<Boolean> boxed = parity(3, true).boxed();
            assertThat(boxed.evaluate()).isFalse();
        }

        @Test
        void testInterleave() {
            BooleanTailCall.interleave(List.of(() -> parity(2, true)));
        }
    }

    @Nested
    @DisplayName("ByteTailCall tests")
    class ByteTests {

        private ByteTailCall countdown(final int n, final byte acc) {
            if (n <= 0) {
                return ByteTailCall.done(acc);
            }
            return ByteTailCall.call(() -> countdown(n - 1, (byte) (acc + 1)));
        }

        @Test
        void testTerminalAndBoxedParity() {
            final ByteTailCall step = ByteTailCall.terminateWith((byte) 127);
            assertThat(step.isTerminal()).isTrue();
            assertThat(step.eval()).isEqualTo((byte) 127);
            assertThat(step.boxed().evaluate()).isEqualTo((byte) 127);
            assertThatThrownBy(step::proceed).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void testNullChecks() {
            assertThatNullPointerException().isThrownBy(() -> ByteTailCall.continueWith(null));
        }

        @Test
        void testStackSafetyDeepRecursion() {
            final byte result = countdown(DEEP_RECURSION_DEPTH, (byte) 0).eval();
            assertThat(result).isEqualTo((byte) (DEEP_RECURSION_DEPTH % 256));
        }

        @Test
        void testBoxedAdapter() {
            final TailCall<Byte> boxed = countdown(5, (byte) 10).boxed();
            assertThat(boxed.evaluate()).isEqualTo((byte) 15);
        }

        @Test
        void testInterleave() {
            ByteTailCall.interleave(List.of(() -> countdown(3, (byte) 0)));
        }
    }

    @Nested
    @DisplayName("ShortTailCall tests")
    class ShortTests {

        private ShortTailCall countdown(final int n, final short acc) {
            if (n <= 0) {
                return ShortTailCall.done(acc);
            }
            return ShortTailCall.call(() -> countdown(n - 1, (short) (acc + 1)));
        }

        @Test
        void testTerminalAndBoxedParity() {
            final ShortTailCall step = ShortTailCall.terminateWith((short) 32000);
            assertThat(step.isTerminal()).isTrue();
            assertThat(step.eval()).isEqualTo((short) 32000);
            assertThat(step.boxed().evaluate()).isEqualTo((short) 32000);
            assertThatThrownBy(step::proceed).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void testNullChecks() {
            assertThatNullPointerException().isThrownBy(() -> ShortTailCall.continueWith(null));
        }

        @Test
        void testStackSafetyDeepRecursion() {
            final short result = countdown(DEEP_RECURSION_DEPTH, (short) 0).eval();
            assertThat(result).isEqualTo((short) (DEEP_RECURSION_DEPTH));
        }

        @Test
        void testBoxedAdapter() {
            final TailCall<Short> boxed = countdown(10, (short) 5).boxed();
            assertThat(boxed.evaluate()).isEqualTo((short) 15);
        }

        @Test
        void testInterleave() {
            ShortTailCall.interleave(List.of(() -> countdown(2, (short) 0)));
        }
    }

    @Nested
    @DisplayName("CharTailCall tests")
    class CharTests {

        private CharTailCall shiftChar(final int n, final char c) {
            if (n <= 0) {
                return CharTailCall.done(c);
            }
            final char next = (char) ('a' + ((c - 'a' + 1) % 26));
            return CharTailCall.call(() -> shiftChar(n - 1, next));
        }

        @Test
        void testTerminalAndBoxedParity() {
            final CharTailCall step = CharTailCall.terminateWith('Z');
            assertThat(step.isTerminal()).isTrue();
            assertThat(step.eval()).isEqualTo('Z');
            assertThat(step.boxed().evaluate()).isEqualTo('Z');
            assertThatThrownBy(step::proceed).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void testNullChecks() {
            assertThatNullPointerException().isThrownBy(() -> CharTailCall.continueWith(null));
        }

        @Test
        void testStackSafetyDeepRecursion() {
            final char result = shiftChar(DEEP_RECURSION_DEPTH, 'a').eval();
            final char expected = (char) ('a' + (DEEP_RECURSION_DEPTH % 26));
            assertThat(result).isEqualTo(expected);
        }

        @Test
        void testBoxedAdapter() {
            final TailCall<Character> boxed = shiftChar(3, 'a').boxed();
            assertThat(boxed.evaluate()).isEqualTo('d');
        }

        @Test
        void testInterleave() {
            CharTailCall.interleave(List.of(() -> shiftChar(2, 'a')));
        }
    }

    @Nested
    @DisplayName("TailStep and universal interleave tests")
    class UniversalTailStepTests {

        @Test
        void testMixedUniversalInterleave() {
            final AtomicInteger intStep = new AtomicInteger();
            final AtomicInteger longStep = new AtomicInteger();
            final AtomicInteger genericStep = new AtomicInteger();

            final List<Supplier<TailStep>> mixedList = List.of(
                    () -> {
                        intStep.incrementAndGet();
                        return IntTailCall.call(() -> IntTailCall.done(1));
                    },
                    () -> {
                        longStep.incrementAndGet();
                        return LongTailCall.call(() -> LongTailCall.done(2L));
                    },
                    () -> {
                        genericStep.incrementAndGet();
                        return TailCall.call(() -> TailCall.done("done"));
                    }
            );

            TailStep.interleave(mixedList);
            assertThat(intStep.get()).isEqualTo(1);
            assertThat(longStep.get()).isEqualTo(1);
            assertThat(genericStep.get()).isEqualTo(1);
        }

        @Test
        void testTailStepInterleaveNullSuppliersThrows() {
            assertThatNullPointerException()
                    .isThrownBy(() -> TailStep.interleave(null));
        }

        @Test
        void testTailStepInterleaveEmptyListDoesNothing() {
            TailStep.interleave(new ArrayList<>());
        }

        @Test
        void testTailStepInterleaveWithNullElementThrows() {
            final List<Supplier<TailStep>> suppliers = new ArrayList<>();
            suppliers.add(null);
            assertThatNullPointerException()
                    .isThrownBy(() -> TailStep.interleave(suppliers));
        }

        @Test
        void testTailStepInterleaveWithSupplierReturningNullThrows() {
            final List<Supplier<TailStep>> suppliers = List.of(() -> null);
            assertThatNullPointerException()
                    .isThrownBy(() -> TailStep.interleave(suppliers));
        }

        @Test
        void testTailStepInterleaveAllMixedUnequalSteps() {
            final Supplier<IntTailCall> intTask = () ->
                    IntTailCall.call(() ->
                            IntTailCall.call(() -> IntTailCall.done(111))); // 3 steps

            final Supplier<TailCall<String>> genericTask = () ->
                    TailCall.call(() -> TailCall.done("finished")); // 2 steps

            final Supplier<DoubleTailCall> doubleTask = () ->
                    DoubleTailCall.done(3.14159); // 1 step

            final List<Supplier<? extends TailStep>> mixed = List.of(intTask, genericTask, doubleTask);
            final List<TailStep> results = TailStep.interleaveAll(mixed);

            assertThat(results).hasSize(3);
            assertThat(results.get(0).isTerminal()).isTrue();
            assertThat(((IntTailCall) results.get(0)).eval()).isEqualTo(111);

            assertThat(results.get(1).isTerminal()).isTrue();
            @SuppressWarnings("unchecked")
            final TailCall<String> tc = (TailCall<String>) results.get(1);
            assertThat(tc.evaluate()).isEqualTo("finished");

            assertThat(results.get(2).isTerminal()).isTrue();
            assertThat(((DoubleTailCall) results.get(2)).eval()).isEqualTo(3.14159);
        }
    }
}
