-- Seller/buyer management seed for the RuoYi validation project.
-- Scope: seller/buyer modules, portal account bindings, dictionaries, menu entries, and permissions.
-- Execution profiles:
--   FRESH_BOOTSTRAP: run after RuoYi bootstrap and top_menu_seed.sql on a new database.
--   PATCH_EXISTING: converge an existing seller/buyer environment without pretending it is fresh.
-- Set both @confirm_seller_buyer_management_seed and @seller_buyer_management_seed_profile before execution.

set names utf8mb4;

set @confirm_seller_buyer_management_seed := coalesce(@confirm_seller_buyer_management_seed, '');
set @seller_buyer_management_seed_profile := coalesce(@seller_buyer_management_seed_profile, '');

delimiter //

drop procedure if exists assert_seller_buyer_management_seed_confirmed//
create procedure assert_seller_buyer_management_seed_confirmed()
begin
  if coalesce(@confirm_seller_buyer_management_seed, '') <> 'APPLY_SELLER_BUYER_MANAGEMENT_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_seller_buyer_management_seed = APPLY_SELLER_BUYER_MANAGEMENT_SEED before running this seed';
  end if;
end//

drop procedure if exists assert_seller_buyer_management_seed_profile//
create procedure assert_seller_buyer_management_seed_profile()
begin
  declare v_existing_terminal_table_count int default 0;

  if database() is null then
    signal sqlstate '45000' set message_text = 'select target database before running seller/buyer management seed';
  end if;

  if coalesce(@seller_buyer_management_seed_profile, '')
      not in ('FRESH_BOOTSTRAP', 'PATCH_EXISTING') then
    signal sqlstate '45000' set message_text = 'set @seller_buyer_management_seed_profile to FRESH_BOOTSTRAP or PATCH_EXISTING before running this seed';
  end if;

  if not exists (
    select 1
    from information_schema.tables
    where table_schema = database()
      and table_name = 'sys_menu'
  ) then
    signal sqlstate '45000' set message_text = 'seller/buyer seed requires RuoYi sys_menu table';
  elseif not exists (
    select 1
    from sys_menu m
    where m.menu_id = 2010
      and coalesce(m.parent_id, -1) = 0
      and coalesce(m.menu_type, '') = 'M'
      and coalesce(m.path, '') = 'partner'
      and coalesce(m.component, '') = ''
      and coalesce(m.route_name, '') = 'PartnerManagement'
      and coalesce(m.perms, '') = ''
  ) then
    signal sqlstate '45000' set message_text = 'seller/buyer seed requires top_menu_seed partner root 2010 before DDL';
  end if;

  select count(1)
    into v_existing_terminal_table_count
  from information_schema.tables
  where table_schema = database()
    and table_name in (
      'seller',
      'buyer',
      'seller_account',
      'buyer_account',
      'seller_role',
      'buyer_role',
      'seller_menu',
      'buyer_menu',
      'seller_account_role',
      'buyer_account_role',
      'seller_role_menu',
      'buyer_role_menu'
    );

  if @seller_buyer_management_seed_profile = 'FRESH_BOOTSTRAP'
      and v_existing_terminal_table_count > 0 then
    signal sqlstate '45000' set message_text = 'seller/buyer seed FRESH_BOOTSTRAP profile requires no existing terminal tables; use PATCH_EXISTING';
  end if;

  if @seller_buyer_management_seed_profile = 'PATCH_EXISTING'
      and v_existing_terminal_table_count = 0 then
    signal sqlstate '45000' set message_text = 'seller/buyer seed PATCH_EXISTING profile requires existing terminal tables; use FRESH_BOOTSTRAP';
  end if;
end//

drop procedure if exists assert_seller_buyer_sys_menu_seed_guard//
create procedure assert_seller_buyer_sys_menu_seed_guard()
begin
  if not exists (
    select 1
    from sys_menu m
    where m.menu_id = 2010
      and coalesce(m.parent_id, -1) = 0
      and coalesce(m.menu_type, '') = 'M'
      and coalesce(m.path, '') = 'partner'
      and coalesce(m.component, '') = ''
      and coalesce(m.route_name, '') = 'PartnerManagement'
      and coalesce(m.perms, '') = ''
  ) then
    signal sqlstate '45000' set message_text = 'seller/buyer seed requires top_menu_seed partner root 2010';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_seller_buyer_sys_menu_guard seed on seed.menu_id = m.menu_id
    where coalesce(m.parent_id, -1) <> seed.parent_id
       or coalesce(m.menu_type, '') <> coalesce(seed.menu_type, '')
       or coalesce(m.path, '') <> coalesce(seed.path, '')
       or coalesce(m.component, '') <> coalesce(seed.component, '')
       or coalesce(m.route_name, '') <> coalesce(seed.route_name, '')
       or coalesce(m.perms, '') <> coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'seller/buyer sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_seller_buyer_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'seller/buyer sys_menu signature is already used by another menu';
  end if;
end//

