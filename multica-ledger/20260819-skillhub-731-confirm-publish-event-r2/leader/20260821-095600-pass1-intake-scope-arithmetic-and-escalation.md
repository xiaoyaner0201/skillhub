---
ts: 20260821-095600
agent: leader
action: Review Pass 1 intake / artifact-absence finding / independent corroboration / scope arithmetic / dongsjoa escalation
tree: 95750507f9594ff5ff7effd72e58813977ffc772
verdict: RETURN
---

评审 报 Pass 1 `BLIND_BEHAVIOR_AUDIT` 完成、Overall RETURN。本席**不采信自述**，逐条独立取证。
结论分两层：

1. **Pass 1 目前不是合法 Gate 输入**——声称的产物不在账本内，无 tree 绑定，无法被 Review Gate 消费；
2. **但其承重结论经本席独立复核全部成立**，其中 R4 是一条真实的、机械可证的新增 PRIVATE 落盘面。
   两者不冲突：证据成立与产物合规是两件事，缺哪个都不能推进 Stage。

## 1. 产物缺失（机械事实，非质疑其工作）

`reviewer/20260821-094307-review-pass1-blind-behavior-audit.md` **不在工作分支上**。两次取证：

| 时刻 (UTC) | 远端 head | reviewer/ 目录 | index.md |
|---|---|---|---|
| 09:45 | `3ce59587`（本席上一提交） | 仅 HD-7 三份 | 无新行 |
| 09:51 | `bd2ba3b9` | 仍仅 HD-7 三份 | 新行来自 验证，非 评审 |

期间确有一次推送，但作者是 `xiaoyaner-multica-qa[bot]`（验证 的 superseding inventory），
与 评审 无关。故不是我读得太早的竞态——**产物确实未推送**。

后果，逐条：

- `review-gate.v1` schema `required` 含 `pass1_source_first`（`schemas/review-gate.schema.json:7`），
  没有产物就产不出合法 receipt；
- 账本纪律：结论必须落为 append-only 分录并绑 tree。评论正文不是产物，
  **自由文本 RETURN 不能替代产物与原始证据**——这条我对每个角色一视同仁；
- Pass 2 以 Pass 1 为输入，产物不落地，Pass 2 无锚。

各 Agent 容器互不可见，你的只读工作树在你那侧；**必须 commit + push 到
`20260819-skillhub-731-confirm-publish-event-r2` 我才看得到**。请补推，附 DCO sign-off。
本分录不因此否定你的结论，只是**在产物落地前不绑定任何 Gate**。

## 2. 承重结论的独立复核（全部成立）

本席在你的产物之外、直接对树取证。以下每条都是我自己跑出来的，不是复述：

| 主张 | 我的取证 | 结论 |
|---|---|---|
| 审查主体 19 文件 / +1054 | `git diff --shortstat d2403bb5 bfcb4fe5 -- ':(exclude)multica-ledger'` → `19 files changed, 1054 insertions(+), 8 deletions(-)` | **逐字成立**（你正确排除了 ledger；含 ledger 是 72 文件 / +7693） |
| R4 发射点为本 PR 新增 | `git diff d2403bb5 bfcb4fe5 -- '*SkillReviewSubmitService.java'` 中 `+ eventPublisher.publishEvent(new SkillPublishedEvent(` 与 `+import ...SkillPublishedEvent` | 成立 |
| 该发射点只在 PRIVATE 可达 | 同方法内 `if (skill.getVisibility() != SkillVisibility.PRIVATE) { throw ... }` 前置于发射 | 成立 |
| 消费链到达索引 | `SearchIndexEventListener.onSkillPublished` → `searchRebuildService.rebuildBySkill(...)`，`@TransactionalEventListener(AFTER_COMMIT)` + `@Async` | 成立 |
| sink 无 index-time 可见性过滤 | `PostgresSearchRebuildService` 全文 `grep -nE "PRIVATE\|visibility\|Visibility"` **仅 1 处命中**，且是 `skill.getVisibility().name()` 写入文档；`toDocument` 唯一的早退是 namespace 缺失 | 成立 |
| 查询侧仍安全（严重度上界） | `PostgresFullTextQueryService` 谓词 `AND (d.visibility = 'PUBLIC' ...OR (d.visibility = 'NAMESPACE_ONLY' AND d.namespace_id IN :memberNamespaceIds))`，PRIVATE 无放行臂 | 成立 |
| R5 `namespace == null` 不可达 | `V2__phase2_skill_tables.sql:7` `namespace_id BIGINT NOT NULL REFERENCES namespace(id)`，无 `ON DELETE` | 成立 |
| R3 `wasPublished` 为常量 | `yankVersion` 内 `if (version.getStatus() != PUBLISHED) throw`，唯一发射点硬编码 `..., true)` | 成立 |
| R8 两端未绑定 | 生产侧 `SkillSubscriptionService.java:54` 抛 **字符串字面量** `"error.skill.subscription.noPermission"`；`SubscriptionMessageBundleTest:12` 自持同名字面量常量；`SkillSubscriptionServiceTest` 三处否决点**只断言 `isInstanceOf(DomainForbiddenException.class)`**，无 `messageCode()` | 成立 |
| R1 本树未引入未加重 | `git diff --name-only d2403bb5 bfcb4fe5 \| grep -iE "myskill\|mecontroller\|/me/\|star"` **零命中** | 成立 |

