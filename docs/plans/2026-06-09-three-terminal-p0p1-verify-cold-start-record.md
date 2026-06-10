# 三端验证门禁冷启动稳定性 P1 修复记录

日期：2026-06-09

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。

当前快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 问题

`verify:three-terminal` 在冷启动场景下存在时序型不稳定：

- 删除 `react-ui/src/.umi-test` 后直接运行完整 `npm run verify:three-terminal`。
- 前端 Jest 可能在 Umi test 临时文件未完整生成时开始加载测试。
- 已观察到 `tests/terminal-session-token.test.ts` 冷态失败，错误为缺少 `react-ui/src/.umi-test/exports.ts`。
- 单独补跑对应 Jest 或第二次完整 gate 会通过，说明不是业务断言失败，而是测试临时文件准备不稳定。

该问题会导致三端 gate 不能作为稳定绿门禁，按快速推进口径采纳为 P1。

## 修复

### 新增 Umi 测试态预热脚本

新增：

- `react-ui/scripts/prepare-umi-test.mjs`

行为：

- 在导入 Umi 测试工具前显式设置 `NODE_ENV=test`。
- 调用 `@umijs/max/test.js` 的 `configUmiAlias(...)`，触发 Umi test 临时文件生成。
- 断言 `react-ui/src/.umi-test/exports.ts` 已生成。

### verify gate 接入预热步骤

更新：

- `react-ui/scripts/verify-three-terminal.mjs`

行为：

- 在 frontend guard 后、typecheck 和 Jest 前新增 `umi test setup` 步骤。
- 该步骤使用当前 Node 进程执行 `scripts/prepare-umi-test.mjs`。
- `portal session unit tests` 进入 Jest 前再次断言 `src/.umi-test/exports.ts` 存在。

### 合同测试固定

更新：

- `react-ui/tests/verify-three-terminal-backend-gate.test.ts`

新增断言：

- `verify-three-terminal.mjs` 必须在 Jest 前执行 `umi test setup`。
- `prepare-umi-test.mjs` 必须设置 `NODE_ENV=test` 后动态导入 `@umijs/max/test.js`。
- `prepare-umi-test.mjs` 必须断言 `src/.umi-test/exports.ts`。

## 子 Agent 结论处理

本轮使用 6 个 `gpt-5.4` 子 Agent，均已关闭。

- 后端 seller/buyer 账号权限隔离切片：未发现新增 P0/P1。
- 管理端控制权接口与权限切片：未发现新增 P0/P1；报告已落到 `docs/reviews/2026-06-09-admin-control-seller-buyer-p0p1-audit.md`。
- SQL/schema/seed/guard 切片：未发现新增 P0/P1。
- React 管理端切片：未发现新增 P0/P1。
- React portal/token/request/401/direct-login 切片：未发现新增 P0/P1；报告已落到 `docs/audits/2026-06-09-react-ui-portal-token-request-401-direct-login-readonly-audit.md`。
- verify gate 切片：发现并采纳 1 个 P1，即本记录修复的冷启动不稳定。

## 验证

### 冷启动完整 gate

验证前安全删除生成目录：

```powershell
cd E:\Urili-Ruoyi\react-ui
Remove-Item -LiteralPath .\src\.umi-test -Recurse -Force
```

实际执行时先校验删除目标在 `react-ui` 内，再删除。

随后执行：

```powershell
npm run verify:three-terminal
```

结果：

注意：以下测试数量是 2026-06-09 冷启动当时快照，后续检查点已扩大到前端 24 suites / 193 tests、product 62 tests。当前口径以最新 `three-terminal-six-hour-review-log.md` 和本文件之后的检查点为准。

- 通过。
- 5 个 frontend guard 均通过。
- 新增 `umi test setup` 步骤通过，并重新生成 `src/.umi-test/exports.ts`。
- React typecheck 通过。
- Frontend Jest：23 suites / 180 tests 通过。
- Backend reactor test-compile 通过。
- Backend three-terminal contracts 通过。
- seller tests：100 通过。
- buyer tests：101 通过。

### 其他验证

```powershell
cd E:\Urili-Ruoyi\react-ui
node scripts\verify-three-terminal.mjs --check-manifest
npx jest --config jest.config.ts tests\verify-three-terminal-backend-gate.test.ts --runInBand
```

结果：

- manifest check 通过，`three-terminal manifest check passed.`。
- `verify-three-terminal-backend-gate.test.ts` 通过，1 suite / 12 tests。
- 单跑该 Jest suite 结束后出现 Jest open handle 提示，但退出码为 0；完整 `verify:three-terminal` 未被该提示阻塞。

```powershell
cd E:\Urili-Ruoyi
git diff --check
```

结果：

- 通过。
- 仅提示当前工作区若干文件 LF/CRLF 转换 warning。

### CodeGraph

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：

- 通过。
- 本轮后续复核曾执行为 `Already up to date`；收口同步输出过 `Synced 4 changed files`，最终再次复核为 `Already up to date`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮改动集中在 React verify gate、合同测试和 Markdown 记录。
