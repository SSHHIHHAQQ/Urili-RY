# 2026-06-08 三端 P0/P1 快速推进记录：JS 镜像、SQL Guard 与 Portal Schema 合同

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：按当时用户规则优先尝试 `gpt-5.3-codex-spark` 子 Agent；现行规则为默认使用 `gpt-5.4`。
- 平台返回额度限制，提示需等到 `2026-06-14 15:12` 后再试；失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 采纳的 P1：Portal 首页非成功响应误当成功、React JS 镜像与 TS/TSX 源分叉、direct-login SQL replay guard 缺精确目标和列定义 assert、Product Portal schema 测试未固定端内 DTO 边界。

## 已修复问题

- `Portal/Home/index.tsx` 增加 portal 响应成功码断言：
  - `info`、`subject`、`account` 必须成功后才进入页面数据。
  - `accounts`、`depts`、`roles`、`sessions` 非成功响应不再被当作空数据静默消费。
  - `sessions` 加载失败不再覆盖已有会话列表为 `[]`。
- React JS 镜像改为纯 re-export，运行入口和 guard 只维护 TS/TSX 源：
  - `access.js`、`app.js`、`requestErrorConfig.js`、`config/proxy.js`
  - portal request/path/message/storage/session 相关 JS 镜像
  - `RemoteMenuRouteGuard.js`、`Portal/DirectLogin/index.js`、`Portal/terminal.js`
- 前端 guard 脚本同步识别“纯 re-export JS 镜像”，真实逻辑只扫描 TS/TSX 源。
- `PortalPasswordChangeContractTest` 固定 `session.ts` 为真实实现、`session.js` 为纯 re-export。
- `app.tsx` 增加 `InitialStateData` 类型适配；`AvatarDropdown`、`Login`、`Welcome` 统一通过 `selectInitialStateModel` 读取 `@@initialState`，解决纯 re-export 后暴露出的类型检查问题。
- `20260604_portal_direct_login_ticket.sql` 增加：
  - legacy normalize 的 expected count/signature。
  - normalize 目标集合 assert。
  - dynamic DDL replay 后的最终列定义 contract assert。
- `20260607_admin_partner_owner_reset_permission_cleanup.sql` 增加事务和清理完成 assert。
- `20260607_terminal_login_log_direct_login_audit.sql`、`20260607_terminal_oper_log_direct_login_audit.sql` 增加 replay-safe column modify 和最终列定义 assert。
- `20260608_terminal_menu_auto_increment_reset.sql` 增加 `AUTO_INCREMENT` reset 后的 post assert。
- `SqlExecutionGuardContractTest` 固定上述 SQL guard 合同。
- `ProductPortalSchemaServiceImplTest` 补齐端内商品 schema DTO 白名单、发布状态、可见状态和启用选项过滤合同。

## 新增或更新的验证

- `react-ui/tests/portal-home-error-handling.test.ts`：固定 Portal 首页非成功响应不能被当作成功数据。
- `ProductPortalSchemaServiceImplTest`：固定端内 schema 只返回 portal DTO 所需字段，不暴露身份、审计、token 等后台字段。
- `SqlExecutionGuardContractTest`：固定 SQL 脚本必须 fail-closed、必须带确认 token、必须在 dynamic DDL 后验证最终列定义。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/portal-home-error-handling.test.ts --runInBand`：当时通过，1 个 suite / 2 个测试；当前公开 `npm run test:unit` 入口已收口为 `verify-three-terminal`，复核请使用 `npm run verify:three-terminal` 或直接调用 `.\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath ... --runInBand`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -f RuoYi-Vue/pom.xml -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`：通过，54 个测试。
- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -f RuoYi-Vue/pom.xml -pl product -Dtest=ProductPortalSchemaServiceImplTest test`：当时通过，3 个测试；当前 `product` 定向复核应按 reactor 模板带 `-am`，例如 `mvn -pl product -am "-Dtest=ProductPortalSchemaServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard、React typecheck、11 个 Jest suite / 52 个测试、后端 reactor `test-compile` 和三端合同测试全部通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 26 个变更文件，`Added: 1, Modified: 25 - 539 nodes in 1.4s`。

## 权限、字典、复用与边界

- 权限检查结果：本轮未新增后端业务接口；主要固定已有三端 guard、route、SQL 和 DTO 合同。
- 字典/选项复用检查结果：本轮未新增字典或选项字段。
- 复用台账检查结果：本轮未新增公共业务组件或公共后端服务；JS 镜像改纯 re-export 后减少重复实现，不需要追加复用台账条目。
- 重复代码检查结果：已消除多处 JS/TS 双实现分叉风险。
- 大文件合理性判断结果：本轮主要修改现有 guard、SQL、合同测试和入口镜像；未新增需要拆分的单一职责业务大文件。
- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮按用户要求未做浏览器、截图、DOM 或 UI 细调验收。

## 残留问题

- P2：历史 Markdown 记录可能仍描述旧的 JS/TS 镜像同步方式，后续可集中整理，不阻塞当前 P0/P1。
- P2：`verify:three-terminal` 是三端隔离与关键合同守门，不等同完整浏览器运行态回归；按当前快速推进模式不做浏览器验收。
