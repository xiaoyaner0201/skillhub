# leader / 20260822-210500 / lane 选择与 Council runtime 异构的耦合：全量切 claude 会触发 `SECURITY_REVIEW_BLOCKED`

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`20e0a6d8-19b5-4b2b-aacb-ae05d8f4aa00`（2026-08-22T21:01Z，dongsjoa 巡检）
- Stage：`PLAN_APPROVED_R2` 不变；Issue 保持 `blocked`；**不重派、不重试、不改 agent 配置、不碰网关**
- 分录性质：**机械约束登记**（HIGH_RISK 异构规则与 lane 变更的耦合），供 dongsjoa 裁决
- **不含任何 RED 受理结论**；`20260822-174500` 的裁定一字不动

## 一、无新增事实（leader 独立读回）

`status = blocked`；最后评论为 leader `20:30:00`；除 leader 本轮 `398b7970` 外无活跃 run；
`…-r3` 头 `077dfad3`（leader 账本 commit），rescue 仍 `f90d64ee`，**无新 Coder 产出**。
巡检本轮亦无写操作。

## 二、巡检采纳 leader `20:30` §三的范围事实，并据此改倾向为「打网关补丁」

leader 同意该结论方向，但其给出的反对理由（「三个 agent 的行为特性与既有分录一致性同时出现断点」）
**偏软，且不是真正的阻碍**。真正的阻碍是一条**机械规则**，此前未被任何一方抬出，登记如下。

## 三、决定性约束：切 `验证` 会摧毁 Council 的 runtime lane 异构

Squad 方法对 HIGH_RISK 的规定（正本在 skill，此处只援引）：Council lens
**「优先分配不同 runtime lane」**，且**「异构不可用时禁止自动进入 READY 交接，转
`SECURITY_REVIEW_BLOCKED`」**。

leader `20260822-143334` 的 Council 名额校验已登记：3 名具名 lens（Kleppmann / Schneier /
Beck 锚点，含 1 名 dissent），并明确写下**「runtime lane 异构分配在 Review 派发时才落实，
本轮不预置」**——即该约束尚未消费，仍在前方等着。

当前 lane 存量（leader `20:30` 实读）：

| lane | agent |
|---|---|
| claude | `调度 96798779`（非 lens 持有者）、`评审 a8512fa6` |
| codex | `规划 e9cf031c`、`实现 fd2d1252`、`验证 4f83bacc` |

**判据阈值**：≥2 条可用 lane → 3 个 lens 可跨 lane 铺开，满足「优先分配不同 lane」；
**仅剩 1 条 lane → 异构不可用**，触发 `SECURITY_REVIEW_BLOCKED`。

据此三条路径的机械后果**互不相同**：

| 路径 | 缺陷覆盖 | Council 异构 | 能否走到 READY |
|---|---|---|---|
| 只切 `实现` | **部分**（QA execution 段仍会命中） | **不受影响**——Coder 既非 lens 持有者、也非 QA/Review 任一侧，`评审`(claude) 与 `验证`(codex) 仍分处两 lane | 可，但中途预期再停一次 |
| 切 `实现`+`验证`(+`规划`) | 完整 | **归零**——全 Squad 塌缩为单一 claude lane | **不可**：转 `SECURITY_REVIEW_BLOCKED` |
| 打网关补丁 | 完整 | **保持两 lane** | 可 |

即全量切 claude **买到了整条链路，却赔掉了链路终点**。除非 dongsjoa 另行签发一份异构豁免——
而那是安全/语义裁决，**不在 leader 职权内，leader 不代决、不预设可豁免**。

## 四、两条对「打补丁」的诚实配平（leader 不选方向）

1. **爆破半径更大**：网关是跨 workspace / 跨 Issue 的生产件，补丁影响面**宽于**仅作用于本 Squad
   的 lane 切换。但需同时记明：该缺陷正在**对已成功的流注入伪造 `response.failed`**，
   属数据正确性缺陷，修它不是为 HD-30 便利。两面都摆出，由 dongsjoa 权衡。
2. **补丁不单独解锁 `实现`**：`20260822-202500` §4.1 的「弃用脏会话」仍是必要前置——
   已被污染的 reasoning item 留在该 agent 的会话状态里，补丁不追溯清理。
   区别在于：**A / C 下弃用是「必要但不充分」（下一次长输出重造）；补丁下弃用是「必要且充分」。**

## 五、不变项与处置

leader 本轮不重派、不重试、不改任何 agent 的 model / provider、不碰网关、不代决异构豁免、
不重复升级、不 @ 老板。方向性取舍归 dongsjoa。

`green_dispatch_gate` CLOSED（(a)(b)(c) 一字不改）、`red_intake` NOT ACCEPTED 三条理由、
`write_set_precheck` CLEAN、加绑条件 2 的唯一受理证据（addendum `3812652e`）、
Plan Gate artifact `61d54ad3…`、R1–R5 与 A/B 硬约束、Publication 边界，全部不动。
9 次失败仍归 runtime/infra（所指为网关滑窗缺陷），不计入 Coder、不进返修链。
