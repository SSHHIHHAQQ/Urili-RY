# 商城商品库存读取边界与 SKU 视图实现记录

## 目标

本次只实现两个能力：

1. 管理端「商品管理 / 商城商品列表」展示库存读取边界。
2. 在同一页面提供 SKU 维度列表，方便按系统 SKU、客户 SKU、商品标题、卖家、类目和状态排查。

运费逻辑本次不做。

## 库存读取边界

当前仓库模块只有仓库主数据，库存模块尚未落地，项目内还没有可作为事实源的库存表。因此本次不新增库存表、不模拟库存、不写假库存数据。

本次只在商品接口 DTO 和 SQL 查询结果中预留只读字段：

- `availableStock`：可售库存聚合值。
- `warehouseCount`：有库存或参与供货的仓库数。
- `inventoryStatus`：库存状态。
- `stockUpdateTime`：库存侧最后同步或调整时间。

当前 SQL 统一返回 `null`，前端展示为 `--`。后续库存模块落地后，只需要把 `ProductDistributionMapper.xml` 中的 `null as available_stock`、`null as warehouse_count` 等替换为库存模块聚合查询或稳定视图，不需要重新改前端列结构。

## SKU 维度列表

新增管理端 SKU 分页接口：

- `GET /product/admin/distribution-products/skus/list`
- 权限：复用 `product:distribution:list`
- 返回：`ProductSku` 列表，并带所属 SPU 快照字段。

支持的主要筛选：

- 系统 SKU
- 客户 SKU
- 系统 SPU
- 客户 SPU
- 商品标题
- 卖家
- 类目
- SKU 状态

页面实现：

- 同一菜单内增加 `SPU视图 / SKU视图` 切换。
- 状态 Tabs 继续按 `待上架 / 已上架 / 已下架 / 停用 / 草稿 / 全部` 的顺序过滤。
- SPU 视图保留原展开 SKU 明细。
- SKU 视图直接展示 SKU 分页列表。
- SKU 视图操作支持：
  - 查看所属 SPU。
  - 编辑商品，并携带 `skuId` 进入编辑页。
  - 切换 SKU 状态。
- 编辑页收到 `skuId` 后会高亮对应 SKU 行，降低新增 SKU 后的查找成本。

## 影响范围

后端：

- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/controller/AdminProductDistributionController.java`
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/ProductSpu.java`
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/ProductSku.java`
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/mapper/ProductDistributionMapper.java`
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductDistributionService.java`
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java`
- `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml`

前端：

- `react-ui/src/pages/Product/Distribution/index.tsx`
- `react-ui/src/pages/Product/Distribution/EditPage.tsx`
- `react-ui/src/pages/Product/Distribution/components/SkuMatrixEditor.tsx`
- `react-ui/src/pages/Product/Distribution/style.module.css`
- `react-ui/src/services/product/distributionProduct.ts`
- `react-ui/src/types/product/distribution-product.d.ts`

测试 stub：

- `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImplTest.java`
- `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImplTest.java`

## 不涉及

- 不新增库存表。
- 不新增仓库绑定表。
- 不执行数据库 DDL。
- 不直接执行数据库 DML。
- 不回填历史数据。
- 不实现运费计费。

## 验证记录

已执行：

- `mvn -pl product,seller,buyer -am -DskipTests test-compile`
  - 结果：通过。
  - 说明：覆盖 product 主代码编译，以及 seller/buyer 测试 stub 编译。
  - 编译输出包含既有 Java 17 模块路径提示和弃用 API 提示，未出现编译失败。
- `npm run tsc -- --pretty false`
  - 结果：通过。
- `codegraph sync .`
  - 结果：通过。
  - 输出摘要：`Synced 3 changed files`，`Modified: 3 - 107 nodes in 1.6s`。
- `mvn -pl ruoyi-admin -am -DskipTests package`
  - 第一次结果：失败，原因是当前后端进程锁定 `ruoyi-admin.jar`，无法重命名为 `.jar.original`。
  - 处理：停止本机 8080 监听的旧后端进程。
  - 第二次结果：通过。
- `.\start-backend-local.ps1 -Restart`
  - 结果：通过，后端按本机运行配置重新启动。
- `GET /product/admin/distribution-products/skus/list?pageNum=1&pageSize=5`
  - 结果：通过。
  - 返回摘要：`code=200`，`total=69`，首条 SKU 带所属商品字段，库存字段为 `null`。
- 浏览器验证 `http://127.0.0.1:8001/product/distribution`
  - 结果：通过。
  - SPU 视图可见 `总可售库存 / 仓库数 / 库存状态` 列，当前展示 `--`。
  - SKU 视图可见 `系统SKU / 客户SKU / SKU规格 / 商品标题 / 可售库存 / 仓库数 / 库存状态 / 查看SPU / 编辑商品`，列表有真实 SKU 数据。
