---
ts: 2026-08-21T09:43:07Z
agent: reviewer
action: review pass1 BLIND_BEHAVIOR_AUDIT
tree: e8aca2a43467dad0da969c182632a7b791f90621
verdict: RETURN
---

# Review Pass 1 — `BLIND_BEHAVIOR_AUDIT`

冻结独立行为图。本文件在读取任何 Plan / QA / Coder 解释 / 旧 PASS 之前完成，作为 Pass 2 `EVIDENCE_CHALLENGE` 的锚。

## 0. 隔离与取证条件

### Run / context provenance

| 项 | 值 |
|---|---|
| agent | 评审 `a8512fa6-b2e1-4a1c-9854-4d3377b13703` |
| Pass 1 run（Multica task） | `693d946c-f84c-4ce1-b66a-c90fd8324dab` |
| Pass 1 context（本地会话） | `2bc9c466-f4ea-4168-bbda-d9cca643a8ca` |
| Pass 1 交付评论 | `fc53490a-2b9c-4488-b017-638e5e31c527` @ 2026-08-21T09:47:08Z |
| 本产物补推 run | 与 Pass 1 同 task / 同 context 的后续 turn（容器已回收重建，工作副本重新 checkout）；未产生新的审查结论，仅落地既有产物 |
| read_only | true —— 本 Pass 未修改任何业务文件；写入仅限 `multica-ledger/.../reviewer/` 与 `index.md` 自有行 |

Pass 1 的 run 与 context 均由本 Agent 独占，与 实现 / 验证 的 run 无交集；调度可用各方交付评论的 `source_task_id` 独立比对，无需采信本表自述。

补推说明：Pass 1 的分录已于 09:43 在当时的容器内 commit（`238d24ac`，基于 `3e056a5f`），但该容器在 turn 之间被回收，commit 未推送。本次从 bare repo 取回同一 commit 并 cherry-pick 到 `…-r2` 当前 head，产物内容与 09:43 冻结时逐字节一致，仅追加本 provenance 小节。调度两次取证（09:45 `3ce59587` / 09:51 `bd2ba3b9`）读到的缺失是真实的，不是竞态。

### 取证条件

- 只读工作树 `/workspaces/hd28-review-pass1`，detached HEAD `bfcb4fe5`，`git rev-parse HEAD^{tree}` = `e8aca2a43467dad0da969c182632a7b791f90621`，与调度冻结值逐字符一致；`git status --porcelain` 为空。
- 该工作树天然不含 `coder/20260821-091620-red-green-regression.md`（该文件在后续 commit `b80982e8` 才加入），blindness 由树结构保证而非自觉遵守。
- **审查主体是 exact tree 与 upstream base 的全量差分**，不是 R2 增量。merge-base `d2403bb5911953b8f53e62c3f0a9edc291363944`，`d2403bb5..bfcb4fe5` 非 ledger 面 19 文件 / +1054 −8。R2 增量（`23658e0f..bfcb4fe5`）5 文件 / +111 −0 只是其中一段。
- inventory 未作为完整性上界使用：行为面由 `SkillPublishedEvent` / `SkillVersionYankedEvent` 的**全部构造点与全部消费点**独立反查得出，不依赖任何既有清单。

### 污染披露（必须计入 Pass 2 的独立性折扣）

1. 早期一次仓库级 `grep -rn "error.skill.subscription.noPermission" .` 未排除 `multica-ledger/`，使约 6 行 `planner/` 与 `leader/` 分录文本进入上下文（键名出现位置，非结论段）。此后所有检索均排除 `multica-ledger/`。
2. Council lens 的 `must_answer` 以 `jq '.council_nomination'` 提取，该键同时带出 `rationale` / `referenced_path_ids` / `referenced_invariant_ids`，属于超出 `must_answer` 的少量读入。Plan 正文未读。

以上两项不足以推翻本 Pass 的独立性（下列 finding 全部有一手代码位置与可执行证伪路径），但必须留痕。

## 1. 独立行为图

### 1.1 事件构造点（全量反查）

