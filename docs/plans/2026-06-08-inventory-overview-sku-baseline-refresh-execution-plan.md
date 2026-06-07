# 库存总览 SKU 基线刷新 SQL 执行计划

日期：2026-06-08

状态：已确认并执行完成。执行记录见 `docs/plans/2026-06-08-inventory-overview-sku-baseline-refresh-execution-record.md`。

## 背景

当前库存总览基础 SQL 已创建表和页面，但旧回填逻辑从 `product_sku_source_binding` / `product_spu_warehouse` 起步。当前运行库中这两类绑定数据均为 0，导致库存总览读模型为空。

本次修正后，库存总览应从有效商城 SKU 起步：

- 有官方来源绑定和来源库存时，生成 `SKU + 来源主仓` 明细行。
- 有三方仓绑定时，生成 `SKU + 三方仓` 明细行。
- 官方仓缺少来源 SKU 绑定时，生成 `来源SKU未绑定` 占位行。
- 没有任何仓库或来源绑定时，生成 `仓库未配置` 占位行。

## SQL 文件

- `RuoYi-Vue/sql/20260608_inventory_overview_sku_baseline_refresh.sql`

确认 token：

```sql
set @confirm_inventory_overview_sku_baseline_refresh = 'APPLY_INVENTORY_OVERVIEW_SKU_BASELINE_REFRESH';
```

## 影响范围

- 补充 `inventory_status` 字典：
  - `NO_WAREHOUSE`：仓库未配置
  - `SOURCE_UNBOUND`：来源SKU未绑定
- 插入或更新 `inventory_sku_warehouse_stock`：
  - 官方仓来源主仓行
  - 官方仓来源 SKU 未绑定占位行
  - 官方仓未匹配来源库存占位行
  - 三方仓行
  - 仓库未配置占位行
- 重建读模型：
  - 清空并重写 `inventory_overview_sku_read_model`
  - 清空并重写 `inventory_overview_spu_read_model`
  - 清空和重写动作已纳入同一事务，失败时不应提交空读模型

## 非目标

- 不新增表结构。
- 不修改来源仓库库存同步。
- 不修改商品创建流程。
- 不接入订单锁定和出库扣减。
- 不修改第三方仓真实库存同步。

## 预期结果

基于当前已验证数据：

- 有效 `product_spu`：24
- 有效 `product_sku`：69
- `product_sku_source_binding`：0
- `product_spu_warehouse`：0

执行后预期：

- `inventory_sku_warehouse_stock` 至少生成 69 条 `NO_WAREHOUSE` 占位行。
- `inventory_overview_sku_read_model` 至少生成 69 条 SKU 行。
- `inventory_overview_spu_read_model` 至少生成 24 条 SPU 行。
- 库存总览页面不再空表，状态显示为 `仓库未配置`。

## 回滚说明

本脚本不新增表结构，但会写入字典和重建读模型。若需回滚：

1. 删除本次新增的 `NO_WAREHOUSE` / `SOURCE_UNBOUND` 字典数据。
2. 删除 `inventory_sku_warehouse_stock` 中 `warehouse_ref_type in ('NO_WAREHOUSE', 'SOURCE_UNBOUND')` 且尚无业务流水依赖的占位行。
3. 清空并按上一版逻辑重建 `inventory_overview_sku_read_model` / `inventory_overview_spu_read_model`。

如占位行已经产生库存流水，不建议物理删除，应改为状态作废并保留流水追溯。
