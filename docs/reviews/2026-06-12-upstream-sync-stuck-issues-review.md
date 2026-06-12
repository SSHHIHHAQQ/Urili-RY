# 2026-06-12 上游同步卡住问题逐项审查

## 审查范围

- 目标：解释同步队列为什么卡住，以及每类问题应如何修复。
- 范围：领星 WMS 上游同步、手动同步队列、Quartz 定时同步、同步状态表、请求日志、时间戳签名。
- 数据源：`.env.local` 指向的远端 `fenxiao` 库，只读查询。
- 未执行：远端 DDL、远端 DML、Redis 清理、后端重启、代码修改。

## 问题 1：本机时间未同步，导致领星请求时间戳被拒

### 现象

- 最近库存同步持续出现领星业务错误：
  - `external_error_code=11002`
  - `external_error_message=时间超时，请重新生产时间戳`
- 失败集中在 `https://api.xlwms.com/openapi/v1/integratedInventory/pageOpen`。

### 证据

- `w32tm /query /status` 显示：
  - `Leap 指示符: 3(未同步)`
  - `源: Local CMOS Clock`
  - `上次成功同步时间: 未指定`
- `w32tm /stripchart /computer:time.windows.com /samples:3 /dataonly` 显示本机与 `time.windows.com` 偏移约 `71.18s`。
- 当前 Java 客户端使用本机时间生成 `reqTime`：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java:409`

### 根因判断

- 领星签名使用 `reqTime`，如果调用方时间偏移超过上游允许窗口，会被拒绝。
- 本机当前没有可靠 NTP 同步，偏移已经超过 1 分钟。即使旧文档曾写 5 分钟窗口，真实 WMS 接口当前可能更严格，或者上游服务器时间窗口与 `time.windows.com` 不完全一致。

### 修复方案

1. 先修运行环境：启用 Windows 时间同步，确保后端机器不再依赖 Local CMOS Clock。
2. 后端启动预检：启动时检测系统时间同步状态或与可信时间源偏移；偏移超过阈值时告警，至少写入日志。
3. 领星调用前防御：如果检测到本机时间不可用或偏移过大，直接 fail-fast，返回“本机时间未同步”，不要继续发请求消耗队列。

### 验证方式

- `w32tm /query /status` 显示已同步时间源。
- `w32tm /stripchart` 偏移回到可接受范围。
- 手动调用领星库存同步不再返回 `11002 时间超时`。

### 远端数据影响

- 该问题修复不需要远端 DML。

## 问题 2：HTTP 调用没有硬超时，线程卡在 `HttpClient.send()`

### 现象

- 页面表现为同步一直卡住。
- `03:10` 后库存同步写了 `SYNCING`，但没有对应 request log 成功或失败记录。
- `03:13` / `03:14` 的手动仓库和物流渠道同步也写了 `SYNCING`，但没有 request log。

### 证据

- JVM 线程栈显示：
  - `quartzScheduler_Worker-5` 卡在 `LingxingOpenApiClient.postToBase -> listInventoryProductPage`
  - `upstream-sync-1` 卡在 `LingxingOpenApiClient.postToBase -> listWarehouses`
  - 多个 `lingxing-http-*` 线程在 `SSLEngine.unwrap` / `SSLFlowDelegate` 路径运行。
- Java 客户端当前只设置：
  - `HttpClient.connectTimeout(...)`
  - `HttpRequest.timeout(...)`
  - 阻塞调用 `httpClient.send(...)`
- 相关代码：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java:41`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java:60`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java:434`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java:440`

### 根因判断

- 代码以为 `HttpRequest.timeout(10s)` 可以覆盖整次请求，但现场证明它没有把当前 HTTPS/SSL 读卡死场景及时打断。
- 因为 `send()` 不返回，后续 `catch` 逻辑不会执行，同步状态也不会落 `FAILED`。

### 修复方案

1. 把 `httpClient.send(...)` 改成可取消的 `sendAsync(...)`。
2. 在外层增加硬超时边界，例如 `CompletableFuture#get(timeout)` 或 `orTimeout`，超时后必须 `future.cancel(true)`。
3. 超时统一映射为 `LINGXING_TIMEOUT_ERROR`，且标记为 retryable。
4. 每次 attempt 都必须在失败时写 request log，状态为 `FAILURE`，错误码为 `LINGXING_TIMEOUT_ERROR`。
5. 增加单元测试：模拟永不返回的 HTTP 调用，验证 10s 内失败、写日志、释放同步锁。

### 可复用参考

- 旧工程 `E:\Urili` 已有硬超时实现：
  - `E:\Urili/packages/integrations/lingxing/src/client.ts:29`
  - `E:\Urili/packages/integrations/lingxing/src/client.ts:255`
  - `E:\Urili/packages/integrations/lingxing/src/client.ts:281`
- 它使用 `AbortController + Promise.race`，并有 timeout 测试覆盖：
  - `E:\Urili/packages/integrations/lingxing/src/public.test.ts:568`

### 验证方式

