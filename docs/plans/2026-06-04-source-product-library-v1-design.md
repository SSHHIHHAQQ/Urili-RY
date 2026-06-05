# 来源商品库首版设计方案

日期：2026-06-04

状态：已根据领星 `/product/pagelist` 真实同步响应修正，作为表结构确认前的设计草案。

## 目标

「来源商品库」用于在管理端集中展示各来源系统同步回来的 SKU 基础信息。

首版目标是先把来源 SKU 快照表补完整：能按 SKU 粒度保存领星同步商品的基础资料、图片、识别码、FNSKU、尺寸重量、WMS 尺寸重量、申报信息、分类、危险品和扩展 JSON，再基于这些字段做管理端只读列表。后续再逐步补库存、商城商品草稿生成等能力。

## 已确认口径

- 首版只做管理端，不做卖家端展示。
- 一行代表一个 SKU。
- 行粒度按「来源系统 + 主仓 + SKU」分开。
- SKU 匹配卖家、匹配商城商品的功能继续放在「上游系统管理」里做；来源商品库只集中展示这些结果。
- 一个来源 SKU 当前只配对一个系统 SKU / 商城商品。
- 如果后续从来源 SKU 创建商城商品，创建结果应为草稿。
- 首版优先简单，不做复杂规则、自动匹配或卖家端分发流程。

## 业务边界

### 本页面承载

- 来源 SKU 的集中查询和展示。
- 来源系统和主仓信息展示。
- 来源 SKU 与商城商品配对结果展示。
- 来源 SKU 与客户/卖家名称快照展示。
- 来源 SKU 同步状态和最近同步时间展示。
- 查看单条来源 SKU 的基础详情。

### 本页面不承载

- 不在这里做 SKU 配对。
- 不在这里做卖家匹配规则。
- 不在这里编辑来源 SKU。
- 不在这里直接修改上游系统同步清单。
- 不在这里直接维护库存事实、商城商品事实或财务信息。
- 不在首版生成商城商品草稿；草稿生成放到第二步。

## 领星字段核查

### 核查来源

- 本地适配器当前调用：`POST https://api.xlwms.com/openapi/v1/product/pagelist`。
- 公开 API 文档确认该接口为「分页查询产品列表」，请求体包含 `appKey`、`reqTime`、`data`。
- 运行库请求日志核查：远端 MySQL `fenxiao`，连接来源为当前激活 `application-druid.yml` 的 `RUOYI_DB_*` 配置，只读查询 `upstream_system_request_log` 中 `operation='SKU_SYNC'` 的脱敏响应。
- 执行命令类型：只读 SELECT。
- 数据影响：无 DDL、无 DML、无远端数据修改。
- 最近一次成功日志：`LX-CA012`，接口 `/product/pagelist`，响应结构为 `code / data / msg`，`data` 下含 `records / total / page / pageSize / pages`。

### 真实响应字段

真实 `records` 单行字段如下：

```text
sku
productName
productAliasName
approveStatus
productDescription
imageUrl
mainCode
otherCode
fnsku
length
lengthBs
width
widthBs
height
heightBs
weight
weightBs
wmsLength
wmsLengthBs
wmsWidth
wmsWidthBs
wmsHeight
wmsHeightBs
wmsWeight
wmsWeightBs
declareNameCn
declareNameEn
customhouseCode
currencyCode
declarePrice
countryOfOriginName
dangerousCargo
cat1Name
cat2Name
cat3Name
platformSkuInfoList
type
brazilTaxInfoList
```

最近 20 条 `SKU_SYNC` 成功日志采样：

- 采样 SKU 记录数：`1901`
- `platformSkuInfoList`：采样内为空数组，仍保留 JSON 字段用于后续平台 SKU 信息。
- `brazilTaxInfoList`：采样内有数据，子字段包括 `accountId`、`cnpj`、`companyName`、`createTime`、`id`、`ncmCode`、`nfeUnit`、`origin`、`sku`、`taxId`、`taxName`、`updateTime`。

## 数据归属

首版不另起一张重复来源商品表，建议扩展现有 `integration` 上游系统数据：

