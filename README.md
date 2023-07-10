# Guice Context Scopes

Classes for building Guice scopes, that get automatically transferred when dispatching work to other threads.<br/>
<br/>
**latest release: [6.5](https://search.maven.org/artifact/pl.morgwai.base/guice-context-scopes/6.5/jar)**
([javadoc](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/6.5))


## OVERVIEW

Asynchronous servers (such as gRPC or asynchronous servlets) often need to switch between various threads (for example switch to a thread-pool associated with some slow external resource). This requires extra care to not lose a given current Guice scope: it needs to be preserved as long as we are in the  _context_  of a given request/call/session, regardless of thread switching.<br/>
<br/>
To ease this up, this lib formally introduces a notion of an [InjectionContext](src/main/java/pl/morgwai/base/guice/scopes/InjectionContext.java) that can be tracked using [ContextTrackers](src/main/java/pl/morgwai/base/guice/scopes/ContextTracker.java) when switching between threads. Trackers are in turn used by [ContextScopes](src/main/java/pl/morgwai/base/guice/scopes/ContextScope.java) to obtain the current Context from which scoped objects will be obtained.<br/>
<br/>
To automate the whole process, [ContextTrackingExecutor](src/main/java/pl/morgwai/base/guice/scopes/ContextTrackingExecutor.java) was introduced (backed by a fixed size `ThreadPoolExecutor` by default) that automatically transfers contexts when executing a task.<br/>
<br/>
Hint: in cases when it's not possible to avoid thread switching without the use of `ContextTrackingExecutor` (for example when passing callbacks to some async calls), static helper methods `getActiveContexts(List<ContextTracker<?>>)` and `executeWithinAll(List<TrackableContext>, Runnable)` defined in `ContextTrackingExecutor` can be used to transfer context manually:

```java
class MyClass {

    // deriving libraries usually bind List<ContextTracker<?>> appropriately
    @Inject List<ContextTracker<?>> allTrackers;

    void methodThatCallsSomeAsyncMethod(/* ... */) {
        // other code here...
        final var activeCtxs = ContextTrackingExecutor.getActiveContexts(allTrackers);
        someAsyncMethod(arg1, /* ... */ argN, (callbackParam) ->
            ContextTrackingExecutor.executeWithinAll(activeCtxs, () -> {
                // callback code here...
            })
        );
    }

    void methodThatUsesSomeGenericExecutor(/* ... */) {
        Runnable myTask;
        // other code here...
        final var activeCtxs = ContextTrackingExecutor.getActiveContexts(allTrackers);
        someGenericExecutor.execute(
                ContextTrackingExecutor.executeWithinAll(activeCtxs, myTask));
    }

    // other stuff of MyClass here...
}
```
Deriving libs are strongly encouraged to automatically bind `List<ContextTracker<?>>` to an instance containing all possible trackers, so that users don't need to enlist them manually.


## DERIVED LIBS

[gRPC Guice Scopes](https://github.com/morgwai/grpc-scopes)<br/>
[Servlet and Websocket Guice Scopes](https://github.com/morgwai/servlet-scopes)
