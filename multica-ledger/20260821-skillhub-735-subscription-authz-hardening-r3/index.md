# 20260821-skillhub-735-subscription-authz-hardening-r3

- Issue：HD-30 `c6feba77-940c-442c-be82-ed51bf7f60c2`（收窄 skillhub#735 为纯订阅授权硬化）
- Base：`e8cab7389f5dc1084fdc1cdf70bafd314d0a3d50`（`iflytek/skillhub@main`，2026-08-21T10:46:19Z，读回于 20260821-113800）
- Base tree：`c81497cf7eb98f0568b8b30c9af1e67797bca27a`
- 风险等级：`HIGH_RISK`（auth/authz、PRIVATE 可见性、event/listener/fan-out、async 事务边界四类同时命中）
- 冻结计划：`planner/20260821-125849-plan-r1.md`（SHA-256 `4465119ef0f150a578893cbf70fa7f4c66c81e0d13696620a8757de3334f35de`），artifact `planner/20260821-125849-plan-gate-r1.json`（`39510ad88cff8cb34f7efc584eb9818a47e6f1cb032c0bf2c11caf3c018d2236`），Plan Gate `PASS`（leader 独立复跑，20260821-133911），人工批准 20260821 by dongsjoa
- 冻结 inventory：`qa/behavior-inventory.v1.json`（SHA-256 `efd911ad203add676cf925c8e20e1cb40251415d1a90600b2ad3de2f48131eae`，`D2`，15 candidates，produced_by_run `8e9589ec-f00d-4f62-9942-ce12f43146de`）
- QA 产物：`qa/qa-gate-r1.json`（SHA-256 `1f16d21a5432b18bc89eb143b731a5a072f032a2809dd803e9c260310c668cd2`），Gate receipt `PASS`（leader 独立复跑，20260821-172933），artifact verdict `overall=UNVERIFIED`（25 BLOCKED probe / 8 blocking UNVERIFIED / 5 开放 HIGH+BLOCKER）
- 冻结计划 r2：`planner/20260822-141107-plan-r2.md`（SHA-256 `c5eb23e39079d6d106428900e04b73089c12cc34c49d79c84611d5745ba937c9`），artifact `planner/20260822-141107-plan-gate-r2.json`（`61d54ad3dc94ae60ea546aa85322aea6e946d2c4ec064428f2525ffcebf04238`），Plan Gate `PASS`（leader 独立复跑，20260822-143334），Plan Run `13699b09-a56a-4b08-8e07-57c355cb3686`，人工批准 20260822-1445 by dongsjoa（附 5 条加绑条件，其中条件 2 的指定测试经核验不适用，已回 Planner）
- 当前状态：PLAN_APPROVED_R2 / **Issue 保持 `blocked`；Coder lane 停机，不自动重试，模型路由待 dongsjoa 裁决。根因已于 20260822-2021 定位到**本机 OmniRoute 终止标记滑窗缺陷**（`streamHandler.ts:685-687` 只在末 4096B 找 `response.completed`，Codex 该帧实测 5928B 被自身挤出窗口 → 注入伪 `response.failed` → CLI 报 stream-disconnected → reasoning item 状态不一致 → 下轮 400 → retire session）：9 次失败仍归 runtime/infra，但所指由「上游抖动」更正为该缺陷；A/C 前提不成立且属自再生循环，B-claude 免疫为结构性（终止帧 51B 恒在窗口内）。**新增机械约束（20260822-2105）：切 `验证` 会使全 Squad 塌缩为单一 claude lane → Council runtime 异构不可用 → 转 `SECURITY_REVIEW_BLOCKED`，即全量切 lane 买到整条链路却赔掉终点；只切 `实现` 异构不受影响但覆盖不全；网关补丁是唯一「全覆盖且保异构」的路径。** RED 仍未受理**（取证分支 `20260822-hd30-red-rescue-session-01a02a28` head `f90d64ee` 已使 ancestry 三项 exit 0，可达性阻塞解除；但 RED 仍不受理：`ddb9560b` 为零内容 merge（树与 `a17eab1d` 逐字节相同）、加绑条件 2 的 probe 只存在于人工署名且自标 NOT RED-verified 的 `f90d64ee`、本轮无任何 RED 实跑输出进入可读 ref。写集 leaf-path 经 leader 逐字符复核越界 0，转 CLEAN。抢救物内容质量合格——断言七槽位、entityId 语义、preference 闸门约束均已核对通过——缺的是提交面与实跑留痕）

## 前序工作面（不在本分支，未被改写）

- HD-28 工作分支 `20260819-skillhub-731-confirm-publish-event-r2`，head `d0a10cb5e042dfe99e80467ada21fe1e37503965`（origin，读回于 20260821-113700）
- 其账本 `multica-ledger/20260819-skillhub-731-confirm-publish-event-r2/` 完整保留在该分支上，本分支不复制、不改写、不继承其 verdict。
- 上游维护者已裁定 HD-28 的任务面作废，故本任务从 live upstream base 重新起面，而非 rebase 该分支的 66 个 commit。

## 分录

