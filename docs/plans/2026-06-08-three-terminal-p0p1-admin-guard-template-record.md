# 2026-06-08 三端 P0/P1 快速推进记录：管理端权限 Guard 与同构模板

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：按当时用户规则优先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；现行规则为默认使用 `gpt-5.4`。
- 平台返回额度或可用性限制，失败的 `gpt-5.3-codex-spark` 子 Agent 已关闭。
- 按 fallback 规则启动 6 个 `gpt-5.4` 只读子 Agent，已全部关闭。
- 主 Agent 已合并并复核结论，只采纳确定 P0/P1；P2 不阻塞当前推进。

## 已修复的 P0/P1

- `UpstreamSystem`：
  - `SyncTabs` 按 `integration:upstream:query`、`integration:upstream:inventoryQuery`、`integration:upstream:log` 分别 gate 基础信息、库存和日志请求。
  - `SkuSyncPanel`、`SkuDimensionPanel`、`SkuInventoryPanel` 在缺少对应权限时不发起状态或列表请求。
- `Finance/Currency`：
  - 汇率历史入口绑定 `finance:currency:query`。
  - 同步配置、同步日志、立即同步分别绑定 `finance:currency:syncConfig`、`finance:currency:log`、`finance:currency:sync`。
- `Inventory/Overview`：
  - SKU 仓库展开明细按 `inventory:overview:query` gate，避免无查询权限仍触发详情请求。
- `Product/Distribution`：
  - 商品分销详情入口和查看按钮按 `product:distribution:query` gate。
- `PartnerManagement`：
  - 移除账号弹窗默认密码 `U12346` fallback。
  - 账号创建必须人工填写 5-20 位密码，重置密码传入人工临时密码，不静默恢复默认密码。
  - 部门弹窗拆分列表加载与部门树加载；部门树失败不阻断部门列表。
- SQL guard：
  - `20260604_three_terminal_isolation_migration.sql` 增加 seller/buyer 用户名重复 preflight，唯一索引改为定义校验后重建，避免历史库索引不一致时静默放过。
  - `20260606_admin_partner_role_menu_grant.sql` 增加 64 个管理端 partner 子按钮精确签名校验，授权前验证 `menu_id + parent_id + menu_type + path + component + route_name + perms`。
- 总验证入口：
  - `verify-three-terminal.mjs` 的后端模块清单改为从 reactor 动态派生，不再硬编码少数模块。
  - critical 后端测试类匹配补入 `Admin.*Permission/Route`，避免新增管理端 route/permission 合同只被编译覆盖。

## 合同与台账

- 新增或加强以下合同：
  - `IntegrationAdminRouteContractTest`
  - `FinanceAdminRouteContractTest`
  - `InventoryAdminRouteContractTest`
  - `ProductAdminRouteContractTest`
  - `SqlExecutionGuardContractTest`
  - `AdminAccountPermissionUiContractTest`
- 已更新：
  - `AGENTS.md`：补充快速推进模式规则。
  - `docs/architecture/reuse-ledger.md`：补充 `PartnerManagement` 管理端同构模板复用规则。
  - `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`：追加本轮检查点。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/check-partner-management-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,IntegrationAdminRouteContractTest,FinanceAdminRouteContractTest,InventoryAdminRouteContractTest,ProductAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，48 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=AdminAccountPermissionUiContractTest" test`：通过，1 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 guard、React typecheck、7 个 Jest suite / 35 个测试、后端 reactor `test-compile` 和三端合同测试全部通过。

## 边界与残留

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮按用户要求未做浏览器、截图、DOM 或 UI 细调验收。
- P2 记录但不阻塞：
  - `requestErrorConfig.ts` 对非 401 的 `ErrorShowType.REDIRECT` 也会清 token，后续可独立收口。
  - `getRoutersInfo()` 非 200 时返回并缓存空远程菜单，后续可独立收口。
