# leader / 20260823-171500 / GREEN 形式受理，**exact subject tree 冻结为 `3c809b93`**；RED intake 扩及第二条 RED delta `5aa53cfe`；`ee13d349` 自陈更正受理；`PLAN_GAP` 路由 Planner（限定改写范围）；并行派 QA 与 Review Pass 1；lane 决定上抬 dongsjoa

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`280747b1-d7ab-416b-8b49-c0525970b170`（2026-08-23T16:46:45Z，coder GREEN 交接）
- Stage：`RED_ACCEPTED_R2` → **`GREEN_ACCEPTED_R2`**；Issue 保持 `in_progress`
- 本分录只判 **evidence FORM**。「这些断言问的是不是对的问题」「行为是否应当如此」属 QA/Reviewer，leader 不代判、不预置结论

## 一、坐标与 ancestry：leader 实跑，全部相符

| 项 | 实测 | |
|---|---|---|
| GREEN commit / tree | `3becef33` / **`3c809b93ff6364f345ccf04e51054be5d91fecb4`** | ✅ 与转述一致 |
| ledger 提交 | `3e205dc6` parents=`3becef33`，diff 12 文件**全在 `multica-ledger/`**，业务面 0 | ✅ |
| 无 force-push | `012e4cf8` → `3becef33` `--is-ancestor` **exit 0**，历史逐段线性、无 merge | ✅ fast-forward |
| ancestry `e8cab738` → GREEN | exit 0 | ✅ |
| ancestry `3da0af1c` → GREEN | exit 0 | ✅ |
| ancestry `45b723ce`（已受理 RED）→ GREEN | exit 0 | ✅ |
| ancestry `5aa53cfe`（第二条 RED）→ GREEN | exit 0 | ✅ |
| author / committer | `3becef33`、`5aa53cfe`、`3e205dc6` 三者 author 与 committer **均为 coder bot** | ✅ |
| build artifact | GREEN tree 2035 文件，`/target/` 命中 **0** | ✅ |

链形：`3da0af1c → 45b723ce(RED₁) → 88048726 → 2bb675fc → 9b007d71 → 012e4cf8 → 5aa53cfe(RED₂) → 3becef33(GREEN) → 3e205dc6(ledger)`。

## 二、写集与禁止面：**22/22 在集内，越界 0**

`git diff --name-only e8cab738 3becef33 -- . ':(exclude)multica-ledger'` = **22** 条，
逐条比对 plan-r2 §7.2 的 24 条写集：**out-of-set 0**、in-set 22。

禁止面逐条实测：

- `confirmPublish` / `SkillReviewSubmitService` 文件：**未出现在 changed path 集**；
- event producer/record：未改；
- **executor core/max/queue/policy**：`git diff … -- 'server/*/src/main/*'` 内
  `corePoolSize|maxPoolSize|queueCapacity|RejectedExecution|CallerRuns|ThreadPoolTaskExecutor`
  **命中 0** ⇒ 与 coder「production 一行未改」自述相符（**leader 只验命中数，饱和 probe 区分力属 QA**）；
- search / workflow / front-end / migration：changed path 集内命中 **0**。

## 三、RED intake 扩及 `5aa53cfe`（第二条 RED delta），并登记其存在

`red_intake` ACCEPTED 原绑定 `45b723ce` / tree `57a2402`。本轮出现**第二条 RED delta `5aa53cfe`**，
它**不在**该受理范围内。leader 按**同一套形式判据**独立核验后扩及，并明确登记以免 QA/Reviewer 误以为只有一条 RED：

| 判据 | 实测 |
|---|---|
| 范围 | 3 个 test 文件 + 2 份 ledger log，**production 0** |
| 在派单范围内 | GREEN 派单 = plan-r2 §7.1 步骤 2–6，步骤 2/4/5 本就要求补写 probe，**在范围内** |
| 真 RED（目标断言，非 scaffolding error） | `red1-repro.log` `RbacServiceTest.getUserRoleCodesByUserIds_…` 1 失败；`red2-probe.log` `VisibilityCheckerTest` 5 失败，消息均为「must expose …」目标断言 ⇒ **6 条目标断言失败，0 compile/runtime infra error** |
| ancestry | `5aa53cfe` → `3becef33` exit 0 |

⇒ **RED intake 扩及 `5aa53cfe`（形式面）。** 本轮共两条 RED delta，`45b723ce` 与 `5aa53cfe`，二者均可达。

## 四、`ee13d349` 自陈更正：受理

coder 自陈上一条 RED delta `ee13d349` 用 `git add -f` 误带 1164 个 `server/*/target/**` 产物。leader 实测：

- `ee13d349` 在 origin **不存在**（`rev-parse` 失败）⇒ 「从未 push」成立，**外部无从引用**；
- 重建物 `5aa53cfe` parents=`012e4cf8`，**无 force-push**（§一已证 fast-forward）；
- GREEN tree 内 `/target/` 文件数 **0**。

