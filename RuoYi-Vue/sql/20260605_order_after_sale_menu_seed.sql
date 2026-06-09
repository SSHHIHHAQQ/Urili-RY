-- Order after-sale menu seed.
-- Scope: add a second-level placeholder menu under "订单管理".
-- No order/after-sale business table, API, button permission, or real page is created here.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_order_after_sale_menu_seed := coalesce(@confirm_order_after_sale_menu_seed, '');
set @order_after_sale_menu_seed_expected_count := coalesce(@order_after_sale_menu_seed_expected_count, '');
set @order_after_sale_menu_seed_expected_signature := coalesce(@order_after_sale_menu_seed_expected_signature, '');

delimiter //

drop procedure if exists assert_order_after_sale_menu_seed_confirmed//
create procedure assert_order_after_sale_menu_seed_confirmed()
begin
  if coalesce(@confirm_order_after_sale_menu_seed, '')
      <> 'APPLY_ORDER_AFTER_SALE_MENU_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_order_after_sale_menu_seed = APPLY_ORDER_AFTER_SALE_MENU_SEED before running this migration';
  end if;
  if coalesce(@order_after_sale_menu_seed_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @order_after_sale_menu_seed_expected_count after previewing exact order after-sale sys_menu rows';
  end if;
  if coalesce(@order_after_sale_menu_seed_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @order_after_sale_menu_seed_expected_signature after previewing exact order after-sale sys_menu rows';
  end if;
end//

drop procedure if exists assert_order_after_sale_sys_menu_guard//
create procedure assert_order_after_sale_sys_menu_guard()
begin
  if exists (
    select 1
    from sys_menu m
    where exists (
        select 1
        from tmp_order_after_sale_sys_menu_guard seed
        where seed.menu_id = m.menu_id
    )
      and not exists (
        select 1
        from tmp_order_after_sale_sys_menu_guard seed
        where seed.menu_id = m.menu_id
          and m.parent_id = seed.parent_id
          and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')
          and coalesce(m.path, '') = coalesce(seed.path, '')
          and coalesce(m.component, '') = coalesce(seed.component, '')
          and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
          and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'order after-sale sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_order_after_sale_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'order after-sale sys_menu signature is already used by another menu';
  end if;
end//

drop procedure if exists assert_order_after_sale_menu_seed_targets//
create procedure assert_order_after_sale_menu_seed_targets()
begin
  declare v_count bigint default 0;
  declare v_signature varchar(64) default '';

  select count(distinct m.menu_id),
         sha2(coalesce(group_concat(distinct
           concat_ws('|',
             m.menu_id,
             coalesce(m.menu_name, ''),
             coalesce(m.parent_id, ''),
             coalesce(m.order_num, ''),
             coalesce(m.path, ''),
             coalesce(m.component, ''),
             coalesce(m.query, ''),
             coalesce(m.route_name, ''),
             coalesce(m.is_frame, ''),
             coalesce(m.is_cache, ''),
             coalesce(m.menu_type, ''),
             coalesce(m.visible, ''),
             coalesce(m.status, ''),
             coalesce(m.perms, ''),
             coalesce(m.icon, ''),
             coalesce(m.remark, '')
           )
           order by m.menu_id separator '\n'
         ), ''), 256)
    into v_count, v_signature
  from sys_menu m
  join tmp_order_after_sale_sys_menu_guard seed
    on m.menu_id = seed.menu_id
    or (coalesce(seed.perms, '') <> '' and coalesce(m.perms, '') = coalesce(seed.perms, ''));

  if v_count <> cast(@order_after_sale_menu_seed_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'order after-sale sys_menu exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@order_after_sale_menu_seed_expected_signature) then
    signal sqlstate '45000' set message_text = 'order after-sale sys_menu exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_order_after_sale_menu_seed_confirmed();
drop procedure if exists assert_order_after_sale_menu_seed_confirmed;

create temporary table if not exists tmp_order_after_sale_sys_menu_guard (
  menu_id    bigint       not null,
  parent_id  bigint       not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  key idx_order_after_sale_sys_menu_guard_id (menu_id)
) engine=memory;

truncate table tmp_order_after_sale_sys_menu_guard;

insert into tmp_order_after_sale_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (2412, 2070, 'C', 'after-sale', 'Common/PlannedPage/index', 'AfterSaleManagement', 'order:afterSale:list');

call assert_order_after_sale_sys_menu_guard();
call assert_order_after_sale_menu_seed_targets();

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2412, '售后管理', 2070, 15, 'after-sale', 'Common/PlannedPage/index', '', 'AfterSaleManagement',
     1, 0, 'C', '0', '0', 'order:afterSale:list', 'IssuesCloseOutlined', 'admin',
     sysdate(), '', null, '订单管理菜单：售后管理，占位入口')
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

drop temporary table if exists tmp_order_after_sale_sys_menu_guard;
drop procedure if exists assert_order_after_sale_menu_seed_targets;
drop procedure if exists assert_order_after_sale_sys_menu_guard;
