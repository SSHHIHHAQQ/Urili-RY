# 2026-06-12 买家端商城权限架构审查

## 结论

当前买家端权限方向与“电商商城”预设方向基本一致，可以继续在这个基础上做商品中心、订单、售后、财务等业务模块。

但当前实现还不是最终形态。它已经具备端内账号、角色、菜单、session、接口权限的基础框架；缺的是一层更明确的“商城业务数据授权”抽象。后续订单、售后、财务如果直接在各自 Controller 或页面里零散判断，会把权限体系做乱。因此建议在继续铺大量业务菜单前，先把权限模型按两层固定下来：

- 功能入口权限：当前账号能不能看到菜单、进入页面、点击按钮。
- 业务数据权限：当前买家主体能不能看、买、下单、售后、结算某一条商品、订单、账单或售后单。

## 当前已满足的方向

### 三端账号权限隔离

当前买家端已经使用独立控制面：

- `buyer_account`
- `buyer_role`
- `buyer_menu`
- `buyer_account_role`
- `buyer_role_menu`
- `buyer_login_log`
- `buyer_oper_log`

买家端 portal 接口通过 `@PortalPreAuthorize(terminal = "buyer", ...)` 和 `PortalSessionContext.requireSession("buyer")` 获取当前登录主体，不复用后台 `sys_user` / `sys_role` / `sys_menu` 做端内控制。这一方向是正确的。

### 子账号角色授权

当前买家端角色是按 `buyer_id` 隔离的，账号绑定角色时也会校验账号和角色都属于当前 buyer。商品中心权限已进入 owner 默认授权，并允许在买家端子账号角色里分配。

这符合商城里的常见模型：公司主账号/owner 拥有完整功能，再给采购、财务、售后、仓库等员工分配不同功能入口。

### 商品中心第一版

当前商品中心采用：

- 菜单权限：`buyer:product:center:list`
- 详情权限：`buyer:product:center:query`
- 数据范围：公共在售商品，暂不按买家限制
- 身份来源：后端 session 派生，不信前端传入 `buyerId`

这符合第一版“所有买家都能看到所有上架商品”的要求。

## 当前不够舒服的点

### `PORTAL_SELF_MANAGEMENT_PERMS` 职责已经变宽

当前 `PORTAL_SELF_MANAGEMENT_PERMS` 既包含账号、部门、角色、日志、会话这些自助管理权限，也包含 `buyer:product:center:*` 业务入口权限。功能上可以运行，但命名已经不准确。

如果后续继续加入：

- `buyer:order:*`
- `buyer:afterSale:*`
- `buyer:finance:*`
- `buyer:cart:*`
- `buyer:payment:*`

继续塞进 `SELF_MANAGEMENT` 语义里会误导后续实现。

建议后续小重构为类似：

- `BUYER_PORTAL_ASSIGNABLE_PERMS`：允许 owner 给子账号角色分配的权限全集。
- `BUYER_PORTAL_NAVIGATION_PERMS`：允许出现在 portal 菜单树和前端导航里的权限。
- `BUYER_PORTAL_SELF_MANAGEMENT_PERMS`：只保留账号、部门、角色、日志、会话。
- `BUYER_PORTAL_BUSINESS_PERMS`：商品、订单、售后、财务等业务入口。

### 还缺统一的商城业务数据授权层

功能入口权限不能替代业务数据权限。

例如后续订单模块不能只判断 `buyer:order:query`，还必须判断：

- 当前订单是否属于当前 `buyer_id`
- 当前账号是否允许查看本部门或全公司订单
- 当前订单状态是否允许取消、支付、售后
- 当前买家主体是否被风控限制下单或付款
- 财务账单是否只能给财务角色查看

商品中心当前只有“公共在售商品”规则，后续如果变成买家白名单、等级价、协议商品、黑名单、国家/仓库限制、渠道限制，就需要独立的数据授权模型，而不是扩展 `buyer_menu`。

## 推荐商城权限模型

### 第一层：平台管理端控制权

管理端继续使用若依后台权限：

- `sys_user`
- `sys_role`
- `sys_menu`
- `sys_oper_log`

管理端负责配置商品、类目、上下架、管控状态、买家主体状态、订单规则、售后规则、财务配置、免密代入、强制下线和审计。

管理端权限命名继续使用后台命名空间，例如：

- `product:center:list`
- `order:admin:list`
- `afterSale:admin:handle`
- `finance:statement:list`
- `buyer:admin:*`

