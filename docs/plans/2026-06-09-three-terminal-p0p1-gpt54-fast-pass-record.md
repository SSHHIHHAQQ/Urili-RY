# 三端隔离 P0/P1 快速推进记录

时间：2026-06-09 00:08，本机 `Asia/Shanghai`。

## 参考方向

- 主参考：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 执行边界：只处理 P0/P1，即编译、guard、接口、权限、串端、service/字段缺失。
- 跳过项：不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

## 本轮目标

1. 固化子 Agent 默认模型为 `gpt-5.4`，不再优先使用 GPT-5.3 Codex。
2. 合并子 Agent 可坐实的 P1 发现。
3. 只做代码级和契约级验证，保持三端独立方向不回退。

## 子 Agent 使用记录

- 实际模型：`gpt-5.4`。
- 数量：6 个。
- 状态：全部关闭。
- 已采纳 P1：
  - 上游系统管理页连接列表前端权限应使用 `integration:upstream:list`，不能用详情权限 `integration:upstream:query`。
  - 官方仓同步创建不能硬编码履约仓配对，应从接入结算类型推导 `FULFILLMENT` 或 `QUOTE`。
  - 商品上下架、批量上下架必须携带原因并写入 `product_distribution_operation_log.reason`。
  - 三端 verifier manifest 的关键测试缺失必须用真实负例验证，而不能只检查脚本文本。
- 记录但不阻塞的 P2：
  - 端内角色查询接口可补更细的编辑权限可见性。
  - 端账号用户名唯一性当前偏全局，后续可按主体维度评估。
  - portal home session 加载失败仍偏 console 输出。
  - 免密弹窗 opener/portal 超时时长不完全一致。

## 本轮代码变更

- `AGENTS.md`
  - 子 Agent 默认模型改为 `gpt-5.4`。
  - 明确快速推进模式下只修 P0/P1。
  - 代码审查交付清单补充数据源、SQL、三端隔离和子 Agent 记录项。

- 上游系统管理前端权限
  - `react-ui/src/pages/UpstreamSystem/index.tsx`
    - 连接列表加载权限从 `integration:upstream:query` 改为 `integration:upstream:list`。
  - `react-ui/tests/upstream-system-permission-guard.test.ts`
    - 固定 `integration:upstream:list` 权限。
    - 补齐 UpstreamSystem `.js` sidecar 纯 re-export 覆盖。

- 官方仓同步配对角色
  - `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java`
    - 官方仓同步连接列表改为支持履约结算和自营应收两类接入。
    - 同步候选和自动配对从接入 `settlementType` 推导 `pairingRole`。
    - 自营应收接入自动配对为 `QUOTE`，上游应付接入自动配对为 `FULFILLMENT`。
  - `react-ui/src/pages/Warehouse/components/OfficialSyncModal.tsx`
    - 同步弹窗文案改为中性“上游仓接入 / 上游仓库”。
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/WarehouseAdminRouteContractTest.java`
    - 增加契约，防止官方仓同步再次硬编码履约配对。

- 商品销售状态原因
  - `ProductStatusUpdateRequest` / `ProductBatchStatusUpdateRequest`
    - 新增 `reason` 字段。
  - `IProductDistributionService` / `AdminProductDistributionController` / `ProductDistributionServiceImpl`
    - 上下架和批量上下架接口透传 `reason`。
    - `ON_SALE` / `OFF_SALE` 要求 `reason` 非空。
    - SPU/SKU 销售状态日志写入 `reason`。
  - `react-ui/src/pages/Product/Distribution/index.tsx`
    - 上下架弹窗要求填写状态调整原因。
  - `react-ui/src/services/product/distributionProduct.ts`
    - 状态接口请求体携带 `reason`。
  - `ProductModuleBoundaryContractTest` / `product-distribution-permission-guard.test.ts`
    - 固定请求、service、前端调用和日志原因契约。

- verifier manifest 负例
  - `react-ui/tests/verify-three-terminal-backend-gate.test.ts`
    - 新增关键后端测试从 manifest 删除时的真实失败用例。
    - 新增关键前端测试从 manifest 删除时的真实失败用例。

## 数据源和远端影响

- 本轮没有执行 DDL。
- 本轮没有执行 DML。
- 本轮没有读取或写入远端 MySQL。
- 本轮没有读取或写入远端 Redis。
- 本轮没有启动或重启后端服务。

## 验证结果

- `npm exec -- jest --config jest.config.ts tests/upstream-system-permission-guard.test.ts tests/verify-three-terminal-backend-gate.test.ts tests/product-distribution-permission-guard.test.ts --runInBand`
  - 3 个测试套件通过，24 个测试通过。
- `mvn -pl "ruoyi-system,product,seller,buyer,warehouse" -am "-Dtest=WarehouseAdminRouteContractTest,ProductModuleBoundaryContractTest,ProductCenterServiceImplTest,SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Reactor 通过。
  - 目标测试通过：仓库契约、商品模块契约、商品中心、卖家 portal 商品、买家 portal 商品。
- `npm run verify:three-terminal`
  - portal token guard、partner guard、seller/buyer portal product guard、product upstream mirror guard 通过。
  - React typecheck 通过。
  - 前端 21 个测试套件通过，160 个测试通过。
  - 后端 reactor test-compile 通过。
  - 后端三端契约通过。
  - 总结果：`three-terminal verification passed`。
- `codegraph sync .`
  - 执行成功。
  - 输出显示同步 2 个 changed files。

## 当前判断

- 本轮没有引入新的账号体系混用。
- 管理端仍使用若依权限体系。
- 本轮变更没有新增 seller/buyer 端内账号、角色、菜单或部门表。
- 官方仓同步的配对角色现在由接入事实推导，不再把自营应收接入误写成履约配对。
- 商品销售状态审计现在能记录“为什么上下架”，不再只有“谁改了什么”。

## 下一步建议

1. 继续按 P0/P1 推进剩余 guard/API/权限/service 缺口。
2. P2 只记录，不阻塞当前快速推进。
3. 后续如要动 SQL 或远端数据，仍按 AGENTS 先写 Markdown 方案并确认。

## 2026-06-09 02:08 SQL Seed 完成态 Guard 收敛

### 参考方向

- 主参考：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 执行边界：继续只修 P0/P1，不做浏览器运行态验收、截图、DOM 检测或 UI 细调。
- 子 Agent 模型：按用户最新要求，全部使用 `gpt-5.4`，不再把 GPT-5.3 Codex 作为首选。

### 子 Agent 使用记录

- 实际模型：`gpt-5.4`。
- 数量：6 个。
- 状态：全部关闭；一次重复关闭同一已关闭 Agent 返回 not found，不代表仍有 Agent 未关闭。
- 采纳结论：
  - `20260604_product_category_attribute_seed.sql` 缺事务、缺写后完成态断言、按钮只 insert missing。
  - `20260605_mall_product_distribution_seed.sql` 缺真实 `sys_menu/sys_dict` 写后完成态断言、按钮只 insert missing。
  - `20260607_inventory_overview_platform_stock.sql` 临时 target 被当成完成态合同，库存当前表和读模型缺真实完成态 fail-closed。
  - 最小验证以 `SqlExecutionGuardContractTest` 为主，不跑浏览器。

### 本轮代码变更

- `RuoYi-Vue/sql/20260604_product_category_attribute_seed.sql`
  - 增加 `start transaction;` / `commit;`，包住 post-DDL seed DML。
  - 增加 `tmp_product_category_attribute_seed_expected` 和 `assert_product_category_attribute_seed_completed()`。
  - 按钮菜单 `2470-2480` 改为 `on duplicate key update`，避免已有按钮展示态漂移。

- `RuoYi-Vue/sql/20260605_mall_product_distribution_seed.sql`
  - 增加 `assert_mall_product_distribution_seed_completed()`。
  - 写后校验 `sys_dict_type`、`sys_dict_data`、旧 `product_sales_status:DISABLED` 停用态和 `sys_menu` 完成态。
  - 按钮菜单 `2481-2486` 改为 upsert。

- `RuoYi-Vue/sql/20260607_inventory_overview_platform_stock.sql`
  - 增加 `tmp_inventory_overview_platform_stock_menu_expected` 和 `assert_inventory_overview_platform_stock_seed_completed()`。
  - 写后校验库存总览菜单、库存当前表目标行、SKU/SPU 读模型缺失或陈旧行。
  - 按钮菜单 `242001-242004` 改为 upsert，并补齐 `2420` 页菜单关键元数据更新。

- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 显式纳入 `20260604_product_category_attribute_seed.sql` 和 `20260605_mall_product_distribution_seed.sql` 的确认 token 合同。
  - 三份 SQL seed 均固定 completed procedure、调用顺序、cleanup 顺序和按钮 upsert 合同。
  - `business_menu_seed.sql` 合同对齐当前真实库存调整审核页 `Inventory/AdjustmentReview/index`。

### 数据源和远端影响

