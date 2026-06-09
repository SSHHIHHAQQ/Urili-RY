-- Three-terminal isolation migration for seller/buyer portal accounts.
-- Confirmed scope: remote DDL/DML is allowed for this task.
-- Admin remains on RuoYi sys_*; seller/buyer portal accounts use independent terminal tables.
-- Non-transactional migration: this script contains MySQL DDL and DML.
-- DDL causes implicit commits; if a failure occurs, changes may be partially applied.
-- Complete legacy preflight before execution reaches the first DDL/DML; preflight
-- only checks blockers and does not repair data. If execution stops after
-- preflight, fix the failure cause and rerun the same script from the beginning.

set names utf8mb4;

set @confirm_three_terminal_isolation_migration := coalesce(@confirm_three_terminal_isolation_migration, '');
set @three_terminal_seller_account_normalize_expected_count :=
    coalesce(@three_terminal_seller_account_normalize_expected_count, null);
set @three_terminal_seller_account_normalize_expected_signature :=
    coalesce(@three_terminal_seller_account_normalize_expected_signature, '');
set @three_terminal_buyer_account_normalize_expected_count :=
    coalesce(@three_terminal_buyer_account_normalize_expected_count, null);
set @three_terminal_buyer_account_normalize_expected_signature :=
    coalesce(@three_terminal_buyer_account_normalize_expected_signature, '');
set @three_terminal_seller_user_id_drop_expected_count :=
    coalesce(@three_terminal_seller_user_id_drop_expected_count, null);
set @three_terminal_seller_user_id_drop_expected_signature :=
    coalesce(@three_terminal_seller_user_id_drop_expected_signature, '');
set @three_terminal_buyer_user_id_drop_expected_count :=
    coalesce(@three_terminal_buyer_user_id_drop_expected_count, null);
set @three_terminal_buyer_user_id_drop_expected_signature :=
    coalesce(@three_terminal_buyer_user_id_drop_expected_signature, '');
set session group_concat_max_len = 1048576;

delimiter //