`SkillPublishedEvent` 在 main 源码中有 4 个构造点：

| # | 位置 | 触发场景 | 本 PR 是否新增 |
|---|---|---|---|
| P1 | `ReviewService.java:233` | 评审通过发布 | 否 |
| P2 | `SkillPublishService.java:572` | 常规发布 | 否 |
| P3 | `PromotionService.java:258` | 晋级发布 | 否 |
| P4 | `SkillReviewSubmitService.java:150` | **PRIVATE skill confirm-publish** | **是** |

`SkillVersionYankedEvent` 在 main 源码中有且仅有 1 个构造点：`SkillGovernanceService.java:283`，本 PR 将其扩为 4 元并**硬编码 `wasPublished = true`**。

### 1.2 事件消费点（全量反查）

| # | 位置 | 消费事件 | 最终 sink |
|---|---|---|---|
| C1 | `NotificationEventListener.onSkillPublished:59` | `SkillPublishedEvent` | `dispatcher.dispatch` → notification 行 + SSE，仅当 `publisherId == skill.ownerId` |
| C2 | `NotificationEventListener.onSkillPublishedForSubscribers:75` | `SkillPublishedEvent` | 订阅者扇出，经新增 eligibility 过滤 |
| C3 | `NotificationEventListener.onSkillVersionYankedForSubscribers:103` | `SkillVersionYankedEvent` | 同上 |
| C4 | `SearchIndexEventListener.onSkillPublished:31` | `SkillPublishedEvent` | `searchRebuildService.rebuildBySkill` → 写 `skill_document` |

四个消费点均为 `@TransactionalEventListener(AFTER_COMMIT) + @Async("skillhubEventExecutor")`，各自独立提交到线程池，无共享可变状态、无顺序依赖、无跨消费者异常传播。**C4 与 C2/C3 共享同一事件对象**——这是本次改动最重要的、且未被声明的耦合面（见 R4）。

### 1.3 P4 的可达性约束（决定性）

`SkillReviewSubmitService.confirmPublish` 在发事件之前有一道硬约束（`:125`）：

```java
if (skill.getVisibility() != SkillVisibility.PRIVATE) {
    throw new DomainBadRequestException("error.skill.confirm.notPrivate");
}
```

因此 **P4 这个本 PR 新增的事件发射点，只在 skill 为 PRIVATE 时可达**。

### 1.4 新增授权过滤链

`SubscriptionRecipientEligibility.eligible` → 两次批量读（`accountRepository.findByIdIn`、`memberRepository.findByNamespaceIdAndUserIdIn`）→ 逐 id 交 `SubscriptionMetadataAccessPolicy.canAccess` 判定。`canAccess` 的 fail-closed 顺序：`account == null || !account.isActive() || namespace == null` → false；ARCHIVED namespace 且无 role → false；hidden → `owner || manager`；`!yankedPublication && latestVersionId == null` → `owner`；最后按 visibility：`PUBLIC → true` / `NAMESPACE_ONLY → role != null` / `PRIVATE → owner || manager`。

判定正确，且 `NotificationEventListenerTest` 与 `SubscriberNotificationSinkTest` 都用**真实**的 `SubscriptionRecipientEligibility` + `SubscriptionMetadataAccessPolicy`（非 mock），mock 只落在 repository / `NotificationService` / `SseEmitterManager` 边界。这是本次改动做得最扎实的一段，明确记 PASS。

## 2. Findings

### R1 — `INVARIANT_VIOLATION`（SECURITY） / HIGH / 继承自 base，**非本树引入**

`GET /api/v1/me/subscriptions` 对订阅列表**不做任何可见性授权**。链路：`MeController.listMySubscriptions` → `MySkillAppService.listMySubscriptions:119` → `skillSubscriptionRepository.findByUserId` → `skillRepository.findByIdIn` → `JpaMySkillQueryRepository.getSkillSummaries:36`。全链无 deny 分支；`toSummaryResponse` 无条件返回 slug / displayName / summary / visibility / status / 计数 / namespace slug / updatedAt，其中 `projectForViewer(skill, currentUserId, Map.of())` 传的是**空 role map**，只影响 version 投影，不影响上述顶层元数据。

