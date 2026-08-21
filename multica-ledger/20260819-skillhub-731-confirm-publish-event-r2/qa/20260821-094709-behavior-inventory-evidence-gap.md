---
ts: 20260821-094709
agent: qa
action: EVIDENCE_GAP inventory supersession
tree: e8aca2a43467dad0da969c182632a7b791f90621
verdict: FROZEN
---

本分录修正但不改写 `qa/20260821-073605-behavior-inventory.json`：旧 inventory 的
`must_fail_old` 混用了 HD-7 之前的 `d2403bb5` 基线与本 artifact 声明的 subject
`23658e0f94f5d6ac1290978671a75eca5d56ef94`。superseding artifact：
`qa/20260821-094709-behavior-inventory-superseding.json`，SHA-256
`7644ce1ef36bf95c4849b309f589f545b82ae2580144ee0ef43a1a0d3fc27820`。

## 重新推导口径

本 inventory 中 `must_fail_old: true` 的含义严格限定为：HD-28 的候选
`bfcb4fe5b9314aa41f3ffe71045d8fb043c6482c` 引入了该 candidate 的行为变化，因此存在一条
对该 candidate 自身行为的探针，在 declared subject `23658e0f` 上 FAIL、在候选上 PASS。
不能继承更早 HD-7 基线，也不能用同一 consumer 内另一个 candidate 的新 sink 代替。

| candidate | 新值 | declared subject 相对候选的事实 |
|---|---:|---|
| `subscription-create-authz-and-response` | `true` | F1 增加 EN/zh bundle key，使旧树裸 code、候选本地化文案；该 candidate 的用户可见响应 sink 真实变化 |
| `retained-subscription-row-readback` | `false` | repository/readback 路径零 diff；保持控制面与 maintainer migration 选择题 |
| `published-owner-notification` | `false` | owner listener 与 sink 零 diff；HD-7 已在 subject 内使事件可达 |
| `published-subscriber-fanout` | `false` | recipient eligibility、dispatcher、持久化、SSE 与 payload fan-out 语义零 diff；F2 只新增独立诊断 sink |
| `published-namespace-missing-diagnostic` | `true` | F2 新增 WARN；旧树无日志，候选有含 skillId/namespaceId 且无 subscriber PII 的诊断 sink |
| `yanked-subscriber-fanout` | `false` | recipient eligibility、dispatcher、持久化、SSE 与 payload fan-out 语义零 diff；F2 只新增独立诊断 sink |
| `yanked-namespace-missing-diagnostic` | `true` | F2 新增 WARN；旧树无日志，候选有诊断 sink |
| `published-search-index-rebuild` | `false` | `skillhub-search` 整个模块零 diff；HD-7 已在 subject 内使 confirmPublish 事件可达 |

结论为 `true / false / false / false / true / false / true / false`。subject/base 声明无需更改；
真正 changed 的三项恰好对应 F1 与 F2 的两个独立 WARN candidate。所有 candidate 的
`verification_status` 仍为 `UNVERIFIED`，D1 仍不是完整性上界。

## Provenance 与证据

- Run：`f7cf1b2d-b27d-4e8d-86f0-df62975c02a2`
- Context：`01a0233a-aea5-75a1-b194-715bdcf26ddd`
- schema：`behavior-inventory.v1` PASS；8 个 candidate 唯一且矩阵逐项断言通过
- exact candidate tree：`e8aca2a43467dad0da969c182632a7b791f90621`
- `23658e0f..bfcb4fe5` 非 ledger changed paths 恰好为 listener、两个 bundle 与两份对应测试；
  `SkillSubscriptionService`、retained readback、`skillhub-search` 均 `git diff --quiet` exit 0

## 交接

- 状态：EVIDENCE_GAP 已以 superseding inventory 冻结；旧 inventory 保留为历史，不再作为生效闭包输入
- 下一步：Leader 独立复跑 discovery gate 并绑定新 digest；之后 Planner 才可出 M3
- 未解决：后续 QA Charter / Execution 仍须独立 Run，动态注册、事务/executor、真实 recipient/payload 与 transport 证据仍待验证
- 需要决策：无
