# 2026-06-07 三端 P0/P1 快速推进：前端 Guard Manifest 与端内菜单段位收口

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

本切片收口两个 P1：

- `react-ui` 三端验证入口仍有部分 guard 固化在脚本内，manifest 不是唯一清单来源。
- `seller_menu` / `buyer_menu` seed 只校验 `AUTO_INCREMENT` 下界，未校验上界，存在新菜单落入对端保留 ID 段的风险。

## 子 Agent 执行情况

- 先按用户最新要求尝试 6 个 `gpt-5.3-codex-spark` 只读子 Agent；平台返回用量限制，需等到 2026-06-08 01:14/01:15 后再试，失败 Agent 已关闭。
- 按 fallback 规则使用 6 个 `gpt-5.4` 只读子 Agent。
- 已返回结论：
  - 前端 service URL、JS/TS 镜像、direct-login/session/audit、seller/buyer 后端账号权限、自助/管理端审计 DTO：未发现 P0/P1。
  - SQL/seed guard：发现 2 个 P1，已采纳。
- 两个只读子 Agent 生成了审查记录：
  - `docs/reviews/2026-06-07-react-ui-three-terminal-service-url-readonly-scan.md`
  - `docs/reviews/2026-06-07-three-terminal-direct-login-session-audit-p0p1-readonly-scan.md`

## 已完成

- `react-ui/tests/three-terminal.manifest.json` 新增 `frontendGuardScripts`，集中登记三端前端 guard。
- `react-ui/scripts/verify-three-terminal.mjs` 改为从 manifest 读取前端 guard 脚本，并校验脚本必须存在于 `package.json`。
- `react-ui/scripts/check-partner-management-template.mjs` 增强为函数级 service URL 合同：
  - 每个 seller/buyer admin service 函数必须调用与函数名匹配的固定 URL。
  - 继续禁止 seller/buyer service 串端、调用 `/api/system/**` 或回退到默认密码重置入口。
- 以下 seed 的 `assert_terminal_menu_range_ready()` 增加 `AUTO_INCREMENT` 上界：
  - `seller_menu` 必须位于 `100000-199999`。
  - `buyer_menu` 必须位于 `200000-299999`。
  - 覆盖 `seller_buyer_management_seed.sql`、`20260604_portal_account_list_permission_seed.sql`、`20260604_portal_dept_role_list_permission_seed.sql`、`20260604_portal_product_category_permission_seed.sql`、`20260607_portal_self_audit_permission_seed.sql`、`20260604_seller_product_schema_permission_seed.sql`、`20260604_buyer_product_schema_permission_seed.sql`。
- `20260607_terminal_menu_id_range_isolation.sql` 增加迁移后最终段位校验：
  - `seller_menu` / `buyer_menu` 主键和 `parent_id` 必须进入最终端内 ID 段。
  - `seller_role_menu` / `buyer_role_menu` 菜单 ID 必须进入最终端内 ID 段。
  - 显式 `commit` 放到 `AUTO_INCREMENT` reset 和最终 guard 之后。
  - 备注：MySQL `ALTER TABLE ... AUTO_INCREMENT` 会隐式提交，脚本已在注释中明确该事务语义边界。
- `TerminalSqlIsolationContractTest` 补充合同：
  - seed 不能只校验 `AUTO_INCREMENT >= floor`，必须同时校验上界。
  - 菜单 ID 重排脚本必须包含最终段位校验，并且 reset 调用必须在显式 `commit` 前。
- 已更新 `docs/architecture/reuse-ledger.md`。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过，输出 `three-terminal manifest check passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=TerminalSqlIsolationContractTest,SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，47 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 7 个 Jest suite / 33 个测试通过；后端 `ruoyi-system` 151、`ruoyi-framework` 15、`integration` 5、`product` 6、`seller` 94、`buyer` 95 个测试通过；`ruoyi-admin -am -DskipTests test-compile` reactor 编译通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" ...`：无输出，本轮触碰文件未发现尾随空白。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮只加强 guard、合同测试和 seed fail-closed 约束，不改变三端业务数据结构。

## 当前残留项

- SQL guard 子 Agent 提到的 `ALTER TABLE ... AUTO_INCREMENT` 隐式提交无法通过 MySQL 普通事务完全回滚；当前脚本已把显式 `commit` 后移，并在 reset 前后做最终段位校验。后续如需更强 DDL 原子性，需要改为人工维护窗口或外部迁移器控制。
- direct-login 跨端失败审计、后台强退 actingAdmin 归属已有源码合同，后续可补运行态回归测试，但不作为本轮 P0/P1 阻塞项。
