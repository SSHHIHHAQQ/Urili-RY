# React 管理端与 Portal 权限/接口只读扫描（2026-06-09）

## 结论

- 本次按指定范围做了只读静态扫描，**未发现新的 P0/P1 问题**。
- 已核对的关键链路当前符合三端独立账号权限快推要求：
  - seller/buyer admin service 入口均走 `/api/seller/admin/**` 或 `/api/buyer/admin/**`
  - portal service 会剥离 caller-controlled 的 `sellerId` / `buyerId` / `subjectId` / `accountId` / `terminal`
  - 401 会清对应端 token 并 `reject/throw`，不会继续当成功响应往下走
  - 远程菜单空 `authority` 继续 fail-closed
  - 卖家/买家管理页、账号弹窗、审计弹窗里的按钮和日志入口都有前端权限门禁
  - 大部分 JS sidecar 已收敛为单行 re-export，未见新的镜像漂移

## 范围

- `react-ui/src/services/seller`
- `react-ui/src/services/buyer`
- `react-ui/src/services/portal`
- `react-ui/src/services/system`
- `react-ui/src/pages/Seller`
- `react-ui/src/pages/Buyer`
- `react-ui/src/pages/Portal`
- `react-ui/src/pages/Product`
- `react-ui/src/pages/Inventory`
- `react-ui/src/pages/Warehouse`
- `react-ui/src/pages/UpstreamSystem`
- `react-ui/src/pages/Finance`
- `react-ui/config/routes`
- `react-ui/src/wrappers`
- `react-ui/src/app.*`
- `react-ui/src/requestErrorConfig.*`
- 与上述链路直接相关的 `PartnerManagement*` 组件、portal utils、相关前端测试

## P0 / P1

### 无新增 P0 / P1 发现

#### 证据 1：seller / buyer admin service 前缀正确

- Seller service 代表性证据：`/api/seller/admin/sellers/**`、`/api/seller/admin/menus/**`
  - [react-ui/src/services/seller/seller.ts](E:\Urili-Ruoyi\react-ui\src\services\seller\seller.ts:3)
  - [react-ui/src/services/seller/seller.ts](E:\Urili-Ruoyi\react-ui\src\services\seller\seller.ts:13)
  - [react-ui/src/services/seller/seller.ts](E:\Urili-Ruoyi\react-ui\src\services\seller\seller.ts:49)
  - [react-ui/src/services/seller/seller.ts](E:\Urili-Ruoyi\react-ui\src\services\seller\seller.ts:139)
- Buyer service 代表性证据：`/api/buyer/admin/buyers/**`、`/api/buyer/admin/menus/**`
  - [react-ui/src/services/buyer/buyer.ts](E:\Urili-Ruoyi\react-ui\src\services\buyer\buyer.ts:3)
  - [react-ui/src/services/buyer/buyer.ts](E:\Urili-Ruoyi\react-ui\src\services\buyer\buyer.ts:13)
  - [react-ui/src/services/buyer/buyer.ts](E:\Urili-Ruoyi\react-ui\src\services\buyer\buyer.ts:49)
  - [react-ui/src/services/buyer/buyer.ts](E:\Urili-Ruoyi\react-ui\src\services\buyer\buyer.ts:107)

#### 证据 2：portal service 会 strip caller-controlled scope 参数

