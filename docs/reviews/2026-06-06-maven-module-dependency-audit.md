# 2026-06-06 Maven/module 依赖只读审计

## 审计范围

- `RuoYi-Vue/pom.xml`
- `RuoYi-Vue/ruoyi-admin/pom.xml`
- `RuoYi-Vue/seller/pom.xml`
- `RuoYi-Vue/buyer/pom.xml`
- `RuoYi-Vue/product/pom.xml`
- `RuoYi-Vue/warehouse/pom.xml`
- `RuoYi-Vue/finance/pom.xml`
- `RuoYi-Vue/integration/pom.xml`
- 主要 service import 与注入链：
  - `product/service/impl/ProductDistributionServiceImpl.java`
  - `seller/service/impl/SellerPortalProductServiceImpl.java`
  - `buyer/service/impl/BuyerPortalProductServiceImpl.java`
  - `seller/service/impl/ProductSellerLookupServiceImpl.java`
  - `warehouse/service/impl/WarehouseServiceImpl.java`

## 结论摘要

- `P0`：未发现当前 Maven 模块声明式循环，也未发现 Java 层“跨模块直接注入对方 Mapper”导致的现有编译/启动硬阻断。
- `P1-1`：`seller/buyer` 只消费商品只读能力，但当前依赖的是整块 `IProductDistributionService`；该实现把 `warehouse/finance/integration` 的必选 Bean 一并带入启动条件，导致卖家/买家侧对 `product` 的依赖仍然是“整块业务服务依赖”，不是稳定的只读契约依赖。
- `P1-2`：`ObjectProvider<ProductSellerLookupService>` 只解决了“seller lookup Bean 可缺省注入”，没有真正消除 `product -> seller` 的运行时反向必选依赖；一旦走到 `insert/update` 写路径，`product` 仍然会在运行时强依赖 `seller`。

## P0 / P1 证据

### P0：未发现声明式循环或 Java 层跨模块 Mapper 直读

1. Maven 依赖链当前是单向的：
   - `seller -> product`：[`RuoYi-Vue/seller/pom.xml:18`](../../RuoYi-Vue/seller/pom.xml) 至 [`RuoYi-Vue/seller/pom.xml:30`](../../RuoYi-Vue/seller/pom.xml)
   - `buyer -> product`：[`RuoYi-Vue/buyer/pom.xml:18`](../../RuoYi-Vue/buyer/pom.xml) 至 [`RuoYi-Vue/buyer/pom.xml:30`](../../RuoYi-Vue/buyer/pom.xml)
   - `product -> finance + warehouse`：[`RuoYi-Vue/product/pom.xml:18`](../../RuoYi-Vue/product/pom.xml) 至 [`RuoYi-Vue/product/pom.xml:36`](../../RuoYi-Vue/product/pom.xml)
   - `warehouse -> finance + integration`：[`RuoYi-Vue/warehouse/pom.xml:17`](../../RuoYi-Vue/warehouse/pom.xml) 至 [`RuoYi-Vue/warehouse/pom.xml:31`](../../RuoYi-Vue/warehouse/pom.xml)
   - `integration -> ruoyi-system`：[`RuoYi-Vue/integration/pom.xml:18`](../../RuoYi-Vue/integration/pom.xml) 至 [`RuoYi-Vue/integration/pom.xml:24`](../../RuoYi-Vue/integration/pom.xml)
   - `finance -> ruoyi-system`：[`RuoYi-Vue/finance/pom.xml:18`](../../RuoYi-Vue/finance/pom.xml) 至 [`RuoYi-Vue/finance/pom.xml:24`](../../RuoYi-Vue/finance/pom.xml)
2. `ruoyi-admin` 显式聚合 `seller/buyer/integration/finance/product/warehouse`，但这些依赖没有再反向指回 `seller` 或 `buyer`：
   - [`RuoYi-Vue/ruoyi-admin/pom.xml:57`](../../RuoYi-Vue/ruoyi-admin/pom.xml) 至 [`RuoYi-Vue/ruoyi-admin/pom.xml:90`](../../RuoYi-Vue/ruoyi-admin/pom.xml)
