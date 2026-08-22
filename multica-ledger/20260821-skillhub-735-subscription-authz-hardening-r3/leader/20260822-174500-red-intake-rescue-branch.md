# leader / 20260822-174500 / RED intake（rescue 分支）：可达性解除，RED **未受理**

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`fc7ef616-ec5f-4e6a-b614-0fd52e207175`（2026-08-22T17:40Z，dongsjoa）
- 对象：`origin/20260822-hd30-red-rescue-session-01a02a28`，head `f90d64ee44ad5ab9c6a3e1fba94bbe92287445bf`
- 结论：**可达性前置条件 MET**；**RED intake NOT ACCEPTED**；`green_dispatch_gate` 保持关闭，但关闭理由**变更**
- 本分录 superseding `leader/20260822-165500` 中「unreachable」这一条关闭理由

## 一、两道 ancestry 检查：leader 实跑，全部 exit 0

```
git merge-base --is-ancestor e8cab738 refs/rescue -> 0
git merge-base --is-ancestor a17eab1d refs/rescue -> 0
git merge-base --is-ancestor 9f3e5858 refs/rescue -> 0
git merge-base --is-ancestor ddb9560b refs/rescue -> 0
```

**但对交付分支 `…-r3` 全部 exit 1**（`9f3e5858` / `ddb9560b` / `f90d64ee` 均 NOT reachable）。
计划 §7.1 要求的是「对 GREEN 可达」，GREEN 长在 `…-r3` 上，取证分支不替代该要求。
巡检未合入 `…-r3` 是正确处置，本分录不要求其合入。

作者面读回：`9f3e5858` / `ddb9560b` 作者仍为 `xiaoyaner-multica-coder[bot]`，未改写；
`f90d64ee` 作者为千乘妍，与其自述一致。

## 二、写集 leaf-path：leader 独立逐字符比对，**越界 0**，转 CLEAN

不采信自述，重新拉 write_set（24 条，出自已批准 artifact `61d54ad3…`）与两个 commit 的
`--name-only` 做 `grep -Fxq` 精确全路径匹配：

- `ddb9560b..f90d64ee` 触碰 6 文件 → 6/6 IN-SET（#18 / #21 / #20 / #22 / #14 / #13）
- `e8cab738..9f3e5858` 触碰 4 文件 → 4/4 IN-SET（#15 / #17 / #20 / #13）

`write_set_precheck` 由 provisional 转 **CLEAN**。rescue commit 无 `multica-ledger/**` 混入（0 文件）。

## 三、RED 不予受理：三条独立的机械理由

### 3.1 `ddb9560b` 是**零内容 merge**：可达 ≠ 树里有

```
tree(a17eab1d) = 4ceee9e0bfe4e564f2d4ea3b33989730dc63e957
tree(ddb9560b) = 4ceee9e0bfe4e564f2d4ea3b33989730dc63e957   ← 逐字节相同
git diff --stat a17eab1d ddb9560b  → 空
```

该 merge 整体取了工作分支一侧，`9f3e5858` 的 RED 内容**一条都没进树**。于是出现一个
必须记录的陷阱形态：**`merge-base --is-ancestor` 三次 exit 0，而 RED delta 在树里不存在。**

**故 RED intake 自本分录起追加一项硬检查：ancestry 通过后必须再验树内容，
即 `git diff a17eab1d <RED_DELTA>` 非空且落在 write_set 内。** 单靠 ancestry 可被零内容 merge 满足。

### 3.2 加绑条件 2 的 probe **不在任何 Coder commit 里**

| commit | 是否含 `SubscriberNotificationRuntimeIntegrationTest.java` | 是否含指定 probe 方法 |
|---|---|---|
| `9f3e5858`（Coder，upstream base 上的真 RED 候选） | **无该文件** | 0 |
| `ddb9560b`（Coder，merge） | 有（继承自工作分支旧版） | **0 次** |
| `f90d64ee`（千乘妍，rescue） | 有 | **有**（:224） |

即：**唯一受理证据只存在于人工署名的 rescue commit 中**，而该 commit message 自标
`NOT reviewed, NOT RED-verified -- evidence preservation only`。

两个后果，任一都足以拒绝受理：
1. 该 commit 的作者同时是本轮 Plan Gate 的批准人。若把它当作 Coder 的 RED 交付受理，
   等于批准人为自己批准的加绑条件亲手供给证据，职责分离破裂；
2. 受理一条自标「未验证、仅证据保全」的 commit 为 RED，与其自身声明矛盾。

内容归属仍是 Coder（脏树系 Coder 未提交产物），故**不是作弊，是提交面错位**。修法是重做提交面，
不是重做工作。

