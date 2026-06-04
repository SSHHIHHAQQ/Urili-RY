# 上游系统管理菜单迁移计划

日期：2026-06-04

## 结论

本计划的迁移方向是：从旧工程 `E:\Urili` 的上游系统管理功能，迁移到当前若依验证工程 `E:\Urili-Ruoyi`。

推荐把“上游系统管理”作为当前顶级菜单 `海外仓服务设置` 下的二级页面落地，路径建议为：

- 菜单目录：`海外仓服务设置`
- 页面菜单：`上游系统管理`
- 前端组件：`react-ui/src/pages/Integration/UpstreamSystem/index.tsx`
- 后端模块：`RuoYi-Vue/integration`
- Java 包：`com.ruoyi.integration`
- 接口前缀：`/integration/admin/upstream-systems`
- 权限前缀：`integration:upstream:*`

本计划是实施前确认文档，不是已落地实现。未执行数据库 DDL/DML，未连接远端 MySQL/Redis，未修改功能代码。

## 本次查到的旧工程功能

旧工程功能入口和证据：

- 管理端页面：`E:\Urili\apps\admin-web\src\features\upstream-systems\UpstreamSystemManagementPage.tsx`
- SKU 配对组件：`E:\Urili\apps\admin-web\src\features\upstream-systems\components\SkuPairingPanel.tsx`
- 管理端 API 编排：`E:\Urili\apps\api\src\admin-upstream-systems.routes.ts`
- 管理端业务服务：`E:\Urili\apps\api\src\admin-upstream-systems.service.ts`
- 上游系统模块：`E:\Urili\packages\modules\upstream-systems`
- 领星适配器：`E:\Urili\packages\integrations\lingxing`
- 旧工程交付记录：
  - `E:\Urili\docs\status\2026-06-02-lingxing-wms-upstream-connection.md`
  - `E:\Urili\docs\status\2026-06-02-lingxing-sku-pairing-sync.md`
  - `E:\Urili\docs\status\2026-06-03-upstream-full-sync-silent-run.md`
  - `E:\Urili\docs\status\2026-06-03-upstream-sidebar-order-adjustment.md`
  - `E:\Urili\docs\status\2026-06-03-upstream-sku-sync-hardening.md`
  - `E:\Urili\docs\adr\0003-lingxing-wms-upstream-master-warehouse-integration.md`

已确认旧工程具备的功能：

| 功能 | 当前旧工程状态 | 迁移判断 |
| --- | --- | --- |
| 主仓接入列表 | 左侧按上游系统分组，显示多个主仓接入 | 迁移 |
| 搜索主仓/系统 | 左侧搜索框过滤主仓和系统名 | 迁移 |
| 新增主仓 | 支持领星 WMS，填写主仓名称、结算类型、appKey、appSecret | 迁移 |
| 授权校验 | 调用领星工单类型接口校验连通性 | 迁移 |
| 重新授权 | 更新 appKey/appSecret，重新拉取候选 | 迁移 |
| 编辑信息 | 修改主仓名称、结算类型，不更新凭证 | 迁移 |
| 同步 | 静默全量同步仓库、物流渠道、SKU 候选 | 迁移 |
| 排序 | `displayOrder` 持久化，编辑/同步/重授权不打乱顺序 | 迁移 |
| 仓库映射页签 | 展示上游仓库候选，配对按钮仍是占位 | 迁移候选展示，不迁移未实现配对 |
| 物流渠道页签 | 合并相同渠道代码，展示覆盖仓库代码，配对按钮仍是占位 | 迁移候选展示，不迁移未实现配对 |
| SKU 配对页签 | 服务端分页/搜索/筛选、同步 SKU、手动配对、解除配对 | 迁移 |
| 箱配对 | 旧页面只有空表入口 | 保留页签占位，明确未实现 |
| 增值服务配对 | 旧页面只有空表入口 | 保留页签占位，明确未实现 |
| 删除主仓 | 旧页面只是 `onAction` 占位，没有真实删除 API | 不迁移物理删除，先做暂停/启用或保留后续项 |
| 请求日志 | 领星请求日志脱敏后追加记录 | 迁移 |
| 定时同步 | SKU 候选 10 分钟同步一次 | 迁移到若依 Quartz 或先落手动同步后再开任务 |

## 核心不变量

迁移时必须保留这些规则：

