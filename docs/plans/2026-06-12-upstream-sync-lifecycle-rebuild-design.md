# 上游系统同步生命周期彻底修复设计方案

生成时间：2026-06-12
项目：`E:\Urili-Ruoyi`
当前状态：设计确认阶段，未改代码，未执行远端 DDL/DML。

## 1. 结论

这次不能只把历史 `SYNCING` 改成失败，也不能只给 HTTP 加一个更长超时。当前卡住是同步生命周期设计不完整造成的：外部请求、手动队列、RuoYi 定时任务、业务批次状态和前端展示之间没有一个可恢复、可审计、可取消的统一任务模型。

彻底修复方向：

1. 同步执行统一回到 RuoYi `sys_job` / Quartz 承载，不再让 Controller 或 JVM 内存队列直接跑外部接口。
2. 新增业务同步任务表，只承载任务生命周期，不替代现有 staging、候选表、库存快照和读模型。
3. 手动同步只负责受理入库，真正执行由 RuoYi dispatcher 定时任务领取。
4. 定时同步也先落任务，再由同一 dispatcher 执行，确保手动、定时、重试、恢复共用一套状态机。
5. 领星 HTTP 客户端改为硬超时、可取消、可记录开始日志、可重试并能识别本机时间漂移。
6. 历史卡住批次单独用 guarded SQL 收口，先预览精确目标和签名，确认后执行，不和代码改造混在一起。

## 2. 当前已知事实

- 当前远端库存在 32 条 `upstream_system_sync_batch.status = 'SYNCING'` 未收口，最早开始时间为 `2026-06-07 00:30:00`。
- 最新影响页面的 5 条 `SYNCING` 包含库存、仓库、物流渠道等手动和定时同步项。
- JVM 线程栈显示：
  - `quartzScheduler_Worker-5` 卡在 `LingxingOpenApiClient.postToBase -> listInventoryProductPage -> HttpClient.send()`。
  - `upstream-sync-1` 卡在 `LingxingOpenApiClient.postToBase -> listWarehouses -> HttpClient.send()`。
- 本机 Windows Time 未同步，`w32tm` 显示源为 `Local CMOS Clock`，与 `time.windows.com` 偏移约 71 秒。
- 最近领星库存接口返回 `11002 时间超时，请重新生产时间戳`。
- 现有项目已有：
  - `LingxingOpenApiClient`：领星签名、HTTP 请求、请求日志回调。
  - `UpstreamLingxingClientFactory`：凭证解密、客户端创建、请求日志落库、异常映射。
  - `IUpstreamSyncService` / `UpstreamSyncServiceImpl`：同步编排。
  - `UpstreamSyncStateRecorder`：同步状态、批次、执行摘要日志。
  - `UpstreamScheduledSyncExecutor` 和独立 RuoYi task：仓库、物流渠道、SKU、尺寸重量、库存。
  - `upstream_system_sync_state`、`upstream_system_sync_batch`、`upstream_system_request_log`。

## 3. 不采用的临时修补

| 做法 | 不采用原因 |
| --- | --- |
| 只把远端 `SYNCING` 批量改成 `FAILED` | 只能让页面暂时不转圈，下一次 HTTP 卡住仍会复发。 |
| 只加长 `HttpRequest.timeout` | 现场已经证明阻塞 `send()` 没有按预期释放执行线程，必须有硬取消和任务层超时。 |
| 把 `upstreamSyncTaskExecutor` 线程池从 1 改成多线程 | 会放大外部接口、CPU 和数据库压力，也不解决 stuck 状态恢复。 |
| Controller 里同步等待执行完成 | 会把 Web 请求变成长事务/长连接，失败后仍难以恢复。 |
| 新造独立调度器绕开 RuoYi | 不符合项目方向，启停、执行一次、日志和权限都应复用若依。 |

## 4. 逐项修复轨道

### 问题 1：本机时间未同步导致领星 reqTime 被拒

根因：领星签名使用当前秒级时间戳；本机时间源未同步，接口返回 `11002`。

