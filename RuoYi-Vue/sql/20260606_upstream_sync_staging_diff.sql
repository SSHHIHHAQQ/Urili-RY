-- Upstream sync staging diff migration.
-- Scope:
-- 1. Add source payload hash columns for warehouse/logistics and WMS payload hash columns for SKU dimensions.
-- 2. Create staging tables and sync batch/state tables.
-- 3. Split RuoYi sys_job entries by sync type and move heavy dimension sync to 23:59 Beijing time.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_upstream_sync_staging_diff := coalesce(@confirm_upstream_sync_staging_diff, '');
set @upstream_sync_staging_diff_job_expected_count :=
    coalesce(@upstream_sync_staging_diff_job_expected_count, '');
set @upstream_sync_staging_diff_job_expected_signature :=
    coalesce(@upstream_sync_staging_diff_job_expected_signature, '');

delimiter //

drop procedure if exists assert_upstream_sync_staging_diff_confirmed//
create procedure assert_upstream_sync_staging_diff_confirmed()
begin
  if coalesce(@confirm_upstream_sync_staging_diff, '')
      <> 'APPLY_UPSTREAM_SYNC_STAGING_DIFF' then
    signal sqlstate '45000' set message_text = 'set @confirm_upstream_sync_staging_diff = APPLY_UPSTREAM_SYNC_STAGING_DIFF before running this migration';
  end if;

  if coalesce(@upstream_sync_staging_diff_job_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @upstream_sync_staging_diff_job_expected_count after previewing exact upstream sync staging diff sys_job rows';
  end if;
  if coalesce(@upstream_sync_staging_diff_job_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @upstream_sync_staging_diff_job_expected_signature after previewing exact upstream sync staging diff sys_job rows';
  end if;
end//

drop procedure if exists add_column_if_missing//
create procedure add_column_if_missing(in p_table varchar(64), in p_column varchar(64), in p_definition text)
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table
      and column_name = p_column
  ) then
    set @ddl = concat('alter table ', p_table, ' add column ', p_column, ' ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists assert_column_exists//
create procedure assert_column_exists(in p_table varchar(64), in p_column varchar(64), in p_message varchar(255))
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table
      and column_name = p_column
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_upstream_sync_staging_diff_job_targets//
create procedure assert_upstream_sync_staging_diff_job_targets()
begin
  declare v_count int default 0;
  declare v_signature varchar(64) default '';
  declare v_group_count int default 0;

  select count(1)
    into v_group_count
  from sys_job
  where invoke_target in ('upstreamSystemTask.syncSkus', 'upstreamSystemTask.syncSkuInfo');
  if v_group_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream sync staging diff SKU info sys_job target must be unique before upsert';
  end if;

  select count(1)
    into v_group_count
  from sys_job
  where invoke_target = 'upstreamSystemTask.syncWarehouses';
  if v_group_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream sync staging diff warehouse sys_job target must be unique before upsert';
  end if;

  select count(1)
    into v_group_count
  from sys_job
  where invoke_target = 'upstreamSystemTask.syncLogisticsChannels';
  if v_group_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream sync staging diff logistics channel sys_job target must be unique before upsert';
  end if;

  select count(1)
    into v_group_count
  from sys_job
  where invoke_target = 'upstreamSystemTask.syncSkuDimensions';
  if v_group_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream sync staging diff SKU dimension sys_job target must be unique before upsert';
  end if;

  select count(1)
    into v_group_count
  from sys_job
  where invoke_target = 'upstreamSystemTask.syncInventory';
  if v_group_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream sync staging diff inventory sys_job target must be unique before upsert';
  end if;

  select count(1)
    into v_group_count
  from sys_job
  where invoke_target in (
      'upstreamSkuInfoSyncTask.sync',
      'upstreamWarehouseSyncTask.sync',
      'upstreamLogisticsChannelSyncTask.sync',
      'upstreamSkuDimensionSyncTask.sync',
      'upstreamInventorySyncTask.sync'
  );
  if v_group_count > 0 then
    signal sqlstate '45000' set message_text = 'upstream sync staging diff must run before upstream task component split sys_job targets exist';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             job_id,
             coalesce(job_name, ''),
             coalesce(job_group, ''),
             coalesce(invoke_target, ''),
             coalesce(cron_expression, ''),
             coalesce(misfire_policy, ''),
             coalesce(concurrent, ''),
             coalesce(status, '')
           )
           order by job_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from sys_job
  where invoke_target in (
      'upstreamSystemTask.syncSkus',
      'upstreamSystemTask.syncSkuInfo',
      'upstreamSystemTask.syncWarehouses',
      'upstreamSystemTask.syncLogisticsChannels',
      'upstreamSystemTask.syncSkuDimensions',
      'upstreamSystemTask.syncInventory'
  );

  if v_count <> cast(@upstream_sync_staging_diff_job_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'upstream sync staging diff sys_job exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@upstream_sync_staging_diff_job_expected_signature) then
    signal sqlstate '45000' set message_text = 'upstream sync staging diff sys_job exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_upstream_sync_staging_diff_confirmed();
drop procedure if exists assert_upstream_sync_staging_diff_confirmed;

call assert_column_exists('upstream_system_warehouse_candidate', 'status',
  'upstream_system_warehouse_candidate.status column is required before upstream sync staging diff migration');
call add_column_if_missing('upstream_system_warehouse_candidate', 'source_payload_json',
  'longtext comment ''上游仓库原始行JSON快照'' after status');
call add_column_if_missing('upstream_system_warehouse_candidate', 'source_payload_hash',
  'varchar(64) default '''' comment ''上游仓库原始行JSON哈希'' after source_payload_json');

call assert_column_exists('upstream_system_logistics_channel_candidate', 'status',
  'upstream_system_logistics_channel_candidate.status column is required before upstream sync staging diff migration');
call add_column_if_missing('upstream_system_logistics_channel_candidate', 'source_payload_json',
  'longtext comment ''上游渠道原始行JSON快照'' after status');
call add_column_if_missing('upstream_system_logistics_channel_candidate', 'source_payload_hash',
  'varchar(64) default '''' comment ''上游渠道原始行JSON哈希'' after source_payload_json');

call assert_column_exists('upstream_system_sku_candidate', 'source_payload_hash',
  'upstream_system_sku_candidate.source_payload_hash column is required before upstream sync staging diff migration');
call add_column_if_missing('upstream_system_sku_candidate', 'wms_payload_json',
  'longtext comment ''WMS尺寸重量原始行JSON快照'' after source_payload_hash');
call add_column_if_missing('upstream_system_sku_candidate', 'wms_payload_hash',
  'varchar(64) default '''' comment ''WMS尺寸重量原始行JSON哈希'' after wms_payload_json');

drop procedure if exists add_column_if_missing;
drop procedure if exists assert_column_exists;

create table if not exists upstream_system_sync_state (
  state_id            bigint(20)   not null auto_increment comment '同步状态ID',
  connection_code     varchar(64)  not null                comment '主仓接入编号',
  sync_type           varchar(32)  not null                comment '同步类型',
  status              varchar(16)  not null default 'NEVER' comment '同步状态',
  sync_batch_id       varchar(64)  default null            comment '最近同步批次号',
  last_started_time   datetime                             comment '最近开始同步时间',
  last_finished_time  datetime                             comment '最近结束同步时间',
  last_success_time   datetime                             comment '最近成功同步时间',
  next_sync_time      datetime                             comment '下次计划同步时间',
  total_count         int         not null default 0       comment '最近拉取总数',
  success_count       int         not null default 0       comment '最近成功处理数',
  failed_count        int         not null default 0       comment '最近失败数',
  last_error_code     varchar(64) default ''               comment '最近错误码',
  last_error_message  varchar(500) default ''              comment '最近错误信息',
  last_mode           varchar(32) default ''               comment '最近同步模式',
  rate_limit_ms       int         not null default 0       comment '最近限速间隔毫秒',
  update_time         datetime                             comment '更新时间',
  primary key (state_id),
  unique key uk_upstream_sync_state_type (connection_code, sync_type),
  key idx_upstream_sync_state_status (sync_type, status)
) engine=innodb comment='上游系统分项同步状态';

create table if not exists upstream_system_sync_batch (
  sync_batch_id   varchar(64)  not null                comment '同步批次号',
  connection_code varchar(64)  not null                comment '主仓接入编号',
  sync_type       varchar(32)  not null                comment '同步类型',
  mode            varchar(32)  not null                comment '同步模式',
  status          varchar(16)  not null                comment '同步状态',
  pulled_count    int         not null default 0       comment '拉取行数',
  inserted_count  int         not null default 0       comment '新增行数',
  changed_count   int         not null default 0       comment '变更行数',
  unchanged_count int         not null default 0       comment '未变化行数',
  disabled_count  int         not null default 0       comment '停用行数',
  failed_count    int         not null default 0       comment '失败行数',
  started_time    datetime                             comment '开始时间',
  finished_time   datetime                             comment '结束时间',
  error_message   varchar(500) default ''              comment '失败原因',
  primary key (sync_batch_id),
  key idx_upstream_sync_batch_connection (connection_code, sync_type, started_time)
) engine=innodb comment='上游系统同步批次';

create table if not exists upstream_system_warehouse_candidate_stage (
  stage_id            bigint(20)   not null auto_increment comment 'staging记录ID',
  connection_code     varchar(64)  not null                comment '主仓接入编号',
  sync_batch_id       varchar(64)  not null                comment '同步批次号',
  warehouse_code      varchar(100) not null                comment '上游仓库代码',
  warehouse_name      varchar(200) not null                comment '上游仓库名称',
  country_code        varchar(32)  default ''              comment '国家/地区代码',
  source_payload_json longtext                             comment '上游原始行JSON',
  source_payload_hash varchar(64)  default ''              comment '上游原始行JSON哈希',
  create_time         datetime    not null                 comment '写入staging时间',
  primary key (stage_id),
  unique key uk_stage_wh_batch (connection_code, sync_batch_id, warehouse_code),
  key idx_stage_wh_batch (connection_code, sync_batch_id)
) engine=innodb comment='上游仓库同步staging';

create table if not exists upstream_system_logistics_channel_candidate_stage (
  stage_id            bigint(20)   not null auto_increment comment 'staging记录ID',
  connection_code     varchar(64)  not null                comment '主仓接入编号',
  sync_batch_id       varchar(64)  not null                comment '同步批次号',
  warehouse_code      varchar(100) not null                comment '上游仓库代码',
  channel_code        varchar(100) not null                comment '上游渠道代码',
  channel_name        varchar(200) not null                comment '上游渠道名称',
  source_payload_json longtext                             comment '上游原始行JSON',
  source_payload_hash varchar(64)  default ''              comment '上游原始行JSON哈希',
  create_time         datetime    not null                 comment '写入staging时间',
  primary key (stage_id),
  unique key uk_stage_channel_batch (connection_code, sync_batch_id, warehouse_code, channel_code),
  key idx_stage_channel_batch (connection_code, sync_batch_id)
) engine=innodb comment='上游物流渠道同步staging';

create table if not exists upstream_system_sku_candidate_stage (
  stage_id              bigint(20)    not null auto_increment comment 'staging记录ID',
  connection_code       varchar(64)   not null                 comment '主仓接入编号',
  sync_batch_id         varchar(64)   not null                 comment '同步批次号',
  master_sku            varchar(128)  not null                 comment '领星masterSku',
  master_product_name   varchar(255)  not null                 comment '领星产品名称',
  product_alias_name    varchar(255)  default ''               comment '领星产品别名',
  approve_status        varchar(32)   default ''               comment '领星产品审核状态',
  product_type          int                                    comment '领星产品类型',
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
  search_text           text          not null                 comment '搜索文本',
  create_time           datetime      not null                 comment '写入staging时间',
  primary key (stage_id),
  unique key uk_stage_sku_batch (connection_code, sync_batch_id, master_sku),
  key idx_stage_sku_hash (connection_code, master_sku, source_payload_hash)
) engine=innodb comment='领星SKU同步staging';

create table if not exists upstream_system_sku_dimension_stage (
  stage_id            bigint(20)   not null auto_increment comment 'staging记录ID',
  connection_code     varchar(64)  not null                comment '主仓接入编号',
  sync_batch_id       varchar(64)  not null                comment '同步批次号',
  master_sku          varchar(128) not null                comment '上游masterSku',
  master_product_name varchar(255) default ''              comment '上游产品名称',
  wms_height          decimal(18,4)                        comment 'WMS高(cm)',
  wms_height_bs       decimal(18,4)                        comment 'WMS英制高(in)',
  wms_length          decimal(18,4)                        comment 'WMS长(cm)',
  wms_length_bs       decimal(18,4)                        comment 'WMS英制长(in)',
  wms_weight          decimal(18,4)                        comment 'WMS重量(kg)',
  wms_weight_bs       decimal(18,4)                        comment 'WMS英制重量(lb)',
  wms_width           decimal(18,4)                        comment 'WMS宽(cm)',
  wms_width_bs        decimal(18,4)                        comment 'WMS英制宽(in)',
  source_payload_json longtext                             comment '上游原始行JSON',
  source_payload_hash varchar(64) default ''               comment '上游原始行JSON哈希',
  create_time         datetime    not null                 comment '写入staging时间',
  primary key (stage_id),
  unique key uk_stage_sku_dimension_batch (connection_code, sync_batch_id, master_sku),
  key idx_stage_sku_dimension_batch (connection_code, sync_batch_id)
) engine=innodb comment='上游SKU仓库尺寸重量同步staging';

call assert_upstream_sync_staging_diff_job_targets();

start transaction;

update sys_job
set job_name = '领星SKU信息每日同步',
    job_group = 'SYSTEM',
    invoke_target = 'upstreamSystemTask.syncSkuInfo',
    cron_expression = '0 40 23 * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每天23:40同步领星SKU基础信息，不包含SKU仓库尺寸重量。'
where invoke_target in ('upstreamSystemTask.syncSkus', 'upstreamSystemTask.syncSkuInfo');

insert into sys_job(job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
select '领星SKU信息每日同步', 'SYSTEM', 'upstreamSystemTask.syncSkuInfo', '0 40 23 * * ?', '3', '1', '0', 'admin', sysdate(),
       '每天23:40同步领星SKU基础信息，不包含SKU仓库尺寸重量。'
where not exists (
  select 1 from sys_job
  where invoke_target in ('upstreamSystemTask.syncSkus', 'upstreamSystemTask.syncSkuInfo')
);

update sys_job
set job_name = '领星仓库每日同步',
    job_group = 'SYSTEM',
    invoke_target = 'upstreamSystemTask.syncWarehouses',
    cron_expression = '0 20 23 * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每天23:20同步领星仓库清单。'
where invoke_target = 'upstreamSystemTask.syncWarehouses';

insert into sys_job(job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
select '领星仓库每日同步', 'SYSTEM', 'upstreamSystemTask.syncWarehouses', '0 20 23 * * ?', '3', '1', '0', 'admin', sysdate(),
       '每天23:20同步领星仓库清单。'
where not exists (
  select 1 from sys_job
  where invoke_target = 'upstreamSystemTask.syncWarehouses'
);

update sys_job
set job_name = '领星物流渠道每日同步',
    job_group = 'SYSTEM',
    invoke_target = 'upstreamSystemTask.syncLogisticsChannels',
    cron_expression = '0 30 23 * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每天23:30同步领星物流渠道清单。'
where invoke_target = 'upstreamSystemTask.syncLogisticsChannels';

insert into sys_job(job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
select '领星物流渠道每日同步', 'SYSTEM', 'upstreamSystemTask.syncLogisticsChannels', '0 30 23 * * ?', '3', '1', '0', 'admin', sysdate(),
       '每天23:30同步领星物流渠道清单。'
where not exists (
  select 1 from sys_job
  where invoke_target = 'upstreamSystemTask.syncLogisticsChannels'
);

update sys_job
set job_name = '领星SKU仓库尺寸重量每日限速同步',
    job_group = 'SYSTEM',
    invoke_target = 'upstreamSystemTask.syncSkuDimensions',
    cron_expression = '0 59 23 * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每天23:59限速同步领星SKU仓库尺寸重量。'
where invoke_target = 'upstreamSystemTask.syncSkuDimensions';

insert into sys_job(job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
select '领星SKU仓库尺寸重量每日限速同步', 'SYSTEM', 'upstreamSystemTask.syncSkuDimensions', '0 59 23 * * ?', '3', '1', '0', 'admin', sysdate(),
       '每天23:59限速同步领星SKU仓库尺寸重量。'
where not exists (
  select 1 from sys_job
  where invoke_target = 'upstreamSystemTask.syncSkuDimensions'
);

update sys_job
set job_name = '领星SKU库存每10分钟同步',
    job_group = 'SYSTEM',
    invoke_target = 'upstreamSystemTask.syncInventory',
    cron_expression = '0 0/10 * * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每10分钟增量同步领星SKU库存。'
where invoke_target = 'upstreamSystemTask.syncInventory';

insert into sys_job(job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
select '领星SKU库存每10分钟同步', 'SYSTEM', 'upstreamSystemTask.syncInventory', '0 0/10 * * * ?', '3', '1', '0', 'admin', sysdate(),
       '每10分钟增量同步领星SKU库存。'
where not exists (
  select 1 from sys_job
  where invoke_target = 'upstreamSystemTask.syncInventory'
);

commit;

drop procedure if exists assert_upstream_sync_staging_diff_job_targets;
