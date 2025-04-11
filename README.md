# Guice Context Scopes

Classes for building Guice [Scope](https://google.github.io/guice/api-docs/6.0.0/javadoc/com/google/inject/Scope.html)s easily transferable when dispatching work to other `Thread`s.<br/>
Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0<br/>
<br/>
**latest release: [12.0](https://search.maven.org/artifact/pl.morgwai.base/guice-context-scopes/12.0/jar)**
([javadoc](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/12.0))<br/>
<br/>
See [CHANGES](CHANGES.md) for the summary of changes between releases. If the major version of a subsequent release remains unchanged, it is supposed to be backwards compatible in terms of API and behaviour with previous ones with the same major version (meaning that it should be safe to just blindly update in dependent projects and things should not break under normal circumstances).


## OVERVIEW

Asynchronous apps (such as gRPC, websockets, async `Servlet`s etc) often need to switch between various `Thread`s. This requires an extra care not to lose the Guice `Scope` associated with a given event/request/call/session: it needs to be preserved as long as we are in the  _context_  of such an event/request/call/session, regardless of `Thread` switching.

To ease this up, this lib formally introduces a notion of an [InjectionContext](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/InjectionContext.html) that stores scoped `Object`s and can be tracked using [ContextTracker](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextTracker.html)s when switching between `Thread`s. `Tracker`s are in turn used by [ContextScope](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextScope.html)s to obtain the `Context` that is current at a given moment and from which scoped `Object`s will be obtained.


## DEVELOPING SCOPES

Creation of a (set of) custom `Scope`(s) boils down to the below things:
1. Defining at least one concrete subclass of [TrackableContext](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/TrackableContext.html) (subclass of `InjectionContext`). For example `ServletRequestContext` in case of Java Servlet containers.
1. Defining a concrete subclass of [ScopeModule](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ScopeModule.html) with `public final ContextScope` fields corresponding to the above `TrackableContext` subclasses, initialized with [newContextScope(name, trackableCtxClass)](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ScopeModule.html#newContextScope(java.lang.String,java.lang.Class)) calls.
1. Hooking creation of the above `TrackableContext` instances into a given existing framework: for example in case of Java Servlet containers, a `Filter` may be created that for each new incoming `ServletRequest` will [execute](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/TrackableContext.html#executeWithinSelf(java.util.concurrent.Callable)) `chain.doFilter(request, response)` within a newly created `ServletRequestContext` instance.
1. Optionally defining subclasses of `InjectionContext` for `Context` types that are _induced_ by some `TrackableContext` subclass. For example entering into a `ServletRequestContext` may induce entering into the corresponding `HttpSessionContext`.
1. Defining `public final InducedContextScope` fields in the `ScopeModule` subclass from the point 2, corresponding to the above induced `Context` types (if any) and initialized with [newInducedContextScope(...)](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ScopeModule.html#newInducedContextScope(java.lang.String,java.lang.Class,pl.morgwai.base.guice.scopes.ContextTracker,java.util.function.Function)) calls.
1. For app-level code development convenience, defining a `public final ContextBinder` field in the `ScopeModule` subclass from the point 2, initialized with a [newContextBinder()](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ScopeModule.html#newContextBinder()) call. This may be useful for app developers when creating their global [ContextTrackingExecutor](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextTrackingExecutor.html) instances bound for injection with `toInstance(myGlobalCtxTrackingExecutor)` calls in their `Module`s: see [USAGE](#usage) section.

App developers should then create an app-wide instance of this `ScopeModule` subclass defined in the point 2, pass its `Scopes` to their other `Module`s (as needed for scoping of their app components, see [PORTABLE MODULES](#developing-portable-modules) section) and finally pass this `ScopeModule` instance to their `Guice.createInjector(...)` call(s) along with their other `Moudle`s.


## USAGE

When switching `Thread`s in a low level library code, static helper methods [getActiveContexts(List&lt;ContextTracker&lt;?&gt;&gt;)](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextTracker.html#getActiveContexts(java.util.List)) and [executeWithinAll(List&lt;TrackableContext&gt;, Runnable)](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/TrackableContext.html#executeWithinAll(java.util.List,java.lang.Runnable)) can be used to manually transfer all active `Context`s:
```java
class MyComponent {

    @Inject List<ContextTracker<?>> allTrackers;

    void methodThatCallsSomeAsyncMethod(/* ... */) {
        // other code here...
        final var activeCtxs = ContextTracker.getActiveContexts(allTrackers);
        someAsyncMethod(
            arg1,
            // ...
            argN,
            (callbackParamIfNeeded) -> TrackableContext.executeWithinAll(
                activeCtxs,
                () -> {
                    // callback code here will run within the same Contexts
                    // as methodThatDispatchesToExecutor(...)
                }
            )
        );
    }
}
```

For higher level abstraction and app-level code, [ContextBinder](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextBinder.html) class was introduced that allows to bind closures defined as common functional interfaces (`Runnable`, `Callable`, `Consumer`, `BiConsumer`, `Function`, `BiFunction`) to `Context`s that were active at the time of a given binding:
```java
class MyComponent {  // compare with the "low-level" version above

    @Inject ContextBinder ctxBinder;

    void methodThatCallsSomeAsyncMethod(/* ... */) {
        // other code here...
        someAsyncMethod(
            arg1,
            // ...
            argN,
            ctxBinder.bindToContext((callbackParamIfNeeded) -> {
                // callback code here will run within the same Contexts
                // as methodThatDispatchesToExecutor(...)
            })
        );
    }
}
```

For app development convenience, [ContextTrackingExecutor](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextTrackingExecutor.html) interface and decorator was provided that uses `ContextBinder`to automatically transfer active `Context`s when executing tasks.
```java
class MyOtherComponent {

    ContextTrackingExecutor executor;

    @Inject void setExecutorAndBinder(ExecutorService executor, ContextBinder ctxBinder) {
        this.executor = ContextTrackingExecutor.of(executor, ctxBinder);
    }

    void methodThatDispatchesToExecutor(/* ... */) {
        // other code here...
        executor.execute(() -> {
            // task code here will run within the same Contexts
            // as methodThatDispatchesToExecutor(...)
        });
    }
}
```


## DERIVED LIBS

[gRPC Guice Scopes](https://github.com/morgwai/grpc-scopes)<br/>
[Servlet and Websocket Guice Scopes](https://github.com/morgwai/servlet-scopes)


## DEVELOPING PORTABLE MODULES

As the official [Guice Servlet Scopes lib](https://github.com/google/guice/wiki/Servlets) stores its `Scope` instances as static vars (`ServletScopes.REQUEST` and `ServletScopes.SESSION`), developers tended to scope their components using these static references directly in their `Module`s or even worse using `@RequestScoped` and `@SessionScoped` annotations. This makes such `Module`s (or even whole components in case of annotations) tightly tied to Java Servlet framework and if there's a need to use them with gRPC or websockets, they must be rewritten.

To avoid this problem, first, scoping annotations should never be used in components that are meant to be portable, so that they are not tied to any given framework. Instead they should be explicitly bound in appropriate `Scope`s in their corresponding `Module`s.<br/>
Second, `Module`s should not use static references to `Scope`s, but instead accept `Scope`s as their constructor params. In case of most technologies, usually 2 types of `Scope`s make sense:
* a short-term one for storing stuff like `EntityManager`s, pooled JDBC `Connection`s or enclosing transactions;
* a long-term one for storing stuff like auth-tokens, credentials, client conversation state (like the immortal shopping cart) etc;

Therefore most `Module`s should have a constructor that accepts such 2 `Scope` references (`public MyModule(Scope shortTermScope, Scope longTermScope) {...}`) and then use these to bind components. This allows to reuse such `Module`s in several environments:
* When developing a Servlet app using the official Guice Servlet Scopes lib, `MyModule` may be created with `new MyModule(ServletScopes.REQUEST, ServletScopes.SESSION)`.
* In case of a websocket client app or a standalone websocket server, it may be created with `new MyModule(websocketModule.containerCallScope, websocketModule.websocketConnectionScope)`.
* For a websocket server app embedded in a Servlet Container it may be either `new MyModule(websocketModule.containerCallScope, websocketModule.websocketConnectionScope)` or `new MyModule(servletModule.containerCallScope, servletModule.httpSessionScope)` depending whether it is desired to share state between websocket `Endpoint`s and `Servlet`s and whether enforcing of `HttpSession` creation for websocket connections is acceptable.
* For a gRPC app, it may be `new MyModule(grpcModule.listenerEventScope, grpcModule.rpcScope)`.
