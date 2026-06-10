# 物流商管理通用表设计草案

日期：2026-06-10

## 设计目标

本文件只设计物流商管理的通用核心表。通用核心表必须适用于任意物流商 API 接入方，不允许出现某一家系统专属字段、专属接口字段或专属业务语义。

本文件不生成 SQL，不写运行库。

## 边界原则

### 通用核心表可以保存什么

- 物流商接入方的通用身份：接入编号、接入方类型、展示名称、API base URL、状态、授权状态。
- 物流商返回的通用物流商渠道：外部渠道 code、外部渠道名称、状态、原始响应摘要。
- URILI 内部稳定系统渠道。
- 物流商渠道到系统渠道和标准最终承运商的映射。
- 面单订单的通用快照：业务单号、外部订单号、状态、渠道、费用快照、异常摘要。
- 面单包裹/文件的通用快照：tracking number、label 文件类型、文件引用。
- 外部请求脱敏日志。

### 通用核心表不能保存什么

- 不能保存某一家物流商的专属凭据字段名。
- 不能保存某一家物流商的用户字段，例如外部用户 ID、客户代码、仓库账号、开发者账号。
- 不能保存某一家物流商的地址编码、渠道字段名或接口专属状态字段。
- 不能为了当前第一家接入方，把单一系统字段提前塞进通用表。

### 单一系统字段放哪里

单一系统字段必须放到接入方扩展表，命名按接入方独立，例如：

```text
logistics_<provider>_connection
logistics_<provider>_token_cache
logistics_<provider>_shipper_address_candidate
```

业务代码边界也一样：通用 Service/Facade 只依赖通用模型；具体适配器负责把接入方字段转换为通用 DTO。

## 模块归属

管理端入口放在 `ruoyi-admin`：

```text
RuoYi-Vue/ruoyi-admin/src/main/java/com/ruoyi/web/controller/logistics/
```

物流商业务模块放在独立 `logistics`：

```text
RuoYi-Vue/logistics/src/main/java/com/ruoyi/logistics/
RuoYi-Vue/logistics/src/main/resources/mapper/logistics/
```

前端当前放在管理端 `react-ui`：

```text
react-ui/src/pages/Logistics/Carrier/
react-ui/src/services/logistics/carrier.ts
```

边界说明：

- Controller、管理端权限、操作日志入口属于 `ruoyi-admin`。
- Service、Domain、Mapper、通用 Facade 和具体物流商 Adapter 属于 `logistics`。
- 后续订单/履约模块调用 `logistics` 暴露的 Service/Facade，不反向依赖 `ruoyi-admin`。

## 表设计总览

| 表名 | 目的 | 是否第一版需要 |
| --- | --- | --- |
| `logistics_carrier_connection` | 物流商 API 接入方通用配置 | 是 |
| `logistics_carrier_channel_candidate` | 物流商原始物流商渠道 | 是 |
| `logistics_system_channel` | URILI 内部稳定系统渠道 | 是 |
| `logistics_carrier_channel_mapping` | 物流商渠道到系统渠道/最终承运商映射 | 是 |
| `logistics_label_order` | 创建面单后的订单级通用快照 | 是 |
| `logistics_label_package` | 每箱/每张面单文件与 tracking 快照 | 是 |
| `logistics_carrier_request_log` | 外部请求脱敏日志 | 是 |

不在通用核心表中设计的内容：

- 接入方凭据扩展表。
- 接入方短期 token 缓存表。
- 接入方发货地址编码表。
- Scan Form、manifest 等后续专项能力表。

## `logistics_carrier_connection`

业务目的：保存平台与物流商 API 接入方的通用连接信息。

业务逻辑：

- 一条记录代表一个平台统一物流商账号或接入实例。
- 只保存所有接入方都具备的通用字段。
- 不保存任何具体凭据字段，具体凭据进入接入方扩展表。

字段草案：