- 未执行 SQL 文件。
- 未执行远端 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - `SqlExecutionGuardContractTest` 75 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue/sql/20260604_product_category_attribute_seed.sql RuoYi-Vue/sql/20260605_mall_product_distribution_seed.sql RuoYi-Vue/sql/20260607_inventory_overview_platform_stock.sql RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java docs/plans/2026-06-09-three-terminal-p0p1-gpt54-fast-pass-record.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`
  - 通过，退出码 0。
  - 仅输出当前工作区 LF/CRLF 换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过。
  - 最终记录同步输出：`Synced 1 changed files`。

### 未验证说明

- 未执行 `node scripts/verify-three-terminal.mjs`：本轮未改 React gate、manifest 或前端运行入口，按快速模式不跑完整三端验证。
- 未做浏览器/截图/DOM 检测：用户已明确本阶段无需运行态 UI 验收。
- 未对远端数据库执行 seed：当前只是 SQL guard 和合同修复，不触达远端数据。

## 2026-06-09 02:31 Seller/Buyer 综合 Seed 与前端 Gate P1 收敛

### 参考方向

- 主参考：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 执行边界：继续只修 P0/P1，P2 记录不阻塞；不做浏览器、截图、DOM 或 UI 细调。
- 子 Agent 模型：按用户最新要求，全部使用 `gpt-5.4`。

### 子 Agent 使用记录

- 实际模型：`gpt-5.4`。
- 数量：6 个。
- 状态：全部关闭。
- 切片：
  - seller/buyer 后端账号权限隔离：未发现新的 P0/P1。
  - portal 会话、direct-login、日志审计：未发现新的 P0/P1；terminal mismatch 普通失败日志记录为 P2。
  - React route/access/proxy/request/token：未发现新的 P0/P1。
  - product/inventory/integration/warehouse 共享域边界：未发现新的 P0/P1。
  - SQL guard：发现并采纳 `seller_buyer_management_seed.sql` 的 4 个 P1。
  - verify gate：发现并采纳前端 manifest 自动发现和 JS 镜像 guard 的 P1。

### 采纳的 P1

- `seller_buyer_management_seed.sql` 写入 `seller_account_role` / `buyer_account_role` 后，完成态断言没有校验每个 active 主体下 `OWNER` 账号是否绑定 active owner role。
- `seller_buyer_management_seed.sql` 写入 `seller_role_menu` / `buyer_role_menu` 后，完成态断言没有校验每个 active owner role 是否拿到整套预期端内只读权限。
- `seller_buyer_management_seed.sql` 大段 post-DDL DML 没有事务边界，中段失败会留下半执行状态。
- `seller_buyer_management_seed.sql` 对已有 `role_key='owner'` 的端内角色只检查 active/del flag，没有检查 role signature 漂移。
- `verify-three-terminal` 关键前端测试自动发现正则没有覆盖 `system-user-service-contract.test.ts` 和 `inventory-adjustment-review-contract.test.ts`。
- 三端控制面关键 JS 运行镜像缺少 sidecar guard：`src/utils/permission.js`、`src/services/system/menu.js`、`src/pages/System/Menu/index.js`、`src/pages/System/Menu/edit.js`、`src/pages/System/Role/authUser.js`。

### 本轮代码变更

- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `assert_terminal_owner_role_slots_ready()` 增加 seller/buyer owner role 签名 fail-closed 校验。
  - `assert_seller_buyer_management_seed_completed()` 增加 owner 账号角色绑定完成态断言。
  - `assert_seller_buyer_management_seed_completed()` 增加 owner 角色端内菜单授权完成态断言。
  - post-DDL seed DML 增加 `start transaction;` / `commit;`，完成态断言通过后再提交。

- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 固定 owner role 签名 guard、owner account-role 绑定完成态、owner role-menu 授权完成态和事务顺序合同。

- `react-ui/scripts/verify-three-terminal.mjs`
  - `criticalFrontendTestPathPattern` 增加 `system-user-service` 和 `inventory-adjustment-review`。

- `react-ui/tests/verify-three-terminal-backend-gate.test.ts`
  - 增加从 manifest 删除 `system-user-service-contract.test.ts`、`inventory-adjustment-review-contract.test.ts` 必须失败的回归测试。

- `react-ui/tests/admin-auth-sidecar-contract.test.ts`
  - 纳入权限工具、系统菜单 service、系统菜单页、菜单编辑页、角色授权页 JS sidecar 合同。

- React JS 运行镜像
  - `src/utils/permission.js`
  - `src/services/system/menu.js`
  - `src/pages/System/Menu/index.js`
  - `src/pages/System/Menu/edit.js`
  - `src/pages/System/Role/authUser.js`
  - 以上文件统一改成纯 re-export 对应 TS/TSX 源文件。

### 数据源和远端影响

- 未执行 SQL 文件。
- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`
  - 修复前基线通过一次，修复后复跑通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer,product,inventory,integration,warehouse,ruoyi-system -am -DskipTests compile`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`
  - 修复前基线通过一次，修复后复跑通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer,ruoyi-system -am "-Dtest=SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest,TerminalAccountIsolationTest,TerminalRoleMenuMapperIsolationContractTest,TerminalSqlIsolationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - seller/buyer 权限和 system 三端隔离合同合计 51 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/admin-auth-sidecar-contract.test.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`
  - 通过。
  - 2 个 suites / 43 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`
  - 首次因测试顺序断言命中前置 `drop temporary table` 清理语句失败，已修正为精确相邻片段校验。
  - 复跑通过，`SqlExecutionGuardContractTest` 75 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue/sql/seller_buyer_management_seed.sql RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java react-ui/scripts/verify-three-terminal.mjs react-ui/tests/admin-auth-sidecar-contract.test.ts react-ui/tests/verify-three-terminal-backend-gate.test.ts react-ui/src/utils/permission.js react-ui/src/services/system/menu.js react-ui/src/pages/System/Menu/index.js react-ui/src/pages/System/Menu/edit.js react-ui/src/pages/System/Role/authUser.js docs/plans/2026-06-09-three-terminal-p0p1-gpt54-fast-pass-record.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`
  - 通过。
  - 仅输出当前工作区 LF/CRLF 换行提示，无空白错误。
- 对当前未跟踪记录/测试文件执行显式尾随空白检查。
  - 通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过。
  - 同步 9 个变更文件。

### 未验证说明

- 本轮未跑完整 `npm run verify:three-terminal`，只跑了覆盖本轮 P1 的 manifest、Jest、TypeScript 和 Maven 窄验证。
- 未做浏览器运行态、截图、DOM 检测或 UI 细调。

## 2026-06-09 02:56 SQL Guard、路由合同与 Domain JS Mirror 收口

### 执行边界

- 参考方向：继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准。
- 执行范围：只处理 P0/P1，即编译、guard、接口、权限、串端、service/字段缺失。
- 明确不做：浏览器运行态验收、截图、DOM 检测、UI 细调。
- 数据边界：未执行 SQL 文件，未连接或写入远程 MySQL，未读取或写入 Redis，未启动或重启后端。

### 子 Agent 使用记录

- 实际模型：`gpt-5.4`。
- 数量：6 个。
- 状态：6 个均已关闭。
- 采纳并修复的 P1：
  - `seller_buyer_management_seed.sql` 缺少 `seller_account_role` / `buyer_account_role` 反向完整性校验。
  - `seller_buyer_management_seed.sql` owner role-menu 完成态只校验包含关系，缺少异常授权检查。
  - `20260607_portal_self_audit_permission_seed.sql` owner 自助审计权限只做 count/inclusion，缺少 exact grant count。
  - `SqlExecutionGuardContractTest` 对 split terminal permission seed 的事务顺序只检查第一条 DML。
  - React domain JS mirror guard 未覆盖 Inventory、Warehouse、Finance、ProductCenter 等运行镜像，且 `npm run lint` 未接入该 guard。
- 未在本轮修复但已记录的 P1 结构风险：
  - `warehouse` 模块仍通过自身 mapper 直接读 `seller` 表，后续应拆为 seller service/facade 或明确共享 read model。
  - `inventory` 模块仍在 mapper 内直接拼 `product` / `integration` 事实表并回写库存事实，后续应拆为 product/integration facade 或稳定读模型。

### 本轮代码变更

- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - 完成态增加 seller/buyer account-role orphan 和跨主体绑定 fail-closed 检查。
  - 完成态增加 owner 角色在当前 seed 命名空间内的异常授权检查。
- `RuoYi-Vue/sql/20260607_portal_self_audit_permission_seed.sql`
  - 完成态增加 seller/buyer owner 自助审计权限 exact grant count 校验。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 固定新增 SQL 完成态断言。
  - `assertSplitTerminalPermissionSeedTransaction` 改为校验出现的每个 seller/buyer 菜单、角色、角色菜单 DML 都在事务和完成态校验之间。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/StandalonePartnerSeedMenuContractTest.java`
  - 修正 insert-select seed 中 `ON DUPLICATE KEY UPDATE values(...)` 被误识别为 values row 的解析误判。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java`
  - 路由归属合同改为解析类级 + 方法级映射后的完整 route，避免把 admin route 子路径中的 `/seller` 误判成 seller portal route。
  - 增加类级 terminal route 兜底。
- `react-ui/scripts/check-product-upstream-js-mirrors.mjs`
  - 扩大 JS mirror guard 扫描范围到 Inventory、Warehouse、Finance、ProductCenter、库存调整、库存同步策略和对应 service。
  - 纯 re-export 规则保留运行时命名导出，但忽略 TypeScript type/interface 导出。
- `react-ui/package.json`
  - `lint` 接入 `guard:product-upstream-mirrors`。
- `react-ui/tests/verify-three-terminal-backend-gate.test.ts`
  - 增加 lint 接入和新增 mirror scope 的合同测试。
- 库存 overview JS mirror
  - `InventoryAdjustButton.js`、`QuantityCell.js`、`SkuWarehouseTable.js`、`SpuSkuWarehouseTable.js`、`WarehouseViewTable.js` 收敛为纯 TSX re-export。
- `react-ui/tests/inventory-overview-contract.test.ts`、`InventoryAdminRouteContractTest.java`
  - 同步新的库存 JS mirror 文本合同。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts/check-product-upstream-js-mirrors.mjs`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/verify-three-terminal-backend-gate.test.ts tests/inventory-overview-contract.test.ts --runInBand`
  - 通过，2 个 suites / 15 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=InventoryAdminRouteContractTest,StandalonePartnerSeedMenuContractTest,TerminalRouteOwnershipTest,SqlExecutionGuardContractTest" test`
  - 通过，86 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 首次复跑失败于 `InventoryAdminRouteContractTest` 的旧 JS mirror 文本期望，已同步修正。
  - 最终复跑通过。
  - 前端 guard、React typecheck、23 个 Jest suites / 174 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过。

### 当前残留项

- P1：`warehouse` 直接读 seller 表的模块边界问题需要独立切片处理，本轮未改。
- P1：`inventory` 直接拼 product/integration 事实表的模块边界问题需要独立切片处理，本轮未改。
- P2：未做浏览器运行态、截图、DOM 检测或 UI 细调，符合本轮快速推进边界。

## 2026-06-09 03:19 Warehouse-Seller 边界切片收口

### 执行边界

- 参考方向：继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准。
- 执行范围：只处理 P0/P1，即编译、guard、接口、权限、串端、service/字段缺失。
- 明确不做：浏览器运行态验收、截图、DOM 检测、UI 细调。
- 数据边界：未执行 SQL 文件，未连接或写入远程 MySQL，未读取或写入 Redis，未启动或重启后端。

### 子 Agent 使用记录

- 实际模型：`gpt-5.4`。
- 数量：4 个只读 explorer。
- 状态：4 个均已关闭。
- 切片：
  - `warehouse` 直接读 `seller` 表边界：确认 direct SQL 点位和最小修复方向。
  - `inventory` 直接拼 `product` / `integration` 事实表边界：确认剩余 P1 仍应独立切片。
  - 三端合同测试落点：确认新增 `WarehouseModuleBoundaryContractTest` 并登记 manifest。
  - seller option 复用：确认现有 warehouse options 属于场景型入口，跨模块统一 selector 后续可另做。

### 采纳的 P1

- `warehouse` 模块通过自身 mapper XML 直接 `join seller` / `from seller`，同时承担卖家 options 和正常卖家校验，违反模块边界。
- 第三方仓列表的 `sellerKeyword` 原先依赖 SQL 联表筛选，迁出联表后必须避免卖家预查询误吃 PageHelper 分页。
- `seller_buyer_management_seed.sql` 中 owner role 校验和最终提交顺序合同因换行片段失配导致完整三端 gate 失败；该 P1 作为验证闸门阻塞项一并收口。

### 本轮代码变更

- `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/WarehouseSellerLookupService.java`
  - 新增 warehouse 侧卖家快照读取端口。
- `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/domain/WarehouseSellerProfile.java`
  - 新增仓库模块使用的卖家资料快照 DTO。
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/WarehouseSellerLookupServiceImpl.java`
  - seller 模块实现 warehouse 卖家快照端口。
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/mapper/SellerMapper.java`
  - 增加 warehouse 卖家快照、keyword 查询、正常卖家 options 和正常卖家计数方法。
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - 将 seller 事实查询留在 seller 模块内。
- `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java`
  - 第三方仓列表查询后批量补全卖家展示字段。
  - 第三方仓详情补全卖家展示字段。
  - 卖家 options 和归属卖家校验改为调用 `WarehouseSellerLookupService`。
  - `sellerKeyword` 只接受分页前预解析后的 `sellerIds`。
