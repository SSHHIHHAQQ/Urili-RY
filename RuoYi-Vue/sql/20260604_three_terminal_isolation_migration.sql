-- Three-terminal isolation migration for seller/buyer portal accounts.
-- Confirmed scope: remote DDL/DML is allowed for this task.
-- Admin remains on RuoYi sys_*; seller/buyer portal accounts use independent terminal tables.

set names utf8mb4;

delimiter //

drop procedure if exists add_column_if_missing//
create procedure add_column_if_missing(in p_table varchar(64), in p_column varchar(64), in p_definition text)
begin
  if not exists (
    select 1 from information_schema.columns
    where table_schema = database() and table_name = p_table and column_name = p_column
  ) then
    set @ddl = concat('alter table ', p_table, ' add column ', p_column, ' ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists drop_column_if_exists//
create procedure drop_column_if_exists(in p_table varchar(64), in p_column varchar(64))
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = database() and table_name = p_table and column_name = p_column
  ) then
    set @ddl = concat('alter table ', p_table, ' drop column ', p_column);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists drop_index_if_exists//
create procedure drop_index_if_exists(in p_table varchar(64), in p_index varchar(64))
begin
  if exists (
    select 1 from information_schema.statistics
    where table_schema = database() and table_name = p_table and index_name = p_index
  ) then
    set @ddl = concat('alter table ', p_table, ' drop index ', p_index);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists add_index_if_missing//
