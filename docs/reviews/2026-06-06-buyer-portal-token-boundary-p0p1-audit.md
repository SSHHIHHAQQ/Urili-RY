# Buyer Portal 真实端接口：token/范围边界审计（P0/P1）

## 审计范围（只读）
- 模块：`RuoYi-Vue/buyer`
- 目标：`buyer` 真实端（非 `/buyer/admin`）控制器/服务/Mapper
- 检查点：
  1) 是否从 `PortalSession/Token` 推导 `buyerId/accountId` 而非直接信任前端传参
  2) 是否使用 `@PortalPreAuthorize` 与 `@PortalLog`
  3) 是否存在 `accountId / roleId / deptId` 裸写入（未验证 `buyerId` 绑定）的 P1 风险

## 结论（只给 P0/P1）
- P0：无
- P1：无

## 证据（可复核路径）

### 1) `/buyer` 真实端 controller 已统一按 session 上下文取 subject/account
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`
  - `getInfo`、`getRouters`、`logout`、`profile`、`accountProfile`、`accounts`、`depts`、`roles`、`accountLoginLogs`、`accountOperLogs`、`accountSessions` 等方法全部 `PortalSessionContext.requireSession("buyer")` 后，再按 `session.getSubjectId()` / `session.getAccountId()` 查询（示例：61-67、96-98、107、128-129、135-143、158-160、171-173、183-185、193-197）
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductDistributionController.java`
  - `list/get/skus` 使用 `PortalSessionContext.requireSession("buyer")` 取 session 后调用服务，未从请求参数承载 buyerId/accountId（34-36、39-42、49-53、62-64）
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductSchemaController.java`
  - `categories`/`schema` 均先 `PortalSessionContext.requireSession("buyer")` 再执行业务（27-36、40-46）
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalAuthController.java`
  - `/buyer/login` 与 `/buyer/direct-login` 为登录链路，不应依赖现有 token；属于登录入口，不在“信任 token 推导 buyerId/accountId”的问题集合内（27-44）

### 2) 真实端服务层继续沿 token scope 校验，不接收前端 buyerId/accountId 作为越权入口
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java`
  - 所有入口方法都要求 `PortalLoginSession`，并在 `assertBuyerSession(session)` 中校验 `terminal/accountId/subjectId/token`（33-37、46-56、108-115）
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java`
  - `selectRoleList/RoleById/insertRole/updateRole...` 通过 `buyerService.selectBuyerById(buyerId)` 强绑定主体后执行（137-142、147-153、167-173、178-182、190-197）
  - `assignAccountRoles / selectAccountRoleIds / selectMenuIdsByRoleId` 先做 `selectRoleById` 或 `assertBuyerAccount`，再进行 mapper 查询（69-73、223-227、233-245）
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalDeptServiceImpl.java`
  - 所有部门操作方法先 `buyerService.selectBuyerById(buyerId)`，再 `setSubjectId(buyerId)`/`selectBuyerDeptById(buyerId, deptId)` 做作用域限定（30-36、39-43、77-88、92-99、113-125）
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `selectBuyerAccountById(buyerId, buyerAccountId)` 内部再次校验 `account.buyerId == buyerId`（134-143）
  - 管理端写入前（如 `forceLogoutBuyerAccountSessions`、`createBuyerAccountDirectLogin`）读取 `account` 后与 `buyerId` 对比（286-291、312-314）
  - 会话方法 `assertBuyerSessionAccount` 再次通过 `selectBuyerAccountById(session.getSubjectId(), session.getAccountId())` 验证会话绑定（365-373）

### 3) `@PortalPreAuthorize` 与 `@PortalLog` 覆盖情况
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`
  - 各业务接口几乎全部带 `@PortalPreAuthorize` 与 `@PortalLog`（58-201 大段，含所有端内信息/日志/会话接口）
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductDistributionController.java`
  - `@PortalPreAuthorize` + `@PortalLog` 已覆盖（34-63）
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductSchemaController.java`
  - `@PortalPreAuthorize` + `@PortalLog` 已覆盖（27-47）
- 说明：`BuyerPortalAuthController` 登录链路无 `PortalPreAuthorize/@PortalLog` 是登录接口特征，当前审计范围内未算 P0/P1 授权缺陷，但可作为一致性补强项（见下）。

## P1 风险判断（本轮）
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerPortalPermissionMapper.java` 中确实存在 mapper 方法定义未带 `buyerId` 参数（`selectBuyerMenuIdsByRoleId(roleId)`、`countBuyerAccountRoleByRoleId(roleId)`），对应 SQL 亦无 buyer 过滤（`...BuyerPortalPermissionMapper.xml` 29-35、234-236）。
- 该类方法未直接暴露给 controller，统一由 `BuyerPortalPermissionServiceImpl` 包装并先做主体/角色绑定校验（69-73、212-217、231-240），因此目前未形成可直接越权写入/读写的 P1。
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerMapper.java#selectBuyerAccountById(accountId)` 也是单参数查询，但所有写入/敏感读路径均在业务服务层做 `buyerId` 归属比对（`BuyerServiceImpl` 286-291、312-314、312-314 等）。

## 建议最小修复（非阻断）
1) 补齐认证链路可观测性
   - 在 `BuyerPortalAuthController` 的登录/免登接口补充 `@PortalLog`（如 `terminal="buyer", title="买家端登录/免密登录"`），便于审计闭环。
2) 防御性收紧 Mapper 边界
   - 建议为 `BuyerPortalPermissionMapper` 的 `selectBuyerMenuIdsByRoleId` / `countBuyerAccountRoleByRoleId` 增加 `buyerId` 入参并在 SQL 增加 `buyer_id` 过滤（服务层先校验后透传），降低未来误用风险。
3) 固化回归用例
   - 增加一条最小测试：尝试用不匹配的 `buyerId/accountId/roleId` 调用管理入口（如 `createBuyerAccountDirectLogin`, `assignAccountRoles`, `selectPortalPermissionInfo`），断言抛异常，防止未来因重构回退。

## 备注
- 本次为只读审计，不做代码改动。
