# 2026-06-12 上游同步队列远端执行记录

## 执行边界

- 执行事项：上游同步任务生命周期迁移、历史 `SYNCING` 批次收口。
- 用户确认：`确认 执行`。
- 连接来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 激活 `druid`，`application-druid.yml` 通过 `.env.local` 的 `RUOYI_DB_*` 连接数据库。
- 目标 MySQL：`gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com:28634/fenxiao`。
- 目标 Redis：`114.132.156.75:6379`，logical DB `1`。
- 凭证处理：本记录不保存、不展示 `.env.local` 中的密码、token、secret。

## 执行脚本

- `RuoYi-Vue/sql/20260612_upstream_sync_task_lifecycle.sql`
  - 影响类型：远端 DDL + 受 guard 保护的字典、Quartz 任务、权限菜单 DML。
  - 确认变量：`APPLY_UPSTREAM_SYNC_TASK_LIFECYCLE`。
- `RuoYi-Vue/sql/20260612_upstream_sync_stuck_batch_reconcile.sql`
  - 影响类型：远端 DML，只把预览确认的历史 `SYNCING` 批次收口为 `TIMEOUT`，不删除历史。
  - 确认变量：`RECONCILE_UPSTREAM_SYNC_STUCK_BATCHES`。

## 执行前预览

| 项目 | 结果 |
| --- | --- |
| 上游系统父菜单 `2031` | `1` |
| dispatcher `sys_job` 目标行数 | `0` |
| dispatcher `sys_job` 目标签名 | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |
| 任务权限菜单目标行数 | `0` |
| 任务权限菜单目标签名 | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |
| 历史 `SYNCING` 批次数 | `32` |
| 历史 `SYNCING` 批次签名 | `42bbdebc8c81da21541d1add698755773a21839d7f635c7212901e1563caf22d` |
| 执行前批次状态分布 | `FAILED=52`，`FRESH=1115`，`SYNCING=32` |

历史 `SYNCING` 精确批次 ID：

```text
04ef1dd7-9208-4b9d-8da4-add58b8b6e93,0671bbe6-bc4e-44d6-9062-03800d7a94f0,14522b58-c56d-4687-9907-a2f6fc2109c0,24baecca-457f-4c86-a5d9-eb8a68435fb1,292276cd-f2a0-4fe1-8fab-e9ba7dc8d216,2c0de1d7-1e91-4415-9e8e-5a68e615cd41,2ced2a7b-3e27-4305-b516-1268f2375aca,402c8f39-ef07-4e52-98ac-1a77c9a6465a,4a8f0e65-e62d-4fac-b715-015a92be4f5a,595798e2-8e1e-4f18-96c7-d86c84588785,6ca0ce2d-7e3f-4008-9c01-9d1b7792b803,76cee9fb-0fe3-444b-acad-a52b61d456bb,7850816b-8508-45e4-8dae-7da85574b2e2,79b3361f-65ed-4bad-b383-5027f88a8d04,84d92459-fdb0-4a32-89cc-b921a948340e,870cd6a4-e3e9-4de8-ad12-705f3a8fbdea,9ce71638-86b1-48d9-9c79-16090a6d2cdd,abb5c043-75f2-47c6-a232-f18874732a00,ae9eba05-448f-41e0-9218-3ade2da5f9c6,b46deb98-4a40-4b16-8d1d-d992cad0baa3,b61bbc85-b6c1-4217-b3e2-1a078c74c0d8,c3aa997a-f588-40f3-9841-3f4b3729dc27,c562f0fb-2393-4cc5-9be7-7b45b2f08721,c6741829-dc28-4268-9c9e-01d8db55718e,c6e768b2-dc8c-4dae-9361-a6656f03e797,d1348df7-a622-4867-b715-9a5c5c7bef11,e76feade-dd54-4f18-aeb1-bf5f026ee0dd,ebb2014a-125b-4639-ac96-c8feb6008321,ec1f80bf-2257-46b6-b1dd-638683182331,f0518887-279a-41d6-99e1-a487838cbcf9,f08a70a3-fab7-430d-833a-7dea906bfa39,f4631b21-ea54-4c78-ad61-2449ea615f48
```

## 执行结果

### 1. 生命周期脚本

