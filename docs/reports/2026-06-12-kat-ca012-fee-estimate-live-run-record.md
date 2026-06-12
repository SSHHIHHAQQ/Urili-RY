# KAT-F-CA012 报价方案链路实跑记录

记录时间：2026-06-12 19:20-19:31

## 执行边界

本次按用户确认走完整链路，所有数据动作都通过当前后端管理端 HTTP API 执行，没有直接写 SQL，也没有输出任何上游凭证、token 或请求明文。

## 测试商品

上游履约仓：`LX-KAT库存仓-58F5A0B6 / US015 / FULFILLMENT`

报价仓：`LX-KAT-91B1E277 / CA012 / QUOTE`

系统仓库：`KAT-F-CA012`，仓库 ID `8`，币种 `USD`

| SPU ID | SKU ID | 系统 SKU | 卖家 SKU | 来源 masterSku | 来源库存 |
| --- | --- | --- | --- | --- | --- |
| `63` | `158` | `UKR3NFDQ584L` | `KAT-CA012-20260612192044-CATBOOK-SKU` | `CESHIAL0008` | `147` |
| `64` | `159` | `UKR3NQYQG4IA` | `KAT-CA012-20260612192044-STICKY-SKU` | `CESHIAL0012` | `96` |
| `65` | `160` | `UKR3O2JQR0VZ` | `KAT-CA012-20260612192044-FOLDER-SKU` | `CESHIAL0019` | `119` |

验证结果：

- `/product/admin/distribution-products/skus/list?sourceWarehouseCode=KAT-F-CA012` 能返回这 3 个 SKU。
- `/finance/admin/fee-estimate/skus/list?sourceWarehouseCode=KAT-F-CA012&skuCode=UKR3` 能返回这 3 个 SKU，并带出尺寸和重量。
- `/inventory/admin/overview/sku/{skuId}/warehouses` 能返回对应库存行，来源仓有可用库存。

## 库存同步

创建商品后，平台库存最初是 `0`，来源库存有数量。已通过库存总览同步策略接口补齐：

- `POST /inventory/admin/overview/sync-policy/preview`
- `POST /inventory/admin/overview/sync-policy/confirm`

| SKU ID | stockId | 变更前模式 | 变更后模式 | 平台可用库存 |
| --- | --- | --- | --- | --- |
| `158` | `3132` | `MANUAL` | `AUTO_SOURCE_AVAILABLE` | `147` |
| `159` | `3138` | `MANUAL` | `AUTO_SOURCE_AVAILABLE` | `96` |
| `160` | `3143` | `MANUAL` | `AUTO_SOURCE_AVAILABLE` | `119` |

## 报价方案

- `schemeId=1`
- 方案名称：`测试1`
- 方案类型：`BILLING`
- 费用来源：`EXTERNAL_ESTIMATE`
- 绑定仓库：`KAT-F-CA012`
- 客户渠道：`UPS-S`、`USPS`、`USPS-SF`

第一次自动追优试算：

- 请求入口：`POST /finance/admin/fee-estimate/calculate`
- 请求模式：`estimateView=BUYER_SIMULATION`，`selectionMode=AUTO_BEST`，`packageInputMode=SKU`
- 买家：`buyerId=36`
- 仓库限制：`KAT-F-CA012`
- 目的地：美国 CA / Los Angeles / 90001
- SKU：`skuId=158`，数量 `1`

第一次结果没有进入外部试算，原因是渠道配置未形成可执行候选：

| 客户渠道 | 系统渠道 | 结果 |
| --- | --- | --- |
| `UPS-S` | 无 | `CUSTOMER_CHANNEL_BUYER_BLOCKED`，当前买家被排除 |
| `USPS` | `USPS` | `SYSTEM_CHANNEL_WAREHOUSE_MISSING`，系统渠道未绑定 `KAT-F-CA012` |
| `USPS` | `USPS-Z` | `SYSTEM_CHANNEL_WAREHOUSE_MISSING`，系统渠道未绑定 `KAT-F-CA012` |
| `USPS-SF` | 无 | `CUSTOMER_LABEL_REQUIRED`，自动最优不使用客户上传面单渠道 |

