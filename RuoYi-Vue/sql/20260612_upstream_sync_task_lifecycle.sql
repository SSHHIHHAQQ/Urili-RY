-- 上游系统同步任务生命周期迁移。
-- Scope:
-- 1. Create request/task tables for recoverable upstream sync execution.
-- 2. Add sync task status dictionaries.
-- 3. Register RuoYi Quartz dispatcher job.
-- 4. Add fine-grained upstream sync task permissions.

set names utf8mb4;
set session group_concat_max_len = 1048576;

set @confirm_upstream_sync_task_lifecycle := coalesce(@confirm_upstream_sync_task_lifecycle, '');
set @upstream_sync_task_lifecycle_expected_job_count :=
    coalesce(@upstream_sync_task_lifecycle_expected_job_count, '');
set @upstream_sync_task_lifecycle_expected_job_signature :=
    coalesce(@upstream_sync_task_lifecycle_expected_job_signature, '');
set @upstream_sync_task_lifecycle_expected_menu_count :=
    coalesce(@upstream_sync_task_lifecycle_expected_menu_count, '');
set @upstream_sync_task_lifecycle_expected_menu_signature :=
    coalesce(@upstream_sync_task_lifecycle_expected_menu_signature, '');

delimiter //

drop procedure if exists assert_upstream_sync_task_lifecycle_confirmed//
create procedure assert_upstream_sync_task_lifecycle_confirmed()
begin
  if coalesce(@confirm_upstream_sync_task_lifecycle, '')
      <> 'APPLY_UPSTREAM_SYNC_TASK_LIFECYCLE' then
    signal sqlstate '45000' set message_text = 'set @confirm_upstream_sync_task_lifecycle = APPLY_UPSTREAM_SYNC_TASK_LIFECYCLE before running this migration';
  end if;

  if coalesce(@upstream_sync_task_lifecycle_expected_job_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @upstream_sync_task_lifecycle_expected_job_count after previewing dispatcher sys_job row';
  end if;
  if coalesce(@upstream_sync_task_lifecycle_expected_job_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @upstream_sync_task_lifecycle_expected_job_signature after previewing dispatcher sys_job row';
  end if;
  if coalesce(@upstream_sync_task_lifecycle_expected_menu_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @upstream_sync_task_lifecycle_expected_menu_count after previewing task permission menu rows';
  end if;
  if coalesce(@upstream_sync_task_lifecycle_expected_menu_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @upstream_sync_task_lifecycle_expected_menu_signature after previewing task permission menu rows';
  end if;
end//

drop procedure if exists assert_upstream_sync_task_lifecycle_targets//
create procedure assert_upstream_sync_task_lifecycle_targets()
begin
  declare v_job_count int default 0;
  declare v_job_signature varchar(64) default '';
  declare v_menu_count int default 0;
  declare v_menu_signature varchar(64) default '';
  declare v_parent_count int default 0;

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
    into v_job_count, v_job_signature
  from sys_job
  where invoke_target = 'upstreamSyncDispatchTask.dispatch';

  if v_job_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream sync dispatcher sys_job invoke_target must be unique before upsert';
  end if;
  if v_job_count <> cast(@upstream_sync_task_lifecycle_expected_job_count as unsigned) then
    signal sqlstate '45000' set message_text = 'upstream sync dispatcher sys_job exact target count mismatch';
  end if;
  if lower(v_job_signature) <> lower(@upstream_sync_task_lifecycle_expected_job_signature) then
    signal sqlstate '45000' set message_text = 'upstream sync dispatcher sys_job exact target signature mismatch';
  end if;

  select count(1)
    into v_parent_count
  from sys_menu
  where menu_id = 2031
    and parent_id = 2030
    and menu_type = 'C'
    and perms = 'integration:upstream:list';
  if v_parent_count <> 1 then
    signal sqlstate '45000' set message_text = 'upstream system parent menu 2031 must exist before adding task permissions';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             menu_id,
             parent_id,
             menu_type,
             coalesce(perms, ''),
             coalesce(menu_name, '')
           )
           order by menu_id separator '|'
         ), ''), 256)
    into v_menu_count, v_menu_signature
  from sys_menu
  where menu_id in (2324, 2325, 2326)
     or perms in (
       'integration:upstream:task:list',
       'integration:upstream:task:retry',
       'integration:upstream:task:cancel'
     );

  if v_menu_count <> cast(@upstream_sync_task_lifecycle_expected_menu_count as unsigned) then
    signal sqlstate '45000' set message_text = 'upstream sync task permission menu exact target count mismatch';
  end if;
  if lower(v_menu_signature) <> lower(@upstream_sync_task_lifecycle_expected_menu_signature) then
    signal sqlstate '45000' set message_text = 'upstream sync task permission menu exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_upstream_sync_task_lifecycle_confirmed();
