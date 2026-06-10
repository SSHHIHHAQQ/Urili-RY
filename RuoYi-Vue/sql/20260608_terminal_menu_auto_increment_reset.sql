-- Terminal menu auto_increment reset.
-- Scope: seller_menu and buyer_menu auto_increment only.
-- Run after 20260607_terminal_menu_id_range_isolation.sql has moved low IDs into final terminal ranges.

set names utf8mb4;
set session group_concat_max_len = 1048576;
set @confirm_terminal_menu_auto_increment_reset := coalesce(@confirm_terminal_menu_auto_increment_reset, '');
set @terminal_menu_auto_increment_seller_expected_count :=
    coalesce(@terminal_menu_auto_increment_seller_expected_count, '');
set @terminal_menu_auto_increment_seller_expected_signature :=
    coalesce(@terminal_menu_auto_increment_seller_expected_signature, '');
set @terminal_menu_auto_increment_buyer_expected_count :=
    coalesce(@terminal_menu_auto_increment_buyer_expected_count, '');
set @terminal_menu_auto_increment_buyer_expected_signature :=
    coalesce(@terminal_menu_auto_increment_buyer_expected_signature, '');

delimiter //

drop procedure if exists assert_terminal_menu_auto_increment_reset_confirmed//
create procedure assert_terminal_menu_auto_increment_reset_confirmed()
begin
  if coalesce(@confirm_terminal_menu_auto_increment_reset, '')
      <> 'APPLY_TERMINAL_MENU_AUTO_INCREMENT_RESET' then
    signal sqlstate '45000' set message_text = 'set @confirm_terminal_menu_auto_increment_reset = APPLY_TERMINAL_MENU_AUTO_INCREMENT_RESET before running this migration';
  end if;

  if coalesce(@terminal_menu_auto_increment_seller_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @terminal_menu_auto_increment_seller_expected_count after previewing exact seller_menu rows';
  end if;
  if coalesce(@terminal_menu_auto_increment_seller_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @terminal_menu_auto_increment_seller_expected_signature after previewing exact seller_menu rows';
  end if;
  if coalesce(@terminal_menu_auto_increment_buyer_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @terminal_menu_auto_increment_buyer_expected_count after previewing exact buyer_menu rows';
  end if;
  if coalesce(@terminal_menu_auto_increment_buyer_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @terminal_menu_auto_increment_buyer_expected_signature after previewing exact buyer_menu rows';
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

drop procedure if exists assert_no_terminal_menu_parent_orphans//
create procedure assert_no_terminal_menu_parent_orphans()
begin
  if exists (
    select 1
    from seller_menu child
    left join seller_menu parent on parent.seller_menu_id = child.parent_id
    where child.parent_id > 0
      and parent.seller_menu_id is null
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu has orphan parent_id values';
  end if;

  if exists (
    select 1
    from buyer_menu child
    left join buyer_menu parent on parent.buyer_menu_id = child.parent_id
    where child.parent_id > 0
      and parent.buyer_menu_id is null
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu has orphan parent_id values';
  end if;
end//

drop procedure if exists assert_terminal_menu_ids_are_in_final_ranges//
create procedure assert_terminal_menu_ids_are_in_final_ranges()
begin
  if exists (
    select 1
    from seller_menu
    where seller_menu_id > 0
      and (seller_menu_id < 100000 or seller_menu_id >= 200000)
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu final IDs must be inside seller range 100000-199999 before auto_increment reset';
  end if;

  if exists (
    select 1
    from seller_menu
    where parent_id > 0
      and (parent_id < 100000 or parent_id >= 200000)
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'seller_menu final parent IDs must be inside seller range 100000-199999 before auto_increment reset';
  end if;

  if exists (
    select 1
    from buyer_menu
    where buyer_menu_id > 0
      and (buyer_menu_id < 200000 or buyer_menu_id >= 300000)
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu final IDs must be inside buyer range 200000-299999 before auto_increment reset';
  end if;

  if exists (
    select 1
    from buyer_menu
    where parent_id > 0
      and (parent_id < 200000 or parent_id >= 300000)
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'buyer_menu final parent IDs must be inside buyer range 200000-299999 before auto_increment reset';
  end if;
end//

drop procedure if exists assert_terminal_role_menu_ids_are_in_final_ranges//
create procedure assert_terminal_role_menu_ids_are_in_final_ranges()
begin
  if exists (
    select 1
    from seller_role_menu
    where seller_menu_id > 0
      and (seller_menu_id < 100000 or seller_menu_id >= 200000)
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'seller_role_menu final menu IDs must be inside seller range 100000-199999 before auto_increment reset';
  end if;

  if exists (
    select 1
    from buyer_role_menu
    where buyer_menu_id > 0
      and (buyer_menu_id < 200000 or buyer_menu_id >= 300000)
    limit 1
  ) then
    signal sqlstate '45000' set message_text = 'buyer_role_menu final menu IDs must be inside buyer range 200000-299999 before auto_increment reset';
  end if;
end//

drop procedure if exists assert_terminal_menu_auto_increment_targets//
create procedure assert_terminal_menu_auto_increment_targets()
begin
  declare v_seller_count bigint default 0;
  declare v_buyer_count bigint default 0;
  declare v_seller_signature varchar(64) default '';
  declare v_buyer_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             seller_menu_id,
             coalesce(parent_id, ''),
             coalesce(menu_name, ''),
             coalesce(menu_type, ''),
             coalesce(path, ''),
             coalesce(component, ''),
             coalesce(perms, '')
           )
           order by seller_menu_id separator '|'
         ), ''), 256)
    into v_seller_count, v_seller_signature
  from seller_menu;

  if v_seller_count <> cast(@terminal_menu_auto_increment_seller_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'seller_menu auto_increment reset exact target count mismatch';
  end if;
  if lower(v_seller_signature) <> lower(@terminal_menu_auto_increment_seller_expected_signature) then
    signal sqlstate '45000' set message_text = 'seller_menu auto_increment reset exact target signature mismatch';
  end if;

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             buyer_menu_id,
             coalesce(parent_id, ''),
             coalesce(menu_name, ''),
             coalesce(menu_type, ''),
             coalesce(path, ''),
             coalesce(component, ''),
             coalesce(perms, '')
           )
           order by buyer_menu_id separator '|'
         ), ''), 256)
    into v_buyer_count, v_buyer_signature
  from buyer_menu;

  if v_buyer_count <> cast(@terminal_menu_auto_increment_buyer_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'buyer_menu auto_increment reset exact target count mismatch';
  end if;
  if lower(v_buyer_signature) <> lower(@terminal_menu_auto_increment_buyer_expected_signature) then
    signal sqlstate '45000' set message_text = 'buyer_menu auto_increment reset exact target signature mismatch';
  end if;
end//

drop procedure if exists reset_terminal_menu_auto_increment//
create procedure reset_terminal_menu_auto_increment(
  in p_table varchar(64),
  in p_id_column varchar(64),
  in p_floor bigint,
  in p_ceiling_exclusive bigint
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

  if @next_terminal_menu_auto_increment >= p_ceiling_exclusive then
    set @terminal_menu_auto_increment_error = concat(
      p_table,
      ' auto_increment would exceed reserved terminal menu ID range'
    );
    signal sqlstate '45000' set message_text = @terminal_menu_auto_increment_error;
  end if;

  set @ddl = concat('alter table `', p_table, '` auto_increment = ', @next_terminal_menu_auto_increment);
  prepare stmt from @ddl;
  execute stmt;
  deallocate prepare stmt;
end//

drop procedure if exists assert_terminal_menu_auto_increment_value//
create procedure assert_terminal_menu_auto_increment_value(
  in p_table varchar(64),
  in p_id_column varchar(64),
  in p_floor bigint,
  in p_ceiling_exclusive bigint
)
begin
  declare v_expected bigint default 0;
  declare v_actual bigint default null;

  set @terminal_menu_auto_increment_expected = p_floor;
  set @sql = concat(
    'select greatest(', p_floor, ', coalesce(max(', p_id_column, '), 0) + 1) ',
    'into @terminal_menu_auto_increment_expected from `', p_table, '`'
  );
  prepare stmt from @sql;
  execute stmt;
  deallocate prepare stmt;

  set v_expected = @terminal_menu_auto_increment_expected;

  if v_expected >= p_ceiling_exclusive then
    set @terminal_menu_auto_increment_error = concat(
      p_table,
      ' expected auto_increment would exceed reserved terminal menu ID range'
    );
    signal sqlstate '45000' set message_text = @terminal_menu_auto_increment_error;
  end if;

  select auto_increment
    into v_actual
  from information_schema.tables
  where table_schema = database()
    and table_name = p_table;

  if v_actual is null or v_actual >= p_ceiling_exclusive then
    set @terminal_menu_auto_increment_error = concat(
      p_table,
      ' auto_increment metadata is outside reserved terminal menu ID range'
    );
    signal sqlstate '45000' set message_text = @terminal_menu_auto_increment_error;
  end if;

  if v_actual > v_expected then
    set @terminal_menu_auto_increment_error = concat(
      p_table,
      ' auto_increment post-assert mismatch'
    );
    signal sqlstate '45000' set message_text = @terminal_menu_auto_increment_error;
  end if;
end//

delimiter ;

call assert_terminal_menu_auto_increment_reset_confirmed();
call assert_table_exists('seller_menu', 'Run terminal menu ID range isolation before terminal menu auto_increment reset');
call assert_table_exists('buyer_menu', 'Run terminal menu ID range isolation before terminal menu auto_increment reset');
call assert_table_exists('seller_role_menu', 'Run terminal menu ID range isolation before terminal menu auto_increment reset');
call assert_table_exists('buyer_role_menu', 'Run terminal menu ID range isolation before terminal menu auto_increment reset');
call assert_no_terminal_menu_orphans();
call assert_no_terminal_menu_parent_orphans();
call assert_terminal_menu_ids_are_in_final_ranges();
call assert_terminal_role_menu_ids_are_in_final_ranges();
call assert_terminal_menu_auto_increment_targets();

call reset_terminal_menu_auto_increment('seller_menu', 'seller_menu_id', 100000, 200000);
call reset_terminal_menu_auto_increment('buyer_menu', 'buyer_menu_id', 200000, 300000);
call assert_terminal_menu_auto_increment_value('seller_menu', 'seller_menu_id', 100000, 200000);
call assert_terminal_menu_auto_increment_value('buyer_menu', 'buyer_menu_id', 200000, 300000);

call assert_no_terminal_menu_orphans();
call assert_no_terminal_menu_parent_orphans();
call assert_terminal_menu_ids_are_in_final_ranges();
call assert_terminal_role_menu_ids_are_in_final_ranges();

drop procedure if exists assert_terminal_menu_auto_increment_reset_confirmed;
drop procedure if exists assert_table_exists;
drop procedure if exists assert_no_terminal_menu_orphans;
drop procedure if exists assert_no_terminal_menu_parent_orphans;
drop procedure if exists assert_terminal_menu_ids_are_in_final_ranges;
drop procedure if exists assert_terminal_role_menu_ids_are_in_final_ranges;
drop procedure if exists assert_terminal_menu_auto_increment_targets;
drop procedure if exists reset_terminal_menu_auto_increment;
drop procedure if exists assert_terminal_menu_auto_increment_value;
