# 管理端 ProTable 全局布局修复记录

## 背景

本次处理用户反馈的四个列表布局问题：

- 属性库筛选区展开/收起后，查询按钮换到第二行，视觉上与筛选区和工具栏挤在一起。
- 上游系统管理的「领星SKU同步清单」表头跟随数据行一起滚动。
- 商品管理的「来源商品库」页面级列表失去 Ant Design Pro 默认白色卡片背景。
- 商品属性配置的「类目属性模板」出现外层表格和表体双滚动条。

## 根因

- 之前把 `urili-fill-table` 做成了“撑满 + 去卡片”的复合语义，导致页面级来源商品库也被移除了卡片边距和视觉背景。
- 领星同步清单只配置了横向 `scroll.x`，没有配置 `scroll.y`，Ant Design 没有生成 fixed-header 结构；全局样式又让 `.ant-table` 成为滚动容器，因此表头和数据一起滚动。
- 类目属性模板既有全局表格滚动规则，又有组件私有 CSS 直接覆盖 `.ant-table` / `.ant-table-container` / `.ant-table-body`，最终形成双滚动层。
- 超宽屏下一行 6 个筛选字段刚好占满 24 栅格，Ant Design Pro 查询动作区域只能换到第二行。
- Ant Design Pro 在无 toolbar 的 ProTable card body 上写入内联 `padding-block-start: 0px`，来源商品库这类无 toolbar 页面需要全局覆盖这个内联值，避免表格贴住卡片顶部。
- 来源商品库虽然配置了 `options.setting` 和 `options.density`，但同时使用 `toolBarRender={false}` 关闭了 ProTable toolbar，导致列设置、密度设置入口不渲染。
- 来源商品库展开态有 9 个筛选字段；`lg` 断点原来按 3 列展示时，9 个字段刚好占满 `3 x 3`，查询动作区只能被挤到第 4 行右侧。

## 修复内容

- `react-ui/src/utils/proTableSearch.ts`
  - 新增 `getProTableScroll(x, config?)`，统一为页面级 ProTable 提供 `scroll.x` 和默认 `scroll.y: '100%'`。
  - 新增 `getProTableColumnsState(persistenceKey, config?)`，统一使用 localStorage 持久化列显示状态。
  - 将默认收起态筛选字段数调整为 `5`，给查询动作区预留位置。
  - 将 `lg` 筛选断点从 3 列调整为 4 列，避免展开态 9 个字段占满三整行后把查询动作区挤到单独一行。
  - 保留 `getPersistedProTableSearch(...)` 和 `getProTablePagination(...)`，作为页面筛选、分页、滚动和列状态工具。

- `react-ui/src/global.css`
  - 页面级 ProTable 保留 Ant Design Pro 默认白色卡片。
  - 全局固定 ProTable 表头滚动结构：`.ant-table-fixed-header` 只让 `.ant-table-body` 滚动。
  - 无 toolbar 的页面级 ProTable card body 补顶部内边距，避免列表贴顶。
  - `upstream-fill-table` 仅用于上游系统详情内部嵌套表格，移除内部卡片边框、阴影、toolbar 和内边距。

- 业务页面接入
  - 来源商品库、商城商品列表、商品分类、属性库、类目属性模板、上游系统同步清单、卖家管理、买家管理、币种配置等主列表接入 `getProTableScroll(...)`。
  - 来源商品库保留 ProTable toolbar，使用 `toolBarRender={() => []}` 避免隐藏密度和列设置入口。
  - 来源商品库接入 `getProTableColumnsState('source-product-library-columns')`，并给纯渲染列和操作列补稳定 `key`。
  - 类目属性模板移除组件私有 AntD 表格内部滚动覆盖，保留外层布局 CSS。
  - 弹窗、抽屉、展开行内的小表格不强行套页面撑满规则，继续按局部容器自适应。

## 页面验证

浏览器验证环境：

- 前端：`http://127.0.0.1:8001`
- 后端：`http://127.0.0.1:8080`
- 视口：`2560 x 720`

已验证页面：

- `/basic-config/product-attribute` 属性库
  - 搜索区高度约 87px。
  - 查询动作区与筛选字段同一行，`actions.top = row.top = 194`。
  - 表格为 fixed-header，滚动层为 `.ant-table-body`。

