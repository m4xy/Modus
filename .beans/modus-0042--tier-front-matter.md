---
# modus-0042
title: Mark tier in document front-matter and check it
status: todo
type: task
priority: normal
order: AGT
created_at: 2026-08-29T00:00:00Z
parent: modus-0040
blocked_by: [modus-0041]
---

# Mark tier in document front-matter and check it

`adr:0006-framework-boundary#the-test` is decidable but nothing applies it. A document
written tomorrow carries no tier and is misfiled by default, which is the failure the ADR
exists to prevent and currently cannot.

Success criteria, each observed rejecting a planted violation
(`doc:00-constitution#observed-failing`):

- Every file under `documentation/` carries `tier: 1 | 2 | 3` in front-matter, and `docs-lint`
  fails when one does not — the same shape as its existing front-matter checks.
- A tier-1 document may not reference a tier-3 one. A tenant-facing document that cites this
  domain's process is the pollution `adr:0006` is about, and it is the one direction that can
  be checked from the reference graph alone.
- The reverse is unrestricted and stated as such: tier 3 citing tier 1 is a domain reading its
  framework, which is the normal case.
- Blocked on `bean:0041` because two documents currently span tiers, so the check cannot pass
  until they are split. Building it first would mean adding an exemption list, which is a
  second copy of the classification that drifts.
