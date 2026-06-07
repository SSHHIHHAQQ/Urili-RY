# 库存总览平台库存设计方案

日期：2026-06-07

状态：方案稿，待确认。未执行 DDL，未执行 DML，未新增后端或前端代码。

## 目标

「库存总览」不是来源仓库库存的换皮列表，而是商城商品可售库存的操作页。

本页要表达：

- 平台当前允许销售多少库存。
- 平台已经被订单锁定多少库存。
- 平台还能卖多少库存。
- 官方仓来源库存和平台库存之间为什么不一致。
- 用户可以在 SKU + 仓库明细行上快速调整平台总库存和平台在途库存。

本页不表达：

- 来源仓库库存的全部明细口径。
- 退货库存、次品库存、箱库存。
- 第三方仓的真实仓库可售性判断。
- 财务库存或结算库存。
- WMS 内部收货、质检、上架流程。

## 已确认的业务规则

1. 页面结构参考商城商品列表，支持 SPU 视图和 SKU 视图。
2. SPU 视图为 SPU 父行 + SKU 展开行。
3. SKU 视图以 SKU 为主行，展开 SKU + 仓库明细行。
4. 实际库存操作只发生在 SKU + 仓库明细行。
5. 官方仓商品不要求用户选择具体官方仓，默认所有官方来源主仓都可用。
6. 官方仓明细行按来源主仓名展示，例如 `CA012`，不显示 `领星WMS` 或 `LX-CA012`。
7. 官方仓只取来源仓库库存的 `综合库存` + `正品`。
8. 只有来源在途库存，没有来源可用库存时，也要展示该来源主仓行。
9. 不做不可售库存。
10. 来源库存为 0 时，平台可售也为 0；平台总库存可以保存，但不生效。
11. 平台需要自己的库存计算逻辑，不能完全相信 10 分钟或更久以前的来源库存快照。
12. 平台锁定库存来自订单占用。当前先设计锁定表，页面第一版可显示 0。
13. 出库完成后，需要用本地同步校准扣减抵消来源库存同步延迟，防止用户手动加库存后超卖。
14. 第三方仓没有来源库存约束，用户手工输入的库存上限就是平台可售库存池。
15. 在途转可售要自动完成，但只依据来源库存快照里的在途、可用、总库存数字变化，不分析 WMS 内部状态。
16. 调整库存尽量走双击字段编辑，确认后由后端预计算风险提示，再二次确认落库。

## 当前代码事实

### 商品侧

当前商品列表已经有 SPU/SKU 双视图：

- `react-ui/src/pages/Product/Distribution/index.tsx`
- 使用 `Radio.Group` 在 `SPU` / `SKU` 间切换。
- SPU 视图使用父表 + `expandedRowRender` 展开 SKU 表。
- `ProductSpu` / `ProductSku` 已有 `availableStock`、`warehouseCount`、`inventoryStatus`、`stockUpdateTime` 字段，但当前不少 SQL 仍返回 `null`，它们不是库存事实源。

当前商品发货仓库绑定：

- `product_spu_warehouse`
- 只表达商品允许使用哪些仓库。
- 不承载库存数量。

当前官方来源 SKU 绑定：

- `product_sku_source_binding`
- 表达商城 SKU 与官方来源 SKU 组的绑定。
- 后续官方库存总览应通过该表找到来源 SKU，再从来源仓库库存读模型推导来源主仓库存。

### 来源仓库库存侧

当前来源仓库库存读模型：

- `source_warehouse_stock_group`
- `source_warehouse_stock_detail`
- `source_warehouse_stock_filter_metric`

库存总览应读取 `source_warehouse_stock_detail`，而不是读取来源仓库库存父级 group 表。原因是库存总览需要 SKU + 来源主仓维度，明细表包含：

- `master_warehouse_name`：来源主仓名，例如 `CA012`。
- `master_sku`：来源 SKU。
- `master_product_name`：来源商品名。
- `inventory_scope`：库存口径。
- `inventory_attribute` / `inventory_attribute_label`：库存属性。
- `total_quantity`：来源总库存。
- `available_quantity`：来源可用库存。
- `in_transit_quantity`：来源在途库存。
- `system_sku`：配对后的系统 SKU。

库存总览官方仓第一版固定筛选：

```text
repository_scope = 'OFFICIAL_MASTER'
inventory_scope = 'COMPREHENSIVE'
inventory_attribute = '0'
```

展示名使用：

```text
master_warehouse_name
```

## 库存口径

### 官方仓

官方仓平台可售库存：

```text
来源有效可用 = max(0, 来源可用库存 - 同步校准扣减)

平台可售库存 = max(
  0,
  min(平台总库存, 来源有效可用) - 平台锁定库存
)
```

