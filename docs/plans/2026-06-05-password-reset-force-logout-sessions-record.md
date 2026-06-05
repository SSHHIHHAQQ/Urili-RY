# 2026-06-05 密码重置强制踢出端会话记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本切片只处理一类问题：管理端重置 seller/buyer 端账号密码后，旧端会话不能继续使用。本轮不改前端、不新增菜单权限、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- `SellerServiceImpl` 的自定义重置密码、恢复默认密码、重置主体主账号密码三个入口增加事务边界。
- seller 端账号密码更新成功后，立即调用既有会话强踢链路，只踢出目标 seller 端账号会话，并通过 `PortalTokenSupport.deleteLoginTokens("seller", tokenIds)` 删除 Redis token。
- `SellerServiceImplTest` 先作为标准模板补齐三类入口的 service 单测，固定密码密文写入、目标账号会话范围和 seller terminal token 删除。
- `BuyerServiceImpl` 按 seller 模板同构复制，只替换 buyer 字段、service 和 terminal。
- `BuyerServiceImplTest` 按 seller 模板补齐三类入口的 service 单测，固定 buyer terminal token 删除。
- 更新 `docs/architecture/reuse-ledger.md` 和 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`。

## 新增问题

- 无。

## 残留问题

- 本切片未做 HTTP smoke；真实接口入口仍复用既有管理端 controller、权限契约和 service 行为。
- 本切片没有新增“密码重置接口权限”测试，因为账号域权限契约已由前序 `*:admin:account:resetPwd` 切片覆盖；本轮只补密码重置后的会话失效行为。
- 当前 seller/buyer service 和 test 文件已经超过拆分阈值；本轮只补同一账号控制链路，没有拆分文件。后续若继续扩账号管理、免密、会话或权限行为，应单独拆分 account/session 子 service 和测试类。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test

cd E:\Urili-Ruoyi
git diff --check -- RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java
```

## 验证结果

- seller 定向测试：`Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- buyer 定向测试：`Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- seller + buyer clean 定向测试：seller / buyer 各 `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`；该命令确认 clean 后重新编译，不依赖旧 class 文件。
- `git diff --check`：通过，仅有 Git 行尾转换 warning，无空白错误。
- 残留关键词扫描：无命中。

## 未验证原因

- 本切片不启动后端，因为只补 service 行为和单元测试。
- 本切片不连接远程 MySQL / Redis，因为未新增或修改 DDL / DML，也未做运行时 smoke。
- 本切片不做浏览器验证，因为前端没有变化。

## 权限检查结果

- 管理端重置端账号密码仍使用既有账号域权限：seller 为 `seller:admin:account:resetPwd`，buyer 为 `buyer:admin:account:resetPwd`。
- 本切片没有改变主体级免密代入、主体级/账号级会话查看和强制踢出权限模型。
- 会话强踢行为发生在 service 内部，不向前端新增绕过权限的接口。

## 字典/选项复用检查结果

- 本切片未新增字典、字段选项或前端下拉。

## 复用台账检查结果

- 已在 `docs/architecture/reuse-ledger.md` 的 `PartnerAccountModal` 段落登记：管理端重置 seller/buyer 端账号密码后，必须清理该端账号既有会话和对应 terminal 的 Redis token。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`，退出码 `0`。

## 大文件合理性判断结果

- `SellerServiceImpl.java`：`738` 行，已超过 500 行；文件本来已承担 seller 主体、账号、免密、登录、会话等多类 service 行为。本切片只在既有账号密码重置链路旁增加会话失效调用，拆分会扩大范围，因此不在本轮拆。
- `BuyerServiceImpl.java`：`738` 行，已超过 500 行；本轮按 seller 模板同构复制，不引入新的结构差异，因此不在本轮拆。
- `SellerServiceImplTest.java`：`1140` 行，已超过 500 行；当前测试类集中守 seller service 行为，本轮只追加三个账号密码重置会话失效用例。后续继续扩账号、会话或免密测试时，应单独切片拆出账号相关测试类。
- `BuyerServiceImplTest.java`：`1139` 行，已超过 500 行；本轮按 seller 测试模板同构复制，不在本轮拆。

## 重复代码检查结果

- seller 先形成模板并通过定向测试，buyer 只做字段、terminal、service 名称替换。
- 两端仍存在同构 service/test 代码；这是三端隔离后 seller/buyer 物理独立的当前实现选择。后续若抽公共 helper，必须先确认不会破坏端隔离、权限边界和可读性。
