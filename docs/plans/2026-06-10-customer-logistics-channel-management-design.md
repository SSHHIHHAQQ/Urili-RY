# 客户渠道管理设计草案

日期：2026-06-10

## 当前口径

客户渠道是买家实际看到、实际下单选择的物流渠道。

系统物流渠道管理维护内部渠道和物流商、仓库、发货地址之间的关系；客户渠道管理维护买家侧展示渠道、面单来源、可见买家范围，以及客户渠道到系统渠道的映射。

第一版客户渠道管理先实现：

- 客户渠道主列表。
- 新增、编辑客户渠道基础信息。
- 绑定系统渠道。
- 绑定买家。

平台渠道映射已经确认归属客户渠道管理，但本轮需求先聚焦绑定系统渠道和绑定买家。平台渠道映射不再放回系统渠道管理，后续可在本菜单增加独立 Tab 或子页。

## 业务目标

客户下单时不直接选择系统渠道，也不直接选择物流商渠道，而是选择客户渠道。

后续订单履约链路建议为：

1. 买家看到可用客户渠道。
2. 买家下单选择客户渠道。
3. 系统根据客户渠道找到绑定的系统渠道。
4. 系统渠道再决定可用仓库、发货地址覆写、物流商账号和物流商渠道。

这样买家侧展示和内部物流商接入可以解耦。

## 关键概念

### 渠道类型

| code | 展示 | 含义 |
| --- | --- | --- |
| `WAREHOUSE_LABEL` | 仓库面单 | 对买家而言，面单由仓库、平台管理端、上游系统或物流商链路解决，买家不需要上传面单。 |
| `THIRD_PARTY_LABEL` | 第三方面单 | 面单来自仓库以外的来源，可能由平台获取，也可能由买家上传。 |

### 上传物流面单

该字段只在渠道类型为 `THIRD_PARTY_LABEL` 时出现。

| code | 展示 | 含义 |
| --- | --- | --- |
| `REQUIRED` | 需要上传 | 订单履约时需要准备并上传物流面单。 |
| `NOT_REQUIRED` | 不需要上传 | 当前客户渠道虽然是第三方面单，但第一版流程不要求系统收集或上传面单。 |

### 平台面单获取

该字段只在渠道类型为 `THIRD_PARTY_LABEL` 且上传物流面单为 `REQUIRED` 时出现。

| code | 展示 | 含义 |
| --- | --- | --- |
| `FETCH` | 获取 | 系统后续可以从平台或上游获取物流面单。 |
| `NOT_FETCH` | 不获取 | 系统不从平台或上游获取物流面单。 |

### 客户上传面单支持

该字段只在渠道类型为 `THIRD_PARTY_LABEL` 且上传物流面单为 `REQUIRED` 时出现。

| code | 展示 | 含义 |
| --- | --- | --- |
| `SUPPORTED` | 支持 | 后续买家下单或补资料时可以上传物流面单。 |
| `UNSUPPORTED` | 不支持 | 买家不能上传物流面单。 |

## 动态显示规则

### 基础信息表单

渠道类型放在基础信息第一行。切换渠道类型时，表单字段按以下规则变化：

| 渠道类型 | 上传物流面单 | 平台面单获取 | 客户上传面单支持 | 保存值 |
| --- | --- | --- | --- | --- |
| 仓库面单 | 不显示 | 不显示 | 不显示 | `NOT_REQUIRED` / `NOT_FETCH` / `UNSUPPORTED` |
| 第三方面单 | 显示，必填 | 根据上传物流面单决定 | 根据上传物流面单决定 | 按用户选择保存 |

上传物流面单字段的联动：

| 上传物流面单 | 平台面单获取 | 客户上传面单支持 | 校验 |
| --- | --- | --- | --- |
| 不需要上传 | 不显示 | 不显示 | 保存为 `NOT_FETCH` / `UNSUPPORTED` |
| 需要上传 | 显示，必填 | 显示，必填 | 平台获取和客户上传至少开启一个 |

如果第三方面单选择“需要上传”，但平台面单获取为“不获取”且客户上传面单为“不支持”，保存时直接拦截。因为这代表系统既拿不到面单，也不允许客户提供面单，后续履约没有可执行入口。

如果平台面单获取为“获取”且客户上传面单为“支持”，后续订单侧可以优先平台获取，客户上传作为补充或兜底。第一版客户渠道管理只保存配置，不实现订单侧获取顺序。

## 页面设计

### 主列表

主列表字段建议：

