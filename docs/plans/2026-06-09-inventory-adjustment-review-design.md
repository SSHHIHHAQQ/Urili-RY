# 库存调整审核设计方案

## 1. 背景与目标

当前库存总览已经支持管理端直接调整 `inventory_sku_warehouse_stock` 的平台总库存和平台在途库存，并通过 `inventory_stock_ledger` 记录流水。

本次要补齐的是“高风险库存退回保护期”能力：当卖家或管理端申请从平台库存池退回一部分库存时，系统按配置判断是否需要进入审核/等待期。进入后，不立即移除平台库存，而是生成库存调整审核单；默认等待一段时间，平台库存继续承接销售消耗，到期后或人工立即生效时，再按规则执行最终可退数量，并写库存流水。

本方案只做业务和表结构设计，不直接执行 DDL/DML，不改代码。

## 2. 已确认业务规则

1. 库存调整审核的核心目的是防止卖家突然大额退回平台库存，导致平台无法满足近期销售需求。
2. 近期销量保护值可以由多组统计窗口参与计算。例如默认规则：
   - 最近 7 天日均销量。
   - 最近 30 天日均销量。
   - 取两者较大值作为保护日销。
   - 最低保护库存 = 保护日销 × 保障天数。
3. 默认示例：
   - 近 30 天日均 30。
   - 近 7 天日均 10。
   - 取 30。
   - 保障天数为 7 时，最低保护库存为 210。
4. 平台按“申请退回数量”判断，不以“申请目标库存”作为业务主语。
5. 可立即退回数量 = 当前平台总库存 - max(最低保护库存, 当前平台锁定库存)。如果申请退回数量大于可立即退回数量，进入库存调整审核/保护期。
6. 保护期内平台库存继续承接销售消耗。
7. 到期或人工立即生效时，实际退回数量按当时剩余库存重新计算：如果卖到只剩 300，而申请退回 850，则最多只能退回 300。
8. 退回库存在当前系统内表现为从平台总库存中移除；本系统不维护卖家外部库存池，WMS 实际出库或库存移动由外部系统处理。
9. 保护期默认可配置为 7 天，也可以调整为 3 天、1 天或其他时长。
10. 管理端可以人工立即生效，相当于抹除剩余等待期。

## 3. 需要支持的配置能力

### 3.1 审核模式

建议支持以下模式：

| 模式 | 含义 | 场景 |
| --- | --- | --- |
| `DISABLED` | 完全放开，不进入审核 | 平台临时放宽、低风险卖家 |
| `CONDITIONAL` | 按条件判断是否进入审核 | 默认策略 |
| `ALWAYS` | 任何匹配调整都进入审核 | 高风险卖家、风控观察卖家 |

### 3.2 调整方向

规则需要支持按调整方向匹配：

| 方向 | 含义 |
| --- | --- |
| `DECREASE` | 只审核降低平台库存 |
| `INCREASE` | 只审核提高平台库存 |
| `BOTH` | 提高和降低都审核 |

默认建议：全局规则只覆盖 `DECREASE`。卖家例外规则可以配置为 `BOTH` 或 `INCREASE`，满足“某个卖家无论什么情况，只要库存往上调也必须审核”。

### 3.3 调整字段范围

第一期建议只把 `PLATFORM_TOTAL` 纳入库存调整审核。

| 字段 | 建议 |
| --- | --- |
| `PLATFORM_TOTAL` | 纳入审核规则 |
| `PLATFORM_IN_TRANSIT` | 默认不纳入，后续如需要再开放 |

原因：本次讨论的是平台总库存退回/移除问题；平台在途库存已有独立业务含义，不应和总库存保护期混在一起。

### 3.4 销量保护和可退回数量

销量保护拆成三个概念：

1. 统计窗口：用哪些历史天数计算日均销量，例如 7 天、30 天。
2. 保障天数：希望平台至少保留多少天销售库存，例如 7 天、30 天。
3. 可退回数量：当前库存扣除最低保护库存和锁定库存后，最多可以立即退回多少。

计算方式：

```text
保护日销 = max(各统计窗口日均销量)
最低保护库存 = ceil(保护日销 × 保障天数)
最低应保留库存 = max(最低保护库存, 当前平台锁定库存)
可立即退回数量 = max(0, 当前平台总库存 - 最低应保留库存)
预计保留库存 = max(0, 当前平台总库存 - 申请退回数量)
```

