-- Buyer portal product schema query permission seed
-- Scope:
-- 1. Ensure current active buyers have a default owner role.
-- 2. Bind current OWNER buyer accounts to the default owner role.
-- 3. Add buyer:product:schema:query as a hidden button permission.
-- 4. Grant the permission to current active buyer roles.

insert into buyer_role
    (buyer_id, role_name, role_key, role_sort, status, del_flag,
     create_by, create_time, update_by, update_time, remark)
select b.buyer_id, 'Owner', 'owner', 1, '0', '0',
       'admin', sysdate(), '', null, 'Default buyer portal owner role'
from buyer b
where b.status = '0'
  and not exists (
      select 1
      from buyer_role r
      where r.buyer_id = b.buyer_id
        and r.role_key = 'owner'
        and r.del_flag = '0'
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

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    'Product Schema Query', 0, 50, '', null, '', '',
    1, 0, 'F', '0', '0', 'buyer:product:schema:query', '#', 'admin',
    sysdate(), '', null, 'Buyer portal product schema read permission'
where not exists (
    select 1 from buyer_menu where perms = 'buyer:product:schema:query'
);

insert into buyer_role_menu (buyer_role_id, buyer_menu_id)
select r.buyer_role_id, m.buyer_menu_id
from buyer_role r
join buyer_menu m on m.perms = 'buyer:product:schema:query'
where r.del_flag = '0'
  and r.status = '0'
  and not exists (
      select 1
      from buyer_role_menu rm
      where rm.buyer_role_id = r.buyer_role_id
        and rm.buyer_menu_id = m.buyer_menu_id
  );
