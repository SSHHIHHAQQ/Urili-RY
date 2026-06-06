# 上游系统同步功能性能化拆分计划
生成时间：2026-06-06

## 背景结论

当前上游系统管理的同步设计过重，尤其是 `领星SKU每10分钟同步`：

- 顶部“同步”接口 `/sync` 当前会一次执行授权校验、仓库、物流渠道、SKU 信息同步。
- SKU 信息同步内部又顺带执行 SKU 仓库尺寸重量同步。
- SKU 仓库尺寸重量同步会按 `skuList` 再请求领星 SKU 列表接口，属于额外外部 HTTPS 请求。
- 当前 `LingxingOpenApiClient` 每次构造都会创建新的 JDK `HttpClient`，配合 10 分钟定时任务会放大 CPU 和线程压力。
- 服务器 CPU 会比本地弱，因此不能再把大同步放在高频任务里，也不能允许多个上游同步互相叠加。

## 设计目标

1. 仓库、物流渠道、SKU 信息、SKU 仓库尺寸重量、SKU 库存全部拆成独立同步能力。
2. 顶部“同步”按钮改为弹窗选择同步项，不再默认一键全量。
3. SKU 仓库尺寸重量全量同步单独作为慢任务，每天北京时间 23:59 执行一次。
4. SKU 仓库尺寸重量支持指定 SKU 快速获取，用于紧急少量 SKU。
5. SKU 库存仍按 10 分钟同步，但要和长任务互斥，不能抢弱服务器资源。
6. 请求日志必须能直观看出请求类型，例如“SKU仓库尺寸重量”。
7. 所有定时任务继续使用若依 `sys_job` / Quartz，不自造调度器。

## 当前需要拆掉的问题

### 1. `/sync` 过于宽泛

现状：

- `syncAll(connectionCode)` 会同步仓库、物流渠道、SKU。
- `syncSkus(...)` 内部又调用 `syncSkuDimensions(...)`。

问题：

- 用户以为只是普通同步，实际可能触发大量外部请求。
- 后台定时任务和人工点击可能互相叠加。
- 请求日志虽然有 `SKU_DIMENSION_SYNC`，但页面显示的是原始 code，不够直观。

调整：

- `/sync` 改为接收同步项列表。
- SKU 信息和 SKU 仓库尺寸重量彻底拆开。
- 人工同步按勾选项顺序执行，并返回每个同步项的结果。

## 新同步项设计

| 同步项 | 业务含义 | 外部请求类型 | 默认频率 | 是否允许人工点 |
| --- | --- | --- | --- | --- |
| 仓库 | 拉取领星主仓仓库清单 | `WAREHOUSE_SYNC` | 每天 23:20，低频 | 允许 |
| 物流渠道 | 拉取领星主仓物流渠道清单 | `LOGISTICS_CHANNEL_SYNC` | 每天 23:30，低频 | 允许 |
| SKU信息 | 拉取领星 SKU 基础资料，不包含 WMS 尺寸重量补拉 | `SKU_SYNC` | 每天 23:40，低频 | 允许 |
| SKU仓库尺寸重量 | 按本地 SKU 缓存分批调用 `skuList` 查询 WMS 尺寸重量 | `SKU_DIMENSION_FULL_SYNC` | 每天 23:59，限速慢跑 | 允许，但必须提示耗时 |
| 指定SKU仓库尺寸重量 | 用户输入或选择少量 SKU，快速获取 WMS 尺寸重量 | `SKU_DIMENSION_SELECTED_SYNC` | 不定时 | 允许 |
| SKU库存 | 拉取上游仓库库存快照 | `INVENTORY_SYNC` | 每 10 分钟 | 允许 |
| 授权校验 | 校验 Key/Secret 是否可用 | `AUTH_CHECK` | 不定时 | 允许 |

说明：

- “SKU仓库尺寸重量”是重任务，不再跟随 SKU 信息同步自动执行。
- “指定SKU仓库尺寸重量”是急用功能，只处理用户指定的少量 SKU。
- “SKU库存”是上游库存源头，需要保持 10 分钟，但必须加锁和失败熔断。

