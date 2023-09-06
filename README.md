# Guice Context Scopes

Classes for building Guice `Scope`s, that get automatically transferred when dispatching work to other threads.<br/>
<br/>
**latest release: [8.2](https://search.maven.org/artifact/pl.morgwai.base/guice-context-scopes/8.2/jar)**
([javadoc](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/8.2))


## OVERVIEW

Asynchronous servers (such as gRPC or asynchronous `Servlet`s) often need to switch between various threads. This requires extra care to not lose a given current Guice `Scope`: it needs to be preserved as long as we are in the  _context_  of a given request/call/session, regardless of thread switching.<br/>
<br/>
To ease this up, this lib formally introduces a notion of an [InjectionContext](src/main/java/pl/morgwai/base/guice/scopes/TrackableContext.java) that can be tracked using [ContextTrackers](src/main/java/pl/morgwai/base/guice/scopes/ContextTracker.java) when switching between threads. Trackers are in turn used by [ContextScopes](src/main/java/pl/morgwai/base/guice/scopes/ContextScope.java) to obtain the current `Context` from which scoped objects will be obtained.<br/>
<br/>
When switching threads, static helper methods `ContextTracker.getActiveContexts(List<ContextTracker<?>>)` and `TrackableContext.executeWithinAll(List<TrackableContext>, Runnable)` can be used to manually transfer all active `Context`s:
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
Additionally [ContextBoundRunnable](src/main/java/pl/morgwai/base/guice/scopes/ContextBoundRunnable.java) `Runnable` decorator that runs its wrapped task within supplied contexts, was introduced to automate `Context`s transfer when using `Executor`s:
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
Deriving libs usually provide implementations of `ExecutorService` that fully automate `Context`s transfer:
```java
class MyContextTrackingExecutor extends ThreadPoolExecutor {

    List<ContextTracker<?>> allTrackers;

    @Override public void execute(Runnable task) {
        super.execute(new ContextBoundRunnable(
                ContextTracker.getActiveContexts(allTrackers), myTask));
    }
}
```
Deriving libs should also bind [ContextBinder](src/main/java/pl/morgwai/base/guice/scopes/ContextBinder.java) that can be used to transfer `Context`s **almost** fully automatically when passing callbacks to async functions that use common functional interfaces (`Runnable`, `Callable`, `Consumer`, `BiConsumer`, `Function`, `BiFunction`) as types for their callbacks:
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


## DERIVED LIBS

[gRPC Guice Scopes](https://github.com/morgwai/grpc-scopes)<br/>
[Servlet and Websocket Guice Scopes](https://github.com/morgwai/servlet-scopes)
