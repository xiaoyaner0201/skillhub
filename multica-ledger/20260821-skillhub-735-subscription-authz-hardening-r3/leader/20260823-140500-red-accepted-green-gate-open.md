# leader / 20260823-140500 / `green_dispatch_gate` (a)(b)(c) 实跑 **PASS**，`red_intake` 三条不受理理由逐条关闭 → RED **ACCEPTED**，派 GREEN；并登记两条更正：**leader 的「换 workdir」补救措施被证伪**、**coder 对 `ab_hard_constraint` 的读法有误**

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`1a7e8878-0559-4794-b611-bb86be26b092`（2026-08-23T13:5x，squad 自动通知「子 Issue 全部完成，可转 `in_review`」）
- Stage：`PLAN_APPROVED_R2` → **`RED_ACCEPTED_R2`**；Issue 保持 `in_progress`
- 本分录 **更正**：`20260823-125500` §四对「新 workdir」作为补救手段的有效性表述；
  并 **驳回** `coder/20260823-134500-tdd-red.md` 开头对 `ab_hard_constraint` 效力的推论
- 分支 head：`3da0af1c` → **`2bb675fc`**；候选 subject tree **`57a2402…`**（commit `45b723ce`）

## 〇、先回答触发评论：**不转 `in_review`**

自动通知的建议不成立。HD-31 的交付面是 **RED**，工程链余下 **GREEN → QA → Review →
Implementation Gate → READY** 四段全部未做，`qa_execution` 仍 `RETURN`/`overall=UNVERIFIED`、
`review_pass1` 仍 `RETURN_TENDENCY` 带 blocker `R-P1-01`。**「子 Issue 全部完成」只说明当前没有
在飞的子任务，不构成链路完成的任何证据**——这是一条零区分力的调度侧观测（同 `105000` §二的教训）。
父 Issue 保持 `in_progress`。且工程链终点是 exact subject tree **READY**，本就不是 `in_review`。

## 一、`green_dispatch_gate` (a)(b)(c)：leader 实跑，逐条 PASS

判据取 `green_dispatch_gate` 登记原文，**未放宽、未替换**。

| 条 | 判据原文 | leader 实测 | 结论 |
|---|---|---|---|
| (a) | `merge-base --is-ancestor` 两次 exit 0 against `…-r3` | `e8cab738` → **exit 0**；`45b723ce` → **exit 0** | **PASS** |
| (b) | `git diff RED_BASE RED_DELTA` **非空**且落在 write_set 内 | `RED_BASE=45b723ce^`（**实测 == `3da0af1c`**），`RED_DELTA=45b723ce`：`--numstat` **6 行 / +1075 / −35** | **PASS** |
| (c) | 分支上存在新的 `coder/2026*-red-output.log` | 新增 **5** 份（`132753` `132947` `133600` + 补交的 `125746` `130134`），r1 的 `20260821-141300` 未被复用/覆盖 | **PASS** |

### 1.1 (c) 的一处 leader 自陈措辞缺陷

`green_dispatch_gate` 的字面写的是 `coder/**20260822**-*-red-output.log`，而实到日志全为
`20260823-*`——**按字面匹配数为 0**。这是 leader 于 08-22 写判据时默认当日交付所致的日期硬编码，
**不是 Coder 的偏离**（延期已裁定为 runtime/infra，不计 Coder）。
本条按其**登记意图**判定：`red_intake` 理由 (3) 明确把不受理的对象界定为「r1 轮次的 3 份
`red-*.log`，**do NOT reuse**」，(c) 要的是**本轮新产生**的输出。该意图 5/5 满足。
**leader 自陈：判据不该把日期写死。此处以意图判定，并把缺陷记在 leader 名下，与 `121000` 同类处理。**

## 二、`write_set_precheck`：重新逐字符比对，越界 0（CLEAN 继续成立）

写集取已批准 artifact `61d54ad3…` 对应的 plan-r2 §7.2 **24 条** leaf path，机械比对：

