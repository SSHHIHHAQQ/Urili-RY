# 三端只读复核切片 A：管理端 seller/buyer 控制面后端 P0/P1 审查

日期：2026-06-09

范围：`RuoYi-Vue` 管理端 seller/buyer 控制面后端，只读复核账号、角色、部门、菜单、会话、登录日志/操作日志、免密票据、强制踢出、重置密码相关 controller/service/contract/test。

结论：**本次切片未发现当前开放的 P0/P1 缺口。**

## 1. 是否发现当前开放 P0/P1

- 结论：**否。**
- 依据：
  - 管理端 seller/buyer 控制器权限、路由、敏感读日志约束已被源码和合同测试双重固定。
  - account 查询、日志筛选、票据筛选、会话/强退、密码重置均持续使用 `sellerId/buyerId + accountId` 约束，没有发现裸 `accountId` 入口。
  - seller/buyer 的登录日志、操作日志、session、direct-login 审计字段和当前 admin actor 绑定规则已被 SQL/合同测试和 service 单测覆盖。
  - 最小后端验证已通过，未出现编译、测试或合同回归失败。

## 2. 文件路径和证据

### 2.1 管理端 controller 权限与路由

- Seller 管理端账号/会话/强退/免密/日志/票据接口权限齐全：
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:91)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:101)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:143)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:152)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:172)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:227)
- Buyer 管理端对称实现一致：
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:91)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:101)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:143)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:152)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:172)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:227)

### 2.2 service 层 subject/account 约束没有回退

- Seller account 查询、重置密码、subject/account 会话强退、日志范围归一化、票据范围归一化，全部继续走带主体约束的实现：
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:155)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:240)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:319)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:326)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:587)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:926)
- Buyer 对称实现一致：
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:155)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:240)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:319)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:326)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:587)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:926)

### 2.3 Mapper / SQL 审计字段和会话范围

- Seller 登录日志、操作日志、session 查询/强退均落了 `direct_login_*` / `acting_admin_*` 字段，并按 `seller_id`、可选 `seller_account_id` 限定：
  - [RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:443)
  - [RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:489)
  - [RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:550)
  - [RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:589)
  - [RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml](/E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:625)
- Buyer 对称实现一致：
  - [RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:384)
  - [RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:430)
  - [RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:491)
  - [RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:530)
  - [RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml](/E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:566)

### 2.4 合同测试已经锁定关键风险点

- Seller 管理端控制面合同：
  - [RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java:50)
  - [RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java:136)
  - [RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java:155)
- Buyer 管理端控制面合同：
  - [RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java:50)
  - [RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java:136)
  - [RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java:155)
- 精确权限、会话审计、SQL 审计字段合同：
  - [RuoYi-Vue/ruoyi-framework/src/test/java/com/ruoyi/framework/web/service/PermissionServiceAccountPermissionTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-framework/src/test/java/com/ruoyi/framework/web/service/PermissionServiceAccountPermissionTest.java:41)
  - [RuoYi-Vue/ruoyi-framework/src/test/java/com/ruoyi/framework/web/service/PermissionServiceAccountPermissionTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-framework/src/test/java/com/ruoyi/framework/web/service/PermissionServiceAccountPermissionTest.java:64)
  - [RuoYi-Vue/ruoyi-framework/src/test/java/com/ruoyi/framework/web/service/PermissionServiceAccountPermissionTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-framework/src/test/java/com/ruoyi/framework/web/service/PermissionServiceAccountPermissionTest.java:87)
  - [RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalLoginSessionConsistencyContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalLoginSessionConsistencyContractTest.java:36)
  - [RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSqlIsolationContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSqlIsolationContractTest.java:17)

### 2.5 本次最小实跑验证

- 已执行并通过：

```powershell
mvn -pl seller,buyer,ruoyi-system,ruoyi-framework -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest,PermissionServiceAccountPermissionTest,PortalLoginSessionConsistencyContractTest,TerminalSqlIsolationContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- 结果摘要：
  - `ruoyi-system`: 23 tests, 0 failures, 0 errors
  - `ruoyi-framework`: 7 tests, 0 failures, 0 errors
  - `seller`: 55 tests, 0 failures, 0 errors
  - `buyer`: 55 tests, 0 failures, 0 errors
  - Reactor `BUILD SUCCESS`

## 3. 建议的最小修复

- **当前无需 P0/P1 修复。**
- 维持建议：
  - 后续若继续改 seller/buyer 管理端控制面，优先保持现有 contract test 和 service test 同步更新，不要只改 controller。
  - 涉及账号、日志、票据筛选时，继续禁止新增裸 `accountId` 查询入口，保持 `subjectId + accountId` fail-closed。
  - 涉及会话、重置密码、强制踢出和免密代入时，继续以 `PortalLoginSessionConsistencyContractTest` 和 `TerminalSqlIsolationContractTest` 作为回归门。

## 4. 可跑的最小验证命令

### 推荐最小命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller,buyer,ruoyi-system,ruoyi-framework -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest,PermissionServiceAccountPermissionTest,PortalLoginSessionConsistencyContractTest,TerminalSqlIsolationContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

### 更窄的合同测试命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system,ruoyi-framework -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest,PermissionServiceAccountPermissionTest,PortalLoginSessionConsistencyContractTest,TerminalSqlIsolationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

### 更窄的 service 回归命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## P2 记录

- 本次切片未记录新的 P2 UI/浏览器类问题；按任务要求未展开浏览器、DOM、截图和 UI 细调验证。
