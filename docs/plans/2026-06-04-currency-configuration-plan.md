# 币种配置与汇率同步实施确认方案

日期：2026-06-04

本方案是实施前确认文档，不是已落地实现。确认前不新增业务表、SQL、后端接口、菜单权限或前端页面。

## 需求理解

本轮目标是新增管理端“币种配置”菜单，用于维护平台可用币种和汇率。

当前需求边界：

- 先建立币种字典，作为平台币种全集，建议使用 ISO 4217 代码，例如 `USD`、`CNY`、`EUR`。
- “币种配置”里新增币种时，从币种字典选择，而不是自由输入。
- 后续业务页面的币种下拉不直接读取币种字典，而读取“币种配置”里启用的可用币种。
- 外部同步得到的汇率定义为官方汇率。
- 官方汇率不直接给业务使用。
- 平台需要维护一个实际使用的汇率，推荐字段名为 `effective_rate`，中文名“生效汇率”。
- `effective_rate` 后续由官方汇率按调整规则计算；调整规则本轮先预留结构，具体规则等后续确认。

字段命名建议：

| 中文含义 | 推荐字段 | 说明 |
| --- | --- | --- |
| 官方汇率 | `official_rate` | 外部 API 同步回来的原始官方汇率。 |
| 生效汇率 | `effective_rate` | 平台当前实际使用汇率，业务取值默认读取它。 |
| 汇率基准币种 | `base_currency_code` | 汇率以哪个币种为基准，例如 `USD`。 |
| 目标币种 | `currency_code` | 当前配置币种，例如 `CNY`。 |
| 官方汇率时间 | `official_rate_time` | 外部 API 返回或本次同步确认的官方汇率时间。 |
| 生效汇率更新时间 | `effective_rate_time` | `effective_rate` 产生或人工调整的时间。 |

补充说明：如果后续订单、账单或结算流水需要保存当时使用的汇率快照，流水表里的字段可命名为 `applied_rate`。当前币种配置表中建议使用 `effective_rate`，表达“当前生效的配置值”。

## 当前项目事实

- 当前后端已有 `seller`、`buyer`、`integration` 模块，还没有 `finance` Maven 模块。
- 顶级菜单已有“财务管理”，但财务二级业务仍需重新梳理。
- `docs/architecture/reuse-ledger.md` 已登记若依字典、ProTable 筛选复用、外部系统接入、凭证加密等规则。
- `integration` 模块已有外部系统接入、凭证加密、脱敏日志和同步请求日志思路，可复用设计，不应在币种同步里重新散写明文凭证或 HTTP 调用逻辑。
- 当前卖家/买家列表里已有余额币种占位，Mapper 中默认返回 `USD`，后续应改为读取可用币种配置或业务余额模型，不应继续硬编码。

## 模块归属

推荐归属：财务模块 `finance`。

原因：

- 汇率会影响余额、充值、账单、结算、财务流水、订单金额折算。
- 金额和汇率都属于财务底线范围，必须使用 `BigDecimal` / `decimal`，不能使用 `float` / `double`。
- 币种可用性属于平台公共配置，但实际业务消费主要在财务与交易相关模块。

推荐新增后端模块：

```text
RuoYi-Vue/finance
  src/main/java/com/ruoyi/finance
  src/main/resources/mapper/finance
```

`ruoyi-admin` 引入 `finance` 依赖，接口路径使用：

```text
/finance/admin/currencies/**
```

权限标识使用：

```text
finance:currency:list
finance:currency:query
finance:currency:add
finance:currency:edit
finance:currency:remove
finance:currency:syncConfig
finance:currency:sync
finance:currency:log
```

## 字典设计

新增若依字典：

| 项 | 建议值 |
| --- | --- |
| 字典名称 | 币种 |
| 字典类型 | `currency_code` |
| 字典键值 | ISO 4217 代码，例如 `USD`、`CNY`、`EUR` |
| 字典标签 | 中文名 / English Name / Code，例如 `美元 / US Dollar (USD)` |
| 默认值 | 建议 `USD`，待确认 |

规则：

- `sys_dict_data.dict_value` 保存币种 code。
- 前端展示 label，接口和业务表保存 code。
- 币种字典只表示“系统认识哪些币种”，不表示业务可用。
- 后续业务页面币种选项必须读取 `finance_currency` 中 `status = 0` 的可用币种，不直接读取 `currency_code` 字典。
- 新增币种配置时，必须校验 `currency_code` 存在于 `sys_dict_data`，且 `dict_type = currency_code`。

