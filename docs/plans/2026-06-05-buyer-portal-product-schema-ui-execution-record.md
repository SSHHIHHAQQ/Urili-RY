# 买家端商品 Schema 前端消费复制执行记录

日期：2026-06-05

## 目标

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：

- 把已验收的 seller 商品 Schema 前端消费模板复制到 `/buyer/portal`。
- buyer 只替换 terminal、路径、权限点、文案和 service。
- 不改后端、不新增 DDL/DML、不改权限 seed、不推进三端物理拆分。

## 改动范围

已完成：

- 新增 `react-ui/src/pages/Portal/Home/BuyerProductSchemaPreview.tsx`：
  - 作为买家端商品 Schema 前端消费入口。
  - 使用“商品浏览准备”文案。
  - 注入 buyer 商品分类和 Schema service。
- 更新 `react-ui/src/pages/Portal/Home/SellerProductSchemaPreview.tsx`：
  - 保留 `SellerProductSchemaPreview` seller 包装入口。
  - 抽出 `PortalProductSchemaPreview` 共用预览模板，避免复制整份表格、加载状态、空状态和字段映射逻辑。
- 更新 `react-ui/src/services/portal/session.ts`：
  - 新增 `getBuyerPortalProductCategories()`。
  - 新增 `getBuyerPortalProductSchema(categoryId)`。
  - 两个请求均通过 buyer portal token 访问，并显式设置 `isToken: false`，避免注入管理端 token。
- 更新 `react-ui/src/pages/Portal/Home/index.tsx`：
  - seller 继续挂载 `SellerProductSchemaPreview`。
  - buyer 挂载 `BuyerProductSchemaPreview`。
- 更新 `docs/architecture/reuse-ledger.md`：
  - 登记 buyer 商品 Schema 前端消费入口。
  - 将 seller/buyer 工作台商品 Schema 卡片状态更新为已对齐。
- 更新 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`：
  - 更新当前状态。
  - 删除“买家端商品 Schema 前端消费未复制”的残留项。
  - 追加本轮检查点。

未改动：

- 未改 `RuoYi-Vue/` 后端代码。
- 未执行 DDL/DML。
- 未新增或修改权限 seed。
- 未改 `react-ui/config/routes.ts`、`react-ui/src/app.tsx`、`react-ui/src/pages/Portal/terminal.ts` 或 token storage 规则。
- 未复制 `seller-ui` / `buyer-ui` 物理前端目录。

## 数据源与运行影响

运行验收前已确认：

- 后端激活配置：`druid`。
- MySQL 配置来源：`RUOYI_DB_*` 运行变量。
- Redis 配置来源：`RUOYI_REDIS_*` 运行变量。
- 后端 `8080` 与前端 `8001` 均在监听。
- `.env.local` 未输出明文。

运行验收影响：

- 本轮浏览器验收通过管理端生成 seller/buyer 免密票据，并消费为端 token。
- 商品分类和 Schema 是 GET 请求，但端内接口带 `@PortalLog`，会写入 seller/buyer 端操作日志。
- 成功验收结束后已调用 `/seller/logout` 与 `/buyer/logout` 清理本轮新 token。
- 第一次浏览器脚本因中文字符串在 PowerShell 管道中被转码为问号而中断；该次已消费的端 token 未输出，未使用管理端强制踢出，避免误伤真实在线会话，等待其自然过期。
- 本轮没有输出 token、密码、Redis key、Authorization header、直登 URL 或 `.env.local` 明文。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/pages/Portal/Home/index.tsx src/pages/Portal/Home/SellerProductSchemaPreview.tsx src/pages/Portal/Home/BuyerProductSchemaPreview.tsx src/services/portal/session.ts src/types/seller-buyer/party.d.ts
```

结果：通过。

浏览器验收：

