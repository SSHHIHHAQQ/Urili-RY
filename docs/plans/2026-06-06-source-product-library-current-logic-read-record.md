# 来源商品库当前逻辑阅读记录

日期：2026-06-06

## 目标

阅读「商品管理 / 来源商品库」相关代码和文档，明确当前页面、接口、数据表、同步链路和配对展示的实际逻辑。本记录只做现状梳理，不做代码和数据库改动。

## 已阅读范围

- `docs/plans/2026-06-04-source-product-library-v1-design.md`
- `docs/plans/2026-06-04-source-product-library-table-execution-record.md`
- `docs/plans/2026-06-05-source-product-library-ui-execution-record.md`
- `docs/architecture/reuse-ledger.md`
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceProductController.java`
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminUpstreamSystemController.java`
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java`
- `RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml`
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/domain/UpstreamSkuSyncItem.java`
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/domain/SourceProductItem.java`
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/domain/query/SourceProductQuery.java`
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java`
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamSystemTask.java`
- `RuoYi-Vue/sql/upstream_system_management_seed.sql`
- `RuoYi-Vue/sql/20260604_source_product_library_sku_candidate_fields.sql`
- `RuoYi-Vue/sql/20260605_source_product_library_menu_component.sql`
- `react-ui/src/pages/Product/SourceProductLibrary/index.tsx`
- `react-ui/src/pages/Product/SourceProductLibrary/SourceProductDetailDrawer.tsx`
- `react-ui/src/pages/Product/SourceProductLibrary/constants.ts`
- `react-ui/src/services/integration/sourceProduct.ts`
- `react-ui/src/services/integration/constants.ts`
- `react-ui/src/types/integration/source-product.d.ts`
- `react-ui/src/pages/UpstreamSystem/components/SkuSyncPanel.tsx`

## 当前定位

来源商品库当前是管理端只读视图，用来集中展示上游系统同步回来的来源 SKU 快照。

它不是商城商品事实源，不直接维护 `product_spu` / `product_sku`，也不直接创建商城商品草稿。商城商品正式事实源仍然是 `product_spu` 和 `product_sku`；后续如果支持从来源商品库生成商品，也应进入草稿或正式商品流程，并保留来源追溯字段。

当前首版边界：

- 只做管理端列表和详情抽屉。
- 一行代表一个来源 SKU。
- 行唯一粒度是 `connection_code + master_sku`，也就是「来源系统接入 / 主仓 + 来源 SKU」。
- SKU 配对仍由「上游系统管理」维护，来源商品库只展示配对结果。
- 不在页面内编辑来源 SKU。
- 不在页面内触发同步。
- 不在页面内维护库存、财务、商城商品事实或卖家分发流程。
- 不向前端暴露完整 `source_payload_json`，只展示快照 Hash 和 JSON 数组条数摘要。

## 菜单与权限

当前菜单 `menu_id = 2400` 指向：

```text
Product/SourceProductLibrary/index
```

当前后端接口权限是：

```text
product:list:list
```

相关接口：

```text
GET /integration/admin/source-products/list
```

前端 service 使用 `/api` 代理前缀：

```text
GET /api/integration/admin/source-products/list
```

设计文档里曾建议后续调整为 `product:source:list`，但当前落地版本仍复用 `product:list:list`，并且菜单 seed / 运行库菜单组件更新脚本也使用这个权限。

## 前端页面逻辑

页面位置：

```text
react-ui/src/pages/Product/SourceProductLibrary/index.tsx
```

页面使用 `PageContainer title={false}` 和 `ProTable`，没有额外页面标题。表格 `rowKey` 是：

```text
connectionCode + ':' + masterSku
```

列表当前展示的主要列：

- 来源仓库：来源系统 label + 主仓名称。
- 来源 SKU：`masterSku`。
- 来源商品：`masterProductName` + 别名或描述。
- 条码 / FNSKU：`mainCode` + `fnsku`。
- 审核状态：领星 `approveStatus` 的本地 label。
- 客户尺寸：产品长宽高和重量。
- 仓库尺寸：WMS 长宽高和重量。
- 同步状态：`ACTIVE` / `MISSING`。
- 配对状态：`PAIRED` / `UNASSIGNED`。
- 匹配客户：`customerName`。
- 商城商品：`systemSku` + `systemSkuName`。
- 同步时间：`lastSeenTime` + `updateTime`。
- 操作：只有「查看」。

筛选项当前保留 8 个：

- 来源系统：`systemKind`
- 仓库名称：`masterWarehouseName`
- 来源 SKU：`masterSku`
- 商品名称：前端字段 `productName`，后端查 `master_product_name` / `product_alias_name` / `product_description`
- 识别码：`identifyCodeKeyword`
- 审核状态：`approveStatus`
- 同步状态：`status`
- 配对状态：`pairingStatus`