彻底修复：

- 运维层先恢复 Windows Time/NTP，不写入代码仓库密钥或远端数据。
- 应用层新增 `ClockHealthService`：
  - 比较 JVM 当前时间与远端 MySQL `utc_timestamp(6)` 或可信时间源。
  - 偏移超过阈值时，拒绝发起领星请求，任务进入 `FAILED` 或 `TIMEOUT`，错误码写 `LOCAL_CLOCK_SKEW`。
  - 错误信息写入同步任务、批次、请求日志和 RuoYi 任务日志。

验证：

- 构造本地时间偏移单元测试，确认不会发出领星 HTTP。
- 现场验证时间恢复后，`AUTH_CHECK` 不再返回 `11002`。

### 问题 2：HTTP 没有硬取消，任务线程卡在 `HttpClient.send()`

根因：当前 `LingxingOpenApiClient` 依赖阻塞式 `httpClient.send(...)`，请求日志也只在响应或异常后写入。

彻底修复：

- 保留 `LingxingOpenApiClient` 作为唯一领星适配器，不把签名和 HTTP 散落到 Service。
- 在适配器内部抽出 `ExternalHttpRequestExecutor` 或 `LingxingOpenApiTransport`：
  - 使用 `sendAsync` 加硬 deadline。
  - deadline 到达后立即返回 `TIMEOUT`，取消 future，不让同步任务线程无限等待。
  - 每次 attempt 开始时先插入请求日志，状态为 `STARTED`。
  - 成功、业务失败、网络失败、超时时补齐同一请求日志的完成字段；不修改已完成历史日志。
  - 429、5xx、可判定网络异常才按配置重试；`11002` 这类时间错误不盲目重试。
- HTTP executor 按外部系统隔离并限制并发，避免一个系统卡住拖垮全部同步。

验证：

- 单元测试模拟不返回的 HTTP server，确认任务在配置超时时间内结束。
- 断言 `upstream_system_request_log` 有 `STARTED -> TIMEOUT` 可观察记录。
- 线程栈验证 dispatcher 线程不会长期停在 `HttpClient.send()`。

### 问题 3：手动同步使用 JVM 内存队列，卡住后无法恢复

根因：`UpstreamManualSyncSubmitter` 使用 `upstreamSyncTaskExecutor` 单线程队列。进程内线程一旦阻塞，数据库里没有可恢复的任务所有权和租约。

彻底修复：

- 手动同步 Controller 只创建数据库任务，返回 `requestNo` 和任务列表。
- 删除或降级 `UpstreamManualSyncSubmitter` 的核心地位：
  - 短期可保留为兼容类，但不再作为主执行路径。
  - 新路径由 `UpstreamSyncTaskService` 入库，RuoYi dispatcher 领取执行。
- 新增 RuoYi 任务 `upstreamSyncDispatchTask.dispatch`：
  - `sys_job.concurrent = '1'`。
  - 高频轻量轮询，例如每 30 秒或 1 分钟。
  - 每轮按优先级领取有限数量任务，执行后释放租约并写结果。

验证：

- 手动触发接口返回后，Web 请求不等待外部接口。
- 停止后端再重启，未完成任务能被 dispatcher 识别并恢复或超时收口。

### 问题 4：任务真正执行前就被标记 `SYNCING`

根因：当前手动同步受理时先调用 `recordSyncing(...)`，队列中等待的项已经显示同步中。

彻底修复：

- 新状态机区分排队、领取、运行：

| 状态 | 含义 |
| --- | --- |
| `PENDING` | 已受理，尚未被 RuoYi dispatcher 领取。 |
| `CLAIMED` | dispatcher 已领取，租约已写入，但还未进入外部请求。 |
| `RUNNING` | 已开始执行当前同步项，已写入 `sync_batch` 的运行态。 |
| `SUCCESS` | 当前同步项成功完成。 |
| `FAILED` | 当前同步项失败且不再重试。 |
| `TIMEOUT` | 超过任务 deadline 或租约超时。 |
| `CANCELED` | 管理端取消未运行或可中断任务。 |
| `SKIPPED` | 前置任务失败、互斥锁占用或不满足执行条件。 |