## 数据表设计

### 1. `finance_currency`

业务目的：维护平台可用币种、当前官方汇率、当前生效汇率和基础展示信息。

承载规则：

- 一行代表一个平台可用币种。
- 控制后续业务可选币种范围。
- 保存当前最新官方汇率和当前最新生效汇率。
- 不保存外部 API 凭证。
- 不保存每次同步历史；历史进入 `finance_currency_rate_history`。
- 不保存订单、账单、余额、流水的业务事实。

建议字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `currency_id` | bigint | 是 | 主键。 |
| `currency_code` | varchar(16) | 是 | 币种 code，来自 `currency_code` 字典。 |
| `currency_name` | varchar(100) | 是 | 币种展示名，可从字典带入后允许维护。 |
| `currency_symbol` | varchar(16) | 否 | 符号，例如 `$`、`¥`。 |
| `base_currency_code` | varchar(16) | 是 | 汇率基准币种，默认建议 `USD`。 |
| `official_rate` | decimal(24,10) | 否 | 官方汇率。 |
| `effective_rate` | decimal(24,10) | 否 | 生效汇率，平台实际使用值。 |
| `rate_precision` | int | 是 | 汇率小数精度，默认 6 或 8，待确认。 |
| `amount_precision` | int | 是 | 金额小数精度，默认 2。 |
| `rounding_mode` | varchar(32) | 是 | 舍入方式，例如 `HALF_UP`。 |
| `adjustment_mode` | varchar(32) | 是 | 调整方式，第一版可用 `NONE` / `MANUAL` 占位。 |
| `adjustment_value` | decimal(24,10) | 否 | 调整值，规则确认后使用。 |
| `official_rate_time` | datetime | 否 | 官方汇率时间。 |
| `effective_rate_time` | datetime | 否 | 生效汇率更新时间。 |
| `is_default` | char(1) | 是 | 是否默认币种，`Y` / `N`，最多一个默认。 |
| `status` | char(1) | 是 | 状态：`0` 正常，`1` 停用。 |
| `create_by` | varchar(64) | 否 | 创建者。 |
| `create_time` | datetime | 否 | 创建时间。 |
| `update_by` | varchar(64) | 否 | 更新者。 |
| `update_time` | datetime | 否 | 更新时间。 |
| `remark` | varchar(500) | 否 | 备注。 |

约束与索引：

- `uk_finance_currency_code(currency_code)`。
- `idx_finance_currency_status(status)`。
- `idx_finance_currency_base(base_currency_code)`。
- 默认币种唯一性建议通过 Service 校验；MySQL 也可用生成列或触发器实现，但第一版不建议复杂化。

### 2. `finance_currency_rate_history`

业务目的：追加记录每次官方汇率同步、人工调整或生效汇率变化。

承载规则：

- 只追加，不覆盖。
- 用于审计：谁、何时、因为什么规则改变了汇率。
- 不作为业务当前值读取来源；当前值仍以 `finance_currency` 为准。

建议字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `rate_history_id` | bigint | 是 | 主键。 |
| `currency_code` | varchar(16) | 是 | 币种 code。 |
| `base_currency_code` | varchar(16) | 是 | 基准币种。 |
| `official_rate` | decimal(24,10) | 否 | 本次官方汇率。 |
| `effective_rate` | decimal(24,10) | 否 | 本次生效汇率。 |
| `adjustment_mode` | varchar(32) | 是 | 调整方式。 |
| `adjustment_value` | decimal(24,10) | 否 | 调整值。 |
| `source_type` | varchar(32) | 是 | `SYNC` / `MANUAL`。 |
| `source_config_id` | bigint | 否 | 同步配置 ID。 |
| `official_rate_time` | datetime | 否 | 官方汇率时间。 |
| `effective_rate_time` | datetime | 是 | 生效时间。 |
| `change_reason` | varchar(500) | 否 | 调整原因。 |
| `create_by` | varchar(64) | 否 | 操作人或系统。 |
| `create_time` | datetime | 是 | 记录时间。 |

索引：

- `idx_currency_rate_history_currency_time(currency_code, create_time)`。
- `idx_currency_rate_history_source(source_type, source_config_id)`。

### 3. `finance_currency_sync_config`

业务目的：维护汇率同步 API、凭证配置和同步计划。

承载规则：

- 保存外部 API 连接信息和同步时间。
- API 密钥、token、secret 必须加密保存，前端和日志只能展示脱敏值。
- 不直接保存每次同步结果；同步结果进入 `finance_currency_rate_history`，请求过程进入 `finance_currency_sync_log`。