配置示例：

| 配置 | 结果 |
| --- | --- |
| 统计窗口 `[7,30]`，保障天数 `7` | 当前默认保护逻辑 |
| 统计窗口 `[30]`，保障天数 `30` | 按一个月销量保护，例如日销 30，最低保护库存 900 |
| 统计窗口 `[7]`，保障天数 `3` | 只按近 7 天趋势，保护 3 天 |

### 3.5 低影响调整豁免

建议支持可选的小额豁免，避免极小调整也进入审核：

| 配置项 | 含义 |
| --- | --- |
| `min_return_qty_to_review` | 申请退回数量达到多少才可能审核 |
| `min_return_ratio_to_review` | 申请退回数量占当前平台总库存比例达到多少才可能审核 |

默认可以都为空，表示只看可立即退回数量和申请退回数量。

例外：如果卖家策略是 `ALWAYS`，小额豁免不生效。

### 3.6 冷却/保护期

建议配置为小时级，页面展示为天：

| 配置项 | 示例 | 含义 |
| --- | --- | --- |
| `cooldown_hours` | `168` | 默认 7 天 |
| `auto_effect_enabled` | `Y` | 到期后是否允许系统自动生效 |
| `manual_effect_allowed` | `Y` | 是否允许管理端人工立即生效 |

默认建议：

- 全局规则：`cooldown_hours=168`，`auto_effect_enabled=Y`，`manual_effect_allowed=Y`。
- 强制审核卖家：可配置 `auto_effect_enabled=N`，必须人工处理。

### 3.7 策略匹配范围

建议使用“策略组 + 绑定”的方式，不把所有条件写死在一个全局配置里。

第一期支持：

| 范围 | 说明 |
| --- | --- |
| 全局默认 | 所有库存调整都兜底匹配 |
| 卖家绑定 | 某个卖家使用指定策略 |

后续可扩展：

| 范围 | 说明 |
| --- | --- |
| 卖家等级/风控等级 | 高风险卖家自动强审 |
| 商品类目 | 高风险类目使用更长保障天数 |
| SKU | 爆品或活动 SKU 单独保护 |
| 仓库类型 | 官方仓和三方仓不同策略 |

优先级建议：

```text
SKU 规则 > 商品类目规则 > 卖家规则 > 卖家等级规则 > 全局默认规则
```

第一期只落地：

```text
卖家规则 > 全局默认规则
```

## 4. 触发判断流程

库存退回申请提交时，后端应按以下顺序判断：

1. 校验现有硬约束：
   - 库存行存在。
   - 申请退回数量必须大于 0。
   - 申请退回数量不能大于当前平台总库存。
   - 退回后不能低于平台锁定库存；如果低于，直接拒绝或进入人工异常处理，不允许静默突破锁定库存。
   - 官方仓不得超过来源可用扣减后的上限。
   - 未配置仓库不能调整。
2. 匹配库存调整审核策略。
3. 如果策略为 `DISABLED`，直接按申请退回数量移除平台库存，并写库存流水。
4. 如果策略为 `ALWAYS`，生成库存调整审核单，不立即移除平台库存。
5. 如果策略为 `CONDITIONAL`：
   - 计算最低保护库存。
   - 计算可立即退回数量。
   - 判断申请退回数量是否超过可立即退回数量。
   - 结合小额豁免条件。
   - 命中则生成审核单；未命中则直接生效。

## 5. 审核单状态流转

建议状态：

| 状态 | 含义 |
| --- | --- |
| `WAITING` | 等待保护期结束 |
| `READY` | 已到计划生效时间，等待系统任务或人工生效 |
| `EFFECTIVE` | 已生效 |
| `REJECTED` | 管理端驳回 |
| `CANCELLED` | 申请方撤回或系统取消 |
| `FAILED` | 生效时遇到库存并发变化或约束失败 |

操作：

| 操作 | 说明 |
| --- | --- |
| 立即生效 | 管理端人工跳过剩余保护期 |
| 调整生效时间 | 管理端缩短或延长保护期 |
| 驳回 | 不执行本次库存移除 |
| 查看日志 | 查看策略命中、时间调整、生效、驳回等记录 |

## 6. 到期生效公式

库存审核单以“申请退回数量”为主口径。申请时可以展示预计保留库存，但最终生效必须重新读取当时库存，按当时最多可退数量执行。

