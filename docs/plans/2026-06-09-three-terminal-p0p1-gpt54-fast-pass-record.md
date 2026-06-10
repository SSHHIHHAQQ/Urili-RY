# 三端隔离 P0/P1 快速推进记录

时间：2026-06-09 00:08，本机 `Asia/Shanghai`。

## 参考方向

- 主参考：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 执行边界：只处理 P0/P1，即编译、guard、接口、权限、串端、service/字段缺失。
- 跳过项：不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

## 读取口径

- 本文件是追加式快照记录，前文的“当前 P1 / 残留项”只代表当时检查点。
- 读取当前状态时，以文末最新检查点为准；如果后续检查点明确关闭了早期 P1，早期段落不再作为当前残留项引用。
- 截至 2026-06-10 02:04，`warehouse/inventory` 共享业务边界早期 P1 已按后续检查点关闭，不再视为当前开放 P1。
- 截至 2026-06-10 12:24，子 Agent 模型规则已按用户最新要求切换：只读、审查、探索类使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类使用 `gpt-5.4`。早期“默认且只能使用 gpt-5.4”的表述只代表当时历史口径。

## 本轮目标

1. 历史当轮目标：按当时口径固化子 Agent 默认使用 `gpt-5.4`；该口径已被 2026-06-10 12:24 后续检查点覆盖。
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
- 子 Agent 模型：历史当轮口径为默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex；当前模型规则已被 2026-06-10 后续检查点覆盖，读取时以本文件“读取口径”和 AGENTS 为准。

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

- 历史当轮命令 `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test` 当轮复跑通过；当前可复用门禁必须使用 `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
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
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
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

- 历史记录（已追补验证）：本检查点当轮未跑完整 `npm run verify:three-terminal`，只跑了覆盖当轮 P1 的 manifest、Jest、TypeScript 和 Maven 窄验证；由于本检查点修改了 `react-ui/scripts/verify-three-terminal.mjs`，该当轮验证口径不能单独作为 verifier 改动闭环。
- 当前追补口径：后续检查点已多次完整运行 `npm run verify:three-terminal` 并通过；最近一次完整 gate 记录为 2026-06-09 portal sessions 分页合同收窄检查点，前端 24 suites / 193 tests，后端 reactor test-compile 14 个模块 SUCCESS，后端三端合同批次 BUILD SUCCESS，其中 product 63 tests、seller 100 tests、buyer 101 tests 通过。
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
- 历史当轮命令 `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=InventoryAdminRouteContractTest,StandalonePartnerSeedMenuContractTest,TerminalRouteOwnershipTest,SqlExecutionGuardContractTest" test` 当轮通过；当前可复用门禁必须使用 `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=InventoryAdminRouteContractTest,StandalonePartnerSeedMenuContractTest,TerminalRouteOwnershipTest,SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
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

## 2026-06-09 P0/P1 快速推进：远端端内菜单 perms 运行库收敛

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 数据源确认

- 当前后端激活配置仍以 `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` / `application-druid.yml` 和本机 `.env.local` 为准。
- MySQL 目标为远端运行库，地址已脱敏。
- Redis 配置确认仍指向远端 Redis，地址已脱敏；本轮未连接、读取或写入 Redis。
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

- 执行前确认：沿用三端隔离快速推进目标中“远程数据库 DDL/DML 已确认可以执行”的用户确认；本次只执行本检查点列明的端内菜单 seed 收敛，不得作为后续无确认重放依据。
- 使用当前 `.env.local` 激活的远端 MySQL 连接，执行 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 执行变量：
  - `@confirm_seller_buyer_management_seed = 'APPLY_SELLER_BUYER_MANAGEMENT_SEED'`
  - `@seller_buyer_management_seed_profile = 'PATCH_EXISTING'`
- 执行结果：`executed_statements=123`。
- 影响范围：远端 MySQL DDL/DML 收敛 terminal 管理 seed、端内菜单权限约束及相关幂等数据；未触碰 Redis。
- 本轮未执行 `20260604_three_terminal_isolation_migration.sql`，因为当前事实显示账号主体列、菜单区间和自增游标已经满足本次 P1 收敛条件；本次缺口集中在 `seller_buyer_management_seed.sql` 已能补齐的端内菜单非空 `perms` 唯一约束。

### 远端库验证结果

- 目标库：远端运行库，名称已脱敏。
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

- 本轮执行了远端 MySQL DDL/DML，连接来源为 `.env.local`，目标地址已脱敏。
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
  - 现行子 Agent 规则已收紧为默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。

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

- 按用户最新要求，本轮直接使用 6 个 `gpt-5.4` 子 Agent，未使用 GPT-5.3 Codex。
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
- P2：历史 Markdown 中仍会出现 GPT-5.3 相关执行事实，但 AGENTS 和现行目标追踪已明确当前默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。

## 2026-06-09 P0/P1 快速推进：Read Model / Currency / Role Grant SQL Completed Guard

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮直接使用 6 个 `gpt-5.4` 子 Agent，未使用 GPT-5.3 Codex。
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

- `AGENTS.md` 已确认当前子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。
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

## 2026-06-09 P0/P1 快速推进：verify gate P1 补强与 gpt-5.4 六切片复核

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 执行情况

- 本轮 6 个子 Agent 全部使用 `gpt-5.4`，均已完成并关闭。
- 覆盖切片：
  - 管理端 seller/buyer account、role、menu、session、direct-login、force logout、reset password 控制接口。
  - portal auth、direct-login、session、log、self DTO、Redis key 和强退审计。
  - SQL migration、seed、schema guard、menu ID 区间、password schema 和高影响 SQL 门禁。
  - React 管理端 seller/buyer 页面、PartnerManagement 组件和 seller/buyer services。
  - React portal auth、request、401、redirect、direct-login 消费和 token 隔离。
  - verify gate、manifest、frontend/backend contract 覆盖。
- 5 个切片未发现新的可坐实 P0/P1。
- verify gate 切片发现 2 个 P1：
  - `config/proxy.ts` 的关键 `/api/` dev proxy 结构契约未被 gate 固定。
  - 前端关键测试只靠文件名正则兜底，缺少与后端类似的显式关键测试 manifest 清单。

### 采纳并修复的 P1

- `react-ui/scripts/check-portal-token-isolation.mjs`
  - 新增 `config/proxy.ts` 结构断言，固定 dev `'/api/'` 代理入口、`target: apiProxyTarget`、`changeOrigin: true`、`pathRewrite: { '^/api': '' }`。
  - 该 guard 已在 `verify-three-terminal` 的 `guard:portal-token` 步骤中执行。
- `react-ui/tests/three-terminal.manifest.json`
  - 新增 `criticalFrontendExplicitTestPaths`，显式固定 token、portal request、direct-login message、partner audit、remote menu、static route、401 redirect、getRouters authority、auth sidecar、system user service、verify gate 和 permission contract 等平台级关键前端测试。
- `react-ui/scripts/verify-three-terminal.mjs`
  - 读取并校验 `criticalFrontendExplicitTestPaths`。
  - 要求显式关键前端测试必须同时存在于 `frontendTestPaths`。
  - 自动发现前端关键测试时，同时使用显式清单和原有文件名正则，避免关键测试改名或正则遗漏后静默脱离 gate。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests\verify-three-terminal-backend-gate.test.ts tests\admin-auth-sidecar-contract.test.ts tests\system-user-service-contract.test.ts --runInBand`
  - 通过，3 个测试套件、45 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=AdminDirectLoginPermissionContractTest,PortalDirectLoginAuthContractTest,PortalAnonymousEndpointContractTest,PortalSelfServiceSurfaceContractTest,PortalPasswordChangeContractTest,PortalLogAspectContractTest,AdminAccountPermissionUiContractTest,PortalAdminAuditBindingContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,TerminalAccountIsolationTest,TerminalSqlIsolationContractTest,TerminalRoleMenuMapperIsolationContractTest,PortalDirectLoginTicketSqlContractTest,PortalLoginSessionConsistencyContractTest,PortalDirectLoginSupportTest,PortalTokenSupportTest,PortalPreAuthorizeAspectTest,PortalOperLogServiceImplTest,SellerServiceImplTest,SellerPortalDeptServiceImplTest,SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplMenuTreeTest,SellerPortalPermissionServiceImplPortalAccessTest,BuyerServiceImplTest,BuyerPortalDeptServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`ruoyi-system` 67 个测试、seller 90 个测试、buyer 90 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/terminal-session-token.test.ts tests/portal-session-request.test.ts tests/portal-direct-login-message.test.ts tests/partner-audit-modal.test.ts tests/remote-menu-route-guard.test.ts tests/static-route-authority-contract.test.ts tests/portal-unauthorized-redirect.test.ts tests/getrouters-authority-contract.test.ts tests/admin-auth-sidecar-contract.test.ts tests/permission-contract.test.ts --runInBand`
  - 通过，10 个测试套件、114 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - 通过。
  - 5 个前端 guard、React typecheck、Jest 23 个测试套件 / 179 个测试、后端 reactor test-compile、后端三端合同、seller tests 100、buyer tests 101 均通过。
- `git diff --check`
  - 通过；仅提示当前工作区多文件 LF/CRLF 转换警告。
- `codegraph sync .`
  - 已执行，结果为 `Synced 2 changed files`。

### 子 Agent 报告

- `docs/plans/2026-06-09-three-terminal-admin-control-readonly-audit.md`
- `docs/reports/2026-06-09-portal-auth-direct-login-session-log-audit-slice-2.md`
- `docs/reports/2026-06-09-p0p1-audit-sql-migration-seed-guard-slice3.md`
- `docs/reports/2026-06-09-task4-react-partner-management-audit.md`
- `docs/audits/2026-06-09-task5-react-portal-auth-guard-request-readonly-audit.md`
- `docs/audits/2026-06-09-verify-gate-contract-audit-slice-6.md`

### 结论

- 本轮发现并修复 2 个 verify gate 层 P1。
- 修复后未发现新的可坐实 P0/P1。
- 当前仍保持快速推进边界：不做浏览器、截图、DOM 或 UI 细调；P2 不阻塞。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮业务代码未改动；改动集中在 `react-ui` verify/guard/manifest 和 Markdown 记录。
- `react-ui/tests/three-terminal.manifest.json` 当前还包含库存相关既有变更；本轮只新增 `criticalFrontendExplicitTestPaths`。

## 2026-06-09 P0/P1 快速推进：seller/buyer 同构复制一致性复核

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 复核范围

- 后端：`RuoYi-Vue/seller` 与 `RuoYi-Vue/buyer` 的 Controller、Service、Mapper、Mapper XML、端内权限注解、端内日志注解和端内账号/角色/菜单关系。
- 前端：`react-ui/src/pages/Seller`、`react-ui/src/pages/Buyer`、`react-ui/src/pages/PartnerManagement`、`react-ui/src/services/seller`、`react-ui/src/services/buyer` 中已确认的同构管理端 UI 接入、service URL、端类型配置和权限标识。
- 复核方式：按卖家模板归一化为买家后比较接口、权限、service 和页面端配置，只把接口缺失、权限缺失、端前缀错误、service URL 串端、字段缺失作为 P0/P1。

### 结构化抽取结果

- 后端抽取 `@RequestMapping`、HTTP mapping、`@PreAuthorize`、`@PortalPreAuthorize`、`@PortalLog` 后归一化比较：seller/buyer 均为 133 项。
- 后端仅发现 3 项日志标题文案差异，集中在 `BuyerPortalProductDistributionController`：卖家侧为“我的商城商品”，买家侧为“商城商品”。这是端内业务语义差异，不构成 P0/P1。
- 前端 service URL 抽取：seller/buyer 均为 12 项，缺失/额外均为 0。
- 前端页面端配置抽取：seller/buyer 均为 7 项，缺失/额外均为 0。

### 最小验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminAccountPermissionUiContractTest,TerminalRoleMenuMapperIsolationContractTest,TerminalAccountIsolationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，14 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/permission-contract.test.ts tests/partner-audit-modal.test.ts tests/portal-session-request.test.ts --runInBand`
  - 通过，3 个测试套件、35 个测试通过。

### 结论

- 未发现 seller/buyer 机械复制层面的接口、权限、service URL、端配置 P0/P1。
- 本轮没有新增子 Agent；沿用已确认规则，后续如需拆分默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。

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

## 2026-06-09 P0/P1 快速推进：gpt-5.4 六切片收敛复核

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 按用户最新要求，本轮 6 个子 Agent 全部使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 覆盖切片：
  - seller 后端账号、角色、菜单、部门、session、log、direct-login 隔离。
  - buyer 后端账号、角色、菜单、部门、session、log、direct-login 隔离。
  - portal auth、direct-login、token、session、Redis key、401、审计 DTO。
  - SQL guard、seed、DDL/DML 确认门禁。
  - React route、access、proxy、request、session、service、JS mirror。
  - verify gate、manifest、Maven reactor、共享业务域边界。
- 6 个子 Agent 均已完成并关闭。
- 结论：6 个切片均未发现新的可坐实 P0/P1。

### 主线程复核

- `AGENTS.md` 已确认当前子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。
- 主线程静态扫描未发现 seller/buyer 生产代码残留裸 `select*AccountById(accountId)`；当前调用均带 `sellerId/buyerId + accountId` 范围。
- 主线程静态扫描未发现 `RuoYi-Vue/seller/src/main` / `RuoYi-Vue/buyer/src/main` 直接依赖 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。
- 主线程静态扫描未发现 direct-login 生产代码读取旧 `portal_direct_login:{token_hash}` key。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - 通过。
  - 前端 portal token guard、partner management guard、seller/buyer portal product guard、product upstream mirror guard、React typecheck、Jest 23 suites / 178 tests、后端 reactor test-compile、后端三端合同、seller tests 100、buyer tests 101 均通过。
- 子 Agent 补充验证：
  - seller 后端隔离窄测试通过，`ruoyi-system` 9 个测试、`seller` 90 个测试通过。
  - buyer 后端隔离窄测试通过。
  - React guard/service 窄测试通过，包含 portal 401、remote menu route guard、static route authority、sidecar、permission、getRouters authority。
  - verify gate 子 Agent 执行 `node scripts/verify-three-terminal.mjs --check-manifest` 通过。

### 当前残留项

- P2：`react-ui/src/app.tsx` 与 `react-ui/src/requestErrorConfig.ts` 各自维护 portal 401 分流逻辑，当前行为一致，后续可抽共享 helper 防漂移。
- P2：seller/buyer portal 自助日志和会话接口同时标注 `@Anonymous` 与 `@PortalPreAuthorize`，当前仍会走 session + permission 校验，但注解语义可后续收窄。
- P2：`react-ui` 同时存在 `jest.config.ts` 和 `jest.config.js`，裸跑 `npx jest` 会要求显式指定配置；当前 verify gate 已显式使用 `jest.config.ts`。
- P2：部分 SQL 脚本采用内联 post-assert 或列/索引 contract 校验，而不是统一的 `assert_*_completed()` 聚合断言；当前不构成 P0/P1。
- P2：`20260604_three_terminal_legacy_sys_user_account_backfill.sql` 保留受确认门禁保护的 legacy `*_account.user_id -> sys_user` 回填 helper；当前未发现运行态继续复用 `sys_user`。
- P2：`product` / `seller` 模块依赖面仍偏宽，当前已有 boundary contract 禁止直碰 mapper/impl 或外部事实表，后续可继续收窄 public service API。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮未改业务代码；仅追加本 Markdown 检查点和目标追踪记录。

## 2026-06-09 P0/P1 快速推进：远程运行库 schema 只读复核

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 数据源确认

- 激活配置来自 `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 与 `application-druid.yml`，数据库连接通过本机 `.env.local` 的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD` 注入。
- 本轮只确认 JDBC URL 来源和远端运行库性质，地址已脱敏；不输出数据库密码、Redis 密码或 token secret。
- 远程 MySQL 目标：连接来源为 `.env.local` 的 `RUOYI_DB_URL`，目标地址已脱敏。
- 本机无 `mysql` 命令行客户端；使用本地 Maven 缓存 `mysql-connector-j` 通过 JDBC 执行只读查询。

### 执行命令类型

- 只读查询：`information_schema.tables`、`information_schema.columns`、`information_schema.statistics`。
- 只读计数：`seller_*` / `buyer_*` / `portal_direct_login_ticket` 的 `count(*)` 约束核对。
- 未执行 `CREATE` / `ALTER` / `DROP` / `INSERT` / `UPDATE` / `DELETE`，未读取或写入 Redis。

### 运行库 P0/P1 约束结果

- 三端核心表存在：`seller`、`buyer`、`seller_account`、`buyer_account`、`seller_role`、`buyer_role`、`seller_menu`、`buyer_menu`、`seller_dept`、`buyer_dept`、`seller_account_role`、`buyer_account_role`、`seller_role_menu`、`buyer_role_menu`、`seller_login_log`、`buyer_login_log`、`seller_oper_log`、`buyer_oper_log`、`seller_session`、`buyer_session`、`portal_direct_login_ticket` 共 21 张表均存在。
- `seller_account` / `buyer_account` 未发现 legacy `user_id` 列。
- `seller_account.password` / `buyer_account.password` 均为 `varchar(100) not null` 且无默认值；两表空密码行数均为 0。
- `seller_menu.seller_menu_id` 均在 `100000-199999`；`buyer_menu.buyer_menu_id` 均在 `200000-299999`；两端菜单 `auto_increment` 分别满足 `>=100000` / `>=200000`。
- `seller_menu` / `buyer_menu` 均存在 `perms_unique_key` 与非空 `perms` 唯一索引。
- `seller_menu` / `buyer_menu` 的 `C/F` 权限非空、非空权限前缀正确、无 `*:admin:*`、无跨端前缀、无 `*` 通配；页面菜单 `C` 的 `component` 非空。
- `seller_menu` / `buyer_menu` 父级引用无孤儿。
- `seller_role_menu` / `buyer_role_menu` 对菜单和角色均无孤儿。
- `seller_account_role` / `buyer_account_role` 对账号和角色均无孤儿，且未发现账号绑定跨主体角色。
- `portal_direct_login_ticket` 包含 `terminal`、`target_subject_id`、`target_account_id`、`acting_admin_id`、`reason`、`token_hash`、`expire_time`、`used_time`、`used_ip`、`status`、`create_time` 等关键列。
- `seller_oper_log` / `buyer_oper_log` 均包含免密代入结构化审计字段：`direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`。

### 结论

- 未发现远程运行库 schema 层新的可坐实 P0/P1。
- 首次查询时用通用字段名 `menu_id` / `role_id` 核对端内菜单和角色关系，运行库实际字段为 `seller_menu_id` / `buyer_menu_id`、`seller_role_id` / `buyer_role_id`；已读取字段后用真实字段名复跑，相关断言全部通过。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮未改业务代码；仅追加本 Markdown 检查点和目标追踪记录。

## 2026-06-09 P0/P1 快速推进：完成度审计证据链收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 本轮完成度审计使用 6 个 `gpt-5.4` 子 Agent；按最新规则，不使用 GPT-5.3 Codex。
- 覆盖切片：
  - 后端 entity / mapper / service 隔离。
  - Controller / 权限注解 / 管理端控制接口。
  - SQL / schema / seed / guard。
  - React 管理端 seller/buyer 页面和 service。
  - React portal token / request / route / proxy guard。
  - verify gate / manifest / Markdown 记录。
- 6 个子 Agent 均已完成并关闭。

### 审计结论

- 未发现 seller/buyer 账号、密码、角色、菜单、部门、日志、会话独立性上的新增功能性 P0/P1。
- 未发现管理端 seller/buyer controller、权限注解、端账号 role assign、会话列表/强退、免密代入接口的新增 P0/P1。
- 未发现 React 管理端页面、services、portal token、401 分流、remote menu route guard、proxy guard 的新增 P0/P1。
- 未发现 seller/buyer 同构复制层面的 service URL、端配置、权限前缀和接口缺失 P0/P1。

### 已关闭的记录层 P1

- `docs/audits/2026-06-09-verify-gate-contract-audit-slice-6.md` 原先仍把两个已修复 verify gate 缺口写成待处理 P1；本轮已改为“已修复并关闭”，并补入修复后验证证据。
- 补齐 `docs/plans/2026-06-04-three-terminal-isolation-migration-db-execution-record.md`：
  - 该文件为 `20260604_three_terminal_isolation_migration.sql` 远程执行的回溯记录。
  - 明确本轮未重新执行 DDL/DML。
  - 引用既有目标追踪和后续远程只读核验证据。
- 补齐 `docs/plans/2026-06-09-seller-buyer-management-seed-patch-existing-db-execution-record.md`：
  - 该文件为 `seller_buyer_management_seed.sql` 的 `PATCH_EXISTING` 远程执行回溯记录。
  - 明确本轮未重新执行 DDL/DML。
  - 保留历史执行摘要目标显示，并记录后续只读核验目标，避免混淆历史执行和当前配置。

### AGENTS 规则收口

- `AGENTS.md` 已从旧软表述收紧为：
  - 子 Agent 默认且只能使用 `gpt-5.4`。
  - 不得再使用 GPT-5.3 Codex。

### 本轮验证

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，`three-terminal manifest check passed.`
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅提示工作区 LF/CRLF 换行转换 warning。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过；本次输出 `Already up to date`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮改动集中在 `AGENTS.md`、Markdown 审计/执行记录和目标追踪记录；未改业务代码。

## 2026-06-09 P0/P1 快速推进：verify gate 冷启动稳定性修复

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 本轮使用 6 个 `gpt-5.4` 子 Agent，均已完成并关闭。
- 后端、管理端控制权、SQL、React 管理端、React portal/token 5 个切片均未发现新的可坐实 P0/P1。
- verify gate 切片发现 1 个 P1：`verify:three-terminal` 冷启动可能因 `react-ui/src/.umi-test/exports.ts` 未生成而在 Jest 阶段失败。

### 采纳并修复的 P1

- 新增 `react-ui/scripts/prepare-umi-test.mjs`。
  - 显式设置 `NODE_ENV=test`。
  - 动态导入 `@umijs/max/test.js`。
  - 调用 `configUmiAlias(...)` 触发 Umi test 临时文件生成。
  - 断言 `src/.umi-test/exports.ts` 已存在。
- 更新 `react-ui/scripts/verify-three-terminal.mjs`。
  - 在 frontend guard 后、typecheck/Jest 前新增 `umi test setup` 步骤。
  - Jest 执行前再次断言 `src/.umi-test/exports.ts` 存在。
- 更新 `react-ui/tests/verify-three-terminal-backend-gate.test.ts`。
  - 增加合同测试固定 Umi test 预热步骤和 `exports.ts` 断言。

### 新增记录

- `docs/plans/2026-06-09-three-terminal-p0p1-verify-cold-start-record.md`
- `docs/reviews/2026-06-09-admin-control-seller-buyer-p0p1-audit.md`
- `docs/audits/2026-06-09-react-ui-portal-token-request-401-direct-login-readonly-audit.md`

### 验证结果

- 删除 `react-ui/src/.umi-test` 后执行 `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 5 个 frontend guard、`umi test setup`、React typecheck、Frontend Jest 23 suites / 180 tests、Backend reactor test-compile、Backend three-terminal contracts、seller tests 100、buyer tests 101 均通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，`three-terminal manifest check passed.`
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests\verify-three-terminal-backend-gate.test.ts --runInBand`
  - 通过，1 suite / 12 tests。
  - 单跑该 suite 结束后有 Jest open handle 提示，退出码为 0；完整 `verify:three-terminal` 未被阻塞。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅提示工作区 LF/CRLF 换行转换 warning。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮改动集中在 React verify gate、合同测试和 Markdown 记录。

## 2026-06-09 P0/P1 快速推进：gpt-5.4 六切片收尾复核与记录层补齐

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 本轮 6 个子 Agent 全部使用 `gpt-5.4`，不使用 GPT-5.3 Codex。
- 覆盖切片：
  - 后端 seller/buyer 账号、角色、菜单、部门、日志、会话和管理端接口隔离。
  - portal auth、direct-login、token/session、Redis key、401 分流和端内自助日志 DTO。
  - SQL schema、seed、guard 和端内菜单 ID/权限约束。
  - React 管理端 seller/buyer 页面、service、权限前缀、路由与同构复制。
  - React portal request、token、session、direct-login 和 JS mirror guard。
  - verify gate、manifest、Markdown 记录和 Maven reactor 口径。
- 6 个子 Agent 均已完成并关闭。
- 代码级结论：6 个切片均未发现新的可坐实 P0/P1。

### 采纳并修复的记录层 P1

- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 顶部现行口径已收紧为“默认且只能使用 `gpt-5.4`；不得再使用 GPT-5.3 Codex”。
- `docs/plans/2026-06-09-three-terminal-admin-control-readonly-audit.md` 补齐后续 P0/P1 复核中的子 Agent 模型、数量、关闭状态和结论采纳。
- `docs/audits/2026-06-09-verify-gate-contract-audit-slice-6.md` 补齐子 Agent 模型、数量、关闭状态和本文件采纳的记录层 P1。
- `docs/plans/2026-06-09-three-terminal-p0p1-verify-cold-start-record.md` 补齐 CodeGraph 同步结果。
- `react-ui/tests/three-terminal.manifest.json` 补入当前已存在的 `ProductReviewMapperContractTest`，避免三端 manifest gate 因关键后端测试未纳管而失败。

### 主线程复核结论

- seller/buyer 生产代码未发现 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 端内账号权限混用。
- seller/buyer 账号查询仍通过 `sellerId/buyerId + accountId` 约束收口，未发现生产代码新增裸 `select*AccountById(accountId)` 单参数入口。
- `PortalDirectLoginSupport` 仍使用 `portal_direct_login:{terminal}:{token_hash}`，票据有效期 30 分钟，消费时校验 terminal、subject、account 和一次性使用。
- seller/buyer 端内角色菜单写入仍有端菜单存在性、菜单 ID 区间、权限前缀、`perms`、`component` 和 role-menu 关系校验。
- React seller/buyer 管理页与 service 未发现端配置、权限前缀、URL 或字段契约串端 P0/P1。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，`three-terminal manifest check passed.`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductReviewMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，1 test，0 failures，0 errors。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-partner-management-template.mjs`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests\permission-contract.test.ts tests\partner-audit-modal.test.ts tests\verify-three-terminal-backend-gate.test.ts --runInBand`
  - 通过，3 suites / 21 tests。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer,ruoyi-system -am "-Dtest=TerminalAccountIsolationTest,TerminalSqlIsolationContractTest,PortalDirectLoginTicketSqlContractTest,AdminDirectLoginPermissionContractTest,PortalAdminAuditBindingContractTest,SellerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，ruoyi-system 22 tests，seller 6 tests，buyer 6 tests。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅提示工作区 LF/CRLF 换行转换 warning。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过；收口同步输出过 `Synced 4 changed files`，最终再次复核输出 `Already up to date`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮只修记录层 P1；未新增业务代码修复。

## 2026-06-09 P0/P1 快速推进：gpt-5.4 六切片再审与完整 Gate 复核

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 本轮 6 个子 Agent 全部使用 `gpt-5.4`，不使用 GPT-5.3 Codex。
- 6 个子 Agent 均已完成并关闭。
- 切片覆盖：
  - 后端 seller/buyer 账号权限隔离。
  - portal auth、direct-login、session、log 和自助 DTO。
  - SQL schema、seed、guard 和 manifest 关联合同。
  - React 管理端 seller/buyer 同构页面、service 和权限。
  - React portal request、proxy、access 和 JS mirror。
  - verify gate、manifest、测试覆盖和记录口径。
- 结论：6 个切片均未发现新的可坐实 P0/P1。

### 本轮新增记录

- `docs/reviews/2026-06-09-three-terminal-slice2-portal-auth-direct-login-session-log-readonly-audit-codex.md`
- `docs/reports/2026-06-09-p0p1-audit-react-portal-request-proxy-access-slice5.md`

### 主线程复核结论

