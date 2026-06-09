# 库存总览平台库存基础版实施记录

日期：2026-06-07

状态：代码、基础增量 SQL、后端重启和浏览器基础验证已完成。2026-06-08 已执行 SKU 基线读模型刷新，当前商城 SKU 已进入库存总览。

## 本次目标

落地库存总览基础版，以“平台总库存”为平台当前库存池字段，支持 SPU / SKU / 仓库三种视图，并在 SKU + 仓库明细行上双击调整平台总库存和平台在途库存。

## 已完成

- 新增后端 `inventory` Maven 模块，并接入 `ruoyi-admin` 聚合编译。
- 新增库存总览增量 SQL：`RuoYi-Vue/sql/20260607_inventory_overview_platform_stock.sql`。
- 新增库存总览读模型修正 SQL：`RuoYi-Vue/sql/20260608_inventory_overview_sku_baseline_refresh.sql`。
- 新增库存总览核心表、流水表、锁定表、来源扣减待抵消表、在途跟踪表，以及 SKU/SPU 永久读模型表。
- 新增后端接口：`/inventory/admin/overview/**`。
- 新增前端页面：`react-ui/src/pages/Inventory/Overview`。
- 通用 `business_menu_seed.sql` 不再拥有 `2420` 库存总览菜单，避免通用 seed 覆盖专用菜单。
- 库存总览页面使用 Ant Design Pro 原生 ProTable 搜索区，不额外做独立筛选区。
- 前端已拆为主页面、SKU 仓库展开表、双击编辑单元格和展示 helper，避免单文件继续膨胀。
- 读模型刷新已调整为从商城 SKU 起步，来源绑定和仓库绑定缺失时生成占位库存行并显示状态。
- 新增库存状态：`仓库未配置`、`来源SKU未绑定`。
- SPU 视图展开层已调整为一次展开展示 SKU 分组和每个 SKU 的仓库库存明细，不再要求二次展开。
- 新增只读聚合接口 `GET /inventory/admin/overview/spu/{spuId}/sku-warehouses`，一次返回 SPU 下 SKU 读模型和对应仓库库存行。
- 新增仓库视图，按 `SKU + 仓库` 最小库存事实行平铺展示；同一 SKU 在多个仓库有库存时会显示多行。
- 新增分页接口 `GET /inventory/admin/overview/warehouse/list`，从 `inventory_sku_warehouse_stock` 读取仓库维度行，并左连 SKU 读模型补充商品名和 SKU 属性。
- 仓库视图的仓库类型筛选使用明细行专用选项，只保留 `官方仓`、`三方仓`、`未配置`，不展示聚合态 `混合`。
- SPU、SKU、仓库三种视图已补充 `配对状态` 筛选，支持查询 `已配对` / `未配对` 库存。
- SPU、SKU、仓库三种视图已补充第一版完整筛选：关键词、库存状态、配对状态、仓库类型、仓库、平台库存区间、来源库存区间、更新时间区间、来源同步时间区间。
- 新增 `GET /inventory/admin/overview/warehouse/options`，仓库下拉从当前库存明细读模型提取，覆盖官方来源主仓、三方仓和未配置占位仓库。
- 前端库存总览筛选统一使用 `buildInventoryOverviewListParams(...)` 将 Ant Design Pro 区间控件展开为后端 `Min/Max` 或 `Start/End` 参数，避免 SPU/SKU 和仓库视图重复写转换逻辑。
- 复用台账已追加库存总览平台库存模板。
- 设计文档状态已更新为“已确认并进入基础版实现”。

## 核心规则

