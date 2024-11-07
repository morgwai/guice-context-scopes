// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.function;

import java.util.concurrent.Callable;
import java.util.function.*;



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



	static ThrowingTask<Void, RuntimeException, RuntimeException>
	newThrowingTask(Runnable runnable) {
		return new ThrowingTask<>() {
			@Override public Void execute() {
				runnable.run();
				return null;
			}
			@Override public String toString() {
				return "ThrowingTask { runnable = " + runnable + " }";
			}
		};
	}



	static <R> ThrowingTask<R, Exception, RuntimeException>
	newThrowingTask(Callable<R> callable) {
		return new ThrowingTask<>() {
			@Override public R execute() throws Exception {
				return callable.call();
			}
			@Override public String toString() {
				return "ThrowingTask { callable = " + callable + " }";
			}
		};
	}



	static <T> ThrowingTask<Void, RuntimeException, RuntimeException>
	newThrowingTask(Consumer<T> consumer, T param) {
		return new ThrowingTask<>() {
			@Override public Void execute() {
				consumer.accept(param);
				return null;
			}
			@Override public String toString() {
				final var q = quote(param);
				return "ThrowingTask { consumer = " + consumer + ", param = " + q + param + q
						+ " }";
			}
		};
	}



	static <T, U> ThrowingTask<Void, RuntimeException, RuntimeException>
	newThrowingTask(BiConsumer<T, U> biConsumer, T param1, U param2) {
		return new ThrowingTask<>() {
			@Override public Void execute() {
				biConsumer.accept(param1, param2);
				return null;
			}
			@Override public String toString() {
				final var q1 = quote(param1);
				final var q2 = quote(param2);
				return "ThrowingTask { biConsumer = " + biConsumer + ", param1 = " + q1 + param1
						+ q1 + ", param2 = " + q2 + param2 + q2 + " }";
			}
		};
	}



	static <T, R> ThrowingTask<R, RuntimeException, RuntimeException>
	newThrowingTask(Function<T, R> function, T param) {
		return new ThrowingTask<>() {
			@Override public R execute() {
				return function.apply(param);
			}
			@Override public String toString() {
				final var q = quote(param);
				return "ThrowingTask { function = " + function + ", param = " + q + param + q
						+ " }";
			}
		};
	}



	static <T, U, R> ThrowingTask<R, RuntimeException, RuntimeException>
	newThrowingTask(BiFunction<T, U, R> biFunction, T param1, U param2) {
		return new ThrowingTask<>() {
			@Override public R execute() {
				return biFunction.apply(param1, param2);
			}
			@Override public String toString() {
				final var q1 = quote(param1);
				final var q2 = quote(param2);
				return "ThrowingTask { biFunction = " + biFunction + ", param1 = " + q1 + param1
						+ q1 + ", param2 = " + q2 + param2 + q2 + " }";
			}
		};
	}



	static <R> ThrowingTask<R, RuntimeException, RuntimeException>
	newThrowingTaskOfSupplier(Supplier<R> supplier) {
		return new ThrowingTask<>() {
			@Override public R execute() {
				return supplier.get();
			}
			@Override public String toString() {
				return "ThrowingTask { supplier = " + supplier + " }";
			}
		};
	}



	private static String quote(Object o) {
		return o instanceof String ? "\"" : o instanceof Character ? "'" : "";
	}
}