- 后端 seller/buyer 账号、角色、菜单、部门、日志、会话仍落在端内表；未发现生产代码新增 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 端内混用。
- 免密登录 Redis key、票据 30 分钟、一次性、terminal/subject/account 校验和跨端失败日志边界未发现新增 P0/P1。
- SQL/schema/seed/guard 在仓库合同层闭合；未执行远端 DDL/DML，本轮不声称新增 live DB 结论。
- React 管理端 seller/buyer 页面、service、权限点、会话列表/强退分权、账号重置密码和免密成功提示未发现新增 P0/P1。
- React portal 401、redirect 白名单、响应体 401 reject、远程菜单空 authority fail-closed 和 JS mirror guard 未发现新增 P0/P1。
- verify gate 完整执行通过，manifest 未发现漏纳管现存关键测试。

### P2 记录

- OWNER 自动建号仍使用 `U12346` 初始默认密码；当前是已确认创建默认密码语义，不属于本轮 resetPwd P0/P1，后续可作为安全硬化单独改造。
- `seller_account_role` / `buyer_account_role`、`seller_role_menu` / `buyer_role_menu` 当前依赖主键、seed/contract/Mapper guard，没有数据库 FK；当前不阻塞，但后续可评估加强。
- `portal.*.web.url` seed 已做到不覆盖已有值，但完成断言只校验 key 存在，不校验 value 是否仍是合法 portal direct-login 地址。
- `buyer` 管理页额外打开 `showRechargePlaceholder`，属于业务占位字段差异，不是同构模板串端 P0/P1。
- `PartnerAccountModal` 的重置密码、复合权限、会话/强退分权目前主要由静态 guard 固定，缺更硬的运行时组件测试。
- `criticalFrontendExplicitTestPaths` 已接入，但现有显式项大多同时命中旧 regex；后续可补一个只靠 explicit 命中的负例合同。
- 前端商品审核断言当前寄存在 `product-distribution-permission-guard.test.ts`；未来拆独立 `product-review*.test.ts` 时需同步 manifest explicit 或 regex。
- `docs/audits/2026-06-09-verify-gate-contract-audit-slice-6.md` 中完整 gate 前端测试数已从旧 `179 tests` 修正为当前 `180 tests`。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 5 个 frontend guard、`umi test setup`、React typecheck、Frontend Jest 23 suites / 180 tests、Backend reactor test-compile、Backend three-terminal contracts、seller tests 100、buyer tests 101 均通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅提示工作区 LF/CRLF 换行转换 warning。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过，输出 `Synced 2 changed files`。
  - 记录回填后再次执行通过，输出 `Synced 1 changed files`。

## 2026-06-09 P0/P1 快速推进：本轮最终收口补充

- 本段用于文件尾部快速定位；详细记录见上方 `商品审核权限 gate 与供货价语义回正` 检查点。
- 子 Agent：本轮 6 个子 Agent 均已完成并关闭，全部使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 最终采纳：只采纳商品审核 2 个 P1，分别是类目 schema 隐藏权限依赖和 `EDIT_PRICE` 供货价语义并回 `SKU_INFO`。
- 最终修复：
  - 审核详情预览增加 `product:categoryAttribute:preview` gate，无权限时不请求类目 schema。
  - `supplyPrice` 不参与 `getSkuInfoChangedFields`，`SKU 资料左右对比` 排除供货价，供货价恢复独立 `SKU 供货价左右对比`。
- 最终验证：
  - `npm run verify:three-terminal`：通过；前端 24 suites / 192 tests，后端 product 53 tests、seller 100 tests、buyer 101 tests。
  - `git diff --check`：通过，仅 LF/CRLF warning。
  - `codegraph sync .`：通过，输出 `Synced 3 changed files`。
- 远端影响：未执行远程 MySQL DDL/DML，未写入 Redis；live schema 检查只读。
- 浏览器、截图、DOM、UI 细调验收按快速推进模式跳过。

## 2026-06-09 P0/P1 快速推进：商品审核权限 gate 与供货价语义回正

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只修 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮使用 6 个子 Agent，全部为 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 6 个子 Agent 均已完成并关闭。
- 切片 A：商品审核 dirty diff 只读审查，发现 2 个 P1，并写入 `docs/reports/2026-06-09-product-review-slice-a-readonly-audit.md`。
- 切片 B：seller/buyer role/menu service 只读审查，未发现 P0/P1，并写入 `docs/reviews/2026-06-09-three-terminal-readonly-audit-slice-b.md`。
- 切片 C：live schema/config 只读审查，未发现 P0/P1；确认当前 live `seller_*` / `buyer_*` / 直登票据关键表与密码列约束满足三端计划。
- 切片 D：verify gate/manifest 只读审查，未发现 P0/P1，并写入 `docs/reviews/2026-06-09-react-ui-three-terminal-slice-d-readonly.md`。
- 切片 E：seller/buyer portal 真实业务接口只读审查，未发现 P0/P1。
- 切片 F：记录层只读审查，未发现 P0/P1。

### 子 Agent 结论采纳

- 采纳切片 A 的 2 个 P1：
  - 商品审核详情预览存在隐藏的 `product:categoryAttribute:preview` 依赖，缺少权限 gate。
  - `EDIT_PRICE` 供货价审核被并回 `SKU_INFO` 语义，导致 review type 与首屏审核重点漂移。
- 其余切片只有 P2 加固建议，本轮不阻塞。

### 已修复

- `react-ui/src/pages/Product/Review/index.tsx`
  - 增加 `canPreviewCategorySchema = access.hasPerms('product:categoryAttribute:preview')`。
  - 打开审核详情预览时把该能力传入 `ProductReviewBusinessPreview`。
- `react-ui/src/pages/Product/Review/components/ProductReviewBusinessPreview.tsx`
  - 增加 `canPreviewCategorySchema` 入参，默认 `false`。
  - 无类目属性预览权限时不再请求 `getCategorySchema`。
  - `getSkuInfoChangedFields` 排除 `supplyPrice`。
  - `SKU 资料左右对比` 的渲染字段集合排除 `supplyPrice`。
  - 恢复独立 `PriceChangeReviewView`，供货价变化继续展示为 `SKU 供货价左右对比`。
- `react-ui/tests/product-distribution-permission-guard.test.ts`
  - 固定审核页必须声明并传递 `canPreviewCategorySchema`。
  - 固定预览组件无权限时不请求类目 schema。
  - 固定供货价不并入 SKU 资料审核，且独立价格审核视图存在。
- `docs/plans/2026-06-08-product-review-implementation-record.md`
  - 追加本轮语义回正记录，明确最新结论覆盖此前“供货价合并回 SKU 信息块”的过期记录。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未写入 Redis。
- live schema 检查只读连接当前激活配置对应 MySQL，未修改数据。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\product-distribution-permission-guard.test.ts --runInBand`
  - 通过，1 suite / 10 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端 24 suites / 192 tests 通过。
  - 后端 reactor test-compile 14 个模块全部 SUCCESS。
  - 后端三端合同 product 53 tests、seller 100 tests、buyer 101 tests 通过。
- 浏览器、截图、DOM、UI 细调验收按当前快速推进模式跳过。

### P2 记录

- 可把 `tests/product-distribution-permission-guard.test.ts` 显式加入 `criticalFrontendExplicitTestPaths`。
- 可在根 `.gitignore` 和 `react-ui/.gitignore` 显式补 `.umi-test` 生成目录。
- 可补 portal query 参数剥离和 seller/buyer distribution query 下推范围的额外架构测试。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮无业务代码 P0/P1 修复；改动集中在 Markdown 记录与已存在 gate 证据收口。

## 2026-06-09 P0/P1 快速推进：gpt-5.4 子 Agent 口径确认与登录态证据复核

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 使用记录

- 本轮没有新增子 Agent。
- 已确认 `AGENTS.md` 当前规则为子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。
- 后续如需继续拆分，只按 `gpt-5.4` 建立子 Agent，并在检查点记录模型、数量、关闭状态和结论采纳。

### 主线程复核结论

- `SellerServiceImpl` / `BuyerServiceImpl` 的普通登录和免密登录均复用端内 `validate*CanLogin(...)`，主体不存在、主体停用、账号停用、账号锁定都会拒绝签发 portal token。
- seller/buyer 账号查询仍通过 `sellerId/buyerId + accountId` 下推到 Mapper；本轮未发现生产代码新增裸 `select*AccountById(accountId)` 单参数入口。
- `PortalTokenSupport` 继续用 `portal_terminal` claim 和 `portal_login_tokens:{terminal}:{tokenId}` 读取会话；`PortalSessionContext.requireSession("seller"/"buyer")` 会拒绝跨端 session。
- `PortalDirectLoginSupport` 当前状态机以一次性票据为准：同端首次提交进入业务 validator 后，无论业务校验成功还是失败，都会删除 Redis payload 并尝试标记 DB ticket 为 `USED`；这与目标追踪早期“不消费失败票据”的记录不同，但已由后续 P1 修复记录更新为当前合同。
- 免密票据 terminal 不匹配仍在载入 ticket 阶段 fail-closed，不消费 ticket、不写外端主体/账号审计字段。
- 本轮未发现新的确定 P0/P1。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer,ruoyi-system -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest,PortalTokenSupportTest,PortalDirectLoginSupportTest,PortalSessionContextTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - ruoyi-system 24 tests，seller 55 tests，buyer 55 tests，均 0 failures / 0 errors。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅提示工作区 LF/CRLF 换行转换 warning。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过，输出 `Already up to date`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮未改业务代码，只补充当前口径和证据记录。

## 2026-06-09 P0/P1 快速推进：gpt-5.4 六切片当前状态再核

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。执行边界仍是快速推进模式：只看 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮 6 个只读检查子 Agent 全部使用 `gpt-5.4`，没有使用 GPT-5.3 Codex。
- `019eab14-13ca-7ea1-ae2e-d7dea6cb8e0c`：后端 seller/buyer 账号权限隔离。
- `019eab14-2807-7e40-976b-6d6cfe8f7bef`：portal auth、direct-login、session、log。
- `019eab14-3c3a-7890-b797-54b417144c6a`：SQL schema、seed、guard。
- `019eab14-506f-7e71-8ef1-e5709132be82`：React 管理端 seller/buyer 页面、service、权限。
- `019eab14-64b0-7af3-93df-0b970927f408`：React portal request、proxy、access、JS mirror。
- `019eab14-78fd-7910-a995-b368c9c18298`：verify gate、manifest、AGENTS 和 Markdown 口径。
- 6 个子 Agent 均已完成并关闭，结论均已由主线程复核；未发现新的可坐实 P0/P1。

### 主线程收敛结论

- 后端 seller/buyer 账号、角色、菜单、部门、日志和会话仍走端内模型，未发现新增 `sys_*` 端内混用或裸 `select*AccountById(accountId)` 单参数入口。
- portal direct-login、401、redirect、token/session、日志 DTO、跨端失败审计当前未发现新增串端 P0/P1。
- SQL/schema/seed/guard 在仓库合同层保持 fail-closed；本轮未执行远端 DDL/DML，也不新增 live DB 结论。
- React 管理端 seller/buyer service、权限点、重置密码、会话列表/强退分权、免密登录成功回传等待未发现新增 P0/P1。
- React portal/proxy/access/JS mirror、远程菜单空 authority fail-closed 和 verify manifest 当前未发现新增 P0/P1。

### P2 记录

- 本轮未读取 live DB，数据库外键、远端实际菜单/角色绑定和环境配置值未作为当前结论。
- high-impact SQL 自动识别可后续从 `@ddl` 扩展到更多 `@dml` / `@sql` 动态变量形态。
- portal 自助日志和会话脱敏当前主要依赖 service DTO 投影，后续可补更硬的序列化边界测试。
- portal 401 处理在 `app.tsx` 和 `requestErrorConfig.ts` 仍有重复逻辑，后续可抽公共 helper。
- 历史记录里保留旧测试数快照，但以后续检查点的最新结果为准。
- `verify:three-terminal` alias 误改的负例合同可后续单独补。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，输出 `three-terminal manifest check passed.`
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`
  - 通过，输出 `Portal token isolation guard passed.`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,PortalDirectLoginAuthContractTest,PortalSelfServiceSurfaceContractTest,PortalDirectLoginSupportTest,PortalTokenSupportTest,SellerServiceImplTest,BuyerServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - ruoyi-system 31 tests，seller 61 tests，buyer 61 tests，均 0 failures / 0 errors。
  - Reactor `ruoyi-common`、`ruoyi-system`、`finance`、`inventory`、`integration`、`warehouse`、`product`、`seller`、`buyer` 均 SUCCESS。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅提示工作区 LF/CRLF 换行转换 warning。
- `Select-String` 行尾空白检查：
  - 通过；两份记录文件没有匹配行尾空白。
- `codegraph sync .`
  - 通过，输出 `Already up to date`。

### 边界说明

- 本轮不执行远程 MySQL DDL/DML。
- 本轮不读取或写入 Redis。
- 本轮不启动或重启后端。
- 本轮不做浏览器、截图、DOM 或 UI 细调验收。

## 2026-06-09 P0/P1 快速推进：gpt-5.4 六切片与记录层 P1 收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。执行边界仍是快速推进模式：只修 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮 6 个子 Agent 全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- `019eab1f-61e0-7d90-9a39-686cb315f1e7`：后端 seller/buyer 端内账号权限隔离。
- `019eab1f-a1a9-7a63-998f-95e2d03bd28b`：管理端控制权接口和权限。
- `019eab20-0ae8-7752-9eaf-c0019a8a0167`：SQL/schema/seed/guard。
- `019eab20-7a70-70e2-9b3d-086fb70725f4`：React 管理端 seller/buyer 管理页和 service。
- `019eab21-094d-7d30-b505-5be682d1bae5`：React portal/token/request/proxy/access/direct-login/401。
- `019eab21-84c2-73a0-a98c-14f4707f11a9`：verify gate、manifest、AGENTS 和 Markdown 记录口径。
- 6 个子 Agent 均已完成并关闭。

### 主线程收敛结论

- 代码级 6 个切片均未发现新的可坐实 P0/P1。
- 完整 `verify:three-terminal` 当前通过，证明当前工作树在快速推进口径下没有暴露编译、guard、权限、串端或关键 service/字段缺失。
- 采纳记录层 P1：部分旧 Markdown 仍把 GPT-5.3 优先、旧 reactor/test discovery、旧 4 个 frontend guard 作为现行口径，可能误导后续 Agent。

### 已修复的记录层 P1

- `docs/plans/2026-06-07-three-terminal-p0p1-verify-manifest-record.md`：补充 GPT-5.3 历史口径已过期，当前只能使用 `gpt-5.4`。
- `docs/plans/2026-06-07-three-terminal-p0p1-verify-reactor-test-compile-narrow-discovery-record.md`：补充 GPT-5.3、固定 `ruoyi-admin` 编译门和 `react-ui/tests` 收窄发现均为历史口径；当前 gate 已动态 reactor 和仓库级发现。
- `docs/plans/2026-06-08-three-terminal-p0p1-source-inventory-warehouse-fact-record.md`：补充 GPT-5.3 尝试为历史事实，非现行规则。
- `docs/plans/2026-06-08-three-terminal-p0p1-verify-backend-module-gate-record.md`：补充当前只能用 `gpt-5.4`，且当前 frontend guard 已是 5 个。
- `docs/plans/2026-06-08-three-terminal-p0p1-verify-script-owner-role-record.md`：补充当前只能用 `gpt-5.4`，且当前 frontend guard 已是 5 个。
- `docs/plans/2026-06-09-three-terminal-p0p1-gpt54-reactor-drift-record.md`：追补 CodeGraph 记录说明。
- `docs/plans/2026-06-08-three-terminal-p0p1-gpt54-doc-contract-followup-record.md`：追补 CodeGraph 记录说明。

### 新增记录

- `docs/reviews/2026-06-09-react-portal-token-request-audit.md`：React portal token/request 只读审计，结论为未发现 P0/P1。

### 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 5 个 frontend guard、`umi test setup`、React typecheck、Frontend Jest 23 suites / 180 tests、Backend reactor test-compile、Backend three-terminal contracts、seller tests 100、buyer tests 101 均通过。
- `git diff --check`
  - 通过；仅提示工作区 LF/CRLF 换行转换 warning。
- 记录文件行尾空白检查：
  - 通过；本轮目标记录和修改的 Markdown 文件没有匹配行尾空白。
- `codegraph sync .`
  - 通过，中间同步输出 `Synced 11 changed files`，其中 Added: 1、Modified: 10。
  - 收尾复核再次执行通过，输出 `Already up to date`。

### 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮不改业务代码；改动集中在 Markdown 记录层 P1 收口。

## 2026-06-09 P0/P1 快速推进：远端 session 免密审计字段补齐

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前用户已明确子 Agent 使用 `gpt-5.4`，不再使用 GPT-5.3 Codex；本检查点未新增子 Agent。

### Live DB 只读核验结论

- 数据源来自 `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`、`application-druid.yml` 和本机 `.env.local`。
- 目标 MySQL 为远端运行库，地址已脱敏。
- Redis 目标为远端 Redis，地址已脱敏；本检查点未读写 Redis。
- 三端核心表存在 `21/21`。
- `seller_account.password` / `buyer_account.password` 均为 `varchar(100) not null`，未发现空白密码行。
- `seller_menu` / `buyer_menu` 的 ID 区间、perms、component、父子关系、role-menu 关联未发现 P0/P1。
- `seller_login_log` / `buyer_login_log`、`seller_oper_log` / `buyer_oper_log` 已有免密代入结构化审计字段。
- P1：远端 `seller_session` / `buyer_session` 缺少 `direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`，而当前 Mapper 已在 session insert/select 使用这些字段。

### 已执行修复

- 执行前确认：沿用三端隔离快速推进目标中“远程数据库 DDL/DML 已确认可以执行”的用户确认；本次只执行 `docs/plans/2026-06-09-three-terminal-live-session-audit-columns-db-fix-record.md` 列明的会话审计字段补齐，不得作为后续无确认重放依据。
- 执行类型：远端 MySQL 受控 DDL。
- 修复范围：仅 `seller_session` / `buyer_session` 追加缺失的 5 个免密代入会话审计字段。
- 影响范围：不执行 DML，不删除行，不更新账号、密码、菜单、角色或业务数据。
- 执行记录：`docs/plans/2026-06-09-three-terminal-live-session-audit-columns-db-fix-record.md`。

执行后只读复核：

```text
CHECK|seller_session_missing_direct_login_audit_columns|-
CHECK|buyer_session_missing_direct_login_audit_columns|-
COL|seller_session|direct_login|tinyint||NO|0
COL|seller_session|direct_login_ticket_id|bigint||YES|null
COL|seller_session|acting_admin_id|bigint||YES|null
COL|seller_session|acting_admin_name|varchar|64|YES|
COL|seller_session|direct_login_reason|varchar|255|YES|
COL|buyer_session|direct_login|tinyint||NO|0
COL|buyer_session|direct_login_ticket_id|bigint||YES|null
COL|buyer_session|acting_admin_id|bigint||YES|null
COL|buyer_session|acting_admin_name|varchar|64|YES|
COL|buyer_session|direct_login_reason|varchar|255|YES|
```

### P2 记录

- 远端 `sys_config` 中 `portal.seller.web.url` / `portal.buyer.web.url` 当前仍是本地验证占位地址 `127.0.0.1:8001`。当前阶段仍在 `react-ui` 验证三端入口，先记录为 P2，不阻塞本轮 P0/P1。

### 收尾验证

- `git diff --check`
  - 通过；仅提示当前工作区已有 LF/CRLF 换行转换 warning。
- 行尾空白检查
  - 通过；本轮新增和修改的三份 Markdown 记录无行尾空白命中。
- `codegraph sync .`
  - 通过，输出 `Already up to date`。
- 浏览器、截图、DOM、UI 细调验收
  - 按当前快速推进模式跳过。

## 2026-06-09 P0/P1 快速推进：gpt-5.4 六切片再核与旧记录口径修正

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前执行边界仍是快速推进模式：只修 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮启动 6 个只读子 Agent，全部显式使用 `gpt-5.4`。
- 6 个子 Agent 均已完成并关闭。
- 覆盖切片：
  - seller/buyer 后端端内账号、角色、菜单、部门权限隔离。
  - direct-login、session、login_log、oper_log、Redis key、wrong-terminal fail-closed。
  - SQL/migration/seed/guard。
  - React portal token/request/proxy/access/direct-login/401。
  - React 管理端 seller/buyer 管理页、service、route、权限按钮。
  - verify gate、manifest、契约测试和 AGENTS/Markdown 记录口径。

### 主线程核验

- `node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，输出 `three-terminal manifest check passed.`。
- `npm run guard:portal-token`
  - 通过，输出 `Portal token isolation guard passed.`。
- `npm run guard:partner-management`
  - 通过，输出 `Partner management template guard passed.`。
- 管理端 seller/buyer Admin Controller 映射权限扫描：
  - 未发现 `Admin*Controller.java` 中 `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` 缺少附近 `@PreAuthorize` 的接口。
- 远端 live DB 只读复核：
  - 数据源来自 `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`、`application-druid.yml` 和本机 `.env.local`。
  - 目标 MySQL 为远端运行库，地址已脱敏。
  - Redis 目标为远端 Redis，地址已脱敏；本检查点未读写 Redis。
  - `seller_session` / `buyer_session` 免密审计字段均已补齐。
  - seller/buyer admin 关键权限菜单未缺失。
  - `portal.seller.web.url` / `portal.buyer.web.url` 仍为本地验证占位地址，继续作为 P2 记录。

Live DB 只读输出摘要：

```text
CHECK|database|<redacted-database>
CHECK|seller_session_missing_direct_login_audit_columns|-
CHECK|buyer_session_missing_direct_login_audit_columns|-
CHECK|missing_admin_permission_menu|-
CHECK|portal_web_url_local_placeholder_count|2
```

### 子 Agent 结论采纳

- 后端账号权限隔离切片：未发现可坐实 P0/P1。
- direct-login/session/log 切片：未发现可坐实 P0/P1；确认新 Redis key 是唯一读取路径，旧 key 只清理。
- SQL/migration/seed/guard 切片：未发现可坐实 P0/P1；确认 session 审计字段 fresh baseline 与主迁移一致。
- React portal/request 切片：未发现可坐实 P0/P1；子 Agent 额外跑 `npx jest --config jest.config.ts --runInBand tests/portal-unauthorized-redirect.test.ts tests/terminal-session-token.test.ts tests/portal-direct-login-message.test.ts tests/portal-session-request.test.ts`，4 suites / 55 tests 通过。
- React 管理端 seller/buyer 切片：未发现可坐实 P0/P1。
- verify gate/记录口径切片：采纳 1 个记录层 P1。

### 已修复的记录层 P1

- `docs/plans/2026-06-08-three-terminal-p0p1-sql-target-transaction-guard-record.md`
  - 问题：旧记录仍把“后续子 Agent 优先 GPT-5.3 Codex，不可用再回退 gpt-5.4”写成规则，容易误导后续执行者。
  - 修复：改为当前追补口径：后续子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex；旧的 GPT-5.3 优先表述已经作废。

### P2 记录

- 远端 `portal.seller.web.url` / `portal.buyer.web.url` 仍是本地验证占位地址 `127.0.0.1:8001`；当前阶段仍在 `react-ui` 验证三端入口，不阻塞本轮 P0/P1。

### 完整验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 5 个 frontend guard 均通过：portal token、partner management、seller portal product、buyer portal product、product upstream mirrors。
  - React typecheck `tsc --noEmit --pretty false` 通过。
  - Frontend Jest：23 suites / 180 tests 通过。
  - Backend reactor test-compile：14 个模块全部 SUCCESS，包含 `ruoyi-admin`。
  - Backend three-terminal contracts：全部 SUCCESS，其中 seller 100 tests、buyer 101 tests 通过。
- 浏览器、截图、DOM、UI 细调验收按当前快速推进模式跳过。

### 收尾检查

- `git diff --check`
  - 通过；仅提示当前工作区已有 LF/CRLF 换行转换 warning。
- 本轮三份记录文件行尾空白检查
  - 通过，无命中。
- `codegraph sync .`
  - 通过，输出 `Already up to date`。

## 2026-06-09 P0/P1 快速推进：gpt-5.4 子 Agent 规则确认与密钥配置占位记录

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本检查点未新增子 Agent。
- 已复核 `AGENTS.md`：后续子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。
- 后续如需继续拆分，仍需在检查点记录实际模型、数量、关闭状态和结论采纳。

### 本轮保留改动

- `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
  - 增加 `urili.secret.encryption-key` 和 `urili.secret.encryption-key-id`。
  - 两个值均绑定环境变量占位：`${URILI_SECRET_ENCRYPTION_KEY:}`、`${URILI_SECRET_ENCRYPTION_KEY_ID:local-v1}`。
  - 未写入任何明文密钥。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\verify-three-terminal-backend-gate.test.ts --runInBand`
  - 通过，1 suite / 20 tests。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，输出 `three-terminal manifest check passed.`。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅提示当前工作区 LF/CRLF 换行转换 warning。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过，输出 `Already up to date`。

## 2026-06-09 P0/P1 快速推进：verify gate 生成目录与 token secret guard 加固

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮启动 6 个只读子 Agent，全部显式使用 `gpt-5.4`。
- 6 个子 Agent 覆盖：
  - seller/buyer portal 接口、登录、direct-login、session、`@PortalPreAuthorize`、`@PortalLog`。
  - 管理端 seller/buyer 账号、角色、部门、菜单、会话、日志、免密票据审计接口。
  - SQL/migration/seed/guard。
  - React 管理端 seller/buyer 页面、services、routes、authority。
  - React portal 登录、direct-login、401、token storage、proxy/js mirror。
  - `verify-three-terminal`、manifest、guard 脚本和 Maven/Jest gate 覆盖。
- 6 个子 Agent 均已完成并关闭。

### 子 Agent 结论采纳

- 采纳 verify gate 切片 2 个 P1：
  - `src/.umi-test` 生成目录未从主 `tsconfig` 排除，后续可能把测试生成物漂移带进主 typecheck。
  - `application.yml` 的认证关键配置 `token.secret` 未被 gate 测试固定为 `${RUOYI_TOKEN_SECRET:}` 环境变量占位。
- 其余 5 个切片未发现新的可坐实 P0/P1。

### 已修复

- `react-ui/tsconfig.json`
  - 将 `src/.umi-test` 加入主 typecheck exclude。
- `react-ui/tests/verify-three-terminal-backend-gate.test.ts`
  - 固定 `src/.umi-test` 必须被 `tsconfig.exclude` 排除。
  - 固定 `.umi-test` 下的错误生成文件不能污染 `npm run tsc`。
  - 固定 `application.yml` 必须保留 `secret: ${RUOYI_TOKEN_SECRET:}`。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\verify-three-terminal-backend-gate.test.ts --runInBand`
  - 通过，1 suite / 20 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 5 个 frontend guard 均通过。
  - React typecheck 通过。
  - Frontend Jest：24 suites / 192 tests 通过。
  - Backend reactor test-compile：14 个模块全部 SUCCESS，包含 `ruoyi-admin`。
  - Backend three-terminal contracts：全部 SUCCESS，其中 product 53 tests、seller 100 tests、buyer 101 tests 通过。
- 浏览器、截图、DOM、UI 细调验收按当前快速推进模式跳过。

### 收尾检查

- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅提示当前工作区 LF/CRLF 换行转换 warning。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过，输出 `Synced 2 changed files`。
  - 记录回填后再次执行通过，输出 `Synced 1 changed files`。

## 2026-06-09 P0/P1 快速推进：六切片复核与旧报告状态收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮使用 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 6 个子 Agent 覆盖管理端 seller/buyer 控制面、portal auth/session/direct-login/log、React 三端前端、SQL/seed/contracts、共享业务模块边界、记录一致性。
- 6 个子 Agent 均已完成并关闭。

### 子 Agent 结论采纳

- A 管理端控制面：未发现当前开放 P0/P1；P2 为 controller 显式测试和少量英文异常文案。
- B portal auth/session/direct-login/log：未发现当前开放 P0/P1。
- C React 三端前端：未发现当前开放 P0/P1；P2 为 `app.tsx` / `requestErrorConfig.ts` 401 分支可继续抽共用 helper。
- D SQL/seed/contracts：未发现当前开放 P0/P1。
- E 共享业务边界：未发现当前开放 P0/P1；此前 warehouse/inventory P1 在当前代码中已不成立。
- F 记录一致性：发现 3 个记录层 P1，即旧只读审查文件仍把已修复问题呈现为当前开放 P1。

