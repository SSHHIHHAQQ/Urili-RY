# 官方仓库报价/履约绑定实现计划

## 背景

当前上游系统管理已经支持：

- `upstream_system_connection`：保存领星 WMS 接入账号，含结算类型。
- `upstream_system_warehouse_pairing`：保存系统仓库与领星仓库配对。
- `upstream_system_logistics_channel_pairing`：保存系统物流渠道与领星渠道配对。
- 官方仓库列表通过 `upstream_system_warehouse_pairing` 展示当前配对关系。

新的业务口径：

- 正常 WMS 模式：系统仓库绑定上游履约仓，上游履约仓负责库存、尺寸重量、出库、成本。
- 自营仓是前期为了复用现成报价能力的补充：系统仓库可额外绑定一个自营报价仓，用来自营仓运费试算，给客户展示销售价格。
- 不做复杂多角色扩展。固定只支持两个绑定位：
  - 上游履约仓
  - 自营报价仓

## 目标边界

本轮目标是完成官方仓库与上游系统仓库、渠道的固定双口径绑定：

```text
官方仓库：美东NY013
  上游履约仓：NY013
  自营报价仓：KAT，可为空

官方仓库 + 系统渠道：
  履约渠道：NY013 下实际出库渠道
  报价渠道：KAT 下报价试算渠道，可为空
```

本轮不做：

- 不做正式报价方案模块。
- 不做订单出库完整流程。
- 不做成本核算流水。
- 不新增泛化的仓库角色体系。
- 不把外部仓库字段冗余到 `warehouse` 或 `official_warehouse` 主表。

## 设计原则

1. 复用当前事实源：官方仓库外部仓绑定仍以 `upstream_system_warehouse_pairing` 为事实源。
2. 简单固定：只增加 `履约` 和 `报价` 两个绑定口径。
3. 结算类型约束：
   - 履约仓只能选择 `upstream-payable`，即上游仓（应付）。
   - 报价仓只能选择 `self-operated-receivable`，即自营仓（应收）。
4. 报价绑定可为空：没有自营报价仓时，后续走系统正式报价方案或提示未配置。
5. 履约绑定必须唯一：一个官方仓库只能有一个履约仓。
6. 报价绑定必须唯一：一个官方仓库最多一个自营报价仓。

## 数据表方案

### 1. 改造 `upstream_system_warehouse_pairing`

在现有表上增加固定绑定类型，不新建泛化仓库角色表。

新增字段：

| 字段 | 类型 | 必填 | 默认值 | 注释 |
|---|---|---:|---|---|
| `pairing_role` | `varchar(32)` | 是 | `FULFILLMENT` | 配对用途：`FULFILLMENT` 履约仓，`QUOTE` 报价仓 |

约束调整：

| 约束 | 字段 | 原因 |
|---|---|---|
| `uk_upstream_wh_pairing_system_role` | `system_warehouse_code, pairing_role` | 同一个系统仓库，每种用途只能绑定一次 |
| `uk_upstream_wh_pairing_upstream_role` | `connection_code, upstream_warehouse_code, pairing_role` | 同一个外部仓库，同一用途只能配给一次，保留当前 1:1 规则 |

迁移规则：

- 现有仓库配对统一迁移为 `FULFILLMENT`。
- 官方仓库列表原来的“主仓配对”展示改名为“履约仓”。

### 2. 改造 `upstream_system_logistics_channel_pairing`

渠道配对也固定拆成履约渠道和报价渠道。

新增字段：

| 字段 | 类型 | 必填 | 默认值 | 注释 |
|---|---|---:|---|---|
| `system_warehouse_code` | `varchar(64)` | 是 | 空字符串迁移后补齐 | 系统仓库代码，渠道绑定必须带仓库上下文 |
| `upstream_warehouse_code` | `varchar(100)` | 是 | 空字符串迁移后补齐 | 上游仓库代码，区分同一渠道在不同仓库下的含义 |
| `pairing_role` | `varchar(32)` | 是 | `FULFILLMENT` | 配对用途：`FULFILLMENT` 履约渠道，`QUOTE` 报价渠道 |

约束调整：

| 约束 | 字段 | 原因 |
|---|---|---|
| `uk_upstream_channel_pairing_system_role` | `system_warehouse_code, system_channel_code, pairing_role` | 同一系统仓库下，同一系统渠道，每种用途只能绑定一次 |
| `idx_upstream_channel_pairing_upstream_role` | `connection_code, upstream_warehouse_code, upstream_channel_code, pairing_role` | 查询外部渠道及防错校验 |

迁移规则：

- 现有渠道配对先迁移为 `FULFILLMENT`。
- 如果旧数据无法唯一反查 `system_warehouse_code` 或 `upstream_warehouse_code`，保留为待修复状态，不自动猜。

## 后端实现计划

### 1. Integration 模块

调整仓库配对：

