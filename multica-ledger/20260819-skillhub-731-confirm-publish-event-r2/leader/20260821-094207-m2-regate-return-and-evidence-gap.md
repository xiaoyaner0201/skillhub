---
ts: 20260821-094207
agent: leader
action: plan gate re-run on M2 artifact / EVIDENCE_GAP on inventory must_fail_old / discovery route
tree: a1b3a91a7074b0294d69afb982cc2a97505b8416
verdict: RETURN
---

对 `planner/20260821-093209-plan-gate-m2-return.json` 的机械复跑：**RETURN**，与 规划 自述一致。
本席独立复跑而非采信自述，结论相同：`PLAN_GAP` 的 artifact 层已正确修正，但 Gate 仍被拦下，
拦截点**不在 规划 可及的层**——缺陷在冻结 inventory 的 `must_fail_old`，属 `EVIDENCE_GAP`，
退回 discovery 责任面（非 Planner）。

## 1. 完整性与 append-only

| # | 检查 | 实测 | 结论 |
|---|---|---|---|
| 1 | 远端 head == 声明 | `4e3ade6cd48ac4b6572b6750f16bae5edeb46528` | PASS |
| 2 | 非 ledger 业务 diff（vs `3e056a5f`） | 空 | PASS |
| 3 | 变更全部落在 `multica-ledger/**` | 2 `A` + `index.md` `M` | PASS |
| 4 | `index.md` 纯表尾追加 | +1 / −0 | PASS |
| 5 | 旧 artifact 未被就地改写 | M1 两份 digest 不变（`d5948e67…0c7e` / `07cc6971…5c15b`） | PASS |
| 6 | plan-r1-m2 散文 digest 与声明一致 | `14da43e6…1983` | PASS |
| 7 | plan-gate-m2 digest 与声明一致 | `f58ece73…f709` | PASS |
| 8 | inventory 未被修改 | 仍 `4682fdd4…b5ec` | PASS |
| 9 | DCO sign-off | 存在，作者一致 | PASS |

候选 exact tree `e8aca2a43467dad0da969c182632a7b791f90621` **未被触碰**，Implementation Gate
的 PASS 与其冻结继续有效。

## 2. 最小性证明（逐块 digest，非目测）

| 块 | 旧(M1) | 新(M2) | 结论 |
|---|---|---|---|
| `acceptance_probes` | `f189cba7e619` | `cd2491f55e46` | **DIFF**（预期） |
| `artifact` | `48bd49417e01` | `615a80f7bd72` | **DIFF**（自指元数据） |
| `changed_behavior` / `council_nomination` / `coverage_closure` / `invariants` / `schema_version` / `source_contract` / `subject` / `work_partition` / `write_set` | — | 同 | SAME |

归一化全文 diff 恰好 6 行、3 组：两个枚举值 + `artifact` 自指四字段
（path / revision 2→3 / run_id / sha256）。**没有第三处实质改动**，特别是 `subject` 与
`coverage_closure` 一字未动，故 `20260821-084606` 的闭包 / Council / partition / write_set
校验结论**按位继承**。

两处枚举翻转命中的正是本席上一分录点名的两条，无误伤：

| probe_id | path_id | 变更 |
|---|---|---|
| `owner-nonowner-denied-control` | `published-owner-control` | `FAIL` → `PASS` |
| `search-private-confirm-rebuild` | `published-search-control` | `FAIL` → `PASS` |

其余 4 条 `expected_before: FAIL` 原样保留（`f1-bundle-en-denial`、`f1-bundle-zh-denial`、
`published-null-namespace-warn`、`yanked-null-namespace-warn`），均落在本轮真实有 diff 的
`skillhub-app` 文件，符合本席新立的常设检查项。

## 3. validator 复跑：RETURN（复现 规划 自述）

`run_gate.sh --kind plan` 独立执行（`/workspaces/HD-28/gates/`），receipt：

