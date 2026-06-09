# 三端隔离 P0/P1 快速推进记录：gpt-5.4 子 Agent 与 Maven Reactor 漂移

时间：2026-06-09 00:23，本机 `Asia/Shanghai`。

## 参考方向

- 主参考：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 执行边界：当前只修 P0/P1，即编译、guard、接口、权限、串端、service/字段缺失。
- 跳过项：不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

## 子 Agent 使用记录

- 用户最新指定：子 Agent 使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 本轮实际模型：`gpt-5.4`。
- 本轮数量：6 个。
- 状态：全部关闭。
- 覆盖切片：
  - seller/buyer 后端账号、角色、菜单、部门、日志、会话隔离。
  - portal auth、direct-login、session、日志。
  - React admin/portal route、service、request、token guard。
  - SQL seed、DDL/DML guard、菜单 seed guard。
  - product/inventory/integration/warehouse 共享业务域。
  - verifier、manifest、AGENTS/docs drift。

## 采纳的 P1

- `seller` 模块窄测试复现 `NoSuchMethodError`：
  - 命令：`mvn -pl seller "-Dtest=SellerPortalProductServiceImplTest" test`。
  - 失败点：`SellerPortalProductServiceImpl` 调用 `IProductDistributionService.selectProductById(Long, Long)` 和 `selectSkuList(Long, Long)`。
  - 当前源码已经存在这些签名，实际根因是本机 `.m2` 中 `product-3.9.2.jar` 落后；`mvn -pl seller` 不带 `-am` 时不会把当前 reactor 的 `product` 源码产物纳入运行时 classpath。
  - 风险：卖家 portal 商品范围契约会被本机旧依赖产物打断，容易把真实 P1 误判为业务源码问题，或让子 Agent/人工窄验证给出不稳定结论。

## 已完成

- 使用 reactor 方式刷新上游模块本机产物：
  - `mvn -pl product -am -DskipTests install`。
  - 构建并安装 `ruoyi-common`、`ruoyi-system`、`finance`、`inventory`、`integration`、`warehouse`、`product`。
- 复跑 seller 目标测试，通过后确认 P1 根因是 Maven 本机依赖漂移，不是 seller 业务代码缺签名。
- 更新 `AGENTS.md`：
  - 明确运行 seller/buyer/product 等跨模块窄测试必须带 `-am`。
  - 明确不要只跑 `mvn -pl seller ...` 或 `mvn -pl buyer ...` 后认定通过。
- 本轮未修改 seller/product 业务代码。

## 记录但不阻塞的 P2

- direct-login portal 弹窗自身失败超时为 5 秒，管理端 opener 等待 15 秒，超时时长不一致；当前仍会由 opener 超时收敛，后续可统一。
- portal 页面仍有少量 `console.log(error)` 和英文失败文案；属于 UI/体验细调，不阻塞当前 P0/P1。
- SQL guard 当前明确覆盖动态 DDL，高影响动态 DML 识别可增强；本轮扫描未发现现有动态 DML。
- React `.js` sidecar guard 依赖 manifest/脚本白名单，不是全仓自动发现；当前关键入口已覆盖。
- `selectOwnProductList()` 存在逐条补 SKU 的 N+1 倾向；当前不影响权限或串端判断。
- `warehouse.ts` 的 `withRuoYiPage()` 只兜底 `current -> pageNum`，未统一规范 `pageSize`；当前关键调用方已显式传 `pageSize`。

## 数据源和远端影响

- 本轮没有执行 DDL。
- 本轮没有执行 DML。
- 本轮没有读取或写入远端 MySQL。
- 本轮没有读取或写入远端 Redis。
- 本轮没有启动或重启后端服务。
- 本轮只更新本机 Maven `.m2` 缓存和 Maven `target` 产物。

## 验证结果

- `mvn -pl seller "-Dtest=SellerPortalProductServiceImplTest" test`
  - 首次复现失败：8 个测试中 6 个 `NoSuchMethodError`。
- `mvn -pl product -DskipTests install`
  - 失败：`inventory:3.9.2` 本机依赖缺失，证明单模块安装同样受本机依赖缓存影响。
- `mvn -pl product -am -DskipTests install`
  - 通过。
  - Reactor 成功安装 `ruoyi-common`、`ruoyi-system`、`finance`、`inventory`、`integration`、`warehouse`、`product`。
- `mvn -pl seller "-Dtest=SellerPortalProductServiceImplTest" test`
  - 通过，8 个测试通过。

## CodeGraph 追补

- 2026-06-09 后续记录层复核发现：本文件记录过 `AGENTS.md` 规则更新，但当时未在本文件中写入 CodeGraph 结果。
- 追补口径：本轮已在仓库根目录执行 `codegraph sync .`，同步结果以 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 和 `docs/plans/2026-06-09-three-terminal-p0p1-gpt54-fast-pass-record.md` 最新检查点为准。

## 当前判断

- 当前源码层面未发现新的 seller/buyer 账号权限串端 P0/P1。
- 三端完整 verifier 已经用 `-am`，不是本次漂移的来源。
- 后续人工或子 Agent 做后端窄测试时，必须把 `-am` 作为默认纪律；否则容易重新被旧 `.m2` 产物误导。
