# 2026-06-07 三端 P0/P1 快速推进：Product 到 Integration 公开 Facade 边界记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 1. 本轮目标

- 收口 `product` 模块直接 import `integration.service.impl.*` 的模块边界 P1。
- 保留 `integration` 对来源商品库和来源库存读模型的内部实现所有权。
- 让 `product` 只依赖 `integration` 的公开 service/facade 合同。

## 2. 子 Agent 使用情况

- 本片未新增子 Agent。
- 原因：上一检查点已用 6 个子 Agent 复核并明确该 P1 切片，当前工作是落实已确认结论；继续开只读 Agent 会重复扫描同一问题。
- 若后续推进 mapper 跨来源表读写迁移，需要先写边界方案，再按 facade/mapper/service 拆分使用子 Agent。

## 3. 已完成

- 新增 `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/ISourceReadModelRefreshService.java`：
  - 暴露 `refreshOfficialMasterByConnection(String connectionCode)`。
  - 作为 product 可依赖的公开刷新合同。
- 新增 `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/SourceReadModelRefreshServiceImpl.java`：
  - 在 integration 内部组合 `SourceProductReadModelService` 和 `SourceWarehouseStockReadModelService`。
  - 对外只暴露统一刷新 facade。
- 更新 `ProductDistributionServiceImpl`：
  - 删除 `SourceProductReadModelService` / `SourceWarehouseStockReadModelService` 两个 integration impl import。
  - 改为注入 `ObjectProvider<ISourceReadModelRefreshService>`。
  - 来源绑定释放/投影同步后只调用公开 facade 刷新读模型。
- 新增 `ProductModuleBoundaryContractTest`：
  - 扫描 `product/src/main/java`。
  - 禁止 product 主代码 import `com.ruoyi.integration.service.impl.*`。
- 更新 `react-ui/scripts/verify-three-terminal.mjs`：
  - 将 `ProductModuleBoundaryContractTest` 加入后端三端验证清单和显式 critical 集合。

## 4. 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration,product -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductModuleBoundaryContractTest,ProductDistributionMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`2` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 `7` 个 Jest suite / `33` 个测试通过；后端 ruoyi-system `143`、ruoyi-framework `15`、integration `4`、product `3`、seller `89`、buyer `90` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 `5` 个变更文件，Added `3`、Modified `2`，共 `234` 个节点。

## 5. 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮只收敛 Java service 依赖边界，不迁移 `ProductDistributionMapper.xml` 中直接读写来源表的 SQL。

## 6. 残留 P1

- `ProductDistributionMapper.xml` 仍直接读写 `source_product_*`、`source_product_warehouse_detail`、`upstream_system_*` 等来源/integration 表；这不是简单 import 问题，需要先明确来源快照、官方仓派生、SKU pairing 投影的事实归属和 facade 合同。
- 商品库存聚合字段 `available_stock`、`warehouse_count`、`inventory_status` 仍需先定库存事实源、SPU 汇总/去重规则和状态枚举生成规则，不能用 `master_sku` 快速 join。