- 只有进入 `RUNNING` 时才写 `upstream_system_sync_batch.status = 'SYNCING'` 和 `upstream_system_sync_state.status = 'SYNCING'`。
- `PENDING` / `CLAIMED` 只存在于新任务表，不污染现有同步状态视图。

验证：

- 人工提交多项同步，未执行项在前端显示“排队中”，不显示“同步中”。
- 第一个任务失败后，后续项按规则进入 `SKIPPED`，而不是永远 `SYNCING`。

### 问题 5：历史 `SYNCING` 批次没有自动收口

根因：当前没有启动恢复器或租约过期扫描，进程重启/线程卡死后状态不会被重新判断。

彻底修复：

- dispatcher 每轮先执行恢复扫描：
  - `CLAIMED` / `RUNNING` 且 `lease_until < now()` 的任务标记 `TIMEOUT` 或进入重试。
  - 对应 `upstream_system_sync_batch` 和 `upstream_system_sync_state` 同步收口。
  - 补一条同步执行摘要日志，说明由恢复器收口。
- 历史 32 条 `SYNCING` 使用单独 SQL 修复脚本：
  - 只处理精确预览集合。
  - 使用 `@expected_count` 和 `@expected_signature`。
  - 不删除数据，只把过期运行态收口为 `TIMEOUT` 或 `FAILED`。
  - 先写 Markdown 执行记录，等待确认后执行。

验证：

- 构造运行中任务，强停后端，等待租约过期后重启，确认自动收口。
- guarded SQL 在签名不匹配时 `45000` fail-closed。

### 问题 6：请求开始阶段不可观察

根因：当前 `LingxingOpenApiClient` 只有在响应或异常后才写请求日志；真正卡住时数据库没有 attempt 记录。

彻底修复：

- `upstream_system_request_log` 支持开始即插入：
  - `request_time`、`trace_id`、`operation`、`endpoint`、脱敏请求体、`status='STARTED'`。
  - 完成时补 `response_time`、`duration_ms`、脱敏响应、错误码、错误信息、最终状态。
- 同步任务表记录当前 attempt 的 `request_log_id`，方便前端从任务直接跳到请求日志。
- 对没有进入 HTTP 的失败，例如本机时间偏移、前置条件缺失，也写同步执行摘要日志。

验证：

- 模拟慢请求期间查询请求日志，可以看到 `STARTED`。
- 任务超时后请求日志更新为 `TIMEOUT`，并保留请求开始时间。

### 问题 7：API 返回的 `syncBatchId` 与真实批次不一致

根因：`syncSingleType(...)` 执行完成后又生成了新的 UUID 放入响应，和实际 `recordSyncing(...)` 使用的批次不是同一个。

彻底修复：

- 新接口返回以 `requestNo` 为主，`items[].syncBatchId` 为每个同步项真实批次。
- 兼容旧字段时，顶层 `syncBatchId` 只能取第一个真实任务的 `syncBatchId`，不得重新生成。
- 前端轮询或查看详情全部按 `requestNo` / `taskId`，不再依赖一个顶层批次代表多个同步项。

验证：

- 单元测试固定 `submit -> task -> batch -> response` 的 ID 一致性。
- 前端展示的批次号能在 `upstream_system_sync_batch` 中查到。

### 问题 8：定时同步和手动同步共享外部客户端线程池，卡住后放大资源消耗

根因：领星客户端使用静态共享 HTTP executor，手动和定时请求没有资源隔离，也没有任务级熔断。

彻底修复：

- 同一个外部系统继续复用统一适配器，但执行资源按用途做边界：
  - dispatcher 串行控制业务任务。
  - HTTP executor 只处理网络 IO，且有 bounded 队列和命名线程。
  - 重任务如 `SKU_DIMENSION` 保持限速和低优先级。
  - `INVENTORY` 高频任务遇到长任务或连接锁时按规则 `SKIPPED`，不抢资源。