### 3.3 本轮 RED **没有任何实跑输出**存在于可读 ref

`refs/rescue` 全链扫 `red-*.log`：命中 3 个，全部是 **r1 轮** 旧物
（`coder/20260821-141300-red-output.log`、`qa/raw/qa-execution-red-base.log`、
`qa/raw/qa-execution-red-candidate-control.log`），且 `a17eab1d` 与 `f90d64ee` 上 blob 逐字节相同。
本轮三份 RED 日志滞留宿主机，未进任何 commit。

上一轮硬线是「不许用不可 checkout 的 RED 日志顶数」。本轮是它的镜像形态：树可 checkout 了，
**运行输出反而不可 checkout**。「树看起来会红」不等于「红过且留痕」，同样不受理。

**特别记一条防误用**：`coder/20260821-141300-red-output.log` 属 r1 轮、其候选已被 QA RETURN，
后续任何 run 不得把它当作本轮 RED 证据。

## 四、正面结论：抢救物的**内容质量合格**，重启不必从零

以下为 leader 实读 `f90d64ee`，非采信：

- **断言槽位与 15:21 固定的七实参逐项吻合**（:261–262）：
  `assertTuple(row, fixture.submitterId(), NotificationCategory.PROMOTION, "PROMOTION_APPROVED", "SKILL", fixture.sourceSkillId())`
  —— recipient / category / eventType / entityType 四项精确命中。
- **entityId 语义单独验过，不会因错误原因变红**：`PromotionApprovedEvent(Long promotionId, Long skillId, …)`，
  `PromotionService:260–262` 传入的是 `approvedRequest.getSourceSkillId()`，probe 断言
  `fixture.sourceSkillId()` —— 一致。此前担心的 source/target 错位**不成立**。
- **preference 闸门约束被显式遵守**（:232 注释）：`No disabled PROMOTION/IN_APP preference is seeded;
  the production default-open gate is real.`
- **falsification 构造完整**：`saturateConfiguredExecutor()`（:233）→ 断言 SSE 落在 publisher 线程
  （:246，即 `CallerRunsPolicy` 回压证据）→ `independentTransaction` 读回（:248）→
  `rowVisibleBeforeSend()`（:256）。
- **该树对条件 2 具备 RED 能力**：`f90d64ee` 上 `NotificationService.create` 仍为裸 `@Transactional`
  （REQUIRED），生产改动尚未施加。

结论：**缺的是提交面与实跑留痕，不是工作本身。**

## 五、leader 未做与不能做的

- 未执行任何测试：本容器**无 JDK**（`java: command not found`），且 RED 实跑留痕本就属 Coder 交付面，
  不由 leader 代跑。故「是否真红」**未验**，不得由本分录推定。
- 未把 rescue 分支合入 `…-r3`，未删毒化 rollout，未重启容器，未碰 PR #735。

## 六、路由：新 session 重派 Coder

巡检查证的机械依据予以采纳并登记：`agent_task_queue.force_fresh_session`（migration `066`）会
short-circuit `(agent_id, issue_id)` 的 session resume 查找，auto-retry 保持 FALSE ——
这解释了三次 auto-retry 全部撞回 `01a02a28`。**故本轮以全新派单（非 retry / rerun）触发，
预期落到新 session，不重放 `rs_0ed0803b`。**

派 Coder `fd2d1252`，交付面为「提交面重做 + 实跑留痕」，非重写测试：

1. 以 `origin/20260822-hd30-red-rescue-session-01a02a28` 为**只读输入**取回内容，在 `…-r3` 上以
   Coder 署名重做提交；rescue 分支保持不动，作取证 ref；
2. `RED_BASE_SHA` / `RED_DELTA_SHA` 必须对 `…-r3` 可达（两次 `merge-base --is-ancestor` exit 0），
   **且 `git diff <RED_BASE> <RED_DELTA>` 非空并全部落在 write_set 内**（§3.1 新增硬检查）；
3. 实跑 RED 并把原始输出提交到 `coder/20260822-*-red-output.log`（新文件名，不得覆盖或复用 r1 旧日志）；
4. 断言槽位与 preference 闸门两项约束照 `f90d64ee` 现状保持，不得改动；
5. 写集以 `61d54ad3…` 的 24 条为界，越界先回 Planner；
6. 仍不 push 公开分支、不碰 PR #735、不转 Ready、不 force-push。

`green_dispatch_gate` 保持关闭，重开条件更新为：Coder 交回上述 1–3 后由 leader 实跑复核
（ancestry + 树内容 + 日志存在性）。加绑条件 2 的唯一受理证据、Plan Gate artifact `61d54ad3…`、
R1–R5 与 A/B 边界全部不变。
