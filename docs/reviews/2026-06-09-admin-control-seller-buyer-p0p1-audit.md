# 2026-06-09 管理端控制权接口与权限只读审计

## 审计目标

只读检查 `E:\Urili-Ruoyi` 当前工作区内三端独立方向下的管理端控制权接口与权限契约，范围限定为：

- seller / buyer 管理端 controller
- `@PreAuthorize`
- `sys_menu` perms seed
- 免密代入
- 强制踢出
- 会话列表
- 账号角色 / 部门 / 菜单维护

只找 P0 / P1：

- 权限缺失
- 权限入口与实际接口不一致
- admin / seller / buyer 串端
- 接口缺字段或 service 缺失

本次不改业务代码，不执行 SQL。

## 结论

本次覆盖范围内，**未发现可坐实的 P0 / P1 问题**。

当前 seller / buyer 管理端控制权链路在以下四个方面保持一致：

1. 后端管理端 controller 的 `@PreAuthorize` 与 React 管理端实际入口匹配。
2. controller 使用的 seller / buyer admin perms 在 `sys_menu` seed 中可找到对应条目。
3. 账号、会话、日志、免密代入、账号角色分配均把 `sellerId/buyerId + accountId/roleId` 作用域下推到 service / mapper。
4. seller / buyer 端菜单、角色分配有终端范围校验，未见 admin / 对端串端写入入口。

## 证据

### 1. seller / buyer 管理端 controller 权限完整

`AdminSellerController` 与 `AdminBuyerController` 已覆盖：

- 主体列表 / 查询 / 新增 / 编辑 / 状态变更
- 账号列表 / 新增 / 编辑 / 锁定 / 解锁 / 重置密码
- 账号角色查询 / 分配
- 会话列表 / 强制踢出
- 免密代入
- 登录日志 / 操作日志 / 免密票据列表

证据：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:43-228`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:43-228`

seller / buyer 的部门、菜单、角色管理 controller 也都带了独立的 admin perms：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerDeptController.java:31-73`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerMenuController.java:32-84`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerRoleController.java:33-85`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerDeptController.java:31-73`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerMenuController.java:31-83`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerRoleController.java:33-85`

### 2. 前端入口与后端实际接口一致

React 管理端 seller / buyer 页面把服务方法全部接到 `PartnerManagementPage`：

- `react-ui/src/pages/Seller/index.tsx:63-113`
- `react-ui/src/pages/Buyer/index.tsx:64-114`

服务路径与后端 controller 路径一致：

- seller: `react-ui/src/services/seller/seller.ts:49-335`
- buyer: `react-ui/src/services/buyer/buyer.ts:49-335`

关键入口与权限对齐情况：

- 账号角色分配按钮同时要求 `*:role:query + *:account:role:query + *:account:role:edit`
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:253-256`
  - 对应后端：
    - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:90-106`
    - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:90-106`
- 主体级“更多”菜单中，部门 / 角色 / 会话 / 强制踢出 / 免密代入都按对应 admin perm 显隐
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:862-944`
- 账号级“更多”菜单中，会话 / 强制踢出 / 免密代入按对应 admin perm 显隐
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:643-709`
- 部门 / 角色 / 菜单弹窗内部的新增、编辑按钮，还额外补了对 `query` 的依赖，避免只放出入口却调用查询接口 403
  - 部门：`react-ui/src/components/PartnerManagement/PartnerDeptModal.tsx:112-115`
  - 角色：`react-ui/src/components/PartnerManagement/PartnerRoleModal.tsx:124-128`
  - 菜单：`react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx:272-275`

本次未发现“按钮只看 A 权限，但实际会调用 B 接口”的 P1 不一致。

### 3. `sys_menu` seed 覆盖了当前 admin perms

`seller_buyer_management_seed.sql` 中可见 seller / buyer 管理端页面 perms 与按钮 perms：

- 顶级页面：`seller:admin:list` / `buyer:admin:list`
- 免密代入：`*:admin:directLogin`
- 强制踢出：`*:admin:forceLogout`
- 会话列表：`*:admin:session:list`
- 菜单管理：`*:admin:menu:*`
- 角色管理：`*:admin:role:*`
- 部门管理：`*:admin:dept:*`
- 登录日志 / 操作日志 / 票据列表
- 账号列表 / 新增 / 编辑 / 锁定 / 重置密码 / 账号角色查询 / 分配

证据：

- `RuoYi-Vue/sql/seller_buyer_management_seed.sql:1498-1720`

静态对比结果中，**未发现 controller 使用了 seed 中不存在的 seller/buyer admin perm**。

### 4. service / mapper 作用域下推到 sellerId / buyerId，未见裸 accountId 串端入口

账号、会话、强制踢出、免密代入都使用 `sellerId/buyerId + accountId` 约束：

- seller
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:145-148`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:152-160`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:251-314`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:926-948`
- buyer
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:145-148`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:152-160`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:251-314`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:926-948`

账号角色分配也先校验“账号属于当前主体”，再校验“角色属于当前主体”：

- seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:231-253,399-407`
- buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:231-253,399-407`

日志 / 审计查询对 `accountId` 场景要求必须同时提供主体 ID，否则直接 fail-closed：

- seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:476-514`
- buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:476-514`

### 5. seller / buyer 端菜单与角色绑定存在终端边界校验

角色绑定菜单前会校验：

- menu id 在当前端存在
- menu id 命中当前端 ID 区间
- page menu component 使用当前端根路径
- perms 使用当前端前缀，且禁止 admin 命名空间

证据：

- seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:305-323`
- buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:305-323`

因此在本次审计范围内，未见 seller/buyer 菜单维护接口把对端菜单或 admin 命名空间静默写入角色的 P1 风险。

## 本次未命中的范围

以下内容不在本次“只读 + 不执行 SQL”范围内，因此未给 live 结论：

- 远端 `sys_menu` 实际落库结果
- `/getRouters` 与 `sessionStorage.admin_remote_menu` 的 live 联动
- 真实浏览器里 seller / buyer portal 对免密代入回执消息的运行态验证
- 数据库中是否已有历史脏数据导致页面 403 / 空列表 / 错配

## 审计方法

- 只读查看 controller / service / React 页面 / service / SQL seed
- 做了静态权限名对比与调用链核对
- 未修改业务代码
- 未执行 SQL
- 未做 live API / 浏览器验证
