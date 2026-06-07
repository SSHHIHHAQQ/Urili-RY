# react-ui 三端 service URL 合同只读扫描

- 日期: 2026-06-07
- 范围: `react-ui` seller / buyer / admin 前端 service URL 合同
- 模式: 只读扫描
- 目标: 只找 P0 / P1, 聚焦编译、guard、接口、权限、串端、service/字段缺失

## 本轮结论

本轮未发现 P0 / P1 级别问题。

已重点核查以下风险点:

1. seller / buyer / admin 是否存在跨端 API 调用
2. `/api/seller/admin` 与 `/api/buyer/admin` 是否混用
3. 函数名与 URL 是否错配
4. manifest / guard 是否漏覆盖关键三端入口
5. portal 401 和远程菜单 guard 是否把 admin 与 portal 前缀正确隔离

## P0 / P1 列表

### 无 P0

未发现会直接导致三端 URL 合同错路由、串端鉴权、admin/portal 401 误清 token、seller/buyer service 混用的 P0 问题。

### 无 P1

未发现 seller service 引到 buyer admin URL、buyer service 引到 seller admin URL、函数名与 URL 语义不匹配、manifest 漏掉关键三端前端测试、guard 漏掉 portal/admin 前缀分流的 P1 问题。

## 关键证据

### 1. seller admin service URL 前缀一致

- `react-ui/src/services/seller/seller.ts:3-339`
- 代表性命中:
  - `getAdminSellerList -> /api/seller/admin/sellers/list`
  - `getAdminSellerMenuTree -> /api/seller/admin/menus/treeselect`
  - `getAdminSellerSessions -> /api/seller/admin/sellers/{sellerId}/sessions/list`
  - `createAdminSellerAccountDirectLogin -> /api/seller/admin/sellers/{sellerId}/accounts/{sellerAccountId}/directLogin`

结论: seller admin service 未发现 buyer 前缀混入。

### 2. buyer admin service URL 前缀一致

- `react-ui/src/services/buyer/buyer.ts:3-339`
- 代表性命中:
  - `getAdminBuyerList -> /api/buyer/admin/buyers/list`
  - `getAdminBuyerMenuTree -> /api/buyer/admin/menus/treeselect`
  - `getAdminBuyerSessions -> /api/buyer/admin/buyers/{buyerId}/sessions/list`
  - `createAdminBuyerAccountDirectLogin -> /api/buyer/admin/buyers/{buyerId}/accounts/{buyerAccountId}/directLogin`

结论: buyer admin service 未发现 seller 前缀混入。

### 3. portal 与 admin 401 分流 guard 存在且方向正确

- `react-ui/src/utils/portalRequest.ts:5-45`
- `react-ui/src/requestErrorConfig.ts:19-42`

关键点:

- `portalRequest.ts` 明确把 `/api/seller/admin` 和 `/api/buyer/admin` 从 portal 识别中排除
- `requestErrorConfig.ts` 仅对非 admin portal URL 调 `clearTerminalSessionToken(portalTerminal)` 和 portal 登录跳转
- admin 路径继续走 `clearSessionToken()` 与 `/user/login`

结论: 未发现 `/api/seller/admin/**`、`/api/buyer/admin/**` 被误判为 portal 请求的风险。

### 4. 远程菜单静态 fallback guard 覆盖 seller / buyer 管理入口

- `react-ui/config/routes.ts:48-70`
- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx:5-59`

关键点:

- `/seller` 绑定 `seller:admin:list`
- `/buyer` 绑定 `buyer:admin:list`
- `RemoteMenuRouteGuard` 对空 authority fail-closed
- `PUBLIC_PORTAL_ROUTE_PATHS` 排除了 `/seller/login`、`/buyer/login`、`/seller/direct-login`、`/buyer/direct-login`、`/seller/portal`、`/buyer/portal`

结论: seller / buyer 管理页的静态 fallback 权限和 portal 公共页豁免均已覆盖。

### 5. manifest 与 verify 脚本已把关键 frontend guard/tests 纳入

- `react-ui/tests/three-terminal.manifest.json:79-86`
- `react-ui/scripts/verify-three-terminal.mjs:31-55`
- `react-ui/scripts/verify-three-terminal.mjs:163-183`
- `react-ui/scripts/verify-three-terminal.mjs:261-330`

关键点:

- manifest 收录了:
  - `tests/terminal-session-token.test.ts`
  - `tests/portal-session-request.test.ts`
  - `tests/remote-menu-route-guard.test.ts`
  - `tests/portal-unauthorized-redirect.test.ts`
- `verify-three-terminal.mjs` 会额外执行:
  - `guard:portal-token`
  - `guard:partner-management`
  - `guard:seller-portal-product`
  - `guard:buyer-portal-product`
- `assertFrontendTestSourcesIncluded()` 会阻止 critical frontend tests 漏入 manifest

结论: 本轮目标范围内没有 manifest / guard 漏覆盖证据。

## 已有测试 / guard 覆盖

### 已验证通过

1. `node scripts/verify-three-terminal.mjs --check-manifest`
   - 结果: passed
2. `node scripts/check-portal-token-isolation.mjs`
   - 结果: passed
3. `node scripts/check-partner-management-template.mjs`
   - 结果: passed
4. `npm run test:unit -- --runTestsByPath tests/portal-unauthorized-redirect.test.ts tests/remote-menu-route-guard.test.ts tests/terminal-session-token.test.ts --runInBand`
   - 结果: 3 suites passed, 20 tests passed
5. `npm run test:unit -- --runTestsByPath tests/portal-session-request.test.ts --runInBand`
   - 结果: 1 suite passed, 3 tests passed

### 覆盖到的风险面

- portal/admin URL 分类
- portal 401 只清当前 terminal token
- admin 401 不误走 portal 登录
- seller / buyer 静态 fallback authority
- 空 authority fail-closed
- 远程菜单缓存按 terminal scope 隔离
- portal query 参数剥离 `sellerId` / `buyerId` / `subjectId` / `accountId` / `terminal`

## 最小修复建议

本轮没有需要立刻落地的 P0 / P1 修复。

建议后续继续保持以下最小约束:

1. 新增 seller admin service 时只允许使用 `/api/seller/admin/**`
2. 新增 buyer admin service 时只允许使用 `/api/buyer/admin/**`
3. 新增 portal service 时统一走 `src/services/portal/session.ts`，不要在 portal 页面里直写 request URL
4. 新增 seller / buyer / portal 相关测试文件时，命中 critical pattern 的必须同步登记到 `tests/three-terminal.manifest.json`
5. 如果后续新增第三类 terminal 路由或新的 portal 公共页，必须同步更新:
   - `src/utils/portalRequest.ts`
   - `src/requestErrorConfig.ts`
   - `src/wrappers/RemoteMenuRouteGuard.tsx`
   - 对应 unit test 与 guard script

## 未覆盖项

本轮未做后端 live 接口联调，也未做浏览器级 seller / buyer / admin 实际登录跳转验证；本次结论基于前端源码、guard 脚本和单测执行结果。
