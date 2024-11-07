// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;



/** {@link ThrowingTask}'s super class with 5 "configurable" {@link Exception} types. */
@FunctionalInterface
public interface Throwing5Task<
	R,
	E1 extends Exception,
	E2 extends Exception,
	E3 extends Exception,
	E4 extends Exception,
	E5 extends Exception
> {

	R execute() throws E1, E2, E3, E4, E5;
}
