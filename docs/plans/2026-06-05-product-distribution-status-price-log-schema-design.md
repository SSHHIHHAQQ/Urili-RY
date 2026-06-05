# 商城商品状态、调价与操作日志表设计

## 设计目的

本设计用于支持商城商品列表的三个新规则：

- `停用` 从销售流转状态中拆出，作为独立管控状态。
- `销售价` 不再在新增/编辑商品页维护，改为列表页 SKU 维度调价。
- 商品状态调整、停用/恢复、销售价调整需要有可追溯操作日志。

## 现有表复用判断

- `product_spu`：继续承载 SPU 主体信息和销售流转状态。
- `product_sku`：继续承载 SKU 信息、供货价、销售价和 SKU 销售流转状态。
- `sys_oper_log`：可记录接口级操作，但不适合承载商品字段差异、批量调价结果、SKU 价格前后值。
- `product_config_change_log`：只用于商品分类、属性、类目属性规则等配置变更，不建议混入商品业务数据。

因此本次需要：

- 对 `product_spu` 增加管控状态字段。
- 对 `product_sku` 增加管控状态字段，并允许销售价为空。
- 新增商品业务操作日志表 `product_distribution_operation_log`。

## 销售流转状态

字段仍使用：

- `product_spu.spu_status`
- `product_sku.sku_status`

允许值：

- `DRAFT`：草稿
- `READY`：待上架
- `ON_SALE`：已上架
- `OFF_SALE`：已下架

不再把 `DISABLED` 作为销售流转状态。

流转规则：

- `DRAFT -> READY`
- `READY -> ON_SALE`
- `ON_SALE -> OFF_SALE`

首版不允许直接：

- `OFF_SALE -> ON_SALE`
- `READY -> DRAFT`
- `ON_SALE -> READY`

## 管控状态字段

### product_spu 新增字段

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `control_status` | `varchar(32)` | 是 | `NORMAL` | SPU 管控状态。`NORMAL` 表示正常，`DISABLED` 表示平台停用。只要为 `DISABLED`，商品不得在买家端展示或售卖，也不得执行普通上架操作。 |
| `control_reason` | `varchar(500)` | 否 | `null` | 最近一次停用原因。停用时必填，用于运营和风控追溯。恢复后保留最近原因，不清空。 |
| `control_by` | `varchar(64)` | 否 | `null` | 最近一次停用操作人账号。 |
| `control_time` | `datetime` | 否 | `null` | 最近一次停用时间。 |
| `recover_by` | `varchar(64)` | 否 | `null` | 最近一次恢复操作人账号。 |
| `recover_time` | `datetime` | 否 | `null` | 最近一次恢复时间。 |

建议索引：

- `idx_product_spu_control_status(control_status)`
- `idx_product_spu_status_control(spu_status, control_status)`

### product_sku 新增/调整字段

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `control_status` | `varchar(32)` | 是 | `NORMAL` | SKU 管控状态。`NORMAL` 表示正常，`DISABLED` 表示平台停用。单个 SKU 可独立停用；如果 SPU 停用，SKU 即使自身正常也不可售。 |
| `control_reason` | `varchar(500)` | 否 | `null` | 最近一次 SKU 停用原因。 |
| `control_by` | `varchar(64)` | 否 | `null` | 最近一次 SKU 停用操作人账号。 |
| `control_time` | `datetime` | 否 | `null` | 最近一次 SKU 停用时间。 |
| `recover_by` | `varchar(64)` | 否 | `null` | 最近一次 SKU 恢复操作人账号。 |
| `recover_time` | `datetime` | 否 | `null` | 最近一次 SKU 恢复时间。 |
| `sale_price` | `decimal(18,4)` | 否 | `null` | SKU 销售价。新增/编辑商品时不再填写；列表页调价后写入。SKU 上架前必须有销售价。 |

建议索引：

- `idx_product_sku_control_status(control_status)`
- `idx_product_sku_status_control(sku_status, control_status)`

## 停用与恢复规则

### 停用

- 正常商品任意销售流转状态都可以停用。
- 停用必须填写原因。
- 停用只修改 `control_status`，不改变原销售流转状态。
- `已上架 + 停用` 的商品恢复后会立即重新可售，因此恢复时前端必须二次确认。

### 恢复

- 仅 `control_status = DISABLED` 的商品或 SKU 可恢复。
- 恢复只把 `control_status` 改回 `NORMAL`。
- 恢复不改变原销售流转状态。
- 恢复后如果原状态是 `ON_SALE`，商品或 SKU 会回到可售状态。

## Tabs 查询规则

SPU 视图：

- 待上架：`spu_status = READY and control_status = NORMAL`
- 已上架：`spu_status = ON_SALE and control_status = NORMAL`
- 已下架：`spu_status = OFF_SALE and control_status = NORMAL`
- 草稿：`spu_status = DRAFT and control_status = NORMAL`
- 停用：`control_status = DISABLED`
- 全部：不限制状态

SKU 视图：

