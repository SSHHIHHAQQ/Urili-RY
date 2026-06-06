# 上游系统管理 WMS 尺寸与 SKU 库存执行记录

日期：2026-06-06

## 当前目标

在上游系统管理中补齐两类来源数据能力：

1. 仓库尺寸重量：使用领星 OMS v1 `product/pagelist` 的 `skuList` 精准查询补齐 `wmsLength/wmsWidth/wmsHeight/wmsWeight`。
2. SKU 库存：由上游系统管理拉取并保存上游 SKU 仓库库存快照，作为“来源仓库库存”的数据源。

## 已完成

- 后端 Lingxing OMS v1 client：
  - 新增 `listProductSkuPageBySkuList(...)`，用于 WMS 尺寸重量精准补查。
  - 新增 `listInventoryProductPage(...)`，调用 `/inventory/productPage` 拉取库存分页。
- 后端同步逻辑：
  - SKU 同步后自动按 `skuList` 补查 WMS 尺寸重量。
  - 普通 SKU 分页同步不会再用空 WMS 字段覆盖已补齐字段。
  - 新增库存同步锁、库存同步状态、库存快照 upsert、旧快照 `MISSING` 标记。
- 后端接口：
  - `POST /integration/admin/upstream-systems/{connectionCode}/sku-dimensions/sync`
  - `POST /integration/admin/upstream-systems/{connectionCode}/inventory/sync`
  - `GET /integration/admin/upstream-systems/{connectionCode}/inventory/list`
  - `GET /integration/admin/upstream-systems/{connectionCode}/inventory-sync-state`
  - `GET /integration/admin/source-warehouse-stocks/list`
- 定时任务入口：
  - 已有 `upstreamSystemTask.syncSkus` 保持 10 分钟一次，SKU 同步内包含 WMS 尺寸补查。
  - 新增 `upstreamSystemTask.syncInventory`，用于库存快照 10 分钟同步。
- 前端：
  - 上游系统管理 Tabs 增加“仓库尺寸重量”和“SKU库存”。
  - “来源仓库库存”页面读取同一份库存快照列表。
- SQL：
  - 更新 `RuoYi-Vue/sql/upstream_system_management_seed.sql`。
  - 新增增量脚本 `RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql`。

## 表与权限

新增表：

- `upstream_system_sku_inventory_snapshot`
- `upstream_system_inventory_sync_state`

新增权限：

- `integration:upstream:dimensionSync`
- `integration:upstream:inventoryQuery`
- `integration:upstream:inventorySync`

增量 SQL 会把已有 `integration:upstream:sync` 角色授予尺寸/库存同步权限，把已有 `integration:upstream:query` 角色授予库存查看权限。

## 已验证

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：通过，CodeGraph 已是最新。

```powershell
npx --yes --package @playwright/cli playwright-cli open http://127.0.0.1:8001/overseas-warehouse-service/upstream-system
npx --yes --package @playwright/cli playwright-cli snapshot
```

结果：通过。页面可打开，“仓库尺寸重量”和“SKU库存”Tab 均可渲染；库存同步状态和库存列表接口返回 200。当前库存快照为空，SKU库存表格显示空态。

## 未执行

- 未执行数据库 DDL/DML。
- 未重启后端。
- 未触发真实库存同步。此前真实调用 `/inventory/productPage` 返回过“无接口权限”，需要领星开通接口权限后再验证库存入库。

## 注意事项

- 上游库存快照不是平台可售库存，不写商品/库存事实表。
- “来源仓库库存”以后应读取 `upstream_system_sku_inventory_snapshot`，不重复做领星适配。
- 如果领星库存接口返回字段名与当前兼容映射不一致，先从 `source_payload_json` 和请求日志定位字段，再补解析映射。

## 2026-06-06 库存同步清单修复

问题：

- `SKU库存同步清单` 前端组件存在，但没有挂回上游系统管理 Tabs。
- 后端库存同步、库存列表、库存同步状态接口被禁用，缺少路由映射。
- 定时任务入口被改成禁用方法，增量 SQL 也是禁用占位脚本。

修复：

- `SyncTabs.tsx` 已补回 `SKU库存同步清单` Tab，位置在“仓库尺寸重量”和“请求日志”之间。
- `AdminUpstreamSystemController` 已恢复：
  - `POST /integration/admin/upstream-systems/{connectionCode}/inventory/sync`
  - `GET /integration/admin/upstream-systems/{connectionCode}/inventory/list`
  - `GET /integration/admin/upstream-systems/{connectionCode}/inventory-sync-state`
- `UpstreamSystemTask` 已恢复 `upstreamSystemTask.syncInventory()`。
- `20260606_upstream_inventory_dimension_sync.sql` 已改成可执行的幂等迁移脚本，库存定时任务通过若依 `sys_job` 注册：
  - 任务名称：`领星SKU库存每10分钟同步`
  - 调用目标：`upstreamSystemTask.syncInventory`
  - cron：`0 0/10 * * * ?`
  - 任务分组：`SYSTEM`
  - 状态：`正常`

验证：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：通过。

未执行：

- 本次未直接执行数据库 SQL；需要执行 `RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql` 后，定时任务菜单才会在当前运行库看到该任务。

## 2026-06-06 最终修复复核

问题：

