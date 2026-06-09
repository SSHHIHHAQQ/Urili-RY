# 2026-06-08 三端隔离 P0/P1 快速推进记录：库存刷新链路与验证闸门

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：优先尝试 6 个 GPT-5.3 Codex 子 Agent，工具模型为 `gpt-5.3-codex-spark`。
- 平台返回额度限制：提示到 `2026-06-14 15:12` 后再试；6 个 GPT-5.3 Codex 子 Agent 均已关闭。
- 按回退规则启动 6 个 `gpt-5.4` 只读子 Agent，覆盖 seller/buyer 后端、Portal 鉴权和 direct-login、SQL guard、React 运行入口、product/inventory/integration 共享域、验证闸门。
- 6 个 `gpt-5.4` 子 Agent 均已返回并关闭。

## 采纳的 P1

- `20260607_upstream_task_component_split.sql` 只按整个 `sys_job` 目标集合做 count/signature guard，没有校验旧 invoke target 和新 invoke target 在同一组内唯一；历史库如果同时存在旧/新行，可能把多行更新成同一目标。
- 库存总览 admin route 合同只校验 `.ts/.tsx`，漏掉实际存在的 `.js` 镜像；存在“TS 正确、JS 漂移、gate 仍通过”的空档。
- `verify-three-terminal` 的关键测试发现规则没有覆盖 `inventory/src/test/java` 和库存总览前端测试，库存模块实现改动可能只经过 test-compile，而没有被提升为必跑合同。
- 来源仓库库存读模型在仓库/SKU 配对变更后不会回刷，导致来源库存页配对状态可能一直停留在旧快照。
- 商品新增、编辑、重绑发货仓库或上游库存同步后，库存总览 read model 没有运行时刷新入口，商品接口读取的 `warehouseCount`、`availableStock`、`inventoryStatus`、`stockUpdateTime` 可能滞后。

## 已完成

- `20260607_upstream_task_component_split.sql` 增加 5 组 invoke target 唯一性 fail-closed 检查，并把 5 个 `sys_job` 更新包进事务。
- `SqlExecutionGuardContractTest` 固定任务组件拆分 seed 必须具备唯一性检查、`start transaction` 和 `commit`。
- 库存总览相关 `.js` 文件改为纯 re-export：service、page、helpers、`QuantityCell`、`SkuWarehouseTable`、`SpuSkuWarehouseTable`、`WarehouseViewTable`。
- `InventoryAdminRouteContractTest` 同步校验库存总览 `.js` 镜像必须纯转发 TS/TSX。
- 新增 `react-ui/tests/inventory-overview-contract.test.ts`，固定库存总览 admin API、权限 guard、JS 镜像和三端 manifest 收录关系。
- `verify-three-terminal.mjs` 的关键后端测试路径加入 `inventory/src/test/java`，关键前端测试路径加入 `inventory-overview`。
- `three-terminal.manifest.json` 纳入 `InventoryOverviewRefreshContractTest` 和 `inventory-overview-contract.test.ts`。
- `inventory` 模块补 JUnit 测试依赖，并新增 `InventoryOverviewRefreshContractTest`，固定库存总览运行时刷新合同。
- `IInventoryOverviewService` 增加 `refreshProductInventoryOverview(spuId)` 和 `refreshSourceInventoryOverviewByConnection(connectionCode)`。
- `InventoryOverviewMapper.xml` 增加按 SPU 删除过期库存行、upsert 当前库存行、按 connection 查询受影响 SPU 的运行时刷新入口；同一 `stock_key` 的平台库存值通过 upsert 保留。
- `ProductDistributionServiceImpl` 在商品新增/编辑保存 SKU、仓库、属性和图片后触发库存总览刷新。
- `UpstreamSystemMapper.xml` 增加仓库/SKU 配对变更后回刷库存快照配对字段的方法。
- `UpstreamSystemServiceImpl` 在仓库配对、SKU 配对和连接主信息变更后重建来源库存读模型。
- `UpstreamSyncServiceImpl` 在库存同步重建来源库存读模型后触发库存总览按 connection 刷新。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory "-Dtest=InventoryOverviewRefreshContractTest" test`：通过，1 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,InventoryAdminRouteContractTest" test`：通过，63 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/inventory-overview-contract.test.ts --runInBand`：通过，1 个 suite / 3 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration,product -am test-compile`：通过，8 个模块参与 test-compile。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard、React typecheck、14 个 Jest suite / 70 个测试、后端 reactor `test-compile` 和后端三端合同测试全部通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 已同步本轮变更。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 当前残留项

- P2：库存总览刷新当前采用按 SPU/connection 的同步重建方式，优先保证正确性；后续数据量上来后，可单独优化为更细粒度的异步刷新队列。
- P2：`buyer` portal 仍是“在售公开浏览”口径，没有 `buyerId` 维度范围约束；这属于业务口径待确认，不纳入本轮 P1。
- P2：工作区仍有较多历史 `.js` 镜像和未跟踪生成文件；本轮只收敛验证点名且影响 P0/P1 的入口。
