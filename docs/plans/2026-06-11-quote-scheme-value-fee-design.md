# 报价方案增值费规则设计

日期：2026-06-11

## 本次目标

在报价方案编辑页新增一个 `增值费` 页签，用来配置“某个物流渠道在某种触发情况下，要额外加收或减收多少钱”。

第一版只做规则配置，不接订单取消后的真实费用计算、不写订单费用流水。

## 页面逻辑

`增值费` 和现有 `物流费`、`操作费` 平级。

页面字段：

- 物流渠道：跟方案类型走。
  - 计费方案：选择客户物流渠道。
  - 成本方案：选择系统物流渠道。
- 触发情况：第一版先支持 `取消订单`。
- 收费方式：
  - 按百分比调整：在原本算出来的费用基础上加收或减收，例如加收 `10%`、减收 `10%`。
  - 固定金额：固定加收或减收一个金额，例如加收 `1`、加收 `0.5`。
- 调整方向：`加收` / `减收`。
- 调整值：百分比或固定金额共用一个数值字段。
- 状态：用 Switch，默认启用。
- 排序、备注。

固定金额的币种不单独选，直接跟报价方案的币种走。

## 新增表

表名：`quote_scheme_value_fee_rule`

用途：保存报价方案下面的增值费规则。

这张表只保存“规则配置”，不保存订单实际收费结果。订单真正收了多少、什么时候收、谁触发的，后面应该进订单费用明细或财务流水，不放在这张表。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| value_fee_rule_id | bigint | 是 | 自增 | 增值费规则 ID |
| scheme_id | bigint | 是 | 无 | 所属报价方案 ID |
| logistics_channel_code | varchar(64) | 是 | 无 | 物流渠道编码。计费方案存客户渠道编码，成本方案存系统渠道编码 |
| logistics_channel_name_snapshot | varchar(200) | 是 | 无 | 渠道名称快照，用来避免列表每次都连表展示 |
| trigger_code | varchar(32) | 是 | 无 | 触发情况。第一版先支持 `ORDER_CANCELLED` |
| calculation_method | varchar(32) | 是 | 无 | 收费方式：`PERCENT` 百分比，`FIXED_AMOUNT` 固定金额 |
| adjustment_direction | varchar(16) | 是 | `INCREASE` | 调整方向：`INCREASE` 加收，`DECREASE` 减收 |
| adjustment_value | decimal(18,6) | 是 | 无 | 调整值。百分比时 `10` 代表 10%，固定金额时 `1` 代表 1 个方案币种单位 |
| status | varchar(16) | 是 | `ENABLED` | 状态：启用/停用 |
| display_order | int | 是 | `0` | 排序 |
| create_by | varchar(64) | 否 | 空 | 创建人 |
| create_time | datetime | 否 | 无 | 创建时间 |
| update_by | varchar(64) | 否 | 空 | 更新人 |
| update_time | datetime | 否 | 无 | 更新时间 |
| remark | varchar(500) | 否 | 空 | 备注 |

## 约束和索引

- 主键：`value_fee_rule_id`
- 唯一约束：`scheme_id + logistics_channel_code + trigger_code`
  - 含义：同一个报价方案、同一个渠道、同一种触发情况，只允许有一条启用/停用规则。
- 普通索引：
  - `scheme_id, status`
  - `scheme_id, trigger_code, status`
  - `logistics_channel_code, trigger_code, status`

## 字典

新增字典：

- `quote_scheme_value_fee_trigger`
  - `ORDER_CANCELLED`：取消订单
- `quote_scheme_value_fee_calc_method`
  - `PERCENT`：按百分比调整
  - `FIXED_AMOUNT`：固定金额
- `quote_scheme_value_fee_direction`
  - `INCREASE`：加收
  - `DECREASE`：减收

状态可以复用当前前端的启用/停用选项，数据库仍保存 `ENABLED` / `DISABLED`。

## 后端接口

归属仍然是 `finance` 模块：

- `GET /finance/admin/quote-schemes/{schemeId}/value-fees/list`
- `POST /finance/admin/quote-schemes/{schemeId}/value-fees`
- `PUT /finance/admin/quote-schemes/{schemeId}/value-fees/{valueFeeRuleId}`
- `DELETE /finance/admin/quote-schemes/{schemeId}/value-fees/{valueFeeRuleId}`

权限建议新增：

- `finance:quoteScheme:valueFee`

查询报价方案详情仍用现有 `finance:quoteScheme:query`。

## 校验规则

- 报价方案不存在时不能保存。
- 计费方案只能选客户物流渠道。
- 成本方案只能选系统物流渠道。
- 触发情况第一版只允许 `ORDER_CANCELLED`。
- 收费方式只允许 `PERCENT` / `FIXED_AMOUNT`。
- 调整方向只允许 `INCREASE` / `DECREASE`。
- 调整值必须大于等于 0。
- 同一个方案、渠道、触发情况不能重复配置。

## 不在本次做

- 不接订单取消事件。
- 不计算订单实际费用。
- 不写订单费用明细或财务流水。
- 不做阶梯计费、国家分区、重量段、仓库维度的增值费。
