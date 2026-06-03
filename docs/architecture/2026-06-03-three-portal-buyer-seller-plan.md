# URILI 三端与买家卖家管理方案

日期：2026-06-03

## 结论

当前验证工程应先做“逻辑三端”，不要一开始就拆成三套后端或三套权限系统。

- 管理端：沿用 `react-ui` + 若依菜单权限，负责平台运营、买家管理、卖家管理、配置、审核、财务和风控。
- 卖家端：先作为独立角色和菜单域接入若依权限，面向卖家处理商品、库存、订单、履约和账单。
- 买家端：先作为独立角色和菜单域接入若依权限，面向买家处理采购、订单、履约跟踪和对账。

后端必须保持一套若依基线：统一登录基础设施、统一用户表、统一角色、统一菜单、统一日志。买家端和卖家端不能重新做第二套账号权限体系，但账号记录必须按端分离：买家端账号和卖家端账号是两条不同的 `sys_user`。同一现实公司如果同时是买家和卖家，系统内也按两条独立客户记录处理，不建立关联。

## 为什么先做逻辑三端

原项目已经有 `admin-web`、`seller-web`、`buyer-web` 的三端思路，但当前工程的核心是若依：

- 若依已有 `sys_user`、`sys_role`、`sys_menu`、`sys_role_menu`、`sys_oper_log`。
- 当前前端基线是 `react-ui`。
- 现在直接新增 `seller-ui`、`buyer-ui` 会先扩大工程边界，还没有解决账号归属、菜单隔离、数据 scope 和后端权限问题。

因此第一阶段先在一套若依体系内跑通：

1. 买家/卖家主体建模。
2. 买家/卖家账号分别绑定不同若依用户。
3. 买家/卖家角色绑定不同菜单。
4. 买家/卖家接口按登录主体自动限制数据范围。
5. 管理端可以创建、启停、重置密码、代登录买家/卖家。

等这些核心规则稳定后，再决定是否物理拆成独立 `seller-ui`、`buyer-ui`。

## 三端边界

| 端 | 使用者 | 主要入口 | 权限来源 | 数据范围 |
| --- | --- | --- | --- | --- |
| 管理端 | 平台管理员、运营、财务、客服 | `react-ui` 管理后台 | 若依后台角色 | 全局或按运营权限 |
| 卖家端 | 卖家主账号、卖家员工 | 卖家菜单域 | 若依卖家角色 + 卖家主体账号绑定 | 只能看自己的卖家数据 |
| 买家端 | 买家主账号、买家员工 | 买家菜单域 | 若依买家角色 + 买家主体账号绑定 | 只能看自己的买家数据 |

## 买家卖家管理要先做什么

管理端的“卖家管理”和“买家管理”不是普通客户列表，而是三端账号体系的入口。

### 卖家管理

管理端创建卖家时，应同时处理：

- 卖家客户资料：内部客户编号、对外客户代码、客户名称、简称、联系人、国家、州/省、城市、邮编、地址行、状态、等级等。
- 卖家登录账号：绑定或创建卖家端专用 `sys_user`。
- 卖家角色：绑定 `URILI_SELLER_ADMIN` 或后续细分角色。
- 卖家数据范围：建立卖家端 `sys_user` 到卖家主体的绑定关系。
- 审计记录：创建、修改、启停、重置密码、代登录都要留操作日志。

### 买家管理

买家管理同理，但绑定买家主体和买家角色：

- 买家客户资料：内部客户编号、对外客户代码、客户名称、简称、联系人、国家、州/省、城市、邮编、地址行、状态、等级等。
- 买家登录账号：绑定或创建买家端专用 `sys_user`。
- 买家角色：绑定 `URILI_BUYER_ADMIN` 或后续细分角色。
- 买家数据范围：建立买家端 `sys_user` 到买家主体的绑定关系。
- 审计记录。

## 建议表结构

第一阶段不要改造若依 `sys_user` 的语义，只新增业务主体和端账号绑定表。

```text
urili_customer
- customer_id
- customer_no
- customer_code
- customer_name
- customer_short_name
- customer_kind      # seller / buyer
- status             # enabled / disabled
- country_code
- state_province
- city
- postal_code
- address_line1
- address_line2
- contact_name
- contact_phone
- contact_email
- remark
- create_by
- create_time
- update_by
- update_time

urili_customer_account
- id
- customer_id
- user_id            # sys_user.user_id
- portal_type        # seller / buyer
- account_role       # owner / admin / staff
- status
- create_time
- update_time
```

