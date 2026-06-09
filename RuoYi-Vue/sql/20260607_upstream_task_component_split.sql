-- 上游系统定时任务组件拆分迁移。
-- 目的：让定时任务菜单中的调用目标不再全部挤在 upstreamSystemTask 上。

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_upstream_task_component_split := coalesce(@confirm_upstream_task_component_split, '');
set @upstream_task_component_split_expected_count :=
    coalesce(@upstream_task_component_split_expected_count, '');
set @upstream_task_component_split_expected_signature :=
    coalesce(@upstream_task_component_split_expected_signature, '');

delimiter //

drop procedure if exists assert_upstream_task_component_split_confirmed//
create procedure assert_upstream_task_component_split_confirmed()
begin
  if coalesce(@confirm_upstream_task_component_split, '')
      <> 'APPLY_UPSTREAM_TASK_COMPONENT_SPLIT' then
    signal sqlstate '45000' set message_text = 'set @confirm_upstream_task_component_split = APPLY_UPSTREAM_TASK_COMPONENT_SPLIT before running this migration';
  end if;

  if coalesce(@upstream_task_component_split_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @upstream_task_component_split_expected_count after previewing exact sys_job targets';
  end if;
  if coalesce(@upstream_task_component_split_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @upstream_task_component_split_expected_signature after previewing exact sys_job targets';
  end if;
end//

drop procedure if exists assert_upstream_task_component_split_targets//
create procedure assert_upstream_task_component_split_targets()
begin
  declare v_count int default 0;
  declare v_group_count int default 0;
  declare v_signature varchar(64) default '';

  select count(1)
    into v_group_count
  from sys_job
  where invoke_target in (
      'upstreamSystemTask.syncSkus',
      'upstreamSystemTask.syncSkuInfo',
      'upstreamSkuInfoSyncTask.sync'
  );
  if v_group_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream task component split SKU info sys_job target must be unique before update';
  end if;

  select count(1)
    into v_group_count
  from sys_job
  where invoke_target in (
      'upstreamSystemTask.syncInventory',
      'upstreamInventorySyncTask.sync'
  );
  if v_group_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream task component split inventory sys_job target must be unique before update';
  end if;

  select count(1)
    into v_group_count
  from sys_job
  where invoke_target in (
      'upstreamSystemTask.syncWarehouses',
      'upstreamWarehouseSyncTask.sync'
  );
  if v_group_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream task component split warehouse sys_job target must be unique before update';
  end if;

  select count(1)
    into v_group_count
  from sys_job
  where invoke_target in (
      'upstreamSystemTask.syncLogisticsChannels',
      'upstreamLogisticsChannelSyncTask.sync'
  );
  if v_group_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream task component split logistics channel sys_job target must be unique before update';
  end if;

  select count(1)
    into v_group_count
  from sys_job
  where invoke_target in (
      'upstreamSystemTask.syncSkuDimensions',
      'upstreamSkuDimensionSyncTask.sync'
  );
  if v_group_count > 1 then
    signal sqlstate '45000' set message_text = 'upstream task component split SKU dimension sys_job target must be unique before update';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             job_id,
             coalesce(job_name, ''),
             coalesce(job_group, ''),
             coalesce(invoke_target, ''),
             coalesce(cron_expression, ''),
             coalesce(status, '')
           )
           order by job_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from sys_job
  where invoke_target in (
      'upstreamSystemTask.syncSkus',
      'upstreamSystemTask.syncSkuInfo',
      'upstreamSkuInfoSyncTask.sync',
      'upstreamSystemTask.syncInventory',
      'upstreamInventorySyncTask.sync',
      'upstreamSystemTask.syncWarehouses',
      'upstreamWarehouseSyncTask.sync',
      'upstreamSystemTask.syncLogisticsChannels',
      'upstreamLogisticsChannelSyncTask.sync',
      'upstreamSystemTask.syncSkuDimensions',
      'upstreamSkuDimensionSyncTask.sync'
  );

  if v_count <> cast(@upstream_task_component_split_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'sys_job exact target count mismatch before upstream task component split';
  end if;
  if lower(v_signature) <> lower(@upstream_task_component_split_expected_signature) then
    signal sqlstate '45000' set message_text = 'sys_job exact target signature mismatch before upstream task component split';
  end if;
end//

delimiter ;

call assert_upstream_task_component_split_confirmed();
call assert_upstream_task_component_split_targets();
drop procedure if exists assert_upstream_task_component_split_confirmed;
drop procedure if exists assert_upstream_task_component_split_targets;

start transaction;

update sys_job
set invoke_target = 'upstreamSkuInfoSyncTask.sync',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每天23:40同步领星SKU基础信息，不包含SKU仓库尺寸重量。'
where invoke_target in (
    'upstreamSystemTask.syncSkus',
    'upstreamSystemTask.syncSkuInfo',
    'upstreamSkuInfoSyncTask.sync'
);

update sys_job
set invoke_target = 'upstreamInventorySyncTask.sync',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每10分钟增量同步领星SKU库存。'
where invoke_target in (
    'upstreamSystemTask.syncInventory',
    'upstreamInventorySyncTask.sync'
);

update sys_job
set invoke_target = 'upstreamWarehouseSyncTask.sync',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每天23:20同步领星仓库清单。'
where invoke_target in (
    'upstreamSystemTask.syncWarehouses',
    'upstreamWarehouseSyncTask.sync'
);

update sys_job
set invoke_target = 'upstreamLogisticsChannelSyncTask.sync',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每天23:30同步领星物流渠道清单。'
where invoke_target in (
    'upstreamSystemTask.syncLogisticsChannels',
    'upstreamLogisticsChannelSyncTask.sync'
);

update sys_job
set invoke_target = 'upstreamSkuDimensionSyncTask.sync',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '每天23:59限速同步领星SKU仓库尺寸重量。'
where invoke_target in (
    'upstreamSystemTask.syncSkuDimensions',
    'upstreamSkuDimensionSyncTask.sync'
);

commit;
