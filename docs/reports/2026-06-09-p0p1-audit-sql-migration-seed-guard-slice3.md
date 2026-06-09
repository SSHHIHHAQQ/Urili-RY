# 只读 P0/P1 审计报告（切片 3：SQL / migration / seed / guard）

- 审计日期：2026-06-09
- 仓库：`E:\Urili-Ruoyi`
- 审计范围：`RuoYi-Vue` 下 seller/buyer 三端隔离相关 SQL、migration、seed、guard、契约测试、seller/buyer 权限与账号绑定链路
- 审计模式：只读，未修改业务代码
- 审计目标：
  - seller/buyer 独立表
  - `password not null` 且无默认值
  - 无 legacy `sys_user` 混用
  - `seller_menu` / `buyer_menu` ID 区间
  - 端内菜单 fail-closed
  - `role-menu` / `account-role` 校验
  - `sys_menu` seed owner / slot / signature guard
  - dynamic DDL 高影响确认门禁

## 结论

本次切片 **未发现明确的 P0/P1 问题**。

当前仓库在你点名的 8 个风险面上，已经形成了：

1. SQL 脚本 fail-closed 确认门禁
2. 增量脚本自动发现与契约测试
3. seller/buyer 端内菜单与角色绑定的运行时校验
4. legacy `sys_user` 回填与移除的分阶段门禁
5. `sys_menu` 顶层 seed 与 seller/buyer seed 的 slot/signature guard

下面给出逐项证据。

## P0/P1 Findings

无。

## 证据

### 1. seller / buyer 独立表存在，且独立账号权限表成套落地

- `seller_account` / `buyer_account` 独立建表：
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:744)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:892)
- 独立关系表成套存在：
  - `seller_account_role` / `buyer_account_role` / `seller_role_menu` / `buyer_role_menu`
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:1087)

原因：seller/buyer 控制面没有继续挂在 `sys_user` / `sys_role` / `sys_menu` 上。

建议：继续保持新增端内能力只进入 seller/buyer 表，不向 `sys_*` 回流。

### 2. password 满足 not null 且无默认值，并且迁移前置拦截空密码

- 建表定义中 `password varchar(100) not null`，未带默认值：
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:751)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:780)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:899)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:928)
- 迁移脚本会先拦截空密码，再允许推进：
  - `assert_no_blank_terminal_passwords`
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:372)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:831)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:832)

原因：符合“password not null no default”要求，且存量脏数据不是静默兜底，而是 fail-closed。

建议：后续新增 seed / migration 继续禁止对 `password` 加 `default ''`。

### 3. legacy sys_user 混用没有留在生产控制链路里，且 user_id 移除有前后门禁

- 迁移前先断言 `seller_account.user_id` / `buyer_account.user_id` 不能再残留绑定：
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:101)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:161)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:162)
- 迁移完成后强制要求移除 `user_id`：
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:649)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:658)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:893)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:894)
- legacy helper 被单独隔离为高影响脚本，并要求显式确认与精确签名：
  - [20260604_three_terminal_legacy_sys_user_account_backfill.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_legacy_sys_user_account_backfill.sql:27)
  - [20260604_three_terminal_legacy_sys_user_account_backfill.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_legacy_sys_user_account_backfill.sql:30)
  - [20260604_three_terminal_legacy_sys_user_account_backfill.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_legacy_sys_user_account_backfill.sql:107)
  - [20260604_three_terminal_legacy_sys_user_account_backfill.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_legacy_sys_user_account_backfill.sql:154)
  - [20260604_three_terminal_legacy_sys_user_account_backfill.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_legacy_sys_user_account_backfill.sql:184)
  - [20260604_three_terminal_legacy_sys_user_account_backfill.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_legacy_sys_user_account_backfill.sql:231)
- 生产代码静态契约禁止 seller/buyer 模块复用 admin `sys_*` 控制面：
  - [TerminalAccountIsolationTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\TerminalAccountIsolationTest.java:31)

原因：legacy `sys_user` 只剩下受控回填脚本，不是运行态依赖。

建议：继续把 legacy helper 留在一次性迁移区，不要在新 service / mapper 中重新引入 `user_id`。

### 4. seller_menu / buyer_menu ID 区间 guard 已落地

- migration 对终态区间做 fail-closed：
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:517)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:1079)
- seed 对区间、`auto_increment`、菜单完整性做 fail-closed：
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:187)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:1278)
- Java 侧读取与写入时再次断言 ID 区间：
  - [PortalPermissionSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalPermissionSupport.java:76)
  - [SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:319)
  - [BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:319)

原因：区间限制同时存在于 SQL 和 service 层，不是单点防线。

建议：新增端内菜单 seed 时继续沿用同一套区间 guard，不要只靠 `AUTO_INCREMENT` 惯性。

### 5. 端内菜单 fail-closed 已落地：权限前缀、禁 admin 前缀、页面 component 必填

