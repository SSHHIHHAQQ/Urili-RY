# 2026-06-06 端内操作日志写入路由测试记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本轮只处理一类问题：端内 `@PortalLog` 生成的操作日志必须通过 `PortalOperLogServiceImpl` 按 terminal 写入 seller/buyer 各自端内操作日志表，不能回落到若依管理端 `sys_oper_log`。本轮不改生产代码、不改前端、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- 新增 `PortalOperLogServiceImplTest`，固定 seller terminal 只调用 `insertSellerOperLog(...)`，不调用 buyer 写入。
- 同一测试按 buyer 模板固定 buyer terminal 只调用 `insertBuyerOperLog(...)`，不调用 seller 写入。
- 新增未知 terminal 负向用例：非 seller/buyer terminal 必须抛出 `ServiceException`，且不写任何端内操作日志。
- 新增 service 依赖负向守卫：`PortalOperLogServiceImpl` 不得持有 `ISysOperLogService` 或 `SysOperLog` 相关依赖。

## 新增问题

- 无新增已知问题。

## 残留问题

- 本轮只验证 service 层 terminal 路由，不重新做 HTTP smoke 或 AOP 异步任务验收。
- 后续真实端内业务接口仍需要继续按端入口模板使用 `@PortalPreAuthorize` 和 `@PortalLog`。

## 权限检查结果

- 本轮不新增权限点、不新增菜单、不改 `seller_menu` / `buyer_menu` / `sys_menu`。
- 已有 `TerminalRouteOwnershipTest` 继续守住受保护端内 handler 必须声明 `@PortalLog(terminal = "...")`。

## 字典/选项复用检查结果

- 不涉及字典、选项或 code/label 变更。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 `PortalOperLogService` 端内写入路由复用规则。

## 数据源与 SQL 检查

- 本轮未读取 `.env.local`。
- 本轮未连接远程 MySQL / Redis。
- 本轮未执行 DDL/DML。
- 本轮不需要新增 SQL 或 seed。

## 大文件合理性判断结果

- `PortalOperLogServiceImplTest`：151 行，未触发 300 行判断阈值。

## 重复代码检查结果

- seller/buyer 写入路由在同一个 service 测试文件内用镜像断言覆盖；没有抽象测试基类，避免为两个分支引入额外复杂度。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -am "-Dtest=PortalOperLogServiceImplTest,TerminalRouteOwnershipTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

cd E:\Urili-Ruoyi
rg -n "[ \t]+$" RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/impl/PortalOperLogServiceImplTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-06-portal-oper-log-service-routing-record.md
git diff --check -- docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md
codegraph sync .
```

## 验证结果

- `PortalOperLogServiceImplTest` + `TerminalRouteOwnershipTest`：通过，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。
- 尾随空白检查已执行，未发现匹配。
- `git diff --check -- <本轮触碰的已跟踪文档文件>`：通过，仅有 LF/CRLF 工作区换行提示。

## 未验证原因

- 未做 HTTP smoke：本轮只补 service 层 terminal 路由测试，未改生产接口行为。
- 未做前端验证：本轮不改前端。
- 未做数据库验证：本轮不改 SQL 或远程数据。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 同步 `1 changed files`。
