---
ts: 20260821-100125
agent: leader
action: discovery gate re-run on superseding inventory
tree: e45ca651d13bfb7af345f964dc055dbc615481c6
verdict: PASS
---

对 验证 的 superseding inventory 复跑 discovery gate。**不采信自述**，逐项独立取证。
结论 **PASS**，`EVIDENCE_GAP` 闭合。

## 1. 受检对象

| 项 | 值 |
|---|---|
| 产物 | `qa/20260821-094709-behavior-inventory-superseding.json` |
| sha256 | `7644ce1ef36bf95c4849b309f589f545b82ae2580144ee0ef43a1a0d3fc27820` |
| schema | `behavior-inventory.v1`，对 `schemas/behavior-inventory.schema.json` 校验 **通过** |
| 被取代 | `qa/20260821-073605-behavior-inventory.json` @ `4682fdd44d7ab56df887e403b4ab2ce8547ff8c8a1bb79385ab6be97f2deb5ec` |

## 2. append-only

`git log --oneline -- qa/20260821-073605-behavior-inventory.json` **只有一条**提交
（`e3cc0fda` freeze），自冻结以来零改写；其 sha256 与 `20260821-074243` gate 收据逐位一致。
新产物为独立文件、独立 digest。**append-only 成立。**

## 3. provenance

| 字段 | 值 | 判定 |
|---|---|---|
| `produced_by_run_id` | `f7cf1b2d-b27d-4e8d-86f0-df62975c02a2` | ≠ 已列全部 Plan Run（`d83deada…` / `3b3dfa72…` / `6168a879-47b7-43fe-a40e-c8c5b4f9529f`）✓ |
| `produced_by_context_id` | `01a0233a-aea5-75a1-b194-715bdcf26ddd` | ≠ Plan context `01a02347-46f7-7962-81ec-4118401867e2` ✓ |
| `maturity` | `D1` | 据实，未冒充 D2 ✓ |
| `coverage_note` | 存在，述明本轮为「相对 declared subject 的重推」 | ✓ |
| 生产方 | 验证（非 Planner，非 Coder） | ✓ |

`validate_gate.py:81,86` 的 provenance 隔离要求满足。QA execution 尚未发生，故「discovery 与
execution 不同 Run」的约束此刻无冲突，留待 QA 阶段复验。

## 4. 最小性（逐字段比对，非抽样）

8 个 candidate **一个不增、一个不减、一个不改名**。逐 candidate 全字段比对，
`must_fail_old` 之外的字段**全部按位相同**：

| candidate | 变更字段 | must_fail_old |
|---|---|---|
| `subscription-create-authz-and-response` | 无 | true（不变） |
| `retained-subscription-row-readback` | 无 | false（不变） |
| `published-owner-notification` | 仅 `must_fail_old` | true → **false** |
| `published-subscriber-fanout` | 仅 `must_fail_old` | true → **false** |
| `published-namespace-missing-diagnostic` | 无 | true（不变） |
| `yanked-subscriber-fanout` | 仅 `must_fail_old` | true → **false** |
| `yanked-namespace-missing-diagnostic` | 无 | true（不变） |
| `published-search-index-rebuild` | 仅 `must_fail_old` | true → **false** |

`subject` 块**一字未动**（`23658e0f` / `d35a58c9`）。即 验证 选择的语义裁定是
**主体不动、字段收敛**，而非改主体——我上一轮给出的升级口子未被触发，符合其职责边界。
除 `generator` 块（新 Run 自指、mode、coverage_note）外无其他改动。**最小性成立。**

## 5. 常设检查项复验（`20260821-094207` 立的那条）

> 凡「旧行为失败」类断言，其取证必须显式相对 artifact 自己声明的 subject，
> 且该 candidate 的模块或 symbol 相对该 subject 必须存在 diff。

相对 declared subject 的真实差分：

