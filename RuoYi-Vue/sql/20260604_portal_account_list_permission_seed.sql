-- 端内账号只读列表权限 seed
-- 目标：
-- 1. 为现有卖家/买家主体补齐默认端内 Owner 角色。
-- 2. 将现有 OWNER 账号绑定到默认端内 Owner 角色。
-- 3. 补齐 /seller/accounts 与 /buyer/accounts 所需端内权限，并授予当前已有启用端内角色。

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

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '账号列表', 0, 30, '', null, '', '',
    1, 0, 'F', '0', '0', 'seller:account:list', '#', 'admin',
    sysdate(), '', null, '卖家端账号只读列表权限'
where not exists (
    select 1 from seller_menu where perms = 'seller:account:list'
);

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    '账号列表', 0, 30, '', null, '', '',
    1, 0, 'F', '0', '0', 'buyer:account:list', '#', 'admin',
    sysdate(), '', null, '买家端账号只读列表权限'
where not exists (
    select 1 from buyer_menu where perms = 'buyer:account:list'
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

insert into buyer_account_role (buyer_account_id, buyer_role_id)
select a.buyer_account_id, r.buyer_role_id
from buyer_account a
join buyer_role r on r.buyer_id = a.buyer_id
                 and r.role_key = 'owner'
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
join seller_menu m on m.perms = 'seller:account:list'
where r.del_flag = '0'
  and r.status = '0'
  and not exists (
      select 1
      from seller_role_menu rm
      where rm.seller_role_id = r.seller_role_id
        and rm.seller_menu_id = m.seller_menu_id
  );

insert into buyer_role_menu (buyer_role_id, buyer_menu_id)
select r.buyer_role_id, m.buyer_menu_id
from buyer_role r
join buyer_menu m on m.perms = 'buyer:account:list'
where r.del_flag = '0'
  and r.status = '0'
  and not exists (
      select 1
      from buyer_role_menu rm
      where rm.buyer_role_id = r.buyer_role_id
        and rm.buyer_menu_id = m.buyer_menu_id
  );
