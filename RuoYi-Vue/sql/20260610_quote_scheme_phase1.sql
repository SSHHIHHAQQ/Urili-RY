-- Quote scheme phase 1 schema and admin permissions.
-- Scope: quote scheme base tables, dictionaries, and admin menu/button permissions.
-- This script creates configuration shells only. It does not run freight/operation fee calculation.

set names utf8mb4;

set @confirm_quote_scheme_phase1 := coalesce(@confirm_quote_scheme_phase1, '');

delimiter //

drop procedure if exists assert_quote_scheme_phase1_confirmed//
create procedure assert_quote_scheme_phase1_confirmed()
begin
  if coalesce(@confirm_quote_scheme_phase1, '')
      <> 'APPLY_QUOTE_SCHEME_PHASE1' then
    signal sqlstate '45000' set message_text = 'set @confirm_quote_scheme_phase1 = APPLY_QUOTE_SCHEME_PHASE1 before running this migration';
  end if;
end//

drop procedure if exists assert_quote_scheme_menu_guard//
create procedure assert_quote_scheme_menu_guard()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2053
      and parent_id = 2030
      and path = 'billing-quote-scheme'
      and menu_type = 'C'
      and component in ('Common/PlannedPage/index', 'Billing/QuoteScheme/index', 'Finance/QuoteScheme/index')
      and route_name in ('BillingQuoteScheme', 'FinanceQuoteScheme')
      and coalesce(perms, '') in ('', 'billing:quoteScheme:list', 'finance:quoteScheme:list')
  ) then
    signal sqlstate '45000' set message_text = 'quote scheme page menu 2053 must exist with expected placeholder or final signature';
  end if;

  if exists (
    select 1
    from sys_menu m
    left join tmp_quote_scheme_button_guard seed
      on seed.menu_id = m.menu_id
     and seed.parent_id = m.parent_id
     and seed.menu_type = m.menu_type
     and coalesce(seed.perms, '') = coalesce(m.perms, '')
    where m.menu_id in (2540, 2541, 2542, 2543, 2544, 2545)
      and seed.menu_id is null
  ) then
    signal sqlstate '45000' set message_text = 'quote scheme button menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_quote_scheme_button_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'quote scheme button permission is already used by another menu';
  end if;
end//

drop procedure if exists assert_quote_scheme_phase1_completed//
create procedure assert_quote_scheme_phase1_completed()
begin
  declare v_dict_type_count int default 0;
  declare v_dict_data_count int default 0;
  declare v_menu_count int default 0;
  declare v_table_count int default 0;

  select count(1)
    into v_table_count
  from information_schema.tables
  where table_schema = database()
    and table_name in ('quote_scheme', 'quote_scheme_scope', 'quote_scheme_warehouse', 'quote_scheme_channel');

  if v_table_count <> 4 then
    signal sqlstate '45000' set message_text = 'quote scheme phase1 tables were not created';
  end if;

  select count(1)
    into v_dict_type_count
  from sys_dict_type
  where status = '0'
    and dict_type in (
      'quote_scheme_type',
      'quote_scheme_fee_source_mode',
      'quote_scheme_scope_type',
      'quote_scheme_warehouse_scope_mode',
      'quote_scheme_status',
      'quote_scheme_channel_status'
    );

  if v_dict_type_count <> 6 then
    signal sqlstate '45000' set message_text = 'quote scheme phase1 dict types were not completed';
  end if;

  select count(1)
    into v_dict_data_count
  from sys_dict_data
  where status = '0'
    and (
      (dict_type = 'quote_scheme_type' and dict_value in ('BILLING', 'COST'))
      or (dict_type = 'quote_scheme_fee_source_mode' and dict_value in ('EXTERNAL_ESTIMATE', 'INTERNAL_RATE'))
      or (dict_type = 'quote_scheme_scope_type' and dict_value in ('ALL_BUYERS', 'BUYER_LEVEL', 'BUYER'))
      or (dict_type = 'quote_scheme_warehouse_scope_mode' and dict_value in ('ALL_WAREHOUSES', 'INCLUDE'))
      or (dict_type = 'quote_scheme_status' and dict_value in ('ENABLED', 'DISABLED'))
      or (dict_type = 'quote_scheme_channel_status' and dict_value in ('ENABLED', 'DISABLED'))
    );

  if v_dict_data_count <> 13 then
    signal sqlstate '45000' set message_text = 'quote scheme phase1 dict data was not completed';
  end if;

  select count(1)
    into v_menu_count
  from sys_menu
  where (menu_id = 2053 and parent_id = 2030 and menu_type = 'C'
         and path = 'billing-quote-scheme'
         and component = 'Finance/QuoteScheme/index'
         and route_name = 'FinanceQuoteScheme'
         and perms = 'finance:quoteScheme:list')
     or (menu_id = 2540 and parent_id = 2053 and menu_type = 'F' and perms = 'finance:quoteScheme:query')
     or (menu_id = 2541 and parent_id = 2053 and menu_type = 'F' and perms = 'finance:quoteScheme:add')
     or (menu_id = 2542 and parent_id = 2053 and menu_type = 'F' and perms = 'finance:quoteScheme:edit')
     or (menu_id = 2543 and parent_id = 2053 and menu_type = 'F' and perms = 'finance:quoteScheme:status')
     or (menu_id = 2544 and parent_id = 2053 and menu_type = 'F' and perms = 'finance:quoteScheme:warehouse')
     or (menu_id = 2545 and parent_id = 2053 and menu_type = 'F' and perms = 'finance:quoteScheme:channel');

  if v_menu_count <> 7 then
    signal sqlstate '45000' set message_text = 'quote scheme phase1 menu final state mismatch';
  end if;