复现：用户 U 在 skill S 为 PUBLIC 时订阅 → owner 将 S 改为 PRIVATE → U 调 `GET /me/subscriptions` → 响应含 S 的 slug、displayName、summary、namespace slug 与 `visibility: "PRIVATE"`。

`git diff --name-only d2403bb5 bfcb4fe5` 不含任何 `MySkill*` / `MeController` / star 相关文件——**该缺陷完全继承自上游 base，本树既未引入也未加重**。

裁定：**不以此阻断 tree `e8aca2a4`**，但状态 OPEN，须作为独立条目上报，且必须进入 F3 披露（见 R2）。回答 `trust-boundary` must_answer #3：构成可复现的 PRIVATE 元数据泄露；**未被阻断**；但也不属于本 PR 可用 non-goal 推掉的范围，因为它改变了 F3 的决策前提。

### R2 — `PLAN_GAP` / MEDIUM

F3 把"存量订阅行是否清理"作为迁移问题交给维护者判断时，把存量行建模为**仅影响通知**。R1 证明存量行同时是一条**活的读通道**。维护者据此在信息不完整的模型上做决策。

修复：F3 段落必须写明"保留的订阅行不是惰性的——它同时使订阅者可通过 `GET /me/subscriptions` 读到该 skill 的元数据（含 PRIVATE）"，并说明该读通道在本 PR 范围之外。

### R3 — `IMPLEMENTATION_DEFECT` / MEDIUM — `wasPublished` 是编译期常量

`SkillGovernanceService.yankVersion` 在发事件之前已有前置校验（`:265`）：

```java
if (version.getStatus() != SkillVersionStatus.PUBLISHED) {
    throw new DomainBadRequestException("error.skill.version.notPublished", version.getVersion());
}
```

而唯一的发射点（`:283`）硬编码 `true`。因此 **`wasPublished == false` 在生产中不可达**：`canAccessYankedPublication` 的 `wasPublished &&` 短路、以及 `canAccess` 中 `!yankedPublication && latestVersionId == null` 的 false 臂，均为生产死代码。

后果不是"多余字段"，而是**证据面虚高**：`SubscriberNotificationSinkTest.yankWithoutVerifiedPublishedPreStateProducesNoPersistenceOrSse` 与 `@ValueSource(booleans = {true, false})` 参数化断言的是系统无法产生的状态，却在覆盖率与"fail-closed 已验证"的叙述中被计为证据。

证伪路径：删除 `wasPublished` 字段、把 `canAccess` 的 `yankedPublication` 直接置 `true`，生产行为零变化——若成立即证明该分支为死码。

### R4 — `PLAN_GAP`（SECURITY 次级标签） / MEDIUM — 新增 PRIVATE 数据落盘面

本 PR 新增的事件发射点 P4（`SkillReviewSubmitService:150`）只在 PRIVATE skill 上可达（§1.3）。该事件被 C4 消费：`SearchIndexEventListener:31` → `PostgresSearchRebuildService.rebuildBySkill:114` → `toDocument:299`。

`toDocument` **没有任何 index-time 可见性过滤**——只在 namespace 查不到时返回 `Optional.empty()`。它把 `displayName`、`summary`、`keywords`、`searchText`（`buildSearchPayload:123` 还会把 latest version 的 frontmatter 与 compliance snapshot 文本摊平进去）连同 `visibility = "PRIVATE"` 一并写入 `skill_document`。

即：**本 PR 之前，PRIVATE skill 的 confirm-publish 从不触达搜索索引；本 PR 之后，它每次都写。而这是一个宣称目的为"收紧 PRIVATE 元数据暴露"的改动。**

查询侧仍然安全：`PostgresFullTextQueryService` 的可见性谓词只放行 `d.visibility = 'PUBLIC'` 或（`NAMESPACE_ONLY` 且 `d.namespace_id IN :memberNamespaceIds`），叠加 `d.status='ACTIVE'` / `s.status='ACTIVE'` / `s.hidden = FALSE` / 归档 namespace 排除；`skillhub-search` 在 R2 增量中零差分。**因此没有查询可见的泄露**，回答 `trust-boundary` must_answer #4 与 `event-state-consistency` must_answer #3：搜索侧不构成绕过。

