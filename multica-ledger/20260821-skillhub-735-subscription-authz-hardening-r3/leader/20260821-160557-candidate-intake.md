---
ts: 20260821-160557
agent: leader
action: 候选 exact tree 机械核对 + 冻结 + 并行 dispatch QA / Review Pass 1
tree: 922195602c92f0f420e17df9021ae39727c95f8d
verdict: ACCEPTED_FOR_VERIFICATION
---

Code/TDD 交接核对。本分录只登记机械读回，不含语义裁决；`ACCEPTED_FOR_VERIFICATION` 只表示
候选可进入 QA/Review，**不是**实现正确性结论。

## 1. 前一轮 `EVIDENCE_GAP` 闭合

20260821-1557 的交接声明候选仅在 Coder 容器内，远端与下游均不可达，已退回。本轮远端读回：

- `git ls-remote origin refs/heads/20260821-…-r3` → `1b705341dbd516a329049df773bd471e22c1f988`
- 候选业务 commit `046c04ed6b5631bae7b854ae60797b3ed7a3a930`，parent `e3312ae5`（= 上轮 leader 分录），
  tree `922195602c92f0f420e17df9021ae39727c95f8d`
- `1b705341` 相对 `046c04ed` 的非 ledger diff 为空 —— 其后确为纯 ledger commit
- 未 rebase、未 amend，Coder 报出的 SHA 与远端对象逐一对应

## 2. 写集与禁止面

| 项 | 读回 |
|---|---|
| 非 ledger diff（vs `e8cab738`） | `17 files changed, 900 insertions(+), 9 deletions(-)` |
| 与冻结 write_set 关系 | 17 条全部 ∈ 18 条 write_set；**无越界**。未触碰的 1 条为 `NotificationDispatcherTest`（Coder 声明无需改） |
| 禁止路径/符号 | `confirmPublish`、`SkillReviewSubmitService`、`SubscriptionMetadataAccessPolicy`、`SubscriptionRecipientEligibility`、migration/flyway/liquibase、`.github/workflows` 在新增行中命中数 0 |
| 8 个既有 `VisibilityChecker` caller | 6 个宿主文件 diff 全部 unchanged；8 个旧调用点行号与 base 一致，未迁移 overload |
| `VisibilityChecker` 既有签名 | diff 为纯新增；两个既有 `canAccess` 未被改写 |
| production `canAccess` 调用点 | base 8 → 候选 10，新增两处为 `SkillSubscriptionService.java:63` 与 `SubscriberAccessResolver.java:57`，与计划一致 |

## 3. 测试证据读回

| 证据 | 读回 |
|---|---|
| RED `coder/20260821-141300-red-output.log` | `Tests run: 14, Failures: 14, Errors: 0, Skipped: 0`，`BUILD FAILURE`；构成 sink 9 + i18n 1 + subscribe 4 |
| 最终定向 GREEN `…155500-candidate-green-output.log` | `Tests run: 54, Failures: 0, Errors: 0` |
| sink 复跑 `…155300-sink-green-output.log` | `Tests run: 16, Failures: 0, Errors: 0` |
| 完整 reactor `…143800-full-test-output.log` | 各模块汇总 **1,412 tests / 0 failure / 0 error / 1 skipped**（我按模块行独立求和复算，与 Coder 报数一致），`BUILD SUCCESS` |
| Semgrep `…150900-semgrep.json` | 51 results / 0 errors；`visibility-checker-call` 10（冻结 inventory 为 8），delta = 计划内两处 |

**登记的绑定缺口（非 RETURN，转 QA 判据）**：完整 reactor 日志时间戳为 `143800`，早于
`155300` / `155500` 的 sink probe 改造，因此 **1,412 tests 全绿并不绑定 exact tree
`92219560`**；该 tree 上只有定向 54 + 16 的绿证据。Coder 分录对这条时间线是透明的，未冒称。
全量绿必须由 QA 在 exact tree 上自行复跑取得，不得引用此日志结案。

## 4. 转下游的判据（含人工批准时追加的四条）

1. 三项 `UNVERIFIED`（real commit→proxy/async sink、real rollback 零 sink、real executor
   rejection 零 sink）与 generated/reflection/external 完整性：须由 QA 出实测 probe 或显式
   结案为残留盲区并进 PR 正文，不得以「测试没覆盖到就是没有」收口。
2. R2 不误伤：须有对可读订阅者集合过滤前后逐元素一致的正向证据。
3. RED 先红后绿已在本轮取得原始输出，QA 仍须在 exact tree 上独立复跑。
4. `SubscriberAccessResolver.java:43-64` 的「零独立判定分支」由 Reviewer 逐行证伪，不采信
   Coder 自述口径。另记一处供 Reviewer 判断（Leader 不裁决）：新 overload 自行处理
   SUPER_ADMIN 后，向既有四参 `canAccess` 传入空 `platformRoles`。

## 5. dispatch

exact tree 冻结于 `922195602c92f0f420e17df9021ae39727c95f8d`（业务面基准 = `046c04ed`）。
其后仅允许 ledger commit；任何非 ledger 变化使本次 QA/Review 结论失效并重走。

按 HIGH_RISK 强制拆分，并行派出（唯一允许的并行点）：

- QA（`4f83bacc`）：blind Charter Run 与 execution Run 分离，Charter Run 不得读 plan artifact
  与 coder 分录。
- Review（`a8512fa6`）：Pass 1 `BLIND_BEHAVIOR_AUDIT`，fresh context、只读 exact tree、
  不获 Plan/QA 产物；Pass 2 `EVIDENCE_CHALLENGE` 待 QA 产物齐备后另派。

Publication 边界不变。
