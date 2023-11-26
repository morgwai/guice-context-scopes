# Guice Context Scopes

Classes for building Guice `Scope`s, that get automatically transferred when dispatching work to other threads.<br/>
<br/>
**latest release: [9.1](https://search.maven.org/artifact/pl.morgwai.base/guice-context-scopes/9.1/jar)**
([javadoc](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/9.1))


## OVERVIEW

Asynchronous servers (such as gRPC or asynchronous `Servlet`s) often need to switch between various threads. This requires extra care to not lose a given current Guice `Scope`: it needs to be preserved as long as we are in the  _context_  of a given request/call/session, regardless of thread switching.<br/>
<br/>
To ease this up, this lib formally introduces a notion of an [InjectionContext](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/InjectionContext.html) that can be tracked using [ContextTracker](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextTracker.html)s when switching between threads. Trackers are in turn used by [ContextScope](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextScope.html)s to obtain a `Context` that is current at a given moment and from which scoped objects will be obtained.<br/>
<br/>
When switching threads, static helper methods [ContextTracker.getActiveContexts(List<ContextTracker<?>>)](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextTracker.html#getActiveContexts(java.util.List)) and [TrackableContext.executeWithinAll(List<TrackableContext>, Runnable)](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/TrackableContext.html#executeWithinAll(java.util.List,java.lang.Runnable)) can be used to manually transfer all active `Context`s:
```java
class MyComponent {

    // deriving libraries should bind List<ContextTracker<?>> appropriately
    @Inject List<ContextTracker<?>> allTrackers;

    void methodThatCallsSomeAsyncMethod(/* ... */) {
        // other code here...
        final var activeCtxs = ContextTracker.getActiveContexts(allTrackers);
        someAsyncMethod(arg1, /* ... */ argN, (callbackParam) ->
            TrackableContext.executeWithinAll(activeCtxs, () -> {
                // callback code here...
            })
        );
    }
}
```
On top of the above, [ContextBoundRunnable](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextBoundRunnable.html) decorator for `Runnable` was introduced: it runs its wrapped `Runnable` task within supplied contexts. This allows to automate `Context` transfer when using `Executor`s:
```java
class MyOtherComponent {

    @Inject List<ContextTracker<?>> allTrackers;

    void methodThatUsesSomeExecutor(/* ... */) {
        Runnable myTask;
        // build myTask here...
        myExecutor.execute(
            new ContextBoundRunnable(
                ContextTracker.getActiveContexts(allTrackers),
                myTask
            )
        );
    }
}
```
Deriving libs should also bind [ContextBinder](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/ContextBinder.html) that can be used to transfer `Context`s **almost** fully automatically when passing callbacks to async functions that use common functional interfaces (`Runnable`, `Callable`, `Consumer`, `BiConsumer`, `Function`, `BiFunction`) as types for their callbacks:
```java
class MyComponent {  // compare with the "manual" version above

    @Inject ContextBinder ctxBinder;

    void methodThatCallsSomeAsyncMethod(/* ... */) {
        // other code here...
        someAsyncMethod(arg1, /* ... */ argN, ctxBinder.bindToContext((callbackParam) -> {
            // callback code here...
        }));
    }
}
```
Deriving libs should provide implementations of `ExecutorService` that fully automate `Context` transfers:
```java
class MyContextTrackingExecutor extends ThreadPoolExecutor {

    ContextBinder ctxBinder;

    @Override public void execute(Runnable task) {
        super.execute(ctxBinder.bindToContext(task));
    }
}
```
See the [package level javadoc](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/package-summary.html) for full code organization guidelines for deriving libs.


## DERIVED LIBS

[gRPC Guice Scopes](https://github.com/morgwai/grpc-scopes)<br/>
[Servlet and Websocket Guice Scopes](https://github.com/morgwai/servlet-scopes)
