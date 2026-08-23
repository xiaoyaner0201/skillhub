# leader / 20260823-124500 / 解除 `blocked`，Coder 第 10 次派单（brief 一字不改）；`upstream_fix` 判据整体作废；一处路径冲突按 dongsjoa 留给 leader 的 Gate 权限机械处理

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`47a95d22-be2d-4a3f-8c0c-85fbd46081fa`（2026-08-23T12:3x，dongsjoa 主控会话）
- Stage：`PLAN_APPROVED_R2` 不变；Issue `blocked` → `in_progress`；Coder lane RE-DISPATCHED
- 本分录 **作废**：`upstream_fix` 全部检查点（含 `20260823-121000` §四刚更正过的恢复主判据）——**判据没判错，是它要等的东西不必再等**
- **不含任何 RED 受理结论**；`20260822-174500` 的裁定一字不动；`green_dispatch_gate` 仍 CLOSED

## 一、可独立验证的部分：全部核实相符

dongsjoa 给的两条上游事实，leader 匿名 GitHub REST 实读：

| 断言 | 实测 |
|---|---|
| 已提 issue `#11245` | **存在**，`state=open`，`created_at=2026-08-23T12:23:04Z`，作者 `xiaoyaner0201`；标题与转述**逐字相同** |
| 引入者 `#10980` merged `2026-08-21T17:59:08Z` | **相符**：`merged_at=2026-08-21T17:59:08Z`，`merge_commit=9469b9c79`，标题 `fix(sse): surface bare upstream close as response.failed for Responses clients` |

`#10980` 的标题本身即印证归因链的第一环——**把「裸 upstream close」判成 `response.failed` 的逻辑扩展到
Responses 客户端**，正是 `failure_root_cause` 里那条伪造终止帧的来源。

**本机补丁与三组验证数据，leader 无访问权（在另一容器），受理转述、不宣称已验。**
但 dongsjoa 的 §六预登记了三种失败形态及各自去向，这是**可证伪的**——
**真正的验证是本次派单本身**，其结果会直接判定该补丁是否奏效。leader 采纳该三分支triage。

## 二、一处机械冲突：请求 §五.3 与 leader 已登记的 `red_base_path_note` 相抵

dongsjoa 写「`78ea9bb5` 崩前已跑通……**这条路径直接接着走**」，同时又写
「`green_dispatch_gate` 三条重开条件……**仍归你判**」。两句在本项上不相容，leader 按后者处理：

1. **`agent/hd30-red-base-20260822` 在 origin 上不存在**（`git ls-remote refs/heads/agent/hd30-red-base-20260822`
   计数 **0**）。staged 状态随 workdir 一起消失（`red_base_path_note` 已登记）。
   **没有"接着走"的对象**，只有 cherry-pick 这一条**知识**可复用。
2. 该侧分支形状正是上一轮零内容 merge `ddb9560b` 的来源，**不满足重开条件 (b)**
   （`git diff RED_BASE RED_DELTA` 非空）。

**但两边其实不冲突到结论**：`20260822-194500` §三的 brief **本来就写着**——

> 已验知识：`git cherry-pick --no-commit 9f3e5858` 自 `e8cab738` 起无冲突、4 测试文件干净 staged
> —— 直接复用，**但不得复制其侧分支形状**（零内容 merge `ddb9560b` 由此而来）
> 落地形状：RED delta 直接落在 `…-r3`，或并入后实测 `git diff <RED_BASE> <RED_DELTA>` 非空

即「brief 一字不改」与「保住护栏」**是同一件事**。leader 按 §五.2「brief 一字不改」执行，
护栏自动随之保留，无需 leader 另加约束。**唯一改动是一处事实刷新**（见 §三）。

## 三、brief 中唯一必须刷新的事实项（非 scope 变更）

目标分支 head 由 `77a2c53a` 变为 **`22494a64`**。leader 实测该区间**全部为 ledger 提交**：

```
e8cab738..HEAD 共 38 个 commit
其中触碰非 multica-ledger 路径的：仅 046c04ed «Harden subscription authorization and fanout»
非 ledger 变更面：17 个文件（与 174500 核过的写集一致）
```

**交付面未被 leader 的账本提交污染**，`write_set_precheck` CLEAN 继续成立。

## 四、派单前不变项复核（leader 实跑）

| 项 | 实测 |
|---|---|
| frozen base 漂移 | `iflytek/skillhub@main` head = `e8cab738…` = 冻结 base，`rev-list --count base..upstream/main` = **0**，**仍为零** |
| `9f3e5858` | 存在，`test: capture subscription authz RED on upstream base`，**4 个测试文件** |
| RED delta 是否已在 `…-r3` | **否**——`VisibilityCheckerLegacyCompatibilityTest.java` 在 rescue 分支存在、在 `…-r3` 不存在。`red_intake` 不受理理由 (1)（`ddb9560b` 零内容）**继续成立** |
| rescue 分支 | `20260822-hd30-red-rescue-session-01a02a28` = `f90d64ee`，origin 上仍在，可只读引用 |

## 五、`green_dispatch_gate` 仍 CLOSED——"解除 blocked"不等于"Gate 打开"

必须写清，避免被误读：**它是 Coder 产出的受理 Gate，不是派单的前置条件**。
条件 (c)（存在新的 `coder/2026*-red-output.log`）在派单前**按构造必然不满足**。
(a)(b)(c) 三条一字不改，仍由 leader 在 Coder 交付后**实跑**复核。

`red_intake` NOT ACCEPTED 三条理由、`write_set_precheck` CLEAN、加绑条件 2 的唯一受理证据
（addendum `3812652e`）、Plan Gate artifact `61d54ad3…`、inventory `efd911ad…`、R1–R5 与 A/B 硬约束、
`superseded_candidate`（不得把新工作绑到 `046c04ed`/tree `92219560`）、Publication 边界，**全部不动**。

## 六、lane 与失败归属

- **lane 路由裁决项消解**：provider 未换、三条 codex agent 原地可用，Council 异构面不变
  （`lane_vs_heterogeneity` 的塌缩风险不再触发），`ab_hard_constraint` 不受影响。
  `20260822-210500` 的三路径后果表随其前提一并失效。
- **前 9 次失败仍归 runtime/infra**，不计入 Coder 评估、不进返修链——与 dongsjoa 一致。
- `20260822-194500` §四「再失败则不自动重试、置 blocked」**由 dongsjoa §六的三分支 triage 取代**：
  失败形态本身即路由依据，leader 按其分流，不再无差别 blocked。

（附记：`coder_lane` 元数据已于 12:33Z 被主控会话侧更新为 UNBLOCKED，leader 读回内容与本轮指令一致，予以沿用。）

## 七、动作边界

本轮 leader 的写操作仅三项：**置 `in_progress`**、**发一条派单评论（@ 实现）**、**追加本分录**。
未 push 公开分支、未碰 PR #735、未建上游 PR、未签 CLA、未改写作者、未 force-push、
未改任何 agent/网关配置、未动 Gate、未签异构豁免、未 @ 老板。
Publication 边界不变——工程链终点仍是 exact subject tree READY。
