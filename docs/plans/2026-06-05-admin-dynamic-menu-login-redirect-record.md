# 2026-06-05 管理端动态菜单登录兜底执行记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，本切片只处理一类问题：管理端动态菜单页面在未登录直达时不应落到静态 404，而应跳转到登录页并保留原始目标地址。

## 范围

- 本切片只改 `react-ui` 管理端路由登录兜底。
- 不改后端。
- 不执行 SQL。
- 不改菜单 seed、权限 seed、卖家/买家业务字段或页面模板。
- 不复制买家，也不调整三端账号权限模型。

## 已完成

- `react-ui/src/app.tsx` 新增统一 `redirectToLogin()`，登录跳转统一保留当前 `pathname + search + hash` 作为 `redirect`。
- `getInitialState().fetchUserInfo()` 获取管理端用户失败时，清理管理端 token 后调用统一登录跳转。
- `layout.onPageChange()` 在非 portal、非登录页且无当前用户时，调用统一登录跳转。
- `onRouteChange()` 在非 portal、非登录页且没有管理端 token 时，先跳登录，不再继续等待动态菜单注入后落 404。
- `render()` 在非 portal 且没有管理端 token 时，先跳登录再渲染，避免首次直达动态菜单页面时静态路由判定成 404。
- `react-ui/src/services/session.ts` 在动态菜单 patch 时增加 `proLayout` 空保护，避免特殊路由树下空对象继续 patch。

## 数据源确认

- 读取 `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`：当前激活 profile 为 `druid`。
- 读取 `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`：MySQL 连接读取 `RUOYI_DB_*` 运行变量。
- 读取 `application.yml`：Redis 连接读取 `RUOYI_REDIS_*` 运行变量。
- 本切片未读取、未输出 `.env.local` 明文凭证。
- 本切片未执行 DDL/DML。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/app.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/services/session.ts`：未处理；该路径被当前 Biome 配置忽略，改由 `npm run tsc` 覆盖类型检查。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- Playwright / Chrome 验收未登录直达 `/partner/seller`：通过，最终地址为 `http://127.0.0.1:8001/user/login?redirect=%2Fpartner%2Fseller`，`redirect=/partner/seller`，页面不是 404。
- Playwright / Chrome 验收管理端登录后直达 `/partner/seller`：通过，页面标题为 `卖家管理 - Ant Design Pro`，页面不是 404，页面包含卖家管理字段，`bodyOverflowX=false`，console error 为 0。

## 当前判断

- 管理端动态菜单页面的未登录直达兜底已修复，不再需要刷新后才能从登录态进入目标动态菜单。
- portal 路由继续排除在管理端登录跳转和动态菜单加载之外，不影响 `/seller/direct-login`、`/buyer/direct-login`、`/seller/portal`、`/buyer/portal`。
- 后续卖家标准模板、买家复制、端内账号/角色/菜单等工作应继续另起切片，不混入本路由兜底切片。

## 未验证原因

- 本切片未重启后端，因为未改后端代码。
- 本切片未执行数据库验证，因为未改数据库、菜单 seed 或权限 seed。

## CodeGraph

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 2 changed files`。
