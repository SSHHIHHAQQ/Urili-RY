-- Customer logistics channel management schema and admin permissions.
-- Scope: customer channel, system-channel bindings, buyer visibility scope, and menu 2042.

set names utf8mb4;

set @confirm_customer_logistics_channel_management := coalesce(@confirm_customer_logistics_channel_management, '');

delimiter //

drop procedure if exists assert_customer_logistics_channel_management_confirmed//
create procedure assert_customer_logistics_channel_management_confirmed()
begin
  if coalesce(@confirm_customer_logistics_channel_management, '')
      <> 'APPLY_CUSTOMER_LOGISTICS_CHANNEL_MANAGEMENT' then
    signal sqlstate '45000' set message_text = 'set @confirm_customer_logistics_channel_management = APPLY_CUSTOMER_LOGISTICS_CHANNEL_MANAGEMENT before running this migration';
  end if;
end//

drop procedure if exists assert_customer_logistics_channel_menu_guard//
create procedure assert_customer_logistics_channel_menu_guard()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2042
      and parent_id = 2030
      and path = 'channel-customer'
      and component = 'Channel/Customer/index'
      and route_name = 'ChannelCustomer'
      and perms in ('channel:customer:list', 'logistics:customerChannel:list')
      and menu_type = 'C'
  ) then
    signal sqlstate '45000' set message_text = 'customer logistics channel page menu 2042 must exist with expected placeholder or final signature';
  end if;

  if exists (
    select 1
    from sys_menu m
    where m.menu_id in (2260, 2261, 2530, 2531, 2532, 2533)
      and not exists (
        select 1
        from tmp_customer_logistics_channel_button_guard seed
        where seed.menu_id = m.menu_id
          and seed.parent_id = m.parent_id
          and seed.menu_type = m.menu_type
          and (
            coalesce(seed.perms, '') = coalesce(m.perms, '')
            or (m.menu_id = 2260 and coalesce(m.perms, '') = 'channel:customer:query')
            or (m.menu_id = 2261 and coalesce(m.perms, '') = 'channel:customer:add')
          )
      )
  ) then
    signal sqlstate '45000' set message_text = 'customer logistics channel button menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_customer_logistics_channel_button_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'customer logistics channel button permission is already used by another menu';
  end if;
end//