- `/seller/portal`：
  - “商品发布准备”卡片可见。
  - `GET /api/seller/product/categories`：HTTP 200。
  - `GET /api/seller/product/categories/446/schema`：HTTP 200。
  - Schema 表格展示 8 行。
  - 页面无横向溢出：`scrollWidth = clientWidth = 1440`。
  - 浏览器 localStorage 中无管理端 `access_token`。
  - console error：0。
  - page error：0。
  - 截图：`output/playwright/seller-portal-product-schema-ui-regression.png`。
- `/buyer/portal`：
  - “商品浏览准备”卡片可见。
  - 不显示“商品发布准备”卡片。
  - `GET /api/buyer/product/categories`：HTTP 200。
  - `GET /api/buyer/product/categories/446/schema`：HTTP 200。
  - Schema 表格展示 8 行。
  - 页面无横向溢出：`scrollWidth = clientWidth = 1440`。
  - 浏览器 localStorage 中无管理端 `access_token`。
  - console error：0。
  - page error：0。
  - 截图：`output/playwright/buyer-portal-product-schema-ui.png`。

## 权限检查结果

- 前端没有传 `sellerId`、`buyerId`、`subjectId` 或 `accountId` 决定分类或 Schema 数据范围。
- buyer 商品分类和 Schema 请求通过 buyer portal token 调用，并显式 `isToken: false`。
- 管理端 token 未注入 buyer portal 商品请求。
- 后端 buyer 商品分类和 Schema 接口已存在，并由 `@PortalPreAuthorize(terminal = "buyer", ...)` 校验端 token 和端内权限。

## 字典/选项复用检查结果

- 属性类型、选项来源、规则模式、分组、是否标识继续复用 `react-ui/src/pages/Product/constants.ts`。
- 分类下拉继续复用 `SEARCHABLE_SELECT_PROPS`，保持可模糊搜索。
- 未在 buyer 组件内新写一套商品属性 code/label 映射。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`。
- seller/buyer 商品 Schema 前端消费模板已登记为已对齐。

## 大文件合理性判断结果

- `SellerProductSchemaPreview.tsx` 接近 300 行阈值，但职责仍单一：承载一个端内商品 Schema 只读卡片模板和 seller 包装入口。
- 本轮没有复制整份表格逻辑到 buyer，而是新增 `BuyerProductSchemaPreview.tsx` 薄包装，降低重复维护成本。
- 后续若继续扩展端内商品卡片，应考虑把 `PortalProductSchemaPreview` 迁移到中性文件名，避免公共组件长期挂在 seller 命名文件下；本轮不顺手改名，避免扩大切片。

## 重复代码检查结果

- buyer 组件只做 title 和 service 注入，没有复制表格列、加载逻辑和字段映射。
- seller/buyer service 暂按端显式函数暴露，保持与当前 service 风格一致。
- 如后续要统一到 `PORTAL_SERVICE[terminal].getProductCategories`，应另起 service surface 统一切片，不在本轮混做。

## 新增问题

- 第一次浏览器验收脚本因中文字符串在 PowerShell 管道中转码失败而中断。后续已改用 Unicode 转义重跑成功。
- `PortalProductSchemaPreview` 当前从 `SellerProductSchemaPreview.tsx` 导出，命名不够中性；本轮先不改文件名，避免扩大改动。

## 已修复问题

- `/buyer/portal` 已接入商品 Schema 前端消费卡片。
- buyer portal 商品请求已使用 buyer token，不复用管理端 token。
- seller/buyer 商品 Schema 工作台卡片已对齐，seller 回归验证通过。

## 残留问题

- 前端三端物理拆分仍未开始，当前仍在 `react-ui/` 验证入口中。
- `ProductPortalSchemaController` 仍位于 `product` 模块但暴露 seller/buyer 路径；鉴权合规，是否迁移为 seller/buyer facade controller 后续单独评估。
- 第一次失败验收创建的端 token 未被强制踢出，等待自然过期；未执行强制踢出是为了避免误伤真实在线会话。

## CodeGraph 更新结果

- 已执行：

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

- 结果：通过，CodeGraph 输出 `Already up to date`。