drop procedure if exists assert_seller_menu_permission_slot//
create procedure assert_seller_menu_permission_slot(
  in p_perms varchar(100),
  in p_parent_id bigint,
  in p_menu_type char(1),
  in p_path varchar(200),
  in p_component varchar(255),
  in p_route_name varchar(50),
  in p_message varchar(128)
)
begin
  if exists (
    select 1
    from seller_menu
    where perms = p_perms
      and (
        coalesce(parent_id, -1) <> p_parent_id
        or coalesce(menu_type, '') <> coalesce(p_menu_type, '')
        or coalesce(path, '') <> coalesce(p_path, '')
        or coalesce(component, '') <> coalesce(p_component, '')
        or coalesce(route_name, '') <> coalesce(p_route_name, '')
      )
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_buyer_menu_permission_slot//
create procedure assert_buyer_menu_permission_slot(
  in p_perms varchar(100),
  in p_parent_id bigint,
  in p_menu_type char(1),
  in p_path varchar(200),
  in p_component varchar(255),
  in p_route_name varchar(50),
  in p_message varchar(128)
)
begin
  if exists (
    select 1
    from buyer_menu
    where perms = p_perms
      and (
        coalesce(parent_id, -1) <> p_parent_id
        or coalesce(menu_type, '') <> coalesce(p_menu_type, '')
        or coalesce(path, '') <> coalesce(p_path, '')
        or coalesce(component, '') <> coalesce(p_component, '')
        or coalesce(route_name, '') <> coalesce(p_route_name, '')
      )
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_terminal_menu_range_ready//
create procedure assert_terminal_menu_range_ready()
begin
  declare v_table_count int default 0;
  declare v_auto_increment bigint default 0;

  select count(1)
    into v_table_count
  from information_schema.tables
  where table_schema = database()
    and table_name = 'seller_menu';

  if v_table_count = 0 then
    signal sqlstate '45000' set message_text = 'seller_menu table is required before terminal menu seed inserts';
  end if;

  if exists (
    select 1
    from seller_menu
    where seller_menu_id > 0
      and (seller_menu_id < 100000 or seller_menu_id >= 200000)
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu contains IDs outside seller range 100000-199999';
  end if;

  select auto_increment
    into v_auto_increment
  from information_schema.tables
  where table_schema = database()
    and table_name = 'seller_menu';

  if coalesce(v_auto_increment, 0) < 100000
      or coalesce(v_auto_increment, 0) >= 200000 then
    signal sqlstate '45000' set message_text = 'seller_menu auto_increment must be between 100000 and 199999 before terminal menu seed inserts';
  end if;

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

  select count(1)
    into v_table_count
  from information_schema.tables
  where table_schema = database()
    and table_name = 'buyer_menu';

  if v_table_count = 0 then
    signal sqlstate '45000' set message_text = 'buyer_menu table is required before terminal menu seed inserts';
  end if;

  if exists (
    select 1
    from buyer_menu
    where buyer_menu_id > 0
      and (buyer_menu_id < 200000 or buyer_menu_id >= 300000)
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu contains IDs outside buyer range 200000-299999';
  end if;

  select auto_increment
    into v_auto_increment
  from information_schema.tables
  where table_schema = database()
    and table_name = 'buyer_menu';

  if coalesce(v_auto_increment, 0) < 200000
      or coalesce(v_auto_increment, 0) >= 300000 then
    signal sqlstate '45000' set message_text = 'buyer_menu auto_increment must be between 200000 and 299999 before terminal menu seed inserts';
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

  if exists (
    select 1
    from seller_role_menu rm
    left join seller_menu m on m.seller_menu_id = rm.seller_menu_id
    where m.seller_menu_id is null
       or m.seller_menu_id < 100000
       or m.seller_menu_id >= 200000
  ) then
    signal sqlstate '45000' set message_text = 'seller_role_menu has orphan or out-of-range seller_menu_id values';
  end if;

  if exists (
    select 1
    from buyer_role_menu rm
    left join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
    where m.buyer_menu_id is null
       or m.buyer_menu_id < 200000
       or m.buyer_menu_id >= 300000
  ) then
    signal sqlstate '45000' set message_text = 'buyer_role_menu has orphan or out-of-range buyer_menu_id values';
  end if;
end//

drop procedure if exists add_index_if_missing//
create procedure add_index_if_missing(in p_table varchar(64), in p_index varchar(64), in p_definition text)
begin
  if not exists (
    select 1
    from information_schema.statistics
    where table_schema = database() and table_name = p_table and index_name = p_index
  ) then
    set @ddl = concat('alter table `', p_table, '` add ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists add_column_if_missing//
create procedure add_column_if_missing(in p_table varchar(64), in p_column varchar(64), in p_definition text)
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database() and table_name = p_table and column_name = p_column
  ) then
    set @ddl = concat('alter table `', p_table, '` add column `', p_column, '` ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_no_blank_terminal_passwords//
create procedure assert_no_blank_terminal_passwords(
  in p_table varchar(64),
  in p_message varchar(128)
)
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table
      and column_name = 'password'
  ) then
    signal sqlstate '45000' set message_text = 'terminal account password column is required before password final structure check';
  end if;

  set @blank_terminal_password_count := 0;
  set @sql = concat(
    'select count(1) into @blank_terminal_password_count from `',
    p_table,
    '` where password is null or trim(password) = ',
    char(39),
    char(39)
  );
  prepare stmt from @sql;
  execute stmt;
  deallocate prepare stmt;

  if @blank_terminal_password_count > 0 then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists modify_terminal_account_password_column_if_needed//
create procedure modify_terminal_account_password_column_if_needed(in p_table varchar(64))
begin
  declare v_missing_count int default 0;
  declare v_mismatch_count int default 0;

  select count(1)
    into v_missing_count
  from information_schema.columns
  where table_schema = database()
    and table_name = p_table
    and column_name = 'password';

  if v_missing_count <> 1 then
    signal sqlstate '45000' set message_text = 'terminal account password column is required before password final structure check';
  end if;

  select count(1)
    into v_mismatch_count
  from information_schema.columns c
  where c.table_schema = database()
    and c.table_name = p_table
    and c.column_name = 'password'
    and (
      lower(c.data_type) <> 'varchar'
      or coalesce(c.character_maximum_length, -1) <> 100
      or c.is_nullable <> 'NO'
      or c.column_default is not null
    );

  if v_mismatch_count > 0 then
    set @ddl = concat('alter table `', p_table, '` modify password varchar(100) not null comment ''密码密文''');
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_terminal_account_password_column_final//
create procedure assert_terminal_account_password_column_final(in p_table varchar(64), in p_message varchar(128))
begin
  if not exists (
    select 1
    from information_schema.columns c
    where c.table_schema = database()
      and c.table_name = p_table
      and c.column_name = 'password'
      and lower(c.data_type) = 'varchar'
      and coalesce(c.character_maximum_length, -1) = 100
      and c.is_nullable = 'NO'
      and c.column_default is null
  ) then
    signal sqlstate '45000' set message_text = p_message;
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

drop procedure if exists assert_terminal_menu_perms_unique_index//
create procedure assert_terminal_menu_perms_unique_index(
  in p_table varchar(64),
  in p_index varchar(64),
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
      or v_actual_columns <> 'perms_unique_key'
      or v_actual_non_unique <> 0 then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_terminal_owner_role_slots_ready//
create procedure assert_terminal_owner_role_slots_ready()
begin
  if exists (
    select 1
    from seller_role r
    join seller s on s.seller_id = r.seller_id
                 and s.status = '0'
    where r.role_key = 'owner'
      and (
        coalesce(r.status, '') <> '0'
        or coalesce(r.del_flag, '') <> '0'
      )
  ) then
    signal sqlstate '45000' set message_text = 'seller owner role must be active before terminal owner grants';
  end if;

  if exists (
    select 1
    from seller_role r
    join seller s on s.seller_id = r.seller_id
                 and s.status = '0'
    where r.role_key = 'owner'
      and r.del_flag = '0'
      and (
        coalesce(r.role_name, '') <> 'Owner'
        or coalesce(r.role_sort, -1) <> 1
        or coalesce(r.status, '') <> '0'
        or coalesce(r.remark, '') <> '默认卖家端 Owner 角色'
      )
  ) then
    signal sqlstate '45000' set message_text = 'seller owner role signature mismatch before terminal owner grants';
  end if;

  if exists (
    select 1
    from buyer_role r
    join buyer b on b.buyer_id = r.buyer_id
                and b.status = '0'
    where r.role_key = 'owner'
      and (
        coalesce(r.status, '') <> '0'
        or coalesce(r.del_flag, '') <> '0'
      )
  ) then
    signal sqlstate '45000' set message_text = 'buyer owner role must be active before terminal owner grants';
  end if;

  if exists (
    select 1
    from buyer_role r
    join buyer b on b.buyer_id = r.buyer_id
                and b.status = '0'
    where r.role_key = 'owner'
      and r.del_flag = '0'
      and (
        coalesce(r.role_name, '') <> 'Owner'
        or coalesce(r.role_sort, -1) <> 1
        or coalesce(r.status, '') <> '0'
        or coalesce(r.remark, '') <> '默认买家端 Owner 角色'
      )
  ) then
    signal sqlstate '45000' set message_text = 'buyer owner role signature mismatch before terminal owner grants';
  end if;
end//

drop procedure if exists assert_seller_buyer_management_seed_completed//
create procedure assert_seller_buyer_management_seed_completed()
begin
  call assert_terminal_account_password_column_final('seller_account', 'seller_account.password final column must be varchar(100) not null without default');
  call assert_terminal_account_password_column_final('buyer_account', 'buyer_account.password final column must be varchar(100) not null without default');

  if exists (
    select 1
    from tmp_seller_buyer_sys_menu_guard seed
    left join sys_menu m on m.menu_id = seed.menu_id
    where m.menu_id is null
       or coalesce(m.parent_id, -1) <> seed.parent_id
       or coalesce(m.menu_type, '') <> coalesce(seed.menu_type, '')
       or coalesce(m.path, '') <> coalesce(seed.path, '')
       or coalesce(m.component, '') <> coalesce(seed.component, '')
       or coalesce(m.route_name, '') <> coalesce(seed.route_name, '')
       or coalesce(m.perms, '') <> coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'seller/buyer management sys_menu final signature mismatch';
  end if;

  if not exists (select 1 from sys_config where config_key = 'portal.seller.web.url')
      or not exists (select 1 from sys_config where config_key = 'portal.buyer.web.url') then
    signal sqlstate '45000' set message_text = 'seller/buyer management portal web url config is incomplete';
  end if;

  if exists (
    select 1
    from (
      select 'seller:account:list' as perms
      union all select 'seller:account:add'
      union all select 'seller:account:edit'
      union all select 'seller:account:role:query'
      union all select 'seller:account:role:edit'
      union all select 'seller:account:loginLog:list'
      union all select 'seller:account:operLog:list'
      union all select 'seller:account:session:list'
      union all select 'seller:dept:list'
      union all select 'seller:dept:query'
      union all select 'seller:dept:add'
      union all select 'seller:dept:edit'
      union all select 'seller:dept:remove'
      union all select 'seller:role:list'
      union all select 'seller:role:query'
      union all select 'seller:role:add'
      union all select 'seller:role:edit'
      union all select 'seller:role:remove'
      union all select 'seller:product:category:list'
      union all select 'seller:product:schema:query'
      union all select 'seller:product:distribution:list'
      union all select 'seller:product:distribution:query'
    ) expected
    where not exists (
      select 1
      from seller_menu m
      where m.perms = expected.perms
        and m.parent_id = 0
        and coalesce(m.menu_type, '') = 'F'
        and coalesce(m.path, '') = ''
        and coalesce(m.component, '') = ''
        and coalesce(m.route_name, '') = ''
    )
  ) then
    signal sqlstate '45000' set message_text = 'seller/buyer management seller terminal permission final state mismatch';
  end if;

  if exists (
    select 1
    from (
      select 'buyer:account:list' as perms
      union all select 'buyer:account:add'
      union all select 'buyer:account:edit'
      union all select 'buyer:account:role:query'
      union all select 'buyer:account:role:edit'
      union all select 'buyer:account:loginLog:list'
      union all select 'buyer:account:operLog:list'
      union all select 'buyer:account:session:list'
      union all select 'buyer:dept:list'
      union all select 'buyer:dept:query'
      union all select 'buyer:dept:add'
      union all select 'buyer:dept:edit'
      union all select 'buyer:dept:remove'
      union all select 'buyer:role:list'
      union all select 'buyer:role:query'
      union all select 'buyer:role:add'
      union all select 'buyer:role:edit'
      union all select 'buyer:role:remove'
      union all select 'buyer:product:category:list'
      union all select 'buyer:product:schema:query'
      union all select 'buyer:product:distribution:list'
      union all select 'buyer:product:distribution:query'
    ) expected
    where not exists (
      select 1
      from buyer_menu m
      where m.perms = expected.perms
        and m.parent_id = 0
        and coalesce(m.menu_type, '') = 'F'
        and coalesce(m.path, '') = ''
        and coalesce(m.component, '') = ''
        and coalesce(m.route_name, '') = ''
    )
  ) then
    signal sqlstate '45000' set message_text = 'seller/buyer management buyer terminal permission final state mismatch';
  end if;

  if exists (
    select 1
    from seller_role_menu rm
    left join seller_menu m on m.seller_menu_id = rm.seller_menu_id
    where m.seller_menu_id is null
       or m.seller_menu_id < 100000
       or m.seller_menu_id >= 200000
  ) then
    signal sqlstate '45000' set message_text = 'seller_role_menu final state has orphan or out-of-range seller_menu_id values';
  end if;

  if exists (
    select 1
    from buyer_role_menu rm
    left join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
    where m.buyer_menu_id is null
       or m.buyer_menu_id < 200000
       or m.buyer_menu_id >= 300000
  ) then
    signal sqlstate '45000' set message_text = 'buyer_role_menu final state has orphan or out-of-range buyer_menu_id values';
  end if;

  if exists (
    select 1
    from seller_account_role ar
    left join seller_account a on a.seller_account_id = ar.seller_account_id
    left join seller_role r on r.seller_role_id = ar.seller_role_id
    where a.seller_account_id is null
       or r.seller_role_id is null
       or a.seller_id <> r.seller_id
  ) then
    signal sqlstate '45000' set message_text = 'seller_account_role final state has orphan or cross-subject role bindings';
  end if;

  if exists (
    select 1
    from buyer_account_role ar
    left join buyer_account a on a.buyer_account_id = ar.buyer_account_id
    left join buyer_role r on r.buyer_role_id = ar.buyer_role_id
    where a.buyer_account_id is null
       or r.buyer_role_id is null
       or a.buyer_id <> r.buyer_id
  ) then
    signal sqlstate '45000' set message_text = 'buyer_account_role final state has orphan or cross-subject role bindings';
  end if;

  if exists (
    select 1
    from seller_account a
    join seller s on s.seller_id = a.seller_id
                 and s.status = '0'
    join seller_role r on r.seller_id = a.seller_id
                      and r.role_key = 'owner'
                      and r.status = '0'
                      and r.del_flag = '0'
    left join seller_account_role ar on ar.seller_account_id = a.seller_account_id
                                    and ar.seller_role_id = r.seller_role_id
    where a.account_role = 'OWNER'
      and ar.seller_account_id is null
  ) then
    signal sqlstate '45000' set message_text = 'seller owner account role binding final state mismatch';
  end if;

  if exists (
    select 1
    from buyer_account a
    join buyer b on b.buyer_id = a.buyer_id
                and b.status = '0'
    join buyer_role r on r.buyer_id = a.buyer_id
                     and r.role_key = 'owner'
                     and r.status = '0'
                     and r.del_flag = '0'
    left join buyer_account_role ar on ar.buyer_account_id = a.buyer_account_id
                                   and ar.buyer_role_id = r.buyer_role_id
    where a.account_role = 'OWNER'
      and ar.buyer_account_id is null
  ) then
    signal sqlstate '45000' set message_text = 'buyer owner account role binding final state mismatch';
  end if;

  if exists (
    select 1
    from seller_role r
    join seller s on s.seller_id = r.seller_id
                 and s.status = '0'
    join (
      select 'seller:account:list' as perms
      union all select 'seller:account:add'
      union all select 'seller:account:edit'
      union all select 'seller:account:role:query'
      union all select 'seller:account:role:edit'
      union all select 'seller:account:loginLog:list'
      union all select 'seller:account:operLog:list'
      union all select 'seller:account:session:list'
      union all select 'seller:dept:list'
      union all select 'seller:dept:query'
      union all select 'seller:dept:add'
      union all select 'seller:dept:edit'
      union all select 'seller:dept:remove'
      union all select 'seller:role:list'
      union all select 'seller:role:query'
      union all select 'seller:role:add'
      union all select 'seller:role:edit'
      union all select 'seller:role:remove'
    ) expected
    join seller_menu m on m.perms = expected.perms
                      and m.parent_id = 0
                      and coalesce(m.menu_type, '') = 'F'
                      and coalesce(m.path, '') = ''
                      and coalesce(m.component, '') = ''
                      and coalesce(m.route_name, '') = ''
    left join seller_role_menu rm on rm.seller_role_id = r.seller_role_id
                                 and rm.seller_menu_id = m.seller_menu_id
    where r.role_key = 'owner'
      and r.status = '0'
      and r.del_flag = '0'
      and rm.seller_role_id is null
  ) then
    signal sqlstate '45000' set message_text = 'seller owner role terminal permission grants final state mismatch';
  end if;

  if exists (
    select 1
    from seller_role r
    join seller s on s.seller_id = r.seller_id
                 and s.status = '0'
    join seller_role_menu rm on rm.seller_role_id = r.seller_role_id
    join seller_menu m on m.seller_menu_id = rm.seller_menu_id
    left join (
      select 'seller:account:list' as perms
      union all select 'seller:account:add'
      union all select 'seller:account:edit'
      union all select 'seller:account:role:query'
      union all select 'seller:account:role:edit'
      union all select 'seller:account:loginLog:list'
      union all select 'seller:account:operLog:list'
      union all select 'seller:account:session:list'
      union all select 'seller:dept:list'
      union all select 'seller:dept:query'
      union all select 'seller:dept:add'
      union all select 'seller:dept:edit'
      union all select 'seller:dept:remove'
      union all select 'seller:role:list'
      union all select 'seller:role:query'
      union all select 'seller:role:add'
      union all select 'seller:role:edit'
      union all select 'seller:role:remove'
    ) expected on expected.perms = m.perms
    where r.role_key = 'owner'
      and r.status = '0'
      and r.del_flag = '0'
      and (
        m.perms like 'seller:account:%'
        or m.perms like 'seller:dept:%'
        or m.perms like 'seller:role:%'
      )
      and expected.perms is null
  ) then
    signal sqlstate '45000' set message_text = 'seller owner role terminal permission grants final state has unexpected permissions';
  end if;

  if exists (
    select 1
    from buyer_role r
    join buyer b on b.buyer_id = r.buyer_id
                and b.status = '0'
    join (
      select 'buyer:account:list' as perms
      union all select 'buyer:account:add'
      union all select 'buyer:account:edit'
      union all select 'buyer:account:role:query'
      union all select 'buyer:account:role:edit'
      union all select 'buyer:account:loginLog:list'
      union all select 'buyer:account:operLog:list'
      union all select 'buyer:account:session:list'
      union all select 'buyer:dept:list'
      union all select 'buyer:dept:query'
      union all select 'buyer:dept:add'
      union all select 'buyer:dept:edit'
      union all select 'buyer:dept:remove'
      union all select 'buyer:role:list'
      union all select 'buyer:role:query'
      union all select 'buyer:role:add'
      union all select 'buyer:role:edit'
      union all select 'buyer:role:remove'
    ) expected
    join buyer_menu m on m.perms = expected.perms
                     and m.parent_id = 0
                     and coalesce(m.menu_type, '') = 'F'
                     and coalesce(m.path, '') = ''
                     and coalesce(m.component, '') = ''
                     and coalesce(m.route_name, '') = ''
    left join buyer_role_menu rm on rm.buyer_role_id = r.buyer_role_id
                                and rm.buyer_menu_id = m.buyer_menu_id
    where r.role_key = 'owner'
      and r.status = '0'
      and r.del_flag = '0'
      and rm.buyer_role_id is null
  ) then
    signal sqlstate '45000' set message_text = 'buyer owner role terminal permission grants final state mismatch';
  end if;

  if exists (
    select 1
    from buyer_role r
    join buyer b on b.buyer_id = r.buyer_id
                and b.status = '0'
    join buyer_role_menu rm on rm.buyer_role_id = r.buyer_role_id
    join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
    left join (
      select 'buyer:account:list' as perms
      union all select 'buyer:account:add'
      union all select 'buyer:account:edit'
      union all select 'buyer:account:role:query'
      union all select 'buyer:account:role:edit'
      union all select 'buyer:account:loginLog:list'
      union all select 'buyer:account:operLog:list'
      union all select 'buyer:account:session:list'
      union all select 'buyer:dept:list'
      union all select 'buyer:dept:query'
      union all select 'buyer:dept:add'
      union all select 'buyer:dept:edit'
      union all select 'buyer:dept:remove'
      union all select 'buyer:role:list'
      union all select 'buyer:role:query'
      union all select 'buyer:role:add'
      union all select 'buyer:role:edit'
      union all select 'buyer:role:remove'
    ) expected on expected.perms = m.perms
    where r.role_key = 'owner'
      and r.status = '0'
      and r.del_flag = '0'
      and (
        m.perms like 'buyer:account:%'
        or m.perms like 'buyer:dept:%'
        or m.perms like 'buyer:role:%'
      )
      and expected.perms is null
  ) then
    signal sqlstate '45000' set message_text = 'buyer owner role terminal permission grants final state has unexpected permissions';
  end if;
end//

delimiter ;

call assert_seller_buyer_management_seed_confirmed();
call assert_seller_buyer_management_seed_profile();

create table if not exists seller (
  seller_id             bigint(20)      not null auto_increment    comment '卖家ID',
  seller_no             varchar(64)     not null                   comment '系统内部卖家编号',
  seller_code           varchar(64)     not null                   comment '对外卖家代码',
  seller_name           varchar(200)    not null                   comment '卖家全称',
  seller_short_name     varchar(100)    not null default ''        comment '卖家简称',
  seller_type           varchar(32)     not null default 'COMPANY' comment '主体类型',
  seller_level          varchar(32)     not null default 'L1'      comment '卖家等级',
  status                char(1)         not null default '0'       comment '状态：0正常 1停用',
  legal_id              varchar(100)    default ''                 comment '法人证件号',
  business_license_no   varchar(100)    default ''                 comment '营业执照号码',
  country_code          varchar(32)     not null                   comment '国家/地区代码',
  state_province        varchar(100)    not null default ''        comment '省/州',
  city                  varchar(100)    not null                   comment '城市',
  postal_code           varchar(32)     not null                   comment '邮编',
  address_line1         varchar(255)    not null                   comment '地址1',
  address_line2         varchar(255)    default ''                 comment '地址2',
  contact_name          varchar(100)    not null default ''        comment '联系人',
  contact_phone         varchar(64)     not null default ''        comment '手机号',
  contact_email         varchar(128)    default ''                 comment '邮箱',
  attachment_file_name  varchar(255)    default ''                 comment '附件文件名',
  attachment_mime_type  varchar(100)    default ''                 comment '附件类型',
  attachment_size_bytes bigint          default null               comment '附件大小',
  attachment_file_url   longtext                                    comment '附件文件地址',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (seller_id),
  unique key uk_seller_no (seller_no),
  unique key uk_seller_code (seller_code),
  key idx_seller_status (status),
  key idx_seller_name (seller_name),
  key idx_seller_short_name (seller_short_name),
  key idx_seller_level (seller_level)
) engine=innodb auto_increment=1 comment = '卖家主体表';

create table if not exists buyer (
  buyer_id              bigint(20)      not null auto_increment    comment '买家ID',
  buyer_no              varchar(64)     not null                   comment '系统内部买家编号',
  buyer_code            varchar(64)     not null                   comment '对外买家代码',
  buyer_name            varchar(200)    not null                   comment '买家全称',
  buyer_short_name      varchar(100)    not null default ''        comment '买家简称',
  buyer_type            varchar(32)     not null default 'COMPANY' comment '主体类型',
  buyer_level           varchar(32)     not null default 'L1'      comment '买家等级',
  status                char(1)         not null default '0'       comment '状态：0正常 1停用',
  legal_id              varchar(100)    default ''                 comment '法人证件号',
  business_license_no   varchar(100)    default ''                 comment '营业执照号码',
  country_code          varchar(32)     not null                   comment '国家/地区代码',
  state_province        varchar(100)    not null default ''        comment '省/州',
  city                  varchar(100)    not null                   comment '城市',
  postal_code           varchar(32)     not null                   comment '邮编',
  address_line1         varchar(255)    not null                   comment '地址1',
  address_line2         varchar(255)    default ''                 comment '地址2',
  contact_name          varchar(100)    not null default ''        comment '联系人',
  contact_phone         varchar(64)     not null default ''        comment '手机号',
  contact_email         varchar(128)    default ''                 comment '邮箱',
  attachment_file_name  varchar(255)    default ''                 comment '附件文件名',
  attachment_mime_type  varchar(100)    default ''                 comment '附件类型',
  attachment_size_bytes bigint          default null               comment '附件大小',
  attachment_file_url   longtext                                    comment '附件文件地址',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (buyer_id),
  unique key uk_buyer_no (buyer_no),
  unique key uk_buyer_code (buyer_code),
  key idx_buyer_status (status),
  key idx_buyer_name (buyer_name),
  key idx_buyer_short_name (buyer_short_name),
  key idx_buyer_level (buyer_level)
) engine=innodb auto_increment=1 comment = '买家主体表';

create table if not exists seller_account (
  seller_account_id     bigint(20)      not null auto_increment    comment '卖家端账号ID',
  seller_id             bigint(20)      not null                   comment '卖家ID',
  dept_id               bigint(20)      default null               comment '卖家端部门ID',
  user_name             varchar(30)     not null                   comment '登录账号',
  nick_name             varchar(30)     not null default ''        comment '姓名',
  password              varchar(100)    not null                   comment '密码密文',
  email                 varchar(50)     default ''                 comment '邮箱',
  phonenumber           varchar(32)     default ''                 comment '手机',
  account_role          varchar(32)     not null default 'OWNER'   comment '卖家侧账号角色',
  status                char(1)         not null default '0'       comment '账号状态：0正常 1停用',
  lock_status           char(1)         not null default '0'       comment '锁定状态：0未锁定 1已锁定',
  lock_reason           varchar(500)    not null default ''        comment '锁定原因',
  last_login_ip         varchar(128)    default ''                 comment '最后登录IP',
  last_login_time       datetime                                   comment '最后登录时间',
  pwd_update_time       datetime                                   comment '密码最后更新时间',
  owner_unique_seller_id bigint(20) generated always as (case when account_role = 'OWNER' then seller_id else null end) stored comment 'OWNER唯一约束辅助列',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (seller_account_id),
  unique key uk_seller_account_username (user_name),
  unique key uk_seller_account_owner (owner_unique_seller_id),
  key idx_seller_account_seller_status (seller_id, status),
  key idx_seller_account_seller_lock (seller_id, lock_status)
) engine=innodb auto_increment=1 comment = '卖家端账号表';

create table if not exists buyer_account (
  buyer_account_id      bigint(20)      not null auto_increment    comment '买家端账号ID',
  buyer_id              bigint(20)      not null                   comment '买家ID',
  dept_id               bigint(20)      default null               comment '买家端部门ID',
  user_name             varchar(30)     not null                   comment '登录账号',
  nick_name             varchar(30)     not null default ''        comment '姓名',
  password              varchar(100)    not null                   comment '密码密文',
  email                 varchar(50)     default ''                 comment '邮箱',
  phonenumber           varchar(32)     default ''                 comment '手机',
  account_role          varchar(32)     not null default 'OWNER'   comment '买家侧账号角色',
  status                char(1)         not null default '0'       comment '账号状态：0正常 1停用',
  lock_status           char(1)         not null default '0'       comment '锁定状态：0未锁定 1已锁定',
  lock_reason           varchar(500)    not null default ''        comment '锁定原因',
  last_login_ip         varchar(128)    default ''                 comment '最后登录IP',
  last_login_time       datetime                                   comment '最后登录时间',
  pwd_update_time       datetime                                   comment '密码最后更新时间',
  owner_unique_buyer_id bigint(20) generated always as (case when account_role = 'OWNER' then buyer_id else null end) stored comment 'OWNER唯一约束辅助列',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (buyer_account_id),
  unique key uk_buyer_account_username (user_name),
  unique key uk_buyer_account_owner (owner_unique_buyer_id),
  key idx_buyer_account_buyer_status (buyer_id, status),
  key idx_buyer_account_buyer_lock (buyer_id, lock_status)
) engine=innodb auto_increment=1 comment = '买家端账号表';

call assert_no_blank_terminal_passwords('seller_account', 'seller_account contains blank passwords; reset or backfill before seller/buyer management seed');
call assert_no_blank_terminal_passwords('buyer_account', 'buyer_account contains blank passwords; reset or backfill before seller/buyer management seed');
call modify_terminal_account_password_column_if_needed('seller_account');
call modify_terminal_account_password_column_if_needed('buyer_account');
call assert_terminal_account_password_column_final('seller_account', 'seller_account.password final column must be varchar(100) not null without default');
call assert_terminal_account_password_column_final('buyer_account', 'buyer_account.password final column must be varchar(100) not null without default');

create table if not exists seller_dept (
  seller_dept_id        bigint(20)      not null auto_increment    comment '卖家端部门ID',
  seller_id             bigint(20)      not null                   comment '卖家ID',
  parent_id             bigint(20)      default 0                  comment '父部门ID',
  ancestors             varchar(500)    default ''                 comment '祖级列表',
  dept_name             varchar(100)    not null                   comment '部门名称',
  order_num             int             default 0                  comment '显示顺序',
  leader                varchar(100)    default ''                 comment '负责人',
  phone                 varchar(32)     default ''                 comment '联系电话',
  email                 varchar(128)    default ''                 comment '邮箱',
  status                char(1)         not null default '0'       comment '部门状态：0正常 1停用',
  del_flag              char(1)         not null default '0'       comment '删除标志：0存在 2删除',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  primary key (seller_dept_id),
  key idx_seller_dept_seller_parent (seller_id, parent_id),
  key idx_seller_dept_status (seller_id, status)
) engine=innodb auto_increment=1 comment = '卖家端部门表';

create table if not exists buyer_dept (
  buyer_dept_id         bigint(20)      not null auto_increment    comment '买家端部门ID',
  buyer_id              bigint(20)      not null                   comment '买家ID',
  parent_id             bigint(20)      default 0                  comment '父部门ID',
  ancestors             varchar(500)    default ''                 comment '祖级列表',
  dept_name             varchar(100)    not null                   comment '部门名称',
  order_num             int             default 0                  comment '显示顺序',
  leader                varchar(100)    default ''                 comment '负责人',
  phone                 varchar(32)     default ''                 comment '联系电话',
  email                 varchar(128)    default ''                 comment '邮箱',
  status                char(1)         not null default '0'       comment '部门状态：0正常 1停用',
  del_flag              char(1)         not null default '0'       comment '删除标志：0存在 2删除',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  primary key (buyer_dept_id),
  key idx_buyer_dept_buyer_parent (buyer_id, parent_id),
  key idx_buyer_dept_status (buyer_id, status)
) engine=innodb auto_increment=1 comment = '买家端部门表';

create table if not exists seller_role (
  seller_role_id        bigint(20)      not null auto_increment    comment '卖家端角色ID',
  seller_id             bigint(20)      not null                   comment '卖家ID',
  role_name             varchar(64)     not null                   comment '角色名称',
  role_key              varchar(64)     not null                   comment '权限字符',
  role_sort             int             not null default 0         comment '显示顺序',
  status                char(1)         not null default '0'       comment '角色状态：0正常 1停用',
  del_flag              char(1)         not null default '0'       comment '删除标志：0存在 2删除',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (seller_role_id),
  unique key uk_seller_role_key (seller_id, role_key),
  key idx_seller_role_status (seller_id, status)
) engine=innodb auto_increment=1 comment = '卖家端角色表';

create table if not exists buyer_role (
  buyer_role_id         bigint(20)      not null auto_increment    comment '买家端角色ID',
  buyer_id              bigint(20)      not null                   comment '买家ID',
  role_name             varchar(64)     not null                   comment '角色名称',
  role_key              varchar(64)     not null                   comment '权限字符',
  role_sort             int             not null default 0         comment '显示顺序',
  status                char(1)         not null default '0'       comment '角色状态：0正常 1停用',
  del_flag              char(1)         not null default '0'       comment '删除标志：0存在 2删除',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (buyer_role_id),
  unique key uk_buyer_role_key (buyer_id, role_key),
  key idx_buyer_role_status (buyer_id, status)
) engine=innodb auto_increment=1 comment = '买家端角色表';

create table if not exists seller_menu (
  seller_menu_id        bigint(20)      not null auto_increment    comment '卖家端菜单ID',
  menu_name             varchar(64)     not null                   comment '菜单名称',
  parent_id             bigint(20)      default 0                  comment '父菜单ID',
  order_num             int             default 0                  comment '显示顺序',
  path                  varchar(200)    default ''                 comment '路由地址',
  component             varchar(255)    default null               comment '组件路径',
  query                 varchar(255)    default ''                 comment '路由参数',
  route_name            varchar(64)     default ''                 comment '路由名称',
  is_frame              int             default 1                  comment '是否外链',
  is_cache              int             default 0                  comment '是否缓存',
  menu_type             char(1)         not null default 'M'       comment '菜单类型',
  visible               char(1)         not null default '0'       comment '显示状态',
  status                char(1)         not null default '0'       comment '菜单状态',
  perms                 varchar(100)    default ''                 comment '权限标识',
  perms_unique_key      varchar(100)    generated always as (case when trim(coalesce(perms, '')) = '' then null else trim(perms) end) stored comment '非空权限唯一约束辅助列',
  icon                  varchar(100)    default '#'                comment '菜单图标',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (seller_menu_id),
  unique key uk_seller_menu_perms (perms_unique_key),
  key idx_seller_menu_perms_lookup (perms),
  key idx_seller_menu_parent (parent_id),
  key idx_seller_menu_status (status)
) engine=innodb auto_increment=100000 comment = '卖家端菜单权限表';

create table if not exists buyer_menu (
  buyer_menu_id         bigint(20)      not null auto_increment    comment '买家端菜单ID',
  menu_name             varchar(64)     not null                   comment '菜单名称',
  parent_id             bigint(20)      default 0                  comment '父菜单ID',
  order_num             int             default 0                  comment '显示顺序',
  path                  varchar(200)    default ''                 comment '路由地址',
  component             varchar(255)    default null               comment '组件路径',
  query                 varchar(255)    default ''                 comment '路由参数',
  route_name            varchar(64)     default ''                 comment '路由名称',
  is_frame              int             default 1                  comment '是否外链',
  is_cache              int             default 0                  comment '是否缓存',
  menu_type             char(1)         not null default 'M'       comment '菜单类型',
  visible               char(1)         not null default '0'       comment '显示状态',
  status                char(1)         not null default '0'       comment '菜单状态',
  perms                 varchar(100)    default ''                 comment '权限标识',
  perms_unique_key      varchar(100)    generated always as (case when trim(coalesce(perms, '')) = '' then null else trim(perms) end) stored comment '非空权限唯一约束辅助列',
  icon                  varchar(100)    default '#'                comment '菜单图标',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (buyer_menu_id),
  unique key uk_buyer_menu_perms (perms_unique_key),
  key idx_buyer_menu_perms_lookup (perms),
  key idx_buyer_menu_parent (parent_id),
  key idx_buyer_menu_status (status)
) engine=innodb auto_increment=200000 comment = '买家端菜单权限表';

create table if not exists seller_account_role (
  seller_account_id     bigint(20)      not null                   comment '卖家端账号ID',
  seller_role_id        bigint(20)      not null                   comment '卖家端角色ID',
  primary key (seller_account_id, seller_role_id)
) engine=innodb comment = '卖家端账号角色关联表';

create table if not exists buyer_account_role (
  buyer_account_id      bigint(20)      not null                   comment '买家端账号ID',
  buyer_role_id         bigint(20)      not null                   comment '买家端角色ID',
  primary key (buyer_account_id, buyer_role_id)
) engine=innodb comment = '买家端账号角色关联表';

create table if not exists seller_role_menu (
  seller_role_id        bigint(20)      not null                   comment '卖家端角色ID',
  seller_menu_id        bigint(20)      not null                   comment '卖家端菜单ID',
  primary key (seller_role_id, seller_menu_id)
) engine=innodb comment = '卖家端角色菜单关联表';

create table if not exists buyer_role_menu (
  buyer_role_id         bigint(20)      not null                   comment '买家端角色ID',
  buyer_menu_id         bigint(20)      not null                   comment '买家端菜单ID',
  primary key (buyer_role_id, buyer_menu_id)
) engine=innodb comment = '买家端角色菜单关联表';

create table if not exists seller_login_log (
  info_id               bigint(20)      not null auto_increment    comment '访问ID',
  seller_id             bigint(20)      default null               comment '卖家ID',
  seller_account_id     bigint(20)      default null               comment '卖家端账号ID',
  user_name             varchar(30)     default ''                 comment '登录账号',
  ipaddr                varchar(128)    default ''                 comment '登录IP',
  login_location        varchar(255)    default ''                 comment '登录地点',
  browser               varchar(50)     default ''                 comment '浏览器',
  os                    varchar(50)     default ''                 comment '操作系统',
  status                char(1)         default '0'                comment '登录状态：0成功 1失败',
  msg                   varchar(255)    default ''                 comment '提示消息',
  direct_login          tinyint(1)      not null default 0         comment '是否免密代入',
  direct_login_ticket_id bigint(20)     default null               comment '免密代入票据ID',
  acting_admin_id       bigint(20)      default null               comment '代入管理员ID',
  acting_admin_name     varchar(64)     default ''                 comment '代入管理员账号',
  direct_login_reason   varchar(255)    default ''                 comment '免密代入原因',
  login_time            datetime                                   comment '访问时间',
  primary key (info_id),
  key idx_seller_login_log_account_time (seller_account_id, login_time),
  key idx_seller_login_log_seller_time (seller_id, login_time)
) engine=innodb auto_increment=1 comment = '卖家端登录日志表';

create table if not exists buyer_login_log (
  info_id               bigint(20)      not null auto_increment    comment '访问ID',
  buyer_id              bigint(20)      default null               comment '买家ID',
  buyer_account_id      bigint(20)      default null               comment '买家端账号ID',
  user_name             varchar(30)     default ''                 comment '登录账号',
  ipaddr                varchar(128)    default ''                 comment '登录IP',
  login_location        varchar(255)    default ''                 comment '登录地点',
  browser               varchar(50)     default ''                 comment '浏览器',
  os                    varchar(50)     default ''                 comment '操作系统',
  status                char(1)         default '0'                comment '登录状态：0成功 1失败',
  msg                   varchar(255)    default ''                 comment '提示消息',
  direct_login          tinyint(1)      not null default 0         comment '是否免密代入',
  direct_login_ticket_id bigint(20)     default null               comment '免密代入票据ID',
  acting_admin_id       bigint(20)      default null               comment '代入管理员ID',
  acting_admin_name     varchar(64)     default ''                 comment '代入管理员账号',
  direct_login_reason   varchar(255)    default ''                 comment '免密代入原因',
  login_time            datetime                                   comment '访问时间',
  primary key (info_id),
  key idx_buyer_login_log_account_time (buyer_account_id, login_time),
  key idx_buyer_login_log_buyer_time (buyer_id, login_time)
) engine=innodb auto_increment=1 comment = '买家端登录日志表';

create table if not exists seller_oper_log (
  oper_id               bigint(20)      not null auto_increment    comment '日志主键',
  seller_id             bigint(20)      default null               comment '卖家ID',
  seller_account_id     bigint(20)      default null               comment '卖家端账号ID',
  title                 varchar(50)     default ''                 comment '模块标题',
  business_type         int             default 0                  comment '业务类型',
  method                varchar(200)    default ''                 comment '方法名称',
  request_method        varchar(10)     default ''                 comment '请求方式',
  oper_name             varchar(30)     default ''                 comment '操作人员',
  oper_url              varchar(255)    default ''                 comment '请求URL',
  oper_ip               varchar(128)    default ''                 comment '主机地址',
  oper_location         varchar(255)    default ''                 comment '操作地点',
  oper_param            varchar(2000)   default ''                 comment '请求参数',
  json_result           varchar(2000)   default ''                 comment '返回参数',
  direct_login          tinyint(1)      not null default 0         comment '是否免密代入',
  direct_login_ticket_id bigint(20)     default null               comment '免密代入票据ID',
  acting_admin_id       bigint(20)      default null               comment '代入管理员ID',
  acting_admin_name     varchar(64)     default ''                 comment '代入管理员账号',
  direct_login_reason   varchar(255)    default ''                 comment '免密代入原因',
  status                int             default 0                  comment '操作状态',
  error_msg             varchar(2000)   default ''                 comment '错误消息',
  oper_time             datetime                                   comment '操作时间',
  cost_time             bigint(20)      default 0                  comment '消耗时间',
  primary key (oper_id),
  key idx_seller_oper_log_account_time (seller_account_id, oper_time),
  key idx_seller_oper_log_seller_time (seller_id, oper_time)
) engine=innodb auto_increment=1 comment = '卖家端操作日志表';

create table if not exists buyer_oper_log (
  oper_id               bigint(20)      not null auto_increment    comment '日志主键',
  buyer_id              bigint(20)      default null               comment '买家ID',
  buyer_account_id      bigint(20)      default null               comment '买家端账号ID',
  title                 varchar(50)     default ''                 comment '模块标题',
  business_type         int             default 0                  comment '业务类型',
  method                varchar(200)    default ''                 comment '方法名称',
  request_method        varchar(10)     default ''                 comment '请求方式',
  oper_name             varchar(30)     default ''                 comment '操作人员',
  oper_url              varchar(255)    default ''                 comment '请求URL',
  oper_ip               varchar(128)    default ''                 comment '主机地址',
  oper_location         varchar(255)    default ''                 comment '操作地点',
  oper_param            varchar(2000)   default ''                 comment '请求参数',
  json_result           varchar(2000)   default ''                 comment '返回参数',
  direct_login          tinyint(1)      not null default 0         comment '是否免密代入',
  direct_login_ticket_id bigint(20)     default null               comment '免密代入票据ID',
  acting_admin_id       bigint(20)      default null               comment '代入管理员ID',
  acting_admin_name     varchar(64)     default ''                 comment '代入管理员账号',
  direct_login_reason   varchar(255)    default ''                 comment '免密代入原因',
  status                int             default 0                  comment '操作状态',
  error_msg             varchar(2000)   default ''                 comment '错误消息',
  oper_time             datetime                                   comment '操作时间',
  cost_time             bigint(20)      default 0                  comment '消耗时间',
  primary key (oper_id),
  key idx_buyer_oper_log_account_time (buyer_account_id, oper_time),
  key idx_buyer_oper_log_buyer_time (buyer_id, oper_time)
) engine=innodb auto_increment=1 comment = '买家端操作日志表';

create table if not exists seller_session (
  token_id              varchar(64)     not null                   comment '会话ID',
  seller_id             bigint(20)      not null                   comment '卖家ID',
  seller_account_id     bigint(20)      not null                   comment '卖家端账号ID',
  user_name             varchar(30)     not null                   comment '登录账号',
  login_ip              varchar(128)    default ''                 comment '登录IP',
  login_time            datetime                                   comment '登录时间',
  expire_time           datetime                                   comment '过期时间',
  logout_time           datetime                                   comment '登出时间',
  status                char(1)         not null default '0'       comment '状态：0有效 1失效',
  direct_login          tinyint(1)      not null default 0         comment '是否免密代入',
  direct_login_ticket_id bigint(20)     default null               comment '免密代入票据ID',
  acting_admin_id       bigint(20)      default null               comment '代入管理员ID',
  acting_admin_name     varchar(64)     default ''                 comment '代入管理员账号',
  direct_login_reason   varchar(255)    default ''                 comment '免密代入原因',
  primary key (token_id),
  key idx_seller_session_account (seller_account_id),
  key idx_seller_session_expire (expire_time)
) engine=innodb comment = '卖家端会话表';

create table if not exists buyer_session (
  token_id              varchar(64)     not null                   comment '会话ID',
  buyer_id              bigint(20)      not null                   comment '买家ID',
  buyer_account_id      bigint(20)      not null                   comment '买家端账号ID',
  user_name             varchar(30)     not null                   comment '登录账号',
  login_ip              varchar(128)    default ''                 comment '登录IP',
  login_time            datetime                                   comment '登录时间',
  expire_time           datetime                                   comment '过期时间',
  logout_time           datetime                                   comment '登出时间',
  status                char(1)         not null default '0'       comment '状态：0有效 1失效',
  direct_login          tinyint(1)      not null default 0         comment '是否免密代入',
  direct_login_ticket_id bigint(20)     default null               comment '免密代入票据ID',
  acting_admin_id       bigint(20)      default null               comment '代入管理员ID',
  acting_admin_name     varchar(64)     default ''                 comment '代入管理员账号',
  direct_login_reason   varchar(255)    default ''                 comment '免密代入原因',
  primary key (token_id),
  key idx_buyer_session_account (buyer_account_id),
  key idx_buyer_session_expire (expire_time)
) engine=innodb comment = '买家端会话表';

create table if not exists portal_direct_login_ticket (
  ticket_id             bigint(20)      not null auto_increment    comment '免密代入票据ID',
  terminal              varchar(20)     not null                   comment '目标端：seller/buyer',
  target_subject_id     bigint(20)      not null                   comment '目标主体ID',
  target_subject_no     varchar(64)     default ''                 comment '目标主体内部编号',
  target_account_id     bigint(20)      not null                   comment '目标端账号ID',
  target_user_name      varchar(64)     not null                   comment '目标登录账号',
  acting_admin_id       bigint(20)      not null                   comment '代入管理员ID',
  acting_admin_name     varchar(64)     not null                   comment '代入管理员账号',
  reason                varchar(255)    default ''                 comment '代入原因',
  token_hash            varchar(64)     not null                   comment '免密token SHA-256哈希',
  expire_time           datetime        not null                   comment '过期时间',
  used_time             datetime        default null               comment '使用时间',
  used_ip               varchar(128)    default ''                 comment '使用IP',
  status                varchar(20)     not null default 'ISSUED'  comment '状态：ISSUED/USED/EXPIRED/CANCELLED',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (ticket_id),
  unique key uk_portal_direct_login_ticket_hash (token_hash),
  key idx_portal_direct_login_ticket_target (terminal, target_subject_id, target_account_id),
  key idx_portal_direct_login_ticket_admin_time (acting_admin_id, create_time),
  key idx_portal_direct_login_ticket_status_expire (status, expire_time)
) engine=innodb auto_increment=1 comment = '管理端免密代入审计票据表';

call assert_terminal_menu_range_ready();
call add_column_if_missing('seller_menu', 'perms_unique_key',
  'varchar(100) generated always as (case when trim(coalesce(perms, '''')) = '''' then null else trim(perms) end) stored');
call add_column_if_missing('buyer_menu', 'perms_unique_key',
  'varchar(100) generated always as (case when trim(coalesce(perms, '''')) = '''' then null else trim(perms) end) stored');
call recreate_index_if_mismatch('seller_menu', 'uk_seller_menu_perms',
  'perms_unique_key', 0, 'unique key uk_seller_menu_perms (perms_unique_key)');
call recreate_index_if_mismatch('buyer_menu', 'uk_buyer_menu_perms',
  'perms_unique_key', 0, 'unique key uk_buyer_menu_perms (perms_unique_key)');
call assert_terminal_menu_perms_unique_index('seller_menu', 'uk_seller_menu_perms',
  'seller_menu perms unique index is invalid');
call assert_terminal_menu_perms_unique_index('buyer_menu', 'uk_buyer_menu_perms',
  'buyer_menu perms unique index is invalid');
call assert_terminal_owner_role_slots_ready();

start transaction;

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select
    '主体类型', 'subject_type', '0', 'admin', sysdate(), '', null, '主体类型：公司/个人/其他'
where not exists (select 1 from sys_dict_type where dict_type = 'subject_type');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'subject_type', '', '', seed.is_default, '0', 'admin', sysdate(), '', null, '主体类型'
from (
    select 1 as dict_sort, '公司' as dict_label, 'COMPANY' as dict_value, 'Y' as is_default
    union all select 2, '个人', 'PERSON', 'N'
    union all select 3, '其他', 'OTHER', 'N'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'subject_type' and d.dict_value = seed.dict_value
);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '卖家等级', 'seller_level', '0', 'admin', sysdate(), '', null, '卖家等级'
where not exists (select 1 from sys_dict_type where dict_type = 'seller_level');

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '买家等级', 'buyer_level', '0', 'admin', sysdate(), '', null, '买家等级'
where not exists (select 1 from sys_dict_type where dict_type = 'buyer_level');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, level_type.dict_type, '', '', seed.is_default, '0', 'admin', sysdate(), '', null, level_type.dict_name
from (
    select 'seller_level' as dict_type, '卖家等级' as dict_name
    union all select 'buyer_level', '买家等级'
) level_type
join (
    select 1 as dict_sort, '等级1' as dict_label, 'L1' as dict_value, 'Y' as is_default
    union all select 2, '等级2', 'L2', 'N'
    union all select 3, '等级3', 'L3', 'N'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = level_type.dict_type and d.dict_value = seed.dict_value
);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '卖家账号角色', 'seller_account_role', '0', 'admin', sysdate(), '', null, '卖家账号角色'
where not exists (select 1 from sys_dict_type where dict_type = 'seller_account_role');

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '卖家账号锁定状态', 'seller_account_lock_status', '0', 'admin', sysdate(), '', null, '卖家账号锁定状态'
where not exists (select 1 from sys_dict_type where dict_type = 'seller_account_lock_status');

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '买家账号角色', 'buyer_account_role', '0', 'admin', sysdate(), '', null, '买家账号角色'
where not exists (select 1 from sys_dict_type where dict_type = 'buyer_account_role');

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '买家账号锁定状态', 'buyer_account_lock_status', '0', 'admin', sysdate(), '', null, '买家账号锁定状态'
where not exists (select 1 from sys_dict_type where dict_type = 'buyer_account_lock_status');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, role_type.dict_type, '', '', seed.is_default, '0', 'admin', sysdate(), '', null, role_type.dict_name
from (
    select 'seller_account_role' as dict_type, '卖家账号角色' as dict_name
    union all select 'buyer_account_role', '买家账号角色'
) role_type
join (
    select 1 as dict_sort, '负责人' as dict_label, 'OWNER' as dict_value, 'Y' as is_default
    union all select 2, '管理员', 'ADMIN', 'N'
    union all select 3, '普通账号', 'STAFF', 'N'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = role_type.dict_type and d.dict_value = seed.dict_value
);

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'seller_account_lock_status', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '卖家账号锁定状态'
from (
    select 1 as dict_sort, '未锁定' as dict_label, '0' as dict_value, 'Y' as is_default, 'success' as list_class
    union all select 2, '已锁定', '1', 'N', 'danger'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'seller_account_lock_status' and d.dict_value = seed.dict_value
);

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'buyer_account_lock_status', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '买家账号锁定状态'
from (
    select 1 as dict_sort, '未锁定' as dict_label, '0' as dict_value, 'Y' as is_default, 'success' as list_class
    union all select 2, '已锁定', '1', 'N', 'danger'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'buyer_account_lock_status' and d.dict_value = seed.dict_value
);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select
    '国家/地区', 'country_region', '0', 'admin', sysdate(), '', null, '国家/地区代码'
where not exists (select 1 from sys_dict_type where dict_type = 'country_region');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'country_region', '', '', seed.is_default, '0', 'admin', sysdate(), '', null, '国家/地区'
from (
    select 1 as dict_sort, '中国 / China (CN)' as dict_label, 'CN' as dict_value, 'Y' as is_default
    union all select 2, '美国 / United States (US)', 'US', 'N'
    union all select 3, '英国 / United Kingdom (GB)', 'GB', 'N'
    union all select 4, '加拿大 / Canada (CA)', 'CA', 'N'
    union all select 5, '墨西哥 / Mexico (MX)', 'MX', 'N'
    union all select 6, '巴西 / Brazil (BR)', 'BR', 'N'
    union all select 7, '日本 / Japan (JP)', 'JP', 'N'
    union all select 8, '韩国 / South Korea (KR)', 'KR', 'N'
    union all select 9, '新加坡 / Singapore (SG)', 'SG', 'N'
    union all select 10, '中国香港 / Hong Kong (HK)', 'HK', 'N'
    union all select 11, '中国台湾 / Taiwan (TW)', 'TW', 'N'
    union all select 12, '中国澳门 / Macau (MO)', 'MO', 'N'
    union all select 13, '澳大利亚 / Australia (AU)', 'AU', 'N'
    union all select 14, '德国 / Germany (DE)', 'DE', 'N'
    union all select 15, '法国 / France (FR)', 'FR', 'N'
    union all select 16, '意大利 / Italy (IT)', 'IT', 'N'
    union all select 17, '西班牙 / Spain (ES)', 'ES', 'N'
    union all select 18, '荷兰 / Netherlands (NL)', 'NL', 'N'
    union all select 19, '越南 / Vietnam (VN)', 'VN', 'N'
    union all select 20, '泰国 / Thailand (TH)', 'TH', 'N'
    union all select 21, '马来西亚 / Malaysia (MY)', 'MY', 'N'
    union all select 22, '印度 / India (IN)', 'IN', 'N'
    union all select 23, '印度尼西亚 / Indonesia (ID)', 'ID', 'N'
    union all select 24, '菲律宾 / Philippines (PH)', 'PH', 'N'
    union all select 25, '阿联酋 / United Arab Emirates (AE)', 'AE', 'N'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'country_region' and d.dict_value = seed.dict_value
);

drop temporary table if exists tmp_seller_buyer_sys_menu_guard;
create temporary table tmp_seller_buyer_sys_menu_guard (
  menu_id bigint not null primary key,
  parent_id bigint not null,
  menu_type char(1) not null,
  path varchar(200) not null default '',
  component varchar(255) not null default '',
  route_name varchar(50) not null default '',
  perms varchar(100) not null default ''
) engine=memory;

insert into tmp_seller_buyer_sys_menu_guard (menu_id, parent_id, menu_type, path, component, route_name, perms)
values
    (2011, 2010, 'C', 'seller', 'Seller/index', 'Seller', 'seller:admin:list'),
    (2012, 2010, 'C', 'buyer', 'Buyer/index', 'Buyer', 'buyer:admin:list'),
    (2200, 2011, 'F', '#', '', '', 'seller:admin:query'),
    (2201, 2011, 'F', '#', '', '', 'seller:admin:add'),
    (2202, 2011, 'F', '#', '', '', 'seller:admin:edit'),
    (2203, 2011, 'F', '#', '', '', 'seller:admin:changeStatus'),
    (2205, 2011, 'F', '#', '', '', 'seller:admin:directLogin'),
    (2206, 2011, 'F', '#', '', '', 'seller:admin:forceLogout'),
    (2256, 2011, 'F', '#', '', '', 'seller:admin:session:list'),
    (2210, 2012, 'F', '#', '', '', 'buyer:admin:query'),
    (2211, 2012, 'F', '#', '', '', 'buyer:admin:add'),
    (2212, 2012, 'F', '#', '', '', 'buyer:admin:edit'),
    (2213, 2012, 'F', '#', '', '', 'buyer:admin:changeStatus'),
    (2215, 2012, 'F', '#', '', '', 'buyer:admin:directLogin'),
    (2216, 2012, 'F', '#', '', '', 'buyer:admin:forceLogout'),
    (2257, 2012, 'F', '#', '', '', 'buyer:admin:session:list'),
    (2220, 2011, 'F', '#', '', '', 'seller:admin:menu:list'),
    (2221, 2011, 'F', '#', '', '', 'seller:admin:menu:query'),
    (2222, 2011, 'F', '#', '', '', 'seller:admin:menu:add'),
    (2223, 2011, 'F', '#', '', '', 'seller:admin:menu:edit'),
    (2224, 2011, 'F', '#', '', '', 'seller:admin:menu:remove'),
    (2225, 2011, 'F', '#', '', '', 'seller:admin:role:list'),
    (2226, 2011, 'F', '#', '', '', 'seller:admin:role:query'),
    (2227, 2011, 'F', '#', '', '', 'seller:admin:role:add'),
    (2228, 2011, 'F', '#', '', '', 'seller:admin:role:edit'),
    (2229, 2011, 'F', '#', '', '', 'seller:admin:role:remove'),
    (2230, 2012, 'F', '#', '', '', 'buyer:admin:menu:list'),
    (2231, 2012, 'F', '#', '', '', 'buyer:admin:menu:query'),
    (2232, 2012, 'F', '#', '', '', 'buyer:admin:menu:add'),
    (2233, 2012, 'F', '#', '', '', 'buyer:admin:menu:edit'),
    (2234, 2012, 'F', '#', '', '', 'buyer:admin:menu:remove'),
    (2235, 2012, 'F', '#', '', '', 'buyer:admin:role:list'),
    (2236, 2012, 'F', '#', '', '', 'buyer:admin:role:query'),
    (2237, 2012, 'F', '#', '', '', 'buyer:admin:role:add'),
    (2238, 2012, 'F', '#', '', '', 'buyer:admin:role:edit'),
    (2239, 2012, 'F', '#', '', '', 'buyer:admin:role:remove'),
    (2240, 2011, 'F', '#', '', '', 'seller:admin:dept:list'),
    (2241, 2011, 'F', '#', '', '', 'seller:admin:dept:query'),
    (2242, 2011, 'F', '#', '', '', 'seller:admin:dept:add'),
    (2243, 2011, 'F', '#', '', '', 'seller:admin:dept:edit'),
    (2244, 2011, 'F', '#', '', '', 'seller:admin:dept:remove'),
    (2245, 2012, 'F', '#', '', '', 'buyer:admin:dept:list'),
    (2246, 2012, 'F', '#', '', '', 'buyer:admin:dept:query'),
    (2247, 2012, 'F', '#', '', '', 'buyer:admin:dept:add'),
    (2248, 2012, 'F', '#', '', '', 'buyer:admin:dept:edit'),
    (2249, 2012, 'F', '#', '', '', 'buyer:admin:dept:remove'),
    (2250, 2011, 'F', '#', '', '', 'seller:admin:loginLog:list'),
    (2251, 2011, 'F', '#', '', '', 'seller:admin:operLog:list'),
    (2252, 2011, 'F', '#', '', '', 'seller:admin:ticket:list'),
    (2310, 2011, 'F', '#', '', '', 'seller:admin:account:list'),
    (2311, 2011, 'F', '#', '', '', 'seller:admin:account:add'),
    (2312, 2011, 'F', '#', '', '', 'seller:admin:account:edit'),
    (2322, 2011, 'F', '#', '', '', 'seller:admin:account:lock'),
    (2313, 2011, 'F', '#', '', '', 'seller:admin:account:resetPwd'),
    (2314, 2011, 'F', '#', '', '', 'seller:admin:account:role:query'),
    (2315, 2011, 'F', '#', '', '', 'seller:admin:account:role:edit'),
    (2253, 2012, 'F', '#', '', '', 'buyer:admin:loginLog:list'),
    (2254, 2012, 'F', '#', '', '', 'buyer:admin:operLog:list'),
    (2255, 2012, 'F', '#', '', '', 'buyer:admin:ticket:list'),
    (2316, 2012, 'F', '#', '', '', 'buyer:admin:account:list'),
    (2317, 2012, 'F', '#', '', '', 'buyer:admin:account:add'),
    (2318, 2012, 'F', '#', '', '', 'buyer:admin:account:edit'),
    (2323, 2012, 'F', '#', '', '', 'buyer:admin:account:lock'),
    (2319, 2012, 'F', '#', '', '', 'buyer:admin:account:resetPwd'),
    (2320, 2012, 'F', '#', '', '', 'buyer:admin:account:role:query'),
    (2321, 2012, 'F', '#', '', '', 'buyer:admin:account:role:edit');

call assert_seller_buyer_sys_menu_seed_guard();

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2011, '卖家管理', 2010, 5, 'seller', 'Seller/index', '', 'Seller',
     1, 0, 'C', '0', '0', 'seller:admin:list', 'ShopOutlined', 'admin',
     sysdate(), '', null, '管理端卖家管理'),
    (2012, '买家管理', 2010, 10, 'buyer', 'Buyer/index', '', 'Buyer',
     1, 0, 'C', '0', '0', 'buyer:admin:list', 'UserOutlined', 'admin',
     sysdate(), '', null, '管理端买家管理'),
    (2200, '卖家查询', 2011, 5, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2201, '卖家新增', 2011, 10, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2202, '卖家修改', 2011, 15, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2203, '卖家启停', 2011, 20, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:changeStatus', '#', 'admin',
     sysdate(), '', null, ''),
    (2205, '卖家免密登录', 2011, 30, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:directLogin', '#', 'admin',
     sysdate(), '', null, ''),
    (2206, '卖家强制踢出', 2011, 32, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:forceLogout', '#', 'admin',
     sysdate(), '', null, ''),
    (2256, '卖家会话查看', 2011, 33, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:session:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2210, '买家查询', 2012, 5, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2211, '买家新增', 2012, 10, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2212, '买家修改', 2012, 15, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2213, '买家启停', 2012, 20, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:changeStatus', '#', 'admin',
     sysdate(), '', null, ''),
    (2215, '买家免密登录', 2012, 30, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:directLogin', '#', 'admin',
     sysdate(), '', null, ''),
    (2216, '买家强制踢出', 2012, 32, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:forceLogout', '#', 'admin',
     sysdate(), '', null, ''),
    (2257, '买家会话查看', 2012, 33, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:session:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2220, '卖家端菜单列表', 2011, 35, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:menu:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2221, '卖家端菜单查询', 2011, 40, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:menu:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2222, '卖家端菜单新增', 2011, 45, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:menu:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2223, '卖家端菜单修改', 2011, 50, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:menu:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2224, '卖家端菜单删除', 2011, 55, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:menu:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2225, '卖家端角色列表', 2011, 60, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:role:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2226, '卖家端角色查询', 2011, 65, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:role:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2227, '卖家端角色新增', 2011, 70, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:role:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2228, '卖家端角色修改', 2011, 75, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:role:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2229, '卖家端角色删除', 2011, 80, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:role:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2230, '买家端菜单列表', 2012, 35, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:menu:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2231, '买家端菜单查询', 2012, 40, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:menu:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2232, '买家端菜单新增', 2012, 45, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:menu:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2233, '买家端菜单修改', 2012, 50, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:menu:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2234, '买家端菜单删除', 2012, 55, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:menu:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2235, '买家端角色列表', 2012, 60, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:role:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2236, '买家端角色查询', 2012, 65, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:role:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2237, '买家端角色新增', 2012, 70, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:role:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2238, '买家端角色修改', 2012, 75, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:role:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2239, '买家端角色删除', 2012, 80, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:role:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2240, '卖家端部门列表', 2011, 85, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:dept:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2241, '卖家端部门查询', 2011, 90, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:dept:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2242, '卖家端部门新增', 2011, 95, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:dept:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2243, '卖家端部门修改', 2011, 100, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:dept:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2244, '卖家端部门删除', 2011, 105, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:dept:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2245, '买家端部门列表', 2012, 85, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:dept:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2246, '买家端部门查询', 2012, 90, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:dept:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2247, '买家端部门新增', 2012, 95, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:dept:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2248, '买家端部门修改', 2012, 100, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:dept:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2249, '买家端部门删除', 2012, 105, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:dept:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2250, '卖家登录日志列表', 2011, 110, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:loginLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2251, '卖家操作日志列表', 2011, 115, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:operLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2252, '卖家免密票据列表', 2011, 120, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:ticket:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2310, '卖家账号列表', 2011, 125, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2311, '卖家账号新增', 2011, 130, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2312, '卖家账号修改', 2011, 135, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2322, '卖家账号锁定解锁', 2011, 138, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:lock', '#', 'admin',
     sysdate(), '', null, ''),
    (2313, '卖家账号重置密码', 2011, 140, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:resetPwd', '#', 'admin',
     sysdate(), '', null, ''),
    (2314, '卖家账号角色查询', 2011, 145, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:role:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2315, '卖家账号角色分配', 2011, 150, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:role:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2253, '买家登录日志列表', 2012, 110, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:loginLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2254, '买家操作日志列表', 2012, 115, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:operLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2255, '买家免密票据列表', 2012, 120, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:ticket:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2316, '买家账号列表', 2012, 125, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2317, '买家账号新增', 2012, 130, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2318, '买家账号修改', 2012, 135, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2323, '买家账号锁定解锁', 2012, 138, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:lock', '#', 'admin',
     sysdate(), '', null, ''),
    (2319, '买家账号重置密码', 2012, 140, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:resetPwd', '#', 'admin',
     sysdate(), '', null, ''),
    (2320, '买家账号角色查询', 2012, 145, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:role:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2321, '买家账号角色分配', 2012, 150, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:role:edit', '#', 'admin',
     sysdate(), '', null, '')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query = values(query),
    route_name = values(route_name),
    is_frame = values(is_frame),
    is_cache = values(is_cache),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    update_by = 'admin',
    update_time = sysdate(),
    remark = values(remark);

insert into sys_config
    (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select '卖家端前端地址', 'portal.seller.web.url', 'http://127.0.0.1:8001/seller/direct-login',
       'Y', 'admin', sysdate(), '', null, '管理端免密登录卖家端地址，占位配置'
where not exists (select 1 from sys_config where config_key = 'portal.seller.web.url');

insert into sys_config
    (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select '买家端前端地址', 'portal.buyer.web.url', 'http://127.0.0.1:8001/buyer/direct-login',
       'Y', 'admin', sysdate(), '', null, '管理端免密登录买家端地址，占位配置'
where not exists (select 1 from sys_config where config_key = 'portal.buyer.web.url');

-- 端内门户默认角色与权限初始化
-- 保证只执行综合 seed 的新环境也具备 seller/buyer 端内只读权限和 Owner 角色授权。

insert into seller_role
    (seller_id, role_name, role_key, role_sort, status, del_flag,
     create_by, create_time, update_by, update_time, remark)
select s.seller_id, 'Owner', 'owner', 1, '0', '0',
       'admin', sysdate(), '', null, '默认卖家端 Owner 角色'
from seller s
where s.status = '0'
  and not exists (
      select 1
      from seller_role r
      where r.seller_id = s.seller_id
        and r.role_key = 'owner'
        and r.del_flag = '0'
  );

insert into buyer_role
    (buyer_id, role_name, role_key, role_sort, status, del_flag,
     create_by, create_time, update_by, update_time, remark)
select b.buyer_id, 'Owner', 'owner', 1, '0', '0',
       'admin', sysdate(), '', null, '默认买家端 Owner 角色'
from buyer b
where b.status = '0'
  and not exists (
      select 1
      from buyer_role r
      where r.buyer_id = b.buyer_id
        and r.role_key = 'owner'
        and r.del_flag = '0'
  );

call assert_seller_menu_permission_slot('seller:account:list', 0, 'F', '', null, '',
    'seller:account:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:add', 0, 'F', '', null, '',
    'seller:account:add menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:edit', 0, 'F', '', null, '',
    'seller:account:edit menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:role:query', 0, 'F', '', null, '',
    'seller:account:role:query menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:role:edit', 0, 'F', '', null, '',
    'seller:account:role:edit menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:loginLog:list', 0, 'F', '', null, '',
    'seller:account:loginLog:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:operLog:list', 0, 'F', '', null, '',
    'seller:account:operLog:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:account:session:list', 0, 'F', '', null, '',
    'seller:account:session:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:dept:list', 0, 'F', '', null, '',
    'seller:dept:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:dept:query', 0, 'F', '', null, '',
    'seller:dept:query menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:dept:add', 0, 'F', '', null, '',
    'seller:dept:add menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:dept:edit', 0, 'F', '', null, '',
    'seller:dept:edit menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:dept:remove', 0, 'F', '', null, '',
    'seller:dept:remove menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:role:list', 0, 'F', '', null, '',
    'seller:role:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:role:query', 0, 'F', '', null, '',
    'seller:role:query menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:role:add', 0, 'F', '', null, '',
    'seller:role:add menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:role:edit', 0, 'F', '', null, '',
    'seller:role:edit menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:role:remove', 0, 'F', '', null, '',
    'seller:role:remove menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:product:category:list', 0, 'F', '', null, '',
    'seller:product:category:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:product:schema:query', 0, 'F', '', null, '',
    'seller:product:schema:query menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:product:distribution:list', 0, 'F', '', null, '',
    'seller:product:distribution:list menu slot is occupied by another signature');
call assert_seller_menu_permission_slot('seller:product:distribution:query', 0, 'F', '', null, '',
    'seller:product:distribution:query menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:list', 0, 'F', '', null, '',
    'buyer:account:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:add', 0, 'F', '', null, '',
    'buyer:account:add menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:edit', 0, 'F', '', null, '',
    'buyer:account:edit menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:role:query', 0, 'F', '', null, '',
    'buyer:account:role:query menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:role:edit', 0, 'F', '', null, '',
    'buyer:account:role:edit menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:loginLog:list', 0, 'F', '', null, '',
    'buyer:account:loginLog:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:operLog:list', 0, 'F', '', null, '',
    'buyer:account:operLog:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:account:session:list', 0, 'F', '', null, '',
    'buyer:account:session:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:dept:list', 0, 'F', '', null, '',
    'buyer:dept:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:dept:query', 0, 'F', '', null, '',
    'buyer:dept:query menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:dept:add', 0, 'F', '', null, '',
    'buyer:dept:add menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:dept:edit', 0, 'F', '', null, '',
    'buyer:dept:edit menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:dept:remove', 0, 'F', '', null, '',
    'buyer:dept:remove menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:role:list', 0, 'F', '', null, '',
    'buyer:role:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:role:query', 0, 'F', '', null, '',
    'buyer:role:query menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:role:add', 0, 'F', '', null, '',
    'buyer:role:add menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:role:edit', 0, 'F', '', null, '',
    'buyer:role:edit menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:role:remove', 0, 'F', '', null, '',
    'buyer:role:remove menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:product:category:list', 0, 'F', '', null, '',
    'buyer:product:category:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:product:schema:query', 0, 'F', '', null, '',
    'buyer:product:schema:query menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:product:distribution:list', 0, 'F', '', null, '',
    'buyer:product:distribution:list menu slot is occupied by another signature');
call assert_buyer_menu_permission_slot('buyer:product:distribution:query', 0, 'F', '', null, '',
    'buyer:product:distribution:query menu slot is occupied by another signature');

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_name, 0, seed.order_num, '', null, '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select '账号列表' as menu_name, 30 as order_num, 'seller:account:list' as perms, '卖家端账号只读列表权限' as remark
    union all select '账号新增', 31, 'seller:account:add', '卖家端账号新增权限'
    union all select '账号修改', 32, 'seller:account:edit', '卖家端账号修改权限'
    union all select '账号角色查询', 33, 'seller:account:role:query', '卖家端账号角色查询权限'
    union all select '账号角色分配', 34, 'seller:account:role:edit', '卖家端账号角色分配权限'
    union all select '登录日志', 35, 'seller:account:loginLog:list', '卖家端本人登录日志只读权限'
    union all select '操作日志', 36, 'seller:account:operLog:list', '卖家端本人操作日志只读权限'
    union all select '会话列表', 37, 'seller:account:session:list', '卖家端本人会话只读权限'
    union all select '部门列表', 40, 'seller:dept:list', '卖家端部门只读列表权限'
    union all select '部门详情', 41, 'seller:dept:query', '卖家端部门详情查询权限'
    union all select '部门新增', 42, 'seller:dept:add', '卖家端部门新增权限'
    union all select '部门修改', 43, 'seller:dept:edit', '卖家端部门修改权限'
    union all select '部门删除', 44, 'seller:dept:remove', '卖家端部门删除权限'
    union all select '角色列表', 45, 'seller:role:list', '卖家端角色只读列表权限'
    union all select '角色详情', 46, 'seller:role:query', '卖家端角色详情查询权限'
    union all select '角色新增', 47, 'seller:role:add', '卖家端角色新增权限'
    union all select '角色修改', 48, 'seller:role:edit', '卖家端角色修改权限'
    union all select '角色删除', 49, 'seller:role:remove', '卖家端角色删除权限'
    union all select 'Product Category List', 50, 'seller:product:category:list', 'Seller portal product category list permission'
    union all select 'Product Schema Query', 51, 'seller:product:schema:query', 'Seller portal product schema read permission'
    union all select 'Distribution Product List', 52, 'seller:product:distribution:list', 'Seller portal own distribution product list permission'
    union all select 'Distribution Product Query', 53, 'seller:product:distribution:query', 'Seller portal own distribution product read permission'
) seed
where not exists (
    select 1 from seller_menu m where m.perms = seed.perms
);

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_name, 0, seed.order_num, '', null, '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select '账号列表' as menu_name, 30 as order_num, 'buyer:account:list' as perms, '买家端账号只读列表权限' as remark
    union all select '账号新增', 31, 'buyer:account:add', '买家端账号新增权限'
    union all select '账号修改', 32, 'buyer:account:edit', '买家端账号修改权限'
    union all select '账号角色查询', 33, 'buyer:account:role:query', '买家端账号角色查询权限'
    union all select '账号角色分配', 34, 'buyer:account:role:edit', '买家端账号角色分配权限'
    union all select '登录日志', 35, 'buyer:account:loginLog:list', '买家端本人登录日志只读权限'
    union all select '操作日志', 36, 'buyer:account:operLog:list', '买家端本人操作日志只读权限'
    union all select '会话列表', 37, 'buyer:account:session:list', '买家端本人会话只读权限'
    union all select '部门列表', 40, 'buyer:dept:list', '买家端部门只读列表权限'
    union all select '部门详情', 41, 'buyer:dept:query', '买家端部门详情查询权限'
    union all select '部门新增', 42, 'buyer:dept:add', '买家端部门新增权限'
    union all select '部门修改', 43, 'buyer:dept:edit', '买家端部门修改权限'
    union all select '部门删除', 44, 'buyer:dept:remove', '买家端部门删除权限'
    union all select '角色列表', 45, 'buyer:role:list', '买家端角色只读列表权限'
    union all select '角色详情', 46, 'buyer:role:query', '买家端角色详情查询权限'
    union all select '角色新增', 47, 'buyer:role:add', '买家端角色新增权限'
    union all select '角色修改', 48, 'buyer:role:edit', '买家端角色修改权限'
    union all select '角色删除', 49, 'buyer:role:remove', '买家端角色删除权限'
    union all select 'Product Category List', 50, 'buyer:product:category:list', 'Buyer portal product category list permission'
    union all select 'Product Schema Query', 51, 'buyer:product:schema:query', 'Buyer portal product schema read permission'
    union all select 'Distribution Product List', 52, 'buyer:product:distribution:list', 'Buyer portal distribution product list permission'
    union all select 'Distribution Product Query', 53, 'buyer:product:distribution:query', 'Buyer portal distribution product read permission'
) seed
where not exists (
    select 1 from buyer_menu m where m.perms = seed.perms
);

insert into seller_account_role (seller_account_id, seller_role_id)
select a.seller_account_id, r.seller_role_id
from seller_account a
join seller_role r on r.seller_id = a.seller_id
                  and r.role_key = 'owner'
                  and r.status = '0'
                  and r.del_flag = '0'
where a.account_role = 'OWNER'
  and not exists (
      select 1
      from seller_account_role ar
      where ar.seller_account_id = a.seller_account_id
        and ar.seller_role_id = r.seller_role_id
  );

insert into buyer_account_role (buyer_account_id, buyer_role_id)
select a.buyer_account_id, r.buyer_role_id
from buyer_account a
join buyer_role r on r.buyer_id = a.buyer_id
                 and r.role_key = 'owner'
                 and r.status = '0'
                 and r.del_flag = '0'
where a.account_role = 'OWNER'
  and not exists (
      select 1
      from buyer_account_role ar
      where ar.buyer_account_id = a.buyer_account_id
        and ar.buyer_role_id = r.buyer_role_id
  );

insert into seller_role_menu (seller_role_id, seller_menu_id)
select r.seller_role_id, m.seller_menu_id
from seller_role r
join seller_menu m on m.perms in (
    'seller:account:list',
    'seller:account:add',
    'seller:account:edit',
    'seller:account:role:query',
    'seller:account:role:edit',
    'seller:account:loginLog:list',
    'seller:account:operLog:list',
    'seller:account:session:list',
    'seller:dept:list',
    'seller:dept:query',
    'seller:dept:add',
    'seller:dept:edit',
    'seller:dept:remove',
    'seller:role:list',
    'seller:role:query',
    'seller:role:add',
    'seller:role:edit',
    'seller:role:remove'
)
                  and m.parent_id = 0
                  and coalesce(m.menu_type, '') = 'F'
                  and coalesce(m.path, '') = ''
                  and coalesce(m.component, '') = ''
                  and coalesce(m.route_name, '') = ''
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and not exists (
      select 1
      from seller_role_menu rm
      where rm.seller_role_id = r.seller_role_id
        and rm.seller_menu_id = m.seller_menu_id
  );

insert into buyer_role_menu (buyer_role_id, buyer_menu_id)
select r.buyer_role_id, m.buyer_menu_id
from buyer_role r
join buyer_menu m on m.perms in (
    'buyer:account:list',
    'buyer:account:add',
    'buyer:account:edit',
    'buyer:account:role:query',
    'buyer:account:role:edit',
    'buyer:account:loginLog:list',
    'buyer:account:operLog:list',
    'buyer:account:session:list',
    'buyer:dept:list',
    'buyer:dept:query',
    'buyer:dept:add',
    'buyer:dept:edit',
    'buyer:dept:remove',
    'buyer:role:list',
    'buyer:role:query',
    'buyer:role:add',
    'buyer:role:edit',
    'buyer:role:remove'
)
                 and m.parent_id = 0
                 and coalesce(m.menu_type, '') = 'F'
                 and coalesce(m.path, '') = ''
                 and coalesce(m.component, '') = ''
                 and coalesce(m.route_name, '') = ''
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and not exists (
      select 1
      from buyer_role_menu rm
      where rm.buyer_role_id = r.buyer_role_id
        and rm.buyer_menu_id = m.buyer_menu_id
  );

call assert_seller_buyer_management_seed_completed();

commit;

drop temporary table if exists tmp_seller_buyer_sys_menu_guard;
drop procedure if exists assert_seller_buyer_management_seed_confirmed;
drop procedure if exists assert_seller_buyer_management_seed_profile;
drop procedure if exists assert_seller_buyer_sys_menu_seed_guard;
drop procedure if exists assert_seller_menu_permission_slot;
drop procedure if exists assert_buyer_menu_permission_slot;
drop procedure if exists assert_terminal_menu_range_ready;
drop procedure if exists add_index_if_missing;
drop procedure if exists add_column_if_missing;
drop procedure if exists assert_no_blank_terminal_passwords;
drop procedure if exists modify_terminal_account_password_column_if_needed;
drop procedure if exists assert_terminal_account_password_column_final;
drop procedure if exists recreate_index_if_mismatch;
drop procedure if exists assert_terminal_menu_perms_unique_index;
drop procedure if exists assert_terminal_owner_role_slots_ready;
drop procedure if exists assert_seller_buyer_management_seed_completed;
