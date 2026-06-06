-- Portal dept/role read-only list permission seed.
-- Purpose:
-- 1. Add seller/buyer terminal permissions for /seller/depts, /seller/roles, /buyer/depts and /buyer/roles.
-- 2. Bind these permissions to current active terminal roles.
-- This script is idempotent and does not create or change table structures.

set names utf8mb4;
set @confirm_portal_dept_role_list_permission_seed := coalesce(@confirm_portal_dept_role_list_permission_seed, '');

delimiter //

drop procedure if exists assert_portal_dept_role_list_permission_seed_confirmed//
create procedure assert_portal_dept_role_list_permission_seed_confirmed()
begin
  if coalesce(@confirm_portal_dept_role_list_permission_seed, '')
      <> 'APPLY_PORTAL_DEPT_ROLE_LIST_PERMISSION_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_portal_dept_role_list_permission_seed = APPLY_PORTAL_DEPT_ROLE_LIST_PERMISSION_SEED before running this seed';
  end if;
end//

delimiter ;

call assert_portal_dept_role_list_permission_seed_confirmed();
drop procedure if exists assert_portal_dept_role_list_permission_seed_confirmed;

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '部门列表', 0, 40, '', null, '', '',
    1, 0, 'F', '0', '0', 'seller:dept:list', '#', 'admin',
    sysdate(), '', null, '卖家端部门只读列表权限'
where not exists (
    select 1 from seller_menu where perms = 'seller:dept:list'
);

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '角色列表', 0, 41, '', null, '', '',
    1, 0, 'F', '0', '0', 'seller:role:list', '#', 'admin',
    sysdate(), '', null, '卖家端角色只读列表权限'
where not exists (
    select 1 from seller_menu where perms = 'seller:role:list'
);

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '部门列表', 0, 40, '', null, '', '',
    1, 0, 'F', '0', '0', 'buyer:dept:list', '#', 'admin',
    sysdate(), '', null, '买家端部门只读列表权限'
where not exists (
    select 1 from buyer_menu where perms = 'buyer:dept:list'
);

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '角色列表', 0, 41, '', null, '', '',
    1, 0, 'F', '0', '0', 'buyer:role:list', '#', 'admin',
    sysdate(), '', null, '买家端角色只读列表权限'
where not exists (
    select 1 from buyer_menu where perms = 'buyer:role:list'
);

insert into seller_role_menu (seller_role_id, seller_menu_id)
select r.seller_role_id, m.seller_menu_id
from seller_role r
join seller_menu m on m.perms in ('seller:dept:list', 'seller:role:list')
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
join buyer_menu m on m.perms in ('buyer:dept:list', 'buyer:role:list')
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and not exists (
      select 1
      from buyer_role_menu rm
      where rm.buyer_role_id = r.buyer_role_id
        and rm.buyer_menu_id = m.buyer_menu_id
  );
