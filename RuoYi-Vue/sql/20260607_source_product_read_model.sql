-- Source product library read model.
-- Confirmed scope: source product library list/detail and future source SKU group binding.
-- Source of truth remains upstream_system_sku_candidate / upstream_system_sku_pairing.

set names utf8mb4;

set @confirm_source_product_read_model := coalesce(@confirm_source_product_read_model, '');

delimiter //

drop procedure if exists assert_source_product_read_model_confirmed//
create procedure assert_source_product_read_model_confirmed()
begin
  if coalesce(@confirm_source_product_read_model, '')
      <> 'APPLY_SOURCE_PRODUCT_READ_MODEL' then
    signal sqlstate '45000' set message_text = 'set @confirm_source_product_read_model = APPLY_SOURCE_PRODUCT_READ_MODEL before running this migration';
  end if;
end//

delimiter ;

call assert_source_product_read_model_confirmed();
drop procedure if exists assert_source_product_read_model_confirmed;

create table if not exists source_product_group (
  source_sku_group_key    varchar(96)   not null                 comment '稳定来源SKU组key',
  repository_scope        varchar(32)   not null                 comment '来源仓库范围：OFFICIAL_MASTER/THIRD_PARTY_MASTER',
  master_sku              varchar(128)  not null                 comment '来源SKU',
  master_product_name     varchar(255)  not null                 comment '来源商品名',
  system_kind             varchar(64)   not null default ''      comment '来源系统类型',
  source_connection_codes varchar(1000) not null default ''      comment '来源连接编号列表',
  source_warehouse_names  varchar(1000) not null default ''      comment '来源仓库名称列表',
  warehouse_count         int           not null default 0       comment '来源仓库数量',
  source_row_count        int           not null default 0       comment '来源明细行数量',
  pairing_status          varchar(32)   not null default 'UNASSIGNED' comment '配对状态',
  status                  varchar(16)   not null default 'ACTIVE' comment '同步状态',
  latest_update_time      datetime      not null                 comment '组内最新更新时间',
  search_text             text          not null                 comment '搜索文本',
  rebuild_time            datetime      not null                 comment '读模型构建时间',
  primary key (source_sku_group_key),
  key idx_source_product_group_sku (repository_scope, master_sku),
  key idx_source_product_group_list (repository_scope, latest_update_time, source_sku_group_key),
  key idx_source_product_group_status (repository_scope, status),
  key idx_source_product_group_pairing (repository_scope, pairing_status)
) engine=innodb comment='来源SKU组读模型';

