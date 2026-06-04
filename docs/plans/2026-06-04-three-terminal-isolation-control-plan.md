# 三端账号权限隔离与管理端控制权计划

日期：2026-06-04

## 结论

本计划替代此前“卖家/买家账号继续复用若依 `sys_user`”的设计方向。

新的方向是：

- 管理端继续使用若依 `sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`sys_oper_log` 等后台基础能力。
- 卖家端建立独立账号、角色、菜单、部门、权限、登录日志、操作日志体系。
- 买家端建立独立账号、角色、菜单、部门、权限、登录日志、操作日志体系。
- 管理端不通过混用账号体系获得控制权，而是通过平台管理接口、主体状态、账号状态、菜单/角色配置、免密代入、强制踢出和审计日志保留控制权。

当前计划只定义方案和实施顺序，不直接执行 DDL、DML、后端代码或前端代码修改。

## 设计边界

### 三端边界

| 端 | 账号来源 | 角色来源 | 菜单来源 | 部门来源 | 日志来源 |
| --- | --- | --- | --- | --- | --- |
| 管理端 | `sys_user` | `sys_role` | `sys_menu` | `sys_dept` | `sys_oper_log` / `sys_logininfor` |
| 卖家端 | `seller_account` | `seller_role` | `seller_menu` | `seller_dept` | `seller_oper_log` / `seller_login_log` |
| 买家端 | `buyer_account` | `buyer_role` | `buyer_menu` | `buyer_dept` | `buyer_oper_log` / `buyer_login_log` |

### 平台共享能力

以下能力不按三端复制，仍作为平台公共能力维护：

- 国家/地区、币种、语言、时区等公共字典。
- 商品、订单、库存、履约、财务等业务事实表，按业务归属字段隔离访问，不按端复制三份。
- 文件存储基础服务。
- 外部系统适配层。
- 平台全局参数。

## 目标模型

### 管理端

管理端保持若依核心能力：

```text
sys_user
sys_role
sys_menu
sys_dept
sys_user_role
sys_role_menu
sys_oper_log
sys_logininfor
sys_config
```

管理端账号只代表平台管理员、运营、客服、财务、风控等后台人员，不代表卖家或买家员工。

### 卖家端

卖家主体资料继续放在 `seller`。

卖家端账号体系建议表：

```text
seller_account
- seller_account_id
- seller_id
- username
- password
- nickname
- email
- phone
- dept_id
- account_type
- account_role
- status
- lock_status
- lock_reason
- last_login_ip
- last_login_time
- pwd_update_time
- create_by
- create_time
- update_by
- update_time
- remark

seller_role
seller_menu
seller_dept
seller_account_role
seller_role_menu
seller_login_log
seller_oper_log
```

第一阶段角色可以只保留：

```text
OWNER
ADMIN
STAFF
```

### 买家端

买家主体资料继续放在 `buyer`。

买家端账号体系建议表：

```text
buyer_account
- buyer_account_id
- buyer_id
- username
- password
- nickname
- email
- phone
- dept_id
- account_type
- account_role
- status
- lock_status
- lock_reason
- last_login_ip
- last_login_time
- pwd_update_time
- create_by
- create_time
- update_by
- update_time
- remark