## 定时任务计划

继续落到若依“系统监控 / 定时任务”菜单中，通过 `sys_job` 管理。

| 任务名称 | 调用目标 | cron | 并发 | 策略 |
| --- | --- | --- | --- | --- |
| 领星仓库每日同步 | `upstreamSystemTask.syncWarehouses` | `0 20 23 * * ?` | 禁止并发 | 低频小任务 |
| 领星物流渠道每日同步 | `upstreamSystemTask.syncLogisticsChannels` | `0 30 23 * * ?` | 禁止并发 | 依赖本地仓库缓存 |
| 领星SKU信息每日同步 | `upstreamSystemTask.syncSkuInfo` | `0 40 23 * * ?` | 禁止并发 | 只同步基础 SKU |
| 领星SKU仓库尺寸重量每日限速同步 | `upstreamSystemTask.syncSkuDimensions` | `0 59 23 * * ?` | 禁止并发 | 分批、限速、可长时间运行 |
| 领星SKU库存每10分钟同步 | `upstreamSystemTask.syncInventory` | `0 0/10 * * * ?` | 禁止并发 | 10分钟，但遇到同主仓重任务跳过 |

执行策略：

- 使用服务器本地时区执行，当前工程按北京时间运行；执行记录里要写明北京时间。
- `misfire_policy` 使用“不立即补跑”，避免服务器恢复后突发请求。
- `concurrent` 保持禁止并发。
- Service 层增加同一主仓的统一同步锁，避免手动同步和定时同步同时打同一个主仓。
- 库存任务如果遇到同主仓正在跑尺寸重量全量同步，当前轮跳过，不作为异常打爆任务日志。

## 限速设计

### SKU仓库尺寸重量每日全量

建议默认参数：

- 每批 SKU：50 个。
- 每批之间 sleep：2000 ms。
- 单次请求超时：10 秒。
- 失败重试：最多 1-2 次，只对网络错误、429、5xx 重试。
- 业务错误，例如无权限，立即失败并进入熔断，不继续刷接口。
- 单个主仓串行执行，多个主仓也串行执行。

按 5400 个 SKU 粗算：

- 50 个一批约 108 次请求。
- 仅限速等待约 216 秒。
- 加上网络耗时，预计数分钟到十几分钟内完成。
- 这个速度比本地快跑慢很多，更适合弱服务器。

### 指定SKU仓库尺寸重量

建议限制：

- 单次最多 100 个 SKU。
- 前端支持粘贴多行、逗号、空格分隔。
- 后端去重、去空、校验最大数量。
- 请求类型写 `SKU_DIMENSION_SELECTED_SYNC`。
- 成功后只刷新相关 SKU 的尺寸重量，不触发全量同步。

## 后端接口调整

### 1. 顶部同步接口

保留路径：

```text
POST /api/integration/admin/upstream-systems/{connectionCode}/sync
```

请求体建议：

```json
{
  "syncTypes": [
    "WAREHOUSE",
    "LOGISTICS_CHANNEL",
    "SKU",
    "SKU_DIMENSION",
    "INVENTORY"
  ]
}
```

执行顺序：

1. 仓库
2. 物流渠道
3. SKU信息
4. SKU仓库尺寸重量
5. SKU库存

规则：

- 没勾选的不同步。
- 勾选物流渠道但本地没有仓库缓存时，提示先同步仓库。
- 勾选 SKU 仓库尺寸重量但本地没有 SKU 缓存时，提示先同步 SKU 信息。
- 返回每个同步项的数量和状态，前端用于展示结果。

### 2. 指定SKU仓库尺寸重量接口

新增路径：

```text
POST /api/integration/admin/upstream-systems/{connectionCode}/sku-dimensions/sync-selected
```

请求体建议：

```json
{
  "skuList": [
    "KATGJ-SS-B885",
    "10511538"
  ]
}
```

