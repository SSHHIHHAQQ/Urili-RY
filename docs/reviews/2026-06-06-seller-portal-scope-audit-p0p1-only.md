# 2026-06-06 Seller Portal 端口范围控制审计（P0/P1�?
## 任务范围
- 只审�?`RuoYi-Vue/seller` �?seller 真实端入口（`SellerPortal*`）；
- 检查点�?
  1) 是否�?`PortalSession`（token）推�?`sellerId/accountId`，不信任前端 `sellerId`�?
  2) 是否使用 `@PortalPreAuthorize` �?`@PortalLog`�?
  3) 是否存在 `accountId/roleId/deptId` 裸写入且未绑�?`sellerId` �?P1 风险�?
- 不修改业务代码，仅审计，只输�?P0/P1 结论�?
## P0 结论
- P0�? �?
## P1 结论
- P1�? �?
## 证据（真实端范围�?
### 1) 真实端接口从会话推导主体，不信任前端 `sellerId`
- `SellerPortalController` 全量真实端接口直�?`PortalSessionContext.requireSession("seller")`，并全程�?`session.getSubjectId()/getAccountId()` 做查询入参�?
  - [SellerPortalController `getInfo`](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:62)
  - [SellerPortalController `getRouters`](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:76)
  - [SellerPortalController `accounts`](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:124)
- 商品相关真实端接口同样以 token 绑定�?session 调用服务层：
  - [SellerPortalProductDistributionController](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java:39)
  - [SellerPortalProductDistributionController.detail](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java:51)
  - [SellerPortalProductDistributionController.skus](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java:62)
- 下沉到服务层后也保持 session 主体约束�?
  - `selectOwnProductList` 通过 `buildOwnProductQuery` 将查�?`sellerId` 固定�?`session.getSubjectId()`：[SellerPortalProductServiceImpl](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:54)
  - `selectOwnProductById` / `selectOwnSkuList` 强制 `requireOwnProduct(session, spuId)`，并校验 `product.getSellerId == session.getSubjectId()`：[SellerPortalProductServiceImpl](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:44)
- 登录日志/操作日志/会话列表的查询也�?session 主体拼接查询条件�?
  - [SellerServiceImpl selectSellerOwnLoginLogList](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:339)
  - [SellerServiceImpl buildSellerOwnLoginLogQuery](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:375)
  - [SellerServiceImpl selectSellerOwnOperLogList](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:348)

### 2) `@PortalPreAuthorize` / `@PortalLog` 覆盖情况（真实端�?- `SellerPortal*` 真实端业务接口全部带 `@PortalPreAuthorize` 且带 `@PortalLog`（除登录类入口）�?  - [SellerPortalController 全量示例](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:62)
  - [SellerPortalProductDistributionController](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java:34)
  - [SellerPortalProductSchemaController](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductSchemaController.java:29)
- 登录入口（`/seller/login`、`/seller/direct-login`）位�?`SellerPortalAuthController`，不应被要求强制 `@PortalPreAuthorize`；其行为由登录服务侧鉴权承担�?
  - [SellerPortalAuthController.login](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalAuthController.java:30)
  - [SellerPortalAuthController.directLogin](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalAuthController.java:37)

### 3) �?`accountId/roleId/deptId` 写入未绑�?`sellerId` 风险（P1 口径�?- 账号类写入在 service 层均先做 seller 与账号归属校验（`selectSellerAccountById(sellerId, accountId)`），再执行写库：
  - [SellerServiceImpl.selectSellerAccountById](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:134)
  - [SellerServiceImpl.insertSellerAccount](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:150)
  - [SellerServiceImpl.updateSellerAccount](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:168)
- 权限/角色绑定路径也做�?`sellerId` + `accountId` 双向校验后再写入（`account_role`、`role_menu`）：
  - [SellerPortalPermissionServiceImpl.assignAccountRoles](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:225)
  - [SellerPortalPermissionServiceImpl.batchSellerAccountRoles](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:244)
  - [SellerPortalPermissionMapper.xml deleteSellerAccountRoles](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml:312)
- 部门变更/查询同样�?`sellerId` 作为硬约束参数进�?mapper�?
  - [SellerPortalDeptServiceImpl.insertDept/updateDept/deleteDeptById](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalDeptServiceImpl.java:77)
  - [SellerPortalDeptServiceImpl query/update](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalDeptServiceImpl.java:40)
  - [SellerPortalDeptMapper.xml where seller_id filters](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalDeptMapper.xml:39)

## 最小修复建议（非阻断）
1. 登录入口可按一致性要求补齐审计注解：`SellerPortalAuthController` �?`/login` �?`/direct-login` 增加 `@PortalLog`（`@PortalPreAuthorize` 维持不加）�?
2. 审计报告口径固定：将该审计结论归档到 `docs/reviews/`，在后续改造时�?`AdminSeller*` �?`SellerPortal*` 明确为不同安全域，避免误把管理端入口套用同一审计规则�?
