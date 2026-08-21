---
ts: 20260821-092115
agent: leader
action: implementation gate / PLAN_GAP finding on expected_before / review pass1 dispatch
tree: f025cd9ea0cb1979184104634612a4638aa2a670
verdict: PASS
---

Implementation Gate 对 exact subject tree `e8aca2a43467dad0da969c182632a7b791f90621` 的机械校验：
**PASS**。候选树对契约的符合度无瑕疵。

同时报出一项**与实现无关、位于冻结契约内**的缺陷：两条探针的 `expected_before: FAIL` 在机械上
不可能成立。这是 `PLAN_GAP`，退回 规划；候选树不受影响，不返修 实现。

## 1. 树与提交完整性

| # | 检查 | 实测 | 结论 |
|---|---|---|---|
| 1 | 远端 head == 声明 | `b80982e893aebe48c566bdb7a388770543d7fb6f` | PASS |
| 2 | 含 ledger 的 tree | `f025cd9ea0cb1979184104634612a4638aa2a670` | PASS |
| 3 | exact subject tree | `e8aca2a43467dad0da969c182632a7b791f90621` | PASS |
| 4 | 业务与 ledger 分两个 commit | `bfcb4fe5` 业务 / `b80982e8` 记账 | PASS |
| 5 | subject 之后非 ledger diff | 空 | PASS |
| 6 | `index.md` | +1 / −0 纯表尾追加 | PASS |
| 7 | worktree clean | 无未跟踪/未提交 | PASS |
| 8 | `git diff --check` | 通过 | PASS |

业务改动与记账分离成两个 commit 是好实践，Gate 因此能直接对 `bfcb4fe5^{tree}` 取证，
不需要从混合提交里剥离 ledger。

## 2. write_set 机械核验

`23658e0f → bfcb4fe5` 非 ledger 变更恰好 5 个文件，全部在 write_set 内，**无越界**：

```
M server/skillhub-app/src/main/java/.../listener/NotificationEventListener.java
M server/skillhub-app/src/main/resources/messages.properties
M server/skillhub-app/src/main/resources/messages_zh.properties
A server/skillhub-app/src/test/java/.../exception/SubscriptionMessageBundleTest.java
M server/skillhub-app/src/test/java/.../listener/NotificationEventListenerTest.java
```

禁写集逐项反查，全部零命中：`skillhub-search`、`.github/workflows/**`、
`SubscriptionMetadataAccessPolicy`、`SubscriptionRecipientEligibility`、`ApiResponseFactory`、
`SkillSubscriptionService`、DB/migration。临时差分 fixture 未进入候选树（5 个文件之外无新增）。

## 3. M1 落实核验

两个 bundle 均为**纯插入一行**（+1 / −0），位置 `:114`，紧随 `error.skill.lifecycle.noPermission`
之后，落在既有 `error.skill.*` 分区内，既有 186 行**一行未动、未重排**，186 → 187。

| 项 | 实测 | 结论 |
|---|---|---|
| EN | `error.skill.subscription.noPermission=You do not have access to this skill` | 与批准文案逐字一致 |
| zh | `error.skill.subscription.noPermission=你没有权限访问该技能` | 逐字一致 |
| key 唯一性 | 各文件 `^error.skill.subscription.noPermission=` 命中 1 次 | PASS |
| 未退回 manage/owner-admin | 是 | PASS |
| 未转义撇号 | `grep -n "'" \| grep -v "''"` 零命中 | PASS |

`SubscriptionMessageBundleTest` 两个方法名与契约 `entry.symbol` 逐字一致，
两条 `.isNotEqualTo(CODE)` 杀手断言均保留。`getMessage(CODE, null, CODE, locale)` 以 code 自身
为 default，key 缺失时必然返回裸 code —— 这使 RED 在结构上真实，不是靠断言措辞制造的假失败。

## 4. F2 落实核验（本 Gate 最关键的一项）

两处 `log.warn` 落在第 81、105 行分支，复用第 28 行既有 logger，模板与 `plan-r1-m1.md:43` 一致。

**控制流未变，这是必须逐字确认的点**：新增的 `if (namespace == null) { log.warn(...); }` 块内
**只有日志、没有 `return`、没有 `else`**，落下来仍调用同一个
`subscriptionEligibility.currentRecipients(skill, namespace, subscribers)` /
`yankedRecipients(...)`，实参一致。若这里多写一个 `return`，fail-closed 语义就会从「namespace 为
null 仍走过滤器并被拒」变成「直接跳过」，行为面完全不同。实测未发生。

日志实参仅 `skill.getId()` / `skill.getNamespaceId()`，无 subscriber ID、无 PII。

测试侧 `NotificationEventListenerTest` diff **删除行数 0**，纯追加；新增两个方法以
`subscriber-pii-sentinel` 作哨兵并断言 `doesNotContain`，且 `singleElement()` 要求恰好一条 WARN，
`verifyNoInteractions(dispatcher)` 守住 fail-closed。`captureLogs` 在 `finally` 里 detach appender
并还原 level，无测试间污染。这是可证伪的断言，不是描述性断言。

## 5. 审批人附加要求：已闭合

审批人要求 `published-search-control` 的探针须落在**查询可见性 sink**，不止 rebuild 调用计数。
契约内该 path 下有三条探针，其中两条正落在 `PostgresFullTextQueryService`：
`search-private-query-deny` 断言「Unauthorized caller receives no PRIVATE result IDs」、
「Portal query SQL contains no PRIVATE or platform-wide bypass」；
`search-public-namespace-control` 断言可见性边界不扩大。要求满足，不作为未决项下传。