- 客户渠道代码
- 客户渠道名称
- 渠道类型
- 承运商
- 签名服务
- 上传物流面单
- 平台面单获取
- 客户上传面单支持
- 状态
- 绑定系统渠道数量
- 买家范围
- 最后更新人
- 最后更新时间
- 操作

主列表筛选建议：

- 渠道类型
- 状态
- 承运商
- 客户渠道代码
- 客户渠道名称

主列表不展示平台渠道映射数量，避免第一版列表过宽。

### 新增和编辑

新增客户渠道采用两步交互：

1. 先填写基础信息并保存。
2. 保存成功后，再显示配置区 Tabs。

编辑客户渠道时，渠道已经存在，直接展示基础信息和配置区 Tabs。

基础信息字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| 渠道类型 | 是 | 仓库面单 / 第三方面单 |
| 客户渠道代码 | 新增必填，编辑不可改 | 买家侧稳定渠道 code |
| 客户渠道名称 | 是 | 买家侧展示名称 |
| 承运商 | 是 | 使用 `logistics_final_carrier` 字典 |
| 签名服务 | 否 | 直接签名、间接签名、成人签名三个复选框 |
| 上传物流面单 | 条件必填 | 只在第三方面单显示 |
| 平台面单获取 | 条件必填 | 第三方面单且需要上传物流面单时显示 |
| 客户上传面单支持 | 条件必填 | 第三方面单且需要上传物流面单时显示 |
| 状态 | 是 | 启用 / 停用 |
| 排序 | 否 | 同一场景下的展示顺序 |
| 备注 | 否 | 运营备注 |

### 配置区 Tabs

第一版只做两个 Tab：

- 绑定系统渠道
- 绑定买家

#### 绑定系统渠道

用途：维护客户渠道可落到哪些内部系统渠道。

页面行为：

- Tab 内有“绑定系统渠道”按钮。
- 点击后打开弹窗，从已启用系统渠道中选择。
- 支持绑定多个系统渠道。
- 每条绑定关系有状态和排序。
- 第一版不做复杂命中规则；如果未来订单侧需要在多个系统渠道之间自动选择，再增加规则配置。

表格字段：

- 系统渠道代码
- 系统渠道名称
- 承运商
- 签名服务
- 绑定状态
- 排序
- 备注
- 操作

第一版建议校验：

- 同一个客户渠道下，不能重复绑定同一个系统渠道。
- 只能绑定启用状态的系统渠道。
- 如果客户渠道本身是启用状态，至少应该绑定一个启用的系统渠道；否则下单时没有内部渠道可走。

#### 绑定买家

用途：控制哪些买家能看到和使用这个客户渠道。

默认规则：

- 不绑定任何买家时，默认所有买家可用。

绑定方式：

| code | 展示 | 含义 |
| --- | --- | --- |
| `ALL` | 全部买家可用 | 不落明细，所有买家可见。 |
| `INCLUDE` | 可用名单 | 只有选中的买家可见。 |
| `EXCLUDE` | 不可用名单 | 除选中的买家外，其他买家可见。 |

页面行为：

- Tab 初始展示当前范围摘要。
- 未配置时显示“当前未绑定买家，默认所有买家可用”。
- 点击“绑定买家”打开弹窗。
- 弹窗顶部选择“可用名单 / 不可用名单”。
- 弹窗右侧或顶部提供搜索框，支持按买家代码、买家名称、买家简称搜索。
- 买家选择表格字段为买家代码、买家名称、买家简称。
- 保存时，如果选择可用名单或不可用名单，必须至少选择一个买家。
- 如果清空配置，则恢复为 `ALL`。

保存后 Tab 内展示：

- 范围类型。
- 已选买家数量。
- 买家代码、买家名称、买家简称。
- 移除操作。

## 数据表设计

### `logistics_customer_channel`

业务目的：保存客户侧可见物流渠道主数据。

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `customer_channel_code` | `varchar(64)` | 是 | 无 | 主键 | 客户渠道代码 |
| `customer_channel_name` | `varchar(200)` | 是 | 无 | 普通索引 | 客户渠道名称 |
| `channel_type` | `varchar(32)` | 是 | 无 | 普通索引 | 渠道类型 |
| `standard_carrier_code` | `varchar(64)` | 是 | 无 | 普通索引 | 标准承运商 code |
| `signature_services` | `varchar(128)` | 否 | `''` |  | 签名服务 code 集合，逗号分隔 |
| `label_upload_required` | `varchar(16)` | 是 | `NOT_REQUIRED` |  | 上传物流面单 |
| `platform_label_fetch` | `varchar(16)` | 是 | `NOT_FETCH` |  | 平台面单获取 |
| `customer_label_upload_supported` | `varchar(16)` | 是 | `UNSUPPORTED` |  | 客户上传面单支持 |
| `buyer_scope_mode` | `varchar(16)` | 是 | `ALL` | 普通索引 | 买家范围模式 |
| `status` | `varchar(16)` | 是 | `ENABLED` | 普通索引 | 状态 |
| `display_order` | `int` | 是 | `0` | 普通索引 | 排序 |
| `create_by` | `varchar(64)` | 否 | `''` |  | 创建者 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` |  | 更新者 |
| `update_time` | `datetime` | 否 | `null` |  | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` |  | 备注 |

