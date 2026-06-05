# 2026-06-05 管理端首次登录动态菜单缓存修复记录

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：管理端首次登录后，左侧动态菜单和动态路由有时需要刷新才稳定显示。

本轮不改卖家/买家业务字段、不改端内权限模型、不复制买家业务模板、不执行 SQL。

## 数据源与影响范围

- 配置确认：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 当前 `spring.profiles.active=druid`。
- MySQL 连接来源：`application-druid.yml` 的 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD` 运行变量。
- Redis 连接来源：`application.yml` 的 `RUOYI_REDIS_HOST`、`RUOYI_REDIS_PORT`、`RUOYI_REDIS_DATABASE`、`RUOYI_REDIS_PASSWORD` 运行变量。
- 本轮未读取或输出 `.env.local` 凭证。
- 本轮未执行 DDL、DML 或人工 SQL。
- 本轮只修改 `react-ui/` 的管理端动态菜单缓存与清理逻辑。

## 已完成

- `react-ui/src/services/session.ts` 新增管理端远程菜单缓存 key：`admin_remote_menu`。
- `setRemoteMenu(...)` 同时维护运行时内存和 `sessionStorage.admin_remote_menu`。
- 页面刷新或首次动态路由 patch 时，`remoteMenu` 可从 `sessionStorage.admin_remote_menu` 恢复。
- `react-ui/src/access.ts` 在清理 admin token 时同步清理 `admin_remote_menu`。
- `react-ui/src/app.tsx` 新增 `clearAdminSession()`，统一清理 admin token 和管理端远程菜单缓存。
- token 过期、无 token、`getInfo` 失败、`getRouters` 失败和登录重定向路径统一使用 `clearAdminSession()`。
- `patchRouteItems(...)` 改为按 path 更新已有 `routes` / `children`，避免动态菜单 patch 重复追加路由。
- 更新 `docs/architecture/reuse-ledger.md`，登记管理端远程菜单缓存和幂等 patch 规则。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/app.tsx src/access.ts src/services/session.ts`：通过，输出 `Checked 2 files in 7ms. No fixes applied.`；当前 Biome 配置只实际检查其中 2 个文件。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：未通过，阻断点为无关的 `src/pages/Product/Attribute/components/CategoryAttributeTemplate.tsx`，错误是 `value: number | undefined` 不能赋给 `{ label: string; value: number }[]`。该文件不是本切片修改范围，本轮不跨切片修复。
- Playwright CLI 低权限买家首次登录：通过。首次从 `/user/login?redirect=%2Fpartner%2Fbuyer` 登录后直接到 `/partner/buyer`，页面标题为 `买家管理 - Ant Design Pro`，表格行数 `1`，`admin_remote_menu` 存在，只有管理端 token 存在，未写入 seller/buyer 端 token。
- Playwright CLI 低权限买家控制台检查：通过，`Errors: 0`、`Warnings: 0`。
- Playwright CLI admin 首次登录：通过。清空会话后从 `/user/login?redirect=%2Fpartner%2Fseller` 登录，首次跳转直接到 `/partner/seller`，页面标题为 `卖家管理 - Ant Design Pro`，表格行数 `3`，左侧菜单和面包屑可见，`admin_remote_menu` 已写入。
- Playwright CLI admin 控制台检查：通过，`Errors: 0`、`Warnings: 0`。
- 截图证据：
  - `output/playwright/buyer-first-login-menu.png`，大小 `51460` bytes。
  - `output/playwright/admin-first-login-menu.png`，大小 `74145` bytes。

## 当前判断

- 管理端首次登录后动态菜单和动态路由现在不再依赖刷新恢复。
- 管理端菜单缓存只跟 admin token 同生命周期；admin token 清理时同步清理 `admin_remote_menu`。
- portal 路由仍排除在管理端 `getUserInfo()`、动态菜单加载和管理端登录态重定向之外。
- 本轮只处理管理端动态菜单时序，不改变卖家/买家低权限、免密代入、账号、菜单、角色或部门业务逻辑。

## 残留问题

- `npm run tsc` 当前被商品属性页面的无关类型错误阻断，需后续单独切片处理。
- `react-ui` 当前仍是管理端验证入口；正式 `seller-ui` / `buyer-ui` 物理拆分仍按后续计划推进。
