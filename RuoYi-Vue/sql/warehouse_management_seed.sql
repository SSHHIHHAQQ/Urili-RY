-- Warehouse management seed for the RuoYi validation project.
-- Scope: warehouse master tables, admin menu entries, and permissions.
-- Run after top_menu_seed.sql, seller_buyer_management_seed.sql, currency_configuration_seed.sql, and upstream_system_management_seed.sql.

set names utf8mb4;

set @confirm_warehouse_management_seed := coalesce(@confirm_warehouse_management_seed, '');

delimiter //

drop procedure if exists assert_warehouse_management_seed_confirmed//
create procedure assert_warehouse_management_seed_confirmed()
begin
  if coalesce(@confirm_warehouse_management_seed, '')
      <> 'APPLY_WAREHOUSE_MANAGEMENT_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_warehouse_management_seed = APPLY_WAREHOUSE_MANAGEMENT_SEED before running this seed';
  end if;
end//

drop procedure if exists assert_warehouse_management_sys_menu_guard//
create procedure assert_warehouse_management_sys_menu_guard()
begin
  declare v_parent_count int default 0;

  select count(1)
    into v_parent_count
  from sys_menu
  where menu_id = 2020
    and menu_name = '仓库管理'
    and parent_id = 0
    and path = 'warehouse'
    and route_name = 'WarehouseManagement'
    and menu_type = 'M';

  if v_parent_count <> 1 then
    signal sqlstate '45000' set message_text = 'warehouse management parent sys_menu 2020 is required before warehouse management seed';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_warehouse_management_sys_menu_guard seed on seed.menu_id = m.menu_id
    where m.parent_id <> seed.parent_id
       or coalesce(m.menu_type, '') <> coalesce(seed.menu_type, '')
       or coalesce(m.path, '') <> coalesce(seed.path, '')
       or coalesce(m.component, '') <> coalesce(seed.component, '')
       or coalesce(m.route_name, '') <> coalesce(seed.route_name, '')
       or coalesce(m.perms, '') <> coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'warehouse management sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_warehouse_management_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'warehouse management sys_menu signature is already used by another menu';
  end if;
end//