说明：

- `平台总库存` 是平台当前库存池，不是历史累计销售上限。
- 订单锁定不改变平台总库存。
- 出库完成会减少平台总库存。
- 出库完成但来源库存尚未同步减少时，写入同步校准扣减。
- 用户手工增加平台总库存时，也只能卖到来源有效可用库存为止。
- 来源库存为 0 时，平台可售为 0。

### 第三方仓

第三方仓平台可售库存：

```text
平台可售库存 = max(0, 平台总库存 - 平台锁定库存)
```

说明：

- 第三方仓没有来源库存约束。
- 不做不可售库存。
- 不判断第三方仓真实是否可售。
- 用户输入多少平台总库存，就视为愿意在平台销售多少库存。

## 官方仓明细行生成规则

### 有来源库存

对每个已绑定官方来源 SKU 的商城 SKU：

1. 从 `product_sku_source_binding` 找到 ACTIVE 绑定。
2. 到 `source_warehouse_stock_detail` 查询综合库存、正品库存。
3. 按 `master_warehouse_name` 分组。
4. 每个来源主仓生成一条 SKU + 仓库明细行。
5. 即使来源可用为 0，只要来源在途大于 0，也生成行。

官方仓明细行展示仓库名：

```text
CA012
```

不展示：

```text
领星WMS
LX-CA012
```

### 无来源库存

如果商品选择了官方仓，但当前没有命中任何来源库存：

- 展示一条占位行。
- 仓库显示为 `官方仓（未匹配来源库存）`。
- 来源总库存、来源可用库存、来源在途库存均为 0。
- 平台可售库存为 0。
- 平台总库存可以保存，但在来源库存为 0 时不生效。

后续如果出现来源主仓：

- 如果只出现一个来源主仓，可以自动把占位行里的平台总库存迁移到该来源主仓行，并写库存流水。
- 如果一次出现多个来源主仓，不自动拆分占位上限，避免系统替用户乱分仓；保留占位行并提示用户到具体来源主仓行上配置。

## 页面设计

### 视图切换

复用商城商品列表的模式，在工具栏使用 `Radio.Group`：

```text
SPU 视图 / SKU 视图
```

不使用顶部大 Tabs 作为 SPU/SKU 视图切换。

### SPU 视图

SPU 父行主列建议：

| 顺序 | 字段 | 说明 |
| --- | --- | --- |
| 1 | 商品信息 | SPU 编码、商品名、主图 |
| 2 | SKU 数 | SPU 下 SKU 数量 |
| 3 | 仓库类型 | 官方仓、三方仓、混合 |
| 4 | 平台总库存 | 汇总 SKU + 仓库明细 |
| 5 | 平台可售库存 | 汇总可售 |
| 6 | 平台锁定库存 | 汇总订单占用 |
| 7 | 平台在途库存 | 汇总平台在途 |
| 8 | 来源可用库存 | 官方仓来源可用汇总，三方仓为 `-` |
| 9 | 库存状态 | 有货、缺货、无来源库存等 |
| 10 | 更新时间 | 读模型更新时间 |

展开行展示 SKU 列表。SKU 行不直接编辑库存，继续进入或展开仓库明细后编辑。

### SKU 视图

SKU 主行主列建议：

| 顺序 | 字段 | 说明 |
| --- | --- | --- |
| 1 | SKU 信息 | SKU 编码、SKU 名、规格图 |
| 2 | 所属 SPU | SPU 编码、商品名 |
| 3 | 仓库数 | SKU 下有效仓库明细行数量 |
| 4 | 仓库类型 | 官方仓、三方仓、混合 |
| 5 | 平台总库存 | SKU 汇总 |
| 6 | 平台可售库存 | SKU 汇总 |
| 7 | 平台锁定库存 | SKU 汇总 |
| 8 | 平台在途库存 | SKU 汇总 |
| 9 | 来源可用库存 | 官方仓汇总，三方仓为 `-` |
| 10 | 库存状态 | 有货、缺货、无来源库存等 |
| 11 | 更新时间 | 读模型更新时间 |

展开行展示 SKU + 仓库明细。

### SKU + 仓库明细行

明细行主列建议：

