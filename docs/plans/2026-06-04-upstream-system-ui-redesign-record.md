# 上游系统管理页面 UI 优化记录

日期：2026-06-04

## 当前目标

把“上游系统管理”从松散的卡片/表格堆叠，调整为更适合后台高频操作的工作台页面：左侧主仓接入列表固定占满高度，右侧概要和同步清单按剩余可视区域铺满；即使仓库映射只有 1 条数据，也不能让页面下半屏暴露大面积空白背景。

## 新增问题

- 用户反馈现有页面“又丑又乱”，主要问题是右侧内容区没有铺满，表格工具按钮漂在空白区域，左侧主仓列表像小卡片而不是工作台侧栏。
- 宽屏场景下数据少时，表格分页器跟随数据停在中上部，剩余空间缺少明确的数据容器边界。

## 已修复问题

- `react-ui/src/pages/UpstreamSystem/index.tsx`：主页面改为左右 flex 工作区，移除依赖内容高度的 grid/Space 包裹。
- `react-ui/src/pages/UpstreamSystem/components/ConnectionSidebar.tsx`：左侧主仓接入改为固定宽度、占满高度、带底部数量区的侧栏。
- `react-ui/src/pages/UpstreamSystem/components/ConnectionSummary.tsx`：右侧基本信息从 `Descriptions` 改成标题、操作区和指标网格，减少信息散乱。
- `react-ui/src/pages/UpstreamSystem/components/SyncTabs.tsx`：Tabs 和 ProTable 改为占满剩余高度，关闭嵌套 ProTable 默认漂浮工具区。
- `react-ui/src/pages/UpstreamSystem/components/SkuSyncPanel.tsx`：SKU 同步状态、筛选和表格合并到同一个可撑满的数据面板。
- `react-ui/src/pages/UpstreamSystem/components/SkuSyncPanel.tsx`：二次收敛 SKU 工具区，移除灰色筛选容器和过重边框，保留轻量状态行 + 筛选行。
- 新增 `react-ui/src/pages/UpstreamSystem/style.module.css` 和 `style.css`，集中维护本页布局和 Ant 组件深层样式。

## 残留问题

- 当前仅优化管理端页面视觉和布局，没有调整业务接口、权限点、数据库表或同步逻辑。
- `SyncTabs.tsx` 仍为 300 行以上，后续如果继续增加页签或列操作，应优先拆出仓库/物流/日志列配置。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome check --write src/pages/UpstreamSystem
npm run tsc
npm run build
```

## 浏览器验证

- 通过菜单进入 `http://127.0.0.1:8001/overseas-warehouse-service/upstream-system`。
- 2048 × 1024 宽屏下，左侧主仓侧栏高度 872px，右侧详情高度 872px。
- 仓库同步清单只有 1 条数据时，表格区域高度 549px，分页器固定在底部。
- SKU 同步清单展示 5401 条数据，筛选区、表格滚动区和分页器位置正常。
- 二次简化后，SKU 工具区无灰色背景、无外框、无额外内边距，保留轻量状态行和筛选行。
- 验证截图：`E:\Urili-Ruoyi\logs\upstream-system-ui-simple-wide.png`。

## 未验证原因

- 本次没有重新验证后端编译和接口权限，因为变更范围只在 `react-ui/src/pages/UpstreamSystem` 的展示层，没有修改 API service 契约、权限标识或后端代码。

## 权限检查结果

- 页面仍沿用原有 `access.hasPerms('integration:upstream:*')` 控制按钮显示。
- 本次没有新增按钮权限点，也没有移除后端权限保护。

## 字典/选项复用检查结果

- 状态、类型、SKU 筛选字段仍集中使用 `constants.ts`。
- 没有新增内联状态映射，也没有新增需要进入 `sys_dict` 的字段。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md` 的“上游系统 React 页面组件”，登记本页布局样式入口和撑满规则。

## 大文件合理性判断结果

- `index.tsx` 约 329 行：当前仍是页面编排入口，业务已拆到组件，暂不继续拆分。
- `SyncTabs.tsx` 约 347 行：集中承载四个页签的表格列和请求逻辑，后续继续扩页签时应拆列配置。
- `style.module.css` 约 315 行：集中承载本页工作台布局样式，避免多组件散写 inline style；后续如果外部系统页面复用该模式，再考虑抽公共布局样式。

## 重复代码检查结果

- 没有复制新的 API 调用或状态映射。
- 本页撑满布局集中在 CSS 文件，避免继续在多个组件里重复写 `display:flex`、`min-height:0`、表格撑满等样式。
