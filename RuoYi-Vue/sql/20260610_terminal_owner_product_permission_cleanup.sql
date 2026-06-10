-- Cleanup historical default Owner grants for seller/buyer portal product permissions.
-- Scope:
-- 1. Keep seller_menu / buyer_menu hidden product permission definitions.
-- 2. Remove only active terminal Owner role grants for seller:product:* and buyer:product:*.
-- 3. Require preview-confirmed exact target counts and signatures before DML.

set names utf8mb4;
set session group_concat_max_len = 1048576;

set @confirm_terminal_owner_product_permission_cleanup := coalesce(@confirm_terminal_owner_product_permission_cleanup, '');
set @terminal_owner_product_cleanup_expected_seller_count := coalesce(@terminal_owner_product_cleanup_expected_seller_count, -1);
set @terminal_owner_product_cleanup_expected_seller_signature := coalesce(@terminal_owner_product_cleanup_expected_seller_signature, '');
set @terminal_owner_product_cleanup_expected_buyer_count := coalesce(@terminal_owner_product_cleanup_expected_buyer_count, -1);
set @terminal_owner_product_cleanup_expected_buyer_signature := coalesce(@terminal_owner_product_cleanup_expected_buyer_signature, '');

delimiter //

drop procedure if exists assert_terminal_owner_product_permission_cleanup_confirmed//
create procedure assert_terminal_owner_product_permission_cleanup_confirmed()
begin
  if coalesce(@confirm_terminal_owner_product_permission_cleanup, '')
      <> 'CLEAN_TERMINAL_OWNER_PRODUCT_PERMISSION_GRANTS' then
    signal sqlstate '45000' set message_text = 'set @confirm_terminal_owner_product_permission_cleanup = CLEAN_TERMINAL_OWNER_PRODUCT_PERMISSION_GRANTS before running cleanup';
  end if;

  if coalesce(@terminal_owner_product_cleanup_expected_seller_count, -1) < 0 then
    signal sqlstate '45000' set message_text = 'set @terminal_owner_product_cleanup_expected_seller_count after previewing exact seller owner product grants';
  end if;

  if coalesce(@terminal_owner_product_cleanup_expected_seller_signature, '') = '' then
    signal sqlstate '45000' set message_text = 'set @terminal_owner_product_cleanup_expected_seller_signature after previewing exact seller owner product grants';
  end if;

  if coalesce(@terminal_owner_product_cleanup_expected_buyer_count, -1) < 0 then
    signal sqlstate '45000' set message_text = 'set @terminal_owner_product_cleanup_expected_buyer_count after previewing exact buyer owner product grants';
  end if;

  if coalesce(@terminal_owner_product_cleanup_expected_buyer_signature, '') = '' then
    signal sqlstate '45000' set message_text = 'set @terminal_owner_product_cleanup_expected_buyer_signature after previewing exact buyer owner product grants';
  end if;
end//

drop procedure if exists assert_seller_owner_product_permission_cleanup_targets//
create procedure assert_seller_owner_product_permission_cleanup_targets()
begin
  declare v_count bigint default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         coalesce(sha2(coalesce(group_concat(
           concat_ws('#', r.seller_id, r.seller_role_id, rm.seller_menu_id, m.perms)
           order by r.seller_id, r.seller_role_id, rm.seller_menu_id
           separator '|'
         ), ''), 256), '')
    into v_count, v_signature
  from seller_role_menu rm
  join seller_role r on r.seller_role_id = rm.seller_role_id
  join seller_menu m on m.seller_menu_id = rm.seller_menu_id
  where r.role_key = 'owner'
    and r.status = '0'
    and r.del_flag = '0'
    and m.perms like 'seller:product:%';

  if v_count <> @terminal_owner_product_cleanup_expected_seller_count then
    signal sqlstate '45000' set message_text = 'seller owner product grant exact target count mismatch';
  end if;

  if lower(v_signature) <> lower(@terminal_owner_product_cleanup_expected_seller_signature) then
    signal sqlstate '45000' set message_text = 'seller owner product grant exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_buyer_owner_product_permission_cleanup_targets//
