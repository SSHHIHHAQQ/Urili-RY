# 来源商品库全局列表样式修复执行记录

日期：2026-06-05

## 目标

修复来源商品库列表样式异常，并按“领星 SKU 同步清单”的列表效果沉淀为可复用全局样式。

本次不修改数据库、不调整接口业务逻辑、不改来源 SKU 同步规则。

## 问题

- 前一版全局样式直接覆盖了 Ant Design 表格内部节点，导致横向表格的真实鼠标滚轮纵向滚动异常。
- 筛选区内部被限制高度并产生滚动，展开/收起状态不符合 Ant Design Pro 原生表现。
- 表格内容区贴近筛选区，且固定表头与右侧操作列存在跑出表格框的风险。
- ProTable 直接写 `pagination={{ pageSize: ... }}` 时，每页条数下拉会被受控 `pageSize` 锁住，切换不生效。

## 对照实现

已对照 `react-ui/src/pages/UpstreamSystem/components/SkuSyncPanel.tsx` 和 `react-ui/src/pages/UpstreamSystem/style.css`：

- “领星 SKU 同步清单”使用 `styles.fillTable + upstream-fill-table` 让 ProTable 外层撑满。
- 表格卡片取消边框、阴影和 card body padding。
- 工具栏隐藏，分页保留在列表底部。
- 它没有启用 Ant Design fixed-header，因此不能把来源商品库的固定表头内部结构规则直接套到所有上游同步清单。

## 修复内容

- 新增全局 opt-in 列表样式 `urili-fill-table`，来源商品库显式套用。
- 保留 `upstream-fill-table` 的既有效果，并把无边框、撑满、隐藏工具栏规则沉淀到全局。
- 固定表头结构只作用于 `.urili-fill-table .ant-table.ant-table-fixed-header`，不再全局覆盖所有 `.ant-table-container` / `.ant-table-content` / `.ant-table-body`。
- 来源商品库启用 `scroll={{ x: 2050, y: '100%' }}`，让表头和表体分离，滚动只发生在表体。
- 移除筛选区 `max-height` 和内部 `overflow: auto`，恢复展开态自然增高。
- 移除筛选表单项上额外的 `display: flex`，避免干扰 ProTable 原生展开/收起隐藏逻辑。
- `getPersistedProTableSearch` 默认收起展示数调整为 5，宽屏下形成 5 个筛选项 + 操作区，避免 6 列布局中动作区换行异常。
- 新增并复用 `getProTablePagination(...)`，统一开启每页条数切换。

## 复用台账

已更新 `docs/architecture/reuse-ledger.md`：

- `getPersistedProTableSearch(...)` 和 `getProTablePagination(...)` 作为标准 ProTable 筛选与分页入口。
- 需要复用“领星 SKU 同步清单”这类无边框、撑满、底部分页列表效果时，在 ProTable 上加 `className="urili-fill-table"`。
- 禁止继续用粗粒度全局规则覆盖所有 Ant Design 表格内部滚动节点。

## 验证命令

- `npx biome lint src/global.css src/utils/proTableSearch.ts src/pages/Product/SourceProductLibrary/index.tsx src/pages/UpstreamSystem/components/SkuSyncPanel.tsx src/pages/UpstreamSystem/components/SyncTabs.tsx`
  - 结果：通过。
- `npm run tsc`
  - 目录：`react-ui`
  - 结果：通过。

## 浏览器验证

使用 Playwright CLI 验证 `http://127.0.0.1:8001`，账号 `admin / admin123`。

### 来源商品库 1280x720

- 路径：`/product/list`
- 收起态：
  - 可见筛选项：6，包括 5 个字段和操作区。
  - 操作文案：`重 置|查 询|展开`。
  - 筛选区高度：160。
  - 表格区域：top 296，height 379。
  - 表头：top 296，height 47。
  - 表体：top 343，height 332，overflow `auto/auto`。
  - 分页器：bottom 712，位于 720 高度视口内。
- 鼠标滚轮验证：
  - 表体 `scrollTop = 500`。
  - 页面 `scrollTop = 0`。
  - 表头 top 仍为 296，确认滚动发生在表体。

### 来源商品库 2560x1440

- 收起态：
  - 可见筛选项：6，包括 5 个字段和操作区。
  - 表格区域：top 240，height 1151，right 2520。
  - 表头：top 240，height 47。
  - 表体：top 287，height 1104，overflow `auto/auto`。
  - 分页器：bottom 1428。
  - fixed header：已启用。
- 鼠标滚轮验证：
  - 表体 `scrollTop = 276`。
  - 页面 `scrollTop = 0`。
  - 表头 top 仍为 240。
- 截图证据：`react-ui/.playwright-cli/page-2026-06-05T04-44-54-929Z.png`。

### 领星 SKU 同步清单 2560x1440

- 路径：`/overseas-warehouse-service/upstream-system`
- 活动页签：`领星SKU同步清单`。
- 可见 `upstream-fill-table` 数量：1。
- 表格区域：top 516，height 858。
- 分页器：bottom 1411。
- fixed header：未启用，确认没有被来源商品库固定表头规则误伤。
- 表格 overflow：`auto/auto`。

## 权限、字典和数据影响

- 权限检查：本次只改前端样式、分页配置和记录文件，不新增接口或权限点。
- 字典/选项复用检查：本次未新增字典或业务选项。
- 数据影响：未执行 DDL/DML，未写入远端 MySQL 或 Redis。
- 重复代码检查：已把分页配置抽到 `getProTablePagination(...)`，并在来源商品库、上游系统同步列表和审计弹窗中复用。

## CodeGraph

- 命令：`codegraph sync .`
- 结果：通过，输出 `Already up to date`。
