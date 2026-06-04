# 币种汇率定时同步实现记录

## 目标

- 汇率同步不在前端轮询，也不在同步设置页面循环调用。
- 后端通过若依 Quartz 定时任务触发。
- 用户配置“汇率基准时间”后，系统在该时间 + 1 分钟发起当天第一次拉取。
- 如果外部数据中没有当天大于等于基准时间的汇率，则每 15 分钟重试一次。
- 初次尝试后最多重试 4 次；仍然没有符合条件的数据时，不更新当前汇率，继续使用上次成功汇率。
- 次日按新的日期重新开始同样的尝试和重试流程。

## 实现方案

- 新增 `currencyRateSyncTask.syncDailyRates` 作为若依 Quartz 任务入口。
- `sys_job` cron 使用 `0 * * * * ?` 每分钟触发一次轻量检查。
- 任务入口只在满足到点条件时调用 `IFinanceCurrencyService.syncRates()`，不会每分钟请求外部 API。
- 当天调度状态写入 Redis：
  - `finance:currency:scheduled-sync:{yyyyMMdd}:attempts`
  - `finance:currency:scheduled-sync:{yyyyMMdd}:completed`
- Redis key 按日期隔离并设置 2 天有效期，次日自然重新开始。
- Quartz 任务使用 `concurrent = 1`，禁止同一个任务并发执行。
- 简化后的同步设置页面不再暴露独立的自动同步开关，后端保存配置时固定 `sync_enabled = 1`；是否停用由配置 `status` 控制。

## 关键规则

- 首次拉取时间：`rate_anchor_time + 1 minute`。
- 重试间隔：15 分钟。
- 最大尝试次数：1 次首次拉取 + 4 次重试 = 5 次。
- “未找到当天汇率基准时间之后的官方汇率”会进入业务重试逻辑，并在重试耗尽前由任务入口吞掉异常。
- 重试耗尽后标记当天完成，不再继续调用外部 API，保留 `finance_currency` 中上次成功汇率。
- 其他异常会抛给 Quartz 任务日志处理，同时仍受当天尝试次数和 15 分钟窗口限流，避免每分钟重复调用外部 API。

## 涉及文件

- `RuoYi-Vue/finance/src/main/java/com/ruoyi/finance/task/CurrencyRateSyncTask.java`
- `RuoYi-Vue/finance/src/main/java/com/ruoyi/finance/task/CurrencyRateSyncSchedulePolicy.java`
- `RuoYi-Vue/finance/src/test/java/com/ruoyi/finance/task/CurrencyRateSyncSchedulePolicyTest.java`
- `RuoYi-Vue/ruoyi-common/src/main/java/com/ruoyi/common/constant/Constants.java`
- `RuoYi-Vue/sql/20260604_currency_rate_sync_job.sql`
- `docs/architecture/reuse-ledger.md`

## 数据库执行状态

- 已生成并执行 `RuoYi-Vue/sql/20260604_currency_rate_sync_job.sql`。
- 执行时间：2026-06-04 15:29 左右。
- 目标环境：当前激活的远端 MySQL，连接来源为本机 `.env.local` 中的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`。
- 命令类型：通过本机临时 Java + MySQL JDBC 执行 SQL 脚本。
- 数据影响：
  - 新增或更新 `sys_job` 中 `currencyRateSyncTask.syncDailyRates` 定时任务。
  - 将 `finance_currency_sync_config` 中 `SHOWAPI_BANK_RATE` 配置更新为 `sync_enabled = 1`、`schedule_type = DAILY`。
- 安全边界：执行记录、SQL、日志查询均未写入或展示数据库密码、Redis 密码、token secret 或 ShowAPI appKey 明文。

## 验证结果

- 已执行 `mvn -DskipTests -pl ruoyi-quartz -am compile`，结果通过。
- 已执行 `mvn -DskipTests -pl ruoyi-admin -am compile`，结果通过。
- 任务入口移回 finance 模块并加入任务白名单后，已再次执行 `mvn -DskipTests -pl ruoyi-admin -am compile`，结果通过。
- 已执行 `mvn -pl finance test`，4 个调度规则单元测试通过。
- 已执行 `npm run tsc`，结果通过。
- 执行远端 SQL 后，已只读验证 `sys_job`：
  - `job_name = 币种官方汇率每日同步`
  - `job_group = SYSTEM`
  - `invoke_target = currencyRateSyncTask.syncDailyRates`
  - `cron_expression = 0 * * * * ?`
  - `misfire_policy = 3`
  - `concurrent = 1`
  - `status = 0`
- 已只读验证同步配置：
  - `provider_code = SHOWAPI_BANK_RATE`
  - `sync_enabled = 1`
  - `schedule_type = DAILY`
  - `status = 0`
- 已重新打包并重启后端：
  - `mvn -DskipTests install` 因旧 jar 被运行中后端锁定，在 `ruoyi-admin` repackage 阶段失败。
  - 停止 8080 后端进程后，执行 `mvn -DskipTests -rf :ruoyi-admin install`，结果通过。
  - 执行 `.\start-backend-local.ps1 -Restart` 后，`http://127.0.0.1:8080` 返回 HTTP 200。
- 已验证 Quartz 运行时实际执行：
  - `finance_currency_sync_log` 最新记录：`SUCCESS`，返回币种数 29，更新币种数 3，请求时间 `2026-06-04 15:31:00`。
  - `sys_job_log` 最新记录：`status = 0`，开始时间 `2026-06-04 15:31:00`，结束时间 `2026-06-04 15:31:01`。
  - `finance_currency` 中 `CNY`、`EUR`、`USD` 已更新为官方汇率时间 `2026-06-04 15:21:22` 的同步结果。
- 已用 UTF-8 读取新增 Java、SQL、Markdown 文件，确认中文内容未损坏。

## 权限与边界检查

- 本次没有新增 Controller 接口，因此没有新增接口权限点。
- Quartz 任务入口放在财务模块的 `com.ruoyi.finance.task` 包下，并已加入若依任务白名单。
- 任务入口只调用 `IFinanceCurrencyService.syncRates()`，不绕过财务模块直接写汇率表。
- ShowAPI appKey 仍走既有加密保存逻辑，新增任务和 SQL 没有写入明文密钥。
- SQL 已经确认执行，后端已重启，Quartz 已装载并成功执行该任务。
