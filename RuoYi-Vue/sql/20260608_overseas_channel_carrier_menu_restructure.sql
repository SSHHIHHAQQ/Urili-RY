-- Overseas warehouse service channel/carrier menu restructure.
-- Scope:
-- 1. Add "物流商管理" under "海外仓服务设置" immediately after "报价方案".
-- 2. Move "系统渠道管理" and "客户渠道管理" from the legacy "渠道管理" root into "海外仓服务设置".
-- 3. Delete the now-empty legacy "渠道管理" root menu.
-- No business table, API, button permission, or real page is created here.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_overseas_channel_carrier_menu_restructure := coalesce(@confirm_overseas_channel_carrier_menu_restructure, '');
set @overseas_channel_legacy_role_menu_expected_delete_count :=
    coalesce(@overseas_channel_legacy_role_menu_expected_delete_count, '');
set @overseas_channel_legacy_menu_expected_delete_count :=
    coalesce(@overseas_channel_legacy_menu_expected_delete_count, '');
set @overseas_channel_restructure_menu_expected_target_count :=
    coalesce(@overseas_channel_restructure_menu_expected_target_count, '');
set @overseas_channel_legacy_role_menu_expected_signature :=
    coalesce(@overseas_channel_legacy_role_menu_expected_signature, '');
set @overseas_channel_legacy_menu_expected_signature :=
    coalesce(@overseas_channel_legacy_menu_expected_signature, '');
set @overseas_channel_restructure_menu_expected_target_signature :=
    coalesce(@overseas_channel_restructure_menu_expected_target_signature, '');

delimiter //

drop procedure if exists assert_overseas_channel_carrier_menu_confirmed//
create procedure assert_overseas_channel_carrier_menu_confirmed()
begin
  if coalesce(@confirm_overseas_channel_carrier_menu_restructure, '')
      <> 'APPLY_OVERSEAS_CHANNEL_CARRIER_MENU_RESTRUCTURE' then
    signal sqlstate '45000' set message_text = 'set @confirm_overseas_channel_carrier_menu_restructure = APPLY_OVERSEAS_CHANNEL_CARRIER_MENU_RESTRUCTURE before running this migration';
  end if;

  if coalesce(@overseas_channel_legacy_role_menu_expected_delete_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @overseas_channel_legacy_role_menu_expected_delete_count after previewing exact sys_role_menu rows';
  end if;

  if coalesce(@overseas_channel_legacy_menu_expected_delete_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @overseas_channel_legacy_menu_expected_delete_count after previewing exact sys_menu rows';
  end if;

  if coalesce(@overseas_channel_restructure_menu_expected_target_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @overseas_channel_restructure_menu_expected_target_count after previewing exact sys_menu restructure rows';
  end if;

  if coalesce(@overseas_channel_legacy_role_menu_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @overseas_channel_legacy_role_menu_expected_signature after previewing exact sys_role_menu rows';
  end if;

  if coalesce(@overseas_channel_legacy_menu_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @overseas_channel_legacy_menu_expected_signature after previewing exact sys_menu rows';
  end if;

  if coalesce(@overseas_channel_restructure_menu_expected_target_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @overseas_channel_restructure_menu_expected_target_signature after previewing exact sys_menu restructure rows';
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

drop procedure if exists assert_legacy_channel_cleanup_targets//
create procedure assert_legacy_channel_cleanup_targets()
begin
  declare v_role_menu_count int default 0;
  declare v_menu_count int default 0;
  declare v_role_menu_signature varchar(64) default '';
  declare v_menu_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':', rm.role_id, rm.menu_id)
           order by rm.role_id, rm.menu_id separator '|'
         ), ''), 256)
    into v_role_menu_count, v_role_menu_signature
  from sys_role_menu rm
  where rm.menu_id = 2040;

  if v_role_menu_count <> cast(@overseas_channel_legacy_role_menu_expected_delete_count as unsigned) then
    signal sqlstate '45000' set message_text = 'overseas channel legacy role-menu cleanup expected count does not match target rows';
  end if;

  if lower(v_role_menu_signature) <> lower(@overseas_channel_legacy_role_menu_expected_signature) then
    signal sqlstate '45000' set message_text = 'overseas channel legacy role-menu exact target signature mismatch';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             m.menu_id,
             coalesce(m.parent_id, ''),
             coalesce(m.menu_name, ''),
             coalesce(m.path, ''),
             coalesce(m.component, ''),
             coalesce(m.route_name, ''),
             coalesce(m.perms, ''),
             coalesce(m.menu_type, '')
           )
           order by m.menu_id separator '|'
         ), ''), 256)
    into v_menu_count, v_menu_signature
  from sys_menu m
  where m.menu_id = 2040;

  if v_menu_count <> cast(@overseas_channel_legacy_menu_expected_delete_count as unsigned) then
    signal sqlstate '45000' set message_text = 'overseas channel legacy menu cleanup expected count does not match target rows';
  end if;

  if lower(v_menu_signature) <> lower(@overseas_channel_legacy_menu_expected_signature) then
    signal sqlstate '45000' set message_text = 'overseas channel legacy menu exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_overseas_channel_restructure_targets//
