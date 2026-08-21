---
ts: 20260821-101142
agent: leader
action: Pass 1 artifact landing / provenance verification / R8 proposal check / correction to dongsjoa escalation
tree: 98df543544fd5b877d287e59bbd52bf07527e5ef
verdict: PASS
---

评审 补推 Pass 1 产物。`20260821-095600` 的**形式缺陷已闭合**，Pass 1 自即刻起是合法的
Review Gate 输入（待 Pass 2 齐备后合成 receipt）。本分录同时记三条**更正**，其中一条改变
上升给 千乘妍 的选项性价比，必须原样带到。

## 1. 落地核验（全部机械）

| 检查 | 结果 |
|---|---|
| 远端 head | `2479b0e8`，基于本席 `3d299678` |
| DCO | `Signed-off-by: xiaoyaner-multica-reviewer[bot]` **存在** |
| 变更面 | `+281 / −0`，两文件，**纯 ledger** |
| 非 ledger diff vs `3d299678` | **空** |
| exact tree vs `bfcb4fe5` 非 ledger diff | **空** → `e8aca2a4…` 冻结未破 |
| index.md | `+1 / −0`，新行按时间序落在 `094207` 与 `094709` 之间，未触碰他人行与表头 |
| 产物 front matter | `tree: e8aca2a43467dad0da969c182632a7b791f90621`、`verdict: RETURN` |

## 2. 冻结完整性：**其自述的取证路径不可复核**（据实记录，非指控）

评审 给的复核方法是 `diff <(git show 238d24ac:<path>) <(git show origin/…-r2:<path>)`。
**`238d24ac` 在本席克隆与远端均不存在**（`git cat-file -t 238d24ac` → `Not a valid object name`；
`branch -a --contains` → `malformed object name`）。按其自陈，该 commit 所在容器已回收、从未推送——
所以这条取证路径**对任何第三方都不可执行**，「零删除零改写」只能记为**自述**，不记为已核验。
不是质疑，是不把不可复核的东西写成已复核。

**可复核的替代检查，本席已做，且它才是承重的那个**：把产物内容与
**09:47:08Z 的交付评论 `fc53490a`** 对比——该评论是平台侧记录，比本次推送早 21 分钟，
评审 无法回改。逐项比对：

- R1–R11 **十一条的编号、类型、严重度全部逐条吻合**
  （R1 `INVARIANT_VIOLATION`(SECURITY)/HIGH/继承；R2 `PLAN_GAP`/MEDIUM；R3 `IMPLEMENTATION_DEFECT`/MEDIUM；
  R4 `PLAN_GAP`(SECURITY 次级)/MEDIUM；R5 `PLAN_GAP`/MEDIUM；R6/R7 `IMPLEMENTATION_DEFECT`/LOW；
  R8 `TEST_GAP`/MEDIUM；R9 `TEST_GAP`/LOW；R10/R11 `IMPLEMENTATION_DEFECT`/LOW）；
- 四维一致：Intent RETURN / Invariant·Security RETURN / Evidence RETURN / Exact Tree PASS；
- **Overall RETURN** 一致；tree 绑定一致；「无 uncovered ID」一致。

即：**推送前后没有发生结论层的事后修改**。这一点成立，`238d24ac` 是否可达就不影响 Gate 采信。
附录 A 显式自标为冻结后追加且声明不改 §1–§5，与上述比对结果相容。

## 3. provenance：按其邀请独立比对 `source_task_id`，并更正一处

评审 自己写明「别采信这张表」，请本席用各方交付评论的 `source_task_id` 独立比对。已照做：

| 交付评论 | 时间 | 角色 | `source_task_id` |
|---|---|---|---|
| `fc53490a` | 09:47:08Z | 评审 Pass 1 | **`693d946c-f84c-4ce1-b66a-c90fd8324dab`** |
| `e8c054b1` | 09:18:33Z | 实现 | `23804a53-0724-4e60-b1c9-4fc891fb8f81` |
| `737cad32` | 09:51:02Z | 验证 inventory | `f7cf1b2d-b27d-4e8d-86f0-df62975c02a2` |
| `aacece17`/`aa0394a3`/`6cbae384` | — | 规划 | `d83deada…` / `3b3dfa72…` / `6168a879…` |

Pass 1 run **与平台记录逐位一致**，且与 实现 / 验证 / 全部 Plan Run **两两相异**。
`20260821-095600` §3 留的那项「无法核验」**现予核验通过**，Pass 1 的 Run 隔离成立。

**更正（本席核出，与 评审 自述不符）**：本次补推**不是同 task**。补推交付评论 `5ef6ae93`
的 `source_task_id` 是 **`a0258876-9e69-4dda-8f71-d2c78d6897c2`**，≠ `693d946c…`。
其本地会话或为同一 context，但 Multica task 确已换。

后果，说清楚以免被误读：

- 对 **Pass 1 本体无影响**——§1–§5 的 blindness 由 `693d946c…` 那一 Run 与 `bfcb4fe5` 的树结构共同保证；
- 但 **附录 A 不 blind**：它写于本席 `20260821-095600` 分录之后、且在 验证 supersession 之后。
  这正当——附录 A 被显式限定为「对既有 finding 的修复面陈述，不是新的审查结论」，
  且它回答的就是本席提的问题。**只是必须按不 blind 登记**，不得在 Pass 2 里被当作盲审结论复用。

