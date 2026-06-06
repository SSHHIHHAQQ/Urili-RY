-- Portal direct-login audit ticket table.
-- Confirmed scope: remote DDL is allowed for this task.
-- Plaintext one-time tokens are returned only to the caller/Redis payload;
-- this table stores SHA-256 token_hash only.
-- Existing partial tables are self-healed by the idempotent helpers below.

set names utf8mb4;

set @confirm_portal_direct_login_ticket_migration := coalesce(@confirm_portal_direct_login_ticket_migration, '');

delimiter //

drop procedure if exists assert_portal_direct_login_ticket_migration_confirmed//
create procedure assert_portal_direct_login_ticket_migration_confirmed()
begin
  if coalesce(@confirm_portal_direct_login_ticket_migration, '')
      <> 'APPLY_PORTAL_DIRECT_LOGIN_TICKET_MIGRATION' then
    signal sqlstate '45000' set message_text = 'set @confirm_portal_direct_login_ticket_migration = APPLY_PORTAL_DIRECT_LOGIN_TICKET_MIGRATION before running this migration';
  end if;
end//

delimiter ;

call assert_portal_direct_login_ticket_migration_confirmed();
drop procedure if exists assert_portal_direct_login_ticket_migration_confirmed;

create table if not exists portal_direct_login_ticket (
  ticket_id             bigint(20)      not null auto_increment,
  terminal              varchar(20)     not null,
  target_subject_id     bigint(20)      not null,
  target_subject_no     varchar(64)     default '',
  target_account_id     bigint(20)      not null,
  target_user_name      varchar(64)     not null,
  acting_admin_id       bigint(20)      not null,
  acting_admin_name     varchar(64)     not null,
  reason                varchar(255)    default '',
  token_hash            varchar(64)     not null,
  expire_time           datetime        not null,
  used_time             datetime        default null,
  used_ip               varchar(128)    default '',
  status                varchar(20)     not null default 'ISSUED',
  create_by             varchar(64)     default '',
  create_time           datetime,
  update_by             varchar(64)     default '',
  update_time           datetime,
  remark                varchar(500)    default '',
  primary key (ticket_id),
  unique key uk_portal_direct_login_ticket_hash (token_hash),
  key idx_portal_direct_login_ticket_target (terminal, target_subject_id, target_account_id),
  key idx_portal_direct_login_ticket_admin_time (acting_admin_id, create_time),
  key idx_portal_direct_login_ticket_status_expire (status, expire_time)
) engine=innodb auto_increment=1 comment = 'Portal direct-login audit ticket';

delimiter //