3. Java 层未发现“模块 A 的 service 直接 import 模块 B 的 mapper”：
   - `rg -n "^import com\.ruoyi\.(seller|buyer|product|warehouse|finance|integration)\.mapper"` 结果只命中各模块自身 mapper。
   - 因此当前没有“service 直接跨模块读对方 Mapper 接口”的证据。

### P1-1：`seller/buyer` 仍依赖整块 `product` 服务，启动条件被 `warehouse/finance/integration` 放大

1. `seller` 与 `buyer` 都只依赖 `product` 模块：
   - [`RuoYi-Vue/seller/pom.xml:26`](../../RuoYi-Vue/seller/pom.xml) 至 [`RuoYi-Vue/seller/pom.xml:30`](../../RuoYi-Vue/seller/pom.xml)
   - [`RuoYi-Vue/buyer/pom.xml:26`](../../RuoYi-Vue/buyer/pom.xml) 至 [`RuoYi-Vue/buyer/pom.xml:30`](../../RuoYi-Vue/buyer/pom.xml)
2. 但 `product` 模块自身又声明依赖 `finance` 与 `warehouse`：
   - [`RuoYi-Vue/product/pom.xml:26`](../../RuoYi-Vue/product/pom.xml) 至 [`RuoYi-Vue/product/pom.xml:36`](../../RuoYi-Vue/product/pom.xml)
3. `warehouse` 继续声明依赖 `integration` 与 `finance`：
   - [`RuoYi-Vue/warehouse/pom.xml:23`](../../RuoYi-Vue/warehouse/pom.xml) 至 [`RuoYi-Vue/warehouse/pom.xml:31`](../../RuoYi-Vue/warehouse/pom.xml)
4. `seller/buyer` 实际只调用 `IProductDistributionService` 的只读子集：
   - `seller` 只调用 `selectProductList / selectProductById / selectSkuList`：[`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:32`](../../RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java), [`:51`](../../RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java), [`:100`](../../RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java)
   - `buyer` 只调用 `selectOnSaleProductList / selectOnSaleProductById / selectOnSaleSkuList`：[`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java:36`](../../RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java), [`:55`](../../RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java), [`:100`](../../RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java)
5. 但 `IProductDistributionService` 是“读写混合大接口”，包含创建、更新、状态流转、调价等写能力：
   - [`RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductDistributionService.java:13`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductDistributionService.java) 至 [`:45`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductDistributionService.java)
6. 其唯一实现 `ProductDistributionServiceImpl` 在 Bean 创建阶段就要求：
   - `ObjectProvider<ProductSellerLookupService>`：[`RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java:83`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java) 至 [`:85`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java)
   - `IFinanceCurrencyService`：[`.../ProductDistributionServiceImpl.java:86`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java) 至 [`:88`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java)
   - `IWarehouseService`：[`.../ProductDistributionServiceImpl.java:89`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java) 至 [`:90`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java)
7. `IWarehouseService` 的实现 `WarehouseServiceImpl` 又必选依赖：
   - `IFinanceCurrencyService`：[`RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java:46`](../../RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java) 至 [`:47`](../../RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java)
   - `IUpstreamSystemService`：[`.../WarehouseServiceImpl.java:49`](../../RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java) 至 [`:50`](../../RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java)

**判断：**

- 当前全量 `ruoyi-admin` 启动场景下，这不是立刻的 `P0`，因为 `ruoyi-admin` 已显式装配全部模块。
- 但它仍然是 `P1` 模块边界问题：卖家/买家只读链路实际上被绑定到 `product -> warehouse -> integration/finance` 的整条启动链，导致后续任何“卖家/买家轻量切片启动”“模块单测隔离”“拆分 seller-ui / buyer-ui 对应后端切片”都会先被 `product` 的写侧依赖拖住。

**最小修复建议：**

1. 把 `IProductDistributionService` 拆成至少两层：
   - `IProductQueryService`：只保留 `seller/buyer` 当前用到的只读查询。
   - `IProductCommandService`：保留建品、改品、上下架、调价等写操作。
2. 让 `SellerPortalProductServiceImpl` 与 `BuyerPortalProductServiceImpl` 只注入 `IProductQueryService`。
3. `ProductQueryServiceImpl` 仅保留 `product` 自有 mapper 依赖；不要把 `warehouse/finance/seller lookup` 强行挂到只读查询 Bean 上。

