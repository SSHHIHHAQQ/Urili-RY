# 仓库管理表设计方案

## 当前目标

为管理端新增“仓库管理 / 官方仓库”和“仓库管理 / 第三方仓库”两个菜单，落地仓库主数据、第三方仓归属卖家、官方仓从上游系统主仓仓库候选同步并自动配对的首版能力。

本方案只做设计确认，不包含可执行 DDL/DML。用户确认后，才能进入 SQL、后端、前端和菜单权限实现。

## 已确认口径

- 仓库按“主表 + 类型扩展表”设计，不做三张平级仓库表。
- 新增 3 张仓库业务表：
  - `warehouse`：仓库共有主数据。
  - `official_warehouse`：官方仓 1:1 扩展表。
  - `third_party_warehouse`：第三方仓 1:1 扩展表。
- 仓库编码全局唯一，不按官方/第三方分别唯一。
- 状态使用若依风格：`0` 正常，`1` 停用。
- 不新增 `source` 字段。手工新增和同步新增最终都是仓库主数据；是否来自上游配对，通过现有 `upstream_system_warehouse_pairing` 判断。
- 官方仓库要做“同步仓库”便捷功能：选择上游系统主仓下的仓库候选，创建官方仓后自动写入现有仓库配对关系。
- 已经配对的上游仓库禁止再次同步，并提示用户。
- 不做物理删除，只做启用/停用。
- 第三方仓只允许绑定正常状态卖家。
- 国家/地区复用若依字典 `country_region`，业务表保存 2 位国家/地区代码。
- 美国州/城市需要完整字典；只有国家选择 `US` 时，州/城市字典联动才生效。其他国家首版不启用州/城市字典，按普通文本输入保存。
- 美国州保存英文全称，例如 `California`，不保存 `CA` 作为仓库地址字段值。
- 美国州/城市支持模糊搜索，也允许自定义输入。
- 官方仓和第三方仓新增/编辑必填：仓库编码、仓库名称、国家、州/省、城市、地址1、邮编、联系人、邮箱、结算币种。

## 复用现有能力

| 能力 | 现有位置 | 复用方式 |
| --- | --- | --- |
| 顶级菜单 | `RuoYi-Vue/sql/top_menu_seed.sql` 的 `warehouse` 顶级菜单 | 在该菜单下挂官方仓库、第三方仓库二级页面 |
| 国家/地区 | 若依字典 `country_region` | 仓库表保存 `country_code`，页面展示字典 label |
| 状态 | 若依 `sys_normal_disable` 口径 | 仓库状态保存 `0` / `1` |
| 币种 | `finance_currency` 和 `IFinanceCurrencyService.selectEnabledCurrencyOptions()` | 结算币种只允许选择启用币种 |
| 卖家 | `seller` 模块 | 第三方仓保存 `seller_id`，页面关联展示卖家编码、名称、简称 |
| 上游主仓仓库候选 | `upstream_system_warehouse_candidate` | 官方仓同步弹窗读取候选仓库 |
| 上游仓库配对 | `upstream_system_warehouse_pairing` | 官方仓同步成功后写入正式配对，不在官方仓表重复保存配对字段 |
| 操作日志 | 若依 `@Log` / `sys_oper_log` | 新增、编辑、启停、同步配对都记录操作日志 |
| 权限 | 若依 `sys_menu` / `@PreAuthorize` | 管理端接口和按钮按权限点控制 |

## 不做范围

- 不做物理删除。
- 不做导出。
- 不做库存数量、库存流水、订单履约状态。
- 不做费用模板、计费规则、运费、仓租或结算流水。
- 不做第三方仓审核流程。
- 不在 `official_warehouse` 中复制上游仓库配对字段。
- 不为非美国国家维护州/城市字典；后续国家再单独扩展。

## 表结构设计

### `warehouse`

业务目的：保存所有仓库共有主数据，是库存、订单、履约、商品发货仓等后续模块引用仓库的统一事实源。

业务逻辑：

