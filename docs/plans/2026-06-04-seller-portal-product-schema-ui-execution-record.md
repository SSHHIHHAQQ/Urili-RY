# 卖家端商品 Schema 前端消费模板执行记录

日期：2026-06-04

## 目标

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按“先做一套标准卖家模板，验收通过后再复制买家；每个切片只改一类东西”的节奏，只处理一类问题：

- 在当前 `react-ui/` 的卖家端工作台 `/seller/portal` 中，真实消费已落地的卖家端商品分类与 Schema 只读接口。
- 不复制买家 UI。
- 不改后端、SQL、权限 seed、数据库结构或业务数据。

## 改动范围

已完成：

- 新增 `react-ui/src/pages/Portal/Home/SellerProductSchemaPreview.tsx`：
  - 只读展示“商品发布准备”卡片。
  - 调用卖家端商品分类接口加载可发布分类。
  - 选择分类后调用卖家端 Schema 接口展示属性、类型、必填、可编辑、可筛选、分组、规则、选项来源、选项和提示。
  - 分类为空或 Schema 为空时展示 Ant Design 空状态，不使用 mock 数据。
- 更新 `react-ui/src/services/portal/session.ts`：
  - 新增 `getSellerPortalProductCategories()`。
  - 新增 `getSellerPortalProductSchema(categoryId)`。
  - 两个请求都通过 seller portal token 访问，并显式设置 `isToken: false`，避免注入管理端 token。
- 更新 `react-ui/src/types/seller-buyer/party.d.ts`：
  - 新增 seller portal 商品分类、商品属性选项和商品 Schema 响应类型。
- 更新 `react-ui/src/pages/Portal/Home/index.tsx`：
  - 仅在 `terminal === 'seller'` 时挂载 `SellerProductSchemaPreview`。
  - 买家端工作台不展示该卡片。
  - 当前账号会话表按前端请求意图只展示 5 条，避免远端历史 session 过多把本轮模板压到页面底部。
  - 修复 Ant Design v6 弃用告警：`Space` 使用 `orientation`，`Card` 使用 `variant="borderless"`，会话表使用前端生成的非敏感 `uiRowKey`。

未改动：

- 未改 `RuoYi-Vue/`。
- 未执行 DDL/DML。
- 未新增或修改端内权限 seed。
- 未复制买家商品 Schema 前端入口。
- 未做商品发布落库、SKU、价格、库存、审核或外部平台同步。

## 数据源与运行影响

运行验收前已确认：

- 后端激活配置：`druid`。
- MySQL 配置来源：`RUOYI_DB_*` 运行变量。
- Redis 配置来源：`RUOYI_REDIS_*` 运行变量。
- `.env.local` 存在但未输出明文。
- MySQL：远端/非本机。
- Redis：远端/非本机。
- 后端 `8080` 与前端 `8001` 均在监听。

运行验收影响：

- 为了在浏览器中访问 `/seller/portal` 和 `/buyer/portal`，测试脚本通过管理端生成了卖家端、买家端免密代入票据，并消费为端 token。
- 该过程会在远端库写入免密票据、登录日志、会话记录和端内操作日志。
- 验收结束后已调用 `/seller/logout` 与 `/buyer/logout` 清理本轮端 token。
- 本轮没有输出 token、密码、Redis key、Authorization header 或 `.env.local` 明文。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/pages/Portal/Home/index.tsx src/pages/Portal/Home/SellerProductSchemaPreview.tsx src/services/portal/session.ts src/types/seller-buyer/party.d.ts
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi
git diff --check -- react-ui/src/types/seller-buyer/party.d.ts react-ui/src/services/portal/session.ts react-ui/src/pages/Portal/Home/index.tsx react-ui/src/pages/Portal/Home/SellerProductSchemaPreview.tsx
```

结果：通过，仅有 LF/CRLF 提示。

浏览器验收：

- 使用 Playwright 临时运行环境，不修改项目依赖。
- 访问 `/seller/portal`：
  - “商品发布准备”卡片可见。
  - `GET /api/seller/product/categories`：HTTP 200。
  - `GET /api/seller/product/categories/446/schema`：HTTP 200。
  - Schema 表格展示 8 行。
  - 页面无横向溢出：`scrollWidth = clientWidth = 1440`。
  - 浏览器 localStorage 中无管理端 `access_token`，存在 seller 端 `seller_access_token`。
  - console error：0。
  - 截图：`output/playwright/seller-portal-product-schema-ui.png`。
- 访问 `/buyer/portal`：
  - “商品发布准备”卡片不可见。
  - console error：0。
  - 截图：`output/playwright/buyer-portal-without-product-schema-ui.png`。

## 权限检查结果

- 前端没有传 `sellerId`、`buyerId`、`subjectId`、`accountId` 决定数据范围。
- 商品分类与 Schema 请求继续由后端 `@PortalPreAuthorize(terminal = "seller", ...)` 校验 seller 端 token 与端内权限。
- 管理端 token 未注入 seller portal 请求。
- 买家端未提前复制 seller UI。

## 字典/选项复用检查结果

- 属性类型、选项来源、规则模式、分组、是否标识复用 `react-ui/src/pages/Product/constants.ts`。
- 未在新页面内另写一套商品属性 code/label 映射。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记卖家端商品 Schema 前端消费模板。
- 后续买家复制规则：只替换 terminal、路径、权限点、文案和 service，不重新设计。

## 大文件合理性判断

- `Portal/Home/index.tsx` 是既有端内工作台页面。本轮只挂载一个已拆分的 seller 商品 Schema 组件，并修复同页 Ant Design v6 告警和会话表 row key；未继续把商品表格逻辑塞进主文件。
- 新增 `SellerProductSchemaPreview.tsx` 职责单一，未达到 300 行阈值。
- `session.ts` 只新增 seller 商品只读 service，未扩展 buyer，也未新增泛化抽象。

## 重复代码检查结果

- 新 UI 的属性类型、选项来源和规则映射复用已有商品配置常量。
- 端 token 注入继续复用 `portal/session.ts` 的统一请求模式。
- 未在页面内手写 Authorization、token key 或商品 Schema 继承合并逻辑。

## 新增问题

- `ProductPortalSchemaController` 当前位于 `product` 模块但暴露 `/seller/...`、`/buyer/...` 端入口，后续如严格收敛模块边界，可单独评估迁移为 seller/buyer facade controller，再委托 product service。
- 当前权限 seed 授予 active 端内角色商品分类和 Schema 权限；如果后续需要精细授权，需要单独收敛角色策略。

## 已修复问题

- 卖家商品 Schema 前端消费模板已落地并通过浏览器验收。
- 端内工作台 Ant Design v6 弃用告警已清理。
- 当前账号会话表 row key 冲突已修复。
- 当前账号会话表只展示本次请求意图中的 5 条，避免历史会话过多影响工作台可读性。

## 残留问题

- 买家端商品 Schema 前端入口尚未复制，需等卖家模板验收后单独做 buyer 复制切片。
- 前端三端物理拆分仍未开始。
- 本轮没有新增自动化测试文件，主要通过 TypeScript、Biome 和 Playwright 运行验收覆盖。

## CodeGraph 更新结果

- 已执行：

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

- 结果：通过，CodeGraph 输出 `Synced 18 changed files`、`Added: 10, Modified: 8`。