页面通过 `cleanParams` 去掉空值，把 ProTable 的 `current` 转成后端分页参数 `pageNum`，并传 `pageSize`。

详情抽屉只使用当前行数据，不再请求详情接口。抽屉展示：

- 图片和基础商品信息。
- 来源系统、主仓和接入编号。
- 来源 SKU、商品名、别名、描述。
- 产品类型、审核状态、同步状态、配对状态。
- 识别码、FNSKU、分类。
- 客户尺寸、WMS 尺寸、英制尺寸重量。
- 申报信息。
- 商城配对信息。
- 平台 SKU 信息 JSON 条数、巴西税务信息 JSON 条数、快照 Hash。

## 后端读接口逻辑

接口入口：

```text
AdminSourceProductController.list(SourceProductQuery query)
```

执行顺序：

1. `@PreAuthorize("@ss.hasPermi('product:list:list')")` 做管理端权限校验。
2. `startPage()` 启用若依分页。
3. 调用 `IUpstreamSystemService.selectSourceProductList(query)`。
4. 返回 `TableDataInfo`，字段结构是若依常规 `code / msg / total / rows`。

Service 层逻辑：

- `normalizeSourceProductQuery` 会 trim 可选字符串。
- `systemKind` 会兼容旧值 `LINGXING_WMS` 和新值 `lingxing-wms`。
- 查询返回后，为每行补 `systemKindLabel`，当前 `lingxing-wms` 显示为「领星WMS」。

Mapper 查询：

```text
upstream_system_sku_candidate c
inner join upstream_system_connection conn on conn.connection_code = c.connection_code
left join upstream_system_sku_pairing p on p.connection_code = c.connection_code and p.master_sku = c.master_sku
```

配对状态不是来源快照表字段，而是查询时推导：

```sql
case when p.sku_pairing_id is null then 'UNASSIGNED' else 'PAIRED' end
```

查询支持：

- `connectionCode` 精确匹配。
- `systemKind` 精确匹配，且兼容 `lingxing-wms` / `LINGXING_WMS`。
- `masterWarehouseName` 模糊匹配。
- `masterSku` 模糊匹配。
- `productName` 模糊匹配商品名、别名、描述。
- `identifyCodeKeyword` 模糊匹配 `main_code`、`other_code`、`fnsku`。
- `categoryKeyword` 模糊匹配三级来源分类。
- `approveStatus` 精确匹配。
- `dangerousCargo` 精确匹配。
- `status` 精确匹配。
- `pairingStatus` 按配对表是否存在过滤。
- `keyword` 统一模糊匹配 `search_text`、接入编号、主仓名、系统 SKU、系统商品名、客户名。

排序：

```text
c.update_time desc, c.master_sku asc
```

当前 SQL 不返回 `source_payload_json`，只返回 `source_payload_hash`。

## 来源 SKU 快照落库逻辑

快照表：

```text
upstream_system_sku_candidate
```

主键：

```text
(connection_code, master_sku)
```

关键字段来源是领星 `/product/pagelist`：

- `sku` -> `master_sku`
- `productName` -> `master_product_name`
- `productAliasName`
- `approveStatus`
- `type`
- `productDescription`
- `imageUrl`
- `mainCode`
- `otherCode`
- `fnsku`
- `length` / `width` / `height` / `weight`
- `wmsLength` / `wmsWidth` / `wmsHeight` / `wmsWeight`
- `declareNameCn` / `declareNameEn`
- `customhouseCode`
- `currencyCode`
- `declarePrice`
- `countryOfOriginName`
- `dangerousCargo`
- `cat1Name` / `cat2Name` / `cat3Name`
- `platformSkuInfoList` -> `platform_sku_info_json`
- `brazilTaxInfoList` -> `brazil_tax_info_json`
- 完整单行 JSON -> `source_payload_json`
- 完整单行 JSON 哈希 -> `source_payload_hash`

同步入口有两个：

1. 管理端上游系统页面手动同步：

```text
POST /integration/admin/upstream-systems/{connectionCode}/skus/sync
```

2. Quartz 任务：

```text
upstreamSystemTask.syncSkus
```

同步服务逻辑：

