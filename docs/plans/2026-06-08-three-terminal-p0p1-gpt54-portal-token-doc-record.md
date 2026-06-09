# 2026-06-08 三端隔离 P0/P1 gpt-5.4 收敛记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调；P2 仅记录，不阻塞。

## 子 Agent 执行情况

- 本轮按用户最新要求直接使用 6 个 `gpt-5.4` 子 Agent，不再把旧模型作为首选。
- 6 个切片分别覆盖：seller/buyer 后端隔离、React 管理端模板、portal 登录与直登、SQL seed/guard、verify manifest/gate、Markdown 口径一致性。
- 6 个子 Agent 均已完成并关闭。

## 采纳并修复的 P1

1. `persistPortalLogin(...)` 在登录结果缺 token 或 terminal 不匹配时不应清理当前端既有 token。
   - 修改 `react-ui/src/pages/Portal/terminal.ts`：无效或跨端响应只 `return false`，不调用 `clearPortalLogin(...)`。
   - 修改 `react-ui/tests/terminal-session-token.test.ts`：跨端响应必须拒绝持久化，且不得清 seller/buyer 任何已有 token。
   - 修改 `react-ui/scripts/check-portal-token-isolation.mjs`：守卫固定失败分支不得清理任何 portal token，同时继续禁止清理响应声明的另一端 token。
   - 修改 `AGENTS.md` 与 `docs/architecture/reuse-ledger.md`：现行口径统一为失败登录、跨端响应或无效响应不清理任何已有端内 token。

2. `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 中旧模型优先和旧默认密码重置口径容易误导后续执行。
   - 将旧检查点里未显式过期的旧模型优先记录标为“历史记录（已过期口径）”，并说明现行规则为默认使用 `gpt-5.4`。
   - 将旧检查点里 `resetDefaultPwd` 仍存在、UI 只做默认密码重置等表述标为“历史记录（已过期口径）”，并说明当前实现已由人工临时密码 `resetPwd` 覆盖。
   - 将旧检查点里“跨端或无效响应只清当前页面端 token”的表述标为“历史记录（已过期口径）”，并说明当前实现不清理任何已有端内 token。

## 未采纳为 P0/P1 的记录项

- SQL dated 脚本部分没有进入手工 high-impact 白名单，但当前仍被 dated 自动发现、全量 incremental 自动发现和专项合同测试覆盖。
- Jest 直接执行时有双配置噪音，定向运行需显式 `--config jest.config.ts`。
- `seller_menu` / `buyer_menu` 是端级共享菜单模板，不应误判为缺少主体 scope。
- `verify-three-terminal` 当前只发现 `*Test.java`，暂不覆盖未来可能新增的 `*Tests.java` 命名；当前仓库未发现这类测试文件。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`：通过，`Portal token isolation guard passed.`
- `cd E:\Urili-Ruoyi\react-ui; cmd /d /s /c ".\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests/terminal-session-token.test.ts tests/portal-unauthorized-redirect.test.ts tests/portal-direct-login-message.test.ts tests/portal-session-request.test.ts --runInBand"`：通过，4 个 suite / 51 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- 子 Agent 补充验证：seller/buyer 后端隔离测试、SQL guard 合同、React 管理端模板、verify gate 相关 Jest 均通过。

## 未执行

- 未执行远端 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。
