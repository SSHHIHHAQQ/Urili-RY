-- Logistics carrier management schema and admin permissions.
-- Scope: generic logistics carrier tables, AGG56 private extension table, dictionaries, and admin button permissions.
-- Sensitive credentials are not stored in this script. Save credentials through the backend API so they are encrypted.

set names utf8mb4;

set @confirm_logistics_carrier_management := coalesce(@confirm_logistics_carrier_management, '');

delimiter //

drop procedure if exists assert_logistics_carrier_management_confirmed//
create procedure assert_logistics_carrier_management_confirmed()
begin
  if coalesce(@confirm_logistics_carrier_management, '')
      <> 'APPLY_LOGISTICS_CARRIER_MANAGEMENT' then
    signal sqlstate '45000' set message_text = 'set @confirm_logistics_carrier_management = APPLY_LOGISTICS_CARRIER_MANAGEMENT before running this migration';
  end if;
end//

drop procedure if exists assert_logistics_carrier_menu_guard//
create procedure assert_logistics_carrier_menu_guard()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2054
      and parent_id = 2030
      and menu_name = '物流商管理'
      and path = 'logistics-carrier'
      and route_name = 'LogisticsCarrier'
      and perms = 'logistics:carrier:list'
      and menu_type = 'C'
      and component in ('Common/PlannedPage/index', 'Logistics/Carrier/index')
  ) then
    signal sqlstate '45000' set message_text = 'logistics carrier page menu 2054 must exist with expected placeholder or final signature';
  end if;

  if exists (
    select 1
    from sys_menu m
    left join tmp_logistics_carrier_button_guard seed
      on seed.menu_id = m.menu_id
     and seed.parent_id = m.parent_id
     and seed.menu_type = m.menu_type
     and coalesce(seed.perms, '') = coalesce(m.perms, '')
    where m.menu_id in (2510, 2511, 2512, 2513, 2514, 2515, 2516, 2517)
      and seed.menu_id is null
  ) then
    signal sqlstate '45000' set message_text = 'logistics carrier button menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_logistics_carrier_button_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'logistics carrier button permission is already used by another menu';
  end if;
end//

