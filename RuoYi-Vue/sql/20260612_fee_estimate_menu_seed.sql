-- Fee estimate menu seed.
-- Scope: finance admin menu entry and permissions only; no business table is created.
-- Run after top_menu_seed.sql and business_menu_seed.sql.

set names utf8mb4;

set @confirm_fee_estimate_menu_seed := coalesce(@confirm_fee_estimate_menu_seed, '');

delimiter //

drop procedure if exists assert_fee_estimate_menu_seed_confirmed//
create procedure assert_fee_estimate_menu_seed_confirmed()
begin
  if coalesce(@confirm_fee_estimate_menu_seed, '')
      <> 'APPLY_FEE_ESTIMATE_MENU_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_fee_estimate_menu_seed = APPLY_FEE_ESTIMATE_MENU_SEED before running this seed';
  end if;
end//

drop procedure if exists assert_fee_estimate_finance_parent//
create procedure assert_fee_estimate_finance_parent()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2050
      and menu_type = 'M'
      and path = 'finance'
  ) then
    signal sqlstate '45000' set message_text = 'finance parent menu 2050 is missing or has unexpected signature';
  end if;
end//

drop procedure if exists assert_fee_estimate_sys_menu_guard//
create procedure assert_fee_estimate_sys_menu_guard()
begin
  if exists (
    select 1
    from sys_menu m
    join tmp_fee_estimate_sys_menu_guard seed
      on seed.menu_id = m.menu_id
    where not (
          m.parent_id = seed.parent_id
      and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')
      and coalesce(m.path, '') = coalesce(seed.path, '')
      and coalesce(m.component, '') = coalesce(seed.component, '')
      and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
      and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'fee estimate sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_fee_estimate_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'fee estimate sys_menu signature is already used by another menu';
  end if;
end//

drop procedure if exists assert_fee_estimate_menu_seed_completed//
create procedure assert_fee_estimate_menu_seed_completed()
begin
  declare v_menu_count int default 0;

  select count(1)
    into v_menu_count
  from sys_menu
  where (menu_id = 2550 and parent_id = 2050 and menu_type = 'C'
         and path = 'fee-estimate' and component = 'Finance/FeeEstimate/index'
         and route_name = 'FinanceFeeEstimate' and perms = 'finance:feeEstimate:list'
         and order_num = 1)
     or (menu_id = 2551 and parent_id = 2550 and menu_type = 'F'
         and perms = 'finance:feeEstimate:query')
     or (menu_id = 2552 and parent_id = 2550 and menu_type = 'F'
         and perms = 'finance:feeEstimate:calculate');

  if v_menu_count <> 3 then
    signal sqlstate '45000' set message_text = 'fee estimate menu seed did not complete expected sys_menu state';
  end if;
end//

delimiter ;

call assert_fee_estimate_menu_seed_confirmed();
drop procedure if exists assert_fee_estimate_menu_seed_confirmed;

call assert_fee_estimate_finance_parent();
drop procedure if exists assert_fee_estimate_finance_parent;

create temporary table if not exists tmp_fee_estimate_sys_menu_guard (
  menu_id    bigint(20) not null primary key,
  parent_id  bigint(20) not null,
  menu_type  char(1) not null,
  path       varchar(200) not null,
  component  varchar(255) default '',
  route_name varchar(50) default '',
  perms      varchar(100) default ''
);

truncate table tmp_fee_estimate_sys_menu_guard;
insert into tmp_fee_estimate_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
(2550, 2050, 'C', 'fee-estimate', 'Finance/FeeEstimate/index', 'FinanceFeeEstimate', 'finance:feeEstimate:list'),
(2551, 2550, 'F', '#', '', '', 'finance:feeEstimate:query'),
(2552, 2550, 'F', '#', '', '', 'finance:feeEstimate:calculate');

call assert_fee_estimate_sys_menu_guard();

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
(2550, '费用试算', 2050, 1, 'fee-estimate', 'Finance/FeeEstimate/index', '', 'FinanceFeeEstimate',
 1, 0, 'C', '0', '0', 'finance:feeEstimate:list', 'CalculatorOutlined', 'admin', sysdate(), '财务管理菜单：费用试算'),
(2551, '费用试算查询', 2550, 1, '#', '', '', '',
 1, 0, 'F', '0', '0', 'finance:feeEstimate:query', '#', 'admin', sysdate(), '费用试算查询按钮'),
(2552, '费用试算计算', 2550, 2, '#', '', '', '',
 1, 0, 'F', '0', '0', 'finance:feeEstimate:calculate', '#', 'admin', sysdate(), '费用试算计算按钮')
on duplicate key update
  menu_name = values(menu_name),
  parent_id = values(parent_id),
  order_num = values(order_num),
  path = values(path),
  component = values(component),
  route_name = values(route_name),
  menu_type = values(menu_type),
  visible = values(visible),
  status = values(status),
  perms = values(perms),
  icon = values(icon),
  update_by = 'admin',
  update_time = sysdate(),
  remark = values(remark);

call assert_fee_estimate_menu_seed_completed();

drop temporary table if exists tmp_fee_estimate_sys_menu_guard;
drop procedure if exists assert_fee_estimate_sys_menu_guard;
drop procedure if exists assert_fee_estimate_menu_seed_completed;
