# 外部费用试算链路验证记录

记录时间：2026-06-12

## 验证范围

本次只验证两条外部费用链路是否能返回可用费用结果：

1. 物流商侧运费试算：管理端物流商账号 `carrierAccountId=1`，接入方 `AGG56`，接口入口 `/logistics/admin/carriers/quote`。
2. 领星/上游侧费用试算：管理端费用试算入口 `/finance/admin/fee-estimate/calculate`，费用来源模式 `EXTERNAL_ESTIMATE`。

本次调用没有输出任何外部系统凭证明文、token 或请求体明细。物流商授权、渠道同步、报价调用会按现有系统逻辑写入脱敏外部请求日志；渠道同步接口会刷新物流商渠道候选数据。

## AGG56 物流商运费试算

### 当前配置

| 项目 | 结果 |
| --- | --- |
| 物流商账号 | `carrierAccountId=1` |
| 接入方 | `AGG56` |
| 账号状态 | `ENABLED` |
| 凭证状态 | `CONFIGURED` |
| API Base URL | `https://www.agg56.com` |
| 渠道映射 | `UPS -> UPS-Ground/JQ3`、`USPS -> USPS GAS` |

### 实测结果

| 测试项 | 结果 |
| --- | --- |
| AGG56 授权校验 `/authorize` | 成功，说明当前凭证可以换取 AGG56 access token |
| AGG56 物流产品同步 `/channels/sync` | 成功，说明 access token 可用于至少一个业务接口 |
| AGG56 报价 `UPS` | 本地接口 HTTP 200，业务 code 500，AGG56 外部 HTTP 403 |
| AGG56 报价 `USPS` | 本地接口 HTTP 200，业务 code 500，AGG56 外部 HTTP 403 |

结论：AGG56 账号授权和渠道同步是通的，但运费试算 `/api/svc/rates` 当前没有返回费用结果，而是被 AGG56 返回 HTTP 403。这个 403 不像是本地账号凭证错误，因为授权和渠道同步都成功；更像是 AGG56 侧对 `rates` 接口权限、账号可用能力、IP 白名单、接口开通状态或报价参数有额外限制。

另外发现一个独立问题：`GET /logistics/admin/carriers/{carrierAccountId}/request-logs/list` 当前会触发分页 SQL 拼接错误，表现为内部连接查询 `limit 1` 被分页插件额外追加 `LIMIT ?`。这不影响本次外部报价调用，但会影响后续从页面/API 查看外部请求日志。

## 领星/上游费用试算

### 当前配置

| 项目 | 结果 |
| --- | --- |
| 计费报价方案 | `schemeId=1`，名称 `测试1` |
| 方案类型 | `BILLING` |
| 费用来源模式 | `EXTERNAL_ESTIMATE` |
| 方案绑定仓库 | `KAT-F-CA012` |
| 自动试算样本 SKU | `skuId=49`，候选仓库 `NY013` |
| 多仓样本 SKU | `skuId=140`，候选仓库 `CA012 / NY013` |
| 领星费用试算 path | `.env.local` 未配置 `URILI_LINGXING_FEE_ESTIMATE_PATH` |

### 实测结果

| 测试项 | 结果 |
| --- | --- |
| `buyerId=3 + skuId=49 + 自动最优` | 失败在内部候选解析，`QUOTE_SCHEME_MISSING`，因为 SKU 候选仓库是 `NY013`，报价方案绑定的是 `KAT-F-CA012` |
| `skuId=140 + 自动最优` | 失败在内部候选解析，`QUOTE_SCHEME_MISSING`，因为 SKU 候选仓库是 `CA012 / NY013`，报价方案绑定的是 `KAT-F-CA012` |
| `skuId=140 + 手动 CA012 + UPS-S` | 失败在内部候选解析，`QUOTE_SCHEME_MISSING`，因为方案绑定值不是 `CA012` |
| `skuId=140 + 手动 KAT-F-CA012 + UPS-S` | 失败在 SKU 仓库校验，`指定仓库不在订单 SKU 的共同可发仓库内：KAT-F-CA012` |

结论：领星/上游费用试算当前还没有真正走到外部接口。阻断点有两个：

