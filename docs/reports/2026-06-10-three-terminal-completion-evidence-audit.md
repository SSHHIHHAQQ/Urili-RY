# 2026-06-10 三端隔离完成度证据审计

本记录以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按当前快速推进模式只判断 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器、截图、DOM 或 UI 细调验收。

## 子 Agent 规则

- 当前规则：后续需要并行拆分时，按任务类型选择子 Agent 模型；只读检查、审查、探索类使用 `gpt-5.3-codex-spark`，代码编辑、实现、修复类使用 `gpt-5.4`。
- 本检查点未启动新子 Agent。
- 历史已使用并关闭的子 Agent 仍按当时实际模型记录；后续如再循环使用，必须按当前规则记录任务类型、模型、数量、关闭状态和采纳结论。

## 完成度证据矩阵

| 项目 | 当前判断 | 证据 | 备注 |
| --- | --- | --- | --- |
| 管理端保留若依后台体系 | 已固定为当前方向 | `AGENTS.md` 明确管理端保留 `sys_*`；三端计划和目标追踪均以此为参考方向 | 管理端账号仍代表平台管理员，不代表卖家/买家员工 |
| 卖家/买家端账号不复用 `sys_user` | P0/P1 未见回退 | `rg` 复核 `RuoYi-Vue/seller`、`RuoYi-Vue/buyer` 未命中 `sys_user` | 当前判断为代码级静态证据，不含远端 live SQL 查询 |
| 卖家/买家端角色、菜单、部门不复用 `sys_role/sys_menu/sys_dept` | P0/P1 未见回退 | `rg` 复核 `RuoYi-Vue/seller`、`RuoYi-Vue/buyer` 未命中 `sys_role/sys_menu/sys_dept`；`TerminalSeedPermissionContractTest`、`TerminalSqlIsolationContractTest` 纳入三端总门 | 端内菜单、角色、部门仍按 seller/buyer 独立表方向 |
| 账号查询绑定主体 ID | P0/P1 未见回退 | 生产代码使用 `selectSellerAccountByIdAndSellerId` / `selectBuyerAccountByIdAndBuyerId`；`TerminalAccountIsolationTest`、`AdminDirectLoginPermissionContractTest` 纳入三端总门 | 防止裸 `accountId` 串端 |
| 管理端控制权 | P0/P1 未见回退 | `AdminSellerController` / `AdminBuyerController` 已有账号重置密码、会话列表、强制踢出等权限；前端 `partner-management-contract.test.ts` 固定 service 和权限 | 当前是控制面框架证据，不代表所有未来业务控制菜单已完成 |
| 重置密码语义 | P0/P1 未见回退 | resetPwd 路由为 `/{subjectId}/accounts/{accountId}/resetPwd`，权限为 `*:admin:account:resetPwd`；合同测试禁止旧 `*:admin:resetPwd` | 不再把“重置密码”静默退回默认密码 |
| 会话查看和强退分权 | P0/P1 未见回退 | 后端使用 `*:admin:session:list` 查看、`*:admin:forceLogout` 强退；前端合同测试固定按钮权限 | 防止只查看会话也绑定强退权限 |
| 免密代入 Redis key 和审计 | P0/P1 未见回退 | `PortalDirectLoginSupportTest` 固定 `portal_direct_login:{terminal}:{token_hash}`；相关 SQL 合同覆盖 `portal_direct_login_ticket` | 本检查点未读写 Redis |
| Portal token/session 端隔离 | P0/P1 未见回退 | `terminal-session-token.test.ts`、`portal-unauthorized-redirect.test.ts`、`PortalDirectLoginAuthContractTest`、`TokenServiceTerminalIsolationTest` 纳入总门 | 当前不做浏览器运行态验证 |
| 前端远程菜单和空权限 guard | P0/P1 未见回退 | `guard:portal-token`、`guard:partner-management`、`remote-menu-route-guard.test.ts` 纳入 `verify-three-terminal` | `.ts/.tsx` 与关键 `.js` 镜像仍由 guard 覆盖 |
| 三端总门禁 | 通过 | `npm run verify:three-terminal` 已在追加检查点通过，最终输出 `three-terminal verification passed.` | 本摘要不再重复易漂移计数；以文末最新验证检查点为准 |

## 当前不宣称完成的范围

- 三个物理前端目录 `admin-ui` / `seller-ui` / `buyer-ui` 尚未拆出；当前仍以 `react-ui/` 作为管理端验证入口并承载 seller/buyer portal 验证。
- 本检查点没有做浏览器、截图、DOM、真实 API smoke 或远端 MySQL/Redis live 查询。
- 商品、库存、订单、履约、财务、外部系统等完整业务菜单不属于当前三端账号权限框架的完成条件。
- P2 项继续记录但不阻塞：README 本地隔离示例口径、Jest 双配置歧义、未跟踪原型目录、sidecar、端内菜单库结构列级约束偏宽、源码中文乱码、integration 响应日志脱敏范围、`credentialKeyId` 信息最小化、product portal N+1 性能债和 product 审核读模型契约。

## 结论

当前没有发现新的三端隔离 P0/P1。按代码级和总门禁口径，三端独立账号权限框架仍维持约 `90%`；该百分比只覆盖账号、角色、菜单、部门、日志、会话、免密代入、权限 gate、SQL guard 和当前 `react-ui` 验证入口，不等同于三个物理前端和完整业务菜单已经完成。

## 追加检查点：6 个 gpt-5.4 子 Agent P0/P1 切片复核

时间：2026-06-10 00:50

参考方向仍为 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。本追加检查点保持快速推进模式，只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮启动 6 个子 Agent，全部显式使用 `gpt-5.4`。
- 6 个子 Agent 均已完成并关闭。
- 6 个切片分别覆盖：
  - seller/buyer 后端账号、service、mapper、portal 注解。
  - SQL、seed、migration、密码列、菜单 ID 区间和 fail-closed guard。
  - 管理端控制权后端：resetPwd、session list、forceLogout、direct-login、审计字段。
  - React 管理端 seller/buyer UI、service、权限和会话参数收口。
  - React portal token、401、redirect 白名单和 direct-login 回传。
  - `verify-three-terminal` manifest、guard、Maven reactor 和 JS mirror 覆盖。

### 子 Agent 结论合并

- 未发现新的 P0/P1。
- seller/buyer 后端模块未发现复用 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 的端内账号权限控制面。
- 账号查询仍按 `seller_id/buyer_id + account_id` 约束，未发现裸 `select*AccountById(accountId)` 生产入口。
- portal 已认证接口从 `PortalSessionContext` 推导当前主体/账号，并由 `@PortalPreAuthorize` / `@PortalLog` 覆盖；匿名登录和免密登录入口按设计只做认证入口日志。
- SQL/seed 当前保持 confirm token、`45000` fail-closed、端账号密码 `varchar(100) not null` 且无默认空串、seller/buyer 菜单 ID 区间 guard。
- 管理端查看会话与强退已分权，resetPwd 写端账号表，免密票据使用 `portal_direct_login:{terminal}:{token_hash}`，acting admin 审计为结构化字段。
- React 管理端 seller/buyer 页面和 service 未发现 URL、权限、terminal、session 参数串端。
- React portal token storage、401 清理、redirect 白名单和 direct-login 成功回传未发现串端。
- `verify-three-terminal` manifest 当前没有发现关键测试漏配；Maven 模块从 reactor 动态推导并使用 `-am`。

### 本轮主线程验证

- `rg -n "sys_user|sys_role|sys_menu|sys_dept|\buser_id\b" RuoYi-Vue\seller RuoYi-Vue\buyer -g "*.java" -g "*.xml" -g "*.sql"`：无命中。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests\partner-management-contract.test.ts tests\terminal-session-token.test.ts tests\portal-unauthorized-redirect.test.ts tests\portal-direct-login-message.test.ts tests\portal-session-request.test.ts --runInBand`：通过，5 suites / 61 tests。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest,AdminDirectLoginPermissionContractTest,PortalDirectLoginAuthContractTest,PortalLogAspectContractTest,PortalOperLogServiceImplTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，10 个 reactor 模块 BUILD SUCCESS；ruoyi-system 28 tests，seller 55 tests，buyer 55 tests。

### 本轮 P2 留存

- `react-ui/src/pages/Portal/Login/index.tsx` 的 `resolveRedirect()` 目前没有单独恶意输入单测；已有路径工具层和实现约束覆盖，记录为 P2 测试空白。
- seller/buyer product schema controller 的 `PortalLog.title` 文案存在少量中英文不一致，不影响当前 P0/P1 合同，记录为 P2 文案统一项。
- 本检查点未做 live `/getRouters`、浏览器、截图、DOM、远端 MySQL 或 Redis 运行态验证，按当前快速推进模式不阻塞。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

## 追加检查点：portal 端真实业务接口范围控制复核

时间：2026-06-10 13:13，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 处理结论

- `P0`：未发现新增。
- `P1`：未坐实新的代码缺口。
- 子 Agent：4 个只读子 Agent 均按规则使用 `gpt-5.3-codex-spark`，均因额度限制失败并已关闭；未回退到 `gpt-5.4`。
- 主线程复核：seller/buyer portal 商品接口从 `PortalSessionContext` 推导主体范围，前端 portal service 剥离 caller-controlled scope 参数，SQL/account/menu/direct-login 相关边界已有合同覆盖。

### 验证

- `mvn -pl ruoyi-system,seller,buyer,product,ruoyi-framework -am "-Dtest=TerminalRouteOwnershipTest,PortalAnonymousEndpointContractTest,PortalDirectLoginAuthContractTest,PortalProductEndpointPermissionContractTest,TerminalAccountIsolationTest,TerminalSqlIsolationContractTest,TerminalSeedPermissionContractTest,PortalLoginSessionConsistencyContractTest,PortalPreAuthorizeAspectTest,TokenServiceTerminalIsolationTest,PortalLogAspectContractTest,SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`ruoyi-system` 35 tests、`ruoyi-framework` 5 tests、`seller` 10 tests、`buyer` 11 tests。
- `npm run verify:three-terminal`：通过，前端 25 suites / 208 tests passed，后端 reactor test-compile 15 个模块 BUILD SUCCESS，后端三端合同测试 12 个模块 BUILD SUCCESS。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

## 追加检查点：三端账号权限 P0/P1 静态复核与总门禁

时间：2026-06-10 13:06，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 处理结论

- `P0`：未发现新增。
- `P1`：未坐实新的代码缺口。
- 子 Agent：本检查点未启动新的子 Agent；后续按用户最新规则执行，只读/审查/探索用 `gpt-5.3-codex-spark`，代码编辑/实现/修复用 `gpt-5.4`。
- 静态复核：未发现 seller/buyer 端内账号权限继续直接依赖若依 `sys_*`，未发现新增裸 accountId 查询入口，未发现旧免密 Redis key、默认密码重置接口或 `session:forceLogout` 命名空间。