create table if not exists source_product_dimension_group (
  source_dimension_group_key varchar(160)  not null                 comment '稳定来源SKU尺寸组key',
  source_sku_group_key       varchar(96)   not null                 comment '稳定来源SKU组key',
  repository_scope           varchar(32)   not null                 comment '来源仓库范围',
  system_kind                varchar(64)   not null default ''      comment '来源系统类型',
  source_connection_codes    varchar(1000) not null default ''      comment '来源连接编号列表',
  source_warehouse_names     varchar(1000) not null default ''      comment '来源仓库名称列表',
  master_warehouse_name      varchar(1000) not null default ''      comment '来源仓库名称展示',
  warehouse_count            int           not null default 0       comment '来源仓库数量',
  source_row_count           int           not null default 0       comment '来源明细行数量',
  master_sku                 varchar(128)  not null                 comment '来源SKU',
  master_product_name        varchar(255)  not null                 comment '来源商品名',
  product_alias_name         varchar(255)  default ''               comment '来源商品别名',
  approve_status             varchar(32)   default ''               comment '审核状态',
  product_type               int                                    comment '产品类型',
  product_description        text                                   comment '来源商品描述',
  image_url                  varchar(1000) default ''               comment '图片URL',
  main_code                  varchar(128)  default ''               comment '主条码',
  other_code                 varchar(1000) default ''               comment '其他条码',
  fnsku                      varchar(1000) default ''               comment 'FNSKU',
  country_of_origin_name     varchar(100)  default ''               comment '原产国家/地区',
  currency_code              varchar(16)   default ''               comment '币种code',
  customhouse_code           varchar(64)   default ''               comment '海关编码',
  dangerous_cargo            int                                    comment '危险品code',
  declare_name_cn            varchar(255)  default ''               comment '申报中文名',
  declare_name_en            varchar(255)  default ''               comment '申报英文名',
  declare_price              decimal(18,4)                          comment '申报价格',
  product_height             decimal(18,4)                          comment '客户高(cm)',
  product_height_bs          decimal(18,4)                          comment '客户英制高(in)',
  product_length             decimal(18,4)                          comment '客户长(cm)',
  product_length_bs          decimal(18,4)                          comment '客户英制长(in)',
  product_weight             decimal(18,4)                          comment '客户重量(kg)',
  product_weight_bs          decimal(18,4)                          comment '客户英制重量(lb)',
  product_width              decimal(18,4)                          comment '客户宽(cm)',
  product_width_bs           decimal(18,4)                          comment '客户英制宽(in)',
  wms_height                 decimal(18,4)                          comment 'WMS高(cm)',
  wms_height_bs              decimal(18,4)                          comment 'WMS英制高(in)',
  wms_length                 decimal(18,4)                          comment 'WMS长(cm)',
  wms_length_bs              decimal(18,4)                          comment 'WMS英制长(in)',
  wms_weight                 decimal(18,4)                          comment 'WMS重量(kg)',
  wms_weight_bs              decimal(18,4)                          comment 'WMS英制重量(lb)',
  wms_width                  decimal(18,4)                          comment 'WMS宽(cm)',
  wms_width_bs               decimal(18,4)                          comment 'WMS英制宽(in)',
  cat1_name                  varchar(100)  default ''               comment '一级分类',
  cat2_name                  varchar(100)  default ''               comment '二级分类',
  cat3_name                  varchar(100)  default ''               comment '三级分类',
  platform_sku_info_json     longtext                               comment '平台SKU信息JSON摘要',
  brazil_tax_info_json       longtext                               comment '巴西税务信息JSON摘要',
  source_payload_hash        varchar(64)   default ''               comment '来源产品快照hash',
  status                     varchar(16)   not null default 'ACTIVE' comment '同步状态',
  search_text                text          not null                 comment '搜索文本',
  sync_batch_id              varchar(64)   default ''               comment '最新同步批次',
  first_seen_time            datetime      not null                 comment '首次发现时间',
  last_seen_time             datetime      not null                 comment '最近发现时间',
  update_time                datetime      not null                 comment '最新更新时间',
  pairing_status             varchar(32)   not null default 'UNASSIGNED' comment '配对状态',
  sku_pairing_id             bigint(20)                             comment '配对ID摘要',
  system_sku                 varchar(1000) default ''               comment '系统SKU摘要',
  system_sku_name            varchar(1000) default ''               comment '系统商品名摘要',
  customer_name              varchar(1000) default ''               comment '客户名摘要',
  rebuild_time               datetime      not null                 comment '读模型构建时间',
  primary key (source_dimension_group_key),
  key idx_source_product_dimension_group_list (repository_scope, update_time, source_dimension_group_key),
  key idx_source_product_dimension_group_sku_group (source_sku_group_key),
  key idx_source_product_dimension_group_master_sku (repository_scope, master_sku),
  key idx_source_product_dimension_group_status (repository_scope, status),
  key idx_source_product_dimension_group_pairing (repository_scope, pairing_status),
  key idx_source_product_dimension_group_approve (repository_scope, approve_status)
) engine=innodb comment='来源SKU尺寸组读模型';