建议字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sync_config_id` | bigint | 是 | 主键。 |
| `provider_code` | varchar(64) | 是 | 服务商代码，例如 `OPEN_EXCHANGE`，具体待确认。 |
| `provider_name` | varchar(100) | 是 | 服务商名称。 |
| `base_currency_code` | varchar(16) | 是 | 基准币种。 |
| `api_base_url` | varchar(500) | 是 | API 地址，不含敏感 query。 |
| `auth_type` | varchar(32) | 是 | `API_KEY` / `BEARER` / `NONE`。 |
| `credential_ciphertext` | varchar(2000) | 否 | 加密凭证。 |
| `credential_key_id` | varchar(64) | 否 | 加密密钥标识。 |
| `credential_masked` | varchar(200) | 否 | 脱敏展示值。 |
| `request_timeout_ms` | int | 是 | 请求超时。 |
| `retry_count` | int | 是 | 重试次数。 |
| `schedule_type` | varchar(32) | 是 | `CRON` / `MANUAL_ONLY`。 |
| `cron_expression` | varchar(100) | 否 | 同步 cron。 |
| `sync_enabled` | char(1) | 是 | 是否启用自动同步。 |
| `last_sync_time` | datetime | 否 | 最近同步时间。 |
| `last_sync_status` | varchar(32) | 否 | 最近同步状态。 |
| `status` | char(1) | 是 | 配置状态：`0` 正常，`1` 停用。 |
| `create_by` | varchar(64) | 否 | 创建者。 |
| `create_time` | datetime | 否 | 创建时间。 |
| `update_by` | varchar(64) | 否 | 更新者。 |
| `update_time` | datetime | 否 | 更新时间。 |
| `remark` | varchar(500) | 否 | 备注。 |

约束与索引：

- `uk_currency_sync_provider(provider_code)`。
- `idx_currency_sync_status(sync_enabled, status)`。

### 4. `finance_currency_sync_log`

业务目的：追加记录每次外部汇率同步请求。

承载规则：

- 只追加，不覆盖。
- 记录 traceId、请求时间、响应时间、状态、错误码、错误信息。
- 不记录明文密钥。
- 响应摘要只保存必要字段，避免保存超大原始报文。

建议字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sync_log_id` | bigint | 是 | 主键。 |
| `trace_id` | varchar(64) | 是 | 请求链路 ID。 |
| `sync_config_id` | bigint | 是 | 同步配置 ID。 |
| `provider_code` | varchar(64) | 是 | 服务商代码。 |
| `request_url` | varchar(500) | 否 | 脱敏后的请求地址。 |
| `request_time` | datetime | 是 | 请求时间。 |
| `response_time` | datetime | 否 | 响应时间。 |
| `cost_ms` | bigint | 否 | 耗时。 |
| `status` | varchar(32) | 是 | `SUCCESS` / `FAILED` / `PARTIAL`。 |
| `error_code` | varchar(100) | 否 | 外部或内部错误码。 |
| `error_message` | varchar(1000) | 否 | 错误信息，需脱敏。 |
| `currency_count` | int | 否 | 本次返回币种数。 |
| `updated_count` | int | 否 | 本次更新币种数。 |
| `response_summary` | varchar(2000) | 否 | 脱敏响应摘要。 |
| `create_time` | datetime | 是 | 记录时间。 |

索引：

- `idx_currency_sync_log_config_time(sync_config_id, request_time)`。
- `idx_currency_sync_log_trace(trace_id)`。
- `idx_currency_sync_log_status(status, request_time)`。

## API 设计

### 币种配置

```text
GET    /finance/admin/currencies/list
GET    /finance/admin/currencies/{currencyCode}
POST   /finance/admin/currencies
PUT    /finance/admin/currencies/{currencyCode}
PUT    /finance/admin/currencies/{currencyCode}/status
DELETE /finance/admin/currencies/{currencyCode}
GET    /finance/admin/currencies/options
```

关键规则：

- `options` 只返回启用的 `finance_currency`，供后续业务币种下拉复用。
- 新增时校验 `currency_code` 必须存在于 `currency_code` 字典。
- 停用币种前，后续如果已有订单、账单、流水引用，不能物理删除，只能停用。
- 第一版可以禁止删除已产生汇率历史的币种。

### 汇率同步设置