### 验证

- `mvn -pl ruoyi-system,seller,buyer,product,ruoyi-framework -am "-Dtest=TerminalRouteOwnershipTest,TerminalAccountIsolationTest,TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest,PortalProductEndpointPermissionContractTest,PortalSelfServiceSurfaceContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminAccountPermissionUiContractTest,PortalLoginSessionConsistencyContractTest,SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,SellerPortalPermissionServiceImplMenuTreeTest,SellerPortalDeptServiceImplTest,SellerPortalProductServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplMenuTreeTest,BuyerPortalDeptServiceImplTest,BuyerPortalProductServiceImplTest,PortalPreAuthorizeAspectTest,TokenServiceTerminalIsolationTest,PortalLogAspectContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`ruoyi-system` 39 tests、`ruoyi-framework` 5 tests、`seller` 45 tests、`buyer` 46 tests。
- `npm run verify:three-terminal`：通过，前端 25 suites / 208 tests passed，后端 reactor test-compile 15 个模块 BUILD SUCCESS，后端三端合同测试 12 个模块 BUILD SUCCESS。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

## 追加检查点：模型规则切换与 logistics gate 复核

时间：2026-06-10 12:24，本机 `Asia/Shanghai`。

### 子 Agent 规则

- 当前规则已改为按任务类型选择模型：只读检查、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮先前按旧规则启动的 6 个 `gpt-5.4` 只读子 Agent 已立即关闭，结论未采纳。
- 本轮随后启动 6 个 `gpt-5.3-codex-spark` 只读子 Agent；logistics manifest 漏登风险已采纳，seller/buyer `session:forceLogout` 新权限建议已按误报记录，SQL/seed 切片因上下文压缩失败无有效结论。
- 收拢阶段子 Agent 管理接口对 6 个句柄返回 `not_found`；主线程已显式发起关闭请求，关闭请求同样返回 `not_found`。当前没有可等待或可关闭的活动子 Agent 句柄。

### 代码与合同证据

- logistics 管理端 controller 已补 `quote` 日志注解和 quote/label PII 参数排除。
- logistics React 管理端页面已把权限 guard 集中并补齐渠道、日志、系统渠道等入口权限。
- 新增 logistics 后端合同测试与前端合同测试，并登记到 `three-terminal.manifest.json`。
- `verify-three-terminal` 已覆盖 logistics 前端关键路径和后端合同模块。

### 验证证据

- `cd E:\Urili-Ruoyi\react-ui; node -e "JSON.parse(require('fs').readFileSync('tests/three-terminal.manifest.json','utf8')); console.log('manifest json ok')"`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/logistics-carrier-contract.test.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`：通过，2 suites / 27 tests。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl logistics,ruoyi-admin -am "-Dtest=LogisticsAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，15 个 reactor 模块 BUILD SUCCESS；`LogisticsAdminRouteContractTest` 2 tests。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 25 suites / 207 tests passed；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 12 个模块 BUILD SUCCESS。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，只有 CRLF 工作区提示。

### 当前审计结论

- `P0` 未见新增。
- logistics 管理端新增能力的 manifest、guard、接口权限、敏感日志排除和三端 gate 覆盖已补强。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和全部业务闭环完成；这些仍按后续阶段继续推进。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

## 追加检查点：logistics 后端关键测试路径纳入三端 gate

时间：2026-06-10 02:04

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只判断 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本轮收拢并关闭 6 个子 Agent，全部使用 `gpt-5.4`。
- 采纳 1 个 P1：`verify-three-terminal` 的关键后端测试路径兜底漏了 `logistics/src/test/java`。
- 未采纳为 P1 的结果：seller/buyer 后端端内账号权限控制面、SQL/seed guard、React portal token/401/direct-login、管理端 seller/buyer UI/service 未发现新的代码级 P0/P1。

### 修复内容

- `react-ui/scripts/verify-three-terminal.mjs`：`criticalBackendTestPathPattern` 已纳入 `logistics[\\/]src[\\/]test[\\/]java[\\/]`。
- `react-ui/tests/verify-three-terminal-backend-gate.test.ts`：补充 `logistics` 路径断言，并增加真实负例 `LogisticsDriftGuardTest.java`，确保未登记 manifest 时 fail-closed。
- `docs/plans/2026-06-09-three-terminal-p0p1-gpt54-fast-pass-record.md`：补充追加式快照读取口径，避免已关闭的历史 P1 被误读为当前残留。
- `docs/reports/2026-06-09-three-terminal-progress-code-review.md`：将会漂移的 manifest 计数改为快照口径，并明确 product portal 样板不计入当前账号权限框架完成度。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/verify-three-terminal-backend-gate.test.ts --runInBand`：通过，1 suite / 23 tests。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 24 suites / 202 tests passed；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 11 个模块 BUILD SUCCESS。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

## 追加检查点：端内 getRouters 返回契约收敛

时间：2026-06-10 01:46

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只判断 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 核对范围

- `react-ui/src/services/portal/session.ts` 的 `getPortalRouters` 返回类型。
- `SellerPortalController#getRouters`、`BuyerPortalController#getRouters`。
- `PortalPermissionSupport` 端内菜单树共享 helper。
- `PortalPermissionSupportTest`、`RouterVoPermissionContractTest`。

### 结论

- 已发现并修复 P1：端内前端 service 按若依 `GetRoutersResult` / `RouterVo` 消费，但后端 seller/buyer `/getRouters` 原先直接返回 `PortalMenu`。
- 修复后 seller/buyer controller 仍从端内独立 `seller_menu` / `buyer_menu` 权限树取数，但对外响应统一转换为前端可消费的 `RouterVo`。
- `RouterVo` 输出保留路由 `path`、`component`、`meta`、`query`、`perms`、隐藏状态和 children，不改变端内账号、角色、菜单或权限表归属。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalPermissionSupportTest,RouterVoPermissionContractTest,SellerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，10 个 reactor 模块 BUILD SUCCESS；23 tests passed。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 24 suites / 201 tests passed；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 11 个模块 BUILD SUCCESS。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，只有 CRLF 工作区提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：已同步。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

## 追加检查点：端内 Portal 首页 C 菜单与远端菜单域收敛

时间：2026-06-10 01:36

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只判断 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 结论

- 已收敛一个 P1：远端 `seller_menu` / `buyer_menu` 只有 `F` 权限时，seller/buyer 端内 `/getRouters` 页面树为空。
- 已新增端内首页 `C` 菜单：
  - seller：`seller:portal:home`，`Seller/Portal/index`
  - buyer：`buyer:portal:home`，`Buyer/Portal/index`
- 已通过 wrapper 复用共享 `Portal/Home` 页面，没有新增页面内容或 UI 细调。
- 已把 seed guard 固定进 `SqlExecutionGuardContractTest`，把前端 JS sidecar/wrapper 固定进 `admin-auth-sidecar-contract.test.ts`。
- 已修复 `20260608_terminal_menu_auto_increment_reset.sql` 在远端 MySQL 上的 session 变量赋值和 auto_increment 元数据断言兼容性。
- 已补登 `ImageResourceUtilsTest` 到三端 manifest，修复 manifest gate 发现的登记缺口。

### 远端执行与核验

- 执行记录：`docs/plans/2026-06-10-terminal-portal-home-menu-seed-db-execution-record.md`
- 执行目标：远端 MySQL `fenxiao`，连接来源 `.env.local`，未输出密钥。
- `20260610_terminal_portal_home_menu_seed.sql`：成功，`executed_statements = 27`。
- `20260608_terminal_menu_auto_increment_reset.sql`：最终成功，`executed_statements = 51`。
- 关键核验：
  - `seller_home_menu_count = 1`
  - `buyer_home_menu_count = 1`
  - `seller_home_owner_grant_count = 3`
  - `buyer_home_owner_grant_count = 1`
  - seller/buyer 端内菜单 ID 越界、非法权限、页面 component 串端、role-menu 孤儿均为 `0`
  - `seller_menu max = 100019`，`SHOW CREATE TABLE AUTO_INCREMENT = 100020`
  - `buyer_menu max = 200014`，`SHOW CREATE TABLE AUTO_INCREMENT = 200015`

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=SqlExecutionGuardContractTest,SellerPortalPermissionServiceImplMenuTreeTest,SellerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`SqlExecutionGuardContractTest` 80 tests，seller 14 tests，buyer 14 tests。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests\admin-auth-sidecar-contract.test.ts tests\remote-menu-route-guard.test.ts tests\getrouters-authority-contract.test.ts --runInBand`：通过，3 suites / 52 tests。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ImageResourceUtilsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，5 tests。

### 未验证项

- 未做浏览器、截图、DOM 或 UI 细调验收。
- 未启动或重启后端。
- 未读取或写入 Redis。
- 未做 live HTTP `/seller/getRouters` / `/buyer/getRouters` 请求；本轮远端验证停留在 DB 只读核验和代码级合同。

## 追加检查点：端内 portal 接口身份来源复核

时间：2026-06-10 01:02

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只判断 P0/P1。

### 核对范围

- seller/buyer portal controller 的端内已认证接口。
- `TerminalRouteOwnershipTest`、`PortalAnonymousEndpointContractTest`、`PortalProductEndpointPermissionContractTest`、`PortalSelfServiceSurfaceContractTest`。
- `react-ui/src/services/portal/session.ts` 与 `tests/portal-session-request.test.ts` 的请求参数收口。

### 结论

- 未发现新的 P0/P1。
- seller/buyer portal 已认证 handler 当前按端声明 `@PortalPreAuthorize` 和 `@PortalLog`，并从 `PortalSessionContext.requireSession("seller"/"buyer")` 推导主体和账号。
- 后端合同已固定：端内接口不得接收调用方传入的 `sellerId`、`buyerId`、`subjectId`、`accountId` 等身份范围参数，不得读取若依管理端登录上下文。
- 前端 portal 请求会剥离调用方传入的主体、账号和 terminal 范围参数；会话列表请求仅保留分页参数。

### 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=TerminalRouteOwnershipTest,PortalAnonymousEndpointContractTest,PortalProductEndpointPermissionContractTest,PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，3 个 reactor 模块 BUILD SUCCESS；11 tests passed。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests\portal-session-request.test.ts tests\portal-product-schema-preview.test.ts --runInBand`：通过，2 suites / 32 tests passed。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

## 追加检查点：只读 Spark 六切片与 integration 脱敏收口

时间：2026-06-10 12:43。

### 子 Agent 使用记录

- 本轮使用 6 个只读子 Agent，模型均为 `gpt-5.3-codex-spark`。
- 6 个子 Agent 均已关闭；2 个无有效结论，4 个有效结论已由主线程复核。
- 采纳并关闭 1 个 P1：`UpstreamMaskUtils.redactJson(...)` 未统一脱敏 `appKey/app_key`。
- 未采纳 3 条 PartnerManagement / direct-login 测试增强建议为当前 P1；现有前后端合同已覆盖核心权限、`session:list` 与 `forceLogout` 分离、人工临时密码重置和 direct-login 失败/超时拒绝。

