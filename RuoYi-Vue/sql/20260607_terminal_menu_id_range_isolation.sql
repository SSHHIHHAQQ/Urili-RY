-- Terminal menu ID range isolation.
-- Scope: seller_menu, buyer_menu and their role-menu bindings only.
-- Purpose: keep seller/buyer numeric menuIds in disjoint ranges while preserving the existing number[] API contract.

set names utf8mb4;
set @confirm_terminal_menu_id_range_isolation := coalesce(@confirm_terminal_menu_id_range_isolation, '');

delimiter //

drop procedure if exists assert_terminal_menu_id_range_isolation_confirmed//
create procedure assert_terminal_menu_id_range_isolation_confirmed()
begin
  if coalesce(@confirm_terminal_menu_id_range_isolation, '')
      <> 'APPLY_TERMINAL_MENU_ID_RANGE_ISOLATION' then
    signal sqlstate '45000' set message_text = 'set @confirm_terminal_menu_id_range_isolation = APPLY_TERMINAL_MENU_ID_RANGE_ISOLATION before running this migration';
  end if;
end//

drop procedure if exists assert_table_exists//
create procedure assert_table_exists(in p_table varchar(64), in p_message varchar(255))
begin
  if not exists (
    select 1
    from information_schema.tables
    where table_schema = database()
      and table_name = p_table
  ) then
    signal sqlstate '45000' set message_text = p_message;
  end if;
end//

