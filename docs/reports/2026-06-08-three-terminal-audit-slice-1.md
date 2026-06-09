# 三端隔离 P0/P1 审计切片 1

- 审计时间：2026-06-08
- 审计方式：只读扫描，不改代码
- 审计范围：
  - `RuoYi-Vue/seller`
  - `RuoYi-Vue/buyer`
  - `RuoYi-Vue/ruoyi-system`
- 聚焦对象：账号、角色、菜单、部门 `Mapper / Service / Controller`
- 审计目标：
  1. 生产代码是否仍复用 `sys_user/sys_role/sys_menu/sys_dept` 作为卖家/买家端内账号权限
  2. 是否存在裸 `accountId` 查询/调用，没有同时带 `sellerId/buyerId`
  3. 是否存在跨端表、跨端权限前缀、`seller:admin` / `buyer:admin` 被端内菜单使用

## 结论摘要

- `P0`：未发现
- `P1`：未发现
- `P2`：未发现明确违规；有 2 个建议继续保留并强化现有 guard

---

## P0

### 未发现 1：卖家/买家端内账号、角色、菜单、部门仍复用 `sys_*` 作为端内权限事实源

证据：

- 卖家账号查询落在 `seller_account` / `seller_dept`，未回落到 `sys_user/sys_dept`
  [SellerMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerMapper.xml:272)
  [SellerMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerMapper.xml:283)
- 卖家菜单/角色关系落在 `seller_menu` / `seller_role` / `seller_role_menu` / `seller_account_role`
  [SellerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerPortalPermissionMapper.xml:129)
  [SellerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerPortalPermissionMapper.xml:237)
  [SellerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerPortalPermissionMapper.xml:273)
  [SellerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerPortalPermissionMapper.xml:290)
- 卖家部门落在 `seller_dept`，部门下账号存在性检查也查 `seller_account`
  [SellerPortalDeptMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerPortalDeptMapper.xml:26)
  [SellerPortalDeptMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerPortalDeptMapper.xml:73)
- 买家账号查询落在 `buyer_account` / `buyer_dept`，未回落到 `sys_user/sys_dept`
  [BuyerMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerMapper.xml:272)
  [BuyerMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerMapper.xml:283)
- 买家菜单/角色关系落在 `buyer_menu` / `buyer_role` / `buyer_role_menu` / `buyer_account_role`
  [BuyerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerPortalPermissionMapper.xml:129)
  [BuyerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerPortalPermissionMapper.xml:237)
  [BuyerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerPortalPermissionMapper.xml:273)
  [BuyerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerPortalPermissionMapper.xml:290)
- 买家部门落在 `buyer_dept`，部门下账号存在性检查也查 `buyer_account`
  [BuyerPortalDeptMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerPortalDeptMapper.xml:26)
  [BuyerPortalDeptMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerPortalDeptMapper.xml:73)

说明：

- `ruoyi-system` 内部仍大量存在 `sys_user/sys_role/sys_menu/sys_dept`，但该部分是管理端基础能力本身，不构成“卖家/买家端内继续复用 `sys_*`”的违规。
- 本次扫描未发现 `seller` / `buyer` 生产代码直接读取 `sys_user/sys_role/sys_menu/sys_dept` 作为端内权限事实源。

建议：

- 继续保持“管理端在 `ruoyi-system`，端内控制面在 `seller/*`、`buyer/*`”的分层，不要把 portal 端查询回接到 `Sys*Mapper`。

### 未发现 2：存在裸 `accountId` 查询/调用，缺少 `sellerId/buyerId` 约束

证据：

- 卖家 Mapper 仅暴露双键查询 `selectSellerAccountByIdAndSellerId`，未暴露单参数 `selectSellerAccountById`
  [SellerMapper.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\mapper\SellerMapper.java:32)
  [SellerMapper.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\mapper\SellerMapper.java:34)
- 卖家账号主查询 SQL 同时带 `seller_id + seller_account_id`
  [SellerMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerMapper.xml:283)
- 卖家 Service 的账号读取入口也要求 `(sellerId, sellerAccountId)`，内部继续调用双键 Mapper
  [SellerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java:151)
  [SellerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java:154)
- 卖家权限服务校验账号时同样要求 `(sellerId, accountId)`
  [SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:398)
  [SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:401)
- 买家 Mapper 仅暴露双键查询 `selectBuyerAccountByIdAndBuyerId`，未暴露单参数 `selectBuyerAccountById`
  [BuyerMapper.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\mapper\BuyerMapper.java:32)
  [BuyerMapper.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\mapper\BuyerMapper.java:34)
- 买家账号主查询 SQL 同时带 `buyer_id + buyer_account_id`
  [BuyerMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerMapper.xml:283)