- 领星只作为上游系统适配器，不直接成为商品、库存、订单、履约、财务事实源。
- 页面不能直接调用领星，只能调用若依后端 API。
- 商品、库存、订单、履约、财务模块不能直接调用领星 client。
- appKey、appSecret 不得明文落库，不得返回前端，不得写日志；前端只展示脱敏值。
- 外部请求日志只追加，不物理删除。
- 仓库、物流渠道、SKU 都只是上游候选或映射状态，不直接写平台业务事实。
- SKU 配对必须一对一：同一主仓接入内，一个系统 SKU 只能配一个上游 masterSku，一个上游 masterSku 只能配一个系统 SKU。
- 下游要向上游发送 SKU 前，必须先通过上游系统模块解析 masterSku；未配对或上游缺失必须阻断。
- 主仓排序由 `display_order` 控制；编辑信息、重新授权、同步不能改变排序。
- 编辑信息、重新授权、同步必须做局部字段更新，不能用旧快照整行覆盖，避免结算类型被并发覆盖。

## 一次性实施范围

本轮建议一次性落地这些内容：

1. 后端 `integration` Maven 子模块。
2. 领星 WMS Java 适配器：签名、请求、重试、超时、错误映射、脱敏日志。
3. 上游系统主仓接入、授权、重新授权、编辑信息、同步、排序。
4. 上游仓库候选展示。
5. 上游物流渠道候选展示，并按渠道代码合并多个仓库代码。
6. 系统仓库与领星主仓仓库一对一配对。
7. 系统物流渠道与领星主仓物流渠道配对：同一个主仓渠道可以配对多个系统渠道，系统渠道只能配对一次。
8. SKU 候选同步、分页、搜索、状态筛选。
9. SKU 一对一配对、解除配对、审计事件。
10. 外部请求日志追加落库。
11. 菜单、按钮权限、若依操作日志。
12. React 管理端页面与 service/types/options 拆分。
13. 复用台账更新。

本轮不建议一次性做这些内容：

- 平台仓库主数据、平台物流渠道主数据的复杂治理。本轮只保存“系统已有编码”和“领星候选编码”的配对关系，不把领星候选反向写成平台事实主数据。
- 箱规配对和增值服务配对真实保存。旧工程只有空入口。
- 订单推送、履约状态回传、库存同步、费用入账。这些必须等订单、履约、库存、财务状态机确认。
- 物理删除主仓接入。涉及凭证、日志、映射和审计，第一版建议只做停用/启用或保留后续。

## 表设计确认

任何新增表执行前需要用户确认。本节给出建议表设计。

### `upstream_system_connection`

业务目的：保存主仓接入记录、授权状态、脱敏凭证展示值、排序和同步状态。

不承载：仓库主数据、库存事实、订单事实、财务事实、明文密钥。

| 字段 | 类型建议 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `connection_code` | varchar(64) | 是 | 无 | 主仓接入编码，例如 `LX-CA012-xxxx` |
| `system_kind` | varchar(32) | 是 | `lingxing-wms` | 上游系统类型 code |
| `master_warehouse_name` | varchar(200) | 是 | 无 | 主仓名称 |
| `settlement_type` | varchar(32) | 是 | `upstream-payable` | 结算类型 |
| `app_key_mask` | varchar(64) | 是 | 无 | 脱敏 appKey |
| `app_secret_mask` | varchar(64) | 是 | 无 | 脱敏 appSecret |
| `app_key_ciphertext` | text | 是 | 无 | 加密 appKey |
| `app_secret_ciphertext` | text | 是 | 无 | 加密 appSecret |
| `credential_key_id` | varchar(64) | 是 | `default` | 加密密钥版本 |
| `status` | varchar(16) | 是 | `ENABLED` | 接入状态 |
| `credential_status` | varchar(16) | 是 | `CONFIGURED` | 凭证状态 |
| `enabled_capabilities` | varchar(500) 或 text | 是 | 空 | 能力 code 列表 JSON 字符串 |
| `display_order` | int | 是 | 0 | 左侧排序 |
| `last_authorized_time` | datetime | 是 | 无 | 最后授权时间 |
| `last_sync_time` | datetime | 否 | null | 最后同步时间 |
| `request_log_count` | int | 是 | 0 | 请求日志摘要计数 |
| `create_by` | varchar(64) | 否 | 空 | 创建人 |
| `create_time` | datetime | 否 | null | 创建时间 |
| `update_by` | varchar(64) | 否 | 空 | 更新人 |
| `update_time` | datetime | 否 | null | 更新时间 |
| `remark` | varchar(500) | 否 | 空 | 备注 |

约束和索引：

- 主键：`connection_code`
- 唯一：`uk_upstream_connection_kind_code(system_kind, connection_code)`
- 索引：`idx_upstream_connection_order(display_order, last_authorized_time)`
- 索引：`idx_upstream_connection_status(status)`

