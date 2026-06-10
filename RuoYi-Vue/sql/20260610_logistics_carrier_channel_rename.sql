-- Rename logistics carrier channel wording.
-- Scope: Chinese display names/comments only. No table name, column name, permission, or business data changes.

set names utf8mb4;

set @confirm_logistics_carrier_channel_rename := coalesce(@confirm_logistics_carrier_channel_rename, '');

delimiter //

drop procedure if exists assert_logistics_carrier_channel_rename_confirmed//
create procedure assert_logistics_carrier_channel_rename_confirmed()
begin
  if coalesce(@confirm_logistics_carrier_channel_rename, '')
      <> 'APPLY_LOGISTICS_CARRIER_CHANNEL_RENAME' then
    signal sqlstate '45000' set message_text = 'set @confirm_logistics_carrier_channel_rename = APPLY_LOGISTICS_CARRIER_CHANNEL_RENAME before running this migration';
  end if;
end//

drop procedure if exists assert_logistics_carrier_channel_rename_completed//
create procedure assert_logistics_carrier_channel_rename_completed()
begin
  if exists (
    select 1
    from sys_dict_type
    where dict_type = 'logistics_channel_status'
      and (dict_name like '%候选渠道%' or remark like '%候选渠道%')
  ) then
    signal sqlstate '45000' set message_text = 'logistics channel dict wording rename failed';
  end if;

  if exists (
    select 1
    from sys_menu
    where menu_id in (2514, 2515)
      and (menu_name like '%候选渠道%' or remark like '%候选渠道%')
  ) then
    signal sqlstate '45000' set message_text = 'logistics channel menu wording rename failed';
  end if;
end//

delimiter ;

call assert_logistics_carrier_channel_rename_confirmed();
drop procedure if exists assert_logistics_carrier_channel_rename_confirmed;

alter table logistics_carrier_connection
  modify last_channel_sync_time datetime null comment '最近物流商渠道同步时间';

alter table logistics_carrier_channel_candidate
  modify status varchar(16) not null default 'ACTIVE' comment '物流商渠道状态',
  comment = '物流商渠道表';

alter table logistics_carrier_channel_mapping
  comment = '物流商渠道与系统渠道映射表';

update sys_dict_type
set dict_name = '物流商渠道状态',
    remark = '物流商渠道同步状态',
    update_by = 'admin',
    update_time = sysdate()
where dict_type = 'logistics_channel_status';

update sys_menu
set remark = replace(remark, '候选渠道', '物流商渠道'),
    update_by = 'admin',
    update_time = sysdate()
where menu_id in (2514, 2515);

call assert_logistics_carrier_channel_rename_completed();
drop procedure if exists assert_logistics_carrier_channel_rename_completed;
