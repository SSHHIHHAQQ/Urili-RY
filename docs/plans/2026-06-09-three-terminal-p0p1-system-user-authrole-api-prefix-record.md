# 2026-06-09 三端 P0/P1 快速推进：system user 授权角色接口前缀修复记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行情况

- 按用户最新要求，本轮直接使用 6 个 `gpt-5.4` 子 Agent，不再把 GPT-5.3 Codex 作为首选。
- 6 个子 Agent 均已完成并关闭。
- 切片覆盖：
  - seller/buyer 后端账号权限隔离
  - portal auth / direct-login / token-session / Redis key / 401 / 审计 DTO
  - SQL guard / seed / menu owner / terminal menu id range
  - React route / access / proxy / request / session / service / JS mirror
  - product / inventory / integration / warehouse 共享业务域
  - verify-three-terminal / manifest / Maven reactor / JS mirror gate

## 采纳的 P1

- `react-ui/src/services/system/user.ts` 和 `react-ui/src/services/system/user.js` 中 `getAuthRole` / `updateAuthRole` 使用 `/system/user/authRole...`，缺少 `/api` 前缀。
- 当前 `react-ui/config/proxy.ts` 只代理 `/api/`，因此该接口在 8001 开发代理下会打到前端源站，绕过后端代理和统一 `/api/**` 请求链路。

## 已完成

- `getAuthRole` 改为请求 `/api/system/user/authRole/{userId}`。
- `updateAuthRole` 改为请求 `/api/system/user/authRole`。
- 同步修复 TS 源文件和 JS 镜像文件。
- 新增 `react-ui/tests/system-user-service-contract.test.ts`，固定授权角色接口必须走 `/api/system/user/authRole`，并拒绝裸 `/system/user/authRole`。
- `react-ui/tests/three-terminal.manifest.json` 纳入新增前端合同测试，确保完整三端验证会覆盖该问题。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests\system-user-service-contract.test.ts --runInBand`：通过，1 个 suite / 1 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`：通过。
  - portal token guard、partner management guard、seller/buyer portal product guard、product upstream mirrors guard 均通过。
  - React typecheck 通过。
  - 前端 Jest 22 个 suites / 161 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同通过，覆盖 200 个 ruoyi-system 测试、16 个 ruoyi-framework 测试、finance / inventory / integration / product / seller / buyer 关键测试。

## 未执行事项

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## P2 留存

- seller/buyer 管理端日志查询异常文案中仍有英文提示，可后续统一为中文。
- `20260604_portal_direct_login_ticket.sql` 的多步迁移非原子，后续可补更明确的 post-step 完整状态断言或拆分记录。
- `seller_buyer_management_seed.sql` 对已存在 portal web url 的坏值校验仍可增强。
- `app.tsx` 与 `requestErrorConfig.ts` 的 401 分流逻辑仍有重复，当前行为一致，后续可抽共享 helper。
- 部分 JS mirror guard 仍由 Jest 合同兜底，后续可把更多 mirror 检查前移为 guard。
- 前端金额、库存数量 contract 仍使用 `number`，后续可按金额字符串和 Long 大数精度边界统一收口。
- product 通过服务层消费 `Warehouse` 领域对象，当前没有越层打表；后续可收敛为更窄的 lookup/fact DTO。
