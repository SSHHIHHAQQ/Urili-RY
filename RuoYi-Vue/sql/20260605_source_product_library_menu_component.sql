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

drop procedure if exists assert_source_product_library_menu_component_guard//
create procedure assert_source_product_library_menu_component_guard()
begin
  if exists (
    select 1
    from sys_menu m
    where exists (
        select 1
        from tmp_source_product_library_menu_component_guard seed
        where seed.menu_id = m.menu_id
    )
      and not exists (
        select 1
        from tmp_source_product_library_menu_component_guard seed
        where seed.menu_id = m.menu_id
          and coalesce(m.path, '') = seed.path
          and coalesce(m.component, '') = seed.component
          and coalesce(m.route_name, '') = seed.route_name
          and coalesce(m.perms, '') = seed.perms
          and coalesce(m.menu_type, '') = seed.menu_type
    )
  ) then
    signal sqlstate '45000' set message_text = 'source product library menu component sys_menu id slot is occupied by another menu';
  end if;

  if exists (
    select 1
    from sys_menu m
    join tmp_source_product_library_menu_component_guard seed
      on m.menu_id <> seed.menu_id
     and coalesce(m.path, '') = seed.path
     and coalesce(m.component, '') = seed.component
     and coalesce(m.route_name, '') = seed.route_name
     and coalesce(m.perms, '') = seed.perms
  ) then
    signal sqlstate '45000' set message_text = 'source product library menu component sys_menu signature is already used by another menu';
  end if;
end//

delimiter ;

call assert_source_product_library_menu_component_confirmed();
drop procedure if exists assert_source_product_library_menu_component_confirmed;

create temporary table if not exists tmp_source_product_library_menu_component_guard (
  menu_id    bigint       not null,
  path       varchar(200) not null,
  component  varchar(255) not null,
  route_name varchar(50)  not null,
  perms      varchar(100) not null,
  menu_type  char(1)      not null,
  key idx_source_product_library_menu_component_guard_id (menu_id)
) engine=memory;

truncate table tmp_source_product_library_menu_component_guard;

insert into tmp_source_product_library_menu_component_guard
    (menu_id, path, component, route_name, perms, menu_type)
values
    (2400, 'list', 'Product/SourceProductLibrary/index',
     'SourceProductLibrary', 'product:list:list', 'C'),
    (2400, 'list', 'Common/PlannedPage/index',
     'ProductList', 'product:list:list', 'C');

call assert_source_product_library_menu_component_guard();

update sys_menu
set component = 'Product/SourceProductLibrary/index',
    route_name = 'SourceProductLibrary',
    perms = 'product:list:list',
    remark = '商品管理菜单：来源商品库，展示各来源系统同步 SKU 基础信息',
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2400
  and menu_type = 'C';

drop temporary table if exists tmp_source_product_library_menu_component_guard;
drop procedure if exists assert_source_product_library_menu_component_guard;
