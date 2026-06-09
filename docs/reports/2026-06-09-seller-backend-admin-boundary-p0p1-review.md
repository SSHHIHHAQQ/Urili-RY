# 2026-06-09 seller 后端管理端控制接口只读 P0/P1 审查

## 审查范围

- 仓库：`E:\Urili-Ruoyi`
- 模块：`RuoYi-Vue/seller`
- 模式：只读审查，不改业务代码
- 参考：
  - `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
  - `AGENTS.md`
- 本次聚焦：
  - seller 管理端控制接口
  - 端内账号 / 角色 / 菜单 / 部门 / 日志 / 会话边界
  - P0/P1 风险：编译、guard、接口、权限、串端、service/字段缺失
  - 重点排查：`sys_*` 混用、裸 `selectAccountById`、`session:list` / `forceLogout` 权限混绑、`resetPwd` 默认密码、`direct-login` 审计串端、`@PreAuthorize` / `@PortalPreAuthorize` 错用

## 结论

本轮 **未发现 seller 后端管理端控制接口范围内的 P0/P1 问题**。

结论基于：

- 管理端 controller / service / mapper / XML 只读检查
- seller 端 portal controller 注解边界核对
- seller 端日志 / 会话 / direct-login 审计链路核对
- 针对性合同测试与服务测试执行通过

## 已核对要点

### 1. `sys_*` 混用

未发现 seller 端账号、角色、菜单、部门、日志、会话链路继续读写 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 作为端内控制面。

证据：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalDeptServiceImpl.java`
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml`
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalDeptMapper.xml`

### 2. 裸 `selectAccountById`

未发现 seller 生产代码保留裸 `selectSellerAccountById(accountId)` 入口；当前实现统一走 seller 作用域约束。

关键证据：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/mapper/SellerMapper.java:46-47`
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:342-351`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:152-160`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:399-407`

### 3. `session:list` / `forceLogout` 权限混绑

未发现 seller 管理端把只读会话列表继续绑到强退权限。

关键证据：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:151-169`
  - 会话列表使用 `seller:admin:session:list`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:171-185`
  - 强制踢出使用 `seller:admin:forceLogout`
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java:74-77`

### 4. `resetPwd` 默认密码回退

未发现 seller 管理端 `resetPwd` 静默回退到默认密码 `U12346`。

关键证据：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:142-149`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:240-247`
- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PartnerSupport.java:226-238`
  - `normalizeTemporaryPassword(...)` 空值直接失败，不兜底默认密码
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java:101-111`

说明：

- `PartnerSupport.DEFAULT_OWNER_PASSWORD = "U12346"` 仍存在，但仅用于新建主体时的 owner 初始化，不是管理端 `resetPwd` 默认回退路径。

### 5. direct-login 审计串端

未发现 seller 端 direct-login 成功、失败、强退审计把外端票据字段错误写入 seller 审计链路。

关键证据：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:684-707`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:743-752`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:966-1008`
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:431-447`
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:489-494`
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:536-547`

说明：

- `recordSellerDirectLoginTokenFailure(...)` 在 terminal 不匹配时走普通失败登录日志，不携带外端 ticket 上下文。
- direct-login 成功、失败、强退日志均写 seller 侧结构化字段：`direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`。

### 6. `@PreAuthorize` / `@PortalPreAuthorize` 错用

未发现管理端 controller 误用 `@PortalPreAuthorize`，也未发现 seller portal controller 漏掉 `@PortalPreAuthorize` / 错回 admin `@PreAuthorize`。

关键证据：

- 管理端：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerDeptController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerRoleController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerMenuController.java`
- portal：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:58-193`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalAuthController.java:25-39`
- 合同测试：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalAnonymousEndpointContractTest.java`
  - `RuoYi-Vue/ruoyi-framework/src/test/java/com/ruoyi/framework/aspectj/PortalPreAuthorizeAspectTest.java`

## 测试验证

执行命令：

```powershell
mvn -pl seller,ruoyi-system -am "-Dtest=SellerAdminPermissionContractTest,TerminalAccountIsolationTest,PortalAdminAuditBindingContractTest,SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：

- `SellerAdminPermissionContractTest` 通过
- `TerminalAccountIsolationTest` 通过
- `PortalAdminAuditBindingContractTest` 通过
- `SellerServiceImplTest` 通过
- 总结：`Tests run: 65, Failures: 0, Errors: 0, Skipped: 0`

## 结论建议

当前 seller 后端管理端控制接口在本次审查范围内，未看到需要立刻阻断推进的 P0/P1 风险。后续如果继续做切片 B/C，建议沿相同方法继续盯：

- buyer 对称实现是否完全同步 seller 的边界合同
- direct-login ticket SQL 与 Redis key 合同是否仍与最新 AGENTS 约束一致
- 管理端前端入口是否真的把 `session:list` 与 `forceLogout` 分开消费，而不是后端正确、前端又绑回去
