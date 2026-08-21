---
ts: 20260821-161836
agent: reviewer
action: BLIND_BEHAVIOR_AUDIT (Review Pass 1)
tree: 922195602c92f0f420e17df9021ae39727c95f8d
verdict: FROZEN
---

Review Pass 1 = `BLIND_BEHAVIOR_AUDIT`。fresh context、只读、结论绑定 exact tree
`922195602c92f0f420e17df9021ae39727c95f8d`。本分录冻结后方可进入 Pass 2
`EVIDENCE_CHALLENGE`。**本 Run 不出四维 verdict，不请求 Review Gate**——
`review-gate.v1` schema 强制要求 `plan_sha256` / `qa_sha256` / `pass2_challenge`，
Pass 1 依定义无法也不应持有这三项。

## Blindness 声明（可机械核验）

本 Run 读取过的账本路径：**仅 `index.md` 末 6 行**（为取表格式追加自己那一行）。

未读取，且为保持 blind 主动拒读：

| 路径 | 拒读理由 |
|---|---|
| `planner/**` | Plan artifact |
| `coder/**`（含全部 `*-output.log`、`semgrep*`） | Coder 解释与实现者证据 |
| `qa/20260821-161203-blind-qa-charter.md` | QA 产物 |
| `qa/behavior-inventory.v1.json` | 交付协议允许 Pass 1 读 discovery inventory，但该文件实际落在 `qa/` 目录下，与调度「不得获取 QA 产物」的指令边界重叠。**主动放弃**，改为完全独立追踪。 |
| `leader/**` | 含 Plan Gate 与候选核对结论 |

代价与处置：`pass1_source_first.inventory_candidate_ids` 本 Run **无法填写**，
留给 Pass 2 做候选对齐。这是有意的取舍——`multica-engineering-delivery` 已规定
inventory 不得作为 Reviewer 的完整性上界，因此不读它不降低本遍的覆盖责任，
只是把「对齐」这一步后移。

Issue 正文与评论中不可回避的上下文已读；本遍所有行为结论均由源码独立重建，
未采信任何 Agent 的自述。

## Tree 绑定

```
$ git rev-parse HEAD^{tree}          # at 046c04ed
922195602c92f0f420e17df9021ae39727c95f8d
$ git status --porcelain             # 空
$ git rev-parse 046c04ed:server e36fd8d3:server
7d525002710465020565f6f8ecd73d2863c60f09
7d525002710465020565f6f8ecd73d2863c60f09
$ git diff --stat 046c04ed e36fd8d3 -- . ':(exclude)multica-ledger'
（空）
```

远端分支自候选起已推进两个 commit（`37a11af6`、`e36fd8d3`），二者业务面 diff 为空、
`server` 子树 hash 逐字节一致，均为纯 ledger commit。exact tree 冻结成立。

## 独立重建的行为图

不依赖任何 inventory，从源码重建。

### 生产者 → 事件 → 消费者

`SkillPublishedEvent` 有 **4 个 producer**：

- `SkillPublishService.java:572`
- `ReviewService.java:233`
- `PromotionService.java:258`
- `SkillReviewSubmitService.java:150`（= 上游 #738 已并入 main 的 `confirmPublish` 补发；
  本次未改动该文件，但它是**因 base 变化而新可达**的 producer，其事件同样流入本次新增的过滤器）

`SkillVersionYankedEvent` 有 **1 个 producer**：`SkillGovernanceService.java:283`。

消费者（全仓 `@EventListener` / `@TransactionalEventListener` 独立枚举，6 个类）：

| consumer | 是否被本次改动影响 |
|---|---|
| `NotificationEventListener#onSkillPublished`（:58，通知 publisher 本人） | 否，未加过滤 |
| `NotificationEventListener#onSkillPublishedForSubscribers`（:74） | **是** |
| `NotificationEventListener#onSkillVersionYankedForSubscribers`（:101） | **是** |
| `SearchIndexEventListener#onSkillPublished`（:31） | 否——搜索索引有自己的 `SearchVisibilityScope`，本次不应也未介入 |
| `SkillRatingEventListener` / `SkillStarEventListener` / `LabelSearchSyncListener` / `BuiltinSkillInitializer` | 不消费这两个事件 |

### 最终 sink

- `S1` `NotificationDispatcher.dispatch(recipientId, …)` —— 收件人可见的通知记录，
  payload 含 `skillId` / `skillName` / `slug` / `namespace` / `version`
  （`NotificationEventListener.java:277-292` `bodyWithSkill` + `versionLabel`）
