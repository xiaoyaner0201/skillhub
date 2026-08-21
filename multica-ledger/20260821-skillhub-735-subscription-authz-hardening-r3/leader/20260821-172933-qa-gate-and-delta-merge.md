---
ts: 20260821-172933
agent: leader
action: QA Gate 独立复跑 + Charter/Pass1/QA 三份产物核对 + delta 归并 + 返修路由
tree: 922195602c92f0f420e17df9021ae39727c95f8d
verdict: RETURN
---

本分录一次性登记三件被推迟的事：blind QA Charter 冻结核对（20260821-1620）、Review Pass 1
收讫核对（20260821-1633）、QA_EXECUTION 的 Gate 独立复跑与两条独立结论的 delta 归并。
`RETURN` 是编排裁决（本轮候选不进入 Implementation Gate），**不是**对实现正确性的语义结论。

## 1. 机械读回（三次推送均为纯 ledger）

| 推送 | head | server 子树 | 非 ledger diff vs `046c04ed` |
|---|---|---|---|
| qa charter | `e36fd8d3` | `7d525002` | 空 |
| reviewer pass 1 | `bcca2e1a` | `7d525002` | 空 |
| qa execution | `fef5846c` | `7d525002` | 空 |

exact tree `922195602c92f0f420e17df9021ae39727c95f8d` 自 `046c04ed` 起未被扰动。
`fef5846c` 是 merge commit（parents `bfe19a69` + `bcca2e1a`），两侧均为 ledger commit。
三次 `index.md` 改动均为表尾追加，头部未被改写。

## 2. QA Gate 独立复跑（不采信 QA receipt）

在 `/workspaces/HD-30/gates/` 用本容器 validator 重跑 `--kind qa`：

- 结果 `PASS`，`warnings=[]`，`errors=[]`
- validator `a7100d86232e21df4f032d07ecbc7e055f2353020be0858230db131a47ca4892`（与 Plan Gate 同一份）
- 与 QA 的 `qa/qa-gate-receipt-r1.json` 逐字段一致；差异仅在 `inputs[].path` 的绝对路径前缀
  （两容器工作目录不同），按文件名归一化后 `==` 为 True

| 绑定项 | 值 | 核对 |
|---|---|---|
| QA artifact | `1f16d21a5432b18bc89eb143b731a5a072f032a2809dd803e9c260310c668cd2` | 重算一致 |
| inventory | `efd911ad203add676cf925c8e20e1cb40251415d1a90600b2ad3de2f48131eae` | 与 Discovery 冻结值同一份 |
| plan artifact | `39510ad88cff8cb34f7efc584eb9818a47e6f1cb032c0bf2c11caf3c018d2236` | 与 Plan Gate 冻结值同一份 |
| subject | commit `046c04ed` / tree `92219560` | 与冻结面一致 |
| Run 分离 | Charter `ca970b26-c4b0-402f-bbe0-9af5d825b6f0` / Execution `c78f0d9e-4324-4b2e-a41f-6ecc5607145f` | 互异，且均异于 Discovery `8e9589ec…` |

**receipt `PASS` 与 artifact verdict 是两件事**：receipt 只证明 schema、digest、coverage set、
exact tree 与 Run 分离成立；artifact 本身是 `plan_compliance=UNVERIFIED` /
`system_behavior=UNVERIFIED` / `overall=UNVERIFIED`。我按 artifact verdict 判 Stage，
不因 receipt 绿而推进。

artifact 内独立复算：`probe_results` 28 条 = **25 BLOCKED + 3 PASS**（PASS 三条为
`subscribe-message-en-zh`、`existing-consumer-regression`、`dynamic-surface-delta-audit`）；
`unverified` 8 条 **全部 `blocks_pass=true`**（3 BLOCKER + 5 HIGH）；`findings` 5 条
（1 BLOCKER + 4 HIGH）。任一 blocking `UNVERIFIED` 即禁止 Overall PASS，QA 的收口与该纪律一致。

## 3. Review Pass 1 核对（20260821-1633 已在 Issue 内详述）

`bcca2e1a` 冻结成立。抽查 6 处引用行号全部精确命中。Pass 1 不出四维 verdict、不请
Review Gate 是正确的：`review-gate.v1` 强制 `plan_sha256` / `qa_sha256` / `pass2_challenge`，
Pass 1 依定义不持有。

对 `R-P1-01` 做过一处**定性纠正**（现象成立，两条定性前提不成立）：

- 冻结 Plan `planner/20260821-125849-plan-r1.md:74-76` 已明确裁决该状态，artifact 内对应
  具名 invariant `LATEST_VERSION_RULE_UNCHANGED` 与 state `yanked-latest-null-denied`
  （`expected_effect: DENY`）；
- 「无测试进入该状态」不成立：probe 存在于
  `SubscriberNotificationSinkTest:148 yank_lastPublishedVersion_nonOwnerGetsNoSink()`，
  当前绿。Reviewer 的 grep 字面为真（`LatestVersionId(null)` 在 test 下 0 命中），
  但该状态由 helper 驱动而非字面 setter。

故改判 `PLAN_GAP` → `INVARIANT_VIOLATION`：目的地同为 Planner，问题从「补漏」变为
「对具名 invariant 当场辩护或重建」。Reviewer 的实质论证在知悉计划裁决后依然完整，
原样转交，不削弱。另登记客观事实：相对 base 这是**存量行为破坏**（base 无条件 dispatch），
落在既有的「必须在 PR 正文以选择题向维护者披露」约束内。