create table if not exists source_product_warehouse_detail (
  id                         bigint(20)    not null auto_increment comment '主键',
  source_sku_group_key       varchar(96)   not null                 comment '稳定来源SKU组key',
  source_dimension_group_key varchar(160)  not null                 comment '稳定来源SKU尺寸组key',
  repository_scope           varchar(32)   not null                 comment '来源仓库范围',
  connection_code            varchar(64)   not null                 comment '来源连接编号',
  master_warehouse_name      varchar(128)  not null default ''      comment '来源仓库名',
  system_kind                varchar(64)   not null default ''      comment '来源系统类型',
  master_sku                 varchar(128)  not null                 comment '来源SKU',
  master_product_name        varchar(255)  not null                 comment '来源商品名',
  product_height             decimal(18,4)                          comment '客户高(cm)',
  product_height_bs          decimal(18,4)                          comment '客户英制高(in)',
  product_length             decimal(18,4)                          comment '客户长(cm)',
  product_length_bs          decimal(18,4)                          comment '客户英制长(in)',
  product_weight             decimal(18,4)                          comment '客户重量(kg)',
  product_weight_bs          decimal(18,4)                          comment '客户英制重量(lb)',
  product_width              decimal(18,4)                          comment '客户宽(cm)',
  product_width_bs           decimal(18,4)                          comment '客户英制宽(in)',
  wms_height                 decimal(18,4)                          comment 'WMS高(cm)',
  wms_height_bs              decimal(18,4)                          comment 'WMS英制高(in)',
  wms_length                 decimal(18,4)                          comment 'WMS长(cm)',
  wms_length_bs              decimal(18,4)                          comment 'WMS英制长(in)',
  wms_weight                 decimal(18,4)                          comment 'WMS重量(kg)',
  wms_weight_bs              decimal(18,4)                          comment 'WMS英制重量(lb)',
  wms_width                  decimal(18,4)                          comment 'WMS宽(cm)',
  wms_width_bs               decimal(18,4)                          comment 'WMS英制宽(in)',
  status                     varchar(16)   not null default 'ACTIVE' comment '同步状态',
  pairing_status             varchar(32)   not null default 'UNASSIGNED' comment '配对状态',
  sku_pairing_id             bigint(20)                             comment '配对ID',
  system_sku                 varchar(128)  default ''               comment '系统SKU',
  system_sku_name            varchar(255)  default ''               comment '系统商品名',
  customer_name              varchar(200)  default ''               comment '客户名',
  source_payload_hash        varchar(64)   default ''               comment '来源产品快照hash',
  wms_payload_hash           varchar(64)   default ''               comment 'WMS尺寸快照hash',
  first_seen_time            datetime      not null                 comment '首次发现时间',
  last_seen_time             datetime      not null                 comment '最近发现时间',
  update_time                datetime      not null                 comment '更新时间',
  rebuild_time               datetime      not null                 comment '读模型构建时间',
  primary key (id),
  unique key uk_source_product_warehouse_row (repository_scope, connection_code, master_sku),
  key idx_source_product_warehouse_sku_group (source_sku_group_key),
  key idx_source_product_warehouse_dimension_group (source_dimension_group_key),
  key idx_source_product_warehouse_master_sku (repository_scope, master_sku),
  key idx_source_product_warehouse_connection (connection_code)
) engine=innodb comment='来源SKU仓库明细读模型';

set group_concat_max_len = 1048576;

delete from source_product_warehouse_detail where repository_scope = 'OFFICIAL_MASTER';
delete from source_product_dimension_group where repository_scope = 'OFFICIAL_MASTER';
delete from source_product_group where repository_scope = 'OFFICIAL_MASTER';