```
comm -23 <changed 6> <write_set 24>   ->  空（越界 0）
禁止写集扫描（confirmPublish / SkillReviewSubmitService）on 45b723ce diff  ->  0 命中
multica-ledger/** 混入 45b723ce      ->  0
45b723ce 生产代码（src/main/）触碰数   ->  0
```

6 个路径全部在 `src/test/` 下，与 plan-r2 §7.1 第 1 步「production fix 之前」的顺序一致。

## 三、`red_intake` 三条不受理理由：逐条关闭

| 原理由（`20260822-174500`） | leader 本轮实测 | 状态 |
|---|---|---|
| (1) `ddb9560b` 是零内容 merge，树与 `a17eab1d` 逐字节相同 | `45b723ce` 是 r3 上的**普通内容 commit（非 merge）**，`45b723ce^ == 3da0af1c`，delta 6 文件 +1075/−35 | **关闭** |
| (2) 条件 2 的 probe 只存在于人工署名、自标 NOT RED-verified 的 `f90d64ee` | `45b723ce` 的 **author 与 committer 均为** `xiaoyaner-multica-coder[bot]`（无人工署名残留）；且内容同一性由 leader **独立复核**，非采信 coder 自述 | **关闭** |
| (3) 无任何 RED 实跑输出进入可读 ref | 5 份原始 maven 输出在分支上，含真实 reactor 摘要、`<<< FAILURE!`、堆栈与 surefire 路径 | **关闭** |

### 3.1 (2) 的独立复核（不采信 coder 的 `cmp` 自述，leader 重跑）

```
git diff 3da0af1c 45b723ce            >  a.patch
git diff 3da0af1c f90d64ee -- server/ >  b.patch
cmp a.patch b.patch   ->  IDENTICAL   (59699 B)
```

即 coder 提交面与已核验合格的抢救物 **逐字节相同**，只换了署名与承载 commit。
`20260822-174500` 已核过 `f90d64ee` 的断言七槽位、entityId 语义与 preference 闸门，
**那份内容核验结论随之直接适用于 `45b723ce`，无需重核**。

### 3.2 RED 实跑：两个基线都非空，且互相独立

| Run | 基线 | 失败 | leader 读到的判别点 |
|---|---|---|---|
| B（`132947`，`EXIT=0`，`failure.ignore=true`） | r3 + delta，tree `57a2402` | **8 失败 / 4 类**（67 tests） | 12 个目标类**全部报告**，无 SKIPPED 空洞；余 8 类全绿 = 回归未破 |
| C（`133600`，`EXIT=0`） | **纯净上游** `e8cab738` + `9f3e5858` | **14 失败 / 3 类** | 证明探针对**未硬化的生产代码**敏感 |

Run A（`132753`，`EXIT=1`，brief 原命令）reactor 在 `skillhub-auth` 即停，12 类中 7 类未跑——
**coder 自己指出该单次输出会高估覆盖面并补跑 B，未拿它当结论**。leader 认可该处置。

两处 leader 认为**必须原样上抬、不得在受理时抹平**的自陈事项（均出自 coder 分录，leader 复核后同意）：

1. `VisibilityCheckerLegacyCompatibilityTest` 在**两个基线上都是 1/1 全绿**，coder 明确声明
   它是回归护栏、**不计入 RED 证据**。leader 核对日志 `133600:95` 与 `132947:90` 属实。
   **受理 RED 时不把它算作探针。**
2. `SubscriberNotificationRuntimeIntegrationTest` 有 2 条以**超时**（`Timed out waiting for
   blocked SSE notification`）而非断言失败的形式变红。**超时类 RED 对机器负载敏感**，
   其转绿不能自动归因于实现。**该条随 RED 一并进入 GREEN brief，作为 GREEN 段的显式核验项。**

