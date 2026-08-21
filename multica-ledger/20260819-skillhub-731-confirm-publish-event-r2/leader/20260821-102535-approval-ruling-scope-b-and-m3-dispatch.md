---
ts: 20260821-102535
agent: leader
action: record approver ruling (scope b / R8 plan A / R1 out-of-chain) and dispatch M3 with hard constraints
tree: 9e93dd098a635b44e85c9416ac57482753defae5
verdict: —
---

千乘妍 于 `20260821-102312` 裁定完毕，`20260821-095600` §8 四项**全部有结论，无待决项**。
本分录把裁定固化为链上硬约束，并据此派 规划 出 M3。本席不复述其理由，只登记**机械后果**。

## 1. 裁定登记（原样，不改写）

| 项 | 裁定 | 对本链的机械后果 |
|---|---|---|
| 范围 | **(b)** —— 另开 Issue，HD-28 按原范围收线 | exact tree 冻结面不变；Implementation Gate PASS 与 Pass 1 **不作废**（作废只由 M3 落地后的 tree 变更触发）；HD-28 收敛为 **R5 / R2 / R8** |
| (a) | 否 | R4 需动 `skillhub-search`（禁写集）、R1 不在本树差分内 |
| (c) | 否 | 审批人明确**不接受**在 Gate 之外收一条 OPEN finding |
| R8 | 采**方案 A** | write_set 增 `SkillSubscriptionServiceTest.java` 一个测试文件；生产零改动 |
| R4 / R3 | 转独立 Issue 承接 | **移出本链**；M3 不得为其预留写集 |
| R1 | 披露通道由老板定，已单独上报 | **移出本链**；不阻塞任何 Stage |
| F3 正文 | 按 R2 重写，但对 R1 技术细节静默 | 只写「订阅行同时被其他读路径消费」这一层事实 |
| 承接 Issue 草稿 | 本链收线后备好交审批人，**先不开** | 待 R1 披露通道确定后与之一并处理 |

审批人自述其 R4 / R1 取证系从 **live 上游 `iflytek/skillhub@main`** 现场复核，非采信任一方自述。
本席不复核审批人的语义裁量，只据其结论路由。

## 2. 本席核出的一处覆盖缺口（不阻塞，须显式登记）

裁定处置了 R1 / R2 / R3 / R4 / R5 / R8 **六条**。Pass 1 共 **十一条**，
`R6 / R7 / R9 / R10 / R11`（全部 LOW）**未被裁定文本触及**。本席不代为裁定，只记机械事实：

| finding | 落点文件 | 相对**当前** write_set |
|---|---|---|
| R7 | `NotificationEventListener.java` | **在内** |
| R11 | `NotificationEventListener.java`（`bodyWithSkill()` 二次 `findById`，`:65/88/116/134/168`） | **在内** |
| R9 | `SubscriptionMessageBundleTest.java`（若修法不外溢到 Boot 装配切片测试） | **在内（条件成立时）** |
| R6 | 索引侧沉默在 `PostgresSearchRebuildService`（`skillhub-search`，禁写集） | **在外** |
| R10 | `SubscriptionRecipientEligibility.java`（`domain/social`） | **在外** |

即：R7 / R11（及条件成立时的 R9）**可在不扩范围的前提下于本轮闭合**——tree 本来就要因 R5/R2/R8 变，
顺手闭合不额外付代价；R6 / R10 与 R4 / R3 同属外溢面，若不裁定则应同去承接 Issue。

**本席不自行扩写 write_set**，M3 亦不得为 R6/R7/R9/R10/R11 预留写集。
处置口在两处，任选其一即可，无须现在答复：
(i) 审批人补一句纳入或排除；(ii) 不补——则 M3 落地后 tree 变，**Pass 1 自动失效并重跑**，
届时 评审 会相对新树重新出具四维，五条 LOW 是否 dimension-blocking 由其语义裁量当场给出。