create procedure assert_buyer_owner_product_permission_cleanup_targets()
begin
  declare v_count bigint default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         coalesce(sha2(coalesce(group_concat(
           concat_ws('#', r.buyer_id, r.buyer_role_id, rm.buyer_menu_id, m.perms)
           order by r.buyer_id, r.buyer_role_id, rm.buyer_menu_id
           separator '|'
         ), ''), 256), '')
    into v_count, v_signature
  from buyer_role_menu rm
  join buyer_role r on r.buyer_role_id = rm.buyer_role_id
  join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
  where r.role_key = 'owner'
    and r.status = '0'
    and r.del_flag = '0'
    and m.perms like 'buyer:product:%';

  if v_count <> @terminal_owner_product_cleanup_expected_buyer_count then
    signal sqlstate '45000' set message_text = 'buyer owner product grant exact target count mismatch';
  end if;

  if lower(v_signature) <> lower(@terminal_owner_product_cleanup_expected_buyer_signature) then
    signal sqlstate '45000' set message_text = 'buyer owner product grant exact target signature mismatch';
  end if;
end//

drop procedure if exists assert_terminal_owner_product_permission_cleanup_completed//
create procedure assert_terminal_owner_product_permission_cleanup_completed()
begin
  if exists (
    select 1
    from seller_role_menu rm
    join seller_role r on r.seller_role_id = rm.seller_role_id
    join seller_menu m on m.seller_menu_id = rm.seller_menu_id
    where r.role_key = 'owner'
      and r.status = '0'
      and r.del_flag = '0'
      and m.perms like 'seller:product:%'
  ) then
    signal sqlstate '45000' set message_text = 'seller owner product grants cleanup has remaining rows';
  end if;

  if exists (
    select 1
    from buyer_role_menu rm
    join buyer_role r on r.buyer_role_id = rm.buyer_role_id
    join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
    where r.role_key = 'owner'
      and r.status = '0'
      and r.del_flag = '0'
      and m.perms like 'buyer:product:%'
  ) then
    signal sqlstate '45000' set message_text = 'buyer owner product grants cleanup has remaining rows';
  end if;

  if (
    select count(1)
    from seller_menu
    where perms in (
      'seller:product:category:list',
      'seller:product:schema:query',
      'seller:product:distribution:list',
      'seller:product:distribution:query'
    )
  ) <> 4 then
    signal sqlstate '45000' set message_text = 'seller product permission menu definitions must remain after cleanup';
  end if;

  if (
    select count(1)
    from buyer_menu
    where perms in (
      'buyer:product:category:list',
      'buyer:product:schema:query',
      'buyer:product:distribution:list',
      'buyer:product:distribution:query'
    )
  ) <> 4 then
    signal sqlstate '45000' set message_text = 'buyer product permission menu definitions must remain after cleanup';
  end if;
end//

delimiter ;

call assert_terminal_owner_product_permission_cleanup_confirmed();
call assert_seller_owner_product_permission_cleanup_targets();
call assert_buyer_owner_product_permission_cleanup_targets();

start transaction;

call assert_seller_owner_product_permission_cleanup_targets();
call assert_buyer_owner_product_permission_cleanup_targets();

delete rm
from seller_role_menu rm
join seller_role r on r.seller_role_id = rm.seller_role_id
join seller_menu m on m.seller_menu_id = rm.seller_menu_id
where r.role_key = 'owner'
  and r.status = '0'
  and r.del_flag = '0'
  and m.perms like 'seller:product:%';

delete rm
from buyer_role_menu rm
join buyer_role r on r.buyer_role_id = rm.buyer_role_id
join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
where r.role_key = 'owner'
  and r.status = '0'
  and r.del_flag = '0'
  and m.perms like 'buyer:product:%';

call assert_terminal_owner_product_permission_cleanup_completed();

commit;

drop procedure if exists assert_terminal_owner_product_permission_cleanup_confirmed;
drop procedure if exists assert_seller_owner_product_permission_cleanup_targets;
drop procedure if exists assert_buyer_owner_product_permission_cleanup_targets;
drop procedure if exists assert_terminal_owner_product_permission_cleanup_completed;