### `upstream_system_warehouse_candidate`

业务目的：保存上游仓库候选快照，用于管理端展示和后续配对。

不承载：平台仓库主数据、库存事实。

| 字段 | 类型建议 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `connection_code` | varchar(64) | 是 | 无 | 主仓接入编码 |
| `warehouse_code` | varchar(100) | 是 | 无 | 领星仓库代码 |
| `warehouse_name` | varchar(200) | 是 | 无 | 领星仓库名称 |
| `country_code` | varchar(32) | 否 | 空 | 国家/地区 code |
| `status` | varchar(16) | 是 | `ACTIVE` | 候选状态 |
| `sync_batch_id` | varchar(64) | 是 | 无 | 同步批次 |
| `first_seen_time` | datetime | 是 | 无 | 首次出现时间 |
| `last_seen_time` | datetime | 是 | 无 | 最近出现时间 |
| `update_time` | datetime | 是 | 无 | 更新时间 |

约束和索引：

- 主键：`connection_code, warehouse_code`
- 索引：`idx_upstream_wh_candidate_status(connection_code, status)`

### `upstream_system_warehouse_pairing`

业务目的：保存系统仓库与领星主仓仓库的正式配对关系。

业务规则：仓库配对必须一对一；同一个系统仓库只能配对一次，不可重复；同一个主仓接入内，同一个领星仓库也只能配对一次。

不承载：平台仓库主数据维护、库存事实、库存流水、仓库启停业务规则。

| 字段 | 类型建议 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `warehouse_pairing_id` | bigint auto_increment | 是 | 无 | 主键 |
| `connection_code` | varchar(64) | 是 | 无 | 主仓接入编码 |
| `upstream_warehouse_code` | varchar(100) | 是 | 无 | 领星仓库代码 |
| `upstream_warehouse_name` | varchar(200) | 是 | 无 | 领星仓库名称快照 |
| `system_warehouse_code` | varchar(64) | 是 | 无 | 系统仓库代码 |
| `system_warehouse_name` | varchar(200) | 是 | 无 | 系统仓库名称快照 |
| `status` | varchar(16) | 是 | `ACTIVE` | 配对状态 |
| `create_by` | varchar(64) | 否 | 空 | 创建人 |
| `create_time` | datetime | 否 | null | 创建时间 |
| `update_by` | varchar(64) | 否 | 空 | 更新人 |
| `update_time` | datetime | 否 | null | 更新时间 |
| `remark` | varchar(500) | 否 | 空 | 备注 |

约束和索引：

- 主键：`warehouse_pairing_id`
- 唯一：`uk_upstream_wh_pairing_system(system_warehouse_code)`
- 唯一：`uk_upstream_wh_pairing_upstream(connection_code, upstream_warehouse_code)`
- 索引：`idx_upstream_wh_pairing_connection(connection_code)`

说明：`system_warehouse_code` 单列唯一用于硬性保证“系统仓库只能配对一次”。`connection_code, upstream_warehouse_code` 唯一用于保证同一个领星仓库在所属主仓接入内也只能配对一次。

### `upstream_system_logistics_channel_candidate`

业务目的：保存上游物流渠道候选快照，按仓库拉取，页面展示时按渠道代码合并。

不承载：平台物流渠道配置、发货事实。

| 字段 | 类型建议 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `connection_code` | varchar(64) | 是 | 无 | 主仓接入编码 |
| `warehouse_code` | varchar(100) | 是 | 无 | 领星仓库代码 |
| `channel_code` | varchar(100) | 是 | 无 | 领星渠道代码 |
| `channel_name` | varchar(200) | 是 | 无 | 领星渠道名称 |
| `status` | varchar(16) | 是 | `ACTIVE` | 候选状态 |
| `sync_batch_id` | varchar(64) | 是 | 无 | 同步批次 |
| `first_seen_time` | datetime | 是 | 无 | 首次出现时间 |
| `last_seen_time` | datetime | 是 | 无 | 最近出现时间 |
| `update_time` | datetime | 是 | 无 | 更新时间 |

约束和索引：

- 主键：`connection_code, warehouse_code, channel_code`
- 索引：`idx_upstream_channel_candidate_code(connection_code, channel_code)`
- 索引：`idx_upstream_channel_candidate_status(connection_code, status)`

### `upstream_system_logistics_channel_pairing`

业务目的：保存系统物流渠道与领星主仓物流渠道的正式配对关系。

业务规则：同一个领星主仓渠道可以配对多个系统渠道；同一个系统渠道只能配对一次，不可重复。

