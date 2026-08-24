---
ts: 20260824-063345
agent: leader
action: Stage 1 重派（实现 Agent 换 runtime lane 后）+ upstream base 漂移读回
tree: c81497cf7eb98f0568b8b30c9af1e67797bca27a
verdict: —
---

# Stage 1 重派：换 lane，不换任何冻结输入

触发：Stage 1（`20260824-044027` 派发）的 Coder Run 在 04:46:20 以同一 codex 错误
`stream disconnected before completion: stream closed before response.completed` 失败——
这是 03:40–04:31 三次失败之后的第四次，同一故障族。巡检（千乘妍）随后把 `实现` 的 runtime
换到 Claude lane 并服务端读回（model `claude/claude-opus-5`、thinking `high`、runtime
`56fd0920-73b3-4230-a092-42b7ec746692`、custom args `[]`），要求 Leader 自主决定如何继续、
不复用已损坏的 Codex provider session。

本 Leader Run 自身此前亦因 provider 侧 502（SSE 在首个非 ping 事件前结束）失败一次，
已由巡检按可重试故障 rerun。**四次 Coder 失败与本次 Leader 失败均为 provider 故障，
不是 subject tree、任务输入或测试失败，一律不作为工程 Gate 证据。**

## 1. 分支仍未被污染（Leader 独立复算，非采信自述）

| 检查 | 命令结果 |
|---|---|
| `origin/<branch>` head | `4d75b9991835deaa2b36206a494ccf07a7a84d05`（= 上一条 Leader 分录，未前进） |
| head tree | `3129286eae56aad76014de35a9e1070667062564` |
| `git log 4d75b999..origin/<branch>` | 空 |
| `git diff --name-only e8cab738 origin/<branch> \| grep -v '^multica-ledger/'` | 空 |
| `git ls-tree -r origin/<branch> -- multica-ledger/` | 12 文件，与 `4d75b999` 一致 |

第四次失败 Run 同样没有写出任何代码或账本。**无需回滚**，Stage 1 从 `4d75b999` 原样重派。

## 2. 四项冻结 digest 全部独立复算，无一漂移

`git show <branch>:<path> | sha256sum`，不读工作树：

| 产物 | 复算 sha256 | 与冻结值 |
|---|---|---|
| `qa/behavior-inventory.v1.json` | `aabf1b09bfea56b78c75f950a5bf3b5472e0cc30a5435681b3d199bd1ccfc1b1` | 一致 |
| `planner/20260824-040000-plan-r1.md` | `1c2b92a6571a239335d300d5a2b363ae5a12ea95b223555c7703731988a42e6f` | 一致 |
| `planner/20260824-040000-plan-gate-r1.json` | `bdfab21ffa1c7c7df05a4478d26d035329cf36c24a1d91b981c0f85e51a92b45` | 一致 |
| `leader/20260824-044027-stage1-red-workorder.json` | `876014dae72a723775e2b384f3c9aa3905ce55dcc313bae4bf3cdc528968fc86` | 一致 |

Gate bundle validator：`scripts/validate_gate.py` sha256
`a7100d86232e21df4f032d07ecbc7e055f2353020be0858230db131a47ca4892`，与前两道 Gate 同一把。

**Stage 1 工单的机械投影重新独立复验**（不采信上一 Run 的自述，也不采信工单自带的
`source_artifact_sha256` 字段）：

```
plan-gate-r1.json acceptance_probes 总数           = 41
其中 expected_before == "FAIL"                     = 21
该 21 条的 probe_id 集合 vs 工单 probes 的 id 集合  = 完全相同
工单 test_files_touched                            = 5
```

即工单确实是冻结 artifact 上 `acceptance_probes[expected_before=="FAIL"]` 的纯投影，
Leader 未在其中掺入任何撰写内容。Stage 1 边界仍是冻结产物上的机械谓词。

## 3. 新事实：upstream/main 已漂移，但与本次写集不相交

读回 `upstream/main`（2026-08-24T06:33Z）：

| | commit | tree |
|---|---|---|
| 冻结 base | `e8cab7389f5dc1084fdc1cdf70bafd314d0a3d50` | `c81497cf7eb98f0568b8b30c9af1e67797bca27a` |
| 现 live head | `9fa6c52a4dd17e2de42a73fcccb7e3c1ad184c85` | `b5ce61dd358e615ecf013041ccfd2b1484d394f1` |

漂移内容（两个 commit，全部 docs）：

```
9fa6c52a docs(troubleshooting): broaden postgres volume permission guidance (#745)
18372961 docs(faq): add CLI namespace-not-found and PostgreSQL permission entries (#743)

document/docs/05-reference/faq.md
document/docs/05-reference/troubleshooting.md
document/i18n/en/docusaurus-plugin-content-docs/current/05-reference/faq.md
document/i18n/en/docusaurus-plugin-content-docs/current/05-reference/troubleshooting.md
```

机械判定：4 个漂移路径全部在 `document/` 下；`plan-gate-r1.json` 的 `write_set` 28 项
全部在 `server/**` 下；两集合**交集为空**。`work_partition` 单一 unit 的 13 个 planned
path_id 亦无一映射到 `document/`。

