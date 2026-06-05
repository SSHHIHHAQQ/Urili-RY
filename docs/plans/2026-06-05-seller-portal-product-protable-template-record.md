# 卖家端商品 ProTable 标准模板记录

## 背景

本记录服务于 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 的三端独立改造目标，并执行当前确认节奏：先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西。

本切片只整理 seller portal “我的商城商品”主列表模板。buyer 前端不在本轮复制，避免 seller 模板未验收时双端同时返工。

## 已处理

- `SellerOwnDistributionProductList.tsx` 从手写 `Card + Table + useEffect` 列表整理为标准 `ProTable`。
- seller 商品主列表统一接入：
  - `getPersistedProTableSearch(...)`
  - `getProTablePagination(...)`
  - `getProTableScroll(...)`
- ProTable request 固定若依分页映射：`current -> pageNum`、`pageSize -> pageSize`。
- 查询参数仅保留业务筛选字段，继续通过 portal service 清理 `sellerId`、`buyerId`、`subjectId`、`accountId`、`terminal` 等客户端身份范围字段。
- `check-seller-portal-product-template.mjs` 增加 ProTable、统一筛选、统一分页、统一滚动和分页映射守卫。
- `docs/architecture/reuse-ledger.md` 已登记 seller 商品主列表 ProTable 模板和 buyer 后续复制边界。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run guard:seller-portal-product
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run guard:buyer-portal-product
```

结果：通过，确认本轮没有破坏 buyer 既有商品浏览模板。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run guard:partner-management
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run guard:portal-token
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc -- --pretty false
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/pages/Portal/Home/SellerOwnDistributionProductList.tsx scripts/check-seller-portal-product-template.mjs
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run lint
```

结果：未通过。失败集中在既有 Biome lint 问题，涉及 `DictTag`、`RightContent`、`utils/tree`、`IconSelector`、`Monitor` 等非本切片修改文件；本切片修改文件定向 lint 已通过。

```powershell
cd E:\Urili-Ruoyi
.\scripts\smoke\seller-portal-product-ui-smoke.ps1 -AdminPassword 'admin123' -ScreenshotPath 'output/playwright/seller-portal-product-protable-smoke.png' -TimeoutMs 60000
```

结果：通过。真实浏览器验收覆盖管理端登录、seller 免密票据、seller portal 跳转、seller token storage 隔离、“我的商城商品”列表、详情弹窗、字段脱敏和退出清理；截图保存到 `output/playwright/seller-portal-product-protable-smoke.png`。

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：通过，CodeGraph 同步 `9 changed files`。

## 当前判断

- seller portal “我的商城商品”主列表已经形成当前标准模板，并已通过真实浏览器 smoke，可以进入人工验收。
- buyer 在本切片当时仍保留此前已验收的首版商品浏览工作台；后续已通过 `docs/plans/2026-06-05-buyer-portal-product-protable-copy-record.md` 按 buyer 浏览口径完成 ProTable 差量复制。
- seller 模板验收通过后，再按 buyer 已确认的浏览口径复制：只替换 terminal、文案、service、DTO、路由和 guard，不重新设计页面。
- 本轮未执行 SQL，未连接远程 MySQL / Redis。