建议索引：

- 主键：`customer_channel_code`
- `idx_logistics_customer_channel_name (customer_channel_name)`
- `idx_logistics_customer_channel_type_status (channel_type, status)`
- `idx_logistics_customer_channel_carrier (standard_carrier_code, status)`
- `idx_logistics_customer_channel_scope (buyer_scope_mode, status)`
- `idx_logistics_customer_channel_order (display_order, customer_channel_code)`

### `logistics_customer_channel_system_mapping`

业务目的：保存客户渠道到系统渠道的绑定关系。

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `mapping_id` | `bigint(20)` | 是 | 自增 | 主键 | 绑定 ID |
| `customer_channel_code` | `varchar(64)` | 是 | 无 | 唯一索引的一部分 | 客户渠道代码 |
| `system_channel_code` | `varchar(64)` | 是 | 无 | 唯一索引的一部分 | 系统渠道代码 |
| `system_channel_name_snapshot` | `varchar(200)` | 是 | 无 |  | 系统渠道名称快照 |
| `standard_carrier_code_snapshot` | `varchar(64)` | 是 | 无 | 普通索引 | 系统渠道承运商快照 |
| `signature_services_snapshot` | `varchar(128)` | 否 | `''` |  | 系统渠道签名服务快照 |
| `status` | `varchar(16)` | 是 | `ENABLED` | 普通索引 | 绑定状态 |
| `display_order` | `int` | 是 | `0` | 普通索引 | 命中顺序或展示顺序 |
| `create_by` | `varchar(64)` | 否 | `''` |  | 创建者 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` |  | 更新者 |
| `update_time` | `datetime` | 否 | `null` |  | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` |  | 备注 |

建议索引：

- 主键：`mapping_id`
- 唯一键：`uk_logistics_customer_channel_system (customer_channel_code, system_channel_code)`
- `idx_logistics_customer_channel_system_status (customer_channel_code, status)`
- `idx_logistics_customer_channel_system_channel (system_channel_code, status)`
- `idx_logistics_customer_channel_system_order (customer_channel_code, display_order)`

### `logistics_customer_channel_buyer_scope`

业务目的：保存客户渠道绑定买家的明细集合。

`ALL` 模式不写明细；`INCLUDE` 和 `EXCLUDE` 模式写选中的买家。

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `scope_id` | `bigint(20)` | 是 | 自增 | 主键 | 范围明细 ID |
| `customer_channel_code` | `varchar(64)` | 是 | 无 | 唯一索引的一部分 | 客户渠道代码 |
| `buyer_id` | `bigint(20)` | 是 | 无 | 唯一索引的一部分 | 买家 ID |
| `buyer_code_snapshot` | `varchar(64)` | 是 | 无 | 普通索引 | 买家代码快照 |
| `buyer_name_snapshot` | `varchar(200)` | 是 | 无 | 普通索引 | 买家名称快照 |
| `buyer_short_name_snapshot` | `varchar(100)` | 否 | `''` | 普通索引 | 买家简称快照 |
| `create_by` | `varchar(64)` | 否 | `''` |  | 创建者 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `remark` | `varchar(500)` | 否 | `''` |  | 备注 |

建议索引：

- 主键：`scope_id`
- 唯一键：`uk_logistics_customer_channel_buyer (customer_channel_code, buyer_id)`
- `idx_logistics_customer_channel_buyer_id (buyer_id)`
- `idx_logistics_customer_channel_buyer_code (buyer_code_snapshot)`
- `idx_logistics_customer_channel_buyer_name (buyer_name_snapshot)`

### 后续平台渠道映射表

平台渠道映射归属客户渠道管理，但本轮先不放进第一版两个 Tab。

后续建议表名：

- `logistics_customer_platform_channel_mapping`

建议字段方向：

