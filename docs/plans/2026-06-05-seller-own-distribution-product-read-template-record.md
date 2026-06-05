# 2026-06-05 卖家端我的商城商品只读后端模板记录

## 参考方向

本记录以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，并遵守当前已确认节奏：

- 先做一套标准卖家模板，验收通过后再复制买家。
- 每个切片只改一类东西，减少返工。
- 若依架构是核心，`product` 是共享业务模块，不作为第四个端承载 `/seller/**` 路由。

## 本轮范围

本轮只做 seller 端“我的商城商品”只读后端模板：

- 商品列表
- 商品详情
- SKU 列表
- seller 端权限 seed
- seller service 契约测试
- 复用台账和目标追踪记录

本轮不做：

- 不复制 buyer。
- 不做前端页面。
- 不修改 `product` 模块 admin 商品接口、mapper 或商品保存规则。
- 不执行远程数据库 DDL，不执行 buyer 相关 DML。
- 不启动三端前端物理拆分。

## 已完成

- 新增 seller 端只读 Controller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java`。
- 新增 seller 端只读 Service：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerPortalProductService.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java`
- 新增 seller 端响应 DTO：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/domain/SellerPortalProduct.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/domain/SellerPortalProductSku.java`
- 新增单元测试：`RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImplTest.java`。
- 新增可重复 HTTP 烟测脚本：`scripts/smoke/seller-own-distribution-product-read-template-smoke.ps1`。
- 更新 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`，新增 seller 端两个只读权限：
  - `seller:product:distribution:list`
  - `seller:product:distribution:query`
- 执行远程运行库 seller 权限 DML：
  - 连接来源：本机 `.env.local` 的 `RUOYI_DB_*`。
  - 目标环境：远程 MySQL，数据库 `fenxiao`。
  - 执行类型：DML，仅写入 `seller_menu` 和 `seller_role_menu`。
  - 执行结果：`seller_menu` 中两个权限从 0 条变为 2 条；新增菜单 2 条；新增 active seller role 授权 6 条；最终相关 role-menu 授权 6 条。
- 更新 `docs/architecture/reuse-ledger.md`，登记 seller 商品只读模板复用规则。
- 更新 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`，追加本轮检查点。

## 模板规则

- Controller 必须使用方法级 `@Anonymous`、`@PortalPreAuthorize`、`@PortalLog`，并显式调用 `PortalSessionContext.requireSession("seller")`。
- 商品范围必须由 `PortalLoginSession.subjectId` 决定。
- 列表查询必须创建新的 `ProductSpu` 查询对象并写入当前 `sellerId`，不得直接透传前端请求对象。
- 允许复制的筛选字段只限业务筛选字段：`keyword`、`sellerSpuCode`、`sellerSkuCode`、`productName`、`productNameEn`、`categoryId`、`spuStatus`。
- DTO 转换必须保留 PageHelper 分页元数据，避免列表 total 退化为当前页条数。
- 详情和 SKU 列表必须先校验 `product.sellerId == session.subjectId`。
- seller 端响应使用 `SellerPortalProduct` / `SellerPortalProductSku`，不直接返回 `ProductSpu` / `ProductSku`。
- buyer 后续不能机械按 `buyerId` 复制商品拥有关系；买家浏览商品可见性要单独确认。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller "-Dtest=SellerPortalProductServiceImplTest" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller test`：通过，`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,PortalTokenSupportTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`：通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\seller-own-distribution-product-read-template-smoke.ps1 -SellerUsername '594165649@qq.com' -OtherSellerUsername '1234'`：通过，覆盖 seller 登录、列表、伪造客户端范围参数、详情、SKU、字段脱敏、跨卖家详情/SKU 拒绝和 logout 清理。
- 远程 MySQL seller 权限 DML：通过，`menuBefore=0`、`insertedMenus=2`、`insertedRoleMenus=6`、`menuAfter=2`、`roleMenuAfter=6`。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白检查：通过。
- 相关文件冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 运行验收

- 当前运行数据源已按 `application.yml` / `application-druid.yml` 和 `.env.local` 确认为远程 MySQL / 远程 Redis；记录中不输出凭据。
- 首次 `mvn -DskipTests install` 在 `ruoyi-admin` repackage 阶段失败，原因是旧 8080 Java 进程锁住 `ruoyi-admin.jar`，报 `Unable to rename ... ruoyi-admin.jar.original`。
- 停止旧 8080 Java 进程后执行 `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -DskipTests install -rf :ruoyi-admin`：通过，`ruoyi-admin.jar` 已重打包。
- `seller-3.9.2.jar` 已确认包含 `SellerPortalProductDistributionController.class` 和 `SellerPortalProductServiceImpl.class`。
- `cd E:\Urili-Ruoyi; .\start-backend-local.ps1`：已启动新后端，8080 Java 进程存在，`/captchaImage` 返回 200。
- sellerId=5 / accountId=5 真实 seller 登录成功，调用：
  - `GET /seller/product/distribution-products/list?pageNum=1&pageSize=10`：`code=200`，`total=4`，`rows=4`。
  - `GET /seller/product/distribution-products/{sampleSpuId}`：`code=200`。
  - `GET /seller/product/distribution-products/{sampleSpuId}/skus`：`code=200`，`skuRows=2`。
  - 列表、详情和 SKU 响应未出现 `sellerId`、`systemSpuCode`、`systemSkuCode` 字段。
- sellerId=9 / accountId=8 真实 seller 登录成功后访问 sellerId=5 的 sample SPU：接口返回业务 `code=500`，消息为“商城商品不存在”，跨卖家访问被拒绝。
- 脚本化烟测使用真实 HTTP 业务链路，会产生 seller 端登录/退出日志和会话记录；脚本结束时已调用 `/seller/logout` 清理本次 token，不输出 token、JWT、Redis key、`.env.local` 或数据库连接明文。
- 脚本化烟测已补强断言：伪造 `sellerId`、`subjectId`、`accountId`、`terminal`、`systemSpuCode`、`sourceType` 不改变当前 seller 列表范围；跨卖家详情和 SKU 均返回业务 `code=500`，语义为“商城商品不存在”。
- 运行验收记录补充后执行 `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 5 changed files`。
- 脚本化烟测补强和文档补记后再次执行 `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 当前判断

seller 端商品只读模板已经可以作为后续 seller 端真实业务接口的范围控制样板。真实后端运行验收和脚本化烟测均已通过，可以进入用户验收。后续复制 buyer 前，需要先完成卖家模板验收，并确认买家商品浏览的业务可见性规则。