### 代码与门禁

- `UpstreamMaskUtils.redactJson(...)` 已补 `appKey` / `app_key` 脱敏。
- 新增 `UpstreamMaskUtilsTest`，并登记到 `react-ui/tests/three-terminal.manifest.json` 的 backend 与 explicit critical 清单。
- `npm run verify:three-terminal` 已通过，前端 25 suites / 207 tests，后端 reactor test-compile 15 个模块，后端三端合同测试 12 个模块。
- `codegraph sync .` 已通过；`Synced 8 changed files`，`Added: 1, Modified: 7 - 338 nodes`。

### 远端影响

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。

## 追加检查点：管理端端内角色/部门/菜单控制面合同复核

时间：2026-06-10 00:57

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只判断 P0/P1。

### 核对范围

- `AdminSellerRoleController`、`AdminSellerDeptController`、`AdminSellerMenuController`。
- `AdminBuyerRoleController`、`AdminBuyerDeptController`、`AdminBuyerMenuController`。
- `seller_buyer_management_seed.sql` 中 seller/buyer admin role、dept、menu 权限 seed。
- `react-ui/tests/three-terminal.manifest.json` 中管理端控制面合同登记。

### 结论

- 未发现新的 P0/P1。
- seller/buyer role、dept、menu 管理端 controller 当前使用 `seller:admin:*` / `buyer:admin:*` 权限命名空间。
- mutating handler 已有 `@Log`，敏感读取和强控制接口继续由合同测试固定。
- `SellerAdminPermissionContractTest`、`BuyerAdminPermissionContractTest`、`AdminAccountPermissionUiContractTest`、`PortalAdminAuditBindingContractTest` 已登记在三端 manifest。

### 验证

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminAccountPermissionUiContractTest,PortalAdminAuditBindingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，3 个 reactor 模块 BUILD SUCCESS；11 tests passed。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

## 追加检查点：当前工作树三端总门禁复跑

时间：2026-06-10 00:54

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只判断 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 本检查点未启动新子 Agent。
- 后续如需并行拆分，按 `AGENTS.md` 当前规则区分任务类型：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。

### 当前态验证

- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。
- 前端 guard：`portal-token`、`partner-management`、`seller-portal-product`、`buyer-portal-product`、`product-upstream-mirrors` 均通过。
- React typecheck 通过。
- 前端 Jest：24 suites / 196 tests passed。
- 后端 reactor test-compile：14 个模块 BUILD SUCCESS。
- 后端三端合同测试：11 个模块 BUILD SUCCESS；product 64 tests、seller 100 tests、buyer 101 tests 通过。

### 当前判断

- 当前工作树未暴露新的三端 P0/P1。
- `verify-three-terminal` 证明当前 manifest、guard、React typecheck、前端关键测试、后端 reactor test-compile 和后端三端合同测试均通过。
- 本检查点未做 live `/getRouters`、浏览器、截图、DOM、远端 MySQL 或 Redis 运行态验证，按当前快速推进模式不阻塞。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

## 追加检查点：logistics SQL 合同与执行记录收口

时间：2026-06-10 13:00，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 处理结论

- `P0`：未发现新增。
- `P1`：已处理 logistics SQL 专项合同覆盖不足和执行记录回滚/脱敏口径不足。
- 未采纳为代码缺口：MySQL DDL 不能靠单个外层事务完整回滚，因此不强行加伪事务，改为记录 DDL 隐式提交事实，并固定确认 token、fail-closed、完成态断言和单独确认的回滚方案。
- 未采纳为新增补测：direct-login 401 不清 token、admin 前缀不走 portal、空 authority fail-closed 已由现有测试覆盖。

### 变更证据

- `RuoYi-Vue/sql/20260610_logistics_carrier_channel_rename.sql`：补 `set names utf8mb4;`。
- `RuoYi-Vue/logistics/src/test/java/com/ruoyi/logistics/architecture/LogisticsAdminRouteContractTest.java`：新增 account-refactor 与 channel-rename SQL 专项合同。
- `docs/plans/2026-06-10-logistics-carrier-account-refactor-sql-execution-record.md`：补确认边界、库名脱敏、回滚方式和 DDL 隐式提交说明。
- `docs/plans/2026-06-10-logistics-carrier-management-sql-execution-record.md`：补确认边界、库名脱敏、回滚方式和 DDL 隐式提交说明。
- `docs/plans/2026-06-10-logistics-carrier-channel-rename-sql-execution-record.md`：补脚本字符集加固说明和 DDL 隐式提交说明。

### 验证

- `mvn -pl logistics -am "-Dtest=LogisticsAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，4 tests passed。
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，80 tests passed。
- `node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `npm run verify:three-terminal`：通过，前端 25 suites / 208 tests passed，后端三端合同测试 12 个模块 BUILD SUCCESS。
- `git diff --check`：通过，只有 CRLF 工作区提示。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

## 追加检查点：当前工作树三端 P0/P1 复核与总门禁复跑

时间：2026-06-10 13:21，本机 `Asia/Shanghai`。

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只判断 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent 使用记录

- 尝试启动 4 个只读、审查、探索类子 Agent，均指定 `gpt-5.3-codex-spark`。
- 4 个子 Agent 均因 Spark 额度限制失败，错误信息为：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at 5:28 PM.`
- 已全部关闭；按用户模型规则，未将只读任务自动切到 `gpt-5.4`。

### 当前态验证

- `node scripts/verify-three-terminal.mjs --check-manifest`：通过，`three-terminal manifest check passed.`
- `npm run tsc`：通过。
- `npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。
- 前端 guard：`portal-token`、`partner-management`、`seller-portal-product`、`buyer-portal-product`、`product-upstream-mirrors` 均通过。
- React typecheck 通过。
- 前端 Jest：25 suites / 208 tests passed。
- 后端 reactor test-compile：15 个模块 BUILD SUCCESS。
- 后端三端合同测试：12 个模块 BUILD SUCCESS。

### 当前判断

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口；本轮没有代码修复。
- `20260610_terminal_portal_home_menu_seed.sql` 已有确认 token、`45000` fail-closed、ID 段、权限前缀、页面签名和 owner grant 完成态断言。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或远程库当前态验收完成。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

## 追加证据：portal 首页商品样板隐藏与产品权限默认授权收窄

时间：2026-06-10 13:35，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent

- 本轮未启动新的子 Agent。
- 当前规则：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮主线程直接完成小范围代码修复，无模型回退。

### 变更证据

- `react-ui/src/pages/Portal/Home/index.tsx`：移除 `SellerProductSchemaPreview`、`BuyerProductSchemaPreview`、`SellerOwnDistributionProductList`、`BuyerDistributionProductList` 的首页 import、权限计算和 JSX 挂载。
- `react-ui/tests/portal-product-schema-preview.test.ts`：新增首页最小框架断言，固定 `product:*` 权限不再驱动 portal 首页展示。
- `react-ui/scripts/check-seller-portal-product-template.mjs`、`react-ui/scripts/check-buyer-portal-product-template.mjs`：继续校验商品组件和 portal service，但把首页挂载商品组件判定为违规。
- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`：继续创建 seller/buyer product hidden F 权限菜单，但 owner 默认 `role_menu` 授权列表只保留 account、loginLog、operLog、session、dept、role。
- `RuoYi-Vue/sql/20260604_portal_product_category_permission_seed.sql`、`20260604_seller_product_schema_permission_seed.sql`、`20260604_buyer_product_schema_permission_seed.sql`：保留权限菜单创建，移除 owner 默认 role_menu 授权。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`：增加 `assertTerminalPermissionSeedMenuOnlyGuard(...)`，固定产品权限 seed 的 menu-only 边界。

### 验证

- `npm run guard:seller-portal-product`：通过。
- `npm run guard:buyer-portal-product`：通过。
- `npx jest --config jest.config.ts tests/portal-product-schema-preview.test.ts --runInBand`：通过，1 suite / 6 tests passed。
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest#terminalPermissionSeedsMustGuardMenuSlotsBeforeRoleBinding+splitTerminalPermissionSeedsMustFailClosedOnInvalidOrDuplicatePermsBeforeRoleBinding+splitTerminalPermissionSeedsMustWrapRoleBindingInTransactionAndAssertCompletion" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，3 tests passed。
- `npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 25 suites / 208 tests passed；React typecheck 通过；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 12 个模块 BUILD SUCCESS。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 本检查点不证明远程库当前历史 product role_menu 授权已被清理；若需要清理，需要单独设计并确认远端 DML。

### 结论

- `P0` 未见新增。
- `P1` 已收敛：当前 portal 首页回到三端最小账号权限框架，产品能力作为后续业务资产保留但不默认展示；未来 seed 回放不再默认给端内 owner 商品权限。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或远程库当前态验收完成。

## 追加证据：远程 owner 历史商品权限授权清理

时间：2026-06-10 13:46，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent

- 本轮未启动新的子 Agent。
- 当前规则按用户最新确认执行：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。

### 处理范围

- `P1`：远程库中 seller/buyer 端内 owner 角色仍保留历史 `product:*` 默认授权，和当前“产品能力保留但不默认展示、不默认授权”的方向不一致。
- 本轮只清理 `seller_role_menu` / `buyer_role_menu` 的历史 owner product 授权，不删除 `seller_menu` / `buyer_menu` 中隐藏的产品权限菜单定义。
- 未写入 Redis，未启动或重启后端，未做浏览器运行态验收。

### 变更证据

- `RuoYi-Vue/sql/20260610_terminal_owner_product_permission_cleanup.sql`：新增 guarded DML 脚本，要求确认 token、预览计数、预览签名；执行前和事务内均重新计算精确目标集合，不匹配则 `45000` fail-closed。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`：新增 SQL 合同，固定该清理脚本必须按精确 role_menu 目标清理，并保留菜单定义。
- `docs/plans/2026-06-10-terminal-owner-product-permission-cleanup-sql-execution-record.md`：新增远程执行记录，记录数据源确认、预览、执行、结果和回滚边界。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`、`docs/plans/2026-06-09-three-terminal-p0p1-gpt54-fast-pass-record.md`：追加本次 P1 收口检查点。

### 远程执行结果

- 数据源来源：读取 `.env.local` 与后端激活配置确认，目标为当前本机若依验证环境使用的远程 MySQL；报告中不记录 URL、账号、库名或密码明文。
- 清理前只读预览：seller owner product 授权 12 条，buyer owner product 授权 4 条。
- 执行方式：通过本机 MySQL JDBC 驱动执行 guarded SQL；执行前注入确认 token、预览计数和预览签名。
- 清理后校验：seller owner product 授权 0 条，buyer owner product 授权 0 条；seller product 菜单定义保留 4 条，buyer product 菜单定义保留 4 条。

### 验证

- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest#highImpactSqlScriptsMustRequireExplicitConfirmToken+datedHighImpactSqlScriptsMustBeAutoDiscoveredAndGuarded+terminalOwnerProductPermissionCleanupMustLockExactRoleMenuTargetsAndKeepMenus" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，3 tests passed。
- `npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；前端 25 suites / 208 tests passed；React typecheck 通过；后端 reactor test-compile 15 个模块 BUILD SUCCESS；后端三端合同测试 12 个模块 BUILD SUCCESS。
- 远程 SQL 执行后结果断言通过：owner product 授权已为 0，隐藏 product 菜单定义仍保留。

