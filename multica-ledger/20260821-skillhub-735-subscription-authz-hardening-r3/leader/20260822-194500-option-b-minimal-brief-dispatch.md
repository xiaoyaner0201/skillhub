# leader / 20260822-194500 / 模型名假设证伪，转 option B：最小自足 brief 重派

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`87b7b2a9-9f75-406d-87f8-2edffd64c2f5`（2026-08-22T19:41Z，dongsjoa 巡检）
- Stage：`PLAN_APPROVED_R2` 不变；Coder lane 保持 DISPATCHED，改以 option B 形态派单
- 本分录 supersede `leader/20260822-191500` §三判据表的第一、二行（已命中并消费）
- **不含任何 RED 受理结论**；`20260822-174500` 的裁定一字不动

## 一、判据命中：leader 实读确认，且两条分支殊途同归

| 事实 | leader 读回 |
|---|---|
| `cd0f9fa3` | 19:16:08 → 19:27:46，**698s** failed |
| `7860c434` | 19:27:48 → 19:28:27，39s failed |
| `8df336fb` | 19:29:00 → 19:29:39，39s failed |
| 失败原因 | 错误评论 `7b0f5bd1`（19:29:39）正文：`stream disconnected before completion: stream closed before response.completed` |
| 活跃 run | 仅 leader 本轮 `a09e0021`；Coder lane 确已停摆 |

命中 `191500` §三判据表**第二行**（形态转为 stream-disconnected → 剥离面消除、只剩流截断 → 转 B）。

**附记一条对判据本身的评价**：该表第一、二行导向的动作**相同**（都转 B），故本次转 B 的结论
**不依赖**对两种失败形态的区分，鲁棒。这是判据设计上的运气，不是识别精度的功劳，
后续设判据时应有意保留这一性质。

巡检称「这不是我改 brief，是执行 leader 预登记的分支」——属实，leader 确认该援引准确。
其保留的镜像告诫（转 B 后成功也只记「本轮通过」）与 `191500` §三第三行一致，继续有效。

## 二、option B 的落法：`最小` 必须同时是 `自足`，否则会适得其反

巡检要点 1 写「不要复述 R1–R5 全文……**Coder 可自行读取**」。leader 采纳前半、
**限定后半**，理由是机械的：

`78ea9bb5` 的实际膨胀路径是**该 run 自己的 27 次 `exec_command`**（读 plan-r1/r2、读账本、
逐条核三个 commit 的 tree 与 ancestry），不是派单正文长度。巡检自己也读到最后一批请求
已涨到 **159 msgs**。若把正文缩短、同时要求 Coder「自行读取」，等于把正文 token 换成
**更多轮工具调用**——而增长恰好发生在那里。

故 option B 的正确形态是：**正文短，且短到不需要再读任何东西**。本任务是机械任务
（cherry-pick → commit → 跑测试 → commit 日志），不需要 R1–R5 语义。派单正文因此内联：
输入 ref 与 SHA、目标分支、精确测试命令（取自 `plan-r2.md` §7.3，避免 Coder 打开计划）、
四条禁令，并显式写明**本任务不需要读 plan 与账本**。

### 一项可消除整类读取的简化

`leader/20260822-174500` §二已核定：`e8cab738..9f3e5858` 4/4 IN-SET、
`ddb9560b..f90d64ee` 6/6 IN-SET，越界 0。故有一条充分条件可直接交给 Coder：

> **只要不新增文件、内容取自 `f90d64ee`，就不可能越写集。**

Coder 因此**无需读取那 24 条 write_set，也无需读取 artifact `61d54ad3…`**。写集边界仍然有效，
只是核验责任留在 leader 侧（重开条件 (b) 本就由 leader 实跑）。

## 三、派单内容（option B 形态，交付面与 `174500` §6 六条等价）

内联给 Coder 的全部信息：

- 只读输入：`origin/20260822-hd30-red-rescue-session-01a02a28`，head `f90d64ee`
- 目标分支：`20260821-skillhub-735-subscription-authz-hardening-r3`，head `77a2c53a`
- upstream base：`e8cab738`（已验为目标分支 ancestor）
- 已验知识：`git cherry-pick --no-commit 9f3e5858` 自 `e8cab738` 起无冲突、4 测试文件干净 staged
  —— 直接复用，**但不得复制其侧分支形状**（零内容 merge `ddb9560b` 由此而来）
- 落地形状：RED delta 直接落在 `…-r3`，或并入后实测 `git diff <RED_BASE> <RED_DELTA>` 非空
- RED 实跑命令：`plan-r2.md` §7.3 第一条定向命令，已内联
- 输出：原始输出提交为新文件 `coder/20260822-*-red-output.log`，不复用不覆盖 r1 三份旧日志
- 保持不动：断言槽位与 preference 闸门照 `f90d64ee` 现状
- 禁令四条：不 push 公开分支、不碰 PR #735、不转 Ready、不 force-push

## 四、不变项与失败后的处置

`green_dispatch_gate` 保持 CLOSED，(a)(b)(c) 三条一字不改，仍由 leader 实跑复核。
`174500` 三条不受理理由、`write_set_precheck` CLEAN、加绑条件 2 的唯一受理证据（addendum
`3812652e`）、Plan Gate artifact `61d54ad3…`、R1–R5 与 A/B 硬约束、Publication 边界，全部不动。
前 6 次失败与本轮 3 次失败**均归 runtime/infra**，不计入 Coder 评估、不进返修链。

按巡检预登记：若压缩 brief 后仍连续 stream-disconnected，则 option B 亦证伪——
**leader 届时不再自动重试**，将 Issue 置 `blocked` 并 @ 巡检，由其升级 dongsjoa 裁定
换 lane 或拆任务。leader 不自行换 lane、不改 agent 配置、不拆已冻结的 work_partition。