不承载：系统物流渠道主数据、物流计费规则、发货事实、履约轨迹。

| 字段 | 类型建议 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `logistics_channel_pairing_id` | bigint auto_increment | 是 | 无 | 主键 |
| `connection_code` | varchar(64) | 是 | 无 | 主仓接入编码 |
| `upstream_channel_code` | varchar(100) | 是 | 无 | 领星物流渠道代码 |
| `upstream_channel_name` | varchar(200) | 是 | 无 | 领星物流渠道名称快照 |
| `system_channel_code` | varchar(64) | 是 | 无 | 系统物流渠道代码 |
| `system_channel_name` | varchar(200) | 是 | 无 | 系统物流渠道名称快照 |
| `status` | varchar(16) | 是 | `ACTIVE` | 配对状态 |
| `create_by` | varchar(64) | 否 | 空 | 创建人 |
| `create_time` | datetime | 否 | null | 创建时间 |
| `update_by` | varchar(64) | 否 | 空 | 更新人 |
| `update_time` | datetime | 否 | null | 更新时间 |
| `remark` | varchar(500) | 否 | 空 | 备注 |

约束和索引：

- 主键：`logistics_channel_pairing_id`
- 唯一：`uk_upstream_channel_pairing_system(system_channel_code)`
- 索引：`idx_upstream_channel_pairing_upstream(connection_code, upstream_channel_code)`
- 索引：`idx_upstream_channel_pairing_connection(connection_code)`

说明：不对 `connection_code, upstream_channel_code` 做唯一约束，因为同一个领星主仓渠道允许配对多个系统渠道；只对 `system_channel_code` 做唯一约束，硬性保证系统渠道不可重复配对。

### `upstream_system_request_log`

业务目的：记录所有外部系统请求尝试，便于追踪授权、同步、失败原因。

不承载：明文密钥、完整 authcode、完整 PII。

| 字段 | 类型建议 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `request_log_id` | bigint auto_increment | 是 | 无 | 主键 |
| `connection_code` | varchar(64) | 否 | null | 可为空，授权失败前可能还没有接入编码 |
| `provider` | varchar(32) | 是 | 无 | `lingxing-wms` |
| `trace_id` | varchar(64) | 是 | 无 | 请求追踪 ID |
| `method` | varchar(16) | 是 | `POST` | HTTP 方法 |
| `url` | varchar(500) | 是 | 无 | 不含 authcode 的 URL |
| `path` | varchar(200) | 是 | 无 | 接口路径 |
| `attempt` | int | 是 | 1 | 第几次尝试 |
| `max_attempts` | int | 是 | 1 | 最大尝试次数 |
| `status` | varchar(16) | 是 | 无 | `SUCCESS` / `FAILURE` |
| `duration_ms` | int | 是 | 0 | 耗时 |
| `request_summary` | longtext | 是 | 无 | 脱敏请求摘要 JSON |
| `response_summary` | longtext | 否 | null | 脱敏响应摘要 JSON |
| `error_summary` | longtext | 否 | null | 脱敏错误摘要 JSON |
| `create_time` | datetime | 是 | 无 | 创建时间 |

约束和索引：

- 主键：`request_log_id`
- 索引：`idx_upstream_request_log_connection(connection_code)`
- 索引：`idx_upstream_request_log_trace(trace_id)`
- 索引：`idx_upstream_request_log_created(create_time)`

### `upstream_system_sku_candidate`

业务目的：保存领星 SKU 候选，不作为平台商品事实。

| 字段 | 类型建议 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `connection_code` | varchar(64) | 是 | 无 | 主仓接入编码 |
| `master_sku` | varchar(128) | 是 | 无 | 上游 masterSku |
| `master_product_name` | varchar(255) | 是 | 无 | 上游产品名 |
| `status` | varchar(16) | 是 | `ACTIVE` | `ACTIVE` / `MISSING` |
| `search_text` | text | 是 | 无 | 搜索文本 |
| `sync_batch_id` | varchar(64) | 是 | 无 | 同步批次 |
| `first_seen_time` | datetime | 是 | 无 | 首次出现时间 |
| `last_seen_time` | datetime | 是 | 无 | 最近出现时间 |
| `update_time` | datetime | 是 | 无 | 更新时间 |

约束和索引：

- 主键：`connection_code, master_sku`
- 索引：`idx_upstream_sku_candidate_status(connection_code, status)`
- 索引：`idx_upstream_sku_candidate_search(connection_code, master_sku, master_product_name)`

说明：旧工程 PostgreSQL 用 `pg_trgm` 做模糊搜索。若依当前是 MySQL，第一版先用 B-tree + `like` 搜索，后续数据量变大再评估全文索引。