### 已修复

- `docs/reports/2026-06-09-product-review-slice-a-readonly-audit.md`
  - 标记商品审核 2 条 P1 为历史发现，已关闭。
- `docs/reviews/2026-06-06-react-ui-three-terminal-p0p1-readonly-audit.md`
  - 标记 `config/routes.js` 陈旧副本和菜单编辑端隔离 guard 不足两项 P1 为历史发现，已关闭。
- `docs/reports/2026-06-07-portal-direct-login-readonly-scan.md`
  - 标记 seller/buyer 自助日志与会话 DTO 泄漏 P1 为历史发现，已由 `PortalOwn*` DTO 和合同测试关闭。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- 子 Agent A：seller/buyer focused Maven 测试通过。
- 子 Agent B：seller/buyer/system/framework focused Maven 测试通过。
- 子 Agent C：manifest check 和 6 个 React Jest suites / 98 tests 通过。
- 子 Agent D：`ruoyi-system` SQL/terminal contract Maven 批次通过。
- 子 Agent E：product/inventory/warehouse/integration/finance 边界合同 Maven 批次通过。
- 主控本轮只修改 Markdown 记录；`cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过，输出 `three-terminal manifest check passed.`。
- `cd E:\Urili-Ruoyi; git diff --check` 通过，仅提示当前工作区 LF/CRLF 换行转换 warning。
- `cd E:\Urili-Ruoyi; codegraph sync .` 通过，输出 `Already up to date`。

## 2026-06-09 P0/P1 快速推进：Product Portal 只读接口与旧记录再收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮使用 6 个子 Agent，全部为 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 切片 A 复核 Maven/module 依赖边界，发现 2 个 P1：seller/buyer/product center 只读链路依赖整块 `IProductDistributionService`；product 写路径仍通过 seller 快照扩展点运行时依赖 seller。
- 切片 B 复核 portal 非管理端业务接口身份来源，未发现前端可注入 `sellerId/buyerId/subjectId/accountId` 的 P0/P1。
- 切片 C 复核 SQL/seed/migration/contracts，未发现当前开放 P0/P1。
- 切片 D 复核 React 管理端 Seller/Buyer 页面和 service，未发现当前开放 P0/P1；该切片新增只读报告 `docs/reports/2026-06-09-readonly-slice-d-partner-management-review.md`。
- 切片 E 复核 verify gate 和 manifest，未发现当前开放 P0/P1。
- 切片 F 复核记录一致性，发现 2 份旧报告仍把已关闭问题呈现为当前开放 P1。

### 已采纳并修复

- 新增 `IProductPortalDistributionService`，只暴露 seller/buyer portal 和 product center 当前需要的商品只读查询。
- 新增 `ProductPortalDistributionServiceImpl`，仅依赖 `ProductDistributionMapper` 承载只读查询，不依赖 `IProductDistributionService`、`ProductSellerLookupService`、`IWarehouseService`、`IFinanceCurrencyService` 或 `IUpstreamSystemService`。
- `SellerPortalProductServiceImpl`、`BuyerPortalProductServiceImpl`、`ProductCenterServiceImpl` 改为注入只读 port，不再注入整块 `IProductDistributionService`。
- `SellerPortalProductServiceImplTest`、`BuyerPortalProductServiceImplTest`、`ProductCenterServiceImplTest` 同步改为只读 port stub。
- `PortalProductEndpointPermissionContractTest` 固定 seller/buyer/product center 必须走只读 port，不能回退到整块分发服务。
- `ProductModuleBoundaryContractTest` 固定只读实现必须保持轻依赖和无写方法。
- `ProductSellerLookupServiceImpl` 对缺失 seller 资料返回 `null`，避免空卖家错误路径 NPE；这不是 product 写侧边界的完整修复。
- `docs/reports/2026-06-09-three-terminal-six-hour-readonly-review.md`、`docs/reviews/2026-06-09-slice-5-verify-three-terminal-readonly-audit.md`、`docs/reviews/2026-06-06-maven-module-dependency-audit.md` 已标注历史 P1 状态，避免把已关闭问题继续当作当前开放 P1。

### 历史未处理 P1（后续已于下一检查点关闭）

- 当时 `product` 写路径仍通过 seller 快照扩展点依赖 seller 资料校验，属于写侧模块边界 P1；本轮只收口只读链路。该问题已在后一检查点“Product 写侧 seller 快照依赖收口”关闭。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer,product,ruoyi-system -am "-Dtest=SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest,ProductCenterServiceImplTest,ProductModuleBoundaryContractTest,PortalProductEndpointPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，reactor 10 个模块 SUCCESS；相关测试 39 个通过，0 failure / 0 error。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，输出 `three-terminal manifest check passed.`。
- `cd E:\Urili-Ruoyi; rg -n "IProductDistributionService" ...seller/buyer/product center target surfaces`
  - 无输出，说明 seller/buyer portal 与 product center 目标面未再命中整块分发服务依赖。

## 2026-06-09 P0/P1 快速推进：Product 写侧 seller 快照依赖收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮使用 6 个子 Agent，全部为 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 切片 A/C 复核 product 写入口链路，确认 `AdminProductDistributionController.add(...)` 是新建商品唯一需要 live seller lookup 的 admin 写入口；草稿更新、编辑提审和审核通过可复用已落库 seller 快照。
- 切片 B/D 复核测试和静态合同缺口，建议固定 product 核心写服务不再依赖 `ProductSellerLookupService`，并保护 seller/buyer portal 只读面。
- 切片 E 复核 `sellerId/sellerNo/sellerName` 字段流转，确认 `product_spu` 和审核 snapshot JSON 具备核心生效字段；`product_review_request` 主表无 `seller_no` 属于后续审计/列表投影缺口，不阻塞本轮 P1。
- 切片 F 复核记录层，提醒只读链路关闭和写侧 P1 状态必须分开记录；本轮实际已关闭写侧 P1，因此按最新实现更新记录。

### 已采纳并修复

- `ProductDistributionServiceImpl`
  - 移除 `ProductSellerLookupService` / `productSellerLookupService` 运行时依赖。
  - 将原 `fillSellerSnapshot(...)` 改为 `normalizeSellerSnapshot(product, current)`。
  - 新建商品要求 admin 写入口先提供 `sellerId/sellerNo/sellerName`，缺失时 fail-closed。
  - 更新、编辑提审和审核生效时复用当前商品已落库 seller 快照，且提交入参如果试图变更 `sellerId`，直接抛出“商品卖家不能在保存时变更”。
- `AdminProductDistributionController`
  - 在 `add(...)` 调用 `insertProduct(...)` 前通过 `ProductSellerLookupService` 装配 seller 快照。
  - seller lookup 只保留在管理端商品新增边缘入口，不进入 product 核心写服务、seller/buyer portal 或 product center 只读面。
- `ProductDistributionServiceImplTest`
  - 固定新建商品缺少已装配 seller 快照时 fail-closed。
  - 固定已有商品保存复用当前 seller 快照。
  - 固定已有商品保存不允许变更 sellerId。
- `ProductModuleBoundaryContractTest`
  - 固定 `ProductDistributionServiceImpl` 不再引用 `ProductSellerLookupService` / `selectSellerSnapshot(...)`。
  - 固定 `ProductSellerLookupService` 只允许出现在 product interface、admin product distribution controller 和 seller adapter。
- `PortalProductEndpointPermissionContractTest`
  - 固定 seller/buyer portal 和 product center 只读 service 不得触碰 `ProductSellerLookupService`。

### 行为口径

- 本轮采用“seller 快照只在商品新建时从 live seller 装配，后续商品更新/审核复用 product 已落库快照”的语义。
- 如果未来业务要求 seller 主档改名后自动刷新商品快照，需要单独设计快照刷新或重算流程；这不再作为当前三端隔离 P1。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer,product,ruoyi-system -am "-Dtest=ProductDistributionServiceImplTest,ProductModuleBoundaryContractTest,PortalProductEndpointPermissionContractTest,SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest,ProductCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，reactor 10 个模块 SUCCESS；相关测试 48 个通过，0 failure / 0 error。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，输出 `three-terminal manifest check passed.`。
- `cd E:\Urili-Ruoyi; rg -n "ProductSellerLookupService|productSellerLookupService|selectSellerSnapshot\(" ...product core/portal/product-center target surfaces`
  - 无输出，说明 product 核心写服务、seller/buyer portal 和 product center 目标面未再命中 seller lookup。

## 2026-06-09 P0/P1 快速推进：gpt-5.4 六切片当前状态再复核与记录层收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮使用并关闭 6 个子 Agent，全部为 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 切片 A：后端管理端 seller/buyer 控制面，未发现当前开放 P0/P1；新增只读记录 `docs/reviews/2026-06-09-three-terminal-slice-a-admin-control-backend-readonly-audit.md`。
- 切片 B：后端 portal auth/session/permission/log、自助日志 DTO 和 subject/account 双约束，未发现当前开放 P0/P1。
- 切片 C：SQL/migration/seed/guard，未发现当前开放 P0/P1。
- 切片 D：React 管理端 seller/buyer 页面、service、routes、authority 和账号/会话权限入口，未发现当前开放 P0/P1。
- 切片 E：React portal 登录、direct-login、token storage、401 redirect、remote menu authority 和 JS mirror，未发现当前开放 P0/P1。
- 切片 F：verify-three-terminal、manifest、guard 和记录一致性，未发现代码级开放 P0/P1；发现记录层 P1，即旧 Markdown 仍有“当前开放”表述。

### 已修复的记录层 P1

- `docs/reviews/2026-06-09-slice-5-verify-three-terminal-readonly-audit.md`
  - 将生成目录污染和 `.gitignore` 噪音两条 P1 改为历史快照口径，明确后续已由 `tsconfig`、`.gitignore` 和 gate 测试关闭。
- `docs/reviews/2026-06-06-react-ui-three-terminal-p0p1-readonly-audit.md`
  - 将 `config/routes.js` 陈旧副本和 `PartnerMenuModal` 端隔离 guard 不足改为历史问题，明确后续已由 re-export、fail-closed 校验和模板 guard 关闭。
- `docs/reviews/2026-06-06-maven-module-dependency-audit.md`
  - 将 product 只读链路和写侧 seller 快照依赖两条 P1 改为当时判断，明确后续已由只读 port 和 admin 写入口 seller 快照装配关闭。
- 本文件中间段的 product 写侧旧 P1 改为历史段，避免读到中段时误判 product 写侧 P1 仍开放。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- 子 Agent A：`mvn -pl seller,buyer,ruoyi-system,ruoyi-framework -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest,PermissionServiceAccountPermissionTest,PortalLoginSessionConsistencyContractTest,TerminalSqlIsolationContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- 子 Agent B：`mvn -pl seller,buyer,ruoyi-system,ruoyi-framework -am "-Dtest=TerminalAccountIsolationTest,TerminalSqlIsolationContractTest,PortalSelfServiceSurfaceContractTest,PortalSessionContextTest,PortalPreAuthorizeAspectTest,PortalLogAspectContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- 子 Agent C：`mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest,TerminalSeedPermissionContractTest,TerminalRoleMenuMapperIsolationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- 子 Agent D：`npm run guard:partner-management` 通过；`npx jest --config jest.config.ts tests/partner-management-contract.test.ts tests/remote-menu-route-guard.test.ts tests/static-route-authority-contract.test.ts --runInBand` 通过，3 suites / 19 tests。
- 子 Agent E：`node scripts/check-portal-token-isolation.mjs` 通过；`npx jest --config jest.config.ts --runInBand tests/terminal-session-token.test.ts tests/portal-session-request.test.ts tests/portal-direct-login-message.test.ts tests/remote-menu-route-guard.test.ts tests/static-route-authority-contract.test.ts tests/portal-unauthorized-redirect.test.ts tests/getrouters-authority-contract.test.ts tests/admin-auth-sidecar-contract.test.ts` 通过，8 suites / 105 tests。
- 子 Agent F：`node scripts/verify-three-terminal.mjs --check-manifest` 通过。
- 主控本轮只修改 Markdown 记录；`node scripts\verify-three-terminal.mjs --check-manifest` 通过，旧开放 P1 关键词回扫无命中，`git diff --check` 通过且仅有 LF/CRLF warning，`codegraph sync .` 通过并输出 `Already up to date`。

## 2026-06-09 P0/P1 快速推进：密钥配置占位与 gpt-5.4 六切片再核

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 当时工作树快照

本段是 20:58 附近的历史快照，已被后续 21:36 / 21:45 检查点覆盖；不要把本段当作当前 worktree 状态。

- `git status --short` 只剩 `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 未暂存改动。
- 本次代码改动只增加：
  - `urili.secret.encryption-key: ${URILI_SECRET_ENCRYPTION_KEY:}`
  - `urili.secret.encryption-key-id: ${URILI_SECRET_ENCRYPTION_KEY_ID:local-v1}`
- `SecretCipherSupport` 已使用上述配置；`react-ui/tests/verify-three-terminal-backend-gate.test.ts` 已把配置存在性纳入 gate。

### 子 Agent 使用记录

- 本轮启动并关闭 6 个只读子 Agent，全部为 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 6 个切片分别覆盖 seller/buyer 后端隔离、portal auth/session/log、SQL guard/seed、React route/access/proxy/request/session/service、product/seller/buyer 商品域、verify gate/manifest。
- 6 个切片均未发现新的可坐实 P0/P1。
- 切片 E 明确指出当前 `git diff --cached` 为空；本轮不能再把上一阶段已提交的 product/seller/buyer 改动当作当前 staged patch。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- 主控：`cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，输出 `three-terminal manifest check passed.`。
- 主控：`cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`
  - 通过，1 suite / 20 tests。
- 主控：`cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SecretCipherSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，但无匹配专项测试；这是新增 `SecretCipherSupportTest` 之前的历史快照，仅作为 reactor 编译链路证据，后续检查点已记录 3 tests 通过。
- 主控：`cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-admin -am "-DskipTests" compile`
  - 通过，14 个模块 SUCCESS。
- 主控：`cd E:\Urili-Ruoyi; git diff --check`
  - 通过，仅有 LF/CRLF warning。
- 子 Agent A/B/C/D/E/F 分别完成后端 Maven、React `tsc`/Jest、manifest 和静态审计切片，均未发现当前 P0/P1；详细命令见各子 Agent 返回记录。

### 当前结论

- `P0` 未发现。
- `P1` 未发现新的可坐实项。
- `P2` 记录但不阻塞：
  - `react-ui` 同时存在 `jest.config.js` 和 `jest.config.ts`，人工跑 `npx jest` 需显式 `--config jest.config.ts`。
  - `seller_menu` / `buyer_menu` 的 `perms/component` 主要由 seed/service/contract fail-closed 保护，不是表结构 `NOT NULL + CHECK` 强约束。
  - 后续若新增关键测试但命名未命中 `criticalFrontendTestPathPattern`，需要主动补 `criticalFrontendExplicitTestPaths`。

## 2026-06-09 P0/P1 快速推进：SecretCipherSupport fail-closed P1 补强

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### P1 修复

- 背景：`application.yml` 已新增 `urili.secret.encryption-key` / `urili.secret.encryption-key-id` 环境变量占位，但后端缺少 `SecretCipherSupport` 专项测试；同时 `encrypt()` 会把 `buildKey()` 的缺 key `ServiceException` 泛化成“凭证加密失败”，不利于 fail-closed 原因定位。
- 修复：
  - `SecretCipherSupport.encrypt(...)` 增加 `catch (ServiceException ex) { throw ex; }`，与 `decrypt(...)` 保持一致。
  - `AGENTS.md` 本机后端启动说明补充 `URILI_*` 运行变量和 `URILI_SECRET_ENCRYPTION_KEY` 敏感信息边界。
  - 新增 `SecretCipherSupportTest`，覆盖加解密 round-trip、缺 key fail-closed，以及字段被注入为空白时 `getEncryptionKeyId()` 回退 `local-v1`；运行配置占位默认是 `URILI_SECRET_ENCRYPTION_KEY_ID=local-v1`。
  - `three-terminal.manifest.json` 显式纳入 `SecretCipherSupportTest`。
  - `verify-three-terminal-backend-gate.test.ts` 增加关键测试清单断言，防止后续移除，并固定 `.env.example` / `AGENTS.md` 中的密钥配置口径。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SecretCipherSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，3 tests，0 failure / 0 error。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，输出 `three-terminal manifest check passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`
  - 通过，1 suite / 21 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过，前端 24 suites / 193 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过，仅有 LF/CRLF warning。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过，输出 `Already up to date`。

### 当前结论

- `P0` 未发现。
- 本轮关闭 1 个 P1 边缘问题：凭证加密配置占位已有后端 fail-closed 行为和总 gate 覆盖。
- 未做浏览器、截图、DOM 或 UI 细调验收。

## 2026-06-09 P0/P1 快速推进：gpt-5.4 六切片复核与现有 guard 复验

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮启动并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 切片 A：seller 后端账号、角色、菜单、部门、登录日志、操作日志、会话隔离，未发现 P0/P1。
- 切片 B：buyer 后端账号、角色、菜单、部门、登录日志、操作日志、会话隔离，未发现 P0/P1。
- 切片 C：portal auth/session/direct-login/401/自助审计 DTO，未发现 P0/P1。
- 切片 D：SQL seed/guard/schema、菜单 ID 区间、perms/component、role-menu 当前端校验，未发现 P0/P1。
- 切片 E：React route/access/proxy/request/service/JS mirror，未发现 P0/P1。
- 切片 F：verify-three-terminal、manifest、contract 覆盖和当前工作树 diff，未发现 P0/P1。

### 主控复核结论

- 子 Agent D 曾把“seller/buyer 生产代码禁止重新引入 `sys_*` 缺少专门静态合同”列为 P2；主控复核后确认该判断不是当前事实：
  - `TerminalAccountIsolationTest.sellerAndBuyerModulesMustNotReuseAdminSysAccountControlPlane()` 已扫描 `seller` / `buyer` 的 `src/main/java` 和 `src/main/resources`。
  - 该合同已禁止 `sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`sys_user_role`、`sys_role_menu`、`SysUser`、`SysRole`、`SysMenu`、`SysDept`、`LoginUser`、`PortalAccountSupport`、`PortalAccountMapper` 等回流。
  - `react-ui/tests/three-terminal.manifest.json` 已包含 `TerminalAccountIsolationTest`。
- 因此本轮没有新增代码修复；继续保留现有 guard，不补重复测试。

### P2 留存

- `react-ui/src/pages/Portal/DirectLogin/index.tsx` 的 5 秒未收到 token 分支只更新本页错误态，管理端 opener 仍等 15 秒 bridge 超时；这是反馈时延问题，不构成串端或泄漏。
- `react-ui/src/app.tsx` 和 `react-ui/src/requestErrorConfig.ts` 各自维护 portal/admin 401 分流逻辑，当前行为一致，但后续有维护漂移风险。
- `react-ui/scripts/check-portal-token-isolation.mjs` 的 runtime JS mirror 检查是显式文件名单，当前名单内已覆盖；未来新增同类入口时需要主动纳入 guard。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，输出 `three-terminal manifest check passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`
  - 通过，1 suite / 21 tests。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SecretCipherSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，3 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`
  - 通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=TerminalAccountIsolationTest,SecretCipherSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，7 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过；前端 24 suites / 193 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS，其中 product 60 tests、seller 100 tests、buyer 101 tests 通过。
- `npm run typecheck` 不是当前 `react-ui/package.json` 中存在的脚本；本轮已按仓库实际脚本改跑 `npm run tsc`。

### 当前结论

- `P0` 未发现。
- `P1` 未发现新的可坐实项。
- 本轮有效推进是：完成 6 个 `gpt-5.4` 切片复核，确认现有 `TerminalAccountIsolationTest` 已覆盖 seller/buyer 生产代码 `sys_*` 防回退，避免重复补测试。
- 未做浏览器、截图、DOM 或 UI 细调验收。

## 2026-06-09 P0/P1 快速推进：商品审核 P1 补洞与记录口径纠偏

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮启动并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 切片 A：product distribution 后端批量 SKU 查询、Mapper、service 和合同测试，未发现 P0/P1；留下 on-sale 批量 SKU 镜像测试 P2。
- 切片 B：管理端商品审核页和 `ProductReviewBusinessPreview`，发现 2 个 P1，均已由主控修复。
- 切片 C：`three-terminal.manifest.json`、`verify-three-terminal-backend-gate.test.ts` 和 product guard，发现详情抽屉 footer 权限断言缺口 P1，已由主控修复。
- 切片 D：`AGENTS.md`、`application.yml`、`SecretCipherSupport`，未坐实三端主线 P1；启动脚本密钥强制校验、decrypt 分支测试和 keyId 默认值统一列为 P2。
- 切片 E：核心三端隔离影响面，未发现 seller/buyer 账号权限串端 P0/P1；留下 product 无 seller scope 批量查询的 portal 误用负向合同 P2。
- 切片 F：Markdown 记录一致性，发现当前记录和工作树脱节、相邻 hardening 被计入三端账号权限完成度的口径问题，本检查点已纠偏。

### P1 修复

- 管理端商品审核详情抽屉提交通过/驳回成功后，现在关闭详情抽屉并清理 `currentReview`，避免旧 `PENDING` 状态下继续展示可点击的二次审核按钮。
- SKU 审核配对键不再使用可变的 `sellerSkuCode` 或数组 `index` 作为 before/after 更新型配对依据；只使用 `skuId`、源侧 group key、`systemSkuCode` 等稳定键，无法稳定配对时使用 before/after 侧独立 unmatched key。
- `product-distribution-permission-guard.test.ts` 追加详情抽屉 footer 权限断言，固定 `hidden={!canRejectProductReview}`、`hidden={!canApproveProductReview}`、`PENDING` 条件和 approve/reject 回调必须同时存在。

### P2 留存

- `selectOnSaleProductList` 的批量 SKU 装载路径缺少与 admin 列表对称的 service 回归测试；当前不阻塞 P0/P1。
- `ProductPortalDistributionServiceImpl` 当前仍走 seller-scoped 单 SPU SKU 查询，未串端；后续可补负向合同，禁止 portal 误切到无 seller scope 的 `selectSkuListBySpuIds` / `selectOnSaleSkuListBySpuIds`。
- 商品审核预览组件当前仍是 admin review 入口；后续可补负向合同，禁止引入 `@/services/seller`、`@/services/buyer`、portal token helper 或 `/api/seller/`、`/api/buyer/`。
- `SecretCipherSupport` 相关启动脚本强制校验、decrypt 异常分支测试和 `encryption-key-id` 默认值统一属于相邻安全/配置 hardening，不计入三端账号权限主线完成度。
- 旧记录中 `supplyPrice` 的历史描述以当前实现和最新 product review 记录为准；不再把旧时间点文字当作当前开放 P1。

### 口径纠偏

- 当前 dirty diff 已不再是“只剩 `application.yml`”状态；补记录前 `git status --short` 为 17 个 tracked 修改和 4 个 untracked 条目。
- product 共享域批量 SKU、审核详情 UI、`SecretCipherSupport` 配置 hardening 都是三端隔离方向的相邻收口，不等同于 seller/buyer 账号、角色、菜单、部门、日志、会话主线完成度增量。
- 本检查点三端独立账号权限框架进度不因这些相邻补洞上调。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`
  - 通过，1 suite / 10 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过；前端 24 suites / 193 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS，其中 product 60 tests、seller 100 tests、buyer 101 tests 通过。

### 当前结论

- `P0` 未发现。
- 当前已坐实的商品审核相关 `P1` 已修复并由 guard/tsc/完整三端 verifier 复验。
- `P2` 已记录但不阻塞当前快速推进。
- 未做浏览器、截图、DOM 或 UI 细调验收。

## 2026-06-09 P0/P1 快速推进：商品审核动作二次 guard 与记录口径 P1 收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮启动并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- product 后端切片：未发现 P0/P1；留下 on-sale 批量 SKU 镜像测试和 portal 性能债 P2。
- 商品审核 UI 切片：发现审核动作只靠按钮隐藏、处理函数缺少二次 guard 的 P1，已由主控修复。
- SecretCipherSupport 切片：未发现 P0/P1；留下 decrypt 分支测试、Base64 key 口径等 P2。
- 核心三端隔离切片：未发现 seller/buyer 账号权限串端 P0/P1。
- verify/manifest 切片：未发现 P0/P1；留下 product guard 显式 critical path 和 test-compile 编译面 P2。
- 记录一致性切片：发现 keyId 默认值记录口径 P1，已由主控修正。

### P1 修复

- `openAction(kind, record)` 现在先调用 `canHandleReviewAction(kind, record)`；无审核单、非 `PENDING`、缺少对应 approve/reject 权限时直接 warning 并 return。
- `submitAction()` 提交前再次调用 `canHandleReviewAction(actionState.kind, actionState.review)`，防止复用调用链绕过按钮可见性。
- 静态 guard 断言已固定 `canHandleReviewAction` 内必须存在 `record.reviewStatus !== 'PENDING'`、`kind === 'APPROVE' && !canApproveProductReview`、`kind === 'REJECT' && !canRejectProductReview`，并固定 `openAction` / `submitAction` 都调用该 guard。
- keyId 默认值 P1 已修正为：`application.yml`、`.env.example`、`SecretCipherSupport`、单测和 verifier 统一使用 `local-v1`，字段被注入为空白时 `getEncryptionKeyId()` 也返回 `local-v1`。

### P2 留存

- `selectOnSaleProductList` 批量 SKU 装载路径缺少对称 service 回归测试。
- seller/buyer portal 读链路尚未复用本轮批量 SKU 性能优化；当前不是串端问题。
- `product-distribution-permission-guard.test.ts` 可显式加入 `criticalFrontendExplicitTestPaths`，当前仍由 critical filename pattern 覆盖。
- `SecretCipherSupport.decrypt()` 缺钥/坏密文回归测试、Base64 key 格式说明属于后续 P2。
- portal/admin 401 helper 双处维护、direct-login 失败反馈时延属于后续 P2。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`
  - 通过，1 suite / 10 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过；前端 24 suites / 193 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS，其中 product 60 tests、seller 100 tests、buyer 101 tests 通过。

### 当前结论

- `P0` 未发现。
- 已坐实的商品审核动作二次 guard P1 和 keyId 记录口径 P1 均已关闭。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-09 P0/P1 快速推进：keyId 默认值统一与历史快照口径收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮启动并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- seller/buyer 后端切片：未发现 P0/P1；P2 为控制器权限契约覆盖可继续加硬。
- portal auth/session/direct-login/401/自助 DTO 切片：未发现 P0/P1，并生成 `docs/reports/2026-06-09-portal-auth-session-direct-login-readonly-review.md` 只读报告。
- product 后端切片：未发现 P0/P1；P2 为 on-sale 批量 SKU 镜像测试和 seller portal 列表性能债。
- 商品审核 UI 切片：未发现新增 P0/P1；P2 为详情抽屉关闭范围、旧视图不可达函数和额外浏览器验证记录口径。
- SecretCipherSupport 切片：发现 `encryption-key-id` 默认值不一致 P1，已由主控修复。
- verify/manifest/Markdown 切片：发现历史快照误写成当前状态、`SecretCipherSupportTest` 历史无匹配验证未标注 P1，已由主控修复。

### P1 修复

- `SecretCipherSupport` 的 keyId 默认值统一为 `local-v1`：
  - `application.yml` 默认占位：`URILI_SECRET_ENCRYPTION_KEY_ID:local-v1`。
  - `.env.example` 示例：`URILI_SECRET_ENCRYPTION_KEY_ID=local-v1`。
  - `SecretCipherSupport` 的 `@Value` 默认值和 `getEncryptionKeyId()` 空白回退均为 `local-v1`。
  - `SecretCipherSupportTest` 和 `verify-three-terminal-backend-gate.test.ts` 同步固定该合同。
- Markdown 历史快照口径收口：
  - 20:58 附近“当前工作树只剩 application.yml”的段落已改为“当时工作树快照”，并明确已被 21:36 / 21:45 后续检查点覆盖。
  - “SecretCipherSupportTest 无匹配专项测试”的验证记录已标注为新增测试前历史快照，后续检查点已记录 3 tests 通过。

### P2 留存

- seller/buyer 控制器权限契约可加入 `TerminalAccountIsolationTest`。
- portal 自助日志查询入参后续可改专用 query DTO。
- portal/admin 401 helper 仍是两处维护。
- product on-sale 批量 SKU 缺镜像 service 单测。
- `product-distribution-permission-guard.test.ts` 可显式加入 `criticalFrontendExplicitTestPaths`。
- `SecretCipherSupport.decrypt()` 缺钥/坏密文回归测试和 Base64 key 格式说明仍是后续 P2。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SecretCipherSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，3 tests。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`
  - 通过，1 suite / 21 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过；前端 24 suites / 193 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS，其中 product 60 tests、seller 100 tests、buyer 101 tests 通过。

