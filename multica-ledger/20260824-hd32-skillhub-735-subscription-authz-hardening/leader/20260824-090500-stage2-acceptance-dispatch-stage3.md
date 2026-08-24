# Leader 分录 — 20260824-090500 — Stage 2 受理、D3 闭合、派发 Stage 3

- Agent：调度（leader）
- 触发：coder 分录 `coder/20260824-084451-stage2-authority-seam.md`
- Stage：IMPLEMENTING
- 裁决：**Stage 2 受理；D3 闭合；分期细化后派发 Stage 3。均不构成 Gate PASS。**

## 1. D3 闭合（补记上一轮受理）

Planner 分录 `planner/20260824-080019-assume-message-copy-supersession.md`（commit `3ecff05b`,
tree `53a6e009`）已复算：diff 仅 2 个 `multica-ledger/**` 文件、零 `server/**` 改动、`index.md`
表尾单行追加、四项冻结 digest 未漂移、subject tree 未变。superseding statement 仅收敛
`ASSUME_MESSAGE_COPY`，与 plan-r1 §1 实际文本一致。

**D3 `PLAN_GAP` 闭合。** 自本分录起，该 assumption 的可复现验收边界为：exact key
`error.skill.subscription.noPermission`、EN 与 zh 均经真实 message source 解析、均不回落裸 key、
两 locale 结果可区分；**字面文案不是冻结验收值**。Stage 3 触碰 `messages*.properties` 时以此为准。

## 2. Stage 2 自述的独立复算

全部 git 层复算，未采信自述：

| 项 | 自述 | 实测 | 结论 |
|---|---|---|---|
| rename commit/tree | `2ef61424` / `662efc4e` | 一致 | ✅ |
| RED commit/tree | `8925fbf2` / `ec82987f` | 一致 | ✅ |
| GREEN commit/tree | `76cc035d` / `179c7aa3` | 一致 | ✅ |
| 终点 commit/tree | `707b4a16` / `63ee9486` | 一致 | ✅ |
| 提交顺序 rename→RED→GREEN→ledger | 可查 | git 历史顺序一致 | ✅ |
| `write_set` violations | 0 | 7 条 `server/**` 路径逐条比对冻结 artifact，0 越界 | ✅ |
| rename 只动名字 | 断言未改 | diff 恰 7 对方法签名行，零断言行 | ✅ |
| 符号 × 代码树 | landed 17 / missing 4 | 程序化匹配一致，missing 恰为 4 条 `*-disabled-*` | ✅ |
| 刻意不接线 | 三入口未接 resolver | 全仓 grep：resolver 自身文件外零引用 | ✅ |
| ledger 纪律 | 单行追加 | index.md 仅 +1 行，头部零改动 | ✅ |

### 2.1 RED 的真实性（这是本阶段最关键的一条）

在 RED 提交 `8925fbf2` 上读回 `SubscriberAccessResolver.resolveReadableRecipients`，其实现为
「去重后原样返回候选集」，**构造器注入的四个 collaborator 一个都没调用**。因此两条 `authority-*`
探针是在**行为**上失败，不是编译失败或桩缺失——满足我在 080500 §5 定的跨阶段约束
「推迟的探针必须先被观测到断言级失败，才允许写使其通过的行为」。

### 2.2 增量的破坏面

`3ecff05b..707b4a16` 四个 `src/main` 文件 numstat 全部为 `N 0`——**整个 Stage 零删除行**：

```
24  0  skillhub-auth/.../rbac/RbacService.java
 6  0  skillhub-domain/.../namespace/NamespaceMemberRepository.java
26  0  skillhub-domain/.../skill/VisibilityChecker.java
 1  0  skillhub-infra/.../jpa/NamespaceMemberJpaRepository.java
```

`VisibilityChecker` 三个既有 overload 逐字未改，12 处 `canAccess` 引用中扣掉 wrapper 自身与
新 resolver 的调用后为 8 个既有 consumer，**无一迁移**，与 plan-r1 的「8 个既有 consumer」吻合。
`PRIVATE -> isOwner || isAdminOrAbove` 原样保留，Issue 明令不得改变的语义未被触碰。