### 第二层：买家端功能入口权限

买家端继续使用 `buyer_menu` / `buyer_role_menu` 控制功能入口。

推荐后续命名：

- 商品浏览：`buyer:product:center:list/query`
- 购物车：`buyer:cart:list/add/edit/remove`
- 下单：`buyer:order:list/query/create/cancel`
- 售后：`buyer:afterSale:list/query/apply/cancel`
- 财务：`buyer:finance:statement:list/query`、`buyer:finance:invoice:list/query`
- 账号组织：继续使用 `buyer:account:*`、`buyer:role:*`、`buyer:dept:*`

这里的权限只表示“账号能不能使用功能”，不表示“能不能访问某条业务数据”。

### 第三层：买家业务数据范围

所有买家端业务接口都应该从 session 派生：

```text
terminal = buyer
buyerId = session.subjectId
accountId = session.accountId
```

不要相信前端传入：

- `buyerId`
- `subjectId`
- `accountId`
- `terminal`
- 任意可扩大数据范围的 scope 参数

后续建议形成统一上下文：

```text
BuyerPortalAccessContext
- buyerId
- accountId
- roles
- permissions
- deptId
- dataScope
```

然后每个业务模块通过自己的 service/facade 收口：

- 商品：`BuyerProductVisibilityService`
- 订单：`BuyerOrderAccessService`
- 售后：`BuyerAfterSaleAccessService`
- 财务：`BuyerFinanceAccessService`

### 第四层：业务状态规则

电商商城里很多操作不是纯权限判断，还要叠加状态机：

- 只有可售 SKU 才能加入购物车或下单。
- 只有未支付/未审核订单才能取消。
- 只有已发货/已签收订单才能申请某些售后。
- 只有财务角色能看账单、发票、付款信息。
- 买家主体被风控限制时，可以允许浏览商品，但禁止下单或支付。

这些规则必须落在业务 service，不要只靠前端按钮隐藏。

## 对当前商品中心的判断

当前商品中心第一版可以继续使用，不需要推倒重做。它已经满足：

- 买家端独立权限命名。
- owner 默认授权。
- 子账号角色可分配。
- 后端从 buyer session 派生主体。
- 第一版公共在售商品可见。
- 商品读取通过 buyer facade 做 DTO 收口。

但后续如果要进入“可购买、加入购物车、提交订单”，不要直接在商品中心页面里加一个按钮调用订单接口。应该先确认：

- 商品是否有买家可购买范围。
- SKU 是否有买家可见价格。
- 库存是否对买家承诺。
- 下单时是否需要采购单/购物车/询价单中间态。
- 订单归属 buyer 还是 buyer_account/dept。
- 财务结算对象是买家主体、部门还是账号。

## 建议下一步

在继续做订单、售后、财务前，建议先做一个小的架构补强，不需要大改表：

1. 把 `PORTAL_SELF_MANAGEMENT_PERMS` 拆成更准确的权限目录命名，避免业务权限继续塞进“自助管理”概念。
2. 固定买家端业务接口模板：`@PortalPreAuthorize` + `PortalSessionContext` + `BuyerPortalAccessContext` + service 层 subject-scope。
3. 为订单/售后/财务提前约定权限命名，不要把业务操作都挂到 `product:center` 下。
4. 商品可见范围先继续保留“公共在售商品”，但在商品 service 层保留未来白名单、等级价、协议商品、风控限制入口。

最终判断：当前基础架构方向可用；如果只是继续做商品中心列表/详情，可以继续推进。如果要开始做购物车、下单、订单列表、售后和财务，建议先完成上述小补强，再铺业务功能。

## 2026-06-12 补强状态

已完成第一项小补强：

- 新增 `BuyerPortalPermissionCatalog`。
- `SELF_MANAGEMENT_PERMS` 只保留账号、部门、角色、日志、会话。
- `BUSINESS_PERMS` 独立承载 `buyer:product:center:list/query`。
- `ROLE_ASSIGNABLE_PERMS` 作为 owner 可分配给子账号角色的权限全集。
- `NAVIGATION_PERMS` 作为 portal 导航和 getRouters 可见权限全集。
- `BuyerPortalController`、`BuyerPortalPermissionServiceImpl`、`BuyerServiceImpl.DEFAULT_OWNER_PERMS` 已改为引用该 catalog。

后续订单、售后、财务权限应继续进入 `BUSINESS_PERMS` 或拆出更细业务 catalog，不再塞回“自助管理权限”概念。