- 旧 MCP/REPL 后台进程把库存同步相关文件反复写回“schema 未确认”的占位版本，导致 `SKU库存同步清单` Tab 虽然存在，但内容退回 `Empty` 占位；后端库存路由、来源仓库库存 Controller、Quartz 任务入口和增量 SQL 也被回退为禁用状态。
- `来源仓库库存` 菜单还指向占位组件，未读取上游库存快照。

修复：

- 清理旧 MCP/REPL 工作进程，避免继续覆盖当前文件。
- 恢复 `SkuInventoryPanel.tsx` 为真实库存同步面板，包含同步状态、筛选、同步按钮和库存表格。
- 恢复前端库存接口：
  - `POST /api/integration/admin/upstream-systems/{connectionCode}/inventory/sync`
  - `GET /api/integration/admin/upstream-systems/{connectionCode}/inventory/list`
  - `GET /api/integration/admin/upstream-systems/{connectionCode}/inventory-sync-state`
  - `GET /api/integration/admin/source-warehouse-stocks/list`
- 恢复后端若依 Controller 路由和权限：
  - `integration:upstream:inventorySync`
  - `integration:upstream:inventoryQuery`
  - `inventory:sourceWarehouse:list`
- 移除 `UpstreamSystemServiceImpl` 中库存 schema 未确认的禁用开关。
- 恢复 `upstreamSystemTask.syncInventory()`，通过若依 Quartz 任务入口同步所有启用的领星 WMS 主仓库存快照。
- 恢复 `20260606_upstream_inventory_dimension_sync.sql` 为幂等建表、授权和 `sys_job` 注册脚本。
- 更新 `business_menu_seed.sql` 和 `20260606_source_warehouse_stock_menu_rename.sql`，让“来源仓库库存”菜单指向真实组件 `Inventory/SourceWarehouseStock/index`。

当前运行库已验证：

- 若依定时任务菜单数据来自 `sys_job`，不是自定义调度。
- 任务名称：`领星SKU库存每10分钟同步`
- 任务分组：`SYSTEM`
- 调用目标：`upstreamSystemTask.syncInventory`
- cron：`0 0/10 * * * ?`
- 状态：`0`，正常。
- “来源仓库库存”菜单组件：`Inventory/SourceWarehouseStock/index`。

验证命令：

```powershell
cd E:\Urili-Ruoyi\react-ui
$env:DISABLE_MFSU='1'
npm run tsc
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
mvn -pl ruoyi-admin -am -DskipTests package
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart
```

结果：后端启动成功，`http://127.0.0.1:8080` 返回 200。

页面验证：

- 使用 Playwright CLI 登录 `http://127.0.0.1:8001/overseas-warehouse-service/upstream-system`。
- Tabs 显示顺序包含：`领星仓库同步清单 / 领星物流渠道同步清单 / 领星SKU同步清单 / 仓库尺寸重量 / SKU库存同步清单 / 请求日志`。
- 点击 `SKU库存同步清单` 后，页面显示真实库存同步面板，不再是占位 Empty。
- 面板包含：同步状态、上次成功、下次同步、库存条数、错误信息、`同步库存` 按钮、SKU/仓库筛选、库存口径/配对/同步状态筛选、库存表格列。

残留外部依赖：

- 当前库存同步状态显示 `无接口权限`，这是领星库存接口权限未开通导致；本地任务、接口、表和页面已经恢复。领星开通 `/inventory/productPage` 权限后，当前 10 分钟任务和手动“同步库存”按钮会写入 `upstream_system_sku_inventory_snapshot`。
## 2026-06-06 库存接口路径修复

只读调查结论：

- 当前系统实际调用 `https://api.xlwms.com/openapi/v1/inventory/productPage`，请求日志返回 `11008 / 无接口权限`。
- 领星 OMS v1 官方库存文档对应接口为 `POST https://api.xlwms.com/openapi/v1/integratedInventory/pageOpen`，标题为“分页查询综合库存”。
- 旧项目方案文档中库存同步同样指向 `integratedInventory/pageOpen`。

修复内容：

- `LingxingOpenApiClient.listInventoryProductPage(...)` 改为调用 `/integratedInventory/pageOpen`。
- 按官方响应字段映射库存：
  - `totalAmount` -> 综合库存总量，口径 `COMPREHENSIVE`
  - `productTotalAmount` + `productStockDtl.availableAmount/lockAmount/transportAmount` -> 产品库存，口径 `PRODUCT`
  - `boxTotalAmount` + `boxStockDtl.availableAmount/lockAmount/transportAmount` -> 箱库存，口径 `BOX`
  - `fbaReturnTotalAmount` + `fbaReturnStockDtl.availableAmount/lockAmount/transportAmount` -> 退货库存，口径 `RETURN`
- `stockType` 原样保存到 `inventory_attribute`，保留领星原始库存属性 code。
- 新接口按 `operateTime` 时间窗口返回库存流水变动范围内的数据，不是严格全量快照；因此库存同步不再把“本批未返回”的旧库存行标记为 `MISSING`，避免 10 分钟增量同步误伤未发生变动的库存。
- 首次成功同步不传时间窗口，走领星默认时间范围；后续成功同步后，按上次成功时间向前重叠 5 分钟生成 `startTime/endTime`。

本次未执行：

- 未手动点击页面“同步库存”。
- 未主动调用库存同步接口写入业务库存快照。
