-- Upstream system code correction.
-- Purpose: align RuoYi migration data codes with the original upstream-system project.
-- Scope: system kind and settlement type codes only. No credential data is touched.

set names utf8mb4;

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
