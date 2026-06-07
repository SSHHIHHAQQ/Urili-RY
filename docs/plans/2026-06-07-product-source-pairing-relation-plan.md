# 商城商品来源 SKU 配对关系落地计划

## 当前只读核查结论

- 当前运行库：`fenxiao`，本轮只读查询结构和统计，未执行 DDL/DML。
- 仓库主数据已经存在：`warehouse` 4 条，其中官方仓 3 条、三方仓 1 条。
- 来源商品读模型已经存在并有数据：
  - `source_product_group`：5507 条。
  - `source_product_dimension_group`：8354 条。
  - `source_product_warehouse_detail`：10736 条。
- 上游库存读模型已经存在并有数据：
  - `upstream_system_sku_inventory_snapshot`：12404 条。
  - `source_warehouse_stock_group`：11860 条。
  - `source_warehouse_stock_detail`：12404 条。
- 商城商品已有数据：`product_spu` 24 条，`product_sku` 69 条。
- `product_spu_warehouse` 当前 0 条，说明商品侧仓库绑定尚未形成真实数据。
- `upstream_system_sku_pairing` 当前 0 条，来源商品库和来源库存当前仍显示未配对。
- 运行库 `upstream_system_warehouse_pairing` 当前没有 `pairing_role` 字段，而源码增量脚本中已经设计了该字段；后续落地前必须先确认 `20260607_upstream_pairing_role_binding.sql` 或同等迁移是否已经执行到当前运行库。

## 设计目标

这次不是再做一个来源商品库，也不是再做一套库存表，而是补齐“商城 SKU 使用哪个来源 SKU”的正式关系。

目标关系必须同时打通：

- 商品列表：知道某个 `product_sku.sku_id` 绑定了哪个来源 SKU。
- 来源商品库：来源 SKU 能显示已经配对到哪个商城 SKU。
- 上游主仓配对：官方仓范围从上游主仓/仓库配对关系推导，不手工重复维护。
- 上游库存：商品 SKU 的库存读取能从来源库存快照按来源 SKU 和仓库映射读取。
- 状态锁定：商品提交后卖家不能换绑；下架后卖家也不能通过新建商品绕过绑定，只有管理端可换绑或释放。

## 表复用原则

### 继续复用的表

| 表 | 用途 | 商品侧怎么用 |
| --- | --- | --- |
| `product_spu` | 商城 SPU 主事实 | 不新增来源字段，只保留商品主体信息 |
| `product_sku` | 商城 SKU 主事实 | 保存系统 SKU、卖家 SKU、价格、状态、尺寸展示值 |
| `warehouse` | 系统仓库主数据 | 读取官方仓/三方仓、币种、仓库状态 |
| `official_warehouse` / `third_party_warehouse` | 仓库类型扩展 | 继续由仓库模块维护 |
| `upstream_system_warehouse_pairing` | 上游仓库到系统仓库配对 | 官方仓可发仓库范围从这里推导 |
| `source_product_group` | 来源 SKU 组读模型 | 作为来源 SKU 选择入口 |
| `source_product_dimension_group` | 来源 SKU 尺寸组读模型 | 作为尺寸重量选择和快照来源 |
| `source_product_warehouse_detail` | 来源 SKU 仓库明细读模型 | 推导来源 SKU 覆盖哪些主仓/连接 |
| `source_warehouse_stock_group` / `source_warehouse_stock_detail` | 来源库存读模型 | 商品库存读取边界，不回写商品表 |
| `upstream_system_sku_pairing` | 上游侧配对摘要 | 作为来源商品库/库存读模型的配对投影，不作为商品侧主事实 |
| `product_distribution_operation_log` | 商品操作日志 | 新增来源绑定、锁定、换绑、释放等操作类型 |

### 不新增的表

第一版不新增 `product_sku_source_binding_warehouse`。

原因：官方仓范围可以从 `product_sku_source_binding.source_dimension_group_key` 关联 `source_product_warehouse_detail`，再通过 `upstream_system_warehouse_pairing` 和 `warehouse` 推导。直接保存一份绑定仓库明细会和仓库配对、仓库币种、仓库状态发生重复，后续仓库配对变更时也更容易出现不一致。

