-- Quote scheme value-added fee rule schema and admin permission.
-- Scope: value fee configuration only. This script does not calculate or post order fees.

set names utf8mb4;

set @confirm_quote_scheme_value_fee_rule := coalesce(@confirm_quote_scheme_value_fee_rule, '');

delimiter //

drop procedure if exists assert_quote_scheme_value_fee_rule_confirmed//
create procedure assert_quote_scheme_value_fee_rule_confirmed()
begin
  if coalesce(@confirm_quote_scheme_value_fee_rule, '')
      <> 'APPLY_QUOTE_SCHEME_VALUE_FEE_RULE' then
    signal sqlstate '45000' set message_text = 'set @confirm_quote_scheme_value_fee_rule = APPLY_QUOTE_SCHEME_VALUE_FEE_RULE before running this migration';
  end if;
end//

drop procedure if exists assert_quote_scheme_value_fee_menu_guard//
create procedure assert_quote_scheme_value_fee_menu_guard()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2053
      and parent_id = 2030
      and path = 'billing-quote-scheme'
      and menu_type = 'C'
      and component = 'Finance/QuoteScheme/index'
      and route_name = 'FinanceQuoteScheme'
      and perms = 'finance:quoteScheme:list'
  ) then
    signal sqlstate '45000' set message_text = 'quote scheme page menu 2053 must exist with final finance signature';
  end if;

  if exists (
    select 1
    from sys_menu m
    left join tmp_quote_scheme_value_fee_button_guard seed
      on seed.menu_id = m.menu_id
     and seed.parent_id = m.parent_id
     and seed.menu_type = m.menu_type
     and coalesce(seed.perms, '') = coalesce(m.perms, '')
    where m.menu_id = 2546
      and seed.menu_id is null
  ) then
    signal sqlstate '45000' set message_text = 'quote scheme value fee button menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_quote_scheme_value_fee_button_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'quote scheme value fee button permission is already used by another menu';
  end if;
end//

drop procedure if exists assert_quote_scheme_value_fee_rule_completed//
create procedure assert_quote_scheme_value_fee_rule_completed()
begin
  declare v_table_count int default 0;
  declare v_dict_type_count int default 0;
  declare v_dict_data_count int default 0;
  declare v_menu_count int default 0;

  select count(1)
    into v_table_count
  from information_schema.tables
  where table_schema = database()
    and table_name = 'quote_scheme_value_fee_rule';

  if v_table_count <> 1 then
    signal sqlstate '45000' set message_text = 'quote scheme value fee rule table was not created';
  end if;

  select count(1)
    into v_dict_type_count
  from sys_dict_type
  where status = '0'
    and dict_type in (
      'quote_scheme_value_fee_trigger',
      'quote_scheme_value_fee_calc_method',
      'quote_scheme_value_fee_direction'
    );

  if v_dict_type_count <> 3 then
    signal sqlstate '45000' set message_text = 'quote scheme value fee dict types were not completed';
  end if;

  select count(1)
    into v_dict_data_count
  from sys_dict_data
  where status = '0'
    and (
      (dict_type = 'quote_scheme_value_fee_trigger' and dict_value = 'ORDER_CANCELLED')
      or (dict_type = 'quote_scheme_value_fee_calc_method' and dict_value in ('PERCENT', 'FIXED_AMOUNT'))
      or (dict_type = 'quote_scheme_value_fee_direction' and dict_value in ('INCREASE', 'DECREASE'))
    );

  if v_dict_data_count <> 5 then
    signal sqlstate '45000' set message_text = 'quote scheme value fee dict data was not completed';
  end if;

  select count(1)
    into v_menu_count
  from sys_menu
  where menu_id = 2546
    and parent_id = 2053
    and menu_type = 'F'
    and perms = 'finance:quoteScheme:valueFee';

  if v_menu_count <> 1 then
    signal sqlstate '45000' set message_text = 'quote scheme value fee permission final state mismatch';
  end if;
end//

delimiter ;

call assert_quote_scheme_value_fee_rule_confirmed();
drop procedure if exists assert_quote_scheme_value_fee_rule_confirmed;

