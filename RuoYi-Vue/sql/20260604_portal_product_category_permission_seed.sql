-- Seller/buyer portal product category list permission seed
-- Scope:
-- 1. Add seller:product:category:list and buyer:product:category:list as hidden button permissions.
-- 2. Grant the permissions to current active seller/buyer roles.

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    'Product Category List', 0, 40, '', null, '', '',
    1, 0, 'F', '0', '0', 'seller:product:category:list', '#', 'admin',
    sysdate(), '', null, 'Seller portal product category list permission'
where not exists (
    select 1 from seller_menu where perms = 'seller:product:category:list'
);

insert into seller_role_menu (seller_role_id, seller_menu_id)
select r.seller_role_id, m.seller_menu_id
from seller_role r
join seller_menu m on m.perms = 'seller:product:category:list'
where r.del_flag = '0'
  and r.status = '0'
  and not exists (
      select 1
      from seller_role_menu rm
      where rm.seller_role_id = r.seller_role_id
        and rm.seller_menu_id = m.seller_menu_id
  );

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select
    'Product Category List', 0, 40, '', null, '', '',
    1, 0, 'F', '0', '0', 'buyer:product:category:list', '#', 'admin',
    sysdate(), '', null, 'Buyer portal product category list permission'
where not exists (
    select 1 from buyer_menu where perms = 'buyer:product:category:list'
);

insert into buyer_role_menu (buyer_role_id, buyer_menu_id)
select r.buyer_role_id, m.buyer_menu_id
from buyer_role r
join buyer_menu m on m.perms = 'buyer:product:category:list'
where r.del_flag = '0'
  and r.status = '0'
  and not exists (
      select 1
      from buyer_role_menu rm
      where rm.buyer_role_id = r.buyer_role_id
        and rm.buyer_menu_id = m.buyer_menu_id
  );
