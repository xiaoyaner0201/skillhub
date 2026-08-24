# Stage 2 — 可复现性修复 + `current-authority-resolution` seam RED → GREEN

- **role**: coder（实现）
- **timestamp**: 20260824-084451Z
- **branch**: `20260824-hd32-skillhub-735-subscription-authz-hardening`
- **start point**: `origin/20260824-hd32-skillhub-735-subscription-authz-hardening` @ `e563013a`
- **dispatch**: `leader/20260824-080500-stage1-adjudication-restage.md`（Stage 2 派工 + 跨期硬约束）
- **frozen inputs**: 自证不重做，引 `leader/20260824-080500-stage1-adjudication-restage.md` §1、§2
- **status**: RED → GREEN 完成；**不自签 PASS**，行为是否真的发生归 QA 独立裁决

---

## 1. 任务一：可复现性修复（工单 `entry.symbol` 回归）

Stage 1 落地的 15 条探针里有 7 条方法名偏离了冻结工单的 `entry.symbol`。因工单 `command` 带
`-Dsurefire.failIfNoSpecifiedTests=false`，QA 逐字重放时会**跑零个测试并 exit 0**——HIGH_RISK 链上的静默绿。
本次只改测试方法名，**断言一行未动**。

| 改回前 | 改回后（= `entry.symbol`） |
| --- | --- |
| `publish_privateOrdinaryMember_reachesNeitherSink` | `publish_privateOrdinaryMember_hasZeroRowAndSse` |
| `publish_hiddenOrdinaryMember_reachesNeitherSink` | `publish_hiddenOrdinaryMember_hasZeroRowAndSse` |
| `publish_namespaceOnlyRemovedMember_reachesNeitherSink` | `publish_namespaceOnlyRemovedMember_hasZeroRowAndSse` |
| `yank_privateOrdinaryMember_reachesNeitherSink` | `yank_privateOrdinaryMember_hasZeroRowAndSse` |
| `yank_hiddenOrdinaryMember_reachesNeitherSink` | `yank_hiddenOrdinaryMember_hasZeroRowAndSse` |
| `yank_namespaceOnlyRemovedMember_reachesNeitherSink` | `yank_namespaceOnlyRemovedMember_hasZeroRowAndSse` |
| `ineligibleRecipient_withPreferenceExplicitlyEnabled_stillReachesNeitherSink` | `ineligibleRecipient_preferenceEnabledStillHasZeroFinalSink` |

私有 helper `assertNoSinkReached` / `assertBothSinksReachedOnce` 不是 `entry.symbol`，保持原名。

**commit** `2ef614245e2b78a0f03f8b3ea3a8bbfba6a6da7e` / **tree** `662efc4e152b57edd051ec37a870e18f1c029fcb`

### 1.1 逐条重放（工单原始 `command`，逐字）

7 条全部命中 1 个测试并**断言级失败**：

```
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0   (exit 1, AssertionError)
```

日志：`/workspaces/HD-32/stage2/replay-<symbol>.log`（7 份）。

### 1.2 静默绿对照

用同一 `command` 形状重放一条**尚未落地**的推迟符号 `publish_disabledUser_hasZeroRowAndSse`：

```
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS          (exit 0)
```

这正是本次要消除的失败模式：符号不存在 → 零测试 → exit 0 → 读起来像绿。

### 1.3 符号 × 代码树全量核对

对工单 21 条 `entry.symbol` 逐条在测试树中程序化匹配：`landed=15 missing=6`。
missing 恰为推迟的 6 条：`subscribe-disabled-denied`、`stale-read-disabled-denied`、
`published-disabled-denied`、`yanked-disabled-denied`、`authority-removed-member`、`authority-disabled-user`
（后两条在任务二中落地）。

---

## 2. 任务二：Stage 2 seam RED → GREEN

path_id `current-authority-resolution`。**先 RED 后 GREEN**，两步分别成 commit，顺序在 git 历史里可查。

### 2.1 RED — 只声明 seam 到可编译

**commit** `8925fbf27dcd97c94c87bc2e584b03fa5c76830a` / **tree** `ec82987fd01a8e520a5ea363b5a663241cc385d5`

此时 `SubscriberAccessResolver.resolveReadableRecipients` 原样返回候选集（= base 行为），
不触碰任何 collaborator。两条探针在断言处失败：

```
removedMember_isDeniedFromCurrentFacts
  org.opentest4j.AssertionFailedError:
  [a retained subscription row is history, not authority]
  Expecting actual: ["former-member-1", "member-1"]
  to contain exactly (and in same order): ["member-1"]

disabledUser_isDeniedEvenWithBindings
  org.opentest4j.AssertionFailedError:
  [role bindings cannot override an inactive account]
  Expecting actual: ["disabled-member-1", "member-1"]
  to contain exactly (and in same order): ["member-1"]
```

