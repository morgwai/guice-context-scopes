# Guice Context Scopes

Classes for building Guice `Scope`s easily transferable when dispatching work to other `Thread`s.<br/>
Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0<br/>
<br/>
**latest release: [10.1](https://search.maven.org/artifact/pl.morgwai.base/guice-context-scopes/10.1/jar)**
([javadoc](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/10.1))


## OVERVIEW

Asynchronous servers (such as gRPC or asynchronous `Servlet`s) often need to switch between various `Thread`s. This requires extra care to not lose a given current Guice `Scope`: it needs to be preserved as long as we are in the  _context_  of a given request/call/session, regardless of `Thread` switching.<br/>
<br/>
To ease this up, this lib formally introduces a notion of an [InjectionContext](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/InjectionContext.html) that stores scoped objects and can be tracked using [ContextTracker](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextTracker.html)s when switching between `Thread`s. `Tracker`s are in turn used by [ContextScope](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextScope.html)s to obtain a `Context` that is current at a given moment and from which scoped objects will be obtained.<br/>
<br/>
When switching `Thread`s in low level library code, static helper methods [ContextTracker.getActiveContexts(List<ContextTracker<?>>)](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextTracker.html#getActiveContexts(java.util.List)) and [TrackableContext.executeWithinAll(List<TrackableContext>, Runnable)](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/TrackableContext.html#executeWithinAll(java.util.List,java.lang.Runnable)) can be used to manually transfer all active `Context`s:
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
For higher level abstraction and for end users, [ContextBinder](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextBinder.html) was introduced that allows to bind closures defined as common functional interfaces (`Runnable`, `Callable`, `Consumer`, `BiConsumer`, `Function`, `BiFunction`) to `Context`s active at the time of a given binding:
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
Deriving libs should provide implementations of `ExecutorService` that fully automate `Context` transfers:
```java
class MyContextTrackingExecutor extends ThreadPoolExecutor {

    @Inject ContextBinder ctxBinder;

    @Override public void execute(Runnable task) {
        super.execute(ctxBinder.bindToContext(task));
    }

    // constructors here...
}
```
See the [package level javadoc](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/package-summary.html) for full code organization guidelines for deriving libs.


## DERIVED LIBS

[gRPC Guice Scopes](https://github.com/morgwai/grpc-scopes)<br/>
[Servlet and Websocket Guice Scopes](https://github.com/morgwai/servlet-scopes)
