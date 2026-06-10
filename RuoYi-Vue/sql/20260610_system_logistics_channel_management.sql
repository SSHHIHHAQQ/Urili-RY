-- System logistics channel management schema and admin permissions.
-- Scope: system channel extensions, warehouse shipper overrides, order rules, and menu 2041.

set names utf8mb4;

set @confirm_system_logistics_channel_management := coalesce(@confirm_system_logistics_channel_management, '');

delimiter //

drop procedure if exists assert_system_logistics_channel_management_confirmed//
create procedure assert_system_logistics_channel_management_confirmed()
begin
  if coalesce(@confirm_system_logistics_channel_management, '')
      <> 'APPLY_SYSTEM_LOGISTICS_CHANNEL_MANAGEMENT' then
    signal sqlstate '45000' set message_text = 'set @confirm_system_logistics_channel_management = APPLY_SYSTEM_LOGISTICS_CHANNEL_MANAGEMENT before running this migration';
  end if;
end//

drop procedure if exists add_system_logistics_column_if_missing//
create procedure add_system_logistics_column_if_missing(
  in p_table_name varchar(128),
  in p_column_name varchar(128),
  in p_alter_sql text
)
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table_name
      and column_name = p_column_name
  ) then
    set @ddl = p_alter_sql;
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_system_logistics_channel_menu_guard//
create procedure assert_system_logistics_channel_menu_guard()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2041
      and parent_id = 2030
      and path = 'channel-system'
      and component = 'Channel/System/index'
      and route_name = 'ChannelSystem'
      and perms in ('channel:system:list', 'logistics:systemChannel:list')
      and menu_type = 'C'
  ) then
    signal sqlstate '45000' set message_text = 'system logistics channel page menu 2041 must exist with expected placeholder or final signature';
  end if;

  if exists (
    select 1
    from sys_menu m
    left join tmp_system_logistics_channel_button_guard seed
      on seed.menu_id = m.menu_id
     and seed.parent_id = m.parent_id
     and seed.menu_type = m.menu_type
     and coalesce(seed.perms, '') = coalesce(m.perms, '')
    where m.menu_id in (2520, 2521, 2522, 2523, 2524, 2525)
      and seed.menu_id is null
  ) then
    signal sqlstate '45000' set message_text = 'system logistics channel button menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_system_logistics_channel_button_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'system logistics channel button permission is already used by another menu';
  end if;
end//