### 结论

- `P0` 未见新增。
- `P1` 已进一步收敛：代码、未来 seed 与远程当前库历史授权状态现在一致，端内 owner 不再默认拥有 seller/buyer 商品权限。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态验收完成。

## 追加检查点：Spark 子 Agent 额度失败后的主线程 P0/P1 复核

时间：2026-06-10 13:56，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1；不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent

- 按用户最新规则尝试启动 6 个只读、审查、探索类 Explorer，均指定 `gpt-5.3-codex-spark`。
- 6 个子 Agent 均因 Spark 额度限制失败，错误信息为：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at 5:28 PM.`
- 已全部关闭；未将只读任务自动切到 `gpt-5.4`。

### 主线程复核范围

- `RuoYi-Vue/seller`、`RuoYi-Vue/buyer`、`RuoYi-Vue/ruoyi-system`：扫描裸 `select*AccountById(accountId)`、端内 `PortalSessionContext` / `@PortalPreAuthorize` / `@PortalLog`、管理端控制面权限。
- `react-ui/src`、`react-ui/tests`、`react-ui/scripts`：扫描 portal token、401、direct-login、route authority、seller/buyer service 路径和管理端按钮权限。
- `RuoYi-Vue/sql`：扫描 seller/buyer 菜单、权限、owner 授权、端地址配置和 SQL guard 关键字。
- 远程 MySQL 只读核对：菜单 ID 段、端内权限前缀、页面 component、owner product 授权、portal home 菜单和 owner 授权、端账号空密码。

### 远程只读结果

- 数据源来源：读取 `.env.local` 与后端激活配置确认，目标为远程 MySQL；未输出 URL、账号、库名或密码明文。
- `seller_menu_out_of_range=0`，`buyer_menu_out_of_range=0`。
- `seller_menu_invalid_perms=0`，`buyer_menu_invalid_perms=0`。
- `seller_page_missing_component=0`，`buyer_page_missing_component=0`。
- `seller_owner_product_grants=0`，`buyer_owner_product_grants=0`。
- `seller_null_blank_passwords=0`，`buyer_null_blank_passwords=0`。
- `seller:portal:home` 与 `buyer:portal:home` 当前远程 component 分别为 `Seller/Portal/index`、`Buyer/Portal/index`，与当前合同和前端 wrapper 一致；owner 授权存在。

### 结论

- `P0` 未见新增。
- 当前未坐实新的 P1 代码缺口；本轮未改业务代码、未执行远程 MySQL DDL/DML、未读写 Redis、未启动或重启后端。
- 仍不声明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态验收完成。

## 追加检查点：账号 SQL 范围约束复核与三端总门禁复跑

时间：2026-06-10 14:05，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent

- 本轮未启动新的子 Agent。
- 当前规则：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮未坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### 复核证据

- `RuoYi-Vue/sql/20260610_terminal_portal_home_menu_seed.sql`：已确认存在确认 token、`45000` fail-closed、seller/buyer 菜单 ID 段、权限前缀、页面 component 签名、owner grant 完成态断言。
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`：账号更新、锁定、重置密码、登录信息更新均带 `seller_id + seller_account_id` 条件。
- `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`：账号更新、锁定、重置密码、登录信息更新均带 `buyer_id + buyer_account_id` 条件。
- `SellerPortalController` / `BuyerPortalController`：受保护 portal 端点继续使用 `@PortalPreAuthorize`、`@PortalLog` 和 `PortalSessionContext.requireSession(...)`。
- `react-ui/src/services/portal/session.ts`：portal 查询继续剥离端内身份范围参数；session list 只放行分页参数。

### 验证

- `node scripts/verify-three-terminal.mjs --check-manifest`：通过，输出 `three-terminal manifest check passed.`
- `npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`
- 前端：26 suites / 213 tests passed。
- React typecheck：通过。
- 后端 reactor test-compile：15 个模块 BUILD SUCCESS。
- 后端三端合同测试：12 个模块 BUILD SUCCESS。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口；本轮无业务代码修改。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：三端门禁覆盖面与 controller 权限扫描

时间：2026-06-10 14:10，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent

- 本轮未启动新的子 Agent。
- 只读、审查、探索类子 Agent 仍按用户最新规则限定为 `gpt-5.3-codex-spark`；本轮没有切到 `gpt-5.4` 做只读任务。
- 本轮没有坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### 复核证据

- Manifest 覆盖：按当前 `verify-three-terminal.mjs` 的 critical frontend/backend test 发现规则比对，未发现关键测试漏登。
- Controller 注解：seller/buyer admin controller 未发现缺 `@PreAuthorize` 或写接口缺 `@Log`；seller/buyer portal controller 未发现缺 `@PortalPreAuthorize`、`@PortalLog` 或 `PortalSessionContext` 的新增 P0/P1。
- 端内账号权限隔离：seller/buyer main 代码未发现端内账号权限实现直接使用若依 `sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`sys_user_role`、`sys_role_menu`。
- 工作树风险：当前仍有 logistics/product/report 等大量未跟踪文件，属于后续提交/整理风险；本轮不作为三端账号权限运行时 P0/P1 处理。

### 验证

- `node scripts/verify-three-terminal.mjs --check-manifest`：通过，输出 `three-terminal manifest check passed.`
- `git diff --check`：通过，仅有已有 LF/CRLF 工作区提示。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口；本轮无业务代码修改。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：日志/会话/直登 Redis key 隔离复核

时间：2026-06-10 14:15，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent

- 本轮未启动新的子 Agent。
- 用户最新规则已作为后续执行标准：只读、审查、探索类子 Agent 使用 `gpt-5.3-codex-spark`；代码编辑、实现、修复类子 Agent 使用 `gpt-5.4`。
- 本轮未坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### 复核证据

- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`：直登 payload key 由 `portal_direct_login:{terminal}:{token_hash}` 组成；读取只走端隔离 key，旧无端前缀 key 只删除清理。
- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java`：portal session key 由 `portal_login_tokens:{terminal}:{tokenId}` 组成；JWT 和 Redis session 都带 terminal 隔离。
- `RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/web/service/TokenService.java`：管理端 token 仍使用若依 `CacheConstants.LOGIN_TOKEN_KEY`，未混用 portal session 或直登前缀。
- `PortalSelfServiceSurfaceContractTest`：继续固定端内自助 DTO 不暴露内部审计字段。
- `SellerServiceImpl` / `BuyerServiceImpl`：后台控制动作记录当前 acting admin；跨端直登失败不导入外端票据账号。

### 验证

- 静态 Node 断言：通过。
- `mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=PortalDirectLoginAuthContractTest,PortalSelfServiceSurfaceContractTest,PortalLoginSessionConsistencyContractTest,PortalPreAuthorizeAspectTest,PortalLogAspectContractTest,TokenServiceTerminalIsolationTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `ruoyi-system`：8 tests passed。
- `ruoyi-framework`：5 tests passed。
- `seller`：55 tests passed。
- `buyer`：55 tests passed。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口；本轮无业务代码修改。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：端内角色/菜单/部门与前端权限串端复核

时间：2026-06-10 14:23，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent

- 本轮按用户最新规则尝试启动 6 个只读 explorer 子 Agent，模型均为 `gpt-5.3-codex-spark`。
- 6 个 explorer 均因 Spark 额度限制失败，并已关闭。
- 本轮未把只读任务切到 `gpt-5.4`；未坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### 复核证据

- `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl`：端内角色、菜单和角色菜单关系保持 seller/buyer 独立；角色绑定前校验菜单 ID 段、component、perms 和权限前缀。
- `SellerPortalDeptServiceImpl` / `BuyerPortalDeptServiceImpl`：部门 CRUD、父级部门和账号部门约束均带当前主体 ID。
- `AdminSellerController` / `AdminBuyerController`：账号角色入口覆盖端角色查询、账号角色查询、账号角色编辑权限；会话列表与强退权限分离。
- `PartnerAccountModal` / `PartnerManagementPage`：前端按钮显隐与后端权限合同一致；免密登录等待消费成功结果。
- `portal/session.ts` / `sessionParams.ts`：前端请求继续剥离 caller-controlled identity scope 参数，session 查询只放行分页参数。
- `RuoYi-Vue/sql`：SQL guard 继续覆盖确认 token、`45000` fail-closed、端菜单 ID 段、端账号 password 最终结构和废弃 split seed fail-closed。

### 验证

- `mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=SqlExecutionGuardContractTest,PortalPermissionSupportTest,PortalPreAuthorizeAspectTest,PortalLogAspectContractTest,SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplMenuTreeTest,SellerPortalPermissionServiceImplPortalAccessTest,SellerPortalDeptServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalDeptServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- 后端定向测试合计：`ruoyi-system` 87、`ruoyi-framework` 4、`seller` 35、`buyer` 35，均 passed。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npx jest --config jest.config.js tests/partner-management-contract.test.ts tests/portal-session-request.test.ts tests/portal-unauthorized-redirect.test.ts tests/portal-direct-login-message.test.ts tests/terminal-session-token.test.ts --runInBand`：通过，5 suites / 62 tests passed。

### 远端影响

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。

### 结论

- `P0` 未见新增。
- 未坐实新的 P1 代码缺口；本轮无业务代码修改。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：远端库三端隔离结构只读核对

时间：2026-06-10 14:27，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 数据源证据

- `application.yml` active profile：`druid`。
- `application-druid.yml` MySQL 来源：`RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`。
- `.env.local` 目标库：远端 `fenxiao`。
- Redis 配置来源：`RUOYI_REDIS_*`；当前 logical DB 为 `1`，本轮未读写 Redis。

### 只读数据库证据

- 命令类型：MySQL JDBC + `jshell` 只读 `SELECT` / `information_schema` 查询。
- 影响范围：无 DDL、无 DML、无 Redis 读写、无后端重启。
- 输出范围：只记录聚合检查结果，不输出业务明细或密钥。

### 聚合结果

