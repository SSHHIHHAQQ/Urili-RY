# 2026-06-08 React 管理端页面/service/route 权限工作树只读扫描

> 历史记录（已过期口径）：本文记录的是当时只读扫描发现的候选 P1。后续检查点已收口 Upstream 请求日志、Finance 币种/历史/同步日志、Product 属性库等 ProTable `current -> pageNum` 适配；当前状态以 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 较新检查点和现行代码为准，不要把本文分页 P1 作为现存阻塞项。

## 范围

- 工作区：`E:\Urili-Ruoyi`
- 扫描方式：只读扫描，不修改业务源码；仅补充本审查 Markdown
- 扫描范围：
  - `react-ui/src/pages/Seller`
  - `react-ui/src/pages/Buyer`
  - `react-ui/src/pages/Product`
  - `react-ui/src/pages/Inventory`
  - `react-ui/src/pages/Warehouse`
  - `react-ui/src/pages/Finance`
  - `react-ui/src/pages/UpstreamSystem`
  - `react-ui/src/components/PartnerManagement`
  - `react-ui/src/services`
  - `react-ui/config/routes.ts`
  - `react-ui/config/routes.js`
  - `react-ui/tests/*permission*`
- 目标：仅找 P0/P1
  - 页面调用错端 service
  - 权限点缺失或 admin/portal namespace 混用
  - routes authority 空权限放行
  - JS mirror 漂移
  - TS/JS 双入口不一致导致运行旧逻辑
  - 筛选/字段缺失导致接口参数错误

## 结论

- **P0：未发现**
- **P1：发现 3 组前端分页参数契约缺口**
- 其余已抽查的高风险面未发现当前工作树 P0/P1：
  - Seller/Buyer 页面未调用错端 service
  - admin/portal namespace 未发现串用
  - `RemoteMenuRouteGuard` 仍对空 `authority` fail-closed
  - `routes.ts` / `routes.js`、`access.ts/js`、`session.ts` 相关守卫合同测试通过
  - Seller/Buyer、Warehouse、Inventory SourceWarehouseStock、Product Distribution、核心 route guard 的 JS/TS 入口合同测试通过

## P1 发现

### 1. Upstream 请求日志页把 ProTable 原始分页参数直接透传给后端

- 严重级别：P1
- 风险：
  - 若后端仍按若依 `/list` 契约只认 `pageNum/pageSize`，则请求日志分页会退回默认第一页或分页失效
  - 症状更像“日志页数据不对/翻页无效”，但根因在前端参数契约
- 证据：
  - `react-ui/src/pages/UpstreamSystem/components/SyncTabs.tsx:448-453`
    - `request={async (params) => { ... getRequestLogList(requestCode, params) ... }}`
  - `react-ui/src/services/integration/upstreamSystem.ts:218-221`
    - `getRequestLogList(...)` 直接 `params`
  - 对比同页其他 tab：
    - `SkuSyncPanel.tsx`、`SkuDimensionPanel.tsx`、`SkuInventoryPanel.tsx` 都有显式 `pageNum: params.current, pageSize: params.pageSize`
- 最小修复建议：
  - 在 `SyncTabs.tsx` 请求日志 tab 内改为显式转换：
    - `const { current, pageSize, ...rest } = params`
    - `getRequestLogList(requestCode, { ...rest, pageNum: current, pageSize })`
  - 或在 `getRequestLogList` service 层统一做若依分页适配，但要保持同类 service 风格一致

### 2. Finance 币种列表/汇率历史/同步日志 3 处都直接透传 ProTable `params`

- 严重级别：P1
- 风险：
  - 币种列表、汇率历史抽屉、同步日志表格都可能出现分页失效
  - 这是同一类 contract 漏洞，影响面比单点更大
- 证据：
  - `react-ui/src/pages/Finance/Currency/index.tsx:323-324`
    - `getCurrencyList(params)`
  - `react-ui/src/pages/Finance/Currency/index.tsx:464-467`
    - `getRateHistoryList(historyCurrency.currencyCode, params)`
  - `react-ui/src/pages/Finance/Currency/components/SyncSettingsPanel.tsx:270-271`
    - `getSyncLogList(params)`
  - `react-ui/src/services/finance/currency.ts:5-8`
  - `react-ui/src/services/finance/currency.ts:56-63`
  - `react-ui/src/services/finance/currency.ts:94-98`
    - 以上 3 个 service 都只是 `params` 直传，没有 `current -> pageNum` 适配
- 最小修复建议：
  - 列表页、历史抽屉、同步日志请求统一改为显式拆出 `current/pageSize`
  - 若考虑统一治理，可在 `finance/currency.ts` 增加局部 `withRuoYiPage(...)` 适配器，风格可参照 `react-ui/src/services/warehouse/warehouse.ts`

### 3. Product 属性库列表直接透传 ProTable `params`

- 严重级别：P1
- 风险：
  - 属性库列表页如果后端走标准若依分页契约，会出现分页参数错误
  - 该页属于 `Product` 范围内真实业务页，不是低频工具页
