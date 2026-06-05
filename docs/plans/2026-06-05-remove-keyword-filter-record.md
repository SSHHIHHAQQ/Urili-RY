# 全页面移除关键词筛选执行记录

## 目标

移除管理端列表页中通用“关键词”查询字段，避免大数据量下触发宽泛模糊查询造成卡顿；保留属性编码、名称、状态、类型等明确字段筛选。

## 已修复问题

- 删除以下页面或组件中的 `title: '关键词'` / `dataIndex: 'keyword'` ProTable 查询列：
  - `react-ui/src/pages/Product/Attribute/components/AttributeLibrary.tsx`
  - `react-ui/src/pages/Product/Attribute/components/categoryAttributeColumns.tsx`
  - `react-ui/src/pages/Product/Category/index.tsx`
  - `react-ui/src/pages/Product/Distribution/index.tsx`
  - `react-ui/src/pages/Product/SourceProductLibrary/index.tsx`
  - `react-ui/src/pages/Finance/Currency/index.tsx`
- 将 `categoryAttributeColumns.tsx` 中仅类型使用的 `ProColumns` 改为 `import type`，清理本次触达文件的 Biome 警告。

## 新增问题

- 未发现新增问题。

## 残留问题

- 后端仍保留 `keyword` 查询参数支持；本次只移除页面入口，不做后端契约破坏性删除。
- 选择器、弹窗和类目树中的局部远程搜索仍保留，它们不是截图中的页面级“关键词”筛选。

## 权限检查结果

- 本次未新增、删除或调整接口、按钮、菜单和权限标识。
- 原有按钮权限控制未改动。

## 字典/选项复用检查结果

- 原有 `SEARCHABLE_SELECT_PROPS`、字典选项和 `valueEnum` 复用未改动。
- 本次没有新增字典或内联选项。

## 复用台账检查结果

- 已检查 `docs/architecture/reuse-ledger.md` 中 ProTable 筛选区复用规则。
- 本次继续保留 `getPersistedProTableSearch`、`getProTableScroll`、`getProTablePagination` 等公共能力，仅移除具体页面的高成本通用查询列。
- 未新增公共组件或公共工具，无需追加复用台账条目。

## 大文件合理性判断结果

- `AttributeLibrary.tsx` 仍超过 400 行，但本次只删除 5 行查询列，未增加职责。
- 其他触达文件均为小范围删除或类型导入修正，不需要拆分。

## 重复代码检查结果

- `rg -n '关键词' react-ui/src` 已无结果。
- `rg -n "dataIndex: 'keyword'" react-ui/src` 已无结果。
- 本次未新增重复逻辑。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
npx biome lint src/pages/Finance/Currency/index.tsx src/pages/Product/Attribute/components/AttributeLibrary.tsx src/pages/Product/Attribute/components/categoryAttributeColumns.tsx src/pages/Product/Category/index.tsx src/pages/Product/Distribution/index.tsx src/pages/Product/SourceProductLibrary/index.tsx
```

```powershell
cd E:\Urili-Ruoyi
rg -n '关键词' react-ui/src
rg -n "dataIndex: 'keyword'" react-ui/src
codegraph sync .
```

## 验证结果

- `npm run tsc`：通过。
- 目标文件 `npx biome lint ...`：通过。
- `rg -n '关键词' react-ui/src`：无结果。
- `rg -n "dataIndex: 'keyword'" react-ui/src`：无结果。
- 浏览器验证 `http://127.0.0.1:8001/basic-config/product-attribute`：通过；属性库筛选区保留“属性编码、属性名称、属性类型、选项来源、状态”，不再显示“关键词”。

## 未验证原因

- 未执行后端编译或接口测试；本次未修改后端代码。
- 未逐页浏览所有菜单；已通过源码全局搜索覆盖 `react-ui/src` 中所有“关键词”页面筛选残留。

## CodeGraph 更新结果

- 已执行 `codegraph sync .`。
- 结果：同步完成。
