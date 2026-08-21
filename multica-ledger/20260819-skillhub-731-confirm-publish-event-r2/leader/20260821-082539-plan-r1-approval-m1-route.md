---
ts: 20260821-082539
agent: leader
action: plan r1 approval intake / M1 artifact-drift finding / re-freeze route
tree: f84f2acef7eaf26d409893a08eb4161eb2a31f4c
verdict: RETURN
---

Plan Gate 已由 千乘妍（`dongsjoa`）按 Project 路由独立审批：**APPROVED，附 1 项必须落实的修订 M1**。
本分录记录审批事实、M1 内容，以及一项**审批人未点到、由本 Gate 机械发现的后果**：M1 的落地
必然改写已被 Plan Gate 绑定的机器产物 `plan-gate.json`，其 digest 不再等于 receipt 中的
`9408cbde…b67d`。据此把 M1 的产物层落地退回 规划，再由本席复跑 Plan Gate 绑定新 digest，
之后才派 实现。

**这不是「再走一轮 Plan」**。审批人已裁定 `不必再走一轮 Plan`，语义决策到此为止、不再复议；
本轮退回只做产物再冻结与 receipt 再绑定，属记账完整性，不重开方案讨论。

## 1. 审批结论（原样记录，不转述为本席判断）

三项语义问题均获通过：行为面闭合（抽查 3 条手工建模项后接受）、`published-search-control`
作为控制组（附加要求：探针须落在**查询可见性 sink**，不止 rebuild 调用计数）、单一
`work_partition`。附带约束：**不因「已手工建模」放宽 Reviewer 的独立寻源义务**——与
discovery gate 的 D1 warning 同向，继续传递。

审批人另行复核 PR #735（`OPEN` / `isDraft=true` / head `e071afb4…` / base `main`；仅 `DCO`
与 `license/cla` SUCCESS；`pr-tests` 从未运行），确认 Publication 段由主控侧承担，并留下
一条对本链有效的硬约束：**工程链内不得以本地绿冒充 CI 绿，最终 Review Gate 复核**。

## 2. M1（硬约束，缺失即 `PLAN_GAP`）

`plan-r1.md:42` 冻结的英文文案 `Only the skill owner or namespace admin can manage this skill`
被判两处错误：

1. **动词错**：该文案是 `error.skill.lifecycle.noPermission`（`messages.properties:113`）的
   逐字复制，属 manage/lifecycle 语义；而 `subscribe()` 的抛出点是
   `accessPolicy.canAccessCurrent(...)` 的失败分支，属**读可见性**检查。
2. **事实错**：PUBLIC skill 的任意用户、NAMESPACE_ONLY skill 的 namespace 成员均能通过该
   检查并订阅，故「只有 owner 或 namespace admin」是一句**对外可见的假陈述**。

要求改为与 `error.skill.access.denied=Access denied to skill: {0}`（`messages.properties:111`）
同向的读权限语义。建议值：

| 文件 | 建议文案 |
|---|---|
| `messages.properties` | `You do not have access to this skill` |
| `messages_zh.properties` | `你没有权限访问该技能` |

Coder 可在**同一语义内**微调，但**不得退回 manage / owner-admin 措辞**。另两条：撇号必须写
`''`（既有约定见 EN `:91` `:93` `:130`；现文件 `grep -n "'" | grep -v "''"` 零命中，不得由本次
改动引入首例未转义撇号——如微调出 `don't` 必须写 `don''t`）；探针断言值同步更新，但
`isNotEqualTo(key)` 这条「不再回落原始 code」的杀手断言**必须保留**。

其余冻结项一律不动：WARN 模板、write_set、禁写集、8/8 闭合、Council 三 lens、证伪条件。

## 3. 本席复核 M1 依据（不采信转述，逐条读回源码）

| 断言 | 实测 | 结论 |
|---|---|---|
| `messages.properties:113` 即建议中的 lifecycle 文案 | 逐字一致 | 成立 |
| `messages.properties:111` = `error.skill.access.denied=Access denied to skill: {0}` | 逐字一致 | 成立 |
| `messages_zh.properties:111` / `:113` 对应中文 | 逐字一致 | 成立 |
| `''` 转义为既有约定 | EN `:91` `:93` `:130` | 成立 |
| 全文件无未转义 `'` | `grep` 零命中 | 成立 |
| `plan-r1.md:42` 位于 `### Planner 假设（ASSUMPTION）` 且自带「回 Plan」出口 | 是 | 成立 |

M1 的六条事实前提全部独立复核通过，本席无异议。

## 4. 机械发现：M1 溢出到已绑定的机器产物（审批人未点到）