drop procedure if exists assert_warehouse_management_seed_completed//
create procedure assert_warehouse_management_seed_completed()
begin
  declare v_dict_type_count int default 0;
  declare v_dict_data_count int default 0;

  select count(1)
    into v_dict_type_count
  from sys_dict_type
  where status = '0'
    and dict_type = 'warehouse_kind';

  if v_dict_type_count <> 1 then
    signal sqlstate '45000' set message_text = 'warehouse management seed did not complete warehouse_kind dict type';
  end if;

  select count(1)
    into v_dict_data_count
  from sys_dict_data
  where status = '0'
    and dict_type = 'warehouse_kind'
    and dict_value in ('official', 'third_party');

  if v_dict_data_count <> 2 then
    signal sqlstate '45000' set message_text = 'warehouse management seed did not complete warehouse_kind dict data';
  end if;

  if exists (
    select 1
    from tmp_warehouse_management_sys_menu_guard seed
    where not exists (
      select 1
      from sys_menu m
      where m.menu_id = seed.menu_id
        and coalesce(m.parent_id, -1) = seed.parent_id
        and coalesce(m.menu_type, '') = seed.menu_type
        and coalesce(m.path, '') = coalesce(seed.path, '')
        and coalesce(m.component, '') = coalesce(seed.component, '')
        and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
        and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'warehouse management seed did not complete expected sys_menu state';
  end if;
end//

delimiter ;

call assert_warehouse_management_seed_confirmed();
drop procedure if exists assert_warehouse_management_seed_confirmed;

create table if not exists warehouse (
  warehouse_id          bigint(20)    not null auto_increment comment '仓库ID',
  warehouse_code        varchar(64)   not null                comment '系统仓库编码',
  warehouse_name        varchar(200)  not null                comment '仓库名称',
  warehouse_kind        varchar(32)   not null                comment '仓库类型：official官方仓 third_party第三方仓',
  country_code          varchar(32)   not null                comment '国家/地区代码',
  state_province        varchar(100)  not null default ''     comment '州/省',
  city                  varchar(100)  not null                comment '城市',
  postal_code           varchar(32)   not null                comment '邮编',
  address_line1         varchar(255)  not null                comment '地址1',
  address_line2         varchar(255)  default ''              comment '地址2',
  contact_name          varchar(100)  not null                comment '联系人',
  contact_phone         varchar(64)   default ''              comment '联系电话',
  contact_email         varchar(128)  not null                comment '联系邮箱',
  company_name          varchar(200)  default ''              comment '公司名称',
  settlement_currency   varchar(16)   not null                comment '结算币种',
  status                char(1)       not null default '0'    comment '状态：0正常 1停用',
  create_by             varchar(64)   default ''              comment '创建者',
  create_time           datetime                              comment '创建时间',
  update_by             varchar(64)   default ''              comment '更新者',
  update_time           datetime                              comment '更新时间',
  remark                varchar(500)  default ''              comment '备注',
  primary key (warehouse_id),
  unique key uk_warehouse_code (warehouse_code),
  key idx_warehouse_kind_status (warehouse_kind, status),
  key idx_warehouse_country_state_city (country_code, state_province, city),
  key idx_warehouse_name (warehouse_name),
  key idx_warehouse_currency (settlement_currency),
  key idx_warehouse_create_time (create_time)
) engine=innodb auto_increment=1 comment='仓库主数据表';

create table if not exists official_warehouse (
  warehouse_id bigint(20)   not null               comment '仓库ID',
  create_by    varchar(64)  default ''             comment '创建者',
  create_time  datetime                            comment '创建时间',
  update_by    varchar(64)  default ''             comment '更新者',
  update_time  datetime                            comment '更新时间',
  remark       varchar(500) default ''             comment '备注',
  primary key (warehouse_id)
) engine=innodb comment='官方仓扩展表';

create table if not exists third_party_warehouse (
  warehouse_id bigint(20)   not null               comment '仓库ID',
  seller_id    bigint(20)   not null               comment '归属卖家ID',
  create_by    varchar(64)  default ''             comment '创建者',
  create_time  datetime                            comment '创建时间',
  update_by    varchar(64)  default ''             comment '更新者',
  update_time  datetime                            comment '更新时间',
  remark       varchar(500) default ''             comment '备注',
  primary key (warehouse_id),
  key idx_third_party_warehouse_seller (seller_id)
) engine=innodb comment='第三方仓扩展表';

create temporary table if not exists tmp_warehouse_management_sys_menu_guard (
  menu_id bigint(20) not null,
  parent_id bigint(20) not null,
  menu_type char(1) not null,
  path varchar(200) not null default '',
  component varchar(255) not null default '',
  route_name varchar(255) not null default '',
  perms varchar(100) not null default '',
  key idx_warehouse_management_sys_menu_guard_id (menu_id)
) engine=memory;

truncate table tmp_warehouse_management_sys_menu_guard;

insert into tmp_warehouse_management_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (2021, 2020, 'C', 'official', 'Warehouse/Official/index', 'OfficialWarehouse', 'warehouse:official:list'),
    (2022, 2020, 'C', 'third-party', 'Warehouse/ThirdParty/index', 'ThirdPartyWarehouse', 'warehouse:thirdParty:list'),
    (202101, 2021, 'F', '#', '', '', 'warehouse:official:list'),
    (202102, 2021, 'F', '#', '', '', 'warehouse:official:add'),
    (202103, 2021, 'F', '#', '', '', 'warehouse:official:edit'),
    (202104, 2021, 'F', '#', '', '', 'warehouse:official:status'),
    (202105, 2021, 'F', '#', '', '', 'warehouse:official:sync'),
    (202201, 2022, 'F', '#', '', '', 'warehouse:thirdParty:list'),
    (202202, 2022, 'F', '#', '', '', 'warehouse:thirdParty:add'),
    (202203, 2022, 'F', '#', '', '', 'warehouse:thirdParty:edit'),
    (202204, 2022, 'F', '#', '', '', 'warehouse:thirdParty:status');

call assert_warehouse_management_sys_menu_guard();

start transaction;

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '仓库类型', 'warehouse_kind', '0', 'admin', sysdate(), '', null, '仓库类型'
where not exists (select 1 from sys_dict_type where dict_type = 'warehouse_kind');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'warehouse_kind', '', '', seed.is_default, '0', 'admin', sysdate(), '', null, '仓库类型'
from (
    select 1 as dict_sort, '官方仓库' as dict_label, 'official' as dict_value, 'Y' as is_default
    union all select 2, '第三方仓库', 'third_party', 'N'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'warehouse_kind' and d.dict_value = seed.dict_value
);

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2021, '官方仓库', 2020, 5, 'official', 'Warehouse/Official/index', '', 'OfficialWarehouse',
     1, 0, 'C', '0', '0', 'warehouse:official:list', 'HomeOutlined', 'admin',
     sysdate(), '', null, '仓库管理菜单：官方仓库'),
    (2022, '第三方仓库', 2020, 10, 'third-party', 'Warehouse/ThirdParty/index', '', 'ThirdPartyWarehouse',
     1, 0, 'C', '0', '0', 'warehouse:thirdParty:list', 'ShopOutlined', 'admin',
     sysdate(), '', null, '仓库管理菜单：第三方仓库'),
    (202101, '官方仓库查询', 2021, 1, '#', '', '', '',
     1, 0, 'F', '0', '0', 'warehouse:official:list', '#', 'admin',
     sysdate(), '', null, ''),
    (202102, '官方仓库新增', 2021, 2, '#', '', '', '',
     1, 0, 'F', '0', '0', 'warehouse:official:add', '#', 'admin',
     sysdate(), '', null, ''),
    (202103, '官方仓库编辑', 2021, 3, '#', '', '', '',
     1, 0, 'F', '0', '0', 'warehouse:official:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (202104, '官方仓库启停', 2021, 4, '#', '', '', '',
     1, 0, 'F', '0', '0', 'warehouse:official:status', '#', 'admin',
     sysdate(), '', null, ''),
    (202105, '官方仓库同步', 2021, 5, '#', '', '', '',
     1, 0, 'F', '0', '0', 'warehouse:official:sync', '#', 'admin',
     sysdate(), '', null, ''),
    (202201, '第三方仓库查询', 2022, 1, '#', '', '', '',
     1, 0, 'F', '0', '0', 'warehouse:thirdParty:list', '#', 'admin',
     sysdate(), '', null, ''),
    (202202, '第三方仓库新增', 2022, 2, '#', '', '', '',
     1, 0, 'F', '0', '0', 'warehouse:thirdParty:add', '#', 'admin',
     sysdate(), '', null, ''),
    (202203, '第三方仓库编辑', 2022, 3, '#', '', '', '',
     1, 0, 'F', '0', '0', 'warehouse:thirdParty:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (202204, '第三方仓库启停', 2022, 4, '#', '', '', '',
     1, 0, 'F', '0', '0', 'warehouse:thirdParty:status', '#', 'admin',
     sysdate(), '', null, '')
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

call assert_warehouse_management_seed_completed();

commit;

drop temporary table if exists tmp_warehouse_management_sys_menu_guard;
drop procedure if exists assert_warehouse_management_seed_completed;
drop procedure if exists assert_warehouse_management_sys_menu_guard;
