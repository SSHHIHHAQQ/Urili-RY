# 三端 P0/P1 免密登录日志结构化审计收口记录

记录时间：2026-06-07 00:48 +08:00

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 本轮范围

继续按三端独立方向推进：管理端保留若依 `sys_*` 后台体系，卖家端和买家端继续使用独立账号、角色、菜单、部门、日志和会话体系。

本轮只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调；代码和契约收口后，已在后续追加回放 `20260607_terminal_login_log_direct_login_audit.sql` 到当前远程运行库；未读取或写入 Redis，未启动或重启后端。

## 子 Agent 执行情况

- 用户最新指定：子 Agent 优先使用 GPT-5.3 Codex；不可用时使用 `gpt-5.4`。
- 本轮延续上一批子 Agent 结论：GPT-5.3 Codex 因平台用量或可用性限制不可用后，实际降级使用 6 个 `gpt-5.4` 子 Agent 做只读审计。
- 本轮采纳并修复的 P0/P1：端内登录日志缺少免密结构化审计字段、免密失败路径缺少 acting admin/ticket/reason 结构化落库、前端登录日志类型漂移、SQL guard 未覆盖新增补丁。
- 本轮所有有效子 Agent 已关闭；一个未完成初始化的子 Agent 也已关闭。

## 新增问题

- `seller_login_log` / `buyer_login_log` 原先没有承接 `direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`。
- `PortalLoginLog` 虽已有 direct-login 字段，但 seller/buyer mapper、fresh seed 和增量 SQL 没有形成完整落库闭环。
- 普通 `buildLoginLog(...)` 对 `directLogin` 没有显式写 `false`，在 MyBatis 显式插入 `NULL` 时会绕过数据库 `default 0`。
- 免密登录成功和已解析 ticket 后的失败日志原先只能靠 `msg` 文本追踪 acting admin/ticket/reason，缺少结构化字段。
- `react-ui` 的 `PortalLoginLog` 类型缺少 direct-login 字段，`guard:partner-management` 也没有固定该类型契约。

## 已修复问题

- `PortalTokenSupport`：
  - 普通登录日志显式写 `directLogin=false`。
  - 新增从 `PortalLoginSession` 构建 direct-login 登录日志的重载。
  - direct-login 审计字段统一通过 `buildDirectLoginLog(...)` 复制。
- `SellerServiceImpl` / `BuyerServiceImpl`：
  - 免密登录成功日志改为写结构化 direct-login 字段。
  - 已解析 ticket 后的业务失败日志改为写结构化 direct-login 字段。
- `SellerMapper.xml` / `BuyerMapper.xml`：
  - 登录日志 resultMap、insert 和 list 查询投影补齐 direct-login 审计字段。
- SQL：
  - `20260604_three_terminal_isolation_migration.sql` 和 `seller_buyer_management_seed.sql` 补齐 fresh/init 场景下的登录日志审计字段。
  - 新增 `20260607_terminal_login_log_direct_login_audit.sql`，用于对既有运行库补齐 seller/buyer login log 审计字段，并带运行时确认 guard。
- 测试与 guard：
  - `PortalDirectLoginSupportTest` 补 consume validator 失败时可审计 payload 的契约。
  - `PortalDirectLoginAuthContractTest` 补登录日志字段、mapper、migration、seed 和新 SQL 补丁契约。
  - `TerminalSqlIsolationContractTest` 补 seller/buyer 登录日志 direct-login 审计字段契约。
  - `SqlExecutionGuardContractTest` 纳入新增 SQL 补丁。
  - seller/buyer service 单测补 direct-login 成功和失败日志结构化字段断言。
  - `check-partner-management-template.mjs` 补 `PortalLoginLog` direct-login 字段契约。
- 前端类型：
  - `react-ui/src/types/seller-buyer/party.d.ts` 的 `PortalLoginLog` 补 direct-login 审计字段。

## 验证命令

已通过：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn "-pl" "ruoyi-system,seller,buyer" "-Dtest=PortalDirectLoginSupportTest,PortalDirectLoginAuthContractTest,TerminalSqlIsolationContractTest,SqlExecutionGuardContractTest,SellerServiceImplTest,BuyerServiceImplTest" test`
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`

说明：