- `upstream_system_sku_candidate`：来源 SKU 同步清单。
- `upstream_system_sku_pairing`：来源 SKU 与系统 SKU / 商城商品的配对关系。
- `upstream_system_sku_sync_state`：SKU 同步状态。
- `upstream_system_connection`：来源系统接入、主仓名称、来源系统类型、最近同步时间。

来源商品库是这些数据的管理端聚合视图，不是新的商城商品事实源。

`upstream_system_sku_candidate` 当前只有 `master_sku`、`master_product_name` 等少量字段，不够承载来源商品库。首版建议对该表做字段扩展，让它从“SKU 配对候选清单”升级为“来源 SKU 快照清单”。

扩展原因：

- 现有主键 `(connection_code, master_sku)` 与来源商品库首版行唯一性一致。
- 现有同步流程已经按分页批量 upsert 该表。
- 现有配对表已经通过 `connection_code + master_sku` 关联。
- 不新增重复表，避免同步清单和来源商品库两份数据不一致。
- 字段仍属于 `integration` 外部系统快照，不把来源快照误当成商城商品事实。

## 行唯一性

首版行唯一键建议使用：

```text
connectionCode + masterSku
```

当前 `connectionCode` 已代表一个来源系统接入和主仓接入，能满足「来源系统 + 主仓 + SKU」的首版粒度。

如果后续领星或其他 WMS 同步结果需要细到仓库 SKU 维度，再将行唯一性扩展为：

```text
sourceSystem + warehouseCode + sourceSku
```

该扩展需要单独写表结构或字段设计方案。

## 首版列表设计

菜单位置：

```text
商品管理 / 来源商品库
```

筛选项：

| 字段 | 说明 | 首版来源 |
| --- | --- | --- |
| 来源系统 | 例如领星 WMS | `upstream_system_connection.system_kind`，展示字典 label |
| 主仓 | 主仓接入名称 | `upstream_system_connection.master_warehouse_name` |
| 来源 SKU | 领星 `masterSku` | `upstream_system_sku_candidate.master_sku` |
| 商品名称 | 领星产品名 | `upstream_system_sku_candidate.master_product_name` |
| 识别码 | 主识别码、其他识别码、FNSKU | 扩展字段 |
| 分类 | 一级/二级/三级分类 | 扩展字段 |
| 危险品 | 领星危险品标识 | 扩展字段 |
| 申报信息 | 申报品名、海关编码、币种、申报价 | 扩展字段 |
| 同步状态 | 正常 / 已缺失等 | `upstream_system_sku_candidate.status` |
| 配对状态 | 未配对 / 已配对 | 由 `upstream_system_sku_pairing` 是否存在推导 |

表格列：

| 列名 | 说明 | 首版是否展示 |
| --- | --- | --- |
| 来源系统 | 领星 WMS、易仓 ERP 等 | 是 |
| 主仓 | 主仓接入名称和接入编号 | 是 |
| 来源 SKU | 来源系统 SKU / masterSku | 是 |
| 来源商品名 | 来源系统商品名 | 是 |
| 图片 | 来源商品主图 | 是 |
| 别名/描述 | 商品别名、描述摘要 | 是 |
| 识别码 | 主识别码、其他识别码、FNSKU | 是 |
| 分类 | 一级/二级/三级分类 | 是 |
| 尺寸重量 | 产品长宽高重 | 是 |
| 申报信息 | 申报品名、海关编码、申报价 | 是 |
| 危险品 | 危险品标识 | 是 |
| 同步状态 | ACTIVE / MISSING 等状态展示 | 是 |
| 匹配客户 | 上游系统管理里保存的客户名称快照 | 是 |
| 匹配商城商品 | 已配对的系统 SKU 和系统 SKU 名称 | 是 |
| 最近发现时间 | 最近一次同步发现该 SKU 的时间 | 是 |
| 更新时间 | 同步清单更新时间 | 是 |
| 库存 | 来源仓库存量或可售量 | 后续 |

操作列：

