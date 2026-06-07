# 2026-06-07 三端 P0/P1 Portal JS Sidecar Guard 收口记录

## 背景

- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 当前模式：只修 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。
- 本轮不做浏览器运行态验收、不做截图、不做 DOM 检测、不做 UI 细调。

## 问题

- `react-ui/src/utils/portalPaths.ts` 存在端路由识别逻辑，但缺少同名 `.js` sidecar。
- `react-ui/scripts/check-portal-token-isolation.mjs` 只对 `react-ui/src/services/portal/session.ts` 做精确的 `isToken:false`、参数清洗和端内 query 安全检查，没有对 `session.js` 做同等断言。
- 在当前 `react-ui/src` 仍保留 TS/JS 双入口的阶段，只检查 TS 会留下 JS sidecar 漂移风险。
- 降级后的 `gpt-5.4` 只读子 Agent 继续指出：`RemoteMenuRouteGuard.*`、`services/session.*`、`remoteMenuStorage.*` 未被 portal token guard 显式覆盖，route guard 和远程菜单 scope 这条链路存在 guard coverage P0。
- 同一子 Agent 指出：portal 登录页 redirect 端校验、portal 首页 token gate、管理端 direct-login caller 的 terminal 传递链未被 guard 显式覆盖，属于 P1 guard coverage 缺口。

## 子 Agent 使用

- 按用户要求，优先尝试 `gpt-5.3-codex-spark` 子 Agent。
- 平台返回用量限制后，已关闭失败 Agent。
- 随后降级启动 `gpt-5.4` 只读子 Agent 做旁路检查；主 Agent 本地继续推进，不等待其阻塞当前 P0/P1 补丁。

## 已完成

- 新增 `react-ui/src/utils/portalPaths.js`，与 `portalPaths.ts` 保持同一套 seller/buyer 登录页、直登入口和 portal 路由识别规则。
- 新增 `react-ui/src/pages/Portal/Login/index.js`，作为 `index.tsx` 的 JS sidecar re-export。
- `react-ui/scripts/check-portal-token-isolation.mjs` 改为同时检查：
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/services/portal/session.js`
  - `react-ui/src/utils/portalPaths.ts`
  - `react-ui/src/utils/portalPaths.js`
  - `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx`
  - `react-ui/src/wrappers/RemoteMenuRouteGuard.js`
  - `react-ui/src/services/session.ts`
  - `react-ui/src/services/session.js`
  - `react-ui/src/utils/remoteMenuStorage.ts`
  - `react-ui/src/utils/remoteMenuStorage.js`
  - `react-ui/src/pages/Portal/Login/index.tsx`
  - `react-ui/src/pages/Portal/Login/index.js`
  - `react-ui/src/pages/Portal/Home/index.tsx`
  - `react-ui/src/pages/Portal/Home/index.js`
  - `react-ui/src/pages/Seller/index.tsx`
  - `react-ui/src/pages/Buyer/index.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
- 新增 guard 断言包括：
  - seller/buyer 静态路由兜底权限。
  - portal 登录、直登、工作台公开路径豁免。
  - 远程菜单缓存 key 必须按 `admin` / `seller` / `buyer` scope 区分。
  - 空 route authority 必须 fail-closed 返回 403。
  - portal 登录 redirect 必须属于当前 terminal。
  - portal 首页加载前必须校验当前 terminal token。
  - 管理端免密登录 opener bridge 必须传入 `config.moduleKey`。
- `docs/architecture/reuse-ledger.md` 已补充 portal session/path TS/JS sidecar 复用规则。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 已补充本检查点。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\check-portal-token-isolation.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过，输出 `Portal token isolation guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/terminal-session-token.test.ts tests/portal-session-request.test.ts tests/remote-menu-route-guard.test.ts tests/portal-unauthorized-redirect.test.ts --runInBand`：通过，4 个 suite、19 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check -- react-ui\src\utils\portalPaths.js react-ui\src\pages\Portal\Login\index.js react-ui\scripts\check-portal-token-isolation.mjs docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\plans\2026-06-07-three-terminal-p0p1-portal-js-sidecar-guard-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，`Synced 3 changed files`，`Added: 2, Modified: 1 - 102 nodes in 797ms`。

## 未验证

- 未运行浏览器、截图或 DOM 检测，符合当前快速推进边界。
- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未重启后端。

## 残留

- 历史残留已收口：`source_warehouse_stock_read_model.sql` 专项 replay-safe 合同已由后续 P1 切片完成；fresh bootstrap 策略仍需单独收口。
