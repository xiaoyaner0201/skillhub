---
ts: 20260819-223121
agent: leader
action: publication preparation
tree: 24ec04f814f666511698b5b1b3e908305b3f526c
verdict: PASS
---

Publication Gate 的本地准备已完成，结论是 **publication-ready，尚未 delivered**。没有推送新 publication branch，也没有创建上游 PR。

## Live source 与远端读回

- 2026-08-19 22:24–22:31 UTC 读回 `iflytek/skillhub#731`：页面标题仍为 `[Bug] confirmPublish does not emit SkillPublishedEvent, so subscribers get no update notification`，正文合同仍要求 `confirmPublish` 在 version/skill 保存后发出 `SkillPublishedEvent`，并要求确认 PRIVATE subscriber 的当前 read permission；页面状态查询与 `state:open` 标签链接证明 Issue 仍 OPEN，Development 显示 `No branches or pull requests`。
- `upstream/main`=`d2403bb5911953b8f53e62c3f0a9edc291363944`；fork `main` 同 SHA；内部远端分支=`bafc583bf97bd1b88afb531f8546d0e03e4fbf55`。upstream 未偏离冻结 BASE，无 rebase/replan。
- 本 runtime 没有 GitHub CLI 凭据；只读 live 页面、`git ls-remote` 和 Issue Development 读回足以确认 source/base 与当前无关联 PR。外部写动作仍需有身份的操作者执行。

## Clean candidate 与机械等价

- 本地分支：`20260819-skillhub-731-confirm-publish-event-publication-r1`，从 live `upstream/main` 建立，未推送。
- candidate commit=`b56837ca7142713c1313689f58dd4f614db714a7`；tree=`24ec04f814f666511698b5b1b3e908305b3f526c`。
- 仅重放冻结 BASE→approved commit `cc10ff21efa507adf095fbe68d38b5a251542c0e` 的 8 个非 ledger commits；共 16 个业务/测试路径，943 insertions、8 deletions；candidate 中 tracked `multica-ledger/**` 数量为 0，工作树 clean。
- approved non-ledger diff 与 publication diff 的 stable patch-id 均为 `348f6563391b59a6b1d1cb611b9a6c51b53c55bf`；path-set SHA-256 均为 `c55fefdada396d39fabe2ffc85b56a96ae48ea8e398a1cd78c7e3c73027d1a2f`；binary diff SHA-256 均为 `7f920b9af1fd68d004615fddabc5dfa19666f95e257b08295046c9f28fe3e04e`。重放只有预期 ledger modify/delete 冲突，删除 ledger 后业务 patch 无冲突且 checksum 完全相同；未产生行为变化。

## Fresh candidate 验证

Temurin JDK 21.0.12，candidate exact tree：

- sink probe：8 tests / 0 failures / 0 errors / 0 skipped；BUILD SUCCESS。
- 冻结 R6 定向并集：domain 76、notification 4、app 49；全部 0 failures / 0 errors / 0 skipped。
- 全 reactor：storage 21、domain 415、auth 120、notification 36、infra 10、search 35、app 763；全部 0 failures / 0 errors；唯一 skip=app 1，为既有 `RedisClusterIntegrationTest` 环境 gate（未配置 `REDIS_CLUSTER_TEST_NODES`）。
- `git diff --check upstream/main..HEAD` exit 0；工作树 clean。

## 待提交 PR 材料

建议 title：`fix: emit confirm-publish events with permission-aware subscriber fanout`

建议正文：

```markdown
## Summary

- emit `SkillPublishedEvent` after `confirmPublish` persists the published version and latest skill version
- apply purpose-aware, current metadata-read eligibility before publish/yank subscriber fanout
- fail closed on stale membership, inactive accounts/namespaces, cross-namespace state, or authoritative lookup failures

Fixes #731.

## Why

The confirm-publish path currently updates persistence without reaching the existing subscriber listener. For PRIVATE skills, fanout also needs to re-check current read eligibility so removed or stale subscribers do not receive recipient-visible metadata.

## Verification

- exact listener → eligibility → dispatcher → Notification persistence/SSE sink probe: 8 passed
- authorization and compatibility suite: domain 76, notification 4, app 49; 0 failures/errors/skips
- full reactor: 1,400 tests, 0 failures/errors; 1 existing Redis-cluster environment skip
- `git diff --check`

## Limits

Not exercised against production infrastructure, a real browser EventSource client, or a configured Redis cluster. The synchronous production listener/eligibility/dispatcher path and final persistence/SSE arguments are covered.
```

提交计划：head=`xiaoyaner0201:20260819-skillhub-731-confirm-publish-event-publication-r1`，base=`iflytek/skillhub:main`，建议先 Draft（高风险 auth/notification 变更，Issue 标记 `risk/high`、`triage/core`）。推送/建 PR 后必须读回 URL、head SHA、base、Draft/OPEN state、checks 初态，并确认 #731 Development/PR link。

## 决策边界

请由具备 fork/upstream GitHub 身份的 Member 决定操作方式：由其直接 push+建 Draft PR，或授权本 Agent 使用指定 GitHub 身份执行。批准后只执行 candidate push、Draft PR create 与远端 readback；退回/暂缓则不发生 push、PR、merge、close、release 或其他外部动作。

- 当前状态：Publication preparation PASS；外部动作待人类决策
- subject：`cc10ff21efa507adf095fbe68d38b5a251542c0e` / `2196fadc347cfdc844703df04877f7c388611a4f`
- candidate：`b56837ca7142713c1313689f58dd4f614db714a7` / `24ec04f814f666511698b5b1b3e908305b3f526c`
- 当前内部 HEAD：`bafc583bf97bd1b88afb531f8546d0e03e4fbf55` / `3c29482bad4c54b8122ae2dd023ee9ebe0da327f`
- 下一步：人类选择 upstream PR 操作身份与方式
