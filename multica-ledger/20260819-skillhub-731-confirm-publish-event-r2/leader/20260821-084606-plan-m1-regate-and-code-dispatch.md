---
ts: 20260821-084606
agent: leader
action: plan gate re-run on M1 artifact / Code-TDD dispatch
tree: 27649c954e41aed9c0aa0ac1e9bc454b78a38909
verdict: PASS
---

对 `planner/20260821-083820-plan-gate-m1.json` 的机械复跑。规划 已自跑一次，本分录**独立复跑**，
不采信其自述。本 Gate 只确认「M1 已按审批裁定落地且**除此之外什么都没动**」，不重开语义审批——
审批人已裁定不再走第二轮 Plan。

## 1. 完整性与 append-only

| # | 检查 | 实测 | 结论 |
|---|---|---|---|
| 1 | 声明 commit == 远端 head | `2d9336f0e0f02a0226909443c942a4fb0455eac8` | PASS |
| 2 | 非 ledger 业务 diff（vs `9f0977c4`） | 空 | PASS |
| 3 | 变更全部落在 `multica-ledger/**` | 2 `A` + `index.md` `M` | PASS |
| 4 | `index.md` 纯表尾追加 | +1 / −0 | PASS |
| 5 | **旧 artifact 未被就地改写** | `9408cbde…b67d` / `46d1441e…5127a` digest 不变 | PASS |
| 6 | plan-r1-m1.md digest 与声明一致 | `07cc6971…5c15b` | PASS |
| 7 | plan-gate-m1.json digest 与声明一致 | `d5948e67…0c7e` | PASS |
| 8 | inventory 未被修改 | 仍 `4682fdd4…b5ec` | PASS |
| 9 | DCO sign-off | 存在，作者一致 | PASS |

第 5 项值得单记：我在上一分录写的是「改 `plan-r1.md:42`」，规划 实际做的是**新增 superseding
产物**而非就地编辑。后者才符合 ledger append-only，规划 的处理比我的措辞更正确，予以采纳。

## 2. 最小性证明（本 Gate 的核心，逐块 digest 比对而非目测）

对新旧两份 artifact 按 key 逐块归一化（`sort_keys` + `ensure_ascii=False`）后取 SHA-256：

| 块 | 旧 | 新 | 结论 |
|---|---|---|---|
| `acceptance_probes` | `62ea23d6b7a3` | `49b1f46d970f` | **DIFF**（预期） |
| `artifact` | `db541a4a2280` | `b6edefe528d2` | **DIFF**（自指元数据） |
| `changed_behavior` | `bdaa3a954ea0` | 同 | SAME |
| `council_nomination` | `7fcef3625649` | 同 | SAME |
| `coverage_closure` | `4440cd0a360b` | 同 | SAME |
| `invariants` | `8454395e9831` | 同 | SAME |
| `source_contract` | `029880d461e1` | 同 | SAME |
| `subject` | `d9ded149472d` | 同 | SAME |
| `work_partition` | `1ffac2192b73` | 同 | SAME |
| `write_set` | `efaf7ef0a82c` | 同 | SAME |

全文归一化 diff 恰好 4 行、3 组：EN 断言、zh 断言、`artifact` 自指四字段
（path / revision 1→2 / run_id / sha256）。**没有第四处改动。**

由此，我在 `leader/20260821-081557-plan-r1-gate.md` 对 closure / Council / work_partition /
write_set / invariants 的既有校验结论**按位继承**，无需重新推导。另额外独立重算一次闭包作为
交叉验证：inventory 8 candidate、declared 集合与 inventory 相等、`changed_behavior` 映射并集
等于 inventory 且恰好一次、`excluded=0` / `unresolved=0` / 6 条 path — 与上一轮一致。
Council 仍 3 名、含 1 名 `dissent_role`。

## 3. M1 落实核验

| 要求 | 实测 | 结论 |
|---|---|---|
| EN 文案 = `You do not have access to this skill` | `f1-bundle-en-denial.sink_assertions[0]` 逐字 | PASS |
| zh 文案 = `你没有权限访问该技能` | `f1-bundle-zh-denial.sink_assertions[0]` 逐字 | PASS |
| 两条探针都改（命名分歧不漏改） | 两条均改 | PASS |
| `isNotEqualTo(key)` 杀手断言保留 | 两条均保留 `resolved message is not error.skill.subscription.noPermission` | PASS |
| 不退回 manage / owner-admin 措辞 | 新产物中旧文案出现次数 = **0** | PASS |
| `expected_before: FAIL` / `after: PASS` 未被削弱 | 两条均保持 | PASS |
| 计划散文同步 | `plan-r1-m1.md:42` 改为 read-access 语义，并写入 `''` 撇号约束 | PASS |
| WARN 模板等其余冻结项 | 散文 diff 仅 M1 行 + 前言/交接记账，`:43` 未动 | PASS |
| `f1-authorized-control` 控制组 | 未改，仍 `PASS→PASS` | PASS |