**受理结论：`red_intake` = ACCEPTED；`green_dispatch_gate` = OPEN（本轮消耗）。**
受理的是**证据形态**（ancestry / 非空 delta / 写集内 / 实跑留痕 / 署名），
**不含任何关于「这些断言问对了问题」的语义判断**——那属 QA 与 Reviewer，leader 不代做。

## 四、**更正一：`20260823-125500` §四的补救手段被本轮证伪**

`multica issue runs 77811ddd` 的 `work_dir` + `runtime_id` 两列合读：

| # | 起 | runtime | workdir | 结果 |
|---|---|---|---|---|
| 1 | 12:52:46 | `49238814`（codex） | **`6e3a1644`** | failed `provider_network`，**651s** |
| 2 | 13:03:39 | `49238814` | `6e3a1644` | failed，34s |
| 3 | 13:04:45 | `49238814` | `6e3a1644` | failed，41s |
| 4 | **13:21:43** | **`56fd0920`（claude）** | `6e3a1644` | **completed**，1656s（落地 RED） |
| 5 | 13:49:22 | `56fd0920` | `6e3a1644` | completed，431s |

**预测部分成立、补救部分证伪，两者必须分开记：**

- ✅ 「新 Issue ⇒ 新 workdir」**已验**：`6e3a1644` ≠ `cd0f9fa3`。`workdir_rotation_rule` 中标注为
  UNVERIFIED 的跨 Issue keying 由此闭合，且**规则须改写**——新 Issue 换 workdir
  **与前一 task 的 `failure_reason` 无关**（前置 task `646c3b94` 是 `provider_network`，按旧表述本不该换）。
  正确表述：**workdir 按 Issue 键；同一 Issue 内当且仅当前一 task `api_invalid_request` 时轮换。**
- ❌ 「换 workdir ⇒ 毒化链断 ⇒ 恢复」**证伪**：全新 workdir 里 codex 仍连失 3 次，
  且第 1 次 **651s** 落在 `20260822-202500` 标定的**一阶**区间——**新 workdir 不带旧 rollout，
  这次失败不可能是重放**。故 `coder_lane` 里写的验证判据（`work_dir` ≠ `cd0f9fa3` 即「毒化链即断」）
  **把「链断」直接等同于「恢复」，是错的**：链确实断了，但没有恢复。

**真正变化的变量是 runtime lane，不是 workdir。** 且这里有一个成色不错的天然对照：
第 3 次与第 4 次之间 **workdir 恒定、Issue 恒定、brief 恒定，唯一变量是 `runtime_id`**。
但仍须按 `105000` §二自律：**n=2、且第 4 次同时是首次真正做事的一次**，
故只登记「lane 是唯一已识别的变化量」，**不登记因果**。

**并连带一条对 dongsjoa 侧结论的、可证伪的观测**：三次 codex 失败（12:52–13:05）全部发生在
容器重启 12:28:13Z **之后**，即**网关补丁生效之后**。这与「补丁使伪失败源归零」**不相容**。
leader 无该容器访问权，**不宣称补丁无效**——可能是该 runtime 未走补丁后的网关、
可能是另一成因。**按 dongsjoa `20260823-124500` §六预登记的三分支 triage，这属于需要其本人
读回判定的形态；leader 只把事实上抬，不代决、不重复升级。**

## 五、**更正二：驳回 coder 对 `ab_hard_constraint` 的推论**

`coder/20260823-134500-tdd-red.md` 开头写：

> `ab_hard_constraint` 所保护的 A/B 对照面（R-P1-02 / R-P1-07）自本轮起在 `实现` 侧不再成立

**该推论不成立**，理由是纯粹的读原文，不含语义裁决：

```
ab_hard_constraint = "R-P1-02/R-P1-07 stay A/B; any text claiming maintainer approved => RETURN"
```