```text
GET  /finance/admin/currency-sync-config
PUT  /finance/admin/currency-sync-config
POST /finance/admin/currency-sync-config/test
POST /finance/admin/currency-sync-config/sync
GET  /finance/admin/currency-sync-config/logs/list
```

关键规则：

- 保存凭证时必须加密，响应只返回 `credential_masked`。
- 测试连接不更新 `finance_currency`，只写同步日志。
- 手动同步会拉取官方汇率，写历史，并按确认后的调整规则更新 `effective_rate`。
- 自动同步后续通过 Quartz 或已有调度能力触发；第一版可先做手动同步和配置保存，再接入定时。

## 前端页面设计

菜单位置建议：

```text
财务管理
  币种配置
```

页面建议分为两个 Tab：

### Tab 1：币种列表

列表字段：

- 币种代码
- 币种名称
- 符号
- 基准币种
- 官方汇率
- 生效汇率
- 调整方式
- 官方汇率时间
- 生效汇率更新时间
- 默认币种
- 状态
- 更新时间

筛选字段：

- 币种代码 / 名称关键词
- 状态
- 是否默认
- 汇率更新时间范围

操作：

- 新增币种
- 编辑基础信息
- 启用 / 停用
- 设为默认
- 查看汇率历史

新增/编辑弹窗字段：

- 币种代码：从 `currency_code` 字典选择。
- 币种名称：默认从字典带入，可编辑。
- 币种符号。
- 基准币种。
- 金额精度。
- 汇率精度。
- 舍入方式。
- 调整方式：第一版先 `NONE` / `MANUAL`。
- 生效汇率：允许财务管理员手动维护。
- 状态。
- 备注。

前端复用要求：

- ProTable 筛选使用 `getPersistedProTableSearch({ labelWidth: ... })`。
- 状态、调整方式、舍入方式等 option 集中维护，不在页面内散写。
- 启用币种选项封装到 `react-ui/src/services/finance/currency.ts`，后续业务页面统一调用。

### Tab 2：同步设置

基础信息：

- API 服务商
- API 地址
- 认证方式
- API Key / Token，保存后只展示脱敏值
- 基准币种
- 请求超时
- 重试次数
- 同步方式：手动 / 定时
- cron 表达式或同步时间
- 最近同步时间
- 最近同步状态

操作：

- 保存设置
- 测试连接
- 立即同步
- 查看同步日志

同步日志列表字段：

- traceId
- 服务商
- 请求时间
- 响应时间
- 耗时
- 同步状态
- 返回币种数
- 更新币种数
- 错误码
- 错误信息

## 同步与汇率计算规则

第一版推荐流程：

1. 管理员维护币种字典。
2. 管理员在币种配置中新增可用币种。
3. 管理员维护同步 API 设置。
4. 手动或定时同步外部官方汇率。
5. 系统更新 `official_rate`。
6. 系统按调整规则计算 `effective_rate`。
7. 写入 `finance_currency_rate_history`。
8. 后续业务只读取 `effective_rate`，不直接读取 `official_rate`。

调整规则待确认前，建议第一版先支持：

- `NONE`：`effective_rate = official_rate`，但仍明确这是“生效汇率”，不是业务直接读官方汇率。
- `MANUAL`：管理员手动填写 `effective_rate`，同步只更新 `official_rate`，不覆盖 `effective_rate`。

后续可扩展：

- `PERCENT_UP`：官方汇率上浮百分比。
- `PERCENT_DOWN`：官方汇率下调百分比。
- `FIXED_DELTA`：固定加减值。
- `ROUNDING`：按币种精度和舍入方式处理。

## 权限与审计

后端接口必须加 `@PreAuthorize`。

新增/编辑/停用/同步/保存凭证必须加 `@Log`：

- 币种新增：`BusinessType.INSERT`
- 币种修改：`BusinessType.UPDATE`
- 币种停用：`BusinessType.UPDATE`
- 同步配置保存：`BusinessType.UPDATE`
- 测试连接：`BusinessType.OTHER`
- 立即同步：`BusinessType.OTHER`

凭证规则：

- 明文凭证不能写入 SQL、Markdown、日志、前端响应。
- 保存凭证需要复用或上移现有 `SecretCipherSupport`，避免在 `finance` 中复制加密逻辑。
- 缺少加密主密钥时，不能保存凭证或发起需要凭证的同步。

## 初始化数据建议

币种字典第一版建议初始化：