drop procedure if exists assert_no_terminal_menu_orphans//
create procedure assert_no_terminal_menu_orphans()
begin
  if exists (
    select 1
    from seller_role_menu rm
    left join seller_menu m on m.seller_menu_id = rm.seller_menu_id
    where m.seller_menu_id is null
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'seller_role_menu has orphan seller_menu_id values';
  end if;

  if exists (
    select 1
    from buyer_role_menu rm
    left join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
    where m.buyer_menu_id is null
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'buyer_role_menu has orphan buyer_menu_id values';
  end if;
end//

drop procedure if exists assert_terminal_menu_ids_are_migratable//
create procedure assert_terminal_menu_ids_are_migratable()
begin
  if exists (
    select 1
    from seller_menu
    where seller_menu_id >= 200000
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu contains IDs outside seller range 100000-199999';
  end if;

  if exists (
    select 1
    from buyer_menu
    where buyer_menu_id >= 100000
      and buyer_menu_id < 200000
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu contains IDs inside reserved seller range 100000-199999';
  end if;

  if exists (
    select 1
    from buyer_menu
    where buyer_menu_id >= 300000
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu contains IDs outside buyer range 200000-299999';
  end if;

  if exists (
    select 1
    from seller_menu old_menu
    join seller_menu target_menu
      on target_menu.seller_menu_id = old_menu.seller_menu_id + 100000
    where old_menu.seller_menu_id > 0
      and old_menu.seller_menu_id < 100000
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu low IDs would collide after +100000 migration';
  end if;

  if exists (
    select 1
    from buyer_menu old_menu
    join buyer_menu target_menu
      on target_menu.buyer_menu_id = old_menu.buyer_menu_id + 200000
    where old_menu.buyer_menu_id > 0
      and old_menu.buyer_menu_id < 100000
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu low IDs would collide after +200000 migration';
  end if;
end//

drop procedure if exists assert_terminal_role_menu_ids_are_migratable//
create procedure assert_terminal_role_menu_ids_are_migratable()
begin
  if exists (
    select 1
    from seller_role_menu
    where seller_menu_id >= 200000
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'seller_role_menu contains IDs outside seller range 100000-199999';
  end if;

  if exists (
    select 1
    from buyer_role_menu
    where buyer_menu_id >= 100000
      and buyer_menu_id < 200000
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'buyer_role_menu contains IDs inside reserved seller range 100000-199999';
  end if;

  if exists (
    select 1
    from buyer_role_menu
    where buyer_menu_id >= 300000
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'buyer_role_menu contains IDs outside buyer range 200000-299999';
  end if;

  if exists (
    select 1
    from seller_role_menu old_rm
    join seller_role_menu target_rm
      on target_rm.seller_role_id = old_rm.seller_role_id
     and target_rm.seller_menu_id = old_rm.seller_menu_id + 100000
    where old_rm.seller_menu_id > 0
      and old_rm.seller_menu_id < 100000
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'seller_role_menu low IDs would collide after +100000 migration';
  end if;

  if exists (
    select 1
    from buyer_role_menu old_rm
    join buyer_role_menu target_rm
      on target_rm.buyer_role_id = old_rm.buyer_role_id
     and target_rm.buyer_menu_id = old_rm.buyer_menu_id + 200000
    where old_rm.buyer_menu_id > 0
      and old_rm.buyer_menu_id < 100000
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'buyer_role_menu low IDs would collide after +200000 migration';
  end if;
end//

drop procedure if exists reset_terminal_menu_auto_increment//
create procedure reset_terminal_menu_auto_increment(
  in p_table varchar(64),
  in p_id_column varchar(64),
  in p_floor bigint
)
begin
  set @next_terminal_menu_auto_increment = p_floor;
  set @sql = concat(
    'select greatest(', p_floor, ', coalesce(max(', p_id_column, '), 0) + 1) ',
    'into @next_terminal_menu_auto_increment from `', p_table, '`'
  );
  prepare stmt from @sql;
  execute stmt;
  deallocate prepare stmt;

  set @ddl = concat('alter table `', p_table, '` auto_increment = ', @next_terminal_menu_auto_increment);
  prepare stmt from @ddl;
  execute stmt;
  deallocate prepare stmt;
end//

delimiter ;

call assert_terminal_menu_id_range_isolation_confirmed();
call assert_table_exists('seller_menu', 'Run 20260604_three_terminal_isolation_migration.sql before terminal menu ID range isolation');
call assert_table_exists('buyer_menu', 'Run 20260604_three_terminal_isolation_migration.sql before terminal menu ID range isolation');
call assert_table_exists('seller_role_menu', 'Run 20260604_three_terminal_isolation_migration.sql before terminal menu ID range isolation');
call assert_table_exists('buyer_role_menu', 'Run 20260604_three_terminal_isolation_migration.sql before terminal menu ID range isolation');
call assert_no_terminal_menu_orphans();
call assert_terminal_menu_ids_are_migratable();
call assert_terminal_role_menu_ids_are_migratable();

start transaction;

update seller_role_menu
set seller_menu_id = seller_menu_id + 100000
where seller_menu_id > 0
  and seller_menu_id < 100000;

update seller_menu
set parent_id = parent_id + 100000
where parent_id > 0
  and parent_id < 100000;

update seller_menu
set seller_menu_id = seller_menu_id + 100000
where seller_menu_id > 0
  and seller_menu_id < 100000;

update buyer_role_menu
set buyer_menu_id = buyer_menu_id + 200000
where buyer_menu_id > 0
  and buyer_menu_id < 100000;

update buyer_menu
set parent_id = parent_id + 200000
where parent_id > 0
  and parent_id < 100000;

update buyer_menu
set buyer_menu_id = buyer_menu_id + 200000
where buyer_menu_id > 0
  and buyer_menu_id < 100000;

commit;

call reset_terminal_menu_auto_increment('seller_menu', 'seller_menu_id', 100000);
call reset_terminal_menu_auto_increment('buyer_menu', 'buyer_menu_id', 200000);

call assert_no_terminal_menu_orphans();

drop procedure if exists assert_terminal_menu_id_range_isolation_confirmed;
drop procedure if exists assert_table_exists;
drop procedure if exists assert_no_terminal_menu_orphans;
drop procedure if exists assert_terminal_menu_ids_are_migratable;
drop procedure if exists assert_terminal_role_menu_ids_are_migratable;
drop procedure if exists reset_terminal_menu_auto_increment;
