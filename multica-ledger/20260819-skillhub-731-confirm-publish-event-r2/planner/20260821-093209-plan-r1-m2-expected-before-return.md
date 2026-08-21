---
ts: 20260821-093209
agent: planner
action: HD-28 plan r1 M2 expected-before correction / inventory contract conflict
tree: e8aca2a43467dad0da969c182632a7b791f90621
verdict: RETURN
---

# HD-28 plan-r1 M2：两条基线预期裁定与 Gate RETURN

本分录回应 `leader/20260821-092115-implementation-gate-and-plan-gap.md`，修正并 supersede
`planner/20260821-083820-plan-r1-m1.md` 对应机器 artifact 中的两条 `expected_before`。Implementation
Gate 已冻结 exact subject tree `e8aca2a43467dad0da969c182632a7b791f90621`；本次只处理契约证据
口径，不修改候选业务树、不重开已批准方案。

## 裁定：两条均为 `PASS → PASS`

| probe_id | 修正 | 基线依据 |
|---|---|---|
| `search-private-confirm-rebuild` | `FAIL → PASS` | artifact 的 frozen subject 是 `23658e0f`。该 tree 已存在 `SearchIndexEventListenerTest#skillPublishedEventShouldTriggerSkillRebuild` 与 `PostgresSearchRebuildServiceTest#rebuildBySkill_shouldUseLatestVersionMetadataWhenPublishedVersionAdvances`；`23658e0f → bfcb4fe5` 的 `skillhub-search` diff 为空。人类计划第 165 行也明确写为“旧/新均 PASS”。 |
| `owner-nonowner-denied-control` | `FAIL → PASS` | `23658e0f` 已存在 `NotificationEventListenerTest#onSkillPublished_shouldSkipWhenPublisherIsNotSkillOwner`，生产路径按 `publisherId != skill.ownerId` 返回；本轮只在同一测试文件纯追加 F2 用例，未修改 owner notification 路径。 |

不能用 HD-7 之前的历史 tree 维持 `FAIL`：本计划、inventory 和 artifact 均把旧行为基线冻结为
`23658e0f`。即使另取更早 tree，两个现有 entry 也不是 HD-7 `confirmPublish` producer 可达性的
端到端探针：search listener 单测在 HD-7 之前已能直接调用 listener 并 PASS；owner non-owner deny
同样是更早已存在的控制行为。用不同基线不会把这两个具体命令变成有效 RED。

真正要求 frozen subject RED 的仍只有四条：`f1-bundle-en-denial`、`f1-bundle-zh-denial`、
`published-null-namespace-warn`、`yanked-null-namespace-warn`。

## 产物最小性

新 artifact 为 `planner/20260821-093209-plan-gate-m2-return.json`。它从
`planner/20260821-083820-plan-gate-m1.json` 复制，只更新 artifact revision/path/digest/run
provenance，并把上述两个 `expected_before` 从 `FAIL` 改为 `PASS`；归一化后其余字段逐字一致。
inventory 未修改，SHA-256 仍为
`4682fdd44d7ab56df887e403b4ab2ce8547ff8c8a1bb79385ab6be97f2deb5ec`。

## Gate 结果：RETURN，根因在 frozen inventory

对修正 artifact 运行 `run_gate.sh --kind plan` 返回：

```text
result=RETURN
errors=["changed paths missing old-behavior RED probes: ['published-owner-control', 'published-search-control']"]
```

validator 按 path 汇总 inventory 的 `must_fail_old`：

- `published-owner-notification.must_fail_old = true`
- `published-search-index-rebuild.must_fail_old = true`

因此两个枚举修正为语义正确的 `PASS` 后，validator 强制要求这两个 path 仍各有一条 RED。问题已
不只是 plan artifact 的两个枚举，而是冻结 inventory 的 `must_fail_old` 与其 own subject
`23658e0f`、现有探针事实互相矛盾。Planner 不生产、不修改 inventory；也不能把其它 PASS 控制组
伪报成 FAIL 来绕过 Gate。故本次新 artifact 诚实保留为 RETURN，不能交 QA Charter 绑定。

## 交接

- 状态：两个 `expected_before` 已裁定为 PASS；Plan Gate 因 frozen inventory 契约冲突 RETURN
- subject：commit `bfcb4fe5b9314aa41f3ffe71045d8fb043c6482c` / tree
  `e8aca2a43467dad0da969c182632a7b791f90621`，无业务返修
- 产物：本分录与 `planner/20260821-093209-plan-gate-m2-return.json`
- 下一步：Leader 将该 `EVIDENCE_GAP` 路由回非 Planner discovery 责任面，裁定/修正两项
  `must_fail_old` 或提供能在 `23658e0f` 真实 RED 的 path-level 探针；随后 Planner 再冻结并过 Gate
- 未解决：QA Independent Charter 继续暂缓；Review Pass 1 可继续，因为候选树未动
- 需要决策：inventory 的两项 `must_fail_old` 如何与 frozen subject `23658e0f` 收敛