- 新增 Java 单元测试覆盖超时取消。
- 本地启动后端，触发手动仓库同步，外部接口卡住时不超过配置超时。
- JVM 线程栈中不再长期存在卡住的 `upstream-sync-*` / `quartzScheduler_Worker-*`。

### 远端数据影响

- 代码修复本身不需要远端 DML。

## 问题 3：手动同步单线程队列被一个卡住任务阻塞

### 现象

- 手动同步线程 `upstream-sync-1` 卡住后，后续人工触发的同步会继续等待。
- 页面会表现为“同步队列卡住”，而不是单个连接失败。

### 证据

- 手动同步执行器配置：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/config/UpstreamSyncTaskExecutorConfig.java:14`
  - `corePoolSize=1`
  - `maxPoolSize=1`
  - `queueCapacity=50`
- 提交入口：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/sync/UpstreamManualSyncSubmitter.java:29`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/sync/UpstreamManualSyncSubmitter.java:44`

### 根因判断

- 单线程队列本身是为了避免弱服务器被人工同步打满，这个设计目标合理。
- 问题在于没有硬超时和隔离边界；一个外部 HTTP 永不返回，就会占住全局唯一 worker。

### 修复方案

1. P0 先修问题 2 的硬超时，避免 worker 永久占用。
2. P1 再考虑队列隔离：保留低并发，但把“全局单线程”改成可配置小并发，例如 2 或 3，同时继续保留连接级 `acquireSyncLock(connectionCode)`。
3. 增加队列指标：当前运行任务、等待任务数、最老等待时间。

### 验证方式

- 人工提交两个不同连接同步：第一个超时后，第二个能继续执行。
- 同一个 connectionCode 的重复同步仍被连接级锁拒绝。

### 远端数据影响

- 不需要远端 DML。

## 问题 4：排队项在真正执行前就被标记为 `SYNCING`

### 现象

- `03:13` / `03:14` 手动同步同时出现仓库和物流渠道 `SYNCING`。
- 但线程栈只显示当前 worker 卡在仓库同步 `listWarehouses`，物流渠道还没有真正开始请求。

### 证据

- 手动异步提交时，所有 work items 在入队后立即调用 `recordSyncing`：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/sync/UpstreamManualSyncSubmitter.java:43`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/sync/UpstreamManualSyncSubmitter.java:125`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/sync/UpstreamManualSyncSubmitter.java:129`

### 根因判断

- 当前状态模型只有 `SYNCING / FRESH / FAILED / SKIPPED / NEVER`，没有区分“已入队”和“正在执行”。
- 因此一个多项同步请求里，后续同步项还没跑，也会在 UI 上显示为同步中。

### 修复方案

1. 增加 `QUEUED` 或 `PENDING` 状态。
2. 入队时写 `QUEUED`，真正进入 `runner.run(workItem)` 前再更新为 `SYNCING`。
3. 如果前置同步失败，后续未执行项应落 `FAILED` 或 `SKIPPED`，错误信息写清“前置同步失败，未执行”。
4. 前端展示区分“排队中”和“同步中”。

### 验证方式

- 提交 `WAREHOUSE + LOGISTICS_CHANNEL` 两项同步。
- 第一项运行时，第一项为 `SYNCING`，第二项为 `QUEUED`。
- 第一项失败时，第二项不应长期保留 `SYNCING`。

### 远端数据影响

- 如果新增状态字典或前端展示，需要确认是否写入字典/菜单数据。

## 问题 5：历史 `SYNCING` 批次没有自动收口

### 现象

- `upstream_system_sync_batch` 当前有 32 条 `SYNCING`。
- 最早 `SYNCING` 批次从 `2026-06-07 00:30:00` 残留到现在。

### 证据

- 只读查询结果：
  - `LX-CA012 / INVENTORY / MANUAL` 有 5 条历史 `SYNCING`。
  - `LX-CA012 / INVENTORY / SCHEDULED` 有 20 条历史 `SYNCING`。
  - 另有 `SKU`、`WAREHOUSE`、`SKU_DIMENSION` 历史残留。
- 记录写入和完成依赖：
  - `recordSyncing` 插入 `SYNCING` 批次。
  - `recordSuccess` / `recordFailure` 才会更新批次完成。
  - 相关代码：`RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/sync/UpstreamSyncStateRecorder.java:30`、`:39`、`:51`、`:184`、`:197`

### 根因判断

- 如果 JVM 被重启、进程被杀、线程永久卡住、或者异常未走到 `recordFailure`，批次会永久停在 `SYNCING`。
- 这些历史批次不会靠后端重启自动修正。

### 修复方案

1. 增加启动修复器：应用启动时扫描超过阈值的 `SYNCING` 批次，标记为 `FAILED`，错误信息写“进程重启或超时未收口”。
2. 增加周期 watchdog：运行中定期扫描超过阈值的 `SYNCING` 状态，进行告警；是否自动落失败要谨慎，因为可能误伤慢任务。
3. 当前远端历史数据修复必须先生成 DML 方案，列出精确目标集合和签名，用户确认后执行。

### 验证方式

- 构造一条超时 `SYNCING` 测试记录，启动后被标记为 `FAILED`。
- 未超阈值的运行中任务不被误改。

