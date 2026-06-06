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