create procedure add_index_if_missing(in p_table varchar(64), in p_index varchar(64), in p_definition text)
begin
  if not exists (
    select 1 from information_schema.statistics
    where table_schema = database() and table_name = p_table and index_name = p_index
  ) then
    set @ddl = concat('alter table ', p_table, ' add ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_no_duplicate_owner_account//
create procedure assert_no_duplicate_owner_account(in p_table varchar(64), in p_subject_column varchar(64), in p_message varchar(128))
begin
  set @duplicate_owner_count = 0;
  set @sql = concat('select count(*) into @duplicate_owner_count from (select ', p_subject_column,
      ' from ', p_table, ' where account_role = ''OWNER'' group by ', p_subject_column, ' having count(*) > 1) t');
  prepare stmt from @sql;
  execute stmt;
  deallocate prepare stmt;
  if @duplicate_owner_count > 0 then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

delimiter ;

create table if not exists seller_account (
  seller_account_id     bigint(20)      not null auto_increment,
  seller_id             bigint(20)      not null,
  dept_id               bigint(20)      default null,
  user_name             varchar(30)     not null,
  nick_name             varchar(30)     not null default '',
  password              varchar(100)    not null default '',
  email                 varchar(50)     default '',
  phonenumber           varchar(32)     default '',
  account_role          varchar(32)     not null default 'OWNER',
  status                char(1)         not null default '0',
  lock_status           char(1)         not null default '0',
  lock_reason           varchar(500)    not null default '',
  last_login_ip         varchar(128)    default '',
  last_login_time       datetime,
  pwd_update_time       datetime,
  owner_unique_seller_id bigint(20) generated always as (case when account_role = 'OWNER' then seller_id else null end) stored,
  create_by             varchar(64)     default '',
  create_time           datetime,
  update_by             varchar(64)     default '',
  update_time           datetime,
  remark                varchar(500)    default '',
  primary key (seller_account_id),
  unique key uk_seller_account_username (user_name),
  unique key uk_seller_account_owner (owner_unique_seller_id),
  key idx_seller_account_seller_status (seller_id, status),
  key idx_seller_account_seller_lock (seller_id, lock_status)
) engine=innodb auto_increment=1 comment = '卖家端账号表';

create table if not exists buyer_account (
  buyer_account_id      bigint(20)      not null auto_increment,
  buyer_id              bigint(20)      not null,
  dept_id               bigint(20)      default null,
  user_name             varchar(30)     not null,
  nick_name             varchar(30)     not null default '',
  password              varchar(100)    not null default '',
  email                 varchar(50)     default '',
  phonenumber           varchar(32)     default '',
  account_role          varchar(32)     not null default 'OWNER',
  status                char(1)         not null default '0',
  lock_status           char(1)         not null default '0',
  lock_reason           varchar(500)    not null default '',
  last_login_ip         varchar(128)    default '',
  last_login_time       datetime,
  pwd_update_time       datetime,
  owner_unique_buyer_id bigint(20) generated always as (case when account_role = 'OWNER' then buyer_id else null end) stored,
  create_by             varchar(64)     default '',
  create_time           datetime,
  update_by             varchar(64)     default '',
  update_time           datetime,
  remark                varchar(500)    default '',
  primary key (buyer_account_id),
  unique key uk_buyer_account_username (user_name),
  unique key uk_buyer_account_owner (owner_unique_buyer_id),
  key idx_buyer_account_buyer_status (buyer_id, status),
  key idx_buyer_account_buyer_lock (buyer_id, lock_status)
) engine=innodb auto_increment=1 comment = '买家端账号表';

call add_column_if_missing('seller_account', 'dept_id', 'bigint(20) default null');
call add_column_if_missing('seller_account', 'user_name', 'varchar(30) null');
call add_column_if_missing('seller_account', 'nick_name', 'varchar(30) null');
call add_column_if_missing('seller_account', 'password', 'varchar(100) null');
call add_column_if_missing('seller_account', 'email', 'varchar(50) null');
call add_column_if_missing('seller_account', 'phonenumber', 'varchar(32) null');
call add_column_if_missing('seller_account', 'lock_status', 'char(1) not null default ''0''');
call add_column_if_missing('seller_account', 'lock_reason', 'varchar(500) not null default ''''');
call add_column_if_missing('seller_account', 'last_login_ip', 'varchar(128) null');
call add_column_if_missing('seller_account', 'last_login_time', 'datetime null');
call add_column_if_missing('seller_account', 'pwd_update_time', 'datetime null');
call add_column_if_missing('seller_account', 'owner_unique_seller_id', 'bigint(20) generated always as (case when account_role = ''OWNER'' then seller_id else null end) stored');

call add_column_if_missing('buyer_account', 'dept_id', 'bigint(20) default null');
call add_column_if_missing('buyer_account', 'user_name', 'varchar(30) null');
call add_column_if_missing('buyer_account', 'nick_name', 'varchar(30) null');
call add_column_if_missing('buyer_account', 'password', 'varchar(100) null');
call add_column_if_missing('buyer_account', 'email', 'varchar(50) null');
call add_column_if_missing('buyer_account', 'phonenumber', 'varchar(32) null');
call add_column_if_missing('buyer_account', 'lock_status', 'char(1) not null default ''0''');
call add_column_if_missing('buyer_account', 'lock_reason', 'varchar(500) not null default ''''');
call add_column_if_missing('buyer_account', 'last_login_ip', 'varchar(128) null');
call add_column_if_missing('buyer_account', 'last_login_time', 'datetime null');
call add_column_if_missing('buyer_account', 'pwd_update_time', 'datetime null');
call add_column_if_missing('buyer_account', 'owner_unique_buyer_id', 'bigint(20) generated always as (case when account_role = ''OWNER'' then buyer_id else null end) stored');

update seller_account
set user_name = coalesce(nullif(user_name, ''), concat('seller_', seller_account_id)),
    nick_name = coalesce(nullif(nick_name, ''), user_name),
    password = coalesce(password, ''),
    email = coalesce(email, ''),
    phonenumber = coalesce(phonenumber, ''),
    status = coalesce(nullif(status, ''), '0'),
    lock_status = case when lock_status in ('0', '1') then lock_status else '0' end,
    lock_reason = coalesce(lock_reason, ''),
    pwd_update_time = coalesce(pwd_update_time, sysdate());

update buyer_account
set user_name = coalesce(nullif(user_name, ''), concat('buyer_', buyer_account_id)),
    nick_name = coalesce(nullif(nick_name, ''), user_name),
    password = coalesce(password, ''),
    email = coalesce(email, ''),
    phonenumber = coalesce(phonenumber, ''),
    status = coalesce(nullif(status, ''), '0'),
    lock_status = case when lock_status in ('0', '1') then lock_status else '0' end,
    lock_reason = coalesce(lock_reason, ''),
    pwd_update_time = coalesce(pwd_update_time, sysdate());

call drop_index_if_exists('seller_account', 'uk_seller_account_user');
call drop_index_if_exists('seller_account', 'uk_seller_account_seller_user');
call drop_index_if_exists('buyer_account', 'uk_buyer_account_user');
call drop_index_if_exists('buyer_account', 'uk_buyer_account_buyer_user');
call drop_column_if_exists('seller_account', 'user_id');
call drop_column_if_exists('buyer_account', 'user_id');

alter table seller_account modify user_name varchar(30) not null, modify nick_name varchar(30) not null default '', modify password varchar(100) not null default '';
alter table buyer_account modify user_name varchar(30) not null, modify nick_name varchar(30) not null default '', modify password varchar(100) not null default '';
call add_index_if_missing('seller_account', 'uk_seller_account_username', 'unique key uk_seller_account_username (user_name)');
call add_index_if_missing('buyer_account', 'uk_buyer_account_username', 'unique key uk_buyer_account_username (user_name)');
call assert_no_duplicate_owner_account('seller_account', 'seller_id', 'seller_account has duplicate OWNER accounts');
call assert_no_duplicate_owner_account('buyer_account', 'buyer_id', 'buyer_account has duplicate OWNER accounts');
call add_index_if_missing('seller_account', 'uk_seller_account_owner', 'unique key uk_seller_account_owner (owner_unique_seller_id)');
call add_index_if_missing('buyer_account', 'uk_buyer_account_owner', 'unique key uk_buyer_account_owner (owner_unique_buyer_id)');
call add_index_if_missing('seller_account', 'idx_seller_account_seller_lock', 'key idx_seller_account_seller_lock (seller_id, lock_status)');
call add_index_if_missing('buyer_account', 'idx_buyer_account_buyer_lock', 'key idx_buyer_account_buyer_lock (buyer_id, lock_status)');

create table if not exists seller_dept (
  seller_dept_id bigint(20) not null auto_increment,
  seller_id bigint(20) not null,
  parent_id bigint(20) default 0,
  ancestors varchar(500) default '',
  dept_name varchar(100) not null,
  order_num int default 0,
  leader varchar(100) default '',
  phone varchar(32) default '',
  email varchar(128) default '',
  status char(1) not null default '0',
  del_flag char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  primary key (seller_dept_id),
  key idx_seller_dept_seller_parent (seller_id, parent_id),
  key idx_seller_dept_status (seller_id, status)
) engine=innodb auto_increment=1 comment = '卖家端部门表';

create table if not exists buyer_dept (
  buyer_dept_id bigint(20) not null auto_increment,
  buyer_id bigint(20) not null,
  parent_id bigint(20) default 0,
  ancestors varchar(500) default '',
  dept_name varchar(100) not null,
  order_num int default 0,
  leader varchar(100) default '',
  phone varchar(32) default '',
  email varchar(128) default '',
  status char(1) not null default '0',
  del_flag char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  primary key (buyer_dept_id),
  key idx_buyer_dept_buyer_parent (buyer_id, parent_id),
  key idx_buyer_dept_status (buyer_id, status)
) engine=innodb auto_increment=1 comment = '买家端部门表';

create table if not exists seller_role (
  seller_role_id bigint(20) not null auto_increment,
  seller_id bigint(20) not null,
  role_name varchar(64) not null,
  role_key varchar(64) not null,
  role_sort int not null default 0,
  status char(1) not null default '0',
  del_flag char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  remark varchar(500) default '',
  primary key (seller_role_id),
  unique key uk_seller_role_key (seller_id, role_key),
  key idx_seller_role_status (seller_id, status)
) engine=innodb auto_increment=1 comment = '卖家端角色表';

create table if not exists buyer_role (
  buyer_role_id bigint(20) not null auto_increment,
  buyer_id bigint(20) not null,
  role_name varchar(64) not null,
  role_key varchar(64) not null,
  role_sort int not null default 0,
  status char(1) not null default '0',
  del_flag char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  remark varchar(500) default '',
  primary key (buyer_role_id),
  unique key uk_buyer_role_key (buyer_id, role_key),
  key idx_buyer_role_status (buyer_id, status)
) engine=innodb auto_increment=1 comment = '买家端角色表';

create table if not exists seller_menu (
  seller_menu_id bigint(20) not null auto_increment,
  menu_name varchar(64) not null,
  parent_id bigint(20) default 0,
  order_num int default 0,
  path varchar(200) default '',
  component varchar(255) default null,
  query varchar(255) default '',
  route_name varchar(64) default '',
  is_frame int default 1,
  is_cache int default 0,
  menu_type char(1) not null default 'M',
  visible char(1) not null default '0',
  status char(1) not null default '0',
  perms varchar(100) default '',
  icon varchar(100) default '#',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  remark varchar(500) default '',
  primary key (seller_menu_id),
  key idx_seller_menu_parent (parent_id),
  key idx_seller_menu_status (status)
) engine=innodb auto_increment=1 comment = '卖家端菜单权限表';

create table if not exists buyer_menu (
  buyer_menu_id bigint(20) not null auto_increment,
  menu_name varchar(64) not null,
  parent_id bigint(20) default 0,
  order_num int default 0,
  path varchar(200) default '',
  component varchar(255) default null,
  query varchar(255) default '',
  route_name varchar(64) default '',
  is_frame int default 1,
  is_cache int default 0,
  menu_type char(1) not null default 'M',
  visible char(1) not null default '0',
  status char(1) not null default '0',
  perms varchar(100) default '',
  icon varchar(100) default '#',
  create_by varchar(64) default '',
  create_time datetime,
  update_by varchar(64) default '',
  update_time datetime,
  remark varchar(500) default '',
  primary key (buyer_menu_id),
  key idx_buyer_menu_parent (parent_id),
  key idx_buyer_menu_status (status)
) engine=innodb auto_increment=1 comment = '买家端菜单权限表';

create table if not exists seller_account_role (
  seller_account_id bigint(20) not null,
  seller_role_id bigint(20) not null,
  primary key (seller_account_id, seller_role_id)
) engine=innodb comment = '卖家端账号角色关联表';

create table if not exists buyer_account_role (
  buyer_account_id bigint(20) not null,
  buyer_role_id bigint(20) not null,
  primary key (buyer_account_id, buyer_role_id)
) engine=innodb comment = '买家端账号角色关联表';

create table if not exists seller_role_menu (
  seller_role_id bigint(20) not null,
  seller_menu_id bigint(20) not null,
  primary key (seller_role_id, seller_menu_id)
) engine=innodb comment = '卖家端角色菜单关联表';

create table if not exists buyer_role_menu (
  buyer_role_id bigint(20) not null,
  buyer_menu_id bigint(20) not null,
  primary key (buyer_role_id, buyer_menu_id)
) engine=innodb comment = '买家端角色菜单关联表';

create table if not exists seller_login_log (
  info_id bigint(20) not null auto_increment,
  seller_id bigint(20) default null,
  seller_account_id bigint(20) default null,
  user_name varchar(30) default '',
  ipaddr varchar(128) default '',
  login_location varchar(255) default '',
  browser varchar(50) default '',
  os varchar(50) default '',
  status char(1) default '0',
  msg varchar(255) default '',
  login_time datetime,
  primary key (info_id),
  key idx_seller_login_log_account_time (seller_account_id, login_time),
  key idx_seller_login_log_seller_time (seller_id, login_time)
) engine=innodb auto_increment=1 comment = '卖家端登录日志表';
call add_column_if_missing('seller_login_log', 'seller_id', 'bigint(20) default null');
call add_column_if_missing('seller_login_log', 'seller_account_id', 'bigint(20) default null');
call add_index_if_missing('seller_login_log', 'idx_seller_login_log_account_time', 'key idx_seller_login_log_account_time (seller_account_id, login_time)');
call add_index_if_missing('seller_login_log', 'idx_seller_login_log_seller_time', 'key idx_seller_login_log_seller_time (seller_id, login_time)');

create table if not exists buyer_login_log (
  info_id bigint(20) not null auto_increment,
  buyer_id bigint(20) default null,
  buyer_account_id bigint(20) default null,
  user_name varchar(30) default '',
  ipaddr varchar(128) default '',
  login_location varchar(255) default '',
  browser varchar(50) default '',
  os varchar(50) default '',
  status char(1) default '0',
  msg varchar(255) default '',
  login_time datetime,
  primary key (info_id),
  key idx_buyer_login_log_account_time (buyer_account_id, login_time),
  key idx_buyer_login_log_buyer_time (buyer_id, login_time)
) engine=innodb auto_increment=1 comment = '买家端登录日志表';
call add_column_if_missing('buyer_login_log', 'buyer_id', 'bigint(20) default null');
call add_column_if_missing('buyer_login_log', 'buyer_account_id', 'bigint(20) default null');
call add_index_if_missing('buyer_login_log', 'idx_buyer_login_log_account_time', 'key idx_buyer_login_log_account_time (buyer_account_id, login_time)');
call add_index_if_missing('buyer_login_log', 'idx_buyer_login_log_buyer_time', 'key idx_buyer_login_log_buyer_time (buyer_id, login_time)');

create table if not exists seller_oper_log (
  oper_id bigint(20) not null auto_increment,
  seller_id bigint(20) default null,
  seller_account_id bigint(20) default null,
  title varchar(50) default '',
  business_type int default 0,
  method varchar(200) default '',
  request_method varchar(10) default '',
  oper_name varchar(30) default '',
  oper_url varchar(255) default '',
  oper_ip varchar(128) default '',
  oper_location varchar(255) default '',
  oper_param varchar(2000) default '',
  json_result varchar(2000) default '',
  status int default 0,
  error_msg varchar(2000) default '',
  oper_time datetime,
  cost_time bigint(20) default 0,
  primary key (oper_id),
  key idx_seller_oper_log_account_time (seller_account_id, oper_time),
  key idx_seller_oper_log_seller_time (seller_id, oper_time)
) engine=innodb auto_increment=1 comment = '卖家端操作日志表';
call add_column_if_missing('seller_oper_log', 'seller_id', 'bigint(20) default null');
call add_column_if_missing('seller_oper_log', 'seller_account_id', 'bigint(20) default null');
call add_index_if_missing('seller_oper_log', 'idx_seller_oper_log_account_time', 'key idx_seller_oper_log_account_time (seller_account_id, oper_time)');
call add_index_if_missing('seller_oper_log', 'idx_seller_oper_log_seller_time', 'key idx_seller_oper_log_seller_time (seller_id, oper_time)');

create table if not exists buyer_oper_log (
  oper_id bigint(20) not null auto_increment,
  buyer_id bigint(20) default null,
  buyer_account_id bigint(20) default null,
  title varchar(50) default '',
  business_type int default 0,
  method varchar(200) default '',
  request_method varchar(10) default '',
  oper_name varchar(30) default '',
  oper_url varchar(255) default '',
  oper_ip varchar(128) default '',
  oper_location varchar(255) default '',
  oper_param varchar(2000) default '',
  json_result varchar(2000) default '',
  status int default 0,
  error_msg varchar(2000) default '',
  oper_time datetime,
  cost_time bigint(20) default 0,
  primary key (oper_id),
  key idx_buyer_oper_log_account_time (buyer_account_id, oper_time),
  key idx_buyer_oper_log_buyer_time (buyer_id, oper_time)
) engine=innodb auto_increment=1 comment = '买家端操作日志表';
call add_column_if_missing('buyer_oper_log', 'buyer_id', 'bigint(20) default null');
call add_column_if_missing('buyer_oper_log', 'buyer_account_id', 'bigint(20) default null');
call add_index_if_missing('buyer_oper_log', 'idx_buyer_oper_log_account_time', 'key idx_buyer_oper_log_account_time (buyer_account_id, oper_time)');
call add_index_if_missing('buyer_oper_log', 'idx_buyer_oper_log_buyer_time', 'key idx_buyer_oper_log_buyer_time (buyer_id, oper_time)');

create table if not exists seller_session (
  token_id varchar(64) not null,
  seller_id bigint(20) not null,
  seller_account_id bigint(20) not null,
  user_name varchar(30) not null,
  login_ip varchar(128) default '',
  login_time datetime,
  expire_time datetime,
  logout_time datetime,
  status char(1) not null default '0',
  primary key (token_id),
  key idx_seller_session_account (seller_account_id),
  key idx_seller_session_expire (expire_time)
) engine=innodb comment = '卖家端会话表';

create table if not exists buyer_session (
  token_id varchar(64) not null,
  buyer_id bigint(20) not null,
  buyer_account_id bigint(20) not null,
  user_name varchar(30) not null,
  login_ip varchar(128) default '',
  login_time datetime,
  expire_time datetime,
  logout_time datetime,
  status char(1) not null default '0',
  primary key (token_id),
  key idx_buyer_session_account (buyer_account_id),
  key idx_buyer_session_expire (expire_time)
) engine=innodb comment = '买家端会话表';

update sys_role
set status = '1',
    del_flag = '2',
    update_by = 'admin',
    update_time = sysdate(),
    remark = concat(coalesce(remark, ''), '；三端独立后端内角色已迁出，保留历史记录')
where role_key in ('seller', 'buyer');

drop procedure if exists add_column_if_missing;
drop procedure if exists drop_column_if_exists;
drop procedure if exists drop_index_if_exists;
drop procedure if exists add_index_if_missing;
drop procedure if exists assert_no_duplicate_owner_account;
