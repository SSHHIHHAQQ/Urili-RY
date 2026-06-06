-- Source warehouse stock menu update.
-- Scope: point the inventory child menu to the real source warehouse stock page.
-- Data source: upstream_system_sku_inventory_snapshot maintained by upstream WMS inventory sync.

set names utf8mb4;

set @confirm_source_warehouse_stock_menu_rename := coalesce(@confirm_source_warehouse_stock_menu_rename, '');

delimiter //

drop procedure if exists assert_source_warehouse_stock_menu_rename_confirmed//
create procedure assert_source_warehouse_stock_menu_rename_confirmed()
begin
  if coalesce(@confirm_source_warehouse_stock_menu_rename, '')
      <> 'APPLY_SOURCE_WAREHOUSE_STOCK_MENU_RENAME' then
    signal sqlstate '45000' set message_text = 'set @confirm_source_warehouse_stock_menu_rename = APPLY_SOURCE_WAREHOUSE_STOCK_MENU_RENAME before running this seed';
  end if;
end//

delimiter ;

call assert_source_warehouse_stock_menu_rename_confirmed();
drop procedure if exists assert_source_warehouse_stock_menu_rename_confirmed;

update sys_menu
set menu_name = '来源仓库库存',
    path = 'source-warehouse-stock',
    component = 'Inventory/SourceWarehouseStock/index',
    route_name = 'SourceWarehouseStock',
    perms = 'inventory:sourceWarehouse:list',
    icon = 'StockOutlined',
    remark = '来源仓库库存读取上游系统SKU库存同步快照，由上游系统管理同步任务维护',
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2421
  and menu_type = 'C';
