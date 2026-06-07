# 来源仓库库存区间筛选样式备选

日期：2026-06-07

## 目标

正式列表页原先直接使用 `digitRange` 展示 4 个库存区间，视觉过重且不够规整。本次先做独立预览页提供多种 Ant 风格方案供确认；确认后正式页面采用方案 B，并移除临时预览路由。

## 临时预览

预览阶段曾使用临时路由：

```text
http://127.0.0.1:8001/inventory/source-warehouse-stock/range-filter-preview
```

该路由只用于样式确认，方案 B 落地后已从 `react-ui/config/routes.ts` / `react-ui/config/routes.js` 中移除，预览页面文件也已删除。

预览截图保留为决策记录：`docs/plans/2026-06-07-source-warehouse-stock-range-filter-preview.png`

## 方案

### 方案 A：Ant 原生区间

- 控件：`ProFormDigitRange`
- 优点：改动最小，和 ProTable 查询表单兼容度高。
- 缺点：4 个区间同时展示时仍偏重，视觉上容易显得散。

### 方案 B：紧凑行内区间

- 控件：`Select` + `Input` + `Space.Compact` + `InputNumber`
- 优点：在列表筛选区内比较稳，主仓/仓库/SKU 与库存区间关系清楚，输入框边界规整。
- 缺点：仍然占用两行筛选区域。

### 方案 C：Popover 高级筛选

- 控件：`Popover` + `Form` + `Space.Compact` + `InputNumber` + `Tag`
- 优点：正式页面最省空间，主筛选区干净，已选库存区间可以用 Tag 摘要展示。
- 缺点：需要多一次点击才能编辑区间。

### 方案 D：条件构建器

- 控件：`Select` + `Space.Compact` + `InputNumber` + `Tag`
- 优点：适合后续库存筛选指标继续增加，比如箱内库存、库龄、销量等。
- 缺点：对当前只有 4 个固定区间来说交互稍重。

## 当前建议

已确认采用方案 B：正式页仍使用原 ProTable 筛选区，不单独新增筛选区；4 个库存区间在原筛选网格内使用 `Space.Compact + InputNumber` 呈现最小值和最大值，保留 Ant 原生输入框和按钮样式。

落地边界：

- 「来源主仓」「来源仓库」继续留在 ProTable 查询区，并保持可搜索下拉。
- 「总库存数」「可用库存数」「锁定库存数」「在途库存数」作为 ProTable 查询字段展示，每个字段占 2 个筛选格。
- 区间字段跟随 ProTable 原有「查询」「重置」按钮，提交后在 ProTable `request` 入口统一转换为后端 `*Min` / `*Max` 参数。
- 不新增后端接口，本次只调整正式页面区间筛选交互。

## 验证

- 预览阶段已验证 A/B/C/D 四个方案均渲染，并生成截图记录。
- 方案 B 落地后执行 `npx biome lint src/pages/Inventory/SourceWarehouseStock/index.tsx config/routes.ts config/routes.js`。
- 方案 B 落地后执行 `npm run tsc -- --pretty false`。
- 浏览器打开正式路由，确认库存区间在原 ProTable 筛选区内渲染、原查询按钮触发区间查询，并确认临时预览路由不再作为正式菜单入口保留。