- 第一次执行 `npm run verify:three-terminal` 时，buyer Surefire fork 报过一次 `NoClassDefFoundError: com/ruoyi/buyer/domain/Buyer`。随后确认 `Buyer.class` 存在，单独不带 `-am` 跑 buyer 会因为本地 reactor 依赖陈旧出现 `NoSuchMethodError`；改用带 `-am` 的 reactor 刷新依赖后通过。
- 重新执行 `npm run verify:three-terminal` 已整体通过，最终输出 `three-terminal verification passed.`。
- 最终三端验证覆盖：portal token guard、partner management guard、seller/buyer portal product guards、TypeScript 编译、前端 Jest 3 个 suite / 9 个 test、后端 `ruoyi-system` 96 个测试、`ruoyi-framework` 15 个测试、`product` 1 个测试、`seller` 69 个测试、`buyer` 70 个测试。

## 远程库执行

- 已追加执行远程 MySQL DDL：`RuoYi-Vue/sql/20260607_terminal_login_log_direct_login_audit.sql`。
- 执行前缺少 10 个字段：seller/buyer login log 各缺少 `direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`。
- 执行后缺少字段：`[]`。
- 执行记录：`docs/plans/2026-06-07-terminal-login-log-direct-login-audit-db-execution-record.md`。

## 未验证原因

- 未读写 Redis：本轮只做代码级和契约验证。
- 未启动或重启后端：用户当前指定快速 P0/P1 收口，且本轮不做运行态浏览器验收。
- 未做浏览器、截图、DOM 或 UI 细调验收：用户已明确本阶段不需要。

## 权限检查结果

- 本轮没有新增后端管理端权限点。
- `seller:admin:directLogin` / `buyer:admin:directLogin` 和 `*:admin:ticket:list` 的既有权限边界未改。
- 新增 SQL 补丁带运行时确认 guard，并已纳入 `SqlExecutionGuardContractTest`。
- 登录日志结构化字段只增强审计投影，不改变端内权限判断。

## 字典/选项复用检查结果

- 本轮未新增字典项。
- 本轮未新增业务下拉选项。
- direct-login 字段属于布尔和审计 ID/名称/原因，不进入 `sys_dict`。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`。
- 复用规则已登记：普通登录日志必须显式 `directLogin=false`；免密成功和已解析 ticket 后的失败日志必须走 `buildDirectLoginLog(...)`；seller/buyer login log 必须保留 direct-login 审计字段。

## CodeGraph 更新结果

- `codegraph sync .`：通过。
- 结果：`Synced 2 changed files`，`Modified: 2 - 199 nodes in 911ms`。
- 回填记录后复跑：`Already up to date`。

## 大文件合理性判断

- `SellerServiceImpl` / `BuyerServiceImpl` 是既有大文件，本轮只在登录/免密登录日志路径做窄范围同构修改，没有扩大职责。
- seller/buyer service 测试文件是既有大文件，本轮只补同一失败矩阵的审计断言，避免新增分散测试类导致 `verify-three-terminal` 白名单维护成本上升。
- 新增 SQL 补丁职责单一，只处理 seller/buyer login log direct-login 审计字段。

## 重复代码检查结果

- seller/buyer 按用户确认的“卖家模板通过后机械复制买家”方式保持同构。
- 跨端公共逻辑继续收口在 `PortalTokenSupport` / `PortalDirectLoginSupport` / guard 脚本，不新建第三套端内实现。
- 本轮未把 seller/buyer 端内账号权限混入若依 `sys_user` / `sys_role` / `sys_menu`。

## 残留问题

P2 记录，不阻塞本轮：

- 新增 SQL 补丁已执行到当前远程运行库；后续如果切换其他环境，需要重新按该环境的数据源确认后回放。
- 登录日志管理端 UI 目前只补类型和 guard，没有新增直登字段展示列；后续如果要展示，优先放在登录日志详情展开区，避免挤压表格列宽。
- 强制踢出如果要记录原因和执行人，需要后续补 DDL 和审计字段。
- seller/buyer session 的 `login_location`、`browser`、`os` 等设备字段仍是后续 DDL 与写入链路增强项。

## AGENTS 影响

- `AGENTS.md` 已有子 Agent 模型优先级规则：优先 GPT-5.3 Codex，不可用再降级 `gpt-5.4`，并要求关闭子 Agent 和写入 Markdown 检查点。
- 本轮不需要新增 AGENTS 规则；具体实现模板已写入复用台账。