- 待上架：`sku_status = READY and sku.control_status = NORMAL and spu.control_status = NORMAL`
- 已上架：`sku_status = ON_SALE and sku.control_status = NORMAL and spu.control_status = NORMAL`
- 已下架：`sku_status = OFF_SALE and sku.control_status = NORMAL and spu.control_status = NORMAL`
- 草稿：`sku_status = DRAFT and sku.control_status = NORMAL and spu.control_status = NORMAL`
- 停用：`sku.control_status = DISABLED or spu.control_status = DISABLED`
- 全部：不限制状态

## 新增表：product_distribution_operation_log

### 业务目的

记录商城商品 SPU/SKU 的业务操作轨迹，重点覆盖：

- 销售流转状态调整
- 平台停用
- 恢复
- 销售价调整
- 后续可扩展到商品信息保存、SKU 新增、SKU 删除等业务操作

### 业务边界

这张表承载商品业务事实的操作追溯，不承载配置项日志，不替代库存流水、财务流水、订单流水，也不作为当前状态来源。当前状态仍以 `product_spu` 和 `product_sku` 为准。

### 字段设计

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `log_id` | `bigint` | 是 | 自增 | 主键。 |
| `batch_no` | `varchar(64)` | 是 | 无 | 批量操作批次号。单个操作也生成批次号，用于把一次批量调价或批量上架的多条日志归并。 |
| `operation_type` | `varchar(32)` | 是 | 无 | 操作类型。建议值：`SALES_STATUS_CHANGE` 销售状态调整，`CONTROL_DISABLE` 停用，`CONTROL_RECOVER` 恢复，`SALE_PRICE_ADJUST` 销售价调整。 |
| `owner_type` | `varchar(16)` | 是 | 无 | 操作对象类型。`SPU` 或 `SKU`。 |
| `spu_id` | `bigint` | 是 | 无 | SPU ID。SKU 操作也必须写入所属 SPU ID。 |
| `sku_id` | `bigint` | 否 | `null` | SKU ID。SPU 操作为空。 |
| `system_spu_code` | `varchar(64)` | 否 | `null` | 系统 SPU 编码快照。 |
| `system_sku_code` | `varchar(64)` | 否 | `null` | 系统 SKU 编码快照。 |
| `seller_id` | `bigint` | 否 | `null` | 卖家 ID 快照。 |
| `seller_name` | `varchar(128)` | 否 | `null` | 卖家名称快照。 |
| `before_sales_status` | `varchar(32)` | 否 | `null` | 操作前销售流转状态。 |
| `after_sales_status` | `varchar(32)` | 否 | `null` | 操作后销售流转状态。 |
| `before_control_status` | `varchar(32)` | 否 | `null` | 操作前管控状态。 |
| `after_control_status` | `varchar(32)` | 否 | `null` | 操作后管控状态。 |
| `before_sale_price` | `decimal(18,4)` | 否 | `null` | 操作前销售价。只在 SKU 调价时有值。 |
| `after_sale_price` | `decimal(18,4)` | 否 | `null` | 操作后销售价。只在 SKU 调价时有值。 |
| `currency_code` | `varchar(16)` | 否 | `null` | 币种快照。 |
| `reason` | `varchar(500)` | 否 | `null` | 操作原因。停用必填；调价可选。 |
| `change_summary` | `varchar(500)` | 否 | `null` | 操作摘要，供列表快速展示。 |
| `diff_json` | `longtext` | 否 | `null` | 字段差异 JSON，用于展开查看修改前后值。 |
| `operator_name` | `varchar(64)` | 是 | 无 | 操作人账号。 |
| `operation_time` | `datetime` | 是 | `sysdate()` | 操作时间。 |
| `operation_source` | `varchar(32)` | 是 | `PAGE` | 操作来源。首版固定 `PAGE`，后续可扩展 `IMPORT`、`SYSTEM`、`API`。 |
| `remark` | `varchar(500)` | 否 | `null` | 备注。 |

### 约束与索引

- 主键：`pk_product_distribution_operation_log(log_id)`
- 普通索引：
  - `idx_product_dist_log_batch(batch_no)`
  - `idx_product_dist_log_spu(spu_id, operation_time)`
  - `idx_product_dist_log_sku(sku_id, operation_time)`
  - `idx_product_dist_log_type(operation_type, operation_time)`
  - `idx_product_dist_log_operator(operator_name, operation_time)`

## 权限点

建议新增或复用以下权限：

- `product:distribution:status`：销售状态调整、停用、恢复。
- `product:distribution:price`：销售价调整。
- `product:distribution:log`：查看商品操作日志。

## 前端组件复用

- 列表：继续使用 Ant Design Pro `ProTable`。
- 批量操作：使用 ProTable `rowSelection` 和工具栏按钮。
- 调价弹窗：使用 Ant Design `Modal`、`Form`、`Radio.Button`、`InputNumber`、`Table`。
- 日志弹窗：复用分类属性操作日志的 `Modal + ProTable + expandable diff table` 交互方式，但数据源改为商品操作日志接口。

## 回滚方式

- 若仅回滚页面和接口：恢复旧前端入口和旧状态接口即可。
- 若回滚字段：先确认不存在 `control_status = DISABLED` 的业务数据，再移除新增字段。
- 若回滚日志表：日志表不作为当前状态来源，可在备份后删除，不影响商品当前状态。
