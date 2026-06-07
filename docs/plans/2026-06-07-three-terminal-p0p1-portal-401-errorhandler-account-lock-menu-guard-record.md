# 2026-06-07 三端独立 P0/P1 401 ErrorHandler 与账号锁定菜单 Guard 记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，在快速推进模式下只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本切片不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 已处理问题

### Portal 401 ErrorHandler 不得吞错

`react-ui/src/requestErrorConfig.ts` 和当前并存的 `.js` 镜像中，`errorHandler` 命中 BizError 401 或 HTTP 401 后原先只执行跳转并 `return`。这会让调用链可能把 401 当成已处理结果继续走成功分支。

本轮改为：

- BizError 401：调用 `handleUnauthorized(...)` 后继续 `throw error`。
- HTTP 401：调用 `handleUnauthorized(...)` 后继续 `throw error`。
- `portal-unauthorized-redirect.test.ts` 新增断言：portal/admin 401 跳转后必须抛回原错误。

### 账号锁定按钮菜单 Slot Guard 补 parent/type

`20260605_seller_account_lock_control.sql` 和 `20260605_buyer_account_lock_control.sql` 的 `assert_sys_menu_slot(...)` 原先只校验 `path/component/route_name/perms`，没有锁定 `parent_id/menu_type`。历史库中如果同一 `menu_id` 被挂到错误父级或类型错误，脚本可能静默通过并在 upsert 中改写。

本轮改为：

- 卖家账号锁定按钮 `2322` 必须保持 `parent_id=2011`、`menu_type=F`。
- 买家账号锁定按钮 `2323` 必须保持 `parent_id=2012`、`menu_type=F`。
- `SqlExecutionGuardContractTest.accountLockMenuSeedsMustGuardSysMenuSlotsBeforeUpsert()` 同步锁住该合同。

## 子 Agent 执行情况

- 按最新口径优先使用 `gpt-5.3-codex-spark`，本轮启动 6 个只读子 Agent。
- SQL 子 Agent 完成并指出账号锁定菜单 seed 缺 `parent_id/menu_type` guard；已采纳并修复。
- React 子 Agent 初次因上下文窗口失败已关闭；用短提示重启后指出 `requestErrorConfig` 401 errorHandler 吞错风险；已采纳并修复。
- 验证入口子 Agent 完成，结论是 `verify-three-terminal.mjs` 当前未见 P0/P1 漏测或空跑问题；未改文件。
- seller、buyer、direct-login 三个只读子 Agent 在等待窗口内未返回，为避免悬挂已关闭；未采纳其未完成输出。
- 本轮没有回退到 `gpt-5.4`。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/portal-unauthorized-redirect.test.ts --runInBand`：通过，7 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 34 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`：通过，前端 6 个 Jest suite / 30 个测试通过，后端三端合同链路通过。
- `git diff --check`：通过，仅有 LF/CRLF 归一化提示。
- `codegraph sync .`：通过，输出 `Synced 4 changed files`、`Modified: 4 - 109 nodes`。

## 未执行事项

- 未执行数据库迁移或远程 DDL/DML。
- 未做浏览器运行态验收、截图或 DOM 检测。
- SQL 子 Agent 提到的“多个端内权限 seed 对 `seller_menu/buyer_menu` 写入缺 fail-closed slot/signature guard”范围较大，未混入本切片，作为后续独立 P1 切片处理。