| 顺序 | 字段 | 说明 |
| --- | --- | --- |
| 1 | 仓库 | 官方仓显示来源主仓名，三方仓显示三方仓名称 |
| 2 | 仓库类型 | 官方仓 / 三方仓 |
| 3 | 来源总库存 | 官方仓显示，三方仓为 `-` |
| 4 | 来源可用库存 | 官方仓显示，三方仓为 `-` |
| 5 | 来源在途库存 | 官方仓显示，三方仓为 `-` |
| 6 | 平台总库存 | 可双击编辑 |
| 7 | 平台可售库存 | 后端计算，只读 |
| 8 | 平台锁定库存 | 后端计算，只读 |
| 9 | 平台在途库存 | 可双击编辑 |
| 10 | 状态 | 正常、无来源库存、缺货、来源异常 |
| 11 | 最后同步时间 | 来源库存更新时间或平台更新时间 |

不把 `同步校准扣减` 放在默认主列。它可以放在展开详情、悬浮说明或后续库存流水中解释。

### 双击编辑

可编辑字段：

- 平台总库存。
- 平台在途库存。

交互：

1. 双击单元格。
2. 单元格切换为数字输入框。
3. 输入框旁边或下方显示确认、取消。
4. 点确认后调用后端预校验接口。
5. 后端返回是否需要二次确认、提示文案、计算后的库存影响。
6. 用户二次确认后调用保存接口。
7. 保存成功后写库存流水，刷新当前行和汇总读模型。

第一版限制：

- 不允许把平台总库存直接调到小于当前锁定库存。
- 如果用户尝试这么做，后端拒绝或返回需要进入后续冻结保护流程的提示。
- 后续销售保护期功能完成前，不做直接减少到低于锁定库存的生效逻辑。

## 在途转可售逻辑

库存总览不判断 WMS 内部状态，只观察来源库存快照数字变化：

- 来源在途库存。
- 来源可用库存。
- 来源总库存。

### 用户设置平台在途库存

用户在 SKU + 官方来源主仓明细行设置平台在途库存，例如：

```text
来源在途库存 = 1000
平台在途库存 = 300
```

表示用户希望这 300 个来源在途库存到货并变成来源可用后，自动增加到平台总库存。

第一版建议限制：

```text
平台在途库存 <= 当前可观察的来源在途库存 + 已进入待上架观察的数量
```

避免用户把不存在的来源在途库存配置成平台在途。

### 变化场景 A：在途减少，可用同步增加

例子：

```text
本次同步前：来源在途 1000，来源可用 0
本次同步后：来源在途 980，来源可用 20
```

处理：

1. 观察到来源在途减少 20。
2. 把平台在途中的 20 标记为可释放候选。
3. 观察到来源可用增加 20。
4. 自动把 20 加到平台总库存。
5. 平台在途减少 20。
6. 写库存流水。

### 变化场景 B：在途一次清零，可用后续慢慢增加

例子：

```text
同步 1：来源在途 1000，来源可用 0
同步 2：来源在途 0，来源可用 0
同步 3：来源在途 0，来源可用 30
同步 4：来源在途 0，来源可用 80
```

处理：

1. 同步 2 观察到来源在途减少 1000。
2. 不直接加平台总库存，因为来源可用没有增加。
3. 把平台在途中对应数量移动到“待来源可用增加”的内部状态。
4. 同步 3 观察到来源可用增加 30，释放 30 到平台总库存。
5. 同步 4 再释放 50。
6. 直到平台在途配置数量全部释放或用户取消。

### 不处理的情况

第一版不主动处理：

- 来源在途没有减少，但来源可用突然增加。
- 来源可用增加被其他出库、调拨或人工调整抵消，导致无法识别释放。
- 来源库存快照回退或乱序。

这些场景保守处理，不自动增加平台总库存，由人工调整解决。

## 订单库存生命周期

### 创建订单

```text
平台锁定库存 +N
平台可售库存 -N
平台总库存不变
```

写入：

- `inventory_reservation`
- `inventory_stock_ledger`

### 取消订单

```text
平台锁定库存 -N
平台可售库存 +N
平台总库存不变
```

更新：

- `inventory_reservation`
- `inventory_stock_ledger`

### 出库完成

```text
平台锁定库存 -N
平台总库存 -N
官方仓同步校准扣减 +N
```

官方仓额外写入：

- `inventory_source_deduction_pending`

第三方仓不写来源同步校准扣减。

## 表设计

### 1. `inventory_sku_warehouse_stock`

SKU + 仓库维度的当前库存状态表。一个商城 SKU 在一个平台仓库维度上一行。