- 一条记录代表一个系统仓库。
- `warehouse_code` 全局唯一，后续库存、订单、上游配对都使用该编码识别仓库。
- `warehouse_kind` 区分官方仓和第三方仓，但页面入口分开，新增时由后端按入口写入，不让用户选择。
- 不保存 `source`，不区分手工仓和同步仓。
- 地址字段只保存仓库当前主数据；地址变更历史首版不单独建流水。
- 不承载库存数量、费用规则、外部请求日志。

字段设计：

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `warehouse_id` | `bigint(20)` | 是 | 自增 | 主键 | 仓库 ID |
| `warehouse_code` | `varchar(64)` | 是 | 无 | 唯一 `uk_warehouse_code` | 系统仓库编码，全局唯一 |
| `warehouse_name` | `varchar(200)` | 是 | 无 | 索引 `idx_warehouse_name` | 仓库名称 |
| `warehouse_kind` | `varchar(32)` | 是 | 无 | 索引 `idx_warehouse_kind_status` | 仓库类型：`official` / `third_party` |
| `country_code` | `varchar(32)` | 是 | 无 | 索引 `idx_warehouse_country_state_city` | 国家/地区代码，复用 `country_region` |
| `state_province` | `varchar(100)` | 是 | `''` | 同上 | 州/省。美国选字典时保存州全称 |
| `city` | `varchar(100)` | 是 | 无 | 同上 | 城市 |
| `postal_code` | `varchar(32)` | 是 | 无 | 无 | 邮编 |
| `address_line1` | `varchar(255)` | 是 | 无 | 无 | 地址1 |
| `address_line2` | `varchar(255)` | 否 | `''` | 无 | 地址2 |
| `contact_name` | `varchar(100)` | 是 | 无 | 无 | 联系人 |
| `contact_phone` | `varchar(64)` | 否 | `''` | 无 | 联系电话 |
| `contact_email` | `varchar(128)` | 是 | 无 | 无 | 联系邮箱 |
| `company_name` | `varchar(200)` | 否 | `''` | 无 | 公司名称 |
| `settlement_currency` | `varchar(16)` | 是 | 无 | 索引 `idx_warehouse_currency` | 结算币种，必须是启用的 `finance_currency.currency_code` |
| `status` | `char(1)` | 是 | `0` | 索引 `idx_warehouse_kind_status` | 状态：`0` 正常，`1` 停用 |
| `create_by` | `varchar(64)` | 否 | `''` | 无 | 创建人 |
| `create_time` | `datetime` | 否 | `null` | 索引 `idx_warehouse_create_time` | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` | 无 | 更新人 |
| `update_time` | `datetime` | 否 | `null` | 无 | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` | 无 | 备注 |

建议索引：

- 主键：`pk_warehouse(warehouse_id)`
- 唯一：`uk_warehouse_code(warehouse_code)`
- 普通：`idx_warehouse_kind_status(warehouse_kind, status)`
- 普通：`idx_warehouse_country_state_city(country_code, state_province, city)`
- 普通：`idx_warehouse_name(warehouse_name)`
- 普通：`idx_warehouse_currency(settlement_currency)`
- 普通：`idx_warehouse_create_time(create_time)`

### `official_warehouse`

业务目的：标记并承载官方仓专属信息。首版官方仓专属字段很少，但保留扩展表可以避免后续把官方仓费用、配对策略、运营属性混进主表。

业务逻辑：

- 与 `warehouse` 通过 `warehouse_id` 1:1。
- 只有 `warehouse.warehouse_kind = official` 的记录才能存在对应扩展记录。
- 上游仓库配对不存这里，正式配对关系仍以 `upstream_system_warehouse_pairing` 为准。
- 首版不放费用、计费、库存或外部请求日志字段。

字段设计：

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `warehouse_id` | `bigint(20)` | 是 | 无 | 主键 | 对应 `warehouse.warehouse_id` |
| `create_by` | `varchar(64)` | 否 | `''` | 无 | 创建人 |
| `create_time` | `datetime` | 否 | `null` | 无 | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` | 无 | 更新人 |
| `update_time` | `datetime` | 否 | `null` | 无 | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` | 无 | 官方仓备注 |

