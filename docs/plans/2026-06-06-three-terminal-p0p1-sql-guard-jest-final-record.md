# 2026-06-06 三端 P0/P1：SQL Guard、验证入口与 Portal 请求收口记录

## 目标

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。

本轮不做浏览器运行态验收、不做截图、不做 DOM 检测、不做 UI 细调。

## 子 Agent 使用情况

- 用户要求优先使用 GPT-5.3 Codex；当前可用子 Agent 实际按降级方案使用 `gpt-5.4`。
- 已并行使用 6 个 `gpt-5.4` 子 Agent 做只读审计，覆盖后端 seller/buyer 主链路、portal auth/log、SQL/seed、React 管理端、验证脚本、integration/product/inventory 边界。
- 采纳的 P0/P1：验证入口可绕过、SQL 缺少运行时确认 guard、库存权限与集成权限串线、portal 请求/直登消息缺最小单测、远端清理 SQL 在 MySQL collation 下失败。
- 未采纳为阻塞项：seller/buyer 后端账号权限主链路未发现继续写回 `sys_*`；product/integration/warehouse 传递依赖偏重记录为后续结构治理项。

## 新增问题

- P1：`test:coverage`、`test:update`、`jest` 等入口可绕过 `verify:three-terminal`，存在假绿风险。
- P1：`verify-three-terminal.mjs` 只跑硬编码测试清单，新增后端三端测试可能未被纳入。
- P1：多份会改远端库的 SQL seed/migration 缺少脚本级确认 guard。
- P1：`integration:upstream:inventoryQuery` 曾可从 `inventory:sourceWarehouse:list` 自动继承，导致库存菜单权限和上游集成权限边界混线。
- P1：portal 请求参数清洗和 direct-login `postMessage` 来源校验缺少 Jest 层最小覆盖。
- P1：远端执行 `20260606_admin_partner_non_admin_button_grant_cleanup.sql` 时，旧 `find_in_set(role_key, @keys)` 在当前 MySQL collation 下失败。

## 已修复问题