- migration 检查 seller/buyer 端内菜单 perms 与 component：
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:523)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:536)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:545)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:565)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:578)
  - [20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql:587)
- seed 重复做同类约束：
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:240)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:249)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:312)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:321)
- Java 归一化 / 可读校验链再次兜底：
  - [PortalPermissionSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalPermissionSupport.java:64)
  - [PortalPermissionSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalPermissionSupport.java:102)
  - [PortalPermissionSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalPermissionSupport.java:126)
  - [SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:279)
  - [BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:279)

原因：端内菜单不会因为 seed 漏口或脏数据而静默串到 admin / other terminal。

建议：未来若扩展 portal 菜单导入能力，也必须直接复用 `PortalPermissionSupport` 这条校验链。

### 6. role-menu / account-role 校验已覆盖“存在性 + 归属 + OWNER 绑定”

- seller：
  - 角色归属校验：[SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:243)
  - OWNER 角色绑定校验：[SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:247)
  - 菜单存在性与端内约束校验：[SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:308)
  - SQL 层 subject scope：[SellerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerPortalPermissionMapper.xml:253)
  - SQL 层角色菜单写入：[SellerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerPortalPermissionMapper.xml:344)
  - SQL 层角色菜单解绑：[SellerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerPortalPermissionMapper.xml:358)
- buyer：
  - 角色归属校验：[BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:243)
  - OWNER 角色绑定校验：[BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:247)
  - 菜单存在性与端内约束校验：[BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:308)
  - SQL 层 subject scope：[BuyerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerPortalPermissionMapper.xml:253)
  - SQL 层角色菜单写入：[BuyerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerPortalPermissionMapper.xml:344)
  - SQL 层角色菜单解绑：[BuyerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerPortalPermissionMapper.xml:358)
- seed 对最终关系表再做完整性检查：
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:345)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:356)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:605)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:616)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:628)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:640)

原因：关系表不是裸写；service 和 SQL 两层都在拦跨主体、跨端、脏菜单、脏角色。

建议：后续若新增批量导入账号角色功能，直接走现有 service，而不是新开绕过校验的 mapper 批量写入。

### 7. sys_menu seed owner / slot / signature guard 已落地

- top menu seed：
  - [top_menu_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\top_menu_seed.sql:20)
  - [top_menu_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\top_menu_seed.sql:42)
  - [top_menu_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\top_menu_seed.sql:55)
  - [top_menu_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\top_menu_seed.sql:181)
  - [top_menu_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\top_menu_seed.sql:251)
- seller/buyer 管理 seed 要求 `2010` 已由 top menu seed 正确占位，且自身再做 id / signature guard：
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:47)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:89)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:116)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:129)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:1448)
  - [seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql:1517)

原因：`2010` 不是由多个 seed 随意 upsert，已有“先 top menu，再业务 seed”的 owner 顺序。

建议：后续涉及 `sys_menu` 的新 seed 继续沿用 `tmp_*_guard + slot/signature` 模式，不要退回简单 `insert ... on duplicate key update`。

### 8. dynamic DDL 与高影响 SQL 已有确认门禁和自动发现测试

- `SqlExecutionGuardContractTest` 显式覆盖 seller/buyer 关键脚本：
  - [SqlExecutionGuardContractTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java:57)
  - [SqlExecutionGuardContractTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java:60)
  - [SqlExecutionGuardContractTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java:183)
  - [SqlExecutionGuardContractTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java:189)
- dated SQL 与全部增量高影响 SQL 自动发现：
  - [SqlExecutionGuardContractTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java:213)
  - [SqlExecutionGuardContractTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java:241)
- dynamic DDL 专项合同：
  - [SqlExecutionGuardContractTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java:4846)
  - [SqlExecutionGuardContractTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java:4922)

原因：不是只靠人工约定，已经有契约测试保证高影响 SQL 必须 fail-closed。

建议：新增 dynamic DDL 脚本时，同步补进 `SqlExecutionGuardContractTest`，否则容易在未来回归中掉链。

## 验证

执行命令：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system,seller,buyer -am "-Dtest=SqlExecutionGuardContractTest,TerminalAccountIsolationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：

- `BUILD SUCCESS`
- `SqlExecutionGuardContractTest`：77 tests passed
- `TerminalAccountIsolationTest`：4 tests passed

## 未覆盖项

本次按“快速推进模式”只看 P0/P1，没有做：

- 浏览器 / DOM / UI 细节验证
- 远端库 live 数据核对
- 非 seller/buyer 范围的业务 SQL 语义复核

## 一句话总结

切片 3 当前没有扫出明确的 SQL / migration / seed / guard 级别 P0/P1 漏口；seller/buyer 独立表、密码约束、legacy `sys_user` 退出、端内菜单 fail-closed、关系表校验、`sys_menu` owner guard、dynamic DDL 门禁都已有静态与测试证据支撑。
