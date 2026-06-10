# 报价方案阶段一设计方案

日期：2026-06-10

## 目标

阶段一只做报价方案的基础配对能力。

本阶段要解决的是：运营可以先维护一个报价方案，指定它适用于哪些买家范围，选择费用来源模式，并把客户渠道加入方案。操作费设置和运费设置当前只做占位引用，不实现具体公式。

阶段一不做自动最优、不做真实价格计算、不影响订单下单链路。

## 业务定位

报价方案不是最终算法引擎，而是后续报价、下单和成本决策可以读取的配置入口。

阶段一只沉淀以下关系：

```text
报价方案
  -> 方案类型：计费方案 / 成本方案
  -> 适用对象：全部买家 / 买家等级 / 指定买家
  -> 生效优先级：时间重叠时优先级高的方案生效
  -> 仓库范围：全部仓库 / 指定仓库
  -> 费用来源模式：系统费率 / 外部试算
  -> 客户渠道明细
  -> 操作费设置占位
  -> 运费设置占位
```

## 费用来源模式

基础信息中新增一个单选字段：`费用来源模式`。

建议 code：

| code | 展示 | 含义 |
| --- | --- | --- |
| `EXTERNAL_ESTIMATE` | 外部试算 | 后续费用来自履约仓、报价仓或物流商 API 的费用试算结果。阶段一只保存模式，不发起试算。 |
| `INTERNAL_RATE` | 系统费率 | 后续费用来自平台自己维护的操作费、运费、附加费等规则。阶段一只保留入口，不实现规则。 |

阶段一默认建议使用 `EXTERNAL_ESTIMATE`，因为当前操作费设置和运费设置菜单尚未落地，实际费用仍依赖外部试算能力。

## 页面设计

### 主列表

列表字段：

- 报价方案编码。
- 报价方案名称。
- 方案类型。
- 费用来源模式。
- 币种。
- 适用对象。
- 仓库范围。
- 生效时间。
- 失效时间。
- 生效优先级。
- 状态。
- 客户渠道数量。
- 最后更新人。
- 最后更新时间。
- 操作。

筛选字段：

- 报价方案编码。
- 报价方案名称。
- 方案类型。
- 费用来源模式。
- 适用对象类型。
- 状态。

### 新增和编辑

基础信息字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| 报价方案编码 | 新增必填，编辑不可改 | 稳定 code，例如 `L1_US_STANDARD` |
| 报价方案名称 | 是 | 运营可读名称 |
| 方案类型 | 是 | 计费方案 / 成本方案 |
| 费用来源模式 | 是 | 单选：外部试算 / 系统费率 |
| 币种 | 是 | 第一版建议使用现有币种字典或配置来源 |
| 适用对象类型 | 是 | 全部买家 / 买家等级 / 指定买家 |
| 买家等级 | 条件必填 | 适用对象类型为买家等级时必填，读取 `buyer_level` |
| 指定买家 | 条件必填 | 适用对象类型为指定买家时必填 |
| 仓库范围 | 是 | 全部仓库 / 指定仓库 |
| 指定仓库 | 条件必填 | 仓库范围为指定仓库时必填 |
| 生效时间 | 是 | 方案开始可用时间 |
| 失效时间 | 否 | 空表示长期有效 |
| 生效优先级 | 是 | 数字越大优先级越高；时间重叠时优先级高的方案生效 |
| 状态 | 是 | 启用 / 停用 |
| 备注 | 否 | 运营备注 |

### 明细区

阶段一只做一个明细 Tab：`客户渠道配置`。

明细字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| 客户渠道 | 是 | 读取客户渠道管理中已启用的客户渠道 |
| 操作费设置 | 否 | 当前只做占位引用；如果系统费率模式启用，后续再变为必填或按规则校验 |
| 运费设置 | 否 | 当前只做占位引用；如果系统费率模式启用，后续再变为必填或按规则校验 |
| 排序 | 否 | 控制展示顺序 |
| 状态 | 是 | 启用 / 停用 |
| 备注 | 否 | 运营备注 |

页面约束：

- 同一个报价方案下，不能重复绑定同一个客户渠道。
- 只能选择启用状态的客户渠道。
- 阶段一不校验客户渠道背后的系统渠道、仓库或物流商链路是否完整。
- 操作费设置和运费设置当前可以为空，因为真实费用来源优先走外部试算。