create table if not exists quote_scheme_value_fee_rule (
  value_fee_rule_id                bigint(20)    not null auto_increment comment '增值费规则ID',
  scheme_id                        bigint(20)    not null                comment '报价方案ID',
  logistics_channel_code           varchar(64)   not null                comment '物流渠道编码',
  logistics_channel_name_snapshot  varchar(200)  not null                comment '物流渠道名称快照',
  trigger_code                     varchar(32)   not null                comment '触发情况',
  calculation_method               varchar(32)   not null                comment '收费方式',
  adjustment_direction             varchar(16)   not null default 'INCREASE' comment '调整方向',
  adjustment_value                 decimal(18,6) not null                comment '调整值',
  status                           varchar(16)   not null default 'ENABLED' comment '状态',
  display_order                    int           not null default 0      comment '显示顺序',
  create_by                        varchar(64)   default ''              comment '创建者',
  create_time                      datetime                              comment '创建时间',
  update_by                        varchar(64)   default ''              comment '更新者',
  update_time                      datetime                              comment '更新时间',
  remark                           varchar(500)  default ''              comment '备注',
  primary key (value_fee_rule_id),
  unique key uk_quote_scheme_value_fee_rule (scheme_id, logistics_channel_code, trigger_code),
  key idx_quote_scheme_value_fee_status (scheme_id, status),
  key idx_quote_scheme_value_fee_trigger (scheme_id, trigger_code, status),
  key idx_quote_scheme_value_fee_channel (logistics_channel_code, trigger_code, status)
) engine=innodb comment='报价方案增值费规则表';

insert ignore into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark) values
('报价方案增值费触发情况', 'quote_scheme_value_fee_trigger', '0', 'admin', sysdate(), '报价方案增值费触发情况'),
('报价方案增值费收费方式', 'quote_scheme_value_fee_calc_method', '0', 'admin', sysdate(), '报价方案增值费收费方式'),
('报价方案增值费调整方向', 'quote_scheme_value_fee_direction', '0', 'admin', sysdate(), '报价方案增值费调整方向');

create temporary table if not exists tmp_quote_scheme_value_fee_dict_data_seed (
  dict_sort  int not null,
  dict_label varchar(100) not null,
  dict_value varchar(100) not null,
  dict_type  varchar(100) not null,
  list_class varchar(100) default '',
  is_default char(1) default 'N',
  status     char(1) default '0',
  remark     varchar(500) default '',
  primary key (dict_type, dict_value)
);

truncate table tmp_quote_scheme_value_fee_dict_data_seed;
insert into tmp_quote_scheme_value_fee_dict_data_seed(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, remark) values
(1, '取消订单', 'ORDER_CANCELLED', 'quote_scheme_value_fee_trigger', 'warning', 'Y', '0', ''),
(1, '按百分比调整', 'PERCENT', 'quote_scheme_value_fee_calc_method', 'primary', 'Y', '0', ''),
(2, '固定金额', 'FIXED_AMOUNT', 'quote_scheme_value_fee_calc_method', 'success', 'N', '0', ''),
(1, '加收', 'INCREASE', 'quote_scheme_value_fee_direction', 'success', 'Y', '0', ''),
(2, '减免', 'DECREASE', 'quote_scheme_value_fee_direction', 'warning', 'N', '0', '');

update sys_dict_data d
join tmp_quote_scheme_value_fee_dict_data_seed seed
  on seed.dict_type = d.dict_type
 and seed.dict_value = d.dict_value
set d.dict_sort = seed.dict_sort,
    d.dict_label = seed.dict_label,
    d.list_class = seed.list_class,
    d.is_default = seed.is_default,
    d.status = seed.status,
    d.update_by = 'admin',
    d.update_time = sysdate(),
    d.remark = seed.remark;

insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, '', seed.list_class,
       seed.is_default, seed.status, 'admin', sysdate(), seed.remark
from tmp_quote_scheme_value_fee_dict_data_seed seed
where not exists (
  select 1
  from sys_dict_data d
  where d.dict_type = seed.dict_type
    and d.dict_value = seed.dict_value
);

create temporary table if not exists tmp_quote_scheme_value_fee_button_guard (
  menu_id   bigint(20) not null primary key,
  parent_id bigint(20) not null,
  menu_type char(1) not null,
  perms     varchar(100) not null
);

truncate table tmp_quote_scheme_value_fee_button_guard;
insert into tmp_quote_scheme_value_fee_button_guard(menu_id, parent_id, menu_type, perms) values
(2546, 2053, 'F', 'finance:quoteScheme:valueFee');

call assert_quote_scheme_value_fee_menu_guard();

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
(2546, '报价方案增值费', 2053, 7, '#', '', '', '', 1, 0, 'F', '0', '0', 'finance:quoteScheme:valueFee', '#', 'admin', sysdate(), '报价方案增值费按钮')
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

call assert_quote_scheme_value_fee_rule_completed();

drop temporary table if exists tmp_quote_scheme_value_fee_button_guard;
drop temporary table if exists tmp_quote_scheme_value_fee_dict_data_seed;
drop procedure if exists assert_quote_scheme_value_fee_menu_guard;
drop procedure if exists assert_quote_scheme_value_fee_rule_completed;
