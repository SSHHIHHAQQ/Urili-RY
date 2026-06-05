# 2026-06-05 卖家账号生命周期 Service 测试守卫记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在管理端共享模板守卫之后，只补卖家端账号生命周期自动化守卫。本轮按“先 seller 标准样板，验收通过后再复制 buyer；每个切片只改一类东西”执行。

## 本轮范围

- 只改 `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`。
- 不改业务实现。
- 不复制买家。
- 不新增或修改 SQL。
- 不执行远程 MySQL / Redis DDL 或 DML。
- 不改前端。

## 已完成

- `SellerServiceImplTest` 从 5 个测试扩展到 12 个测试。
- 补充卖家端账号新增测试：
  - 密码必须加密保存。
  - 未显式传账号角色时默认 `STAFF`。
  - 部门必须属于当前卖家。
- 补充卖家端账号新增负向测试：
  - 传入其他卖家的部门时拒绝保存。
- 补充默认密码重置测试：
  - 重置目标为 `seller_account` 端内账号。
  - 默认密码按 BCrypt 密文保存，不保存明文。
- 补充账号停用测试：
  - 编辑账号为停用状态后，只强制踢出该账号会话。
  - 删除 Redis token 时使用 `seller` 端前缀。
- 补充登录成功测试：
  - 登录成功后更新 `seller_account.last_login_*`。
  - 写入 `seller_session`。
  - 写入 `seller_login_log` 成功记录。
- 补充登录失败测试：
  - 停用账号不能登录。
  - 不签发 portal token。
  - 写入 `seller_login_log` 失败记录。
- 补充当前账号会话列表测试：
  - 查询范围来自 `PortalLoginSession.subjectId/accountId`。
  - 当前 token 标记为 `current=true`。
  - 其他 token 标记为 `current=false`。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl seller -am test
```

验证结果：

- `mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`SellerServiceImplTest` 为 `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl seller -am test`：通过，`ruoyi-system` 为 `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`seller` 为 `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\seller\src\test\java\com\ruoyi\seller\service\impl\SellerServiceImplTest.java`：通过，仅有 LF/CRLF 工作区换行提示。
- 冲突标记检查：通过。

## 未验证原因

- 本轮没有复制买家；按已确认节奏，buyer 应在 seller 测试模板验收后独立切片复制。
- 本轮没有跑真实 HTTP smoke；这是 service 单测守卫切片，不改变运行接口。
- 本轮没有执行 SQL；不涉及数据库结构或权限 seed 变化。

## 权限检查结果

- 本轮不新增接口，不改 `@PreAuthorize`。
- 测试固定的是端内账号 service 行为，不替代管理端接口权限、按钮权限或低权限运行验收。

## 字典/选项复用检查结果

- 本轮未新增字典或选项。
- 账号角色仍复用 `PartnerSupport.ACCOUNT_ROLE_STAFF` / `PartnerSupport.ACCOUNT_ROLE_OWNER`。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md` 的 `PartnerAccountModal / PartnerAccountRoleModal` 条目，登记 `SellerServiceImplTest` 新增账号生命周期守卫。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 3 changed files`、`Modified: 3 - 213 nodes in 564ms`。

## 大文件合理性判断结果

- `SellerServiceImplTest.java` 扩展后仍围绕 `SellerServiceImpl` 的端账号、登录、日志和会话行为，职责单一。
- 本轮新增的是测试辅助代理和测试用 token support，避免改业务实现或新增测试专用生产代码。

## 重复代码检查结果

- 本轮没有复制 buyer 测试。
- seller 测试代理复用同一个 `RecordingSellerMapper` / `RecordingPortalTokenSupport`，没有为每个断言重新写一套 mock。

## 残留问题

- `lock_status` / `lock_reason` / 解锁账号仍未设计和落地，后续需要单独 DDL/后端/前端方案。
- 主账号唯一性目前仍主要依赖 `createOwnerAccountIfNeeded` 和前端禁选 OWNER；是否需要数据库唯一约束或后端禁止第二个 OWNER，需单独讨论。
- buyer 账号生命周期测试尚未复制。
