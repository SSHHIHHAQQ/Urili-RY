# 2026-06-09 Task 4 React 管理端只读 P0/P1 审计

## 范围

- 仓库：`E:\Urili-Ruoyi`
- 前端范围：`react-ui` 管理端 seller / buyer 页面与 `PartnerManagement` 复用组件
- 重点：`PartnerManagement` / `Seller` / `Buyer` 的 service URL、terminal/type 配置、权限标识、reset password / direct login / session / log / role / menu 入口
- 模式：只读审计，不改代码，不做浏览器 / 截图 / DOM / UI 细调

## 结论

本次范围内**未发现 P0/P1 问题**。当前 seller / buyer 管理端页面和 services 在以下高风险点上保持一致：

- seller 与 buyer 的页面配置、service URL、`moduleKey`、`idField`、`accountIdField` 均按端隔离
- reset password / direct login / session / force logout / role / menu / audit 入口均命中正确端接口
- 账号角色分配入口的前端权限组合与后端 `@PreAuthorize` 组合一致
- 会话查看与强制踢出权限已分离，未出现把 `session:list` 误绑到 `forceLogout` 的串权
- 审计查询参数对账号级范围做了 `subjectId + accountId` / `targetSubjectId + targetAccountId` 绑定，未出现裸账号查询
- 端内菜单编辑表单包含 seller / buyer 串端防护：禁止 opposite terminal、禁止 admin namespace、禁止 wildcard perms

## P0/P1 Findings

无。

## 证据

### 1. seller / buyer 页面配置按端隔离

- `react-ui/src/pages/Seller/index.tsx` 将 `moduleKey` 固定为 `seller`，并使用 `sellerId` / `sellerAccountId` 与 seller services 绑定，见 `Seller` 页配置和 service 注入：`react-ui/src/pages/Seller/index.tsx:36-110`
- `react-ui/src/pages/Buyer/index.tsx` 将 `moduleKey` 固定为 `buyer`，并使用 `buyerId` / `buyerAccountId` 与 buyer services 绑定，见 `Buyer` 页配置和 service 注入：`react-ui/src/pages/Buyer/index.tsx:36-111`
- 路由层也按端隔离，`/seller` 只要求 `seller:admin:list`，`/buyer` 只要求 `buyer:admin:list`：`react-ui/config/routes.ts:48-58`

### 2. service URL 与后端 controller 一一对应

#### seller

- 前端 seller account / session / direct login / audit service：`react-ui/src/services/seller/seller.ts:42-335`
- 后端 seller admin controller 对应接口与权限：
  - accounts：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:84-89`
  - account roles：`.../AdminSellerController.java:91-107`
  - reset password：`.../AdminSellerController.java:143-150`
  - sessions / account sessions：`.../AdminSellerController.java:152-170`
  - force logout：`.../AdminSellerController.java:172-186`
  - direct login / account direct login：`.../AdminSellerController.java:188-205`
  - login / oper / ticket audit：`.../AdminSellerController.java:207-235`
- 逐项比对后，前端 URL 前缀均为 `/api/seller/admin/...`，未混入 buyer 路由

#### buyer

- 前端 buyer account / session / direct login / audit service：`react-ui/src/services/buyer/buyer.ts:42-335`
- 后端 buyer admin controller 对应接口与权限：
  - accounts：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:84-89`
  - account roles：`.../AdminBuyerController.java:91-107`
  - reset password：`.../AdminBuyerController.java:143-150`
  - sessions / account sessions：`.../AdminBuyerController.java:152-170`
  - force logout：`.../AdminBuyerController.java:172-186`
  - direct login / account direct login：`.../AdminBuyerController.java:188-205`
  - login / oper / ticket audit：`.../AdminBuyerController.java:207-235`
- 逐项比对后，前端 URL 前缀均为 `/api/buyer/admin/...`，未混入 seller 路由

### 3. reset password / direct login / session / force logout 权限绑定正确

- 主体页“更多”菜单：
  - direct login 仅看 `${permPrefix}:directLogin`
  - session 仅看 `${permPrefix}:session:list`
  - force logout 仅看 `${permPrefix}:forceLogout`
  - 见 `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:863-898`
