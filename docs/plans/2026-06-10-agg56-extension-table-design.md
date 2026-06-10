# AGG56 接入方扩展表设计草案

日期：2026-06-10

## 设计目标

本文件只设计 AGG56 接入方私有字段。AGG56 字段不得进入物流商管理通用核心表。

通用核心表见：

```text
docs/plans/2026-06-10-logistics-carrier-table-design.md
```

AGG56 扩展表只在 `provider_kind = AGG56` 的连接上使用。其他物流商不得复用本表保存自己的专属字段。

## 边界

### AGG56 扩展表保存什么

- AGG56 的 `app_token` / `app_key` 密文和脱敏值。
- AGG56 授权返回的用户信息快照。
- AGG56 短期 `access_token` 的缓存元信息。
- 后续如果完整发件地址跑不通，再保存 AGG56 发货地址编码候选。

### AGG56 扩展表不保存什么

- 不保存通用连接名称、base URL、启停状态，这些属于 `logistics_carrier_connection`。
- 不保存物流商渠道、系统渠道、面单订单和请求日志，这些属于通用核心表。
- 不保存明文凭据、明文 access token 或完整 Authorization header。

## 表设计总览

| 表名 | 目的 | 第一版是否需要 |
| --- | --- | --- |
| `logistics_agg56_connection` | AGG56 凭据密文和授权用户快照 | 是 |
| `logistics_agg56_token_cache` | AGG56 短期 access token 缓存元信息 | 可选，优先 Redis |
| `logistics_agg56_shipper_address_candidate` | AGG56 发货地址编码候选 | 暂缓，完整地址跑不通再补 |

## `logistics_agg56_connection`

业务目的：保存 AGG56 接入所需的私有凭据和授权用户快照。

业务逻辑：

- 与通用 `logistics_carrier_connection.connection_code` 一对一。
- 凭据必须先调用 AGG56 授权接口校验成功，再加密保存。
- 授权失败不得覆盖旧凭据。
- 明文凭据不得写入 SQL、日志或文档。

字段草案：

| 字段 | 类型建议 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `connection_code` | `varchar(64)` | 是 | 无 | 主键 | 物流商接入编号，引用通用连接 |
| `app_token_mask` | `varchar(64)` | 是 | `''` |  | AGG56 `app_token` 脱敏值 |
| `app_key_mask` | `varchar(64)` | 是 | `''` |  | AGG56 `app_key` 脱敏值 |
| `app_token_ciphertext` | `text` | 是 | 无 |  | AGG56 `app_token` 密文 |
| `app_key_ciphertext` | `text` | 是 | 无 |  | AGG56 `app_key` 密文 |
| `credential_key_id` | `varchar(64)` | 是 | `default` |  | 加密密钥版本 |
| `agg56_user_id` | `varchar(64)` | 否 | `''` |  | AGG56 授权返回用户 ID |
| `agg56_user_account_mask` | `varchar(128)` | 否 | `''` |  | AGG56 授权返回账号脱敏值 |
| `agg56_customer_code` | `varchar(64)` | 否 | `''` | 普通索引 | AGG56 客户代码 |
| `create_time` | `datetime` | 否 | `null` |  | 创建时间 |
| `update_time` | `datetime` | 否 | `null` |  | 更新时间 |

说明：

- 表名和字段名明确带 `agg56`，避免误认为是通用字段。
- 通用页面展示时可通过 provider adapter 读取脱敏字段。

## `logistics_agg56_token_cache`

业务目的：保存 AGG56 短期 access token 的缓存元信息。

建议：

- 第一选择是 Redis 缓存，不落数据库。
- 如果需要多实例共享且 Redis 缓存不足，再考虑本表。

字段草案：

| 字段 | 类型建议 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `connection_code` | `varchar(64)` | 是 | 无 | 主键 | 物流商接入编号 |
| `access_token_ciphertext` | `text` | 是 | 无 |  | AGG56 短期 token 密文 |
| `token_key_id` | `varchar(64)` | 是 | `default` |  | 加密密钥版本 |
| `issued_time` | `datetime` | 否 | `null` |  | 获取时间 |
| `expire_time` | `datetime` | 否 | `null` | 普通索引 | 过期时间 |
| `update_time` | `datetime` | 否 | `null` |  | 更新时间 |

说明：

- 不保存明文 `access_token`。
- 当前 AGG56 文档建议 24 小时重新获取一次。

## `logistics_agg56_shipper_address_candidate`

业务目的：保存 AGG56 发货地址编码候选。

当前口径：

- 第一版创建面单优先使用外部调用方传入的完整发件地址。
- 第一版不依赖 AGG56 `shipper_code`。
- 如果真实联调证明完整地址跑不通，或 AGG56 价格分区必须依赖备案地址编码，再启用本表。

当前状态：暂缓建表。

字段草案：

| 字段 | 类型建议 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `connection_code` | `varchar(64)` | 是 | 无 | 联合主键 | 物流商接入编号 |
| `shipper_code` | `varchar(64)` | 是 | 无 | 联合主键 | AGG56 发货地址编码 |
| `shipper_name` | `varchar(200)` | 否 | `''` |  | 发件人 |
| `shipper_company` | `varchar(200)` | 否 | `''` |  | 公司 |
| `shipper_telphone_mask` | `varchar(64)` | 否 | `''` |  | 电话脱敏 |
| `shipper_country` | `varchar(32)` | 否 | `''` |  | 国家 |
| `shipper_postcode` | `varchar(32)` | 否 | `''` |  | 邮编 |
| `shipper_state` | `varchar(64)` | 否 | `''` |  | 州 |
| `shipper_city` | `varchar(128)` | 否 | `''` |  | 城市 |
| `shipper_street_address1` | `varchar(255)` | 否 | `''` |  | 地址 1 |
| `is_default` | `tinyint` | 是 | `0` | 普通索引 | 是否默认 |
| `status` | `varchar(16)` | 是 | `ACTIVE` | 普通索引 | 状态 |
| `sync_batch_id` | `varchar(64)` | 是 | 无 | 普通索引 | 同步批次 |
| `first_seen_time` | `datetime` | 是 | 无 |  | 首次发现 |
| `last_seen_time` | `datetime` | 是 | 无 |  | 最近发现 |

## 与通用表的关系

| 通用表 | AGG56 扩展关系 |
| --- | --- |
| `logistics_carrier_connection` | `logistics_agg56_connection.connection_code` 一对一 |
| `logistics_carrier_channel_candidate` | AGG56 适配器把 `sm_code` / `sm_name` 映射为通用外部渠道 code/name |
| `logistics_label_order` | AGG56 适配器把 `order_code`、`reference_no`、状态、错误信息映射为通用字段 |
| `logistics_label_package` | AGG56 适配器把 `tracking_number`、`label_url`、`file_type` 映射为通用字段 |
| `logistics_carrier_request_log` | AGG56 请求响应进入通用脱敏日志 |

## 待确认

1. 是否同意 AGG56 凭据独立放 `logistics_agg56_connection`，不进入通用连接表。
2. 是否同意短期 access token 第一版优先走 Redis，不建 `logistics_agg56_token_cache`。
3. 是否同意 AGG56 发货地址编码表继续暂缓。
