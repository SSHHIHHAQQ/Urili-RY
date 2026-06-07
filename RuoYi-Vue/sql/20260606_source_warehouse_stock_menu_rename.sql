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

drop procedure if exists assert_source_warehouse_stock_sys_menu_guard//
create procedure assert_source_warehouse_stock_sys_menu_guard()
begin
  if exists (
    select 1
    from sys_menu m
    where exists (
        select 1
        from tmp_source_warehouse_stock_sys_menu_guard seed
        where seed.menu_id = m.menu_id
    )
      and not exists (
        select 1
        from tmp_source_warehouse_stock_sys_menu_guard seed
        where seed.menu_id = m.menu_id
          and m.parent_id = seed.parent_id
          and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')
          and coalesce(m.path, '') = coalesce(seed.path, '')
          and coalesce(m.component, '') = coalesce(seed.component, '')
          and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
          and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'source warehouse stock sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_source_warehouse_stock_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'source warehouse stock sys_menu signature is already used by another menu';
  end if;
end//

delimiter ;

call assert_source_warehouse_stock_menu_rename_confirmed();
drop procedure if exists assert_source_warehouse_stock_menu_rename_confirmed;

create temporary table if not exists tmp_source_warehouse_stock_sys_menu_guard (
  menu_id    bigint       not null,
  parent_id  bigint       not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  key idx_source_warehouse_stock_sys_menu_guard_id (menu_id)
) engine=memory;

truncate table tmp_source_warehouse_stock_sys_menu_guard;

insert into tmp_source_warehouse_stock_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (2421, 2080, 'C', 'source-warehouse-stock', 'Inventory/SourceWarehouseStock/index', 'SourceWarehouseStock', 'inventory:sourceWarehouse:list'),
    (2421, 2080, 'C', 'source-warehouse-stock', 'Common/PlannedPage/index', 'SourceWarehouseStock', 'inventory:sourceWarehouse:list');

call assert_source_warehouse_stock_sys_menu_guard();

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

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select 2421, '来源仓库库存', 2080, 10, 'source-warehouse-stock', 'Inventory/SourceWarehouseStock/index', '', 'SourceWarehouseStock',
       1, 0, 'C', '0', '0', 'inventory:sourceWarehouse:list', 'StockOutlined', 'admin',
       sysdate(), '', null, '来源仓库库存读取上游系统SKU库存同步快照，由上游系统管理同步任务维护'
where not exists (select 1 from sys_menu where menu_id = 2421);

drop temporary table if exists tmp_source_warehouse_stock_sys_menu_guard;
drop procedure if exists assert_source_warehouse_stock_sys_menu_guard;