说明：

- 如果后续确认官方仓没有任何专属字段，也可以在实现前再评估是否暂缓建 `official_warehouse`。当前按用户确认的三表模型保留。
- 不建议建立数据库外键，保持与当前业务表风格一致；由 Service 校验 `warehouse_id` 和 `warehouse_kind`。

### `third_party_warehouse`

业务目的：保存第三方仓与卖家主体的归属关系。

业务逻辑：

- 与 `warehouse` 通过 `warehouse_id` 1:1。
- 只有 `warehouse.warehouse_kind = third_party` 的记录才能存在对应扩展记录。
- 新增第三方仓时只能选择正常状态卖家。
- 表内只保存 `seller_id`，卖家编码、名称、简称展示时实时关联 `seller`，不做快照。
- 卖家后续停用不影响历史仓库展示，但不能再新建绑定该卖家。

字段设计：

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `warehouse_id` | `bigint(20)` | 是 | 无 | 主键 | 对应 `warehouse.warehouse_id` |
| `seller_id` | `bigint(20)` | 是 | 无 | 索引 `idx_third_party_warehouse_seller` | 归属卖家 ID |
| `create_by` | `varchar(64)` | 否 | `''` | 无 | 创建人 |
| `create_time` | `datetime` | 否 | `null` | 无 | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` | 无 | 更新人 |
| `update_time` | `datetime` | 否 | `null` | 无 | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` | 无 | 第三方仓备注 |

建议索引：

- 主键：`pk_third_party_warehouse(warehouse_id)`
- 普通：`idx_third_party_warehouse_seller(seller_id)`

## 美国州/城市字典设计

完整美国城市数据量不适合放入 `sys_dict_data`，建议使用专用字典表。国家仍复用 `country_region`，美国州/城市表只服务 `country_code = US` 的地址输入联动。

### `us_state`

业务目的：维护美国州候选项，前端按州全称模糊搜索。

字段设计：

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `state_id` | `bigint(20)` | 是 | 自增 | 主键 | 州 ID |
| `state_code` | `varchar(16)` | 是 | 无 | 唯一 `uk_us_state_code` | 州缩写，例如 `CA` |
| `state_name` | `varchar(100)` | 是 | 无 | 唯一 `uk_us_state_name` | 州全称，例如 `California` |
| `status` | `char(1)` | 是 | `0` | 索引 `idx_us_state_status` | 状态：`0` 正常，`1` 停用 |
| `create_by` | `varchar(64)` | 否 | `''` | 无 | 创建人 |
| `create_time` | `datetime` | 否 | `null` | 无 | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` | 无 | 更新人 |
| `update_time` | `datetime` | 否 | `null` | 无 | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` | 无 | 备注 |

保存规则：

- 仓库表 `state_province` 保存 `state_name`，不保存 `state_code`。
- `state_code` 只用于导入、去重和城市关联。
- 前端显示和搜索以 `state_name` 为主，可同时匹配 `state_code`。

### `us_city`

业务目的：维护完整美国城市候选项，前端在选择美国和州后按城市名模糊搜索。

字段设计：

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `city_id` | `bigint(20)` | 是 | 自增 | 主键 | 城市 ID |
| `place_geoid` | `varchar(16)` | 是 | 无 | 唯一 `uk_us_city_geoid` | Census place GEOID，用于完整去重 |
| `state_code` | `varchar(16)` | 是 | 无 | 索引 `idx_us_city_state_city` | 所属州缩写 |
| `state_name` | `varchar(100)` | 是 | 无 | 索引 `idx_us_city_state_city` | 所属州全称冗余，便于查询展示 |
| `city_name` | `varchar(150)` | 是 | 无 | 索引 `idx_us_city_state_city` | 城市基础名称，地址字段保存该值 |
| `place_name` | `varchar(200)` | 是 | 无 | 无 | Census 原始 place 名称，例如 `Los Angeles city` |
| `place_type` | `varchar(64)` | 否 | `''` | 无 | Census place 类型，例如 `city`、`town`、`CDP` |
| `status` | `char(1)` | 是 | `0` | 索引 `idx_us_city_status` | 状态：`0` 正常，`1` 停用 |
| `create_by` | `varchar(64)` | 否 | `''` | 无 | 创建人 |
| `create_time` | `datetime` | 否 | `null` | 无 | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` | 无 | 更新人 |
| `update_time` | `datetime` | 否 | `null` | 无 | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` | 无 | 备注 |

