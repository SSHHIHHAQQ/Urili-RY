-- Seller/buyer management seed for the RuoYi validation project.
-- Scope: seller/buyer modules, portal account bindings, dictionaries, menu entries, and permissions.

set names utf8mb4;

create table if not exists seller (
  seller_id             bigint(20)      not null auto_increment    comment '卖家ID',
  seller_no             varchar(64)     not null                   comment '系统内部卖家编号',
  seller_code           varchar(64)     not null                   comment '对外卖家代码',
  seller_name           varchar(200)    not null                   comment '卖家全称',
  seller_short_name     varchar(100)    not null default ''        comment '卖家简称',
  seller_type           varchar(32)     not null default 'COMPANY' comment '主体类型',
  seller_level          varchar(32)     not null default 'L1'      comment '卖家等级',
  status                char(1)         not null default '0'       comment '状态：0正常 1停用',
  legal_id              varchar(100)    default ''                 comment '法人证件号',
  business_license_no   varchar(100)    default ''                 comment '营业执照号码',
  country_code          varchar(32)     not null                   comment '国家/地区代码',
  state_province        varchar(100)    not null default ''        comment '省/州',
  city                  varchar(100)    not null                   comment '城市',
  postal_code           varchar(32)     not null                   comment '邮编',
  address_line1         varchar(255)    not null                   comment '地址1',
  address_line2         varchar(255)    default ''                 comment '地址2',
  contact_name          varchar(100)    not null default ''        comment '联系人',
  contact_phone         varchar(64)     not null default ''        comment '手机号',
  contact_email         varchar(128)    default ''                 comment '邮箱',
  attachment_file_name  varchar(255)    default ''                 comment '附件文件名',
  attachment_mime_type  varchar(100)    default ''                 comment '附件类型',
  attachment_size_bytes bigint          default null               comment '附件大小',
  attachment_file_url   longtext                                    comment '附件文件地址',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (seller_id),
  unique key uk_seller_no (seller_no),
  unique key uk_seller_code (seller_code),
  key idx_seller_status (status),
  key idx_seller_name (seller_name),
  key idx_seller_short_name (seller_short_name),
  key idx_seller_level (seller_level)
) engine=innodb auto_increment=1 comment = '卖家主体表';

create table if not exists buyer (
  buyer_id              bigint(20)      not null auto_increment    comment '买家ID',
  buyer_no              varchar(64)     not null                   comment '系统内部买家编号',
  buyer_code            varchar(64)     not null                   comment '对外买家代码',
  buyer_name            varchar(200)    not null                   comment '买家全称',
  buyer_short_name      varchar(100)    not null default ''        comment '买家简称',
  buyer_type            varchar(32)     not null default 'COMPANY' comment '主体类型',
  buyer_level           varchar(32)     not null default 'L1'      comment '买家等级',
  status                char(1)         not null default '0'       comment '状态：0正常 1停用',
  legal_id              varchar(100)    default ''                 comment '法人证件号',
  business_license_no   varchar(100)    default ''                 comment '营业执照号码',
  country_code          varchar(32)     not null                   comment '国家/地区代码',
  state_province        varchar(100)    not null default ''        comment '省/州',
  city                  varchar(100)    not null                   comment '城市',
  postal_code           varchar(32)     not null                   comment '邮编',
  address_line1         varchar(255)    not null                   comment '地址1',
  address_line2         varchar(255)    default ''                 comment '地址2',
  contact_name          varchar(100)    not null default ''        comment '联系人',
  contact_phone         varchar(64)     not null default ''        comment '手机号',
  contact_email         varchar(128)    default ''                 comment '邮箱',
  attachment_file_name  varchar(255)    default ''                 comment '附件文件名',
  attachment_mime_type  varchar(100)    default ''                 comment '附件类型',
  attachment_size_bytes bigint          default null               comment '附件大小',
  attachment_file_url   longtext                                    comment '附件文件地址',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (buyer_id),
  unique key uk_buyer_no (buyer_no),
  unique key uk_buyer_code (buyer_code),
  key idx_buyer_status (status),
  key idx_buyer_name (buyer_name),
  key idx_buyer_short_name (buyer_short_name),
  key idx_buyer_level (buyer_level)
) engine=innodb auto_increment=1 comment = '买家主体表';

