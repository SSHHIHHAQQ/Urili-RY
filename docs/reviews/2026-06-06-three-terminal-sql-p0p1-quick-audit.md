# 2026-06-06 三端隔离 SQL 只读审计（P0/P1 快速版）

**范围**：`RuoYi-Vue/sql` 三端相关 SQL（schema + seed）
**目标**：仅给出 P0/P1 结论（不改代码，仅审计）

## 结论

### P0
- 暂无直接触发启动/编译失败的确定性 P0。

### P1
1. **`sys_menu` 菜单 ID 在多份三端菜单种子中重复定义，执行顺序耦合明显**
   - 重复示例（`sys_menu` 定义 ID）：
     - `seller_buyer_management_seed.sql` 与 `20260606_admin_partner_page_direct_login_seed.sql`：`2010/2011/2012/2205/2215`
       - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:573,578,581,584,602,623`
       - `RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql:12-26`
     - `seller_buyer_management_seed.sql` 与 `20260604_portal_permission_admin_menu_seed.sql`：`2220-2249`
       - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:629-716`
       - `RuoYi-Vue/sql/20260604_portal_permission_admin_menu_seed.sql:11-100`
     - `seller_buyer_management_seed.sql` 与 `20260604_portal_audit_admin_menu_seed.sql`：`2250-2255`
       - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:719-777`
       - `RuoYi-Vue/sql/20260604_portal_audit_admin_menu_seed.sql:11-30`
     - `seller_buyer_management_seed.sql` 与 `20260605_admin_seller_account_permission_seed.sql`：`2310-2316`
       - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:728-746`
       - `RuoYi-Vue/sql/20260605_admin_seller_account_permission_seed.sql:8-30`
     - `seller_buyer_management_seed.sql` 与 `20260605_admin_buyer_account_permission_seed.sql`：`2316-2323`
       - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:758-776`
       - `RuoYi-Vue/sql/20260605_admin_buyer_account_permission_seed.sql:8-30`
     - `seller_buyer_management_seed.sql` 与 `20260605_seller_account_lock_control.sql`/`20260605_buyer_account_lock_control.sql`：`2322/2323`
       - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:737-769`
       - `RuoYi-Vue/sql/20260605_seller_account_lock_control.sql:98`
       - `RuoYi-Vue/sql/20260605_buyer_account_lock_control.sql:98`
   - 影响：多脚本在重复 ID 上 `ON DUPLICATE KEY UPDATE`（或与 `WHERE NOT EXISTS` 后配套）会放大“顺序即结果”风险。远程重放时，若执行路径不统一，菜单/按钮权限最终值可能被覆盖（尤其是历史库补漏执行）。
   - 最小修复：
     1) 建立**单一 sys_menu 权威源**（建议 `seller_buyer_management_seed.sql` 作为主清单），其余脚本仅做 `WHERE ...` 校验或不再重复同一 `menu_id`；
     2) 在新增脚本中加入 `menu_id` 冲突断言（预检）并要求文档化固定执行序列。

2. **`sys_role` 被三端隔离迁移脚本直接写入/禁用（高风险边界）**
   - 证据：`RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql:525-531`
     ```sql
     update sys_role
     set status = '1', del_flag = '2'
     where role_key in ('seller', 'buyer');
     ```
   - 影响：若系统里仍有依赖这些旧 `sys_role` 的业务/页面/接口，执行脚本后会被批量判为禁用，形成运行期阻断；对于“远程库回放 + 回归不完整”是高概率问题。
   - 最小修复：
     - 引入兼容期开关：在确认无 `sys_role` 依赖前不直接下线，或改为“可回退/显式确认”步骤；
     - 先跑只读检查脚本统计 `sys_role`/`sys_role_menu`/`sys_user_role` 关联占用，再执行下线。

### 自愈/幂等/可回滚（P0/P1 扫描结论）
- 有幂等性保护：
  - 多个菜单种子已使用 `on duplicate key update`（如 `20260604_portal_permission_admin_menu_seed.sql:101`, `20260605_admin_buyer_account_permission_seed.sql:29` 等）。
  - 多份 seed 使用 `where not exists`（如 `seller_buyer_management_seed.sql:573`, `RuoYi-Vue/sql/20260605_admin_seller_account_permission_seed.sql`等）。
  - DDL 场景有 `create table if not exists` + `add_column_if_missing`/`add_index_if_missing` 自愈工具（如 `20260604_three_terminal_isolation_migration.sql:9`、`20260604_portal_direct_login_ticket.sql:38`）。
- 可回滚记录不足：
  - 当前三端相关 SQL（含 `20260604_three_terminal_isolation_migration.sql`、`seller_buyer_management_seed.sql` 及各类增量 seed）均无统一 down/rollback 脚本或执行反向脚本说明；多为一次性前向变更。

## 可见的安全边界
- 未发现三端相关脚本里有 `insert into sys_user` / `insert into sys_role`（仅见 `sys_menu` 与 `sys_dict`、`sys_config` 的三端权限/配置种子；`sys_user` 仅被 `20260604_three_terminal_legacy_sys_user_account_backfill.sql` 作为历史数据回填源读取）。
- 因此“向 `sys_user` 写入 seller/buyer 账号”未发现；但 `sys_role` 写入点存在（见 P1#2）。
