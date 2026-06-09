# verify gate / 合同测试覆盖审计（任务切片 6）

- 日期：2026-06-09
- 范围：
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `react-ui/tests/three-terminal.manifest.json`
  - `react-ui/tests/*` 中三端关键前端合同测试
  - `RuoYi-Vue/pom.xml`
  - `RuoYi-Vue/**/src/test/java/**/*Test.java`
- 方式：只读审计，不改业务代码；补充运行了 `node scripts/verify-three-terminal.mjs --check-manifest`
- 目标：只找 P0/P1，聚焦编译、guard、接口、权限、串端、service/字段缺失，以及 verify gate 的覆盖缺口/硬编码风险

## 结论

- 未发现当前会直接导致 gate 漏跑、manifest 漂移或 reactor 模块硬编码失效的 **P0**。
- 本文件原始只读审计发现的 2 个 **P1** 已在主线程采纳并关闭：
  1. dev proxy 关键转发契约已纳入 `check-portal-token-isolation.mjs`，并由 `verify-three-terminal` 的 `guard:portal-token` 步骤执行。
  2. 前端关键测试已新增 `criticalFrontendExplicitTestPaths` 显式清单，并由 `verify-three-terminal.mjs --check-manifest` 校验必须进入 `frontendTestPaths`。
- 当前状态：无待处理 P0/P1；本文件保留原始发现和修复证据，避免后续把已关闭问题误判为仍未处理。

## P0/P1 Findings（已关闭）

### P1-1：历史缺口，`proxy.ts` 曾缺少关键转发契约校验

状态：已修复并关闭。