1. 仓库编码不统一：商品/SKU 候选仓库使用 `CA012` / `NY013`，报价方案绑定使用 `KAT-F-CA012`。这会导致报价方案匹配失败。
2. 外部费用试算接口 path 未配置：即使仓库匹配打通，当前环境也会在调用领星前返回 `LINGXING_ESTIMATE_ENDPOINT_UNCONFIGURED`。

## 外部调用组件设计

### 物流商侧

物流商外部调用现在放在 `logistics` 模块：

- 管理端入口：`AdminLogisticsCarrierController`
- 业务服务：`ILogisticsCarrierService` / `LogisticsCarrierServiceImpl`
- AGG56 HTTP 客户端：`logistics.agg56.Agg56OpenApiClient`
- 脱敏请求日志：`logistics_carrier_request_log`
- 凭证解密：通过 `SecretCipherSupport`，不在 Controller 里处理明文

这条链路不是写死在前端或 Controller 里的，已经有独立物流商模块和 AGG56 客户端。但当前 `LogisticsCarrierServiceImpl` 里仍然直接识别 `AGG56`，后续接第二家物流商时，建议再抽一层 `LogisticsCarrierProviderAdapter` 和 provider registry，避免 Service 继续堆 `if AGG56`。

### 领星/上游侧

领星费用试算按端口/适配器方式拆分：

- `finance` 模块只定义端口：`FinanceFeeEstimateExternalService`
- `finance` 的 `FeeEstimateServiceImpl` 只负责候选解析、方案匹配和调用端口
- `integration` 模块实现领星适配：`LingxingFinanceFeeEstimateExternalServiceImpl`
- 领星 HTTP 客户端：`integration.lingxing.LingxingOpenApiClient`
- 客户端工厂和请求日志：`UpstreamLingxingClientFactory`，日志落 `upstream_request_log`

这条边界是规范的：finance 不直接依赖领星 HTTP 细节，integration 负责外部系统调用、凭证解密、请求日志和上游配对校验。

## 下一步建议

1. 先统一仓库编码来源：报价方案绑定仓库应和 SKU 候选仓库使用同一套 code。当前至少要把 `KAT-F-CA012` 与 `CA012` 的关系整理清楚，不能一条链路用展示/报价仓编码，另一条链路用系统仓编码。
2. 配置真实领星费用试算 path：补 `URILI_LINGXING_FEE_ESTIMATE_PATH`，并确认领星费用试算请求/响应字段。
3. 找 AGG56 确认 `/api/svc/rates` 的 403 原因：重点确认账号是否开通报价权限、是否有 IP 白名单、`rates` 是否需要额外字段或不同 header。
4. 修复物流商请求日志列表分页 bug，方便后续从后台直接查看外部调用轨迹。

## 请求日志分页 bug 修复

修复时间：2026-06-12

### 问题

`GET /logistics/admin/carriers/{carrierAccountId}/request-logs/list?pageNum=1&pageSize=10` 原先会返回 SQL 语法错误。根因是 Controller 先执行 `startPage()`，Service 里的 `selectRequestLogList` 又先调用 `selectConnectionByAccountId` 做连接校验，PageHelper 把分页 `LIMIT ?` 套到了这次连接校验 SQL 上，形成 `limit 1 LIMIT ?`。

### 修复

- `LogisticsCarrierServiceImpl.selectRequestLogList` 不再在分页上下文里提前查询连接详情，只执行真正的请求日志列表查询。
- 新增合同测试 `carrierRequestLogListMustKeepPageHelperOnLogQuery`，固定请求日志列表方法不能再先调用 `selectConnectionByAccountId` 或 `requireProviderConnection`。
- 顺手补齐 `AdminFeeEstimateController` 缺失的 `RequestParam` import，避免 Maven reactor 编译被现有未跟踪文件中的漏 import 阻断。

### 验证

- `mvn -pl logistics -am "-Dtest=LogisticsAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，13 tests。
- `mvn -pl ruoyi-admin -am -DskipTests package`：通过。
- 本地后端重启后，请求 `GET /logistics/admin/carriers/1/request-logs/list?pageNum=1&pageSize=10`：返回 `code=200`、`total=13`、`rowCount=10`，不再出现双 `LIMIT` SQL 错误。
