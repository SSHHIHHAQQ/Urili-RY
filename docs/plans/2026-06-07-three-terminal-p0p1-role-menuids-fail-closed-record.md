# 2026-06-07 三端 P0/P1 快速推进：角色菜单 menuIds 全量校验记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮范围：只处理卖家端、买家端角色绑定菜单时的 P1 fail-closed 缺口。不做浏览器运行态验收、截图、DOM 检测或 UI 细调；不执行远程数据库 DDL/DML。

## 子 Agent 使用情况

- 本轮按当前目标使用 6 个 `gpt-5.4` 子 Agent 并行只读扫描。
- 覆盖范围包括 seller/buyer 菜单模型、role-menu mapper、service 单测、React portal 401、复用规则和三端验证入口。
- 6 个子 Agent 已返回结论；本轮采纳 role-menu `menuIds` 全量校验 P1。

## 新增问题

- `seller_menu` / `buyer_menu` 当前是端级共享菜单模板，不是主体私有菜单。
- `batchSellerRoleMenu(...)` / `batchBuyerRoleMenu(...)` 依赖 `insert into ... select ... join seller_menu/buyer_menu`，如果前端传入不存在或跨端的菜单 ID，SQL 会静默少插入，调用方无法感知。
- `insertRole(...)` / `updateRole(...)` 原先没有在写角色、清旧角色菜单、插入新角色菜单前全量校验 `menuIds` 是否均存在于当前端菜单表。

## 已修复问题

- seller 先做标准模板：新增 `countSellerMenusByIds(...)`，`insertRole(...)` / `updateRole(...)` 在任何角色或角色菜单写入前调用 `assertRoleMenusExist(...)`。
- buyer 按 seller 模板机械复制：新增 `countBuyerMenusByIds(...)`，同样在写入前全量校验 `menuIds`。
- seller/buyer service 单测新增 insert/update 负向用例，断言非法菜单 ID 会在 `insertRole/updateRole/deleteRoleMenu/batchRoleMenu` 前 fail-closed。
- `AGENTS.md` 和 `docs/architecture/reuse-ledger.md` 已同步规则：角色绑定菜单必须先全量校验 `menuIds` 均存在于当前端菜单表。

## 残留问题

- React portal 401 行为级 Jest 用例仍未补；当前静态 guard 已覆盖 terminal token 清理和登录跳转关键路径，本轮记录为前端回归测试补强项。
- 如果未来要把 `seller_menu` / `buyer_menu` 改为主体私有菜单，不是本轮小补丁范围，必须先重新设计 schema、接口和合同测试。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 验证结果

- targeted Maven：通过。
- `SellerPortalPermissionServiceImplTest`：`14` 个测试通过。
- `BuyerPortalPermissionServiceImplTest`：`14` 个测试通过。

## 未验证原因

- 未做浏览器、截图、DOM 或 UI 细调验收：当前快速模式明确不需要。
- 未执行远程 MySQL DDL/DML：本轮只改 Java、Mapper XML、测试和 Markdown，没有运行库数据变更。
- 未读取或写入 Redis：本轮不涉及真实 token/session 运行态。

## 权限检查结果

- 本轮不新增管理端 `sys_menu` 权限点，不改 `sys_role_menu`。
- seller/buyer 角色菜单绑定仍分别只写 `seller_role_menu` / `buyer_role_menu`，并在写入前确认 `menuIds` 均来自本端菜单表。

## 字典/选项复用检查结果

- 本轮未新增字典、枚举或前端选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 role-menu 绑定 `menuIds` 全量校验规则和 seller/buyer 对称负向测试要求。

## CodeGraph 更新结果

- 已执行 `cd E:\Urili-Ruoyi; codegraph sync .`，结果为 `Already up to date`。

## 大文件合理性判断结果

- 本轮未新增大代码文件。
- 两个 service 测试类因既有测试代理较长，仍保持单一职责：端内权限 service 负向合同测试；不为本次小补丁拆分。

## 重复代码检查结果

- seller 侧先补 mapper/service/test 标准模板，buyer 侧只替换端类型、字段名、Mapper 方法名和中文错误语义。
