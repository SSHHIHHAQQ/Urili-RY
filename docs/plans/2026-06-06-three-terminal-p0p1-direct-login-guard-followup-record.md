# 2026-06-06 三端 P0/P1：免密登录与 SQL Guard 补充收口记录

## 目标

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。

本轮不做浏览器运行态验收、不做截图、不做 DOM 检测、不做 UI 细调。

## 子 Agent 使用情况

- 按用户要求使用 `gpt-5.4` 并行启动 6 个只读子 Agent，覆盖 seller 后端、buyer 后端、portal auth/log/session、SQL/seed、React portal 请求、product/integration/inventory 边界。
- 6 个子 Agent 均已关闭。
- 采纳并修复的 P0/P1：
  - 端内权限/日志/OWNER 约束 SQL 缺运行时确认 guard。
  - legacy `sys_user` 端账号回填 helper 缺契约覆盖。
  - 管理端免密登录桥接在收到合法 READY 前会主动发送 token。
  - `PortalDirectLoginSupport` 暴露无 validator 的 token 消费入口。
  - `PortalDirectLoginSupport.createToken(...)` 未在服务层显式校验 acting admin。
- 未采纳为本轮阻塞：
  - `seller_menu` / `buyer_menu` 当前是端级菜单池，不带 `seller_id` / `buyer_id`。该点与“菜单是否 subject-scoped”存在设计表述冲突，记录为 P2 澄清项，不在本轮擅自改表。
  - 商品/订单类 SQL 仍有无 guard 脚本，属于商品域/菜单占位治理，未作为三端身份 P0/P1 扩大处理。

## 新增问题

- P1：`20260604_portal_account_list_permission_seed.sql`、`20260604_portal_dept_role_list_permission_seed.sql`、`20260604_portal_product_category_permission_seed.sql`、`20260604_seller_product_schema_permission_seed.sql`、`20260604_buyer_product_schema_permission_seed.sql`、`20260605_terminal_owner_account_unique_constraint.sql`、`20260606_terminal_log_scope_indexes.sql` 会写端内权限或结构，但缺显式确认 guard。
- P1：`20260604_three_terminal_legacy_sys_user_account_backfill.sql` 是旧混用库 DML helper，虽然已有双确认，但未被 SQL 契约测试固定 fail-fast 条件。
- P1：`react-ui/src/utils/portalDirectLoginMessage.ts` 与 `.js` 中曾在合法 READY 前通过 timer 主动 `postMessage` 免密 token。
- P1：`PortalDirectLoginSupport` 曾保留 `consumeToken(String portalType, String token)` 无 validator 重载，后续调用方可能绕过目标主体/账号状态校验。
- P1：`PortalDirectLoginSupport.createToken(...)` 曾依赖数据库非空约束兜底 acting admin，服务层没有明确 fail-close。

## 已修复问题

- 为上述 7 个端内权限/日志/OWNER SQL 脚本补充 `@confirm_*`、确认 token、`assert_*_confirmed()` 和 `signal sqlstate '45000'`。
- `SqlExecutionGuardContractTest` 纳入新增 guard 脚本，并增加 legacy `sys_user` backfill helper 的双确认、旧库限定、缺 `sys_user` / 空密码 fail-fast 契约。
- `portalDirectLoginMessage.ts` / `.js` 删除 READY 前主动投递 token 的定时器，只在 `event.source`、`event.origin`、`terminal` 均匹配后发送一次 token。
- `check-portal-token-isolation.mjs` 同时检查 `.ts` / `.js`，并禁止 `setInterval(postToken` / `setTimeout(postToken` 这类 READY 前主动投递模式。
- `portal-direct-login-message.test.ts` 增加 fake timer 断言：未收到合法 READY 前推进时间也不得发送 token。
- `verify-three-terminal.mjs` 将 portal token/session 相关 Jest 单测纳入标准验证链。
- `PortalDirectLoginSupport` 删除无 validator 消费重载，并在 validator 为空时直接抛错。
- `PortalDirectLoginSupport` 在创建票据前显式解析并校验 acting admin；缺失时抛 `免密登录后台操作人不能为空`，不写票据、不写 Redis。
- `PortalDirectLoginSupportTest`、`PortalDirectLoginAuthContractTest`、seller/buyer service 测试桩同步更新，避免测试层重新引入无 validator 消费模式。

## 残留问题

- P2：`seller_menu` / `buyer_menu` 是端级菜单池还是 subject-scoped 菜单池需要后续明确；当前没有擅自加 `seller_id` / `buyer_id` 改表。
- P2：商品/订单/仓库相关 SQL guard 治理需要单独按业务域收口，本轮没有扩大到非三端身份脚本。
- P2：仍未做浏览器运行态验证和 UI 细调，符合本轮快速推进边界。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=PortalDirectLoginSupportTest,PortalDirectLoginAuthContractTest,SqlExecutionGuardContractTest" test`：通过，`16` 个测试通过。当前可复用命令必须保留 `-am`，避免本机 Maven 缓存掩盖 reactor 依赖漂移。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/portal-direct-login-message.test.ts tests/terminal-session-token.test.ts tests/portal-session-request.test.ts --runInBand`：通过，`3` 个 test suite、`7` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；覆盖 portal token guard、partner/product guards、React typecheck、portal Jest 单测和后端三端合同。

## 未验证原因

- 未启动浏览器、未截图、未做 DOM 检测或 UI 细调，因为用户已明确排除。
- 未重启后端服务，本轮以代码、类型检查、Jest 和 Maven 合同测试为准。
- 未连接 Redis。
- 未执行远程 MySQL DDL/DML，本轮只修改 SQL 文件和契约测试。

## 权限检查结果

- seller/buyer 后端主链路子 Agent 未发现继续依赖 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 做端内账号权限控制的 P0/P1。
- `PortalDirectLoginSupport` 现在强制调用方提供 validator，避免端内主体/账号状态校验被遗漏。
- 前端免密 token 只在目标 popup、目标 origin、目标 terminal 的 READY 握手后发送。

## 字典/选项复用检查结果

- 本轮未新增字典或业务选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，补充三端免密登录和 SQL guard 收口规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，返回 `Synced 11 changed files`，`Modified: 11 - 616 nodes`。

## 大文件合理性判断结果

- `PortalDirectLoginSupportTest.java` 和 `verify-three-terminal.mjs` 已超过 300 行附近或接近复杂测试入口规模，但职责仍单一：分别固定免密登录支持类契约和三端验证入口。本轮不拆分，避免在 P0/P1 快速模式中引入结构性改动。
- 其他本轮新增/修改内容为小范围 guard、测试和记录。

## 重复代码检查结果

- 免密登录发送侧同步修复 `.ts` 和 `.js` 双文件，属于当前项目已有双文件产物并存现状；本轮没有引入新的业务逻辑分叉。
- seller/buyer 测试桩同步删除无 validator 重载，保持同构。
