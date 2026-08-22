---
ts: 20260822-150358
agent: planner
action: HD-30 plan r2 evidence-contract addendum
subject_base_tree: c81497cf7eb98f0568b8b30c9af1e67797bca27a
returned_candidate_tree: 922195602c92f0f420e17df9021ae39727c95f8d
verdict: FROZEN
---

# HD-30 plan-r2 加绑条件 2：非 subscriber runtime evidence contract

本分录只具体化 `planner/20260822-141107-plan-r2.md` §2.2 已批准的「至少一条
non-subscriber event listener runtime regression」，并替换人工批准中不适用的测试指派。
本 Run 为 `187a9c90-5bc7-4128-a7e9-bfe7fa3cdcda`，context 为
`01a0245b-3485-7d62-8601-0e432a47f693`。

## 1. 取代范围

`server/skillhub-app/src/test/java/com/iflytek/skillhub/controller/portal/PromotionApprovalFlowIntegrationTest.java`
不得作为 `NotificationService.create(REQUIRES_NEW)` 的加绑证据：它以 `@MockBean` 替换
`NotificationDispatcher`，不会到达 `NotificationService.create`、Notification row 或 SSE；
`REQUIRED` 与 `REQUIRES_NEW` 在该测试中均会恒绿。该文件可以继续作为普通 promotion workflow
regression 运行，但不能关闭本 evidence contract。

本分录不改变 R1–R5、任何具名 invariant/source-contract 决策、candidate disposition、path id、
work partition 或写集 path。冻结机器 artifact
`planner/20260822-141107-plan-gate-r2.json` 及其 SHA-256
`61d54ad3dc94ae60ea546aa85322aea6e946d2c4ec064428f2525ffcebf04238` 保持不变；不产生新的
Plan Gate artifact。

## 2. 指定 probe

- Path：
  `server/skillhub-app/src/test/java/com/iflytek/skillhub/listener/SubscriberNotificationRuntimeIntegrationTest.java`
- Symbol：
  `promotionApproved_saturatedConfiguredExecutor_persistsCommittedRowBeforeRealManagerSse`
- 单测命令：

```bash
cd server && ./mvnw -pl skillhub-app -am \
  -Dtest=SubscriberNotificationRuntimeIntegrationTest#promotionApproved_saturatedConfiguredExecutor_persistsCommittedRowBeforeRealManagerSse \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

该 probe 必须走完整真实链：

```text
proxied PromotionService.approvePromotion @Transactional
  -> injected ApplicationEventPublisher publishes PromotionApprovedEvent
  -> registered NotificationEventListener.onPromotionApproved
  -> configured skillhubEventExecutor in live CallerRuns saturation
  -> real NotificationDispatcher
  -> NotificationService.create(REQUIRES_NEW)
  -> committed Notification row
  -> real SseEmitterManager with recording SseEmitter transport
```

这是 non-subscriber 路径：目标 event/category 必须为
`PROMOTION_APPROVED` / `PROMOTION`，recipient 为 promotion submitter；不得以
`SkillPublishedEvent` 的 subscriber fan-out 充当证据。

## 3. Fixture 与运行前置条件

1. 以真实 repository 持久化 PENDING promotion、source skill/version、target namespace 与 submitter；
   调用 Spring 容器取得的真实 `PromotionService` proxy，不得直调 listener 或手工发布替代事件。
   reviewer 与 source skill owner 必须不同、fixture 不建立 subscriber，使同一 approval 产生的
   `SkillPublishedEvent` 不制造额外 Notification row；以真实 permission checker 可接受的
   `SKILL_ADMIN` reviewer 输入完成 approval，不借此裁决 R-P1-07 的 SUPER_ADMIN 选择题。
2. 从 Spring bean 取得生产 `skillhubEventExecutor` 对应的 `ThreadPoolTaskExecutor`。用阻塞 filler
   tasks 占满 4 个 active workers 与 100 个 queue slots；执行前读回并断言
   `activeCount == maxPoolSize`、`remainingCapacity == 0` 且 executor 未 shutdown。不得换用 synthetic
   或 rejecting executor。
3. 通过真实 `SseEmitterManager.register(submitterId)` 注册 recording emitter。测试 transport 可在
   已批准路径
   `server/skillhub-app/src/test/java/com/iflytek/skillhub/notification/sse/RecordingSseEmitterTestConfiguration.java`
   使用 package-private emitter-factory constructor 注入 recording `SseEmitter`；必须真的经过
   manager 的 `register` 与 `push`，只在 `PROMOTION_APPROVED` 的 `notification` event 上用 latch
   阻塞，不能把 manager 或 dispatcher mock/stub 掉。
4. `NotificationEventListener`、`ApplicationEventPublisher`、transaction manager、configured executor、
   `NotificationDispatcher`、`NotificationService`、Notification persistence 与 `SseEmitterManager`
   均不得使用 Mockito、`@MockBean`、direct invocation 或 annotation reflection 替代。

## 4. 可验收断言

在专用 publisher thread 调用真实 proxy，并等待 recording SSE 进入阻塞 latch 后，必须同时断言：

1. publisher future 尚未完成，且 recording sink thread id 等于 publisher/commit thread id，证明
   生产 `CallerRunsPolicy` 生效；
2. 从独立 transaction/connection 可见 promotion 已为 APPROVED 且 target skill 已落库，证明 source
   transaction 已提交，而不是在 commit 前阻塞；
3. 从独立 transaction/connection 已可见且仅可见一条该 submitter 的
   `category=PROMOTION`、`eventType=PROMOTION_APPROVED`、`entityType=SKILL`、
   `entityId=sourceSkillId` Notification row，证明 row commit 先于 SSE；
4. 该 row 的 body 包含与 fixture 一致的 skill、promotion 与 reviewer 字段。

释放 SSE latch 后还必须断言：

1. publisher future 正常完成；
2. SSE payload 的 row id、recipient、category、event type、entity tuple 与独立读回的 row 完全一致；
3. recording transport 的顺序为该 row 已独立可见后才发送 `notification` event，且只发送一次；
4. `finally` 无条件释放 filler tasks、等待 executor drain，并 complete `register` 返回的 emitter、
   让 manager 的 completion callback 完成 deregister，防止污染同 JVM 的后续测试。

该 probe 的反事实必须成立：若仅把 `NotificationService.create` 恢复为 `REQUIRED`，ordinary async
thread 仍可能自开 transaction 而恒绿；但在本 probe 的 saturated CallerRuns after-commit 路径中，
它会加入已提交但尚未 cleanup 的 source transaction，因而第 3 条「独立可见的已提交 row」必须失败。
不得以放宽断言、改用普通 executor capacity 或把 SSE 移出真实 manager 来消除该失败。

## 5. 写集与 RETURN 边界

本指定只使用 plan-r2 已归属单一 work unit 的两个测试 path，因此写集增量为 **0 path**。若实现该
probe 需要修改上述两个 path 以外的任何文件，Coder 必须停止并回 Planner；不得自行扩大 fixture、
生产代码或另一测试文件的写集。R1–R5 与 `R-P1-02` / `R-P1-07` 的既有裁决和未决边界一律不动。