- 每个任务有 `deadline_at`、`lease_until`、`max_attempts`，不会无限消耗资源。

验证：

- 同时提交手动 SKU 尺寸和库存任务，库存任务按优先级/互斥规则排队或跳过。
- 线程数、任务数、请求日志都能解释当前状态。

## 5. 目标架构

```text
管理端按钮 / RuoYi 定时计划
        |
        v
UpstreamSyncTaskService 受理任务
        |
        v
upstream_system_sync_request
upstream_system_sync_task
        |
        v
RuoYi sys_job: upstreamSyncDispatchTask.dispatch
        |
        v
IUpstreamSyncService / sync components
        |
        +--> LingxingOpenApiClient / ExternalHttpRequestExecutor
        |       |
        |       +--> upstream_system_request_log
        |
        +--> staging / snapshot / read model
        |
        +--> UpstreamSyncStateRecorder
                |
                +--> upstream_system_sync_batch
                +--> upstream_system_sync_state
                +--> legacy sync state
```

边界说明：

- RuoYi `sys_job` 负责调度、启停、执行一次和 `sys_job_log`。
- 新任务表负责业务任务生命周期和恢复。
- 现有 `upstream_system_sync_batch` 继续代表一次同步项的结果统计。
- 现有 `upstream_system_sync_state` 继续代表某连接某同步类型的最新状态。
- 现有 staging、候选表、库存快照、来源读模型继续承载业务数据，不被新任务表替代。

## 6. 新增表设计确认草案

该部分属于数据表设计草案。确认前不新增 SQL、Entity、Mapper、Service、Controller 或前端页面。

### 6.1 `upstream_system_sync_request`

业务目的：记录一次同步受理请求。一次请求可以包含多个同步项，例如仓库、物流渠道、SKU、库存。

业务逻辑：

- 承载“谁在什么时候提交了什么同步请求”。
- 不保存外部凭证。
- 不保存上游业务快照。
- 不替代 `sys_job_log` 和 `upstream_system_sync_batch`。

字段草案：

| 字段 | 类型 | 必填 | 默认值 | 含义 |
| --- | --- | --- | --- | --- |
| `request_id` | bigint | 是 | auto_increment | 主键。 |
| `request_no` | varchar(64) | 是 | 无 | 请求号，UUID 或短 ID。 |
| `connection_code` | varchar(64) | 是 | 无 | 主仓接入编号。 |
| `trigger_source` | varchar(32) | 是 | 无 | `MANUAL`、`SCHEDULED`、`RECOVERY`。 |
| `mode` | varchar(32) | 是 | 无 | `MANUAL`、`SCHEDULED`、`SELECTED`。 |
| `requested_sync_types` | varchar(255) | 是 | 无 | 受理时的同步类型列表，逗号分隔或 JSON 字符串。 |
| `status` | varchar(16) | 是 | `PENDING` | 请求整体状态。 |
| `submitted_by` | varchar(64) | 否 | `''` | 提交人，系统任务写 `system`。 |
| `submitted_time` | datetime | 是 | sysdate | 提交时间。 |
| `started_time` | datetime | 否 | null | 首个任务开始时间。 |
| `finished_time` | datetime | 否 | null | 所有任务结束时间。 |
| `task_count` | int | 是 | 0 | 任务总数。 |
| `success_count` | int | 是 | 0 | 成功任务数。 |
| `failed_count` | int | 是 | 0 | 失败任务数。 |
| `timeout_count` | int | 是 | 0 | 超时任务数。 |
| `skipped_count` | int | 是 | 0 | 跳过任务数。 |
| `cancelled_count` | int | 是 | 0 | 取消任务数。 |
| `last_error_message` | varchar(500) | 否 | `''` | 最近错误摘要。 |
| `create_time` | datetime | 是 | sysdate | 创建时间。 |
| `update_time` | datetime | 否 | null | 更新时间。 |
| `remark` | varchar(500) | 否 | `''` | 备注。 |

