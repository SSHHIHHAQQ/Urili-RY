# Seller/Buyer 后端端内账号权限控制面 P0/P1 复核报告（只读）

时间：2026-06-10
范围：`RuoYi-Vue/seller`、`RuoYi-Vue/buyer`、相关 `ruoyi-system` support/test
目标：复核是否存在 P0/P1 风险，重点核对：
- 是否复用 `sys_user/sys_role/sys_menu/sys_dept`
- 是否存在裸 `select*AccountById(accountId)` 或只按 `accountId` 查询
- Portal endpoint 是否信任前端传入 `sellerId/buyerId/subjectId/accountId`
- `getRouters` / `RouterVo` 契约是否完整
- `@PortalPreAuthorize`、`@PortalLog` 覆盖是否齐全

---

## 结论先行

### 本次范围内 **无 P0**。
### 存在 **1 个 P1 倾向问题**（权限语义治理级），见下文。

---

## 复核证据

### 1) 无 `sys_*` 复用（admin 控制面）

`seller`/`buyer` 主体与会计等生产代码中没有出现 `sys_user/sys_role/sys_menu/sys_dept`（含类名变体）：
- `RuoYi-Vue/seller/src/main/java` 全量扫描无匹配结果（`rg`）：`\bsys_user\b|\bsys_role\b|\bsys_menu\b|\bsys_dept\b`
- `RuoYi-Vue/buyer/src/main/java` 全量扫描无匹配结果（同上）
- 架构守门测试也同步约束该边界：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalAccountIsolationTest.java:31-47`
    - 正向约束：`sellerAndBuyerModulesMustNotReuseAdminSysAccountControlPlane`
    - 正则包含 `sys_user|sys_role|sys_menu|sys_dept|...`（`lines 19-22`）

### 2) 无裸 `accountId` 查询（生产路径）

`Seller` 与 `Buyer` 的账号查询方法均为复合主键（subjectId + accountId）：
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/mapper/SellerMapper.java:46-47`
  `selectSellerAccountByIdAndSellerId(@Param("sellerId") Long sellerId, @Param("sellerAccountId") Long sellerAccountId)`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerMapper.java:34-35`
  `selectBuyerAccountByIdAndBuyerId(@Param("buyerId") Long buyerId, @Param("buyerAccountId") Long buyerAccountId)`
- 服务签名同步：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerService.java:36`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerService.java:36`
- 门禁测试明确禁止 account-only：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalAccountIsolationTest.java:103-167`
  - 禁止 `sellerMapper.selectSellerAccountById(`、`buyerMapper.selectBuyerAccountById(`
  - 禁止 account-only 签名正则（`ACCOUNT_ID_ONLY_SIGNATURE`）

### 3) Portal endpoint 是否信任前端 ID

### 3.1 端内会话 API 未从前端传入 subject/account 做主控

`SellerPortalController` 与 `BuyerPortalController` 的所有会话相关接口都通过 `PortalSessionContext.requireSession(...)` 拿到会话，再以 session 的 `subjectId/accountId` 查询：
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java`
  - `getInfo`：`session.getSubjectId()` + `permissionService.selectPortalPermissionInfo(session)`（`line 65`）
  - `getRouters`：`session` 下发菜单树（`line 75`）
  - `accountProfile`：`sellerService.selectSellerAccountById(session.getSubjectId(), session.getAccountId())`（`line 106`）
  - `accounts`/`depts`/`roles`/日志/会话列表全部由 `session` 派生参数驱动（`lines 127,142,157,168,178,192`）
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`
  - 同步模式：`session.getSubjectId()` / `session.getAccountId()`（`lines 65,75,96,106,127,142,157,168,178,192`）

### 3.2 免密登录 endpoint 不读取前端 `sellerId/buyerId`

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalAuthController.java:36`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalAuthController.java:36`

只接收 `directLoginToken`：
- `Map<String, String> body` + `resolveDirectLoginToken(body).get("directLoginToken")`

服务端直接消费 token 并以 token 内字段为准：
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:684-697`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:684-697`
- token 解析/断言与记录也固定读 `token.getPartnerId()/getAccountId()`（如 `lines 686-697`, `710-715`, `727-735` / `727-736`）

### 4) `getRouters` / `RouterVo` 契约