- 核心表存在：seller/buyer 主体、账号、角色、菜单、部门、账号角色、角色菜单、登录日志、操作日志、会话、免密票据表均存在。
- 密码列：`seller_account.password` / `buyer_account.password` 均为 `varchar(100)`、`NOT NULL`、默认值 `NULL`；空白密码计数均为 0。
- 菜单隔离：seller/buyer 菜单 ID 段异常、perms 异常、页面 component 异常均为 0。
- 关系隔离：seller/buyer 角色菜单孤儿或跨 ID 段、账号角色跨主体、部门父级跨主体均为 0。
- 审计字段：seller/buyer 登录日志、操作日志、会话表均具备 5 个免密审计字段。
- portal web url：当前为本地验证占位地址，属于后续真实三前端环境配置项，不作为当前 P0/P1。

### 结论

- 远端库当前结构与三端账号权限隔离核心模型一致。
- `P0` 未见新增。
- 未坐实新的 P1 数据库缺口；本轮无业务代码修改，无 DDL/DML。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、live `/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：管理端运行态 API 菜单权限合同验证

时间：2026-06-10 14:31，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 运行态证据

- 8080 已监听；Java 进程启动时间为 2026-06-10 14:25:59。
- 当前运行 jar 时间为 2026-06-10 14:25:49。
- `/captchaImage` 返回 `captchaEnabled=false`。
- 本轮未读取 Redis，也未输出登录 token。

### API 证据

- `/login`：`code=200`。
- `/getInfo`：`code=200`，角色 `admin`，权限 `*:*:*`。
- `/getRouters`：`code=200`，路由总数 64。
- `/partner/seller`：component `Seller/index`，perms `seller:admin:list`。
- `/partner/buyer`：component `Buyer/index`，perms `buyer:admin:list`。
- `/seller/admin/sellers/list?pageNum=1&pageSize=1`：带 token 返回 `code=200`，`total=4`；无 token 返回业务 `code=401`。
- `/buyer/admin/buyers/list?pageNum=1&pageSize=1`：带 token 返回 `code=200`，`total=1`；无 token 返回业务 `code=401`。
- `/seller/admin/menus/list`：带 token 返回 `code=200`，`dataCount=11`；无 token 返回业务 `code=401`。
- `/buyer/admin/menus/list`：带 token 返回 `code=200`，`dataCount=11`；无 token 返回业务 `code=401`。

### 结论

- 管理端运行态菜单和接口与三端隔离后台控制面一致。
- `P0` 未见新增。
- 未坐实新的 P1 运行态缺口；本轮无业务代码修改，无 DDL/DML。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：Portal 未登录与串端拒绝运行态验证

时间：2026-06-10 14:39，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent 证据

- `AGENTS.md` 中已存在模型规则：只读检查、审查、探索类使用 `gpt-5.3-codex-spark`，代码编辑、实现、修复类使用 `gpt-5.4`。
- 本轮 2 个只读探索子 Agent 均按 `gpt-5.3-codex-spark` 启动请求执行，但因额度限制失败，提示 17:28 后重试。
- 两个失败 Agent 均已关闭；本轮没有把只读任务回退给 `gpt-5.4`。

### API 证据

- 运行态：8080 正在监听，Java 进程启动于 2026-06-10 14:25:59。
- 无 token：`/seller/getInfo`、`/buyer/getInfo`、`/seller/getRouters`、`/buyer/getRouters`、`/seller/profile`、`/buyer/profile`、`/seller/account/login-logs?pageNum=1&pageSize=1`、`/buyer/account/login-logs?pageNum=1&pageSize=1`、`/seller/product/categories`、`/buyer/product/categories` 均返回业务 `code=401`。
- 管理端 token 打 portal：`/seller/getInfo`、`/buyer/getInfo`、`/seller/getRouters`、`/buyer/getRouters`、`/seller/profile`、`/buyer/profile` 均返回业务 `code=401`。
- 伪造 token：`/seller/getInfo`、`/buyer/getInfo` 均返回业务 `code=401`。

### Contract 证据

- `SellerPortalController` / `BuyerPortalController` 抽查显示受保护 portal handler 继续使用 `@Anonymous` + `@PortalPreAuthorize` + `@PortalLog` + `PortalSessionContext.requireSession(...)`。
- `mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=PortalAnonymousEndpointContractTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest,PortalPreAuthorizeAspectTest,PortalPermissionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`ruoyi-system` 16 tests passed，`ruoyi-framework` 3 tests passed。
- `npx jest --config jest.config.js tests/portal-unauthorized-redirect.test.ts tests/terminal-session-token.test.ts --runInBand`：通过，2 suites / 24 tests passed。
- `node scripts\verify-three-terminal.mjs --check-manifest`（在 `react-ui/` 下执行）：通过。
- `git diff --check`：通过，仅有既有 CRLF 工作区提示。
- `codegraph sync .`：通过，结果为 `Already up to date`。

### 影响与结论

- 未执行手工 SQL，未执行远端 MySQL DDL/DML 命令，未重启后端。
- `/login` 仅用于临时管理端 token 对照，可能产生正常管理端登录日志和 Redis token；未输出 token 明文，未改业务数据。
- `P0` 未见新增，未坐实新的 P1 运行态缺口。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：三端总门禁与登录入口串端复核

时间：2026-06-10 14:45，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 计划口径复核

- 计划文件已写明：阶段 4/5 是长期路线图和终态验收，不属于当前三端账号权限 P0/P1 快速推进的完成条件。
- 当前 P0/P1 仍以 `verify-three-terminal`、模块编译、合同/单测和权限/串端 fail-closed 校验为准。

### 总门禁证据

- `node scripts\verify-three-terminal.mjs`：通过，输出 `three-terminal verification passed`。
- 前端：26 suites / 213 tests passed。
- 前端 guard：`guard:portal-token`、`guard:partner-management`、`guard:seller-portal-product`、`guard:buyer-portal-product`、`guard:product-upstream-mirrors` 均通过。
- React typecheck：通过。
- 后端 reactor `test-compile`：15 个模块均为 `SUCCESS`。
- 后端三端合同测试：12 个相关模块均为 `SUCCESS`。

### 登录串端证据

- 管理端账号口径请求 `/seller/login`：业务 `code=500`，未返回 token。
- 管理端账号口径请求 `/buyer/login`：业务 `code=500`，未返回 token。
- 本轮未读取、输出或尝试 seller/buyer 端内账号密码。

### 影响与结论

- 两次 portal 登录失败请求可能产生端内失败登录日志；未执行手工 SQL，未执行远端 MySQL DDL/DML，未重启后端。
- 当前工作树未暴露新的三端账号权限 P0/P1。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：端账号 sys_* 回挂与账号范围 SQL 复核

时间：2026-06-10 14:47，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 证据

- seller/buyer 生产 Java 扫描未发现端内账号、角色、菜单、部门代码回挂 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。
- `@PreAuthorize` 命中集中在管理端 controller，继续使用 `seller:admin:*` / `buyer:admin:*` 后台权限命名空间。
- `@PortalPreAuthorize` 命中集中在 portal controller，端内身份继续从 `PortalSessionContext.requireSession(...)` 推导。
- `SecurityUtils.getUserId()` 命中为管理端控制动作审计记录 acting admin，不作为端内账号来源。
- seller/buyer 账号、账号角色、角色菜单、会话、重置密码相关 XML 均保持主体 ID + 账号 ID 范围约束。

### Contract

- `mvn -pl ruoyi-system -am "-Dtest=TerminalAccountIsolationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `TerminalAccountIsolationTest`：4 tests passed。

### 结论

- 未发现新的三端账号权限 P0/P1。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：三端 SQL seed/cleanup guard 复核

时间：2026-06-10 14:50，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 证据

- `RuoYi-Vue/sql/20260610_terminal_portal_home_menu_seed.sql`：保留确认 token、`45000` fail-closed、端内菜单 ID 区间、权限前缀、component 路径、slot 签名和 owner grant 完成断言。
- `RuoYi-Vue/sql/20260610_terminal_owner_product_permission_cleanup.sql`：保留确认 token、预期数量/签名校验、事务内精确删除和完成断言；只清理 owner 角色上的 product 授权，不删除 product 菜单定义。
- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`：当前 owner 默认授权不再包含 `seller:product:*` / `buyer:product:*`。
- `RuoYi-Vue/sql/20260608_terminal_menu_auto_increment_reset.sql` 与 20260604 product/portal seed 仍在 SQL guard 自动发现范围内。

### Contract

- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `SqlExecutionGuardContractTest`：81 tests passed。

### 结论

- 未发现新的 SQL seed/cleanup P0/P1。
- 本轮未修改业务代码，未执行手工 SQL，未执行远端 MySQL DDL/DML。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：Portal 首页会话权限 P1 收口

时间：2026-06-10 15:01，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核和修复 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent

- 尝试启动 4 个只读 explorer，模型为 `gpt-5.3-codex-spark`。
- 结果：全部因 Spark 额度限制失败，提示 17:28 后重试；已关闭。
- 本轮未将只读检查切换到 `gpt-5.4`。

### 证据与修复

- 当前工作树 `react-ui/src/pages/Portal/Home/index.tsx` 会无条件请求 `/api/{seller|buyer}/account/sessions`。
- 后端 `SellerPortalController` / `BuyerPortalController` 对该接口要求 `seller:account:session:list` / `buyer:account:session:list`。
- 已将 Portal 首页改为先读取当前端权限，再按 `*:account:session:list` 决定是否请求和展示会话卡片。
- 无该权限时只清空会话状态，不请求受保护接口。
- `react-ui/tests/portal-home-error-handling.test.ts` 已增加契约。
- `react-ui/tests/three-terminal.manifest.json` 已将该测试加入 critical 前端清单。

### Contract