- 官方仓库存来自来源仓库库存正品综合库存，按来源主仓展示，例如 `CA012`。
- 页面不展示上游系统名或外部接入编码作为官方仓明细主标识。
- 官方仓平台可售库存按 `min(平台总库存, 来源可用库存 - 待抵消来源扣减) - 平台锁定库存` 计算。
- 三方仓平台可售库存按 `平台总库存 - 平台锁定库存` 计算。
- 库存总览不再以来源 SKU 绑定或仓库绑定作为展示前提；有效商城 SKU 必须进入读模型。
- SPU 视图是商品维度入口，展开后直接显示 SKU + 仓库库存明细；SKU 视图是单 SKU 定位入口，仍支持展开查看仓库明细；仓库视图是最平铺的库存事实行入口，以 `SKU + 仓库` 为一行。
- 配对状态筛选只区分库存承载关系是否完整：`NO_WAREHOUSE` 和 `SOURCE_UNBOUND` 归为未配对；`NO_SOURCE`、`SOURCE_ONLY_IN_TRANSIT` 等仍是已配对但当前来源库存不足或仅在途。
- SKU + 仓库明细仍是最小库存事实维度；多个官方来源主仓或多个三方仓都会生成多条明细行。
- 未配置仓库的占位行不允许手工调整平台库存。
- 平台总库存不能调整到小于当前平台锁定库存。
- 三方仓不支持设置平台在途库存。
- 库存调整先 preview，再 confirm，落 `inventory_stock_ledger`。

## 权限检查

- 后端接口已加权限：
  - `inventory:overview:list`
  - `inventory:overview:query`
  - `inventory:overview:adjust`
  - `inventory:overview:ledger`
- 增量 SQL 专用维护 `2420` 菜单和按钮权限。
- 前端双击编辑会读取 `access.hasPerms('inventory:overview:adjust')`，无权限时不可编辑。
- `SqlExecutionGuardContractTest` 已增加库存总览 SQL guard 和通用菜单 seed 不抢占库存总览菜单的合同检查。

## 字典与选项

- 新增 `inventory_status` 字典。
- 追加 `NO_WAREHOUSE`、`SOURCE_UNBOUND` 库存状态。
- 新增 `inventory_operation_type` 字典。
- 前端状态展示使用集中 helper，未在页面内散落多份状态映射。

## 复用与重复代码

- 已更新 `docs/architecture/reuse-ledger.md`。
- 前端通用展示方法已拆到 `helpers.tsx`。
- 双击编辑单元格独立为 `components/QuantityCell.tsx`。
- SKU 仓库展开表独立为 `components/SkuWarehouseTable.tsx`，仓库明细表抽为 `WarehouseStockTable` 供 SPU 展开层复用。
- SPU 下 SKU + 仓库分组展开层独立为 `components/SpuSkuWarehouseTable.tsx`。
- 仓库平铺视图独立为 `components/WarehouseViewTable.tsx`，避免继续膨胀主页面。

## 大文件检查

- `react-ui/src/pages/Inventory/Overview/index.tsx`：407 行，已超过 400 行检查线；当前只负责三视图编排、权限 gating 和筛选选项加载，数量/时间区间转换、仓库平铺表、SPU 展开层和调整弹窗均已拆出，暂不继续拆分。
- `react-ui/src/pages/Inventory/Overview/helpers.tsx`：261 行，集中承载库存总览展示映射和筛选参数转换，职责单一，暂不拆分。
- `react-ui/src/pages/Inventory/Overview/components/InventoryAdjustButton.tsx`：429 行，已超过 400 行检查线；当前集中承载同一套 SPU/SKU/仓库调整弹窗和批量预览确认交互，拆开会增加状态流转成本。后续如果再加入销售保护期、批量导入或复杂风险提示，应拆成 `useInventoryAdjustRows`、`InventoryAdjustModal` 和表格列配置。
- `react-ui/src/pages/Inventory/Overview/components/SkuWarehouseTable.tsx`：259 行，职责集中在 SKU 展开/仓库明细表复用，暂不拆分。
- `react-ui/src/pages/Inventory/Overview/components/SpuSkuWarehouseTable.tsx`：89 行，职责单一，暂不拆分。
- `react-ui/src/pages/Inventory/Overview/components/WarehouseViewTable.tsx`：276 行，职责单一，暂不拆分。
- `InventoryOverviewServiceImpl.java`：854 行，已超过 500 行检查线；当前同时承载查询、读模型刷新、单行调整和批量调整，已经接近拆分点。本次为避免在业务规则仍在收缩时扩大 service 拆分，先保持交易规则集中；后续接入订单占用、来源扣减抵消、在途自动转可售或销售保护期时，应优先拆出 `InventoryOverviewAdjustmentService` 和库存重算 helper。
- `20260607_inventory_overview_platform_stock.sql`：701 行，包含同一批库存表、菜单权限、字典、回填和 guard。作为一次性增量脚本保持单文件，避免拆分后执行顺序和确认 token 分散。
- `20260608_inventory_overview_sku_baseline_refresh.sql`：只做字典补充、库存承载行补齐和读模型刷新，不新增表结构。
- `2026-06-07-inventory-overview-platform-stock-design.md`：890 行，是完整设计记录，不按源码拆分。