但静态数据面确实被扩大，且改动与其测试都未提及。归类为 PLAN_GAP 而非 INVARIANT_VIOLATION，因为不变量未被破坏，被破坏的是"本改动的行为面已被完整建模"这一前提。

修复：要么在 `toDocument` 加 index-time 过滤，要么在 PR 正文显式声明"PRIVATE skill 的 confirm-publish 现在会进入 `skill_document`，查询侧仍按 visibility 拒绝"，并补一条断言 `searchIndexService.index(...)` 在 `confirmPublish` 下的调用事实。

### R5 — `PLAN_GAP` / MEDIUM — 新增的 WARN 覆盖不到它所声称的事故类

`namespace == null` 实际上不可达：`V2__phase2_skill_tables.sql` 中 `namespace_id BIGINT NOT NULL REFERENCES namespace(id)` 且**无 `ON DELETE` 子句**（即 NO ACTION / RESTRICT），无后续 migration 摘除该 FK，`NamespaceService.deleteNamespace` 另有 `assertNoDependentData` 前置，`Namespace` 无软删除。`Optional.empty()` 需要一次 FK 违约才能出现。

而改动所称的动机事故——一次 namespace 加载抖动——在代码里表现为 `namespaceRepository.findById` **抛出** `DataAccessException`，它在新增 null 检查**之前**就已逃逸。全仓无 `AsyncConfigurer` / `AsyncUncaughtExceptionHandler` 实现（`AsyncConfig` 只定义了 `ThreadPoolTaskExecutor`），异常落到 Spring 默认的 `SimpleAsyncUncaughtExceptionHandler`，记的 ERROR **不含 skillId / namespaceId**。

即：真正会发生的那一类事故，诊断性没有改善；被改善的那条路径，生产中打不出来。

证伪路径（本树已自带脚手架）：`NotificationEventListenerTest.publishFanoutFailsClosedBeforeDispatchWhenNamespaceReadFails` 已经让 `findById` 抛异常——在该用例上加断言"未产生任何携带 skillId / namespaceId 的 WARN"，即可复现本 finding。

### R6 — `IMPLEMENTATION_DEFECT` / LOW — 同一事件的两个消费者对同一前置条件给出不同可观测性契约

同为 `namespaceRepository.findById` 落空：`NotificationEventListener:80/110` 打 WARN 并带 ID；`PostgresSearchRebuildService.toDocument:300-303` 静默返回 `Optional.empty()`，不记任何日志。若该条件真的触发，运维只会看到通知侧一条 WARN，索引侧的跳过完全不可见。回答 `event-state-consistency` must_answer #2 的一半：`Optional.empty` 与 repository 异常在**设计上被当作同一件事**（都走 fail-closed），但在**可观测性与重试语义上**并未对齐——异常路径没有 sink 语义可言，它直接逃逸。

### R7 — `IMPLEMENTATION_DEFECT` / LOW — WARN 断言了自己没有验证的结果

`"Subscriber notification skipped because namespace was not found"` 在 `currentRecipients(...)` **执行之前**输出，且刻意没有 `return`。它陈述了一个尚未观察到的结果。当前恰好成立（policy 对 `namespace == null` 全拒），但这是两处代码的巧合而非同一处的事实。若 policy 未来放宽，日志将在事故现场主动误导。

修复：只记观察到的事实（namespace 未找到），并在过滤后补记实际 recipient 数；同时加注释锁定"此处刻意不 early return"的理由，否则后续维护者极易顺手加上 `return` 而改变 fail-closed 之外的语义。

### R8 — `TEST_GAP` / MEDIUM — 消息键两端未被任何测试绑定

树内没有任何断言把 `SkillSubscriptionService.java:54` 抛出的 code 与 bundle 中注册的键绑在一起：