insert into source_product_group(
  source_sku_group_key, repository_scope, master_sku, master_product_name, system_kind,
  source_connection_codes, source_warehouse_names, warehouse_count, source_row_count,
  pairing_status, status, latest_update_time, search_text, rebuild_time
)
select concat('OFFICIAL_MASTER:', sha2(concat(
         'OFFICIAL_MASTER|',
         char_length(trim(coalesce(c.master_sku, ''))), ':', trim(coalesce(c.master_sku, '')),
         '|',
         char_length(trim(coalesce(c.master_product_name, ''))), ':', trim(coalesce(c.master_product_name, ''))
       ), 256)) as source_sku_group_key,
       'OFFICIAL_MASTER' as repository_scope,
       c.master_sku,
       c.master_product_name,
       max(case when conn.system_kind = 'LINGXING_WMS' then 'lingxing-wms' else conn.system_kind end) as system_kind,
       coalesce(group_concat(distinct c.connection_code order by c.connection_code separator ','), '') as source_connection_codes,
       coalesce(group_concat(distinct conn.master_warehouse_name order by conn.master_warehouse_name separator ' / '), '') as source_warehouse_names,
       count(distinct c.connection_code) as warehouse_count,
       count(1) as source_row_count,
       case
         when sum(case when p.sku_pairing_id is not null then 1 else 0 end) = 0 then 'UNASSIGNED'
         when sum(case when p.sku_pairing_id is not null then 1 else 0 end) = count(1) then 'PAIRED'
         else 'PARTIAL'
       end as pairing_status,
       case when count(distinct c.status) = 1 then max(c.status) else 'MIXED' end as status,
       max(c.update_time) as latest_update_time,
       concat_ws(' ',
         c.master_sku,
         c.master_product_name,
         coalesce(group_concat(distinct conn.master_warehouse_name separator ' '), ''),
         coalesce(group_concat(distinct nullif(p.system_sku, '') separator ' '), ''),
         coalesce(group_concat(distinct nullif(p.system_sku_name, '') separator ' '), ''),
         coalesce(group_concat(distinct nullif(p.customer_name, '') separator ' '), ''),
         coalesce(max(c.search_text), '')
       ) as search_text,
       sysdate() as rebuild_time
from upstream_system_sku_candidate c
inner join upstream_system_connection conn
        on conn.connection_code = c.connection_code
left join upstream_system_sku_pairing p
       on p.connection_code = c.connection_code
      and p.master_sku = c.master_sku
where (conn.system_kind = 'lingxing-wms' or conn.system_kind = 'LINGXING_WMS')
group by c.master_sku, c.master_product_name
on duplicate key update
  system_kind = values(system_kind),
  source_connection_codes = values(source_connection_codes),
  source_warehouse_names = values(source_warehouse_names),
  warehouse_count = values(warehouse_count),
  source_row_count = values(source_row_count),
  pairing_status = values(pairing_status),
  status = values(status),
  latest_update_time = values(latest_update_time),
  search_text = values(search_text),
  rebuild_time = values(rebuild_time);