- `/product/list` 来源商品库
  - 页面级 ProTable 白色卡片恢复，card body padding 为 `12px 24px 16px`。
  - 表体滚动到 `scrollTop = 260` 后，表头位置保持不变。

- `/overseas-warehouse-service/upstream-system` 领星SKU同步清单
  - 当前 Tab 为「领星SKU同步清单」。
  - `.ant-table` overflow 为 `hidden`，`.ant-table-body` overflow 为 `auto`。
  - 表体滚动到 `scrollTop = 320` 后，表头 top 保持不变。

- `/basic-config/product-attribute` 类目属性模板
  - 两块规则表都为 fixed-header。
  - `.ant-table` 和 `.ant-table-container` overflow 为 `hidden`。
  - 只有 `.ant-table-body` overflow 为 `auto`，未再出现双滚动层。

- 抽查主业务列表
  - `/basic-config/product-category` 商品分类
  - `/product/distribution` 商城商品列表
  - `/partner/seller` 卖家管理
  - `/partner/buyer` 买家管理
  - `/finance/currency` 币种配置
  - 以上主列表均为 fixed-header，滚动层为 `.ant-table-body`，页面级白色卡片存在。

## 补充修复验证

针对来源商品库筛选动作区和表格设置入口，补充验证如下：

- 视口 `1536 x 768`，来源商品库收起态：
  - 默认显示 5 个筛选字段。
  - `重置 / 查询 / 展开` 与筛选字段同一行。
  - 搜索区底部到表格卡片顶部间距为 8px，未再压到表格上。

- 来源商品库 ProTable toolbar：
  - toolbar 正常渲染。
  - 设置入口数量为 3 个：刷新、密度、列设置。

- 密度设置：
  - 点击「紧凑」后，表格 class 变为 `ant-table-small`。

- 列设置：
  - 隐藏「仓库尺寸」后，表头中该列消失。
  - localStorage 写入 `source-product-library-columns`。
  - 刷新页面后，「仓库尺寸」仍保持隐藏，列状态持久化生效。

- 展开态断点回归：
  - `960 x 1024`：9 个筛选字段分 5 行，动作区与最后一个字段「配对状态」同一行。
  - `1180 x 1024`：9 个筛选字段分 3 行，动作区与最后一个字段「配对状态」同一行。
  - `1280 x 1024`：9 个筛选字段分 3 行，动作区与最后一个字段「配对状态」同一行。
  - `1366 x 1024`：9 个筛选字段分 2 行，动作区与最后一行字段同一行。
  - 以上视口下，筛选区底部到表格卡片顶部均保留 10px 间距，未再出现动作区单独悬在表格上方的问题。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/global.css src/utils/proTableSearch.ts src/pages/Product/SourceProductLibrary/index.tsx src/pages/UpstreamSystem/components/SkuSyncPanel.tsx src/pages/UpstreamSystem/components/SyncTabs.tsx src/pages/Product/Attribute/components/AttributeLibrary.tsx src/pages/Product/Attribute/components/CategoryAttributeTemplate.tsx src/pages/Product/Attribute/components/CategoryAttributeTemplate.css src/pages/Product/Category/index.tsx src/pages/Product/Distribution/index.tsx src/components/PartnerManagement/PartnerManagementPage.tsx src/pages/Finance/Currency/index.tsx src/pages/Finance/Currency/components/SyncSettingsPanel.tsx
npm run tsc
```

结果：

- `biome lint`：通过。
- `npm run tsc`：通过。

## CodeGraph

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：

- 通过。
- 同步 12 个变更文件。
- Added: 3, Modified: 9。

## 复用规则更新

已更新 `docs/architecture/reuse-ledger.md`：

- 新增 `getProTableScroll(x, config?)` 复用说明。
- 明确 `urili-fill-table` 只做页面级撑满，不去掉白色卡片。
- 明确 `upstream-fill-table` 只用于上游系统详情内部嵌套清单。
- 新增页面级 ProTable 必须优先使用 `getProTableScroll(x)`，不要再用单页 CSS 覆盖 AntD 表格内部滚动结构。

## 未纳入本次全局规则的场景

- 弹窗内 ProTable，例如属性选项管理、审核记录弹窗。
- 抽屉内表格，例如商品详情抽屉和汇率历史抽屉。
- 展开行内 SKU 明细表。

这些场景不占满页面主数据区，强行套页面级高度规则会增加局部滚动风险；如后续要统一弹窗/抽屉表格，再单独抽局部容器规范。
