-- Buyer product center terminal menu seed
-- Scope:
-- 1. Add buyer portal top-level product center C menu.
-- 2. Add buyer product center query F permission under the product center menu.
-- 3. Grant product center list/query permissions to current active buyer OWNER roles.
-- 4. Keep buyer product center permissions independent from sys_menu and seller_menu.

set names utf8mb4;
set @confirm_buyer_product_center_menu_seed := coalesce(@confirm_buyer_product_center_menu_seed, '');

delimiter //

drop procedure if exists assert_buyer_product_center_menu_seed_confirmed//
create procedure assert_buyer_product_center_menu_seed_confirmed()
begin
  if coalesce(@confirm_buyer_product_center_menu_seed, '')
      <> 'APPLY_BUYER_PRODUCT_CENTER_MENU_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_buyer_product_center_menu_seed = APPLY_BUYER_PRODUCT_CENTER_MENU_SEED before running this seed';
  end if;
end//

drop procedure if exists assert_buyer_product_center_menu_ready//
create procedure assert_buyer_product_center_menu_ready()
begin
  declare v_table_count int default 0;
  declare v_auto_increment bigint default 0;

  select count(1)
    into v_table_count
  from information_schema.tables
  where table_schema = database()
    and table_name = 'buyer_menu';

  if v_table_count = 0 then
    signal sqlstate '45000' set message_text = 'buyer_menu table is required before buyer product center seed inserts';
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
    signal sqlstate '45000' set message_text = 'buyer_menu auto_increment must be between 200000 and 299999 before buyer product center seed inserts';
  end if;

  if exists (
    select 1
    from buyer_menu
    where coalesce(perms, '') = ''
       or coalesce(perms, '') like '%*%'
       or coalesce(perms, '') not like 'buyer:%'
       or coalesce(perms, '') like 'buyer:admin:%'
       or coalesce(perms, '') like 'seller:%'
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
      select perms
      from buyer_menu
      group by perms
      having count(1) > 1
    ) duplicate_buyer_menu_perms
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu perms must be unique before buyer product center grants';
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

drop procedure if exists assert_buyer_product_center_seed_completed//
create procedure assert_buyer_product_center_seed_completed()
begin
  declare v_owner_role_count int default 0;
  declare v_grant_count int default 0;

  if not exists (
    select 1
    from buyer_menu
    where perms = 'buyer:product:center:list'
      and parent_id = 0
      and menu_type = 'C'
      and path = '/buyer/portal/product-center'
      and component = 'Buyer/ProductCenter/index'
      and route_name = 'BuyerProductCenter'
  ) then
    signal sqlstate '45000' set message_text = 'buyer product center page menu signature mismatch';
  end if;

  if not exists (
    select 1
    from buyer_menu child
    join buyer_menu parent on parent.buyer_menu_id = child.parent_id
    where parent.perms = 'buyer:product:center:list'
      and child.perms = 'buyer:product:center:query'
      and child.menu_type = 'F'
      and coalesce(child.path, '') = ''
      and coalesce(child.component, '') = ''
      and coalesce(child.route_name, '') = ''
  ) then
    signal sqlstate '45000' set message_text = 'buyer product center query button signature mismatch';
  end if;

  select count(1)
    into v_owner_role_count
  from buyer_role r
  where r.del_flag = '0'
    and r.status = '0'
    and r.role_key = 'owner';

  select count(1)
    into v_grant_count
  from buyer_role r
  join buyer_role_menu rm on rm.buyer_role_id = r.buyer_role_id
  join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
  where r.del_flag = '0'
    and r.status = '0'
    and r.role_key = 'owner'
    and m.perms in ('buyer:product:center:list', 'buyer:product:center:query');

  if v_grant_count <> v_owner_role_count * 2 then
    signal sqlstate '45000' set message_text = 'buyer owner roles product center permission exact grant count mismatch';
  end if;
end//

delimiter ;

call assert_buyer_product_center_menu_seed_confirmed();
call assert_buyer_product_center_menu_ready();

call assert_buyer_menu_permission_slot('buyer:product:center:list', 0, 'C', '/buyer/portal/product-center', 'Buyer/ProductCenter/index', 'BuyerProductCenter',
    'buyer:product:center:list menu slot is occupied by another signature');

start transaction;

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '商品中心', 0, 10, '/buyer/portal/product-center', 'Buyer/ProductCenter/index', '', 'BuyerProductCenter',
    1, 0, 'C', '0', '0', 'buyer:product:center:list', 'shopping', 'admin',
    sysdate(), '', null, '买家端商品中心顶级菜单'
where not exists (
    select 1 from buyer_menu where perms = 'buyer:product:center:list'
);

set @buyer_product_center_menu_id := (
  select buyer_menu_id
  from buyer_menu
  where perms = 'buyer:product:center:list'
);

call assert_buyer_menu_permission_slot('buyer:product:center:query', @buyer_product_center_menu_id, 'F', '', null, '',
    'buyer:product:center:query menu slot is occupied by another signature');

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '商品中心查询', @buyer_product_center_menu_id, 1, '', null, '', '',
    1, 0, 'F', '0', '0', 'buyer:product:center:query', '#', 'admin',
    sysdate(), '', null, '买家端商品中心详情查询权限'
where not exists (
    select 1 from buyer_menu where perms = 'buyer:product:center:query'
);

insert into buyer_role_menu (buyer_role_id, buyer_menu_id)
select r.buyer_role_id, m.buyer_menu_id
from buyer_role r
join buyer_menu m on m.perms in ('buyer:product:center:list', 'buyer:product:center:query')
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and (
      (m.perms = 'buyer:product:center:list'
        and m.parent_id = 0
        and coalesce(m.menu_type, '') = 'C'
        and coalesce(m.component, '') = 'Buyer/ProductCenter/index')
      or (m.perms = 'buyer:product:center:query'
        and m.parent_id = @buyer_product_center_menu_id
        and coalesce(m.menu_type, '') = 'F'
        and coalesce(m.component, '') = '')
  )
  and not exists (
      select 1
      from buyer_role_menu rm
      where rm.buyer_role_id = r.buyer_role_id
        and rm.buyer_menu_id = m.buyer_menu_id
  );

call assert_buyer_product_center_seed_completed();
commit;

drop procedure if exists assert_buyer_product_center_menu_seed_confirmed;
drop procedure if exists assert_buyer_product_center_menu_ready;
drop procedure if exists assert_buyer_menu_permission_slot;
drop procedure if exists assert_buyer_product_center_seed_completed;
