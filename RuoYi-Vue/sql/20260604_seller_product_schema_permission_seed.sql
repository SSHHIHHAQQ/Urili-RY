-- Seller portal product schema query permission seed
-- Scope:
-- 1. Ensure current active sellers have a default owner role.
-- 2. Bind current OWNER seller accounts to the default owner role.
-- 3. Add seller:product:schema:query as a hidden button permission.
-- 4. Do not default-grant this product permission to terminal Owner roles.

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
    where coalesce(perms, '') = ''
       or coalesce(perms, '') = '*'
       or coalesce(perms, '') not like 'seller:%'
       or coalesce(perms, '') like 'seller:admin:%'
       or coalesce(perms, '') like 'buyer:%'
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
      select perms
      from seller_menu
      group by perms
      having count(1) > 1
    ) duplicate_seller_menu_perms
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu perms must be unique before terminal role grants';
  end if;
end//

drop procedure if exists assert_seller_product_schema_permission_seed_completed//
create procedure assert_seller_product_schema_permission_seed_completed()
begin
  if exists (
    select 1
    from seller s
    where s.status = '0'
      and not exists (
        select 1
        from seller_role r
        where r.seller_id = s.seller_id
          and r.role_key = 'owner'
          and r.status = '0'
          and r.del_flag = '0'
      )
  ) then
    signal sqlstate '45000' set message_text = 'active sellers must have default owner roles';
  end if;

  if exists (
    select 1
    from seller_account a
    where a.account_role = 'OWNER'
      and not exists (
        select 1
        from seller_account_role ar
        join seller_role r on r.seller_role_id = ar.seller_role_id
        where ar.seller_account_id = a.seller_account_id
          and r.seller_id = a.seller_id
          and r.role_key = 'owner'
          and r.status = '0'
          and r.del_flag = '0'
      )
  ) then
    signal sqlstate '45000' set message_text = 'seller owner accounts must bind default owner role';
  end if;

  if not exists (
    select 1
    from seller_menu
    where perms = 'seller:product:schema:query'
      and parent_id = 0
      and menu_type = 'F'
      and coalesce(path, '') = ''
      and coalesce(component, '') = ''
      and coalesce(route_name, '') = ''
  ) then
    signal sqlstate '45000' set message_text = 'seller product schema permission was not created';
  end if;

end//

delimiter ;

call assert_terminal_menu_range_ready();

start transaction;

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
        and r.status = '0'
        and r.del_flag = '0'
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

call assert_seller_product_schema_permission_seed_completed();
commit;

drop procedure if exists assert_seller_menu_permission_slot;
drop procedure if exists assert_terminal_menu_range_ready;
drop procedure if exists assert_seller_product_schema_permission_seed_completed;
