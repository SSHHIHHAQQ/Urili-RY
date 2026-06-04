-- Portal direct-login audit ticket table.
-- Confirmed scope: remote DDL is allowed for this task.
-- Plaintext one-time tokens are returned only to the caller/Redis payload;
-- this table stores SHA-256 token_hash only.

set names utf8mb4;

create table if not exists portal_direct_login_ticket (
  ticket_id             bigint(20)      not null auto_increment,
  terminal              varchar(20)     not null,
  target_subject_id     bigint(20)      not null,
  target_subject_no     varchar(64)     default '',
  target_account_id     bigint(20)      not null,
  target_user_name      varchar(64)     not null,
  acting_admin_id       bigint(20)      not null,
  acting_admin_name     varchar(64)     not null,
  reason                varchar(255)    default '',
  token_hash            varchar(64)     not null,
  expire_time           datetime        not null,
  used_time             datetime        default null,
  used_ip               varchar(128)    default '',
  status                varchar(20)     not null default 'ISSUED',
  create_by             varchar(64)     default '',
  create_time           datetime,
  update_by             varchar(64)     default '',
  update_time           datetime,
  remark                varchar(500)    default '',
  primary key (ticket_id),
  unique key uk_portal_direct_login_ticket_hash (token_hash),
  key idx_portal_direct_login_ticket_target (terminal, target_subject_id, target_account_id),
  key idx_portal_direct_login_ticket_admin_time (acting_admin_id, create_time),
  key idx_portal_direct_login_ticket_status_expire (status, expire_time)
) engine=innodb auto_increment=1 comment = 'Portal direct-login audit ticket';