- 剥离键集合包含 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId`、`terminal`
  - [react-ui/src/services/portal/session.ts](E:\Urili-Ruoyi\react-ui\src\services\portal\session.ts:10)
- `sanitizePortalQueryParams()` 对 query 做 fail-closed 过滤
  - [react-ui/src/services/portal/session.ts](E:\Urili-Ruoyi\react-ui\src\services\portal\session.ts:29)
- 登录日志、操作日志、会话、分销商品列表都走过滤后的参数
  - [react-ui/src/services/portal/session.ts](E:\Urili-Ruoyi\react-ui\src\services\portal\session.ts:124)
  - [react-ui/src/services/portal/session.ts](E:\Urili-Ruoyi\react-ui\src\services\portal\session.ts:135)
  - [react-ui/src/services/portal/session.ts](E:\Urili-Ruoyi\react-ui\src\services\portal\session.ts:146)
  - [react-ui/src/services/portal/session.ts](E:\Urili-Ruoyi\react-ui\src\services\portal\session.ts:177)
  - [react-ui/src/services/portal/session.ts](E:\Urili-Ruoyi\react-ui\src\services\portal\session.ts:228)

#### 证据 3：401 会 reject / throw，不会继续按成功路径处理

- app 级 response interceptor 发现响应体 `401` 后直接 `Promise.reject(response)`
  - [react-ui/src/app.tsx](E:\Urili-Ruoyi\react-ui\src\app.tsx:350)
- requestErrorConfig 对 BizError `401`、HTTP status `401` 都会先清 token/跳转，再 `throw error`
  - [react-ui/src/requestErrorConfig.ts](E:\Urili-Ruoyi\react-ui\src\requestErrorConfig.ts:81)
  - [react-ui/src/requestErrorConfig.ts](E:\Urili-Ruoyi\react-ui\src\requestErrorConfig.ts:116)
- portal/admin 401 reject 合同已有前端测试兜底
  - [react-ui/tests/portal-unauthorized-redirect.test.ts](E:\Urili-Ruoyi\react-ui\tests\portal-unauthorized-redirect.test.ts:109)
  - [react-ui/tests/portal-unauthorized-redirect.test.ts](E:\Urili-Ruoyi\react-ui\tests\portal-unauthorized-redirect.test.ts:127)
  - [react-ui/tests/portal-unauthorized-redirect.test.ts](E:\Urili-Ruoyi\react-ui\tests\portal-unauthorized-redirect.test.ts:202)

#### 证据 4：远程菜单空 authority 继续 fail-closed

- `RemoteMenuRouteGuard` 只有 `permissions.length > 0` 且权限命中才放行，否则直接 403
  - [react-ui/src/services/session.ts](E:\Urili-Ruoyi\react-ui\src\services\session.ts:112)
- 静态路由包装器会把 `/seller`、`/buyer`、`/product/distribution/*` 等路由补齐 authority
  - [react-ui/src/wrappers/RemoteMenuRouteGuard.tsx](E:\Urili-Ruoyi\react-ui\src\wrappers\RemoteMenuRouteGuard.tsx:12)
  - [react-ui/src/wrappers/RemoteMenuRouteGuard.tsx](E:\Urili-Ruoyi\react-ui\src\wrappers\RemoteMenuRouteGuard.tsx:106)
- route 配置本身也显式要求 seller/buyer admin authority
  - [react-ui/config/routes.ts](E:\Urili-Ruoyi\react-ui\config\routes.ts:48)
  - [react-ui/config/routes.ts](E:\Urili-Ruoyi\react-ui\config\routes.ts:54)

#### 证据 5：按钮、会话、审计日志入口受权限控制

- 卖家/买家主体页：
  - 审计入口以 `*:loginLog:list` / `*:operLog:list` / `*:ticket:list` 任一权限为前提
  - 主体级 direct-login / dept / role / session / forceLogout / menu / add 都按 `access.hasPerms(...)` 门禁
  - 证据：
    - [react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx](E:\Urili-Ruoyi\react-ui\src\components\PartnerManagement\PartnerManagementPage.tsx:481)
    - [react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx](E:\Urili-Ruoyi\react-ui\src\components\PartnerManagement\PartnerManagementPage.tsx:863)
    - [react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx](E:\Urili-Ruoyi\react-ui\src\components\PartnerManagement\PartnerManagementPage.tsx:978)
- 账号弹窗：
  - “分配角色”同时要求 `*:role:query` + `*:account:role:query` + `*:account:role:edit`
  - 账号级 direct-login / resetPwd / session / audit / forceLogout 都做了权限判断
  - 证据：
    - [react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx](E:\Urili-Ruoyi\react-ui\src\components\PartnerManagement\PartnerAccountModal.tsx:253)
    - [react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx](E:\Urili-Ruoyi\react-ui\src\components\PartnerManagement\PartnerAccountModal.tsx:258)
    - [react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx](E:\Urili-Ruoyi\react-ui\src\components\PartnerManagement\PartnerAccountModal.tsx:645)
- 审计弹窗：
  - 登录日志、操作日志、免密票据三个 Tab 分别按独立权限展示
  - 证据：
    - [react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx](E:\Urili-Ruoyi\react-ui\src\components\PartnerManagement\PartnerAuditModal.tsx:436)

#### 证据 6：Product / Inventory / Warehouse / UpstreamSystem / Finance 页面已继续走 useAccess 门禁

- 代表性页面：
  - [react-ui/src/pages/Product/Distribution/index.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Product\Distribution\index.tsx:246)
  - [react-ui/src/pages/Inventory/Overview/index.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Inventory\Overview\index.tsx:45)
  - [react-ui/src/pages/Inventory/SourceWarehouseStock/index.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Inventory\SourceWarehouseStock\index.tsx:333)
  - [react-ui/src/pages/Warehouse/WarehouseManagementPage.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Warehouse\WarehouseManagementPage.tsx:113)
  - [react-ui/src/pages/UpstreamSystem/index.tsx](E:\Urili-Ruoyi\react-ui\src\pages\UpstreamSystem\index.tsx:88)
  - [react-ui/src/pages/Finance/Currency/index.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Finance\Currency\index.tsx:77)

#### 证据 7：JS sidecar 当前大多为单行 re-export，未见新的镜像漂移

- 代表性 sidecar：
  - [react-ui/src/app.js](E:\Urili-Ruoyi\react-ui\src\app.js:1)
  - [react-ui/src/requestErrorConfig.js](E:\Urili-Ruoyi\react-ui\src\requestErrorConfig.js:1)
  - [react-ui/src/services/seller/seller.js](E:\Urili-Ruoyi\react-ui\src\services\seller\seller.js:1)
  - [react-ui/src/services/portal/session.js](E:\Urili-Ruoyi\react-ui\src\services\portal\session.js:1)
  - [react-ui/config/routes.js](E:\Urili-Ruoyi\react-ui\config\routes.js:1)

## P2

### P2-1 `system/user.js` 仍是手写完整镜像，漂移风险高于 export-only sidecar

- 现状：
  - TS 版接口定义在 [react-ui/src/services/system/user.ts](E:\Urili-Ruoyi\react-ui\src\services\system\user.ts:123)
  - JS 版不是单行 re-export，而是完整复制实现 [react-ui/src/services/system/user.js](E:\Urili-Ruoyi\react-ui\src\services\system\user.js:108)
- 影响：
  - 当前看 `authRole` 前缀没有错，但后续一旦 `user.ts` 调整参数、headers、返回值或错误处理，`user.js` 更容易遗漏同步。
- 建议最小修复：
  - 把 `react-ui/src/services/system/user.js` 改成和其它 sidecar 一致的单行 `export * from './user.ts';`。

### P2-2 静态 route authority 存在双份定义，后续仍有文档外漂移风险

- 现状：
  - `services/session.ts` 内维护了一份 `STATIC_GUARDED_LAYOUT_ROUTES`
    - [react-ui/src/services/session.ts](E:\Urili-Ruoyi\react-ui\src\services\session.ts:146)
  - `wrappers/RemoteMenuRouteGuard.tsx` 内又维护了一份 `STATIC_ROUTE_REQUIREMENTS`
    - [react-ui/src/wrappers/RemoteMenuRouteGuard.tsx](E:\Urili-Ruoyi\react-ui\src\wrappers\RemoteMenuRouteGuard.tsx:12)
- 影响：
  - 当前两份内容看起来仍然对齐，但新增静态页面时容易只改一处，形成“路由能进但菜单补丁/包装守卫不一致”的隐性漂移。
- 建议最小修复：
  - 抽一份共享常量给 `session.ts` 和 `RemoteMenuRouteGuard.tsx` 共用，保留现有契约测试继续兜底。

## 建议最小修复顺序

1. 先把 `react-ui/src/services/system/user.js` 收敛成 export-only sidecar。
2. 再把静态 route authority 提取成单一来源。
3. P0/P1 本轮无需因为上述扫描结果额外改 seller/buyer admin service、portal request、401 流程或按钮权限门禁。

## 备注

- 本次为只读静态扫描，**未运行浏览器验证、未执行接口调用、未改源码**。
- 工作区当前存在大量未提交修改；本报告没有回滚或覆盖任何现有改动。
