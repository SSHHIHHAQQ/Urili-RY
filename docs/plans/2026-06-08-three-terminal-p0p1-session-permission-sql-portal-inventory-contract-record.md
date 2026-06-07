# 2026-06-08 三端 P0/P1：会话权限、SQL Guard、Portal 改密与库存总览契约记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮继续按快速推进口径执行：只处理 P0/P1，也就是编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 按用户最新规则先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent。
- 平台返回 GPT-5.3 Codex Spark 用量限制，提示需等到 `2026-06-08 01:14/01:15` 后再试；失败 Agent 已关闭。
- 按 fallback 规则启动 6 个 `gpt-5.4` 只读子 Agent，并已全部关闭。
- 采纳的 P1：
  - 管理端“会话查看”与“强制踢出”共用 `*:admin:forceLogout`，无法只读授权。
  - `20260606_legacy_disable_sys_seller_buyer_roles.sql` 只靠 `role_key=seller/buyer + role_id`，目标签名不够强。
  - `20260608_inventory_overview_sku_baseline_refresh.sql` 删除读模型后再重建，缺少原子化保护。
  - portal 改密链路未进入 three-terminal 显式契约。
  - 库存总览 `warehouses/adjust/ledger` 核心交互只被路由前缀级契约覆盖。
- 未采纳为本轮改动：
  - 浏览器运行态、截图、DOM、按钮显隐细调。
  - 子 Agent 报告中未形成确定 P0/P1 的审计建议，仅保留 Markdown 审计报告作为旁证。

## 已完成

- 新增管理端只读会话权限：
  - `seller:admin:session:list`
  - `buyer:admin:session:list`
- 后端管理端 controller：
  - `GET /seller/admin/sellers/{sellerId}/sessions/list`
  - `GET /seller/admin/sellers/{sellerId}/accounts/{accountId}/sessions/list`
  - `GET /buyer/admin/buyers/{buyerId}/sessions/list`
  - `GET /buyer/admin/buyers/{buyerId}/accounts/{accountId}/sessions/list`
  - 以上只读接口改用 `*:admin:session:list`。
  - `DELETE .../sessions` 强退接口继续保留 `*:admin:forceLogout`。
- 前端管理端模板：
  - `PartnerManagementPage` 的主体级“会话”入口改看 `*:admin:session:list`。
  - `PartnerAccountModal` 的账号级“会话”入口改看 `*:admin:session:list`。
  - “强制踢出”仍只看 `*:admin:forceLogout`。
- 管理端菜单与授权 SQL：
  - `seller_buyer_management_seed.sql` 新增 `2256/2257` 菜单位，不复用历史清理过的 `2204/2214`。
  - `20260606_admin_partner_role_menu_grant.sql` 将新只读会话权限授予 admin 角色。
  - `20260606_admin_partner_non_admin_button_grant_cleanup.sql` 将新只读会话权限纳入非 admin 按钮授权清理范围。
- SQL Guard：
  - `20260606_legacy_disable_sys_seller_buyer_roles.sql` 新增 `@legacy_sys_role_cleanup_expected_signature`，执行前按 `role_id/role_key/role_name/status/del_flag` 重新计算精确签名，不匹配直接 `45000`。
  - `20260608_inventory_overview_sku_baseline_refresh.sql` 将读模型清空和 SKU/SPU 读模型重建放入 `start transaction` / `commit` 事务边界。
- 契约测试：
  - 新增 `PortalPasswordChangeContractTest`，固定 seller/buyer `/account/password` 的 `PUT`、terminal 绑定、当前 session 推导、service 调用和前端 service URL/header。
  - 加强 `InventoryAdminRouteContractTest`，固定 `spuList/skuList/warehouses/previewAdjust/confirmAdjust/ledgerList` 六个入口和前端组件 service 绑定。
  - `SellerAdminPermissionContractTest`、`BuyerAdminPermissionContractTest`、`AdminAccountPermissionUiContractTest`、`check-partner-management-template.mjs` 同步拆分会话查看与强退权限。
  - `SqlExecutionGuardContractTest` 固定 legacy role 精确签名和库存读模型事务合同。
  - `PortalPasswordChangeContractTest` 已登记到 `react-ui/tests/three-terminal.manifest.json`。
- 规则沉淀：
  - `AGENTS.md` 新增会话查看/强退权限拆分规则，以及高影响 legacy 清理、读模型全量刷新 SQL 的 fail-closed/原子化规则。
  - `docs/architecture/reuse-ledger.md` 与目标追踪同步本轮结论。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminAccountPermissionUiContractTest,PortalPasswordChangeContractTest,InventoryAdminRouteContractTest,FinanceAdminRouteContractTest,ProductAdminRouteContractTest,SqlExecutionGuardContractTest" test`：通过，52 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-partner-management-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard 通过，React typecheck 通过，前端 7 个 Jest suite / 34 个测试通过，后端 reactor `test-compile` 通过，后端三端合同测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；只有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" ...`：无输出，本轮触碰文件未发现尾随空白。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 15 个变更文件，Added 3、Modified 12，共 832 个节点。

## 边界

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 残留

- 运行态 403/200 对照、按钮显隐和浏览器菜单缓存验证仍不是当前快速推进阻塞项。
- 商品配对投影下沉到 integration facade、库存事实源与聚合口径仍按后续设计继续推进。
- 库存总览 SKU 基线刷新 SQL 已增强事务保护，但仍未执行；执行前仍需按 SQL 执行计划确认目标环境和 token。
