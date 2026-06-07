# 2026-06-07 端内菜单 ID 段隔离远程 DB 执行记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本记录只覆盖 `RuoYi-Vue/sql/20260607_terminal_menu_id_range_isolation.sql` 对当前激活远程 MySQL 的执行。执行目标来自 `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`、`application-druid.yml` 与本机 `.env.local` 注入变量；不记录密码、token secret 或 Redis 密码。

## 目标环境

- 配置来源：`spring.profiles.active=druid`，主库 URL 使用 `${RUOYI_DB_URL}`。
- 目标 MySQL：`gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com:28634/fenxiao`。
- 目标库确认：`database() = fenxiao`。
- MySQL 版本：`8.0.30-cynos-3.1.16.003`。
- Redis：本轮不读写 Redis。

## 执行脚本

- SQL 文件：`RuoYi-Vue/sql/20260607_terminal_menu_id_range_isolation.sql`
- 确认 token：`@confirm_terminal_menu_id_range_isolation = APPLY_TERMINAL_MENU_ID_RANGE_ISOLATION`
- 执行类型：DDL + DML。
- 影响范围：
  - `seller_menu.seller_menu_id`
  - `seller_menu.parent_id`
  - `seller_role_menu.seller_menu_id`
  - `buyer_menu.buyer_menu_id`
  - `buyer_menu.parent_id`
  - `buyer_role_menu.buyer_menu_id`
  - `seller_menu` / `buyer_menu` 的 `auto_increment`

## 执行前只读预检

- `seller_menu`：`10` 行，ID 范围 `8-18`，低位 ID `10`，seller 目标段 `0`，越界 `0`。
- `buyer_menu`：`10` 行，ID 范围 `3-13`，低位 ID `10`，buyer 目标段 `0`，seller 保留段 `0`，越界 `0`。
- `seller_role_menu`：`30` 行，低位 ID `30`，seller 目标段 `0`，越界 `0`。
- `buyer_role_menu`：`10` 行，低位 ID `10`，buyer 目标段 `0`，seller 保留段 `0`，越界 `0`。
- 孤儿引用：
  - `seller_role_menu -> seller_menu`：`0`
  - `buyer_role_menu -> buyer_menu`：`0`
- 迁移后碰撞预检：
  - `seller_menu +100000`：`0`
  - `buyer_menu +200000`：`0`
  - `seller_role_menu +100000`：`0`
  - `buyer_role_menu +200000`：`0`

## 执行结果

- 第一次执行：`EXECUTION_OK statements=36 target_db=fenxiao`。
- 第一次执行后发现 `seller_menu` / `buyer_menu` 的 ID 已迁入目标段，但 `information_schema.tables.auto_increment` 与 `SHOW TABLE STATUS` 在当前 CynosDB 上仍显示旧低位值。
- 随后用 `SHOW CREATE TABLE` 核验，确认直接 `ALTER TABLE ... AUTO_INCREMENT = max(id)+1` 实际已写入表定义：`seller_menu AUTO_INCREMENT=100019`，`buyer_menu AUTO_INCREMENT=200014`。
- 更新脚本：保留动态 `reset_terminal_menu_auto_increment(...)`，删除依赖 `information_schema.tables.auto_increment` 的脚本内自检，避免在当前 CynosDB 元数据返回旧值时误报。
- 第二次执行更新后的脚本：`EXECUTION_OK statements=39 target_db=fenxiao`。

## 执行后核验

- `seller_menu`：`10` 行，ID 范围 `100008-100018`，低位 ID `0`，seller 目标段 `10`，越界 `0`。
- `buyer_menu`：`10` 行，ID 范围 `200003-200013`，低位 ID `0`，seller 保留段 `0`，buyer 目标段 `10`，越界 `0`。
- `seller_role_menu`：`30` 行，低位 ID `0`，seller 目标段 `30`，越界 `0`。
- `buyer_role_menu`：`10` 行，低位 ID `0`，seller 保留段 `0`，buyer 目标段 `10`，越界 `0`。
- 孤儿引用：
  - `seller_role_menu -> seller_menu`：`0`
  - `buyer_role_menu -> buyer_menu`：`0`
- `SHOW CREATE TABLE seller_menu`：`AUTO_INCREMENT=100019`。
- `SHOW CREATE TABLE buyer_menu`：`AUTO_INCREMENT=200014`。

## 未验证原因

- 本轮不做浏览器、截图、DOM 或 UI 细调验收。
- 本轮不读写 Redis。

## CodeGraph 更新结果

- 已执行 `cd E:\Urili-Ruoyi; codegraph sync .`，最终结果为 `Already up to date`。
