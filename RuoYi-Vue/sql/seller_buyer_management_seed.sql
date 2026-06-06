-- Seller/buyer management seed for the RuoYi validation project.
-- Scope: seller/buyer modules, portal account bindings, dictionaries, menu entries, and permissions.

set names utf8mb4;

set @confirm_seller_buyer_management_seed := coalesce(@confirm_seller_buyer_management_seed, '');

delimiter //

drop procedure if exists assert_seller_buyer_management_seed_confirmed//
create procedure assert_seller_buyer_management_seed_confirmed()
begin
  if coalesce(@confirm_seller_buyer_management_seed, '') <> 'APPLY_SELLER_BUYER_MANAGEMENT_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_seller_buyer_management_seed = APPLY_SELLER_BUYER_MANAGEMENT_SEED before running this seed';
  end if;
end//

drop procedure if exists assert_seller_buyer_sys_menu_seed_guard//
create procedure assert_seller_buyer_sys_menu_seed_guard()
begin
  if exists (
    select 1
    from sys_menu m
    join tmp_seller_buyer_sys_menu_guard seed on seed.menu_id = m.menu_id
    where coalesce(m.path, '') <> coalesce(seed.path, '')
       or coalesce(m.component, '') <> coalesce(seed.component, '')
       or coalesce(m.route_name, '') <> coalesce(seed.route_name, '')
       or coalesce(m.perms, '') <> coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'seller/buyer sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_seller_buyer_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'seller/buyer sys_menu signature is already used by another menu';
  end if;
end//

delimiter ;

call assert_seller_buyer_management_seed_confirmed();

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
  seller_account_id     bigint(20)      not null auto_increment    comment '卖家端账号ID',
  seller_id             bigint(20)      not null                   comment '卖家ID',
  dept_id               bigint(20)      default null               comment '卖家端部门ID',
  user_name             varchar(30)     not null                   comment '登录账号',
  nick_name             varchar(30)     not null default ''        comment '姓名',
  password              varchar(100)    not null default ''        comment '密码密文',
  email                 varchar(50)     default ''                 comment '邮箱',
  phonenumber           varchar(32)     default ''                 comment '手机',
  account_role          varchar(32)     not null default 'OWNER'   comment '卖家侧账号角色',
  status                char(1)         not null default '0'       comment '账号状态：0正常 1停用',
  lock_status           char(1)         not null default '0'       comment '锁定状态：0未锁定 1已锁定',
  lock_reason           varchar(500)    not null default ''        comment '锁定原因',
  last_login_ip         varchar(128)    default ''                 comment '最后登录IP',
  last_login_time       datetime                                   comment '最后登录时间',
  pwd_update_time       datetime                                   comment '密码最后更新时间',
  owner_unique_seller_id bigint(20) generated always as (case when account_role = 'OWNER' then seller_id else null end) stored comment 'OWNER唯一约束辅助列',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (seller_account_id),
  unique key uk_seller_account_username (user_name),
  unique key uk_seller_account_owner (owner_unique_seller_id),
  key idx_seller_account_seller_status (seller_id, status),
  key idx_seller_account_seller_lock (seller_id, lock_status)
) engine=innodb auto_increment=1 comment = '卖家端账号表';

create table if not exists buyer_account (
  buyer_account_id      bigint(20)      not null auto_increment    comment '买家端账号ID',
  buyer_id              bigint(20)      not null                   comment '买家ID',
  dept_id               bigint(20)      default null               comment '买家端部门ID',
  user_name             varchar(30)     not null                   comment '登录账号',
  nick_name             varchar(30)     not null default ''        comment '姓名',
  password              varchar(100)    not null default ''        comment '密码密文',
  email                 varchar(50)     default ''                 comment '邮箱',
  phonenumber           varchar(32)     default ''                 comment '手机',
  account_role          varchar(32)     not null default 'OWNER'   comment '买家侧账号角色',
  status                char(1)         not null default '0'       comment '账号状态：0正常 1停用',
  lock_status           char(1)         not null default '0'       comment '锁定状态：0未锁定 1已锁定',
  lock_reason           varchar(500)    not null default ''        comment '锁定原因',
  last_login_ip         varchar(128)    default ''                 comment '最后登录IP',
  last_login_time       datetime                                   comment '最后登录时间',
  pwd_update_time       datetime                                   comment '密码最后更新时间',
  owner_unique_buyer_id bigint(20) generated always as (case when account_role = 'OWNER' then buyer_id else null end) stored comment 'OWNER唯一约束辅助列',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (buyer_account_id),
  unique key uk_buyer_account_username (user_name),
  unique key uk_buyer_account_owner (owner_unique_buyer_id),
  key idx_buyer_account_buyer_status (buyer_id, status),
  key idx_buyer_account_buyer_lock (buyer_id, lock_status)
) engine=innodb auto_increment=1 comment = '买家端账号表';

