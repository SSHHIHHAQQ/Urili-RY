-- 库存总览 SKU 基线读模型刷新
-- Scope: 以商城 SKU 作为库存总览基础行，来源/仓库关系只影响库存状态和可售计算。

set names utf8mb4;

set @confirm_inventory_overview_sku_baseline_refresh := coalesce(@confirm_inventory_overview_sku_baseline_refresh, '');

delimiter //

drop procedure if exists assert_inventory_overview_sku_baseline_refresh_confirmed//
create procedure assert_inventory_overview_sku_baseline_refresh_confirmed()
begin
  if coalesce(@confirm_inventory_overview_sku_baseline_refresh, '')
      <> 'APPLY_INVENTORY_OVERVIEW_SKU_BASELINE_REFRESH' then
    signal sqlstate '45000' set message_text = 'set @confirm_inventory_overview_sku_baseline_refresh = APPLY_INVENTORY_OVERVIEW_SKU_BASELINE_REFRESH before running this migration';
  end if;
end//

drop procedure if exists assert_inventory_overview_table_exists//
create procedure assert_inventory_overview_table_exists(in p_table_name varchar(128), in p_message varchar(255))
begin
  if not exists (
    select 1
    from information_schema.tables
    where table_schema = database()
      and table_name = p_table_name
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

delimiter ;

call assert_inventory_overview_sku_baseline_refresh_confirmed();
call assert_inventory_overview_table_exists('product_spu', 'product_spu is required before inventory overview sku baseline refresh');
call assert_inventory_overview_table_exists('product_sku', 'product_sku is required before inventory overview sku baseline refresh');
call assert_inventory_overview_table_exists('product_spu_warehouse', 'product_spu_warehouse is required before inventory overview sku baseline refresh');
call assert_inventory_overview_table_exists('product_sku_source_binding', 'product_sku_source_binding is required before inventory overview sku baseline refresh');
call assert_inventory_overview_table_exists('source_warehouse_stock_detail', 'source_warehouse_stock_detail is required before inventory overview sku baseline refresh');
call assert_inventory_overview_table_exists('inventory_sku_warehouse_stock', 'inventory_sku_warehouse_stock is required before inventory overview sku baseline refresh');
call assert_inventory_overview_table_exists('inventory_overview_sku_read_model', 'inventory_overview_sku_read_model is required before inventory overview sku baseline refresh');
call assert_inventory_overview_table_exists('inventory_overview_spu_read_model', 'inventory_overview_spu_read_model is required before inventory overview sku baseline refresh');

drop procedure if exists assert_inventory_overview_sku_baseline_refresh_confirmed;
drop procedure if exists assert_inventory_overview_table_exists;

update sys_dict_data
set dict_label = '无来源库存',
    list_class = 'warning',
    update_by = 'admin',
    update_time = sysdate()
where dict_type = 'inventory_status'
  and dict_value = 'NO_SOURCE';

insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'inventory_status', '', seed.list_class, 'N', '0', 'admin', sysdate(), '库存总览库存状态'
from (
    select 6 dict_sort, '仓库未配置' dict_label, 'NO_WAREHOUSE' dict_value, 'error' list_class
    union all select 7, '来源SKU未绑定', 'SOURCE_UNBOUND', 'warning'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'inventory_status' and d.dict_value = seed.dict_value
);

insert into inventory_sku_warehouse_stock(
    stock_key, spu_id, sku_id, seller_id, system_sku_code, warehouse_kind, warehouse_ref_type,
    warehouse_id, warehouse_code, warehouse_name, source_scope, source_master_warehouse_name,
    source_inventory_scope, source_inventory_attribute, source_total_qty, source_available_qty,
    source_in_transit_qty, source_snapshot_time, platform_total_qty, platform_reserved_qty,
    platform_in_transit_qty, pending_available_inbound_qty, pending_source_deduction_qty,
    platform_available_qty, effective_status, calc_time, create_by, create_time, update_by, update_time, remark
)
select concat('INV:', sha2(concat_ws('|', 'SKU', sk.sku_id, 'OFFICIAL_MASTER', src.master_warehouse_name), 256)),
       sk.spu_id,
       sk.sku_id,
       sk.seller_id,
       sk.system_sku_code,
       'official',
       'OFFICIAL_MASTER',
       null,
       '',
       src.master_warehouse_name,
       'OFFICIAL_MASTER',
       src.master_warehouse_name,
       'COMPREHENSIVE',
       '0',
       src.source_total_qty,
       src.source_available_qty,
       src.source_in_transit_qty,
       src.source_snapshot_time,
       0, 0, 0, 0, 0, 0,
       case
         when src.source_available_qty > 0 then 'OUT_OF_STOCK'
         when src.source_in_transit_qty > 0 then 'SOURCE_ONLY_IN_TRANSIT'
         else 'NO_SOURCE'
       end,
       sysdate(), 'admin', sysdate(), 'admin', sysdate(), '来源库存刷新生成'
from product_sku sk
join product_spu p on p.spu_id = sk.spu_id and p.del_flag = '0'
join product_sku_source_binding b
  on b.sku_id = sk.sku_id
 and b.binding_status = 'ACTIVE'
join (
    select d.source_stock_group_key,
           d.master_warehouse_name,
           sum(coalesce(d.total_quantity, 0)) source_total_qty,
           sum(coalesce(d.available_quantity, 0)) source_available_qty,
           sum(coalesce(d.in_transit_quantity, 0)) source_in_transit_qty,
           max(d.update_time) source_snapshot_time
    from source_warehouse_stock_detail d
    where d.repository_scope = 'OFFICIAL_MASTER'
      and d.inventory_scope = 'COMPREHENSIVE'
      and d.inventory_attribute = '0'
      and nullif(d.master_warehouse_name, '') is not null
    group by d.source_stock_group_key, d.master_warehouse_name
) src on src.source_stock_group_key = b.source_sku_group_key
where sk.del_flag = '0'
on duplicate key update
    source_total_qty = values(source_total_qty),
    source_available_qty = values(source_available_qty),
    source_in_transit_qty = values(source_in_transit_qty),
    source_snapshot_time = values(source_snapshot_time),
    warehouse_kind = 'official',
    warehouse_ref_type = 'OFFICIAL_MASTER',
    warehouse_name = values(warehouse_name),
    source_scope = 'OFFICIAL_MASTER',
    source_master_warehouse_name = values(source_master_warehouse_name),
    platform_available_qty = greatest(0,
      least(platform_total_qty, greatest(0, values(source_available_qty) - pending_source_deduction_qty))
      - platform_reserved_qty),
    effective_status = case
      when greatest(0,
        least(platform_total_qty, greatest(0, values(source_available_qty) - pending_source_deduction_qty))
        - platform_reserved_qty) > 0 then 'IN_STOCK'
      when values(source_available_qty) <= 0 and values(source_in_transit_qty) > 0 then 'SOURCE_ONLY_IN_TRANSIT'
      when values(source_available_qty) <= 0 and values(source_in_transit_qty) <= 0 then 'NO_SOURCE'
      else 'OUT_OF_STOCK'
    end,
    calc_time = sysdate(),
    update_by = 'admin',
    update_time = sysdate();

insert into inventory_sku_warehouse_stock(
    stock_key, spu_id, sku_id, seller_id, system_sku_code, warehouse_kind, warehouse_ref_type,
    warehouse_id, warehouse_code, warehouse_name, source_scope, source_master_warehouse_name,
    source_inventory_scope, source_inventory_attribute, source_total_qty, source_available_qty,
    source_in_transit_qty, source_snapshot_time, platform_total_qty, platform_reserved_qty,
    platform_in_transit_qty, pending_available_inbound_qty, pending_source_deduction_qty,
    platform_available_qty, effective_status, calc_time, create_by, create_time, update_by, update_time, remark
)
select concat('INV:', sha2(concat_ws('|', 'SKU', sk.sku_id, 'SOURCE_UNBOUND'), 256)),
       sk.spu_id,
       sk.sku_id,
       sk.seller_id,
       sk.system_sku_code,
       'official',
       'SOURCE_UNBOUND',
       null,
       '',
       '官方仓（来源SKU未绑定）',
       'OFFICIAL_MASTER',
       '',
       'COMPREHENSIVE',
       '0',
       0, 0, 0, null, 0, 0, 0, 0, 0, 0, 'SOURCE_UNBOUND',
       sysdate(), 'admin', sysdate(), 'admin', sysdate(), '官方仓来源SKU未绑定占位行'
from product_sku sk
join product_spu p on p.spu_id = sk.spu_id and p.del_flag = '0'
where sk.del_flag = '0'
  and exists (
    select 1
    from product_spu_warehouse pw
    where pw.spu_id = sk.spu_id
      and pw.warehouse_kind = 'official'
  )
  and not exists (
    select 1
    from product_sku_source_binding b
    where b.sku_id = sk.sku_id
      and b.binding_status = 'ACTIVE'
  )
on duplicate key update
    spu_id = values(spu_id),
    seller_id = values(seller_id),
    system_sku_code = values(system_sku_code),
    warehouse_kind = 'official',
    warehouse_ref_type = 'SOURCE_UNBOUND',
    platform_available_qty = 0,
    effective_status = 'SOURCE_UNBOUND',
    calc_time = sysdate(),
    update_by = 'admin',
    update_time = sysdate();

insert into inventory_sku_warehouse_stock(
    stock_key, spu_id, sku_id, seller_id, system_sku_code, warehouse_kind, warehouse_ref_type,
    warehouse_id, warehouse_code, warehouse_name, source_scope, source_master_warehouse_name,
    source_inventory_scope, source_inventory_attribute, source_total_qty, source_available_qty,
    source_in_transit_qty, source_snapshot_time, platform_total_qty, platform_reserved_qty,
    platform_in_transit_qty, pending_available_inbound_qty, pending_source_deduction_qty,
    platform_available_qty, effective_status, calc_time, create_by, create_time, update_by, update_time, remark
)
select concat('INV:', sha2(concat_ws('|', 'SKU', sk.sku_id, 'UNMATCHED_OFFICIAL'), 256)),
       sk.spu_id,
       sk.sku_id,
       sk.seller_id,
       sk.system_sku_code,
       'official',
       'UNMATCHED_OFFICIAL',
       null,
       '',
       '官方仓（未匹配来源库存）',
       'OFFICIAL_MASTER',
       '',
       'COMPREHENSIVE',
       '0',
       0, 0, 0, null, 0, 0, 0, 0, 0, 0, 'NO_SOURCE',
       sysdate(), 'admin', sysdate(), 'admin', sysdate(), '官方仓无来源库存占位行'
from product_sku sk
join product_spu p on p.spu_id = sk.spu_id and p.del_flag = '0'
where sk.del_flag = '0'
  and exists (
    select 1
    from product_sku_source_binding b
    where b.sku_id = sk.sku_id
      and b.binding_status = 'ACTIVE'
  )
  and not exists (
    select 1
    from product_sku_source_binding b
    join source_warehouse_stock_detail d
      on d.source_stock_group_key = b.source_sku_group_key
     and d.repository_scope = 'OFFICIAL_MASTER'
     and d.inventory_scope = 'COMPREHENSIVE'
     and d.inventory_attribute = '0'
     and nullif(d.master_warehouse_name, '') is not null
    where b.sku_id = sk.sku_id
      and b.binding_status = 'ACTIVE'
  )
on duplicate key update
    spu_id = values(spu_id),
    seller_id = values(seller_id),
    system_sku_code = values(system_sku_code),
    warehouse_kind = 'official',
    warehouse_ref_type = 'UNMATCHED_OFFICIAL',
    platform_available_qty = 0,
    effective_status = 'NO_SOURCE',
    calc_time = sysdate(),
    update_by = 'admin',
    update_time = sysdate();

insert into inventory_sku_warehouse_stock(
    stock_key, spu_id, sku_id, seller_id, system_sku_code, warehouse_kind, warehouse_ref_type,
    warehouse_id, warehouse_code, warehouse_name, source_scope, source_master_warehouse_name,
    source_inventory_scope, source_inventory_attribute, source_total_qty, source_available_qty,
    source_in_transit_qty, source_snapshot_time, platform_total_qty, platform_reserved_qty,
    platform_in_transit_qty, pending_available_inbound_qty, pending_source_deduction_qty,
    platform_available_qty, effective_status, calc_time, create_by, create_time, update_by, update_time, remark
)
select concat('INV:', sha2(concat_ws('|', 'SKU', sk.sku_id, 'THIRD_PARTY_WAREHOUSE', pw.warehouse_id), 256)),
       sk.spu_id,
       sk.sku_id,
       sk.seller_id,
       sk.system_sku_code,
       'third_party',
       'THIRD_PARTY_WAREHOUSE',
       pw.warehouse_id,
       pw.warehouse_code,
       pw.warehouse_name,
       '',
       '',
       '',
       '',
       0, 0, 0, null, 0, 0, 0, 0, 0, 0, 'OUT_OF_STOCK',
       sysdate(), 'admin', sysdate(), 'admin', sysdate(), '三方仓库存刷新生成'
from product_sku sk
join product_spu p on p.spu_id = sk.spu_id and p.del_flag = '0'
join product_spu_warehouse pw
  on pw.spu_id = sk.spu_id
 and pw.warehouse_kind = 'third_party'
where sk.del_flag = '0'
on duplicate key update
    spu_id = values(spu_id),
    seller_id = values(seller_id),
    system_sku_code = values(system_sku_code),
    warehouse_kind = 'third_party',
    warehouse_ref_type = 'THIRD_PARTY_WAREHOUSE',
    warehouse_id = values(warehouse_id),
    warehouse_code = values(warehouse_code),
    warehouse_name = values(warehouse_name),
    platform_available_qty = greatest(0, platform_total_qty - platform_reserved_qty),
    effective_status = case
      when greatest(0, platform_total_qty - platform_reserved_qty) > 0 then 'IN_STOCK'
      else 'OUT_OF_STOCK'
    end,
    calc_time = sysdate(),
    update_by = 'admin',
    update_time = sysdate();

insert into inventory_sku_warehouse_stock(
    stock_key, spu_id, sku_id, seller_id, system_sku_code, warehouse_kind, warehouse_ref_type,
    warehouse_id, warehouse_code, warehouse_name, source_scope, source_master_warehouse_name,
    source_inventory_scope, source_inventory_attribute, source_total_qty, source_available_qty,
    source_in_transit_qty, source_snapshot_time, platform_total_qty, platform_reserved_qty,
    platform_in_transit_qty, pending_available_inbound_qty, pending_source_deduction_qty,
    platform_available_qty, effective_status, calc_time, create_by, create_time, update_by, update_time, remark
)
select concat('INV:', sha2(concat_ws('|', 'SKU', sk.sku_id, 'NO_WAREHOUSE'), 256)),
       sk.spu_id,
       sk.sku_id,
       sk.seller_id,
       sk.system_sku_code,
       'unconfigured',
       'NO_WAREHOUSE',
       null,
       '',
       '发货仓库未配置',
       '',
       '',
       '',
       '',
       0, 0, 0, null, 0, 0, 0, 0, 0, 0, 'NO_WAREHOUSE',
       sysdate(), 'admin', sysdate(), 'admin', sysdate(), '商城SKU缺少仓库/来源绑定占位行'
from product_sku sk
join product_spu p on p.spu_id = sk.spu_id and p.del_flag = '0'
where sk.del_flag = '0'
  and not exists (
    select 1
    from product_spu_warehouse pw
    where pw.spu_id = sk.spu_id
  )
  and not exists (
    select 1
    from product_sku_source_binding b
    where b.sku_id = sk.sku_id
      and b.binding_status = 'ACTIVE'
  )
on duplicate key update
    spu_id = values(spu_id),
    seller_id = values(seller_id),
    system_sku_code = values(system_sku_code),
    warehouse_kind = 'unconfigured',
    warehouse_ref_type = 'NO_WAREHOUSE',
    platform_available_qty = 0,
    effective_status = 'NO_WAREHOUSE',
    calc_time = sysdate(),
    update_by = 'admin',
    update_time = sysdate();

start transaction;

delete from inventory_overview_sku_read_model;
delete from inventory_overview_spu_read_model;

insert into inventory_overview_sku_read_model(
    sku_stock_key, spu_id, sku_id, seller_id, system_sku_code, product_name, sku_name, sku_image_url,
    warehouse_kind_summary, warehouse_count, platform_total_qty, platform_available_qty,
    platform_reserved_qty, platform_in_transit_qty, source_total_qty, source_available_qty,
    source_in_transit_qty, inventory_status, latest_source_snapshot_time, latest_stock_update_time,
    search_text, rebuild_time
)
select concat('SKU:', sk.sku_id),
       sk.spu_id,
       sk.sku_id,
       sk.seller_id,
       sk.system_sku_code,
       p.product_name,
       trim(concat_ws(' / ', nullif(sk.color, ''), nullif(sk.size, ''), nullif(sk.model, ''))),
       coalesce(sk.sku_image_url, ''),
       case
         when count(st.stock_id) = 0 then 'unconfigured'
         when count(distinct st.warehouse_kind) = 1 then min(st.warehouse_kind)
         else 'MIXED'
       end,
       count(st.stock_id),
       sum(coalesce(st.platform_total_qty, 0)),
       sum(coalesce(st.platform_available_qty, 0)),
       sum(coalesce(st.platform_reserved_qty, 0)),
       sum(coalesce(st.platform_in_transit_qty, 0)),
       sum(case when st.warehouse_kind = 'official' then coalesce(st.source_total_qty, 0) else 0 end),
       sum(case when st.warehouse_kind = 'official' then coalesce(st.source_available_qty, 0) else 0 end),
       sum(case when st.warehouse_kind = 'official' then coalesce(st.source_in_transit_qty, 0) else 0 end),
       case
         when sum(coalesce(st.platform_available_qty, 0)) > 0 then 'IN_STOCK'
         when count(st.stock_id) = 0 then 'NO_WAREHOUSE'
         when sum(case when st.effective_status = 'NO_WAREHOUSE' then 1 else 0 end) > 0 then 'NO_WAREHOUSE'
         when sum(case when st.effective_status = 'SOURCE_UNBOUND' then 1 else 0 end) > 0 then 'SOURCE_UNBOUND'
         when sum(case when st.effective_status = 'SOURCE_ONLY_IN_TRANSIT' then 1 else 0 end) > 0 then 'SOURCE_ONLY_IN_TRANSIT'
         when sum(case when st.effective_status = 'NO_SOURCE' then 1 else 0 end) > 0 then 'NO_SOURCE'
         else 'OUT_OF_STOCK'
       end,
       max(st.source_snapshot_time),
       max(st.update_time),
       concat_ws(' ', p.system_spu_code, p.product_name, sk.system_sku_code, sk.seller_sku_code, sk.color, sk.size, sk.model),
       sysdate()
from product_sku sk
join product_spu p on p.spu_id = sk.spu_id and p.del_flag = '0'
left join inventory_sku_warehouse_stock st on st.sku_id = sk.sku_id
where sk.del_flag = '0'
group by sk.sku_id, sk.spu_id, sk.seller_id, sk.system_sku_code, p.product_name, sk.sku_image_url,
         sk.color, sk.size, sk.model, p.system_spu_code, sk.seller_sku_code;

insert into inventory_overview_spu_read_model(
    spu_stock_key, spu_id, seller_id, system_spu_code, product_name, main_image_url, sku_count,
    warehouse_kind_summary, warehouse_count, platform_total_qty, platform_available_qty,
    platform_reserved_qty, platform_in_transit_qty, source_total_qty, source_available_qty,
    source_in_transit_qty, inventory_status, latest_source_snapshot_time, latest_stock_update_time,
    search_text, rebuild_time
)
select concat('SPU:', p.spu_id),
       p.spu_id,
       p.seller_id,
       p.system_spu_code,
       p.product_name,
       coalesce(p.main_image_url, ''),
       count(distinct sk.sku_id),
       case when count(distinct srm.warehouse_kind_summary) = 1 then min(srm.warehouse_kind_summary) else 'MIXED' end,
       sum(coalesce(srm.warehouse_count, 0)),
       sum(coalesce(srm.platform_total_qty, 0)),
       sum(coalesce(srm.platform_available_qty, 0)),
       sum(coalesce(srm.platform_reserved_qty, 0)),
       sum(coalesce(srm.platform_in_transit_qty, 0)),
       sum(coalesce(srm.source_total_qty, 0)),
       sum(coalesce(srm.source_available_qty, 0)),
       sum(coalesce(srm.source_in_transit_qty, 0)),
       case
         when sum(coalesce(srm.platform_available_qty, 0)) > 0 then 'IN_STOCK'
         when sum(case when srm.inventory_status = 'NO_WAREHOUSE' then 1 else 0 end) > 0 then 'NO_WAREHOUSE'
         when sum(case when srm.inventory_status = 'SOURCE_UNBOUND' then 1 else 0 end) > 0 then 'SOURCE_UNBOUND'
         when sum(case when srm.inventory_status = 'SOURCE_ONLY_IN_TRANSIT' then 1 else 0 end) > 0 then 'SOURCE_ONLY_IN_TRANSIT'
         when sum(case when srm.inventory_status = 'NO_SOURCE' then 1 else 0 end) > 0 then 'NO_SOURCE'
         else 'OUT_OF_STOCK'
       end,
       max(srm.latest_source_snapshot_time),
       max(srm.latest_stock_update_time),
       concat_ws(' ', p.system_spu_code, p.seller_spu_code, p.product_name, p.product_name_en, p.seller_name),
       sysdate()
from product_spu p
join product_sku sk on sk.spu_id = p.spu_id and sk.del_flag = '0'
join inventory_overview_sku_read_model srm on srm.sku_id = sk.sku_id
where p.del_flag = '0'
group by p.spu_id, p.seller_id, p.system_spu_code, p.product_name, p.main_image_url,
         p.seller_spu_code, p.product_name_en, p.seller_name;

commit;
