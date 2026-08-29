# Bounded context: `identity`

Who is acting, and what they may reach: actors, and the per-domain grants that are the
only source of authority in Modus.

Package layout follows `doc:20-ddd-practices#ports-and-adapters` §5.1: `published`
(`ActorId`, `DomainId`, `GrantId`, `Capability`, `ActorKind`), `event`, `aggregate`,
`port`, and the context root (`AccessDecision`, `PermissionResolver`).

An aggregate depends only on `published` and `event`; `AccessDecision` and
`PermissionResolver` depend on the aggregates. That direction is what keeps
`rule:archunit/thereAreNoPackageCycles` satisfied inside the context.

`identity` imports no other context (`doc:10-architecture#bounded-contexts`).
