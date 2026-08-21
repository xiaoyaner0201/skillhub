---
ts: 20260821-123244
agent: qa
action: behavior discovery
tree: c81497cf7eb98f0568b8b30c9af1e67797bca27a
verdict: FROZEN
---

在 live upstream base `e8cab7389f5dc1084fdc1cdf70bafd314d0a3d50` / tree
`c81497cf7eb98f0568b8b30c9af1e67797bca27a` 上冻结 Plan 前行为面 inventory。本 Run
未读取 Plan 或旧 PASS，也未作 `unsubscribe` / `isSubscribed` 产品语义裁决，未替 Planner
处置 `VisibilityChecker` 差异。

## 发现方法与证据

- 使用 Semgrep `1.132.0` Java AST/typed metavariable 规则扫描四个 production root，共
  510 个 Java 文件，parsed lines 约 100%，9 条规则得到 49 个直接语法/类型 finding。
- 规则：`qa/discovery/semgrep-hd30.yml`；原始 JSON：
  `qa/discovery/semgrep-results.json`。
- inventory 覆盖 R1 的 `subscribe` / `unsubscribe` / `isSubscribed`，R2 的 published 与
  yanked subscriber fanout，四个 `SkillPublishedEvent` producer、一个 yank producer、
  notification persistence + SSE sink，以及 `VisibilityChecker.canAccess` 的全部 8 个
  production 调用点。
- `VisibilityChecker` 现役消费面并非正文列举的四个上界：还发现
  `SkillQueryService` 三个调用点和 `SkillDownloadService` 一个调用点；inventory 按行为
  sink 拆成 8 个 R3 candidate。

## 冻结产物

- Inventory：`multica-ledger/20260821-skillhub-735-subscription-authz-hardening-r3/qa/behavior-inventory.v1.json`
- SHA-256：`efd911ad203add676cf925c8e20e1cb40251415d1a90600b2ad3de2f48131eae`
- Schema：`behavior-inventory.v1`，15 candidates，schema validation PASS。
- Provenance：`produced_by_run_id=8e9589ec-f00d-4f62-9942-ce12f43146de`；
  `produced_by_context_id=01a02421-600c-7c10-96ec-64a52988313e`。
- Generator maturity：`D2`（Semgrep Java AST 跨文件 symbol/call-site 枚举 + 人工 dataflow
  stitching）。

## 盲区

静态 AST 不证明 Spring runtime listener registry、代理/注解执行顺序、事务 phase、executor
拒绝与失败恢复；也不证明 reflection、generated code、外部模块或 runtime conditional
registration。上述范围以两个 `status: UNVERIFIED` candidate 显式保留，Planner 不得据
inventory 缺席推定安全。D2 仅描述这次 direct Java syntax/call-site discovery 的成熟度，
不把 inventory 宣称为 whole-program 完整性上界。

## 交接

- 状态：FROZEN；inventory 可交 Planner 只读消费。
- 产物：上述 inventory、Semgrep 规则和 raw results；subject tree `c81497c`。
- 下一步：调度校验 provenance/digest 后派独立 Plan Run；Planner 对 15 candidates 恰好一次
  `planned` / `excluded` disposition，并保留 UNVERIFIED 的人工建模义务。
- 未解决：Spring 动态注册/事务运行时路径、reflection/generated/external 路径未验证；
  `unsubscribe` / `isSubscribed` 与 yank 差异语义未裁决。
- 需要决策：由 Planner 按 Source Contract 与 repository invariants 做上述语义处置。
