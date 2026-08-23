# leader / 20260823-091000 / `next` 通道核实通过；并登记一条否定结论：**不存在配置级绕开**

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`feddac5b-5ae8-494b-be11-df4d2f2e9567`（2026-08-23T09:00Z，dongsjoa 巡检）
- Stage：`PLAN_APPROVED_R2` 不变；Issue 保持 `blocked`；**不重派、不重试、不改 agent 配置、不碰镜像/compose/容器/连接配置**
- 分录性质：外部事实独立验证 + leader 自陈更正 + **否定结论登记（排除一条看似可行的路径）**
- **不含任何 RED 受理结论**；`20260822-174500` 的裁定一字不动

## 一、Multica 侧无变化

`status = blocked`；实现 lane 自 `99a96c03` 起零产出；`…-r3` 头 `0c6179f2`，rescue 仍 `f90d64ee`；
Gate 全部未动。

## 二、巡检 §一 的机械判据 leader 逐条实测复核，全部成立

| 核验项 | leader 实测（git，匿名读上游） |
|---|---|
| `resolve-docker-publish-version.sh` 的 `next` 分支逻辑 | 实读：`release/v*` 且 `REF_NAME == DEFAULT_BRANCH` → `VERSION="next"`，否则 `exit 1` ✅ |
| 该脚本在 `main` 上 | **ABSENT**（`git cat-file -e` 失败）✅ |
| `docker-publish.yml` 触发面 | `main` 上仅 `- main`；`release/v3.8.50` 上多一行 `- "release/v*"` ✅ |
| `Dockerfile.bun` | release 分支 **EXISTS**、`main` 上 **ABSENT** ✅ |
| `e73ab004` 相对 release head `1f6e781c` | `rev-list --left-right --count` = **0 / 22**，即祖先、落后 22 ✅ |

**巡检未能直接给出、但可由其自身证据闭合的一环**：`default_branch` 是否真为 `release/v3.8.50`，
属仓库设置，leader 无 API 权限。但脚本在 `REF_NAME != DEFAULT_BRANCH` 时是 **exit 1**，
而巡检记录的每一次 run 的 `Resolve Docker release metadata` 步骤**均 success**——
该步骤成功本身即证明 `DEFAULT_BRANCH == release/v3.8.50`。此环由其证据自洽闭合，无需另行取证。

**结论：`next` 确为「当前默认 release 分支」的浮动通道，未来任何一次\*成功\*的 `next` 构建必含 `e73ab004`。**

### leader 自陈更正

`20260823-084500` §四结尾写「得改等 `release/v3.8.50` 线上的标签，或等它反向合入 `main`」——
按上表，两者都不需要，`next` 本身就是那条线的标签。该次生推论**作废**。
（同分录的主张——「等待前必须先核实 `next` 的来源分支」——则被本轮验证证明是必要的：
若不核实，「合并到 `release/v3.8.50` 而非 `main`」与「`next` 从哪构建」这两件事无法接上。）

## 三、Docker Hub：leader 独立读回，与巡检逐字一致

匿名读 `hub.docker.com/v2/repositories/diegosouzapw/omniroute/tags/next`：

- `last_updated` = **`2026-08-22T23:37:24.404383Z`**
- digest = **`sha256:6f95064bf0981c16a18aa9beee08bc3d9741c9e130154c79a785562b0ab7b0e2`**

截至本轮（约 09:10Z）**仍未变动**，即上游 CI 至今未产出新镜像。

## 四、CI 连红：无 Actions API 权限，属受理转述；但诱因时间线 leader 可独立佐证

失败 run 明细在 GitHub Actions API（leader 无凭据），**不宣称已验**。但其归因可由 commit 时间独立佐证：

| commit | 时间（UTC） | 说明 |
|---|---|---|
| `7ddbaf69` | 08-22T00:28:43 | `feat(cli): add native Bun backend support and Dockerfile.bun` |
| `7246e5ac` | 08-22T23:24:21 | `fix(bun): Dockerfile.bun entrypoint / bun:sqlite` |
| `de5e237a` | **08-22T23:31:59** | `feat(ci): publish Bun container images (-bun / -web-bun)` |

巡检记的最后一次成功构建为 **23:14:29Z**，`next` 发布于 **23:37:24Z**；
`de5e237a`（23:31:59Z）落在两者之间，此后全红。**时间序与「BUN 双镜像发布引入了坏掉的构建面」相容**。
且 `Dockerfile.bun` 与该 resolve 脚本均为 release 分支独有（§二实测），故 `main` 线不受影响——
这也解释了为何唯一一次 `main` 的 run 不同形。

**修复 `e73ab004` 与该 CI 缺陷无关，是被连带堵住**——此判断 leader 同意。

## 五、leader 自陈：撤回自打补丁的**理由**确被证伪，但**结论**站得住（换了地基）

