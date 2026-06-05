# 三端账号权限 sys_* 隔离测试记录

## 背景

当前开发方向以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准。管理端保留若依 `sys_*` 后台控制面；卖家端和买家端账号、角色、菜单、部门、日志和会话必须使用各自端内表，不能重新混用 `sys_user`、`sys_role`、`sys_menu` 或 `sys_dept`。

本轮继续按“卖家先形成模板，买家只替换配置和 service”的节奏推进，但这次不新增业务接口、不执行 SQL，只把已经确认的账号权限隔离边界固化成自动化测试。

## 本次改动

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalAccountIsolationTest.java`。
- 测试扫描 `seller`、`buyer` 模块的 `src/main/java` 和 `src/main/resources`。
- 若端模块源码或 mapper XML 中出现 `sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`sys_user_role`、`sys_role_menu`、`SysUser`、`SysRole`、`SysMenu`、`SysDept`、`PortalAccountSupport`、`PortalAccountMapper` 或 `seller_account.user_id` / `buyer_account.user_id`，测试失败。

## 设计判断

- 管理端继续可以使用若依 `sys_*`，因此本测试不扫描 `ruoyi-system`、`ruoyi-admin` 或管理端 mapper。
- seller/buyer 端管理接口可以由管理端调用，但它们维护的是端内账号权限表，不应重新 join 或依赖管理端 `sys_*` 账号权限表。
- 本测试只证明没有明显回退到 `sys_*` 控制面；不替代端内具体业务权限、数据范围、字段脱敏和接口烟测。

## 验证结果

- `cd RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalAccountIsolationTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`。
- 新增测试文件 93 行，未触发 300 行文件大小判断阈值。
- `git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalAccountIsolationTest.java RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java docs/plans/2026-06-05-terminal-account-isolation-test-record.md docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`：通过，仅有 LF/CRLF 提示。
- `codegraph sync .`：通过；代码变更同步时输出 `Synced 1 changed files`，记录补充后最终复跑输出 `Already up to date`。

## 风险与剩余事项

- 本测试不扫描 `RuoYi-Vue/sql`，因为管理端菜单 seed 和若依后台初始化本身会合法使用 `sys_menu` 等后台表。
- 如果后续把 seller/buyer 端账号权限逻辑迁到新的 terminal 子模块或资源目录，需要把新路径纳入测试扫描。
- 真实业务接口的数据范围仍需逐接口验证，本测试只证明端账号权限控制面没有明显回退到管理端 `sys_*`。