- `result`: **RETURN**
- `errors`: `["changed paths missing old-behavior RED probes: ['published-owner-control', 'published-search-control']"]`
- `runs`: PLAN Run `6168a879-47b7-43fe-a40e-c8c5b4f9529f` / context `01a02347-46f7-7962-81ec-4118401867e2`
- `inputs`: artifact `f58ece73…f709`、inventory `4682fdd4…b5ec`
- `validator`: `validate_gate.py` v0.1，sha256 `a7100d86…4892`（与前两轮同一版本）

规划 的自述与本席复跑逐字一致，未见修饰或规避。**规划 在能通过 Gate 与如实报告冲突之间
选了后者，这是正确的**：伪报任一控制组为 `FAIL` 都能骗过 validator，其代价是把一条假的
「旧行为失败」证据钉进契约。此处记明，作为链内行为的正面事实。

## 4. 拦截规则的机械读法

`scripts/validate_gate.py:201-219`：

```
若 path 映射的任一 inventory candidate 有 must_fail_old == true，
则该 path 必须至少有一条 expected_before == "FAIL" 的探针。
```

与本席在 `20260821-092115` 新立的常设检查项并置，得到一个**结构性死锁**：

- 常设检查项：声明 `expected_before: FAIL` 的探针，其模块或 symbol 必须相对 subject 有 diff；
- validator：`must_fail_old: true` 的 path 必须有 `expected_before: FAIL` 的探针；
- 机械事实：这两条 path 相对 subject `23658e0f` **零 diff**。

三者不可同时成立。规划 无论怎么改 artifact 都出不去——**唯一的出口在 inventory 侧**，
而 inventory 不在 Planner 的可写面（Discovery 生产方必须非 Planner，`validate_gate.py:81,86`
以 Run/context 隔离强制）。规划 拒绝越界修改 inventory，判断正确。

## 5. `EVIDENCE_GAP`：inventory 的 `must_fail_old` 与自身 subject 冲突

冻结 inventory `4682fdd4…b5ec` 自述 `subject.base_commit = 23658e0f`，
`base_tree = d35a58c9…`。8 个 candidate 中 6 个标 `must_fail_old: true`。逐条对照：

| candidate | must_fail_old | 映射 path | 该 path 的 FAIL 探针 | 机械事实 |
|---|---|---|---|---|
| `subscription-create-authz-and-response` | true | `subscription-denial-localization` | `f1-bundle-en-denial` / `f1-bundle-zh-denial` | F1 新注册 key，旧树真实 RED — 自洽 |
| `retained-subscription-row-readback` | false | `retained-subscription-disclosure` | — | 自洽 |
| `published-owner-notification` | **true** | `published-owner-control` | **无** | 见下 |
| `published-subscriber-fanout` | true | `published-subscriber-observability` | `published-null-namespace-warn` | 见 §5.2 |
| `published-namespace-missing-diagnostic` | true | `published-subscriber-observability` | `published-null-namespace-warn` | F2 新增 WARN，旧树真实 RED — 自洽 |
| `yanked-subscriber-fanout` | true | `yanked-subscriber-observability` | `yanked-null-namespace-warn` | 见 §5.2 |
| `yanked-namespace-missing-diagnostic` | true | `yanked-subscriber-observability` | `yanked-null-namespace-warn` | 自洽 |
| `published-search-index-rebuild` | **true** | `published-search-control` | **无** | 见下 |

### 5.1 两条被拦下的，`must_fail_old: true` 机械上不可能成立

| candidate | 取证 | 结论 |
|---|---|---|
| `published-owner-notification` | 该 path 的 entry `NotificationEventListenerTest#onSkillPublished_shouldSkipWhenPublisherIsNotSkillOwner` 在 subject `23658e0f` 已存在（`git grep -c` 命中 1）；该测试文件本轮 diff 删除行数为 0；owner notification 分支未被 F1/F2 触碰 | 旧树即 PASS |
| `published-search-index-rebuild` | entry `SearchIndexEventListenerTest#skillPublishedEventShouldTriggerSkillRebuild` 在 `23658e0f` 已存在（命中 1）；`server/skillhub-search` 在 `23658e0f → bfcb4fe5` **diff 为空** | 旧树即 PASS |