**R4 是本轮最重的一条，且不是推测。** 一个宣称收紧 PRIVATE 暴露的改动，把 PRIVATE skill 的
displayName / summary / keywords / searchText 静默写进 `skill_document`，改动与测试均未提及。
你同时把严重度**自己压住**（查询侧无泄露，故非 HIGH），没有借安全名义抬价——这点记明。

R8 尤其值得点出：F1 存在的全部理由就是防止裸 key 外泄，而现有测试恰恰无法拦住导致裸 key 的
那一类改动。这是「绿而未验」，不是「已验证」。

## 3. blindness 评估（按我实际下发的约束，不是事后加码）

`20260821-092115` 我下发的 Pass 1 约束原文是：不读 `coder/20260821-091620-red-green-regression.md`、
不读任何 QA 产物、Run/context 与 Coder 及后续 QA execution 相异。据此逐条对表：

| 你自陈的污染 | 是否违反我下发的约束 |
|---|---|
| 只读工作树 detached 在 `bfcb4fe5`，Coder 分录在后续 `b80982e8` 才进树 | **未违反**，且 blindness 由树结构保证，比口头承诺强 |
| grep 未排除 `multica-ledger/`，约 6 行 planner/leader 分录**键名位置**进入上下文 | 未违反（非 Coder/QA 产物），计**独立性折扣** |
| `jq '.council_nomination'` 连带读到 rationale 与 path/invariant id | **未违反**——`must_answer` 本就出自契约、Pass 1 必须逐条作答，读该块是履约所需，我从未禁止；Plan 正文未读 |

即：**无 blindness 违规**，两处折扣已登记，Pass 2 复核时一并计入。
主动自陈污染而非隐去，是正确的做法，此处记为链内正面事实。

尚**无法核验**的一项：Pass 1 的 Run/context 是否与 Coder 及后续 QA execution 相异——
该字段在未推送的产物里。产物落地后本席补验。

## 4. 主体口径分歧（本轮真正的结构问题）

| 面 | 声明主体 | 出处 |
|---|---|---|
| 契约 / inventory | `23658e0f`（HD-7 之后） | artifact `subject`、inventory `subject.base_commit` |
| Pass 1 审查 | `d2403bb5`（upstream base） | 本轮 评审 自述 |

两者**都自洽，但回答的是不同问题**：契约主体界定「HD-28 改了什么」，Pass 1 主体界定
「上游最终收到的树里有什么」。工程链终点是 exact subject tree READY——READY 认证的是**要发出去的树**，
所以 Pass 1 取全量差分有其道理；而 HD-28 的**修复范围**是 Issue 与已获批准计划冻结的 R2 增量。

同一时刻 验证 已就上一轮 `EVIDENCE_GAP` 出 superseding inventory
（`qa/20260821-094709-behavior-inventory-superseding.json`，sha256 `7644ce1e…7820`），
**保留 subject `23658e0f`**，把 8 项 `must_fail_old` 相对该 subject 重新推导，4 项翻为 `false`
（含我点名的两条阻塞项与两条同族 fanout 项）。即 验证 的语义裁定选了「主体不动、字段收敛」。
该产物本席尚未开 discovery gate，待其正式交付后另行复跑；**先行提示：`bd2ba3b9` 无
`Signed-off-by`，DCO 缺失，请在我复跑前补上。**

这不构成 验证 与 评审 冲突：一个收敛契约字段，一个扩大认证视野。**但它把一个决策逼到台面上**——
认证范围内存在修复范围外的缺陷时怎么办。这不由本席代答，见 §6。

## 5. 范围算术（机械，决定谁能动手）

生效 write_set 五个业务文件 + `coder/` + `index.md`；禁写集含 `skillhub-search`、
`SubscriptionMetadataAccessPolicy`、`SkillSubscriptionService` 等。逐条落位：

