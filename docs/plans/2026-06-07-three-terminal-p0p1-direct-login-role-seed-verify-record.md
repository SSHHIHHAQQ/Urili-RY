# 三端 P0/P1 免密一次性、角色绑定与 Seed Guard 收口记录

记录时间：2026-06-07 00:21 +08:00

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 本轮范围

继续按三端独立方向推进：管理端保留若依 `sys_*` 后台体系，卖家端和买家端继续使用独立账号、角色、菜单、部门、日志和会话体系。

本轮只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调；不执行远程 MySQL/Redis DDL/DML。

## 子 Agent 执行情况

- 历史记录（已过期口径）：用户最新指定：子 Agent 优先使用 GPT-5.3 Codex；不可用时使用 `gpt-5.4`。
- 本轮先尝试 GPT-5.3 Codex，平台返回用量/可用性限制后关闭失败 Agent。
- 实际降级使用 6 个 `gpt-5.4` 子 Agent 并行审计后全部关闭。
- 采纳并修复的 P1：免密 token 失败尝试未消费、停用角色仍可绑定、角色编辑缺少菜单树查询权限、账号弹窗部门树查询权限、SQL seed guard/收敛和验证入口覆盖。

## 已完成

### 免密 token 一次性语义

- `PortalDirectLoginSupport.consumeToken(...)` 改为拿到 DB ticket 和 Redis payload 后，首次提交无论业务校验成功还是失败，都会删除 Redis payload 并尝试标记 DB ticket 为 `USED`。
- 黑名单 IP、目标 payload 不匹配、seller/buyer 业务 validator 抛出 `ServiceException` 等失败提交不再保留可重放 token。
- Redis payload 缺失时仍将 DB ticket 标记为 `EXPIRED`，不再保持可疑 `ISSUED` 状态。
- `PortalDirectLoginSupportTest` 同步固定失败提交即消费的行为。

### 端内角色绑定状态闭环

- `SellerPortalPermissionMapper.xml` / `BuyerPortalPermissionMapper.xml`：
  - `select*AccountRoleIds` 增加 `role.status = '0'`，停用角色不再作为已选中角色回显。
  - `count*RolesByIds` 增加 `status = '0'`，账号绑定角色时只能绑定启用角色。
- `SellerPortalPermissionServiceImplTest` / `BuyerPortalPermissionServiceImplTest` 增加停用角色绑定拒绝测试。

### 管理端同构模板权限闭环

- `PartnerAccountModal` 在缺少 `dept:query` 时不再调用部门树接口。
- `PartnerRoleModal` 的角色编辑入口从 `role:edit + role:query` 收紧为 `role:edit + role:query + menu:query`，避免打开必然缺菜单树数据的半残表单。
- `check-partner-management-template.mjs` 同步固定 `.tsx` / `.js` sidecar 的权限闭环。

### SQL Seed Guard 与收敛

- `top_menu_seed.sql` 增加 `@confirm_top_menu_seed` 和 `APPLY_TOP_MENU_SEED` 运行时确认 guard，并纳入 `SqlExecutionGuardContractTest`。
- `seller_buyer_management_seed.sql` 对 `portal.seller.web.url` / `portal.buyer.web.url` 增加先 `update` 再缺失插入的收敛逻辑，已有错误值会被纠正为当前验证地址。
- `SqlExecutionGuardContractTest` 增加 seller/buyer direct-login URL seed 收敛契约。

### 验证入口闭环

- `verify-three-terminal.mjs` 将 `product` 模块纳入 source root、surefire report 模块和 Maven `-pl`，确保 `ProductPortalSchemaServiceImplTest` 不再只出现在名单里却不执行。
- `verify-three-terminal.mjs` 增加前端测试清单守卫，发现 `react-ui/tests/*.test.*` 未纳入三端验证时直接失败。

## 未本轮修改的审计项

- 管理端非 admin `sys_role` 保留卖家/买家管理页级授权：当前按后台授权模型保留，不视为卖家/买家端账号混用；卖家端、买家端内权限仍不得复用 `sys_*`。
- 免密失败日志缺少 acting admin/ticket/reason 完整字段：涉及登录日志表和审计字段扩展，记录为后续审计增强，不在本轮无 DDL 模式下改表。
- product/inventory 页面直接消费 integration service：存在模块 owner/facade 债务，需先确认 owner，是收口到 integration 还是补 product/inventory facade；本轮不做临时 facade。
- buyer 充值和余额占位：用户此前确认先占位，本轮不移除占位。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalDirectLoginSupportTest,SqlExecutionGuardContractTest" test`：通过，`18` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller "-Dtest=SellerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`8` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer "-Dtest=BuyerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`8` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\check-partner-management-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；最终输出 `three-terminal verification passed.`，前端 Jest `9` 个测试通过，后端三端契约整体通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；首次同步返回 `Synced 23 changed files`，`Modified: 23 - 985 nodes`。

## 边界说明

- 本轮未执行 SQL，未写远程 MySQL。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
