---
ts: 20260824-080019
agent: planner
action: supersede ASSUME_MESSAGE_COPY observable
tree: —
verdict: FROZEN
---

# Planner 分录 — `ASSUME_MESSAGE_COPY` 可观察量替代

## 1. 替代范围

本分录仅替代冻结 artifact
`planner/20260824-040000-plan-gate-r1.json` 中 contract id
`ASSUME_MESSAGE_COPY` 的陈述。历史 `plan-r1` 与 plan-gate artifact 保持不可变；本分录是后续
实现、QA 与 Review 解释该 assumption 时的 append-only 权威补充。

触发证据为 `leader/20260824-080500-stage1-adjudication-restage.md` §4：原陈述
“The denial code resolves to the frozen English and Chinese copy in plan-r1.” 强于其证据锚点所能
支持的 REQUIREMENT 可观察量。

## 2. Superseding statement

`ASSUME_MESSAGE_COPY` 收敛为：

> 订阅授权拒绝使用精确 message key `error.skill.subscription.noPermission`；English 与 zh locale
> 均通过真实 message source 解析为本地化结果，两者均不回落为裸 key，且 locale 结果彼此可区分。
> plan-r1 不冻结或批准 English / Chinese 的字面文案。

因此，字面串
`You do not have permission to subscribe to this skill` / `你没有权限订阅该技能`
不是本计划的冻结验收值；可复现验收边界是 exact key、真实双 locale 解析、非裸 key 回落与
locale 区分。

## 3. 不变边界

- 不改变其他 source contract、changed behavior、invariant、acceptance probe、coverage closure、
  `write_set`、`work_partition` 或 council nomination。
- 不改变冻结 inventory、base commit/tree、计划 provenance 或 candidate disposition。
- 不修改生产代码、测试代码、冻结计划或冻结 artifact；subject tree 未被本分录改写。
- 本分录不签 Plan Gate、Code、QA 或 Review PASS。