| 时间 | Agent | 动作 | 产物 | tree | 结论 |
|---|---|---|---|---|---|
| 20260821-113951 | leader | intake + dispatch discovery | leader/20260821-113951-intake-dispatch.md | `c81497c` | — |
| 20260821-123244 | qa | behavior discovery | qa/20260821-123244-behavior-discovery.md | `c81497c` | FROZEN |
| 20260821-125849 | planner | plan r1 | planner/20260821-125849-plan-r1.md | `c81497c` | FROZEN |
| 20260821-133911 | leader | plan gate + 人工批准 + dispatch code/tdd | leader/20260821-133911-plan-gate.md | `c81497c` | PASS |
| 20260821-155700 | coder | RED→GREEN | coder/20260821-155700-code-tdd.md | `9221956` | — |
| 20260821-160557 | leader | 候选核对 + 冻结 + 并行派 QA/Review Pass 1 | leader/20260821-160557-candidate-intake.md | `9221956` | ACCEPTED_FOR_VERIFICATION |
| 20260821-161203 | qa | blind QA charter | qa/20260821-161203-blind-qa-charter.md | `9221956` | FROZEN |
| 20260821-161836 | reviewer | Pass 1 blind behavior audit | reviewer/20260821-161836-blind-behavior-audit-pass1.md | `9221956` | FROZEN |
| 20260821-172100 | qa | QA execution | qa/20260821-172100-qa-execution.md | `9221956` | RETURN |
| 20260821-172933 | leader | QA Gate 复跑 + delta 归并 + 返修路由 | leader/20260821-172933-qa-gate-and-delta-merge.md | `9221956` | RETURN |
| 20260822-141107 | planner | plan r2 delta + local Plan Gate | planner/20260822-141107-plan-r2.md；planner/20260822-141107-plan-gate-r2.json；planner/20260822-141107-plan-gate-receipt-r2.json | `9221956` | FROZEN / GATE PASS（独立复跑与人工批准待调度） |
| 20260822-143334 | leader | Plan Gate r2 独立复跑 + Council 名额校验 + 转人工批准 | leader/20260822-143334-plan-gate-r2.md；leader/20260822-143334-plan-gate-receipt-r2-leader.json | `c81497c`（plan subject base） | PASS（人工批准待办） |
| 20260822-145214 | leader | 受理人工批准 + 加绑条件机械核验 + 并行派 Planner delta / Coder RED | leader/20260822-145214-plan-approved-r2-dispatch.md | `c81497c` | APPROVED（加绑条件 2 回 Planner） |
| 20260822-150358 | planner | plan r2 加绑条件 2 evidence contract 窄 delta | planner/20260822-150358-plan-r2-evidence-addendum.md | `c81497c`（plan subject base） | FROZEN |
| 20260822-150907 | leader | evidence addendum 受理 + probe 真实性核验 | leader/20260822-150907-evidence-addendum-accepted.md | `c81497c` | ACCEPTED（加绑条件 2 闭合，不重跑 Gate） |
| 20260822-152000 | leader | 人工撤销原指派 + 批准替代 probe 受理；断言槽位与 preference 闸门核验 | leader/20260822-152000-cond2-ratified-superseding.md | `c81497c` | SUPERSEDING（REVOKED + APPROVED；GREEN 段放行但待 RED intake） |
| 20260822-165500 | leader | Coder lane 阻塞登记 + 产物可达性/写集初核 | leader/20260822-165500-coder-lane-blocked-session-poisoned.md | `c81497c` | BLOCKED（runtime/infra，非工程 finding） |
| 20260822-174500 | leader | RED intake（rescue 分支）：ancestry 实跑 + 树内容核验 + leaf-path 复核 + 新 session 重派 | leader/20260822-174500-red-intake-rescue-branch.md | `c81497c` | NOT ACCEPTED（可达性 MET，RED 未受理；写集转 CLEAN） |
| 20260822-181000 | leader | Coder 第 5 次失败登记 + leader 前提更正（comment 触发默认 resume）+ 退休后原样重派 | leader/20260822-181000-coder-5th-failure-session-retired-redispatch.md | `c81497c` | RE-DISPATCHED（scope 不变，附预置升级判据） |
| 20260822-185000 | leader | 第 6 次失败：归因从 session 毒化重定位到 lane；判据表作废；Coder 暂停重派 | leader/20260822-185000-coder-lane-fault-relocated-paused.md | `c81497c` | PAUSED（待 boss 裁定 lane 归属；工程结论全部不动） |
| 20260822-191500 | leader | boss 裁定受理：三条 codex agent 改分档模型名（provider 未换），配置读回核验 + 恢复派单 | leader/20260822-191500-lane-ruling-model-tier-resume.md | `c81497c` | DISPATCHED（scope 原样；单次成功不记为因果确认） |
| 20260822-194500 | leader | 模型名假设证伪（3/3 stream-disconnected），转 option B：最小**自足** brief 重派 | leader/20260822-194500-option-b-minimal-brief-dispatch.md | `c81497c` | DISPATCHED（交付面等价；再失败则 blocked 不自动重试） |
| 20260822-200500 | leader | 停机受理 + 更正：option B 未获执行（run 时长 35s×2/28s）+ leader 判据缺陷登记 + 待裁决项前提核对 | leader/20260822-200500-blocked-option-b-never-tested.md | `c81497c` | BLOCKED（不重派；方向性裁决归 dongsjoa） |
| 20260822-202500 | leader | 根因受理（网关滑窗缺陷）+ runtime/infra 所指更正 + 时长判据精确化（定位次非定根因）+ B-brief/B-claude 歧义消除 + 剩余链路 lane 覆盖面标注 | leader/20260822-202500-root-cause-gateway-window-superseding.md | `c81497c` | SUPERSEDING（Gate 全不动；仍 BLOCKED，不重派） |
| 20260822-210500 | leader | 机械约束登记：切 `验证` 会使 Squad 塌缩为单 lane → Council 异构不可用 → `SECURITY_REVIEW_BLOCKED`；三路径后果表 + 补丁的两条配平 | leader/20260822-210500-lane-choice-vs-council-heterogeneity.md | `c81497c` | CONSTRAINT（不选方向；仍 BLOCKED，不重派） |