我独立复核的行为链（支持现象成立）：`SkillGovernanceService:277` → `findLatestPublishedVersionId`
（`:295` 结尾 `.orElse(null)`）→ `:279` 落库 → `:283` 才发事件 → AFTER_COMMIT 监听器必读到 null
→ `VisibilityChecker:29-31` owner-only。全仓其余两处 `setLatestVersionId(null)`
（`SkillPublishService:585` 同事务 FK 规避后回填、`SkillHardDeleteService:105` 删除通道）
均不产生同样的提交后持久态，yank 是唯一的。

## 4. delta 归并：两条独立结论的收敛与分层

| 面 | Review Pass 1（静态重建） | QA Execution（动态执行） | 归并结论 |
|---|---|---|---|
| Spring runtime sink | `U1` 声明不覆盖，交 QA | `QAC-09` BLOCKER 未实测 | **两侧都没有运行时证据**，非某一方偷懒 |
| executor 拒绝策略 | `U4` 未评估 | `QAC-11` BLOCKER，手写 rejecting Executor 不算 | 收敛 |
| R2 不误伤 | 源码层可证（`:43` 保序去重 / `:64` 保序 filter） | `QA-EX-03` HIGH：resolver 层已证，**listener 最终 sink 层无 InOrder 且 mock 掉 persistence/SSE** | 非矛盾，是层级差；**不得据 Pass 1 认为此项已闭合** |
| generated/reflection/external | `U3` 仅 grep 两路，反射无覆盖 | `QAC-18` HIGH，Semgrep 511 文件 0 parse error 但不闭合动态面 | 收敛为残留盲区，走 PR 披露 |
| RED 真实性 | 未涉 | `QAC-15` HIGH：14 项确为目标缺陷失败，但中间 tree 不可达 | 人工批准第 3 条未闭合 |

**独立新增（我在归并时发现，两方均未点名）**：冻结 Plan 的 probe
`runtime-executor-rejection-zero-sink` 其 sink_assertion 为「deterministically rejecting
`skillhubEventExecutor` produces zero notification rows」，而生产配置
`AsyncConfig.java:32` 是 `CallerRunsPolicy` —— **饱和时任务不被丢弃，而是在提交线程同步执行**。
该 probe 描述的「零 row」是配置下不可能出现的行为，只能靠替换成另一个 executor 才「通过」，
即测一个虚构对象。这不是 Coder 执行不到位，是 **plan 层的 probe 规格与生产配置相互矛盾**，
判 `PLAN_GAP` 转 Planner。对照组：同批的 `runtime-commit-reaches-sink` 与
`runtime-rollback-zero-sink` 规格本身是充分的（明写 real rows / registered listener），
Coder 未做到属执行面，不是规格问题。

## 5. 返修路由（本分录签发）

| ID | 类别 | 去向 |
|---|---|---|
| `R-P1-01` | `INVARIANT_VIOLATION` | Planner：辩护或重建 `LATEST_VERSION_RULE_UNCHANGED` |
| 新增 executor 规格矛盾 | `PLAN_GAP` | Planner：重建该 invariant 与 probe |
| `R-P1-02` `R-P1-07` | 显式 disposition | Planner 起草为维护者选择题，本链不自行定案 |
| `R-P1-03` | `IMPLEMENTATION_DEFECT` | Planner 定 `RbacService` 批量方法口径 → Coder 实现 |
| `R-P1-04` `R-P1-05` | LOW | Coder |
| `R-P1-06` | LOW | 仅登记，构造不出 |
| `QA-EX-01` `QAC-09/10` | BLOCKER/HIGH `TEST_GAP` | Coder 写真实 Spring 集成 probe |
| `QA-EX-03` `QA-EX-04` `QAC-01/02/04/05/06/07/08/12` | HIGH `TEST_GAP` | Coder：listener 层 InOrder、真实 row/SSE、批量失败四变体 |
| `QAC-13` | HIGH | Coder 补 base-vs-candidate differential 输出 |
| `QA-EX-02` `QAC-15` | HIGH `EVIDENCE_GAP` | **Coder 必须把 RED 态落成分支上可达的真实 commit**（先测试-only 失败 commit，再 GREEN commit），使 QA 可 checkout 复跑 |
| `QA-EX-05` `QAC-18` | HIGH `EVIDENCE_GAP` | 残留盲区，PR 正文披露 |

Charter 的 18 组不需要 delta —— 其纪律严于候选实现，本轮 BLOCKED 是实现未达标而非章程有误。

## 6. 时序

Planner → Plan Gate（我独立复跑 + 千乘妍审批，因动到具名 invariant 且扩了 probe 范围）
→ Coder → QA 新 execution Run（Charter 沿用冻结件）→ Reviewer delta 复审 + Pass 2
`EVIDENCE_CHALLENGE` → Implementation Gate。

本轮不派 Pass 2：新一轮必然改动 exact tree（`R-P1-03` 改码 + 整轮补测），Pass 2 结论绑定
tree，现在跑必被作废；且 `EVIDENCE_CHALLENGE` 对最终 QA artifact 才有最大价值。

## 7. 如实登记的完整性折损

Reviewer 的 `bcca2e1a` 与 QA 的 `17a7eee8` 先后落到同一分支，两遍产物此后在共享分支上
物理互相可达。QA_EXECUTION 期间我已书面要求其不读 Review 产物，QA 分录亦未引用 Pass 1
任何 finding id，行为上未见交叉；但**从 Pass 1 推送那一刻起，两遍的隔离依赖的是指令而非
不可达性**。下一轮起：Reviewer 与 QA 的产物在各自 Run 完成前不推共享分支，由我在 Gate
时统一并入。此条不粉饰，写入本分录供后续 Gate 判读。
