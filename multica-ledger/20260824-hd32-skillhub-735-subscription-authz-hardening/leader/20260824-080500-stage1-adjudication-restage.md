# Leader 分录 — 20260824-080500 — Stage 1 裁决与重新分期

- Agent：调度（leader）
- 触发：coder 分录 `coder/20260824-074326-stage1-red.md`（Stage 1 RED PARTIAL + PLAN_GAP）
- Stage：IMPLEMENTING
- 裁决：**Stage 1 受理为 RED PARTIAL；6 条不可表达探针不是 PLAN_GAP，是 Leader 分期缺陷，由我承担；
  另发现 1 处未披露的可复现性缺陷，退回 Coder 修正。**

## 1. Coder 自述的独立复算

全部用 git 层事实复算，未采信自述：

| 项 | 自述 | 实测 | 结论 |
|---|---|---|---|
| 代码 commit / tree | `44b71e0e` / `ca28196b` | 一致 | ✅ |
| 含 ledger commit / tree | `faf75ca4` / `c5121b6e` | 一致 | ✅ |
| `src/main/` 改动 | 零 | `git diff --name-only 744193d7 faf75ca4 \| grep /src/main/` 为空 | ✅ |
| 触及文件 | 5 个允许文件的子集 | 4 个（`SubscriberAccessResolverTest` 未创建） | ✅ 未越界 |
| 测试总数 | 33 | `@Test` 计数 9+16+7+1 = 33 | ✅ |
| 新增探针数 | 15 | delta 3+4+7+1 = 15 | ✅ |
| ledger 纪律 | 单行追加 | index.md 仅 +1 行，头部零改动 | ✅ |

`Tests run: 33, Failures: 15, Errors: 0` 属 Coder 实测上报，本分录**不背书为已验证**：
「行为是否真的发生」由 QA 独立执行裁决，Leader 不代 QA 签字。

## 2. G1 / G2 / D1 的复算：事实成立，但归因不是 Planner

**G1 成立。** `server/skillhub-app/src/main/java/com/iflytek/skillhub/listener/SubscriberAccessResolver.java`
在冻结 base `e8cab738` 与当前 head `faf75ca4` 上均不存在，仅存在于 r3 提交 `046c04ed`。
该文件确在冻结 artifact `write_set` 内，且是**新建 src/main 文件**。

**G2 成立。** 四条 `*-disabled-*` 的三个生产入口在 base 上均不持有账号状态协作者：

- `SkillSubscriptionService`：`SkillSubscriptionRepository` / `SkillRepository` / `ApplicationEventPublisher`
- `MySkillAppService`：7 个协作者，无 user/account 仓储
- `NotificationEventListener`：`RecipientResolver`（仅持 `NamespaceMemberRepository` + `UserRoleBindingRepository`）等 7 项
- `VisibilityChecker.canAccess` 两个 overload 的形参里没有任何账号状态入参

`UserStatus.DISABLED` 在 base 上确实存在（`AuthController`、`AdminUserAppService`），但**不可从这三个入口到达**。
Coder 拒绝用未打桩 mock 的「查不到用户」冒充 disabled-user 覆盖，是正确的——那是另一条不变量（账号缺失 → fail-closed）。

**D1 成立。** `RecordingSseEmitterTestConfiguration.java` 在冻结 artifact `write_set` 内，但不在
Stage 1 工单 `test_files_touched` 的 5 个文件内。原因是我的投影规则只取探针 `entry.path` 的并集，
支撑性 fixture 不在任何探针的 `entry` 上，因此被机械地漏掉。

### 归因：这是我的分期缺陷，不是 PLAN_GAP

计划本身自洽：它明确要新建 `SubscriberAccessResolver`、要给 `VisibilityChecker` 加 full-context
overload、并在 REQUIREMENT 里写明「disabled/missing account 即使携带角色也必须拒绝」。
**使这 6 条不可表达的是我给 Stage 1 定的边界**——「只写测试、不得动 `src/main/`、编译错误不算 RED」。
我把 21 条 `expected_before=="FAIL"` 探针整体投影进了一个结构上装不下其中 6 条的阶段。

按路由表这属 `RISK_ROUTE_GAP`（分期/分级判断错误），**回到调度自身**，不转 Planner。
Planner 无需重做计划，`write_set`、探针定义、`work_partition` 全部不动。

维护者第 4 项要求的 disabled-user 覆盖**未被丢弃**，只是改期到允许动 `src/main/` 的阶段。

## 3. 新发现（Coder 未披露）：7 条探针的 `command` 已失效

Coder 披露了 D1/D2/D3，但漏了这一条。工单每条探针的 `command` 形如
`-Dtest=Class#symbol -Dsurefire.failIfNoSpecifiedTests=false`。落地的 15 条里有 **7 条改了方法名**，
与冻结 artifact 的 `entry.symbol` 不再匹配：

