# 三端隔离 SQL/Seed 只读审计（P0/P1）

## 范围
- 文件：
  - `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`
  - `RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql`
  - `RuoYi-Vue/sql/20260605_terminal_owner_account_unique_constraint.sql`
  - `RuoYi-Vue/sql/20260606_terminal_log_scope_indexes.sql`
  - `RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql`
  - `RuoYi-Vue/sql/20260606_legacy_disable_sys_seller_buyer_roles.sql`
  - `RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_permission_admin_menu_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_audit_admin_menu_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_force_logout_menu_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_account_list_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_dept_role_list_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_product_category_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_seller_product_schema_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_buyer_product_schema_permission_seed.sql`
  - `RuoYi-Vue/sql/20260605_admin_seller_account_permission_seed.sql`
  - `RuoYi-Vue/sql/20260605_admin_buyer_account_permission_seed.sql`
  - `RuoYi-Vue/sql/20260605_seller_account_lock_control.sql`
  - `RuoYi-Vue/sql/20260605_buyer_account_lock_control.sql`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`

## 结论速览
- P0：未发现
- P1：3 项
- 幂等/索引/owner 唯一：当前脚本链路整体为“可重复执行为主”，未见明显“同名错索引”直接证据

## P1 问题清单

### 1) Legacy sys_role 清理脚本缺少环境/语义范围保护（高）
**路径与证据**
- `RuoYi-Vue/sql/20260606_legacy_disable_sys_seller_buyer_roles.sql:23-24`（过程定义）
- `RuoYi-Vue/sql/20260606_legacy_disable_sys_seller_buyer_roles.sql:30-41`（仅基于 `sys_role.role_key IN ('seller','buyer')` 与活动绑定做检查）
- `RuoYi-Vue/sql/20260606_legacy_disable_sys_seller_buyer_roles.sql:51-61`（直接更新
  `sys_role` 的状态与删除标记）

**风险**
- 该脚本未区分“legacy sys_* seller/buyer 角色”与系统中可能被复用的其他 `role_key` 名称。
- 若环境仍保留历史/自定义 `sys_role` 使用 `seller/buyer` 标识，可能被误禁用（覆盖性风险、不可逆操作）。

**最小修复建议**
- 在更新前加入明确环境锚点：例如检测到 `seller_account`/`buyer_account` + `seller_role`/`buyer_role` 为已迁移状态时才允许执行。
- 增加手工确认参数（例如 `@require_legacy_disable=1`）并默认拒绝执行，除非显式传参。
- 先做预扫描报告：仅列出将被影响的 `sys_role.role_id`，审核通过后再执行。

---

### 2) Legacy 回填脚本缺 guard，`sys_user` 回填可误伤（高）
**路径与证据**
- `RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql:4-6`（文件头说明“仅历史混合库使用”）
- `RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql:15-18` / `40-41`（只检查是否存在 `user_id` 列）
- `RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql:19-31` / `42-51`（直接 `update ... left join sys_user` 回填）

**风险**
- 该脚本执行条件仅为列存在，没有环境指纹校验。
- 在非 legacy 环境误触发时，会把 `seller_account`/`buyer_account` 已有字段按 `sys_user` 覆盖或替换，造成凭据/状态污染。

**最小修复建议**
- 添加强制环境检测（例如同时校验 legacy 标记表/字段、`sys_role` 特征、ticket/log 表新旧字段版本）。
- 回填前先做只读快照与变更计数：`user_id` 非空、将要替换字段数、`coalesce(nullif(...),...)` 的实际命中比。
- 将“执行语句”切换为可回退流程：写临时表先预计算、确认后再 `UPDATE ...`。

---

### 3) 管理端新权限菜单与角色授权存在可见性缺口（中高）
**路径与证据**
- 仅在菜单层写入：
  - `RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql:86-89`（`seller:admin:directLogin` / `buyer:admin:directLogin`）
  - `RuoYi-Vue/sql/20260604_portal_force_logout_menu_seed.sql:12-15`（`seller:admin:forceLogout` / `buyer:admin:forceLogout`）
  - `RuoYi-Vue/sql/20260604_portal_audit_admin_menu_seed.sql:12-27`（`seller:admin:ticket:list` / `buyer:admin:ticket:list`）
- 同时在三端管理侧权限绑定脚本中未见上述权限在 `sys_role_menu`（或等价管理员角色授予）路径的新增绑定语句；相关语句主要集中在 terminal 菜单（`seller_menu` / `buyer_menu`）绑定。

**风险**
- 新增按钮权限可能仅入库成菜单项，但未在当前三端种子链路中保证管理端角色被授予，导致功能入口在部分环境“可配置但不可见/不可用”。

**最小修复建议**
- 增加一个独立的管理员角色授权 seed，显式将上述权限绑定到“管理员基础角色”。
- 对缺口做幂等补丁：`where not exists (select 1 from sys_role_menu ...)` 再补 `INSERT`。
- 增加回归校验：校验 `menu_id` 存在 + `perms` 存在 + 对目标管理员角色的 `sys_role_menu` 映射存在。

---

## 验证建议（不执行数据库）
1. 审核脚本级自检（静态）：
   - `rg -n "legacy_disable_sys_seller_buyer_roles|three_terminal_legacy_sys_user_account_backfill|directLogin|forceLogout|ticket:list" RuoYi-Vue/sql`
   - `rg -n "insert into sys_role_menu|sys_role_menu" RuoYi-Vue/sql/*06* RuoYi-Vue/sql/20260604*`
2. 迁移顺序核对：
   - `20260604_three_terminal_...`（表结构）
   - `20260605_terminal_owner_account_unique_constraint.sql`
   - `20260606_terminal_log_scope_indexes.sql`
3. 菜单/权限覆盖性核对：
   - 用权限清单比对“菜单条目 perms”与“管理员角色映射”是否一一对应。
