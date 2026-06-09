# 2026-06-09 Buyer 后端只读 P0/P1 审查（切片 B）

## 审查范围

- 仓库：`E:\Urili-Ruoyi`
- 模块：`RuoYi-Vue/buyer`
- 基线：
  - `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
  - `AGENTS.md`
- 模式：只读、快速推进模式
- 重点：
  - buyer/seller 串端
  - 权限前缀错误
  - 裸 `accountId` 查询
  - `session/list` 与 `forceLogout`
  - `resetPwd`
  - `direct-login` ticket payload / log / session 审计字段
  - buyer 端误用 seller 表或 `sys_*`

## 结论

本轮在 `RuoYi-Vue/buyer` 范围内，**未发现命中本切片范围的 P0/P1 缺陷**。

buyer 模块与 seller 模块做了归一化机械同构比对（仅忽略 `buyer/seller` 词面差异），本次比对结果未发现 buyer 偏离 seller 基线的额外差异；buyer 侧关键链路也都保持了 `buyerId + buyerAccountId` 约束、buyer 权限前缀约束，以及 direct-login 审计字段落库。

## 已核证据

### 1. 未发现裸 `accountId` 查询

- `BuyerMapper` 仅暴露 `selectBuyerAccountByIdAndBuyerId`，没有裸 `select*AccountById(accountId)`：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerMapper.java`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:283`
- `BuyerServiceImpl` 内部账号读取均走 `buyerId + buyerAccountId`：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:155`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:276`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:302`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:1017`
- 角色分配与 portal 权限链路也都带 `buyerId + accountId`：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:399-407`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:239-304`

### 2. `session/list` 与 `forceLogout` 权限分离正确

- 会话列表接口绑定 `buyer:admin:session:list`：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:152-169`
- 强退接口单独绑定 `buyer:admin:forceLogout`：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:172-185`
- service 和 mapper 侧都按 `buyerId` / `buyerAccountId` 限定 session 范围：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:251-281`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:491-553`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:562-579`

### 3. `resetPwd` 语义符合当前约束

- 管理端只保留 `resetPwd`，未发现 `resetDefaultPwd` 回退入口：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:143-150`
- service 侧走 `PartnerSupport.normalizeTemporaryPassword(password)`，说明是人工输入临时密码语义，不是静默重置默认密码：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:240-247`
- 密码重置后会强退该账号会话：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:951-963`

### 4. direct-login ticket / login log / session 审计字段齐全

- 管理端查询 ticket 时强制 `terminal=buyer`，并对 `targetSubjectId/targetAccountId` 做 buyer 范围校验：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:583-613`
- buyer 端消费免密票据固定走 `consumeToken("buyer", ...)`，端类型不匹配时不会按 buyer 成功消费：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:684-724`
- 登录日志落库包含 `direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`：
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:372-428`
- 操作日志查询映射也包含上述结构化审计字段：
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:430-475`
- session 落库与查询同样保留上述审计字段：
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:477-553`
- 强退审计对 direct-login session 走 `buildDirectLoginLog(...)`，普通 session 走 `buildLoginLog(...)`，并在后台强退场景补 `actingAdminId/actingAdminName`：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:936-1007`

### 5. buyer 权限前缀与菜单 guard 正常

- portal 权限读取后会逐项校验，要求权限必须以 `buyer:` 开头，禁止 `buyer:admin:` 和 `*:*:*`：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:443-470`
- 角色绑定菜单时，会验证菜单属于 buyer 端、组件路径符合 buyer 端、perms 符合 buyer 端：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:305-322`
- buyer 菜单 ID 区间也限制在 `200000-299999`：
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:326-338`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:367-378`

### 6. 未发现 buyer 误用 seller 表或 `sys_*`

- 对 `RuoYi-Vue/buyer/src/main/java` 与 `RuoYi-Vue/buyer/src/main/resources/mapper/buyer` 扫描 `seller_`、`Seller`、`seller:`、`/seller`、`sys_`，未命中可疑引用。
- buyer/seller 主源码做归一化机械同构 diff，未发现 buyer 相对 seller 的异常偏差。

## 最小修复建议

本切片当前 **无需 P0/P1 代码修复**。

若要继续降低回归风险，建议只补测试，不改实现：

1. 给 buyer 补一条与 seller 对称的 contract test，固定 `session/list` 必须使用 `buyer:admin:session:list`，`DELETE .../sessions` 必须使用 `buyer:admin:forceLogout`。
2. 给 buyer 补一条 direct-login 审计 contract test，固定 `buyer_login_log` / `buyer_session` 的 `direct_login_*` 与 `acting_admin_*` 字段不会回退成空落库。

## 本轮未验证

- 未做浏览器、截图、DOM、UI 细调验证（按本轮快速推进模式跳过）。
- 未连接 live 数据源验证远端库中的 buyer 表结构和历史数据质量；本轮仅做源码只读审查。
- 未运行 `verify-three-terminal` 或 buyer 相关测试；本轮目标是静态审查与 seller 同构比对，不是执行验证。

## 审查命令摘要

- 读取方案与模块源码：
  - `Get-Content docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
  - `Get-ChildItem RuoYi-Vue/buyer -Recurse -File`
- 扫描 buyer/seller 关键关键词：
  - `rg -n "accountId|buyerId|sellerId|resetPwd|forceLogout|directLogin|actingAdmin|ticket|hasPermi|PortalPreAuthorize|PortalLog" ...`
- buyer/seller 机械同构归一化 diff：
  - 按 `buyer/seller`、`Buyer/Seller`、`BUYER/SELLER` 归一化后逐文件比较 `src/main` 下 `.java/.xml`