巡检指出 `20260823-084500` §5.1 的撤回理由（「数小时后即需回收的本地分叉」）以「数小时内能拉到镜像」
为前提，而该前提已被 §四证伪。**该批评成立，leader 接受。**

但撤回结论不依赖那个时间量词，重述其真正地基：

1. 修复既已合入且 `next` 确从该分支构建（§二），本地补丁**必然是临时的**——只是回收时刻未知；
2. **延迟不会降低对账成本，只会抬高**：分叉越久，越可能有人忘记本机处于"已打补丁"状态；
3. **一条此前双方都未登记的隐患**：镜像一旦恢复发布并被拉取，手改的容器补丁会**静默消失**——
   若届时链路已依赖它，会出现无人预期的回归，且症状与本次滑窗缺陷同形，极难归因。

即：撤回从「反正很快就有镜像」改为「本地分叉的回收成本随时间单调上升，且存在静默回归面」。
**是否维持撤回仍归 dongsjoa**——若 HD-30 的排期压力超过上述成本，那是调度取舍，不是机械判据，
leader 不代决。

## 六、**新登记的否定结论：不存在「改配置绕开」这条路**

leader 主动找过一条比"升级镜像 / 改源码"爆破半径都小的路径——既然缺陷由
`response.completed` 帧内联 `encrypted_content` 撑爆 4096 尾窗触发，那么**若能让该帧不带
`encrypted_content`，缺陷即不触发**，且这可能只是一处连接级配置。实读上游 `1f6e781c` 源码后，
**该路径不成立**，登记如下以免他人重复投入：

1. `providerSpecificData.preserveEncryptedReasoning` **确实是连接级布尔开关**
   （`src/shared/validation/providerSpecificData.ts` 有 schema 校验，Dashboard `EditConnectionModal` 可改，
   `codex.ts:1399` 读 `=== true`，故**默认为 false**）；
2. **但它只作用于请求侧、且只是兜底**：`reasoningInputPolicy.ts:40-46`
   `resolveReasoningTransport()` 先查 `REASONING_TRANSPORTS`，命中即返回，
   `preserveEncryptedReasoning` 仅在**未命中**时决定 `opaque` / `plaintext`；其 `stripOpaqueFields()`
   删的是**请求 body 里**的 `encrypted_content`，**不改变上游返回帧的大小**；
3. **决定响应帧是否内联 `encrypted_content` 的是另一处，且是硬编码的**：
   `codex.ts:358-377` `ensureCodexReasoningSummary()` **无条件**把
   `"reasoning.encrypted_content"` 塞进 `body.include`（若数组不存在则新建）。
   **唯一逃逸口是 `reasoning.effort === "none"`（第 363 行提前返回）**——即彻底关闭推理。

故：连接级配置**改不掉**响应帧大小；唯一在带内的杠杆是把 codex agent 的推理关掉，
而 `实现` 当前 `thinking_level = xhigh`、任务是 HIGH_RISK 的 TDD，关推理属严重能力降级，
且会改变执行侧特性。**leader 不把它列为候选，仅登记已排除。**

## 七、选项集与检查点

选项集**不变**（升级镜像 / 切 lane 两支，加 dongsjoa 手上的自打补丁是否维持撤回）：

- **等镜像**：方向已验证成立，但可用时刻取决于第三方 CI，不可预估；
- **切 lane**：代价维持 `20260822-210500`——全量切触发 `SECURITY_REVIEW_BLOCKED`（异构塌缩为单 lane），
  只切 `实现` 异构安全但覆盖不全。此前「镜像若可行则两条代价都不必付」的宽慰**不再成立**，
  两条代价重回台面；
- **脏会话弃用**：仍是必要前置，与镜像时间线无关。

巡检 §五 的检查点（`next` 的 `last_updated` 越过 `2026-08-22T23:37:24Z` 即说明新镜像必含修复）
**判据成立**，其依赖的祖先关系已由 §二实测（0/22）。leader 同意采用该单值判定。

## 八、动作边界与不变项

只读：匿名 fetch 上游 `main` / `release/v3.8.50`，读 workflow、CI 脚本、`codex.ts`、
`reasoningInputPolicy.ts`、`providerSpecificData.ts`、diff 与 commit 时间；读 Docker Hub 公开 tag。
**无任何写操作**——未 push、未改 compose、未拉镜像、未重启容器、未改任何连接/agent 配置、
未重派、未重试、未动 Gate、未签异构豁免、未代决路由、未重复升级、未去上游开 issue。

`green_dispatch_gate` CLOSED（(a)(b)(c) 一字不改）、`red_intake` NOT ACCEPTED 三条理由、
`write_set_precheck` CLEAN、加绑条件 2 的唯一受理证据（addendum `3812652e`）、
Plan Gate artifact `61d54ad3…`、R1–R5 与 A/B 硬约束、Publication 边界，全部不动。
9 次失败仍归 runtime/infra，不计入 Coder、不进返修链。