end//

delimiter ;

call assert_quote_scheme_phase1_confirmed();
drop procedure if exists assert_quote_scheme_phase1_confirmed;

create table if not exists quote_scheme (
  scheme_id             bigint(20)   not null auto_increment     comment '报价方案ID',
  scheme_code           varchar(64)  not null                    comment '报价方案编码',
  scheme_name           varchar(200) not null                    comment '报价方案名称',
  scheme_type           varchar(32)  not null default 'BILLING'  comment '方案类型：BILLING计费方案，COST成本方案',
  fee_source_mode       varchar(32)  not null default 'EXTERNAL_ESTIMATE' comment '费用来源模式',
  currency_code         varchar(16)  not null                    comment '币种',
  scope_type            varchar(32)  not null default 'ALL_BUYERS' comment '适用对象类型',
  warehouse_scope_mode  varchar(32)  not null default 'ALL_WAREHOUSES' comment '仓库范围模式',
  effective_time        datetime     not null                    comment '生效时间',
  expire_time           datetime                                 comment '失效时间',
  effective_priority    int          not null default 0          comment '生效优先级，越大越优先',
  status                varchar(16)  not null default 'ENABLED'  comment '状态',
  create_by             varchar(64)  default ''                  comment '创建者',
  create_time           datetime                                 comment '创建时间',
  update_by             varchar(64)  default ''                  comment '更新者',
  update_time           datetime                                 comment '更新时间',
  remark                varchar(500) default ''                  comment '备注',
  primary key (scheme_id),
  unique key uk_quote_scheme_code (scheme_code),
  key idx_quote_scheme_type_scope (scheme_type, scope_type, status),
  key idx_quote_scheme_warehouse_scope (warehouse_scope_mode, status),
  key idx_quote_scheme_effective (effective_time, expire_time, effective_priority, status),
  key idx_quote_scheme_fee_source (fee_source_mode, status),
  key idx_quote_scheme_currency (currency_code)
) engine=innodb comment='报价方案主表';

create table if not exists quote_scheme_scope (
  scope_id                    bigint(20)   not null auto_increment comment '适用对象明细ID',
  scheme_id                   bigint(20)   not null                comment '报价方案ID',
  scope_type                  varchar(32)  not null                comment '适用对象类型',
  scope_key                   varchar(128) not null                comment '适用对象唯一键',
  buyer_level_code            varchar(32)  default null            comment '买家等级code',
  buyer_level_name_snapshot   varchar(100) default null            comment '买家等级名称快照',
  buyer_id                    bigint(20)   default null            comment '买家ID',
  buyer_code_snapshot         varchar(64)  default null            comment '买家编码快照',
  buyer_name_snapshot         varchar(200) default null            comment '买家名称快照',
  buyer_short_name_snapshot   varchar(100) default null            comment '买家简称快照',
  create_by                   varchar(64)  default ''              comment '创建者',
  create_time                 datetime                             comment '创建时间',
  remark                      varchar(500) default ''              comment '备注',
  primary key (scope_id),
  unique key uk_quote_scheme_scope (scheme_id, scope_key),
  key idx_quote_scheme_scope_type (scope_type),
  key idx_quote_scheme_scope_level (buyer_level_code),
  key idx_quote_scheme_scope_buyer (buyer_id)
) engine=innodb comment='报价方案适用对象明细表';

