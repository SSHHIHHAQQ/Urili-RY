# 2026-06-12 上游同步生命周期修复实施记录

## 结论

本次已按确认方向完成代码侧修复：手动同步和定时同步统一进入数据库任务生命周期，由 RuoYi Quartz dispatcher 领取执行；领星 HTTP 调用增加开始日志、硬超时和取消边界；前端增加同步任务页、重试、取消入口及细粒度权限。

本次没有执行任何远端 DDL/DML，没有清理 Redis，没有重启后端。新增 SQL 均为待确认后执行的 guarded 脚本。

## 每个问题的处理状态

### 问题 1：本机时间未同步导致领星 reqTime 被拒

- 已修复代码防线：新增 `UpstreamClockHealthGuard`，创建领星客户端前用当前激活数据库时间校验本机时钟。
- 超过 60 秒偏移时直接 fail-fast，错误码为 `LOCAL_CLOCK_SKEW`，不再继续调用领星接口消耗同步队列。
- 未做运维动作：没有修改 Windows Time/NTP 配置。现场机器时间仍需单独恢复。

### 问题 2：HTTP 调用没有硬超时，线程卡在 `HttpClient.send()`

- 已将领星请求从阻塞式 `httpClient.send(...)` 改为 `sendAsync(...) + future.get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)`。
- 超时后执行 `future.cancel(true)`，错误码归类为 `LINGXING_TIMEOUT`。
- 每次 attempt 开始先写 `STARTED` 请求日志，完成后更新同一条日志为成功或失败。

### 问题 3：手动同步 JVM 单线程队列被一个卡住任务阻塞

- 手动同步接口现在只负责受理任务并入库，返回 `requestNo`、`requestId`、`taskId`、真实 `syncBatchId`。
- 真正执行由 `upstreamSyncDispatchTask.dispatch` 统一领取，避免 Web 请求或内存队列直接跑外部接口。
- 旧 `manualSyncSubmitter` 不再作为同步主路径。

### 问题 4：任务真正执行前就显示 `SYNCING`

- 新增任务状态机：`PENDING`、`CLAIMED`、`RUNNING`、`SUCCESS`、`FAILED`、`TIMEOUT`、`CANCELED`、`SKIPPED`。
- 只有任务进入 `RUNNING` 后才写现有 `upstream_system_sync_batch` 和 `upstream_system_sync_state` 的 `SYNCING`。
- 前端新增“同步任务”Tab，可直接看到排队、领取、运行、失败、超时和取消状态。

### 问题 5：历史 `SYNCING` 批次没有自动收口

- 已新增运行中任务 lease 恢复逻辑：dispatcher 每轮先扫描过期 `CLAIMED/RUNNING` 任务并收口为 `TIMEOUT`。
- 已生成历史批次修复脚本 `RuoYi-Vue/sql/20260612_upstream_sync_stuck_batch_reconcile.sql`。
- 未执行远端 DML。历史 32 条 `SYNCING` 必须等精确目标集合、count/signature 和执行确认后再处理。

### 问题 6：请求开始阶段不可观测

- `upstream_system_request_log` 现在支持开始即插入 `STARTED`。
- 完成、失败、超时后通过 `request_log_id` 更新原日志行。
- 前端请求日志颜色区分 `STARTED`、`SUCCESS`、`TIMEOUT` 和失败类状态。

### 问题 7：API 返回的 `syncBatchId` 和真实批次不一致

- 新任务模型中每个 task 创建时生成真实 `syncBatchId`，执行、批次表和返回 item 共用同一个 ID。
- 多项同步返回以 `requestNo` 表示整次请求，每个 item 独立返回自己的 `taskId` 和 `syncBatchId`。
- 顶层兼容字段只取第一个真实任务批次，不再额外生成假批次。

### 问题 8：定时同步和手动同步共享外部 HTTP executor，卡住后放大资源消耗

- 已通过 dispatcher 限制每轮领取数量，并保留连接级 `syncingConnectionCodes` 锁。
- HTTP 层增加硬超时和取消，降低 `lingxing-http-*` 长时间堆积风险。
- 本次未拆分多个 HTTP executor；如果后续仍出现资源争用，再按 endpoint 或任务类型拆分执行资源。

## 新增和修改范围

