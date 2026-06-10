# 系统物流渠道管理实现记录

## 目标

实现管理端“系统物流渠道管理”菜单，用于维护平台内部系统物流渠道。

当前确认口径：

- 系统渠道只维护渠道基础信息、物流商映射、仓库与发货地址、下单规则占位。
- 系统渠道基础信息新增“渠道履约模式”：`物流商打单` 或 `直推履约仓`。
- 买家范围和平台渠道映射迁出系统渠道管理，后续归入客户渠道管理。
- 服务等级不进入第一版。

## 实现范围

### 后端

- 管理端接口：`/logistics/admin/system-channels`。
- 权限点：
  - `logistics:systemChannel:list`
  - `logistics:systemChannel:query`
  - `logistics:systemChannel:add`
  - `logistics:systemChannel:edit`
  - `logistics:systemChannel:status`
  - `logistics:systemChannel:binding`
  - `logistics:systemChannel:rule`
- 已移除系统渠道维度：
  - 买家范围接口和 DTO。
  - 买家选项接口。
  - 平台渠道映射接口和 DTO。
  - `logistics:systemChannel:platformMapping` 权限。
  - 服务等级字段读写。
- 保留系统渠道基础信息 `signature_services`，保存签名服务 code 集合。
- 新增系统渠道基础信息 `fulfillment_mode`，默认 `CARRIER_LABELING`。
- `DIRECT_FULFILLMENT_WAREHOUSE` 模式不允许新增物流商映射。
- 仓库绑定必须校验仓库存在且启用。
- 添加仓库时只绑定仓库，不要求填写发货地址。
- 绑定仓库未配置发货地址时，默认使用仓库主数据地址。
- 配置发货地址时，外部物流商发货地址编码和详细地址字段全部可选。

### 前端

- 页面：`react-ui/src/pages/Channel/System/index.tsx`。
- API service：`react-ui/src/services/logistics/systemChannel.ts`。
- 使用 Ant Design Pro `ProTable`、`ProForm`、`ModalForm`、`Tabs` 和 Ant Design 原生控件。
- 主列表展示：
  - 系统渠道代码
  - 系统渠道名称
  - 渠道履约模式
  - 关联物流商账号
  - 承运商
  - 签名服务
  - 状态
  - 绑定仓库数量
  - 最后更新人和更新时间
- 主列表已移除：
  - 服务等级
  - 覆写地址
  - 买家范围
  - 下单规则
  - 平台映射
- 新增系统渠道为两步交互：先保存基础信息，再显示配置区。
- 编辑系统渠道直接展示基础信息和配置区。
- 配置区 Tabs 仅保留：
  - 物流商映射：仅 `物流商打单` 模式展示
  - 仓库与发货地址
  - 下单规则
- 已删除编辑弹窗中的绑定买家和平台渠道映射 Tabs。
- 下单规则本版仅保留预留区和规则备注。

### SQL

- `RuoYi-Vue/sql/20260610_system_logistics_channel_management.sql` 已同步当前口径：
  - 新增 `fulfillment_mode` 字段，默认 `CARRIER_LABELING`。
  - 新增 `logistics_system_channel_fulfillment_mode` 字典。
  - 不再创建 `service_level`。
  - 不再创建 `logistics_system_channel_buyer_scope`。
  - 不再创建 `logistics_platform_channel_mapping`。
  - 不再 seed `logistics_channel_buyer_scope_mode`。
  - 不再 seed `logistics_platform_kind`。
  - 不再 seed `logistics:systemChannel:platformMapping`。
- `RuoYi-Vue/sql/20260610_logistics_carrier_management.sql` 已同步移除 `logistics_system_channel.service_level` 建表字段。

## 验证记录

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
.\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\logistics-system-channel-contract.test.ts --runInBand
```

结果：通过，5 个用例通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
.\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\logistics-carrier-contract.test.ts --runInBand
```

结果：通过，6 个用例通过。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl logistics,ruoyi-admin -am "-Dtest=LogisticsAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：通过，12 个用例通过，`BUILD SUCCESS`。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：通过，81 个用例通过，`BUILD SUCCESS`。

### 浏览器验证

本次新增 `fulfillment_mode` 字段后，增量 SQL 尚未执行，未做新增/编辑提交的浏览器运行态验证；待数据库字段落库并重启后端后再补充验证。

- 页面地址：`http://127.0.0.1:8001/overseas-warehouse-service/channel-system`。
- 上一轮已验证主列表列为：系统渠道代码、系统渠道名称、关联物流商账号、承运商、签名服务、状态、绑定仓库数量、最后更新人、最后更新时间、操作。
- 已验证主列表不再出现服务等级、覆写地址、买家范围、下单规则、平台映射。
- 已验证编辑弹窗基础信息不再出现服务等级。
- 已验证编辑弹窗 Tabs 仅保留：物流商映射、仓库与发货地址、下单规则。
- 已验证编辑弹窗不再出现绑定买家、买家范围、平台渠道映射、平台映射。
- 控制台未观察到本次改动导致的运行时错误；仅有既有 AntD Drawer `width` 弃用提示。

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：完成，输出 `Already up to date`。
