# 商品审核 Slice A 只读 P0/P1 审查

日期：2026-06-09
模型：`gpt-5.4`
模式：只读审查，不修改业务代码

> 2026-06-09 记录层 P1 修正：本文是只读审查的历史发现记录，文中的 2 条 P1 已在同日后续快速推进检查点完成修复，并由 `react-ui/tests/product-distribution-permission-guard.test.ts` 和 `npm run verify:three-terminal` 固定。后续不要把本文的 `P1：2 条` 当作当前开放阻塞；当前状态以 `docs/plans/2026-06-09-three-terminal-p0p1-gpt54-fast-pass-record.md` 和 `docs/plans/2026-06-08-product-review-implementation-record.md` 的最新检查点为准。

## 范围

- `react-ui/src/pages/Product/Review/components/ProductReviewBusinessPreview.tsx`
- `react-ui/tests/product-distribution-permission-guard.test.ts`
- `docs/plans/2026-06-08-product-review-implementation-record.md`
- 对照 `AGENTS.md`
- 对照 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 结论

- `P0`：未发现
- `P1`：2 条（历史发现，已关闭）
- seller/buyer 端入口串入：admin 审核页本轮未发现新的 seller/buyer 继续编辑入口回流
- DTO 字段泄露：本次 dirty diff 未发现新的 seller/buyer portal DTO 泄露证据；当前看到的 `sellerId`、`sellerName`、`submitSubjectId`、`submitAccountId` 仍停留在管理端商品审核 DTO 语境内

## P1 Findings

### 1. 审核详情预览新增了未被 review 权限合同覆盖的 admin 类目 schema 依赖

- 风险级别：`P1`
- 证据：
  - [react-ui/src/pages/Product/Review/components/ProductReviewBusinessPreview.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Product\Review\components\ProductReviewBusinessPreview.tsx:1590) 在详情预览 `useEffect` 中，只要存在 `categoryId` 就会调用 `getCategorySchema(categoryId, { skipErrorHandler: true })`
  - [react-ui/src/services/product/product.ts](E:\Urili-Ruoyi\react-ui\src\services\product\product.ts:13) `getCategorySchema` 命中的是管理端接口基座 `'/api/product/admin'`
  - [react-ui/src/pages/Product/Review/index.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Product\Review\index.tsx:790) 到 [react-ui/src/pages/Product/Review/index.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Product\Review\index.tsx:794) 审核页当前只显式 gate 了 `review:productDistribution:*` 与日志权限，没有同步声明 `product:categoryAttribute:preview`
  - [react-ui/tests/product-distribution-permission-guard.test.ts](E:\Urili-Ruoyi\react-ui\tests\product-distribution-permission-guard.test.ts:287) 只固定“组件会调用 schema 接口”，没有固定“审核页/详情入口也必须具备该依赖权限”或“缺权限时 fail-closed 降级”
- 影响：
  - 管理端用户如果只有商品审核查看权限、没有类目属性预览权限，列表可进、详情抽屉可开，但详情预览内部会额外打 admin schema API，运行时容易出现 403 或静默降级。
  - 这会把 admin review-only 页变成“隐藏依赖更多商品维护权限”的页面，违反最小权限和可预期权限合同。
- 最小修复建议：
  - 给 `ProductReviewBusinessPreview` 注入 `canPreviewCategorySchema`，无权限时不要发请求，直接退回 code 展示或空 schema。
  - 或者把商品审核详情路由/按钮权限合同补齐为同时要求 `product:categoryAttribute:preview`，但这会扩大查看审核所需权限，优先级低于前一种。
  - 测试侧补一条：审核页若依赖 schema，则必须同时固定权限 gate；若不依赖权限，则必须固定无权限时不请求该接口。

### 2. dirty change 把 `EDIT_PRICE` 审核口径并回 `SKU_INFO`，测试和实施记录一起固化了 review-only 语义回退

