-- Lingxing SKU scheduled sync job
-- Scope:
-- 1. Register the Quartz entrypoint in RuoYi sys_job.
-- 2. Trigger every 10 minutes.
-- 3. Do not trigger missed executions immediately, to avoid external API bursts.
-- 4. Disallow concurrent execution. The service also guards per connection.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_upstream_sku_sync_job := coalesce(@confirm_upstream_sku_sync_job, '');
set @upstream_sku_sync_job_expected_count :=
    coalesce(@upstream_sku_sync_job_expected_count, '');
set @upstream_sku_sync_job_expected_signature :=
    coalesce(@upstream_sku_sync_job_expected_signature, '');

delimiter //

drop procedure if exists assert_upstream_sku_sync_job_confirmed//
create procedure assert_upstream_sku_sync_job_confirmed()
begin
  if coalesce(@confirm_upstream_sku_sync_job, '')
      <> 'APPLY_UPSTREAM_SKU_SYNC_JOB' then
    signal sqlstate '45000' set message_text = 'set @confirm_upstream_sku_sync_job = APPLY_UPSTREAM_SKU_SYNC_JOB before running this migration';
  end if;

  if coalesce(@upstream_sku_sync_job_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @upstream_sku_sync_job_expected_count after previewing exact upstream SKU sys_job row';
  end if;
  if coalesce(@upstream_sku_sync_job_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @upstream_sku_sync_job_expected_signature after previewing exact upstream SKU sys_job row';
  end if;
end//

drop procedure if exists assert_upstream_sku_sync_job_targets//
create procedure assert_upstream_sku_sync_job_targets()
begin
  declare v_count int default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             job_id,
             coalesce(job_name, ''),
             coalesce(job_group, ''),
             coalesce(invoke_target, ''),
             coalesce(cron_expression, ''),
             coalesce(misfire_policy, ''),
             coalesce(concurrent, ''),
             coalesce(status, '')
           )
           order by job_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from sys_job
  where invoke_target = 'upstreamSystemTask.syncSkus';

  if v_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream SKU sync sys_job invoke_target must be unique before upsert';
  end if;
  if v_count <> cast(@upstream_sku_sync_job_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'upstream SKU sync sys_job exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@upstream_sku_sync_job_expected_signature) then
    signal sqlstate '45000' set message_text = 'upstream SKU sync sys_job exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_upstream_sku_sync_job_confirmed();
call assert_upstream_sku_sync_job_targets();

start transaction;

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
WHERE invoke_target = 'upstreamSystemTask.syncSkus';

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
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_job
    WHERE invoke_target = 'upstreamSystemTask.syncSkus'
);

commit;

drop procedure if exists assert_upstream_sku_sync_job_confirmed;
drop procedure if exists assert_upstream_sku_sync_job_targets;
