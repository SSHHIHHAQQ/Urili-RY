-- Overseas warehouse service channel/carrier menu restructure.
-- Scope:
-- 1. Add "物流商管理" under "海外仓服务设置" immediately after "报价方案".
-- 2. Move "系统渠道管理" and "客户渠道管理" from the legacy "渠道管理" root into "海外仓服务设置".
-- 3. Delete the now-empty legacy "渠道管理" root menu.
-- No business table, API, button permission, or real page is created here.

set names utf8mb4;

set @confirm_overseas_channel_carrier_menu_restructure := coalesce(@confirm_overseas_channel_carrier_menu_restructure, '');

delimiter //

drop procedure if exists assert_overseas_channel_carrier_menu_confirmed//
create procedure assert_overseas_channel_carrier_menu_confirmed()
begin
  if coalesce(@confirm_overseas_channel_carrier_menu_restructure, '')
      <> 'APPLY_OVERSEAS_CHANNEL_CARRIER_MENU_RESTRUCTURE' then
    signal sqlstate '45000' set message_text = 'set @confirm_overseas_channel_carrier_menu_restructure = APPLY_OVERSEAS_CHANNEL_CARRIER_MENU_RESTRUCTURE before running this migration';
  end if;
end//

drop procedure if exists assert_overseas_channel_parent_ready//
create procedure assert_overseas_channel_parent_ready()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2030
      and menu_name = '海外仓服务设置'
      and parent_id = 0
      and path = 'overseas-warehouse-service'
      and route_name = 'OverseasWarehouseServiceManagement'
      and menu_type = 'M'
  ) then
    signal sqlstate '45000' set message_text = 'overseas warehouse service parent menu 2030 is required before channel/carrier menu restructure';
  end if;
end//

drop procedure if exists assert_overseas_channel_menu_guard//
create procedure assert_overseas_channel_menu_guard()
begin
  if exists (
    select 1
    from sys_menu m
    left join tmp_overseas_channel_menu_guard seed
      on seed.menu_id = m.menu_id
     and m.parent_id = seed.parent_id
     and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
    where m.menu_id in (2041, 2042, 2054)
      and seed.menu_id is null
  ) then
    signal sqlstate '45000' set message_text = 'overseas channel/carrier sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    where m.menu_id not in (2041, 2042, 2054)
      and m.parent_id = 2030
      and (
        coalesce(m.path, '') in ('channel-system', 'channel-customer', 'logistics-carrier')
        or coalesce(m.perms, '') in ('channel:system:list', 'channel:customer:list', 'logistics:carrier:list')
      )
  ) then
    signal sqlstate '45000' set message_text = 'overseas channel/carrier target signature is already used by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    where m.menu_id = 2040
      and not (
        m.parent_id = 0
        and m.menu_name = '渠道管理'
        and coalesce(m.path, '') in ('urili-channel', 'channel')
        and coalesce(m.component, '') = ''
        and coalesce(m.route_name, '') in ('', 'Channel')
        and coalesce(m.perms, '') = ''
        and coalesce(m.menu_type, '') = 'M'
      )
  ) then
    signal sqlstate '45000' set message_text = 'legacy channel root 2040 is not the expected removable menu';
  end if;
end//

drop procedure if exists assert_legacy_channel_root_empty//
create procedure assert_legacy_channel_root_empty()
begin
  if exists (
    select 1
    from sys_menu
    where parent_id = 2040
  ) then
    signal sqlstate '45000' set message_text = 'legacy channel root 2040 still has children after re-parenting';
  end if;
end//

delimiter ;

call assert_overseas_channel_carrier_menu_confirmed();
drop procedure if exists assert_overseas_channel_carrier_menu_confirmed;

create temporary table if not exists tmp_overseas_channel_menu_guard (
  menu_id    bigint       not null,
  parent_id  bigint       not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  key idx_overseas_channel_menu_guard_id (menu_id)
) engine=memory;

truncate table tmp_overseas_channel_menu_guard;

insert into tmp_overseas_channel_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (2041, 2040, 'C', 'channel-system', 'Channel/System/index', 'ChannelSystem', 'channel:system:list'),
    (2041, 2030, 'C', 'channel-system', 'Channel/System/index', 'ChannelSystem', 'channel:system:list'),
    (2042, 2040, 'C', 'channel-customer', 'Channel/Customer/index', 'ChannelCustomer', 'channel:customer:list'),
    (2042, 2030, 'C', 'channel-customer', 'Channel/Customer/index', 'ChannelCustomer', 'channel:customer:list'),
    (2054, 2030, 'C', 'logistics-carrier', 'Common/PlannedPage/index', 'LogisticsCarrier', 'logistics:carrier:list');

call assert_overseas_channel_parent_ready();
call assert_overseas_channel_menu_guard();

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2054, '物流商管理', 2030, 4, 'logistics-carrier', 'Common/PlannedPage/index', '', 'LogisticsCarrier',
     1, 0, 'C', '0', '0', 'logistics:carrier:list', 'TruckOutlined', 'admin',
     sysdate(), '', null, '海外仓服务设置菜单：物流商管理，占位入口'),
    (2041, '系统渠道管理', 2030, 5, 'channel-system', 'Channel/System/index', '', 'ChannelSystem',
     1, 0, 'C', '0', '0', 'channel:system:list', 'tree', 'admin',
     sysdate(), '', null, '海外仓服务设置菜单：系统渠道管理，从历史渠道管理目录迁入'),
    (2042, '客户渠道管理', 2030, 6, 'channel-customer', 'Channel/Customer/index', '', 'ChannelCustomer',
     1, 0, 'C', '0', '0', 'channel:customer:list', 'tree', 'admin',
     sysdate(), '', null, '海外仓服务设置菜单：客户渠道管理，从历史渠道管理目录迁入')
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

update sys_menu
set order_num = 7,
    update_by = 'admin',
    update_time = sysdate(),
    remark = '领星主仓接入、同步清单和配对管理'
where menu_id = 2031
  and parent_id = 2030
  and menu_type = 'C';

call assert_legacy_channel_root_empty();

delete from sys_role_menu
where menu_id = 2040;

delete from sys_menu
where menu_id = 2040;

drop temporary table if exists tmp_overseas_channel_menu_guard;
drop procedure if exists assert_overseas_channel_parent_ready;
drop procedure if exists assert_overseas_channel_menu_guard;
drop procedure if exists assert_legacy_channel_root_empty;