官方仓的仓库维度是来源主仓；三方仓的仓库维度是系统三方仓。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| stock_id | bigint | 是 | auto_increment | 主键 |
| stock_key | varchar(128) | 是 |  | 稳定库存行 key，建议由 SKU + 仓库引用维度生成 |
| spu_id | bigint | 是 |  | 商城 SPU ID |
| sku_id | bigint | 是 |  | 商城 SKU ID |
| seller_id | bigint | 是 |  | 卖家 ID 快照 |
| system_sku_code | varchar(64) | 是 |  | 系统 SKU 编码快照 |
| warehouse_kind | varchar(32) | 是 |  | `official` / `third_party` |
| warehouse_ref_type | varchar(32) | 是 |  | `OFFICIAL_MASTER` / `THIRD_PARTY_WAREHOUSE` / `UNMATCHED_OFFICIAL` |
| warehouse_id | bigint | 否 | null | 三方仓 warehouse_id；官方来源主仓可为空 |
| warehouse_code | varchar(64) | 是 | '' | 三方仓编码或官方占位编码 |
| warehouse_name | varchar(200) | 是 | '' | 页面展示仓库名；官方仓为 `CA012` 或占位名 |
| source_scope | varchar(32) | 是 | 'OFFICIAL_MASTER' | 来源范围，三方仓留默认或空 |
| source_master_warehouse_name | varchar(128) | 是 | '' | 来源主仓名，例如 `CA012` |
| source_inventory_scope | varchar(32) | 是 | 'COMPREHENSIVE' | 来源库存口径 |
| source_inventory_attribute | varchar(64) | 是 | '0' | 来源库存属性，第一版固定正品 |
| source_total_qty | bigint | 是 | 0 | 最近一次读模型写入的来源总库存 |
| source_available_qty | bigint | 是 | 0 | 最近一次读模型写入的来源可用库存 |
| source_in_transit_qty | bigint | 是 | 0 | 最近一次读模型写入的来源在途库存 |
| source_snapshot_time | datetime | 否 | null | 来源库存最新更新时间 |
| platform_total_qty | bigint | 是 | 0 | 平台总库存，也是平台当前库存池 |
| platform_reserved_qty | bigint | 是 | 0 | 平台锁定库存汇总，来自 reservation |
| platform_in_transit_qty | bigint | 是 | 0 | 平台在途库存剩余未释放数量 |
| pending_available_inbound_qty | bigint | 是 | 0 | 来源在途已减少但来源可用尚未增加的内部观察数量 |
| pending_source_deduction_qty | bigint | 是 | 0 | 出库已完成但来源库存尚未同步扣减的校准数量 |
| platform_available_qty | bigint | 是 | 0 | 后端计算后的平台可售库存 |
| effective_status | varchar(32) | 是 | 'ACTIVE' | `ACTIVE` / `NO_SOURCE` / `OUT_OF_STOCK` / `DISABLED` |
| version | int | 是 | 0 | 乐观锁版本 |
| calc_time | datetime | 否 | null | 最近计算时间 |
| create_by | varchar(64) | 是 | '' | 创建者 |
| create_time | datetime | 否 | null | 创建时间 |
| update_by | varchar(64) | 是 | '' | 更新者 |
| update_time | datetime | 否 | null | 更新时间 |
| remark | varchar(500) | 是 | '' | 备注 |

建议索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| pk_inventory_sku_warehouse_stock | stock_id | 主键 |
| uk_inventory_sku_warehouse_stock_key | stock_key | 幂等生成库存行 |
| idx_inventory_stock_sku | sku_id, warehouse_kind | SKU 维度查询 |
| idx_inventory_stock_spu | spu_id | SPU 展开查询 |
| idx_inventory_stock_seller | seller_id | 卖家筛选 |
| idx_inventory_stock_source_wh | source_scope, source_master_warehouse_name | 官方来源主仓查询 |
| idx_inventory_stock_status | effective_status | 状态筛选 |

### 2. `inventory_stock_ledger`