## 验证命令

| 命令 | 结果 |
| --- | --- |
| `mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test` | 通过，38 个测试成功 |
| `mvn -pl ruoyi-system -Dtest=InventoryAdminRouteContractTest test` | 通过，库存总览 admin route、warehouse/list、SPU/SKU/仓库组件契约已覆盖 |
| `mvn -pl inventory -am -DskipTests compile` | 通过 |
| `java ... UriliSqlRunner .env.local RuoYi-Vue/sql/20260607_inventory_overview_platform_stock.sql` | 通过，执行 54 条 SQL 语句 |
| `jshell ... RuoYi-Vue/sql/20260608_inventory_overview_sku_baseline_refresh.sql` | 通过，执行 30 条 SQL 语句 |
| `mvn -pl ruoyi-admin -am -DskipTests '-Dspring-boot.repackage.skip=true' package` | 通过 |
| `mvn -pl ruoyi-admin -am -DskipTests package` | 仓库视图变更后第二次通过，第一次因旧后端进程锁定 jar 在 repackage 阶段失败 |
| `npm run tsc -- --pretty false` | 通过 |
| `npx biome lint src\pages\Inventory\Overview\index.tsx src\pages\Inventory\Overview\helpers.tsx src\pages\Inventory\Overview\components\QuantityCell.tsx src\pages\Inventory\Overview\components\SkuWarehouseTable.tsx src\pages\Inventory\Overview\components\SpuSkuWarehouseTable.tsx src\pages\Inventory\Overview\components\WarehouseViewTable.tsx src\pages\Inventory\Overview\style.module.css src\services\inventory\overview.ts src\types\inventory\overview.d.ts` | 通过 |
| `git diff --check` | 无空白错误；仅有既有 CRLF/LF 提示 |
| `codegraph sync .` | 通过；本次配对状态筛选补充后同步 `Synced 18 changed files` |
| `.\start-backend-local.ps1 -Restart` | 通过，8080 已启动 |
| `GET /inventory/admin/overview/spu/list` | 登录后返回 `code=200,total=24`，首行状态 `NO_WAREHOUSE` |
| `GET /inventory/admin/overview/spu/{spuId}/sku-warehouses` | 登录后返回 `code=200`，首个 SPU 返回 5 个 SKU 分组，首个 SKU 有 1 条仓库明细 |
| `GET /inventory/admin/overview/sku/list` | 登录后返回 `code=200,total=69`，首行状态 `NO_WAREHOUSE` |
| `GET /inventory/admin/overview/sku/{skuId}/warehouses` | 登录后返回 `code=200`，首个 SKU 有 1 条 `NO_WAREHOUSE` 明细 |
| `GET /inventory/admin/overview/warehouse/list` | 登录后返回 `code=200,total=69`，首行带 SKU、商品名、仓库名和 `NO_WAREHOUSE` 状态 |
| `GET /inventory/admin/overview/*/list?pairingStatus=UNASSIGNED` | 通过；SPU 返回 24 条，SKU 返回 69 条，仓库视图返回 69 条 |
| `GET /inventory/admin/overview/*/list?pairingStatus=PAIRED` | 通过；当前库暂无已配对库存，SPU/SKU/仓库三类列表均返回 0 条 |
| 浏览器打开 `http://127.0.0.1:8001/inventory/overview` | 通过，SPU 展开一次显示 5 个 SKU 分组并直接展示仓库明细；SKU 视图仍可展开仓库；仓库视图平铺显示 20 行/总共 69 条，列头包含 SKU信息、仓库、类型和库存字段；仓库类型下拉只有官方仓、三方仓、未配置，不包含混合；SPU/SKU/仓库三视图均显示配对状态筛选，未配对查询分别返回 24/69/69 条，已配对在当前库返回空；控制台无错误 |

### 2026-06-08 第一版筛选补充验证