这里的 **A/B 指两道留给上游维护者的选择题**——`R-P1-02`（metadata detail gate 还是
published-content gate，plan-r2 §3.1）与 `R-P1-07`（SUPER_ADMIN pull 权与 unsolicited push
是否同权，plan-r2 §3.2）——要求它们**保持未定案**，且任何产出出现「维护者已批准/已确认」即 RETURN。
**它与哪个模型执行 agent 无关**：换 lane 既不解析这两道题、也不产生批准字样。
leader 已于 `20260822-185000` §与 `20260822-200500` §登记过完全相同的读法，本轮**只是重申，不是新判**。

**⇒ `ab_hard_constraint` 完好，未因 lane 变更受任何影响。** coder 分录该段不改写（append-only），
由本分录 superseding 其效力推论；其余部分全部有效。

### 5.1 lane 变更本身：登记，异构安全，但留一条给 QA/Review 的开放项

- 事实：`实现` 本轮在 `56fd0920`（`claude/claude-opus-5`）执行，非既往 `49238814`（codex）。
  **该变更不是 leader 所为**，发生在 13:05–13:21 之间；leader 未改任何 runtime 配置。
- Council 异构：`规划`（`e9cf031c`）与 `验证`（`4f83bacc`）仍在 codex → **≥2 lane 成立**，
  按 `lane_vs_heterogeneity`「Only-实现 switch is heterogeneity-safe」，
  **不触发 `SECURITY_REVIEW_BLOCKED`**。
- **开放项（leader 不裁决）**：plan-r2 §7.2 把 `RED→GREEN ancestry` 列为该 unit 原子证据链的一环。
  若 GREEN 落在与 RED 不同的 lane，该证据链跨两个 lane。现有任何已登记约束**都没有禁止**这一点，
  故 leader **不据此设限、不阻断**；仅登记，交 QA 与 Reviewer 在其证据评估中自行处置。

## 六、GREEN 派单（第 12 次派单，第 1 次 GREEN）

`green_dispatch_gate` PASS 是 GREEN 的**唯一**前置，已满足。派 `实现`，scope 取 plan-r2 §7.1 步骤 2–6。

承载体选择：HD-31 已 `done`，**本轮改回 HD-30 上的 `@mention` 单路径**，不新建子 Issue，
按 Squad 协议避免双触发。workdir 侧无需再动——`6e3a1644` 已被 Run B/C 实证为**可用**
（125M warm 仓库、独立 target、跑通 5 分半的全 reactor）。

brief 中 leader 明确上抬的三项（**不改 scope，只是把已登记事实带过去**）：

1. **起点 head `2bb675fc`**，候选 RED tree `57a2402`（commit `45b723ce`）。
2. **`$HOME` 是 `noexec` tmpfs**，`mvnw` 首调必 exit 126；解法已由 coder 实证——
   `MAVEN_USER_HOME` 指向 `/workspaces/…/.toolchains/m2`，**命令行本身逐字不改**。
3. **两条超时型 RED（§3.2.2）必须单独交代**：其转绿要有区别于「机器更闲」的证据。

不变项一并带入：`ab_hard_constraint`（R-P1-02/R-P1-07 保持 A/B、禁「维护者已批准」字样）、
写集 24 条与禁止写集、`superseded_candidate`（不得把新工作绑到 `046c04ed`/tree `92219560`
作为冻结主题树）、R1–R5 与具名 invariant、Publication 边界。

## 七、动作边界

本轮 leader 写操作四项：**追加本分录 + 改 index 头部**、**push 到自有 fork 的内部分支**、
**发一条评论（回 `1a7e8878` 并 @ 实现 派 GREEN）**、**更新 metadata**。
未 push 公开分支、未碰 PR #735、未建上游 PR、未签 CLA、未改写作者、未 force-push、
未改任何 agent/网关/daemon 配置、未签异构豁免、未 @ 老板、未把父 Issue 转 `in_review`。
未做任何语义裁决：RED 的**证据形态**受理归 leader，**断言是否问对了问题**归 QA 与 Reviewer。
Publication 边界不变——工程链终点仍是 exact subject tree READY。
