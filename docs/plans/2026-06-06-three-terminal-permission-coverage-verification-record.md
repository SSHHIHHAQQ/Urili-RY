# 2026-06-06 三端管理权限覆盖验证记录

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向。

当前进入快速推进模式：只处理 P0/P1，也就是编译、guard、接口、权限、串端、service/字段缺失等问题；不做浏览器截图、DOM 检测或 UI 细调。

上一检查点已将 `RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql` 执行到当前远程运行库，补齐管理端 seller/buyer 页面基础菜单与免密登录权限。本轮继续收口一个运行态可用性风险：当前管理端代码实际使用的 seller/buyer 管理权限点，是否都已经存在于远程运行库 `sys_menu`。

## 数据源确认

- 后端当前激活配置为 `druid`。
- 本轮从本机 `.env.local` 读取 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD` 注入当前进程，仅用于 JDBC 只读验证。
- 本轮记录不输出 JDBC URL、数据库账号、数据库密码、Redis 地址、Redis 密码或 token secret。
- 本轮没有读取或写入 Redis。

## 子 Agent 执行情况

- 按用户要求优先使用 `gpt-5.3-codex-spark` 启动 6 个只读 explorer 子 Agent。
- 子 Agent 切片覆盖：seller/buyer 后端控制面、portal 会话/日志链路、前端同构模板、SQL/seed、product/warehouse 边界、验证脚本与合同测试。
- 主线程两次等待均超时，未收到可用审计结论。
- 为避免阻塞主线，已关闭 6 个子 Agent；关闭前状态均为 `running`，随后均收到 `shutdown` 通知。
- 本轮有效结论以主线程当前验证和远程库只读核验为准。

## 已完成验证

### 三端最小门禁

执行命令：

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run verify:three-terminal
```

验证结果：

- `guard:portal-token`：通过，输出 `Portal token isolation guard passed.`。
- `guard:partner-management`：通过，输出 `Partner management template guard passed.`。
- `guard:seller-portal-product`：通过，输出 `Seller portal product template guard passed.`。
- `guard:buyer-portal-product`：通过，输出 `Buyer portal product template guard passed.`。
- `tsc --noEmit --pretty false`：通过。
- 后端 `ruoyi-system,ruoyi-framework,seller,buyer` 三端合同测试通过。
- Maven 汇总结果：`BUILD SUCCESS`。
- 最终输出：`three-terminal verification passed.`。

后端合同测试覆盖的关键测试包括：

- `AdminDirectLoginPermissionContractTest`
- `AdminAccountPermissionUiContractTest`
- `SellerAdminPermissionContractTest`
- `BuyerAdminPermissionContractTest`
- `TerminalRouteOwnershipTest`
- `TerminalAccountIsolationTest`
- `TerminalSeedPermissionContractTest`
- `TerminalSqlIsolationContractTest`
- `PortalLoginResultTest`
- `PortalDirectLoginResultTest`
- `PortalDirectLoginTicketTest`
- `PortalHomeProfileSerializationTest`
- `PortalPreAuthorizeAspectTest`
- `LogAspectSensitiveFieldFilterTest`
- `PortalPermissionCheckerTest`
- `TokenServiceTerminalIsolationTest`
- `SellerServiceImplTest`
- `BuyerServiceImplTest`

### 远程库权限覆盖只读核验

验证方式：

- 使用 Maven 本地缓存的 `mysql-connector-j` JDBC 驱动。
- 生成临时 Java 类，通过 JDBC 查询当前远程运行库 `sys_menu`。
- 查询前执行 `set names utf8mb4`。
- 查询内容只读，不执行 DDL/DML。

首次尝试说明：

- 第一次使用 `jshell` 脚本执行读库比对时，脚本文件带 UTF-8 BOM，导致 `import java.sql.*` 未被正确解析。
- 该次输出的 `missing=66` 是无效结果，已丢弃，不作为证据。
- 最终有效证据来自后续临时 Java 类执行结果。

有效读库结果：

- 当前代码涉及的 seller/buyer 管理端权限点总数：`66`。
- 远程库 `sys_menu` 命中权限点数：`66`。
- 缺失权限点数：`0`。
- 命中行均为 `status=0`。
- 命中行均为 `visible=0`。

覆盖范围包括：

- seller/buyer 页面基础权限：`*:admin:list`、`*:admin:query`、`*:admin:add`、`*:admin:edit`、`*:admin:changeStatus`、`*:admin:resetPwd`。
- seller/buyer 端内账号权限：`*:admin:account:list/add/edit/lock/resetPwd/role:query/role:edit`。
- seller/buyer 端内菜单权限：`*:admin:menu:list/query/add/edit/remove`。
- seller/buyer 端内角色权限：`*:admin:role:list/query/add/edit/remove`。
- seller/buyer 端内部门权限：`*:admin:dept:list/query/add/edit/remove`。
- seller/buyer 会话、免密和审计权限：`*:admin:forceLogout`、`*:admin:directLogin`、`*:admin:loginLog:list`、`*:admin:operLog:list`、`*:admin:ticket:list`。

## 当前判断

- 当前三端最小门禁通过，未发现新增编译、guard 或合同测试 P0/P1。
- 当前远程运行库 `sys_menu` 已覆盖管理端代码实际使用的 66 个 seller/buyer 管理权限点。
- 上一轮只验证 4 个 standalone seed 权限点；本轮已扩展为当前管理端代码用到的 seller/buyer 管理权限点全量覆盖。
- 本轮没有新增代码改动，因此没有新增复用台账项。

## 边界说明

- 本轮未修改业务代码。
- 本轮未修改 SQL 文件。
- 本轮未执行远程数据库 DDL/DML。
- 本轮未写 `sys_role_menu`。
- 本轮未读取或写入 Redis。
- 本轮未重启后端。
- 本轮未做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run verify:three-terminal
```

```powershell
cd E:\Urili-Ruoyi
# 通过临时 Java 类 + mysql-connector-j 执行远程库 sys_menu 只读权限覆盖查询
```

## CodeGraph

`cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。