insert into source_product_dimension_group(
  source_dimension_group_key, source_sku_group_key, repository_scope, system_kind,
  source_connection_codes, source_warehouse_names, master_warehouse_name, warehouse_count, source_row_count,
  master_sku, master_product_name, product_alias_name, approve_status, product_type, product_description,
  image_url, main_code, other_code, fnsku, country_of_origin_name, currency_code, customhouse_code,
  dangerous_cargo, declare_name_cn, declare_name_en, declare_price,
  product_height, product_height_bs, product_length, product_length_bs,
  product_weight, product_weight_bs, product_width, product_width_bs,
  wms_height, wms_height_bs, wms_length, wms_length_bs,
  wms_weight, wms_weight_bs, wms_width, wms_width_bs,
  cat1_name, cat2_name, cat3_name, platform_sku_info_json, brazil_tax_info_json,
  source_payload_hash, status, search_text, sync_batch_id, first_seen_time, last_seen_time, update_time,
  pairing_status, sku_pairing_id, system_sku, system_sku_name, customer_name, rebuild_time
)
select concat(
         concat('OFFICIAL_MASTER:', sha2(concat(
           'OFFICIAL_MASTER|',
           char_length(trim(coalesce(c.master_sku, ''))), ':', trim(coalesce(c.master_sku, '')),
           '|',
           char_length(trim(coalesce(c.master_product_name, ''))), ':', trim(coalesce(c.master_product_name, ''))
         ), 256)),
         ':',
         sha2(concat_ws('|',
           coalesce(cast(c.product_length as char), 'NULL'),
           coalesce(cast(c.product_width as char), 'NULL'),
           coalesce(cast(c.product_height as char), 'NULL'),
           coalesce(cast(c.product_weight as char), 'NULL'),
           coalesce(cast(c.wms_length as char), 'NULL'),
           coalesce(cast(c.wms_width as char), 'NULL'),
           coalesce(cast(c.wms_height as char), 'NULL'),
           coalesce(cast(c.wms_weight as char), 'NULL')
         ), 256)
       ) as source_dimension_group_key,
       concat('OFFICIAL_MASTER:', sha2(concat(
         'OFFICIAL_MASTER|',
         char_length(trim(coalesce(c.master_sku, ''))), ':', trim(coalesce(c.master_sku, '')),
         '|',
         char_length(trim(coalesce(c.master_product_name, ''))), ':', trim(coalesce(c.master_product_name, ''))
       ), 256)) as source_sku_group_key,
       'OFFICIAL_MASTER' as repository_scope,
       max(case when conn.system_kind = 'LINGXING_WMS' then 'lingxing-wms' else conn.system_kind end) as system_kind,
       coalesce(group_concat(distinct c.connection_code order by c.connection_code separator ','), '') as source_connection_codes,
       coalesce(group_concat(distinct conn.master_warehouse_name order by conn.master_warehouse_name separator ' / '), '') as source_warehouse_names,
       coalesce(group_concat(distinct conn.master_warehouse_name order by conn.master_warehouse_name separator ' / '), '') as master_warehouse_name,
       count(distinct c.connection_code) as warehouse_count,
       count(1) as source_row_count,
       c.master_sku,
       c.master_product_name,
       max(c.product_alias_name) as product_alias_name,
       max(c.approve_status) as approve_status,
       max(c.product_type) as product_type,
       max(c.product_description) as product_description,
       max(c.image_url) as image_url,
       max(c.main_code) as main_code,
       max(c.other_code) as other_code,
       max(c.fnsku) as fnsku,
       max(c.country_of_origin_name) as country_of_origin_name,
       max(c.currency_code) as currency_code,
       max(c.customhouse_code) as customhouse_code,
       max(c.dangerous_cargo) as dangerous_cargo,
       max(c.declare_name_cn) as declare_name_cn,
       max(c.declare_name_en) as declare_name_en,
       max(c.declare_price) as declare_price,
       c.product_height,
       max(c.product_height_bs) as product_height_bs,
       c.product_length,
       max(c.product_length_bs) as product_length_bs,
       c.product_weight,
       max(c.product_weight_bs) as product_weight_bs,
       c.product_width,
       max(c.product_width_bs) as product_width_bs,
       c.wms_height,
       max(c.wms_height_bs) as wms_height_bs,
       c.wms_length,
       max(c.wms_length_bs) as wms_length_bs,
       c.wms_weight,
       max(c.wms_weight_bs) as wms_weight_bs,
       c.wms_width,
       max(c.wms_width_bs) as wms_width_bs,
       max(c.cat1_name) as cat1_name,
       max(c.cat2_name) as cat2_name,
       max(c.cat3_name) as cat3_name,
       max(c.platform_sku_info_json) as platform_sku_info_json,
       max(c.brazil_tax_info_json) as brazil_tax_info_json,
       max(c.source_payload_hash) as source_payload_hash,
       case when count(distinct c.status) = 1 then max(c.status) else 'MIXED' end as status,
       concat_ws(' ',
         c.master_sku,
         c.master_product_name,
         coalesce(group_concat(distinct c.connection_code separator ' '), ''),
         coalesce(group_concat(distinct conn.master_warehouse_name separator ' '), ''),
         coalesce(group_concat(distinct nullif(p.system_sku, '') separator ' '), ''),
         coalesce(group_concat(distinct nullif(p.system_sku_name, '') separator ' '), ''),
         coalesce(group_concat(distinct nullif(p.customer_name, '') separator ' '), ''),
         coalesce(max(c.search_text), '')
       ) as search_text,
       max(c.sync_batch_id) as sync_batch_id,
       min(c.first_seen_time) as first_seen_time,
       max(c.last_seen_time) as last_seen_time,
       max(c.update_time) as update_time,
       case
         when sum(case when p.sku_pairing_id is not null then 1 else 0 end) = 0 then 'UNASSIGNED'
         when sum(case when p.sku_pairing_id is not null then 1 else 0 end) = count(1) then 'PAIRED'
         else 'PARTIAL'
       end as pairing_status,
       max(p.sku_pairing_id) as sku_pairing_id,
       coalesce(group_concat(distinct nullif(p.system_sku, '') order by p.system_sku separator ' / '), '') as system_sku,
       coalesce(group_concat(distinct nullif(p.system_sku_name, '') order by p.system_sku_name separator ' / '), '') as system_sku_name,
       coalesce(group_concat(distinct nullif(p.customer_name, '') order by p.customer_name separator ' / '), '') as customer_name,
       sysdate() as rebuild_time