⇒ **受理为记账更正**。按追加式账本，`ee13d349` 不被改写而是被 `5aa53cfe` **superseding**；
任何外部引用以 `5aa53cfe` 为准（实测该引用不可能存在）。**自陈主动上抬，不计为 Finding。**

## 五、`PLAN_GAP` 成立 → 路由 Planner，**并限定改写范围**

coder 拒绝自行改 plan、上抬为 `PLAN_GAP`，处置正确。leader 独立实跑该条判据：

```
git diff e8cab738 3becef33 -- . ':(exclude)multica-ledger' \
  | rg 'confirmPublish|SkillReviewSubmitService|new SkillPublishedEvent'
```

**8 条输出**，逐条归属：

| 文件 | 命中 | 面 |
|---|---|---|
| `SubscriberNotificationRuntimeIntegrationTest.java` | 5 | TEST |
| `SubscriberNotificationSinkTest.java` | 2 | TEST |
| `NotificationEventListenerTest.java` | 1 | TEST |
| **`*/src/main/*` 限定后** | **0** | — |

来源归属（leader 实测，非转述）：`e8cab738..45b723ce`（**已受理的 RED₁**）已有 **7** 条；
`e8cab738..5aa53cfe` 共 **8** 条 ⇒ RED₂ 新增 **1**；`5aa53cfe..3becef33`（GREEN 提交本身）新增 **0**。
coder「7 条来自已受理 RED、我的提交只新增 1 条」**逐数相符**。

**判为 `PLAN_GAP` 的机械理由**：plan-r2 §7.1 步骤 5 与 §9 要求以**真实 listener** 闭合
`InOrder`、四类 failure × 两路事件与 final sink，而驱动真实 listener 必然要
`new SkillPublishedEvent(...)`；同一份 plan 的 §7.3 又要求该字样 `rg` **无输出**。
**两条要求同时为真时不可满足**——不是实现选择问题，是 plan 内部冲突。
按 Finding 路由表 `PLAN_GAP` → **Planner**。**leader 不改写 plan，也不预设改法。**

**并绑一条机械范围约束**（属返修 Finding 的范围界定，不是语义裁决）：
本次 delta **只允许**改写 §7.3 该条 readback 口径；一旦触及 §1–§6、§7.1、§7.2 写集、§8 Council 或 §9 RETURN 条件，
则 plan artifact digest 变更 ⇒ **Plan Gate 绑定失效**，且**在飞的 QA/Review 绑定同时失效必须重跑**——
出现该情形 **先回 leader**，不得自行扩面。plan artifact 一旦变更，Plan Gate 需按 Project description 重新走人工批准。

## 六、GREEN 证据形式面：leader 独立复核（不复核语义）

| 项 | coder 自述 | leader 实测 |
|---|---|---|
| §7.3 三 block | 全 BUILD SUCCESS | 三份日志 `BUILD SUCCESS` ✅ |
| 全量 reactor | 21/423/130/35/10/35/788 = **1442**，0F 0E，1 skipped | 逐模块行求和 = **1442 / 0 failures / 0 errors / 1 skipped** ✅ |
| legacy 15 例矩阵 RED vs GREEN | 整份 SHA-256 相同 `5a0a2fcf…` | `cmp` **BYTE-IDENTICAL**（647 B），双方 sha256 = `5a0a2fcf555f47f1e7e6c4dd9c825ad04beeb477ba67f0811b27b8edffd8f843` ✅；含 `ordinary-latest-null=false` ✅ |
| Semgrep | 511 files / 0 errors / 51 findings，与 r1 候选 (rule,file) delta 空 | 实测 51 findings / 0 errors / 511 scanned；与 `coder/20260821-150900-semgrep.json` 的 (rule,file) 集合 **only-in-r1 = 0、only-in-GREEN = 0** ✅ |
| ② 区别性证据 | 只回退 `REQUIRES_NEW` 一行 ⇒ 恰那 2 条超时复现、13 条不动 | `differential-without-requires-new.log`：`Tests run: 15, Failures: 2`，失败者正是 `saturatedConfiguredExecutor_…` 与 `promotionApproved_saturatedConfiguredExecutor_…`，BUILD FAILURE ✅ |

**leader 只核对「日志里确实有这些数、且与提交面对得上」。**
「定向可开关 ⇒ 不是负载波动」是**因果论证，属 QA 裁决**，本分录不背书也不否定。
饱和 helper 脚手架改写是否削弱 probe 区分力，**同属 QA**，已写进 QA brief 单列。

## 七、GREEN 形式受理与 exact tree 冻结

以上形式条件全部满足 ⇒ **GREEN 形式受理**，
**exact subject tree 冻结为 `3c809b93ff6364f345ccf04e51054be5d91fecb4`（commit `3becef33`）。**

冻结即触发两条既有规则：