- `SubscriptionMessageBundleTest` 用自己硬编码的 `private static final String CODE = "error.skill.subscription.noPermission"` 去查 bundle——它验证的是"bundle 里有这个键"，与生产抛什么无关。
- `SkillSubscriptionServiceTest:149/169/210` 只断言 `isInstanceOf(DomainForbiddenException.class)`，从不断言 `messageCode()`。
- `SkillSubscriptionControllerTest` 无 403 / message 断言。

反例：把任一侧的键改名，全部测试保持绿，生产 403 body 退化为裸键。修复成本一行：在服务层断言 `messageCode()`，或加一条打到 403 body 的切片测试。

### R9 — `TEST_GAP` / LOW — bundle 测试绕开了 Boot 装配

`SubscriptionMessageBundleTest` 自建 `ResourceBundleMessageSource` 并 `setBasename("messages")`，而生产走 `application.yml` 的 `spring.messages.basename: messages`。改动该配置会让生产回退到裸键而测试仍绿。

### R10 — `IMPLEMENTATION_DEFECT` / LOW — 新增批量读无分块，且落在 CallerRuns 的提交线程上

`SubscriptionRecipientEligibility.eligible:43/45` 把完整候选集直接交给 `findByIdIn(ids)` 与 `findByNamespaceIdAndUserIdIn(nsId, ids)`，无分块；上游 `findSubscribersBySkillId` 不分页。Postgres 绑定参数上限 65535，订阅者超过该量级时整次扇出直接失败——fail-closed 成立，但按 R5 该失败无 skillId / namespaceId 日志，静默。

叠加面：`AsyncConfig` 的 `skillhubEventExecutor` 用 `CallerRunsPolicy`（core 2 / max 4 / queue 100）。队列饱和时 `@Async` 退化为在**事务提交线程**上同步执行，而本 PR 恰好给每个订阅者事件新增了两次同步 DB 往返，抬高了触达该退化点的概率。

### R11 — `IMPLEMENTATION_DEFECT` / LOW — namespace 被读两次

新代码取到 `namespace` 用于授权判定后，`bodyWithSkill()` 又独立 `namespaceRepository.findById` 一次用于 payload 的 `namespace` 字段。授权决策与载荷内容来自同一行的两次独立读取——正是改动所声称要应对的那种抖动下，两者可以不一致。

## 3. Council — 三 lens 逐条作答

### 3.1 `trust-boundary`（Bruce Schneier-style review，dissent_role: false）

> 以下为指定视角风格的评审，非本人立场，不代表其真实观点。

1. **两条 null-namespace WARN 是否只含 skillId / namespaceId、无候选订阅者身份？** 是。格式串只有两个占位符，实参为 `skill.getId()` / `skill.getNamespaceId()`；`NotificationEventListenerTest` 用 `subscriber-pii-sentinel` 作哨兵并断言 `.doesNotContain(...)`。补充事实：`NotificationDispatcher` 本就在 DEBUG / WARN 记 `recipientId`，新日志严格更保守。**PASS。**
2. **removed / cross-namespace 在 notification 行与 SSE 两个 sink 上是否都为零？** 是。`SubscriberNotificationSinkTest` 用真实 `NotificationDispatcher(notificationService, preferenceService, sseEmitterManager)`，在 `publishPersistsAndPushesOnlyCurrentEligibleNonPublisherAcrossAuthorizationMatrix` 中以 8 元候选矩阵（publisher / current-admin / stale-removed / inactive / missing / private-member / cross-namespace / platform-super-admin）断言只有 `current-admin` 落到 `notificationService.create(...)` 与单条 SSE。断言到达最终 recipient + payload，不是 mock 的 dispatcher，也不是计数。**PASS。**
3. **`retained-private-ordinary-member` 的 `SkillSummaryResponse` 是否构成可复现的 PRIVATE 元数据泄露；若构成，是否已阻断而非以 F3 non-goal 延后？** 构成，且**未阻断**。见 R1（可复现路径已给出）。同时按仓库安全不变量，它不能被 Plan non-goal 排除——但它继承自 base，本树未引入，故不阻断本树，改为 R1 + R2 两条独立路由。**RETURN。**
4. **`SkillPublishedEvent` 的三个消费者是否有绕过当前授权边界的 recipient-visible payload？** recipient-visible 层面没有：C1 受 `publisherId == ownerId` 约束；C2 受新 eligibility 约束；C4 的产物只经查询侧可见性谓词暴露，PRIVATE 被拒。**但 C4 存在 recipient 不可见的落盘扩面**，见 R4。**RETURN（降级到 PLAN_GAP）。**

