# 商城商品列表库存调整接入记录

## 目标

在商城商品列表中复用库存总览的库存调整能力，让运营可以从商品 SPU 或 SKU 行快速进入库存调整，不需要再跳转到库存总览页定位同一商品。

## 已确认规则

- 入口支持单行 SPU 和单行 SKU，不做批量调整。
- 草稿、待上架、已上架、已下架都允许调整库存。
- 停用是独立管控状态，可以理解为管理侧下架；停用商品不在买家商品中心展示，但仍允许管理端调整库存。
- 调整库存只走库存模块的库存流水和校验，不新增商城商品操作日志。
- 权限沿用库存总览调整权限：`inventory:overview:query` + `inventory:overview:adjust`。

## 实现范围

- 抽取共享组件：`react-ui/src/components/InventoryAdjust/InventoryAdjustButton.tsx`。
- 库存总览页继续复用该共享组件，原组件路径保留为 re-export，避免影响已有页面入口。
- 商城商品列表在 SPU 视图、SKU 视图和展开 SKU 行的“更多”菜单里加入“调整库存”。
- 商品列表的“更多”菜单显式使用 Ant Design `Dropdown` 的点击触发，避免 hover 触发在表格固定列里交互不稳定。
- 同步 TSX 和 JS 镜像，避免 dev server 读取同名 JS 时出现页面逻辑不一致。

## 验证

- `npm run tsc -- --pretty false` 通过。
- `npx biome lint ...` 覆盖新增共享组件、库存总览页、商品列表页 TSX/JS 文件，通过。
- 浏览器验证 `http://127.0.0.1:8001/product/distribution`：
  - SPU 行“更多”菜单显示“调整库存”。
  - 点击 SPU “调整库存”后弹出库存调整弹窗，并加载 SKU 仓库明细行。
  - 切换 SKU 视图后，SKU 行“更多”菜单显示“调整库存”。