- `UpstreamWarehousePairing` 增加 `pairingRole`。
- `WarehousePairingRequest` 增加 `pairingRole`。
- 新增校验：
  - `FULFILLMENT` 必须绑定结算类型为 `upstream-payable` 的接入。
  - `QUOTE` 必须绑定结算类型为 `self-operated-receivable` 的接入。
  - 同一系统仓库同一 `pairingRole` 不可重复。
- 仓库同步清单 Tab 支持按角色配对/解除。

调整渠道配对：

- `UpstreamLogisticsChannelPairing` 增加 `systemWarehouseCode`、`upstreamWarehouseCode`、`pairingRole`。
- `LogisticsChannelPairingRequest` 同步增加字段。
- 新增校验：
  - 渠道绑定必须先存在对应角色的仓库绑定。
  - 报价渠道必须挂在自营报价仓下面。
  - 履约渠道必须挂在上游履约仓下面。

### 2. Warehouse 模块

官方仓库列表调整：

- 列表展示两个绑定摘要：
  - 履约仓
  - 报价仓
- 官方仓库同步弹窗调整：
  - 先保留原“同步官方仓库”能力，但同步后创建的是 `FULFILLMENT` 绑定。
  - 额外提供“设置报价仓”入口，不强制同步官方仓库时同时配置。

新增 options 接口：

- 官方仓可选履约仓：只返回 `settlement_type = upstream-payable` 的接入及其仓库候选。
- 官方仓可选报价仓：只返回 `settlement_type = self-operated-receivable` 的接入及其仓库候选。

## 前端实现计划

### 1. 上游系统管理页

仓库 Tab：

- 表格增加“用途”或拆分操作：
  - 配为履约仓
  - 配为报价仓
- 当前连接如果是上游仓（应付），只显示履约配对操作。
- 当前连接如果是自营仓（应收），只显示报价配对操作。

物流渠道 Tab：

- 渠道配对弹窗增加：
  - 系统仓库
  - 用途：履约 / 报价
  - 绑定的外部仓库
  - 外部渠道
- 根据用途过滤可选连接、仓库和渠道。

### 2. 官方仓库页

列表列调整：

- 原“主仓配对”改为“履约仓”。
- 增加“报价仓”列。
- 未配置报价仓显示“未配置”，不作为错误。
- 未配置履约仓显示“未配置”，后续出库前必须阻断。

操作调整：

- 保留“同步仓库”。
- 增加“设置绑定”或“设置报价仓”。
- 页面不做复杂流程图，只做清晰表格和弹窗。

## 权限与字典

复用现有权限优先：

- 仓库绑定操作可先复用 `warehouse:official:sync` 或 `integration:upstream:pair`。
- 如果后续按钮需要更细权限，再新增：
  - `warehouse:official:binding`
  - `integration:upstream:pair`

新增字典建议：

| 字典类型 | 用途 |
|---|---|
| `upstream_pairing_role` | `FULFILLMENT` 履约，`QUOTE` 报价 |

## 迁移与兼容

1. 增量 SQL 使用日期前缀，例如 `20260607_official_warehouse_quote_fulfillment_binding.sql`。
2. SQL 必须带确认 token 和 fail-closed guard。
3. 旧仓库配对全部迁移为 `FULFILLMENT`。
4. 旧官方仓库列表继续可显示履约仓，不因为报价仓为空报错。
5. 旧渠道配对迁移为 `FULFILLMENT`，无法确定仓库上下文的记录标记为待修复，不自动猜。

## 验证计划

后端：

- `mvn -pl integration,warehouse -am -DskipTests compile`
- 覆盖校验：
  - 官方仓同一角色不能重复绑定。
  - 履约仓不能选择自营仓接入。
  - 报价仓不能选择上游仓接入。
  - 报价绑定可以为空。

前端：

- `npm run tsc`
- 页面验证：
  - 官方仓列表展示履约仓和报价仓。
  - 上游仓连接只能做履约绑定。
  - 自营仓连接只能做报价绑定。
  - 渠道绑定按仓库和用途过滤。

工程检查：

- `codegraph sync .`
- 更新 `docs/architecture/reuse-ledger.md`
- 生成实施记录 Markdown。

## 风险点

1. 旧渠道配对缺少仓库上下文，迁移时可能无法自动确定归属，需要页面提示重新绑定。
2. 当前来源仓库库存读模型依赖仓库配对状态，新增 `pairing_role` 后，库存侧只应该认 `FULFILLMENT`，不能把报价仓算成库存来源。
3. 官方仓同步现有逻辑会自动创建配对，改造时必须明确它创建的是履约仓配对。

## 建议实施顺序

1. 先落 SQL 和后端模型，完成仓库双绑定。
2. 再改官方仓库列表和上游系统仓库 Tab。
3. 再改物流渠道双绑定。
4. 最后回归来源仓库库存、SKU库存同步清单，确认只读取履约绑定。
