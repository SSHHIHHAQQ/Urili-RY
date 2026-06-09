# 三端端内菜单 Component 根路径 P1 修复记录

日期：2026-06-09

## 目标

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 或 UI 细调。

## 新增问题

- P1：端内菜单 seed / migration 对 `seller_menu` / `buyer_menu` 的页面菜单 `component` 只校验非空，没有校验必须位于当前端页面根路径。
  - seller 页面菜单应使用 `Seller/%`。
  - buyer 页面菜单应使用 `Buyer/%`。
  - 风险：合法 `perms` 但错误 `component` 仍可能通过 SQL guard，导致端内菜单指向跨端页面或共享占位页。

## 已修复问题

- 已把相关 SQL 的页面菜单预检从“component 非空”升级为“component 非空且必须在当前端根路径下”。
- 已同步更新 `SqlExecutionGuardContractTest`，固定该 fail-closed 规则。

涉及文件：

- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`
- `RuoYi-Vue/sql/20260604_portal_account_list_permission_seed.sql`
- `RuoYi-Vue/sql/20260604_portal_dept_role_list_permission_seed.sql`
- `RuoYi-Vue/sql/20260604_portal_product_category_permission_seed.sql`
- `RuoYi-Vue/sql/20260604_seller_product_schema_permission_seed.sql`
- `RuoYi-Vue/sql/20260604_buyer_product_schema_permission_seed.sql`
- `RuoYi-Vue/sql/20260607_portal_self_audit_permission_seed.sql`
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`

## 子 Agent 使用记录

- 本轮启动 6 个只读子 Agent，全部使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- 6 个子 Agent 均已关闭。
- 采纳结果：
  - 切片 3 SQL/seed/guard 发现并采纳 1 个 P1：端内页面菜单 component 缺当前端根路径校验。
  - 其他切片未发现可坐实 P0/P1。
  - 切片 4 与切片 5 额外生成了只读审计 Markdown，主线程未把它们作为代码修复依据之外的阻塞项。

## 数据源与远端影响

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮只修改本地 SQL seed / migration 文件和 Java 合同测试。

## 三端隔离判断

- 管理端仍保留若依 `sys_*` 控制面。
- 卖家/买家端内菜单继续使用 `seller_menu` / `buyer_menu`。
- 本轮增强的是端内菜单 SQL guard：页面菜单 `C` 不允许空 component，也不允许跨端 component。
- seller 端页面菜单必须匹配 `Seller/%`；buyer 端页面菜单必须匹配 `Buyer/%`。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`
  - 首次运行失败，暴露同类 split terminal permission seed 也需要同步根路径校验。
  - 补齐后通过：77 tests，0 failures，0 errors，0 skipped。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，输出 `three-terminal manifest check passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`
  - 通过，输出 `Portal token isolation guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`
  - 通过，输出 `Partner management template guard passed.`。
- 静态复查：
  - `rg -n "page menus require component(?! under)|where menu_type = 'C'\s*\r?\n\s*and coalesce\(component, ''\) = ''" --pcre2 RuoYi-Vue\sql`
  - 无旧页面菜单 component 非空校验残留命中。
- `git diff --check`
  - 通过；仅提示当前工作区已有 LF/CRLF 换行转换 warning。
- 本轮触达文件行尾空白检查
  - 通过，无命中。
- `codegraph sync .`
  - 通过；代码变更同步时输出 `Synced 1 changed files`，文档回填后最终复跑输出 `Already up to date`。

## 未验证原因

- 未运行浏览器、截图、DOM 或 UI 细调验收，符合当前快速推进边界。
- 未回放远端 SQL；本轮是本地 seed / migration 和合同测试修复，远端 DDL/DML 虽已被用户确认可执行，但当前 P1 不需要立即触达远端数据。

## 残留问题

- 无新增 P0/P1。
- P2 继续沿用既有记录：远端 `portal.seller.web.url` / `portal.buyer.web.url` 当前仍是本地验证占位地址，当前阶段不阻塞 P0/P1。

## 一句话总结

已将 seller/buyer 端内页面菜单 component guard 从“非空”收紧为“必须在当前端根路径下”，并由 SQL 合同测试固定。
