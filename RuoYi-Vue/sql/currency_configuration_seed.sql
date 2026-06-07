-- Currency configuration seed for the RuoYi validation project.
-- Scope: currency dictionary, finance currency tables, menu entries, and permissions.
-- Run after top_menu_seed.sql and business_menu_seed.sql so parent directories are present.

set names utf8mb4;

set @confirm_currency_configuration_seed := coalesce(@confirm_currency_configuration_seed, '');

delimiter //

drop procedure if exists assert_currency_configuration_seed_confirmed//
create procedure assert_currency_configuration_seed_confirmed()
begin
  if coalesce(@confirm_currency_configuration_seed, '')
      <> 'APPLY_CURRENCY_CONFIGURATION_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_currency_configuration_seed = APPLY_CURRENCY_CONFIGURATION_SEED before running this seed';
  end if;
end//

delimiter ;

call assert_currency_configuration_seed_confirmed();
drop procedure if exists assert_currency_configuration_seed_confirmed;

delimiter //

drop procedure if exists assert_currency_configuration_sys_menu_guard//
create procedure assert_currency_configuration_sys_menu_guard()
begin
  if exists (
    select 1
    from sys_menu m
    where exists (
        select 1
        from tmp_currency_configuration_sys_menu_guard seed
        where seed.menu_id = m.menu_id
    )
      and not exists (
        select 1
        from tmp_currency_configuration_sys_menu_guard seed
        where seed.menu_id = m.menu_id
          and m.parent_id = seed.parent_id
          and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')
          and coalesce(m.path, '') = coalesce(seed.path, '')
          and coalesce(m.component, '') = coalesce(seed.component, '')
          and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
          and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'currency configuration sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_currency_configuration_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'currency configuration sys_menu signature is already used by another menu';
  end if;
end//

delimiter ;

create table if not exists finance_currency (
  currency_id            bigint(20)      not null auto_increment    comment '币种配置ID',
  currency_code          varchar(16)     not null                   comment '币种代码',
  currency_name          varchar(100)    not null                   comment '币种名称',
  currency_symbol        varchar(16)     default ''                 comment '币种符号',
  base_currency_code     varchar(16)     not null default 'CNY'     comment '汇率基准币种',
  official_rate          decimal(24,10)  default null               comment '官方汇率',
  effective_rate         decimal(24,10)  default null               comment '生效汇率',
  rate_precision         int             not null default 8         comment '汇率小数精度',
  amount_precision       int             not null default 2         comment '金额小数精度',
  rounding_mode          varchar(32)     not null default 'HALF_UP' comment '舍入方式',
  adjustment_mode        varchar(32)     not null default 'NONE'    comment '汇率调整方式',
  adjustment_value       decimal(24,10)  default null               comment '汇率调整值',
  official_rate_time     datetime                                   comment '官方汇率时间',
  effective_rate_time    datetime                                   comment '生效汇率更新时间',
  is_default             char(1)         not null default 'N'       comment '是否默认币种：Y是 N否',
  status                 char(1)         not null default '0'       comment '状态：0正常 1停用',
  create_by              varchar(64)     default ''                 comment '创建者',
  create_time            datetime                                   comment '创建时间',
  update_by              varchar(64)     default ''                 comment '更新者',
  update_time            datetime                                   comment '更新时间',
  remark                 varchar(500)    default ''                 comment '备注',
  primary key (currency_id),
  unique key uk_finance_currency_code (currency_code),
  key idx_finance_currency_status (status),
  key idx_finance_currency_base (base_currency_code)
) engine=innodb auto_increment=1 comment = '财务币种配置表';

create table if not exists finance_currency_rate_history (
  rate_history_id        bigint(20)      not null auto_increment    comment '汇率历史ID',
  currency_code          varchar(16)     not null                   comment '币种代码',
  base_currency_code     varchar(16)     not null                   comment '汇率基准币种',
  official_rate          decimal(24,10)  default null               comment '官方汇率',
  effective_rate         decimal(24,10)  default null               comment '生效汇率',
  adjustment_mode        varchar(32)     not null default 'NONE'    comment '汇率调整方式',
  adjustment_value       decimal(24,10)  default null               comment '汇率调整值',
  source_type            varchar(32)     not null                   comment '来源类型：SYNC/MANUAL',
  source_config_id       bigint(20)      default null               comment '同步配置ID',
  official_rate_time     datetime                                   comment '官方汇率时间',
  effective_rate_time    datetime       not null                    comment '生效时间',
  change_reason          varchar(500)    default ''                 comment '调整原因',
  create_by              varchar(64)     default ''                 comment '创建者',
  create_time            datetime       not null                    comment '创建时间',
  primary key (rate_history_id),
  key idx_currency_rate_history_currency_time (currency_code, create_time),
  key idx_currency_rate_history_source (source_type, source_config_id)
) engine=innodb auto_increment=1 comment = '财务币种汇率历史表';

create table if not exists finance_currency_sync_config (
  sync_config_id         bigint(20)      not null auto_increment    comment '同步配置ID',
  provider_code          varchar(64)     not null                   comment '服务商代码',
  provider_name          varchar(100)    not null                   comment '服务商名称',
  base_currency_code     varchar(16)     not null default 'CNY'     comment '汇率基准币种',
  api_base_url           varchar(500)    not null                   comment 'API地址',
  auth_type              varchar(32)     not null default 'NONE'    comment '认证方式',
  credential_ciphertext  varchar(2000)   default null               comment '加密凭证',
  credential_key_id      varchar(64)     default ''                 comment '加密密钥标识',
  credential_masked      varchar(200)    default ''                 comment '脱敏凭证',
  request_timeout_ms     int             not null default 10000     comment '请求超时毫秒',
  retry_count            int             not null default 0         comment '重试次数',
  schedule_type          varchar(32)     not null default 'MANUAL_ONLY' comment '同步计划类型',
  cron_expression        varchar(100)    default ''                 comment 'cron表达式',
  rate_anchor_time       time            not null default '09:30:00' comment '汇率基准时间',
  sync_enabled           char(1)         not null default '0'       comment '是否启用自动同步：1是 0否',
  last_sync_time         datetime                                   comment '最近同步时间',
  last_sync_status       varchar(32)     default ''                 comment '最近同步状态',
  status                 char(1)         not null default '0'       comment '配置状态：0正常 1停用',
  create_by              varchar(64)     default ''                 comment '创建者',
  create_time            datetime                                   comment '创建时间',
  update_by              varchar(64)     default ''                 comment '更新者',
  update_time            datetime                                   comment '更新时间',
  remark                 varchar(500)    default ''                 comment '备注',
  primary key (sync_config_id),
  unique key uk_currency_sync_provider (provider_code),
  key idx_currency_sync_status (sync_enabled, status)
) engine=innodb auto_increment=1 comment = '财务币种汇率同步配置表';

create table if not exists finance_currency_sync_log (
  sync_log_id            bigint(20)      not null auto_increment    comment '同步日志ID',
  trace_id               varchar(64)     not null                   comment '请求链路ID',
  sync_config_id         bigint(20)      not null                   comment '同步配置ID',
  provider_code          varchar(64)     not null                   comment '服务商代码',
  request_url            varchar(500)    default ''                 comment '脱敏请求地址',
  request_time           datetime       not null                    comment '请求时间',
  response_time          datetime                                   comment '响应时间',
  cost_ms                bigint(20)      default null               comment '耗时毫秒',
  status                 varchar(32)     not null                   comment '同步状态',
  error_code             varchar(100)    default ''                 comment '错误码',
  error_message          varchar(1000)   default ''                 comment '错误信息',
  currency_count         int             default 0                  comment '返回币种数',
  updated_count          int             default 0                  comment '更新币种数',
  response_summary       varchar(2000)   default ''                 comment '脱敏响应摘要',
  create_time            datetime       not null                    comment '创建时间',
  primary key (sync_log_id),
  key idx_currency_sync_log_config_time (sync_config_id, request_time),
  key idx_currency_sync_log_trace (trace_id),
  key idx_currency_sync_log_status (status, request_time)
) engine=innodb auto_increment=1 comment = '财务币种汇率同步日志表';

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '币种', 'currency_code', '0', 'admin', sysdate(), '', null, '平台币种全集，业务可用币种以 finance_currency 为准'
where not exists (select 1 from sys_dict_type where dict_type = 'currency_code');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'currency_code', '', '', seed.is_default, '0', 'admin', sysdate(), '', null, '币种'
from (
    select 1 as dict_sort, '美元 / US Dollar (USD)' as dict_label, 'USD' as dict_value, 'N' as is_default
    union all select 2, '人民币 / Chinese Yuan (CNY)', 'CNY', 'Y'
    union all select 3, '欧元 / Euro (EUR)', 'EUR', 'N'
    union all select 4, '英镑 / Pound Sterling (GBP)', 'GBP', 'N'
    union all select 5, '加拿大元 / Canadian Dollar (CAD)', 'CAD', 'N'
    union all select 6, '澳大利亚元 / Australian Dollar (AUD)', 'AUD', 'N'
    union all select 7, '日元 / Japanese Yen (JPY)', 'JPY', 'N'
    union all select 8, '港币 / Hong Kong Dollar (HKD)', 'HKD', 'N'
    union all select 9, '墨西哥比索 / Mexican Peso (MXN)', 'MXN', 'N'
    union all select 10, '巴西雷亚尔 / Brazilian Real (BRL)', 'BRL', 'N'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'currency_code' and d.dict_value = seed.dict_value
);

insert into finance_currency
    (currency_code, currency_name, currency_symbol, base_currency_code, official_rate, effective_rate,
     rate_precision, amount_precision, rounding_mode, adjustment_mode, official_rate_time, effective_rate_time,
     is_default, status, create_by, create_time, remark)
select seed.currency_code, seed.currency_name, seed.currency_symbol, 'CNY', seed.official_rate, seed.effective_rate,
       8, 2, 'HALF_UP', 'NONE', sysdate(), sysdate(), seed.is_default, '0', 'admin', sysdate(), '初始化可用币种'
from (
    select 'USD' as currency_code, '美元' as currency_name, '$' as currency_symbol, null as official_rate, null as effective_rate, 'N' as is_default
    union all select 'CNY', '人民币', '¥', 1.0000000000, 1.0000000000, 'Y'
    union all select 'EUR', '欧元', '€', null, null, 'N'
) seed
where not exists (
    select 1 from finance_currency c where c.currency_code = seed.currency_code
);

create temporary table if not exists tmp_currency_configuration_sys_menu_guard (
  menu_id    bigint       not null,
  parent_id  bigint       not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  key idx_currency_configuration_sys_menu_guard_id (menu_id)
) engine=memory;

truncate table tmp_currency_configuration_sys_menu_guard;

insert into tmp_currency_configuration_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (2442, 2050, 'C', 'currency', 'Finance/Currency/index', 'FinanceCurrency', 'finance:currency:list'),
    (2442, 2050, 'C', 'currency', 'Common/PlannedPage/index', 'CurrencyConfig', 'basic:currency:list'),
    (2460, 2442, 'F', '#', '', '', 'finance:currency:query'),
    (2461, 2442, 'F', '#', '', '', 'finance:currency:add'),
    (2462, 2442, 'F', '#', '', '', 'finance:currency:edit'),
    (2463, 2442, 'F', '#', '', '', 'finance:currency:remove'),
    (2464, 2442, 'F', '#', '', '', 'finance:currency:syncConfig'),
    (2465, 2442, 'F', '#', '', '', 'finance:currency:sync'),
    (2466, 2442, 'F', '#', '', '', 'finance:currency:log');

call assert_currency_configuration_sys_menu_guard();

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2442, '币种配置', 2050, 30, 'currency', 'Finance/Currency/index', '', 'FinanceCurrency',
     1, 0, 'C', '0', '0', 'finance:currency:list', 'MoneyCollectOutlined', 'admin',
     sysdate(), '', null, '财务管理菜单：币种配置'),
    (2460, '币种查询', 2442, 5, '#', '', '', '',
     1, 0, 'F', '0', '0', 'finance:currency:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2461, '币种新增', 2442, 10, '#', '', '', '',
     1, 0, 'F', '0', '0', 'finance:currency:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2462, '币种修改', 2442, 15, '#', '', '', '',
     1, 0, 'F', '0', '0', 'finance:currency:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2463, '币种删除', 2442, 20, '#', '', '', '',
     1, 0, 'F', '0', '0', 'finance:currency:remove', '#', 'admin',
     sysdate(), '', null, ''),
    (2464, '同步配置', 2442, 25, '#', '', '', '',
     1, 0, 'F', '0', '0', 'finance:currency:syncConfig', '#', 'admin',
     sysdate(), '', null, ''),
    (2465, '汇率同步', 2442, 30, '#', '', '', '',
     1, 0, 'F', '0', '0', 'finance:currency:sync', '#', 'admin',
     sysdate(), '', null, ''),
    (2466, '同步日志', 2442, 35, '#', '', '', '',
     1, 0, 'F', '0', '0', 'finance:currency:log', '#', 'admin',
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

drop temporary table if exists tmp_currency_configuration_sys_menu_guard;
drop procedure if exists assert_currency_configuration_sys_menu_guard;