- `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/controller/AdminWarehouseController.java`
  - 第三方仓列表在 `startPage()` 前执行 `prepareThirdPartyWarehouseQuery(...)`，避免 PageHelper 被卖家预查询消费。
- `RuoYi-Vue/warehouse/src/main/resources/mapper/warehouse/WarehouseMapper.xml`
  - 移除 `seller` 联表和 seller options / count SQL。
  - 保留 `third_party_warehouse.seller_id`，通过预解析后的 `sellerIds` 过滤。
- `RuoYi-Vue/warehouse/src/test/java/com/ruoyi/warehouse/architecture/WarehouseModuleBoundaryContractTest.java`
  - 新增静态合同：warehouse mapper 不得直接读 seller 存储；warehouse 不得 import seller 模块内部；第三方仓 seller 预处理必须早于 `startPage()`。
- `react-ui/tests/three-terminal.manifest.json`
  - 登记 `WarehouseModuleBoundaryContractTest`。
- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - 重写合同要求的 owner role join 和 `assert_seller_buyer_management_seed_completed()` / `commit` / cleanup 相邻块，修复精确换行片段匹配。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl warehouse,seller -am "-Dtest=WarehouseModuleBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - `WarehouseModuleBoundaryContractTest` 2 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 首次发现 `WarehouseModuleBoundaryContractTest` 在 `backendTestClasses` 重复登记，已修正。
  - 复跑通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest#sellerBuyerManagementSeedMustFailClosedOnInactiveTerminalOwnerRoles+sellerBuyerManagementSeedMustPreflightRoleMenuAndAssertFinalState" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - 失败的 2 个 SQL guard 聚焦测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 首次复跑失败于 `SqlExecutionGuardContractTest` 的 seed 精确换行合同，已修正。
  - 最终复跑通过。
  - 前端 guard、React typecheck、23 个 Jest suites / 174 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过。
- `cd E:\Urili-Ruoyi; git diff --check -- <本轮相关文件>`
  - 通过。
  - 仅输出当前工作区 LF/CRLF 换行提示，无空白错误。
- `rg -n "join seller|from seller|join seller_account|from seller_account|com\.ruoyi\.seller" RuoYi-Vue\warehouse\src\main\java RuoYi-Vue\warehouse\src\main\resources\mapper\warehouse\WarehouseMapper.xml`
  - 无命中，退出码 1，符合预期。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过。
  - 同步 14 个变更文件。

### 当前残留项

- P1：`inventory` 仍直接拼 `product` / `integration` 事实表，下一切片应优先切 `source_warehouse_stock_detail` 直连。
- P2：未做浏览器运行态、截图、DOM 检测或 UI 细调，符合本轮快速推进边界。

## 2026-06-09 03:38 Inventory-Integration 来源库存影响范围切片

### 执行边界

- 参考方向：继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准。
- 执行范围：只处理 P0/P1，即编译、guard、接口、权限、串端、service/字段缺失。
- 明确不做：浏览器运行态验收、截图、DOM 检测、UI 细调。
- 数据边界：未执行 SQL 文件，未连接或写入远程 MySQL，未读取或写入 Redis，未启动或重启后端。

### 子 Agent 使用记录

- 实际模型：`gpt-5.4`。
- 数量：3 个只读 explorer。
- 状态：3 个均已关闭。
- 切片：
  - `inventory` 使用 `source_warehouse_stock_detail` 的点位和最小切法。
  - `integration` source warehouse stock read model 字段与聚合粒度。
  - `inventory` 模块边界合同测试落点。

### 采纳的 P1

- `inventory` 的 `selectSpuIdsBySourceConnection` 通过 mapper XML 直接 `join source_warehouse_stock_detail`，用于按 `connectionCode` 找受影响 SPU，属于 inventory 对 integration 来源库存 read model 的直连。
- 不能把官方仓库存行生成直接改到 `source_warehouse_stock_group`，因为 group key 不包含 `master_warehouse_name`，会把多主仓库存行合并错。
- 因此本轮只切“来源连接影响范围发现链”：integration 只提供受影响来源 SKU key，inventory 自己用 `product_sku_source_binding` 换算 SPU。

### 本轮代码变更

- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/domain/InventorySourceSkuKey.java`
  - 新增来源 SKU 身份 DTO，承载 `sourceScope`、`masterSku`、`masterProductName`。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/InventorySourceWarehouseStockLookupService.java`
  - 新增 inventory 侧来源仓库存查询端口。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/SourceWarehouseStockInventoryLookupServiceImpl.java`
  - integration 实现 inventory 端口，通过自身 mapper 查询来源库存影响范围。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/mapper/UpstreamSystemMapper.java`
  - 增加 `selectAffectedOfficialMasterSourceSkuKeysByConnection(...)`。
- `RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml`
  - 新增 `InventorySourceSkuKeyResult`。
  - 新增 `selectAffectedOfficialMasterSourceSkuKeysByConnection`，从当前连接快照和现有 `source_warehouse_stock_group` 组合取受影响来源 SKU key。
  - 明确不读取 `product_sku` / `product_sku_source_binding`。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java`
  - `selectSourceInventoryOverviewSpuIdsByConnection(...)` 改为通过 `ObjectProvider<InventorySourceWarehouseStockLookupService>` 获取来源 SKU key。
  - `refreshSourceInventoryOverviewByConnection(...)` 复用同一来源 SKU key -> SPU 转换链。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/mapper/InventoryOverviewMapper.java`
  - 删除旧 `selectSpuIdsBySourceConnection(...)` 声明，新增 `selectSpuIdsBySourceSkuKeys(...)`。
- `RuoYi-Vue/inventory/src/main/resources/mapper/inventory/InventoryOverviewMapper.xml`
  - 删除旧 `selectSpuIdsBySourceConnection` 对 `source_warehouse_stock_detail` 的 join。
  - 新增 `selectSpuIdsBySourceSkuKeys`，只读取 inventory/product 绑定侧数据并保留 `master_product_name_snapshot` 当前匹配口径。
- `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryOverviewRefreshContractTest.java`
  - 更新刷新合同，固定 inventory 通过来源库存端口获取 source key，再通过自身 mapper 换算 SPU。
- `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventorySourceWarehouseStockBoundaryContractTest.java`
  - 新增边界合同：inventory 的 SPU 发现语句不得读取 integration 表；integration 的 source key 查询不得读取 product 表；不得把 `source_stock_group_key` 和 `source_sku_group_key` 当成同一域 key。
- `react-ui/tests/three-terminal.manifest.json`
  - 登记 `InventorySourceWarehouseStockBoundaryContractTest`。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory,integration -am "-Dtest=InventoryOverviewRefreshContractTest,InventorySourceWarehouseStockBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - `InventoryOverviewRefreshContractTest`、`InventorySourceWarehouseStockBoundaryContractTest` 共 2 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `rg -n "selectSpuIdsBySourceConnection" RuoYi-Vue\inventory RuoYi-Vue\integration react-ui\tests\three-terminal.manifest.json`
  - 生产代码无残留；仅新合同中的“不得出现”断言命中。
- `rg -n -F "from product_" / "join product_" / "product_sku_source_binding" RuoYi-Vue\integration\src\main\resources\mapper\integration\UpstreamSystemMapper.xml RuoYi-Vue\integration\src\main\java\com\ruoyi\integration\service\impl\SourceWarehouseStockInventoryLookupServiceImpl.java`
  - 无命中，退出码 1，符合预期。
- `cd E:\Urili-Ruoyi; git diff --check -- <本轮相关文件>`
  - 通过。
  - 仅输出当前工作区 LF/CRLF 换行提示，无空白错误。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端 guard、React typecheck、23 个 Jest suites / 174 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过，其中 inventory 2 个合同测试通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过。
  - CodeGraph 输出 `Synced 10 changed files`。

### 当前残留项

- P1：`inventory` 的官方仓库存行生成仍需按 `master_warehouse_name` 读取 `source_warehouse_stock_detail`，本轮未改变业务聚合口径。
- P1：`inventory` 仍直接读取 `product_sku`、`product_spu`、`product_sku_source_binding` 等 product 表，后续应独立切 product/inventory 边界。
- P1/P2：稳定官方仓主数据口径仍未完成，当前库存行仍以来源主仓名聚合，不是以平台 `warehouse_id/warehouse_code` 聚合。
- P2：未做浏览器运行态、截图、DOM 检测或 UI 细调，符合本轮快速推进边界。

## 2026-06-09 P0/P1 快速推进：Inventory Seller Option 与模块边界合同

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮子 Agent 全部使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 实际使用 3 个只读 explorer，均已关闭。
- 只读结论：
  - `inventory` 官方仓库存行生成可以继续用 integration port 窄切，但不能把整条库存重建链一次拆到 `product`。
  - `inventory -> product` 的下一刀应优先切 SKU/SPU 发现查询，不动 `upsert* + refresh*` 重建链。
  - `manifest` 本身无明显漏登；P1 缺口是 `inventory`、`integration`、`product` 的模块级边界合同还不够硬。

### 本轮代码变更

- `RuoYi-Vue/inventory/src/main/resources/mapper/inventory/InventoryOverviewMapper.xml`
  - `selectSellerOptions` 改为直接读取 `inventory_overview_spu_read_model`。
  - 不再为了卖家筛选项 join `product_spu` 或 `inventory_sku_warehouse_stock`。
- `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventorySourceWarehouseStockBoundaryContractTest.java`
  - 新增 seller option 边界合同，固定卖家筛选项只能来自 inventory SPU read model。
- `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryModuleBoundaryContractTest.java`
  - 新增模块级合同：`inventory` Java 代码不得 import `product/integration/warehouse` 的 mapper 或 impl service。
  - 新增资源边界合同：`source_warehouse_stock_*` / `upstream_system_*` 读取只能保留在已记录的 `InventoryOverviewMapper.xml` 残留范围内。
- `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/architecture/IntegrationModuleBoundaryContractTest.java`
  - 新增模块级合同：`integration` 不得 import `product/warehouse` 的 mapper 或 impl service。
  - 固定 `integration` 继续通过 `IWarehouseFactLookupService`、`IInventoryOverviewService`、`InventorySourceWarehouseStockLookupService` 这类 public port 协作。
  - 固定 `integration/pom.xml` 不得直接依赖 `warehouse`。
- `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductModuleBoundaryContractTest.java`
  - 将原先只禁止 `integration.service.impl` 的检查，扩展为禁止 `integration/inventory/warehouse/seller/buyer` 的 mapper 或 impl service。
  - 新增 admin controller 端面合同，禁止商品管理端 controller 出现 seller/buyer 端内权限串或 portal 路由。
