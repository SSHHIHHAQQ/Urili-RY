# 2026-06-05 卖家端商品前端模板契约守卫记录

## 参考方向

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，并遵守当前节奏：

- 先做一套标准卖家模板，验收通过后再复制买家。
- 每个切片只改一类东西，减少返工。
- 当前切片只补 seller portal 商品前端模板契约守卫，不复制 buyer，不改后端接口，不执行数据库 DDL/DML。

## 本轮范围

本轮只处理前端静态守卫和脚本入口：

- 新增 `react-ui/scripts/check-seller-portal-product-template.mjs`。
- 新增 `react-ui` 脚本 `guard:seller-portal-product`。
- 将 `guard:seller-portal-product` 接入 `npm run lint`。

本轮不做：

- 不复制 buyer 商品页面或 buyer 商品 service。
- 不修改 seller 商品 UI 展示、后端接口、权限 seed 或数据库数据。
- 不启动三端前端物理拆分。

## 守卫内容

`check-seller-portal-product-template.mjs` 固定以下 seller 模板契约：

- `SellerOwnDistributionProductList.tsx` 必须通过 `@/services/portal/session` 调用 seller portal 商品 service。
- 组件必须使用 `API.Partner.SellerPortalProduct` 和 `API.Partner.SellerPortalProductSku`，不得复用管理端 `API.ProductDistribution` 类型。
- 组件不得直接调用 `request(...)`，不得导入管理端 product service，不得硬编码商品 API 路径。
- `Portal/Home/index.tsx` 必须只在 `terminal === 'seller'` 分支渲染 `SellerOwnDistributionProductList`。
- `portal/session.ts` 的 seller 商品列表、详情和 SKU service 必须使用 seller portal URL，并显式设置 `isToken:false`。
- seller 商品列表 service 必须调用 `sanitizePortalQueryParams(params)`，不得透传原始 `params`。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过，`Seller portal product template guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过，`Portal token isolation guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check scripts/check-seller-portal-product-template.mjs`：通过。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 本切片相关文件尾随空白和冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，新增脚本索引输出 `Synced 1 changed files`、`Added: 1 - 24 nodes`；记录回填后再次同步输出 `Synced 2 changed files`、`Modified: 2 - 100 nodes`。

补充说明：`npx biome check scripts/check-seller-portal-product-template.mjs package.json` 中的 `package.json` 命中既有 CRLF 换行格式基线；本轮未全文件重写 `package.json` 换行，避免制造无关 diff。脚本入口已通过实际 `npm run guard:seller-portal-product` 验证。

## 当前判断

seller portal “我的商城商品”前端模板现在有独立静态守卫。后续复制 buyer 前，应先完成 seller 模板验收，再按已确认的 buyer 商品浏览口径替换 terminal、service、类型、路由和文案；如果 buyer 商品口径与 seller 商品拥有关系不同，不能机械复制。
