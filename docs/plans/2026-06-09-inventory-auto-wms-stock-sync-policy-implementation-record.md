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

首次实现时没有直接执行该 SQL。2026-06-09 已按用户“执行”授权应用到当前 `.env.local` 指向的远端 MySQL `fenxiao`，执行记录见：

- `docs/plans/2026-06-09-inventory-auto-wms-stock-sync-policy-sql-execution-record.md`

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
  - 已执行 `20260609_inventory_auto_wms_stock_sync_policy.sql`，并重新 package / 启动 `ruoyi-admin.jar`。
  - 新接口 `/inventory/admin/overview/seller/options` 和 `/inventory/admin/overview/official-warehouse/options` 已在 8080 返回 200。
  - 浏览器弹窗中卖家下拉已有真实卖家选项，官方仓多选只显示官方仓主数据，不再混入库存占位仓库名。

## 后续注意

- 自动同步策略 SQL 已执行；后续如果回放到其它环境，需要重新按目标环境预览签名并执行。
- 如果后续新增新的自动同步口径，必须同时扩展 SQL 字典、后端策略服务、前端文案、合同测试和库存流水类型。
- 如果要让卖家端也配置该能力，应复用现有同步策略服务，不要在卖家端页面复制库存计算逻辑。