### P1-2：`ObjectProvider` 修复不够，`product` 写路径仍有运行时反向必选 `seller` 依赖

1. `ProductDistributionServiceImpl` 的写路径 `insertProduct/updateProduct` 都先走 `normalizeSpuForSave(...)`：
   - [`RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java:137`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java) 至 [`:157`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java)
   - [`.../ProductDistributionServiceImpl.java:402`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java) 至 [`:428`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java)
2. `normalizeSpuForSave(...)` 无条件调用 `fillSellerSnapshot(product)`：
   - [`.../ProductDistributionServiceImpl.java:426`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java) 至 [`:428`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java)
3. `fillSellerSnapshot(...)` 虽然使用 `getIfAvailable()`，但找不到实现时直接抛错：
   - [`.../ProductDistributionServiceImpl.java:447`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java) 至 [`:452`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java)
   - 异常文案：`Product seller lookup service is not enabled`
4. 该扩展点的唯一实现仍在 `seller` 模块，并且实现本身继续依赖 `ISellerService`：
   - [`RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/ProductSellerLookupService.java:7`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/ProductSellerLookupService.java) 至 [`:9`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/ProductSellerLookupService.java)
   - [`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/ProductSellerLookupServiceImpl.java:12`](../../RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/ProductSellerLookupServiceImpl.java) 至 [`:23`](../../RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/ProductSellerLookupServiceImpl.java)

**判断：**

- `ObjectProvider` 只把“缺 Bean 时启动直接炸掉”降成了“真正执行写路径时炸掉”。
- 这意味着 `product` 仍不是一个真正独立的共享基础模块；它在运行时仍通过写路径反向要求 `seller` 存在。
- 因为 `seller/buyer` 当前只走读路径，所以这不是现网立即启动阻断；但如果后续试图把 `product` 单独作为共享域模块抽离，当前修复不够。

**最小修复建议：**

1. 不要让 `product` 核心写服务自己回查 `seller`。
2. 最小代价做法：
   - 把 `fillSellerSnapshot(...)` 从 `ProductDistributionServiceImpl` 挪到管理端/卖家端上层 facade；
   - `product` 写服务只接收已经校验过的 `sellerId/sellerNo/sellerName` 快照输入。
3. 如果必须保留扩展点模式，则把“无 seller 扩展时不可写”显式做成：
   - 单独的 `ProductAdminWriteService`
   - 或 `@ConditionalOnBean(ProductSellerLookupService.class)` 的写侧 Bean
   - 避免当前这种“接口看起来可选，行为上仍强依赖”的半解耦状态。

## 其他观察

- `seller` 与 `buyer` 对 `product` 的 schema 查询依赖是更干净的：它们注入的是 `IProductPortalSchemaService`，没有看到额外拖入 `warehouse/finance/integration` 的写侧依赖。
- 未发现 Java 层跨模块 `Mapper` 直注；但 `warehouse` 自己的 `WarehouseMapper.xml` 确实直接 join 了 `seller` 与 `upstream_*` 表：
  - [`RuoYi-Vue/warehouse/src/main/resources/mapper/warehouse/WarehouseMapper.xml:79`](../../RuoYi-Vue/warehouse/src/main/resources/mapper/warehouse/WarehouseMapper.xml) 至 [`:82`](../../RuoYi-Vue/warehouse/src/main/resources/mapper/warehouse/WarehouseMapper.xml)
  - [`.../WarehouseMapper.xml:270`](../../RuoYi-Vue/warehouse/src/main/resources/mapper/warehouse/WarehouseMapper.xml) 至 [`:273`](../../RuoYi-Vue/warehouse/src/main/resources/mapper/warehouse/WarehouseMapper.xml)
  - 这属于 SQL/数据层耦合，不是本次“跨模块 Mapper 接口直读”证据，但后续如果做严格模块隔离，需要继续拆。

## 新增问题

1. `P1`：`seller/buyer` 只读链路依赖整块 `IProductDistributionService`，被 `warehouse/finance/integration` 启动链放大。
2. `P1`：`ObjectProvider<ProductSellerLookupService>` 仍未消除 `product` 写路径对 `seller` 的运行时强依赖。

## 已修复问题

- 本次为只读审计，未修改源码，暂无“已修复问题”。

## 残留问题

