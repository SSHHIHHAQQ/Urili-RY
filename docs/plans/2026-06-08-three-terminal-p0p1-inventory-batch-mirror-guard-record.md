# 2026-06-08 三端 P0/P1 库存批量调整与 Product/Upstream JS 镜像 Guard 记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 本轮按用户最新要求使用 6 个 `gpt-5.4` 子 Agent，不再使用 GPT-5.3 Codex。
- 覆盖范围：seller/buyer 后端隔离、portal auth/direct-login/session/log、SQL guard、React 管理端路由权限、共享业务域、verifier/manifest/docs/JS 镜像。
- 6 个子 Agent 均已完成并关闭。

## 已处理 P1

- 库存总览批量调整：补前端 service 合同，固定 `previewInventoryOverviewBatchAdjust`、`confirmInventoryOverviewBatchAdjust` 及 `/adjust/batch-preview`、`/adjust/batch-confirm` 管理端接口，避免后续页面入口调用缺 service/字段。
- Product/UpstreamSystem JS 镜像：新增 `guard:product-upstream-mirrors`，要求 Product、UpstreamSystem、product service、integration service 下存在 TS/TSX 同名源文件的 JS 镜像必须是纯 re-export。
- Product/UpstreamSystem JS 镜像：将相关 JS 双文件收敛为纯 re-export，避免运行入口绕过 TS/TSX 里的权限、路由和 service 合同。
- UpstreamSystem 权限测试：业务权限断言回到 TSX 源文件，JS 入口交给纯镜像 guard 统一保护。
- 库存总览刷新合同：`InventoryOverviewRefreshContractTest` 只检查 `refreshProductInventoryOverview` 方法体内“重建库存行早于 SKU 读模型刷新”的顺序，避免批量调整方法中的同名刷新调用造成全文件字符串误判。
- 库存总览 service：批量调整后的刷新变量改为 `affectedSkuId` / `affectedSpuId`，语义更明确，也避免合同误读为产品重建刷新入口。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:product-upstream-mirrors`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests/inventory-overview-contract.test.ts --runInBand`：通过，1 个 suite / 3 个测试。
- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests/product-distribution-permission-guard.test.ts tests/product-center-contract.test.ts tests/source-product-library-contract.test.ts tests/upstream-system-permission-guard.test.ts tests/inventory-overview-contract.test.ts --runInBand`：通过，5 个 suite / 27 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory -am "-Dtest=InventoryOverviewRefreshContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 前端 guard 通过：portal token、partner management、seller portal product、buyer portal product、product upstream mirrors。
  - React typecheck 通过。
  - Jest 21 个 suite / 150 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过。

## 未执行

- 未执行远端 MySQL DDL/DML，未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。

## P2 留存

- SQL 自动 guard 可继续加强深度解析和递归发现，但本轮没有确认会导致 P0/P1 失效。
- direct-login 目标窗口超时提示和 401 helper 复用仍可优化，但当前三端 guard 和测试通过。
- `npx jest` 未显式 config 时会同时看到 `jest.config.js` 和 `jest.config.ts`，官方命令应继续使用 `--config jest.config.ts`。
- 库存批量调整的 UI 细节不在本轮范围；当前只固定接口、service 和权限合同。
