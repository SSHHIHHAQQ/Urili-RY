-- Seller portal product schema query permission seed
-- Scope:
-- 1. Ensure current active sellers have a default owner role.
-- 2. Bind current OWNER seller accounts to the default owner role.
-- 3. Add seller:product:schema:query as a hidden button permission.
-- 4. Grant the permission to current active seller roles.

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
where r.del_flag = '0'
  and r.status = '0'
  and not exists (
      select 1
      from seller_role_menu rm
      where rm.seller_role_id = r.seller_role_id
        and rm.seller_menu_id = m.seller_menu_id
  );