### 当前结论

- `P0` 未发现。
- 已坐实的 keyId 默认值 P1、历史快照记录 P1 均已关闭。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-09 P0/P1 快速推进：on-sale 批量 SKU 合同与 stale guard 收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮启动并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- seller/buyer 后端账号、角色、菜单、部门切片：未发现 P0/P1。
- portal 登录、direct-login、token/session、Redis key、401、自助日志 DTO 切片：未发现 P0/P1。
- SQL/迁移/seed/schema guard 切片：未发现 P0/P1。
- 管理端 React 卖家/买家页面和 services 切片：未发现 P0/P1。
- product 当前 dirty diff 切片：未发现新的三端隔离 P0/P1。
- verify/manifest/Markdown 切片：未发现 P0/P1，指出冷启动记录测试计数属于历史快照，已由主控补充说明。

### P1 修复

- `ProductDistributionServiceImplTest` 补 `selectOnSaleProductListLoadsCurrentPageSkusInSingleBatch`，固定 on-sale 商品列表和普通商品列表一样使用当前页 SPU 批量 SKU 查询，不回退逐条 `selectOnSaleSkuListBySpuId`。
- `product-distribution-permission-guard.test.ts` 修正拆组件后的 stale guard 断言：
  - 不再要求 `Product/Review/index.tsx` 保留旧 `renderReviewFocus` 函数。
  - 继续要求审核详情使用 `ProductReviewBusinessPreview` 并传入 `canPreviewCategorySchema`。
  - 将非官方仓发货仓显示逻辑固定在真实实现组件 `ProductReviewBusinessPreview` 的 `shouldShowDeliveryWarehouse` 合同上。
- `docs/plans/2026-06-09-three-terminal-p0p1-verify-cold-start-record.md` 标注冷启动测试数量为历史快照，避免与当前 gate 数量混淆。

### P2 留存

- `seller_menu` / `buyer_menu` 单条菜单查询仍依赖分表和 ID 段 guard，数据库层不是 `NOT NULL/CHECK` 硬约束。
- seller/buyer 测试里模拟 `SysUser` 作为管理端审计 actor，后续可改辅助方法命名或注释，避免误读成 seller/buyer 账号混用。
- `/account/sessions` 前端仍传筛选参数，后端 portal 自助会话接口当前未消费该查询对象。
- portal/admin 401 分流逻辑仍在 `requestErrorConfig.ts` 和 `app.tsx` 双处维护。
- `PartnerDeptModal` / `PartnerRoleModal` / `PartnerMenuModal` 的端内 service 和权限门禁契约覆盖可继续加硬。
- 卖家样板机械同步买家目前人工可见成立，后续可补配置对称性测试。
- 前端 `.js` mirror guard 仍是关键域显式白名单，不是全仓自动发现。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductDistributionServiceImplTest,ProductReviewServiceImplTest,ProductDistributionMapperContractTest,ProductReviewMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，43 tests。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`
  - 通过，1 suite / 10 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过；前端 24 suites / 193 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS，其中 product 62 tests、seller 100 tests、buyer 101 tests 通过。

### 当前结论

- `P0` 未发现。
- 已坐实的 on-sale 批量 SKU 镜像合同缺口和拆组件后 stale guard P1 均已关闭。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-09 P0/P1 快速推进：portal sessions 分页合同收窄与 gpt-5.4 六切片收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1 相关接口、权限、guard、串端和字段缺失；浏览器、截图、DOM 检测和 UI 细调继续跳过。

### 子 Agent 使用记录

- 本轮启动并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- seller/buyer 后端账号、角色、菜单、部门、日志、会话切片：未发现 P0/P1；P2 留存为 controller 权限契约可继续补强。
- portal 自助接口、401、direct-login、DTO 泄露切片：未发现 P0/P1；确认 `/account/sessions` 当前应为仅分页合同。
- SQL/seed/schema guard 切片：未发现 P0/P1；P2 留存为端内菜单表结构仍主要靠 seed/static contract fail-closed。
- React 管理端卖家/买家切片：未发现 P0/P1；P2 留存为 Partner 部门/角色/菜单弹窗契约覆盖不足。
- product dirty diff 切片：未发现 P0/P1；P2 留存为 product review guard 测试偏源码快照、portal product 批量性能债。
- verify/manifest/Markdown 切片：未发现 P0/P1；P2 留存为旧记录里仍有历史测试数量和 GPT-5.3 执行事实，需要读最新检查点判定当前状态。

### 接口合同收敛

- `react-ui/src/services/portal/session.ts` 新增 `PortalSessionPageParams` 和 `sanitizePortalSessionPageParams()`。
- `getPortalSessions()` 及 seller/buyer portal session service 现在只向 `/account/sessions` 传递 `pageNum/pageSize`，不再把 `accountId/ipaddr/status` 等筛选参数传给后端。
- `react-ui/tests/portal-session-request.test.ts` 已固定负向合同：即使调用方误传 `accountId/ipaddr`，最终请求也只保留分页参数。
- 后端 seller/buyer portal controller 当前 `accountSessions()` 不接查询 DTO，仅按当前 portal session 的主体和账号作用域查询会话；本轮未改后端。

### Gate 修复

- 首次重跑 `npm run verify:three-terminal` 时，`product-distribution-permission-guard.test.ts` 仍要求旧源码字符串 `reviewStatus: 'PENDING'`，但当前真实实现已改为 `const pending = record.reviewStatus === 'PENDING';` 并通过 `canHandleReviewAction()` 做二次 guard。
- 本轮只修测试合同，不改业务逻辑：将 stale 断言改为当前列表操作列的 `const pending = record.reviewStatus === 'PENDING';`。
- `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand` 已通过，10 tests。

### P2 留存

- seller/buyer controller 权限契约可以继续补 MockMvc 或窄合同测试。
- `seller_menu` / `buyer_menu` 的 `component/perms` schema 级硬约束偏弱，目前依赖 seed 和静态合同守住。
- Partner 部门/角色/菜单弹窗的 service 与权限门禁契约覆盖不足。
- `product-distribution-permission-guard.test.ts` 已修掉本轮发现的 stale 字符串断言，但整体仍偏源码快照，后续可补更行为化的审核 guard 测试。
- seller/buyer portal 商品列表仍未吃到 admin 批量 SKU 优化，属于性能债。
- `verify-three-terminal` 的 critical frontend path 正则未显式覆盖 `product-review` 关键词，但当前 manifest 和已有 guard 测试仍覆盖现有关键文件。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/portal-session-request.test.ts --runInBand`
  - 通过，1 suite / 26 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`
  - 通过，1 suite / 10 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过；前端 24 suites / 193 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS，其中 product 63 tests、seller 100 tests、buyer 101 tests 通过。
- 子 Agent 额外验证记录：
  - portal 切片：`portal-session-request.test.ts` + `portal-unauthorized-redirect.test.ts` 45 tests 通过；seller/buyer/system 后端窄测通过。
  - product 切片：product Maven 窄测、`npm run tsc -- --pretty false`、product/verify/portal 前端窄测通过。

### 当前结论

- `P0` 未发现。
- 本轮未发现新的开放 P1；`/account/sessions` 从“疑似前后端筛选语义漂移”收敛为明确的仅分页接口合同。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-09 P0/P1 快速推进：gpt-5.4 六切片记录口径 P1 收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1 相关编译、guard、接口、权限、串端、service/字段缺失和会误导后续执行的验证记录口径；浏览器、截图、DOM 检测和 UI 细调继续跳过。

### 子 Agent 使用记录

- 本轮启动并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- seller/buyer 后端 controller 权限切片：未发现 P0/P1。
- portal auth/session/direct-login/401/自助 DTO 切片：未发现 P0/P1；生成只读报告 `docs/reviews/2026-06-09-portal-auth-session-direct-login-readonly-review-codex.md`。
- SQL/seed/schema/verify gate 切片：未发现 P0/P1；P2 留存为 SQL/隔离合同未全部列入 explicit critical 和 SQL seed 末尾运行时完整性复验偏弱。
- React 管理端卖家/买家切片：未发现 P0/P1，也未发现新增 P2。
- product dirty diff 切片：未发现 P0/P1；P2 留存为 `ProductReviewMapper` 新 statement 结构合同不足和 seller portal 商品列表性能债。
- verify/Markdown 记录切片：发现并关闭 2 个记录层 P1。

### P1 修复

- 修正 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 中 seller 窄测命令口径：
  - 不带 `-am` 的 `mvn -pl seller "-Dtest=SellerPortalProductServiceImplTest" test` 保留为历史复现/本机缓存预热后的事实。
  - 明确该命令不得作为当前可复用门禁。
  - 当前可复用命令必须带 `-am`：`mvn -pl seller -am "-Dtest=SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- 修正本文件早期检查点中 verifier 改动后的验证口径：
  - 该检查点当轮只跑 `--check-manifest` 和窄测，不能单独作为 `react-ui/scripts/verify-three-terminal.mjs` 改动闭环。
  - 已补充后续完整 `npm run verify:three-terminal` 通过记录，作为当前闭环证据。

### P2 留存

- 旧追加式记录中仍保留 GPT-5.3 历史执行事实；当前规则以 AGENTS 和最新检查点为准：子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。
- `verify-three-terminal` 的前端 critical 正则未显式包含 `product-review` 关键词；当前 manifest 和现有测试仍覆盖现有关键文件。
- admin 401 分流在 `app.tsx` 和 `requestErrorConfig.ts` 双处维护，远程菜单缓存清理口径存在轻微漂移。
- SQL/隔离关键测试主要靠类名/路径正则兜底，explicit critical 清单偏窄。
- `ProductReviewMapper` 新增 SQL statement 可补结构性合同。
- seller portal 商品列表仍有性能债。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，10 tests；reactor 带起 `ruoyi-common`、`ruoyi-system`、`finance`、`inventory`、`integration`、`warehouse`、`product`、`seller`。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过；前端 24 suites / 193 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS，其中 product 63 tests、seller 100 tests、buyer 101 tests 通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅 LF/CRLF warning，无空白错误。

### 当前结论

- `P0` 未发现。
- 已坐实的 2 个记录层 P1 已关闭。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-09 P0/P1 快速推进：portal sessions 类型收窄与旧命令口径追补

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续只修 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent 使用记录

- 本轮启动并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 后端 seller/buyer、portal auth/session/direct-login、SQL/seed、React route/access/service、product 相邻脏改动、verify/Markdown 六个切片均未坐实新的代码/SQL P0/P1。
- 采纳记录层 P1：旧 `npm run test:unit -- --runTestsByPath ...` 和不带 `-am` 的 product/seller 窄测仍可能被误读为当前门禁。
- 未采纳 product 切片报告的 guard 失败为现态 P1：主工作区复跑 `product-distribution-permission-guard.test.ts` 和完整 `verify-three-terminal` 均通过。

### 已完成

- `react-ui/src/services/portal/session.ts` 收窄 portal 自助会话列表参数类型，只允许 `pageNum/pageSize`。
- `react-ui/tests/portal-session-request.test.ts` 保留恶意跨账号参数回归，并用 `as any` 表示这不是正常类型合同。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 顶部现行口径增加旧命令全局过期规则，并补点名旧命令检查点的历史过期标记。

### P2 记录

- admin 侧会话 service 参数面仍偏宽，现有调用安全但后续可收窄。
- `ProductReviewMapper` 新 SQL 结构合同可增强。
- `product-review` 命名未显式进入 frontend critical 正则。
- admin/portal 401 helper 双处维护。
- GPT-5.3 历史事实 grep 噪音仍高，但现行规则已明确只能使用 `gpt-5.4`。

### 验证

- `npx jest --config jest.config.ts tests/portal-session-request.test.ts --runInBand`：通过，1 suite / 26 tests。
- `npm run tsc`：通过。
- `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`：通过，1 suite / 10 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npm run verify:three-terminal`：通过，前端 24 suites / 193 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS，其中 product 64 tests、seller 100 tests、buyer 101 tests。
- 未执行远程 MySQL DDL/DML，未读取或写入 Redis，未启动或重启后端。

### 当前结论

- `P0` 未发现。
- 已坐实的记录层 P1 已关闭。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-09 P0/P1 快速推进：product-review critical manifest 盲区收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续只修 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 已完成

- `react-ui/scripts/verify-three-terminal.mjs` 的 frontend critical 自动发现正则加入 `product-review`。
- `react-ui/tests/verify-three-terminal-backend-gate.test.ts` 新增 `product-review-drift.test.ts` 临时文件负例，确认新 product review guard 测试漏登记 manifest 会 fail-closed。
- 本轮未启动子 Agent；后续如需拆分仍默认且只能使用 `gpt-5.4`。

### 远端影响

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。

### 验证

- `npx jest --config jest.config.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`：通过，1 suite / 22 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npm run verify:three-terminal`：通过，前端 24 suites / 194 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS，其中 product 64 tests、seller 100 tests、buyer 101 tests。

### 当前结论

- `P0` 未发现。
- `product-review` critical 命名盲区已关闭。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-09 P0/P1 快速推进：admin session 参数面收窄与记录层 P1 再收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续只修 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent 使用记录

- 本轮启动并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- seller/buyer 后端、portal auth/session/direct-login/log、SQL/seed/guard/schema、React 管理端 seller/buyer、React portal/token/request/route 五个代码切片均未坐实新的 P0/P1。
- verify/manifest/Markdown 切片发现并关闭 2 个记录层 P1：旧不带 `-am` 的 `SqlExecutionGuardContractTest` Maven 命令，以及旧 4 个 frontend guard / 前端关键目录收窄发现口径。

### 已完成

- 新增 `react-ui/src/services/seller-buyer/sessionParams.ts`，统一过滤 admin seller/buyer session list 分页参数。
- `react-ui/src/services/seller/seller.ts` 与 `react-ui/src/services/buyer/buyer.ts` 的 `getAdmin*Sessions` / `getAdmin*AccountSessions` 已从 `Record<string, any>` 收窄为 `API.Partner.PartnerSessionPageParams`，并只转发 `pageNum/pageSize`。
- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx` 和 `react-ui/src/types/seller-buyer/party.d.ts` 同步收窄类型。
- `react-ui/tests/partner-management-contract.test.ts` 固定 admin session list 参数合同，并补运行时回归，证明调用方强行传入 `subjectId/accountId/ipaddr/tokenId` 时，请求仍只携带 `pageNum/pageSize`。
- `docs/architecture/reuse-ledger.md` 登记 Partner Admin Session 分页参数过滤模板。

### 远端影响

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。

### 验证

- `npx jest --config jest.config.ts tests/partner-management-contract.test.ts --runInBand`：通过，1 suite / 6 tests。
- `npx tsc --noEmit`：通过。
- `npm run verify:three-terminal`：通过，前端 24 suites / 196 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS，其中 product 64 tests、seller 100 tests、buyer 101 tests。

### 当前结论

- `P0` 未发现。
- admin 侧会话 service 参数面偏宽已关闭。
- 已坐实的 2 个记录层 P1 已关闭。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-09 P0/P1 快速推进：操作日志凭证脱敏与远端记录脱敏收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续只修 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent 使用记录

- 本轮共使用并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 切片 A：seller/buyer admin session 参数合同未发现 P0/P1。
- 切片 B：product review/product distribution 后端 dirty diff 未发现 P0/P1，P2 留存为 `/pending-counts` 后续可补 controller 权限契约测试。
- 切片 C：product review 前端 dirty diff 未发现 P0/P1，P2 留存为商品审核查询类型收窄和 critical 显式清单补强。
- 切片 D：坐实 P0 管理端操作日志可能记录外部系统凭证明文；坐实 P1 密钥缺失只在使用时 fail-closed，启动期缺少明确提示。
- 切片 E：verify/manifest 未发现 P0/P1，P2 留存为 critical 显式清单仍部分依赖命名/路径启发式。
- 切片 F：坐实记录层 P1，远端连接信息明文写入多份 Markdown，部分远端 DDL/DML 执行记录缺少确认链。

### 已完成

- `LogAspect` / `PortalLogAspect` 的敏感字段名单新增 `appKey`、`appSecret`、`credential`、`credentialCiphertext`、`appKeyCiphertext`、`appSecretCiphertext`。
- `LogAspect` 补齐 request 参数 map 过滤，管理端 query/form 参数与 request body 走同一敏感字段剔除逻辑。
- `AdminUpstreamSystemController` 的上游系统接入、重新授权接口，以及 `AdminCurrencyController` 的币种汇率同步设置、测试连接接口，均在 `@Log.excludeParamNames` 显式排除凭证字段。
- `LogAspectSensitiveFieldFilterTest` 补合同，固定管理端和端内日志脱敏名单对齐，并验证 request 参数 map 会剔除凭证字段。
- `SecretCipherSupport` 启动期在缺少 `URILI_SECRET_ENCRYPTION_KEY` 时输出明确 warning，不输出密钥值；保持当前验证环境启动行为不变，首次使用仍 fail-closed。
- Markdown 记录中远端 MySQL / Redis 主机、端口和库名已统一改为地址脱敏，保留连接来源、远端性质、执行类型、是否读写 Redis 等审计事实。
- 远端 DDL/DML 记录补执行前确认链，明确只能用于本次列明范围，不能作为后续无确认重放依据。
- `docs/architecture/reuse-ledger.md` 登记管理端与端内操作日志凭证字段脱敏模板。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 本检查点未输出远端数据库、Redis、token secret 或明文密钥。

### 验证

- `mvn -pl ruoyi-framework,ruoyi-system,finance,integration -am "-Dtest=LogAspectSensitiveFieldFilterTest,SecretCipherSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，7 个 reactor 模块 SUCCESS；`LogAspectSensitiveFieldFilterTest` 5 tests、`SecretCipherSupportTest` 3 tests 通过。
- `npm run guard:partner-management`：通过。
- `npm run verify:three-terminal`：通过，前端 24 suites / 196 tests；后端 reactor test-compile 14 个模块 SUCCESS；后端三端合同批次 BUILD SUCCESS，其中 product 64 tests、seller 100 tests、buyer 101 tests。
- 远端地址明文特征回扫：无命中；具体匹配串不写入文档，避免记录本身重新暴露地址特征。
- 子 Agent 模型弱口径回扫：`当前子 Agent 默认模型为` / `默认模型为 gpt-5.4` / `默认使用 gpt-5.4` 在 AGENTS、目标追踪和快速推进记录中无命中。

### 当前结论

- 已坐实 P0：操作日志凭证明文落库风险已关闭。
- 已坐实 P1：密钥缺失启动期无提示已用 warning 收口；远端连接信息文档明文和远端 DDL/DML 确认链缺口已关闭。
- 未处理 P2：密钥缺失按存量密文/任务启用状态做条件 fail-fast、critical explicit 清单补强、product review 查询类型收窄、`/pending-counts` controller 权限契约测试。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-09 检查点：gpt-5.4 子 Agent 口径确认与文档 P1 收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。用户已再次明确：子 Agent 使用 `gpt-5.4`，不要再使用 GPT-5.3 Codex；本轮启动并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。

### 子 Agent 结论处理

- seller/buyer/ruoyi-system/SQL 核心隔离切片：未发现 P0/P1；裸 accountId 查询、terminal scoped direct-login key、端内权限合同仍保持收敛。
- React seller/buyer admin 与 portal 切片：未发现 P0/P1；会话与强退权限分离、portal 401 按端清 token、session 参数只放行分页参数。
- product/inventory/integration/finance dirty diff 切片：未发现 P0/P1；P2 留存 `on-sale` 最新审核汇总语义和 `/pending-counts` 后端契约测试。
- verify/manifest 切片：未发现代码 P0；坐实交付风险，当前 manifest/service/import 已依赖若干未跟踪文件，后续提交必须一并纳入变更集，不能漏掉。
- docs/AGENTS 切片：坐实记录层 P1，部分新增记录仍把不带 `-am` 的 Maven 命令写成可复用验证命令，且部分远端 DDL/DML 记录确认链不完整。
- framework/system/finance/integration 安全切片：未发现 P0/P1；凭证响应模型已有 `@JsonIgnore` / `WRITE_ONLY`，剩余序列化负向合同属于 P2。

### 已完成

- 确认 `AGENTS.md` 当前规则已经是子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex；本轮执行也按该规则完成。
- 将新增检查点中的不带 `-am` Maven 验证命令改为“历史当轮命令 + 当前 reactor-safe 可复用门禁”的口径，避免后续照抄旧命令。
- 补齐 3 份远端 DDL/DML 执行记录的用户确认来源、确认 token 或缺失说明、影响范围、回滚方式，并进一步脱敏远端运行库名称。
- 复核 finance/integration 凭证响应字段：`credentialCiphertext`、`appKeyCiphertext`、`appSecretCiphertext` 已用 Jackson 注解禁止响应序列化。

### 当前必须随变更集保留的新增文件

- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/SecretCipherSupportTest.java`
- `react-ui/src/services/seller-buyer/sessionParams.ts`
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/ProductReviewListDisplayItem.java`
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/ProductReviewTypeCount.java`

这些文件已被 manifest、service 或 product 代码引用。2026-06-10 00:14 已将以上 4 个文件执行 `git add -- ...` 暂存，关闭“本地存在但提交/CI 漏文件”的 P1 交付风险；未暂存其他大量改动或原型文件。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 本检查点未输出远端数据库、Redis、token secret 或明文密钥。

### 验证

- 远端地址明文特征回扫：无命中；具体匹配串不写入文档，避免记录本身重新暴露地址特征。
- 旧 Maven 命令口径回扫：最新检查点已改为“历史当轮命令 + 当前 `-am` reactor-safe 门禁”；追加式早期历史记录仍有旧命令，按文件顶部过期口径处理，不作为当前门禁。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi; git diff --cached --check`：通过，无空白错误。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,product,seller,buyer -am "-Dtest=SecretCipherSupportTest,ProductReviewServiceImplTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；10 个 reactor 模块 BUILD SUCCESS，`SecretCipherSupportTest` 3 tests、`ProductReviewServiceImplTest` 19 tests、`SellerServiceImplTest` 55 tests、`BuyerServiceImplTest` 55 tests。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅输出当前工作区已有 LF/CRLF 换行转换 warning，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

### 当前结论

- `P0` 未见新增。
- 已坐实的记录层 P1 已补口径：旧 Maven 命令不再作为当前门禁，远端 DDL/DML 执行记录确认链已补。
- 已坐实的交付层 P1 已收口：4 个被代码或 manifest 依赖的新增文件已暂存，避免提交时漏带。
- 未处理 P2：`seller-buyer/sessionParams.ts` 可补 `.js` sidecar、`SecretCipherSupport` decrypt 负向合同、finance/integration 响应序列化合同、product `on-sale` 最新审核汇总语义、`pending-counts` 后端契约测试、原型文件是否纳入版本控制。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-10 检查点：关键新增文件纳入变更集

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍是快速推进模式：只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮启动 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 已回收切片：verify/manifest 切片坐实 3 组 P1 交付风险；framework/system/finance/integration 安全切片未发现 P0/P1；seller/buyer/ruoyi-system 后端核心切片未发现 P0/P1；product dirty diff 切片未发现 P0/P1。
- 6 个子 Agent 已全部回收并关闭；未保留后台子 Agent。

### 已完成

- 对 4 个已被当前代码或 manifest 依赖的新增文件执行 `git add -- ...`，只暂存这些关键新增文件：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/SecretCipherSupportTest.java`
  - `react-ui/src/services/seller-buyer/sessionParams.ts`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/ProductReviewListDisplayItem.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/ProductReviewTypeCount.java`
- 未暂存 `react-ui/public/prototypes/`、新增报告草稿或其他大量修改。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 验证

- `cd E:\Urili-Ruoyi; git status --short -- <4 个关键新增文件>`：4 个文件均为 `A`。
- `cd E:\Urili-Ruoyi; git diff --cached --check`：通过，无空白错误。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,product,seller,buyer -am "-Dtest=SecretCipherSupportTest,ProductReviewServiceImplTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；10 个 reactor 模块 BUILD SUCCESS。

### 当前结论

- `P0` 未见新增。
- 已坐实交付层 P1 已关闭：关键新增文件不再处于未跟踪状态。
- P2 留存为 sidecar、序列化/解密负向合同、product portal N+1 性能债、product 审核读模型契约和原型文件归属。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-10 检查点：子 Agent 全量回收与文档 P1 再收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。用户再次明确：子 Agent 使用 `gpt-5.4`，不要用 GPT-5.3 Codex；本轮未再启动新子 Agent，前序 6 个 `gpt-5.4` 只读子 Agent 已全部关闭。

### 子 Agent 结论处理

- seller/buyer/ruoyi-system 后端核心、React 管理端与 portal、product dirty diff、framework/system/finance/integration 安全切片均未发现新的 P0/P1。
- verify/manifest 切片坐实的 4 个关键新增文件未跟踪交付 P1 已关闭：这些文件已暂存为 `A`。
- docs/AGENTS 切片坐实的记录层 P1 已关闭：被点名记录中的远端运行库名已脱敏，库存调整审核 SQL 执行记录已补 `@confirm_inventory_adjustment_review` 以及 menu/job count/signature 确认变量。

### 已完成

- `docs/reviews/2026-06-03-current-naming-residual-audit.md`：远端目标库名和示例命令中的 `--database` 已脱敏。
- `docs/plans/2026-06-08-product-review-sql-execution-record.md`：验证结果中的当前 database 已改为远端运行库名称脱敏。
- `docs/plans/2026-06-09-inventory-adjustment-review-sql-execution-record.md`：数据源确认和执行前预览签名中的目标库名已脱敏，并新增 SQL 确认 token 小节。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 与本记录已把“剩余子 Agent 待回收”的临时状态改为全部关闭。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 本检查点未输出远端数据库、Redis、token secret 或明文密钥。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,product,seller,buyer -am "-Dtest=SecretCipherSupportTest,ProductReviewServiceImplTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；10 个 reactor 模块 BUILD SUCCESS，`SecretCipherSupportTest` 3 tests、`ProductReviewServiceImplTest` 19 tests、`SellerServiceImplTest` 55 tests、`BuyerServiceImplTest` 55 tests。

### 当前结论

- `P0` 未见新增。
- 已坐实交付层 P1 和记录层 P1 均已关闭。
- P2 留存为 README 本地隔离示例口径、Jest 双配置歧义、`sessionParams.ts` sidecar、序列化/解密负向合同、integration 响应日志脱敏范围、`credentialKeyId` 信息最小化、product portal N+1 性能债、product 审核读模型契约和原型文件归属。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-10 检查点：记录层 P1 二次收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍保持快速推进模式，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮启动并关闭 6 个只读子 Agent，全部显式使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- seller/buyer/ruoyi-system/ruoyi-framework 后端核心切片：未发现新的 P0/P1。
- React 管理端与 portal 串端切片：未发现新的 P0/P1。
- SQL/seed/菜单权限 guard 切片：未发现新的 P0/P1。
- product/finance/integration dirty diff 切片：未发现新的 P0/P1。
- verify/manifest 交付切片：未发现新的 P0/P1；P2 为未跟踪原型目录和 `sessionParams.ts` 无 `.js` sidecar。
- docs/AGENTS 切片：坐实记录层 P1，并已采纳修复。

### 已完成