补充（本视角主动提出、不在 must_answer 内）：`subscribe` 的 403-vs-404 存在性预言机为既有行为，本次未改变，不作为本树 finding。

### 3.2 `event-state-consistency`（Martin Kleppmann-style review，dissent_role: false）

> 以下为指定视角风格的评审，非本人立场，不代表其真实观点。

1. **搜索重建是否只从已提交的最新状态出发？** 是。C4 为 `AFTER_COMMIT`，`rebuildBySkill` 用 `skillId` 重新 `findById` 拉取权威行，不信任事件载荷；`resolveLatestVersion` 走 `skill.getLatestVersionId()`。两处 `setVisibility`（`ReviewService:227` / `SkillPublishService:566`）后面都紧跟一次 `SkillPublishedEvent` 发布，因此不存在"改了可见性但索引里留着旧 visibility"的陈旧泄露窗口。**PASS。**
2. **`namespace Optional.empty` 与 `namespace repository exception` 是否被设计错误地等同？** 在 fail-closed 结果上二者一致（都不发通知），这没问题；**错误在于二者被赋予了完全不同的可观测性，且改动只装备了不可达的那一条**。见 R5、R6。这是本 lens 最实质的发现。**RETURN。**
3. **搜索查询是否对匿名 / 普通成员 / 平台级调用者拒绝 PRIVATE？** 是，谓词在 SQL 层，无调用方旁路。**PASS。**（落盘面另见 R4。）
4. **一个异步消费者的失败能否阻塞、伪造或重复另一个的最终副作用？** 常规路径不能：四个消费点各自独立提交任务，无共享状态、无顺序依赖，异常不跨任务传播。**但 `CallerRunsPolicy` 下存在一个真实的耦合窗口**：队列饱和时任务退化为在提交线程上同步执行，`afterCommit` 阶段抛出的异常会向提交方传播，可能影响同一提交上下文中后续同步器的执行。本 PR 抬高了触达该窗口的概率（每事件 +2 次同步 DB 往返）。见 R10。**条件性 RETURN（LOW）。**

补充：`SkillVersionYankedEvent` 没有任何搜索侧消费者。yank 之后 `skill.latestVersionId` 可能变更，但 `skill_document` 的 keywords / searchText 仍派生自被 yank 的版本，索引陈旧。此为继承缺陷（yank 事件本就存在），但本 PR 正在扩展该事件记录却未顺带对齐，记为观察项而非 finding。

### 3.3 `evidence-dissent`（Kent Beck-style review，dissent_role: **true**）

> 以下为指定视角风格的评审，非本人立场，不代表其真实观点。本席位职责是反对，不是配平。