## 推荐新增表

只新增一张商品侧主事实表：`product_sku_source_binding`。

业务目的：

- 保存商城 SKU 与来源 SKU 的正式绑定关系。
- 保存来源 SKU 组、尺寸组、来源 SKU、来源商品名和尺寸重量快照。
- 承担卖家不能绕过换绑的约束。
- 为管理端换绑、释放、锁定提供状态基础。

建议字段：

| 字段 | 说明 |
| --- | --- |
| `binding_id` | 主键 |
| `spu_id` | 商城 SPU ID，关联 `product_spu.spu_id` |
| `sku_id` | 商城 SKU ID，关联 `product_sku.sku_id` |
| `seller_id` | 卖家 ID 快照，用于占用约束和查询 |
| `system_sku_code` | 系统 SKU 编码快照 |
| `source_scope` | 来源范围，第一版固定 `OFFICIAL_MASTER` |
| `source_sku_group_key` | 来源 SKU 组 key，用于占用判断 |
| `source_dimension_group_key` | 来源 SKU 尺寸组 key，用于尺寸重量和仓库范围推导 |
| `master_sku` | 来源 SKU 快照 |
| `master_product_name_snapshot` | 来源商品名快照 |
| `source_payload_hash` | 来源商品快照 hash |
| `wms_payload_hash` | WMS 尺寸快照 hash，可为空 |
| `measure_length_cm` | 最终采用长度 cm |
| `measure_width_cm` | 最终采用宽度 cm |
| `measure_height_cm` | 最终采用高度 cm |
| `measure_weight_kg` | 最终采用重量 kg |
| `measure_source` | 尺寸来源：`WMS` / `PRODUCT` |
| `binding_status` | 绑定状态：`ACTIVE` / `REPLACED` / `RELEASED` |
| `lock_status` | 锁定状态：`UNLOCKED` / `LOCKED` |
| `locked_time` | 锁定时间 |
| `locked_by` | 锁定人 |
| `release_reason` | 释放原因 |
| `replace_reason` | 换绑原因 |
| `create_by` / `create_time` | 创建信息 |
| `update_by` / `update_time` | 更新信息 |
| `remark` | 备注 |

建议约束：

- 一个商城 SKU 同时只能有一条 `ACTIVE` 绑定。
- 一个来源 SKU 组在有效状态下只能绑定一个商城 SKU。第一版建议按全局约束做，和现有 `upstream_system_sku_pairing(connection_code, master_sku)` 的一对一能力保持一致。
- 如果后续确认同一个官方来源 SKU 可以被多个卖家共用，则不能直接复用现有 `upstream_system_sku_pairing` 的一对一投影，需要单独改来源商品库和库存读模型的配对摘要口径。

## 关键业务关系

### 官方仓商品新增/编辑

1. 用户选择仓库类型为官方仓。
2. 页面不再让用户手工选择具体发货仓库。
3. SKU 行通过来源 SKU 选择器生成或绑定。
4. 后端读取 `source_product_group` / `source_product_dimension_group` / `source_product_warehouse_detail` 校验来源 SKU 是否有效。
5. 后端通过 `upstream_system_warehouse_pairing` + `warehouse` 校验该来源 SKU 是否能映射到正常官方仓，并取得币种。
6. 后端把尺寸重量写入 `product_sku` 展示字段，同时把标准化数值写入 `product_sku_source_binding`。
7. 保存草稿时绑定为 `UNLOCKED`；提交待上架或离开草稿时锁定为 `LOCKED`。

### 来源商品库和库存的配对显示

商品侧绑定成功后，需要同步维护 `upstream_system_sku_pairing` 作为上游侧配对投影：

- 对 `source_product_warehouse_detail` 中同一 `source_dimension_group_key` 覆盖的每个 `connection_code + master_sku`，写入或更新对应的 `upstream_system_sku_pairing`。
- `system_sku` 写 `product_sku.system_sku_code`。
- `system_sku_name` 建议写商品中文标题 + SKU规格摘要。
- `customer_name` 写卖家名称快照。

