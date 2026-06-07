# 2026-06-07 三端 P0/P1 免密 Key 与票据审计作用域收口记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，继续三端独立账号权限改造。当前只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent

- 本轮启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 已采纳：
  - 免密登录旧 Redis key `portal_direct_login:{token_hash}` 仍可被认证链路读取。
  - 管理端免密票据列表按账号筛选时缺少主体 ID 和账号归属校验。
  - seller/buyer 管理端账号编辑接口漏 `@Validated`。
- 已记录为后续 P1：
  - `source_warehouse_stock_read_model.sql` 缺专项 replay-safe 合同。
  - React `portalPaths` JS sidecar / `session.js` guard 覆盖仍有盲区。
- 未作为本轮 P1：
  - seller/buyer 端内权限主链未发现继续依赖 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。

## 已完成

- `PortalDirectLoginSupport.getPayload(...)` 改为只读取 `portal_direct_login:{terminal}:{token_hash}`。
- 旧 `portal_direct_login:{token_hash}` 只保留为历史残留删除目标，不再作为认证读取来源。
- `PortalDirectLoginSupportTest` 将旧 key 用例改为负向：只存在旧 key 时拒绝登录、票据置为 `EXPIRED`、同时清理新旧 key。
- `PortalDirectLoginAuthContractTest` 增加合同，禁止重新读取 `redisCache.getCacheObject(legacyCacheKey(tokenHash))`。
- `SellerServiceImpl` / `BuyerServiceImpl` 的免密票据列表按账号筛选时，强制要求主体 ID，并通过 scoped account mapper 校验账号归属。
- `AdminSellerController.editAccount(...)` / `AdminBuyerController.editAccount(...)` 补齐 `@Validated @RequestBody`。
- `SellerServiceImplTest` / `BuyerServiceImplTest` 增加 ticket 审计列表账号筛选作用域测试。
- `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest` 增加账号编辑请求体校验合同。
- `AGENTS.md`、`docs/architecture/reuse-ledger.md`、`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 和目标追踪已同步。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalDirectLoginSupportTest,PortalDirectLoginAuthContractTest" test`：通过，19 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer,ruoyi-system "-Dtest=SellerServiceImplTest,BuyerServiceImplTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,PortalDirectLoginSupportTest,PortalDirectLoginAuthContractTest" test`：通过，129 个测试通过。
- 静态搜索生产代码未发现 `redisCache.getCacheObject(legacyCacheKey(tokenHash))`。
- 静态搜索确认 seller/buyer `editAccount` 均包含 `@Validated @RequestBody`。
- `cd E:\Urili-Ruoyi; git diff --check -- ...`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，`Synced 11 changed files`，`Modified: 11 - 1,106 nodes in 2.0s`。

## 未验证

- 未执行浏览器、截图、DOM 检测：用户已明确当前快速推进模式不做这些验收。
- 未连接远程 MySQL / Redis，未执行 DDL/DML：本轮为 Java service、controller、测试和 Markdown 规则收口，不需要改远程数据。
- 未执行完整 `verify-three-terminal`：本轮先跑最小必要 Java 测试；后续集中验证入口切片再跑全量。

## 残留

- P1：`source_warehouse_stock_read_model.sql` 需要补专项 replay-safe 合同。
- P1：React portal JS sidecar 与 `check-portal-token-isolation.mjs` 需要补强。
- P2：商品模板相关浏览器 smoke 规则只适用于商品切片，不作为当前账号权限 P0/P1 通用验收门槛。
