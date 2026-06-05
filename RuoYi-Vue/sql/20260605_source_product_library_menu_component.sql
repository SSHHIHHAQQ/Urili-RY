-- Source product library menu component migration.
-- Scope: point the existing "来源商品库" menu to the implemented admin React page.

update sys_menu
set component = 'Product/SourceProductLibrary/index',
    route_name = 'SourceProductLibrary',
    perms = 'product:list:list',
    remark = '商品管理菜单：来源商品库，展示各来源系统同步 SKU 基础信息',
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2400
  and menu_type = 'C';