drop procedure if exists assert_three_terminal_isolation_migration_confirmed//
create procedure assert_three_terminal_isolation_migration_confirmed()
begin
  if coalesce(@confirm_three_terminal_isolation_migration, '')
      <> 'APPLY_THREE_TERMINAL_ISOLATION_MIGRATION' then
    signal sqlstate '45000' set message_text = 'set @confirm_three_terminal_isolation_migration = APPLY_THREE_TERMINAL_ISOLATION_MIGRATION before running this migration';
  end if;
  if @three_terminal_seller_account_normalize_expected_count is null then
    signal sqlstate '45000' set message_text = 'set @three_terminal_seller_account_normalize_expected_count after previewing exact seller_account normalize rows';
  end if;
  if coalesce(@three_terminal_seller_account_normalize_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @three_terminal_seller_account_normalize_expected_signature after previewing exact seller_account normalize rows';
  end if;
  if @three_terminal_buyer_account_normalize_expected_count is null then
    signal sqlstate '45000' set message_text = 'set @three_terminal_buyer_account_normalize_expected_count after previewing exact buyer_account normalize rows';
  end if;
  if coalesce(@three_terminal_buyer_account_normalize_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @three_terminal_buyer_account_normalize_expected_signature after previewing exact buyer_account normalize rows';
  end if;
  if @three_terminal_seller_user_id_drop_expected_count is null then
    signal sqlstate '45000' set message_text = 'set @three_terminal_seller_user_id_drop_expected_count after previewing seller_account user_id column';
  end if;
  if coalesce(@three_terminal_seller_user_id_drop_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @three_terminal_seller_user_id_drop_expected_signature after previewing seller_account user_id column';
  end if;
  if @three_terminal_buyer_user_id_drop_expected_count is null then
    signal sqlstate '45000' set message_text = 'set @three_terminal_buyer_user_id_drop_expected_count after previewing buyer_account user_id column';
  end if;
  if coalesce(@three_terminal_buyer_user_id_drop_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @three_terminal_buyer_user_id_drop_expected_signature after previewing buyer_account user_id column';
  end if;
end//

delimiter ;

call assert_three_terminal_isolation_migration_confirmed();
drop procedure if exists assert_three_terminal_isolation_migration_confirmed;

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

drop procedure if exists drop_column_if_exists//
create procedure drop_column_if_exists(in p_table varchar(64), in p_column varchar(64))
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = database() and table_name = p_table and column_name = p_column
  ) then
    set @ddl = concat('alter table ', p_table, ' drop column ', p_column);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_no_legacy_account_user_bindings//
create procedure assert_no_legacy_account_user_bindings(in p_table varchar(64), in p_message varchar(128))
begin
  set @legacy_account_user_count := 0;
  if exists (
    select 1 from information_schema.columns
    where table_schema = database() and table_name = p_table and column_name = 'user_id'
  ) then
    set @sql = concat('select count(1) into @legacy_account_user_count from ', p_table, ' where user_id is not null');
    prepare stmt from @sql;
    execute stmt;
    deallocate prepare stmt;
  end if;

  if @legacy_account_user_count > 0 then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_database_selected//
create procedure assert_database_selected()
begin
  if database() is null then
    signal sqlstate '45000' set message_text = 'select target database before running three-terminal isolation migration';
  end if;
end//

drop procedure if exists assert_existing_column_if_table_present//
create procedure assert_existing_column_if_table_present(in p_table varchar(64), in p_column varchar(64), in p_message varchar(128))
begin
  declare v_table_count int default 0;
  declare v_column_count int default 0;

  select count(1)
    into v_table_count
  from information_schema.tables
  where table_schema = database() and table_name = p_table;

  if v_table_count > 0 then
    select count(1)
      into v_column_count
    from information_schema.columns
    where table_schema = database() and table_name = p_table and column_name = p_column;

    if v_column_count <> 1 then
      signal sqlstate '45000' set message_text = p_message;
    end if;
  end if;
end//

drop procedure if exists assert_three_terminal_isolation_preflight//
create procedure assert_three_terminal_isolation_preflight()
begin
  call assert_database_selected();
  call assert_existing_column_if_table_present('seller_account', 'seller_account_id', 'seller_account.seller_account_id is required before three-terminal isolation migration');
  call assert_existing_column_if_table_present('seller_account', 'seller_id', 'seller_account.seller_id is required before three-terminal isolation migration');
  call assert_existing_column_if_table_present('seller_account', 'account_role', 'seller_account.account_role is required before three-terminal isolation migration');
  call assert_existing_column_if_table_present('buyer_account', 'buyer_account_id', 'buyer_account.buyer_account_id is required before three-terminal isolation migration');
  call assert_existing_column_if_table_present('buyer_account', 'buyer_id', 'buyer_account.buyer_id is required before three-terminal isolation migration');
  call assert_existing_column_if_table_present('buyer_account', 'account_role', 'buyer_account.account_role is required before three-terminal isolation migration');
  call assert_no_legacy_account_user_bindings('seller_account', 'seller_account.user_id still has bindings; run legacy sys_user backfill first');
  call assert_no_legacy_account_user_bindings('buyer_account', 'buyer_account.user_id still has bindings; run legacy sys_user backfill first');
end//

drop procedure if exists drop_index_if_exists//
create procedure drop_index_if_exists(in p_table varchar(64), in p_index varchar(64))
begin
  if exists (
    select 1 from information_schema.statistics
    where table_schema = database() and table_name = p_table and index_name = p_index
  ) then
    set @ddl = concat('alter table ', p_table, ' drop index ', p_index);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists add_index_if_missing//
create procedure add_index_if_missing(in p_table varchar(64), in p_index varchar(64), in p_definition text)
begin
  if not exists (
    select 1 from information_schema.statistics
    where table_schema = database() and table_name = p_table and index_name = p_index
  ) then
    set @ddl = concat('alter table ', p_table, ' add ', p_definition);
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

drop procedure if exists assert_no_duplicate_owner_account//
create procedure assert_no_duplicate_owner_account(in p_table varchar(64), in p_subject_column varchar(64), in p_message varchar(128))
begin
  set @duplicate_owner_count = 0;
  set @sql = concat('select count(*) into @duplicate_owner_count from (select ', p_subject_column,
      ' from ', p_table, ' where account_role = ''OWNER'' group by ', p_subject_column, ' having count(*) > 1) t');
  prepare stmt from @sql;
  execute stmt;
  deallocate prepare stmt;
  if @duplicate_owner_count > 0 then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_no_duplicate_terminal_user_name//
create procedure assert_no_duplicate_terminal_user_name(in p_table varchar(64), in p_message varchar(128))
begin
  set @duplicate_terminal_user_name_count := 0;
  set @sql = concat('select count(1) into @duplicate_terminal_user_name_count from (select user_name from ',
      p_table, ' group by user_name having count(1) > 1) t');
  prepare stmt from @sql;
  execute stmt;
  deallocate prepare stmt;
  if @duplicate_terminal_user_name_count > 0 then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists modify_terminal_account_identity_columns_if_needed//
create procedure modify_terminal_account_identity_columns_if_needed(in p_table varchar(64))
begin
  declare v_missing_count int default 0;
  declare v_mismatch_count int default 0;

  select count(1)
    into v_missing_count
  from (
    select 'user_name' as expected_column
    union all select 'nick_name'
    union all select 'password'
  ) expected
  left join information_schema.columns c
    on c.table_schema = database()
   and c.table_name = p_table
   and c.column_name = expected.expected_column
  where c.column_name is null;

  if v_missing_count > 0 then
    signal sqlstate '45000' set message_text = 'terminal account identity columns are required before account identity modify';
  end if;

  select count(1)
    into v_mismatch_count
  from (
    select 'user_name' as expected_column, 'varchar' as expected_type, 30 as expected_length, 'NO' as expected_nullable, cast(null as char) as expected_default
    union all select 'nick_name', 'varchar', 30, 'NO', ''
    union all select 'password', 'varchar', 100, 'NO', cast(null as char)
  ) expected
  left join information_schema.columns c
    on c.table_schema = database()
   and c.table_name = p_table
   and c.column_name = expected.expected_column
  where c.column_name is null
     or lower(c.data_type) <> expected.expected_type
     or coalesce(c.character_maximum_length, -1) <> expected.expected_length
     or c.is_nullable <> expected.expected_nullable
     or coalesce(c.column_default, '<NULL>') <> coalesce(expected.expected_default, '<NULL>');

  if v_mismatch_count > 0 then
    set @ddl = concat('alter table `', p_table, '` modify user_name varchar(30) not null, modify nick_name varchar(30) not null default '''', modify password varchar(100) not null comment ''密码密文''');
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_owner_generated_column//
create procedure assert_owner_generated_column(
  in p_table varchar(64),
  in p_column varchar(64),
  in p_subject_column varchar(64),
  in p_message varchar(128)
)
begin
  declare v_column_count int default 0;
  declare v_extra text default '';
  declare v_generation_expression text default '';
  declare v_normalized_expression text default '';
  declare v_expected_expression text default '';

  select count(1), coalesce(max(extra), ''), coalesce(max(generation_expression), '')
    into v_column_count, v_extra, v_generation_expression
  from information_schema.columns
  where table_schema = database() and table_name = p_table and column_name = p_column;

  set v_normalized_expression = lower(v_generation_expression);
  set v_normalized_expression = replace(v_normalized_expression, '`', '');
  set v_normalized_expression = replace(v_normalized_expression, ' ', '');
  set v_normalized_expression = replace(v_normalized_expression, '\n', '');
  set v_normalized_expression = replace(v_normalized_expression, '\r', '');
  set v_normalized_expression = replace(v_normalized_expression, '(', '');
  set v_normalized_expression = replace(v_normalized_expression, ')', '');
  set v_normalized_expression = replace(v_normalized_expression, '\\', '');
  set v_normalized_expression = replace(v_normalized_expression, '_utf8mb3', '');
  set v_normalized_expression = replace(v_normalized_expression, '_utf8mb4', '');
  set v_normalized_expression = replace(v_normalized_expression, '''', '');
  set v_expected_expression = concat('casewhenaccount_role=ownerthen', lower(p_subject_column), 'elsenullend');

  if v_column_count <> 1
      or upper(v_extra) not like '%STORED GENERATED%'
      or v_normalized_expression <> v_expected_expression then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_no_blank_terminal_passwords//
create procedure assert_no_blank_terminal_passwords(
  in p_table varchar(64),
  in p_message varchar(128)
)
begin
  declare v_column_count int default 0;

  select count(1)
    into v_column_count
  from information_schema.columns
  where table_schema = database()
    and table_name = p_table
    and column_name = 'password';

  if v_column_count <> 1 then
    signal sqlstate '45000' set message_text = 'terminal account password column is required before password preflight';
  end if;

  set @blank_terminal_password_count = 0;
  set @terminal_password_preflight_sql = concat(
    'select count(1) into @blank_terminal_password_count from `', p_table,
    '` where password is null or trim(password) = ', char(39), char(39)
  );
  prepare stmt from @terminal_password_preflight_sql;
  execute stmt;
  deallocate prepare stmt;

  if @blank_terminal_password_count > 0 then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_seller_account_normalize_targets//
create procedure assert_seller_account_normalize_targets()
begin
  declare v_count bigint default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(concat_ws('|',
           seller_account_id,
           seller_id,
           coalesce(user_name, '<NULL>'),
           coalesce(nick_name, '<NULL>'),
           coalesce(email, '<NULL>'),
           coalesce(phonenumber, '<NULL>'),
           coalesce(status, '<NULL>'),
           coalesce(lock_status, '<NULL>'),
           coalesce(lock_reason, '<NULL>'),
           coalesce(date_format(pwd_update_time, '%Y-%m-%d %H:%i:%s'), '<NULL>')
         ) order by seller_account_id separator '\n'), ''), 256)
    into v_count, v_signature
  from seller_account
  where coalesce(user_name, '') = ''
     or coalesce(nick_name, '') = ''
     or email is null
     or phonenumber is null
     or coalesce(status, '') = ''
     or coalesce(lock_status, '') not in ('0', '1')
     or lock_reason is null
     or pwd_update_time is null;

  if v_count <> @three_terminal_seller_account_normalize_expected_count then
    signal sqlstate '45000' set message_text = 'seller_account normalize exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@three_terminal_seller_account_normalize_expected_signature) then
    signal sqlstate '45000' set message_text = 'seller_account normalize exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_buyer_account_normalize_targets//
create procedure assert_buyer_account_normalize_targets()
begin
  declare v_count bigint default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(concat_ws('|',
           buyer_account_id,
           buyer_id,
           coalesce(user_name, '<NULL>'),
           coalesce(nick_name, '<NULL>'),
           coalesce(email, '<NULL>'),
           coalesce(phonenumber, '<NULL>'),
           coalesce(status, '<NULL>'),
           coalesce(lock_status, '<NULL>'),
           coalesce(lock_reason, '<NULL>'),
           coalesce(date_format(pwd_update_time, '%Y-%m-%d %H:%i:%s'), '<NULL>')
         ) order by buyer_account_id separator '\n'), ''), 256)
    into v_count, v_signature
  from buyer_account
  where coalesce(user_name, '') = ''
     or coalesce(nick_name, '') = ''
     or email is null
     or phonenumber is null
     or coalesce(status, '') = ''
     or coalesce(lock_status, '') not in ('0', '1')
     or lock_reason is null
     or pwd_update_time is null;

  if v_count <> @three_terminal_buyer_account_normalize_expected_count then
    signal sqlstate '45000' set message_text = 'buyer_account normalize exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@three_terminal_buyer_account_normalize_expected_signature) then
    signal sqlstate '45000' set message_text = 'buyer_account normalize exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_terminal_account_user_id_drop_target//
create procedure assert_terminal_account_user_id_drop_target(
  in p_table varchar(64),
  in p_expected_count bigint,
  in p_expected_signature varchar(64),
  in p_count_message varchar(128),
  in p_signature_message varchar(128)
)
begin
  declare v_count bigint default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(concat_ws('|',
           table_name,
           column_name,
           column_type,
           is_nullable,
           coalesce(column_default, '<NULL>'),
           coalesce(extra, '')
         ) order by table_name, column_name separator '\n'), ''), 256)
    into v_count, v_signature
  from information_schema.columns
  where table_schema = database()
    and table_name = p_table
    and column_name = 'user_id';

  if v_count <> p_expected_count then
    signal sqlstate '45000' set message_text = p_count_message;
  end if;
  if lower(v_signature) <> lower(p_expected_signature) then
    signal sqlstate '45000' set message_text = p_signature_message;
  end if;
end//

drop procedure if exists assert_terminal_menu_integrity_ready//
create procedure assert_terminal_menu_integrity_ready()
begin
  if exists (
    select 1
    from seller_menu
    where (
        menu_type in ('C', 'F')
        and coalesce(trim(perms), '') = ''
      )
       or (
        coalesce(trim(perms), '') <> ''
        and (
          coalesce(trim(perms), '') = '*'
          or coalesce(trim(perms), '') not like 'seller:%'
          or coalesce(trim(perms), '') like 'seller:admin:%'
          or coalesce(trim(perms), '') like 'buyer:%'
        )
      )
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu contains invalid terminal perms';
  end if;

  if exists (
    select 1
    from seller_menu
    where menu_type = 'C'
      and (
        coalesce(trim(component), '') = ''
        or coalesce(trim(component), '') not like 'Seller/%'
      )
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu page menus require component under Seller/';
  end if;

  if exists (
    select 1
    from (
      select trim(perms) as perms_unique
      from seller_menu
      where coalesce(trim(perms), '') <> ''
      group by trim(perms)
      having count(1) > 1
    ) duplicate_seller_menu_perms
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu perms must be unique before terminal role grants';
  end if;

  if exists (
    select 1
    from buyer_menu
    where (
        menu_type in ('C', 'F')
        and coalesce(trim(perms), '') = ''
      )
       or (
        coalesce(trim(perms), '') <> ''
        and (
          coalesce(trim(perms), '') = '*'
          or coalesce(trim(perms), '') not like 'buyer:%'
          or coalesce(trim(perms), '') like 'buyer:admin:%'
          or coalesce(trim(perms), '') like 'seller:%'
        )
      )
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu contains invalid terminal perms';
  end if;

  if exists (
    select 1
    from buyer_menu
    where menu_type = 'C'
      and (
        coalesce(trim(component), '') = ''
        or coalesce(trim(component), '') not like 'Buyer/%'
      )
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu page menus require component under Buyer/';
  end if;

  if exists (
    select 1
    from (
      select trim(perms) as perms_unique
      from buyer_menu
      where coalesce(trim(perms), '') <> ''
      group by trim(perms)
      having count(1) > 1
    ) duplicate_buyer_menu_perms
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu perms must be unique before terminal role grants';
  end if;
end//

drop procedure if exists assert_three_terminal_required_column//
create procedure assert_three_terminal_required_column(in p_table varchar(64), in p_column varchar(64), in p_message varchar(128))
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table
      and column_name = p_column
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_three_terminal_isolation_migration_completed//
create procedure assert_three_terminal_isolation_migration_completed()
begin
  call assert_three_terminal_required_column('seller_account', 'seller_account_id', 'seller_account.seller_account_id final column is required');
  call assert_three_terminal_required_column('seller_account', 'seller_id', 'seller_account.seller_id final column is required');
  call assert_three_terminal_required_column('seller_account', 'dept_id', 'seller_account.dept_id final column is required');
  call assert_three_terminal_required_column('seller_account', 'user_name', 'seller_account.user_name final column is required');
  call assert_three_terminal_required_column('seller_account', 'password', 'seller_account.password final column is required');
  call assert_three_terminal_required_column('seller_account', 'account_role', 'seller_account.account_role final column is required');
  call assert_three_terminal_required_column('seller_account', 'status', 'seller_account.status final column is required');
  call assert_three_terminal_required_column('seller_account', 'lock_status', 'seller_account.lock_status final column is required');
  call assert_three_terminal_required_column('seller_account', 'last_login_time', 'seller_account.last_login_time final column is required');
  call assert_three_terminal_required_column('seller_account', 'pwd_update_time', 'seller_account.pwd_update_time final column is required');
  call assert_three_terminal_required_column('buyer_account', 'buyer_account_id', 'buyer_account.buyer_account_id final column is required');
  call assert_three_terminal_required_column('buyer_account', 'buyer_id', 'buyer_account.buyer_id final column is required');
  call assert_three_terminal_required_column('buyer_account', 'dept_id', 'buyer_account.dept_id final column is required');
  call assert_three_terminal_required_column('buyer_account', 'user_name', 'buyer_account.user_name final column is required');
  call assert_three_terminal_required_column('buyer_account', 'password', 'buyer_account.password final column is required');
  call assert_three_terminal_required_column('buyer_account', 'account_role', 'buyer_account.account_role final column is required');
  call assert_three_terminal_required_column('buyer_account', 'status', 'buyer_account.status final column is required');
  call assert_three_terminal_required_column('buyer_account', 'lock_status', 'buyer_account.lock_status final column is required');
  call assert_three_terminal_required_column('buyer_account', 'last_login_time', 'buyer_account.last_login_time final column is required');
  call assert_three_terminal_required_column('buyer_account', 'pwd_update_time', 'buyer_account.pwd_update_time final column is required');

  if exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = 'seller_account'
      and column_name = 'user_id'
  ) then
    signal sqlstate '45000' set message_text = 'seller_account.user_id must be removed after isolation migration';
  end if;
  if exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = 'buyer_account'
      and column_name = 'user_id'
  ) then
    signal sqlstate '45000' set message_text = 'buyer_account.user_id must be removed after isolation migration';
  end if;

  if exists (
    select 1
    from seller_menu
    where seller_menu_id not between 100000 and 199999
       or (parent_id <> 0 and parent_id not between 100000 and 199999)
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu ids must stay in seller terminal range';
  end if;
  if exists (
    select 1
    from buyer_menu
    where buyer_menu_id not between 200000 and 299999
       or (parent_id <> 0 and parent_id not between 200000 and 299999)
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu ids must stay in buyer terminal range';
  end if;

  if exists (
    select 1
    from seller_role_menu rm
    left join seller_role r on r.seller_role_id = rm.seller_role_id
    left join seller_menu m on m.seller_menu_id = rm.seller_menu_id
    where r.seller_role_id is null or m.seller_menu_id is null
  ) then
    signal sqlstate '45000' set message_text = 'seller_role_menu contains orphan role or menu ids';
  end if;
  if exists (
    select 1
    from buyer_role_menu rm
    left join buyer_role r on r.buyer_role_id = rm.buyer_role_id
    left join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
    where r.buyer_role_id is null or m.buyer_menu_id is null
  ) then
    signal sqlstate '45000' set message_text = 'buyer_role_menu contains orphan role or menu ids';
  end if;

  call assert_three_terminal_required_column('seller_login_log', 'seller_id', 'seller_login_log.seller_id final column is required');
  call assert_three_terminal_required_column('seller_login_log', 'seller_account_id', 'seller_login_log.seller_account_id final column is required');
  call assert_three_terminal_required_column('seller_login_log', 'direct_login', 'seller_login_log.direct_login final column is required');
  call assert_three_terminal_required_column('seller_login_log', 'direct_login_ticket_id', 'seller_login_log.ticket final column is required');
  call assert_three_terminal_required_column('seller_login_log', 'acting_admin_id', 'seller_login_log.admin_id final column is required');
  call assert_three_terminal_required_column('seller_login_log', 'acting_admin_name', 'seller_login_log.admin_name final column is required');
  call assert_three_terminal_required_column('seller_login_log', 'direct_login_reason', 'seller_login_log.reason final column is required');
  call assert_three_terminal_required_column('buyer_login_log', 'buyer_id', 'buyer_login_log.buyer_id final column is required');
  call assert_three_terminal_required_column('buyer_login_log', 'buyer_account_id', 'buyer_login_log.buyer_account_id final column is required');
  call assert_three_terminal_required_column('buyer_login_log', 'direct_login', 'buyer_login_log.direct_login final column is required');
  call assert_three_terminal_required_column('buyer_login_log', 'direct_login_ticket_id', 'buyer_login_log.ticket final column is required');
  call assert_three_terminal_required_column('buyer_login_log', 'acting_admin_id', 'buyer_login_log.admin_id final column is required');
  call assert_three_terminal_required_column('buyer_login_log', 'acting_admin_name', 'buyer_login_log.admin_name final column is required');
  call assert_three_terminal_required_column('buyer_login_log', 'direct_login_reason', 'buyer_login_log.reason final column is required');
  call assert_three_terminal_required_column('seller_oper_log', 'seller_id', 'seller_oper_log.seller_id final column is required');
  call assert_three_terminal_required_column('seller_oper_log', 'seller_account_id', 'seller_oper_log.seller_account_id final column is required');
  call assert_three_terminal_required_column('seller_oper_log', 'direct_login', 'seller_oper_log.direct_login final column is required');
  call assert_three_terminal_required_column('seller_oper_log', 'direct_login_ticket_id', 'seller_oper_log.ticket final column is required');
  call assert_three_terminal_required_column('seller_oper_log', 'acting_admin_id', 'seller_oper_log.admin_id final column is required');
  call assert_three_terminal_required_column('seller_oper_log', 'acting_admin_name', 'seller_oper_log.admin_name final column is required');
  call assert_three_terminal_required_column('seller_oper_log', 'direct_login_reason', 'seller_oper_log.reason final column is required');
  call assert_three_terminal_required_column('buyer_oper_log', 'buyer_id', 'buyer_oper_log.buyer_id final column is required');
  call assert_three_terminal_required_column('buyer_oper_log', 'buyer_account_id', 'buyer_oper_log.buyer_account_id final column is required');
  call assert_three_terminal_required_column('buyer_oper_log', 'direct_login', 'buyer_oper_log.direct_login final column is required');
  call assert_three_terminal_required_column('buyer_oper_log', 'direct_login_ticket_id', 'buyer_oper_log.ticket final column is required');
  call assert_three_terminal_required_column('buyer_oper_log', 'acting_admin_id', 'buyer_oper_log.admin_id final column is required');
  call assert_three_terminal_required_column('buyer_oper_log', 'acting_admin_name', 'buyer_oper_log.admin_name final column is required');
  call assert_three_terminal_required_column('buyer_oper_log', 'direct_login_reason', 'buyer_oper_log.reason final column is required');
  call assert_three_terminal_required_column('seller_session', 'seller_id', 'seller_session.seller_id final column is required');
  call assert_three_terminal_required_column('seller_session', 'seller_account_id', 'seller_session.seller_account_id final column is required');
  call assert_three_terminal_required_column('seller_session', 'direct_login', 'seller_session.direct_login final column is required');
  call assert_three_terminal_required_column('seller_session', 'direct_login_ticket_id', 'seller_session.ticket final column is required');
  call assert_three_terminal_required_column('seller_session', 'acting_admin_id', 'seller_session.admin_id final column is required');
  call assert_three_terminal_required_column('seller_session', 'acting_admin_name', 'seller_session.admin_name final column is required');
  call assert_three_terminal_required_column('seller_session', 'direct_login_reason', 'seller_session.reason final column is required');
  call assert_three_terminal_required_column('buyer_session', 'buyer_id', 'buyer_session.buyer_id final column is required');
  call assert_three_terminal_required_column('buyer_session', 'buyer_account_id', 'buyer_session.buyer_account_id final column is required');
  call assert_three_terminal_required_column('buyer_session', 'direct_login', 'buyer_session.direct_login final column is required');
  call assert_three_terminal_required_column('buyer_session', 'direct_login_ticket_id', 'buyer_session.ticket final column is required');
  call assert_three_terminal_required_column('buyer_session', 'acting_admin_id', 'buyer_session.admin_id final column is required');
  call assert_three_terminal_required_column('buyer_session', 'acting_admin_name', 'buyer_session.admin_name final column is required');
  call assert_three_terminal_required_column('buyer_session', 'direct_login_reason', 'buyer_session.reason final column is required');
end//

delimiter ;

call assert_three_terminal_isolation_preflight();

create table if not exists seller_account (
  seller_account_id     bigint(20)      not null auto_increment,
  seller_id             bigint(20)      not null,
  dept_id               bigint(20)      default null,
  user_name             varchar(30)     not null,
  nick_name             varchar(30)     not null default '',
  password              varchar(100)    not null,
  email                 varchar(50)     default '',
  phonenumber           varchar(32)     default '',
  account_role          varchar(32)     not null default 'OWNER',
  status                char(1)         not null default '0',
  lock_status           char(1)         not null default '0',
  lock_reason           varchar(500)    not null default '',
  last_login_ip         varchar(128)    default '',
  last_login_time       datetime,
  pwd_update_time       datetime,
  owner_unique_seller_id bigint(20) generated always as (case when account_role = 'OWNER' then seller_id else null end) stored,
  create_by             varchar(64)     default '',
  create_time           datetime,
  update_by             varchar(64)     default '',
  update_time           datetime,
  remark                varchar(500)    default '',
  primary key (seller_account_id),
  unique key uk_seller_account_username (user_name),
  unique key uk_seller_account_owner (owner_unique_seller_id),
  key idx_seller_account_seller_status (seller_id, status),
  key idx_seller_account_seller_lock (seller_id, lock_status)
) engine=innodb auto_increment=1 comment = '卖家端账号表';

create table if not exists buyer_account (
  buyer_account_id      bigint(20)      not null auto_increment,
  buyer_id              bigint(20)      not null,
  dept_id               bigint(20)      default null,
  user_name             varchar(30)     not null,
  nick_name             varchar(30)     not null default '',
  password              varchar(100)    not null,
  email                 varchar(50)     default '',
  phonenumber           varchar(32)     default '',
  account_role          varchar(32)     not null default 'OWNER',
  status                char(1)         not null default '0',
  lock_status           char(1)         not null default '0',
  lock_reason           varchar(500)    not null default '',
  last_login_ip         varchar(128)    default '',
  last_login_time       datetime,
  pwd_update_time       datetime,
  owner_unique_buyer_id bigint(20) generated always as (case when account_role = 'OWNER' then buyer_id else null end) stored,
  create_by             varchar(64)     default '',
  create_time           datetime,
  update_by             varchar(64)     default '',
  update_time           datetime,
  remark                varchar(500)    default '',
  primary key (buyer_account_id),
  unique key uk_buyer_account_username (user_name),
  unique key uk_buyer_account_owner (owner_unique_buyer_id),
  key idx_buyer_account_buyer_status (buyer_id, status),
  key idx_buyer_account_buyer_lock (buyer_id, lock_status)
) engine=innodb auto_increment=1 comment = '买家端账号表';

call add_column_if_missing('seller_account', 'dept_id', 'bigint(20) default null');
call add_column_if_missing('seller_account', 'user_name', 'varchar(30) null');
call add_column_if_missing('seller_account', 'nick_name', 'varchar(30) null');
call add_column_if_missing('seller_account', 'password', 'varchar(100) null');
call add_column_if_missing('seller_account', 'email', 'varchar(50) null');
call add_column_if_missing('seller_account', 'phonenumber', 'varchar(32) null');
call add_column_if_missing('seller_account', 'lock_status', 'char(1) not null default ''0''');
call add_column_if_missing('seller_account', 'lock_reason', 'varchar(500) not null default ''''');
call add_column_if_missing('seller_account', 'last_login_ip', 'varchar(128) null');
call add_column_if_missing('seller_account', 'last_login_time', 'datetime null');
call add_column_if_missing('seller_account', 'pwd_update_time', 'datetime null');
call add_column_if_missing('seller_account', 'owner_unique_seller_id', 'bigint(20) generated always as (case when account_role = ''OWNER'' then seller_id else null end) stored');

call add_column_if_missing('buyer_account', 'dept_id', 'bigint(20) default null');
call add_column_if_missing('buyer_account', 'user_name', 'varchar(30) null');
call add_column_if_missing('buyer_account', 'nick_name', 'varchar(30) null');
call add_column_if_missing('buyer_account', 'password', 'varchar(100) null');
call add_column_if_missing('buyer_account', 'email', 'varchar(50) null');
call add_column_if_missing('buyer_account', 'phonenumber', 'varchar(32) null');
call add_column_if_missing('buyer_account', 'lock_status', 'char(1) not null default ''0''');
call add_column_if_missing('buyer_account', 'lock_reason', 'varchar(500) not null default ''''');
call add_column_if_missing('buyer_account', 'last_login_ip', 'varchar(128) null');
call add_column_if_missing('buyer_account', 'last_login_time', 'datetime null');
call add_column_if_missing('buyer_account', 'pwd_update_time', 'datetime null');
call add_column_if_missing('buyer_account', 'owner_unique_buyer_id', 'bigint(20) generated always as (case when account_role = ''OWNER'' then buyer_id else null end) stored');

call assert_owner_generated_column('seller_account', 'owner_unique_seller_id', 'seller_id', 'seller owner generated column definition is invalid');
call assert_owner_generated_column('buyer_account', 'owner_unique_buyer_id', 'buyer_id', 'buyer owner generated column definition is invalid');
call assert_no_blank_terminal_passwords('seller_account', 'seller_account contains blank passwords; reset or backfill before isolation migration');
call assert_no_blank_terminal_passwords('buyer_account', 'buyer_account contains blank passwords; reset or backfill before isolation migration');
call assert_seller_account_normalize_targets();
call assert_buyer_account_normalize_targets();

update seller_account
set user_name = coalesce(nullif(user_name, ''), concat('seller_', seller_account_id)),
    nick_name = coalesce(nullif(nick_name, ''), user_name),
    email = coalesce(email, ''),
    phonenumber = coalesce(phonenumber, ''),
    status = coalesce(nullif(status, ''), '0'),
    lock_status = case when lock_status in ('0', '1') then lock_status else '0' end,
    lock_reason = coalesce(lock_reason, ''),
    pwd_update_time = coalesce(pwd_update_time, sysdate())
where coalesce(user_name, '') = ''
   or coalesce(nick_name, '') = ''
   or email is null
   or phonenumber is null
   or coalesce(status, '') = ''
   or coalesce(lock_status, '') not in ('0', '1')
   or lock_reason is null
   or pwd_update_time is null;

update buyer_account
set user_name = coalesce(nullif(user_name, ''), concat('buyer_', buyer_account_id)),
    nick_name = coalesce(nullif(nick_name, ''), user_name),
    email = coalesce(email, ''),
    phonenumber = coalesce(phonenumber, ''),
    status = coalesce(nullif(status, ''), '0'),
    lock_status = case when lock_status in ('0', '1') then lock_status else '0' end,
    lock_reason = coalesce(lock_reason, ''),
    pwd_update_time = coalesce(pwd_update_time, sysdate())
where coalesce(user_name, '') = ''
   or coalesce(nick_name, '') = ''
   or email is null
   or phonenumber is null
   or coalesce(status, '') = ''
   or coalesce(lock_status, '') not in ('0', '1')
   or lock_reason is null
   or pwd_update_time is null;

call assert_no_duplicate_terminal_user_name('seller_account', 'seller_account has duplicate user_name values before username unique index migration');
call assert_no_duplicate_terminal_user_name('buyer_account', 'buyer_account has duplicate user_name values before username unique index migration');
call assert_terminal_account_user_id_drop_target(
  'seller_account',
  @three_terminal_seller_user_id_drop_expected_count,
  @three_terminal_seller_user_id_drop_expected_signature,
  'seller_account user_id column drop exact target count mismatch',
  'seller_account user_id column drop exact target signature mismatch'
);
call assert_terminal_account_user_id_drop_target(
  'buyer_account',
  @three_terminal_buyer_user_id_drop_expected_count,
  @three_terminal_buyer_user_id_drop_expected_signature,
  'buyer_account user_id column drop exact target count mismatch',
  'buyer_account user_id column drop exact target signature mismatch'
);

call drop_index_if_exists('seller_account', 'uk_seller_account_user');
call drop_index_if_exists('seller_account', 'uk_seller_account_seller_user');
call drop_index_if_exists('buyer_account', 'uk_buyer_account_user');
call drop_index_if_exists('buyer_account', 'uk_buyer_account_buyer_user');
call drop_column_if_exists('seller_account', 'user_id');
call drop_column_if_exists('buyer_account', 'user_id');

call modify_terminal_account_identity_columns_if_needed('seller_account');
call modify_terminal_account_identity_columns_if_needed('buyer_account');
call recreate_index_if_mismatch('seller_account', 'uk_seller_account_username',
  'user_name', 0, 'unique key uk_seller_account_username (user_name)');
call recreate_index_if_mismatch('buyer_account', 'uk_buyer_account_username',
  'user_name', 0, 'unique key uk_buyer_account_username (user_name)');
call assert_index_definition('seller_account', 'uk_seller_account_username',
  'user_name', 0, 'seller_account username unique index is invalid');
call assert_index_definition('buyer_account', 'uk_buyer_account_username',
  'user_name', 0, 'buyer_account username unique index is invalid');
call assert_no_duplicate_owner_account('seller_account', 'seller_id', 'seller_account has duplicate OWNER accounts');
call assert_no_duplicate_owner_account('buyer_account', 'buyer_id', 'buyer_account has duplicate OWNER accounts');
call recreate_index_if_mismatch('seller_account', 'uk_seller_account_owner',
  'owner_unique_seller_id', 0, 'unique key uk_seller_account_owner (owner_unique_seller_id)');
call recreate_index_if_mismatch('buyer_account', 'uk_buyer_account_owner',
  'owner_unique_buyer_id', 0, 'unique key uk_buyer_account_owner (owner_unique_buyer_id)');
call assert_index_definition('seller_account', 'uk_seller_account_owner',
  'owner_unique_seller_id', 0, 'seller_account OWNER unique index is invalid');
call assert_index_definition('buyer_account', 'uk_buyer_account_owner',
  'owner_unique_buyer_id', 0, 'buyer_account OWNER unique index is invalid');
call add_index_if_missing('seller_account', 'idx_seller_account_seller_lock', 'key idx_seller_account_seller_lock (seller_id, lock_status)');
call add_index_if_missing('buyer_account', 'idx_buyer_account_buyer_lock', 'key idx_buyer_account_buyer_lock (buyer_id, lock_status)');

create table if not exists seller_dept (
  seller_dept_id bigint(20) not null auto_increment,
  seller_id bigint(20) not null,
  parent_id bigint(20) default 0,
  ancestors varchar(500) default '',
  dept_name varchar(100) not null,
  order_num int default 0,
  leader varchar(100) default '',
  phone varchar(32) default '',
  email varchar(128) default '',
  status char(1) not null default '0',
  del_flag char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  primary key (seller_dept_id),
  key idx_seller_dept_seller_parent (seller_id, parent_id),
  key idx_seller_dept_status (seller_id, status)
) engine=innodb auto_increment=1 comment = '卖家端部门表';

create table if not exists buyer_dept (
  buyer_dept_id bigint(20) not null auto_increment,
  buyer_id bigint(20) not null,
  parent_id bigint(20) default 0,
  ancestors varchar(500) default '',
  dept_name varchar(100) not null,
  order_num int default 0,
  leader varchar(100) default '',
  phone varchar(32) default '',
  email varchar(128) default '',
  status char(1) not null default '0',
  del_flag char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  primary key (buyer_dept_id),
  key idx_buyer_dept_buyer_parent (buyer_id, parent_id),
  key idx_buyer_dept_status (buyer_id, status)
) engine=innodb auto_increment=1 comment = '买家端部门表';

create table if not exists seller_role (
  seller_role_id bigint(20) not null auto_increment,
  seller_id bigint(20) not null,
  role_name varchar(64) not null,
  role_key varchar(64) not null,
  role_sort int not null default 0,
  status char(1) not null default '0',
  del_flag char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  remark varchar(500) default '',
  primary key (seller_role_id),
  unique key uk_seller_role_key (seller_id, role_key),
  key idx_seller_role_status (seller_id, status)
) engine=innodb auto_increment=1 comment = '卖家端角色表';

create table if not exists buyer_role (
  buyer_role_id bigint(20) not null auto_increment,
  buyer_id bigint(20) not null,
  role_name varchar(64) not null,
  role_key varchar(64) not null,
  role_sort int not null default 0,
  status char(1) not null default '0',
  del_flag char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  remark varchar(500) default '',
  primary key (buyer_role_id),
  unique key uk_buyer_role_key (buyer_id, role_key),
  key idx_buyer_role_status (buyer_id, status)
) engine=innodb auto_increment=1 comment = '买家端角色表';

create table if not exists seller_menu (
  seller_menu_id bigint(20) not null auto_increment,
  menu_name varchar(64) not null,
  parent_id bigint(20) default 0,
  order_num int default 0,
  path varchar(200) default '',
  component varchar(255) default null,
  query varchar(255) default '',
  route_name varchar(64) default '',
  is_frame int default 1,
  is_cache int default 0,
  menu_type char(1) not null default 'M',
  visible char(1) not null default '0',
  status char(1) not null default '0',
  perms varchar(100) default '',
  perms_unique_key varchar(100) generated always as (case when trim(coalesce(perms, '')) = '' then null else trim(perms) end) stored,
  icon varchar(100) default '#',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  remark varchar(500) default '',
  primary key (seller_menu_id),
  unique key uk_seller_menu_perms (perms_unique_key),
  key idx_seller_menu_perms_lookup (perms),
  key idx_seller_menu_parent (parent_id),
  key idx_seller_menu_status (status)
) engine=innodb auto_increment=100000 comment = '卖家端菜单权限表';

create table if not exists buyer_menu (
  buyer_menu_id bigint(20) not null auto_increment,
  menu_name varchar(64) not null,
  parent_id bigint(20) default 0,
  order_num int default 0,
  path varchar(200) default '',
  component varchar(255) default null,
  query varchar(255) default '',
  route_name varchar(64) default '',
  is_frame int default 1,
  is_cache int default 0,
  menu_type char(1) not null default 'M',
  visible char(1) not null default '0',
  status char(1) not null default '0',
  perms varchar(100) default '',
  perms_unique_key varchar(100) generated always as (case when trim(coalesce(perms, '')) = '' then null else trim(perms) end) stored,
  icon varchar(100) default '#',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  remark varchar(500) default '',
  primary key (buyer_menu_id),
  unique key uk_buyer_menu_perms (perms_unique_key),
  key idx_buyer_menu_perms_lookup (perms),
  key idx_buyer_menu_parent (parent_id),
  key idx_buyer_menu_status (status)
) engine=innodb auto_increment=200000 comment = '买家端菜单权限表';

create table if not exists seller_account_role (
  seller_account_id bigint(20) not null,
  seller_role_id bigint(20) not null,
  primary key (seller_account_id, seller_role_id)
) engine=innodb comment = '卖家端账号角色关联表';

create table if not exists buyer_account_role (
  buyer_account_id bigint(20) not null,
  buyer_role_id bigint(20) not null,
  primary key (buyer_account_id, buyer_role_id)
) engine=innodb comment = '买家端账号角色关联表';

create table if not exists seller_role_menu (
  seller_role_id bigint(20) not null,
  seller_menu_id bigint(20) not null,
  primary key (seller_role_id, seller_menu_id)
) engine=innodb comment = '卖家端角色菜单关联表';

create table if not exists buyer_role_menu (
  buyer_role_id bigint(20) not null,
  buyer_menu_id bigint(20) not null,
  primary key (buyer_role_id, buyer_menu_id)
) engine=innodb comment = '买家端角色菜单关联表';

call assert_terminal_menu_integrity_ready();
call add_column_if_missing('seller_menu', 'perms_unique_key',
  'varchar(100) generated always as (case when trim(coalesce(perms, '''')) = '''' then null else trim(perms) end) stored');
call add_column_if_missing('buyer_menu', 'perms_unique_key',
  'varchar(100) generated always as (case when trim(coalesce(perms, '''')) = '''' then null else trim(perms) end) stored');
call recreate_index_if_mismatch('seller_menu', 'uk_seller_menu_perms',
  'perms_unique_key', 0, 'unique key uk_seller_menu_perms (perms_unique_key)');
call recreate_index_if_mismatch('buyer_menu', 'uk_buyer_menu_perms',
  'perms_unique_key', 0, 'unique key uk_buyer_menu_perms (perms_unique_key)');
call assert_index_definition('seller_menu', 'uk_seller_menu_perms',
  'perms_unique_key', 0, 'seller_menu perms unique index is invalid');
call assert_index_definition('buyer_menu', 'uk_buyer_menu_perms',
  'perms_unique_key', 0, 'buyer_menu perms unique index is invalid');

create table if not exists seller_login_log (
  info_id bigint(20) not null auto_increment,
  seller_id bigint(20) default null,
  seller_account_id bigint(20) default null,
  user_name varchar(30) default '',
  ipaddr varchar(128) default '',
  login_location varchar(255) default '',
  browser varchar(50) default '',
  os varchar(50) default '',
  status char(1) default '0',
  msg varchar(255) default '',
  direct_login tinyint(1) not null default 0,
  direct_login_ticket_id bigint(20) default null,
  acting_admin_id bigint(20) default null,
  acting_admin_name varchar(64) default '',
  direct_login_reason varchar(255) default '',
  login_time datetime,
  primary key (info_id),
  key idx_seller_login_log_account_time (seller_account_id, login_time),
  key idx_seller_login_log_seller_time (seller_id, login_time)
) engine=innodb auto_increment=1 comment = '卖家端登录日志表';
call add_column_if_missing('seller_login_log', 'seller_id', 'bigint(20) default null');
call add_column_if_missing('seller_login_log', 'seller_account_id', 'bigint(20) default null');
call add_column_if_missing('seller_login_log', 'direct_login', 'tinyint(1) not null default 0');
call add_column_if_missing('seller_login_log', 'direct_login_ticket_id', 'bigint(20) default null');
call add_column_if_missing('seller_login_log', 'acting_admin_id', 'bigint(20) default null');
call add_column_if_missing('seller_login_log', 'acting_admin_name', 'varchar(64) default ''''');
call add_column_if_missing('seller_login_log', 'direct_login_reason', 'varchar(255) default ''''');
call recreate_index_if_mismatch('seller_login_log', 'idx_seller_login_log_account_time',
  'seller_account_id,login_time', 1, 'key idx_seller_login_log_account_time (seller_account_id, login_time)');
call recreate_index_if_mismatch('seller_login_log', 'idx_seller_login_log_seller_time',
  'seller_id,login_time', 1, 'key idx_seller_login_log_seller_time (seller_id, login_time)');
call assert_index_definition('seller_login_log', 'idx_seller_login_log_account_time',
  'seller_account_id,login_time', 1, 'seller_login_log account/time index is invalid');
call assert_index_definition('seller_login_log', 'idx_seller_login_log_seller_time',
  'seller_id,login_time', 1, 'seller_login_log seller/time index is invalid');

create table if not exists buyer_login_log (
  info_id bigint(20) not null auto_increment,
  buyer_id bigint(20) default null,
  buyer_account_id bigint(20) default null,
  user_name varchar(30) default '',
  ipaddr varchar(128) default '',
  login_location varchar(255) default '',
  browser varchar(50) default '',
  os varchar(50) default '',
  status char(1) default '0',
  msg varchar(255) default '',
  direct_login tinyint(1) not null default 0,
  direct_login_ticket_id bigint(20) default null,
  acting_admin_id bigint(20) default null,
  acting_admin_name varchar(64) default '',
  direct_login_reason varchar(255) default '',
  login_time datetime,
  primary key (info_id),
  key idx_buyer_login_log_account_time (buyer_account_id, login_time),
  key idx_buyer_login_log_buyer_time (buyer_id, login_time)
) engine=innodb auto_increment=1 comment = '买家端登录日志表';
call add_column_if_missing('buyer_login_log', 'buyer_id', 'bigint(20) default null');
call add_column_if_missing('buyer_login_log', 'buyer_account_id', 'bigint(20) default null');
call add_column_if_missing('buyer_login_log', 'direct_login', 'tinyint(1) not null default 0');
call add_column_if_missing('buyer_login_log', 'direct_login_ticket_id', 'bigint(20) default null');
call add_column_if_missing('buyer_login_log', 'acting_admin_id', 'bigint(20) default null');
call add_column_if_missing('buyer_login_log', 'acting_admin_name', 'varchar(64) default ''''');
call add_column_if_missing('buyer_login_log', 'direct_login_reason', 'varchar(255) default ''''');
call recreate_index_if_mismatch('buyer_login_log', 'idx_buyer_login_log_account_time',
  'buyer_account_id,login_time', 1, 'key idx_buyer_login_log_account_time (buyer_account_id, login_time)');
call recreate_index_if_mismatch('buyer_login_log', 'idx_buyer_login_log_buyer_time',
  'buyer_id,login_time', 1, 'key idx_buyer_login_log_buyer_time (buyer_id, login_time)');
call assert_index_definition('buyer_login_log', 'idx_buyer_login_log_account_time',
  'buyer_account_id,login_time', 1, 'buyer_login_log account/time index is invalid');
call assert_index_definition('buyer_login_log', 'idx_buyer_login_log_buyer_time',
  'buyer_id,login_time', 1, 'buyer_login_log buyer/time index is invalid');

create table if not exists seller_oper_log (
  oper_id bigint(20) not null auto_increment,
  seller_id bigint(20) default null,
  seller_account_id bigint(20) default null,
  title varchar(50) default '',
  business_type int default 0,
  method varchar(200) default '',
  request_method varchar(10) default '',
  oper_name varchar(30) default '',
  oper_url varchar(255) default '',
  oper_ip varchar(128) default '',
  oper_location varchar(255) default '',
  oper_param varchar(2000) default '',
  json_result varchar(2000) default '',
  direct_login tinyint(1) not null default 0,
  direct_login_ticket_id bigint(20) default null,
  acting_admin_id bigint(20) default null,
  acting_admin_name varchar(64) default '',
  direct_login_reason varchar(255) default '',
  status int default 0,
  error_msg varchar(2000) default '',
  oper_time datetime,
  cost_time bigint(20) default 0,
  primary key (oper_id),
  key idx_seller_oper_log_account_time (seller_account_id, oper_time),
  key idx_seller_oper_log_seller_time (seller_id, oper_time)
) engine=innodb auto_increment=1 comment = '卖家端操作日志表';
call add_column_if_missing('seller_oper_log', 'seller_id', 'bigint(20) default null');
call add_column_if_missing('seller_oper_log', 'seller_account_id', 'bigint(20) default null');
call add_column_if_missing('seller_oper_log', 'direct_login', 'tinyint(1) not null default 0');
call add_column_if_missing('seller_oper_log', 'direct_login_ticket_id', 'bigint(20) default null');
call add_column_if_missing('seller_oper_log', 'acting_admin_id', 'bigint(20) default null');
call add_column_if_missing('seller_oper_log', 'acting_admin_name', 'varchar(64) default ''''');
call add_column_if_missing('seller_oper_log', 'direct_login_reason', 'varchar(255) default ''''');
call recreate_index_if_mismatch('seller_oper_log', 'idx_seller_oper_log_account_time',
  'seller_account_id,oper_time', 1, 'key idx_seller_oper_log_account_time (seller_account_id, oper_time)');
call recreate_index_if_mismatch('seller_oper_log', 'idx_seller_oper_log_seller_time',
  'seller_id,oper_time', 1, 'key idx_seller_oper_log_seller_time (seller_id, oper_time)');
call assert_index_definition('seller_oper_log', 'idx_seller_oper_log_account_time',
  'seller_account_id,oper_time', 1, 'seller_oper_log account/time index is invalid');
call assert_index_definition('seller_oper_log', 'idx_seller_oper_log_seller_time',
  'seller_id,oper_time', 1, 'seller_oper_log seller/time index is invalid');

create table if not exists buyer_oper_log (
  oper_id bigint(20) not null auto_increment,
  buyer_id bigint(20) default null,
  buyer_account_id bigint(20) default null,
  title varchar(50) default '',
  business_type int default 0,
  method varchar(200) default '',
  request_method varchar(10) default '',
  oper_name varchar(30) default '',
  oper_url varchar(255) default '',
  oper_ip varchar(128) default '',
  oper_location varchar(255) default '',
  oper_param varchar(2000) default '',
  json_result varchar(2000) default '',
  direct_login tinyint(1) not null default 0,
  direct_login_ticket_id bigint(20) default null,
  acting_admin_id bigint(20) default null,
  acting_admin_name varchar(64) default '',
  direct_login_reason varchar(255) default '',
  status int default 0,
  error_msg varchar(2000) default '',
  oper_time datetime,
  cost_time bigint(20) default 0,
  primary key (oper_id),
  key idx_buyer_oper_log_account_time (buyer_account_id, oper_time),
  key idx_buyer_oper_log_buyer_time (buyer_id, oper_time)
) engine=innodb auto_increment=1 comment = '买家端操作日志表';
call add_column_if_missing('buyer_oper_log', 'buyer_id', 'bigint(20) default null');
call add_column_if_missing('buyer_oper_log', 'buyer_account_id', 'bigint(20) default null');
call add_column_if_missing('buyer_oper_log', 'direct_login', 'tinyint(1) not null default 0');
call add_column_if_missing('buyer_oper_log', 'direct_login_ticket_id', 'bigint(20) default null');
call add_column_if_missing('buyer_oper_log', 'acting_admin_id', 'bigint(20) default null');
call add_column_if_missing('buyer_oper_log', 'acting_admin_name', 'varchar(64) default ''''');
call add_column_if_missing('buyer_oper_log', 'direct_login_reason', 'varchar(255) default ''''');
call recreate_index_if_mismatch('buyer_oper_log', 'idx_buyer_oper_log_account_time',
  'buyer_account_id,oper_time', 1, 'key idx_buyer_oper_log_account_time (buyer_account_id, oper_time)');
call recreate_index_if_mismatch('buyer_oper_log', 'idx_buyer_oper_log_buyer_time',
  'buyer_id,oper_time', 1, 'key idx_buyer_oper_log_buyer_time (buyer_id, oper_time)');
call assert_index_definition('buyer_oper_log', 'idx_buyer_oper_log_account_time',
  'buyer_account_id,oper_time', 1, 'buyer_oper_log account/time index is invalid');
call assert_index_definition('buyer_oper_log', 'idx_buyer_oper_log_buyer_time',
  'buyer_id,oper_time', 1, 'buyer_oper_log buyer/time index is invalid');

create table if not exists seller_session (
  token_id varchar(64) not null,
  seller_id bigint(20) not null,
  seller_account_id bigint(20) not null,
  user_name varchar(30) not null,
  login_ip varchar(128) default '',
  login_time datetime,
  expire_time datetime,
  logout_time datetime,
  status char(1) not null default '0',
  direct_login tinyint(1) not null default 0,
  direct_login_ticket_id bigint(20) default null,
  acting_admin_id bigint(20) default null,
  acting_admin_name varchar(64) default '',
  direct_login_reason varchar(255) default '',
  primary key (token_id),
  key idx_seller_session_account (seller_account_id),
  key idx_seller_session_expire (expire_time)
) engine=innodb comment = '卖家端会话表';

create table if not exists buyer_session (
  token_id varchar(64) not null,
  buyer_id bigint(20) not null,
  buyer_account_id bigint(20) not null,
  user_name varchar(30) not null,
  login_ip varchar(128) default '',
  login_time datetime,
  expire_time datetime,
  logout_time datetime,
  status char(1) not null default '0',
  direct_login tinyint(1) not null default 0,
  direct_login_ticket_id bigint(20) default null,
  acting_admin_id bigint(20) default null,
  acting_admin_name varchar(64) default '',
  direct_login_reason varchar(255) default '',
  primary key (token_id),
  key idx_buyer_session_account (buyer_account_id),
  key idx_buyer_session_expire (expire_time)
) engine=innodb comment = '买家端会话表';

call add_column_if_missing('seller_session', 'direct_login', 'tinyint(1) not null default 0');
call add_column_if_missing('seller_session', 'direct_login_ticket_id', 'bigint(20) default null');
call add_column_if_missing('seller_session', 'acting_admin_id', 'bigint(20) default null');
call add_column_if_missing('seller_session', 'acting_admin_name', 'varchar(64) default ''''');
call add_column_if_missing('seller_session', 'direct_login_reason', 'varchar(255) default ''''');
call add_column_if_missing('buyer_session', 'direct_login', 'tinyint(1) not null default 0');
call add_column_if_missing('buyer_session', 'direct_login_ticket_id', 'bigint(20) default null');
call add_column_if_missing('buyer_session', 'acting_admin_id', 'bigint(20) default null');
call add_column_if_missing('buyer_session', 'acting_admin_name', 'varchar(64) default ''''');
call add_column_if_missing('buyer_session', 'direct_login_reason', 'varchar(255) default ''''');

call assert_three_terminal_isolation_migration_completed();

-- Legacy sys_role seller/buyer cleanup is intentionally not part of the current
-- three-terminal isolation migration. Seller/buyer terminal roles are managed by
-- seller_role/buyer_role; any old mixed-account sys_role cleanup must run through
-- an explicit legacy helper after the target environment is confirmed.

drop procedure if exists add_column_if_missing;
drop procedure if exists drop_column_if_exists;
drop procedure if exists assert_three_terminal_isolation_preflight;
drop procedure if exists assert_existing_column_if_table_present;
drop procedure if exists assert_database_selected;
drop procedure if exists assert_no_legacy_account_user_bindings;
drop procedure if exists drop_index_if_exists;
drop procedure if exists add_index_if_missing;
drop procedure if exists recreate_index_if_mismatch;
drop procedure if exists assert_index_definition;
drop procedure if exists assert_terminal_menu_integrity_ready;
drop procedure if exists assert_no_duplicate_owner_account;
drop procedure if exists assert_no_duplicate_terminal_user_name;
drop procedure if exists modify_terminal_account_identity_columns_if_needed;
drop procedure if exists assert_owner_generated_column;
drop procedure if exists assert_no_blank_terminal_passwords;
drop procedure if exists assert_seller_account_normalize_targets;
drop procedure if exists assert_buyer_account_normalize_targets;
drop procedure if exists assert_terminal_account_user_id_drop_target;
drop procedure if exists assert_three_terminal_isolation_migration_completed;
drop procedure if exists assert_three_terminal_required_column;