因此：**工程链的冻结 base 保持 `e8cab738`，不重设、不重跑 Discovery/Plan。** 这是路径不相交
的机械结论，不是「docs 改动应该无害」的直觉判断——若漂移曾触碰 `write_set` 任一项，此处
就该是 `INVARIANT_VIOLATION` 路由而不是本段。

边界说清楚，免得后续被当成「已对齐 live upstream」：本段只证明**工程期**冻结 base 无需变更。
Issue 正文要求「基于最新 upstream `main`」，该对齐属 **Publication 期**动作——Project
description 已规定从 live upstream base 重建 clean 公开分支并出三项 production-equivalence
证据。Publication 不在本工程链内，Leader 不执行、不预判其 rebase/force-push 路径选择。

## 4. 上下文预算：约束沿用，阈值数字不沿用

上一条分录的预算纪律**原样有效**——账本 4 个大文件合计 ≈77k tokens，禁止整目录 dump 源码，
用 `grep -n` / `sed -n` 定位后按需读，16179 字节的工单替代 97638 字节的冻结 artifact。

但有一点必须显式更正，否则 Coder 会盯错指标：`190400 threshold / 272000 limit` 是
**OmniRoute codex lane 的网关属性**，来自那次 `Proactive compression triggered` 日志。
`实现` 已换到 Claude lane，该具体数字在新 lane 上不成立。所以本次不给魔法数，只给：

- 目标仍是单 Run **<150k tokens**，不要接近任何压缩/截断边界；
- **必须回报本 Run 实测峰值上下文用量** —— Stage 2 的切点按实测定，不按 Leader 猜；
- 中途发现预算吃紧就 push 已完成部分并回报，不硬撑。

四次同族失败的直接诱因是「48 分钟单 Run + 191k 上下文 + 压缩后会话重放」。换 lane 消除了
那条已损坏的会话，但**没有消除**「单 Run 吃太多」这个诱因本身，所以纪律不放松。

## 5. Council lane 异构性：状态变了，记账，本轮不裁决

方法正本要求 HIGH_RISK 的 Council 具名 lens **优先分配不同 runtime lane**。lane 清单本轮发生
变化：`实现` 从 codex 迁到 Claude；`规划` / `验证` 仍在 `codex/gpt-5.6-sol`；本 Leader 亦在
Claude lane。

- 这对 Review 阶段的异构性**是净改善**，不是退化；
- 但 `plan-r1` 的 `council_nomination` 三位具名 lens 是在旧 lane 清单下提名的，届时
  `BLIND_BEHAVIOR_AUDIT` 与 `EVIDENCE_CHALLENGE` 两 Run 的 lane 分配需按**当时**的实际
  清单重新核对；
- Leader 只做机械校验（名额、锚点互异、含反对者、rationale 引具体 path/invariant），
  **不替 Planner 换人选**。异构不可用时不得自动进 READY，转 `SECURITY_REVIEW_BLOCKED`。

列为未解决项，不在本轮裁决。

## 6. Stage 1 派发（内容与 20260824-044027 逐字相同，仅换会话/lane）

- 起点：`origin/20260824-hd32-skillhub-735-subscription-authz-hardening` @ `4d75b999`，**全新会话**
- 工单：`leader/20260824-044027-stage1-red-workorder.json`（`876014da…`，16KB，21 条探针）
- 范围：仅写工单 `test_files_touched` 的 5 个测试文件；**不得改任何 `src/main/`**
- 验收：21 条在 base 上**真实按预期失败**，失败原因须为断言/契约不满足；编译错误或
  `ClassNotFoundException` **不算 RED**
- 禁止读取：`qa/discovery/semgrep-results.json`（126KB）；`plan-gate-r1.json` 不整体读；
  `behavior-inventory.v1.json` 本 Stage 用不到
- 工具链：`JAVA_HOME` 指向持久 JDK `21.0.12.1`，
  `MAVEN_OPTS=-Djansi.tmpdir=/workspaces/HD-32/tooling/jansi -Djansi.force=false`
- 交接：测试代码 + 分录 commit 并 push（各容器 `/workspaces` 互不可见）
- 不自签 PASS：交 exact commit / tree、clean status、真实命令与真实输出

`work_partition` 仍为单一 unit `subscription-authz-hardening`，13 个 planned path 不变，
顺序阶段化不构成重新切分（论证见 `20260824-044027-code-restage-dispatch.md` §2），
不需要 `plan-r2`。

## 7. 保持原状（本轮不动）

- `PRIVATE -> owner || manager` 与上游 reviewer FenjuFu `owner only` 的张力（intake §5）
- Publication force-push vs merge-main 路径选择（intake §6）

均待老板裁决。

## 交接

- 状态：IMPLEMENTING（Stage 1 of N，N 未定）
- 冻结输入：base `e8cab738` / tree `c81497cf`；inventory `aabf1b09`；plan `1c2b92a6`；
  artifact `bdfab21f`；工单 `876014da`；validator `0.1` / `a7100d86`
- 下一步：实现（Coder，Claude lane 新会话）执行 Stage 1 RED 冻结
- 未解决：Stage 2+ 切点（待 Stage 1 实测上下文）；POSITIVE 探针独立性（已转 QA 章程）；
  Council lane 分配需在 Review 期按当时 lane 清单重核；upstream live head `9fa6c52a`
  的对齐属 Publication 期；intake §5 / §6 待老板裁决
- 需要决策：无