- 证据：
  - `react-ui/src/pages/Product/Attribute/components/AttributeLibrary.tsx:385-386`
    - `const resp = await getAttributeList(params);`
  - `react-ui/src/services/product/product.ts:109-112`
    - `getAttributeList(...)` 直接 `params`
  - 对比同范围内已正确适配的页面：
    - `react-ui/src/pages/Product/Distribution/index.tsx`
    - `react-ui/src/pages/Product/Review/index.tsx`
    - `react-ui/src/pages/Product/SourceProductLibrary/index.tsx`
    - 这些页面都已显式转换为 `pageNum/pageSize`
- 最小修复建议：
  - 在 `AttributeLibrary.tsx` 的 `request` 中改为：
    - `const { current, pageSize, ...rest } = params`
    - `getAttributeList({ ...rest, pageNum: current, pageSize })`

## 未发现的高风险项

### 1. 页面调用错端 service

- `react-ui/src/pages/Seller/index.tsx` 仅接 `@/services/seller/seller`
- `react-ui/src/pages/Buyer/index.tsx` 仅接 `@/services/buyer/buyer`
- `react-ui/src/pages/Product/Distribution/*.tsx` 管理端卖家查询使用 `getAdminSellerList`
- 未发现 Seller 页面误接 Buyer service，或 Buyer 页面误接 Seller service

### 2. admin / portal namespace 混用

- 扫描范围内管理端页使用的权限前缀主要为：
  - `seller:admin:*`
  - `buyer:admin:*`
  - `product:*`
  - `inventory:*`
  - `warehouse:*`
  - `finance:*`
  - `integration:upstream:*`
- 未发现管理端 Product/Warehouse/Finance/Upstream 页面混入 portal 登录态权限前缀

### 3. routes authority 空权限放行

- `react-ui/src/services/session.ts` 中 `RemoteMenuRouteGuard` 仍要求：
  - `permissions.length > 0`
  - 权限不为空才允许渲染
- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx` 仍为 seller/buyer/product 等静态路由补 fallback authority
- 未发现空 `authority` 被放行

### 4. TS/JS 双入口关键合同

- 已通过合同测试确认以下关键入口仍是受控状态：
  - `react-ui/config/routes.js`
  - `react-ui/src/access.js`
  - `react-ui/src/wrappers/RemoteMenuRouteGuard.js`
  - Seller/Buyer 页面 JS mirror
  - Warehouse / Finance / SourceWarehouseStock / Product Distribution 关键 JS mirror
- `UpstreamSystem` 目录下仍存在少量编译态 `.js` 副本不是 pure re-export，但本次抽查的权限逻辑与 `.tsx` 源一致，当前未形成新的 P0/P1 证据

## 测试与命令

### 读取/扫描命令

```powershell
rg --files react-ui/src/pages/... react-ui/src/components/PartnerManagement react-ui/src/services react-ui/tests
rg -n "authority:|wrappers:|seller:admin:|buyer:admin:|pageNum:|getCurrencyList\(params\)|getRequestLogList\(requestCode, params\)|getAttributeList\(params\)" react-ui/src/pages react-ui/src/services react-ui/tests
```

### 测试结果

1. 直接跑 `npm test -- --runInBand ...` 失败
   - 原因：仓库脚本 `scripts/verify-three-terminal.mjs` 不接受 case 路径透传，只允许 `--coverage` / `-u`
   - 这不是本次扫描发现的业务问题，是测试入口约束

2. 直接调用底层 Jest，相关合同测试通过

```powershell
E:\Urili-Ruoyi\react-ui\node_modules\.bin\jest.cmd --config jest.config.ts --runInBand tests/remote-menu-route-guard.test.ts tests/admin-auth-sidecar-contract.test.ts tests/warehouse-permission-guard.test.ts tests/source-warehouse-stock-contract.test.ts tests/finance-currency-contract.test.ts tests/upstream-system-permission-guard.test.ts tests/product-distribution-permission-guard.test.ts
```

- 结果：`7 suites passed, 57 tests passed`

```powershell
E:\Urili-Ruoyi\react-ui\node_modules\.bin\jest.cmd --config jest.config.ts --runInBand tests/permission-contract.test.ts tests/partner-audit-modal.test.ts tests/getrouters-authority-contract.test.ts
```

- 结果：`3 suites passed, 9 tests passed`

### 当前测试缺口

- 本次已通过的合同测试覆盖了：
  - route guard
  - authority fail-closed
  - seller/buyer namespace 隔离
  - 多个 JS/TS sidecar 合同
- **未覆盖本次 3 组分页参数契约缺口**
  - `Finance/Currency`
  - `Product/Attribute`
  - `UpstreamSystem` 请求日志

## 备注

- 本工作树当前有大量已有改动和未跟踪文件；本次扫描未回滚、未整理、未覆盖他人改动
- 本次仅新增本 Markdown 审查记录