from upstream_system_sku_candidate c
inner join upstream_system_connection conn
        on conn.connection_code = c.connection_code
left join upstream_system_sku_pairing p
       on p.connection_code = c.connection_code
      and p.master_sku = c.master_sku
where (conn.system_kind = 'lingxing-wms' or conn.system_kind = 'LINGXING_WMS')
group by c.master_sku, c.master_product_name,
         c.product_length, c.product_width, c.product_height, c.product_weight,
         c.wms_length, c.wms_width, c.wms_height, c.wms_weight
on duplicate key update
  system_kind = values(system_kind),
  source_connection_codes = values(source_connection_codes),
  source_warehouse_names = values(source_warehouse_names),
  master_warehouse_name = values(master_warehouse_name),
  warehouse_count = values(warehouse_count),
  source_row_count = values(source_row_count),
  product_alias_name = values(product_alias_name),
  approve_status = values(approve_status),
  product_type = values(product_type),
  product_description = values(product_description),
  image_url = values(image_url),
  main_code = values(main_code),
  other_code = values(other_code),
  fnsku = values(fnsku),
  country_of_origin_name = values(country_of_origin_name),
  currency_code = values(currency_code),
  customhouse_code = values(customhouse_code),
  dangerous_cargo = values(dangerous_cargo),
  declare_name_cn = values(declare_name_cn),
  declare_name_en = values(declare_name_en),
  declare_price = values(declare_price),
  product_height_bs = values(product_height_bs),
  product_length_bs = values(product_length_bs),
  product_weight_bs = values(product_weight_bs),
  product_width_bs = values(product_width_bs),
  wms_height_bs = values(wms_height_bs),
  wms_length_bs = values(wms_length_bs),
  wms_weight_bs = values(wms_weight_bs),
  wms_width_bs = values(wms_width_bs),
  cat1_name = values(cat1_name),
  cat2_name = values(cat2_name),
  cat3_name = values(cat3_name),
  platform_sku_info_json = values(platform_sku_info_json),
  brazil_tax_info_json = values(brazil_tax_info_json),
  source_payload_hash = values(source_payload_hash),
  status = values(status),
  search_text = values(search_text),
  sync_batch_id = values(sync_batch_id),
  first_seen_time = values(first_seen_time),
  last_seen_time = values(last_seen_time),
  update_time = values(update_time),
  pairing_status = values(pairing_status),
  sku_pairing_id = values(sku_pairing_id),
  system_sku = values(system_sku),
  system_sku_name = values(system_sku_name),
  customer_name = values(customer_name),
  rebuild_time = values(rebuild_time);

