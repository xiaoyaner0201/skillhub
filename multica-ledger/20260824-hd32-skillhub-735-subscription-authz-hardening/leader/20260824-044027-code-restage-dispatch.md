---
ts: 20260824-044027
agent: leader
action: Code 阶段重编排（三次 Run 故障后）+ dispatch Stage 1（RED 冻结）
tree: c81497cf7eb98f0568b8b30c9af1e67797bca27a
verdict: —
---

# Code 阶段重编排

触发：巡检（千乘妍）报三次 Code Run 连续失败（`90ac4666` / `0b7e17af` / `20738bc3`），
根因为 provider 侧会话重放——首个 Run 上下文达 191k > 190400 主动压缩阈值，压缩丢弃了某次
custom tool call 的输出，上游返回 `400: No tool output found for custom tool call
call_2tQWOCj9G1K6rGXPZvfUEnFV`；第 2、3 次重试复用同一条已损坏会话。与 02:44 的
`403 Model not allowed` 属不同故障族。

## 1. 分支未被污染（Leader 独立复算，非采信自述）

| 检查 | 结果 |
|---|---|
| `origin/<branch>` head | `8c7affc77f0bd41ef97ef77b9a1a51446d5d450a`（= Plan Gate 分录） |
| `git log 8c7affc7..origin/<branch>` | 空 |
| `git diff --name-only e8cab738 origin/<branch> \| grep -v '^multica-ledger/'` | 空 |

三次失败 Run 的 `output_bytes=0` / `tools=0` 属实。**无需回滚**，Stage 从 `8c7affc7` 起。

## 2. 顺序执行不是重新切分——`work_partition` 保持冻结

巡检要求「按 TDD 阶段切成多个 Run」。需要先判定这是否触碰冻结计划。

`plan-r1` 的 `no_split_rationale` 逐字理由是「splitting would create **conflicting writes**
and unverifiable partial policy states」。方法正本中 `work_partition` 的判据全部是**并行**判据
（「两个无依赖单元的 `write_set` 相交 → RETURN：写集冲突不可并行」「把无依赖单元派给不同
Coder Run」）。即：`no_split_rationale` 反对的是**把同一写集拆给并行单元**。

**顺序阶段化不产生并行写集冲突**：各 Stage 串行、后一 Stage 从前一 Stage 的 head 起，
任一时刻只有一个 Run 持有写权。因此：

- `work_partition` 仍是单一 unit `subscription-authz-hardening`，13 个 planned path 不变；
- 不需要 `plan-r2`，冻结 artifact 保持真实（与巡检「不需要重跑 Discovery 或 Plan」一致）；
- 「unverifiable partial policy states」另一半由方法正本自身覆盖：**QA 与 Review 按整体
  exact tree 进行，不按单元拆**——中间 tree 从不提交验收，不存在「验收了半个策略」的风险。

结论：**Planner 定切分，Leader 定编排**。本次只动编排，不动切分。Leader 未改冻结计划一字。

## 3. 单 Run 上下文预算：先算账本自身的开销

巡检要求单 Run <150k。Leader 实测本账本的字节成本——若 Coder 按直觉「先读一遍账本」：

| 文件 | 字节 | 处置 |
|---|---|---|
| `qa/discovery/semgrep-results.json` | 126630 | **禁止读取**。Discovery 中间产物，结论已固化进 inventory |
| `planner/20260824-040000-plan-gate-r1.json` | 97638 | **禁止整体读取**，按需 `python3 -c` 取切片 |
| `qa/behavior-inventory.v1.json` | 31120 | Stage 1 不需要。14 candidate 已由 Plan 全部 disposition |
| `planner/20260824-040000-plan-r1.md` | 20368 | 可读 |

合计 275756 字节 ≈ 77k tokens——**在碰到任何一行源码之前就吃掉半个预算**。这足以解释
191k 是怎么攒出来的。故本次不只是「叮嘱省着点」，而是给出可执行替代物（§4）。

## 4. 冻结产物的机械投影：Stage 1 工单

`leader/20260824-044027-stage1-red-workorder.json`
sha256 `876014dae72a723775e2b384f3c9aa3905ce55dcc313bae4bf3cdc528968fc86`，16179 字节。

生成方式是**纯机械过滤**，无 Leader 撰写的内容：

```
acceptance_probes[ expected_before == "FAIL" ]
```

产出 21 条 RED 探针（与 Plan Gate §F 复算的 21 条一致），落在 **5 个测试文件**：

```
SkillSubscriptionServiceTest      SubscriptionMessageBundleTest
SubscriberAccessResolverTest      SubscriberNotificationSinkTest
MySkillAppServiceTest
```

工单内含每条探针的 `probe_id / path_id / class / entry / sink_assertions / command /
expected_before / expected_after`。**16179 替代 97638 字节，Stage 1 无需打开冻结 artifact。**

选 `expected_before == FAIL` 作为 Stage 1 边界，是因为它是**冻结产物上的机械谓词**，
不是 Leader 对「哪些先做」的直觉判断——符合「不按直觉拆任务」。

## 5. Stage 1 派发（RED 冻结，只此一个 Stage）

- 起点：`origin/20260824-hd32-skillhub-735-subscription-authz-hardening` @ `8c7affc7`，**新开会话**
- 范围：仅写 §4 的 5 个测试文件；**不得改任何 `src/main/`**
- 验收：21 条探针在 base 上**真实按预期失败**，且失败原因为断言/契约不满足，
  而非编译错误或 `ClassNotFoundException`——「没编译过」不算 RED
- 工具链：`JAVA_HOME` 指向持久 JDK `21.0.12.1`，
  `MAVEN_OPTS=-Djansi.tmpdir=/workspaces/HD-32/tooling/jansi -Djansi.force=false`
- 交接：测试代码 + 分录 commit 并 push 到交付分支（各容器 `/workspaces` 互不可见）
- **必须回报峰值上下文用量**——Stage 2 的边界按实测定，不按 Leader 猜测

**只派 Stage 1**。后续 Stage 的切点若需要判断「策略在此处是否自洽」，那是语义判断，
届时由 Planner 出 `work_partition`-only 的 `plan-r2`，Leader 不代劳。

## 6. 保持原状（本轮不动）

- `PRIVATE -> owner || manager` 与上游 reviewer FenjuFu `owner only` 的张力（intake §5）
- Publication force-push vs merge-main 路径选择（intake §6）

均待老板裁决，巡检已确认本轮不动。

## 交接

- 状态：IMPLEMENTING（Stage 1 of N，N 未定）
- 产物：`leader/20260824-044027-stage1-red-workorder.json`（`876014da…`）
- 下一步：实现（Coder）新会话执行 Stage 1 RED 冻结
- 未解决：Stage 2+ 切点（待 Stage 1 实测上下文后定）；POSITIVE 探针独立性（已转 QA 章程）；
  intake §5 / §6 两项待老板裁决
- 需要决策：无