- `react-ui/tests/three-terminal.manifest.json`
  - 登记 `InventoryModuleBoundaryContractTest` 和 `IntegrationModuleBoundaryContractTest`。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory -am "-Dtest=InventorySourceWarehouseStockBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`InventorySourceWarehouseStockBoundaryContractTest` 2 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory,integration,product -am "-Dtest=InventoryModuleBoundaryContractTest,IntegrationModuleBoundaryContractTest,ProductModuleBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`InventoryModuleBoundaryContractTest` 2 个测试、`IntegrationModuleBoundaryContractTest` 3 个测试、`ProductModuleBoundaryContractTest` 4 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端 guard、React typecheck、23 个 Jest suites / 174 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过，其中 `inventory` 合同 5 个、`integration` 合同 10 个、`product` 合同 38 个通过。
- `cd E:\Urili-Ruoyi; git diff --check -- <本轮相关文件>`
  - 通过。
  - 仅输出当前工作区 LF/CRLF 换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过。
  - CodeGraph 输出 `Synced 5 changed files`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 2026-06-09 P0/P1 快速推进：Direct Login 日志、Role Grant SQL 与并发库存契约收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮 6 个子 Agent 全部使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 覆盖 seller 后端、buyer 后端、portal auth/direct-login/session/log、SQL seed/guard、React route/access/proxy/request/service、verify/shared-domain 六个切片。
- 6 个子 Agent 均已完成并关闭；本轮收尾复查时这些已知 Agent ID 已不在管理器中。

### 采纳的 P1

- `SellerPortalAuthController` / `BuyerPortalAuthController` 的 `/direct-login` 使用 `Map<String, String>` 接收 body，仅靠 `excludeParamNames = { "directLoginToken" }` 会受大小写和参数名漂移影响，失败路径存在一次性免密 token 被请求日志记录的风险。
- `20260606_admin_partner_role_menu_grant.sql` 中 `assert_admin_partner_child_menu_signature()` 提前 drop 临时表，导致后置 completed assertion 读取不到 `tmp_admin_partner_child_menu_signature`，脚本会在完成校验阶段失败。
- 三端总门禁暴露库存契约冲突：旧 `InventorySourceWarehouseStockBoundaryContractTest` 要求卖家选项来自库存读模型；并发库存线程按用户最新业务要求已把卖家选项改成来自 `seller` 主表。按“更近且有业务确认”的口径，选择 `seller` 主表，避免两个线程继续互相覆盖。

### 本轮代码变更

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalAuthController.java`
  - 免密登录 `@PortalLog` 增加 `isSaveRequestData = false`，保留 `isSaveResponseData = false` 和 anonymous 入口。
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalAuthController.java`
  - 同步禁用免密登录请求体日志。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalDirectLoginAuthContractTest.java`
  - 固定 seller/buyer `/direct-login` 必须禁用请求体和响应体日志。
- `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`
  - 保留 child menu signature 临时表直到 completed assertion 之后，再统一清理。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 固定 role grant 脚本的临时表生命周期和 completed assertion 顺序。
- `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventorySourceWarehouseStockBoundaryContractTest.java`
  - 将卖家选项契约改为来自 `seller` 主表，并继续禁止读取 product SPU 和库存行。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/InventoryAdminRouteContractTest.java`
  - 同步库存总览卖家选项契约为 `from seller s`。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=SqlExecutionGuardContractTest,PortalDirectLoginAuthContractTest,PortalLogAspectContractTest,LogAspectSensitiveFieldFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,inventory -am "-Dtest=InventoryAdminRouteContractTest,InventorySourceWarehouseStockBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - 通过。
  - 前端 guard、React typecheck、Jest 23 suites / 178 tests、后端 reactor test-compile、后端三端合同均通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过，只有当前工作区既有 LF/CRLF 提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过，CodeGraph 输出 `Synced 4 changed files`。

### P2 记账项

- `PortalLogAspect` 在 JSON 序列化失败 fallback 路径仍可能记录原始参数文本，并存在 `printStackTrace()`；本轮 direct-login 入口已关闭请求体日志，不作为当前 P1 扩散。
- `react-ui/src/app.tsx` 与 `react-ui/src/requestErrorConfig.ts` 仍各维护一份 portal 401 分流逻辑；当前 guard 和 Jest 已通过，后续可抽共享 helper。
- `verify-three-terminal` 关键测试发现仍部分依赖文件名/类名正则；当前未漏挂，不阻塞。
- integration / warehouse 边界仍有后续端口化空间；当前合同已固定允许范围，不作为当前 P1。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮发现另一个库存线程 active 并正在改同一 Mapper，最终按该线程中用户最新业务要求收敛契约；没有继续强写库存 Mapper。

## 2026-06-09 P0/P1 快速推进：远端端内菜单 perms 运行库收敛

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 数据源确认

- 当前后端激活配置仍以 `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` / `application-druid.yml` 和本机 `.env.local` 为准。
- MySQL 目标为远端 `fenxiao@TENCENT64.site:28594`。
- Redis 配置确认仍指向远端 `114.132.156.75:6379`，database `0`；本轮未连接、读取或写入 Redis。
- 本记录不包含 `.env.local` 中的数据库密码、Redis 密码或 token secret。

### 子 Agent 使用记录

- 按用户最新要求，本轮只使用 `gpt-5.4` 子 Agent，不再使用 GPT-5.3 Codex。
- 3 个只读子 Agent 分别复核运行库 schema 缺口、SQL 执行顺序和代码运行期风险。
- 结论一致：`seller_menu` / `buyer_menu` 缺 `perms_unique_key` 与非空 `perms` 唯一索引不是当前 Java/MyBatis 直接崩溃型 P0，但会破坏端内菜单权限 slot 和 role-menu grant 的数据库完整性前提，属于 P1。
- 子 Agent 均已返回完成状态；关闭时工具显示这些 agent 已不在活动列表。

### 采纳的 P1

- 远端运行库 `seller_menu` / `buyer_menu` 已有端内菜单数据，且菜单 ID 已在隔离区间，但缺少：
  - `perms_unique_key` 生成列；
  - `uk_seller_menu_perms` / `uk_buyer_menu_perms` 唯一索引。
- 风险：没有数据库唯一兜底时，后续 SQL 或人工 DML 可以写出重复非空 `perms`，使基于 `perms` 的 terminal menu slot guard 和 role-menu grant 失去“一条权限只对应一个菜单槽位”的前提。
- 预检未发现重复非空 `perms`、非法端前缀、菜单 ID 越界或 role-menu 孤儿关系，因此可以执行已有 fail-closed seed 做运行库收敛。

### 已执行

- 使用当前 `.env.local` 激活的远端 MySQL 连接，执行 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 执行变量：
  - `@confirm_seller_buyer_management_seed = 'APPLY_SELLER_BUYER_MANAGEMENT_SEED'`
  - `@seller_buyer_management_seed_profile = 'PATCH_EXISTING'`
- 执行结果：`executed_statements=123`。
- 影响范围：远端 MySQL DDL/DML 收敛 terminal 管理 seed、端内菜单权限约束及相关幂等数据；未触碰 Redis。
- 本轮未执行 `20260604_three_terminal_isolation_migration.sql`，因为当前事实显示账号主体列、菜单区间和自增游标已经满足本次 P1 收敛条件；本次缺口集中在 `seller_buyer_management_seed.sql` 已能补齐的端内菜单非空 `perms` 唯一约束。

### 远端库验证结果

- 目标库：`fenxiao`。
- `seller_menu` / `buyer_menu` 均已存在 `perms_unique_key` 生成列，表达式为空白 `perms` 转 `NULL`，非空 `perms` 使用 `trim(perms)`。
- `uk_seller_menu_perms` / `uk_buyer_menu_perms` 均已存在，且为 `perms_unique_key` 上的唯一索引。
- 非法 terminal perms 数量：
  - `seller_invalid_perms=0`
  - `buyer_invalid_perms=0`
- 重复非空 perms 分组：
  - `seller_duplicate_perms=0`
  - `buyer_duplicate_perms=0`
- 菜单 ID 区间：
  - `seller_menu`：min `100008`，max `100018`，count `10`，out_of_range `0`，auto_increment `100019`
  - `buyer_menu`：min `200003`，max `200013`，count `10`，out_of_range `0`，auto_increment `200014`
- 账号表：
  - `seller_account.password` / `buyer_account.password` 均为 `varchar(100) not null`，默认值为 `null`。
  - 查询结果未出现 `seller_account.user_id` / `buyer_account.user_id` legacy 列。
- role-menu 关系：
  - `seller_role_menu_orphans_or_cross_range=0`
  - `buyer_role_menu_orphans_or_cross_range=0`

### 本地验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`SqlExecutionGuardContractTest` 77 个测试、`TerminalSqlIsolationContractTest` 12 个测试通过，共 89 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，`three-terminal manifest check passed`。
- 静态扫描：
  - `RuoYi-Vue/seller/src/main` / `RuoYi-Vue/buyer/src/main` 未发现裸 `select*AccountById(accountId)`；只命中带主体 ID 的 `select*AccountById(subjectId, accountId)` 方法和调用。
  - 同一扫描未发现端内生产代码直接依赖 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。

### 边界说明

- 本轮执行了远端 MySQL DDL/DML，目标为 `fenxiao@TENCENT64.site:28594`。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 2026-06-09 P0/P1 快速推进：六切片复核无新增阻塞

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 本轮使用 6 个 `gpt-5.4` 子 Agent，不使用 GPT-5.3 Codex。
- 覆盖 seller 后端、buyer 后端、portal auth/direct-login/token/log、SQL seed/guard、React route/service/access/manifest、共享业务域六个切片。
- 6 个子 Agent 均已完成并关闭。
- 六个切片结论一致：未发现当前 P0/P1 范围内的新增阻塞。

### 主线程复核

- `react-ui` manifest 自检通过。
- `RuoYi-Vue/seller/src/main` 和 `RuoYi-Vue/buyer/src/main` 未发现端内生产代码直接依赖 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。
- 生产与测试扫描未发现裸单参数 `select*AccountById(accountId)` 入口。
- 生产、前端和测试范围未发现旧 `portal_direct_login:{token_hash}` 读取。
- seller/buyer 管理端关键接口权限、会话只读权限、强退权限、账号角色组合权限、portal `@PortalPreAuthorize` 均保持端隔离。
- SQL 侧密码列无 `default ''` 兜底，三端迁移继续保留空密码 fail-closed 合同。

### 本轮代码变更

- 无新增 P0/P1 代码修复。
- 仅追加本 Markdown 检查点和目标追踪记录。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest,SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest,SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - seller 侧 82 个测试通过。
  - buyer 侧 83 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - 通过。
  - 前端 guard、React typecheck、Jest 23 suites / 178 tests、后端 reactor test-compile、后端三端合同均通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 已执行，结果 `Already up to date`。

### 当前残留项

