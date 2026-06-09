# 商品中心实现与执行记录

## 目标

- 在当前管理端新增“商品中心”菜单，用于提前内测买家端商品列表。
- 商品中心只展示已上架商品，字段按买家可见口径收敛，不展示客户 SKU、供货价、卖家、管控原因等卖家/管理端敏感字段。
- 前端列表与详情按可复用方式实现，后续可迁移到买家端。

## 实现范围

- 后端：
  - 新增 `AdminProductCenterController`，后台路由为 `/product/admin/product-center`。
  - 新增 `IProductCenterService` / `ProductCenterServiceImpl` 和 `ProductCenterProduct/Sku/Warehouse/Attribute/Query` DTO。
  - 复用 `IProductDistributionService.selectOnSaleProductList/selectOnSaleProductById/selectOnSaleSkuList`，只映射买家可见字段。
  - `ProductDistributionServiceImpl.selectOnSaleProductById` 补齐属性、图片和仓库数据，供商品中心详情真实展示。
  - `ProductDistributionMapper.xml` 的已上架商品查询支持系统 SPU/SKU 条件。
- 前端：
  - 新增共享组件 `react-ui/src/components/ProductCenter/ProductCenterPage.tsx`。
  - 新增共享详情 `ProductCenterDetailModal.tsx`，复用买家详情图文流。
  - `BuyerProductPreviewModal` 增加 `preview/real` 模式；商品中心使用真实模式，不显示预览提示、样式价和假库存。
  - 管理端入口 `react-ui/src/pages/Product/ProductCenter/index.tsx` 只负责注入 admin 权限和 admin service。
  - 新增 `react-ui/src/services/product/productCenter.ts` 和类型 `react-ui/src/types/product/product-center.d.ts`。
- SQL：
  - 新增 `RuoYi-Vue/sql/20260608_product_center_menu_seed.sql`。
  - 写入 `sys_menu` 菜单 `2404 商品中心` 和按钮 `2487 商品中心查询`。
  - 不新增业务表，不执行 DDL 建表。
- 文档与合同：
  - 方案：`docs/plans/2026-06-08-product-center-reusable-page-design.md`。
  - 复用台账：`docs/architecture/reuse-ledger.md`。
  - 合同测试：`ProductCenterServiceImplTest`、`product-center-contract.test.ts`、`ProductAdminRouteContractTest`、`SqlExecutionGuardContractTest`。

## 数据源确认

- 后端激活配置：`application.yml` 当前 active profile 为 `druid`。
- MySQL URL 来源：`application-druid.yml` 使用 `${RUOYI_DB_URL:}`，实际运行值来自 `.env.local`。
- 本次 SQL 目标：`.env.local` 中的远端 MySQL，连接地址已脱敏记录为远端 MySQL 地址，不包含明文主机和参数。
- Redis 来源：`.env.local`，`RUOYI_REDIS_HOST=114.132.156.75`，`RUOYI_REDIS_PORT=6379`。
- 本次影响：远端 MySQL `sys_menu` 写入/更新两条菜单记录；未写 Redis。

## SQL 执行

- 执行方式：本机无 `mysql` CLI，使用 Maven 本地仓库 MySQL JDBC 驱动和临时 Java runner 执行。
- 确认 token：`@confirm_product_center_menu_seed = 'APPLY_PRODUCT_CENTER_MENU_SEED'`。
- 首次执行失败原因：SQL guard 中 MySQL 临时表在同一查询内重复打开，触发 `Can't reopen table: 'seed'`；失败发生在事务提交前，未完成菜单写入。
- 修复方式：
  - ID/签名 guard 改为单次 join。
  - 完成校验改为固定目标数 `2`，避免重复 count 同一临时表。
- 最终执行结果：
  - `executedStatements=26`
  - `2404 商品中心 2060 center Product/ProductCenter/index product:center:list`
  - `2487 商品中心查询 2404 # product:center:query`

## 运行态验证

- 前端：`http://127.0.0.1:8001` 已有服务运行，HTTP 200。
- 后端：
  - 旧 8080 进程不包含新类，已停止旧进程。
  - 已重新打包：`mvn -pl ruoyi-admin -am -DskipTests package`。
  - 已重启：`.\start-backend-local.ps1 -Restart`。
  - `http://127.0.0.1:8080` HTTP 200。
  - `product-3.9.2.jar` 已包含 `AdminProductCenterController`、`ProductCenterServiceImpl`、`ProductCenterProduct`。
- API：
  - 未登录请求 `/product/admin/product-center/list?pageNum=1&pageSize=1` 返回 `code=401`，路由非 404。
  - 默认 admin 登录成功后，请求商品中心列表返回 `code=200`、`total=8`、`rowCount=1`。
  - `/getRouters` 已包含 `Product/ProductCenter/index` 和 `product:center:list`。

## 验证命令

- `mvn -pl product -am -Dtest=ProductCenterServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：通过，5 tests。
- `mvn -pl ruoyi-system -am "-Dtest=ProductAdminRouteContractTest,SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：通过，69 tests。
- `mvn -pl buyer -am -Dtest=BuyerPortalProductServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：通过，9 tests。
- `npm exec jest -- --config jest.config.ts --runTestsByPath tests/product-center-contract.test.ts tests/product-distribution-permission-guard.test.ts tests/portal-product-schema-preview.test.ts --runInBand`
  - 结果：通过，3 suites / 15 tests。
- `npm run guard:buyer-portal-product`
  - 结果：通过。
- `npm run tsc -- --pretty false`
  - 结果：通过。
- `npm run verify:three-terminal`
  - 结果：通过，前端 16 suites / 108 tests；后端三端合同通过。
- `codegraph sync .`
  - 结果：`Already up to date`。

## 权限与三端隔离

- 管理端权限：`product:center:list`、`product:center:query`。
- 商品中心管理端接口继续走 `@PreAuthorize("@ss.hasPermi('product:center:*')")`，不使用 seller/buyer portal 权限。
- 前端共享组件不直接写 `access.hasPerms`，只通过 props 接收权限和 service，便于后续买家端替换数据源。
- 未新增卖家端/买家端账号、角色、菜单、日志、会话或 Redis key。

## 字段边界

- 展示字段保留：商品中文名、英文名、系统 SPU、系统 SKU、类目、卖点、销售价、币种、库存、发货仓库、详情图文、商品参数。
- 明确不展示：客户 SKU、卖家 ID/名称、供货价、管控原因、供货侧成本字段。
- `ProductCenterServiceImplTest` 和 `product-center-contract.test.ts` 已固定这些边界。

## 文件规模判断

- `ProductCenterPage.tsx`：273 行，低于 300 行。
- `20260608_product_center_menu_seed.sql`：158 行。
- `ProductCenterServiceImpl.java`：342 行，超过 300 行触发检查；当前职责单一，集中在“商城商品域对象映射为买家可见 DTO”，拆分会增加理解成本，暂不拆分。

## 工具与残留说明

- Browser in-app 工具：本轮工具发现未暴露可用 Browser 控制工具，未做浏览器截图/DOM 点击验证；已用前端 dev server、TypeScript、Jest、三端总验证和后端 API 运行态替代验证。
- 子 Agent：未使用。
- CodeGraph：已同步。