- `S2` `skill_subscription` 持久化行（`SkillSubscriptionService.java:74`）
- `S3` `SkillSubscribedEvent`（:76）
- `S4` HTTP 403 + `error.skill.subscription.noPermission`（:69，`DomainForbiddenException.statusCode()=403`）

### 权限判定链

```
subscribe:  Controller:27 → principal.platformRoles()
            → SkillSubscriptionService:55-70
            → VisibilityChecker.canAccess(5-arg):39-59
            → VisibilityChecker.canAccess(4-arg):21-37

fanout:     Listener:76/103 findSubscribersBySkillId  (已 .distinct())
            → Listener:80/107 namespaceRepository.findById  → empty 即 return
            → SubscriberAccessResolver.resolveReadableSubscribers:40-65
            → 同一个 5-arg → 4-arg
            → Listener:89-95 / 116-122 skip publisher/actor → dispatch
```

## 逐行处理：调度点名的两个问题

### Q1 —— `SubscriberAccessResolver.java:43-64` 是否真的零独立判定分支

**结论：是。逐行证伪未找到任何独立 allow/deny 分支。**这不是接受实现者的
「设计上没有」，下面是我自己重建的逐行论证。

| 行 | 内容 | 是否构成独立判定 |
|---|---|---|
| 43 | `new LinkedHashSet<>(candidateUserIds).stream().toList()` | 否。纯集合去重 + 保序。它能且只能移除完全重复的元素，而唯一 caller 的 `findSubscribersBySkillId`（`SkillSubscriptionService.java:97`）已 `.distinct()`，故此行在生产路径上是恒等变换。无任何对 skill/user 属性的谓词。 |
| 44-45 | `userAccountRepository.findByIdIn` → `toMap` | 否。批量取数，无 filter。缺号 ⇒ key 缺失 ⇒ 第 59 行传 `null` ⇒ 拒绝由 `VisibilityChecker.java:44` 作出。**判定在 checker 内，不在这里。** `toMap` 无重复键风险（`UserAccount` 主键即 id）。 |
| 46-48 | `findByNamespaceIdAndUserIdIn(skill.getNamespaceId(), …)` → `toMap` | 否，但**是一处输入域收窄**。只查该 skill 所属 namespace 的成员关系。这与 4-arg 契约 `Map<Long, NamespaceRole>`（全 namespace）不同，之所以等价，是因为 4-arg 只在 `:27` `:34` `:35` 三处读 `skill.getNamespaceId()` 这一个键。等价性成立但非局部——见 `R-P1-04`。`toMap` 无重复键风险：`namespace_member` 有 `@UniqueConstraint(namespace_id, user_id)`（`NamespaceMember.java:8-9`）。 |
| 49-53 | 聚合 `UserRoleBinding.getRole().getCode()` | 否。无条件收集，不过滤、不增补。`UserRoleBinding`（`UserRoleBinding.java:10-40`）无 status / expiry / 作用域列，`@UniqueConstraint(user_id, role_id)`，故不存在「过期绑定被当作有效」的可能。**但它绕开了仓库正本 `RbacService.getUserRoleCodes`——见 `R-P1-03`。** |
| 54-63 | 逐个 `canAccess`，五参原样转发 | 否。第 61 行 `membership == null ? null : getRole()` 是 null-object 映射，与 4-arg 内部对缺键的处理同义。 |
| 64 | `filter(candidate -> readable.get(candidate))` | 否。仅投影 checker 返回的 boolean，无附加谓词。`readable` 在 55-63 对全部 candidate 赋值，故自动拆箱无 NPE。 |

**要点**：43-64 唯一具有政策性质的内容不是分支，而是**重建了哪些授权输入**
（账号、本 namespace 成员关系、平台角色）。这一层确实无独立判定；本遍的两条实质
finding 恰好落在「重建方式」上，而不是「是否有 if」。

### Q2 —— 新 overload 自行处理 SUPER_ADMIN 后向 4-arg 传空 `platformRoles`，是否丢了行为

**结论：在本 tree 上不丢任何行为，可证明；但等价性是非局部的，属于脆弱写法。**

证明：`VisibilityChecker.java:58` 可达 ⟺ `:47` 的 `isSuperAdmin(platformRoles)` 为
false。4-arg 消费 `platformRoles` 的唯一出口是 `:23` 的 `isSuperAdmin`
（`:69-71` 只检查是否含 `"SUPER_ADMIN"`），而 `isSuperAdmin(Set.of())` 同样为 false。
两者在该点必然同值，故以 `Set.of()` 替换不可能改变 4-arg 的返回。