- P2：seller/buyer admin controller 级权限注解可后续补专门合同测试；当前 controller 注解存在且 service/mapper 行为已有覆盖，不作为 P1。
- P2：buyer 自助登出、改密 service 自身主要做 session shape 与主体账号归属校验，在线态依赖入口 `@PortalPreAuthorize`；后续若出现内部复用入口，需要继续防止绕过 portal guard。
- P2：`portal_direct_login_ticket` DDL 同时存在于专用脚本与综合 seed，当前定义一致，后续可防 schema drift。
- P2：裸跑 `npx jest` 会遇到 `jest.config.js` 与 `jest.config.ts` 双配置；正式入口已统一走 `verify-three-terminal`。
- P2：`portal.seller.web.url` / `portal.buyer.web.url` 仍信任环境配置；配置错端时更可能表现为弹窗不可用，后续可增加配置形态校验。
- P2：portal 401 处理逻辑在 `app.tsx` 和 `requestErrorConfig.ts` 各维护一份，当前行为一致，后续可抽共享 helper。
- P2：`verify-three-terminal` 不支持 `--runTestsByPath`，窄测需要显式走 Jest。
- P2：共享业务域仍以 `seller_id` 作为部分共享事实过滤主轴；当前没有变成第四终端，长期可继续收敛为更中性的 subject 抽象。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮未做远端直登人工交叉验证；当前只以代码、guard、合同和单测作为 P0/P1 快速推进证据。

## 2026-06-09 P0/P1 快速推进：Portal 商品 Facade 会话完整性修复

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 本轮继续使用 6 个 `gpt-5.4` 子 Agent，不使用 GPT-5.3 Codex。
- 覆盖账号密码、角色菜单部门、portal auth/direct-login、SQL seed/guard、React entry/service/verify、共享业务域六个切片。
- 6 个子 Agent 均已完成并关闭。

### 采纳的 P1

- 共享业务域切片发现 seller/buyer portal 商品 facade 的会话校验弱于端内账号基线：只校验 `terminal + subjectId + accountId`，没有要求 `tokenId` 非空，也没有确认当前账号仍绑定在当前主体下。
- 已修复为 seller/buyer 对称校验：必须满足端类型正确、`subjectId` 存在、`accountId` 存在、`tokenId` 非空，并通过当前端 Mapper 校验 `subjectId + accountId` 绑定仍存在；否则返回未授权。

### 本轮代码变更

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java`
  - 注入 `SellerMapper`。
  - `assertSellerSession(...)` 增加 `tokenId` 非空和 `selectSellerAccountByIdAndSellerId(...)` 绑定校验。
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java`
  - 注入 `BuyerMapper`。
  - `assertBuyerSession(...)` 增加 `tokenId` 非空和 `selectBuyerAccountByIdAndBuyerId(...)` 绑定校验。
- `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImplTest.java`
  - 增加空 `tokenId` 拒绝用例。
  - 增加账号主体绑定不存在拒绝用例。
- `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImplTest.java`
  - 增加空 `tokenId` 拒绝用例。
  - 增加账号主体绑定不存在拒绝用例。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - `SellerPortalProductServiceImplTest` 10 个测试通过。
  - `BuyerPortalProductServiceImplTest` 11 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - 通过。
  - 前端 guard、React typecheck、Jest 23 suites / 178 tests、后端 reactor test-compile、后端三端合同均通过。

### 当前残留项

- P2/待确认：新建 OWNER 主账号默认密码 `U12346` 仍与安全基线存在张力；因用户此前明确指定默认密码，本轮不静默改变。
- P2：端内菜单权限写入的友好错误提示还可增强；当前 SQL/service guard 已能 fail-closed。
- P2：direct-login 页面 5 秒无 token 时没有立刻向 opener 回传失败；当前管理端仍有 15 秒 bridge timeout。
- P2：SQL 增量自动发现当前只扫描 `RuoYi-Vue/sql` 直接子文件，后续可评估是否递归扫描。
- P2：直接运行 `npx jest` 会同时看到 `jest.config.js` 和 `jest.config.ts`，后续可统一入口或继续要求显式 `--config`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 2026-06-09 P0/P1 快速推进：端内菜单 perms SQL 契约收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮 6 个子 Agent 全部使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 覆盖切片：账号密码、角色/菜单/部门、日志/session/direct-login、管理端控制权、SQL/schema、React guard/service/verify。
- 6 个子 Agent 均已完成并关闭。

### 采纳的 P1

- `seller_menu` / `buyer_menu` 的运行时与 SQL guard 对目录菜单 `M` 的 `perms` 规则不一致：
  - 运行时和前端允许目录菜单 `M` 不填 `perms`。
  - SQL seed / migration 曾把任意空 `perms` 都判为非法，并对 `perms` 本身建唯一索引，导致多个目录空权限会冲突。
- 已统一为当前端内菜单契约：
  - 页面 `C` 和按钮 `F` 的 `perms` 必填。
  - 目录 `M` 允许空 `perms`。
  - 非空 `perms` 必须使用当前端前缀 `seller:` / `buyer:`，禁止 `*`、管理端命名空间和跨端前缀。
  - 非空 `perms` 唯一，空 `perms` 不参与唯一冲突。

### 本轮代码变更

- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - 新增 `perms_unique_key` 生成列，并把 `uk_seller_menu_perms` / `uk_buyer_menu_perms` 改为基于非空权限辅助列唯一。
  - 新增 replay-safe `add_column_if_missing(...)` 和 `recreate_index_if_mismatch(...)` helper。
  - `assert_terminal_menu_template_ready(...)` 只拒绝 `C/F` 空 `perms`，并只检查非空权限重复。
- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`
  - 与综合 seed 同步 terminal menu 规则、生成列和唯一索引修复。
  - `assert_terminal_menu_integrity_ready()` 只拒绝 `C/F` 空 `perms`，并只检查非空权限重复。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 更新 SQL 合同，锁定 `perms_unique_key`、`C/F` 必填、非空唯一、replay-safe 列和索引重建顺序。

### 未采纳为本轮 P1 的结论

- 子 Agent 提到新建 OWNER 主账号默认密码 `U12346` 属于可预测口令风险。
- 主线程不在本轮直接改动该行为，原因是用户此前明确指定登录密码默认 `U12346`；这属于产品安全策略冲突，需要后续单独确认。
- 当前只作为 P2/待确认项记录，不静默改变账号创建语义。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`SqlExecutionGuardContractTest` 77 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - 通过。
  - 前端 guard、React typecheck、Jest 23 suites / 178 tests、后端 reactor test-compile、后端三端合同均通过。

### 当前残留项

- P2/待确认：新建 OWNER 主账号默认密码 `U12346` 是否继续保留，需要在产品便捷性和安全基线之间单独确认。
- P2：端内 `account_role` / `role_menu` 当前主要依赖应用层、seed 自检和合同测试约束，后续如迁移窗口允许可补数据库外键。
- P2：旧 direct-login Redis key 删除兼容分支可在确认线上无旧 key 后清理。
- P2：`@@initialState` 生成产物暂未纳入 verify gate，本轮未出现故障，后续可补低成本 guard。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 2026-06-09 P0/P1 快速推进：React App Runtime InitialState 桥接修复

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 本轮使用 6 个 `gpt-5.4` 子 Agent，不使用 GPT-5.3 Codex。
- 覆盖 seller/buyer 后端隔离、portal 登录/会话/direct-login/401/审计 DTO、SQL guard/seed、React route/access/proxy/request/session/service/JS mirror、共享业务域、verify gate/manifest/Maven reactor 六个切片。
- 6 个子 Agent 均已完成并关闭。

### 采纳的 P1

- React 入口切片发现 `react-ui/src/app.js` 只有 `export * from './app.tsx'`，Umi 的 `@@initialState` 生成物无法识别 `getInitialState`，生成文件只剩 `loading` / `refresh` 空壳。
- 影响：管理端 runtime 入口存在实质回归风险，典型表现是 `@@initialState` 缺少 `setInitialState`，影响当前用户状态链路和设置抽屉。

### 已完成

- `react-ui/src/app.js`
  - 从隐式 `export *` 改为显式桥接 `getInitialState`、`layout`、`rootContainer`、`onRouteChange`、`patchClientRoutes`、`render`、`request`。
- `react-ui/tests/admin-auth-sidecar-contract.test.ts`
  - 更新 sidecar 合同期望，锁定 `app.js` 必须显式桥接 runtime 关键导出。
- `react-ui/scripts/check-portal-token-isolation.mjs`
  - 同步 portal token guard 的 exact source 期望，避免 guard 把 runtime 修复误判为漂移。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npx max setup`
  - 通过，重新生成 Umi 文件。
- `react-ui/src/.umi/plugin-initialState/@@initialState.ts`
  - 已生成 `import { getInitialState } from '@/app'`，并包含 `setInitialState`。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/admin-auth-sidecar-contract.test.ts --runInBand`
  - 通过，1 suite / 33 tests。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - 通过。
  - 前端 guard、React typecheck、Jest 23 suites / 178 tests、后端 reactor test-compile、后端三端合同均通过。

### 未采纳为 P1 的结论

- SQL guard、seller/buyer 后端隔离、portal/session/direct-login、自助日志 DTO、共享业务域、verify gate 子 Agent 均未发现新的可坐实 P0/P1。
- 共享域依赖扇出、product schema 共享读取、SQL 非递归发现、verify 手工调用 cwd、portal 401 双实现等继续作为 P2 记录，不阻塞当前快速推进。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

### 当前残留项

- P1：`inventory` 官方仓库存行生成仍直接读 `source_warehouse_stock_detail`，但已被模块合同限制在 `InventoryOverviewMapper.xml` 已知残留范围内。
- P1：`inventory` 仍直接读部分 `product_*` 表，下一刀建议只切 SKU/SPU 发现查询端口化，不一次拆库存重建链。
- P1/P2：库存行 key 仍以 `master_warehouse_name` 为口径，不是平台官方仓主数据 ID；这需要等官方仓主数据方案确认后再迁。

## 2026-06-09 P0/P1 快速推进：Inventory Product Lookup Port

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮子 Agent 全部使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 实际使用 3 个只读 explorer，均已关闭。
- 只读结论：
  - `product` 已依赖 `inventory`，所以接口不能放到 `product` 再让 `inventory` 依赖，否则会形成 Maven 循环。
  - 推荐接口放在 inventory 侧作为 consumer-owned port，product 侧新增独立 lookup service 实现，不塞进 `ProductDistributionServiceImpl`。
  - 旧 `InventoryOverviewMapper` 的 `selectSkuIdsBySpuId` / `selectSpuIdsBySourceSkuKeys` 必须删除，provider 缺失应 fail-fast。
  - 本轮复用已有测试类，不新增 manifest 测试类。

### 本轮代码变更

- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/InventoryProductLookupService.java`
  - 新增 inventory-owned product SKU/SPU 查询端口。
  - 暴露 `selectSkuIdsBySpuId(...)` 和 `selectSpuIdsBySourceSkuKeys(...)`。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java`
  - 新增 `ObjectProvider<InventoryProductLookupService>`。
  - `refreshProductInventoryOverview(...)` 刷 SKU read model 时改走 product lookup port。
  - `selectSourceInventoryOverviewSpuIdsByConnection(...)` 将 source key 转 SPU 时改走 product lookup port。
  - provider 缺失时 fail-fast 抛出 `商品库存查询服务不可用`。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/mapper/InventoryOverviewMapper.java`
  - 删除旧 product SKU/SPU 发现 mapper 声明。
