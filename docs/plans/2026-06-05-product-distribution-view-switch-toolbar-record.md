# 商城商品列表视图切换工具栏调整记录

## 目标

- 将商城商品列表的 `SPU视图 / SKU视图` 从状态 Tabs 左侧移出，避免把“表格视图切换”和“商品状态筛选”混在同一语义区域。
- 按已确认方案使用 Ant Design `Radio.Button` 实现视图切换。

## 调整内容

- `react-ui/src/pages/Product/Distribution/index.tsx`
  - 移除原表头中的 `Segmented` 视图切换。
  - 保留状态 Tabs 作为表格左侧标题区内容。
  - 在 ProTable 右侧工具栏新增 `Radio.Group` + `Radio.Button`，位于 `新增商品` 按钮之前。

## 边界

- 未修改后端接口。
- 未修改数据库结构。
- 未修改权限标识。
- 未修改 SPU / SKU 列表数据口径。
- 未新增公共组件；本次调整仅为当前页面局部 UI 布局修正。

## 验证

- `npm run tsc -- --pretty false`：通过。
- Playwright 浏览器验证：通过。
  - 登录管理端后进入 `http://127.0.0.1:8001/product/distribution`。
  - 确认状态 Tabs 位于表格左侧标题区。
  - 确认 `SPU视图 / SKU视图` 使用 `Radio.Button` 位于右侧工具栏，并位于 `新增商品` 按钮左侧。
  - 切换到 SKU 视图后列表正常加载；再切回 SPU 视图后列表正常加载。
  - 截图：`react-ui/output/playwright/product-distribution-view-switch-toolbar.png`。
