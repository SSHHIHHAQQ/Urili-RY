# 上游系统管理主仓详情默认收起调整记录

## 目标

主仓顶部详情区包含授权状态、最近同步、最近授权、请求日志数、结算类型、Key、备注等低频信息。日常操作更关注左侧主仓选择、右侧同步清单和操作按钮，因此将该详情区改为默认收起，并提供手动展开/收起入口。

## 调整内容

- `react-ui/src/pages/UpstreamSystem/components/ConnectionSummary.tsx`
  - 新增本地 `detailsOpen` 状态，默认 `false`。
  - 主仓标题、启用状态、连接编码、系统类型和右侧业务操作按钮保持常驻展示。
  - 新增“展开详情 / 收起详情”按钮。
  - 切换 `connection.connectionCode` 时自动恢复收起状态，避免上一个主仓的展开状态带到下一个主仓。
- `react-ui/src/pages/UpstreamSystem/style.module.css`
  - 增加详情按钮的轻量样式，使其作为辅助入口，不抢占编辑、同步、授权等业务按钮的视觉优先级。

## 影响范围

- 仅前端展示状态变更。
- 不改接口契约。
- 不改数据库表、字典、权限点、菜单和后端逻辑。
- 不影响主仓同步、SKU 同步、请求日志和配对列表的数据加载。

## 验证记录

- `cd E:\Urili-Ruoyi\react-ui && npx biome check --write src/pages/UpstreamSystem`：通过，自动修正 1 个文件格式。
- `cd E:\Urili-Ruoyi\react-ui && npm run build`：通过。
- `cd E:\Urili-Ruoyi\react-ui && npm run tsc`：通过。
- 浏览器验证 `http://127.0.0.1:8001/overseas-warehouse-service/upstream-system`：
  - 初始进入页面显示“展开详情”，不显示“授权状态”“最近同步”等详情标签。
  - 点击“展开详情”后显示详情区和“收起详情”。
  - 点击“收起详情”后详情区隐藏。
  - 展开 CA012 后切换到 NY013，页面自动回到收起状态。

## 复用与规范检查

- 复用 Ant Design `Button` 与现有 `@ant-design/icons` 图标体系。
- 沿用当前 `ConnectionSummary` 组件和 CSS Modules 样式，不新增通用抽象。
- 本次没有新增字典、权限点或后端接口。
- 本次没有新增重复业务逻辑。