## 6. `PLAN_GAP`：两条探针的 `expected_before: FAIL` 机械上不可能成立

契约内 6 条探针声明 `expected_before: FAIL`。其中 4 条位于候选实际修改的 `skillhub-app` 文件，
自洽。另 2 条不自洽：

| probe_id | entry symbol | 机械事实 | 结论 |
|---|---|---|---|
| `search-private-confirm-rebuild` | `SearchIndexEventListenerTest#skillPublishedEventShouldTriggerSkillRebuild` | `skillhub-search` 在 `23658e0f → bfcb4fe5` 之间 **diff 为空** | 零 diff 模块内的单元测试不可能 FAIL→PASS |
| `owner-nonowner-denied-control` | `NotificationEventListenerTest#onSkillPublished_shouldSkipWhenPublisherIsNotSkillOwner` | 该方法在 subject `23658e0f` **已存在**；该测试文件 diff 删除行数为 0；其覆盖的 owner notification 路径未被 F1/F2 触碰 | 旧树即 PASS，不可能 FAIL |

即：这两条在旧树上必然 PASS，而契约声明它们 FAIL。**缺陷在契约，不在候选树**——实现侧无可
返修之处，故 Implementation Gate 不因此 RETURN。

合理猜测是这两个值相对 HD-7 之前的基线（那时 `confirmPublish` 尚未发出事件）写就，未随 subject
收敛到 `23658e0f` 而更新；但基线口径属语义判断，不由本席代答，交 规划 裁定。

### 本席的漏检自陈

这两条在我此前两道 Gate 都应当被拦下而未拦：`20260821-081557` 我查的是探针**可执行性**
（class#method 是否真实存在）与闭包；`20260821-084606` 我查的是 M1 delta 的**最小性**。两者都
没有把 `expected_before` 与「该 symbol / 该模块相对 subject 是否有 diff」做交叉核对。这是我的
Gate 覆盖缺口，现补入常设检查项：**凡声明 `expected_before: FAIL` 的探针，其 entry 所在模块或
symbol 必须相对 subject 存在 diff，否则判 `PLAN_GAP`。**

### 为什么必须在 QA Charter 之前修

QA Charter 一旦写就即绑定当前 artifact digest。若先派 QA 再改契约，Charter 绑的就是陈旧
digest，QA Gate 必须重跑——正是 M1 那一轮已经付过一次代价的漂移陷阱。契约即契约，这条我在
`20260821-082539` 自己立过，此处一体适用。修正只涉两个枚举值，成本远低于一轮返工。

## 7. 路由

- **实现**：无返修项。候选树 `e8aca2a4…` 冻结，Implementation Gate PASS。
- **规划**（`PLAN_GAP`）：裁定上述两条 `expected_before`——改为 `PASS` 或给出基线口径依据；
  其余字段一字不动，追加 planner 分录声明新 digest。之后本席复跑 `run_gate.sh --kind plan`
  绑新 digest。
- **评审**（可即刻开工）：Review **Pass 1 `BLIND_BEHAVIOR_AUDIT`** 现在就派。exact tree 已冻结，
  而 Pass 1 按定义是**不以计划与 inventory 为锚**的独立重建，`expected_before` 的取值不进入其
  输入面；`PLAN_GAP` 的修复只动 ledger 内的 artifact，不动候选树，故不存在返工风险。
- **验证**：QA Charter **暂缓**，待新 digest 绑定后再派，避免 Charter 绑陈旧契约。

## 8. 传递给下游的约束（继续有效）

1. D1 warning：inventory 不是行为面完整性上界，Reviewer Pass 1 必须独立寻源，
   不得以 inventory 或本计划为覆盖上界；审批人另有明示——不因「已手工建模」放宽该义务。
2. Pass 1 必须 blind：不得读 Coder 的 `coder/20260821-091620-red-green-regression.md`、
   不得读任何 QA 产物，Run/context 与 Coder 及后续 QA execution 相异。
3. Council 三 lens 与 `must_answer` 按契约执行，含 `evidence-dissent` 反对席。
4. 不得以本地绿冒充 CI 绿。PR #735 仍 Draft，`pr-tests` 从未在该 head 运行；Coder 已如实
   自陈只报本地绿，未越界。该项由最终 Review Gate 复核。
5. Coder 报告的 1404 tests / 0 failures / 1 既有环境 skip（`RedisClusterIntegrationTest`）
   与差分 fixture 双树 SHA-256 `661ad8d6…83e0` + `cmp` exit 0，均为**待 QA 独立复算**的自述，
   本 Gate 不采信为已验证事实。

## 交接

- 状态：Implementation Gate PASS，exact tree `e8aca2a4…` 冻结；并行开 Review Pass 1；
  Plan 段因 `PLAN_GAP` 局部回炉，QA 暂缓
- 产物：本分录
- 工作分支：`20260819-skillhub-731-confirm-publish-event-r2` @ `b80982e8`
- 下一步：规划 修 `expected_before` → 本席复跑 Plan Gate → 派 QA Charter；评审 Pass 1 同步进行
- 未解决：Publication 段（验收 5/6）由主控侧承接
- 需要决策：无。`PLAN_GAP` 属工程链内返修，按 Project 路由 AI 自驱，不 @ Member
