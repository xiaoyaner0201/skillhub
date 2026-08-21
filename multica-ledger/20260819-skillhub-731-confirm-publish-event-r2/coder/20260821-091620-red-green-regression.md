---
ts: 20260821-091620
agent: coder
action: RED→GREEN / regression
tree: e8aca2a43467dad0da969c182632a7b791f90621
verdict: —
---

生效契约仅认 `planner/20260821-083820-plan-gate-m1.json`，SHA-256
`d5948e67b98f809fd53adda4bc85647ad2b23e71e2b9bc69b900f102457e0c7e`。候选业务
commit `bfcb4fe5b9314aa41f3ffe71045d8fb043c6482c`，subject tree
`e8aca2a43467dad0da969c182632a7b791f90621`。

## RED → GREEN

修复前的四条入口/side-effect sink 探针均真实 RED：

- `f1-bundle-en-denial`：1 failure，实际解析值为原始 key
  `error.skill.subscription.noPermission`，不等于期望 EN 文案；同次运行的 4 个 unauthorized
  persistence/count/event 控制用例通过。
- `f1-bundle-zh-denial`：1 failure，实际解析值为原始 key，不等于期望 zh 文案。
- `published-null-namespace-warn`：1 failure，期望 1 条 WARN、实得 0。
- `yanked-null-namespace-warn`：1 failure，期望 1 条 WARN、实得 0。

最小实现后，bundle EN/zh 与 published/yanked null-namespace 四条定向探针共同运行通过；
相关 app 定向套件（`SubscriptionMessageBundleTest`、`SkillSubscriptionServiceTest`、
`NotificationEventListenerTest`、`SubscriberNotificationSinkTest`、
`NotificationDispatcherTest`）通过。WARN 仅含 skillId/namespaceId，不含 subscriber ID，且
null namespace 仍由原 eligibility fail-closed，未改变控制流。

## M1 明示落实

- `server/skillhub-app/src/main/resources/messages.properties:114`：
  `error.skill.subscription.noPermission=You do not have access to this skill`
- `server/skillhub-app/src/main/resources/messages_zh.properties:114`：
  `error.skill.subscription.noPermission=你没有权限访问该技能`
- `f1-bundle-en-denial` 断言 diff：新增
  `.isEqualTo("You do not have access to this skill")`，并保留
  `.isNotEqualTo(CODE)`。
- `f1-bundle-zh-denial` 断言 diff：新增
  `.isEqualTo("你没有权限访问该技能")`，并保留 `.isNotEqualTo(CODE)`。

两个 bundle 均从 186 行变为 187 行，仅在既有 `error.skill.*` 分区新增上述唯一 key；旧
186 行逐行不变，未引入未转义单引号，未回退 manage / owner-admin 文案。

## 元素级兼容探针

在两个独立 clean worktree `23658e0f94f5d6ac1290978671a75eca5d56ef94` 与
`bfcb4fe5b9314aa41f3ffe71045d8fb043c6482c` 中复制并运行同一临时 fixture，按
`(recipient,eventType,title,payload,entityType,entityId)` 序列化 ordered invocation：

```json
[{"probe":"published-nonnull-elementwise","tuples":[["current-admin","SUBSCRIPTION_NEW_VERSION","Skill updated: Test Skill","{\"skillId\":1,\"skillName\":\"Test Skill\",\"slug\":\"test-skill\",\"namespace\":\"demo\"}","SKILL",1]]},{"probe":"yanked-nonnull-elementwise","tuples":[["current","SUBSCRIPTION_VERSION_YANKED","Skill version yanked: Test Skill","{\"skillId\":1,\"skillName\":\"Test Skill\",\"slug\":\"test-skill\",\"namespace\":\"demo\"}","SKILL",1]]}]
```

两份 JSON SHA-256 均为
`661ad8d6a43490dda2694121a3f2d73a32ca14cb90a5637de9d714ed1d4883e0`；实际
`cmp` exit 0。临时 fixture 随后从两个 worktree 移除，两边恢复 clean，未进入候选树。

## 回归与边界

- search 控制组三类完整测试：29 tests，0 failures/errors/skips，BUILD SUCCESS；
  `skillhub-search` 对取件 HEAD 无 diff。
- 冻结 reactor 命令：storage 21、domain 415、auth 120、notification 36、infra 10、
  search 35、app 767；合计 1404 tests，0 failures，0 errors，1 skipped，BUILD SUCCESS。
  唯一 skip 为既有 `RedisClusterIntegrationTest` 环境 gate。
- `git diff --check` exit 0；非 ledger 变更仅为冻结 write_set 中五个路径。
- F3 PR 正文草稿未改；没有 migration/prune/`skill_subscription` 清理，没有修改
  `ApiResponseFactory` 或异常体系。
- PR #735 仍为 Draft；`pr-tests` 未运行。本分录只陈述本地结果，不声称 CI 绿。

## 交接

- 状态：Coder RED→GREEN、元素级兼容探针与相关回归完成；不自签 QA/Review PASS。
- 产物：业务 commit `bfcb4fe5b9314aa41f3ffe71045d8fb043c6482c`；subject tree
  `e8aca2a43467dad0da969c182632a7b791f90621`。
- 下一步：Leader 对本 subject tree 执行 Implementation Gate，随后进入 fresh QA。
- 未解决：无。
- 需要决策：无。
