// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;



/**
 * Task that can throw configurable {@link Exception}s.
 * This allows to precisely declare thrown types and avoid boilerplate try-catch-rethrow blocks.
 * <p>
 * Note: Java compiler as of version 11 is unable to reasonably infer types if a lambda expression
 * throws more than 1 {@link Exception}. In such cases it is necessary to cast such an expression to
 * {@code ThrowingTask} with specific type arguments:</p>
 * <pre>{@code
 * ctx.executeWithinSelf(
 *     (ThrowingTask<Void, IOException, ServletException, RuntimeException>) () -> {
 *         chain.doFilter(request, response);
 *         return null;  // Void
 *     }
 * );}</pre>
 * <p>...Or to pass explicit arguments to a given generic method:</p>
 * <pre>{@code
 * ctx.<Void, IOException, ServletException, RuntimeException>executeWithinSelf(
 *     () -> {
 *         chain.doFilter(request, response);
 *         return null;  // Void
 *     }
 * );}</pre>
 */
@FunctionalInterface
public interface ThrowingTask<R, E1 extends Exception, E2 extends Exception, E3 extends Exception> {

	R execute() throws E1, E2, E3;
}
