# leader / 20260822-202500 / 根因定位到本机网关终止标记窗口缺陷：runtime/infra 所指更正（superseding）

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`c3f9aad8-e6f7-4ec0-b1f3-edd7e3419501`（2026-08-22T20:21Z，dongsjoa 巡检，转 dongsjoa 网关源码层定位）
- Stage：`PLAN_APPROVED_R2` 不变；Issue 保持 `blocked`；本轮**不重派、不重试、不改 agent 配置、不碰网关**
- 本分录 **supersede**：`leader/20260822-200500` §三的判据表述、§4.1 的前置条件表述、§4.4 的「上游间歇不稳」
- **不含任何 RED 受理结论**；`20260822-174500` 的裁定一字不动

## 一、无新增事实（leader 独立读回，与巡检声明一致）

`status = blocked`；最后一条评论为 leader `20:11:06`；除 leader 本轮 `6155c4c8` 外无活跃 run，
上一条 run 为 leader 自己的 `cf6ee38f`（20:11:41 completed）；
`…-r3` 头仍 `40356b57`（leader 账本 commit），rescue 分支仍 `f90d64ee`，**无新 Coder 产出**。

## 二、受理根因，并更正 runtime/infra 的所指

`streamHandler.ts:685-687` 的终止标记检测只在**最后 4096 字节**滑窗内找 `event: response.completed`；
Codex 的 completed 帧实测 **5928 字节**，被自身体积挤出窗口 → `clientTerminalSeen=false` →
`resolveSilentCloseOutcome` 在**已成功**的流后追加伪造 `response.failed`（`"id":null`）。
对照实验（Codex 长 5928B → failed ×2；Codex 短 1778B → 0；Claude 51B → 0）**只改输出长度即精确翻转**，
因果判定成立。

网关源码与 curl 复现在主控侧，leader 不可读，属**受理转述**；但其与 leader 侧全部可读事实相容，
且比 leader 既往两个假说的解释力都强（见 §三）。

**runtime/infra 分类不变，所指更正**：由「codex 上游间歇不稳」改为
**「本机 OmniRoute 终止标记滑窗缺陷」**。9 次失败仍不计入 Coder、不进返修链、不路由
Planner/QA/Reviewer。归属未变，只是指称对象换了。

`200500` §4.4 用 19:50 三次 1-msg 失败推出的「上游抖动」**作废**——该现象同属本缺陷，
与上游无关。

## 三、对 leader 自己 `200500` §三判据的再更正（部分保留，非全废）

巡检称「用 run 时长或网关 item id 也区分不出来」。leader 受理其主旨——**两个候选假说
（脏会话 / 上下文膨胀）本来就都不是根因，真正的判别量是响应体长度**——但需精确到一处，
否则会连带废掉一条仍然有效的区分：

**run 时长区分的是失败在链条中的\*位次\*，不是根因。** 按新因果链复核 leader 可读的时长数据：

| run | 时长 | 新链条下的位次 |
|---|---|---|
| `cd0f9fa3` | 698s | **一阶**：自身长输出触发窗口缺陷，注入伪 `response.failed` |
| `f3a6cc15` / `8969232a` / `99a96c03` | 35s / 35s / 28s | **二阶**：开局回传上一轮被污染的 reasoning item → 400 |

即时长确实把一阶与二阶分开了，且分得对；它只是**不能识别一阶的成因**。
故更正后的判据应为：**时长定位次，响应体长度定根因**，两者互补，不是替代。
`200500` §三里把「数百秒死于中途」标为「上下文/流的问题」的**所指**错误，此处一并更正为
「自身输出长度触发网关窗口缺陷」。

## 四、对待裁决项的机械影响（leader 仍不选方向）

### 4.1 A / C 的前提不仅不成立，而且是**自再生**循环

`200500` §4.1 写「弃用脏会话是 A/C 的前置条件」。按新根因该说法**不充分**：弃用脏会话只把状态
清回一阶起点，而**下一次长输出会立即重新制造它**。缺陷确定性、发生在本机、与档位无关，
工程 agent 输出天然长，命中率接近 100%。故 A 无「恢复」可等，C 换档不触及变量——
**两者都是循环，不是概率问题**。此条 supersede `200500` §4.1。

### 4.2 B（切 claude）的免疫是**结构性**的，且这改变了既有的因果判据

Claude 终止帧 51 字节恒在窗口内。故若裁为 B 并成功，**不再受 `191500` §三第三行
「单次成功不确认因果」的限制**——那条限制是为「阵发」假说设的，而阵发假说已随 §二作废。
B 的成功属机制可预期，可直接记为「已绕开该缺陷」（但仍不等于缺陷已修复）。

### 4.3 一条尚未进入裁决面的范围事实：**只切 `实现` 不足以覆盖剩余链路**

leader 实读 `multica agent list`：

| agent | id | model | lane |
|---|---|---|---|
| 规划 | `e9cf031c` | `codex/gpt-5.6-sol-xhigh` | codex |
| 实现 | `fd2d1252` | `codex/gpt-5.6-sol-xhigh` | codex |
| 验证 | `4f83bacc` | `codex/gpt-5.6-sol-xhigh` | codex |
| 调度 | `96798779` | `claude/claude-opus-5` | claude |
| 评审 | `a8512fa6` | `claude/claude-opus-5` | claude |

本 Issue 剩余链路是 **Coder → QA execution → Review Pass 2 → Implementation Gate**。
其中 **QA execution 是长输出任务**（r1 的 `qa/20260821-172100` 覆盖 25 条 BLOCKED probe），
`验证` 仍在 codex；`规划` 在 codex，若后续出 `PLAN_GAP` 亦需其回场。
故 B 若只切 `实现`，**同一缺陷会在 QA 段原样复现**。这不是建议扩大 B，只是标注：
B 的现有措辞是**部分覆盖**，裁决时应知其边界。leader 不代决切几个 agent。

### 4.4 生产网关变更不在本 Squad 授权内

改 `4096` 常量或向上游提 issue 属**生产变更**，归 dongsjoa 主控自决；leader 不代决、不重复升级、
不 @ 老板、不碰网关配置。

## 五、一处必须消除的记账歧义：本 Issue 有两个不同的「option B」

- **B-brief**（`87b7b2a9` 19:41 提出）＝压小派单 brief 降低单轮上下文；
- **B-claude**（`62245023` 20:03 提出，本轮沿用）＝`实现` lane 暂切 claude。

`leader/20260822-200500` 中 §二讨论的是 **B-brief**，§4.3 讨论的是 **B-claude**，同名不同物，
读者极易误并。本分录起一律使用带后缀的名称。

并按新根因给 B-brief 一个终局定性：`200500` §二判其「未获执行」仍属实（35s 秒挂于二阶，
brief 大小没机会起作用）；但更根本的是——**缺陷由\*输出\*长度驱动，压缩\*输入\* brief 本就
不作用于该变量**，故 B-brief 不只是未被测试，而是**指向了错误的自变量**。该路线可关闭，
不必再安排一次「公平测试」。

## 六、不变项

`green_dispatch_gate` CLOSED（(a)(b)(c) 一字不改）、`red_intake` NOT ACCEPTED 三条理由、
`write_set_precheck` CLEAN、加绑条件 2 的唯一受理证据（addendum `3812652e`）、
Plan Gate artifact `61d54ad3…`、R1–R5 与 A/B 硬约束、Publication 边界，全部不动。
本根因与 `20260822-174500` 的 RED 裁定**正交**（后者判的是树内容与提交面，与运行时无关）。
