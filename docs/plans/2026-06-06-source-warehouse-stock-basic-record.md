# 来源仓库库存基础处理记录

日期：2026-06-06

## 当前目标

将库存管理下原「关联仓库库存」改成「来源仓库库存」，并补齐该菜单的只读基础页面和只读查询接口。

本轮仍不负责上游系统库存拉取、不负责库存快照表设计、不负责同步落库、不负责平台真实库存账本。

## 已确认边界

- 「来源仓库库存」用于展示从上游主仓系统同步并落库后的来源库存快照，类似「来源商品库」展示来源商品快照。
- 上游库存的拉取、适配、落库由上游系统管理侧统一处理。
- 本菜单只读取上游系统管理侧已经落库的数据。
- 该菜单不是平台真实库存账本，不承担库存总览、库存流水、手工调整、订单锁定或发货扣减。
- 本轮页面不提供“同步库存”按钮，不创建 `inventory:sourceWarehouse:sync` 权限。

## 本轮完成

- 菜单名：`来源仓库库存`
- path：`source-warehouse-stock`
- routeName：`SourceWarehouseStock`
- component：`Inventory/SourceWarehouseStock/index`
- 权限标识：`inventory:sourceWarehouse:list`
- 只读接口：`GET /integration/admin/source-warehouse-stocks/list`
- 前端页面：`react-ui/src/pages/Inventory/SourceWarehouseStock/index.tsx`
- 前端 service：`react-ui/src/services/integration/sourceWarehouseStock.ts`
- 前端静态路由：`/inventory/source-warehouse-stock`

对应 SQL：

```text
RuoYi-Vue/sql/20260606_source_warehouse_stock_menu_rename.sql
```

## 后端读取规则

- 复用 integration 模块现有来源库存快照 DTO 和 Mapper 查询。
- 新增独立管理端只读 Controller：`AdminSourceWarehouseStockController`。
- 接口权限使用库存菜单权限：`inventory:sourceWarehouse:list`。
- 列表可按来源系统编号、来源仓库、SKU/商品、同步状态、仓库配对状态、SKU 配对状态、库存口径、库存属性筛选。
- `connectionCode` 可选；不传时跨来源系统展示已经落库的来源库存快照。
- 如果传入 `connectionCode`，Service 会先校验该上游系统存在。

## 前端展示规则

- 使用 ProTable 标准列表页。
- 复用 `getPersistedProTableSearch(...)`、`getProTablePagination(...)`、`getProTableScroll(...)` 和 `getProTableColumnsState(...)`。
- 只展示来源库存快照和配对结果，不允许在页面内发起同步或落库。
- 同步状态、配对状态复用 integration 共享选项。
- 库存口径集中维护在 integration 共享选项中，当前支持 `PRODUCT`、`BOX`、`RETURN`、`COMPREHENSIVE`，未识别的上游 code 按原始 code 展示。

## 本轮明确不做

- 不新增库存快照表 DDL。
- 不新增上游库存同步任务。
- 不新增领星库存 API client。
- 不新增来源库存写入逻辑。
- 不新增同步按钮或 `inventory:sourceWarehouse:sync` 权限。
- 不执行数据库 DDL / DML。

## 后续接入条件

等上游系统管理侧完成并确认：

1. 上游库存快照表最终字段和唯一键。
2. 上游库存同步和落库链路。
3. 状态 code、库存口径 code、配对关联方式。
4. 本地运行库已执行对应 SQL。

之后本菜单只需要按已落库字段继续扩展展示、筛选、导出或详情，不在菜单内重复处理上游适配和落库。

当前运行库如果尚未创建 `upstream_system_sku_inventory_snapshot`，授权后的列表接口会返回 SQL 表不存在错误；这是上游库存快照表未落地的环境前置条件，不在本轮通过本菜单补 DDL。