- `RuoYi-Vue/inventory/src/main/resources/mapper/inventory/InventoryOverviewMapper.xml`
  - 删除旧 `selectSkuIdsBySpuId` 和 `selectSpuIdsBySourceSkuKeys`。
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductInventoryLookupServiceImpl.java`
  - 新增 product 侧端口实现，底层只调用 `ProductDistributionMapper`。
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/mapper/ProductDistributionMapper.java`
  - 新增 `selectInventorySkuIdsBySpuId(...)` 和 `selectInventorySpuIdsBySourceSkuKeys(...)`。
- `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml`
  - 新增 product-owned SKU/SPU 发现 SQL。
  - 保留 `sk.del_flag = '0'`、`b.binding_status = 'ACTIVE'`、`source_scope + master_sku + master_product_name_snapshot` 三元匹配和稳定排序。
- `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryOverviewRefreshContractTest.java`
  - 合同改为固定 inventory 通过 product lookup port 刷 SKU 和转换 SPU。
- `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventorySourceWarehouseStockBoundaryContractTest.java`
  - 固定旧 inventory mapper 方法和 SQL 不得残留。
  - 固定 product 侧实现与 mapper 承接原 product identity 查询语义。
- `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductDistributionMapperContractTest.java`
  - 新增 product inventory lookup statement 语义合同。
- `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductModuleBoundaryContractTest.java`
  - 新增 product lookup service 合同，禁止依赖 inventory mapper/impl。

### 验证结果

- `rg -n "selectSkuIdsBySpuId|selectSpuIdsBySourceSkuKeys|selectInventorySkuIdsBySpuId|selectInventorySpuIdsBySourceSkuKeys|InventoryProductLookupService|ProductInventoryLookupServiceImpl" RuoYi-Vue\inventory\src\main RuoYi-Vue\product\src\main RuoYi-Vue\inventory\src\test RuoYi-Vue\product\src\test`
  - 旧 `selectSkuIdsBySpuId` / `selectSpuIdsBySourceSkuKeys` 在 inventory 生产 mapper 中无残留。
  - 新查询只在 product 生产 mapper/service 和相关合同测试中出现。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory,product -am "-Dtest=InventoryOverviewRefreshContractTest,InventorySourceWarehouseStockBoundaryContractTest,InventoryModuleBoundaryContractTest,ProductDistributionMapperContractTest,ProductModuleBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - `inventory` 合同 5 个测试通过，`product` 合同 15 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端 guard、React typecheck、23 个 Jest suites / 174 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过，其中 `inventory` 合同 5 个、`product` 合同 40 个通过。
- `cd E:\Urili-Ruoyi; git diff --check -- <本轮相关文件>`
  - 通过。
  - 仅输出当前工作区 LF/CRLF 换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过。
  - CodeGraph 输出 `Synced 11 changed files`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

### 当前残留项

- P1：`inventory` 官方仓库存行生成仍直接读 `source_warehouse_stock_detail`，当前只用合同限制残留范围。
- P1/P2：库存行 key 仍以 `master_warehouse_name` 为口径，等待官方仓主数据方案后再迁。

## 2026-06-09 P0/P1 快速推进：Inventory Official Source Stock Port

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮子 Agent 全部使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 实际使用并关闭 3 个只读 explorer。
- 子 Agent 结论处理：
  - 采纳：官方仓库存行生成不是单点 SQL，delete obsolete、official upsert、unmatched official 都必须同步处理。
  - 采纳：保留现有“先删废弃，再 upsert，再刷新 read model”的刷新顺序，避免扩大库存平台字段覆盖风险。
  - 未采纳：直接让 inventory SQL 改读 `source_warehouse_stock_group` / `source_warehouse_stock_filter_metric`。原因是这仍然让 inventory 直接读取 integration 表，只是从 detail 换成读模型表，不符合本轮 P1 模块边界收口方向。

### 本轮代码变更

- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/domain/InventoryOfficialSourceStock.java`
  - 新增官方仓来源库存聚合 DTO，按 `sourceScope + masterSku + masterProductName + masterWarehouseName` 承载来源库存切片。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/InventorySourceWarehouseStockLookupService.java`
  - 新增 `selectOfficialMasterStocksBySourceSkuKeys(...)`，由 inventory 定义消费端口。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/SourceWarehouseStockInventoryLookupServiceImpl.java`
  - 实现新的 inventory-owned port 方法，委托 integration 自有 `UpstreamSystemMapper`。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/mapper/UpstreamSystemMapper.java`
  - 新增 `selectOfficialMasterSourceStocksBySourceSkuKeys(...)`。
- `RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml`
  - 新增官方仓来源库存聚合查询；`source_warehouse_stock_detail` 读取保留在 integration 边界内。
  - 固定 `OFFICIAL_MASTER`、`COMPREHENSIVE`、`inventory_attribute = '0'`、`master_warehouse_name` 非空，以及按来源 SKU + 来源主仓名聚合。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/InventoryProductLookupService.java`
  - 新增 `selectSourceSkuKeysBySpuId(...)`，由 product 侧提供当前 SPU 的 active source key。
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductInventoryLookupServiceImpl.java`
  - 实现 `selectSourceSkuKeysBySpuId(...)`。
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/mapper/ProductDistributionMapper.java`
  - 新增 `selectInventorySourceSkuKeysBySpuId(...)`。
- `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml`
  - 新增 product-owned source key 查询，只读 `product_sku` 和 `product_sku_source_binding`，不读 integration 来源库存表。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java`
  - `refreshProductInventoryOverview(...)` 先通过 product port 获取 source keys，再通过 integration port 获取官方仓来源库存切片。
  - delete obsolete、official upsert、unmatched official upsert 改为传入 `sourceStocks`，保留原刷新顺序。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/mapper/InventoryOverviewMapper.java`
  - delete obsolete、official upsert、unmatched official upsert 签名增加 `sourceStocks` 参数。
- `RuoYi-Vue/inventory/src/main/resources/mapper/inventory/InventoryOverviewMapper.xml`
  - 新增 `officialMasterSourceStockRows` 参数派生表片段。
  - 删除 inventory XML 中对 `source_warehouse_stock_detail` 的直接读取。
  - official、unmatched、current key 计算统一使用 `sourceStocks` 派生表。
- 合同测试同步收紧：
  - `InventoryOverviewRefreshContractTest`
  - `InventorySourceWarehouseStockBoundaryContractTest`
  - `InventoryModuleBoundaryContractTest`
  - `IntegrationModuleBoundaryContractTest`
  - `ProductDistributionMapperContractTest`
  - `ProductModuleBoundaryContractTest`

### 验证结果

- `rg -n "source_warehouse_stock_detail|source_warehouse_stock_group|upstream_system_" RuoYi-Vue\inventory\src\main\resources RuoYi-Vue\inventory\src\main\java`
  - 无命中，退出码 1，符合预期。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory,integration,product -am "-Dtest=InventoryOverviewRefreshContractTest,InventorySourceWarehouseStockBoundaryContractTest,InventoryModuleBoundaryContractTest,IntegrationModuleBoundaryContractTest,ProductDistributionMapperContractTest,ProductModuleBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - `inventory` 合同 5 个测试通过，`integration` 合同 3 个测试通过，`product` 合同 15 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端 guard、React typecheck、23 个 Jest suites / 174 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过。
  - CodeGraph 输出 `Synced 18 changed files`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

### 当前残留项

- P1/P2：库存行 key 仍以 `master_warehouse_name` 为口径，等待官方仓主数据方案后再迁。
- P2：本轮没有把库存官方仓 upsert 全量改成 Java batch 写入；当前选择是保留 SQL 驱动刷新顺序，减少库存平台字段覆盖风险。

## 2026-06-09 P0/P1 快速推进：Portal Direct Login 401 会话保护

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮 6 个子 Agent 全部使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 6 个子 Agent 均已关闭。
- 实际启动并关闭 6 个只读 explorer。
- 子 Agent 结论处理：
  - 采纳：React portal `/api/{seller|buyer}/direct-login` 失败 401 会误走普通 portal 401 逻辑，清掉当前端已有 token 并跳登录页，属于三端会话控制 P1。
  - 记录待办：`warehouse` mapper 仍直接读取 `upstream_system_*`，属于模块边界 P1，建议下一刀通过 integration 只读 pairing snapshot port 富化仓库列表。
  - 记录待办：`inventory` mapper 仍直接读取 `product_*`，属于更重的模块边界 P1，建议后续优先切 `refreshSkuReadModel` / `refreshSpuReadModel` 所需 product 快照 port。
  - 未采纳为本轮改动：seller/buyer 后端账号权限链、SQL/DDL guard、verify manifest/gate、管理端权限面未发现新的确定 P0/P1。

### 本轮代码变更

- `react-ui/src/utils/portalRequest.ts`
  - 新增 `isPortalDirectLoginApiUrl(...)`，只识别 `/api/seller/direct-login` 和 `/api/buyer/direct-login`。
- `react-ui/src/requestErrorConfig.ts`
  - 普通 portal API 401 仍清当前端 token 并跳对应登录页。
  - direct-login API 401 只抛给页面处理，不清 token、不跳登录页。
- `react-ui/src/app.tsx`
  - body-level `code/errorCode = 401` 的响应拦截同样豁免 direct-login API，避免与统一错误处理口径不一致。
- `react-ui/tests/portal-unauthorized-redirect.test.ts`
  - 新增 direct-login BizError 401、响应体 401、HTTP 401 三类合同。
  - 固定失败时仍 reject，但不得调用 `clearTerminalSessionToken`、不得清 admin token、不得 `history.replace`。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/portal-unauthorized-redirect.test.ts --runInBand`
  - 通过，`1` 个 suite / `19` 个测试。
  - 直接运行 `npx jest tests/...` 会因为仓库同时存在 `jest.config.js` 和 `jest.config.ts` 失败，已按项目配置显式指定 `--config jest.config.ts` 重跑通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`
  - 通过，输出 `Portal token isolation guard passed.`
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过。
  - 仅输出当前工作区 LF/CRLF 换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过。
  - CodeGraph 输出 `Synced 4 changed files`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮未跑完整 `npm run verify:three-terminal`；当前改动是前端 direct-login 401 会话保护窄切片，已跑相关 Jest、manifest、tsc 和 portal token guard。

### 当前残留项

- P1：`warehouse` 列表 SQL 仍直接读取 `upstream_system_warehouse_pairing` / `upstream_system_connection`，下一刀建议迁到 integration 公开只读 port 后由 `WarehouseServiceImpl` 批量 enrich。
- P1：`inventory` 核心刷新 SQL 仍直接读取 `product_*`，后续应把刷新所需 product 快照收口到 `InventoryProductLookupService`。
- P1/P2：库存行 key 仍以 `master_warehouse_name` 为口径，等待官方仓主数据方案后再迁。
- P2：`app.tsx` 与 `requestErrorConfig.ts` 的 401 分流逻辑仍有重复，当前只修行为一致性，不做抽象重构。

## 2026-06-09 P0/P1 快速推进：Warehouse Upstream Pairing Projection Port

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮 6 个子 Agent 全部使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 覆盖切片：warehouse SQL 直连点、integration projection port 设计、合同测试、前端字段兼容、Maven 依赖链、Markdown 记录口径。
- 结论收口：2 个前序子 Agent 已关闭，3 个本轮收尾关闭，1 个历史 ID 收尾时已不在当前可管理列表；当前没有保留运行中的子 Agent。
- 采纳的 P1：`warehouse` mapper 直接 join `upstream_system_warehouse_pairing` / `upstream_system_connection`，违反模块边界，应迁到 integration 公开只读投影端口后由 `WarehouseServiceImpl` enrich。
- 未采纳为本轮改动：前端页面字段保持现有扁平字段即可兼容，不需要改 UI；`inventory` 读 `product_*` 属于后续 P1，继续记录不阻塞本轮。

### 本轮代码变更

- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/domain/UpstreamWarehousePairingSnapshot.java`
  - 新增 integration-owned 仓库配对只读快照 DTO。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/IUpstreamWarehousePairingProjectionService.java`
  - 新增按系统仓库编码批量读取 active pairing snapshot 的公开端口。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamWarehousePairingProjectionServiceImpl.java`
  - 过滤空编码、去重后委托 integration 自有 mapper。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/mapper/UpstreamSystemMapper.java`
  - 新增 `selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes(...)`。
- `RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml`
  - 新增 `WarehousePairingSnapshotResult` 和 active pairing snapshot 查询；`upstream_system_*` 读取保留在 integration 边界内。
- `RuoYi-Vue/warehouse/src/main/resources/mapper/warehouse/WarehouseMapper.xml`
  - 移除 `WarehouseSelect` 中对 `upstream_system_warehouse_pairing` / `upstream_system_connection` 的直接 join，保留对外字段 null alias 以维持 resultMap 和前端字段契约。
- `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java`
  - 官方仓列表、第三方仓列表、按 ID 查询和配对前查询统一通过 integration projection port enrich 配对字段。
- `RuoYi-Vue/warehouse/src/test/java/com/ruoyi/warehouse/architecture/WarehouseModuleBoundaryContractTest.java`
  - 固定 warehouse 主资源不得出现 `upstream_system_`，且不得 import integration mapper / service.impl。
- `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/architecture/IntegrationModuleBoundaryContractTest.java`
  - 固定 integration 拥有仓库配对投影 SQL 和公开 projection service。
- `AGENTS.md`
  - 现行子 Agent 规则已保持为默认 `gpt-5.4`；除非用户在当前任务重新明确要求，否则不再把 GPT-5.3 Codex 作为首选。

### 验证结果

- `rg -n "upstream_system_" RuoYi-Vue/warehouse/src/main`
  - 无命中，退出码 1，符合预期。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration,warehouse -am "-Dtest=WarehouseModuleBoundaryContractTest,IntegrationModuleBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - `integration` 合同 4 个测试通过，`warehouse` 合同 3 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl warehouse,integration,inventory,product -am -DskipTests compile`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过，退出码 0；仅输出当前工作区 LF/CRLF 换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过；先输出 `Synced 9 changed files`，补写最终记录后复跑为 `Already up to date`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮未跑完整 `npm run verify:three-terminal`；当前是后端模块边界窄切片，已跑相关 Maven 合同、后端 compile 和 manifest check。

