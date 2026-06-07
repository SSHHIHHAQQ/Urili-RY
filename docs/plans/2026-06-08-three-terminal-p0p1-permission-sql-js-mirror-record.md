# 2026-06-08 三端 P0/P1 快速推进记录：分配角色权限、Legacy SQL Guard 与 JS 镜像

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 按用户最新规则优先启动 6 个 `gpt-5.3-codex-spark` 子 Agent，本轮均成功运行。
- 6 个子 Agent 已全部关闭。
- 只采纳能落到具体文件和合同的确定 P0/P1；未确认项不进入当前修复。

## 已采纳的 P1

- 后端 seller/buyer 管理端账号分配角色写接口：
  - `AdminSellerController#assignAccountRoles` 只要求 `seller:admin:account:role:edit`，缺少 `seller:admin:account:role:query` 和 `seller:admin:role:query`。
  - `AdminBuyerController#assignAccountRoles` 同样缺少 `buyer:admin:account:role:query` 和 `buyer:admin:role:query`。
- Legacy SQL helper：
  - `20260604_three_terminal_legacy_sys_user_account_backfill.sql` 只有 ID 白名单和 expected_count，缺少 seller/buyer account + sys_user 精确 target signature。
  - `20260606_legacy_disable_sys_seller_buyer_roles.sql` 已有 sys_role 精确 signature，但缺少端内 owner 角色已就绪的语义 guard。
- 前端管理端 JS/TS 镜像：
  - `Product/Distribution/EditPage.js` 落后于 TSX，缺少来源 SKU 查询权限 gate 和 `getSourceProductList` 请求。
  - `UpstreamSystem/index.js` 落后于 TSX，缺少按同步类型的权限过滤、SKU 尺寸/库存同步入口和对应 action refs。

## 已完成

- `AdminSellerController` / `AdminBuyerController` 的账号角色分配写接口改为同时要求：
  - `*:admin:account:role:edit`
  - `*:admin:account:role:query`
  - `*:admin:role:query`
- `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest` 固定上述三权限联合约束。
- `20260604_three_terminal_legacy_sys_user_account_backfill.sql` 新增：
  - `@legacy_seller_expected_signature`
  - `@legacy_buyer_expected_signature`
  - seller/buyer account 与 sys_user 目标行的 `sha2(group_concat(...))` 精确签名校验。
- `20260606_legacy_disable_sys_seller_buyer_roles.sql` 新增 `assert_terminal_owner_roles_ready()`，禁用 legacy `sys_role` 前确认 seller/buyer OWNER 账号已有端内 owner 角色。
- `Product/Distribution/EditPage.js` 由当前 TSX 机械转译同步，补齐来源 SKU 选择和权限 gate。
- `UpstreamSystem/index.js` 由当前 TSX 机械转译同步，补齐按同步类型的权限过滤和 SKU 尺寸/库存同步入口。
- `product-distribution-permission-guard.test.ts` 补充 JS 镜像来源 SKU gate 合同。
- 新增 `upstream-system-permission-guard.test.ts`，并登记到 `three-terminal.manifest.json`；`verify-three-terminal.mjs` 的关键前端测试发现规则同步包含该测试。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,SqlExecutionGuardContractTest" test`：通过，52 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/product-distribution-permission-guard.test.ts tests/upstream-system-permission-guard.test.ts --runInBand`：通过，2 个 suite / 4 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个 guard 通过，React typecheck 通过，8 个 Jest suite / 36 个测试通过，后端 reactor `test-compile` 通过，后端三端合同测试通过。

## 边界与残留

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮按用户要求未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮未发现新的确定 P0。
- P2 继续记录但不阻塞：非 401 redirect 清 token、`getRoutersInfo()` 非 200 缓存空菜单。
