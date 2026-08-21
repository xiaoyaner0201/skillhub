---
ts: 20260821-102507
agent: planner
action: HD-28 plan r1 M3b scope ruling and R8 write-set activation
tree: e8aca2a43467dad0da969c182632a7b791f90621
verdict: FROZEN
---

# HD-28 plan-r1 M3b：审批裁定落地

本分录 supersede `planner/20260821-101027-plan-r1-m3.md` 中等待审批的 scope/write-set 部分，不改写 M3。审批人已裁定：范围取 (b)，HD-28 只闭合 R5 / R2 / R8；R1、R3、R4 移出本链，后续由单独责任面承接。本次是裁定落地，不重开已经闭合的 F1/F2 方案选择。

## 生效变化

1. R8 方案 A 生效。`server/skillhub-app/src/test/java/com/iflytek/skillhub/domain/social/SkillSubscriptionServiceTest.java` 加入 `write_set` 与单一 work unit；只断言 `DomainForbiddenException.messageCode()`，明确禁止用当前恰好相等的 `hasMessage()` 代替显式契约。生产 `SkillSubscriptionService.java` 仍为禁写路径。
2. F3 草稿改为 `planner/20260821-102507-pr-body-draft-m3b.md`。它只向 maintainer 陈述“retained rows 同时被现有 read paths 消费”，不包含 R1 的入口、PRIVATE 字段、复现或披露技术细节；R1 的披露通道由审批人与老板另行决定。
3. R1/R3/R4 从 HD-28 的生效 Source Contract、invariant 与 falsification 阻塞项移除，不为其预留写集；search 仅保留 inventory 要求的 `published-search-control` 查询可见性控制 probe，且该 probe 不是对 R4 的关闭声明。

## R8 可执行契约

在 inactive-account、removed-member、parameterized unauthorized metadata 三类已有 denial case 上捕获 `DomainForbiddenException`：

```java
DomainForbiddenException exception = catchThrowableOfType(
        () -> service.subscribe(1L, "user-1"),
        DomainForbiddenException.class);

assertThat(exception.messageCode())
        .isEqualTo("error.skill.subscription.noPermission");
```

保留各用例原有的 repository/count/event 零 mutation 断言。若采用 fluent assertion，也必须直接读取 `messageCode()`；不得使用 `hasMessage()`、`getMessage()` 或仅断言异常类型。生产 literal 单方漂移由此 test RED，bundle key/value 单方漂移由 `SubscriptionMessageBundleTest` RED。

## 生效写集

- `server/skillhub-app/src/main/resources/messages.properties`
- `server/skillhub-app/src/main/resources/messages_zh.properties`
- `server/skillhub-app/src/main/java/com/iflytek/skillhub/listener/NotificationEventListener.java`
- `server/skillhub-app/src/test/java/com/iflytek/skillhub/listener/NotificationEventListenerTest.java`
- `server/skillhub-app/src/test/java/com/iflytek/skillhub/exception/SubscriptionMessageBundleTest.java`
- `server/skillhub-app/src/test/java/com/iflytek/skillhub/domain/social/SkillSubscriptionServiceTest.java`
- Coder 自有 ledger 目录与 `index.md` 表尾

单一 `hd28-targeted-remediation` work unit 不变：R5 与 R8 合并成一次业务 tree 变更，之后重跑 Implementation Gate、QA 与 fresh Review。禁止集继续含 policy、event、search、DB/migration、workflow 与生产 `SkillSubscriptionService.java`。

## 交接

- 状态：审批范围与 R8 write-set 已全部落地，无待决项留在 Planner
- subject：commit `bfcb4fe5b9314aa41f3ffe71045d8fb043c6482c` / tree `e8aca2a43467dad0da969c182632a7b791f90621`；本次仍为 ledger-only
- 产物：本分录、`planner/20260821-102507-plan-gate-m3b.json`、去敏后的 F3 草稿
- 下一步：Leader 对 M3b 复跑 Plan Gate，PASS 后一次性派 Code；R1/R3/R4 不得带回本链
- 未解决：D1 盲区与 Publication/remote CI；均由后续既定责任面闭合
- 需要决策：无
