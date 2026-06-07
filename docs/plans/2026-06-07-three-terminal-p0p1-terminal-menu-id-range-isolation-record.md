# 2026-06-07 三端 P0/P1 快速推进：端内菜单 ID 段隔离脚本与合同记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮范围：只处理 `seller_menu` / `buyer_menu` 裸数字 `menuIds` 的 schema 防御深度和合同测试；保持前端 `number[]` 契约不变，不改成 `perms` / `menuCode` 授权输入；不执行远程数据库 DDL/DML；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 本轮继续按快速推进模式使用子 Agent 做只读复核。
- 既有 5 个 `gpt-5.4` 子 Agent 已完成并关闭，分别覆盖 SQL seed/schema、后端 role-menu 写入链路、合同测试落点、前端 `number[]` 影响和文档边界。
- 按用户要求尝试优先使用 `gpt-5.3-codex-spark`；该模型返回额度限制，随后改用 `gpt-5.4` 做补充只读复核。
- 最终采纳结论：当前不把授权契约改为 `perms` / `menuCode`，先准备 ID 段隔离迁移脚本和合同；远程执行留到后续单独确认。

## 新增问题

- `seller_menu` / `buyer_menu` fresh seed 原先都从 `auto_increment=1` 开始，两个端内菜单表的数字 ID 空间天然重叠。
- 当前 runtime service 已按分表读取和写入，跨端同号 ID 不会写到另一端表；但数据库 schema 层没有 ID 段防御，绕过 service 的手写 SQL 或历史低位 ID 仍缺少结构性约束。
- 直接把角色授权输入改成 `perms` / `menuCode` 会牵动前端树勾选、DTO、Mapper、回填和测试，超出当前 P0/P1 快速推进边界。

## 已修复问题

- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`：`seller_menu` fresh seed 改为 `auto_increment=100000`，`buyer_menu` fresh seed 改为 `auto_increment=200000`。
- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`：三端隔离迁移中的 `seller_menu` / `buyer_menu` fresh DDL 同步改为 `100000` / `200000` 起始段。
- 新增 `RuoYi-Vue/sql/20260607_terminal_menu_id_range_isolation.sql`：
  - 需要 `@confirm_terminal_menu_id_range_isolation = APPLY_TERMINAL_MENU_ID_RANGE_ISOLATION`。
  - 执行前检查 `seller_menu`、`buyer_menu`、`seller_role_menu`、`buyer_role_menu` 存在。
  - 执行前检查 role-menu 孤儿引用、ID 越界、低位旧 ID 迁移后主键碰撞和 role-menu 联合主键碰撞。
  - 迁移时同步更新 `seller_role_menu.seller_menu_id`、`seller_menu.parent_id`、`seller_menu.seller_menu_id`，以及 buyer 对称字段。
  - 迁移后通过 `reset_terminal_menu_auto_increment(...)` 设置下一自增值为 `greatest(号段起点, max(id)+1)`，避免存量迁移后继续使用低位自增。
- `SqlExecutionGuardContractTest` 已将新脚本纳入显式 high-impact SQL confirm token 合同。
- `TerminalSqlIsolationContractTest` 新增端内菜单 ID 段合同，锁住 fresh seed 起始段和迁移脚本必须覆盖 role-menu / parent_id / menu_id。
- `AGENTS.md` 和 `docs/architecture/reuse-ledger.md` 已同步端内菜单 ID 段规则。
- 远程 DB 已执行迁移，执行记录见 `docs/plans/2026-06-07-terminal-menu-id-range-isolation-db-execution-record.md`。

## 残留问题

- 当前远程库 `fenxiao` 已执行端内菜单 ID 段隔离迁移；如后续有其他环境，也必须按激活数据源确认、生成执行记录并显式设置确认 token 后再执行迁移。
- 本轮没有把角色授权接口改成稳定 `perms` / `menuCode`；这属于更大的 P2 契约改造，需要另行设计前端树回填、后端 DTO、Mapper 和历史数据迁移。
- 本轮没有新增数据库外键；当前脚本先做可重放迁移和 fail-closed 巡检。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalSqlIsolationContractTest,SqlExecutionGuardContractTest,TerminalSeedPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 验证结果

- targeted Maven：通过。
- `SqlExecutionGuardContractTest`：`31` 个测试通过。
- `TerminalSeedPermissionContractTest`：`1` 个测试通过。
- `TerminalSqlIsolationContractTest`：`11` 个测试通过。
- 总计：`43` 个测试通过。
- 远程 DB 执行：
  - 第一次脚本执行：`EXECUTION_OK statements=36 target_db=fenxiao`。
  - 修正动态自增重置后复跑：`EXECUTION_OK statements=39 target_db=fenxiao`。
  - 最终远程库核验：seller 菜单 `100008-100018`，buyer 菜单 `200003-200013`，role-menu 全部在对应号段，孤儿引用 `0`。
  - `SHOW CREATE TABLE` 核验：`seller_menu AUTO_INCREMENT=100019`，`buyer_menu AUTO_INCREMENT=200014`。

## 未验证原因

- 未读取或写入 Redis：本轮不涉及 token/session 运行态。
- 未做浏览器、截图、DOM 或 UI 细调验收：当前快速模式明确不需要。

## 权限检查结果

- 本轮不新增管理端 `sys_menu` 权限点，不改 `sys_role_menu`。
- seller/buyer 角色菜单运行时仍按当前端表校验 `menuIds`，并保持 `seller_role_menu` / `buyer_role_menu` 分表写入。

## 字典/选项复用检查结果

- 本轮未新增字典、枚举或前端选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 seller/buyer 端内菜单数字 ID 段：seller `100000-199999`，buyer `200000-299999`。

## CodeGraph 更新结果

- 已执行 `cd E:\Urili-Ruoyi; codegraph sync .`，首次结果为 `Synced 2 changed files`；记录更新后再次执行，结果为 `Already up to date`。

## 大文件合理性判断结果

- 新增 SQL 迁移脚本职责单一：只处理端内菜单 ID 段隔离和 role-menu/parent_id 同步迁移。
- `TerminalSqlIsolationContractTest` 新增一个合同方法，仍属于三端 SQL 隔离合同职责范围。

## 重复代码检查结果

- seller/buyer 迁移逻辑保持对称，只替换表名、字段名和偏移量；未新增前端或 service 重复逻辑。
