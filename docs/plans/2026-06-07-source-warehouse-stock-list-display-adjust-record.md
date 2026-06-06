# 来源仓库库存列表展示调整记录

日期：2026-06-07

## 目标

按业务视角调整「库存管理 / 来源仓库库存」列表展示，减少外部系统技术信息，突出主仓、仓库、SKU、库存数量和配对状态。

## 改动范围

- 前端将「来源系统」改为「来源主仓」，展示 `masterWarehouseName`，不再展示 `systemKindLabel` 和 `connectionCode`。
- 前端库存口径改为 Tabs，顺序为：综合库存、产品库存、退货库存、箱库存。
- 前端库存属性从 `0/1` 显示改为中文：`0 = 正品`，`1 = 次品`。
- 前端表格列顺序调整为：来源主仓、来源仓库、来源 SKU、商品名称、库存属性、库存数量、批次库位、系统仓库、商城 SKU、客户、配对状态、同步状态、时间。
- 后端只读查询新增 `masterWarehouseKeyword`，用于按来源主仓名模糊筛选。

## 边界

- 未修改上游库存同步、落库、外部 API client。
- 未修改库存快照表 DDL。
- 未新增库存流水或平台真实库存逻辑。
- 本次只调整来源仓库库存只读列表展示和读查询筛选。

## 验证记录

- `npx biome lint src/pages/Inventory/SourceWarehouseStock/index.tsx src/services/integration/sourceWarehouseStock.ts`：通过。
- `npm run tsc -- --pretty false`：通过。
- `mvn -pl integration -am -DskipTests compile`：通过。
- 直连后端接口 `/integration/admin/source-warehouse-stocks/list?pageNum=1&pageSize=2&inventoryScope=COMPREHENSIVE&inventoryAttribute=0`：返回 `code=200`，样例数据包含 `masterWarehouseName=CA012`、`inventoryAttribute=0`。
- 主仓名负向筛选 `/integration/admin/source-warehouse-stocks/list?...&masterWarehouseKeyword=NO_SUCH_MASTER_WAREHOUSE_20260607`：返回 `total=0`。
- 浏览器打开 `/inventory/source-warehouse-stock`：已显示「来源主仓」，Tabs 顺序为「综合库存 / 产品库存 / 退货库存 / 箱库存」，表格行显示 `CA012` 和 `正品`，未显示 `领星WMS` 或 `LX-CA012`。
