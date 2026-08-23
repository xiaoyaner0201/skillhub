---
ts: 20260823-171000
agent: planner
action: HD-30 plan r3 §7.3 readback delta
tree: 3c809b93ff6364f345ccf04e51054be5d91fecb4
verdict: FROZEN
---

# HD-30 plan-r3：仅修正 §7.3 禁止面 readback

本分录以 `planner/20260822-141107-plan-r2.md` 与
`planner/20260822-150358-plan-r2-evidence-addendum.md` 为完整前置计划，只 supersede
plan-r2 §7.3 中把三个 token 合并为一次全业务 diff 扫描的 readback 口径。Plan Run 为
`1e0d8d64-f303-419d-914c-92cd4b0e42df`，context 为
`01a0245b-3485-7d62-8601-0e432a47f693`；二者均异于 Discovery Run/context。

## 1. PLAN_GAP 与裁决

原 §7.3 要求：

```bash
git diff e8cab738 GREEN_SHA -- . ':(exclude)multica-ledger' | \
  rg 'confirmPublish|SkillReviewSubmitService|new SkillPublishedEvent'
```

并要求 `rg` 无输出。该命令把两种不同边界错误地合并：

- `confirmPublish` / `SkillReviewSubmitService` 代表本任务禁止触碰的已合并业务面，任何
  non-ledger production 或 test diff 命中都应阻断；
- `new SkillPublishedEvent` 在 `src/main` 中代表新增/改写 producer，必须阻断；但在已批准
  test write set 中，它是驱动真实 registered listener、最终 row/SSE 与 `InOrder` 断言的
  event fixture。禁止 test 构造会与 §7.1 步骤 5 及 §9 的真实 listener/final sink 要求矛盾。

对冻结 subject commit `3becef33` / tree `3c809b93` 独立读回：全 non-ledger diff 有 8 条
`new SkillPublishedEvent`，全部位于 `src/test`；限定 `server/**/src/main/**` 后为 0。
`confirmPublish|SkillReviewSubmitService` 在全 non-ledger zero-context diff 中为 0。故本次
裁决为：**保留全 diff 的 confirmPublish/SkillReviewSubmitService 禁止面；只把 event
constructor 禁止面收窄到 production `src/main`。**

## 2. Superseding §7.3 readback

plan-r2 §7.3 的三组 Maven 命令、动态审计、changed-path 命令与两次 ancestry 命令原样继承。
仅将最后的合并 `rg` 替换为以下两个 zero-context scan：

```bash
git diff --unified=0 e8cab738 GREEN_SHA -- . ':(exclude)multica-ledger' | \
  rg 'confirmPublish|SkillReviewSubmitService'

git diff --unified=0 e8cab738 GREEN_SHA -- \
  ':(glob)server/**/src/main/**' | \
  rg 'new SkillPublishedEvent'
```

验收口径：

1. 两个 `rg` 都必须无输出并 exit 1；其它错误码不得解释成「无命中」。
2. 第一条扫描覆盖全部 non-ledger diff，任何 production/test 对
   `confirmPublish` 或 `SkillReviewSubmitService` 的改动仍直接违反禁止面。
3. 第二条只允许既有 test write set 用 `new SkillPublishedEvent(...)` 作为真实 listener 的
   输入夹具；任何 `src/main` 命中仍直接违反 event producer 禁止面。
4. `git diff --name-only` 的唯一 owner/write-set 校验与 §9 RETURN 条件保持原样；event record、
   producer path 或其它越界文件仍由 changed-path gate 阻断，不能借本 delta 扩面。
5. 使用 `--unified=0`，避免未修改的相邻 context line 被误判成候选新增行为。

在冻结 subject 上机械试跑：第一条 `rg` 为 0 命中 / exit 1；第二条为 0 命中 / exit 1；
原全业务 diff 对 `new SkillPublishedEvent` 仍为 8 命中，证明修订只消除 test-fixture 假阳性，
没有把 production producer 放行。

## 3. 不变面

§1–§6、§7.1、§7.2 写集/work partition、§8 Council、§9 RETURN 条件，以及 R1–R5、
15 candidate disposition、全部 invariant/path/state/sink/probe、R-P1-02/R-P1-07 A/B 边界均
保持不变。冻结 subject tree 仍为 `3c809b93ff6364f345ccf04e51054be5d91fecb4`；本分录不授权
任何业务或测试代码变化，也不改 QA/Reviewer 对 probe 区分力与系统行为的独立裁决职责。

机器 artifact `planner/20260823-171000-plan-gate-r3.json` 仅更新 revision、本文 digest 与本
Plan Run/context；其 source contract、changed behavior、invariants、coverage closure、41 个
acceptance probes、write set、work partition 与 Council nomination 均继承 r2、无语义改动。

## 交接

- 状态：§7.3 内部冲突已消除，plan-r3 FROZEN。
- 主题：commit `3becef33` / tree `3c809b93`。
- 下一步：调度独立复跑 Plan Gate 并按项目路由取得人工批准；不得把本 Planner local receipt
  当成人工批准。
- 未解决：QA/Review 对 exact tree 的行为与证据裁决仍在各自独立 Run；本 delta 不预判。
- 需要决策：无新增语义决策；仅需 Plan Gate 审批本 readback 修订。