create procedure assert_overseas_channel_restructure_targets()
begin
  declare v_menu_count int default 0;
  declare v_menu_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             m.menu_id,
             coalesce(m.parent_id, ''),
             coalesce(m.menu_name, ''),
             coalesce(m.order_num, ''),
             coalesce(m.path, ''),
             coalesce(m.component, ''),
             coalesce(m.route_name, ''),
             coalesce(m.perms, ''),
             coalesce(m.menu_type, '')
           )
           order by m.menu_id separator '|'
         ), ''), 256)
    into v_menu_count, v_menu_signature
  from sys_menu m
  where m.menu_id in (2031, 2041, 2042, 2054);

  if v_menu_count <> cast(@overseas_channel_restructure_menu_expected_target_count as unsigned) then
    signal sqlstate '45000' set message_text = 'overseas channel restructure menu expected count does not match target rows';
  end if;

  if lower(v_menu_signature) <> lower(@overseas_channel_restructure_menu_expected_target_signature) then
    signal sqlstate '45000' set message_text = 'overseas channel restructure menu exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_overseas_channel_restructure_completed//
create procedure assert_overseas_channel_restructure_completed()
begin
  if exists (select 1 from sys_role_menu where menu_id = 2040) then
    signal sqlstate '45000' set message_text = 'legacy channel root role-menu rows still exist after cleanup';
  end if;

  if exists (select 1 from sys_menu where menu_id = 2040) then
    signal sqlstate '45000' set message_text = 'legacy channel root menu still exists after cleanup';
  end if;

  if exists (select 1 from sys_menu where parent_id = 2040) then
    signal sqlstate '45000' set message_text = 'legacy channel root still owns child menus after cleanup';
  end if;

  if not exists (
    select 1 from sys_menu
    where menu_id = 2054
      and parent_id = 2030
      and order_num = 4
      and path = 'logistics-carrier'
      and component = 'Common/PlannedPage/index'
      and route_name = 'LogisticsCarrier'
      and perms = 'logistics:carrier:list'
      and menu_type = 'C'
  ) then
    signal sqlstate '45000' set message_text = 'logistics carrier menu 2054 was not created with expected signature';
  end if;

  if not exists (
    select 1 from sys_menu
    where menu_id = 2041
      and parent_id = 2030
      and order_num = 5
      and path = 'channel-system'
      and component = 'Channel/System/index'
      and route_name = 'ChannelSystem'
      and perms = 'channel:system:list'
      and menu_type = 'C'
  ) then
    signal sqlstate '45000' set message_text = 'system channel menu 2041 was not restructured with expected signature';
  end if;

  if not exists (
    select 1 from sys_menu
    where menu_id = 2042
      and parent_id = 2030
      and order_num = 6
      and path = 'channel-customer'
      and component = 'Channel/Customer/index'
      and route_name = 'ChannelCustomer'
      and perms = 'channel:customer:list'
      and menu_type = 'C'
  ) then
    signal sqlstate '45000' set message_text = 'customer channel menu 2042 was not restructured with expected signature';
  end if;

  if not exists (
    select 1 from sys_menu
    where menu_id = 2031
      and parent_id = 2030
      and order_num = 7
      and menu_type = 'C'
  ) then
    signal sqlstate '45000' set message_text = 'upstream system menu 2031 was not reordered with expected signature';
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
call assert_overseas_channel_restructure_targets();

start transaction;

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
call assert_legacy_channel_cleanup_targets();

select rm.role_id, rm.menu_id
from sys_role_menu rm
where rm.menu_id = 2040
order by rm.role_id, rm.menu_id;

select m.menu_id, m.parent_id, m.menu_name, m.path, m.component, m.route_name, m.perms, m.menu_type
from sys_menu m
where m.menu_id = 2040
order by m.menu_id;

delete rm
from sys_role_menu rm
where rm.menu_id = 2040;

delete m
from sys_menu m
where m.menu_id = 2040
  and m.parent_id = 0
  and m.menu_name = '渠道管理'
  and coalesce(m.path, '') in ('urili-channel', 'channel')
  and coalesce(m.component, '') = ''
  and coalesce(m.route_name, '') in ('', 'Channel')
  and coalesce(m.perms, '') = ''
  and coalesce(m.menu_type, '') = 'M';

call assert_overseas_channel_restructure_completed();

commit;

drop temporary table if exists tmp_overseas_channel_menu_guard;
drop procedure if exists assert_overseas_channel_parent_ready;
drop procedure if exists assert_overseas_channel_menu_guard;
drop procedure if exists assert_legacy_channel_root_empty;
drop procedure if exists assert_legacy_channel_cleanup_targets;
drop procedure if exists assert_overseas_channel_restructure_targets;
drop procedure if exists assert_overseas_channel_restructure_completed;
