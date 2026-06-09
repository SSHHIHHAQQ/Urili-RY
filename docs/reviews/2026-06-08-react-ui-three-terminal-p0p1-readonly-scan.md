# react-ui 三端隔离 P0/P1 只读扫描

> 历史记录（已过期口径）：本文记录的是当时只读扫描发现的候选 P1。后续检查点已收口 `getRoutersInfo()` 非 200 抛错、非 401 REDIRECT 不清 token、菜单拉取异常不无条件强退等问题；当前状态以 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 较新检查点和现行代码为准，不要把本文 P1 作为现存阻塞项。

日期：2026-06-08
范围：`E:\Urili-Ruoyi\react-ui`
方式：只读扫描，不改文件，不做浏览器/UI
基线：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 的“快速推进口径”

## 结论

本轮命中 3 个建议修复的 P1 问题，均在你点名的链路里：

1. `getRoutersInfo()` 会把非 200 结果吞成 `[]`，随后被持久化为空远程菜单缓存。
2. `requestErrorConfig.ts` 的 `ErrorShowType.REDIRECT` 会在非 401 场景下清 token 并跳登录。
3. `onRouteChange` / `render` 在 `/api/getRouters` 任意异常时都会清 admin 会话，不区分 401 和普通失败。

同时，本轮未发现以下目标问题：

- 响应体 401 处理后不 reject
- admin/portal 401 串端清 token
- 本轮目标文件中的 JS/TS 镜像不一致

## P1 发现

### 1. `getRoutersInfo()` 会把非 200 菜单响应缓存成空菜单

- 文件/方法：
  - `react-ui/src/services/session.ts` `getRoutersInfo`
  - `react-ui/src/app.tsx` `onRouteChange` / `render`
  - `react-ui/src/pages/User/Login/index.tsx` `handleSubmit`
- 证据：
  - `react-ui/src/services/session.ts:308-315`
    - `res.code === 200` 才转换菜单；否则直接 `return []`
  - `react-ui/src/app.tsx:226-228`
    - `const menus = getRemoteMenu(); if (menus !== null || location.pathname === PageEnum.LOGIN) return;`
    - 这里只判断 `null`，`[]` 会被当作“已有缓存”
  - `react-ui/src/app.tsx:232-233`
    - `const routers = await getRoutersInfo(); setRemoteMenu(routers);`
  - `react-ui/src/app.tsx:264-265`
    - `getRoutersInfo().then(res => { setRemoteMenu(res); })`
  - `react-ui/src/pages/User/Login/index.tsx:151-154`
    - 登录成功后立即 `const routers = await getRoutersInfo(); setRemoteMenu(routers);`
- 风险：
  - 只要 `/api/getRouters` 返回业务非 200 且没有抛异常，就会把 `[]` 写入 `sessionStorage`
  - 后续 `onRouteChange` 因为读到的不是 `null`，不会再重拉菜单
  - 表现会是“登录成功但菜单为空/路由不再恢复”，属于快速推进口径里的 P1
- 是否建议修复：是
- 修复方向：
  - `getRoutersInfo()` 对非 200 不要返回 `[]`，应抛错或返回 `null`
  - `setRemoteMenu(...)` 前只允许写入有效菜单数组
  - `onRouteChange` 的重试判断不要把空数组当成成功缓存

### 2. `requestErrorConfig.ts` 存在非 401 清 token 路径

- 文件/方法：
  - `react-ui/src/requestErrorConfig.ts` `errorConfig.errorHandler`
- 证据：
  - `react-ui/src/requestErrorConfig.ts:88-111`
    - 先判断 `if (isUnauthorizedCode(errorCode)) { handleUnauthorized(requestUrl); throw error; }`
    - 但后面的 `case ErrorShowType.REDIRECT:` 仍然无条件执行 `handleUnauthorized(requestUrl);`
  - `react-ui/src/requestErrorConfig.js:73-99`
    - JS 镜像同样保留了这条路径
- 风险：
  - 只要后端返回 `success=false` 且 `showType=REDIRECT`，即便 `errorCode` 不是 401，也会清 token 并跳登录
  - 这违反了“三端 401 按端隔离、非 401 不误清会话”的目标
