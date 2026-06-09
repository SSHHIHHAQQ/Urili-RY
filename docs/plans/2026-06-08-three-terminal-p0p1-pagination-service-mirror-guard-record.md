# 2026-06-08 三端隔离 P0/P1：分页契约与 Service JS 镜像 Guard 记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent

- 最新用户指令要求子 Agent 使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 本轮先按前序口径尝试 6 个 `gpt-5.3-codex-spark` 子 Agent，平台返回额度限制，失败 Agent 已关闭。
- 随后按最新用户指令启动 6 个 `gpt-5.4` 只读子 Agent，全部已关闭。
- 切片覆盖 SQL/seed guard、seller/buyer 后端隔离、direct-login/token/session/401、React admin/portal contract、product/integration/warehouse/finance 边界、验证闸门。

## 采纳的 P1

- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx` 当前已经把 ProTable 的 `current/pageSize` 转成若依接口需要的 `pageNum/pageSize`，但 `guard:partner-management` 没把这条契约钉住，后续容易静默退回直接传 `params`。
- `react-ui/src/pages/UpstreamSystem/components/SkuSyncPanel.tsx`、`SkuDimensionPanel.tsx`、`SkuInventoryPanel.tsx`、`react-ui/src/pages/Product/SourceProductLibrary/index.tsx`、`react-ui/src/pages/Product/Review/index.tsx` 当前分页映射正确，但 manifest-owned test 覆盖不足。
- `react-ui/src/services/integration/upstreamSystem.js` 是完整 JS 副本，不是纯 re-export，存在和 `upstreamSystem.ts` 分叉后运行态接口漂移的风险。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/IntegrationAdminRouteContractTest.java` 仍要求 `upstreamSystem.js` 自身包含 admin baseUrl，和当前统一 JS mirror 纯 re-export 的主模式冲突。

## 已完成

- `react-ui/src/services/integration/upstreamSystem.js` 改为 `export * from './upstreamSystem.ts';`，运行镜像不再保留重复实现。
- `react-ui/scripts/check-partner-management-template.mjs` 增加 Partner 主列表分页契约断言：
  - `buildListParams(...)` 必须保留 `pageNum: current` 和 `pageSize`。
  - 主列表 request 必须调用 `config.services.list(buildListParams(rest, current, pageSize))`。
  - 禁止 `config.services.list(params)` 这种直接透传 ProTable 参数。
- `react-ui/tests/upstream-system-permission-guard.test.ts` 增加：
  - `upstreamSystem.js` 必须是纯 named re-export。
  - SKU 同步、SKU 尺寸、库存同步面板必须把 `params.current/params.pageSize` 映射为 `pageNum/pageSize`。
- `react-ui/tests/product-distribution-permission-guard.test.ts` 增加商品审核列表分页契约断言。
- `react-ui/tests/source-product-library-contract.test.ts` 增加来源商品库分页契约断言。
- `IntegrationAdminRouteContractTest` 改为检查 `upstreamSystem.ts` 的 admin baseUrl，同时要求 `upstreamSystem.js` 是纯 re-export。
- `AGENTS.md` 更新子 Agent 模型规则：默认使用 `gpt-5.4`，除非用户在当前任务重新明确要求，否则不再把 GPT-5.3 Codex 作为首选。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; node scripts/check-partner-management-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/upstream-system-permission-guard.test.ts tests/product-distribution-permission-guard.test.ts tests/source-product-library-contract.test.ts --runInBand`：通过，3 个 suite / 20 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=IntegrationAdminRouteContractTest" test`：通过，1 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 前端：4 个 guard 通过，`tsc` 通过，20 个 Jest suite / 145 个测试通过。
  - 后端：reactor `test-compile` 通过，三端合同测试通过，后端合计 196 + 16 + 9 + 1 + 6 + 35 + 96 + 97 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，退出码 0；仅输出当前工作区 LF/CRLF 换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 同步 6 个变更文件。

## 边界

- 未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器、截图、DOM 或 UI 细调验收。

## P2 残留

- `SqlExecutionGuardContractTest` 的 auto-discovery 仍可进一步收紧确认 token、`45000` 和 `assert_*_confirmed()` 必须属于同一确认链。
- `DYNAMIC_HIGH_IMPACT_SQL_HINT` 仍偏向识别 `@ddl`，后续可泛化到任意动态 DDL 变量名。
- 新建主体时 owner 默认密码仍为固定 `U12346`；当前不属于 resetPwd 静默回退，但后续应考虑首登激活或显式临时密码。
- portal 页面如果未来直接调用 `/api/*/admin/**`，管理端 401 分支可补充 portal 路由场景测试。
- portal 商品列表存在重复 SKU 查询 / N+1 优化空间。
- `AdminSourceProductController` 与 `AdminSourceWarehouseStockController` 的自定义分页未处理 `orderBy`，排序参数会被静默忽略。
- 仓库同时存在 `jest.config.ts` 和 `jest.config.js`，当前 verifier 固定使用 `jest.config.ts`，但手工 raw Jest 仍可能有配置漂移风险。
