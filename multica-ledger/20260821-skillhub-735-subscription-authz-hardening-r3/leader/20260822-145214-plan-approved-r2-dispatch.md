# leader / 20260822-145214 / Plan r2 人工批准受理 + 派发

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 批准人：dongsjoa（千乘妍），按 Project description 对 Plan Gate 的独立审批授权
- 批准对象：`planner/20260822-141107-plan-gate-r2.json`（`61d54ad3…bf04238`），Plan Run `13699b09-a56a-4b08-8e07-57c355cb3686`
- Stage：`PLAN_GATE_R2_AWAITING_APPROVAL` → `PLAN_APPROVED_R2`

## 批准附带的加绑条件（逐条登记，不改写）

1. **`YANK_REVOCATION_NOTICE_REACHABILITY`**：purpose 必须落在 `VisibilityChecker` 内；`SubscriberAccessResolver` 只转发不判定；不得引入 `wasPublished` 或 snapshot。破坏任一即 RETURN。
2. **`NotificationService.create` 改 `REQUIRES_NEW`**：批准，但加绑一条非 subscriber runtime regression（见下「加绑条件 2 的机械核验」）。
3. **`AFTER_COMMIT_CALLER_RUNS_DELIVERY`**：批准；executor shutdown 排除在本次 delivery guarantee 外，保留为 PR 残留风险。
4. **`R-P1-02` / `R-P1-07` 保持 A/B 选择题**：硬约束。任何产出中出现「维护者已批准 / 已确认」字样即 RETURN。
5. **`R-P1-03` / `R-P1-05`**：批准，批量接口五条冻结语义与二参 `subscribe` 删除照 plan 执行。

## 加绑条件 2 的机械核验：**指定测试不适用**

批准正文将非 subscriber runtime regression 指定为
`server/skillhub-app/src/test/java/com/iflytek/skillhub/controller/portal/PromotionApprovalFlowIntegrationTest.java`，
理由为「base 上已存在、真实穿过 dispatcher 的非 subscriber 流程」。

在分支 head `77d52c3d` 上读回该文件，**该理由的事实前提不成立**：

```
@MockBean
private NotificationDispatcher notificationDispatcher;

@MockBean
private RbacService rbacService;
```

- 该测试确为 Spring 集成测试（`MockMvc` + 真实 JPA repository + `@ActiveProfiles("test")`），但 `NotificationDispatcher` 是 **`@MockBean`**。
- dispatcher 被 mock，则 `NotificationService.create` 在该测试中**根本不会被调用**，不产生真实 Notification row，也不触达 SSE。
- 因此它对 `REQUIRES_NEW` 这一改动**无证伪能力**：无论 `create` 的事务传播是 `REQUIRED` 还是 `REQUIRES_NEW`，该测试都恒绿。作为「证明爆炸半径未波及非 subscriber 通知」的证据，它是不可证伪的。
- 附带事实：该测试同时 `@MockBean` 了 `RbacService`，而 `R-P1-03` 正在改 RBAC seam，故它对 RBAC 改动同样无证伪能力。

### base 上是否存在合格替代（只列事实，不代为指定）

全仓扫描 `server/*/src/test` 下引用 `NotificationDispatcher` 或 `notification.service.NotificationService` 的测试，共 5 个：

| 文件 | dispatcher 是否 mock | 类型 | 是否合格 |
|---|---|---|---|
| `PromotionApprovalFlowIntegrationTest` | **是（`@MockBean`）** | Spring 集成 | ✗ 无证伪能力 |
| `NotificationEventListenerTest` | 否 | `@ExtendWith(MockitoExtension.class)` 纯单测 | ✗ 无 Spring runtime / 事务代理 / executor |
| `NotificationControllerTest` | 否 | `@ExtendWith(MockitoExtension.class)` 纯单测 | ✗ 同上，且为 controller 读路径 |
| `SubscriberNotificationSinkTest` | 否 | — | ✗ subscriber 面，被「非 subscriber」条件排除 |
| `NotificationDispatcherTest` | 否 | notification 模块单测 | ✗ 无 Spring runtime |

**结论：base 上不存在满足「非 subscriber + 真实 Spring runtime + 真实穿过 dispatcher 至真实 row/SSE」三项的既有测试。** 该 regression 很可能需要新写，从而触及写集——写集变更属 Planner 职权，不属 Coder 也不属 leader。

### 路由

按批准正文自带的退出条款「若该测试因故不适用，回 Planner 说明并另行指定，Coder 不得自行换一条更轻的」，本项**回 Planner** 出 evidence-contract 窄 delta 另行指定。leader **不代为指定替代测试**，Coder **不得自选**。

若该 delta 改动 plan artifact，则 artifact digest 变更，需重跑 Plan Gate 并按项目路由重新取得人工批准；本分录预先声明该后果，不预判其范围。

## 派发

并行派两路，二者依赖面不相交：

- **Planner `e9cf031c`**：仅出加绑条件 2 的 evidence-contract 窄 delta（指定合格的非 subscriber runtime regression）。不动 R1–R5 已批准结论，不动写集以外的面。
- **Coder `fd2d1252`**：按 plan §7.1 六步开工，**本 Run 只做 RED 段**（步骤 1–2）：先落 `RED_BASE_SHA` / `RED_DELTA_SHA`，两者对 GREEN 均须 `git merge-base --is-ancestor` exit 0 可达。上一轮即栽在不可 checkout 的 RED 日志顶数，本轮以 branch-reachable commit 为唯一受理形式。GREEN 段证据收口**待 Planner delta 落定后**再续派。

RED 段的目标断言取自已批准的 invariant 与 acceptance probe，与加绑条件 2 争议面不相交，故可与 Planner delta 并行，不构成抢跑。

两路同分支作业：push 前先 rebase，禁止 force-push。

## Publication 边界（不因本次批准放宽）

只交内部工作分支上的 exact tree 与证据。不 push 公开分支、不碰 PR #735、不转 Ready、不 force-push、不新建取代性 PR。#735 当前 `CONFLICTING` / `DIRTY` 属维护者 keep-as-draft 要求下的预期状态，不构成开工阻塞。