call assert_upstream_sync_task_lifecycle_targets();

create table if not exists upstream_system_sync_request (
  request_id            bigint(20)   not null auto_increment comment '同步请求ID',
  request_no            varchar(64)  not null                comment '同步请求号',
  connection_code       varchar(64)  not null                comment '主仓接入编号',
  trigger_source        varchar(32)  not null                comment '触发来源',
  mode                  varchar(32)  not null                comment '同步模式',
  requested_sync_types  varchar(255) not null                comment '请求同步类型',
  status                varchar(16)  not null default 'PENDING' comment '请求状态',
  submitted_by          varchar(64)  default ''              comment '提交人',
  submitted_time        datetime                             comment '提交时间',
  started_time          datetime                             comment '开始时间',
  finished_time         datetime                             comment '结束时间',
  task_count            int         not null default 0       comment '任务总数',
  success_count         int         not null default 0       comment '成功任务数',
  failed_count          int         not null default 0       comment '失败任务数',
  timeout_count         int         not null default 0       comment '超时任务数',
  skipped_count         int         not null default 0       comment '跳过任务数',
  cancelled_count       int         not null default 0       comment '取消任务数',
  last_error_message    varchar(500) default ''              comment '最近错误摘要',
  create_time           datetime                             comment '创建时间',
  update_time           datetime                             comment '更新时间',
  remark                varchar(500) default ''              comment '备注',
  primary key (request_id),
  unique key uk_upstream_sync_request_no (request_no),
  key idx_upstream_sync_request_connection (connection_code, submitted_time),
  key idx_upstream_sync_request_status (status, submitted_time)
) engine=innodb comment='上游系统同步请求';

create table if not exists upstream_system_sync_task (
  task_id                 bigint(20)   not null auto_increment comment '同步任务ID',
  request_no              varchar(64)  not null                comment '同步请求号',
  sync_batch_id           varchar(64)  not null                comment '同步批次号',
  connection_code         varchar(64)  not null                comment '主仓接入编号',
  sync_type               varchar(32)  not null                comment '同步类型',
  mode                    varchar(32)  not null                comment '同步模式',
  trigger_source          varchar(32)  not null                comment '触发来源',
  status                  varchar(16)  not null default 'PENDING' comment '任务状态',
  priority                int          not null default 100    comment '优先级',
  payload_redacted        longtext                              comment '脱敏任务参数',
  lease_owner             varchar(128) default ''              comment '租约持有者',
  lease_until             datetime                             comment '租约截止时间',
  attempt_count           int          not null default 0      comment '已尝试次数',
  max_attempts            int          not null default 1      comment '最大尝试次数',
  next_attempt_time       datetime                             comment '下次尝试时间',
  deadline_at             datetime                             comment '硬截止时间',
  started_time            datetime                             comment '开始执行时间',
  finished_time           datetime                             comment '结束时间',
  current_request_log_id  bigint(20)                            comment '当前请求日志ID',
  trace_id                varchar(64)  default ''              comment '追踪号',
  sys_job_invoke_target   varchar(500) default ''              comment 'RuoYi调用目标',
  pulled_count            int          not null default 0      comment '拉取行数',
  inserted_count          int          not null default 0      comment '新增行数',
  changed_count           int          not null default 0      comment '变更行数',
  unchanged_count         int          not null default 0      comment '未变化行数',
  disabled_count          int          not null default 0      comment '停用行数',
  failed_count            int          not null default 0      comment '失败行数',
  error_code              varchar(64)  default ''              comment '错误码',
  error_message           varchar(500) default ''              comment '错误摘要',
  create_time             datetime                             comment '创建时间',
  update_time             datetime                             comment '更新时间',
  remark                  varchar(500) default ''              comment '备注',
  primary key (task_id),
  unique key uk_upstream_sync_task_batch (sync_batch_id),
  key idx_upstream_sync_task_request (request_no, task_id),
  key idx_upstream_sync_task_claim (status, next_attempt_time, priority, task_id),
  key idx_upstream_sync_task_connection (connection_code, sync_type, status),
  key idx_upstream_sync_task_lease (status, lease_until)
) engine=innodb comment='上游系统同步任务';

