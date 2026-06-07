-- Product category and attribute configuration seed for the RuoYi validation project.
-- Scope: product configuration tables, dictionaries, admin menu entries, and permissions.
-- This script is prepared for review; do not execute against a remote database without confirmation and backup.

set names utf8mb4;

set @confirm_product_category_attribute_seed := coalesce(@confirm_product_category_attribute_seed, '');

delimiter //

drop procedure if exists assert_product_category_attribute_seed_confirmed//
create procedure assert_product_category_attribute_seed_confirmed()
begin
  if coalesce(@confirm_product_category_attribute_seed, '')
      <> 'APPLY_PRODUCT_CATEGORY_ATTRIBUTE_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_product_category_attribute_seed = APPLY_PRODUCT_CATEGORY_ATTRIBUTE_SEED before running this migration';
  end if;
end//

delimiter ;

call assert_product_category_attribute_seed_confirmed();
drop procedure if exists assert_product_category_attribute_seed_confirmed;

delimiter //

drop procedure if exists assert_product_category_attribute_sys_menu_guard//
create procedure assert_product_category_attribute_sys_menu_guard()
begin
  if exists (
    select 1
    from sys_menu m
    where exists (
        select 1
        from tmp_product_category_attribute_sys_menu_guard seed
        where seed.menu_id = m.menu_id
    )
      and not exists (
        select 1
        from tmp_product_category_attribute_sys_menu_guard seed
        where seed.menu_id = m.menu_id
          and m.parent_id = seed.parent_id
          and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')
          and coalesce(m.path, '') = coalesce(seed.path, '')
          and coalesce(m.component, '') = coalesce(seed.component, '')
          and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
          and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'product category attribute sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_product_category_attribute_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'product category attribute sys_menu signature is already used by another menu';
  end if;
end//

delimiter ;

create table if not exists product_category (
  category_id      bigint(20)    not null auto_increment comment '商品分类ID',
  parent_id        bigint(20)    not null default 0       comment '父分类ID',
  ancestors        varchar(500)  not null default '0'     comment '祖先链',
  category_code    varchar(64)   not null                 comment '分类编码',
  category_name    varchar(128)  not null                 comment '分类名称',
  category_level   int           not null default 1       comment '分类层级',
  publish_enabled  char(1)       not null default 'N'     comment '兼容字段，是否可发布由是否存在子分类自动判断：Y是 N否',
  sort_order       int           not null default 0       comment '显示排序',
  schema_version   int           not null default 1       comment '类目属性规则版本',
  status           char(1)       not null default '0'     comment '状态：0正常 1停用',
  del_flag         char(1)       not null default '0'     comment '删除标志：0存在 2删除',
  create_by        varchar(64)   default ''               comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)   default ''               comment '更新者',
  update_time      datetime                              comment '更新时间',
  remark           varchar(500)  default ''               comment '备注',
  primary key (category_id),
  unique key uk_product_category_code (category_code),
  unique key uk_product_category_parent_name (parent_id, category_name),
  key idx_product_category_parent (parent_id),
  key idx_product_category_ancestors (ancestors),
  key idx_product_category_status (status)
) engine=innodb auto_increment=1 comment='商品分类表';

create table if not exists product_attribute (
  attribute_id      bigint(20)   not null auto_increment comment '商品属性ID',
  attribute_code    varchar(64)  not null                comment '属性编码',
  attribute_name    varchar(128) not null                comment '属性名称',
  attribute_type    varchar(32)  not null                comment '属性类型',
  option_source     varchar(32)  not null default 'NONE' comment '选项来源',
  dict_type         varchar(100) default ''              comment '若依字典类型',
  unit              varchar(32)  default ''              comment '单位',
  value_precision   int          default null            comment '数值小数位',
  status            char(1)      not null default '0'    comment '状态：0正常 1停用',
  del_flag          char(1)      not null default '0'    comment '删除标志：0存在 2删除',
  create_by         varchar(64)  default ''              comment '创建者',
  create_time       datetime                             comment '创建时间',
  update_by         varchar(64)  default ''              comment '更新者',
  update_time       datetime                             comment '更新时间',
  remark            varchar(500) default ''              comment '备注',
  primary key (attribute_id),
  unique key uk_product_attribute_code (attribute_code),
  key idx_product_attribute_name (attribute_name),
  key idx_product_attribute_type (attribute_type),
  key idx_product_attribute_status (status)
) engine=innodb auto_increment=1 comment='商品属性库表';

create table if not exists product_attribute_option (
  option_id     bigint(20)   not null auto_increment comment '属性选项ID',
  attribute_id  bigint(20)   not null                comment '商品属性ID',
  option_code   varchar(64)  not null                comment '选项编码',
  option_label  varchar(128) not null                comment '选项名称',
  sort_order    int          not null default 0      comment '显示排序',
  default_flag  char(1)      not null default 'N'    comment '是否默认：Y是 N否',
  status        char(1)      not null default '0'    comment '状态：0正常 1停用',
  create_by     varchar(64)  default ''              comment '创建者',
  create_time   datetime                             comment '创建时间',
  update_by     varchar(64)  default ''              comment '更新者',
  update_time   datetime                             comment '更新时间',
  remark        varchar(500) default ''              comment '备注',
  primary key (option_id),
  unique key uk_product_attribute_option_code (attribute_id, option_code),
  key idx_product_attribute_option_attribute (attribute_id),
  key idx_product_attribute_option_status (status)
) engine=innodb auto_increment=1 comment='商品属性选项表';

create table if not exists product_category_attribute (
  category_attribute_id bigint(20)   not null auto_increment comment '类目属性配置ID',
  category_id           bigint(20)   not null                comment '商品分类ID',
  attribute_id          bigint(20)   not null                comment '商品属性ID',
  rule_mode             varchar(32)  not null default 'ADD'  comment '规则模式',
  required_flag         char(1)      not null default 'N'    comment '是否必填：Y是 N否',
  visible_flag          char(1)      not null default 'Y'    comment '是否展示：Y是 N否',
  editable_flag         char(1)      not null default 'Y'    comment '卖家是否可编辑：Y是 N否',
  filterable_flag       char(1)      not null default 'N'    comment '是否预留筛选：Y是 N否',
  group_code            varchar(64)  default ''              comment '表单分组',
  sort_order            int          not null default 0      comment '显示排序',
  placeholder           varchar(255) default ''              comment '输入提示',
  help_text             varchar(500) default ''              comment '帮助说明',
  validation_rule       text                                  comment '校验规则JSON',
  status                char(1)      not null default '0'    comment '状态：0正常 1停用',
  create_by             varchar(64)  default ''              comment '创建者',
  create_time           datetime                             comment '创建时间',
  update_by             varchar(64)  default ''              comment '更新者',
  update_time           datetime                             comment '更新时间',
  remark                varchar(500) default ''              comment '备注',
  primary key (category_attribute_id),
  unique key uk_product_category_attribute (category_id, attribute_id),
  key idx_product_category_attribute_category (category_id),
  key idx_product_category_attribute_attribute (attribute_id),
  key idx_product_category_attribute_mode (rule_mode),
  key idx_product_category_attribute_status (status)
) engine=innodb auto_increment=1 comment='类目属性配置表';

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '商品属性类型', 'product_attribute_type', '0', 'admin', sysdate(), '', null, '商品属性类型'
where not exists (select 1 from sys_dict_type where dict_type = 'product_attribute_type');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'product_attribute_type', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '商品属性类型'
from (
    select 1 as dict_sort, '文本' as dict_label, 'TEXT' as dict_value, 'default' as list_class, 'Y' as is_default
    union all select 2, '数字', 'NUMBER', 'default', 'N'
    union all select 3, '是否', 'BOOLEAN', 'primary', 'N'
    union all select 4, '单选', 'SINGLE_SELECT', 'success', 'N'
    union all select 5, '多选', 'MULTI_SELECT', 'success', 'N'
    union all select 6, '日期', 'DATE', 'warning', 'N'
    union all select 7, '文件', 'FILE', 'info', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'product_attribute_type' and d.dict_value = seed.dict_value);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '商品属性选项来源', 'product_attribute_option_source', '0', 'admin', sysdate(), '', null, '商品属性选项来源'
where not exists (select 1 from sys_dict_type where dict_type = 'product_attribute_option_source');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'product_attribute_option_source', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '商品属性选项来源'
from (
    select 1 as dict_sort, '无选项' as dict_label, 'NONE' as dict_value, 'default' as list_class, 'Y' as is_default
    union all select 2, '属性选项', 'ATTRIBUTE_OPTION', 'success', 'N'
    union all select 3, '若依字典', 'SYS_DICT', 'primary', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'product_attribute_option_source' and d.dict_value = seed.dict_value);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '类目属性规则模式', 'product_category_attribute_rule_mode', '0', 'admin', sysdate(), '', null, '类目属性规则模式'
where not exists (select 1 from sys_dict_type where dict_type = 'product_category_attribute_rule_mode');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'product_category_attribute_rule_mode', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '类目属性规则模式'
from (
    select 1 as dict_sort, '本类目新增' as dict_label, 'ADD' as dict_value, 'success' as list_class, 'Y' as is_default
    union all select 2, '本类目调整', 'OVERRIDE', 'primary', 'N'
    union all select 3, '本类目停用', 'DISABLE', 'warning', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'product_category_attribute_rule_mode' and d.dict_value = seed.dict_value);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '商品属性分组', 'product_attribute_group', '0', 'admin', sysdate(), '', null, '商品属性分组'
where not exists (select 1 from sys_dict_type where dict_type = 'product_attribute_group');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'product_attribute_group', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '商品属性分组'
from (
    select 1 as dict_sort, '基础信息' as dict_label, 'BASIC' as dict_value, 'default' as list_class, 'Y' as is_default
    union all select 2, '功能信息', 'FUNCTION', 'primary', 'N'
    union all select 3, '合规信息', 'COMPLIANCE', 'warning', 'N'
    union all select 4, '物流信息', 'LOGISTICS', 'info', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'product_attribute_group' and d.dict_value = seed.dict_value);

create temporary table if not exists tmp_product_category_attribute_sys_menu_guard (
  menu_id    bigint       not null,
  parent_id  bigint       not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  key idx_product_category_attribute_sys_menu_guard_id (menu_id)
) engine=memory;

truncate table tmp_product_category_attribute_sys_menu_guard;

insert into tmp_product_category_attribute_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (2060, 0, 'M', 'product', '', 'ProductManagement', ''),
    (2440, 2090, 'C', 'product-category', 'Product/Category/index', 'ProductCategoryConfig', 'product:category:list'),
    (2440, 2090, 'C', 'product-category', 'Common/PlannedPage/index', 'ProductCategoryConfig', 'basic:productCategory:list'),
    (2441, 2090, 'C', 'product-attribute', 'Product/Attribute/index', 'ProductAttributeConfig', 'product:attribute:list'),
    (2441, 2090, 'C', 'product-attribute', 'Common/PlannedPage/index', 'ProductAttributeConfig', 'basic:productAttribute:list'),
    (2470, 2440, 'F', '#', '', '', 'product:category:query'),
    (2471, 2440, 'F', '#', '', '', 'product:category:add'),
    (2472, 2440, 'F', '#', '', '', 'product:category:edit'),
    (2473, 2440, 'F', '#', '', '', 'product:category:remove'),
    (2474, 2441, 'F', '#', '', '', 'product:attribute:query'),
    (2475, 2441, 'F', '#', '', '', 'product:attribute:add'),
    (2476, 2441, 'F', '#', '', '', 'product:attribute:edit'),
    (2477, 2441, 'F', '#', '', '', 'product:attribute:remove'),
    (2478, 2441, 'F', '#', '', '', 'product:categoryAttribute:list'),
    (2479, 2441, 'F', '#', '', '', 'product:categoryAttribute:edit'),
    (2480, 2441, 'F', '#', '', '', 'product:categoryAttribute:preview');

call assert_product_category_attribute_sys_menu_guard();

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select 2060, '商品管理', 0, 10, 'product', null, '', 'ProductManagement',
       1, 0, 'M', '0', '0', '', 'ShoppingOutlined', 'admin',
       sysdate(), '', null, '顶级菜单：商品管理'
where not exists (select 1 from sys_menu where menu_id = 2060);

update sys_menu
set menu_name = '商品分类配置',
    parent_id = 2090,
    order_num = 5,
    path = 'product-category',
    component = 'Product/Category/index',
    route_name = 'ProductCategoryConfig',
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'product:category:list',
    icon = 'ApartmentOutlined',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '基础配置菜单：商品分类配置'
where menu_id = 2440;

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select 2440, '商品分类配置', 2090, 5, 'product-category', 'Product/Category/index', '', 'ProductCategoryConfig',
       1, 0, 'C', '0', '0', 'product:category:list', 'ApartmentOutlined', 'admin',
       sysdate(), '', null, '基础配置菜单：商品分类配置'
where not exists (select 1 from sys_menu where menu_id = 2440);

update sys_menu
set menu_name = '商品属性配置',
    parent_id = 2090,
    order_num = 10,
    path = 'product-attribute',
    component = 'Product/Attribute/index',
    route_name = 'ProductAttributeConfig',
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'product:attribute:list',
    icon = 'TagsOutlined',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '基础配置菜单：商品属性配置'
where menu_id = 2441;

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select 2441, '商品属性配置', 2090, 10, 'product-attribute', 'Product/Attribute/index', '', 'ProductAttributeConfig',
       1, 0, 'C', '0', '0', 'product:attribute:list', 'TagsOutlined', 'admin',
       sysdate(), '', null, '基础配置菜单：商品属性配置'
where not exists (select 1 from sys_menu where menu_id = 2441);

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_id, seed.menu_name, seed.parent_id, seed.order_num, '#', '', '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select 2470 as menu_id, '商品分类查询' as menu_name, 2440 as parent_id, 1 as order_num, 'product:category:query' as perms, '商品分类按钮：查询' as remark
    union all select 2471, '商品分类新增', 2440, 2, 'product:category:add', '商品分类按钮：新增'
    union all select 2472, '商品分类修改', 2440, 3, 'product:category:edit', '商品分类按钮：修改'
    union all select 2473, '商品分类删除', 2440, 4, 'product:category:remove', '商品分类按钮：删除'
    union all select 2474, '商品属性查询', 2441, 1, 'product:attribute:query', '商品属性按钮：查询'
    union all select 2475, '商品属性新增', 2441, 2, 'product:attribute:add', '商品属性按钮：新增'
    union all select 2476, '商品属性修改', 2441, 3, 'product:attribute:edit', '商品属性按钮：修改'
    union all select 2477, '商品属性删除', 2441, 4, 'product:attribute:remove', '商品属性按钮：删除'
    union all select 2478, '类目属性查看', 2441, 5, 'product:categoryAttribute:list', '类目属性按钮：查看'
    union all select 2479, '类目属性配置', 2441, 6, 'product:categoryAttribute:edit', '类目属性按钮：配置'
    union all select 2480, '类目属性预览', 2441, 7, 'product:categoryAttribute:preview', '类目属性按钮：预览'
) seed
where not exists (select 1 from sys_menu m where m.menu_id = seed.menu_id)
  and not exists (select 1 from sys_menu p where p.perms = seed.perms);

drop temporary table if exists tmp_product_category_attribute_sys_menu_guard;
drop procedure if exists assert_product_category_attribute_sys_menu_guard;
