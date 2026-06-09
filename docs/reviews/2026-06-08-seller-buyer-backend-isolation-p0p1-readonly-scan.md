# 2026-06-08 seller/buyer 后端隔离只读 P0/P1 扫描

## 扫描范围

- 仓库：`E:\Urili-Ruoyi`
- 模式：只读 review，不修改业务代码，不做浏览器/截图/DOM/UI 细调
- 切片：`RuoYi-Vue/seller`、`RuoYi-Vue/buyer`
- 重点：账号、角色、菜单、部门、登录日志、操作日志、会话、portal product 相关 `Controller` / `Service` / `Mapper` / `Test`
- 关注等级：仅 P0/P1
  - 编译/契约
  - guard
  - 接口/权限
  - 串端
  - service/字段缺失
  - 裸 `accountId`
  - terminal 不匹配仍审计
  - `resetPwd` 默认密码回退
  - `session:list` / `forceLogout` 权限错配
  - buyer/seller 机械复制遗漏

## 新增问题

本切片未发现新增 P0/P1 问题。

## 已确认通过的 P0/P1 检查点

1. **未发现 seller/buyer 端内控制面混用 `sys_*`**
   - seller 账号/会话/日志主链路使用 `seller_account`、`seller_role`、`seller_menu`、`seller_dept`、`seller_login_log`、`seller_oper_log`、`seller_session`：
     - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:272`
     - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:384`
     - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml:47`
     - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalDeptMapper.xml:27`
   - buyer 对称使用 `buyer_*`：
     - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:272`
     - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:384`
     - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:47`
     - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalDeptMapper.xml:27`

2. **未发现裸 `accountId` 查询**
   - 生产 Mapper/Service 已统一为 `subjectId + accountId`：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/mapper/SellerMapper.java:34`
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerMapper.java:34`
     - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:283`
     - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:283`
   - 静态契约测试明确禁止 account-only lookup：
     - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalAccountIsolationTest.java:103`

3. **跨主体查询防线存在**
   - 管理端会话/强退/account 读取均先校验主体与账号归属：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:151`
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:273`
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:151`
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:273`
   - portal 端自助日志/会话查询也固定到当前 session 的 `subjectId + accountId`：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:525`
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:540`
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:525`
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:540`

4. **terminal 不匹配时未把外端票据审计写入当前端**
   - seller：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:733`
   - buyer：
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:733`
   - 对应单测已覆盖 foreign ticket 场景：
     - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java:966`
     - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java:966`

5. **`resetPwd` 未回退到默认密码语义**
   - 管理端接口使用 `*:admin:account:resetPwd`，并把请求体密码传给 service：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:143`
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:143`
   - service 侧走 `PartnerSupport.normalizeTemporaryPassword(password)`，未见默认 `U12346` 或 `resetDefaultPwd`：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:237`
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:237`
   - seed/合同测试也固定为 `*:admin:account:resetPwd`，禁止旧权限名：
     - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSeedPermissionContractTest.java:69`
     - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java:71`
     - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java:71`

6. **`session:list` 与 `forceLogout` 权限未混绑**
   - seller 管理端：
     - 会话列表：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:152`
     - 强退：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:172`
   - buyer 管理端：
     - 会话列表：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:152`
     - 强退：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:172`
   - 后端契约测试已明确校验：
     - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java:74`
     - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java:74`

