# Bounded context: `domainmgmt`

Domains themselves: creation, the process a domain imposes on its work, and — once
`bean:0031` lands — which Modules are installed in one and who may see them.

Package layout follows `doc:20-ddd-practices#ports-and-adapters` §5.1: `published`
(`DomainName`, `StateName`, `StateTransition`, `ProcessDefinition`), `event`, `aggregate`,
`port`.

`DomainId` is not declared here. It is shared kernel, beside `DomainEvent`
(`adr:0004-domain-id-shared-kernel`) — this context's events name a domain, and a published
package may not reach into another context's.

That is the whole of what this context imports from outside itself, and it stays that way.
The allowlist of `doc:10-architecture#bounded-contexts` §3.1 also permits `identity`'s
published language, and since `bean:0066` this context consumes `GrantRevoked` — but the
consumer is a use case, `core.application.domainmgmt.usecase.ObserveGrantRevokedUseCase`, so
nothing in this package imports `identity` at all. `bean:0031` adds the module-visibility
reaction to that handler; what exists today is the referential check, and its KDoc says so.

`ProcessDefinition` is published rather than internal because it appears in this context's
events, and because `doc:20-ddd-practices#aggregates` §2.2 passes it into
`WorkItem.transitionTo` — `work` needs the type, not a copy of the data. States are opaque
names, never an enum and never `work`'s `WorkItemState`: `doc:00-constitution#domain-scoping`
forbids hardcoding one process, and this context may not import `work` at all.
