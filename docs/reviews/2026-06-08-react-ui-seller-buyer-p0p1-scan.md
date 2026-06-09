# react-ui Seller/Buyer 管理端 P0/P1 只读扫描

- 扫描时间：2026-06-08
- 扫描范围：
  - `react-ui/src/pages/Seller`
  - `react-ui/src/pages/Buyer`
  - `react-ui/src/services/seller`
  - `react-ui/src/services/buyer`
  - `react-ui/src/types/seller-buyer`
  - `react-ui/tests/*`、`react-ui/scripts/check-partner-management-template.mjs`
- 关注项：
  - 卖家/买家管理端页面或 service 字段缺失
  - 权限标识串端
  - seller/buyer 复制不一致
  - 重置密码 / 免密登录 / 余额占位 / 创建时间 + 最后登录时间字段契约

## 结论

本次只读扫描**未发现 P0/P1 级别问题**。

## P0/P1 Findings

| 文件 | 行号 | 风险 | 建议 |
| --- | --- | --- | --- |
| 无 | - | 本次未发现 P0/P1 | 维持当前 seller/buyer 模板 guard 与三端前端契约测试；后续若改 `PartnerManagement` 模板或 seller/buyer service，先跑 partner-management / portal-token guard，并复跑相关 Jest 用例。 |

## 关键核对点

1. seller/buyer 页面配置未发现串端或缺项
   - `react-ui/src/pages/Seller/index.tsx:46` 绑定 `moduleKey: 'seller'`
   - `react-ui/src/pages/Seller/index.tsx:58` 卖家余额文案仍为占位态
   - `react-ui/src/pages/Seller/index.tsx:63` 账号权限前缀为 `seller:admin:*`
   - `react-ui/src/pages/Seller/index.tsx:108`
   - `react-ui/src/pages/Seller/index.tsx:109`
   - `react-ui/src/pages/Seller/index.tsx:112`
   - `react-ui/src/pages/Buyer/index.tsx:46` 绑定 `moduleKey: 'buyer'`
   - `react-ui/src/pages/Buyer/index.tsx:58` 买家余额文案仍为占位态
   - `react-ui/src/pages/Buyer/index.tsx:59` 买家额外保留 `showRechargePlaceholder`
   - `react-ui/src/pages/Buyer/index.tsx:64` 账号权限前缀为 `buyer:admin:*`
   - `react-ui/src/pages/Buyer/index.tsx:109`
   - `react-ui/src/pages/Buyer/index.tsx:110`
   - `react-ui/src/pages/Buyer/index.tsx:113`

2. seller/buyer service 关键接口路径成对一致，未发现串端
   - `react-ui/src/services/seller/seller.ts:256` 重置密码走 `/api/seller/admin/sellers/.../resetPwd`
   - `react-ui/src/services/seller/seller.ts:273` 主体会话列表走 `/api/seller/admin/sellers/.../sessions/list`
   - `react-ui/src/services/seller/seller.ts:289` 账号会话列表走 `/api/seller/admin/sellers/.../accounts/.../sessions/list`
   - `react-ui/src/services/seller/seller.ts:298` 主体免密登录走 `/api/seller/admin/sellers/.../directLogin`
   - `react-ui/src/services/seller/seller.ts:309` 账号免密登录走 `/api/seller/admin/sellers/.../accounts/.../directLogin`
   - `react-ui/src/services/seller/seller.ts:335` 免密票据审计走 `/api/seller/admin/sellers/directLoginTickets/list`
   - `react-ui/src/services/buyer/buyer.ts:256` 重置密码走 `/api/buyer/admin/buyers/.../resetPwd`
   - `react-ui/src/services/buyer/buyer.ts:273` 主体会话列表走 `/api/buyer/admin/buyers/.../sessions/list`
   - `react-ui/src/services/buyer/buyer.ts:289` 账号会话列表走 `/api/buyer/admin/buyers/.../accounts/.../sessions/list`
   - `react-ui/src/services/buyer/buyer.ts:298` 主体免密登录走 `/api/buyer/admin/buyers/.../directLogin`
   - `react-ui/src/services/buyer/buyer.ts:309` 账号免密登录走 `/api/buyer/admin/buyers/.../accounts/.../directLogin`
   - `react-ui/src/services/buyer/buyer.ts:335` 免密票据审计走 `/api/buyer/admin/buyers/directLoginTickets/list`

3. 关键类型字段仍在，未发现“创建时间 / 最后登录时间 / 审计字段”缺失
   - `react-ui/src/types/seller-buyer/party.d.ts:12` `PortalAccountBase`
   - `react-ui/src/types/seller-buyer/party.d.ts:24` `lastLoginTime?: string`
   - `react-ui/src/types/seller-buyer/party.d.ts:27` `createTime?: string`
   - `react-ui/src/types/seller-buyer/party.d.ts:153` `DirectLoginResult`
   - `react-ui/src/types/seller-buyer/party.d.ts:300` `PortalLoginLog`
   - `react-ui/src/types/seller-buyer/party.d.ts:319` `PortalOperLog`
   - `react-ui/src/types/seller-buyer/party.d.ts:369` `PortalSessionProfile`
   - `react-ui/src/types/seller-buyer/party.d.ts:577` `PortalDirectLoginTicket`
   - `react-ui/src/types/seller-buyer/party.d.ts:592` `createTime?: string`
   - `react-ui/src/types/seller-buyer/seller.d.ts:35`
   - `react-ui/src/types/seller-buyer/seller.d.ts:37`
   - `react-ui/src/types/seller-buyer/seller.d.ts:55`
   - `react-ui/src/types/seller-buyer/seller.d.ts:56`
   - `react-ui/src/types/seller-buyer/buyer.d.ts:35`
   - `react-ui/src/types/seller-buyer/buyer.d.ts:37`
   - `react-ui/src/types/seller-buyer/buyer.d.ts:55`
   - `react-ui/src/types/seller-buyer/buyer.d.ts:56`

4. 现有 guard / tests 已覆盖关键串端与 authority fail-closed 约束
   - `react-ui/tests/three-terminal.manifest.json:91`
   - `react-ui/tests/three-terminal.manifest.json:95`
   - `react-ui/tests/three-terminal.manifest.json:96`
   - `react-ui/tests/three-terminal.manifest.json:98`
   - `react-ui/tests/three-terminal.manifest.json:105`
   - `react-ui/tests/three-terminal.manifest.json:109`
   - 已执行：
     - `node scripts/check-partner-management-template.mjs` -> passed
     - `node scripts/check-portal-token-isolation.mjs` -> passed
     - `npx jest --config jest.config.ts tests/partner-audit-modal.test.ts tests/remote-menu-route-guard.test.ts tests/getrouters-authority-contract.test.ts --runInBand` -> 3 suites / 14 tests passed

## 本次未纳入

- UI 细节
- 浏览器级验证
- P2 级别可维护性建议
