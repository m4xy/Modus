package uk.m4xy.modus.core.domain.port

import java.time.Instant

/**
 * The current instant, as a dependency rather than as ambient state.
 *
 * `doc:00-constitution` §1.3 bans `Instant.now()`, `LocalDate.now()` and
 * `System.currentTimeMillis()` from `core-domain`, and `doc:20-ddd-practices` §5.3 names
 * this port as the replacement. Until `bean:0065` there was no replacement to name: the
 * ban was real and the alternative did not exist, so the first aggregate that had to mint
 * a timestamp had nowhere to get one.
 *
 * **This port is for a caller that must *produce* an instant.** An aggregate that is
 * *handed* one keeps taking `at: Instant` as a parameter — `doc:20-ddd-practices` §8's
 * table sanctions both shapes, and `Domain.adoptProcess`, `PermissionGrant.issue`,
 * `PermissionGrant.revoke` and `Actor.register` are all correct as they stand. Injecting a
 * clock into an aggregate that already receives the instant would add a dependency and
 * decide nothing.
 *
 * Context-free by design (`bean:0065` decision 2). Every context needs the time and none
 * owns it, so declaring one of these per context would produce six mutually incompatible
 * types requiring six bindings of what is, by construction, one implementation per process
 * — a fixed clock wired into five contexts and forgotten in the sixth is a defect that
 * shape makes representable and this one does not.
 *
 * It is **not** shared kernel: `rule:archunit/sharedKernelIsLeaf` is scoped by an exact
 * name set, so it cannot see this package at all
 * (`adr:0004-domain-id-shared-kernel#shared-kernel-membership`). The rules that do see it
 * are `rule:archunit/portsAreInterfaces`, `rule:archunit/ambientCapabilityPortsAreLeaf` and,
 * because bytecode cannot see a value class, `rule:archunit/ambientCapabilityPortSourceIsLeaf`.
 */
public interface ClockPort {
    /**
     * The current instant, at whatever resolution the implementation offers.
     *
     * Two calls may return the same value: a caller that needs distinguishable instants
     * needs a sequence, not a clock. Nothing here promises monotonicity — the system clock
     * can go backwards, and a port that promised otherwise would be lying about the
     * adapter behind it.
     */
    public fun now(): Instant
}