- `product` 查询/写入职责未拆分，后续模块隔离仍会持续受影响。
- `warehouse` 仍存在 SQL 级跨业务表 join，后续若继续收紧模块边界，需要额外审计。

## 验证命令

```powershell
Get-Content docs\architecture\reuse-ledger.md
Get-Content RuoYi-Vue\pom.xml
Get-Content RuoYi-Vue\seller\pom.xml
Get-Content RuoYi-Vue\buyer\pom.xml
Get-Content RuoYi-Vue\product\pom.xml
Get-Content RuoYi-Vue\warehouse\pom.xml
Get-Content RuoYi-Vue\finance\pom.xml
Get-Content RuoYi-Vue\integration\pom.xml
Get-Content RuoYi-Vue\ruoyi-admin\pom.xml
rg -n "<artifactId>|<dependency>|<module>" RuoYi-Vue\*.xml RuoYi-Vue\*\pom.xml
rg -n "^import com\.ruoyi\.(seller|buyer|product|warehouse|finance|integration)\.(service|mapper|domain|controller|task|support)" RuoYi-Vue\seller\src\main\java RuoYi-Vue\buyer\src\main\java RuoYi-Vue\product\src\main\java RuoYi-Vue\warehouse\src\main\java RuoYi-Vue\finance\src\main\java RuoYi-Vue\integration\src\main\java
rg -n "^import com\.ruoyi\.(seller|buyer|product|warehouse|finance|integration)\.mapper" RuoYi-Vue\seller\src\main\java RuoYi-Vue\buyer\src\main\java RuoYi-Vue\product\src\main\java RuoYi-Vue\warehouse\src\main\java RuoYi-Vue\finance\src\main\java RuoYi-Vue\integration\src\main\java
rg -n "ObjectProvider|IProductDistributionService|IWarehouseService|IFinanceCurrencyService|IUpstreamSystemService|ProductSellerLookupService" RuoYi-Vue\seller\src\main\java RuoYi-Vue\buyer\src\main\java RuoYi-Vue\product\src\main\java RuoYi-Vue\warehouse\src\main\java
```

## 未验证原因

- 未执行 `mvn compile` / `mvn test` / 启动应用。
- 原因：本次任务明确为“只读审计”，且目标是定位 Maven/module 依赖 P0/P1，不是做构建产物验证。

## 权限检查结果

- 本次聚焦模块依赖与注入关系，未发现新增权限点审计对象。
- `seller/buyer` 门户商品 facade 仍通过各自终端 service 进入 `product`，未发现越过后端权限体系直接访问 `Mapper` 的情况。

## 字典/选项复用检查结果

- 本次范围不涉及字典/选项新增或复用变更，未见与依赖问题直接相关的异常。

## 复用台账检查结果

- 已读取 [`docs/architecture/reuse-ledger.md`](../architecture/reuse-ledger.md)。
- 台账中“product 是共享商品基础，不是第四终端”的约束，与本次发现一致；当前问题不是业务归属错误，而是共享模块内部查询/写入职责未进一步拆开。

## CodeGraph 更新结果

- 未执行 `codegraph sync .`。
- 原因：本次无代码修改，且用户要求只读审计。

## 大文件合理性判断结果

- [`RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java`](../../RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java) 约 1257 行，明显超过 `500` 行检查阈值。
- 从当前审计结果看，该文件同时承载：
  - 查询
  - 建品/改品
  - 上下架状态机
  - 调价
  - seller 快照装配
  - warehouse 校验
  - finance 币种校验
  - 操作日志
- 这已经不是单一职责文件，且直接对应本次 `P1-1/P1-2`。后续应优先按“查询 / 写入 / 校验装配 / 日志”拆分，而不是继续向该类堆逻辑。

## 重复代码检查结果

- `SellerPortalProductServiceImpl` 与 `BuyerPortalProductServiceImpl` 存在明显同构 facade 结构，但当前差异点仍然清晰：
  - seller：按 `subjectId` 限定自有商品
  - buyer：只暴露 `ON_SALE` 只读浏览
- 该重复更接近已确认模板化推进，不属于本次 `P0/P1` 主问题。
- 真正需要优先消除的不是 facade 级重复，而是它们共同依赖了过大的 `IProductDistributionService`。
