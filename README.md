# jtco

[![Java CI with Maven](https://github.com/ahauschulte/jtco/actions/workflows/maven.yml/badge.svg)](https://github.com/ahauschulte/jtco/actions/workflows/maven.yml)
[![javadoc](https://javadoc.io/badge2/io.github.ahauschulte.jtco/jtco/javadoc.svg)](https://javadoc.io/doc/io.github.ahauschulte.jtco/jtco)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ahauschulte.jtco/jtco.svg)](https://central.sonatype.com/artifact/io.github.ahauschulte.jtco/jtco)

## Summary

The jtco (Java Tail Call Optimisation) library provides a substitute for Java’s missing tail call optimisation
capabilities. It implements a variant of the trampoline pattern to allow tail-recursive methods to execute without
increasing the call stack size, thereby preventing stack overflow errors. By converting recursive calls into a loop
that repeatedly invokes methods without adding new stack frames, jtco enables deeply recursive methods to run in
constant stack space. jtco is implemented in plain Java 21 and has no runtime dependencies.

## Recursion

A recursive method call is a method that calls itself within its own code. This technique is used to solve
problems that can be broken down into smaller, similar subproblems. A recursive method typically has a base
case that terminates the recursion and one or more recursive cases that reduce the problem's size and bring
it closer to the base case. An example is the calculation of the factorial of a number.

```java
public int factorial(final int n) {
    if (n <= 1) { // Base case
        return 1;
    } else { // Recursive case
        return n * factorial(n - 1);
    }
}
```

Recursive methods offer both advantages and disadvantages. One of the main advantages is their ability to solve
problems in a concise and elegant manner, especially when dealing with tasks that can be broken down into smaller,
similar subproblems. Recursion can lead to cleaner and more readable code, often reflecting the natural structure
of the problem being solved.

However, recursion also has its drawbacks. The most significant disadvantage is the risk of a stack overflow error
if the recursion depth becomes too deep, which can occur with problems that require many recursive calls or
with large input values. Each recursive call consumes a stack frame, and when the stack memory is exhausted,
a stack overflow occurs.

Beyond the risk of stack overflow, recursion introduces several other performance-related issues. Recursion incurs
a significant overhead due to the mechanics of method calls. Every time a method calls itself, the system must push
the current state onto the stack, jump to the method’s code, and set up a new execution context. This involves
numerous low-level operations, such as saving registers, setting up the stack frame, and managing the return address.
These operations, while quick in isolation, accumulate substantial overhead in deeply recursive methods, leading to
slower execution compared to iterative solutions.

Additionally, recursive methods often present limited optimisation opportunities for modern compilers, including the
Java HotSpot Compiler. While iterative loops can benefit from various compiler optimisations, such as loop unrolling
and vectorisation, recursive methods are more challenging to optimise due to their reliance on the call stack and
the dynamic nature of recursion. This means that recursive algorithms may not achieve the same level of performance
enhancements as their iterative counterparts, potentially leading to slower execution times.

This is where tail call optimisation becomes beneficial.

## Tail Call Optimisation

Tail Call Optimisation (TCO) is a sophisticated technique employed in many programming languages to enhance the
performance of recursive methods. When a method makes a call to another method as its final action, it is termed a
“tail call.” TCO transforms these tail calls into a more efficient form of execution, thereby preventing the
accumulation of stack frames which typically occurs with standard recursive calls.

At its core, TCO works by recognising that the current method’s stack frame is no longer needed after the tail call is
made. Since the method has no further work to perform after the call returns, its stack frame can be safely discarded.
The stack frame of the current method is then replaced with the stack frame of the called method, thereby maintaining a
constant stack size regardless of the recursion depth.

This optimisation is particularly beneficial for algorithms that rely heavily on recursion, such as those found in
functional programming paradigms. Without TCO, deep recursion can lead to stack overflow errors, as each call consumes
stack space.

### Benefits of Tail Call Optimisation

1. Efficiency: By reusing stack frames, TCO reduces the overhead associated with recursive method calls, leading to
   improved performance and faster execution times.
2. Scalability: TCO allows recursive methods to handle large inputs and deep recursion levels without running out of
   stack space, making them more robust and scalable.
3. Elegance: Recursive solutions often reflect the natural structure of problems, especially in domains such as
   tree traversal, mathematical computations, and functional programming. TCO enables developers to maintain this
   elegance without compromising on performance or risking stack overflow errors.
4. Memory Management: By maintaining a constant stack size, TCO leads to more predictable memory usage, which is
   crucial in resource-constrained environments, real-time systems, or for safety-critical applications.

## Leveraging Tail Call Optimisation Techniques in Java 

Unfortunately, the common JVM implementations do not natively support TCO for various reasons. However, developers can
employ certain techniques to simulate its benefits. One of those techniques is the so-called trampoline pattern. The
trampoline pattern allows for the iterative execution of tail-recursive methods. This involves structuring the
tail-recursive calls such that each call returns an object representing a method call to be executed next, and a loop
repeatedly invokes these method objects without growing the call stack.

The introduction of lambdas in Java 8 significantly benefited the trampoline pattern by simplifying the code and
enhancing readability. Lambdas allow for concise and expressive function definitions, making it easier to represent
the recursive steps as a functional interface. This reduces boilerplate code and improves the overall clarity and
maintainability of the trampoline implementation.

The jtco library addresses the lack of native TCO in Java by offering an implementation of the trampoline pattern. It
provides means that enables tail-recursive methods to be executed without increasing the call stack size, effectively
preventing stack overflow errors. The API is designed with structure and simplicity in mind, promoting the creation of
clean, readable, and maintainable client code. By utilising lambdas and functional programming constructs, jtco
simplifies the creation of efficient and elegant recursive algorithms, allowing developers to leverage tail call
optimisation techniques within the Java ecosystem.

While jtco brings a touch of tail call optimisation to Java, it is essential to understand the distinction between true
TCO and the trampoline pattern. True TCO is a compiler-level optimisation that reuses the current method’s stack frame
for tail calls, ensuring constant stack space and efficient execution. In contrast, the trampoline pattern simulates TCO
by manually managing the execution flow through an iterative loop, invoking methods without growing the call stack. The
trampoline pattern, while effective, is not completely equivalent to true TCO due to its additional overhead. It
requires wrapping recursive calls in lambda expressions and repeatedly invoking them in a loop, which introduces runtime
complexity and can result in slower performance compared to native TCO. True TCO is inherently more efficient, as it
optimises at the compiler level without the need for such manual intervention.

## Synchronous Method Interleaving

The jtco library offers a straightforward way to implement synchronous method interleaving. This technique coordinates
the execution of multiple recursive methods so that they advance incrementally together, rather than allowing one method
to complete entirely before starting the next. This ensures balanced progression among tasks, which is essential in
scenarios demanding fairness and balanced resource usage.

### Key Characteristics of Synchronous Method Interleaving

1. Step-by-Step Advancement: Each recursive method progresses one step at a time in a coordinated fashion.
2. Fair Resource Usage: Ensures that all tasks are given equal opportunity to make progress, avoiding scenarios where
   one task monopolizes the resources.
3. Avoiding Stack Overflow: By using TCO, synchronous method interleaving helps prevent stack overflow errors,
   even with deep recursion.

While jtco’s approach to synchronous method interleaving is suitable for many scenarios, there may be instances where
its API lacks the necessary flexibility. There is an inherent tension between providing maximal flexibility and jtco’s
design goals of maintaining a structured, simple, and robust API. In situations where jtco’s capabilities prove too
restrictive, it is advisable to employ a tailored implementation of the trampoline pattern to achieve the desired
flexibility and control.

## Examples

### Factorial

```java
import io.github.ahauschulte.jtco.TailCall;

import java.math.BigInteger;

/**
 * Factorial demo
 *
 * <p>This example showcases how to use the {@link TailCall} interface and its implementations
 * to compute the factorial of a number in a way that avoids deep recursion by leveraging
 * tail call optimisation.
 */
public class Factorial {

    /**
     * Calculates the factorial of a given number using a tail-recursive approach.
     *
     * <p>This method uses the {@link TailCall} interface to create a tail-recursive
     * implementation of the factorial function. The method continues the tail call
     * chain until it reaches the base case, at which point it returns the accumulated
     * result.
     *
     * @param n       the number to calculate the factorial for
     * @param prevAcc the accumulator that holds the intermediate results of the factorial calculation
     * @return a {@code TailCall} representing the next step in the tail call chain
     */
    private static TailCall<BigInteger> factorial(final int n, final BigInteger prevAcc) {
        if (n == 0) {
            return TailCall.terminateWith(prevAcc);
        } else {
            final BigInteger nextAcc = BigInteger.valueOf(n).multiply(prevAcc);
            return TailCall.continueWith(() -> factorial(n - 1, nextAcc));
        }
    }

    /**
     * Initiates the factorial calculation for a given number.
     *
     * <p>This method initializes the accumulator to 1 (as a {@code BigInteger})
     * and starts the tail call chain by calling the {@link #factorial(int, BigInteger)} method.
     *
     * @param n the number to calculate the factorial for
     * @return the factorial of the given number as a {@code BigInteger}
     */
    private static BigInteger factorial(final int n) {
        return factorial(n, BigInteger.ONE).evaluate();
    }

    public static void main(final String[] args) {
        final int n = 1_000;
        System.out.printf("factorial(%d): %d%n", n, factorial(n));
    }
}
```

### Fibonacci Sequence

```java
import io.github.ahauschulte.jtco.TailCall;

import java.math.BigInteger;

/**
 * Fibonacci demo
 *
 * <p>This example shows how to use the {@link TailCall} interface to calculate the
 * Fibonacci sequence in a tail call optimized manner, avoiding deep recursion and
 * enhancing performance.
 */
public class Fibonacci {

    /**
     * Calculates the Fibonacci number for a given position using a tail-recursive approach.
     *
     * <p>This method uses the {@link TailCall} interface to create a tail-recursive
     * implementation of the Fibonacci function. The method continues the tail call
     * chain until it reaches the base case, at which point it returns the accumulated
     * result.
     *
     * @param n the position in the Fibonacci sequence to calculate
     * @param a the Fibonacci number at position n-1
     * @param b the Fibonacci number at position n
     * @return a {@code TailCall} representing the next step in the tail call chain
     */
    private static TailCall<BigInteger> fibonacci(final long n, final BigInteger a, final BigInteger b) {
        if (n == 0) {
            return TailCall.terminateWith(a);
        } else {
            return TailCall.continueWith(() -> fibonacci(n - 1, b, a.add(b)));
        }
    }

    /**
     * Initiates the Fibonacci calculation for a given number.
     *
     * <p>This method initialises the first two numbers in the Fibonacci sequence (0 and 1)
     * and starts the tail call chain.
     *
     * @param n the position in the Fibonacci sequence to calculate
     * @return the Fibonacci number at the given position as a {@code BigInteger}
     */
    private static BigInteger fibonacci(final int n) {
        return fibonacci(n, BigInteger.ZERO, BigInteger.ONE).evaluate();
    }

    public static void main(final String[] args) {
        final int n = 1_000;
        System.out.printf("fibonacci(%d): %d%n", n, fibonacci(n));
    }
}
```

### Synchronous Method Interleaving

A somewhat contrived example of how to employ synchronous method interleaving.

```java
import io.github.ahauschulte.jtco.TailCall;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Synchronous method interleaving demo
 *
 * <p>This class interleaves the computation of factorial and Fibonacci numbers, combining their results step-by-step.
 */
public class Interleaving {
    private record CombinedResult(BigInteger factorial, BigInteger fibonacci) {
    }

    private static final Deque<BigInteger> INDIVIDUAL_RESULT_STACK = new ArrayDeque<>();
    private static final Deque<CombinedResult> COMBINED_RESULT_STACK = new ArrayDeque<>();

    /**
     * Computes the factorial of a number using tail-recursive calls.
     *
     * @param n       the number to compute the factorial for
     * @param i       the current step in the recursion
     * @param prevAcc the accumulator holding the intermediate result
     * @return a {@link TailCall} representing the next step in the tail call chain
     */
    private static TailCall<BigInteger> factorial(final int n, final int i, final BigInteger prevAcc) {
        INDIVIDUAL_RESULT_STACK.push(prevAcc);
        if (i > n) {
            return TailCall.terminateWith(prevAcc);
        } else {
            final BigInteger nextAcc = BigInteger.valueOf(i).multiply(prevAcc);
            return TailCall.continueWith(() -> factorial(n, i + 1, nextAcc));
        }
    }

    /**
     * Computes the Fibonacci number at a given position using tail-recursive calls.
     *
     * @param n the position in the Fibonacci sequence
     * @param a the Fibonacci number at position n-1
     * @param b the Fibonacci number at position n
     * @return a {@link TailCall} representing the next step in the tail call chain
     */
    private static TailCall<BigInteger> fibonacci(final long n, final BigInteger a, final BigInteger b) {
        INDIVIDUAL_RESULT_STACK.push(a);
        if (n == 0) {
            return TailCall.terminateWith(a);
        } else {
            return TailCall.continueWith(() -> fibonacci(n - 1, b, a.add(b)));
        }
    }

    /**
     * Combines the results of the factorial and Fibonacci computations.
     *
     * @return a {@link TailCall} representing the next step in the tail call chain
     */
    private static TailCall<Void> combineFactorialAndFibonacci() {
        if (INDIVIDUAL_RESULT_STACK.isEmpty()) {
            return TailCall.terminateWith(null);
        } else {
            final BigInteger factorial = INDIVIDUAL_RESULT_STACK.pollLast();
            final BigInteger fibonacci = INDIVIDUAL_RESULT_STACK.pollLast();
            COMBINED_RESULT_STACK.push(new CombinedResult(factorial, fibonacci));
            return TailCall.continueWith(Interleaving::combineFactorialAndFibonacci);
        }
    }

    public static void main(final String[] args) {
        TailCall.interleave(List.of(
                () -> factorial(9, 1, BigInteger.ONE),
                () -> fibonacci(15, BigInteger.ZERO, BigInteger.ONE),
                Interleaving::combineFactorialAndFibonacci
        ));
        COMBINED_RESULT_STACK.reversed().forEach(System.out::println);
    }
}
```

## Motivation

I created this project to learn about the trampoline pattern and its application as a substitute for Java's lack of
tail call optimisation. Developing the little jtco library has been quite a fun learning experience, showing me how to
elegantly compensate for Java's limitations in this area.

I also had great fun having conversations with ChatGPT, which I extensively used for creating this documentation and
the JavaDoc for the library.

## AI Tools Used

This project utilises AI tools, specifically ChatGPT by OpenAI, to assist with documentation. All AI-generated content
has been reviewed and validated by human contributors to ensure accuracy and quality.
