# Bounded context: `execution`

Runs of an agent against a work item: triggers, supervision, lifecycle and the output stream produced.

This package is currently a placeholder. The domain model is owned by a later
work item; only the package boundary is established here so that the
architecture tests have something real to enforce.

Rules that already apply to this package:

- No framework types. Kotlin stdlib only.
- No dependency on `core-application`, any `adapter-*` or any `module-*`.
- No dependency on another bounded context's internals without an explicit,
  reviewed reason (cross-context references are caught by the cycle check).