## 数据表设计草案

### `quote_scheme`

业务目的：保存报价方案主信息、方案类型、费用来源模式、仓库范围模式和生效优先级。

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `scheme_id` | `bigint(20)` | 是 | 自增 | 主键 | 报价方案 ID |
| `scheme_code` | `varchar(64)` | 是 | 无 | 唯一索引 | 报价方案编码 |
| `scheme_name` | `varchar(200)` | 是 | 无 | 普通索引 | 报价方案名称 |
| `scheme_type` | `varchar(32)` | 是 | `BILLING` | 普通索引 | 方案类型：计费方案 / 成本方案 |
| `fee_source_mode` | `varchar(32)` | 是 | `EXTERNAL_ESTIMATE` | 普通索引 | 费用来源模式 |
| `currency_code` | `varchar(16)` | 是 | 无 | 普通索引 | 币种 code |
| `scope_type` | `varchar(32)` | 是 | `ALL_BUYERS` | 普通索引 | 适用对象类型 |
| `warehouse_scope_mode` | `varchar(32)` | 是 | `ALL_WAREHOUSES` | 普通索引 | 仓库范围模式：全部仓库 / 指定仓库 |
| `effective_time` | `datetime` | 是 | 无 | 普通索引 | 生效时间 |
| `expire_time` | `datetime` | 否 | `null` | 普通索引 | 失效时间 |
| `effective_priority` | `int` | 是 | `0` | 普通索引 | 生效优先级，数字越大越优先 |
| `status` | `varchar(16)` | 是 | `ENABLED` | 普通索引 | 启用状态 |
| `create_by` | `varchar(64)` | 否 | `''` |  | 创建人 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` |  | 更新人 |
| `update_time` | `datetime` | 否 | `null` |  | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` |  | 备注 |

建议索引：

- `uk_quote_scheme_code (scheme_code)`
- `idx_quote_scheme_type_scope (scheme_type, scope_type, status)`
- `idx_quote_scheme_warehouse_scope (warehouse_scope_mode, status)`
- `idx_quote_scheme_effective (effective_time, expire_time, effective_priority, status)`
- `idx_quote_scheme_fee_source (fee_source_mode, status)`

### `quote_scheme_scope`

业务目的：保存报价方案适用对象明细。

`ALL_BUYERS` 不写明细；`BUYER_LEVEL` 写买家等级；`BUYER` 写指定买家。

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `scope_id` | `bigint(20)` | 是 | 自增 | 主键 | 适用范围 ID |
| `scheme_id` | `bigint(20)` | 是 | 无 | 唯一索引一部分 | 报价方案 ID |
| `scope_type` | `varchar(32)` | 是 | 无 | 普通索引 | 适用对象类型 |
| `scope_key` | `varchar(128)` | 是 | 无 | 唯一索引一部分 | 适用对象唯一键，例如 `LEVEL:L1`、`BUYER:1001` |
| `buyer_level_code` | `varchar(32)` | 否 | `null` | 普通索引 | 买家等级 code |
| `buyer_level_name_snapshot` | `varchar(100)` | 否 | `null` |  | 买家等级名称快照 |
| `buyer_id` | `bigint(20)` | 否 | `null` | 普通索引 | 买家 ID |
| `buyer_code_snapshot` | `varchar(64)` | 否 | `''` |  | 买家代码快照 |
| `buyer_name_snapshot` | `varchar(200)` | 否 | `''` |  | 买家名称快照 |
| `buyer_short_name_snapshot` | `varchar(100)` | 否 | `''` |  | 买家简称快照 |
| `create_by` | `varchar(64)` | 否 | `''` |  | 创建人 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |

建议约束：

- `BUYER_LEVEL`：同一方案下不能重复同一个 `buyer_level_code`。
- `BUYER`：同一方案下不能重复同一个 `buyer_id`。
- `ALL_BUYERS`：不写明细。

实现 SQL 时可通过服务层校验配合唯一索引，避免 MySQL 对 nullable 唯一字段的差异导致误判。

### `quote_scheme_warehouse`

业务目的：保存报价方案适用哪些仓库。

