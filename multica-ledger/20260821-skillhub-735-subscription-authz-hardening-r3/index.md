# 20260821-skillhub-735-subscription-authz-hardening-r3

- Issue：HD-30 `c6feba77-940c-442c-be82-ed51bf7f60c2`（收窄 skillhub#735 为纯订阅授权硬化）
- Base：`e8cab7389f5dc1084fdc1cdf70bafd314d0a3d50`（`iflytek/skillhub@main`，2026-08-21T10:46:19Z，读回于 20260821-113800）
- Base tree：`c81497cf7eb98f0568b8b30c9af1e67797bca27a`
- 风险等级：`HIGH_RISK`（auth/authz、PRIVATE 可见性、event/listener/fan-out、async 事务边界四类同时命中）
- 冻结计划：`planner/20260821-125849-plan-r1.md`（SHA-256 `4465119ef0f150a578893cbf70fa7f4c66c81e0d13696620a8757de3334f35de`），artifact `planner/20260821-125849-plan-gate-r1.json`（`39510ad88cff8cb34f7efc584eb9818a47e6f1cb032c0bf2c11caf3c018d2236`），Plan Gate `PASS`（leader 独立复跑，20260821-133911），人工批准 20260821 by dongsjoa
- 冻结 inventory：`qa/behavior-inventory.v1.json`（SHA-256 `efd911ad203add676cf925c8e20e1cb40251415d1a90600b2ad3de2f48131eae`，`D2`，15 candidates，produced_by_run `8e9589ec-f00d-4f62-9942-ce12f43146de`）
- 当前状态：VERIFYING（exact tree `922195602c92f0f420e17df9021ae39727c95f8d`，业务 commit `046c04ed`，其后仅 ledger commit）

## 前序工作面（不在本分支，未被改写）

- HD-28 工作分支 `20260819-skillhub-731-confirm-publish-event-r2`，head `d0a10cb5e042dfe99e80467ada21fe1e37503965`（origin，读回于 20260821-113700）
- 其账本 `multica-ledger/20260819-skillhub-731-confirm-publish-event-r2/` 完整保留在该分支上，本分支不复制、不改写、不继承其 verdict。
- 上游维护者已裁定 HD-28 的任务面作废，故本任务从 live upstream base 重新起面，而非 rebase 该分支的 66 个 commit。

## 分录

| 时间 | Agent | 动作 | 产物 | tree | 结论 |
|---|---|---|---|---|---|
| 20260821-113951 | leader | intake + dispatch discovery | leader/20260821-113951-intake-dispatch.md | `c81497c` | — |
| 20260821-123244 | qa | behavior discovery | qa/20260821-123244-behavior-discovery.md | `c81497c` | FROZEN |
| 20260821-125849 | planner | plan r1 | planner/20260821-125849-plan-r1.md | `c81497c` | FROZEN |
| 20260821-133911 | leader | plan gate + 人工批准 + dispatch code/tdd | leader/20260821-133911-plan-gate.md | `c81497c` | PASS |
| 20260821-155700 | coder | RED→GREEN | coder/20260821-155700-code-tdd.md | `9221956` | — |
| 20260821-160557 | leader | 候选核对 + 冻结 + 并行派 QA/Review Pass 1 | leader/20260821-160557-candidate-intake.md | `9221956` | ACCEPTED_FOR_VERIFICATION |
