# 三端 P0/P1 管理端 Redirect、Finance 与 Warehouse Gate 收口记录

日期：2026-06-08

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

本轮继续按快速推进模式处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 历史记录（已过期口径）：按用户要求优先尝试 GPT-5.3 Codex（工具模型 `gpt-5.3-codex-spark`）；本轮 4 个尝试均因平台 usage limit 失败，提示需等到 2026-06-14 15:12 后再试，失败 Agent 已关闭。
- 按规则回退使用 6 个 `gpt-5.4` 只读子 Agent，覆盖 seller/buyer 后端隔离、SQL seed/guard、React guard/session/direct-login、product/inventory/integration/warehouse、verify manifest/gate、admin 控制权六个切片。
- 6 个回退子 Agent 均已关闭；主线程复核后采纳 3 个确认 P1，未采纳只读扫描中无证据的扩大项。

## 确认的 P1

1. 管理端登录成功后的 `redirect` 直接使用 query 参数，缺少站内相对路径、admin 路径和 portal 路径约束，存在登录后 open redirect / 串端误跳风险。
2. finance 模块已有后端单测和前端运行面，但未被三端 verifier critical 规则完整纳入；finance JS mirror 也未固定为纯 TS/TSX re-export。
3. warehouse 管理端已有后端权限和前端按钮 gate，但缺少同等级合同测试；warehouse JS mirror 仍是编译副本，且页面没有在无 list 权限时短路列表/选项请求。

## 已完成

- 新增 `react-ui/src/utils/adminRedirect.ts` / `.js`，管理端登录成功后只允许站内 admin 相对路径；拒绝外部 URL、`//`、反斜杠、`/user/login`、seller/buyer portal 路径。
- `react-ui/src/pages/User/Login/index.tsx` 改为使用 `resolveAdminRedirectFromSearch(window.location.search)`。
- finance JS mirror 改为纯 re-export：
  - `react-ui/src/pages/Finance/Currency/index.js`
  - `react-ui/src/pages/Finance/Currency/constants.js`
  - `react-ui/src/pages/Finance/Currency/components/SyncSettingsPanel.js`
  - `react-ui/src/services/finance/currency.js`
- `FinanceAdminRouteContractTest` 增加 finance JS mirror 合同；新增 `finance-currency-contract.test.ts`。
- `three-terminal.manifest.json` 增加 finance 模块单测和 finance 前端合同；`verify-three-terminal.mjs` 把 finance 后端路径、Finance/Currency 类名和 finance/currency 前端测试纳为 critical。
- warehouse 页面补 `warehouse:*:list` 权限短路：无 list 权限时不加载字典/选项，也不请求主列表。
- warehouse JS mirror 改为纯 re-export：
  - `react-ui/src/services/warehouse/warehouse.js`
  - `react-ui/src/pages/Warehouse/WarehouseManagementPage.js`
  - `react-ui/src/pages/Warehouse/constants.js`
  - `react-ui/src/pages/Warehouse/Official/index.js`
  - `react-ui/src/pages/Warehouse/ThirdParty/index.js`
  - `react-ui/src/pages/Warehouse/components/OfficialSyncModal.js`
  - `react-ui/src/pages/Warehouse/components/WarehouseFields.js`
  - `react-ui/src/pages/Warehouse/components/WarehouseFormModal.js`
  - `react-ui/src/pages/Warehouse/components/WarehousePairingModal.js`
- `WarehouseAdminRouteContractTest` 增加后端权限、前端 gate 和 JS mirror 合同；新增 `warehouse-permission-guard.test.ts`。
- `three-terminal.manifest.json` 增加 warehouse 前端合同；`verify-three-terminal.mjs` 把 warehouse 前端测试纳为 critical。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\admin-auth-sidecar-contract.test.ts tests\finance-currency-contract.test.ts tests\warehouse-permission-guard.test.ts tests\verify-three-terminal-backend-gate.test.ts --runInBand`：通过，4 个 suite / 22 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,finance,warehouse -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=FinanceAdminRouteContractTest,WarehouseAdminRouteContractTest,FinanceCurrencyServiceImplTest,CurrencyRateSyncSchedulePolicyTest" test`：通过；`ruoyi-system` 2 个测试，`finance` 9 个测试，`warehouse` 无单测源码。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal -- --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；React typecheck、4 个前端 guard、20 个前端 Jest suite / 127 个测试、后端 reactor test-compile、后端三端合同测试均通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅输出既有 LF/CRLF 换行风格 warning。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 24 个变更文件，新增 4 个、修改 20 个，195 个节点。

## 未执行

- 未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。

## 残留

- 本轮没有确认新的 P0/P1 残留。
- P2：历史页面中仍有若依 React 旧权限命名、历史 JS mirror 和 UI 细节可继续清理；按当前快速推进模式不阻塞。