## 4. R8 方案 A：机械可行性核验通过

评审 提案（不自行扩写 write_set，交本席校验，做法正确）：

```java
.isInstanceOf(DomainForbiddenException.class)
.extracting(e -> ((LocalizedDomainException) e).messageCode())
.isEqualTo("error.skill.subscription.noPermission");
```

逐项核：

| 项 | 核验 |
|---|---|
| `messageCode()` 存在且 public | `LocalizedDomainException.java:18` `public String messageCode()` ✓ |
| 继承链成立 | `DomainForbiddenException extends LocalizedDomainException`；后者 `implements LocalizedMessage` ✓ |
| 跨模块可见 | 生产类在 `skillhub-domain`，测试在 `skillhub-app/src/test`，该测试**已 import** `com.iflytek.skillhub.domain.shared.exception.DomainForbiddenException`，同包再引入 `LocalizedDomainException` 无新增依赖 ✓ |
| 外溢面 | **仅 `SkillSubscriptionServiceTest.java` 一个测试文件**，生产代码零改动，`SkillSubscriptionService` 仍留在禁写集 ✓ |
| 闭合性 | 改生产字面量 → 该测试 RED；改 bundle 键 → `SubscriptionMessageBundleTest` RED。两端各一哨兵 ✓ |

**一条给 实现 的实施注意**（本席核出，不在提案内）：`LocalizedDomainException` 的构造函数
`super(messageCode)`，故 `getMessage()` 恰好等于 messageCode。**不得改用 `hasMessage()` 绑定**——
那是绑到实现细节，`messageCode()` 才是显式契约，将来若构造函数不再把 code 传给 `super`，
`hasMessage()` 版本会静默失效，正好复现 R8 本身。

本席只判**机械可行**。write_set 修订本身仍须由 规划 写进 M3、经 plan gate、并由 千乘妍 审批；
本席不自行放宽。**不重新 @ 规划**（其 M3 Run 已于 10:04 派出，重复 mention 会并发双跑），
本提案在 plan gate 复跑时由本席并入核对。

## 5. 对 `20260821-095600` §8 的更正：(c) 的真实含义

评审 指出、本席**已独立核实**：`schemas/review-gate.schema.json` 的
`properties.overall.enum` **恰为** `["PASS", "RETURN", "UNVERIFIED"]` —— 三态，
**没有「PASS with declared exception」**。

本席 `20260821-095600` §8 把选项 (c) 写作「作为『已知未决』写入 F3 正文并据此进入 READY」，
**该措辞不准确**，会让审批人以为披露能让 Gate 转 PASS。据实更正：

> 披露改变的是维护者掌握的信息，**不是树的行为**。R4 在 (c) 下仍然 OPEN，
> Invariant/Security 维度不会因 F3 正文多一段话而转 PASS。且 评审 已明确表态
> **不会为迁就 (c) 调整 verdict**——这是其语义裁量，本席不代改、不施压。
> 故 (c) 的真实含义是：**由审批人在 Gate 之外显式接受一条 OPEN 的 MEDIUM finding 进入 READY**。

三选项的机械后果（评审 陈述，本席核对无误，均不构成本席倾向）：

| 选项 | 机械后果 |
|---|---|
| (a) 扩范围 | R4/R3 进入可修范围，Invariant/Security 可清；**代价：树必变，Implementation Gate PASS 与 Pass 1 同时作废，整轮重来** |
| (b) 另开 Issue，HD-28 收敛为 R5/R2/R8 | 三条均可在修订后的 write_set 内闭合；**唯一能让四维在一轮修复后全部到 PASS 的路径** |
| (c) 已知未决 + 进 READY | Review Gate 不会给 PASS；需审批人在 Gate 之外显式接受 OPEN finding |

## 6. 未变的项

- exact tree `e8aca2a4…` 继续冻结；Pass 2 不开，等 QA 产物齐备后另起 Run；
- ledger 键名 6 行的独立性折扣继续挂在 Pass 2；
- M3 落地、tree 变更后本 verdict 自动失效，Pass 1 须重跑——评审 已自认这一点，正确；
- Plan 段仍待 M3；`validate_gate.py:523` 的 digest 阻塞未解除。

## 交接

- 状态：Pass 1 形式与实质均已成立（Overall RETURN）；`EVIDENCE_GAP` 已闭合；Plan 段待 M3
- 产物：本分录
- 工作分支：`20260819-skillhub-731-confirm-publish-event-r2` @ `2479b0e8`
- 下一步：规划 出 M3（含 R8 方案 A 的 write_set 修订）→ 本席复跑 plan gate → 一轮批量 Code
  → QA Charter/Execution → Pass 2 → Review Gate
- 未解决：`20260821-095600` §8 四项待 千乘妍 裁定，**其中 (c) 的含义已按本分录 §5 更正后重新上报**
- 需要决策：是（更正后的 (a)/(b)/(c) 与 R8 write_set 修订）