create table if not exists seller_account (
  seller_account_id     bigint(20)      not null auto_increment    comment '卖家账号绑定ID',
  seller_id             bigint(20)      not null                   comment '卖家ID',
  user_id               bigint(20)      not null                   comment '若依用户ID',
  account_role          varchar(32)     not null default 'OWNER'   comment '卖家侧账号角色',
  status                char(1)         not null default '0'       comment '绑定状态：0正常 1停用',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (seller_account_id),
  unique key uk_seller_account_user (user_id),
  unique key uk_seller_account_seller_user (seller_id, user_id),
  key idx_seller_account_seller_status (seller_id, status)
) engine=innodb auto_increment=1 comment = '卖家账号绑定表';

create table if not exists buyer_account (
  buyer_account_id      bigint(20)      not null auto_increment    comment '买家账号绑定ID',
  buyer_id              bigint(20)      not null                   comment '买家ID',
  user_id               bigint(20)      not null                   comment '若依用户ID',
  account_role          varchar(32)     not null default 'OWNER'   comment '买家侧账号角色',
  status                char(1)         not null default '0'       comment '绑定状态：0正常 1停用',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (buyer_account_id),
  unique key uk_buyer_account_user (user_id),
  unique key uk_buyer_account_buyer_user (buyer_id, user_id),
  key idx_buyer_account_buyer_status (buyer_id, status)
) engine=innodb auto_increment=1 comment = '买家账号绑定表';

insert into sys_role
    (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
     status, del_flag, create_by, create_time, update_by, update_time, remark)
select
    '卖家端账号', 'seller', 200, '1', 1, 1,
    '0', '0', 'admin', sysdate(), '', null, '卖家端登录账号基础角色'
where not exists (select 1 from sys_role where role_key = 'seller');

update sys_role
set role_name = '卖家端账号',
    role_sort = 200,
    data_scope = '1',
    status = '0',
    del_flag = '0',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '卖家端登录账号基础角色'
where role_key = 'seller';

insert into sys_role
    (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
     status, del_flag, create_by, create_time, update_by, update_time, remark)
select
    '买家端账号', 'buyer', 201, '1', 1, 1,
    '0', '0', 'admin', sysdate(), '', null, '买家端登录账号基础角色'
where not exists (select 1 from sys_role where role_key = 'buyer');

update sys_role
set role_name = '买家端账号',
    role_sort = 201,
    data_scope = '1',
    status = '0',
    del_flag = '0',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '买家端登录账号基础角色'
where role_key = 'buyer';

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select
    '主体类型', 'subject_type', '0', 'admin', sysdate(), '', null, '主体类型：公司/个人/其他'
where not exists (select 1 from sys_dict_type where dict_type = 'subject_type');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'subject_type', '', '', seed.is_default, '0', 'admin', sysdate(), '', null, '主体类型'
from (
    select 1 as dict_sort, '公司' as dict_label, 'COMPANY' as dict_value, 'Y' as is_default
    union all select 2, '个人', 'PERSON', 'N'
    union all select 3, '其他', 'OTHER', 'N'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'subject_type' and d.dict_value = seed.dict_value
);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '卖家等级', 'seller_level', '0', 'admin', sysdate(), '', null, '卖家等级'
where not exists (select 1 from sys_dict_type where dict_type = 'seller_level');

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '买家等级', 'buyer_level', '0', 'admin', sysdate(), '', null, '买家等级'
where not exists (select 1 from sys_dict_type where dict_type = 'buyer_level');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, level_type.dict_type, '', '', seed.is_default, '0', 'admin', sysdate(), '', null, level_type.dict_name
from (
    select 'seller_level' as dict_type, '卖家等级' as dict_name
    union all select 'buyer_level', '买家等级'
) level_type
join (
    select 1 as dict_sort, '等级1' as dict_label, 'L1' as dict_value, 'Y' as is_default
    union all select 2, '等级2', 'L2', 'N'
    union all select 3, '等级3', 'L3', 'N'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = level_type.dict_type and d.dict_value = seed.dict_value
);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '卖家账号角色', 'seller_account_role', '0', 'admin', sysdate(), '', null, '卖家账号角色'
where not exists (select 1 from sys_dict_type where dict_type = 'seller_account_role');

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '买家账号角色', 'buyer_account_role', '0', 'admin', sysdate(), '', null, '买家账号角色'
where not exists (select 1 from sys_dict_type where dict_type = 'buyer_account_role');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, role_type.dict_type, '', '', seed.is_default, '0', 'admin', sysdate(), '', null, role_type.dict_name
from (
    select 'seller_account_role' as dict_type, '卖家账号角色' as dict_name
    union all select 'buyer_account_role', '买家账号角色'
) role_type
join (
    select 1 as dict_sort, '负责人' as dict_label, 'OWNER' as dict_value, 'Y' as is_default
    union all select 2, '管理员', 'ADMIN', 'N'
    union all select 3, '普通账号', 'STAFF', 'N'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = role_type.dict_type and d.dict_value = seed.dict_value
);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select
    '国家/地区', 'country_region', '0', 'admin', sysdate(), '', null, '国家/地区代码'