### 当前残留项

- P1：`inventory` 核心刷新 SQL 仍直接读取 `product_*`，后续应把刷新所需 product 快照收口到 `InventoryProductLookupService`。
- P1/P2：库存行 key 仍以 `master_warehouse_name` 为口径，等待官方仓主数据方案后再迁。
- P2：warehouse 配对 enrich 仍保留对外扁平字段，后续如要改成结构化 DTO，需要同步前端字段契约。

## 2026-06-09 P0/P1 快速推进：Inventory Product Snapshot Port

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮 6 个子 Agent 全部使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 覆盖切片：inventory 直读 product 表扫描、product 快照 port 可行性、调用签名收口、合同测试、Maven 依赖链、前端影响面。
- 采纳的 P1：`InventoryOverviewMapper.xml` 中库存刷新和 SKU/SPU read model 刷新直接读取 `product_sku` / `product_spu` / `product_spu_warehouse` / `product_sku_source_binding`，应改为 inventory-owned port + product-owned snapshot 查询。
- 未采纳为本轮改动：让 product 反向写 inventory 读模型；这会反转模块所有权，当前选择是 product 只提供快照，inventory 仍 owns 库存刷新和 read model 写入。

### 本轮代码变更

- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/domain/InventoryProductSkuSnapshot.java`
  - 新增 inventory-owned SKU/SPU 快照 DTO，承载库存刷新和读模型需要的 product 字段。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/domain/InventoryProductSourceBindingSnapshot.java`
  - 新增 source binding 快照 DTO，承载来源 scope、master SKU、master product name。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/domain/InventoryProductWarehouseSnapshot.java`
  - 新增 product warehouse 快照 DTO，承载仓库 kind/code/name。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/InventoryProductLookupService.java`
  - 增加 SKU 快照、source binding 快照、warehouse 快照读取方法。
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductInventoryLookupServiceImpl.java`
  - 由 product 模块实现 inventory-owned port，并委托 product mapper 查询 product 自有表。
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/mapper/ProductDistributionMapper.java`
  - 增加 inventory snapshot 查询 mapper 方法。
- `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml`
  - 新增 `selectInventorySkuSnapshotsBySpuId`、`selectInventorySkuSnapshotsBySkuIds`、`selectInventorySourceBindingSnapshotsBySpuId`、`selectInventoryWarehouseSnapshotsBySpuId`。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/mapper/InventoryOverviewMapper.java`
  - 库存刷新和 read model 刷新方法签名改为接收 product 快照列表。
- `RuoYi-Vue/inventory/src/main/resources/mapper/inventory/InventoryOverviewMapper.xml`
  - 用 `productSkuSnapshotRows`、`productSourceBindingSnapshotRows`、`productWarehouseSnapshotRows` 参数行替换所有 direct `product_*` 表读取。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java`
  - `refreshProductInventoryOverview(...)` 一次性加载 product 快照、source stock slices，再传入 mapper 重建库存行和 read model。
  - 人工调整、批量调整后的 read model 刷新改为通过 product snapshot port。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryAdjustmentReviewServiceImpl.java`
  - 审核单生效后的 read model 刷新改为通过 product snapshot port。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryStockSyncPolicyServiceImpl.java`
  - 自动同步策略应用后的 read model 刷新改为通过 product snapshot port。
- `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryOverviewRefreshContractTest.java`
- `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventorySourceWarehouseStockBoundaryContractTest.java`
- `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductDistributionMapperContractTest.java`
- `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductModuleBoundaryContractTest.java`
  - 固定 inventory mapper 不得直接读取 `product_*`，product mapper 才能读取 product 自有表并输出 snapshot。

### 验证结果

- `rg -n "product_sku|product_spu|product_spu_warehouse|product_sku_source_binding" RuoYi-Vue\inventory\src\main\resources\mapper\inventory\InventoryOverviewMapper.xml`
  - 无命中，退出码 1，符合预期。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory,product,integration,warehouse -am "-Dtest=InventoryOverviewRefreshContractTest,InventorySourceWarehouseStockBoundaryContractTest,ProductDistributionMapperContractTest,ProductModuleBoundaryContractTest,InventoryModuleBoundaryContractTest,IntegrationModuleBoundaryContractTest,WarehouseModuleBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - `inventory` 5 个测试通过，`integration` 4 个测试通过，`warehouse` 3 个测试通过，`product` 15 个测试通过。
  - 首轮失败是新增合同断言过窄，要求 `and sk.del_flag = '0'`；实际 SQL 合理写法是 `where sk.del_flag = '0'`，已修正合同后重跑通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过，退出码 0；仅输出当前工作区 LF/CRLF 换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过；先输出 `Synced 16 changed files`，补写最终记录后复跑为 `Already up to date`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮未跑完整 `npm run verify:three-terminal`；当前是后端模块边界窄切片，已跑相关 Maven 合同和编译链。

### 当前残留项

- P1/P2：库存行 key 仍以 `master_warehouse_name` 为口径，等待官方仓主数据方案后再迁。
- P2：product snapshot 目前通过 MyBatis `union all` 参数行注入 inventory SQL，后续如果刷新体量增大，可以评估批处理临时表或 Java batch 写入，但不阻塞当前 P0/P1。
- P2：warehouse 配对 enrich 仍保留对外扁平字段，后续如要改成结构化 DTO，需要同步前端字段契约。

## 2026-06-09 P0/P1 快速推进：Inventory SQL Guard Exact Target Contract

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮直接使用 6 个 `gpt-5.4` 子 Agent，不再把 GPT-5.3 Codex 作为首选。
- 6 个子 Agent 覆盖 seller/buyer 后端账号权限隔离、portal auth/direct-login/token-session/Redis key/401/审计 DTO、SQL guard/seed、React route/access/proxy/request/session/service/JS mirror、共享业务域、verify gate/manifest/Maven reactor 六个切片。
- 6 个子 Agent 均已完成并关闭。
- 采纳的 P1：
  - `20260609_inventory_auto_wms_stock_sync_policy.sql` high-impact guard 偏弱，缺 exact target、schema definition 和 completed 断言。
  - `20260609_inventory_adjustment_review.sql` 脚本已有预检，但缺写后 completed 断言和专属合同，后续容易回退。
- 未采纳为本轮改动：
  - seller/buyer 后端隔离、portal direct-login/token/401、React route/service/JS mirror、共享域边界、verify gate 切片均未发现新的可坐实 P0/P1。
  - `gpt-5.4` gate 子 Agent 已跑过完整 `verify-three-terminal` 通过；主线程本轮只补本次改动后的最小必要验证。

### 本轮代码变更

- `RuoYi-Vue/sql/20260609_inventory_auto_wms_stock_sync_policy.sql`
  - 增加 `@inventory_auto_wms_*_expected_count` 和 `@inventory_auto_wms_*_expected_signature`，要求执行前预览 exact target。
  - 新增 `tmp_inventory_auto_wms_sys_menu_seed`、`tmp_inventory_auto_wms_dict_type_seed`、`tmp_inventory_auto_wms_dict_data_seed`。
  - 新增 `tmp_inventory_auto_wms_column_contract` 和 `tmp_inventory_auto_wms_index_contract`，在 DDL helper 执行后校验列类型、默认值、nullable、索引列顺序。
  - 新增 `assert_inventory_auto_wms_seed_targets()`、`assert_inventory_auto_wms_schema_ready()`、`assert_inventory_auto_wms_stock_sync_policy_completed()`，把目标集漂移、同名错定义和写后状态异常都改为 fail-closed。
