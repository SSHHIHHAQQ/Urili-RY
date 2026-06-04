# 上游系统管理表字段说明

日期：2026-06-04

本文只解释字段含义，不代表已经执行建表。执行任何 DDL 前仍需再次确认。

## 总体关系

- `upstream_system_connection`：一条主仓接入，例如一个领星主仓。
- `upstream_system_warehouse_candidate`：从领星拉回来的仓库候选。
- `upstream_system_warehouse_pairing`：系统仓库和领星仓库的正式配对。
- `upstream_system_logistics_channel_candidate`：从领星拉回来的物流渠道候选。
- `upstream_system_logistics_channel_pairing`：系统物流渠道和领星主仓物流渠道的正式配对。
- `upstream_system_sku_candidate`：从领星拉回来的 SKU 候选。
- `upstream_system_sku_pairing`：系统 SKU 和领星 masterSku 的正式配对。
- `upstream_system_sku_sync_state`：每个主仓的 SKU 同步状态。
- `upstream_system_request_log`：调用领星接口的请求日志。
- `upstream_system_sku_pairing_audit_event`：SKU 配对和解除配对的审计记录。

## `upstream_system_connection`

用途：保存一个领星主仓接入。它管“这个主仓是谁、怎么授权、是否启用、排在第几个”，不管库存、订单、财务。

| 字段 | 中文注释 |
| --- | --- |
| `connection_code` | 主仓接入编号。系统内部识别一条接入记录的唯一编码，例如 `LX-CA012-0001`。 |
| `system_kind` | 上游系统类型。当前主要是 `LINGXING_WMS`，以后如果接 WMS、ERP，也用这个字段区分。 |
| `master_warehouse_name` | 主仓显示名称。给管理后台看的名称，例如“领星-美西主仓”。 |
| `settlement_type` | 结算类型。说明这条主仓接入以后费用由谁承担或如何结算，保存 code，不保存中文。 |
| `app_key_mask` | 脱敏后的 appKey。只给页面展示，例如 `abc****xyz`，不能用于真实请求。 |
| `app_secret_mask` | 脱敏后的 appSecret。只给页面展示，不能用于真实请求。 |
| `app_key_ciphertext` | 加密后的 appKey。后端调用领星接口时解密使用，前端永远不返回。 |
| `app_secret_ciphertext` | 加密后的 appSecret。后端调用领星接口时解密使用，前端永远不返回。 |
| `credential_key_id` | 加密密钥版本。以后换加密密钥时，用它判断这条凭证是哪一版密钥加密的。 |
| `status` | 接入状态。表示这条主仓接入是否启用，例如 `ENABLED`、`DISABLED`。 |
| `credential_status` | 凭证状态。表示授权信息是否可用，例如已配置、失效、授权失败。 |
| `enabled_capabilities` | 已启用能力列表。保存这个主仓支持哪些能力，例如仓库同步、渠道同步、SKU 同步。 |
| `display_order` | 左侧菜单排序。数字越小越靠前，拖拽排序时只改这个字段。 |
| `last_authorized_time` | 最近一次授权成功时间。用于判断这条接入最近什么时候验证过凭证。 |
| `last_sync_time` | 最近一次同步时间。主仓下仓库、渠道、SKU 任一核心同步完成后可更新。 |
| `request_log_count` | 请求日志数量摘要。页面上快速展示“已产生多少条外部请求日志”。 |
| `create_by` | 创建人。记录是谁创建了这条主仓接入。 |
| `create_time` | 创建时间。记录这条主仓接入什么时候创建。 |
| `update_by` | 最近更新人。记录最后是谁修改了这条主仓接入。 |
| `update_time` | 最近更新时间。记录最后一次修改时间。 |
| `remark` | 备注。人工填写的补充说明。 |

## `upstream_system_warehouse_candidate`

用途：保存从领星接口拉回来的仓库列表。它只是“候选”，不是系统仓库主数据。

