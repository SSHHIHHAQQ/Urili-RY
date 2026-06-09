# 三端 P0/P1 静态卖家/买家路由 Guard 补强记录

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按当前快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

本切片处理的问题是：管理端真实菜单路径已通过远程菜单 patch 绑定权限，但 `react-ui/config/routes.ts` / `routes.js` 中仍存在静态 `/seller`、`/buyer` fallback 路由。如果远程菜单尚未注入、或有人直达这些静态路径，必须同样经过管理端权限 guard，不能成为无权限直达页面。

## 本次改动

- 导出 `react-ui/src/services/session.ts` 中已有的 `RemoteMenuRouteGuard`，供静态 route wrapper 复用。
- 新增 `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx` 与 `.js` 镜像文件：
  - 优先读取 Umi route 上的 `authority`。
  - 对静态 `/seller`、`/buyer` 保留 fallback 权限映射。
  - 使用同一个 `RemoteMenuRouteGuard` 判定 `access.hasPerms(...)`。
- `react-ui/config/routes.ts` 与 `routes.js` 中：
  - `/seller` 增加 `authority: ['seller:admin:list']`。
  - `/buyer` 增加 `authority: ['buyer:admin:list']`。
  - 两者均增加 `wrappers: ['@/wrappers/RemoteMenuRouteGuard']`。
- 补强 `react-ui/scripts/check-partner-management-template.mjs`：
  - 固定静态 `/seller`、`/buyer` 路由必须带 `authority` 和 wrapper。
  - 固定 wrapper 必须覆盖 `/seller`、`/buyer` fallback 权限。
  - 固定 `RemoteMenuRouteGuard` 必须导出。
- 新增 `react-ui/tests/remote-menu-route-guard.test.ts`：
  - 有任一匹配权限时渲染子内容。
  - 缺少权限时渲染 403。
- 更新 `react-ui/scripts/verify-three-terminal.mjs`，把新增 Jest 测试纳入三端总验证清单。
- 更新 `react-ui/tests/portal-session-request.test.ts`，固定 `/seller`、`/buyer` 页面路径不能被误判为 seller/buyer 端内 API 请求。
- 历史记录（已过期口径）：按用户最新要求更新 `AGENTS.md`：子 Agent 模型优先使用 GPT-5.3 Codex（工具模型 `gpt-5.3-codex-spark`）；不可用再回退 `gpt-5.4`，并记录原因、模型和数量。

## 子 Agent 使用情况

本切片未新增子 Agent。后续如需继续并行检查，按 `AGENTS.md` 新规则执行：优先 GPT-5.3 Codex；若不可用，再使用 `gpt-5.4`，并在记录中说明不可用原因和实际使用情况。（历史记录，已过期口径）

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\check-partner-management-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过，输出 `Partner management template guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/remote-menu-route-guard.test.ts tests/portal-session-request.test.ts --runInBand`：通过，`2` 个 test suite、`5` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，输出 `three-terminal verification passed.`；前端 `4` 个 test suite、`11` 个测试通过；后端 ruoyi-system `100`、ruoyi-framework `15`、product `1`、seller `80`、buyer `81` 测试通过。

## 数据源与运行边界

- 本切片未连接远程 MySQL / Redis。
- 本切片未执行 SQL，未写远程数据。
- 本切片未启动浏览器，未做截图、DOM 检测或 UI 细调。

## P2 记录

- 当前只补静态 fallback 路由 guard；管理端菜单真实入口仍以远程菜单和后端 `sys_menu` 权限为准。
- 若后续拆出 `seller-ui` / `buyer-ui`，端内前端不应复用此管理端 wrapper；端内权限应走 seller/buyer 自己的端内菜单和权限模型。

## 2026-06-07 回归保护补强

本轮继续处理同一问题域：静态 `/seller`、`/buyer` fallback guard 已有实现，但缺少 wrapper fallback 的执行级回归测试，且尾斜杠路径 `/seller/`、`/buyer/` 可能因为 `location.pathname` 精确匹配而漏掉 fallback authority。

### 子 Agent 使用情况

- 本轮使用并关闭 4 个 `gpt-5.4` 只读子 Agent。
- 子 Agent 结论汇总：
  - 当前 `/seller`、`/buyer` 主路径实现已闭合，缺口主要是回归保护和尾斜杠归一化。
  - 不建议引入宽泛 `/seller/*`、`/buyer/*` 管理端路由，否则可能误伤 `/seller/login`、`/seller/direct-login`、`/seller/portal` 及对应 buyer 端入口。
  - `remoteMenuStorage.ts` / `.js` 作为 scoped key 镜像应被 guard 脚本直接覆盖。

### 追加改动

- `RemoteMenuRouteGuard` wrapper 增加 `getStaticRouteAuthority(...)`：
  - `/seller`、`/seller/` 回填 `seller:admin:list`。
  - `/buyer`、`/buyer/` 回填 `buyer:admin:list`。
  - `/seller/login`、`/seller/direct-login`、`/seller/portal` 及其子路径不被误判为管理端 seller 权限；buyer 同理。
  - 显式 `route.authority` 仍优先于静态 fallback。
- `remote-menu-route-guard.test.ts` 增加 wrapper 执行级断言：
  - seller/buyer fallback authority 映射。
  - buyer 权限不能通过 seller fallback。
  - 显式 route authority 优先于 fallback。
  - portal 入口前缀不被静态管理端 fallback 捕获。
- `check-partner-management-template.mjs` 增加：
  - wrapper helper、portal 公开入口例外和 `startsWith` 归一化检查。
  - `remoteMenuStorage.ts` / `.js` scoped key 镜像检查。

### 追加验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/remote-menu-route-guard.test.ts --runInBand`：通过，`1` 个 suite、`9` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，前端 `4` 个 suite、`18` 个测试通过；后端 ruoyi-system `114`、ruoyi-framework `15`、product `1`、seller `83`、buyer `84` 测试通过。
- 旧残留文本复查：通过，未发现把本项继续列为待办的过期表述。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅输出既有 LF/CRLF 换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 4 changed files`，`Modified: 4 - 64 nodes in 1.0s`。

### 边界说明

- 本轮未连接远程 MySQL / Redis。
- 本轮未执行 SQL，未写远程数据。
- 本轮未启动浏览器，未做截图、DOM 检测或 UI 细调。
- 本轮不新增 `/seller/*`、`/buyer/*` 管理端静态宽路由，避免误伤端内 portal 入口。