- `docs/plans/2026-06-08-three-terminal-p0p1-product-review-price-sql-guard-record.md`：旧 GPT-5.3 Codex 尝试改为历史过期口径，并补当前 `gpt-5.4` 规则说明；`ruoyi-system` 验证命令改为 `-am` reactor-safe 口径。
- `docs/plans/2026-06-07-three-terminal-p0p1-source-product-read-model-sql-contract-record.md`：旧 `gpt-5.3-codex-spark` 回退说明改为历史过期口径；`ruoyi-system` 验证命令改为 `-am` reactor-safe 口径。
- `docs/reviews/2026-06-06-seller-buyer-admin-portal-p0p1-audit.md`：历史 seller/buyer 单模块编译命令标为当轮事实，并补当前可复用 `mvn -pl seller,buyer -am -DskipTests compile -q`。
- `docs/plans/2026-06-06-three-terminal-p0p1-direct-login-guard-followup-record.md`：`ruoyi-system` Maven 验证命令补 `-am`。
- `docs/plans/2026-06-09-three-terminal-live-session-audit-columns-db-fix-record.md`：补用户确认与执行边界、回滚方式，并脱敏 `CHECK|database`。
- `docs/plans/2026-06-09-inventory-auto-wms-stock-sync-policy-sql-execution-record.md`：补用户确认与执行边界、完整 expected count/signature 确认变量、影响范围和回滚方式。
- `docs/plans/2026-06-09-product-code-pool-job-db-execution-record.md`：补用户确认与执行边界、影响范围、回滚方式和验证边界，明确只证明 `sys_job` 写入，不证明 Quartz 已实际触发补池。
- `docs/plans/2026-06-09-inventory-adjustment-review-sql-execution-record.md`：补独立回滚方式。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 本检查点未输出远端数据库、Redis、token secret 或明文密钥。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx tsc --noEmit`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/partner-management-contract.test.ts tests/portal-session-request.test.ts tests/portal-unauthorized-redirect.test.ts tests/portal-direct-login-message.test.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`：通过，5 suites / 78 tests。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,product,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,TerminalSeedPermissionContractTest,PortalDirectLoginAuthContractTest,SecretCipherSupportTest,ProductReviewServiceImplTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，10 个 reactor 模块 BUILD SUCCESS。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework,ruoyi-system,finance,integration -am "-Dtest=LogAspectSensitiveFieldFilterTest,PortalLogAspectContractTest,SecretCipherSupportTest,LingxingOpenApiClientTest,FinanceCurrencyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，7 个 reactor 模块 BUILD SUCCESS。

### 当前结论

- `P0` 未见新增。
- 代码、SQL、前端串端和 manifest 切片未发现新的开放 P1。
- 已坐实记录层 P1 已关闭：旧模型口径、旧 Maven 命令、远端执行记录确认链/回滚/脱敏缺口均已补。
- P2 留存为 README 本地隔离示例口径、Jest 双配置歧义、未跟踪原型目录、`sessionParams.ts` sidecar、端内菜单库结构列级约束偏宽、源码中文乱码、integration 响应日志脱敏范围、`credentialKeyId` 信息最小化、product portal N+1 性能债和 product 审核读模型契约。
- 本轮不扩大三端账号权限完成度口径。

## 2026-06-10 检查点：三端总门禁通过复核

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前仍保持快速推进模式，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本检查点未启动新子 Agent。
- 如后续需要继续并行拆分，按 `AGENTS.md` 当前规则区分任务类型：只读、审查、探索类使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类使用 `gpt-5.4`。

### 已完成

- 复核当前工作区、`AGENTS.md` 和三端隔离参考方向。
- 运行完整 `verify:three-terminal` 总门禁，覆盖 portal token guard、partner management guard、seller/buyer portal product guard、product upstream mirror guard、React typecheck、24 个前端 Jest suite、后端 reactor test-compile 和后端三端合同测试。
- 本轮未发现新的 P0/P1，因此没有改代码。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 本检查点未输出远端数据库、Redis、token secret 或明文密钥。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。
- 前端：24 suites / 196 tests passed，React typecheck 通过。
- 后端 reactor test-compile：14 个模块 BUILD SUCCESS。
- 后端三端合同测试：11 个模块 BUILD SUCCESS；其中 product 64 tests、seller 100 tests、buyer 101 tests 通过。

### 当前结论

- `P0` 未见新增。
- 当前三端总门禁已通过，未暴露新的编译、guard、接口、权限、串端、service/字段缺失问题。
- P2 继续保留，不阻塞当前快速推进：README 本地隔离示例口径、Jest 双配置歧义、未跟踪原型目录、`sessionParams.ts` sidecar、端内菜单库结构列级约束偏宽、源码中文乱码、integration 响应日志脱敏范围、`credentialKeyId` 信息最小化、product portal N+1 性能债和 product 审核读模型契约。
- 本轮不扩大三端账号权限完成度口径。

## 追加检查点：logistics 后端关键测试路径 P1 收敛

时间：2026-06-10 02:04，本机 `Asia/Shanghai`。

参考方向仍为 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。当前继续只处理 P0/P1，不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 本轮收拢 6 个子 Agent，全部使用 `gpt-5.4`。
- 6 个子 Agent 均已关闭。
- 采纳 P1：`verify-three-terminal` 的 `criticalBackendTestPathPattern` 未包含 `logistics/src/test/java`，导致物流模块后续新增三端敏感测试时可能漏登 manifest 而未被路径兜底发现。
- 未发现新的 P0/P1 的切片：seller/buyer 后端账号权限控制面、SQL/seed guard、React portal token/401/direct-login、React 管理端 seller/buyer UI/service。

### 本轮代码变更

- `react-ui/scripts/verify-three-terminal.mjs`
  - 后端关键测试路径兜底新增 `logistics[\\/]src[\\/]test[\\/]java[\\/]`。
- `react-ui/tests/verify-three-terminal-backend-gate.test.ts`
  - 补充 `logistics` 路径断言。
  - 增加真实负例：临时创建 `LogisticsDriftGuardTest.java` 后，manifest 未登记时 `--check-manifest` 必须失败并输出测试类名。

### 记录口径修正

- `docs/plans/2026-06-09-three-terminal-p0p1-gpt54-fast-pass-record.md`
  - 增加“读取口径”，明确本文件是追加式快照，当前状态以文末最新检查点为准。
- `docs/reports/2026-06-10-three-terminal-completion-evidence-audit.md`
  - 摘要里的三端总门禁不再重复易漂移计数，改为引用文末最新验证检查点。
- `docs/reports/2026-06-09-three-terminal-progress-code-review.md`
  - manifest 计数改为 2026-06-09 23:40 快照口径。
  - React portal 进度改为“登录/直登/portal home 骨架已完成；商品/分类样板为超范围样板，不计入当前账号权限框架完成度”。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/verify-three-terminal-backend-gate.test.ts --runInBand`：通过，1 suite / 23 tests。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 24 suites / 202 tests passed；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 11 个模块 BUILD SUCCESS。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 当前结论

- `P0` 未见新增。
- 已收敛 P1：`logistics` 后端测试路径已纳入三端关键测试 manifest 漏登兜底。
- 本轮不扩大三端账号权限完成度口径。

## 追加检查点：只读 Spark / 编辑 5.4 规则切换与 logistics 合同补齐

时间：2026-06-10 12:24，本机 `Asia/Shanghai`。

参考方向仍为 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。当前继续只处理 P0/P1，不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 用户最新规则已落入 `AGENTS.md`：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 先前按旧口径启动的 6 个 `gpt-5.4` 只读子 Agent 已立即关闭，结论未采纳。
- 本轮随后启动 6 个 `gpt-5.3-codex-spark` 只读子 Agent。
- 已采纳结论：logistics 前端/manifest 缺少三端 gate 覆盖，已补测试、manifest 和 verifier 兜底。
- 未采纳结论：seller/buyer `session:forceLogout` 新权限建议。主线程按 `AGENTS.md` 复核后确认当前 `*:admin:session:list` 与 `*:admin:forceLogout` 已符合既定规则，该建议按误报记录。
- SQL/seed 子 Agent 因上下文压缩失败无有效结论；收拢阶段所有子 Agent 句柄返回 `not_found`，主线程已显式尝试关闭且同样返回 `not_found`，当前没有可等待或可关闭的活动句柄。

### 本轮代码变更

- `RuoYi-Vue/ruoyi-admin/src/main/java/com/ruoyi/web/controller/logistics/AdminLogisticsCarrierController.java`
  - `quote` 增加 `@Log`。
  - `quote` / `createLabel` 日志参数排除 `recipientAddress`、`shipperAddress`、`boxes`。
- `react-ui/src/pages/Logistics/Carrier/index.tsx`
  - 物流管理端页面权限常量化。
  - 修正渠道、日志、系统渠道等入口的权限 guard。
- `RuoYi-Vue/logistics/src/test/java/com/ruoyi/logistics/architecture/LogisticsAdminRouteContractTest.java`
  - 新增 logistics 管理端 controller、SQL seed、权限和敏感日志排除合同。
- `react-ui/tests/logistics-carrier-contract.test.ts`
  - 新增 logistics 前端 service、页面权限、SQL/menu/manifest 合同。
- `react-ui/scripts/verify-three-terminal.mjs`
  - 前端关键测试路径兜底新增 `logistics|carrier`。
- `react-ui/tests/three-terminal.manifest.json`
  - 登记 `LogisticsAdminRouteContractTest` 和 `tests/logistics-carrier-contract.test.ts`。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; node -e "JSON.parse(require('fs').readFileSync('tests/three-terminal.manifest.json','utf8')); console.log('manifest json ok')"`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/logistics-carrier-contract.test.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`：通过，2 suites / 27 tests。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl logistics,ruoyi-admin -am "-Dtest=LogisticsAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，15 个 reactor 模块 BUILD SUCCESS；`LogisticsAdminRouteContractTest` 2 tests。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 25 suites / 207 tests passed；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 12 个模块 BUILD SUCCESS。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，只有 CRLF 工作区提示。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 当前结论

- `P0` 未见新增。
- 已收敛 P1：logistics 管理端新增能力已进入三端 manifest、前端关键测试兜底和后端合同门禁。
- 本轮不扩大三端账号权限完成度口径。

## 追加检查点：gpt-5.3-codex-spark 只读六切片收拢与 integration 脱敏 P1

时间：2026-06-10 12:43，本机 `Asia/Shanghai`。

参考方向仍为 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。当前继续只处理 P0/P1，不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 本轮 6 个只读、审查、探索类子 Agent 均使用 `gpt-5.3-codex-spark`。
- 6 个子 Agent 均已发起关闭并回收；2 个早期切片无有效结论，4 个切片返回有效结论。
- 采纳 P1：integration 外部请求/响应日志脱敏遗漏 `appKey/app_key`。
- 不采纳为当前 P1：
  - PartnerManagement / direct-login 的 3 条补测试建议，主线程复核后确认已有核心契约覆盖。
  - `session:forceLogout` 新权限命名空间建议，与当前 `AGENTS.md` 明确规则冲突，继续按误报处理。
- 交付风险留存：未跟踪文件体量大，后续提交前需确认变更清单；本轮未擅自暂存。

### 本轮代码变更

- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/support/UpstreamMaskUtils.java`
  - `redactJson(...)` 新增 `appKey` / `app_key` 脱敏规则。
- `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/support/UpstreamMaskUtilsTest.java`
  - 新增脱敏回归测试。
- `react-ui/tests/three-terminal.manifest.json`
  - 登记 `UpstreamMaskUtilsTest` 并加入 explicit critical backend test 清单。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration -am "-Dtest=UpstreamMaskUtilsTest,LingxingOpenApiClientTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，5 个 reactor 模块 BUILD SUCCESS；2 tests passed。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 25 suites / 207 tests passed；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 12 个模块 BUILD SUCCESS。
- `cd E:\Urili-Ruoyi; git diff -- RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/support/UpstreamMaskUtils.java RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/support/UpstreamMaskUtilsTest.java react-ui/tests/three-terminal.manifest.json --check`：通过，仅有 CRLF 工作区提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；`Synced 8 changed files`，`Added: 1, Modified: 7 - 338 nodes`。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 当前结论

- `P0` 未见新增。
- 已收敛 P1：外部凭证脱敏覆盖 `appKey/app_key`，并进入三端总门禁。
- 本轮不扩大三端账号权限完成度口径。

## 追加检查点：logistics SQL 合同与执行记录 P1 收口

时间：2026-06-10 13:00，本机 `Asia/Shanghai`。

参考方向仍为 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。当前继续只处理 P0/P1，不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 只读、审查、探索类子 Agent 继续使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类使用 `gpt-5.4`。
- `SQL/seed` 只读切片返回有效结论，提出 logistics SQL 回滚记录和专项合同覆盖不足；主线程采纳后修复。
- `React portal/session` 只读切片返回有效结论，提出 direct-login / 401 / 空 authority 补测建议；主线程复核后确认已有测试覆盖，不新增重复测试。
- `seller/buyer backend` 切片 shutdown，无可采纳结论。
- 其余 3 个只读切片受 `gpt-5.3-codex-spark` 额度限制未形成结论，已关闭；未按只读任务自动切到 `gpt-5.4`。

### 本轮代码与文档变更

- `RuoYi-Vue/sql/20260610_logistics_carrier_channel_rename.sql`
  - 补充 `set names utf8mb4;`。
- `RuoYi-Vue/logistics/src/test/java/com/ruoyi/logistics/architecture/LogisticsAdminRouteContractTest.java`
  - 新增 account-refactor 与 channel-rename 两类 SQL 专项合同断言。
- `docs/plans/2026-06-10-logistics-carrier-account-refactor-sql-execution-record.md`
  - 补确认边界、库名脱敏、回滚方式和 MySQL DDL 隐式提交说明。
- `docs/plans/2026-06-10-logistics-carrier-management-sql-execution-record.md`
  - 补确认边界、库名脱敏、回滚方式和 MySQL DDL 隐式提交说明。
- `docs/plans/2026-06-10-logistics-carrier-channel-rename-sql-execution-record.md`
  - 补后续脚本字符集加固说明和 MySQL DDL 隐式提交说明。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl logistics -am "-Dtest=LogisticsAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`LogisticsAdminRouteContractTest` 4 tests passed。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`SqlExecutionGuardContractTest` 80 tests passed。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 25 suites / 208 tests passed；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 12 个模块 BUILD SUCCESS。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，只有 CRLF 工作区提示。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 当前结论

- `P0` 未见新增。
- 已收敛 P1：logistics 新增 SQL 被通用 SQL guard 和专项合同同时覆盖；执行记录补齐确认、回滚和脱敏口径。
- 本轮不扩大三端账号权限完成度口径。

## 追加检查点：三端账号权限 P0/P1 静态复核与总门禁

时间：2026-06-10 13:06，本机 `Asia/Shanghai`。

参考方向仍为 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。当前继续只处理 P0/P1，不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 本检查点未启动新的子 Agent。
- 已按用户最新要求确认模型规则：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类使用 `gpt-5.4`。
- 本轮通过主线程静态扫描和现有合同门禁即可验证当前切片；未将只读任务自动切换到 `gpt-5.4`。

### 静态复核

- `seller` / `buyer` 主代码未发现端内账号权限直接依赖 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。
- 账号查询保持 `seller_id/buyer_id + account_id` 约束，未发现新增裸 `select*AccountById(accountId)` 生产入口。
- 管理端接口继续使用 `seller:admin:*` / `buyer:admin:*` 权限；portal 受保护接口继续使用 `@PortalPreAuthorize` / `@PortalLog`。
- 未发现新增旧免密 Redis key、默认密码重置接口或 `session:forceLogout` 命名空间。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer,product,ruoyi-framework -am "-Dtest=TerminalRouteOwnershipTest,TerminalAccountIsolationTest,TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest,PortalProductEndpointPermissionContractTest,PortalSelfServiceSurfaceContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminAccountPermissionUiContractTest,PortalLoginSessionConsistencyContractTest,SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,SellerPortalPermissionServiceImplMenuTreeTest,SellerPortalDeptServiceImplTest,SellerPortalProductServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplMenuTreeTest,BuyerPortalDeptServiceImplTest,BuyerPortalProductServiceImplTest,PortalPreAuthorizeAspectTest,TokenServiceTerminalIsolationTest,PortalLogAspectContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`ruoyi-system` 39 tests、`ruoyi-framework` 5 tests、`seller` 45 tests、`buyer` 46 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 25 suites / 208 tests passed；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 12 个模块 BUILD SUCCESS。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 当前结论

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口；当前三端账号权限隔离和管理端控制面在现有合同范围内通过。
- 本轮不扩大三端账号权限完成度口径，不声明浏览器运行态或三物理前端完成。

## 追加检查点：portal 端真实业务接口范围控制复核

时间：2026-06-10 13:13，本机 `Asia/Shanghai`。

参考方向仍为 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。本轮继续只处理 P0/P1，不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 尝试启动 4 个只读、审查、探索类子 Agent，均指定 `gpt-5.3-codex-spark`。
- 4 个子 Agent 均因 Spark 额度限制失败，错误信息为：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at 5:28 PM.`
- 已全部关闭；按用户模型规则，未将只读任务自动切到 `gpt-5.4`。
- 本轮有效结论来自主线程静态扫描和本地门禁。

### 静态复核

- seller/buyer portal 商品控制器均通过 `PortalSessionContext.requireSession(...)` 取得端会话，并声明 `@PortalPreAuthorize` / `@PortalLog`。
- 卖家端商品服务通过 `buildOwnProductQuery(...)` 强制把查询范围设置为当前 `session.getSubjectId()`。
- 买家端商品服务只保留商品检索字段，不使用前端 `buyerId` 作为数据范围。
- `react-ui/src/services/portal/session.ts` 通过 `sanitizePortalQueryParams(...)` 剥离端身份字段，覆盖 audit、session 和 portal 商品列表请求。
- SQL/account/menu/direct-login 相关风险已由现有后端合同覆盖，本轮未发现新增 P0/P1。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer,product,ruoyi-framework -am "-Dtest=TerminalRouteOwnershipTest,PortalAnonymousEndpointContractTest,PortalDirectLoginAuthContractTest,PortalProductEndpointPermissionContractTest,TerminalAccountIsolationTest,TerminalSqlIsolationContractTest,TerminalSeedPermissionContractTest,PortalLoginSessionConsistencyContractTest,PortalPreAuthorizeAspectTest,TokenServiceTerminalIsolationTest,PortalLogAspectContractTest,SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`ruoyi-system` 35 tests、`ruoyi-framework` 5 tests、`seller` 10 tests、`buyer` 11 tests。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 25 suites / 208 tests passed；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 12 个模块 BUILD SUCCESS。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 当前结论

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口。
- 本轮不扩大三端账号权限完成度口径，不声明浏览器运行态或三物理前端完成。

## 追加检查点：当前工作树三端 P0/P1 复核与总门禁复跑

时间：2026-06-10 13:21，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1，不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 尝试启动 4 个只读、审查、探索类子 Agent，均指定 `gpt-5.3-codex-spark`。
- 4 个子 Agent 均因 Spark 额度限制失败，错误信息为：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at 5:28 PM.`
- 已全部关闭；按用户模型规则，未将只读任务自动切到 `gpt-5.4`。
- 本轮有效结论来自主线程静态扫描和本地门禁。

### 静态复核

- `react-ui` portal service 继续通过 `sanitizePortalQueryParams(...)` 剥离端身份字段；未发现新增 portal 调用把前端传入的 `sellerId` / `buyerId` / `subjectId` / `accountId` 当作范围字段。
- seller/buyer portal wrapper 当前只代理到共享 `Portal/Home`，未新增独立绕过 token/权限的页面逻辑。
- `20260610_terminal_portal_home_menu_seed.sql` 已具备确认 token、`45000` fail-closed、ID 段、权限前缀、页面签名和 owner grant 完成态断言。
- logistics 新增合同和 manifest 项当前可被总门禁发现；未发现新增 critical test 脱离 manifest 的 P0/P1。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 25 suites / 208 tests passed；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 12 个模块 BUILD SUCCESS。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 当前结论

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口；本轮没有代码修复。
- 本轮不扩大三端账号权限完成度口径，不声明浏览器运行态、live `/getRouters` 或三物理前端完成。

## 追加检查点：portal 首页商品样板隐藏与默认授权收窄

时间：2026-06-10 13:35，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1，不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 本轮未启动新的子 Agent。
- 当前模型规则按用户最新要求执行：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮为小范围 P1 代码修复，主线程直接完成；无子 Agent 回退、无未关闭子 Agent。

### 处理结论

- `react-ui/src/pages/Portal/Home/index.tsx` 不再挂载卖家/买家的商品 schema 预览和分销商品列表。
- 商品组件、DTO 和 portal 商品 service 保留为后续复用资产；本轮不删除，也不把它们作为当前最小 portal 首页内容。
- seller/buyer product guard 和 `portal-product-schema-preview.test.ts` 改为固定“组件可复用但首页不挂载”的边界。
- 产品权限 seed 保留 hidden F 权限菜单定义，但不再默认授权给端内 owner 角色。
- `SqlExecutionGuardContractTest` 新增 menu-only seed 口径，防止后续产品权限 seed 又变回默认 owner 授权。

### 验证

- `npm run guard:seller-portal-product`：通过。
- `npm run guard:buyer-portal-product`：通过。
- `npx jest --config jest.config.ts tests/portal-product-schema-preview.test.ts --runInBand`：通过，1 suite / 6 tests passed。
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest#terminalPermissionSeedsMustGuardMenuSlotsBeforeRoleBinding+splitTerminalPermissionSeedsMustFailClosedOnInvalidOrDuplicatePermsBeforeRoleBinding+splitTerminalPermissionSeedsMustWrapRoleBindingInTransactionAndAssertCompletion" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，3 tests passed。
- `npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 25 suites / 208 tests passed；React typecheck 通过；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 12 个模块 BUILD SUCCESS。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 未清理当前远程库可能已有的历史 product 权限授权；如后续要清理，需要单独走远端 DML 方案和执行记录。

### 当前判断

- `P0` 未见新增。
- 已收敛 P1：当前最小 portal 首页不再暴露商品样板，未来 seed 默认授权也不再把商品权限给 owner。
- 本轮不扩大三端账号权限完成度口径，不声明浏览器运行态、live `/getRouters`、远程库当前态或三物理前端完成。

## 追加检查点：远程 owner 历史商品权限授权清理

时间：2026-06-10 13:46，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1，不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 本轮未启动新的子 Agent。
- 当前模型规则仍为：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。

### 采纳的 P1

- 上一检查点已把代码和未来 seed 收窄为“product 权限不默认授给 owner”，但远程当前库仍有历史授权。
- 只读预览确认：seller owner product grants = 12，buyer owner product grants = 4。
- 这会让 owner 继续直接调用已保留的 portal product API，和当前最小 portal 框架口径不一致，因此作为 P1 清理。

### 处理结果

- 新增 `RuoYi-Vue/sql/20260610_terminal_owner_product_permission_cleanup.sql`。
- 新增 SQL 合同测试，固定 count/signature fail-closed、只删 role_menu、不删菜单定义。
- 远程 MySQL 已执行清理。
- 清理后 seller/buyer owner product grants 均为 0。
- seller/buyer product permission menus 各保留 4 条。

### 验证

- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest#highImpactSqlScriptsMustRequireExplicitConfirmToken+datedHighImpactSqlScriptsMustBeAutoDiscoveredAndGuarded+terminalOwnerProductPermissionCleanupMustLockExactRoleMenuTargetsAndKeepMenus" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，3 tests passed。
- 远程验证 SQL：seller/buyer owner product grants 均为 0；seller/buyer product permission menus 均为 4。

### 远端影响

- 本检查点执行了远程 MySQL DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 执行记录：`docs/plans/2026-06-10-terminal-owner-product-permission-cleanup-sql-execution-record.md`。

### 当前判断

- `P0` 未见新增。
- 已收敛 P1：当前代码、未来 seed 和远程库当前授权状态一致，不再默认给端内 owner product 权限。
- 本轮不扩大三端账号权限完成度口径，不声明浏览器运行态、live `/getRouters` 或三物理前端完成。

## 追加检查点：Spark 子 Agent 额度失败后的主线程远程不变量复核

时间：2026-06-10 13:56，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1，不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 按用户最新规则尝试启动 6 个只读、审查、探索类 Explorer，均指定 `gpt-5.3-codex-spark`。
- 6 个子 Agent 均因 Spark 额度限制失败，错误信息为：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at 5:28 PM.`
- 已全部关闭；未将只读任务自动切到 `gpt-5.4`。

### 主线程复核

- 静态扫描：seller/buyer 裸 `select*AccountById(accountId)`、portal controller 三件套、React portal token/401/direct-login、管理端按钮权限和 SQL seed guard。
- 远程 MySQL 只读核对：菜单 ID 段、端内权限前缀、页面 component、owner product 授权、portal home 菜单和 owner 授权、端账号空密码。

远程库只读结果：

- seller/buyer 菜单 ID 段越界：0。
- seller/buyer 端内权限前缀异常：0。
- seller/buyer 页面菜单 component 缺失或不合规：0。
- seller/buyer owner product 授权：0。
- seller/buyer 端账号空密码：0。
- seller/buyer `portal:home` 菜单存在且 component 与当前合同一致：`Seller/Portal/index` / `Buyer/Portal/index`；owner 授权存在。

### 当前判断

- `P0` 未见新增。
- 当前未坐实新的 P1 代码缺口。
- 本轮未改业务代码、未执行远程 MySQL DDL/DML、未读写 Redis、未启动或重启后端。

## 追加检查点：账号 SQL 范围约束复核与三端总门禁复跑

时间：2026-06-10 14:05，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent

- 本轮未启动新的子 Agent。
- 当前规则仍为：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮未坐实需要代码编辑的 P0/P1，因此没有启动 `gpt-5.4` 实现子 Agent。

### 复核结果

- `20260610_terminal_portal_home_menu_seed.sql` 已具备确认 token、`45000` fail-closed、ID 段、权限前缀、页面签名和 owner grant 完成态断言。
- `SellerMapper.xml` / `BuyerMapper.xml` 账号 DML 已带 `seller_id/buyer_id + account_id` 范围条件，未发现只按裸 accountId 更新账号状态、密码或登录信息的新增 P1。
- `SellerPortalController` / `BuyerPortalController` 的受保护 portal 端点仍从 `PortalSessionContext.requireSession(...)` 取当前端身份，并配套 `@PortalPreAuthorize` / `@PortalLog`。
- `react-ui/src/services/portal/session.ts` 继续剥离 caller-controlled identity scope 参数，会话列表只放行 `pageNum` / `pageSize`。

### 验证

- `node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。
- 前端：26 suites / 213 tests passed。
- React typecheck：通过。
- 后端 reactor test-compile：15 个模块 BUILD SUCCESS。
- 后端三端合同测试：12 个模块 BUILD SUCCESS。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口；本轮无业务代码修改。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：三端门禁覆盖面与 controller 权限扫描

时间：2026-06-10 14:10，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent

- 本轮未启动新的子 Agent。
- 只读、审查、探索类子 Agent 仍按用户最新规则限定为 `gpt-5.3-codex-spark`；此前 Spark 额度失败，本轮使用主线程静态扫描，没有把只读任务切到 `gpt-5.4`。
- 本轮没有需要实现的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### 复核结果

- 关键测试覆盖：按 `verify-three-terminal.mjs` 的 critical frontend/backend 发现规则重新比对 manifest，未发现关键测试漏登。
- Controller 权限：seller/buyer admin controller 均未扫出缺 `@PreAuthorize` 或写接口缺 `@Log`；seller/buyer portal controller 均未扫出缺 `@PortalPreAuthorize`、`@PortalLog` 或 `PortalSessionContext` 的新增 P0/P1。
- `sys_*` 混用：seller/buyer main 代码未发现端内账号权限实现直接使用 `sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`sys_user_role`、`sys_role_menu`。
- 未跟踪文件：当前仍有 logistics/product/report 等大量未跟踪文件，作为交付整理风险留存；本轮不把它扩大成三端账号权限运行时 P0/P1，也未做 staging。

### 验证

