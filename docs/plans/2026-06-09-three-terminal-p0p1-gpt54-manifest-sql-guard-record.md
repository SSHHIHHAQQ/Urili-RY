# 2026-06-09 三端 P0/P1 manifest 与 SQL guard 修复记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。浏览器、截图、DOM 和 UI 细调不作为本轮阻塞项。

## 新增问题

本轮 6 个 `gpt-5.4` 子 Agent 和主线程复核后，坐实 4 个 P1，未发现 P0。

1. `verify-three-terminal` 前端测试发现会扫描整个 `react-ui`，但未忽略本地生成目录 `src/.umi-test`、`src/.umi-undefined`、`test-results`，可能被生成物里的测试或镜像误伤。
2. `criticalBackendExplicitTestClasses` 未显式纳入卖家/买家管理关键合同：`AdminAccountPermissionUiContractTest`、`SellerAdminPermissionContractTest`、`BuyerAdminPermissionContractTest`、`StandalonePartnerSeedMenuContractTest`。
3. `seller_buyer_management_seed.sql` 的 `PATCH_EXISTING` 路径没有把已存在 `seller_account.password` / `buyer_account.password` 终态固定为 `varchar(100) not null` 且无默认值。
4. `SqlExecutionGuardContractTest` 自动发现端内菜单 SQL 时，对 `update seller_menu` / `update buyer_menu` 没有拦截把 `component` 或 `perms` 更新为空的回归。

## 已修复问题

- `react-ui/scripts/verify-three-terminal.mjs` 已把 `.umi-test`、`.umi-undefined`、`test-results` 加入前端测试发现忽略目录。
- `react-ui/tests/three-terminal.manifest.json` 已把 4 个卖家/买家管理关键后端合同纳入 `criticalBackendExplicitTestClasses`。
- `react-ui/tests/verify-three-terminal-backend-gate.test.ts` 已补充：
  - 生成目录里的本地测试不会污染 manifest 发现。
  - 4 个关键后端合同必须保持 critical。
  - 从 manifest 删除这些关键合同会 fail-closed。
- `RuoYi-Vue/sql/seller_buyer_management_seed.sql` 已补充端账号密码列 fail-closed 流程：
  - 缺少 `password` 列直接失败。
  - 存在 null 或空白密码直接失败，不生成默认密码，不兜底成空串。
  - 数据合格时才把 `password` 列修正为 `varchar(100) not null` 且无默认值。
  - seed 完成态再次校验 `seller_account.password` / `buyer_account.password` 最终列结构。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java` 已补充：
  - `PATCH_EXISTING` 密码列终态合同。
  - 端内菜单 `UPDATE` 把 `component` / `perms` 改空的负例。

## 残留问题

- 本轮未发现新的 P0/P1。
- P2：`portal.seller.web.url` / `portal.buyer.web.url` 仍是本地验证占位地址，继续记录，不阻塞当前 P0/P1。
- 浏览器、截图、DOM、UI 细调按快速推进模式跳过。

## 数据源确认

- 当前配置来源：
  - MySQL：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`，通过 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD` 注入。
  - Redis：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`，通过 `RUOYI_REDIS_*` 注入。
- 本轮未读取远程 MySQL 数据。
- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮只修改 SQL 脚本和合同测试，未回放到远端库。

## 表设计与高影响 SQL

- 本轮不新增业务表。
- 本轮修改已有 confirmed seed：`RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 该 seed 仍要求确认 token 和 profile；新增逻辑只加强 `PATCH_EXISTING` 的 fail-closed 终态校验。
- 密码列修正不生成密码、不写默认空串；如存在 null/空白密码，脚本会在修改列结构前 `45000` 失败。

## 三端隔离判断

- 管理端继续使用若依 `sys_*`。
- 卖家/买家账号、角色、菜单、部门、日志、会话仍在独立表和模块内。
- 本轮静态扫描未发现 `RuoYi-Vue/seller` / `RuoYi-Vue/buyer` 生产代码混用 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。
- 本轮静态扫描未发现生产代码保留裸 `select*AccountById(accountId)` 查询；账号查询仍带 `sellerId/buyerId + accountId`。
- direct-login、session、日志、自助 DTO 切片未发现新的串端 P0/P1。

## 子 Agent 使用记录

- 本轮启动 6 个子 Agent，全部使用 `gpt-5.4`，全部已关闭，未使用 GPT-5.3 Codex。
- 切片 1：后端 seller/buyer 账号、角色、菜单、部门链路，未发现 P0/P1。
- 切片 2：direct-login、session、Redis key、日志审计，未发现 P0/P1。
- 切片 3：SQL seed/migration/guard，发现并采纳 2 个 P1。
- 切片 4：React 管理端 seller/buyer 路由、service、权限，发现并采纳 2 个 manifest critical P1。
- 切片 5：portal token/request/proxy/access/direct-login，未发现 P0/P1。
- 切片 6：验证入口、manifest、测试发现范围，发现并采纳 1 个 P1。

## 权限检查

- 本轮未新增接口权限。
- `AdminAccountPermissionUiContractTest`、`SellerAdminPermissionContractTest`、`BuyerAdminPermissionContractTest` 已加入 manifest critical，确保账号角色弹窗权限、重置密码语义、session list / force logout 分权继续被三端总门覆盖。
- `StandalonePartnerSeedMenuContractTest` 已加入 manifest critical，确保卖家/买家管理菜单 seed 和权限契约继续被三端总门覆盖。

## 字典和复用

- 本轮不新增字典、状态选项或业务选择器。
- 本轮不新增公共组件或 service 抽象；不需要更新复用台账。

## 大文件与重复代码判断

- 本轮修改的 SQL 与合同测试属于既有治理文件，职责仍是三端 seed / SQL guard 合同。
- `SqlExecutionGuardContractTest.java` 已是大型合同测试文件，本轮只追加 P0/P1 断言，不在本轮拆分，避免在快速推进模式中引入无关重构。
- 未新增重复业务逻辑。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，`three-terminal manifest check passed`。
- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests/verify-three-terminal-backend-gate.test.ts --runInBand`
  - 通过，1 suite / 18 tests。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`
  - 通过，79 tests，0 failures，0 errors，0 skipped。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - Frontend Jest：23 suites / 186 tests 通过。
  - Backend reactor test-compile：14 个模块全部 SUCCESS，包含 `ruoyi-admin`。
  - Backend three-terminal contracts：seller 100 tests、buyer 101 tests 通过。

## 未验证原因

- `cd E:\Urili-Ruoyi\react-ui; npx jest tests/verify-three-terminal-backend-gate.test.ts --runInBand`
  - 未采用为有效验证；仓库同时存在 `jest.config.js` 和 `jest.config.ts`，隐式 config 被 Jest 拒绝。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/verify-three-terminal-backend-gate.test.ts --runInBand`
  - 未采用为有效验证；当前 `test:unit` 已接入 `verify-three-terminal`，只允许 `--coverage`、`-u`、`--updateSnapshot` 透传。
- 浏览器、截图、DOM 检测按快速推进模式跳过。

## CodeGraph

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 一句话总结

本轮用 6 个 `gpt-5.4` 子 Agent 做 P0/P1 切片审计，修复 4 类可坐实 P1，并通过三端总门验证。
