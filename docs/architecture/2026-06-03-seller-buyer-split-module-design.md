# 卖家/买家模块分表规则

日期：2026-06-03

## 状态

本文件记录已调整后的规则。根据 `AGENTS.md`，新增业务表前仍需你确认；确认前不落 SQL、Entity、Mapper、Service、Controller、菜单权限或前端页面。

## 核心决定

- 不再做一个客户表。
- 不再用 `customer_kind` 区分卖家和买家。
- 数据库业务表名不带 `urili_` 前缀，直接按业务对象命名。
- 代码命名空间也不带 `Urili` / `urili` 前缀，Java、API、权限、前端目录都按业务对象命名。
- 卖家是一个独立 module，买家是一个独立 module。
- 若依账号、角色、菜单、权限、字典、日志继续复用，不另起第二套账号体系。

## 表命名

采用四张核心表：

```text
seller
buyer
seller_account
buyer_account
```

旧草稿中的表需要废弃或迁移：

```text
urili_customer                  -> seller / buyer
urili_customer_account          -> seller_account / buyer_account
customer_kind = seller/buyer    -> 不再使用
```

## 主体规则

- `seller` 只保存卖家主体资料。
- `buyer` 只保存买家主体资料。
- 同一家公司如果既是卖家又是买家，也必须分别创建一条 `seller` 和一条 `buyer`。
- 卖家和买家默认不自动关联；以后如果要识别同一现实主体，需要单独设计关联表。
- 主体表只保存基础资料和管理状态，不保存订单、库存、财务流水、外部请求日志。

## 账号规则

- 登录账号仍保存到若依 `sys_user`。
- `seller_account` 绑定 `seller.seller_id` 和 `sys_user.user_id`。
- `buyer_account` 绑定 `buyer.buyer_id` 和 `sys_user.user_id`。
- 卖家账号和买家账号必须是不同的 `sys_user`。
- 同一个 `sys_user.user_id` 不能同时出现在 `seller_account` 和 `buyer_account`。
- 密码只在 `sys_user.password`，绑定表不保存密码。

## 字段规则

卖家字段使用 `seller_` 前缀：

```text
seller_id
seller_no
seller_code
seller_name
seller_short_name
seller_type
seller_level
```

买家字段使用 `buyer_` 前缀：

```text
buyer_id
buyer_no
buyer_code
buyer_name
buyer_short_name
buyer_type
buyer_level
```

公共资料字段两边保持一致：

```text
status
legal_id
business_license_no
country_code
state_province
city
postal_code
address_line1
address_line2
contact_name
contact_phone
contact_email
attachment_file_name
attachment_file_url
create_by
create_time
update_by
update_time
remark
```

## code/label 规则

- 数据库和 API 保存 code。
- 前端展示 label。
- 新业务字典类型也尽量不带 `urili_` 前缀：
  - `subject_type`：`COMPANY` / `PERSON` / `OTHER`
  - `seller_level`：`L1` / `L2` / `L3`
  - `buyer_level`：`L1` / `L2` / `L3`
  - `seller_account_role`：`OWNER` / `ADMIN` / `STAFF`
  - `buyer_account_role`：`OWNER` / `ADMIN` / `STAFF`
- 国家/地区使用 `country_region`，不再新增 `urili_country_region` 依赖。

## 后端模块规则

卖家和买家必须拆成 `RuoYi-Vue` 父工程下的物理 Maven 子模块：

```text
RuoYi-Vue/seller
RuoYi-Vue/buyer
```

`seller` module 内放：

```text
com.ruoyi.seller.controller.AdminSellerController
com.ruoyi.seller.domain.Seller
com.ruoyi.seller.domain.SellerAccount
com.ruoyi.seller.mapper.SellerMapper
com.ruoyi.seller.service.ISellerService
com.ruoyi.seller.service.impl.SellerServiceImpl
mapper/seller/SellerMapper.xml
```

`buyer` module 内放：

```text
com.ruoyi.buyer.controller.AdminBuyerController
com.ruoyi.buyer.domain.Buyer
com.ruoyi.buyer.domain.BuyerAccount
com.ruoyi.buyer.mapper.BuyerMapper
com.ruoyi.buyer.service.IBuyerService
com.ruoyi.buyer.service.impl.BuyerServiceImpl
mapper/buyer/BuyerMapper.xml
```

公共支撑保留在 `ruoyi-system`：