| 字段 | 类型建议 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `connection_code` | `varchar(64)` | 是 | 无 | 主键 | 物流商接入编号 |
| `provider_kind` | `varchar(32)` | 是 | 无 | 普通索引 | 接入方类型 |
| `connection_name` | `varchar(200)` | 是 | 无 |  | 管理端展示名称 |
| `api_base_url` | `varchar(500)` | 是 | 无 |  | API base URL |
| `status` | `varchar(16)` | 是 | `ENABLED` | 普通索引 | 接入启停状态 |
| `credential_status` | `varchar(32)` | 是 | `UNCONFIGURED` | 普通索引 | 凭据状态 |
| `last_authorized_time` | `datetime` | 否 | `null` |  | 最近授权成功时间 |
| `last_channel_sync_time` | `datetime` | 否 | `null` |  | 最近物流商渠道同步时间 |
| `display_order` | `int` | 是 | `0` | 普通索引 | 展示排序 |
| `create_by` | `varchar(64)` | 否 | `''` |  | 创建者 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` |  | 更新者 |
| `update_time` | `datetime` | 否 | `null` |  | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` |  | 备注 |

设计说明：

- 这里不放凭据密文。不同物流商凭据字段不同，必须由扩展表承载。
- `provider_kind` 只是路由适配器的通用类型，不代表最终承运商。

## `logistics_carrier_channel_candidate`

业务目的：保存物流商系统返回的原始物流商渠道。

业务逻辑：

- 物流商渠道只代表“外部系统说它有这个服务”。
- 物流商渠道不等于系统渠道。
- 物流商渠道消失时标记为 `MISSING`，不物理删除。
- 具体物流商返回字段由适配器映射到通用字段。

字段草案：

| 字段 | 类型建议 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `connection_code` | `varchar(64)` | 是 | 无 | 联合主键 | 物流商接入编号 |
| `external_channel_code` | `varchar(128)` | 是 | 无 | 联合主键 | 外部渠道代码 |
| `external_channel_name` | `varchar(255)` | 是 | 无 | 普通索引 | 外部渠道名称 |
| `raw_final_carrier_text` | `varchar(255)` | 否 | `''` |  | 外部返回或人工识别的承运商原文 |
| `status` | `varchar(16)` | 是 | `ACTIVE` | 普通索引 | 物流商渠道状态 |
| `sync_batch_id` | `varchar(64)` | 是 | 无 | 普通索引 | 同步批次 |
| `source_payload_json` | `json` | 否 | `null` |  | 脱敏原始响应 |
| `source_payload_hash` | `varchar(64)` | 否 | `''` | 普通索引 | 响应 hash |
| `first_seen_time` | `datetime` | 是 | 无 |  | 首次发现 |
| `last_seen_time` | `datetime` | 是 | 无 |  | 最近发现 |
| `update_time` | `datetime` | 是 | 无 |  | 更新时间 |

## `logistics_system_channel`

业务目的：保存 URILI 内部稳定使用的系统渠道。

业务逻辑：

- 系统渠道是订单、履约、报价调用时使用的内部稳定编码。
- 第一版系统渠道全局唯一。
- 标准最终承运商来自字典。

字段草案：

| 字段 | 类型建议 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `system_channel_code` | `varchar(64)` | 是 | 无 | 主键 | 系统渠道代码 |
| `system_channel_name` | `varchar(200)` | 是 | 无 | 唯一建议 | 系统渠道名称 |
| `standard_carrier_code` | `varchar(64)` | 是 | 无 | 普通索引 | 标准最终承运商字典值 |
| `service_level` | `varchar(64)` | 否 | `''` |  | 服务等级 |
| `status` | `varchar(16)` | 是 | `ENABLED` | 普通索引 | 启停状态 |
| `display_order` | `int` | 是 | `0` | 普通索引 | 排序 |
| `create_by` | `varchar(64)` | 否 | `''` |  | 创建者 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` |  | 更新者 |
| `update_time` | `datetime` | 否 | `null` |  | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` |  | 备注 |