- `react-ui/package.json`：`test`、`test:coverage`、`test:update`、`jest` 均先执行 `verify:three-terminal`；新增 `test:unit` 作为显式单测入口。
- `react-ui/scripts/verify-three-terminal.mjs`：新增后端测试源码存在性、未列入测试检测、重复测试类名检测、surefire report 产出检测，并把 `SqlExecutionGuardContractTest` 纳入必跑清单。
- `react-ui/tests/portal-session-request.test.ts`：覆盖 seller/buyer portal 请求 token 选择、范围参数清洗、admin API 不被识别为 portal 请求。
- `react-ui/tests/portal-direct-login-message.test.ts`：覆盖 direct-login token 只向匹配 popup、origin、terminal 的 ready 消息发送。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`：固定高影响 SQL 必须带确认令牌，并固定库存菜单权限不得反授集成库存权限。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceWarehouseStockController.java`：全局来源仓库库存列表只接受 `inventory:sourceWarehouse:list`，不再接受 `integration:upstream:inventoryQuery`。
- `RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql`：新增 `@confirm_upstream_inventory_dimension_sync`，并去掉从 `inventory:sourceWarehouse:list` 自动授予 `integration:upstream:inventoryQuery`。
- `RuoYi-Vue/sql/business_menu_seed.sql`：默认把来源仓库库存菜单覆盖为 `Common/PlannedPage/index` 占位；真实页面恢复必须走专用确认脚本。
- `RuoYi-Vue/sql/20260606_source_warehouse_stock_menu_rename.sql`：新增 `@confirm_source_warehouse_stock_menu_rename`。
- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`、`20260604_portal_direct_login_ticket.sql`、`20260605_seller_account_lock_control.sql`、`20260605_buyer_account_lock_control.sql`、`20260606_admin_partner_page_direct_login_seed.sql`、`warehouse_management_seed.sql`：均补运行时确认 guard。
- `RuoYi-Vue/sql/20260606_admin_partner_non_admin_button_grant_cleanup.sql`、`20260606_legacy_disable_sys_seller_buyer_roles.sql`：`role_key` 白名单判断改为带 `utf8mb4_unicode_ci` collation 的 `find_in_set`，适配当前远端 MySQL。
- `AdminDirectLoginPermissionContractTest`：同步校验带 collation 的白名单表达式。

## 远端数据库执行记录

- 连接来源：本机 `.env.local` 注入的当前若依远端 MySQL 配置；记录不输出 JDBC URL、账号、密码、Redis 信息或 token secret。
- 执行前预览：
  - `codex_seller_audit_only` / role_id `102`：待清理非 admin 子按钮授权 `32` 条。
  - `codex_buyer_audit_only` / role_id `103`：待清理非 admin 子按钮授权 `32` 条。
  - 合计待清理 `64` 条。
  - 旧 `seller` / `buyer` sys_role 已是 disabled/deleted 状态，本轮未执行 legacy sys_role cleanup。
- 第一次执行：因 MySQL collation 混用导致 `find_in_set` 报错，事务未提交，未形成有效 DML。
- 修复 collation 后第二次执行：`20260606_admin_partner_non_admin_button_grant_cleanup.sql` 成功执行，`executedStatements=22`，`positiveUpdateCountSum=66`，`remainingTargetButtonGrants=0`。
- 本轮未对库存菜单占位、SQL guard、warehouse seed、portal ticket 等脚本做远端回放；这些是代码/脚本层收口。

## 残留问题

- P2：seller/buyer portal 仍通过 `product -> warehouse -> integration` 形成较重传递编译依赖，后续可拆只读 facade/contract。
- P2：强制踢出审计仍主要依赖管理端 `sys_oper_log` 与端内 login log 关联，端内 oper log 里没有直接写 `actingAdminId`。
- P2：`portal_direct_login_ticket` 列表 mapper 仍可返回 `token_hash` 字段；当前 UI 未展示，但后续应改安全投影。
- P2：当前运行库如果此前已回放过库存真实菜单，本轮未执行远端 DML 回滚；代码和 seed 已收口，运行库是否调整需要单独确认。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/terminal-session-token.test.ts tests/portal-session-request.test.ts tests/portal-direct-login-message.test.ts --runInBand`：通过，`3` 个 test suite、`7` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；后端三端合约测试包含 `SqlExecutionGuardContractTest`。
- `cd E:\Urili-Ruoyi\react-ui; npm run jest -- --runInBand`：通过，先跑 `verify:three-terminal`，再跑 Jest，`3` 个 test suite、`7` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run jest -- --listTests`：通过，确认参数正确下传并列出 3 个 Jest 测试文件。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。

## 未验证原因

- 未做浏览器运行态、截图、DOM 或 UI 细调验收，因为用户明确要求本轮不做。
- 未重启后端服务；本轮以代码、脚本、单测、合约测试验证为准。
- 未读写 Redis。
- 除非 admin partner 非 admin 子按钮清理外，未执行其他远端 DDL/DML。

## 权限检查结果

- 管理端 seller/buyer 主链路子 Agent 审计未发现端内账号权限继续写回 `sys_*` 的 P0/P1。
- 来源仓库库存全局接口已收窄到 `inventory:sourceWarehouse:list`。
- `integration:upstream:inventoryQuery` 不再从 `inventory:sourceWarehouse:list` 自动继承。
- 高影响 SQL 脚本新增 `SqlExecutionGuardContractTest` 固定必须显式确认。

## 字典/选项复用检查结果

- 本轮未新增字典或业务选项。
- 库存能力仍处于未确认/占位状态，未恢复真实页面前不得新增页面内库存状态映射。

## 复用台账检查结果

- 已追加复用台账：三端验证入口、portal 请求/直登消息 Jest 模板、高影响 SQL 确认 guard。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：第一次通过，输出 `Synced 6 changed files`，`Added: 3, Modified: 3 - 95 nodes`；记录回填后二次同步通过，输出 `Already up to date`。

## 大文件合理性判断结果

- 新增 `SqlExecutionGuardContractTest.java`、两个 Jest 测试文件和本记录均职责单一，未触发 300 行以上复杂业务文件治理风险。
- `verify-three-terminal.mjs` 已是三端验证聚合脚本，本轮新增的是测试发现和 report 校验逻辑，职责仍集中。

## 重复代码检查结果

- seller/buyer 未新增业务逻辑复制。
- portal 请求清洗和 direct-login 消息校验通过测试固定现有公共方法，未在页面中散写重复逻辑。
