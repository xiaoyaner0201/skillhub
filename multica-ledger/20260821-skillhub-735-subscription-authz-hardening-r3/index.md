# 20260821-skillhub-735-subscription-authz-hardening-r3

- Issue：HD-30 `c6feba77-940c-442c-be82-ed51bf7f60c2`（收窄 skillhub#735 为纯订阅授权硬化）
- Base：`e8cab7389f5dc1084fdc1cdf70bafd314d0a3d50`（`iflytek/skillhub@main`，2026-08-21T10:46:19Z，读回于 20260821-113800）
- Base tree：`c81497cf7eb98f0568b8b30c9af1e67797bca27a`
- 风险等级：`HIGH_RISK`（auth/authz、PRIVATE 可见性、event/listener/fan-out、async 事务边界四类同时命中）
- 冻结计划：（待 Planner 产出）
- 冻结 inventory：（待 Discovery 产出）
- 当前状态：PLANNING

## 前序工作面（不在本分支，未被改写）

- HD-28 工作分支 `20260819-skillhub-731-confirm-publish-event-r2`，head `d0a10cb5e042dfe99e80467ada21fe1e37503965`（origin，读回于 20260821-113700）
- 其账本 `multica-ledger/20260819-skillhub-731-confirm-publish-event-r2/` 完整保留在该分支上，本分支不复制、不改写、不继承其 verdict。
- 上游维护者已裁定 HD-28 的任务面作废，故本任务从 live upstream base 重新起面，而非 rebase 该分支的 66 个 commit。

## 分录

| 时间 | Agent | 动作 | 产物 | tree | 结论 |
|---|---|---|---|---|---|
| 20260821-113951 | leader | intake + dispatch discovery | leader/20260821-113951-intake-dispatch.md | `c81497c` | — |
