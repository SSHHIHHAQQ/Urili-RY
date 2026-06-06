-- Source product library menu component migration.
-- Scope: point the existing "来源商品库" menu to the implemented admin React page.

set names utf8mb4;

set @confirm_source_product_library_menu_component := coalesce(@confirm_source_product_library_menu_component, '');

delimiter //

drop procedure if exists assert_source_product_library_menu_component_confirmed//
create procedure assert_source_product_library_menu_component_confirmed()
begin
  if coalesce(@confirm_source_product_library_menu_component, '')
      <> 'APPLY_SOURCE_PRODUCT_LIBRARY_MENU_COMPONENT' then
    signal sqlstate '45000' set message_text = 'set @confirm_source_product_library_menu_component = APPLY_SOURCE_PRODUCT_LIBRARY_MENU_COMPONENT before running this migration';
  end if;
end//

delimiter ;

call assert_source_product_library_menu_component_confirmed();
drop procedure if exists assert_source_product_library_menu_component_confirmed;

update sys_menu
set component = 'Product/SourceProductLibrary/index',
    route_name = 'SourceProductLibrary',
    perms = 'product:list:list',
    remark = '商品管理菜单：来源商品库，展示各来源系统同步 SKU 基础信息',
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2400
  and menu_type = 'C';