insert into source_product_warehouse_detail(
  source_sku_group_key, source_dimension_group_key, repository_scope, connection_code,
  master_warehouse_name, system_kind, master_sku, master_product_name,
  product_height, product_height_bs, product_length, product_length_bs,
  product_weight, product_weight_bs, product_width, product_width_bs,
  wms_height, wms_height_bs, wms_length, wms_length_bs,
  wms_weight, wms_weight_bs, wms_width, wms_width_bs,
  status, pairing_status, sku_pairing_id, system_sku, system_sku_name, customer_name,
  source_payload_hash, wms_payload_hash, first_seen_time, last_seen_time, update_time, rebuild_time
)
select concat('OFFICIAL_MASTER:', sha2(concat(
         'OFFICIAL_MASTER|',
         char_length(trim(coalesce(c.master_sku, ''))), ':', trim(coalesce(c.master_sku, '')),
         '|',
         char_length(trim(coalesce(c.master_product_name, ''))), ':', trim(coalesce(c.master_product_name, ''))
       ), 256)) as source_sku_group_key,
       concat(
         concat('OFFICIAL_MASTER:', sha2(concat(
           'OFFICIAL_MASTER|',
           char_length(trim(coalesce(c.master_sku, ''))), ':', trim(coalesce(c.master_sku, '')),
           '|',
           char_length(trim(coalesce(c.master_product_name, ''))), ':', trim(coalesce(c.master_product_name, ''))
         ), 256)),
         ':',
         sha2(concat_ws('|',
           coalesce(cast(c.product_length as char), 'NULL'),
           coalesce(cast(c.product_width as char), 'NULL'),
           coalesce(cast(c.product_height as char), 'NULL'),
           coalesce(cast(c.product_weight as char), 'NULL'),
           coalesce(cast(c.wms_length as char), 'NULL'),
           coalesce(cast(c.wms_width as char), 'NULL'),
           coalesce(cast(c.wms_height as char), 'NULL'),
           coalesce(cast(c.wms_weight as char), 'NULL')
         ), 256)
       ) as source_dimension_group_key,
       'OFFICIAL_MASTER' as repository_scope,
       c.connection_code,
       conn.master_warehouse_name,
       case when conn.system_kind = 'LINGXING_WMS' then 'lingxing-wms' else conn.system_kind end as system_kind,
       c.master_sku,
       c.master_product_name,
       c.product_height,
       c.product_height_bs,
       c.product_length,
       c.product_length_bs,
       c.product_weight,
       c.product_weight_bs,
       c.product_width,
       c.product_width_bs,
       c.wms_height,
       c.wms_height_bs,
       c.wms_length,
       c.wms_length_bs,
       c.wms_weight,
       c.wms_weight_bs,
       c.wms_width,
       c.wms_width_bs,
       c.status,
       case when p.sku_pairing_id is null then 'UNASSIGNED' else 'PAIRED' end as pairing_status,
       p.sku_pairing_id,
       p.system_sku,
       p.system_sku_name,
       p.customer_name,
       c.source_payload_hash,
       c.wms_payload_hash,
       c.first_seen_time,
       c.last_seen_time,
       c.update_time,
       sysdate() as rebuild_time
from upstream_system_sku_candidate c
inner join upstream_system_connection conn
        on conn.connection_code = c.connection_code
left join upstream_system_sku_pairing p
       on p.connection_code = c.connection_code
      and p.master_sku = c.master_sku
where (conn.system_kind = 'lingxing-wms' or conn.system_kind = 'LINGXING_WMS')
on duplicate key update
  source_sku_group_key = values(source_sku_group_key),
  source_dimension_group_key = values(source_dimension_group_key),
  master_warehouse_name = values(master_warehouse_name),
  system_kind = values(system_kind),
  master_product_name = values(master_product_name),
  product_height = values(product_height),
  product_height_bs = values(product_height_bs),
  product_length = values(product_length),
  product_length_bs = values(product_length_bs),
  product_weight = values(product_weight),
  product_weight_bs = values(product_weight_bs),
  product_width = values(product_width),
  product_width_bs = values(product_width_bs),
  wms_height = values(wms_height),
  wms_height_bs = values(wms_height_bs),
  wms_length = values(wms_length),
  wms_length_bs = values(wms_length_bs),
  wms_weight = values(wms_weight),
  wms_weight_bs = values(wms_weight_bs),
  wms_width = values(wms_width),
  wms_width_bs = values(wms_width_bs),
  status = values(status),
  pairing_status = values(pairing_status),
  sku_pairing_id = values(sku_pairing_id),
  system_sku = values(system_sku),
  system_sku_name = values(system_sku_name),
  customer_name = values(customer_name),
  source_payload_hash = values(source_payload_hash),
  wms_payload_hash = values(wms_payload_hash),
  first_seen_time = values(first_seen_time),
  last_seen_time = values(last_seen_time),
  update_time = values(update_time),
  rebuild_time = values(rebuild_time);