批准文案本身不含撇号，`''` 风险仅在 Coder 同语义微调时出现，约束已随 dispatch 下传。

## 4. validator 复跑

`run_gate.sh --kind plan` 独立执行（`/workspaces/HD-28/gates/`），receipt：

- `result`: **PASS**，`errors`: 空
- `runs`: PLAN Run `3b3dfa72-bc85-4fde-9cf4-a481c3d33f4d` / context `01a02347-46f7-7962-81ec-4118401867e2`
- provenance 隔离：与 discovery Run `b4535e97-…` / context `01a0233a-…` 均不同 ⇒ 非自证
- `inputs`: artifact `d5948e67…0c7e`、inventory `4682fdd4…b5ec`
- `validator`: `validate_gate.py` v0.1，sha256 `a7100d86…4892`（与上轮同一版本）
- `warnings`: 1 条，D1「inventory is NOT an upper bound」

context_id 与上一 Plan Run 相同、run_id 不同：schema 只约束与 discovery 隔离，同一 Planner
会话延续不构成自证，判 PASS。

## 5. receipt 取代关系

新 receipt **superseding** `leader/20260821-081557-plan-r1-gate.md` 的 `inputs.artifact`
`9408cbde…b67d`。旧分录不改写，仍作为历史事实保留；自本分录起，下游一律绑定
`d5948e67…0c7e`。`plan-r1.md:42` 的旧文案自此**不再是任何生效契约的一部分**。

`index.md` 头部「冻结计划」同步更新为 `planner/20260821-083820-plan-r1-m1.md`。

## 6. 派发 Code/TDD（约束原文下传，Coder 不得自行放宽）

工作分支 `20260819-skillhub-731-confirm-publish-event-r2` @ `2d9336f0`；生效契约
`planner/20260821-083820-plan-gate-m1.json` = `d5948e67…0c7e`。

1. **M1 为硬约束**，未落实即 `PLAN_GAP`，不得进入 QA。
2. Coder 分录必须显式记录 M1 落实位置：`messages.properties` 与 `messages_zh.properties`
   的**实际行号**，以及**探针断言 diff**。
3. 断言 diff 须同时覆盖 `f1-bundle-en-denial` 与 `f1-bundle-zh-denial` **两条**
   （计划散文侧对应单个 `P-F1-BUNDLE-EN-ZH`，勿只改一条）。
4. F1：只注册 `error.skill.subscription.noPermission` 一个 key，落在既有 `error.skill.*`
   分区内，**不重排、不改写既有 186 行**；不动 `ApiResponseFactory` 与异常体系。
5. 文案不得退回 manage / owner-admin 语义；同语义微调可以，撇号一律 `''`
   （既有约定 EN `:91` `:93` `:130`，当前文件无未转义 `'`，不得由本次改动引入首例）。
6. F2：`NotificationEventListener` 第 81、105 行两处 null-namespace 分支各加一条
   `log.warn`，复用第 28 行既有 logger，模板见 `plan-r1-m1.md:43`；
   **不改过滤语义**，null namespace 仍拒；**不得输出 subscriber ID 或任何 PII**。
7. 收件人集合逐元素一致由差分探针 `published-nonnull-elementwise` /
   `yanked-nonnull-elementwise` 证明（`23658e0f` 与候选 exact commit 两个 clean worktree
   跑同一 fixture，序列化 ordered tuple 后 `cmp`），不以计数或描述代替。
8. RED 必须先于 GREEN 且留下真实失败输出；`expected_before: FAIL` 的探针在旧 tree 上确实
   FAIL 才算数。
9. 严守 `write_set`：`skillhub-search` 三条控制组探针的文件**不在** write_set，不得改动。
10. F3 只出 PR 正文草稿，不实现任何 migration / prune / `skill_subscription` 清理。
11. 不 push 公开分支、不建/改上游 PR、不签 CLA、不改写作者、不 force-push。
12. 不得以本地绿冒充 CI 绿——审批人已登记为最终 Review Gate 的复核项。

## 交接

- 状态：Plan Stage 关闭。M1 再冻结 PASS，新 digest 已绑定；进入 Code/TDD
- 产物：本分录；receipt 已复算，validator hash `a7100d86…4892`
- 工作分支：`20260819-skillhub-731-confirm-publish-event-r2` @ `2d9336f0`
- 下一步：实现 进 Code/TDD，产出 RED → GREEN → regression 分录，交本席 Implementation Gate
- 未解决：Publication 段（验收 5/6）由主控侧承接；「不得以本地绿冒充 CI 绿」待最终 Review Gate 复核
- 需要决策：无。Plan 段人类审批已闭合，Code/QA/Review 按 Project 路由自驱，不 @ Member
