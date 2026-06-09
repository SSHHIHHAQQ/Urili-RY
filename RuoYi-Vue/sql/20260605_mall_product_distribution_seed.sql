-- Mall product SPU/SKU seed for the RuoYi validation project.
-- Scope: product_spu/product_sku/product_attribute_value/product_image tables,
-- dictionaries, admin menu component, and product:distribution permissions.
-- This script is prepared for review; confirm active datasource before executing.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_mall_product_distribution_seed := coalesce(@confirm_mall_product_distribution_seed, '');
set @mall_product_distribution_dict_seed_expected_count :=
    coalesce(@mall_product_distribution_dict_seed_expected_count, '');
set @mall_product_distribution_dict_seed_expected_signature :=
    coalesce(@mall_product_distribution_dict_seed_expected_signature, '');
set @mall_product_distribution_disabled_status_expected_count :=
    coalesce(@mall_product_distribution_disabled_status_expected_count, '');
set @mall_product_distribution_disabled_status_expected_signature :=
    coalesce(@mall_product_distribution_disabled_status_expected_signature, '');
set @mall_product_distribution_menu_seed_expected_count :=
    coalesce(@mall_product_distribution_menu_seed_expected_count, '');
set @mall_product_distribution_menu_seed_expected_signature :=
    coalesce(@mall_product_distribution_menu_seed_expected_signature, '');

delimiter //