drop procedure if exists assert_system_logistics_channel_management_completed//
create procedure assert_system_logistics_channel_management_completed()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2041
      and parent_id = 2030
      and path = 'channel-system'
      and component = 'Channel/System/index'
      and route_name = 'ChannelSystem'
      and perms = 'logistics:systemChannel:list'
      and menu_type = 'C'
  ) then
    signal sqlstate '45000' set message_text = 'system logistics channel page menu final permission mismatch';
  end if;

  if exists (
    select 1
    from tmp_system_logistics_channel_button_guard seed
    where not exists (
      select 1
      from sys_menu m
      where m.menu_id = seed.menu_id
        and m.parent_id = seed.parent_id
        and m.menu_type = seed.menu_type
        and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'system logistics channel button permission final state mismatch';
  end if;
end//

delimiter ;

call assert_system_logistics_channel_management_confirmed();
drop procedure if exists assert_system_logistics_channel_management_confirmed;

create table if not exists logistics_system_channel (
  system_channel_code      varchar(64)  not null                  comment '系统渠道代码',
  system_channel_name      varchar(200) not null                  comment '系统渠道名称',
  fulfillment_mode          varchar(32)  not null default 'CARRIER_LABELING' comment '渠道履约模式',
  standard_carrier_code    varchar(64)  not null                  comment '标准最终承运商code',
  signature_services        varchar(128) default ''                comment '支持的签名服务code集合，逗号分隔',
  status                   varchar(16)  not null default 'ENABLED' comment '状态',
  display_order            int          not null default 0        comment '显示排序',
  create_by                varchar(64)  default ''                comment '创建者',
  create_time              datetime                               comment '创建时间',
  update_by                varchar(64)  default ''                comment '更新者',
  update_time              datetime                               comment '更新时间',
  remark                   varchar(500) default ''                comment '备注',
  primary key (system_channel_code),
  key idx_logistics_system_channel_fulfillment (fulfillment_mode, status),
  key idx_logistics_system_channel_carrier (standard_carrier_code, status),
  key idx_logistics_system_channel_order (display_order, system_channel_code)
) engine=innodb comment='系统物流渠道表';

call add_system_logistics_column_if_missing(
  'logistics_system_channel',
  'fulfillment_mode',
  'alter table logistics_system_channel add column fulfillment_mode varchar(32) not null default ''CARRIER_LABELING'' comment ''渠道履约模式'' after system_channel_name'
);

call add_system_logistics_column_if_missing(
  'logistics_system_channel',
  'signature_services',
  'alter table logistics_system_channel add column signature_services varchar(128) default '''' comment ''支持的签名服务code集合，逗号分隔'' after standard_carrier_code'
);

create table if not exists logistics_system_channel_warehouse (
  binding_id              bigint(20)   not null auto_increment   comment '绑定ID',
  system_channel_code     varchar(64)  not null                  comment '系统渠道代码',
  warehouse_id            bigint(20)   not null                  comment '仓库ID',
  warehouse_code          varchar(64)  not null                  comment '仓库代码快照',
  warehouse_name          varchar(200) not null                  comment '仓库名称快照',
  warehouse_kind          varchar(32)  not null                  comment '仓库类型快照',
  status                  varchar(16)  not null default 'ENABLED' comment '绑定状态',
  shipper_address_mode    varchar(32)  not null default 'WAREHOUSE' comment '发货地址模式',
  external_shipper_code   varchar(100) default ''                comment '外部物流商发货地址编码',
  shipper_company_name    varchar(200) default ''                comment '发货公司',
  shipper_contact_name    varchar(100) default ''                comment '发货联系人',
  shipper_contact_phone   varchar(64)  default ''                comment '发货联系电话',
  shipper_contact_email   varchar(128) default ''                comment '发货联系邮箱',
  shipper_country_code    varchar(32)  default ''                comment '发货国家/地区',
  shipper_state_province  varchar(100) default ''                comment '发货州/省',
  shipper_city            varchar(100) default ''                comment '发货城市',
  shipper_postal_code     varchar(32)  default ''                comment '发货邮编',
  shipper_address_line1   varchar(255) default ''                comment '发货地址1',
  shipper_address_line2   varchar(255) default ''                comment '发货地址2',
  create_by               varchar(64)  default ''                comment '创建者',
  create_time             datetime                               comment '创建时间',
  update_by               varchar(64)  default ''                comment '更新者',
  update_time             datetime                               comment '更新时间',
  remark                  varchar(500) default ''                comment '备注',
  primary key (binding_id),
  unique key uk_logistics_system_channel_warehouse (system_channel_code, warehouse_id),
  key idx_logistics_system_channel_warehouse_status (system_channel_code, status),
  key idx_logistics_system_channel_warehouse_shipper (external_shipper_code)
) engine=innodb comment='系统渠道仓库及发货地址覆写表';

create table if not exists logistics_system_channel_order_setting (
  setting_id             bigint(20)   not null auto_increment comment '规则ID',
  system_channel_code    varchar(64)  not null                comment '系统渠道代码',
  destination_countries  varchar(500) default ''              comment '目的国家/地区code集合，逗号分隔',
  min_weight             decimal(18,4)                         comment '最小重量',
  max_weight             decimal(18,4)                         comment '最大重量',
  max_length             decimal(18,4)                         comment '最大长度',
  max_width              decimal(18,4)                         comment '最大宽度',
  max_height             decimal(18,4)                         comment '最大高度',
  max_girth              decimal(18,4)                         comment '最大围长',
  signature_service      varchar(64)  default ''              comment '默认签名服务',
  validation_mode        varchar(16)  not null default 'STRICT' comment '校验模式',
  create_by              varchar(64)  default ''               comment '创建者',
  create_time            datetime                              comment '创建时间',
  update_by              varchar(64)  default ''               comment '更新者',
  update_time            datetime                              comment '更新时间',
  remark                 varchar(500) default ''               comment '备注',
  primary key (setting_id),
  unique key uk_logistics_channel_order_setting (system_channel_code)
) engine=innodb comment='系统渠道下单规则表';

insert ignore into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark) values
('系统物流渠道状态', 'logistics_system_channel_status', '0', 'admin', sysdate(), '系统物流渠道启停状态'),
('系统渠道履约模式', 'logistics_system_channel_fulfillment_mode', '0', 'admin', sysdate(), '系统渠道履约链路模式'),
('系统渠道绑定状态', 'logistics_channel_binding_status', '0', 'admin', sysdate(), '系统渠道仓库和物流商绑定状态'),
('系统渠道发货地址模式', 'logistics_shipper_address_mode', '0', 'admin', sysdate(), '系统渠道仓库发货地址覆写模式'),
('系统渠道下单校验模式', 'logistics_order_rule_validation_mode', '0', 'admin', sysdate(), '系统渠道第一版下单规则校验模式');

create temporary table if not exists tmp_system_logistics_channel_dict_data_seed (
  dict_sort    int not null,
  dict_label   varchar(100) not null,
  dict_value   varchar(100) not null,
  dict_type    varchar(100) not null,
  list_class   varchar(100) default '',
  is_default   char(1) default 'N',
  status       char(1) default '0',
  remark       varchar(500) default '',
  primary key (dict_type, dict_value)
);

truncate table tmp_system_logistics_channel_dict_data_seed;
insert into tmp_system_logistics_channel_dict_data_seed(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, remark) values
(1, '启用', 'ENABLED', 'logistics_system_channel_status', 'success', 'Y', '0', ''),
(2, '停用', 'DISABLED', 'logistics_system_channel_status', 'default', 'N', '0', ''),
(1, '物流商打单', 'CARRIER_LABELING', 'logistics_system_channel_fulfillment_mode', 'processing', 'Y', '0', '先由物流商打单，再推送履约仓/WMS'),
(2, '直推履约仓', 'DIRECT_FULFILLMENT_WAREHOUSE', 'logistics_system_channel_fulfillment_mode', 'default', 'N', '0', '不经物流商打单，直接推送订单或已有面单信息到履约仓/WMS'),
(1, '启用', 'ENABLED', 'logistics_channel_binding_status', 'success', 'Y', '0', ''),
(2, '停用', 'DISABLED', 'logistics_channel_binding_status', 'default', 'N', '0', ''),
(1, '使用仓库地址', 'WAREHOUSE', 'logistics_shipper_address_mode', 'default', 'Y', '0', '未填写覆写地址时使用仓库主数据地址'),
(2, '使用外部地址编码', 'EXTERNAL_CODE', 'logistics_shipper_address_mode', 'primary', 'N', '0', '优先传外部物流商发货地址编码'),
(3, '覆写发货地址', 'OVERRIDE', 'logistics_shipper_address_mode', 'warning', 'N', '0', '按当前渠道和仓库覆写发货地址'),
(1, '严格拦截', 'STRICT', 'logistics_order_rule_validation_mode', 'danger', 'Y', '0', ''),
(2, '仅提示', 'WARNING', 'logistics_order_rule_validation_mode', 'warning', 'N', '0', '');

update sys_dict_data d
join tmp_system_logistics_channel_dict_data_seed seed
  on seed.dict_type = d.dict_type
 and seed.dict_value = d.dict_value
set d.dict_sort = seed.dict_sort,
    d.dict_label = seed.dict_label,
    d.list_class = seed.list_class,
    d.is_default = seed.is_default,
    d.status = seed.status,
    d.update_by = 'admin',
    d.update_time = sysdate(),
    d.remark = seed.remark;

insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, '', seed.list_class,
       seed.is_default, seed.status, 'admin', sysdate(), seed.remark
from tmp_system_logistics_channel_dict_data_seed seed
where not exists (
  select 1
  from sys_dict_data d
  where d.dict_type = seed.dict_type
    and d.dict_value = seed.dict_value
);

create temporary table if not exists tmp_system_logistics_channel_button_guard (
  menu_id   bigint(20) not null primary key,
  parent_id bigint(20) not null,
  menu_type char(1) not null,
  perms     varchar(100) not null
);

truncate table tmp_system_logistics_channel_button_guard;
insert into tmp_system_logistics_channel_button_guard(menu_id, parent_id, menu_type, perms) values
(2520, 2041, 'F', 'logistics:systemChannel:query'),
(2521, 2041, 'F', 'logistics:systemChannel:add'),
(2522, 2041, 'F', 'logistics:systemChannel:edit'),
(2523, 2041, 'F', 'logistics:systemChannel:status'),
(2524, 2041, 'F', 'logistics:systemChannel:binding'),
(2525, 2041, 'F', 'logistics:systemChannel:rule');

call assert_system_logistics_channel_menu_guard();

update sys_menu
set menu_name = '系统渠道管理',
    component = 'Channel/System/index',
    route_name = 'ChannelSystem',
    perms = 'logistics:systemChannel:list',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '系统物流渠道管理页面'
where menu_id = 2041;

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
(2520, '系统渠道查询', 2041, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:systemChannel:query', '#', 'admin', sysdate(), '系统渠道查询按钮'),
(2521, '系统渠道新增', 2041, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:systemChannel:add', '#', 'admin', sysdate(), '系统渠道新增按钮'),
(2522, '系统渠道编辑', 2041, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:systemChannel:edit', '#', 'admin', sysdate(), '系统渠道编辑按钮'),
(2523, '系统渠道启停', 2041, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:systemChannel:status', '#', 'admin', sysdate(), '系统渠道启停按钮'),
(2524, '系统渠道绑定', 2041, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:systemChannel:binding', '#', 'admin', sysdate(), '系统渠道物流商和仓库绑定按钮'),
(2525, '系统渠道下单规则', 2041, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:systemChannel:rule', '#', 'admin', sysdate(), '系统渠道下单规则按钮')
on duplicate key update
  menu_name = values(menu_name),
  parent_id = values(parent_id),
  order_num = values(order_num),
  path = values(path),
  component = values(component),
  route_name = values(route_name),
  menu_type = values(menu_type),
  visible = values(visible),
  status = values(status),
  perms = values(perms),
  icon = values(icon),
  update_by = 'admin',
  update_time = sysdate(),
  remark = values(remark);

call assert_system_logistics_channel_management_completed();

drop temporary table if exists tmp_system_logistics_channel_button_guard;
drop temporary table if exists tmp_system_logistics_channel_dict_data_seed;
drop procedure if exists assert_system_logistics_channel_menu_guard;
drop procedure if exists assert_system_logistics_channel_management_completed;
drop procedure if exists add_system_logistics_column_if_missing;
