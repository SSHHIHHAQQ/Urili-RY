# 买家端商城商品浏览只读后端模板记录

日期：2026-06-05

## 目标

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 和 `docs/plans/2026-06-05-buyer-product-browse-copy-boundary-plan.md` 为参考方向。

当前节奏是：先做一套标准 seller 模板，验收通过后复制 buyer；每个切片只改一类东西。本轮只做 buyer 后端只读模板，不做前端，不执行远程数据库 DDL/DML，不做 HTTP smoke。

## 已完成

- 新增 buyer 端商品浏览只读 Controller：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductDistributionController.java`
  - `GET /buyer/product/distribution-products/list`
  - `GET /buyer/product/distribution-products/{spuId}`
  - `GET /buyer/product/distribution-products/{spuId}/skus`
- 新增 buyer 端商品浏览 Service：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerPortalProductService.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java`
- 新增 buyer 端响应 DTO：
  - `BuyerPortalProduct`
  - `BuyerPortalProductSku`
- 扩展 product 共享只读查询：
  - `IProductDistributionService.selectOnSaleProductList(...)`
  - `IProductDistributionService.selectOnSaleProductById(...)`
  - `IProductDistributionService.selectOnSaleSkuList(...)`
  - Mapper 查询只聚合 `ON_SALE` SKU 的销售价、币种和 SKU 数量。
- 更新综合 seed 文件：
  - `buyer:product:distribution:list`
  - `buyer:product:distribution:query`
  - 写入 `buyer_menu` seed，并加入 active `buyer_role_menu` 授权 seed。
- 新增 buyer service 单测：
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImplTest.java`
- 回补 seller service 单测假实现，适配 product service 新增接口。

## 模板规则

- buyer 端身份只用于鉴权和端内 session 校验，不代表商品归属。
- buyer 商品列表、详情和 SKU 都必须使用后端 `ON_SALE` 可见性：
  - SPU 必须是 `ON_SALE`。
  - SKU 必须是 `ON_SALE`。
  - 列表价格聚合只来自 `ON_SALE` SKU。
  - 没有 `ON_SALE` SKU 的 SPU 对 buyer 不可见。
- buyer 请求参数只透传业务筛选：
  - `keyword`
  - `productName`
  - `productNameEn`
  - `categoryId`
- buyer 请求参数不透传身份或后台字段：
  - `sellerId`
  - `systemSpuCode`
  - `systemSkuCode`
  - `sellerSpuCode`
  - `sellerSkuCode`
  - `sourceType`
  - `spuStatus`
- buyer DTO 不暴露 seller 内部字段、系统编码、供货价、后台审计字段、token 或 Redis key。
- buyer 端权限写入 `buyer_menu` / `buyer_role_menu`，不写入 `sys_menu` / `sys_role`。

## 边界说明

- 本轮没有执行远程 MySQL DDL/DML。
- 本轮没有读取或输出 `.env.local` 明文。
- 本轮没有修改前端。
- 本轮没有新增或运行 HTTP smoke。
- 本轮没有启动或重启后端。
- product 模块只新增共享只读查询口径，保留管理端商品查询行为不变。

## 后续补记

2026-06-05 后续独立切片已完成 buyer 商品浏览权限 DML 与 HTTP smoke，记录见：

- `docs/plans/2026-06-05-buyer-product-permission-dml-smoke-record.md`

补记结论：

- 远程运行库已补齐 `buyer_menu` 中的 `buyer:product:distribution:list` 与 `buyer:product:distribution:query`。
- 远程运行库已将两个权限授权给 active buyer role。
- 已重建并启动后端 jar。
- 已通过真实 buyer HTTP smoke，覆盖无 token 拒绝、登录、`getInfo` 权限集合、列表、伪造范围参数不生效、详情、SKU、固定不存在商品拒绝和 logout 后旧 token 失效。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer "-Dtest=BuyerPortalProductServiceImplTest" test`：未作为通过项；因未带 `-am`，buyer 编译时读取到旧 product 接口产物，报找不到新增方法。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am test`：通过，buyer 模块 `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`，依赖模块同轮通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,PortalTokenSupportTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`：通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。

## 检查清单

- 新增问题：buyer 商品浏览若复用管理端商品查询，会把非上架 SKU 的价格和规格暴露给买家。
- 已修复问题：本切片新增 product 上架商品只读查询，buyer 后端只使用该查询口径。
- 残留问题：buyer 前端工作台卡片和浏览器 smoke 尚未做；远程 `buyer_menu` / `buyer_role_menu` 权限 DML 与 buyer HTTP smoke 已在后续独立切片补齐。
- 验证命令：已记录在“验证结果”。
- 未验证原因：本切片自身未执行 HTTP smoke，因为当时不执行远程 DML、不启动后端、不做运行库权限补齐；后续独立切片已补齐。
- 权限检查结果：新增 buyer 端权限已写入综合 seed；远程运行库权限和 active buyer role 授权已在后续独立切片补齐。
- 字典/选项复用检查结果：本切片未新增字典；商品状态继续复用 product 既有 `ON_SALE` code。
- 复用台账检查结果：已更新 `docs/architecture/reuse-ledger.md`。
- CodeGraph 更新结果：已执行 `codegraph sync .`，输出 `Synced 11 changed files`、`Added: 6, Modified: 5 - 456 nodes in 1.1s`。
- 大文件合理性判断结果：新增文件职责单一；最大新增 service 文件未超过需要拆分的治理阈值。
- 重复代码检查结果：buyer 复用 seller 的端入口结构和测试模式，但业务谓词改为 `ON_SALE` 可浏览商品，没有复制 seller 的商品拥有关系。