- `node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `git diff --check`：通过，仅有已有 LF/CRLF 工作区提示。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口；本轮无业务代码修改。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：日志/会话/直登 Redis key 隔离复核

时间：2026-06-10 14:15，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent

- 本轮未启动新的子 Agent。
- 只读、审查、探索类子 Agent 后续统一使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮没有需要实现的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### 复核结果

- `PortalDirectLoginSupport`：直登 payload 只读取端隔离 key `portal_direct_login:{terminal}:{token_hash}`；旧无端前缀 key 仅删除清理，不作为认证读取来源。
- `PortalTokenSupport`：portal session 使用 `portal_login_tokens:{terminal}:{tokenId}`；JWT 带 terminal claim；读取 session 时用 expected terminal 计算 Redis key。
- `TokenService`：管理端仍使用若依 `CacheConstants.LOGIN_TOKEN_KEY`，未混用 portal session 或直登 Redis 前缀。
- 自助日志/会话 DTO：合同测试继续固定 `PortalOwnLoginLogProfile`、`PortalOwnOperLogProfile`、`PortalOwnSessionProfile` 不暴露内部审计字段。
- 后台控制审计：seller/buyer service 仍由当前管理端账号写入 acting admin；直登跨端失败不把外端票据 accountId 当作当前端账号记录。

### 验证

- 静态 Node 断言：通过。
- `mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=PortalDirectLoginAuthContractTest,PortalSelfServiceSurfaceContractTest,PortalLoginSessionConsistencyContractTest,PortalPreAuthorizeAspectTest,PortalLogAspectContractTest,TokenServiceTerminalIsolationTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `ruoyi-system`：8 tests passed。
- `ruoyi-framework`：5 tests passed。
- `seller`：55 tests passed。
- `buyer`：55 tests passed。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口；本轮无业务代码修改。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：端内角色/菜单/部门与前端权限串端复核

时间：2026-06-10 14:23，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent

- 本轮按用户最新规则尝试启动 6 个只读 explorer 子 Agent，模型均为 `gpt-5.3-codex-spark`。
- 6 个 explorer 均因 Spark 额度限制失败，提示需 17:28 后再试。
- 已关闭全部失败子 Agent。
- 按规则未把只读扫描切到 `gpt-5.4`；本轮没有需要实现的 P0/P1，因此也未启动 `gpt-5.4` 实现子 Agent。

### 复核结果

- 角色/菜单：seller/buyer 角色、菜单、角色菜单关系均落在端内表；角色绑定菜单前校验当前端菜单 ID 段、component、perms 和 terminal 前缀。
- 部门：seller/buyer 部门服务和 mapper 均按当前主体 ID 查询、更新、删除，父级部门也通过当前主体范围校验。
- 管理端权限：账号分配角色前端入口与后端接口均覆盖端角色查询、账号角色查询、账号角色编辑三类权限；session list 与 force logout 权限未混绑。
- 直登前端：管理端免密登录继续等待 portal direct-login bridge 的消费成功结果，不只按 READY 或 token 发出即提示成功。
- 参数剥离：portal 自助接口剥离 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId`、`terminal` 等 caller-controlled 范围参数；管理端 session 查询只放行分页参数。
- SQL 候选复核：管理端 `sys_menu` 中的 `seller:admin:*` / `buyer:admin:*` 是后台权限，不是端内菜单违规；废弃 split seed 直接 fail-closed；端账号 password 最终结构由 guard 固定为 `varchar(100) not null` 且无默认值。

### 验证

- `mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=SqlExecutionGuardContractTest,PortalPermissionSupportTest,PortalPreAuthorizeAspectTest,PortalLogAspectContractTest,SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplMenuTreeTest,SellerPortalPermissionServiceImplPortalAccessTest,SellerPortalDeptServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalDeptServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- 后端：`ruoyi-system` 87 tests passed；`ruoyi-framework` 4 tests passed；`seller` 35 tests passed；`buyer` 35 tests passed。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npx jest --config jest.config.js tests/partner-management-contract.test.ts tests/portal-session-request.test.ts tests/portal-unauthorized-redirect.test.ts tests/portal-direct-login-message.test.ts tests/terminal-session-token.test.ts --runInBand`：通过，5 suites / 62 tests passed。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口；本轮无业务代码修改。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：远端库三端隔离结构只读核对

时间：2026-06-10 14:27，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 数据源与执行边界

- 已确认后端 active profile 为 `druid`。
- MySQL 连接来自 `.env.local` 中的 `RUOYI_DB_*`，目标库为远端 `fenxiao`。
- Redis 配置来自 `.env.local` 中的 `RUOYI_REDIS_*`，当前 logical DB 为 `1`；本轮未读写 Redis。
- 使用 MySQL JDBC + `jshell` 执行只读 `SELECT` / `information_schema` 聚合查询。
- 未执行远端 MySQL DDL/DML，未输出任何明文密钥或业务明细。

### 复核结果

- 三端核心表存在：seller/buyer 主体、账号、角色、菜单、部门、账号角色、角色菜单、登录日志、操作日志、会话、免密票据表均存在。
- 端账号密码列：`seller_account.password` / `buyer_account.password` 均为 `varchar(100) not null` 且无默认值；空白密码计数均为 0。
- 端内菜单：seller/buyer 菜单 ID 段、perms 前缀和页面 component 聚合异常计数均为 0。
- 端内关系：seller/buyer 角色菜单孤儿或跨 ID 段计数均为 0；账号角色跨主体计数均为 0；部门父级跨主体计数均为 0。
- 免密审计字段：seller/buyer 登录日志、操作日志、会话表均具备 `direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`。
- portal web url 当前是本地验证占位地址，作为环境配置留意项；后续真实三物理前端上线时需要替换，不作为本轮 P0/P1。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1 数据库缺口；本轮无业务代码修改，无 DDL/DML。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：管理端运行态 API 菜单权限合同验证

时间：2026-06-10 14:31，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 运行态边界

- 本机 8080 正在监听，当前 Java 进程启动于 2026-06-10 14:25:59。
- 当前 jar：`RuoYi-Vue/ruoyi-admin/target/ruoyi-admin.jar`，LastWriteTime 为 2026-06-10 14:25:49。
- `/captchaImage` 返回 `captchaEnabled=false`，本轮未读写 Redis。
- 使用 `/login` 获取临时管理端 token，仅用于本轮 API 验证，未记录 token 明文。

### 验证结果

- `/login`、`/getInfo`、`/getRouters` 均返回 `code=200`。
- `/getInfo` 当前角色为 `admin`，权限为 `*:*:*`。
- `/getRouters` 路由总数 64；`/partner/seller` 指向 `Seller/index` 且 `perms=seller:admin:list`；`/partner/buyer` 指向 `Buyer/index` 且 `perms=buyer:admin:list`。
- 带 token 调用 seller/buyer 管理端主体列表与端内菜单列表均返回 `code=200`。
- 不带 token 调用 seller/buyer 管理端主体列表与端内菜单列表均返回业务 `code=401`，未发现匿名裸露。

### 远端影响

- 本检查点未执行远端 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未重启后端。

### 结论

- 管理端运行态 API 与三端隔离后台控制面一致。
- `P0` 未见新增。
- 未坐实新的 P1 运行态缺口；本轮无业务代码修改。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：Portal 未登录与串端拒绝运行态验证

时间：2026-06-10 14:39，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- `AGENTS.md` 已确认模型规则：只读检查、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮尝试 2 个只读探索子 Agent：seller portal P0/P1 权限风险扫描、buyer portal P0/P1 权限风险扫描。
- 实际模型：`gpt-5.3-codex-spark`。
- 结果：两个子 Agent 均因 Spark 额度限制失败，提示 17:28 后重试；两个 Agent 均已关闭。
- 处理：按用户最新规则，未将只读任务切换到 `gpt-5.4`，由主线程继续完成只读验证。

### 运行态 API 证据

- 8080 当前由 Java 进程监听，进程启动时间为 2026-06-10 14:25:59。
- 本轮使用 `/login` 获取临时管理端 token 进行串端拒绝对照，未记录 token 明文。
- 无 token 访问 seller/buyer portal 资料、菜单、日志、商品分类相关接口共 10 个，全部返回业务 `code=401`。
- 管理端 token 访问 seller/buyer portal 的 `/getInfo`、`/getRouters`、`/profile` 共 6 个接口，全部返回业务 `code=401`。
- 伪造 token 访问 `/seller/getInfo`、`/buyer/getInfo`，全部返回业务 `code=401`。

### 最小验证

- `mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=PortalAnonymousEndpointContractTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest,PortalPreAuthorizeAspectTest,PortalPermissionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- 后端结果：`ruoyi-system` 16 tests passed；`ruoyi-framework` 3 tests passed；seller/buyer reactor compile 通过。
- `npx jest --config jest.config.js tests/portal-unauthorized-redirect.test.ts tests/terminal-session-token.test.ts --runInBand`：通过，2 suites / 24 tests passed。
- `node scripts\verify-three-terminal.mjs --check-manifest`（在 `react-ui/` 下执行）：通过。
- `git diff --check`：通过，仅有既有 CRLF 工作区提示。
- `codegraph sync .`：通过，结果为 `Already up to date`。

### 远端影响

- 本检查点未执行手工 SQL，也未执行远端 MySQL DDL/DML 命令。
- `/login` 按若依正常流程可能产生管理端登录日志和 Redis token；该副作用仅用于本轮临时 API 验证，未改业务数据。
- 本检查点未重启后端，未输出任何 token、数据库、Redis 或密钥明文。

### 结论

- portal 运行态拒绝无 token、管理端 token 和伪造 token，未发现 admin/seller/buyer 登录态串用。
- `P0` 未见新增。
- 未坐实新的 P1 运行态缺口；本轮无业务代码修改。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：三端总门禁与登录入口串端复核

时间：2026-06-10 14:45，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 模型与子 Agent

- 本轮未发现需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。
- 只读子 Agent 仍按最新规则应使用 `gpt-5.3-codex-spark`；本轮未重复启动已知额度受限的 Spark 子 Agent。

### 验证结果

- `node scripts\verify-three-terminal.mjs`：通过，输出 `three-terminal verification passed`。
- 前端：26 suites / 213 tests passed。
- 前端 guard：portal token、partner management、seller portal product、buyer portal product、product upstream mirrors 均通过。
- React typecheck：通过。
- 后端 reactor `test-compile`：15 个模块均为 `SUCCESS`。
- 后端三端合同测试：12 个相关模块均为 `SUCCESS`，覆盖 seller/buyer 账号权限、portal、日志、菜单、SQL guard、product/inventory/integration/finance/logistics 等当前 manifest 范围。

### 登录入口串端复核

- `/seller/login` 使用管理端账号口径请求：业务 `code=500`，消息为账号或密码错误，未返回 token。
- `/buyer/login` 使用管理端账号口径请求：业务 `code=500`，消息为账号或密码错误，未返回 token。
- 未读取、输出或尝试任何 seller/buyer 端内账号密码。

### 远端影响

- 两次 portal 登录失败请求可能产生端内失败登录日志；未改业务数据。
- 本检查点未执行手工 SQL，未执行远端 MySQL DDL/DML。
- 本检查点未重启后端，未输出任何 token、数据库、Redis 或密钥明文。

### 结论

- 当前工作树三端总门禁通过。
- 管理端账号不能直接登录 seller/buyer portal。
- `P0` 未见新增。
- 未坐实新的 P1 编译、guard、接口、权限、串端、service/字段缺口。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：端账号 sys_* 回挂与账号范围 SQL 复核

时间：2026-06-10 14:47，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 扫描范围

- seller/buyer 生产 Java：`RuoYi-Vue/seller/src/main/java`、`RuoYi-Vue/buyer/src/main/java`。
- seller/buyer Mapper XML：`SellerPortalPermissionMapper.xml`、`BuyerPortalPermissionMapper.xml`、`SellerMapper.xml`、`BuyerMapper.xml`。
- 关键词：`sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`SysUser`、`SysRole`、`SysMenu`、`SysDept`、若依 `ISys*Service`、`SecurityUtils.getLoginUser`、`SecurityUtils.getUserId`、`select*AccountById(...)`、`accountId`。

### 结果

- 未发现 seller/buyer 端内账号、角色、菜单、部门生产代码回挂若依 `sys_*`。
- 管理端 controller 继续使用若依 `@PreAuthorize("@ss.hasPermi('seller:admin:*' / 'buyer:admin:*')")`，这是平台后台控制面预期行为。
- portal controller 继续使用 `@PortalPreAuthorize` 和 `PortalSessionContext.requireSession(...)`。
- `SecurityUtils.getUserId()` 只用于 seller/buyer 管理端控制动作审计里的 acting admin 记录，不作为端内登录账号来源。
- 端账号、账号角色、角色菜单、会话、重置密码相关 XML 均保持主体 ID + 账号 ID 范围约束。

### 验证

- `mvn -pl ruoyi-system -am "-Dtest=TerminalAccountIsolationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `TerminalAccountIsolationTest`：4 tests passed。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1：未见端账号权限回挂 `sys_*`，未见裸 accountId 查询/写入绕过主体 ID。
- 本轮未修改业务代码，未执行手工 SQL，未执行远端 MySQL DDL/DML。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：三端 SQL seed/cleanup guard 复核

时间：2026-06-10 14:50，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 复核范围

- `RuoYi-Vue/sql/20260610_terminal_portal_home_menu_seed.sql`
- `RuoYi-Vue/sql/20260610_terminal_owner_product_permission_cleanup.sql`
- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
- `RuoYi-Vue/sql/20260608_terminal_menu_auto_increment_reset.sql`
- `RuoYi-Vue/sql/20260604_seller_product_schema_permission_seed.sql`
- `RuoYi-Vue/sql/20260604_buyer_product_schema_permission_seed.sql`
- `RuoYi-Vue/sql/20260604_portal_product_category_permission_seed.sql`

### 结果

- portal home 菜单 seed 保留确认 token、`45000` fail-closed、端内菜单 ID 区间、权限前缀、component 路径、slot 签名和 owner 授权完成断言。
- owner product 授权 cleanup 保留确认 token、预期数量/签名校验、事务内精确删除和完成断言；只清理 owner 角色上的 product 授权，不删除 product 菜单定义。
- `seller_buyer_management_seed.sql` 当前 owner 默认授权不再包含 `seller:product:*` / `buyer:product:*`。
- 相关三端 SQL 仍被 `SqlExecutionGuardContractTest` 自动发现并纳入 guard。

### 验证

- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `SqlExecutionGuardContractTest`：81 tests passed。

### 结论

- `P0` 未见新增。
- 未坐实新的 SQL seed/cleanup P1。
- 本轮未修改业务代码，未执行手工 SQL，未执行远端 MySQL DDL/DML。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：Portal 首页会话权限 P1 收口

时间：2026-06-10 15:01，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent

- 尝试启动 4 个只读 explorer，模型为 `gpt-5.3-codex-spark`。
- 结果：全部因 Spark 额度限制失败，提示 17:28 后重试；已关闭。
- 本轮没有把只读检查改用 `gpt-5.4`。

### 修复

- P1：Portal 首页原当前工作树会无条件请求受 `*:account:session:list` 保护的 `/api/{seller|buyer}/account/sessions`。
- `react-ui/src/pages/Portal/Home/index.tsx` 已改为拿到当前端权限后才加载和展示会话卡片。
- 无 `seller:account:session:list` / `buyer:account:session:list` 时，不请求会话接口，并清空会话状态。
- `react-ui/tests/portal-home-error-handling.test.ts` 增加权限契约。
- `react-ui/tests/three-terminal.manifest.json` 已把 `portal-home-error-handling.test.ts` 放入 critical 前端测试清单。

### 验证

- `npx tsc --noEmit --pretty false`：通过。
- `npx jest --config jest.config.ts tests/portal-home-error-handling.test.ts tests/portal-session-request.test.ts --runInBand`：通过，2 suites / 29 tests passed。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npx jest --config jest.config.ts tests/portal-home-error-handling.test.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`：通过，2 suites / 26 tests passed；Jest 结束后有开放句柄提示，测试状态为 passed。
- `node scripts\check-seller-portal-product-template.mjs`、`node scripts\check-buyer-portal-product-template.mjs`：均通过。

### 结论

- 已修复一个 Portal 首页权限面 P1。
- `P0` 未见新增。
- 本轮未执行手工 SQL，未执行远端 MySQL DDL/DML，未重启后端。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：三端全量门禁复核

时间：2026-06-10 15:05，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 预检查

- `react-ui/tests/portal-product-schema-preview.test.ts` 与 seller/buyer portal product guard 当前一致：商品 widget 保留为 detached reuse widgets，不挂回最小 Portal 首页。
- `npx jest --config jest.config.ts tests/portal-product-schema-preview.test.ts --runInBand`：通过，1 suite / 6 tests passed。

### Full verifier

- `node scripts\verify-three-terminal.mjs`：通过，输出 `three-terminal verification passed`。
- 前端 guard：portal token、partner management、seller portal product、buyer portal product、product upstream mirrors 均通过。
- React typecheck：通过。
- 前端测试：26 suites / 214 tests passed。
- 后端 reactor `test-compile`：15 个模块均为 `SUCCESS`。
- 后端三端合同批次：12 个模块均为 `SUCCESS`；seller 100 tests、buyer 101 tests、product 69 tests、logistics 6 tests 通过。

### 结论

- 当前全量三端门禁未暴露新的 P0/P1。
- 本轮未修改业务代码，未执行手工 SQL，未执行远端 MySQL DDL/DML，未重启后端。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：远端三端菜单与授权只读核对

时间：2026-06-10 15:09，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 数据源与执行边界

- 激活配置：`application.yml` 使用 `druid`，`application-druid.yml` 从 `RUOYI_DB_*` 读取 MySQL。
- 连接来源：当前工作区 `.env.local`。
- 目标库名：`fenxiao`。
- 命令类型：Python + `pymysql` 只读 `SELECT` 聚合查询。
- 未输出数据库主机、账号、密码、token、Redis 或密钥明文。
- 未执行 DDL/DML，未读写 Redis，未重启后端。

### 核对结果

- `seller_account` / `buyer_account`、`seller_role` / `buyer_role`、`seller_menu` / `buyer_menu`、`seller_dept` / `buyer_dept`、`seller_login_log` / `buyer_login_log`、`seller_oper_log` / `buyer_oper_log` 均存在。
- `seller:portal:home` / `buyer:portal:home` 精确菜单各 1 条。
- owner 首页授权与 owner 角色数一致：seller `3/3`，buyer `1/1`。
- seller/buyer 菜单 ID 越界、非法 perms、页面 component 为空或跨端、role-menu 孤儿关系均为 `0`。
- owner 商品授权已清零：seller `0`，buyer `0`。
- seller/buyer product 权限菜单定义均保留 4 条。

### 结论

- 当前远端运行库三端菜单与 owner 授权关键状态未暴露新的 P0/P1。
- 本轮未修改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未重启后端。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：端账号密码与免密审计远端只读核对

时间：2026-06-10 15:16，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 用户最新模型规则：只读检查、审查、探索类使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类使用 `gpt-5.4`。
- 尝试启动 2 个只读探索子 Agent，实际模型均为 `gpt-5.3-codex-spark`。
- 两个子 Agent 均因 Spark 额度限制失败，提示需等到 17:28 后重试；均已关闭。
- 未改用 `gpt-5.4` 顶替只读子任务；本检查点由主线程完成。

### 数据源与执行边界

- 连接来源：当前工作区 `.env.local`。
- 目标库名：`fenxiao`。
- 命令类型：Python + `pymysql` 只读 `SELECT` 聚合查询。
- 未输出数据库主机、账号、密码、token、Redis 或密钥明文。
- 未执行 DDL/DML，未读写 Redis，未重启后端。

### 核对结果

- 远端表存在：`seller_account`、`buyer_account`、`seller_login_log`、`buyer_login_log`、`seller_oper_log`、`buyer_oper_log`、`seller_session`、`buyer_session`、`portal_direct_login_ticket`。
- 密码列：`seller_account.password` / `buyer_account.password` 均为 `varchar(100) not null` 且无默认值；空白密码行数均为 `0`。
- legacy 列：`seller_account.user_id` / `buyer_account.user_id` 均不存在。
- 免密审计字段：seller/buyer 登录日志、操作日志、会话表均具备 `direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`。
- 票据表：`portal_direct_login_ticket` 关键字段齐全，非法票据行数为 `0`，`token_hash` 唯一索引存在。
- 代码合同：管理端重置端账号密码为人工临时密码语义，未发现默认密码重置入口回归；端账号查询保留 `subjectId + accountId` 组合约束；免密 token 有效期为 30 分钟并按 terminal 分 Redis key。

### 结论

- 当前远端运行库和代码合同未暴露端账号密码、legacy `user_id`、免密审计或票据表相关 P0/P1。
- 本轮未修改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未重启后端。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：管理端会话分权与端内自助 DTO 泄露复核

时间：2026-06-10 15:20，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- 尝试启动 2 个只读探索子 Agent，实际模型均为 `gpt-5.3-codex-spark`。
- 两个子 Agent 均因 Spark 额度限制失败，提示需等到 17:28 后重试；均已关闭。
- 因前两个均失败，本轮未继续启动剩余 4 个只读切片。
- 未改用 `gpt-5.4` 顶替只读子任务。

### 核对结果

- 会话权限分权：当前规则是会话查看用 `*:admin:session:list`，强制踢出用 `*:admin:forceLogout`；后端 controller、前端 `PartnerManagementPage` / `PartnerAccountModal` 和合同测试均匹配该规则。
- 历史报告中建议新增 `*:admin:session:forceLogout` 的结论继续按误报处理，不进入代码修改范围。
- 端内自助 DTO：seller/buyer 自助登录日志、操作日志、会话接口返回 `PortalOwn*Profile`，序列化测试禁止泄露 `tokenId`、`directLoginTicketId`、`actingAdmin*`、`directLoginReason`、`operParam`、`jsonResult` 等内部字段。
- `react-ui/tests/three-terminal.manifest.json` 已覆盖上述后端合同测试。

### Contract

- `mvn -pl ruoyi-system -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,PortalSelfServiceSurfaceContractTest,PortalSelfAuditSerializationTest,PortalHomeProfileSerializationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- 结果：16 tests passed；`ruoyi`、`ruoyi-common`、`ruoyi-system` 均 `BUILD SUCCESS`。

### 结论

- 当前管理端会话查看/强退分权和端内自助 DTO 可见面未暴露新的 P0/P1。
- 本轮未修改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未重启后端。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：三端 manifest 覆盖与 portal 接口边界复核

时间：2026-06-10 15:24，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

### 子 Agent 使用记录

- Spark 只读子 Agent 仍处于额度限制窗口，本轮未继续创建新的只读子 Agent。
- 未坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### 覆盖核对

- 前端测试清单：`react-ui/tests/*.test.ts` 与 manifest 双向一致，未发现未登记测试或不存在文件。
- 后端测试清单：当前关键模块 `*Test.java` 与 `backendTestClasses` 双向一致，未发现未登记测试类或不存在类。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。

### Portal 边界核对

- 当前 seller/buyer portal controller 仅分布在 `seller` / `buyer` 模块。
- 非 auth portal handler 由合同测试约束：
  - 使用端内 `@PortalPreAuthorize` / `@PortalLog`。
  - 不接收客户端传入的身份范围参数。
  - 不使用若依管理端登录上下文。
  - 商品 portal 接口必须通过 `PortalSessionContext` 和端内 service 推导主体范围。

### Contract

- `mvn -pl ruoyi-system,seller,buyer,product -am "-Dtest=TerminalRouteOwnershipTest,PortalAnonymousEndpointContractTest,PortalProductEndpointPermissionContractTest,SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- 结果：`ruoyi-system` 10 tests、`seller` 10 tests、`buyer` 11 tests；10 个 reactor 模块均 `BUILD SUCCESS`。

### 结论

- 当前三端 manifest 覆盖和 seller/buyer portal 接口边界未暴露新的 P0/P1。
- 本轮未修改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未重启后端。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：Spark 额度失败后的三端总门禁复跑

时间：2026-06-10 15:31，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent

- 按用户最新规则，本轮尝试启动 6 个只读 explorer，模型均为 `gpt-5.3-codex-spark`。
- 6 个子 Agent 均因 Spark 额度限制失败，提示需等到 17:28 后重试；均已关闭。
- 未把只读任务切换到 `gpt-5.4`；未坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### 验证

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\verify-three-terminal.mjs`：通过，输出 `three-terminal verification passed`。
- 前端 guard：5 个 guard 均通过。
- React typecheck：通过。
- 前端测试：26 suites / 214 tests passed。
- 后端 reactor `test-compile`：15 个模块均 `SUCCESS`。
- 后端三端合同批次：12 个模块均 `SUCCESS`；seller 100 tests、buyer 101 tests、product 69 tests、logistics 6 tests 通过。

### 静态复核

- seller/buyer 生产代码未发现端内账号权限控制面直接复用若依 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。
- seller/buyer 账号查询仍按主体 ID + 账号 ID 组合约束，未发现裸 accountId 生产查询入口。
- 旧直登 key、误命名会话权限、默认密码重置入口和 React 三端关键 URL/权限/token 扫描未坐实新的 P0/P1。
- 命中的平台配置、字典和管理端审计上下文不作为端内账号权限混用问题处理。

### 结论

- 未发现新的 P0/P1。
- 本轮未修改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 继续保留未完成边界：三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收不在本轮完成范围。

## 追加检查点：Portal 首页刷新会话权限 P1 收口

时间：2026-06-10 15:39，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### P1

- 坐实并修复 1 个前端权限面 P1：`react-ui/src/pages/Portal/Home/index.tsx` 的“刷新”按钮会在无 `seller/buyer:account:session:list` 权限时仍调用 `loadSessions(terminal)`。
- 该问题不影响后端强制拒绝，但会让无权限端内账号主动请求受保护会话接口，属于 portal 权限面前端回归。

### 修复范围

- `react-ui/src/pages/Portal/Home/index.tsx`
  - 增加 `clearSessions()`。
  - 初始加载继续由权限快照判断是否请求会话。
  - 刷新按钮自身不直接请求 `/api/{terminal}/account/sessions`；它先清空本地会话状态，再刷新权限快照，后续由最新权限快照的 effect 决定是否拉取会话。
- `react-ui/tests/portal-home-error-handling.test.ts`
  - 增加刷新路径合同，固定 `onClick={handleRefresh}` 且 `handleRefresh` 内不得直接调用 `loadSessions(terminal)`。

### 验证

- `npx jest --config jest.config.ts tests/portal-home-error-handling.test.ts tests/portal-session-request.test.ts --runInBand`：通过，2 suites / 30 tests passed。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `npx tsc --noEmit --pretty false`：通过。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\verify-three-terminal.mjs`：通过；前端 26 suites / 215 tests passed，后端 test-compile 15 模块成功，后端三端合同批次 12 模块成功。

### 结论

- Portal 首页刷新会话权限 P1 已关闭。
- 最终实现口径为：刷新按钮不直接请求会话，端内会话接口只由最新权限快照 gating 后触发。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 继续保留未完成边界：三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收不在本轮完成范围。

## 追加检查点：Portal 首页刷新会话权限 Guard 补强

时间：2026-06-10 15:51，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 按最新规则尝试启动 6 个只读探索子 Agent，模型均指定为 `gpt-5.3-codex-spark`。
- 6 个子 Agent 均因 Spark 额度限制失败，提示需等到 17:28 后重试或切换模型。
- 6 个失败子 Agent 均已关闭。
- 本轮未用 `gpt-5.4` 顶替只读任务；代码编辑由主线程完成。

### 采纳的 P1 补强

- 上一检查点已把 Portal 首页刷新按钮改为“清空会话状态 + 刷新权限快照”，并由最新权限快照 effect 决定是否请求会话。
- 本轮补强 `react-ui/scripts/check-portal-token-isolation.mjs`：
  - 增加源码块提取辅助函数。
  - 提取 `handleRefresh` 函数体。
  - 要求 `handleRefresh` 包含 `clearSessions();` 和 `loadData(terminal);`。
  - 禁止 `handleRefresh` 直接包含 `loadSessions(terminal)`。

### 验证

- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npx jest --config jest.config.ts tests/portal-home-error-handling.test.ts tests/portal-session-request.test.ts --runInBand`：通过，2 suites / 30 tests passed。
- `git diff --check`：通过，仅输出当前工作树既有 LF/CRLF 提示。
- `codegraph sync .`：通过，Synced 2 changed files。

### 结论

- Portal 首页刷新会话权限 P1 的 guard 覆盖已从 Jest 合同扩展到三端快速脚本。
- 本轮未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 继续保留未完成边界：三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收不在本轮完成范围。

## 追加检查点：模型规则残留扫描与完整三端门禁复核

时间：2026-06-10 15:59，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 模型规则核对