- 文件：
  - [react-ui/scripts/check-portal-token-isolation.mjs](E:\Urili-Ruoyi\react-ui\scripts\check-portal-token-isolation.mjs#L1005)
  - [react-ui/config/proxy.ts](E:\Urili-Ruoyi\react-ui\config\proxy.ts#L12)
  - [react-ui/tests/system-user-service-contract.test.ts](E:\Urili-Ruoyi\react-ui\tests\system-user-service-contract.test.ts#L7)
- 证据：
  - `check-portal-token-isolation.mjs` 对 `proxy.ts` 的检查只覆盖：
    - 不能写死 `http://localhost:8080`
    - 必须支持 `API_PROXY_TARGET`
  - 它没有断言这些 dev proxy 关键条件仍然存在：
    - `'/api/'` 代理入口必须存在
    - `pathRewrite: { '^/api': '' }` 必须保持
    - `changeOrigin: true` 必须保持
  - `system-user-service-contract.test.ts` 只验证 service 仍走 `'/api/system/user/authRole'`，不能覆盖 proxy 层 rewrite 漂移。
- 风险：
  - 一旦有人把 `react-ui/config/proxy.ts` 的 rewrite 改坏，例如误改成 `'^': ''`、删掉 `'/api/'` 入口、或去掉 `changeOrigin`，当前 `verify-three-terminal` 仍可能通过。
  - 这会把 seller/buyer/admin 的所有前端 API 调用拖成运行时故障，属于快速推进模式下应被 gate 直接拦截的 P1。
- 建议：
  - 已在 `check-portal-token-isolation.mjs` 补对 `proxy.ts` 的结构性断言：
    - `dev['/api/']` 必须存在
    - `target` 必须来自 `apiProxyTarget`
    - `pathRewrite` 必须是 `'^/api' -> ''`
    - `changeOrigin` 必须为 `true`
  - 该 guard 已由完整 `node scripts\verify-three-terminal.mjs` 执行。

### P1-2：历史缺口，前端关键测试曾缺少显式关键清单

状态：已修复并关闭。

- 文件：
  - [react-ui/scripts/verify-three-terminal.mjs](E:\Urili-Ruoyi\react-ui\scripts\verify-three-terminal.mjs#L35)
  - [react-ui/tests/three-terminal.manifest.json](E:\Urili-Ruoyi\react-ui\tests\three-terminal.manifest.json#L115)
- 证据：
  - backend 侧同时具备：
    - `criticalBackendTestClassPattern`
    - `criticalBackendExplicitTestClasses`
    - 且 manifest 里显式声明 `criticalBackendExplicitTestClasses`
  - frontend 侧只有：
    - `criticalFrontendTestPathPattern`
    - `frontendTestPaths`
  - 没有与 backend 对称的 `criticalFrontendExplicitTestPaths`。
- 风险：
  - 现在 frontend 关键测试是否必须登记到 manifest，主要依赖文件名命中这个正则：
    - `terminal|portal|partner|remote-menu|getrouters|authority|auth-sidecar|...`
  - 如果后续新增一个真实关键合同测试，但文件名没有命中这些关键词，例如更偏业务/模块命名，gate 不一定会报“critical frontend test files are not included in three-terminal manifest”。
  - 这属于 verify gate 的硬编码治理风险：当前代码库能过，但以后扩面时容易出现“测试文件存在，但 verify gate 不强制执行”的漏网点。
- 建议：
  - 已在 manifest 中新增 `criticalFrontendExplicitTestPaths`。
  - `verify-three-terminal.mjs --check-manifest` 已校验显式关键清单必须都出现在 `frontendTestPaths`。
  - 当前已显式纳管的平台级 contract 测试包括：
    - `tests/portal-session-request.test.ts`
    - `tests/portal-unauthorized-redirect.test.ts`
    - `tests/remote-menu-route-guard.test.ts`
    - `tests/admin-auth-sidecar-contract.test.ts`
    - `tests/static-route-authority-contract.test.ts`

## 无 P0 的直接证据

### 1. manifest 当前未漂移

- 命令：`node scripts/verify-three-terminal.mjs --check-manifest`
- 结果：`three-terminal manifest check passed.`
- 说明：
  - 当前 `frontendTestPaths`、`frontendGuardScripts`、`backendTestClasses` 与现存文件是一致的。

### 2. 后端 reactor 模块当前不是硬编码列表

- 文件：
  - [react-ui/scripts/verify-three-terminal.mjs](E:\Urili-Ruoyi\react-ui\scripts\verify-three-terminal.mjs#L231)
  - [RuoYi-Vue/pom.xml](E:\Urili-Ruoyi\RuoYi-Vue\pom.xml#L232)
- 证据：
  - `readBackendReactorModules()` 从 `RuoYi-Vue/pom.xml` 读取 `<module>`。
  - `getBackendTestSourceRoots()` 再按存在 `src/test/java` 的模块派生测试源根。
  - `getBackendTestModules()` 按 manifest 指定的测试类反推需要的模块，并在 Maven 命令中使用 `-pl <动态模块列表> -am`。
- 结论：
  - 当前实现满足“不要长期把 Maven `-pl` 模块清单硬编码成少数模块”的要求。

### 3. 后端 manifest 当前覆盖了仓库内所有现存 `*Test.java`

- 对比结果：`react-ui/tests/three-terminal.manifest.json` 中 `backendTestClasses` 与 `RuoYi-Vue/**/src/test/java/**/*Test.java` 的 basename 对比无差异。
- 说明：
  - 当前不存在“测试类已落库，但 manifest 未列出”的现成缺口。

### 4. 前端三端关键面当前已有直接覆盖证据

- token / portal 会话隔离：
  - [react-ui/tests/terminal-session-token.test.ts](E:\Urili-Ruoyi\react-ui\tests\terminal-session-token.test.ts#L11)
  - [react-ui/tests/portal-session-request.test.ts](E:\Urili-Ruoyi\react-ui\tests\portal-session-request.test.ts#L224)
  - [react-ui/tests/portal-unauthorized-redirect.test.ts](E:\Urili-Ruoyi\react-ui\tests\portal-unauthorized-redirect.test.ts#L72)
- remote menu / authority / static admin route：
  - [react-ui/tests/getrouters-authority-contract.test.ts](E:\Urili-Ruoyi\react-ui\tests\getrouters-authority-contract.test.ts#L14)
  - [react-ui/tests/remote-menu-route-guard.test.ts](E:\Urili-Ruoyi\react-ui\tests\remote-menu-route-guard.test.ts#L68)
  - [react-ui/tests/static-route-authority-contract.test.ts](E:\Urili-Ruoyi\react-ui\tests\static-route-authority-contract.test.ts#L12)
- JS/TS 运行时镜像与入口 sidecar：
  - [react-ui/tests/admin-auth-sidecar-contract.test.ts](E:\Urili-Ruoyi\react-ui\tests\admin-auth-sidecar-contract.test.ts#L178)
- 后端 gate 自身不回退成静态 `-pl`：
  - [react-ui/tests/verify-three-terminal-backend-gate.test.ts](E:\Urili-Ruoyi\react-ui\tests\verify-three-terminal-backend-gate.test.ts#L32)

## 审计说明

- 本次为只读审计，未修改业务代码、SQL、配置。
- 未运行浏览器/截图/DOM/UI 检查，符合“快速推进模式只看 P0/P1”的约束。
- 本文件原始审计阶段未执行 `codegraph sync .`：当时无代码更新，仅新增审计 Markdown 记录。

## 修复后验证补记

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过，`three-terminal manifest check passed.`
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests\verify-three-terminal-backend-gate.test.ts tests\admin-auth-sidecar-contract.test.ts tests\system-user-service-contract.test.ts --runInBand`
  - 通过，3 suites / 45 tests。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - 通过，Jest 23 suites / 180 tests；seller tests 100；buyer tests 101；后端 reactor test-compile 和三端合同均通过。

## 子 Agent 使用记录

- 本文件原始审计阶段未写明子 Agent 模型和关闭状态。
- 2026-06-09 后续 P0/P1 目标追踪复核使用 6 个 `gpt-5.4` 子 Agent，verify gate 切片指出 3 个记录层 P1：目标追踪顶部模型口径过宽、部分审计 Markdown 缺少子 Agent 使用记录、冷启动记录缺少 CodeGraph 结果。
- 6 个子 Agent 均已完成并关闭。
- 本文件采纳的记录层 P1 是补齐子 Agent 使用记录；verify gate 代码层未发现新的 P0/P1。
