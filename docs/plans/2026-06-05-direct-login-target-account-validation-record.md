# 2026-06-05 免密票据目标账号失效守卫记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本切片只补 direct-login 票据目标账号失效的 service 测试：管理端签发免密票据后，如果目标 seller/buyer 端账号不存在，或账号已被改绑到其他主体，目标端不能创建登录 session。本轮不改生产代码、不改前端、不新增权限点、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- `SellerServiceImplTest` 先作为标准模板补齐 2 个用例：账号缺失拒绝、账号归属变化拒绝。
- seller 模板通过后，`BuyerServiceImplTest` 按同构方式复制 2 个用例，只替换 terminal、实体、编号和 service。
- 新增用例均验证失败时 `PortalTokenSupport.createLogin(...)` 不会被调用，不会写入端登录 session。
- 新增用例均验证失败登录日志写入目标主体 ID，账号 ID 保持为空，避免把不存在或错绑账号记成有效账号。
- 更新 `docs/architecture/reuse-ledger.md` 和 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`。

## 新增问题

- 无。

## 残留问题

- 本切片没有新增真实 HTTP / 浏览器 smoke；当前只固定 service 层对目标账号缺失和归属变化的守卫。
- 本切片没有新增票据状态；“validator 失败时不 mark used、不删 Redis payload”的行为由 `PortalDirectLoginSupportTest` 的前序用例继续守住。
- `SellerServiceImplTest` / `BuyerServiceImplTest` 文件已经偏大，后续继续扩账号、会话、免密测试时应单独切片评估拆分测试类。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalDirectLoginSupportTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test

cd E:\Urili-Ruoyi
git diff --check -- RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-direct-login-target-account-validation-record.md
```

## 验证结果

- seller 模板测试：`SellerServiceImplTest` 为 `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- buyer 同构测试：`BuyerServiceImplTest` 为 `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- clean 合并验证：`PortalDirectLoginSupportTest` 为 `Tests run: 6`，`SellerServiceImplTest` 为 `Tests run: 28`，`BuyerServiceImplTest` 为 `Tests run: 28`，均无失败，`BUILD SUCCESS`。
- `git diff --check`：通过；仅有 Git 行尾转换 warning，无空白错误。
- 冲突标记扫描：未命中。

## 未验证原因

- 本切片不启动后端，因为没有改 controller、路由、权限注解或运行时配置。
- 本切片不连接远程 MySQL / Redis，因为未新增或修改 DDL / DML，也未做真实票据消费 smoke。
- 本切片不做浏览器验证，因为前端没有变化。

## 权限检查结果

- 本切片未新增或修改权限点。
- 管理端免密代入仍使用 `seller:admin:directLogin` / `buyer:admin:directLogin`。
- 免密票据审计仍使用 `seller:admin:ticket:list` / `buyer:admin:ticket:list`。

## 字典/选项复用检查结果

- 本切片未新增字典、字段选项或前端下拉。

## 复用台账检查结果

- 已在 `docs/architecture/reuse-ledger.md` 的 `PortalDirectLoginSupport` 段落登记：目标端消费免密 token 前必须校验目标账号存在且仍属于票据主体。
- 已在 `PartnerAccountModal` / seller-buyer 账号管理段落登记：`SellerServiceImplTest` / `BuyerServiceImplTest` 已按同一模板覆盖账号不存在和账号改绑后的 direct-login 拒绝。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 2 changed files`，退出码 `0`。

## 大文件合理性判断结果

- `SellerServiceImplTest.java`：`1347` 行，已超过 500 行；本切片只在既有 direct-login 测试组内新增 2 个同类 service 用例，不在本轮拆分。后续继续扩账号、会话或免密测试时，应单独评估拆分测试类。
- `BuyerServiceImplTest.java`：`1346` 行，已超过 500 行；本切片按 seller 模板复制 2 个同构用例，不在本轮拆分。
- 本切片没有修改生产 service 文件。

## 重复代码检查结果

- seller 先形成标准模板并通过单测，buyer 只替换 terminal、实体、编号和 service。
- 两端测试存在同构重复，这是三端账号权限物理隔离后的当前选择；本轮不抽共享测试 helper，避免把端边界隐藏到过度抽象里。