规则：

- 后端最多接受 100 个 SKU。
- 用领星 `product/pagelist` 的 `skuList` 口径查询。
- 查到的 SKU 如果本地已有，则更新 WMS 尺寸重量。
- 查到的 SKU 如果本地不存在，建议插入最小 SKU 候选记录，避免紧急 SKU 查完后页面仍不可见。

## Service 拆分

建议拆出明确方法：

```text
syncWarehousesOnly(connectionCode)
syncLogisticsChannelsOnly(connectionCode)
syncSkuInfoOnly(connectionCode)
syncSkuDimensionsOnly(connectionCode)
syncSkuDimensionsBySkuList(connectionCode, skuList)
syncWarehouseStocksOnly(connectionCode)
syncSelectedTypes(connectionCode, syncTypes)
```

关键变化：

- `syncSkuInfoOnly` 不再调用 `syncSkuDimensions`。
- `syncSkuDimensionsOnly` 只从本地 SKU 缓存读取 SKU，然后按 `skuList` 分批补 WMS 尺寸重量。
- `syncSelectedTypes` 只负责按顺序编排，不把业务逻辑写在 Controller。

## 请求日志调整

当前日志表 `upstream_system_request_log` 已经能记录：

- `operation`
- `endpoint`
- `request_time`
- `response_time`
- `duration_ms`
- 脱敏请求体
- 脱敏响应体
- 外部错误码和错误信息

不建议为了显示中文类型新增日志字段，优先用 `operation` 映射中文标签。

前端展示改为：

| 当前列 | 调整后 |
| --- | --- |
| 操作 | 类型，显示中文，例如“SKU仓库尺寸重量” |
| 结果 | 保留 |
| 耗时(ms) | 保留 |
| 错误码 | 默认可隐藏或窄列 |
| 错误信息 | 保留 |
| TraceId | 保留复制 |
| 请求体/响应体 | 不默认占表格列，放到“查看详情”弹窗 |

类型映射：

| operation | 页面显示 |
| --- | --- |
| `AUTH_CHECK` | 授权校验 |
| `WAREHOUSE_SYNC` | 仓库 |
| `LOGISTICS_CHANNEL_SYNC` | 物流渠道 |
| `SKU_SYNC` | SKU信息 |
| `SKU_DIMENSION_SYNC` | SKU仓库尺寸重量 |
| `SKU_DIMENSION_FULL_SYNC` | SKU仓库尺寸重量 |
| `SKU_DIMENSION_SELECTED_SYNC` | 指定SKU仓库尺寸重量 |
| `INVENTORY_SYNC` | SKU库存 |

## 前端调整

### 1. 顶部“同步”按钮

点击后打开 Modal。

复选框：

- 仓库
- 物流渠道
- SKU信息
- SKU仓库尺寸重量
- SKU库存

建议默认：

- 默认勾选仓库、物流渠道。
- SKU信息、SKU仓库尺寸重量、SKU库存由用户主动勾选，避免误触发重任务。

提交后：

- 调用新的 `/sync` 请求体。
- 成功后刷新左侧主仓摘要和当前 Tabs。
- 结果提示按同步项分别展示，不只显示仓库/渠道/SKU 三个数字。

### 2. SKU仓库尺寸重量 Tab

新增按钮：

- “指定SKU获取”

交互：

- 支持选中表格行后带入 SKU。
- 支持粘贴 SKU 列表。
- 最多 100 个。
- 成功后刷新尺寸重量列表和请求日志。

### 3. 请求日志 Tab

调整为更简单的表格：

- 时间
- 类型
- 结果
- 耗时
- 错误信息
- TraceId
- 查看详情

默认按时间倒序。

## 状态表建议

当前已有：

- `upstream_system_sku_sync_state`
- `upstream_system_inventory_sync_state`

问题：

- SKU 信息和 SKU 仓库尺寸重量共用 SKU 状态，页面上“下次同步”会混乱。
- 仓库、物流渠道没有独立状态。
- 后续同步项变多后，继续加独立状态表会重复。

