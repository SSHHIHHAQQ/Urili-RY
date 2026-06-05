# 上游 SKU 定时同步任务漏项修复记录

日期：2026-06-05

## 问题

“系统监控 / 定时任务”页面没有显示 10 分钟同步领星 SKU 的任务。

## 原因

- 上一阶段只实现了手动 SKU 同步接口 `POST /integration/admin/upstream-systems/{connectionCode}/skus/sync`。
- 上一阶段已在阶段总结中记录残留问题：“本轮未接若依定时任务做 10 分钟自动 SKU 同步”。
- 若依定时任务页面读取的是 `sys_job`；当前没有 `upstreamSystemTask.syncSkus` 的 Quartz Bean 和 `sys_job` 任务登记，所以页面不会显示该任务。

## 修复内容

- 新增 `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamSystemTask.java`。
- 新增 Quartz 调用入口：`upstreamSystemTask.syncSkus`。
- 任务逻辑：
  - 查询所有已启用主仓接入。
  - 只处理 `lingxing-wms` 和兼容旧值 `LINGXING_WMS`。
  - 逐个复用 `IUpstreamSystemService.syncSkusOnly(connectionCode)`。
  - 单个主仓失败后继续处理其他主仓，最后统一让若依任务日志记录失败状态。
- 新增幂等 SQL：`RuoYi-Vue/sql/20260605_upstream_sku_sync_job.sql`。
- 更新复用台账：`docs/architecture/reuse-ledger.md`。

## sys_job 设计

| 字段 | 值 |
| --- | --- |
| `job_name` | `领星SKU每10分钟同步` |
| `job_group` | `SYSTEM` |
| `invoke_target` | `upstreamSystemTask.syncSkus` |
| `cron_expression` | `0 0/10 * * * ?` |
| `misfire_policy` | `3`，错过触发不立即补跑，避免外部接口突刺 |
| `concurrent` | `1`，禁止并发执行 |
| `status` | `0`，正常 |

## 数据库执行状态

- 已按当前激活 `druid` 数据源执行 `RuoYi-Vue/sql/20260605_upstream_sku_sync_job.sql`。
- 目标库：当前运行库 `fenxiao`。
- 执行结果：`sys_job` 中已存在 `job_id = 101`，`invoke_target = 'upstreamSystemTask.syncSkus'`。
- 该 SQL 只新增或更新 `sys_job` 中 `invoke_target = 'upstreamSystemTask.syncSkus'` 的任务，未改表结构，未清空或迁移 SKU 清单数据。

## 验证计划

| 验证项 | 命令/动作 | 预期 |
| --- | --- | --- |
| 后端模块编译 | `mvn -pl integration -am -DskipTests compile` | 通过 |
| 后端全量打包 | `mvn -DskipTests install` | 通过 |
| CodeGraph | `codegraph sync .` | 通过 |
| 数据库只读验证 | 查询 `sys_job` 的 `upstreamSystemTask.syncSkus` | SQL 执行后存在且状态正常 |
| 页面验证 | 打开 `/monitor/job` | SQL 执行后显示“领星SKU每10分钟同步” |

## 验证结果

| 验证项 | 结果 |
| --- | --- |
| `mvn -pl integration -am -DskipTests compile` | 通过 |
| `mvn -DskipTests install` | 首次因运行中的 `ruoyi-admin.jar` 锁文件失败；停止旧后端后复跑通过 |
| 后端重启 | 已通过 `start-backend-local.ps1 -Restart` 重启，8080 `/captchaImage` 返回 HTTP 200 |
| `codegraph sync .` | 通过，首次 `Synced 9 changed files`，终态复跑 `Synced 1 changed files` |
| 数据库执行 | 已执行，`job_id=101` |
| 页面验证 | `/monitor/job` 已显示 `领星SKU每10分钟同步 / upstreamSystemTask.syncSkus / 0 0/10 * * * ? / 正常` |
| 自动任务日志 | `sys_job_log` 已记录 15:20 和 15:30 两次执行，状态均为 `0` |
| SKU 同步状态 | `LX-CA012` 与 `LX-NY013-3275A1E1` 均为 `FRESH` |

## 运行验证明细

| 项 | 结果 |
| --- | --- |
| 15:20 自动执行 | 成功，`job_log_id=1854`，15:20:00 开始，15:23:18 结束 |
| 15:30 自动执行 | 成功，`job_log_id=1873`，15:30:00 开始，15:33:00 结束 |
| `LX-CA012` 最新状态 | `FRESH`，15:30:00 开始，15:31:34 成功结束 |
| `LX-NY013-3275A1E1` 最新状态 | `FRESH`，15:31:34 开始，15:33:00 成功结束 |
| 手动执行一次 | 15:25 通过页面触发过一次；期间后端重启导致该次未完整落任务日志，后续 15:30 自动任务已把状态修正为成功 |

## 当前结论

这是实现漏项：SKU 同步能力本身已实现，但之前没有按若依 Quartz 方式接入 `sys_job`。当前已补齐，并已验证定时任务可见且自动执行成功。
