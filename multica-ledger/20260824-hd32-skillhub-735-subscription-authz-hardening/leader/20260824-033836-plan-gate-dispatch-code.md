---
ts: 20260824-033836
agent: leader
action: Plan Gate（机械校验）+ dispatch Code/TDD
tree: c81497cf7eb98f0568b8b30c9af1e67797bca27a
verdict: PASS
---

# Plan Gate：PASS

对象：commit `4bff2ca4199ff34add5780081b8b773eb4f9582f`。subject tree 仍为 base tree
`c81497cf7eb98f0568b8b30c9af1e67797bca27a`——Plan 阶段不动业务代码。

## A. 结构与完整性（Leader 独立复算）

| # | 检查 | 结果 |
|---|---|---|
| 1 | 仅 `multica-ledger/**` 变化 | PASS |
| 2 | plan-r1.md sha256 = `1c2b92a6…` | PASS |
| 3 | plan-gate-r1.json sha256 = `bdfab21f…` | PASS |
| 4 | `index.md` header 零改动，仅追加 planner 一行 | PASS |
| 5 | `run_gate.sh --kind plan` 独立运行 | **PASS**，`warnings: []`、`errors: []` |
| 6 | validator 身份 | `0.1` / `a7100d86232e…`（与 intake 记录一致） |

## B. Provenance（Discovery Gate 延后项，此刻执行）

| 项 | Discovery | Plan | 结论 |
|---|---|---|---|
| run_id | `c794d517-0192-42b3-8837-7415cdb672be` | `50739374-3ed1-4be6-8c59-ade3d98da490` | 互异 PASS |
| context_id | `01a031a7-ebeb-7fd0-a738-f3f29398dd2a` | `plan-context-50739374-3ed1-4be6-8c59-ade3d98da490` | 互异 PASS |

Discovery Gate 分录中标记为「延后到 Plan Gate」的自证检查，至此闭合。

**观察（不构成 RETURN）**：Plan 的 `context_id` 是由 run_id 派生的合成串，而 Discovery 侧是
真实 context UUID。本次两者 run_id 确已互异，反自证目的达成，schema 也只要求非空字符串，
故判 PASS。但合成 context 削弱了信号——若某次 Plan 真的与 Discovery 共用 context，派生串
仍会「看起来不同」。后续 Run 请填真实 context 标识。

## C. 闭包（`coverage_closure`）

- 声明的 candidate 集合与 inventory 的 14 个 id **完全相同**（非子集）；
- `planned` = 14，`excluded` = `[]`，`unresolved` = `[]`；
- 13 个 `planned_path_ids`。

两个 `CANDIDATE_REQUIRES_PLAN_DISPOSITION` 均落入 planned，未被 non-goal 排除：
`r1-my-subscriptions-stale-row-read`（current-read 过滤 items 与 total，不删行不改计数）、
`r3-subscription-metadata-access-policy-contract`（不带回平行 class）。
两个 `UNVERIFIED` 亦落入 planned（`spring-runtime-dispatch`、`dynamic-surface-audit`），
未默认安全。

## D. Council 提名（Leader 只做机械校验，不替换人选）

| 校验项 | 结果 |
|---|---|
| 2–3 位 | 3 位 PASS |
| 锚点互异 | Schneier / Kleppmann / Beck，三种检查偏好不重叠 PASS |
| ≥1 位 `dissent_role: true` | `stale-disclosure-dissent` PASS |
| dissent 必须回答「本方案在什么条件下是错的」 | Q1 逐字为此问 PASS |
| `must_answer` 3–5 条 | 4 / 4 / 5 PASS |
| `rationale` 引用具体 path/invariant 而非风险标签复述 | PASS，见下 |

rationale 实际引用到的 id：

- Schneier：`my-subscriptions-current-read`、`subscription-policy-disposition`、
  `STALE_ROW_METADATA_CONFIDENTIALITY`、`PRIVATE_OWNER_MANAGER_BOUNDARY`
