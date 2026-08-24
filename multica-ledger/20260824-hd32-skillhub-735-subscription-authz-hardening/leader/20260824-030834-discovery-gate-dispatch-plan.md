---
ts: 20260824-030834
agent: leader
action: Discovery Gate（机械校验）+ dispatch Plan
tree: c81497cf7eb98f0568b8b30c9af1e67797bca27a
verdict: PASS
---

# Discovery Gate：PASS

对象：commit `ada1a8941a94785d0c6fa60708a4e5d5fc9a7b6c`，分支
`20260824-hd32-skillhub-735-subscription-authz-hardening`（origin 已读回）。
subject tree 仍为 base tree `c81497cf7eb98f0568b8b30c9af1e67797bca27a`——Discovery 不改业务代码。

## 校验项（全部由本 Leader 独立复算，非采信 QA 自述）

| # | 检查 | 方法 | 结果 |
|---|---|---|---|
| 1 | 仅 `multica-ledger/**` 变化 | `git diff --name-only 9b857a58 ada1a894 \| grep -v '^multica-ledger/'` → 空 | PASS |
| 2 | inventory 绑定正确 base | `subject.base_commit` = `e8cab738…`，`subject.base_tree` = `c81497cf…` | PASS |
| 3 | inventory digest | 独立 `sha256sum` = `aabf1b09bfea56b78c75f950a5bf3b5472e0cc30a5435681b3d199bd1ccfc1b1`，与 QA 自报一致 | PASS |
| 4 | schema 合法 | `jsonschema` Draft2020-12 校验 `behavior-inventory.schema.json` | PASS |
| 5 | `maturity` ≠ D0 | `D2`（semgrep 1.132.0 typed Java AST + 人工缝合） | PASS，无强制 warning |
| 6 | `generator` 七必填字段 | 齐全 | PASS |
| 7 | provenance ≠ 失败 Run | run `c794d517-0192-42b3-8837-7415cdb672be`、context `01a031a7-ebeb-7fd0-a738-f3f29398dd2a`；均 ≠ 失败 Run `4d8afc6e-02e7-4c51-ac60-9c9e548f3e76` | PASS |
| 8 | `index.md` 写权 | header 零改动，仅表尾追加 qa 一行 | PASS |
| 9 | 陈旧订阅行已列为 candidate | `r1-my-subscriptions-stale-row-read`（Leader 在 dispatch 中明文强制项） | PASS |
| 10 | UNVERIFIED 未默认安全 | 2 个显式 UNVERIFIED candidate | PASS |

**延后到 Plan Gate 的检查**：`produced_by_run_id` / `produced_by_context_id` ≠ **Plan Run**
的比对，此刻 Plan Run 尚不存在，无法执行。上表第 7 行只证明它 ≠ 失败的旧 Discovery Run。
两个值已在此落账，Plan Gate 时由 `run_gate.sh --kind plan` 强制比对。

## 冻结产物

- inventory：`qa/behavior-inventory.v1.json`，14 candidates，sha256 `aabf1b09…`
- 分录：`qa/20260824-030408-behavior-discovery.md`
- 确定性命令与 semgrep 规则：`qa/discovery/semgrep-hd32.yml`、`qa/discovery/semgrep-results.json`

14 个 candidate：

```
r1-subscribe-create                              r2-published-subscriber-fanout
r1-unsubscribe-revocation-escape                 r2-yanked-subscriber-fanout
r1-is-subscribed-read                            r2-published-search-index-consumer
r1-my-subscriptions-stale-row-read      [DISP]   r3-visibility-shared-policy-surface
r3-subscription-metadata-access-policy-contract [DISP]
r3-namespace-membership-authority                r3-rbac-account-authority
r4-notification-persistence-and-transport
unverified-spring-runtime-dispatch      [UNVERIFIED]
unverified-reflection-generated-external-partial-parse [UNVERIFIED]
```

## Gate 之外的一处事实读回（修正 intake §5 的问题表述）

intake §5 把 Issue 的「不改变 `SubscriptionMetadataAccessPolicy` 既有授权语义」与上游
FenjuFu 的 `PRIVATE -> owner only` 并列为「两种读法的张力」。Discovery 把这个 candidate 标为
「base 缺席」后，本 Leader 用 `git ls-tree` 三处读回：

| 树 | `SubscriptionMetadataAccessPolicy.java` |
|---|---|
| base `e8cab738`（upstream/main） | **不存在** |
| PR #735 head `e071afb` | 存在（`server/skillhub-domain/.../social/`） |
| 候选分支 r3 `6a61b74` | **不存在** |

即：该类**由 PR #735 自身引入**，在 main 上没有「既有语义」可言；而被指定为可复用输入的
r3 根本没有这个类（改用 `VisibilityChecker` + `SubscriberAccessResolver`）。

因此 intake §5 的张力不是「Issue 与上游评审对同一段既有代码有分歧」，而是：
**Issue 的这条边界所指的对象，在本次 base 上不存在。** 它只能被理解为「相对 PR #735 head
的既有形态」。这一条与 maintainer 第 3 项（reuse existing `VisibilityChecker` or a shared
access policy instead of introducing a parallel visibility model）是否可同时满足，
是 Planner 的语义判断，**Leader 不裁决**；若 Planner 判定二者不可同时满足，报 `PLAN_GAP`
回本 Leader 走路由，不得自行选一边实现。

## 工具链（解除 intake §7 登记项）

QA 容器回报：`java` / `mvn` 不在 PATH，但存在持久 JDK `21.0.12.1` + Maven `3.9.13`；
设置 `JAVA_HOME` 与 `MAVEN_OPTS=-Djansi.tmpdir=/workspaces/HD-32/tooling/jansi -Djansi.force=false`
后 `mvn -version` 成功，reactor 可启动并进入 `skillhub-domain` 依赖构建。

**边界**：本次 Discovery Run **未**声称后端完整测试通过。"reactor 能启动"不等于
"测试可跑通"，Code/QA 阶段仍须自行证明，不得引用本条当作构建已验证。

## 交接

- 状态：DISCOVERY PASS，inventory FROZEN → PLANNING
- 产物：inventory sha256 `aabf1b09…`，绑 base `e8cab738` / tree `c81497cf…`
- 下一步：Planner 产出 `plan-r1` + plan-gate artifact
- 未解决：`SubscriptionMetadataAccessPolicy` 边界对象在 base 缺席（见上）；
  r3 另改的 `RbacService` / `NotificationService` 是否在四项范围内（intake §4）；
  Publication 路径选择（intake §6）
- 需要决策：无（均按路由在后续阶段处理）
