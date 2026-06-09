# 2026-06-09 三端管理端控制接口只读 P0/P1 审计（切片 1）

## 审计范围

- 工作区：`E:\Urili-Ruoyi`
- 审计方式：只读，不改代码
- 审计目标：管理端 seller / buyer admin 控制接口
- 重点接口族：
  - `account`
  - `role`
  - `menu`
  - `session`
  - `direct-login`
  - `force logout`
  - `reset password`
- P0/P1 口径：
  - 编译
  - guard
  - 接口
  - 权限
  - 串端
  - service / 字段缺失

## 结论

本次切片 **未发现 P0/P1 问题**。

seller / buyer 管理端控制面在当前工作树下表现为：

- Controller 路径与 terminal 前缀对称。
- `@PreAuthorize` 权限点对称，且关键操作未退化为过宽权限。
- `@Log` 在敏感写操作和敏感读操作上存在，direct-login ticket 列表也保留了敏感读日志。
- account 查询、会话、密码重置、强退、免密代入均走 `sellerId/buyerId + accountId` 双键约束，没有发现裸 `accountId` 的管理端 service / mapper 漏口。
- direct-login ticket 查询在 service 层会强制补 terminal scope，并在按账号筛选时校验 `subjectId + accountId` 归属。
- seller / buyer 两端实现保持镜像，对称性没有发现破口。

## P0/P1 发现

无。

## 关键核对文件

### Controller

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java`
  - `84-107`：账号列表、账号角色回显与分配权限组合
  - `143-203`：`resetPwd` / `sessions` / `forceLogout` / `directLogin`
  - `227-233`：`directLoginTickets` 使用 `seller:admin:ticket:list`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java`
  - `84-107`：账号列表、账号角色回显与分配权限组合
  - `143-203`：`resetPwd` / `sessions` / `forceLogout` / `directLogin`
  - `227-233`：`directLoginTickets` 使用 `buyer:admin:ticket:list`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerRoleController.java`
  - `33-82`：role `list/query/add/edit/remove/optionselect`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerRoleController.java`
  - `33-82`：role `list/query/add/edit/remove/optionselect`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerMenuController.java`
  - `32-81`：menu `list/query/treeselect/add/edit/remove`
  - `53-59`：`roleMenuTreeselect` 同时要求 `role:query + menu:query`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerMenuController.java`
  - `31-80`：menu `list/query/treeselect/add/edit/remove`
  - `52-58`：`roleMenuTreeselect` 同时要求 `role:query + menu:query`

### Service

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `240-246`：管理端账号密码重置后强制踢出会话
  - `251-281`：主体级 / 账号级 session 查询与强退
  - `285-313`：主体级 / 账号级 direct-login
  - `317-326`、`476-507`：登录日志 / 操作日志按 `sellerId + accountId` 归属收口
  - `584-602`：direct-login ticket terminal + subject/account scope 归一
  - `936-1001`：强退审计绑定当前 admin actor
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - 与 seller 对应段落对称：`240-246`、`251-281`、`285-313`、`317-326`、`476-507`、`584-602`、`936-1001`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java`
  - `239-250`、`398-405`：账号角色分配前，账号归属必须命中 `sellerId + accountId`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java`
  - `239-250`、`398-405`：账号角色分配前，账号归属必须命中 `buyerId + accountId`
- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`
  - `55-107`：票据生成要求当前 admin actor，写入 acting admin / reason / token hash
  - `179-237`：消费票据时校验 terminal、expiry、target subject/account 一致性
  - `282-295`：票据一次性消费并清理 terminal-scoped Redis key

### Mapper 接口

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/mapper/SellerMapper.java`
  - `44-90`：管理端相关账号 / 会话 / 日志接口全部为 `sellerId` 或 `sellerId + sellerAccountId` 约束
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerMapper.java`
  - `32-78`：管理端相关账号 / 会话 / 日志接口全部为 `buyerId` 或 `buyerId + buyerAccountId` 约束

## 命令与搜索证据

### 代码搜索

1. 列出 seller / buyer 管理端控制器与相关 service / mapper：

```powershell
rg --files RuoYi-Vue | rg "(seller|buyer).*(Controller|Service|Mapper)|Portal|DirectLogin|Session|Account|Role|Menu"
```

2. 审计 controller 的路径、权限、日志注解、关键 handler：

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@PreAuthorize|@Log|forceLogout|direct-login|resetPwd|resetDefaultPwd|sessions/list|roleIds|assign|terminal|seller:admin|buyer:admin" RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller
```

3. 审计是否存在裸 `accountId` 管理端查询口、是否误用旧权限名：

```powershell
rg -n "select.*AccountById\\(|resetDefaultPwd|directLoginTickets|forceLogout.*admin|actingAdmin|session:list|forceLogout|resetPwd|directLogin" RuoYi-Vue/seller/src/main/java RuoYi-Vue/buyer/src/main/java RuoYi-Vue/ruoyi-system/src/main/java
```

### 契约测试

执行命令：

```powershell
mvn -pl ruoyi-system -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest,PortalAdminAuditBindingContractTest,PortalLoginSessionConsistencyContractTest,AdminAccountPermissionUiContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：

- `BUILD SUCCESS`
- 共执行 `14` 个测试
- `Failures: 0`
- `Errors: 0`

覆盖到的关键契约包括：

- `SellerAdminPermissionContractTest`
- `BuyerAdminPermissionContractTest`
- `AdminDirectLoginPermissionContractTest`
- `PortalAdminAuditBindingContractTest`
- `PortalLoginSessionConsistencyContractTest`
- `AdminAccountPermissionUiContractTest`

## 本轮未纳入范围

- 浏览器运行态 / DOM / 截图 / UI 细调
- 非管理端 portal 自助接口深挖
- SQL seed 实库回放验证
- 前端页面交互实操

## 备注

- 当前工作树存在用户/既有未提交改动；本次审计以当前工作树为准，未改动任何源码。
- 本次为只读审计，未执行 `codegraph sync .`；原因是未发生代码更新。

## 子 Agent 使用记录

- 本文件原始审计阶段未记录子 Agent 使用。
- 2026-06-09 后续 P0/P1 目标追踪复核使用 6 个 `gpt-5.4` 子 Agent，覆盖 seller/buyer 后端隔离、portal auth/direct-login/session/log、SQL guard、React 管理端、React portal/request/401 和 verify gate。
- 6 个子 Agent 均已完成并关闭。
- 与本文件相关的管理端控制权和 React 管理端切片均未发现新的可坐实 P0/P1；结论被主线程采纳为“不需要新增业务代码修复”。