create table if not exists seller_dept (
  seller_dept_id        bigint(20)      not null auto_increment    comment '卖家端部门ID',
  seller_id             bigint(20)      not null                   comment '卖家ID',
  parent_id             bigint(20)      default 0                  comment '父部门ID',
  ancestors             varchar(500)    default ''                 comment '祖级列表',
  dept_name             varchar(100)    not null                   comment '部门名称',
  order_num             int             default 0                  comment '显示顺序',
  leader                varchar(100)    default ''                 comment '负责人',
  phone                 varchar(32)     default ''                 comment '联系电话',
  email                 varchar(128)    default ''                 comment '邮箱',
  status                char(1)         not null default '0'       comment '部门状态：0正常 1停用',
  del_flag              char(1)         not null default '0'       comment '删除标志：0存在 2删除',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  primary key (seller_dept_id),
  key idx_seller_dept_seller_parent (seller_id, parent_id),
  key idx_seller_dept_status (seller_id, status)
) engine=innodb auto_increment=1 comment = '卖家端部门表';

create table if not exists buyer_dept (
  buyer_dept_id         bigint(20)      not null auto_increment    comment '买家端部门ID',
  buyer_id              bigint(20)      not null                   comment '买家ID',
  parent_id             bigint(20)      default 0                  comment '父部门ID',
  ancestors             varchar(500)    default ''                 comment '祖级列表',
  dept_name             varchar(100)    not null                   comment '部门名称',
  order_num             int             default 0                  comment '显示顺序',
  leader                varchar(100)    default ''                 comment '负责人',
  phone                 varchar(32)     default ''                 comment '联系电话',
  email                 varchar(128)    default ''                 comment '邮箱',
  status                char(1)         not null default '0'       comment '部门状态：0正常 1停用',
  del_flag              char(1)         not null default '0'       comment '删除标志：0存在 2删除',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  primary key (buyer_dept_id),
  key idx_buyer_dept_buyer_parent (buyer_id, parent_id),
  key idx_buyer_dept_status (buyer_id, status)
) engine=innodb auto_increment=1 comment = '买家端部门表';

create table if not exists seller_role (
  seller_role_id        bigint(20)      not null auto_increment    comment '卖家端角色ID',
  seller_id             bigint(20)      not null                   comment '卖家ID',
  role_name             varchar(64)     not null                   comment '角色名称',
  role_key              varchar(64)     not null                   comment '权限字符',
  role_sort             int             not null default 0         comment '显示顺序',
  status                char(1)         not null default '0'       comment '角色状态：0正常 1停用',
  del_flag              char(1)         not null default '0'       comment '删除标志：0存在 2删除',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (seller_role_id),
  unique key uk_seller_role_key (seller_id, role_key),
  key idx_seller_role_status (seller_id, status)
) engine=innodb auto_increment=1 comment = '卖家端角色表';

create table if not exists buyer_role (
  buyer_role_id         bigint(20)      not null auto_increment    comment '买家端角色ID',
  buyer_id              bigint(20)      not null                   comment '买家ID',
  role_name             varchar(64)     not null                   comment '角色名称',
  role_key              varchar(64)     not null                   comment '权限字符',
  role_sort             int             not null default 0         comment '显示顺序',
  status                char(1)         not null default '0'       comment '角色状态：0正常 1停用',
  del_flag              char(1)         not null default '0'       comment '删除标志：0存在 2删除',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (buyer_role_id),
  unique key uk_buyer_role_key (buyer_id, role_key),
  key idx_buyer_role_status (buyer_id, status)
) engine=innodb auto_increment=1 comment = '买家端角色表';

create table if not exists seller_menu (
  seller_menu_id        bigint(20)      not null auto_increment    comment '卖家端菜单ID',
  menu_name             varchar(64)     not null                   comment '菜单名称',
  parent_id             bigint(20)      default 0                  comment '父菜单ID',
  order_num             int             default 0                  comment '显示顺序',
  path                  varchar(200)    default ''                 comment '路由地址',
  component             varchar(255)    default null               comment '组件路径',
  query                 varchar(255)    default ''                 comment '路由参数',
  route_name            varchar(64)     default ''                 comment '路由名称',
  is_frame              int             default 1                  comment '是否外链',
  is_cache              int             default 0                  comment '是否缓存',
  menu_type             char(1)         not null default 'M'       comment '菜单类型',
  visible               char(1)         not null default '0'       comment '显示状态',
  status                char(1)         not null default '0'       comment '菜单状态',
  perms                 varchar(100)    default ''                 comment '权限标识',
  icon                  varchar(100)    default '#'                comment '菜单图标',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (seller_menu_id),
  key idx_seller_menu_parent (parent_id),
  key idx_seller_menu_status (status)
) engine=innodb auto_increment=1 comment = '卖家端菜单权限表';

