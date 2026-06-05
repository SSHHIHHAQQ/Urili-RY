# 2026-06-05 端账号角色绑定服务测试守卫记录

## 目标

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按“先做卖家标准模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本轮只补服务层自动化守卫：管理端给卖家/买家端账号分配端内角色时，必须同时满足“账号属于当前主体”和“角色属于当前主体”，不能跨主体写入 `seller_account_role` / `buyer_account_role`。

## 范围

已新增：

- `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplTest.java`
- `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplTest.java`

本轮未修改：

- 不改后端接口行为。
- 不改前端 UI。
- 不新增表，不执行 SQL，不写远程库。
- 不改菜单 seed、权限 seed 或字典。

## 覆盖规则

卖家侧先形成标准测试模板：

- 正常绑定：`roleIds` 会经过 `PortalPermissionSupport.sanitizeIds(...)` 过滤空值、非正数并保序去重。
- 清空绑定：没有有效角色时，删除旧绑定并返回成功，不调用批量插入。
- 跨卖家账号：账号不属于当前 `sellerId` 时抛出 `ServiceException`，且不删除、不插入绑定。
- 跨卖家角色：角色不全部属于当前 `sellerId` 时抛出 `ServiceException`，且不删除、不插入绑定。

买家侧按卖家模板同构复制，只替换 buyer 命名、字段、mapper 和 service。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -Dtest=SellerPortalPermissionServiceImplTest test`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -Dtest=BuyerPortalPermissionServiceImplTest test`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am test`：通过；`ruoyi-system` 24 个测试、`finance` 9 个测试、`seller` 7 个测试、`buyer` 7 个测试均通过。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplTest.java docs/plans/2026-06-05-terminal-account-role-binding-service-test-record.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/architecture/reuse-ledger.md`：通过，仅有 LF/CRLF 提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 11 changed files`。

## 当前判断

- 账号角色绑定已经有 seller/buyer 两端 service 层守卫。
- 本测试不启动 Spring 容器、不连接数据库，适合作为后续重构端内权限 service 的快速回归。
- 管理端接口 `@PreAuthorize`、按钮权限、浏览器弹窗和真实数据库写入仍按各自切片单独验证。
- CodeGraph 更新结果：已执行 `codegraph sync .`，输出 `Synced 11 changed files`。
