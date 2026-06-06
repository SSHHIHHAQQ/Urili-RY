# 2026-06-06 seller/buyer 同构模板与 services 只读 P0/P1 审计

## 审计范围

- `react-ui/src/pages/Seller/index.tsx`
- `react-ui/src/pages/Buyer/index.tsx`
- `react-ui/src/components/PartnerManagement/*`
- `react-ui/src/services/seller/seller.ts`
- `react-ui/src/services/buyer/buyer.ts`
- `react-ui/src/utils/portalDirectLoginMessage.ts`
- `react-ui/src/utils/portalRequest.ts`
- `react-ui/src/services/portal/session.ts`
- `react-ui/src/app.tsx`
- `react-ui/src/requestErrorConfig.ts`
- `react-ui/src/pages/Portal/DirectLogin/index.tsx`
- `react-ui/scripts/check-partner-management-template.mjs`
- `react-ui/scripts/check-seller-portal-product-template.mjs`
- `react-ui/scripts/check-buyer-portal-product-template.mjs`
- `react-ui/scripts/check-portal-token-isolation.mjs`

## 结论

本次只读审计未发现符合本任务口径的 P0 / P1 问题。

当前 seller 模板作为母版、buyer 模板作为机械复制目标的关键约束已经落到共享模板、端隔离 service、postMessage 直登链路、portal token 隔离和 guard 脚本中。现状没有发现：

- seller/buyer 串端调用
- buyer 缺 seller 已有的权限、字段映射或 service 装配
- portal direct-login token 通过 URL 透传
- portal 401 误清 admin token 或误清另一个 terminal token

## P0 问题

无。

## P1 问题

无。

## 证据

### 1. seller/buyer 页面配置按端隔离，且装配项对齐

- `SellerPage` 使用 `moduleKey: 'seller'`、`sellerId/sellerNo/sellerCode/sellerName/sellerAccountId`，并装配 seller 全量 account/dept/menu/role/session/directLogin/audit services，见 `react-ui/src/pages/Seller/index.tsx:46-115`。
- `BuyerPage` 使用 `moduleKey: 'buyer'`、`buyerId/buyerNo/buyerCode/buyerName/buyerAccountId`，并装配 buyer 对应全量 services，见 `react-ui/src/pages/Buyer/index.tsx:46-116`。
- 两侧 `accountPermissions` 都完整覆盖 `list/add/edit/lock/resetPwd/roleQuery/roleEdit`，仅顺序不同，不存在 buyer 漏权限点，见：
  - `react-ui/src/pages/Seller/index.tsx:64-72`
  - `react-ui/src/pages/Buyer/index.tsx:65-73`
- 结构化核对结果：
  - seller service 导出数 `43`
  - buyer service 导出数 `43`
  - 归一化后导出集合一致
  - `config.services` key 数均为 `41`，顺序一致

### 2. 共享模板按 `config.moduleKey` 派生权限，不写死 seller/buyer