- `RuoYi-Vue/sql/20260609_inventory_adjustment_review.sql`
  - 新增 `assert_inventory_adjustment_review_completed()`，在 `commit` 前校验菜单、默认策略、全局绑定和 Quartz job 写后完成态。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 将 `20260609_inventory_auto_wms_stock_sync_policy.sql` 加入显式 high-impact guard 清单。
  - 新增 auto WMS 专属合同，固定 exact target、schema contract、事务顺序和 completed guard。
  - 新增 inventory adjustment review 专属合同，固定 target signature、事务顺序和 completed guard。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`SqlExecutionGuardContractTest` 77 个测试通过。
  - 首轮未加 `-Dsurefire.failIfNoSpecifiedTests=false` 时，`ruoyi-common` 因无匹配测试失败；这是 Maven/Surefire 参数问题，不是合同失败。
  - 第二轮合同测试曾因测试字符串匹配到过程体内较早的 `assert_inventory_auto_wms_schema_ready()` 失败，已将顺序断言收敛为“索引补齐在事务开始前”后通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过，退出码 0；仅输出当前工作区 LF/CRLF 换行提示，无空白错误。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮未跑完整 `node scripts\verify-three-terminal.mjs`；子 Agent 已在只读 gate 切片跑通过完整闸门，主线程针对本次 SQL/test 变更跑了 SQL guard 合同、manifest check 和 diff check。

### 当前残留项

- P2：auto WMS 脚本现在对核心列/索引做 definition contract；如果后续补更多非关键审计字段，可以扩展 contract 覆盖面。
- P2：库存行 key 仍以 `master_warehouse_name` 为口径，等待官方仓主数据方案后再迁。
- P2：历史 Markdown 中仍会出现 GPT-5.3 相关执行事实，但 AGENTS 和现行目标追踪已明确当前默认 `gpt-5.4`。

## 2026-06-09 P0/P1 快速推进：Read Model / Currency / Role Grant SQL Completed Guard

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮直接使用 6 个 `gpt-5.4` 子 Agent，不再使用 GPT-5.3 Codex 作为首选。
- 6 个子 Agent 覆盖 seller/buyer 后端隔离、portal auth/direct-login/token-session/Redis key/401/审计 DTO、SQL guard/seed、React route/access/proxy/request/session/service/JS mirror、共享业务域、verify gate/manifest/Maven reactor 六个切片。
- 6 个子 Agent 均已完成并关闭。
- 采纳的 P1 来自 SQL guard 子 Agent：
  - `20260607_source_product_read_model.sql` 是 delete/rebuild 读模型脚本，缺写后 completed assertion 和合同覆盖。
  - `20260607_source_warehouse_stock_read_model.sql` 同类缺写后 completed assertion 和合同覆盖。
  - `20260604_currency_showapi_sync_migration.sql` 属于动态 DDL helper + 多表高影响 DML，缺 exact target/signature 和 completed assertion。
  - `20260606_admin_partner_role_menu_grant.sql` 已有 exact target 预校验，但缺 post-DML completed assertion。
- 其余 5 个子 Agent 未发现新的可坐实 P0/P1；共享域和 verify gate 只留下 P2 watch，不阻塞当前推进。

### 本轮代码变更

- `RuoYi-Vue/sql/20260607_source_product_read_model.sql`
  - 新增 `assert_source_product_read_model_completed()`。
  - 在 `commit` 前校验 `source_product_group`、`source_product_dimension_group`、`source_product_warehouse_detail` 与临时表的行数和关键键集合一致。
- `RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql`
  - 新增 `assert_source_warehouse_stock_read_model_completed()`。
  - 在 `commit` 前校验 detail、group、filter metric 三张来源仓库存读模型表与临时表的行数和关键键集合一致。
- `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`
  - 新增 `assert_admin_partner_role_menu_grant_completed()`。
  - 在 `commit` 前校验 admin 角色对主体管理根/页菜单和 seller/buyer 管理按钮菜单的预期授权均已存在。
- `RuoYi-Vue/sql/20260604_currency_showapi_sync_migration.sql`
  - 增加 sync config、finance_currency、sys_dict_data 三组 preview expected count/signature 变量。
  - 新增 `assert_currency_showapi_sync_targets()` 和 `assert_currency_showapi_sync_completed()`。
  - 将高影响 DML 包入事务，并在 `commit` 前检查 ShowAPI provider、CNY/USD 币种默认值和 currency_code 字典默认值完成态。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 固定以上 4 个 SQL 的 completed guard、exact target、事务顺序和 drop procedure 合同，防止后续回退。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - 修复前主线程基线通过：前端 guard、React typecheck、Jest 23 suites / 178 tests、后端 reactor test-compile、后端三端合同均通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`SqlExecutionGuardContractTest` 77 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过，退出码 0；仅输出当前工作区 LF/CRLF 换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过；首次同步返回 `Synced 1 changed files`，补写最终记录后复跑返回 `Already up to date`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

### 当前残留项

- P2：verify gate 子 Agent 观察到前端关键测试自动发现正则暂未包含 `product-review` 关键词；当前未漏测，因为商品审核断言仍在已入 manifest 的 `product-distribution-permission-guard.test.ts` 中。
- P2：共享域子 Agent 观察到 product/integration 直接依赖较宽的 `IInventoryOverviewService`，warehouse 直接编排 `IUpstreamSystemService`；当前是 public service 调用，不构成 P1，但后续可收窄端口。
- P2：若后续要把单菜单 seed 也统一要求 completed assertion，可继续补 `20260605_order_after_sale_menu_seed.sql`、`20260605_source_product_library_menu_component.sql` 等低风险脚本。

## 2026-06-09 P0/P1 快速推进：gpt-5.4 六切片只读复核检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮 6 个子 Agent 全部使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 覆盖切片：seller/buyer 后端账号权限隔离、portal auth/direct-login/token/session/Redis key/401/审计 DTO、SQL guard/seed、React route/access/proxy/request/session/service/JS mirror、共享业务域、verify gate/manifest/Maven reactor。
- 6 个子 Agent 均已完成并关闭。
- 结论：6 个切片均未发现新的可坐实 P0/P1。

### 主线程复核

- `AGENTS.md` 已确认当前子 Agent 默认模型为 `gpt-5.4`。
- 本地主线程静态扫描未发现 seller/buyer 生产代码残留裸 `select*AccountById(accountId)`。
- 本地主线程静态扫描未发现 `RuoYi-Vue/seller` / `RuoYi-Vue/buyer` 生产代码直接依赖 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。
- inventory 主代码当前未命中 `product_*`、`source_warehouse_stock_*`、`upstream_system_*` 直读，旧模块边界 P1 已按前序记录收口。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，manifest check passed。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest,TerminalAccountIsolationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`SqlExecutionGuardContractTest` 77 个测试、`TerminalAccountIsolationTest` 4 个测试通过。
- 子 Agent 补充验证：
  - SQL guard 子 Agent 跑 `SqlExecutionGuardContractTest` 通过。
  - seller/buyer 后端隔离子 Agent 跑 seller/buyer 服务、权限、SQL isolation 相关窄测试通过。
  - React guard/service 子 Agent 跑 5 个前端契约测试，`96 passed / 96 total`。
  - 共享业务域子 Agent 跑 product/inventory/warehouse/integration/finance 边界与单元窄测试通过。
  - verify gate 子 Agent 跑 manifest 与 5 个前端 guard 脚本通过。

### P2 记账项

- 两份读模型重建脚本 `20260607_source_product_read_model.sql` / `20260607_source_warehouse_stock_read_model.sql` 当前已有确认 token、表列预检、scoped delete、事务和 completed assertion；后续可再补 `expected_count` / `expected_signature` 执行前签名锁定。
- `product` 侧仍直接读取 `inventory_overview_spu_read_model` / `inventory_overview_sku_read_model` 取库存汇总字段；当前只读 read model 且有合同 allowlist，不升 P1，后续可改为 inventory-owned query port。
- `integration` 侧 `selectOfficialWarehousesBySourceDimensionGroup(s)` 仍直接 join `warehouse`；当前有合同约束且不影响本轮 P0/P1，后续可迁到 warehouse-owned projection port。
- `finance` 当前缺少同 product/inventory/warehouse/integration 等级的 architecture/boundary contract；后续可补 `FinanceModuleBoundaryContractTest`。
- `verify-three-terminal` 关键测试漏挂检测仍部分依赖文件名/类名正则，sidecar 与业务 mirror guard 仍有手工维护清单；当前未漏挂，不升 P1。
- `react-ui/src/app.tsx` 与 `react-ui/src/requestErrorConfig.ts` 各自维护 portal 401 分流和 direct-login 例外逻辑；当前行为一致，后续可抽共享 helper 防漂移。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮未改业务代码；仅追加本 Markdown 检查点和目标追踪记录。

## 2026-06-09 P0/P1 快速推进：Verifier 全量复核与 SQL Confirm Guard

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 本轮继续使用 6 个 `gpt-5.4` 子 Agent，不使用 GPT-5.3 Codex。
- 覆盖 seller/buyer 账号权限隔离、portal 登录/会话/direct-login/审计、SQL guard、React 三端入口、共享业务域、verify gate 六个切片。
- 6 个子 Agent 均已完成并关闭。

### 采纳的 P1

- `SqlExecutionGuardContractTest` 的 high-impact SQL 自动发现只要求出现 `call assert_*_confirmed()`，在脚本未声明对应确认过程时也可能误判通过。
- 已收口为：确认调用必须引用本脚本内声明的 `assert_*_confirmed` procedure；未声明或调用其他确认过程都 fail-closed。

### 未采纳为 P1 的结论

- 共享业务域子 Agent 把 `product -> inventory_overview_*_read_model` 和 `integration -> warehouse` XML 读取判为 P1；主线程复核后不采纳为当前 P1。
- 原因：`ProductDistributionMapperContractTest` 已用 explicit allowlist 固定 product 只能读取 inventory overview read model，不允许读取 inventory/source/upstream 事实表；同一合同也固定 integration port 链路里当前的官方仓查询形态。
- 这两项仍作为 P2 记账：后续可继续迁成 inventory-owned query port 和 warehouse-owned projection port，但当前不是编译、权限、串端、service/字段缺失阻塞。

### 本轮代码变更

- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 新增 `undeclared_confirm.sql` 负例，防止未声明的 `assert_*_confirmed()` 调用绕过 high-impact 自动发现。
  - `assertConfirmationCallBeforeDml(...)` 在没有本脚本声明的确认过程时直接报错。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - 通过。
  - 前端 guard、React typecheck、Jest 23 suites / 178 tests、后端 reactor test-compile、后端三端合同均通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`SqlExecutionGuardContractTest` 77 个测试通过。
- 主线程静态复核：
  - seller/buyer 生产代码未发现裸 `select*AccountById(accountId)`。
  - `RuoYi-Vue/seller/src/main` / `RuoYi-Vue/buyer/src/main` 未发现 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 直接依赖。
  - direct-login 生产代码未发现旧 `portal_direct_login:{token_hash}` 读取。

### 当前残留项

- P2：`ADMIN/STAFF` 初始角色模板未像 `OWNER` 一样 seed + contract 固定；当前不影响运行时隔离。
- P2：portal 401 分流逻辑在 `app.tsx` 与 `requestErrorConfig.ts` 各维护一份，当前行为一致但后续可抽共享 helper。
- P2：`product -> inventory read model`、`integration -> warehouse` 可后续端口化，但当前已有合同固定边界和允许范围。
- P2：SQL terminal menu seed、sys_menu owner/signature guard 和 dynamic DDL 深度 guard 仍有部分文件级专测，后续可继续提升自动发现覆盖面。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
