# leader / 20260823-101500 / 自陈：`no_alternate_tag` 的取证范围不足——上游有**第二个 registry（GHCR）**；实测后结论维持且更强

- Agent：leader（调度，`96798779-3720-43f7-9ee1-a8389ad7ab0e`）
- 触发评论：`e5396e04-b25e-4493-b48a-e218b4efd561`（2026-08-23T10:05Z，dongsjoa 巡检）
- Stage：`PLAN_APPROVED_R2` 不变；Issue 保持 `blocked`；**不重派、不重试、不改配置、不碰镜像/compose/容器**
- 分录性质：**leader 自陈取证范围更正** + 补齐取证 + 否定结论范围收紧后重新成立
- **不含任何 RED 受理结论**；`20260822-174500` 的裁定一字不动

## 一、先记一条对我自己的更正：`20260823-095500` 的取证范围**不足以支撑其措辞**

`20260823-095500` §二把结论写成「**全 registry 范围**内没有任何 tag 含该修复」，
但当时**只查了 Docker Hub 一个 registry**。本轮读 `docker-publish.yml`（`FETCH_HEAD` = `0b41259f`）发现：

```
125:    env:
126:      IMAGE_NAME: diegosouzapw/omniroute
127:      GHCR_IMAGE_NAME: ghcr.io/diegosouzapw/omniroute
...
159:          outputs: type=image,push-by-digest=true,name-canonical=true,push=true
161:            ${{ env.IMAGE_NAME }}
162:            ${{ env.GHCR_IMAGE_NAME }}
```

且 manifest 作业里 **`Create Docker Hub manifest` 与 `Create GHCR manifest` 是两个并列步骤**，
产出同一套 tag（`${VERSION}${suffix}`）。即**上游有两个发布目标，我上一轮只验了其中一个**。
措辞越出了证据。此条**更正 `095500` §二的范围表述**（其 Docker Hub 部分的数据本身不变）。

## 二、补齐取证：GHCR 实测，与 Docker Hub **逐字节同一状态**

匿名取 GHCR pull token（该 package 公开可读），`GET /v2/.../tags/list?n=1000`：**135 个 tag**，
其中非数字（浮动）tag 恰为 `latest` / `latest-web` / `main` / `main-web` / `next` / `next-web`。

对四个浮动 tag 取 `Docker-Content-Digest`，与 `095500` 记录的 Docker Hub digest 并列：

| tag | GHCR 实测 digest | Docker Hub（`095500` 记录） | 是否同一 |
|---|---|---|---|
| `next` | `sha256:6f95064bf0981c16a18aa9beee08bc3d9741c9e130154c79a785562b0ab7b0e2` | `sha256:6f95064b…` | **同一** |
| `next-web` | `sha256:53b69fae84a87a1862765e02018c9e2436547da2c94219cf9f8359b5319f7059` | `sha256:53b69fae…` | **同一** |
| `main` | `sha256:a30676da5a18839494674f9f5e764f0a1c820bfdda01521d53e94d986d16ebb2` | `sha256:a30676da…` | **同一** |
| `latest` | `sha256:92c768c56e2de32c51a0621ef182835018b00b288c9bb235c5c5e4514658c1a1` | `sha256:92c768c5…` | **同一** |

**digest 相等比时间戳相等更强**：它证明从 GHCR 拉到的就是同一份镜像内容，而不只是"发布时间接近"。
故 GHCR 同样冻结在 08-22T23:37 那一批，**同样不含 `e73ab004`**。

**⇒ 「改从 GHCR 拉」这条路一并关闭。** 结论从「Docker Hub 无可替代 tag」收紧措辞、
并扩展到两个 registry 后**重新成立，且证据强于上一轮**。

## 三、两条附带确认

1. **`-bun` / `-web-bun` 在 GHCR 同样不存在。** `095500` §3.1 从 Docker Hub 侧推出"BUN 发布面
   自引入起产出为零"，本轮在**第二个独立 registry** 上得到同一结果，该佐证不再是单一来源。
2. **`docker-publish.yml` 是全仓库唯一能推镜像的工作流。** 遍历 `FETCH_HEAD:.github/workflows/`
   下全部文件，含 `build-push-action` / `imagetools create` / `docker push` / `ghcr.io` 的**只有它一个**。
   故不存在"别的流水线还在正常发布"的可能，`095500` §二的封闭性至此完整。

## 四、对巡检本轮事实的复核与同意

- `release/v3.8.50` head：leader 匿名 fetch 实测 = **`0b41259f`**（`fix(search): fall back to
  duckduckgo-free…` #11097），与巡检 10:01:11Z 读数一致；
- publish 工作流 10/10 failure 的 run 明细在 Actions API，leader 无凭据，**不宣称已验**；
  但其结论与本轮两条独立证据相容——两个 registry 同时冻结在同一批 digest，正是
  "`Build and push platform image by digest` 失败 ⇒ 两个 registry 都拿不到 digest ⇒ manifest 作业 skipped"
  的必然结果。**巡检的因果链与 registry 侧观测自洽。**
- 检查点判据不变，leader 同意维持。

## 五、裁决面：不变

选项集与 `20260823-095500` §五完全一致，本轮无新增可交付信息，**leader 不重复升级、不代决**。
待裁决项仍是 lane 路由（全量切 → 异构塌缩 → `SECURITY_REVIEW_BLOCKED`；只切 `实现` → 异构安全但
QA 段会原样再撞）vs 本地网关补丁；脏会话弃用是共同必要前置。

## 六、动作边界与不变项

只读：匿名 fetch 上游 `release/v3.8.50`、读 workflow 文件；匿名取 GHCR pull token 并读 tag 列表与
manifest header。**无任何写操作**——未 push 镜像、未 pull 镜像、未改 compose、未重启容器、
未改任何连接/agent 配置、未重派、未重试、未动 Gate、未签异构豁免、未代决路由、
未去上游开 issue、未 @ 任何人。

`green_dispatch_gate` CLOSED（(a)(b)(c) 一字不改）、`red_intake` NOT ACCEPTED 三条理由、
`write_set_precheck` CLEAN、加绑条件 2 的唯一受理证据（addendum `3812652e`）、
Plan Gate artifact `61d54ad3…`、R1–R5 与 A/B 硬约束、Publication 边界，全部不动。
9 次失败仍归 runtime/infra，不计入 Coder、不进返修链。
