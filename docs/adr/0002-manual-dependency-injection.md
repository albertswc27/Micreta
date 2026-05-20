# ADR-0002: Manual DI instead of Hilt

- Status: Accepted
- Date: 2026-05-19
- Sprint: v0.1.0 (carried forward)

## Context

The dependency graph for Micreta v0.1.0 / v0.2.0 is shallow:
~15 long-lived singletons (TTS, voice, OBD repo, settings, etc.) consumed
by ~10 ViewModels and the foreground service. Hilt would add the KSP
annotation processor, slow incremental builds, and produce no measurable
benefit until the project crosses ~50 collaborators.

## Decision

`AppContainer` constructs every long-lived collaborator with `by lazy {}`.
`MicretaApp.container` is exposed as a singleton; ViewModels reach it via
`MicretaApp.get().container`. ViewModel factories are tiny manual classes.

## Consequences

- **+** Faster cold and incremental builds (no KSP).
- **+** Zero magic — every collaborator is constructible from source by
  reading `AppContainer.kt`.
- **−** ViewModel factories are a small amount of boilerplate; acceptable
  at this scale.
- **−** If the project grows past v0.3+, revisit; swap to Hilt with a
  scripted migration — call sites only reference concrete types via
  `container.x` so the refactor is mechanical.