- `npx tsc --noEmit --pretty false`：通过。
- `npx jest --config jest.config.ts tests/portal-home-error-handling.test.ts tests/portal-session-request.test.ts --runInBand`：通过，2 suites / 29 tests passed。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npx jest --config jest.config.ts tests/portal-home-error-handling.test.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`：通过，2 suites / 26 tests passed；Jest 结束后有开放句柄提示，测试状态为 passed。
- `node scripts\check-seller-portal-product-template.mjs`、`node scripts\check-buyer-portal-product-template.mjs`：均通过。

### 结论

- 已修复一个 Portal 首页权限面 P1。
- 未发现新的 P0。
- 本轮未执行手工 SQL，未执行远端 MySQL DDL/DML，未重启后端。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：三端全量门禁复核

时间：2026-06-10 15:05，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 证据

- `react-ui/tests/portal-product-schema-preview.test.ts` 与 seller/buyer portal product guard 当前一致，均要求商品 widget 保持 detached，不挂回最小 Portal 首页。
- `npx jest --config jest.config.ts tests/portal-product-schema-preview.test.ts --runInBand`：通过，1 suite / 6 tests passed。

### Full verifier

- `node scripts\verify-three-terminal.mjs`：通过，输出 `three-terminal verification passed`。
- 前端 guard：`guard:portal-token`、`guard:partner-management`、`guard:seller-portal-product`、`guard:buyer-portal-product`、`guard:product-upstream-mirrors` 均通过。
- React typecheck：通过。
- 前端测试：26 suites / 214 tests passed。
- 后端 reactor `test-compile`：15 个模块均为 `SUCCESS`。
- 后端三端合同批次：12 个模块均为 `SUCCESS`；seller 100 tests、buyer 101 tests、product 69 tests、logistics 6 tests 通过。

### 结论

- 当前全量三端门禁未暴露新的 P0/P1。
- 本轮未修改业务代码，未执行手工 SQL，未执行远端 MySQL DDL/DML，未重启后端。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：远端三端菜单与授权只读核对

时间：2026-06-10 15:09，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 数据源与执行边界

- 激活配置：`application.yml` 使用 `druid`，`application-druid.yml` 从 `RUOYI_DB_*` 读取 MySQL。
- 连接来源：当前工作区 `.env.local`。
- 目标库名：`fenxiao`。
- 命令类型：Python + `pymysql` 只读 `SELECT` 聚合查询。
- 未输出数据库主机、账号、密码、token、Redis 或密钥明文。
- 未执行 DDL/DML，未读写 Redis，未重启后端。

### Evidence

- 独立端表存在：seller/buyer account、role、menu、dept、login_log、oper_log 均存在。
- 首页菜单：seller `1`，buyer `1`。
- Owner 首页授权：seller owner roles/grants 为 `3/3`，buyer owner roles/grants 为 `1/1`。
- 菜单 guard：seller/buyer 菜单 ID 越界、非法权限前缀、页面 component 为空或跨端、role-menu 孤儿关系均为 `0`。
- Owner 商品授权：seller `0`，buyer `0`。
- 商品权限菜单定义：seller `4`，buyer `4`。

### 结论

- 当前远端运行库三端菜单与 owner 授权关键状态未暴露新的 P0/P1。
- 本轮未修改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未重启后端。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：端账号密码与免密审计远端只读核对

时间：2026-06-10 15:16，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent 记录

- 按用户最新规则，只读探索尝试使用 `gpt-5.3-codex-spark`。
- 实际启动 2 个只读子 Agent，均因 Spark 额度限制失败，提示需等到 17:28 后重试。
- 两个失败子 Agent 已关闭；未改用 `gpt-5.4` 顶替只读任务。

### 数据源与执行边界

- 连接来源：当前工作区 `.env.local`。
- 目标库名：`fenxiao`。
- 命令类型：Python + `pymysql` 只读 `SELECT` 聚合查询。
- 未输出数据库主机、账号、密码、token、Redis 或密钥明文。
- 未执行 DDL/DML，未读写 Redis，未重启后端。

### Evidence

- 账号与审计表：`seller_account`、`buyer_account`、`seller_login_log`、`buyer_login_log`、`seller_oper_log`、`buyer_oper_log`、`seller_session`、`buyer_session`、`portal_direct_login_ticket` 均存在。
- 密码字段：seller/buyer account password 均为 `varchar(100) not null`，无默认值，空白密码行均为 `0`。
- legacy 清理：seller/buyer account 均无 `user_id` 列。
- 审计字段：seller/buyer 登录日志、操作日志、会话表均包含 `direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`。
- 票据表：关键列齐全，非法票据行数为 `0`，`token_hash` 唯一索引存在。
- 代码合同：重置密码未回退默认密码语义；端账号查询按主体 ID 和账号 ID 组合约束；免密 token 30 分钟有效，Redis key 按 terminal 隔离。

### 结论

- 当前端账号密码、免密审计和票据链路没有发现新的 P0/P1。
- 本轮未修改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未重启后端。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：管理端会话分权与端内自助 DTO 泄露复核

时间：2026-06-10 15:20，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent 记录

- 按用户最新规则，只读探索尝试使用 `gpt-5.3-codex-spark`。
- 实际启动 2 个只读子 Agent，均因 Spark 额度限制失败，提示需等到 17:28 后重试。
- 两个失败子 Agent 已关闭；未继续启动剩余 4 个只读切片，未改用 `gpt-5.4` 顶替只读任务。

### Evidence

- 会话分权：seller/buyer 管理端查看会话使用 `*:admin:session:list`，强制踢出使用 `*:admin:forceLogout`；后端、前端和合同测试一致。
- 历史报告中“新增 `*:admin:session:forceLogout`”建议继续按误报处理，因为它与当前 AGENTS 规则不一致。
- 端内自助可见面：seller/buyer 自助日志和会话接口返回 `PortalOwn*Profile`，不返回 `tokenId`、`directLoginTicketId`、`actingAdmin*`、`directLoginReason`、`operParam`、`jsonResult` 等内部审计字段。
- Manifest：`SellerAdminPermissionContractTest`、`BuyerAdminPermissionContractTest`、`PortalSelfServiceSurfaceContractTest`、`PortalSelfAuditSerializationTest`、`PortalHomeProfileSerializationTest` 已登记在 `react-ui/tests/three-terminal.manifest.json`。

### Contract

- `mvn -pl ruoyi-system -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,PortalSelfServiceSurfaceContractTest,PortalSelfAuditSerializationTest,PortalHomeProfileSerializationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- 结果：5 个合同类，16 tests passed；`ruoyi`、`ruoyi-common`、`ruoyi-system` 均 `BUILD SUCCESS`。

### 结论

- 当前管理端会话分权与端内自助 DTO 泄露面未发现新的 P0/P1。
- 本轮未修改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未重启后端。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：三端 manifest 覆盖与 portal 接口边界复核

时间：2026-06-10 15:24，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent 记录

- Spark 只读子 Agent 仍处于额度限制窗口，本轮未继续创建新的只读子 Agent。
- 未坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### Evidence

- Manifest：前端 `*.test.ts` 与 manifest 双向一致，后端 `*Test.java` 与 manifest 双向一致；`verify-three-terminal --check-manifest` 通过。
- Portal route ownership：`TerminalRouteOwnershipTest` 约束 seller/buyer portal 路由只能在 seller/buyer 模块，受保护 handler 不能接收客户端身份范围参数，也不能使用若依管理端登录上下文。
- Portal anonymous/auth：`PortalAnonymousEndpointContractTest` 约束登录/免密登录和 portal token filter 边界。
- Product portal：`PortalProductEndpointPermissionContractTest` 约束商品 portal controller 从 `PortalSessionContext` 派生端身份和主体范围，并通过端内 service 调用。

### Contract

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过，输出 `three-terminal manifest check passed.`。
- `mvn -pl ruoyi-system,seller,buyer,product -am "-Dtest=TerminalRouteOwnershipTest,PortalAnonymousEndpointContractTest,PortalProductEndpointPermissionContractTest,SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- 结果：`ruoyi-system` 10 tests、`seller` 10 tests、`buyer` 11 tests；10 个 reactor 模块均 `BUILD SUCCESS`。

### 结论

- 当前三端 manifest 覆盖与 seller/buyer portal 接口边界未发现新的 P0/P1。
- 本轮未修改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未重启后端。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：Spark 额度失败后的三端总门禁复跑

时间：2026-06-10 15:31，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### 子 Agent 记录

- 本轮按用户最新规则尝试启动 6 个只读 explorer，模型均为 `gpt-5.3-codex-spark`。
- 6 个子 Agent 均因 Spark 额度限制失败，提示需等到 17:28 后重试；均已关闭。
- 本轮未将只读检查切换到 `gpt-5.4`。
- 未坐实需要代码编辑的 P0/P1，因此未启动 `gpt-5.4` 实现子 Agent。

### Evidence

- Manifest gate：`node scripts\verify-three-terminal.mjs --check-manifest` 通过。
- Full verifier：`node scripts\verify-three-terminal.mjs` 通过，输出 `three-terminal verification passed`。
- 前端 guard：`guard:portal-token`、`guard:partner-management`、`guard:seller-portal-product`、`guard:buyer-portal-product`、`guard:product-upstream-mirrors` 均通过。
- React typecheck：通过。
- 前端测试：26 suites / 214 tests passed。
- 后端 reactor `test-compile`：15 个模块均 `SUCCESS`。
- 后端三端合同批次：12 个模块均 `SUCCESS`；seller 100 tests、buyer 101 tests、product 69 tests、logistics 6 tests 通过。
- 定向静态扫描未发现 seller/buyer 端内账号权限控制面回挂若依 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`。
- 定向静态扫描未发现裸单参数 accountId 生产查询入口；当前账号查询继续带主体 ID。
- 定向静态扫描未坐实旧 direct-login key、误命名会话权限、默认密码重置入口或 React 三端 URL/权限/token 串端的新 P0/P1。

### 结论

- 当前全量三端门禁和定向静态扫描未暴露新的 P0/P1。
- 本轮未修改业务代码，未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：Portal 首页刷新会话权限 P1 收口

时间：2026-06-10 15:39，本机 `Asia/Shanghai`。

本轮仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为证据方向，只复核和修复 P0/P1，不做浏览器、截图、DOM 或 UI 细调。

### Finding

- `react-ui/src/pages/Portal/Home/index.tsx` 中 Portal 首页首次加载已按 `account:session:list` 权限控制会话请求，但“刷新”按钮仍无条件执行 `loadSessions(terminal)`。
- 影响：无 `seller/buyer:account:session:list` 的端内账号点击刷新时，会请求受保护会话接口；后端仍会拒绝，但前端权限面不够 fail-closed。

### Fix

- 增加 `clearSessions()`，统一清空会话状态并取消旧请求序列。
- “刷新”按钮改为 `handleRefresh`，先清空本地会话状态，再刷新主体/账号/权限快照。
- `handleRefresh` 不直接调用 `loadSessions(terminal)`；会话请求统一由最新权限快照的 `useEffect` gating 后触发。
- `portal-home-error-handling.test.ts` 增加刷新路径合同。

### Evidence

