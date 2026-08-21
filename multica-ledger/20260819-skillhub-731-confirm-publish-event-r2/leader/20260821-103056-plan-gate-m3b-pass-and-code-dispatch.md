---
ts: 20260821-103056
agent: leader
action: plan gate re-run on M3b / PASS / batched code dispatch
tree: e786a5e3e2cdd25f43e373f0f25200c9df5ea168
verdict: PASS
---

Plan 段闭合。**M3b（`3b42b8ff`）plan gate PASS**，validator 与审批人约束面**两侧都过**。
自 `20260821-092115` 起的三轮漂移死锁到此结束，Code 一次性派出。

本分录同时记录一段**中间态**，因为它解释了为什么账本里有两份 M3，也因为其中一条
**是必须带到 Publication 的硬提示**。

## 1. 中间态（据实，非追责）

本席在 `10:28` 对 **M3**（`6f5b4f2b`，ts `101027`）跑过一次 plan gate：
validator **PASS**（收据 `leader/20260821-102844-plan-gate-m3-receipt.json` 未入账，
因该 artifact 已被 supersede），但相对 `20260821-102535` §3 的约束面有两处阻断——
R8 文件未进生效 `write_set`；F3 公开段含 R1 可复现细节。

本席尚未落账，**规划 已自行推出 M3b 并把两处都闭合**。M3 写于 `10:10`、裁定发于 `10:23`，
它对 (b) 是盲的；这不是 validator 失败，也不是 规划 的过失。**不记 RETURN**，
按 supersede 处理——M3 从未成为生效计划。

## 2. validator 结果（机械）

收据：`leader/20260821-103056-plan-gate-m3b-receipt.json`

| 项 | 值 |
|---|---|
| `result` | **PASS**，`errors` 空 |
| artifact sha256 | `ed9f486034077ef70a5f1f264042bd95182e08514d07123250867fd1a96c4077` |
| inventory sha256 | `7644ce1ef36bf95c4849b309f589f545b82ae2580144ee0ef43a1a0d3fc27820` ✓ 绑生效 inventory |
| Plan Run / context | `41eaa5fd-6ef3-4627-ab73-db6c2c2aa5aa` / `01a02347-46f7-7962-81ec-4118401867e2` |
| provenance 隔离 | ≠ discovery Run `f7cf1b2d…` / context `01a0233a…` ✓ |
| `artifact.revision` | 5 ✓ |
| validator sha256 | `a7100d86232e21df4f032d07ecbc7e055f2353020be0858230db131a47ca4892` |
| warnings | 仅 D1 常设警告（inventory 非行为面上界，Reviewer 须独立追踪） |

`validate_gate.py:523` 的 digest 阻塞解除；`:201-219` 的 `expected_before` 矛盾随新矩阵消解。

## 3. 审批人三条硬约束：逐条核验

| 约束 | 核验 | 结果 |
|---|---|---|
| ① 范围为 (b)，R4/R3/R1 移出且不预留写集 | `write_set` 八项无 `skillhub-search`、无 `SubscriptionMetadataAccessPolicy`、无 `MeController`/`MySkillAppService`/`JpaMySkillQueryRepository`；禁写集显式含 policy / event / search / DB / workflow 与生产 `SkillSubscriptionService.java`；`published-search-control` 仅保留 inventory 要求的查询可见性控制 probe，且 M3b 明写该 probe **不是对 R4 的关闭声明** | **过** |
| ② R8 绑定面 = `SkillSubscriptionServiceTest` 一个文件，`messageCode()` 不得替换为 `hasMessage()` | 该文件已进生效 `write_set` 与同一 work unit；契约段明令「不得使用 `hasMessage()`、`getMessage()` 或仅断言异常类型」；生产 `SkillSubscriptionService.java` 仍在禁写集 | **过** |
| ③ F3 正文按 R2 重写但对 R1 技术细节静默 | `pr-body-draft-m3b.md` 只留「Retained subscription rows are also consumed by existing read paths」这一层事实；**无端点、无类名、无策略名、无字段清单、无 `visibility: "PRIVATE"` 例证**；内部注记整节已删 | **过** |

本席另核 `work_partition`：单 unit `hd28-targeted-remediation`，`depends_on` 空，
`write_set` 与顶层逐项一致（含新加的测试文件），无跨 unit 冲突面；
`no_split_rationale` 成立——R5 与 R8 必须同改一棵业务树，分批只会多作废一次 Gate。
Council 提名 required=true 且各 lens 绑定了 path/invariant/must_answer，名额与锚点齐备。

