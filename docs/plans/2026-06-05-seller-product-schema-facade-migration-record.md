# 卖家商品 Schema facade 迁移执行记录

日期：2026-06-05

## 目标

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板先做、验收后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：

- 将 seller 商品分类和 Schema 端入口从 product controller 迁入 seller 模块 facade。
- product 模块只保留共享商品 schema 只读服务能力。
- 路径、权限点、日志 title、前端调用保持不变。
- buyer 端入口本轮不迁移，后续按 seller 模板单独复制。

## 改动范围

已完成：

- 新增 `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductPortalSchemaService.java`：
  - 对 seller/buyer portal 暴露只读商品分类与 Schema 窄接口。
- 新增 `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductPortalSchemaServiceImpl.java`：
  - 集中承载启用分类过滤、可发布过滤、schema 可见属性过滤、启用选项过滤和端内 DTO 映射。
  - 内部复用 `IProductConfigService.selectCategoryList(...)` 与 `IProductConfigService.previewCategorySchema(...)`。
- 更新 `RuoYi-Vue/seller/pom.xml`：
  - 增加 seller -> product 依赖。
  - 依赖方向为 `seller -> product -> ruoyi-system`，不形成循环。
- 新增 `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductSchemaController.java`：
  - 承载 `GET /seller/product/categories`。
  - 承载 `GET /seller/product/categories/{categoryId}/schema`。
  - 方法级保留 `@Anonymous`、`@PortalPreAuthorize(terminal = "seller", ...)` 和 `@PortalLog(terminal = "seller", ...)`。
- 更新 `RuoYi-Vue/product/src/main/java/com/ruoyi/product/controller/ProductPortalSchemaController.java`：
  - 移除 seller 映射。
  - 只保留 buyer 商品分类与 Schema 映射。
  - 继续委托 `IProductPortalSchemaService`。
- 更新 `docs/architecture/reuse-ledger.md`。
- 更新 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`。

未改动：

- 未新增 DDL/DML。
- 未改权限 seed。
- 未改 React 前端。
- 未迁移 buyer facade。

## 数据源与运行影响

运行验证前已确认：

- 后端激活配置：`druid`。
- MySQL 配置来源：`RUOYI_DB_*` 运行变量。
- Redis 配置来源：`RUOYI_REDIS_*` 运行变量。
- `.env.local` 未输出明文。

运行验证影响：

- 本轮为了验证 seller/buyer portal 接口，生成并消费 seller/buyer 免密票据。
- 商品分类和 Schema 是 GET 请求，但端内接口带 `@PortalLog`，会写入 seller/buyer 端操作日志。
- 验证结束后已调用 `/seller/logout` 和 `/buyer/logout` 清理本轮新 token。
- 本轮没有输出 token、密码、Redis key、Authorization header、直登 URL 或 `.env.local` 明文。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests compile
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests package
```

结果：通过，`ruoyi-admin.jar` 已重新打包。

```powershell
cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart
```

结果：通过；8080 正常监听，`/captchaImage` 返回 `code=200`。

接口矩阵：

- seller token 调 `GET /seller/product/categories`：`code=200`，返回 160 个分类。
- seller token 调 `GET /seller/product/categories/{categoryId}/schema`：`code=200`，返回 8 个 schema 字段。
- buyer token 调 `GET /buyer/product/categories`：`code=200`，返回 160 个分类。
- buyer token 调 `GET /buyer/product/categories/{categoryId}/schema`：`code=200`，返回 8 个 schema 字段。
- 无 token 调 seller 分类接口：`code=401`。
- admin token 调 seller 分类接口：`code=401`。
- buyer token 调 seller 分类接口：`code=401`。
- seller token 调 buyer 分类接口：`code=401`。

## 权限检查结果

- seller facade 使用方法级 `@Anonymous`，未使用类级匿名。
- seller facade 使用 `@PortalPreAuthorize(terminal = "seller", hasPermi = "seller:product:category:list")`。
- seller facade 使用 `@PortalPreAuthorize(terminal = "seller", hasPermi = "seller:product:schema:query")`。
- seller facade 继续调用 `PortalSessionContext.requireSession("seller")`，不信任前端传入 `sellerId`、`accountId` 或 `terminal`。
- seller facade 使用 `@PortalLog(terminal = "seller", ...)`，并关闭响应体日志。
- 管理端 token 和 buyer token 不能访问 seller 商品分类接口。

## 字典/选项复用检查结果

- 本轮只迁移后端入口归属，不改商品属性类型、选项来源、规则模式、分组、是否标识等 code/label 规则。
- 前端继续复用 `react-ui/src/pages/Product/constants.ts` 和现有 portal product DTO。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`：
  - seller 入口已迁入 seller facade。
  - product 模块保留 `IProductPortalSchemaService` 共享只读 schema 服务。
  - buyer facade 迁移仍为后续单独切片。

## 大文件合理性判断结果

- `ProductPortalSchemaServiceImpl.java` 职责单一：只做端内商品分类与 Schema 只读 DTO 组装，不承载平台商品配置写操作。
- `SellerPortalProductSchemaController.java` 只承载两个 seller 端只读入口，未接近 300 行阈值。
- `ProductPortalSchemaController.java` 已从 seller/buyer 双入口收窄为 buyer 当前入口，职责更窄。

## 重复代码检查结果

- seller facade 没有复制商品分类过滤、schema 继承合并或 DTO 映射逻辑。
- seller facade 只委托 `IProductPortalSchemaService`。
- buyer 当前仍复用同一 product service；后续迁移 buyer facade 时也应只复制 controller 入口，不复制 product schema 计算。

## 新增问题

- buyer 商品分类与 Schema 入口当前仍由 product 模块 controller 承载，controller 归属尚未完全按 terminal 模块收口。

## 已修复问题

- seller 商品分类与 Schema 入口已迁入 seller 模块 facade。
- product 模块不再暴露 seller 商品分类与 Schema 路径。
- seller 路径、权限点、日志 title 和前端调用保持兼容。

## 残留问题

- buyer facade 迁移尚未开始，需要后续按 seller 模板单独复制。
- 前端三端物理拆分仍未开始。

## CodeGraph 更新结果

- 已执行 `codegraph sync .`。
- 结果：通过，输出 `Synced 5 changed files`，包含 `Added: 3, Modified: 2 - 75 nodes in 478ms`。