| 命令或验证项 | 结果 |
| --- | --- |
| `mvn -pl inventory -am -DskipTests compile` | 通过，inventory 模块 11 个源码文件编译成功 |
| `mvn -pl ruoyi-system -Dtest=InventoryAdminRouteContractTest test` | 通过，新增 `warehouse/options`、仓库筛选、区间筛选和前端 helper 契约已覆盖 |
| `npm run tsc -- --pretty false` | 通过 |
| `npx biome lint src\pages\Inventory\Overview\index.tsx src\pages\Inventory\Overview\helpers.tsx src\pages\Inventory\Overview\components\WarehouseViewTable.tsx src\types\inventory\overview.d.ts src\services\inventory\overview.ts` | 通过 |
| `mvn -pl ruoyi-admin -am -DskipTests package` | 第一次被 integration 模块既有 `ArrayList` 缺失 import 阻塞；补最小 import 后第二次通过，并生成新的 `ruoyi-admin.jar` |
| `.\start-backend-local.ps1 -Restart` | 通过，8080 已启动并返回 HTTP 200 |
| `GET /inventory/admin/overview/warehouse/options` | 登录后返回 `code=200`，当前库返回 1 个仓库选项：`发货仓库未配置` |
| `GET /inventory/admin/overview/spu/list?platformAvailableQtyMin=0&platformAvailableQtyMax=0` | 登录后返回 `code=200,total=24` |
| `GET /inventory/admin/overview/sku/list?platformTotalQtyMin=0&platformTotalQtyMax=0` | 登录后返回 `code=200,total=69` |
| `GET /inventory/admin/overview/warehouse/list?platformTotalQtyMin=0&platformTotalQtyMax=0` | 登录后返回 `code=200,total=69` |
| `GET /inventory/admin/overview/warehouse/list?latestStockUpdateTimeStart=2026-06-08 00:00:00` | 登录后返回 `code=200,total=69` |
| `GET /inventory/admin/overview/warehouse/list?warehouseKey=unconfigured\|NO_WAREHOUSE\|\|发货仓库未配置` | 登录后返回 `code=200,total=69`；PowerShell 默认输出中文 JSON 会乱码，使用正确 UTF-8 URL 编码后筛选结果正常 |
| 浏览器验证 `http://127.0.0.1:8001/inventory/overview` | 通过；SPU、SKU、仓库三视图筛选项均在原 ProTable 查询区内；仓库下拉可打开并显示 `发货仓库未配置`；选择仓库并查询后仍返回库存行；平台总库存 0-0 区间查询后仍返回库存行；仓库视图平铺列包含 SKU信息、仓库、类型、来源总库存、来源可用库存、来源在途库存、同步时间；浏览器控制台无错误 |
| `git diff --check` | 通过，无空白错误；仅输出既有 LF/CRLF 提示 |
| `codegraph sync .` | 通过；最终同步 2 个变更文件 |

### 2026-06-08 操作列与批量调整补充

本次补充库存总览操作列，覆盖 SPU 维度、SKU 维度、SPU 展开层 SKU 标题、SKU 展开层仓库行和仓库视图平铺行。

后端新增：

- 请求 DTO：`InventoryOverviewBatchAdjustRequest`、`InventoryOverviewBatchAdjustItem`。
- 预览结果 DTO：`InventoryOverviewBatchAdjustPreviewResult`、`InventoryOverviewBatchAdjustRowPreview`。
- 接口：
  - `POST /inventory/admin/overview/adjust/batch-preview`
  - `POST /inventory/admin/overview/adjust/batch-confirm`
- 规则：
  - 聚合行只作为操作入口，最终全部提交 `stockId` 明细行。
  - 官方仓平台总库存最大值为 `来源可用库存 - 待来源扣减`。
  - 官方仓平台在途库存最大值为 `来源在途库存 + 待来源可用观察数量`。
  - 三方仓平台总库存只要求不小于平台锁定库存，不使用来源库存上限。
  - 三方仓不允许设置平台在途库存。
  - `NO_WAREHOUSE` 行不允许保存。
  - 任一明细行校验失败时整批失败，不允许部分成功。

前端新增：

- `InventoryAdjustButton.tsx`：统一操作列按钮和小弹窗。
- `InventoryAdjustButton.js`：纯 TSX re-export 镜像。
- SPU / SKU 主表、`SpuSkuWarehouseTable`、`SkuWarehouseTable`、`WarehouseViewTable` 均接入操作列。
- 弹窗显示汇总区、明细表和备注，明细表包含调整后平台总库存、调整后平台在途库存、增减值、平台锁定库存和后端校验信息。

