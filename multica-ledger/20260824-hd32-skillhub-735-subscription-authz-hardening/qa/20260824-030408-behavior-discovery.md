---
ts: 20260824-030408
agent: qa
action: behavior discovery
tree: c81497cf7eb98f0568b8b30c9af1e67797bca27a
verdict: FROZEN
---

在 frozen upstream base `e8cab7389f5dc1084fdc1cdf70bafd314d0a3d50` / tree
`c81497cf7eb98f0568b8b30c9af1e67797bca27a` 上冻结 Plan 前行为面 inventory。本 Run
`c794d517-0192-42b3-8837-7415cdb672be` / context
`01a031a7-ebeb-7fd0-a738-f3f29398dd2a` 未读取本 Issue 的 Plan 或旧 PASS，历史 r3 只作为
源码/规则前例，不继承其 inventory 或结论。

## D2 发现证据

- Semgrep `1.132.0` 以 13 条 Java AST/typed metavariable 规则扫描 domain、app、auth、infra、
  notification、search 六个 production root：629 个 Java 文件、122 个 finding、约 99.9%
  parsed。
- 规则：`qa/discovery/semgrep-hd32.yml`；原始 JSON：
  `qa/discovery/semgrep-results.json`（SHA-256 `1ed0f459bdf6cf8e84297d7b3481313ad11f17ef84b936f753273d60edcce8fd`）。
- 直接语法闭包包括 4 个 `SkillPublishedEvent` producer、1 个 yank producer、subscriber
  notification 两个 listener、同 event 的 search-index consumer、subscription repository 的
  create/read/delete/page/fanout 面、8 个 `VisibilityChecker` caller，以及 notification DB + SSE
  最终 sink。authority 规则同时枚举 namespace membership、RBAC 和 account-status 读取面。

## 冻结产物与关键 candidate

- Inventory：`qa/behavior-inventory.v1.json`；14 candidates；schema validation PASS。
- Inventory SHA-256：`aabf1b09bfea56b78c75f950a5bf3b5472e0cc30a5435681b3d199bd1ccfc1b1`。
- Generator maturity：`D2`；命令完整记录于 inventory。
- 陈旧订阅行没有因 migration/prune non-goal 被漏掉：独立 candidate
  `r1-my-subscriptions-stale-row-read` 追到 `/me/subscriptions` 的
  `PageResponse<SkillSummaryResponse>` recipient-visible payload；另有 unsubscribe escape、
  subscription-state read 与 fanout-time stale relationship candidates，均要求 Planner 给出
  planned/excluded 证据。
- 链路闭合到 `NotificationRepository.save`、`SseEmitterManager.push` 和后续
  `NotificationController.list`；payload 明列 skill name/slug/namespace/version 等可见字段。

## UNVERIFIED

静态 AST 不证明 Spring listener registry、代理/事务 phase、no-transaction、executor 饱和、
异常隔离和 delivery ordering；也不证明 reflection、generated/external code 或 runtime
conditional registration。上述边界各自保留为 `UNVERIFIED` candidate。Semgrep 对无关的
`UserProfileController.java:169` 有一处 partial-parse warning，也已如实纳入覆盖说明。

## Java / Maven 工具链

- `java` / `mvn` 不在本容器 `PATH`。
- 可复用持久工具链：Oracle JDK `21.0.12.1` 与 Maven `3.9.13` 位于
  `/workspaces/qa-hd30-tooling-c78f0d9e/`；显式设置 `JAVA_HOME` 后 `mvn -version` 成功。
- `/tmp` 为 noexec，需设置
  `MAVEN_OPTS=-Djansi.tmpdir=/workspaces/HD-32/tooling/jansi -Djansi.force=false`。
- 实际执行 `mvn -pl skillhub-domain -am -DskipTests package` 已进入 reactor
  `skillhub-parent -> skillhub-storage -> skillhub-domain`，依赖下载中途结束前台检查；这证明
  JDK/Maven 能启动本仓库后端构建，但本 Discovery Run **不声称完整测试已通过**。后续 Code/QA
  可用相同显式工具链跑 backend tests。

## Gate bundle

按要求复制到 `/workspaces/HD-32/gates/` 后验证 inventory schema PASS；validator
`validate_gate.py` SHA-256
`a7100d86232e21df4f032d07ecbc7e055f2353020be0858230db131a47ca4892`。Plan Gate 尚无 Plan
artifact，故本 Run 不伪造 Plan receipt。

## 交接

- 状态：FROZEN；inventory 可交 Planner 只读消费。
- 产物：inventory、Semgrep 规则与 raw result；subject tree `c81497c`。
- 下一步：Leader 校验 provenance/digest 后派独立 Plan Run；Planner 对 14 candidates 恰好一次
  planned/excluded disposition。
- 未解决：Spring runtime、reflection/generated/external、partial-parser 边界仍 UNVERIFIED；
  `SubscriptionMetadataAccessPolicy` 在 base 缺席，与 `VisibilityChecker` 的收敛方式待 Plan；
  陈旧订阅行不得只凭 non-goal 排除。
- 需要决策：无，由 Planner 按 Source Contract 与 repository invariants 建模。