- `PartnerManagementPage` 用 `const permPrefix = \`${config.moduleKey}:admin\`` 作为页面级权限前缀，见 `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:538-549`。
- 页面主表上的状态切换、直登、部门、角色、重置主账号密码、会话、强制踢出、审计、菜单配置、新增主体都从 `permPrefix` 或 `accountPermissions` 取权限，没有硬编码 seller/buyer，见 `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:905-1103`。
- `PartnerAccountModal` 同样从 `config.accountPermissions` 和 `permPrefix` 取账号级权限，没有跨端 service 或固定路径，代码通过 `config.services.*` 调用，见 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:213-227, 316-429, 556-652`。

### 3. portal direct-login 使用 postMessage，且 origin/terminal 都被约束

- 管理端打开 portal 直登窗口时，不把 token 拼到 URL，而是通过 `postMessage(payload, targetOrigin)` 发给 popup；同时 ready message 也校验 `event.source === popup`、`event.origin === targetOrigin`、`terminal` 一致，见 `react-ui/src/utils/portalDirectLoginMessage.ts:16-85`。
- portal 直登页只接受 `window.opener` 发来的 message，并校验 `event.origin === openerOrigin`、`terminal` 一致，收到后才调用 `PORTAL_SERVICE[terminal].directLogin(token)`，见 `react-ui/src/pages/Portal/DirectLogin/index.tsx:31-119`。

### 4. portal request 层按 terminal 取 token，并清洗身份参数

- `portal/session.ts` 只从 `getTerminalAccessToken(terminal)` 取 terminal 专属 token，不走 admin token，见 `react-ui/src/services/portal/session.ts:24-27`。
- `sellerId/buyerId/subjectId/accountId/sellerAccountId/buyerAccountId/terminal` 都在 `PORTAL_SCOPE_PARAM_KEYS` 中，查询请求统一经过 `sanitizePortalQueryParams`，见 `react-ui/src/services/portal/session.ts:10-35`。
- seller portal 商品接口固定打到 `buildPortalUrl('seller', ...)`，buyer portal 商品接口固定打到 `buildPortalUrl('buyer', ...)`，并统一 `isToken: false`，见：
  - `react-ui/src/services/portal/session.ts:157-206`
  - `react-ui/src/services/portal/session.ts:208-257`

### 5. portal 401 只清理命中的 terminal，不误伤 admin 管理端

- `getPortalTerminalFromApiUrl` 明确排除 `/api/seller/admin` 与 `/api/buyer/admin`，只把 `/api/seller/**`、`/api/buyer/**` 非 admin 接口识别为 portal terminal，见 `react-ui/src/utils/portalRequest.ts:5-45`。
- `app.tsx` 收到 401 时，若命中 portal terminal，只执行 `clearTerminalSessionToken(portalTerminal)`，否则才清理 admin，会话边界正确，见 `react-ui/src/app.tsx:53-61, 273-307`。
- `requestErrorConfig.ts` 的业务错误与 HTTP 错误分支也沿用同一条 terminal 定位和清理逻辑，见 `react-ui/src/requestErrorConfig.ts:28-37, 76-80`。

### 6. guard 脚本已覆盖本次关注面，且实跑通过

- `node scripts/check-partner-management-template.mjs` -> `Partner management template guard passed.`
- `node scripts/check-seller-portal-product-template.mjs` -> `Seller portal product template guard passed.`
- `node scripts/check-buyer-portal-product-template.mjs` -> `Buyer portal product template guard passed.`
- `node scripts/check-portal-token-isolation.mjs` -> `Portal token isolation guard passed.`

## 最小修复建议

当前范围内不需要代码级修复。

建议仅做以下最小化守护动作：

1. 把以下四个 guard 作为 seller 模板改动后的必跑项，最好挂到 PR/CI：
   - `node scripts/check-partner-management-template.mjs`
   - `node scripts/check-seller-portal-product-template.mjs`
   - `node scripts/check-buyer-portal-product-template.mjs`
   - `node scripts/check-portal-token-isolation.mjs`
2. 继续坚持“只改 seller 母版，再机械复制 buyer”的策略；buyer 页面只允许替换：
   - `moduleKey`
   - 字段名映射
   - 权限前缀
   - service import / service binding
3. guard 脚本执行目录要固定在 `react-ui/`。如果从仓库根目录直接跑 `node react-ui/scripts/check-partner-management-template.mjs`，会因为 `process.cwd()` 指向错误而产生整片 `src/... is missing` 的假阳性。这是工具使用约束，不是业务代码缺陷。

## 低于 P1 的观察

### 1. `react-ui` 同时保留 `.ts/.tsx` 与 `.js` 双份文件

- 本次抽查中 seller/buyer、PartnerManagement、portal 相关链路都同时存在 TS 与 JS 双份实现。
- 当前 guard 脚本已对部分 JS/TS 双份文件同时检查，因此这项风险暂未上升为 P1。
- 但后续如果只改 TS 不同步 JS，容易出现“审查看到的是 TS，运行命中的是另一份文件”的维护漂移风险。

### 2. `PartnerManagementPage.tsx` 体量较大，但当前不构成 P1

- 文件已超过 500 行，当前约 1297 行。
- 这份大文件的收益在于 seller/buyer 共用一套主体管理模板，避免两端各自复制业务流程；在本次“避免串端、避免缺 service/权限”的目标下，共享模板优先级高于立即拆分。
- 当前风险更多是后续维护复杂度，而不是本次审计范围内的隔离缺陷。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
node scripts/check-partner-management-template.mjs
node scripts/check-seller-portal-product-template.mjs
node scripts/check-buyer-portal-product-template.mjs
node scripts/check-portal-token-isolation.mjs
```

## 未验证原因

- 本次是只读 P0/P1 审计，没有启动前端浏览器流，也没有改动代码后再做交互回归。
- 未对后端接口做在线联调；本报告聚焦静态代码证据、脚本守护和前端链路隔离。

## 权限检查结果

- seller 页面与 buyer 页面都把账号权限显式下放到 `accountPermissions`。
- 共享模板、账号弹窗、审计弹窗都通过 `config.moduleKey` 组合权限前缀。
- 未发现 buyer 漏掉 seller 已具备的页面级或账号级权限门控。

## 字典/选项复用检查结果

- 主体等级、主体类型、账号角色、账号锁定状态、菜单状态等都走统一字典加载或共享 fallback。
- 未发现 buyer 为了复制 seller 而单独内联一套冲突选项。

## 复用台账检查结果

- 本次范围内 seller/buyer 管理页通过 `PartnerManagementPage`、`PartnerAccountModal`、`PartnerRoleModal`、`PartnerDeptModal`、`PartnerMenuModal`、`PartnerAuditModal` 共享模板落地。
- 复用方向与“先做 seller 母版，再机械复制 buyer”的目标一致。

## CodeGraph 更新结果

- 本次未修改业务代码，也未改动报告之外的仓库文件。
- 未执行 `codegraph sync .`。

## 大文件合理性判断结果

- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx` 明显超过 500 行。
- 现阶段不拆分仍有合理性：它承担 seller/buyer 同构主体管理模板的单一共享职责，优先避免双端重复逻辑和权限/字段漂移。
- 该项记为后续维护风险，不记为本次 P0/P1。

## 重复代码检查结果

- seller/buyer 页面本身仅保留 config 差异，主体逻辑都落到共享模板与端专属 service。
- seller/buyer services 归一化后导出集合一致，没有发现 buyer 少 service、seller 独有能力未复制的情况。
