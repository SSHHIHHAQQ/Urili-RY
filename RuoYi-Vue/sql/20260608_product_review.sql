-- Product review workflow tables, permissions, and admin menu component.
-- Scope: product_review_* tables, review dictionaries, review center menu 2451 component, and button permissions.
-- This script is prepared for review; confirm active datasource before executing.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_product_review := coalesce(@confirm_product_review, '');
set @product_review_menu_expected_count := coalesce(@product_review_menu_expected_count, '');
set @product_review_menu_expected_signature := coalesce(@product_review_menu_expected_signature, '');
set @product_review_dict_type_expected_count := coalesce(@product_review_dict_type_expected_count, '');
set @product_review_dict_type_expected_signature := coalesce(@product_review_dict_type_expected_signature, '');
set @product_review_dict_data_expected_count := coalesce(@product_review_dict_data_expected_count, '');
set @product_review_dict_data_expected_signature := coalesce(@product_review_dict_data_expected_signature, '');

delimiter //

drop procedure if exists assert_product_review_confirmed//
create procedure assert_product_review_confirmed()
begin
  if coalesce(@confirm_product_review, '') <> 'APPLY_PRODUCT_REVIEW' then
    signal sqlstate '45000' set message_text = 'set @confirm_product_review = APPLY_PRODUCT_REVIEW before running this migration';
  end if;
  if coalesce(@product_review_menu_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @product_review_menu_expected_count after previewing exact product review sys_menu rows';
  end if;
  if coalesce(@product_review_menu_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @product_review_menu_expected_signature after previewing exact product review sys_menu rows';
  end if;
  if coalesce(@product_review_dict_type_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @product_review_dict_type_expected_count after previewing exact product review sys_dict_type rows';
  end if;
  if coalesce(@product_review_dict_type_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @product_review_dict_type_expected_signature after previewing exact product review sys_dict_type rows';
  end if;
  if coalesce(@product_review_dict_data_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @product_review_dict_data_expected_count after previewing exact product review sys_dict_data rows';
  end if;
  if coalesce(@product_review_dict_data_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @product_review_dict_data_expected_signature after previewing exact product review sys_dict_data rows';
  end if;
end//

drop procedure if exists assert_product_review_sys_menu_guard//
create procedure assert_product_review_sys_menu_guard()
begin
  if not exists (
    select 1
    from sys_menu m
    join tmp_product_review_sys_menu_guard seed
      on seed.menu_id = 2451
     and m.menu_id = seed.menu_id
    where coalesce(m.parent_id, -1) = seed.parent_id
      and coalesce(m.menu_type, '') = seed.menu_type
      and coalesce(m.path, '') = coalesce(seed.path, '')
      and coalesce(m.component, '') in (coalesce(seed.component, ''), 'Common/PlannedPage/index')
      and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
      and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'product review page menu 2451 must exist with expected signature before product review migration';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_product_review_sys_menu_guard seed on seed.menu_id = m.menu_id
    where not (
      m.parent_id = seed.parent_id
      and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')
      and coalesce(m.path, '') = coalesce(seed.path, '')
      and coalesce(m.component, '') in (coalesce(seed.component, ''), 'Common/PlannedPage/index')
      and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
      and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'product review sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_product_review_sys_menu_guard seed
      on coalesce(seed.perms, '') <> ''
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
     and m.menu_id <> seed.menu_id
  ) then
    signal sqlstate '45000' set message_text = 'product review sys_menu permission is already used by another menu';
  end if;
end//

drop procedure if exists assert_product_review_schema_ready//
create procedure assert_product_review_schema_ready()
begin
  if exists (
    select 1
    from tmp_product_review_column_contract expected
    left join information_schema.columns c
      on c.table_schema = database()
     and c.table_name = expected.table_name
     and c.column_name = expected.column_name
     and c.ordinal_position = expected.ordinal_position
     and lower(c.column_type) = lower(expected.column_type)
     and c.is_nullable = expected.is_nullable
     and (
       expected.column_default is null and c.column_default is null
       or expected.column_default = 'current_timestamp'
          and lower(coalesce(c.column_default, '')) in ('current_timestamp', 'current_timestamp()')
       or coalesce(c.column_default, '') = coalesce(expected.column_default, '')
     )
     and (
       lower(coalesce(c.extra, '')) = lower(expected.extra)
       or expected.column_default = 'current_timestamp'
          and lower(coalesce(c.extra, '')) in ('', 'default_generated')
     )
    where c.column_name is null
  ) or (
    select count(1)
    from information_schema.columns c
    where c.table_schema = database()
      and c.table_name in (select distinct table_name from tmp_product_review_column_contract)
  ) <> (select count(1) from tmp_product_review_column_contract) then
    signal sqlstate '45000' set message_text = 'product_review schema column contract mismatch';
  end if;

  if exists (
    select 1
    from tmp_product_review_index_contract expected
    left join information_schema.statistics s
      on s.table_schema = database()
     and s.table_name = expected.table_name
     and s.index_name = expected.index_name
     and s.non_unique = expected.non_unique
     and s.seq_in_index = expected.seq_in_index
     and s.column_name = expected.column_name
    where s.index_name is null
  ) or (
    select count(1)
    from information_schema.statistics s
    where s.table_schema = database()
      and s.table_name in (select distinct table_name from tmp_product_review_index_contract)
  ) <> (select count(1) from tmp_product_review_index_contract) then
    signal sqlstate '45000' set message_text = 'product_review schema index contract mismatch';
  end if;
end//

drop procedure if exists assert_product_review_seed_targets//
create procedure assert_product_review_seed_targets()
begin
  if exists (
    select 1
    from sys_dict_type t
    join tmp_product_review_dict_type_seed seed on seed.dict_type = t.dict_type
    where coalesce(t.dict_name, '') <> seed.dict_name
       or coalesce(t.status, '') <> '0'
  ) then
    signal sqlstate '45000' set message_text = 'product review dict type target is occupied by incompatible row';
  end if;

  if exists (
    select 1
    from sys_dict_data d
    join tmp_product_review_dict_data_seed seed
      on seed.dict_type = d.dict_type
     and seed.dict_value = d.dict_value
    where coalesce(d.dict_label, '') <> seed.dict_label
       or coalesce(d.list_class, '') <> seed.list_class
       or coalesce(d.is_default, '') <> seed.is_default
       or coalesce(d.status, '') <> '0'
  ) then
    signal sqlstate '45000' set message_text = 'product review dict data target is occupied by incompatible row';
  end if;
end//

drop procedure if exists assert_product_review_seed_target_signatures//
create procedure assert_product_review_seed_target_signatures()
begin
  declare v_menu_count bigint default 0;
  declare v_menu_signature varchar(64) default '';
  declare v_dict_type_count bigint default 0;
  declare v_dict_type_signature varchar(64) default '';
  declare v_dict_data_count bigint default 0;
  declare v_dict_data_signature varchar(64) default '';

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
  join tmp_product_review_sys_menu_guard seed
    on m.menu_id = seed.menu_id
    or (coalesce(seed.perms, '') <> '' and coalesce(m.perms, '') = coalesce(seed.perms, ''));

  if v_menu_count <> cast(@product_review_menu_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'product review sys_menu exact target count mismatch';
  end if;
  if lower(v_menu_signature) <> lower(@product_review_menu_expected_signature) then
    signal sqlstate '45000' set message_text = 'product review sys_menu exact target signature mismatch';
  end if;

  select count(distinct t.dict_id),
         sha2(coalesce(group_concat(distinct
           concat_ws('|',
             t.dict_id,
             coalesce(t.dict_name, ''),
             coalesce(t.dict_type, ''),
             coalesce(t.status, ''),
             coalesce(t.remark, '')
           )
           order by t.dict_id separator '\n'
         ), ''), 256)
    into v_dict_type_count, v_dict_type_signature
  from sys_dict_type t
  join tmp_product_review_dict_type_seed seed on seed.dict_type = t.dict_type;

  if v_dict_type_count <> cast(@product_review_dict_type_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'product review sys_dict_type exact target count mismatch';
  end if;
  if lower(v_dict_type_signature) <> lower(@product_review_dict_type_expected_signature) then
    signal sqlstate '45000' set message_text = 'product review sys_dict_type exact target signature mismatch';
  end if;

  select count(distinct d.dict_code),
         sha2(coalesce(group_concat(distinct
           concat_ws('|',
             d.dict_code,
             coalesce(d.dict_sort, ''),
             coalesce(d.dict_label, ''),
             coalesce(d.dict_value, ''),
             coalesce(d.dict_type, ''),
             coalesce(d.css_class, ''),
             coalesce(d.list_class, ''),
             coalesce(d.is_default, ''),
             coalesce(d.status, ''),
             coalesce(d.remark, '')
           )
           order by d.dict_code separator '\n'
         ), ''), 256)
    into v_dict_data_count, v_dict_data_signature
  from sys_dict_data d
  join tmp_product_review_dict_data_seed seed
    on seed.dict_type = d.dict_type
   and seed.dict_value = d.dict_value;

  if v_dict_data_count <> cast(@product_review_dict_data_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'product review sys_dict_data exact target count mismatch';
  end if;
  if lower(v_dict_data_signature) <> lower(@product_review_dict_data_expected_signature) then
    signal sqlstate '45000' set message_text = 'product review sys_dict_data exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_product_review_seed_completed//
create procedure assert_product_review_seed_completed()
begin
  if (
    select count(1)
    from sys_dict_type t
    join tmp_product_review_dict_type_seed seed on seed.dict_type = t.dict_type
    where coalesce(t.dict_name, '') = seed.dict_name
      and coalesce(t.status, '') = '0'
  ) <> (select count(1) from tmp_product_review_dict_type_seed) then
    signal sqlstate '45000' set message_text = 'product review dict type seed completion mismatch';
  end if;

  if (
    select count(1)
    from sys_dict_data d
    join tmp_product_review_dict_data_seed seed
      on seed.dict_type = d.dict_type
     and seed.dict_value = d.dict_value
    where coalesce(d.dict_label, '') = seed.dict_label
      and coalesce(d.list_class, '') = seed.list_class
      and coalesce(d.is_default, '') = seed.is_default
      and coalesce(d.status, '') = '0'
  ) <> (select count(1) from tmp_product_review_dict_data_seed) then
    signal sqlstate '45000' set message_text = 'product review dict data seed completion mismatch';
  end if;

  if (
    select count(1)
    from sys_menu m
    join tmp_product_review_sys_menu_guard seed on seed.menu_id = m.menu_id
    where m.parent_id = seed.parent_id
      and coalesce(m.menu_type, '') = seed.menu_type
      and coalesce(m.path, '') = seed.path
      and coalesce(m.component, '') = seed.component
      and coalesce(m.route_name, '') = seed.route_name
      and coalesce(m.perms, '') = seed.perms
  ) <> (select count(1) from tmp_product_review_sys_menu_guard) then
    signal sqlstate '45000' set message_text = 'product review sys_menu seed completion mismatch';
  end if;
end//

delimiter ;

call assert_product_review_confirmed();
drop procedure if exists assert_product_review_confirmed;

create temporary table if not exists tmp_product_review_sys_menu_guard (
  menu_id    bigint       not null,
  parent_id  bigint       not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  key idx_product_review_sys_menu_guard_id (menu_id)
) engine=memory;

truncate table tmp_product_review_sys_menu_guard;

insert into tmp_product_review_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (2451, 2100, 'C', 'product-distribution', 'Product/Review/index', 'ProductDistributionReview', 'review:productDistribution:list'),
    (2491, 2451, 'F', '#', '', '', 'review:productDistribution:query'),
    (2492, 2451, 'F', '#', '', '', 'review:productDistribution:approve'),
    (2493, 2451, 'F', '#', '', '', 'review:productDistribution:reject'),
    (2494, 2451, 'F', '#', '', '', 'review:productDistribution:log');

call assert_product_review_sys_menu_guard();

create temporary table if not exists tmp_product_review_dict_type_seed (
  dict_name varchar(100) not null,
  dict_type varchar(100) not null,
  key idx_product_review_dict_type_seed (dict_type)
) engine=memory;

truncate table tmp_product_review_dict_type_seed;

insert into tmp_product_review_dict_type_seed(dict_name, dict_type) values
    ('商品审核类型', 'product_review_type'),
    ('商品审核状态', 'product_review_status'),
    ('商品审核风险等级', 'product_review_risk_level');

create temporary table if not exists tmp_product_review_dict_data_seed (
  dict_sort  int          not null,
  dict_label varchar(100) not null,
  dict_value varchar(100) not null,
  dict_type  varchar(100) not null,
  list_class varchar(100) not null,
  is_default char(1)      not null,
  key idx_product_review_dict_data_seed (dict_type, dict_value)
) engine=memory;

truncate table tmp_product_review_dict_data_seed;

insert into tmp_product_review_dict_data_seed(dict_sort, dict_label, dict_value, dict_type, list_class, is_default) values
    (1, '新增商品', 'NEW_PRODUCT', 'product_review_type', 'primary', 'Y'),
    (2, '新增SKU', 'ADD_SKU', 'product_review_type', 'processing', 'N'),
    (3, '商品资料变更', 'EDIT_PRODUCT_INFO', 'product_review_type', 'warning', 'N'),
    (4, 'SKU资料变更', 'EDIT_SKU_INFO', 'product_review_type', 'warning', 'N'),
    (5, '价格变更', 'EDIT_PRICE', 'product_review_type', 'danger', 'N'),
    (1, '待审核', 'PENDING', 'product_review_status', 'warning', 'Y'),
    (2, '已通过', 'APPROVED', 'product_review_status', 'success', 'N'),
    (3, '已驳回', 'REJECTED', 'product_review_status', 'danger', 'N'),
    (4, '已撤回', 'WITHDRAWN', 'product_review_status', 'info', 'N'),
    (1, '低风险', 'LOW', 'product_review_risk_level', 'success', 'Y'),
    (2, '中风险', 'MEDIUM', 'product_review_risk_level', 'warning', 'N'),
    (3, '高风险', 'HIGH', 'product_review_risk_level', 'danger', 'N');

call assert_product_review_seed_targets();
call assert_product_review_seed_target_signatures();

create temporary table if not exists tmp_product_review_column_contract (
  table_name       varchar(64)  not null,
  column_name      varchar(64)  not null,
  ordinal_position int          not null,
  column_type      varchar(255) not null,
  is_nullable      varchar(3)   not null,
  column_default   varchar(255) default null,
  extra            varchar(255) not null default '',
  key idx_product_review_column_contract (table_name, column_name)
) engine=memory;

truncate table tmp_product_review_column_contract;

insert into tmp_product_review_column_contract
    (table_name, column_name, ordinal_position, column_type, is_nullable, column_default, extra)
values
    ('product_review_request', 'review_id', 1, 'bigint', 'NO', null, 'auto_increment'),
    ('product_review_request', 'review_no', 2, 'varchar(64)', 'NO', null, ''),
    ('product_review_request', 'review_type', 3, 'varchar(32)', 'NO', null, ''),
    ('product_review_request', 'review_status', 4, 'varchar(32)', 'NO', null, ''),
    ('product_review_request', 'spu_id', 5, 'bigint', 'NO', null, ''),
    ('product_review_request', 'system_spu_code', 6, 'varchar(64)', 'YES', '', ''),
    ('product_review_request', 'seller_id', 7, 'bigint', 'NO', null, ''),
    ('product_review_request', 'seller_name', 8, 'varchar(255)', 'YES', '', ''),
    ('product_review_request', 'category_id', 9, 'bigint', 'YES', null, ''),
    ('product_review_request', 'category_name', 10, 'varchar(255)', 'YES', '', ''),
    ('product_review_request', 'product_name_before', 11, 'varchar(255)', 'YES', '', ''),
    ('product_review_request', 'product_name_after', 12, 'varchar(255)', 'YES', '', ''),
    ('product_review_request', 'main_image_url_before', 13, 'varchar(1000)', 'YES', '', ''),
    ('product_review_request', 'main_image_url_after', 14, 'varchar(1000)', 'YES', '', ''),
    ('product_review_request', 'submit_terminal', 15, 'varchar(16)', 'NO', null, ''),
    ('product_review_request', 'submit_subject_id', 16, 'bigint', 'YES', null, ''),
    ('product_review_request', 'submit_account_id', 17, 'bigint', 'YES', null, ''),
    ('product_review_request', 'submit_user_name', 18, 'varchar(64)', 'NO', null, ''),
    ('product_review_request', 'submit_time', 19, 'datetime', 'NO', null, ''),
    ('product_review_request', 'reviewer_id', 20, 'bigint', 'YES', null, ''),
    ('product_review_request', 'reviewer_name', 21, 'varchar(64)', 'YES', '', ''),
    ('product_review_request', 'review_time', 22, 'datetime', 'YES', null, ''),
    ('product_review_request', 'review_reason', 23, 'varchar(500)', 'YES', '', ''),
    ('product_review_request', 'risk_level', 24, 'varchar(16)', 'YES', 'LOW', ''),
    ('product_review_request', 'risk_summary', 25, 'varchar(1000)', 'YES', '', ''),
    ('product_review_request', 'item_count', 26, 'int', 'NO', '0', ''),
    ('product_review_request', 'sku_count', 27, 'int', 'NO', '0', ''),
    ('product_review_request', 'price_before_min', 28, 'decimal(18,4)', 'YES', null, ''),
    ('product_review_request', 'price_before_max', 29, 'decimal(18,4)', 'YES', null, ''),
    ('product_review_request', 'price_after_min', 30, 'decimal(18,4)', 'YES', null, ''),
    ('product_review_request', 'price_after_max', 31, 'decimal(18,4)', 'YES', null, ''),
    ('product_review_request', 'currency_summary', 32, 'varchar(64)', 'YES', '', ''),
    ('product_review_request', 'warehouse_summary', 33, 'varchar(500)', 'YES', '', ''),
    ('product_review_request', 'diff_summary', 34, 'varchar(1000)', 'YES', '', ''),
    ('product_review_request', 'active_pending_key', 35, 'varchar(128)', 'NO', null, ''),
    ('product_review_request', 'del_flag', 36, 'char(1)', 'NO', '0', ''),
    ('product_review_request', 'create_by', 37, 'varchar(64)', 'YES', '', ''),
    ('product_review_request', 'create_time', 38, 'datetime', 'YES', null, ''),
    ('product_review_request', 'update_by', 39, 'varchar(64)', 'YES', '', ''),
    ('product_review_request', 'update_time', 40, 'datetime', 'YES', null, ''),
    ('product_review_request', 'remark', 41, 'varchar(500)', 'YES', '', ''),
    ('product_review_item', 'item_id', 1, 'bigint', 'NO', null, 'auto_increment'),
    ('product_review_item', 'review_id', 2, 'bigint', 'NO', null, ''),
    ('product_review_item', 'item_type', 3, 'varchar(16)', 'NO', null, ''),
    ('product_review_item', 'change_type', 4, 'varchar(32)', 'NO', null, ''),
    ('product_review_item', 'spu_id', 5, 'bigint', 'NO', null, ''),
    ('product_review_item', 'sku_id', 6, 'bigint', 'YES', null, ''),
    ('product_review_item', 'system_sku_code', 7, 'varchar(64)', 'YES', '', ''),
    ('product_review_item', 'seller_sku_code', 8, 'varchar(128)', 'YES', '', ''),
    ('product_review_item', 'item_status', 9, 'varchar(32)', 'NO', null, ''),
    ('product_review_item', 'before_hash', 10, 'varchar(128)', 'YES', '', ''),
    ('product_review_item', 'after_hash', 11, 'varchar(128)', 'YES', '', ''),
    ('product_review_item', 'diff_summary', 12, 'varchar(1000)', 'YES', '', ''),
    ('product_review_item', 'risk_summary', 13, 'varchar(1000)', 'YES', '', ''),
    ('product_review_item', 'sort_order', 14, 'int', 'NO', '0', ''),
    ('product_review_item', 'create_time', 15, 'datetime', 'YES', null, ''),
    ('product_review_snapshot', 'snapshot_id', 1, 'bigint', 'NO', null, 'auto_increment'),
    ('product_review_snapshot', 'review_id', 2, 'bigint', 'NO', null, ''),
    ('product_review_snapshot', 'item_id', 3, 'bigint', 'YES', null, ''),
    ('product_review_snapshot', 'snapshot_role', 4, 'varchar(16)', 'NO', null, ''),
    ('product_review_snapshot', 'payload_type', 5, 'varchar(32)', 'NO', null, ''),
    ('product_review_snapshot', 'payload_json', 6, 'longtext', 'YES', null, ''),
    ('product_review_snapshot', 'payload_hash', 7, 'varchar(128)', 'YES', '', ''),
    ('product_review_snapshot', 'create_time', 8, 'datetime', 'YES', null, ''),
    ('product_review_operation_log', 'log_id', 1, 'bigint', 'NO', null, 'auto_increment'),
    ('product_review_operation_log', 'review_id', 2, 'bigint', 'NO', null, ''),
    ('product_review_operation_log', 'spu_id', 3, 'bigint', 'NO', null, ''),
    ('product_review_operation_log', 'operation_type', 4, 'varchar(32)', 'NO', null, ''),
    ('product_review_operation_log', 'before_status', 5, 'varchar(32)', 'YES', '', ''),
    ('product_review_operation_log', 'after_status', 6, 'varchar(32)', 'NO', null, ''),
    ('product_review_operation_log', 'operator_terminal', 7, 'varchar(16)', 'NO', null, ''),
    ('product_review_operation_log', 'operator_id', 8, 'bigint', 'YES', null, ''),
    ('product_review_operation_log', 'operator_name', 9, 'varchar(64)', 'NO', null, ''),
    ('product_review_operation_log', 'operation_time', 10, 'datetime', 'NO', 'current_timestamp', ''),
    ('product_review_operation_log', 'reason', 11, 'varchar(500)', 'YES', '', ''),
    ('product_review_operation_log', 'remark', 12, 'varchar(500)', 'YES', '', '');

create temporary table if not exists tmp_product_review_index_contract (
  table_name   varchar(64) not null,
  index_name   varchar(64) not null,
  non_unique   int         not null,
  seq_in_index int         not null,
  column_name  varchar(64) not null,
  key idx_product_review_index_contract (table_name, index_name, seq_in_index)
) engine=memory;

truncate table tmp_product_review_index_contract;

insert into tmp_product_review_index_contract
    (table_name, index_name, non_unique, seq_in_index, column_name)
values
    ('product_review_request', 'PRIMARY', 0, 1, 'review_id'),
    ('product_review_request', 'uk_product_review_no', 0, 1, 'review_no'),
    ('product_review_request', 'idx_product_review_type_status', 1, 1, 'review_type'),
    ('product_review_request', 'idx_product_review_type_status', 1, 2, 'review_status'),
    ('product_review_request', 'idx_product_review_type_status', 1, 3, 'submit_time'),
    ('product_review_request', 'idx_product_review_status_time', 1, 1, 'review_status'),
    ('product_review_request', 'idx_product_review_status_time', 1, 2, 'submit_time'),
    ('product_review_request', 'idx_product_review_spu', 1, 1, 'spu_id'),
    ('product_review_request', 'idx_product_review_spu', 1, 2, 'submit_time'),
    ('product_review_request', 'idx_product_review_seller', 1, 1, 'seller_id'),
    ('product_review_request', 'idx_product_review_seller', 1, 2, 'submit_time'),
    ('product_review_request', 'idx_product_review_submit', 1, 1, 'submit_terminal'),
    ('product_review_request', 'idx_product_review_submit', 1, 2, 'submit_time'),
    ('product_review_request', 'idx_product_review_pending_key', 1, 1, 'active_pending_key'),
    ('product_review_request', 'idx_product_review_pending_key', 1, 2, 'review_status'),
    ('product_review_item', 'PRIMARY', 0, 1, 'item_id'),
    ('product_review_item', 'idx_product_review_item_review', 1, 1, 'review_id'),
    ('product_review_item', 'idx_product_review_item_review', 1, 2, 'sort_order'),
    ('product_review_item', 'idx_product_review_item_spu', 1, 1, 'spu_id'),
    ('product_review_item', 'idx_product_review_item_spu', 1, 2, 'review_id'),
    ('product_review_item', 'idx_product_review_item_sku', 1, 1, 'sku_id'),
    ('product_review_item', 'idx_product_review_item_sku', 1, 2, 'review_id'),
    ('product_review_snapshot', 'PRIMARY', 0, 1, 'snapshot_id'),
    ('product_review_snapshot', 'idx_product_review_snapshot_review', 1, 1, 'review_id'),
    ('product_review_snapshot', 'idx_product_review_snapshot_review', 1, 2, 'snapshot_role'),
    ('product_review_snapshot', 'idx_product_review_snapshot_item', 1, 1, 'item_id'),
    ('product_review_snapshot', 'idx_product_review_snapshot_item', 1, 2, 'snapshot_role'),
    ('product_review_operation_log', 'PRIMARY', 0, 1, 'log_id'),
    ('product_review_operation_log', 'idx_product_review_log_review', 1, 1, 'review_id'),
    ('product_review_operation_log', 'idx_product_review_log_review', 1, 2, 'operation_time'),
    ('product_review_operation_log', 'idx_product_review_log_spu', 1, 1, 'spu_id'),
    ('product_review_operation_log', 'idx_product_review_log_spu', 1, 2, 'operation_time'),
    ('product_review_operation_log', 'idx_product_review_log_type', 1, 1, 'operation_type'),
    ('product_review_operation_log', 'idx_product_review_log_type', 1, 2, 'operation_time');

create table if not exists product_review_request (
  review_id              bigint        not null auto_increment comment '审核单主键',
  review_no              varchar(64)   not null                comment '审核单号',
  review_type            varchar(32)   not null                comment '审核类型',
  review_status          varchar(32)   not null                comment '审核状态',
  spu_id                 bigint        not null                comment '商品SPU ID',
  system_spu_code        varchar(64)   default ''              comment '系统SPU编码快照',
  seller_id              bigint        not null                comment '卖家ID快照',
  seller_name            varchar(255)  default ''              comment '卖家名称快照',
  category_id            bigint        default null            comment '类目ID快照',
  category_name          varchar(255)  default ''              comment '类目名称快照',
  product_name_before    varchar(255)  default ''              comment '变更前商品标题快照',
  product_name_after     varchar(255)  default ''              comment '变更后商品标题快照',
  main_image_url_before  varchar(1000) default ''              comment '变更前主图快照',
  main_image_url_after   varchar(1000) default ''              comment '变更后主图快照',
  submit_terminal        varchar(16)   not null                comment '提交端：ADMIN/SELLER',
  submit_subject_id      bigint        default null            comment '提交主体ID',
  submit_account_id      bigint        default null            comment '提交账号ID',
  submit_user_name       varchar(64)   not null                comment '提交人',
  submit_time            datetime      not null                comment '提交时间',
  reviewer_id            bigint        default null            comment '审核人管理端用户ID',
  reviewer_name          varchar(64)   default ''              comment '审核人账号',
  review_time            datetime      default null            comment '审核时间',
  review_reason          varchar(500)  default ''              comment '通过说明或驳回原因',
  risk_level             varchar(16)   default 'LOW'           comment '风险等级',
  risk_summary           varchar(1000) default ''              comment '风险标签摘要',
  item_count             int           not null default 0      comment '影响对象数量',
  sku_count              int           not null default 0      comment '涉及SKU数',
  price_before_min       decimal(18,4) default null            comment '变更前最低价',
  price_before_max       decimal(18,4) default null            comment '变更前最高价',
  price_after_min        decimal(18,4) default null            comment '变更后最低价',
  price_after_max        decimal(18,4) default null            comment '变更后最高价',
  currency_summary       varchar(64)   default ''              comment '币种摘要',
  warehouse_summary      varchar(500)  default ''              comment '仓库摘要',
  diff_summary           varchar(1000) default ''              comment '字段变化摘要',
  active_pending_key     varchar(128)  not null                comment '待审冲突控制key',
  del_flag               char(1)       not null default '0'    comment '删除标志：0存在 2删除',
  create_by              varchar(64)   default ''              comment '创建者',
  create_time            datetime                              comment '创建时间',
  update_by              varchar(64)   default ''              comment '更新者',
  update_time            datetime                              comment '更新时间',
  remark                 varchar(500)  default ''              comment '备注',
  primary key (review_id),
  unique key uk_product_review_no (review_no),
  key idx_product_review_type_status (review_type, review_status, submit_time),
  key idx_product_review_status_time (review_status, submit_time),
  key idx_product_review_spu (spu_id, submit_time),
  key idx_product_review_seller (seller_id, submit_time),
  key idx_product_review_submit (submit_terminal, submit_time),
  key idx_product_review_pending_key (active_pending_key, review_status)
) engine=innodb auto_increment=1 comment='商品审核单表';

create table if not exists product_review_item (
  item_id         bigint        not null auto_increment comment '审核明细主键',
  review_id       bigint        not null                comment '审核单ID',
  item_type       varchar(16)   not null                comment '对象类型：SPU/SKU',
  change_type     varchar(32)   not null                comment '变更类型：CREATE/UPDATE/PRICE_UPDATE',
  spu_id          bigint        not null                comment 'SPU ID',
  sku_id          bigint        default null            comment 'SKU ID',
  system_sku_code varchar(64)   default ''              comment '系统SKU编码快照',
  seller_sku_code varchar(128)  default ''              comment '客户SKU编码快照',
  item_status     varchar(32)   not null                comment '明细状态',
  before_hash     varchar(128)  default ''              comment '变更前快照hash',
  after_hash      varchar(128)  default ''              comment '变更后快照hash',
  diff_summary    varchar(1000) default ''              comment '本对象变化摘要',
  risk_summary    varchar(1000) default ''              comment '本对象风险摘要',
  sort_order      int           not null default 0      comment '展示排序',
  create_time     datetime                              comment '创建时间',
  primary key (item_id),
  key idx_product_review_item_review (review_id, sort_order),
  key idx_product_review_item_spu (spu_id, review_id),
  key idx_product_review_item_sku (sku_id, review_id)
) engine=innodb auto_increment=1 comment='商品审核对象明细表';

create table if not exists product_review_snapshot (
  snapshot_id   bigint       not null auto_increment comment '审核快照主键',
  review_id     bigint       not null                comment '审核单ID',
  item_id       bigint       default null            comment '审核明细ID',
  snapshot_role varchar(16)  not null                comment '快照角色：BEFORE/AFTER/DIFF',
  payload_type  varchar(32)  not null                comment '载荷类型：SPU/SKU/ATTRIBUTES/IMAGES/WAREHOUSES/PRICE',
  payload_json  longtext                             comment '快照JSON',
  payload_hash  varchar(128) default ''              comment '快照hash',
  create_time   datetime                             comment '创建时间',
  primary key (snapshot_id),
  key idx_product_review_snapshot_review (review_id, snapshot_role),
  key idx_product_review_snapshot_item (item_id, snapshot_role)
) engine=innodb auto_increment=1 comment='商品审核快照表';

create table if not exists product_review_operation_log (
  log_id            bigint       not null auto_increment comment '审核操作日志主键',
  review_id         bigint       not null                comment '审核单ID',
  spu_id            bigint       not null                comment 'SPU ID',
  operation_type    varchar(32)  not null                comment '操作类型：SUBMIT/APPROVE/REJECT/WITHDRAW',
  before_status     varchar(32)  default ''              comment '操作前审核状态',
  after_status      varchar(32)  not null                comment '操作后审核状态',
  operator_terminal varchar(16)  not null                comment '操作端',
  operator_id       bigint       default null            comment '操作账号ID',
  operator_name     varchar(64)  not null                comment '操作人',
  operation_time    datetime     not null default current_timestamp comment '操作时间',
  reason            varchar(500) default ''              comment '操作原因',
  remark            varchar(500) default ''              comment '备注',
  primary key (log_id),
  key idx_product_review_log_review (review_id, operation_time),
  key idx_product_review_log_spu (spu_id, operation_time),
  key idx_product_review_log_type (operation_type, operation_time)
) engine=innodb auto_increment=1 comment='商品审核操作日志表';

call assert_product_review_schema_ready();

start transaction;

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_name, seed.dict_type, '0', 'admin', sysdate(), '', null, concat(seed.dict_name, '字典')
from tmp_product_review_dict_type_seed seed
where not exists (select 1 from sys_dict_type t where t.dict_type = seed.dict_type);

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, '', seed.list_class, seed.is_default, '0',
       'admin', sysdate(), '', null, concat(seed.dict_label, '字典项')
from tmp_product_review_dict_data_seed seed
where not exists (select 1 from sys_dict_data d where d.dict_type = seed.dict_type and d.dict_value = seed.dict_value);

update sys_menu
set menu_name = '商品审核',
    component = 'Product/Review/index',
    perms = 'review:productDistribution:list',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '审核中心菜单：商品审核'
where menu_id = 2451;

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_id, seed.menu_name, 2451, seed.order_num, '#', '', '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select 2491 as menu_id, '商品审核查询' as menu_name, 1 as order_num, 'review:productDistribution:query' as perms, '商品审核按钮：查询详情' as remark
    union all select 2492, '商品审核通过', 2, 'review:productDistribution:approve', '商品审核按钮：审核通过'
    union all select 2493, '商品审核驳回', 3, 'review:productDistribution:reject', '商品审核按钮：审核驳回'
    union all select 2494, '商品审核日志', 4, 'review:productDistribution:log', '商品审核按钮：审核日志'
) seed
where not exists (select 1 from sys_menu m where m.menu_id = seed.menu_id)
  and not exists (select 1 from sys_menu p where p.perms = seed.perms);

call assert_product_review_seed_completed();

commit;

drop temporary table if exists tmp_product_review_dict_data_seed;
drop temporary table if exists tmp_product_review_dict_type_seed;
drop temporary table if exists tmp_product_review_index_contract;
drop temporary table if exists tmp_product_review_column_contract;
drop temporary table if exists tmp_product_review_sys_menu_guard;
drop procedure if exists assert_product_review_seed_completed;
drop procedure if exists assert_product_review_seed_target_signatures;
drop procedure if exists assert_product_review_seed_targets;
drop procedure if exists assert_product_review_schema_ready;
drop procedure if exists assert_product_review_sys_menu_guard;
