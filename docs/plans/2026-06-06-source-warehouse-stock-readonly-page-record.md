# 来源仓库库存只读页面执行记录

日期：2026-06-06

## 目标

在不接管上游库存同步和落库的前提下，为「库存管理 / 来源仓库库存」补齐可进入的只读列表页和基础后端查询入口。

## 改动范围

- 后端新增 `AdminSourceWarehouseStockController`。
- 后端复用 `IUpstreamSystemService.selectSourceWarehouseStockList(...)`。
- Mapper 列表查询支持不传 `connectionCode` 时跨来源系统读取快照。
- 前端新增 `Inventory/SourceWarehouseStock/index.tsx`。
- 前端新增 `services/integration/sourceWarehouseStock.ts`。
- 前端新增静态路由 `/inventory/source-warehouse-stock`，避免直达和刷新时落到 404。
- 菜单 SQL 将 `2421` 指向真实页面。
- integration 共享选项新增库存口径展示配置。

## 边界

- 未新增库存表 DDL。
- 未新增库存同步按钮。
- 未新增库存同步权限。
- 未新增外部系统 API client。
- 未新增库存流水或平台真实库存账本逻辑。

## 权限

- 页面和只读接口使用 `inventory:sourceWarehouse:list`。
- 上游系统管理内部的库存同步权限不属于本菜单。

## 复用检查

- 页面复用全局 ProTable 查询、分页、滚动和列状态工具。
- 状态和配对选项复用 `react-ui/src/services/integration/constants.ts`。
- 后端读取复用 integration 模块已有来源库存 DTO、Query、Mapper 和 Service。

## 验证记录

- `npx biome lint config/routes.ts config/routes.js src/pages/Inventory/SourceWarehouseStock/index.tsx src/services/integration/sourceWarehouseStock.ts src/services/integration/constants.ts src/pages/UpstreamSystem/constants.ts src/pages/UpstreamSystem/components/SkuInventoryPanel.tsx`：通过。
- `npm run tsc -- --pretty false`：通过。
- `mvn -pl integration -am -DskipTests clean compile`：通过，确认新增 Controller、DTO 字段和 Mapper 查询可编译。
- `codegraph sync .`：通过。
- `rg "syncUpstreamInventory|syncWarehouseStocksOnly\\(|inventory:sourceWarehouse:sync|sourceWarehouseStock.*sync|/inventory/sync"`：未发现本菜单新增同步按钮、同步权限或同步调用。
- `GET http://127.0.0.1:8080/integration/admin/source-warehouse-stocks/list?pageNum=1&pageSize=10`：未登录返回业务 `401`，确认路由存在且受权限保护。
- 浏览器打开 `http://127.0.0.1:8001/inventory/source-warehouse-stock`：已渲染查询区和表格列，未再落到 404。
- 使用本地登录 token 请求 `/api/integration/admin/source-warehouse-stocks/list?pageNum=1&pageSize=10`：接口进入授权后查询，当前运行库 `fenxiao` 缺少 `upstream_system_sku_inventory_snapshot` 表，返回业务 `500`；该表由上游系统管理侧后续建表和落库，本轮不补 DDL。

未执行数据库 DDL/DML，未启动库存同步任务，未调用外部系统 API。
