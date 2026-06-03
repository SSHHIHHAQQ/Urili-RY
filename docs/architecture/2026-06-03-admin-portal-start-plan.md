# URILI 管理端第一步实施规格

日期：2026-06-03

## 当前决策

最终方向已经确定为三前端：

```text
admin-ui / seller-ui / buyer-ui
```

当前阶段不立即复制三份前端。先把 `react-ui` 作为管理端改造入口，用若依后端跑通平台侧客户、端账号、菜单和权限。

## 用户已确认的规则

- 业务命名使用 `customer`，不用 `party`。
- 卖家客户和买家客户对系统而言是两类独立客户记录。
- 现实中同一家公司如果既是卖家又是买家，也要分别在卖家管理和买家管理中创建客户与账号；系统不建立二者关联。
- 买家端账号和卖家端账号必须是不同 `sys_user`。
- 地址第一版直接拆成国家、州/省、城市、邮编、地址行。
- 买家/卖家登录名不自动加 `buyer_`、`seller_` 前缀；从客户自身视角看，账号就是自己的账号。
- 客户识别使用两个字段：系统自动生成的内部客户编号，以及人工维护的对外客户代码。

## 客户编号和客户代码

第一版保留两个字段：

| 字段 | 中文名 | 维护方式 | 用途 |
| --- | --- | --- | --- |
| `customer_no` | 内部客户编号 | 系统自动生成，只读 | 系统内部引用、订单、账单、日志、客服查询 |
| `customer_code` | 对外客户代码 | 管理端人工维护 | 跟客户、外部系统、导入导出、对账沟通时使用 |

`customer_no` 不是登录名，也不是给客户自己维护的代码。

`customer_no` 的用途：

- 给运营、客服、财务、订单、日志一个稳定编号。
- 避免直接暴露数据库自增 ID。
- 避免客户名称变化后影响查询、对账和引用。
- 后续订单、库存、账单、履约记录可以引用客户编号做展示和检索。

`customer_code` 的用途：

- 客户和平台日常沟通时使用。
- 对接外部系统、导入导出、账单对账时使用。
- 可以按客户已有编号维护，便于客户识别。

卖家客户和买家客户各自独立生成 `customer_no`，即使现实中公司名称相同，也会生成两条不同内部编号。`customer_code` 也分别维护，不建立系统关联。

## 管理端第一步目标

先做“平台管理端的买家/卖家管理基础”，不做买家端和卖家端页面。

第一步只覆盖：

- 卖家客户资料。
- 买家客户资料。
- 买家端账号和卖家端账号分别绑定不同 `sys_user`。
- 管理端菜单和权限命名进入 `urili:admin:*` 域。
- 后续为代登录、启停、重置密码、审计留接口和权限空间。

不覆盖：

- 商品、库存、订单、履约、财务。
- 买家端页面。
- 卖家端页面。
- 三前端物理复制。
- 买家客户和卖家客户之间的事实关联。

## 数据模型草案

### urili_customer

客户主体。卖家客户和买家客户分别建记录。

| 字段 | 类型建议 | 必填 | 说明 |
| --- | --- | --- | --- |
| customer_id | bigint | 是 | 主键 |
| customer_no | varchar(64) | 是 | 系统内部客户编号，唯一，自动生成，只读 |
| customer_code | varchar(64) | 是 | 对外客户代码，人工维护，用于客户和外部系统沟通 |
| customer_kind | varchar(32) | 是 | `seller` / `buyer` |
| customer_name | varchar(200) | 是 | 公司/客户全称 |
| customer_short_name | varchar(100) | 否 | 简称 |
| customer_type | varchar(32) | 是 | 客户类型，第一阶段默认 `company` |
| customer_level | varchar(32) | 否 | 客户等级，可后续接字典 |
| status | char(1) | 是 | `0` 正常，`1` 停用，沿用若依风格 |
| country_code | varchar(32) | 是 | 国家 code |
| state_province | varchar(100) | 是 | 州/省 |
| city | varchar(100) | 是 | 城市 |
| postal_code | varchar(32) | 是 | 邮编 |
| address_line1 | varchar(255) | 是 | 地址行 1 |
| address_line2 | varchar(255) | 否 | 地址行 2 |
| contact_name | varchar(100) | 否 | 主联系人 |
| contact_phone | varchar(64) | 否 | 联系电话 |
| contact_email | varchar(128) | 否 | 联系邮箱 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 否 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

建议索引：

- `unique(customer_no)`
- `unique(customer_kind, customer_code)`：同一端客户代码唯一。
- `index(customer_kind, status)`
- `index(customer_name)`

重要规则：

- 同一现实公司可以在 `seller` 和 `buyer` 下各有一条 `urili_customer`。
- 这两条客户记录不做系统关联。
- 客户名称、联系人、地址可以相同，但内部客户编号、对外客户代码、账号、权限和业务数据彼此独立。

