-- Inventory adjustment review workflow.
-- Scope: review policies, review requests, operation logs, SKU sales daily aggregate,
-- admin menu 2452 component/buttons, and due-review Quartz job.
-- Confirm active datasource before executing this migration.

set names utf8mb4;
set session group_concat_max_len = 1048576;

set @confirm_inventory_adjustment_review := coalesce(@confirm_inventory_adjustment_review, '');
set @inventory_adjustment_review_menu_expected_count :=
    coalesce(@inventory_adjustment_review_menu_expected_count, '');
set @inventory_adjustment_review_menu_expected_signature :=
    coalesce(@inventory_adjustment_review_menu_expected_signature, '');
set @inventory_adjustment_review_job_expected_count :=
    coalesce(@inventory_adjustment_review_job_expected_count, '');
set @inventory_adjustment_review_job_expected_signature :=
    coalesce(@inventory_adjustment_review_job_expected_signature, '');

delimiter //

drop procedure if exists assert_inventory_adjustment_review_confirmed//
create procedure assert_inventory_adjustment_review_confirmed()
begin
  if coalesce(@confirm_inventory_adjustment_review, '')
      <> 'APPLY_INVENTORY_ADJUSTMENT_REVIEW' then
    signal sqlstate '45000' set message_text = 'set @confirm_inventory_adjustment_review = APPLY_INVENTORY_ADJUSTMENT_REVIEW before running this migration';
  end if;
  if coalesce(@inventory_adjustment_review_menu_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @inventory_adjustment_review_menu_expected_count after previewing exact inventory adjustment review sys_menu rows';
  end if;
  if coalesce(@inventory_adjustment_review_menu_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @inventory_adjustment_review_menu_expected_signature after previewing exact inventory adjustment review sys_menu rows';
  end if;
  if coalesce(@inventory_adjustment_review_job_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @inventory_adjustment_review_job_expected_count after previewing exact inventory adjustment review sys_job row';
  end if;
  if coalesce(@inventory_adjustment_review_job_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @inventory_adjustment_review_job_expected_signature after previewing exact inventory adjustment review sys_job row';
  end if;
end//

drop procedure if exists assert_inventory_adjustment_review_parent_ready//
create procedure assert_inventory_adjustment_review_parent_ready()
begin
  if not exists (
    select 1
    from sys_menu
    where menu_id = 2100
      and parent_id = 0
      and menu_type = 'M'
      and path = 'review-center'
      and route_name = 'ReviewCenter'
  ) then
    signal sqlstate '45000' set message_text = 'inventory adjustment review requires review center parent 2100 from top_menu_seed.sql';
  end if;
end//

drop procedure if exists assert_inventory_adjustment_review_menu_slots//
create procedure assert_inventory_adjustment_review_menu_slots()
begin
  if exists (
    select 1
    from sys_menu m
    join tmp_inventory_adjustment_review_sys_menu seed
      on seed.menu_id = m.menu_id
    where not (
      coalesce(m.parent_id, -1) = seed.parent_id
      and coalesce(m.menu_type, '') = seed.menu_type
      and coalesce(m.path, '') = seed.path
      and (
        coalesce(m.component, '') = seed.component
        or (seed.menu_id = 2452 and coalesce(m.component, '') = 'Common/PlannedPage/index')
      )
      and coalesce(m.route_name, '') = seed.route_name
      and coalesce(m.perms, '') = seed.perms
    )
  ) then
    signal sqlstate '45000' set message_text = 'inventory adjustment review sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_inventory_adjustment_review_sys_menu seed
      on coalesce(seed.perms, '') <> ''
     and coalesce(m.perms, '') = seed.perms
     and m.menu_id <> seed.menu_id
  ) then
    signal sqlstate '45000' set message_text = 'inventory adjustment review sys_menu permission is already used by another menu';
  end if;
end//

drop procedure if exists assert_inventory_adjustment_review_target_signatures//
create procedure assert_inventory_adjustment_review_target_signatures()
begin
  declare v_menu_count bigint default 0;
  declare v_menu_signature varchar(64) default '';
  declare v_job_count bigint default 0;
  declare v_job_signature varchar(64) default '';

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
  join tmp_inventory_adjustment_review_sys_menu seed
    on m.menu_id = seed.menu_id
    or (coalesce(seed.perms, '') <> '' and coalesce(m.perms, '') = seed.perms);

  if v_menu_count <> cast(@inventory_adjustment_review_menu_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'inventory adjustment review sys_menu exact target count mismatch';
  end if;
  if lower(v_menu_signature) <> lower(@inventory_adjustment_review_menu_expected_signature) then
    signal sqlstate '45000' set message_text = 'inventory adjustment review sys_menu exact target signature mismatch';
  end if;

  select count(distinct job_id),
         sha2(coalesce(group_concat(distinct
           concat_ws('|',
             job_id,
             coalesce(job_name, ''),
             coalesce(job_group, ''),
             coalesce(invoke_target, ''),
             coalesce(cron_expression, ''),
             coalesce(misfire_policy, ''),
             coalesce(concurrent, ''),
             coalesce(status, ''),
             coalesce(remark, '')
           )
           order by job_id separator '\n'
         ), ''), 256)
    into v_job_count, v_job_signature
  from sys_job
  where invoke_target = 'inventoryAdjustmentReviewTask.effectDueReviews';

  if v_job_count <> cast(@inventory_adjustment_review_job_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'inventory adjustment review sys_job exact target count mismatch';
  end if;
  if lower(v_job_signature) <> lower(@inventory_adjustment_review_job_expected_signature) then
    signal sqlstate '45000' set message_text = 'inventory adjustment review sys_job exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_inventory_adjustment_review_completed//
create procedure assert_inventory_adjustment_review_completed()
begin
  declare v_seed_count bigint default 0;

  select count(1)
    into v_seed_count
  from tmp_inventory_adjustment_review_sys_menu;

  if (
    select count(1)
    from sys_menu m
    join tmp_inventory_adjustment_review_sys_menu seed on seed.menu_id = m.menu_id
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
  ) <> v_seed_count then
    signal sqlstate '45000' set message_text = 'inventory adjustment review sys_menu seed completion mismatch';
  end if;

  if not exists (
    select 1
    from inventory_adjustment_review_policy p
    where p.policy_name = '默认库存退回保护策略'
      and p.policy_status = 'ENABLED'
      and p.review_mode = 'CONDITIONAL'
      and p.direction_scope = 'DECREASE'
      and p.field_scope = 'PLATFORM_TOTAL'
      and p.sales_window_days = '[7,30]'
      and p.sales_aggregate_mode = 'MAX_DAILY_AVG'
      and p.reserve_days = 7
      and p.cooldown_hours = 168
      and p.auto_effect_enabled = 'Y'
      and p.manual_effect_allowed = 'Y'
  ) then
    signal sqlstate '45000' set message_text = 'inventory adjustment review default policy completion mismatch';
  end if;

  if not exists (
    select 1
    from inventory_adjustment_review_policy_binding b
    join inventory_adjustment_review_policy p on p.policy_id = b.policy_id
    where p.policy_name = '默认库存退回保护策略'
      and b.binding_type = 'GLOBAL'
      and b.binding_id_value = 0
      and b.priority = 100
      and b.status = 'ENABLED'
  ) then
    signal sqlstate '45000' set message_text = 'inventory adjustment review default binding completion mismatch';
  end if;

  if not exists (
    select 1
    from sys_job
    where job_name = '库存调整审核到期自动生效'
      and job_group = 'SYSTEM'
      and invoke_target = 'inventoryAdjustmentReviewTask.effectDueReviews'
      and cron_expression = '0 0/10 * * * ?'
      and misfire_policy = '3'
      and concurrent = '1'
      and status = '0'
  ) then
    signal sqlstate '45000' set message_text = 'inventory adjustment review sys_job completion mismatch';
  end if;
end//

delimiter ;

call assert_inventory_adjustment_review_confirmed();

create table if not exists inventory_adjustment_review_policy (
  policy_id                  bigint       not null auto_increment comment '策略ID',
  policy_name                varchar(100) not null                comment '策略名称',
  policy_status              varchar(20)  not null default 'ENABLED' comment '策略状态：ENABLED/DISABLED',
  review_mode                varchar(20)  not null default 'CONDITIONAL' comment '审核模式：DISABLED/CONDITIONAL/ALWAYS',
  direction_scope            varchar(20)  not null default 'DECREASE' comment '方向范围：DECREASE/INCREASE/BOTH',
  field_scope                varchar(50)  not null default 'PLATFORM_TOTAL' comment '字段范围',
  sales_window_days          varchar(100) not null default '[7,30]' comment '销量窗口天数JSON',
  sales_aggregate_mode       varchar(50)  not null default 'MAX_DAILY_AVG' comment '销量聚合方式',
  reserve_days               int          not null default 7      comment '保护库存按日销保留天数',
  cooldown_hours             int          not null default 168    comment '审核保护小时数',
  min_return_qty_to_review   bigint       default null            comment '低于该退回数量不进审核',
  min_return_ratio_to_review decimal(10,4) default null           comment '低于该退回比例不进审核',
  auto_effect_enabled        char(1)      not null default 'Y'    comment '是否允许到期自动生效',
  manual_effect_allowed      char(1)      not null default 'Y'    comment '是否允许人工提前生效',
  create_by                  varchar(64)  default ''              comment '创建者',
  create_time                datetime                             comment '创建时间',
  update_by                  varchar(64)  default ''              comment '更新者',
  update_time                datetime                             comment '更新时间',
  remark                     varchar(500) default ''              comment '备注',
  primary key (policy_id),
  unique key uk_inventory_adjustment_review_policy_name (policy_name),
  key idx_inventory_adjustment_review_policy_status (policy_status, review_mode)
) engine=innodb default charset=utf8mb4 comment='库存调整审核策略';

create table if not exists inventory_adjustment_review_policy_binding (
  binding_id       bigint      not null auto_increment comment '绑定ID',
  policy_id        bigint      not null                comment '策略ID',
  binding_type     varchar(20) not null default 'SELLER' comment '绑定类型：GLOBAL/SELLER',
  binding_id_value bigint      not null default 0      comment '绑定对象ID，GLOBAL固定为0',
  priority         int         not null default 100    comment '优先级，数字越小越优先',
  status           varchar(20) not null default 'ENABLED' comment '状态：ENABLED/DISABLED',
  create_by        varchar(64) default ''              comment '创建者',
  create_time      datetime                            comment '创建时间',
  update_by        varchar(64) default ''              comment '更新者',
  update_time      datetime                            comment '更新时间',
  remark           varchar(500) default ''             comment '备注',
  primary key (binding_id),
  unique key uk_inventory_adjustment_review_binding_target (binding_type, binding_id_value),
  key idx_inventory_adjustment_review_binding_policy (policy_id, status, priority)
) engine=innodb default charset=utf8mb4 comment='库存调整审核策略绑定';

create table if not exists inventory_adjustment_review_request (
  review_id                              bigint       not null auto_increment comment '审核单ID',
  review_no                              varchar(40)  not null                comment '审核单号',
  review_status                          varchar(20)  not null default 'WAITING' comment '状态：WAITING/EFFECTIVE/REJECTED',
  policy_id                              bigint       default null            comment '命中策略ID',
  policy_snapshot_json                   longtext                             comment '策略快照JSON',
  stock_id                               bigint       not null                comment '库存行ID',
  stock_key                              varchar(100) not null                comment '库存行业务键',
  spu_id                                 bigint       not null                comment 'SPU ID',
  sku_id                                 bigint       not null                comment 'SKU ID',
  seller_id                              bigint       not null                comment '卖家ID',
  system_sku_code                        varchar(100) default ''              comment '系统SKU编码',
  product_name                           varchar(500) default ''              comment '商品名称快照',
  sku_name                               varchar(500) default ''              comment 'SKU名称快照',
  warehouse_kind                         varchar(30)  default ''              comment '仓库类型',
  warehouse_ref_type                     varchar(50)  default ''              comment '仓库引用类型',
  warehouse_name                         varchar(200) default ''              comment '仓库名称快照',
  adjust_field                           varchar(50)  not null                comment '调整字段',
  adjust_direction                       varchar(20)  not null                comment '调整方向：DECREASE/INCREASE',
  request_before_platform_total_qty      bigint       not null default 0      comment '申请时平台总库存',
  requested_adjust_qty                   bigint       not null default 0      comment '申请退回/调整数量',
  request_expected_after_platform_total_qty bigint    not null default 0      comment '申请后平台总库存',
  platform_reserved_qty_snapshot         bigint       not null default 0      comment '申请时平台锁定库存',
  sales_7d_qty                           bigint       not null default 0      comment '近7日销量',
  sales_7d_daily_avg                     decimal(18,4) not null default 0    comment '近7日日均销量',
  sales_30d_qty                          bigint       not null default 0      comment '近30日销量',
  sales_30d_daily_avg                    decimal(18,4) not null default 0    comment '近30日日均销量',
  threshold_daily_avg                    decimal(18,4) not null default 0    comment '阈值日均销量',
  threshold_reserve_days                 int          not null default 7      comment '阈值保留天数',
  protected_retained_qty                 bigint       not null default 0      comment '销量保护应保留库存',
  min_retained_qty                       bigint       not null default 0      comment '含锁定库存后的最低保留库存',
  immediate_returnable_qty               bigint       not null default 0      comment '申请时可立即退回数量',
  trigger_reason                         varchar(500) default ''              comment '触发原因',
  submit_terminal                        varchar(20)  not null default 'ADMIN' comment '提交端',
  submit_user_id                         bigint       default null            comment '提交人ID',
  submit_user_name                       varchar(64)  default ''              comment '提交人',
  submit_reason                          varchar(500) default ''              comment '提交原因',
  submit_time                            datetime     not null                comment '提交时间',
  planned_effective_time                 datetime     not null                comment '计划生效时间',
  effective_time                         datetime                             comment '实际生效时间',
  effective_operator_id                  bigint       default null            comment '生效操作人ID',
  effective_operator_name                varchar(64)  default ''              comment '生效操作人',
  effective_before_platform_total_qty    bigint       default null            comment '生效前平台总库存',
  actual_effect_qty                      bigint       default null            comment '实际生效数量',
  unfulfilled_qty                        bigint       default null            comment '无法满足数量',
  effective_after_platform_total_qty     bigint       default null            comment '生效后平台总库存',
  review_reason                          varchar(500) default ''              comment '审核处理原因',
  version                                int          not null default 0      comment '乐观锁版本',
  create_by                              varchar(64)  default ''              comment '创建者',
  create_time                            datetime                             comment '创建时间',
  update_by                              varchar(64)  default ''              comment '更新者',
  update_time                            datetime                             comment '更新时间',
  remark                                 varchar(500) default ''              comment '备注',
  primary key (review_id),
  unique key uk_inventory_adjustment_review_no (review_no),
  key idx_inventory_adjustment_review_status_time (review_status, planned_effective_time),
  key idx_inventory_adjustment_review_stock (stock_id, review_status),
  key idx_inventory_adjustment_review_seller (seller_id, submit_time),
  key idx_inventory_adjustment_review_sku (sku_id, submit_time)
) engine=innodb default charset=utf8mb4 comment='库存调整审核单';

create table if not exists inventory_adjustment_review_operation_log (
  log_id           bigint      not null auto_increment comment '日志ID',
  review_id        bigint      not null                comment '审核单ID',
  review_no        varchar(40) not null                comment '审核单号',
  operation_type   varchar(30) not null                comment '操作类型',
  before_status    varchar(20) default ''              comment '操作前状态',
  after_status     varchar(20) default ''              comment '操作后状态',
  operation_reason varchar(500) default ''             comment '操作原因',
  operator_id      bigint      default null            comment '操作人ID',
  operator_name    varchar(64) default ''              comment '操作人',
  operate_time     datetime    not null                comment '操作时间',
  change_summary   varchar(1000) default ''            comment '变化摘要',
  create_time      datetime                            comment '创建时间',
  remark           varchar(500) default ''             comment '备注',
  primary key (log_id),
  key idx_inventory_adjustment_review_log_review (review_id, operate_time),
  key idx_inventory_adjustment_review_log_no (review_no)
) engine=innodb default charset=utf8mb4 comment='库存调整审核操作日志';

create table if not exists inventory_sku_sales_daily (
  sales_daily_id bigint not null auto_increment comment '日销量ID',
  sku_id         bigint not null                comment 'SKU ID',
  seller_id      bigint not null                comment '卖家ID',
  stat_date      date   not null                comment '统计日期',
  sold_qty       bigint not null default 0      comment '销量',
  create_by      varchar(64) default ''         comment '创建者',
  create_time    datetime                       comment '创建时间',
  update_by      varchar(64) default ''         comment '更新者',
  update_time    datetime                       comment '更新时间',
  remark         varchar(500) default ''        comment '备注',
  primary key (sales_daily_id),
  unique key uk_inventory_sku_sales_daily (sku_id, stat_date),
  key idx_inventory_sku_sales_daily_seller (seller_id, stat_date)
) engine=innodb default charset=utf8mb4 comment='SKU日销量聚合';

create temporary table if not exists tmp_inventory_adjustment_review_sys_menu (
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
  key idx_inventory_adjustment_review_sys_menu_id (menu_id)
) engine=memory;

truncate table tmp_inventory_adjustment_review_sys_menu;

insert into tmp_inventory_adjustment_review_sys_menu
  (menu_id, menu_name, parent_id, order_num, menu_type, path, component, route_name, perms, icon, remark)
values
  (2452, '库存调整审核', 2100, 15, 'C', 'inventory-adjustment', 'Inventory/AdjustmentReview/index',
   'InventoryAdjustmentReview', 'review:inventoryAdjustment:list', 'AuditOutlined', '审核中心菜单：库存调整审核'),
  (2501, '库存调整审核查询', 2452, 1, 'F', '#', '', '', 'review:inventoryAdjustment:query', '#', '库存调整审核查询按钮'),
  (2502, '库存调整审核立即生效', 2452, 2, 'F', '#', '', '', 'review:inventoryAdjustment:effect', '#', '库存调整审核立即生效按钮'),
  (2503, '库存调整审核调整时间', 2452, 3, 'F', '#', '', '', 'review:inventoryAdjustment:edit', '#', '库存调整审核调整计划生效时间按钮'),
  (2504, '库存调整审核驳回', 2452, 4, 'F', '#', '', '', 'review:inventoryAdjustment:reject', '#', '库存调整审核驳回按钮'),
  (2505, '库存调整审核日志', 2452, 5, 'F', '#', '', '', 'review:inventoryAdjustment:log', '#', '库存调整审核日志按钮'),
  (2506, '库存调整审核配置', 2452, 6, 'F', '#', '', '', 'review:inventoryAdjustment:config', '#', '库存调整审核策略配置按钮');

call assert_inventory_adjustment_review_parent_ready();
call assert_inventory_adjustment_review_menu_slots();
call assert_inventory_adjustment_review_target_signatures();

start transaction;

insert into inventory_adjustment_review_policy (
  policy_name, policy_status, review_mode, direction_scope, field_scope, sales_window_days,
  sales_aggregate_mode, reserve_days, cooldown_hours, min_return_qty_to_review,
  min_return_ratio_to_review, auto_effect_enabled, manual_effect_allowed, create_by, create_time, remark
) values (
  '默认库存退回保护策略', 'ENABLED', 'CONDITIONAL', 'DECREASE', 'PLATFORM_TOTAL', '[7,30]',
  'MAX_DAILY_AVG', 7, 168, null, null, 'Y', 'Y', 'admin', sysdate(),
  '默认按近7日和近30日日均销量取大值，保留7天预估销量；申请退回数量超过可立即退回数量时进入审核。'
)
on duplicate key update
  policy_status = values(policy_status),
  review_mode = values(review_mode),
  direction_scope = values(direction_scope),
  field_scope = values(field_scope),
  sales_window_days = values(sales_window_days),
  sales_aggregate_mode = values(sales_aggregate_mode),
  reserve_days = values(reserve_days),
  cooldown_hours = values(cooldown_hours),
  auto_effect_enabled = values(auto_effect_enabled),
  manual_effect_allowed = values(manual_effect_allowed),
  update_by = 'admin',
  update_time = sysdate(),
  remark = values(remark);

set @inventory_adjustment_review_default_policy_id := (
  select policy_id
  from inventory_adjustment_review_policy
  where policy_name = '默认库存退回保护策略'
  limit 1
);

insert into inventory_adjustment_review_policy_binding (
  policy_id, binding_type, binding_id_value, priority, status, create_by, create_time, remark
) values (
  @inventory_adjustment_review_default_policy_id, 'GLOBAL', 0, 100, 'ENABLED', 'admin', sysdate(),
  '默认全局库存调整审核策略绑定'
)
on duplicate key update
  policy_id = values(policy_id),
  priority = values(priority),
  status = values(status),
  update_by = 'admin',
  update_time = sysdate(),
  remark = values(remark);

insert into sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
  update_by, update_time, remark
)
select
  menu_id, menu_name, parent_id, order_num, path, component, '', route_name,
  1, 0, menu_type, '0', '0', perms, icon, 'admin', sysdate(), '', null, remark
from tmp_inventory_adjustment_review_sys_menu
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

update sys_job
set job_name = '库存调整审核到期自动生效',
    job_group = 'SYSTEM',
    invoke_target = 'inventoryAdjustmentReviewTask.effectDueReviews',
    cron_expression = '0 0/10 * * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每10分钟处理已到计划生效时间的库存调整审核单。'
where invoke_target = 'inventoryAdjustmentReviewTask.effectDueReviews';

insert into sys_job (
  job_name, job_group, invoke_target, cron_expression, misfire_policy,
  concurrent, status, create_by, create_time, remark
)
select
  '库存调整审核到期自动生效', 'SYSTEM', 'inventoryAdjustmentReviewTask.effectDueReviews',
  '0 0/10 * * * ?', '3', '1', '0', 'admin', sysdate(),
  '每10分钟处理已到计划生效时间的库存调整审核单。'
where not exists (
  select 1
  from sys_job
  where invoke_target = 'inventoryAdjustmentReviewTask.effectDueReviews'
);

call assert_inventory_adjustment_review_completed();

commit;

drop temporary table if exists tmp_inventory_adjustment_review_sys_menu;
drop procedure if exists assert_inventory_adjustment_review_completed;
drop procedure if exists assert_inventory_adjustment_review_target_signatures;
drop procedure if exists assert_inventory_adjustment_review_menu_slots;
drop procedure if exists assert_inventory_adjustment_review_parent_ready;
drop procedure if exists assert_inventory_adjustment_review_confirmed;