| 操作 | 首版处理 |
| --- | --- |
| 查看 | 打开详情抽屉，只读展示当前行信息 |
| 去上游系统管理配对 | 第二步可加跳转，首版不强制 |
| 创建商城商品草稿 | 第二步实现，首版不放或置灰 |

## 详情抽屉

首版详情只读，分三块：

### 基础信息

- 来源 SKU
- 来源商品名
- 商品别名
- 商品描述
- 产品类型
- 审核状态
- 商品图片
- 同步状态
- 首次发现时间
- 最近发现时间
- 更新时间

### 识别码与分类

- 主识别码
- 其他识别码
- FNSKU
- 一级分类
- 二级分类
- 三级分类

### 尺寸重量

- 产品长宽高重
- `lengthBs / widthBs / heightBs / weightBs`
- WMS 长宽高重
- WMS `Bs` 长宽高重

### 来源信息

- 来源系统类型
- 主仓接入名称
- 主仓接入编号
- 同步批次号

### 配对信息

- 配对状态
- 匹配客户
- 匹配商城商品 SKU
- 匹配商城商品名称

### 申报与扩展信息

- 中文申报名
- 英文申报名
- 海关编码
- 币种
- 申报价
- 原产国
- 危险品标识
- 平台 SKU 信息 JSON
- 巴西税务信息 JSON

## 表结构补充方案

### 调整对象

```text
upstream_system_sku_candidate
```

### 调整方式

当前阶段建议扩展现有表，不新建来源商品表。

落地时需要另写 SQL 执行记录，并在确认目标数据源后执行 `ALTER TABLE`。本方案保留字段设计口径，实际执行结果以 `docs/plans/2026-06-04-source-product-library-table-execution-record.md` 为准。

### 字段设计

| 字段名 | 类型建议 | 必填 | 默认值 | 领星字段 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `product_alias_name` | varchar(255) | 否 | `''` | `productAliasName` | 商品别名 |
| `approve_status` | varchar(32) | 否 | `''` | `approveStatus` | 领星审核状态 code |
| `product_type` | int | 否 | null | `type` | 产品类型 code，先保存来源值 |
| `product_description` | text | 否 | null | `productDescription` | 商品描述 |
| `image_url` | varchar(1000) | 否 | `''` | `imageUrl` | 来源商品图片 URL |
| `main_code` | varchar(128) | 否 | `''` | `mainCode` | 主识别码 / 产品条码 |
| `other_code` | varchar(1000) | 否 | `''` | `otherCode` | 其他识别码，按来源原值保存 |
| `fnsku` | varchar(1000) | 否 | `''` | `fnsku` | Amazon FNSKU，多个值按来源原值保存 |
| `product_length` | decimal(18,4) | 否 | null | `length` | 产品长度 |
| `product_width` | decimal(18,4) | 否 | null | `width` | 产品宽度 |
| `product_height` | decimal(18,4) | 否 | null | `height` | 产品高度 |
| `product_weight` | decimal(18,4) | 否 | null | `weight` | 产品重量 |
| `product_length_bs` | decimal(18,4) | 否 | null | `lengthBs` | 来源 `lengthBs`，含义沿用领星字段 |
| `product_width_bs` | decimal(18,4) | 否 | null | `widthBs` | 来源 `widthBs` |
| `product_height_bs` | decimal(18,4) | 否 | null | `heightBs` | 来源 `heightBs` |
| `product_weight_bs` | decimal(18,4) | 否 | null | `weightBs` | 来源 `weightBs` |
| `wms_length` | decimal(18,4) | 否 | null | `wmsLength` | WMS 长度 |
| `wms_width` | decimal(18,4) | 否 | null | `wmsWidth` | WMS 宽度 |
| `wms_height` | decimal(18,4) | 否 | null | `wmsHeight` | WMS 高度 |
| `wms_weight` | decimal(18,4) | 否 | null | `wmsWeight` | WMS 重量 |
| `wms_length_bs` | decimal(18,4) | 否 | null | `wmsLengthBs` | WMS `Bs` 长度 |
| `wms_width_bs` | decimal(18,4) | 否 | null | `wmsWidthBs` | WMS `Bs` 宽度 |
| `wms_height_bs` | decimal(18,4) | 否 | null | `wmsHeightBs` | WMS `Bs` 高度 |
| `wms_weight_bs` | decimal(18,4) | 否 | null | `wmsWeightBs` | WMS `Bs` 重量 |
| `declare_name_cn` | varchar(255) | 否 | `''` | `declareNameCn` | 中文申报名 |
| `declare_name_en` | varchar(255) | 否 | `''` | `declareNameEn` | 英文申报名 |
| `customhouse_code` | varchar(64) | 否 | `''` | `customhouseCode` | 海关编码 / HS Code |
| `currency_code` | varchar(16) | 否 | `''` | `currencyCode` | 申报价币种 code |
| `declare_price` | decimal(18,4) | 否 | null | `declarePrice` | 申报价 |
| `country_of_origin_name` | varchar(100) | 否 | `''` | `countryOfOriginName` | 原产国名称 |
| `dangerous_cargo` | int | 否 | null | `dangerousCargo` | 危险品标识，保存来源 code |
| `cat1_name` | varchar(100) | 否 | `''` | `cat1Name` | 一级分类名称 |
| `cat2_name` | varchar(100) | 否 | `''` | `cat2Name` | 二级分类名称 |
| `cat3_name` | varchar(100) | 否 | `''` | `cat3Name` | 三级分类名称 |
| `platform_sku_info_json` | longtext | 否 | null | `platformSkuInfoList` | 平台 SKU 信息 JSON，首版不拆子表 |
| `brazil_tax_info_json` | longtext | 否 | null | `brazilTaxInfoList` | 巴西税务信息 JSON，首版不拆子表 |
| `source_payload_json` | longtext | 否 | null | 整行 `records[]` | 来源响应行完整快照，便于后续字段追溯 |
| `source_payload_hash` | varchar(64) | 否 | `''` | 整行 hash | 用于后续判断来源快照是否变化 |

