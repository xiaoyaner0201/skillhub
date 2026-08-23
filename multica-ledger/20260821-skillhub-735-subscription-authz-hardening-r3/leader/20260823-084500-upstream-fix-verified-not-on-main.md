# leader / 20260823-084500 / 上游修复 leader 独立验证通过；但它**不在 `main` 上**，「等 `next` 重建」的前提需先核实

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`28916e49-a062-4d8e-9d48-c9a11a2939bb`（2026-08-23T08:16Z，dongsjoa 巡检）
- Stage：`PLAN_APPROVED_R2` 不变；Issue 保持 `blocked`；**不重派、不重试、不改 agent 配置、不碰镜像/compose/容器**
- 分录性质：外部事实独立验证 + 一处事实更正 + 裁决面机械影响
- **不含任何 RED 受理结论**；`20260822-174500` 的裁定一字不动

## 一、Multica 侧无变化（leader 读回）

`status = blocked`；除 leader 本轮 `8be1eac7` 外无活跃 run；实现 lane 自 `99a96c03` 起零产出；
`…-r3` 头仍 `a6104f06`，rescue 仍 `f90d64ee`。工程 Gate 全部未动。

## 二、上游修复：leader **实拉上游远端独立验证**，不采信转述

本容器无 `gh` 凭据、无 `docker`，但 `git fetch` 匿名读公开仓库可用。经
`multica repo checkout` 取得 OmniRoute 后直接 fetch 上游 `diegosouzapw/OmniRoute`：

| 核验项 | leader 实测结果 |
|---|---|
| `git merge-base --is-ancestor e73ab004 <upstream/release/v3.8.50>` | **exit 0** —— 修复确在该发布线上 |
| merge commit `e73ab0040cf33d6fafd45562b45558cca42f03ad` | 存在；`2026-08-22 22:49:11 -0300` = **2026-08-23T01:49:11Z**，与巡检所述 01:49:12Z 一致 |
| 作者 | `Ke Jin`（GitHub handle `jackjinke`），**非我方** |
| `streamHandler.ts` hunk | 与巡检引用**逐字一致**（先扫描 `hasClientTerminalSseMarker`，后 `slice(-4096)`） |
| 改动面 | `codex.ts` / `streamHandler.ts` / `streamReadiness.ts` + 3 个测试文件，152 插入 / 14 删除——与巡检所述一致 |

**巡检的转述准确，不需要更正。** 以下两条是 leader 额外做的、转述里没有的验证。

## 三、该修复**确实覆盖我方失败形态**——两个前提逐条验过，不是推定

上游 PR 把场景写成 *compaction* 帧，我方是超长 `response.completed` 帧（5928B）。
「修法通用」不能只凭 PR 措辞断言，leader 读源码核了两点：

1. **改动落在通用路径 `noteClientChunk` 内，无任何 compaction 条件分支。** 任何超出尾窗预算的
   终止帧都走这里，故与帧类型无关，覆盖我方形态。
2. **`clientTerminalSeen` 具备粘性**——`streamHandler.ts:683` 是 `if (clientTerminalSeen) return;`，
   一旦置真即提前返回，后续 chunk 不会把它重新赋值为 false。

第 2 点是**这次重排是否成立的关键**：若该标志可被后续 chunk 覆写，「先扫描后截断」只会把缺陷
从「标记被自身体积挤出窗口」搬成「标记先被看见、再被后续帧挤掉并复位」，等于没修。
粘性存在，故重排是完备修复。**leader 据此确认：升级到含该修复的镜像后，本 Issue 的一阶故障面消失。**

## 四、一处需要更正的事实：修复**不在 `main` 上**

巡检标题写「**已合入 main**」，正文则正确写着 base 为 `release/v3.8.50`。leader 实测：

```
git merge-base --is-ancestor e73ab004 <upstream/main>   → 非祖先
upstream/main head = c68cda7d  2026-08-21 22:06:21 -0300（早于修复）
```

**修复目前只在 `release/v3.8.50`，不在 `main`。**

这不是措辞挑剔，它直接决定「等 `next` 镜像重建」这条路是否成立：

> **若 Docker 标签 `next` 构建自 `main`，则未来任何一次 `next` 重建都不会包含该修复**——
> 等待是徒劳的，必须改等 `release/v3.8.50` 线上的标签，或等修复反向合入 `main`。

巡检的推理「`next` 上次构建 08-22T23:37Z 早于合并 01:49Z ⇒ 现有镜像都不含修复」**结论对**，
但它隐含了「`next` 构建自被修分支」这一未验证前提。**建议在等待之前先核实 `next` 的构建来源分支**
（该核实在主控侧，leader 无 docker/registry 访问权，不代做）。

## 五、裁决面的机械变化（leader 仍不选方向）

1. **「我方自行打补丁」应当\*撤回\*，不只是降价。** 上游已合入，自行改本机生产件会造出一个
   数小时后即需回收的本地分叉，徒增对账面。该支可以从选项集中划掉。
2. **「切 lane」那支的代价评估维持 `20260822-210500`**：全量切仍会使 Squad 塌缩为单一 lane →
   Council 异构不可用 → `SECURITY_REVIEW_BLOCKED`；只切 `实现` 异构安全但覆盖不全。
   若镜像升级可行，这两条代价**都不必付**。
3. **升级不单独解锁 `实现`**：`20260822-202500` §4.1 的弃用脏会话仍是必要前置——已污染的
   reasoning item 留在该 agent 会话状态里，镜像升级不追溯清理。区别是：**旧镜像下弃用「必要
   不充分」，含修复的镜像下弃用「必要且充分」。**
4. 镜像升级仍是**生产 Live 变更、跨 workspace**，不在本链授权内，归 dongsjoa。

## 六、leader 本轮的动作边界

只读：`multica repo checkout` 取 OmniRoute、匿名 fetch 上游两条分支、读源码与 diff。
**无任何写操作**——未 push、未改 compose、未拉镜像、未重启容器、未重派、未改 agent 配置、
未动 Gate、未签异构豁免、未代决路由、未重复升级老板。

巡检 §五的 cron working-directory lock 失败归主控调度，本链不处理，leader 亦不代查。

## 七、不变项

`green_dispatch_gate` CLOSED（(a)(b)(c) 一字不改）、`red_intake` NOT ACCEPTED 三条理由、
`write_set_precheck` CLEAN、加绑条件 2 的唯一受理证据（addendum `3812652e`）、
Plan Gate artifact `61d54ad3…`、R1–R5 与 A/B 硬约束、Publication 边界，全部不动。
9 次失败仍归 runtime/infra（所指为该滑窗缺陷），不计入 Coder、不进返修链。
