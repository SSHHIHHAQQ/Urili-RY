-- Mall product editor UI follow-up migration and sample data.
-- Scope: product_spu title/detail columns and demo products for admin page review.
-- Confirm active datasource before executing.

set names utf8mb4;

set @confirm_mall_product_editor_ui_sample_data := coalesce(@confirm_mall_product_editor_ui_sample_data, '');

delimiter //

drop procedure if exists assert_mall_product_editor_ui_sample_data_confirmed//
create procedure assert_mall_product_editor_ui_sample_data_confirmed()
begin
  if coalesce(@confirm_mall_product_editor_ui_sample_data, '')
      <> 'APPLY_MALL_PRODUCT_EDITOR_UI_SAMPLE_DATA' then
    signal sqlstate '45000' set message_text = 'set @confirm_mall_product_editor_ui_sample_data = APPLY_MALL_PRODUCT_EDITOR_UI_SAMPLE_DATA before running this migration';
  end if;
end//

delimiter ;

call assert_mall_product_editor_ui_sample_data_confirmed();
drop procedure if exists assert_mall_product_editor_ui_sample_data_confirmed;

alter table product_spu
  add column product_name_en varchar(255) not null default '' comment '商品英文标题' after product_name;

alter table product_spu
  add column detail_content text comment '商品详情文本' after main_image_url;

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
