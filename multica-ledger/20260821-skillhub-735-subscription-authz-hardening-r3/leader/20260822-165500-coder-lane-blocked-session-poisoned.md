# leader / 20260822-165500 / Coder lane 阻塞：会话毒化 + RED 产物场外滞留

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`0c1796fe-cdc8-407e-bb4a-68f7b920d24b`（2026-08-22T16:50:35Z，dongsjoa 巡检）
- Stage：`PLAN_APPROVED_R2` 不变；Coder lane 置 **BLOCKED**
- 分录性质：阻塞登记 + 机械核验，**不含任何 RED 受理结论**

## 一、失败已确定性复现，Coder lane 不再重派

`multica issue runs` 读回（leader 独立读，非转述），Coder `fd2d1252` 四次终态全 failed：

| Run | started → completed | 报错 |
|---|---|---|
| `1afdebdd` | 14:56:10 → 15:36:50 | `[400] invalid_encrypted_content`，item `rs_0ff13fcb…` |
| `1003b3c8` | 15:47:55 → 16:29:31 | `stream disconnected before completion` |
| `af4e0eeb` | 16:29:33 → 16:30:02 | 同上（29s） |
| `1f148717` | 16:30:36 → 16:31:07 | 同上（31s） |

巡检侧读 OmniRoute 容器日志给出的真实上游报文为 `[400] … encrypted content … could not be
decrypted or parsed`（item `rs_0ed0803b…`），daemon 的 `agent_error.provider_network` 归因**错误**：
codex CLI 把 400 当可重试，5 次 `Reconnecting` 全撞同一堵墙，耗尽后向 daemon 报「stream
disconnected」。根因定位在 session `01a02a28-51f3-7940-8462-84a4b45b7ccc` 的 rollout 第 762 行一条
`payload.type=reasoning` / `id=rs_0ed0803b…` 的 item，每次 `resume_session=true` 都被重放。

**leader 判定**：这是确定性故障不是抖动，重派同一 session 只会产生第 5 次同样的失败。Coder lane
在换 session 之前**不再重派**。该判定属机械判定（同一输入必然复现同一 400），不是风险偏好。

注：15:47 那轮重派是巡检依据「无活跃 Run、不存在并发双跑」作出的，判断在当时成立；leader 本轮
不追溯该决定，只登记其结果已被后三次失败证伪。leader 自身在 15:47:52 的 Run 为 `cancelled`，
故 `85deec71` 那条重派指令未经 leader 处理，本分录一并补记。

## 二、RED 产物的 leader 侧独立核验：**不可达，故不进入 intake**

| 项 | leader 实读 |
|---|---|
| fork 分支 `…-r3` head | `a17eab1d3fd7d6788cfd32be8da623248cbb82f5`（仍是 leader 20260822-152000 的账本 commit） |
| `git ls-remote origin` 全 ref 搜 `ddb9560b` / `9f3e5858` | **零命中**，两个 RED commit 不在 fork 任何 ref 上 |
| `git cat-file -e` 本容器 | 两个 object 均 ABSENT |
| 本容器可见 `/workspaces` | 仅 `HD-28` / `HD-30` / 本 workspace；Coder 的 `1003b3c8` workdir 不可见 |

**结论：RED intake 的前置条件当前不可能满足。** `RED_BASE_SHA` / `RED_DELTA_SHA` 的
branch-reachable 与两次 `git merge-base --is-ancestor` exit 0 必须在 leader 可读的 ref 上实跑；
宿主机 `~/Documents/multica-rescue/HD-30-20260822/` 的 bundle/patch 对 leader 不可读，**不构成
branch-reachable，也不构成受理形式**。`green_dispatch_gate` 保持关闭。

抢救动作本身 leader 记录为**证据保全**，与「RED 合格」是两件事，不因抢救而获得任何受理效力。

## 三、可在场外先行核的一项：未跟踪新增 **未越写集**（初判，非受理）

巡检报的脏树含 3 处未跟踪新增：`notification/`、`auth/rbac/`、`VisibilityCheckerLegacyCompatibilityTest.java`。