库存流水表。只追加，不直接覆盖历史记录。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| ledger_id | bigint | 是 | auto_increment | 主键 |
| stock_id | bigint | 是 |  | 库存行 ID |
| stock_key | varchar(128) | 是 |  | 库存行 key 快照 |
| spu_id | bigint | 是 |  | SPU ID 快照 |
| sku_id | bigint | 是 |  | SKU ID 快照 |
| seller_id | bigint | 是 |  | 卖家 ID 快照 |
| warehouse_kind | varchar(32) | 是 |  | 仓库类型快照 |
| warehouse_ref_type | varchar(32) | 是 |  | 仓库引用类型快照 |
| warehouse_name | varchar(200) | 是 | '' | 仓库展示名快照 |
| operation_type | varchar(64) | 是 |  | 操作类型 code |
| operation_source | varchar(32) | 是 |  | `ADMIN` / `ORDER` / `SYSTEM_SYNC` |
| biz_type | varchar(64) | 是 | '' | 业务类型，例如订单、手工调整、在途释放 |
| biz_no | varchar(128) | 是 | '' | 业务单号 |
| delta_qty | bigint | 是 | 0 | 本次影响库存数量，可正可负 |
| before_platform_total_qty | bigint | 是 | 0 | 调整前平台总库存 |
| after_platform_total_qty | bigint | 是 | 0 | 调整后平台总库存 |
| before_available_qty | bigint | 是 | 0 | 调整前平台可售 |
| after_available_qty | bigint | 是 | 0 | 调整后平台可售 |
| before_reserved_qty | bigint | 是 | 0 | 调整前平台锁定 |
| after_reserved_qty | bigint | 是 | 0 | 调整后平台锁定 |
| before_in_transit_qty | bigint | 是 | 0 | 调整前平台在途 |
| after_in_transit_qty | bigint | 是 | 0 | 调整后平台在途 |
| risk_confirmed | char(1) | 是 | 'N' | 是否经过风险二次确认 |
| risk_message | varchar(1000) | 是 | '' | 二次确认提示文案快照 |
| reason | varchar(500) | 是 | '' | 调整原因 |
| operator_id | bigint | 否 | null | 操作人 ID |
| operator_name | varchar(64) | 是 | '' | 操作人名称 |
| operate_time | datetime | 是 |  | 操作时间 |
| create_time | datetime | 否 | null | 创建时间 |
| remark | varchar(500) | 是 | '' | 备注 |

建议索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| pk_inventory_stock_ledger | ledger_id | 主键 |
| idx_inventory_ledger_stock | stock_id, operate_time | 库存行流水 |
| idx_inventory_ledger_sku | sku_id, operate_time | SKU 流水 |
| idx_inventory_ledger_biz | biz_type, biz_no | 业务反查 |
| idx_inventory_ledger_operator | operator_id, operate_time | 审计查询 |

### 3. `inventory_reservation`

平台订单锁定库存表。第一版库存总览页面可显示 0，但表结构要为订单模块预留。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| reservation_id | bigint | 是 | auto_increment | 主键 |
| reservation_no | varchar(64) | 是 |  | 锁定单号 |
| stock_id | bigint | 是 |  | 库存行 ID |
| stock_key | varchar(128) | 是 |  | 库存行 key 快照 |
| spu_id | bigint | 是 |  | SPU ID 快照 |
| sku_id | bigint | 是 |  | SKU ID 快照 |
| seller_id | bigint | 是 |  | 卖家 ID 快照 |
| order_no | varchar(128) | 是 | '' | 平台订单号 |
| order_item_no | varchar(128) | 是 | '' | 平台订单明细号 |
| reserved_qty | bigint | 是 | 0 | 锁定数量 |
| released_qty | bigint | 是 | 0 | 已释放数量 |
| consumed_qty | bigint | 是 | 0 | 已出库消耗数量 |
| status | varchar(32) | 是 | 'RESERVED' | `RESERVED` / `RELEASED` / `CONSUMED` / `CANCELLED` |
| reserve_time | datetime | 是 |  | 锁定时间 |
| release_time | datetime | 否 | null | 释放时间 |
| consume_time | datetime | 否 | null | 出库消耗时间 |
| expire_time | datetime | 否 | null | 锁定过期时间 |
| create_by | varchar(64) | 是 | '' | 创建者 |
| create_time | datetime | 否 | null | 创建时间 |
| update_by | varchar(64) | 是 | '' | 更新者 |
| update_time | datetime | 否 | null | 更新时间 |
| remark | varchar(500) | 是 | '' | 备注 |

建议索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| pk_inventory_reservation | reservation_id | 主键 |
| uk_inventory_reservation_no | reservation_no | 幂等锁定 |
| idx_inventory_reservation_stock | stock_id, status | 按库存行汇总锁定 |
| idx_inventory_reservation_order | order_no, order_item_no | 订单反查 |
| idx_inventory_reservation_status | status, reserve_time | 状态任务 |

### 4. `inventory_source_deduction_pending`

官方仓来源同步延迟校准表。只用于官方仓。

