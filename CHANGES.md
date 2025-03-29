# Summaries of visible changes between releases

### 12.0
- `InjectionContext`: `removeScopedObject(key)` now returns `boolean`, `produceIfAbsent(...)` is now package-private.
- `ScopeModule`: add `newInducedContextScope(...)` variant accepting a base `Scope` instead of a `Tracker`.
- `ContextScope`: more descriptive `toString()`.
- Mark some methods as `final`.