| 字段 | 中文注释 |
| --- | --- |
| `connection_code` | 主仓接入编号。表示这个领星仓库来自哪一个主仓接入。 |
| `warehouse_code` | 领星仓库代码。领星返回的仓库唯一标识。 |
| `warehouse_name` | 领星仓库名称。领星返回的仓库中文或展示名称。 |
| `country_code` | 国家或地区代码。领星返回的仓库所在国家或地区，例如 `US`。 |
| `status` | 候选状态。表示这个候选是否仍然在领星接口里存在，例如 `ACTIVE`、`MISSING`。 |
| `sync_batch_id` | 同步批次号。一次同步生成一个批次，用来追踪这条数据是哪次同步来的。 |
| `first_seen_time` | 首次发现时间。第一次从领星拉到这个仓库的时间。 |
| `last_seen_time` | 最近发现时间。最近一次从领星同步时还能看到这个仓库的时间。 |
| `update_time` | 更新时间。候选快照最后一次被更新的时间。 |

## `upstream_system_warehouse_pairing`

用途：保存系统仓库和领星仓库的正式配对关系。规则是一对一。

| 字段 | 中文注释 |
| --- | --- |
| `warehouse_pairing_id` | 配对记录主键。数据库内部使用的自增 ID。 |
| `connection_code` | 主仓接入编号。表示配对的是哪个领星主仓接入下的仓库。 |
| `upstream_warehouse_code` | 领星仓库代码。被配对的领星仓库。 |
| `upstream_warehouse_name` | 领星仓库名称快照。配对时记录一份名称，方便历史查看。 |
| `system_warehouse_code` | 系统仓库代码。我们自己系统里的仓库编码。这个字段全局唯一，保证系统仓库只能配一次。 |
| `system_warehouse_name` | 系统仓库名称快照。配对时记录一份名称，方便页面展示和历史追踪。 |
| `status` | 配对状态。表示这条配对是否有效，例如 `ACTIVE`。 |
| `create_by` | 创建人。记录是谁建立了这个配对。 |
| `create_time` | 创建时间。记录配对建立时间。 |
| `update_by` | 最近更新人。记录最后是谁修改了这条配对。 |
| `update_time` | 最近更新时间。记录最后一次修改时间。 |
| `remark` | 备注。人工填写的补充说明，例如为什么这样配。 |

约束说明：

- `system_warehouse_code` 唯一：系统仓库只能配对一次，不可重复。
- `connection_code + upstream_warehouse_code` 唯一：同一主仓接入下，一个领星仓库只能配对一次。

## `upstream_system_logistics_channel_candidate`

用途：保存从领星接口拉回来的物流渠道列表。它只是“候选”，不是系统物流渠道主数据。

| 字段 | 中文注释 |
| --- | --- |
| `connection_code` | 主仓接入编号。表示这个渠道来自哪一个主仓接入。 |
| `warehouse_code` | 领星仓库代码。领星物流渠道是按仓库拉取的，所以要保留仓库代码。 |
| `channel_code` | 领星物流渠道代码。领星返回的渠道标识。 |
| `channel_name` | 领星物流渠道名称。领星返回的渠道展示名称。 |
| `status` | 候选状态。表示这个渠道是否仍然在领星接口里存在，例如 `ACTIVE`、`MISSING`。 |
| `sync_batch_id` | 同步批次号。一次同步生成一个批次，用来追踪这条数据是哪次同步来的。 |
| `first_seen_time` | 首次发现时间。第一次从领星拉到这个渠道的时间。 |
| `last_seen_time` | 最近发现时间。最近一次从领星同步时还能看到这个渠道的时间。 |
| `update_time` | 更新时间。候选快照最后一次被更新的时间。 |

## `upstream_system_logistics_channel_pairing`

用途：保存系统物流渠道和领星主仓物流渠道的正式配对关系。