drop procedure if exists assert_customer_logistics_channel_management_completed//
create procedure assert_customer_logistics_channel_management_completed()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2042
      and parent_id = 2030
      and path = 'channel-customer'
      and component = 'Channel/Customer/index'
      and route_name = 'ChannelCustomer'
      and perms = 'logistics:customerChannel:list'
      and menu_type = 'C'
  ) then
    signal sqlstate '45000' set message_text = 'customer logistics channel page menu final permission mismatch';
  end if;

  if exists (
    select 1
    from tmp_customer_logistics_channel_button_guard seed
    where not exists (
      select 1
      from sys_menu m
      where m.menu_id = seed.menu_id
        and m.parent_id = seed.parent_id
        and m.menu_type = seed.menu_type
        and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'customer logistics channel button permission final state mismatch';
  end if;
end//

delimiter ;

call assert_customer_logistics_channel_management_confirmed();
drop procedure if exists assert_customer_logistics_channel_management_confirmed;

create table if not exists logistics_customer_channel (
  customer_channel_code            varchar(64)  not null                  comment '客户渠道代码',
  customer_channel_name            varchar(200) not null                  comment '客户渠道名称',
  channel_type                     varchar(32)  not null                  comment '渠道类型',
  standard_carrier_code            varchar(64)  not null                  comment '标准最终承运商code',
  signature_services               varchar(128) default ''                comment '支持的签名服务code集合，逗号分隔',
  label_upload_required            varchar(16)  not null default 'NOT_REQUIRED' comment '上传物流面单',
  platform_label_fetch             varchar(16)  not null default 'NOT_FETCH' comment '平台面单获取',
  customer_label_upload_supported  varchar(16)  not null default 'UNSUPPORTED' comment '客户上传面单支持',
  buyer_scope_mode                 varchar(16)  not null default 'ALL'     comment '买家范围模式',
  status                           varchar(16)  not null default 'ENABLED' comment '状态',
  display_order                    int          not null default 0         comment '显示排序',
  create_by                        varchar(64)  default ''                comment '创建者',
  create_time                      datetime                                comment '创建时间',
  update_by                        varchar(64)  default ''                comment '更新者',
  update_time                      datetime                                comment '更新时间',
  remark                           varchar(500) default ''                comment '备注',
  primary key (customer_channel_code),
  key idx_logistics_customer_channel_name (customer_channel_name),
  key idx_logistics_customer_channel_type_status (channel_type, status),
  key idx_logistics_customer_channel_carrier (standard_carrier_code, status),
  key idx_logistics_customer_channel_scope (buyer_scope_mode, status),
  key idx_logistics_customer_channel_order (display_order, customer_channel_code)
) engine=innodb comment='客户物流渠道表';

create table if not exists logistics_customer_channel_system_mapping (
  mapping_id                      bigint(20)   not null auto_increment   comment '绑定ID',
  customer_channel_code           varchar(64)  not null                  comment '客户渠道代码',
  system_channel_code             varchar(64)  not null                  comment '系统渠道代码',
  system_channel_name_snapshot    varchar(200) not null                  comment '系统渠道名称快照',
  standard_carrier_code_snapshot  varchar(64)  not null                  comment '标准最终承运商code快照',
  signature_services_snapshot     varchar(128) default ''                comment '签名服务code快照，逗号分隔',
  status                          varchar(16)  not null default 'ENABLED' comment '绑定状态',
  display_order                   int          not null default 0         comment '显示排序',
  create_by                       varchar(64)  default ''                comment '创建者',
  create_time                     datetime                                comment '创建时间',
  update_by                       varchar(64)  default ''                comment '更新者',
  update_time                     datetime                                comment '更新时间',
  remark                          varchar(500) default ''                comment '备注',
  primary key (mapping_id),
  unique key uk_logistics_customer_channel_system (customer_channel_code, system_channel_code),
  key idx_logistics_customer_channel_system_status (customer_channel_code, status),
  key idx_logistics_customer_channel_system_channel (system_channel_code, status),
  key idx_logistics_customer_channel_system_order (customer_channel_code, display_order)
) engine=innodb comment='客户渠道与系统渠道绑定表';

create table if not exists logistics_customer_channel_buyer_scope (
  scope_id                   bigint(20)   not null auto_increment comment '范围明细ID',
  customer_channel_code      varchar(64)  not null                comment '客户渠道代码',
  buyer_id                   bigint(20)   not null                comment '买家ID',
  buyer_code_snapshot        varchar(64)  not null                comment '买家代码快照',
  buyer_name_snapshot        varchar(200) not null                comment '买家名称快照',
  buyer_short_name_snapshot  varchar(100) default ''              comment '买家简称快照',
  create_by                  varchar(64)  default ''              comment '创建者',
  create_time                datetime                              comment '创建时间',
  remark                     varchar(500) default ''              comment '备注',
  primary key (scope_id),
  unique key uk_logistics_customer_channel_buyer (customer_channel_code, buyer_id),
  key idx_logistics_customer_channel_buyer_id (buyer_id),
  key idx_logistics_customer_channel_buyer_code (buyer_code_snapshot),
  key idx_logistics_customer_channel_buyer_name (buyer_name_snapshot)
) engine=innodb comment='客户渠道买家范围表';

insert ignore into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark) values
('客户渠道类型', 'logistics_customer_channel_type', '0', 'admin', sysdate(), '客户渠道面单来源类型'),
('物流面单上传要求', 'logistics_label_upload_required', '0', 'admin', sysdate(), '客户渠道物流面单上传要求'),
('平台面单获取', 'logistics_platform_label_fetch', '0', 'admin', sysdate(), '客户渠道平台面单获取配置'),
('客户上传面单支持', 'logistics_customer_label_upload_support', '0', 'admin', sysdate(), '客户渠道客户上传面单支持配置'),
('客户渠道买家范围模式', 'logistics_customer_channel_scope_mode', '0', 'admin', sysdate(), '客户渠道买家可见范围模式'),
('客户渠道状态', 'logistics_customer_channel_status', '0', 'admin', sysdate(), '客户渠道启停状态'),
('客户渠道绑定状态', 'logistics_customer_channel_binding_status', '0', 'admin', sysdate(), '客户渠道绑定系统渠道状态');

create temporary table if not exists tmp_customer_logistics_channel_dict_data_seed (
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

truncate table tmp_customer_logistics_channel_dict_data_seed;
insert into tmp_customer_logistics_channel_dict_data_seed(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, remark) values
(1, '仓库面单', 'WAREHOUSE_LABEL', 'logistics_customer_channel_type', 'success', 'Y', '0', '买家不需要上传面单'),
(2, '第三方面单', 'THIRD_PARTY_LABEL', 'logistics_customer_channel_type', 'processing', 'N', '0', '面单来自仓库以外来源'),
(1, '需要上传', 'REQUIRED', 'logistics_label_upload_required', 'warning', 'N', '0', ''),
(2, '不需要上传', 'NOT_REQUIRED', 'logistics_label_upload_required', 'default', 'Y', '0', ''),
(1, '获取', 'FETCH', 'logistics_platform_label_fetch', 'success', 'N', '0', ''),
(2, '不获取', 'NOT_FETCH', 'logistics_platform_label_fetch', 'default', 'Y', '0', ''),
(1, '支持', 'SUPPORTED', 'logistics_customer_label_upload_support', 'success', 'N', '0', ''),
(2, '不支持', 'UNSUPPORTED', 'logistics_customer_label_upload_support', 'default', 'Y', '0', ''),
(1, '全部买家可用', 'ALL', 'logistics_customer_channel_scope_mode', 'default', 'Y', '0', ''),
(2, '可用名单', 'INCLUDE', 'logistics_customer_channel_scope_mode', 'success', 'N', '0', ''),
(3, '不可用名单', 'EXCLUDE', 'logistics_customer_channel_scope_mode', 'warning', 'N', '0', ''),
(1, '启用', 'ENABLED', 'logistics_customer_channel_status', 'success', 'Y', '0', ''),
(2, '停用', 'DISABLED', 'logistics_customer_channel_status', 'default', 'N', '0', ''),
(1, '启用', 'ENABLED', 'logistics_customer_channel_binding_status', 'success', 'Y', '0', ''),
(2, '停用', 'DISABLED', 'logistics_customer_channel_binding_status', 'default', 'N', '0', '');

update sys_dict_data d
join tmp_customer_logistics_channel_dict_data_seed seed
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
from tmp_customer_logistics_channel_dict_data_seed seed
where not exists (
  select 1
  from sys_dict_data d
  where d.dict_type = seed.dict_type
    and d.dict_value = seed.dict_value
);

create temporary table if not exists tmp_customer_logistics_channel_button_guard (
  menu_id   bigint(20) not null primary key,
  parent_id bigint(20) not null,
  menu_type char(1) not null,
  perms     varchar(100) not null
);

truncate table tmp_customer_logistics_channel_button_guard;
insert into tmp_customer_logistics_channel_button_guard(menu_id, parent_id, menu_type, perms) values
(2260, 2042, 'F', 'logistics:customerChannel:query'),
(2261, 2042, 'F', 'logistics:customerChannel:add'),
(2530, 2042, 'F', 'logistics:customerChannel:edit'),
(2531, 2042, 'F', 'logistics:customerChannel:status'),
(2532, 2042, 'F', 'logistics:customerChannel:binding'),
(2533, 2042, 'F', 'logistics:customerChannel:buyer');

call assert_customer_logistics_channel_menu_guard();

update sys_menu
set menu_name = '客户渠道管理',
    component = 'Channel/Customer/index',
    route_name = 'ChannelCustomer',
    perms = 'logistics:customerChannel:list',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '客户渠道管理页面'
where menu_id = 2042;

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
(2260, '客户渠道查询', 2042, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:customerChannel:query', '#', 'admin', sysdate(), '客户渠道查询按钮'),
(2261, '客户渠道新增', 2042, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:customerChannel:add', '#', 'admin', sysdate(), '客户渠道新增按钮'),
(2530, '客户渠道编辑', 2042, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:customerChannel:edit', '#', 'admin', sysdate(), '客户渠道编辑按钮'),
(2531, '客户渠道启停', 2042, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:customerChannel:status', '#', 'admin', sysdate(), '客户渠道启停按钮'),
(2532, '客户渠道绑定系统渠道', 2042, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:customerChannel:binding', '#', 'admin', sysdate(), '客户渠道绑定系统渠道按钮'),
(2533, '客户渠道绑定买家', 2042, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:customerChannel:buyer', '#', 'admin', sysdate(), '客户渠道绑定买家按钮')
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

call assert_customer_logistics_channel_management_completed();

drop temporary table if exists tmp_customer_logistics_channel_button_guard;
drop temporary table if exists tmp_customer_logistics_channel_dict_data_seed;
drop procedure if exists assert_customer_logistics_channel_menu_guard;
drop procedure if exists assert_customer_logistics_channel_management_completed;
