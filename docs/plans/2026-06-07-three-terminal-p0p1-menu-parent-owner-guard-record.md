# 2026-06-07 三端 P0/P1 快速推进：业务与上游菜单父级 Owner Guard 记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮范围：只处理 P0/P1 的 SQL guard 和菜单 owner 串线风险。不做浏览器运行态验收、截图、DOM 检测或 UI 细调；不执行远程数据库 DDL/DML。

## 子 Agent 使用情况

- 本轮按当前目标使用 6 个 `gpt-5.4` 子 Agent 并行只读扫描。
- 覆盖范围包括 `business_menu_seed.sql`、`upstream_system_management_seed.sql`、旧 SQL 可重放风险、React portal 401、seller/buyer 菜单裸 `menuId` 和 Markdown 残留口径。
- 6 个子 Agent 已返回结论；本轮采纳 SQL 父级 owner guard P1。

## 新增问题

- `business_menu_seed.sql` 已有自身 `sys_menu` slot/signature guard，但写入 `2400/2401/2403/2410/2411/2420/2422/2430-2434/2450-2452` 前未断言父目录 `2050/2060/2070/2080/2100` 仍由 `top_menu_seed.sql` 提供。
- `upstream_system_management_seed.sql` 已有 `2031/2300-2309` slot/signature guard，但写入 `2031` 前未断言父目录 `2030` 仍是预期的「海外仓服务设置」顶级菜单。
- 两个 seed 的已有 id-slot guard 只比对 `path/component/route_name/perms`，未把 `parent_id/menu_type` 纳入同 ID 菜单判断，存在历史菜单同签名但挂错父级时被静默 re-parent 的 P1 风险。

## 已修复问题

- `business_menu_seed.sql` 在 `assert_business_menu_sys_menu_guard()` 中增加父目录 ready guard，要求 `2050/2060/2070/2080/2100` 均存在且 `parent_id=0`、`path/route_name/menu_type` 与顶级菜单 owner 一致。
- `upstream_system_management_seed.sql` 在 `assert_upstream_system_management_sys_menu_guard()` 中增加父目录 ready guard，要求 `2030` 存在且签名仍为「海外仓服务设置」顶级菜单。
- 两个 seed 的 `tmp_*_sys_menu_guard` 增加 `parent_id/menu_type`，同 ID 菜单校验同步纳入这两个 owner 维度。
- `SqlExecutionGuardContractTest` 固定 business/upstream 两个 seed 的父级 owner guard 和 `parent_id/menu_type` 防回退合同。

## 残留问题

- React portal 401 响应体分支当前会触发跳转但仍返回成功响应；portal 401 跳登录未保留 redirect。该项记录为后续前端 P1。
- seller/buyer 管理端菜单链路仍存在裸 `menuId` 读改删和角色绑菜单仅信任 `menuIds` 的 P1 候选，后续应按 seller 模板先收，再机械复制 buyer。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 中仍有部分历史残留口径需要清账；本轮只修正刚收口的菜单父级 guard 口径。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`

## 验证结果

- `SqlExecutionGuardContractTest`：通过，`27` 个测试通过。

## 未验证原因

- 未做浏览器、截图、DOM 或 UI 细调验收：当前快速模式明确不需要。
- 未执行远程 MySQL DDL/DML：本轮只修改 SQL 文件和静态合同测试，没有运行库数据变更。
- 未读取或写入 Redis：本轮不涉及真实 token/session 运行态。

## 权限检查结果

- 本轮不新增权限点，不改 `sys_role_menu` 授权范围。
- SQL guard 只防止业务菜单和上游系统菜单挂到错误父级或被错误 slot 覆盖。

## 字典/选项复用检查结果

- 本轮未新增字典、枚举或前端选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 business/upstream 菜单 seed 必须先断言父级 owner，并在 id-slot guard 中纳入 `parent_id/menu_type`。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Synced 1 changed files`，`Modified: 1 - 58 nodes`。

## 大文件合理性判断结果

- 本轮未新增大代码文件。
- 修改集中在 SQL seed、静态合同测试和 Markdown 记录，符合 P1 guard 小步收口。

## 重复代码检查结果

- business 与 upstream 沿用 warehouse seed 已确认的父级 ready guard 模式。
- seller/buyer 菜单裸 `menuId` P1 暂未实现，后续应按“seller 标准模板通过后机械复制 buyer”的方式推进。