- `Seller` 端：`SellerPortalController#getRouters` 返回 `PortalPermissionSupport.buildRouters(permissionService.selectPortalMenuTree(session))`（`SellerPortalController.java:75-76`）
- `Buyer` 端：同上（`BuyerPortalController.java:75-76`）
- `PortalPermissionSupport.buildRouters` 设置 `RouterVo.perms = menu.getPerms()`（`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalPermissionSupport.java:256`）
- `RouterVo` 存在 `perms` 字段及 getter/setter（`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/vo/RouterVo.java:47,123-131`）
- 契约测试确认上述要求（`RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/RouterVoPermissionContractTest.java:16-47`）

### 5) `@PortalPreAuthorize` / `@PortalLog` 覆盖

- `seller`/`buyer` Portal 端核心接口均带注解对齐：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java`（各方法均有 `@PortalPreAuthorize` + `@PortalLog`）
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`（各方法均有 `@PortalPreAuthorize` + `@PortalLog`）
- 登录/免密登录前置流程为匿名入口且使用 `@PortalLog`（无 `@PortalPreAuthorize` 为预期）：
  - `SellerPortalAuthController`、`BuyerPortalAuthController`
- 权限模板测试也约束关键 admin handler 权限位，包含会话/强退条目：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java:74-77`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java:74-77`

---

## P1 发现（治理级）

### admin 会话强退权限与会话列表权限未分离

- 现状：
  - 会话列表与账户/主体会话查询：
    - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:151/161`
    - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:151/161`
  - 强制退出使用的是 `seller:admin:forceLogout` / `buyer:admin:forceLogout`：
    - `AdminSellerController.java:171/179`
    - `AdminBuyerController.java:171/179`
- 契约测试也按上述现状进行断言（未要求分离）：
  - `SellerAdminPermissionContractTest.java:74-77`
  - `BuyerAdminPermissionContractTest.java:76-77`

这属于 AGENTS 规则中“会话列表与强退权限应分离”的治理性差异（与直接越权风险不同），建议以 P1 风险记录并修正为：
- 新增会话强退权限：`seller:admin:session:forceLogout`、`buyer:admin:session:forceLogout`
- 强退接口改用新权限，`session:list` 保持只读列表权限不变。

---

## 最小修法建议（仅建议，不改动文件）

1. 新增权限常量/种子与前后端契约：
   - `seller:admin:session:forceLogout`
   - `buyer:admin:session:forceLogout`
2. 后端改造：
   - `AdminSellerController` 两个 `@PreAuthorize("@ss.hasPermi('seller:admin:forceLogout')")` 改为 `seller:admin:session:forceLogout`
   - `AdminBuyerController` 两个同类改为 `buyer:admin:session:forceLogout`
3. 更新权限合同测试：
   - `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest` 对 `forceLogoutSeller` / `forceLogoutSellerAccount` / `forceLogoutBuyer` / `forceLogoutBuyerAccount` 的断言同步为新权限。
4. 如需保留旧强退权限语义，可在菜单/权限文档中同步更新 seed 与角色分配策略，不混用“会话列表”权限。

---

## 当前范围内剩余问题说明

除上述 P1 外，当前范围（`seller/buyer` 后端 + 相关 `ruoyi-system` 支持测试）内未发现可直接触发的 P0。
若你要，我可以下一步把这条 P1 按“最小修法”直接形成可执行修改补丁（含测试更新与 seed 权限说明）。

---

## 主线程复核结论：上述 P1 不采纳

时间：2026-06-10 12:24 +08:00

主线程按当前 `AGENTS.md` 复核后，判定本报告中的“新增 `seller:admin:session:forceLogout` / `buyer:admin:session:forceLogout`”建议不采纳，原因如下：

- 当前项目规则要求：管理端查看在线会话使用 `seller:admin:session:list` / `buyer:admin:session:list`，真正强制踢出使用 `seller:admin:forceLogout` / `buyer:admin:forceLogout`。
- 当前代码事实已经符合该规则：
  - `AdminSellerController#sessions` / `#accountSessions` 使用 `seller:admin:session:list`。
  - `AdminBuyerController#sessions` / `#accountSessions` 使用 `buyer:admin:session:list`。
  - `AdminSellerController#forceLogoutSeller` / `#forceLogoutSellerAccount` 使用 `seller:admin:forceLogout`。
  - `AdminBuyerController#forceLogoutBuyer` / `#forceLogoutBuyerAccount` 使用 `buyer:admin:forceLogout`。
- `react-ui` 管理端入口同样按 `:session:list` 展示会话查看，按 `:forceLogout` 展示强制踢出。

因此，这条只读子 Agent 结论按误报处理，不进入代码修改范围。
