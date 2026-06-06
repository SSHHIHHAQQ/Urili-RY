-- 端内本人审计自助权限 seed
-- 目标：
-- 1. 补齐 /seller/account/login-logs、/seller/account/oper-logs、/seller/account/sessions 的端内权限。
-- 2. 补齐 /buyer/account/login-logs、/buyer/account/oper-logs、/buyer/account/sessions 的端内权限。
-- 3. 将权限授予当前启用的端内 OWNER 角色，保持卖家/买家端内权限表独立于 sys_*。

set names utf8mb4;
set @confirm_portal_self_audit_permission_seed := coalesce(@confirm_portal_self_audit_permission_seed, '');

delimiter //

drop procedure if exists assert_portal_self_audit_permission_seed_confirmed//
create procedure assert_portal_self_audit_permission_seed_confirmed()
begin
  if coalesce(@confirm_portal_self_audit_permission_seed, '')
      <> 'APPLY_PORTAL_SELF_AUDIT_PERMISSION_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_portal_self_audit_permission_seed = APPLY_PORTAL_SELF_AUDIT_PERMISSION_SEED before running this seed';
  end if;
end//

delimiter ;

call assert_portal_self_audit_permission_seed_confirmed();
drop procedure if exists assert_portal_self_audit_permission_seed_confirmed;

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_name, 0, seed.order_num, '', null, '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select '登录日志' as menu_name, 31 as order_num, 'seller:account:loginLog:list' as perms, '卖家端本人登录日志只读权限' as remark
    union all select '操作日志', 32, 'seller:account:operLog:list', '卖家端本人操作日志只读权限'
    union all select '会话列表', 33, 'seller:account:session:list', '卖家端本人会话只读权限'
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
    select '登录日志' as menu_name, 31 as order_num, 'buyer:account:loginLog:list' as perms, '买家端本人登录日志只读权限' as remark
    union all select '操作日志', 32, 'buyer:account:operLog:list', '买家端本人操作日志只读权限'
    union all select '会话列表', 33, 'buyer:account:session:list', '买家端本人会话只读权限'
) seed
where not exists (
    select 1 from buyer_menu m where m.perms = seed.perms
);

insert into seller_role_menu (seller_role_id, seller_menu_id)
select r.seller_role_id, m.seller_menu_id
from seller_role r
join seller_menu m on m.perms in (
    'seller:account:loginLog:list',
    'seller:account:operLog:list',
    'seller:account:session:list'
)
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
    'buyer:account:loginLog:list',
    'buyer:account:operLog:list',
    'buyer:account:session:list'
)
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and not exists (
      select 1
      from buyer_role_menu rm
      where rm.buyer_role_id = r.buyer_role_id
        and rm.buyer_menu_id = m.buyer_menu_id
  );