```
git diff --name-only 23658e0f bfcb4fe5 -- ':(exclude)multica-ledger'
  server/.../listener/NotificationEventListener.java
  server/.../resources/messages.properties
  server/.../resources/messages_zh.properties
  server/.../test/.../SubscriptionMessageBundleTest.java
  server/.../test/.../NotificationEventListenerTest.java
  → 5 files changed, 111 insertions(+)
```

逐条对表：

| 仍为 true 的 candidate | 落点模块 | 相对 `23658e0f` 有 diff？ |
|---|---|---|
| `subscription-create-authz-and-response` | `messages*.properties` + bundle 测试 | **是** |
| `published-namespace-missing-diagnostic` | `NotificationEventListener.java` | **是** |
| `yanked-namespace-missing-diagnostic` | `NotificationEventListener.java` | **是** |

| 翻为 false 的 candidate | 落点模块 | 相对 `23658e0f` 有 diff？ |
|---|---|---|
| `published-owner-notification` | owner 通知链 | 否 |
| `published-subscriber-fanout` | 扇出链 | 否 |
| `yanked-subscriber-fanout` | 扇出链 | 否 |
| `published-search-index-rebuild` | `skillhub-search` | 否 |

**三条 true 全部有 diff 支撑，四条无 diff 的全部已翻为 false。零例外。**
上一轮的结构性死锁——常设检查项 × `validate_gate.py:201-219` × 零 diff 三者互斥——**已消解**，
且是从 inventory 侧（唯一正确的出口）消解的，不是靠放宽任一规则。

## 6. 判定与级联

**discovery gate：PASS。** 生效 inventory 自即刻起为 `7644ce1e…7820`。

尚未解除的下游阻塞（顺序不可颠倒）：`validate_gate.py:523` 强制
artifact `subject.discovery_inventory_sha256` 等于生效 inventory digest，当前 M2 artifact
`f58ece73…f709` 仍绑旧 digest `4682fdd4…b5ec`，**plan gate 此刻必然 RETURN**。
须由 规划 出 M3 后本席复跑。

M3 一次性须含（**只冻结这一次**）：

1. `subject.discovery_inventory_sha256` → `7644ce1e…7820`；
2. `expected_before` 随新矩阵校准——`published-owner-control` / `published-search-control`
   两条控制组不再被 `must_fail_old` 逼出 `FAIL` 探针，M2 里那两处 `PASS` 现在有了上游支撑；
3. R5 / R2 两条 Pass 1 `PLAN_GAP`（见 `leader/20260821-095600-…`）；
4. R8 所需的 `write_set` 修订提案（须外溢至少一个测试文件，见同分录 §5）；
5. `artifact` 自指四字段（path / revision → 4 / run_id / sha256）。

同一漂移陷阱已付三轮代价，本轮不再分批冻结。

## 7. 一条降级的提示（非阻断）

`bd2ba3b9` 无 `Signed-off-by`。本席上一分录曾把它列为「复跑 discovery gate 前须补」，
**现更正为非阻断**：`-r2` 是携带 ledger 的内部分支，永不直推上游；Publication 侧按
`internal-audit-to-clean-upstream-pr` 从 live upstream base 重建 clean 分支并统一作者与 DCO，
本分支的 sign-off 缺失不会传导到上游 PR 的 DCO check。故记为账本卫生事项，
请后续提交补上，不因此拦 Stage，也不要求回改历史提交。

## 交接

- 状态：discovery gate PASS，`EVIDENCE_GAP` 闭合；Plan 段待 M3；exact tree `e8aca2a4…` 继续冻结
- 产物：本分录（gate 收据）
- 工作分支：`20260819-skillhub-731-confirm-publish-event-r2`
- 下一步：**规划 出 M3**（含上述 5 项）→ 本席复跑 plan gate → 一轮批量 Code → QA Charter/Execution
  → Review Pass 2 → Review Gate
- 未解决：`leader/20260821-095600-…` §8 四项（R4 / R3 / R1 / write_set 修订）待 千乘妍 裁定；
  该裁定可与 M3 并行，但会改变 F3 正文与最终 write_set
- 需要决策：否（本分录纯机械）
