-- Deprecated split seed.
-- Use seller_buyer_management_seed.sql plus guarded admin partner grant scripts instead.

set names utf8mb4;

delimiter //

drop procedure if exists abort_deprecated_admin_buyer_account_permission_seed//
create procedure abort_deprecated_admin_buyer_account_permission_seed()
begin
  signal sqlstate '45000'
    set message_text = 'Deprecated split seed: use seller_buyer_management_seed.sql and guarded admin partner grant scripts';
end//

delimiter ;

call abort_deprecated_admin_buyer_account_permission_seed();
