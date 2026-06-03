# 卖家/买家管理操作功能对照检查

日期：2026-06-04

范围：

- 原项目：`E:\Urili\apps\admin-web`、`E:\Urili\packages\contracts`、`E:\Urili\packages\modules\seller`、`E:\Urili\packages\modules\buyer`
- 当前若依工程：`E:\Urili-Ruoyi\RuoYi-Vue`、`E:\Urili-Ruoyi\react-ui`
- 本次只做静态代码和功能入口对照；未执行数据库 DDL/DML，未修改功能代码。

## 结论摘要

当前若依工程已经完成了卖家/买家管理的基础主体资料和账号管理骨架，但没有完全覆盖原项目管理端暴露出来的所有操作。

已经基本覆盖：

- 卖家、买家分菜单、分页面、分后端模块。
- 卖家表、买家表、卖家账号绑定表、买家账号绑定表的拆分方向。
- 列表分页、详情回填、新增、编辑、启停。
- 内部编号、客户代码、名称、简称、登录账号、客户类型、等级、联系人、电话、邮箱、国家/地区、州/省、城市、邮编、地址、备注等资料字段。
- 国家/地区使用若依字典 `country_region`，前端支持模糊搜索下拉。
- 客户类型支持公司、个人、其他。
- 创建主体时自动创建主账号；每个客户可继续创建子账号。
- 账号列表、新增账号、重置账号密码。

未覆盖或需要重新设计：

- 原项目的“登录卖家端 / 登录买家端”免密登录操作，当前没有后端接口，也没有页面入口。
- 原项目买家侧的“充值”入口，当前没有后端接口，也没有页面入口；且这属于财务/余额模块，不能直接做简单 CRUD。
- 原项目的余额展示和余额区间筛选，当前没有余额字段和查询条件。
- 原项目的创建时间、最后登录时间区间筛选，当前页面展示了时间，但搜索条件未开放。
- 原项目是一键把客户登录密码重置为默认密码；当前是对某个子账号输入新密码后重置，交互和语义不同。

## 原项目可确认的操作

| 功能 | 原项目证据 | 说明 |
|---|---|---|
| 列表 | `apps/admin-web/src/features/customers/components/CustomerListPage.tsx` | 卖家/买家共用客户列表组件。 |
| 新增 | `CustomerFormModal.tsx`、`admin-customers.ts` | 创建时填写用户名、客户等级、客户类型、客户代码、客户名称等资料。 |
| 编辑 | `CustomerFormModal.tsx`、`useCustomerManagement.ts` | 通过客户代码读取详情并更新资料。 |
| 启用/停用 | `useCustomerManagement.ts` | 调用更新客户状态。 |
| 重置密码 | `useCustomerManagement.ts`、`admin-customers.ts` | 重置为默认密码 `U123456`，返回 `defaultPassword`。 |
| 免密登录端 | `App.tsx`、`useCustomerManagement.ts`、`admin-customers.ts` | 卖家为“登录卖家端”，买家为“登录买家端”，调用 direct-login 并打开 `loginUrl`。 |
| 买家充值入口 | `App.tsx` | 买家 actionOptions 包含“充值”；未在扫描范围内发现完整财务充值实现。 |
| 余额展示/筛选 | `CustomerListPage.tsx`、`admin-customers.ts`、模块 domain | 列表有 `accountBalance`，筛选有余额范围。 |
| 创建/最后登录时间筛选 | `CustomerListPage.tsx`、`admin-customers.ts`、模块 domain | 支持 `createdFrom/createdTo`、`lastLoginFrom/lastLoginTo`。 |

未发现原项目客户管理已经实现删除、导入、导出；所以当前不把删除、导入、导出列为“漏做”。

## 当前若依实现对照

| 功能 | 当前状态 | 证据 | 结论 |
|---|---|---|---|
| 卖家/买家分模块 | 已做 | `RuoYi-Vue/seller`、`RuoYi-Vue/buyer` | 符合当前架构核心要求。 |
| 分表 | 已做 | `seller`、`buyer`、`seller_account`、`buyer_account` | 符合“买卖账号无关联”的方向。 |
| 列表分页 | 已做 | `AdminSellerController.list`、`AdminBuyerController.list` | 可继续沿用若依分页。 |
| 新增主体 | 已做 | `insertSeller`、`insertBuyer` | 会自动生成内部编号并创建主账号。 |
| 编辑主体 | 已做 | `updateSeller`、`updateBuyer` | 基础资料可编辑。 |
| 启停 | 已做 | `/changeStatus`、前端 Switch | 已按若依权限控制。 |
| 内部编号 | 已做 | `PartnerSupport.generateNo` | 卖家前缀 S，买家前缀 B。 |
| 客户代码必填 | 已做 | Domain 校验、Mapper 唯一查询 | 当前保存为 `seller_code` / `buyer_code`。 |
| 国家/地区下拉 | 已做 | `country_region` 字典、前端 `showSearch` | 有 fallback，但仍应以字典为准。 |
| 子账号管理 | 已做 | `/{id}/accounts`、账号弹窗 | 这是在原项目基础上按三端账号模型补强的能力。 |
| 账号重置密码 | 部分符合 | 当前对具体账号输入新密码 | 和原项目“一键重置为默认密码”不同。 |
| 免密登录端 | 未做 | 当前 controller/service/frontend 未发现 direct-login | 后续要等三端入口和 token 规则确认后设计。 |
| 买家充值 | 未做 | 当前 controller/service/frontend 未发现 recharge | 应归财务模块，不建议现在直接加字段。 |
| 余额展示/筛选 | 未做 | 当前没有 `accountBalance` 字段 | 需要先设计钱包/余额/流水归属。 |
| 创建/最后登录时间筛选 | 部分符合 | 当前表格展示时间，但 `search: false` | 可作为低风险前端/Mapper补充项。 |
| 删除 | 未做 | 原项目未发现实现 | 暂不建议补，主体停用比删除更符合审计。 |
| 导入/导出 | 未做 | 原项目未发现实现 | 可后续按若依能力单独规划。 |