### urili_customer_account

客户端账号。买家端账号和卖家端账号必须是不同 `sys_user`。

| 字段 | 类型建议 | 必填 | 说明 |
| --- | --- | --- | --- |
| account_id | bigint | 是 | 主键 |
| customer_id | bigint | 是 | 关联 `urili_customer.customer_id` |
| user_id | bigint | 是 | 关联 `sys_user.user_id` |
| portal_type | varchar(32) | 是 | `seller` / `buyer`，必须与 `customer_kind` 一致 |
| account_role | varchar(32) | 是 | `owner` / `admin` / `staff` |
| status | char(1) | 是 | `0` 正常，`1` 停用 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 否 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

建议约束：

- `unique(user_id)`：一个登录账号只能绑定一个客户端账号身份。
- `unique(customer_id, user_id)`：防止同一客户重复绑定同一用户。
- `index(customer_id, portal_type, status)`。

## 管理端菜单

当前菜单字段以 `RuoYi-Vue/sql/urili_top_menu_seed.sql` 为顶级菜单口径，已不再使用旧的 `URILI运营后台` 包裹根菜单。

当前管理端顶级菜单：

```text
客户管理
- 卖家管理     urili:admin:seller:list
- 买家管理     urili:admin:buyer:list

商品管理
订单管理
库存管理
仓库管理
海外仓服务设置
财务管理
日志中心
工具中心
```

已建权限统一进入管理端权限域：

```text
urili:admin:*
```

未实现页面的子菜单和按钮权限继续保持停用，避免暴露空页面。

## 管理端卖家管理

第一版页面建议字段：

- 客户编号。
- 客户代码。
- 客户名称。
- 客户简称。
- 国家。
- 州/省。
- 城市。
- 邮编。
- 地址行 1。
- 地址行 2。
- 联系人。
- 联系电话。
- 联系邮箱。
- 状态。
- 卖家端账号数。
- 创建时间。

第一版操作：

- 查询。
- 新增卖家客户。
- 编辑卖家客户资料。
- 启停卖家客户。
- 创建卖家端账号。
- 重置卖家端账号密码。
- 登录卖家端：先保留权限和按钮字段，具体代登录机制后续实现。

## 管理端买家管理

第一版页面建议字段：

- 客户编号。
- 客户代码。
- 客户名称。
- 客户简称。
- 国家。
- 州/省。
- 城市。
- 邮编。
- 地址行 1。
- 地址行 2。
- 联系人。
- 联系电话。
- 联系邮箱。
- 状态。
- 买家端账号数。
- 创建时间。

第一版操作：

- 查询。
- 新增买家客户。
- 编辑买家客户资料。
- 启停买家客户。
- 创建买家端账号。
- 重置买家端账号密码。
- 登录买家端：先保留权限和按钮字段，具体代登录机制后续实现。

## 后端接口草案

第一阶段接口按管理端域命名：

```text
GET    /urili/admin/sellers
POST   /urili/admin/sellers
GET    /urili/admin/sellers/{customerId}
PUT    /urili/admin/sellers/{customerId}
PUT    /urili/admin/sellers/{customerId}/status
POST   /urili/admin/sellers/{customerId}/accounts
POST   /urili/admin/sellers/{customerId}/accounts/{userId}/reset-password

GET    /urili/admin/buyers
POST   /urili/admin/buyers
GET    /urili/admin/buyers/{customerId}
PUT    /urili/admin/buyers/{customerId}
PUT    /urili/admin/buyers/{customerId}/status
POST   /urili/admin/buyers/{customerId}/accounts
POST   /urili/admin/buyers/{customerId}/accounts/{userId}/reset-password
```

后端权限：

```text
urili:admin:seller:list
urili:admin:seller:query
urili:admin:seller:add
urili:admin:seller:edit
urili:admin:seller:changeStatus
urili:admin:seller:resetPwd
urili:admin:seller:directLogin

urili:admin:buyer:list
urili:admin:buyer:query
urili:admin:buyer:add
urili:admin:buyer:edit
urili:admin:buyer:changeStatus
urili:admin:buyer:resetPwd
urili:admin:buyer:directLogin
```

## 与若依表的关系

- `sys_user`：仍然是登录账号表。
- `sys_role`：卖家端账号和买家端账号分配不同角色。
- `sys_user_role`：绑定账号角色。
- `sys_menu`：功能权限点。
- `sys_oper_log`：记录管理端创建、修改、启停、重置密码、代登录等操作。
- `sys_dept`：不用于买家/卖家客户。买家和卖家是交易客户，不是平台内部部门。

## 进入实现前仍需确认

1. `customer_no` 生成规则是否采用 `C` + 年月日 + 4 位流水，还是按买家/卖家分前缀。
2. `customer_type`、`customer_level` 第一版是否需要，还是先删除。
3. 国家、州/省、城市第一版用文本输入，还是马上接若依字典/选项。
