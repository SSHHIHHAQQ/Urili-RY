# product/inventory/integration/warehouse/finance 共享域与三端边界只读审计

## 审计范围

- 工作区：`E:\Urili-Ruoyi`
- 模块切片：`product` / `inventory` / `integration` / `warehouse` / `finance`
- 审计目标：
  - `product` 是否被做成第四端
  - 共享域是否裸暴露 portal/admin 权限面
  - 是否跨模块直 import mapper 或绕过 facade/public service
  - seller/buyer portal 是否信任前端传入主体 ID
  - 是否存在字段/service 缺失导致编译或接口失败

## 验证动作

- 后端编译：`mvn -pl product,inventory,integration,warehouse,finance -am -DskipTests compile`
- 前端类型检查：`npm run tsc -- --pretty false`
- 额外抽查：
  - `mvn -pl buyer -Dtest=BuyerPortalProductServiceImplTest test`
  - `mvn -pl seller -am clean test "-Dtest=SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" `

---

## P0

### 无

本次在审计范围内未发现会直接导致三端串权、共享域退化成第四端、或 portal 直接信任前端主体 ID 的 P0 问题。

---

## P1

### 无

本次在审计范围内未发现明确 P1。

### 支撑证据

1. `product` 仍是共享商品基础域，不是第四端
   - 管理端入口仍挂在 admin 面，而不是把 `product` 模块自身暴露成独立终端：
     - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/controller/AdminProductDistributionController.java:32-44`
   - seller portal 和 buyer portal 的入口分别放在 `seller` / `buyer` 模块，通过各自 controller 暴露：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductSchemaController.java:20-47`
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java:25-65`
   - 共享 `product` 侧只提供只读 schema service，没有出现 terminal 入口混入：
     - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductPortalSchemaServiceImpl.java:17-21`

2. 共享域没有裸暴露 portal/admin 混合权限面
   - `integration` 现有合同测试明确要求 `/integration/admin/**` 只能留在 admin security surface：
     - `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/architecture/IntegrationAdminPermissionContractTest.java:28-44`
   - `inventory`、`warehouse`、`finance` 当前入口都仍是 admin 路径：
     - `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/controller/AdminInventoryOverviewController.java:26-33`
     - `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/controller/AdminWarehouseController.java:29-36`
     - `RuoYi-Vue/finance/src/main/java/com/ruoyi/finance/controller/AdminCurrencyController.java:30-37`

3. seller portal 没有信任前端传入 `sellerId`，主体范围从 session 派生
   - controller 每个入口都先取 `PortalSessionContext.requireSession("seller")`：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java:37-42`
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java:49-64`
   - service 构造查询时强制 `scoped.setSellerId(session.getSubjectId())`，并在详情/SKU 查询时继续用 `session.getSubjectId()` 约束：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:55-70`
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:94-106`
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:118-140`

4. portal 只拿到白名单 DTO，没有把主体/账号/票据等内审字段从共享域漏出去
   - schema service 只映射类目、属性、选项白名单字段：
     - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductPortalSchemaServiceImpl.java:73-140`
   - 对应测试明确禁止 `subjectId` / `sellerId` / `buyerId` / `accountId` / `directLoginTicketId` 等字段出现在 portal DTO：
     - `RuoYi-Vue/product/src/test/java/com/ruoyi/product/service/impl/ProductPortalSchemaServiceImplTest.java:65-91`

5. 没看到跨模块直 import 其他模块 mapper 的证据，`product` 侧跨模块依赖仍经 public service/facade
   - `ProductDistributionServiceImpl` 依赖的是 `IFinanceCurrencyService`、`ISourceReadModelRefreshService`、`ISourceSkuPairingProjectionService`、`IInventoryOverviewService`、`IWarehouseService`，不是别的模块 mapper：
     - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java:29-55`
   - `product` 自己也有合同测试，禁止 import `com.ruoyi.integration.service.impl.*`：
     - `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductModuleBoundaryContractTest.java:16-45`

6. 本次没复现字段/service 缺失导致的编译失败
   - 后端定向 compile 成功：`product/inventory/integration/warehouse/finance`
   - 前端 `tsc --noEmit` 成功

---

## P2

### P2-1 `seller` 单模块直接跑测会受本地陈旧构建产物影响，出现假红 `NoSuchMethodError`

- 现象：
  - 直接执行 `mvn -pl seller -Dtest=SellerPortalProductServiceImplTest test` 时，`SellerPortalProductServiceImplTest` 报：
    - `NoSuchMethodError: IProductDistributionService.selectProductById(Long, Long)`
    - `NoSuchMethodError: IProductDistributionService.selectSkuList(Long, Long)`
- 复核：
  - 执行 `mvn -pl seller -am clean test "-Dtest=SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` 后测试通过。
- 代码证据：
  - `seller` 当前源码确实调用了这两个双参数接口：
    - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:101-102`
    - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:139`
  - `product` 当前接口也确实定义了这两个方法：
    - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductDistributionService.java:18-20`
    - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductDistributionService.java:50-52`
- 判断：
  - 这是本地 `target` / reactor 构建状态不一致导致的测试假红，不是当前源码中的明确边界缺陷。
- 最小修复建议：
  - 把 seller 相关验证命令固定成 reactor clean 方式执行，避免单模块直接复用陈旧产物：
    - `mvn -pl seller -am clean test`
  - 若要把这个问题彻底制度化，补一条团队执行约定或 CI 脚本，不建议把它升级成业务代码改动。

---

## 结论

- 本次切片内 **无明确 P0 / P1**。
- 当前实现与目标边界基本一致：
  - `product` 还是共享商品基础域，不是第四端；
  - `inventory` / `integration` / `warehouse` / `finance` 仍停留在 admin surface；
  - seller portal 商品链路从 session 派生主体范围，没有看到信任前端 `sellerId` 的实现；
  - 没发现跨模块直 import 其他模块 mapper 的现行违规。
- 唯一值得登记的是一个 **P2 级本地构建态假红**，不属于当前源码缺陷。