### 索引建议

保留现有索引：

- 主键：`(connection_code, master_sku)`
- `idx_upstream_sku_candidate_status(connection_code, status)`
- `idx_upstream_sku_candidate_search(connection_code, master_sku, master_product_name)`

新增索引建议：

| 索引名 | 字段 | 用途 |
| --- | --- | --- |
| `idx_upstream_sku_candidate_main_code` | `(connection_code, main_code)` | 按主识别码查询 |
| `idx_upstream_sku_candidate_fnsku` | `(connection_code, fnsku)` | 按 FNSKU 查询 |
| `idx_upstream_sku_candidate_approve` | `(connection_code, approve_status)` | 按审核状态筛选 |
| `idx_upstream_sku_candidate_category` | `(connection_code, cat1_name, cat2_name, cat3_name)` | 按来源分类筛选 |

`search_text` 同步时应追加以下字段，支持统一模糊搜索：

```text
master_sku
master_product_name
product_alias_name
main_code
other_code
fnsku
declare_name_cn
declare_name_en
customhouse_code
cat1_name
cat2_name
cat3_name
```

### 不拆子表的原因

`platformSkuInfoList`、`brazilTaxInfoList` 首版先保存 JSON，不拆子表：

- 来源商品库首版只做集中展示，不围绕平台 SKU 或巴西税务信息做独立业务流程。
- 采样中 `platformSkuInfoList` 当前为空，暂不值得新增子表。
- `brazilTaxInfoList` 属于国家/地区扩展信息，结构可能随来源系统变化。
- 如果后续需要按平台 SKU、税号、NCM code 高频筛选，再单独设计子表和索引。

### 与字典/配置的关系

- `currency_code` 后续展示优先复用财务币种配置或若依字典，不在页面硬编码币种 label。
- `approve_status`、`product_type`、`dangerous_cargo` 首版先按来源 code 展示；如果页面需要稳定中文 label，再新增或复用若依字典。
- `cat1_name / cat2_name / cat3_name` 是来源分类名称快照，不等于平台商品分类，不直接写入 `product_category`。

### 与商城商品的关系

本表只保存来源商品快照，不保存商城商品正式资料。

