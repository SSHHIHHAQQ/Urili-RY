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
- 不执行远程数据库 DDL/DML。
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
- 更新 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`，新增 seller 端两个只读权限：
  - `seller:product:distribution:list`
  - `seller:product:distribution:query`
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
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白检查：通过。
- 相关文件冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 当前判断

seller 端商品只读模板已经可以作为后续 seller 端真实业务接口的范围控制样板。后续复制 buyer 前，需要先完成卖家模板验收，并确认买家商品浏览的业务可见性规则。