| probe_id | 工单 `entry.symbol` | 实际方法名 |
|---|---|---|
| `published-private-denied` | `publish_privateOrdinaryMember_hasZeroRowAndSse` | `publish_privateOrdinaryMember_reachesNeitherSink` |
| `published-hidden-denied` | `publish_hiddenOrdinaryMember_hasZeroRowAndSse` | `publish_hiddenOrdinaryMember_reachesNeitherSink` |
| `published-removed-denied` | `publish_namespaceOnlyRemovedMember_hasZeroRowAndSse` | `publish_namespaceOnlyRemovedMember_reachesNeitherSink` |
| `yanked-private-denied` | `yank_privateOrdinaryMember_hasZeroRowAndSse` | `yank_privateOrdinaryMember_reachesNeitherSink` |
| `yanked-hidden-denied` | `yank_hiddenOrdinaryMember_hasZeroRowAndSse` | `yank_hiddenOrdinaryMember_reachesNeitherSink` |
| `yanked-removed-denied` | `yank_namespaceOnlyRemovedMember_hasZeroRowAndSse` | `yank_namespaceOnlyRemovedMember_reachesNeitherSink` |
| `notification-ineligible-zero` | `ineligibleRecipient_preferenceEnabledStillHasZeroFinalSink` | `ineligibleRecipient_withPreferenceExplicitlyEnabled_stillReachesNeitherSink` |

其余 8 条 symbol 精确匹配。15 = 8 + 7，与自述数量吻合。

**失败模式是静默绿**：`failIfNoSpecifiedTests=false` 的语义就是「指定的测试不存在时不报错」，
所以 QA 若逐条重放工单 `command`，这 7 条会跑零个测试并 `exit 0`，被读成通过。
HIGH_RISK 链上这是不可接受的可复现性缺陷。

工单是冻结产物、append-only，**不改工单**；退回 Coder 把方法名改回 `entry.symbol`，
恢复对冻结 artifact 的可重放绑定。命名精确性让位于可复现性。

## 4. D2 / D3 处置

**D2 受理为已知限制，不返修。** 复算属实：三个入口在 base 上都不接收成员关系协作者，
removed-member 的正向对照在单元层只能用 owner。成员维度的对照留到引入协作者之后的阶段。

**D3 成立，判为真 `PLAN_GAP`，转 Planner。** 冻结 artifact 的
`ASSUME_MESSAGE_COPY` 声称「denial code 解析为 plan-r1 中冻结的 EN/ZH 文案」，其 evidence 锚点为
`planner/20260824-040000-plan-r1.md#1`；但 plan-r1 §1 实际只写了
「错误 code 为 `error.skill.subscription.noPermission`，EN/zh 均须真实解析」，**没有冻结任何字面串**。
假设陈述强于其自证锚点。Coder 断的是冻结的可观察量（不回落裸 code、EN≠ZH），与 plan-r1 一致，未被误导。

按 append-only 纪律，不改写冻结 artifact，由 Planner 出 superseding 分录收敛该假设。

## 5. 重新分期（顺序阶段化，`work_partition` 仍为单一 unit）

不重新切分，按计划自带的 `path_id` 顺序推进；共享 seam 先行，正是它解锁 G1/G2：

- **Stage 2 — `current-authority-resolution` seam**：`VisibilityChecker` full-context overload、
  新建 `SubscriberAccessResolver`、账号状态协作者接线；`authority-removed-member` /
  `authority-disabled-user` 两条走 RED → GREEN。
- **Stage 3 — `subscription-create` + `my-subscriptions-current-read`**：对应已落地探针转 GREEN，
  并补 `subscribe-disabled-denied` / `stale-read-disabled-denied` 两条。
- **Stage 4 — `published-subscriber-fanout` + `yanked-subscriber-fanout`**：对应探针转 GREEN，
  补 `published-disabled-denied` / `yanked-disabled-denied` 两条，并落地 D1 的
  `RecordingSseEmitterTestConfiguration`。

**跨阶段不变的边界约束**：这 6 条推迟的探针，必须先被观测到**断言级失败**，才允许写使其通过的行为；
为让它们编译而先声明 seam 是允许的。这是分期约束，不是实现设计——seam 形状仍以 plan-r1 的
INFERENCE 为准（resolver 只批量装配 facts 并调用 checker，不复制决策树）。

## 6. 预算

Coder 实测单次请求峰值 **169,457 tokens**，在第 116/148 次采样触发一次自动压缩，压缩后稳定 ~89.6k。
采纳其两条定界建议：后续 Stage 按 path_id 分组（每程 4–6 个文件），且**冻结输入自证结论由 Leader
以摘要形式前置传递**，不要求每程重复复算 97KB artifact——本分录第 1、2 节即该摘要。

## 7. 边界

未 push 公开分支、未建上游 PR、未签 CLA、未改写作者、未 force-push、未改冻结产物、未降级 HIGH_RISK。
Publication 仍不在本链内。未升级人工：本轮全部为工程内部裁决与返修路由，Project description
的升级判据均未命中。
