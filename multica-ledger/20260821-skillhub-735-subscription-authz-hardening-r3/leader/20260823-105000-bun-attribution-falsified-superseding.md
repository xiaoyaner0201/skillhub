# leader / 20260823-105000 / **BUN 归因被证伪（superseding）**：leader 把一条"后果"当成了"证据"；另实测 frozen base 零漂移

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`4c6787e6-474a-4e6d-a1b7-a4c0e80b0b1b`（2026-08-23T10:41Z，dongsjoa 巡检）
- Stage：`PLAN_APPROVED_R2` 不变；Issue 保持 `blocked`；**不重派、不重试、不改配置、不碰镜像/compose/容器**
- 本分录 **supersede**：`leader/20260823-091000` §四的归因、`leader/20260823-095500` §3.1、`leader/20260823-101500` §三第 1 点
- **不含任何 RED 受理结论**；`20260822-174500` 的裁定一字不动

## 一、被证伪的是 leader 的归因，不是巡检的

`20260823-091000` §四写「时间序与『BUN 双镜像发布引入了坏掉的构建面』相容」，
`20260823-095500` §3.1 进一步把「`-bun` tag 从未发布」写成「**registry 侧独立佐证了该归因**」，
`20260823-101500` §三第 1 点又说该佐证在第二个 registry 上得到重复。**这三处的归因部分全部作废。**

巡检 §三 用 step 级顺序（step 6 基础镜像 FAILURE，step 8/9 BUN 未轮到即 skipped）推翻它。
leader 无 Actions 凭据，**不复述其 step 数据**，但用**纯 git 手段**得到一个更强、且不依赖任何 token 的证明：

| 实测 | 结果 |
|---|---|
| `805252a9`（巡检记的首次失败）改了哪些文件 | `open-sse/mcp-server/{__tests__,schemas/providerEnums.ts,schemas/tools.ts,server.ts}`、`src/shared/utils/noAuthProviders.ts` —— **`.github/` 与 `Dockerfile*` 一个都没碰** |
| `805252a9` 时 `docker-publish.yml` 里的 BUN 面 | `grep -ci bun` = **4**，且四处全是 `ubuntu-latest` / `ubuntu-24.04` / `ubuntu-24.04-arm` 的假阳性——**真正的 BUN 步骤数为 0** |
| 同一文件在 `de5e237a` 时 | `grep -ci bun` = **35** |
| `git diff ec6010f0 805252a9 -- .github/workflows/docker-publish.yml Dockerfile Dockerfile.bun` | **空**——成功→失败的边界上，三个构建定义文件**逐字节未变** |
| `805252a9` 与 `de5e237a` 提交时刻 | `2026-08-22T20:23:04-03:00` vs `20:31:59-03:00`，相差 **8 分 55 秒** |

**⇒ 通道断裂发生在一个「工作流里根本还没有 BUN 步骤」的时刻，且边界两侧构建定义逐字节相同。**
`de5e237a` 引入 BUN 发布面时通道已红，**它不可能是起因**。此结论不依赖 Actions API，
与巡检的 step 级证据互相独立、指向同一结论。

## 二、leader 的推理缺陷（登记，不只是改结论）

「`-bun` tag 从未发布」这个观测，**在任何一种"发生在 manifest 步骤之前的失败"下都必然为真**——
无论起因是 BUN、是基础镜像、还是 registry 侧配额/鉴权。**它对竞争假说的区分力为零。**
leader 却把它写成"佐证归因"，即**把一条后果当成了证据**。

`20260823-101500` §三第 1 点说该佐证"在第二个 registry 上重复、不再是单一来源"——
**这个"加强"同样无效**：把一个零区分力的观测复现在第二个 registry 上，得到的仍是零区分力。
增加的是观测的可靠性，不是它的判别力，两者不能互换。

这与 `20260822-202500` §三登记过的那次判据缺陷同型（当时是把不可靠的 `stream disconnected`
标签当判据）：**都是错把"与假说相容"当成"支持该假说"。** 记在此处以便复查。

## 三、被证伪的**不包括**任何排除性结论

必须分开两件事，否则会误伤：

| 条目 | 状态 |
|---|---|
| 「`-bun` / `-web-bun` 从未成功发布过」这一**观测** | **仍然为真**（两个 registry 实测） |
| 用它**佐证 BUN 是起因** | **作废**（§一、§二） |
| `no_alternate_tag`（两个 registry 均无可替代 tag、回滚非修复路径、自建严格更劣） | **不受影响**——该结论从不依赖起因，只依赖"哪些 tag 存在" |
| `no_config_workaround` | **不受影响**——纯源码判定，与 CI 无关 |
| `failure_root_cause`（滑窗缺陷 `streamHandler.ts` 4096 尾窗） | **不受影响**——那是本机网关的根因，与上游 CI 断裂是两件独立的事，本轮讨论的始终只是后者 |

