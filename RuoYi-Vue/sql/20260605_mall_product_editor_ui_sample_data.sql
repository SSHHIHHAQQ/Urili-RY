-- Mall product editor UI follow-up migration and sample data.
-- Scope: product_spu title/detail columns and demo products for admin page review.
-- Confirm active datasource before executing.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_mall_product_editor_ui_sample_data := coalesce(@confirm_mall_product_editor_ui_sample_data, '');
set @mall_product_editor_title_backfill_expected_count :=
    coalesce(@mall_product_editor_title_backfill_expected_count, '');
set @mall_product_editor_title_backfill_expected_signature :=
    coalesce(@mall_product_editor_title_backfill_expected_signature, '');
set @mall_product_editor_demo_existing_expected_count :=
    coalesce(@mall_product_editor_demo_existing_expected_count, '');
set @mall_product_editor_demo_existing_expected_signature :=
    coalesce(@mall_product_editor_demo_existing_expected_signature, '');

delimiter //

drop procedure if exists assert_mall_product_editor_ui_sample_data_confirmed//
create procedure assert_mall_product_editor_ui_sample_data_confirmed()
begin
  if coalesce(@confirm_mall_product_editor_ui_sample_data, '')
      <> 'APPLY_MALL_PRODUCT_EDITOR_UI_SAMPLE_DATA' then
    signal sqlstate '45000' set message_text = 'set @confirm_mall_product_editor_ui_sample_data = APPLY_MALL_PRODUCT_EDITOR_UI_SAMPLE_DATA before running this migration';
  end if;
  if coalesce(@mall_product_editor_title_backfill_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @mall_product_editor_title_backfill_expected_count after previewing exact product_spu title backfill rows';
  end if;
  if coalesce(@mall_product_editor_title_backfill_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @mall_product_editor_title_backfill_expected_signature after previewing exact product_spu title backfill rows';
  end if;
  if coalesce(@mall_product_editor_demo_existing_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @mall_product_editor_demo_existing_expected_count after previewing existing SPUDEMO20260605/SKUDEMO20260605 rows';
  end if;
  if coalesce(@mall_product_editor_demo_existing_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @mall_product_editor_demo_existing_expected_signature after previewing existing SPUDEMO20260605/SKUDEMO20260605 rows';
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

drop procedure if exists assert_mall_product_editor_count_signature//
create procedure assert_mall_product_editor_count_signature(
  in p_actual_count bigint,
  in p_actual_signature varchar(64),
  in p_expected_count varchar(64),
  in p_expected_signature varchar(64),
  in p_count_message varchar(255),
  in p_signature_message varchar(255)
)
begin
  if p_actual_count <> cast(p_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = p_count_message;
  end if;
  if lower(p_actual_signature) <> lower(p_expected_signature) then
    signal sqlstate '45000' set message_text = p_signature_message;
  end if;
end//

delimiter ;

call assert_mall_product_editor_ui_sample_data_confirmed();
drop procedure if exists assert_mall_product_editor_ui_sample_data_confirmed;

call add_column_if_missing('product_spu', 'product_name_en',
  'varchar(255) not null default '''' comment ''商品英文标题'' after product_name');
call add_column_if_missing('product_spu', 'detail_content',
  'text comment ''商品详情文本'' after main_image_url');

select count(1),
       sha2(coalesce(group_concat(
         concat_ws(':', spu_id, coalesce(system_spu_code, ''), coalesce(product_name, ''), coalesce(product_name_en, ''))
         order by spu_id separator '|'
       ), ''), 256)
  into @mall_product_editor_title_backfill_actual_count,
       @mall_product_editor_title_backfill_actual_signature
from product_spu
where product_name_en = '';

call assert_mall_product_editor_count_signature(
  @mall_product_editor_title_backfill_actual_count,
  @mall_product_editor_title_backfill_actual_signature,
  @mall_product_editor_title_backfill_expected_count,
  @mall_product_editor_title_backfill_expected_signature,
  'mall product editor title backfill exact target count mismatch',
  'mall product editor title backfill exact target signature mismatch'
);

select count(1),
       sha2(coalesce(group_concat(row_signature order by row_key separator '|'), ''), 256)
  into @mall_product_editor_demo_existing_actual_count,
       @mall_product_editor_demo_existing_actual_signature
from (
    select concat('SPU:', system_spu_code) row_key,
           concat_ws(':', 'SPU', spu_id, coalesce(system_spu_code, ''), coalesce(product_name, ''),
             coalesce(product_name_en, ''), coalesce(spu_status, '')) row_signature
    from product_spu
    where system_spu_code like 'SPUDEMO20260605%'
    union all
    select concat('SKU:', system_sku_code) row_key,
           concat_ws(':', 'SKU', sku_id, spu_id, coalesce(system_sku_code, ''), coalesce(sku_status, ''),
             coalesce(sale_price, ''), coalesce(currency_code, '')) row_signature
    from product_sku
    where system_sku_code like 'SKUDEMO20260605%'
) demo_rows;

call assert_mall_product_editor_count_signature(
  @mall_product_editor_demo_existing_actual_count,
  @mall_product_editor_demo_existing_actual_signature,
  @mall_product_editor_demo_existing_expected_count,
  @mall_product_editor_demo_existing_expected_signature,
  'mall product editor demo data exact target count mismatch',
  'mall product editor demo data exact target signature mismatch'
);

start transaction;

update product_spu
set product_name_en = product_name
where product_name_en = '';

set @demo_seller_id := 5;
set @demo_seller_no := 'S20260603202422748237';
set @demo_seller_name := '跨新科技';

insert into product_spu (
    system_spu_code, seller_spu_code, seller_id, seller_no, seller_name,
    category_id, category_code, category_name, product_name, product_name_en,
    selling_point, main_image_url, detail_content, spu_status, source_type,
    source_ref_type, source_ref_id, del_flag, create_by, create_time, update_by, update_time, remark
)
select 'SPUDEMO202606050001', 'KX-CAP-001', @demo_seller_id, @demo_seller_no, @demo_seller_name,
       600, 'accessories_hats_baseball_cap', '棒球帽', '轻量透气棒球帽', 'Lightweight Breathable Baseball Cap',
       '轻薄帽身，适合通勤、户外和日常穿搭。',
       'https://images.unsplash.com/photo-1521369909029-2afed882baee?auto=format&fit=crop&w=900&q=80',
       '采用轻量面料和可调节后扣，帽檐挺括，适合春夏户外与日常搭配。建议冷水手洗并自然晾干。',
       'ON_SALE', 'ADMIN_MANUAL', '', '', '0', 'admin', sysdate(), 'admin', sysdate(), '演示商品：棒球帽'
where not exists (select 1 from product_spu where system_spu_code = 'SPUDEMO202606050001');

set @spu_cap := (select spu_id from product_spu where system_spu_code = 'SPUDEMO202606050001' limit 1);

insert into product_sku (
    spu_id, seller_id, system_sku_code, seller_sku_code, color, size, weight, material, style, model,
    package_quantity, capacity, sku_image_url, supply_price, sale_price, currency_code, sku_status,
    sort_order, del_flag, create_by, create_time, update_by, update_time, remark
)
select @spu_cap, @demo_seller_id, 'SKUDEMO202606050001', 'KX-CAP-BLK-OS', '黑色', '均码', '80g', '聚酯纤维', '运动休闲', 'CAP-LITE',
       '1顶', '', 'https://images.unsplash.com/photo-1521369909029-2afed882baee?auto=format&fit=crop&w=500&q=80',
       18.5000, 39.9000, 'CNY', 'ON_SALE', 1, '0', 'admin', sysdate(), 'admin', sysdate(), '演示SKU'
where not exists (select 1 from product_sku where system_sku_code = 'SKUDEMO202606050001');

insert into product_sku (
    spu_id, seller_id, system_sku_code, seller_sku_code, color, size, weight, material, style, model,
    package_quantity, capacity, sku_image_url, supply_price, sale_price, currency_code, sku_status,
    sort_order, del_flag, create_by, create_time, update_by, update_time, remark
)
select @spu_cap, @demo_seller_id, 'SKUDEMO202606050002', 'KX-CAP-KHK-OS', '卡其色', '均码', '80g', '聚酯纤维', '运动休闲', 'CAP-LITE',
       '1顶', '', 'https://images.unsplash.com/photo-1521369909029-2afed882baee?auto=format&fit=crop&w=500&q=80',
       18.5000, 39.9000, 'CNY', 'READY', 2, '0', 'admin', sysdate(), 'admin', sysdate(), '演示SKU'
where not exists (select 1 from product_sku where system_sku_code = 'SKUDEMO202606050002');

insert into product_spu (
    system_spu_code, seller_spu_code, seller_id, seller_no, seller_name,
    category_id, category_code, category_name, product_name, product_name_en,
    selling_point, main_image_url, detail_content, spu_status, source_type,
    source_ref_type, source_ref_id, del_flag, create_by, create_time, update_by, update_time, remark
)
select 'SPUDEMO202606050002', 'KX-SHOE-001', @demo_seller_id, @demo_seller_no, @demo_seller_name,
       565, 'shoes_sports_shoes_running_shoes', '跑步鞋', '城市缓震跑步鞋', 'Urban Cushion Running Shoes',
       '轻量缓震中底，适合日常慢跑和通勤步行。',
       'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80',
       '鞋面采用透气网布，后跟稳定包裹，中底提供日常跑步所需的缓震支撑。建议按平时运动鞋尺码选择。',
       'ON_SALE', 'ADMIN_MANUAL', '', '', '0', 'admin', sysdate(), 'admin', sysdate(), '演示商品：跑步鞋'
where not exists (select 1 from product_spu where system_spu_code = 'SPUDEMO202606050002');

set @spu_shoe := (select spu_id from product_spu where system_spu_code = 'SPUDEMO202606050002' limit 1);

insert into product_sku (
    spu_id, seller_id, system_sku_code, seller_sku_code, color, size, weight, material, style, model,
    package_quantity, capacity, sku_image_url, supply_price, sale_price, currency_code, sku_status,
    sort_order, del_flag, create_by, create_time, update_by, update_time, remark
)
select @spu_shoe, @demo_seller_id, 'SKUDEMO202606050003', 'KX-SHOE-RED-42', '红色', '42', '680g/双', '网布+橡胶', '运动', 'RUN-CITY',
       '1双', '', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=500&q=80',
       138.0000, 299.0000, 'CNY', 'ON_SALE', 1, '0', 'admin', sysdate(), 'admin', sysdate(), '演示SKU'
where not exists (select 1 from product_sku where system_sku_code = 'SKUDEMO202606050003');

insert into product_sku (
    spu_id, seller_id, system_sku_code, seller_sku_code, color, size, weight, material, style, model,
    package_quantity, capacity, sku_image_url, supply_price, sale_price, currency_code, sku_status,
    sort_order, del_flag, create_by, create_time, update_by, update_time, remark
)
select @spu_shoe, @demo_seller_id, 'SKUDEMO202606050004', 'KX-SHOE-RED-43', '红色', '43', '700g/双', '网布+橡胶', '运动', 'RUN-CITY',
       '1双', '', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=500&q=80',
       138.0000, 299.0000, 'CNY', 'READY', 2, '0', 'admin', sysdate(), 'admin', sysdate(), '演示SKU'
where not exists (select 1 from product_sku where system_sku_code = 'SKUDEMO202606050004');

insert into product_spu (
    system_spu_code, seller_spu_code, seller_id, seller_no, seller_name,
    category_id, category_code, category_name, product_name, product_name_en,
    selling_point, main_image_url, detail_content, spu_status, source_type,
    source_ref_type, source_ref_id, del_flag, create_by, create_time, update_by, update_time, remark
)
select 'SPUDEMO202606050003', 'KX-BAG-001', @demo_seller_id, @demo_seller_no, @demo_seller_name,
       588, 'bags_luggage_backpacks_commuter_backpack', '通勤双肩包', '防泼水通勤双肩包', 'Water-Resistant Commuter Backpack',
       '分区收纳，适合电脑、文件和短途出行。',
       'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=900&q=80',
       '主仓可容纳 15.6 英寸笔记本，前袋适合收纳证件、充电器和随身小物。面料具备日常防泼水能力。',
       'READY', 'ADMIN_MANUAL', '', '', '0', 'admin', sysdate(), 'admin', sysdate(), '演示商品：通勤双肩包'
where not exists (select 1 from product_spu where system_spu_code = 'SPUDEMO202606050003');

set @spu_bag := (select spu_id from product_spu where system_spu_code = 'SPUDEMO202606050003' limit 1);

insert into product_sku (
    spu_id, seller_id, system_sku_code, seller_sku_code, color, size, weight, material, style, model,
    package_quantity, capacity, sku_image_url, supply_price, sale_price, currency_code, sku_status,
    sort_order, del_flag, create_by, create_time, update_by, update_time, remark
)
select @spu_bag, @demo_seller_id, 'SKUDEMO202606050005', 'KX-BAG-BLK-20L', '黑色', '20L', '760g', '尼龙', '通勤', 'BAG-COM',
       '1个', '20L', 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=500&q=80',
       88.0000, 179.0000, 'CNY', 'READY', 1, '0', 'admin', sysdate(), 'admin', sysdate(), '演示SKU'
where not exists (select 1 from product_sku where system_sku_code = 'SKUDEMO202606050005');

insert into product_spu (
    system_spu_code, seller_spu_code, seller_id, seller_no, seller_name,
    category_id, category_code, category_name, product_name, product_name_en,
    selling_point, main_image_url, detail_content, spu_status, source_type,
    source_ref_type, source_ref_id, del_flag, create_by, create_time, update_by, update_time, remark
)
select 'SPUDEMO202606050004', 'KX-TEE-001', @demo_seller_id, @demo_seller_no, @demo_seller_name,
       446, 'women_clothing_tops_tshirt', 'T恤', '纯棉宽松短袖 T 恤', 'Cotton Relaxed Fit Short Sleeve T-Shirt',
       '柔软纯棉面料，基础版型，适合多季节内搭或单穿。',
       'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80',
       '采用亲肤纯棉面料，肩线自然，适合日常休闲穿搭。建议反面洗涤，避免长时间暴晒。',
       'ON_SALE', 'ADMIN_MANUAL', '', '', '0', 'admin', sysdate(), 'admin', sysdate(), '演示商品：T恤'
where not exists (select 1 from product_spu where system_spu_code = 'SPUDEMO202606050004');

set @spu_tee := (select spu_id from product_spu where system_spu_code = 'SPUDEMO202606050004' limit 1);

insert into product_sku (
    spu_id, seller_id, system_sku_code, seller_sku_code, color, size, weight, material, style, model,
    package_quantity, capacity, sku_image_url, supply_price, sale_price, currency_code, sku_status,
    sort_order, del_flag, create_by, create_time, update_by, update_time, remark
)
select @spu_tee, @demo_seller_id, 'SKUDEMO202606050006', 'KX-TEE-WHT-M', '白色', 'M', '180g', '纯棉', '基础款', 'TEE-COTTON',
       '1件', '', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=500&q=80',
       35.0000, 79.0000, 'CNY', 'ON_SALE', 1, '0', 'admin', sysdate(), 'admin', sysdate(), '演示SKU'
where not exists (select 1 from product_sku where system_sku_code = 'SKUDEMO202606050006');

insert into product_sku (
    spu_id, seller_id, system_sku_code, seller_sku_code, color, size, weight, material, style, model,
    package_quantity, capacity, sku_image_url, supply_price, sale_price, currency_code, sku_status,
    sort_order, del_flag, create_by, create_time, update_by, update_time, remark
)
select @spu_tee, @demo_seller_id, 'SKUDEMO202606050007', 'KX-TEE-BLK-L', '黑色', 'L', '190g', '纯棉', '基础款', 'TEE-COTTON',
       '1件', '', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=500&q=80',
       35.0000, 79.0000, 'CNY', 'ON_SALE', 2, '0', 'admin', sysdate(), 'admin', sysdate(), '演示SKU'
where not exists (select 1 from product_sku where system_sku_code = 'SKUDEMO202606050007');

insert into product_attribute_value (
    owner_type, owner_id, spu_id, category_id, category_schema_version,
    attribute_id, attribute_code, attribute_name, attribute_type, value_code,
    value_text, value_number, value_date, value_json, create_by, create_time, update_by, update_time, remark
)
select 'SPU', @spu_tee, @spu_tee, 446, 2,
       378, 'clothing_length', '衣长类型', 'SINGLE_SELECT', 'REGULAR',
       null, null, null, null, 'admin', sysdate(), 'admin', sysdate(), '演示商品类目属性'
where not exists (
    select 1 from product_attribute_value where owner_type = 'SPU' and owner_id = @spu_tee and attribute_id = 378
);

insert into product_image (owner_type, owner_id, spu_id, sku_id, image_url, image_role, sort_order, create_by, create_time)
select 'SPU', p.spu_id, p.spu_id, null, p.main_image_url, 'MAIN', 0, 'admin', sysdate()
from product_spu p
where p.system_spu_code like 'SPUDEMO20260605%'
  and not exists (select 1 from product_image i where i.owner_type='SPU' and i.owner_id=p.spu_id and i.image_role='MAIN');

insert into product_image (owner_type, owner_id, spu_id, sku_id, image_url, image_role, sort_order, create_by, create_time)
select 'SPU', p.spu_id, p.spu_id, null, p.main_image_url, 'GALLERY', 1, 'admin', sysdate()
from product_spu p
where p.system_spu_code like 'SPUDEMO20260605%'
  and not exists (select 1 from product_image i where i.owner_type='SPU' and i.owner_id=p.spu_id and i.image_role='GALLERY');

insert into product_image (owner_type, owner_id, spu_id, sku_id, image_url, image_role, sort_order, create_by, create_time)
select 'SPU', p.spu_id, p.spu_id, null, p.main_image_url, 'DETAIL', 1, 'admin', sysdate()
from product_spu p
where p.system_spu_code like 'SPUDEMO20260605%'
  and not exists (select 1 from product_image i where i.owner_type='SPU' and i.owner_id=p.spu_id and i.image_role='DETAIL');

insert into product_image (owner_type, owner_id, spu_id, sku_id, image_url, image_role, sort_order, create_by, create_time)
select 'SKU', s.sku_id, s.spu_id, s.sku_id, s.sku_image_url, 'SKU_MAIN', s.sort_order, 'admin', sysdate()
from product_sku s
where s.system_sku_code like 'SKUDEMO20260605%'
  and not exists (select 1 from product_image i where i.owner_type='SKU' and i.owner_id=s.sku_id and i.image_role='SKU_MAIN');

commit;

drop procedure if exists add_column_if_missing;
drop procedure if exists assert_mall_product_editor_count_signature;
