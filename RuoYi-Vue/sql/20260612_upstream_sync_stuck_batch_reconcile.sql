-- 上游同步历史卡住批次收口脚本。
-- Scope:
-- 1. Only update preview-confirmed upstream_system_sync_batch rows.
-- 2. Recalculate exact target count and SHA-256 signature before update.
-- 3. Close stuck SYNCING records as TIMEOUT; do not delete history.

set names utf8mb4;
set session group_concat_max_len = 1048576;

set @confirm_upstream_sync_stuck_batch_reconcile :=
    coalesce(@confirm_upstream_sync_stuck_batch_reconcile, '');
set @upstream_sync_stuck_batch_ids :=
    coalesce(@upstream_sync_stuck_batch_ids, '');
set @upstream_sync_stuck_batch_expected_count :=
    coalesce(@upstream_sync_stuck_batch_expected_count, '');
set @upstream_sync_stuck_batch_expected_signature :=
    coalesce(@upstream_sync_stuck_batch_expected_signature, '');

delimiter //

drop procedure if exists assert_upstream_sync_stuck_batch_reconcile_confirmed//
create procedure assert_upstream_sync_stuck_batch_reconcile_confirmed()
begin
  if coalesce(@confirm_upstream_sync_stuck_batch_reconcile, '')
      <> 'RECONCILE_UPSTREAM_SYNC_STUCK_BATCHES' then
    signal sqlstate '45000' set message_text = 'set @confirm_upstream_sync_stuck_batch_reconcile = RECONCILE_UPSTREAM_SYNC_STUCK_BATCHES before running this migration';
  end if;
  if length(trim(both ',' from coalesce(@upstream_sync_stuck_batch_ids, ''))) = 0 then
    signal sqlstate '45000' set message_text = 'set @upstream_sync_stuck_batch_ids to preview-confirmed comma-separated sync_batch_id values';
  end if;
  if coalesce(@upstream_sync_stuck_batch_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @upstream_sync_stuck_batch_expected_count after previewing exact stuck batches';
  end if;
  if coalesce(@upstream_sync_stuck_batch_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @upstream_sync_stuck_batch_expected_signature after previewing exact stuck batches';
  end if;
end//

drop procedure if exists assert_upstream_sync_stuck_batch_targets//
create procedure assert_upstream_sync_stuck_batch_targets()
begin
  declare v_count int default 0;
  declare v_signature varchar(64) default '';
  declare v_outside_count int default 0;

  select count(1)
    into v_outside_count
  from upstream_system_sync_batch b
  where find_in_set(b.sync_batch_id, @upstream_sync_stuck_batch_ids) > 0
    and b.status <> 'SYNCING';
  if v_outside_count > 0 then
    signal sqlstate '45000' set message_text = 'upstream stuck batch ids include rows that are not SYNCING';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             b.sync_batch_id,
             b.connection_code,
             b.sync_type,
             b.mode,
             b.status,
             date_format(b.started_time, '%Y-%m-%d %H:%i:%s'),
             coalesce(b.error_message, '')
           )
           order by b.sync_batch_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from upstream_system_sync_batch b
  where find_in_set(b.sync_batch_id, @upstream_sync_stuck_batch_ids) > 0;

  if v_count <> cast(@upstream_sync_stuck_batch_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'upstream stuck batch expected count does not match selected rows';
  end if;
  if lower(v_signature) <> lower(@upstream_sync_stuck_batch_expected_signature) then
    signal sqlstate '45000' set message_text = 'upstream stuck batch exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_upstream_sync_stuck_batch_reconcile_confirmed();
call assert_upstream_sync_stuck_batch_targets();

start transaction;

update upstream_system_sync_batch b
set b.status = 'TIMEOUT',
    b.finished_time = coalesce(b.finished_time, sysdate()),
    b.failed_count = greatest(coalesce(b.failed_count, 0), 1),
    b.error_message = '历史同步任务长时间处于SYNCING，按确认方案收口为TIMEOUT'
where find_in_set(b.sync_batch_id, @upstream_sync_stuck_batch_ids) > 0
  and b.status = 'SYNCING';

update upstream_system_sync_state s
join upstream_system_sync_batch b
  on b.connection_code = s.connection_code
 and b.sync_type = s.sync_type
 and b.sync_batch_id = s.sync_batch_id
set s.status = 'TIMEOUT',
    s.last_finished_time = coalesce(s.last_finished_time, sysdate()),
    s.failed_count = greatest(coalesce(s.failed_count, 0), 1),
    s.last_error_code = 'SYNC_TASK_TIMEOUT',
    s.last_error_message = '历史同步任务长时间处于SYNCING，按确认方案收口为TIMEOUT',
    s.update_time = sysdate()
where find_in_set(b.sync_batch_id, @upstream_sync_stuck_batch_ids) > 0
  and s.status = 'SYNCING';

update upstream_system_sku_sync_state s
join upstream_system_sync_batch b
  on b.connection_code = s.connection_code
 and b.sync_batch_id = s.sync_batch_id
set s.status = 'TIMEOUT',
    s.last_finished_time = coalesce(s.last_finished_time, sysdate()),
    s.last_error_message = '历史同步任务长时间处于SYNCING，按确认方案收口为TIMEOUT'
where find_in_set(b.sync_batch_id, @upstream_sync_stuck_batch_ids) > 0
  and s.status = 'SYNCING';

update upstream_system_inventory_sync_state s
join upstream_system_sync_batch b
  on b.connection_code = s.connection_code
 and b.sync_batch_id = s.sync_batch_id
set s.status = 'TIMEOUT',
    s.last_finished_time = coalesce(s.last_finished_time, sysdate()),
    s.last_error_message = '历史同步任务长时间处于SYNCING，按确认方案收口为TIMEOUT'
where find_in_set(b.sync_batch_id, @upstream_sync_stuck_batch_ids) > 0
  and s.status = 'SYNCING';

commit;

drop procedure if exists assert_upstream_sync_stuck_batch_reconcile_confirmed;
drop procedure if exists assert_upstream_sync_stuck_batch_targets;