## 补齐的绑定

### 系统渠道仓库绑定

接口：`POST /logistics/admin/system-channels/USPS-Z/warehouses`

新增绑定：

- `bindingId=7`
- 系统渠道：`USPS-Z`
- 仓库：`warehouseId=8 / KAT-F-CA012`
- 地址模式：`WAREHOUSE`
- 状态：`ENABLED`

### 上游报价仓物流渠道配对

接口：`POST /integration/admin/upstream-systems/LX-KAT-91B1E277/logistics-channel-pairings`

新增配对：

- `logisticsChannelPairingId=5`
- 配对角色：`QUOTE`
- 上游渠道：`CA012-USPS-WEST`
- 系统渠道：`USPS-Z`
- 状态：`ACTIVE`

## 再次试算

第二次候选解析已打通：

- `warehouseCandidateCount=1`
- `quoteSchemeCandidateCount=1`
- `customerChannelCandidateCount=3`
- `routeCandidateCount=4`
- `executableRouteCount=1`

唯一可执行候选：

| 仓库 | 客户渠道 | 系统渠道 | 履约模式 | 是否可执行 |
| --- | --- | --- | --- | --- |
| `KAT-F-CA012` | `USPS` | `USPS-Z` | `DIRECT_FULFILLMENT_WAREHOUSE` | 是 |

试算最终结果：

| 渠道 | 系统渠道 | 结果 |
| --- | --- | --- |
| `UPS-S` | 无 | `CUSTOMER_CHANNEL_BUYER_BLOCKED` |
| `USPS` | `USPS` | `SYSTEM_CHANNEL_WAREHOUSE_MISSING` |
| `USPS` | `USPS-Z` | `LINGXING_ESTIMATE_ENDPOINT_UNCONFIGURED` |
| `USPS-SF` | 无 | `CUSTOMER_LABEL_REQUIRED` |

当前链路已经走到上游试算适配器。阻塞点只剩一个：`LingxingFinanceFeeEstimateExternalServiceImpl` 仍要求 `URILI_LINGXING_FEE_ESTIMATE_PATH`，当前环境未配置，所以没有真正发起领星费用试算 HTTP 请求。

## 结论

已打通：

- KAT-F-CA012 报价方案仓库绑定。
- 履约仓来源商品选择、商品创建、SKU 列表查询。
- 来源仓库存到平台库存的同步策略。
- 报价方案候选仓库解析。
- 客户渠道到系统渠道候选解析。
- 系统渠道仓库绑定。
- QUOTE 报价仓仓库配对。
- QUOTE 报价仓物流渠道配对。

未打通：

- 领星费用试算真实 HTTP 调用。原因不是仓库、SKU、报价方案或渠道映射，而是当前代码仍缺领星运费试算 endpoint path。

补充核对：

- 已查领星 WMS 官方 API 文档索引 `https://apidoc-omp.xlwms.com/llms.txt`，公开 OpenAPI 索引里没有列出运费试算 endpoint。
- 已查领星帮助文档“使用OMP实现运费试算”，该文档说明 OMP 后台有运费试算页面，但没有提供 OpenAPI endpoint；文档还说明引用物流商报价的试算限制在 OMS 单据内。

后续要继续真实接入，必须二选一：

1. 拿到领星官方运费试算 OpenAPI path 和字段后，把 `LingxingOpenApiClient` 固定成正式方法，不再依赖临时环境变量。
2. 如果这个 endpoint 不是公开 OpenAPI，而是客户专属配置，就需要把“费用试算 endpoint 配置”纳入上游系统管理。该方案会涉及新增字段或配置项，按项目规则需要先单独确认数据设计。
