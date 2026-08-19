---
ts: 20260819-212700
agent: qa
action: independent charter r3
tree: 2196fadc347cfdc844703df04877f7c388611a4f
verdict: —
---

## 形成顺序与边界

本章程在采用冻结 Plan R6、QA R2 finding 和 Coder closure 之前形成。先读取 source tree `2196fadc347cfdc844703df04877f7c388611a4f`、BASE `d2403bb5911953b8f53e62c3f0a9edc291363944` 至 subject commit `cc10ff21efa507adf095fbe68d38b5a251542c0e` 的完整非 ledger diff、生产 listener、订阅写入口、资格策略、账户/成员批量仓储、dispatcher、notification persistence 与 SSE sink，以及相关既有测试。

## 新可达路径

1. 管理发布、审核发布与 promotion 产生 `SkillPublishedEvent`，由 production `NotificationEventListener` 读取订阅候选，经 production `SubscriptionRecipientEligibility`/`SubscriptionMetadataAccessPolicy` 批量重验账户与 namespace membership，再由 production `NotificationDispatcher` 调用 `NotificationService.create` 并推送 SSE。
2. 已发布版本 yank 保存状态后产生带 `wasPublished` 的 `SkillVersionYankedEvent`，沿相同 listener、资格、dispatcher、persistence/SSE 路径到达订阅者。
3. Portal subscription PUT 进入 `SkillSubscriptionService.subscribe`，在 save/event 前使用当前 skill、namespace、account 与 membership 权威事实授权；GET/DELETE 保持原读写语义。

## 受影响角色与状态

- publisher/actor、当前 subscriber、stale/removed subscriber、inactive/missing account；
- PUBLIC、NAMESPACE_ONLY、PRIVATE、hidden skill；active/archived namespace；
- OWNER/ADMIN/MEMBER、跨 namespace member、仅平台 SUPER_ADMIN；
- 当前发布、撤权/降权、yank 后有/无 fallback、以及 `wasPublished=false`。

## 最危险反例

- subscription PUT 在 mutation 后才拒绝，或平台权限绕过 namespace/PRIVATE 资格；
- listener 只过滤候选但测试绕过 production dispatcher，导致 recipient/payload/persistence/SSE 未被真正验证；
- batch authority 加载中途失败后发生 partial preference/persistence/SSE；
- archived/PUBLIC 或 hidden/PRIVATE 的 stale subscriber 收到元数据；
- publisher/actor 自收通知，或 `wasPublished=false` 仍 fan-out；
- 新发布事件破坏 search、audit/outbox、download/install、direct detail/status 等既有消费者。

## 可证伪探针

- 在一个 production listener invocation 中注入 mixed candidates，断言仅当前合格且非 publisher/actor recipient 到达 production dispatcher 的 persistence 与 SSE；逐字段断言 category/event type、title、body、entity、recipient-visible payload。
- 对 account、namespace、membership 三个 authority batch 分别抛错；断言首个 preference/create/SSE 前传播异常、0 partial fan-out，且每类批量调用至多一次、无 N+1。
- subscription PUT 探测合法、inactive、PRIVATE nonmember/撤权/降权、cross-namespace、hidden、platform-only SUPER_ADMIN，并断言 save/event 零交互；另跑 GET/DELETE 兼容。
- publish/yank 探测 stale/removed、inactive/missing、PRIVATE current/removed、cross-namespace、hidden、archived、platform-only SUPER_ADMIN、fallback/no-fallback 与 `wasPublished` gate。
- 跑冻结 R6 定向用例、通知 sink probe、相关兼容回归及全 reactor build；记录每模块 tests/failures/errors/skips 与 skip 来源。

## 明确未覆盖

- 不连接真实浏览器 EventSource、真实数据库或外部消息基础设施；只允许在 repository/persistence 与 SSE transport 边界 mock。
- 不验证生产部署、负载/并发、跨进程投递与 CI 环境；这些不替代本轮对同步生产调用链和 reactor 回归的验证。

## 交接

- 状态：Independent Verification Charter 已先于 Plan/closure 对照形成。
- 产物：本分录；绑定 subject tree `2196fadc347cfdc844703df04877f7c388611a4f`。
- 下一步：执行 `R3 Charter ∪ Plan R6 acceptance ∪ QA R2 finding closure ∪ existing regression`。
- 未解决：动态执行结果待后续 verdict 分录。
- 需要决策：无。
