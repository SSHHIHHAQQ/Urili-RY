# 上游系统定时任务组件拆分执行记录

日期：2026-06-07
项目：`E:\Urili-Ruoyi`
范围：上游系统管理的若依 Quartz 定时任务入口

## 背景

定时任务菜单中，领星仓库、物流渠道、SKU、库存、SKU 仓库尺寸重量几个任务都挂在 `upstreamSystemTask.*` 下。这样虽然可以执行，但所有任务入口揉在同一个 Bean 里，菜单和后续维护都不清楚。

## 根因

- `sys_job.invoke_target` 统一指向 `upstreamSystemTask.*`。
- `UpstreamSystemTask` 同时承担多个同步入口和公共执行逻辑。
- 若依 Quartz 白名单只允许 `com.ruoyi.quartz.task`、`com.ruoyi.finance.task`，新增 integration 独立任务 Bean 后，直接修改任务会被白名单拒绝。

## 已修改

### 后端任务入口

新增每类同步一个独立组件：

- `upstreamWarehouseSyncTask.sync`：领星仓库每日同步。
- `upstreamLogisticsChannelSyncTask.sync`：领星物流渠道每日同步。
- `upstreamSkuInfoSyncTask.sync`：领星 SKU 基础信息每日同步。
- `upstreamSkuDimensionSyncTask.sync`：领星 SKU 仓库尺寸重量每日限速同步。
- `upstreamInventorySyncTask.sync`：领星 SKU 库存每 10 分钟同步。

新增 `UpstreamScheduledSyncExecutor` 承载公共调度边界：

- 只筛选启用的领星主仓。
- 在当前 JVM 内串行执行上游同步，避免多个同步任务在同一进程内互相抢资源。
- 汇总成功、跳过和失败连接。
- 任一连接失败时抛出受控 `ServiceException`，让若依任务日志能看到失败。

`UpstreamSystemTask` 改为历史兼容入口：

- 保留旧方法名。
- 只转发到新的独立 Task。
- 新任务不再继续往这个类里塞方法。

### 若依白名单

`Constants.JOB_WHITELIST_STR` 增加：

```text
com.ruoyi.integration.task
```

这样若依定时任务菜单可以合法保存 integration 模块的任务 Bean。

### 当前库任务记录

通过若依 `/monitor/job` 接口更新当前运行库的任务配置，没有直接手写连接库 SQL。

| jobId | 调用方法 | cron |
| --- | --- | --- |
| 101 | `upstreamSkuInfoSyncTask.sync` | `0 40 23 * * ?` |
| 102 | `upstreamInventorySyncTask.sync` | `0 0/10 * * * ?` |
| 103 | `upstreamWarehouseSyncTask.sync` | `0 20 23 * * ?` |
| 104 | `upstreamLogisticsChannelSyncTask.sync` | `0 30 23 * * ?` |
| 105 | `upstreamSkuDimensionSyncTask.sync` | `0 59 23 * * ?` |

同时补充 SQL 迁移脚本：

```text
RuoYi-Vue/sql/20260607_upstream_task_component_split.sql
```

用于其他环境同步同样的 `sys_job.invoke_target` 变更。

## 验证

已执行：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
mvn -DskipTests package
```

结果：

- integration 模块编译通过。
- 后端整体打包通过。

已重启后端：

```powershell
cd E:\Urili-Ruoyi
.\start-backend-local.ps1
```

已通过若依 API 验证任务列表：

```text
101  领星SKU信息每日同步              upstreamSkuInfoSyncTask.sync
102  领星SKU库存每10分钟同步          upstreamInventorySyncTask.sync
103  领星仓库每日同步                 upstreamWarehouseSyncTask.sync
104  领星物流渠道每日同步             upstreamLogisticsChannelSyncTask.sync
105  领星SKU仓库尺寸重量每日限速同步  upstreamSkuDimensionSyncTask.sync
```

已手动执行轻量任务：

```text
PUT /monitor/job/run
jobId = 103
```

结果：

```text
RUN {"msg":"操作成功","code":200}
4462  领星仓库每日同步  upstreamWarehouseSyncTask.sync  status=0
```

已更新 CodeGraph：

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：

```text
Already up to date
```

## 未执行

- 未手动执行 SKU 仓库尺寸重量同步，原因是该任务会按 SKU 拉 WMS 尺寸重量，属于 CPU 和外部请求压力更高的任务。
- 未手动执行库存同步，原因是该任务本身 10 分钟周期运行，当前只需要验证 Quartz 可以识别新 Bean。
- Maven 本次使用 `-DskipTests`，只做编译和打包验证，没有跑全量测试。

## 权限与日志检查

- 任务仍然使用若依原生 `系统监控 / 定时任务` 能力。
- 启停、编辑、执行一次、调度日志仍受若依 `monitor:job:*` 权限控制。
- 调度日志写入若依 `sys_job_log`。
- 上游接口请求日志仍走原有上游系统请求日志链路，没有新增第二套外部请求日志。

## 复用检查

- 领星签名、HTTP 请求、请求日志、落库仍复用 `LingxingOpenApiClient` 和 `IUpstreamSystemService` 分项同步方法。
- 新增公共执行器只处理 Quartz 调度边界，不承载领星业务协议。
- 复用台账已同步更新，后续新增同步任务应新建独立 Task 并复用 `UpstreamScheduledSyncExecutor`。

## 残留风险

- 旧 `upstreamSystemTask.*` 仍保留兼容转发，短期是为了避免已有环境未迁移时任务直接失效；后续确认所有环境都已迁移后，可以再删除旧入口。
- SKU 仓库尺寸重量同步仍是高成本任务，应继续保持每日 23:59 限速执行，不建议从定时任务菜单频繁手动触发。