| 字段 | 中文注释 |
| --- | --- |
| `logistics_channel_pairing_id` | 配对记录主键。数据库内部使用的自增 ID。 |
| `connection_code` | 主仓接入编号。表示配对的是哪个领星主仓接入下的渠道。 |
| `upstream_channel_code` | 领星物流渠道代码。被配对的领星主仓渠道。 |
| `upstream_channel_name` | 领星物流渠道名称快照。配对时记录一份名称，方便历史查看。 |
| `system_channel_code` | 系统物流渠道代码。我们自己系统里的渠道编码。这个字段全局唯一，保证系统渠道只能配一次。 |
| `system_channel_name` | 系统物流渠道名称快照。配对时记录一份名称，方便页面展示和历史追踪。 |
| `status` | 配对状态。表示这条配对是否有效，例如 `ACTIVE`。 |
| `create_by` | 创建人。记录是谁建立了这个配对。 |
| `create_time` | 创建时间。记录配对建立时间。 |
| `update_by` | 最近更新人。记录最后是谁修改了这条配对。 |
| `update_time` | 最近更新时间。记录最后一次修改时间。 |
| `remark` | 备注。人工填写的补充说明，例如为什么这样配。 |

约束说明：

- `system_channel_code` 唯一：系统物流渠道只能配对一次，不可重复。
- `connection_code + upstream_channel_code` 不唯一：同一个领星主仓渠道允许配对多个系统渠道。

## `upstream_system_sku_candidate`

用途：保存从领星拉回来的 SKU 列表。它只是上游 SKU 候选，不是系统商品主数据。

| 字段 | 中文注释 |
| --- | --- |
| `connection_code` | 主仓接入编号。表示这个 SKU 来自哪一个主仓接入。 |
| `master_sku` | 领星 masterSku。领星接口里的主 SKU 编码，后续发货出站要用它。 |
| `master_product_name` | 领星产品名称。领星接口返回的产品名，用于搜索和展示。 |
| `status` | 候选状态。表示这个 masterSku 是否还在领星存在，例如 `ACTIVE`、`MISSING`。 |
| `search_text` | 搜索文本。把 SKU、产品名等拼成搜索字段，提高页面查询效率。 |
| `sync_batch_id` | 同步批次号。一次同步生成一个批次，用来追踪这条数据是哪次同步来的。 |
| `first_seen_time` | 首次发现时间。第一次从领星拉到这个 SKU 的时间。 |
| `last_seen_time` | 最近发现时间。最近一次从领星同步时还能看到这个 SKU 的时间。 |
| `update_time` | 更新时间。候选快照最后一次被更新的时间。 |

## `upstream_system_sku_pairing`

用途：保存系统 SKU 和领星 masterSku 的正式配对关系。规则是一对一。

| 字段 | 中文注释 |
| --- | --- |
| `sku_pairing_id` | 配对记录主键。数据库内部使用的自增 ID。 |
| `connection_code` | 主仓接入编号。表示这条 SKU 配对属于哪个领星主仓接入。 |
| `master_sku` | 领星 masterSku。被配对的上游 SKU。 |
| `system_sku` | 系统 SKU。我们自己系统里的 SKU 编码。 |
| `system_sku_name` | 系统 SKU 名称快照。配对时记录一份名称，方便展示和历史追踪。 |
| `customer_name` | 客户名称快照。如果系统 SKU 有所属客户，这里保留展示用名称。 |
| `create_by` | 创建人。记录是谁建立了这个 SKU 配对。 |
| `create_time` | 创建时间。记录 SKU 配对建立时间。 |
| `update_by` | 最近更新人。记录最后是谁修改了这条 SKU 配对。 |
| `update_time` | 最近更新时间。记录最后一次修改时间。 |

约束说明：

- `connection_code + master_sku` 唯一：同一主仓接入下，一个领星 masterSku 只能配一个系统 SKU。
- `connection_code + system_sku` 唯一：同一主仓接入下，一个系统 SKU 只能配一个领星 masterSku。

## `upstream_system_sku_sync_state`