约束和索引：

- 主键：`request_id`。
- 唯一键：`uk_upstream_sync_request_no(request_no)`。
- 索引：`idx_upstream_sync_request_connection(connection_code, submitted_time)`。
- 索引：`idx_upstream_sync_request_status(status, submitted_time)`。

### 6.2 `upstream_system_sync_task`

业务目的：记录每一个可执行同步项。dispatcher 只领取这张表里的任务。

业务逻辑：

- 一个任务只对应一个 `connection_code + sync_type + sync_batch_id`。
- 任务表保存状态机、租约、重试、超时、关联请求日志。
- 任务成功/失败后同步更新 `upstream_system_sync_batch` 和 `upstream_system_sync_state`。
- 任务表不直接保存上游业务明细。

字段草案：

| 字段 | 类型 | 必填 | 默认值 | 含义 |
| --- | --- | --- | --- | --- |
| `task_id` | bigint | 是 | auto_increment | 主键。 |
| `request_no` | varchar(64) | 是 | 无 | 所属请求号。 |
| `sync_batch_id` | varchar(64) | 是 | 无 | 真实同步批次号。 |
| `connection_code` | varchar(64) | 是 | 无 | 主仓接入编号。 |
| `sync_type` | varchar(32) | 是 | 无 | `WAREHOUSE`、`LOGISTICS_CHANNEL`、`SKU`、`SKU_DIMENSION`、`INVENTORY`。 |
| `mode` | varchar(32) | 是 | 无 | `MANUAL`、`SCHEDULED`、`SELECTED`。 |
| `status` | varchar(16) | 是 | `PENDING` | 任务状态。 |
| `priority` | int | 是 | 100 | 优先级，数字越小越先执行。 |
| `payload_redacted` | longtext | 否 | null | 任务参数脱敏快照，例如指定 SKU 列表。 |
| `lease_owner` | varchar(128) | 否 | `''` | 当前领取者，例如 hostname + pid。 |
| `lease_until` | datetime | 否 | null | 租约过期时间。 |
| `attempt_count` | int | 是 | 0 | 已尝试次数。 |
| `max_attempts` | int | 是 | 1 | 最大尝试次数。 |
| `next_attempt_time` | datetime | 否 | null | 下次可尝试时间。 |
| `deadline_at` | datetime | 否 | null | 任务硬截止时间。 |
| `started_time` | datetime | 否 | null | 开始执行时间。 |
| `finished_time` | datetime | 否 | null | 结束时间。 |
| `current_request_log_id` | bigint | 否 | null | 当前外部请求日志 ID。 |
| `trace_id` | varchar(64) | 否 | `''` | 任务追踪号。 |
| `sys_job_invoke_target` | varchar(500) | 否 | `''` | 执行任务的 RuoYi invoke target。 |
| `pulled_count` | int | 是 | 0 | 拉取行数。 |
| `inserted_count` | int | 是 | 0 | 新增行数。 |
| `changed_count` | int | 是 | 0 | 变更行数。 |
| `unchanged_count` | int | 是 | 0 | 未变化行数。 |
| `disabled_count` | int | 是 | 0 | 停用行数。 |
| `failed_count` | int | 是 | 0 | 失败行数。 |
| `error_code` | varchar(64) | 否 | `''` | 错误码。 |
| `error_message` | varchar(500) | 否 | `''` | 错误摘要。 |
| `create_time` | datetime | 是 | sysdate | 创建时间。 |
| `update_time` | datetime | 否 | null | 更新时间。 |
| `remark` | varchar(500) | 否 | `''` | 备注。 |

约束和索引：

- 主键：`task_id`。
- 唯一键：`uk_upstream_sync_task_batch(sync_batch_id)`。
- 索引：`idx_upstream_sync_task_request(request_no, task_id)`。
- 索引：`idx_upstream_sync_task_claim(status, next_attempt_time, priority, task_id)`。
- 索引：`idx_upstream_sync_task_connection(connection_code, sync_type, status)`。
- 索引：`idx_upstream_sync_task_lease(status, lease_until)`。