出库完成后，来源仓库库存快照可能还没有同步扣减。本表记录“平台已经出库，但来源快照还没体现”的数量，在计算官方仓可售库存时临时扣掉。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| pending_id | bigint | 是 | auto_increment | 主键 |
| stock_id | bigint | 是 |  | 库存行 ID |
| stock_key | varchar(128) | 是 |  | 库存行 key 快照 |
| sku_id | bigint | 是 |  | SKU ID 快照 |
| source_master_warehouse_name | varchar(128) | 是 |  | 来源主仓名 |
| outbound_biz_no | varchar(128) | 是 |  | 出库业务单号 |
| pending_qty | bigint | 是 | 0 | 初始待校准数量 |
| covered_qty | bigint | 是 | 0 | 已被来源快照覆盖数量 |
| remaining_qty | bigint | 是 | 0 | 剩余待校准数量 |
| baseline_source_available_qty | bigint | 是 | 0 | 出库时来源可用库存基线 |
| baseline_source_total_qty | bigint | 是 | 0 | 出库时来源总库存基线 |
| baseline_source_snapshot_time | datetime | 否 | null | 出库时来源快照时间 |
| status | varchar(32) | 是 | 'PENDING' | `PENDING` / `PARTIAL` / `COVERED` / `MANUAL_CLOSED` |
| cover_time | datetime | 否 | null | 完全覆盖时间 |
| create_time | datetime | 否 | null | 创建时间 |
| update_time | datetime | 否 | null | 更新时间 |
| remark | varchar(500) | 是 | '' | 备注 |

建议索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| pk_inventory_source_deduction_pending | pending_id | 主键 |
| idx_inventory_source_deduction_stock | stock_id, status | 库存行待校准汇总 |
| idx_inventory_source_deduction_sku_wh | sku_id, source_master_warehouse_name, status | 来源主仓校准 |
| idx_inventory_source_deduction_biz | outbound_biz_no | 出库单反查 |

### 5. `inventory_in_transit_tracking`

平台在途库存跟踪表。只用于官方仓自动在途转可售。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| tracking_id | bigint | 是 | auto_increment | 主键 |
| tracking_no | varchar(64) | 是 |  | 在途跟踪单号 |
| stock_id | bigint | 是 |  | 库存行 ID |
| stock_key | varchar(128) | 是 |  | 库存行 key 快照 |
| sku_id | bigint | 是 |  | SKU ID 快照 |
| source_master_warehouse_name | varchar(128) | 是 |  | 来源主仓名 |
| configured_qty | bigint | 是 | 0 | 用户配置的平台在途数量 |
| released_qty | bigint | 是 | 0 | 已释放到平台总库存的数量 |
| pending_available_qty | bigint | 是 | 0 | 来源在途已减少、等待来源可用增加的数量 |
| remaining_qty | bigint | 是 | 0 | 剩余未释放数量 |
| baseline_source_in_transit_qty | bigint | 是 | 0 | 配置时来源在途基线 |
| baseline_source_available_qty | bigint | 是 | 0 | 配置时来源可用基线 |
| last_source_in_transit_qty | bigint | 是 | 0 | 上次观察到的来源在途 |
| last_source_available_qty | bigint | 是 | 0 | 上次观察到的来源可用 |
| last_source_snapshot_time | datetime | 否 | null | 上次来源快照时间 |
| status | varchar(32) | 是 | 'ACTIVE' | `ACTIVE` / `COMPLETED` / `CANCELLED` |
| create_by | varchar(64) | 是 | '' | 创建者 |
| create_time | datetime | 否 | null | 创建时间 |
| update_by | varchar(64) | 是 | '' | 更新者 |
| update_time | datetime | 否 | null | 更新时间 |
| remark | varchar(500) | 是 | '' | 备注 |

建议索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| pk_inventory_in_transit_tracking | tracking_id | 主键 |
| uk_inventory_in_transit_tracking_no | tracking_no | 幂等跟踪 |
| idx_inventory_in_transit_stock | stock_id, status | 库存行在途跟踪 |
| idx_inventory_in_transit_sku_wh | sku_id, source_master_warehouse_name, status | 来源主仓在途处理 |

### 6. `inventory_overview_sku_read_model`

库存总览 SKU 视图读模型。一个 SKU 一行。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| sku_stock_key | varchar(128) | 是 |  | 主键，SKU 读模型 key |
| spu_id | bigint | 是 |  | SPU ID |
| sku_id | bigint | 是 |  | SKU ID |
| seller_id | bigint | 是 |  | 卖家 ID |
| system_sku_code | varchar(64) | 是 |  | 系统 SKU 编码 |
| product_name | varchar(255) | 是 | '' | 商品名 |
| sku_name | varchar(255) | 是 | '' | SKU 名 |
| sku_image_url | varchar(1000) | 是 | '' | SKU 图片 |
| warehouse_kind_summary | varchar(32) | 是 | '' | 官方仓、三方仓或混合 |
| warehouse_count | int | 是 | 0 | 仓库明细行数量 |
| platform_total_qty | bigint | 是 | 0 | 平台总库存汇总 |
| platform_available_qty | bigint | 是 | 0 | 平台可售库存汇总 |
| platform_reserved_qty | bigint | 是 | 0 | 平台锁定库存汇总 |
| platform_in_transit_qty | bigint | 是 | 0 | 平台在途库存汇总 |
| source_total_qty | bigint | 是 | 0 | 官方仓来源总库存汇总 |
| source_available_qty | bigint | 是 | 0 | 官方仓来源可用库存汇总 |
| source_in_transit_qty | bigint | 是 | 0 | 官方仓来源在途库存汇总 |
| inventory_status | varchar(32) | 是 | 'OUT_OF_STOCK' | 库存状态 |
| latest_source_snapshot_time | datetime | 否 | null | 最新来源快照时间 |
| latest_stock_update_time | datetime | 否 | null | 最新平台库存更新时间 |
| search_text | text | 是 |  | 搜索文本 |
| rebuild_time | datetime | 是 |  | 读模型构建时间 |

