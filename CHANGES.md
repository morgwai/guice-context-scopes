# Summaries of visible changes between releases

### 12.1
- Support for `Context` nesting without joining scoped `Object`s and ability to get a reference to an enclosing `Context`. **NOTE:** this induced a change of the serialized form, so any persistent `Context` stores need to be drained.

### 12.0
- `InjectionContext`: `removeScopedObject(key)` now returns `boolean`, `produceIfAbsent(...)` is now package-private.
- `ScopeModule`: add `newInducedContextScope(...)` variant accepting a base `Scope` instead of a `Tracker`.
- `ContextScope`: more descriptive `toString()`.
- Mark some methods as `final`.

### 11.0
- Replace `ContextTrackingExecutorDecorator` class with `ContextTrackingExecutor` interface.
- Rename `ContextScopesModule` to `ScopeModule`.
- Replace variants of `executeWithinAll(...)` and `executeWithinSelf(task)` taking a `Callable` with ones taking a `Throwing4Computation` and a `Throwing4Task` from [functional-interfaces](https://github.com/morgwai/functional-interfaces).
- Refactor `ContextBoundClosure` subclasses to use `ThrowingTask` and `ThrowingComputation`.
- Remove unused `ContextBoundClosure.RunnableWrapper`.
- Support for nested `Context`s in `InjectionContext`.
- Convert `ContextScope.ScopedProvider` to an anonymous class.