风险不在今天：该等价性依赖「4-arg 永远不消费 SUPER_ADMIN 以外的平台角色」这一
未被任何注释或测试钉死的性质。一旦有人给 4-arg 加上（例如）`SKILL_ADMIN` 分支，
两条新路径会**静默地、朝拒绝方向**丢失它。修复成本为零——第 58 行原样转发
`platformRoles` 今天行为完全相同。记为 `R-P1-04`。

## Findings

### `R-P1-01` — HIGH — `PLAN_GAP` — yank 掉最后一个已发布版本时，全部订阅者静默收不到 yank 通知

- **path_id** `P-FANOUT-YANK` / **state_id** `S-LATEST-VERSION-NULL`
- **位置**：`SkillGovernanceService.java:274-281` + `NotificationEventListener.java:107-122` + `VisibilityChecker.java:29-31`
- **可复现反例**：PUBLIC skill，`status=ACTIVE`，`hidden=false`，仅 1 个 `PUBLISHED` 版本；
  订阅者 A、B 为 ACTIVE 账号、非 owner、在该 namespace 无角色、无平台角色。管理员 yank 该版本。
  1. `yankVersion` 把 version 置 `YANKED`；因 `versionId.equals(skill.getLatestVersionId())` 成立，
     执行 `skill.setLatestVersionId(findLatestPublishedVersionId(...))`，而该方法只统计
     `SkillVersionStatus.PUBLISHED`，此时已无剩余 ⇒ **`latestVersionId = null`**（`:275-279` / `:288-296`）。
  2. `:283` 发布 `SkillVersionYankedEvent`。`@TransactionalEventListener` 默认 `AFTER_COMMIT`，
     叠加 `@Async`，监听器读到的是**提交后**状态。
  3. `Listener:102` 重新 `findById` 拿到 `latestVersionId == null` 的 skill。
  4. `resolveReadableSubscribers` → 5-arg（账号 ACTIVE、非 super admin、namespace 非 ARCHIVED）
     → 4-arg `:29-31`：`latestVersionId == null` ⇒ `return isOwner(...)` ⇒ A、B 均 false。
  5. 返回空列表 ⇒ **`dispatch` 零次**。改动前 A、B 都会收到 `SUBSCRIPTION_VERSION_YANKED`。
- **影响**：撤回通知的收件人恰恰是装了这个被撤回版本的人。把「事后可读性」当作
  「是否有权知道这次状态变更」，而这次变更本身就是让它变不可读的原因——过滤器自我否定。
  触发条件不是边角：单一已发布版本的 skill 是常态；只要还有另一个 `PUBLISHED` 版本，
  `latestVersionId` 不变，通知照常，所以缺陷只在最该发通知的那一次出现。
- **证伪路径**：若能证明产品语义上「skill 退回未发布态后不应再向订阅者发任何通知」，
  则本条降级为需向维护者披露的行为破坏性变更，而非缺陷。该论证必须显式给出，
  不能由沉默承担。
- **当前无覆盖**：`grep -rn "LatestVersionId(null)" server/*/src/test` 命中 0，
  本 tree 内没有任何测试进入该状态，因此看不出这是被裁决过的取舍。
- **required_repair**：yank 通道的收件人资格改为按 **yank 前**可读性评估
  （或对 yank 通知显式豁免 `latestVersionId == null` 分支），并把选定语义写进 PR 正文
  交维护者裁决。
- **status** OPEN

### `R-P1-02` — MEDIUM — `PLAN_GAP` — 上游有两道读闸，本次静默选了宽的那道，未作处置说明

- **path_id** `P-SUBSCRIBE` + `P-FANOUT-PUBLISH` / **state_id** `S-SKILL-ARCHIVED`
- **位置**：`SkillQueryService.java:809-825`（`assertPublishedAccessible`）vs `:201-211`（`getSkillDetail`）
- **事实**：上游对「读一个 skill」存在两套不同强度的闸门。
  - `getSkillDetail`：ARCHIVED namespace 非成员拒 + `visibilityChecker.canAccess`。**不看 `skill.getStatus()`。**
  - `assertPublishedAccessible`（被 8 个内容类端点调用，`:301/331/390/406/424/443/456/560`）：
    额外含 `skill.getStatus() != SkillStatus.ACTIVE && !canManageRestrictedSkill(...)` ⇒ 403。
  - `Skill.isHidden()`（`Skill.java:148`）是独立 boolean 字段，**与 `SkillStatus` 无关**，
    因此 `SkillStatus.ARCHIVED`（由 `SkillGovernanceService.java:118` 设置）不被 hidden 分支覆盖。