建议索引：

- 主键：`pk_us_city(city_id)`
- 唯一：`uk_us_city_geoid(place_geoid)`
- 普通：`idx_us_city_state_city(state_name, city_name)`
- 普通：`idx_us_city_status(status)`

保存规则：

- 仓库表 `city` 保存 `city_name`。
- 国家不是 `US` 时，前端不调用美国州/城市接口，后端不要求命中 `us_state` / `us_city`。
- 国家是 `US` 时，前端提供州/城市字典候选和模糊搜索；用户自定义输入仍允许保存，后端只做长度、必填、邮箱、币种和编码唯一等基础校验。
- 完整城市初始化数据需要在执行 SQL 前记录数据来源、数据日期、导入脚本和回滚方式。

## 官方仓同步并自动配对流程

入口：管理端“仓库管理 / 官方仓库”页面工具栏按钮“同步仓库”。

流程：

1. 用户打开同步弹窗。
2. 选择一个已启用的上游系统主仓接入。
3. 页面读取该主仓下 `upstream_system_warehouse_candidate` 的仓库候选，并合并 `upstream_system_warehouse_pairing` 标记已配对项。
4. 已经配对的候选禁用选择，并提示“该上游仓库已配对，不能重复同步”。
5. 用户选择未配对候选后，系统带出：
   - 上游仓库编码。
   - 上游仓库名称。
   - 国家代码。
   - 默认系统仓库编码等于上游仓库编码，但允许用户修改。
   - 默认系统仓库名称等于上游仓库名称，但允许用户修改。
6. 用户补齐仓库必填字段：州/省、城市、地址1、邮编、联系人、邮箱、结算币种等。
7. 后端在一个事务内：
   - 校验上游主仓存在且启用。
   - 校验候选仓库存在且未配对。
   - 校验系统仓库编码全局唯一。
   - 创建 `warehouse`，类型写入 `official`，状态默认 `0`。
   - 创建 `official_warehouse`。
   - 通过 integration 服务写入 `upstream_system_warehouse_pairing`。
8. 如果配对插入时发生唯一冲突，整个事务回滚，避免产生未配对的半成品官方仓。

同编码已存在的处理：

- 若候选已配对：直接禁止同步。
- 若系统仓库编码已存在：首版不自动覆盖已有仓库资料，提示用户更换系统仓库编码或后续走“配对已有官方仓”能力。
- “配对已有官方仓”可以作为后续增强，不放入本次首版范围。

## 第三方仓新增/编辑流程

- 新增第三方仓时必须选择卖家。
- 卖家选择器只展示 `seller.status = 0` 的卖家。
- 仓库保存时创建 `warehouse` 和 `third_party_warehouse`。
- 编辑第三方仓允许修改地址、联系人、结算币种、备注和归属卖家。
- 如果仓库已经被后续库存、订单、履约使用，是否允许修改归属卖家需要后续业务模块落地后重新收口；首版由于还没有库存/订单事实表，暂不限制。

## 页面和菜单设计

### 菜单

沿用现有顶级菜单 `warehouse`。

建议新增二级菜单：

| 菜单 | parent | path | component | route_name | 权限 |
| --- | --- | --- | --- | --- | --- |
| 官方仓库 | `warehouse` | `official` | `Warehouse/Official/index` | `OfficialWarehouse` | `warehouse:official:list` |
| 第三方仓库 | `warehouse` | `third-party` | `Warehouse/ThirdParty/index` | `ThirdPartyWarehouse` | `warehouse:thirdParty:list` |

### 权限点

官方仓库：

| 操作 | 权限点 |
| --- | --- |
| 查询/列表 | `warehouse:official:list` |
| 新增 | `warehouse:official:add` |
| 编辑 | `warehouse:official:edit` |
| 启停 | `warehouse:official:status` |
| 同步仓库 | `warehouse:official:sync` |

