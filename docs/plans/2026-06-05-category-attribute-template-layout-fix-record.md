# 类目属性模板布局修复执行记录

日期：2026-06-05

## 1. 目标

修复商品属性配置中“类目属性模板”页签在不同窗口高度下的布局问题：

- 矮屏时“继承预览”不能被本类目规则和筛选区挤出可视区域。
- 高屏时右侧规则列表区域需要填满底部，不能留下大块空白。
- 左侧类目树和右侧规则区需要使用同一工作区高度，滚动只发生在类目树和表格数据体内部。

## 2. 实现范围

本次只修改管理端前端 `react-ui`，未修改后端接口、数据库、菜单权限或业务数据。

## 3. 修改文件

- `react-ui/src/pages/Product/Attribute/components/CategoryAttributeTemplate.tsx`
- `react-ui/src/pages/Product/Attribute/components/CategoryAttributeTemplate.css`
- `react-ui/src/pages/Product/Attribute/components/CategoryTreeFilterPanel.tsx`
- `react-ui/src/pages/Product/Attribute/components/AttributeOptionManager.tsx`
- `react-ui/src/utils/proTableSearch.ts`

## 4. 处理结果

- 新增当前页专用布局 CSS，使用 `product-category-attribute-template` 前缀，避免影响其他 ProTable 页面。
- 取消右侧自然流布局，改为固定工作区内的纵向 flex：上方“本类目规则”固定比例，下方“继承预览”吃掉剩余高度。
- 两个 ProTable 均设置内部滚动，表格体滚动，不再让整页滚动或把另一个表格挤出屏幕。
- “继承预览”筛选区默认折叠，并使用新的折叠状态 key，避免旧 localStorage 中展开状态继续影响高度。
- `getPersistedProTableSearch(...)` 支持页面传入 `defaultCollapsed` 作为无本地记录时的默认值。
- 当前商品属性页内的 AntD 6 警告一并处理：`Space direction` 改为 `orientation`，属性选项弹窗 `destroyOnClose` 改为 `destroyOnHidden`。

## 5. 验证结果

### 5.1 代码检查

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome check --write src/pages/Product/Attribute/components/CategoryAttributeTemplate.tsx src/pages/Product/Attribute/components/CategoryAttributeTemplate.css src/pages/Product/Attribute/components/CategoryTreeFilterPanel.tsx src/pages/Product/Attribute/components/AttributeOptionManager.tsx src/utils/proTableSearch.ts
npm run tsc
```

结果：通过。

### 5.2 浏览器验证

验证地址：

```text
http://127.0.0.1:8001/basic-config/product-attribute
```

验证方式：登录 `admin / admin123` 后切换到“类目属性模板”页签，使用 Playwright 测量布局。

2048 x 1152 高屏结果：

- 工作区高度：952px，底部到 1140px。
- 左侧类目面板高度：952px，底部到 1140px。
- 右侧规则区高度：952px，底部到 1140px。
- 继承预览 frame 高度：550px，表格体高度：263px。
- 页面级纵向溢出：0。

2048 x 768 矮屏结果：

- 工作区高度：574px，底部到 758px。
- 左侧类目面板高度：574px，底部到 758px。
- 右侧规则区高度：574px，底部到 758px。
- 本类目规则 frame 高度：260px，表格体高度：44px。
- 继承预览 frame 高度：304px，表格体高度：69px。
- 页面级纵向溢出：0。

结论：高屏能填满底部，矮屏能显示继承预览，滚动被限制在内部区域。

### 5.3 CodeGraph

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：通过，输出 `Synced 1 changed files`。

## 6. 未验证项

- 未执行后端编译，因为本次未修改后端代码。
- 未新增自动化测试，本次为布局修复，已通过 TypeScript、Biome 和浏览器测量覆盖。
