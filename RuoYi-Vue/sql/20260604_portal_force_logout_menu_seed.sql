-- Deprecated split seed.
-- Use seller_buyer_management_seed.sql plus guarded admin partner grant scripts instead.

set names utf8mb4;

delimiter //

drop procedure if exists abort_deprecated_portal_force_logout_menu_seed//
create procedure abort_deprecated_portal_force_logout_menu_seed()
begin
  signal sqlstate '45000'
    set message_text = 'Deprecated split seed: use seller_buyer_management_seed.sql and guarded admin partner grant scripts';
end//

delimiter ;

call abort_deprecated_portal_force_logout_menu_seed();
