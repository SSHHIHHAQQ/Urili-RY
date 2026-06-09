-- Source product library menu component migration.
-- Scope: point the existing "来源商品库" menu to the implemented admin React page.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_source_product_library_menu_component := coalesce(@confirm_source_product_library_menu_component, '');
set @source_product_library_menu_component_expected_count :=
    coalesce(@source_product_library_menu_component_expected_count, '');
set @source_product_library_menu_component_expected_signature :=
    coalesce(@source_product_library_menu_component_expected_signature, '');

delimiter //

drop procedure if exists assert_source_product_library_menu_component_confirmed//
create procedure assert_source_product_library_menu_component_confirmed()
begin
  if coalesce(@confirm_source_product_library_menu_component, '')
      <> 'APPLY_SOURCE_PRODUCT_LIBRARY_MENU_COMPONENT' then
    signal sqlstate '45000' set message_text = 'set @confirm_source_product_library_menu_component = APPLY_SOURCE_PRODUCT_LIBRARY_MENU_COMPONENT before running this migration';
  end if;
  if coalesce(@source_product_library_menu_component_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @source_product_library_menu_component_expected_count after previewing exact source product library sys_menu rows';
  end if;
  if coalesce(@source_product_library_menu_component_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @source_product_library_menu_component_expected_signature after previewing exact source product library sys_menu rows';
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

drop procedure if exists assert_source_product_library_menu_component_targets//
create procedure assert_source_product_library_menu_component_targets()
begin
  declare v_count bigint default 0;
  declare v_signature varchar(64) default '';

  select count(distinct m.menu_id),
         sha2(coalesce(group_concat(distinct
           concat_ws('|',
             m.menu_id,
             coalesce(m.menu_name, ''),
             coalesce(m.parent_id, ''),
             coalesce(m.order_num, ''),
             coalesce(m.path, ''),
             coalesce(m.component, ''),
             coalesce(m.query, ''),
             coalesce(m.route_name, ''),
             coalesce(m.is_frame, ''),
             coalesce(m.is_cache, ''),
             coalesce(m.menu_type, ''),
             coalesce(m.visible, ''),
             coalesce(m.status, ''),
             coalesce(m.perms, ''),
             coalesce(m.icon, ''),
             coalesce(m.remark, '')
           )
           order by m.menu_id separator '\n'
         ), ''), 256)
    into v_count, v_signature
  from sys_menu m
  join tmp_source_product_library_menu_component_guard seed
    on m.menu_id = seed.menu_id
    or (coalesce(seed.perms, '') <> '' and coalesce(m.perms, '') = coalesce(seed.perms, ''));

  if v_count <> cast(@source_product_library_menu_component_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'source product library sys_menu exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@source_product_library_menu_component_expected_signature) then
    signal sqlstate '45000' set message_text = 'source product library sys_menu exact target signature mismatch';
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
     'SourceProductLibrary', 'integration:upstream:query', 'C'),
    (2400, 'list', 'Product/SourceProductLibrary/index',
     'SourceProductLibrary', 'product:list:list', 'C'),
    (2400, 'list', 'Common/PlannedPage/index',
     'ProductList', 'product:list:list', 'C');

call assert_source_product_library_menu_component_guard();
call assert_source_product_library_menu_component_targets();

update sys_menu
set component = 'Product/SourceProductLibrary/index',
    route_name = 'SourceProductLibrary',
    perms = 'integration:upstream:query',
    remark = '商品管理菜单：来源商品库，展示各来源系统同步 SKU 基础信息',
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2400
  and menu_type = 'C';

drop temporary table if exists tmp_source_product_library_menu_component_guard;
drop procedure if exists assert_source_product_library_menu_component_targets;
drop procedure if exists assert_source_product_library_menu_component_guard;