- `npx jest --config jest.config.ts tests/portal-home-error-handling.test.ts tests/portal-session-request.test.ts --runInBand`：通过，2 suites / 30 tests passed。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `npx tsc --noEmit --pretty false`：通过。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\verify-three-terminal.mjs`：通过，输出 `three-terminal verification passed`。
- 全量 verifier 中前端测试变为 26 suites / 215 tests passed；后端 reactor `test-compile` 15 个模块 `SUCCESS`；后端三端合同批次 12 个模块 `SUCCESS`。

### 结论

- Portal 首页刷新会话权限 P1 已关闭。
- 最终实现口径为：刷新按钮不直接请求会话，端内会话接口只由最新权限快照 gating 后触发。
- 本轮未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态或 UI 细节验收完成。

## 追加检查点：manifest 漏登 P1 收口与总门禁恢复

时间：2026-06-10 18:53，本机 `Asia/Shanghai`。

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只处理 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent

- 启动 6 个 `gpt-5.3-codex-spark` 只读 explorer。
- 已完成并关闭、结论采纳为“未发现 P0/P1”的切片：
  - React 管理端 seller/buyer service、权限、会话分权、人工临时密码重置和模板完整性。
  - React portal token、401、direct-login、remote menu 空权限拒绝和 proxy 串端检查。
  - 三端 verifier、manifest、guard、动态 Maven reactor 和测试覆盖检查。
- 未采纳为证据的切片：
  - SQL/seed guard：上下文窗口错误退出，已关闭。
  - 后端 seller/buyer portal 自助链路：连续超时后关闭。
  - 管理端控制链路：连续超时后关闭。
- 本轮未启动 `gpt-5.4` 实现子 Agent；主线程完成坐实 P1 的最小补丁。

### Finding

- 完整 `node scripts\verify-three-terminal.mjs` 首次复跑失败，失败点为 `tests/verify-three-terminal-backend-gate.test.ts`。
- `react-ui/tests/upstream-system-permission-guard.test.ts` 已存在且已跟踪，但未登记到 `react-ui/tests/three-terminal.manifest.json`。
- 该漏登导致三端 manifest gate 负例测试被无关缺口抢先触发，属于当前 P1。

### Fix

- `react-ui/tests/three-terminal.manifest.json` 补登：
  - `frontendTestPaths`: `tests/upstream-system-permission-guard.test.ts`
  - `criticalFrontendExplicitTestPaths`: `tests/upstream-system-permission-guard.test.ts`

### Evidence

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过，输出 `three-terminal manifest check passed.`。
- `npx jest --config jest.config.ts --runTestsByPath tests\verify-three-terminal-backend-gate.test.ts --runInBand`：通过，1 suite / 23 tests。
- `node scripts\verify-three-terminal.mjs`：通过，最终输出 `three-terminal verification passed`。
- 全量三端 gate：前端 27 suites / 220 tests passed；后端 reactor `test-compile` 15 个模块 `SUCCESS`；后端三端合同批次 12 个模块 `SUCCESS`；seller 110 tests、buyer 112 tests、product 69 tests、logistics 11 tests 通过。

### 结论

- `P0` 未见新增。
- 已关闭 1 个 P1：关键前端权限测试漏登 manifest 导致三端总门禁失败。
- 当前三端账号权限框架代码级门禁重新通过。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
- 本检查点仍不证明三物理前端、完整 seller/buyer portal 菜单、浏览器运行态和 UI 细节验收完成。

## 追加检查点：当前态三端 P0/P1 复核

时间：2026-06-10 18:58，本机 `Asia/Shanghai`。

### Scope

- 本轮只复核 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器、截图、DOM 或 UI 细调验收。
- 未启动新的子 Agent；当前模型规则仍为只读检查用 `gpt-5.3-codex-spark`，代码编辑用 `gpt-5.4`。

### Evidence

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `git diff --check`：通过，仅有既有 LF/CRLF 提示。
- `node scripts\verify-three-terminal.mjs`：通过，最终输出 `three-terminal verification passed`。
- 前端 27 suites / 220 tests passed；后端 reactor `test-compile` 15 个模块 `SUCCESS`；后端三端合同批次 12 个模块 `SUCCESS`，seller 110 tests、buyer 112 tests、product 69 tests、logistics 11 tests 通过。

### Conclusion

- `P0` 未见新增。
- 未坐实新的 P1 编译、guard、接口、权限、串端、service/字段缺口。
- 本轮无业务代码修改，无远端 MySQL DDL/DML，无 Redis 读写，无后端启动或重启。
- 本检查点仍不证明三物理前端、live `/seller/getRouters` / `/buyer/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：Spark 只读子 Agent 失败后的主线程 P0/P1 续扫

时间：2026-06-10 19:28，本机 `Asia/Shanghai`。

### Scope

- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 只判断 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不覆盖：浏览器、截图、DOM、UI 细调、三物理前端拆分验收、live `/seller/getRouters` / `/buyer/getRouters` 验收。
- 未执行远端 MySQL DDL/DML；未读写 Redis；未启动或重启后端。

### Sub Agents

- 启动 6 个 `gpt-5.3-codex-spark` 只读 explorer。
- 6 个 explorer 均因 Spark 额度限制失败，错误信息均为：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at Jun 17th, 2026 2:16 AM.`
- 6 个 explorer 均已关闭；无可采纳结论。
- 本轮未把只读任务切到 `gpt5.4`。

### Evidence

- seller/buyer 生产模块未见 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 端内控制面混用。
- 账号查询继续带主体 ID 和账号 ID；Mapper XML 核心读写继续带 `seller_id` / `buyer_id` 约束。
- 管理端账号角色、会话列表、强制踢出、重置密码权限链路未见 P1 回退。
- portal 自助日志和会话接口返回端内可见 `PortalOwn*Profile`。
- direct-login Redis payload 只读取 terminal scoped key；legacy key 只删除不读取。
- owner 默认角色授权清单未包含产品权限。
- React 侧 portal 查询参数剥离、远程菜单空权限 fail-closed、401 reject/throw 和管理端 service 路径未见串端。

### Verification

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `npx jest --config jest.config.ts --runTestsByPath tests\partner-management-contract.test.ts tests\portal-session-request.test.ts tests\terminal-session-token.test.ts tests\remote-menu-route-guard.test.ts tests\admin-auth-sidecar-contract.test.ts --runInBand`：通过；5 suites、88 tests。
- `mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,TerminalSqlIsolationContractTest,TerminalRoleMenuMapperIsolationContractTest,PortalDirectLoginSupportTest,PortalPermissionCheckerTest,PortalPermissionSupportTest,PortalSelfServiceSurfaceContractTest,PortalPasswordChangeContractTest,AdminAccountPermissionUiContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,SellerServiceImplTest,SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,BuyerServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；ruoyi-system 53 tests、seller 88 tests、buyer 89 tests。

### Conclusion

- `P0` 未见新增。
- 未坐实新的 P1 编译、guard、接口、权限、串端、service/字段缺口。
- 本轮无业务代码修改，无远端 MySQL DDL/DML，无 Redis 读写，无后端启动或重启。

## 追加检查点：远程库权限 seed 与 owner 绑定只读核对

时间：2026-06-10 19:25，本机 `Asia/Shanghai`。

### Scope

- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 数据源：当前后端激活 `druid` 配置与本机 `.env.local`。
- 操作边界：只执行远程 MySQL 只读聚合查询；未执行 DDL/DML；未读写 Redis；未输出任何连接信息、token 或密钥。
- 不覆盖范围：浏览器、截图、DOM、UI 细调、三物理前端拆分验收、live `/seller/getRouters` / `/buyer/getRouters` 验收。

### Evidence

- `missing_sys_menu_required_admin_perms = 0`。
- `seller_owner_role_count = 4`；`buyer_owner_role_count = 1`。
- `seller_owner_account_without_owner_role = 0`；`buyer_owner_account_without_owner_role = 0`。
- `seller_owner_missing_self_perms = 0`；`buyer_owner_missing_self_perms = 0`。
- `seller_owner_missing_portal_home = 0`；`buyer_owner_missing_portal_home = 0`。
- `seller_owner_product_grants = 0`；`buyer_owner_product_grants = 0`。
- `seller_role_menu_missing_menu = 0`；`buyer_role_menu_missing_menu = 0`。
- `seller_account_role_missing_role = 0`；`buyer_account_role_missing_role = 0`。

### Conclusion

- `P0` 未见新增。
- 未坐实新的 P1 权限 seed、owner 绑定、角色菜单关联或账号角色关联缺口。
- 本轮无业务代码修改，无远端 MySQL DDL/DML，无 Redis 读写，无后端启动或重启。

## 追加检查点：远程库三端关键结构只读核对

时间：2026-06-10 19:18，本机 `Asia/Shanghai`。

### 数据源

- 后端激活配置为 `druid`，JDBC 由 `.env.local` 中 `RUOYI_DB_*` 提供。
- Redis 由 `.env.local` 中 `RUOYI_REDIS_*` 提供；本轮未连接、未读取、未写入 Redis。
- 本轮只执行远程 MySQL 只读 schema 和聚合 count 查询；未执行 DDL/DML，未输出任何连接明文。

### 主线程复核

- 三端核心表存在：`seller`、`buyer`、`seller_account`、`buyer_account`、`seller_role`、`buyer_role`、`seller_menu`、`buyer_menu`、`seller_dept`、`buyer_dept`、`seller_account_role`、`buyer_account_role`、`seller_role_menu`、`buyer_role_menu`、`seller_login_log`、`buyer_login_log`、`seller_oper_log`、`buyer_oper_log`、`seller_session`、`buyer_session`、`portal_direct_login_ticket`。
- `seller_account.password` / `buyer_account.password` 均为 `varchar(100)`、`nullable=NO`、无默认值。
- seller/buyer 登录日志、操作日志、会话表均存在免密代入结构化字段：`direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`。
- 聚合异常计数均为 0：菜单 ID 越界、页面/按钮权限前缀异常、页面组件路径异常、账号空密码、role-menu 跨 ID 区间。
- `portal.seller.web.url` / `portal.buyer.web.url` 当前仍命中本地验证地址特征，继续按 P2 环境配置项记录，不阻塞当前 P0/P1。

### Evidence

- 远程 MySQL `information_schema` 表/列查询：通过。
- 远程 MySQL seller/buyer 菜单、账号、role-menu 聚合 count 查询：通过，异常计数均为 0。
- 复核 SQL 和 mapper 后确认当前会话表名为 `seller_session` / `buyer_session`，不是旧猜测的 `seller_login_session` / `buyer_login_session`。

### Conclusion

- `P0` 未见新增。
- 未坐实新的 P1 远程数据库结构、字段、菜单 ID 区间、权限前缀、组件路径或端账号密码约束缺口。
- 本轮无业务代码修改，无远端 MySQL DDL/DML，无 Redis 读写，无后端启动或重启。
- 本检查点仍不证明三物理前端、live `/seller/getRouters` / `/buyer/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：Spark 额度失败后的主线程 P0/P1 续扫

时间：2026-06-10 19:10，本机 `Asia/Shanghai`。

### 子 Agent

- 本轮按当前 `AGENTS.md` 规则尝试启动 6 个只读 explorer 子 Agent，模型均指定为 `gpt-5.3-codex-spark`。
- 6 个子 Agent 均因 Spark 额度限制失败，错误信息均为：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at Jun 17th, 2026 2:16 AM.`
- 6 个失败子 Agent 均已关闭。
- 本轮未把只读任务切到 `gpt5.4`；`gpt5.4` 仍只用于代码编辑、实现、修复类子 Agent。

### 主线程复核

