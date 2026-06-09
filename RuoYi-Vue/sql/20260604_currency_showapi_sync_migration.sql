-- Currency ShowAPI sync simplification migration.
-- Scope: add rate anchor time and switch existing sync config metadata to fixed ShowAPI bank rate provider.
-- Sensitive appKey must not be stored in this SQL file; save it through the backend encrypted config API.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_currency_showapi_sync_migration := coalesce(@confirm_currency_showapi_sync_migration, '');
set @currency_showapi_sync_config_expected_count := coalesce(@currency_showapi_sync_config_expected_count, '');
set @currency_showapi_sync_config_expected_signature := coalesce(@currency_showapi_sync_config_expected_signature, '');
set @currency_showapi_currency_expected_count := coalesce(@currency_showapi_currency_expected_count, '');
set @currency_showapi_currency_expected_signature := coalesce(@currency_showapi_currency_expected_signature, '');
set @currency_showapi_dict_expected_count := coalesce(@currency_showapi_dict_expected_count, '');
set @currency_showapi_dict_expected_signature := coalesce(@currency_showapi_dict_expected_signature, '');

delimiter //

drop procedure if exists assert_currency_showapi_sync_migration_confirmed//
create procedure assert_currency_showapi_sync_migration_confirmed()
begin
  if coalesce(@confirm_currency_showapi_sync_migration, '')
      <> 'APPLY_CURRENCY_SHOWAPI_SYNC_MIGRATION' then
    signal sqlstate '45000' set message_text = 'set @confirm_currency_showapi_sync_migration = APPLY_CURRENCY_SHOWAPI_SYNC_MIGRATION before running this migration';
  end if;

  if coalesce(@currency_showapi_sync_config_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @currency_showapi_sync_config_expected_count after previewing exact finance_currency_sync_config targets';
  end if;

  if coalesce(@currency_showapi_sync_config_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @currency_showapi_sync_config_expected_signature after previewing exact finance_currency_sync_config targets';
  end if;

  if coalesce(@currency_showapi_currency_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @currency_showapi_currency_expected_count after previewing exact finance_currency targets';
  end if;

  if coalesce(@currency_showapi_currency_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @currency_showapi_currency_expected_signature after previewing exact finance_currency targets';
  end if;

  if coalesce(@currency_showapi_dict_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @currency_showapi_dict_expected_count after previewing exact sys_dict_data currency_code targets';
  end if;

  if coalesce(@currency_showapi_dict_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @currency_showapi_dict_expected_signature after previewing exact sys_dict_data currency_code targets';
  end if;
end//

drop procedure if exists add_column_if_missing//
create procedure add_column_if_missing(in p_table varchar(64), in p_column varchar(64), in p_definition text)
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table
      and column_name = p_column
  ) then
    set @ddl = concat('alter table ', p_table, ' add column ', p_column, ' ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_showapi_provider_conflict_absent//
create procedure assert_showapi_provider_conflict_absent()
begin
  if exists (select 1 from finance_currency_sync_config where provider_code = 'GENERIC_RATES')
     and exists (select 1 from finance_currency_sync_config where provider_code = 'SHOWAPI_BANK_RATE') then
    signal sqlstate '45000' set message_text = 'finance currency sync config has both GENERIC_RATES and SHOWAPI_BANK_RATE; resolve provider_code conflict before migration';
  end if;
end//

drop procedure if exists assert_currency_showapi_sync_targets//
create procedure assert_currency_showapi_sync_targets()
begin
  declare v_count bigint default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':', target_action, provider_code, provider_name, base_currency_code, sync_enabled, status)
           order by target_action, provider_code separator '|'
         ), ''), 256)
    into v_count, v_signature
  from (
      select 'UPDATE' as target_action,
             provider_code,
             coalesce(provider_name, '') as provider_name,
             coalesce(base_currency_code, '') as base_currency_code,
             coalesce(sync_enabled, '') as sync_enabled,
             coalesce(status, '') as status
      from finance_currency_sync_config
      where provider_code = 'GENERIC_RATES'
      union all
      select 'INSERT', 'SHOWAPI_BANK_RATE', '', '', '', ''
      where not exists (
          select 1 from finance_currency_sync_config where provider_code = 'SHOWAPI_BANK_RATE'
      )
  ) targets;

  if v_count <> cast(@currency_showapi_sync_config_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'currency ShowAPI sync config exact target count mismatch';
  end if;

  if lower(v_signature) <> lower(@currency_showapi_sync_config_expected_signature) then
    signal sqlstate '45000' set message_text = 'currency ShowAPI sync config exact target signature mismatch';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':', currency_code, coalesce(base_currency_code, ''),
             coalesce(cast(official_rate as char), ''), coalesce(cast(effective_rate as char), ''),
             coalesce(is_default, ''))
           order by currency_code separator '|'
         ), ''), 256)
    into v_count, v_signature
  from finance_currency
  where base_currency_code <> 'CNY'
     or (currency_code = 'USD' and official_rate = 1.0000000000)
     or (currency_code = 'CNY' and (official_rate is null or effective_rate is null))
     or (currency_code in ('CNY', 'USD')
         and coalesce(is_default, '') <> case when currency_code = 'CNY' then 'Y' else 'N' end);

  if v_count <> cast(@currency_showapi_currency_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'currency ShowAPI finance_currency exact target count mismatch';
  end if;

  if lower(v_signature) <> lower(@currency_showapi_currency_expected_signature) then
    signal sqlstate '45000' set message_text = 'currency ShowAPI finance_currency exact target signature mismatch';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':', dict_code, dict_value, coalesce(is_default, ''))
           order by dict_code separator '|'
         ), ''), 256)
    into v_count, v_signature
  from sys_dict_data
  where dict_type = 'currency_code'
    and dict_value in ('CNY', 'USD')
    and coalesce(is_default, '') <> case when dict_value = 'CNY' then 'Y' else 'N' end;

  if v_count <> cast(@currency_showapi_dict_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'currency ShowAPI sys_dict_data exact target count mismatch';
  end if;

  if lower(v_signature) <> lower(@currency_showapi_dict_expected_signature) then
    signal sqlstate '45000' set message_text = 'currency ShowAPI sys_dict_data exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_currency_showapi_sync_completed//
create procedure assert_currency_showapi_sync_completed()
begin
  if exists (select 1 from finance_currency_sync_config where provider_code = 'GENERIC_RATES') then
    signal sqlstate '45000' set message_text = 'currency ShowAPI sync migration completed with stale GENERIC_RATES provider';
  end if;

  if not exists (
      select 1
      from finance_currency_sync_config
      where provider_code = 'SHOWAPI_BANK_RATE'
        and provider_name = 'ShowAPI银行汇率查询'
        and base_currency_code = 'CNY'
        and api_base_url = 'https://route.showapi.com/105-30'
        and auth_type = 'APP_KEY'
        and request_timeout_ms = 10000
        and retry_count = 0
        and schedule_type = 'DAILY'
        and cron_expression = ''
        and rate_anchor_time = '09:30:00'
        and status = '0'
  ) then
    signal sqlstate '45000' set message_text = 'currency ShowAPI sync config completed state missing expected provider row';
  end if;

  if exists (select 1 from finance_currency where base_currency_code <> 'CNY') then
    signal sqlstate '45000' set message_text = 'currency ShowAPI migration completed with non-CNY base currency rows';
  end if;

  if exists (
      select 1
      from finance_currency
      where currency_code = 'CNY'
        and (official_rate is null or effective_rate is null or official_rate <> 1.0000000000 or effective_rate <> 1.0000000000
             or coalesce(is_default, '') <> 'Y')
  ) then
    signal sqlstate '45000' set message_text = 'currency ShowAPI migration completed with invalid CNY currency row';
  end if;

  if exists (
      select 1
      from finance_currency
      where currency_code = 'USD'
        and coalesce(is_default, '') <> 'N'
  ) then
    signal sqlstate '45000' set message_text = 'currency ShowAPI migration completed with invalid USD default currency row';
  end if;

  if exists (
      select 1
      from sys_dict_data
      where dict_type = 'currency_code'
        and dict_value in ('CNY', 'USD')
        and coalesce(is_default, '') <> case when dict_value = 'CNY' then 'Y' else 'N' end
  ) then
    signal sqlstate '45000' set message_text = 'currency ShowAPI migration completed with invalid currency_code dictionary default';
  end if;
end//

delimiter ;

call assert_currency_showapi_sync_migration_confirmed();
drop procedure if exists assert_currency_showapi_sync_migration_confirmed;

call add_column_if_missing('finance_currency_sync_config', 'rate_anchor_time',
  'time not null default ''09:30:00'' comment ''汇率基准时间'' after cron_expression');

call assert_showapi_provider_conflict_absent();
call assert_currency_showapi_sync_targets();

start transaction;

update finance_currency_sync_config
set provider_code = 'SHOWAPI_BANK_RATE',
    provider_name = 'ShowAPI银行汇率查询',
    base_currency_code = 'CNY',
    api_base_url = 'https://route.showapi.com/105-30',
    auth_type = 'APP_KEY',
    request_timeout_ms = 10000,
    retry_count = 0,
    schedule_type = 'DAILY',
    cron_expression = '',
    rate_anchor_time = '09:30:00',
    status = '0',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '固定接入 ShowAPI 银行汇率查询；appKey 通过后端加密保存'
where provider_code = 'GENERIC_RATES';

insert into finance_currency_sync_config(
    provider_code, provider_name, base_currency_code, api_base_url,
    auth_type, request_timeout_ms, retry_count, schedule_type,
    cron_expression, rate_anchor_time, sync_enabled, status,
    create_by, create_time, remark
)
select 'SHOWAPI_BANK_RATE', 'ShowAPI银行汇率查询', 'CNY', 'https://route.showapi.com/105-30',
       'APP_KEY', 10000, 0, 'DAILY',
       '', '09:30:00', '0', '0',
       'admin', sysdate(), '固定接入 ShowAPI 银行汇率查询；appKey 通过后端加密保存'
where not exists (
    select 1 from finance_currency_sync_config where provider_code = 'SHOWAPI_BANK_RATE'
);

update finance_currency
set base_currency_code = 'CNY',
    official_rate = case when currency_code = 'CNY' then 1.0000000000 else null end,
    effective_rate = case when currency_code = 'CNY' then 1.0000000000 else null end,
    official_rate_time = case when currency_code = 'CNY' then sysdate() else null end,
    effective_rate_time = case when currency_code = 'CNY' then sysdate() else null end,
    update_by = 'admin',
    update_time = sysdate()
where base_currency_code <> 'CNY'
   or (currency_code = 'USD' and official_rate = 1.0000000000)
   or (currency_code = 'CNY' and (official_rate is null or effective_rate is null));

update finance_currency
set is_default = case when currency_code = 'CNY' then 'Y' else 'N' end,
    update_by = 'admin',
    update_time = sysdate()
where currency_code in ('CNY', 'USD')
  and coalesce(is_default, '') <> case when currency_code = 'CNY' then 'Y' else 'N' end;

update sys_dict_data
set is_default = case when dict_value = 'CNY' then 'Y' else 'N' end,
    update_by = 'admin',
    update_time = sysdate()
where dict_type = 'currency_code'
  and dict_value in ('CNY', 'USD')
  and coalesce(is_default, '') <> case when dict_value = 'CNY' then 'Y' else 'N' end;

call assert_currency_showapi_sync_completed();

commit;

drop procedure if exists assert_currency_showapi_sync_completed;
drop procedure if exists assert_currency_showapi_sync_targets;
drop procedure if exists assert_showapi_provider_conflict_absent;
drop procedure if exists add_column_if_missing;
