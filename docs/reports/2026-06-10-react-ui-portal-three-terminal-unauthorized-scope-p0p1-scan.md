# React UI 三端 Portal/请求/路由/Js 镜像 只读复核（P0/P1）

日期：2026-06-10
范围：`react-ui` 仅扫描，不修改任何业务代码

## 结论

- **P0：无可复现阻断问题**
- **P1：无明确阻断问题**

## 核验项与证据

### 1) `/api/seller/**` `/api/buyer/**` 401 仅清当前端 token，admin 401 走 admin

- `react-ui/src/utils/portalRequest.ts` 对 `/api/seller`/`/api/buyer` 做终端分类，并显式排除 `/api/seller/admin/*`、`/api/buyer/admin/*`（L29-L45）。
- `react-ui/src/requestErrorConfig.ts` 与 `react-ui/src/app.tsx` 均以终端分类结果为准，分类为 `seller/buyer` 时只执行 `clearTerminalSessionToken`，并跳对应 `/seller|buyer/login`；分类缺失时走 `clearSessionToken`（admin）重定向到 `/user/login`（L33-L45, L73-L85）。
- 现有测试 `react-ui/tests/portal-unauthorized-redirect.test.ts` 覆盖：
  - seller/buyer portal 401 清对应端 token；
  - `admin` 前缀路径 `/api/seller/admin/*` `/api/buyer/admin/*` 仍走 admin 分支（L233-L280）。

### 2) `401` 白名单（direct-login 不清 token/不跳转）

- `isPortalDirectLoginApiUrl` 仅在 `/api/seller/direct-login`、`/api/buyer/direct-login` 返回 true（`portalRequest.ts` L47-L50）。
- `handleUnauthorized*` 在 direct-login 场景直接 `return`（`requestErrorConfig.ts` L36-L40，`app.tsx` L76-L81）。
- 测试断言了 direct-login 401 不清 token 且不跳转（`portal-unauthorized-redirect.test.ts` L127-L141、L163-L179）。

### 3) 重定向白名单

- `react-ui/src/utils/portalPaths.ts` 将允许路径固定为：
  - `/{terminal}/login`
  - `/{terminal}/direct-login`
  - `/{terminal}/portal*`（L23-L28，L44-L45）
- `Portal/Login` 使用 `resolveRedirect` 时仅保留上述白名单内的 redirect，其他场景回退到 `homePath`（`Login/index.tsx` L38-L50）。

### 4) `direct-login` postMessage 成功回传

- 消费端 `react-ui/src/pages/Portal/DirectLogin/index.tsx` 在调用 portal `directLogin` 成功后发送 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 的 `success`（L64-L88）。
- 反向桥接模块 `react-ui/src/utils/portalDirectLoginMessage.ts` 定义 ready/token/result 消息体与匹配逻辑，`openPortalDirectLoginWindow` 发送 token，并等待匹配 result 才 resolve（L71-L80、L98-L175）。
- `react-ui/tests/portal-direct-login-message.test.ts` 覆盖 “READY->token->success result”的成功链路和 timeout/失败链路（L30-L113、L152-L208）。

### 5) `getRouters` authority 为空的 fail-closed

- `react-ui/src/services/session.ts` 的 `toRouteAuthority`：`perms` 空时产出 `[]`（L105-L108）。
- `RemoteMenuRouteGuard` `normalizeAuthority` 后在 `permissions.length > 0` 之前拒绝渲染，空 authority 直接 403（`wrappers/RemoteMenuRouteGuard.tsx` L95-L127）。
- 测试覆盖：空 perms 映射为 `[]` 后 403（`tests/getrouters-authority-contract.test.ts` L19-L53）。

### 6) `.js mirror` 纯重导出

- 已扫描到关键镜像文件均为 re-export：
  - `src/access.js`、`src/requestErrorConfig.js`、`src/app.js`、`src/utils/portalRequest.js`、`src/utils/portalPaths.js`、`src/wrappers/RemoteMenuRouteGuard.js`、`src/services/portal/session.js`、`src/utils/portalDirectLoginMessage.js`、`config/routes.js`、`config/proxy.js`（各文件仅 1~2 行 re-export）。

### 7) Seller/Buyer portal 入口与 wrapper

- `config/routes.ts` 中 `/seller`、`/buyer` 显式绑定 `wrappers: ['@/wrappers/RemoteMenuRouteGuard']` 与各自权限；
- `/seller/login` `/buyer/login` `/seller/direct-login` `/buyer/direct-login` `/seller/portal` `/buyer/portal` 为 `layout:false` 的门户入口，未绑定 wrapper（设计上采用页面内 token 校验）；
- 测试文件 `tests/remote-menu-route-guard.test.ts` 与 `tests/static-route-authority-contract.test.ts` 同时核验上述静态路由与 wrapper 约束。

## `request/response` 失败回传一致性

- `request` 的 `responseInterceptors[0]` 在接收到 code=401 时也走 `handleUnauthorizedResponse` 并 `reject`（`app.tsx` L352-L357），从而与页面入口、redirect 一致。

## 最小修法建议（范围内）

1. 本次扫描范围内未发现 P0/P1 阻断项，**不建议改业务代码**。
2. 可选收敛（非阻塞）：
   - 若希望门户入口 `/seller/portal` `/buyer/portal` 与其他受限静态路由一致，可在 wrapper 层加一层“纯登录态检测 wrapper”（只校验是否有终端 token，不引入额外权限），减少首屏闪烁；与当前逻辑无冲突。
