# 上游系统 SKU 库存清单为空修复记录
## 背景

上游系统管理的「SKU库存同步清单」页面显示库存条数为 0，且表格无数据。实际运行库中 `upstream_system_sku_inventory_snapshot` 已存在 CA012 和 NY013 的库存快照数据。

## 根因

1. 库存清单接口在 Controller 中先调用若依 `startPage()`，随后 Service 内部先执行了一次 `selectConnectionByCode()` 校验。
2. PageHelper 把分页参数错误地追加到了这次校验查询上，生成了类似 `limit 1 LIMIT ?` 的 SQL，导致接口 500。
3. CA012 的库存同步状态表历史上停留在 `SYNCING`，并且 `active_count/total_count` 为 0；同步失败逻辑还会把旧计数清零，导致页面顶部继续显示 0。

## 修复内容

1. `selectSourceWarehouseStockList` 不再在分页查询链路中额外调用 `selectConnectionByCode()`，避免 PageHelper 拦截错误查询。
2. 库存同步开始时保留上一次成功的库存条数，不再清空 `activeCount/totalCount`。
3. 库存同步失败时也保留上一次成功的库存条数，只更新失败状态和错误信息。
4. 按现有快照数据修正 CA012 库存同步状态：
   - `upstream_system_inventory_sync_state`：恢复为 `FRESH`，有效库存条数 11608。
   - `upstream_system_sync_state` 中 `INVENTORY` 分项：恢复为 `FRESH`，成功处理数 11608。

## 数据影响

本次数据库修正只更新同步状态表，不改库存快照明细，不改领星原始返回数据，不改系统仓库或 SKU 配对关系。

目标运行库：当前后端激活配置对应的 `fenxiao`。

## 验证结果

已执行：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
mvn -DskipTests package
```

结果：

- integration 编译通过。
- 全量后端打包通过，`ruoyi-admin.jar` 已重新生成。
- 后端已通过 `start-backend-local.ps1` 重新启动。

接口验证：

- `GET /integration/admin/upstream-systems/LX-CA012/inventory/list?pageNum=1&pageSize=2`
  - `code=200`
  - `total=11608`
  - `rows=2`
- `GET /integration/admin/upstream-systems/LX-CA012/inventory-sync-state`
  - `status=FRESH`
  - `activeCount=11608`
  - `totalCount=11608`
- `GET /integration/admin/upstream-systems/LX-NY013-3275A1E1/inventory/list?pageNum=1&pageSize=2`
  - `code=200`
  - `total=796`
  - `rows=2`
- `GET /integration/admin/upstream-systems/LX-NY013-3275A1E1/inventory-sync-state`
  - `status=FRESH`
  - `activeCount=796`
  - `totalCount=796`

已额外点查 CA012 SKU `2105115-silver-L`，库存清单返回了不同库存口径的记录，其中包含 `total=2`、`available=2` 的记录。

## 残留风险

同一 SKU 在领星库存接口中会按库存口径、库存属性、批次或库位拆成多行；当前页面展示的是上游库存快照明细，不是按 SKU 汇总后的总库存。后续如果业务需要「SKU 汇总库存」，应新增聚合接口或在现有接口增加汇总视图，不应直接覆盖当前明细口径。