建议新增统一状态表，后续页面统一读取：

表名：

```text
upstream_system_sync_state
```

字段建议：

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `state_id` | bigint | 状态ID |
| `connection_code` | varchar(64) | 主仓接入编号 |
| `sync_type` | varchar(32) | 同步类型：WAREHOUSE、LOGISTICS_CHANNEL、SKU、SKU_DIMENSION、INVENTORY |
| `status` | varchar(16) | NEVER、SYNCING、FRESH、FAILED、SKIPPED |
| `sync_batch_id` | varchar(64) | 最近同步批次 |
| `last_started_time` | datetime | 最近开始时间 |
| `last_finished_time` | datetime | 最近结束时间 |
| `last_success_time` | datetime | 最近成功时间 |
| `next_sync_time` | datetime | 下次计划时间 |
| `total_count` | int | 最近处理总数 |
| `success_count` | int | 最近成功数 |
| `failed_count` | int | 最近失败数 |
| `last_error_code` | varchar(64) | 最近错误码 |
| `last_error_message` | varchar(500) | 最近错误信息 |
| `last_mode` | varchar(32) | SCHEDULED、MANUAL、SELECTED |
| `rate_limit_ms` | int | 最近使用的限速间隔 |
| `update_time` | datetime | 更新时间 |

约束：

- 唯一键：`uk_upstream_sync_state_type(connection_code, sync_type)`。
- 索引：`idx_upstream_sync_state_status(sync_type, status)`。

兼容策略：

- 第一版可以保留现有 SKU 和库存状态表，新增统一状态表供新页面展示。
- 稳定后再考虑迁移旧状态表，避免这次改动过大。

## HttpClient 与外部请求保护

必须和同步拆分一起修：

1. `LingxingOpenApiClient` 不再每次构造新建 JDK `HttpClient`。
2. 改为共享客户端或 Spring Bean，底层使用受控线程池。
3. 对领星请求加全局或连接级限速。
4. 对同一主仓加统一同步锁。
5. 对无权限、签名错误、接口不存在等业务错误做失败熔断，不按 10 分钟继续刷。
6. 对网络错误、429、5xx 做有限重试。

## 实施顺序

1. 暂停旧的 `领星SKU每10分钟同步`，避免继续触发重任务。
2. 修 `HttpClient` 生命周期和同步锁。
3. 拆后端 Service：仓库、物流渠道、SKU信息、尺寸重量、库存。
4. 修改 `syncSkus`，去掉内部尺寸重量同步。
5. 新增指定 SKU 获取尺寸重量接口。
6. 新增或更新若依 `sys_job` 定时任务。
7. 调整顶部“同步”弹窗。
8. 调整 SKU仓库尺寸重量 Tab 的指定 SKU 获取入口。
9. 调整请求日志显示。
10. 编译、前端类型检查、重启后端。
11. 先手动跑小任务验证，再启用夜间任务。

## 验证方案

后端：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
mvn -pl ruoyi-admin -am -DskipTests package
```

前端：

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

运行验证：

- 后端重启后确认 `/monitor/job` 能看到新若依定时任务。
- 手动同步只勾选仓库、物流渠道，确认请求日志显示中文类型。
- 手动同步只勾选 SKU信息，确认不会触发 SKU 仓库尺寸重量日志。
- 指定 1-3 个 SKU 获取尺寸重量，确认请求日志类型为“指定SKU仓库尺寸重量”。
- 库存同步确认调用 `integratedInventory/pageOpen`。
- 观察 15-30 分钟 Java CPU 和 `HttpClient-*` 线程数量，不再持续堆高。

## 需要确认的点

1. 是否接受新增统一同步状态表 `upstream_system_sync_state`。
2. 顶部“同步”弹窗默认是否只勾选仓库、物流渠道。
3. SKU信息每日 23:40 同步是否可以，还是你希望更频繁。
4. 指定 SKU 获取尺寸重量单次上限是否按 100 个。