`ALL_WAREHOUSES` 不写明细；`INCLUDE` 写指定仓库。阶段一只控制“这个方案适用于哪些仓库”，不在这里保存仓库报价、仓库成本或仓库 API 试算结果。

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `scheme_warehouse_id` | `bigint(20)` | 是 | 自增 | 主键 | 方案仓库范围 ID |
| `scheme_id` | `bigint(20)` | 是 | 无 | 唯一索引一部分 | 报价方案 ID |
| `warehouse_code` | `varchar(64)` | 是 | 无 | 唯一索引一部分 | 系统仓库编码 |
| `warehouse_name_snapshot` | `varchar(200)` | 是 | 无 |  | 仓库名称快照 |
| `warehouse_kind_snapshot` | `varchar(32)` | 否 | `''` | 普通索引 | 仓库类型快照，例如官方仓、第三方仓 |
| `create_by` | `varchar(64)` | 否 | `''` |  | 创建人 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `remark` | `varchar(500)` | 否 | `''` |  | 备注 |

建议索引：

- `uk_quote_scheme_warehouse (scheme_id, warehouse_code)`
- `idx_quote_scheme_warehouse_code (warehouse_code)`
- `idx_quote_scheme_warehouse_kind (warehouse_kind_snapshot)`

### `quote_scheme_channel`

业务目的：保存报价方案下可用客户渠道，以及阶段一的操作费/运费设置占位引用。

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `scheme_channel_id` | `bigint(20)` | 是 | 自增 | 主键 | 方案渠道明细 ID |
| `scheme_id` | `bigint(20)` | 是 | 无 | 唯一索引一部分 | 报价方案 ID |
| `customer_channel_code` | `varchar(64)` | 是 | 无 | 唯一索引一部分 | 客户渠道代码 |
| `customer_channel_name_snapshot` | `varchar(200)` | 是 | 无 |  | 客户渠道名称快照 |
| `operation_fee_code` | `varchar(64)` | 否 | `null` | 普通索引 | 操作费设置占位 code |
| `freight_fee_code` | `varchar(64)` | 否 | `null` | 普通索引 | 运费设置占位 code |
| `status` | `varchar(16)` | 是 | `ENABLED` | 普通索引 | 明细状态 |
| `display_order` | `int` | 是 | `0` | 普通索引 | 排序 |
| `create_by` | `varchar(64)` | 否 | `''` |  | 创建人 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` |  | 更新人 |
| `update_time` | `datetime` | 否 | `null` |  | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` |  | 备注 |

建议索引：

- `uk_quote_scheme_channel (scheme_id, customer_channel_code)`
- `idx_quote_scheme_channel_status (scheme_id, status)`
- `idx_quote_scheme_channel_customer (customer_channel_code, status)`
- `idx_quote_scheme_channel_order (scheme_id, display_order)`

## 字典和选项

建议新增字典：

| 字典类型 | 用途 | 选项 |
| --- | --- | --- |
| `quote_scheme_type` | 方案类型 | `BILLING` / `COST` |
| `quote_scheme_fee_source_mode` | 费用来源模式 | `EXTERNAL_ESTIMATE` / `INTERNAL_RATE` |
| `quote_scheme_scope_type` | 适用对象类型 | `ALL_BUYERS` / `BUYER_LEVEL` / `BUYER` |
| `quote_scheme_warehouse_scope_mode` | 仓库范围模式 | `ALL_WAREHOUSES` / `INCLUDE` |
| `quote_scheme_status` | 方案状态 | `ENABLED` / `DISABLED` |
| `quote_scheme_channel_status` | 方案渠道明细状态 | `ENABLED` / `DISABLED` |

复用现有字典：

- `buyer_level`：买家等级。
- 币种相关字典或已有币种配置来源。

## 权限设计

菜单：`报价方案`，继续使用现有菜单位 `2053`。

建议权限：

| 权限 | 用途 |
| --- | --- |
| `finance:quoteScheme:list` | 菜单和主列表 |
| `finance:quoteScheme:query` | 详情查询 |
| `finance:quoteScheme:add` | 新增报价方案 |
| `finance:quoteScheme:edit` | 编辑报价方案 |
| `finance:quoteScheme:status` | 启停报价方案 |
| `finance:quoteScheme:warehouse` | 维护方案仓库范围 |
| `finance:quoteScheme:channel` | 维护客户渠道配置 |

