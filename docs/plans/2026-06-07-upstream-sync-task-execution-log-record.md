# 上游同步任务执行日志写入记录

## 背景

上游系统管理已有两类日志能力：

- 若依 Quartz 的 `sys_job_log`：由若依定时任务框架在任务执行后自动写入。
- 上游系统请求日志 `upstream_system_request_log`：由 `LingxingOpenApiClient` 记录领星 HTTP 请求明细。

本次问题是上游系统页面的请求日志缺少“一次同步任务执行摘要”，只能看到外部接口请求明细，不方便判断某次定时同步任务整体成功、失败、耗时和数量。

## 本次修改

1. 新增 `IUpstreamSystemService.syncScheduled(connectionCode, syncType)`。
2. 定时任务组件统一调用 `syncScheduled(...)`，同步状态和同步批次的 `mode` 写为 `SCHEDULED`。
3. 在 `UpstreamSystemServiceImpl.executeSyncItem(...)` 成功和失败出口写入一条 `upstream_system_request_log` 摘要日志。
4. 摘要日志内容包括：
   - `trace_id`：同步批次号 `syncBatchId`
   - `operation`：`TASK_WAREHOUSE_SYNC` / `TASK_LOGISTICS_CHANNEL_SYNC` / `TASK_SKU_SYNC` / `TASK_SKU_DIMENSION_SYNC` / `TASK_INVENTORY_SYNC`
   - `endpoint`：`upstream-sync://{mode}/{syncType}`
   - `request_payload_redacted`：同步批次、同步类型、执行模式
   - `response_payload_redacted`：拉取、新增、变更、未变更、停用、结果数量和错误信息
   - `status`：`SUCCESS` / `FAILURE`
5. 前端请求日志类型映射补充中文展示：
   - 仓库同步任务
   - 物流渠道同步任务
   - SKU信息同步任务
   - SKU仓库尺寸重量同步任务
   - SKU库存同步任务

## 未修改

- 未新增表。
- 未修改若依 Quartz `sys_job_log` 写入逻辑；该逻辑仍由 `AbstractQuartzJob` 负责。
- 未手动触发真实领星同步，避免在验证阶段额外写远端业务数据。

## 验证

| 验证项 | 结果 |
| --- | --- |
| 后端 `integration` 及依赖模块真实重新编译 | 通过：`mvn -pl integration -am -DskipTests clean compile` |
| 前端类型检查 | 通过：`npm run tsc` |
| Git 空白检查 | 通过，仅有 CRLF 提示 |
| CodeGraph 同步 | 已执行：`codegraph sync .`，结果为 already up to date |
| 前端 Biome 指定文件检查 | 未执行到文件，`src/services/integration/constants.ts` 被当前 Biome 配置忽略 |

## 后续验证建议

等待下一次 Quartz 自动执行，或在定时任务菜单手动执行任意一个上游同步任务后，检查：

```sql
select request_log_id, connection_code, trace_id, operation, endpoint, status,
       duration_ms, create_time
from upstream_system_request_log
where operation like 'TASK_%'
order by request_log_id desc
limit 20;
```

预期能看到对应的 `TASK_*` 摘要日志，同时若依定时任务日志仍能在 `系统监控 / 定时任务 / 调度日志` 中查看。