- `syncSkusOnly(connectionCode)` 先校验主仓接入存在且启用。
- 使用 `syncingSkuConnectionCodes` 防止同一主仓重复同步。
- 生成 `syncBatchId`。
- 写入 `upstream_system_sku_sync_state` 为 `SYNCING`。
- 调用 `LingxingOpenApiClient.listProductSkuPage(current, 100, traceId)` 分页读取 `/product/pagelist`。
- 每页转成 `UpstreamSkuSyncItem` 后批量 `insert ... on duplicate key update` 到 `upstream_system_sku_candidate`。
- 新同步到的行标记为 `ACTIVE`，更新 `sync_batch_id`、`last_seen_time`、`update_time`。
- 同步结束后执行 `markMissingSkus`：同一 `connection_code` 下 `sync_batch_id` 不是当前批次的旧行标记为 `MISSING`。
- 成功后同步状态写成 `FRESH`，记录最近成功时间和下次同步时间；失败则写成 `FAILED` 和错误信息。

## 配对逻辑来源

SKU 配对表：

```text
upstream_system_sku_pairing
```

关键约束：

- `uk_upstream_sku_pairing_master (connection_code, master_sku)`
- `uk_upstream_sku_pairing_system (connection_code, system_sku)`

因此当前一个来源 SKU 只能配一个系统 SKU，同一主仓接入下一个系统 SKU 也只能配一个来源 SKU。

配对维护入口不在来源商品库，而在上游系统管理的 SKU 同步清单：

- `GET /integration/admin/upstream-systems/{connectionCode}/skus/list`
- `GET /integration/admin/upstream-systems/{connectionCode}/sku-pairings`
- `POST /integration/admin/upstream-systems/{connectionCode}/sku-pairings`
- `DELETE /integration/admin/upstream-systems/sku-pairings/{skuPairingId}`

新增配对时，Service 会先查来源 SKU 是否存在于同步清单；不存在则拒绝并提示「领星SKU不在同步清单中，请先同步SKU」。

配对写入后会记录 `PAIR` 审计事件；解除配对后会记录 `UNPAIR` 审计事件。

来源商品库只通过左连接展示：

- `pairingStatus`
- `skuPairingId`
- `systemSku`
- `systemSkuName`
- `customerName`

## 选项与字典复用

当前前端共享在：

```text
react-ui/src/services/integration/constants.ts
```

已共享：

- 来源系统选项：`systemKindOptions`
- 来源系统 label：`systemKindText`
- 同步状态：`syncItemStatusText`
- 配对状态：`pairingStatusText`

来源商品库页面内仍本地维护：

- 领星审核状态：`approveStatusText`
- 领星危险品：`dangerousCargoText`
- 领星产品类型：`productTypeText`

复用台账记录的口径是：如果这些状态后续跨页面复用，再上移到共享选项或若依字典。

## 当前验证记录

历史执行记录显示：

- `mvn -pl integration -am -DskipTests package` 通过。
- `npm run tsc` 曾在首版页面落地时通过。
- `mvn -DskipTests package` 曾通过。
- 浏览器验证曾确认 `/product/list` 加载来源商品库，总数 `5401` 条，详情抽屉可打开。
- 后续表格交互修复时，`npx biome lint ...SourceProductLibrary...` 通过。
- 后续一次 `npm run tsc` 被无关文件 `src/pages/Product/Attribute/components/CategoryAttributeTemplate.tsx` JSX 标签未闭合阻断。

本次阅读未重新启动服务、未访问数据库、未执行接口请求、未跑编译测试。

## 当前结论

来源商品库当前逻辑可以概括为：

```text
领星 /product/pagelist
  -> UpstreamSystemServiceImpl 同步并 upsert 来源 SKU 快照
  -> upstream_system_sku_candidate 保存来源商品快照
  -> 上游系统管理维护 upstream_system_sku_pairing
  -> 来源商品库 GET /integration/admin/source-products/list 只读聚合展示
```

它当前不是商品创建入口，也不是配对维护入口。它的核心价值是把上游同步快照、来源主仓、同步状态和配对结果放到一个管理端列表里查询和查看详情。

## 后续改动注意点

- 不要新增第二张来源商品事实表，除非先写表设计方案并确认。
- 不要把 `upstream_system_sku_candidate` 当成商城商品正式主数据。
- 不要在来源商品库页面内复制 SKU 配对规则；配对仍应走上游系统管理的接口、唯一约束和审计。
- 如果新增导出、批量创建草稿、同步库存等操作，需要新增按钮权限、审计日志和验证记录。
- 如果要从来源商品库生成商城商品，必须另写商品草稿/正式商品来源关系设计，不得直接覆盖 `product_spu` / `product_sku`。
- 如果要展示或筛选更多来源字段，优先确认字段是否已经在 `upstream_system_sku_candidate` 落库；复杂数组字段目前仍是 JSON 快照，不应直接拆业务流程。
- 如果要暴露更完整详情，避免把 `source_payload_json` 原样返回前端；当前只返回 hash 是有意控制敏感面。
- 如果要调整权限，需同步菜单权限、后端 `@PreAuthorize`、前端访问控制和 SQL 执行记录。
