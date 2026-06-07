# 来源仓库库存读模型与合并展示实施记录

日期：2026-06-07

## 目标

将「库存管理 / 来源仓库库存」从库存快照明细平铺列表，调整为按来源 SKU + 商品名合并展示，并直接采用永久读模型表，避免列表请求实时聚合大表。

## 本次改动

### 数据库脚本

新增：

- `RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql`

内容：

- 新增 `source_warehouse_stock_group`：父级列表读模型。
- 新增 `source_warehouse_stock_detail`：展开明细读模型。
- 新增 `source_warehouse_stock_filter_metric`：常用筛选指标读模型。
- SQL 使用 `@confirm_source_warehouse_stock_read_model = APPLY_SOURCE_WAREHOUSE_STOCK_READ_MODEL` guard，未确认时 fail-closed。
- SQL 已增加源表/源列前置校验：确认 `upstream_system_sku_inventory_snapshot` / `upstream_system_connection` 及关键列存在后，才允许创建、定向删除和回填读模型。
- SQL 已固定 `source_warehouse_stock_group.search_text` 中的 `group_concat` 使用 `order by`，避免同批源数据重放后摘要顺序抖动。
- 脚本包含从 `upstream_system_sku_inventory_snapshot` 初始回填读模型的 DML。

### 后端

新增：

- `SourceWarehouseStockGroupItem`
- `SourceWarehouseStockReadModelService`

调整：

- `SourceWarehouseStockQuery` 增加 `sourceStockGroupKey`、`repositoryScope`。
- `AdminSourceWarehouseStockController` 新增：
  - `GET /integration/admin/source-warehouse-stocks/groups/list`
  - `GET /integration/admin/source-warehouse-stocks/groups/detail`
  - `GET /integration/admin/source-warehouse-stocks/options/master-warehouses`
  - `GET /integration/admin/source-warehouse-stocks/options/source-warehouses`
- `IUpstreamSystemService` / `UpstreamSystemServiceImpl` / `UpstreamSystemMapper` 增加读模型列表、明细和重建方法。
- `SourceWarehouseStockQuery` 增加来源主仓、来源仓库编码，以及总库存、可用库存、锁定库存、在途库存的最小值/最大值筛选字段。
- 库存同步成功后调用 `SourceWarehouseStockReadModelService.rebuildOfficialMasterByConnection(connectionCode)`，按连接重建受影响来源库存组。

### 前端

调整：

- `react-ui/src/pages/Inventory/SourceWarehouseStock/index.tsx`
  - 父级 ProTable 改读 `/groups/list`。
  - `rowKey` 使用 `sourceStockGroupKey`。
  - 展开行调用 `/groups/detail` 展示明细。
  - 父级按来源 SKU、商品名、来源主仓、仓库数、库存属性、库存数量、系统仓库、商城 SKU、客户、配对状态、同步状态展示。
  - 「来源主仓」「来源仓库」筛选改为可搜索下拉。
  - 在原 ProTable 筛选区内增加「总库存数」「可用库存数」「锁定库存数」「在途库存数」方案 B 紧凑区间输入。
- `react-ui/src/services/integration/sourceWarehouseStock.ts`
  - 新增分组列表、分组明细、来源主仓选项、来源仓库选项请求函数。
- `react-ui/src/types/integration/upstream-system.d.ts`
  - 新增 `SourceWarehouseStockGroupItem` 和分组分页结果类型。
- `react-ui/src/services/integration/constants.ts`
  - 抽取库存属性选项：`0 = 正品`、`1 = 次品`。

### 复用台账

更新：

- `docs/architecture/reuse-ledger.md`

记录来源仓库库存应读取 `source_warehouse_stock_*` 读模型，不回退到实时聚合快照大表；读模型不是平台真实库存或库存流水事实表。

## 权限检查

本次新增接口复用现有只读权限：

```text
inventory:sourceWarehouse:list
```

未新增同步、导出、重建、删除等按钮权限。

## 字典和选项检查

- 库存口径继续复用 integration 共享选项。
- 同步状态继续复用 integration 共享选项。
- 配对状态继续复用 integration 共享选项。
- 库存属性已抽到 integration constants，页面不再单独硬编码搜索选项。
- 来源主仓和来源仓库下拉选项来自 `source_warehouse_stock_filter_metric` 读模型，不在页面内手写。

## 数据边界

事实源仍是：

```text
upstream_system_sku_inventory_snapshot
```

新增 `source_warehouse_stock_*` 只作为可覆盖读模型：

- 不作为平台真实库存。
- 不作为库存总览。
- 不作为库存流水。
- 不记录订单占用、履约扣减或财务库存。
- 不保存上游原始库存 JSON。

## 验证结果

### 数据源确认

已按当前后端激活配置确认：本次 SQL 与接口验证使用 `application.yml` / `application-druid.yml` 指向的运行数据源，连接参数来自本机环境变量配置；未使用 Docker 本地 MySQL / Redis。

### 数据库脚本

已在显式 guard 下执行：

```sql
set @confirm_source_warehouse_stock_read_model = 'APPLY_SOURCE_WAREHOUSE_STOCK_READ_MODEL';
```

执行后计数：

```text
upstream_system_sku_inventory_snapshot = 12404
source_warehouse_stock_group = 11860
source_warehouse_stock_detail = 12404
source_warehouse_stock_filter_metric = 84612
```

校验结果：

