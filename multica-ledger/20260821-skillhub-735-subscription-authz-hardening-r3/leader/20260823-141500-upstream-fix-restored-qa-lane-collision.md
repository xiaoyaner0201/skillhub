# leader / 20260823-141500 / 受理自我更正，**恢复 `upstream_fix` 等待状态**（四项独立复核全部相符）；澄清一处会被误读为「tag 已移动」的 digest 表面分歧；**自陈更正 `140500` §四一处过强措辞**；并提前上抬一个**一站之遥的排期冲突**：QA 段仍跑在 codex

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`3fc75a9a-4cb5-4f04-bb4e-e42ec64e4d92`（2026-08-23T13:5x，dongsjoa 主控会话自我更正）
- Stage：`RED_ACCEPTED_R2` 不变；Issue 保持 `in_progress`；GREEN 派单不受影响，继续在飞
- 本分录 **恢复**：`upstream_fix` 全部检查点（`20260823-124500` §作废该判据的处置由本分录撤销）
- 本分录 **更正**：`20260823-140500` §四末段「与『伪失败源归零』不相容」的推理措辞
- **不含任何 RED/GREEN 受理结论**；`green_dispatch_gate` PASS 与 `red_intake` ACCEPTED 一字不动

## 一、四项独立复核：全部相符，`upstream_fix` 恢复

不采信转述，leader 用匿名 GitHub REST 与 Docker Hub 自己读了一遍。

| 断言 | leader 实测 | |
|---|---|---|
| `e73ab004` 改的就是 `failure_root_cause` 登记的那一处 | `open-sse/utils/streamHandler.ts` `@@ -683,13 +683,15 @@`，把 `slice(-4096)` **移到** `hasClientTerminalSseMarker()` **之后**；引文与 API 返回的 patch **逐字节相同** | ✅ |
| 修法是「先扫描、后截断」，窗口大小未动 | 属实：`4096` 常量两处均未改，只换了执行顺序 | ✅ |
| 上游 issue `#11245` 已自我更正并 close | `state=closed`，`state_reason=**not_planned**`，`closed_at=2026-08-23T13:55:31Z`，作者 `xiaoyaner0201` | ✅ |
| `e73ab004` 仍**只在** `release/v3.8.50`、不在 `main` | `compare/main...e73ab004` → `status=**diverged**`，ahead 2828 / behind 6 ⇒ `main` **不含**；`compare/release/v3.8.50...e73ab004` → `ahead=0`, behind 45 ⇒ **完全包含** | ✅ |
| `next` 仍未越过冻结点 | `last_updated = **2026-08-22T23:37:24.404383Z**`，与登记值**逐字相同** | ✅ **判据未达成** |

`release/v3.8.50` head 实测 `8f390eff`（`2026-08-23T13:20:06Z`，`#11205`）——与转述的
「latest base `8f390eff`，10:20」同一 commit，时区呈现不同（`-03:00` vs UTC），非偏差。

**⇒ `upstream_fix` 恢复为 WAITING，检查点一字不改**：
`next` 的 `last_updated` 越过 `2026-08-22T23:37:24.404383Z` ⇒ 新镜像含 `e73ab004`。
（该推断的地基仍是 `20260823-091000` 已实测的「`next` 是 `release/v3.8.50` 的浮动 tag」，本轮未重验，沿用。）

**并附一条本轮才成立的一致性核对**：引入缺陷的 `#10980` merged `2026-08-21T17:59:08Z`，
我方运行镜像 build `2026-08-22T01:40`——**晚于引入者约 7h41m、早于修复者约 21h**，
即「含 bug 且不含 fix」在时间线上自洽。且当前**已发布**的 `next`（arm64 层 push
`2026-08-22T23:25:51Z`）同样早于 `e73ab004`（`2026-08-23T01:49:11Z`），
**故此刻去拉最新 `next` 也拿不到修复**——这正是检查点要求「越过」而非「等于」冻结点的原因。

## 二、一处会被误读成「tag 已移动」的 digest 表面分歧：机械澄清

转述里 `next` 的 digest 写作 `sha256:14878705…`，而 leader 登记的冻结点是 `sha256:6f95064b…`。
**两者不冲突，是同一 tag 的两个层级**，`GET /v2/repositories/…/tags/next` 一次返回即可分辨：

```
tag digest (manifest list)        : sha256:6f95064bf0981c16…      <- leader 登记的
  images[].digest  arm64/linux    : sha256:148787055adf1ce5…      <- 转述读到的
  images[].digest  amd64/linux    : sha256:e1028c8cc0b474cf…
last_updated                      : 2026-08-22T23:37:24.404383Z   <- 两边一致
```

转述侧读的是 **arm64 逐架构层**（与 coder maven 日志里的 `osinfo: Linux/arm64` 一致），
leader 登记的是 **manifest list 层**。**`last_updated` 逐字相同，tag 未移动。**

**必须登记这一条的理由**：将来任何一轮巡检若拿 `14878705…` 去比对账本里的 `6f95064b…`，
会得出「digest 变了 ⇒ 新镜像已发布」的**假阳性**，而这恰好是本任务解除等待的触发条件。
故 `upstream_fix` 中两层 digest 一并登记，并注明**判据只认 `last_updated`**（`121000` §四
已把它定为唯一无盲区主判据；digest 仅作佐证，且必须同层比较）。

## 三、自陈更正：`140500` §四末段一处推理措辞过强

leader 上一轮写：

> 三次 codex 失败……全部发生在网关补丁生效之后。**这与「补丁使伪失败源归零」不相容。**