## 当前缺口优先级

### P0：先确认运行库种子是否一致

当前源码和 SQL 种子使用的端账号角色 key 是：

- 卖家端账号角色：`seller`
- 买家端账号角色：`buyer`

创建卖家/买家主账号和子账号时，会按这两个 role key 绑定若依角色。如果远程运行库没有执行当前种子，新增账号可能会出现角色绑定失败或账号没有端权限的问题。

本次没有查询远程数据库；下一步如果要继续实现，应先按 AGENTS 规则读取当前激活配置，再只读确认远程库里的 `sys_role.role_key`、菜单权限、字典是否和当前 seed 一致。

### P1：补齐原项目已有、但不涉及财务表的操作

1. 账号重置密码语义统一：
   - 方案 A：保留当前“选账号、输入新密码”。
   - 方案 B：增加“重置为默认密码 U12346”的快捷操作。
   - 推荐：管理端保留当前账号级能力，再增加默认重置快捷入口。因为现在客户有主账号和子账号，原项目的“客户级重置密码”已经不足以表达目标账号。

2. 创建时间、最后登录时间区间筛选：
   - 前端 ProTable 开放 dateRange 搜索。
   - 后端 Seller/Buyer domain 增加查询参数或复用若依 `params`。
   - Mapper 增加 `create_time`、owner `login_date` 范围条件。
   - 风险低，适合作为下一步小任务。

3. 免密登录端：
   - 不应直接照搬旧项目 in-memory token。
   - 当前三端最终是三个前端项目，管理端应调用后端生成一次性登录 token，再打开卖家端/买家端 URL。
   - 需要先确认卖家端、买家端前端 URL、token 有效期、一次性消费、审计日志、是否允许停用账号登录。

### P2：涉及财务或业务事实表，必须先设计

1. 买家充值：
   - 不能直接在 buyer 表上加一个余额字段后修改。
   - 应进入财务模块，至少需要余额账户、充值流水、操作人、原因、币种、金额、审核/作废规则。
   - 当前只能保留为后续设计项。

2. 账户余额展示和余额筛选：
   - 原项目有 `accountBalance`，但当前若依还没有财务/余额事实表。
   - 等财务模块确认后，卖家/买家列表可以只读展示聚合余额，不在主体表里承载财务事实。

## 推荐下一步计划

1. 先做只读运行库核查：
   - 读取当前激活的 MySQL/Redis 配置。
   - 查询远程库是否已有 `seller`、`buyer`、`seller_account`、`buyer_account`。
   - 查询 `seller`、`buyer` 角色、菜单权限、`country_region`、`subject_type`、等级、账号角色字典是否齐全。
   - 输出 Markdown 执行记录，不做写入。

2. 修正低风险管理端缺口：
   - 增加创建时间、最后登录时间筛选。
   - 增加“重置为默认密码 U12346”快捷操作，仍保留账号级重置。
   - 检查默认密码文案和实际后端常量一致。

3. 单独写免密登录方案：
   - 明确三端 URL、token 生成/消费、权限点、审计日志。
   - 后端新增 `seller:admin:directLogin`、`buyer:admin:directLogin` 权限。
   - 管理端只负责生成链接并打开，不绕过后端权限。

4. 单独写买家充值/余额方案：
   - 按财务模块设计，不在买家表直接做余额 CRUD。
   - 确认币种、金额精度、流水只追加、冲销/作废、权限和日志。

## 验证与未验证

已验证：

- 使用 `rg` 和定向读取对照了原项目管理端、contracts、seller/buyer module。
- 使用 `rg` 和定向读取对照了当前若依 seller/buyer controller、service、mapper、React 页面和 service。
- 未发现当前实现包含 direct-login、recharge、accountBalance 相关接口或页面入口。

未验证：

- 未连接远程 MySQL/Redis；没有确认运行库是否已经执行最新 seed。
- 未运行 Maven 编译或前端构建；本次是只读功能对照，未改代码。
- 未做浏览器截图验证；本次目标是检查操作功能是否做齐。