buyer_role
buyer_menu
buyer_dept
buyer_account_role
buyer_role_menu
buyer_login_log
buyer_oper_log
```

第一阶段角色同样可以只保留：

```text
OWNER
ADMIN
STAFF
```

## 管理端控制权设计

管理端控制权来自平台接口和业务规则，不来自账号混用。

### 主体控制

管理端可以控制卖家和买家主体状态。

建议在 `seller`、`buyer` 上补充或确认以下平台控制字段：

```text
status
audit_status
risk_status
disabled_reason
disabled_by
disabled_time
platform_remark
```

规则：

- 卖家主体停用后，该卖家所有卖家端账号不可登录，不可操作。
- 买家主体停用后，该买家所有买家端账号不可登录，不可操作。
- 风控限制不一定等于停用，可用于后续限制下单、发货、提现、充值等敏感操作。

### 账号控制

管理端通过后台接口控制端内账号：

```text
/seller/admin/accounts/**
/buyer/admin/accounts/**
```

管理端能力包括：

- 创建主账号。
- 创建子账号。
- 停用账号。
- 解锁账号。
- 重置默认密码。
- 查看最后登录。
- 强制退出登录。
- 查看账号所属部门、角色和菜单权限。

这些操作必须写管理端审计日志，并明确目标端、目标主体、目标账号。

### 菜单和角色控制

卖家端菜单和角色由管理端维护，但落库到卖家端自己的表：

```text
seller_menu
seller_role
seller_role_menu
seller_account_role
```

买家端同理：

```text
buyer_menu
buyer_role
buyer_role_menu
buyer_account_role
```

管理端有配置权，卖家/买家端只有使用权。是否允许卖家或买家自定义子角色，可以后续再打开。

### 免密代入

管理端可以生成卖家端或买家端免密登录票据，但必须审计。

建议单独设计平台代入票据表：

```text
portal_direct_login_ticket
- ticket_id
- terminal
- target_subject_id
- target_account_id
- acting_admin_id
- reason
- token_hash
- expire_time
- used_time
- used_ip
- status
- create_time
```

规则：

- token 有效期 30 分钟。
- token 只能消费一次。
- token 只保存哈希，不保存明文。
- 停用主体、停用账号、过期 token 都不能使用。
- 代入操作必须记录 `acting_admin_id`，不能无痕伪装成普通端内账号。

## 登录与会话

三端登录入口分开：

```text
/admin/login
/seller/login
/buyer/login
```

token/session 必须带端类型：

```text
terminal = admin / seller / buyer
accountId = 当前端账号ID
subjectId = seller_id 或 buyer_id
```

Redis key 建议分前缀：

```text
login_tokens:admin:{token}
login_tokens:seller:{token}
login_tokens:buyer:{token}

seller_account_tokens:{seller_account_id}
buyer_account_tokens:{buyer_account_id}
seller_subject_tokens:{seller_id}
buyer_subject_tokens:{buyer_id}
```

这样管理端停用一个卖家或买家时，可以踢掉对应主体下全部在线账号。

## API 路由

建议按端分路由：

```text
/admin/**
/seller/**
/buyer/**
```

当前若依后台管理接口已经有部分路径类似：

```text
/seller/admin/sellers
/buyer/admin/buyers
```

后续可以逐步收敛为更清晰的管理端前缀，或者保留当前路径但在权限和身份校验上明确这是管理端接口。

核心规则：

- 管理端接口允许显式传 `sellerId` / `buyerId` 查询和管理。
- 卖家端接口不能相信前端传入的 `sellerId`，必须从 token 当前身份取。
- 买家端接口不能相信前端传入的 `buyerId`，必须从 token 当前身份取。

## 数据隔离

后端必须强制数据隔离。

| 端 | 数据范围 |
| --- | --- |
| 管理端 | 全局，受管理端角色权限约束 |
| 卖家端 | 当前 `seller_id` 下的数据 |
| 买家端 | 当前 `buyer_id` 下的数据 |

业务表不因为三端而复制三份，而是通过业务归属字段隔离：

```text
seller_id
buyer_id
created_by_terminal
created_by_account_id
updated_by_terminal
updated_by_account_id
```

财务、库存、外部请求日志仍然要遵守只追加和可追溯原则。

## 当前实现需要迁移的点

当前已经落地的卖家/买家管理里，以下设计需要调整：

- `seller_account.user_id` 不能再作为卖家端账号主身份。
- `buyer_account.user_id` 不能再作为买家端账号主身份。
- `PortalAccountSupport` 当前创建 `sys_user` 的逻辑要废弃或改造成只服务旧迁移。
- 重置密码从更新 `sys_user.password` 改为更新 `seller_account.password` / `buyer_account.password`。
- 最后登录时间从 `sys_user.login_date` 改为 `seller_account.last_login_time` / `buyer_account.last_login_time`。
- 免密登录从基于 `sys_user` 改为基于卖家/买家账号表。
- 列表查询中 join `sys_user owner` 的地方要改为读取端内账号表。
- 复用台账中关于 `PortalAccountSupport` 和“密码仍存 sys_user.password”的内容需要在实施时同步更新为过期或迁移说明。

## 实施阶段

### 阶段 0：冻结旧方向

目标：避免继续在 `sys_user` 上加卖家/买家端能力。

动作：

1. 标记旧文档中“卖家/买家复用 sys_user”的结论已过期。
2. 暂停继续扩展 `PortalAccountSupport`。
3. 新增需求先按三端独立身份模型评估。

验收：

- 有 Markdown 记录说明新方向。
- 没有新增依赖 `seller_account.user_id` / `buyer_account.user_id` 的功能。

### 阶段 1：账号表改造方案

目标：先确认表结构，不直接执行。

动作：

1. 输出 `seller_account` 新字段方案。
2. 输出 `buyer_account` 新字段方案。
3. 输出 `seller_role`、`seller_menu`、`seller_dept` 等基础表方案。
4. 输出 `buyer_role`、`buyer_menu`、`buyer_dept` 等基础表方案。
5. 输出迁移策略：现有绑定数据如何转成端内账号数据。

验收：

- 用户确认表设计。
- 明确回滚方式。
- 明确远程库执行计划。

### 阶段 2：数据库迁移

目标：让数据库具备三端独立账号权限基础。

动作：

1. 只读确认当前远程库表结构和现有数据量。
2. 生成 DDL/DML 执行记录。
3. 执行已确认 SQL。
4. 校验卖家/买家账号数据数量、唯一性、密码密文、主账号归属。

验收：

- `sys_user` 不再新增卖家/买家端账号。
- `seller_account` / `buyer_account` 能独立承载登录账号和密码。
- 原有卖家/买家管理列表仍可展示主账号。

### 阶段 3：后端身份改造

目标：管理端、卖家端、买家端认证分开。

动作：

1. 保留管理端若依登录。
2. 新增卖家端登录服务。
3. 新增买家端登录服务。
4. token 加入 `terminal`、`accountId`、`subjectId`。
5. 调整重置密码、最后登录、账号停用、免密登录逻辑。
6. 后端接口增加端类型校验和主体数据范围校验。

验收：

- 管理端账号不能登录卖家端/买家端。
- 卖家端账号不能登录管理端/买家端。
- 买家端账号不能登录管理端/卖家端。
- 停用主体后，该主体下账号无法登录。
- 停用账号后，该账号无法登录。

### 阶段 4：端内权限基础

目标：卖家端和买家端有自己的角色、菜单、部门。

动作：

1. 管理端新增卖家端菜单管理入口。
2. 管理端新增买家端菜单管理入口。
3. 管理端新增卖家端角色管理入口。
4. 管理端新增买家端角色管理入口。
5. 管理端新增卖家端部门/员工管理入口。
6. 管理端新增买家端部门/员工管理入口。

第一版可以只做后台配置和基础查询，不急于做完整端内 UI。

验收：

- 卖家端菜单不写入 `sys_menu`。
- 买家端菜单不写入 `sys_menu`。
- 卖家端角色不写入 `sys_role`。
- 买家端角色不写入 `sys_role`。

### 阶段 5：前端三端物理拆分

目标：账号体系稳定后再拆前端。

建议目录：

```text
admin-ui/
seller-ui/
buyer-ui/
```

当前 `react-ui/` 可以先继续作为管理端，等登录、token、菜单、权限模型稳定后再复制和精简。

验收：

- 三个前端独立构建。
- 三个前端使用不同 token storage key。
- 三个前端使用不同登录入口和菜单接口。
- 卖家端、买家端没有管理端菜单代码依赖。

## 风险和注意事项

- 这是一次身份模型调整，不是简单字段调整，必须先方案确认再改表。
- 现有 `seller_account` / `buyer_account` 已经有数据时，迁移要保留旧数据可回滚。
- 密码字段只能存 BCrypt 密文，不能在备注、日志、SQL、前端响应里保存默认密码明文。
- 管理端免密代入必须审计，不能无痕冒充端内员工。
- 卖家/买家端数据范围必须后端强制，不能只靠前端隐藏。
- 公共字典和业务事实表不要盲目复制三份，否则后续维护成本会很高。

## 下一步建议

建议下一步只做一件事：输出并确认三端独立账号权限的表结构设计。

确认通过后，再进入数据库迁移和后端改造。未确认前，不执行 DDL/DML，不改认证代码。
