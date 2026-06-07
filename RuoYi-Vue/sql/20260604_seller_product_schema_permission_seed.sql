-- Seller portal product schema query permission seed
-- Scope:
-- 1. Ensure current active sellers have a default owner role.
-- 2. Bind current OWNER seller accounts to the default owner role.
-- 3. Add seller:product:schema:query as a hidden button permission.
-- 4. Grant the permission to current active seller roles.

set names utf8mb4;
set @confirm_seller_product_schema_permission_seed := coalesce(@confirm_seller_product_schema_permission_seed, '');

delimiter //

drop procedure if exists assert_seller_product_schema_permission_seed_confirmed//
create procedure assert_seller_product_schema_permission_seed_confirmed()
begin
  if coalesce(@confirm_seller_product_schema_permission_seed, '')
      <> 'APPLY_SELLER_PRODUCT_SCHEMA_PERMISSION_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_seller_product_schema_permission_seed = APPLY_SELLER_PRODUCT_SCHEMA_PERMISSION_SEED before running this seed';
  end if;
end//

delimiter ;

call assert_seller_product_schema_permission_seed_confirmed();
drop procedure if exists assert_seller_product_schema_permission_seed_confirmed;

delimiter //

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
        parent_id <> p_parent_id
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

  if coalesce(v_auto_increment, 0) < 100000 then
    signal sqlstate '45000' set message_text = 'seller_menu auto_increment must be >= 100000 before terminal menu seed inserts';
  end if;
end//

delimiter ;

call assert_terminal_menu_range_ready();

insert into seller_role
    (seller_id, role_name, role_key, role_sort, status, del_flag,
     create_by, create_time, update_by, update_time, remark)
select s.seller_id, 'Owner', 'owner', 1, '0', '0',
       'admin', sysdate(), '', null, 'Default seller portal owner role'
from seller s
where s.status = '0'
  and not exists (
      select 1
      from seller_role r
      where r.seller_id = s.seller_id
        and r.role_key = 'owner'
        and r.del_flag = '0'
  );

insert into seller_account_role (seller_account_id, seller_role_id)
select a.seller_account_id, r.seller_role_id
from seller_account a
join seller_role r on r.seller_id = a.seller_id
                  and r.role_key = 'owner'
                  and r.del_flag = '0'
where a.account_role = 'OWNER'
  and not exists (
      select 1
      from seller_account_role ar
      where ar.seller_account_id = a.seller_account_id
        and ar.seller_role_id = r.seller_role_id
  );

call assert_seller_menu_permission_slot('seller:product:schema:query', 0, 'F', '', null, '',
    'seller:product:schema:query menu slot is occupied by another signature');

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    'Product Schema Query', 0, 50, '', null, '', '',
    1, 0, 'F', '0', '0', 'seller:product:schema:query', '#', 'admin',
    sysdate(), '', null, 'Seller portal product schema read permission'
where not exists (
    select 1 from seller_menu where perms = 'seller:product:schema:query'
);

insert into seller_role_menu (seller_role_id, seller_menu_id)
select r.seller_role_id, m.seller_menu_id
from seller_role r
join seller_menu m on m.perms = 'seller:product:schema:query'
                  and m.parent_id = 0
                  and coalesce(m.menu_type, '') = 'F'
                  and coalesce(m.path, '') = ''
                  and coalesce(m.component, '') = ''
                  and coalesce(m.route_name, '') = ''
where r.del_flag = '0'
  and r.status = '0'
  and not exists (
      select 1
      from seller_role_menu rm
      where rm.seller_role_id = r.seller_role_id
        and rm.seller_menu_id = m.seller_menu_id
  );

drop procedure if exists assert_seller_menu_permission_slot;
drop procedure if exists assert_terminal_menu_range_ready;
