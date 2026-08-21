---
ts: 20260821-103525
agent: leader
action: independent digest corroboration of M3b delivery / bind effective F3 draft by digest
tree: e786a5e3e2cdd25f43e373f0f25200c9df5ea168
verdict: PASS
---

规划 于 `20260821-103440` 报交 M3b。plan gate 已于 `20260821-103056` 判 **PASS** 并派出 Code，
本分录**不重判**，只做两件事：登记独立取证结果，以及把生效 F3 草稿**按 digest 绑死**。

## 1. 逐位复算（不采信自述）

| 文件 | 自述 SHA-256 | 本席复算 | 判定 |
|---|---|---|---|
| `planner/20260821-102507-plan-gate-m3b.json` | `ed9f4860…c4077` | 同 | **逐位一致** |
| `planner/20260821-102507-plan-r1-m3b-scope-ruling.md` | `1ed03af6…8f659` | 同 | **逐位一致** |
| `planner/20260821-102507-pr-body-draft-m3b.md` | `0c37a557…a58198` | 同 | **逐位一致** |

artifact digest 与本席 `20260821-103056` 收据 `inputs[0].sha256` 亦逐位一致——
即本席独立跑出的 gate 与 规划 本地跑的是**同一份 artifact**，不是两份内容相近的文件。

另核：`6f5b4f2b` 与 `3b42b8ff` 均带 `Signed-off-by: xiaoyaner-multica-planner[bot]`；
非 ledger diff vs `bfcb4fe5` **为空**；candidate business tree 仍为 `e8aca2a4…`。
**M3b 是纯 ledger 推送，未动业务树。**

## 2. 生效 F3 草稿的 digest 绑定（把 `20260821-103056` §4 的提示升级为可校验项）

`20260821-103056` §4 已登记「账本里有两份 F3 草稿，只有一份可用」。
该提示当时**只按文件名**成立，人若取错文件名之外无从校验。现补 digest：

| F3 草稿 | SHA-256 | 状态 |
|---|---|---|
| `planner/20260821-102507-pr-body-draft-m3b.md` | `0c37a557209aa2a40107daea4ed4891824e5815ab64935e14bb7f71958a58198` | **唯一生效** |
| `planner/20260821-101027-pr-body-draft-m3.md` | 不绑定 | **已 supersede，禁止用于发布**（公开段含 R1 的入口、类名、字段清单与 `visibility: "PRIVATE"` 例证） |

Publication 侧取用前须复算 digest 并与上表比对，**不得凭文件名或「找到一份 pr-body-draft」判断**。
R1 披露通道未定，误取 supersede 版即完成一次不可逆公开披露。
本项列为 Review Gate 与 Publication 的必查项。

同理，首轮 M3 artifact `0242d643…d3eb` 为审批到达前的历史记录，
**后续任何 Gate 与 Publication 均不得消费**；生效绑定是 `ed9f4860…c4077`。
规划 这一自我限定正确，本席据实登记。

## 3. 未变的项

- Plan 段仍闭合，Code 在途，本分录不改变任何 Stage 状态；
- exact tree `e8aca2a4…` 在 实现 落地前继续冻结；
- 落地后 Implementation Gate 与 Review Pass 1 自动作废并重跑，QA 全段相对新树重跑，
  QA execution 须与 discovery Run `f7cf1b2d…` 不同 Run；
- R6/R9/R10/R11 四条 LOW 归属仍未裁（见 `20260821-102535` §2），不阻塞。

## 交接

- 状态：M3b 交付经独立复算确认；plan gate PASS 不变；等 实现 的 RED→GREEN→regression
- 产物：本分录
- 工作分支：`20260819-skillhub-731-confirm-publish-event-r2`
- 下一步：实现 落地 → 本席跑 Implementation Gate → QA Charter/Execution → Pass 1 重跑 + Pass 2 → Review Gate → READY
- 未解决：四条 LOW 归属（不阻塞）；R1 披露通道待老板裁定（已移出本链）
- 需要决策：否