更根本的一点：**这两条正是计划把它们定为控制组（behavior must NOT change）的 path**。
`must_fail_old: true` 断言「旧行为必须失败」＝行为已变；控制组断言「行为不得变」。
同一条 candidate 上两个断言互斥。这不是 validator 过严，是 inventory 的事实字段错了。

### 5.2 根因：基线锚点漂移（与上一轮 `PLAN_GAP` 同源）

`confirmPublish` 发出 `SkillPublishedEvent` 是 **HD-7 的修复**，已包含在 subject `23658e0f`。
相对 HD-7 之前的基线 `d2403bb5`，owner notification 与 search rebuild 确实「旧行为失败」；
相对 inventory 自己声明的 subject `23658e0f`，两者早已 PASS。即：**这些字段是对着 `d2403bb5`
写的，未随 subject 收敛到 `23658e0f`。**

这与我在 `20260821-092115` 判的 `PLAN_GAP` 是同一个根因、不同层：契约的 `expected_before`
继承了 inventory 的 `must_fail_old`。修了下游没修上游，所以 Gate 在同一处又拦了一次。

因此本席**不把返修范围限定在被 validator 点名的两条**。`published-subscriber-fanout` 与
`yanked-subscriber-fanout` 的 `must_fail_old: true` 目前是被 F2 的 observability 探针满足的
——WARN 日志确实旧树 RED，但 fanout 行为本身本轮未变。这是 validator 层满足、语义层可疑，
不构成阻塞，但属同一漂移家族，应一并重新判定。**8 项全部相对 declared subject `23658e0f`
重新推导**，而不是点改两个布尔值。

具体取何值属 discovery 的语义判断，**不由本席代答**——本席只给出上述机械事实。若 discovery
认定 `must_fail_old: true` 应当保留，则其推论是 subject/base 声明错了，那是比本项严重得多的
发现，须另行升级，不得就地和稀泥。

### 5.3 本席的漏检自陈（第二次，同一家族）

`leader/20260821-074243-discovery-gate.md` 我校验了 inventory 的 schema、候选完整性、
provenance 隔离与 D1 成熟度，**没有把 `must_fail_old` 与「该 candidate 的 producer/consumer
路径相对 declared subject 是否真有行为差异」做交叉核对**。上一分录我已就 `expected_before`
补过一条常设检查项，但当时只补了下游、没回头补上游——同一个盲点因此拦了两次才被拦住。

现将两条合并为一条更根本的常设检查项，覆盖 Discovery 与 Plan 两道 Gate：

> **凡「旧行为失败 / 旧行为不同」类断言（inventory `must_fail_old: true`、契约
> `expected_before: FAIL`），其取证必须显式相对 artifact 自己声明的 subject，
> 且该 candidate/probe 的模块或 symbol 相对该 subject 必须存在 diff。
> 沿用更早基线而未随 subject 收敛者，一律判缺陷：在 inventory 判 `EVIDENCE_GAP`，
> 在契约判 `PLAN_GAP`。**

## 6. digest 级联（必须提前说清，避免第三次同类返工）

`validate_gate.py:523` 强制 `artifact.subject.discovery_inventory_sha256` == 实际 inventory
digest。故 inventory 一旦再冻结，`4682fdd4…b5ec` 失效，连锁如下：

| 被绑定处 | 影响 |
|---|---|
| `leader/20260821-074243-discovery-gate.md` receipt | 须由 superseding discovery receipt 取代 |
| `leader/20260821-081557-plan-r1-gate.md` | 已被 M1 receipt superseded，无新增影响 |
| `leader/20260821-084606-plan-m1-regate-and-code-dispatch.md` receipt | 其 `inputs.inventory` 被取代 |
| artifact `subject` 块 | 本轮 M1→M2 该块一直 SAME，再冻结后**首次变更**，`subject` 的按位继承到此为止，须重新校验 |

因此顺序不可颠倒，且**中间任何一环不得并行抢跑**：

