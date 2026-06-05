-- Mall product SKU dimension fields.
-- Scope: product_sku length/width/height fields for admin SKU editor.
-- Confirm active datasource before executing.

set names utf8mb4;

alter table product_sku
  add column length_value varchar(128) default '' comment '长度，含单位文本' after size,
  add column width_value varchar(128) default '' comment '宽度，含单位文本' after length_value,
  add column height_value varchar(128) default '' comment '高度，含单位文本' after width_value;

update product_sku
set length_value = '24cm', width_value = '18cm', height_value = '2cm'
where system_sku_code in ('SKUDEMO202606050001', 'SKUDEMO202606050002');

update product_sku
set length_value = '32cm', width_value = '21cm', height_value = '12cm'
where system_sku_code in ('SKUDEMO202606050003', 'SKUDEMO202606050004');

update product_sku
set length_value = '46cm', width_value = '30cm', height_value = '15cm'
where system_sku_code = 'SKUDEMO202606050005';

update product_sku
set length_value = '28cm', width_value = '20cm', height_value = '3cm'
where system_sku_code in ('SKUDEMO202606050006', 'SKUDEMO202606050007');
