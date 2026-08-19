---
ts: 20260819-210445
agent: qa
action: independent verification charter r2
tree: 71d9c55387b0a0b8336905d3aa9986fff6be6c97
verdict: —
---

## 形成顺序与 subject

本章程在读取冻结 R6 Plan、实现账本和 Coder 测试结论之前形成。输入仅为 Source Contract、BASE `d2403bb5911953b8f53e62c3f0a9edc291363944` 到业务 commit `f3decf8147fe1ef8809cac21afce3a5b77d607bb` 的完整业务 diff、subject tree `71d9c55387b0a0b8336905d3aa9986fff6be6c97`、surrounding production code 与既有测试。

## 新可达路径

1. review approval / governance publish → `SkillPublishedEvent` → `NotificationEventListener.onSkillPublishedForSubscribers` → subscription candidates → authoritative namespace/account/membership batch reads → metadata eligibility → publisher exclusion → `NotificationDispatcher` → preference → `NotificationService.create` → recipient SSE payload。
2. governance yank → `SkillVersionYankedEvent(wasPublished)` → subscriber listener → pre-yank publication eligibility → actor exclusion → dispatcher/persistence/SSE。
3. subscription PUT → skill/account/namespace/membership current facts → metadata policy → subscription save + counter + event；GET/DELETE 仍沿旧路径。

## 受影响角色与状态

- PUBLIC、NAMESPACE_ONLY、PRIVATE；hidden；有/无 latest version；ACTIVE/ARCHIVED skill 与 namespace。
- owner、namespace OWNER/ADMIN/MEMBER、nonmember、removed/stale member、cross-namespace member、仅 platform SUPER_ADMIN。
- active、disabled、missing account；publisher/actor 与普通 subscriber；mixed candidate batch。

## 最危险反例与可证伪探针

- PRIVATE member/nonmember 或 hidden 普通 member 被错误允许订阅/收通知；探针断言 mutation、counter、event、dispatcher 全为 0。
- 撤权、inactive、missing、cross-namespace/stale recipient 混入 fan-out；探针断言只有合格 recipient 且 publisher/actor 排除。
- namespace/account/membership 权威读取异常发生在部分 dispatch 后；分别注入三类异常并断言 dispatcher 为 0；再沿 dispatcher 断言 persistence/SSE 为 0。
- yank 读取 post-yank 状态导致错误丢失合法通知，或未发布版本错误通知；分别验证 `wasPublished=true/false` 与 fallback/no-fallback。
- helper 测试替代真实 sink；沿 listener 捕获 dispatcher exact 参数，并沿真实 dispatcher 捕获 `NotificationService.create` 与 SSE recipient/payload。
- 批量资格实现退化为 N+1 或吞错；核验 repository invocation count 和异常传播。
- PUT 策略误迁移至 GET/DELETE，或 platform SUPER_ADMIN 获得 bypass；验证旧入口依赖隔离和无 bypass。
- archived PUBLIC direct detail/status、discovery、version/content/resolve、download/install 合同意外收紧；运行对应既有回归。

## 初始未覆盖

- 不修改候选测试，因此只使用现存可达探针；若缺少真实 listener→dispatcher→持久化/SSE 的单一 Spring 集成测试，将明确记为测试边界，不以 mock 链替代该事实。
- 不连接生产数据库、真实浏览器 SSE 客户端或外部基础设施；覆盖以 reactor 测试环境和 mock 边界为准。
- 不做并发/负载/长时间 soak；静态核验批量调用，动态核验调用次数。

## 交接

- 状态：独立章程已冻结，尚未形成 verdict。
- 产物：本文件，绑定 subject tree `71d9c55387b0a0b8336905d3aa9986fff6be6c97`。
- 下一步：读取冻结 Plan 与既有回归，执行三者并集。
- 未解决：动态证据待执行。
- 需要决策：无。
