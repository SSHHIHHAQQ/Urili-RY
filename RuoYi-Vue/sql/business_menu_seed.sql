-- Business menu seed for the RuoYi validation project.
-- Scope: second-level business menu entries only. No business table, API, or button permission is created here.
-- Run after top_menu_seed.sql so parent directories are present.

set names utf8mb4;

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
    (2402, '商城商品列表', 2060, 15, 'distribution', 'Product/Distribution/index', '', 'DistributionProduct',
     1, 0, 'C', '0', '0', 'product:distribution:list', 'ShareAltOutlined', 'admin',
     sysdate(), '', null, '商品管理菜单：商城商品列表'),
    (2403, '仓库商品关联', 2060, 20, 'warehouse-link', 'Common/PlannedPage/index', '', 'WarehouseProductLink',
     1, 0, 'C', '0', '0', 'product:warehouseLink:list', 'LinkOutlined', 'admin',
     sysdate(), '', null, '商品管理菜单：仓库商品关联，占位入口'),

    (2410, '订单列表', 2070, 5, 'list', 'Common/PlannedPage/index', '', 'OrderList',
     1, 0, 'C', '0', '0', 'order:list:list', 'ProfileOutlined', 'admin',
     sysdate(), '', null, '订单管理菜单：订单列表，占位入口'),
    (2411, '退件管理', 2070, 10, 'return', 'Common/PlannedPage/index', '', 'ReturnManagement',
     1, 0, 'C', '0', '0', 'order:return:list', 'UndoOutlined', 'admin',
     sysdate(), '', null, '订单管理菜单：退件管理，占位入口'),
    (2412, '售后管理', 2070, 15, 'after-sale', 'Common/PlannedPage/index', '', 'AfterSaleManagement',
     1, 0, 'C', '0', '0', 'order:afterSale:list', 'IssuesCloseOutlined', 'admin',
     sysdate(), '', null, '订单管理菜单：售后管理，占位入口'),

    (2420, '库存总览', 2080, 5, 'overview', 'Common/PlannedPage/index', '', 'InventoryOverview',
     1, 0, 'C', '0', '0', 'inventory:overview:list', 'DashboardOutlined', 'admin',
     sysdate(), '', null, '库存管理菜单：库存总览，占位入口'),
    (2421, '来源仓库库存', 2080, 10, 'source-warehouse-stock', 'Inventory/SourceWarehouseStock/index', '', 'SourceWarehouseStock',
     1, 0, 'C', '0', '0', 'inventory:sourceWarehouse:list', 'StockOutlined', 'admin',
     sysdate(), '', null, '库存管理菜单：来源仓库库存，读取上游系统SKU库存同步快照'),
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

    (2440, '商品分类配置', 2090, 5, 'product-category', 'Common/PlannedPage/index', '', 'ProductCategoryConfig',
     1, 0, 'C', '0', '0', 'basic:productCategory:list', 'ApartmentOutlined', 'admin',
     sysdate(), '', null, '基础配置菜单：商品分类配置，占位入口'),
    (2441, '商品属性配置', 2090, 10, 'product-attribute', 'Common/PlannedPage/index', '', 'ProductAttributeConfig',
     1, 0, 'C', '0', '0', 'basic:productAttribute:list', 'ControlOutlined', 'admin',
     sysdate(), '', null, '基础配置菜单：商品属性配置，占位入口'),
    (2442, '币种配置', 2090, 15, 'currency', 'Common/PlannedPage/index', '', 'CurrencyConfig',
     1, 0, 'C', '0', '0', 'basic:currency:list', 'MoneyCollectOutlined', 'admin',
     sysdate(), '', null, '基础配置菜单：币种配置，占位入口'),

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

-- Keep the source warehouse stock entry as a placeholder until the inventory
-- snapshot schema, sync job, and permission rollout are explicitly confirmed.
update sys_menu
set component = 'Common/PlannedPage/index',
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2421
  and perms = 'inventory:sourceWarehouse:list';
