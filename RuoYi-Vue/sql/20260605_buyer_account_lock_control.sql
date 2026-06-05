-- Buyer account lock control migration.
-- Scope: buyer_account only. This copies the validated seller lock template with buyer-specific names.

set names utf8mb4;

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

delimiter ;

call add_column_if_missing(
  'buyer_account',
  'lock_status',
  'char(1) not null default ''0'' comment ''锁定状态：0未锁定 1已锁定'' after status'
);

call add_column_if_missing(
  'buyer_account',
  'lock_reason',
  'varchar(500) not null default '''' comment ''锁定原因'' after lock_status'
);

update buyer_account
set lock_status = case when lock_status in ('0', '1') then lock_status else '0' end,
    lock_reason = coalesce(lock_reason, '')
where lock_status is null
   or lock_status = ''
   or lock_status not in ('0', '1')
   or lock_reason is null;

call add_index_if_missing(
  'buyer_account',
  'idx_buyer_account_buyer_lock',
  'key idx_buyer_account_buyer_lock (buyer_id, lock_status)'
);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '买家账号锁定状态', 'buyer_account_lock_status', '0', 'admin', sysdate(), '', null, '买家账号锁定状态'
where not exists (
    select 1 from sys_dict_type where dict_type = 'buyer_account_lock_status'
);

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
     status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'buyer_account_lock_status',
       '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '买家账号锁定状态'
from (
    select 1 as dict_sort, '未锁定' as dict_label, '0' as dict_value, 'Y' as is_default, 'success' as list_class
    union all select 2, '已锁定', '1', 'N', 'danger'
) seed
where not exists (
    select 1
    from sys_dict_data d
    where d.dict_type = 'buyer_account_lock_status'
      and d.dict_value = seed.dict_value
);

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2323, '买家账号锁定解锁', 2012, 138, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:lock', '#', 'admin',
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
