# leader / 20260822-185000 / 第 6 次失败：故障面重定位到 lane，Coder 暂停重派

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`bdc39699-70ba-49cf-8aa9-53332b56f209`（2026-08-22T18:43Z，dongsjoa 巡检）
- Stage：`PLAN_APPROVED_R2` 不变；Coder lane 置 **PAUSED（待 boss 裁定 lane 归属）**
- 分录性质：诊断更新 + 升级判据作废 + 暂停登记。**不含任何 RED 受理结论，不改动 `20260822-174500` 的裁定**
- 本分录 supersede `leader/20260822-181000` §四的升级判据表

## 一、我的预置判据按第二行命中，第一行未命中（leader 实读）

`20260822-181000` §四三行判据中：

| 实读事实 | 来源 | 命中行 |
|---|---|---|
| `78ea9bb5` 18:09:49 → 18:14:53，历时 **5m04s** | `multica issue runs`（leader 独立读） | 非第一行（<20s 秒挂） |
| 错误评论 `aace6db7`（18:14:53）正文 item = `rs_018fde0af8142d86016a89e704d27c87d0b3cc699792407717` | leader 直读本 Issue 评论 | 第二行：**item id 不同** |

故第二行成立：**「新 session 亦被毒化 → 属另一故障面，重新定位，不得套用本轮归因」**。
`20260822-165500` / `181000` 的「session 毒化重放」归因**到此为止**，不再向后适用。
巡检对我判据的援引正确。

同时确认：`181000` §四受理的「退休后开新 session」机制**已生效**（新 session `01a02aaa`），
该机制无需再列为待验项。

## 二、lane 层归因：受理，且 leader 用自己可读的面独立复算

OmniRoute 计数（410 次 `codex/gpt-5.6-sol` 请求 / 139 次 terminal-marker 缺失 / claude lane 0 次）
在主控侧，leader 不可读，属受理转述。**但按 agent 拆 run 的分布 leader 可独立读回**，结论方向一致：

| lane | agent | completed | failed |
|---|---|---|---|
| claude | 调度 `96798779` | 45 | 0 |
| claude | 评审 `a8512fa6` | 1 | 0 |
| codex | 实现 `fd2d1252` | 2 | 6 |
| codex | 规划 `e9cf031c` | 3 | 6 |
| codex | 验证 `4f83bacc` | 3 | 2 |

claude 两条 lane 合计 46 完成 / **0 失败**；codex 三条 lane 合计 8 完成 / **14 失败**。
lane 与失败强相关这一点，leader 独立成立。

## 三、但同一份数据推翻两条附带判断，且这两条会影响 boss 的裁决面

### 3.1 codex lane 不是「持续不可用」，是**阵发**

leader 按时序排全部 22 次 codex run，失败集中在**三个爆发段**，段间有干净成功：

```
08-21 11:42–12:01  验证 FAIL ×2
08-21 12:24–16:20  验证/规划/实现 ok ×6      ← 含全 Issue 最长的两次 run
08-21 17:33–17:59  规划 FAIL ×6
08-22 14:06–14:56  规划 ok ×2
08-22 14:56–18:09  实现 FAIL ×6
```

**全 Issue 最长的两次 run 都在 codex 上且都成功**：`d5de8897`（实现 `fd2d1252` 本人，**136.2 分钟**，
08-21 13:42）、`c78f0d9e`（验证，64.9 分钟）。

### 3.2 故「第 7 次的先验成功率没有理由高于前 6 次」这句过强

同一 agent、同一 lane 有 136 分钟成功先例，故基础成功率不为 0。准确表述是
**失败率高且当前处于爆发段**，而非成功不可能。

**leader 仍然同意暂停**，理由不变且充分：连续 6 次失败足以停手，且每次尝试都会再产出一条
污染 item。但措辞差别会传导到 boss 的选项集：若按「不可能成功」理解，唯一解是改配置；
若按「阵发」理解，则**等待爆发段过去后重试**是一个不触碰任何配置的候选项。
两者成本相差很大，故本分录把这一条更正登记。

### 3.3 「上下文规模」假说同样被削弱

除 3.1 的 136 分钟成功外，另有一处直接反证：08-22 **14:56:10 两条 codex run 同分钟启动**——
规划 `187a9c90` 完成（11.3m），实现 `1afdebdd` 失败（40.7m）。故该时刻**不存在 lane 级全停**。
上下文重与失败可能相关，但本数据集无法把它与 lane 归因分离（重任务恰好都在 codex 上），
**属混淆变量，不宜作为独立结论上报**。