create table if not exists buyer_menu (
  buyer_menu_id         bigint(20)      not null auto_increment    comment '买家端菜单ID',
  menu_name             varchar(64)     not null                   comment '菜单名称',
  parent_id             bigint(20)      default 0                  comment '父菜单ID',
  order_num             int             default 0                  comment '显示顺序',
  path                  varchar(200)    default ''                 comment '路由地址',
  component             varchar(255)    default null               comment '组件路径',
  query                 varchar(255)    default ''                 comment '路由参数',
  route_name            varchar(64)     default ''                 comment '路由名称',
  is_frame              int             default 1                  comment '是否外链',
  is_cache              int             default 0                  comment '是否缓存',
  menu_type             char(1)         not null default 'M'       comment '菜单类型',
  visible               char(1)         not null default '0'       comment '显示状态',
  status                char(1)         not null default '0'       comment '菜单状态',
  perms                 varchar(100)    default ''                 comment '权限标识',
  icon                  varchar(100)    default '#'                comment '菜单图标',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (buyer_menu_id),
  key idx_buyer_menu_parent (parent_id),
  key idx_buyer_menu_status (status)
) engine=innodb auto_increment=1 comment = '买家端菜单权限表';

create table if not exists seller_account_role (
  seller_account_id     bigint(20)      not null                   comment '卖家端账号ID',
  seller_role_id        bigint(20)      not null                   comment '卖家端角色ID',
  primary key (seller_account_id, seller_role_id)
) engine=innodb comment = '卖家端账号角色关联表';

create table if not exists buyer_account_role (
  buyer_account_id      bigint(20)      not null                   comment '买家端账号ID',
  buyer_role_id         bigint(20)      not null                   comment '买家端角色ID',
  primary key (buyer_account_id, buyer_role_id)
) engine=innodb comment = '买家端账号角色关联表';

create table if not exists seller_role_menu (
  seller_role_id        bigint(20)      not null                   comment '卖家端角色ID',
  seller_menu_id        bigint(20)      not null                   comment '卖家端菜单ID',
  primary key (seller_role_id, seller_menu_id)
) engine=innodb comment = '卖家端角色菜单关联表';

create table if not exists buyer_role_menu (
  buyer_role_id         bigint(20)      not null                   comment '买家端角色ID',
  buyer_menu_id         bigint(20)      not null                   comment '买家端菜单ID',
  primary key (buyer_role_id, buyer_menu_id)
) engine=innodb comment = '买家端角色菜单关联表';

create table if not exists seller_login_log (
  info_id               bigint(20)      not null auto_increment    comment '访问ID',
  seller_id             bigint(20)      default null               comment '卖家ID',
  seller_account_id     bigint(20)      default null               comment '卖家端账号ID',
  user_name             varchar(30)     default ''                 comment '登录账号',
  ipaddr                varchar(128)    default ''                 comment '登录IP',
  login_location        varchar(255)    default ''                 comment '登录地点',
  browser               varchar(50)     default ''                 comment '浏览器',
  os                    varchar(50)     default ''                 comment '操作系统',
  status                char(1)         default '0'                comment '登录状态：0成功 1失败',
  msg                   varchar(255)    default ''                 comment '提示消息',
  login_time            datetime                                   comment '访问时间',
  primary key (info_id),
  key idx_seller_login_log_account_time (seller_account_id, login_time),
  key idx_seller_login_log_seller_time (seller_id, login_time)
) engine=innodb auto_increment=1 comment = '卖家端登录日志表';

create table if not exists buyer_login_log (
  info_id               bigint(20)      not null auto_increment    comment '访问ID',
  buyer_id              bigint(20)      default null               comment '买家ID',
  buyer_account_id      bigint(20)      default null               comment '买家端账号ID',
  user_name             varchar(30)     default ''                 comment '登录账号',
  ipaddr                varchar(128)    default ''                 comment '登录IP',
  login_location        varchar(255)    default ''                 comment '登录地点',
  browser               varchar(50)     default ''                 comment '浏览器',
  os                    varchar(50)     default ''                 comment '操作系统',
  status                char(1)         default '0'                comment '登录状态：0成功 1失败',
  msg                   varchar(255)    default ''                 comment '提示消息',
  login_time            datetime                                   comment '访问时间',
  primary key (info_id),
  key idx_buyer_login_log_account_time (buyer_account_id, login_time),
  key idx_buyer_login_log_buyer_time (buyer_id, login_time)
) engine=innodb auto_increment=1 comment = '买家端登录日志表';

