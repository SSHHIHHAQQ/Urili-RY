# 2026-06-07 三端 P0/P1 快速推进：Portal 401 Redirect 与 Reject 记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮边界

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 本轮只修改 `react-ui` 的 portal 401 前端行为、静态 guard 和 Jest 测试。

## 子 Agent 使用情况

- 本轮沿用同日上一检查点已确认的 `gpt-5.3-codex-spark` 额度限制结果，未再次消耗尝试；直接启动 6 个 `gpt-5.4` 只读子 Agent。
- 6 个子 Agent 覆盖 portal 401 行为、portal 401 guard、Jest 落点、TS/JS 镜像、旧 SQL `MODIFY` 可重放性和 `2010` 菜单 owner 兼容双写。
- 本轮采纳 portal 401 P1：portal 401 不保留 redirect，且 `app.tsx` 响应体 `code/errorCode = 401` 分支处理后仍返回 response。
- 旧 SQL `MODIFY` 和 `2010` 菜单 owner 兼容双写仍记录为后续 P1，不混入本前端切片。

## 新增问题

- P1：`requestErrorConfig.ts` / `app.tsx` 命中 portal 401 后只跳 `/seller/login` 或 `/buyer/login`，没有携带当前 portal 页面 `redirect`，登录后不能回到原页面。
- P1：`app.tsx` 响应拦截器发现 HTTP 200 响应体 `code/errorCode = 401` 后会清 token 并跳登录，但仍 `return response`，业务页面可能继续当成功结果消费。

## 已修复问题

- `requestErrorConfig.ts` / `requestErrorConfig.js`
  - 新增 `redirectToPortalLogin(...)`，portal 401 跳对应端登录页时携带当前路由 `redirect`。
  - 保留非 portal 401 的管理端登录跳转逻辑。
- `app.tsx` / `app.js`
  - 同步新增 `redirectToPortalLogin(...)`。
  - 响应体 401 分支处理后改为 `Promise.reject(response)`，避免成功链继续执行。
- `scripts/check-portal-token-isolation.mjs`
  - portal 401 静态 guard 改为要求调用 `redirectToPortalLogin(portalTerminal)`。
  - 新增 `redirect` 参数保留检查。
  - 新增 `app.tsx` / `app.js` 响应体 401 后必须 reject 的静态检查。
- `tests/portal-unauthorized-redirect.test.ts`
  - 新增 portal 401 只清对应端 token、保留 redirect、body 401 reject、admin 401 不误走 portal 的 Jest 行为测试。
- `AGENTS.md` 和 `docs/architecture/reuse-ledger.md`
  - 同步 portal 401 redirect 与 body 401 reject 规则。

## 残留问题

- `20260604_three_terminal_isolation_migration.sql` 仍有两条裸 `ALTER TABLE ... MODIFY`，子 Agent 已确认为后续 P1。
- `sys_menu 2010` 仍存在 top owner + 兼容 seed 双写同签名，子 Agent 已确认为后续 P1。
- 本轮未运行浏览器、截图、DOM 或 UI 细调验收。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- tests/portal-unauthorized-redirect.test.ts tests/portal-session-request.test.ts tests/terminal-session-token.test.ts --runInBand`
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`

## 验证结果

- `npm run guard:portal-token`：通过。
- `npm run test:unit -- tests/portal-unauthorized-redirect.test.ts tests/portal-session-request.test.ts tests/terminal-session-token.test.ts --runInBand`：通过，`3` 个 suite / `10` 个测试通过。
- `npm run tsc`：通过。

## 未验证原因

- 未执行远程 SQL、未读取或写入 Redis：本轮是前端 portal 401 行为切片，不需要动运行库。
- 未做浏览器、截图、DOM 或 UI 细调验收：用户已明确当前快速模式无需浏览器运行态验收。
- 未运行完整 `verify-three-terminal`：本轮只改前端 401 行为，已运行 portal guard、相关 Jest 和 TypeScript 编译；完整验证可在后续合并检查点统一跑。

## 权限检查结果

- 本轮不新增后端接口、菜单或按钮权限。
- `/api/seller/admin/**` 和 `/api/buyer/admin/**` 仍由 `getPortalTerminalFromApiUrl(...)` 排除，不会误按 portal 401 清端内 token。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或前端下拉。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 portal 401 redirect 与 body 401 reject 规则。

## CodeGraph 更新结果

- `codegraph sync .`：通过，返回 `Synced 6 changed files`，`Added: 1, Modified: 5 - 172 nodes`。

## 大文件合理性判断结果

- `app.tsx` 已超过 300 行，但本轮只在既有 401 处理和响应拦截器内小范围补行为，不改变职责边界。
- `check-portal-token-isolation.mjs` 已超过 500 行，但它本身就是 portal token/session 静态 guard 集合；本轮只补同一 guard 主题，不拆分。

## 重复代码检查结果

- 当前仓库仍保留 `.tsx/.ts` 与 `.js` 镜像，本轮按既有模式同步维护。
- 401 redirect helper 在 `requestErrorConfig` 和 `app` 各保留一份，因为两处分别是 Umi request 错误处理和运行时响应拦截入口，后续如统一抽公共工具再单独处理。