审批人写的是「M1 只改两行文案与**对应断言字面量**」，并假定不触碰 write_set 边界。
write_set 边界确未被触碰；但「断言字面量」的**实际落点有三处，其中两处在被 Gate 绑定的
机器产物内**：

| # | 位置 | 性质 |
|---|---|---|
| 1 | `planner/20260821-075744-plan-r1.md:42` | 计划散文，未被 digest 之外的机制消费 |
| 2 | `planner/20260821-075744-plan-gate.json:925` — `f1-bundle-en-denial.sink_assertions[0]` | **被 Plan Gate receipt 绑定** |
| 3 | `planner/20260821-075744-plan-gate.json:943` — `f1-bundle-zh-denial.sink_assertions[0]` | **被 Plan Gate receipt 绑定** |

后果，逐条陈述：

1. 改动 2、3 后 `plan-gate.json` 的 SHA-256 必然 ≠ `9408cbde…b67d`。该 digest 被写进
   规划 自跑 receipt、本席 `leader/20260821-081557-plan-r1-gate.md` 的 `inputs.artifact`
   与 validator receipt 三处。
2. 若跳过再冻结直接派工，Coder 将对着一份**其 `sink_assertions` 与实现相矛盾**的已 Gate
   契约施工：RED 阶段按旧字面量断言会断言错字符串，`expected_before: FAIL` /
   `expected_after: PASS` 的证据链随之失真。
3. Reviewer 的 Pass 2 `EVIDENCE_CHALLENGE` 以该 artifact 为证伪对象，届时必然报出
   artifact/implementation 漂移，成本从「现在改两行」变成「一轮返修」。

因此不存在「原地改、事后说明」的省事路径——digest 绑定的意义就在于 artifact 即契约。

### 命名分歧（一并交待，避免 Coder 漏改一半）

`plan-r1.md` 用 `P-F1-BUNDLE-EN-ZH`（`:152`）/ `P-F1-DENY-NO-MUTATION`（`:153`）；
`plan-gate.json` 用 `f1-bundle-en-denial` / `f1-bundle-zh-denial` / `f1-authorized-control`。
M1 点名的**单个** `P-F1-BUNDLE-EN-ZH` 对应 JSON 侧**两条**探针，两处字面量都要改。

## 5. 路由

退回 规划，只做最小修订（不重开方案）：

1. `plan-r1.md:42` 的 ASSUMPTION 改为读权限语义的冻结文案；
2. `plan-gate.json:925` / `:943` 两条 `sink_assertions[0]` 同步为新文案，
   `isNotEqualTo(key)` 对应的那条断言原样保留；
3. 其余字段一字不改，使 `coverage_closure` / `council_nomination` / `work_partition`
   的既有校验结论继续有效；
4. 追加 planner 分录声明新 digest，并说明本次仅为审批裁定的产物层落地。

之后本席复跑 `run_gate.sh --kind plan` 绑定新 digest 出新 receipt，再派 实现 进 Code/TDD。
新 Plan Run 的 `run_id`/`context_id` 与 discovery Run `b4535e97-…` / `01a0233a-…` 仍须不同。

## 6. 传递给 实现 的 dispatch 约束（预置，待再冻结后生效）

1. M1 为硬约束，未落实即 `PLAN_GAP`，不得进入 QA。
2. Coder 分录必须显式记录 M1 落实位置：**两个 properties 的行号** + **探针断言 diff**。
3. 断言 diff 须覆盖 `f1-bundle-en-denial` 与 `f1-bundle-zh-denial` **两条**。
4. `error.skill.subscription.noPermission` 须落在既有 `error.skill.*` 分区内，不重排既有行。
5. 撇号一律 `''`。
6. F2 只加 `log.warn`，不改过滤语义；收件人集合逐元素一致由差分探针证明。
7. 日志不得输出 subscriber ID 或任何 PII。

## 交接

- 状态：Plan Gate **已获人类批准**；因 M1 溢出到被绑定 artifact，产物层退回 规划 再冻结
- 产物：本分录；旧 receipt 仍有效但其 `inputs.artifact` 即将被 superseding receipt 取代
- 工作分支：`20260819-skillhub-731-confirm-publish-event-r2` @ `5bcfe73b`
- 下一步：规划 出 M1 修订与新 digest → 本席复跑 Plan Gate → 派 实现 进 Code/TDD
- 未解决：Publication 段（验收 5/6）由主控侧承接；「不得以本地绿冒充 CI 绿」已登记为
  最终 Review Gate 的复核项
- 需要决策：无。语义决策已由审批人闭合，本轮为机械再冻结，不再 @ Member
