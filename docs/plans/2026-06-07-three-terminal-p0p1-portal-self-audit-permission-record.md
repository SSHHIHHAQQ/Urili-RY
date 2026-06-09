# 2026-06-07 三端 P0/P1：Portal 自助审计权限收口记录

## 参考方向

- 参考方案：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 当前模式：只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。
- 明确不做：浏览器运行态验收、截图、DOM 检测、UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：当前规则：子 Agent 优先使用 GPT-5.3 Codex；不可用、额度限制或上下文失败时降级 `gpt-5.4`。
- 本检查点采纳上一轮 6 个 `gpt-5.4` 只读子 Agent 的 P1 扫描结果，所有子 Agent 已关闭。
- 已采纳并收口：seller/buyer 端内 `/account/login-logs`、`/account/oper-logs`、`/account/sessions` 不能只校验 terminal，还必须具备端内细粒度权限。
- 继续记录为残留 P1：余额/充值仍是占位口径、部分旧 DDL 可重放性、`sys_menu` 旧 seed slot/signature guard、部门/角色隔离契约测试。

## 已落地内容

- `SellerPortalController` 的自助登录日志、操作日志、会话列表接口已增加端内权限：
  - `seller:account:loginLog:list`
  - `seller:account:operLog:list`
  - `seller:account:session:list`
- `BuyerPortalController` 的自助登录日志、操作日志、会话列表接口已增加端内权限：
  - `buyer:account:loginLog:list`
  - `buyer:account:operLog:list`
  - `buyer:account:session:list`
- `seller_buyer_management_seed.sql` 已包含上述权限，并授予端内 OWNER 角色。
- 新增可单独执行的权限补丁脚本：`RuoYi-Vue/sql/20260607_portal_self_audit_permission_seed.sql`。
- `SqlExecutionGuardContractTest` 已覆盖该补丁脚本的显式确认 guard。
- `TerminalSeedPermissionContractTest` 已覆盖 seller/buyer 综合 seed 中的自助审计权限。
- `PortalAnonymousEndpointContractTest` 已覆盖匿名例外接口上的自助审计权限契约。

## 远程库执行与核验

数据源确认：

- `application.yml` 当前激活 profile 为 `druid`。
- `application-druid.yml` 使用 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD` 注入数据库连接。
- `.env.local` 中已确认存在 `RUOYI_DB_*` 和 `RUOYI_REDIS_*` 必要变量，未输出明文值。

执行记录：

- 已通过 JDBC 执行 `20260607_portal_self_audit_permission_seed.sql`；本次执行影响 `18` 行。
- 执行和只读复核均未输出 `.env.local` 明文。
- PowerShell 管道调用 `jshell` 时首行存在 BOM 解析噪声，不影响后续 JDBC 查询结果。

只读核验结果：

```text
seller_menu_self_audit=3
buyer_menu_self_audit=3
seller_owner_self_audit_grants=9
buyer_owner_self_audit_grants=3
```

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalAnonymousEndpointContractTest,TerminalSeedPermissionContractTest,SqlExecutionGuardContractTest" test`：通过，`9` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 `3` 个 suite / `9` 个测试通过，后端三端契约 ruoyi-system `99`、ruoyi-framework `15`、product `1`、seller `72`、buyer `73` 个测试通过。
- 数据库只读核验：通过，seller/buyer 自助审计权限和 OWNER 授权均存在。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；首次结果为 `Synced 5 changed files`，`Modified: 5 - 251 nodes in 1.2s`。

## 边界说明

- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 本检查点未做浏览器、截图、DOM 或 UI 细调验收。
- `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 是本轮开始前已存在的本地脏改，本检查点未处理。
- `.codegraph/` 是本机索引目录，不作为业务代码或迁移产物提交。

## 残留 P1

- 余额/充值仍是占位口径，不能作为真实财务语义；真正余额应进入 finance 读模型或聚合口径。
- 旧 DDL 脚本已补 fail-closed guard，但部分脚本仍不是完全可重放 DDL。
- `sys_menu` 旧 seed 的 slot/signature guard 仍需逐步补齐。
- 部门树跨主体写入/删除、角色菜单 `checkedKeys` 主体隔离、OWNER 角色禁停用/禁删除仍需补运行时隔离契约测试。
