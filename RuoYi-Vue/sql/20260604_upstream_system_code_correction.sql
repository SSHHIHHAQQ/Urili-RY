-- Upstream system code correction.
-- Purpose: align RuoYi migration data codes with the original upstream-system project.
-- Scope: system kind and settlement type codes only. No credential data is touched.

set names utf8mb4;

set @confirm_upstream_system_code_correction := coalesce(@confirm_upstream_system_code_correction, '');
set @upstream_system_code_correction_expected_count :=
    coalesce(@upstream_system_code_correction_expected_count, null);
set @upstream_system_code_correction_expected_signature :=
    coalesce(@upstream_system_code_correction_expected_signature, '');
set session group_concat_max_len = 1048576;

delimiter //

drop procedure if exists assert_upstream_system_code_correction_confirmed//
create procedure assert_upstream_system_code_correction_confirmed()
begin
  if coalesce(@confirm_upstream_system_code_correction, '')
      <> 'APPLY_UPSTREAM_SYSTEM_CODE_CORRECTION' then
    signal sqlstate '45000' set message_text = 'set @confirm_upstream_system_code_correction = APPLY_UPSTREAM_SYSTEM_CODE_CORRECTION before running this migration';
  end if;
  if @upstream_system_code_correction_expected_count is null then
    signal sqlstate '45000' set message_text = 'set @upstream_system_code_correction_expected_count after previewing exact upstream code correction rows';
  end if;
  if coalesce(@upstream_system_code_correction_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @upstream_system_code_correction_expected_signature after previewing exact upstream code correction rows';
  end if;
end//

drop procedure if exists assert_upstream_system_code_correction_targets//
create procedure assert_upstream_system_code_correction_targets()
begin
  declare v_count bigint default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(target order by target separator '\n'), ''), 256)
    into v_count, v_signature
  from (
    select concat_ws('|',
             'connection',
             connection_code,
             coalesce(system_kind, '<NULL>'),
             coalesce(settlement_type, '<NULL>'),
             coalesce(master_warehouse_name, '<NULL>'),
             coalesce(status, '<NULL>')
           ) as target
    from upstream_system_connection
    where system_kind = 'LINGXING_WMS'
       or settlement_type in ('UPSTREAM_PAYABLE', 'PLATFORM_ADVANCE')
    union all
    select concat_ws('|',
             'dict',
             dict_code,
             coalesce(cast(dict_sort as char), '<NULL>'),
             coalesce(dict_label, '<NULL>'),
             coalesce(dict_value, '<NULL>'),
             coalesce(dict_type, '<NULL>'),
             coalesce(is_default, '<NULL>'),
             coalesce(status, '<NULL>')
           ) as target
    from sys_dict_data
    where (dict_type = 'upstream_system_kind' and dict_value = 'LINGXING_WMS')
       or (dict_type = 'upstream_settlement_type'
           and dict_value in ('UPSTREAM_PAYABLE', 'PLATFORM_ADVANCE'))
  ) targets;

  if v_count <> @upstream_system_code_correction_expected_count then
    signal sqlstate '45000' set message_text = 'upstream system code correction exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@upstream_system_code_correction_expected_signature) then
    signal sqlstate '45000' set message_text = 'upstream system code correction exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_upstream_system_code_correction_confirmed();
call assert_upstream_system_code_correction_targets();
drop procedure if exists assert_upstream_system_code_correction_confirmed;
drop procedure if exists assert_upstream_system_code_correction_targets;

start transaction;

update upstream_system_connection
set system_kind = 'lingxing-wms'
where system_kind = 'LINGXING_WMS';

update upstream_system_connection
set settlement_type = 'upstream-payable'
where settlement_type = 'UPSTREAM_PAYABLE';

update upstream_system_connection
set settlement_type = 'self-operated-receivable'
where settlement_type = 'PLATFORM_ADVANCE';

delete from sys_dict_data
where dict_type = 'upstream_system_kind'
  and dict_value = 'LINGXING_WMS';

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select 1, '领星WMS', 'lingxing-wms', 'upstream_system_kind', '', '', 'Y', '0', 'admin', sysdate(), '', null, '上游系统类型'
where not exists (select 1 from sys_dict_data where dict_type = 'upstream_system_kind' and dict_value = 'lingxing-wms');

delete from sys_dict_data
where dict_type = 'upstream_settlement_type'
  and dict_value in ('UPSTREAM_PAYABLE', 'PLATFORM_ADVANCE');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'upstream_settlement_type', '', '', seed.is_default, '0', 'admin', sysdate(), '', null, '上游结算类型'
from (
    select 1 as dict_sort, '上游仓（应付）' as dict_label, 'upstream-payable' as dict_value, 'Y' as is_default
    union all select 2, '自营仓（应收）', 'self-operated-receivable', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'upstream_settlement_type' and d.dict_value = seed.dict_value);

commit;
