# Portal 401 端 Token 隔离执行记录

日期：2026-06-05

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 目标

只处理一类问题：seller/buyer portal 请求返回 401 时，只清理对应端 token，不清理管理端 `access_token`、`admin_remote_menu`，也不跳管理端登录页。

本轮不改后端、不执行 SQL、不改管理端业务页面、不启动三端物理前端拆分。

## 已完成

- 新增 `react-ui/src/utils/portalRequest.ts`，根据请求 URL 判断 portal terminal。
- 明确排除 `/api/seller/admin/**` 和 `/api/buyer/admin/**`，避免管理端后台接口被误判为 portal 请求。
- 更新 `react-ui/src/app.tsx` 响应拦截器：业务 `code=401` 且属于 portal API 时，只调用 `clearTerminalSessionToken(portalTerminal)`。
- 更新 `react-ui/src/requestErrorConfig.ts`：BizError 和 HTTP status 401 同样按请求 URL 区分 portal / admin。
- 更新 `react-ui/scripts/check-portal-token-isolation.mjs`，把 portal 401 隔离纳入静态守卫。
- 更新 `docs/architecture/reuse-ledger.md` 和目标追踪记录。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run guard:portal-token
```

结果：通过，输出 `Portal token isolation guard passed.`

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/app.tsx src/requestErrorConfig.ts src/utils/portalRequest.ts scripts/check-portal-token-isolation.mjs
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome check src/utils/portalRequest.ts scripts/check-portal-token-isolation.mjs
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc -- --pretty false
```

结果：通过。

## 未验证原因

- 未运行浏览器 smoke。本轮是前端请求错误处理和静态守卫切片，不涉及页面布局或后端接口变更；后续做 portal 页面回归时可复跑 seller/buyer portal smoke。
- 未执行 SQL。本轮不涉及数据库结构或数据。

## 权限检查结果

- 本轮不新增后端权限点。
- portal API 与管理端后台 API 的 401 分支已区分：
  - `/api/seller/**`、`/api/buyer/**` 的非 admin 请求按 portal token 处理。
  - `/api/seller/admin/**`、`/api/buyer/admin/**` 继续按管理端 admin session 处理。

## 字典/选项复用检查结果

本轮不涉及字典、选项或业务 code/label。

## 复用台账检查结果

已更新 `docs/architecture/reuse-ledger.md` 的三端前端 session 基础层规则。

## CodeGraph 更新结果

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：通过；首次输出 `Synced 4 changed files`，记录回填后最终复跑输出 `Already up to date`。

## 大文件合理性判断结果

- `react-ui/src/app.tsx` 是既有运行时配置文件，本轮只做小范围 401 分支接入，不重构整文件。
- `react-ui/src/requestErrorConfig.ts` 是既有请求错误处理配置，本轮只补 portal URL 判定，不拆分。
- 新增 `react-ui/src/utils/portalRequest.ts` 体量小、职责单一。

## 重复代码检查结果

- portal API URL 判定集中在 `getPortalTerminalFromApiUrl(...)`，避免 `app.tsx` 和 `requestErrorConfig.ts` 分别维护 seller/buyer URL 判断。
- 静态守卫统一放在已有 `check-portal-token-isolation.mjs`，没有新增第二套 portal token guard。

## 残留问题

- 子 agent 审计指出锁定/解锁权限契约测试仍可补强，适合后续独立测试切片。
- 子 agent 审计指出三端 SQL seed 对软删 Owner 角色存在幂等冲突风险，适合后续独立 SQL 核验切片。
