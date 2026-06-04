# 上游系统管理旧项目逻辑差异核对

日期：2026-06-04

## 核对范围

旧项目重点文件：

- `E:\Urili\apps\admin-web\src\features\upstream-systems\UpstreamSystemManagementPage.tsx`
- `E:\Urili\apps\admin-web\src\features\upstream-systems\components\SkuPairingPanel.tsx`
- `E:\Urili\apps\api\src\admin-upstream-systems.routes.ts`
- `E:\Urili\apps\api\src\admin-upstream-systems.service.ts`
- `E:\Urili\packages\modules\upstream-systems\src`
- `E:\Urili\apps\worker\src\jobs\upstream-sku-sync.*`

若依当前重点文件：

- `RuoYi-Vue/integration`
- `RuoYi-Vue/sql/upstream_system_management_seed.sql`
- `react-ui/src/pages/UpstreamSystem`
- `react-ui/src/services/integration/upstreamSystem.ts`
- `react-ui/src/types/integration/upstream-system.d.ts`

## 总体判断

若依首版已经覆盖旧项目的主干闭环：主仓接入、加密凭证、授权校验、真实同步、SKU 同步清单、SKU 一对一配对、请求日志、权限控制。

若依首版比旧项目更完整的地方是：仓库配对和物流渠道配对已经有独立 MySQL 表、唯一约束和真实增删接口；旧项目页面里仓库/物流渠道更多是 payload 展示和 `onAction` 占位。

旧项目比若依首版更完整的地方集中在：左侧主仓工作台、主仓排序、SKU 面板体验、SKU 自动同步/同步摘要、下游 `resolveMasterSku` 硬拦截能力。

## 差异清单

| 模块 | 旧项目逻辑 | 若依当前逻辑 | 差异判断 | 建议 |
| --- | --- | --- | --- | --- |
| 页面布局 | 左侧主仓列表选择，右侧基本信息和 Tabs 随选中主仓变化 | 上方主仓 ProTable，下方 Tabs 随选中主仓变化 | 数据流已有，布局未对齐 | 建议改，已单独写布局分析 |
| 主仓分组 | 按 `systemName` 分组，例如“领星WMS” | 当前列表未分组，系统类型字段存在 | 体验差异 | 建议在左侧列表中按上游系统类型分组 |
| 主仓排序 | 有 `displayOrder`、reorder mode、`PATCH /api/admin/upstream-systems/order` | 表有 `display_order`，列表按它排序，但没有排序接口和 UI | 旧项目更完整 | 建议补，尤其左侧列表做出来后需要稳定顺序 |
| 新增主仓 | 创建时校验领星授权并保存加密凭证 | 创建时校验授权并保存加密凭证 | 基本一致 | 保持 |
| 编辑信息 | 信息编辑和重新授权分开 | 已分为编辑信息、重新授权 | 基本一致 | 保持 |
| 删除主仓 | 旧 UI 有“删除”入口，但后端未看到完整删除接口 | 当前是启停，不物理删除 | 若依更稳妥 | 不建议物理删除；如需要做“删除”，建议实现为停用/归档 |
| 状态模型 | `enabled`、`paused`、`warning`，凭证状态有 `configured`、`expiring`、`missing` | `ENABLED`、`DISABLED`，凭证状态主要是 `CONFIGURED` | 旧项目状态更细 | 可后置；真实运营需要预警时再补 `WARNING`/凭证过期 |
| Enabled capabilities | 旧项目连接记录存 `enabledCapabilities` | 若依有字段但前端未展示 | 轻微差异 | 暂不急，等多上游系统或能力开关出现再展示 |
| 仓库同步清单 | 仓库 payload 存在连接记录 JSON 中，页面可展示 | 独立同步清单表，支持 missing 标记 | 若依更强 | 保持若依当前设计 |
| 仓库配对 | 页面有“配对/解除”入口，但旧代码里是 `onAction` 占位 | 已有正式配对表、接口、唯一约束 | 若依更强 | 保持；后续从手输改主数据下拉 |
| 物流渠道同步清单 | 按 channelCode 聚合多个仓库 | 若依前端也按渠道聚合多个仓库 | 基本一致 | 保持 |
| 物流渠道配对 | 页面有“配对/解除”入口，但旧代码里是 `onAction` 占位 | 已有正式配对表，允许同一上游渠道配多个系统渠道，系统渠道唯一 | 若依更强 | 保持 |
| SKU 同步清单 | 服务端分页，支持状态筛选、搜索字段选择、同步摘要 | 服务端分页，支持状态和 keyword；未做字段选择和同步摘要展示 | 旧项目前端体验更完整 | 建议补 |
| SKU 配对字段 | `masterSku`、`systemSku`、`systemSkuName`、`customerName` | 后端和类型有 `customerName`，当前通用配对弹窗未展示 `customerName` | 若依后端有，前端没露出 | 建议补 SKU 专用配对弹窗或扩展 PairingModal |
| SKU 配对方式 | `PUT /sku-mappings`，按 `masterSku` upsert | 当前 `POST /sku-pairings`，重复时拒绝，解除按 pairingId | 语义差异 | 当前更符合若依新增/删除风格；如要编辑已有配对，需要补“修改配对” |
| SKU 解除方式 | 按 `masterSku` 解除 | 按 `skuPairingId` 解除 | 差异可接受 | 保持当前 ID 删除更稳定 |
| SKU 同步按钮 | SKU Tab 内有“同步SKU”，也有整体同步 | 当前只有整体同步；Controller 有 `sku-sync-state` 查询但没有 SKU-only 同步接口 | 旧项目更细 | 建议补 SKU-only 同步，避免每次都拉仓库/渠道 |
| SKU 同步摘要 | 展示同步状态、上次同步、下次同步、10分钟间隔、total/inserted/updated/missing | 若依状态表有状态和时间，但没有 total/inserted/updated/markedMissing 字段 | 旧项目更完整 | 建议补统计字段，页面展示摘要 |
| SKU 同步并发保护 | `UpstreamSkuCandidateSyncService` 内存 Set 防止同连接重复同步 | 当前没有显式并发保护 | 旧项目更稳 | 建议补，避免重复点击同步造成并发写入 |
| SKU fresh 跳过 | 非 force 时 fresh 直接返回 | 当前手动同步总是同步全量 | 旧项目更节省 | 建议补 force 参数或前端确认 |
| 10分钟后台同步 | Worker 用 BullMQ 每 10 分钟扫描启用连接并同步 SKU | 当前只有手动同步，状态表写 nextSyncTime 但没有后台任务 | 旧项目更完整 | 建议后续用若依定时任务实现，不必引入 BullMQ |
| 下游 SKU 解析 | 旧项目目录接口有 `resolveMasterSku`，未配对时抛 `UPSTREAM_SKU_PAIRING_REQUIRED` | 当前只有配对管理，还没有给后续订单/履约调用的解析接口 | 旧项目更完整 | 建议在后续订单/履约前必须补，未配对硬拦截 |
| 导入/自动配对/导出 | 旧 SKU 面板有按钮，但实现是 `onAction` 占位 | 当前没有 | 旧项目不是完整闭环 | 暂不建议做，等系统 SKU 主数据稳定后再设计 |
| SKU 详情 | 旧 SKU 面板有“详情”入口，占位 `onAction` | 当前没有 | 旧项目占位 | 暂不做 |
| 箱配对/增值服务配对 | 旧项目 Tab 已露出，但表格空数据 | 当前没有 | 旧项目占位 | 不建议现在放出来，避免误导 |
| 请求日志 | 旧项目保存 request logs，并在列表里有 `requestLogCount` | 若依有独立请求日志页面、分页和脱敏抽查 | 若依更强 | 保持；可在基本信息里补日志数量 |
| 权限模型 | 旧项目只有 read/write 两类权限，API fail closed | 若依已拆 list/query/add/edit/credential/sync/pair/log | 若依更细 | 保持 |
| OpenAPI/typed client | 旧项目有 contracts + generated client | 若依前端手写 service/type | 技术栈差异 | 当前可接受；若接口增多，可用若依代码生成或维护类型文件 |

