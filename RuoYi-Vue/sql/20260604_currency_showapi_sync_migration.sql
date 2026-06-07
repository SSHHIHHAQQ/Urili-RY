-- Currency ShowAPI sync simplification migration.
-- Scope: add rate anchor time and switch existing sync config metadata to fixed ShowAPI bank rate provider.
-- Sensitive appKey must not be stored in this SQL file; save it through the backend encrypted config API.

set names utf8mb4;

set @confirm_currency_showapi_sync_migration := coalesce(@confirm_currency_showapi_sync_migration, '');

delimiter //

drop procedure if exists assert_currency_showapi_sync_migration_confirmed//
create procedure assert_currency_showapi_sync_migration_confirmed()
begin
  if coalesce(@confirm_currency_showapi_sync_migration, '')
      <> 'APPLY_CURRENCY_SHOWAPI_SYNC_MIGRATION' then
    signal sqlstate '45000' set message_text = 'set @confirm_currency_showapi_sync_migration = APPLY_CURRENCY_SHOWAPI_SYNC_MIGRATION before running this migration';
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

delimiter ;

call assert_currency_showapi_sync_migration_confirmed();
drop procedure if exists assert_currency_showapi_sync_migration_confirmed;

call add_column_if_missing('finance_currency_sync_config', 'rate_anchor_time',
  'time not null default ''09:30:00'' comment ''汇率基准时间'' after cron_expression');

call assert_showapi_provider_conflict_absent();

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
  and is_default <> case when currency_code = 'CNY' then 'Y' else 'N' end;

update sys_dict_data
set is_default = case when dict_value = 'CNY' then 'Y' else 'N' end,
    update_by = 'admin',
    update_time = sysdate()
where dict_type = 'currency_code'
  and dict_value in ('CNY', 'USD')
  and is_default <> case when dict_value = 'CNY' then 'Y' else 'N' end;

drop procedure if exists assert_showapi_provider_conflict_absent;
drop procedure if exists add_column_if_missing;
