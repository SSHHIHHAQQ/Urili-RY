# 端内前端登录持久化 terminal 守卫执行记录

日期：2026-06-05

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为当前方向。

本轮只处理一类问题：前端端内登录 token 持久化必须校验期望 terminal，避免 seller/buyer 端 token 串写，也避免 portal 请求回退使用管理端 `access_token`。

不处理的内容：

- 不复制买家后端日志模板。
- 不改后端接口、SQL、菜单或权限。
- 不改管理端登录 token 存储。
- 不开始 `seller-ui` / `buyer-ui` 物理拆分。

## 已完成

- 更新 `react-ui/src/pages/Portal/terminal.ts`：
  - `persistPortalLogin(result, expectedTerminal)` 必须接收期望端类型。
  - 如果响应缺少 token 或响应 `terminal` 与当前端不一致，只清理当前页面端 token 并返回 `false`，不清理响应声明的另一端 token。
  - 只有校验通过时才写入当前端 `seller_*` 或 `buyer_*` token key。
- 更新 `react-ui/src/pages/Portal/DirectLogin/index.tsx`：
  - 免密登录成功后统一调用 `persistPortalLogin(response.data, terminal)`。
  - 页面不再直接承担端 token 写入判断。
- 新增 `react-ui/scripts/check-portal-token-isolation.mjs`：
  - 禁止 `src/pages/Portal/**` 和 `src/services/portal/**` 直接使用管理端 `getAccessToken`、`setSessionToken`、`clearSessionToken`。
  - 禁止 portal 目录出现裸 `access_token` / `portal_login_token`。
  - 校验 `src/services/portal/session.ts` 每个 portal request 都显式 `isToken:false`。
- 更新 `react-ui/package.json`：
  - 新增 `guard:portal-token`。
  - 将 `guard:portal-token` 接入 `npm run lint` 前置步骤。
- 更新 `docs/architecture/reuse-ledger.md`，登记端内前端 token 持久化和静态守卫规则。

## 并行审计结论

- 当前 seller/buyer portal 代码没有直接调用管理端 `getAccessToken` / `setSessionToken` / `clearSessionToken`。
- 当前 portal service 通过 `getTerminalAccessToken(terminal)` 构造端内 Authorization。
- 当前 portal 请求都设置了 `isToken:false`，避免全局请求拦截器注入管理端 token。
- 残留模板风险是以后新增 portal API 时忘记 `isToken:false`；本轮已通过静态守卫脚本锁住。
- 2 个子 agent 均已关闭。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint scripts\check-portal-token-isolation.mjs src\pages\Portal\terminal.ts src\pages\Portal\DirectLogin\index.tsx package.json`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run lint`：未通过；新增 `guard:portal-token` 已通过，失败发生在既有 Biome 全仓问题，例如 `src/components/DictTag/index.tsx`、`src/components/IconSelector/style.module.css`、`src/components/IconSelector/themeIcons.tsx` 和 `src/pages/Monitor/Druid/index.tsx`，不属于本切片引入。
- `git diff --check -- react-ui\src\pages\Portal\terminal.ts react-ui\src\pages\Portal\DirectLogin\index.tsx react-ui\scripts\check-portal-token-isolation.mjs react-ui\package.json docs\architecture\reuse-ledger.md docs\plans\2026-06-05-portal-token-persist-terminal-guard-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，本轮代码变更后的首次同步输出 `Synced 3 changed files`；记录回填后最终复跑输出 `Already up to date`。

## 当前判断

- 端内前端 token 持久化已具备 terminal 校验防线。
- 新增端内 portal 前端请求时，如果忘记 `isToken:false` 或误用管理端 token API，会被 `guard:portal-token` 捕获。
- 本轮没有越过“卖家日志模板验收后复制买家”的边界。