### `upstream_system_sku_pairing`

业务目的：保存系统 SKU 与上游 masterSku 的一对一配对关系。

| 字段 | 类型建议 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `sku_pairing_id` | bigint auto_increment | 是 | 无 | 主键 |
| `connection_code` | varchar(64) | 是 | 无 | 主仓接入编码 |
| `master_sku` | varchar(128) | 是 | 无 | 上游 masterSku |
| `system_sku` | varchar(128) | 是 | 无 | 系统 SKU |
| `system_sku_name` | varchar(255) | 是 | 无 | 系统 SKU 名称快照 |
| `customer_name` | varchar(200) | 否 | 空 | 客户名称快照 |
| `create_by` | varchar(64) | 否 | 空 | 创建人 |
| `create_time` | datetime | 否 | null | 创建时间 |
| `update_by` | varchar(64) | 否 | 空 | 更新人 |
| `update_time` | datetime | 否 | null | 更新时间 |

约束和索引：

- 唯一：`uk_upstream_sku_pairing_master(connection_code, master_sku)`
- 唯一：`uk_upstream_sku_pairing_system(connection_code, system_sku)`
- 索引：`idx_upstream_sku_pairing_connection(connection_code)`

### `upstream_system_sku_sync_state`

业务目的：保存每个主仓 SKU 同步状态和 10 分钟新鲜度窗口。

| 字段 | 类型建议 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `connection_code` | varchar(64) | 是 | 无 | 主键 |
| `status` | varchar(16) | 是 | `NEVER` | `NEVER` / `SYNCING` / `FRESH` / `STALE` / `FAILED` |
| `sync_batch_id` | varchar(64) | 否 | null | 批次 |
| `started_time` | datetime | 否 | null | 开始时间 |
| `completed_time` | datetime | 否 | null | 完成时间 |
| `next_sync_time` | datetime | 否 | null | 下次同步时间 |
| `total_fetched` | int | 是 | 0 | 拉取数量 |
| `inserted_count` | int | 是 | 0 | 新增数量 |
| `updated_count` | int | 是 | 0 | 更新数量 |
| `marked_missing_count` | int | 是 | 0 | 标记缺失数量 |
| `error_message` | varchar(500) | 否 | 空 | 错误摘要 |

### `upstream_system_sku_pairing_audit_event`

业务目的：只追加记录 SKU 配对和解除配对审计事件。

| 字段 | 类型建议 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `audit_event_id` | bigint auto_increment | 是 | 无 | 主键 |
| `connection_code` | varchar(64) | 是 | 无 | 主仓接入编码 |
| `action` | varchar(16) | 是 | 无 | `PAIRED` / `UNPAIRED` |
| `master_sku` | varchar(128) | 是 | 无 | 上游 masterSku |
| `system_sku` | varchar(128) | 否 | null | 系统 SKU |
| `actor_username` | varchar(64) | 是 | 无 | 操作人 |
| `occurred_time` | datetime | 是 | 无 | 发生时间 |
| `summary` | varchar(500) | 是 | 无 | 可读摘要 |

约束和索引：

- 主键：`audit_event_id`
- 索引：`idx_upstream_sku_audit_connection(connection_code)`
- 索引：`idx_upstream_sku_audit_time(occurred_time)`

## 字典与 code 规则

建议新增若依字典：

| 字典类型 | 选项 code | label |
| --- | --- | --- |
| `upstream_system_kind` | `lingxing-wms` | 领星 WMS |
| `upstream_connection_status` | `ENABLED` / `PAUSED` / `WARNING` | 启用 / 暂停 / 预警 |
| `upstream_credential_status` | `CONFIGURED` / `EXPIRING` / `MISSING` | 已配置 / 即将过期 / 缺失 |
| `upstream_settlement_type` | `upstream-payable` / `self-operated-receivable` | 上游仓（应付） / 自营仓（应收） |
| `upstream_candidate_status` | `ACTIVE` / `MISSING` | 有效 / 上游缺失 |
| `upstream_sku_pairing_status` | `UNASSIGNED` / `PAIRED` / `MISSING_UPSTREAM` | 未配对 / 已配对 / 上游缺失 |
| `upstream_sku_sync_status` | `NEVER` / `SYNCING` / `FRESH` / `STALE` / `FAILED` | 未同步 / 同步中 / 已同步 / 需同步 / 同步失败 |

2026-06-04 修正：迁移时继续沿用旧工程的 `lingxing-wms`、`upstream-payable`、`self-operated-receivable` 这类 hyphen code 作为数据库 code。若依只负责通过字典展示 label，不重新发明业务 code。