## 四、一处需要更正的约束援引：lane 变更**不触碰** `ab_hard_constraint`

巡检称改 model/provider「直接触碰 `ab_hard_constraint` 所保护的 A/B 对照面
（R-P1-02 / R-P1-07 仍须保持 A/B）」。leader 读回 metadata 原文：

```
ab_hard_constraint = "R-P1-02/R-P1-07 stay A/B; any text claiming maintainer approved => RETURN"
```

该约束的保护对象是**计划内容**——两条 plan risk 在维护者未确认前必须并列保留、产出中不得出现
「维护者已批准」字样。它与**哪个模型执行 agent** 无关：换 lane 既不解析 R-P1-02 / R-P1-07，
也不产生「维护者已确认」文本。属**援引错位**。

lane 变更仍需 boss 裁定，但理由是「workspace 配置变更 + 额度/成本」，**不是撞上硬约束**。
两者对 boss 的决策成本不同，故必须更正；否则 boss 会以为这条路被一条不可放宽的约束堵死。

（若另有「codex vs claude 模型对照」的实验意图，它**未记录在任何 leader 可读处**；
若存在，应显式立项，不应由本 key 承载。）

## 五、「已验证可行的路径」：有价值，但**不是成品配方**，照抄会走回上一轮的坑

巡检报 `78ea9bb5` 崩前跑通：

```
git switch -c agent/hd30-red-base-20260822 e8cab738
git cherry-pick --no-commit 9f3e5858     → 4 个测试文件干净 staged，无冲突
```

leader 核：`e8cab738` **确为** `…-r3` 的 ancestor（`merge-base --is-ancestor` exit 0），故该起点本身没错。
**但该分支从 upstream base 岔出，不长在 `…-r3` 上。** 上一轮 `ddb9560b` 的零内容 merge，正是
「侧分支 + 并回工作分支」这个形状产生的：ancestry 全过，树里没内容。

故本分录明确登记：

- 该步骤是**可行的中间步**，证明 cherry-pick 无冲突，省去下一轮摸索；
- 它**不满足** `green_dispatch_gate` 的 (a)(b) 两条。交付时 RED delta 必须**直接落在 `…-r3` 上**
  （在 `…-r3` 上 cherry-pick），或在并入后实测 `git diff <RED_BASE> <RED_DELTA>` 非空且在写集内；
- 接手方不得把它当作完整配方照抄。

另核：`git ls-remote origin` **无** `agent/hd30-red-base-20260822`。staged 状态随 workdir 消失，
故这是**已验证的知识，不是留存的产物**，无可抢救物，与巡检「不存在需再抢救的东西」一致。

## 六、leader 的处置

1. **已暂停对 `实现 fd2d1252` 的一切重派**，直到 boss 就 lane 归属裁定。本分录不 mention 该 agent。
2. **本轮 6 次失败不计入 Coder 能力评估，不进入返修链**：分类仍为 **runtime/infra**
   （同 `20260822-165500` §五），非 `IMPLEMENTATION_DEFECT` / `TEST_GAP` / `PLAN_GAP`，
   故不路由 Planner / QA / Reviewer。
3. **不改任何 agent 的 model / provider**——同意这属 workspace 配置变更，不在 leader 自决范围。
4. **不重复升级**：巡检已在 boss 的 Discord thread 同步，leader 不再另发一次升级，不重复 @ 老板。
5. 工程结论全部不动：`20260822-174500` 的 RED NOT ACCEPTED 三条理由与本故障**正交**，继续有效；
   `write_set_precheck` 保持 CLEAN；`green_dispatch_gate` 保持 CLOSED，三条重开条件一字不改；
   加绑条件 2 的唯一受理证据、Plan Gate artifact `61d54ad3…`、R1–R5 与 A/B 边界不变；
   Publication 边界不变。

## 七、恢复派单的前置条件（替代 `181000` §四的判据表）

boss 裁定 lane 归属后，无论裁为「换 lane」还是「原 lane 等爆发段过后重试」，恢复派单前须满足：

- 有一次 **非本 Issue 的、同 lane 的长任务成功**，或 boss 明确指定新 lane；
- 派单 scope 仍照 `20260822-174500` §6 六条**原样**，附本分录 §五的路径限定；
- 首次恢复后若再失败，读回失败形状（时长 / item id / 是否 mid-work）与本分录三行对照，
  不再默认套用任何既有归因。
