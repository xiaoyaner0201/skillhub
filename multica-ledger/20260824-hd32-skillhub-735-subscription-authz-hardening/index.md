# 20260824-hd32-skillhub-735-subscription-authz-hardening

- Issue：HD-32 `f3e0e5ef-0e5f-4006-a4e9-b6fb603a821d` — 收窄并完成 iflytek/skillhub#735 订阅授权硬化
- Base：`e8cab7389f5dc1084fdc1cdf70bafd314d0a3d50`（upstream/main，读回于 2026-08-24T02:39Z）
- Base tree：`c81497cf7eb98f0568b8b30c9af1e67797bca27a`
- 风险分级：**HIGH_RISK**（authz / PRIVATE / event-fanout / 持久化派生记录，四类触发器）
- 冻结 inventory：`qa/behavior-inventory.v1.json` sha256 `aabf1b09bfea56b78c75f950a5bf3b5472e0cc30a5435681b3d199bd1ccfc1b1`（D2，14 candidates）
- Discovery provenance：run `c794d517-0192-42b3-8837-7415cdb672be` / context `01a031a7-ebeb-7fd0-a738-f3f29398dd2a`（Plan Run 必须 ≠ 此二值）
- 冻结计划：（待 Planner 提交）
- 当前状态：PLANNING

## 分录

| 时间 | Agent | 动作 | 产物 | tree | 结论 |
|---|---|---|---|---|---|
| 20260824-024121 | leader | intake+dispatch | leader/20260824-024121-intake-dispatch.md | — | — |
| 20260824-030408 | qa | behavior discovery | qa/20260824-030408-behavior-discovery.md | `c81497cf7eb98f0568b8b30c9af1e67797bca27a` | FROZEN |
| 20260824-030834 | leader | discovery gate + dispatch plan | leader/20260824-030834-discovery-gate-dispatch-plan.md | `c81497cf7eb98f0568b8b30c9af1e67797bca27a` | PASS |