- `RuoYi-Vue/seller` / `RuoYi-Vue/buyer` 主模块扫描未命中 `sys_user`、`sys_role`、`sys_menu`、`sys_dept`。
- seller/buyer 账号查询扫描未发现裸单参数 accountId 生产入口，生产代码继续带主体 ID。
- seller/buyer 管理端 controller 会话列表和强退权限已分开：只读列表使用 `*:admin:session:list`，强退使用 `*:admin:forceLogout`。
- seller/buyer resetPwd 当前为人工临时密码入口，未发现 `resetDefaultPwd` 或静默默认密码入口。
- React seller/buyer 管理端会话列表 service 只传 `pageNum` / `pageSize`，未携带主体或账号范围字段。

### Evidence

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `npx jest --config jest.config.ts --runTestsByPath tests\partner-management-contract.test.ts tests\portal-session-request.test.ts tests\terminal-session-token.test.ts tests\remote-menu-route-guard.test.ts tests\admin-auth-sidecar-contract.test.ts --runInBand`：通过；5 suites / 88 tests。
- `mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,TerminalSqlIsolationContractTest,TerminalRoleMenuMapperIsolationContractTest,PortalDirectLoginSupportTest,PortalPermissionCheckerTest,PortalPermissionSupportTest,SellerServiceImplTest,SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,BuyerServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；ruoyi-system 42 tests、seller 88 tests、buyer 89 tests。
- `node scripts\verify-three-terminal.mjs`：通过，最终输出 `three-terminal verification passed`。
- 前端 27 suites / 220 tests passed；后端 reactor `test-compile` 15 个模块 `SUCCESS`；后端三端合同批次 12 个模块 `SUCCESS`，seller 110 tests、buyer 112 tests、product 69 tests、logistics 11 tests 通过。

### Conclusion

- `P0` 未见新增。
- 未坐实新的 P1 编译、guard、接口、权限、串端、service/字段缺口。
- 本轮无业务代码修改，无远端 MySQL DDL/DML，无 Redis 读写，无后端启动或重启。
- 本检查点仍不证明三物理前端、live `/seller/getRouters` / `/buyer/getRouters`、浏览器运行态或 UI 细节验收完成。

## 追加检查点：未跟踪 portal wrapper 与模型规则复核

时间：2026-06-10 19:02，本机 `Asia/Shanghai`。

### 子 Agent

- 本轮未启动新的子 Agent。
- 用户最新模型规则已写入 `AGENTS.md`：只读检查、审查、探索类子 Agent 使用 `GPT-5.3-Codex-Spark`；代码编辑、实现、修复类子 Agent 使用 `gpt5.4`。如工具只接受模型 id，对应使用 `gpt-5.3-codex-spark` / `gpt-5.4`。
- 本轮未坐实需要代码编辑的 P0/P1，因此未启动 `gpt5.4` 实现子 Agent。

### 主线程复核

- `react-ui/src/pages/Seller/Portal/index.tsx` 与 `react-ui/src/pages/Buyer/Portal/index.tsx` 均只委托到 `@/pages/Portal/Home`，未新增端内业务逻辑。
- `react-ui/src/pages/Seller/Portal/index.js` 与 `react-ui/src/pages/Buyer/Portal/index.js` 均只 re-export 对应 `index.tsx`，未绕过 TS 入口。
- `react-ui/src/services/seller-buyer/sessionParams.ts` 只保留 `pageNum` / `pageSize`，不传主体 ID、账号 ID 或其他范围字段。
- `react-ui/tests/admin-auth-sidecar-contract.test.ts` 已固定 seller/buyer portal wrapper 和 JS mirror；`react-ui/tests/partner-management-contract.test.ts` 已固定 session params sanitizer；两者均已登记在 `react-ui/tests/three-terminal.manifest.json`。

### Evidence

- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `git diff --check`：通过，仅有既有 LF/CRLF 提示。
- `node scripts\verify-three-terminal.mjs`：通过，最终输出 `three-terminal verification passed`。
- 前端 27 suites / 220 tests passed；后端 reactor `test-compile` 15 个模块 `SUCCESS`；后端三端合同批次 12 个模块 `SUCCESS`，seller 110 tests、buyer 112 tests、product 69 tests、logistics 11 tests 通过。

### Conclusion

- `P0` 未见新增。
- 未坐实新的 P1 编译、guard、接口、权限、串端、service/字段缺口。
- 本轮无业务代码修改，无远端 MySQL DDL/DML，无 Redis 读写，无后端启动或重启。
- 本检查点仍不证明三物理前端、live `/seller/getRouters` / `/buyer/getRouters`、浏览器运行态或 UI 细节验收完成。
## 追加检查点：live API 与远程库只读证据

### 本轮边界

- 目标文件：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 执行口径：快速推进，只看三端账号权限 P0/P1，不做浏览器、截图、DOM、UI 细调。
- 子 Agent：未启动新的子 Agent；当前规则为只读类使用 `gpt-5.3-codex-spark`，代码编辑类使用 `gpt-5.4`。

### 已验证

- 后端 `8080`、前端 `8001` 当前均有进程监听。
- 后端运行配置仍为 `active: druid`，远程 MySQL / Redis 变量来自 `.env.local`；报告未保存连接明文、密码、token 或密钥。
- 管理端列表接口当前可读：卖家 `code=200,total=14,rows=5`；买家 `code=200,total=11,rows=5`。
- seller 免密代入、seller portal `getInfo/getRouters` 成功：票据 `30` 分钟，权限数 `7`、角色数 `1`、顶层路由数 `1`、首路由 `SellerPortalHome`。
- buyer 免密代入、buyer portal `getInfo/getRouters` 成功：票据 `30` 分钟，权限数 `7`、角色数 `1`、顶层路由数 `1`、首路由 `BuyerPortalHome`。
- 远程库只读检查：端账号密码列为 `varchar(100) not null` 且无空密码；`seller_account` / `buyer_account` 未发现 `user_id` 列；端内菜单、角色菜单、账号角色关系检查无异常。
- 静态扫描：seller/buyer 生产代码未命中 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 等若依后台控制面依赖，也未命中裸单参账号查询入口。
- 测试：manifest、portal token guard、partner management template guard、前端 5 个合同测试 88 条、后端 reactor 窄测试 230 条均通过。

### 未作为完成证据的事项

- 仍未做浏览器级验收、截图、DOM 检测；这是当前快速推进模式下的明确跳过项。
- 三端物理前端目录拆分仍不是当前 P0/P1 完成项。
- 远程库存在 `3` 条 seller、`1` 条 buyer 端账号用户名与 `sys_user.user_name` 重名；对应旧后台 `seller` / `buyer` 角色已为禁用/删除态。当前未发现运行态继续复用 `sys_user`，但该项后续如要清理，应单独走 guarded legacy cleanup 预览、签名和执行记录。

### 结论

- 当前新增证据支持：三端账号权限主链路在运行态可用，seller/buyer 端内账号权限不依赖若依 `sys_*` 作为端内控制面。
- 本轮未坐实新的 P0/P1，因此未做业务代码修复，也未启动代码编辑类子 Agent。

## 追加检查点：三端总门禁复跑

### 边界

- 仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。
- 当前快速推进模式只判断 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 子 Agent

- 尝试启动 2 个只读 explorer，模型均为 `gpt-5.3-codex-spark`。
- 2 个 explorer 均因 Spark 额度限制失败并已关闭。
- 未将只读任务回退到 `gpt5.4`；本轮没有坐实需要代码编辑的 P0/P1。

### 已验证

- 主线程回扫最新记录、当前 dirty tree 和 portal sidecar 后，未发现新的三端账号权限 P0/P1。
- `npm run verify:three-terminal` 通过。
- 前端 27 个 Jest suite、220 条测试通过。
- 后端 reactor test-compile 通过，三端 manifest 对应后端合同测试通过。
- portal token、partner management、seller/buyer portal product、product upstream mirrors guard 均通过。

### 未作为完成证据的事项

- 本轮仍未做浏览器、截图、DOM 检测。
- 三端物理前端拆分仍不在当前 P0/P1 快速推进完成条件内。
- Jest 的 open handle 提示仍存在，命令退出码为 `0`；本轮作为 P2 测试清理噪音记录。

### 结论

- `P0` 未见新增。
- 未坐实新的 `P1` 编译、guard、接口、权限、串端、service/字段缺口。
- 未执行远端 MySQL DDL/DML，未读取或写入 Redis，未启动或重启后端。

## 追加检查点：manifest 与接口覆盖复核

### 边界

- 仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。
- 当前快速推进模式只判断 P0/P1，不做浏览器、截图、DOM 或 UI 细调验收。

### 已验证

- `three-terminal.manifest.json` 覆盖当前 27 个前端 `.test.ts`。
- `three-terminal.manifest.json` 覆盖当前 80 个后端 `*Test.java`。
- seller/buyer controller 共 122 个 Mapping 未发现裸接口。
- seller/buyer 前端 service 导出函数和 API 路径保持同构，无单端漏复制。
- portal session service 仍集中剥离端内身份范围参数。

### 结论

- 未发现新的 P0/P1。
- 本轮无业务代码修改，无远程 MySQL DDL/DML，无 Redis 读写，无后端重启。

## 追加审计：direct-login 子路径公开范围 P1 修复

### 本轮边界

- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 执行口径：快速推进，只修 P0/P1，不做浏览器、截图、DOM、UI 细调。
- 子 Agent：按最新规则尝试 2 个只读 `gpt-5.3-codex-spark` explorer，均因 Spark 额度限制失败并已关闭；未切换到 `gpt5.4` 做只读审查。

### 审计结论

- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx` 原先把 `/seller/direct-login/*`、`/buyer/direct-login/*` 都算作公共 portal 路径；这与 `react-ui/src/utils/portalPaths.ts` 的 direct-login 精确白名单口径不一致。
- 该项会影响三端路由 guard 的 fail-closed 语义，定为 P1。
- 已修复为：
  - `PUBLIC_PORTAL_EXACT_ROUTE_PATHS`：只放行 `/seller/login`、`/buyer/login`、`/seller/direct-login`、`/buyer/direct-login` 精确路径。
  - `PUBLIC_PORTAL_PREFIX_ROUTE_PATHS`：只对 `/seller/portal`、`/buyer/portal` 子树使用前缀匹配。
  - `/seller/direct-login/next`、`/buyer/direct-login/next` 回落到管理端静态权限 fallback。

### 验证证据

- `npx jest --config jest.config.ts --runTestsByPath tests\remote-menu-route-guard.test.ts tests\portal-session-request.test.ts tests\terminal-session-token.test.ts --runInBand`：3 suites / 44 tests 通过。
- `node scripts\check-portal-token-isolation.mjs`：通过。
- `node scripts\check-partner-management-template.mjs`：通过。
- `node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `npm run verify:three-terminal`：通过；前端 27 suites / 220 tests，后端 reactor `test-compile` 与 manifest 后端合同测试均通过。

### 未作为完成证据的事项

- 未做浏览器运行态验收、截图、DOM 检测。
- 未验证三端物理前端拆分完成。
- 未执行远端 MySQL DDL/DML，未读写 Redis，未启动或重启后端。