## 后端实现计划

### 1. Maven 模块

新增：

```text
RuoYi-Vue/integration/
  pom.xml
  src/main/java/com/ruoyi/integration/
    controller/AdminUpstreamSystemController.java
    domain/
    dto/
    mapper/
    service/
    service/impl/
    support/
    lingxing/
  src/main/resources/mapper/integration/
```

调整：

- `RuoYi-Vue/pom.xml` 增加 `integration` module。
- `RuoYi-Vue/ruoyi-admin/pom.xml` 增加 `integration` 依赖。

### 2. 领星适配器

建议放在：

```text
RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/
```

职责：

- `LingxingOpenApiClient`：HTTP POST、超时、重试、错误映射。
- `LingxingSigningSupport`：签名。规则为 `appKey + sortedJson(data) + reqTimeSeconds` 后用 `appSecret` 做 HMAC-SHA256。
- `LingxingLogRedactionSupport`：脱敏 request/response/error。
- `LingxingCredentialCipher`：AES-GCM 加解密 appKey/appSecret。
- `LingxingPayloadMapper`：把 `whCode`、`whNameCn`、`countryCode`、`channelCode`、`channelName`、`sku`、`productName` 映射成若依 DTO。

外部接口：

- `POST /openapi/v1/workOrder/types`
- `POST /openapi/v1/warehouse/options`
- `POST /openapi/v1/logistics/channel/list`
- `POST /openapi/v1/product/pagelist`

SKU 分页规则：

- 请求字段使用 `page`、`pageSize`。
- `pageSize` 上限 100。
- 不使用旧错误字段 `pageNo` / `currentPage`。

### 3. Service 分层

建议：

- `IUpstreamSystemService`
  - listConnections
  - createLingxingWmsConnection
  - reauthorizeLingxingWmsConnection
  - updateConnectionInfo
  - syncConnection
  - updateConnectionOrder
  - listWarehouseCandidates
  - listLogisticsChannelCandidates
  - listSkuMappings
  - syncSkuCandidates
  - upsertSkuPairing
  - deleteSkuPairing
  - resolveMasterSku
- `UpstreamSkuSyncService`
  - 10 分钟新鲜度判断
  - 同步中保护
  - 批次写入
  - 失败状态记录
- `UpstreamRequestLogService`
  - append-only request log

Controller 不直接调用 Mapper。跨模块后续只能调用 Service 或明确 public support，不允许直接读取表。

## API 设计

建议采用若依风格接口：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/integration/admin/upstream-systems/list` | `integration:upstream:list` | 主仓接入列表 |
| GET | `/integration/admin/upstream-systems/{connectionCode}` | `integration:upstream:query` | 主仓详情 |
| POST | `/integration/admin/upstream-systems` | `integration:upstream:add` | 新增主仓并授权，请求体带 `systemKind` |
| PUT | `/integration/admin/upstream-systems/{connectionCode}/credentials` | `integration:upstream:credential` | 重新授权 |
| PUT | `/integration/admin/upstream-systems/{connectionCode}` | `integration:upstream:edit` | 编辑主仓名称、结算类型 |
| POST | `/integration/admin/upstream-systems/{connectionCode}/sync` | `integration:upstream:sync` | 静默全量同步 |
| PUT | `/integration/admin/upstream-systems/order` | `integration:upstream:order` | 保存左侧排序 |
| GET | `/integration/admin/upstream-systems/{connectionCode}/warehouses/list` | `integration:upstream:list` | 仓库候选 |
| GET | `/integration/admin/upstream-systems/{connectionCode}/logistics-channels/list` | `integration:upstream:list` | 物流渠道候选，后端可直接返回合并结果 |
| GET | `/integration/admin/upstream-systems/{connectionCode}/sku-mappings/list` | `integration:upstream:skuList` | SKU 映射列表 |
| POST | `/integration/admin/upstream-systems/{connectionCode}/sku-candidates/sync` | `integration:upstream:skuSync` | 强制同步 SKU |
| PUT | `/integration/admin/upstream-systems/{connectionCode}/sku-mappings` | `integration:upstream:skuPair` | 保存 SKU 配对 |
| DELETE | `/integration/admin/upstream-systems/{connectionCode}/sku-mappings/{masterSku}` | `integration:upstream:skuUnpair` | 解除 SKU 配对 |

所有写接口必须加 `@Log`。

建议 `@Log`：

- 新增主仓：`BusinessType.INSERT`
- 编辑信息：`BusinessType.UPDATE`
- 重新授权：`BusinessType.UPDATE`
- 同步：`BusinessType.UPDATE`
- 保存排序：`BusinessType.UPDATE`
- SKU 配对：`BusinessType.UPDATE`
- SKU 解除配对：`BusinessType.UPDATE`

## 菜单与权限

建议新建 SQL：

```text
RuoYi-Vue/sql/upstream_system_management_seed.sql
```

菜单建议：

| menu_id | 菜单名 | parent_id | path | component | perms |
| ---: | --- | ---: | --- | --- | --- |
| 2031 | 上游系统管理 | 2030 | `upstream-system` | `Integration/UpstreamSystem/index` | `integration:upstream:list` |

按钮权限建议：

| 菜单名 | perms |
| --- | --- |
| 查询上游系统 | `integration:upstream:query` |
| 新增主仓 | `integration:upstream:add` |
| 编辑主仓 | `integration:upstream:edit` |
| 重新授权 | `integration:upstream:auth` |
| 同步主仓 | `integration:upstream:sync` |
| 保存排序 | `integration:upstream:order` |
| SKU 查看 | `integration:upstream:skuList` |
| SKU 同步 | `integration:upstream:skuSync` |
| SKU 配对 | `integration:upstream:skuPair` |
| SKU 解除配对 | `integration:upstream:skuUnpair` |

前端按钮必须使用权限控制；后端权限不能省略。

## 前端实现计划

建议目录：

```text
react-ui/src/pages/Integration/UpstreamSystem/
  index.tsx
  components/
    ConnectionSidebar.tsx
    ConnectionBasicCard.tsx
    ConnectionFormModal.tsx
    ReauthorizeModal.tsx
    WarehouseCandidateTable.tsx
    LogisticsChannelTable.tsx
    SkuPairingPanel.tsx
  constants.ts
  types.ts
  options.ts
  utils.ts