- 账号页“更多”菜单同样把三者分开，未把“会话”入口继续绑到强退权限：`react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:645-664`
- 对应后端接口也严格分离：
  - sessions：`seller ...:152-170` / `buyer ...:152-170`
  - force logout：`seller ...:172-186` / `buyer ...:172-186`
  - direct login：`seller ...:188-205` / `buyer ...:188-205`

### 4. 账号角色分配权限组合正确

- 前端“分配角色”按钮要求：
  - `*:admin:role:query`
  - `*:admin:account:role:query`
  - `*:admin:account:role:edit`
  - 见 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:253-256,676-683`
- 后端 account roles 查询 / 写入要求：
  - seller：`RuoYi-Vue/seller/.../AdminSellerController.java:91-107`
  - buyer：`RuoYi-Vue/buyer/.../AdminBuyerController.java:91-107`
- 前后端权限组合一致，未发现“按钮能点但接口 403”或“缺少 account role query”问题

### 5. 审计查询参数未出现裸账号范围

- `buildAuditParams` 只有在存在 `partnerId` 时才会附带 `accountId`，避免裸账号查询：`react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx:126-157`
- 登录/操作日志使用 `subjectId + accountId`；免密票据使用 `targetSubjectId + targetAccountId`：`.../PartnerAuditModal.tsx:456-497`
- seller / buyer 后端服务都会在账号级日志查询时要求先解析主体范围：
  - seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:476-514,592-607`
  - buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:476-514,592-607`

### 6. account payload 字段名与共享/端域模型一致

- 前端 account payload 使用 `userName` / `nickName` / `password` / `deptId` / `accountRole` / `status` / `phonenumber`，并同时带 `accountId` 与端专属 `sellerAccountId` / `buyerAccountId`：`react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:179-200`
- 前端共享 typings 定义同样使用这些字段：`react-ui/src/types/seller-buyer/party.d.ts:12-35`
- 后端共享 `PortalAccount` 与端专属 `SellerAccount` / `BuyerAccount` 字段名一致：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalAccount.java:15-189`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/domain/SellerAccount.java:12-58`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/domain/BuyerAccount.java:12-58`
- 未发现字段名错发到错误 terminal domain 的问题

### 7. 端内菜单表单包含 seller / buyer 串端防护

- 路由地址禁止 opposite terminal 与 admin/shared root：`react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx:201-218`
- 组件路径必须使用当前 terminal root，禁止 opposite terminal：`.../PartnerMenuModal.tsx:220-241`
- perms 必须以当前 terminal 前缀开头，禁止 wildcard 与 `*:admin:*`：`.../PartnerMenuModal.tsx:243-259`
- seller / buyer 后端菜单 controller 也分别独立：
  - seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerMenuController.java:25-85`
  - buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerMenuController.java:24-84`

## 已执行验证

在 `E:\Urili-Ruoyi\react-ui` 执行：

```powershell
npm run tsc
node scripts/check-partner-management-template.mjs
npx jest --config jest.config.ts tests/partner-audit-modal.test.ts tests/remote-menu-route-guard.test.ts --runInBand
```

结果：

- `tsc --noEmit` 通过
- `check-partner-management-template.mjs` 通过
- `partner-audit-modal` 与 `remote-menu-route-guard` 共 18 个测试全部通过

补充说明：

- `jest` 默认执行若不显式指定 `--config`，会因 `jest.config.js` 与 `jest.config.ts` 同时存在而报配置冲突；这属于测试工具调用方式问题，不是本次 seller / buyer 页面或 services 的 P0/P1 缺陷

## 未执行项

- 未做浏览器、截图、DOM、UI 细调验证：按本次快速推进模式明确跳过
- 未改任何业务代码、SQL、菜单 seed 或配置

## 一句话总结

本次切片 4 审计范围内，React 管理端 seller / buyer 的页面配置、services、权限入口和审计/会话/免密登录链路未发现 P0/P1 级 seller/buyer 串端或字段缺失问题。
