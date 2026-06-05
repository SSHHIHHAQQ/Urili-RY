# 商城商品列表状态流转、调价与操作日志实现记录

## 目标

本次实现管理端「商城商品列表」已确认的状态和价格管理能力：

- 商品销售流转按 `待上架 / 已上架 / 已下架 / 草稿` 管理，并在列表页按 `待上架、已上架、已下架、停用、草稿、全部` 展示 Tabs。
- `停用` 作为独立管控状态，不混入销售流转；停用后可从停用 Tab 执行恢复。
- 列表页同时支持 SPU 视图和 SKU 视图，视图切换使用 Ant Design `Radio.Button` 放在表格工具栏。
- 新增和编辑页不再维护 SKU 销售价；销售价改为在列表页通过调价弹窗设置。
- 增加商品分发操作日志，记录状态调整、停用/恢复、SKU 调价等操作。

## 数据库与接口

- 新增 SQL：`RuoYi-Vue/sql/20260605_product_distribution_status_price_log.sql`。
- 新增 `product_distribution_operation_log` 操作日志表。
- `product_spu`、`product_sku` 增加独立管控字段：`control_status`、`control_reason`、`control_by`、`control_time`、`recover_by`、`recover_time`。
- `product_sku.sale_price` 调整为可空，新增/编辑阶段允许不填销售价。
- 已按当前运行配置连接目标库执行迁移 SQL，执行语句数 13，字段存在性和 `sale_price` 可空性已复核通过。

后端新增和调整的主要能力：

- SPU 批量销售状态调整：草稿到待上架、待上架到已上架、已上架到已下架。
- SKU 批量销售状态调整：遵循同一销售流转边界。
- SPU/SKU 批量停用与恢复：使用独立 `control_status`。
- SKU 批量调价：按选中的 SKU 写入销售价，并记录操作日志。
- 操作日志分页查询：`/product/admin/distribution-products/operation-logs/list`。

## 前端实现

- `react-ui/src/pages/Product/Distribution/index.tsx`
  - 状态 Tabs 按确认顺序展示。
  - SPU/SKU 视图切换移动到表格工具栏，使用 `Radio.Button`。
  - 不同 Tab 下展示对应批量动作，例如批量上架、批量下架、提交待上架、批量停用、恢复。
  - SKU 视图支持直接检索 SKU，并展示 `颜色：白色 / 尺寸：M` 这种规格名和值。
  - SKU 尺寸重量展示为 `32.00 x 24.00 x 6.00 cm 0.47 kg` 结构。
  - 增加「调整售价」弹窗和「操作日志」抽屉。
- `react-ui/src/pages/Product/Distribution/EditPage.tsx`
  - 新增/编辑页移除销售价维护入口，保留供货价和币种相关展示边界。
- `react-ui/src/pages/Product/Distribution/components/ProductDistributionOperationLogDrawer.tsx`
  - 新增操作日志抽屉，使用 Ant Design Pro 表格查询布局。

## 验证

已执行：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl product,seller,buyer -am -DskipTests test-compile
```

结果：通过，`product`、`seller`、`buyer` 及依赖模块 test-compile 均成功。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc -- --pretty false
```

结果：未通过，但失败点在非本次商品列表文件 `react-ui/src/pages/Warehouse/components/WarehouseFields.tsx:135` 和 `:161`，类型错误为 `FormInstance<any>` 可能被返回为 `ReactNode`。本次商品列表文件未出现新的 TypeScript 报错。

浏览器验证：

- 登录 `http://127.0.0.1:8001/product/distribution` 成功。
- SPU 列表接口 `GET /api/product/admin/distribution-products/list?spuStatus=READY&controlStatus=NORMAL&pageNum=1&pageSize=20` 返回 200。
- SKU 列表接口 `GET /api/product/admin/distribution-products/skus/list?skuStatus=READY&controlStatus=NORMAL&pageNum=1&pageSize=20` 返回 200，页面显示 28 条 SKU。
- SKU 视图可见规格名和值，例如 `颜色：米白 / 尺寸：80x160cm`。
- SKU 视图可见尺寸重量，例如 `32.00 x 24.00 x 6.00 cm 0.47 kg`。
- 选中 SKU 后可打开「调整售价」弹窗，弹窗展示按供货价加价、按当前售价调整、统一设置售价三种方式。
- 「操作日志」抽屉可打开，接口 `GET /api/product/admin/distribution-products/operation-logs/list?pageSize=10&pageNum=1` 返回 200。
- 浏览器控制台 `warning` 和 `error` 均为 0。

## 已知非本次问题

- 外链 Unsplash 图片在浏览器中出现 `net::ERR_BLOCKED_BY_ORB`，属于外部图片加载限制，不影响商品接口和页面交互。
- 前端全量 `tsc` 当前被仓库模块 `WarehouseFields.tsx` 阻塞，未在本次商品列表任务中修改。

## CodeGraph

已执行：

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：通过，输出摘要为 `Synced 3 changed files`，`Modified: 3 - 61 nodes in 505ms`。