日志：`/workspaces/HD-32/stage2/red-<symbol>.log`。

> Mockito STRICT_STUBS 陷阱（运行前拦下）：RED 期 resolver 不调用 collaborator，三个 fixture `when(...)`
> 会抛 `UnnecessaryStubbingException`——那是 **error 不是断言失败**，会让 RED 不成立。改用
> `lenient().when(...)`；`NO_N_PLUS_ONE` 由 `verify(..., times(1))` 直接盯住，覆盖面更准。

### 2.2 GREEN — 写实现

**commit** `76cc035db851c491e892b64f7969828be07c0eb8` / **tree** `179c7aa3d3e1f6a46c39f92ebc617b321497e2e0`

两条探针 `Tests run: 1, Failures: 0, Errors: 0`，exit 0。日志：`/workspaces/HD-32/stage2/green-<symbol>.log`。

seam 形状（按 plan-r1 INFERENCE）：

- `VisibilityChecker`：新增 `AccessFacts` record + 全上下文重载 `canAccess(Skill, AccessFacts)`。
  账号状态 fail-closed **前置于所有角色规则**，SUPER_ADMIN / namespace OWNER 都无法把停用账号放回来。
  三个既有重载一字未改，8 个既有调用方不迁移。
- `SubscriberAccessResolver`（新建，在 write_set 内）：每个权威源**恰好一次批量读**，决策循环内零仓储调用；
  任一源抛出即抛出整个方法，不会有半份收件人集合流到下游 sink。缺账号行 == 非 ACTIVE，一律拒。
- `NamespaceMemberRepository` / `NamespaceMemberJpaRepository`：`findByNamespaceIdAndUserIdIn`。
- `RbacService`：`getUserRoleCodesIn`，经同一条 `PlatformRoleDefaults.withDefaultUserRole` 归一，
  避免出现第二套角色模型；未绑定用户也出现在结果里。

### 2.3 write_set 纪律

`AccessFacts` **嵌在 `VisibilityChecker.java` 内**而非新开 `SkillAccessContext.java`——新文件会落在冻结
`write_set` 之外。命名精确性让位于冻结边界。

`git diff --name-only e563013a HEAD` 的 `server/**` 全部 7 条路径逐条比对 `plan-gate-r1.json` 的
`write_set` / test write_set：**violations = 0**。

### 2.4 刻意不接线

Stage 2 只做共享 seam。`SkillSubscriptionService`、`MySkillAppService`、`NotificationEventListener`
**没有**接到 resolver 上。因此推迟的 4 条 `*-disabled-*` 探针仍未被观测到——这是跨期硬约束
「推迟的 6 条必须先被观测到断言级失败，才允许写使其通过的行为」的直接落实，不是遗漏。

---

## 3. 全量回归（真实输出）

```
./mvnw -pl skillhub-domain,skillhub-auth,skillhub-infra,skillhub-notification,skillhub-search,skillhub-app -am test
```

| module | 结果 |
| --- | --- |
| skillhub-domain | SUCCESS，412 |
| skillhub-auth | SUCCESS，126 |
| skillhub-notification | SUCCESS，35 |
| skillhub-infra | SUCCESS，10 |
| skillhub-search | SUCCESS，35 |
| skillhub-app | `Tests run: 756, Failures: 15, Errors: 0, Skipped: 1` |

15 条失败逐条归位到 Stage 1 推迟集所在的同 4 个类：`SkillSubscriptionServiceTest` 3、
`SubscriptionMessageBundleTest` 1、`MySkillAppServiceTest` 4、`SubscriberNotificationSinkTest` 7。
`SubscriberAccessResolverTest` 2/2 通过。其余无回归。日志：`/workspaces/HD-32/stage2/reactor.log`。

---

## 4. 交接

- **本期终点 tree**：`179c7aa3d3e1f6a46c39f92ebc617b321497e2e0`（commit `76cc035d`）
- 三个可查节点：rename `2ef61424`/`662efc4e` → RED `8925fbf2`/`ec82987f` → GREEN `76cc035d`/`179c7aa3`
- QA 现在逐字重放工单 `command` 会真跑到测试，不再零测试 exit 0
- `Errors: 0` 是实测上报，不代表行为已验证——归 QA 独立执行裁决
- 下一步按裁决：Stage 3 = `subscription-create` + `my-subscriptions-current-read` → GREEN + 两条 `*-disabled-*`；
  Stage 4 = 两条 fanout + 两条 `*-disabled-*` + `RecordingSseEmitterTestConfiguration`
