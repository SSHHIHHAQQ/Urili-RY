-- Source product library field migration for upstream_system_sku_candidate.
-- Scope: add Lingxing product snapshot fields used by the admin source product library.
-- Run after upstream_system_management_seed.sql has created upstream_system_sku_candidate.

set names utf8mb4;

set @confirm_source_product_library_sku_candidate_fields := coalesce(@confirm_source_product_library_sku_candidate_fields, '');

delimiter //

drop procedure if exists assert_source_product_library_sku_candidate_fields_confirmed//
create procedure assert_source_product_library_sku_candidate_fields_confirmed()
begin
  if coalesce(@confirm_source_product_library_sku_candidate_fields, '')
      <> 'APPLY_SOURCE_PRODUCT_LIBRARY_SKU_CANDIDATE_FIELDS' then
    signal sqlstate '45000' set message_text = 'set @confirm_source_product_library_sku_candidate_fields = APPLY_SOURCE_PRODUCT_LIBRARY_SKU_CANDIDATE_FIELDS before running this migration';
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

drop procedure if exists create_index_if_missing//
create procedure create_index_if_missing(in p_table varchar(64), in p_index varchar(64), in p_definition text)
begin
  if not exists (
    select 1
    from information_schema.statistics
    where table_schema = database()
      and table_name = p_table
      and index_name = p_index
  ) then
    set @ddl = concat('create index ', p_index, ' on ', p_table, ' ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

delimiter ;

call assert_source_product_library_sku_candidate_fields_confirmed();
drop procedure if exists assert_source_product_library_sku_candidate_fields_confirmed;

call assert_column_exists('upstream_system_sku_candidate', 'master_product_name',
  'upstream_system_sku_candidate.master_product_name column is required before source product library field migration');

call add_column_if_missing('upstream_system_sku_candidate', 'product_alias_name',
  'varchar(255) default '''' comment ''领星产品别名'' after master_product_name');
call add_column_if_missing('upstream_system_sku_candidate', 'approve_status',
  'varchar(32) default '''' comment ''领星产品审核状态'' after product_alias_name');
call add_column_if_missing('upstream_system_sku_candidate', 'product_type',
  'int comment ''领星产品类型：0自有产品，1分销产品'' after approve_status');
call add_column_if_missing('upstream_system_sku_candidate', 'product_description',
  'text comment ''领星产品描述'' after product_type');
call add_column_if_missing('upstream_system_sku_candidate', 'image_url',
  'varchar(1000) default '''' comment ''领星产品图片URL'' after product_description');
call add_column_if_missing('upstream_system_sku_candidate', 'main_code',
  'varchar(128) default '''' comment ''产品条码(EAN/UPC)'' after image_url');
call add_column_if_missing('upstream_system_sku_candidate', 'other_code',
  'varchar(1000) default '''' comment ''其他条码'' after main_code');
call add_column_if_missing('upstream_system_sku_candidate', 'fnsku',
  'varchar(1000) default '''' comment ''FNSKU'' after other_code');
call add_column_if_missing('upstream_system_sku_candidate', 'country_of_origin_name',
  'varchar(100) default '''' comment ''原产国家/地区代码或名称'' after fnsku');
call add_column_if_missing('upstream_system_sku_candidate', 'currency_code',
  'varchar(16) default '''' comment ''申报币种code'' after country_of_origin_name');
call add_column_if_missing('upstream_system_sku_candidate', 'customhouse_code',
  'varchar(64) default '''' comment ''海关编码'' after currency_code');
call add_column_if_missing('upstream_system_sku_candidate', 'dangerous_cargo',
  'int comment ''所属危险品code'' after customhouse_code');
call add_column_if_missing('upstream_system_sku_candidate', 'declare_name_cn',
  'varchar(255) default '''' comment ''申报中文名'' after dangerous_cargo');
call add_column_if_missing('upstream_system_sku_candidate', 'declare_name_en',
  'varchar(255) default '''' comment ''申报英文名'' after declare_name_cn');
call add_column_if_missing('upstream_system_sku_candidate', 'declare_price',
  'decimal(18,4) comment ''申报价格'' after declare_name_en');
call add_column_if_missing('upstream_system_sku_candidate', 'product_height',
  'decimal(18,4) comment ''产品高(cm)'' after declare_price');
call add_column_if_missing('upstream_system_sku_candidate', 'product_height_bs',
  'decimal(18,4) comment ''产品英制高(in)'' after product_height');
call add_column_if_missing('upstream_system_sku_candidate', 'product_length',
  'decimal(18,4) comment ''产品长(cm)'' after product_height_bs');
call add_column_if_missing('upstream_system_sku_candidate', 'product_length_bs',
  'decimal(18,4) comment ''产品英制长(in)'' after product_length');
call add_column_if_missing('upstream_system_sku_candidate', 'product_weight',
  'decimal(18,4) comment ''产品重量(kg)'' after product_length_bs');
call add_column_if_missing('upstream_system_sku_candidate', 'product_weight_bs',
  'decimal(18,4) comment ''产品英制重量(lb)'' after product_weight');
call add_column_if_missing('upstream_system_sku_candidate', 'product_width',
  'decimal(18,4) comment ''产品宽(cm)'' after product_weight_bs');
call add_column_if_missing('upstream_system_sku_candidate', 'product_width_bs',
  'decimal(18,4) comment ''产品英制宽(in)'' after product_width');
call add_column_if_missing('upstream_system_sku_candidate', 'wms_height',
  'decimal(18,4) comment ''WMS高(cm)'' after product_width_bs');
call add_column_if_missing('upstream_system_sku_candidate', 'wms_height_bs',
  'decimal(18,4) comment ''WMS英制高(in)'' after wms_height');
call add_column_if_missing('upstream_system_sku_candidate', 'wms_length',
  'decimal(18,4) comment ''WMS长(cm)'' after wms_height_bs');
call add_column_if_missing('upstream_system_sku_candidate', 'wms_length_bs',
  'decimal(18,4) comment ''WMS英制长(in)'' after wms_length');
call add_column_if_missing('upstream_system_sku_candidate', 'wms_weight',
  'decimal(18,4) comment ''WMS重量(kg)'' after wms_length_bs');
call add_column_if_missing('upstream_system_sku_candidate', 'wms_weight_bs',
  'decimal(18,4) comment ''WMS英制重量(lb)'' after wms_weight');
call add_column_if_missing('upstream_system_sku_candidate', 'wms_width',
  'decimal(18,4) comment ''WMS宽(cm)'' after wms_weight_bs');
call add_column_if_missing('upstream_system_sku_candidate', 'wms_width_bs',
  'decimal(18,4) comment ''WMS英制宽(in)'' after wms_width');
call add_column_if_missing('upstream_system_sku_candidate', 'cat1_name',
  'varchar(100) default '''' comment ''领星一级分类名称'' after wms_width_bs');
call add_column_if_missing('upstream_system_sku_candidate', 'cat2_name',
  'varchar(100) default '''' comment ''领星二级分类名称'' after cat1_name');
call add_column_if_missing('upstream_system_sku_candidate', 'cat3_name',
  'varchar(100) default '''' comment ''领星三级分类名称'' after cat2_name');
call add_column_if_missing('upstream_system_sku_candidate', 'platform_sku_info_json',
  'longtext comment ''平台SKU信息JSON'' after cat3_name');
call add_column_if_missing('upstream_system_sku_candidate', 'brazil_tax_info_json',
  'longtext comment ''巴西税务信息JSON'' after platform_sku_info_json');
call add_column_if_missing('upstream_system_sku_candidate', 'source_payload_json',
  'longtext comment ''领星产品原始行JSON快照'' after brazil_tax_info_json');
call add_column_if_missing('upstream_system_sku_candidate', 'source_payload_hash',
  'varchar(64) default '''' comment ''领星产品原始行JSON哈希'' after source_payload_json');

call create_index_if_missing('upstream_system_sku_candidate', 'idx_upstream_sku_candidate_main_code',
  '(connection_code, main_code)');
call create_index_if_missing('upstream_system_sku_candidate', 'idx_upstream_sku_candidate_fnsku',
  '(connection_code, fnsku(191))');
call create_index_if_missing('upstream_system_sku_candidate', 'idx_upstream_sku_candidate_approve',
  '(connection_code, approve_status)');
call create_index_if_missing('upstream_system_sku_candidate', 'idx_upstream_sku_candidate_category',
  '(connection_code, cat1_name, cat2_name, cat3_name)');

drop procedure if exists create_index_if_missing;
drop procedure if exists add_column_if_missing;
drop procedure if exists assert_column_exists;
