# 2026-06-05 端内 OWNER 主账号唯一性 Service 硬化记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，补齐端内账号控制面的一个后端兜底：同一卖家或买家主体不能通过管理端账号接口手工新增第二个 `OWNER` 主账号。本轮按“seller 先形成标准样板，验收后复制 buyer；每个切片只改一类东西”执行。

## 本轮范围

- 修改 `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`。
- 修改 `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`。
- 修改 `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`。
- 修改 `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`。
- 更新复用台账和目标追踪。
- 不改前端 UI。
- 不新增或修改 SQL。
- 不执行远程 MySQL / Redis DDL 或 DML。
- 不处理 `lock_status` / `lock_reason` / 解锁账号。

## 已完成

- seller 侧先做标准样板：
  - `insertSellerAccount(...)` 在账号归一化后判断 `accountRole=OWNER` 时是否已有主账号，已有则抛出 `卖家主账号已存在`。
  - `updateSellerAccount(...)` 编辑账号时强制沿用当前账号角色，不采纳前端 payload 的 `accountRole`。
  - `SellerServiceImplTest` 新增“新增第二个 OWNER 拒绝”测试。
  - `SellerServiceImplTest` 新增“编辑主账号时保留当前 OWNER 角色”测试。
- buyer 侧按 seller 模板复制：
  - `insertBuyerAccount(...)` 在账号归一化后判断 `accountRole=OWNER` 时是否已有主账号，已有则抛出 `买家主账号已存在`。
  - `updateBuyerAccount(...)` 编辑账号时强制沿用当前账号角色，不采纳前端 payload 的 `accountRole`。
  - `BuyerServiceImplTest` 新增“新增第二个 OWNER 拒绝”测试。
  - `BuyerServiceImplTest` 新增“编辑主账号时保留当前 OWNER 角色”测试。
- 6 个只读 explorer 子 agent 已辅助盘点现状并全部关闭。结论一致：前端禁选不足以作为后端约束，当前 SQL 层也没有 OWNER 唯一约束。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl seller -am test
mvn -pl buyer -am test
```

验证结果：

- `mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`SellerServiceImplTest` 为 `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`BuyerServiceImplTest` 为 `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl seller -am test`：通过，`ruoyi-system` 为 `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`finance` 为 `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`seller` 为 `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`。本次并行跑 Maven 时出现一次 Surefire 临时目录 warning，但构建结果为 `BUILD SUCCESS`。
- `mvn -pl buyer -am test`：通过，`ruoyi-system` 为 `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`finance` 为 `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`buyer` 为 `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`。

## 未验证原因

- 本轮没有跑 HTTP smoke；改动是 Service 层业务规则和单测守卫，不改变接口路径或前端交互。
- 本轮没有做浏览器验收；不改变可见 UI。
- 本轮没有执行 SQL；数据库唯一约束属于后续独立 DDL 方案。

## 权限检查结果

- 本轮不新增接口，不改 `@PreAuthorize`。
- 管理端账号新增/编辑接口仍使用既有 `seller:admin:account:*` / `buyer:admin:account:*` 权限点。
- 本轮补的是后端业务兜底，不替代管理端按钮权限或低权限运行验收。

## 字典/选项复用检查结果

- 本轮未新增字典或选项。
- 账号角色仍复用 `PartnerSupport.ACCOUNT_ROLE_OWNER` / `PartnerSupport.ACCOUNT_ROLE_STAFF` 和既有端账号角色字典。
- 角色白名单校验尚未单独补齐；如需补 `ADMIN` 常量或字典驱动校验，建议作为后续独立小切片处理。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md` 的 `PartnerAccountModal / PartnerAccountRoleModal` 条目，登记 seller/buyer 主账号唯一性 Service 兜底。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 5 changed files`、`Modified: 5 - 457 nodes in 819ms`。

## 大文件合理性判断结果

- `SellerServiceImplTest.java` / `BuyerServiceImplTest.java` 已超过 500 行，本轮只在既有账号生命周期测试文件内各补 2 个同类 service 规则测试，并复用现有 `Recording*Mapper` / `RecordingPortalTokenSupport`。
- 继续扩账号、登录、日志、会话测试时，应考虑抽公共测试支撑，降低 seller/buyer 双份 helper 漂移风险；本轮暂不拆分，避免把测试结构重构混入主账号唯一性切片。

## 重复代码检查结果

- buyer 侧按 seller 已验收模板复制，只替换 terminal、字段、service 和文案。
- 没有新增第二套前端账号管理组件。
- 没有新增 mapper 查询方法；当前复用既有 `selectOwnerSellerAccountBySellerId` / `selectOwnerBuyerAccountByBuyerId` 做 Service 级判断。

## 残留问题

- 数据库层“每个主体只能一个 OWNER”的唯一约束已在后续 `docs/plans/2026-06-05-terminal-owner-account-db-constraint-record.md` 落地。
- `lock_status` / `lock_reason` / 解锁账号仍未设计和落地。
- `accountRole` 白名单校验已在后续 `docs/plans/2026-06-05-terminal-account-role-whitelist-service-record.md` 补齐。