## `logistics_carrier_channel_mapping`

业务目的：保存物流商渠道到系统渠道和标准最终承运商的映射关系。

业务逻辑：

- 外部模块可以指定物流商接入和系统渠道下单。
- 同一个系统渠道可以被多个物流商接入映射。
- 同一个物流商渠道第一版只允许一个启用映射，避免下单歧义。

字段草案：

| 字段 | 类型建议 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `mapping_id` | `bigint` | 是 | 自增 | 主键 | 映射 ID |
| `connection_code` | `varchar(64)` | 是 | 无 | 普通索引 | 物流商接入编号 |
| `external_channel_code` | `varchar(128)` | 是 | 无 | 普通索引 | 外部渠道代码 |
| `external_channel_name_snapshot` | `varchar(255)` | 是 | 无 |  | 外部渠道名称快照 |
| `system_channel_code` | `varchar(64)` | 是 | 无 | 普通索引 | 系统渠道代码 |
| `system_channel_name_snapshot` | `varchar(200)` | 是 | 无 |  | 系统渠道名称快照 |
| `standard_carrier_code` | `varchar(64)` | 是 | 无 | 普通索引 | 标准最终承运商 |
| `status` | `varchar(16)` | 是 | `ENABLED` | 普通索引 | 映射启停状态 |
| `create_by` | `varchar(64)` | 否 | `''` |  | 创建者 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` |  | 更新者 |
| `update_time` | `datetime` | 否 | `null` |  | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` |  | 备注 |

建议约束：

- 启用映射必须保证同一个 `connection_code + external_channel_code` 不出现多个启用关系。
- MySQL 对部分唯一索引和状态过滤支持有限，最终 SQL 需要结合状态字段和 Service guard 处理。

## `logistics_label_order`

业务目的：保存创建面单后的订单级通用快照。

业务逻辑：

- 本表不是平台订单事实源，也不是财务流水。
- 本表用于保障幂等、取消、获取面单和外部模块查询结果。
- 外部调用方传入的全局唯一业务单号必须唯一。
- 具体物流商订单状态由适配器归一化后写入通用字段，原始响应进入脱敏响应摘要。

字段草案：

| 字段 | 类型建议 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `label_order_id` | `bigint` | 是 | 自增 | 主键 | 面单订单 ID |
| `business_order_no` | `varchar(128)` | 是 | 无 | 唯一 | 外部全局唯一业务单号 |
| `connection_code` | `varchar(64)` | 是 | 无 | 普通索引 | 物流商接入编号 |
| `provider_kind` | `varchar(32)` | 是 | 无 | 普通索引 | 接入方类型 |
| `provider_order_no` | `varchar(128)` | 否 | `''` | 普通索引 | 物流商订单号 |
| `provider_reference_no` | `varchar(128)` | 否 | `''` | 普通索引 | 物流商参考号 |
| `external_channel_code` | `varchar(128)` | 是 | 无 | 普通索引 | 外部渠道代码 |
| `external_channel_name_snapshot` | `varchar(255)` | 是 | 无 |  | 外部渠道名称快照 |
| `system_channel_code` | `varchar(64)` | 是 | 无 | 普通索引 | 系统渠道代码 |
| `standard_carrier_code` | `varchar(64)` | 是 | 无 | 普通索引 | 标准最终承运商 |
| `label_status` | `varchar(32)` | 是 | `CREATING` | 普通索引 | 面单状态 |
| `provider_status_code` | `varchar(64)` | 否 | `''` | 普通索引 | 物流商原始状态码 |
| `provider_status_text` | `varchar(255)` | 否 | `''` |  | 物流商原始状态说明 |
| `provider_error_message` | `varchar(1000)` | 否 | `''` |  | 物流商错误信息 |
| `zone` | `varchar(64)` | 否 | `''` |  | 分区 |
| `charge_weight` | `decimal(18,4)` | 否 | `null` |  | 计费重 |
| `charge_weight_unit` | `varchar(16)` | 否 | `''` |  | 计费重单位 |
| `currency_code` | `varchar(16)` | 否 | `''` |  | 币种 |
| `total_charge_snapshot` | `decimal(18,4)` | 否 | `null` |  | 总费用快照 |
| `request_payload_hash` | `varchar(64)` | 否 | `''` | 普通索引 | 下单请求 hash |
| `response_payload_json` | `json` | 否 | `null` |  | 脱敏响应摘要 |
| `cancel_time` | `datetime` | 否 | `null` |  | 取消时间 |
| `create_by` | `varchar(64)` | 否 | `''` |  | 创建者 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` |  | 更新者 |
| `update_time` | `datetime` | 否 | `null` |  | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` |  | 备注 |

