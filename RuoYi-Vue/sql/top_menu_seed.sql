-- Top-level menu seed for the RuoYi validation project.
-- Scope: top-level sys_menu directories only. No page, API, button permission, or business table is created here.

set names utf8mb4;

set @confirm_top_menu_seed := coalesce(@confirm_top_menu_seed, '');

delimiter //

drop procedure if exists assert_top_menu_seed_confirmed//
create procedure assert_top_menu_seed_confirmed()
begin
  if coalesce(@confirm_top_menu_seed, '')
     <> 'APPLY_TOP_MENU_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_top_menu_seed = APPLY_TOP_MENU_SEED before running this seed';
  end if;
end//

drop procedure if exists assert_top_menu_sys_menu_guard//
create procedure assert_top_menu_sys_menu_guard()
begin
  if exists (
    select 1
    from sys_menu m
    where exists (
        select 1
        from tmp_top_menu_sys_menu_guard seed
        where seed.menu_id = m.menu_id
    )
      and not exists (
        select 1
        from tmp_top_menu_sys_menu_guard seed
        where seed.menu_id = m.menu_id
          and coalesce(m.path, '') = coalesce(seed.path, '')
          and coalesce(m.parent_id, -1) = seed.parent_id
          and coalesce(m.menu_type, '') = seed.menu_type
          and coalesce(m.component, '') = coalesce(seed.component, '')
          and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
          and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'top menu sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_top_menu_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'top menu sys_menu signature is already used by another menu';
  end if;
end//

drop procedure if exists assert_top_menu_legacy_cleanup_guard//
create procedure assert_top_menu_legacy_cleanup_guard()
begin
  if exists (
    select 1
    from sys_menu m
    where exists (
        select 1
        from tmp_top_menu_legacy_cleanup_guard seed
        where seed.menu_id = m.menu_id
    )
      and not exists (
        select 1
        from tmp_top_menu_legacy_cleanup_guard seed
        where seed.menu_id = m.menu_id
          and (seed.parent_id is null or coalesce(m.parent_id, -1) = seed.parent_id)
          and (seed.menu_name is null or coalesce(m.menu_name, '') = coalesce(seed.menu_name, ''))
          and (seed.path is null or coalesce(m.path, '') = coalesce(seed.path, ''))
          and (seed.component is null or coalesce(m.component, '') = coalesce(seed.component, ''))
          and (seed.route_name is null or coalesce(m.route_name, '') = coalesce(seed.route_name, ''))
          and (seed.perms is null or coalesce(m.perms, '') = coalesce(seed.perms, ''))
          and (seed.menu_type is null or coalesce(m.menu_type, '') = coalesce(seed.menu_type, ''))
    )
  ) then
    signal sqlstate '45000' set message_text = 'top menu legacy cleanup sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu target
    join tmp_top_menu_legacy_cleanup_guard seed
      on seed.menu_id = target.menu_id
     and (seed.parent_id is null or coalesce(target.parent_id, -1) = seed.parent_id)
     and (seed.menu_name is null or coalesce(target.menu_name, '') = coalesce(seed.menu_name, ''))
     and (seed.path is null or coalesce(target.path, '') = coalesce(seed.path, ''))
     and (seed.component is null or coalesce(target.component, '') = coalesce(seed.component, ''))
     and (seed.route_name is null or coalesce(target.route_name, '') = coalesce(seed.route_name, ''))
     and (seed.perms is null or coalesce(target.perms, '') = coalesce(seed.perms, ''))
     and (seed.menu_type is null or coalesce(target.menu_type, '') = coalesce(seed.menu_type, ''))
    join sys_menu other
      on other.menu_id <> target.menu_id
     and (seed.parent_id is null or coalesce(other.parent_id, -1) = seed.parent_id)
     and (seed.menu_name is null or coalesce(other.menu_name, '') = coalesce(seed.menu_name, ''))
     and (seed.path is null or coalesce(other.path, '') = coalesce(seed.path, ''))
     and (seed.component is null or coalesce(other.component, '') = coalesce(seed.component, ''))
     and (seed.route_name is null or coalesce(other.route_name, '') = coalesce(seed.route_name, ''))
     and (seed.perms is null or coalesce(other.perms, '') = coalesce(seed.perms, ''))
     and (seed.menu_type is null or coalesce(other.menu_type, '') = coalesce(seed.menu_type, ''))
  ) then
    signal sqlstate '45000' set message_text = 'top menu legacy cleanup sys_menu signature is already used by another menu';
  end if;
end//

delimiter ;

call assert_top_menu_seed_confirmed();
drop procedure if exists assert_top_menu_seed_confirmed;

create temporary table if not exists tmp_top_menu_sys_menu_guard (
  menu_id    bigint       not null,
  parent_id  bigint       not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  key idx_top_menu_sys_menu_guard_id (menu_id)
) engine=memory;

truncate table tmp_top_menu_sys_menu_guard;

insert into tmp_top_menu_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (1, 0, 'M', 'system', '', '', ''),
    (2, 0, 'M', 'monitor', '', '', ''),
    (3, 0, 'M', 'tool', '', '', ''),
    (108, 0, 'M', 'log-center', '', 'LogCenter', ''),
    (108, 0, 'M', 'log', '', '', ''),
    (2010, 0, 'M', 'partner', '', 'PartnerManagement', ''),
    (2020, 0, 'M', 'warehouse', '', 'WarehouseManagement', ''),
    (2030, 0, 'M', 'overseas-warehouse-service', '', 'OverseasWarehouseServiceManagement', ''),
    (2050, 0, 'M', 'finance', '', 'FinanceManagement', ''),
    (2060, 0, 'M', 'product', '', 'ProductManagement', ''),
    (2070, 0, 'M', 'order', '', 'OrderManagement', ''),
    (2080, 0, 'M', 'inventory', '', 'InventoryManagement', ''),
    (2090, 0, 'M', 'basic-config', '', 'BasicConfig', ''),
    (2100, 0, 'M', 'review-center', '', 'ReviewCenter', '');

create temporary table if not exists tmp_top_menu_legacy_cleanup_guard (
  menu_id    bigint       not null,
  parent_id  bigint       default null,
  menu_name  varchar(50)  default null,
  path       varchar(200) default null,
  component  varchar(255) default null,
  route_name varchar(50)  default null,
  perms      varchar(100) default null,
  menu_type  char(1)      default null,
  key idx_top_menu_legacy_cleanup_guard_id (menu_id)
) engine=memory;

truncate table tmp_top_menu_legacy_cleanup_guard;

insert into tmp_top_menu_legacy_cleanup_guard
    (menu_id, parent_id, menu_name, path, component, route_name, perms, menu_type)
values
    (2040, 0, '渠道管理', 'urili-channel', '', '', '', 'M'),
    (2040, 0, '渠道管理', 'channel', '', '', '', 'M'),
    (2000, 0, 'URILI运营后台', null, null, null, '', 'M');

call assert_top_menu_sys_menu_guard();

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2010, '主体管理', 0, 5, 'partner', null, '', 'PartnerManagement',
     1, 0, 'M', '0', '0', '', 'TeamOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：主体管理'),
    (2060, '商品管理', 0, 10, 'product', null, '', 'ProductManagement',
     1, 0, 'M', '0', '0', '', 'ShoppingOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：商品管理'),
    (2070, '订单管理', 0, 15, 'order', null, '', 'OrderManagement',
     1, 0, 'M', '0', '0', '', 'OrderedListOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：订单管理'),
    (2080, '库存管理', 0, 20, 'inventory', null, '', 'InventoryManagement',
     1, 0, 'M', '0', '0', '', 'StockOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：库存管理'),
    (2020, '仓库管理', 0, 25, 'warehouse', null, '', 'WarehouseManagement',
     1, 0, 'M', '0', '0', '', 'HomeOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：仓库管理，复用原仓储管理菜单位'),
    (2030, '海外仓服务设置', 0, 30, 'overseas-warehouse-service', null, '', 'OverseasWarehouseServiceManagement',
     1, 0, 'M', '0', '0', '', 'GlobalOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：海外仓服务设置，复用原上游系统菜单位'),
    (2050, '财务管理', 0, 35, 'finance', null, '', 'FinanceManagement',
     1, 0, 'M', '0', '0', '', 'AccountBookOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：财务管理，复用原计费管理菜单位'),
    (2090, '基础配置', 0, 40, 'basic-config', null, '', 'BasicConfig',
     1, 0, 'M', '0', '0', '', 'SettingOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：基础配置'),
    (2100, '审核中心', 0, 45, 'review-center', null, '', 'ReviewCenter',
     1, 0, 'M', '0', '0', '', 'AuditOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：审核中心'),
    (108, '日志中心', 0, 50, 'log-center', null, '', 'LogCenter',
     1, 0, 'M', '0', '0', '', 'FileTextOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：日志中心，复用若依原日志管理目录'),
    (3, '工具中心', 0, 55, 'tool', null, '', '',
     1, 0, 'M', '0', '0', '', 'ToolOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：工具中心，复用若依原系统工具目录')
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

-- Keep RuoYi native admin/monitor entries after the requested top-level menu order.
update sys_menu
set order_num = 90,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 1;

update sys_menu
set order_num = 95,
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2;

-- The previous channel draft is not part of the requested top-level menu list.
-- Keep it for reference, but disable it so it does not appear as an active top-level menu.
call assert_top_menu_legacy_cleanup_guard();

update sys_menu
set order_num = 80,
    visible = '1',
    status = '1',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '本轮顶级菜单口径未纳入，保留历史草案'
where menu_id = 2040;

-- If an earlier seed created the old wrapper root, keep it inactive.
update sys_menu
set visible = '1',
    status = '1',
    order_num = 100,
    update_by = 'admin',
    update_time = sysdate(),
    remark = '已由独立顶级菜单替代，保留历史草案'
where menu_id = 2000;

drop temporary table if exists tmp_top_menu_sys_menu_guard;
drop temporary table if exists tmp_top_menu_legacy_cleanup_guard;
drop procedure if exists assert_top_menu_sys_menu_guard;
drop procedure if exists assert_top_menu_legacy_cleanup_guard;
