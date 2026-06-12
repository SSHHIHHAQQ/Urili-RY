-- Upstream system management seed for the RuoYi validation project.
-- Scope: integration tables, dictionaries, menu entries, and permissions.
-- Sensitive credentials are not stored in this script. Save credentials through the backend API so they are encrypted.
-- Fresh bootstrap must also run the mandatory post-seed chain documented in docs/architecture/integration-bootstrap-required-sql.md.

set names utf8mb4;

set @confirm_upstream_system_management_seed := coalesce(@confirm_upstream_system_management_seed, '');

delimiter //

drop procedure if exists assert_upstream_system_management_seed_confirmed//
create procedure assert_upstream_system_management_seed_confirmed()
begin
  if coalesce(@confirm_upstream_system_management_seed, '')
      <> 'APPLY_UPSTREAM_SYSTEM_MANAGEMENT_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_upstream_system_management_seed = APPLY_UPSTREAM_SYSTEM_MANAGEMENT_SEED before running this seed';
  end if;
end//

drop procedure if exists assert_upstream_system_management_sys_menu_guard//
create procedure assert_upstream_system_management_sys_menu_guard()
begin
  declare v_parent_count int default 0;

  select count(1)
    into v_parent_count
  from sys_menu
  where menu_id = 2030
    and menu_name = '海外仓服务设置'
    and parent_id = 0
    and path = 'overseas-warehouse-service'
    and route_name = 'OverseasWarehouseServiceManagement'
    and menu_type = 'M';

  if v_parent_count <> 1 then
    signal sqlstate '45000' set message_text = 'upstream system management parent sys_menu 2030 is required before upstream system management seed';
  end if;

  if exists (
    select 1
    from sys_menu m
    where exists (
        select 1
        from tmp_upstream_system_management_sys_menu_guard seed
        where seed.menu_id = m.menu_id
    )
      and not exists (
        select 1
        from tmp_upstream_system_management_sys_menu_guard seed
        where seed.menu_id = m.menu_id
          and coalesce(m.parent_id, -1) = seed.parent_id
          and coalesce(m.menu_type, '') = seed.menu_type
          and coalesce(m.path, '') = coalesce(seed.path, '')
          and coalesce(m.component, '') = coalesce(seed.component, '')
          and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
          and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'upstream system management sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_upstream_system_management_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'upstream system management sys_menu signature is already used by another menu';
  end if;
end//

drop procedure if exists assert_upstream_system_management_seed_completed//
create procedure assert_upstream_system_management_seed_completed()
begin
  declare v_dict_type_count int default 0;
  declare v_dict_data_count int default 0;

  select count(1)
    into v_dict_type_count
  from sys_dict_type
  where status = '0'
    and dict_type in (
      'upstream_system_kind',
      'upstream_connection_status',
      'upstream_sync_item_status',
      'upstream_settlement_type',
      'upstream_pairing_role'
    );

  if v_dict_type_count <> 5 then
    signal sqlstate '45000' set message_text = 'upstream system management seed did not complete expected dict types';
  end if;

  select count(1)
    into v_dict_data_count
  from sys_dict_data
  where status = '0'
    and (
      (dict_type = 'upstream_system_kind' and dict_value = 'lingxing-wms')
      or (dict_type = 'upstream_connection_status' and dict_value in ('ENABLED', 'DISABLED'))
      or (dict_type = 'upstream_sync_item_status' and dict_value in ('ACTIVE', 'MISSING'))
      or (dict_type = 'upstream_settlement_type' and dict_value in ('upstream-payable', 'self-operated-receivable'))
      or (dict_type = 'upstream_pairing_role' and dict_value in ('FULFILLMENT', 'QUOTE'))
    );

  if v_dict_data_count <> 9 then
    signal sqlstate '45000' set message_text = 'upstream system management seed did not complete expected dict data';
  end if;

  if exists (
    select 1
    from tmp_upstream_system_management_sys_menu_guard seed
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
    signal sqlstate '45000' set message_text = 'upstream system management seed did not complete expected sys_menu state';
  end if;
end//

delimiter ;

call assert_upstream_system_management_seed_confirmed();
drop procedure if exists assert_upstream_system_management_seed_confirmed;

create table if not exists upstream_system_connection (
  connection_code        varchar(64)   not null                  comment '主仓接入编号',
  system_kind            varchar(32)   not null default 'lingxing-wms' comment '上游系统类型',
  master_warehouse_name  varchar(200)  not null                  comment '主仓显示名称',
  settlement_type        varchar(32)   not null                  comment '结算类型',
  app_key_mask           varchar(64)   not null default ''       comment '脱敏后的appKey',
  app_secret_mask        varchar(64)   not null default ''       comment '脱敏后的appSecret',
  app_key_ciphertext     text          not null                  comment '加密后的appKey',
  app_secret_ciphertext  text          not null                  comment '加密后的appSecret',
  credential_key_id      varchar(64)   not null default 'default' comment '加密密钥版本',
  status                 varchar(16)   not null default 'ENABLED' comment '接入状态',
  credential_status      varchar(16)   not null default 'PENDING' comment '凭证状态',
  enabled_capabilities   varchar(500)  not null default ''       comment '启用能力JSON',
  display_order          int           not null default 0        comment '显示排序',
  last_authorized_time   datetime                                comment '最近授权成功时间',
  last_sync_time         datetime                                comment '最近同步时间',
  request_log_count      int           not null default 0        comment '请求日志数量摘要',
  create_by              varchar(64)   default ''                comment '创建者',
  create_time            datetime                                comment '创建时间',
  update_by              varchar(64)   default ''                comment '更新者',
  update_time            datetime                                comment '更新时间',
  remark                 varchar(500)  default ''                comment '备注',
  primary key (connection_code),
  unique key uk_upstream_connection_kind_code (system_kind, connection_code),
  key idx_upstream_connection_order (display_order, last_authorized_time),
  key idx_upstream_connection_status (status)
) engine=innodb comment='上游系统主仓接入表';

create table if not exists upstream_system_warehouse_candidate (
  connection_code   varchar(64)   not null                 comment '主仓接入编号',
  warehouse_code    varchar(100)  not null                 comment '领星仓库代码',
  warehouse_name    varchar(200)  not null                 comment '领星仓库名称',
  country_code      varchar(32)   default ''               comment '国家/地区代码',
  status            varchar(16)   not null default 'ACTIVE' comment '同步清单状态',
  sync_batch_id     varchar(64)   not null                 comment '同步批次号',
  first_seen_time   datetime      not null                 comment '首次发现时间',
  last_seen_time    datetime      not null                 comment '最近发现时间',
  update_time       datetime      not null                 comment '更新时间',
  primary key (connection_code, warehouse_code),
  key idx_upstream_wh_candidate_status (connection_code, status)
) engine=innodb comment='领星仓库同步清单';

create table if not exists upstream_system_warehouse_pairing (
  warehouse_pairing_id    bigint(20)    not null auto_increment comment '仓库配对ID',
  connection_code         varchar(64)   not null                comment '主仓接入编号',
  upstream_warehouse_code varchar(100)  not null                comment '领星仓库代码',
  upstream_warehouse_name varchar(200)  not null                comment '领星仓库名称快照',
  system_warehouse_code   varchar(64)   not null                comment '系统仓库代码',
  system_warehouse_name   varchar(200)  not null                comment '系统仓库名称快照',
  pairing_role            varchar(32)   not null default 'FULFILLMENT' comment '配对用途：FULFILLMENT履约仓，QUOTE报价仓',
  status                  varchar(16)   not null default 'ACTIVE' comment '配对状态',
  create_by               varchar(64)   default ''              comment '创建者',
  create_time             datetime                              comment '创建时间',
  update_by               varchar(64)   default ''              comment '更新者',
  update_time             datetime                              comment '更新时间',
  remark                  varchar(500)  default ''              comment '备注',
  primary key (warehouse_pairing_id),
  unique key uk_upstream_wh_pairing_system_role (system_warehouse_code, pairing_role),
  unique key uk_upstream_wh_pairing_upstream_role (connection_code, upstream_warehouse_code, pairing_role),
  key idx_upstream_wh_pairing_connection (connection_code)
) engine=innodb comment='系统仓库与领星仓库配对表';

create table if not exists upstream_system_logistics_channel_candidate (
  connection_code   varchar(64)   not null                 comment '主仓接入编号',
  warehouse_code    varchar(100)  not null                 comment '领星仓库代码',
  channel_code      varchar(100)  not null                 comment '领星物流渠道代码',
  channel_name      varchar(200)  not null                 comment '领星物流渠道名称',
  status            varchar(16)   not null default 'ACTIVE' comment '同步清单状态',
  sync_batch_id     varchar(64)   not null                 comment '同步批次号',
  first_seen_time   datetime      not null                 comment '首次发现时间',
  last_seen_time    datetime      not null                 comment '最近发现时间',
  update_time       datetime      not null                 comment '更新时间',
  primary key (connection_code, warehouse_code, channel_code),
  key idx_upstream_channel_candidate_code (connection_code, channel_code),
  key idx_upstream_channel_candidate_status (connection_code, status)
) engine=innodb comment='领星物流渠道同步清单';

create table if not exists upstream_system_logistics_channel_pairing (
  logistics_channel_pairing_id bigint(20)   not null auto_increment comment '物流渠道配对ID',
  connection_code              varchar(64)  not null                comment '主仓接入编号',
  system_warehouse_code        varchar(64)  not null default ''     comment '历史兼容：系统仓库代码，系统渠道级配对不再使用',
  upstream_warehouse_code      varchar(100) not null default ''     comment '历史兼容：领星仓库代码，系统渠道级配对不再使用',
  upstream_channel_code        varchar(100) not null                comment '领星物流渠道代码',
  upstream_channel_name        varchar(200) not null                comment '领星物流渠道名称快照',
  system_channel_code          varchar(64)  not null                comment '系统物流渠道代码',
  system_channel_name          varchar(200) not null                comment '系统物流渠道名称快照',
  pairing_role                 varchar(32)  not null default 'FULFILLMENT' comment '配对用途：FULFILLMENT履约渠道，QUOTE报价渠道',
  status                       varchar(16)  not null default 'ACTIVE' comment '配对状态',
  create_by                    varchar(64)  default ''              comment '创建者',
  create_time                  datetime                             comment '创建时间',
  update_by                    varchar(64)  default ''              comment '更新者',
  update_time                  datetime                             comment '更新时间',
  remark                       varchar(500) default ''              comment '备注',
  primary key (logistics_channel_pairing_id),
  unique key uk_upstream_channel_pairing_system_role (system_channel_code, pairing_role),
  key idx_upstream_channel_pairing_upstream_role (connection_code, upstream_channel_code, pairing_role),
  key idx_upstream_channel_pairing_connection (connection_code)
) engine=innodb comment='系统物流渠道与领星渠道配对表';

create table if not exists upstream_system_sku_candidate (
  connection_code       varchar(64)   not null                 comment '主仓接入编号',
  master_sku            varchar(128)  not null                 comment '领星masterSku',
  master_product_name   varchar(255)  not null                 comment '领星产品名称',
  product_alias_name    varchar(255)  default ''               comment '领星产品别名',
  approve_status        varchar(32)   default ''               comment '领星产品审核状态',
  product_type          int                                    comment '领星产品类型：0自有产品，1分销产品',
  product_description   text                                   comment '领星产品描述',
  image_url             varchar(1000) default ''               comment '领星产品图片URL',
  main_code             varchar(128)  default ''               comment '产品条码(EAN/UPC)',
  other_code            varchar(1000) default ''               comment '其他条码',
  fnsku                 varchar(1000) default ''               comment 'FNSKU',
  country_of_origin_name varchar(100) default ''               comment '原产国家/地区代码或名称',
  currency_code         varchar(16)   default ''               comment '申报币种code',
  customhouse_code      varchar(64)   default ''               comment '海关编码',
  dangerous_cargo       int                                    comment '所属危险品code',
  declare_name_cn       varchar(255)  default ''               comment '申报中文名',
  declare_name_en       varchar(255)  default ''               comment '申报英文名',
  declare_price         decimal(18,4)                          comment '申报价格',
  product_height        decimal(18,4)                          comment '产品高(cm)',
  product_height_bs     decimal(18,4)                          comment '产品英制高(in)',
  product_length        decimal(18,4)                          comment '产品长(cm)',
  product_length_bs     decimal(18,4)                          comment '产品英制长(in)',
  product_weight        decimal(18,4)                          comment '产品重量(kg)',
  product_weight_bs     decimal(18,4)                          comment '产品英制重量(lb)',
  product_width         decimal(18,4)                          comment '产品宽(cm)',
  product_width_bs      decimal(18,4)                          comment '产品英制宽(in)',
  wms_height            decimal(18,4)                          comment 'WMS高(cm)',
  wms_height_bs         decimal(18,4)                          comment 'WMS英制高(in)',
  wms_length            decimal(18,4)                          comment 'WMS长(cm)',
  wms_length_bs         decimal(18,4)                          comment 'WMS英制长(in)',
  wms_weight            decimal(18,4)                          comment 'WMS重量(kg)',
  wms_weight_bs         decimal(18,4)                          comment 'WMS英制重量(lb)',
  wms_width             decimal(18,4)                          comment 'WMS宽(cm)',
  wms_width_bs          decimal(18,4)                          comment 'WMS英制宽(in)',
  cat1_name             varchar(100)  default ''               comment '领星一级分类名称',
  cat2_name             varchar(100)  default ''               comment '领星二级分类名称',
  cat3_name             varchar(100)  default ''               comment '领星三级分类名称',
  platform_sku_info_json longtext                              comment '平台SKU信息JSON',
  brazil_tax_info_json  longtext                               comment '巴西税务信息JSON',
  source_payload_json   longtext                               comment '领星产品原始行JSON快照',
  source_payload_hash   varchar(64)   default ''               comment '领星产品原始行JSON哈希',
  status                varchar(16)   not null default 'ACTIVE' comment '同步清单状态',
  search_text           text          not null                 comment '搜索文本',
  sync_batch_id         varchar(64)   not null                 comment '同步批次号',
  first_seen_time       datetime      not null                 comment '首次发现时间',
  last_seen_time        datetime      not null                 comment '最近发现时间',
  update_time           datetime      not null                 comment '更新时间',
  primary key (connection_code, master_sku),
  key idx_upstream_sku_candidate_status (connection_code, status),
  key idx_upstream_sku_candidate_search (connection_code, master_sku, master_product_name),
  key idx_upstream_sku_candidate_main_code (connection_code, main_code),
  key idx_upstream_sku_candidate_fnsku (connection_code, fnsku(191)),
  key idx_upstream_sku_candidate_approve (connection_code, approve_status),
  key idx_upstream_sku_candidate_category (connection_code, cat1_name, cat2_name, cat3_name)
) engine=innodb comment='领星SKU同步清单';

create table if not exists upstream_system_sku_pairing (
  sku_pairing_id   bigint(20)    not null auto_increment comment 'SKU配对ID',
  connection_code  varchar(64)   not null                comment '主仓接入编号',
  master_sku       varchar(128)  not null                comment '领星masterSku',
  system_sku       varchar(128)  not null                comment '系统SKU',
  system_sku_name  varchar(255)  not null                comment '系统SKU名称快照',
  customer_name    varchar(200)  default ''              comment '客户名称快照',
  create_by        varchar(64)   default ''              comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)   default ''              comment '更新者',
  update_time      datetime                              comment '更新时间',
  remark           varchar(500)  default ''              comment '备注',
  primary key (sku_pairing_id),
  unique key uk_upstream_sku_pairing_master (connection_code, master_sku),
  unique key uk_upstream_sku_pairing_system (connection_code, system_sku),
  key idx_upstream_sku_pairing_connection (connection_code)
) engine=innodb comment='系统SKU与领星masterSku配对表';

create table if not exists upstream_system_sku_sync_state (
  connection_code       varchar(64)  not null                 comment '主仓接入编号',
  status                varchar(16)  not null default 'NEVER' comment '同步状态',
  sync_batch_id         varchar(64)  default null             comment '同步批次号',
  last_started_time     datetime                              comment '最近开始同步时间',
  last_finished_time    datetime                              comment '最近结束同步时间',
  last_success_time     datetime                              comment '最近同步成功时间',
  last_error_message    varchar(500) default ''               comment '最近失败原因',
  next_sync_time        datetime                              comment '下次计划同步时间',
  update_time           datetime                              comment '更新时间',
  primary key (connection_code)
) engine=innodb comment='SKU同步状态表';


create table if not exists upstream_system_request_log (
  request_log_id             bigint(20)   not null auto_increment comment '请求日志ID',
  connection_code            varchar(64)  not null                comment '主仓接入编号',
  trace_id                   varchar(64)  not null                comment '请求追踪号',
  operation                  varchar(64)  not null                comment '业务操作名称',
  endpoint                   varchar(255) not null                comment '领星接口地址',
  request_time               datetime                             comment '请求发起时间',
  response_time              datetime                             comment '响应返回时间',
  duration_ms                bigint                                comment '请求耗时毫秒',
  request_payload_redacted   longtext                              comment '脱敏请求内容',
  response_payload_redacted  longtext                              comment '脱敏响应内容',
  external_error_code        varchar(64)  default ''              comment '外部错误码',
  external_error_message     varchar(500) default ''              comment '外部错误信息',
  status                     varchar(16)  not null                comment '请求结果状态',
  create_time                datetime                             comment '创建时间',
  primary key (request_log_id),
  key idx_upstream_request_log_connection (connection_code),
  key idx_upstream_request_log_trace (trace_id),
  key idx_upstream_request_log_created (create_time)
) engine=innodb comment='上游系统请求日志表';

create table if not exists upstream_system_sku_pairing_audit_event (
  audit_event_id  bigint(20)   not null auto_increment comment '审计事件ID',
  connection_code varchar(64)  not null                comment '主仓接入编号',
  master_sku      varchar(128) not null                comment '领星masterSku',
  system_sku      varchar(128) not null                comment '系统SKU',
  event_type      varchar(32)  not null                comment '事件类型',
  operator        varchar(64)  default ''              comment '操作人',
  event_time      datetime                             comment '操作时间',
  before_snapshot longtext                             comment '变更前快照',
  after_snapshot  longtext                             comment '变更后快照',
  remark          varchar(500) default ''              comment '备注',
  primary key (audit_event_id),
  key idx_upstream_sku_audit_connection (connection_code, event_time),
  key idx_upstream_sku_audit_sku (connection_code, master_sku, system_sku)
) engine=innodb comment='SKU配对审计事件表';

create temporary table if not exists tmp_upstream_system_management_sys_menu_guard (
  menu_id    bigint       not null,
  parent_id  bigint       not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  key idx_upstream_system_management_sys_menu_guard_id (menu_id)
) engine=memory;

truncate table tmp_upstream_system_management_sys_menu_guard;

insert into tmp_upstream_system_management_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (2031, 2030, 'C', 'upstream-system', 'UpstreamSystem/index', 'UpstreamSystem', 'integration:upstream:list'),
    (2300, 2031, 'F', '#', '', '', 'integration:upstream:query'),
    (2301, 2031, 'F', '#', '', '', 'integration:upstream:add'),
    (2302, 2031, 'F', '#', '', '', 'integration:upstream:edit'),
    (2303, 2031, 'F', '#', '', '', 'integration:upstream:credential'),
    (2304, 2031, 'F', '#', '', '', 'integration:upstream:sync'),
    (2305, 2031, 'F', '#', '', '', 'integration:upstream:pair'),
    (2306, 2031, 'F', '#', '', '', 'integration:upstream:log'),
    (2307, 2031, 'F', '#', '', '', 'integration:upstream:dimensionSync'),
    (2308, 2031, 'F', '#', '', '', 'integration:upstream:inventoryQuery'),
    (2309, 2031, 'F', '#', '', '', 'integration:upstream:inventorySync'),
    (2324, 2031, 'F', '#', '', '', 'integration:upstream:task:list'),
    (2325, 2031, 'F', '#', '', '', 'integration:upstream:task:retry'),
    (2326, 2031, 'F', '#', '', '', 'integration:upstream:task:cancel');

call assert_upstream_system_management_sys_menu_guard();

start transaction;

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '上游系统类型', 'upstream_system_kind', '0', 'admin', sysdate(), '', null, '上游系统类型'
where not exists (select 1 from sys_dict_type where dict_type = 'upstream_system_kind');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select 1, '领星WMS', 'lingxing-wms', 'upstream_system_kind', '', '', 'Y', '0', 'admin', sysdate(), '', null, '上游系统类型'
where not exists (select 1 from sys_dict_data where dict_type = 'upstream_system_kind' and dict_value = 'lingxing-wms');

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '上游接入状态', 'upstream_connection_status', '0', 'admin', sysdate(), '', null, '上游接入状态'
where not exists (select 1 from sys_dict_type where dict_type = 'upstream_connection_status');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'upstream_connection_status', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '上游接入状态'
from (
    select 1 as dict_sort, '启用' as dict_label, 'ENABLED' as dict_value, 'primary' as list_class, 'Y' as is_default
    union all select 2, '停用', 'DISABLED', 'default', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'upstream_connection_status' and d.dict_value = seed.dict_value);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '同步清单状态', 'upstream_sync_item_status', '0', 'admin', sysdate(), '', null, '同步清单状态'
where not exists (select 1 from sys_dict_type where dict_type = 'upstream_sync_item_status');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'upstream_sync_item_status', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '同步清单状态'
from (
    select 1 as dict_sort, '正常' as dict_label, 'ACTIVE' as dict_value, 'success' as list_class, 'Y' as is_default
    union all select 2, '上游缺失', 'MISSING', 'warning', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'upstream_sync_item_status' and d.dict_value = seed.dict_value);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '上游结算类型', 'upstream_settlement_type', '0', 'admin', sysdate(), '', null, '上游结算类型'
where not exists (select 1 from sys_dict_type where dict_type = 'upstream_settlement_type');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'upstream_settlement_type', '', '', seed.is_default, '0', 'admin', sysdate(), '', null, '上游结算类型'
from (
    select 1 as dict_sort, '上游仓（应付）' as dict_label, 'upstream-payable' as dict_value, 'Y' as is_default
    union all select 2, '自营仓（应收）', 'self-operated-receivable', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'upstream_settlement_type' and d.dict_value = seed.dict_value);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '上游配对用途', 'upstream_pairing_role', '0', 'admin', sysdate(), '', null, '上游配对用途'
where not exists (select 1 from sys_dict_type where dict_type = 'upstream_pairing_role');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'upstream_pairing_role', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '上游配对用途'
from (
    select 1 as dict_sort, '履约' as dict_label, 'FULFILLMENT' as dict_value, 'primary' as list_class, 'Y' as is_default
    union all select 2, '报价', 'QUOTE', 'warning', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'upstream_pairing_role' and d.dict_value = seed.dict_value);

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2031, '上游系统管理', 2030, 5, 'upstream-system', 'UpstreamSystem/index', '', 'UpstreamSystem',
     1, 0, 'C', '0', '0', 'integration:upstream:list', 'ApiOutlined', 'admin',
     sysdate(), '', null, '领星主仓接入、同步清单和配对管理'),
    (2300, '上游系统查询', 2031, 5, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:query', '#', 'admin',
     sysdate(), '', null, ''),
    (2301, '上游系统新增', 2031, 10, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:add', '#', 'admin',
     sysdate(), '', null, ''),
    (2302, '上游系统修改', 2031, 15, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:edit', '#', 'admin',
     sysdate(), '', null, ''),
    (2303, '上游系统授权', 2031, 20, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:credential', '#', 'admin',
     sysdate(), '', null, ''),
    (2304, '上游系统同步', 2031, 25, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:sync', '#', 'admin',
     sysdate(), '', null, ''),
    (2305, '上游系统配对', 2031, 30, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:pair', '#', 'admin',
     sysdate(), '', null, ''),
    (2306, '请求日志查看', 2031, 35, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:log', '#', 'admin',
     sysdate(), '', null, ''),
    (2307, '仓库尺寸重量同步', 2031, 40, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:dimensionSync', '#', 'admin',
     sysdate(), '', null, ''),
    (2308, 'SKU库存查看', 2031, 45, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:inventoryQuery', '#', 'admin',
     sysdate(), '', null, ''),
    (2309, 'SKU库存同步', 2031, 50, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:inventorySync', '#', 'admin',
     sysdate(), '', null, ''),
    (2324, '同步任务查看', 2031, 55, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:task:list', '#', 'admin',
     sysdate(), '', null, '查看上游同步任务请求和执行项'),
    (2325, '同步任务重试', 2031, 60, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:task:retry', '#', 'admin',
     sysdate(), '', null, '重试失败或超时的上游同步任务'),
    (2326, '同步任务取消', 2031, 65, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:task:cancel', '#', 'admin',
     sysdate(), '', null, '取消尚未执行的上游同步任务')
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

call assert_upstream_system_management_seed_completed();

commit;

drop temporary table if exists tmp_upstream_system_management_sys_menu_guard;
drop procedure if exists assert_upstream_system_management_seed_completed;
drop procedure if exists assert_upstream_system_management_sys_menu_guard;

-- Inventory schema and scheduled jobs are maintained by follow-up migrations.