drop procedure if exists add_column_if_missing//
create procedure add_column_if_missing(in p_table varchar(64), in p_column varchar(64), in p_definition text)
begin
  if not exists (
    select 1 from information_schema.columns
    where table_schema = database() and table_name = p_table and column_name = p_column
  ) then
    set @ddl = concat('alter table ', p_table, ' add column ', p_column, ' ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists recreate_index_if_mismatch//
create procedure recreate_index_if_mismatch(
  in p_table varchar(64),
  in p_index varchar(64),
  in p_expected_columns varchar(512),
  in p_expected_non_unique int,
  in p_definition text
)
begin
  declare v_index_count int default 0;
  declare v_actual_columns text default '';
  declare v_actual_non_unique int default null;

  select count(distinct index_name),
         coalesce(group_concat(column_name order by seq_in_index separator ','), ''),
         max(non_unique)
    into v_index_count, v_actual_columns, v_actual_non_unique
  from information_schema.statistics
  where table_schema = database() and table_name = p_table and index_name = p_index;

  if v_index_count > 0
      and (v_actual_columns <> p_expected_columns or v_actual_non_unique <> p_expected_non_unique) then
    set @ddl = concat('alter table `', p_table, '` drop index `', p_index, '`');
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
    set v_index_count = 0;
  end if;

  if v_index_count = 0 then
    set @ddl = concat('alter table `', p_table, '` add ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_index_definition//
create procedure assert_index_definition(
  in p_table varchar(64),
  in p_index varchar(64),
  in p_expected_columns varchar(512),
  in p_expected_non_unique int,
  in p_message varchar(128)
)
begin
  declare v_index_count int default 0;
  declare v_actual_columns text default '';
  declare v_actual_non_unique int default null;

  select count(distinct index_name),
         coalesce(group_concat(column_name order by seq_in_index separator ','), ''),
         max(non_unique)
    into v_index_count, v_actual_columns, v_actual_non_unique
  from information_schema.statistics
  where table_schema = database() and table_name = p_table and index_name = p_index;

  if v_index_count <> 1
      or v_actual_columns <> p_expected_columns
      or v_actual_non_unique <> p_expected_non_unique then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_no_invalid_direct_login_ticket_rows//
create procedure assert_no_invalid_direct_login_ticket_rows()
begin
  if exists (
    select 1 from portal_direct_login_ticket
    where terminal not in ('seller', 'buyer')
       or target_subject_id <= 0
       or target_account_id <= 0
       or acting_admin_id <= 0
       or token_hash is null
       or token_hash = ''
       or token_hash like 'legacy-%'
       or expire_time is null
  ) then
    signal sqlstate '45000' set message_text = 'portal_direct_login_ticket has invalid legacy rows';
  end if;
end//

drop procedure if exists assert_column_exists//
create procedure assert_column_exists(in p_table varchar(64), in p_column varchar(64), in p_message varchar(128))
begin
  if not exists (
    select 1 from information_schema.columns
    where table_schema = database() and table_name = p_table and column_name = p_column
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

delimiter ;

call assert_column_exists('portal_direct_login_ticket', 'ticket_id', 'portal_direct_login_ticket.ticket_id is required');

call add_column_if_missing('portal_direct_login_ticket', 'terminal', 'varchar(20) default null');
call add_column_if_missing('portal_direct_login_ticket', 'target_subject_id', 'bigint(20) default null');
call add_column_if_missing('portal_direct_login_ticket', 'target_subject_no', 'varchar(64) default ''''');
call add_column_if_missing('portal_direct_login_ticket', 'target_account_id', 'bigint(20) default null');
call add_column_if_missing('portal_direct_login_ticket', 'target_user_name', 'varchar(64) default null');
call add_column_if_missing('portal_direct_login_ticket', 'acting_admin_id', 'bigint(20) default null');
call add_column_if_missing('portal_direct_login_ticket', 'acting_admin_name', 'varchar(64) default null');
call add_column_if_missing('portal_direct_login_ticket', 'reason', 'varchar(255) default ''''');
call add_column_if_missing('portal_direct_login_ticket', 'token_hash', 'varchar(64) default null');
call add_column_if_missing('portal_direct_login_ticket', 'expire_time', 'datetime default null');
call add_column_if_missing('portal_direct_login_ticket', 'used_time', 'datetime default null');
call add_column_if_missing('portal_direct_login_ticket', 'used_ip', 'varchar(128) default ''''');
call add_column_if_missing('portal_direct_login_ticket', 'status', 'varchar(20) not null default ''ISSUED''');
call add_column_if_missing('portal_direct_login_ticket', 'create_by', 'varchar(64) default ''''');
call add_column_if_missing('portal_direct_login_ticket', 'create_time', 'datetime');
call add_column_if_missing('portal_direct_login_ticket', 'update_by', 'varchar(64) default ''''');
call add_column_if_missing('portal_direct_login_ticket', 'update_time', 'datetime');
call add_column_if_missing('portal_direct_login_ticket', 'remark', 'varchar(500) default ''''');

update portal_direct_login_ticket set target_subject_no = '' where target_subject_no is null;
update portal_direct_login_ticket set reason = '' where reason is null;
update portal_direct_login_ticket set used_ip = '' where used_ip is null;
update portal_direct_login_ticket set status = 'ISSUED' where status is null or status = '';
update portal_direct_login_ticket set create_by = '' where create_by is null;
update portal_direct_login_ticket set update_by = '' where update_by is null;
update portal_direct_login_ticket set remark = '' where remark is null;

call assert_no_invalid_direct_login_ticket_rows();

alter table portal_direct_login_ticket
  modify terminal varchar(20) not null,
  modify target_subject_id bigint(20) not null,
  modify target_subject_no varchar(64) default '',
  modify target_account_id bigint(20) not null,
  modify target_user_name varchar(64) not null,
  modify acting_admin_id bigint(20) not null,
  modify acting_admin_name varchar(64) not null,
  modify reason varchar(255) default '',
  modify token_hash varchar(64) not null,
  modify expire_time datetime not null,
  modify used_time datetime default null,
  modify used_ip varchar(128) default '',
  modify status varchar(20) not null default 'ISSUED',
  modify create_by varchar(64) default '',
  modify create_time datetime,
  modify update_by varchar(64) default '',
  modify update_time datetime,
  modify remark varchar(500) default '';

call recreate_index_if_mismatch('portal_direct_login_ticket', 'uk_portal_direct_login_ticket_hash',
  'token_hash', 0, 'unique key uk_portal_direct_login_ticket_hash (token_hash)');
call recreate_index_if_mismatch('portal_direct_login_ticket', 'idx_portal_direct_login_ticket_target',
  'terminal,target_subject_id,target_account_id', 1,
  'key idx_portal_direct_login_ticket_target (terminal, target_subject_id, target_account_id)');
call recreate_index_if_mismatch('portal_direct_login_ticket', 'idx_portal_direct_login_ticket_admin_time',
  'acting_admin_id,create_time', 1,
  'key idx_portal_direct_login_ticket_admin_time (acting_admin_id, create_time)');
call recreate_index_if_mismatch('portal_direct_login_ticket', 'idx_portal_direct_login_ticket_status_expire',
  'status,expire_time', 1,
  'key idx_portal_direct_login_ticket_status_expire (status, expire_time)');

call assert_index_definition('portal_direct_login_ticket', 'uk_portal_direct_login_ticket_hash',
  'token_hash', 0, 'portal_direct_login_ticket token_hash unique index is invalid');
call assert_index_definition('portal_direct_login_ticket', 'idx_portal_direct_login_ticket_target',
  'terminal,target_subject_id,target_account_id', 1, 'portal_direct_login_ticket target index is invalid');
call assert_index_definition('portal_direct_login_ticket', 'idx_portal_direct_login_ticket_admin_time',
  'acting_admin_id,create_time', 1, 'portal_direct_login_ticket admin/time index is invalid');
call assert_index_definition('portal_direct_login_ticket', 'idx_portal_direct_login_ticket_status_expire',
  'status,expire_time', 1, 'portal_direct_login_ticket status/expire index is invalid');

drop procedure if exists add_column_if_missing;
drop procedure if exists recreate_index_if_mismatch;
drop procedure if exists assert_index_definition;
drop procedure if exists assert_no_invalid_direct_login_ticket_rows;
drop procedure if exists assert_column_exists;
