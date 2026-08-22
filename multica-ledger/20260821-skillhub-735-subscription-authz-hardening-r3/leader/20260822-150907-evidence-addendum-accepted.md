# leader / 20260822-150907 / 加绑条件 2 evidence delta 受理

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 对象：`planner/20260822-150358-plan-r2-evidence-addendum.md`（`3812652e…d86fe29f`）
- 结论：**ACCEPTED**，加绑条件 2 闭合
- 不触发新 Plan Gate（理由见下，已实证）

## 机械核对（逐项读回，不采信自述）

| 项 | 自述 | 实读 | 结论 |
|---|---|---|---|
| 远端 head | `d3b6239e2161f55692f136cec36e49fa0a527224` | 同 | ✅ |
| head tree | `3c7252af251ba9df63569f86aff83ad96a860ed4` | 同 | ✅ |
| addendum SHA-256 | `3812652e…d86fe29f` | 同 | ✅ |
| plan-gate artifact digest | 仍为 `61d54ad3…bf04238` | 同 | ✅ |
| 非 ledger diff vs `fe6d1773` | 空 | 空 | ✅ |
| `server` subtree | — | `7d525002710465020565f6f8ecd73d2863c60f09`，与 `fe6d1773` 一致未动 | ✅ |

### 为何不需要重跑 Plan Gate

不是采信「artifact 未改」这句话，而是实测：`git diff 77d52c3d..d3b6239e -- planner/20260822-141107-plan-gate-r2.json` 为空，即机器 artifact 自人工批准那一刻起**逐字节未变**。已批准的 Gate 绑定（artifact digest / inventory digest / validator hash / Plan Run）全部继续成立，故不重跑、不重取批准。addendum 是 prose 层的证据指定，未进入机器 artifact。

### 写集增量为 0：已核，非采信

读回已批准 artifact 的 `work_partition[0].write_set`（24 条），delta 用到的两个 path 均已在其中：

- `server/skillhub-app/src/test/java/com/iflytek/skillhub/listener/SubscriberNotificationRuntimeIntegrationTest.java`（第 21 条）
- `server/skillhub-app/src/test/java/com/iflytek/skillhub/notification/sse/RecordingSseEmitterTestConfiguration.java`（第 22 条）

故「写集增量 0 path」成立，未越过已批准写集。

## 新 probe 的真实性核验（防上一轮的虚构 probe 复发）

上一轮 `runtime-executor-rejection-zero-sink` 是照着计划虚构出来的，本轮逐个符号对 base（`server` subtree `7d525002`）核实，全部存在且语义吻合：

| 符号 | 读回 |
|---|---|
| `NotificationEventListener.onPromotionApproved` | 存在（:211），且带 `@Async("skillhubEventExecutor")` + `@TransactionalEventListener` —— 正是 after-commit + 生产 executor 链 |
| 事件 category / event | 该方法内实发 `"PROMOTION_APPROVED"`、entity `"SKILL"`（:219） |
| `PromotionService.approvePromotion` | 存在（domain :183），带 `@Transactional`，可被代理 |
| `SseEmitterManager` | 存在（`skillhub-notification/.../sse/SseEmitterManager.java`） |
| `skillhubEventExecutor` | core=2 / **max=4** / **queue=100** / `CallerRunsPolicy` —— 与 addendum「填满 4 worker + 100 queue slot」逐字吻合 |
| `NotificationService.create` | 当前为裸 `@Transactional`（REQUIRED），即本次改动的对象 |

`onPromotionApproved` 走 `PROMOTION_APPROVED` / promotion submitter 收件面，**不经 subscriber fan-out**，满足加绑条件「非 subscriber」。

falsification 逻辑自洽：`@Async` 监听器在 executor 饱和时由 `CallerRunsPolicy` 落回 publisher/commit 线程同步执行，此时若 `create` 仍是 `REQUIRED`，则其写入挂在尚未提交的外层事务上，独立 transaction 看不到「已提交 row」，断言失败；改 `REQUIRES_NEW` 才可见。故该 probe 对本改动**具备证伪能力**，与被判不适用的 `PromotionApprovalFlowIntegrationTest`（`@MockBean NotificationDispatcher`，恒绿）形成对照。

命名说明：文件名为 `SubscriberNotificationRuntimeIntegrationTest`，但本 probe 方法以 promotion 为收件面，属非 subscriber 流程。文件名不改变流程归属，且该 path 已在批准写集内，故不因命名退回。

## 状态与后续

- 加绑条件 2 **闭合**，`pending_evidence_delta` 解除。
- Coder `fd2d1252` 的 RED 段 Run 自 20260822-145607 起**仍在执行中**，本轮**不重复 mention**，避免同分支并发双跑。
- GREEN 段证据收口待 Coder 交回 RED 报告后由 leader 续派；届时该 probe 作为加绑条件 2 的唯一受理证据。
- R1–R5、具名 invariant、candidate disposition、work partition 及 `R-P1-02` / `R-P1-07` A/B 边界均未因本 delta 改动。