核心原则：

- 登录账号仍然是 `sys_user`。
- 买家/卖家业务主体放在 `urili_customer`。
- 账号和业务主体通过 `urili_customer_account` 绑定。
- `customer_no` 是系统自动生成的内部客户编号。
- `customer_code` 是人工维护的必填对外客户代码，用于客户沟通和外部系统，同一端内必须唯一。
- 买家端和卖家端必须是不同 `sys_user`，即使现实中是同一个公司。
- 同一现实公司同时作为买家和卖家时，在系统内也是两条独立 `urili_customer`，不建立关联。
- 买家/卖家登录名不强制加 `buyer_`、`seller_` 前缀。
- 一个账号是否能进卖家端或买家端，由角色、`portal_type` 和绑定关系共同决定。

## 菜单和权限

菜单要分成三个域，但仍放在若依 `sys_menu`：

```text
URILI管理端
- 客户管理
  - 卖家管理
  - 买家管理
- 仓储管理
- 上游系统
- 渠道管理
- 计费管理

卖家工作台
- 商品管理
- 库存管理
- 订单处理
- 履约管理
- 账单对账

买家工作台
- 商品目录
- 采购下单
- 采购订单
- 履约跟踪
- 对账付款
```

权限 key 建议按端区分：

```text
urili:admin:seller:list
urili:admin:buyer:list
urili:seller:product:list
urili:seller:order:process
urili:buyer:catalog:list
urili:buyer:order:create
```

管理端权限允许显式传 `sellerId`、`buyerId` 查询；卖家端和买家端接口不应该相信前端传来的主体 ID，而应从当前端账号绑定关系中取当前主体。

## API 分层

建议后端接口按端拆路径：

```text
/admin/urili/customers/sellers
/admin/urili/customers/buyers
/seller/urili/profile
/seller/urili/orders
/buyer/urili/profile
/buyer/urili/orders
```

规则：

- `/admin/**`：平台管理接口，可以管理多个买家/卖家。
- `/seller/**`：卖家端接口，只能访问当前登录卖家主体的数据。
- `/buyer/**`：买家端接口，只能访问当前登录买家主体的数据。
- 数据 scope 必须在后端校验，不能只靠前端菜单隐藏。

## 登录和代登录

普通登录：

- 三端可以共用若依登录基础能力。
- 登录入口或登录请求必须带明确端类型：`admin`、`seller`、`buyer`。
- 登录成功后校验账号是否属于对应端。
- 同一家公司如果同时是买家和卖家，也必须分别创建买家客户、卖家客户，并分别用买家端账号和卖家端账号登录，不做同账号切换买卖身份。

管理端代登录：

- 不直接暴露买家/卖家密码。
- 管理端生成短时一次性代登录票据。
- 买家端或卖家端用票据换取登录 token。
- 必须写入操作日志，记录管理员、目标客户、时间、IP 和原因。

## 前端落地顺序

### 第一阶段：不拆工程

在 `react-ui` 内先跑通三端逻辑：

1. 管理端继续使用现有若依布局。
2. 新增买家/卖家管理页面。
3. 新增卖家和买家的角色、菜单字段，但未实现页面时保持停用。
4. 登录后根据后端菜单显示对应端的菜单域。

### 第二阶段：端内页面成型

当卖家端和买家端页面数量明显增加后，再评估是否从 `react-ui` 拆出独立应用：

```text
react-ui/      # 管理端
seller-ui/     # 卖家端
buyer-ui/      # 买家端
```

如果要拆，必须先更新 `AGENTS.md` 和目录规则，再迁移代码。不能在当前规则下直接新增第二、第三套前端。

## 当前最小下一步

建议下一步只做三件事：

1. 定义 `urili_customer` 和 `urili_customer_account` 字段草案。
2. 调整已建菜单字段，把管理端权限 key 从 `urili:customer:*` 收敛为 `urili:admin:*`，避免以后和买家/卖家端权限混在一起。
3. 新增卖家端、买家端的菜单字段草案，但保持 `status = '1'` 停用，不做页面内容。