申请时：

```text
申请退回数量 = requested_adjust_qty（adjust_direction = DECREASE）
申请时预计保留库存 = max(0, 申请时平台总库存 - 申请退回数量)
```

生效时：

```text
理论可退数量 = max(0, 生效前平台总库存 - 当前平台锁定库存)
实际退回数量 = min(申请退回数量, 理论可退数量)
生效后平台总库存 = 生效前平台总库存 - 实际退回数量
未退回数量 = 申请退回数量 - 实际退回数量
```

例子：

| 申请时平台库存 | 申请退回 | 申请时预计保留 | 生效前平台库存 | 实际退回 | 生效后平台库存 |
| --- | --- | --- | --- | --- | --- |
| 1000 | 850 | 150 | 900 | 850 | 50 |
| 1000 | 850 | 150 | 300 | 300 | 0 |
| 1000 | 850 | 150 | 100 | 100 | 0 |

### 必须保留的系统硬下限

无论是到期自动生效还是人工立即生效，都不能把平台总库存降到当前平台锁定库存以下：

```text
最终平台总库存 >= 当前平台锁定库存
```

如果锁定库存导致无法退回全部申请数量，则只退回可退部分，并在审核单中记录 `unfulfilled_qty`；如果业务要求“不能部分退回”，则进入 `FAILED` 并提示运营人工处理。推荐第一期允许按可退数量部分生效，因为这更符合“卖到只剩多少就退多少”的业务口径。

## 7. 建议新增表

### 7.1 库存调整审核策略组

建议表名：`inventory_adjustment_review_policy`

字段草案：

| 字段 | 含义 |
| --- | --- |
| `policy_id` | 主键 |
| `policy_name` | 策略名称 |
| `policy_status` | 启用/停用 |
| `review_mode` | `DISABLED` / `CONDITIONAL` / `ALWAYS` |
| `direction_scope` | `DECREASE` / `INCREASE` / `BOTH` |
| `field_scope` | `PLATFORM_TOTAL` / `ALL` |
| `sales_window_days` | JSON，例如 `[7,30]` |
| `sales_aggregate_mode` | 默认 `MAX_DAILY_AVG` |
| `reserve_days` | 保障天数 |
| `cooldown_hours` | 保护期小时数 |
| `min_decrease_qty_to_review` | 降低数量门槛 |
| `min_decrease_ratio_to_review` | 降低比例门槛 |
| `auto_effect_enabled` | 到期是否自动生效 |
| `manual_effect_allowed` | 是否允许人工立即生效 |
| `remark` | 备注 |
| `create_by/create_time/update_by/update_time` | 审计字段 |

### 7.2 策略绑定

建议表名：`inventory_adjustment_review_policy_binding`

字段草案：

| 字段 | 含义 |
| --- | --- |
| `binding_id` | 主键 |
| `policy_id` | 策略 ID |
| `binding_type` | `GLOBAL` / `SELLER`，后续扩展 `CATEGORY` / `SKU` |
| `binding_id_value` | 绑定对象 ID；全局可为 0 |
| `priority` | 优先级 |
| `status` | 启用/停用 |
| `remark` | 备注 |
| `create_by/create_time/update_by/update_time` | 审计字段 |

### 7.3 库存调整审核单

建议表名：`inventory_adjustment_review_request`

字段草案：