### 远端数据影响

- 当前历史状态修复涉及远端 DML，必须单独确认后执行。

## 问题 6：请求开始阶段没有可观测日志，卡住时看不到正在请求哪个接口

### 现象

- 当前卡住的 `03:10` / `03:13` / `03:14` 请求，在 `upstream_system_request_log` 中没有 attempt 记录。
- 只能通过 JVM 线程栈确认它卡在哪个接口。

### 证据

- `LingxingOpenApiClient.postToBase` 只在 `send()` 返回或抛异常后调用 `writeLog(log)`：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java:440`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java:471`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java:483`

### 根因判断

- 请求日志是完成态日志，不是 attempt 生命周期日志。
- 外部请求卡在网络层时，完成态日志永远不会写入。

### 修复方案

1. 最小方案：硬超时修复后，保证超时能写失败日志。
2. 完整方案：新增 attempt-start 日志或单独的运行中观测表，字段包括 `traceId`、operation、endpoint、startedTime、attempt、status。
3. 如果不想新增表，可在同步状态 `last_error_message` 或扩展字段里记录“当前 endpoint / attempt started”，但这会污染业务状态，不推荐。

### 验证方式

- 模拟外部请求挂住，超时前能从状态看到当前 endpoint，超时后有失败 request log。

### 远端数据影响

- 完整方案若新增表或字段，需要先走表设计确认。

## 问题 7：同步返回的 `syncBatchId` 与真实批次不一致

### 现象

- 同步服务在执行完成后，返回结果里重新生成了一个 `syncBatchId`，不是实际写入 `upstream_system_sync_batch` 的批次号。

### 证据

- `syncSelected` 中先 `result.setSyncBatchId(UUID.randomUUID().toString())`，随后每个 `executeSyncItem` 内部又生成自己的真实批次：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSyncServiceImpl.java:108`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSyncServiceImpl.java:337`
- `syncSingleType` 同样返回一个新的随机批次：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSyncServiceImpl.java:321`

### 根因判断

- 返回值和持久化批次不是同一个来源。
- 如果前端或排障人员用返回的 `syncBatchId` 查日志，会查不到真实批次，降低定位效率。

### 修复方案

1. `executeSyncItem` 返回值中携带真实 `syncBatchId`。
2. 单项同步返回真实批次号。
3. 多项同步返回一个 `requestBatchId`，同时每个 item 返回自己的 `syncBatchId`，不要用一个随机 ID 冒充真实批次。

### 验证方式

- API 返回的 `syncBatchId` 能在 `upstream_system_sync_batch` 查到。
- 多项同步每个 item 的 batch 都能查到对应日志。

### 远端数据影响

- 不需要远端 DML。

## 问题 8：定时同步和手动同步共用外部客户端线程池，卡住后会放大资源消耗

### 现象

- JVM 线程栈中多个 `lingxing-http-*` 线程在 SSL 读路径运行，且 CPU 累积很高。
- Java 进程工作集约 3.4GB。

### 证据

- `LingxingOpenApiClient` 使用静态共享 HTTP executor：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java:43`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java:60`
- 定时同步和手动同步都通过同一个客户端类调用领星。

### 根因判断

- 正常情况下共享 executor 可以减少资源开销。
- 但当 HTTP 调用无法取消时，定时任务和手动任务会共同占用同一批 `lingxing-http-*` 线程，风险会扩散到整个接入模块。

### 修复方案

1. 先做问题 2 的硬超时和取消。
2. 对 HTTP executor 增加命名、指标和饱和告警。
3. 如后续仍出现资源竞争，再拆分定时同步和手动同步的 executor，或者按 endpoint 配置并发上限。

### 验证方式

- 压测多个超时请求后，`lingxing-http-*` 不应长期堆积。
- JVM 工作集和 CPU 不应持续异常增长。

### 远端数据影响

- 不需要远端 DML。

## 推荐修复顺序

1. P0：立即恢复运行环境时间同步。
2. P0：增加领星 HTTP 硬超时、取消和失败日志。
3. P0：重启本机后端，释放当前卡住线程。
4. P1：为历史 `SYNCING` 批次生成远端 DML 修复方案，确认后执行。
5. P1：区分 `QUEUED` 和 `SYNCING`，避免排队项误显示为运行中。
6. P1：修正 API 返回的 `syncBatchId`。
7. P2：增加同步队列指标、HTTP executor 指标、启动修复器和 watchdog。

## 验证清单

- `w32tm /query /status`
- `w32tm /stripchart /computer:time.windows.com /samples:3 /dataonly`
- 领星库存同步手动触发，不再返回 `11002`。
- 模拟超时接口，Java 客户端能在配置超时内失败并写 request log。
- `upstream_system_sync_state` 不再长期停留在 `SYNCING`。
- `upstream_system_sync_batch` 新增批次都能进入 `FRESH` 或 `FAILED`。
- API 返回批次号能在批次表查到。

## 本次未验证

- 未执行代码修复。
- 未重启后端。
- 未清理远端历史 `SYNCING`。
- 未对领星真实接口做修复后重测。
- 未运行编译或测试。
