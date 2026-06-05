# 商品分类搜索模式展开重复修复记录

## 问题

商品分类配置页搜索 `women_clothing` 后，搜索结果同时包含一级、二级、三级类目。展开二级类目后，前端又请求该二级类目的子级并合并到当前搜索结果，导致三级类目重复出现；反复收起再展开时重复行继续累加。

## 原因

- `/product/admin/categories/search` 返回扁平搜索结果。
- 前端搜索模式仍按树表格处理结果。
- `normalizeCategoryTableRows` 会按 `childrenCount` 给有子级的搜索结果生成“加载中”占位子节点。
- 表格展开时继续调用 `/categories/children`，把本来已在扁平搜索结果中的子级再次合并进当前行。

## 方案

采用“搜索模式禁用展开”：

- 搜索模式下不生成占位子节点。
- 搜索模式下 `rowExpandable` 返回 false，不展示展开按钮。
- 搜索结果继续展示完整路径，方便定位类目。
- 非搜索模式保持原懒加载树逻辑。

## 修改

- `react-ui/src/pages/Product/Category/index.tsx`
  - `normalizeCategoryTableRows` 增加 `disableExpand` 参数。
  - 搜索模式调用 `normalizeCategoryTableRows(resp.rows || [], true)`。
  - `expandable.rowExpandable` 在搜索模式下禁用展开。
  - `onExpand` 增加搜索模式保护。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- 浏览器打开 `http://127.0.0.1:8001/basic-config/product-category`。
- 搜索分类编码 `women_clothing`：
  - 结果行展示完整路径。
  - 搜索结果不再出现“展开行”按钮。
  - 网络请求只有初始顶级类目请求和搜索请求，没有展开子级请求。
  - 控制台错误 0，警告 0。

## CodeGraph

- 已执行 `codegraph sync .`，结果：Already up to date。