create table if not exists seller_oper_log (
  oper_id               bigint(20)      not null auto_increment    comment '日志主键',
  seller_id             bigint(20)      default null               comment '卖家ID',
  seller_account_id     bigint(20)      default null               comment '卖家端账号ID',
  title                 varchar(50)     default ''                 comment '模块标题',
  business_type         int             default 0                  comment '业务类型',
  method                varchar(200)    default ''                 comment '方法名称',
  request_method        varchar(10)     default ''                 comment '请求方式',
  oper_name             varchar(30)     default ''                 comment '操作人员',
  oper_url              varchar(255)    default ''                 comment '请求URL',
  oper_ip               varchar(128)    default ''                 comment '主机地址',
  oper_location         varchar(255)    default ''                 comment '操作地点',
  oper_param            varchar(2000)   default ''                 comment '请求参数',
  json_result           varchar(2000)   default ''                 comment '返回参数',
  status                int             default 0                  comment '操作状态',
  error_msg             varchar(2000)   default ''                 comment '错误消息',
  oper_time             datetime                                   comment '操作时间',
  cost_time             bigint(20)      default 0                  comment '消耗时间',
  primary key (oper_id),
  key idx_seller_oper_log_account_time (seller_account_id, oper_time),
  key idx_seller_oper_log_seller_time (seller_id, oper_time)
) engine=innodb auto_increment=1 comment = '卖家端操作日志表';

create table if not exists buyer_oper_log (
  oper_id               bigint(20)      not null auto_increment    comment '日志主键',
  buyer_id              bigint(20)      default null               comment '买家ID',
  buyer_account_id      bigint(20)      default null               comment '买家端账号ID',
  title                 varchar(50)     default ''                 comment '模块标题',
  business_type         int             default 0                  comment '业务类型',
  method                varchar(200)    default ''                 comment '方法名称',
  request_method        varchar(10)     default ''                 comment '请求方式',
  oper_name             varchar(30)     default ''                 comment '操作人员',
  oper_url              varchar(255)    default ''                 comment '请求URL',
  oper_ip               varchar(128)    default ''                 comment '主机地址',
  oper_location         varchar(255)    default ''                 comment '操作地点',
  oper_param            varchar(2000)   default ''                 comment '请求参数',
  json_result           varchar(2000)   default ''                 comment '返回参数',
  status                int             default 0                  comment '操作状态',
  error_msg             varchar(2000)   default ''                 comment '错误消息',
  oper_time             datetime                                   comment '操作时间',
  cost_time             bigint(20)      default 0                  comment '消耗时间',
  primary key (oper_id),
  key idx_buyer_oper_log_account_time (buyer_account_id, oper_time),
  key idx_buyer_oper_log_buyer_time (buyer_id, oper_time)
) engine=innodb auto_increment=1 comment = '买家端操作日志表';

create table if not exists seller_session (
  token_id              varchar(64)     not null                   comment '会话ID',
  seller_id             bigint(20)      not null                   comment '卖家ID',
  seller_account_id     bigint(20)      not null                   comment '卖家端账号ID',
  user_name             varchar(30)     not null                   comment '登录账号',
  login_ip              varchar(128)    default ''                 comment '登录IP',
  login_time            datetime                                   comment '登录时间',
  expire_time           datetime                                   comment '过期时间',
  logout_time           datetime                                   comment '登出时间',
  status                char(1)         not null default '0'       comment '状态：0有效 1失效',
  direct_login          tinyint(1)      not null default 0         comment '是否免密代入',
  direct_login_ticket_id bigint(20)     default null               comment '免密代入票据ID',
  acting_admin_id       bigint(20)      default null               comment '代入管理员ID',
  acting_admin_name     varchar(64)     default ''                 comment '代入管理员账号',
  direct_login_reason   varchar(255)    default ''                 comment '免密代入原因',
  primary key (token_id),
  key idx_seller_session_account (seller_account_id),
  key idx_seller_session_expire (expire_time)
) engine=innodb comment = '卖家端会话表';

create table if not exists buyer_session (
  token_id              varchar(64)     not null                   comment '会话ID',
  buyer_id              bigint(20)      not null                   comment '买家ID',
  buyer_account_id      bigint(20)      not null                   comment '买家端账号ID',
  user_name             varchar(30)     not null                   comment '登录账号',
  login_ip              varchar(128)    default ''                 comment '登录IP',
  login_time            datetime                                   comment '登录时间',
  expire_time           datetime                                   comment '过期时间',
  logout_time           datetime                                   comment '登出时间',
  status                char(1)         not null default '0'       comment '状态：0有效 1失效',
  direct_login          tinyint(1)      not null default 0         comment '是否免密代入',
  direct_login_ticket_id bigint(20)     default null               comment '免密代入票据ID',
  acting_admin_id       bigint(20)      default null               comment '代入管理员ID',
  acting_admin_name     varchar(64)     default ''                 comment '代入管理员账号',
  direct_login_reason   varchar(255)    default ''                 comment '免密代入原因',
  primary key (token_id),
  key idx_buyer_session_account (buyer_account_id),
  key idx_buyer_session_expire (expire_time)
) engine=innodb comment = '买家端会话表';

