-- Business menu seed for the RuoYi validation project.
-- Scope: second-level business menu entries only. No business table, API, or button permission is created here.
-- Run after top_menu_seed.sql so parent directories are present.

set names utf8mb4;

set @confirm_business_menu_seed := coalesce(@confirm_business_menu_seed, '');

delimiter //

drop procedure if exists assert_business_menu_seed_confirmed//
create procedure assert_business_menu_seed_confirmed()
begin
  if coalesce(@confirm_business_menu_seed, '')
      <> 'APPLY_BUSINESS_MENU_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_business_menu_seed = APPLY_BUSINESS_MENU_SEED before running this seed';
  end if;
end//

drop procedure if exists assert_business_menu_sys_menu_guard//
create procedure assert_business_menu_sys_menu_guard()
begin
  declare v_parent_count int default 0;

  select count(1)
    into v_parent_count
  from sys_menu
  where (
      menu_id = 2050
      and menu_name = '财务管理'
      and parent_id = 0
      and path = 'finance'
      and route_name = 'FinanceManagement'
      and menu_type = 'M'
    )
    or (
      menu_id = 2060
      and menu_name = '商品管理'
      and parent_id = 0
      and path = 'product'
      and route_name = 'ProductManagement'
      and menu_type = 'M'
    )
    or (
      menu_id = 2070
      and menu_name = '订单管理'
      and parent_id = 0
      and path = 'order'
      and route_name = 'OrderManagement'
      and menu_type = 'M'
    )
    or (
      menu_id = 2080
      and menu_name = '库存管理'
      and parent_id = 0
      and path = 'inventory'
      and route_name = 'InventoryManagement'
      and menu_type = 'M'
    )
    or (
      menu_id = 2100
      and menu_name = '审核中心'
      and parent_id = 0
      and path = 'review-center'
      and route_name = 'ReviewCenter'
      and menu_type = 'M'
    );

  if v_parent_count <> 5 then
    signal sqlstate '45000' set message_text = 'business menu parent sys_menu entries are required before business menu seed';
  end if;

  if exists (
    select 1
    from sys_menu m
    where exists (
        select 1
        from tmp_business_menu_sys_menu_guard seed
        where seed.menu_id = m.menu_id
    )
      and not exists (
        select 1
        from tmp_business_menu_sys_menu_guard seed
        where seed.menu_id = m.menu_id
          and coalesce(m.parent_id, -1) = seed.parent_id
          and coalesce(m.menu_type, '') = seed.menu_type
          and coalesce(m.path, '') = coalesce(seed.path, '')
          and coalesce(m.component, '') = coalesce(seed.component, '')
          and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
          and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'business sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_business_menu_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'business sys_menu signature is already used by another menu';
  end if;
end//

delimiter ;

call assert_business_menu_seed_confirmed();
drop procedure if exists assert_business_menu_seed_confirmed;

create temporary table if not exists tmp_business_menu_sys_menu_guard (
  menu_id    bigint       not null,
  parent_id  bigint       not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  key idx_business_menu_sys_menu_guard_id (menu_id)
) engine=memory;

truncate table tmp_business_menu_sys_menu_guard;

insert into tmp_business_menu_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (2400, 2060, 'C', 'list', 'Product/SourceProductLibrary/index', 'SourceProductLibrary', 'product:list:list'),
    (2401, 2060, 'C', 'zone', 'Common/PlannedPage/index', 'ProductZone', 'product:zone:list'),
    (2403, 2060, 'C', 'warehouse-link', 'Common/PlannedPage/index', 'WarehouseProductLink', 'product:warehouseLink:list'),
    (2410, 2070, 'C', 'list', 'Common/PlannedPage/index', 'OrderList', 'order:list:list'),
    (2411, 2070, 'C', 'return', 'Common/PlannedPage/index', 'ReturnManagement', 'order:return:list'),
    (2422, 2080, 'C', 'flow', 'Common/PlannedPage/index', 'InventoryFlow', 'inventory:flow:list'),
    (2430, 2050, 'C', 'fund-account', 'Common/PlannedPage/index', 'FundAccount', 'finance:fundAccount:list'),
    (2431, 2050, 'C', 'collection-account', 'Common/PlannedPage/index', 'CollectionAccount', 'finance:collectionAccount:list'),
    (2432, 2050, 'C', 'distribution-fund', 'Common/PlannedPage/index', 'DistributionFund', 'finance:distributionFund:list'),
    (2433, 2050, 'C', 'fee', 'Common/PlannedPage/index', 'FeeManagement', 'finance:fee:list'),
    (2434, 2050, 'C', 'profit-reconciliation', 'Common/PlannedPage/index', 'ProfitReconciliation', 'finance:profitReconciliation:list'),
    (2450, 2100, 'C', 'recharge', 'Common/PlannedPage/index', 'RechargeReview', 'review:recharge:list'),
    (2451, 2100, 'C', 'product-distribution', 'Common/PlannedPage/index', 'ProductDistributionReview', 'review:productDistribution:list'),
    (2452, 2100, 'C', 'inventory-adjustment', 'Common/PlannedPage/index', 'InventoryAdjustmentReview', 'review:inventoryAdjustment:list');

call assert_business_menu_sys_menu_guard();

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2400, '来源商品库', 2060, 5, 'list', 'Product/SourceProductLibrary/index', '', 'SourceProductLibrary',
     1, 0, 'C', '0', '0', 'product:list:list', 'AppstoreOutlined', 'admin',
     sysdate(), '', null, '商品管理菜单：来源商品库，展示各来源系统同步 SKU 基础信息'),
    (2401, '商品专区', 2060, 10, 'zone', 'Common/PlannedPage/index', '', 'ProductZone',
     1, 0, 'C', '0', '0', 'product:zone:list', 'TagsOutlined', 'admin',
     sysdate(), '', null, '商品管理菜单：商品专区，占位入口'),
    (2403, '仓库商品关联', 2060, 20, 'warehouse-link', 'Common/PlannedPage/index', '', 'WarehouseProductLink',
     1, 0, 'C', '0', '0', 'product:warehouseLink:list', 'LinkOutlined', 'admin',
     sysdate(), '', null, '商品管理菜单：仓库商品关联，占位入口'),

    (2410, '订单列表', 2070, 5, 'list', 'Common/PlannedPage/index', '', 'OrderList',
     1, 0, 'C', '0', '0', 'order:list:list', 'ProfileOutlined', 'admin',
     sysdate(), '', null, '订单管理菜单：订单列表，占位入口'),
    (2411, '退件管理', 2070, 10, 'return', 'Common/PlannedPage/index', '', 'ReturnManagement',
     1, 0, 'C', '0', '0', 'order:return:list', 'UndoOutlined', 'admin',
     sysdate(), '', null, '订单管理菜单：退件管理，占位入口'),

    (2422, '库存流水', 2080, 15, 'flow', 'Common/PlannedPage/index', '', 'InventoryFlow',
     1, 0, 'C', '0', '0', 'inventory:flow:list', 'UnorderedListOutlined', 'admin',
     sysdate(), '', null, '库存管理菜单：库存流水，占位入口'),

    (2430, '资金账户', 2050, 5, 'fund-account', 'Common/PlannedPage/index', '', 'FundAccount',
     1, 0, 'C', '0', '0', 'finance:fundAccount:list', 'WalletOutlined', 'admin',
     sysdate(), '', null, '财务管理菜单：资金账户，占位入口'),
    (2431, '收款账户', 2050, 10, 'collection-account', 'Common/PlannedPage/index', '', 'CollectionAccount',
     1, 0, 'C', '0', '0', 'finance:collectionAccount:list', 'BankOutlined', 'admin',
     sysdate(), '', null, '财务管理菜单：收款账户，占位入口'),
    (2432, '分销资金', 2050, 15, 'distribution-fund', 'Common/PlannedPage/index', '', 'DistributionFund',
     1, 0, 'C', '0', '0', 'finance:distributionFund:list', 'TransactionOutlined', 'admin',
     sysdate(), '', null, '财务管理菜单：分销资金，占位入口'),
    (2433, '费用管理', 2050, 20, 'fee', 'Common/PlannedPage/index', '', 'FeeManagement',
     1, 0, 'C', '0', '0', 'finance:fee:list', 'DollarOutlined', 'admin',
     sysdate(), '', null, '财务管理菜单：费用管理，占位入口'),
    (2434, '利润对账', 2050, 25, 'profit-reconciliation', 'Common/PlannedPage/index', '', 'ProfitReconciliation',
     1, 0, 'C', '0', '0', 'finance:profitReconciliation:list', 'ReconciliationOutlined', 'admin',
     sysdate(), '', null, '财务管理菜单：利润对账，占位入口'),

    (2450, '充值审核', 2100, 5, 'recharge', 'Common/PlannedPage/index', '', 'RechargeReview',
     1, 0, 'C', '0', '0', 'review:recharge:list', 'AuditOutlined', 'admin',
     sysdate(), '', null, '审核中心菜单：充值审核，占位入口'),
    (2451, '商品分销审核', 2100, 10, 'product-distribution', 'Common/PlannedPage/index', '', 'ProductDistributionReview',
     1, 0, 'C', '0', '0', 'review:productDistribution:list', 'AuditOutlined', 'admin',
     sysdate(), '', null, '审核中心菜单：商品分销审核，占位入口'),
    (2452, '库存调整审核', 2100, 15, 'inventory-adjustment', 'Common/PlannedPage/index', '', 'InventoryAdjustmentReview',
     1, 0, 'C', '0', '0', 'review:inventoryAdjustment:list', 'AuditOutlined', 'admin',
     sysdate(), '', null, '审核中心菜单：库存调整审核，占位入口')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query = values(query),
    route_name = values(route_name),
    is_frame = values(is_frame),
    is_cache = values(is_cache),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    update_by = 'admin',
    update_time = sysdate(),
    remark = values(remark);

drop temporary table if exists tmp_business_menu_sys_menu_guard;
drop procedure if exists assert_business_menu_sys_menu_guard;
