# URILI 多用户商城三端最终方向

日期：2026-06-03

> 过期说明：本文是早期三端方向草案，其中“买家端账号和卖家端账号必须是不同 `sys_user`”的实现口径已废弃。当前三端独立账号权限方向以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准。

## 结论

URILI 是多用户交易商城，不是普通后台系统，也不是传统 C 端电商前台。最终方向是：

```text
一套若依后端
一套若依登录基础设施
一套若依角色菜单权限
三套物理前端：admin-ui / seller-ui / buyer-ui
卖家客户和买家客户在系统内独立建档
```

当前阶段先以 `react-ui` 作为管理端改造入口，不立即复制三份前端。等客户模型、端账号、菜单域和管理端流程稳定后，再拆出三个前端项目。

## 已确认业务规则

- 使用 `customer` 命名，不使用 `party`。
- 同一现实公司可以同时是卖家和买家，但系统内不建立关联。
- 卖家管理中创建的是卖家客户；买家管理中创建的是买家客户。
- 如果同一家公司既是卖家又是买家，也会生成两条客户记录和两个端账号。
- 买家端账号和卖家端账号必须是不同的 `sys_user`。
- 买家/卖家登录名不自动加 `buyer_`、`seller_` 前缀。
- 地址第一版拆成国家、州/省、城市、邮编、地址行。
- 客户识别使用两个字段：`customer_no` 为系统自动生成的内部客户编号，`customer_code` 为人工维护的对外客户代码。

## 为什么不用统一 party

之前考虑过统一 `party`，让同一家公司可以同时拥有 seller 和 buyer 能力。但当前业务规则明确：即使现实中是同一家公司，系统也应分别在卖家管理和买家管理中生成账号和客户档案，二者不建立关系。

因此当前采用：

```text
urili_customer
urili_customer_account
```

而不是：

```text
urili_party
urili_party_capability
urili_party_account
```

这个选择的好处：

- 管理端逻辑更直接。
- 买家端和卖家端账号天然隔离。
- 审计日志不会混淆买家行为和卖家行为。
- 订单、财务、履约可以明确区分买方客户和卖方客户。
- 不需要处理同一公司跨端身份切换。

代价：

- 同一现实公司的基础信息可能重复录入。
- 后续如果需要集团级统一视图，需要另做关联模型或手工匹配能力。

这个代价当前可接受，因为三端账号隔离和业务清晰度更重要。

## 核心模型

### urili_customer

客户档案。通过 `customer_kind` 区分卖家客户和买家客户。

```text
customer_id
customer_no
customer_code
customer_kind      # seller / buyer
customer_name
customer_short_name
customer_type
customer_level
status
country_code
state_province
city
postal_code
address_line1
address_line2
contact_name
contact_phone
contact_email
remark
create_by
create_time
update_by
update_time
```

`customer_no` 是系统内部客户编号，不是登录名。建议系统自动生成，管理端只读展示。

`customer_code` 是人工维护的必填对外客户代码，用于客户沟通、外部系统、导入导出和对账。它不代替 `customer_no`，也不作为登录名。同一端内 `customer_code` 必须唯一。

### urili_customer_account

客户的端账号绑定。

```text
account_id
customer_id
user_id            # sys_user.user_id
portal_type        # seller / buyer
account_role       # owner / admin / staff
status
create_by
create_time
update_by
update_time
```

核心约束：

- `portal_type` 必须与 `urili_customer.customer_kind` 一致。
- 一个 `sys_user` 只能绑定一个客户端账号身份。
- 卖家端账号和买家端账号必须是两条不同的 `sys_user`。

## 三端定义

### 管理端

职责：

- 管理卖家客户。
- 管理买家客户。
- 创建对应端账号。
- 启停客户和账号。
- 重置密码。
- 后续代登录。
- 商品审核、价格上架、订单监控、财务结算。

### 卖家端

职责：

- 管理自己的商品、库存、仓库。
- 处理卖家侧订单和履约。
- 查看卖家账单和费用明细。

数据范围从卖家端登录账号绑定的 `customer_id` 推导，不能相信前端传入的 sellerId。

### 买家端

职责：

- 查看商品目录。
- 创建采购订单。
- 跟踪履约。
- 查看对账和付款信息。

数据范围从买家端登录账号绑定的 `customer_id` 推导，不能相信前端传入的 buyerId。

## 权限和数据隔离

若依 `sys_role` 负责“能不能访问功能”，URILI 的 `customer_id + portal_type` 负责“能访问哪些业务数据”。

管理端权限示例：

```text
urili:admin:seller:list
urili:admin:seller:add
urili:admin:seller:edit
urili:admin:seller:changeStatus
urili:admin:seller:resetPwd

urili:admin:buyer:list
urili:admin:buyer:add
urili:admin:buyer:edit
urili:admin:buyer:changeStatus
urili:admin:buyer:resetPwd
```

卖家端权限示例：

```text
urili:seller:product:list
urili:seller:warehouse:list
urili:seller:order:process
urili:seller:bill:list
```

买家端权限示例：

```text
urili:buyer:catalog:list
urili:buyer:order:create
urili:buyer:order:list
urili:buyer:statement:list
```

## 前端拆分节奏

### 第一阶段

使用 `react-ui` 做管理端：

- 跑通若依登录。
- 跑通管理端菜单。
- 跑通卖家管理、买家管理。
- 跑通客户和端账号绑定。

### 第二阶段

拆出：

```text
admin-ui
seller-ui
buyer-ui
```

拆分前必须先抽共享能力：

- 请求封装。
- 登录 token 处理。
- 字典和选项。
- 权限判断。
- 格式化工具。
- 通用表格/表单基础组件。

### 第三阶段

三端独立部署：

- 管理端给平台人员使用。
- 卖家端给卖家客户使用。
- 买家端给买家客户使用。

## 当前下一步

先做管理端：

1. 确认 `urili_customer` 和 `urili_customer_account` 字段。
2. 生成 SQL 设计稿。
3. 实现后端管理端接口。
4. 实现 `react-ui` 中的卖家管理和买家管理页面。