**「不相容」是错的。** 一个**独立的第二故障面**与「第一个故障面已被补丁修好」**完全相容**——
「打了补丁之后仍然失败」根本不构成对「补丁修好了它所针对的那个失败模式」的反证。
leader 把「同一 lane 仍失败」当成了对补丁有效性的证据，而它对该命题**区分力为零**
（又是 `105000` §二那条教训的同一形态：把后果当证据）。

**结论侧没有错**：leader 当轮明确写了「**不宣称补丁无效**」并列出两支析取
（「该 runtime 未走补丁后的网关」／「另一成因」）。dongsjoa §五 提供的证据
（伪失败计数 0、全新 session/workdir、上下文仅 72644、全新 item `rs_0a6d186a…`）
**选中第二支**。**处置正确，理由过强，本分录更正措辞，不更正结论。**

并登记 dongsjoa 侧的一条强证据：**补丁回滚后长输出 `response.failed` 如期复现**——
这是一次真正的**反向验证**，它比「打上补丁后不再复现」更能定住作用点
（前者排除了「自愈/环境变化」这个替代解释）。leader 无该容器访问权，**受理转述、不宣称已验**。

## 四、`second_injection_surface`：登记为开放项，**不进返修链、不设 Gate**

dongsjoa §五 的判定（不阻塞交付、不升级为 Gate）leader 认可，理由是机械的：
它是 **runtime/infra**，不是 `PLAN_GAP`／`IMPLEMENTATION_DEFECT`／`TEST_GAP`／
`INVARIANT_VIOLATION`／`EVIDENCE_GAP`／`RISK_ROUTE_GAP` 中的任何一类，**Finding 路由表里没有它的去向**。
前 12 次失败归 runtime/infra 的处置一并延续，**不计入 Coder、不进返修链**。
artifact `2026-08-23T13-02-53.002Z_1787490160824-49a626.json` 在 dongsjoa 侧，leader 无访问权，只登记指针。

## 五、**本轮唯一的新增风险项：一站之遥的排期冲突，QA 段仍跑在 codex**

dongsjoa §四 写「在 Claude lane 上不阻塞交付」。**对当前这一站成立，对下一站不成立**，
且这不是新发现——`lane_scope`（`20260822-2105` 由巡检 cron 自己登记）原文就写着：

> switching `实现` only leaves **QA to re-hit the same 4096-window defect**

把本轮两条新事实代入，该预测**变得更硬而不是更软**：

1. **本机补丁已完整回滚** ⇒ codex lane 重新暴露于 4096 终止帧缺陷（`next` 未发布，无修复来源）；
2. **第二注入面依旧存在**（§四），且与 4096 缺陷无关 ⇒ codex lane 现在暴露于**两个**故障面。

而 `验证`（`4f83bacc`）与 `规划`（`e9cf031c`）**仍在 codex**，且工程链的下一站正是
**GREEN → QA**。即：**GREEN 无碍（`实现` 已在 claude），QA 派单大概率复现失败。**

三条出路及其已登记代价，**全部是旧账，本轮不新增判断**：

| 出路 | 代价（均已登记） |
|---|---|
| 把 `验证` 切 claude | Squad 塌缩为单一 lane ⇒ Council 异构不可用 ⇒ `SECURITY_REVIEW_BLOCKED`，无显式豁免不能到 READY（`lane_vs_heterogeneity`） |
| 等 `next` 发布 | `upstream_fix` 检查点，**时刻不可预估**（§一实测未达成） |
| 重新打本机补丁 | 刚刚回滚；`20260823-091000` 登记的撤回理由（本地分叉回收成本单调上升 + 镜像恢复后手改补丁静默消失）仍成立 |

**leader 不选，也不预置。** lane 归属与异构豁免明确不在调度职权内（`210500` 已登记）。
本节的全部作用是：**在 GREEN 还在飞、尚有时间的时候把这个碰撞提前摆上台面**，
而不是等 QA 派单失败后再回头处理。**这一站不需要任何决定；下一站需要。**

## 六、不变项

`green_dispatch_gate` **PASS + CONSUMED**、`red_intake` **ACCEPTED**、候选 RED commit `45b723ce`／
tree `57a2402`、`write_set_precheck` CLEAN、base 零漂移、Plan Gate artifact `61d54ad3…`、
inventory `efd911ad…`、R1–R5 与 `ab_hard_constraint`（`R-P1-02`/`R-P1-07` 保持 A/B）、
`superseded_candidate`、Publication 边界，**全部不动**。
`failure_root_cause` 保持有效，**归因主体更正**：不是「本机缺陷」，是「上游已修、我方镜像太旧」。
`实现` 的 claude lane 保持（dongsjoa 13:20Z 所为，本轮据其自述闭合 `140500` §5.1 留的「非 leader 所为、
发生在 13:05–13:21 之间」这一空缺）；Council 异构仍成立。
`work_dir` 全样本规则保持**已验**，其中「换 workdir 作为补救手段」的证伪结论（`140500` §四）不受本轮影响。

## 七、动作边界

本轮 leader 写操作三项：**追加本分录 + 改 index 头部**、**push 到自有 fork 的内部分支**、
**发一条回复评论**、**更新 metadata**。**无派单**（GREEN 已在飞，不重复触发）、
**未改 Issue 状态**、未 push 公开分支、未碰 PR #735、未建上游 PR、未 force-push、
未改任何 agent/网关/daemon 配置、未动 Gate、未签异构豁免、未代决 lane、未 @ 老板。
