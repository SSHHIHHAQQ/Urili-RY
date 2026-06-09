# 2026-06-09 seller/buyer 后端只读 P0/P1 审计（切片 1）

## 范围

- 目标文档：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 审计对象：
  - `RuoYi-Vue/seller`
  - `RuoYi-Vue/buyer`
- 只看 P0/P1：
  - `sys_*` 混用
  - 裸 `accountId` 查询
  - 跨端 `role/menu` 写入
  - 缺少 SQL 层 `subject + account` 约束
  - 后端权限标识缺失
- 只读，不改业务代码

## 结论

本切片 **未发现明确的 P0/P1 问题**。

当前 `seller` / `buyer` 后端账号、角色、菜单、部门主链路已经基本按三端隔离方案落到独立表，并且关键查询/写入路径都带了主体约束或端范围约束，未看到继续直接混用 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 作为卖家端、买家端端内控制面的实现。

## 本轮未发现 P0/P1 的依据

### 1. 未发现 `sys_*` 直接混用

- 对 `seller` / `buyer` 的 Controller、Service、Mapper、XML 做了 `sys_user|sys_role|sys_menu|sys_dept` 检索，未命中端内账号/角色/菜单/部门链路代码。
- 角色、菜单、部门均走各自独立表：
  - `seller_role` / `seller_menu` / `seller_dept`
  - `buyer_role` / `buyer_menu` / `buyer_dept`

### 2. 未发现裸 `accountId` 查询入口

- 卖家账号主查询统一走 `selectSellerAccountByIdAndSellerId(...)`，服务入口也要求传入 `sellerId + accountId`：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:152`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:342`
- 买家账号主查询统一走 `selectBuyerAccountByIdAndBuyerId(...)`，服务入口也要求传入 `buyerId + accountId`：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:152`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:283`

### 3. 账号角色写入已带主体约束

- 卖家端账号角色分配先校验账号归属，再校验角色是否属于当前卖家，然后按 `sellerId + accountId` 清理/重绑：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:231`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:239`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml:248`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml:336`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml:344`
- 买家端对称实现：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:231`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:239`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:248`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:336`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:344`

### 4. 菜单授权已做端范围 guard，未见跨端 role/menu 写入

- 卖家端菜单写入前会校验端范围和菜单合法性；批量写 `seller_role_menu` 时只接受 `100000-199999` 的 `seller_menu_id`：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:305`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml:324`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml:366`
- 买家端对称限制在 `200000-299999`：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:305`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:324`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:366`

### 5. 日志/会话查询已补主体校验，未见只按 `accountId` 裸查

- 卖家管理端登录日志/操作日志查询前，若带 `accountId` 但没带 `sellerId` 会直接拒绝；若两者都带，会先校验账号属于该卖家：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:317`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:496`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:443`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:495`
- 卖家会话查询/强退也都走 `sellerId`，账号级再追加 `seller_account_id`：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:251`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:550`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:563`
- 买家端对称实现：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:317`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:496`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:385`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:431`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:495`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:534`

### 6. 管理端与 portal 端 Controller 未见明显漏鉴权

- 管理端账号/角色/菜单/部门 Controller 均加了 `@PreAuthorize(...)`，账号角色回显也同时要求端角色查询权限和账号角色查询权限：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:91`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:91`
- 管理端会话列表与强退权限已分开：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:152`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:172`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:152`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:172`
- portal 端账号/部门/角色/日志/会话接口均走 `@PortalPreAuthorize(...)`：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:120`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:165`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:187`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:120`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:165`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:187`

## P0/P1 列表

无。

## 建议的最小后续动作

虽然本切片未发现明确 P0/P1，但建议继续用测试把这些约束钉死，避免后续回归：

1. 对 `seller` / `buyer` 的日志查询继续保留或补强合同测试，固定“带 `accountId` 必须同时带 `subjectId`”。
2. 对 `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 继续保留菜单 ID 区间和账号角色归属测试，固定“跨端 menuId / 跨主体 roleId fail-closed”。
3. 后续切片继续审计登录、直登消费、ticket、token/Redis 相关链路，确认没有跨端票据污染和串端审计写入。

## 验证方式

- 只读静态审计
- `rg` 检索 Controller / Service / Mapper / XML
- 人工复核关键路径：
  - 账号查询
  - 账号角色分配
  - 角色菜单绑定
  - 登录日志 / 操作日志 / 会话查询
  - 管理端 / portal 端权限注解