drop procedure if exists assert_logistics_carrier_management_completed//
create procedure assert_logistics_carrier_management_completed()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2054
      and parent_id = 2030
      and path = 'logistics-carrier'
      and component = 'Logistics/Carrier/index'
      and route_name = 'LogisticsCarrier'
      and perms = 'logistics:carrier:list'
      and menu_type = 'C'
  ) then
    signal sqlstate '45000' set message_text = 'logistics carrier page menu final component mismatch';
  end if;

  if exists (
    select 1
    from tmp_logistics_carrier_button_guard seed
    where not exists (
      select 1
      from sys_menu m
      where m.menu_id = seed.menu_id
        and m.parent_id = seed.parent_id
        and m.menu_type = seed.menu_type
        and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'logistics carrier button permission final state mismatch';
  end if;
end//

delimiter ;

call assert_logistics_carrier_management_confirmed();
drop procedure if exists assert_logistics_carrier_management_confirmed;

create table if not exists logistics_carrier_connection (
  carrier_account_id       bigint(20)   not null auto_increment  comment '物流商账号ID',
  connection_code          varchar(64)  not null                  comment '物流商接入编号',
  provider_kind            varchar(32)  not null                  comment '接入方类型，例如AGG56',
  connection_name          varchar(200) not null                  comment '物流商账号名称',
  api_base_url             varchar(500) not null                  comment 'API Base URL',
  status                   varchar(16)  not null default 'ENABLED' comment '接入状态',
  credential_status        varchar(16)  not null default 'UNCONFIGURED' comment '凭据状态',
  last_authorized_time     datetime                               comment '最近授权成功时间',
  last_channel_sync_time   datetime                               comment '最近物流商渠道同步时间',
  display_order            int          not null default 0        comment '显示排序',
  create_by                varchar(64)  default ''                comment '创建者',
  create_time              datetime                               comment '创建时间',
  update_by                varchar(64)  default ''                comment '更新者',
  update_time              datetime                               comment '更新时间',
  remark                   varchar(500) default ''                comment '备注',
  primary key (carrier_account_id),
  unique key uk_logistics_connection_code (connection_code),
  key idx_logistics_connection_kind (provider_kind, status),
  key idx_logistics_connection_order (display_order, create_time)
) engine=innodb comment='物流商API接入通用连接表';

create table if not exists logistics_agg56_connection (
  carrier_account_id         bigint(20)   not null                 comment '物流商账号ID',
  connection_code            varchar(64)  not null                 comment '物流商接入编号',
  app_token_mask             varchar(64)  not null default ''      comment '脱敏后的app_token',
  app_key_mask               varchar(64)  not null default ''      comment '脱敏后的app_key',
  app_token_ciphertext       text         not null                 comment '加密后的app_token',
  app_key_ciphertext         text         not null                 comment '加密后的app_key',
  credential_key_id          varchar(64)  not null default 'default' comment '加密密钥版本',
  agg56_user_id              varchar(64)  default ''               comment 'AGG56用户ID',
  agg56_user_account_mask    varchar(200) default ''               comment '脱敏后的AGG56账号',
  agg56_customer_code        varchar(64)  default ''               comment 'AGG56客户代码',
  create_time                datetime                              comment '创建时间',
  update_time                datetime                              comment '更新时间',
  primary key (carrier_account_id),
  unique key uk_logistics_agg56_connection_code (connection_code)
) engine=innodb comment='AGG56私有接入信息表';

create table if not exists logistics_carrier_channel_candidate (
  carrier_account_id       bigint(20)   not null                  comment '物流商账号ID',
  connection_code          varchar(64)  not null                  comment '物流商接入编号',
  external_channel_code    varchar(128) not null                  comment '外部渠道代码',
  external_channel_name    varchar(200) not null                  comment '外部渠道名称',
  raw_final_carrier_text   varchar(200) default ''                comment '外部返回的最终承运商原始文本',
  status                   varchar(16)  not null default 'ACTIVE' comment '物流商渠道状态',
  sync_batch_id            varchar(64)  not null                  comment '同步批次号',
  source_payload_json      longtext                               comment '外部原始渠道JSON',
  source_payload_hash      varchar(64)  default ''                comment '外部原始渠道JSON哈希',
  first_seen_time          datetime     not null                  comment '首次发现时间',
  last_seen_time           datetime     not null                  comment '最近发现时间',
  update_time              datetime     not null                  comment '更新时间',
  primary key (carrier_account_id, external_channel_code),
  key idx_logistics_channel_candidate_status (carrier_account_id, status),
  key idx_logistics_channel_candidate_connection (connection_code),
  key idx_logistics_channel_candidate_name (external_channel_name)
) engine=innodb comment='物流商渠道表';

create table if not exists logistics_system_channel (
  system_channel_code      varchar(64)  not null                  comment '系统渠道代码',
  system_channel_name      varchar(200) not null                  comment '系统渠道名称',
  standard_carrier_code    varchar(64)  not null                  comment '标准最终承运商code',
  status                   varchar(16)  not null default 'ENABLED' comment '状态',
  display_order            int          not null default 0        comment '显示排序',
  create_by                varchar(64)  default ''                comment '创建者',
  create_time              datetime                               comment '创建时间',
  update_by                varchar(64)  default ''                comment '更新者',
  update_time              datetime                               comment '更新时间',
  remark                   varchar(500) default ''                comment '备注',
  primary key (system_channel_code),
  key idx_logistics_system_channel_carrier (standard_carrier_code, status),
  key idx_logistics_system_channel_order (display_order, system_channel_code)
) engine=innodb comment='系统物流渠道表';

create table if not exists logistics_carrier_channel_mapping (
  mapping_id                      bigint(20)   not null auto_increment comment '映射ID',
  carrier_account_id              bigint(20)   not null                comment '物流商账号ID',
  connection_code                 varchar(64)  not null                comment '物流商接入编号',
  external_channel_code           varchar(128) not null                comment '外部渠道代码',
  external_channel_name_snapshot  varchar(200) not null                comment '外部渠道名称快照',
  system_channel_code             varchar(64)  not null                comment '系统渠道代码',
  system_channel_name_snapshot    varchar(200) not null                comment '系统渠道名称快照',
  standard_carrier_code           varchar(64)  not null                comment '标准最终承运商code',
  status                          varchar(16)  not null default 'ENABLED' comment '映射状态',
  create_by                       varchar(64)  default ''              comment '创建者',
  create_time                     datetime                             comment '创建时间',
  update_by                       varchar(64)  default ''              comment '更新者',
  update_time                     datetime                             comment '更新时间',
  remark                          varchar(500) default ''              comment '备注',
  primary key (mapping_id),
  unique key uk_logistics_mapping_external (carrier_account_id, external_channel_code),
  key idx_logistics_mapping_system (carrier_account_id, system_channel_code, status),
  key idx_logistics_mapping_connection (connection_code)
) engine=innodb comment='物流商渠道与系统渠道映射表';

create table if not exists logistics_label_order (
  label_order_id          bigint(20)   not null auto_increment comment '面单订单ID',
  business_order_no       varchar(100) not null                comment '全局唯一业务单号',
  carrier_account_id      bigint(20)   not null                comment '物流商账号ID',
  connection_code         varchar(64)  not null                comment '物流商接入编号',
  provider_kind           varchar(32)  not null                comment '接入方类型',
  system_channel_code     varchar(64)  not null                comment '系统渠道代码',
  external_channel_code   varchar(128) not null                comment '外部渠道代码',
  provider_order_no       varchar(100) default ''              comment '物流商订单号',
  status                  varchar(32)  not null default 'CREATING' comment '面单状态',
  label_file_types        varchar(100) default ''              comment '面单文件格式集合',
  zone_code               varchar(64)  default ''              comment '分区代码',
  charge_weight           varchar(64)  default ''              comment '计费重',
  logistics_error         varchar(500) default ''              comment '物流商异常信息',
  create_payload_json     longtext                             comment '创建面单请求快照',
  provider_result_json    longtext                             comment '物流商返回结果快照',
  created_time            datetime                             comment '创建面单时间',
  last_fetched_time       datetime                             comment '最近获取面单时间',
  create_by               varchar(64)  default ''              comment '创建者',
  create_time             datetime                             comment '创建时间',
  update_by               varchar(64)  default ''              comment '更新者',
  update_time             datetime                             comment '更新时间',
  remark                  varchar(500) default ''              comment '备注',
  primary key (label_order_id),
  unique key uk_logistics_label_business_order_no (business_order_no),
  key idx_logistics_label_provider_order (carrier_account_id, provider_order_no),
  key idx_logistics_label_connection (connection_code),
  key idx_logistics_label_status (status, create_time)
) engine=innodb comment='物流商面单订单表';

create table if not exists logistics_label_package (
  label_package_id       bigint(20)   not null auto_increment comment '面单包裹ID',
  label_order_id         bigint(20)   not null                comment '面单订单ID',
  provider_package_no    varchar(100) default ''              comment '物流商包裹/箱号',
  tracking_number        varchar(128) default ''              comment '跟踪号',
  label_url              varchar(1000) default ''             comment '面单URL',
  file_type              varchar(32)  default ''              comment '面单文件格式',
  status                 varchar(32)  not null default 'CREATED' comment '包裹面单状态',
  source_payload_json    longtext                             comment '物流商包裹面单原始JSON',
  create_time            datetime                             comment '创建时间',
  update_time            datetime                             comment '更新时间',
  primary key (label_package_id),
  key idx_logistics_label_order_package (label_order_id, provider_package_no),
  key idx_logistics_label_tracking (tracking_number)
) engine=innodb comment='物流商面单包裹表';

create table if not exists logistics_carrier_request_log (
  request_log_id             bigint(20)   not null auto_increment comment '请求日志ID',
  carrier_account_id         bigint(20)   not null                comment '物流商账号ID',
  connection_code            varchar(64)  not null                comment '物流商接入编号',
  provider_kind              varchar(32)  not null                comment '接入方类型',
  trace_id                   varchar(64)  not null                comment '调用追踪ID',
  operation                  varchar(32)  not null                comment '操作类型',
  endpoint                   varchar(200) not null                comment '外部接口路径',
  http_method                varchar(16)  not null                comment 'HTTP方法',
  business_order_no          varchar(100) default ''              comment '业务单号',
  provider_order_no          varchar(100) default ''              comment '物流商订单号',
  request_time               datetime                             comment '请求时间',
  response_time              datetime                             comment '响应时间',
  duration_ms                bigint(20)   default 0               comment '耗时毫秒',
  http_status                int                                  comment 'HTTP状态码',
  provider_code              varchar(64)  default ''              comment '物流商返回code',
  provider_message           varchar(500) default ''              comment '物流商返回message',
  status                     varchar(16)  not null                comment '调用状态',
  request_payload_redacted   longtext                             comment '脱敏后的请求体',
  response_payload_redacted  longtext                             comment '脱敏后的响应体',
  primary key (request_log_id),
  key idx_logistics_request_log_connection (carrier_account_id, request_time),
  key idx_logistics_request_log_connection_code (connection_code),
  key idx_logistics_request_log_trace (trace_id),
  key idx_logistics_request_log_business (business_order_no),
  key idx_logistics_request_log_provider_order (carrier_account_id, provider_order_no)
) engine=innodb comment='物流商外部请求日志表';

insert ignore into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark) values
('物流商接入方类型', 'logistics_provider_kind', '0', 'admin', sysdate(), '物流商API接入方类型'),
('物流商接入状态', 'logistics_connection_status', '0', 'admin', sysdate(), '物流商接入启停状态'),
('物流商凭据状态', 'logistics_credential_status', '0', 'admin', sysdate(), '物流商授权凭据状态'),
('物流商渠道状态', 'logistics_channel_status', '0', 'admin', sysdate(), '物流商渠道同步状态'),
('物流商映射状态', 'logistics_mapping_status', '0', 'admin', sysdate(), '物流商渠道映射状态'),
('物流商面单状态', 'logistics_label_status', '0', 'admin', sysdate(), '物流商面单状态'),
('物流商面单文件格式', 'logistics_label_file_type', '0', 'admin', sysdate(), '物流商面单文件格式'),
('物流商签名服务', 'logistics_signature_service', '0', 'admin', sysdate(), '物流商签名服务'),
('标准最终承运商', 'logistics_final_carrier', '0', 'admin', sysdate(), '物流商标准最终承运商');

create temporary table if not exists tmp_logistics_carrier_dict_data_seed (
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

truncate table tmp_logistics_carrier_dict_data_seed;
insert into tmp_logistics_carrier_dict_data_seed(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, remark) values
(1, 'AGG56', 'AGG56', 'logistics_provider_kind', 'primary', 'N', '0', 'AGG56物流商系统'),
(1, '启用', 'ENABLED', 'logistics_connection_status', 'success', 'Y', '0', ''),
(2, '停用', 'DISABLED', 'logistics_connection_status', 'default', 'N', '0', ''),
(1, '未配置', 'UNCONFIGURED', 'logistics_credential_status', 'default', 'Y', '0', ''),
(2, '已配置', 'CONFIGURED', 'logistics_credential_status', 'success', 'N', '0', ''),
(3, '无效', 'INVALID', 'logistics_credential_status', 'danger', 'N', '0', ''),
(1, '有效', 'ACTIVE', 'logistics_channel_status', 'success', 'Y', '0', ''),
(2, '已缺失', 'MISSING', 'logistics_channel_status', 'warning', 'N', '0', ''),
(1, '启用', 'ENABLED', 'logistics_mapping_status', 'success', 'Y', '0', ''),
(2, '停用', 'DISABLED', 'logistics_mapping_status', 'default', 'N', '0', ''),
(1, '创建中', 'CREATING', 'logistics_label_status', 'processing', 'N', '0', ''),
(2, '已创建', 'CREATED', 'logistics_label_status', 'success', 'Y', '0', ''),
(3, '等待面单', 'PENDING_LABEL', 'logistics_label_status', 'warning', 'N', '0', ''),
(4, '创建失败', 'FAILED', 'logistics_label_status', 'danger', 'N', '0', ''),
(5, '已取消', 'CANCELLED', 'logistics_label_status', 'default', 'N', '0', ''),
(6, '取消失败', 'CANCEL_FAILED', 'logistics_label_status', 'danger', 'N', '0', ''),
(1, 'PDF', 'PDF', 'logistics_label_file_type', 'primary', 'Y', '0', ''),
(2, 'ZPL', 'ZPL', 'logistics_label_file_type', 'primary', 'N', '0', ''),
(3, 'PNG', 'PNG', 'logistics_label_file_type', 'primary', 'N', '0', ''),
(1, '普通签名', 'SSF', 'logistics_signature_service', 'primary', 'N', '0', ''),
(2, '成人签名', 'ASS', 'logistics_signature_service', 'warning', 'N', '0', ''),
(3, '直接签名', 'DSO', 'logistics_signature_service', 'primary', 'N', '0', ''),
(1, 'UPS', 'UPS', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(2, 'FedEx', 'FEDEX', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(3, 'USPS', 'USPS', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(4, 'DHL', 'DHL', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(5, 'DHL eCommerce', 'DHL_ECOMMERCE', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(6, 'Amazon Logistics', 'AMAZON_LOGISTICS', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(7, 'Amazon Shipping', 'AMAZON_SHIPPING', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(8, 'Ontrac', 'ONTRAC', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(9, 'LaserShip', 'LASERSHIP', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(10, 'TForce Freight', 'TFORCE_FREIGHT', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(11, 'GOFO', 'GOFO', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(12, 'UniUni', 'UNIUNI', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(13, 'SpeedX', 'SPEEDX', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(14, '4PX', '4PX', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(15, 'YANWEN', 'YANWEN', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(16, 'Canada Post', 'CANADA_POST', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(17, 'Royal Mail', 'ROYAL_MAIL', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(18, 'Australia Post', 'AUSTRALIA_POST', 'logistics_final_carrier', 'primary', 'N', '0', '用户提供清单'),
(19, 'Other', 'OTHER', 'logistics_final_carrier', 'default', 'N', '0', '用户提供清单兜底项'),
(20, 'Custom', 'CUSTOM', 'logistics_final_carrier', 'default', 'N', '0', '用户提供清单兜底项'),
(21, '自营', 'SELF_OPERATED', 'logistics_final_carrier', 'default', 'N', '0', '用户提供清单兜底项');

update sys_dict_data d
join tmp_logistics_carrier_dict_data_seed seed
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
from tmp_logistics_carrier_dict_data_seed seed
where not exists (
  select 1
  from sys_dict_data d
  where d.dict_type = seed.dict_type
    and d.dict_value = seed.dict_value
);

create temporary table if not exists tmp_logistics_carrier_button_guard (
  menu_id   bigint(20) not null primary key,
  parent_id bigint(20) not null,
  menu_type char(1) not null,
  perms     varchar(100) not null
);

truncate table tmp_logistics_carrier_button_guard;
insert into tmp_logistics_carrier_button_guard(menu_id, parent_id, menu_type, perms) values
(2510, 2054, 'F', 'logistics:carrier:query'),
(2511, 2054, 'F', 'logistics:carrier:add'),
(2512, 2054, 'F', 'logistics:carrier:edit'),
(2513, 2054, 'F', 'logistics:carrier:credential'),
(2514, 2054, 'F', 'logistics:carrier:sync'),
(2515, 2054, 'F', 'logistics:carrier:channel'),
(2516, 2054, 'F', 'logistics:carrier:label'),
(2517, 2054, 'F', 'logistics:carrier:log');

call assert_logistics_carrier_menu_guard();

update sys_menu
set component = 'Logistics/Carrier/index',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '物流商管理页面'
where menu_id = 2054;

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
(2510, '物流商查询', 2054, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:carrier:query', '#', 'admin', sysdate(), '物流商查询按钮'),
(2511, '物流商新增', 2054, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:carrier:add', '#', 'admin', sysdate(), '物流商新增按钮'),
(2512, '物流商编辑', 2054, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:carrier:edit', '#', 'admin', sysdate(), '物流商编辑按钮'),
(2513, '物流商授权', 2054, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:carrier:credential', '#', 'admin', sysdate(), '物流商凭据授权按钮'),
(2514, '物流商同步', 2054, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:carrier:sync', '#', 'admin', sysdate(), '物流商渠道同步按钮'),
(2515, '物流商渠道', 2054, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:carrier:channel', '#', 'admin', sysdate(), '物流商渠道映射按钮'),
(2516, '物流商面单', 2054, 7, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:carrier:label', '#', 'admin', sysdate(), '物流商面单按钮'),
(2517, '物流商日志', 2054, 8, '#', '', '', '', 1, 0, 'F', '0', '0', 'logistics:carrier:log', '#', 'admin', sysdate(), '物流商请求日志按钮')
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

call assert_logistics_carrier_management_completed();

drop temporary table if exists tmp_logistics_carrier_button_guard;
drop temporary table if exists tmp_logistics_carrier_dict_data_seed;
drop procedure if exists assert_logistics_carrier_menu_guard;
drop procedure if exists assert_logistics_carrier_management_completed;