- `source_warehouse_stock_detail` 行数与库存快照行数一致。
- `source_warehouse_stock_group` 已按来源 SKU + 商品名 + 库存口径生成父级读模型。
- 抽样验证跨来源主仓同 SKU 合并：`taifu101` 在综合库存口径下父级明细数为 2，父级总库存 47 等于子级明细求和 47，可用库存 44 等于子级明细求和 44。
- 明细接口返回的来源主仓名为 `CA012` 这类主仓名，不展示系统类型或连接编码。

### SQL 合同收口

后续已补充专项 replay-safe 合同：

- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `sourceWarehouseStockReadModelMustStayReplaySafeAndScoped()`

合同锁定：

- `20260607_source_warehouse_stock_read_model.sql` 必须显式登记到高影响 SQL guard 清单。
- 确认 token 必须早于首个高影响 DDL/DML。
- 必须保留源表/源列前置校验，且校验必须早于创建、删除和回填读模型。
- 必须保留三张 `create table if not exists` 读模型表。
- 必须保留 `repository_scope = 'OFFICIAL_MASTER'` 定向删除和回填。
- 必须按 detail -> group -> filter_metric 顺序回填。
- 必须禁止 `truncate table`、`drop table`、无作用域 `delete from source_warehouse_stock_*`。
- 必须保留有序 `search_text` 聚合。

验证命令：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test
```

结果：通过，`SqlExecutionGuardContractTest` 30 个测试通过。

未变更边界：

- 本次合同收口未重新执行远程 SQL。
- 本次合同收口未解决 fresh bootstrap 策略：后续仍需决定 `source_warehouse_stock_*` 是吸收到 seed 基线，还是固定为 bootstrap 后必跑 SQL 清单。

### 后端编译与接口

已执行：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
```

结果：通过。

已执行：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests package
```

结果：通过，`ruoyi-admin.jar` 已重新打包。

已通过本机启动脚本重启后端，并验证：

```text
GET /integration/admin/source-warehouse-stocks/groups/list
GET /integration/admin/source-warehouse-stocks/groups/detail
GET /integration/admin/source-warehouse-stocks/options/master-warehouses
GET /integration/admin/source-warehouse-stocks/options/source-warehouses
```

结果：均返回 `code = 200`。综合库存分组列表返回 `total = 2965`，明细接口可按 `sourceStockGroupKey` 返回子级仓库库存。

补充筛选验证：

- `options/master-warehouses?inventoryScope=COMPREHENSIVE` 返回 `code = 200`，当前选项数为 2，包含 `CA012`。
- `options/source-warehouses?inventoryScope=COMPREHENSIVE` 返回 `code = 200`，当前选项数为 2，示例为 `CA91244744 / MEISU`。
- `groups/list?inventoryScope=COMPREHENSIVE&masterWarehouseName=CA012` 返回 `code = 200`。
- `groups/list?inventoryScope=COMPREHENSIVE&upstreamWarehouseCode=CA91244744` 返回 `code = 200`。
- `groups/list?inventoryScope=COMPREHENSIVE&totalQuantityMin=1&totalQuantityMax=50&availableQuantityMin=1&availableQuantityMax=50` 返回 `code = 200`，抽样行库存数量落在区间内。

### 前端静态验证

已执行：

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/pages/Inventory/SourceWarehouseStock/index.tsx config/routes.ts config/routes.js
npm run tsc -- --pretty false
```

结果：

- Biome lint 通过。
- TypeScript 编译通过。

### 浏览器验证

已通过 Codex in-app browser 验证：

- 可登录并进入 `http://127.0.0.1:8001/inventory/source-warehouse-stock`。
- 页面显示菜单名「来源仓库库存」。
- 筛选区显示「来源主仓」「来源仓库」「SKU / 商品」「同步状态」「仓库配对」「SKU配对」「库存属性」。
- 「来源主仓」「来源仓库」为可搜索下拉，来源主仓下拉可展开并显示 `CA012` / `NY013`。
- 原 ProTable 筛选区内显示「总库存数」「可用库存数」「锁定库存数」「在途库存数」紧凑区间输入；每个区间使用 `Space.Compact + InputNumber` 展示最小值和最大值。
- 页面层面输入总库存区间 `1 - 5` 后点击原筛选区「查询」，列表刷新为区间内库存行，抽样行总库存均在 `1 - 5` 范围内。
- Tabs 顺序为「综合库存 / 产品库存 / 退货库存 / 箱库存」，四个 Tab 均可切换。
- 父级表格显示「来源SKU、商品名称、来源主仓、来源仓库数、库存属性、总库存、可用库存、锁定库存、在途库存、箱内库存、系统仓库、商城SKU、客户、仓库配对、SKU配对、同步状态、更新时间」。
- 「库存属性」展示中文 `正品`，未展示 `0`。
- 抽样父级行显示来源主仓 `CA012` 或 `CA012 / NY013`，未展示系统类型和连接编码。
- 综合库存首行可展开，子级明细显示「来源主仓、来源仓库、库存属性、总库存、可用库存、锁定库存、在途库存、箱内库存、批次、库位、系统仓库、商城SKU、客户、仓库配对、SKU配对、同步状态、同步时间」。

## 残留问题

- 多个明细维度组合筛选时，第一版先保证命中父级和展开明细准确；如果父级数量也必须按任意组合筛选精确重算，需要再设计组合指标表。
- 如果 `search_text like` 在更大数据量下仍慢，需要补搜索 token 表或专用全文搜索方案。

## CodeGraph

已执行：

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：同步成功。