- Kleppmann：`spring-runtime-dispatch`、`notification-final-sinks`、
  `published-search-index-consumer`
- Beck：`subscription-create`、`my-subscriptions-current-read`

无一条是「因为涉及权限」式标签复述。Linus Torvalds-style 已明确 dec‍lined 并给理由，
符合「不需要某视角就写明，不凑名额」。

## E. `work_partition`

单一 unit `subscription-authz-hardening`：`path_ids` 与 13 个 planned path **完全相同**，
`depends_on: []`，`write_set` 28 项，`no_split_rationale` 存在。单元并集覆盖全部 planned path，
无写集相交/成环/重复覆盖可言（只有一个单元）。符合「写集高度耦合就出单一单元并说明理由」。

## F. 验收探针

41 条，全部带 `command` 与 `sink_assertions`，13 个 planned path 全部至少一条覆盖。

- class 分布：`NEGATIVE` 16、`REVOCATION` 7、`FAILURE` 9、`COMPATIBILITY` 5、`STALE_STATE` 4
- `expected_before`：`FAIL` 21、`NOT_APPLICABLE` 12、`PASS` 8；`expected_after` 全部 `PASS`
- 即 **21 条为要求 RED 的探针**，Code 阶段必须在 base 上真实按预期失败

可证伪条件存在于 `plan-r1.md` §10 `Falsification conditions` 与 §ASSUMPTION（可证伪）。

### 观察（不构成 RETURN，转 QA 章程）

probe class 中**没有独立的 POSITIVE 类**。合法路径的验收以「allowed control」形式嵌在
41 条中的 14 条断言里（如「zero row, counter and event **while PRIVATE manager control is
allowed**」），是可执行的，故判 PASS。

但要指出：本次是**收紧**授权，其首要回归风险恰是**过度拒绝**——合法用户被误挡。
把合法路径只作为拒绝探针的对照项，对该风险的敏感度弱于独立正向探针。
本条转给 QA：Independent Charter 应把「合法访问仍然可用」列为一等探针，
而不是沿用 Plan 的嵌入式对照。这是 QA 独立章程的职责，不是 Plan 的缺陷。

## G. 抽样复核 Planner 的事实断言

Planner 称「PRIVATE `owner || manager` 固定在既有 `VisibilityChecker` 的 full-context
overload，旧 overload 与 **8 个既有 consumer** 做差分兼容验证」。Leader 在 base 上读回：

- `VisibilityChecker` 在 base 存在，恰有两个 overload：3 参（`skill, userId, nsRoles`）与
  4 参（多 `Set<String> platformRoles`）——「full-context overload」指向明确；
- `visibilityChecker.canAccess(...)` 在 main 源码中的直接调用点恰为 **8 处**，分布在 6 个文件：
  `CompatSkillLookupService:55`、`SecurityAuditController:110`、`SkillLabelAppService:170`、
  `SkillDownloadService:285`、`SkillQueryService:209/278/823`、`SkillTagService:55`。

数字与分布均属实，非估算。

## 结论

Plan Gate **PASS**。Plan 与 artifact 冻结，进入 Code/TDD。
`plan-r1` 为冻结计划，Coder 不得自行修改；发现与源码/需求/不变量冲突时报 `PLAN_GAP` 回 Leader。

## 交接

- 状态：PLAN PASS，FROZEN → IMPLEMENTING
- 产物：`planner/20260824-040000-plan-r1.md`（`1c2b92a6…`）、
  `planner/20260824-040000-plan-gate-r1.json`（`bdfab21f…`）
- 下一步：实现（Coder）按单一 unit `subscription-authz-hardening` 落地，21 条 RED 先失败
- 未解决：POSITIVE 探针独立性（已转 QA 章程，见 §F）；
  Publication 路径选择（intake §6，READY 时提出）
- 需要决策：无
