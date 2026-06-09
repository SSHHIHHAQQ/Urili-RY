# 三端隔离只读审查（切片 B）

- 日期：2026-06-09
- 范围：`E:\Urili-Ruoyi\RuoYi-Vue\seller`、`E:\Urili-Ruoyi\RuoYi-Vue\buyer`
- 模型：`gpt-5.4`
- 模式：只读 P0/P1 审查，不改业务代码
- 参考：
  - `AGENTS.md`
  - `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 审查目标

核查 seller/buyer Java `Role/Menu/Dept/Account` service 侧是否满足以下约束：

1. `role_menu` 写入前是否对 `menuIds` 做全量校验。
2. 是否存在跨端 menu id、空 `perms` / `component`、`seller:admin` / `buyer:admin` 命名空间被端内 role 绑定的风险。
3. seller / buyer 是否保持对称。
4. 是否存在对应 contract tests / critical tests 覆盖。

## 结论速览

- P0：未发现
- P1：未发现
- seller / buyer：当前实现与测试保持对称
- contract / critical tests：已覆盖本切片关注点，且本轮定向执行通过

## Findings

本切片未发现需要立即修复的 P0 / P1 问题。

## 证据

### 1. `role_menu` 写入前存在 fail-closed 全量校验

- seller：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:171`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:185`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:305`
- buyer：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:171`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:185`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:305`

判定依据：

- `insertRole(...)` / `updateRole(...)` 在真正写 `*_role_menu` 前，都会先调用 `assertRoleMenusExist(role)`。
- `assertRoleMenusExist(role)` 先用 `countSellerMenusByIds(...)` / `countBuyerMenusByIds(...)` 做集合级计数校验，再逐个 `select*MenuById(...)` 校验：
  - menu id 区间必须属于当前端；
  - 页面菜单 `component` 必须使用当前端根路径；
  - 页面/按钮菜单 `perms` 不能为空；
  - `perms` 不允许跨端前缀；
  - `perms` 不允许 `seller:admin:*` / `buyer:admin:*`；
  - `perms` 不允许 `*` 通配。

### 2. shared support 已把空 perms/component、跨端前缀、admin 命名空间做成公共 fail-closed

- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalPermissionSupport.java:64`
- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalPermissionSupport.java:76`
- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalPermissionSupport.java:102`
- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalPermissionSupport.java:126`

其中：

- `assertTerminalMenuId(...)` 固定 seller 为 `100000-199999`、buyer 为 `200000-299999`。
- `assertTerminalMenuComponent(...)` 对页面菜单强制当前端页面根路径。
- `assertTerminalMenuPerms(...)` 明确拒绝：
  - 空页面/按钮权限；
  - 非当前端前缀；
  - `seller:admin:*` / `buyer:admin:*`；
  - `*` 通配。

### 3. mapper 层也有终端隔离约束，不是只靠 service

- seller mapper：
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml:324`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml:367`
- buyer mapper：
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:324`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:367`

判定依据：

- `countSellerMenusByIds` / `countBuyerMenusByIds` 只统计当前端表、当前端 ID 区间、`status='0'` 的菜单。
- `batchSellerRoleMenu` / `batchBuyerRoleMenu` 只从当前端 `*_role` 与 `*_menu` 表写入，且再次限制当前端 ID 区间和 `status='0'`。

### 4. seller / buyer 对称性成立

- service 实现结构、校验逻辑、错误路径一致，只替换了 terminal、表名和 ID 区间。
- `SellerPortalDeptServiceImpl` 与 `BuyerPortalDeptServiceImpl` 也保持对称，且本切片未发现 dept/account service 绕过 role/menu 边界直接写 `*_role_menu` 的路径。

## 测试覆盖

### 已有 critical / contract tests

- `react-ui/tests/three-terminal.manifest.json:68-79`
- `react-ui/tests/three-terminal.manifest.json:109-120`
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRoleMenuMapperIsolationContractTest.java`
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java`
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java`
- `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplTest.java`
- `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplMenuTreeTest.java`
- `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplTest.java`
- `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplMenuTreeTest.java`

覆盖到的关键点：

- `insertRole` / `updateRole` 在越界 `menuIds` 时 fail-closed。
- dirty menu（跨端 ID、admin perms、共享 component）不能被 role 绑定。
- portal 读菜单时也会拒绝 dirty terminal menu。
- role-menu mapper 不能串用另一端表或 `sys_menu`。
- seller / buyer admin controller 权限模板保持对称。

### 本轮执行命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller,buyer,ruoyi-system -am "-Dtest=SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplMenuTreeTest,TerminalRoleMenuMapperIsolationContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：

- `BUILD SUCCESS`
- `BuyerAdminPermissionContractTest`：4 通过
- `SellerAdminPermissionContractTest`：4 通过
- `TerminalRoleMenuMapperIsolationContractTest`：1 通过
- `SellerPortalPermissionServiceImplMenuTreeTest`：8 通过
- `SellerPortalPermissionServiceImplTest`：17 通过
- `BuyerPortalPermissionServiceImplMenuTreeTest`：8 通过
- `BuyerPortalPermissionServiceImplTest`：17 通过

## 最小修复建议

本切片无 P0 / P1 修复项。

若后续继续增强稳态，最小可做但不阻塞本轮的补强是：

1. 给 `updateRole(...)` 增加一条“dirty in-range menu”显式单测，和 `insertRole(...)` 的 dirty case 对齐。
2. 在 `PortalPermissionSupportTest` 中把 `assertTerminalMenuComponent(...)` / `assertTerminalMenuPerms(...)` 的 seller/buyer 对称矩阵补全成表驱动测试，减少镜像回归风险。