1. **f1 bundle 探针是否加载真实 bundle、并在 `23658e0f` 上以裸键 RED？** 加载的是真实 `.properties`（自建 `ResourceBundleMessageSource`，basename `messages`），`getMessage(CODE, null, CODE, locale)` 的 defaultMessage 即 CODE，配合 `.isNotEqualTo(CODE)`，在旧树上确会 RED。RED 机制成立。**但它证明的命题比它看起来的弱**——见 R8、R9：它证明"bundle 里有这个键"，不证明"生产抛的是这个键"，也不证明"生产用这个 basename"。
2. **null-warning 探针是否挂在真实 logger 上，断言 level + IDs + 哨兵缺失？** 是。`captureLogs()` 通过 logback `ListAppender` 挂到 `NotificationEventListener` 的真实 logger，`finally` 中恢复 level 并 detach；断言 `singleElement()` + WARN + `.contains("skillId=1","namespaceId=5")` + `.doesNotContain("subscriber-pii-sentinel")` + `verifyNoInteractions(dispatcher)`。机制无可挑剔。**但它把探针精度全部投在了一条生产不可达的路径上**（R5）。这是本席位最强的反对：探针质量高不等于探针位置对。
3. **非 null 的逐元素探针是否比较 recipient / eventType / payload 三元组而非计数？** 是。`SubscriberNotificationSinkTest` 逐个 `verify(notificationService).create(recipient, category, eventType, title, body, "SKILL", id)` 并 `assertSingleSse(recipient, eventType, body)`，body 是完整 JSON 字面量。**PASS，且这是本次证据面里最强的一段。**
4. **`SubscriberNotificationSinkTest` 是否断言在 `NotificationService` 持久化与 `SseEmitterManager` 传输、而非 mock 的 dispatcher？** 是。`@BeforeEach` 里 `new NotificationDispatcher(notificationService, preferenceService, sseEmitterManager)` 为真实对象，mock 只在其下游。**PASS。**
5. **本方案在什么条件下是错的？哪些仍然是绿的探针无法暴露 wording / recipient / PII / search-isolation 错误？**

   本方案错在**它把"改动的行为面"等同于"改动的 diff 面"**。三条具体条件：

   - 若 `SkillReviewSubmitService` 新增的事件发射被视为"只是补一个缺失的通知"，方案就是错的——它同时打开了 `SearchIndexEventListener`，而后者对 PRIVATE 无 index-time 过滤（R4）。**全部现有探针都无法暴露这一点**：没有任何一条断言触及 `searchIndexService.index(...)`，`SubscriberNotificationSinkTest` 的 mock 集合里根本没有搜索侧对象。search-isolation 的"绿"是未被测试，不是已被验证。
   - 若 `wasPublished` 被视为一个真实的运行时变量，方案就是错的——它是编译期常量（R3）。针对 `wasPublished=false` 的探针**永远绿，且永远无意义**，同时在覆盖率叙述里冒充 fail-closed 证据。
   - 若"消息键已本地化"被视为端到端事实，方案就是错的——键两端从未被绑定（R8）。**wording 错误无法被任何现有探针暴露**：改名任一侧，全绿。

   PII 维度是唯一真正闭合的：哨兵断言直接、位置正确、否定式覆盖。recipient 维度亦闭合（三元组逐元素）。**wording 与 search-isolation 两个维度目前是空的。**

   本席位不认为这是一次可以 PASS 的证据面。

无 `NOMINATION_MISS`：三个 lens 在本改动上都有实质检查面。

## 4. 四维 verdict

| 维度 | 结论 | 依据 |
|---|---|---|
| Intent | **RETURN** | 核心意图（扇出收敛到当前有权者）实现正确且证据扎实；但同一 PR 夹带了一个未声明的新事件发射点，其效果是扩大 PRIVATE 落盘面（R4），并且新增诊断能力打不到它所声称的事故类（R5）。 |
| Invariant / Security | **RETURN** | 无查询可见的新泄露，fail-closed 成立，日志无 PII。归因于本树的问题是 R4；R1（HIGH）为可复现的 PRIVATE 元数据读通道，但完全继承自 base，本树未引入亦未加重，故不阻断本树，单列路由。 |
| Evidence | **RETURN** | R3（探针断言生产不可达状态，虚高覆盖）、R8 / R9（wording 两端未绑定）、以及 search-isolation 维度零探针。 |
| Exact Tree | **PASS** | `HEAD^{tree}` 与冻结值逐字符一致，worktree 干净；审查范围覆盖 `d2403bb5..bfcb4fe5` 全量 19 文件，非仅 R2 增量。 |

**Overall：RETURN。**

无 uncovered ID。开放 finding：R1（HIGH，继承，不阻断本树）、R2 / R3 / R4 / R5 / R8（MEDIUM）、R6 / R7 / R9 / R10 / R11（LOW）。

Coder 自述的 1404 tests / 0 failures 与差分 fixture 双树 SHA-256 未在本 Pass 采信，本 Pass 的全部结论独立于该自述。