账号状态门的位置也复算过：`canAccess(Skill, AccessFacts)` 在委派给既有 4 参 overload **之前**
执行 `!facts.accountActive() -> false`，因此 `isSuperAdmin` 短路在门之后，停用账号无法被
SUPER_ADMIN 或 namespace OWNER 放回。

### 2.3 write_set 纪律的两个正确取舍

- `AccessFacts` 嵌在 `VisibilityChecker.java` 内而非新建 `SkillAccessContext.java`——新文件会落在
  冻结 `write_set` 之外。取舍正确。
- resolver 使用 `UserAccountRepository.findByIdIn` 与 `UserAccount.isActive()`：二者**在冻结 base
  `e8cab738` 上已存在**（`UserAccountRepository.java:14`、`UserAccount.java:80`），故只读复用，
  无需修改该文件，未越 `write_set`。

顺带确认 G2 的原判仍成立：账号状态在 base 上**存在**但**从三个入口不可达**，所以它确实需要一次
`src/main` 接线，Stage 1 的 test-only 边界装不下——归因仍是我的分期缺陷，不是 Planner。

### 2.4 不背书项

`Tests run: 756, Failures: 15, Errors: 0` 与各模块 SUCCESS 计数属 Coder 实测上报。
**本分录不背书为已验证**：行为是否真的发生由 QA 独立执行裁决。Coder 亦未自签 PASS，口径正确。

## 3. 分期细化（仍是顺序阶段化，不重新切分）

080500 §5 把 `subscription-create` 与 `my-subscriptions-current-read` 放在同一个 Stage 3。
按实测预算重新定界：Stage 1 在第 4 个文件附近撞压缩，Stage 2 共 6 个文件、峰值仍达 169,457
（与 Stage 1 同值，判为压缩触发阈值而非巧合）。合并后的 Stage 3 约 8 个文件，会撞压缩。

因此拆为：

- **Stage 3 — `subscription-create` + i18n**：`SkillSubscriptionService` 接 resolver/checker、
  `messages.properties` / `messages_zh.properties`；3 条 subscribe 探针转 GREEN，
  `subscribe-disabled-denied` 走 RED → GREEN，`subscribe-message-en-zh` 转 GREEN。约 5 个文件。
- **Stage 4 — `my-subscriptions-current-read`**：`MySkillAppService`（必要时 `MeController`）；
  4 条 stale-read 探针转 GREEN，`stale-read-disabled-denied` 走 RED → GREEN。约 4 个文件。
- **Stage 5 — 两条 fanout path**：`NotificationEventListener` / `NotificationService`；7 条探针转
  GREEN，两条 `*-disabled-*` 走 RED → GREEN，并落地 D1 的
  `RecordingSseEmitterTestConfiguration`。

`work_partition` 仍为单一 unit、13 个 planned path_id；顺序阶段化不构成重新切分，不需要 `plan-r2`。

## 4. Stage 3 边界（写进派发）

- 只许动冻结 `write_set` 内路径；新建文件前先确认它在 `write_set` 内。
- `subscribe-disabled-denied` 必须先断言级 RED 再 GREEN，与 Stage 2 同样分两个 commit。
- i18n 验收以本分录 §1 的 superseding observable 为准，**不得把字面文案当冻结验收值**。
- plan-r1 REQUIREMENT：`unsubscribe` 撤权逃生路径与 `isSubscribed` 的 own-row boolean
  不得被 subscribe 新门禁反向锁死。
- `NON_GOAL_NO_DATA_CLEANUP`：不删 row、不修 count、不写 migration/prune。
- 不得改变 `PRIVATE -> owner || manager`。

## 5. 边界

未 push 公开分支、未建上游 PR、未签 CLA、未改写作者、未 force-push、未改冻结产物、未降级
HIGH_RISK、未自签任何 Gate PASS。Publication 仍不在本链内。未升级人工。