## 建议优先级

### P1：建议这轮布局一起改

1. 左侧主仓列表 + 右侧详情布局。
2. 左侧按上游系统类型分组。
3. 右上基本信息区补全当前主仓摘要：状态、Key 脱敏、结算类型、最近授权、最近同步、请求日志数量。
4. 当前主仓操作集中到右上：编辑、重新授权、校验、同步、启停。

原因：这是用户感知最强的差异，而且不需要改数据库。

### P1：建议补 SKU 面板体验

1. SKU Tab 顶部展示同步状态：状态、上次同步、下次同步。
2. SKU 列表增加搜索字段选择：全部、masterSku、产品名、系统SKU、客户。
3. SKU 配对弹窗展示并保存 `customerName`。
4. 增加 SKU-only 同步按钮。

原因：旧项目 SKU 管理已经接近完整，若依当前能用，但运营效率不足。

### P2：建议补后台同步和排序

1. 主仓排序接口和左侧 reorder mode。
2. 用若依定时任务实现每 10 分钟 SKU 同步，而不是照搬 BullMQ。
3. SKU 同步并发保护和 fresh/force 语义。
4. SKU 同步统计字段：totalFetched、inserted、updated、markedMissing。

原因：这些影响稳定运行，但不一定阻塞当前页面改版。

### P2：后续业务前必须补

1. `resolveMasterSku(connectionCode, systemSku)` 服务方法。
2. 未配对时硬拦截后续订单/履约/提交上游动作。
3. 对接后续业务模块时只能通过 Service/Facade 调用，不允许直接读配对表。

原因：旧项目把这条当作业务底线；当前只是管理侧配对完成，还没有下游消费方。

### 暂不建议做

1. 物理删除主仓。
2. 箱配对、增值服务配对空 Tab。
3. 导入配对、自动配对、导出配对。
4. SKU 详情页。

原因：旧项目这些要么只是入口，要么依赖系统 SKU 主数据和后续业务规则。现在做容易变成空壳。

## 我建议下一步怎么改

如果你确认继续，我建议按这个顺序：

1. 先改前端布局：左侧主仓列表、右上基本信息、右下保留当前 Tabs。
2. 同步补 SKU 面板体验：同步摘要、搜索字段、客户字段、SKU-only 同步按钮。
3. 再补后端小接口：主仓排序、SKU-only 同步、SKU sync state 统计字段。
4. 最后补若依定时任务和 `resolveMasterSku`，这个可以等订单/履约模块前再做。

这样不会为了照旧项目把占位功能搬进来，同时能把旧项目已经成熟的业务逻辑补齐。
