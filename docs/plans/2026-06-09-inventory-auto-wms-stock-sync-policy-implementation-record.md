# 库存总览自动同步 WMS 库存实施记录

## 背景

库存总览需要支持“自动同步WMS库存”模式，用于卖家把来源仓库的可用库存持续同步为平台总库存。该能力同时需要支持卖家维度、仓库设置、SPU设置、SKU设置和 SKU+仓库明细行设置，并且保留手动设置平台库存模式。

## 本轮实现

- 新增库存同步策略模型：
  - `inventory_stock_sync_policy`
  - 策略范围：`SELLER`、`WAREHOUSE`、`SPU`、`SKU`、`SKU_WAREHOUSE`
  - 同步方式：`MANUAL`、`AUTO_SOURCE_AVAILABLE`
- 扩展 SKU+仓库库存行：
  - `sync_mode`
  - `sync_policy_id`
  - `sync_policy_scope`
  - `sync_policy_key`
  - `sync_status`
  - `last_auto_sync_time`
- 扩展库存流水：
  - 同步策略 ID、范围、key 快照
  - 自动同步流水操作类型 `AUTO_SOURCE_SYNC`
- 扩展 SKU/SPU 永久读模型：
  - 卖家编号、卖家名称
  - 同步方式摘要、同步策略范围摘要
- 后端新增接口：
  - `GET /inventory/admin/overview/seller/options`
  - `POST /inventory/admin/overview/sync-policy/preview`
  - `POST /inventory/admin/overview/sync-policy/confirm`
- 后端规则：
  - 来源刷新后先应用自动同步策略，再刷新 SKU/SPU 永久读模型。
  - 自动同步模式只按来源可用库存计算平台总库存，不直接把来源在途库存加入平台总库存。
  - 自动同步模式下禁止手动调整平台总库存。
  - 自动同步后的平台总库存不会低于平台锁定库存。
- 前端新增：
  - 库存总览统一筛选区增加卖家和同步方式筛选。
  - SPU/SKU/仓库视图增加卖家和同步方式展示。
  - 工具栏增加“自动同步WMS库存设置”按钮。
  - SPU、SKU、SKU+仓库明细行增加“同步方式”操作。
  - 新增两段式弹窗：先预览影响，再确认应用。
  - 仓库视图操作列取消右固定，避免右侧固定列压住中间滚动区域。
- 复用台账已登记 `InventorySyncPolicy` 组件和后端策略服务。

## 本次修复

- 将自动同步设置范围文案从“卖家默认”改为“卖家维度”。
- 卖家下拉改为读取 `seller` 主表，避免库存读模型为空时卖家设置和仓库设置没有可选卖家。
- 新增官方仓库选项接口 `GET /inventory/admin/overview/official-warehouse/options`，仓库设置弹窗只使用 `warehouse` 主数据里的正常官方仓，不再混入“来源SKU未绑定”“发货仓库未配置”等库存占位行。
- 仓库设置的仓库字段改为多选；后端会把多个仓库拆成多条策略预览和保存，保存失败时整体回滚，不允许部分成功。
- SPU、SKU、SKU+仓库明细行设置从手输 ID 改为可搜索选择器，选择后自动带出后端计算所需的卖家、SPU、SKU 和仓库信息。

## SQL 状态

已新增迁移脚本：

- `RuoYi-Vue/sql/20260609_inventory_auto_wms_stock_sync_policy.sql`

本轮没有直接执行该 SQL。脚本包含确认变量、基础表存在校验、菜单 slot guard、动态 DDL guard 和权限 seed。

执行前需要显式设置：

```sql
set @confirm_inventory_auto_wms_stock_sync_policy = 'APPLY_INVENTORY_AUTO_WMS_STOCK_SYNC_POLICY';
```

## 验证记录

- 后端 clean compile：
  - `mvn -pl inventory -am -DskipTests clean compile`
  - 结果：通过
- 后端库存/路由合同测试：
  - `mvn -pl inventory,ruoyi-system -am "-Dtest=InventoryOverviewRefreshContractTest,InventoryAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：通过
- SQL guard 合同：
  - `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：通过
- 前端库存总览合同测试：
  - `./node_modules/.bin/jest.cmd --config jest.config.ts tests/inventory-overview-contract.test.ts --runInBand`
  - 结果：通过
- 前端类型检查：
  - `npm run tsc`
  - 结果：通过
- 浏览器验证：
  - 打开 `http://127.0.0.1:8001/inventory/overview`
  - SPU视图、SKU视图、仓库视图可切换。
  - 统一筛选区可见卖家和同步方式筛选。
  - 工具栏可见“自动同步WMS库存设置”。
  - 弹窗可打开，未选择卖家时预览会被表单校验拦截。
  - 仓库视图取消操作列右固定后，未再出现右侧固定列压住中间列的切割效果。

## 本次修复复验

- 代码级验证：
  - `mvn -pl inventory -am -DskipTests clean compile`：通过。
  - `mvn -pl inventory,ruoyi-system -am "-Dtest=InventoryOverviewRefreshContractTest,InventoryAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
  - `./node_modules/.bin/jest.cmd --config jest.config.ts tests/inventory-overview-contract.test.ts --runInBand`：通过。
  - `npm run tsc`：通过。
  - `git diff --check`：通过，仅有当前工作区 LF/CRLF 提示。
  - `codegraph sync .`：通过，结果为 already up to date。
- 浏览器复验：
  - `http://127.0.0.1:8001/inventory/overview` 可打开库存总览。
  - 自动同步弹窗中设置范围显示为“卖家维度”，没有旧文案“卖家默认”。
  - 仓库设置下同时显示卖家选择和仓库选择，仓库选择是 Ant Design 多选控件。
  - 仓库设置的下拉未再显示“来源SKU未绑定”“发货仓库未配置”等库存占位仓库名。
- 运行态说明：
  - 当前 `127.0.0.1:8080` 仍未暴露本轮新增的 `/inventory/admin/overview/seller/options` 和 `/inventory/admin/overview/official-warehouse/options` 映射，直接探测返回旧运行态的 `No static resource ...`。
  - 本轮未执行 `20260609_inventory_auto_wms_stock_sync_policy.sql`，也未重启后端；需要在 SQL 明确授权并应用后，再重启新 jar 做完整运行态复验。

## 后续注意

- 真正启用后端新增接口前，需要确认目标数据库并执行迁移脚本。
- 如果后续新增新的自动同步口径，必须同时扩展 SQL 字典、后端策略服务、前端文案、合同测试和库存流水类型。
- 如果要让卖家端也配置该能力，应复用现有同步策略服务，不要在卖家端页面复制库存计算逻辑。
