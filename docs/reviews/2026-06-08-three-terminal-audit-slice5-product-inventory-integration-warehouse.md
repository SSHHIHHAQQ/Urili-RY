# 三端隔离 P0/P1 审计切片 5（只读）

- 日期：2026-06-08
- 范围：`product` / `inventory` / `integration` / `warehouse` 与 `seller` / `buyer portal` 商品接口交叉点
- 目标：只读扫描三端隔离 P0/P1 风险
- 重点：管理端接口缺 `@PreAuthorize`、portal 接口是否相信前端 `sellerId` / `buyerId`、product/inventory read model 刷新链路缺 service/字段、跨模块直接写对方表

## 结论

- `P0`：未发现本切片内可直接定级的 P0。
- `P1`：发现 1 条实质风险，已影响当前商品来源绑定到库存快照/来源库存读模型的刷新链。
- `P2`：发现 1 条契约缺口，会放大上面这类回归的再次引入概率。

## P0

### 未发现

- 本次扫描的管理端 controller 中，未发现 `product` / `inventory` / `integration` / `warehouse` 范围内缺失 `@PreAuthorize` 的接口：
  - [AdminProductDistributionController.java](E:/Urili-Ruoyi/RuoYi-Vue/product/src/main/java/com/ruoyi/product/controller/AdminProductDistributionController.java:38)
  - [AdminSourceProductController.java](E:/Urili-Ruoyi/RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceProductController.java:30)
  - [AdminSourceWarehouseStockController.java](E:/Urili-Ruoyi/RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceWarehouseStockController.java:31)
  - [AdminUpstreamSystemController.java](E:/Urili-Ruoyi/RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminUpstreamSystemController.java:59)
  - [AdminInventoryOverviewController.java](E:/Urili-Ruoyi/RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/controller/AdminInventoryOverviewController.java:33)
  - [AdminWarehouseController.java](E:/Urili-Ruoyi/RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/controller/AdminWarehouseController.java:36)

- 本次扫描的 seller/buyer portal 商品接口，未发现相信前端传入 `sellerId` / `buyerId` / `subjectId` 的入口；当前主体作用域来自 `PortalSessionContext`：
  - [SellerPortalProductDistributionController.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java:39)
  - [BuyerPortalProductDistributionController.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductDistributionController.java:39)
  - [SellerPortalProductServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:55)
  - [BuyerPortalProductServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java:58)

## P1

### 1. `product` 模块直接写 `integration` 的 `upstream_system_sku_pairing`，并绕过库存快照刷新链，导致来源库存/库存总览可明显滞后

- 证据 1：`product` mapper 直接声明并执行对 `integration` 表 `upstream_system_sku_pairing` 的写操作，而不是走 `integration` service/facade`
  - [ProductDistributionMapper.java](E:/Urili-Ruoyi/RuoYi-Vue/product/src/main/java/com/ruoyi/product/mapper/ProductDistributionMapper.java:120)
  - [ProductDistributionMapper.xml](E:/Urili-Ruoyi/RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml:1120)
  - [ProductDistributionMapper.xml](E:/Urili-Ruoyi/RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml:1129)

- 证据 2：`product` 侧来源绑定变更时，只会：
  - 删/写 `upstream_system_sku_pairing`
  - 调 `refreshSourceReadModels(...)`
  - 调当前商品的 `refreshInventoryOverview(spuId)`
  但不会刷新 `upstream_system_sku_inventory_snapshot`
  - [ProductDistributionServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java:1063)
  - [ProductDistributionServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java:1073)
  - [ProductDistributionServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java:1111)
  - [ProductDistributionServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java:1126)

- 证据 3：`source_warehouse_stock_detail` 的重建事实源就是 `upstream_system_sku_inventory_snapshot`；如果不先刷新快照里的 `system_sku/system_sku_name/customer_name`，后续来源库存读模型会继续读旧值
  - [UpstreamSystemMapper.xml](E:/Urili-Ruoyi/RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml:2409)
  - [UpstreamSystemMapper.xml](E:/Urili-Ruoyi/RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml:2449)
  - [UpstreamSystemMapper.xml](E:/Urili-Ruoyi/RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml:2794)

- 证据 4：`integration` 自己的“正规链路”在 SKU 配对新增/删除后，明确会先刷新 inventory snapshot，再重建 source warehouse stock read model，再刷新 inventory overview；这一步在 `product` 直写场景里被绕过了
  - [UpstreamSystemServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java:495)
  - [UpstreamSystemServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java:521)

- 影响判断：
  - 商品来源绑定/换绑/解绑后，`source_product_*` 会重建；
  - 但 `source_warehouse_stock_*` 和基于它继续汇总的 `inventory_overview_*` 可能仍基于旧 snapshot 的 `system_sku`/客户映射；
  - 结果是管理端商品、来源库存、库存总览三处数据可能短期不一致，要等下一次 integration 配对/同步链路才被动修正。

- 建议：
  - 把 `upstream_system_sku_pairing` 的新增/删除上收回 `integration` 模块，由 `product` 调公开 service/facade，不再直接写对方表。
  - 如果短期不搬迁，至少在 `product` 的 `syncSourcePairingProjection/removeSourcePairingProjection` 后补齐与 `integration` 同等的刷新顺序：
    1. refresh inventory snapshot by connection
    2. rebuild source warehouse stock read model by connection
    3. refresh source inventory overview by connection

## P2

### 1. 现有契约测试把这条跨模块写入当成“允许债务”固定下来，但没有覆盖 product 侧必须刷新 inventory snapshot 的合同

- 证据 1：`ProductDistributionMapperContractTest` 显式把 `deleteUpstreamSkuPairingsBySystemSkuAndConnectionCodes` / `upsertUpstreamSkuPairingsForBinding` 放进 allowlist，说明当前测试是在容忍 product -> integration 表写入，而不是阻止它
  - [ProductDistributionMapperContractTest.java](E:/Urili-Ruoyi/RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductDistributionMapperContractTest.java:347)
  - [ProductDistributionMapperContractTest.java](E:/Urili-Ruoyi/RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductDistributionMapperContractTest.java:381)

- 证据 2：`InventoryOverviewRefreshContractTest` 只校验了：
  - product 插入/更新后刷新当前 `spu`
  - integration 的 pairing 变更会刷新 snapshot/read model/overview
  但没有约束 product 侧“来源绑定投影写入 upstream pairing 表”后也必须补齐 snapshot 刷新
  - [InventoryOverviewRefreshContractTest.java](E:/Urili-Ruoyi/RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryOverviewRefreshContractTest.java:73)
  - [InventoryOverviewRefreshContractTest.java](E:/Urili-Ruoyi/RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryOverviewRefreshContractTest.java:80)
  - [InventoryOverviewRefreshContractTest.java](E:/Urili-Ruoyi/RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryOverviewRefreshContractTest.java:88)

- 影响判断：
  - 当前这类问题不是“没人知道”，而是“测试只守住了一半合同”；
  - 后续继续在 product 侧补投影逻辑时，很容易再次引入 source/inventory read model 滞后。

- 建议：
  - 新增一条 product/integration 边界契约：`product` 不得直接写 `upstream_system_sku_pairing`；或如果暂不收口，至少要求 product 侧每次投影写入后必须调用 snapshot refresh + inventory overview refresh。
  - 把这条合同放进现有 contract test，而不是继续仅用 allowlist 记债。

## 备注

- 本次为只读扫描，未改业务代码。
- 本次为了交付审计结果，仅新增本 Markdown 报告。
