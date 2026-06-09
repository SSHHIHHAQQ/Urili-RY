# 商城商品来源库存状态匹配修复记录

## 背景

草稿商品已经绑定官方来源 SKU，但商城商品列表的库存状态仍显示为 `NO_SOURCE`（无来源库存）。

## 根因

商品侧来源绑定表 `product_sku_source_binding` 保存的是来源商品维度：

- `source_sku_group_key`：来源 SKU 组 key，用于占用判断。
- `source_dimension_group_key`：来源 SKU 尺寸组 key，用于尺寸重量和仓库范围推导。
- `master_sku` / `master_product_name_snapshot`：来源 SKU 业务身份快照。

来源库存读模型 `source_warehouse_stock_detail` 的 `source_stock_group_key` 是来源库存组 key，生成规则包含库存范围，并且 key 前缀不同。它不是 `product_sku_source_binding.source_sku_group_key`。

原库存总览刷新逻辑使用：

```sql
d.source_stock_group_key = b.source_sku_group_key
```

直接连接两个不同语义域的 key，导致来源库存无法匹配，最终生成 `UNMATCHED_OFFICIAL` 占位库存行，列表展示为 `NO_SOURCE`。

## 修复内容

调整 `InventoryOverviewMapper.xml` 中官方来源库存匹配逻辑：

- 不再使用 `source_stock_group_key = source_sku_group_key`。
- 改为按来源 SKU 业务身份匹配：
  - `repository_scope`
  - `master_sku`
  - `master_product_name_snapshot`
- 同时覆盖：
  - 保存商品后刷新单个 SPU 库存总览。
  - 来源库存同步后反查受影响 SPU。
  - 删除旧库存行前的当前库存 key 推导。
  - 官方仓无来源库存占位判断。

## 验证

测试：

```powershell
mvn -pl inventory -am "-Dtest=InventoryOverviewRefreshContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl inventory -am test
mvn -pl ruoyi-admin -am -DskipTests package
```

结果：

- `InventoryOverviewRefreshContractTest` 通过。
- `inventory -am test` 通过，共 200 个测试。
- `ruoyi-admin` 打包成功。

运行态验证：

- 重启后端 8080。
- 读取草稿 `spuId=18`，确认 2 个 SKU 均存在 active 来源绑定。
- 来源库存只读接口确认：
  - `KAT03WY-B-G-XS` 来源可用库存 27。
  - `Silver necklace` 来源可用库存 21。
- 对 `spuId=18` 执行原样保存触发库存总览刷新。
- 刷新后商品不再显示 `NO_SOURCE`，两个 SKU 的来源仓均为 `NY013`，来源可用库存分别为 27 和 21。

## 说明

刷新后当前商品库存状态为 `OUT_OF_STOCK`，不是 `NO_SOURCE`。这是因为来源库存已经读取到，但商城侧平台库存总量仍为 0，有效可售库存仍为 0。后续库存模块同步或调整平台库存后，状态会继续按平台可售库存变化。