7. **portal 自助日志/会话接口未直接暴露内部审计模型**
   - seller 自助接口返回 `PortalOwn*Profile`：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerService.java:64`
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:329`
   - buyer 对称：
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerService.java:64`
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:329`
   - DTO 序列化合同也在：
     - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java:24`

8. **buyer/seller 机械复制面未见明显串端遗漏**
   - portal 权限服务仍显式阻止 `seller:admin:*` / `buyer:admin:*` 漏入端内权限：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java:463`
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java:463`
   - 端内菜单 ID 区间校验仍在：
     - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml:326`
     - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml:326`
   - portal product 只读接口 seller/buyer 分别校验 terminal，且 seller 读取限定 `session.subjectId`，buyer 仅走上架可见商品视图，未见把 seller scope 机械带入 buyer：
     - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:29`
     - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java:33`
     - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImplTest.java`
     - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImplTest.java`

## 残留问题

- 本次切片内未确认新的 seller/buyer 后端 P0/P1 残留问题。
- 残留风险主要是**已有防线未来被回归破坏**，而不是当前代码已经失守。当前仓库存在大量进行中的未提交改动，后续改动如果触碰 seller/buyer、portal log、terminal contract、seed SQL，需要继续依赖现有合同测试兜底。

## 最小修复建议

当前无需为本切片追加代码修复。

最小后续动作建议：

1. 保持 `TerminalAccountIsolationTest`、`SellerAdminPermissionContractTest`、`BuyerAdminPermissionContractTest`、`PortalDirectLoginAuthContractTest` 为 seller/buyer 改动前置门禁。
2. 若后续继续扩展 portal product 或 admin 审计能力，优先复制现有 seller/buyer 对称测试模板，不要只改单边。
3. 若后续有人尝试恢复 account-only lookup、旧 `resetPwd` 权限名、或把 `session:list` 与 `forceLogout` 合并，直接视为 P1 回归。

## 验证命令

本次只读扫描使用了以下命令：

```powershell
Get-Content AGENTS.md -Encoding UTF8
Get-Content docs/plans/2026-06-04-three-terminal-isolation-control-plan.md -Encoding UTF8
rg -n "select.*AccountById|resetPwd|forceLogout|session:list|directLogin|sys_user|sys_role|sys_menu|sys_dept" RuoYi-Vue/seller RuoYi-Vue/buyer
Get-Content RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java -Encoding UTF8
Get-Content RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java -Encoding UTF8
Get-Content RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java -Encoding UTF8
Get-Content RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java -Encoding UTF8
Get-Content RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml -Encoding UTF8
Get-Content RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml -Encoding UTF8
Get-Content RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml -Encoding UTF8
Get-Content RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml -Encoding UTF8
git status --short
```

## 未验证原因

- 未运行 `mvn test` / `verify-three-terminal`：本轮按用户要求执行只读源码扫描，未进入编译或测试执行。
- 未连接 MySQL / Redis：本轮不做 SQL、运行态或缓存验证，只基于代码与配置文件判定。
- 未做浏览器、截图、DOM、UI 校验：按本轮 P0/P1 范围显式跳过。

## 数据源确认结果

- 激活 profile：`spring.profiles.active = druid`
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml:71`
- MySQL：`application-druid.yml` 使用环境变量 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml:8`
- Redis：`application.yml` 使用环境变量 `RUOYI_REDIS_HOST` / `RUOYI_REDIS_PORT` / `RUOYI_REDIS_PASSWORD`
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml:88`
- 本次未读取 `.env.local`，因此**只确认了连接来源是环境变量注入，未解析出实际目标主机**
- 本次未触达远端 DB/Redis

## 远端 DB/Redis 影响记录

- 本次未执行 DDL / DML
- 本次未执行 Redis 读写
- 本次未启动或重启后端

## 表设计 / 高影响 SQL / 确认 token / 回滚

- 本次为只读代码审查，不涉及新表设计
- 本次未执行高影响 SQL
- 本次不涉及确认 token
- 本次不涉及回滚动作

## 三端隔离判断结果

- 账号：通过，seller/buyer 账号查询仍保持主体约束
- 权限：通过，`session:list` / `forceLogout` / `account:resetPwd` 权限名正确分离
- 菜单：通过，端内菜单表与端内权限前缀校验存在
- 日志：通过，terminal 不匹配票据不会把外端审计上下文写入当前端
- 会话：通过，会话列表/强退均走主体+账号约束
- token / Redis key：本轮未直接读 support 实现细节，但相关合同测试仍在；本切片未见 seller/buyer controller/service 回归迹象

## 权限检查结果

- 管理端 seller/buyer account/session/directLogin/audit 相关接口权限绑定正确
- portal seller/buyer 自助接口权限前缀正确，且禁止 `seller:admin:*` / `buyer:admin:*` 混入端内权限

## 字典 / 选项复用检查结果

- 本切片未发现 seller/buyer 端内权限、账号、会话逻辑自行复制字典体系的问题
- 国家/地区等公共校验仍通过共享 `dictTypeService` / `PartnerSupport` 处理，未见端内重复实现

## 复用台账检查结果

- 本轮未更新 `docs/architecture/reuse-ledger.md`
- 从 seller/buyer 对称实现看，当前复用了共享的 `PartnerSupport`、`PortalTokenSupport`、`PortalDirectLoginSupport`、`PortalPermissionSupport`，未见额外散落实现

## CodeGraph 更新结果

- 未执行 `codegraph sync .`
- 原因：本次为只读扫描，没有代码更新

## 大文件合理性判断结果

- 本轮未新增或修改业务文件，不涉及新的大文件合理性判断
- `SellerServiceImpl` / `BuyerServiceImpl` 体量较大，但当前职责仍围绕 terminal 账号、会话、审计主链，对本次只读 P0/P1 扫描不构成新增问题

## 重复代码检查结果

- seller/buyer 存在大量镜像式对称实现，但本轮未发现因为机械复制导致的 seller/buyer 串端、权限错配或字段缺失
- 这种对称实现后续仍建议继续由合同测试守住，不建议在本轮只读扫描里把“对称代码多”本身定为问题

## 子 Agent 使用记录

- 未使用子 Agent

## 工作区状态说明

- 当前工作区已有大量用户在制改动，`git status --short` 为脏工作树
- 本次只读扫描未回退任何现有改动，也未修改 seller/buyer 业务代码

## 一句话结论

本次 seller/buyer 后端隔离只读 P0/P1 扫描未发现新增问题，当前切片里关于 `sys_*` 混用、裸 `accountId`、terminal 串端审计、`resetPwd` 默认密码回退、`session:list` / `forceLogout` 权限错绑的关键防线和合同测试都还在。
