-- 币种官方汇率定时同步任务
-- 说明：
-- 1. Quartz 每分钟触发一次轻量检查。
-- 2. 真实外部接口调用由后端任务按“汇率基准时间 + 1 分钟”触发。
-- 3. 未找到基准时间之后的数据时，每 15 分钟重试一次，最多重试 4 次。
-- 4. 多次重试仍失败时不更新币种汇率，继续保留上次成功汇率；次日重新开始。

SET @job_id := (
    SELECT job_id
    FROM sys_job
    WHERE invoke_target = 'currencyRateSyncTask.syncDailyRates'
    LIMIT 1
);

UPDATE sys_job
SET job_name = '币种官方汇率每日同步',
    job_group = 'SYSTEM',
    cron_expression = '0 * * * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = NOW(),
    remark = '每分钟检查币种汇率同步计划，到汇率基准时间+1分钟后拉取；无基准后数据则15分钟重试一次，最多重试4次。'
WHERE @job_id IS NOT NULL
  AND job_id = @job_id;

INSERT INTO sys_job (
    job_name,
    job_group,
    invoke_target,
    cron_expression,
    misfire_policy,
    concurrent,
    status,
    create_by,
    create_time,
    remark
)
SELECT
    '币种官方汇率每日同步',
    'SYSTEM',
    'currencyRateSyncTask.syncDailyRates',
    '0 * * * * ?',
    '3',
    '1',
    '0',
    'admin',
    NOW(),
    '每分钟检查币种汇率同步计划，到汇率基准时间+1分钟后拉取；无基准后数据则15分钟重试一次，最多重试4次。'
WHERE @job_id IS NULL;

UPDATE finance_currency_sync_config
SET sync_enabled = '1',
    schedule_type = 'DAILY',
    update_by = 'admin',
    update_time = NOW()
WHERE provider_code = 'SHOWAPI_BANK_RATE';
