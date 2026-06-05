-- Product configuration change log migration.
-- Scope: product_config_change_log table and historical update_time backfill.
-- This script is prepared for review; do not execute against a remote database without confirmation and backup.

set names utf8mb4;

create table if not exists product_config_change_log (
  log_id          bigint(20)    not null auto_increment comment '修改记录ID',
  biz_type        varchar(32)   not null                comment '业务类型：CATEGORY/ATTRIBUTE/ATTRIBUTE_OPTION/CATEGORY_ATTRIBUTE_RULE',
  biz_id          bigint(20)    not null                comment '业务主键',
  biz_code        varchar(128)  default ''              comment '业务编码',
  biz_name        varchar(256)  default ''              comment '业务名称',
  action_type     varchar(32)   not null                comment '操作类型：CREATE/UPDATE/ENABLE/DISABLE/DELETE',
  action_source   varchar(32)   not null default 'PAGE' comment '操作来源：PAGE页面 IMPORT导入',
  operator_name   varchar(64)   default ''              comment '操作人',
  change_summary  varchar(1000) default ''              comment '变更摘要',
  before_json     longtext                              comment '修改前快照JSON',
  after_json      longtext                              comment '修改后快照JSON',
  diff_json       longtext                              comment '字段差异JSON',
  change_time     datetime      not null                comment '变更时间',
  remark          varchar(500)  default ''              comment '备注',
  primary key (log_id),
  key idx_product_config_change_log_biz (biz_type, biz_id, change_time),
  key idx_product_config_change_log_time (change_time),
  key idx_product_config_change_log_operator (operator_name)
) engine=innodb auto_increment=1 comment='商品配置修改记录表';

-- Optional historical backfill: creation is also an update for update-time semantics.
update product_category
set update_by = if(update_by is null or update_by = '', create_by, update_by),
    update_time = create_time
where update_time is null
  and create_time is not null;

update product_attribute
set update_by = if(update_by is null or update_by = '', create_by, update_by),
    update_time = create_time
where update_time is null
  and create_time is not null;

update product_attribute_option
set update_by = if(update_by is null or update_by = '', create_by, update_by),
    update_time = create_time
where update_time is null
  and create_time is not null;

update product_category_attribute
set update_by = if(update_by is null or update_by = '', create_by, update_by),
    update_time = create_time
where update_time is null
  and create_time is not null;
