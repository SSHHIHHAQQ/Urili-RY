# 2026-06-08 三端 P0/P1 快速推进：库存权限与上游仓库角色修复记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。执行边界仍为快速推进模式：只处理 P0/P1（编译、guard、接口、权限、串端、service/字段缺失），不做浏览器运行态验收、截图、DOM 检测或 UI 细调；P2 只记录，不阻塞。

## 子 Agent 执行情况

- 按用户最新要求，本轮使用并关闭 6 个 `gpt-5.4` 子 Agent，不再把 GPT-5.3 Codex 作为首选。
- 6 个切片分别覆盖商品中心/审核、库存概览、integration/warehouse 事实链路、React 页面与 service、SQL seed guard、验证清单与文档。
- 已关闭的子 Agent：Dalton、Helmholtz、Carson、Pauli、Sartre、Bacon。
- 采纳的 P1：库存调整权限必须同时满足查询权限，库存仓库明细无查询权限时必须 fail-closed；库存调整原因必须前后端必填；上游仓库配对角色不能在自营应收场景硬编码为 `FULFILLMENT`；上游系统连接列表首屏无查询权限时必须短路。
- 未采纳为阻塞项：商品中心/审核、SQL seed guard、验证清单只发现 P2 或历史文档漂移，本轮不阻塞。

## 已完成修复

- `react-ui/src/pages/Inventory/Overview/index.tsx` 将库存调整权限收敛为 `inventory:overview:query + inventory:overview:adjust`，并把查询权限下传给仓库明细表。
- `react-ui/src/pages/Inventory/Overview/components/WarehouseViewTable.tsx` 增加 `canQuery` fail-closed，缺少查询权限时不发明细请求。
- `react-ui/src/pages/Inventory/Overview/components/QuantityCell.tsx` 与 `InventoryAdjustButton.tsx` 将调整原因改为必填，预览和确认使用同一份 trim 后原因。
- `react-ui/src/services/inventory/overview.ts` 将库存调整预览/确认请求类型里的 `reason` 改为必填。
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java` 在单条和批量库存调整入口校验 `reason` 非空并 trim。
- `RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml` 在来源官方仓投影和库存快照仓库配对刷新中按连接结算类型选择 `QUOTE` / `FULFILLMENT`。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/sync/UpstreamInventorySyncComponent.java` 构建仓库配对映射时按连接结算类型推导目标配对角色。
- `react-ui/src/pages/UpstreamSystem/index.tsx` 增加 `integration:upstream:query` 首屏短路，缺权限时清空连接状态并避免发请求。
- 更新前后端合同测试，锁定库存权限、库存调整原因、上游仓库角色和上游系统首屏查询 guard。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/inventory-overview-contract.test.ts tests/upstream-system-permission-guard.test.ts --runInBand`：通过，2 个 suite / 10 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory -Dtest=InventoryOverviewRefreshContractTest test`：通过，1 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration -am test-compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am -Dtest=ProductDistributionMapperContractTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，product 合同测试 9 个。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=InventoryAdminRouteContractTest test`：通过，1 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；包含 React typecheck、前端 21 个 suite / 157 个测试、后端 reactor test-compile，以及 ruoyi-system / framework / finance / inventory / integration / product / seller / buyer 合同测试。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，退出码 0；仅输出当前工作区 LF/CRLF 换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，文档落盘后最终复跑返回 `Already up to date`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 当前残留项

- P2：库存调整弹窗的具体布局和文案细节未做浏览器级检查，按用户要求不阻塞。
- P2：商品中心/审核测试命名和历史文档统计存在漂移，未影响本轮 P0/P1。
- P2：`WarehouseMapper.xml` 仍存在 `p.pairing_role = 'FULFILLMENT'`，但该查询另有 `QUOTE` 分支 join，当前未作为缺陷处理。