- 是否建议修复：是
- 修复方向：
  - `REDIRECT` 分支不要直接复用 `handleUnauthorized`
  - 只有明确 401 时才允许清 token
  - 如果确实需要非 401 页面跳转，应拆成“跳转但不清会话”的独立分支

### 3. `onRouteChange` / `render` 会在远程菜单普通异常时直接清 admin 会话

- 文件/方法：
  - `react-ui/src/app.tsx` `onRouteChange`
  - `react-ui/src/app.tsx` `render`
- 证据：
  - `react-ui/src/app.tsx:231-238`
    - `getRoutersInfo()` 失败后，`catch` 里直接 `clearAdminSession(); redirectToLogin();`
  - `react-ui/src/app.tsx:264-269`
    - `getRoutersInfo()` 失败后，`catch` 里直接 `clearAdminSession();`
- 风险：
  - `/api/getRouters` 的网络抖动、5xx、临时代理异常、非鉴权报错，都会被等价处理成“需要登出”
  - 这属于明确的非 401 清 token
  - 与问题 1 叠加后，当前链路既可能“异常时强退”，也可能“非 200 时缓存空菜单”
- 是否建议修复：是
- 修复方向：
  - `catch` 里应区分 401 和普通失败
  - 普通失败保留现有 admin token，只提示/重试，不要立即 `clearAdminSession()`
  - 清会话动作应尽量收敛到统一 401 处理链路

## 未发现对应问题

### 1. 响应体 401 不 reject

- 结论：未发现
- 证据：
  - `react-ui/src/app.tsx:308-315`
    - `if (isUnauthorizedCode(getResponseCode(response?.data))) {`
    - `handleUnauthorizedResponse(response?.config?.url);`
    - `return Promise.reject(response);`
  - `react-ui/src/app.js:267-274`
    - JS 镜像保持一致
- 判断：
  - 当前 runtime request 配置对响应体 401 已经在跳转后显式 reject，没有把原响应继续交给业务页当成功结果

### 2. admin / portal 401 串端

- 结论：未发现
- 证据：
  - `react-ui/src/utils/portalRequest.ts:33-43`
    - `getPortalTerminalFromApiUrl(...)` 会跳过 `/api/seller/admin`、`/api/buyer/admin`
  - `react-ui/src/requestErrorConfig.ts:33-42`
    - portal 请求只执行 `clearTerminalSessionToken(portalTerminal)`；否则才走 admin `clearSessionToken()`
  - `react-ui/src/app.tsx:58-67`
    - `handleUnauthorizedResponse(...)` 同样按 portal terminal 定向清 token / 跳登录
- 判断：
  - 当前 portal/admin 401 分流逻辑是按 URL 前缀做的，并且明确排除了 admin API 前缀，没有看到串端清 token 的 P0/P1 证据

### 3. 本轮目标文件 JS / TS 镜像不一致

- 结论：未发现
- 证据：
  - `react-ui/src/requestErrorConfig.js` 与 `react-ui/src/requestErrorConfig.ts` 的 `handleUnauthorized` / `errorHandler` 分支一致
  - `react-ui/src/app.js` 与 `react-ui/src/app.tsx` 的 `onRouteChange` / `render` / `responseInterceptors` 分支一致
  - `react-ui/src/services/session.js:1`
    - 直接 `export * from './session.ts';`
  - 执行 `node scripts/check-portal-token-isolation.mjs` 结果：`Portal token isolation guard passed.`
- 判断：
  - 本轮目标链路内没有发现 “TS 修了但 JS 镜像没跟上” 的问题

## 验证记录

本轮只读执行了以下检查：

```powershell
Get-Content -Encoding UTF8 docs\plans\2026-06-04-three-terminal-isolation-control-plan.md
rg -n "requestErrorConfig|responseInterceptors|getRoutersInfo|onRouteChange|render|admin_remote_menu|portal" react-ui
node scripts/check-portal-token-isolation.mjs
```

## 建议优先级

建议先修顺序：

1. `getRoutersInfo()` 非 200 返回 `[]` 的空缓存问题
2. `requestErrorConfig.ts` 的 `ErrorShowType.REDIRECT` 非 401 清 token
3. `onRouteChange` / `render` 的菜单拉取异常强退

这三处都属于快速推进口径里的 fail-closed 关键链路，修完后再看是否还需要补更细的 guard 或单测。
