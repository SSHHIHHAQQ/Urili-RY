# 2026-06-08 三端 P0/P1 快速推进：来源库存预捕获刷新与仓库事实源校验记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

> 当前口径追补（2026-06-09）：本文件中的 GPT-5.3 Codex 子 Agent 尝试仅代表当时历史执行事实，不再作为后续规则。当前 `AGENTS.md` 已收紧为子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。

本轮继续执行快速推进模式：只修 P0/P1（编译、guard、接口、权限、串端、service/字段缺失），P2 只记录不阻塞；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用记录

- 先尝试 6 个 GPT-5.3 Codex 子 Agent（`gpt-5.3-codex-spark`），平台返回额度限制，提示需等到 2026-06-14 15:12 后再试；失败 Agent 已关闭。
- 回退使用 6 个 `gpt-5.4` 只读子 Agent，覆盖后端隔离、免密/401、SQL guard、React guard、product/inventory/integration、文档/manifest/pom 六个切片。
- 6 个 `gpt-5.4` 子 Agent 均已关闭；主 Agent 采纳 3 个 P1，并把 P2 仅记录不阻塞。

## 已修复 P1

1. `AGENTS.md` 代码审查交付清单落后于强规则。
   - 已补充数据源确认、远端 DB/Redis 影响、表设计/高影响 SQL 确认、三端隔离判断、子 Agent 实际模型和回退记录等必填项。

2. connection 级来源库存重建后，如果整组来源库存消失，库存总览读模型可能残留旧快照。
   - `IInventoryOverviewService` 新增 `selectSourceInventoryOverviewSpuIdsByConnection(...)` 和带 `preRebuildSpuIds` 的刷新入口。
   - `InventoryOverviewServiceImpl` 使用 `LinkedHashSet` 合并重建前和重建后的受影响 SPU，再逐个刷新商品库存总览。
   - `UpstreamSystemServiceImpl`、`UpstreamSyncServiceImpl`、`SourceReadModelRefreshServiceImpl` 在删重建 `source_warehouse_stock_detail` 前先捕获受影响 SPU，重建后带入刷新，避免旧读模型残留。
   - `InventoryOverviewRefreshContractTest` 增加合同，固定预捕获集合和并集刷新链路。

3. 上游仓配对接口信任请求里的 `systemWarehouseCode/systemWarehouseName`。
   - integration 模块新增 `IWarehouseFactLookupService` 端口和 `WarehouseFact` 简化事实 DTO，避免 integration 直接依赖 warehouse 模块造成 Maven 循环。
   - warehouse 模块新增 `WarehouseFactLookupServiceImpl`，按 `warehouse_code` 回查事实源，并只接受正常官方仓。
   - `UpstreamSystemServiceImpl#insertWarehousePairing(...)` 改为用事实源返回的仓库编码和名称落配对，不再信任请求里的系统仓名称。
   - `IntegrationAdminPermissionContractTest` 增加合同，固定仓配对必须走 warehouse fact lookup，且 integration pom 不得直接依赖 warehouse 模块。

4. `npm run verify:three-terminal` 暴露 React typecheck P0。
   - `Product/Review/index.tsx` 中 `useRef<ActionType>()` 在当前 React 类型下缺少初始值。
   - 已按 ProTable 常用形态改为 `useRef<ActionType>(null)`，不调整页面行为。

## P2 记录

- SQL guard 自动发现动态 DML 的覆盖仍偏保守；当前高风险脚本已有手工合同兜底，后续可增强通用 pattern。
- 前端关键测试自动纳管仍依赖文件名正则，后续可按受保护文件清单反向映射。
- 前端 401 端隔离逻辑目前有两套实现，当前行为一致，后续可抽公共 helper 降低漂移风险。
- direct-login 旧 Redis key 仍有 delete-only 清理分支，当前未发现读写依赖。
- 目标追踪历史块仍有旧口径残留，但顶部现行口径索引已覆盖；后续可分批清理历史 Markdown。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory -Dtest=InventoryOverviewRefreshContractTest test`：通过，1 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration -am "-Dtest=IntegrationAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，6 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl warehouse -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 14 个 Jest suite / 93 个测试、React typecheck、四个前端 guard、后端 reactor test-compile 和后端三端合同链路均通过。

## 未执行

- 未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图或 DOM 验收。
- 未执行 UI 细调。