| 字段 | 含义 |
| --- | --- |
| `review_id` | 主键 |
| `review_no` | 审核单号 |
| `review_status` | `WAITING` / `READY` / `EFFECTIVE` / `REJECTED` / `CANCELLED` / `FAILED` |
| `policy_id` | 命中的策略 ID |
| `policy_snapshot_json` | 命中策略快照 |
| `stock_id/stock_key` | 库存行 |
| `spu_id/sku_id/seller_id` | 商品与卖家快照 |
| `warehouse_kind/warehouse_ref_type/warehouse_name` | 仓库快照 |
| `adjust_field` | 调整字段 |
| `adjust_direction` | 调整方向 |
| `request_before_platform_total_qty` | 申请时平台总库存 |
| `requested_adjust_qty` | 申请调整数量；`DECREASE` 时就是申请退回数量，`INCREASE` 时就是申请增加数量 |
| `request_expected_after_platform_total_qty` | 申请时按调整方向和数量推导的预计生效后平台库存 |
| `platform_reserved_qty_snapshot` | 申请时锁定库存 |
| `sales_7d_qty/sales_7d_daily_avg` | 近 7 天销量快照 |
| `sales_30d_qty/sales_30d_daily_avg` | 近 30 天销量快照 |
| `threshold_daily_avg` | 采用的保护日销 |
| `threshold_reserve_days` | 采用的保障天数 |
| `protected_retained_qty` | 最低保护库存 |
| `min_retained_qty` | 申请时最低应保留库存，取最低保护库存和锁定库存较大值 |
| `immediate_returnable_qty` | 申请时可立即退回数量 |
| `trigger_reason` | 命中审核原因 |
| `submit_terminal` | `ADMIN` / `SELLER` / `SYSTEM` |
| `submit_user_id/submit_user_name` | 提交人 |
| `submit_reason` | 申请原因 |
| `submit_time` | 提交时间 |
| `planned_effective_time` | 计划生效时间 |
| `effective_time` | 实际生效时间 |
| `effective_operator_id/effective_operator_name` | 生效操作人 |
| `effective_before_platform_total_qty` | 生效前平台总库存 |
| `actual_effect_qty` | 实际生效数量；降低时就是实际退回数量 |
| `unfulfilled_qty` | 未满足数量；降低时就是未退回数量 |
| `effective_after_platform_total_qty` | 生效后平台总库存 |
| `review_reason` | 人工处理原因 |
| `version` | 乐观锁版本 |
| `create_by/create_time/update_by/update_time` | 审计字段 |

### 7.4 审核操作日志

建议表名：`inventory_adjustment_review_operation_log`

字段草案：

| 字段 | 含义 |
| --- | --- |
| `log_id` | 主键 |
| `review_id/review_no` | 审核单 |
| `operation_type` | `SUBMIT` / `EFFECT_NOW` / `AUTO_EFFECT` / `CHANGE_EFFECTIVE_TIME` / `REJECT` / `CANCEL` / `FAIL` |
| `before_status/after_status` | 状态变化 |
| `operation_reason` | 操作原因 |
| `operator_id/operator_name` | 操作人 |
| `operate_time` | 操作时间 |
| `change_summary` | 变化摘要 |

### 7.5 SKU 日销量读模型

建议表名：`inventory_sku_sales_daily`

字段草案：

| 字段 | 含义 |
| --- | --- |
| `stat_date` | 统计日期 |
| `sku_id` | SKU ID |
| `seller_id` | 卖家 ID |
| `sold_qty` | 当日有效销量 |
| `order_count` | 当日订单数，可选 |
| `stat_source` | 来源，例如 `ORDER` / `RESERVATION` |
| `refresh_time` | 刷新时间 |

注意：当前订单模块尚未完整落地，第一期如果没有真实订单销量来源，销量读模型可以先只设计不启用，或者由后续订单锁定/出库流水生成。

## 8. 后端接口草案

### 8.1 审核列表

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/inventory/admin/adjustment-reviews/list` | `review:inventoryAdjustment:list` | 审核单列表 |
| GET | `/inventory/admin/adjustment-reviews/{reviewId}` | `review:inventoryAdjustment:query` | 审核单详情 |
| GET | `/inventory/admin/adjustment-reviews/{reviewId}/logs` | `review:inventoryAdjustment:log` | 操作日志 |
| POST | `/inventory/admin/adjustment-reviews/{reviewId}/effect-now` | `review:inventoryAdjustment:effect` | 人工立即生效 |
| POST | `/inventory/admin/adjustment-reviews/{reviewId}/effective-time` | `review:inventoryAdjustment:edit` | 调整计划生效时间 |
| POST | `/inventory/admin/adjustment-reviews/{reviewId}/reject` | `review:inventoryAdjustment:reject` | 驳回 |

### 8.2 策略配置

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/inventory/admin/adjustment-review-policies/list` | `review:inventoryAdjustment:config` | 策略组列表 |
| POST | `/inventory/admin/adjustment-review-policies` | `review:inventoryAdjustment:config` | 新增策略 |
| PUT | `/inventory/admin/adjustment-review-policies/{policyId}` | `review:inventoryAdjustment:config` | 修改策略 |
| POST | `/inventory/admin/adjustment-review-policies/{policyId}/bindings` | `review:inventoryAdjustment:config` | 绑定卖家 |

