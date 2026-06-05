# 买家端商城商品前端工作台复制记录

日期：2026-06-05

## 目标

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`、`docs/plans/2026-06-05-buyer-product-browse-copy-boundary-plan.md`、`docs/plans/2026-06-05-buyer-distribution-product-read-template-record.md` 和 `docs/plans/2026-06-05-buyer-product-permission-dml-smoke-record.md` 为参考方向。

当前只做 buyer 前端工作台复制和验收，不做后端，不执行 DDL/DML，不重复补远程权限。

执行原则：

- 先以 seller portal 商品卡片作为标准模板。
- buyer 只替换 terminal、service、路由、DTO、文案和 smoke 断言。
- buyer 商品浏览不是“自有商品”，不展示 seller 内部字段、系统编码、供货价或后台审计字段。

## 已完成

- 新增 buyer 商品工作台卡片：
  - `react-ui/src/pages/Portal/Home/BuyerDistributionProductList.tsx`
- 在 buyer portal 首页接入商品卡片：
  - `react-ui/src/pages/Portal/Home/index.tsx`
- 新增 buyer 商品 portal service：
  - `getBuyerPortalDistributionProducts`
  - `getBuyerPortalDistributionProduct`
  - `getBuyerPortalDistributionProductSkus`
- 新增 buyer 商品 DTO 类型：
  - `BuyerPortalProduct`
  - `BuyerPortalProductSku`
  - `BuyerPortalProductPageResult`
  - `BuyerPortalProductInfoResult`
  - `BuyerPortalProductSkuListResult`
- 新增 buyer 商品前端模板契约守卫：
  - `react-ui/scripts/check-buyer-portal-product-template.mjs`
  - `react-ui/package.json` 新增 `guard:buyer-portal-product`
- 更新 portal token/query 隔离守卫：
  - `getBuyerPortalDistributionProducts` 纳入 `sanitizePortalQueryParams(params)` 检查。
- 新增 buyer 商品前端浏览器 smoke：
  - `scripts/smoke/buyer-portal-product-ui-smoke.ps1`
  - `scripts/smoke/buyer-portal-product-ui-smoke.mjs`

## 模板规则

- buyer 卡片标题使用“商城商品”，不使用“我的商城商品”。
- buyer 页面展示销售价和币种，不展示供货价。
- buyer 表格不展示 `sellerSpuCode` / `sellerSkuCode` / `systemSpuCode` / `systemSkuCode`。
- buyer service 必须使用 `buildPortalUrl('buyer', ...)` 和 `buildPortalAuthHeaders('buyer')`。
- buyer 列表请求必须走 `sanitizePortalQueryParams(params)`，不得从前端传 `buyerId`、`subjectId`、`accountId`、`terminal` 决定范围。
- buyer 组件不得直接调用 `request(...)`，不得导入管理端 product service，不得复用 seller DTO 或管理端 `API.ProductDistribution` 作为端内 API 标准。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:buyer-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check ..\scripts\smoke\buyer-portal-product-ui-smoke.mjs`：通过。
- PowerShell 解析 `scripts/smoke/buyer-portal-product-ui-smoke.ps1`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/pages/Portal/Home/BuyerDistributionProductList.tsx src/pages/Portal/Home/index.tsx src/services/portal/session.ts src/types/seller-buyer/party.d.ts scripts/check-buyer-portal-product-template.mjs scripts/check-portal-token-isolation.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\buyer-portal-product-ui-smoke.ps1 -BuyerId 2 -TimeoutMs 45000`：通过，覆盖管理端生成 buyer 免密票据、buyer direct-login、buyer portal、token storage 隔离、商品卡片、详情弹窗和退出清理。
- Browser 插件内置浏览器轻量检查：通过，`/buyer/portal` 可见“买家端”“商品浏览准备”“商城商品”和“详情”。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 本切片相关文件尾随空白和冲突标记检查：通过。
- 敏感明文检查：未发现真实连接串、Bearer 明文、免密 token 明文或 JSON token/loginUrl 明文；命中项仅为文档中的“不得输出/省略号示例/环境变量名”说明。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，先输出 `Synced 23 changed files`、`Added: 17, Modified: 6 - 663 nodes in 1.5s`；记录回填后复跑输出 `Synced 2 changed files`、`Added: 2 - 34 nodes in 516ms`。

## 未作为通过项

- `cd E:\Urili-Ruoyi\react-ui; npm run lint`：未作为本切片通过项。三个 guard 均通过，但全量 `biome:lint` 命中既有无关文件问题，例如 `IconSelector`、`DictTag`、`Druid iframe` 等。本切片只按小步边界验证新增和相关文件，不修复无关历史 lint。

## 检查清单

- 新增问题：第一次 buyer 浏览器 smoke 暴露 `Table rowKey` 使用 antd 已废弃 `index` 参数；已改为稳定 `uiRowKey`。
- 已修复问题：buyer 前端缺少商品卡片、buyer 商品 service、buyer DTO、buyer 商品模板 guard 和 buyer 浏览器 smoke。
- 残留问题：真实不可见 SPU 样本负向仍只在后端 smoke 中用固定不存在商品覆盖；buyer 前端暂不做购物车、下单、库存承诺或客户专属价格。
- 验证命令：已记录在“验证结果”。
- 未验证原因：未执行全量 `npm run build`，因为当前切片只做工作台验证，且 build 会写入 `dist`；未修复全量 Biome 历史问题。
- 权限检查结果：前端通过 buyer portal service 调用 `buyer:product:distribution:list/query` 对应接口；不新增权限，不重复执行 DML。
- 字典/选项复用检查结果：商品状态展示复用 `getSalesStatusText`，本切片不新增字典。
- 复用台账检查结果：待回填 `docs/architecture/reuse-ledger.md`。
- CodeGraph 更新结果：已执行 `codegraph sync .`，先输出 `Synced 23 changed files`、`Added: 17, Modified: 6 - 663 nodes in 1.5s`；记录回填后复跑输出 `Synced 2 changed files`、`Added: 2 - 34 nodes in 516ms`。
- 大文件合理性判断结果：`BuyerDistributionProductList.tsx` 超过 300 行，当前职责仍单一，属于 seller 模板复制后的卡片组件；后续 seller/buyer 商品卡片继续分化或重复增加时再抽公共端内只读商品列表组件。
- 重复代码检查结果：本轮按已确认“seller 模板先验收，再复制 buyer”推进；复制只替换 terminal、service、路由、DTO 和断言文本，未重新设计页面结构。