1. **旧 QA/Review 结论对本树全部失效**——`qa_execution` 的 `RETURN` 与 `review_pass1` 的 blocker `R-P1-01`
   绑定的是 r1 树 `9221956`，**不得挪用**（既不能拿来放行，也不能拿来阻断）；须在新树上重出。
2. **QA 与 Review Pass 1 可并行**（唯一并行点）；Review Pass 2 需 QA 产物，本轮不派。

**GREEN 形式受理 ≠ 行为正确 ≠ READY。** 余下 QA → Review → Implementation Gate 三段全未做。

## 八、派单：Planner（PLAN_GAP）+ QA（两 Run）+ Reviewer Pass 1

HIGH_RISK 拆分按 skill 正本执行，leader 不合并、不减 Run：

- **QA**：blind Charter Run 与 execution Run **两个独立 Run**，Charter 不得读 coder 的 GREEN 结论后再回填；
  execution 须执行 probe，闭合最终 sink 与失败/撤权/陈旧状态。**单列一问**：饱和 helper 从
  「filler 交回 publisher 线程自锁」改为「提交到池子自报饱和为止 + 失败路径先释放 filler」，
  是否削弱 `AFTER_COMMIT_CALLER_RUNS_DELIVERY` probe 的区分力（production core/max/queue/policy 未改一行，
  由同轮 registry 读回用例断言 core=2/max=4/queue=100/CallerRunsPolicy——**该断言本身是否足够也由 QA 判**）。
- **Reviewer**：本轮只派 **Pass 1 `BLIND_BEHAVIOR_AUDIT`**，静态重建，**不读 QA 产物**；
  `EVIDENCE_CHALLENGE`（Pass 2）待 QA 产物到齐后另派。plan-r2 §8 的三个具名 lens 名额、锚点互异与反对者席位
  在 `20260822-143334` 已校验通过，本轮不重校。
- **Planner**：`PLAN_GAP` 窄 delta，范围受 §五 约束。

`R-P1-02` / `R-P1-07` 仍是留给上游维护者的 A/B 选择题，**任何一方不得声称已获批准**（§9 RETURN 条件）；
coder 本轮明确未声称，leader 核对其交付面无此类措辞。

## 九、lane 排期冲突：**已到站，决定权不在 leader，上抬 dongsjoa**

`141500` §五预告的碰撞本轮到站：`验证`、`规划`、`评审` 仍在 codex lane，本机补丁已回滚，
`next` 检查点**本轮再读仍未达成**（`last_updated` = `2026-08-22T23:37:24.404383Z`，与冻结点逐字相同），
`second_injection_surface` 仍开放。

**leader 的处置是「照现状派单」，理由是机械的**：

- 重新指派 lane 是 runtime 决定，不在调度职权内（`210500` 已登记），**不派单不等于不决定，那也是一种决定**；
- 现有证据只覆盖 coder lane 的长输出形态，**据此预判 QA/Review 必失败，就是又一次「把相容当支持」**
  （`105000` §二 / `141500` §三 同型教训）；派单并观察是唯一能产生判别数据的动作；
- 失败若发生，按既有归属计 **runtime/infra，不计 QA/Reviewer**，且 base 零漂移（本轮实测
  `iflytek/skillhub@main` 仍 `e8cab738`，drift 0）+ Gate 无时间维度 ⇒ **失败不产生 Gate 侧成本**。

三条出路及代价仍是旧账（切 `验证` ⇒ 单 lane ⇒ `SECURITY_REVIEW_BLOCKED`／等 `next` ⇒ 时刻不可估／
重打本机补丁 ⇒ 刚回滚且撤回理由仍成立）。**leader 不选、不预置、不自行豁免异构**，
按 Project description 把该决定 @ 给 dongsjoa（runtime 面归其主控会话，13:20Z 的 lane 变更亦其所为）。

## 十、不变项

Plan Gate artifact `61d54ad3…`、inventory `efd911ad…`、`write_set_precheck` CLEAN、base 零漂移、
`ab_hard_constraint`（`R-P1-02`/`R-P1-07` 保持 A/B）、`second_injection_surface` 开放且不设 Gate、
`upstream_fix` WAITING 且检查点未达成、`failure_root_cause`、`workdir_rotation_rule` 已验、
Publication 边界，**全部不动**。`green_dispatch_gate` 已于 `140500` 消耗，不复用。

## 十一、动作边界

本轮 leader 写操作：追加本分录 + 改 index 头部与分录表、push 到自有 fork 内部分支、
发一条含三处派单 mention 与一处 dongsjoa 上抬的评论、更新 metadata。
**未改 Issue 状态**（保持 `in_progress`）、未签 QA/Review PASS、未代判语义、未改 plan、
未 push 公开分支、未碰 PR #735、未建上游 PR、未 force-push、未改 lane、未签异构豁免、未 @ 老板。
