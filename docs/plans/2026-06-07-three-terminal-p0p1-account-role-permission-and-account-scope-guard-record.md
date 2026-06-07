# 三端 P0/P1：账号角色权限与账号作用域 Guard 记录

时间：2026-06-07 23:54 +08:00

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 范围

本轮继续按快速推进模式处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行情况

- 用户确认模型顺序：优先 GPT-5.3 Codex，对应工具模型 `gpt-5.3-codex-spark`；如果不可用再使用 `gpt-5.4`。
- 本目标同一时间段内 `gpt-5.3-codex-spark` 已返回用量限制，提示需等到 `2026-06-08 01:14` 后再试；本轮按 fallback 规则使用 6 个 `gpt-5.4` 只读子 Agent。
- 6 个 fallback 子 Agent 已全部关闭，主 Agent 只采纳确定 P0/P1，P2/设计债记录为不阻塞。

## 采纳的 P1

- 账号角色分配按钮权限不完整：前端只要求 `*:admin:account:role:query` 和 `*:admin:account:role:edit`，但后端账号角色回显接口还要求 `*:admin:role:query`。已改为三者同时满足才展示“分配角色”。
- 账号作用域静态合同不够硬：已有合同禁止裸 `select*AccountById(accountId)` mapper，但测试桩仍保留裸 mapper 名称，且未固定 Service 层不得新增单参数账号查询入口。已移除测试桩裸账号分支，并强化 `TerminalAccountIsolationTest`。
- 三端隔离参考文档中旧的 account-only 审计反查例外已过期。已改为管理端登录日志、操作日志和免密票据审计列表按账号筛选时也必须显式提供对应主体 ID。

## 已完成修改

- `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
- `react-ui/src/components/PartnerManagement/PartnerAccountModal.js`
- `react-ui/scripts/check-partner-management-template.mjs`
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminAccountPermissionUiContractTest.java`
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalAccountIsolationTest.java`
- `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/*Test.java` 中相关账号/权限测试桩
- `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/*Test.java` 中相关账号/权限测试桩
- `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- `AGENTS.md`
- `docs/architecture/reuse-ledger.md`

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=AdminAccountPermissionUiContractTest,TerminalAccountIsolationTest" test`：通过，5 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- 此前在代码变更后已运行 `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 7 个 Jest suite / 34 个测试通过；后端 `ruoyi-system` 154、`ruoyi-framework` 15、`integration` 5、`product` 8、`seller` 96、`buyer` 97 个测试通过；后端 reactor `test-compile` 通过。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 库存事实源、商品库存聚合、`ProductDistributionMapper` 跨模块访问仍按 P2/设计债处理，不阻塞当前 P0/P1 收口。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。
