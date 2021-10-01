# Guice Context Scopes

Classes for building Guice scopes, that get automatically transferred when dispatching work to other threads.<br/>
<br/>
**latest release: [2.1](https://search.maven.org/artifact/pl.morgwai.base/guice-context-scopes/2.1/jar)**
([javadoc](https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/2.1))


## OVERVIEW

Asynchronous servers (such as gRPC or asynchronous servlets) often need to switch between various threads (for example switch to a thread-pool associated with some slow external resource). This requires extra care to not lose a given current Guice scope: it needs to be preserved as long as we are in a  _context_  of a given server-side call/session, regardless of thread switching.<br/>
<br/>
To ease this up, this lib formally introduces a notion of a [ServerSideContext](src/main/java/pl/morgwai/base/guice/scopes/ServerSideContext.java) that can be tracked using [ContextTrackers](src/main/java/pl/morgwai/base/guice/scopes/ContextTracker.java) when switching between threads. Trackers are in turn used by [ContextScopes](src/main/java/pl/morgwai/base/guice/scopes/ContextScope.java) to obtain the current Context from which scopes will obtain/store scoped objects.<br/>
<br/>
To automate the whole process, [ContextTrackingExecutor](src/main/java/pl/morgwai/base/guice/scopes/ContextTrackingExecutor.java) was introduced (backed by a fixed size `ThreadPoolExecutor` by default) that automatically transfers contexts when executing a task.<br/>
<br/>
Hint: in cases when it's not possible to avoid thread switching without the use of `ContextTrackingExecutor` (for example when passing callbacks to some async calls), static helper methods `getActiveContexts(ContextTracker...)` and `executeWithinAll(List<ServerSideContext>, Runnable)` defined in `ContextTrackingExecutor` can be used to transfer context manually:

```java
class MyClass {

    @Inject ContextTracker<ContextT1> tracker1;
    @Inject ContextTracker<ContextT2> tracker2;

    void myMethod(Object param) {
        // myMethod code
        var activeCtxList = ContextTrackingExecutor.getActiveContexts(tracker1, tracker2);
        someAsyncMethod(param, (callbackParam) ->
            ContextTrackingExecutor.executeWithinAll(activeCtxList, () -> {
                // callback code
            }
        ));
    }
}
```
Deriving libs are strongly encouraged to automatically bind `ContextTracker<?>[]` to an instance containing all possible trackers, so that users don't need to enlist them manually.


## DERIVED LIBS

[gRPC Guice Scopes](https://github.com/morgwai/grpc-scopes)<br/>
[Servlet and Websocket Guice Scopes](https://github.com/morgwai/servlet-scopes)