注意：`upstream_system_sku_pairing` 不是商品侧主事实，只是为了让来源商品库和来源库存读模型继续复用现有配对摘要。商品侧主事实仍是 `product_sku_source_binding`。

### 上游库存读取

商品列表不保存库存数量。

库存读取边界：

1. SKU 视图通过 `product_sku_source_binding` 找到 `source_dimension_group_key` / `master_sku`。
2. 通过 `source_product_warehouse_detail` 找到来源连接和来源主仓。
3. 通过 `upstream_system_warehouse_pairing` 找到系统仓库。
4. 通过 `source_warehouse_stock_detail` 按 `connection_code + master_sku` 汇总库存。
5. 展示时只显示可售库存、仓库数、库存状态等读取结果，不回写 `product_sku`。

## 前置风险和处理

1. 运行库仓库配对表缺少 `pairing_role`。
   - 先确认迁移是否已经执行。
   - 如果没有执行，商品侧落地前应先完成该迁移。
   - 商品侧查询官方仓时优先用 `pairing_role = FULFILLMENT`，避免报价仓和履约仓混用。

2. 来源 SKU 组和尺寸组不是一回事。
   - 占用判断用 `source_sku_group_key`。
   - 尺寸重量和仓库明细用 `source_dimension_group_key`。
   - 选择器需要让用户看到不同尺寸组，或者在只有一个尺寸组时自动选中。

3. `upstream_system_sku_pairing` 是全局一对一。
   - 第一版建议官方来源 SKU 全局只能绑定一个商城 SKU。
   - 如果业务要支持多个卖家共用同一个官方来源 SKU，需要先重做来源商品库和库存读模型的配对展示规则。

4. `product_spu_warehouse` 当前为空。
   - 官方仓场景第一版不把它作为主事实。
   - SPU 视图需要仓库类型、仓库数、币种时，从绑定关系派生。
   - 三方仓手工选择场景可以继续使用 `product_spu_warehouse`。

## 实施阶段

### 阶段 1：确认并补齐上游仓库配对迁移

- 确认 `upstream_system_warehouse_pairing.pairing_role` 是否需要补执行。
- 确认官方仓配对行是否覆盖 CA012、NY013。
- 确认系统仓库币种一致性，官方仓来源 SKU 不再额外手选仓库。

### 阶段 2：新增商品侧来源绑定表

- 新增 `product_sku_source_binding`。
- 新增实体、Mapper、Service 方法。
- 新增绑定校验：来源有效、官方仓可映射、币种一致、SKU未被占用。
- 新增操作日志类型：`SOURCE_BIND`、`SOURCE_LOCK`、`SOURCE_REBIND`、`SOURCE_RELEASE`。

### 阶段 3：接入新增/编辑商品流程

- 官方仓模式下，SKU 来源必须来自来源商品库。
- 尺寸重量只读，由来源商品尺寸组带出。
- 保存草稿可修改绑定；非草稿不允许卖家修改绑定。
- 编辑页非草稿只展示绑定信息。

### 阶段 4：接入列表和库存读取

- SPU/SKU 列表增加来源绑定状态、来源 SKU、来源仓范围。
- SKU 视图读取来源库存聚合，不把库存写回商品表。
- 来源商品库和来源库存读模型继续通过 `upstream_system_sku_pairing` 展示配对摘要。

### 阶段 5：管理端换绑/释放

- 管理端提供换绑弹窗，必须填写原因。
- 换绑时旧 `product_sku_source_binding` 标记 `REPLACED`，新绑定标记 `ACTIVE`。
- 同步更新 `upstream_system_sku_pairing` 投影。
- 释放时标记 `RELEASED`，并清理或更新对应配对投影。

## 待确认点

1. 官方来源 SKU 第一版是否按“全局只能绑定一个商城 SKU”处理？
2. 如果一个来源 SKU 组有多个尺寸组，页面是让用户选择尺寸组，还是默认选择最新/最完整的一组？
3. 管理端释放绑定后，是否允许同一卖家重新绑定到另一个商城 SKU？
4. 当前运行库的 `pairing_role` 迁移是否由我先补齐执行，还是由负责上游系统/仓库的 agent 先收口？