- 风险级别：`P1`
- 证据：
  - [react-ui/src/pages/Product/Review/components/ProductReviewBusinessPreview.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Product\Review\components\ProductReviewBusinessPreview.tsx:1191) `getSkuInfoChangedFields` 不再排除 `supplyPrice`
  - [react-ui/src/pages/Product/Review/components/ProductReviewBusinessPreview.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Product\Review\components\ProductReviewBusinessPreview.tsx:1496) 到 [react-ui/src/pages/Product/Review/components\ProductReviewBusinessPreview.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Product\Review\components\ProductReviewBusinessPreview.tsx:1500) 纯供货价变更会同时命中 `SKU_INFO` 和 `SUPPLY_PRICE`
  - [react-ui/src/pages/Product/Review/components/ProductReviewBusinessPreview.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Product\Review\components\ProductReviewBusinessPreview.tsx:1533) 到 [react-ui/src/pages/Product/Review/components/ProductReviewBusinessPreview.tsx](E:\Urili-Ruoyi\react-ui\src\pages\Product\Review\components\ProductReviewBusinessPreview.tsx:1546) 删除了独立价格视图后，`EDIT_PRICE` 会落入通用 `SKU 资料左右对比`
  - [react-ui/tests/product-distribution-permission-guard.test.ts](E:\Urili-Ruoyi\react-ui\tests\product-distribution-permission-guard.test.ts:272) 到 [react-ui/tests/product-distribution-permission-guard.test.ts](E:\Urili-Ruoyi\react-ui\tests\product-distribution-permission-guard.test.ts:355) 反向固定“不能有 `PriceChangeReviewView`、`supplyPrice` 必须属于 SKU 资料对比”
  - [docs/plans/2026-06-08-product-review-implementation-record.md](E:\Urili-Ruoyi\docs\plans\2026-06-08-product-review-implementation-record.md:55) 与 [docs/plans/2026-06-08-product-review-implementation-record.md](E:\Urili-Ruoyi\docs\plans\2026-06-08-product-review-implementation-record.md:68) 明确写的是“管理端商品审核列表只负责查看、通过和驳回”，强调 admin review-only，不承载卖家继续编辑流程；但 [docs/plans/2026-06-08-product-review-implementation-record.md](E:\Urili-Ruoyi\docs\plans\2026-06-08-product-review-implementation-record.md:232) 到 [docs/plans/2026-06-08-product-review-implementation-record.md](E:\Urili-Ruoyi\docs\plans\2026-06-08-product-review-implementation-record.md:241) 又把供货价审核重新并回 SKU 资料语义
- 影响：
  - `EDIT_PRICE` 的审核焦点不再独立，审核员看到的是 SKU 资料视图，review type 与首屏审核语义不再一一对应。
  - 后续如果 seller 端继续编辑、待审锁定、驳回稿处理继续按 `EDIT_PRICE` 单独流转，前端详情会先把价格变更误混成“SKU 资料变更”，容易造成审核日志、培训口径、回归测试三边漂移。
- 最小修复建议：
  - 恢复 `supplyPrice` 不参与 `getSkuInfoChangedFields` 的规则，保留独立的供货价审核主视图；SKU 卡片里可以展示价格标签，但不要把 `EDIT_PRICE` 首屏语义并回 `SKU_INFO`
  - 如果产品决定永久合并口径，也要同步改后端 review type 命名、实施记录和回归测试命名，避免继续叫 `EDIT_PRICE` 却展示成“SKU 资料”
  - 测试侧至少拆开两条断言：`EDIT_SKU_INFO` 字段集合同、`EDIT_PRICE` 独立审核视图合同，避免一个快照测试同时把两个语义绑死

## 未发现的项

- 未看到管理端商品审核页重新挂回卖家 `继续编辑`、`reviewId` 深链或 seller/buyer portal 登录入口
- 未看到 buyer 端权限前缀混入该组件或该测试
- 未看到新的 portal 自助 DTO、direct-login 审计字段向 seller/buyer 自助接口扩散

## 已执行命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc -- --pretty false
npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand
```

结果：

- `tsc`：通过
- `jest tests/product-distribution-permission-guard.test.ts`：通过，`1 suite / 10 tests`

## 建议补跑命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand
rg -n "categoryAttribute:preview|getCategorySchema|PriceChangeReviewView|supplyPrice" src/pages/Product/Review tests/product-distribution-permission-guard.test.ts
```

如果进入修复模式，再补：

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc -- --pretty false
npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand
```
