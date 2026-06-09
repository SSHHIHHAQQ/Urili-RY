# 2026-06-09 共享域模块边界只读扫描

## 扫描范围

- 仓库：`E:\Urili-Ruoyi`
- 目标模块：`product` / `inventory` / `integration` / `warehouse` / `finance`
- 扫描口径：三端独立，快速推进仅关注 P0/P1
- 重点检查：
  - 跨模块 `mapper` / `service.impl` 直连
  - `inventory` 直接读 `product` / `integration` / `upstream` 事实表
  - `product` 直接读 `integration` / `warehouse` 事实表
  - `warehouse` 直接读 `upstream_system_*`
  - `integration` 反向依赖 `product` / `warehouse` internals

## 规则基线

- `AGENTS.md:139-140`：`Service` 不应绕过业务模块边界直接读写其他模块表实现细节；跨模块调用应通过 `Service` / `Facade` / 明确公共接口。
- `AGENTS.md:149`：共享业务模块至少区分 `product` / `inventory` / `finance` / `integration` 等独立模块。
- `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md:18`：当前 P0/P1 只处理编译、guard、接口、权限、串端、service/字段缺失，以合同/单测和 fail-closed 校验为准。

## P0/P1 发现

### 1. 本轮未发现 P0/P1 共享域模块边界违规

当前工作树中，目标范围没有发现以下高优先级问题：

- 跨模块直接 import 对方 `mapper` 或 `service.impl`
- `inventory` 直接查询 `source_warehouse_stock_*` / `upstream_system_*`
- `product` 直接查询 `source_product_*` / `source_warehouse_*` / `upstream_system_*`
- `warehouse` 直接依赖 `integration` 内部实现或直接读 `upstream_system_*`
- `integration` 直接依赖 `product` / `warehouse` 的 `mapper` 或 `service.impl`

## 证据

### 代码证据

- `integration` 通过公开端口对接库存/仓库，而不是反向读对方 internals：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java:70`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java:73`
- `inventory` 通过 inventory-owned 端口读取商品快照与来源仓库存切片：
  - `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java:73`
  - `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java:76`
- `product` 通过公开 service/port 读取库存汇总与来源 SKU 配对投影，而不是直读 integration 事实表：
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java:122`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java:128`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java:131`
- `product` 的库存汇总 SQL 已切到 inventory read model，而不是直拼来源仓事实：
  - `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml:241`
  - `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml:253`
- `inventory` 的商品身份/来源 key 发现已下沉到 inventory-owned port，由 `product` 实现该端口：
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductInventoryLookupServiceImpl.java:18`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductInventoryLookupServiceImpl.java:80`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductInventoryLookupServiceImpl.java:90`
- `warehouse` 的上游仓配对读取已走 integration projection port：
  - `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java:63`
  - `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java:66`
  - `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java:484`

### 契约测试证据

- `integration` 禁止直接依赖 `product` / `warehouse` internals，并要求通过 public port：
  - `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/architecture/IntegrationModuleBoundaryContractTest.java:18`
  - `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/architecture/IntegrationModuleBoundaryContractTest.java:73-79`
  - `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/architecture/IntegrationModuleBoundaryContractTest.java:107-116`
- `inventory` 禁止 import 跨模块 `mapper` / `service.impl`，资源层禁止直读 `source_warehouse_stock_*` / `upstream_system_*`：
  - `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryModuleBoundaryContractTest.java:18`
  - `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryModuleBoundaryContractTest.java:60`
- `inventory` 对“来源连接 -> source key -> product binding -> spu”链路已有边界约束：
  - `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventorySourceWarehouseStockBoundaryContractTest.java:89`
  - `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventorySourceWarehouseStockBoundaryContractTest.java:118`
  - `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventorySourceWarehouseStockBoundaryContractTest.java:138-153`
  - `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventorySourceWarehouseStockBoundaryContractTest.java:206-224`
  - `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventorySourceWarehouseStockBoundaryContractTest.java:271-273`
- `warehouse` 已有“禁止直读 upstream storage，必须走 integration projection port”的契约：
  - `RuoYi-Vue/warehouse/src/test/java/com/ruoyi/warehouse/architecture/WarehouseModuleBoundaryContractTest.java:47`
  - `RuoYi-Vue/warehouse/src/test/java/com/ruoyi/warehouse/architecture/WarehouseModuleBoundaryContractTest.java:150-155`
- `product` 已有“库存汇总只允许来自 inventory read model”“integration/warehouse 存储必须经 port”契约：
  - `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductDistributionMapperContractTest.java:245`
  - `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductDistributionMapperContractTest.java:251`
  - `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductDistributionMapperContractTest.java:464`
  - `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductDistributionMapperContractTest.java:492`

### 运行验证

- 已在当前工作树执行：

```powershell
mvn -pl product,inventory,integration,warehouse,finance -am "-Dtest=**/*ContractTest,**/*BoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- 结果：`BUILD SUCCESS`
- 相关模块边界契约全部通过：
  - `inventory`: `InventoryModuleBoundaryContractTest`, `InventorySourceWarehouseStockBoundaryContractTest`
  - `integration`: `IntegrationModuleBoundaryContractTest`
  - `warehouse`: `WarehouseModuleBoundaryContractTest`
  - `product`: `ProductModuleBoundaryContractTest`, `ProductDistributionMapperContractTest`

## 最小修复建议

本轮无必须落地的 P0/P1 修复。

后续继续推进这几个共享域模块时，最小守法动作如下：

1. 新增跨模块读取时，优先补 public port，不要回退到 `mapper` / `service.impl` import。
2. 新增 `Mapper.xml` 时，把 `source_warehouse_stock_*`、`source_product_*`、`upstream_system_*` 是否越界纳入对应模块契约测试。
3. 保持 `product -> inventory` 只读 read model、`inventory -> product/integration` 只读 lookup port、`warehouse -> integration` 只读 projection port 这三条边界不回退。