1. 验证 出 superseding inventory（新 digest），Run/context 与任一 Plan Run 相异
   （已用 Plan Run：`d83deada…` / `3b3dfa72…` / `6168a879-47b7-43fe-a40e-c8c5b4f9529f`，
   共用 context `01a02347-46f7-7962-81ec-4118401867e2`；原 discovery Run
   `b4535e97-7654-47ff-84c0-8c97cadedc18` / context `01a0233a-aea5-75a1-b194-715bdcf26ddd`）；
2. 本席复跑 discovery gate，绑新 inventory digest；
3. 规划 出 M3 artifact：仅更新 `subject.discovery_inventory_sha256` 与 `artifact` 自指，
   并按新 inventory 校准 `expected_before`；其余字段一字不动；
4. 本席复跑 `run_gate.sh --kind plan` 绑新 digest；
5. 之后才派 QA Charter。

## 7. 路由

- **验证**（`EVIDENCE_GAP`，本轮唯一开工项）：按 §5 重新判定 8 项 `must_fail_old`，
  出 superseding inventory 分录。**不得就地改写** `qa/20260821-073605-behavior-inventory.json`
  ——append-only，新产物 + 新 digest，旧产物作为历史事实保留。
- **规划**：**待命**，不重开方案。待新 inventory digest 绑定后出 M3，只动
  `subject.discovery_inventory_sha256` 与 `artifact` 自指及必要的 `expected_before` 校准。
  M2 分录本身不作废，作为 `EVIDENCE_GAP` 的取证保留。
- **实现**：无返修项。候选 exact tree `e8aca2a4…` 冻结不动，Implementation Gate PASS 继续有效。
  本轮全部动作只在 ledger 内，业务树零影响。
- **评审**：Review Pass 1 `BLIND_BEHAVIOR_AUDIT` **继续，不受影响**。Pass 1 按定义不以计划与
  inventory 为锚，`must_fail_old` / `expected_before` 的取值不进入其输入面；且 exact tree 未变。
  D1 warning 与 blind 约束继续有效。
- **验证的 QA Charter**：**继续暂缓**。Charter 绑 artifact digest，而 artifact 必将随 inventory
  再变一次；此刻派发必然绑陈旧契约。这已是同一漂移陷阱第三次出现，不再重复付这个代价。
  注意：本轮 验证 承担的是 **discovery 责任面**，与其后的 QA execution 是不同 Run，
  不得合并——合并即自证。

## 8. 传递给下游的约束（继续有效）

1. D1 warning：inventory 不是行为面完整性上界；本轮更进一步——inventory 的字段本身已被证明
   可含事实错误，Reviewer Pass 1 必须独立寻源，不得以 inventory 或计划为覆盖上界。
2. Pass 1 必须 blind：不读 Coder 分录、不读任何 QA 产物，Run/context 与 Coder 及后续 QA 相异。
3. Council 三 lens 与 `must_answer` 按契约执行，含 `evidence-dissent` 反对席。
4. 不得以本地绿冒充 CI 绿。PR #735 仍 Draft，`pr-tests` 从未在该 head 运行。最终 Review Gate 复核。
5. Coder 自述的 1404 tests / 差分 fixture 双树 SHA-256 `661ad8d6…83e0` 仍为**待 QA 独立复算**项。

## 交接

- 状态：Plan Gate 复跑 **RETURN**（复现，非采信）；`PLAN_GAP` artifact 层已闭合，暴露上游
  `EVIDENCE_GAP`；候选 exact tree 冻结不动，Review Pass 1 并行继续，QA Charter 继续暂缓
- 产物：本分录；receipt 已独立复算，validator hash `a7100d86…4892`
- 工作分支：`20260819-skillhub-731-confirm-publish-event-r2` @ `4e3ade6c`
- 下一步：验证 出 superseding inventory → 本席复跑 discovery gate → 规划 出 M3 → 本席复跑
  plan gate → 派 QA Charter
- 未解决：Publication 段（验收 5/6）由主控侧承接
- 需要决策：无。`EVIDENCE_GAP` 属工程链内返修，按 Project 路由 AI 自驱，不 @ Member
