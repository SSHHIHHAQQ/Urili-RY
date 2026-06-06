# 2026-06-06 三端 P0/P1 路由、SQL 与库存入口收口执行记录

## 参考方向

- 当前改造继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。
- 执行模式遵守本轮快速推进要求：只处理 P0/P1，也就是编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。
- 管理端继续保留若依 `sys_*` 后台体系；卖家端、买家端账号、密码、角色、菜单、权限、部门、登录日志、操作日志和会话继续按端独立。

## 子 Agent 执行情况

- 已按要求优先尝试 `gpt-5.3-codex-spark`，但本轮 6 个 GPT-5.3 子 Agent 均因平台额度限制不可用。
- 已降级使用 6 个 `gpt-5.4` 子 Agent，覆盖后端 portal、React、SQL、seller/buyer 模块、session/log、验证脚本等切片；本轮所有子 Agent 均已关闭。
- 已采纳的 P0/P1：动态管理端路由 fail-closed、Portal Home 权限 gating、SQL legacy 脏占位风险、端内权限 seed 误授权、未确认库存入口暴露、默认 `npm test` 不可用。
- 未纳入本轮阻塞的项记录为 P2：portal service 层重复校验抽取、ticketId 审计串联、seller/buyer 重复代码治理、product/integration 更细模块级行为测试、前端 bundle 级检查。

## 新增问题

- 动态后端菜单路由只写 `authority`，缺少前端 fail-closed route guard，低权限用户可进入页面壳层后再依赖后端拒绝。
- Portal Home 已拿到端内 permissions，但账户、部门、角色、商品 schema、分销商品模块按 terminal 渲染，未按权限点收口。
- `20260604_three_terminal_isolation_migration.sql` 删除 `seller_account.user_id` / `buyer_account.user_id` 前缺少 legacy 绑定硬校验。
- `20260604_portal_direct_login_ticket.sql` 曾通过 `legacy-*`、空字符串、0 等占位值自愈历史脏行，失败前会污染远端数据。
- 端内 account/dept/role 增量权限 seed 对所有启用角色默认授权，可能扩大端内敏感列表权限。
- 上游库存同步/库存列表/库存状态接口、Tab、旧 Quartz target 与 SQL seed 与当前“来源仓库库存只占位、未确认 schema”边界冲突。
- `npm test -- --runInBand` 先后暴露出 Jest 多配置、`@umijs/max/test` ESM 路径、无测试用例失败退出三个默认验证入口问题。

## 已修复问题

- `react-ui/src/services/session.ts` 新增 `RemoteMenuRouteGuard`，动态菜单路由按 `menuItem.authority` fail closed；`session.js` 改为转发 TS 源码。
- `react-ui/src/pages/Portal/Home/index.tsx` 按 `${terminal}:account:list`、`${terminal}:dept:list`、`${terminal}:role:list`、`${terminal}:product:*` 权限点决定是否请求和渲染对应模块；`index.js` 转发 TSX 源码。
- `20260604_three_terminal_isolation_migration.sql` 增加 `assert_no_legacy_account_user_bindings`，删除 legacy `user_id` 前硬校验。
- `20260604_portal_direct_login_ticket.sql` 移除非法占位自愈写入，保留非法 legacy 行 fail-loud 断言；`PortalDirectLoginTicketSqlContractTest` 同步改为禁止脏占位契约。
- `20260604_portal_account_list_permission_seed.sql` 和 `20260604_portal_dept_role_list_permission_seed.sql` 默认授权收敛到 `role_key='owner'`。
- 多个独立 admin 菜单旧 seed 改为 fail-loud 废弃脚本，避免裸 fixed `menu_id` upsert 误覆盖；后续应使用综合 seed 与带 guard 的授权脚本。
- `AdminUpstreamSystemController` 删除未确认的库存同步、库存列表、库存状态 HTTP 映射；`AdminSourceWarehouseStockController` 降成不可路由占位类。
- `SyncTabs.tsx` 删除未确认的 SKU 库存 Tab；`upstreamSystem.ts` 删除库存请求函数；`sourceWarehouseStock.ts` 只返回空占位结果。
- `upstream_system_management_seed.sql` 移除未确认库存快照表 DDL、库存状态表 DDL 和库存权限 seed；`20260606_upstream_inventory_dimension_sync.sql` 改为清理/禁用旧库存权限和旧 Quartz target。
- `business_menu_seed.sql` 的「来源仓库库存」改为 `Common/PlannedPage/index` 占位，不指向真实库存页面。
- `react-ui/package.json` 的 `test` / `jest` 显式使用 `jest.config.ts --passWithNoTests`；`jest.config.ts` / `jest.config.js` 改用 `@umijs/max/test.js`。
- `docs/architecture/reuse-ledger.md` 将来源仓库库存从“只读视图”改为“占位”，记录恢复前必须先确认 schema 和同步落库方案。

## 残留问题

- 来源仓库库存的 service、mapper、DTO 内部实现仍有残留，但当前无 HTTP、Tab、菜单真实页面、权限 seed 或激活 job 入口；后续恢复必须先做方案确认。
- `20260606_admin_partner_non_admin_button_grant_cleanup.sql` 仍未执行，非 admin 角色历史按钮授权清理需要单独确认 role_id 与 expected count。
- seller/buyer portal auth、permission、product facade 仍有重复代码，当前按已确认“卖家模板再复制买家”的方式保留，后续可抽公共 support。
- product/integration 行为测试仍不足，本轮只处理最小 P0/P1，不扩展模块级业务测试。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:buyer-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-admin -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm test -- --runInBand`：通过，Jest 当前无前端用例并以 code 0 退出。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 工作区换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 14 changed files`。
- 静态搜索确认：`AdminUpstreamSystemController`、`react-ui/src`、`react-ui/config` 中不再存在库存同步/库存列表/库存状态可触达入口；上游库存权限字符串仅保留在清理脚本中。

## 未验证原因

- 按用户要求，本轮未执行浏览器运行态验收、截图、DOM 检测或 UI 细调。
- 本轮未启动后端服务。
- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未执行 `20260606_admin_partner_non_admin_button_grant_cleanup.sql`，因为它会删除远程 `sys_role_menu` 权限记录，必须单独确认。

## 权限检查结果

- 管理端动态路由已增加前端 fail-closed wrapper，但后端权限仍是最终强制边界。
- Portal Home 已按端内 permission 决定是否请求和渲染模块，避免只按 terminal 展示。
- 端内 account/dept/role 增量权限 seed 已收敛到 owner 默认授权。
- 上游库存相关权限不再通过 seed 激活；旧权限清理脚本只做禁用/清理。

## 字典/选项复用检查结果

- 本轮未新增字典和业务选项。
- 来源仓库库存仍记录为占位，不恢复库存口径、同步状态或配对状态选项使用。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，将来源仓库库存登记为占位而非已开放只读视图。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .` 已执行并通过，输出 `Synced 14 changed files`。

## 大文件合理性判断结果

- `Portal/Home/index.tsx` 和 `SyncTabs.tsx` 本轮只做 P0/P1 精准收口，未引入新的大文件拆分。
- `PartnerManagementPage` 仍是既有大模板文件，本轮未继续扩大其职责。

## 重复代码检查结果

- `session.js`、`Portal/Home/index.js`、`SyncTabs.js` 等 JS twin 采用转发 TS/TSX 的方式降低漂移风险。
- seller/buyer 镜像实现当前按已确认模板化复制策略保留；未做无方案抽象。