本次验证：

| 命令或验证项 | 结果 |
| --- | --- |
| `npx biome lint src\pages\Inventory\Overview\index.tsx src\pages\Inventory\Overview\helpers.tsx src\pages\Inventory\Overview\components\InventoryAdjustButton.tsx src\pages\Inventory\Overview\components\WarehouseViewTable.tsx src\pages\Inventory\Overview\components\SkuWarehouseTable.tsx src\pages\Inventory\Overview\components\SpuSkuWarehouseTable.tsx src\types\inventory\overview.d.ts src\services\inventory\overview.ts` | 通过 |
| `npm run tsc -- --pretty false` | 通过 |
| `npx jest --config jest.config.ts tests\inventory-overview-contract.test.ts --runInBand` | 通过，3 个测试成功 |
| `mvn -pl inventory -am -DskipTests compile` | 通过 |
| `mvn -pl ruoyi-system -Dtest=InventoryAdminRouteContractTest test` | 通过 |
| `mvn -pl ruoyi-admin -am -DskipTests package` | 第一次因 8080 旧后端进程占用 `ruoyi-admin.jar` 在 repackage 阶段失败；停旧进程后重新执行通过，并生成新 jar |
| `.\start-backend-local.ps1 -Restart` | 通过，8080 已启动并返回 HTTP 200 |
| `GET /inventory/admin/overview/warehouse/list?pageNum=1&pageSize=20` | 登录后返回 `code=200,total=76`；当前运行库已存在官方仓库存行 |
| `POST /inventory/admin/overview/adjust/batch-preview` | 官方仓明细行预览返回 `code=200,allowed=true`，提示需要确认后保存 |
| `POST /inventory/admin/overview/adjust/batch-preview` 官方仓超上限 | 来源可用 21、待来源扣减 0 时尝试调整到 22，返回 `allowed=false`，提示最大可调整为 21 |
| `POST /inventory/admin/overview/adjust/batch-confirm` 未配置仓行 | 返回 `code=500`，提示商品未配置发货仓库，不能调整平台库存 |
| `POST /inventory/admin/overview/adjust/batch-confirm` 合法官方仓行 + 非法未配置仓行 | 返回 `code=500`；合法官方仓行平台总库存前后均为 0，证明没有部分成功 |
| 浏览器验证 `http://127.0.0.1:8001/inventory/overview` | 通过；SPU 视图主行有操作列；SPU 展开一次后直接显示 SKU 摘要和仓库明细，SKU 标题和仓库行均有调整入口；SKU 视图主行有操作列和展开入口；仓库视图以 SKU+仓库平铺，包含操作列；SPU 调整弹窗包含汇总区、调整后平台总库存、调整后平台在途、增减值和平台锁定列；未执行保存 |

当前运行态说明：

- 早先记录中的“当前库存读模型全是仓库未配置”只代表当时数据状态；本次 2026-06-08 21:50 后验证时，当前运行库已存在官方仓库存行，仓库平铺列表 total 为 76。
- 本次为了避免制造业务数据，浏览器只验证弹窗打开和字段展示，未执行成功保存；成功路径通过后端预览、编译、契约和非写入 API 验证覆盖。

### 2026-06-08 SPU 展开层平铺优化

用户反馈 SPU 展开后“SKU 摘要行 + 每个 SKU 一个仓库表”的布局信息密度低，并且多个 SKU 会重复出现多套表头。已调整为：

- `SpuSkuWarehouseTable` 将 `spu/{spuId}/sku-warehouses` 返回的 SKU 分组压平成一个 `SKU + 仓库` 明细表。
- 展开层只保留一个表头。
- 第一列为 `SKU信息`，同一 SKU 多个仓库时重复展示 SKU 信息。
- 不同 SKU 之间通过轻微行间距区分，不再使用卡片和二级表头。
- SKU 聚合调整入口移动到每个 SKU 第一条仓库行的 SKU 单元格中，文案为 `调整SKU`；仓库行仍保留仓库维度 `调整库存`。

本次验证：