建议索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| pk_inventory_overview_sku | sku_stock_key | 主键 |
| uk_inventory_overview_sku_id | sku_id | SKU 唯一行 |
| idx_inventory_overview_sku_spu | spu_id | SPU 展开 |
| idx_inventory_overview_sku_seller | seller_id | 卖家筛选 |
| idx_inventory_overview_sku_status | inventory_status | 状态筛选 |
| idx_inventory_overview_sku_list | latest_stock_update_time, sku_id | 列表排序 |

### 7. `inventory_overview_spu_read_model`

库存总览 SPU 视图读模型。一个 SPU 一行。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| spu_stock_key | varchar(128) | 是 |  | 主键，SPU 读模型 key |
| spu_id | bigint | 是 |  | SPU ID |
| seller_id | bigint | 是 |  | 卖家 ID |
| system_spu_code | varchar(64) | 是 |  | 系统 SPU 编码 |
| product_name | varchar(255) | 是 | '' | 商品名 |
| main_image_url | varchar(1000) | 是 | '' | 主图 |
| sku_count | int | 是 | 0 | SKU 数量 |
| warehouse_kind_summary | varchar(32) | 是 | '' | 官方仓、三方仓或混合 |
| warehouse_count | int | 是 | 0 | 仓库明细行数量 |
| platform_total_qty | bigint | 是 | 0 | 平台总库存汇总 |
| platform_available_qty | bigint | 是 | 0 | 平台可售库存汇总 |
| platform_reserved_qty | bigint | 是 | 0 | 平台锁定库存汇总 |
| platform_in_transit_qty | bigint | 是 | 0 | 平台在途库存汇总 |
| source_total_qty | bigint | 是 | 0 | 官方仓来源总库存汇总 |
| source_available_qty | bigint | 是 | 0 | 官方仓来源可用库存汇总 |
| source_in_transit_qty | bigint | 是 | 0 | 官方仓来源在途库存汇总 |
| inventory_status | varchar(32) | 是 | 'OUT_OF_STOCK' | 库存状态 |
| latest_source_snapshot_time | datetime | 否 | null | 最新来源快照时间 |
| latest_stock_update_time | datetime | 否 | null | 最新平台库存更新时间 |
| search_text | text | 是 |  | 搜索文本 |
| rebuild_time | datetime | 是 |  | 读模型构建时间 |

建议索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| pk_inventory_overview_spu | spu_stock_key | 主键 |
| uk_inventory_overview_spu_id | spu_id | SPU 唯一行 |
| idx_inventory_overview_spu_seller | seller_id | 卖家筛选 |
| idx_inventory_overview_spu_status | inventory_status | 状态筛选 |
| idx_inventory_overview_spu_list | latest_stock_update_time, spu_id | 列表排序 |

## 字典和 code

建议新增或复用以下 code：

| 类型 | code | label | 说明 |
| --- | --- | --- | --- |
| `warehouse_kind` | `official` | 官方仓 | 已存在，继续复用 |
| `warehouse_kind` | `third_party` | 第三方仓库 | 已存在，继续复用 |
| `inventory_warehouse_ref_type` | `OFFICIAL_MASTER` | 来源主仓 | 官方仓实际库存行 |
| `inventory_warehouse_ref_type` | `THIRD_PARTY_WAREHOUSE` | 三方仓 | 第三方仓库存行 |
| `inventory_warehouse_ref_type` | `UNMATCHED_OFFICIAL` | 未匹配官方仓 | 官方仓无来源库存占位行 |
| `inventory_status` | `IN_STOCK` | 有货 | 平台可售大于 0 |
| `inventory_status` | `OUT_OF_STOCK` | 缺货 | 平台可售等于 0 |
| `inventory_status` | `NO_SOURCE` | 无来源库存 | 官方仓未匹配来源库存 |
| `inventory_status` | `SOURCE_ONLY_IN_TRANSIT` | 仅来源在途 | 来源可用为 0、来源在途大于 0 |
| `inventory_operation_type` | `MANUAL_INCREASE` | 手工增加库存 | 管理端调高平台总库存 |
| `inventory_operation_type` | `MANUAL_DECREASE` | 手工减少库存 | 管理端调低平台总库存 |
| `inventory_operation_type` | `ORDER_RESERVE` | 订单锁定 | 创建订单占用库存 |
| `inventory_operation_type` | `ORDER_RELEASE` | 订单释放 | 取消订单释放库存 |
| `inventory_operation_type` | `OUTBOUND_DEDUCT` | 出库扣减 | 出库完成扣减平台库存池 |
| `inventory_operation_type` | `SOURCE_DEDUCTION_PENDING` | 来源校准扣减 | 官方仓同步延迟校准 |
| `inventory_operation_type` | `IN_TRANSIT_CONFIG` | 在途配置 | 用户设置平台在途 |
| `inventory_operation_type` | `IN_TRANSIT_RELEASE` | 在途释放 | 来源可用增加后自动加平台库存 |