## 4. 一条必须带到 Publication 的硬提示

**账本里现在有两份 F3 草稿，只有一份可用。**

| 文件 | 状态 |
|---|---|
| `planner/20260821-101027-pr-body-draft-m3.md` | **已 supersede，禁止用于发布**——其公开段含 R1 的入口、类名、字段清单与 `visibility: "PRIVATE"` 例证 |
| `planner/20260821-102507-pr-body-draft-m3b.md` | **唯一生效版本** |

append-only 意味着前者会一直留在分支上。R1 的披露通道尚未确定，
误取前者推上游即完成一次**不可逆的公开披露**。Publication 侧按文件名取用、
不得凭「找到一份 pr-body-draft」就复制。此条写进本分录，
是为了让它出现在 Review Gate 与 Publication 的必查项里，而不是只活在本次对话里。

## 5. 派 Code：一次性，批量

生效计划：`planner/20260821-102507-plan-r1-m3b-scope-ruling.md`
（叠加 `20260821-101027-plan-r1-m3.md` 中未被 supersede 的 F1/F2 方案面）。
本轮闭合 **R5 / R2 / R8**，外加 M3 已顺手做对的 **R7**。

实施边界（本席校验，不得自行放宽）：

1. **R5**：`NamespaceRepository.findById` 的窄 `try/catch (RuntimeException | Error)`，
   只包单次 `findById`，只记 `skillId`/`namespaceId`，随后**原样重抛同一 throwable**——
   不 swallow、不 wrap、不 return、不 dispatch；捕获范围不得包住 eligibility、JSON 或 dispatcher。
   publish 与 yank 各一条对称 probe，用真实 logger/ListAppender，断言 throwable identity、
   ID 在场、PII sentinel 缺席、dispatcher 零次。
2. **R8**：只改 `SkillSubscriptionServiceTest.java`，三条既有 denial case 上断言
   `messageCode()`。**禁止 `hasMessage()` / `getMessage()`**——`LocalizedDomainException`
   构造为 `super(messageCode)` 致两者当前恰好等价，那是实现细节，构造一变断言即静默失效，
   正好复现 R8 本身。保留各用例原有的 repository/count/event 零 mutation 断言。
3. **不变式**：非 null 时 `(recipient,eventType,title,payload,entityType,entityId)` 六元组
   相对 `23658e0f` 逐元素一致。控制组（owner / 两条 fan-out / search）是 `PASS → PASS`，
   **不得伪报为 RED**——inventory 已把它们的 `must_fail_old` 翻为 false，探针须与之一致。
4. **F1 文案冻结不动**：`You do not have access to this skill` / `你没有权限访问该技能`；
   properties 中撇号仍写 `''`；key 数量与位置不变。
5. **禁写集**：policy、event model/producer、payload、dispatcher、`skillhub-search`、
   DB/migration、`.github/workflows/**`、生产 `SkillSubscriptionService.java`。
6. RED → GREEN → regression 三段留痕，ledger 写 `coder/` 目录与 `index.md` 表尾。

## 6. tree 变更的级联（提前登记，届时不必重新溯源）

M3b 落地即改业务树，`e8aca2a4…` 冻结随之解除并换新 exact tree。自动作废并须重跑：
**Implementation Gate**、**Review Pass 1**（评审 已自认）。QA 全段相对新树重跑，
QA execution 必须与 discovery Run `f7cf1b2d…` 不同 Run。

`20260821-102535` §2 登记的五条 LOW：**R7 已由 M3/M3b 闭合**；
R9 / R11 在 write_set 内但本轮未纳入；R6 / R10 在 write_set 外。四条归属仍未裁，
不阻塞——Pass 1 重跑时 评审 会相对新树重新出具四维，是否 dimension-blocking 届时当场可见。

## 交接

- 状态：**Plan 段闭合，plan gate PASS**；Code 已派；exact tree 即将变更
- 产物：本分录 + `leader/20260821-103056-plan-gate-m3b-receipt.json`
- 工作分支：`20260819-skillhub-731-confirm-publish-event-r2` @ `3b42b8ff`
- 下一步：实现 一轮批量 Code（R5/R2/R8）→ 本席跑 Implementation Gate →
  QA Charter/Execution → Pass 1 重跑 + Pass 2 → Review Gate → READY
- 未解决：R6/R9/R10/R11 四条 LOW 归属（不阻塞）；R1 披露通道待老板裁定（已移出本链）；
  R4/R3 承接 Issue 草稿由本席在收线后备好交审批人，**先不开**
- 需要决策：否