## 9. 前端页面设计

菜单 `库存调整审核` 从占位页改为实际页面，建议路径：

```text
react-ui/src/pages/Inventory/AdjustmentReview/index.tsx
```

页面结构：

1. 顶部 Tabs：
   - 全部
   - 等待中
   - 待生效
   - 已生效
   - 已驳回
   - 异常
2. 筛选区：
   - 审核单号
   - 卖家
   - SKU / 商品关键字
   - 仓库
   - 状态
   - 计划生效时间
   - 提交时间
3. 表格列：
   - 审核单号
   - 卖家
   - SKU / 商品
   - 仓库
   - 申请退回数量
   - 申请时平台库存 / 预计保留库存
   - 申请时可立即退回数量
   - 当前平台库存
   - 最低保护库存
   - 实际退回 / 未退回数量
   - 近 7 天 / 近 30 天日销
   - 剩余等待时间
   - 状态
   - 操作
4. 操作：
   - 详情
   - 立即生效
   - 更多：调整生效时间、驳回、日志

策略配置可以第一期放在同页右上角“规则配置”抽屉，后续再拆独立菜单。

## 10. 与现有库存调整链路的关系

现有链路：

```text
库存调整 preview -> confirm -> 按目标库存更新 inventory_sku_warehouse_stock -> 写 inventory_stock_ledger
```

调整后：

```text
库存调整 preview -> confirm
  -> 把降低平台总库存转换为申请退回数量
  -> 不需要审核：按申请退回数量直接移除平台库存
  -> 需要审核：生成 inventory_adjustment_review_request，不更新库存
```

审核单生效时：

```text
读取当前库存行
重新校验硬约束
计算实际可退数量
乐观锁更新 inventory_sku_warehouse_stock
写 inventory_stock_ledger
写审核操作日志
刷新 SKU/SPU 库存读模型
```

实现影响：

1. 现有库存调整弹窗如果仍输入“调整后平台总库存”，后端在命中降低平台总库存时必须先转换：

```text
申请退回数量 = 当前平台总库存 - 调整后平台总库存
```

2. 库存调整审核单、审核列表和日志都以“申请退回数量”为主展示字段，不把“调整后平台总库存”当主字段。
3. 后续卖家端发起库存退回申请时，入口应直接输入“申请退回数量”，不再让卖家反推要把平台总库存调到多少。
4. 库存流水写入仍落到 `inventory_stock_ledger`，降低库存时 `delta_qty = -实际退回数量`，`after_platform_total_qty = before_platform_total_qty - 实际退回数量`。
5. 如果策略命中的是提高库存审核，仍使用同一张审核表，通过 `adjust_direction=INCREASE` 和 `requested_adjust_qty` 表达；该分支不参与“可退回数量”计算。

## 11. 待确认问题

1. 日销量口径：
   - 按订单创建、付款、锁定库存、还是出库完成统计？
   - 取消订单、退款订单是否扣回销量？
2. 提交主体：
   - 第一期是否只做管理端发起库存调整进入审核？
   - 还是要同步支持卖家端发起库存移除申请？
3. 到期生效：
   - 默认是否由定时任务自动生效？
   - 高风险卖家是否必须人工处理？
4. 提高库存强制审核：
   - 命中 `ALWAYS + INCREASE` 后，是等待冷却期自动生效，还是必须人工点击生效？
5. 部分生效策略：
   - 如果锁定库存导致不能退回全部申请数量，第一期推荐按可退数量部分生效，并记录未退回数量。

## 12. 推荐一期范围

建议第一期按以下范围实现：

1. 新增全局策略和卖家策略。
2. 支持 `DISABLED` / `CONDITIONAL` / `ALWAYS`。
3. 支持平台总库存 `PLATFORM_TOTAL`。
4. 默认条件：申请退回数量超过 `当前平台总库存 - max(max(7 天日均, 30 天日均) × 7, 当前锁定库存)`。
5. 支持配置保障天数和冷却期。
6. 支持管理端立即生效、调整生效时间、驳回、日志。
7. 接入现有库存调整 confirm 链路：将降低平台总库存转换为申请退回数量，不需要审核则直接移除，需要审核则生成审核单。
8. 暂不把平台在途库存纳入审核。
9. 暂不做 SKU/类目策略，只保留表结构扩展空间。