create table if not exists quote_scheme_warehouse (
  scheme_warehouse_id     bigint(20)   not null auto_increment comment '报价方案仓库明细ID',
  scheme_id               bigint(20)   not null                comment '报价方案ID',
  warehouse_code          varchar(64)  not null                comment '仓库编码',
  warehouse_name_snapshot varchar(200) not null                comment '仓库名称快照',
  warehouse_kind_snapshot varchar(32)  default ''              comment '仓库类型快照',
  create_by               varchar(64)  default ''              comment '创建者',
  create_time             datetime                             comment '创建时间',
  remark                  varchar(500) default ''              comment '备注',
  primary key (scheme_warehouse_id),
  unique key uk_quote_scheme_warehouse (scheme_id, warehouse_code),
  key idx_quote_scheme_warehouse_code (warehouse_code),
  key idx_quote_scheme_warehouse_kind (warehouse_kind_snapshot)
) engine=innodb comment='报价方案适用仓库表';

create table if not exists quote_scheme_channel (
  scheme_channel_id             bigint(20)   not null auto_increment comment '报价方案渠道明细ID',
  scheme_id                     bigint(20)   not null                comment '报价方案ID',
  customer_channel_code         varchar(64)  not null                comment '客户渠道编码',
  customer_channel_name_snapshot varchar(200) not null               comment '客户渠道名称快照',
  operation_fee_code            varchar(64)  default null            comment '操作费设置占位编码',
  operation_fee_name_snapshot   varchar(200) default null            comment '操作费设置名称快照',
  freight_fee_code              varchar(64)  default null            comment '运费设置占位编码',
  freight_fee_name_snapshot     varchar(200) default null            comment '运费设置名称快照',
  status                        varchar(16)  not null default 'ENABLED' comment '状态',
  display_order                 int          not null default 0      comment '显示顺序',
  create_by                     varchar(64)  default ''              comment '创建者',
  create_time                   datetime                             comment '创建时间',
  update_by                     varchar(64)  default ''              comment '更新者',
  update_time                   datetime                             comment '更新时间',
  remark                        varchar(500) default ''              comment '备注',
  primary key (scheme_channel_id),
  unique key uk_quote_scheme_channel (scheme_id, customer_channel_code),
  key idx_quote_scheme_channel_status (scheme_id, status),
  key idx_quote_scheme_channel_customer (customer_channel_code, status),
  key idx_quote_scheme_channel_order (scheme_id, display_order)
) engine=innodb comment='报价方案客户渠道明细表';

insert ignore into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark) values
('报价方案类型', 'quote_scheme_type', '0', 'admin', sysdate(), '报价方案计费/成本类型'),
('报价方案费用来源模式', 'quote_scheme_fee_source_mode', '0', 'admin', sysdate(), '报价方案费用来源模式'),
('报价方案适用对象类型', 'quote_scheme_scope_type', '0', 'admin', sysdate(), '报价方案适用对象类型'),
('报价方案仓库范围模式', 'quote_scheme_warehouse_scope_mode', '0', 'admin', sysdate(), '报价方案仓库范围模式'),
('报价方案状态', 'quote_scheme_status', '0', 'admin', sysdate(), '报价方案启停状态'),
('报价方案渠道状态', 'quote_scheme_channel_status', '0', 'admin', sysdate(), '报价方案渠道启停状态');

create temporary table if not exists tmp_quote_scheme_dict_data_seed (
  dict_sort  int not null,
  dict_label varchar(100) not null,
  dict_value varchar(100) not null,
  dict_type  varchar(100) not null,
  list_class varchar(100) default '',
  is_default char(1) default 'N',
  status     char(1) default '0',
  remark     varchar(500) default '',
  primary key (dict_type, dict_value)
);