- 当前 `AGENTS.md` 和目标追踪顶部现行口径已一致：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 追加式历史记录中仍能搜索到“默认且只能使用 `gpt-5.4`”等旧口径，但属于早期检查点或已被最新现行口径覆盖；本轮未把历史执行事实逐条重写。
- 本轮没有坐实需要代码编辑的 P0/P1，未启动 `gpt-5.4` 实现子 Agent。

### 验证

- `node scripts\verify-three-terminal.mjs`：通过，输出 `three-terminal verification passed`。
- 前端 guard：5 个均通过。
- React typecheck：通过。
- 前端测试：26 suites / 215 tests passed。
- 后端 reactor `test-compile`：15 个模块均 `SUCCESS`。
- 后端三端合同批次：12 个模块均 `SUCCESS`；seller 100 tests、buyer 101 tests、product 69 tests、logistics 6 tests 通过。
- `git diff --check`：通过，仅输出当前工作树既有 LF/CRLF 提示。
- `codegraph sync .`：通过，Already up to date。

### 结论

- 完整三端门禁未暴露新的 P0/P1。
- 本轮未修改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 继续保留未完成边界：三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收不在本轮完成范围。

## 追加检查点：主体停用普通登录合同补强

时间：2026-06-10 16:08，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 模型与子 Agent

- 当前执行规则已确认：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮是两个后端测试合同的小范围补强，主线程直接完成；未启动新的子 Agent。

### 采纳的 P1 补强

- 补齐 seller/buyer 普通密码登录的主体停用合同测试。
- `SellerServiceImplTest` 新增断言：卖家主体停用时 `loginSeller` 返回 `ServiceException("卖家已停用")`，不创建 token，不写会话，不更新登录信息，并写失败登录日志。
- `BuyerServiceImplTest` 新增断言：买家主体停用时 `loginBuyer` 返回 `ServiceException("买家已停用")`，不创建 token，不写会话，不更新登录信息，并写失败登录日志。

### 验证

- `mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；seller 56 tests、buyer 56 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：第一次在仓库根目录误跑，因脚本路径不在根目录而 `MODULE_NOT_FOUND`；随后在 `react-ui/` 下重跑通过。
- `git diff --check`：通过，仅输出当前工作树既有 LF/CRLF 提示。
- `codegraph sync .`：通过。

### 结论

- 主体停用时端内普通登录的 P1 合同覆盖已补齐。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 继续保留未完成边界：三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收不在本轮完成范围。

## 追加检查点：主体/账号停用后已有会话不可继续操作合同补强

时间：2026-06-10 16:14，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 采纳的 P1 补强

- 针对“主体停用后不可操作”的现有会话链路补合同测试。
- 卖家端新增断言：`selectPermissions(...)` 在卖家主体停用或卖家账号停用时直接抛出 `ServiceException`，并且不继续查询在线会话、角色或权限。
- 买家端新增断言：`selectPermissions(...)` 在买家主体停用或买家账号停用时直接抛出 `ServiceException`，并且不继续查询在线会话、角色或权限。

### 验证

- `mvn -pl seller,buyer -am "-Dtest=SellerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；seller 8 tests、buyer 8 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：在 `react-ui/` 下通过。
- `git diff --check`：通过，仅输出当前工作树既有 LF/CRLF 提示。

### 模型与子 Agent

- 本轮未启动新的子 Agent。
- 当前规则继续保持：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮补强完成后曾尝试并行启动 6 个只读 explorer 子 Agent，均指定 `gpt-5.3-codex-spark`；全部因 Spark 配额限制失败，提示 17:28 后再试，未产生可用审查结论，随后全部关闭。由于这批任务是只读检查，未改派 `gpt-5.4`。

### 结论

- 已有会话在主体或账号停用后不可继续通过权限入口操作的 P1 合同覆盖已补齐。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 继续保留未完成边界：三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收不在本轮完成范围。

## 追加检查点：主体停用联动 owner 账号与会话强退合同补强

时间：2026-06-10 16:20，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 采纳的 P1 补强

- 针对管理端主体状态控制权补合同测试：停用卖家/买家主体时，不能只更新主体表状态，还必须同步 owner 登录账号状态并强退该主体所有在线会话。
- 卖家端新增断言：`updateSellerStatus(...)` 使用当前管理端账号写主体状态更新参数，owner 账号同步停用并带 `updateBy`，主体维度强退所有 seller 会话，删除 seller token，并写当前管理端执行人的强退审计。
- 买家端新增断言：`updateBuyerStatus(...)` 使用当前管理端账号写主体状态更新参数，owner 账号同步停用并带 `updateBy`，主体维度强退所有 buyer 会话，删除 buyer token，并写当前管理端执行人的强退审计。

### 验证

- `mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；seller 57 tests、buyer 57 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：在 `react-ui/` 下通过。

### 模型与子 Agent

- 本轮未启动新的子 Agent。
- 当前规则继续保持：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。

### 结论

- 主体停用联动 owner 账号停用和主体级会话强退的 P1 合同覆盖已补齐。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 继续保留未完成边界：三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收不在本轮完成范围。

## 追加检查点：免密票据签发后主体缺失合同补强

时间：2026-06-10 16:28，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 采纳的 P1 补强

- 补齐免密登录票据已签发后，目标主体被删除或不存在时的失败合同。
- 卖家端新增断言：`directLoginSeller(...)` 在卖家主体不存在时抛出 `ServiceException("卖家主体不存在")`，不创建 portal token，不写会话，并写当前端免密失败审计。
- 买家端新增断言：`directLoginBuyer(...)` 在买家主体不存在时抛出 `ServiceException("买家主体不存在")`，不创建 portal token，不写会话，并写当前端免密失败审计。

### 验证

- `mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；seller 58 tests、buyer 58 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：在 `react-ui/` 下通过。

### 模型与子 Agent

- 本轮未启动新的子 Agent。
- 当前规则继续保持：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。Spark 当前仍处于配额限制窗口，未再次启动只读子 Agent。

### 结论

- 免密票据签发后目标主体缺失的 P1 失败合同覆盖已补齐。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 继续保留未完成边界：三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收不在本轮完成范围。

## 追加检查点：账号停用与锁定强退审计合同补强

时间：2026-06-10 16:35，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 尝试启动 3 个只读 explorer 子 Agent，模型均指定为 `gpt-5.3-codex-spark`。
- 3 个子 Agent 均因 Spark 额度限制失败，提示需等到 17:28 后重试或切换模型。
- 3 个失败子 Agent 均已关闭。
- 本轮未用 `gpt-5.4` 顶替只读任务；代码编辑由主线程完成。

### 采纳的 P1 补强

- 补齐管理端账号停用和锁定账号引发强退时的审计合同。
- 卖家侧先改 `SellerServiceImplTest`，把账号停用/锁定测试从旧 `onlineTokenIds` fallback 改为真实 `PortalLoginSession` 输入，覆盖普通会话和免密代入会话。
- 合同固定：只强退当前账号范围，写入当前管理端执行人的强退审计，免密会话保留原票据字段，并只删除 seller terminal token。
- 买家侧按卖家模板机械复制到 `BuyerServiceImplTest`，只替换 buyer 字段、终端和文案。

### 验证

- `mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；seller 58 tests、buyer 58 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：在 `react-ui/` 下通过。
- `git diff --check`：通过，仅输出当前工作树既有 LF/CRLF 提示。

### 结论

- 管理端账号停用/锁定导致强退的 P1 审计合同覆盖已补齐。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 继续保留未完成边界：三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收不在本轮完成范围。

## 追加检查点：主体级免密代入 owner 账号签发合同补强

时间：2026-06-10 16:45，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 已按用户最新要求确认现行规则：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮为小范围代码级测试补强，由主线程直接完成；未启动新的子 Agent。

### 采纳的 P1 补强

- 补齐管理端按主体发起免密代入时的 owner 主账号选择与签发前失败合同。
- 卖家侧先改 `SellerServiceImplTest`，固定 `createSellerDirectLogin(...)` 只能使用当前卖家主体的 owner 账号签发 token。
- 卖家侧同时覆盖 owner 缺失、owner 停用、owner 锁定三个失败路径，断言失败发生在 `createToken(...)` 前。
- 买家侧按卖家模板机械复制到 `BuyerServiceImplTest`，只替换 buyer 字段、终端和文案。

### 验证

- `mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；seller 62 tests、buyer 62 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：在 `react-ui/` 下通过。
- `git diff --check`：通过，仅输出当前工作树既有 LF/CRLF 提示。
- `codegraph sync .`：通过，`Synced 2 changed files`。

### 结论

- 管理端主体级免密代入不会绕过 owner 账号状态控制的 P1 合同覆盖已补齐。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 继续保留未完成边界：三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收不在本轮完成范围。

## 追加检查点：端内自助改密强退审计合同补强

时间：2026-06-10 16:51，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮未启动新的子 Agent。
- 当前规则继续保持：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本切片是小范围测试合同补强，主线程直接完成；未把只读扫描改派给 `gpt-5.4`。

### 采纳的 P1 补强

- 修正 seller/buyer 服务测试桩：真实 `onlineSessions` 非空时也必须按主体和账号过滤，避免测试桩掩盖强退范围错误。
- 卖家侧先改 `updateSellerOwnPasswordForcesAccountSessionsOut`，用普通会话 + 免密代入会话替代旧 `onlineTokenIds` fallback。
- 合同固定：端内自助改密强退只删除当前端 token；普通会话不写管理端审计；免密会话保留原票据结构化审计字段，不能被当前管理端审计覆盖。
- 买家侧按卖家模板机械复制到 `BuyerServiceImplTest`。

### 验证

- `mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；seller 62 tests、buyer 62 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：在 `react-ui/` 下通过。
- `git diff --check`：通过，仅输出当前工作树既有 LF/CRLF 提示。
- `codegraph sync .`：通过，`Synced 3 changed files`。

### 结论

- 端内自助改密强退的会话范围和免密审计 P1 合同覆盖已补齐。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 继续保留未完成边界：三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收不在本轮完成范围。

## 追加检查点：子 Agent 模型规则确认与 P0/P1 续扫

时间：2026-06-10 16:59，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent 使用记录

- 已按用户最新规则固定：只读、检查、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮尝试启动 3 个只读 explorer 子 Agent，均指定 `gpt-5.3-codex-spark`。
- 3 个 Spark 子 Agent 均因额度限制失败，提示 17:28 后再试；均已关闭。
- 本轮未把只读任务切到 `gpt-5.4`，也未启动代码编辑 worker。

### 主线程复核

- 端内自助 DTO：已有 `PortalSelfServiceSurfaceContractTest`、`PortalSelfAuditSerializationTest`、`PortalHomeProfileSerializationTest` 固定可见 DTO 和内部字段不外泄。
- 管理端控制权限：seller/buyer admin controller、React 管理端入口和 service 已有合同固定 `session:list` / `forceLogout` 分权、账号角色三权限 gate、临时密码重置、审计和票据入口权限。
- 裸账号查询：`TerminalAccountIsolationTest` 已固定 seller/buyer 账号查询必须带主体 ID，未发现新的 account-only 生产入口。

### 结论

- `P0` 未见新增。
- 未坐实新的 `P1` 代码缺口；本轮无业务代码修改。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：Spark 只读子 Agent 失败后的主线程 P0/P1 续扫

时间：2026-06-10 19:28，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 启动 6 个 `gpt-5.3-codex-spark` 只读 explorer，覆盖后端账号权限隔离、portal 会话/直登/审计、SQL seed/guard、React guard/service、管理端控制权、verify manifest 覆盖。
- 6 个 explorer 均因 Spark 额度限制失败：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at Jun 17th, 2026 2:16 AM.`
- 6 个 explorer 均已关闭；无可采纳结论。
- 本轮未把只读任务切到 `gpt5.4`，因为 `gpt5.4` 只用于代码编辑、实现、修复类子 Agent。

### 主线程复核

- seller/buyer 生产模块未见 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 端内控制面混用。
- 账号查询继续带主体 ID 和账号 ID；Mapper XML 核心读写继续带 `seller_id` / `buyer_id` 约束。
- 管理端账号角色、会话列表、强制踢出、重置密码权限链路未见 P1 回退。
- portal 自助日志和会话接口返回端内可见 `PortalOwn*Profile`，未直接暴露内部完整审计模型。
- direct-login Redis payload 只读取 terminal scoped key；legacy key 只用于删除。
- owner 默认角色授权清单未包含产品权限，产品权限仅保留为隐藏 `F` 菜单定义。
- React 侧 portal 查询参数剥离、远程菜单空权限 fail-closed、401 reject/throw 和管理端 service 路径未见串端。

### 验证

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `npx jest --config jest.config.ts --runTestsByPath tests\partner-management-contract.test.ts tests\portal-session-request.test.ts tests\terminal-session-token.test.ts tests\remote-menu-route-guard.test.ts tests\admin-auth-sidecar-contract.test.ts --runInBand`：通过；5 suites、88 tests。
- `mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,TerminalSqlIsolationContractTest,TerminalRoleMenuMapperIsolationContractTest,PortalDirectLoginSupportTest,PortalPermissionCheckerTest,PortalPermissionSupportTest,PortalSelfServiceSurfaceContractTest,PortalPasswordChangeContractTest,AdminAccountPermissionUiContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,SellerServiceImplTest,SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,BuyerServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；ruoyi-system 53 tests、seller 88 tests、buyer 89 tests。

### 结论

- `P0` 未见新增。
- 未坐实新的 `P1` 代码缺口；本轮无业务代码修改。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：远程库权限 seed 与 owner 绑定只读核对

时间：2026-06-10 19:25，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。数据源来自当前后端激活 `druid` 配置与本机 `.env.local`，不输出任何连接信息或密钥。本轮只执行远程 MySQL 只读聚合查询，不执行 DDL/DML，不读写 Redis。

### 子 Agent 使用记录

- 本轮未启动新的子 Agent。
- 只读、检查、审查、探索类子 Agent 继续使用 `GPT-5.3-Codex-Spark`；代码编辑、实现、修复类子 Agent 使用 `gpt5.4`。
- 本轮没有把只读任务切给 `gpt5.4`，也没有坐实需要代码编辑的 P0/P1。

### 只读核对

- `missing_sys_menu_required_admin_perms = 0`：管理端 seller/buyer 必需权限菜单均存在且启用。
- `seller_owner_role_count = 4`，`buyer_owner_role_count = 1`。
- `seller_owner_account_without_owner_role = 0`，`buyer_owner_account_without_owner_role = 0`。
- `seller_owner_missing_self_perms = 0`，`buyer_owner_missing_self_perms = 0`。
- `seller_owner_missing_portal_home = 0`，`buyer_owner_missing_portal_home = 0`。
- `seller_owner_product_grants = 0`，`buyer_owner_product_grants = 0`。
- `seller_role_menu_missing_menu = 0`，`buyer_role_menu_missing_menu = 0`。
- `seller_account_role_missing_role = 0`，`buyer_account_role_missing_role = 0`。

### 结论

- `P0` 未见新增。
- 未坐实新的 `P1` 权限 seed、owner 绑定、角色菜单关联或账号角色关联缺口。
- 本轮无业务代码修改，无远端 MySQL DDL/DML，无 Redis 读写，无后端启动或重启。

## 追加检查点：未跟踪 portal wrapper 与模型规则复核

时间：2026-06-10 19:02，本机 `Asia/Shanghai`。

### Scope

- 本轮只复核 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器、截图、DOM 或 UI 细调验收。
- 未启动新的子 Agent；当前模型规则已按用户最新口径写入 `AGENTS.md`：只读检查使用 `GPT-5.3-Codex-Spark`，代码编辑使用 `gpt5.4`；工具 id 对应 `gpt-5.3-codex-spark` / `gpt-5.4`。

### Evidence

- `react-ui/src/pages/Seller/Portal/index.tsx`、`react-ui/src/pages/Buyer/Portal/index.tsx` 均只 re-export 共享 `Portal/Home`。
- `react-ui/src/pages/Seller/Portal/index.js`、`react-ui/src/pages/Buyer/Portal/index.js` 均只 re-export 对应 `index.tsx`。
- `react-ui/src/services/seller-buyer/sessionParams.ts` 只保留 `pageNum` / `pageSize`，不传主体或账号范围字段。
- `admin-auth-sidecar-contract.test.ts`、`partner-management-contract.test.ts` 与 `three-terminal.manifest.json` 已覆盖上述 wrapper 与 sanitizer。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `git diff --check`：通过，仅有既有 LF/CRLF 提示。
- `node scripts\verify-three-terminal.mjs`：通过，最终输出 `three-terminal verification passed`。
- 前端 27 suites / 220 tests passed；后端 reactor `test-compile` 15 个模块 `SUCCESS`；后端三端合同批次 12 个模块 `SUCCESS`，seller 110 tests、buyer 112 tests、product 69 tests、logistics 11 tests 通过。

### Conclusion

- `P0` 未见新增。
- 未坐实新的 P1 编译、guard、接口、权限、串端、service/字段缺口。
- 本轮无业务代码修改，无远端 MySQL DDL/DML，无 Redis 读写，无后端启动或重启。
- 本检查点仍不证明三物理前端、live `/seller/getRouters` / `/buyer/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：远程库三端关键结构只读核对

时间：2026-06-10 19:18，本机 `Asia/Shanghai`。

### Scope

- 本轮只补远程库结构 P0/P1 证据：三端账号、密码、角色、菜单、部门、日志、会话、免密审计字段和菜单/权限聚合异常。
- 不做浏览器、截图、DOM 或 UI 细调验收。
- 只执行只读 `information_schema` 与聚合 count 查询；未执行远程 MySQL DDL/DML，未读写 Redis。

### Evidence

- 当前后端激活 `druid`，JDBC 由 `.env.local` 的 `RUOYI_DB_*` 提供；本记录不暴露连接明文。
- 远程库存在：`seller`、`buyer`、`seller_account`、`buyer_account`、`seller_role`、`buyer_role`、`seller_menu`、`buyer_menu`、`seller_dept`、`buyer_dept`、`seller_account_role`、`buyer_account_role`、`seller_role_menu`、`buyer_role_menu`、`seller_login_log`、`buyer_login_log`、`seller_oper_log`、`buyer_oper_log`、`seller_session`、`buyer_session`、`portal_direct_login_ticket`。
- `seller_account.password` / `buyer_account.password` 均为 `varchar(100)`、`nullable=NO`、无默认值。
- seller/buyer 登录日志、操作日志、会话表均存在 `direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`。
- 聚合异常计数均为 0：菜单 ID 越界、页面/按钮权限前缀异常、页面组件路径异常、账号空密码、role-menu 跨 ID 区间。
- `portal.seller.web.url` / `portal.buyer.web.url` 仍命中本地验证地址特征；按既有口径记录为 P2 环境配置项，不阻塞当前 P0/P1。

### Conclusion

- `P0` 未见新增。
- 未坐实新的 P1 远程数据库结构、字段、菜单 ID 区间、权限前缀、组件路径或端账号密码约束缺口。
- 本轮无业务代码修改，无远端 MySQL DDL/DML，无 Redis 读写，无后端启动或重启。
- 本检查点仍不证明三物理前端、live `/seller/getRouters` / `/buyer/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：Spark 额度失败后的主线程 P0/P1 续扫

时间：2026-06-10 19:10，本机 `Asia/Shanghai`。

### Scope

- 本轮只复核 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器、截图、DOM 或 UI 细调验收。
- 按当前 `AGENTS.md` 规则尝试启动 6 个只读 explorer，模型均为 `gpt-5.3-codex-spark`；6 个均因 Spark 额度限制失败并已关闭。
- 本轮未把只读任务切到 `gpt5.4`，因为 `gpt5.4` 只用于代码编辑、实现、修复类子 Agent。

### Evidence

- `RuoYi-Vue/seller` / `RuoYi-Vue/buyer` 主模块扫描未命中 `sys_user`、`sys_role`、`sys_menu`、`sys_dept`。
- seller/buyer 账号查询扫描未发现裸单参数 accountId 生产入口；当前生产代码继续使用主体 ID + 账号 ID。
- seller/buyer 管理端 controller 当前会话列表和强退权限已分开：`*:admin:session:list` 用于 `/sessions/list`，`*:admin:forceLogout` 用于 DELETE 强退。
- seller/buyer resetPwd 当前为人工临时密码入口，`normalizeTemporaryPassword` 校验 5-20 位；未发现 `resetDefaultPwd` 静默默认密码入口。
- React seller/buyer 管理端会话列表 service 只传 `pageNum` / `pageSize`，不传主体或账号范围字段。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `npx jest --config jest.config.ts --runTestsByPath tests\partner-management-contract.test.ts tests\portal-session-request.test.ts tests\terminal-session-token.test.ts tests\remote-menu-route-guard.test.ts tests\admin-auth-sidecar-contract.test.ts --runInBand`：通过；5 suites / 88 tests。
- `mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,TerminalSqlIsolationContractTest,TerminalRoleMenuMapperIsolationContractTest,PortalDirectLoginSupportTest,PortalPermissionCheckerTest,PortalPermissionSupportTest,SellerServiceImplTest,SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,BuyerServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；ruoyi-system 42 tests、seller 88 tests、buyer 89 tests。
- `node scripts\verify-three-terminal.mjs`：通过，最终输出 `three-terminal verification passed`。
- 前端 27 suites / 220 tests passed；后端 reactor `test-compile` 15 个模块 `SUCCESS`；后端三端合同批次 12 个模块 `SUCCESS`，seller 110 tests、buyer 112 tests、product 69 tests、logistics 11 tests 通过。

### Conclusion

- `P0` 未见新增。
- 未坐实新的 P1 编译、guard、接口、权限、串端、service/字段缺口。
- 本轮无业务代码修改，无远端 MySQL DDL/DML，无 Redis 读写，无后端启动或重启。
- 本检查点仍不证明三物理前端、live `/seller/getRouters` / `/buyer/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：manifest 漏登 P1 收口与三端总门禁复跑

时间：2026-06-10 18:53，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 启动 6 个只读 explorer，均使用 `gpt-5.3-codex-spark`。
- 完成并关闭、结论采纳为“未发现 P0/P1”的切片：React 管理端 seller/buyer、React portal token/401/direct-login/remote menu、三端 verifier/manifest/test 覆盖。
- SQL/seed guard 切片上下文窗口错误退出并关闭，未采纳。
- 后端 seller/buyer portal 自助链路和管理端控制链路两个切片连续超时，已关闭，未采纳。
- 本轮未启动 `gpt-5.4` 实现子 Agent；坐实的 P1 由主线程最小修复。

### P1

- 首次完整 `node scripts\verify-three-terminal.mjs` 失败于 `tests/verify-three-terminal-backend-gate.test.ts`。
- 根因：已跟踪关键前端权限测试 `tests/upstream-system-permission-guard.test.ts` 未登记到 `react-ui/tests/three-terminal.manifest.json`。
- 影响：三端总门禁失败，且 manifest gate 的负例测试被无关漏登项抢先触发。

### Fix

- `react-ui/tests/three-terminal.manifest.json`：
  - `frontendTestPaths` 补 `tests/upstream-system-permission-guard.test.ts`。
  - `criticalFrontendExplicitTestPaths` 补 `tests/upstream-system-permission-guard.test.ts`。

### Evidence

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npx jest --config jest.config.ts --runTestsByPath tests\verify-three-terminal-backend-gate.test.ts --runInBand`：通过；1 suite、23 tests。
- `node scripts\verify-three-terminal.mjs`：通过，输出 `three-terminal verification passed`。
- 全量三端 gate 中前端 27 suites / 220 tests passed；后端 reactor `test-compile` 15 个模块 `SUCCESS`；后端三端合同批次 12 个模块 `SUCCESS`，seller 110 tests、buyer 112 tests、product 69 tests、logistics 11 tests 通过。

### 结论

- `P0` 未见新增。
- 已关闭 1 个 P1：关键前端权限测试漏登 manifest 导致三端总门禁失败。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：当前态三端 P0/P1 复核

时间：2026-06-10 18:58，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮未启动新的子 Agent；当前规则仍为只读、检查、审查、探索类使用 `gpt-5.3-codex-spark`，代码编辑、实现、修复类使用 `gpt-5.4`。
- 本轮没有坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### 主线程复核

- seller/buyer 主模块未命中 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。
- 旧默认重置密码入口未回归：`resetDefaultPwd` 和 `*:admin:resetPwd` 命中均为禁止项、测试或历史清理 SQL；`U12346` 命中为初始 owner 默认密码常量和测试。
- 未发现新的裸单参数账号查询入口；seller/buyer 账号查询继续以主体 ID + 账号 ID 为边界。

### Evidence

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `git diff --check`：通过，仅输出当前工作树既有 LF/CRLF 提示。
- `node scripts\verify-three-terminal.mjs`：通过，输出 `three-terminal verification passed`。
- 全量三端 gate 中前端 27 suites / 220 tests passed；后端 reactor `test-compile` 15 个模块 `SUCCESS`；后端三端合同批次 12 个模块 `SUCCESS`，seller 110 tests、buyer 112 tests、product 69 tests、logistics 11 tests 通过。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1 编译、guard、接口、权限、串端、service/字段缺口；本轮无业务代码修改。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：当前工作树三端总门禁复跑

时间：2026-06-10 18:26，本机 `Asia/Shanghai`。

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 当前子 Agent 模型规则继续以 `AGENTS.md` 为准：只读、检查、审查、探索类使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类使用 `gpt-5.4`。
- 本轮没有坐实需要并行拆分的新 P0/P1，因此未启动子 Agent。

### 当前工作树复核

- 当前 dirty tree 仍混有三端主线、product、integration、logistics 和多份报告/计划文档；本轮不回退用户或其他流程留下的改动。
- 最近记录里明确坐实的三端 P1 已标注关闭；剩余主要是 P2 或相邻业务 scope drift 风险，不阻塞当前 P0/P1 快速推进。

### 验证

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `git diff --check`：通过，仅输出当前工作树既有 LF/CRLF 提示。
- `node scripts\verify-three-terminal.mjs`：通过，输出 `three-terminal verification passed`。
  - 前端 guard 全部通过。
  - React typecheck 通过。
  - 前端测试：27 suites、220 tests passed。
  - 后端 reactor `test-compile`：15 个模块均 `SUCCESS`。
  - 后端三端合同批次：12 个模块均 `SUCCESS`；seller 110 tests、buyer 112 tests、product 69 tests、logistics 11 tests 均通过。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1 编译、guard、接口、权限、串端、service/字段缺口；本轮无业务代码修改。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：Spark 六切片 P0/P1 收拢与记录口径修正

时间：2026-06-10 18:21，本机 `Asia/Shanghai`。

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只修和验证 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 当前模型规则继续按用户最新要求执行：只读、检查、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮启动 6 个只读 explorer 子 Agent，全部指定 `gpt-5.3-codex-spark`。
- 完成并关闭、结论采纳为“未发现 P0/P1”的切片：
  - 后端 seller/buyer portal 自助链路。
  - 管理端账号、会话、强退、重置密码、免密代入控制链路。
  - React portal token、401、direct-login、remote menu guard。
  - 三端 manifest、guard、测试覆盖。
- 因上下文窗口错误退出并关闭、未采纳结论的切片：
  - SQL/seed guard。
  - React 管理端 seller/buyer 串端检查。
- 本轮未启动 `gpt-5.4` 代码编辑子 Agent，因为没有确认需要修复的 P0/P1。

### 主线程复核

- 修正少量历史 Markdown 旧模型口径，避免把早期“只用 gpt-5.4”的历史当轮规则误读为现行规则。
- SQL/seed 主线程补扫确认：端账号密码列仍有最终 guard，`portal.seller.web.url` / `portal.buyer.web.url` 只在缺失时插入，不覆盖已有配置。
- React 管理端主线程补扫确认：seller service/page 范围未命中 buyer API 或 buyer 管理权限串；buyer service/page 范围未命中 seller API 或 seller 管理权限串。
- 已知 Portal Home P1 修复仍成立：无会话权限时不加载 sessions，刷新按钮只刷新首页信息并清空 sessions，不再无条件调用会话接口。