- 后端任务生命周期：`UpstreamSyncRequestRecord`、`UpstreamSyncTask`、`UpstreamSyncTaskMapper`、`UpstreamSyncDispatchTask`。
- 后端同步服务：`IUpstreamSyncService`、`UpstreamSyncServiceImpl`。
- 领星请求边界：`LingxingOpenApiClient`、`LingxingRequestLogEntry`、`UpstreamLingxingClientFactory`、`UpstreamClockHealthGuard`。
- 管理端接口：`AdminUpstreamSystemController` 新增同步请求/任务列表、重试、取消接口。
- 前端：`SyncTabs.tsx` 新增同步任务 Tab，service/type/constants 增加任务 API 和状态展示。
- SQL：新增生命周期迁移脚本和历史 stuck 批次收口脚本，更新上游系统菜单 seed。
- 合同测试：补充 integration 架构合同、权限合同、前端 upstream 权限 guard。

## 验证结果

- `mvn -pl integration -am -DskipTests compile`：通过。
- `mvn -pl integration -am "-Dtest=IntegrationModuleBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，7 tests。
- `mvn -pl integration -am "-Dtest=IntegrationAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，7 tests。
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，82 tests。
- `mvn -pl ruoyi-admin -am -DskipTests compile`：通过。
- `node_modules\.bin\jest.cmd --config jest.config.ts tests/upstream-system-permission-guard.test.ts --runInBand`：通过，6 tests。
- `npm test -- --check-manifest`：通过。
- `npm test`：通过，三端总 gate 通过，前端 33 suites / 304 tests。
- `codegraph sync .`：通过，结果为 `Already up to date`。

## 未执行项

- 未执行 `20260612_upstream_sync_task_lifecycle.sql`。
- 未执行 `20260612_upstream_sync_stuck_batch_reconcile.sql`。
- 未重启后端，因此运行中的旧 JVM 卡住线程不会因代码提交自动释放。
- 未做 live API 点击验证，因为新增任务表和 sys_job 尚未在目标库执行。
- 未修改 Windows Time/NTP，现场时间同步仍需单独恢复。

## 后续执行条件

1. 确认目标数据源和 SQL 执行窗口。
2. 对生命周期脚本预览 `sys_job` 与权限菜单目标集合，填入 count/signature 后执行。
3. 对历史 stuck 批次预览精确 `sync_batch_id` 集合，填入 count/signature 后执行。
4. 重启后端，使新 dispatcher、时间守卫和 HTTP 硬超时生效。
5. 再做一次 live 验证：手动提交同步，检查任务从 `PENDING -> RUNNING -> SUCCESS/FAILED/TIMEOUT`，并检查请求日志 `STARTED -> 终态`。

## 远端执行补充

- 已按用户确认执行 `20260612_upstream_sync_task_lifecycle.sql` 和 `20260612_upstream_sync_stuck_batch_reconcile.sql`。
- 目标库：`gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com:28634/fenxiao`，连接来源为 `.env.local` 的 `RUOYI_DB_*`，未记录任何密码、token 或 secret。
- 执行前 guard 预览：dispatcher `sys_job` 目标行数 `0`，权限菜单目标行数 `0`，历史 `SYNCING` 批次 `32` 条，批次签名 `42bbdebc8c81da21541d1add698755773a21839d7f635c7212901e1563caf22d`。
- 执行后远端状态：`upstream_system_sync_request`、`upstream_system_sync_task` 已创建；`integration_sync_task_status` 字典 `8` 条；`integration_sync_trigger_source` 字典 `3` 条；Quartz dispatcher `job_id=109` 已启用；任务权限按钮 `2324/2325/2326` 已写入。
- 历史 stuck 收口结果：`upstream_system_sync_batch` 剩余 `SYNCING=0`，状态分布为 `FAILED=52`、`FRESH=1115`、`TIMEOUT=32`；三张同步状态表均无残留 `SYNCING`。
- 现场补充修正：将 `UpstreamSyncDispatchTask` 显式命名为 `@Component("upstreamSyncDispatchTask")`，并重新打包 `ruoyi-admin.jar` 后重启；11:21:30、11:22:00、11:22:30 的 `upstreamSyncDispatchTask.dispatch` job log 均为成功状态 `0`。
- API 验证：`/login` 成功；`/integration/admin/upstream-systems/list` 返回 `total=5`；`LX-CA012` 的 `/sync-tasks/list` 和 `/sync-requests/list` 均返回 HTTP `200`。
- 详细执行记录见 `docs/reports/2026-06-12-upstream-sync-live-execution-record.md`。
