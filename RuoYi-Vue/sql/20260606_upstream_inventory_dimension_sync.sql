-- Upstream WMS dimension and SKU inventory sync.
-- Scope:
-- 1. Create source warehouse inventory snapshot tables.
-- 2. Grant upstream dimension/inventory permissions from the upstream-system menu owner.
-- 3. Register the RuoYi sys_job entry for 10-minute inventory sync.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_upstream_inventory_dimension_sync := coalesce(@confirm_upstream_inventory_dimension_sync, '');
set @upstream_inventory_role_menu_expected_count :=
    coalesce(@upstream_inventory_role_menu_expected_count, '');
set @upstream_inventory_role_menu_expected_signature :=
    coalesce(@upstream_inventory_role_menu_expected_signature, '');
set @upstream_inventory_sync_job_expected_count :=
    coalesce(@upstream_inventory_sync_job_expected_count, '');
set @upstream_inventory_sync_job_expected_signature :=
    coalesce(@upstream_inventory_sync_job_expected_signature, '');

delimiter //

drop procedure if exists assert_upstream_inventory_dimension_sync_confirmed//
create procedure assert_upstream_inventory_dimension_sync_confirmed()
begin
  if coalesce(@confirm_upstream_inventory_dimension_sync, '')
      <> 'APPLY_UPSTREAM_INVENTORY_DIMENSION_SYNC' then
    signal sqlstate '45000' set message_text = 'set @confirm_upstream_inventory_dimension_sync = APPLY_UPSTREAM_INVENTORY_DIMENSION_SYNC before running this migration';
  end if;

  if coalesce(@upstream_inventory_role_menu_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @upstream_inventory_role_menu_expected_count after previewing exact upstream inventory sys_role_menu grant rows';
  end if;
  if coalesce(@upstream_inventory_role_menu_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @upstream_inventory_role_menu_expected_signature after previewing exact upstream inventory sys_role_menu grant rows';
  end if;
  if coalesce(@upstream_inventory_sync_job_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @upstream_inventory_sync_job_expected_count after previewing exact upstream inventory sys_job row';
  end if;
  if coalesce(@upstream_inventory_sync_job_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @upstream_inventory_sync_job_expected_signature after previewing exact upstream inventory sys_job row';
  end if;
end//

drop procedure if exists assert_upstream_inventory_menu_owner_ready//
create procedure assert_upstream_inventory_menu_owner_ready()
begin
  if not exists (
      select 1
      from sys_menu
      where menu_id = 2307
        and parent_id = 2031
        and menu_type = 'F'
        and perms = 'integration:upstream:dimensionSync'
  ) then
    signal sqlstate '45000' set message_text = 'upstream inventory dimension sync requires upstream_system_management_seed.sql to own menu 2307';
  end if;

  if not exists (
      select 1
      from sys_menu
      where menu_id = 2308
        and parent_id = 2031
        and menu_type = 'F'
        and perms = 'integration:upstream:inventoryQuery'
  ) then
    signal sqlstate '45000' set message_text = 'upstream inventory dimension sync requires upstream_system_management_seed.sql to own menu 2308';
  end if;

  if not exists (
      select 1
      from sys_menu
      where menu_id = 2309
        and parent_id = 2031
        and menu_type = 'F'
        and perms = 'integration:upstream:inventorySync'
  ) then
    signal sqlstate '45000' set message_text = 'upstream inventory dimension sync requires upstream_system_management_seed.sql to own menu 2309';
  end if;
end//

drop procedure if exists assert_upstream_inventory_role_menu_targets//
create procedure assert_upstream_inventory_role_menu_targets()
begin
  declare v_count int default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':', grant_target.role_id, grant_target.menu_id)
           order by grant_target.role_id, grant_target.menu_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from (
      select distinct source_role.role_id, target_menu.menu_id
      from sys_role_menu source_role
      inner join sys_menu source_menu on source_menu.menu_id = source_role.menu_id
      inner join sys_menu target_menu on target_menu.perms in (
          'integration:upstream:dimensionSync',
          'integration:upstream:inventorySync'
      )
      where source_menu.perms = 'integration:upstream:sync'
        and not exists (
            select 1
            from sys_role_menu existing
            where existing.role_id = source_role.role_id
              and existing.menu_id = target_menu.menu_id
        )
      union
      select distinct source_role.role_id, target_menu.menu_id
      from sys_role_menu source_role
      inner join sys_menu source_menu on source_menu.menu_id = source_role.menu_id
      inner join sys_menu target_menu on target_menu.perms = 'integration:upstream:inventoryQuery'
      where source_menu.perms = 'integration:upstream:query'
        and not exists (
            select 1
            from sys_role_menu existing
            where existing.role_id = source_role.role_id
              and existing.menu_id = target_menu.menu_id
        )
  ) grant_target;

  if v_count <> cast(@upstream_inventory_role_menu_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'upstream inventory role-menu exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@upstream_inventory_role_menu_expected_signature) then
    signal sqlstate '45000' set message_text = 'upstream inventory role-menu exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_upstream_inventory_sync_job_targets//
create procedure assert_upstream_inventory_sync_job_targets()
begin
  declare v_count int default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             job_id,
             coalesce(job_name, ''),
             coalesce(job_group, ''),
             coalesce(invoke_target, ''),
             coalesce(cron_expression, ''),
             coalesce(misfire_policy, ''),
             coalesce(concurrent, ''),
             coalesce(status, '')
           )
           order by job_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from sys_job
  where invoke_target = 'upstreamSystemTask.syncInventory';

  if v_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream inventory sys_job invoke_target must be unique before upsert';
  end if;
  if v_count <> cast(@upstream_inventory_sync_job_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'upstream inventory sys_job exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@upstream_inventory_sync_job_expected_signature) then
    signal sqlstate '45000' set message_text = 'upstream inventory sys_job exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_upstream_inventory_dimension_sync_confirmed();
call assert_upstream_inventory_menu_owner_ready();
call assert_upstream_inventory_role_menu_targets();
call assert_upstream_inventory_sync_job_targets();
drop procedure if exists assert_upstream_inventory_dimension_sync_confirmed;
drop procedure if exists assert_upstream_inventory_menu_owner_ready;

create table if not exists upstream_system_sku_inventory_snapshot (
  inventory_snapshot_id bigint(20)   not null auto_increment comment '库存快照ID',
  connection_code       varchar(64)  not null                comment '主仓接入编号',
  upstream_warehouse_code varchar(100) not null              comment '领星仓库代码',
  upstream_warehouse_name varchar(200) default ''            comment '领星仓库名称',
  master_sku            varchar(128) not null                comment '领星masterSku',
  master_product_name   varchar(255) default ''              comment '领星产品名称',
  inventory_scope       varchar(32)  not null default 'COMPREHENSIVE' comment '库存口径',
  inventory_attribute   varchar(64)  not null default ''     comment '库存属性',
  batch_no              varchar(128) not null default ''     comment '批次号',
  location_code         varchar(128) not null default ''     comment '库位代码',
  total_quantity        bigint(20)   not null default 0      comment '总库存',
  available_quantity    bigint(20)   not null default 0      comment '可用库存',
  locked_quantity       bigint(20)   not null default 0      comment '锁定库存',
  in_transit_quantity   bigint(20)   not null default 0      comment '在途库存',
  boxed_quantity        bigint(20)   default null            comment '箱内库存',
  unboxed_quantity      bigint(20)   default null            comment '散件库存',
  system_warehouse_code varchar(64)  default ''              comment '系统仓库代码',
  system_warehouse_name varchar(200) default ''              comment '系统仓库名称',
  system_sku            varchar(128) default ''              comment '系统SKU',
  system_sku_name       varchar(255) default ''              comment '系统SKU名称',
  customer_name         varchar(200) default ''              comment '客户名称',
  status                varchar(16)  not null default 'ACTIVE' comment '同步状态',
  sync_batch_id         varchar(64)  not null                comment '同步批次号',
  source_payload_json   longtext                              comment '上游原始库存行JSON快照',
  source_payload_hash   varchar(64)  default ''              comment '上游原始库存行JSON哈希',
  first_seen_time       datetime     not null                comment '首次发现时间',
  last_seen_time        datetime     not null                comment '最近发现时间',
  update_time           datetime     not null                comment '更新时间',
  primary key (inventory_snapshot_id),
  unique key uk_upstream_inventory_snapshot_natural
    (connection_code, upstream_warehouse_code, master_sku, inventory_scope, inventory_attribute, batch_no, location_code),
  key idx_upstream_inventory_snapshot_connection (connection_code, status),
  key idx_upstream_inventory_snapshot_warehouse (connection_code, upstream_warehouse_code),
  key idx_upstream_inventory_snapshot_sku (connection_code, master_sku),
  key idx_upstream_inventory_snapshot_system_wh (system_warehouse_code),
  key idx_upstream_inventory_snapshot_system_sku (connection_code, system_sku),
  key idx_upstream_inventory_snapshot_update (update_time)
) engine=innodb comment='上游SKU仓库库存快照';

create table if not exists upstream_system_inventory_sync_state (
  connection_code     varchar(64) not null                 comment '主仓接入编号',
  status              varchar(16) not null default 'NEVER' comment '同步状态',
  sync_batch_id       varchar(64) default null             comment '同步批次号',
  last_started_time   datetime                             comment '最近开始同步时间',
  last_finished_time  datetime                             comment '最近结束同步时间',
  last_success_time   datetime                             comment '最近同步成功时间',
  next_sync_time      datetime                             comment '下次计划同步时间',
  total_count         int        not null default 0        comment '最近同步总条数',
  active_count        int        not null default 0        comment '当前有效条数',
  missing_count       int        not null default 0        comment '最近标记缺失条数',
  last_error_message  varchar(500) default ''              comment '最近失败原因',
  update_time         datetime                             comment '更新时间',
  primary key (connection_code)
) engine=innodb comment='上游库存同步状态';

start transaction;

insert into sys_role_menu(role_id, menu_id)
select distinct source_role.role_id, target_menu.menu_id
from sys_role_menu source_role
inner join sys_menu source_menu on source_menu.menu_id = source_role.menu_id
inner join sys_menu target_menu on target_menu.perms in (
    'integration:upstream:dimensionSync',
    'integration:upstream:inventorySync'
)
where source_menu.perms = 'integration:upstream:sync'
  and not exists (
      select 1
      from sys_role_menu existing
      where existing.role_id = source_role.role_id
        and existing.menu_id = target_menu.menu_id
  );

insert into sys_role_menu(role_id, menu_id)
select distinct source_role.role_id, target_menu.menu_id
from sys_role_menu source_role
inner join sys_menu source_menu on source_menu.menu_id = source_role.menu_id
inner join sys_menu target_menu on target_menu.perms = 'integration:upstream:inventoryQuery'
where source_menu.perms = 'integration:upstream:query'
  and not exists (
      select 1
      from sys_role_menu existing
      where existing.role_id = source_role.role_id
        and existing.menu_id = target_menu.menu_id
  );

update sys_job
set job_name = '领星SKU库存每10分钟同步',
    job_group = 'SYSTEM',
    invoke_target = 'upstreamSystemTask.syncInventory',
    cron_expression = '0 0/10 * * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每10分钟同步已启用领星主仓的SKU库存快照，使用若依定时任务调度'
where invoke_target = 'upstreamSystemTask.syncInventory';

insert into sys_job (
    job_name,
    job_group,
    invoke_target,
    cron_expression,
    misfire_policy,
    concurrent,
    status,
    create_by,
    create_time,
    remark
)
select
    '领星SKU库存每10分钟同步',
    'SYSTEM',
    'upstreamSystemTask.syncInventory',
    '0 0/10 * * * ?',
    '3',
    '1',
    '0',
    'admin',
    sysdate(),
    '每10分钟同步已启用领星主仓的SKU库存快照，使用若依定时任务调度'
where not exists (
    select 1
    from sys_job
    where invoke_target = 'upstreamSystemTask.syncInventory'
);

commit;

drop procedure if exists assert_upstream_inventory_role_menu_targets;
drop procedure if exists assert_upstream_inventory_sync_job_targets;
