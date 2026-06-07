# 三端 P0/P1 端内操作日志免密代入结构化审计记录

日期：2026-06-07

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只处理 P0/P1：字段缺失、接口链路、权限/审计 guard 和契约测试。本轮收口 `seller_oper_log` / `buyer_oper_log` 免密代入审计只能写 `oper_param` 文本前缀的问题。

## 已完成

- `PortalOperLog` 增加 `directLogin`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason`。
- `PortalLogAspect` 和 `PortalPreAuthorizeAspect` 在端内业务操作、鉴权失败日志中写入结构化 direct-login 审计字段，并保留 `directLoginAudit{...}` 参数前缀作为兼容信息。
- `PortalOperLogMapper.xml` 的 seller/buyer 写入 SQL 增加 5 个结构化字段。
- `SellerMapper.xml` / `BuyerMapper.xml` 的 `PortalOperLog` resultMap 和操作日志列表查询增加 5 个结构化字段，并支持最小筛选参数透传。
- `20260604_three_terminal_isolation_migration.sql`、`seller_buyer_management_seed.sql` 增加端内操作日志字段基线。
- 新增 guarded/idempotent 补丁 `20260607_terminal_oper_log_direct_login_audit.sql`，用于后续确认后补运行库。
- 补强 `TerminalSqlIsolationContractTest`、`SqlExecutionGuardContractTest`、`PortalDirectLoginAuthContractTest`、`PortalLogAspectContractTest`。
- 同步 `AGENTS.md`、`docs/architecture/reuse-ledger.md`、三端隔离方向文档。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=TerminalSqlIsolationContractTest,SqlExecutionGuardContractTest,PortalDirectLoginAuthContractTest,PortalLogAspectContractTest,PortalOperLogServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：通过。`ruoyi-system` 相关 `38` 个测试通过，`ruoyi-framework` 相关 `1` 个测试通过，Reactor `BUILD SUCCESS`。

```powershell
cd E:\Urili-Ruoyi
git diff --check
```

结果：通过。仅输出既有 LF/CRLF 换行提示，无 whitespace 错误。

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：通过。首次同步输出 `Synced 10 changed files`，`Modified: 10 - 352 nodes in 1.1s`；记录回填后复跑输出 `Already up to date`。

## 未执行项

- 未连接远程 MySQL / Redis。
- 未执行 `20260607_terminal_oper_log_direct_login_audit.sql`。
- 未启动后端或前端。
- 未做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 残留问题

- 管理端操作日志弹窗展示 direct-login 结构化字段属于 P2，本轮不阻塞。
- 本实现切片当时未执行远程库补字段；后续已确认目标数据源并补齐 `seller_oper_log` / `buyer_oper_log` direct-login 结构化审计列，见 `docs/plans/2026-06-07-terminal-oper-log-direct-login-audit-db-execution-record.md`。

## 复用与权限检查

- 复用现有 `PortalOperLog`、`@PortalLog`、`@PortalPreAuthorize`、`PortalOperLogMapper` 和 seller/buyer 同构 Mapper 模板。
- 端内日志继续按 terminal 写入 `seller_oper_log` / `buyer_oper_log`，不回落到若依 `sys_oper_log`。
- 本轮不新增权限点、不新增菜单、不修改按钮权限。

## 大文件与重复代码判断

- 本轮未新增超过 300 行的代码文件。
- seller/buyer 变更按同构模板复制，仅替换端类型和表字段前缀；未引入新的业务逻辑分叉。
