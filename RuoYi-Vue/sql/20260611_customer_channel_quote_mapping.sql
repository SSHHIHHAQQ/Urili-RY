-- Customer channel quote upstream channel mapping.
-- Scope: add the quote master-channel mapping table used by customer channel management.

set names utf8mb4;

set @confirm_customer_channel_quote_mapping := coalesce(@confirm_customer_channel_quote_mapping, '');

delimiter //

drop procedure if exists assert_customer_channel_quote_mapping_confirmed//
create procedure assert_customer_channel_quote_mapping_confirmed()
begin
  if coalesce(@confirm_customer_channel_quote_mapping, '')
      <> 'APPLY_CUSTOMER_CHANNEL_QUOTE_MAPPING' then
    signal sqlstate '45000' set message_text = 'set @confirm_customer_channel_quote_mapping = APPLY_CUSTOMER_CHANNEL_QUOTE_MAPPING before running this migration';
  end if;
end//

drop procedure if exists assert_customer_channel_quote_mapping_completed//
create procedure assert_customer_channel_quote_mapping_completed()
begin
  if not exists (
    select 1
    from information_schema.tables
    where table_schema = database()
      and table_name = 'logistics_customer_channel_quote_mapping'
  ) then
    signal sqlstate '45000' set message_text = 'logistics_customer_channel_quote_mapping table was not created';
  end if;

  if not exists (
    select 1
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'logistics_customer_channel_quote_mapping'
      and index_name = 'uk_logistics_customer_quote_channel'
  ) then
    signal sqlstate '45000' set message_text = 'customer channel quote unique index was not created';
  end if;

  if not exists (
    select 1
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'logistics_customer_channel_quote_mapping'
      and index_name = 'idx_logistics_customer_quote_upstream'
  ) then
    signal sqlstate '45000' set message_text = 'customer channel quote upstream index was not created';
  end if;
end//

delimiter ;

call assert_customer_channel_quote_mapping_confirmed();
drop procedure if exists assert_customer_channel_quote_mapping_confirmed;

create table if not exists logistics_customer_channel_quote_mapping (
  mapping_id                        bigint(20)   not null auto_increment   comment 'mapping id',
  customer_channel_code             varchar(64)  not null                  comment 'customer channel code',
  connection_code                   varchar(64)  not null                  comment 'quote master connection code',
  master_warehouse_name_snapshot    varchar(200) not null                  comment 'quote master warehouse name snapshot',
  upstream_channel_code             varchar(100) not null                  comment 'upstream channel code',
  upstream_channel_name             varchar(200) not null                  comment 'upstream channel name snapshot',
  pairing_role                      varchar(32)  not null default 'QUOTE'  comment 'pairing role: quote',
  status                            varchar(16)  not null default 'ENABLED' comment 'status',
  create_by                         varchar(64)  default ''                comment 'created by',
  create_time                       datetime                                comment 'created time',
  update_by                         varchar(64)  default ''                comment 'updated by',
  update_time                       datetime                                comment 'updated time',
  remark                            varchar(500) default ''                comment 'remark',
  primary key (mapping_id),
  unique key uk_logistics_customer_quote_channel (customer_channel_code, pairing_role),
  key idx_logistics_customer_quote_upstream (connection_code, upstream_channel_code, pairing_role)
) engine=innodb comment='customer channel quote upstream channel mapping';

call assert_customer_channel_quote_mapping_completed();
drop procedure if exists assert_customer_channel_quote_mapping_completed;