create table if not exists portal_direct_login_ticket (
  ticket_id             bigint(20)      not null auto_increment    comment '免密代入票据ID',
  terminal              varchar(20)     not null                   comment '目标端：seller/buyer',
  target_subject_id     bigint(20)      not null                   comment '目标主体ID',
  target_subject_no     varchar(64)     default ''                 comment '目标主体内部编号',
  target_account_id     bigint(20)      not null                   comment '目标端账号ID',
  target_user_name      varchar(64)     not null                   comment '目标登录账号',
  acting_admin_id       bigint(20)      not null                   comment '代入管理员ID',
  acting_admin_name     varchar(64)     not null                   comment '代入管理员账号',
  reason                varchar(255)    default ''                 comment '代入原因',
  token_hash            varchar(64)     not null                   comment '免密token SHA-256哈希',
  expire_time           datetime        not null                   comment '过期时间',
  used_time             datetime        default null               comment '使用时间',
  used_ip               varchar(128)    default ''                 comment '使用IP',
  status                varchar(20)     not null default 'ISSUED'  comment '状态：ISSUED/USED/EXPIRED/CANCELLED',
  create_by             varchar(64)     default ''                 comment '创建者',
  create_time           datetime                                   comment '创建时间',
  update_by             varchar(64)     default ''                 comment '更新者',
  update_time           datetime                                   comment '更新时间',
  remark                varchar(500)    default ''                 comment '备注',
  primary key (ticket_id),
  unique key uk_portal_direct_login_ticket_hash (token_hash),
  key idx_portal_direct_login_ticket_target (terminal, target_subject_id, target_account_id),
  key idx_portal_direct_login_ticket_admin_time (acting_admin_id, create_time),
  key idx_portal_direct_login_ticket_status_expire (status, expire_time)
) engine=innodb auto_increment=1 comment = '管理端免密代入审计票据表';

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
select '卖家账号锁定状态', 'seller_account_lock_status', '0', 'admin', sysdate(), '', null, '卖家账号锁定状态'
where not exists (select 1 from sys_dict_type where dict_type = 'seller_account_lock_status');

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '买家账号角色', 'buyer_account_role', '0', 'admin', sysdate(), '', null, '买家账号角色'
where not exists (select 1 from sys_dict_type where dict_type = 'buyer_account_role');

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '买家账号锁定状态', 'buyer_account_lock_status', '0', 'admin', sysdate(), '', null, '买家账号锁定状态'
where not exists (select 1 from sys_dict_type where dict_type = 'buyer_account_lock_status');

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

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'seller_account_lock_status', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '卖家账号锁定状态'
from (
    select 1 as dict_sort, '未锁定' as dict_label, '0' as dict_value, 'Y' as is_default, 'success' as list_class
    union all select 2, '已锁定', '1', 'N', 'danger'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'seller_account_lock_status' and d.dict_value = seed.dict_value
);

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'buyer_account_lock_status', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '买家账号锁定状态'
from (
    select 1 as dict_sort, '未锁定' as dict_label, '0' as dict_value, 'Y' as is_default, 'success' as list_class
    union all select 2, '已锁定', '1', 'N', 'danger'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'buyer_account_lock_status' and d.dict_value = seed.dict_value
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

drop temporary table if exists tmp_seller_buyer_sys_menu_guard;
create temporary table tmp_seller_buyer_sys_menu_guard (
  menu_id bigint not null primary key,
  path varchar(200) not null default '',
  component varchar(255) not null default '',
  route_name varchar(50) not null default '',
  perms varchar(100) not null default ''
) engine=memory;

insert into tmp_seller_buyer_sys_menu_guard (menu_id, path, component, route_name, perms)
values
    (2010, 'partner', '', 'PartnerManagement', ''),
    (2011, 'seller', 'Seller/index', 'Seller', 'seller:admin:list'),
    (2012, 'buyer', 'Buyer/index', 'Buyer', 'buyer:admin:list'),
    (2200, '#', '', '', 'seller:admin:query'),
    (2201, '#', '', '', 'seller:admin:add'),
    (2202, '#', '', '', 'seller:admin:edit'),
    (2203, '#', '', '', 'seller:admin:changeStatus'),
    (2204, '#', '', '', 'seller:admin:resetPwd'),
    (2205, '#', '', '', 'seller:admin:directLogin'),
    (2206, '#', '', '', 'seller:admin:forceLogout'),
    (2210, '#', '', '', 'buyer:admin:query'),
    (2211, '#', '', '', 'buyer:admin:add'),
    (2212, '#', '', '', 'buyer:admin:edit'),
    (2213, '#', '', '', 'buyer:admin:changeStatus'),
    (2214, '#', '', '', 'buyer:admin:resetPwd'),
    (2215, '#', '', '', 'buyer:admin:directLogin'),
    (2216, '#', '', '', 'buyer:admin:forceLogout'),
    (2220, '#', '', '', 'seller:admin:menu:list'),
    (2221, '#', '', '', 'seller:admin:menu:query'),
    (2222, '#', '', '', 'seller:admin:menu:add'),
    (2223, '#', '', '', 'seller:admin:menu:edit'),
    (2224, '#', '', '', 'seller:admin:menu:remove'),
    (2225, '#', '', '', 'seller:admin:role:list'),
    (2226, '#', '', '', 'seller:admin:role:query'),
    (2227, '#', '', '', 'seller:admin:role:add'),
    (2228, '#', '', '', 'seller:admin:role:edit'),
    (2229, '#', '', '', 'seller:admin:role:remove'),
    (2230, '#', '', '', 'buyer:admin:menu:list'),
    (2231, '#', '', '', 'buyer:admin:menu:query'),
    (2232, '#', '', '', 'buyer:admin:menu:add'),
    (2233, '#', '', '', 'buyer:admin:menu:edit'),
    (2234, '#', '', '', 'buyer:admin:menu:remove'),
    (2235, '#', '', '', 'buyer:admin:role:list'),
    (2236, '#', '', '', 'buyer:admin:role:query'),
    (2237, '#', '', '', 'buyer:admin:role:add'),
    (2238, '#', '', '', 'buyer:admin:role:edit'),
    (2239, '#', '', '', 'buyer:admin:role:remove'),
    (2240, '#', '', '', 'seller:admin:dept:list'),
    (2241, '#', '', '', 'seller:admin:dept:query'),
    (2242, '#', '', '', 'seller:admin:dept:add'),
    (2243, '#', '', '', 'seller:admin:dept:edit'),
    (2244, '#', '', '', 'seller:admin:dept:remove'),
    (2245, '#', '', '', 'buyer:admin:dept:list'),
    (2246, '#', '', '', 'buyer:admin:dept:query'),
    (2247, '#', '', '', 'buyer:admin:dept:add'),
    (2248, '#', '', '', 'buyer:admin:dept:edit'),
    (2249, '#', '', '', 'buyer:admin:dept:remove'),
    (2250, '#', '', '', 'seller:admin:loginLog:list'),
    (2251, '#', '', '', 'seller:admin:operLog:list'),
    (2252, '#', '', '', 'seller:admin:ticket:list'),
    (2310, '#', '', '', 'seller:admin:account:list'),
    (2311, '#', '', '', 'seller:admin:account:add'),
    (2312, '#', '', '', 'seller:admin:account:edit'),
    (2322, '#', '', '', 'seller:admin:account:lock'),
    (2313, '#', '', '', 'seller:admin:account:resetPwd'),
    (2314, '#', '', '', 'seller:admin:account:role:query'),
    (2315, '#', '', '', 'seller:admin:account:role:edit'),
    (2253, '#', '', '', 'buyer:admin:loginLog:list'),
    (2254, '#', '', '', 'buyer:admin:operLog:list'),
    (2255, '#', '', '', 'buyer:admin:ticket:list'),
    (2316, '#', '', '', 'buyer:admin:account:list'),
    (2317, '#', '', '', 'buyer:admin:account:add'),
    (2318, '#', '', '', 'buyer:admin:account:edit'),
    (2323, '#', '', '', 'buyer:admin:account:lock'),
    (2319, '#', '', '', 'buyer:admin:account:resetPwd'),
    (2320, '#', '', '', 'buyer:admin:account:role:query'),
    (2321, '#', '', '', 'buyer:admin:account:role:edit');

call assert_seller_buyer_sys_menu_seed_guard();

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2010, '主体管理', 0, 5, 'partner', null, '', 'PartnerManagement',
     1, 0, 'M', '0', '0', '', 'TeamOutlined', 'admin',
     sysdate(), '', null, '顶级菜单：主体管理'),
    (2011, '卖家管理', 2010, 5, 'seller', 'Seller/index', '', 'Seller',
     1, 0, 'C', '0', '0', 'seller:admin:list', 'ShopOutlined', 'admin',
     sysdate(), '', null, '管理端卖家管理'),
    (2012, '买家管理', 2010, 10, 'buyer', 'Buyer/index', '', 'Buyer',
     1, 0, 'C', '0', '0', 'buyer:admin:list', 'UserOutlined', 'admin',
     sysdate(), '', null, '管理端买家管理'),
    (2200, '卖家查询', 2011, 5, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2201, '卖家新增', 2011, 10, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2202, '卖家修改', 2011, 15, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2203, '卖家启停', 2011, 20, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:changeStatus', '#', 'admin',
     sysdate(), '', null, ''),
    (2204, '卖家重置密码', 2011, 25, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:resetPwd', '#', 'admin',
     sysdate(), '', null, ''),
    (2205, '卖家免密登录', 2011, 30, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:directLogin', '#', 'admin',
     sysdate(), '', null, ''),
    (2206, '卖家强制踢出', 2011, 32, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:forceLogout', '#', 'admin',
     sysdate(), '', null, ''),
    (2210, '买家查询', 2012, 5, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2211, '买家新增', 2012, 10, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2212, '买家修改', 2012, 15, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2213, '买家启停', 2012, 20, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:changeStatus', '#', 'admin',
     sysdate(), '', null, ''),
    (2214, '买家重置密码', 2012, 25, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:resetPwd', '#', 'admin',
     sysdate(), '', null, ''),
    (2215, '买家免密登录', 2012, 30, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:directLogin', '#', 'admin',
     sysdate(), '', null, ''),
    (2216, '买家强制踢出', 2012, 32, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:forceLogout', '#', 'admin',
     sysdate(), '', null, ''),
    (2220, '卖家端菜单列表', 2011, 35, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:menu:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2221, '卖家端菜单查询', 2011, 40, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:menu:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2222, '卖家端菜单新增', 2011, 45, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:menu:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2223, '卖家端菜单修改', 2011, 50, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:menu:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2224, '卖家端菜单删除', 2011, 55, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:menu:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2225, '卖家端角色列表', 2011, 60, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:role:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2226, '卖家端角色查询', 2011, 65, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:role:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2227, '卖家端角色新增', 2011, 70, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:role:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2228, '卖家端角色修改', 2011, 75, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:role:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2229, '卖家端角色删除', 2011, 80, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:role:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2230, '买家端菜单列表', 2012, 35, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:menu:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2231, '买家端菜单查询', 2012, 40, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:menu:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2232, '买家端菜单新增', 2012, 45, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:menu:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2233, '买家端菜单修改', 2012, 50, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:menu:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2234, '买家端菜单删除', 2012, 55, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:menu:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2235, '买家端角色列表', 2012, 60, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:role:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2236, '买家端角色查询', 2012, 65, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:role:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2237, '买家端角色新增', 2012, 70, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:role:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2238, '买家端角色修改', 2012, 75, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:role:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2239, '买家端角色删除', 2012, 80, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:role:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2240, '卖家端部门列表', 2011, 85, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:dept:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2241, '卖家端部门查询', 2011, 90, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:dept:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2242, '卖家端部门新增', 2011, 95, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:dept:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2243, '卖家端部门修改', 2011, 100, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:dept:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2244, '卖家端部门删除', 2011, 105, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:dept:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2245, '买家端部门列表', 2012, 85, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:dept:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2246, '买家端部门查询', 2012, 90, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:dept:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2247, '买家端部门新增', 2012, 95, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:dept:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2248, '买家端部门修改', 2012, 100, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:dept:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2249, '买家端部门删除', 2012, 105, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:dept:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2250, '卖家登录日志列表', 2011, 110, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:loginLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2251, '卖家操作日志列表', 2011, 115, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:operLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2252, '卖家免密票据列表', 2011, 120, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:ticket:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2310, '卖家账号列表', 2011, 125, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2311, '卖家账号新增', 2011, 130, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2312, '卖家账号修改', 2011, 135, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2322, '卖家账号锁定解锁', 2011, 138, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:lock', '#', 'admin',
     sysdate(), '', null, ''),
    (2313, '卖家账号重置密码', 2011, 140, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:resetPwd', '#', 'admin',
     sysdate(), '', null, ''),
    (2314, '卖家账号角色查询', 2011, 145, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:role:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2315, '卖家账号角色分配', 2011, 150, '#', '', '', '',
     1, 0, 'F', '0', '0', 'seller:admin:account:role:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2253, '买家登录日志列表', 2012, 110, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:loginLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2254, '买家操作日志列表', 2012, 115, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:operLog:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2255, '买家免密票据列表', 2012, 120, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:ticket:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2316, '买家账号列表', 2012, 125, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:list', '#', 'admin',
     sysdate(), '', null, ''),
    (2317, '买家账号新增', 2012, 130, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2318, '买家账号修改', 2012, 135, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2323, '买家账号锁定解锁', 2012, 138, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:lock', '#', 'admin',
     sysdate(), '', null, ''),
    (2319, '买家账号重置密码', 2012, 140, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:resetPwd', '#', 'admin',
     sysdate(), '', null, ''),
    (2320, '买家账号角色查询', 2012, 145, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:role:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2321, '买家账号角色分配', 2012, 150, '#', '', '', '',
     1, 0, 'F', '0', '0', 'buyer:admin:account:role:edit', '#', 'admin',
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

insert into sys_config
    (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select '卖家端前端地址', 'portal.seller.web.url', 'http://127.0.0.1:8001/seller/direct-login',
       'Y', 'admin', sysdate(), '', null, '管理端免密登录卖家端地址，占位配置'
where not exists (select 1 from sys_config where config_key = 'portal.seller.web.url');

insert into sys_config
    (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select '买家端前端地址', 'portal.buyer.web.url', 'http://127.0.0.1:8001/buyer/direct-login',
       'Y', 'admin', sysdate(), '', null, '管理端免密登录买家端地址，占位配置'
where not exists (select 1 from sys_config where config_key = 'portal.buyer.web.url');

-- 端内门户默认角色与权限初始化
-- 保证只执行综合 seed 的新环境也具备 seller/buyer 端内只读权限和 Owner 角色授权。

insert into seller_role
    (seller_id, role_name, role_key, role_sort, status, del_flag,
     create_by, create_time, update_by, update_time, remark)
select s.seller_id, 'Owner', 'owner', 1, '0', '0',
       'admin', sysdate(), '', null, '默认卖家端 Owner 角色'
from seller s
where s.status = '0'
  and not exists (
      select 1
      from seller_role r
      where r.seller_id = s.seller_id
        and r.role_key = 'owner'
        and r.del_flag = '0'
  );

insert into buyer_role
    (buyer_id, role_name, role_key, role_sort, status, del_flag,
     create_by, create_time, update_by, update_time, remark)
select b.buyer_id, 'Owner', 'owner', 1, '0', '0',
       'admin', sysdate(), '', null, '默认买家端 Owner 角色'
from buyer b
where b.status = '0'
  and not exists (
      select 1
      from buyer_role r
      where r.buyer_id = b.buyer_id
        and r.role_key = 'owner'
        and r.del_flag = '0'
  );

insert into seller_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_name, 0, seed.order_num, '', null, '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select '账号列表' as menu_name, 30 as order_num, 'seller:account:list' as perms, '卖家端账号只读列表权限' as remark
    union all select '部门列表', 40, 'seller:dept:list', '卖家端部门只读列表权限'
    union all select '角色列表', 41, 'seller:role:list', '卖家端角色只读列表权限'
    union all select 'Product Category List', 50, 'seller:product:category:list', 'Seller portal product category list permission'
    union all select 'Product Schema Query', 51, 'seller:product:schema:query', 'Seller portal product schema read permission'
    union all select 'Distribution Product List', 52, 'seller:product:distribution:list', 'Seller portal own distribution product list permission'
    union all select 'Distribution Product Query', 53, 'seller:product:distribution:query', 'Seller portal own distribution product read permission'
) seed
where not exists (
    select 1 from seller_menu m where m.perms = seed.perms
);

insert into buyer_menu
    (menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_name, 0, seed.order_num, '', null, '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select '账号列表' as menu_name, 30 as order_num, 'buyer:account:list' as perms, '买家端账号只读列表权限' as remark
    union all select '部门列表', 40, 'buyer:dept:list', '买家端部门只读列表权限'
    union all select '角色列表', 41, 'buyer:role:list', '买家端角色只读列表权限'
    union all select 'Product Category List', 50, 'buyer:product:category:list', 'Buyer portal product category list permission'
    union all select 'Product Schema Query', 51, 'buyer:product:schema:query', 'Buyer portal product schema read permission'
    union all select 'Distribution Product List', 52, 'buyer:product:distribution:list', 'Buyer portal distribution product list permission'
    union all select 'Distribution Product Query', 53, 'buyer:product:distribution:query', 'Buyer portal distribution product read permission'
) seed
where not exists (
    select 1 from buyer_menu m where m.perms = seed.perms
);

insert into seller_account_role (seller_account_id, seller_role_id)
select a.seller_account_id, r.seller_role_id
from seller_account a
join seller_role r on r.seller_id = a.seller_id
                  and r.role_key = 'owner'
                  and r.del_flag = '0'
where a.account_role = 'OWNER'
  and not exists (
      select 1
      from seller_account_role ar
      where ar.seller_account_id = a.seller_account_id
        and ar.seller_role_id = r.seller_role_id
  );

insert into buyer_account_role (buyer_account_id, buyer_role_id)
select a.buyer_account_id, r.buyer_role_id
from buyer_account a
join buyer_role r on r.buyer_id = a.buyer_id
                 and r.role_key = 'owner'
                 and r.del_flag = '0'
where a.account_role = 'OWNER'
  and not exists (
      select 1
      from buyer_account_role ar
      where ar.buyer_account_id = a.buyer_account_id
        and ar.buyer_role_id = r.buyer_role_id
  );

insert into seller_role_menu (seller_role_id, seller_menu_id)
select r.seller_role_id, m.seller_menu_id
from seller_role r
join seller_menu m on m.perms in (
    'seller:account:list',
    'seller:dept:list',
    'seller:role:list',
    'seller:product:category:list',
    'seller:product:schema:query',
    'seller:product:distribution:list',
    'seller:product:distribution:query'
)
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and not exists (
      select 1
      from seller_role_menu rm
      where rm.seller_role_id = r.seller_role_id
        and rm.seller_menu_id = m.seller_menu_id
  );

insert into buyer_role_menu (buyer_role_id, buyer_menu_id)
select r.buyer_role_id, m.buyer_menu_id
from buyer_role r
join buyer_menu m on m.perms in (
    'buyer:account:list',
    'buyer:dept:list',
    'buyer:role:list',
    'buyer:product:category:list',
    'buyer:product:schema:query',
    'buyer:product:distribution:list',
    'buyer:product:distribution:query'
)
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and not exists (
      select 1
      from buyer_role_menu rm
      where rm.buyer_role_id = r.buyer_role_id
        and rm.buyer_menu_id = m.buyer_menu_id
  );

drop temporary table if exists tmp_seller_buyer_sys_menu_guard;
drop procedure if exists assert_seller_buyer_management_seed_confirmed;
drop procedure if exists assert_seller_buyer_sys_menu_seed_guard;