| Finding | 需触碰 | 是否在 write_set | 可否在 HD-28 内修 |
|---|---|---|---|
| R5 `PLAN_GAP` | `NotificationEventListener.java` | **是** | 可 |
| R2 `PLAN_GAP` | F3 PR 正文草稿（planner 产物） | 不涉业务树 | 可 |
| R8 `TEST_GAP` | 需引用生产侧抛出的 code：`SkillSubscriptionServiceTest.java` 或 `SkillSubscriptionService.java` | **均否**（后者且在禁写集） | **需先修订 write_set** |
| R3 `IMPLEMENTATION_DEFECT` | `SkillGovernanceService` / `SkillVersionYankedEvent` | 否，且属 HD-7 主修复 | **否** |
| R4 `PLAN_GAP`(SECURITY) | `skillhub-search` | 否，且在**禁写集**；producer 是 Issue 明令不得触碰的 `confirmPublish` 主修复 | **否** |
| R1 `INVARIANT_VIOLATION`(SECURITY) | `MySkillAppService` / `JpaMySkillQueryRepository` | 不在本树差分内 | **否** |

R8 的落点值得单说：生产侧是**内联字面量**、不是常量，所以「两端绑定」无法只在 write_set 内的
`SubscriptionMessageBundleTest` 完成，必然外溢一个文件。这是最小的一类 write_set 修订（仅测试面），
但仍是对已获批准计划的范围改动，不由本席自行放宽。

## 6. 树冻结耦合（必须先说，避免第四次返工）

exact tree `e8aca2a4…` 同时被 **Implementation Gate PASS** 与 **Pass 1 结论**绑定。
任何业务代码改动都会同时作废这两者。因此：

> **R5 / R8 及后续任何代码修复必须合并为一轮**，在契约再冻结之后一次性做完；
> 不得先补 R8 那"一行"再谈其余——那一行会让 Pass 1 白做。

## 7. 路由

- **评审**：补推 Pass 1 产物（含 Run/context provenance、DCO sign-off）。**不开 Pass 2**——
  Pass 2 需 QA 产物，而 QA 仍暂缓。你的 Overall RETURN 本席接受为实质结论，形式待产物落地后绑定。
- **规划**：R5 / R2 归你，连同 M3 一并处理；R8 需你提出 write_set 修订。
  **仍只冻结一次**：等 验证 的新 inventory 过 discovery gate 后，M3 一次性含
  `subject.discovery_inventory_sha256`、`expected_before` 校准、R5/R2 与 write_set 修订。
  同一漂移陷阱已付三次代价，不再分批。
- **验证**：superseding inventory 已见，补 DCO 后本席复跑 discovery gate。QA Charter 继续暂缓。
- **实现**：**不动手**。树冻结，等契约再冻结后一轮批量做。
- **上升 千乘妍**：R4 / R3 / R1 与 write_set 修订，见 §8。

## 8. 上升事项（Plan 范围决策，本席无权代决）

Pass 1 在**认证范围内**报出三条**修复范围外**的缺陷。本席只陈述机械事实，请裁定：

1. **R4**（MEDIUM，PRIVATE 新增落盘面，无查询可见泄露）——由 HD-7 的 `confirmPublish` 修复引入，
   已在 Draft PR #735 中。修它必须动 `skillhub-search`（禁写集）或主修复（Issue 明令不碰）。
2. **R3**（MEDIUM，`wasPublished` 死码致证据虚高）——同属 HD-7 面，不在 write_set。
3. **R1**（HIGH，SECURITY，`GET /me/subscriptions` 无可见性授权）——**完全继承自上游 base，
   本树未引入未加重**（已机械核实）。这是 `iflytek/skillhub` 上游的活体缺陷，
   其披露方式属外部动作，本席不代决；若需协同披露或涉法律面，请再上升老板。
4. **write_set 修订**：R8 的两端绑定必须外溢至少一个测试文件。

可选路径（不预设倾向）：(a) 扩 HD-28 范围并重开 Plan；(b) 另开 Issue，HD-28 按原范围收线；
(c) 作为「已知未决」写入 F3 正文并据此进入 READY。任一选择都会改变 F3 要向维护者陈述的内容——
R2 已经指出 F3 当前把存量订阅行建模为「仅影响通知」是不完整的。

## 交接

- 状态：Pass 1 实质 RETURN 接受、形式待产物落地；`e8aca2a4…` 继续冻结；Plan 段待新 inventory 后一次性再冻结
- 产物：本分录
- 工作分支：`20260819-skillhub-731-confirm-publish-event-r2` @ `bd2ba3b9`
- 下一步：评审 补推产物 → 验证 补 DCO → 本席复跑 discovery gate → 规划 出 M3（含 R5/R2/write_set 修订）
  → 本席复跑 plan gate → 一轮批量 Code → QA → Pass 2 → Review Gate
- 未解决：§8 四项待 千乘妍 裁定；Publication 段（验收 5/6）由主控侧承接
- 需要决策：**是**，已按 Project 路由真实 @ 千乘妍；R1 若需协同披露再上升老板
