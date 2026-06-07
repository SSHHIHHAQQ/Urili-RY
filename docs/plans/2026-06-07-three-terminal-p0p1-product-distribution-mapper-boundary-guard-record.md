# 2026-06-07 三端 P0/P1：商品分销 Mapper 边界与库存占位 Guard

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

本轮聚焦 `ProductDistributionMapper.xml` 的两个残留 P1：

- 商品分销 mapper 仍直接读写来源商品、来源仓、`upstream_system_sku_pairing` 等跨模块表，需要先冻结当前 debt surface，避免继续扩散。
- 商品分销列表已经暴露 `available_stock`、`warehouse_count`、`inventory_status`、`stock_update_time` 字段，但当前仍是显式占位，不能被临时 `master_sku` 或来源库存 join 伪接通。

## 子 Agent 执行情况

- 先按用户最新要求尝试 6 个 `gpt-5.3-codex-spark` 只读子 Agent；平台返回用量限制，失败 Agent 已关闭。
- 回退使用 `gpt-5.4`，共 6 个只读子 Agent 完成扫描，均已关闭。
- 采纳的 P1：
  - `ProductDistributionMapper.xml` 跨模块表访问最适合收口在现有 `ProductDistributionMapperContractTest`，不需要新增测试类或修改 `verify-three-terminal.mjs`。
  - 当前允许的跨模块 debt surface 应先按 statement 级 allowlist 冻结；完整迁移到 facade/读模型前，不继续扩散。
  - 商品库存聚合字段当前只能保持显式 `null as ...` 占位，并禁止 mapper 直接引用来源库存读模型或库存快照源伪造真实库存。
- 未纳入本轮：
  - 不重构 mapper 到 integration facade。
  - 不设计真实库存事实源、SPU 汇总/去重规则或库存状态枚举。
  - 不调整前端库存展示文案和列样式。

## 已完成

- `ProductDistributionMapperContractTest` 新增 `externalTableUsageMustStayWithinExplicitDebtAllowlist()`。
  - 按 XML statement 解析 `select/insert/update/delete`。
  - 只允许当前已知跨模块表出现在指定 statement。
  - 后续如果新增跨模块表或把当前外部表扩散到新 statement，测试会 fail-closed。
- `ProductDistributionMapperContractTest` 新增 `inventorySummaryFieldsMustStayExplicitPlaceholdersUntilInventoryFactSourceIsDesigned()`。
  - 固定 `available_stock`、`warehouse_count`、`inventory_status`、`stock_update_time` 每个字段当前必须保持 6 处显式 `null as ...`。
  - 禁止 `ProductDistributionMapper.xml` 直接引用 `source_warehouse_stock_*` 或 `upstream_system_sku_inventory_snapshot` 伪接通商品库存。
- 文件大小自查：`ProductDistributionMapperContractTest.java` 当前 340 行，触发 300 行职责检查但未达到 400 行阈值；该文件仍是单一 mapper XML 静态合同，且已被 `verify-three-terminal.mjs` 显式覆盖，本轮不拆分。
- 已更新 `docs/architecture/reuse-ledger.md`。

## 当前允许的跨模块 Mapper Debt Surface

| XML statement | 当前允许外部表 | 说明 |
| --- | --- | --- |
| `selectSourceBindingSnapshot` | `source_product_dimension_group`、`source_product_warehouse_detail` | 读取官方来源 SKU 尺寸组快照。 |
| `selectOfficialWarehousesBySourceDimensionGroup` | `source_product_warehouse_detail`、`upstream_system_warehouse_pairing`、`warehouse` | 通过来源 SKU 尺寸组和履约仓配对推导官方仓。 |
| `selectSourceConnectionCodesByDimensionGroup` | `source_product_warehouse_detail` | 读取来源连接编码。 |
| `deleteUpstreamSkuPairingsBySystemSku` | `upstream_system_sku_pairing` | 当前仍作为来源商品/库存读模型配对投影同步 debt。 |
| `upsertUpstreamSkuPairingsForBinding` | `source_product_warehouse_detail`、`upstream_system_sku_pairing` | 读取来源仓明细并写配对投影。 |

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductDistributionMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`ProductDistributionMapperContractTest` 3 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 7 个 Jest suite / 33 个测试通过；后端 ruoyi-system 147、ruoyi-framework 15、integration 4、product 5、seller 92、buyer 93 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" "RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductDistributionMapperContractTest.java" "docs/plans/2026-06-07-three-terminal-p0p1-product-distribution-mapper-boundary-guard-record.md" "docs/architecture/reuse-ledger.md" "docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md"`：无输出，当前切片新增/变更文件未发现尾随空白。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；输出 `Synced 1 changed files`、`Modified: 1 - 37 nodes`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮只是冻结当前 mapper debt 和库存占位语义，不代表真实库存已经接通。

## 残留 P1

- `ProductDistributionMapper.xml` 仍存在当前 allowlist 内的跨模块读写 debt；后续要彻底消除，需要先定来源快照、SKU pairing 投影、官方仓派生和读模型刷新 facade 的事实归属。
- 商品库存聚合字段仍需后续按库存事实源、SPU 汇总/去重规则、仓库口径和状态枚举生成规则设计后再接通。
- 前端商品分销页目前仍有库存字段展示位，后续如接真实库存，应同步补前端展示合同，避免用 `availableStock > 0` 单独推导库存状态。