用途：保存每个主仓接入的 SKU 同步状态。它让页面能看到“正在同步、同步成功、同步失败、是否过期”。

| 字段 | 中文注释 |
| --- | --- |
| `connection_code` | 主仓接入编号。每个主仓接入只有一条 SKU 同步状态。 |
| `status` | 同步状态。例如从未同步、同步中、已同步、已过期、同步失败。 |
| `sync_batch_id` | 当前或最近一次同步批次号。用于关联这次同步产生的候选数据。 |
| `last_started_time` | 最近一次开始同步时间。用于判断同步是否卡住。 |
| `last_finished_time` | 最近一次结束同步时间。成功或失败都会记录结束时间。 |
| `last_success_time` | 最近一次同步成功时间。用于判断 10 分钟窗口是否新鲜。 |
| `last_error_message` | 最近一次失败原因。页面展示给管理员排查问题。 |
| `next_sync_time` | 下一次计划同步时间。如果启用 Quartz 定时同步，用它展示预计同步时间。 |
| `update_time` | 更新时间。同步状态最后一次被更新的时间。 |

## `upstream_system_request_log`

用途：保存调用领星接口的请求日志。它只追加，不物理删除，用来追踪授权、同步和失败原因。

| 字段 | 中文注释 |
| --- | --- |
| `request_log_id` | 请求日志主键。数据库内部使用的自增 ID。 |
| `connection_code` | 主仓接入编号。表示这条请求属于哪个主仓接入。 |
| `trace_id` | 请求追踪号。一次业务操作可能产生多个外部请求，用它串起来排查。 |
| `operation` | 业务操作名称。例如授权校验、同步仓库、同步物流渠道、同步 SKU。 |
| `endpoint` | 领星接口地址。记录调用的是哪个接口，不包含密钥。 |
| `request_time` | 请求发起时间。 |
| `response_time` | 响应返回时间。 |
| `duration_ms` | 请求耗时，单位毫秒。用于判断接口是否慢。 |
| `request_payload_redacted` | 脱敏后的请求内容。敏感字段必须打码。 |
| `response_payload_redacted` | 脱敏后的响应内容。敏感字段必须打码。 |
| `external_error_code` | 领星返回的错误码。调用失败时用于定位问题。 |
| `external_error_message` | 领星返回的错误信息。调用失败时用于展示和排查。 |
| `status` | 请求结果状态。例如成功、失败、超时、重试后成功。 |
| `create_time` | 日志创建时间。 |

## `upstream_system_sku_pairing_audit_event`

用途：保存 SKU 配对变更历史。它记录“谁在什么时候把什么 SKU 配给了什么 masterSku，或者解除了配对”。

| 字段 | 中文注释 |
| --- | --- |
| `audit_event_id` | 审计事件主键。数据库内部使用的自增 ID。 |
| `connection_code` | 主仓接入编号。表示这次 SKU 配对变更属于哪个主仓接入。 |
| `master_sku` | 领星 masterSku。变更涉及的上游 SKU。 |
| `system_sku` | 系统 SKU。变更涉及的系统 SKU。 |
| `event_type` | 事件类型。例如配对、解除配对、重新配对。 |
| `operator` | 操作人。记录谁做了这次变更。 |
| `event_time` | 操作时间。记录变更发生的时间。 |
| `before_snapshot` | 变更前内容。JSON 格式，记录修改前的配对状态。 |
| `after_snapshot` | 变更后内容。JSON 格式，记录修改后的配对状态。 |
| `remark` | 备注。记录变更原因或人工说明。 |

## 需要确认的一个点

仓库配对和物流渠道配对目前设计为：

- 仓库：系统仓库全局只能配一次。
- 物流渠道：系统物流渠道全局只能配一次。

如果以后系统仓库或系统渠道是按租户、客户、卖家隔离的，那么唯一约束要改成“隔离范围 + 系统编码”，例如 `tenant_id + system_warehouse_code`。当前若依工程还没有这类业务隔离字段，所以第一版先按全局唯一设计。
