---
# modus-0019
title: Authentication and grant administration
status: todo
type: feature
priority: high
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0018]
---

# Authentication and grant administration

Why: `bean:0009` modelled who may do what and deliberately shipped no credentials, no
sessions and no way to issue or revoke a grant. Until an actor can authenticate, every
route `bean:0018` adds is either open or unusable.

Success criteria:

- An authenticated request resolves to an `ActorId`; an unauthenticated one resolves to
  none and is denied down the same `PermissionResolver` path as any empty grant set, so
  absence and denial stay one input (`bean:0009` criterion 4).
- Issue, narrow and revoke exactly as `doc:10-architecture#domain-root-convention` §5.5
  names them, each a write on one named domain. No wildcard grant, no role type, no global
  administrator. The §5.5 bootstrap path is safe only because registering an `Actor`
  carries no authority.
- Grant administration is itself domain-scoped: a domain the caller cannot see returns 404.
  Credentials are never a domain concern (`doc:20-ddd-practices#domain-prohibitions`).
- Every denial path observed failing on a planted bypass
  (`doc:00-constitution#observed-failing`).

Blocks `bean:0022`.
