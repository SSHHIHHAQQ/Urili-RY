-- Portal direct-login audit ticket table.
-- Confirmed scope: remote DDL is allowed for this task.
-- Plaintext one-time tokens are returned only to the caller/Redis payload;
-- this table stores SHA-256 token_hash only.
-- Existing partial tables are self-healed by the idempotent helpers below.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_portal_direct_login_ticket_migration := coalesce(@confirm_portal_direct_login_ticket_migration, '');
set @portal_direct_login_ticket_normalize_expected_count :=
    coalesce(@portal_direct_login_ticket_normalize_expected_count, '');
set @portal_direct_login_ticket_normalize_expected_signature :=
    coalesce(@portal_direct_login_ticket_normalize_expected_signature, '');

delimiter //

drop procedure if exists assert_portal_direct_login_ticket_migration_confirmed//
create procedure assert_portal_direct_login_ticket_migration_confirmed()
begin
  if coalesce(@confirm_portal_direct_login_ticket_migration, '')
      <> 'APPLY_PORTAL_DIRECT_LOGIN_TICKET_MIGRATION' then
    signal sqlstate '45000' set message_text = 'set @confirm_portal_direct_login_ticket_migration = APPLY_PORTAL_DIRECT_LOGIN_TICKET_MIGRATION before running this migration';
  end if;

  if coalesce(@portal_direct_login_ticket_normalize_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @portal_direct_login_ticket_normalize_expected_count after previewing exact portal_direct_login_ticket normalize rows';
  end if;

  if coalesce(@portal_direct_login_ticket_normalize_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @portal_direct_login_ticket_normalize_expected_signature after previewing exact portal_direct_login_ticket normalize rows';
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
    where terminal is null
       or terminal not in ('seller', 'buyer')
       or target_subject_id is null
       or target_subject_id <= 0
       or target_account_id is null
       or target_account_id <= 0
       or target_user_name is null
       or trim(target_user_name) = ''
       or acting_admin_id is null
       or acting_admin_id <= 0
       or acting_admin_name is null
       or trim(acting_admin_name) = ''
       or token_hash is null
       or trim(token_hash) = ''
       or token_hash like 'legacy-%'
       or expire_time is null
       or status is null
       or status not in ('ISSUED', 'USED', 'EXPIRED')
       or (status = 'USED' and used_time is null)
       or (status = 'ISSUED' and used_time is not null)
  ) then
    signal sqlstate '45000' set message_text = 'portal_direct_login_ticket has invalid legacy rows';
  end if;
end//

drop procedure if exists assert_portal_direct_login_ticket_normalize_targets//
create procedure assert_portal_direct_login_ticket_normalize_targets()
begin
  declare v_count bigint default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             ticket_id,
             coalesce(target_subject_no, '<NULL>'),
             coalesce(reason, '<NULL>'),
             coalesce(used_ip, '<NULL>'),
             coalesce(status, '<NULL>'),
             coalesce(create_by, '<NULL>'),
             coalesce(update_by, '<NULL>'),
             coalesce(remark, '<NULL>')
           )
           order by ticket_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from portal_direct_login_ticket
  where target_subject_no is null
     or reason is null
     or used_ip is null
     or status is null
     or status = ''
     or create_by is null
     or update_by is null
     or remark is null;

  if v_count <> cast(@portal_direct_login_ticket_normalize_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'portal_direct_login_ticket normalize exact target count mismatch';
  end if;

  if lower(v_signature) <> lower(@portal_direct_login_ticket_normalize_expected_signature) then
    signal sqlstate '45000' set message_text = 'portal_direct_login_ticket normalize exact target signature mismatch';
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

drop procedure if exists assert_portal_direct_login_ticket_column_contract//
create procedure assert_portal_direct_login_ticket_column_contract()
begin
  declare v_mismatch_count int default 0;

  select count(1)
    into v_mismatch_count
  from (
    select 'terminal' as expected_column, 'varchar' as expected_type, 20 as expected_length, 'NO' as expected_nullable, cast(null as char) as expected_default
    union all select 'target_subject_id', 'bigint', null, 'NO', null
    union all select 'target_subject_no', 'varchar', 64, 'YES', ''
    union all select 'target_account_id', 'bigint', null, 'NO', null
    union all select 'target_user_name', 'varchar', 64, 'NO', null
    union all select 'acting_admin_id', 'bigint', null, 'NO', null
    union all select 'acting_admin_name', 'varchar', 64, 'NO', null
    union all select 'reason', 'varchar', 255, 'YES', ''
    union all select 'token_hash', 'varchar', 64, 'NO', null
    union all select 'expire_time', 'datetime', null, 'NO', null
    union all select 'used_time', 'datetime', null, 'YES', null
    union all select 'used_ip', 'varchar', 128, 'YES', ''
    union all select 'status', 'varchar', 20, 'NO', 'ISSUED'
    union all select 'create_by', 'varchar', 64, 'YES', ''
    union all select 'create_time', 'datetime', null, 'YES', null
    union all select 'update_by', 'varchar', 64, 'YES', ''
    union all select 'update_time', 'datetime', null, 'YES', null
    union all select 'remark', 'varchar', 500, 'YES', ''
  ) expected
  left join information_schema.columns c
    on c.table_schema = database()
   and c.table_name = 'portal_direct_login_ticket'
   and c.column_name = expected.expected_column
  where c.column_name is null
     or lower(c.data_type) <> expected.expected_type
     or (expected.expected_length is not null and coalesce(c.character_maximum_length, -1) <> expected.expected_length)
     or c.is_nullable <> expected.expected_nullable
     or coalesce(c.column_default, '<NULL>') <> coalesce(expected.expected_default, '<NULL>');

  if v_mismatch_count > 0 then
    signal sqlstate '45000' set message_text = 'portal_direct_login_ticket final column contract mismatch';
  end if;
end//

drop procedure if exists assert_portal_direct_login_ticket_identity_contract//
create procedure assert_portal_direct_login_ticket_identity_contract()
begin
  declare v_column_count int default 0;
  declare v_index_count int default 0;
  declare v_actual_columns text default '';
  declare v_actual_non_unique int default null;

  select count(1)
    into v_column_count
  from information_schema.columns
  where table_schema = database()
    and table_name = 'portal_direct_login_ticket'
    and column_name = 'ticket_id'
    and lower(data_type) = 'bigint'
    and is_nullable = 'NO'
    and lower(extra) like '%auto_increment%';

  if v_column_count <> 1 then
    signal sqlstate '45000' set message_text = 'portal_direct_login_ticket.ticket_id must be bigint not null auto_increment';
  end if;

  select count(distinct index_name),
         coalesce(group_concat(column_name order by seq_in_index separator ','), ''),
         max(non_unique)
    into v_index_count, v_actual_columns, v_actual_non_unique
  from information_schema.statistics
  where table_schema = database()
    and table_name = 'portal_direct_login_ticket'
    and index_name = 'PRIMARY';

  if v_index_count <> 1
      or v_actual_columns <> 'ticket_id'
      or v_actual_non_unique <> 0 then
    signal sqlstate '45000' set message_text = 'portal_direct_login_ticket primary key must be ticket_id';
  end if;
end//

drop procedure if exists modify_portal_direct_login_ticket_columns_if_needed//
create procedure modify_portal_direct_login_ticket_columns_if_needed()
begin
  declare v_missing_count int default 0;
  declare v_mismatch_count int default 0;

  select count(1)
    into v_missing_count
  from (
    select 'terminal' as expected_column
    union all select 'target_subject_id'
    union all select 'target_subject_no'
    union all select 'target_account_id'
    union all select 'target_user_name'
    union all select 'acting_admin_id'
    union all select 'acting_admin_name'
    union all select 'reason'
    union all select 'token_hash'
    union all select 'expire_time'
    union all select 'used_time'
    union all select 'used_ip'
    union all select 'status'
    union all select 'create_by'
    union all select 'create_time'
    union all select 'update_by'
    union all select 'update_time'
    union all select 'remark'
  ) expected
  left join information_schema.columns c
    on c.table_schema = database()
   and c.table_name = 'portal_direct_login_ticket'
   and c.column_name = expected.expected_column
  where c.column_name is null;

  if v_missing_count > 0 then
    signal sqlstate '45000' set message_text = 'portal_direct_login_ticket expected columns are required before ticket column modify';
  end if;

  select count(1)
    into v_mismatch_count
  from (
    select 'terminal' as expected_column, 'varchar' as expected_type, 20 as expected_length, 'NO' as expected_nullable, cast(null as char) as expected_default
    union all select 'target_subject_id', 'bigint', null, 'NO', null
    union all select 'target_subject_no', 'varchar', 64, 'YES', ''
    union all select 'target_account_id', 'bigint', null, 'NO', null
    union all select 'target_user_name', 'varchar', 64, 'NO', null
    union all select 'acting_admin_id', 'bigint', null, 'NO', null
    union all select 'acting_admin_name', 'varchar', 64, 'NO', null
    union all select 'reason', 'varchar', 255, 'YES', ''
    union all select 'token_hash', 'varchar', 64, 'NO', null
    union all select 'expire_time', 'datetime', null, 'NO', null
    union all select 'used_time', 'datetime', null, 'YES', null
    union all select 'used_ip', 'varchar', 128, 'YES', ''
    union all select 'status', 'varchar', 20, 'NO', 'ISSUED'
    union all select 'create_by', 'varchar', 64, 'YES', ''
    union all select 'create_time', 'datetime', null, 'YES', null
    union all select 'update_by', 'varchar', 64, 'YES', ''
    union all select 'update_time', 'datetime', null, 'YES', null
    union all select 'remark', 'varchar', 500, 'YES', ''
  ) expected
  join information_schema.columns c
    on c.table_schema = database()
   and c.table_name = 'portal_direct_login_ticket'
   and c.column_name = expected.expected_column
  where lower(c.data_type) <> expected.expected_type
     or (expected.expected_length is not null and coalesce(c.character_maximum_length, -1) <> expected.expected_length)
     or c.is_nullable <> expected.expected_nullable
     or coalesce(c.column_default, '<NULL>') <> coalesce(expected.expected_default, '<NULL>');

  if v_mismatch_count > 0 then
    set @ddl = 'alter table portal_direct_login_ticket modify terminal varchar(20) not null, modify target_subject_id bigint(20) not null, modify target_subject_no varchar(64) default '''', modify target_account_id bigint(20) not null, modify target_user_name varchar(64) not null, modify acting_admin_id bigint(20) not null, modify acting_admin_name varchar(64) not null, modify reason varchar(255) default '''', modify token_hash varchar(64) not null, modify expire_time datetime not null, modify used_time datetime default null, modify used_ip varchar(128) default '''', modify status varchar(20) not null default ''ISSUED'', modify create_by varchar(64) default '''', modify create_time datetime, modify update_by varchar(64) default '''', modify update_time datetime, modify remark varchar(500) default ''''';
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

delimiter ;

call assert_column_exists('portal_direct_login_ticket', 'ticket_id', 'portal_direct_login_ticket.ticket_id is required');
call assert_portal_direct_login_ticket_identity_contract();

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

call assert_portal_direct_login_ticket_normalize_targets();

update portal_direct_login_ticket set target_subject_no = '' where target_subject_no is null;
update portal_direct_login_ticket set reason = '' where reason is null;
update portal_direct_login_ticket set used_ip = '' where used_ip is null;
update portal_direct_login_ticket set status = 'ISSUED' where status is null or status = '';
update portal_direct_login_ticket set create_by = '' where create_by is null;
update portal_direct_login_ticket set update_by = '' where update_by is null;
update portal_direct_login_ticket set remark = '' where remark is null;

call assert_no_invalid_direct_login_ticket_rows();

call modify_portal_direct_login_ticket_columns_if_needed();
call assert_portal_direct_login_ticket_column_contract();
call assert_portal_direct_login_ticket_identity_contract();

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
drop procedure if exists assert_portal_direct_login_ticket_normalize_targets;
drop procedure if exists assert_column_exists;
drop procedure if exists assert_portal_direct_login_ticket_column_contract;
drop procedure if exists assert_portal_direct_login_ticket_identity_contract;
drop procedure if exists modify_portal_direct_login_ticket_columns_if_needed;
