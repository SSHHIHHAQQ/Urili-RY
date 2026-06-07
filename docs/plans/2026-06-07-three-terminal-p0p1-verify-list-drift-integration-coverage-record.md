# 2026-06-07 三端 P0/P1 验证入口清单漂移与 Integration 覆盖记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮聚焦 `verify-three-terminal.mjs` 自身清单漂移和 `integration` 模块漏覆盖；不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 新增问题

- P0：`verify-three-terminal.mjs` 已会扫描 `react-ui` 下所有 `*.test.*` / `*.spec.*`，但 `frontendTestPaths` 未登记新增的 `tests/portal-unauthorized-redirect.test.ts`，公开验证入口会在执行 Jest 前 fail-closed 自阻断。
- P1：来源商品、来源仓库库存和上游同步相关能力已落在 `integration` 模块，但 `verify-three-terminal.mjs` 的 Maven reactor 和 report 清理/检查模块未包含 `integration`，三端快速验证入口没有兜住该编译边界。

## 已修复问题

- `react-ui/scripts/verify-three-terminal.mjs` 的 `frontendTestPaths` 补入 `tests/portal-unauthorized-redirect.test.ts`。
- `backendReportModules` 后续已改为从根 `pom.xml` 动态读取 reactor 模块；`integration` 当前进入编译闭环。
- `assertBackendTestReportsExist()` 当前按显式 `backendTestClasses` 校验 surefire 报告，不按无测试模块强制产出报告；`integration` 尚未进入 report 级必跑清单。
- 后端 Maven `-pl` 从 `ruoyi-system,ruoyi-framework,product,seller,buyer` 扩展为 `ruoyi-system,ruoyi-framework,integration,product,seller,buyer`。
- 后续 P1 已收口：`20260607_source_warehouse_stock_read_model.sql` 已补显式专项 SQL contract，不再仅依赖日期高影响 SQL 自动发现。

## 残留问题

- 历史残留已收口：`integration` fresh bootstrap schema 已由 `docs/plans/2026-06-07-three-terminal-p0p1-integration-bootstrap-chain-record.md` 固定为 bootstrap 后必跑 SQL 清单，不再停留在“策略未定”状态。
- P1：旧 `portal_direct_login:{token_hash}` Redis key 仍有兼容读取路径；下一切片可改为仅读新 key、旧 key 只允许清理。
- 2026-06-07 追记：`integration` 当前只有编译闭环覆盖；如果后续需要 report 级 guard，必须在 `integration` 模块新增明确契约测试后再加入 `backendTestClasses`。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/terminal-session-token.test.ts tests/portal-session-request.test.ts tests/portal-direct-login-message.test.ts tests/remote-menu-route-guard.test.ts tests/portal-unauthorized-redirect.test.ts --runInBand`：通过，5 个 suite、21 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration,ruoyi-admin -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 5 个 suite / 21 个测试通过，后端 ruoyi-system 127、ruoyi-framework 15、product 1、seller 87、buyer 88 个测试通过，`integration` 模块进入 Maven reactor 并编译通过。
- 后续补充验证：`cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 30 个测试通过。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。
- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只修改验证脚本和 Markdown。
- 未新建 fresh bootstrap 空库验证：后续已固定为 bootstrap 后必跑 SQL 清单，并由 `SqlExecutionGuardContractTest#integrationFreshBootstrapMustDeclareMandatoryPostSeedSqlChain` 静态合同覆盖；本记录当时未做真实空库回放。

## 权限检查结果

- 本轮未改管理端、卖家端、买家端权限判断逻辑。
- `verify-three-terminal` 继续运行 portal token guard、partner management guard、seller/buyer portal product guard 和后端权限/SQL/Portal 合同。

## 字典/选项复用检查结果

- 本轮未新增字典类型、字典数据或前端选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，记录新增 Jest 必须同步进入 `frontendTestPaths`，以及 `integration` 至少进入三端快速验证 Maven reactor。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Synced 1 changed files`，`Modified: 1 - 23 nodes in 758ms`。

## 大文件合理性判断结果

- `verify-three-terminal.mjs` 已超过 250 行，但职责仍集中在三端快速验证入口；本轮只补清单和模块覆盖，不拆分脚本。
- 本轮新增 Markdown 记录为阶段留痕，不涉及大代码文件。

## 重复代码检查结果

- 未新增平行验证脚本；仍复用 `verify-three-terminal.mjs` 作为统一入口。
- 未复制前端或后端业务逻辑。

## 子 Agent 使用记录

- 本切片来自本轮 `gpt-5.4` 只读子 Agent 对 fresh bootstrap/integration 方向的复核结论。
- 子 Agent 同时指出 fresh bootstrap schema 仍是 P1，但最小优先切片应先修验证入口自身漂移和 `integration` 漏覆盖；本轮采纳该排序。

## 一句话总结

本轮修复了三端快速验证入口的前端测试清单漂移，并把 `integration` 编译纳入标准验证入口；完整 `npm run verify:three-terminal` 已通过。
