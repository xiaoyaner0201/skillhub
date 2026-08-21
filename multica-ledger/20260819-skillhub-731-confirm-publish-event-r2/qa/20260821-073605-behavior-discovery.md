---
ts: 20260821-073605
agent: qa
action: BEHAVIOR_DISCOVERY
tree: d35a58c983b1ea3d0fa4830e2c0ca3bd43283673
verdict: FROZEN
---

## Subject

- Commit: `23658e0f94f5d6ac1290978671a75eca5d56ef94`
- Tree: `d35a58c983b1ea3d0fa4830e2c0ca3bd43283673`
- Mode: `BEHAVIOR_DISCOVERY`; no HD-28 Plan was consumed and no production code or tests were changed.
- Artifact: `qa/20260821-073605-behavior-inventory.json`
- Artifact SHA-256: `4682fdd44d7ab56df887e403b4ab2ce8547ff8c8a1bb79385ab6be97f2deb5ec`
- Provenance: Run `b4535e97-7654-47ff-84c0-8c97cadedc18`; context `01a0233a-aea5-75a1-b194-715bdcf26ddd`.

## Discovery result

The D1 inventory freezes eight candidates across the subscription write/403 response, retained stale subscription readback, published owner notification, published subscriber fan-out, both missing-namespace diagnostic branches, yanked subscriber fan-out, and the other `SkillPublishedEvent` consumer that rebuilds the PostgreSQL search index.

The two notification fan-outs close through current account/membership eligibility, preference suppression, notification persistence, SSE transport, and the recipient-visible notification payload. The search consumer closes to `skill_search_document`; its downstream query authorization remains explicitly `UNVERIFIED`. Dynamic registration/reflection, Spring proxy and transaction ordering, executor rejection, database triggers, frontend behavior, and unmatched paths are also not asserted safe.

Schema validation passed for `behavior-inventory.v1` with `maturity=D1`; D1 is a necessary tripwire, not a completeness upper bound. The generic bundle has no discovery-only Gate kind, so no Plan/QA/Review receipt was fabricated before a Plan exists.

## Handoff

- Status: inventory frozen; all candidates remain for Planner disposition and later executable verification.
- Next: Planner must map every candidate exactly once and preserve all `UNVERIFIED` boundaries; blind Reviewer must not treat this D1 inventory as an upper bound.
- Unresolved: real log output, message resolution, final recipient/payload side effects, RED sensitivity, async failure semantics, stale-row readback confidentiality, and search query authorization require later probes/review.
- Decision needed: none in this Run; migration/prune remains Publication/maintainer choice as stated by the Issue.
