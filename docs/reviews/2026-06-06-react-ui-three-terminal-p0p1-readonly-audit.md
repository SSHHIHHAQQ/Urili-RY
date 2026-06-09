# 2026-06-06 react-ui 三端独立前端 P0/P1 只读审计

> 2026-06-09 记录层 P1 修正：本文中的 2 条前端 P1 已由后续 `config/routes.js` re-export、`PartnerMenuModal` fail-closed 校验、`check-partner-management-template.mjs` guard 和三端验证收口。本文保留历史证据，不再代表当前开放 P1。

## 审计范围

- 范围：`react-ui/src`、`react-ui/config/routes.ts`、`react-ui/scripts`
- 目标：前端三端独立 P0/P1 只读审计
- 明确不做：浏览器验证、代码修改、后端/SQL 改动

## 一句话结论

- **P0：0 项**
- **P1：2 项（历史发现，已关闭）**
- 当前 `seller/buyer/admin` token 分流、portal 401/退出/direct-login 的主链路静态代码已基本对齐三端独立；本文记录的 **`config/routes.js` 陈旧副本** 和 **菜单编辑端隔离 guard 仍是黑名单式校验** 是历史风险，已在后续检查点关闭。

## 历史新增问题（已关闭）

### P1-1 `config/routes.js` 当时已落后于 `config/routes.ts`，可能把 portal 登录入口带回旧路由集

- 当时风险描述：
  - `react-ui/config/routes.ts` 已声明 `/seller/login` 和 `/buyer/login`。
  - 同目录的 `react-ui/config/routes.js` 仍然缺少这两个 portal 登录入口。
  - 当前静态证据显示 TS 配置链路优先命中 `routes.ts`，所以这不是现状 P0；但只要有人回退到 `config.js`、脚本误扫 JS 配置、或者后续入口调整，这个陈旧副本就会直接把 portal 登录入口带回旧集合。
  - 现有 portal guard 也**没有覆盖 routes 配置文件**，这类漂移不会被 `npm run guard:portal-token` 捕获。
- 证据：
  - `react-ui/config/routes.ts:18-27` 定义了 `/seller/login`、`/buyer/login`
  - `react-ui/config/routes.js:12-31` 只有 `/seller/direct-login`、`/buyer/direct-login`、`/seller/portal`、`/buyer/portal`，缺少 `/seller/login`、`/buyer/login`
  - `react-ui/scripts/check-portal-token-isolation.mjs:20-33` 只校验 `portal/session`、`app`、`requestErrorConfig`、`proxy`、`portalPaths` 等文件，未纳入 `config/routes.ts` / `config/routes.js`
- 影响判断：
  - 当前更像“**陈旧副本 + guard 覆盖缺口**”，不是已证实的线上断路，所以定为 **P1**。

### P1-2 菜单编辑端隔离 guard 当时不够，只拦“对端”，没有拦“admin/common/shared”

- 当时风险描述：
  - `PartnerMenuModal` 的前端校验只做了三件事：
    1. 路由 path 不能显式指向对端前缀
    2. component 不能显式指向对端页面根目录
    3. perms 只要求以 `seller:` 或 `buyer:` 开头
  - 这意味着下面这些仍然能从前端表单层面通过：
    - path 指到 `/system/...`、`/monitor/...`、`/tool/...`
    - component 指到 `System/...`、`Common/...`、其他共享/管理端页面
    - perms 只要前缀是 `seller:` / `buyer:`，但并不校验是否落在允许的端内权限目录
  - 如果后端没有更强的约束，这会让卖家/买家菜单配置仍可把 admin/common 页挂进端内菜单树，前端 guard 本身不足以支撑“端隔离”。
- 证据：
  - `react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx:190-213`
    - `validateMenuPathForTerminal(...)` 仅拦对端前缀
    - `validateMenuComponentForTerminal(...)` 仅拦对端根目录
    - `validateMenuPermsForTerminal(...)` 仅校验 `${moduleKey}:` 前缀
  - `react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx:491-520`
    - 上述三个 validator 被直接挂到 `path` / `component` / `perms` 字段，没有更强 allowlist
  - `react-ui/scripts/check-partner-management-template.mjs:72-229`
    - 现有模板 guard 重点是 seller/buyer service URL、必需 service、跨端 service path 混用
    - 脚本没有对 menu path/component/perms 做更细的 allowlist 校验
