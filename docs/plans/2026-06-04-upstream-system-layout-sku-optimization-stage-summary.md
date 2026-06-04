# 上游系统管理布局与 SKU 体验优化阶段总结

日期：2026-06-04

## 新增问题

- 浏览器首次直接打开 `/overseas-warehouse-service/upstream-system` 时出现 404。原因是当前 React 依赖登录后的远端菜单动态 patch 路由，未登录时直接访问动态菜单路径不会注册该路由。登录后从菜单进入页面正常。
- 首次接口验证发现 SKU 列表分页报错：`selectConnectionByCode` 预查被 PageHelper 加上分页，形成 `limit 1 LIMIT 10`。

## 已修复问题

- 上游系统页面从顶部主仓表改为左侧主仓列表、右侧主仓摘要和配置 Tabs。
- 新增主仓排序接口和左侧排序模式，保存 `display_order`。
- 新增 SKU-only 同步接口和前端“同步SKU”按钮。
- SKU Tab 补齐同步状态、上次成功、下次同步、字段筛选、配对状态筛选、同步状态筛选、客户名称列。
- SKU 配对弹窗新增客户名称字段，继续复用原 `PairingModal`。
- SKU 列表移除分页内的主仓预查，避免 PageHelper 上下文污染。

## 残留问题

- 本轮未新增 SKU 精确统计字段，例如新增、更新、缺失标记数量；后续如需要应先走表结构确认。
- 本轮未接若依定时任务做 10 分钟自动 SKU 同步。
- 本轮未实现下游 `resolveMasterSku` 硬拦截服务。
- `UpstreamSystemServiceImpl.java` 仍然较大，后续如果扩展订单、库存、费用或更多外部系统，应拆出同步服务、配对服务和日志查询服务。

## 验证命令

| 类型 | 命令/动作 | 结果 |
| --- | --- | --- |
| 后端模块编译 | `mvn -pl integration -am -DskipTests compile` | 通过 |
| 后端全量打包 | `mvn -DskipTests install` | 通过 |
| 前端格式化/检查 | `npx biome check --write src/pages/UpstreamSystem src/services/integration src/types/integration` | 通过，自动格式化上游系统相关文件 |
| 前端类型检查 | `npm run tsc` | 通过 |
| 前端构建 | `npm run build` | 通过 |
| 管理员接口 | `admin/admin123` 调用列表、排序、SKU 状态、SKU 列表、SKU-only 同步 | 通过 |
| 真实 SKU 同步 | `POST /integration/admin/upstream-systems/LX-CA012/skus/sync` | 通过，返回 `skuCount=5401` |
| 无权限接口 | `ry/admin123` 调用列表、排序、SKU-only 同步 | 均返回 403 |
| 浏览器验证 | 登录后从菜单进入“海外仓服务设置 / 上游系统管理” | 通过 |

## 未验证原因

- 未验证真实新增 SKU 配对写入，避免在真实 `LX-CA012` 数据上制造测试配对。
- 未测试并发触发 SKU 同步的真实竞争请求，只通过代码路径和单次同步验证互斥保护可用；真实并发压测留到后续专项。

## 权限检查结果

- 排序接口复用 `integration:upstream:edit`。
- SKU-only 同步接口复用 `integration:upstream:sync`。
- 前端排序、新增、编辑、授权、同步、启停、配对按钮继续用 `access.hasPerms(...)` 控制。
- `ry/admin123` 对上游系统列表、排序、SKU-only 同步均返回 403。

## 字典/选项复用检查结果

- 连接状态、授权状态、同步清单状态、SKU 同步状态、配对状态、SKU 搜索字段均集中在 `react-ui/src/pages/UpstreamSystem/constants.ts`。
- 页面文案继续使用“同步清单”，未展示“候选”。
- 本轮未新增字典表数据。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md` 的上游系统 React 页面组件条目。
- 已登记分页接口不要在 `startPage()` 后做 `selectOne` 预查的规则。

## 大文件合理性判断结果

- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java` 当前约 666 行。保留原因：这是当前上游系统聚合的若依 Service 实现，排序、同步状态、配对约束、审计和外部适配器调用仍在同一业务边界内；本轮不引入跨服务重构，避免扩大影响面。
- `react-ui/src/pages/UpstreamSystem/index.tsx` 当前约 348 行。保留原因：主入口主要负责页面布局、弹窗状态和动作编排，表格细节已拆到 `ConnectionSidebar`、`ConnectionSummary`、`SyncTabs`、`SkuSyncPanel`。
- `react-ui/src/pages/UpstreamSystem/components/SyncTabs.tsx` 当前约 334 行。保留原因：SKU 已拆出，剩余是仓库、物流渠道、请求日志三个 Tab 的表格编排；后续继续增加导入、自动配对或详情时再拆。

## 重复代码检查结果

- SKU 同步状态文本、筛选项、配对状态没有散落到页面内部，统一放在 `constants.ts`。
- SKU-only 同步复用 `LingxingOpenApiClient`、`syncSkus` 和 `upsertSkuSyncState`，没有新增第二套领星签名或请求日志逻辑。
- 弹窗继续复用 `PairingModal`，仅通过 `showCustomerName` 控制 SKU 专属字段。
