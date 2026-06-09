# 2026-06-08 三端 P0/P1：SQL 精确目标、Auth 边界与路由 Guard 记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

本轮继续执行快速推进模式：只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 历史记录（已过期口径）：按当时规则先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；现行规则为默认使用 `gpt-5.4`。
- 平台返回额度限制：需等到 `2026-06-14 15:12` 后再试。
- 已关闭失败的 GPT-5.3 子 Agent，并按 fallback 规则启动 6 个 `gpt-5.4` 只读子 Agent。
- 6 个 `gpt-5.4` 子 Agent 均已完成并关闭。

## 采纳的 P1

- `20260604_three_terminal_isolation_migration.sql` 对 `seller_account` / `buyer_account` 做账号字段规范化，以及删除 legacy `user_id` 列前缺少精确目标 count/signature。
- `20260604_upstream_system_code_correction.sql` 对 `upstream_system_connection` 和 `sys_dict_data` 的旧 code 更新/删除只有确认 token，缺少精确目标 count/signature。
- `TerminalRouteOwnershipTest` 只扫描 `product` 模块，非 seller/buyer 模块暴露 `/seller/**` 或 `/buyer/**` 的假绿风险仍在。
- `PortalAnonymousEndpointContractTest` 对 auth controller 只校验注解形态，没有固定认证入口不得读取客户端身份范围字段、不得使用管理端登录上下文。
- `check-portal-token-isolation.mjs` 对部分 JS sidecar 只检查包含 re-export 字符串，不能防止文件夹带副作用或绕过逻辑。

## 已完成

- `20260604_three_terminal_isolation_migration.sql`：
  - 新增 seller/buyer 账号规范化 expected count/signature。
  - 新增 seller/buyer legacy `user_id` 列删除 expected count/signature。
  - 在账号规范化 `update` 和 `drop_column_if_exists(..., 'user_id')` 前执行精确目标校验。
- `20260604_upstream_system_code_correction.sql`：
  - 新增 `@upstream_system_code_correction_expected_count/signature`。
  - 在更新 `upstream_system_connection` 和删除旧 `sys_dict_data` 前校验统一目标集合签名。
- `SqlExecutionGuardContractTest`：
  - 固定三端主迁移账号规范化、legacy `user_id` 删除和 upstream code correction 的 exact target guard。
- `TerminalRouteOwnershipTest`：
  - 从只扫描 `product` 扩展为扫描所有非 `seller` / `buyer` 后端模块。
- `PortalAnonymousEndpointContractTest`：
  - auth endpoint 不得接收或读取 `sellerId` / `buyerId` / `subjectId` / `accountId` 等客户端身份范围字段。
  - auth endpoint 和匿名 portal endpoint 不得使用 `SecurityUtils` / `LoginUser` / `SysUser` 等管理端登录上下文。
- `check-portal-token-isolation.mjs`：
  - `src/services/session.js`、`Portal/Login/index.js`、`Portal/Home/index.js` 必须是纯 re-export。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,TerminalRouteOwnershipTest,PortalAnonymousEndpointContractTest" test`
  - 通过：59 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端：4 个 guard 通过，React typecheck 通过，10 个 Jest suite / 46 个测试通过。
  - 后端：reactor `test-compile` 通过，三端合同测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过，CodeGraph 返回 `Already up to date`。

## 边界

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
