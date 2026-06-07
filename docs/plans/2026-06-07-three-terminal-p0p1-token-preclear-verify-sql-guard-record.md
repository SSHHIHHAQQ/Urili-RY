# 2026-06-07 三端 P0/P1：Token 预清、验证入口与 SQL Fail-Closed 记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮继续按快速推进口径执行：只处理 P0/P1，也就是编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 先按最新规则尝试 2 个 `gpt-5.3-codex-spark` 子 Agent。
- 平台返回 GPT-5.3 Codex Spark 用量限制，提示需等到 `2026-06-08 01:14` 后再试；失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 追加检查时再次按规则尝试 2 个 `gpt-5.3-codex-spark` 子 Agent，仍因用量限制失败并关闭；随后启动 6 个 `gpt-5.4` 只读子 Agent，已全部关闭。
- 采纳的 P1：
  - portal 登录页和 `/direct-login` 页面会在新 token 成功前清理已有端 token，失败或超时时可能误踢已有会话。
  - `verify-three-terminal` 前端关键测试发现范围过窄，后端 Maven `-pl` 模块列表落后于 reactor。
  - 三端隔离迁移会把存量端账号空密码规范成空串，而不是 fail-closed。
  - `seller_buyer_management_seed.sql` 的端内菜单 ID 区间 guard 执行晚于部分 DML。
- 未采纳为本轮改动：
  - 运行态按钮显隐、接口 200/403 对照和浏览器菜单缓存验证，按本轮边界不执行。
  - `ProductDistributionMapper` 跨模块表访问和商品库存聚合口径仍按设计确认/P2 处理。

## 已完成

- `Portal/Login/index.tsx` 移除提交前 `clearPortalLogin(terminal)`。
- `Portal/DirectLogin/index.tsx` 和 `.js` 移除挂载、消费失败、等待 token 超时路径里的 `clearPortalLogin(terminal)`。
- `terminal-session-token.test.ts` 增加合同，固定普通登录页和直登页不得在成功持久化前预清 terminal token。
- `verify-three-terminal.mjs` 的前端关键测试发现范围改为 `react-ui` 仓库级根目录，继续使用 ignored dirs 与关键路径过滤。
- `verify-three-terminal.mjs` 的后端合同测试 Maven 模块改为从 `RuoYi-Vue/pom.xml` reactor 动态派生存在 `src/test/java` 的模块。
- `TerminalAccountIsolationTest` 将 `LoginUser` 纳入 seller/buyer 端内控制面禁用清单，并扫描 portal shared support 与 portal AOP 切面。
- `20260604_three_terminal_isolation_migration.sql` 新增 `assert_no_blank_terminal_passwords(...)`，在端账号更新前拒绝 null/空白密码，并移除 `password = coalesce(password, '')`。
- `seller_buyer_management_seed.sql` 将 `assert_terminal_menu_range_ready()` 前移到端内表创建完成后、字典/sys_menu/sys_config/role/menu 权限 DML 前。
- `SqlExecutionGuardContractTest` 固定上述 SQL fail-closed 合同。
- `AGENTS.md` 与 `docs/architecture/reuse-ledger.md` 已同步新增规则。
- 追加修复 `persistPortalLogin(...)` terminal mismatch 串端副作用：响应端类型不等于当前页面端时，只清理当前页面端 token，不再清理响应声明的另一端 token。
- `check-portal-token-isolation.mjs` 追加固定：
  - 普通登录页和直登页不得在成功前调用 `clearPortalLogin(...)`。
  - `persistPortalLogin(...)` 不得调用 `clearPortalLogin(result.terminal)` 清理另一端。
- `terminal-session-token.test.ts` 更新 mismatch 合同，固定拒绝串写 token 时只清理当前页面端。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/terminal-session-token.test.ts --runInBand`：通过，1 个 suite / 5 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,SqlExecutionGuardContractTest" test`：通过，41 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest,StandalonePartnerSeedMenuContractTest" test`：通过，51 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 7 个 Jest suite / 34 个测试通过；后端 `ruoyi-system` 154、`ruoyi-framework` 15、`integration` 5、`product` 8、`seller` 96、`buyer` 97 个测试通过；后端 reactor `test-compile` 覆盖到 `ruoyi-admin` 及其依赖链，合同测试执行模块动态派生后跑通。
- `cd E:\Urili-Ruoyi; rg -n "清理相关端|清理不匹配端|clears both|both terminal slots|clearPortalLogin\(result\.terminal\)|clearPortalLogin\(result\?\.terminal\)" ...`：只命中本记录中新加的禁用项说明，不存在旧合同残留。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；只有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" ...`：无输出，本轮触碰文件未发现尾随空白。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；输出 `Synced 4 changed files`，`Modified: 4 - 122 nodes`。

## 边界

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 残留

- 运行态 403/200 对照、按钮显隐和浏览器菜单缓存验证仍不是当前快速推进阻塞项。
- MySQL DDL 隐式提交风险仍按既有迁移记录处理；本轮只增强空密码和菜单 ID 段 preflight。
- 长期结构债务仍包括商品配对投影下沉到 integration facade、库存事实源与聚合口径设计。
