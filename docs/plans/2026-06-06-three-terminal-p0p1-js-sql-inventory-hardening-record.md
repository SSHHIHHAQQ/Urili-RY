# 2026-06-06 三端 P0/P1：JS 副本、SQL 护栏与库存隐藏激活收口记录

本记录以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前执行模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 输入与子 Agent

- 用户要求子 Agent 优先使用 `gpt-5.3-codex-spark`，不可用时使用 `gpt-5.4`。
- 本轮实际情况：`gpt-5.3-codex-spark` 不可用，已降级使用 6 个 `gpt-5.4` 子 Agent。
- 6 个有效子 Agent 已全部关闭。
- 采纳的 P0/P1：
  - `routes.js` 缺 `/seller/login`、`/buyer/login`。
  - Partner 菜单编辑 guard 对 path/component/perms 的端隔离不足。
  - Partner 管理 guard 对 JS twin 覆盖不完整。
  - 默认 `npm test` 未串入 `verify:three-terminal`。
  - legacy/cleanup SQL 需要精确 `role_id` 与 expected count guard。
  - 上游库存同步、库存查询、库存 DDL、库存权限和 Quartz job 违反当前“来源仓库库存只占位、不建表、不接同步”的边界。

## 已落地修改

- `react-ui/config/routes.js`：补齐 `/seller/login`、`/buyer/login`。
- `react-ui/scripts/check-portal-token-isolation.mjs`：检查 `routes.ts` 和 `routes.js` 的 portal 路由一致性。
- `react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx`：增加端内菜单 path/component/perms guard。
- `react-ui/src/components/PartnerManagement/PartnerMenuModal.js`：改为显式转发 TSX 源文件，避免 JS/TS 双份实现漂移。
- `react-ui/scripts/check-partner-management-template.mjs`：纳入 Seller/Buyer JS 页面、菜单弹窗 JS、审计弹窗 JS 的合同检查。
- `react-ui/package.json`：`test` 改为先执行 `verify:three-terminal` 再执行 `jest`。
- `RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql`：增加缺失 `sys_user` 与空密码硬断言。
- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`：端内 owner 默认授权收窄到 `role_key='owner'`。
- `RuoYi-Vue/sql/20260606_legacy_disable_sys_seller_buyer_roles.sql`：增加精确 role id 和 expected count guard，并修复 delimiter。
- `RuoYi-Vue/sql/20260606_admin_partner_non_admin_button_grant_cleanup.sql`：增加精确 role id 和 expected delete count guard。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminUpstreamSystemController.java`：禁用未确认的库存同步、库存列表、库存状态入口。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamSystemTask.java`：库存定时任务入口改为 disabled fail-loud。
- `RuoYi-Vue/sql/upstream_system_management_seed.sql`：移除未确认库存 DDL、库存权限 seed。
- `RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql`：改为执行即失败的占位脚本。
- `react-ui/src/pages/UpstreamSystem/components/SyncTabs.tsx`：移除未确认 SKU 库存 tab。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:buyer-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-admin -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 3 changed files`。

## 未执行项与边界

- 未执行远端数据库 DDL/DML。
- 未读写 Redis。
- 未启动浏览器，未做截图、DOM 或 UI 细调验收。
- 未执行 `20260606_admin_partner_non_admin_button_grant_cleanup.sql`；该脚本会删除远端 `sys_role_menu` 权限，必须单独确认 role id 和 expected count 后再执行。
- 上游库存内部 service/mapper/DTO 仍有残留，但当前可触达 HTTP 入口、前端 tab、seed 权限、DDL 和 Quartz job 激活已关闭。是否恢复必须先确认来源仓库库存 schema 与同步落库方案。
- `seller/buyer` 依赖完整 `product` artifact 的架构债未在本轮改造；后续建议拆只读 contract/facade，避免端内轻量切片被商品写侧实现拖住。