- **影响**：本次新增的判定与 `getSkillDetail` 一致、与 `assertPublishedAccessible` 不一致。
  后果是一个已被管理员下架（`SkillStatus.ARCHIVED`）的 skill：元数据端点可读、8 个内容端点 403、
  **仍可被订阅，其订阅者仍在 fanout 收件人集合内**。
- **判断**：维护者原文是「cannot read the skill **metadata**」，按字面对齐 `getSkillDetail` 站得住，
  所以我不判为缺陷。但两道闸存在强度差是客观事实，本次改动在其间做了选择却未留任何论证，
  下游无法区分「裁决过」与「没看见」。
- **required_repair**：显式 disposition——要么在账本/PR 正文写明按 metadata 闸对齐及理由，
  要么补齐 `SkillStatus != ACTIVE` 的拒绝。二选一，不接受沉默。
- **status** OPEN

### `R-P1-03` — MEDIUM — `IMPLEMENTATION_DEFECT` — 平台角色推导出现第二个正本，正是维护者第 3 条反对的形状

- **path_id** `P-FANOUT-PUBLISH` / **state_id** `S-PLATFORM-ROLE-DERIVATION`
- **位置**：`SubscriberAccessResolver.java:49-53` vs `RbacService.java:35-39`
- **事实**：仓库正本是 `RbacService.getUserRoleCodes(userId)` =
  `PlatformRoleDefaults.withDefaultUserRole(bindings → codes)`；登录侧
  `IdentityBindingService.java:81-84` 也走同一规范化。`SubscriberAccessResolver` 绕开
  `RbacService`，直接查 `UserRoleBindingRepository.findByUserIdIn` 自行拼装，
  **且未应用 `withDefaultUserRole`**（无绑定用户拿到 `Set.of()`，而正本会给 `{"USER"}`）。
- **今天的行为差**：无。消费方只看 `"SUPER_ADMIN"`，`withDefaultUserRole` 只在集合为空时补
  `"USER"`（`PlatformRoleDefaults.java:20-27`），永不引入 `SUPER_ADMIN`。
- **为什么仍要记**：维护者第 3 条要的是「复用既有 access policy，不要并行模型」。
  可见性判定确实收敛到了 `VisibilityChecker`，但**平台授权推导反而多出了一处**。
  一个语义相同、实现独立、且已经漂移了一个规范化步骤的副本，就是并行模型的定义。
- **required_repair**：在 `RbacService` 上加批量方法（如 `getUserRoleCodesIn(Collection<String>)`）
  并由 resolver 调用，使角色码推导只有一个正本。
- **status** OPEN

### `R-P1-04` — LOW — `IMPLEMENTATION_DEFECT` — 两处非局部等价性未被钉死

- **位置**：`VisibilityChecker.java:58`（传 `Set.of()` 而非 `platformRoles`）；
  `SubscriberAccessResolver.java:46-47`（成员关系只查 `skill.getNamespaceId()`）
- **影响**：两处今天都可证明等价（论证见上文 Q1/Q2），但等价性依赖 4-arg 的内部实现细节，
  无注释、无测试固定。任一处将来朝拒绝方向静默失效。
- **required_repair**：第 58 行原样转发 `platformRoles`（今天行为完全相同，直接消除耦合）；
  成员关系收窄处补一行注释说明依据。
- **status** OPEN

### `R-P1-05` — LOW — `IMPLEMENTATION_DEFECT` — 2 参 `subscribe` overload 成为静默拒绝 SUPER_ADMIN 的陷阱

- **位置**：`SkillSubscriptionService.java:52-54`
- **事实**：独立枚举全仓 `.subscribe(` 调用点，生产侧只剩
  `SkillSubscriptionController.java:27` 一处，且走 3 参版。2 参 overload 生产调用点为 **0**，
  但它 public 且硬编码 `Set.of()`。
- **影响**：任何未来 caller 误用它，SUPER_ADMIN 会被 403，且没有任何提示。
- **required_repair**：删除，或改为显式要求调用方传入平台角色。
- **status** OPEN

### `R-P1-06` — LOW — `PLAN_GAP` — namespace 缺失时两条路径失败方向相反（登记）

