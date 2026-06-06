# 上游系统同步 staging 差异合并执行记录

## 目标

按已确认方案重构上游系统管理同步能力：

- 仓库、物流渠道、SKU 信息、SKU 仓库尺寸重量拆分同步。
- 全量同步先写 staging，再按 payload hash 做数据库差异合并。
- hash 未变化的数据不更新主表业务字段和 `update_time`。
- 上游全量快照消失的数据标记为 `DISABLED`，不直接删除。
- SKU 仓库尺寸重量改为每日 23:59 限速同步，并支持指定 SKU 快速获取。
- SKU 库存保留 10 分钟一次的上游库存快照同步，不写平台可售库存。
- 定时任务统一登记在若依 `sys_job`，可在“系统监控 / 定时任务”查看和启停。

## 代码改动

- 后端：
  - 新增分项同步请求、分项同步结果、同步状态和同步批次领域对象。
  - `AdminUpstreamSystemController` 的顶部 `/sync` 支持 `syncTypes`，并按同步项做后端权限校验。
  - `UpstreamSystemServiceImpl` 将仓库、物流渠道、SKU 信息、SKU 仓库尺寸重量改为 staging 写入后差异合并。
  - `LingxingOpenApiClient` 复用 `HttpClient`，请求日志 `operation` 区分授权、仓库、物流、SKU、尺寸重量、库存。
  - `UpstreamSystemTask` 拆成 `syncWarehouses`、`syncLogisticsChannels`、`syncSkuInfo`、`syncSkuDimensions`、`syncInventory`。
- 前端：
  - 顶部“同步”改为 Modal 勾选同步内容。
  - Tabs 顺序保持为仓库、物流渠道、SKU、仓库尺寸重量、SKU库存、请求日志。
  - 仓库尺寸重量页新增“指定SKU获取”。
  - 请求日志类型显示中文业务类型。
- SQL：
  - 新增 `20260606_upstream_sync_staging_diff.sql`。
  - 更新 `upstream_system_management_seed.sql` 的库存权限 seed。
- 文档：
  - 更新 `docs/architecture/reuse-ledger.md`。

## 数据库执行记录

- 目标环境：后端当前激活 `druid` 数据源，远端 MySQL `fenxiao` 库。
- 连接来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`。
- 执行类型：DDL + DML，会影响远端业务库结构、菜单权限和若依定时任务配置。
- 执行脚本：
  - `RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql`
  - `RuoYi-Vue/sql/20260606_upstream_sync_staging_diff.sql`
- 确认变量：
  - `@confirm_upstream_inventory_dimension_sync = APPLY_UPSTREAM_INVENTORY_DIMENSION_SYNC`
  - `@confirm_upstream_sync_staging_diff = APPLY_UPSTREAM_SYNC_STAGING_DIFF`

落库核查结果：

| 类型 | 名称 |
| --- | --- |
| 表 | `upstream_system_sync_state` |
| 表 | `upstream_system_sync_batch` |
| 表 | `upstream_system_warehouse_candidate_stage` |
| 表 | `upstream_system_logistics_channel_candidate_stage` |
| 表 | `upstream_system_sku_candidate_stage` |
| 表 | `upstream_system_sku_dimension_stage` |
| 表 | `upstream_system_sku_inventory_snapshot` |

若依定时任务核查结果：

| 任务 | 调用目标 | cron | 状态 | 并发 |
| --- | --- | --- | --- | --- |
| 领星仓库每日同步 | `upstreamSystemTask.syncWarehouses` | `0 20 23 * * ?` | 启用 | 禁止并发 |
| 领星物流渠道每日同步 | `upstreamSystemTask.syncLogisticsChannels` | `0 30 23 * * ?` | 启用 | 禁止并发 |
| 领星SKU信息每日同步 | `upstreamSystemTask.syncSkuInfo` | `0 40 23 * * ?` | 启用 | 禁止并发 |
| 领星SKU仓库尺寸重量每日限速同步 | `upstreamSystemTask.syncSkuDimensions` | `0 59 23 * * ?` | 启用 | 禁止并发 |
| 领星SKU库存每10分钟同步 | `upstreamSystemTask.syncInventory` | `0 0/10 * * * ?` | 启用 | 禁止并发 |

## 验证命令

| 命令 | 结果 |
| --- | --- |
| `mvn -pl integration -am -DskipTests compile` | 通过 |
| `npm run tsc` | 通过 |
| `mvn -DskipTests package` | 通过，生成 `ruoyi-admin.jar` |
| `.\start-backend-local.ps1 -Restart` | 已重启 |
| `Invoke-WebRequest http://127.0.0.1:8080` | HTTP 200 |
| Playwright 打开 `/overseas-warehouse-service/upstream-system` | 主仓、SKU、尺寸重量、库存、请求日志、同步 Modal 均可见 |

## 权限检查

- 顶部 `/sync` 动态校验权限：
  - 仓库、物流渠道、SKU 信息：`integration:upstream:sync`
  - SKU 仓库尺寸重量：`integration:upstream:dimensionSync`
  - SKU 库存：`integration:upstream:inventorySync`
- 前端按权限隐藏同步选项，但以后端校验为准。
- 库存查看使用 `integration:upstream:inventoryQuery`。

## 字典和选项复用

- 同步类型、请求日志 operation 文案继续集中维护在 `react-ui/src/services/integration/constants.ts`。
- 页面状态渲染继续复用 `statusTag(...)`。
- 上游删除状态统一使用 `DISABLED`；保留 `MISSING` 展示兼容旧数据。

## 残留风险

- Playwright 使用本机 Chrome channel 验证，未下载 Playwright 自带浏览器。
- 未在本次记录中执行真实领星全量尺寸重量任务，避免再次触发长时间限速同步。
- `sys_job` cron 按数据库和应用当前 `Asia/Shanghai` / `GMT+8` 配置理解为北京时间；若服务器部署到非北京时间系统时区，需要额外确认 Quartz 运行时 timezone。
- CodeGraph 已执行 `codegraph sync .`，结果为 `Already up to date`。
