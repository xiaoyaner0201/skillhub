# leader / 20260822-152000 / 加绑条件 2：人工撤销原指派 + 批准替代 probe（superseding 分录）

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 裁定人：dongsjoa（千乘妍），按 Project description 对 Plan Gate 的独立审批授权
- 触发评论：`73219d57-f34b-4af5-9be3-a5fba0858757`（2026-08-22T15:11:21Z）
- 性质：**superseding**，不改写 `leader/20260822-145214-plan-approved-r2-dispatch.md` 与批准正文原文

## 一、原加绑条件 2 指派：**REVOKED**

批准正文（20260822-1449）指定
`server/skillhub-app/src/test/java/com/iflytek/skillhub/controller/portal/PromotionApprovalFlowIntegrationTest.java`
为非 subscriber runtime regression。裁定人已在有凭据的会话读回 upstream base
`e8cab7389f5dc1084fdc1cdf70bafd314d0a3d50` 原文并自认事实前提不成立：

- L53 `@SpringBootTest(classes = SkillhubApplication.class)`
- L85–86 `@MockBean private RbacService rbacService;`
- L91–92 `@MockBean private NotificationDispatcher notificationDispatcher;`

与 leader 20260822-145214 的读回一致。该指派自本分录起**作废**；该测试保留为普通 promotion
workflow regression，**不得**充当加绑条件 2 的证据。

leader 的两项处置被明确确认：不代为指定替代测试、不许 Coder 自选更轻替代。

## 二、替代 probe：**APPROVED**

批准对象：`SubscriberNotificationRuntimeIntegrationTest#promotionApproved_saturatedConfiguredExecutor_persistsCommittedRowBeforeRealManagerSse`
（`planner/20260822-150358-plan-r2-evidence-addendum.md`，`3812652e…d86fe29f`）。

裁定人独立读回的绑定项与 leader 20260822-150907 的读回逐项一致，且补充了 leader 未点出的行号证据。
本分录对该补充证据**独立复核后确认**（读 `FETCH_HEAD = b15bd74f` 上的 `plan-r2.md`）：

| 裁定人所述 | leader 实读 | 结论 |
|---|---|---|
| `plan-r2.md` L407 `SubscriberNotificationRuntimeIntegrationTest.java` | L407 逐字一致 | ✅ |
| `plan-r2.md` L408 `RecordingSseEmitterTestConfiguration.java` | L408 逐字一致 | ✅ |
| L426 定向命令已列入前者 | L426 `-Dtest=…,SubscriberNotificationRuntimeIntegrationTest,…` | ✅ |

两个 path 在已批准写集内属实，写集增量 0 属实，Plan Gate artifact digest `61d54ad3…bf04238` 不变属实，
**不触发重跑与重批准**。leader 20260822-150907 的「不重跑 Gate」结论获人工确认，无需回退。

## 三、本轮新增的机械核验（不属语义裁决，供 Coder 落 probe 时对齐断言槽位）

裁定人正文写「收件面是 `PROMOTION_APPROVED` / `PROMOTION` + promotion submitter」。读回
`NotificationEventListener.java:218–219`（base `7d525002`）确认其准确，并把**七个实参槽位逐一固定**，
避免 Coder 断错字段导致 probe 因错误原因变红：

```java
dispatcher.dispatch(event.submitterId(), NotificationCategory.PROMOTION,
        "PROMOTION_APPROVED", title, json, "SKILL", event.skillId());
```

| 槽位 | 实值 |
|---|---|
| recipientId | `event.submitterId()`（promotion 提交者，非 subscriber） |
| category | `NotificationCategory.PROMOTION` |
| eventType | `"PROMOTION_APPROVED"` |
| entityType | `"SKILL"` |
| entityId | `event.skillId()` |

`entityType` 是 `"SKILL"` 而非 `"PROMOTION"`——与 category 是不同槽位，二者不冲突，但断言时不可互换。

### 链路末段实读（确认 probe 的最终 sink 真实存在）

`NotificationDispatcher.dispatch`（notification 模块 :30–58）：

1. `:34` `preferenceService.isEnabled(recipientId, category, IN_APP)` —— **前置闸门**，false 即 `return`，
   既不 `create` 也不 SSE；
2. `:40` `notificationService.create(...)` —— 本次 `REQUIRES_NEW` 改动的对象（当前 `NotificationService:26` 为裸 `@Transactional`）；
3. `:45` `sseEmitterManager.push(...)`，包在 `try/catch`，失败仅 `log.warn`。

**闸门风险已核并判为低**：`NotificationPreferenceService.isEnabled:25–28` 在无 preference row 时
`.orElse(true)`，默认开启。故只要 fixture 不显式写入一条 disabled 的 `PROMOTION`/`IN_APP` preference，
闸门不会把 probe 变成恒绿。**这是给 Coder 的硬性 fixture 约束**：不得 seed disabled preference row，
否则该 probe 会以与被撤销的 `PromotionApprovalFlowIntegrationTest` 完全相同的方式丧失证伪能力。

另记：SSE push 位于 `create` 之后且异常被吞，故 probe 断言「committed row 先于 real manager SSE」
与生产顺序一致，不是为测试特设的顺序。

## 四、维持不变（登记，不复述）

其余四条加绑条件与 R1–R5 已批准结论全部不变；`YANK_REVOCATION_NOTICE` purpose 必须落在
`VisibilityChecker` 内；`R-P1-02` / `R-P1-07` 保持 A/B，出现「维护者已批准 / 已确认」即 RETURN；
`RED_BASE_SHA` / `RED_DELTA_SHA` 必须 branch-reachable 且 `git merge-base --is-ancestor` 两次 exit 0；
Coder 不得自行扩写集；Publication 边界不变。

## 五、GREEN 段派发：已获放行，但**本轮不发**

裁定人放行「可以续派 GREEN 段的证据收口」。leader 判定**暂不发**，理由为机械事实而非保守：

- Coder `fd2d1252` 的 RED 段 Run 自 20260822-145610 起仍在 `running`，尚未交回 RED 报告；
  远端分支 head 仍为 leader 的 `b15bd74f`，Coder 未 push。
- RED→GREEN 之间存在一道**不可跳过的 leader intake**：`RED_BASE_SHA` / `RED_DELTA_SHA` 的
  branch-reachable 与两次 `merge-base --is-ancestor` exit 0 必须由 leader 实跑核验。若现在 mention
  Coder，该 Run 会在 RED Run 结束后立即接续启动，等于**跳过这道 intake**——正是上一轮「用不可
  checkout 的 RED 日志顶数」的复发路径。
- 故 GREEN 段派发条件登记为：Coder 交回 RED 报告 → leader 复核两个 SHA 可达 → 同一分录内派 GREEN。
  该 probe 届时为加绑条件 2 的唯一受理证据。
