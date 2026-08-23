---
ts: 20260823-135048
agent: coder
action: EVIDENCE
tree: 57a240252dabc45b9f4dcc4cfa3500b5eca0e205
verdict: —
---

补充分录，**追加而非改写** `20260823-134500-tdd-red.md`。该分录 §四 末条记「12:57 与 13:01
两份上一轮遗留 log 留在磁盘上未提交」。本分录把这两份也入库，理由与边界如下。

**主题树不变**：本次只新增 `multica-ledger/` 下的文件，业务面零改动。

```
git diff --stat 45b723ce HEAD -- . ':(exclude)multica-ledger/**'   ->  空
```

候选 exact tree 仍为 `57a240252dabc45b9f4dcc4cfa3500b5eca0e205`（commit `45b723ce`），
不因本次账本追加而变化。

## 入库的两份

| 文件 | 产出 lane | 内容 |
|---|---|---|
| `coder/20260823-125746-red-output.log` | codex（上一 session） | 单行 `exit 126` —— `mvnw` 在 `noexec` 的 `$HOME` 上 `Permission denied` |
| `coder/20260823-130134-red-output.log` | codex（上一 session） | 189 行，`skillhub-domain` 30/30 绿，`RbacServiceTest` 1 跑 1 失败，reactor 停在 `skillhub-auth`，`BUILD FAILURE` |

两份均以 `git add -f` 绕过 `.gitignore:19`（`*.log`），与本轮三份同一处置。
**原文逐字节入库，未编辑、未截断、未重排。**

## 为什么值得留

1. `125746` 是 `noexec` 阻塞的**独立复现**——它与本轮 `132753` 出自不同 lane、不同
   session，却是同一个 exit 126。单次失败可以算偶然，两次跨 lane 的同因失败是环境事实，
   后续任何在本容器跑 `mvnw` 的 run 都应直接照 `MAVEN_USER_HOME` 处置，不必再撞一次。
2. `130134` 是**上一轮 codex lane 确实跑出过正确 RED** 的唯一在库证据：
   `RbacServiceTest…:50 [RbacService must expose the single-source batch role lookup]`
   在那一轮就已按预期失败。那次 session 是死在 stream 上，不是死在 RED 上。
   这条区分对 lane 归因有用——把第 11 次派单的失败整体记成「Coder 未完成」并不准确。
3. 巡检 §3 记录了宿主侧抢救副本 `~/.hermes/workspace/hd31-rescue-20260823/`。副本在
   容器外、不可寻址、不随 ref 走。入库后该暴露面关闭，证据回到可寻址 ref 上。

## 边界

- 未改断言、未改生产代码、未改 `20260823-134500-tdd-red.md` 一字。
- 未复用、未覆盖 r1 的三份旧 log；本轮三份 `132753 / 132947 / 133600` 仍是新文件。
- 这两份**不计入本轮 RED 结论**。RED 判据仍只取 §三 的 Run A/B/C。
- 未 push 公开分支、未碰 PR #735、未转 Ready、未 force-push、未改 issue 状态。
