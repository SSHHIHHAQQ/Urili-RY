# 2026-06-07 三端 P0/P1 快速推进：审计合同、配对删除作用域与仓库 Admin 路由

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。

本轮不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 先按用户最新要求尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；平台返回用量限制，因此没有可用结果。
- 回退使用 6 个 `gpt-5.4` 子 Agent 做只读扫描。
- 6 个 fallback 子 Agent 已关闭。
- 采纳的 P1：
  - `docs/architecture/reuse-ledger.md` 顶部仍有“新增 critical integration 合同测试改脚本本体”的旧指引，需改为 manifest。
  - 管理端 audit 查询已有后端能力，但缺 `operLog` 正向账号级查询测试和 admin 审计参数绑定合同。
  - `seller_buyer_management_seed.sql` 的 fresh bootstrap `portal_direct_login_ticket` DDL 缺少同级合同。
  - integration 仓库/物流渠道/SKU 配对删除使用裸 pairingId，缺 `connectionCode` 作用域。
  - 仓库管理端 controller 暴露在裸 `/warehouse`，应收敛到 `/warehouse/admin` 管理端命名空间。

## 已完成

- `SellerServiceImplTest` / `BuyerServiceImplTest` 补充 `select*OperLogListUsesExplicitSubjectAndAccountScope`，固定管理端操作日志账号级查询必须带主体和账号双作用域。
- 新增 `PortalAdminAuditBindingContractTest`，固定 seller/buyer 管理端 `loginLogs`、`operLogs`、`directLoginTickets` 直接绑定请求模型，并原样传给 service，避免丢失 `subjectId/accountId` 过滤。
- `PortalDirectLoginTicketSqlContractTest` 补充 fresh bootstrap seed 合同，固定 `portal_direct_login_ticket` 在综合 seed 中也必须包含审计字段和关键索引。
- integration 配对删除接口改为 `/{connectionCode}/warehouse-pairings/{id}`、`/{connectionCode}/logistics-channel-pairings/{id}`、`/{connectionCode}/sku-pairings/{id}`。
- integration service、mapper 和 XML 删除 SQL 同步加入 `connectionCode + pairingId` 双作用域；SKU 删除前也按 `connectionCode + skuPairingId` 查询当前配对。
- 官方仓手工解绑/换绑调用 `deleteWarehousePairing(...)` 时同步传入已保存的连接编号。
- React integration service 与页面调用同步传入 `connectionCode`；TS 和 JS 镜像都已更新。
- `AdminWarehouseController` 改为 `/warehouse/admin`；React warehouse service TS/JS base URL 改为 `/api/warehouse/admin`。
- 新增 `WarehouseAdminRouteContractTest`，固定仓库管理端后端路由和前端 service 不得回退到裸 `/warehouse`。
- `three-terminal.manifest.json` 加入 `PortalAdminAuditBindingContractTest` 和 `WarehouseAdminRouteContractTest`；后者同时加入 `criticalBackendExplicitTestClasses`。
- `docs/architecture/reuse-ledger.md` 更新 manifest 指引、integration 配对删除作用域规则和 warehouse admin route 规则。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,integration,seller,buyer,warehouse -am "-Dtest=PortalAdminAuditBindingContractTest,PortalDirectLoginTicketSqlContractTest,WarehouseAdminRouteContractTest,IntegrationAdminPermissionContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 前端 7 个 Jest suite / 33 个测试通过。
  - 后端 `ruoyi-system` 151、`ruoyi-framework` 15、`integration` 5、`product` 5、`seller` 94、`buyer` 95 个测试通过。
  - `ruoyi-admin -am -DskipTests test-compile` reactor 编译通过。
  - Jest 结束后仍有既有 open handle 提示，但测试结果为通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" ...`：无输出，本轮触碰文件未发现尾随空白。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 29 个变更文件，Added 11、Modified 18，共 1,623 个节点。

## 边界

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 后续

- `ProductDistributionMapper` 跨模块表访问仍需按来源快照、SKU pairing 投影和事实归属方案继续收口。
- 商品库存聚合字段仍需库存事实源与汇总规则设计确认后再接通。
