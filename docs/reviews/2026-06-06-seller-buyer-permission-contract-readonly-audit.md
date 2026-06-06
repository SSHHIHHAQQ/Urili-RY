# 卖家/买家权限契约只读审计（2026-06-06）

## 审计范围
- SQL seed：`RuoYi-Vue/sql/seller_buyer_management_seed.sql`
- 后端权限契约测试：`RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/*`
- 前端 Guard 脚本：`react-ui/scripts/check-partner-management-template.mjs`

## 执行结论（仅 P0/P1）
- 未发现当前范围内可落地的 P0/P1 问题。
- 已执行静态脚本与架构测试，均无异常：
  - `node scripts/check-partner-management-template.mjs` 在 `E:\Urili-Ruoyi\react-ui` 下输出 `Partner management template guard passed.`
  - 运行 `mvn -q -Dtest='TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest,AdminAccountPermissionUiContractTest,TerminalRouteOwnershipTest,TerminalAccountIsolationTest' test`
    - 目标模块：`RuoYi-Vue/ruoyi-system`
    - 退出码：0（通过）
- 结论：当前未触发“权限缺失、菜单挂错端、端内角色菜单未绑定（在当前 seed 里的 owner/default 绑定链路下）”的阻断级问题。

## P0/P1 逐项检查证据

### 1) 权限缺失
- 后端控制器端口权限与 seed 的一致性从测试层面已验证：
  - 测试文件 [SellerAdminPermissionContractTest](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java:1) 和 [BuyerAdminPermissionContractTest](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java:1) 对 `@PreAuthorize` 权限集合与 seed 匹配进行强约束。
  - [TerminalSeedPermissionContractTest](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSeedPermissionContractTest.java:1) 强校验终端 Portal 权限在 seed 中存在。
- Portal/管理端路由权限端口的模板也被 [TerminalRouteOwnershipTest](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java:1) 与
  [TerminalSqlIsolationContractTest](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSqlIsolationContractTest.java:1) 约束覆盖。

### 2) 菜单挂错端
- seed 中端内系统菜单权限只按端打散，未出现跨端穿透迹象：
  - 买卖家端菜单表定义：`seller_menu` 与 `buyer_menu` 分表创建（见 [seller_buyer_management_seed.sql:218](/E:/Urili-Ruoyi/RuoYi-Vue/sql/seller_buyer_management_seed.sql:218), [seller_buyer_management_seed.sql:244](/E:/Urili-Ruoyi/RuoYi-Vue/sql/seller_buyer_management_seed.sql:244)）。
  - 买卖家端菜单种子都只使用各自前缀权限（例如 `seller:account:list`、`buyer:account:list`）；未发现 seed 内互串前缀（`seller:` 出现在 `seller_menu` 仅，`buyer:` 出现在 `buyer_menu` 仅）。
  - 管理端菜单与端内菜单仍分离（`sys_menu` vs `seller_menu/buyer_menu`）。

### 3) 端内角色菜单绑定
- 基础“端内角色菜单”绑定链路存在且完整：
  - `seller_menu` 注入覆盖：`seller:account:list/seller:dept:list/seller:role:list` 与分销/商品类权限，见 [seller_buyer_management_seed.sql:843](/E:/Urili-Ruoyi/RuoYi-Vue/sql/seller_buyer_management_seed.sql:843)。
  - `buyer_menu` 注入覆盖同口径内容（`buyer:*`），见 [seller_buyer_management_seed.sql:863](/E:/Urili-Ruoyi/RuoYi-Vue/sql/seller_buyer_management_seed.sql:863)。
  - 角色-菜单绑定覆盖同一权限集合，分别在 [seller_role_menu](/E:/Urili-Ruoyi/RuoYi-Vue/sql/seller_buyer_management_seed.sql:911) 与 [buyer_role_menu](/E:/Urili-Ruoyi/RuoYi-Vue/sql/seller_buyer_management_seed.sql:932) 语句中完成。
- 与 Portal 控制器实际权限口径对齐（如 `seller:product:distribution:list`、`seller:role:list` 等）经静态抽取比对后未见缺口（7/7 对齐）。

### 4) Guard 覆盖边界
- 前端统一模板 Guard 已覆盖 seller/buyer 页面与 service 映射：
  - [check-partner-management-template.mjs](/E:/Urili-Ruoyi/react-ui/scripts/check-partner-management-template.mjs:1) 在当前代码快照中未触发任何 violation（通过）。
- 审计测试覆盖点与前端脚本不冲突，整体未见“guard 命中缺失导致的 P0/P1 级缺口”。

## P2（仅记录）
1. `seller_role_menu` 与 `buyer_role_menu` 的默认绑定范围目前是基于“所有有效角色”条件执行（`r.del_flag='0' and r.status='0'`），未限定 `owner` 等角色类型
   - 代码位：`seller_role_menu` 插入语句 [911-929](/E:/Urili-Ruoyi/RuoYi-Vue/sql/seller_buyer_management_seed.sql:911)
   - [932-950](/E:/Urili-Ruoyi/RuoYi-Vue/sql/seller_buyer_management_seed.sql:932)
   - 影响：当出现精细化角色模型时，需确认是否允许所有活动角色自动继承该基础菜单集合。

## 验证命令
- `cd E:\Urili-Ruoyi\react-ui; node scripts/check-partner-management-template.mjs`
- `cd E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system; mvn -q -Dtest='TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest,AdminAccountPermissionUiContractTest,TerminalRouteOwnershipTest,TerminalAccountIsolationTest' test`
