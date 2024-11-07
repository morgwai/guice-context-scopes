// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;



/**
 * Task that can throw "configurable" {@link Exception} types.
 * This allows to precisely declare thrown types and avoid boilerplate try-catch-rethrow blocks.
 * <p>
 * Note: Java compiler as of version 11 is able to accurately infer types of 0 or 1
 * {@link Exception}s thrown by a lambda expression (inferring {@link RuntimeException} to "fill the
 * blanks" where needed). However if a lambda throws 2 (or more) {@link Exception}s, it is necessary
 * to cast such an expression to {@code ThrowingTask} (or {@link Throwing5Task}) with specific type
 * arguments:</p>
 * <pre>{@code
 * public void doFilter(
 *     ServletRequest request,
 *     ServletResponse response,
 *     FilterChain chain
 * ) throws IOException, ServletException {
 *     final var ctx = getContext((HttpServletRequest) request);
 *     ctx.executeWithinSelf(
 *         (ThrowingTask<Void, IOException, ServletException>) () -> {
 *             chain.doFilter(request, response);
 *             return null;  // Void
 *         }
 *     );
 * }}</pre>
 * <p>...Or to pass explicit arguments to a given generic method:</p>
 * <pre>{@code
 * ctx.<Void, IOException, ServletException, RuntimeException, RuntimeException, RuntimeException>
 *         executeWithinSelf(
 *     () -> {
 *         chain.doFilter(request, response);
 *         return null;  // Void
 *     }
 * );}</pre>
 */
@FunctionalInterface
public interface ThrowingTask<
	R,
	E1 extends Exception,
	E2 extends Exception
> extends Throwing5Task<R, E1, E2, RuntimeException, RuntimeException, RuntimeException> {

	R execute() throws E1, E2;
}
