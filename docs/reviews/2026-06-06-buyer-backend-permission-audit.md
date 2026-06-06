# E:\Urili-Ruoyi 买家模块后端权限审计（只保留 P0/P1）

审计范围：
- 模块：`RuoYi-Vue/buyer`
- 仅审计 buyer 管理端与买家端接口（`AdminBuyer*Controller`、`BuyerPortal*Controller`）
- 相关共用类：`PortalPreAuthorize`、`PortalPreAuthorizeAspect`

## 主代理复核结论

- 下方“买家端会话/个人信息/日志接口只做 terminal/session 鉴权”不按本轮 P1 阻塞处理。
- 复核理由：`getInfo`、`getRouters`、`logout`、`profile`、`account/profile`、`account/password`、查看自己的登录日志/操作日志/会话，属于登录后自助基础能力；只要求端会话鉴权是当前 seller/buyer portal 的一致模板。
- 真正需要细分 `hasPermi` 的账号、部门、角色等端内管理资源已经有 `buyer:account:list`、`buyer:dept:list`、`buyer:role:list` 等权限点。
- 结论：本报告保留为只读审计输入，不作为当前 P0/P1 修复项。

## P0

- 无新增 P0（高危）发现。

## P1

1. `BuyerPortalController` 的部分买家端会话/个人信息/日志接口只做 terminal/session 鉴权，没有 `hasPermi` 细分权限（仅有 `@PortalPreAuthorize(terminal = "buyer")`）：
   - `/buyer/getInfo` → [BuyerPortalController.java:59-60](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:60)
   - `/buyer/getRouters` → [BuyerPortalController.java:68-71](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:70)
   - `/buyer/logout` → [BuyerPortalController.java:78-81](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:80)
   - `/buyer/profile` → [BuyerPortalController.java:88-91](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:90)
   - `/buyer/account/profile` → [BuyerPortalController.java:98-101](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:100)
   - `/buyer/account/password` → [BuyerPortalController.java:108-111](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:110)
   - `/buyer/account/login-logs` → [BuyerPortalController.java:163-166](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:166)
   - `/buyer/account/oper-logs` → [BuyerPortalController.java:174-177](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:177)
   - `/buyer/account/sessions` → [BuyerPortalController.java:185-188](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:188)

   - 风险：目前该类接口只有登录会话级鉴权，不区分是否允许该账号执行该动作，容易引入“角色误配/越权放大”的边界风险。
   - 最小修复建议：
     - 为接口补齐精细 `hasPermi`，按功能拆分权限码，例如 `buyer:account:self:view`、`buyer:account:password:change`、`buyer:account:loginLog:list`、`buyer:account:operLog:list`、`buyer:account:session:list`。
     - 若采用新权限码，补齐 `sql/seller_buyer_management_seed.sql` 的权限初始化，并保持买家端权限分配 UI 与后端一致。

## 说明（非 P0/P1）

- Admin 侧已整体按 `@PreAuthorize("@ss.hasPermi('buyer:admin:...')")` 落地，未见 `@PreAuthorize` 与 `@PortalPreAuthorize` 串端。
- 买家端路由除 `BuyerPortalAuthController` 外（登录入口例外）均满足 `@Anonymous + @PortalPreAuthorize(terminal="buyer") + @PortalLog + PortalSessionContext.requireSession("buyer")` 的控制模板。
- 契约验证已执行并通过：
  - `mvn -pl ruoyi-system -Dtest="TerminalRouteOwnershipTest,BuyerAdminPermissionContractTest" test`
  - 结果：`BUILD SUCCESS`（0 failures）
