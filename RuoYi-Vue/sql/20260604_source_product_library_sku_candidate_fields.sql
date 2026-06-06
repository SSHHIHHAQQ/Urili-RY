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

delimiter ;

call assert_source_product_library_sku_candidate_fields_confirmed();
drop procedure if exists assert_source_product_library_sku_candidate_fields_confirmed;

alter table upstream_system_sku_candidate
  add column product_alias_name varchar(255) default '' comment '领星产品别名' after master_product_name,
  add column approve_status varchar(32) default '' comment '领星产品审核状态' after product_alias_name,
  add column product_type int comment '领星产品类型：0自有产品，1分销产品' after approve_status,
  add column product_description text comment '领星产品描述' after product_type,
  add column image_url varchar(1000) default '' comment '领星产品图片URL' after product_description,
  add column main_code varchar(128) default '' comment '产品条码(EAN/UPC)' after image_url,
  add column other_code varchar(1000) default '' comment '其他条码' after main_code,
  add column fnsku varchar(1000) default '' comment 'FNSKU' after other_code,
  add column country_of_origin_name varchar(100) default '' comment '原产国家/地区代码或名称' after fnsku,
  add column currency_code varchar(16) default '' comment '申报币种code' after country_of_origin_name,
  add column customhouse_code varchar(64) default '' comment '海关编码' after currency_code,
  add column dangerous_cargo int comment '所属危险品code' after customhouse_code,
  add column declare_name_cn varchar(255) default '' comment '申报中文名' after dangerous_cargo,
  add column declare_name_en varchar(255) default '' comment '申报英文名' after declare_name_cn,
  add column declare_price decimal(18,4) comment '申报价格' after declare_name_en,
  add column product_height decimal(18,4) comment '产品高(cm)' after declare_price,
  add column product_height_bs decimal(18,4) comment '产品英制高(in)' after product_height,
  add column product_length decimal(18,4) comment '产品长(cm)' after product_height_bs,
  add column product_length_bs decimal(18,4) comment '产品英制长(in)' after product_length,
  add column product_weight decimal(18,4) comment '产品重量(kg)' after product_length_bs,
  add column product_weight_bs decimal(18,4) comment '产品英制重量(lb)' after product_weight,
  add column product_width decimal(18,4) comment '产品宽(cm)' after product_weight_bs,
  add column product_width_bs decimal(18,4) comment '产品英制宽(in)' after product_width,
  add column wms_height decimal(18,4) comment 'WMS高(cm)' after product_width_bs,
  add column wms_height_bs decimal(18,4) comment 'WMS英制高(in)' after wms_height,
  add column wms_length decimal(18,4) comment 'WMS长(cm)' after wms_height_bs,
  add column wms_length_bs decimal(18,4) comment 'WMS英制长(in)' after wms_length,
  add column wms_weight decimal(18,4) comment 'WMS重量(kg)' after wms_length_bs,
  add column wms_weight_bs decimal(18,4) comment 'WMS英制重量(lb)' after wms_weight,
  add column wms_width decimal(18,4) comment 'WMS宽(cm)' after wms_weight_bs,
  add column wms_width_bs decimal(18,4) comment 'WMS英制宽(in)' after wms_width,
  add column cat1_name varchar(100) default '' comment '领星一级分类名称' after wms_width_bs,
  add column cat2_name varchar(100) default '' comment '领星二级分类名称' after cat1_name,
  add column cat3_name varchar(100) default '' comment '领星三级分类名称' after cat2_name,
  add column platform_sku_info_json longtext comment '平台SKU信息JSON' after cat3_name,
  add column brazil_tax_info_json longtext comment '巴西税务信息JSON' after platform_sku_info_json,
  add column source_payload_json longtext comment '领星产品原始行JSON快照' after brazil_tax_info_json,
  add column source_payload_hash varchar(64) default '' comment '领星产品原始行JSON哈希' after source_payload_json;

create index idx_upstream_sku_candidate_main_code
  on upstream_system_sku_candidate (connection_code, main_code);

create index idx_upstream_sku_candidate_fnsku
  on upstream_system_sku_candidate (connection_code, fnsku(191));

create index idx_upstream_sku_candidate_approve
  on upstream_system_sku_candidate (connection_code, approve_status);

create index idx_upstream_sku_candidate_category
  on upstream_system_sku_candidate (connection_code, cat1_name, cat2_name, cat3_name);