建议约束：

- `uk_logistics_label_business_order_no(business_order_no)`
- `idx_logistics_label_provider_order(connection_code, provider_order_no)`

## `logistics_label_package`

业务目的：保存每箱/每张面单的 tracking 和文件快照。

业务逻辑：

- 一个面单订单可以返回多张 label。
- 只保存通用 tracking、文件类型、外部文件 URL 和系统文件引用。
- 文件内容走统一文件存储入口，不把文件二进制直接塞进本表。

字段草案：

| 字段 | 类型建议 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `label_package_id` | `bigint` | 是 | 自增 | 主键 | 面单包裹 ID |
| `label_order_id` | `bigint` | 是 | 无 | 普通索引 | 面单订单 ID |
| `tracking_number` | `varchar(128)` | 否 | `''` | 普通索引 | 跟踪号 |
| `provider_package_no` | `varchar(128)` | 否 | `''` | 普通索引 | 物流商包裹/箱号 |
| `label_file_type` | `varchar(16)` | 是 | 无 | 普通索引 | PDF/ZPL/PNG |
| `external_label_url` | `varchar(1000)` | 否 | `''` |  | 外部 label URL |
| `stored_file_id` | `varchar(128)` | 否 | `''` | 普通索引 | 系统文件 ID |
| `stored_file_url` | `varchar(1000)` | 否 | `''` |  | 系统文件 URL |
| `currency_code` | `varchar(16)` | 否 | `''` |  | 币种 |
| `shipping_charge_snapshot` | `decimal(18,4)` | 否 | `null` |  | 运费快照 |
| `fee_detail_json` | `json` | 否 | `null` |  | 每箱费用明细 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `update_time` | `datetime` | 否 | `null` |  | 更新时间 |

建议约束：

- `idx_logistics_label_tracking(tracking_number)`
- `idx_logistics_label_order_package(label_order_id, provider_package_no)`

## `logistics_carrier_request_log`

业务目的：保存物流商 API 请求脱敏日志。

业务逻辑：

- 只追加，不物理删除。
- 不保存完整 token、key、access token、Authorization、完整 PII。
- 费用试算结果只进入此表，不单独做费用记录或看板。

字段草案：

| 字段 | 类型建议 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `request_log_id` | `bigint` | 是 | 自增 | 主键 | 日志 ID |
| `connection_code` | `varchar(64)` | 是 | 无 | 普通索引 | 物流商接入编号 |
| `provider_kind` | `varchar(32)` | 是 | 无 | 普通索引 | 接入方类型 |
| `trace_id` | `varchar(64)` | 是 | 无 | 唯一建议 | 请求 trace |
| `operation` | `varchar(64)` | 是 | 无 | 普通索引 | 操作类型 |
| `endpoint` | `varchar(255)` | 是 | 无 |  | 请求路径 |
| `http_method` | `varchar(16)` | 是 | 无 |  | HTTP 方法 |
| `business_order_no` | `varchar(128)` | 否 | `''` | 普通索引 | 外部业务单号 |
| `provider_order_no` | `varchar(128)` | 否 | `''` | 普通索引 | 物流商订单号 |
| `request_time` | `datetime` | 是 | 无 | 普通索引 | 请求时间 |
| `response_time` | `datetime` | 否 | `null` |  | 响应时间 |
| `duration_ms` | `bigint` | 否 | `null` |  | 耗时 |
| `http_status` | `int` | 否 | `null` |  | HTTP 状态 |
| `provider_code` | `varchar(64)` | 否 | `''` | 普通索引 | 物流商响应 code |
| `provider_message` | `varchar(1000)` | 否 | `''` |  | 物流商响应 message |
| `status` | `varchar(16)` | 是 | 无 | 普通索引 | SUCCESS/FAILED |
| `request_payload_redacted` | `json` | 否 | `null` |  | 脱敏请求 |
| `response_payload_redacted` | `json` | 否 | `null` |  | 脱敏响应 |