start transaction;

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '上游同步任务状态', 'integration_sync_task_status', '0', 'admin', sysdate(), '', null, '上游同步任务状态'
where not exists (select 1 from sys_dict_type where dict_type = 'integration_sync_task_status');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'integration_sync_task_status', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '上游同步任务状态'
from (
    select 1 as dict_sort, '排队中' as dict_label, 'PENDING' as dict_value, 'default' as list_class, 'Y' as is_default
    union all select 2, '已领取', 'CLAIMED', 'processing', 'N'
    union all select 3, '同步中', 'RUNNING', 'processing', 'N'
    union all select 4, '成功', 'SUCCESS', 'success', 'N'
    union all select 5, '失败', 'FAILED', 'danger', 'N'
    union all select 6, '超时', 'TIMEOUT', 'warning', 'N'
    union all select 7, '已取消', 'CANCELED', 'default', 'N'
    union all select 8, '已跳过', 'SKIPPED', 'warning', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'integration_sync_task_status' and d.dict_value = seed.dict_value);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '上游同步触发来源', 'integration_sync_trigger_source', '0', 'admin', sysdate(), '', null, '上游同步触发来源'
where not exists (select 1 from sys_dict_type where dict_type = 'integration_sync_trigger_source');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'integration_sync_trigger_source', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '上游同步触发来源'
from (
    select 1 as dict_sort, '手动触发' as dict_label, 'MANUAL' as dict_value, 'primary' as list_class, 'Y' as is_default
    union all select 2, '定时触发', 'SCHEDULED', 'processing', 'N'
    union all select 3, '恢复器', 'RECOVERY', 'warning', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'integration_sync_trigger_source' and d.dict_value = seed.dict_value);

update sys_job
set job_name = '上游同步任务分发',
    job_group = 'SYSTEM',
    invoke_target = 'upstreamSyncDispatchTask.dispatch',
    cron_expression = '0/30 * * * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每30秒领取上游同步任务表中的排队任务，统一执行手动和定时同步。'
where invoke_target = 'upstreamSyncDispatchTask.dispatch';

insert into sys_job (
    job_name, job_group, invoke_target, cron_expression, misfire_policy,
    concurrent, status, create_by, create_time, remark
)
select
    '上游同步任务分发', 'SYSTEM', 'upstreamSyncDispatchTask.dispatch',
    '0/30 * * * * ?', '3', '1', '0', 'admin', sysdate(),
    '每30秒领取上游同步任务表中的排队任务，统一执行手动和定时同步。'
where not exists (
    select 1 from sys_job where invoke_target = 'upstreamSyncDispatchTask.dispatch'
);

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2324, '同步任务查看', 2031, 55, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:task:list', '#', 'admin',
     sysdate(), '', null, '查看上游同步任务请求和执行项'),
    (2325, '同步任务重试', 2031, 60, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:task:retry', '#', 'admin',
     sysdate(), '', null, '重试失败或超时的上游同步任务'),
    (2326, '同步任务取消', 2031, 65, '#', '', '', '',
     1, 0, 'F', '0', '0', 'integration:upstream:task:cancel', '#', 'admin',
     sysdate(), '', null, '取消尚未执行的上游同步任务')
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

commit;

drop procedure if exists assert_upstream_sync_task_lifecycle_confirmed;
drop procedure if exists assert_upstream_sync_task_lifecycle_targets;