- 执行结果：成功。
- 执行语句数：`26`。
- guard 结果：通过预览数量和 SHA-256 签名校验后执行。
- 实际落库：
  - 新建 `upstream_system_sync_request`。
  - 新建 `upstream_system_sync_task`。
  - 写入 `integration_sync_task_status` 字典数据 `8` 条。
  - 写入 `integration_sync_trigger_source` 字典数据 `3` 条。
  - 写入 RuoYi Quartz 任务 `upstreamSyncDispatchTask.dispatch`，`job_id=109`，cron `0/30 * * * * ?`，`concurrent=1`，`status=0`。
  - 写入任务权限按钮 `2324/2325/2326`，权限分别为 `integration:upstream:task:list`、`integration:upstream:task:retry`、`integration:upstream:task:cancel`。

### 2. 历史 `SYNCING` 收口脚本

- 执行结果：成功。
- 执行语句数：`20`。
- guard 结果：通过 32 个精确批次 ID、数量和 SHA-256 签名校验后执行。
- 执行后批次状态分布：`FAILED=52`，`FRESH=1115`，`TIMEOUT=32`。
- 剩余 `SYNCING` 批次数：`0`。
- `upstream_system_sync_state` 状态分布：`FRESH=21`，`TIMEOUT=4`，剩余 `SYNCING=0`。
- `upstream_system_sku_sync_state` 状态分布：`FRESH=5`，剩余 `SYNCING=0`。
- `upstream_system_inventory_sync_state` 状态分布：`FRESH=5`，剩余 `SYNCING=0`。
- 32 条 `TIMEOUT` 批次均保留收口说明：`历史同步任务长时间处于SYNCING，按确认方案收口为TIMEOUT`。

### 3. 运行态修正与重启

- 首次执行 `start-backend-local.ps1 -Restart` 后，远端 Quartz 已加载新 `sys_job`，但当时本地 `ruoyi-admin.jar` 仍是旧 jar，11:19:00、11:19:30、11:20:00 的 dispatcher 调度出现 `NoSuchBeanDefinitionException: No bean named 'upstreamSyncDispatchTask' available`。
- 处理方式：
  - 将 `UpstreamSyncDispatchTask` 的 Bean 名显式固定为 `@Component("upstreamSyncDispatchTask")`，与 RuoYi Quartz `invoke_target` 硬绑定。
  - 更新 `IntegrationModuleBoundaryContractTest`，固定任务 Bean 名必须匹配 `upstreamSyncDispatchTask.dispatch`。
  - 重新执行 `mvn -pl ruoyi-admin -am -DskipTests package` 打包 `RuoYi-Vue/ruoyi-admin/target/ruoyi-admin.jar`。
  - 再次执行 `start-backend-local.ps1 -Restart`。
- 最终运行态：
  - `http://127.0.0.1:8080/` 返回 HTTP `200`。
  - 8080 当前由 `E:\develop\jdk-21\bin\java.exe -jar E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-admin\target\ruoyi-admin.jar` 监听。
  - `sys_job_log` 显示 11:21:30、11:22:00、11:22:30 的 `upstreamSyncDispatchTask.dispatch` 均为成功状态 `0`，无异常信息。

### 4. API 验证

- `/captchaImage`：HTTP `200`，`captchaEnabled=false`。
- `/login`：HTTP `200`，`code=200`，token 获取成功。
- `/integration/admin/upstream-systems/list?pageNum=1&pageSize=10`：HTTP `200`，`code=200`，`total=5`。
- `/integration/admin/upstream-systems/LX-CA012/sync-tasks/list?pageNum=1&pageSize=10`：HTTP `200`，`code=200`，`total=0`。
- `/integration/admin/upstream-systems/LX-CA012/sync-requests/list?pageNum=1&pageSize=10`：HTTP `200`，`code=200`，`total=0`。

### 5. 本次补充验证命令

- `mvn -pl integration -am "-Dtest=IntegrationModuleBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：通过，`7` tests。
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：通过，`82` tests。
- `mvn -pl ruoyi-admin -am -DskipTests package`
  - 结果：通过，生成并重打包 `RuoYi-Vue/ruoyi-admin/target/ruoyi-admin.jar`。