## 四、leader 侧新增的、支持巡检"基础设施侧"读法的两条 git 证据

1. **`docker-publish.yml` 自 `de5e237a`（08-22T23:31:59Z）起再无人改动**——其后 release 分支已推进
   **47 个 commit**，该文件一次都没被碰过。这是对巡检 §四"11 小时无人认领"的独立 git 侧佐证。
2. **基础镜像 `Dockerfile` 最后一次改动是 `11884013`（2026-08-20T22:02:14-03:00 = 08-21T01:02Z）**，
   早于断裂近两天。

即：**失败固定在 step 6，而 step 6 的全部输入（`Dockerfile`、workflow 定义）在断裂前后都没变过。**
在输入不变而输出翻转的情况下，起因只能在这两者之外——与巡检"registry 侧配额/鉴权/buildx 缓存"
的读法相容。leader **不进一步指名具体成因**（需日志，双方都无凭据）。

## 五、leader 独立实测的一条巡检未覆盖的事实：**交付面零漂移**

巡检 §六 读了 PR #735 的状态。leader 直接核了更上游的一层——**冻结 base 本身**：

```
iflytek/skillhub@main  head = e8cab7389f5dc1084fdc1cdf70bafd314d0a3d50  (2026-08-21T18:46:19+08:00)
frozen base            = e8cab7389f5dc1084fdc1cdf70bafd314d0a3d50
rev-list --count base..upstream/main = 0
```

**上游 main 与本 Issue 的冻结 base 逐字节相同，两天来零 commit。**

这有一条直接的 Gate 后果：**本次阻塞不产生任何 Gate 侧成本。**
Plan Gate PASS 绑定的是 artifact digest / inventory digest / exact tree / validator hash，**没有时间维度**；
使旧 QA/Review 失效的判据是"非 ledger 业务变化"，而本轮既无我方产出、上游 base 也未移动。
故 `61d54ad3…`、inventory `efd911ad…`、addendum `3812652e` 与全部既有裁定**不因等待而衰减**，
恢复后无需重跑、无 rebase 债。

**并置两个方向可以看清代价落在哪里**：我们**等待**的那条线（OmniRoute release）47 个 commit 高速推进，
我们**交付**的那条线（skillhub main）纹丝不动。
**延迟的成本全部集中在 runtime/工具链维度，交付面维度为零。** 这是 dongsjoa 排期取舍时的一条硬事实。

## 六、对巡检结论与新检查点的态度

- 同意 §四的结论修订：等待已由"慢"变为**无主、无 ETA**；`20260823-091000` §五登记的隐患
  （本地分叉回收成本随时间单调上升 + 镜像恢复后手改补丁静默消失）权重继续上升，
  并叠加 `20260823-095500` §五（修复与 head 距离单调变大，恢复时携带的树远超单个修复）。
- 新检查点 ② `docker-publish.yml` 出现任意一次 `conclusion=success` —— **判据成立且严格更优**：
  它是 ① 的必要前置，且可观测时刻更早。leader 同意采纳。
- 新检查点 ③ 上游出现 issue/PR 认领 —— 判据成立。leader 补一条**纯 git 的等价先行信号**：
  `docker-publish.yml` 或 `Dockerfile` 在 release 分支上出现任何新 commit（当前基线：分别停在
  `de5e237a` 与 `11884013`）。无需 search API 凭据即可观测。
- 巡检 §二 的 49/49、§三 的 step 级明细、§四 的 search 计数均需 Actions/search 凭据，
  leader **不宣称已验**；但其与本轮全部 git 侧实测相容。

## 七、动作边界与不变项

只读：匿名 fetch 上游 OmniRoute `release/v3.8.50` 与 iflytek/skillhub `main`，读 commit、
文件内容与 diff。**无任何写操作**——未 push 镜像、未 pull 镜像、未改 compose、未重启容器、
未改任何连接/agent 配置、未重派、未重试、未动 Gate、未签异构豁免、未代决路由、
未去上游开 issue、未重复升级、未 @ 任何人。

`green_dispatch_gate` CLOSED（(a)(b)(c) 一字不改）、`red_intake` NOT ACCEPTED 三条理由、
`write_set_precheck` CLEAN、加绑条件 2 的唯一受理证据（addendum `3812652e`）、
Plan Gate artifact `61d54ad3…`、R1–R5 与 A/B 硬约束、Publication 边界，全部不动。
9 次失败仍归 runtime/infra，不计入 Coder、不进返修链。脏会话弃用仍是共同必要前置。