truncate table tmp_quote_scheme_dict_data_seed;
insert into tmp_quote_scheme_dict_data_seed(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, remark) values
(1, '计费方案', 'BILLING', 'quote_scheme_type', 'primary', 'Y', '0', ''),
(2, '成本方案', 'COST', 'quote_scheme_type', 'warning', 'N', '0', ''),
(1, '外部试算', 'EXTERNAL_ESTIMATE', 'quote_scheme_fee_source_mode', 'primary', 'Y', '0', ''),
(2, '系统费率', 'INTERNAL_RATE', 'quote_scheme_fee_source_mode', 'default', 'N', '0', '阶段一仅占位'),
(1, '全部买家', 'ALL_BUYERS', 'quote_scheme_scope_type', 'default', 'Y', '0', ''),
(2, '买家等级', 'BUYER_LEVEL', 'quote_scheme_scope_type', 'primary', 'N', '0', ''),
(3, '指定买家', 'BUYER', 'quote_scheme_scope_type', 'warning', 'N', '0', ''),
(1, '全部仓库', 'ALL_WAREHOUSES', 'quote_scheme_warehouse_scope_mode', 'default', 'Y', '0', ''),
(2, '指定仓库', 'INCLUDE', 'quote_scheme_warehouse_scope_mode', 'primary', 'N', '0', ''),
(1, '启用', 'ENABLED', 'quote_scheme_status', 'success', 'Y', '0', ''),
(2, '停用', 'DISABLED', 'quote_scheme_status', 'default', 'N', '0', ''),
(1, '启用', 'ENABLED', 'quote_scheme_channel_status', 'success', 'Y', '0', ''),
(2, '停用', 'DISABLED', 'quote_scheme_channel_status', 'default', 'N', '0', '');

update sys_dict_data d
join tmp_quote_scheme_dict_data_seed seed
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
from tmp_quote_scheme_dict_data_seed seed
where not exists (
  select 1
  from sys_dict_data d
  where d.dict_type = seed.dict_type
    and d.dict_value = seed.dict_value
);

create temporary table if not exists tmp_quote_scheme_button_guard (
  menu_id   bigint(20) not null primary key,
  parent_id bigint(20) not null,
  menu_type char(1) not null,
  perms     varchar(100) not null
);

truncate table tmp_quote_scheme_button_guard;
insert into tmp_quote_scheme_button_guard(menu_id, parent_id, menu_type, perms) values
(2540, 2053, 'F', 'finance:quoteScheme:query'),
(2541, 2053, 'F', 'finance:quoteScheme:add'),
(2542, 2053, 'F', 'finance:quoteScheme:edit'),
(2543, 2053, 'F', 'finance:quoteScheme:status'),
(2544, 2053, 'F', 'finance:quoteScheme:warehouse'),
(2545, 2053, 'F', 'finance:quoteScheme:channel');

call assert_quote_scheme_menu_guard();

update sys_menu
set component = 'Finance/QuoteScheme/index',
    route_name = 'FinanceQuoteScheme',
    perms = 'finance:quoteScheme:list',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '报价方案页面'
where menu_id = 2053;

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
(2540, '报价方案查询', 2053, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'finance:quoteScheme:query', '#', 'admin', sysdate(), '报价方案查询按钮'),
(2541, '报价方案新增', 2053, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'finance:quoteScheme:add', '#', 'admin', sysdate(), '报价方案新增按钮'),
(2542, '报价方案编辑', 2053, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'finance:quoteScheme:edit', '#', 'admin', sysdate(), '报价方案编辑按钮'),
(2543, '报价方案启停', 2053, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'finance:quoteScheme:status', '#', 'admin', sysdate(), '报价方案启停按钮'),
(2544, '报价方案仓库', 2053, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'finance:quoteScheme:warehouse', '#', 'admin', sysdate(), '报价方案仓库范围按钮'),
(2545, '报价方案渠道', 2053, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'finance:quoteScheme:channel', '#', 'admin', sysdate(), '报价方案客户渠道按钮')
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

call assert_quote_scheme_phase1_completed();

drop temporary table if exists tmp_quote_scheme_button_guard;
drop temporary table if exists tmp_quote_scheme_dict_data_seed;
drop procedure if exists assert_quote_scheme_menu_guard;
drop procedure if exists assert_quote_scheme_phase1_completed;
