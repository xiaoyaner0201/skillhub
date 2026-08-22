# leader / 20260822-181000 / Coder 第 5 次失败：session 未换（leader 前提证伪）+ 退休后重派

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`97834f69-c490-4652-81f3-98f494a67ea8`（2026-08-22T18:06Z，dongsjoa 巡检）
- Stage：`PLAN_APPROVED_R2` 不变；RED intake 结论（`leader/20260822-174500`）不变
- 分录性质：leader 自身前提的更正 + 阻塞状态更新 + 重派登记，**不含任何 RED 受理结论**

## 一、leader 前一分录的一条前提被证伪，本分录更正

`leader/20260822-174500` §6 写道：「本轮以全新派单（非 retry / rerun）触发，预期落到新 session」。
**该前提不成立。** comment 触发的 task 默认即 `resume_session=true` + `reuse_workdir=true`，与
「新派单 / retry」的区分无关；migration `066` 的 `force_fresh_session` 是一个需被显式置位的列，
不因「新派单」自动为 TRUE。故 17:50 的派单照样接回 `01a02a28`。

该结论的原始依据（daemon.log）在 `multica-implementation` 容器，leader 不可读，属受理转述。
**但 leader 有一项本地可读的独立佐证，且强于转述**：

失败以评论 `50d656c3-6161-4eab-be28-a722baf56e08`（17:51:06）落在本 Issue 上，leader 直接读回其正文：

```
[400] The encrypted content for item rs_0ed0803b90eb758c016a89ce438c8c87d09c755bb8c188afa9
      could not be verified ... invalid_encrypted_content
```

该 item id 与 `leader/20260822-165500` 已登记的毒化项（session `01a02a28` rollout 第 762 行，
`payload.type=reasoning` / `id=rs_0ed0803b…`）**逐字相同**。全新 session 不可能携带同一条 reasoning
item id，故「同一 session 被 resume」由 leader 侧独立成立，不依赖 daemon.log。

附带更正一处巡检正文的措辞：其称「这次是新的 item id `rs_0ed0803b…`，因为提示词变了」。事实是
**同一个 item id 的重放**，非新项。该措辞偏差方向是低估其自身证据强度，不影响其结论。

## 二、Run 面读回（leader 独立读 `multica issue runs`）

| Run | started → completed | 结果 |
|---|---|---|
| `9c0ee793` | 17:50:55 → 17:51:06（11s） | failed，item `rs_0ed0803b…` |

本轮 Coder lane 累计第 **5** 次失败（`1afdebdd` / `1003b3c8` / `af4e0eeb` / `1f148717` / `9c0ee793`）。

## 三、交付面未被本次失败改变（leader 实读，非采信）

`git ls-remote origin`：

- `…-r3` head = `a71322ea58c55fa0dc51fe1f7c1d418708d93a01`（仍是 leader 20260822-174500 的账本 commit）
- rescue 分支 head = `f90d64ee44ad5ab9c6a3e1fba94bbe92287445bf`（未动）

无任何新 Coder commit，**不存在需再次抢救的产物**。`leader/20260822-174500` 的 RED intake 结论
（NOT ACCEPTED，三条理由）与 §6 scope 全部原样有效。

## 四、退休机制：受理，但附可证伪的预置判据

巡检报 daemon 对 `api_invalid_request` 会主动退休 session，17:51:05 已对 `01a02a28` 写下退休；
先例为 15:36:47 退休 `01a0248f` → 15:47:54 的 `1003b3c8` 开出全新 `01a02a28`。该先例与
`leader/20260822-165500` 表中的时间戳自洽。

leader 不能读 daemon.log，故不宣称已验证退休。改为**预置下一轮的判据，使下一次失败无需再做一轮诊断**：

| 下一 Coder Run 的表现 | 判定 | 动作 |
|---|---|---|
| ~20s 内失败，item 仍为 `rs_0ed0803b…` | 退休**未生效** | 停止重派，升级老板裁决换 Runtime / 换 provider |
| 失败但 item id **不同** | 新 session 亦被毒化，属**另一故障面** | 重新定位，不得套用本轮归因 |
| 越过 ~2min 仍在跑 | session 确已更换 | 按正常 lane 等待交回，走 §六 intake |

第一行才是巡检保留的升级条件；在此之前按其判断归类为可自解故障，leader 不上抛、不改 Runtime、
不重启容器、不删毒化 rollout。

## 五、重派：scope 原样，不作任何放宽

按 `leader/20260822-174500` §6 六条原样重派 Coder `fd2d1252`，交付面仍为「提交面重做 + 实跑留痕」，
**测试内容沿用 `f90d64ee` 已验证的那份，不重写**。写集仍以 `61d54ad3…` 的 24 条为界。

`green_dispatch_gate` 保持关闭，三条重开条件不变：对 `…-r3` 两次 `merge-base --is-ancestor` exit 0；
`git diff <RED_BASE> <RED_DELTA>` 非空且全部落在 write_set 内；分支上存在新的
`coder/20260822-*-red-output.log`。加绑条件 2 的唯一受理证据、Plan Gate artifact `61d54ad3…`、
R1–R5 与 A/B 边界全部不变。Publication 边界不变。
