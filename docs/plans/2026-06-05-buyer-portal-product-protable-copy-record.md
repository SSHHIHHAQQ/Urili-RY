# 买家端商城商品 ProTable 复制记录

日期：2026-06-05

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按当前确认节奏推进：已确认模式模板化复制，卖家先做标准样板，买家只替换配置和 service。

当前只处理一类问题：将 `/buyer/portal` 的“商城商品”首版 `Card + Table + useEffect` 列表复制升级为标准 `ProTable` 模板。本轮不改后端、不改 SQL、不改权限 seed、不改买家商品浏览业务口径、不启动 `seller-ui` / `buyer-ui` 物理拆分。

## 已完成

- 更新 `react-ui/src/pages/Portal/Home/BuyerDistributionProductList.tsx`：
  - 从手写 `Card + Table + useEffect` 列表升级为标准 `ProTable`。
  - 接入 `getPersistedProTableSearch(...)` 保存筛选区展开/收起状态。
  - 接入 `getProTablePagination(...)` 和 `getProTableScroll(...)`。
  - 固定若依分页映射：`current -> pageNum`、`pageSize -> pageSize`。
  - 保留 buyer 浏览口径：只展示平台 `ON_SALE` 商品的商品信息、类目、销售价、SKU 数量、状态和详情。
  - buyer 查询只保留关键词和分页，不提供 `spuStatus`、seller 客户编码、系统编码、供货价或 sourceType 这类后台/卖家字段筛选。
  - `spuStatus` 只作为展示列，不作为 buyer 查询条件；buyer 可见性由后端 `selectOnSaleProductList(...)` 固定为 `ON_SALE`。
  - 详情弹窗继续只展示 buyer 可见字段，不展示 seller/system/供货价字段。
- 更新 `react-ui/scripts/check-buyer-portal-product-template.mjs`：
  - 增加 `ProTable`、`ProColumns`、`getPersistedProTableSearch(...)`、`getProTablePagination(...)`、`getProTableScroll(...)` 静态契约断言。
  - 增加 `pageNum: currentPage` 和 `pageSize: currentPageSize` 分页映射断言。
  - 增加 buyer 商品列表不得传 `spuStatus: params.spuStatus` 的断言，固定买家端状态筛选例外。
- 更新 `docs/architecture/reuse-ledger.md`：
  - 登记 buyer 商品 ProTable 模板和后续复用规则。
- 更新 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`：
  - 当前状态改为“端内商城商品前端工作台模板：双端 ProTable 标准模板已完成”。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:buyer-portal-product`：通过，`Buyer portal product template guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过，`Seller portal product template guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过，`Portal token isolation guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/pages/Portal/Home/BuyerDistributionProductList.tsx scripts/check-buyer-portal-product-template.mjs`：通过。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\buyer-portal-product-ui-smoke.ps1 -AdminPassword 'admin123' -ScreenshotPath 'output/playwright/buyer-portal-product-protable-smoke.png' -TimeoutMs 60000`：通过。
  - 覆盖管理端登录、buyer 免密票据生成、buyer portal 加载、buyer token storage 隔离、商城商品列表渲染、商品详情弹窗渲染和退出清理。
  - 截图保存到 `output/playwright/buyer-portal-product-protable-smoke.png`。
- `git diff --check -- react-ui/src/pages/Portal/Home/BuyerDistributionProductList.tsx react-ui/scripts/check-buyer-portal-product-template.mjs docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-buyer-portal-product-protable-copy-record.md docs/plans/2026-06-05-seller-portal-product-protable-template-record.md`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，首次输出 `Synced 1 changed files`、`Modified: 1 - 35 nodes in 461ms`；记录回填后复跑输出 `Already up to date`。

## 数据与权限影响

- 本轮未执行 SQL，未直接连接远程 MySQL / Redis。
- 浏览器 smoke 通过管理端接口生成 buyer 免密票据，并正常产生登录、会话、退出和免密票据审计记录；脚本未输出 token、directLoginToken、免密 URL、Redis key 或 `.env.local` 明文。
- 本轮不新增权限点，不修改 `buyer_menu` / `buyer_role_menu`，继续复用已落地的 `buyer:product:distribution:list` / `buyer:product:distribution:query`。

## 当前判断

- buyer portal “商城商品”主列表已经按 seller ProTable 模板完成同构复制，但业务口径仍保持 buyer 浏览：平台已上架商品只读浏览，不展示 seller 私有字段，不按 buyerId 做商品归属。
- seller/buyer 双端商品工作台现在都使用标准 ProTable、统一筛选持久化、统一分页和统一滚动配置。
- 后续如果 seller 商品模板继续变化，应先验证 seller，再按 buyer 浏览口径复制差量，不重新设计 buyer 页面。
