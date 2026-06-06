# E:\Urili-Ruoyi SQL 隔离审计（P0/P1）：seller/buyer 三端（只读）

**时间**：2026-06-06
**范围**：仅审计 `RuoYi-Vue/sql` 中 seller/buyer 相关 SQL（schema + seed）以及 `sys_menu` 权限种子
**目标**：重点识别是否有直接阻断“编译/启动/权限菜单生效/端间串用”的 P0/P1

## 主代理复核结论

- 下方 SQL 问题不作为当前代码级 P0/P1 阻塞处理，先归为迁移脚本治理风险。
- 复核理由：这些脚本是按日期拆分的历史/增量迁移输入，当前任务没有执行数据库 DDL/DML，也没有验证到当前运行库菜单权限已经被重复 `menu_id` 覆盖出错。
- 如果后续要重新初始化或重放全量脚本，必须单独做“权限种子单一权威源 + 执行顺序 + 菜单快照”治理；在执行远程数据库前仍需单独 Markdown 方案和确认。

## P0（无）

- 当前只读审计范围内未发现可直接导致服务**启动失败**或**编译失败**的确定性 P0 级问题（`sys_menu` 相关脚本均有幂等处理，迁移 DDL 有 `if exists` 防护）。

## P1（需立即处理）

### 1) `sys_menu.menu_id` 在多份种子中重复定义，存在执行顺序耦合

- 重复的 `menu_id` 已在多个文件出现（同一 `menu_id` 不同文件均执行 `INSERT ... ON DUPLICATE KEY UPDATE` 或重复 `INSERT INTO sys_menu`）。
- 关键样例：
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:578`（`2010`）与 `RuoYi-Vue/sql/top_menu_seed.sql:11`（`2010`）
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:605,626` 与 `RuoYi-Vue/sql/20260604_portal_force_logout_menu_seed.sql:11,14`（`2206/2216`）
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:629-716` 与 `RuoYi-Vue/sql/20260604_portal_permission_admin_menu_seed.sql:11-98`（`2220-2249`）
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:719,722,725,749-776` 与 `RuoYi-Vue/sql/20260604_portal_audit_admin_menu_seed.sql:11-26`（`2250-2255`）
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:728-746` 与 `RuoYi-Vue/sql/20260605_admin_seller_account_permission_seed.sql:8-27`（`2310-2315`）
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:758-776` 与 `RuoYi-Vue/sql/20260605_admin_buyer_account_permission_seed.sql:8-27`（`2316-2321`）
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql:737` 与 `RuoYi-Vue/sql/20260605_seller_account_lock_control.sql:98`（`2322`）；`seller_buyer_management_seed.sql:767` 与 `20260605_buyer_account_lock_control.sql:98`（`2323`）
- 影响：若脚本执行顺序变化，菜单 `perms`/`menu_name`/`status` 等最终值会被后执行脚本覆盖，可能导致管理员端权限按钮失效或串改（尤其是多次交付时）。
- 最小修复建议：建立**单一权限种子权威源**，按“只写一次”约束收敛 `menu_id`；其他文件统一改为引用该文件或按顺序约束执行。`sys_menu` 侧建议增加“执行前去重告警（同ID不同定义）”检查。

### 2) 三端隔离迁移脚本会直接废弃旧 `sys_role`：`seller` / `buyer` 角色

- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql:485-491`
  ```sql
  update sys_role set status='1', del_flag='2' where role_key in ('seller','buyer');
  ```
- 影响：只要有未改造到新权限模型的功能还在按 `sys_role` 判断，迁移后会被整体失效（无权限可见/无权限返回），属于运行期功能阻断风险。
- 最小修复建议：确认并清理所有旧链路的 `sys_role` 依赖；若需兼容期，先做兼容角色映射或显式兼容策略，再执行禁用动作。

### 3) 生成列 + 唯一约束链路与版本兼容性导致迁移脚本中断风险

- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql:150-151`
  `owner_unique_seller_id` 生成列（`stored generated`）
- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql:163-201`
  `owner_unique_buyer_id` + `add unique index uk_*_owner`
- 同逻辑也在 `RuoYi-Vue/sql/seller_buyer_management_seed.sql:97-106,127-136` 中重复出现。
- 影响：目标环境若为不支持该语法/行为的 MySQL 变体或版本，脚本执行会中断，属于启动前数据库迁移阻断风险。
- 最小修复建议：在执行前做一次版本检测（如 `@@version`），不支持时走回退脚本（用普通列+校验触发器/SQL约束）。

## P2（完整性问题，简要记录）

- `seller_account`、`buyer_account`、`seller_role`、`seller_menu` 等表以及角色菜单关联表在当前 schema 中**没有外键约束**（如 `seller_account_role`、`seller_role_menu` 仅有主键）；历史脏数据可能导致“账号-角色-菜单”悬挂（可能引发运行期权限不可预期）。
- `20260604_portal_*` 与 `20260605_admin_*_permission_seed` 分离维护，不统一执行顺序与差异校验，新增权限项时容易出现“看起来生效、实则未赋权”问题。

## 结论

- 当前仅见少量 **P1** 风险，且主要集中在**种子执行顺序与兼容性**；未见立刻可复现的“必然阻断启动”路径。
- 建议先在测试库做“脚本顺序矩阵演练 + 菜单权限可见性快照”并冻结权限种子源，再推进生产执行。