### 6.3 字典和 code/label

优先进入若依 `sys_dict`，前端通过统一字典或 integration constants 映射展示。

| 字典类型 | code | label |
| --- | --- | --- |
| `integration_sync_task_status` | `PENDING` | 排队中 |
| `integration_sync_task_status` | `CLAIMED` | 已领取 |
| `integration_sync_task_status` | `RUNNING` | 同步中 |
| `integration_sync_task_status` | `SUCCESS` | 成功 |
| `integration_sync_task_status` | `FAILED` | 失败 |
| `integration_sync_task_status` | `TIMEOUT` | 超时 |
| `integration_sync_task_status` | `CANCELED` | 已取消 |
| `integration_sync_task_status` | `SKIPPED` | 已跳过 |
| `integration_sync_trigger_source` | `MANUAL` | 手动触发 |
| `integration_sync_trigger_source` | `SCHEDULED` | 定时触发 |
| `integration_sync_trigger_source` | `RECOVERY` | 恢复器 |

### 6.4 与现有表关系

| 现有表 | 保留用途 | 新关系 |
| --- | --- | --- |
| `upstream_system_sync_batch` | 单个同步项的结果统计 | 每个 `upstream_system_sync_task` 对应一个真实 `sync_batch_id`。 |
| `upstream_system_sync_state` | 连接 + 同步类型的最新状态 | 只在任务进入 `RUNNING`、最终成功/失败/超时时更新。 |
| `upstream_system_request_log` | 外部请求和同步执行摘要日志 | 任务保存当前 `request_log_id` 和 `trace_id`，方便跳转。 |
| `sys_job` | RuoYi 定时任务配置 | 新增 dispatcher job，已有同步 job 改为任务生产入口或保留执行入口。 |
| `sys_job_log` | RuoYi 调度日志 | 通过 invokeTarget、traceId、任务号关联；如必须精确 `job_log_id`，再小范围扩展 Quartz ThreadLocal。 |

## 7. RuoYi 定时任务设计

### 7.1 保留现有同步 job

现有 task 组件继续保留：

- `upstreamWarehouseSyncTask.sync`
- `upstreamLogisticsChannelSyncTask.sync`
- `upstreamSkuInfoSyncTask.sync`
- `upstreamSkuDimensionSyncTask.sync`
- `upstreamInventorySyncTask.sync`

调整方向：

- 定时 job 不再直接调用领星接口。
- 定时 job 改为创建 `SCHEDULED` 任务。
- 如果需要短期兼容，可以先保留原执行方法，新增 `enqueue` 方法；迁移完成后再切换 `sys_job.invoke_target`。

### 7.2 新增 dispatcher job

新增 RuoYi 任务：

```text
upstreamSyncDispatchTask.dispatch
```

建议配置：

| 配置项 | 建议值 |
| --- | --- |
| `job_group` | `SYSTEM` |
| `cron_expression` | `0/30 * * * * ?` 或 `0 * * * * ?` |
| `misfire_policy` | 不立即补跑 |
| `concurrent` | `1` |
| `status` | 初始启用，确认后落库 |

执行规则：

1. 恢复过期租约任务。
2. 按优先级领取 `PENDING` 或可重试任务。
3. 进入 `CLAIMED`，写 `lease_owner` 和 `lease_until`。
4. 检查连接状态、凭证状态、本机时间健康、同步锁。
5. 进入 `RUNNING`，写真实 `SYNCING` 批次和状态。
6. 调用 `IUpstreamSyncService` 的分项执行能力。
7. 写成功、失败、超时或跳过结果。
8. 刷新请求汇总状态。

## 8. 后端改造范围

### 8.1 新增/改造类

