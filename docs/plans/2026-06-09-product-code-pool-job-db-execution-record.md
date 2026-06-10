# 商城商品编码池定时任务数据库执行记录

## 目标

将商城商品 SPU/SKU 编码池补充任务写入若依 `sys_job`，使其能在若依管理端“定时任务”菜单中看到并由 Quartz 调度执行。

## 目标环境

- 数据库来源：`.env.local` 的 `RUOYI_DB_URL`
- 目标库：远端运行库，地址已脱敏
- Redis 来源：`.env.local` 的 `RUOYI_REDIS_*`
- Redis 目标：远端 Redis，地址已脱敏

## 用户确认与执行边界

- 用户确认来源：用户在当前任务中下达“执行”授权，并且三端快速推进目标已明确远程数据库 DDL/DML 可以执行。
- 本确认仅适用于 `RuoYi-Vue/sql/20260609_product_code_pool_job.sql` 在本记录列明范围内的一次执行；不得作为后续无确认重放依据。
- 本次只写若依 `sys_job` 调度配置，不修改商品事实数据，不写 Redis 编码池，不触发编码池补充任务。
- 本记录证明数据库任务配置写入结果；不等同于证明 Quartz 已按 cron 实际触发并成功补池。

## 执行脚本

- `RuoYi-Vue/sql/20260609_product_code_pool_job.sql`

## 预览结果

只读预览目标行：

```text
where invoke_target = 'productCodePoolTask.maintainPools'
count = 0
signature = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```

当前远端库没有该任务，因此若依“定时任务”页面不会展示商城商品编码池补充任务。

## 预期写入

写入或更新一条 `sys_job`：

| 字段 | 值 |
| --- | --- |
| job_name | 商城商品编码池每日补充 |
| job_group | SYSTEM |
| invoke_target | productCodePoolTask.maintainPools |
| cron_expression | 0 59 22 * * ? |
| misfire_policy | 3 |
| concurrent | 1 |
| status | 0 |
| remark | 每天22:59检查Redis中的SPU/SKU编码池容量，低于阈值时自动补充。 |

## 安全措施

- 脚本带确认变量 `@confirm_product_code_pool_job = 'APPLY_PRODUCT_CODE_POOL_JOB'`。
- 脚本带目标集合 count/signature 校验。
- 如果目标行数或签名与预览不一致，脚本会 `45000` fail-closed。
- 不涉及业务商品数据修改，不涉及编码池 Redis 数据修改。

## 影响范围

- 写入或更新 `sys_job` 中 `invoke_target = 'productCodePoolTask.maintainPools'` 的一条任务。
- 不改写商品 SPU/SKU 数据，不生成新编码，不读取或写入 Redis 编码池。
- 不改菜单、角色或权限。

## 回滚方式

- 默认不自动回滚。
- 如需回滚，只允许针对 `invoke_target = 'productCodePoolTask.maintainPools'` 的精确 `sys_job` 目标执行停用或删除。
- 回滚前必须重新预览目标 count/signature，确认没有其它脚本或人工修改接管该任务；不得按宽泛 `job_group` 或任务名称删除。
- 回滚不涉及商品事实数据或 Redis 编码池数据。

## 待确认执行参数

执行前确认链：本节参数是本次写入前的确认材料；执行时仅按下列确认变量、目标 count 和 signature 执行，不得作为后续无确认重放依据。

```sql
set @confirm_product_code_pool_job = 'APPLY_PRODUCT_CODE_POOL_JOB';
set @product_code_pool_job_expected_count = '0';
set @product_code_pool_job_expected_signature = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
source RuoYi-Vue/sql/20260609_product_code_pool_job.sql;
```

## 数据库写入结果

执行时间：2026-06-09

结果：

```text
updated = 0
inserted = 1
job_id = 108
invoke_target = productCodePoolTask.maintainPools
cron_expression = 0 59 22 * * ?
misfire_policy = 3
concurrent = 1
status = 0
```

首次执行时发现 PowerShell 管道传递中文给 Python 时显示为问号，已立即使用 Unicode 转义重新更新 `job_name` 和 `remark`，并通过 `hex(job_name)` / `hex(remark)` 确认真中文已写入数据库：

```text
job_name = 商城商品编码池每日补充
remark = 每天22:59检查Redis中的SPU/SKU编码池容量，低于阈值时自动补充。
job_name_hex_prefix = E59586E59F8EE59586
remark_hex_prefix = E6AF8FE5A4A932323A
```

## 验证边界

- 已验证：远端运行库 `sys_job` 目标任务写入结果、中文字段修正结果和目标字段值。
- 未验证：Quartz 是否已按 cron 实际触发、`productCodePoolTask.maintainPools` 是否完成一次真实补池、管理端任务页运行态展示。
- 未写 Redis，也未触发编码池补充任务。
