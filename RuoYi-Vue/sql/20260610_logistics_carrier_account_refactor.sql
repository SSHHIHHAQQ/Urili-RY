-- Logistics carrier account refactor.
-- Scope: add internal carrier_account_id and move runtime lookup from connection_code to account id.
-- Sensitive credentials are not stored in this script.

set names utf8mb4;

set @confirm_logistics_carrier_account_refactor := coalesce(@confirm_logistics_carrier_account_refactor, '');

delimiter //

drop procedure if exists assert_logistics_carrier_account_refactor_confirmed//
create procedure assert_logistics_carrier_account_refactor_confirmed()
begin
  if coalesce(@confirm_logistics_carrier_account_refactor, '')
      <> 'APPLY_LOGISTICS_CARRIER_ACCOUNT_REFACTOR' then
    signal sqlstate '45000' set message_text = 'set @confirm_logistics_carrier_account_refactor = APPLY_LOGISTICS_CARRIER_ACCOUNT_REFACTOR before running this migration';
  end if;
end//

drop procedure if exists add_logistics_column_if_missing//
create procedure add_logistics_column_if_missing(
  in p_table_name varchar(128),
  in p_column_name varchar(128),
  in p_alter_sql text
)
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table_name
      and column_name = p_column_name
  ) then
    set @ddl = p_alter_sql;
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists add_logistics_index_if_missing//
create procedure add_logistics_index_if_missing(
  in p_table_name varchar(128),
  in p_index_name varchar(128),
  in p_alter_sql text
)
begin
  if not exists (
    select 1
    from information_schema.statistics
    where table_schema = database()
      and table_name = p_table_name
      and index_name = p_index_name
  ) then
    set @ddl = p_alter_sql;
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_logistics_account_id_backfilled//
create procedure assert_logistics_account_id_backfilled()
begin
  if exists (select 1 from logistics_carrier_connection where carrier_account_id is null) then
    signal sqlstate '45000' set message_text = 'logistics_carrier_connection carrier_account_id backfill failed';
  end if;

  if exists (select 1 from logistics_agg56_connection where carrier_account_id is null) then
    signal sqlstate '45000' set message_text = 'logistics_agg56_connection carrier_account_id backfill failed';
  end if;

  if exists (select 1 from logistics_carrier_channel_candidate where carrier_account_id is null) then
    signal sqlstate '45000' set message_text = 'logistics_carrier_channel_candidate carrier_account_id backfill failed';
  end if;

  if exists (select 1 from logistics_carrier_channel_mapping where carrier_account_id is null) then
    signal sqlstate '45000' set message_text = 'logistics_carrier_channel_mapping carrier_account_id backfill failed';
  end if;

  if exists (select 1 from logistics_label_order where carrier_account_id is null) then
    signal sqlstate '45000' set message_text = 'logistics_label_order carrier_account_id backfill failed';
  end if;

  if exists (select 1 from logistics_carrier_request_log where carrier_account_id is null) then
    signal sqlstate '45000' set message_text = 'logistics_carrier_request_log carrier_account_id backfill failed';
  end if;
end//

delimiter ;

call assert_logistics_carrier_account_refactor_confirmed();
drop procedure if exists assert_logistics_carrier_account_refactor_confirmed;

call add_logistics_column_if_missing(
  'logistics_carrier_connection',
  'carrier_account_id',
  'alter table logistics_carrier_connection add column carrier_account_id bigint(20) not null auto_increment comment ''物流商账号ID'' first, add unique key uk_logistics_connection_account_id (carrier_account_id)'
);

call add_logistics_column_if_missing(
  'logistics_agg56_connection',
  'carrier_account_id',
  'alter table logistics_agg56_connection add column carrier_account_id bigint(20) null comment ''物流商账号ID'' first'
);

call add_logistics_column_if_missing(
  'logistics_carrier_channel_candidate',
  'carrier_account_id',
  'alter table logistics_carrier_channel_candidate add column carrier_account_id bigint(20) null comment ''物流商账号ID'' first'
);

call add_logistics_column_if_missing(
  'logistics_carrier_channel_mapping',
  'carrier_account_id',
  'alter table logistics_carrier_channel_mapping add column carrier_account_id bigint(20) null comment ''物流商账号ID'' after mapping_id'
);

call add_logistics_column_if_missing(
  'logistics_label_order',
  'carrier_account_id',
  'alter table logistics_label_order add column carrier_account_id bigint(20) null comment ''物流商账号ID'' after business_order_no'
);

call add_logistics_column_if_missing(
  'logistics_carrier_request_log',
  'carrier_account_id',
  'alter table logistics_carrier_request_log add column carrier_account_id bigint(20) null comment ''物流商账号ID'' after request_log_id'
);

update logistics_agg56_connection a
join logistics_carrier_connection c on c.connection_code = a.connection_code
set a.carrier_account_id = c.carrier_account_id
where a.carrier_account_id is null;

update logistics_carrier_channel_candidate ch
join logistics_carrier_connection c on c.connection_code = ch.connection_code
set ch.carrier_account_id = c.carrier_account_id
where ch.carrier_account_id is null;

update logistics_carrier_channel_mapping m
join logistics_carrier_connection c on c.connection_code = m.connection_code
set m.carrier_account_id = c.carrier_account_id
where m.carrier_account_id is null;

update logistics_label_order o
join logistics_carrier_connection c on c.connection_code = o.connection_code
set o.carrier_account_id = c.carrier_account_id
where o.carrier_account_id is null;

update logistics_carrier_request_log l
join logistics_carrier_connection c on c.connection_code = l.connection_code
set l.carrier_account_id = c.carrier_account_id
where l.carrier_account_id is null;

call assert_logistics_account_id_backfilled();
drop procedure if exists assert_logistics_account_id_backfilled;

alter table logistics_agg56_connection modify carrier_account_id bigint(20) not null comment '物流商账号ID';
alter table logistics_carrier_channel_candidate modify carrier_account_id bigint(20) not null comment '物流商账号ID';
alter table logistics_carrier_channel_mapping modify carrier_account_id bigint(20) not null comment '物流商账号ID';
alter table logistics_label_order modify carrier_account_id bigint(20) not null comment '物流商账号ID';
alter table logistics_carrier_request_log modify carrier_account_id bigint(20) not null comment '物流商账号ID';

call add_logistics_index_if_missing(
  'logistics_agg56_connection',
  'uk_logistics_agg56_account',
  'alter table logistics_agg56_connection add unique key uk_logistics_agg56_account (carrier_account_id)'
);

call add_logistics_index_if_missing(
  'logistics_carrier_channel_candidate',
  'idx_logistics_candidate_account_status',
  'alter table logistics_carrier_channel_candidate add key idx_logistics_candidate_account_status (carrier_account_id, status)'
);

call add_logistics_index_if_missing(
  'logistics_carrier_channel_mapping',
  'idx_logistics_mapping_account_system',
  'alter table logistics_carrier_channel_mapping add key idx_logistics_mapping_account_system (carrier_account_id, system_channel_code, status)'
);

call add_logistics_index_if_missing(
  'logistics_label_order',
  'idx_logistics_label_account_provider_order',
  'alter table logistics_label_order add key idx_logistics_label_account_provider_order (carrier_account_id, provider_order_no)'
);

call add_logistics_index_if_missing(
  'logistics_carrier_request_log',
  'idx_logistics_request_log_account_time',
  'alter table logistics_carrier_request_log add key idx_logistics_request_log_account_time (carrier_account_id, request_time)'
);

drop procedure if exists add_logistics_column_if_missing;
drop procedure if exists add_logistics_index_if_missing;
