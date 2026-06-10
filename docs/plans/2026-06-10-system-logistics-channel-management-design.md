# 系统物流渠道管理设计草案

日期：2026-06-10

## 当前口径

本菜单只维护 URILI 内部的系统物流渠道本体，以及系统渠道和物流商、仓库、发货地址、下单规则之间的关系。

买家范围、客户可见性、平台渠道映射不放在系统物流渠道管理中，后续归入客户渠道管理。

## 页面范围

### 主列表

主列表展示字段：

- 系统渠道代码
- 系统渠道名称
- 渠道履约模式
- 关联物流商账号
- 承运商
- 签名服务
- 状态
- 绑定仓库数量
- 最后更新人
- 最后更新时间
- 操作

主列表不展示：

- 服务等级
- 覆写地址数量
- 买家范围
- 下单规则
- 平台映射数量

### 新增和编辑

新增系统渠道采用两步交互：

1. 先填写基础信息并保存。
2. 保存成功后再显示配置区 Tabs。

编辑系统渠道时，渠道已经存在，直接展示基础信息和配置区 Tabs。

基础信息字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| 渠道履约模式 | 必填 | `物流商打单` 表示先通过物流商账号完成打单，再推送履约仓/WMS；`直推履约仓` 表示跳过系统渠道的物流商打单步骤，直接把订单或已有面单信息推给履约仓/WMS |
| 系统渠道代码 | 新增必填，编辑不可改 | 内部稳定 code |
| 系统渠道名称 | 必填 | 运营展示名 |
| 承运商 | 必填 | 使用 `logistics_final_carrier` 字典 |
| 状态 | 必填 | 启用 / 停用 |
| 签名服务 | 可选 | 直接签名、间接签名、成人签名三个复选框 |
| 备注 | 可选 | 运营备注 |

配置区 Tabs：

- 物流商映射：仅当渠道履约模式为 `物流商打单` 时展示，维护一个系统渠道可对应的多个物流商账号和外部物流商渠道，所有映射同等级。
- 仓库与发货地址：维护系统渠道可用仓库；每个绑定仓库可单独配置可选发货地址。
- 下单规则：第一版只保留预留说明和备注，不录入具体包裹规则。

## 关键业务规则

- 系统渠道代码新增后不可编辑。
- 渠道履约模式默认 `物流商打单`，存量渠道沿用该模式。
- 渠道履约模式为 `直推履约仓` 时，不展示物流商映射 Tab，后端也不允许新增物流商映射。
- 服务等级不进入第一版字段。
- 一个系统渠道可以绑定多个物流商账号和外部渠道，不区分主备。
- 添加仓库时只添加仓库，不要求配置发货地址。
- 某个渠道绑定仓库未配置发货地址时，下单使用该仓库主数据地址。
- 某个渠道绑定仓库配置了发货地址时，只对该渠道下该仓库生效。
- 外部物流商发货地址编码和详细地址字段全部可选；用户填写什么，后续推给上游时按字段传递，由上游接口决定是否接受。
- 下单规则后续会单独设计包裹长宽高、最长边、次长边、体积重、实重、计费重、危险品、地址类型和组合条件，本版不展开。
- 买家范围和平台渠道映射迁出本菜单，后续在客户渠道管理中设计。

## 表结构边界

本菜单保留：

- `logistics_system_channel`
- `logistics_carrier_channel_mapping`
- `logistics_system_channel_warehouse`
- `logistics_system_channel_order_setting`

本菜单不再新增或维护：

- `logistics_system_channel_buyer_scope`
- `logistics_platform_channel_mapping`
- `logistics_channel_buyer_scope_mode`
- `logistics_platform_kind`

## 权限

系统渠道管理保留以下权限：

| 权限 | 用途 |
| --- | --- |
| `logistics:systemChannel:list` | 菜单和主列表 |
| `logistics:systemChannel:query` | 详情、绑定、规则查询 |
| `logistics:systemChannel:add` | 新增系统渠道 |
| `logistics:systemChannel:edit` | 编辑基础信息 |
| `logistics:systemChannel:status` | 启停 |
| `logistics:systemChannel:binding` | 维护物流商映射、仓库和发货地址 |
| `logistics:systemChannel:rule` | 维护下单规则占位 |

不再保留 `logistics:systemChannel:platformMapping`。

## 接口

管理端接口：

| 接口 | 说明 |
| --- | --- |
| `GET /logistics/admin/system-channels/list` | 主列表 |
| `GET /logistics/admin/system-channels/{code}` | 详情 |
| `POST /logistics/admin/system-channels` | 新增 |
| `PUT /logistics/admin/system-channels/{code}` | 编辑 |
| `PUT /logistics/admin/system-channels/{code}/status` | 启停 |
| `GET/POST/DELETE /logistics/admin/system-channels/{code}/carrier-mappings` | 物流商映射 |
| `GET/POST/PUT/DELETE /logistics/admin/system-channels/{code}/warehouses` | 仓库绑定与仓库发货地址配置 |
| `GET/PUT /logistics/admin/system-channels/{code}/order-setting` | 下单规则占位 |

不再提供系统渠道维度的 `buyer-scope`、`platform-mappings`、`options/buyers`。
