-- Inventory overview auto WMS stock sync policy.
-- Purpose:
-- 1. Store seller/warehouse/SPU/SKU/stock-row inventory sync policies.
-- 2. Snapshot the effective sync policy on SKU + warehouse stock rows for fast list filtering.
-- 3. Add the admin permission button for inventory overview sync policy changes.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_inventory_auto_wms_stock_sync_policy := coalesce(@confirm_inventory_auto_wms_stock_sync_policy, '');
set @inventory_auto_wms_menu_expected_count := coalesce(@inventory_auto_wms_menu_expected_count, '');
set @inventory_auto_wms_menu_expected_signature := coalesce(@inventory_auto_wms_menu_expected_signature, '');
set @inventory_auto_wms_dict_type_expected_count := coalesce(@inventory_auto_wms_dict_type_expected_count, '');
set @inventory_auto_wms_dict_type_expected_signature := coalesce(@inventory_auto_wms_dict_type_expected_signature, '');
set @inventory_auto_wms_dict_data_expected_count := coalesce(@inventory_auto_wms_dict_data_expected_count, '');
set @inventory_auto_wms_dict_data_expected_signature := coalesce(@inventory_auto_wms_dict_data_expected_signature, '');

delimiter //

drop procedure if exists assert_inventory_auto_wms_stock_sync_policy_confirmed//
create procedure assert_inventory_auto_wms_stock_sync_policy_confirmed()
begin
  if coalesce(@confirm_inventory_auto_wms_stock_sync_policy, '')
      <> 'APPLY_INVENTORY_AUTO_WMS_STOCK_SYNC_POLICY' then
    signal sqlstate '45000' set message_text = 'set @confirm_inventory_auto_wms_stock_sync_policy = APPLY_INVENTORY_AUTO_WMS_STOCK_SYNC_POLICY before running this migration';
  end if;
  if coalesce(@inventory_auto_wms_menu_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @inventory_auto_wms_menu_expected_count after previewing exact inventory auto wms sys_menu rows';
  end if;
  if coalesce(@inventory_auto_wms_menu_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @inventory_auto_wms_menu_expected_signature after previewing exact inventory auto wms sys_menu rows';
  end if;
  if coalesce(@inventory_auto_wms_dict_type_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @inventory_auto_wms_dict_type_expected_count after previewing exact inventory auto wms sys_dict_type rows';
  end if;
  if coalesce(@inventory_auto_wms_dict_type_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @inventory_auto_wms_dict_type_expected_signature after previewing exact inventory auto wms sys_dict_type rows';
  end if;
  if coalesce(@inventory_auto_wms_dict_data_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @inventory_auto_wms_dict_data_expected_count after previewing exact inventory auto wms sys_dict_data rows';
  end if;
  if coalesce(@inventory_auto_wms_dict_data_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @inventory_auto_wms_dict_data_expected_signature after previewing exact inventory auto wms sys_dict_data rows';
  end if;
end//

drop procedure if exists assert_inventory_auto_wms_table_exists//
create procedure assert_inventory_auto_wms_table_exists(in p_table varchar(64), in p_message varchar(128))
begin
  if not exists (
    select 1
    from information_schema.tables
    where table_schema = database()
      and table_name = p_table
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_inventory_auto_wms_menu_guard//
create procedure assert_inventory_auto_wms_menu_guard()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2420
      and parent_id = 2080
      and menu_type = 'C'
      and path = 'overview'
      and component = 'Inventory/Overview/index'
      and route_name = 'InventoryOverview'
      and perms = 'inventory:overview:list'
  ) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sync policy requires inventory overview menu 2420';
  end if;

  if exists (
    select 1
    from sys_menu
    where menu_id = 242005
      and (
        coalesce(parent_id, -1) <> 2420
        or coalesce(menu_type, '') <> 'F'
        or coalesce(path, '') <> '#'
        or coalesce(component, '') <> ''
        or coalesce(route_name, '') <> ''
        or coalesce(perms, '') <> 'inventory:overview:syncPolicy'
      )
  ) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sync policy sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu
    where menu_id <> 242005
      and coalesce(path, '') = '#'
      and coalesce(component, '') = ''
      and coalesce(route_name, '') = ''
      and coalesce(perms, '') = 'inventory:overview:syncPolicy'
  ) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sync policy permission is already used by another menu';
  end if;
end//

drop procedure if exists assert_inventory_auto_wms_seed_targets//
create procedure assert_inventory_auto_wms_seed_targets()
begin
  declare v_menu_count bigint default 0;
  declare v_menu_signature varchar(64) default '';
  declare v_dict_type_count bigint default 0;
  declare v_dict_type_signature varchar(64) default '';
  declare v_dict_data_count bigint default 0;
  declare v_dict_data_signature varchar(64) default '';

  if exists (
    select 1
    from sys_dict_type t
    join tmp_inventory_auto_wms_dict_type_seed seed on seed.dict_type = t.dict_type
    where coalesce(t.dict_name, '') <> seed.dict_name
       or coalesce(t.status, '') <> '0'
  ) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sys_dict_type target is occupied by incompatible row';
  end if;

  if exists (
    select 1
    from sys_dict_data d
    join tmp_inventory_auto_wms_dict_data_seed seed
      on seed.dict_type = d.dict_type
     and seed.dict_value = d.dict_value
    where coalesce(d.dict_sort, -1) <> seed.dict_sort
       or coalesce(d.dict_label, '') <> seed.dict_label
       or coalesce(d.list_class, '') <> seed.list_class
       or coalesce(d.is_default, '') <> seed.is_default
       or coalesce(d.status, '') <> '0'
  ) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sys_dict_data target is occupied by incompatible row';
  end if;

  select count(distinct m.menu_id),
         sha2(coalesce(group_concat(distinct
           concat_ws('|',
             m.menu_id,
             coalesce(m.menu_name, ''),
             coalesce(m.parent_id, ''),
             coalesce(m.order_num, ''),
             coalesce(m.path, ''),
             coalesce(m.component, ''),
             coalesce(m.query, ''),
             coalesce(m.route_name, ''),
             coalesce(m.is_frame, ''),
             coalesce(m.is_cache, ''),
             coalesce(m.menu_type, ''),
             coalesce(m.visible, ''),
             coalesce(m.status, ''),
             coalesce(m.perms, ''),
             coalesce(m.icon, ''),
             coalesce(m.remark, '')
           )
           order by m.menu_id separator '\n'
         ), ''), 256)
    into v_menu_count, v_menu_signature
  from sys_menu m
  join tmp_inventory_auto_wms_sys_menu_seed seed
    on m.menu_id = seed.menu_id
    or (coalesce(seed.perms, '') <> '' and coalesce(m.perms, '') = seed.perms);

  if v_menu_count <> cast(@inventory_auto_wms_menu_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sys_menu exact target count mismatch';
  end if;
  if lower(v_menu_signature) <> lower(@inventory_auto_wms_menu_expected_signature) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sys_menu exact target signature mismatch';
  end if;

  select count(distinct t.dict_type),
         sha2(coalesce(group_concat(distinct
           concat_ws('|',
             coalesce(t.dict_name, ''),
             coalesce(t.dict_type, ''),
             coalesce(t.status, ''),
             coalesce(t.remark, '')
           )
           order by t.dict_type separator '\n'
         ), ''), 256)
    into v_dict_type_count, v_dict_type_signature
  from sys_dict_type t
  join tmp_inventory_auto_wms_dict_type_seed seed on seed.dict_type = t.dict_type;

  if v_dict_type_count <> cast(@inventory_auto_wms_dict_type_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sys_dict_type exact target count mismatch';
  end if;
  if lower(v_dict_type_signature) <> lower(@inventory_auto_wms_dict_type_expected_signature) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sys_dict_type exact target signature mismatch';
  end if;

  select count(distinct d.dict_code),
         sha2(coalesce(group_concat(distinct
           concat_ws('|',
             coalesce(d.dict_sort, ''),
             coalesce(d.dict_label, ''),
             coalesce(d.dict_value, ''),
             coalesce(d.dict_type, ''),
             coalesce(d.list_class, ''),
             coalesce(d.is_default, ''),
             coalesce(d.status, ''),
             coalesce(d.remark, '')
           )
           order by d.dict_type, d.dict_value separator '\n'
         ), ''), 256)
    into v_dict_data_count, v_dict_data_signature
  from sys_dict_data d
  join tmp_inventory_auto_wms_dict_data_seed seed
    on seed.dict_type = d.dict_type
   and seed.dict_value = d.dict_value;

  if v_dict_data_count <> cast(@inventory_auto_wms_dict_data_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sys_dict_data exact target count mismatch';
  end if;
  if lower(v_dict_data_signature) <> lower(@inventory_auto_wms_dict_data_expected_signature) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sys_dict_data exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_inventory_auto_wms_schema_ready//
create procedure assert_inventory_auto_wms_schema_ready()
begin
  if exists (
    select 1
    from tmp_inventory_auto_wms_column_contract expected
    left join information_schema.columns actual
      on actual.table_schema = database()
     and actual.table_name = expected.table_name
     and actual.column_name = expected.column_name
    where actual.column_name is null
       or (
         case
           when lower(actual.column_type) like 'bigint%' then 'bigint'
           else lower(actual.column_type)
         end
       ) <> expected.column_type
       or coalesce(actual.is_nullable, '') <> expected.is_nullable
       or coalesce(lower(actual.column_default), '__NULL__') <> coalesce(lower(expected.column_default), '__NULL__')
  ) then
    signal sqlstate '45000' set message_text = 'inventory auto wms schema column contract mismatch';
  end if;

  if exists (
    select 1
    from tmp_inventory_auto_wms_index_contract expected
    left join information_schema.statistics actual
      on actual.table_schema = database()
     and actual.table_name = expected.table_name
     and actual.index_name = expected.index_name
     and actual.seq_in_index = expected.seq_in_index
     and actual.column_name = expected.column_name
    where actual.index_name is null
       or coalesce(actual.non_unique, -1) <> expected.non_unique
  ) then
    signal sqlstate '45000' set message_text = 'inventory auto wms schema index contract mismatch';
  end if;
end//

drop procedure if exists assert_inventory_auto_wms_stock_sync_policy_completed//
create procedure assert_inventory_auto_wms_stock_sync_policy_completed()
begin
  call assert_inventory_auto_wms_schema_ready();

  if (
    select count(1)
    from sys_menu m
    join tmp_inventory_auto_wms_sys_menu_seed seed on seed.menu_id = m.menu_id
    where coalesce(m.menu_name, '') = seed.menu_name
      and coalesce(m.parent_id, -1) = seed.parent_id
      and coalesce(m.order_num, -1) = seed.order_num
      and coalesce(m.menu_type, '') = seed.menu_type
      and coalesce(m.path, '') = seed.path
      and coalesce(m.component, '') = seed.component
      and coalesce(m.route_name, '') = seed.route_name
      and coalesce(m.perms, '') = seed.perms
      and coalesce(m.icon, '') = seed.icon
      and coalesce(m.remark, '') = seed.remark
  ) <> (select count(1) from tmp_inventory_auto_wms_sys_menu_seed) then
    signal sqlstate '45000' set message_text = 'inventory auto wms sys_menu seed completion mismatch';
  end if;

  if (
    select count(1)
    from sys_dict_type t
    join tmp_inventory_auto_wms_dict_type_seed seed on seed.dict_type = t.dict_type
    where coalesce(t.dict_name, '') = seed.dict_name
      and coalesce(t.status, '') = '0'
  ) <> (select count(1) from tmp_inventory_auto_wms_dict_type_seed) then
    signal sqlstate '45000' set message_text = 'inventory auto wms dict type seed completion mismatch';
  end if;

  if (
    select count(1)
    from sys_dict_data d
    join tmp_inventory_auto_wms_dict_data_seed seed
      on seed.dict_type = d.dict_type
     and seed.dict_value = d.dict_value
    where coalesce(d.dict_sort, -1) = seed.dict_sort
      and coalesce(d.dict_label, '') = seed.dict_label
      and coalesce(d.list_class, '') = seed.list_class
      and coalesce(d.is_default, '') = seed.is_default
      and coalesce(d.status, '') = '0'
  ) <> (select count(1) from tmp_inventory_auto_wms_dict_data_seed) then
    signal sqlstate '45000' set message_text = 'inventory auto wms dict data seed completion mismatch';
  end if;
end//

drop procedure if exists add_column_if_missing//
create procedure add_column_if_missing(
  in p_table varchar(64),
  in p_column varchar(64),
  in p_definition text
)
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table
      and column_name = p_column
  ) then
    set @ddl = concat('alter table `', p_table, '` add column ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

drop procedure if exists add_index_if_missing//
create procedure add_index_if_missing(
  in p_table varchar(64),
  in p_index_name varchar(64),
  in p_definition text
)
begin
  if not exists (
    select 1
    from information_schema.statistics
    where table_schema = database()
      and table_name = p_table
      and index_name = p_index_name
  ) then
    set @ddl = concat('alter table `', p_table, '` add ', p_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end//

delimiter ;

call assert_inventory_auto_wms_stock_sync_policy_confirmed();
call assert_inventory_auto_wms_table_exists('inventory_sku_warehouse_stock',
  'inventory_sku_warehouse_stock is required before auto wms sync policy');
call assert_inventory_auto_wms_table_exists('inventory_stock_ledger',
  'inventory_stock_ledger is required before auto wms sync policy');
call assert_inventory_auto_wms_table_exists('inventory_overview_sku_read_model',
  'inventory_overview_sku_read_model is required before auto wms sync policy');
call assert_inventory_auto_wms_table_exists('inventory_overview_spu_read_model',
  'inventory_overview_spu_read_model is required before auto wms sync policy');
call assert_inventory_auto_wms_table_exists('product_spu',
  'product_spu is required before auto wms sync policy');
call assert_inventory_auto_wms_menu_guard();

create temporary table if not exists tmp_inventory_auto_wms_sys_menu_seed (
  menu_id    bigint       not null,
  menu_name  varchar(50)  not null default '',
  parent_id  bigint       not null,
  order_num  int          not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  icon       varchar(100) not null default '',
  remark     varchar(500) not null default '',
  key idx_inventory_auto_wms_sys_menu_seed_id (menu_id)
) engine=memory;

truncate table tmp_inventory_auto_wms_sys_menu_seed;

insert into tmp_inventory_auto_wms_sys_menu_seed
  (menu_id, menu_name, parent_id, order_num, menu_type, path, component, route_name, perms, icon, remark)
values
  (242005, '库存同步方式设置', 2420, 5, 'F', '#', '', '', 'inventory:overview:syncPolicy', '#',
   '库存总览自动同步WMS库存设置按钮');

create temporary table if not exists tmp_inventory_auto_wms_dict_type_seed (
  dict_name varchar(100) not null,
  dict_type varchar(100) not null,
  key idx_inventory_auto_wms_dict_type_seed (dict_type)
) engine=memory;

truncate table tmp_inventory_auto_wms_dict_type_seed;

insert into tmp_inventory_auto_wms_dict_type_seed(dict_name, dict_type) values
  ('库存同步方式', 'inventory_stock_sync_mode');

create temporary table if not exists tmp_inventory_auto_wms_dict_data_seed (
  dict_sort  int          not null,
  dict_label varchar(100) not null,
  dict_value varchar(100) not null,
  dict_type  varchar(100) not null,
  list_class varchar(100) not null default '',
  is_default char(1)      not null default 'N',
  key idx_inventory_auto_wms_dict_data_seed (dict_type, dict_value)
) engine=memory;

truncate table tmp_inventory_auto_wms_dict_data_seed;

insert into tmp_inventory_auto_wms_dict_data_seed
  (dict_sort, dict_label, dict_value, dict_type, list_class, is_default)
values
  (1, '手动设置平台库存', 'MANUAL', 'inventory_stock_sync_mode', 'default', 'N'),
  (2, '自动同步WMS库存', 'AUTO_SOURCE_AVAILABLE', 'inventory_stock_sync_mode', 'processing', 'N'),
  (3, '混合', 'MIXED', 'inventory_stock_sync_mode', 'warning', 'N'),
  (9, '自动同步WMS库存', 'AUTO_SOURCE_SYNC', 'inventory_operation_type', '', 'N');

call assert_inventory_auto_wms_seed_targets();

create temporary table if not exists tmp_inventory_auto_wms_column_contract (
  table_name     varchar(64)  not null,
  column_name    varchar(64)  not null,
  column_type    varchar(255) not null,
  is_nullable    varchar(3)   not null,
  column_default varchar(255) null,
  key idx_inventory_auto_wms_column_contract (table_name, column_name)
) engine=memory;

truncate table tmp_inventory_auto_wms_column_contract;

insert into tmp_inventory_auto_wms_column_contract
  (table_name, column_name, column_type, is_nullable, column_default)
values
  ('inventory_stock_sync_policy', 'policy_key', 'varchar(200)', 'NO', null),
  ('inventory_stock_sync_policy', 'seller_id', 'bigint', 'NO', null),
  ('inventory_stock_sync_policy', 'scope_type', 'varchar(32)', 'NO', null),
  ('inventory_stock_sync_policy', 'sync_mode', 'varchar(32)', 'NO', 'MANUAL'),
  ('inventory_stock_sync_policy', 'enabled', 'char(1)', 'NO', 'Y'),
  ('inventory_stock_sync_policy', 'version', 'int', 'NO', '0'),
  ('inventory_sku_warehouse_stock', 'sync_mode', 'varchar(32)', 'NO', 'MANUAL'),
  ('inventory_sku_warehouse_stock', 'sync_policy_id', 'bigint', 'YES', null),
  ('inventory_sku_warehouse_stock', 'sync_policy_scope', 'varchar(32)', 'NO', 'SYSTEM'),
  ('inventory_sku_warehouse_stock', 'sync_policy_key', 'varchar(200)', 'NO', ''),
  ('inventory_sku_warehouse_stock', 'sync_status', 'varchar(32)', 'NO', 'NORMAL'),
  ('inventory_sku_warehouse_stock', 'last_auto_sync_time', 'datetime', 'YES', null),
  ('inventory_stock_ledger', 'sync_policy_id', 'bigint', 'YES', null),
  ('inventory_stock_ledger', 'sync_policy_scope', 'varchar(32)', 'NO', ''),
  ('inventory_stock_ledger', 'sync_policy_key', 'varchar(200)', 'NO', ''),
  ('inventory_overview_sku_read_model', 'seller_no', 'varchar(64)', 'NO', ''),
  ('inventory_overview_sku_read_model', 'seller_name', 'varchar(255)', 'NO', ''),
  ('inventory_overview_sku_read_model', 'sync_mode_summary', 'varchar(32)', 'NO', 'MANUAL'),
  ('inventory_overview_sku_read_model', 'sync_policy_scope_summary', 'varchar(32)', 'NO', 'SYSTEM'),
  ('inventory_overview_spu_read_model', 'seller_no', 'varchar(64)', 'NO', ''),
  ('inventory_overview_spu_read_model', 'seller_name', 'varchar(255)', 'NO', ''),
  ('inventory_overview_spu_read_model', 'sync_mode_summary', 'varchar(32)', 'NO', 'MANUAL'),
  ('inventory_overview_spu_read_model', 'sync_policy_scope_summary', 'varchar(32)', 'NO', 'SYSTEM');

create temporary table if not exists tmp_inventory_auto_wms_index_contract (
  table_name   varchar(64)  not null,
  index_name   varchar(64)  not null,
  non_unique   int          not null,
  seq_in_index int          not null,
  column_name  varchar(64)  not null,
  key idx_inventory_auto_wms_index_contract (table_name, index_name, seq_in_index)
) engine=memory;

truncate table tmp_inventory_auto_wms_index_contract;

insert into tmp_inventory_auto_wms_index_contract
  (table_name, index_name, non_unique, seq_in_index, column_name)
values
  ('inventory_stock_sync_policy', 'uk_inventory_stock_sync_policy_key', 0, 1, 'policy_key'),
  ('inventory_stock_sync_policy', 'idx_inventory_stock_sync_policy_seller', 1, 1, 'seller_id'),
  ('inventory_stock_sync_policy', 'idx_inventory_stock_sync_policy_seller', 1, 2, 'scope_type'),
  ('inventory_stock_sync_policy', 'idx_inventory_stock_sync_policy_seller', 1, 3, 'enabled'),
  ('inventory_stock_sync_policy', 'idx_inventory_stock_sync_policy_stock', 1, 1, 'stock_id'),
  ('inventory_stock_sync_policy', 'idx_inventory_stock_sync_policy_sku', 1, 1, 'sku_id'),
  ('inventory_stock_sync_policy', 'idx_inventory_stock_sync_policy_spu', 1, 1, 'spu_id'),
  ('inventory_sku_warehouse_stock', 'idx_inventory_stock_sync_mode', 1, 1, 'sync_mode'),
  ('inventory_sku_warehouse_stock', 'idx_inventory_stock_sync_mode', 1, 2, 'sync_policy_scope'),
  ('inventory_sku_warehouse_stock', 'idx_inventory_stock_sync_policy', 1, 1, 'sync_policy_id');

create table if not exists inventory_stock_sync_policy (
  policy_id bigint(20) not null auto_increment comment '策略主键',
  policy_key varchar(200) not null comment '策略自然键',
  seller_id bigint(20) not null comment '卖家ID',
  scope_type varchar(32) not null comment '策略范围：SELLER/WAREHOUSE/SPU/SKU/SKU_WAREHOUSE',
  warehouse_key varchar(255) not null default '' comment '仓库稳定key',
  warehouse_name varchar(200) not null default '' comment '仓库展示名快照',
  spu_id bigint(20) default null comment 'SPU ID',
  sku_id bigint(20) default null comment 'SKU ID',
  stock_id bigint(20) default null comment 'SKU+仓库库存行ID',
  sync_mode varchar(32) not null default 'MANUAL' comment '同步方式：MANUAL/AUTO_SOURCE_AVAILABLE',
  enabled char(1) not null default 'Y' comment '是否启用',
  version int not null default 0 comment '乐观锁版本',
  last_apply_time datetime default null comment '最近应用时间',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default '' comment '备注',
  primary key (policy_id),
  unique key uk_inventory_stock_sync_policy_key (policy_key),
  key idx_inventory_stock_sync_policy_seller (seller_id, scope_type, enabled),
  key idx_inventory_stock_sync_policy_stock (stock_id),
  key idx_inventory_stock_sync_policy_sku (sku_id),
  key idx_inventory_stock_sync_policy_spu (spu_id)
) engine=innodb default charset=utf8mb4 comment='库存同步策略表';

call add_column_if_missing('inventory_sku_warehouse_stock', 'sync_mode',
  'sync_mode varchar(32) not null default ''MANUAL'' comment ''库存同步方式'' after effective_status');
call add_column_if_missing('inventory_sku_warehouse_stock', 'sync_policy_id',
  'sync_policy_id bigint(20) default null comment ''当前命中同步策略ID'' after sync_mode');
call add_column_if_missing('inventory_sku_warehouse_stock', 'sync_policy_scope',
  'sync_policy_scope varchar(32) not null default ''SYSTEM'' comment ''当前命中同步策略范围'' after sync_policy_id');
call add_column_if_missing('inventory_sku_warehouse_stock', 'sync_policy_key',
  'sync_policy_key varchar(200) not null default '''' comment ''当前命中同步策略key'' after sync_policy_scope');
call add_column_if_missing('inventory_sku_warehouse_stock', 'sync_status',
  'sync_status varchar(32) not null default ''NORMAL'' comment ''同步状态'' after sync_policy_key');
call add_column_if_missing('inventory_sku_warehouse_stock', 'last_auto_sync_time',
  'last_auto_sync_time datetime default null comment ''最近自动同步时间'' after sync_status');
call add_index_if_missing('inventory_sku_warehouse_stock', 'idx_inventory_stock_sync_mode',
  'index idx_inventory_stock_sync_mode (sync_mode, sync_policy_scope)');
call add_index_if_missing('inventory_sku_warehouse_stock', 'idx_inventory_stock_sync_policy',
  'index idx_inventory_stock_sync_policy (sync_policy_id)');

call add_column_if_missing('inventory_stock_ledger', 'sync_policy_id',
  'sync_policy_id bigint(20) default null comment ''同步策略ID快照'' after seller_id');
call add_column_if_missing('inventory_stock_ledger', 'sync_policy_scope',
  'sync_policy_scope varchar(32) not null default '''' comment ''同步策略范围快照'' after sync_policy_id');
call add_column_if_missing('inventory_stock_ledger', 'sync_policy_key',
  'sync_policy_key varchar(200) not null default '''' comment ''同步策略key快照'' after sync_policy_scope');

call add_column_if_missing('inventory_overview_sku_read_model', 'seller_no',
  'seller_no varchar(64) not null default '''' comment ''卖家编号'' after seller_id');
call add_column_if_missing('inventory_overview_sku_read_model', 'seller_name',
  'seller_name varchar(255) not null default '''' comment ''卖家名称'' after seller_no');
call add_column_if_missing('inventory_overview_sku_read_model', 'sync_mode_summary',
  'sync_mode_summary varchar(32) not null default ''MANUAL'' comment ''同步方式摘要'' after inventory_status');
call add_column_if_missing('inventory_overview_sku_read_model', 'sync_policy_scope_summary',
  'sync_policy_scope_summary varchar(32) not null default ''SYSTEM'' comment ''同步策略范围摘要'' after sync_mode_summary');

call add_column_if_missing('inventory_overview_spu_read_model', 'seller_no',
  'seller_no varchar(64) not null default '''' comment ''卖家编号'' after seller_id');
call add_column_if_missing('inventory_overview_spu_read_model', 'seller_name',
  'seller_name varchar(255) not null default '''' comment ''卖家名称'' after seller_no');
call add_column_if_missing('inventory_overview_spu_read_model', 'sync_mode_summary',
  'sync_mode_summary varchar(32) not null default ''MANUAL'' comment ''同步方式摘要'' after inventory_status');
call add_column_if_missing('inventory_overview_spu_read_model', 'sync_policy_scope_summary',
  'sync_policy_scope_summary varchar(32) not null default ''SYSTEM'' comment ''同步策略范围摘要'' after sync_mode_summary');

call assert_inventory_auto_wms_schema_ready();

start transaction;

insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '库存同步方式', 'inventory_stock_sync_mode', '0', 'admin', sysdate(), '库存总览同步方式'
where not exists (select 1 from sys_dict_type where dict_type = 'inventory_stock_sync_mode');

insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'inventory_stock_sync_mode', '', seed.list_class, 'N', '0', 'admin', sysdate(), '库存总览同步方式'
from (
    select 1 dict_sort, '手动设置平台库存' dict_label, 'MANUAL' dict_value, 'default' list_class
    union all select 2, '自动同步WMS库存', 'AUTO_SOURCE_AVAILABLE', 'processing'
    union all select 3, '混合', 'MIXED', 'warning'
) seed
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'inventory_stock_sync_mode' and d.dict_value = seed.dict_value
);

insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 9, '自动同步WMS库存', 'AUTO_SOURCE_SYNC', 'inventory_operation_type', '', '', 'N', '0', 'admin', sysdate(), '自动同步WMS库存流水'
where not exists (
    select 1 from sys_dict_data d
    where d.dict_type = 'inventory_operation_type'
      and d.dict_value = 'AUTO_SOURCE_SYNC'
);

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select 242005, '库存同步方式设置', 2420, 5, '#', '', '', '',
       1, 0, 'F', '0', '0', 'inventory:overview:syncPolicy', '#', 'admin',
       sysdate(), '', null, '库存总览自动同步WMS库存设置按钮'
where not exists (select 1 from sys_menu where menu_id = 242005);

call assert_inventory_auto_wms_stock_sync_policy_completed();

commit;

drop temporary table if exists tmp_inventory_auto_wms_index_contract;
drop temporary table if exists tmp_inventory_auto_wms_column_contract;
drop temporary table if exists tmp_inventory_auto_wms_dict_data_seed;
drop temporary table if exists tmp_inventory_auto_wms_dict_type_seed;
drop temporary table if exists tmp_inventory_auto_wms_sys_menu_seed;
drop procedure if exists assert_inventory_auto_wms_stock_sync_policy_completed;
drop procedure if exists assert_inventory_auto_wms_schema_ready;
drop procedure if exists assert_inventory_auto_wms_seed_targets;
drop procedure if exists assert_inventory_auto_wms_stock_sync_policy_confirmed;
drop procedure if exists assert_inventory_auto_wms_table_exists;
drop procedure if exists assert_inventory_auto_wms_menu_guard;
drop procedure if exists add_column_if_missing;
drop procedure if exists add_index_if_missing;