API 和数据库保存 code，前端通过字典或统一 option catalog 展示中文。

## 权限点

第一版建议：

| 权限 | 用途 |
| --- | --- |
| `inventory:overview:list` | 库存总览列表 |
| `inventory:overview:query` | 库存总览详情查询 |
| `inventory:overview:adjust` | 调整平台总库存和平台在途库存 |
| `inventory:overview:ledger` | 查看库存流水 |
| `inventory:overview:export` | 导出库存总览，第一版可不做 |

新增接口时必须同步：

- Controller `@PreAuthorize`。
- mutating 接口 `@Log`。
- 菜单 seed 的 `sys_menu.perms`。
- React 按钮权限控制。
- 权限合同测试。

## 接口草案

第一版后台接口建议：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/inventory/admin/overview/spu/list` | `inventory:overview:list` | SPU 视图列表 |
| GET | `/inventory/admin/overview/sku/list` | `inventory:overview:list` | SKU 视图列表 |
| GET | `/inventory/admin/overview/sku/{skuId}/warehouses` | `inventory:overview:query` | SKU + 仓库明细 |
| POST | `/inventory/admin/overview/adjust/preview` | `inventory:overview:adjust` | 调整前预计算和风险提示 |
| POST | `/inventory/admin/overview/adjust/confirm` | `inventory:overview:adjust` | 二次确认后落库 |
| GET | `/inventory/admin/overview/ledger/list` | `inventory:overview:ledger` | 库存流水查询 |

## 读模型刷新

需要刷新的触发点：

1. 商品 SPU/SKU 新增、编辑、上下架、禁用。
2. 商品官方来源 SKU 绑定变更。
3. 商品发货仓库配置变更。
4. 来源仓库库存读模型刷新完成。
5. 平台库存调整保存成功。
6. 订单锁定、释放、出库完成。
7. 在途跟踪释放库存。

第一版可以先做同步刷新：

- 调整单个 SKU + 仓库行后，只刷新当前 SKU 和所属 SPU 的读模型。
- 来源仓库库存刷新后，批量刷新受影响的官方仓 SKU。

后续数据量继续增加后，再改成任务队列或批处理。

## 未来保留：销售保护期

未来减少平台总库存时，需要接近期销售数量判断。

方向：

- 增加销售统计读模型。
- 当用户减少库存上限低于保护阈值时，不直接生效。
- 进入冻结保护期。
- 保护期结束后按剩余可释放数量执行减少。

第一版不建销售保护期表，不实现冻结期。当前只保留接口和流水上的风险确认字段。

## 实施顺序建议

1. 确认本方案。
2. 写库存总览 SQL 方案和确认 token。
3. 新建 `inventory` 后端模块或在已存在模块中补齐 Controller、Service、Mapper。
4. 实现库存当前表、流水表、锁定表、同步校准表、在途跟踪表、SPU/SKU 读模型。
5. 实现只读列表和仓库明细接口。
6. 实现调整预览和确认接口。
7. 实现 React 页面，复用商品列表 SPU/SKU 视图切换和 ProTable 查询区。
8. 接浏览器验证：SPU/SKU 视图、官方仓来源主仓行、无来源占位行、在途行、双击编辑。
9. 补权限、菜单和合同测试。
10. 运行后端测试、前端 lint/typecheck、CodeGraph sync，并写执行记录。

## 当前待确认点

本方案按当前沟通已经默认以下结论：

1. 官方仓明细行按来源主仓名展示。
2. 官方仓来源库存取综合库存 + 正品。
3. 只有来源在途、无来源可用时也展示。
4. 同步校准扣减不放主列表列。
5. 不做不可售库存。

如果确认，就可以进入 SQL 与接口实现方案。
