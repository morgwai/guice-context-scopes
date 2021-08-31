# Guice Context Scopes

Classes for building Guice scopes, that get automatically transferred when dispatching work to other threads.<br/>
<br/>
**latest release: [1.0-alpha5](https://search.maven.org/artifact/pl.morgwai.base/guice-context-scopes/1.0-alpha5/jar)**


## OVERVIEW

Asynchronous servers (such as gRPC or asynchronous servlets) often need to switch between various threads (for example switch to a thread-pool associated with some slow external resource). This requires extra care to not lose a given current Guice scope: it needs to be preserved as long as we are in a  _context_  of a given server-side call/session, regardless of thread switching.<br/>
<br/>
To ease this up, this lib formally introduces a notion of a [ServerSideContext](src/main/java/pl/morgwai/base/guice/scopes/ServerSideContext.java) that can be tracked using [ContextTrackers](src/main/java/pl/morgwai/base/guice/scopes/ContextTracker.java) when switching between threads. Trackers are in turn used by [ContextScopes](src/main/java/pl/morgwai/base/guice/scopes/ContextScope.java) to obtain the current Context from which scopes will obtain/store scoped objects.<br/>
<br/>
To automate the whole process, [ContextTrackingExecutor](src/main/java/pl/morgwai/base/guice/scopes/ContextTrackingExecutor.java) was introduced (extending `ThreadPoolExecutor`) that automatically transfers contexts when executing a task.


## DERIVED LIBS

[gRPC Guice Scopes](https://github.com/morgwai/grpc-scopes)<br/>
[Servlet and Websocket Guice Scopes](https://github.com/morgwai/servlet-scopes)
