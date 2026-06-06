# 商城商品 SKU 来源配对计划

## 当前数据体检

- 当前运行库：`fenxiao`，本次只读查询，未执行 DDL/DML。
- 来源 SKU 原始行：`10736` 条，全部 `ACTIVE`。
- 来源连接：
  - `LX-CA012` / `CA012`：`5402` 条。
  - `LX-NY013-3275A1E1` / `NY013`：`5334` 条。
- 来源审核状态：全部 `approve_status = 2`。
- 来源产品类型：全部 `product_type = 0`。
- 来源申报币种：全部 `USD`。
- 仓库映射：
  - `CA012` 已映射系统仓 `warehouse_id=1`，`official / USD / 正常`。
  - `NY013` 已映射系统仓 `warehouse_id=2`，`official / USD / 正常`。
  - `10736` 条来源 SKU 均可映射到正常官方仓。
- 来源库当前聚合后约 `5649` 个来源 SKU 组：
  - `5087` 个来源 SKU 组同时存在于 2 个官方仓。
  - `562` 个来源 SKU 组只存在于 1 个官方仓。
  - 原始 `master_sku` 跨连接重复数：`5301`。
- 尺寸重量：
  - `product_length / product_width / product_height / product_weight` 完整覆盖 `10736` 条。
  - `wms_length / wms_width / wms_height / wms_weight` 完整覆盖 `0` 条。
  - 所以当前阶段不能把 WMS 字段当作唯一可信尺寸来源，只能先用产品尺寸兜底，并标记来源。
- 图片与识别码：
  - `image_url` 覆盖 `0` 条。
  - `source_payload_hash` 覆盖 `10736` 条。
  - `main_code` 覆盖 `10736` 条。
  - `fnsku` 覆盖 `0` 条。
- 当前 `upstream_system_sku_pairing` 为 `0` 条；商城商品 SKU 尚未和来源 SKU 建立正式绑定。

## 关键判断

1. 商品列表侧不应该直接绑定单条 `connection_code + master_sku` 原始行。
   当前大量来源 SKU 同时存在于 CA012 和 NY013。如果绑定单行，会把同一个真实来源 SKU 在不同官方仓的可发货能力拆碎。

2. 商品列表侧应该绑定“来源 SKU 组”，并保存该来源 SKU 组对应的官方仓明细。
   当前来源商品库页面已经有 `sourceSkuGroupKey`、`sourceDimensionGroupKey`、`sourceConnectionCodes`、`sourceWarehouseNames`、`warehouseCount`、`sourceRowCount` 这类聚合概念。商品列表后续应消费这个聚合结果，而不是自己重新拼一套聚合规则。

3. 官方仓商品不再手工选择发货仓库。
   SKU 绑定来源 SKU 组后，由来源 SKU 组下的官方仓明细派生仓库范围、币种和仓库类型。`product_spu_warehouse` 只能作为派生展示或缓存，不作为官方仓绑定主事实。

4. 来源商品库负责来源事实，商品列表负责商城使用关系。
   来源商品库提供可信来源 SKU、官方仓映射、最终尺寸重量和可选状态；商品列表保存“商城 SKU 使用了哪个来源 SKU”的绑定、锁定、换绑权限和操作日志。

## 来源商品库侧依赖

商品列表开始落地前，来源商品库最好先稳定以下接口或字段：

1. 来源 SKU 选择接口。
   - 支持官方仓范围筛选。
   - 支持 SKU、商品名、条码、审核状态、同步状态、绑定状态筛选。
   - 返回分页数据，不能前端拉全量过滤。

2. 稳定来源 SKU 组标识。
   - 返回可落库绑定的稳定 `sourceSkuGroupKey`。
   - 该 key 需要稳定，不建议长期使用易变字段简单拼接。

3. 返回来源 SKU 组下的官方仓明细。
   - `connectionCode`
   - `masterSku`
   - `systemWarehouseId`
   - `systemWarehouseCode`
   - `systemWarehouseName`
   - `warehouseKind`
   - `settlementCurrency`
   - `upstreamWarehouseCode`
   - `upstreamWarehouseName`

4. 返回最终可信尺寸重量。
   - `measureLengthCm`
   - `measureWidthCm`
   - `measureHeightCm`
   - `measureWeightKg`
   - `measureSource`
   当前数据下建议 `measureSource = PRODUCT`，因为 WMS 字段全空。

5. 返回可选状态。
   - `selectable`
   - `unselectableReason`
   - `sourceStatus`
   - `approveStatus`

## 商品列表侧计划

### 阶段 1：表设计

新增商城 SKU 来源绑定主表，建议名：`product_sku_source_binding`。

主要用途：
- 保存商城 SKU 当前绑定的来源 SKU 组。
- 保存绑定快照、锁定状态、换绑状态。
- 承担“卖家不能下架后新建商品换绑”的约束基础。

