# leader / 20260823-125500 / 纯 API 侧推出 **workdir 轮换规则**（3/3 命中、12/12 反例），据此证明 §四 三个手段中只有第 3 条可行；改以子 Issue 承载派单

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`db096449-45d5-4987-9021-4c94d243dd9c`（2026-08-23T12:4x，dongsjoa 主控会话）
- Stage：`PLAN_APPROVED_R2` 不变；Issue 保持 `in_progress`；Coder lane 第 11 次派单（改换承载体）
- **不含任何 RED 受理结论**；`20260822-174500` 的裁定一字不动；`green_dispatch_gate` 仍 CLOSED

## 一、三次失败复核：属实，且 run 记录侧独立佐证「重放」形态

| task | 起止 | 时长 | `failure_reason` |
|---|---|---|---|
| `673a06bc` | 12:39:14 → 12:40:09 | **55s** | `agent_error.provider_network` |
| `c9583b39` | 12:40:11 → 12:40:43 | **32s** | 同上 |
| `646c3b94` | 12:41:16 → 12:43:37 | **141s** | 同上 |

与 dongsjoa 记的 daemon 侧 resume 时刻（12:39:15 / 12:40:12 / 12:41:17）逐一对应。
按 `20260822-202500` 已登记的时长判据，这三次全部落在**二阶重放**区间（非 698s 那种一阶形态），
与「起手即读到旧坏块」相容。

## 二、本轮的实质发现：**workdir 轮换规则可以纯 API 推出，不需要任何容器访问**

`multica issue runs --output json` 的记录里带 **`work_dir`** 字段。取 `实现`（`fd2d1252`）在本 Issue 上
全部 **17 个 task**，按时间排序后 workdir 的变化是**完全规则**的：

| task | workdir | `failure_reason` | 下一 task 是否换 workdir |
|---|---|---|---|
| `d5de8897` / `2e099f64` | `d5de8897` | completed | 否 |
| **`1afdebdd`** | `d5de8897` | **`api_invalid_request`** | **是 → `1003b3c8`** |
| `1003b3c8` / `af4e0eeb` / `1f148717` | `1003b3c8` | `agent_error.provider_network` ×3 | 否 |
| **`9c0ee793`** | `1003b3c8` | **`api_invalid_request`** | **是 → `78ea9bb5`** |
| **`78ea9bb5`** | `78ea9bb5` | **`api_invalid_request`** | **是 → `cd0f9fa3`** |
| `cd0f9fa3` … `646c3b94`（**9 个**） | `cd0f9fa3` | `agent_error.provider_network` ×9 | **否**（9 连不换） |

**规则：当且仅当前一 task 以 `api_invalid_request` 失败时，下一 task 才换 workdir。**
`agent_error.provider_network` 一次都没有触发过轮换——**3 次命中、12 次反例，无例外。**

这与 dongsjoa §三 的 daemon 侧读法（`api_invalid_request` 判 resume-unsafe 退休 session；
`provider_network` 当抖动保留）**完全一致，但推导路径完全独立**：本节只用了 Multica run 记录，
未读 daemon.log、未进 `multica-implementation` 容器、未查 OmniRoute DB。
且它比日志证据**更强**——轮换在 `work_dir` 字段上直接可见，且带 12 个反例。

另：三次轮换后的 workdir 名恰为 `1003b3c8` / `78ea9bb5` / `cd0f9fa3`，即 `cd0f9fa3` 正是
dongsjoa 定位到毒化 rollout 的那个目录（`/workspaces/…/cd0f9fa3/codex-home/sessions/…/rollout-…-01a02ae6….jsonl`）。
**两条证据链在同一个路径上闭合。**

## 三、据此逐条判定 §四 的三个手段

| 手段 | 判定 |
|---|---|
| 1. `multica issue rerun <id>` | **不可用，两条独立理由**：(i) HD-30 的 `assignee_type = squad`，`rerun` 重新入队的是「当前 agent assignment」，而 squad 路由到 **leader**——目标是我，不是 `实现`；(ii) 即便定向到 `实现`，§二 的 9 连反例证明 `provider_network` 之后新建 task **不会**换 workdir，仍会落回 `cd0f9fa3` |
| 2. `force_fresh_session` 显式置位 | **CLI 面不存在**——`issue` / `runtime` / `daemon` / `rerun --help` 全查过，无此列。且 daemon 写命令在受管 task 内不可用，即便可用也只作用于**我方** runtime，不是 `multica-implementation` |
| 3. 落到新 workdir | **唯一可行，且是我手上唯一的杠杆**——见 §四 |

## 四、执行：以**子 Issue** 承载本次派单

Squad 协议给出的两条派单路径中，`@mention` 会落回既有 task 谱系；
**新建 `--status todo` 且 assignee 为 `实现` 的子 Issue** 则产生新的 Issue id 与新的 task 谱系，
是我能触及的、指向「新 workdir / codex-home 不带旧 rollout」的手段。

**诚实标注证据强度**：§二 的轮换规则是**已验**的（17 个 task 全样本）；
但「新 Issue 必然得到新 workdir」是**推理，未验**——本 task 凭据下 `issue list` 只可见 HD-30 一条，
**拿不到跨 Issue 样本**。故此步是有依据的最优手段，不是已证明的手段。

**并因此给 dongsjoa 一条更省事的验证判据**（替代读容器内 daemon.log）：

> 子 Issue 首个 `实现` run 起来后，`multica issue runs <child-id> --output json` 读 **`work_dir`**。
> 只要它不是 `/workspaces/…/cd0f9fa3/workdir`，毒化链即断。

该判据**双方都能从 API 读到，无需进容器**，且比 `grep resuming session` 更早可得。

**brief 一字不改**——`20260822-194500` §三 原文照搬，仅刷新目标 head。
承载体从「本 Issue 的 @mention」换成「子 Issue」是**派单容器的变更，不是 scope 变更**。
按 Squad 协议禁止双路径，本轮**不再 @ 实现**，避免同一工作被触发两次。

## 五、daemon 归类问题：登记，不行动

同一个上游 400 因外层包装不同落入 `api_invalid_request` 与 `agent_error.provider_network`
两种归类，后者不触发退休 → 无限重放。§二 的表把这条从「日志读法」升级为「run 记录上可复现的规则」，
**证据已备齐可供另开条目**。但它是平台层缺陷、不在 HD-30 交付面内，
且 dongsjoa 未要求我开单——**leader 只登记，不代其开 Issue、不扩权**。

## 六、不变项

`green_dispatch_gate` CLOSED，(a)(b)(c) 一字不改，仍由 leader 在 Coder 交付后实跑；
`red_intake` NOT ACCEPTED 三条理由、`write_set_precheck` CLEAN、addendum `3812652e`、
Plan Gate artifact `61d54ad3…`、inventory `efd911ad…`、R1–R5 与 A/B 硬约束、
`superseded_candidate`（不得绑到 `046c04ed`/tree `92219560`）、base 零漂移、Publication 边界，**全部不动**。
前 9 次 + 本轮 3 次共 12 次失败**均归 runtime/infra**，不计入 Coder、不进返修链。

本轮 leader 写操作：追加本分录、建一个子 Issue、发一条回复评论、更新 metadata。
未 push 公开分支、未碰 PR #735、未建上游 PR、未 force-push、未改任何 agent/网关/daemon 配置、
未动 Gate、未签异构豁免、未 @ 老板。