- 影响判断：
  - 这是 **菜单配置层的隔离不足**，不是 token 串用或 401 立刻跳错登录页，因此定为 **P1**。

## 已修复问题

### 当前代码状态下，portal 401 / 退出 / direct-login 主链路没有继续跳回 `/user/login`

- 静态证据：
  - `react-ui/src/app.tsx:48-57`：按 `requestUrl` 推导 terminal，401 时跳 `getPortalLoginPath(portalTerminal)`
  - `react-ui/src/requestErrorConfig.ts:23-32`：同样按 terminal 清理对应 token 并跳 terminal login
  - `react-ui/src/utils/portalPaths.ts:5-29`：统一定义 `/seller/login`、`/buyer/login`、`isPortalRoute`
  - `react-ui/src/utils/portalRequest.ts:29-44`：识别 portal API 时排除 `/api/seller/admin`、`/api/buyer/admin`
  - `react-ui/src/pages/Portal/Home/index.tsx:162-166`、`209-232`：加载失败、缺 token、logout 都跳 `PORTAL_META[terminal].loginPath`
  - `react-ui/src/pages/Portal/DirectLogin/index.tsx:144-145`：失败回跳 terminal portal login
- 命令验证：
  - `npm run guard:portal-token` 通过

### 当前 seller/buyer service 路径、权限 key、字段主干未发现 P0/P1 缺失

- 静态证据：
  - `react-ui/src/pages/Seller/index.tsx:46-115`：seller 模块 `moduleKey`、字段映射、service 注入完整
  - `react-ui/src/pages/Buyer/index.tsx:46-116`：buyer 模块 `moduleKey`、字段映射、service 注入完整
  - `react-ui/src/services/seller/seller.ts:152-197`、`309-347`：seller 菜单、角色、direct-login、audit 路径齐全
  - `react-ui/src/services/buyer/buyer.ts:107-153`、`309-347`：buyer 菜单、角色、direct-login、audit 路径齐全
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:538-549`、`951-1080`：页面按钮权限与 `permPrefix = ${moduleKey}:admin` 一致
- 命令验证：
  - `npm run guard:partner-management` 通过
  - `npm run guard:seller-portal-product` 通过
  - `npm run guard:buyer-portal-product` 通过

## 历史残留问题（已关闭）

- `react-ui/config/routes.js` 当时是陈旧副本，且当时 guard 不覆盖它；后续已改为 re-export 并由 `remote-menu-route-guard.test.ts` / `admin-auth-sidecar-contract.test.ts` 固定。
- `PartnerMenuModal.tsx` 当时是黑名单式隔离，不是 allowlist 式隔离；后续已补 fail-closed 校验和 `check-partner-management-template.mjs` guard，当前不再作为开放 P1。

## 验证命令

```powershell
cd E:\Urili-Ruoyi
rg -n "seller|buyer|admin|portal|token|direct-login|401|/user/login" react-ui/src react-ui/config/routes.ts react-ui/scripts

cd E:\Urili-Ruoyi\react-ui
npm run guard:portal-token
npm run guard:partner-management
npm run guard:seller-portal-product
npm run guard:buyer-portal-product