drop procedure if exists assert_mall_product_distribution_seed_confirmed//
create procedure assert_mall_product_distribution_seed_confirmed()
begin
  if coalesce(@confirm_mall_product_distribution_seed, '')
      <> 'APPLY_MALL_PRODUCT_DISTRIBUTION_SEED' then
    signal sqlstate '45000' set message_text = 'set @confirm_mall_product_distribution_seed = APPLY_MALL_PRODUCT_DISTRIBUTION_SEED before running this migration';
  end if;
  if coalesce(@mall_product_distribution_dict_seed_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @mall_product_distribution_dict_seed_expected_count after previewing mall product distribution dict seed rows';
  end if;
  if coalesce(@mall_product_distribution_dict_seed_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @mall_product_distribution_dict_seed_expected_signature after previewing mall product distribution dict seed rows';
  end if;
  if coalesce(@mall_product_distribution_disabled_status_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @mall_product_distribution_disabled_status_expected_count after previewing product_sales_status DISABLED rows';
  end if;
  if coalesce(@mall_product_distribution_disabled_status_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @mall_product_distribution_disabled_status_expected_signature after previewing product_sales_status DISABLED rows';
  end if;
  if coalesce(@mall_product_distribution_menu_seed_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @mall_product_distribution_menu_seed_expected_count after previewing mall product distribution menu seed rows';
  end if;
  if coalesce(@mall_product_distribution_menu_seed_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @mall_product_distribution_menu_seed_expected_signature after previewing mall product distribution menu seed rows';
  end if;
end//

drop procedure if exists assert_mall_product_distribution_sys_menu_guard//
create procedure assert_mall_product_distribution_sys_menu_guard()
begin
  if exists (
    select 1
    from sys_menu m
    where exists (
        select 1
        from tmp_mall_product_distribution_sys_menu_guard seed
        where seed.menu_id = m.menu_id
    )
      and not exists (
        select 1
        from tmp_mall_product_distribution_sys_menu_guard seed
        where seed.menu_id = m.menu_id
          and m.parent_id = seed.parent_id
          and coalesce(m.menu_type, '') = coalesce(seed.menu_type, '')
          and coalesce(m.path, '') = coalesce(seed.path, '')
          and coalesce(m.component, '') = coalesce(seed.component, '')
          and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
          and coalesce(m.perms, '') = coalesce(seed.perms, '')
    )
  ) then
    signal sqlstate '45000' set message_text = 'mall product distribution sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_mall_product_distribution_sys_menu_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = coalesce(seed.path, '')
     and coalesce(m.component, '') = coalesce(seed.component, '')
     and coalesce(m.route_name, '') = coalesce(seed.route_name, '')
     and coalesce(m.perms, '') = coalesce(seed.perms, '')
  ) then
    signal sqlstate '45000' set message_text = 'mall product distribution sys_menu signature is already used by another menu';
  end if;
end//

drop procedure if exists assert_mall_product_distribution_count_signature//
create procedure assert_mall_product_distribution_count_signature(
  in p_actual_count bigint,
  in p_actual_signature varchar(64),
  in p_expected_count varchar(64),
  in p_expected_signature varchar(64),
  in p_count_message varchar(255),
  in p_signature_message varchar(255)
)
begin
  if p_actual_count <> cast(p_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = p_count_message;
  end if;
  if lower(p_actual_signature) <> lower(p_expected_signature) then
    signal sqlstate '45000' set message_text = p_signature_message;
  end if;
end//

drop procedure if exists assert_mall_product_distribution_seed_completed//
create procedure assert_mall_product_distribution_seed_completed()
begin
  if (
    select count(1)
    from sys_dict_type t
    where (
        t.dict_type = 'product_sales_status'
        and coalesce(t.dict_name, '') = '商品销售状态'
        and coalesce(t.status, '') = '0'
      )
      or (
        t.dict_type = 'product_control_status'
        and coalesce(t.dict_name, '') = '商品管控状态'
        and coalesce(t.status, '') = '0'
      )
      or (
        t.dict_type = 'product_source_type'
        and coalesce(t.dict_name, '') = '商品创建来源'
        and coalesce(t.status, '') = '0'
      )
  ) <> 3 then
    signal sqlstate '45000' set message_text = 'mall product distribution dict type seed completion mismatch';
  end if;

  if (
    select count(1)
    from sys_dict_data d
    where (
        d.dict_type = 'product_sales_status'
        and d.dict_value = 'DRAFT'
        and coalesce(d.dict_label, '') = '草稿'
        and coalesce(d.list_class, '') = 'default'
        and coalesce(d.is_default, '') = 'Y'
        and coalesce(d.status, '') = '0'
      )
      or (
        d.dict_type = 'product_sales_status'
        and d.dict_value = 'READY'
        and coalesce(d.dict_label, '') = '待上架'
        and coalesce(d.list_class, '') = 'warning'
        and coalesce(d.is_default, '') = 'N'
        and coalesce(d.status, '') = '0'
      )
      or (
        d.dict_type = 'product_sales_status'
        and d.dict_value = 'ON_SALE'
        and coalesce(d.dict_label, '') = '已上架'
        and coalesce(d.list_class, '') = 'success'
        and coalesce(d.is_default, '') = 'N'
        and coalesce(d.status, '') = '0'
      )
      or (
        d.dict_type = 'product_sales_status'
        and d.dict_value = 'OFF_SALE'
        and coalesce(d.dict_label, '') = '已下架'
        and coalesce(d.list_class, '') = 'info'
        and coalesce(d.is_default, '') = 'N'
        and coalesce(d.status, '') = '0'
      )
      or (
        d.dict_type = 'product_control_status'
        and d.dict_value = 'NORMAL'
        and coalesce(d.dict_label, '') = '正常'
        and coalesce(d.list_class, '') = 'success'
        and coalesce(d.is_default, '') = 'Y'
        and coalesce(d.status, '') = '0'
      )
      or (
        d.dict_type = 'product_control_status'
        and d.dict_value = 'DISABLED'
        and coalesce(d.dict_label, '') = '停用'
        and coalesce(d.list_class, '') = 'danger'
        and coalesce(d.is_default, '') = 'N'
        and coalesce(d.status, '') = '0'
      )
      or (
        d.dict_type = 'product_source_type'
        and d.dict_value = 'ADMIN_MANUAL'
        and coalesce(d.dict_label, '') = '管理端手工创建'
        and coalesce(d.list_class, '') = 'primary'
        and coalesce(d.is_default, '') = 'Y'
        and coalesce(d.status, '') = '0'
      )
      or (
        d.dict_type = 'product_source_type'
        and d.dict_value = 'SELLER_SUBMIT'
        and coalesce(d.dict_label, '') = '卖家提交'
        and coalesce(d.list_class, '') = 'success'
        and coalesce(d.is_default, '') = 'N'
        and coalesce(d.status, '') = '0'
      )
      or (
        d.dict_type = 'product_source_type'
        and d.dict_value = 'SOURCE_PRODUCT'
        and coalesce(d.dict_label, '') = '来源商品库生成'
        and coalesce(d.list_class, '') = 'info'
        and coalesce(d.is_default, '') = 'N'
        and coalesce(d.status, '') = '0'
      )
  ) <> 9 then
    signal sqlstate '45000' set message_text = 'mall product distribution dict data seed completion mismatch';
  end if;

  if exists (
    select 1
    from sys_dict_data d
    where d.dict_type = 'product_sales_status'
      and d.dict_value = 'DISABLED'
      and (
        coalesce(d.status, '') <> '1'
        or coalesce(d.remark, '') <> '停用已拆分为商品管控状态，不再作为销售状态'
      )
  ) then
    signal sqlstate '45000' set message_text = 'mall product distribution disabled status completion mismatch';
  end if;

  if (
    select count(1)
    from sys_menu m
    join tmp_mall_product_distribution_seed_expected seed on seed.menu_id = m.menu_id
    where coalesce(m.menu_name, '') = seed.menu_name
      and coalesce(m.parent_id, -1) = seed.parent_id
      and coalesce(m.order_num, -1) = seed.order_num
      and coalesce(m.menu_type, '') = seed.menu_type
      and coalesce(m.visible, '') = seed.visible
      and coalesce(m.status, '') = seed.status
      and coalesce(m.path, '') = seed.path
      and coalesce(m.component, '') = seed.component
      and coalesce(m.route_name, '') = seed.route_name
      and coalesce(m.perms, '') = seed.perms
      and coalesce(m.icon, '') = seed.icon
      and coalesce(m.remark, '') = seed.remark
  ) <> (select count(1) from tmp_mall_product_distribution_seed_expected) then
    signal sqlstate '45000' set message_text = 'mall product distribution sys_menu seed completion mismatch';
  end if;
end//

delimiter ;

call assert_mall_product_distribution_seed_confirmed();
drop procedure if exists assert_mall_product_distribution_seed_confirmed;

create table if not exists product_spu (
  spu_id           bigint(20)    not null auto_increment comment 'SPU主键',
  system_spu_code  varchar(64)   not null                comment '系统SPU编码',
  seller_spu_code  varchar(128)  default ''              comment '客户SPU编码',
  seller_id        bigint(20)    not null                comment '绑定卖家ID',
  seller_no        varchar(64)   default ''              comment '卖家编号快照',
  seller_name      varchar(255)  default ''              comment '卖家名称快照',
  category_id      bigint(20)    not null                comment '商品分类ID',
  category_code    varchar(64)   default ''              comment '商品分类编码快照',
  category_name    varchar(255)  default ''              comment '商品分类名称快照',
  product_name     varchar(255)  not null                comment '商品中文标题',
  product_name_en  varchar(255)  not null default ''     comment '商品英文标题',
  selling_point    varchar(500)  default ''              comment '商品卖点',
  main_image_url   varchar(1000) default ''              comment 'SPU主图资源路径',
  detail_content   text                                  comment '商品详情文本',
  spu_status       varchar(32)   not null default 'DRAFT' comment 'SPU销售状态',
  control_status   varchar(32)   not null default 'NORMAL' comment 'SPU管控状态：NORMAL正常，DISABLED停用',
  control_reason   varchar(500)  default null            comment '最近一次停用原因',
  control_by       varchar(64)   default null            comment '最近一次停用操作人',
  control_time     datetime                              comment '最近一次停用时间',
  recover_by       varchar(64)   default null            comment '最近一次恢复操作人',
  recover_time     datetime                              comment '最近一次恢复时间',
  source_type      varchar(32)   not null default 'ADMIN_MANUAL' comment '创建来源',
  source_ref_type  varchar(32)   default ''              comment '来源对象类型',
  source_ref_id    varchar(128)  default ''              comment '来源对象ID',
  del_flag         char(1)       not null default '0'    comment '删除标志：0存在 2删除',
  create_by        varchar(64)   default ''              comment '创建者',
  create_time      datetime                              comment '创建时间',
  update_by        varchar(64)   default ''              comment '更新者',
  update_time      datetime                              comment '更新时间',
  remark           varchar(500)  default ''              comment '备注',
  primary key (spu_id),
  unique key uk_product_spu_system_code (system_spu_code),
  key idx_product_spu_seller (seller_id),
  key idx_product_spu_seller_code (seller_id, seller_spu_code),
  key idx_product_spu_category (category_id),
  key idx_product_spu_status (spu_status),
  key idx_product_spu_control_status (control_status),
  key idx_product_spu_status_control (spu_status, control_status),
  key idx_product_spu_source (source_type),
  key idx_product_spu_update_time (update_time)
) engine=innodb auto_increment=1 comment='商城商品SPU表';

create table if not exists product_sku (
  sku_id            bigint(20)    not null auto_increment comment 'SKU主键',
  spu_id            bigint(20)    not null                comment 'SPU主键',
  seller_id         bigint(20)    not null                comment '绑定卖家ID',
  system_sku_code   varchar(64)   not null                comment '系统SKU编码',
  seller_sku_code   varchar(128)  default ''              comment '客户SKU编码',
  color             varchar(128)  default ''              comment '颜色',
  size              varchar(128)  default ''              comment '尺寸',
  length_value      varchar(128)  default ''              comment '长度，含单位文本',
  width_value       varchar(128)  default ''              comment '宽度，含单位文本',
  height_value      varchar(128)  default ''              comment '高度，含单位文本',
  weight            varchar(128)  default ''              comment '重量，含单位文本',
  material          varchar(128)  default ''              comment '材质',
  style             varchar(128)  default ''              comment '风格',
  model             varchar(128)  default ''              comment '型号',
  package_quantity  varchar(128)  default ''              comment '商品数量或包装数量',
  capacity          varchar(128)  default ''              comment '容量，含单位文本',
  sku_image_url     varchar(1000) default ''              comment 'SKU主图或规格示意图',
  supply_price      decimal(18,4) not null                comment '供货价',
  sale_price        decimal(18,4)                         comment '销售价',
  currency_code     varchar(16)   not null                comment '币种code',
  sku_status        varchar(32)   not null default 'DRAFT' comment 'SKU销售状态',
  control_status    varchar(32)   not null default 'NORMAL' comment 'SKU管控状态：NORMAL正常，DISABLED停用',
  control_reason    varchar(500)  default null            comment '最近一次停用原因',
  control_by        varchar(64)   default null            comment '最近一次停用操作人',
  control_time      datetime                              comment '最近一次停用时间',
  recover_by        varchar(64)   default null            comment '最近一次恢复操作人',
  recover_time      datetime                              comment '最近一次恢复时间',
  sort_order        int           not null default 0      comment '显示排序',
  del_flag          char(1)       not null default '0'    comment '删除标志：0存在 2删除',
  create_by         varchar(64)   default ''              comment '创建者',
  create_time       datetime                              comment '创建时间',
  update_by         varchar(64)   default ''              comment '更新者',
  update_time       datetime                              comment '更新时间',
  remark            varchar(500)  default ''              comment '备注',
  primary key (sku_id),
  unique key uk_product_sku_system_code (system_sku_code),
  key idx_product_sku_spu (spu_id),
  key idx_product_sku_seller (seller_id),
  key idx_product_sku_seller_code (seller_id, seller_sku_code),
  key idx_product_sku_status (sku_status),
  key idx_product_sku_control_status (control_status),
  key idx_product_sku_status_control (sku_status, control_status),
  key idx_product_sku_currency (currency_code),
  key idx_product_sku_update_time (update_time)
) engine=innodb auto_increment=1 comment='商城商品SKU表';

create table if not exists product_attribute_value (
  value_id                bigint(20)   not null auto_increment comment '属性值主键',
  owner_type              varchar(16)  not null default 'SPU'   comment '归属类型：SPU/SKU',
  owner_id                bigint(20)   not null                 comment '归属对象ID',
  spu_id                  bigint(20)   not null                 comment 'SPU主键',
  category_id             bigint(20)   not null                 comment '商品分类ID',
  category_schema_version int          default null             comment '类目属性规则版本',
  attribute_id            bigint(20)   not null                 comment '属性ID',
  attribute_code          varchar(64)  not null                 comment '属性编码快照',
  attribute_name          varchar(128) default ''               comment '属性名称快照',
  attribute_type          varchar(32)  not null                 comment '属性类型快照',
  value_code              varchar(128) default ''               comment '单选值code',
  value_text              text                                  comment '文本值',
  value_number            decimal(18,4)                         comment '数值',
  value_date              date                                  comment '日期值',
  value_json              text                                  comment '多选、文件等复杂值JSON',
  create_by               varchar(64)  default ''               comment '创建者',
  create_time             datetime                              comment '创建时间',
  update_by               varchar(64)  default ''               comment '更新者',
  update_time             datetime                              comment '更新时间',
  remark                  varchar(500) default ''               comment '备注',
  primary key (value_id),
  unique key uk_product_attribute_value_owner (owner_type, owner_id, attribute_id),
  key idx_product_attribute_value_spu (spu_id),
  key idx_product_attribute_value_category (category_id),
  key idx_product_attribute_value_code (attribute_code)
) engine=innodb auto_increment=1 comment='商城商品类目属性值表';

create table if not exists product_image (
  image_id    bigint(20)    not null auto_increment comment '图片主键',
  owner_type  varchar(16)   not null                comment '归属类型：SPU/SKU',
  owner_id    bigint(20)    not null                comment '归属对象ID',
  spu_id      bigint(20)    not null                comment 'SPU主键',
  sku_id      bigint(20)    default null            comment 'SKU主键',
  image_url   varchar(1000) not null                comment '图片资源路径',
  image_role  varchar(32)   not null                comment '图片角色：MAIN/GALLERY/SKU_MAIN',
  sort_order  int           not null default 0      comment '显示排序',
  create_by   varchar(64)   default ''              comment '创建者',
  create_time datetime                              comment '创建时间',
  primary key (image_id),
  key idx_product_image_owner (owner_type, owner_id),
  key idx_product_image_spu (spu_id),
  key idx_product_image_sku (sku_id),
  key idx_product_image_role (image_role)
) engine=innodb auto_increment=1 comment='商城商品图片表';

create table if not exists product_distribution_operation_log (
  log_id                bigint        not null auto_increment comment '日志ID',
  batch_no              varchar(64)   not null                comment '批量操作批次号',
  operation_type        varchar(32)   not null                comment '操作类型',
  owner_type            varchar(16)   not null                comment '对象类型：SPU/SKU',
  spu_id                bigint        not null                comment 'SPU ID',
  sku_id                bigint        default null            comment 'SKU ID',
  system_spu_code       varchar(64)   default null            comment '系统SPU编码快照',
  system_sku_code       varchar(64)   default null            comment '系统SKU编码快照',
  seller_id             bigint        default null            comment '卖家ID快照',
  seller_name           varchar(128)  default null            comment '卖家名称快照',
  before_sales_status   varchar(32)   default null            comment '操作前销售状态',
  after_sales_status    varchar(32)   default null            comment '操作后销售状态',
  before_control_status varchar(32)   default null            comment '操作前管控状态',
  after_control_status  varchar(32)   default null            comment '操作后管控状态',
  before_sale_price     decimal(18,4) default null            comment '操作前销售价',
  after_sale_price      decimal(18,4) default null            comment '操作后销售价',
  currency_code         varchar(16)   default null            comment '币种快照',
  reason                varchar(500)  default null            comment '操作原因',
  change_summary        varchar(500)  default null            comment '操作摘要',
  diff_json             longtext                              comment '字段差异JSON',
  operator_name         varchar(64)   not null                comment '操作人账号',
  operation_time        datetime      not null default current_timestamp comment '操作时间',
  operation_source      varchar(32)   not null default 'PAGE' comment '操作来源',
  remark                varchar(500)  default null            comment '备注',
  primary key (log_id),
  key idx_product_dist_log_batch (batch_no),
  key idx_product_dist_log_spu (spu_id, operation_time),
  key idx_product_dist_log_sku (sku_id, operation_time),
  key idx_product_dist_log_type (operation_type, operation_time),
  key idx_product_dist_log_operator (operator_name, operation_time)
) engine=innodb auto_increment=1 comment='商城商品业务操作日志';

create temporary table if not exists tmp_mall_product_distribution_write_targets (
  target_group varchar(64) not null,
  target_key varchar(255) not null,
  target_signature varchar(64) not null,
  key idx_mall_product_distribution_write_targets (target_group, target_key)
) engine=memory;

truncate table tmp_mall_product_distribution_write_targets;

insert into tmp_mall_product_distribution_write_targets(target_group, target_key, target_signature) values
  ('DICT_SEED', 'TYPE:product_sales_status', sha2('TYPE:product_sales_status:商品销售状态', 256)),
  ('DICT_SEED', 'DATA:product_sales_status:DRAFT', sha2('DATA:product_sales_status:DRAFT:草稿:default:Y', 256)),
  ('DICT_SEED', 'DATA:product_sales_status:READY', sha2('DATA:product_sales_status:READY:待上架:warning:N', 256)),
  ('DICT_SEED', 'DATA:product_sales_status:ON_SALE', sha2('DATA:product_sales_status:ON_SALE:已上架:success:N', 256)),
  ('DICT_SEED', 'DATA:product_sales_status:OFF_SALE', sha2('DATA:product_sales_status:OFF_SALE:已下架:info:N', 256)),
  ('DICT_SEED', 'TYPE:product_control_status', sha2('TYPE:product_control_status:商品管控状态', 256)),
  ('DICT_SEED', 'DATA:product_control_status:NORMAL', sha2('DATA:product_control_status:NORMAL:正常:success:Y', 256)),
  ('DICT_SEED', 'DATA:product_control_status:DISABLED', sha2('DATA:product_control_status:DISABLED:停用:danger:N', 256)),
  ('DICT_SEED', 'TYPE:product_source_type', sha2('TYPE:product_source_type:商品创建来源', 256)),
  ('DICT_SEED', 'DATA:product_source_type:ADMIN_MANUAL', sha2('DATA:product_source_type:ADMIN_MANUAL:管理端手工创建:primary:Y', 256)),
  ('DICT_SEED', 'DATA:product_source_type:SELLER_SUBMIT', sha2('DATA:product_source_type:SELLER_SUBMIT:卖家提交:success:N', 256)),
  ('DICT_SEED', 'DATA:product_source_type:SOURCE_PRODUCT', sha2('DATA:product_source_type:SOURCE_PRODUCT:来源商品库生成:info:N', 256));

insert into tmp_mall_product_distribution_write_targets(target_group, target_key, target_signature)
select 'DISABLED_STATUS',
       concat_ws(':', 'DATA', dict_code, dict_type, dict_value),
       sha2(concat_ws(':',
         dict_code,
         coalesce(dict_sort, ''),
         coalesce(dict_label, ''),
         coalesce(dict_value, ''),
         coalesce(dict_type, ''),
         coalesce(list_class, ''),
         coalesce(status, '')
       ), 256)
from sys_dict_data
where dict_type = 'product_sales_status'
  and dict_value = 'DISABLED';

select count(1),
       sha2(coalesce(group_concat(concat_ws(':', target_key, target_signature) order by target_key separator '|'), ''), 256)
  into @mall_product_distribution_dict_seed_actual_count,
       @mall_product_distribution_dict_seed_actual_signature
from tmp_mall_product_distribution_write_targets
where target_group = 'DICT_SEED';

call assert_mall_product_distribution_count_signature(
  @mall_product_distribution_dict_seed_actual_count,
  @mall_product_distribution_dict_seed_actual_signature,
  @mall_product_distribution_dict_seed_expected_count,
  @mall_product_distribution_dict_seed_expected_signature,
  'mall product distribution dict seed exact target count mismatch',
  'mall product distribution dict seed exact target signature mismatch'
);

select count(1),
       sha2(coalesce(group_concat(concat_ws(':', target_key, target_signature) order by target_key separator '|'), ''), 256)
  into @mall_product_distribution_disabled_status_actual_count,
       @mall_product_distribution_disabled_status_actual_signature
from tmp_mall_product_distribution_write_targets
where target_group = 'DISABLED_STATUS';

call assert_mall_product_distribution_count_signature(
  @mall_product_distribution_disabled_status_actual_count,
  @mall_product_distribution_disabled_status_actual_signature,
  @mall_product_distribution_disabled_status_expected_count,
  @mall_product_distribution_disabled_status_expected_signature,
  'mall product distribution DISABLED exact target count mismatch',
  'mall product distribution DISABLED exact target signature mismatch'
);

create temporary table if not exists tmp_mall_product_distribution_sys_menu_guard (
  menu_id    bigint       not null,
  parent_id  bigint       not null,
  menu_type  char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  key idx_mall_product_distribution_sys_menu_guard_id (menu_id)
) engine=memory;

create temporary table if not exists tmp_mall_product_distribution_seed_expected (
  menu_id    bigint       not null,
  menu_name  varchar(50)  not null,
  parent_id  bigint       not null,
  order_num  int          not null,
  menu_type  char(1)      not null,
  visible    char(1)      not null,
  status     char(1)      not null,
  path       varchar(200) not null default '',
  component  varchar(255) not null default '',
  route_name varchar(50)  not null default '',
  perms      varchar(100) not null default '',
  icon       varchar(100) not null default '',
  remark     varchar(500) not null default '',
  key idx_mall_product_distribution_seed_expected_id (menu_id)
) engine=memory;

truncate table tmp_mall_product_distribution_sys_menu_guard;
truncate table tmp_mall_product_distribution_seed_expected;

insert into tmp_mall_product_distribution_sys_menu_guard(menu_id, parent_id, menu_type, path, component, route_name, perms) values
    (2402, 2060, 'C', 'distribution', 'Product/Distribution/index', 'DistributionProduct', 'product:distribution:list'),
    (2481, 2402, 'F', '#', '', '', 'product:distribution:query'),
    (2482, 2402, 'F', '#', '', '', 'product:distribution:add'),
    (2483, 2402, 'F', '#', '', '', 'product:distribution:edit'),
    (2484, 2402, 'F', '#', '', '', 'product:distribution:status'),
    (2485, 2402, 'F', '#', '', '', 'product:distribution:price'),
    (2486, 2402, 'F', '#', '', '', 'product:distribution:log');

insert into tmp_mall_product_distribution_seed_expected
    (menu_id, menu_name, parent_id, order_num, menu_type, visible, status,
     path, component, route_name, perms, icon, remark)
values
    (2402, '商城商品列表', 2060, 15, 'C', '0', '0', 'distribution', 'Product/Distribution/index', 'DistributionProduct', 'product:distribution:list', 'ShareAltOutlined', '商品管理菜单：商城商品列表'),
    (2481, '商城商品查询', 2402, 1, 'F', '0', '0', '#', '', '', 'product:distribution:query', '#', '商城商品按钮：查询'),
    (2482, '商城商品新增', 2402, 2, 'F', '0', '0', '#', '', '', 'product:distribution:add', '#', '商城商品按钮：新增'),
    (2483, '商城商品修改', 2402, 3, 'F', '0', '0', '#', '', '', 'product:distribution:edit', '#', '商城商品按钮：修改'),
    (2484, '商城商品状态', 2402, 4, 'F', '0', '0', '#', '', '', 'product:distribution:status', '#', '商城商品按钮：状态切换'),
    (2485, '商城商品调价', 2402, 5, 'F', '0', '0', '#', '', '', 'product:distribution:price', '#', '商城商品按钮：调整销售价'),
    (2486, '商城商品操作日志', 2402, 6, 'F', '0', '0', '#', '', '', 'product:distribution:log', '#', '商城商品按钮：操作日志');

call assert_mall_product_distribution_sys_menu_guard();

insert into tmp_mall_product_distribution_write_targets(target_group, target_key, target_signature)
select 'MENU_SEED',
       concat_ws(':', 'MENU', menu_id, parent_id, menu_type, path, component, route_name, perms),
       sha2(concat_ws(':', menu_id, parent_id, menu_type, path, component, route_name, perms), 256)
from tmp_mall_product_distribution_sys_menu_guard;

select count(1),
       sha2(coalesce(group_concat(concat_ws(':', target_key, target_signature) order by target_key separator '|'), ''), 256)
  into @mall_product_distribution_menu_seed_actual_count,
       @mall_product_distribution_menu_seed_actual_signature
from tmp_mall_product_distribution_write_targets
where target_group = 'MENU_SEED';

call assert_mall_product_distribution_count_signature(
  @mall_product_distribution_menu_seed_actual_count,
  @mall_product_distribution_menu_seed_actual_signature,
  @mall_product_distribution_menu_seed_expected_count,
  @mall_product_distribution_menu_seed_expected_signature,
  'mall product distribution menu seed exact target count mismatch',
  'mall product distribution menu seed exact target signature mismatch'
);

start transaction;

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '商品销售状态', 'product_sales_status', '0', 'admin', sysdate(), '', null, '商城商品SPU/SKU销售状态'
where not exists (select 1 from sys_dict_type where dict_type = 'product_sales_status');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'product_sales_status', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '商品销售状态'
from (
    select 1 as dict_sort, '草稿' as dict_label, 'DRAFT' as dict_value, 'default' as list_class, 'Y' as is_default
    union all select 2, '待上架', 'READY', 'warning', 'N'
    union all select 3, '已上架', 'ON_SALE', 'success', 'N'
    union all select 4, '已下架', 'OFF_SALE', 'info', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'product_sales_status' and d.dict_value = seed.dict_value);

update sys_dict_data
set status = '1',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '停用已拆分为商品管控状态，不再作为销售状态'
where dict_type = 'product_sales_status'
  and dict_value = 'DISABLED';

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '商品管控状态', 'product_control_status', '0', 'admin', sysdate(), '', null, '商城商品SPU/SKU独立管控状态'
where not exists (select 1 from sys_dict_type where dict_type = 'product_control_status');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'product_control_status', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '商品管控状态'
from (
    select 1 as dict_sort, '正常' as dict_label, 'NORMAL' as dict_value, 'success' as list_class, 'Y' as is_default
    union all select 2, '停用', 'DISABLED', 'danger', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'product_control_status' and d.dict_value = seed.dict_value);

insert into sys_dict_type
    (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
select '商品创建来源', 'product_source_type', '0', 'admin', sysdate(), '', null, '商城商品创建来源'
where not exists (select 1 from sys_dict_type where dict_type = 'product_source_type');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
select seed.dict_sort, seed.dict_label, seed.dict_value, 'product_source_type', '', seed.list_class, seed.is_default, '0', 'admin', sysdate(), '', null, '商品创建来源'
from (
    select 1 as dict_sort, '管理端手工创建' as dict_label, 'ADMIN_MANUAL' as dict_value, 'primary' as list_class, 'Y' as is_default
    union all select 2, '卖家提交', 'SELLER_SUBMIT', 'success', 'N'
    union all select 3, '来源商品库生成', 'SOURCE_PRODUCT', 'info', 'N'
) seed
where not exists (select 1 from sys_dict_data d where d.dict_type = 'product_source_type' and d.dict_value = seed.dict_value);

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
values
    (2402, '商城商品列表', 2060, 15, 'distribution', 'Product/Distribution/index', '', 'DistributionProduct',
     1, 0, 'C', '0', '0', 'product:distribution:list', 'ShareAltOutlined', 'admin',
     sysdate(), '', null, '商品管理菜单：商城商品列表')
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

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
select seed.menu_id, seed.menu_name, 2402, seed.order_num, '#', '', '', '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin',
       sysdate(), '', null, seed.remark
from (
    select 2481 as menu_id, '商城商品查询' as menu_name, 1 as order_num, 'product:distribution:query' as perms, '商城商品按钮：查询' as remark
    union all select 2482, '商城商品新增', 2, 'product:distribution:add', '商城商品按钮：新增'
    union all select 2483, '商城商品修改', 3, 'product:distribution:edit', '商城商品按钮：修改'
    union all select 2484, '商城商品状态', 4, 'product:distribution:status', '商城商品按钮：状态切换'
    union all select 2485, '商城商品调价', 5, 'product:distribution:price', '商城商品按钮：调整销售价'
    union all select 2486, '商城商品操作日志', 6, 'product:distribution:log', '商城商品按钮：操作日志'
) seed
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

call assert_mall_product_distribution_seed_completed();

commit;

drop temporary table if exists tmp_mall_product_distribution_write_targets;
drop temporary table if exists tmp_mall_product_distribution_seed_expected;
drop temporary table if exists tmp_mall_product_distribution_sys_menu_guard;
drop procedure if exists assert_mall_product_distribution_seed_completed;
drop procedure if exists assert_mall_product_distribution_sys_menu_guard;
drop procedure if exists assert_mall_product_distribution_count_signature;