where not exists (select 1 from sys_dict_type where dict_type = 'country_region');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'country_region', '', '', seed.is_default, '0', 'admin', sysdate(), '', null, '国家/地区'
from (
    select 1 as dict_sort, '中国 / China (CN)' as dict_label, 'CN' as dict_value, 'Y' as is_default
    union all select 2, '美国 / United States (US)', 'US', 'N'
    union all select 3, '英国 / United Kingdom (GB)', 'GB', 'N'
    union all select 4, '加拿大 / Canada (CA)', 'CA', 'N'
    union all select 5, '墨西哥 / Mexico (MX)', 'MX', 'N'
    union all select 6, '巴西 / Brazil (BR)', 'BR', 'N'
    union all select 7, '日本 / Japan (JP)', 'JP', 'N'
    union all select 8, '韩国 / South Korea (KR)', 'KR', 'N'
    union all select 9, '新加坡 / Singapore (SG)', 'SG', 'N'
    union all select 10, '中国香港 / Hong Kong (HK)', 'HK', 'N'
    union all select 11, '中国台湾 / Taiwan (TW)', 'TW', 'N'
    union all select 12, '中国澳门 / Macau (MO)', 'MO', 'N'
    union all select 13, '澳大利亚 / Australia (AU)', 'AU', 'N'
    union all select 14, '德国 / Germany (DE)', 'DE', 'N'
    union all select 15, '法国 / France (FR)', 'FR', 'N'
    union all select 16, '意大利 / Italy (IT)', 'IT', 'N'
    union all select 17, '西班牙 / Spain (ES)', 'ES', 'N'
    union all select 18, '荷兰 / Netherlands (NL)', 'NL', 'N'
    union all select 19, '越南 / Vietnam (VN)', 'VN', 'N'
    union all select 20, '泰国 / Thailand (TH)', 'TH', 'N'
    union all select 21, '马来西亚 / Malaysia (MY)', 'MY', 'N'
    union all select 22, '印度 / India (IN)', 'IN', 'N'
    union all select 23, '印度尼西亚 / Indonesia (ID)', 'ID', 'N'
    union all select 24, '菲律宾 / Philippines (PH)', 'PH', 'N'
    union all select 25, '阿联酋 / United Arab Emirates (AE)', 'AE', 'N'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'country_region' and d.dict_value = seed.dict_value
);

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2010, '主体管理', 0, 1, 'partner', null, '', 'PartnerManagement',
     1, 0, 'M', '0', '0', '', 'TeamOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：主体管理'),
    (2011, '卖家管理', 2010, 1, 'seller', 'Seller/index', '', 'Seller',
     1, 0, 'C', '0', '0', 'seller:admin:list', 'ShopOutlined', 'admin',
     sysdate(), '', null, '管理端卖家管理'),
    (2012, '买家管理', 2010, 2, 'buyer', 'Buyer/index', '', 'Buyer',
     1, 0, 'C', '0', '0', 'buyer:admin:list', 'UserOutlined', 'admin',
     sysdate(), '', null, '管理端买家管理'),
    (2200, '卖家查询', 2011, 1, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2201, '卖家新增', 2011, 2, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2202, '卖家修改', 2011, 3, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2203, '卖家启停', 2011, 4, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:changeStatus', '#', 'admin',
     sysdate(), '', null, ''),
    (2204, '卖家重置密码', 2011, 5, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:resetPwd', '#', 'admin',
     sysdate(), '', null, ''),
    (2210, '买家查询', 2012, 1, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2211, '买家新增', 2012, 2, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2212, '买家修改', 2012, 3, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2213, '买家启停', 2012, 4, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:changeStatus', '#', 'admin',
     sysdate(), '', null, ''),
    (2214, '买家重置密码', 2012, 5, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:resetPwd', '#', 'admin',
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
