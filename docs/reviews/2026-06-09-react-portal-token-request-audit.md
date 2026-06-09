# React Portal Token/Request 链路只读审计

日期：2026-06-09

## 审计范围

- `react-ui/src/app.tsx`
- `react-ui/src/requestErrorConfig.ts`
- `react-ui/src/access.ts`
- `react-ui/src/services/session.ts`
- `react-ui/src/services/portal/session.ts`
- `react-ui/src/utils/portalPaths.ts`
- `react-ui/src/utils/portalRequest.ts`
- `react-ui/src/utils/portalDirectLoginMessage.ts`
- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx`
- `react-ui/src/pages/Portal/Login/index.tsx`
- `react-ui/src/pages/Portal/DirectLogin/index.tsx`
- `react-ui/src/pages/Portal/terminal.ts`
- `react-ui/config/proxy.ts`
- 对应 `.js` mirror
- `react-ui/tests/*portal*`, `react-ui/tests/*terminal*`, `react-ui/tests/*authority*`
- `react-ui/scripts/check-portal-token-isolation.mjs`
- `react-ui/tests/three-terminal.manifest.json`

## 结论

本次按 P0/P1 关注点进行只读审计，**未发现命中的 P0/P1 问题**。以下关键风险点当前实现均满足 fail-closed 或已有 guard/test 固定：

- seller/buyer 401 仅清当前端 token，不误清 admin 或另一端 token
- portal 登录页 redirect 白名单未放宽到 `/seller/*`、`/buyer/*` 管理端路径
- direct-login 失败、超时、跨端或 401 时不会预清已有 portal token
- 全局响应拦截器命中响应体 `code/errorCode = 401` 后会 reject，不会继续当成功结果下发
- dev proxy 仍保留 `^/api -> ''` rewrite
- 空 `authority` 仍然 403 fail-closed
- 相关 JS mirror 已纳入 guard/test，当前都是纯 re-export

## 新增问题

- 无

## 已修复问题

- 本次只读审计，未修改代码

## 残留问题

- 未发现本轮点名范围内的 P0/P1 残留问题
- 非阻塞工程项：`react-ui` 同时存在 `jest.config.js` 与 `jest.config.ts`，直接运行未指定 `--config` 的 Jest 会报多配置冲突；本次通过显式 `--config .\\jest.config.js` 绕过。该项不属于你点名的 portal/token/request/proxy/access/direct-login/401 业务缺陷，但会影响手工执行测试命令的稳定性

## 关键证据

### 1. seller/buyer 401 只清当前端 token

- `react-ui/src/app.tsx:73-85`：`handleUnauthorizedResponse` 先用 `getPortalTerminalFromApiUrl(requestUrl)` 判定端；命中 portal 且不是 direct-login API 时，仅执行 `clearTerminalSessionToken(portalTerminal)`，随后 `redirectToPortalLogin(portalTerminal)`
- `react-ui/src/requestErrorConfig.ts:33-45`：request error handler 同样只清匹配 terminal 的 token；admin 路径走 `clearSessionToken()`
- `react-ui/src/utils/portalRequest.ts:33-43`：明确排除了 `/api/seller/admin/**` 与 `/api/buyer/admin/**`，避免管理端接口被误判为 portal 401
- `react-ui/tests/portal-unauthorized-redirect.test.ts:89-125, 207-280`：覆盖 seller portal 401、buyer portal 401、seller/buyer admin 401 三类分流

### 2. direct-login 失败不清已有 token

- `react-ui/src/app.tsx:75-81`：`isPortalDirectLoginApiUrl(requestUrl)` 命中时直接 `return`，不会清 token 或跳登录
- `react-ui/src/requestErrorConfig.ts:35-41`：同样对 direct-login API 401 直接返回
- `react-ui/src/pages/Portal/terminal.ts:42-48`：`persistPortalLogin` 只在 `result.token` 存在且 `result.terminal === expectedTerminal` 时写 token；失败只返回 `false`
- `react-ui/src/pages/Portal/Login/index.tsx:67-76`：登录失败仅 `message.error`，没有预清 token
- `react-ui/src/pages/Portal/DirectLogin/index.tsx:75-95`：direct-login 消费失败仅回传 error 并展示错误态，没有清任何 terminal token
- `react-ui/tests/terminal-session-token.test.ts:75-102`、`react-ui/tests/portal-unauthorized-redirect.test.ts:127-205`：覆盖 terminal mismatch、BizError 401、HTTP 401、响应体 401 不清 token

### 3. redirect 白名单没有放宽

- `react-ui/src/utils/portalPaths.ts:23-28`：portal 合法路径只认 `/{terminal}/login`、`/{terminal}/direct-login`、`/{terminal}/portal/**`
- `react-ui/src/pages/Portal/Login/index.tsx:38-50`：`resolveRedirect` 要求 `isPortalTerminalPath(redirect, terminal)` 为真，且排除当前端 login/direct-login；否则回退到 `PORTAL_META[terminal].homePath`
- `react-ui/tests/portal-session-request.test.ts:246-258`：明确断言 `/seller/login/next`、`/seller/direct-login/next`、`/buyer/admin/menus`、`/seller`、`/buyer` 不是 portal 合法路径

### 4. 响应体 401 会 reject

- `react-ui/src/app.tsx:353-359`：response interceptor 发现 `getResponseCode(response?.data) === 401` 后，先走 unauthorized handler，再 `return Promise.reject(response)`
- `react-ui/tests/portal-unauthorized-redirect.test.ts:143-179, 262-280`：覆盖 buyer portal body-level 401、buyer direct-login body-level 401、seller admin body-level 401，均断言 `rejects`

### 5. proxy rewrite 未丢失

- `react-ui/config/proxy.ts:12-29`：dev 代理仍是 `API_PROXY_TARGET || 'http://127.0.0.1:8080'`，并保留 `pathRewrite: { '^/api': '' }`
- `react-ui/config/proxy.js:1`：JS mirror 为纯 re-export
- `react-ui/scripts/check-portal-token-isolation.mjs:1005-1034`：显式 guard `API_PROXY_TARGET`、`changeOrigin: true`、`^/api` rewrite 和 `proxy.js` mirror

### 6. 空 authority 仍 fail-closed

- `react-ui/src/services/session.ts:95-107`：空 perms 转成 `[]`
- `react-ui/src/services/session.ts:121-135`：`allowed = permissions.length > 0 && ...`，空 authority 必定 403
- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx:111-114`：route 未给 authority 时才回退静态 authority；再缺失则传 `[]` 给 guard
- `react-ui/tests/remote-menu-route-guard.test.ts:150-165`
- `react-ui/tests/getrouters-authority-contract.test.ts:19-53`

### 7. JS mirror 已纳入 guard

- 运行入口/关键 sidecar：
  - `react-ui/src/app.js:1-9`
  - `react-ui/src/access.js:1-2`
  - `react-ui/src/requestErrorConfig.js:1`
  - `react-ui/src/utils/portalPaths.js:1`
  - `react-ui/src/utils/portalRequest.js:1`
  - `react-ui/src/utils/portalDirectLoginMessage.js:1`
  - `react-ui/src/pages/Portal/terminal.js:1`
  - `react-ui/src/pages/Portal/Login/index.js:1`
  - `react-ui/src/pages/Portal/DirectLogin/index.js:1`
  - `react-ui/src/pages/Portal/Home/index.js:1`
  - `react-ui/config/proxy.js:1`
  - `react-ui/config/routes.js:1`
- `react-ui/tests/admin-auth-sidecar-contract.test.ts:8-182`：对上述 sidecar 建立纯 re-export 契约
- `react-ui/scripts/check-portal-token-isolation.mjs:901-1068`：对 `app.js`、`requestErrorConfig.js`、`proxy.js`、`access.js`、portal 相关 JS mirror 建立 guard
- `react-ui/tests/three-terminal.manifest.json:117-177`：`guard:portal-token` 已登记进三端 manifest

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run guard:portal-token
node .\node_modules\jest\bin\jest.js --config .\jest.config.js tests/portal-unauthorized-redirect.test.ts tests/portal-session-request.test.ts tests/portal-direct-login-message.test.ts tests/terminal-session-token.test.ts tests/remote-menu-route-guard.test.ts tests/getrouters-authority-contract.test.ts tests/static-route-authority-contract.test.ts tests/portal-home-error-handling.test.ts --runInBand
```

结果：

- `guard:portal-token` 通过
- 相关 8 个测试集通过，74 个测试通过

## 未验证原因

- 按本轮要求，未做浏览器、截图、DOM、UI 细调验证
- 未执行完整 `verify-three-terminal` 全套；本次只跑了与审计范围直接相关的前端 guard 和 focused tests

## 数据源确认结果

- 本次为前端只读静态审计
- 未读取 MySQL/Redis 配置
- 未触达远端 DB/Redis

## 远端 DB/Redis 影响记录

- 无

## 表设计 / 高影响 SQL / 确认 token / 回滚

- 不涉及

## 三端隔离判断结果

- 账号：portal 登录持久化要求 `result.terminal === expectedTerminal`
- 权限：空 authority 403，静态管理页 authority 显式绑定
- 菜单：remote menu 缺 perms 会转成空 authority 并 fail-closed
- 日志/会话：portal 自助查询请求会剥离前端传入的 `sellerId/buyerId/subjectId/accountId`
- token/Redis key：前端 token storage 已按 admin/seller/buyer 隔离；本次未涉及后端 Redis key 运行态核验
- 串端：当前点名范围内未发现 seller/buyer/admin 串端处理

## 权限检查结果

- 通过；未发现 seller/buyer 空 authority 放行、seller/buyer admin 401 分流错误、或 portal 路由被管理端 authority 污染的证据

## 字典/选项复用检查结果

- 本轮不涉及

## 复用台账检查结果

- 本轮不涉及新增复用项；未修改代码

## CodeGraph 更新结果

- 未执行；本次只读审计，无代码更新

## 大文件合理性判断结果

- 本轮不涉及新增/修改业务文件

## 重复代码检查结果

- 只读审计，未发现本轮点名链路中因镜像文件产生逻辑分叉；相关 `.js` 文件均为纯 re-export

## 子 Agent 使用记录

- 未使用
