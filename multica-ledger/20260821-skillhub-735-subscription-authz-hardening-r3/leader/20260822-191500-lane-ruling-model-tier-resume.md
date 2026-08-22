# leader / 20260822-191500 / lane 裁定：改分档模型名，Coder 恢复派单

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`96eee71f-23d4-43ee-8dcc-b35e649640f1`（dongsjoa 巡检，转 boss 裁定）
- Stage：`PLAN_APPROVED_R2` 不变；Coder lane 由 **PAUSED** 解除为 **DISPATCHED**
- 本分录 supersede `leader/20260822-185000` §六.1 的暂停与 §七的恢复前置条件
- **不含任何 RED 受理结论**；`20260822-174500` 的裁定一字不动

## 一、配置变更：leader 独立读回，与巡检所述逐项一致

`multica agent list` 实读（非采信转述）：

| agent | id | model | thinking_level | updated_at |
|---|---|---|---|---|
| 实现 | `fd2d1252` | `codex/gpt-5.6-sol-xhigh` | `xhigh` | 2026-08-22T19:10:58Z |
| 规划 | `e9cf031c` | `codex/gpt-5.6-sol-xhigh` | `xhigh` | 2026-08-22T19:11:01Z |
| 验证 | `4f83bacc` | `codex/gpt-5.6-sol-xhigh` | `high` | 2026-08-22T19:11:04Z |
| 调度 | `96798779` | `claude/claude-opus-5` | — | （未动） |
| 评审 | `a8512fa6` | `claude/claude-opus-5` | `high` | （未动） |

三条 codex agent 全部由裸名改为 `-xhigh`；`thinking_level` 三项均保持原值，**未被顺带改动**，
与巡检声明一致。**provider 仍为 codex，未更换。**

key `multica-local-codex` 的 `allowedModels` / `allowedCombos` 与 canary（`http=200`、
`model=gpt-5.6-sol-xhigh`）在主控侧，leader 不可读，属受理转述，本分录不宣称已验。

## 二、A/B 对照面：无影响，账本不做 lane 变更归因

provider 未变，`ab_hard_constraint`（R-P1-02 / R-P1-07 保持 A/B、产出不得出现「维护者已批准」）
本就只约束计划内容，本次变更既不改 provider 也不触计划内容，**双重无涉**。
`leader/20260822-185000` §四的更正与本裁定不冲突：该更正说的是「换 lane 也不撞这条约束」，
本次连 lane 都没换，问题不再发生。

## 三、根因假说：采纳其**处置**，但因果**不记为已证**

巡检自述该假说（裸名 → 目录 miss → `resolveReasoningTransport()` 退化 `plaintext` →
`stripOpaqueFields()` 剥离 `encrypted_content`）**未逐行验证触发条件**，并以「裸名已不在目录
本身即是必须消除的既存缺陷」作为采纳理由。leader 同意该理由，且它不依赖假说成立。

`leader/20260822-185000` §3.1 的因果链方向被采信，此处补记其上游层，二者相容。

**但本分录追加一条 leader 侧的判据限定，避免下一轮误记「已修复」：**

`185000` §3.1 已确立 codex 失败是**阵发**——爆发段之间存在干净成功，且全 Issue 最长的两次 run
（含 `fd2d1252` 本人 136.2 分钟的 `d5de8897`）都是**变更前**的 codex 成功。故：

> **重派后单次成功，不足以确认本次变更是原因**——它同样与「爆发段自然结束」相容。

判据对称化如下：

| 重派后表现 | 可推出的结论 |
|---|---|
| 再出 `invalid_encrypted_content` 400 | 假说**证伪**（巡检 §6 已预置），转选项 B：拆小 brief 压低单轮上下文 |
| 失败形态转为 `stream disconnected before completion` | 剥离面消除、只剩流截断，同样转 B |
| 单次成功 | **不确认因果**。仅记录为「本轮通过」，不得写成「lane 故障已修复」 |
| 多次成功，或一次长时/重上下文 run 成功 | 归因强度上升，可记为「与变更相容的正向证据」，仍非证明 |

第三行是本分录新增的，其余照巡检 §6 受理。

## 四、`78ea9bb5` 那条「已跑通路径」：知识复用，形状仍须改

巡检要求「直接接着走」。leader 同意复用其**知识**（`cherry-pick 9f3e5858` 无冲突、4 个测试文件
干净 staged，`e8cab738` 确为 `…-r3` ancestor，leader 已实跑 exit 0），可省下一轮摸索。

但 `leader/20260822-185000` §五的限定不撤：该步骤建的是**从 upstream base 岔出的侧分支**，
而上一轮零内容 merge `ddb9560b` 正是「侧分支 + 并回工作分支」这个形状产生的。

**这不构成改 brief**：`green_dispatch_gate` 条件 (b)（`git diff <RED_BASE> <RED_DELTA>` 非空且落在
写集内）自 `20260822-174500` 起即为既定重开条件，本分录只是把它在派单文本里显式化，
scope 六条一字未改。落地要求：RED delta **直接落在 `…-r3` 上**，或并入后实测 diff 非空。

另核：`git ls-remote origin` 仍**无** `agent/hd30-red-base-20260822`，
`…-r3` head 仍为 `6c33e9d2`（leader 账本 commit），rescue 分支仍 `f90d64ee`。无新 Coder 产出。

## 五、处置

1. Coder lane 由 PAUSED **解除**，按 `20260822-174500` §6 六条**原样**重派 `fd2d1252`，
   brief 未作任何改动；
2. 前 6 次失败维持 **runtime/infra** 分类，不计入 Coder 能力评估、不进返修链
   （巡检 §5.4 与 leader `185000` §六.2 一致）；
3. `green_dispatch_gate` 保持 CLOSED，三条重开条件一字不改，仍由 leader 实跑复核；
4. `20260822-174500` 的 RED NOT ACCEPTED 三条理由、`write_set_precheck` CLEAN、加绑条件 2 的唯一
   受理证据、Plan Gate artifact `61d54ad3…`、R1–R5 与 A/B 边界、Publication 边界，全部不变；
5. leader 不改任何 agent 配置、不碰 key、不重复升级老板。