第三方仓库：

| 操作 | 权限点 |
| --- | --- |
| 查询/列表 | `warehouse:thirdParty:list` |
| 新增 | `warehouse:thirdParty:add` |
| 编辑 | `warehouse:thirdParty:edit` |
| 启停 | `warehouse:thirdParty:status` |

### 前端约束

- 复用 Ant Design Pro / ProTable 风格。
- 不在页面额外手写标题。
- 列表搜索使用现有 ProTable 搜索预设。
- 国家、州、城市、卖家、币种选择器均支持模糊搜索。
- 国家为 `US`：
  - 州选择器读取 `us_state`，展示州全称，支持自定义输入。
  - 城市选择器按州读取 `us_city`，支持自定义输入。
- 国家不是 `US`：
  - 州/城市不调用美国字典。
  - 使用普通输入或可自定义输入控件。
- 行内操作最多展示两个高频按钮，其余进入“更多”下拉。
- 不做删除按钮。

## 后端接口草案

官方仓库：

| 方法 | 路径 | 权限 | 用途 |
| --- | --- | --- | --- |
| `GET` | `/warehouse/official/list` | `warehouse:official:list` | 官方仓库分页查询 |
| `GET` | `/warehouse/official/{warehouseId}` | `warehouse:official:list` | 官方仓详情 |
| `POST` | `/warehouse/official` | `warehouse:official:add` | 手工新增官方仓 |
| `PUT` | `/warehouse/official` | `warehouse:official:edit` | 编辑官方仓 |
| `PUT` | `/warehouse/official/status` | `warehouse:official:status` | 启停官方仓 |
| `GET` | `/warehouse/official/sync-candidates` | `warehouse:official:sync` | 查询可同步上游仓库候选 |
| `POST` | `/warehouse/official/sync` | `warehouse:official:sync` | 创建官方仓并自动配对 |

第三方仓库：

| 方法 | 路径 | 权限 | 用途 |
| --- | --- | --- | --- |
| `GET` | `/warehouse/third-party/list` | `warehouse:thirdParty:list` | 第三方仓库分页查询 |
| `GET` | `/warehouse/third-party/{warehouseId}` | `warehouse:thirdParty:list` | 第三方仓详情 |
| `POST` | `/warehouse/third-party` | `warehouse:thirdParty:add` | 新增第三方仓 |
| `PUT` | `/warehouse/third-party` | `warehouse:thirdParty:edit` | 编辑第三方仓 |
| `PUT` | `/warehouse/third-party/status` | `warehouse:thirdParty:status` | 启停第三方仓 |

