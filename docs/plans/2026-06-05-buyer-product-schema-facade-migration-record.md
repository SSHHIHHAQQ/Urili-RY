# 买家商品 Schema facade 迁移执行记录

日期：2026-06-05

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按已验证的 seller 商品 Schema facade 模板，把 buyer 商品分类和 Schema 端入口从 `product` 模块收口到 `buyer` 模块。

本轮只处理端入口归属，不改业务规则、不改前端字段、不改权限 seed、不新增 DDL/DML。

## 已完成改动

- 更新 `RuoYi-Vue/buyer/pom.xml`：
  - 新增对 `product` 模块的依赖。
  - 依赖方向为 `buyer -> product -> ruoyi-system`，不形成循环。
- 新增 `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductSchemaController.java`：
  - 承载 `GET /buyer/product/categories`。
  - 承载 `GET /buyer/product/categories/{categoryId}/schema`。
  - 继续使用原 buyer 端权限点、日志 title 和端 session 校验。
- 删除 `RuoYi-Vue/product/src/main/java/com/ruoyi/product/controller/ProductPortalSchemaController.java`：
  - product 模块不再暴露 seller/buyer 端入口。
  - product 模块只保留 `IProductPortalSchemaService` 和 `ProductPortalSchemaServiceImpl` 作为共享只读 schema 服务。
- 更新 `docs/architecture/reuse-ledger.md` 和 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`：
  - 标记 seller/buyer 商品 Schema 端入口均已迁入各自 terminal facade。

## 数据源与运行影响

- 本轮代码改造不执行 SQL，不新增 DDL/DML。
- 后续真实接口验证前必须继续以 `application.yml`、`application-druid.yml` 和 `.env.local` 注入的 `RUOYI_*` 运行变量确认当前 MySQL/Redis 来源。
- 接口验证会生成正常远程运行痕迹，包括 direct-login ticket、portal session、login log 和 oper log；不输出 token、Redis key、`.env.local` 或数据库连接明文。

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

结果：通过，8080 正常监听，`/captchaImage` 返回 `code=200`。

接口矩阵：

- admin 登录：`code=200`。
- 管理端读取 seller/buyer 列表：`code=200`。
- 管理端生成 seller/buyer 免密代入票据：`code=200`。
- seller/buyer 消费免密代入票据：`code=200`。
- seller token 调 `GET /seller/product/categories`：`code=200`，返回 160 个分类。
- seller token 调 `GET /seller/product/categories/{categoryId}/schema`：`code=200`，返回 8 个 schema 字段，敏感字段检查为 false。
- buyer token 调 `GET /buyer/product/categories`：`code=200`，返回 160 个分类。
- buyer token 调 `GET /buyer/product/categories/{categoryId}/schema`：`code=200`，返回 8 个 schema 字段，敏感字段检查为 false。
- buyer token 携带伪造 `buyerId`、`sellerId`、`accountId`、`terminal` 参数调用 buyer 分类接口：`code=200`，返回 160 个分类，结果未因参数扩大范围。
- 无 token 调 buyer 分类接口：`code=401`。
- admin token 调 buyer 分类接口：`code=401`。
- seller token 调 buyer 分类接口：`code=401`。
- seller token 调 buyer schema 接口：`code=401`。
- buyer token 调 seller 分类接口：`code=401`。
- buyer token 调 seller schema 接口：`code=401`。
- 验证结束后调用 `/buyer/logout`、`/seller/logout`：均为 `code=200`。
- 登出后旧 buyer/seller token 调 `/buyer/getInfo`、`/seller/getInfo`：均为 `code=401`。

## 权限检查结果

- buyer facade 使用方法级 `@Anonymous`，未使用类级匿名。
- buyer facade 使用 `@PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:product:category:list")`。
- buyer facade 使用 `@PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:product:schema:query")`。
- buyer facade 继续调用 `PortalSessionContext.requireSession("buyer")`，不信任前端传入 `buyerId`、`accountId` 或 `terminal`。
- buyer facade 使用 `@PortalLog(terminal = "buyer", ...)`，并关闭响应体日志。

## 字典/选项复用检查结果

- 本轮只迁移后端入口归属，不改商品属性类型、选项来源、规则模式、分组、是否标识等 code/label 规则。
- 前端继续复用 `react-ui/src/pages/Product/constants.ts` 和现有 portal product DTO。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`：
  - seller/buyer 端入口均已迁入各自 terminal facade。
  - product 模块保留 `IProductPortalSchemaService` 共享只读 schema 服务。

## 大文件合理性判断结果

- `BuyerPortalProductSchemaController.java` 只承载两个 buyer 端只读入口，未接近 300 行阈值。
- `ProductPortalSchemaServiceImpl.java` 职责单一：只做端内商品分类与 Schema 只读 DTO 组装，不承载平台商品配置写操作。
- 删除 `ProductPortalSchemaController.java` 后，product 模块不再混放 terminal 端入口。

## 重复代码检查结果

- buyer facade 没有复制商品分类过滤、schema 继承合并或 DTO 映射逻辑。
- buyer facade 只委托 `IProductPortalSchemaService`。
- seller/buyer controller 结构同构，但核心 product schema 计算仍只有一份。

## 新增问题

- 暂无新增问题。

## 已修复问题

- buyer 商品分类与 Schema 入口已迁入 buyer 模块 facade。
- product 模块不再暴露 seller/buyer 商品分类与 Schema 路径。
- buyer 路径、权限点、日志 title 和前端调用保持兼容。

## 残留问题

- 前端三端物理拆分仍未开始。
- 本轮 buyer 商品 Schema facade 迁移无剩余验证项。

## CodeGraph 更新结果

- 已执行 `codegraph sync .`。
- 结果：通过，输出 `Synced 11 changed files`，包含 `Added: 9, Modified: 1, Removed: 1 - 307 nodes in 501ms`。
