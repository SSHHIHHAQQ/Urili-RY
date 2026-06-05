# 2026-06-05 端账号角色白名单 Service 校验记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在 OWNER 主账号唯一性 Service 硬化之后，补齐端账号 `accountRole` 合法值校验。目标是防止 API 传入 `ROOT`、`SUPER` 等非法端账号角色并落库。

## 本轮范围

- 修改 `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PartnerSupport.java`。
- 修改 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PartnerSupportTest.java`。
- 修改 `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`。
- 修改 `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`。
- 修改 `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`。
- 修改 `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`。
- 更新复用台账和目标追踪。
- 不改前端 UI。
- 不新增或修改 SQL。
- 不执行远程 MySQL / Redis DDL 或 DML。

## 已完成

- `PartnerSupport` 新增 `ACCOUNT_ROLE_ADMIN` 常量。
- `PartnerSupport` 新增 `normalizeAccountRole(...)`：
  - 空值默认 `STAFF`。
  - 输入统一转大写。
  - 只允许 `OWNER` / `ADMIN` / `STAFF`。
  - 非法值抛出 `账号角色不正确`。
- seller 侧先接入：
  - `normalizeSellerAccount(...)` 改为调用 `PartnerSupport.normalizeAccountRole(...)`。
  - `SellerServiceImplTest` 新增非法 `accountRole` 拒绝测试。
- buyer 侧按 seller 模板复制：
  - `normalizeBuyerAccount(...)` 改为调用 `PartnerSupport.normalizeAccountRole(...)`。
  - `BuyerServiceImplTest` 新增非法 `accountRole` 拒绝测试。
- `PartnerSupportTest` 新增角色默认值、大写化和非法值拒绝测试。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -am "-Dtest=PartnerSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl seller -am test
mvn -pl buyer -am test
```

验证结果：

- `mvn -pl ruoyi-system -am "-Dtest=PartnerSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`PartnerSupportTest` 为 `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`SellerServiceImplTest` 为 `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`BuyerServiceImplTest` 为 `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl seller -am test`：通过，`ruoyi-system` 为 `Tests run: 42, Failures: 0, Errors: 0, Skipped: 0`，`finance` 为 `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`seller` 为 `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl buyer -am test`：通过，`ruoyi-system` 为 `Tests run: 42, Failures: 0, Errors: 0, Skipped: 0`，`finance` 为 `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`buyer` 为 `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`。

## 未验证原因

- 本轮没有跑 HTTP smoke；改动是 Service 层输入校验和公共 helper，不改变接口路径或前端交互。
- 本轮没有做浏览器验收；不改变可见 UI。
- 本轮没有执行 SQL；不涉及数据库结构或 seed 变化。

## 权限检查结果

- 本轮不新增接口，不改 `@PreAuthorize`。
- 管理端账号新增/编辑接口仍使用既有账号域权限点。
- 本轮补的是服务端输入校验，不替代权限或按钮控制。

## 字典/选项复用检查结果

- 本轮未新增字典数据。
- 角色合法值与第一阶段计划保持一致：`OWNER` / `ADMIN` / `STAFF`。
- `ADMIN` 已补公共常量，后续如果端账号角色字典 code 调整，必须同步 `PartnerSupport.normalizeAccountRole(...)`。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 `PartnerSupport.normalizeAccountRole(...)` 作为端账号角色白名单公共 helper。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 6 changed files`、`Modified: 6 - 462 nodes in 764ms`。

## 大文件合理性判断结果

- `SellerServiceImplTest.java` / `BuyerServiceImplTest.java` 已超过 500 行。本轮只补每端 1 个同类非法角色 service 测试，没有新增新的测试框架或大范围拆分。
- 后续继续扩端账号 service 测试时，应考虑抽公共测试支撑，降低 seller/buyer helper 漂移风险。

## 重复代码检查结果

- 角色合法值判断集中在 `PartnerSupport.normalizeAccountRole(...)`，seller/buyer service 复用同一个 helper。
- buyer 测试按 seller 模板复制，只替换 terminal、字段、service 和文案。

## 残留问题

- 数据库层“每个主体只能一个 OWNER”的唯一约束已在后续 `docs/plans/2026-06-05-terminal-owner-account-db-constraint-record.md` 落地。
- `lock_status` / `lock_reason` / 解锁账号仍未设计和落地。
