# 2026-06-07 三端 P0/P1 快速推进：全量 SQL Guard、路由兜底与查询绑定记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮范围：只处理 P0/P1 的 guard、接口可达性和字段绑定问题；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 新增问题

- `SqlExecutionGuardContractTest` 自动发现范围只覆盖 `20260606*.sql` / `20260607*.sql`，导致 2026-06-04/05 高影响 SQL 可能漏 guard。
- 管理端 seller/buyer 页缺 `/seller`、`/buyer` 静态路由兜底，直达或刷新依赖远端菜单注入。
- `companyName` 查询字段在前端透传到后端，但 `Seller` / `Buyer` domain 缺 setter，Spring 绑定可能静默失败。

## 已修复问题

- SQL guard 自动发现范围扩展为全部 `202606*.sql`。
- 13 个旧高影响 SQL 脚本补齐 `@confirm_*`、确认过程、`signal sqlstate '45000'` 和首个高影响 DDL/DML 前确认调用。
- `react-ui/config/routes.ts` 和 `react-ui/config/routes.js` 补 `/seller`、`/buyer` 静态路由。
- `Seller` / `Buyer` 增加 `companyName` 查询绑定字段和 setter，同时保留响应展示时从全称/简称回退生成 `companyName`。
- 复用台账已同步更新 SQL guard 自动发现规则。

## 残留问题

- 旧 DDL 脚本已 fail-closed，但部分脚本仍不是完全可重放 DDL。
- 旧 `sys_menu` seed 的 slot/signature guard 仍需逐步补齐。
- portal 自助日志/会话接口是否需要端内细粒度权限，待产品口径确认。
- 余额/充值仍是占位口径，不能作为真实财务能力。
- `verify-three-terminal.mjs` 仍有模块/测试清单硬编码边界。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am -DskipTests compile`
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
- `cd E:\Urili-Ruoyi; git diff --check`
- `cd E:\Urili-Ruoyi; codegraph sync .`

## 验证结果

- `SqlExecutionGuardContractTest`：通过，`7` 个测试通过。
- seller/buyer 后端编译：通过。
- `guard:partner-management`：通过。
- `tsc --noEmit`：通过。
- `verify:three-terminal`：通过；前端 `3` 个 suite / `9` 个测试通过，后端 ruoyi-system `99`、ruoyi-framework `15`、product `1`、seller `72`、buyer `73` 个测试通过。
- `git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。

## 未验证原因

- 未做浏览器、截图、DOM 或 UI 细调验收：用户已明确当前快速模式无需浏览器运行态验收。
- 未执行远程 MySQL DDL/DML，未读取或写入 Redis：本轮只做本地代码、SQL 文件和契约验证。

## 权限检查结果

- 本轮未新增后端接口或按钮权限。
- SQL guard 只增加执行确认门禁，不改变权限数据本身。

## 字典/选项复用检查结果

- 本轮未新增字典或选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，明确高影响 SQL 自动发现必须覆盖全部 `202606*.sql`。

## CodeGraph 更新结果

- `codegraph sync .`：通过；首次结果为 `Synced 6 changed files`，`Modified: 6 - 130 nodes in 1.2s`；回填记录后最终复跑结果为 `Already up to date`。

## 大文件合理性判断结果

- 本轮未新增大代码文件。
- SQL 脚本仅补统一确认门禁；阶段记录属于 Markdown 留痕。

## 重复代码检查结果

- Redis key、companyName 绑定和路由兜底均保持在现有模块内，没有新增平行业务实现。

## 子 Agent 结论处理

- 已采纳全量 SQL guard、静态路由兜底、`companyName` 查询绑定。
- 暂不采纳 seller/buyer 菜单表按主体拆分，因为当前模型是端级菜单目录与主体内角色授权，改表需要单独设计确认。
- portal 自助审计权限、余额占位、DDL 可重放和 verify 自动发现记录为后续 P1。