### 验证

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `npx jest --config jest.config.ts --runTestsByPath tests\portal-home-error-handling.test.ts tests\portal-session-request.test.ts tests\partner-management-contract.test.ts --runInBand`：通过；3 suites、37 tests。Jest 提示可能存在未关闭异步句柄，命令退出码为 0，按 P2 记录。
- `mvn -pl seller,buyer,ruoyi-system -am "-Dtest=TerminalAccountIsolationTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,PortalAnonymousEndpointContractTest,PortalPasswordChangeContractTest,PortalSelfServiceSurfaceContractTest,SellerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；31 tests。
- `git diff --check`：通过，仅输出当前工作树既有 LF/CRLF 提示。

### 结论

- `P0` 未见新增。
- 未坐实新的 `P1` 代码缺口；本轮无业务代码修改。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：只读 Spark / 实现 gpt-5.4 规则确认与管理端链路续扫

时间：2026-06-10 18:06，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 已按用户最新要求确认：只读、检查、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- `AGENTS.md` 当前已写入上述规则。
- 本轮启动 3 个 `gpt-5.3-codex-spark` 只读 explorer：
  - 账号 SQL 作用域 explorer 超时后关闭，未产出结论。
  - portal 权限/审计 explorer 完成并关闭，未发现 P0/P1；主线程确认管理端继续使用 `@PreAuthorize` + `@Log`，不改为 portal 控制面。
  - React 管理端 explorer 因上下文窗口错误退出并关闭，未产出结论。
- 本轮没有启动 `gpt-5.4` worker，因为未发现需要代码编辑修复的 P0/P1。

### 主线程复核

- `rg` 扫描 seller/buyer 生产模块未发现端内账号权限继续依赖 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。
- `SellerMapper.xml` / `BuyerMapper.xml` 账号查询、更新、锁定、重置密码、登录信息更新均带主体 ID 和账号 ID 条件。
- controller 结构化扫描未发现 seller/buyer portal 受保护入口缺少 `@PortalPreAuthorize`、`@PortalLog` 或 `PortalSessionContext.requireSession(...)`。
- React `PartnerManagement` 与 seller/buyer service 继续符合已有契约：路径不串端、账号角色三权限 gate、会话列表与强退分权、临时密码重置。

### 验证

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npx jest --config jest.config.ts --runTestsByPath tests\partner-management-contract.test.ts --runInBand`：通过；1 suite、7 tests。
- `mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,PortalAnonymousEndpointContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；13 tests。
- `git diff --check`：通过，仅输出当前工作树既有 LF/CRLF 提示。
- `codegraph sync .`：通过，`Synced 2 changed files`。

### 结论

- `P0` 未见新增。
- 未坐实新的 `P1` 代码缺口；本轮无业务代码修改。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：OWNER 主账号默认角色闭环修复

时间：2026-06-10 17:46，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只修 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 只读运行库核对由主线程执行，只做远端 MySQL SELECT 聚合。
- 代码编辑任务启动 1 个 `gpt-5.4` worker 子 Agent（Wegener）处理买家侧初始实现；主线程复核后把 seller/buyer 收敛为同构实现，并关闭该子 Agent。
- 本轮没有把只读任务交给 `gpt-5.4`；只读子 Agent 模型规则仍为 `gpt-5.3-codex-spark`。

### 运行库只读发现

- 数据源来源：当前 `.env.local` 的 `RUOYI_DB_URL`，运行库名 `fenxiao`；未输出任何连接密钥。
- 只读 SELECT 聚合发现：端内表、密码列、菜单 ID 区间、权限前缀、页面 component、role-menu、portal home、免密票据、审计列和 portal URL 配置均正常。
- 唯一异常：`seller_owner_account_missing_owner_role_count=1`，定位到存量 `seller_account_id=9`、`seller_id=10`、`user_name=pengju` 缺少 owner 角色绑定，且该主体缺 active owner role / owner 菜单授权。
- 未执行远端 MySQL DDL/DML；该存量数据缺口需要单独确认精确 DML 后再修复。

### 代码修复

- `SellerServiceImpl` / `BuyerServiceImpl`：OWNER 主账号创建或同步时，先确保 active owner 角色存在，再补齐默认 7 个基础菜单授权，最后幂等绑定主账号 owner 角色；失败时 fail-closed。
- `SellerPortalPermissionMapper` / `BuyerPortalPermissionMapper` 及 XML：新增 owner 角色、owner 默认菜单授权、owner 主账号角色绑定的幂等补缺方法。
- `SellerServiceImplTest` / `BuyerServiceImplTest`：新增主体创建路径回归，覆盖 owner role、owner menu grants、owner account role binding；测试桩同步模拟 MyBatis 生成账号 ID 和端内权限补缺。

### 验证

- `mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；seller 63 tests、buyer 64 tests。
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；93 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `SellerPortalPermissionMapper.xml` / `BuyerPortalPermissionMapper.xml`：XML parser 解析通过。
- `git diff --check`：通过，仅有既有 LF/CRLF 提示。

### 结论

- `P0` 未见新增。
- 已修复 `P1`：未来新建或同步卖家/买家 OWNER 主账号时，不再产生“有主账号但缺 owner 角色/默认基础权限”的端内控制面缺口。
- 本轮未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端；远端存量 `seller_account_id=9` 缺口仍待单独确认数据修复。

## 追加检查点：远端 OWNER 存量控制面缺口精确 DML 修复

时间：2026-06-10 17:55，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 启动 1 个 `gpt-5.3-codex-spark` 只读 explorer 做旁路审查。
- 主线程已完成远端只读预览、签名、事务修复和后置验证；该子 Agent 未产出结论即关闭，未作为本轮证据来源。

### 执行记录

- 新增 Markdown：`docs/plans/2026-06-10-terminal-owner-role-backfill-db-execution-record.md`。
- 目标库名：`fenxiao`。
- 执行前精确候选：`seller_id=10`、`seller_account_id=9`、`user_name=pengju`。
- 执行前签名：`2c46558ccd06926dcc9ad9b3e3fceae0a780d974f7b1e47406abe23344ac68c7`。
- 执行脚本在事务内重新计算签名，签名不匹配则回滚。

### DML 结果

- `seller_role`：新增 1 行，`seller_role_id=11`，`seller_id=10`，`role_key='owner'`。
- `seller_role_menu`：新增 7 行，绑定 owner 默认基础菜单。
- `seller_account_role`：新增 1 行，绑定 `seller_account_id=9` 到 `seller_role_id=11`。
- 未触碰 `sys_*`、`buyer_*`、product、inventory、finance、integration 等表。
- 未读写 Redis，未启动或重启后端。

### 后置只读复核

- seller/buyer 全局 OWNER 主账号缺 owner role 绑定：`0`。
- seller/buyer active owner role 缺默认基础菜单授权：`0`。
- seller/buyer owner product grant：`0`。
- seller/buyer role-menu 与 account-role 孤儿关系：`0`。

### 验证

- `mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；seller 63 tests、buyer 64 tests。
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；93 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。

### 结论

- `P0` 未见新增。
- 远端运行库既有 `seller_account_id=9` / `seller_id=10` 的 `P1` OWNER 控制面缺口已修复。
- 本轮不包含浏览器、截图、DOM 或 UI 细调验收。

## 追加检查点：管理端控制权链路复核

时间：2026-06-10 17:24，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 用户最新模型规则继续生效：只读、检查、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 当前时间仍早于 Spark 额度提示的 17:28 重试点，本轮未启动新的只读子 Agent。
- 未坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### 主线程复核

- 后端 seller/buyer admin controller 继续使用端管理命名空间 `seller:admin:*` / `buyer:admin:*`，不使用 portal 注解或端内账号上下文。
- 账号角色回显/分配、账号重置密码、会话查看、强制踢出、免密代入和票据审计均有独立权限合同。
- 服务层继续使用 scoped mapper 和端账号/端会话/端日志表；未发现回退默认密码重置、裸账号查询或 `sys_*` 端内控制面。
- React 管理端 service 和 `PartnerManagement` 模板继续按卖家/买家配置机械复用，权限和接口路径未串端。

### 验证

- `mvn -pl ruoyi-system,seller,buyer -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest,AdminAccountPermissionUiContractTest,TerminalAccountIsolationTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；138 tests。
- `npx jest --config jest.config.ts --runTestsByPath tests\partner-management-contract.test.ts tests\admin-auth-sidecar-contract.test.ts --runInBand`：通过；2 suites、44 tests。
- `npm run guard:partner-management`：通过。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。

### 结论

- `P0` 未见新增。
- 未坐实新的 `P1` 代码缺口；本轮无业务代码修改。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：登录入口与 portal 路由串端复核

时间：2026-06-10 17:21，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 用户最新模型规则继续生效：只读、检查、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 当前时间仍早于 Spark 额度提示的 17:28 重试点，本轮未启动新的只读子 Agent。
- 未坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### 主线程复核

- 端内登录入口仍由 `SellerPortalAuthController` / `BuyerPortalAuthController` 承载，认证和免密登录入口不依赖管理端登录上下文。
- seller/buyer portal 受保护路由由 `@Anonymous` + `@PortalPreAuthorize` + `@PortalLog` + `PortalSessionContext.requireSession(...)` 固定。
- 后端合同继续禁止 portal handler 接收客户端传入的 `sellerId/buyerId/subjectId/accountId` 作为身份范围。
- React 侧 terminal token、portal 401、direct-login postMessage、scope 参数剥离和 remote menu guard 均按端隔离合同复核。

### 验证

- `mvn -pl ruoyi-system,seller,buyer,ruoyi-framework -am "-Dtest=TerminalRouteOwnershipTest,PortalAnonymousEndpointContractTest,PortalDirectLoginAuthContractTest,PortalLoginSessionConsistencyContractTest,TokenServiceTerminalIsolationTest,PortalPreAuthorizeAspectTest,PortalDirectLoginSupportTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；157 tests。
- `npx jest --config jest.config.ts --runTestsByPath tests\terminal-session-token.test.ts tests\portal-unauthorized-redirect.test.ts tests\portal-direct-login-message.test.ts tests\portal-session-request.test.ts tests\remote-menu-route-guard.test.ts --runInBand`：通过；5 suites、68 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。

### 结论

- `P0` 未见新增。
- 未坐实新的 `P1` 代码缺口；本轮无业务代码修改。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：terminal SQL 与 portal 最小首页复核

时间：2026-06-10 17:16，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 用户最新模型规则继续生效：只读、检查、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 当前目标追踪文本里的“子Agent用gpt-5.4”属于旧口径，后续执行以最新规则和 `AGENTS.md` 为准。
- 本轮未启动新的子 Agent；原因是 Spark 仍在额度限制窗口内，且主线程没有发现需要代码编辑的 P0/P1。

### 主线程复核

- `seller_buyer_management_seed.sql` 当前 owner 默认授权不再包含 `seller:product:*` / `buyer:product:*`，只保留账号、登录日志、操作日志、会话、部门和角色列表类基础权限。
- 20260604 product 权限 seed 当前只创建 hidden `F` 菜单定义，不再写 `seller_role_menu` / `buyer_role_menu` 给 owner。
- `20260610_terminal_owner_product_permission_cleanup.sql` 已用 count/signature fail-closed、事务内精确删除和完成断言固定历史 owner product 授权清理边界。
- `20260610_terminal_portal_home_menu_seed.sql` 已固定 `seller:portal:home` / `buyer:portal:home` 的页面菜单签名、ID 段、端内权限前缀和 owner 首页授权断言。
- `Portal/Home` 当前保留最小 portal 首页框架，不再承载商品 schema / distribution 样板。

### 验证

- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest,PortalPermissionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；99 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-seller-portal-product-template.mjs`：通过。
- `node scripts\check-buyer-portal-product-template.mjs`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `npx jest --config jest.config.ts --runTestsByPath tests\portal-home-error-handling.test.ts tests\portal-product-schema-preview.test.ts tests\admin-auth-sidecar-contract.test.ts tests\portal-session-request.test.ts --runInBand`：通过；4 suites、73 tests。

### 结论

- `P0` 未见新增。
- 未坐实新的 `P1` 代码缺口；本轮无业务代码修改。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：三端 P0/P1 主线程验证收口

时间：2026-06-10 17:10，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只判断 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 用户最新模型规则已确认：只读、检查、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮未启动新的子 Agent；原因是当前检查仍在 Spark 额度限制窗口内，且未坐实需要代码实现的 P0/P1。
- 本轮未把只读任务切给 `gpt-5.4`。

### 主线程复核

- 免密代入：复核 terminal 级 Redis key、hash token、一次性消费、跨端票据 fail-closed 和结构化审计字段。
- portal 权限：复核 `PortalPreAuthorizeAspect`、`PortalPermissionChecker`、seller/buyer controller 与 permission service，确认主体和账号来自端 token/session。
- React guard：复核端 token storage、portal 401、direct-login 失败处理、remote menu 空权限 fail-closed。
- 商品 portal：复核 seller/buyer 商品 portal 后端服务、SQL/service 合同和前端 service/模板测试覆盖。

### 验证

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npm test -- --runTestsByPath ... --runInBand`：失败；当前 `npm test` 是三端 gate 包装，不支持透传具体 Jest 文件，已改用 `npx jest --config jest.config.ts ...`。
- `npx jest --config jest.config.ts --runTestsByPath tests\getrouters-authority-contract.test.ts tests\remote-menu-route-guard.test.ts tests\terminal-session-token.test.ts tests\portal-unauthorized-redirect.test.ts --runInBand`：通过；4 suites、39 tests。
- `mvn -pl ruoyi-framework,ruoyi-system,seller,buyer -am "-Dtest=PortalDirectLoginSupportTest,PortalTokenSupportTest,PortalPermissionCheckerTest,PortalLogAspectContractTest,PortalPreAuthorizeAspectTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；154 tests。
- `mvn -pl seller,buyer -am "-Dtest=SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,SellerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；66 tests。
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest,RouterVoPermissionContractTest,TerminalAccountIsolationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；86 tests。
- `mvn -pl ruoyi-system,seller,buyer,product -am "-Dtest=PortalProductEndpointPermissionContractTest,SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest,ProductDistributionMapperContractTest,ProductModuleBoundaryContractTest,ProductPortalSchemaServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；45 tests。
- `npx jest --config jest.config.ts --runTestsByPath tests\portal-session-request.test.ts tests\portal-product-schema-preview.test.ts tests\product-distribution-permission-guard.test.ts --runInBand`：通过；3 suites、42 tests。
- `git diff --check`：通过，仅有既有 LF/CRLF 提示。

### 结论

- `P0` 未见新增。
- 未坐实新的 `P1` 代码缺口；本轮无业务代码修改。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
## 追加检查点：运行态 API 与远程库只读复核

### 范围

- 继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为方向。
- 本轮只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器、截图、DOM、UI 细调。

### 子 Agent

- 本轮未启动新的子 Agent。
- 当前模型规则：只读、检查、审查、探索类子 Agent 使用 `GPT-5.3-Codex-Spark` / `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt5.4` / `gpt-5.4`。
- 本轮没有坐实需要代码编辑的 P0/P1，因此未启动 `gpt5.4` 实现子 Agent。

### 运行态证据

- 后端 `8080` 和前端 `8001` 均在监听。
- 后端配置来源确认：`active: druid`，MySQL / Redis 运行变量来自 `.env.local`，未记录任何连接明文或密钥。
- API 验证产生了管理端登录、免密票据、端内登录和 portal session 运行态记录；未执行数据库 DDL/DML，未读取或写入 Redis。
- 管理端卖家列表返回 `code=200`，摘要为 `total=14`、`rows=5`。
- 管理端买家列表返回 `code=200`，摘要为 `total=11`、`rows=5`。
- seller 端免密代入成功，票据有效期 `30` 分钟，`/seller/getInfo` 返回权限数 `7`、角色数 `1`，`/seller/getRouters` 顶层路由数 `1`，首路由 `SellerPortalHome`。
- buyer 端免密代入成功，票据有效期 `30` 分钟，`/buyer/getInfo` 返回权限数 `7`、角色数 `1`，`/buyer/getRouters` 顶层路由数 `1`，首路由 `BuyerPortalHome`。

### 远程库只读证据

- 表计数摘要：`seller_account=35`、`buyer_account=35`、`seller_role=42`、`buyer_role=37`、`seller_menu=11`、`buyer_menu=11`、`seller_dept=4`、`buyer_dept=4`。
- `seller_account.password` / `buyer_account.password` 均为 `varchar(100) not null` 且无默认空串，空密码行数均为 `0`。
- `seller_account` / `buyer_account` 当前未发现 `user_id` 列。
- 端内角色菜单、账号角色、菜单 ID 区间、perms 前缀和页面 component 检查均为 `0` 异常。
- 发现 `3` 条 seller 端账号、`1` 条 buyer 端账号与 `sys_user.user_name` 重名；对应旧 `sys_role` 的 `seller` / `buyer` 角色均为 `status=1`、`del_flag=2`。这说明当前运行态不复用 `sys_user`，但后续可单独评估是否需要 guarded legacy sys_user cleanup。

### 验证命令

- `rg` seller/buyer 生产代码 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` / `sys_user_role` / `sys_role_menu` / `SysUser` / `SysRole` / `SysMenu` / `SysDept` / `LoginUser` / `PortalAccountSupport` / `PortalAccountMapper` / `seller_account.user_id` / `buyer_account.user_id`：无命中。
- `rg` seller/buyer 生产代码裸单参账号查询入口：无命中。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `npx jest --config jest.config.ts --runTestsByPath tests\partner-management-contract.test.ts tests\portal-session-request.test.ts tests\terminal-session-token.test.ts tests\remote-menu-route-guard.test.ts tests\admin-auth-sidecar-contract.test.ts --runInBand`：5 suite / 88 tests 通过。
- `mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,TerminalSqlIsolationContractTest,TerminalRoleMenuMapperIsolationContractTest,PortalDirectLoginSupportTest,PortalPermissionCheckerTest,PortalPermissionSupportTest,PortalSelfServiceSurfaceContractTest,PortalPasswordChangeContractTest,AdminAccountPermissionUiContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,SellerServiceImplTest,SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,BuyerServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，后端合计 230 tests。

### 结论

- 未发现新的代码层 P0/P1。
- 本轮新增的 live API 证据证明 seller/buyer 端 `direct-login -> portal token -> getInfo/getRouters` 均闭环。
- `sys_user` 重名残留暂按 legacy cleanup 记录项处理，不阻塞当前 P0/P1 快速推进。

## 追加检查点：Spark 额度失败后复跑三端总门禁

### 范围

- 继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为方向。
- 本轮只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器、截图、DOM、UI 细调。

### 子 Agent

- 尝试启动 2 个只读 explorer，均使用 `gpt-5.3-codex-spark`。
- 2 个 explorer 均因 Spark 额度限制失败，提示需等到 `Jun 17th, 2026 2:16 AM` 或切换模型。
- 2 个失败 explorer 均已关闭。
- 本轮未把只读任务切给 `gpt5.4`；代码编辑类才使用 `gpt5.4`。

### 主线程检查

- 最新记录回扫未发现三端账号权限主线仍开放的当前 P0/P1；历史 P1 或相邻业务 P1 不作为本轮阻塞。
- `Seller/Portal` / `Buyer/Portal` sidecar 与端内菜单 seed 组件路径一致，均复用 `Portal/Home`。
- `TODO/FIXME/return null` 等关键词回扫未坐实新的账号权限 P0/P1。

### 验证

- `npm run verify:three-terminal`：通过。
- 前端 27 suites / 220 tests 通过。
- 后端 reactor test-compile 通过，三端 manifest 中后端合同链路通过。
- guard：portal token、partner management、seller/buyer portal product、product upstream mirrors 均通过。
- `Jest did not exit one second after the test run has completed` 仍出现，但命令退出码为 `0`；本轮按 P2 测试清理噪音记录。

### 结论

- 未发现新的代码层 P0/P1。
- 本轮未改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：manifest 与接口覆盖缺口复核

### 范围

- 继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为方向。
- 本轮只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器、截图、DOM、UI 细调。

### 子 Agent

- 本轮未启动新的子 Agent。
- 当前规则仍为只读、检查、审查、探索类使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类使用 `gpt-5.4`。
- 未坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` worker。

### 主线程检查

- 前端 manifest：27 个 `.test.ts` 全部登记，无漏登。
- 后端 manifest：10 个相关后端模块下 80 个 `*Test.java` 全部登记，无漏登。
- 后端 seller/buyer controller：122 个 Mapping 均有 admin 权限、portal 权限、匿名标记或允许匿名登录审计，无裸接口。
- 前端 seller/buyer service：导出函数 `41/41` 对称，接口路径归一化后 `28/28` 对称。
- portal service 身份范围参数仍集中剥离，未发现页面或 utils 新增透传。

### 结论

- 未发现新的 P0/P1。
- 本轮未改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：OWNER 闭环、role-menu 校验与自助日志 DTO 复核

### 范围

- 继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为方向。
- 本轮只补查三端账号权限框架 P0/P1：OWNER 主账号默认角色/菜单授权闭环、端内 role-menu 写入隔离、自助日志 DTO 脱敏、相关 SQL/manifest 覆盖。
- 不处理冻结旁路：logistics、system/customer channel、商品/库存/财务/外部系统完整业务、浏览器 UI、DOM/截图、三物理前端拆分。

### 子 Agent

- 本轮未启动子 Agent。
- 执行规则仍为只读检查、审查、探索类使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类使用 `gpt-5.4`。

### 主线程检查

- OWNER 闭环：seller/buyer service 都在主账号创建/维护路径绑定 owner 角色，并校验 owner 默认菜单权限完整。
- role-menu 隔离：当前合同覆盖 role-menu mapper 只能使用当前端表和当前端主体/菜单范围。
- 自助日志 DTO：端内自助登录日志/操作日志返回 `PortalOwnLoginLogProfile` / `PortalOwnOperLogProfile`，由合同固定不泄漏内部审计模型。
- manifest：OWNER、role-menu、端内自助日志和 SQL guard 相关后端测试均已登记。

### 验证

- `mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalSeedPermissionContractTest,TerminalRoleMenuMapperIsolationContractTest,PortalSelfServiceSurfaceContractTest,PortalSelfAuditSerializationTest,PortalHomeProfileSerializationTest,TerminalSqlIsolationContractTest,SqlExecutionGuardContractTest,SellerServiceImplTest,BuyerServiceImplTest,SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`ruoyi-system` 104 tests、`seller` 88 tests、`buyer` 89 tests。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。

### 结论

- 未发现新的 P0/P1。
- 本轮未改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：sys_* 混用、Redis key 与 portal 范围证据补强

### 范围

- 继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为方向。
- 本轮只补查三端账号权限框架 P0/P1：端内 `sys_*` 混用、直登 Redis key、portal 前端身份范围、401 串端、manifest 覆盖。
- 不处理冻结旁路：logistics、system/customer channel、商品/库存/财务/外部系统完整业务、浏览器 UI、DOM/截图、三物理前端拆分。

### 子 Agent

- 本轮未启动子 Agent。
- 执行规则仍为只读检查、审查、探索类使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类使用 `gpt-5.4`。
- 目标工具 objective 文本里的旧子 Agent 描述视为历史残留，不作为本轮执行规则。

### 主线程检查

- seller/buyer 端内生产目录未命中 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 或对应 `Sys*` / `ISys*` 依赖。
- `PortalDirectLoginSupport` 生产代码只读取 `portal_direct_login:{terminal}:{token_hash}`；旧 key 只用于删除残留，不用于认证读取。
- `react-ui/src/services/portal/session.ts` 统一过滤主体/账号范围参数，portal API 不由前端传 `sellerId/buyerId/accountId` 决定数据范围。
- `portalRequest.ts` 将 portal 401 限定到非 admin 的 `/api/seller/**` / `/api/buyer/**`，admin 前缀仍走管理端；`direct-login` API 只精确匹配。
- 三端关键后端合同、前端测试和 guard 脚本均在 `three-terminal.manifest.json` 登记。

### 验证

- `npx jest --config jest.config.ts --runTestsByPath tests\terminal-session-token.test.ts tests\portal-session-request.test.ts tests\portal-unauthorized-redirect.test.ts tests\portal-direct-login-message.test.ts tests\remote-menu-route-guard.test.ts tests\partner-management-contract.test.ts --runInBand`：6 suites / 75 tests 通过。
- `mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,PortalDirectLoginAuthContractTest,TerminalRouteOwnershipTest,TerminalSqlIsolationContractTest,PortalAnonymousEndpointContractTest,AdminDirectLoginPermissionContractTest,PortalDirectLoginSupportTest,PortalTokenSupportTest,TokenServiceTerminalIsolationTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`ruoyi-system` 59 tests、`ruoyi-framework` 1 test。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。

### 结论

- 未发现新的 P0/P1。
- 本轮未改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：direct-login 子路径公开范围 P1 修复

### 范围

- 继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为方向。
- 本轮只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器、截图、DOM、UI 细调。

### 子 Agent

- 尝试启动 2 个只读 explorer，模型均为 `gpt-5.3-codex-spark`。
- 2 个 explorer 均因 Spark 额度限制失败并已关闭。
- 本轮未将只读任务切给 `gpt5.4`；修复由主线程完成，未启动代码编辑类子 Agent。

### P1 修复

- 问题：`react-ui/src/wrappers/RemoteMenuRouteGuard.tsx` 把 `/seller/direct-login/*`、`/buyer/direct-login/*` 当成公共 portal 路径；但正确口径是 `login` / `direct-login` 精确匹配，只有 `/{terminal}/portal` 子树允许前缀匹配。
- 修复：
  - 拆分 `PUBLIC_PORTAL_EXACT_ROUTE_PATHS` 与 `PUBLIC_PORTAL_PREFIX_ROUTE_PATHS`。
  - `direct-login` 子路径不再公共放行，会回落到 `/seller` / `/buyer` 静态权限。
  - 补充 `remote-menu-route-guard.test.ts` 断言。
  - 更新 `check-portal-token-isolation.mjs` 与 `check-partner-management-template.mjs` 的 guard 常量检查。

### 验证

- `npx jest --config jest.config.ts --runTestsByPath tests\remote-menu-route-guard.test.ts tests\portal-session-request.test.ts tests\terminal-session-token.test.ts --runInBand`：3 suites / 44 tests 通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npm run verify:three-terminal`：通过；前端 27 suites / 220 tests，后端 reactor `test-compile` 与 manifest 后端合同测试均通过。

### 结论

- 已修复一个路由 guard 公开范围过宽的 P1。
- 未发现新的 P0/P1。
- 本轮未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。

## 追加检查点：子 Agent 模型规则更新后的只读复核

### 范围

- 继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为方向。
- 本轮只判断三端账号权限框架 P0/P1：编译、guard、接口、权限、串端、service/字段缺失、裸账号查询、SQL fail-closed、菜单 ID 段、权限前缀和 OWNER 授权闭环。
- 不做浏览器、截图、DOM、UI 细调；不继续扩展 logistics、system/customer channel、商品/库存/财务/外部系统完整业务或三物理前端拆分。

### 子 Agent

- 最新规则：只读检查、审查、探索类子 Agent 使用 `GPT-5.3-Codex-Spark`；代码编辑、实现、修复类子 Agent 使用 `gpt5.4`。工具模型 id 对应 `gpt-5.3-codex-spark` / `gpt-5.4`。
- 规则更新后，已关闭此前未采纳的 2 个只读 `gpt-5.4` explorer。
- 按新规则尝试启动 6 个只读 `gpt-5.3-codex-spark` explorer，均因 Spark 额度限制失败并已关闭。
- 本轮未把只读任务回退到 `gpt5.4`，未启动代码编辑类子 Agent。

### 主线程检查

- `AGENTS.md` 已写入最新模型规则。
- controller 权限扫描：seller/buyer admin 与 portal Mapping 均有 admin 权限、portal 权限或允许匿名登录审计。
- mapper/service 范围扫描：未发现裸账号 ID 查询；账号相关查询、锁定、重置密码、会话、强退、直登仍带主体 ID。
- 前端 guard 扫描：portal 公开路径精确/前缀拆分存在，`direct-login` 子路径不被公共放行；空权限仍 fail-closed。
- SQL 扫描：三端账号权限相关 SQL 均存在确认/预期 token 标记和 `45000` fail-closed。

### 验证

- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，81 tests / 0 failures。

### 结论

- 未发现新的 P0/P1。
- 本轮未改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
