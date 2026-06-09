-- 币种官方汇率定时同步任务
-- 说明：
-- 1. Quartz 每分钟触发一次轻量检查。
-- 2. 真实外部接口调用由后端任务按“汇率基准时间 + 1 分钟”触发。
-- 3. 未找到基准时间之后的数据时，每 15 分钟重试一次，最多重试 4 次。
-- 4. 多次重试仍失败时不更新币种汇率，继续保留上次成功汇率；次日重新开始。

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_currency_rate_sync_job := coalesce(@confirm_currency_rate_sync_job, '');
set @currency_rate_sync_job_expected_count :=
    coalesce(@currency_rate_sync_job_expected_count, '');
set @currency_rate_sync_job_expected_signature :=
    coalesce(@currency_rate_sync_job_expected_signature, '');
set @currency_rate_sync_config_expected_count :=
    coalesce(@currency_rate_sync_config_expected_count, '');
set @currency_rate_sync_config_expected_signature :=
    coalesce(@currency_rate_sync_config_expected_signature, '');

delimiter //

drop procedure if exists assert_currency_rate_sync_job_confirmed//
create procedure assert_currency_rate_sync_job_confirmed()
begin
  if coalesce(@confirm_currency_rate_sync_job, '')
      <> 'APPLY_CURRENCY_RATE_SYNC_JOB' then
    signal sqlstate '45000' set message_text = 'set @confirm_currency_rate_sync_job = APPLY_CURRENCY_RATE_SYNC_JOB before running this migration';
  end if;

  if coalesce(@currency_rate_sync_job_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @currency_rate_sync_job_expected_count after previewing exact currency rate sys_job row';
  end if;
  if coalesce(@currency_rate_sync_job_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @currency_rate_sync_job_expected_signature after previewing exact currency rate sys_job row';
  end if;
  if coalesce(@currency_rate_sync_config_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @currency_rate_sync_config_expected_count after previewing exact SHOWAPI_BANK_RATE sync config row';
  end if;
  if coalesce(@currency_rate_sync_config_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @currency_rate_sync_config_expected_signature after previewing exact SHOWAPI_BANK_RATE sync config row';
  end if;
end//

drop procedure if exists assert_currency_rate_sync_job_targets//
create procedure assert_currency_rate_sync_job_targets()
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
  where invoke_target = 'currencyRateSyncTask.syncDailyRates';

  if v_count > 1 then
    signal sqlstate '45000' set message_text = 'currency rate sync sys_job invoke_target must be unique before upsert';
  end if;
  if v_count <> cast(@currency_rate_sync_job_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'currency rate sync sys_job exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@currency_rate_sync_job_expected_signature) then
    signal sqlstate '45000' set message_text = 'currency rate sync sys_job exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_currency_rate_sync_config_targets//
create procedure assert_currency_rate_sync_config_targets()
begin
  declare v_count int default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             sync_config_id,
             coalesce(provider_code, ''),
             coalesce(provider_name, ''),
             coalesce(base_currency_code, ''),
             coalesce(schedule_type, ''),
             coalesce(sync_enabled, ''),
             coalesce(status, '')
           )
           order by sync_config_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from finance_currency_sync_config
  where provider_code = 'SHOWAPI_BANK_RATE';

  if v_count > 1 then
    signal sqlstate '45000' set message_text = 'SHOWAPI_BANK_RATE sync config provider_code must be unique before update';
  end if;
  if v_count <> cast(@currency_rate_sync_config_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'SHOWAPI_BANK_RATE sync config exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@currency_rate_sync_config_expected_signature) then
    signal sqlstate '45000' set message_text = 'SHOWAPI_BANK_RATE sync config exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_currency_rate_sync_job_confirmed();
call assert_currency_rate_sync_job_targets();
call assert_currency_rate_sync_config_targets();

start transaction;

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
WHERE invoke_target = 'currencyRateSyncTask.syncDailyRates';

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
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_job
    WHERE invoke_target = 'currencyRateSyncTask.syncDailyRates'
);

UPDATE finance_currency_sync_config
SET sync_enabled = '1',
    schedule_type = 'DAILY',
    update_by = 'admin',
    update_time = NOW()
WHERE provider_code = 'SHOWAPI_BANK_RATE';

commit;

drop procedure if exists assert_currency_rate_sync_job_confirmed;
drop procedure if exists assert_currency_rate_sync_job_targets;
drop procedure if exists assert_currency_rate_sync_config_targets;