## 字典设计

建议新增或使用以下通用字典：

| dict_type | 目的 | 示例值 |
| --- | --- | --- |
| `logistics_provider_kind` | 物流商接入方类型 | 由具体接入方 seed 扩展 |
| `logistics_connection_status` | 接入启停状态 | `ENABLED` / `DISABLED` |
| `logistics_credential_status` | 凭据状态 | `UNCONFIGURED` / `CONFIGURED` / `INVALID` |
| `logistics_channel_status` | 物流商渠道状态 | `ACTIVE` / `MISSING` |
| `logistics_mapping_status` | 渠道映射状态 | `ENABLED` / `DISABLED` |
| `logistics_label_status` | 面单状态 | `CREATING` / `CREATED` / `PENDING_LABEL` / `FAILED` / `CANCELLED` / `CANCEL_FAILED` |
| `logistics_label_file_type` | 面单文件格式 | `PDF` / `ZPL` / `PNG` |
| `logistics_final_carrier` | 标准最终承运商 | 按用户提供清单生成预览后确认 |
| `logistics_signature_service` | 签名服务 | `SSF` / `ASS` / `DSO` |

## 权限设计

建议菜单和按钮权限：

| 权限 | 用途 |
| --- | --- |
| `logistics:carrier:list` | 查看物流商管理菜单/列表 |
| `logistics:carrier:query` | 查询物流商详情、物流商渠道、面单记录 |
| `logistics:carrier:add` | 新增物流商接入 |
| `logistics:carrier:edit` | 编辑接入基础信息和启停 |
| `logistics:carrier:credential` | 配置或重新授权凭据 |
| `logistics:carrier:sync` | 同步物流商渠道 |
| `logistics:carrier:channel` | 维护系统渠道和渠道映射 |
| `logistics:carrier:label` | 管理端查看/获取/取消面单 |
| `logistics:carrier:log` | 查看外部请求日志 |

内部业务调用的报价、创建面单、获取面单、取消面单能力，建议通过 Service/Facade 控制，不直接暴露为管理端按钮权限。

## 初始化数据

第一版通用初始化建议：

- 物流商管理菜单 `2054` 下的按钮权限。
- 通用状态字典。
- `logistics_final_carrier` 字典预览确认后再生成 seed。

具体接入方类型、接入方扩展表和接入方凭据，不在本通用方案中初始化。

## 迁移与回滚

迁移：

- 所有 DDL/DML 必须生成带确认 token 的幂等 SQL。
- 执行前必须确认当前激活数据源。
- 执行记录必须写明目标环境、连接来源、执行命令类型和影响范围。

回滚：

- 未产生面单记录前，可以通过 guarded SQL 删除初始化字典和空表。
- 一旦产生面单记录、物流商渠道映射或请求日志，不允许直接 drop 表回滚；只能停用入口或追加修正迁移。
- 请求日志、面单订单、label 包裹记录原则上只追加，不物理删除。

## 待确认

1. 是否同意通用核心表不保存任何接入方专属凭据字段。
2. 是否同意第一版同时建 `logistics_system_channel`，为“系统渠道管理”提供真实表。
3. 是否同意通用 SQL 与具体接入方扩展 SQL 分开生成、分开确认。