- **位置**：`NotificationEventListener.java:80-83`/`107-110`（fail-closed：直接 return，
  连 owner 都收不到）vs `SkillSubscriptionService.java:59` + `VisibilityChecker.java:50`
  （fail-open：`namespace == null` 时跳过 ARCHIVED 判定，落到基础可见性）
- **可达性**：实测极低。`NamespaceService.deleteNamespace:120-132` 要求先
  `assertNoDependentData(namespaceId)`，即 namespace 下无 skill 才允许硬删，
  因此「skill 存在而 namespace 不存在」在正常流程中构造不出来。
- **required_repair**：仅登记，或统一为 fail-closed。不阻断。
- **status** OPEN

### `R-P1-07` — LOW — `INVARIANT_VIOLATION`（`SECURITY` 标签）— VisibilityChecker 的 SUPER_ADMIN 旁路首次在生产生效，且落在推送通道上

- **path_id** `P-FANOUT-PUBLISH` / **state_id** `S-SUPER-ADMIN-SUBSCRIBER`
- **事实**：独立枚举全仓 11 个 `.canAccess(` 调用点，改动前的 9 个**全部**是 3 参形式，
  即恒传 `Set.of()`（`VisibilityChecker.java:17-19`）——`:23` 的 SUPER_ADMIN 旁路在生产中
  从未被激活过。本次新增的两个 5 参调用点是**第一次**把真实平台角色喂进去。
- **影响**：订阅了自己既不拥有、也无 namespace 管理权的 PRIVATE / hidden skill 的
  SUPER_ADMIN，现在会收到含 `skillName` / `slug` / `namespace` / `version` 的通知（sink `S1`）。
- **校准（避免夸大）**：这不构成新的机密性突破——`SecurityAuditController.java:101-103`
  已有 `SUPER_ADMIN || SKILL_ADMIN` 短路，平台管理员读 skill 元数据是仓库既有模式。
  实质变化是**通道**：从「主动去管理界面拉」变成「未经请求地推进个人通知收件箱」。
- **为什么仍需处置**：仓库对这件事有明确的反向表态——
  `SkillSearchAppService.java:160-163` 的 `hasPlatformWideReadAccess` 直接 `return false`，
  注释写着 "Super admins should use a dedicated admin interface, not the public portal"。
  订阅 fanout 属于 portal 面，新行为与该表态冲突。
- **required_repair**：显式裁决 fanout 是否应尊重 SUPER_ADMIN 旁路，并把结论写进 PR 正文；
  若倾向对齐 search 的表态，则在 resolver 层不传平台角色。
- **status** OPEN

## 独立确认为正确的行为（Pass 1 正向结论）

以下各条均由源码独立重建，非采信任何自述。运行时事实仍待 QA。

1. **R2 不误伤，源码层可证**：`:43` `LinkedHashSet` 保序去重，`:64` `filter` 保序，
   中间无重排。对全体可读订阅者，输出与输入**逐元素一致且顺序一致**。
2. **ARCHIVED namespace 规则与上游读闸同构**：`VisibilityChecker.java:50-54` 的
   「ARCHIVED 且非成员 ⇒ 拒」与 `SkillQueryService.java:204-206` / `:814-816` 一致。
3. **FROZEN 不作读障碍是对的**：独立枚举全部 `NamespaceStatus.FROZEN` 使用点
   （`ReviewService:361`、`PromotionService:337`、`SkillPublishService:167/663`、
   `NamespaceAccessPolicy:37`），全部是**写**障碍。新 overload 不判 FROZEN，与仓库语义一致。
4. **账号状态闸的位置是对的**：`UserAccount.isActive()` 仅 `ACTIVE`
   （`UserStatus` = ACTIVE/PENDING/DISABLED/MERGED）。登录侧
   `IdentityBindingService.java:74-79` 已拦 PENDING/DISABLED，故该闸在 subscribe 路径上
   几乎恒真；它的真实价值在 fanout 路径——订阅后才被停用的陈旧订阅者，
   正是维护者点名的 disabled-user 场景。位置正确。
5. **publisher / actor 跳过行为保留**：`:90-92`、`:117-119` 未变。
6. **批量取数达标**：3 次批量查询，fan-out 循环内无逐个查库。
7. **两处 `Collectors.toMap` 无重复键风险**：`namespace_member` 与 `user_role_binding`
   均有对应 `@UniqueConstraint`。