后续「创建商城商品草稿」时，应另行设计商城商品草稿表或正式商品表，并通过关系字段追溯来源：

```text
connection_code + master_sku
```

来源快照可以作为草稿默认值来源，但不能反向覆盖商城商品事实。

## 接口建议

首版建议新增一个只读聚合查询接口，不改变现有上游系统管理接口：

```text
GET /integration/admin/source-products/list
```

原因：

- 数据所有权在 `integration` 模块。
- 来源商品库首版本质是上游 SKU 同步清单的聚合视图。
- 避免 `product` 共享领域模块直接依赖上游系统内部 Mapper。
- 前端菜单在商品管理下不影响后端数据归属。

返回对象建议：

```text
connectionCode
systemKind
systemKindLabel
masterWarehouseName
masterSku
masterProductName
status
pairingStatus
systemSku
systemSkuName
customerName
syncBatchId
firstSeenTime
lastSeenTime
updateTime
productAliasName
approveStatus
productType
productDescription
imageUrl
mainCode
otherCode
fnsku
productLength
productWidth
productHeight
productWeight
declareNameCn
declareNameEn
customhouseCode
currencyCode
declarePrice
countryOfOriginName
dangerousCargo
cat1Name
cat2Name
cat3Name
```

权限建议：

```text
product:source:list
```

说明：当前菜单 seed 暂时还是旧占位权限 `product:list:list`。进入正式实现时，建议同步调整为 `product:source:list`，并写菜单权限 SQL 执行记录。

## 前端建议

新增页面：

```text
react-ui/src/pages/Product/SourceLibrary/index.tsx
```

新增 service：

```text
react-ui/src/services/product/sourceProduct.ts
```

新增类型：

```text
react-ui/src/types/product/source-product.d.ts
```

页面规则：

- 使用 ProTable。
- 筛选区使用 `getPersistedProTableSearch(...)`。
- 下拉选择使用 `SEARCHABLE_SELECT_PROPS`。
- 表格主数据区按现有三端列表规则撑满可视区域。
- 页面不额外写页面级标题。
- 行操作首版最多保留「查看」。

## 第二步能力

首版列表稳定后，再做以下能力：

1. 跳转到上游系统管理的 SKU 配对位置。
2. 创建商城商品草稿。
3. 显示来源商品图片、规格、条码。
4. 集成库存展示。
5. 支持按匹配客户 / 商城商品筛选。
6. 支持卖家端只读查看平台分发商品，来源显示为「系统」。

## 创建商城商品草稿的后续原则

后续如果实现「创建商城商品草稿」，应遵守：

- 只能从已配对或已确认的来源 SKU 创建。
- 结果进入商城商品草稿，不直接上架。
- 草稿带来源标识，区分于卖家独立上传商品。
- 来源 SKU 与商城商品的关系必须可追溯。
- 分类、属性、价格、库存、描述等不足信息由草稿完善流程处理。
- 涉及新增商城商品表、来源关系表或草稿状态前，必须另写表结构设计方案并确认。

## 验证标准

首版实现完成后至少验证：

- 管理端有权限用户可访问「商品管理 / 来源商品库」。
- 无权限用户不能访问接口。
- 列表分页由后端完成，不在前端一次性拉取全部 SKU。
- 能按来源系统、主仓、SKU、商品名、识别码、FNSKU、分类、审核状态、同步状态、配对状态筛选。
- 真实领星 SKU 数据可展示，至少覆盖当前已同步的 `LX-CA012` 数据。
- 不输出上游系统密钥、token 或敏感请求日志。
- 不修改上游同步清单、配对关系或商城商品数据。
- 同步后新增扩展字段能从领星 `records[]` 正确落库。
- `source_payload_json` 只保存来源商品行，不保存 appKey、authcode、appSecret 或请求级凭证。

## 本阶段不做事项

- 不新增重复来源商品表。
- 未经确认前不执行远端数据库 DDL。
- 不做自动匹配。
- 不做商城商品草稿生成。
- 不做卖家端页面。
- 不做库存事实落库。
- 不做外部系统再次同步逻辑。
