# 2026-06-05 买家账号生命周期 Service 测试复制记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在卖家账号生命周期 Service 测试守卫验收后，把同一套账号生命周期测试模板复制到买家端。本轮按“先 seller 标准样板，验收通过后复制 buyer；每个切片只改一类东西”执行。

## 本轮范围

- 只改 `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`。
- 不改业务实现。
- 不改卖家测试。
- 不新增或修改 SQL。
- 不执行远程 MySQL / Redis DDL 或 DML。
- 不改前端。

## 已完成

- `BuyerServiceImplTest` 从 5 个测试扩展到 12 个测试。
- 按 seller 模板复制买家端账号新增测试：
  - 密码必须加密保存。
  - 未显式传账号角色时默认 `STAFF`。
  - 部门必须属于当前买家。
- 按 seller 模板复制买家端账号新增负向测试：
  - 传入其他买家的部门时拒绝保存。
- 按 seller 模板复制默认密码重置测试：
  - 重置目标为 `buyer_account` 端内账号。
  - 默认密码按 BCrypt 密文保存，不保存明文。
- 按 seller 模板复制账号停用测试：
  - 编辑账号为停用状态后，只强制踢出该账号会话。
  - 删除 Redis token 时使用 `buyer` 端前缀。
- 按 seller 模板复制登录成功测试：
  - 登录成功后更新 `buyer_account.last_login_*`。
  - 写入 `buyer_session`。
  - 写入 `buyer_login_log` 成功记录。
- 按 seller 模板复制登录失败测试：
  - 停用账号不能登录。
  - 不签发 portal token。
  - 写入 `buyer_login_log` 失败记录。
- 按 seller 模板复制当前账号会话列表测试：
  - 查询范围来自 `PortalLoginSession.subjectId/accountId`。
  - 当前 token 标记为 `current=true`。
  - 其他 token 标记为 `current=false`。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl buyer -am test
```

验证结果：

- `mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`BuyerServiceImplTest` 为 `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl buyer -am test`：通过，`ruoyi-system` 为 `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`finance` 为 `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`buyer` 为 `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`。

## 未验证原因

- 本轮没有跑真实 HTTP smoke；这是 service 单测复制切片，不改变运行接口。
- 本轮没有执行 SQL；不涉及数据库结构或权限 seed 变化。
- 本轮没有做浏览器验收；不改变前端 UI。

## 权限检查结果

- 本轮不新增接口，不改 `@PreAuthorize`。
- 测试固定的是端内账号 service 行为，不替代管理端接口权限、按钮权限或低权限运行验收。

## 字典/选项复用检查结果

- 本轮未新增字典或选项。
- 账号角色仍复用 `PartnerSupport.ACCOUNT_ROLE_STAFF` / `PartnerSupport.ACCOUNT_ROLE_OWNER`。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md` 的 `PartnerAccountModal / PartnerAccountRoleModal` 条目，登记 `SellerServiceImplTest` / `BuyerServiceImplTest` 按同一模板守住账号生命周期。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 大文件合理性判断结果

- `BuyerServiceImplTest.java` 扩展后仍围绕 `BuyerServiceImpl` 的端账号、登录、日志和会话行为，职责单一。
- 本轮新增的是测试辅助代理和测试用 token support，避免改业务实现或新增测试专用生产代码。

## 重复代码检查结果

- 本轮是经 seller 验收后的同构复制，替换了 terminal、字段、service 和文案。
- buyer 测试代理复用同一个 `RecordingBuyerMapper` / `RecordingPortalTokenSupport`，没有为每个断言重新写一套 mock。

## 残留问题

- `lock_status` / `lock_reason` / 解锁账号仍未设计和落地，后续需要单独 DDL/后端/前端方案。
- 主账号唯一性目前仍主要依赖 `createOwnerAccountIfNeeded` 和前端禁选 OWNER；是否需要数据库唯一约束或后端禁止第二个 OWNER，需单独讨论。
