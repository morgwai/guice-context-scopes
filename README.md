# Guice Context Scopes

Classes for building Guice [Scope](https://google.github.io/guice/api-docs/6.0.0/javadoc/com/google/inject/Scope.html)s easily transferable when dispatching work to other `Thread`s.<br/>
Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0<br/>
<br/>
**latest release: [10.1](https://search.maven.org/artifact/pl.morgwai.base/guice-context-scopes/10.1/jar)**
([javadoc](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/10.1))


## OVERVIEW

Asynchronous servers (such as gRPC or asynchronous `Servlet`s) often need to switch between various `Thread`s. This requires extra care to not lose a given current Guice `Scope`: it needs to be preserved as long as we are in the  _context_  of a given event/request/call/session, regardless of `Thread` switching.

To ease this up, this lib formally introduces a notion of an [InjectionContext](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/InjectionContext.html) that stores scoped `Object`s and can be tracked using [ContextTracker](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextTracker.html)s when switching between `Thread`s. `Tracker`s are in turn used by [ContextScope](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextScope.html)s to obtain the `Context` that is current at a given moment and from which scoped `Object`s will be obtained.


## DEVELOPING SCOPES

Creation of a (set of) custom `Scope`(s) boils down to the below things:
1. Defining a concrete subclass of [TrackableContext](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/TrackableContext.html) (subclass of `InjectionContext`). For example `ServletRequestContext` in case of Java Servlet containers.
1. Creating a `ContextTracker` instance and a `ContextScope` instance corresponding to the above `TrackableContext` subclass in some central, easy to access location.
1. Hooking creation of the above `TrackableContext` instances into a given existing framework: for example in case of Java Servlet containers, a `Filter` may be created that for each new incoming `ServletRequest` will [execute](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/TrackableContext.html#executeWithinSelf(java.util.concurrent.Callable)) `chain.doFilter(request, response)` within a newly created `ServletRequestContext` instance.
1. Optionally defining subclasses of `InjectionContext` for `Context` types that are _induced_ by the above `TrackableContext`. For example entering into a `ServletRequestContext` may induce entering into the corresponding `HttpSessionContext`.
1. Creating [InducedContextScope](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/InducedContextScope.html) instances corresponding to the above induced `Context` types, if any.

See the [package level javadoc](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/package-summary.html) for full code organization guidelines for deriving libs.


## USAGE

When switching `Thread`s in a low level library code, static helper methods [getActiveContexts(List&lt;ContextTracker&lt;?&gt;&gt;)](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextTracker.html#getActiveContexts(java.util.List)) and [executeWithinAll(List&lt;TrackableContext&gt;, Runnable)](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/TrackableContext.html#executeWithinAll(java.util.List,java.lang.Runnable)) can be used to manually transfer all active `Context`s:
```java
class MyComponent {

    // deriving libraries should bind List<ContextTracker<?>> appropriately
    @Inject List<ContextTracker<?>> allTrackers;

    void methodThatCallsSomeAsyncMethod(/* ... */) {
        // other code here...
        final var activeCtxs = ContextTracker.getActiveContexts(allTrackers);
        someAsyncMethod(
            arg1,
            /* ... */
            argN,
            (callbackParamIfNeeded) -> TrackableContext.executeWithinAll(
                activeCtxs,
                () -> {
                    // callback code here...
                }
            )
        );
    }
}
```

For higher level abstraction and for end users, [ContextBinder](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextBinder.html) class was introduced that allows to bind closures defined as common functional interfaces (`Runnable`, `Callable`, `Consumer`, `BiConsumer`, `Function`, `BiFunction`) to `Context`s that were active at the time of a given binding:
```java
class MyComponent {  // compare with the "low-level" version above

    @Inject ContextBinder ctxBinder;

    void methodThatCallsSomeAsyncMethod(/* ... */) {
        // other code here...
        someAsyncMethod(
            arg1,
            /* ... */
            argN,
            ctxBinder.bindToContext((callbackParamIfNeeded) -> {
                // callback code here...
            })
        );
    }
}
```

`ContextBinder` may also be used to create `Exectuor`s that automatically transfer active `Context`s when executing tasks:
```java
class MyContextTrackingExecutor extends ThreadPoolExecutor {

    @Inject ContextBinder ctxBinder;

    @Override public void execute(Runnable task) {
        super.execute(ctxBinder.bindToContext(task));
    }

    // constructors here...
}
```
For convenience [ContextTrackingExecutorDecorator](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextTrackingExecutorDecorator.html) class was provided to add the above `Context` transferring functionality to any existing `ExecutorService`.


## DERIVED LIBS

[gRPC Guice Scopes](https://github.com/morgwai/grpc-scopes)<br/>
[Servlet and Websocket Guice Scopes](https://github.com/morgwai/servlet-scopes)
