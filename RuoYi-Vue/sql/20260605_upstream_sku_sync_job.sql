-- Lingxing SKU scheduled sync job
-- Scope:
-- 1. Register the Quartz entrypoint in RuoYi sys_job.
-- 2. Trigger every 10 minutes.
-- 3. Do not trigger missed executions immediately, to avoid external API bursts.
-- 4. Disallow concurrent execution. The service also guards per connection.

set names utf8mb4;

set @confirm_upstream_sku_sync_job := coalesce(@confirm_upstream_sku_sync_job, '');

delimiter //

drop procedure if exists assert_upstream_sku_sync_job_confirmed//
create procedure assert_upstream_sku_sync_job_confirmed()
begin
  if coalesce(@confirm_upstream_sku_sync_job, '')
      <> 'APPLY_UPSTREAM_SKU_SYNC_JOB' then
    signal sqlstate '45000' set message_text = 'set @confirm_upstream_sku_sync_job = APPLY_UPSTREAM_SKU_SYNC_JOB before running this migration';
  end if;
end//

delimiter ;

call assert_upstream_sku_sync_job_confirmed();
drop procedure if exists assert_upstream_sku_sync_job_confirmed;

SET @job_id := (
    SELECT job_id
    FROM sys_job
    WHERE invoke_target = 'upstreamSystemTask.syncSkus'
    LIMIT 1
);

UPDATE sys_job
SET job_name = '领星SKU每10分钟同步',
    job_group = 'SYSTEM',
    invoke_target = 'upstreamSystemTask.syncSkus',
    cron_expression = '0 0/10 * * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = NOW(),
    remark = '每10分钟同步已启用领星主仓的SKU清单，复用上游系统管理同步服务。'
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
    '领星SKU每10分钟同步',
    'SYSTEM',
    'upstreamSystemTask.syncSkus',
    '0 0/10 * * * ?',
    '3',
    '1',
    '0',
    'admin',
    NOW(),
    '每10分钟同步已启用领星主仓的SKU清单，复用上游系统管理同步服务。'
WHERE @job_id IS NULL;