leader 逐条 `git cat-file -e` 扫已批准 artifact（`61d54ad3…`）的 24 条 write_set 对 base
（`server` subtree `7d525002`），**恰有且仅有 3 条在 base 上不存在**：

| # | write_set path | base |
|---|---|---|
| 13 | `server/skillhub-domain/src/test/java/com/iflytek/skillhub/domain/skill/VisibilityCheckerLegacyCompatibilityTest.java` | NEW |
| 14 | `server/skillhub-auth/src/test/java/com/iflytek/skillhub/auth/rbac/RbacServiceTest.java` | NEW |
| 22 | `server/skillhub-app/src/test/java/com/iflytek/skillhub/notification/sse/RecordingSseEmitterTestConfiguration.java` | NEW |

三处未跟踪新增与这三条 NEW path **一一对应**（`notification/` ↔ #22 所在新目录、`auth/rbac/` ↔ #14
所在新目录、第三条同名）。故**无 prima facie 写集越界**，Planner 不需要因此被回退。

限定：该结论只到目录/文件名层级。叶子路径是否逐字落在 #13/#14/#22 上，须等产物进入 leader 可读
ref 后复核。#21 `SubscriberNotificationRuntimeIntegrationTest.java` 在 base 上 EXISTS，属修改而非新增，
与「3 个 test 文件 modified」自洽。

## 四、未受理、未假定的事项（明确列出，防后续 run 误当已决）

- 两个 RED commit 是否按 addendum `3812652e` 指定的 probe 落的 —— **未验**。
- `ddb9560b` 单独是否构成完整 RED —— **未验**。抢救物是「2 commit + 脏树 + 未跟踪」，RED delta 若
  有部分只存在于脏树，则 commit 本身不满足 branch-reachable 要求，需重做提交而非直接受理。
- 三份 RED 运行日志的红/绿语义 —— **未验**。日志本身在上一轮已明确不得单独顶数。
- 该轮 Coder 是否遵守了 15:21 新固定的两项硬约束（断言槽位 `entityType="SKILL"`、fixture 不得 seed
  disabled preference row）—— **未验**。时序上 `1003b3c8` 由携带该两项约束的 `85deec71` 触发、
  commit 落在 16:02–16:03，故**有可能**已按新口径，但不得据此推定。

## 五、leader 的路由与请求

阻塞归类为 **runtime/infra**，非 `PLAN_GAP` / `IMPLEMENTATION_DEFECT` / `TEST_GAP`，故不路由给
Planner / QA / Reviewer，工程链在 Coder lane 处停等。Stage 不回退，已批准结论不动。

换 session 的方式已由巡检上抛老板，leader 不重复升级、不代决、不自行删除毒化 rollout、不重启容器。

leader 提请把「恢复产物到分支」与「换 session 重派」**拆成两件独立的事**，理由是机械的：

1. 抢救 bundle 的两个 commit 以 `a17eab1d` 为父，推回分支是 **fast-forward，不需要 force-push**，
   因而不触及需要老板裁决的不可逆动作面；
2. 该动作不依赖被毒化的 session，也不依赖任何 Coder Run；
3. 推回后产物从「单机单副本 + workdir 已 GC eligible」变为分支上的持久副本，GC 风险消失；
4. 更重要的是，推回后 leader 才能实跑 RED intake，从而回答一个决定性问题：**重派 Coder 到底需不
   需要重做 RED**。这个答案会直接缩小老板要裁决的范围——若 RED 合格，新 session 从 GREEN 起步；
   若不合格，新 session 重做 RED。在产物不可读之前，这个问题任何人都只能猜。

推回 **不等于受理**：intake 仍照旧跑，结论仍由 leader 在收到可读 ref 后另出分录。

## 六、边界（不因阻塞放宽）

不 push 公开分支、不碰 PR #735（仍 `OPEN / Draft / CONFLICTING`，head `e071afb4`）、不转 Ready、
不 force-push、不新建取代性 PR。加绑条件 2 的唯一受理证据、Plan Gate artifact `61d54ad3…`、
R1–R5 与 A/B 边界全部不变。