登记此项的原因：审批人「(b) 是唯一能让四维在一轮修复后全部到 PASS 的路径」这一判断，
**以五条 LOW 不 hold 住任一维度为前提**。该前提当前未经 评审 确认。不确认不影响本轮推进，
但若 Pass 1 重跑时某维度因 LOW 仍为 RETURN，那不是新增阻塞，是本前提未成立——先记在这里，届时不必重新溯源。

## 3. 派 规划 出 M3：硬约束（本席校验，不得自行放宽）

M3 **一次性冻结**，不再分批（同一漂移陷阱已付三轮代价）。须含：

1. `subject.discovery_inventory_sha256` → **`7644ce1ef36bf95c4849b309f589f545b82ae2580144ee0ef43a1a0d3fc27820`**
   （`validate_gate.py:523` 当前必然 RETURN，直到此项绑新值）；
2. `expected_before` 随新矩阵校准——`published-owner-control` / `published-search-control`
   两条控制组不再被 `must_fail_old` 逼出 `FAIL` 探针；
3. **R5**（`PLAN_GAP`：新增 WARN 覆盖不到其所声称的事故类）；
4. **R2**（`PLAN_GAP`：F3 正文把存量订阅行建模为「仅影响通知」不成立）；
5. **R8 方案 A** 的 `write_set` 修订：**只加 `server/skillhub-app/src/test/java/com/iflytek/skillhub/domain/social/SkillSubscriptionServiceTest.java` 一个测试文件**；
6. `artifact` 自指四字段（path / revision → 4 / run_id / sha256）。

审批人指定的三条硬约束，原样落入 dispatch：

- **① 范围为 (b)。** R4 / R3 / R1 明确移出本链，**不得在 M3 中为其预留写集**，
  不得以「顺带」名义把 `skillhub-search`、`SubscriptionMetadataAccessPolicy`、
  `MeController` / `MySkillAppService` / `JpaMySkillQueryRepository` 纳入任一集合。
- **② R8 的绑定面就是 `SkillSubscriptionServiceTest` 一个文件。**
  断言用 `messageCode()`，**不得替换为 `hasMessage()`**——`LocalizedDomainException`
  构造为 `super(messageCode)` 致两者当前恰好等价，那是实现细节；构造一变断言即静默失效，
  正好复现 R8 本身。生产零改动，`SkillSubscriptionService` 仍留禁写集。
- **③ F3 正文按 R2 重写，但对 R1 技术细节保持静默**，只写「订阅行同时被其他读路径消费」
  这一层事实。不得出现链路、类名、端点或可据以复现的描述。

`10:04` 那次 M3 dispatch **早于本裁定**，其时 (a)/(b)/(c) 未决，故不含上述三条。
本分录**取代**该次 dispatch 的约束面；以本分录为准。

## 4. 未变的项

- exact tree `e8aca2a4…` 在 M3 落地前继续冻结；
- Pass 1（Overall RETURN）在 tree 变更前继续有效，变更后自动失效并须重跑——评审 已自认；
- 附录 A 按**不 blind** 登记（见 `20260821-101142` §3），Pass 2 不得当盲审结论复用；
- ledger 键名 6 行的独立性折扣继续挂在 Pass 2；
- QA execution 必须与 discovery Run `f7cf1b2d…` 不同 Run；
- Publication 不在工程链内，工程链终点是 exact subject tree READY。

## 5. 本席自留项

本链收线后备好 **R4 / R3 承接 Issue 草稿**交审批人，**不自行开 Issue**——
待 R1 披露通道确定后一并处理，避免分两次打扰同一批维护者。

## 交接

- 状态：范围裁定完成，(b) 生效；Plan 段待 M3；`EVIDENCE_GAP` 已闭合；exact tree 未动
- 产物：本分录（裁定登记 + dispatch 约束面）
- 工作分支：`20260819-skillhub-731-confirm-publish-event-r2`
- 下一步：规划 出 M3（§3 六项 + 三条硬约束）→ 本席复跑 plan gate → 一轮批量 Code
  → QA Charter/Execution → Pass 1 重跑 + Pass 2 → Review Gate → READY
- 未解决：R6/R7/R9/R10/R11 五条 LOW 的归属（§2，不阻塞）
- 需要决策：否（本轮已由审批人裁定完毕）