地址/选项：

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/warehouse/options/us-states` | 美国州候选，模糊搜索 |
| `GET` | `/warehouse/options/us-cities` | 美国城市候选，按州和关键词搜索 |
| `GET` | `/finance/currency/options` | 复用现有启用币种 options |
| `GET` | `/seller/options` | 建议新增或复用卖家 options，只返回正常状态卖家 |

## 字典和 code/label 规则

| 项 | 存储 | 展示 |
| --- | --- | --- |
| 仓库类型 | `official` / `third_party` | 官方仓库 / 第三方仓库 |
| 仓库状态 | `0` / `1` | 正常 / 停用 |
| 国家/地区 | `country_region.dict_value`，例如 `US` | `country_region.dict_label` |
| 美国州 | `state_name`，例如 `California` | `state_name` |
| 美国城市 | `city_name`，例如 `Los Angeles` | `city_name` |
| 结算币种 | `finance_currency.currency_code`，例如 `USD` | 币种 code/name/symbol 组合展示 |
| 第三方仓卖家 | `seller_id` | 卖家编号、卖家代码、卖家名称、卖家简称 |

## 初始化数据和迁移方式

确认后建议拆分为以下 SQL：

1. `warehouse_management_seed.sql`
   - 创建 `warehouse`、`official_warehouse`、`third_party_warehouse`。
   - 初始化仓库类型字典或常量记录。
   - 初始化菜单和权限点。
2. `warehouse_us_address_seed.sql`
   - 创建 `us_state`、`us_city`。
   - 导入完整美国州/城市数据。
   - 记录数据来源和导入日期。

执行前必须再次确认当前激活数据源，不默认使用本地 Docker MySQL。

## 回滚方式

设计层回滚：

- 用户未确认前不执行 SQL，不产生数据库变更。

执行层回滚草案：

- 菜单权限：按 menu_id 或 perms 删除新增官方仓/第三方仓菜单和按钮权限。
- 仓库业务表：如果无业务数据，按依赖顺序删除 `third_party_warehouse`、`official_warehouse`、`warehouse`。
- 美国地址字典：删除 `us_city`、`us_state`。
- 如果已经产生仓库数据，不做直接 drop，先导出备份，再通过停用或迁移脚本处理。

## 权限检查结果

- 官方仓、第三方仓都属于管理端，使用若依 `sys_menu` / `sys_role` / `@PreAuthorize`。
- 卖家端、买家端首版不开放仓库维护入口。
- 官方仓同步按钮必须同时有前端按钮权限和后端 `warehouse:official:sync` 权限。
- 第三方仓只能绑定正常状态卖家，不能通过前端传入任意 sellerId 绕过后端校验。

## 字典/选项复用检查结果

- 国家/地区复用 `country_region`。
- 状态复用若依正常/停用口径。
- 结算币种复用 `finance_currency` 启用币种 options。
- 美国完整州/城市不放入 `sys_dict_data`，使用专用表。
- 前端不内联国家、州、城市、币种、卖家大段选项。

## 复用台账检查结果

- 仓库模块落地后需要更新 `docs/architecture/reuse-ledger.md`：
  - 登记 `warehouse` 模块为系统仓库主数据事实源。
  - 登记 `us_state` / `us_city` 的使用规则。
  - 登记官方仓同步复用 `upstream_system_warehouse_pairing`，不复制配对字段。
  - 登记后续商品、库存、订单引用仓库时优先使用 `warehouse`。

## 大文件合理性判断

- 本方案是设计文档，不触发代码文件行数拆分。
- 后续实现时，前端官方仓/第三方仓页面应抽取共享表格、表单、地址选择器、同步弹窗，避免单页超过 400 行后职责混杂。
- 后端 Service 如同时承载普通 CRUD、同步配对、地址选项，应拆分为主服务、同步服务、选项服务或内部 helper。

## 重复代码检查结果

- 官方仓和第三方仓共享 `warehouse` 主数据 DTO、校验、Mapper 片段和前端表单字段。
- 官方仓同步不重复实现上游仓库配对逻辑，应调用 integration 服务或提取稳定 facade。
- 地址联动控件可抽成共享组件，避免两个页面重复写国家/州/城市联动。

## 验证计划

设计确认后实现阶段至少验证：

- 后端单元/集成测试：
  - 仓库编码全局唯一。
  - 官方仓新增会写 `warehouse` + `official_warehouse`。
  - 第三方仓新增会写 `warehouse` + `third_party_warehouse`，且卖家必须正常。
  - 已配对上游仓库不能再次同步。
  - 官方仓同步创建和配对在同一事务内成功或回滚。
  - 国家非 `US` 时不要求命中美国州/城市表。
  - 国家为 `US` 时支持字典候选，同时允许自定义输入。
- 前端验证：
  - 官方仓列表、第三方仓列表筛选和分页。
  - 新增/编辑必填校验。
  - 美国州/城市联动只在国家为 `US` 时生效。
  - 官方仓同步弹窗禁用已配对候选并提示。
  - 无权限用户看不到按钮，后端也拒绝调用。
- 数据库验证：
  - 初始化 SQL 可重复执行。
  - 唯一索引生效。
  - 菜单权限 seed 可重复执行。

## 未验证原因

- 当前是设计确认阶段，未执行 SQL、未改后端、未改前端、未跑测试。
- 完整美国城市数据源尚未进入执行阶段；导入前需要记录数据来源、数据日期和导入脚本。