| 类 | 作用 |
| --- | --- |
| `UpstreamSyncTaskService` | 受理任务、查询任务、取消、重试、恢复扫描。 |
| `UpstreamSyncDispatchTask` | RuoYi dispatcher 入口。 |
| `UpstreamSyncTaskMapper` | 任务表和请求表读写。 |
| `ClockHealthService` | 本机时间健康检查。 |
| `ExternalHttpRequestExecutor` | 外部 HTTP 硬超时、取消、attempt 日志、重试。 |
| `LingxingOpenApiTransport` | 如果拆分，承载领星 HTTP transport；`LingxingOpenApiClient` 继续承载签名和响应解析。 |

### 8.2 保留并复用

| 组件 | 复用方式 |
| --- | --- |
| `LingxingOpenApiClient` | 保留领星签名、响应解析、operation 语义。 |
| `UpstreamLingxingClientFactory` | 继续负责凭证解密、日志回调、异常映射。 |
| `IUpstreamSyncService` | 继续作为同步业务接口，但增加 task-aware 执行入口。 |
| `UpstreamSyncStateRecorder` | 继续集中写状态、批次、同步执行摘要，扩展 `TIMEOUT/SKIPPED/CANCELED`。 |
| staging/diff 组件 | 继续负责仓库、物流、SKU、尺寸、库存落库。 |

### 8.3 Controller 边界

`AdminUpstreamSystemController`：

- 手动同步接口保留现有路径和权限。
- 新行为只受理任务并返回任务请求信息。
- 不直接调用外部系统。
- 不直接依赖 `ruoyi-quartz` 的 `ISysJobService`，避免 `integration` 模块反向依赖 Quartz 实现。是否立即唤醒 dispatcher，后续只在依赖边界允许时通过 `ruoyi-admin` 小范围桥接。

## 9. 前端改造范围

复用 Ant Design Pro 和现有 UpstreamSystem 页面，不新造 UI 体系。

### 9.1 页面结构

在上游系统详情中新增或调整为：

- “同步任务” Tab 或 Drawer。
- 任务列表使用 `ProTable`。
- 状态使用 `Tag` 和统一状态映射。
- 操作用 `Dropdown` 收起低频操作，例如取消、重试、查看请求日志。
- 筛选区使用 `getPersistedProTableSearch(...)`。
- 下拉复用 `SEARCHABLE_SELECT_PROPS`。

### 9.2 API

建议新增：

| 方法 | 权限 | 说明 |
| --- | --- | --- |
| `GET /integration/admin/upstream-systems/{connectionCode}/sync-requests` | `integration:upstream:task:list` 或 `integration:upstream:log` | 请求列表。 |
| `GET /integration/admin/upstream-systems/{connectionCode}/sync-tasks` | `integration:upstream:task:list` 或 `integration:upstream:log` | 任务列表。 |
| `POST /integration/admin/upstream-systems/{connectionCode}/sync-tasks/{taskId}/retry` | `integration:upstream:task:retry` 或 `integration:upstream:sync` | 重试失败任务。 |
| `POST /integration/admin/upstream-systems/{connectionCode}/sync-tasks/{taskId}/cancel` | `integration:upstream:task:cancel` 或 `integration:upstream:sync` | 取消未运行任务。 |

权限需要确认：

- 方案 A：新增更细权限 `integration:upstream:task:list/retry/cancel`，权限最清晰，但要同步 seed、controller、前端 gate、合同测试。
- 方案 B：首版复用 `integration:upstream:log` 查看、`integration:upstream:sync` 重试/取消，改动小，但权限语义不如 A。

推荐方案 A。

## 10. SQL 与迁移边界

新增 SQL 建议拆两类：

1. 结构脚本：`RuoYi-Vue/sql/20260612_upstream_sync_task_lifecycle.sql`
   - 新建任务请求表和任务表。
   - 新增字典。
   - 新增或调整 `sys_job` dispatcher。
   - 必须保留 confirm token、前置表/列校验、`45000` fail-closed。
