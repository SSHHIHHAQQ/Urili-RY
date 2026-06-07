# 三端 P0/P1 快速推进：密码列默认值与 inventory 合同记录

日期：2026-06-07

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行

- 先按最新规则尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；平台返回用量限制，提示需等到 `2026-06-08 01:14/01:15` 后再试，失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 采纳的 P1：
  - `seller_account.password` / `buyer_account.password` 仍存在 `not null default ''` schema 通道，可能让遗漏密码的插入静默落为空密文。
  - `inventory` 管理端路由和权限只被 reactor compile 覆盖，未进入三端 backend contract manifest。
- 未发现新的确定 P0；seller/buyer 管理端权限、portal 自助接口、token/401 串端、账号/角色/菜单/部门/日志/session 对称性扫描未发现新的 P0/P1。

## 已完成

- `RuoYi-Vue/sql/seller_buyer_management_seed.sql` 中 seller/buyer 端账号 `password` 列去掉 `default ''`，保留 `varchar(100) not null comment '密码密文'`。
- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql` 中账号身份列定义收敛的期望默认值改为 `cast(null as char)`，动态 `MODIFY` 改为无默认值，并同步 fresh create 定义。
- `SqlExecutionGuardContractTest` 增加 `terminalAccountPasswordColumnsMustNotDefaultToBlank()`，固定 seed 和迁移脚本不得恢复空串默认值。
- 新增 `InventoryAdminRouteContractTest`，固定：
  - 后端 controller 使用 `/inventory/admin/overview`；
  - 后端权限为 `inventory:overview:list/query/adjust/ledger`；
  - 禁止 anonymous、portal、seller、buyer 权限面混入；
  - React service 使用 `/api/inventory/admin/overview`；
  - React 页面库存调整入口由 `inventory:overview:adjust` 控制。
- `react-ui/tests/three-terminal.manifest.json` 登记 `InventoryAdminRouteContractTest`。
- 已更新 `AGENTS.md`、`docs/architecture/reuse-ledger.md` 和三端控制计划。

## 远端数据库执行记录

- 配置来源：
  - 后端 `application.yml` 激活 `druid`。
  - `application-druid.yml` 通过 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD` 读取数据库连接。
  - 本次从本机 `.env.local` 读取上述变量；密码、token 和具体连接串均未输出。
- 目标环境：
  - JDBC 当前库：`fenxiao`。
  - 主机类型：remote，不是 `localhost` / `127.0.0.1` / `::1`。
- 执行前只读预检：
  - `seller_account.password`：`varchar(100)`，`nullable=NO`，`default=<EMPTY>`，空密码行数 `0`。
  - `buyer_account.password`：`varchar(100)`，`nullable=NO`，`default=<EMPTY>`，空密码行数 `0`。
- 执行的 DDL：
  - `alter table seller_account modify password varchar(100) not null comment '密码密文'`
  - `alter table buyer_account modify password varchar(100) not null comment '密码密文'`
- 编码修正：
  - 首次 DDL 经 PowerShell + JShell 输入时列注释被写成 `????`，`hex(column_comment)=3F3F3F3F`。
  - 已立即用 Java Unicode escape 重放同等 DDL 修正注释。
- 执行后验证：
  - `seller_account.password`：`nullable=NO`，`default=<NULL>`，`hex(column_comment)=E5AF86E7A081E5AF86E69687`，空密码行数 `0`。
  - `buyer_account.password`：`nullable=NO`，`default=<NULL>`，`hex(column_comment)=E5AF86E7A081E5AF86E69687`，空密码行数 `0`。
- 影响范围：
  - 只修改 `seller_account.password` 和 `buyer_account.password` 两列的 schema 默认值与注释。
  - 未执行 DML，未改任何账号数据行。
  - 未读取或写入 Redis。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，38 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=InventoryAdminRouteContractTest,SqlExecutionGuardContractTest" test`：通过，39 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个 guard 通过，React typecheck 通过，前端 7 个 Jest suite / 34 个测试通过，后端 reactor `test-compile` 通过，后端三端合同测试通过：`ruoyi-system` 156、`ruoyi-framework` 15、`integration` 5、`product` 8、`seller` 96、`buyer` 97 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" ...`：无输出，本轮触碰文件未发现尾随空白。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 6 个变更文件，Added 1、Modified 5，共 171 个节点。

## 边界说明

- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 运行态 403/200 对照、按钮显隐和浏览器菜单缓存验证仍不是当前快速推进阻塞项。