cd E:\Urili-Ruoyi
git diff --no-index -- react-ui/config/routes.ts react-ui/config/routes.js
git diff --no-index -- react-ui/src/app.tsx react-ui/src/app.js
git diff --no-index -- react-ui/src/pages/Portal/Home/index.tsx react-ui/src/pages/Portal/Home/index.js
git diff --no-index -- react-ui/src/pages/Portal/DirectLogin/index.tsx react-ui/src/pages/Portal/DirectLogin/index.js
```

另做了本地模块解析确认，结果为：

```text
react-ui/config/config.ts :: ./routes => E:/Urili-Ruoyi/react-ui/config/routes.ts
react-ui/src/app.tsx :: ./requestErrorConfig => E:/Urili-Ruoyi/react-ui/src/requestErrorConfig.ts
react-ui/src/pages/Portal/DirectLogin/index.tsx :: ../terminal => E:/Urili-Ruoyi/react-ui/src/pages/Portal/terminal.ts
react-ui/src/pages/Portal/Home/index.tsx :: ../terminal => E:/Urili-Ruoyi/react-ui/src/pages/Portal/terminal.ts
```

## 未验证原因

- 按要求**未做浏览器验证**。
- 本次未跑 `max build` / `max dev`，因此“Umi 路由组件在最终构建产物中是否一定优先吃 `index.tsx` 而不是 `index.js`”没有做构建产物级确认；当前判断基于：
  - TS 配置入口解析结果
  - `git diff --no-index` 对双份文件的静态对比
  - 现有 guard 脚本结果

## 权限检查结果

- `seller/buyer` 管理页统一通过 `permPrefix = ${config.moduleKey}:admin` 组装权限，主按钮、更多操作、菜单入口都走 `access.hasPerms(...)`
  - 证据：`react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:538-549`、`951-1080`
- `PartnerMenuModal` 的编辑/删除/新增按钮也走 `${permPrefix}:menu:*`
  - 证据：`react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx:227-245`、`393-442`
- 权限主干一致，但 **菜单表单 guard 本身不足**，见 P1-2。

## 字典/选项复用检查结果

- 当前前端仍在复用统一字典/选项能力，没有发现 P0/P1 级别的 seller/buyer 分叉问题：
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:37-40` 引入 `getDictSelectOption`、`getDictValueEnum`、`getPersistedProTableSearch`、`SEARCHABLE_SELECT_PROPS`
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:555-562` 统一拉取 `sys_normal_disable`、`subject_type`、`country_region`、`config.levelDictType`
  - `react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx:20`、`528-531` 复用统一搜索型选择器

## 复用台账检查结果

- 本次是窄范围只读审计，未展开 `docs/architecture/reuse-ledger.md` 的全量复核。
- 就本次两项 P1 看，问题核心分别是：
  - TS/JS 双份副本漂移
  - 菜单端隔离 guard 设计偏弱
- 这两项都不是“是否已登记复用台账”能直接拦住的问题，因此本次不把复用台账缺失单独升为 P0/P1。

## CodeGraph 更新结果

- 本次未修改代码，**未执行 `codegraph sync .`**。
- 原因：只读审计，不产生索引应跟随的代码变更。

## 大文件合理性判断结果

- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx` 当前 **1214 行**
- `react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx` 当前 **504 行**
- 结论：
  - 两个文件都已超过 AGENTS 里的自检阈值。
  - 它们不是本次新增问题，但确实提升了“三端隔离回归”的维护风险，尤其 `PartnerManagementPage.tsx` 同时承载列表、表单、账号、部门、角色、菜单、审计、会话、direct-login 入口编排。
  - 本次只读审计不建议顺手拆分；后续若继续在三端隔离方向迭代，优先考虑把菜单/审计/direct-login 相关编排再下沉。

## 重复代码检查结果

- `app.tsx/app.js`、`Portal/Home/index.tsx/index.js`、`Portal/DirectLogin/index.tsx/index.js` 当前更像“TS 源 + JS 派生副本”，没有发现本次审计范围内仍然明显分叉的 portal token/401 旧逻辑。
- **当时真正分叉的已确认点是 `config/routes.ts` 与 `config/routes.js`**：
  - `routes.ts` 有 `/seller/login`、`/buyer/login`
  - `routes.js` 没有
- 因此“TS 与 JS 双份文件是否会造成实际构建用旧逻辑”的当前结论是：
  - **未找到主链路已经吃到旧 JS 的直接证据**
  - **但 `routes.js` 这份陈旧副本当时足以构成 P1 回归源；后续已关闭**
