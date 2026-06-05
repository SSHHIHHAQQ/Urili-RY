# 2026-06-05 免密票据消费前状态校验记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本切片只处理一类问题：管理端已生成的 seller/buyer 免密票据，如果目标主体或端账号后续被停用/锁定，不能先把票据标记为 `USED` 再拒绝登录。本轮不改前端、不新增权限、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- `PortalDirectLoginSupport` 增加 `consumeToken(portalType, token, validator)` 重载，validator 在 `markPortalDirectLoginTicketUsed(...)` 和 Redis payload 删除之前执行。
- 保留原两参数 `consumeToken(portalType, token)`，继续兼容旧调用。
- `SellerServiceImpl.directLoginSeller(...)` 接入消费前 validator，按 token 中的 subject/account 重新读取当前 `seller` 和 `seller_account`，并复用登录状态校验。
- `BuyerServiceImpl.directLoginBuyer(...)` 按 seller 模板同构复制，读取当前 `buyer` 和 `buyer_account`。
- `PortalDirectLoginSupportTest` 增加 validator 失败测试，固定失败时不标记 `USED`、不标记 `EXPIRED`、不删除 Redis payload。
- `SellerServiceImplTest` / `BuyerServiceImplTest` 各补 4 个用例：正常免密消费、票据生成后主体停用拒绝、账号停用拒绝、账号锁定拒绝。
- 更新 `docs/architecture/reuse-ledger.md` 和 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`。

## 新增问题

- 无。

## 残留问题

- 管理端 direct-login 权限已有静态契约测试，但 `PermissionServiceAccountPermissionTest` 还缺一组矩阵断言：只有 `*:admin:account:resetPwd`、`*:admin:account:lock` 或 `*:admin:forceLogout` 时不能通过 `*:admin:directLogin`。建议下一切片单独补权限矩阵，不和本轮消费顺序混合。
- 仍未做 HTTP smoke；真实接口入口复用既有 seller/buyer direct-login controller，本轮只固定 support/service 语义。
- 当前没有主动撤销未消费免密票据的 `REVOKED` 状态；本轮选择“消费前状态校验失败则不消耗票据”，不新增票据状态或 DDL。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalDirectLoginSupportTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalDirectLoginSupportTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test

cd E:\Urili-Ruoyi
git diff --check -- RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java
```

## 验证结果

- 非 clean 定向测试：`PortalDirectLoginSupportTest` 为 `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`；`SellerServiceImplTest` 为 `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`；`BuyerServiceImplTest` 为 `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`。
- clean 定向测试：同上，且重新编译 `ruoyi-system`、`seller`、`buyer`，`BUILD SUCCESS`。
- `git diff --check`：通过，仅有 Git 行尾转换 warning，无空白错误。
- 残留关键词扫描：无常见代码占位词、冲突标记或未完成占位标记命中。

## 未验证原因

- 本切片不启动后端，因为只改 support/service 消费顺序和对应单元测试。
- 本切片不连接远程 MySQL / Redis，因为未新增或修改 DDL / DML，也未做运行时 smoke。
- 本切片不做浏览器验证，因为前端没有变化。

## 权限检查结果

- 本轮没有新增或修改权限点。
- 管理端主体级/账号级免密代入仍使用 `seller:admin:directLogin` / `buyer:admin:directLogin`。
- 票据列表仍使用 `seller:admin:ticket:list` / `buyer:admin:ticket:list`。

## 字典/选项复用检查结果

- 本切片未新增字典、字段选项或前端下拉。

## 复用台账检查结果

- 已在 `docs/architecture/reuse-ledger.md` 的 `PortalDirectLoginSupport` 段落登记：目标端消费免密 token 时，必须在票据标记 `USED` 和删除 Redis payload 之前完成当前主体/端账号状态校验。
- 已在 `PartnerAccountModal` 段落登记：seller/buyer service 测试已按同一模板守住免密票据消费前状态复验。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 7 changed files`，退出码 `0`。
- 文档回填后复跑 `codegraph sync .`：通过，输出 `Already up to date`，退出码 `0`。

## 大文件合理性判断结果

- `PortalDirectLoginSupport.java`：`239` 行，未触发 300 行阈值。
- `PortalDirectLoginSupportTest.java`：`498` 行，接近 500 行；职责仍集中在共享免密票据支撑，新增用例属于同一职责。本轮不拆，后续继续扩票据状态、撤销或并发消费测试时应考虑拆分。
- `SellerServiceImpl.java`：`751` 行，已超过 500 行；本轮只在既有免密登录消费链路中接入 validator，不新增独立业务模块，因此不在本轮拆。
- `BuyerServiceImpl.java`：`751` 行，已超过 500 行；按 seller 模板同构复制，不在本轮拆。
- `SellerServiceImplTest.java`：`1292` 行，已超过 500 行；新增用例属于 seller service 免密登录行为守卫。后续继续扩账号、会话或免密测试时，应单独切片拆出账号/免密相关测试类。
- `BuyerServiceImplTest.java`：`1291` 行，已超过 500 行；按 seller 测试模板同构复制，不在本轮拆。

## 重复代码检查结果

- seller 先形成模板并通过定向测试，buyer 只替换端类型、字段、service 和 terminal。
- 两端仍存在同构 service/test 代码；这是三端隔离后 seller/buyer 物理独立的当前实现选择。后续若抽公共 helper，必须先确认不会破坏端隔离、权限边界和可读性。
