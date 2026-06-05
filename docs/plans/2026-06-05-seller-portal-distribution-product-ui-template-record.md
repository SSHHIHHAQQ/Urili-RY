# 2026-06-05 卖家端我的商城商品前端工作台模板记录

## 参考方向

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，并遵守当前节奏：

- 先做一套标准卖家模板，验收通过后再复制买家。
- 每个切片只改一类东西，减少返工。
- 当前阶段仍以 `react-ui/` 作为管理端和端内验证入口，不启动 `seller-ui` / `buyer-ui` 物理拆分。

## 本轮范围

本轮只做 seller 端“我的商城商品”前端工作台只读接入：

- seller portal 商品列表 service。
- seller portal 商品详情和 SKU service。
- seller portal 工作台只读商品列表卡片。
- seller portal 商品详情弹窗。
- 类型声明、复用台账和目标追踪记录。

本轮不做：

- 不复制 buyer。
- 不新增或修改后端接口。
- 不执行远程数据库 DDL/DML。
- 不修改 seller/buyer 权限 seed。
- 不新增管理端商品页面。

## 已完成

- 更新 `react-ui/src/types/seller-buyer/party.d.ts`，新增 seller 端商品只读 DTO 类型：
  - `SellerPortalProduct`
  - `SellerPortalProductSku`
  - `SellerPortalProductPageResult`
  - `SellerPortalProductInfoResult`
  - `SellerPortalProductSkuListResult`
- 更新 `react-ui/src/services/portal/session.ts`，新增 seller 端商品只读 service：
  - `getSellerPortalDistributionProducts`
  - `getSellerPortalDistributionProduct`
  - `getSellerPortalDistributionProductSkus`
- 新增 `react-ui/src/pages/Portal/Home/SellerOwnDistributionProductList.tsx`：
  - 在 seller portal 工作台展示“我的商城商品”表格。
  - 支持分页、刷新、详情弹窗和 SKU 列表。
  - 只读展示 seller DTO 字段，不展示 `sellerId`、系统 SPU/SKU、token 或后台审计字段。
- 更新 `react-ui/src/pages/Portal/Home/index.tsx`：
  - 仅当 `terminal === 'seller'` 时展示“我的商城商品”卡片。
  - buyer portal 不展示该卡片，本轮不复制 buyer。

## 模板规则

- seller portal 商品请求必须通过 `react-ui/src/services/portal/session.ts`，继续使用 seller token，并显式 `isToken: false`。
- seller portal 商品列表参数必须经过 `sanitizePortalQueryParams(...)`，过滤 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId` 和 `terminal` 等客户端身份范围字段。
- 前端只负责展示和分页，不传 seller 身份范围；真实数据范围仍以后端 `PortalLoginSession.subjectId` 为准。
- seller 响应类型独立于管理端 `API.ProductDistribution.Spu`，避免把管理端字段作为 seller 端 API 标准。
- buyer 后续不能直接按商品拥有关系复制；需要先确认买家商品浏览可见性、上架状态、价格口径和库存边界。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过，`Portal token isolation guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过，`tsc --noEmit` 无错误。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check src/pages/Portal/Home/SellerOwnDistributionProductList.tsx src/services/portal/session.ts src/types/seller-buyer/party.d.ts`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check src/pages/Portal/Home/SellerOwnDistributionProductList.tsx src/pages/Portal/Home/index.tsx src/services/portal/session.ts src/types/seller-buyer/party.d.ts`：未作为本轮通过项；`Portal/Home/index.tsx` 存在大量既有格式化差异，已避免整文件格式化造成无关 diff。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白和冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 6 changed files`，`Added: 1, Modified: 5 - 210 nodes`。

## 浏览器验收

- 当前后端 `8080` 和前端 `8001` 均已监听。
- 使用管理端登录生成 sellerId=5 的免密登录票据进入 `http://127.0.0.1:8001/seller/portal`；本记录不输出 admin token、directLoginToken、seller token 或登录 URL。
- 浏览器 DOM 验证：
  - 当前 URL 为 `/seller/portal`。
  - 页面出现“卖家端”。
  - 页面出现“商品发布准备”。
  - 页面出现新增“我的商城商品”。
  - 商品列表出现“客户SPU”和商品列。
- 点击商品行“详情”后：
  - 弹窗出现“商品详情”。
  - SKU 表出现“客户SKU”和“SKU规格”。
  - 详情展示“商品状态”。
- 截图检查未发现新增卡片或详情弹窗明显遮挡、错位。
- 浏览器控制台错误检查：无 error / warning / warn 记录。
- 验收结束后通过 UI 点击“退出”，页面回到 `/user/login`，本次 seller token 已由前端 logout 流程清理。

## 当前判断

seller 端“我的商城商品”已经具备后端只读模板、HTTP 烟测脚本和 seller portal 前端工作台只读接入。当前仍未复制 buyer；后续需要先完成 seller 模板验收，再决定买家商品浏览口径并按同构方式推进。