- 买家 Service 的账号读取入口也要求 `(buyerId, buyerAccountId)`，内部继续调用双键 Mapper
  [BuyerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java:151)
  [BuyerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java:154)
- 买家权限服务校验账号时同样要求 `(buyerId, accountId)`
  [BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:398)
  [BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:401)

建议：

- 继续禁止新增单参数 `select*AccountById(accountId)` Mapper/Service 入口。

---

## P1

### 未发现 1：端内菜单使用跨端权限前缀、`seller:admin` / `buyer:admin` 或通配权限

证据：

- 通用校验明确要求端内菜单权限必须以当前端前缀开头，禁止 `*`，禁止 `seller:admin:` / `buyer:admin:`
  [PortalPermissionSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalPermissionSupport.java:126)
- 卖家菜单写入前统一调用 `normalizeTerminalMenu(menu, "seller")`，角色绑定时再次逐条校验菜单 ID / component / perms
  [SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:99)
  [SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:304)
  [SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:318)
- 卖家 portal 权限下发前再次拒绝 `seller:admin:*`、非 `seller:` 前缀和 `*:*:*`
  [SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:463)
- 卖家角色菜单批量绑定前，先通过 `countSellerMenusByIds` 将菜单 ID 限定在 `100000-199999`
  [SellerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\resources\mapper\seller\SellerPortalPermissionMapper.xml:324)
- 买家菜单写入前统一调用 `normalizeTerminalMenu(menu, "buyer")`，角色绑定时再次逐条校验菜单 ID / component / perms
  [BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:99)
  [BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:304)
  [BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:318)
- 买家 portal 权限下发前再次拒绝 `buyer:admin:*`、非 `buyer:` 前缀和 `*:*:*`
  [BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:463)
- 买家角色菜单批量绑定前，先通过 `countBuyerMenusByIds` 将菜单 ID 限定在 `200000-299999`
  [BuyerPortalPermissionMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\resources\mapper\buyer\BuyerPortalPermissionMapper.xml:324)

建议：

- 继续把 `PortalPermissionSupport` 作为唯一入口，不要在 Controller 或 Mapper 层旁路写菜单。

### 未发现 2：生产代码存在跨端表读写

证据：

- 对 `seller/src/main` 搜索 `buyer_*` / `Buyer`，未命中生产代码
- 对 `buyer/src/main` 搜索 `seller_*` / `Seller`，未命中生产代码
- `ruoyi-system` 中与 seller/buyer 相关的生产 SQL 命中仅为 portal 操作日志表插入，不涉及端内账号/角色/菜单/部门事实源
  [PortalOperLogMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\resources\mapper\system\PortalOperLogMapper.xml:9)
  [PortalOperLogMapper.xml](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\resources\mapper\system\PortalOperLogMapper.xml:25)

建议：

- 后续新增 `ruoyi-system` 支撑能力时，保持它只承载共享审计/票据/配置，不直接掌管 seller/buyer 端内 RBAC 事实表。

---

## P2

### P2 建议 1：继续保留并扩展“端内菜单 guard + 角色绑定 guard”的双层防线

原因：

- 当前实现同时在写菜单时校验 `component/perms`，在角色绑菜单时校验菜单区间和逐条菜单合法性，这套组合已经把“跨端菜单 ID”“管理端前缀污染”“空 perms”挡在 Service 层。

证据：

- [PortalPermissionSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalPermissionSupport.java:57)
- [SellerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:304)
- [BuyerPortalPermissionServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:304)

建议：

- 后续如果新增 seed、批量导入、迁移脚本或后台批处理，也要复用同一套校验，不要只在 Web Controller 路径生效。

### P2 建议 2：继续保留“Controller 不直连 Mapper”的分层约束

原因：

- 本次扫描未发现目标范围内 Controller 直接注入 Mapper；当前风险控制主要集中在 Service 层，若以后绕过 Service，很容易重新引入裸查或跨端绑定。

建议：

- 继续用架构契约测试或简单静态扫描守住 `Controller -> Service -> Mapper` 分层。

---

## 最终结论

本次只读扫描中，针对你指定的 3 类问题，在目标范围内 **未发现 P0/P1 级违规**：

1. 未发现卖家/买家端内账号权限继续复用 `sys_user/sys_role/sys_menu/sys_dept`
2. 未发现裸 `accountId` 查询/调用绕过 `sellerId/buyerId`
3. 未发现端内菜单使用跨端权限前缀、`seller:admin` / `buyer:admin` 或跨端表

当前这块生产代码看起来已经基本符合“三端隔离 P0/P1 审计切片 1”的目标要求。