- 客户渠道代码
- 平台类型
- 平台账号或店铺标识
- 平台渠道代码
- 平台渠道名称
- 状态
- 创建和更新时间
- 备注

是否本轮一起建表，需要在平台类型、平台账号来源、店铺维度和唯一约束确认后再定。

## 字典建议

| 字典类型 | 用途 | code |
| --- | --- | --- |
| `logistics_customer_channel_type` | 客户渠道类型 | `WAREHOUSE_LABEL` / `THIRD_PARTY_LABEL` |
| `logistics_label_upload_required` | 上传物流面单 | `REQUIRED` / `NOT_REQUIRED` |
| `logistics_platform_label_fetch` | 平台面单获取 | `FETCH` / `NOT_FETCH` |
| `logistics_customer_label_upload_support` | 客户上传面单支持 | `SUPPORTED` / `UNSUPPORTED` |
| `logistics_customer_channel_scope_mode` | 买家范围模式 | `ALL` / `INCLUDE` / `EXCLUDE` |
| `logistics_customer_channel_status` | 客户渠道状态 | `ENABLED` / `DISABLED` |
| `logistics_customer_channel_binding_status` | 客户渠道绑定状态 | `ENABLED` / `DISABLED` |

承运商继续复用 `logistics_final_carrier`。

签名服务继续复用 `logistics_signature_service`。

## 权限建议

| 权限 | 用途 |
| --- | --- |
| `logistics:customerChannel:list` | 菜单和主列表 |
| `logistics:customerChannel:query` | 详情、绑定查询 |
| `logistics:customerChannel:add` | 新增客户渠道 |
| `logistics:customerChannel:edit` | 编辑客户渠道 |
| `logistics:customerChannel:status` | 启停客户渠道 |
| `logistics:customerChannel:binding` | 维护系统渠道绑定 |
| `logistics:customerChannel:buyer` | 维护绑定买家 |

当前历史菜单可能仍有 `channel:customer:list`、`channel:customer:query`、`channel:customer:add`。正式 SQL 迁移时建议和系统渠道一样升级为 `logistics:customerChannel:*`，并保留 guard，避免误覆盖已有菜单。

## 接口建议

| 接口 | 说明 |
| --- | --- |
| `GET /logistics/admin/customer-channels/list` | 主列表 |
| `GET /logistics/admin/customer-channels/{code}` | 详情 |
| `POST /logistics/admin/customer-channels` | 新增 |
| `PUT /logistics/admin/customer-channels/{code}` | 编辑 |
| `PUT /logistics/admin/customer-channels/{code}/status` | 启停 |
| `GET /logistics/admin/customer-channels/{code}/system-mappings` | 查询绑定系统渠道 |
| `POST /logistics/admin/customer-channels/{code}/system-mappings` | 新增绑定系统渠道 |
| `PUT /logistics/admin/customer-channels/{code}/system-mappings/{mappingId}` | 编辑绑定系统渠道 |
| `DELETE /logistics/admin/customer-channels/{code}/system-mappings/{mappingId}` | 删除绑定系统渠道 |
| `GET /logistics/admin/customer-channels/{code}/buyer-scope` | 查询绑定买家 |
| `PUT /logistics/admin/customer-channels/{code}/buyer-scope` | 保存绑定买家 |
| `GET /logistics/admin/customer-channels/options/system-channels` | 系统渠道选择器 |
| `GET /logistics/admin/customer-channels/options/buyers` | 买家选择器 |

## 第一版校验规则

- 客户渠道代码新增后不可编辑。
- 客户渠道代码全局唯一。
- 渠道类型必填。
- 仓库面单隐藏第三方面单相关字段，并保存默认值。
- 第三方面单选择需要上传物流面单时，平台获取和客户上传至少开启一个。
- 客户渠道启用前建议至少绑定一个启用系统渠道。
- 绑定买家为 `ALL` 时不写买家明细。
- 绑定买家为 `INCLUDE` 或 `EXCLUDE` 时必须选择至少一个买家。
- 买家选择只绑定买家主体，不绑定买家子账号。

## 待确认点

1. 一个客户渠道绑定多个系统渠道时，第一版是否只按排序展示，暂不实现自动命中规则。
2. 客户渠道启用时，如果没有绑定系统渠道，是强制拦截还是只做提示。
3. 第三方面单且需要上传物流面单时，如果平台获取和客户上传都开启，后续订单侧是否默认平台获取优先。
4. 平台渠道映射是否本轮一起纳入客户渠道管理，还是等平台账号和店铺维度明确后再加。