8. **既有 9 个 `canAccess` 调用点与两个既有签名均未被改写**，无 caller 迁移。
9. **#738 引入的新 producer 已被覆盖**：`SkillReviewSubmitService.java:150` 虽不在写集内，
   其 `SkillPublishedEvent` 同样流入新过滤器。
10. **`SearchIndexEventListener` 未被介入是对的**：搜索有独立
    `SearchVisibilityScope`，不应由本次改动接管。

## 本遍的 UNVERIFIED（不得被下游解释掉）

- `U1` 全部结论为静态重建。运行时是否如此由 QA 负责，Pass 1 不代偿。
- `U2` 未读 discovery inventory（见 Blindness 声明），故未做 candidate 对齐；
  `inventory_candidate_ids` 留空，Pass 2 补。
- `U3` 未枚举反射 / 动态注册 / 框架代理导致的消费者。本遍的消费者集合由
  `@EventListener` / `@TransactionalEventListener` 注解 + 类型引用两路 grep 求得，
  对非注解注册路径无覆盖。
- `U4` 未评估 `skillhubEventExecutor` 的拒绝策略与异常吞没行为对 sink 的影响。
- `U5` 未审阅本 tree 内的测试断言质量——属 Pass 2 `EVIDENCE_CHALLENGE` 范围，
  本遍刻意不进入，以免绿证据锚定推理。

## pass1_source_first（供 Pass 2 组装 receipt）

```
reviewed_path_ids     = [P-SUBSCRIBE, P-FANOUT-PUBLISH, P-FANOUT-YANK,
                         P-PUBLISHER-SELF-NOTIFY, P-SEARCH-INDEX]
reviewed_sink_ids     = [S1-dispatch-recipient, S2-subscription-row,
                         S3-subscribed-event, S4-http-403-i18n]
reviewed_state_ids    = [S-LATEST-VERSION-NULL, S-SKILL-ARCHIVED, S-NS-ARCHIVED,
                         S-NS-FROZEN, S-NS-MISSING, S-ACCOUNT-DISABLED,
                         S-ACCOUNT-PENDING, S-ACCOUNT-MERGED, S-MEMBER-REMOVED,
                         S-SUPER-ADMIN-SUBSCRIBER, S-PLATFORM-ROLE-DERIVATION,
                         S-VISIBILITY-PRIVATE, S-VISIBILITY-NAMESPACE-ONLY, S-HIDDEN]
independent_probe_ids = [PR-YANK-LAST-PUBLISHED-VERSION, PR-ARCHIVED-SKILL-SUBSCRIBE,
                         PR-SUPER-ADMIN-PRIVATE-FANOUT, PR-ORDER-PRESERVING-NO-FALSE-DENY,
                         PR-DISABLED-SUBSCRIBER-FANOUT]
uncovered_ids         = [U1, U2, U3, U4, U5]
inventory_candidate_ids = （本 Run 有意不填，见 Blindness 声明）
```

## 交接

- 状态：Pass 1 冻结。**不出四维 verdict、不请求 Review Gate**（Pass 1 依定义不持有
  `plan_sha256` / `qa_sha256` / `pass2_challenge`，`review-gate.v1` 无法也不应在此产出）。
- 产物：本分录，绑定 tree `9221956`。
- 开放 finding：1 HIGH（`R-P1-01`）、2 MEDIUM（`R-P1-02` `R-P1-03`）、4 LOW。
  按交付协议，一个可复现 HIGH 不能被其他维度 PASS 覆盖。
- 下一步：调度按路由裁决。`R-P1-01` 主类 `PLAN_GAP` ⇒ Planner 重新建模；
  `R-P1-03` `R-P1-04` `R-P1-05` 主类 `IMPLEMENTATION_DEFECT` ⇒ Coder；
  `R-P1-02` `R-P1-07` 需要的是显式 disposition 而非改码。
- 给 QA（不越界，仅提示探针，QA 章程已独立冻结）：`PR-YANK-LAST-PUBLISHED-VERSION`
  是本遍唯一 HIGH 的可执行判据，且本 tree 内 `LatestVersionId(null)` 零命中。
- 需要决策：`R-P1-01` 的产品语义（yank 后是否仍应通知）、`R-P1-07` 的 SUPER_ADMIN
  推送边界——两者都应以选择题形式留给上游维护者，不得由本链自行选定。
- 未解决：`U1`–`U5`。tree 一旦发生非 ledger 变化，本分录连同全部 finding 失效并重跑。
