# 2026-06-12 上游同步队列卡住检查记录

## 结论

- 当前不是普通排队等待，而是本机后端 JVM 内已有同步线程卡在领星 WMS HTTP 调用中。
- `upstream_system_sync_batch` 存在 32 条 `SYNCING` 未收口记录，最早可追溯到 `2026-06-07 00:30:00`。
- 当前仍影响 UI 的最新 `SYNCING` 状态有 5 条：
  - `LX-CA012` / `INVENTORY` / `SCHEDULED` / `04ef1dd7`，`2026-06-12 03:10:00` 开始，未完成。
  - `LX-KAT-91B1E277` / `WAREHOUSE` / `MANUAL` / `24baecca`，`2026-06-12 03:13:58` 开始，未完成。
  - `LX-KAT-91B1E277` / `LOGISTICS_CHANNEL` / `MANUAL` / `f4631b21`，`2026-06-12 03:13:58` 开始，未完成。
  - `LX-KAT库存仓-58F5A0B6` / `WAREHOUSE` / `MANUAL` / `292276cd`，`2026-06-12 03:14:09` 开始，未完成。
  - `LX-KAT库存仓-58F5A0B6` / `LOGISTICS_CHANNEL` / `MANUAL` / `c6741829`，`2026-06-12 03:14:09` 开始，未完成。

## 数据源确认

- 后端激活配置：`application.yml` 使用 `spring.profiles.active=druid`。
- MySQL 连接来源：`.env.local` 的 `RUOYI_DB_URL`。
- 本次只读目标：`gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com:28634/fenxiao`。
- MySQL 服务端时间：`2026-06-12 03:22:17`。
- Redis 连接来源：`.env.local`，目标 `114.132.156.75:6379`，DB `1`。
- 本次未读取本地 Docker MySQL / Redis。

## 已执行检查

- 只读 SQL：
  - `upstream_system_connection`
  - `upstream_system_sync_state`
  - `upstream_system_sync_batch`
  - `upstream_system_sku_sync_state`
  - `upstream_system_inventory_sync_state`
  - `upstream_system_request_log`
  - `sys_job`
  - `sys_job_log`
  - `QRTZ_TRIGGERS`
  - `QRTZ_FIRED_TRIGGERS`
- JVM 线程栈：`jcmd 25444 Thread.print`，仅用于确认当前运行线程位置。

## 关键证据

- 最近库存定时同步持续失败，外部接口返回：
  - `external_error_code=11002`
  - `external_error_message=时间超时，请重新生产时间戳`
  - 失败集中在 `https://api.xlwms.com/openapi/v1/integratedInventory/pageOpen`
- `03:10` 后的最新库存定时同步未写入 request log，说明请求还未从 `HttpClient.send()` 返回。
- `03:13` / `03:14` 的手动仓库和物流渠道同步也未写入 request log，说明手动同步线程同样卡在 HTTP send 阶段。
- JVM 线程栈显示：
  - `quartzScheduler_Worker-5` 卡在 `LingxingOpenApiClient.postToBase -> listInventoryProductPage`。
  - `upstream-sync-1` 卡在 `LingxingOpenApiClient.postToBase -> listWarehouses`。
  - 多个 `lingxing-http-*` 线程处于 `SSLEngine.unwrap` / `SSLFlowDelegate` 路径，并有高 CPU 累积。

## 判断

- 直接卡住的原因是领星 WMS HTTP 客户端调用没有在代码设定的 `10s` request timeout 内返回，导致：
  - 定时库存任务占住 Quartz 工作线程和 `UpstreamScheduledSyncExecutor` 的 JVM 串行锁。
  - 手动同步占住 `upstreamSyncTaskExecutor` 的唯一工作线程。
  - 后续手动同步会继续进入等待或表现为页面同步中。
- 历史 `SYNCING` 批次不会因为后端重启自动收口；这些是远端库里的历史状态残留。未确认前不能直接 DML 修正。

## 未执行操作

- 未执行远端 DDL。
- 未执行远端 DML。
- 未清理 Redis。
- 未重启后端。
- 未修改代码，因此未运行 `codegraph sync .`。

## 建议下一步

1. 立即恢复服务：可重启本机后端，释放当前 JVM 内卡住的 HTTP 线程和内存锁。
2. 状态修复：如需把远端库中历史 `SYNCING` 批次标记为失败，需要先按项目规则生成 DML 执行方案并确认精确目标集合。
3. 代码修复：给领星 HTTP 调用增加更硬的超时和取消边界，避免 `HttpClient.send()` 长时间不返回；同时考虑在异步任务启动后增加 watchdog，把超时同步项落失败。
