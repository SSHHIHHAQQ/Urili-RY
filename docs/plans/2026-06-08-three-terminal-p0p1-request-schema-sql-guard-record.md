# 2026-06-08 三端隔离 P0/P1 收口记录：请求拦截、Schema Preview 与 SQL Guard

## 目标边界

- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 执行模式：只修 P0/P1，覆盖编译、guard、接口、权限、串端、service/字段缺失。
- 本轮不做浏览器运行态验收、截图、DOM 检测或 UI 细调。
- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。

## 子 Agent 执行情况

- 历史记录（已过期口径）：按最新规则优先启动 6 个 `gpt-5.3-codex-spark` 子 Agent。
- 平台返回额度限制，提示需等到 `2026-06-14 15:12` 后再试；6 个失败 Agent 已关闭。
- 按 fallback 规则启动 6 个 `gpt-5.4` 只读子 Agent，并已全部关闭。
- 采纳的 P1：
  - 管理端请求拦截器不应在请求发出前因本地 `expireTime` 缺失或过期直接清理 admin token。
  - Seller/Buyer Portal 商品 Schema Preview 未进入专门 guard 与 manifest 测试。
  - 若干 SQL 脚本缺精确目标确认、事务边界或动态 DDL 后的最终字段/索引定义断言。
- 未采纳为阻塞项：
  - 一个子 Agent 提到 seller/buyer 直接模块测试曾出现 `NoSuchMethodError`；本轮 `npm run verify:three-terminal` 中 seller/buyer service 测试已通过，未复现，按 P2/已复核记录。

## 已完成

- `react-ui/src/app.tsx` 与 `react-ui/src/app.js`：
  - 请求拦截器只在本地 token 未过期时附带 `Authorization`。
  - 本地过期或缺少 `expireTime` 时不再请求前清 token，交给响应 401 统一处理。
- `react-ui/tests/portal-unauthorized-redirect.test.ts`：
  - 增加本地过期/缺失 `expireTime` 不清 admin token 的合同。
  - 增加未过期时正常附带 admin `Authorization` 的合同。
- `react-ui/scripts/check-seller-portal-product-template.mjs` 与 `react-ui/scripts/check-buyer-portal-product-template.mjs`：
  - 增加 Seller/Buyer `ProductSchemaPreview` 端内 service 绑定检查。
  - 禁止跨端 schema API、直接 request、admin 商品 service 混入 portal preview。
  - 检查 `Portal/Home/index.tsx` 中 seller/buyer 分支渲染对应 preview。
- `react-ui/tests/portal-product-schema-preview.test.ts` 与 `react-ui/tests/three-terminal.manifest.json`：
  - 增加 schema preview 静态合同并纳入三端 manifest。
- `RuoYi-Vue/sql/20260608_terminal_menu_auto_increment_reset.sql`：
  - 增加 seller/buyer menu exact target count/signature。
  - 在 auto_increment reset 前校验当前菜单集合，避免合法区间内的漂移被静默固化。
- `RuoYi-Vue/sql/20260608_overseas_channel_carrier_menu_restructure.sql`：
  - 将 sys_menu/sys_role_menu 多步写操作放进事务。
- `RuoYi-Vue/sql/20260604_upstream_system_code_correction.sql`：
  - 将连接表 code 更新和字典 delete/reseed 放进事务。
- `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`：
  - 增加即将写入的 `(role_id, menu_id)` grant exact target count/signature。
  - 将两段 `sys_role_menu` grant 放进同一事务。
- `RuoYi-Vue/sql/20260605_seller_account_lock_control.sql` 与 `RuoYi-Vue/sql/20260605_buyer_account_lock_control.sql`：
  - 在动态 DDL helper 后增加 `lock_status`、`lock_reason`、锁定索引的最终定义断言。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`：
  - 固定上述 SQL P1 guard，防止后续脚本退回弱确认或半事务写法。

## 验证结果

- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/portal-unauthorized-redirect.test.ts --runInBand`：当时通过，1 个 suite / 15 个测试；当前公开 `npm run test:unit` 入口已收口为 `verify-three-terminal`，不再支持 `--runTestsByPath` / `--runInBand` 参数透传，复核请使用 `npm run verify:three-terminal` 或直接调用 `.\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath ... --runInBand`。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-seller-portal-product-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-buyer-portal-product-template.mjs`：通过。
- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/portal-unauthorized-redirect.test.ts tests/portal-product-schema-preview.test.ts --runInBand`：当时通过，2 个 suite / 18 个测试；当前复核请使用 `npm run verify:three-terminal` 或直接调用 Jest 二进制。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，53 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard、React typecheck、11 个 Jest suite / 51 个测试、后端 reactor `test-compile`、后端三端合同测试全部通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；`Synced 7 changed files`，`Added: 1, Modified: 6 - 250 nodes in 990ms`。

## 边界说明

- 本轮只做代码级 P0/P1 收口，没有执行远程数据库变更。
- 本轮没有启动或重启后端。
- 本轮没有做浏览器、截图、DOM 或 UI 细调验收。
- 工作区仍存在其他前序库存、商品、demo 图片等改动，本轮未回滚也未作为本检查点的业务结论。
