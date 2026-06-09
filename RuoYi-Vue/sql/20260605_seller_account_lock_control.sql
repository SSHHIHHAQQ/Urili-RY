-- Seller account lock control migration.
-- Scope: seller_account only. Buyer account lock control is intentionally not copied in this slice.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_seller_account_lock_control := coalesce(@confirm_seller_account_lock_control, '');
set @seller_account_lock_normalize_expected_count :=
    coalesce(@seller_account_lock_normalize_expected_count, '');
set @seller_account_lock_normalize_expected_signature :=
    coalesce(@seller_account_lock_normalize_expected_signature, '');

delimiter //

drop procedure if exists assert_seller_account_lock_control_confirmed//
create procedure assert_seller_account_lock_control_confirmed()
begin
  if coalesce(@confirm_seller_account_lock_control, '')
      <> 'APPLY_SELLER_ACCOUNT_LOCK_CONTROL' then
    signal sqlstate '45000' set message_text = 'set @confirm_seller_account_lock_control = APPLY_SELLER_ACCOUNT_LOCK_CONTROL before running this migration';
  end if;

  if coalesce(@seller_account_lock_normalize_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @seller_account_lock_normalize_expected_count after previewing exact seller_account lock normalize rows';
  end if;
  if coalesce(@seller_account_lock_normalize_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @seller_account_lock_normalize_expected_signature after previewing exact seller_account lock normalize rows';
  end if;
end//

delimiter ;

call assert_seller_account_lock_control_confirmed();
drop procedure if exists assert_seller_account_lock_control_confirmed;

delimiter //

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

drop procedure if exists add_index_if_missing//
create procedure add_index_if_missing(in p_table varchar(64), in p_index varchar(64), in p_definition text)
begin
  if not exists (
    select 1
    from information_schema.statistics
    where table_schema = database()
      and table_name = p_table
      and index_name = p_index
  ) then
    set @ddl = concat('alter table ', p_table, ' add ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_sys_menu_slot//
create procedure assert_sys_menu_slot(
  in p_menu_id bigint,
  in p_parent_id bigint,
  in p_menu_type char(1),
  in p_path varchar(200),
  in p_component varchar(255),
  in p_route_name varchar(50),
  in p_perms varchar(100),
  in p_message varchar(128)
)
begin
  if exists (
    select 1
    from sys_menu
    where menu_id = p_menu_id
      and (
        coalesce(parent_id, -1) <> p_parent_id
        or coalesce(menu_type, '') <> coalesce(p_menu_type, '')
        or coalesce(path, '') <> coalesce(p_path, '')
        or coalesce(component, '') <> coalesce(p_component, '')
        or coalesce(route_name, '') <> coalesce(p_route_name, '')
        or coalesce(perms, '') <> coalesce(p_perms, '')
      )
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_sys_menu_signature_available//
create procedure assert_sys_menu_signature_available(
  in p_menu_id bigint,
  in p_path varchar(200),
  in p_component varchar(255),
  in p_route_name varchar(50),
  in p_perms varchar(100),
  in p_message varchar(128)
)
begin
  if exists (
    select 1
    from sys_menu
    where menu_id <> p_menu_id
      and coalesce(path, '') = coalesce(p_path, '')
      and coalesce(component, '') = coalesce(p_component, '')
      and coalesce(route_name, '') = coalesce(p_route_name, '')
      and coalesce(perms, '') = coalesce(p_perms, '')
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_partner_seller_parent_menu_ready//
create procedure assert_partner_seller_parent_menu_ready()
begin
  declare v_count int default 0;

  select count(1)
    into v_count
  from sys_menu
  where (menu_id = 2010
         and menu_name = '主体管理'
         and parent_id = 0
         and menu_type = 'M'
         and coalesce(path, '') = 'partner'
         and coalesce(component, '') = ''
         and coalesce(route_name, '') = 'PartnerManagement'
         and coalesce(perms, '') = '')
     or (menu_id = 2011
         and menu_name = '卖家管理'
         and parent_id = 2010
         and menu_type = 'C'
         and coalesce(path, '') = 'seller'
         and coalesce(component, '') = 'Seller/index'
         and coalesce(route_name, '') = 'Seller'
         and coalesce(perms, '') = 'seller:admin:list');

  if v_count <> 2 then
    signal sqlstate '45000' set message_text = 'partner seller parent menu signature does not match expected';
  end if;
end//

drop procedure if exists assert_seller_account_lock_normalize_targets//
create procedure assert_seller_account_lock_normalize_targets()
begin
  declare v_count int default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             seller_account_id,
             seller_id,
             coalesce(user_name, ''),
             coalesce(lock_status, ''),
             coalesce(lock_reason, '')
           )
           order by seller_account_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from seller_account
  where lock_status is null
     or lock_status = ''
     or lock_status not in ('0', '1')
     or lock_reason is null;

  if v_count <> cast(@seller_account_lock_normalize_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'seller_account lock normalize exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@seller_account_lock_normalize_expected_signature) then
    signal sqlstate '45000' set message_text = 'seller_account lock normalize exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_seller_account_lock_columns_ready//
create procedure assert_seller_account_lock_columns_ready()
begin
  declare v_count int default 0;

  select count(1)
    into v_count
  from information_schema.columns
  where table_schema = database()
    and table_name = 'seller_account'
    and column_name = 'lock_status'
    and column_type = 'char(1)'
    and is_nullable = 'NO'
    and column_default = '0';

  if v_count <> 1 then
    signal sqlstate '45000' set message_text = 'seller_account.lock_status definition does not match expected';
  end if;

  select count(1)
    into v_count
  from information_schema.columns
  where table_schema = database()
    and table_name = 'seller_account'
    and column_name = 'lock_reason'
    and column_type = 'varchar(500)'
    and is_nullable = 'NO'
    and column_default = '';

  if v_count <> 1 then
    signal sqlstate '45000' set message_text = 'seller_account.lock_reason definition does not match expected';
  end if;
end//

drop procedure if exists assert_seller_account_lock_index_ready//
create procedure assert_seller_account_lock_index_ready()
begin
  declare v_count int default 0;

  select count(1)
    into v_count
  from information_schema.statistics
  where table_schema = database()
    and table_name = 'seller_account'
    and index_name = 'idx_seller_account_seller_lock'
    and (
      (seq_in_index = 1 and column_name = 'seller_id')
      or (seq_in_index = 2 and column_name = 'lock_status')
    );

  if v_count <> 2 then
    signal sqlstate '45000' set message_text = 'seller_account lock index definition does not match expected';
  end if;
end//

delimiter ;

call assert_partner_seller_parent_menu_ready();
call assert_sys_menu_slot(2322, 2011, 'F', '#', '', '', 'seller:admin:account:lock', 'sys_menu 2322 is occupied by another menu');
call assert_sys_menu_signature_available(2322, '#', '', '', 'seller:admin:account:lock', 'seller account lock menu signature is already used by another menu');

call add_column_if_missing(
  'seller_account',
  'lock_status',
  'char(1) not null default ''0'' comment ''锁定状态：0未锁定 1已锁定'' after status'
);

call add_column_if_missing(
  'seller_account',
  'lock_reason',
  'varchar(500) not null default '''' comment ''锁定原因'' after lock_status'
);

call assert_seller_account_lock_columns_ready();
call assert_seller_account_lock_normalize_targets();

update seller_account
set lock_status = case when lock_status in ('0', '1') then lock_status else '0' end,
    lock_reason = coalesce(lock_reason, '')
where lock_status is null
   or lock_status = ''
   or lock_status not in ('0', '1')
   or lock_reason is null;

call add_index_if_missing(
  'seller_account',
  'idx_seller_account_seller_lock',
  'key idx_seller_account_seller_lock (seller_id, lock_status)'
);
call assert_seller_account_lock_index_ready();

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '卖家账号锁定状态', 'seller_account_lock_status', '0', 'admin', sysdate(), '', null, '卖家账号锁定状态'
where not exists (
    select 1 from sys_dict_type where dict_type = 'seller_account_lock_status'
);

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
     status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'seller_account_lock_status',
       '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '卖家账号锁定状态'
from (
    select 1 as dict_sort, '未锁定' as dict_label, '0' as dict_value, 'Y' as is_default, 'success' as list_class
    union all select 2, '已锁定', '1', 'N', 'danger'
) seed
where not exists (
    select 1
    from sys_dict_data d
    where d.dict_type = 'seller_account_lock_status'
      and d.dict_value = seed.dict_value
);

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2322, '卖家账号锁定解锁', 2011, 138, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:lock', '#', 'admin',
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

drop procedure if exists add_column_if_missing;
drop procedure if exists add_index_if_missing;
drop procedure if exists assert_partner_seller_parent_menu_ready;
drop procedure if exists assert_seller_account_lock_columns_ready;
drop procedure if exists assert_seller_account_lock_index_ready;
drop procedure if exists assert_seller_account_lock_normalize_targets;
drop procedure if exists assert_sys_menu_slot;
drop procedure if exists assert_sys_menu_signature_available;