```text
USD 美元 / US Dollar (USD)
CNY 人民币 / Chinese Yuan (CNY)
EUR 欧元 / Euro (EUR)
GBP 英镑 / Pound Sterling (GBP)
CAD 加拿大元 / Canadian Dollar (CAD)
AUD 澳大利亚元 / Australian Dollar (AUD)
JPY 日元 / Japanese Yen (JPY)
HKD 港币 / Hong Kong Dollar (HKD)
MXN 墨西哥比索 / Mexican Peso (MXN)
BRL 巴西雷亚尔 / Brazilian Real (BRL)
```

可用币种第一版建议只启用：

```text
USD
CNY
EUR
```

默认币种建议：

```text
USD
```

以上初始化值需要确认后再写 SQL。

## 实施顺序

### 阶段 1：确认方案

确认内容：

- 默认基准币种是否为 `USD`。
- 第一版可用币种是否为 `USD`、`CNY`、`EUR`。
- 汇率精度、金额精度、舍入方式。
- 同步 API 服务商和认证方式。
- 第一版是否只做手动同步，还是同时接入定时同步。
- 调整规则第一版采用 `NONE` + `MANUAL`，还是需要立即支持上浮/下调。

### 阶段 2：数据库与菜单 SQL

动作：

- 新增 `currency_code` 字典初始化 SQL。
- 新增 `finance_currency`、`finance_currency_rate_history`、`finance_currency_sync_config`、`finance_currency_sync_log`。
- 新增“财务管理 / 币种配置”菜单和按钮权限。
- 写数据库执行记录，执行前确认目标数据源。

验收：

- 字典、表、菜单、权限都可重复执行。
- 不产生重复菜单、重复字典、重复币种。
- 执行记录写清楚目标环境、连接来源和是否影响远端数据。

### 阶段 3：后端实现

动作：

- 新增 `finance` Maven 模块。
- 接入 `ruoyi-admin`。
- 实现 Domain、Mapper、Service、Controller。
- 实现可用币种 options 接口。
- 实现同步配置保存、测试连接、手动同步、同步日志查询。
- 复用或上移凭证加密支持。

验收：

- 后端编译通过。
- 有权限用户可以访问接口。
- 无权限用户被后端拒绝。
- 密钥响应脱敏，日志不出现明文凭证。

### 阶段 4：前端实现

动作：

- 新增 `react-ui/src/pages/Finance/Currency/index.tsx`。
- 新增 `react-ui/src/services/finance/currency.ts`。
- 新增 `react-ui/src/types/finance/currency.d.ts`。
- 页面使用 ProTable + Tab 结构。
- 后续业务币种下拉统一调用可用币种 options。

验收：

- 菜单可打开。
- 币种列表、筛选、新增、编辑、启停可用。
- 同步设置保存、测试连接、立即同步、日志查看可用。
- 页面不内联大段 option。

### 阶段 5：验证与记录

验证命令建议：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests install
```

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run build
```

浏览器验证：

- 使用 `admin / admin123` 登录管理端。
- 查看“财务管理 / 币种配置”菜单。
- 新增币种、编辑生效汇率、启停币种。
- 保存同步配置，确认凭证脱敏展示。
- 测试连接和立即同步后查看同步日志。

记录：

- 写数据库执行记录。
- 写阶段总结。
- 更新 `docs/architecture/reuse-ledger.md`：
  - `currency_code` 字典。
  - 可用币种 options 接口。
  - 汇率同步配置与凭证加密复用。
  - 前端 finance currency service/type/page 复用规则。

## 待确认问题

1. 默认基准币种是否按 `USD`？
2. 第一版可用币种是否先启用 `USD`、`CNY`、`EUR`？
3. 汇率精度默认用 6 位还是 8 位？推荐 8 位。
4. 金额精度默认是否用 2 位？推荐 2 位。
5. 第一版调整规则是否先做 `NONE` + `MANUAL`，上浮/下调规则后续再补？
6. 同步 API 服务商是哪一个？认证方式是 API Key、Bearer Token，还是无认证？
7. 第一版是否先做“手动同步 + 同步设置”，定时同步等服务商确认后再接 Quartz？
8. 币种停用后，后续已被业务引用时是否只允许停用、不允许删除？推荐是。

## 风险

- 汇率会影响财务、订单、余额和结算，不能用简单 CRUD 直接替代业务规则。
- 官方汇率和生效汇率必须分开，否则后续调整规则无法审计。
- 外部 API 凭证必须加密保存并脱敏展示。
- 同步日志和汇率历史必须只追加，不能覆盖历史记录。
- 后续业务引用币种时必须读取可用币种配置，不能回到直接读字典或页面硬编码。