| 命令或验证项 | 结果 |
| --- | --- |
| `npx biome lint src\pages\Inventory\Overview\components\SkuWarehouseTable.tsx src\pages\Inventory\Overview\components\SpuSkuWarehouseTable.tsx src\pages\Inventory\Overview\components\InventoryAdjustButton.tsx src\pages\Inventory\Overview\style.module.css tests\inventory-overview-contract.test.ts` | 通过 |
| `npm run tsc -- --pretty false` | 通过 |
| `npx jest --config jest.config.ts tests\inventory-overview-contract.test.ts --runInBand` | 通过，4 个测试成功 |
| 浏览器验证 `http://127.0.0.1:8001/inventory/overview` | 通过；SPU 展开层 DOM 中只有 1 个 `thead`，可见表格只保留一个表头；展开行包含 `SKU信息`、仓库列、`调整SKU` 和仓库行 `调整库存`；不同 SKU 起始行带分组间距；截图确认没有多套表头堆叠 |

### 2026-06-08 库存调整原因改为选填

用户确认库存总览里调整库存时，调整原因不需要必填，只作为选填备注。

已调整：

- 行内双击调整入口 `QuantityCell` 不再校验空原因，输入框 placeholder 改为 `调整原因（选填）`。
- SPU / SKU / 仓库聚合调整弹窗 `InventoryAdjustButton` 不再校验空原因，输入框 placeholder 改为 `调整原因（选填）`。
- 前端库存总览 service 请求类型将 `reason` 改为可选字段。
- 后端 `InventoryOverviewServiceImpl` 对单行和批量调整请求只做 trim，空白原因归一为 `null`，不再拒绝保存。
- 合同测试固定页面不得出现 `请填写库存调整原因` 必填提示。

本次验证：

| 命令或验证项 | 结果 |
| --- | --- |
| `npx biome lint src\pages\Inventory\Overview\components\QuantityCell.tsx src\pages\Inventory\Overview\components\InventoryAdjustButton.tsx tests\inventory-overview-contract.test.ts` | 通过 |
| `npm run tsc -- --pretty false` | 通过 |
| `npx jest --config jest.config.ts tests\inventory-overview-contract.test.ts --runInBand` | 通过，4 个测试成功 |
| `mvn -pl inventory -am -DskipTests compile` | 通过 |
| `mvn -pl ruoyi-system -Dtest=InventoryAdminRouteContractTest test` | 通过 |
| `mvn -pl ruoyi-admin -am -DskipTests package` | 通过；执行前已停止旧 8080 进程，避免 repackage 阶段 jar 锁定 |
| `.\start-backend-local.ps1 -Restart` | 通过，8080 已启动并返回 HTTP 200 |
| `POST /inventory/admin/overview/adjust/preview` 空 `reason` + 未配置仓库存行 | 返回 `code=200,allowed=false`，业务提示为“商品未配置发货仓库，不能调整平台库存”，证明未被调整原因拦截 |
| `POST /inventory/admin/overview/adjust/confirm` 空 `reason` + 未配置仓库存行 | 返回 `code=500`，业务提示仍为“商品未配置发货仓库，不能调整平台库存”，未出现“调整原因不能为空” |
| 浏览器验证 `http://127.0.0.1:8001/inventory/overview` | 通过；调整弹窗 placeholder 为 `调整原因（选填）`，空原因修改库存后点击“预览并保存”进入“确认库存调整”二次确认，没有出现必填原因提示；验证后已取消弹窗，未执行保存 |
| `git diff --check` | 通过，无空白错误；仅输出既有 LF/CRLF 提示 |
| `codegraph sync .` | 通过；结果为 `Already up to date` |

## 未验证项

- 历史验证阶段曾出现“当前库存读模型已生成 24 个 SPU、69 个 SKU，状态均为 `仓库未配置`”的数据状态；该状态已被后续商品/来源仓库存配置刷新覆盖，不再代表 2026-06-08 21:50 后的当前运行库。
- 前端 dev server 使用 `DISABLE_MFSU=1` 启动验证；默认 MFSU 缓存启动存在同路径输出冲突，需要后续单独清理前端缓存策略。

## 下一步

1. 商品侧后续需要补齐发货仓/来源 SKU 配置流程，或在商品创建时明确官方仓与三方仓归属。
2. 后续接入订单占用和出库扣减逻辑，落地 `inventory_reservation` 和来源扣减待抵消流程。
3. 单独处理前端 MFSU 缓存冲突，恢复默认 dev server 启动方式。