```text
com.ruoyi.system.domain.PartnerProfile
com.ruoyi.system.domain.PortalAccount
com.ruoyi.system.mapper.PortalAccountMapper
com.ruoyi.system.service.support.PartnerSupport
com.ruoyi.system.service.support.PortalAccountSupport
```

说明：

- Maven module、Java 类名、Controller 包名、Mapper、Service、API 路径、权限 key、前端目录都不默认带 `Urili` / `urili`。
- 表名不带 `urili_`。
- Controller 包按业务对象拆分，例如 `com.ruoyi.seller.controller` 和 `com.ruoyi.buyer.controller`。
- Controller 不直接调用 Mapper。
- 买家 Service 不直接写卖家表，卖家 Service 不直接写买家表。
- 公共逻辑可以抽内部 support，例如账号创建、密码重置、编号生成、字段校验，但不能重新包装成“客户模块”，也不能让 `seller` 与 `buyer` 两个 module 互相依赖。

## 前端模块规则

当前阶段仍只在 `react-ui/` 做管理端验证，不新增三套前端工程。

建议目录：

```text
react-ui/src/pages/Seller/
react-ui/src/pages/Buyer/
react-ui/src/services/seller/seller.ts
react-ui/src/services/buyer/buyer.ts
react-ui/src/types/seller-buyer/seller.d.ts
react-ui/src/types/seller-buyer/buyer.d.ts
react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx
```

规则：

- 不再使用 `CustomerManagementPage(kind)` 作为主入口。
- 卖家页面和买家页面分开。
- 可复用字典 option、国家/地区选择器、附件控件、表单校验 helper。
- 不在页面内硬编码大段状态、等级、类型映射。

## 权限规则

管理端权限仍按端区分：

```text
seller:admin:list
seller:admin:query
seller:admin:add
seller:admin:edit
seller:admin:changeStatus
seller:admin:resetPwd

buyer:admin:list
buyer:admin:query
buyer:admin:add
buyer:admin:edit
buyer:admin:changeStatus
buyer:admin:resetPwd
```

权限 key 不使用 `urili:` 前缀；菜单、后端 `@PreAuthorize` 和前端 `access.hasPerms(...)` 必须保持一致。

## API 路径规则

管理端接口按业务模块命名：

```text
/seller/admin/sellers
/buyer/admin/buyers
```

前端请求经代理使用：

```text
/api/seller/admin/sellers
/api/buyer/admin/buyers
```

## 迁移规则

如果旧单表 SQL 没执行：

1. 废弃旧单表 SQL 草稿。
2. 新增分表 SQL，创建 `seller`、`buyer`、`seller_account`、`buyer_account`。
3. 按卖家/买家独立模块实现后端和前端。

如果旧单表 SQL 已执行：

1. 新建四张新表。
2. 把 `urili_customer.customer_kind = 'seller'` 迁移到 `seller`。
3. 把 `urili_customer.customer_kind = 'buyer'` 迁移到 `buyer`。
4. 把 `urili_customer_account.portal_type = 'seller'` 迁移到 `seller_account`。
5. 把 `urili_customer_account.portal_type = 'buyer'` 迁移到 `buyer_account`。
6. 校验迁移前后数量、编号唯一性、账号绑定唯一性。
7. 旧表改名为 legacy 表保留，不立即删除。

## 验证规则

实施后至少验证：

- 新增卖家只写 `seller` 和 `seller_account`。
- 新增买家只写 `buyer` 和 `buyer_account`。
- 同一 `seller_code` 不能重复。
- 同一 `buyer_code` 不能重复。
- 同一个 `sys_user` 不能同时绑定卖家和买家。
- 有权限用户可访问卖家/买家管理；无权限用户被后端拒绝。
- 前端按钮隐藏不能替代后端 `@PreAuthorize`。
- 字典展示 label，接口和数据库保存 code。
- 新增、修改、启停、重置密码写操作日志。

## 本轮记录

- 已调整：业务表名改为 `seller`、`buyer`、`seller_account`、`buyer_account`。
- 已调整：代码命名空间去掉 `Urili` / `urili`，改为 `Seller` / `Buyer`、`seller:admin:*` / `buyer:admin:*`、`/seller/...` / `/buyer/...`。
- 已精炼：删除长字段展开说明，只保留规则和必须确认的边界。
- 已实施：SQL、Java 后端模块、React 页面/service/types 已按卖家/买家分模块落地。
- 已确认并实施：国家/地区字典从旧草稿 `urili_country_region` 改为 `country_region`。
