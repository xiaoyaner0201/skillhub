# leader / 20260822-200500 / 停机受理；但 option B **从未被真正测试**，且我的判据建在不可靠信号上

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`62245023-e71d-40de-a613-5f68b25bd66d`（2026-08-22T20:03Z，dongsjoa 巡检 cron）
- Stage：`PLAN_APPROVED_R2` 不变；Issue 已由巡检置 `blocked`（leader 读回确认）
- 分录性质：停机受理 + **对 leader 自己判据的更正** + 供裁决用的机械事实
- **不含任何 RED 受理结论**；`20260822-174500` 的裁定一字不动

## 一、停机受理，四条全部同意

leader 读回确认：Issue `status = blocked`；活跃 run 仅 leader 本轮 `cf6ee38f`；
错误评论 `708fd106`（19:47:02）正文为 `stream disconnected before completion`。
不再自动重试、9 次失败归 runtime/infra 不计入 Coder、工作面事实未变——四条 leader 均无异议，
不重开、不绕过。停机本身是正确处置。

## 二、但一条事实必须在裁决前更正：**option B 根本没被测到**

leader 读回三次 run 的**时长**：

| run | 起止 | 时长 |
|---|---|---|
| `f3a6cc15`（option B 首发） | 19:44:49 → 19:45:24 | **35s** |
| `8969232a` | 19:45:26 → 19:46:01 | **35s** |
| `99a96c03` | 19:46:34 → 19:47:02 | **28s** |
| 对照 `cd0f9fa3`（option B 之前） | 19:16:08 → 19:27:46 | **698s** |

option B 的假设是「压小 brief → 降低单轮上下文 → 避开膨胀」。**一个 35 秒就死的 run，
不可能积累起任何上下文**——它在 brief 大小能起作用之前就死了。

与巡检自己的网关证据一致：同一个 `rs_02f95f…` reasoning item 被重放 **36 次**全部 400。
即这三次是**继承脏会话后的秒挂**，不是上下文膨胀，也不是流截断。

**故 option B 的状态不是「已证伪」，而是「未获执行」。** 这个区别直接影响待裁决项：
若按「已证伪」理解，缩小 brief 这条路被划掉；按「未测试」理解，它仍是未被排除的候选。

## 三、leader 自己的判据错在哪（本分录记为 leader 侧缺陷）

`20260822-191500` §三我写下：「失败形态转为 `stream disconnected` → 剥离面已消除、只剩流截断
→ 转 B」。该判据**键在 daemon 的错误文本上，而该文本早已被证明不可靠**——
`leader/20260822-165500` §一即已登记：codex CLI 把 400 当可重试，重连耗尽后向 daemon 报
「stream disconnected」，daemon 的归因是错的。本次网关侧同样是 400，daemon 侧同样显示
stream-disconnected。

**即：我用一个不能区分两个假设的信号，去做区分两个假设的判据。** 这是判据设计缺陷，不是执行问题。
巡检严格按我写下的规则执行，援引无误，责任在我。

**更正后的判据形态**（供后续沿用）：不得以 daemon 错误文本为准，改键在两项客观量上——

1. **run 时长**：数十秒级 = 继承脏会话秒挂；数百秒级且死于中途 = 上下文/流的问题；
2. **网关侧 item id**：与既有毒化项相同 = 重放；全新 id = 新故障面。

## 四、供裁决的机械事实（leader 不选方向，只标注前提真伪）

### 4.1 「弃用脏会话」是 A / C 的**前置条件**，不是 A 的风险脚注

巡检把「脏上下文需先弃用当前 run 线程」写在 A 的风险里。按 §二，任何在同一
`(agent, issue)` 上的重试都会重新继承它并在约 35s 死掉，**与选哪个模型无关**。
故它对 A、C 是必须先做的前置动作。

对 B（切 claude）能否也继承，取决于 session resume 是否只键在 `(agent, issue)` 而与 model 无关
——**leader 无法读到该实现，不作判断**。但需提醒：若未先弃用而直接切 B，B 一旦秒挂，
很可能被误记为「claude lane 也不行」，而真因仍是脏会话。**判据同 §三：看时长。**

### 4.2 选项 C 的前提经 leader 核对**不成立**

C 为「codex lane 退回非 `-xhigh` 档」，即退回裸名。但：

- boss 19:1x 的裁定依据正是**裸名已从 OmniRoute 目录移除**，`-xhigh` 改动就是为消除它；
  C 等于把刚被移除的条件装回去；
- C 附带的 ACL 顾虑（「allowedModels 只放行裸 canonical ID，变体切换需同步核对」）**已过时**：
  巡检自己在 19:1x 报过 `allowedModels` 已改为
  `["codex/gpt-5.6-sol","codex/gpt-5.6-sol-xhigh"]`，且 `cd0f9fa3` 确以 `-xhigh` 路由并跑满 698s、
  无 403——巡检本轮也确认「无 403 / Model not allowed」。

故 C 既回退了刚修好的东西，其列出的阻碍又不存在。**leader 不建议方向，只标注：C 的两条前提都不真。**

### 4.3 选项 B 在工程侧无阻碍

`ab_hard_constraint` 约束的是计划内容（R-P1-02 / R-P1-07 保持 A/B、产出不得称「维护者已批准」），
与执行 agent 用哪个模型无关（`leader/20260822-185000` §四已核 metadata 原文）。故若裁为 B，
账本**无需**做 lane 变更归因。另：Coder 已验产出（`f90d64ee` 的测试内容）是
cherry-pick + 跑测试的机械产物，**与模型无关**，切 lane 不损失任何既有工作。

### 4.4 一条独立削弱「上下文假说」的证据

巡检报 19:50:18 / 19:50:38 / 19:50:59 三次 **1 msgs 全新请求**同样失败，19:52:17 才成功一次。
1 条消息不存在上下文膨胀。**该数据点独立支持「codex 上游本身间歇不稳」**，
并说明选项 A（等恢复）的等待时长不可预估。

## 五、leader 的处置

不重派、不改任何 agent 配置、不碰 key、不自行换 lane、不拆已冻结的 `work_partition`；
方向性取舍归 dongsjoa，巡检已升级，**leader 不重复升级、不重复 @ 人类**。

`green_dispatch_gate` 保持 CLOSED（(a)(b)(c) 一字不改），`red_intake` 保持 NOT ACCEPTED，
`write_set_precheck` CLEAN，rescue ref `f90d64ee` 有效，`…-r3` head `e2d24bbf`，
加绑条件 2 的唯一受理证据、Plan Gate artifact `61d54ad3…`、R1–R5 与 A/B 硬约束、
Publication 边界，全部不变。
