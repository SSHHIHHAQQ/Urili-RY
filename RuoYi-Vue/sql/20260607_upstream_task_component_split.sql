-- 上游系统定时任务组件拆分迁移。
-- 目的：让定时任务菜单中的调用目标不再全部挤在 upstreamSystemTask 上。

set names utf8mb4;

set @confirm_upstream_task_component_split := coalesce(@confirm_upstream_task_component_split, '');

delimiter //

drop procedure if exists assert_upstream_task_component_split_confirmed//
create procedure assert_upstream_task_component_split_confirmed()
begin
  if coalesce(@confirm_upstream_task_component_split, '')
      <> 'APPLY_UPSTREAM_TASK_COMPONENT_SPLIT' then
    signal sqlstate '45000' set message_text = 'set @confirm_upstream_task_component_split = APPLY_UPSTREAM_TASK_COMPONENT_SPLIT before running this migration';
  end if;
end//

delimiter ;

call assert_upstream_task_component_split_confirmed();
drop procedure if exists assert_upstream_task_component_split_confirmed;

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