建议关键字段：
- `binding_id`
- `spu_id`
- `sku_id`
- `seller_id`
- `source_group_key`
- `master_sku`
- `master_product_name_snapshot`
- `source_payload_hash`
- `measure_length_cm`
- `measure_width_cm`
- `measure_height_cm`
- `measure_weight_kg`
- `measure_source`
- `currency_code`
- `bind_status`：`ACTIVE / REPLACED / RELEASED`
- `lock_status`：`UNLOCKED / LOCKED`
- `locked_time`
- `locked_by`
- `replace_reason`
- `create_by / create_time / update_by / update_time`

新增绑定仓库明细表，建议名：`product_sku_source_binding_warehouse`。

主要用途：
- 保存一个商城 SKU 来源绑定下实际可派生出的官方仓列表。
- 支撑列表展示仓库数、仓库类型、币种和后续库存读取边界。

建议关键字段：
- `id`
- `binding_id`
- `sku_id`
- `spu_id`
- `seller_id`
- `connection_code`
- `master_sku`
- `warehouse_id`
- `warehouse_code`
- `warehouse_name`
- `warehouse_kind`
- `settlement_currency`
- `upstream_warehouse_code`
- `upstream_warehouse_name`

约束建议：
- 一个商城 SKU 只能有一个 `ACTIVE` 来源绑定。
- 同一卖家同一 `source_group_key` 只能有一个有效绑定，防止下架后新建商品绕过绑定。
- 管理端换绑时旧绑定置为 `REPLACED`，新绑定置为 `ACTIVE`。

### 阶段 2：后端服务规则

- 官方仓模式新增/编辑商品时，SKU 必须带来源 SKU 绑定。
- 后端根据来源商品库返回的来源 SKU 组快照写入绑定表和绑定仓库明细表。
- `product_sku` 的尺寸重量展示字段由绑定快照生成，不允许官方仓场景手填覆盖。
- `product_sku.currency_code` 来自绑定官方仓币种，不取来源 SKU 申报币种。
- 提交审核或离开草稿时锁定绑定。
- 非草稿状态下，卖家端不能修改、解绑或换绑来源 SKU。
- 管理端可以换绑，但必须填写原因并记录操作日志。

### 阶段 3：前端页面规则

- 仓库类型选择为“官方仓”时，不显示手工仓库选择器。
- SKU 区域改为“选择来源 SKU”生成 SKU 行。
- 选中来源 SKU 后自动带出：
  - 来源 SKU
  - 来源商品名
  - 官方仓范围
  - 币种
  - 尺寸重量
  - 来源状态
  - 绑定状态
- 官方仓 SKU 的长宽高重量置为只读。
- 非草稿编辑页展示绑定信息但不允许卖家换绑。
- 管理端换绑使用专门弹窗，不混在普通编辑保存里。

### 阶段 4：列表展示

SPU 视图建议展示：
- 来源绑定状态。
- 仓库类型：官方仓。
- 仓库数。
- 币种。

SKU 视图建议展示：
- 来源 SKU。
- 来源商品名。
- 来源仓库范围。
- 尺寸重量。
- 绑定锁定状态。

### 阶段 5：操作日志

复用商品列表已有 `product_distribution_operation_log` 机制，新增操作类型：
- `SOURCE_BIND`
- `SOURCE_LOCK`
- `SOURCE_REBIND`
- `SOURCE_RELEASE`
- `SOURCE_BIND_REJECTED`

日志必须记录：
- 操作人。
- 操作来源。
- SPU / SKU。
- 变更前后来源 SKU。
- 变更前后仓库范围。
- 原因。

## 不建议直接复用的现有表

### `upstream_system_sku_pairing`

不建议作为商城 SKU 正式绑定表。

原因：
- 它保存的是 `system_sku` 字符串，不是 `product_sku.sku_id`。
- 没有 `spu_id / sku_id / seller_id`。
- 没有锁定、换绑、释放、审核状态。
- 没有官方仓明细快照。
- 更像来源商品库内部的上游 SKU 配对能力。

### `product_spu_warehouse`

不建议作为官方仓主绑定表。

原因：
- 它是 SPU 级仓库绑定，不能表达 SKU 级来源关系。
- 官方仓仓库范围应该由来源 SKU 组派生。
- 后续可以保留为 SPU 汇总缓存或列表查询辅助，但不能作为绑定主事实。

## 建议先确认的问题

1. 商品列表绑定对象是否正式采用“来源 SKU 组”，而不是单条 `connection_code + master_sku`。
2. 当前 WMS 尺寸全空，第一版是否允许使用产品尺寸作为官方仓可信尺寸，并标记 `measureSource = PRODUCT`。
3. 同一个来源 SKU 组存在多个官方仓时，商城 SKU 是否默认绑定全部官方仓。
4. 下架商品占用来源 SKU 的时间范围：是否永久占用，直到管理端释放或换绑。
5. 当前商品状态是否要新增“待审核”，还是第一版先用离开草稿作为绑定锁定点。
