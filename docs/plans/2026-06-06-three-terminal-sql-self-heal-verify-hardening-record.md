# 2026-06-06 三端 SQL 自愈与验证入口 P0/P1 收口记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

本轮范围：继续按三端独立方向推进，只处理 P0/P1。重点收口 direct-login ticket 增量脚本、OWNER 生成列约束、standalone 管理端菜单 seed、`verify:three-terminal` 空跑风险。不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 新增问题

- `20260604_portal_direct_login_ticket.sql` 只有 `create table if not exists`，对历史半成品表缺少列/索引自愈。
- OWNER 唯一约束脚本原先只判断列名/索引名存在，不能发现 `owner_unique_*` 生成列表达式被改错。
- `20260606_admin_partner_page_direct_login_seed.sql` 的中文和引号存在损坏风险，standalone 回放时可能出现 SQL 语法或菜单名称异常。
- `verify-three-terminal.mjs` 清单包含已不存在的测试类，配合 `surefire.failIfNoSpecifiedTests=false` 有静默漏跑风险。

## 已修复问题

- `portal_direct_login_ticket` 增量脚本增加 `add_column_if_missing`、`add_index_if_missing`、`assert_column_exists`，可补齐历史表缺失列和索引。
- OWNER 唯一约束脚本增加 `assert_owner_generated_column`，校验 `generation_expression` 和 `STORED GENERATED`，避免只看列名导致误判。
- `20260604_three_terminal_isolation_migration.sql` 同步加入 OWNER 生成列表达式校验。
- standalone 菜单 seed 改为合法 UTF-8 中文，并补齐 `2010 -> 2011/2012 -> 2205/2215` 菜单父链。
- `PortalDirectLoginTicketMapper.xml` 的 `markPortalDirectLoginTicketExpired` 增加 `used_time is null`，避免已使用 ticket 被误标过期。
- 新增 `PortalDirectLoginTicketSqlContractTest`，锁住 direct-login ticket SQL 自愈和 mapper 一次性/过期边界。
- 新增 `StandalonePartnerSeedMenuContractTest`，解析 standalone seed，校验菜单父链、权限点和中文名称不可含 `?`。
- `TerminalSqlIsolationContractTest` 增加 OWNER 生成列表达式级别校验。
- `verify-three-terminal.mjs` 改为先检查测试类源码存在，Maven 执行后再检查 surefire report，避免测试类不存在时假通过。

## 残留问题

- 本轮没有执行远程数据库 DDL/DML；SQL 文件已经加固，但是否回放到远程库需要单独按数据库变更确认流程执行。
- 本轮没有启动浏览器，没有做截图、DOM 或 UI 视觉验收。
- 三端物理前端拆分仍未开始，当前仍以 `react-ui/` 作为管理端验证入口。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalSqlIsolationContractTest,PortalDirectLoginTicketSqlContractTest,StandalonePartnerSeedMenuContractTest" test`
  - 通过：`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`
  - 通过：无语法错误输出
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过：前端 4 个 guard、`tsc`、后端三端合同测试全部通过，最终输出 `three-terminal verification passed.`

## 未验证原因

- 未做浏览器运行态验收：用户已明确当前无需浏览器截图/DOM 检测确认。
- 未执行远程 DDL/DML：本轮改动主要是脚本与静态契约加固，数据库实际回放属于独立变更动作。

## 权限检查结果

- `verify:three-terminal` 后端合同测试已覆盖 seller/buyer 管理端权限、direct-login 权限、账号控制 UI 权限和三端路由归属。
- standalone seed 新增测试确认 `seller:admin:list`、`buyer:admin:list`、`seller:admin:directLogin`、`buyer:admin:directLogin` 的菜单父链闭合。

## 字典/选项复用检查结果

- 本轮没有新增字典或业务选项。

## 复用台账检查结果

- 本轮没有新增可复用业务组件、前端 option catalog 或后端公共 Service；主要是 SQL/mapper guard 和测试契约加固。

## 大文件合理性判断结果

- 新增测试类职责单一，未触发 300 行以上拆分判断。
- `verify-three-terminal.mjs` 增加测试清单和 surefire report 检查，仍保持单一脚本职责。

## 重复代码检查结果

- direct-login ticket SQL 契约独立成 `PortalDirectLoginTicketSqlContractTest`，避免继续堆进已有 direct-login 权限测试。
- standalone seed 菜单解析独立成 `StandalonePartnerSeedMenuContractTest`，后续同类 seed 可复用其解析方式。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 5 changed files`，`Added: 2, Modified: 3 - 104 nodes`。