## 5. 未做的事

- **未开 Pass 2 `EVIDENCE_CHALLENGE`**。QA 产物齐备后另起 Run，读冻结后的本文件 + Plan + QA + 测试，专查 Plan 相对本行为图的遗漏、QA 是否只跑 Planner 给的例子、目标 RED 在旧行为上是否真会失败、mock 是否挡在最终边界之前。
- **未跑 Review Gate**。`review-gate.v1` 同时要求 `pass1_source_first` 与 `pass2_challenge`，Pass 2 未完成前无法产生合法 receipt，因此本 Run 不请求 Review Gate。

---

## 附录 A —— 冻结后追加（2026-08-21T10:06:48Z）

**本附录不修改 §1–§5 的任何内容。** Pass 1 的行为图与 finding 集在 09:43 冻结，逐字节未动。此处只回应调度在 `leader/20260821-095600-…` 中提出的两个需要 Reviewer 表态的问题，属于对既有 finding 的**修复面陈述**，不是新的审查结论。

### A.1 R8 的最小绑定面（提案，交调度校验，本 Agent 不自行扩写 write_set）

调度的判断正确：生产侧 `SkillSubscriptionService:54` 是内联字面量，两端绑定必然外溢至少一个文件。两条候选：

**方案 A（推荐 · 最小面 · 零生产改动）** —— 在 `SkillSubscriptionServiceTest` 已有的三处断言上补 `messageCode()`：

```java
assertThatThrownBy(...)
    .isInstanceOf(DomainForbiddenException.class)
    .extracting(e -> ((LocalizedDomainException) e).messageCode())
    .isEqualTo("error.skill.subscription.noPermission");
```

闭合性：改生产字面量 → 该测试 RED；改 bundle 键 → `SubscriptionMessageBundleTest` RED。两端各有一枚哨兵，任一侧单方漂移必被捕获。
外溢面：`SkillSubscriptionServiceTest` 一个文件，**生产代码零改动**。

**方案 B（不推荐）** —— 把字面量提取为生产侧常量，bundle 测试改为引用该常量。绑定更紧（改常量值 → bundle 查不到 → RED），但外溢到生产文件，且引入一个跨模块共享符号。R8 是 MEDIUM 级 TEST_GAP，不值这个代价。

倾向方案 A。是否采纳、以及 write_set 如何修订，由调度裁定。

### A.2 三个裁定选项对四维 verdict 的机械后果（供审批人参考，不构成建议）

调度已将 (a)/(b)/(c) 上呈审批人。本 Agent 不参与范围决策，仅陈述各选项落到 verdict 上的必然结果：

- **(a) 扩范围重开 Plan** —— R4 / R3 进入可修范围。R4 修复后 Invariant/Security 可清；R3 修复后 Evidence 少一条。R1 仍在树外，不受影响。代价是树必然变更，Implementation Gate PASS 与本 Pass 1 同时作废，需整轮重来。
- **(b) 另开 Issue，HD-28 按原范围收线** —— HD-28 的 finding 集收敛为 R5 / R2 / R8，三条均可在（修订后的）write_set 内闭合。这是唯一能让四维在**一轮**修复后全部到达 PASS 的路径。R4 / R3 / R1 转入新 Issue，其状态不再由本 Pass 的 verdict 承载。
- **(c) 作为「已知未决」写入 F3 正文并据此 READY** —— **需要审批人明确知道：披露改变的是维护者掌握的信息，不是树的行为。** R4 在 (c) 下仍然 OPEN，Invariant/Security 维度不会因为正文多了一段话而转 PASS。`review-gate.v1` 的 `overall` 只有 `PASS` / `RETURN` / `UNVERIFIED` 三态，没有「PASS with declared exception」。因此 (c) 的实际含义是：由审批人在 Gate 之外显式接受一条 OPEN 的 MEDIUM finding 进入 READY，而不是由 Reviewer 把它判成 PASS。本 Agent 不会为迁就 (c) 而调整 verdict。

R1 的披露方式属外部动作，本 Agent 不表态。