阶段一最终归入 `finance` 模块。`2053` 报价方案菜单位继续保留，接口、权限和后端实现统一使用 `finance` 命名。

## 接口设计

管理端接口建议：

| 接口 | 说明 |
| --- | --- |
| `GET /finance/admin/quote-schemes/list` | 报价方案列表 |
| `GET /finance/admin/quote-schemes/{schemeId}` | 报价方案详情 |
| `POST /finance/admin/quote-schemes` | 新增报价方案 |
| `PUT /finance/admin/quote-schemes/{schemeId}` | 编辑报价方案 |
| `PUT /finance/admin/quote-schemes/{schemeId}/status` | 启停报价方案 |
| `GET /finance/admin/quote-schemes/{schemeId}/channels/list` | 查询方案客户渠道明细 |
| `POST /finance/admin/quote-schemes/{schemeId}/channels` | 新增方案客户渠道 |
| `PUT /finance/admin/quote-schemes/{schemeId}/channels/{schemeChannelId}` | 编辑方案客户渠道 |
| `DELETE /finance/admin/quote-schemes/{schemeId}/channels/{schemeChannelId}` | 删除方案客户渠道 |
| `GET /finance/admin/quote-schemes/{schemeId}/warehouses` | 查询方案仓库范围 |
| `PUT /finance/admin/quote-schemes/{schemeId}/warehouses` | 保存方案仓库范围 |
| `GET /finance/admin/quote-schemes/options/customer-channels` | 客户渠道选择器 |
| `GET /finance/admin/quote-schemes/options/buyers` | 买家选择器 |
| `GET /finance/admin/quote-schemes/options/warehouses` | 仓库选择器 |
| `GET /finance/admin/quote-schemes/options/fee-placeholders` | 操作费/运费占位选择器 |

阶段一的 `fee-placeholders` 可以返回空列表或少量占位数据；不代表真实计费规则已经完成。

## 校验规则

- 报价方案编码全局唯一，新增后不可编辑。
- 生效时间必须小于失效时间。
- `ALL_BUYERS` 不允许提交范围明细。
- `BUYER_LEVEL` 至少选择一个买家等级。
- `BUYER` 至少选择一个买家。
- `ALL_WAREHOUSES` 不允许提交仓库明细。
- `INCLUDE` 至少选择一个启用仓库。
- 同一报价方案下客户渠道不能重复。
- 新增客户渠道明细时，只能选择启用状态的客户渠道。
- 时间重叠允许存在，但命中时按 `effective_priority` 数字大的方案优先生效。
- 如果同一方案类型、同一适用对象、同一仓库范围下出现时间重叠且优先级相同，启用时应拦截或至少强提示，避免后续匹配结果不稳定。

## 第一版不做

- 不做自动最优客户渠道。
- 不做外部 API 试算调用。
- 不做主仓、报价仓或物流商成本比较。
- 不做操作费、运费、附加费公式。
- 不做手工费率表。
- 不做订单金额快照。
- 不做订单下单链路接入。
- 不做平台利润底线。

## 实施顺序建议

1. 确认本设计方案。
2. 补阶段一 SQL 设计和菜单权限 guard。
3. 在后端 `finance` 模块新增最小 Controller / Service / Mapper。
4. 新增 React 管理端页面 `Finance/QuoteScheme/index`，并保留 `Billing/QuoteScheme/index` 兼容入口。
5. 补前后端契约测试。
6. 执行编译、测试、浏览器页面验证。
7. 追加阶段一实施记录 Markdown。

## 实施收口

1. 权限和接口前缀已确定为 `finance:quoteScheme:*` 与 `/finance/admin/quote-schemes`。
2. 阶段一不新增操作费/运费占位表，只在 `quote_scheme_channel` 保留 `operation_fee_code`、`freight_fee_code` 及名称快照字段。
3. 报价方案放在 `finance` Maven 模块；仓库、买家、客户渠道通过 lookup port 返回只读快照，避免 `finance` 反向依赖 `warehouse`、`buyer`、`logistics` 的 Mapper。