react-ui/src/services/integration/upstreamSystem.ts
react-ui/src/types/integration/upstream-system.d.ts
```

前端复用规则：

- `getPersistedProTableSearch`：后续列表型表格筛选区复用。
- `getDictSelectOption` / `getDictValueEnum`：所有状态、类型、结算类型、SKU 状态从若依字典读取。
- `feedback.tsx`：使用全局 Ant Design feedback 桥，不直接用静态 `message` / `notification`。
- `matchPermission` / `useAccess`：按钮按权限显示。

页面布局：

- 左侧：主仓接入侧栏、搜索、新增主仓、调整位置/确认位置。
- 右侧：基本信息卡片 + 映射页签。
- 页签：
  - 仓库映射：候选展示，支持系统仓库与领星仓库一对一配对和解除配对。
  - 物流渠道：候选展示，按渠道代码合并仓库代码，支持系统渠道配对到领星主仓渠道；同一个领星渠道可配多个系统渠道。
  - SKU 配对：真实功能，支持服务端分页、状态筛选、字段搜索、同步 SKU、配对、解除。
  - 箱配对：占位，明确未接入。
  - 增值服务配对：占位，明确未接入。

为了避免旧工程 `UpstreamSystemManagementPage.tsx` 891 行的大文件问题，若依迁移时必须拆组件。单文件超过 300 行需要复查职责，超过 400 行需要说明，超过 500 行必须拆分或给出理由。

## 同步与定时任务

第一版必须支持手动同步：

- 新增主仓授权成功后同步仓库、物流渠道。
- 顶部“同步”按钮静默全量同步仓库、物流渠道、SKU。
- SKU 页签“同步 SKU”静默强制同步 SKU。
- 失败诊断进入请求日志和同步状态，不弹大量提示。

定时同步建议：

- 若依有 `ruoyi-quartz`，可添加一个 seed job 调用 `upstreamSystemTask.syncSkuCandidates()`。
- 默认 10 分钟一次。
- 任务必须禁止并发或在 Service 内做同步中保护。
- 若未在第一版启用 Quartz，也必须保留手动同步与 10 分钟新鲜度状态；后续单独打开任务。

## 配置与密钥

建议配置项：

```yaml
ruoyi:
  integration:
    lingxing:
      base-url: ${LINGXING_OPENAPI_BASE_URL:https://api.xlwms.com/openapi/v1}
      timeout-ms: ${LINGXING_OPENAPI_TIMEOUT_MS:10000}
      max-retries: ${LINGXING_OPENAPI_MAX_RETRIES:2}
      retry-delay-ms: ${LINGXING_OPENAPI_RETRY_DELAY_MS:250}
    secret:
      encryption-key: ${URILI_SECRET_ENCRYPTION_KEY:}
      encryption-key-id: ${URILI_SECRET_ENCRYPTION_KEY_ID:default}
```

实施前必须确认：

- 加密 key 从环境变量注入，不写入仓库。
- key 缺失时禁止新增或重新授权上游系统。
- 日志只写脱敏值。

## 实施顺序

建议一次性实施时按以下顺序推进：

1. 只读读取当前激活数据源配置，确认 MySQL/Redis 来源；不默认使用本地 Docker。
2. 写 `upstream_system_management_seed.sql`，包含表、字典、菜单、按钮权限、可选 Quartz job。
3. 新增 `integration` Maven 子模块，并挂入 root/admin pom。
4. 实现领星 Java client、签名、脱敏、重试、错误映射和请求日志 DTO。
5. 实现 domain、Mapper、Mapper XML、Service、Controller。
6. 实现 SKU 同步服务、一对一配对和 `resolveMasterSku`。
7. 实现 React service/types/options。
8. 拆分并实现上游系统管理页面。
9. 更新 `docs/architecture/reuse-ledger.md`。
10. 运行后端和前端验证。
11. 写阶段交付记录 Markdown。

## 验收标准

功能验收：

- 登录 `admin / admin123` 后，能在 `海外仓服务设置 -> 上游系统管理` 看到页面。
- 无权限用户看不到菜单或按钮，后端也会拒绝接口。
- 新增领星 WMS 主仓时，授权成功后出现主仓记录，前端不显示明文 appSecret。
- 授权失败时不创建有效接入记录，但请求日志可追踪失败。
- 编辑主仓名称/结算类型不会打乱左侧顺序。
- 重新授权不会覆盖刚保存的结算类型。
- 同步按钮静默运行，成功后刷新仓库、物流渠道、SKU 状态。
- 调整位置/确认位置后刷新页面仍保持顺序。
- 仓库候选只展示，不写入库存事实。
- 物流渠道候选按渠道代码合并仓库代码，只展示，不写入履约事实。
- SKU 配对支持分页、状态筛选、搜索、配对、解除。
- 同一 `connection_code` 内 SKU 配对一对一冲突会失败。
- 上游 SKU 缺失时显示 `MISSING_UPSTREAM`。
- `resolveMasterSku` 对未配对或上游缺失 SKU 返回阻断错误。
- 请求日志中没有明文 appKey、appSecret、authcode、PII。

验证命令：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests install

cd E:\Urili-Ruoyi\react-ui
npm run lint
npm run build
```

如果实施中新增前端测试或后端单元测试，应补充运行对应测试命令。

浏览器验证：

- 后端：`http://127.0.0.1:8080`
- 前端：`http://127.0.0.1:8001`
- 登录：`admin / admin123`
- 验证菜单、权限按钮、授权弹窗、同步、排序、SKU 配对。

## 风险与待确认点

必须确认后才能执行数据库写入：

1. 是否确认新增上述上游系统业务表。
2. 是否确认 code 使用若依风格大写 code，而不是旧工程 hyphen code。
3. 是否第一版启用 Quartz 10 分钟 SKU 定时同步；如果不启用，则只做手动同步和状态展示。
4. 箱规配对、增值服务配对是否只保留候选/占位，不做真实保存。
5. 主仓接入是否不提供物理删除，只提供暂停/启用或暂不实现删除。
6. 加密 key 是否使用 `URILI_SECRET_ENCRYPTION_KEY` / `URILI_SECRET_ENCRYPTION_KEY_ID`。

当前判断：

- 可以准备一次性实现“菜单 + 核心接入 + 仓库配对 + 物流渠道配对 + SKU 配对”。
- 不建议在同一轮补箱规、增值服务、订单出站，因为这些会跨到仓库、履约、商品、财务模块，旧工程也没有完整落地。

## 已验证与未验证

已验证：

- 已阅读当前工程 `AGENTS.md` 规则。
- 已检查当前工程复用台账 `docs/architecture/reuse-ledger.md`。
- 已检查当前顶级菜单 seed，确认 `2030` 是“海外仓服务设置”，备注为复用原上游系统菜单位。
- 已静态检查旧工程上游系统页面、SKU 配对组件、API route、service、contracts、module、Lingxing client、migration、状态报告和 ADR。
- 已静态检查当前若依工程卖家/买家模块模式、菜单 seed、权限模式、前端 service/types/page 结构。

未验证：

- 未连接当前激活 MySQL/Redis。
- 未执行任何 SQL。
- 未运行 Maven/前端构建。
- 未浏览器打开页面，因为本次只做迁移计划。
