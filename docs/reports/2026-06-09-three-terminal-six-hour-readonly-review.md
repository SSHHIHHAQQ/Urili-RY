# 2026-06-09 三端权限改造六小时只读审查

- 审查时间：2026-06-09 13:42-14:00，Asia/Shanghai
- 审查窗口：2026-06-09 07:40:35 之后到本次审查
- 审查模式：只读审查；未修改业务代码；未执行 DDL/DML；未读取或写入 Redis
- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 1. 最近 6 小时完成了什么

### 已提交变更

当前窗口内主要提交为 `6a04df7 feat: add product review and inventory adjustment workflows`。提交范围很大，既包含商品审核、库存调整、库存同步策略等业务能力，也包含三端权限改造相关的 guard / contract / SQL seed 加固。

三端权限方向相关的实质完成项主要包括：

- seller / buyer 后端隔离、portal auth / direct-login / session / log、SQL guard、React route / request / token、verify gate 等多切片 P0/P1 审查与收口记录。
- `seller_buyer_management_seed.sql`、端内菜单 ID 区间、owner role-menu/account-role 完成态、SQL confirm guard、sys_menu owner/signature guard 等合同继续补强。
- React 侧新增或强化 portal token 隔离、401 分流、remote menu 空 authority 拒绝、JS sidecar/mirror、manifest 收录、admin service 前缀等回归测试。
- 管理端 seller / buyer 账号、角色、菜单、部门、会话、免密代入、日志审计的页面与 service 同构复核完成，未发现 service URL / 权限前缀 / 端配置串端。
- 远程运行库 schema 曾做只读复核，目标追踪记录显示三端核心表、password 约束、菜单 ID 区间、role-menu/account-role、direct-login 审计字段等均通过。

### 当前未提交变更

当前工作区仍有未提交改动。和三端权限直接相关的是：

- `AGENTS.md` 将子 Agent 规则收紧为只能使用 `gpt-5.4`。
- `react-ui/scripts/check-portal-token-isolation.mjs` 增加 dev `'/api/'` proxy 结构合同：`target: apiProxyTarget`、`changeOrigin: true`、`pathRewrite: { '^/api': '' }`。
- `react-ui/scripts/verify-three-terminal.mjs` 和 `react-ui/tests/three-terminal.manifest.json` 增加 `criticalFrontendExplicitTestPaths`，显式固定关键前端测试必须进入三端验证 manifest。
- 新增只读审计报告：portal auth/direct-login/session/log、SQL/migration/seed/guard、React 管理端控制面、React portal auth/request、verify gate。
- seller / buyer / ruoyi-system 生产代码当前没有新的未提交改动；未提交业务 diff 主要在库存/商品审核 UI 和验证记录。

## 2. 方向偏差与 P0/P1 风险

本次审查未发现新的可坐实 P0/P1。

方向判断：

- 符合“管理端保留若依 `sys_*`，seller/buyer 端内账号、角色、菜单、部门、日志、会话独立”的主方向。
- 未发现 seller/buyer 生产代码继续用 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 承载端内账号权限。
- 未发现裸 `select*AccountById(accountId)` 生产入口；现有 service / mapper 入口均带 `sellerId/buyerId + accountId`。
- portal 控制器的业务 endpoint 均有 `@PortalPreAuthorize` 和 `@PortalLog`，方法内从 `PortalSessionContext.requireSession(...)` 取当前端会话。
- React portal request 会剥离前端可注入的 `sellerId/buyerId/subjectId/accountId` 范围参数，避免由前端决定数据范围。
- 当前推进仍是权限框架、guard、最小端内菜单/权限模板和共享业务读接口模板，没有看到铺完整 seller/buyer 业务菜单的偏移。

保留的非阻塞风险：

- P2：seller/buyer 自助接口仍常见 `@Anonymous + @PortalPreAuthorize` 组合。当前鉴权链实际会校验 portal session 和权限，但注解语义后续可收窄。
- P2：`react-ui/src/app.tsx` 与 `react-ui/src/requestErrorConfig.ts` 各维护一份 portal 401 分流逻辑，当前一致，后续有漂移风险。
- P2：`react-ui` 同时存在 `jest.config.ts` 和 `jest.config.js`，定向 Jest 需要显式 `--config jest.config.ts`。
- P2：legacy `sys_user` 回填 helper 仍作为受确认门禁保护的一次性迁移脚本存在；当前未发现运行态依赖。
- P2：工作区有 `.codegraph/`、`.umi-test/`、`react-ui/src/.umi-undefined/`、`node_modules/`、`react-ui/test-results/` 等本地生成或依赖目录，提交前需要继续排除。

## 3. 已验证与未验证

本次审查实际执行：

```powershell
git status --short
git log --since='2026-06-09 07:40:35 +0800' --date=iso --name-status -- ...
git diff --stat -- ...
rg -n "sys_user|sys_role|sys_menu|sys_dept|SysUser|SysRole|SysMenu|SysDept|SecurityUtils|LoginUser" RuoYi-Vue/seller/src/main RuoYi-Vue/buyer/src/main
rg -n "select[A-Za-z]*AccountById\s*\(|select.*AccountById\s*\(|AccountById\s*\(" RuoYi-Vue/seller/src/main RuoYi-Vue/buyer/src/main RuoYi-Vue/ruoyi-system/src/main
node scripts\verify-three-terminal.mjs --check-manifest
node scripts\check-portal-token-isolation.mjs
git diff --check -- ...
```

本次命令结果：

- `three-terminal manifest check passed.`
- `Portal token isolation guard passed.`
- `git diff --check` 通过，仅有 LF/CRLF 工作区换行提示。
- 静态搜索未发现 seller/buyer 生产代码直接依赖 `sys_*` 控制面；命中的 `SecurityUtils` 属于管理端操作人、密码加密/匹配或审计 actor，不是复用若依账号表。
- 静态搜索未发现单参数裸 accountId 查询入口；现有 `selectSellerAccountById` / `selectBuyerAccountById` 均为双参数 service 方法。

本次未执行：

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动、停止或重启后端。
- 未运行完整 `node scripts\verify-three-terminal.mjs`。
- 未运行 Maven/Jest 全量或窄测试。
- 未做浏览器、截图、DOM、UI 细调验证。
- 未运行 `codegraph sync .`，因为本次没有业务代码更新；仅新增本只读审查报告。

## 4. 下一步建议优先级

P0/P1 优先级：

1. 提交或合并当前未提交变更前，复跑完整 `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`，因为当前 manifest / proxy guard /库存审核测试都有未提交变更。
2. 对当前新增/未跟踪的审计报告、执行记录和生成目录做提交范围确认；继续排除 `.codegraph/`、`.umi-test/`、`.umi-undefined/`、`node_modules/`、`test-results/`。
3. 如果继续推进三端权限，只处理 guard、权限注解、接口、SQL fail-closed、端内会话/日志审计和最小菜单模板；不要扩展完整 seller/buyer 业务菜单。

P2 后续：

1. 抽一个共享 helper 统一 `app.tsx` 与 `requestErrorConfig.ts` 的 portal 401 分流。
2. 梳理 `@Anonymous + @PortalPreAuthorize` 的注解语义，让 portal session endpoint 更直观。
3. 把定向 Jest 入口摩擦独立收口，避免后续审计只能依赖完整 verifier。
4. 继续把共享业务域 public service API 收窄，但不作为当前三端权限 P0/P1 阻塞项。