2. 历史收口脚本：`RuoYi-Vue/sql/20260612_upstream_sync_stuck_batch_reconcile.sql`
   - 只处理确认过的历史 `SYNCING` 集合。
   - 必须要求 `@expected_count` 和 `@expected_signature`。
   - 执行前重新计算签名，不匹配直接失败。
   - 只更新状态，不删除历史。

未确认前不执行任何远端 DDL/DML。

## 11. 实施分期

### Phase 0：方案确认

- 确认本方案中的任务表、状态机、权限策略和 dispatcher 方式。
- 确认历史 `SYNCING` 收口口径使用 `TIMEOUT` 还是 `FAILED`。

### Phase 1：即时恢复前置

- 修复本机 Windows Time/NTP。
- 可选：重启后端释放当前卡住的 JVM HTTP 线程。
- 只读复查同步队列和请求日志。
- 仍不执行远端 DML，除非你确认历史收口脚本。

### Phase 2：外部请求硬超时和开始日志

- 改造 `LingxingOpenApiClient` / transport。
- 添加 `ClockHealthService`。
- 请求开始先落日志。
- 补 timeout/cancel/retry 单元测试。

### Phase 3：任务生命周期表和 dispatcher

- 新增请求表、任务表、Mapper、Service。
- 新增 `upstreamSyncDispatchTask.dispatch`。
- 手动同步改为受理入库。
- 定时同步 job 改为生产任务或迁移到同一 dispatcher 执行链。

### Phase 4：前端任务可视化

- 增加同步任务列表、请求详情、状态展示、重试/取消入口。
- 同步新增权限 gate 和合同测试。

### Phase 5：历史卡住批次收口

- 生成只读预览和签名。
- 写 Markdown 执行记录。
- 等确认后执行 guarded SQL。
- 执行后复查 32 条历史 `SYNCING` 是否清零。

### Phase 6：验证和索引

- 后端测试、前端合同测试、API 运行态验证。
- 代码改动后运行 `codegraph sync .` 并写入执行记录。

## 12. 验证清单

后端：

- `mvn -pl integration -am "-Dtest=..." test`
- `mvn -pl ruoyi-admin -am -DskipTests compile`
- 覆盖：
  - HTTP 硬超时。
  - `STARTED` 请求日志。
  - 任务状态机。
  - lease 过期恢复。
  - `syncBatchId` 一致性。
  - 时间偏移 fail-fast。
  - RuoYi dispatcher 只从任务表领取执行。

前端：

- `react-ui` 增加 UpstreamSystem 任务列表合同测试。
- `upstream-system-permission-guard.test.ts` 覆盖新增权限。
- JS 镜像仍保持纯 re-export。
- `three-terminal.manifest.json` 登记关键测试。

运行态：

- 登录管理端 `admin/admin123`。
- 手动提交仓库同步，确认接口立即返回 `requestNo`。
- 观察任务从 `PENDING -> RUNNING -> SUCCESS`。
- 模拟外部超时，确认任务进入 `TIMEOUT`，页面不无限转圈。
- RuoYi `系统监控 / 定时任务` 能看到 dispatcher 执行日志。
- 上游请求日志能看到开始、完成、错误码和耗时。

数据：

- 只读查询 active datasource 后再验证。
- 历史收口脚本执行前必须预览 count/signature。
- 不读取或写入本地 Docker MySQL/Redis，除非明确要求隔离验证。

## 13. 需要确认的问题

1. 是否同意新增 `upstream_system_sync_request` 和 `upstream_system_sync_task` 两张任务生命周期表？
2. 历史卡住的 `SYNCING` 批次最终状态用 `TIMEOUT` 还是 `FAILED`？
3. 前端任务权限采用新增精细权限，还是首版复用 `integration:upstream:log/sync`？
4. dispatcher 频率采用 30 秒还是 1 分钟？
5. 手动同步提交后是否接受最多等待 dispatcher 下一轮执行，还是需要增加一个 `ruoyi-admin` 桥接能力立即触发 dispatcher？

确认后再进入实现；未确认前不新增 SQL、不改远端数据。
